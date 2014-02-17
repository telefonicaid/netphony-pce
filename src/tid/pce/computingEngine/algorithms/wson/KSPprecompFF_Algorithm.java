package tid.pce.computingEngine.algorithms.wson;

import java.net.Inet4Address;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

import org.jgrapht.GraphPath;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;

import tid.pce.computingEngine.algorithms.ComputingAlgorithm;
import tid.ospf.ospfv2.lsa.tlv.subtlv.complexFields.BitmapLabelSet;
import tid.pce.computingEngine.ComputingRequest;
import tid.pce.computingEngine.algorithms.AlgorithmReservation;
import tid.pce.computingEngine.algorithms.ComputingAlgorithmPreComputation;
import tid.pce.computingEngine.algorithms.PCEPUtils;
import tid.pce.computingEngine.algorithms.wson.wa.FirstFit;
import tid.pce.pcep.constructs.EndPoint;
import tid.pce.pcep.constructs.EndPointAndRestrictions;
import tid.pce.pcep.constructs.P2MPEndpoints;
import tid.pce.pcep.constructs.P2PEndpoints;
import tid.pce.pcep.constructs.Path;
import tid.pce.pcep.constructs.Request;
import tid.pce.pcep.constructs.Response;
import tid.pce.pcep.messages.PCEPResponse;
import tid.pce.pcep.objects.EndPoints;
import tid.pce.pcep.objects.EndPointsIPv4;
import tid.pce.pcep.objects.ExplicitRouteObject;
import tid.pce.pcep.objects.GeneralizedEndPoints;
import tid.pce.pcep.objects.NoPath;
import tid.pce.pcep.objects.ObjectParameters;
import tid.pce.pcep.objects.RequestParameters;
import tid.pce.pcep.objects.ReservationConf;
import tid.pce.pcep.objects.tlvs.NoPathTLV;
import tid.pce.server.wson.ReservationManager;
import tid.pce.tedb.DomainTEDB;
import tid.pce.tedb.IntraDomainEdge;
import tid.pce.tedb.SimpleTEDB;
import tid.pce.tedb.TEDB;
import tid.pce.tedb.WSONInformation;
import tid.rsvp.RSVPProtocolViolationException;
import tid.rsvp.constructs.gmpls.DWDMWavelengthLabel;
import tid.rsvp.objects.subobjects.GeneralizedLabelEROSubobject;
import tid.rsvp.objects.subobjects.IPv4prefixEROSubobject;
import tid.rsvp.objects.subobjects.UnnumberIfIDEROSubobject;
import tid.pce.computingEngine.*;

public class KSPprecompFF_Algorithm implements ComputingAlgorithm {
	private WSONInformation WSONInfo;

	private Logger log=Logger.getLogger("PCEServer");
	
	private DomainTEDB ted;
	
	private GenericLambdaReservation  reserv;
	
	private ReservationManager reservationManager;
	
	private KSPprecompFF_AlgorithmPreComputation preComp;
	
	private ComputingRequest pathReq;
	
	public KSPprecompFF_Algorithm(ComputingRequest pathReq,TEDB ted, ReservationManager reservationManager ){
		//this.num_lambdas=((DomainTEDB)ted).getNumLambdas();
		this.pathReq=pathReq;
		this.reservationManager=reservationManager;
		this.ted=(DomainTEDB)ted;
	}
	
	public PCEPResponse call(){ 
		
		//Time stamp of the start of the algorithm;
		long tiempoini =System.nanoTime();
		
		log.finest("Starting KSPprecomp Algorithm");
		
		//Create the response message
		//It will contain either the path or noPath
		PCEPResponse m_resp=new PCEPResponse();
		//The request that needs to be solved
		Request req=pathReq.getRequestList().get(0);
		//Request Id, needed for the response
		long reqId=req.getRequestParameters().getRequestID();
		log.info("Request id: "+reqId+", getting endpoints");
		//Start creating the response
		Response response=new Response();
		RequestParameters rp = new RequestParameters();
		rp.setBidirect(req.getRequestParameters().isBidirect());
		rp.setRequestID(reqId);
		response.setRequestParameters(rp);
		m_resp.addResponse(response);
		
		EndPoints  EP= req.getEndPoints();	
		Inet4Address source_router_id_addr = null;
		Inet4Address dest_router_id_addr = null;

		if (EP.getOT()==ObjectParameters.PCEP_OBJECT_TYPE_ENDPOINTS_IPV4){
			EndPointsIPv4  ep=(EndPointsIPv4) req.getEndPoints();
			source_router_id_addr=ep.getSourceIP();
			dest_router_id_addr=ep.getDestIP();
		}else if (EP.getOT()==ObjectParameters.PCEP_OBJECT_TYPE_ENDPOINTS_IPV6){

		}else if (EP.getOT()==ObjectParameters.PCEP_OBJECT_TYPE_GENERALIZED_ENDPOINTS){
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
				
		//Now, check if the source and destination are in the TED.
		log.fine("Source: "+source_router_id_addr+"; Destination:"+dest_router_id_addr);
		if (!(((ted.containsVertex(source_router_id_addr))&&(ted.containsVertex(dest_router_id_addr))))){
			log.warning("Source or destination are NOT in the TED");	
			NoPath noPath= new NoPath();
			noPath.setNatureOfIssue(ObjectParameters.NOPATH_NOPATH_SAT_CONSTRAINTS);
			NoPathTLV noPathTLV=new NoPathTLV();
			if (!((ted.containsVertex(source_router_id_addr)))){
				log.finest("Unknown source");	
				noPathTLV.setUnknownSource(true);	
			}
			if (!((ted.containsVertex(dest_router_id_addr)))){
				log.finest("Unknown destination");
				noPathTLV.setUnknownDestination(true);	
			}

			noPath.setNoPathTLV(noPathTLV);				
			response.setNoPath(noPath);
			return m_resp;
		}
		
		boolean nopath=true;//Initially, we still have no path
		int lambda_chosen=0;//We begin with lambda index 0
		GraphPath<Object,IntraDomainEdge> gp_chosen=null;
		boolean noLambda = true;
		
		log.fine("Starting the selection of the path");
		
		if (!(source_router_id_addr.equals(dest_router_id_addr)))
			nopath=false;
		
		Hashtable<Object,List<GraphPath<Object,IntraDomainEdge>>> routeInterList= new Hashtable<Object,List<GraphPath<Object,IntraDomainEdge>>>();
		
		routeInterList = preComp.routeTable.get(source_router_id_addr);
		
		List<GraphPath<Object,IntraDomainEdge>> edge_K_list;
		edge_K_list = routeInterList.get(dest_router_id_addr);
		
		GraphPath<Object, IntraDomainEdge> EdgePath;
				
		int j =0;
		if (nopath==false){
			for (j =0; j<edge_K_list.size(); j++)
			{
				gp_chosen = edge_K_list.get(j);
	
				Path path=new Path();
				ExplicitRouteObject ero= new ExplicitRouteObject();
				List<IntraDomainEdge> edge_list=gp_chosen.getEdgeList();
				
				//FirstFit returns the lambda for the path
				lambda_chosen = FirstFit.getLambda(edge_list);
				
				//FF returns "-1" in case there is no lambda free for path
				if (lambda_chosen == -1)
				{
					noLambda = true;
					continue;
				}
								
				noLambda = false;
							
				//in case there is a lambda free for path
				
				int i;
				for (i=0;i<edge_list.size();i++){
					UnnumberIfIDEROSubobject eroso= new UnnumberIfIDEROSubobject();
					eroso.setRouterID((Inet4Address)edge_list.get(i).getSource());
					eroso.setInterfaceID(edge_list.get(i).getSrc_if_id());
					eroso.setLoosehop(false);
					ero.addEROSubobject(eroso);
					
					GeneralizedLabelEROSubobject genLabel= new GeneralizedLabelEROSubobject();
					ero.addEROSubobject(genLabel);
					//ITU-T Format
					DWDMWavelengthLabel WDMlabel=new DWDMWavelengthLabel();
					WDMlabel.setGrid(preComp.getWSONInfo().getGrid());
					WDMlabel.setChannelSpacing(preComp.getWSONInfo().getCs());
					WDMlabel.setN(lambda_chosen+preComp.getWSONInfo().getnMin());
					WDMlabel.setIdentifier(0);
					try {
						WDMlabel.encode();
					} catch (RSVPProtocolViolationException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					genLabel.setLabel(WDMlabel.getBytes());		
					
				}
				IPv4prefixEROSubobject eroso= new IPv4prefixEROSubobject();
				eroso.setIpv4address((Inet4Address)edge_list.get(edge_list.size()-1).getTarget());
				eroso.setPrefix(32);
				ero.addEROSubobject(eroso);
				path.seteRO(ero);
				PCEPUtils.completeMetric(path, req, edge_list);
				response.addPath(path);
	
				//FIXME: RESERVATION NEEDS TO BE IMPROVED!!!
				LinkedList<Object> sourceVertexList=new LinkedList<Object>();
				LinkedList<Object> targetVertexList=new LinkedList<Object>();
				for (i=0;i<edge_list.size();i++){
					sourceVertexList.add(edge_list.get(i).getSource());
					targetVertexList.add(edge_list.get(i).getTarget());
				}
				
				
				if (req.getReservation()!=null){
					
					reserv= new GenericLambdaReservation();
					reserv.setResp(m_resp);
					reserv.setLambda_chosen(lambda_chosen);
					reserv.setReservation(req.getReservation());
					reserv.setSourceVertexList(sourceVertexList);
					reserv.setTargetVertexList(targetVertexList);
					
					if (rp.isBidirect() == true){
						reserv.setBidirectional(true);
					}
					
					else{
						reserv.setBidirectional(false);
					}
					
					reserv.setReservationManager(reservationManager);
				}
			}
		}
		long tiempofin =System.nanoTime();
		long tiempotot=tiempofin-tiempoini;
		log.info("Ha tardado "+tiempotot+" nanosegundos");
		
		if (noLambda == true){
			log.fine("No path found");
			NoPath noPath= new NoPath();
			noPath.setNatureOfIssue(ObjectParameters.NOPATH_NOPATH_SAT_CONSTRAINTS);
			NoPathTLV noPathTLV=new NoPathTLV();
			noPath.setNoPathTLV(noPathTLV);				
			response.setNoPath(noPath);
			return m_resp;
		}
		return m_resp;
	}
	
	public void setPreComp(KSPprecompFF_AlgorithmPreComputation preComp) {
		this.preComp = preComp;
	}
	
	public AlgorithmReservation getReserv() {
		return reserv;
	}

	
}
