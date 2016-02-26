package es.tid.pce.computingEngine.algorithms;

import java.net.Inet4Address;
import java.util.List;
import java.util.logging.Logger;

import org.jgrapht.GraphPath;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;

import es.tid.of.DataPathID;
import es.tid.pce.computingEngine.ComputingRequest;
import es.tid.pce.computingEngine.ComputingResponse;
import es.tid.pce.pcep.constructs.EndPoint;
import es.tid.pce.pcep.constructs.EndPointAndRestrictions;
import es.tid.pce.pcep.constructs.P2MPEndpoints;
import es.tid.pce.pcep.constructs.P2PEndpoints;
import es.tid.pce.pcep.constructs.Path;
import es.tid.pce.pcep.constructs.Request;
import es.tid.pce.pcep.constructs.Response;
import es.tid.pce.pcep.objects.EndPoints;
import es.tid.pce.pcep.objects.EndPointsIPv4;
import es.tid.pce.pcep.objects.ExplicitRouteObject;
import es.tid.pce.pcep.objects.GeneralizedEndPoints;
import es.tid.pce.pcep.objects.Metric;
import es.tid.pce.pcep.objects.Monitoring;
import es.tid.pce.pcep.objects.NoPath;
import es.tid.pce.pcep.objects.ObjectParameters;
import es.tid.pce.pcep.objects.RequestParameters;
import es.tid.pce.pcep.objects.tlvs.NoPathTLV;
import es.tid.rsvp.objects.subobjects.DataPathIDEROSubobject;
import es.tid.rsvp.objects.subobjects.IPv4prefixEROSubobject;
import es.tid.rsvp.objects.subobjects.UnnumberIfIDEROSubobject;
import es.tid.rsvp.objects.subobjects.UnnumberedDataPathIDEROSubobject;
import es.tid.tedb.IntraDomainEdge;
import es.tid.tedb.MDTEDB;
import es.tid.tedb.SimpleITTEDB;
import es.tid.tedb.SimpleTEDB;
import es.tid.tedb.TEDB;

public class DefaultSinglePathComputing implements ComputingAlgorithm {

	private SimpleDirectedWeightedGraph<Object,IntraDomainEdge> networkGraph;
	private Logger log=Logger.getLogger("PCEServer");
	private ComputingRequest pathReq;
	private TEDB ted;

	public DefaultSinglePathComputing(ComputingRequest pathReq,TEDB ted ){
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


		EndPoints  EP= req.getEndPoints();	
		Object source_router_id_addr = null;
		Object dest_router_id_addr = null;
		Object router_xro = null;
		long source_port = 0;
		long destination_port = 0;
		DataPathID xro = new DataPathID();
		

		if (EP.getOT()==ObjectParameters.PCEP_OBJECT_TYPE_ENDPOINTS_IPV4){
			EndPointsIPv4  ep=(EndPointsIPv4) req.getEndPoints();
			source_router_id_addr=ep.getSourceIP();
			dest_router_id_addr=ep.getDestIP();
		}else if (EP.getOT()==ObjectParameters.PCEP_OBJECT_TYPE_ENDPOINTS_IPV6){
			log.info("ENDPOINTS IPv6 not supported");
		}

		if (EP.getOT()==ObjectParameters.PCEP_OBJECT_TYPE_GENERALIZED_ENDPOINTS){
			GeneralizedEndPoints  gep=(GeneralizedEndPoints) req.getEndPoints();
			if(gep.getGeneralizedEndPointsType()==ObjectParameters.PCEP_GENERALIZED_END_POINTS_TYPE_P2P){
				P2PEndpoints p2pep= gep.getP2PEndpoints();
				EndPoint sourceep = p2pep.getSourceEndPoint();
				EndPoint destep=p2pep.getDestinationEndPoint();
								
				if (sourceep.getEndPointIPv4TLV() != null && destep.getEndPointIPv4TLV() != null){
					// IPv4 End Points					
					source_router_id_addr=sourceep.getEndPointIPv4TLV().IPv4address;
					dest_router_id_addr=destep.getEndPointIPv4TLV().IPv4address;
					
				}else if (sourceep.getUnnumberedEndpoint() != null && destep.getUnnumberedEndpoint() != null){
					// IPv4 End Points					
					source_router_id_addr=sourceep.getUnnumberedEndpoint().IPv4address;
					dest_router_id_addr=destep.getUnnumberedEndpoint().IPv4address;
					
					source_port=sourceep.getUnnumberedEndpoint().getIfID();
					destination_port=destep.getUnnumberedEndpoint().getIfID();
					
				}else if (sourceep.getEndPointDataPathTLV() != null && destep.getEndPointDataPathTLV() != null){
					// Datapath ID End Points
					log.info("router_id_addr type: DataPathID, "+sourceep.toString().toUpperCase()+"  "+destep.toString().toUpperCase());
					source_router_id_addr=sourceep.getEndPointDataPathTLV().getDataPathID();
					dest_router_id_addr=destep.getEndPointDataPathTLV().getDataPathID();
/*Aqui no tiene sentido el xro porque no depende del endpoint
					//Case XRO is not null
					if(req.getXro()!=null){
						router_xro=req.getXro().getEROSubobjectList().getFirst().toString().substring(0,23);
						xro.setDataPathID((String)router_xro);
						log.info("Algorithm.getXro ::"+xro);
					}
				*/
				}else if (sourceep.getEndPointUnnumberedDataPathTLV() != null && destep.getEndPointUnnumberedDataPathTLV() != null){
					// UnnumberedDataPath ID End Points
					log.info("router_id_addr type: Unnumbered DataPathID, "+sourceep.toString().toUpperCase()+"  "+destep.toString().toUpperCase());
					source_router_id_addr=sourceep.getEndPointUnnumberedDataPathTLV().getDataPathID();
					dest_router_id_addr=destep.getEndPointUnnumberedDataPathTLV().getDataPathID();

					source_port=sourceep.getEndPointUnnumberedDataPathTLV().getIfID();
					destination_port=destep.getEndPointUnnumberedDataPathTLV().getIfID();
				}else {
					log.info("Error in the EndPoints -  not defined");
				}

			}
			if(gep.getGeneralizedEndPointsType()==ObjectParameters.PCEP_GENERALIZED_END_POINTS_TYPE_P2MP_NEW_LEAVES){
				P2MPEndpoints p2mpep= gep.getP2MPEndpoints();
				EndPointAndRestrictions epandrest=p2mpep.getEndPointAndRestrictions();
				EndPoint sourceep=epandrest.getEndPoint();
				source_router_id_addr=sourceep.getEndPointIPv4TLV().IPv4address;
				int cont=0;
				while (cont<=p2mpep.getEndPointAndRestrictionsList().size()){
					epandrest=p2mpep.getEndPointAndRestrictionsList().get(cont);
					EndPoint destep=epandrest.getEndPoint();
					source_router_id_addr=sourceep.getEndPointIPv4TLV().IPv4address;
					dest_router_id_addr=destep.getEndPointIPv4TLV().IPv4address;

				}
			}
		}

		log.info("Algorithm->  Source:: "+source_router_id_addr+" Destination:: "+dest_router_id_addr);
		log.info("Check if we have source and destination in our TED");
		
		
		//Case XRO is not null
		if(req.getXro()!=null){
			xro.setDataPathID(req.getXro().getXROSubobjectList().getFirst().toString());
			log.info("Algorithm.getXro ::"+xro);
			if (networkGraph.containsVertex(xro)){
				log.info("Delete node in graph:: "+xro);
				networkGraph.removeVertex(xro);
			}
		}

		if (!((networkGraph.containsVertex(source_router_id_addr))&&(networkGraph.containsVertex(dest_router_id_addr)))){
			log.warning("DefaultSinglePathComputing:: Source or destination are NOT in the TED");	
			NoPath noPath= new NoPath();
			noPath.setNatureOfIssue(ObjectParameters.NOPATH_NOPATH_SAT_CONSTRAINTS);
			NoPathTLV noPathTLV=new NoPathTLV();
			if (!((networkGraph.containsVertex(source_router_id_addr)))){
				log.warning("Unknown source");	
				noPathTLV.setUnknownSource(true);	
			}
			if (!((networkGraph.containsVertex(dest_router_id_addr)))){
				log.warning("Unknown destination");
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
			log.warning("DefaultSinglePathComputing:: No Path Found");	
			NoPath noPath= new NoPath();
			noPath.setNatureOfIssue(ObjectParameters.NOPATH_NOPATH_SAT_CONSTRAINTS);				
			response.setNoPath(noPath);
			m_resp.addResponse(response);
			return m_resp;
		}

		// Code ERO Object
		m_resp.addResponse(response);
		Path path=new Path();
		ExplicitRouteObject ero= new ExplicitRouteObject();
		List<IntraDomainEdge> edge_list=gp.getEdgeList();
		
		// Add first hop in the ERO Object in there is an interface
		if (source_port!=0){
			if (edge_list.get(0).getSource() instanceof Inet4Address){
				UnnumberIfIDEROSubobject eroso = new UnnumberIfIDEROSubobject();
				eroso.setRouterID((Inet4Address)edge_list.get(0).getSource());
				eroso.setInterfaceID(source_port);
				eroso.setLoosehop(false);
				ero.addEROSubobject(eroso);
			}else if (edge_list.get(0).getSource() instanceof DataPathID){
				UnnumberedDataPathIDEROSubobject eroso = new UnnumberedDataPathIDEROSubobject();
				eroso.setDataPath((DataPathID)edge_list.get(0).getSource());
				eroso.setInterfaceID(source_port);
				eroso.setLoosehop(false);
				ero.addEROSubobject(eroso);
			}else{
				log.info("Edge instance error");
			}
		} 
		
		// Add intermediate hops
		int i;
		for (i=0;i<edge_list.size();i++){
			if (edge_list.get(i).getSource() instanceof Inet4Address){
				UnnumberIfIDEROSubobject eroso = new UnnumberIfIDEROSubobject();
				eroso.setRouterID((Inet4Address)edge_list.get(i).getSource());
				eroso.setInterfaceID(edge_list.get(i).getSrc_if_id());
				eroso.setLoosehop(false);
				ero.addEROSubobject(eroso);
			}else if (edge_list.get(i).getSource() instanceof DataPathID){
				UnnumberedDataPathIDEROSubobject eroso = new UnnumberedDataPathIDEROSubobject();
				eroso.setDataPath((DataPathID)edge_list.get(i).getSource());
				eroso.setInterfaceID(edge_list.get(i).getSrc_if_id());
				eroso.setLoosehop(false);
				ero.addEROSubobject(eroso);
			}else{
				log.info("Edge instance error");
			}
		}
		// Add last hop in the ERO Object
		log.info("jm dspc destination_port: "+ destination_port);
		if (destination_port!=0){
			if (edge_list.get(edge_list.size()-1).getTarget() instanceof Inet4Address){
				log.info("jm defoultsingle ultima interfaz ip"+ edge_list.get(edge_list.size()-1));
				UnnumberIfIDEROSubobject eroso = new UnnumberIfIDEROSubobject();
				eroso.setRouterID((Inet4Address)edge_list.get(edge_list.size()-1).getTarget());
				eroso.setInterfaceID(destination_port);
				eroso.setLoosehop(false);
				ero.addEROSubobject(eroso);
			}else if (edge_list.get(edge_list.size()-1).getTarget() instanceof DataPathID){
				log.info("jm defoultsingle ultima interfaz dpid"+ edge_list.get(edge_list.size()-1));
				UnnumberedDataPathIDEROSubobject eroso = new UnnumberedDataPathIDEROSubobject();
				eroso.setDataPath((DataPathID)edge_list.get(edge_list.size()-1).getTarget());
				eroso.setInterfaceID(destination_port);
				eroso.setLoosehop(false);
				ero.addEROSubobject(eroso);
			}			
		} else {
			if (edge_list.get(edge_list.size()-1).getTarget() instanceof Inet4Address){
				IPv4prefixEROSubobject eroso= new IPv4prefixEROSubobject();
				eroso.setIpv4address((Inet4Address)edge_list.get(edge_list.size()-1).getTarget());
				eroso.setPrefix(32);
				ero.addEROSubobject(eroso);
			} else if (edge_list.get(edge_list.size()-1).getTarget() instanceof DataPathID){
				DataPathIDEROSubobject eroso = new DataPathIDEROSubobject();
				eroso.setDataPath((DataPathID)edge_list.get(edge_list.size()-1).getTarget());
				ero.addEROSubobject(eroso);
			}else{
				log.info("Edge instance error");
			}
		}

		log.info("Algorithm.ero :: "+ero.toString());
		path.setEro(ero);
		log.info("Algorithm.path:: "+path.toString());

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
