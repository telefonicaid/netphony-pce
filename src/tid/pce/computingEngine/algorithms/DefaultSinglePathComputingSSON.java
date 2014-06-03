package tid.pce.computingEngine.algorithms;

import java.io.IOException;
import java.net.Inet4Address;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

import org.jgrapht.GraphPath;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;

import tid.rsvp.objects.subobjects.IPv4prefixEROSubobject;
import tid.rsvp.objects.subobjects.UnnumberIfIDEROSubobject;
import tid.pce.computingEngine.ComputingRequest;
import tid.pce.computingEngine.ComputingResponse;
import tid.pce.pcep.PCEPProtocolViolationException;
import tid.pce.pcep.constructs.EndPoint;
import tid.pce.pcep.constructs.EndPointAndRestrictions;
import tid.pce.pcep.constructs.P2MPEndpoints;
import tid.pce.pcep.constructs.P2PEndpoints;
import tid.pce.pcep.constructs.Path;
import tid.pce.pcep.constructs.Request;
import tid.pce.pcep.constructs.Response;
import tid.pce.pcep.objects.EndPoints;
import tid.pce.pcep.objects.EndPointsIPv4;
import tid.pce.pcep.objects.ExplicitRouteObject;
import tid.pce.pcep.objects.GeneralizedEndPoints;
import tid.pce.pcep.objects.Metric;
import tid.pce.pcep.objects.Monitoring;
import tid.pce.pcep.objects.NoPath;
import tid.pce.pcep.objects.ObjectParameters;
import tid.pce.pcep.objects.RequestParameters;
import tid.pce.pcep.objects.tlvs.EndPointIPv4TLV;
import tid.pce.pcep.objects.tlvs.EndPointsIPv4TLV;
import tid.pce.pcep.objects.tlvs.NoPathTLV;
import tid.pce.pcep.objects.tlvs.PCEPTLV;
import tid.pce.tedb.ITMDTEDB;
import tid.pce.tedb.IntraDomainEdge;
import tid.pce.tedb.MDTEDB;
import tid.pce.tedb.TEDB;
import tid.pce.tedb.SimpleTEDB;
import tid.pce.tedb.SimpleITTEDB;

public class DefaultSinglePathComputingSSON implements ComputingAlgorithm {
	
	private SimpleDirectedWeightedGraph<Object,IntraDomainEdge> networkGraph;
	private Logger log=Logger.getLogger("PCEServer");
	private ComputingRequest pathReq;
	private TEDB ted;
	
	public DefaultSinglePathComputingSSON(ComputingRequest pathReq,TEDB ted ){
		this.ted=ted;
		try {
			if (ted.getClass().equals(SimpleTEDB.class)){
				this.networkGraph= ((SimpleTEDB)ted).getDuplicatedNetworkGraph();
			} else if (ted.getClass().equals(MDTEDB.class) ){
				//this.networkGraph= ((MDTEDB)ted).getDuplicatedNetworkGraph();
				this.networkGraph=null;
			} else if (ted.getClass().equals(SimpleITTEDB.class) ){
				this.networkGraph= ((SimpleITTEDB)ted).getDuplicatedNetworkGraph();
			}
	
			this.pathReq=pathReq;	
		}catch (Exception e){
			this.pathReq=pathReq;
			this.networkGraph=null;
		}
		
	}
	
	public ComputingResponse call(){
		long tiempoini =System.nanoTime();
		ComputingResponse m_resp=new ComputingResponse();
		Request req=pathReq.getRequestList().get(0);
		long reqId=req.getRequestParameters().getRequestID();
		log.info("Processing Single Path Computing Request id: "+reqId);
		
		//Start creating the response
		Response response=new Response();
		RequestParameters rp = new RequestParameters();
		rp.setRequestID(reqId);
		response.setRequestParameters(rp);
		 if (this.networkGraph==null){
			 NoPath noPath= new NoPath();
			noPath.setNatureOfIssue(ObjectParameters.NOPATH_NOPATH_SAT_CONSTRAINTS);
				NoPathTLV noPathTLV=new NoPathTLV();	
				noPath.setNoPathTLV(noPathTLV);				
				response.setNoPath(noPath);
				m_resp.addResponse(response);
				return m_resp;
		 }
		
		
		
		//esto hay que cambiarlo para poder leer del GENERALIZED END POINTS
		//if (getObjectType(req.getEndPoints()))
		EndPoints  EP= req.getEndPoints();	
		Object source_router_id_addr = null;
		Object dest_router_id_addr = null;
		
		if (EP.getOT()==ObjectParameters.PCEP_OBJECT_TYPE_ENDPOINTS_IPV4){
			EndPointsIPv4  ep=(EndPointsIPv4) req.getEndPoints();
			source_router_id_addr=ep.getSourceIP();
			dest_router_id_addr=ep.getDestIP();
		}else if (EP.getOT()==ObjectParameters.PCEP_OBJECT_TYPE_ENDPOINTS_IPV6){
			
		}
		
		if (EP.getOT()==ObjectParameters.PCEP_OBJECT_TYPE_GENERALIZED_ENDPOINTS){
			GeneralizedEndPoints  gep=(GeneralizedEndPoints) req.getEndPoints();
			if(gep.getGeneralizedEndPointsType()==ObjectParameters.PCEP_GENERALIZED_END_POINTS_TYPE_P2P){
				P2PEndpoints p2pep= gep.getP2PEndpoints();
				EndPoint sourceep=p2pep.getSourceEndPoint();
				EndPoint destep=p2pep.getDestinationEndPoint();
				source_router_id_addr=sourceep.getEndPointIPv4TLV().IPv4address;
				dest_router_id_addr=destep.getEndPointIPv4TLV().IPv4address;
			}
			if(gep.getGeneralizedEndPointsType()==ObjectParameters.PCEP_GENERALIZED_END_POINTS_TYPE_P2MP_NEW_LEAVES){
				P2MPEndpoints p2mpep= gep.getP2MPEndpoints();
				EndPointAndRestrictions epandrest=p2mpep.getEndPointAndRestrictions();
				EndPoint sourceep=epandrest.getEndPoint();
				source_router_id_addr=sourceep.getEndPointIPv4TLV().IPv4address;
				int cont=0;
				while (cont<=p2mpep.getEndPointAndRestrictionsList().size()){ //esto est� mal
					epandrest=p2mpep.getEndPointAndRestrictionsList().get(cont);
					EndPoint destep=epandrest.getEndPoint();
					source_router_id_addr=sourceep.getEndPointIPv4TLV().IPv4address;
					dest_router_id_addr=destep.getEndPointIPv4TLV().IPv4address;

				}
			}
		}
		//aqu� acaba lo que he a�adido

		
			
		//EndPointsIPv4  ep=(EndPointsIPv4) req.getEndPoints();
		//Object source_router_id_addr=ep.getSourceIP();
		log.info("Source: "+source_router_id_addr);
		//Object dest_router_id_addr=ep.getDestIP();
		log.info("Destination: "+dest_router_id_addr);
		log.finest("Check if we have source and destination in our TED");
		((SimpleTEDB)ted).printTopology();
		
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
			return m_resp;
		}
			
		log.finest("Computing path");
		//long tiempoini =System.nanoTime();
		DijkstraShortestPath<Object,IntraDomainEdge>  dsp=new DijkstraShortestPath<Object,IntraDomainEdge> (networkGraph, source_router_id_addr, dest_router_id_addr);
		GraphPath<Object,IntraDomainEdge> gp=dsp.getPath();
	
		log.finest("Creating response");
		if (gp==null){
			log.warning("No Path Found");	
			NoPath noPath= new NoPath();
			noPath.setNatureOfIssue(ObjectParameters.NOPATH_NOPATH_SAT_CONSTRAINTS);				
			response.setNoPath(noPath);
			m_resp.addResponse(response);
			return m_resp;
		}
	
		m_resp.addResponse(response);
		Path path=new Path();
		ExplicitRouteObject ero= new ExplicitRouteObject();
		List<IntraDomainEdge> edge_list=gp.getEdgeList();
		int i;
		for (i=0;i<edge_list.size();i++){
			UnnumberIfIDEROSubobject eroso= new UnnumberIfIDEROSubobject();
			eroso.setRouterID((Inet4Address)edge_list.get(i).getSource());
			eroso.setInterfaceID(edge_list.get(i).getSrc_if_id());
			eroso.setLoosehop(false);
			ero.addEROSubobject(eroso);
		 }
		IPv4prefixEROSubobject eroso= new IPv4prefixEROSubobject();
		eroso.setIpv4address((Inet4Address)edge_list.get(edge_list.size()-1).getTarget());
		eroso.setPrefix(32);
		ero.addEROSubobject(eroso);
		path.seteRO(ero);
		if (req.getMetricList().size()!=0){
			Metric metric=new Metric();
			metric.setMetricType(req.getMetricList().get(0).getMetricType() );
			log.fine("Number of hops "+edge_list.size());
			float metricValue=(float)edge_list.size();
			metric.setMetricValue(metricValue);
			path.getMetricList().add(metric);
		}
		response.addPath(path);
		long tiempofin =System.nanoTime();
		long tiempotot=tiempofin-tiempoini;
		log.info("Ha tardado "+tiempotot+" nanosegundos");
		Monitoring monitoring=pathReq.getMonitoring();
		if (monitoring!=null){
			if (monitoring.isProcessingTimeBit()){
				
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
