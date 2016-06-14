package es.tid.pce.computingEngine.algorithms;

import java.net.Inet4Address;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.jgrapht.GraphPath;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.graph.DirectedWeightedMultigraph;

import es.tid.pce.computingEngine.ComputingRequest;
import es.tid.pce.computingEngine.ComputingResponse;
import es.tid.pce.computingEngine.algorithms.multidomain.MDFunctions;
import es.tid.pce.parentPCE.ChildPCERequestManager;
import es.tid.pce.parentPCE.ParentPCESession;
import es.tid.pce.pcep.constructs.EndPoint;
import es.tid.pce.pcep.constructs.EndPointAndRestrictions;
import es.tid.pce.pcep.constructs.GeneralizedBandwidth;
import es.tid.pce.pcep.constructs.GeneralizedBandwidthSSON;
import es.tid.pce.pcep.constructs.NCF;
import es.tid.pce.pcep.constructs.P2MPEndpoints;
import es.tid.pce.pcep.constructs.P2PEndpoints;
import es.tid.pce.pcep.constructs.PCEPIntiatedLSP;
import es.tid.pce.pcep.constructs.Path;
import es.tid.pce.pcep.constructs.Request;
import es.tid.pce.pcep.constructs.Response;
import es.tid.pce.pcep.constructs.StateReport;
import es.tid.pce.pcep.messages.PCEPInitiate;
import es.tid.pce.pcep.messages.PCEPMessageTypes;
import es.tid.pce.pcep.messages.PCEPReport;
import es.tid.pce.pcep.messages.PCEPRequest;
import es.tid.pce.pcep.objects.Bandwidth;
import es.tid.pce.pcep.objects.BandwidthRequested;
import es.tid.pce.pcep.objects.BandwidthRequestedGeneralizedBandwidth;
import es.tid.pce.pcep.objects.BitmapLabelSet;
import es.tid.pce.pcep.objects.EndPoints;
import es.tid.pce.pcep.objects.EndPointsIPv4;
import es.tid.pce.pcep.objects.ExcludeRouteObject;
import es.tid.pce.pcep.objects.ExplicitRouteObject;
import es.tid.pce.pcep.objects.GeneralizedEndPoints;
import es.tid.pce.pcep.objects.LSP;
import es.tid.pce.pcep.objects.LabelSetInclusiveList;
import es.tid.pce.pcep.objects.Monitoring;
import es.tid.pce.pcep.objects.NoPath;
import es.tid.pce.pcep.objects.ObjectParameters;
import es.tid.pce.pcep.objects.ObjectiveFunction;
import es.tid.pce.pcep.objects.RequestParameters;
import es.tid.pce.pcep.objects.SRP;
import es.tid.pce.pcep.objects.subobjects.UnnumberIfIDXROSubobject;
import es.tid.pce.pcep.objects.subobjects.XROSubObjectValues;
import es.tid.pce.pcep.objects.subobjects.XROSubobject;
import es.tid.pce.pcep.objects.tlvs.EndPointIPv4TLV;
import es.tid.pce.pcep.objects.tlvs.NoPathTLV;
import es.tid.pce.pcep.objects.tlvs.SymbolicPathNameTLV;
import es.tid.pce.pcep.objects.tlvs.UnnumberedEndpointTLV;
import es.tid.pce.pcep.objects.tlvs.subtlvs.SymbolicPathNameSubTLV;
import es.tid.rsvp.constructs.gmpls.DWDMWavelengthLabel;
import es.tid.rsvp.objects.subobjects.EROSubobject;
import es.tid.rsvp.objects.subobjects.GeneralizedLabelEROSubobject;
import es.tid.rsvp.objects.subobjects.IPv4prefixEROSubobject;
import es.tid.rsvp.objects.subobjects.LabelEROSubobject;
import es.tid.rsvp.objects.subobjects.UnnumberIfIDEROSubobject;
import es.tid.tedb.DomainTEDB;
import es.tid.tedb.ITMDTEDB;
import es.tid.tedb.InterDomainEdge;
import es.tid.tedb.MDTEDB;
import es.tid.tedb.ReachabilityManager;
import es.tid.tedb.TEDB;

/**
 * Algorithm to Minimize the number of Transit Domains (MTD)
 * it is specified in 
 * @author ogondio
 *
 */
public class MDHPCEMinNumberDomainsAlgorithm implements ComputingAlgorithm{
	private DirectedWeightedMultigraph<Object,InterDomainEdge> networkGraph;
	private Logger log=LoggerFactory.getLogger("PCEServer");
	private ComputingRequest pathReq;
	private ChildPCERequestManager childPCERequestManager;
	private ReachabilityManager reachabilityManager;

	public static int GENERIC_CHANNEL = 0;
	public static int MEDIA_CHANNEL = 1;
	public static int OF_CODE_MUTIDOMAIN_MEDIA_CHANNEL = 60000;
	public static int OF_CODE_MUTIDOMAIN_SBVT_CHANNEL = 60001;
	public static int OF_CODE_MEDIA_CHANNEL = 58000;
	public static int SBVT_CHANNEL = 2;
	public static int OF_CODE_SBVT_CHANNEL = 58020;

	public int channelType;
	
	public long reqId;
	
	boolean explicit_label = false;

	EndPoints  original_end_points; // Original EndPoints of the Request

	public MDHPCEMinNumberDomainsAlgorithm(ComputingRequest pathReq,TEDB ted,ChildPCERequestManager cprm , ReachabilityManager rm){
		if(ted.isITtedb()){
			this.networkGraph=((ITMDTEDB)ted).getDuplicatedMDNetworkGraph();
		}else{
			this.networkGraph=((MDTEDB)ted).getDuplicatedMDNetworkGraph();
		}
		this.reachabilityManager=rm;
		this.pathReq=pathReq;		
		this.childPCERequestManager=cprm;
	}



	public ComputingResponse call() throws Exception{
		long tiempoini =System.nanoTime(); // To measure the time, 
		ComputingResponse m_resp=new ComputingResponse(); //Create the response of the computation.
		m_resp.setReachabilityManager(reachabilityManager);
		//FIXME: Remove ASAP
		m_resp.setEncodingType(pathReq.getEcodingType());
		int m = 0;

		Request req=pathReq.getRequestList().get(0); // Get the original request
		this.reqId=req.getRequestParameters().getRequestID(); //Get the request ID.
		log.info("Processing MD Path Computing with MDHPCEMinNumberDomainsAlgorithm (Minimum transit Domains) with Request id: "+reqId);

		//Start creating the response
		//We create it now, in case we need to send a NoPath later
		Response response=new Response();
		RequestParameters rp = new RequestParameters();
		rp.setRequestID(reqId); // We answer with the same request ID.
		response.setRequestParameters(rp);

		original_end_points= req.getEndPoints(); //Get the original end points. 
		Inet4Address source_router_id_addr = getSourceRouter(original_end_points); //FIXME: change to object
		long src_if_id=getSourceIfID(original_end_points);
		Inet4Address dest_router_id_addr = getDestRouter(original_end_points); //FIXME: change to object
		long dst_if_id=getDestIfID(original_end_points);

		//First, we obtain the domains of each endPoint
		Inet4Address source_domain_id=this.reachabilityManager.getDomain(source_router_id_addr);
		Inet4Address dest_domain_id=this.reachabilityManager.getDomain(dest_router_id_addr);
		log.info("MD Request from "+source_router_id_addr+" (domain "+source_domain_id+") to "+ dest_router_id_addr+" (domain "+dest_domain_id+")");

		//CHECK IF DOMAIN_ID ARE NULL!!!!!!
		log.debug("Check if SRC and Dest domains are OK");
		if ((dest_domain_id==null)||(source_domain_id==null)){
			//ONE OF THEM IS NOT REACHABLE, SEND NOPATH!!!
			log.warn("One of the domains is not reachable, sending NOPATH");
			NoPath noPath= new NoPath();
			noPath.setNatureOfIssue(ObjectParameters.NOPATH_NOPATH_SAT_CONSTRAINTS);
			response.setNoPath(noPath);
			m_resp.addResponse(response);
			return m_resp;
		}
		if (!((networkGraph.containsVertex(source_domain_id))&&(networkGraph.containsVertex(dest_domain_id)))){
			Iterator<Object> it = networkGraph.vertexSet().iterator();
			while (it.hasNext()){
				log.info(it.next().toString());
			}
			log.warn("Source or destination domains are NOT in the TED");
			//FIXME: VER ESTE CASO
			NoPath noPath= new NoPath();
			noPath.setNatureOfIssue(ObjectParameters.NOPATH_NOPATH_SAT_CONSTRAINTS);
			NoPathTLV noPathTLV=new NoPathTLV();
			if (!((networkGraph.containsVertex(source_router_id_addr)))){
				log.debug("Unknown source domain");	
				noPathTLV.setUnknownSource(true);	
			}
			if (!((networkGraph.containsVertex(dest_router_id_addr)))){
				log.debug("Unknown destination domain");
				noPathTLV.setUnknownDestination(true);	
			}

			noPath.setNoPathTLV(noPathTLV);				
			response.setNoPath(noPath);
			m_resp.addResponse(response);
			return m_resp;
		}
		
		int of=-1;
		if(req.getObjectiveFunction()!=null){
			of=req.getObjectiveFunction().getOFcode();
			if (of==MDHPCEMinNumberDomainsAlgorithm.OF_CODE_MUTIDOMAIN_MEDIA_CHANNEL){
				channelType=MDHPCEMinNumberDomainsAlgorithm.MEDIA_CHANNEL;
				log.info("We are dealing with a MEDIA CHANNEL");
			}else if (of==MDHPCEMinNumberDomainsAlgorithm.OF_CODE_MUTIDOMAIN_SBVT_CHANNEL){
				channelType=MDHPCEMinNumberDomainsAlgorithm.SBVT_CHANNEL;
				log.info("We are dealing with a SBVT CHANNEL");
			}else {
				channelType=MDHPCEMinNumberDomainsAlgorithm.GENERIC_CHANNEL;
				log.info("We are dealing with a GENERIC CHANNEL");
			}
		}else {
			channelType=MDHPCEMinNumberDomainsAlgorithm.GENERIC_CHANNEL;
			log.info("We are dealing with a GENERIC CHANNEL AS NO OF WAS RECEIVED");
		}

		
		//Prune the graph if needed
		if (channelType==MDHPCEMinNumberDomainsAlgorithm.MEDIA_CHANNEL) {
			pruneSVBTs(this.networkGraph);
		}

		//Now, compute the shortest sequence of domains
		log.info("Processing XRO");
		//processXRO(req.getXro(),networkGraph);
		MDFunctions.processXRO(req.getXro(),reachabilityManager, networkGraph);

		log.info("Computing MD Sequence of domains");
		DijkstraShortestPath<Object,InterDomainEdge>  dsp=new DijkstraShortestPath<Object,InterDomainEdge> (networkGraph, source_domain_id, dest_domain_id);	
		LinkedList<PCEPRequest> reqList= new LinkedList<PCEPRequest>();
		LinkedList<Object> domainList= new LinkedList<Object>();

		GraphPath<Object,InterDomainEdge> gp=dsp.getPath();
		if (gp==null){
			log.error("Problem getting the domain sequence");
			NoPath noPath2= new NoPath();
			noPath2.setNatureOfIssue(ObjectParameters.NOPATH_NOPATH_SAT_CONSTRAINTS);
			NoPathTLV noPathTLV=new NoPathTLV();
			noPath2.setNoPathTLV(noPathTLV);				
			response.setNoPath(noPath2);
			m_resp.addResponse(response);
			return m_resp;
		}
		List<InterDomainEdge> edge_list=gp.getEdgeList();


		//Let's check the OF code and get the channel type


	
		boolean first_domain_equal=false;
		if (source_domain_id.equals(dest_domain_id)){
			PCEPRequest pcreqToDomain=createRequest(this.channelType, req.getXro(), pathReq.getRequestList().get(0).getBandwidth().duplicate(),pathReq.getRequestList().get(0).getEndPoints());

			reqList.add(pcreqToDomain);
			domainList.add(source_domain_id);
			log.info("Sending ONLY ONE request: "+pcreqToDomain.toString()+" to domain "+source_domain_id);

		} else {
			int i=0;
			/////////////////////////////////////////////////////////
			//Create request for the FIRST domain involved
			//////////////////////////////////////////////////////////
			Inet4Address destIP = null;
			EndPoints endpointsRequest = null;
			if (original_end_points.getOT()==ObjectParameters.PCEP_OBJECT_TYPE_ENDPOINTS_IPV4){			
				endpointsRequest = new EndPointsIPv4();
				((EndPointsIPv4) endpointsRequest).setSourceIP(source_router_id_addr);
				destIP = (Inet4Address)edge_list.get(0).getSrc_router_id();
				((EndPointsIPv4) endpointsRequest).setDestIP(destIP);

			}else if (original_end_points.getOT()==ObjectParameters.PCEP_OBJECT_TYPE_ENDPOINTS_IPV6){
				//NO IMPLEMENTADO
			}

			if (original_end_points.getOT()==ObjectParameters.PCEP_OBJECT_TYPE_GENERALIZED_ENDPOINTS){
				GeneralizedEndPoints  gep=(GeneralizedEndPoints) req.getEndPoints();
				EndPoint sourceEP=new EndPoint();
				EndPoint destEP=new EndPoint();
				if(gep.getGeneralizedEndPointsType()==ObjectParameters.PCEP_GENERALIZED_END_POINTS_TYPE_P2P){
					if (gep.getP2PEndpoints().getSourceEndPoint().getEndPointIPv4TLV()!=null){
						source_router_id_addr=  gep.getP2PEndpoints().getSourceEndPoint().getEndPointIPv4TLV().getIPv4address();
						EndPointIPv4TLV sourceIPv4TLV = new EndPointIPv4TLV();
						EndPointIPv4TLV destIPv4TLV = new EndPointIPv4TLV();
						sourceIPv4TLV.setIPv4address(source_router_id_addr);
						destIP=(Inet4Address)edge_list.get(0).getSrc_router_id();
						destIPv4TLV.setIPv4address(destIP);
						sourceEP.setEndPointIPv4TLV(sourceIPv4TLV);
						destEP.setEndPointIPv4TLV(destIPv4TLV);
					}else if (gep.getP2PEndpoints().getSourceEndPoint().getUnnumberedEndpoint()!=null){
						source_router_id_addr=  gep.getP2PEndpoints().getSourceEndPoint().getUnnumberedEndpoint().getIPv4address();
						UnnumberedEndpointTLV sourceIPv4TLV = new UnnumberedEndpointTLV();
						UnnumberedEndpointTLV destIPv4TLV = new UnnumberedEndpointTLV();
						sourceIPv4TLV.setIPv4address(source_router_id_addr);
						destIP=(Inet4Address)edge_list.get(0).getSrc_router_id();
						sourceIPv4TLV.setIfID(gep.getP2PEndpoints().getSourceEndPoint().getUnnumberedEndpoint().getIfID());
						destIPv4TLV.setIPv4address(destIP);
						destIPv4TLV.setIfID(edge_list.get(0).getSrc_if_id());
						sourceEP.setUnnumberedEndpoint(sourceIPv4TLV);
						destEP.setUnnumberedEndpoint(destIPv4TLV);
					}					
					P2PEndpoints p2pep=new P2PEndpoints();
					p2pep.setSourceEndpoint(sourceEP);
					p2pep.setDestinationEndPoints(destEP);

					endpointsRequest = new GeneralizedEndPoints();
					((GeneralizedEndPoints) endpointsRequest).setP2PEndpoints(p2pep);

				}

			}

			Inet4Address domain =(Inet4Address)edge_list.get(0).getSource();
			log.info("First part of the LSP is in domain: "+ domain+" from "+ source_router_id_addr+" to "+destIP);
			//FIXME: METRICA? OF? BW?
			long requestID;
			
			

			if (source_router_id_addr.equals(destIP)){
				log.info("Origin and destination are the same");
				first_domain_equal=true;
			}
			else {
				PCEPRequest pcreqToFirstDomain=new PCEPRequest();
				if (pathReq.getMonitoring()!=null){
					pcreqToFirstDomain.setMonitoring(pathReq.getMonitoring());
				}
				if (pathReq.getPccReqId()!=null){
					pcreqToFirstDomain.setPccReqId(pathReq.getPccReqId());
				}
				Request requestToFirstDomain=new Request();
				log.info("Llega BW "+pathReq.getRequestList().get(0).getBandwidth().toString());
				
								
				Request req1=pathReq.getRequestList().get(0);
				BandwidthRequestedGeneralizedBandwidth  bw =null;
				
				if (req1.getBandwidth() instanceof BandwidthRequestedGeneralizedBandwidth){
					bw= (BandwidthRequestedGeneralizedBandwidth)req1.getBandwidth(); 
					
					if(bw.getGeneralizedBandwidth()!= null){
						
						if(bw.getGeneralizedBandwidth() instanceof GeneralizedBandwidthSSON ){
						
						GeneralizedBandwidthSSON a = (GeneralizedBandwidthSSON)bw.getGeneralizedBandwidth();
						m=a.getM();
						
						}
							
					}
				}
				
				
				//requestToFirstDomain.setObjectiveFunction(pathReq.getRequestList().get(0).getObjectiveFunction());
				requestToFirstDomain.setBandwidth(pathReq.getRequestList().get(0).getBandwidth().duplicate());
				if(pathReq.getRequestList().get(0).getReservation()!=null){
					requestToFirstDomain.setReservation(pathReq.getRequestList().get(0).getReservation());
				}
				addXRO(req.getXro(),requestToFirstDomain);
				requestToFirstDomain.setEndPoints(endpointsRequest);
				RequestParameters rpFirstDomain=new RequestParameters();
				requestID=ParentPCESession.getNewReqIDCounter();
				rpFirstDomain.setRequestID(requestID);
				rpFirstDomain.setPbit(true);
				requestToFirstDomain.setRequestParameters(rpFirstDomain);
				if (channelType==MDHPCEMinNumberDomainsAlgorithm.MEDIA_CHANNEL){
					ObjectiveFunction objectiveFunction = new ObjectiveFunction();
					objectiveFunction.setOFcode(MDHPCEMinNumberDomainsAlgorithm.OF_CODE_MEDIA_CHANNEL);
					requestToFirstDomain.setObjectiveFunction(objectiveFunction);
				}else if (channelType==MDHPCEMinNumberDomainsAlgorithm.SBVT_CHANNEL){
					ObjectiveFunction objectiveFunction = new ObjectiveFunction();
					objectiveFunction.setOFcode(MDHPCEMinNumberDomainsAlgorithm.OF_CODE_SBVT_CHANNEL);
					requestToFirstDomain.setObjectiveFunction(objectiveFunction);
				}
				pcreqToFirstDomain.addRequest(requestToFirstDomain);
				reqList.add(pcreqToFirstDomain);
				domainList.add(domain);
				log.info("Sending 1st request"+requestToFirstDomain.toString()+" to domain "+domain);
			}
			for (i=1;i<edge_list.size();++i){

				domain =(Inet4Address)edge_list.get(i).getSource();

				if (original_end_points.getOT()==ObjectParameters.PCEP_OBJECT_TYPE_ENDPOINTS_IPV4){
					endpointsRequest=new EndPointsIPv4();
					((EndPointsIPv4)endpointsRequest).setSourceIP((Inet4Address)edge_list.get(i-1).getDst_router_id());
					((EndPointsIPv4)endpointsRequest).setDestIP((Inet4Address)edge_list.get(i).getSrc_router_id());
				}else if (original_end_points.getOT()==ObjectParameters.PCEP_OBJECT_TYPE_ENDPOINTS_IPV6){

				}

				if (original_end_points.getOT()==ObjectParameters.PCEP_OBJECT_TYPE_GENERALIZED_ENDPOINTS){
					GeneralizedEndPoints  gep=(GeneralizedEndPoints) req.getEndPoints();
					if(gep.getGeneralizedEndPointsType()==ObjectParameters.PCEP_GENERALIZED_END_POINTS_TYPE_P2P){
						EndPoint sourceEP=new EndPoint();
						EndPoint destEP=new EndPoint();
						if (gep.getP2PEndpoints().getSourceEndPoint().getEndPointIPv4TLV()!=null){
							EndPointIPv4TLV sourceIPv4TLV = new EndPointIPv4TLV();
							EndPointIPv4TLV destIPv4TLV = new EndPointIPv4TLV();
							sourceIPv4TLV.setIPv4address((Inet4Address)edge_list.get(i-1).getDst_router_id());
							destIP=(Inet4Address)edge_list.get(i).getSrc_router_id();
							destIPv4TLV.setIPv4address(destIP);
							sourceEP.setEndPointIPv4TLV(sourceIPv4TLV);
							destEP.setEndPointIPv4TLV(destIPv4TLV);
						}else if (gep.getP2PEndpoints().getSourceEndPoint().getUnnumberedEndpoint()!=null){

							UnnumberedEndpointTLV sourceIPv4TLV = new UnnumberedEndpointTLV();
							UnnumberedEndpointTLV destIPv4TLV = new UnnumberedEndpointTLV();
							sourceIPv4TLV.setIPv4address((Inet4Address)edge_list.get(i-1).getDst_router_id());
							destIP=(Inet4Address)edge_list.get(i).getSrc_router_id();
							sourceIPv4TLV.setIfID(edge_list.get(i-1).getDst_if_id());
							destIPv4TLV.setIPv4address(destIP);
							destIPv4TLV.setIfID(edge_list.get(i).getSrc_if_id());
							sourceEP.setUnnumberedEndpoint(sourceIPv4TLV);
							destEP.setUnnumberedEndpoint(destIPv4TLV);
						}					

						P2PEndpoints p2pep=new P2PEndpoints();
						p2pep.setSourceEndpoint(sourceEP);
						p2pep.setDestinationEndPoints(destEP);

						endpointsRequest = new GeneralizedEndPoints();
						((GeneralizedEndPoints) endpointsRequest).setP2PEndpoints(p2pep);

					}
					if(gep.getGeneralizedEndPointsType()==ObjectParameters.PCEP_GENERALIZED_END_POINTS_TYPE_P2MP_NEW_LEAVES){
						//POR HACER
						//					P2MPEndpoints p2mpep= gep.getP2MPEndpoints();
						//					EndPointAndRestrictions epandrest=p2mpep.getEndPointAndRestrictions();
						//					EndPoint sourceep=epandrest.getEndPoint();
						//					source_router_id_addr=sourceep.getEndPointIPv4TLV().IPv4address;
						//					int cont=0;
						//					while (cont<=p2mpep.getEndPointAndRestrictionsList().size()){ //esto est� mal
						//						epandrest=p2mpep.getEndPointAndRestrictionsList().get(cont);
						//						EndPoint destep=epandrest.getEndPoint();
						//						source_router_id_addr=sourceep.getEndPointIPv4TLV().IPv4address;
						//						dest_router_id_addr=destep.getEndPointIPv4TLV().IPv4address;
						//
						//					}
					}
				}

				log.info("New part of the LSP is in domain: "+ domain+" from "+ edge_list.get(i-1).getDst_router_id()+" to "+edge_list.get(i).getSrc_router_id());
				PCEPRequest pcreq=new PCEPRequest();

				if (pathReq.getMonitoring()!=null){
					pcreq.setMonitoring(pathReq.getMonitoring());
				}
				if (pathReq.getPccReqId()!=null){
					pcreq.setPccReqId(pathReq.getPccReqId());
				}

				Request request=new Request();
				//request.setObjectiveFunction(pathReq.getRequestList().get(0).getObjectiveFunction());
				request.setBandwidth(pathReq.getRequestList().get(0).getBandwidth());
				if(pathReq.getRequestList().get(0).getReservation()!=null){
					request.setReservation(pathReq.getRequestList().get(0).getReservation());
				}
				addXRO(req.getXro(),request);
				request.setEndPoints(endpointsRequest);
				RequestParameters rp2=new RequestParameters();
				requestID=ParentPCESession.getNewReqIDCounter();
				rp2.setRequestID(requestID);
				rp2.setPbit(true);
				request.setRequestParameters(rp2);
				if (channelType==MDHPCEMinNumberDomainsAlgorithm.MEDIA_CHANNEL){
					ObjectiveFunction objectiveFunction = new ObjectiveFunction();
					objectiveFunction.setOFcode(MDHPCEMinNumberDomainsAlgorithm.OF_CODE_MEDIA_CHANNEL);
					request.setObjectiveFunction(objectiveFunction);
				}else if (channelType==MDHPCEMinNumberDomainsAlgorithm.SBVT_CHANNEL){
					ObjectiveFunction objectiveFunction = new ObjectiveFunction();
					objectiveFunction.setOFcode(MDHPCEMinNumberDomainsAlgorithm.OF_CODE_SBVT_CHANNEL);
					request.setObjectiveFunction(objectiveFunction);
				}
				pcreq.addRequest(request);
				reqList.add(pcreq);
				domainList.add(domain);
				log.info("Sending request "+i+ " to domain "+domain);
			}
			//Create request for last domain
			EndPoints endpointsLastDomain=null;
			Inet4Address Last_domain =(Inet4Address)edge_list.get(i-1).getTarget();
			Inet4Address last_source_IP=(Inet4Address)edge_list.get(i-1).getDst_router_id();
			log.info("Last part of the LSP is in domain: "+ Last_domain+" from "+ last_source_IP+" to "+dest_router_id_addr);

			if (original_end_points.getOT()==ObjectParameters.PCEP_OBJECT_TYPE_ENDPOINTS_IPV4){
				endpointsLastDomain = new EndPointsIPv4();			
				((EndPointsIPv4)endpointsLastDomain).setDestIP(dest_router_id_addr);
				//FIXME: PONGO EL IF NO NUMERADO????
				((EndPointsIPv4)endpointsLastDomain).setSourceIP(last_source_IP);
				//FIXME: METRICA? OF? BW?

			}else if (original_end_points.getOT()==ObjectParameters.PCEP_OBJECT_TYPE_ENDPOINTS_IPV6){

			}

			if (original_end_points.getOT()==ObjectParameters.PCEP_OBJECT_TYPE_GENERALIZED_ENDPOINTS){
				GeneralizedEndPoints  gep=(GeneralizedEndPoints) req.getEndPoints();
				if(gep.getGeneralizedEndPointsType()==ObjectParameters.PCEP_GENERALIZED_END_POINTS_TYPE_P2P){
					EndPoint sourceEP=new EndPoint();
					EndPoint destEP=new EndPoint();
					if (gep.getP2PEndpoints().getSourceEndPoint().getEndPointIPv4TLV()!=null){
						EndPointIPv4TLV sourceIPv4TLV = new EndPointIPv4TLV();
						EndPointIPv4TLV destIPv4TLV = new EndPointIPv4TLV();
						sourceIPv4TLV.setIPv4address(last_source_IP);
						destIPv4TLV.setIPv4address(dest_router_id_addr);
						sourceEP.setEndPointIPv4TLV(sourceIPv4TLV);
						destEP.setEndPointIPv4TLV(destIPv4TLV);
					}else if (gep.getP2PEndpoints().getSourceEndPoint().getUnnumberedEndpoint()!=null){

						UnnumberedEndpointTLV sourceIPv4TLV = new UnnumberedEndpointTLV();
						UnnumberedEndpointTLV destIPv4TLV = new UnnumberedEndpointTLV();
						sourceIPv4TLV.setIPv4address(last_source_IP);
						sourceIPv4TLV.setIfID(edge_list.get(i-1).getDst_if_id());
						destIPv4TLV.setIPv4address(dest_router_id_addr);
						destIPv4TLV.setIfID(dst_if_id);
						sourceEP.setUnnumberedEndpoint(sourceIPv4TLV);
						destEP.setUnnumberedEndpoint(destIPv4TLV);
					}					

					P2PEndpoints p2pep=new P2PEndpoints();
					p2pep.setSourceEndpoint(sourceEP);
					p2pep.setDestinationEndPoints(destEP);

					endpointsLastDomain = new GeneralizedEndPoints();
					((GeneralizedEndPoints) endpointsLastDomain).setP2PEndpoints(p2pep);

				}
				if(gep.getGeneralizedEndPointsType()==ObjectParameters.PCEP_GENERALIZED_END_POINTS_TYPE_P2MP_NEW_LEAVES){
					//POR HACER
					//				P2MPEndpoints p2mpep= gep.getP2MPEndpoints();
					//				EndPointAndRestrictions epandrest=p2mpep.getEndPointAndRestrictions();
					//				EndPoint sourceep=epandrest.getEndPoint();
					//				source_router_id_addr=sourceep.getEndPointIPv4TLV().IPv4address;
					//				int cont=0;
					//				while (cont<=p2mpep.getEndPointAndRestrictionsList().size()){ //esto est� mal
					//					epandrest=p2mpep.getEndPointAndRestrictionsList().get(cont);
					//					EndPoint destep=epandrest.getEndPoint();
					//					source_router_id_addr=sourceep.getEndPointIPv4TLV().IPv4address;
					//					dest_router_id_addr=destep.getEndPointIPv4TLV().IPv4address;
					//
					//				}
				}
			}


			PCEPRequest pcreqToLastDomain=new PCEPRequest();
			if (pathReq.getMonitoring()!=null){
				pcreqToLastDomain.setMonitoring(pathReq.getMonitoring());
			}
			if (pathReq.getPccReqId()!=null){
				pcreqToLastDomain.setPccReqId(pathReq.getPccReqId());
			}
			Request requestToLastDomain=new Request();
			//requestToLastDomain.setObjectiveFunction(pathReq.getRequestList().get(0).getObjectiveFunction());
			requestToLastDomain.setBandwidth(pathReq.getRequestList().get(0).getBandwidth());
			if(pathReq.getRequestList().get(0).getReservation()!=null){
				log.info("Objeto reservation: "+pathReq.getRequestList().get(0).getReservation());
				requestToLastDomain.setReservation(pathReq.getRequestList().get(0).getReservation());
				log.info("Objeto reservation al hijo: "+requestToLastDomain.getReservation());
			}
			addXRO(req.getXro(),requestToLastDomain);
			requestToLastDomain.setEndPoints(endpointsLastDomain);
			RequestParameters rpLastDomain=new RequestParameters();
			requestID=ParentPCESession.getNewReqIDCounter();
			rpLastDomain.setRequestID(requestID);
			rpLastDomain.setPbit(true);
			requestToLastDomain.setRequestParameters(rpLastDomain);
			if (channelType==MDHPCEMinNumberDomainsAlgorithm.MEDIA_CHANNEL){
				ObjectiveFunction objectiveFunction = new ObjectiveFunction();
				objectiveFunction.setOFcode(MDHPCEMinNumberDomainsAlgorithm.OF_CODE_MEDIA_CHANNEL);
				requestToLastDomain.setObjectiveFunction(objectiveFunction);
			}else if (channelType==MDHPCEMinNumberDomainsAlgorithm.SBVT_CHANNEL){
				ObjectiveFunction objectiveFunction = new ObjectiveFunction();
				objectiveFunction.setOFcode(MDHPCEMinNumberDomainsAlgorithm.OF_CODE_SBVT_CHANNEL);
				requestToLastDomain.setObjectiveFunction(objectiveFunction);
			}
			pcreqToLastDomain.addRequest(requestToLastDomain);

			//Send the last request
			//cpr=new ChildPCERequest(childPCERequestManager, pcreqToLastDomain, Last_domain);
			//ft=new FutureTask<PCEPResponse>(cpr);
			//requestsToChildrenList.add(ft);
			log.info("Sending last request to domain "+edge_list.get(i-1));
			//ft.run();
			//childPCERequestManager.addRequest(pcreqToLastDomain, Last_domain);
			reqList.add(pcreqToLastDomain);
			domainList.add(Last_domain);
		}
		LinkedList <ComputingResponse> respList;
		long tiempo3 =System.nanoTime();
		try {
			respList=childPCERequestManager.executeRequests(reqList, domainList);	
		}catch (Exception e){
			log.error("PROBLEM SENDING THE REQUESTS");
			NoPath noPath2= new NoPath();
			noPath2.setNatureOfIssue(ObjectParameters.NOPATH_NOPATH_SAT_CONSTRAINTS);
			NoPathTLV noPathTLV=new NoPathTLV();
			noPath2.setNoPathTLV(noPathTLV);				
			response.setNoPath(noPath2);
			m_resp.addResponse(response);
			return m_resp;
		}



		m_resp.addResponse(response);
		Path path=new Path();
		ExplicitRouteObject ero= new ExplicitRouteObject();
		int j=0;//Count the interDomain links
		if (first_domain_equal==true){
			if (this.channelType!=MDHPCEMinNumberDomainsAlgorithm.SBVT_CHANNEL) {
				IPv4prefixEROSubobject sobjt4=new IPv4prefixEROSubobject();
				sobjt4.setIpv4address(source_router_id_addr);
				sobjt4.setPrefix(32);
				ero.addEROSubobject(sobjt4);
				UnnumberIfIDEROSubobject idLink = new UnnumberIfIDEROSubobject();
				idLink.setInterfaceID(edge_list.get(0).getSrc_if_id());
				idLink.setRouterID((Inet4Address)edge_list.get(0).getSrc_router_id());
				ero.addEROSubobject(idLink);
				j+=1;
			}
			
		}
		boolean childrenFailed=false;
		int i;

		boolean use_elc=true;
		boolean label_continuity=false;
		boolean remove_elcs=true;

		boolean first=false;
		boolean last=false;
		boolean removeFirstNode=true;
		//LinkedList<NCF> ncflist=new LinkedList<NCF>();
		byte[] bitmap=null;
		DWDMWavelengthLabel label=null;
		DWDMWavelengthLabel label2=null;
		boolean no_lambda = false;
		LinkedList<ExplicitRouteObject> eroList =new LinkedList<ExplicitRouteObject>();
		LinkedList<ExplicitRouteObject> eroList2 =new LinkedList<ExplicitRouteObject>();
		for (i=0;i<respList.size();++i){
			if (respList.get(i)==null){
				childrenFailed=true;
			}
			else {
				//System.out.println(respList.get(i).getResponse(0).toString());
				if(respList.get(i).getResponse(0).getNoPath()!=null){
					log.info("ALGUIEN RESPONDIO NOPATH");
					childrenFailed=true;
				}
				else {		
					log.info("Respuesta de "+i+" es: "+respList.get(i).toString());
					ExplicitRouteObject eroInternal =respList.get(i).getResponse(0).getPath(0).geteRO();
					//addFirstObject(eroInternal, );
					log.info(" "+eroInternal.toString());

					if (i==0){
						first=true;
					}else if (i==respList.size()-1){
						last=true;
					}
					if (this.channelType==MDHPCEMinNumberDomainsAlgorithm.SBVT_CHANNEL){
						ero.addEROSubobjectList(eroInternal.getEROSubobjectList());
					}
					else {
						
						if (this.channelType==MDHPCEMinNumberDomainsAlgorithm.MEDIA_CHANNEL){
							label_continuity=true;
						}
						else if (this.channelType==MDHPCEMinNumberDomainsAlgorithm.SBVT_CHANNEL){
							label_continuity=false;
						}
						ExplicitRouteObject cleanEro=prepareERO(eroInternal,channelType,  remove_elcs, original_end_points, first,  last,  removeFirstNode);
						if (label_continuity) {
							
							if (i==0){
								if ((respList.get(i).getResponse(0).getPath(0).getLabelSet())!=null){
									log.info("TENEMOS BITMAP LABEL SET");
									bitmap=new byte[(((BitmapLabelSet)respList.get(i).getResponse(0).getPath(0).getLabelSet()).getBytesBitmap().length)];
									label=new DWDMWavelengthLabel();
									label.setM(m);
									System.arraycopy(((BitmapLabelSet)respList.get(i).getResponse(0).getPath(0).getLabelSet()).getBytesBitmap(), 0, bitmap, 0, (((BitmapLabelSet)respList.get(i).getResponse(0).getPath(0).getLabelSet()).getBytesBitmap().length));								
								} else {
									log.info("NO TENEMOS LABEL SET");
								}
								if (respList.get(i).getResponse(0).getPath(0).getSuggestedLabel()!=null) {
									log.info("TENEMOS SUGGESTED LABEL");
									label=respList.get(i).getResponse(0).getPath(0).getSuggestedLabel().getDwdmWavelengthLabel();
									//
								}
							}else {
																
								if ((respList.get(i).getResponse(0).getPath(0).getLabelSet())!=null){
									//bitmap=restrictNCFList(ncflist,((LabelSetInclusiveList)respList.get(i).getResponse(0).getPath(0).getLabelSet()).getNCFList() );	
									restrictBitmap(bitmap,((BitmapLabelSet)respList.get(i).getResponse(0).getPath(0).getLabelSet()).getBytesBitmap());
								} else {
									
									label2=getELCfromERO(eroInternal);
									if (label==null){
										label=label2;
									}else if(label!=label2){
										no_lambda = true;
									}
								}
							}
						}
						ExplicitRouteObject ero2= new ExplicitRouteObject();
						ero2.addEROSubobjectList(cleanEro.getEROSubobjectList());
						eroList.add(ero2);
						//ero.addEROSubobjectList(eroInternal.EROSubobjectList);
						ero.addEROSubobjectList(cleanEro.getEROSubobjectList());
						if (channelType!=MDHPCEMinNumberDomainsAlgorithm.SBVT_CHANNEL){
							UnnumberIfIDEROSubobject unnumberIfDEROSubobj = new UnnumberIfIDEROSubobject(); 
							if (edge_list != null){
								if (j<edge_list.size()){						
									unnumberIfDEROSubobj.setInterfaceID(edge_list.get(j).getSrc_if_id());
									unnumberIfDEROSubobj.setRouterID((Inet4Address)edge_list.get(j).getSrc_router_id());
									log.debug(" eroExternal "+unnumberIfDEROSubobj.toString());
									//ero.addEROSubobject(unnumberIfDEROSubobj);
									addEROifnotexists(ero,unnumberIfDEROSubobj);
									addEROifnotexists(ero2,unnumberIfDEROSubobj);
									j++;
								}
							}
							
						}
					}


				}
			}
		}
		
		
		
		
		if (childrenFailed==true){
			log.warn("Some child has failed");
			NoPath noPath= new NoPath();
			response.setNoPath(noPath);
		}
		else {
			if (label_continuity) {
				if (use_elc){
					if (label!=null) {			
						explicit_label =true;
						if (no_lambda==true){
							log.warn("NO LABEL!!!");
							NoPath noPath= new NoPath();
							response.setNoPath(noPath);
								
						}else{			
							
							if (explicit_label == true)	{
								ero=addELC(ero,label,original_end_points,this.channelType);
								Iterator<ExplicitRouteObject> it= eroList.iterator();
								int k=0;
								while (it.hasNext()){
									ExplicitRouteObject erori=it.next();
									ExplicitRouteObject ero3=addELC(erori,label,reqList.get(k).getRequest(0).getEndPoints(),this.channelType);
									eroList2.add(ero3);
								}
			
								path.setEro(ero);
								response.addPath(path);	
							}else {
								log.warn("NO LABEL!!!");
								NoPath noPath= new NoPath();
								response.setNoPath(noPath);
							}
						}
							
					}else {
						log.warn("NO LABEL!!!");
						NoPath noPath= new NoPath();
						response.setNoPath(noPath);
					}

				}
			}else {
				path.setEro(ero);
				response.addPath(path);	
			}

		}


	

		long tiempofin =System.nanoTime();
		long tiempotot=tiempofin-tiempoini;
		log.info("Finish processing request "+this.reqId+"in " +tiempotot+" nanosegundos");
		//System.out.println("TOTAL "+(long)(tiempotot/100));
		Monitoring monitoring=pathReq.getMonitoring();
		if (monitoring!=null){
			if (monitoring.isProcessingTimeBit()){

			}
		}
		return m_resp;
	}

	private DWDMWavelengthLabel getELCfromERO(ExplicitRouteObject eroInternal) {
		DWDMWavelengthLabel label=null;
		
		Iterator<EROSubobject> iterEro= eroInternal.getEROSubobjectList().iterator();
		while (iterEro.hasNext()){
			EROSubobject eroSubobject = iterEro.next();
			if (eroSubobject instanceof GeneralizedLabelEROSubobject){
				//log.info("Es de tipo GeneralizedLabelEROSubobject");
				label = ((GeneralizedLabelEROSubobject)eroSubobject).getDwdmWavelengthLabel();
			}
		}
		
		return label;
	}

	
	
	private ExplicitRouteObject prepareERO(ExplicitRouteObject ero, int channelType, boolean removeELC, EndPoints ep,boolean first, boolean last, boolean removeFirstNode ){
		ExplicitRouteObject eroClean= new ExplicitRouteObject();
		if (channelType==MDHPCEMinNumberDomainsAlgorithm.MEDIA_CHANNEL){
			return ero;

		}
		else if (channelType==MDHPCEMinNumberDomainsAlgorithm.SBVT_CHANNEL){
			return ero;
		}else {

			for (int i=0;i<ero.getEROSubobjectList().size();++i) {
				if (ero.getEROSubobjectList().get(i) instanceof GeneralizedLabelEROSubobject){
					log.info("VEMOS UNA LABEL...");
					if (!removeELC) {

						eroClean.addEROSubobject(ero.getEROSubobjectList().get(i));
					}else {
						log.info("LA CEPILLAMOS...");
					}
				} else{
					eroClean.addEROSubobject(ero.getEROSubobjectList().get(i));
				}

			}
		}


		return eroClean;
	}

	public void addEROifnotexists(ExplicitRouteObject ero,UnnumberIfIDEROSubobject unnumberIfDEROSubobj){
		if (ero.getEROSubobjectList().getLast() instanceof UnnumberIfIDEROSubobject){
			if ( ( (UnnumberIfIDEROSubobject)ero.getEROSubobjectList().getLast()).getRouterID().equals(unnumberIfDEROSubobj.getRouterID())){

			}else {
				ero.getEROSubobjectList().add(unnumberIfDEROSubobj);
			}
		}else {
			ero.getEROSubobjectList().add(unnumberIfDEROSubobj);
		}
	}

	public LinkedList<NCF> restrictNCFList(LinkedList<NCF> ncflist,LinkedList<NCF> restrict ) {
		LinkedList<NCF> list=new LinkedList<NCF>();
		for (int i =0;i<ncflist.size();++i){
			if (restrict.contains(ncflist.get(i))){
				list.add(ncflist.get(i));
			}
		}

		return list;
	}

	public void restrictBitmap(byte[] bitmap, byte[] bitmap2) {
		if (bitmap2==null){
			log.info("BORRAAAANDO");
			
			for (int i=0;i<bitmap.length;++i){
				bitmap[i]=0;
			}
		}else {
			for (int i=0;i<bitmap.length;++i){
				bitmap[i]=(byte)((bitmap[i]&0xFF)&(bitmap2[i]&0xFF));
			}
		}

	}

	public boolean isLabelFree(byte[] bitmap){
		boolean isFree=false;
		if (bitmap==null){
			isFree=true;
		}else{
			for (int i=0;i<bitmap.length;++i){
				if ((bitmap[i]&0xFF)>0){
					isFree=true;
				}
			}
		}
		return isFree;
	}


	public int getFirstN(byte[] bitmap, int m){
		int n=-1;
		int max_lambdas=bitmap.length*8;
		for (int i=m; i<max_lambdas;++i){
			boolean free=true;
			for (int j=i-m;j<i+m ;++j){
				int num_byte=j/8;
				//int pos=j%8;
				//log.info("mirando lambda "+i+" en el byte "+num_byte+" con valor "+bitmap[num_byte]+" en la posicion "+(i%8));
				if ( (bitmap[num_byte]&(0x80>>>(j%8)))==(0x80>>>(j%8))){				
					//return i;
					//lambda is free
				}else {
					free=false;
				}
			}
			if (free==true){
				return i;
			}

		}
		return n;
	}


	public ExplicitRouteObject addELC(ExplicitRouteObject ero,DWDMWavelengthLabel dwdmWavelengthLabel, EndPoints ep, int channelType){
		//		boolean unNumberedIf=false;
		//		if (ep instanceof GeneralizedEndPoints){
		//			if (((GeneralizedEndPoints)ep).getP2PEndpoints()!=null){
		//				if (((GeneralizedEndPoints)ep).getP2PEndpoints().getSourceEndPoint().getUnnumberedEndpoint()!=null){
		//					unNumberedIf=true;
		//				}
		//			}
		//		 }
		ExplicitRouteObject ero2 = new ExplicitRouteObject();
		int i=0;
		if (channelType==MDHPCEMinNumberDomainsAlgorithm.MEDIA_CHANNEL) {
			for (i=0;i<ero.getEROSubobjectList().size();++i) {
				if (i==0){
					ero2.addEROSubobject(ero.getEROSubobjectList().get(i));	
				}else if (i==ero.getEROSubobjectList().size()-1){
					ero2.addEROSubobject(ero.getEROSubobjectList().get(i));	
				}else {
					ero2.addEROSubobject(ero.getEROSubobjectList().get(i));	
					if (ero.getEROSubobjectList().get(i) instanceof UnnumberIfIDEROSubobject){
						GeneralizedLabelEROSubobject ge= new GeneralizedLabelEROSubobject();
						ge.setDwdmWavelengthLabel(dwdmWavelengthLabel);
						ero2.addEROSubobject(ge);
					}
				}

			}
		}else {
			for (i=0;i<ero.getEROSubobjectList().size();++i) {
				ero2.addEROSubobject(ero.getEROSubobjectList().get(i));	
				if (ero.getEROSubobjectList().get(i) instanceof UnnumberIfIDEROSubobject){
					GeneralizedLabelEROSubobject ge= new GeneralizedLabelEROSubobject();
					ge.setDwdmWavelengthLabel(dwdmWavelengthLabel);
					ero2.addEROSubobject(ge);
				}
			}
		}

		return ero2;
	}

	@Override
	public AlgorithmReservation getReserv() {
		// TODO Auto-generated method stub
		return null;
	}


	public void addXRO(ExcludeRouteObject xro,Request req){
		req.setXro(xro);
	}


	public void processXRO(ExcludeRouteObject xro,DirectedWeightedMultigraph<Inet4Address,InterDomainEdge> networkGraph){
		if (xro!=null){
			for (int i=0;i<xro.getXROSubobjectList().size();++i){
				XROSubobject eroso=xro.getXROSubobjectList().get(i);
				if (eroso.getType()==XROSubObjectValues.XRO_SUBOBJECT_UNNUMBERED_IF_ID){
					UnnumberIfIDXROSubobject eros=(UnnumberIfIDXROSubobject)eroso;
					boolean hasVertex=networkGraph.containsVertex(eros.getRouterID());
					if (hasVertex){
						Set<InterDomainEdge> setEdges=networkGraph.edgesOf(eros.getRouterID());
						Iterator<InterDomainEdge> iter=setEdges.iterator();
						while (iter.hasNext()){
							InterDomainEdge edge=iter.next();
							if (edge.getSrc_if_id()==eros.getInterfaceID()){
								networkGraph.removeEdge(edge);																
								//InterDomainEdge edge2=networkGraph.getEdge(edge.getDst_router_id(), edge.getSrc_router_id());
							}
						}

					}
				}
			}
		}

	}


	public Inet4Address getSourceRouter(EndPoints  EP) {
		Inet4Address source_router_id_addr=null;
		if (EP.getOT()==ObjectParameters.PCEP_OBJECT_TYPE_ENDPOINTS_IPV4){
			EndPointsIPv4  ep=(EndPointsIPv4) EP;
			source_router_id_addr=ep.getSourceIP();
		}else if (EP.getOT()==ObjectParameters.PCEP_OBJECT_TYPE_ENDPOINTS_IPV6){

		}

		if (EP.getOT()==ObjectParameters.PCEP_OBJECT_TYPE_GENERALIZED_ENDPOINTS){
			GeneralizedEndPoints  gep=(GeneralizedEndPoints) EP;
			if(gep.getGeneralizedEndPointsType()==ObjectParameters.PCEP_GENERALIZED_END_POINTS_TYPE_P2P){
				P2PEndpoints p2pep= gep.getP2PEndpoints();
				EndPoint sourceep=p2pep.getSourceEndPoint();
				if (sourceep.getEndPointIPv4TLV()!=null){
					source_router_id_addr=sourceep.getEndPointIPv4TLV().getIPv4address();
				}else if (sourceep.getUnnumberedEndpoint()!=null){
					source_router_id_addr=sourceep.getUnnumberedEndpoint().getIPv4address();
				}			
			}
			if(gep.getGeneralizedEndPointsType()==ObjectParameters.PCEP_GENERALIZED_END_POINTS_TYPE_P2MP_NEW_LEAVES){
				P2MPEndpoints p2mpep= gep.getP2MPEndpoints();
				EndPointAndRestrictions epandrest=p2mpep.getEndPointAndRestrictions();
				EndPoint sourceep=epandrest.getEndPoint();
				source_router_id_addr=sourceep.getEndPointIPv4TLV().IPv4address;
				int cont=0;
				while (cont<=p2mpep.getEndPointAndRestrictionsList().size()){ //esto est� mal
					epandrest=p2mpep.getEndPointAndRestrictionsList().get(cont);
					source_router_id_addr=sourceep.getEndPointIPv4TLV().IPv4address;

				}
			}
		}
		return source_router_id_addr;
	}

	public long getSourceIfID(EndPoints  EP) {
		long if_id=-1;
		if (EP.getOT()==ObjectParameters.PCEP_OBJECT_TYPE_GENERALIZED_ENDPOINTS){
			GeneralizedEndPoints  gep=(GeneralizedEndPoints) EP;
			if(gep.getGeneralizedEndPointsType()==ObjectParameters.PCEP_GENERALIZED_END_POINTS_TYPE_P2P){
				if (gep.getP2PEndpoints().getSourceEndPoint().getUnnumberedEndpoint()!=null){
					if_id =gep.getP2PEndpoints().getSourceEndPoint().getUnnumberedEndpoint().getIfID() ;
				}			
			}

		}
		return if_id;
	}

	public long getDestIfID(EndPoints  EP) {
		long if_id=-1;
		if (EP.getOT()==ObjectParameters.PCEP_OBJECT_TYPE_GENERALIZED_ENDPOINTS){
			GeneralizedEndPoints  gep=(GeneralizedEndPoints) EP;
			if(gep.getGeneralizedEndPointsType()==ObjectParameters.PCEP_GENERALIZED_END_POINTS_TYPE_P2P){
				if (gep.getP2PEndpoints().getDestinationEndPoint().getUnnumberedEndpoint()!=null){
					if_id =gep.getP2PEndpoints().getDestinationEndPoint().getUnnumberedEndpoint().getIfID() ;
				}			
			}

		}
		return if_id;
	}


	public Inet4Address getDestRouter(EndPoints  EP) {
		Inet4Address dest_router_id_addr=null;
		if (EP.getOT()==ObjectParameters.PCEP_OBJECT_TYPE_ENDPOINTS_IPV4){
			EndPointsIPv4  ep=(EndPointsIPv4) EP;
			dest_router_id_addr=ep.getDestIP();
		}else if (EP.getOT()==ObjectParameters.PCEP_OBJECT_TYPE_ENDPOINTS_IPV6){

		}

		if (EP.getOT()==ObjectParameters.PCEP_OBJECT_TYPE_GENERALIZED_ENDPOINTS){
			GeneralizedEndPoints  gep=(GeneralizedEndPoints) EP;
			if(gep.getGeneralizedEndPointsType()==ObjectParameters.PCEP_GENERALIZED_END_POINTS_TYPE_P2P){
				P2PEndpoints p2pep= gep.getP2PEndpoints();
				EndPoint sourceep=p2pep.getSourceEndPoint();
				EndPoint destep=p2pep.getDestinationEndPoint();

				if (destep.getEndPointIPv4TLV()!=null){
					dest_router_id_addr=destep.getEndPointIPv4TLV().getIPv4address();
				}else if (destep.getUnnumberedEndpoint()!=null){
					dest_router_id_addr=destep.getUnnumberedEndpoint().getIPv4address();
				}

			}

		}

		return dest_router_id_addr;

	}


	public ExplicitRouteObject checkMediaChannel(ExplicitRouteObject ero) {
		return ero;
	}

	private PCEPRequest createRequest (int channelType, ExcludeRouteObject xro, Bandwidth bw, EndPoints ep) {
		PCEPRequest pcreqToDomain=new PCEPRequest();
		if (pathReq.getMonitoring()!=null){
			pcreqToDomain.setMonitoring(pathReq.getMonitoring());
		}
		if (pathReq.getPccReqId()!=null){
			pcreqToDomain.setPccReqId(pathReq.getPccReqId());
		}
		Request requestToDomain=new Request();

		if (bw!=null){
			requestToDomain.setBandwidth(bw);	
		}
		addXRO(xro,requestToDomain);
		requestToDomain.setEndPoints(ep);
		RequestParameters rpDomain=new RequestParameters();
		int newRequestID=ParentPCESession.getNewReqIDCounter();
		rpDomain.setRequestID(newRequestID);
		rpDomain.setPbit(true);
		requestToDomain.setRequestParameters(rpDomain);
		if (channelType==MDHPCEMinNumberDomainsAlgorithm.MEDIA_CHANNEL){
			ObjectiveFunction objectiveFunction = new ObjectiveFunction();
			objectiveFunction.setOFcode(MDHPCEMinNumberDomainsAlgorithm.OF_CODE_MEDIA_CHANNEL);
			requestToDomain.setObjectiveFunction(objectiveFunction);
		}else if (channelType==MDHPCEMinNumberDomainsAlgorithm.SBVT_CHANNEL){
			ObjectiveFunction objectiveFunction = new ObjectiveFunction();
			objectiveFunction.setOFcode(MDHPCEMinNumberDomainsAlgorithm.OF_CODE_SBVT_CHANNEL);
			requestToDomain.setObjectiveFunction(objectiveFunction);
		}
		pcreqToDomain.addRequest(requestToDomain);

		return pcreqToDomain;
	}
	
	
	public void pruneSVBTs (DirectedWeightedMultigraph<Object,InterDomainEdge> graph){
		Iterator<InterDomainEdge> it=graph.edgeSet().iterator();
		LinkedList<InterDomainEdge> delList=new LinkedList<InterDomainEdge>();
		while (it.hasNext()){
			InterDomainEdge edge = it.next();
			if (edge.getTE_info()!=null){
				if (edge.getTE_info().getMfOTF()!=null){
					//graph.removeEdge(edge);
					delList.add(edge);
				}
			}
		}
		for (int i=0;i<delList.size();++i){
			graph.removeEdge(delList.get(i));
		}
		
		
	}

}
