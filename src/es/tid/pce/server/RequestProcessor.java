package es.tid.pce.server;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.util.List;
import java.util.logging.Logger;

import org.jgrapht.GraphPath;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;

import es.tid.pce.computingEngine.ComputingResponse;
import es.tid.pce.pcep.PCEPProtocolViolationException;
import es.tid.pce.pcep.constructs.EndPoint;
import es.tid.pce.pcep.constructs.EndPointAndRestrictions;
import es.tid.pce.pcep.constructs.P2MPEndpoints;
import es.tid.pce.pcep.constructs.P2PEndpoints;
import es.tid.pce.pcep.constructs.Path;
import es.tid.pce.pcep.constructs.Response;
import es.tid.pce.pcep.messages.PCEPRequest;
import es.tid.pce.pcep.messages.PCEPResponse;
import es.tid.pce.pcep.objects.EndPoints;
import es.tid.pce.pcep.objects.EndPointsIPv4;
import es.tid.pce.pcep.objects.ExplicitRouteObject;
import es.tid.pce.pcep.objects.GeneralizedEndPoints;
import es.tid.pce.pcep.objects.NoPath;
import es.tid.pce.pcep.objects.ObjectParameters;
import es.tid.pce.pcep.objects.RequestParameters;
import es.tid.pce.pcep.objects.tlvs.NoPathTLV;
import es.tid.rsvp.objects.subobjects.IPv4prefixEROSubobject;
import es.tid.rsvp.objects.subobjects.UnnumberIfIDEROSubobject;
import es.tid.tedb.IntraDomainEdge;
import es.tid.tedb.SimpleITTEDB;
import es.tid.tedb.SimpleTEDB;
import es.tid.tedb.TEDB;

public class RequestProcessor implements Runnable {
	
	private PCEPRequest req;
	private DataOutputStream out;
	private Logger log=Logger.getLogger("PCEServer");;
	
	private TEDB ted;
	private SimpleDirectedWeightedGraph<Object,IntraDomainEdge> networkGraph;
	
	private ParentPCERequestManager childPCERequestManager;
	
	
	public RequestProcessor(PCEPRequest req, DataOutputStream out, TEDB ted,ParentPCERequestManager childPCERequestManager){
		this.req=req;
		this.out=out;
		this.ted=ted;
		this.childPCERequestManager=childPCERequestManager;
	}
	@Override
	public void run() {
		log.info("Processing request");
		//Obtain the algorithm
		//FIXME: ESCOGER EL ALGORITMO!!!!! AHORA, A CAPONAZO
		log.info("Choosing default algorithm");
		//Obtain TED Copy
		//FIXME: Deber�a de haber una copia siempre a mano.....?
		//FIXME: ESCOGER DEPENDIENDO DEL TIPO DE RED
		log.info("Obtaining Network Graph copy");
		
		EndPoints  EP = null;
		
		Inet4Address source_router_id_addr = null;
		Inet4Address dest_router_id_addr = null;
		
		if (!ted.isITtedb()){
			networkGraph= ((SimpleTEDB)ted).getDuplicatedNetworkGraph();
			EP = this.req.getRequest(0).getEndPoints();
			source_router_id_addr= ((EndPointsIPv4)EP).getSourceIP();
			dest_router_id_addr=((EndPointsIPv4)EP).getDestIP();
		}else{
			networkGraph= ((SimpleITTEDB)ted).getDuplicatedNetworkGraph();
			EP = this.req.getRequest(0).getEndPoints();
			if (EP.getOT()==ObjectParameters.PCEP_OBJECT_TYPE_GENERALIZED_ENDPOINTS){
				GeneralizedEndPoints  gep=(GeneralizedEndPoints) EP;
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
		}
		
		
		//EndPointsIPv4  ep=(EndPointsIPv4) this.req.getRequest(0).getEndPoints();
		//Inet4Address source_router_id_addr=ep.getSourceIP();
		log.info("Source: "+source_router_id_addr);
		//Inet4Address dest_router_id_addr=ep.getDestIP();
		log.info("Destination: "+dest_router_id_addr);
		
	
		
		
		log.info("Check if we have source and destination in our TED");
		if (!((networkGraph.containsVertex(source_router_id_addr))&&(networkGraph.containsVertex(dest_router_id_addr)))){
			if (childPCERequestManager!=null){
				log.info("Source or destination are NOT in the TED, asking the parent PCE");
				//PCEPRequest msg_req=new PCEPRequest();
				PCEPResponse resp=childPCERequestManager.newRequest(req);
				try {
					try {
						resp.encode();
					} catch (PCEPProtocolViolationException e1) {
						// TODO Auto-generated catch block
						log.severe("Response from Parent PCE not valid!!!!!");
						return;
					}
					log.info("Request from Parent PCE processeed, about to send response");
					out.write(resp.getBytes());
					out.flush();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return;				
			}
			else{
				log.warning("Source or destination are NOT in the TED");
				ComputingResponse m_resp=new ComputingResponse();
				Response response=new Response();
				RequestParameters rp = new RequestParameters();
				rp.setRequestID(this.req.getRequest(0).getRequestParameters().getRequestID());
				response.setRequestParameters(rp);
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
				try {
					m_resp.encode();
				} catch (PCEPProtocolViolationException e1) {
					// TODO Auto-generated catch block
					log.severe("Response not valid!!!!!");
					return;
				}
				log.info("RequestProcessor: request processed");
				// TODO Auto-generated method stub
				try {
					log.info("Request processeed, about to send response");
					out.write(m_resp.getBytes());
					out.flush();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				log.info("Response sent!!");
				return;
			}
			
		}
		long tiempoini =System.currentTimeMillis();
		ComputingResponse m_resp=new ComputingResponse();
		Response response=new Response();
		// check if src and dst are the same 
		if (source_router_id_addr.equals(dest_router_id_addr)){
			log.info("Source and destination are the same!");
			Path path=new Path();
			
			RequestParameters rp = new RequestParameters();
			rp.setRequestID(this.req.getRequest(0).getRequestParameters().getRequestID());
			response.setRequestParameters(rp);
			
			ExplicitRouteObject ero= new ExplicitRouteObject();
			IPv4prefixEROSubobject eroso= new IPv4prefixEROSubobject();
			eroso.setIpv4address(source_router_id_addr);
			eroso.setPrefix(32);
			ero.addEROSubobject(eroso);
			path.seteRO(ero);
			
			/*if (req.getMetricList().size()!=0){
				Metric metric=new Metric();
				metric.setMetricType(req.getMetricList().get(0).getMetricType() );
				log.fine("Number of hops "+0);
				float metricValue=0;
				metric.setMetricValue(metricValue);
				path.getMetricList().add(metric);
			}*/
			response.addPath(path);
			long tiempofin =System.nanoTime();
			long tiempotot=tiempofin-tiempoini;
			
			//Monitoring monitoring=pathReq.getMonitoring();
			/*if (monitoring!=null){
				if (monitoring.isProcessingTimeBit()){
					
				}
			}*/
			m_resp.addResponse(response);
			//return m_resp;
			
			
		}
		else{
			log.info("Computing path");
			
			DijkstraShortestPath<Object,IntraDomainEdge>  dsp=new DijkstraShortestPath<Object,IntraDomainEdge> (networkGraph, source_router_id_addr, dest_router_id_addr);
			GraphPath<Object,IntraDomainEdge> gp=dsp.getPath();
			long tiempofin =System.currentTimeMillis();
			long tiempotot=tiempofin-tiempoini;
			log.info("Ha tardado "+tiempotot+" milisegundos");
			
			log.info("Creating response");
		
			
			RequestParameters rp = new RequestParameters();
			rp.setRequestID(this.req.getRequest(0).getRequestParameters().getRequestID());
			response.setRequestParameters(rp);
			m_resp.addResponse(response);
			Path path=new Path();
			ExplicitRouteObject ero= new ExplicitRouteObject();
			
			
			List<IntraDomainEdge> edge_list=gp.getEdgeList();
			/* PARA EL CASO DE IPV4, PONGO AHORA IF NO NUMERADOS
			int i;
			for (i=0;i<edge_list.size();i++){
				IPv4prefixEROSubobject eroso= new IPv4prefixEROSubobject();
				eroso.setIpv4address(edge_list.get(i).getSource());
				eroso.setPrefix(32);
				ero.addEROSubobject(eroso);
			 }		
			IPv4prefixEROSubobject eroso= new IPv4prefixEROSubobject();
			eroso.setIpv4address(edge_list.get(edge_list.size()-1).getTarget());
			eroso.setPrefix(32);
			ero.addEROSubobject(eroso);*/
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
			response.addPath(path);
		}
		
		
		try {
			
			m_resp.encode();
			
		} catch (PCEPProtocolViolationException e1) {
			// TODO Auto-generated catch block
			log.severe("Response not valid!!!!!");
			return;
		}
		log.info("RequestProcessor: request processed");
		// TODO Auto-generated method stub
		try {
			

			log.info("Request processeed, about to send response");
			out.write(m_resp.getBytes());
			out.flush();
			
			

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		log.info("Response sent!!");
	}
	
}
