package es.tid.pce.computingEngine;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.Socket;
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
import es.tid.pce.parentPCE.MDLSPDB.MultiDomainLSPDB;
import es.tid.pce.parentPCE.MDLSPDB.SaveLSPinRedis;
import es.tid.pce.pcep.PCEPProtocolViolationException;
import es.tid.pce.pcep.constructs.EndPoint;
import es.tid.pce.pcep.constructs.ErrorConstruct;
import es.tid.pce.pcep.constructs.MetricPCE;
import es.tid.pce.pcep.constructs.Notify;
import es.tid.pce.pcep.constructs.P2PEndpoints;
import es.tid.pce.pcep.constructs.PCEPIntiatedLSP;
import es.tid.pce.pcep.constructs.Path;
import es.tid.pce.pcep.constructs.Request;
import es.tid.pce.pcep.constructs.Response;
import es.tid.pce.pcep.constructs.StateReport;
import es.tid.pce.pcep.messages.PCEPError;
import es.tid.pce.pcep.messages.PCEPInitiate;
import es.tid.pce.pcep.messages.PCEPMessageTypes;
import es.tid.pce.pcep.messages.PCEPNotification;
import es.tid.pce.pcep.messages.PCEPReport;
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
import es.tid.pce.pcep.objects.tlvs.subtlvs.SymbolicPathNameSubTLV;
import es.tid.pce.server.ParentPCERequestManager;
import es.tid.pce.server.SD_LSP;
import es.tid.pce.server.SaveLSPinRedisSingleDom;
import es.tid.pce.server.IniPCCManager;
import es.tid.pce.server.communicationpce.CollaborationPCESessionManager;
import es.tid.pce.server.lspdb.SingleDomainLSPDB;
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
public class SingleDomainIniProcessorThread extends Thread{
	
	private static int lspIdSeq=1;
	
	private SingleDomainLSPDB singleDomainLSPDB;

	 /** The queue to read the initation requests
	 */
	private LinkedBlockingQueue<InitiationRequest> lspInitiationRequestQueue;
	 
	private IniPCCManager iniManager;
	
	

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

	SaveLSPinRedisSingleDom savelsp;
	

	/**
	 * Logger
	 */
	private Logger log;

	public SingleDomainIniProcessorThread( LinkedBlockingQueue<InitiationRequest> lspInitiationRequestQueue, SingleDomainLSPDB singleDomainLSPDB, IniPCCManager iniManager){
		log=Logger.getLogger("PCEServer");
		this.lspInitiationRequestQueue=lspInitiationRequestQueue;
		this.singleDomainLSPDB=singleDomainLSPDB;
		this.iniManager=iniManager;	
		//FIXME
		savelsp=new SaveLSPinRedisSingleDom();
		savelsp.configure(singleDomainLSPDB, "127.0.0.1",6379);
		running=true;
		
	}
	

	/**
	 * The "core" of the Initiation Request
	 */
	public void run(){	
		InitiationRequest iniReq;
		ExplicitRouteObject fullEro=null;
		while (running) {
			
			
			log.info("Waiting for a new Initiation Request");
			
			
			try {
				iniReq=lspInitiationRequestQueue.take();
				
				PCEPIntiatedLSP pini=iniReq.getLspIniRequest();
				PCEPIntiatedLSP piniToNode = new PCEPIntiatedLSP();
				SRP srp= new SRP();
				//fixme: sacar de otro sitio
				long sRP_ID_number_to_node =ParentPCESession.getNewReqIDCounter() ;
				srp.setSRP_ID_number(sRP_ID_number_to_node);
				piniToNode.setRsp(srp);
				srp.setrFlag(pini.getRsp().isrFlag());
				
				long sRP_ID_number = pini.getRsp().getSRP_ID_number();
				log.info("Starting to process Init with SRP ID "+sRP_ID_number);

				
				//Check if delete
				if (pini.getRsp().isrFlag()){
					delete(pini.getLsp().getLspId());
				}
				else {
					//Check if need to take path computation
					boolean needComputation=false;
					if (pini.getEro()!=null){
						needComputation=false;
						log.info("Provision Simple LSP  with ERO");
					}else {
						needComputation=true;
						log.info("Provision Simple LSP that needs computation");
					}
					if (needComputation){
						fullEro=null;
					}else {
						fullEro=pini.getEro();
						log.info("XXXX fullEro=pini.getEro();"+fullEro);
					}
					piniToNode.setEro(fullEro);
					LSP lsp= new LSP();
					lsp.setSymbolicPathNameTLV_tlv(pini.getLsp().getSymbolicPathNameTLV_tlv());
					piniToNode.setLsp(lsp);
					LinkedList<PCEPInitiate> iniList= new LinkedList<PCEPInitiate>();
					log.info("Creating the bandwidth");
					Bandwidth bw=null;

					//Get the Bandwidth
					if (pini.getBandwidth()!=null){
						bw= pini.getBandwidth().duplicate();
					}
					piniToNode.setBandwidth(bw);
					piniToNode.setEndPoint(pini.getEndPoint());
					
					

					PCEPInitiate pcepInitiate = new PCEPInitiate();
					pcepInitiate.getPcepIntiatedLSPList().add(piniToNode);
					
					//In this case there is not an LSP in the PCEPInitiate
					//It must be resolved in the PCE, afterwards the PCEPInitiate is completed and
					//sent to the corresponding node
						log.info("INITIATE with info, sending to node");
						String miIP;	
						Inet4Address ip;
					if (pini.getEndPoint()!= null){
							log.info("jm endPoint NO es null");								
						}
					else {
						log.info("jm endPoint es null");
					}
						miIP=getSourceIP(pini.getEndPoint());
						log.info("jm ver ip to socket connect: "+miIP);
				      
						ip=(Inet4Address)Inet4Address.getByName(miIP);
				
						StateReport sr=this.iniManager.newIni(pcepInitiate, ip);
						if (sr!=null){

		     				int lspId = sr.getLSP().getLspId();
		     				//multiDomainLSPDB.getMultiDomain_LSP_list().put(lspId, mdlsp);
							log.info("SE LLAMOOOOOOO ");
							StateReport srmd = new StateReport();
							SRP srpp = new SRP();
							srpp.setSRP_ID_number(sRP_ID_number);
							srmd.setSRP(srpp);
							LSP lspp = new LSP();
							lspp.setLspId(lspId);
							srmd.setLSP(lspp);
							Path p=new Path();
							p.seteRO(fullEro);
							srmd.setPath(p);
							
							log.info(" XXXX fullEro: "+fullEro);
							log.info(" XXXX srmd: "+srmd);
							
							PCEPReport rep = new PCEPReport();
							rep.getStateReportList().add(srmd);
							rep.encode();
							
							log.info("Mando: "+ rep.toString());					
							iniReq.getOut().write(rep.getBytes());
							iniReq.getOut().flush();
							SD_LSP sd_lsp=new SD_LSP();
							sd_lsp.setpLSPID(lspId);
							sd_lsp.setFullERO(fullEro);
							sd_lsp.setStateRport(sr);
							singleDomainLSPDB.getSingleDomain_LSP_list().put(lspId,sd_lsp );
							this.savelsp.run();
						}else {
							//FIXME: Mandar error
						}
				}
						
			}catch (Exception e){
				e.printStackTrace();
				log.severe("PROBLEM SENDING THE INITIATES");
			}
		}
		
				
				

				

		}

	
	
	public String getSourceIP(Object endPoint) {
		
		String sourceIP=null;
		
		if (endPoint == null){
			log.info("jm endPoint es null");
			
		}else if (endPoint instanceof EndPointsIPv4){
			log.info("jm endPoint es de tipo EndPointsIPv4");
			sourceIP = ((EndPointsIPv4) endPoint).getSourceIP().toString();
			
			
		}else if (endPoint instanceof EndPointsUnnumberedIntf){
			log.info("jm endPoint es de tipo EndPointsUnnumberedIntf");
			sourceIP = ((EndPointsUnnumberedIntf) endPoint).getSourceIP().toString();
			
		}else if (endPoint instanceof GeneralizedEndPoints){
			log.info("jm endPoint es de tipo GeneralizedEndPoints");
			sourceIP = ((GeneralizedEndPoints) endPoint).getP2PEndpoints().getSourceEndPoint().toString();
			
		}else log.info("jm endPoint NO es de tipo conocido");
		
		return sourceIP;
	}
	

	public EndPoints getEndPoints(ExplicitRouteObject ero){
		log.info("Getting EndPoints");
		
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
					destinationEndPoint=new EndPoint();
					UnnumberedEndpointTLV epipv42=new UnnumberedEndpointTLV();
					epipv42.setIPv4address(((UnnumberIfIDEROSubobject)eroso).getRouterID());
					epipv42.setIfID(((UnnumberIfIDEROSubobject)eroso).getInterfaceID());
					destinationEndPoint.setUnnumberedEndpoint(epipv42);
				}
				else {
					destinationEndPoint=new EndPoint();
					UnnumberedEndpointTLV epipv4=new UnnumberedEndpointTLV();
					epipv4.setIPv4address(((UnnumberIfIDEROSubobject)eroso).getRouterID());
					epipv4.setIfID(((UnnumberIfIDEROSubobject)eroso).getInterfaceID());
					destinationEndPoint.setUnnumberedEndpoint(epipv4);
				}
			}
		}
		p2pEndpoints.setSourceEndPoints(sourceEndPoint);
		p2pEndpoints.setDestinationEndPoints(destinationEndPoint);
		return gep;
		
	}
	
	public void delete (int lspID){
		log.info("GOING TO DELTE "+lspID);
		SD_LSP sdlsp= this.singleDomainLSPDB.getSingleDomain_LSP_list().get(lspID);
		if (sdlsp==null) {
			log.severe("LSP is NULL!!");
		}
		LinkedList<PCEPInitiate> iniList= new LinkedList<PCEPInitiate>();
		//LinkedList<Object> domainList=new  LinkedList<Object>();
		if (sdlsp!=null){
			
				log.info("LSP OK in DB!!!");
			
							
				PCEPInitiate ini = new PCEPInitiate();
				PCEPIntiatedLSP inilsp = new PCEPIntiatedLSP();
				ini.getPcepIntiatedLSPList().add(inilsp);
				SRP srp= new SRP();
				srp.setSRP_ID_number(ParentPCESession.getNewReqIDCounter());
				inilsp.setRsp(srp);
				srp.setrFlag(true);
				LSP lsp =new LSP();
				SymbolicPathNameTLV symbolicPathNameTLV_tlv = new SymbolicPathNameTLV();
				
				StateReport SR =sdlsp.getStateRport();
				
				symbolicPathNameTLV_tlv.setSymbolicPathNameID(SR.getLSP().getSymbolicPathNameTLV_tlv().getSymbolicPathNameID() );
	        	lsp.setLspId(sdlsp.getpLSPID()); 
				lsp.setSymbolicPathNameTLV_tlv(symbolicPathNameTLV_tlv);
				inilsp.setLsp(lsp);
				iniList.add(ini);
				//domainList.add(domain);
		
			try {
				log.info("GOING TO send the deletes of "+lspID);
 				//childPCERequestManager.executeInitiates(iniList, domainList);
				log.info("Removing SD LSP "+lspID);
				 this.singleDomainLSPDB.getSingleDomain_LSP_list().remove(lspID); 
			}catch (Exception e){
				log.severe("PROBLEM SENDING THE DELETES");
			}
			
		}
	}
	
	
	public static synchronized int getID(){
		SingleDomainIniProcessorThread.lspIdSeq+=1;
		return SingleDomainIniProcessorThread.lspIdSeq;
	}
	
	
	
	
}



