package tid.pce.computingEngine.algorithms.wson.svec;

import java.net.Inet4Address;
import java.util.List;
import java.util.logging.Logger;

import org.jgrapht.GraphPath;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;

import tid.pce.computingEngine.ComputingRequest;
import tid.pce.computingEngine.ComputingResponse;
import tid.pce.computingEngine.algorithms.AlgorithmReservation;
import tid.pce.computingEngine.algorithms.ComputingAlgorithm;
import tid.pce.computingEngine.algorithms.PCEPUtils;
import tid.pce.computingEngine.algorithms.wson.wa.FirstFit;
import tid.pce.pcep.constructs.Path;
import tid.pce.pcep.constructs.Request;
import tid.pce.pcep.constructs.Response;
import tid.pce.pcep.objects.EndPointsIPv4;
import tid.pce.pcep.objects.ExplicitRouteObject;
import tid.pce.pcep.objects.NoPath;
import tid.pce.pcep.objects.ObjectParameters;
import tid.pce.pcep.objects.RequestParameters;
import tid.pce.pcep.objects.tlvs.NoPathTLV;
import tid.pce.tedb.IntraDomainEdge;
import tid.pce.tedb.SimpleTEDB;
import tid.pce.tedb.TEDB;
import tid.rsvp.RSVPProtocolViolationException;
import tid.rsvp.constructs.gmpls.DWDMWavelengthLabel;
import tid.rsvp.objects.subobjects.GeneralizedLabelEROSubobject;
import tid.rsvp.objects.subobjects.IPv4prefixEROSubobject;
import tid.rsvp.objects.subobjects.UnnumberIfIDEROSubobject;


public class SVEC_SP_FF_WSON_PathComputing implements ComputingAlgorithm {
	
	private SimpleDirectedWeightedGraph<Object,IntraDomainEdge> networkGraphOrig;
	private Logger log=Logger.getLogger("PCEServer");
	private ComputingRequest pathReq;
	
	/**
	 * Access to the Precomputation part of the algorithm.
	 */
	private SVEC_SP_FF_WSON_PathComputingPreComputation preComp;
	
	public SVEC_SP_FF_WSON_PathComputing(ComputingRequest pathReq,TEDB ted){
		this.networkGraphOrig= ((SimpleTEDB)ted).getDuplicatedNetworkGraph();
		this.pathReq=pathReq;
	}
	
	public ComputingResponse call(){
		ComputingResponse m_resp=new ComputingResponse();
		m_resp.setEncodingType(pathReq.getEcodingType());
		SimpleDirectedWeightedGraph<Object,IntraDomainEdge> networkGraph=PCEPUtils.duplicateTEDDB(networkGraphOrig);
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
					log.warning("Source or destination are NOT in the TED");	
					NoPath noPath= new NoPath();
					noPath.setNatureOfIssue(ObjectParameters.NOPATH_NOPATH_SAT_CONSTRAINTS);
					NoPathTLV noPathTLV=new NoPathTLV();
					if (!((networkGraph.containsVertex(source_router_id_addr)))){
						log.finest("Unknown source");	
						noPathTLV.setUnknownSource(true);	
					}
					if (!((networkGraph.containsVertex(dest_router_id_addr)))){
						log.finest("Unknown destination");
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
						path.seteRO(ero);
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
			log.info("There are "+pathReq.getRequestList().size()+" requests");
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
					log.warning("Source or destination are NOT in the TED");	
					NoPath noPath= new NoPath();
					noPath.setNatureOfIssue(ObjectParameters.NOPATH_NOPATH_SAT_CONSTRAINTS);
					NoPathTLV noPathTLV=new NoPathTLV();
					if (!((networkGraph.containsVertex(source_router_id_addr)))){
						log.finest("Unknown source");	
						noPathTLV.setUnknownSource(true);	
					}
					if (!((networkGraph.containsVertex(dest_router_id_addr)))){
						log.finest("Unknown destination");
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
						//log.info("PATH FOUND!!!!");
						
						
						Path path=new Path();
						ExplicitRouteObject ero= new ExplicitRouteObject();
						List<IntraDomainEdge> edge_list=gp.getEdgeList();
						int lambda=FirstFit.getLambda(edge_list);
						if (lambda>=0){
							//log.info("LAMBDA FOUND!!!!");
							int j;
							for (j=0;j<edge_list.size();j++){
								UnnumberIfIDEROSubobject eroso= new UnnumberIfIDEROSubobject();
								eroso.setRouterID((Inet4Address)edge_list.get(j).getSource());
								eroso.setInterfaceID(edge_list.get(j).getSrc_if_id());
								eroso.setLoosehop(false);
								ero.addEROSubobject(eroso);
								GeneralizedLabelEROSubobject genLabel= new GeneralizedLabelEROSubobject();
								ero.addEROSubobject(genLabel);
								//ITU-T Format
								DWDMWavelengthLabel WDMlabel=new DWDMWavelengthLabel();
								WDMlabel.setGrid(preComp.getWSONInfo().getGrid());
								WDMlabel.setChannelSpacing(preComp.getWSONInfo().getCs());
								WDMlabel.setN(lambda+preComp.getWSONInfo().getnMin());
								WDMlabel.setIdentifier(0);
								try {
									WDMlabel.encode();
								} catch (RSVPProtocolViolationException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								genLabel.setLabel(WDMlabel.getBytes());		
								edge_list.get(j).getTE_info().setWavelengthOccupied(lambda);
							 }
							IPv4prefixEROSubobject eroso= new IPv4prefixEROSubobject();
							eroso.setIpv4address((Inet4Address)edge_list.get(edge_list.size()-1).getTarget());
							eroso.setPrefix(32);
							ero.addEROSubobject(eroso);
							path.seteRO(ero);
							response.addPath(path);
							
							
						}else {
							NoPath noPath= new NoPath();
							noPath.setNatureOfIssue(ObjectParameters.NOPATH_NOPATH_SAT_CONSTRAINTS);
							response.setNoPath(noPath);		
						}	
					}
				}
			}
		}
		return m_resp;
	}
	
	public void setPreComp(SVEC_SP_FF_WSON_PathComputingPreComputation preComp) {
		this.preComp = preComp;
	}

	@Override
	public AlgorithmReservation getReserv() {
		// TODO Auto-generated method stub
		return null;
	}

}
