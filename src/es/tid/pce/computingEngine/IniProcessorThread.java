package es.tid.pce.computingEngine;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import es.tid.pce.computingEngine.algorithms.ComputingAlgorithm;
import es.tid.pce.computingEngine.algorithms.ComputingAlgorithmManager;
import es.tid.pce.computingEngine.algorithms.ComputingAlgorithmManagerSSON;
import es.tid.pce.computingEngine.algorithms.DefaultSVECPathComputing;
import es.tid.pce.computingEngine.algorithms.DefaultSinglePathComputing;
import es.tid.pce.computingEngine.algorithms.multiLayer.OperationsCounter;
import es.tid.pce.parentPCE.ChildPCERequestManager;
import es.tid.pce.parentPCE.MD_LSP;
import es.tid.pce.parentPCE.ParentPCESession;
import es.tid.pce.pcep.PCEPProtocolViolationException;
import es.tid.pce.pcep.constructs.EndPoint;
import es.tid.pce.pcep.constructs.ErrorConstruct;
import es.tid.pce.pcep.constructs.MetricPCE;
import es.tid.pce.pcep.constructs.Notify;
import es.tid.pce.pcep.constructs.P2PEndpoints;
import es.tid.pce.pcep.constructs.PCEPIntiatedLSP;
import es.tid.pce.pcep.constructs.Request;
import es.tid.pce.pcep.constructs.Response;
import es.tid.pce.pcep.constructs.StateReport;
import es.tid.pce.pcep.messages.PCEPError;
import es.tid.pce.pcep.messages.PCEPInitiate;
import es.tid.pce.pcep.messages.PCEPMessageTypes;
import es.tid.pce.pcep.messages.PCEPNotification;
import es.tid.pce.pcep.messages.PCEPRequest;
import es.tid.pce.pcep.messages.PCEPResponse;
import es.tid.pce.pcep.objects.Bandwidth;
import es.tid.pce.pcep.objects.EndPoints;
import es.tid.pce.pcep.objects.EndPointsIPv4;
import es.tid.pce.pcep.objects.EndPointsUnnumberedIntf;
import es.tid.pce.pcep.objects.ExplicitRouteObject;
import es.tid.pce.pcep.objects.GeneralizedEndPoints;
import es.tid.pce.pcep.objects.LSP;
import es.tid.pce.pcep.objects.NoPath;
import es.tid.pce.pcep.objects.Notification;
import es.tid.pce.pcep.objects.ObjectParameters;
import es.tid.pce.pcep.objects.ObjectiveFunction;
import es.tid.pce.pcep.objects.PCEPErrorObject;
import es.tid.pce.pcep.objects.PceIdIPv4;
import es.tid.pce.pcep.objects.ProcTime;
import es.tid.pce.pcep.objects.RequestParameters;
import es.tid.pce.pcep.objects.SRP;
import es.tid.pce.pcep.objects.tlvs.EndPointDataPathTLV;
import es.tid.pce.pcep.objects.tlvs.EndPointIPv4TLV;
import es.tid.pce.pcep.objects.tlvs.NoPathTLV;
import es.tid.pce.pcep.objects.tlvs.PathReservationTLV;
import es.tid.pce.pcep.objects.tlvs.SymbolicPathNameTLV;
import es.tid.pce.pcep.objects.tlvs.UnnumberedEndpointTLV;
import es.tid.pce.server.ParentPCERequestManager;
import es.tid.pce.server.communicationpce.CollaborationPCESessionManager;
import es.tid.pce.server.wson.ReservationManager;
import es.tid.rsvp.objects.subobjects.EROSubobject;
import es.tid.rsvp.objects.subobjects.IPv4prefixEROSubobject;
import es.tid.rsvp.objects.subobjects.UnnumberIfIDEROSubobject;
import es.tid.tedb.DomainTEDB;
import es.tid.tedb.ReachabilityManager;
import es.tid.tedb.TEDB;
//import tid.pce.pcep.objects.tlvs.NotificationTLV;
//import tid.pce.pcep.objects.tlvs.subtlvs.NotificationSubTLV;
import es.tid.util.Analysis;
import es.tid.util.UtilsFunctions;

/**
 * IniProcessorThread is devoted to LSP Inititation from the PCE
 * @author ogondio
 *
 */
public class IniProcessorThread extends Thread{
	
	private static int lspIdSeq=1;
	
	private Hashtable <Integer,MD_LSP> multiDomain_LSP_list;

	 /** The queue to read the initation requests
	 */
	private LinkedBlockingQueue<InitiationRequest> lspInitiationRequestQueue;
	 
	private ChildPCERequestManager childPCERequestManager;
	private ReachabilityManager reachabilityManager;

	public LinkedBlockingQueue<InitiationRequest> getLspInitiationRequestQueue() {
		return lspInitiationRequestQueue;
	}

	public void setLspInitiationRequestQueue(
			LinkedBlockingQueue<InitiationRequest> lspInitiationRequestQueue) {
		this.lspInitiationRequestQueue = lspInitiationRequestQueue;
	}

	/**
	 * Indicates whether the thread is running
	 */
	private boolean running;

	

	/**
	 * Logger
	 */
	private Logger log;

	public IniProcessorThread( LinkedBlockingQueue<InitiationRequest> lspInitiationRequestQueue, ReachabilityManager reachabilityManager,ChildPCERequestManager childPCERequestManager ){
		log=Logger.getLogger("PCEServer");
		this.lspInitiationRequestQueue=lspInitiationRequestQueue;
		this.reachabilityManager=reachabilityManager;
		this.childPCERequestManager=childPCERequestManager;
		multiDomain_LSP_list=new Hashtable<Integer,MD_LSP>();
		
	}
	

	/**
	 * The "core" of the Initiation Request
	 */
	public void run(){	
		InitiationRequest iniReq;
		while (running) {
			log.info("Waiting for a new Initiation Request");
			try {
				iniReq=lspInitiationRequestQueue.take();
				PCEPIntiatedLSP pini=iniReq.getLspIniRequest();
				//Check if delete
				if (pini.getRsp().isrFlag()){
					delete(pini.getLsp().getLspId());
				}
				else {
					//Check if need to take path computation
					boolean needComputation=false;
					if (pini.getEro()!=null){
						needComputation=false;
					}else {
						needComputation=true;
					}
					ExplicitRouteObject fullEro;
					if (needComputation){
						fullEro=null;
					}else {
						fullEro=pini.getEro();
					}

					log.info("Provision Multidomain LSP ");

					LinkedList<PCEPInitiate> iniList= new LinkedList<PCEPInitiate>();
					Bandwidth bw=null;

					//Get the Bandwidth
					if (pini.getBandwidth()!=null){
						bw= pini.getBandwidth().duplicate();
					}
					
					
					//Create the domain list
					Iterator<EROSubobject> eroi= fullEro.getEROSubobjectList().iterator();
					Inet4Address lastDomain=null;
					Hashtable<Inet4Address,ExplicitRouteObject> eroSplit = new Hashtable<Inet4Address,ExplicitRouteObject>();
					ExplicitRouteObject lastEro=null;
					while (eroi.hasNext()){
						EROSubobject eroso=eroi.next();
						Inet4Address domain=null;
						if (eroso instanceof IPv4prefixEROSubobject) {
							domain=this.reachabilityManager.getDomain(((IPv4prefixEROSubobject) eroso).getIpv4address());
						}else if (eroso instanceof UnnumberIfIDEROSubobject) {
							domain=this.reachabilityManager.getDomain(((UnnumberIfIDEROSubobject) eroso).getRouterID());
						}
						if (domain!=null){
							if (lastDomain==null){
								ExplicitRouteObject eeero=new ExplicitRouteObject();
								lastEro=eeero;
								eeero.addEROSubobject(eroso);
								eroSplit.put(domain,eeero );
							}else if (domain.equals(lastDomain)){
								lastEro.addEROSubobject(eroso);
							}else {
								ExplicitRouteObject eeero=new ExplicitRouteObject();
								lastEro=eeero;
								eeero.addEROSubobject(eroso);
								eroSplit.put(domain,eeero );
								lastDomain=domain;
							}
						}else {
							lastEro.addEROSubobject(eroso);
						}
						
					}
					
					Enumeration<Inet4Address> domains=eroSplit.keys();
					LinkedList<Object> domainList=new  LinkedList<Object>();
					while(domains.hasMoreElements()){
						Inet4Address domain = domains.nextElement();
						PCEPInitiate ini = new PCEPInitiate();
						PCEPIntiatedLSP inilsp = new PCEPIntiatedLSP();
						ini.getPcepIntiatedLSPList().add(inilsp);
						SRP srp= new SRP();
						srp.setSRP_ID_number(ParentPCESession.getNewReqIDCounter());
						inilsp.setRsp(srp);
						inilsp.setEndPoint(getEndPoints(eroSplit.get(domain)));
						inilsp.setEro(eroSplit.get(domain));
						inilsp.setBandwidth(bw);
						LSP lsp =new LSP();
						lsp.setLspId(0);
						SymbolicPathNameTLV symbolicPathNameTLV_tlv = new SymbolicPathNameTLV();
						String name ="IDEALIST "+ParentPCESession.getNewReqIDCounter();
						byte [] symbolicPathNameID= name.getBytes();
						symbolicPathNameTLV_tlv.setSymbolicPathNameID(symbolicPathNameID);
		
						lsp.setSymbolicPathNameTLV_tlv(symbolicPathNameTLV_tlv);
						inilsp.setLsp(lsp);
						iniList.add(ini);
						domainList.add(domain);
					}
					
					
					try {
	     				childPCERequestManager.executeInitiates(iniList, domainList);
						log.info("SE LLAMOOOOOOO ");
					}catch (Exception e){
						log.severe("PROBLEM SENDING THE INITIATES");
					}
				}
				
				


			} catch (InterruptedException e) {
				log.warning("There is no ini to make");
				e.printStackTrace();
				break;
			}
		}
	}	
//	
//	private void provisionToChildPCES(){
//		log.info("Goint ");
//		LinkedList <ComputingResponse> respList2=null;
//		LinkedList<PCEPInitiate> iniList= new LinkedList<PCEPInitiate>();
//		//Create the domain list
//		Iterator eroi= 
//		for ()
//			for (i=0;i<respList.size();++i){
//				PCEPInitiate ini = new PCEPInitiate();
//				PCEPIntiatedLSP inilsp = new PCEPIntiatedLSP();
//				ini.getPcepIntiatedLSPList().add(inilsp);
//				SRP srp= new SRP();
//				srp.setSRP_ID_number(ParentPCESession.getNewReqIDCounter());
//				inilsp.setRsp(srp);
//				inilsp.setEndPoint(reqList.get(i).getRequest(0).getEndPoints());
//				inilsp.setEro(eroList2.get(i));
//				inilsp.setBandwidth(pathReq.getRequestList().get(0).getBandwidth().duplicate());
//				LSP lsp =new LSP();
//				lsp.setLspId(0);
//				SymbolicPathNameTLV symbolicPathNameTLV_tlv = new SymbolicPathNameTLV();
//				String name ="IDEALIST "+ParentPCESession.getNewReqIDCounter();
//				byte [] symbolicPathNameID= name.getBytes();
//				symbolicPathNameTLV_tlv.setSymbolicPathNameID(symbolicPathNameID);
//
//				lsp.setSymbolicPathNameTLV_tlv(symbolicPathNameTLV_tlv);
//				inilsp.setLsp(lsp);
//				iniList.add(ini);
//			}
//			try {
//				log.info("VAAAAAAAAMOS ");
//				respList2= childPCERequestManager.executeInitiates(iniList, domainList);
//				log.info("SE LLAMOOOOOOO ");
//
//			}catch (Exception e){
//				log.severe("PROBLEM SENDING THE INITIATES");
//				NoPath noPath2= new NoPath();
//				noPath2.setNatureOfIssue(ObjectParameters.NOPATH_NOPATH_SAT_CONSTRAINTS);
//				NoPathTLV noPathTLV=new NoPathTLV();
//				noPath2.setNoPathTLV(noPathTLV);				
//				response.setNoPath(noPath2);
//				m_resp.addResponse(response);
//				return m_resp;
//			}
//		}
//		log.info("A VER QUE SE RESPONDE ");
//		if (respList2==null){
//			log.warning("RESPLIST = NULL");
//			childrenFailed=true;
//		}else {
//			for (i=0;i<respList.size();++i){
//				log.info("viendo  "+i);
//				if (respList2.get(i)==null){
//					childrenFailed=true;
//				}
//			}
//		}
//		if (respList2!=null){
//
//			if (childrenFailed) {
//				log.warning("Some child has failed to initiate");
//				LinkedList<PCEPInitiate> deleteList= new LinkedList<PCEPInitiate>();
//				LinkedList<Object> domainList2 = new LinkedList<Object>();
//				for (i=0;i<respList2.size();++i){
//					if (respList2.get(i)!=null){
//						domainList2.add(domainList.get(i));
//						//Send delete
//						PCEPInitiate ini = new PCEPInitiate();
//						PCEPIntiatedLSP inilsp = new PCEPIntiatedLSP();
//						ini.getPcepIntiatedLSPList().add(inilsp);
//						SRP srp= new SRP();
//						srp.setSRP_ID_number(ParentPCESession.getNewReqIDCounter());
//						srp.setrFlag(true);
//						inilsp.setRsp(srp);
//						inilsp.setEndPoint(reqList.get(i).getRequest(0).getEndPoints());
//						inilsp.setEro(respList2.get(i).getResponse(0).getPath(0).geteRO());
//						LSP lsp =new LSP();
//						lsp.setLspId((respList2.get(i).getReportList().getFirst().getLSP().getLspId()));
//
//						lsp.setSymbolicPathNameTLV_tlv(respList2.get(i).getReportList().getFirst().getLSP().getSymbolicPathNameTLV_tlv());
//						inilsp.setLsp(lsp);
//						deleteList.add(ini);
//
//					}
//				}
//					try {
//						respList= childPCERequestManager.executeInitiates(deleteList, domainList2);	
//					}catch (Exception e){
//						log.severe("PROBLEM SENDING THE DELETES");
//						NoPath noPath2= new NoPath();
//						noPath2.setNatureOfIssue(ObjectParameters.NOPATH_NOPATH_SAT_CONSTRAINTS);
//						NoPathTLV noPathTLV=new NoPathTLV();
//						noPath2.setNoPathTLV(noPathTLV);				
//						response.setNoPath(noPath2);
//						m_resp.addResponse(response);
//						return m_resp;
//					}
//				
//
//			} else {
//				StateReport sr = new StateReport();
//				LSP lsp=new LSP();
//				sr.setLSP(lsp);
//				m_resp.addReport(sr);
//			}
//		
//	}
//	
	public EndPoints getEndPoints(ExplicitRouteObject ero){
		Iterator<EROSubobject> eroi= ero.getEROSubobjectList().iterator();
		EROSubobject eroso;
		GeneralizedEndPoints gep = new GeneralizedEndPoints();
		P2PEndpoints p2pEndpoints = new P2PEndpoints();
		EndPoint sourceEndPoint=null;
		EndPoint destinationEndPoint=null;
		
		gep.setP2PEndpoints(p2pEndpoints);
		while (eroi.hasNext()){
			eroso=eroi.next();
			if (eroso instanceof IPv4prefixEROSubobject) {
				if(sourceEndPoint==null){
					sourceEndPoint=new EndPoint();
					EndPointIPv4TLV epipv4=new EndPointIPv4TLV();
					epipv4.setIPv4address(((IPv4prefixEROSubobject)eroso).getIpv4address());
					sourceEndPoint.setEndPointIPv4TLV(epipv4);					
				}
				else if (!eroi.hasNext()){
					destinationEndPoint=new EndPoint();
					EndPointIPv4TLV epipv4=new EndPointIPv4TLV();
					epipv4.setIPv4address(((IPv4prefixEROSubobject)eroso).getIpv4address());
					destinationEndPoint.setEndPointIPv4TLV(epipv4);
				}
			}else if (eroso instanceof UnnumberIfIDEROSubobject) {
				if(sourceEndPoint==null){
					sourceEndPoint=new EndPoint();
					UnnumberedEndpointTLV epipv4=new UnnumberedEndpointTLV();
					epipv4.setIPv4address(((UnnumberIfIDEROSubobject)eroso).getRouterID());
					epipv4.setIfID(((UnnumberIfIDEROSubobject)eroso).getInterfaceID());
					sourceEndPoint.setUnnumberedEndpoint(epipv4);					
				}
				else if (!eroi.hasNext()){
					destinationEndPoint=new EndPoint();
					UnnumberedEndpointTLV epipv4=new UnnumberedEndpointTLV();
					epipv4.setIfID(((UnnumberIfIDEROSubobject)eroso).getInterfaceID());
					destinationEndPoint.setUnnumberedEndpoint(epipv4);
				}
			}
		}
		p2pEndpoints.setSourceEndPoints(sourceEndPoint);
		p2pEndpoints.setDestinationEndPoints(destinationEndPoint);
		return null;
		
	}
	
	public void delete (int lspID){
		MD_LSP mdlsp= this.multiDomain_LSP_list.get(lspID);
		LinkedList<PCEPInitiate> iniList= new LinkedList<PCEPInitiate>();
		LinkedList<Object> domainList=new  LinkedList<Object>();
		if (mdlsp!=null){
			Enumeration<Inet4Address> domains=mdlsp.getDomainLSPIDMap().keys();
			while(domains.hasMoreElements()){
				Inet4Address domain = domains.nextElement();
				PCEPInitiate ini = new PCEPInitiate();
				PCEPIntiatedLSP inilsp = new PCEPIntiatedLSP();
				ini.getPcepIntiatedLSPList().add(inilsp);
				SRP srp= new SRP();
				srp.setSRP_ID_number(ParentPCESession.getNewReqIDCounter());
				inilsp.setRsp(srp);
				srp.setrFlag(true);
				LSP lsp =new LSP();
				lsp.setLspId(mdlsp.getDomainLSPIDMap().get(domain));
				inilsp.setLsp(lsp);
				iniList.add(ini);
				domainList.add(domain);
			}
			try {
 				childPCERequestManager.executeInitiates(iniList, domainList);
				log.info("SE LLAMOOOOOOO ");
			}catch (Exception e){
				log.severe("PROBLEM SENDING THE INITIATES");
			}
			
		}
	}
	
	
	public synchronized int getID(){
		IniProcessorThread.lspIdSeq+=1;
		return IniProcessorThread.lspIdSeq;
	}
}



