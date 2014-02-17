package tid.pce.computingEngine.algorithms;

import java.net.Inet4Address;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import org.jgrapht.GraphPath;
import org.jgrapht.graph.DirectedWeightedMultigraph;

import tid.pce.computingEngine.ComputingRequest;
import tid.pce.parentPCE.ChildPCERequestManager;
import tid.pce.parentPCE.ParentPCESession;
import tid.pce.parentPCE.ReachabilityManager;
import tid.pce.pcep.constructs.Path;
import tid.pce.pcep.constructs.Request;
import tid.pce.pcep.constructs.Response;
import tid.pce.pcep.messages.PCEPRequest;
import tid.pce.pcep.messages.PCEPResponse;
import tid.pce.pcep.objects.EndPointsIPv4;
import tid.pce.pcep.objects.ExplicitRouteObject;
import tid.pce.pcep.objects.Monitoring;
import tid.pce.pcep.objects.NoPath;
import tid.pce.pcep.objects.ObjectParameters;
import tid.pce.pcep.objects.RequestParameters;
import tid.pce.pcep.objects.tlvs.NoPathTLV;
import tid.pce.tedb.InterDomainEdge;
import tid.pce.tedb.MDTEDB;
import tid.pce.tedb.TEDB;
import tid.rsvp.objects.subobjects.EROSubobject;
import tid.rsvp.objects.subobjects.SubObjectValues;



/**
 * 
 * @author ogondio
 *
 */
public class ECMPMinTotalHopsPCEAlgorithm implements ComputingAlgorithm{
	private DirectedWeightedMultigraph<Object,InterDomainEdge> networkGraph;
	private Logger log=Logger.getLogger("PCEServer");
	private ComputingRequest pathReq;
	private ChildPCERequestManager childPCERequestManager;
	private ReachabilityManager reachabilityManager;
	private int k;

	private Hashtable<SourceDest,Integer> srcDstPet;

	private LinkedList<Integer> listaPets2;

	public ECMPMinTotalHopsPCEAlgorithm(ComputingRequest pathReq,TEDB ted,ChildPCERequestManager cprm , ReachabilityManager rm){
		this.networkGraph=((MDTEDB)ted).getDuplicatedMDNetworkGraph();
		this.reachabilityManager=rm;
		this.pathReq=pathReq;		
		this.childPCERequestManager=cprm;
		k=2;
	}



	public PCEPResponse call(){
		srcDstPet= new Hashtable<SourceDest,Integer>();

		listaPets2=new LinkedList<Integer>();

		// Initial time of the computation/
		long tiempoini =System.nanoTime();
		// Obtain the initial request
		Request req=pathReq.getRequestList().get(0);
		//Obtain the request ID
		long reqId=req.getRequestParameters().getRequestID();

		// Start creating the response
		// We create it now in case we need to send a NoPath later
		// First, Create the response message
		PCEPResponse m_resp=new PCEPResponse();
		Response response=new Response();
		RequestParameters rp = new RequestParameters();
		//We include the id of the request in the response
		rp.setRequestID(reqId);
		response.setRequestParameters(rp);
		try{			
			//STEP ONE: Obtain the domains of each endPoint
			//FIXME: NOW IT WORKS ONLY WITH IPv4 endpoints
			EndPointsIPv4 epRequest=(EndPointsIPv4) req.getEndPoints();
			Inet4Address source_router_id_addr=epRequest.getSourceIP();
			Inet4Address source_domain_id=this.reachabilityManager.getDomain(source_router_id_addr);
			Inet4Address dest_router_id_addr=epRequest.getDestIP();
			Inet4Address dest_domain_id=this.reachabilityManager.getDomain(dest_router_id_addr);
			log.info("KSP-MinHops Algorithm, processing request"+reqId+" from "+source_router_id_addr+" (domain "+source_domain_id+") to "+ dest_router_id_addr+" (domain "+dest_domain_id+")");
			//CHECK IF DOMAIN_ID ARE NULL!!!!!!
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

			//STEP TWO, compute the k shortest sequence of domains
			DijkstraShortestPathv2<Object,InterDomainEdge>  dsp=new DijkstraShortestPathv2<Object,InterDomainEdge> (networkGraph, source_domain_id, dest_domain_id,k);
			LinkedList<PCEPRequest> reqList= new LinkedList<PCEPRequest>();
			LinkedList<Inet4Address> domainList= new LinkedList<Inet4Address>();

			List<GraphPath<Inet4Address, InterDomainEdge>> pathListo=dsp.getPathListo();
			int numpaths=pathListo.size();
			log.info("NUMPATHS ES "+numpaths);
			GraphPath<Inet4Address,InterDomainEdge> gpl=pathListo.get(0);
			List<InterDomainEdge> edge_list=gpl.getEdgeList();
			int numEdges=edge_list.size();
			//log.info("number of involved domains = "+edge_list.size()+1);
			int i=0;

			//System.out.println("edge 0 es "+edge_list.get(0));

			//Create request for the first domain involved
			EndPointsIPv4 endpointsRequest=new EndPointsIPv4();
			Inet4Address domain = (Inet4Address)edge_list.get(0).getSource();
			endpointsRequest.setSourceIP(source_router_id_addr);
			Inet4Address destIP = (Inet4Address)edge_list.get(0).getSrc_router_id();
			//FIXME: PONGO EL IF NO NUMERADO????
			endpointsRequest.setDestIP(destIP);
			log.info("First part of the LSP 1 is in domain: "+ domain+" from "+ source_router_id_addr+" to "+destIP);
			//FIXME: METRICA? OF? BW?
			long requestID;
			log.info("edge_list.size()"+edge_list.size() );
			boolean first_domain_equal=false;

			PCEPRequest pcreqToFirstDomain=new PCEPRequest();
			Request requestToFirstDomain=new Request();
			requestToFirstDomain.setEndPoints(endpointsRequest);
			RequestParameters rpFirstDomain=new RequestParameters();
			requestID=ParentPCESession.getNewReqIDCounter();
			rpFirstDomain.setRequestID(requestID);
			rpFirstDomain.setPbit(true);
			requestToFirstDomain.setRequestParameters(rpFirstDomain);
			pcreqToFirstDomain.addRequest(requestToFirstDomain);
			reqList.add(pcreqToFirstDomain);
			srcDstPet.put(new SourceDest(endpointsRequest.getSourceIP().hashCode(),endpointsRequest.getDestIP().hashCode()),new Integer(0));
			domainList.add(domain);
			log.info("AAI SRC "+ endpointsRequest.getSourceIP()+" DST "+endpointsRequest.getDestIP());
			log.info("PREPARING request 0 to domain "+domain);

			for (i=1;i<edge_list.size();++i){
				endpointsRequest=new EndPointsIPv4();
				domain = (Inet4Address)edge_list.get(i).getSource();
				endpointsRequest.setSourceIP((Inet4Address)edge_list.get(i-1).getDst_router_id());
				endpointsRequest.setDestIP((Inet4Address)edge_list.get(i).getSrc_router_id());
				log.info("New part of the LSP 1 is in domain: "+ domain+" from "+ edge_list.get(i-1).getDst_router_id()+" to "+edge_list.get(i).getSrc_router_id());
				PCEPRequest pcreq=new PCEPRequest();
				Request request=new Request();
				request.setEndPoints(endpointsRequest);
				RequestParameters rp2=new RequestParameters();
				requestID=ParentPCESession.getNewReqIDCounter();
				rp2.setRequestID(requestID);
				rp2.setPbit(true);
				request.setRequestParameters(rp2);
				pcreq.addRequest(request);
				reqList.add(pcreq);
				log.info("AA SRC "+ endpointsRequest.getSourceIP()+" DST "+endpointsRequest.getDestIP());
				srcDstPet.put(new SourceDest(endpointsRequest.getSourceIP().hashCode(),endpointsRequest.getDestIP().hashCode()),new Integer(i));
				domainList.add(domain);
				log.info("PREPARING request"+i+"  to domain "+domain);

			}
			//Create request for last domain
			EndPointsIPv4 endpointsLastDomain=new EndPointsIPv4();
			Inet4Address Last_domain = (Inet4Address)edge_list.get(i-1).getTarget();
			endpointsLastDomain.setDestIP(dest_router_id_addr);
			Inet4Address last_source_IP = (Inet4Address)edge_list.get(i-1).getDst_router_id();
			log.info("Last part of the LSP 1is in domain: "+ Last_domain+" from "+ last_source_IP+" to "+dest_router_id_addr);

			//FIXME: PONGO EL IF NO NUMERADO????
			endpointsLastDomain.setSourceIP(last_source_IP);
			//FIXME: METRICA? OF? BW?
			PCEPRequest pcreqToLastDomain=new PCEPRequest();
			Request requestToLastDomain=new Request();
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
			log.info("PREPARING request"+i+"  to domain "+Last_domain);

			//log.info("Sending last request to domain "+edge_list.get(i-1));
			//ft.run();
			//childPCERequestManager.addRequest(pcreqToLastDomain, Last_domain);
			reqList.add(pcreqToLastDomain);
			log.info("LD SRC "+ endpointsLastDomain.getSourceIP().hashCode()+" DST "+endpointsLastDomain.getDestIP().hashCode());
			srcDstPet.put(new SourceDest(endpointsLastDomain.getSourceIP().hashCode(),endpointsLastDomain.getDestIP().hashCode()),new Integer(i));
			domainList.add(Last_domain);

			//
			if (numpaths>1){
				gpl=pathListo.get(1);
				edge_list=gpl.getEdgeList();
				//Create request for the first domain involved
				endpointsRequest=new EndPointsIPv4();
				domain = (Inet4Address)edge_list.get(0).getSource();
				endpointsRequest.setSourceIP(source_router_id_addr);
				destIP = (Inet4Address)edge_list.get(0).getSrc_router_id();
				//FIXME: PONGO EL IF NO NUMERADO????
				endpointsRequest.setDestIP(destIP);
				log.info("AA2 SRC "+ endpointsRequest.getSourceIP()+" DST "+endpointsRequest.getDestIP());
				Integer orden=null;
				//Integer orden=srcDstPet.get(new SourceDest(endpointsRequest.getSourceIP().hashCode(),endpointsRequest.getDestIP().hashCode()));
				Enumeration<SourceDest> enu=srcDstPet.keys();
				while(enu.hasMoreElements()){
					SourceDest ss=enu.nextElement();
					if(ss.equals(new SourceDest(endpointsRequest.getSourceIP().hashCode(),endpointsRequest.getDestIP().hashCode()))){
						log.info("IGUAL");
						orden=srcDstPet.get(ss);
						//System.out.println("orden es "+orden);	
						break;
					}else{
						log.info("DIFERENTE");
						orden=null;
					}
				}
				if (orden==null){

					log.info("First part of the LSP 2 is in domain: "+ domain+" from "+ source_router_id_addr+" to "+destIP);
					//FIXME: METRICA? OF? BW?
					pcreqToFirstDomain=new PCEPRequest();
					requestToFirstDomain=new Request();
					requestToFirstDomain.setEndPoints(endpointsRequest);
					rpFirstDomain=new RequestParameters();
					requestID=ParentPCESession.getNewReqIDCounter();
					rpFirstDomain.setRequestID(requestID);
					rpFirstDomain.setPbit(true);
					requestToFirstDomain.setRequestParameters(rpFirstDomain);
					pcreqToFirstDomain.addRequest(requestToFirstDomain);
					reqList.add(pcreqToFirstDomain);
					srcDstPet.put(new SourceDest(endpointsRequest.getSourceIP().hashCode(),endpointsRequest.getDestIP().hashCode()),new Integer(0));
					domainList.add(domain);
					//log.info("Sending 1st request to domain "+domain);
					log.info("PREPARING request"+(srcDstPet.size()-1)+"  to domain "+domain);

					listaPets2.add(srcDstPet.size()-1);
				}else {
					log.info("YA ESTABA");
					listaPets2.add(orden);
				}
				for (i=1;i<edge_list.size();++i){
					//System.out.println("edge "+edge_list.get(i));
					endpointsRequest=new EndPointsIPv4();
					domain = (Inet4Address)edge_list.get(i).getSource();
					endpointsRequest.setSourceIP((Inet4Address)edge_list.get(i-1).getDst_router_id());
					endpointsRequest.setDestIP((Inet4Address)edge_list.get(i).getSrc_router_id());
					log.info("New part of the LSP is in domain: "+ domain+" from "+ edge_list.get(i-1).getDst_router_id()+" to "+edge_list.get(i).getSrc_router_id());
					orden=srcDstPet.get(new SourceDest(endpointsRequest.getSourceIP().hashCode(),endpointsRequest.getDestIP().hashCode()));
					if (orden==null){
						PCEPRequest pcreq=new PCEPRequest();
						Request request=new Request();
						request.setEndPoints(endpointsRequest);
						RequestParameters rp2=new RequestParameters();
						requestID=ParentPCESession.getNewReqIDCounter();
						rp2.setRequestID(requestID);
						rp2.setPbit(true);
						request.setRequestParameters(rp2);
						pcreq.addRequest(request);
						reqList.add(pcreq);
						srcDstPet.put(new SourceDest(endpointsRequest.getSourceIP().hashCode(),endpointsRequest.getDestIP().hashCode()),new Integer(i));
						domainList.add(domain);
						log.info("PREPARING request"+i+"  to domain "+domain);

						listaPets2.add(srcDstPet.size()-1);
					}
					else {
						listaPets2.add(orden);
					}
				}
				//Create request for last domain
				endpointsLastDomain=new EndPointsIPv4();
				Last_domain = (Inet4Address)edge_list.get(i-1).getTarget();
				endpointsLastDomain.setDestIP(dest_router_id_addr);
				last_source_IP = (Inet4Address)edge_list.get(i-1).getDst_router_id();
				log.info("Last part of the LSP is in domain: "+ Last_domain+" from "+ last_source_IP+" to "+dest_router_id_addr);
				//FIXME: PONGO EL IF NO NUMERADO????
				endpointsLastDomain.setSourceIP(last_source_IP);
				log.info("HLAAAA");
				//				Enumeration<SourceDest> enu=srcDstPet.keys();
				//				while(enu.hasMoreElements()){
				//					log.info("MAS");
				//					SourceDest ss=enu.nextElement();
				//					log.info("hay "+ss);	
				//					if(ss.equals(new SourceDest(endpointsLastDomain.getSourceIP().hashCode(),endpointsLastDomain.getDestIP().hashCode()))){
				//						log.info("IGUAL");
				//					}else{
				//						log.info("DIFERENTE");
				//					}
				//				}


				//				log.info("LD2 SRC "+ endpointsLastDomain.getSourceIP().hashCode()+" DST "+endpointsLastDomain.getDestIP().hashCode());
				//				orden=srcDstPet.get(new SourceDest(endpointsLastDomain.getSourceIP().hashCode(),endpointsLastDomain.getDestIP().hashCode()));
				//				log.info("coooo "+srcDstPet.contains(new SourceDest(endpointsLastDomain.getSourceIP().hashCode(),endpointsLastDomain.getDestIP().hashCode())));
				//				System.out.println("containsKey "+srcDstPet.containsKey(new SourceDest(endpointsLastDomain.getSourceIP().hashCode(),endpointsLastDomain.getDestIP().hashCode())));
				enu=srcDstPet.keys();
				while(enu.hasMoreElements()){
					SourceDest ss=enu.nextElement();
					if(ss.equals(new SourceDest(endpointsLastDomain.getSourceIP().hashCode(),endpointsLastDomain.getDestIP().hashCode()))){
						log.info("IGUAL");
						orden=srcDstPet.get(ss);
						//System.out.println("orden es "+orden);	
						break;
					}else{
						log.info("DIFERENTE");
						orden=null;
					}
				}

				if (orden==null){					
					//FIXME: METRICA? OF? BW?
					pcreqToLastDomain=new PCEPRequest();
					requestToLastDomain=new Request();
					requestToLastDomain.setEndPoints(endpointsLastDomain);
					rpLastDomain=new RequestParameters();
					requestID=ParentPCESession.getNewReqIDCounter();
					rpLastDomain.setRequestID(requestID);
					rpLastDomain.setPbit(true);
					requestToLastDomain.setRequestParameters(rpLastDomain);
					pcreqToLastDomain.addRequest(requestToLastDomain);

					//Send the last request
					//cpr=new ChildPCERequest(childPCERequestManager, pcreqToLastDomain, Last_domain);
					//ft=new FutureTask<PCEPResponse>(cpr);
					//requestsToChildrenList.add(ft);
					log.info("PREPARING LAST request"+(srcDstPet.size()-1)+"  to domain "+Last_domain);
					//ft.run();
					//childPCERequestManager.addRequest(pcreqToLastDomain, Last_domain);
					reqList.add(pcreqToLastDomain);
					srcDstPet.put(new SourceDest(endpointsRequest.getSourceIP().hashCode(),endpointsRequest.getDestIP().hashCode()),new Integer(i));
					domainList.add(Last_domain);
					listaPets2.add(srcDstPet.size()-1);

				}
				else {
					log.info("YA ESTABA");
					listaPets2.add(orden);

				}
			}

			LinkedList <PCEPResponse> respList;
			respList=childPCERequestManager.executeRequests(reqList, domainList);

			int caminoOptimo=0;
			//CALCULAR METRICAS....
			int metrica1=0;
			ExplicitRouteObject ero1= new ExplicitRouteObject();
			boolean childrenFailed=false;
			for (i=0;i<numEdges+1;++i){
				if (respList.get(i)==null){
					childrenFailed=true;
				}	else {
					PCEPResponse resp=respList.get(i);
					ExplicitRouteObject eroInternal =respList.get(i).getResponse(0).getPath(0).geteRO();
					LinkedList<EROSubobject> eross =eroInternal.getEROSubobjectList();
					for (int j=0;j<eross.size();j++){
						if(eross.get(j).getType()==SubObjectValues.ERO_SUBOBJECT_UNNUMBERED_IF_ID){
							metrica1=metrica1+1;
						}
					}
					ero1.addEROSubobjectList(eroInternal.EROSubobjectList);	
				}
			}
			int metrica2=0;
			if (numpaths>1){

				ExplicitRouteObject ero2= new ExplicitRouteObject();

				for (i=0;i<numEdges+1;++i){
					Integer numero=listaPets2.get(i);
					if (respList.get(numero)==null){
						childrenFailed=true;
					}	else {
						PCEPResponse resp=respList.get(i);
						ExplicitRouteObject eroInternal =respList.get(i).getResponse(0).getPath(0).geteRO();
						LinkedList<EROSubobject> eross =eroInternal.getEROSubobjectList();
						for (int j=0;j<eross.size();j++){
							if(eross.get(j).getType()==SubObjectValues.ERO_SUBOBJECT_UNNUMBERED_IF_ID){
								metrica2=metrica2+1;
							}
						}
						ero2.addEROSubobjectList(eroInternal.EROSubobjectList);	
					}

				}


				m_resp.addResponse(response);
				Path path=new Path();

				if (childrenFailed==true){
					log.warning("Some child has failed");
					NoPath noPath= new NoPath();
					response.setNoPath(noPath);
				}
				else {
					if (metrica1<=metrica2){					
						path.seteRO(ero1);
					}else {
						path.seteRO(ero2);
					}
					response.addPath(path);
				}
			}
			else {
				m_resp.addResponse(response);
				Path path=new Path();
				if (childrenFailed==true){
					log.warning("Some child has failed");
					NoPath noPath= new NoPath();
					response.setNoPath(noPath);
				}
				else {				
					path.seteRO(ero1);
					response.addPath(path);
				}
			}
			long tiempofin =System.nanoTime();
			long tiempotot=tiempofin-tiempoini;

			log.info("Ha tardado "+tiempotot+" nanosegundos");
			Monitoring monitoring=pathReq.getMonitoring();
			if (monitoring!=null){
				if (monitoring.isProcessingTimeBit()){

				}
			}
		}
		catch (Exception e){
			log.warning("PROBLEM COMPUTING PATH!!");
			//FIXME: Send something different than nopath?
			NoPath noPath= new NoPath();
			noPath.setNatureOfIssue(ObjectParameters.NOPATH_NOPATH_SAT_CONSTRAINTS);
			response.setNoPath(noPath);
			m_resp.addResponse(response);
			return m_resp;
		}
		return m_resp;
	}



	@Override
	public AlgorithmReservation getReserv() {
		// TODO Auto-generated method stub
		return null;
	}
}
