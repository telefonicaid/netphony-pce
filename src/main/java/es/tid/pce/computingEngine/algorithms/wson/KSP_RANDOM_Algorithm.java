package es.tid.pce.computingEngine.algorithms.wson;

import java.net.Inet4Address;
import java.util.LinkedList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.jgrapht.GraphPath;
import org.jgrapht.alg.KShortestPaths;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;

import es.tid.ospf.ospfv2.lsa.tlv.subtlv.complexFields.BitmapLabelSet;
import es.tid.pce.computingEngine.ComputingRequest;
import es.tid.pce.computingEngine.ComputingResponse;
import es.tid.pce.computingEngine.algorithms.AlgorithmReservation;
import es.tid.pce.computingEngine.algorithms.ComputingAlgorithm;
import es.tid.pce.computingEngine.algorithms.PCEPUtils;
import es.tid.pce.pcep.constructs.EndPoint;
import es.tid.pce.pcep.constructs.EndPointAndRestrictions;
import es.tid.pce.pcep.constructs.IPv4AddressEndPoint;
import es.tid.pce.pcep.constructs.Path;
import es.tid.pce.pcep.constructs.Request;
import es.tid.pce.pcep.constructs.Response;
import es.tid.pce.pcep.objects.EndPoints;
import es.tid.pce.pcep.objects.EndPointsIPv4;
import es.tid.pce.pcep.objects.ExplicitRouteObject;
import es.tid.pce.pcep.objects.GeneralizedEndPoints;
import es.tid.pce.pcep.objects.NoPath;
import es.tid.pce.pcep.objects.ObjectParameters;
import es.tid.pce.pcep.objects.P2MPGeneralizedEndPoints;
import es.tid.pce.pcep.objects.P2PGeneralizedEndPoints;
import es.tid.pce.pcep.objects.RequestParameters;
import es.tid.pce.pcep.objects.tlvs.NoPathTLV;
import es.tid.pce.server.wson.ReservationManager;
import es.tid.rsvp.RSVPProtocolViolationException;
import es.tid.rsvp.constructs.gmpls.DWDMWavelengthLabel;
import es.tid.rsvp.objects.subobjects.GeneralizedLabelEROSubobject;
import es.tid.rsvp.objects.subobjects.IPv4prefixEROSubobject;
import es.tid.rsvp.objects.subobjects.UnnumberIfIDEROSubobject;
import es.tid.tedb.DomainTEDB;
import es.tid.tedb.IntraDomainEdge;
import es.tid.tedb.TEDB;
import es.tid.tedb.WSONInformation;

public class KSP_RANDOM_Algorithm implements ComputingAlgorithm {
	private WSONInformation WSONInfo;

	private Logger log=LoggerFactory.getLogger("PCEServer");
	
	private DomainTEDB ted;
	
	private GenericLambdaReservation  reserv;
	
	private ReservationManager reservationManager;
	
	private KSP_RANDOM_AlgorithmPreComputation preComp;
	
	private ComputingRequest pathReq;
	
	public KSP_RANDOM_Algorithm(ComputingRequest pathReq,TEDB ted, ReservationManager reservationManager ){
		//this.num_lambdas=((DomainTEDB)ted).getNumLambdas();
		this.pathReq=pathReq;
		this.reservationManager=reservationManager;
		this.ted=(DomainTEDB)ted;
	}
	
	public ComputingResponse call(){ 
		
		//Time stamp of the start of the algorithm;
		long tiempoini =System.nanoTime();
		
		log.debug("Starting KSPprecomp Algorithm");
		
		//Create the response message
		//It will contain either the path or noPath
		ComputingResponse m_resp=new ComputingResponse();
		m_resp.setEncodingType(pathReq.getEcodingType());
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
		Object source_router_id_addr = null;
		Object dest_router_id_addr = null;

		if (EP.getOT()==ObjectParameters.PCEP_OBJECT_TYPE_ENDPOINTS_IPV4){
			EndPointsIPv4  ep=(EndPointsIPv4) req.getEndPoints();
			source_router_id_addr=ep.getSourceIP();
			dest_router_id_addr=ep.getDestIP();
		}else if (EP.getOT()==ObjectParameters.PCEP_OBJECT_TYPE_ENDPOINTS_IPV6){

		}else if (EP.getOT()==ObjectParameters.PCEP_OBJECT_TYPE_GENERALIZED_ENDPOINTS){
			GeneralizedEndPoints  gep=(GeneralizedEndPoints) req.getEndPoints();
			if(gep.getGeneralizedEndPointsType()==ObjectParameters.PCEP_GENERALIZED_END_POINTS_TYPE_P2P){
				P2PGeneralizedEndPoints p2pep= (P2PGeneralizedEndPoints)gep;		
				source_router_id_addr = ((IPv4AddressEndPoint)p2pep.getSourceEndpoint().getEndPoint()).getEndPointIPv4().getIPv4address();
				dest_router_id_addr = ((IPv4AddressEndPoint)p2pep.getDestinationEndpoint().getEndPoint()).getEndPointIPv4().getIPv4address();
			}
			if(gep.getGeneralizedEndPointsType()==ObjectParameters.PCEP_GENERALIZED_END_POINTS_TYPE_P2MP_NEW_LEAVES){
				P2MPGeneralizedEndPoints p2mpep= (P2MPGeneralizedEndPoints)gep;
				EndPointAndRestrictions epandrest=p2mpep.getEndpointAndRestrictions();
				EndPoint sourceep=epandrest.getEndPoint();
				source_router_id_addr=((IPv4AddressEndPoint)sourceep).getEndPointIPv4().IPv4address;
				int cont=0;
				while (cont<=p2mpep.getEndpointAndRestrictionsList().size()){ //esto est� mal
					epandrest=p2mpep.getEndpointAndRestrictionsList().get(cont);
					EndPoint destep=epandrest.getEndPoint();
					dest_router_id_addr=((IPv4AddressEndPoint)destep).getEndPointIPv4().IPv4address;

				}
			}
		}
				
		//Now, check if the source and destination are in the TED.
		log.debug("Source: "+source_router_id_addr+"; Destination:"+dest_router_id_addr);
		if (!(((ted.containsVertex(source_router_id_addr))&&(ted.containsVertex(dest_router_id_addr))))){
			log.warn("Source or destination are NOT in the TED");	
			NoPath noPath= new NoPath();
			noPath.setNatureOfIssue(ObjectParameters.NOPATH_NOPATH_SAT_CONSTRAINTS);
			NoPathTLV noPathTLV=new NoPathTLV();
			if (!((ted.containsVertex(source_router_id_addr)))){
				log.debug("Unknown source");	
				noPathTLV.setUnknownSource(true);	
			}
			if (!((ted.containsVertex(dest_router_id_addr)))){
				log.debug("Unknown destination");
				noPathTLV.setUnknownDestination(true);	
			}

			noPath.setNoPathTLV(noPathTLV);				
			response.setNoPath(noPath);
			return m_resp;
		}
		
		boolean nopath=false;//Initially, we still have no path
		int lambda_chosen=-1;//We begin with lambda index 0
		GraphPath<Object,IntraDomainEdge> gp_chosen=null;
		boolean noLambda = true;
		SimpleDirectedWeightedGraph<Object, IntraDomainEdge> Graph;
		int k = 1;
		
		
		log.debug("Starting the selection of the path");
		KShortestPaths<Object,IntraDomainEdge> ksp = new KShortestPaths<Object,IntraDomainEdge> (preComp.getNetworkGraph(), source_router_id_addr, k);
		List<GraphPath<Object,IntraDomainEdge>> routeList = ksp.getPaths(dest_router_id_addr); //list of the path edges
	
		if (routeList.isEmpty()==true){
			nopath = true;
			log.info("No path found");
			NoPath noPath= new NoPath();
			noPath.setNatureOfIssue(ObjectParameters.NOPATH_NOPATH_SAT_CONSTRAINTS);
			NoPathTLV noPathTLV=new NoPathTLV();
			noPath.setNoPathTLV(noPathTLV);				
			response.setNoPath(noPath);
			return m_resp;
			
		}
		
		int num_lambdas = ted.getWSONinfo().getNumLambdas();
				
		int j1 =0;
		if (nopath==false){
							
			for (j1=0; j1<routeList.size(); j1++)
			{
				gp_chosen = routeList.get(j1);
								
				Path path=new Path();
				ExplicitRouteObject ero= new ExplicitRouteObject();
				List<IntraDomainEdge> edge_list=gp_chosen.getEdgeList();
				
				//set of available lambdas
				int max_lambdas=((BitmapLabelSet)edge_list.get(0).getTE_info().getAvailableLabels().getLabelSet()).getNumLabels();
				int num_bytes=((BitmapLabelSet)edge_list.get(0).getTE_info().getAvailableLabels().getLabelSet()).getBytesBitMap().length;
				byte [] bitmap=new byte[num_bytes];
				
				for (int i=0;i<num_bytes;++i){
					bitmap[i]=(byte)(((BitmapLabelSet)edge_list.get(0).getTE_info().getAvailableLabels().getLabelSet()).getBytesBitMap()[i]|((BitmapLabelSet)edge_list.get(0).getTE_info().getAvailableLabels().getLabelSet()).getBytesBitmapReserved()[i]);	
				}
				
				for (int j11=1;j11<edge_list.size();++j11){
					for (int i=0;i<num_bytes;++i){
						bitmap[i]=(byte)(bitmap[i] | (((BitmapLabelSet)edge_list.get(j11).getTE_info().getAvailableLabels().getLabelSet()).getBytesBitMap()[i]|((BitmapLabelSet)edge_list.get(j11).getTE_info().getAvailableLabels().getLabelSet()).getBytesBitmapReserved()[i]));	
					}
				}
				//System.out.println("lambdas disponibles:");
				int[] lista= new int[max_lambdas];
				int k1=0;
				for (int i=0; i<max_lambdas;++i){
					int num_byte=i/8;
					if ((bitmap[num_byte]&(0x80>>>(i%8)))==0){
						lista[k1] = i; // list of lambdas available in the whole path
						k1++;
					}			
				}
				int semilla_aleatoria = (int) Math.floor(Math.random()*k1);
				
				lambda_chosen = lista[semilla_aleatoria];
				
				
				if (lista.equals(null))
				{
					noLambda = true;
					continue;
				}
								
				noLambda = false;
							
				//in case there is a lambda free for path
				
				int i1;
				for (i1=0;i1<edge_list.size();i1++){
					UnnumberIfIDEROSubobject eroso= new UnnumberIfIDEROSubobject();
					eroso.setRouterID((Inet4Address)edge_list.get(i1).getSource());
					eroso.setInterfaceID(edge_list.get(i1).getSrc_if_id());
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
				path.setEro(ero);
				PCEPUtils.completeMetric(path, req, edge_list);
				response.addPath(path);
	
				//FIXME: RESERVATION NEEDS TO BE IMPROVED!!!
				LinkedList<Object> sourceVertexList=new LinkedList<Object>();
				LinkedList<Object> targetVertexList=new LinkedList<Object>();
				for (i1=0;i1<edge_list.size();i1++){
					sourceVertexList.add(edge_list.get(i1).getSource());
					targetVertexList.add(edge_list.get(i1).getTarget());
				}	
				sourceVertexList.add(edge_list.get(i1-1).getSource());
				targetVertexList.add(edge_list.get(i1-1).getTarget());
				
				
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
				break;
			}
		}
		long tiempofin =System.nanoTime();
		long tiempotot=tiempofin-tiempoini;
		log.info("Ha tardado "+tiempotot+" nanosegundos");
		
		if (noLambda == true){
			log.debug("No path found");
			NoPath noPath= new NoPath();
			noPath.setNatureOfIssue(ObjectParameters.NOPATH_NOPATH_SAT_CONSTRAINTS);
			NoPathTLV noPathTLV=new NoPathTLV();
			noPath.setNoPathTLV(noPathTLV);				
			response.setNoPath(noPath);
			return m_resp;
		}
		return m_resp;
	}
	
	public void setPreComp(KSP_RANDOM_AlgorithmPreComputation preComp) {
		this.preComp = preComp;
	}
	
	public AlgorithmReservation getReserv() {
		return reserv;
	}

	
}
