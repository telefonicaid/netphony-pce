package es.tid.pce.computingEngine.algorithms;

import java.net.Inet4Address;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

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
import es.tid.rsvp.objects.subobjects.GeneralizedLabelEROSubobject;
import es.tid.rsvp.objects.subobjects.IPv4prefixEROSubobject;
import es.tid.rsvp.objects.subobjects.UnnumberIfIDEROSubobject;
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
	private Logger log=Logger.getLogger("PCEServer");
	private ComputingRequest pathReq;
	private ChildPCERequestManager childPCERequestManager;
	private ReachabilityManager reachabilityManager;

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
		if (pathReq.getEcodingType()==PCEPMessageTypes.MESSAGE_INITIATE){
			m_resp.setEncodingType(PCEPMessageTypes.MESSAGE_REPORT);
		}
		else {
			m_resp.setEncodingType(pathReq.getEcodingType());
		}

		Request req=pathReq.getRequestList().get(0); // Get the original request
		long reqId=req.getRequestParameters().getRequestID(); //Get the request ID.
		log.info("Processing MD Path Computing with MDHPCEMinNumberDomainsAlgorithm (Minimum transit Domains) with Request id: "+reqId);

		//Start creating the response
		//We create it now, in case we need to send a NoPath later
		Response response=new Response();
		RequestParameters rp = new RequestParameters();
		rp.setRequestID(reqId); // We answer with the same request ID.
		response.setRequestParameters(rp);

		EndPoints  original_end_points= req.getEndPoints(); //Get the original end points. 
		Inet4Address source_router_id_addr = getSourceRouter(original_end_points);
		long src_if_id=getSourceIfID(original_end_points);
		Inet4Address dest_router_id_addr = getDestRouter(original_end_points);
		long dst_if_id=getDestIfID(original_end_points);

		//First, we obtain the domains of each endPoint
		Inet4Address source_domain_id=this.reachabilityManager.getDomain(source_router_id_addr);
		Inet4Address dest_domain_id=this.reachabilityManager.getDomain(dest_router_id_addr);
		log.info("MD Request from "+source_router_id_addr+" (domain "+source_domain_id+") to "+ dest_router_id_addr+" (domain "+dest_domain_id+")");

		//CHECK IF DOMAIN_ID ARE NULL!!!!!!
		log.info("Check if SRC and Dest domains are OK");
		if ((dest_domain_id==null)||(source_domain_id==null)){
			//ONE OF THEM IS NOT REACHABLE, SEND NOPATH!!!
			log.warning("One of the domains is not reachable, sending NOPATH");
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
			log.warning("Source or destination domains are NOT in the TED");
			//FIXME: VER ESTE CASO
			NoPath noPath= new NoPath();
			noPath.setNatureOfIssue(ObjectParameters.NOPATH_NOPATH_SAT_CONSTRAINTS);
			NoPathTLV noPathTLV=new NoPathTLV();
			if (!((networkGraph.containsVertex(source_router_id_addr)))){
				log.finest("Unknown source domain");	
				noPathTLV.setUnknownSource(true);	
			}
			if (!((networkGraph.containsVertex(dest_router_id_addr)))){
				log.finest("Unknown destination domain");
				noPathTLV.setUnknownDestination(true);	
			}

			noPath.setNoPathTLV(noPathTLV);				
			response.setNoPath(noPath);
			m_resp.addResponse(response);
			return m_resp;
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
			log.severe("Problem getting the domain sequence");
			NoPath noPath2= new NoPath();
			noPath2.setNatureOfIssue(ObjectParameters.NOPATH_NOPATH_SAT_CONSTRAINTS);
			NoPathTLV noPathTLV=new NoPathTLV();
			noPath2.setNoPathTLV(noPathTLV);				
			response.setNoPath(noPath2);
			m_resp.addResponse(response);
			return m_resp;
		}
		List<InterDomainEdge> edge_list=gp.getEdgeList();
		long tiempo2 =System.nanoTime();//FIXME: No se usa por ahora
		boolean first_domain_equal=false;
		if (source_domain_id.equals(dest_domain_id)){
			PCEPRequest pcreqToDomain=new PCEPRequest();
			if (pathReq.getMonitoring()!=null){
				pcreqToDomain.setMonitoring(pathReq.getMonitoring());
			}
			if (pathReq.getPccReqId()!=null){
				pcreqToDomain.setPccReqId(pathReq.getPccReqId());
			}
			Request requestToDomain=new Request();
			//requestToDomain.setObjectiveFunction(pathReq.getRequestList().get(0).getObjectiveFunction());			
			requestToDomain.setBandwidth(pathReq.getRequestList().get(0).getBandwidth().duplicate());	
			addXRO(req.getXro(),requestToDomain);
			requestToDomain.setEndPoints(pathReq.getRequestList().get(0).getEndPoints());
			RequestParameters rpDomain=new RequestParameters();
			int newRequestID=ParentPCESession.getNewReqIDCounter();
			rpDomain.setRequestID(newRequestID);
			rpDomain.setPbit(true);
			requestToDomain.setRequestParameters(rpDomain);
			pcreqToDomain.addRequest(requestToDomain);
			reqList.add(pcreqToDomain);
			domainList.add(source_domain_id);
			log.info("Sending ONLY ONE request: "+requestToDomain.toString()+" to domain "+source_domain_id);

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
						destIPv4TLV.setIfID(edge_list.get(0).getDst_if_id());
						sourceEP.setUnnumberedEndpoint(sourceIPv4TLV);
						destEP.setUnnumberedEndpoint(destIPv4TLV);
					}					
					P2PEndpoints p2pep=new P2PEndpoints();
					p2pep.setSourceEndPoints(sourceEP);
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
				//requestToFirstDomain.setObjectiveFunction(pathReq.getRequestList().get(0).getObjectiveFunction());
				requestToFirstDomain.setBandwidth(pathReq.getRequestList().get(0).getBandwidth());
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
							sourceIPv4TLV.setIfID(edge_list.get(i-1).getSrc_if_id());
							destIPv4TLV.setIPv4address(destIP);
							destIPv4TLV.setIfID(edge_list.get(0).getDst_if_id());
							sourceEP.setUnnumberedEndpoint(sourceIPv4TLV);
							destEP.setUnnumberedEndpoint(destIPv4TLV);
						}					

						P2PEndpoints p2pep=new P2PEndpoints();
						p2pep.setSourceEndPoints(sourceEP);
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
						sourceIPv4TLV.setIfID(edge_list.get(i-1).getSrc_if_id());
						destIPv4TLV.setIPv4address(dest_router_id_addr);
						destIPv4TLV.setIfID(dst_if_id);
						sourceEP.setUnnumberedEndpoint(sourceIPv4TLV);
						destEP.setUnnumberedEndpoint(destIPv4TLV);
					}					

					P2PEndpoints p2pep=new P2PEndpoints();
					p2pep.setSourceEndPoints(sourceEP);
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
			log.severe("PROBLEM SENDING THE REQUESTS");
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
		boolean childrenFailed=false;
		int i;

		boolean use_elc=true;
		boolean label_continuity=true;
		boolean remove_elcs=true;

		boolean first=false;
		boolean last=false;
		boolean removeFirstNode=true;
		//LinkedList<NCF> ncflist=new LinkedList<NCF>();
		byte[] bitmap=null;
		DWDMWavelengthLabel label=null;
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
					log.info(" "+eroInternal.toString());
					if (i==0){
						first=true;
					}else if (i==respList.size()-1){
						last=true;
					}
					log.info("A limpiar 1 ");
					ExplicitRouteObject cleanEro=prepareERO(eroInternal,  remove_elcs, original_end_points, first,  last,  removeFirstNode);
					if (label_continuity) {
						log.info("A limpiar 2");
						if (i==0){
							if ((respList.get(i).getResponse(0).getPath(0).getLabelSet())!=null){
								log.info("TENEMOS BITMAP LABEL SET");
								bitmap=new byte[(((BitmapLabelSet)respList.get(i).getResponse(0).getPath(0).getLabelSet()).getBytesBitmap().length)];
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
								restrictBitmap(bitmap,null);
							}
						}
					}
					ExplicitRouteObject ero2= new ExplicitRouteObject();
					ero2.addEROSubobjectList(cleanEro.getEROSubobjectList());
					eroList.add(ero2);
					//ero.addEROSubobjectList(eroInternal.EROSubobjectList);
					ero.addEROSubobjectList(cleanEro.getEROSubobjectList());
					UnnumberIfIDEROSubobject unnumberIfDEROSubobj = new UnnumberIfIDEROSubobject(); 
					if (edge_list != null){
						if (j<edge_list.size()){						
							unnumberIfDEROSubobj.setInterfaceID(edge_list.get(j).getSrc_if_id());
							unnumberIfDEROSubobj.setRouterID((Inet4Address)edge_list.get(j).getSrc_router_id());
							log.info(" eroExternal "+unnumberIfDEROSubobj.toString());
							//ero.addEROSubobject(unnumberIfDEROSubobj);
							addEROifnotexists(ero,unnumberIfDEROSubobj);
							addEROifnotexists(ero2,unnumberIfDEROSubobj);
							j++;
						}
					}
					;
				}
			}
		}
		if (childrenFailed==true){
			log.warning("Some child has failed");
			NoPath noPath= new NoPath();
			response.setNoPath(noPath);
		}
		else {
			log.warning("AAAA");
			if (label_continuity) {
				log.warning("bbbb");
				if (use_elc){
					log.warning("cccc");
					if (label!=null) {
						log.warning("ddd");
						if (isLabelFree(bitmap)){
							int m=label.getM();
							int n=getFirstN(bitmap,m);
							label.setN(n);
							ero=addELC(ero,label,original_end_points);
							Iterator<ExplicitRouteObject> it= eroList.iterator();
							int k=0;
							while (it.hasNext()){
								ExplicitRouteObject erori=it.next();
								ExplicitRouteObject ero3=addELC(erori,label,reqList.get(k).getRequest(0).getEndPoints());
								eroList2.add(ero3);
							}

							path.seteRO(ero);
							response.addPath(path);	
						}else {
							log.warning("NO LABEL!!!");
							NoPath noPath= new NoPath();
							response.setNoPath(noPath);
						}
					}else {
						log.warning("NO LABEL!!!");
						NoPath noPath= new NoPath();
						response.setNoPath(noPath);
					}

				}
			}else {
				path.seteRO(ero);
				response.addPath(path);	
			}

		}


		//OSCAR INI
		log.info("VAMOS A MANDAR LOS INIS ");
		LinkedList <ComputingResponse> respList2=null;
		if (pathReq.getEcodingType()==PCEPMessageTypes.MESSAGE_INITIATE){
			LinkedList<PCEPInitiate> iniList= new LinkedList<PCEPInitiate>();
			for (i=0;i<respList.size();++i){
				PCEPInitiate ini = new PCEPInitiate();
				PCEPIntiatedLSP inilsp = new PCEPIntiatedLSP();
				ini.getPcepIntiatedLSPList().add(inilsp);
				SRP srp= new SRP();
				srp.setSRP_ID_number(ParentPCESession.getNewReqIDCounter());
				inilsp.setRsp(srp);
				inilsp.setEndPoint(reqList.get(i).getRequest(0).getEndPoints());
				inilsp.setEro(eroList2.get(i));
				inilsp.setBandwidth(pathReq.getRequestList().get(0).getBandwidth().duplicate());
				LSP lsp =new LSP();
				lsp.setLspId(0);
				SymbolicPathNameTLV symbolicPathNameTLV_tlv = new SymbolicPathNameTLV();
				//String name =pathReq.getIniLSP().getLsp().getSymbolicPathNameTLV_tlv().toString()+"-segment-"+i;
				String name ="IDEALIST "+ ParentPCESession.getNewReqIDCounter()+"-segment-"+i;
				byte [] symbolicPathNameID= name.getBytes();
				symbolicPathNameTLV_tlv.setSymbolicPathNameID(symbolicPathNameID);

				lsp.setSymbolicPathNameTLV_tlv(symbolicPathNameTLV_tlv);
				inilsp.setLsp(lsp);
				iniList.add(ini);
			}
			try {
				log.info("VAAAAAAAAMOS ");
				respList2= childPCERequestManager.executeInitiates(iniList, domainList);
				log.info("SE LLAMOOOOOOO ");

			}catch (Exception e){
				log.severe("PROBLEM SENDING THE INITIATES");
				NoPath noPath2= new NoPath();
				noPath2.setNatureOfIssue(ObjectParameters.NOPATH_NOPATH_SAT_CONSTRAINTS);
				NoPathTLV noPathTLV=new NoPathTLV();
				noPath2.setNoPathTLV(noPathTLV);				
				response.setNoPath(noPath2);
				m_resp.addResponse(response);
				return m_resp;
			}
		}
		log.info("A VER QUE SE RESPONDE ");
		if (respList2==null){
			log.warning("RESPLIST = NULL");
			childrenFailed=true;
		}else {
			for (i=0;i<respList2.size();++i){
				log.info("viendo  "+i);
				if (respList2.get(i)==null){
					childrenFailed=true;
				}
			}
		}
		if (respList2!=null){

			if (childrenFailed) {
				log.warning("Some child has failed to initiate");
				LinkedList<PCEPInitiate> deleteList= new LinkedList<PCEPInitiate>();
				LinkedList<Object> domainList2 = new LinkedList<Object>();
				for (i=0;i<respList2.size();++i){
					if (respList2.get(i)!=null){
						domainList2.add(domainList.get(i));
						//Send delete
						PCEPInitiate ini = new PCEPInitiate();
						PCEPIntiatedLSP inilsp = new PCEPIntiatedLSP();
						ini.getPcepIntiatedLSPList().add(inilsp);
						SRP srp= new SRP();
						srp.setSRP_ID_number(ParentPCESession.getNewReqIDCounter());
						srp.setrFlag(true);
						inilsp.setRsp(srp);
						inilsp.setEndPoint(reqList.get(i).getRequest(0).getEndPoints());
						inilsp.setEro(respList2.get(i).getResponse(0).getPath(0).geteRO());
						LSP lsp =new LSP();
						lsp.setLspId((respList2.get(i).getReportList().getFirst().getLSP().getLspId()));

						lsp.setSymbolicPathNameTLV_tlv(respList2.get(i).getReportList().getFirst().getLSP().getSymbolicPathNameTLV_tlv());
						inilsp.setLsp(lsp);
						deleteList.add(ini);

					}
				}
					try {
						respList= childPCERequestManager.executeInitiates(deleteList, domainList2);	
					}catch (Exception e){
						log.severe("PROBLEM SENDING THE DELETES");
						NoPath noPath2= new NoPath();
						noPath2.setNatureOfIssue(ObjectParameters.NOPATH_NOPATH_SAT_CONSTRAINTS);
						NoPathTLV noPathTLV=new NoPathTLV();
						noPath2.setNoPathTLV(noPathTLV);				
						response.setNoPath(noPath2);
						m_resp.addResponse(response);
						return m_resp;
					}
				

			} else {
				StateReport sr = new StateReport();
				
				LSP lsp=new LSP();
				lsp.setLspId(1);
				SymbolicPathNameTLV symbolicPathNameTLV_tlv = new SymbolicPathNameTLV();
				String name="IDEALIST";
				
				symbolicPathNameTLV_tlv.setSymbolicPathNameID(name.getBytes());
				lsp.setSymbolicPathNameTLV_tlv(symbolicPathNameTLV_tlv);
				sr.setLSP(lsp);
				SRP srp = new SRP();
				
				srp.setSRP_ID_number(pathReq.getIniLSP().getRsp().getSRP_ID_number());
				sr.setSRP(srp);
				Path path2 = new Path();
				path2.seteRO(ero);
				sr.setPath(path2);
				m_resp.addReport(sr);
			}
		}




		//OSCAR FIN

		long tiempofin =System.nanoTime();
		long tiempotot=tiempofin-tiempoini;
		log.info("Ha tardado "+tiempotot+" nanosegundos");
		//System.out.println("TOTAL "+(long)(tiempotot/100));
		Monitoring monitoring=pathReq.getMonitoring();
		if (monitoring!=null){
			if (monitoring.isProcessingTimeBit()){

			}
		}
		return m_resp;
	}

	private ExplicitRouteObject prepareERO(ExplicitRouteObject ero, boolean removeELC, EndPoints ep,boolean first, boolean last, boolean removeFirstNode ){
		// boolean removeFirstNode=false;
		//		 if (ep instanceof GeneralizedEndPoints){
		//			if (((GeneralizedEndPoints)ep).getP2PEndpoints()!=null){
		//				if (((GeneralizedEndPoints)ep).getP2PEndpoints().getSourceEndPoint().getUnnumberedEndpoint()!=null){
		//					removeFirstNode=true;
		//				}
		//			}
		//		 }

		ExplicitRouteObject eroClean= new ExplicitRouteObject();
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
			//				if (first){
			//					log.info("Primer dominio");
			//					if (i==0){
			//						if (removeFirstNode) {
			//							if (ep instanceof GeneralizedEndPoints){
			//								if (((GeneralizedEndPoints)ep).getP2PEndpoints()!=null){
			//									if (((GeneralizedEndPoints)ep).getP2PEndpoints().getSourceEndPoint().getEndPointIPv4TLV()!=null){
			//										if (ero.getEROSubobjectList().get(i) instanceof IPv4prefixEROSubobject){
			//											if (((GeneralizedEndPoints)ep).getP2PEndpoints().getSourceEndPoint().getEndPointIPv4TLV().getIPv4address().equals(((IPv4prefixEROSubobject)ero.getEROSubobjectList().get(i)).getIpv4address())){
			//												//First node needs to be removed
			//											}else {
			//												//The first subobject is not the sources... we add it
			//												eroClean.addEROSubobject(ero.getEROSubobjectList().get(i));
			//											}
			//										}
			//										else if (ero.getEROSubobjectList().get(i) instanceof UnnumberIfIDEROSubobject){
			//											eroClean.addEROSubobject(ero.getEROSubobjectList().get(i));
			//										}
			//									} else if (((GeneralizedEndPoints)ep).getP2PEndpoints().getSourceEndPoint().getUnnumberedEndpoint()!=null){
			//										if (ero.getEROSubobjectList().get(i) instanceof IPv4prefixEROSubobject){
			//											//The source is an input interface that needs to be added
			//											UnnumberIfIDEROSubobject uu=new UnnumberIfIDEROSubobject();
			//											uu.setInterfaceID(((GeneralizedEndPoints)ep).getP2PEndpoints().getSourceEndPoint().getUnnumberedEndpoint().getIfID());
			//											uu.setRouterID(((GeneralizedEndPoints)ep).getP2PEndpoints().getSourceEndPoint().getUnnumberedEndpoint().getIPv4address());
			//												eroClean.addEROSubobject(ero.getEROSubobjectList().get(i));
			//										}
			//										else if (ero.getEROSubobjectList().get(i) instanceof UnnumberIfIDEROSubobject){
			//											if (((GeneralizedEndPoints)ep).getP2PEndpoints().getSourceEndPoint().getUnnumberedEndpoint().getIfID()==((UnnumberIfIDEROSubobject)ero.getEROSubobjectList().get(i)).getInterfaceID()){
			//												//its the same interface ID
			//												if (((GeneralizedEndPoints)ep).getP2PEndpoints().getSourceEndPoint().getUnnumberedEndpoint().getIPv4address().equals(((UnnumberIfIDEROSubobject)ero.getEROSubobjectList().get(i)).getRouterID())){
			//													//its the same router ID
			//													eroClean.addEROSubobject(ero.getEROSubobjectList().get(i));
			//												}else {
			//													//The source is an input interface that needs to be added
			//													UnnumberIfIDEROSubobject uu=new UnnumberIfIDEROSubobject();
			//													uu.setInterfaceID(((GeneralizedEndPoints)ep).getP2PEndpoints().getSourceEndPoint().getUnnumberedEndpoint().getIfID());
			//													uu.setRouterID(((GeneralizedEndPoints)ep).getP2PEndpoints().getSourceEndPoint().getUnnumberedEndpoint().getIPv4address());
			//														eroClean.addEROSubobject(ero.getEROSubobjectList().get(i));
			//												}
			//											}else {
			//												//The source is an input interface that needs to be added
			//												UnnumberIfIDEROSubobject uu=new UnnumberIfIDEROSubobject();
			//												uu.setInterfaceID(((GeneralizedEndPoints)ep).getP2PEndpoints().getSourceEndPoint().getUnnumberedEndpoint().getIfID());
			//												uu.setRouterID(((GeneralizedEndPoints)ep).getP2PEndpoints().getSourceEndPoint().getUnnumberedEndpoint().getIPv4address());
			//												eroClean.addEROSubobject(ero.getEROSubobjectList().get(i));
			//											}											
			//										}
			//									}
			//										
			//										
			//								}
			//							}else {
			//								
			//							}
			//						}
			//					}

			//				}
			//If it is not the last leg 


			//}

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
		for (int i=0;i<bitmap.length;++i){
			if ((bitmap[i]&0xFF)>0){
				isFree=true;
			}
		}
		return isFree;
	}

	//	public int getFirstN(byte[] bitmap){
	//		int n=-1;
	//		int max_lambdas=bitmap.length*8;
	//		for (int i=0; i<max_lambdas;++i){
	//			int num_byte=i/8;
	//			int pos=i%8;
	//			//log.info("mirando lambda "+i+" en el byte "+num_byte+" con valor "+bitmap[num_byte]+" en la posicion "+(i%8));
	//			if ( (bitmap[num_byte]&(0x80>>>(i%8)))==(0x80>>>(i%8))){				
	//				return i;
	//			}			
	//		}
	//		return n;
	//	}

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


	public ExplicitRouteObject addELC(ExplicitRouteObject ero,DWDMWavelengthLabel dwdmWavelengthLabel, EndPoints ep){
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
		for (i=0;i<ero.getEROSubobjectList().size();++i) {
			ero2.addEROSubobject(ero.getEROSubobjectList().get(i));	
			if (ero.getEROSubobjectList().get(i) instanceof UnnumberIfIDEROSubobject){
				GeneralizedLabelEROSubobject ge= new GeneralizedLabelEROSubobject();
				ge.setDwdmWavelengthLabel(dwdmWavelengthLabel);
				ero2.addEROSubobject(ge);
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
			for (int i=0;i<xro.getEROSubobjectList().size();++i){
				XROSubobject eroso=xro.getEROSubobjectList().get(i);
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

}
