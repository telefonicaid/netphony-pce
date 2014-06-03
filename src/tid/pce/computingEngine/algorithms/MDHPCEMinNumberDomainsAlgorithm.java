package tid.pce.computingEngine.algorithms;

import java.net.Inet4Address;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import org.jgrapht.GraphPath;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.graph.DirectedWeightedMultigraph;

import tid.pce.computingEngine.ComputingRequest;
import tid.pce.computingEngine.ComputingResponse;
import tid.pce.computingEngine.algorithms.multidomain.MDFunctions;
import tid.pce.parentPCE.ChildPCERequestManager;
import tid.pce.parentPCE.ParentPCESession;
import tid.pce.parentPCE.ReachabilityManager;
import tid.pce.pcep.constructs.EndPoint;
import tid.pce.pcep.constructs.EndPointAndRestrictions;
import tid.pce.pcep.constructs.P2MPEndpoints;
import tid.pce.pcep.constructs.P2PEndpoints;
import tid.pce.pcep.constructs.Path;
import tid.pce.pcep.constructs.Request;
import tid.pce.pcep.constructs.Response;
import tid.pce.pcep.messages.PCEPRequest;
import tid.pce.pcep.objects.EndPoints;
import tid.pce.pcep.objects.EndPointsIPv4;
import tid.pce.pcep.objects.ExcludeRouteObject;
import tid.pce.pcep.objects.ExplicitRouteObject;
import tid.pce.pcep.objects.GeneralizedEndPoints;
import tid.pce.pcep.objects.Monitoring;
import tid.pce.pcep.objects.NoPath;
import tid.pce.pcep.objects.ObjectParameters;
import tid.pce.pcep.objects.RequestParameters;
import tid.pce.pcep.objects.subobjects.UnnumberIfIDXROSubobject;
import tid.pce.pcep.objects.subobjects.XROSubObjectValues;
import tid.pce.pcep.objects.subobjects.XROSubobject;
import tid.pce.pcep.objects.tlvs.EndPointIPv4TLV;
import tid.pce.pcep.objects.tlvs.NoPathTLV;
import tid.pce.tedb.InterDomainEdge;
import tid.pce.tedb.MDTEDB;
import tid.pce.tedb.ITMDTEDB;
import tid.pce.tedb.TEDB;
import tid.rsvp.objects.subobjects.IPv4prefixEROSubobject;
import tid.rsvp.objects.subobjects.UnnumberIfIDEROSubobject;

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
	
	
	
	public ComputingResponse call()throws Exception{

		long tiempoini =System.nanoTime();
		ComputingResponse m_resp=new ComputingResponse();
		m_resp.setReachabilityManager(reachabilityManager);
		m_resp.setEncodingType(pathReq.getEcodingType());
		Request req=pathReq.getRequestList().get(0);
		long reqId=req.getRequestParameters().getRequestID();
		log.info("Processing MD Path Computing with MDHPCEMinNumberDomainsAlgorithm (Minimum transit Domains)with Request id: "+reqId);
 
		//Start creating the response
		//We create it now, in case we need to send a NoPath later
		Response response=new Response();
		RequestParameters rp = new RequestParameters();
		rp.setRequestID(reqId);
		response.setRequestParameters(rp);
		
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
		long tiempo2 =System.nanoTime();

		if (source_domain_id.equals(dest_domain_id)){
			NoPath noPath2= new NoPath();
			noPath2.setNatureOfIssue(ObjectParameters.NOPATH_NOPATH_SAT_CONSTRAINTS);
			NoPathTLV noPathTLV=new NoPathTLV();
			noPath2.setNoPathTLV(noPathTLV);				
			response.setNoPath(noPath2);
			m_resp.addResponse(response);
			return m_resp;
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
			destIP = (Inet4Address)edge_list.get(0).getSrc_router_id();
			((EndPointsIPv4) endpointsRequest).setDestIP(destIP);
			
		}else if (EP.getOT()==ObjectParameters.PCEP_OBJECT_TYPE_ENDPOINTS_IPV6){
			//NO IMPLEMENTADO
		}
		
		if (EP.getOT()==ObjectParameters.PCEP_OBJECT_TYPE_GENERALIZED_ENDPOINTS){
			GeneralizedEndPoints  gep=(GeneralizedEndPoints) req.getEndPoints();
			if(gep.getGeneralizedEndPointsType()==ObjectParameters.PCEP_GENERALIZED_END_POINTS_TYPE_P2P){
				EndPointIPv4TLV sourceIPv4TLV = new EndPointIPv4TLV();
				EndPointIPv4TLV destIPv4TLV = new EndPointIPv4TLV();
				sourceIPv4TLV.setIPv4address(source_router_id_addr);
				destIP=(Inet4Address)edge_list.get(0).getSrc_router_id();
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

			}
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
			requestToFirstDomain.setObjectiveFunction(pathReq.getRequestList().get(0).getObjectiveFunction());
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
			request.setObjectiveFunction(pathReq.getRequestList().get(0).getObjectiveFunction());
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
		requestToLastDomain.setObjectiveFunction(pathReq.getRequestList().get(0).getObjectiveFunction());
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
		//System.out.println("TOTAL "+(long)(tiempotot/100));
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
}
