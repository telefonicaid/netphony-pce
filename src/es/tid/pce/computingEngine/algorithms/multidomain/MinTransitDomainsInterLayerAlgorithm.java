package es.tid.pce.computingEngine.algorithms.multidomain;

import java.net.Inet4Address;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import org.jgrapht.GraphPath;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.graph.DirectedWeightedMultigraph;

import es.tid.pce.computingEngine.ComputingRequest;
import es.tid.pce.computingEngine.ComputingResponse;
import es.tid.pce.computingEngine.algorithms.AlgorithmReservation;
import es.tid.pce.computingEngine.algorithms.ComputingAlgorithm;
import es.tid.pce.parentPCE.ChildPCERequestManager;
import es.tid.pce.parentPCE.ParentPCESession;
import es.tid.pce.pcep.constructs.EndPoint;
import es.tid.pce.pcep.constructs.EndPointAndRestrictions;
import es.tid.pce.pcep.constructs.P2MPEndpoints;
import es.tid.pce.pcep.constructs.P2PEndpoints;
import es.tid.pce.pcep.constructs.Path;
import es.tid.pce.pcep.constructs.Request;
import es.tid.pce.pcep.constructs.Response;
import es.tid.pce.pcep.constructs.SwitchEncodingType;
import es.tid.pce.pcep.messages.PCEPRequest;
import es.tid.pce.pcep.objects.EndPoints;
import es.tid.pce.pcep.objects.EndPointsIPv4;
import es.tid.pce.pcep.objects.ExcludeRouteObject;
import es.tid.pce.pcep.objects.ExplicitRouteObject;
import es.tid.pce.pcep.objects.GeneralizedEndPoints;
import es.tid.pce.pcep.objects.InterLayer;
import es.tid.pce.pcep.objects.Monitoring;
import es.tid.pce.pcep.objects.NoPath;
import es.tid.pce.pcep.objects.ObjectParameters;
import es.tid.pce.pcep.objects.RequestParameters;
import es.tid.pce.pcep.objects.SwitchLayer;
import es.tid.pce.pcep.objects.tlvs.EndPointIPv4TLV;
import es.tid.pce.pcep.objects.tlvs.NoPathTLV;
import es.tid.rsvp.objects.subobjects.IPv4prefixEROSubobject;
import es.tid.rsvp.objects.subobjects.UnnumberIfIDEROSubobject;
import es.tid.tedb.ITMDTEDB;
import es.tid.tedb.InterDomainEdge;
import es.tid.tedb.MDTEDB;
import es.tid.tedb.ReachabilityManager;
import es.tid.tedb.TEDB;

/**
 * Algorithm to Minimize the number of Transit Domains (MTD) with ability to perform Interlayer paths
 * @author ogondio
 *
 */


public class MinTransitDomainsInterLayerAlgorithm implements ComputingAlgorithm{
	private DirectedWeightedMultigraph<Object,InterDomainEdge> networkGraph;
	private Logger log=Logger.getLogger("PCEServer");
	private ComputingRequest pathReq;
	private ChildPCERequestManager childPCERequestManager;
	private ReachabilityManager reachabilityManager;
	
	public MinTransitDomainsInterLayerAlgorithm(ComputingRequest pathReq,TEDB ted,ChildPCERequestManager cprm , ReachabilityManager rm){
		if(ted.isITtedb()){
			this.networkGraph=((ITMDTEDB)ted).getDuplicatedMDNetworkGraph();
		}else{
			this.networkGraph=((MDTEDB)ted).getDuplicatedMDNetworkGraph();
		}
		this.reachabilityManager=rm;
		this.pathReq=pathReq;		
		this.childPCERequestManager=cprm;
	}
	
	
	
	public ComputingResponse call()throws Exception{
		//For performance monitoring, first get initial time.
		//Oscar: ver si se puede quitar despues!!! Asi se deja toda la parte del monitoring fuera
		long tiempoini =System.nanoTime();
		//Create PCEP Response message
		ComputingResponse m_resp=new ComputingResponse();
		m_resp.setEncodingType(pathReq.getEcodingType());
		//Get the request
		Request req=pathReq.getRequestList().get(0);
		//Get the request ID
		long reqId=req.getRequestParameters().getRequestID();
		//Trace to inform that the algorithm is starting
		log.info("Processing MultiDomain Path Computation with Interlayer Capabiilties for Request id: "+reqId);
 
		//Start creating the response OBJECT
		//We create it now, in case we need to send a NoPath later
		Response response=new Response();
		m_resp.addResponse(response);
		RequestParameters rp = new RequestParameters();
		rp.setRequestID(reqId);
		response.setRequestParameters(rp);
		
		//Now, get the endpoints
		
		EndPoints  EP= req.getEndPoints();	
		Inet4Address source_router_id_addr = null;
		Inet4Address dest_router_id_addr = null;
		
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
			//m_resp.addResponse(response);
			return m_resp;
		}
		if (!((networkGraph.containsVertex(source_domain_id))&&(networkGraph.containsVertex(dest_domain_id)))){
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
			//m_resp.addResponse(response);
			return m_resp;
		}
		if (source_domain_id.equals(dest_domain_id)){
			log.warning("Source and destination domain is the same, case not allowed");
			NoPath noPath2= new NoPath();
			noPath2.setNatureOfIssue(ObjectParameters.NOPATH_NOPATH_SAT_CONSTRAINTS);
			NoPathTLV noPathTLV=new NoPathTLV();
			noPath2.setNoPathTLV(noPathTLV);				
			response.setNoPath(noPath2);
			//m_resp.addResponse(response);
			return m_resp;
		}
		
		//Now, compute the shortest sequence of domains
		log.info("Processing XRO");
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
			//m_resp.addResponse(response);
			return m_resp;
		}
		List<InterDomainEdge> edge_list=gp.getEdgeList();
		long tiempo2 =System.nanoTime();
		log.info("Vamos a comprobar InterLayer");
		boolean switchLayerPresent=false;
		boolean interLayerAllowed=false;		
		int requestedLSPEncodingType=0;		
		int requestedSwitchingType=0;
		//FIXME: ESTO ESTA A PELO!!!
		int interDomainLinksSwitchingType=150;
		//FIXME: ESTO ESTA A PELO!!!
		int interDomainLinksEncodingType=8;
		boolean interLayerComputation=false;
		
		if (req.getInterLayer()!=null){
			interLayerAllowed=req.getInterLayer().isIFlag();	
			if (interLayerAllowed){
				log.info("InterLayer Allowed!");
			}
				
		}
		log.info("Vamos a comprobar SwitchLayer");
		if (req.getSwitchLayer()!=null){
			switchLayerPresent=true;
			if (req.getSwitchLayer().getSwitchLayers().size()>=1){
				if (req.getSwitchLayer().getSwitchLayers().getFirst().isIflag()){
					requestedLSPEncodingType=req.getSwitchLayer().getSwitchLayers().getFirst().getLSPEncodingType();
					log.info("LSP Encoding Type: "+requestedLSPEncodingType);
					requestedSwitchingType=req.getSwitchLayer().getSwitchLayers().getFirst().getSwitchingType();
					log.info("LSP Switching Type: "+requestedSwitchingType);					
				}
			}
		}
		
		//LOGIC: If the switchlayer Object is present, and the switch layer is differert, check if interlayer is allows
		if (switchLayerPresent) {
			if (requestedSwitchingType!=interDomainLinksSwitchingType){
				if (interLayerAllowed){
					interLayerComputation=true;
				}
				
			}
		}
		
		//log.info("number of involved domains = "+edge_list.size()+1);
		int i=0;
		
		/////////////////////////////////////////////////////////
		//Create request for the FIRST domain involved
		//////////////////////////////////////////////////////////
		Inet4Address destIP = null;
		EndPoints endpointsRequest = null;
		if (EP.getOT()==ObjectParameters.PCEP_OBJECT_TYPE_ENDPOINTS_IPV4){			
			endpointsRequest = new EndPointsIPv4();
			((EndPointsIPv4) endpointsRequest).setSourceIP(source_router_id_addr);
			destIP=(Inet4Address)edge_list.get(0).getSrc_router_id();
			((EndPointsIPv4) endpointsRequest).setDestIP(destIP);			
		} else {
			//FIXME: AHORA envio solo un nopath.. ver el resto DE CASOS
			this.createNoPath(m_resp);
			return m_resp;
		}
	
		Inet4Address domain =(Inet4Address)edge_list.get(0).getSource();
		log.info("First part of the LSP is in domain: "+ domain+" from "+ source_router_id_addr+" to "+destIP);
		//FIXME: METRICA? OF? BW?
		long requestID;
		boolean first_domain_equal=false;
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
			addXRO(req.getXro(),requestToFirstDomain);
			requestToFirstDomain.setEndPoints(endpointsRequest);
			RequestParameters rpFirstDomain=new RequestParameters();
			requestID=ParentPCESession.getNewReqIDCounter();
			rpFirstDomain.setRequestID(requestID);
			rpFirstDomain.setPbit(true);
			InterLayer interLayerFirstDomain=new InterLayer();
			interLayerFirstDomain.setIFlag(false);
			SwitchLayer switchLayerFirstDomain= new SwitchLayer();
			SwitchEncodingType set=new SwitchEncodingType();
			
			switchLayerFirstDomain.getSwitchLayers().add(set);
			if (interLayerComputation){
				set.setLSPEncodingType(requestedLSPEncodingType);
				set.setSwitchingType(requestedSwitchingType);				
			}else {
				set.setLSPEncodingType(interDomainLinksEncodingType);
				set.setSwitchingType(interDomainLinksSwitchingType);
			}
			set.setIflag(true);
			requestToFirstDomain.setInterLayer(interLayerFirstDomain);
			requestToFirstDomain.setSwitchLayer(switchLayerFirstDomain);
			requestToFirstDomain.setRequestParameters(rpFirstDomain);
			pcreqToFirstDomain.addRequest(requestToFirstDomain);
			reqList.add(pcreqToFirstDomain);
			domainList.add(domain);
			log.info("Sending 1st request to domain "+domain);
		}
		for (i=1;i<edge_list.size();++i){
			
			domain =(Inet4Address)edge_list.get(i).getSource();
		
			if (EP.getOT()==ObjectParameters.PCEP_OBJECT_TYPE_ENDPOINTS_IPV4){
				endpointsRequest=new EndPointsIPv4();
				((EndPointsIPv4)endpointsRequest).setSourceIP((Inet4Address)edge_list.get(i-1).getDst_router_id());
				((EndPointsIPv4)endpointsRequest).setDestIP((Inet4Address)edge_list.get(i).getSrc_router_id());
			}else if (EP.getOT()==ObjectParameters.PCEP_OBJECT_TYPE_ENDPOINTS_IPV6){
				
			}
			
			if (EP.getOT()==ObjectParameters.PCEP_OBJECT_TYPE_GENERALIZED_ENDPOINTS){
				GeneralizedEndPoints  gep=(GeneralizedEndPoints) req.getEndPoints();
				if(gep.getGeneralizedEndPointsType()==ObjectParameters.PCEP_GENERALIZED_END_POINTS_TYPE_P2P){
					EndPointIPv4TLV sourceIPv4TLV = new EndPointIPv4TLV();
					EndPointIPv4TLV destIPv4TLV = new EndPointIPv4TLV();
					sourceIPv4TLV.setIPv4address((Inet4Address)edge_list.get(i-1).getDst_router_id());
					destIP=(Inet4Address)edge_list.get(i).getSrc_router_id();
					destIPv4TLV.setIPv4address(destIP);
					
					EndPoint sourceEP=new EndPoint();
					EndPoint destEP=new EndPoint();
					sourceEP.setEndPointIPv4TLV(sourceIPv4TLV);
					destEP.setEndPointIPv4TLV(destIPv4TLV);
					
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
			addXRO(req.getXro(),request);
			request.setEndPoints(endpointsRequest);
			RequestParameters rp2=new RequestParameters();
			requestID=ParentPCESession.getNewReqIDCounter();
			rp2.setRequestID(requestID);
			rp2.setPbit(true);
			request.setRequestParameters(rp2);
			InterLayer interLayer=new InterLayer();
			interLayer.setIFlag(false);
			SwitchLayer switchLayer= new SwitchLayer();
			SwitchEncodingType set=new SwitchEncodingType();
			
			switchLayer.getSwitchLayers().add(set);
			set.setLSPEncodingType(interDomainLinksEncodingType);
			set.setSwitchingType(interDomainLinksSwitchingType);
			set.setIflag(true);
			request.setInterLayer(interLayer);
			request.setSwitchLayer(switchLayer);
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
		
		if (EP.getOT()==ObjectParameters.PCEP_OBJECT_TYPE_ENDPOINTS_IPV4){
			endpointsLastDomain = new EndPointsIPv4();			
			((EndPointsIPv4)endpointsLastDomain).setDestIP(dest_router_id_addr);
					//FIXME: PONGO EL IF NO NUMERADO????
			((EndPointsIPv4)endpointsLastDomain).setSourceIP(last_source_IP);
			//FIXME: METRICA? OF? BW?

		}else if (EP.getOT()==ObjectParameters.PCEP_OBJECT_TYPE_ENDPOINTS_IPV6){
			
		}
		
		if (EP.getOT()==ObjectParameters.PCEP_OBJECT_TYPE_GENERALIZED_ENDPOINTS){
			GeneralizedEndPoints  gep=(GeneralizedEndPoints) req.getEndPoints();
			if(gep.getGeneralizedEndPointsType()==ObjectParameters.PCEP_GENERALIZED_END_POINTS_TYPE_P2P){
				EndPointIPv4TLV sourceIPv4TLV = new EndPointIPv4TLV();
				EndPointIPv4TLV destIPv4TLV = new EndPointIPv4TLV();
				sourceIPv4TLV.setIPv4address(last_source_IP);
				destIPv4TLV.setIPv4address(dest_router_id_addr);
				
				EndPoint sourceEP=new EndPoint();
				EndPoint destEP=new EndPoint();
				sourceEP.setEndPointIPv4TLV(sourceIPv4TLV);
				destEP.setEndPointIPv4TLV(destIPv4TLV);
				
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
		addXRO(req.getXro(),requestToLastDomain);
		requestToLastDomain.setEndPoints(endpointsLastDomain);
		RequestParameters rpLastDomain=new RequestParameters();
		requestID=ParentPCESession.getNewReqIDCounter();
		rpLastDomain.setRequestID(requestID);
		rpLastDomain.setPbit(true);
		requestToLastDomain.setRequestParameters(rpLastDomain);
		InterLayer interLayerToLastDomain=new InterLayer();
		interLayerToLastDomain.setIFlag(false);
		SwitchLayer switchLayerToLastDomain= new SwitchLayer();
		SwitchEncodingType setToLastDomain=new SwitchEncodingType();
		
		switchLayerToLastDomain.getSwitchLayers().add(setToLastDomain);
		if (interLayerComputation){
			setToLastDomain.setLSPEncodingType(requestedLSPEncodingType);
			setToLastDomain.setSwitchingType(requestedSwitchingType);				
		}else {
			setToLastDomain.setLSPEncodingType(interDomainLinksEncodingType);
			setToLastDomain.setSwitchingType(interDomainLinksSwitchingType);
		}
		setToLastDomain.setIflag(true);
		requestToLastDomain.setInterLayer(interLayerToLastDomain);
		requestToLastDomain.setSwitchLayer(switchLayerToLastDomain);
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
			//m_resp.addResponse(response);
			return m_resp;
		}
		
		

		//m_resp.addResponse(response);
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
		
		
		for (i=0;i<respList.size();++i){
			if (respList.get(i)==null){
				childrenFailed=true;
			}
			else {
				if(respList.get(i).getResponse(0).getNoPath()!=null){
					log.info("ALGUIEN RESPONDIO NOPATH");
					childrenFailed=true;
				}
				else {					
					ExplicitRouteObject eroInternal =respList.get(i).getResponse(0).getPath(0).geteRO();
					log.info(" eroInternal "+eroInternal.toString());
					ero.addEROSubobjectList(eroInternal.EROSubobjectList);
					UnnumberIfIDEROSubobject unnumberIfDEROSubobj = new UnnumberIfIDEROSubobject(); 
					if (edge_list != null){
						if (j<edge_list.size()){						
							unnumberIfDEROSubobj.setInterfaceID(edge_list.get(j).getSrc_if_id());
							unnumberIfDEROSubobj.setRouterID((Inet4Address)edge_list.get(j).getSrc_router_id());
							log.info(" eroExternal "+unnumberIfDEROSubobj.toString());
							ero.addEROSubobject(unnumberIfDEROSubobj);
							j++;
						}
					}
				}
			}
		}
		if (childrenFailed==true){
			log.warning("Some child has failed");
			NoPath noPath= new NoPath();
			response.setNoPath(noPath);
		}
		else {
			path.seteRO(ero);
			response.addPath(path);			
		}
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



	
	public AlgorithmReservation getReserv() {
		
		return null;
	}
	
	
	public void addXRO(ExcludeRouteObject xro,Request req){
		req.setXro(xro);
	}
	
	private void createNoPath(ComputingResponse m_resp){
		
		NoPath noPath= new NoPath();
		noPath.setNatureOfIssue(ObjectParameters.NOPATH_NOPATH_SAT_CONSTRAINTS);
		NoPathTLV noPathTLV=new NoPathTLV();
		noPath.setNoPathTLV(noPathTLV);			
		noPathTLV.setPCEunavailable(true);
		m_resp.getResponseList().getFirst().setNoPath(noPath);
		

	}
	
	
	
}
