package es.tid.pce.computingEngine.algorithms;

import java.net.Inet4Address;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.jgrapht.GraphPath;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;

import es.tid.pce.computingEngine.ComputingRequest;
import es.tid.pce.computingEngine.ComputingResponse;
import es.tid.pce.pcep.constructs.Path;
import es.tid.pce.pcep.constructs.Request;
import es.tid.pce.pcep.constructs.Response;
import es.tid.pce.pcep.objects.EndPointsIPv4;
import es.tid.pce.pcep.objects.ExplicitRouteObject;
import es.tid.pce.pcep.objects.NoPath;
import es.tid.pce.pcep.objects.ObjectParameters;
import es.tid.pce.pcep.objects.RequestParameters;
import es.tid.pce.pcep.objects.tlvs.NoPathTLV;
import es.tid.rsvp.objects.subobjects.IPv4prefixEROSubobject;
import es.tid.rsvp.objects.subobjects.UnnumberIfIDEROSubobject;
import es.tid.tedb.IntraDomainEdge;
import es.tid.tedb.SimpleTEDB;
import es.tid.tedb.TEDB;


public class DefaultSVECPathComputing implements ComputingAlgorithm {
	
	private SimpleDirectedWeightedGraph<Object,IntraDomainEdge> networkGraph;
	private Logger log=LoggerFactory.getLogger("PCEServer");
	private ComputingRequest pathReq;
	
	public DefaultSVECPathComputing(ComputingRequest pathReq,TEDB ted){
		this.networkGraph= ((SimpleTEDB)ted).getDuplicatedNetworkGraph();
		this.pathReq=pathReq;
	}
	
	public ComputingResponse call(){
		ComputingResponse m_resp=new ComputingResponse();
		
		log.info("Processing SVEC Path Computing Request: "+pathReq.getSvec().toString());
		if (pathReq.getSvec().getSvec().islDiverseBit()|pathReq.getSvec().getSvec().isnDiverseBit()| pathReq.getSvec().getSvec().issRLGDiverseBit()){
			log.info("Diverse Path Computation");
			for (int i=0;i<pathReq.getSvec().getSvec().getRequestIDlist().size();++i){
				Request req=pathReq.getRequestList().get(i);
				long reqId=req.getRequestParameters().getRequestID();
				RequestParameters rp = new RequestParameters();
				rp.setRequestID(reqId);
				Response response=new Response();
				response.setRequestParameters(rp);
				
				EndPointsIPv4  ep=(EndPointsIPv4) req.getEndPoints();
				Object source_router_id_addr=ep.getSourceIP();
				log.info("Source: "+source_router_id_addr);
				Object dest_router_id_addr=ep.getDestIP();
				log.info("Destination: "+dest_router_id_addr);
				if (!((networkGraph.containsVertex(source_router_id_addr))&&(networkGraph.containsVertex(dest_router_id_addr)))){
					log.warn("Source or destination are NOT in the TED");	
					NoPath noPath= new NoPath();
					noPath.setNatureOfIssue(ObjectParameters.NOPATH_NOPATH_SAT_CONSTRAINTS);
					NoPathTLV noPathTLV=new NoPathTLV();
					if (!((networkGraph.containsVertex(source_router_id_addr)))){
						log.debug("Unknown source");	
						noPathTLV.setUnknownSource(true);	
					}
					if (!((networkGraph.containsVertex(dest_router_id_addr)))){
						log.debug("Unknown destination");
						noPathTLV.setUnknownDestination(true);	
					}
					
					noPath.setNoPathTLV(noPathTLV);				
					response.setNoPath(noPath);
					m_resp.addResponse(response);					
				}
				else {
					log.info("Computing path");
					long tiempoini =System.currentTimeMillis();
					DijkstraShortestPath<Object,IntraDomainEdge>  dsp=new DijkstraShortestPath<Object,IntraDomainEdge> (networkGraph, source_router_id_addr, dest_router_id_addr);
					GraphPath<Object,IntraDomainEdge> gp=dsp.getPath();
					long tiempofin =System.currentTimeMillis();
					long tiempotot=tiempofin-tiempoini;
					log.info("Ha tardado "+tiempotot+" milisegundos");
					m_resp.addResponse(response);
					if (gp==null){
						log.info("NO PATH FOUND!!!!");
						NoPath noPath= new NoPath();
						noPath.setNatureOfIssue(ObjectParameters.NOPATH_NOPATH_SAT_CONSTRAINTS);
						response.setNoPath(noPath);
						
					}
					else {
						log.info("PATH FOUND!!!!");
						Path path=new Path();
						ExplicitRouteObject ero= new ExplicitRouteObject();
						List<IntraDomainEdge> edge_list=gp.getEdgeList();
						int j;
						for (j=0;j<edge_list.size();j++){
							UnnumberIfIDEROSubobject eroso= new UnnumberIfIDEROSubobject();
							eroso.setRouterID((Inet4Address)edge_list.get(j).getSource());
							eroso.setInterfaceID(edge_list.get(j).getSrc_if_id());
							eroso.setLoosehop(false);
							ero.addEROSubobject(eroso);
						 }
						IPv4prefixEROSubobject eroso= new IPv4prefixEROSubobject();
						eroso.setIpv4address((Inet4Address)edge_list.get(edge_list.size()-1).getTarget());
						eroso.setPrefix(32);
						ero.addEROSubobject(eroso);
						path.setEro(ero);
						response.addPath(path);
						if (pathReq.getSvec().getSvec().islDiverseBit()){
							log.info("Removing edges from graph!");
							for (j=0;j<edge_list.size();j++){
								networkGraph.removeEdge(edge_list.get(j));
							 }
							
						}
						else if (pathReq.getSvec().getSvec().issRLGDiverseBit()){
							log.info("Removing edges (NOW SRLGs are the links) from graph!");
							for (j=0;j<edge_list.size();j++){
								networkGraph.removeEdge(edge_list.get(j));
							 }
						}
						else {
							log.info("Removing nodes from graph!");
							for (j=1;j<edge_list.size();j++){
								networkGraph.removeVertex(edge_list.get(j).getSource());
							 }
						}
						
					}
					
					
				}
				
				
			}
		}else {
			log.info("Sincronized Path Computation");
			for (int i=0;i<pathReq.getSvec().getSvec().getRequestIDlist().size();++i){
				Request req=pathReq.getRequestList().get(i);
				long reqId=req.getRequestParameters().getRequestID();
				RequestParameters rp = new RequestParameters();
				rp.setRequestID(reqId);
				Response response=new Response();
				response.setRequestParameters(rp);
				EndPointsIPv4  ep=(EndPointsIPv4) req.getEndPoints();
				Object source_router_id_addr=ep.getSourceIP();
				log.info("Source: "+source_router_id_addr);
				Object dest_router_id_addr=ep.getDestIP();
				log.info("Destination: "+dest_router_id_addr);
				if (!((networkGraph.containsVertex(source_router_id_addr))&&(networkGraph.containsVertex(dest_router_id_addr)))){
					log.warn("Source or destination are NOT in the TED");	
					NoPath noPath= new NoPath();
					noPath.setNatureOfIssue(ObjectParameters.NOPATH_NOPATH_SAT_CONSTRAINTS);
					NoPathTLV noPathTLV=new NoPathTLV();
					if (!((networkGraph.containsVertex(source_router_id_addr)))){
						log.debug("Unknown source");	
						noPathTLV.setUnknownSource(true);	
					}
					if (!((networkGraph.containsVertex(dest_router_id_addr)))){
						log.debug("Unknown destination");
						noPathTLV.setUnknownDestination(true);	
					}
					
					noPath.setNoPathTLV(noPathTLV);				
					response.setNoPath(noPath);
					m_resp.addResponse(response);					
				}
				else {
					log.info("Computing path");
					long tiempoini =System.currentTimeMillis();
					DijkstraShortestPath<Object,IntraDomainEdge>  dsp=new DijkstraShortestPath<Object,IntraDomainEdge> (networkGraph, source_router_id_addr, dest_router_id_addr);
					GraphPath<Object,IntraDomainEdge> gp=dsp.getPath();
					long tiempofin =System.currentTimeMillis();
					long tiempotot=tiempofin-tiempoini;
					log.info("Ha tardado "+tiempotot+" milisegundos");
					m_resp.addResponse(response);
					if (gp==null){
						log.info("NO PATH FOUND!!!!");
						NoPath noPath= new NoPath();
						noPath.setNatureOfIssue(ObjectParameters.NOPATH_NOPATH_SAT_CONSTRAINTS);
						response.setNoPath(noPath);					
					}
					else {
						log.info("PATH FOUND!!!!");
						Path path=new Path();
						ExplicitRouteObject ero= new ExplicitRouteObject();
						List<IntraDomainEdge> edge_list=gp.getEdgeList();
						int j;
						for (j=0;j<edge_list.size();j++){
							System.out.println("edge "+j);
							System.out.println("size es "+edge_list.size());
							System.out.println("src es "+edge_list.get(j));
							UnnumberIfIDEROSubobject eroso= new UnnumberIfIDEROSubobject();
							eroso.setRouterID((Inet4Address)edge_list.get(j).getSource());
							eroso.setInterfaceID(edge_list.get(j).getSrc_if_id());
							eroso.setLoosehop(false);
							ero.addEROSubobject(eroso);
						 }
						IPv4prefixEROSubobject eroso= new IPv4prefixEROSubobject();
						eroso.setIpv4address((Inet4Address)edge_list.get(edge_list.size()-1).getTarget());
						eroso.setPrefix(32);
						ero.addEROSubobject(eroso);
						path.setEro(ero);
						response.addPath(path);
						
					}			
				}
			}
		}
		return m_resp;
	}

	@Override
	public AlgorithmReservation getReserv() {
		// TODO Auto-generated method stub
		return null;
	}
	
	

}
