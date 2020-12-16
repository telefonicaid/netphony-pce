package es.tid.pce.server;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.tid.pce.computingEngine.ReportDispatcher;
import es.tid.pce.computingEngine.RequestDispatcher;
import es.tid.pce.pcep.PCEPProtocolViolationException;
import es.tid.pce.pcep.constructs.StateReport;
import es.tid.pce.pcep.messages.PCEPClose;
import es.tid.pce.pcep.messages.PCEPInitiate;
import es.tid.pce.pcep.messages.PCEPMessage;
import es.tid.pce.pcep.messages.PCEPMessageTypes;
import es.tid.pce.pcep.messages.PCEPMonReq;
import es.tid.pce.pcep.messages.PCEPNotification;
import es.tid.pce.pcep.messages.PCEPReport;
import es.tid.pce.pcep.messages.PCEPRequest;
import es.tid.pce.pcep.objects.EndPointsIPv4;
import es.tid.pce.pcep.objects.EndPointsUnnumberedIntf;
import es.tid.pce.pcep.objects.GeneralizedEndPoints;
import es.tid.pce.pcep.objects.OPEN;
import es.tid.pce.pcep.objects.SRP;
import es.tid.pce.pcepsession.DeadTimerThread;
import es.tid.pce.pcepsession.GenericPCEPSession;
import es.tid.pce.pcepsession.KeepAliveThread;
import es.tid.pce.pcepsession.PCEPSessionsInformation;
import es.tid.pce.server.communicationpce.CollaborationPCESessionManager;
import es.tid.pce.server.management.PCEManagementSession;
import es.tid.pce.server.wson.ReservationManager;
import es.tid.tedb.TEDB;
import es.tid.util.UtilsFunctions;
import es.tid.pce.pcepsession.PCEPValues;
import es.tid.pce.server.communicationpce.RollSessionType;

/** Thread that maintains a PCEP Session with one PCC Client. 
 * <p> Reads the first message, and if it is a valid OPEN Message, initiates a new 
 * PCEP Session </p>
 * <p> Then, listens for incoming PCEP messages in a PCEP session and distributes
 * them to the appropiate module </p>
 * <p> Requests will be forwarded to the RequestQueue, which puts them in a queue
 * and are processing by the set of Request Processor Threads </p>
 * 
 * @author Oscar Gonz�lez de Dios
 */
public class DomainPCESession extends GenericPCEPSession{

	private static HashMap<DataOutputStream,DataOutputStream> initiate_report_pair = new HashMap<DataOutputStream,DataOutputStream>();
	
	
	/**
	 * Timer to schedule KeepWait and OpenWait Timers
	 */

	/**
	 * Parent PCE Server General Parameters
	 */
	private PCEServerParameters params;

	private RequestDispatcher requestDispatcher;

	private NotificationDispatcher notificationDispatcher;

	private ReservationManager rm;
	
	private long internalSessionID;
	
	private static long lastInternalSessionID=0;

	private  CollaborationPCESessionManager collaborationPCESessionManager=null;
	
	private SingleDomainInitiateDispatcher iniDispatcher;
		
	ReportDispatcher reportDispatcher = null;
	
	private IniPCCManager iniManager;

	
	public DomainPCESession(Socket s, PCEServerParameters params, 
			RequestDispatcher requestDispatcher, TEDB ted,NotificationDispatcher notificationDispatcher, 
			ReservationManager rm, PCEPSessionsInformation pcepSessionInformation,
			ReportDispatcher reportDispatcher, SingleDomainInitiateDispatcher iniDispatcher){
		super(pcepSessionInformation);
		this.setFSMstate(PCEPValues.PCEP_STATE_IDLE);
		log=LoggerFactory.getLogger("PCEServer");
		log.info("New Domain PCESession: "+s);
		this.socket = s;
		try {
			s.setTcpNoDelay(params.isNodelay());
		} catch (SocketException e) {
			e.printStackTrace();
		}
		this.requestDispatcher=requestDispatcher;
		this.params = params;
		timer=new Timer();
		this.keepAliveLocal=params.getKeepAliveTimer();
		this.deadTimerLocal=params.getDeadTimer();
		this.notificationDispatcher=notificationDispatcher;
		this.rm=rm;
		this.internalSessionID=getNewInternalSessionID();
		this.reportDispatcher = reportDispatcher;
		this.iniDispatcher=iniDispatcher;
		
	}


	public DomainPCESession(Socket s, PCEServerParameters params, RequestDispatcher requestDispatcher, 
			TEDB ted,NotificationDispatcher notificationDispatcher, ReservationManager rm, 
			CollaborationPCESessionManager collaborationPCESessionManager, 
			PCEPSessionsInformation pcepSessionInformation, ReportDispatcher reportDispatcher){
		super(pcepSessionInformation);
		this.setFSMstate(PCEPValues.PCEP_STATE_IDLE);
		log=LoggerFactory.getLogger("PCEServer");
		log.info("New Domain PCESession: "+s);
		this.socket = s;

		try {
			s.setTcpNoDelay(params.isNodelay());
		} catch (SocketException e) {
			e.printStackTrace();
		}
		this.requestDispatcher=requestDispatcher;
		this.params = params;
		timer=new Timer();
		this.keepAliveLocal=params.getKeepAliveTimer();
		this.deadTimerLocal=params.getDeadTimer();
		this.notificationDispatcher=notificationDispatcher;
		this.rm=rm;
		this.internalSessionID=getNewInternalSessionID();
		this.collaborationPCESessionManager=collaborationPCESessionManager;
		this.reportDispatcher = reportDispatcher;
	}

	/**
	 * Initiates a Session between the Domain PCE and the peer PCC
	 */
	public void run() {
		
		//Remove this line and the one below if you don't know what it means
		PCEManagementSession.oneSession.add(this);
		
	
		remotePCEId = (Inet4Address) socket.getInetAddress();
		
		//log.info("Database version we are concerned right now: "+params.getLspDB().getPCCDatabaseVersion(remotePCEId)+"destIp address:"+socket.getRemoteSocketAddress());
		initializePCEPSession(params.isZeroDeadTimerPCCAccepted(),params.getMinKeepAliveTimerPCCAccepted(),params.getMaxDeadTimerPCCAccepted(),false,false,null,null, params.getLspDB()==null ? 0 : params.getLspDB().getPCCDatabaseVersion(remotePCEId));
		if (iniDispatcher!=null){
			this.iniManager=iniDispatcher.getIniManager();
			if (iniManager!=null){
				this.iniManager.getPccOutputStream().put(this.remotePeerIP, this.getOut());

			}
			//FIXME: remove session if it disappears
		}
		if (isSessionStateful && pcepSessionManager.isStateful())
		{
			processOpen(this.open);
		}
		
		
		//Session is UP now, start timers
		log.info("PCE Session succesfully established!!");	
		//Poner que tipo de session es?? como lo se??
		if (collaborationPCESessionManager!=null){
			int roll=RollSessionType.COLLABORATIVE_PCE;/*Como seeeee esl rollllll*/
			//Si el roll es de PCE de backup, tengo que meter el Dataoutput en collaborative PCEs			
			collaborationPCESessionManager.getOpenedSessionsManager().registerNewSession(/*this.remoteDomainId,*//*this.remotePCEId,*/ out,roll);
		}
		this.deadTimerT=new DeadTimerThread(this, this.deadTimerLocal);
		startDeadTimer();	
		this.keepAliveT=new KeepAliveThread(out, this.keepAliveLocal);
		startKeepAlive();

		//Listen to new messages
		try{
			while(this.FSMstate==PCEPValues.PCEP_STATE_SESSION_UP) {
				try {
					if(params.isOptimizedRead()){
						this.msg=readMsgOptimized(in);
					}else {
						this.msg = readMsg(in);//Read a new message	
					}

				}catch (IOException e){
					cancelDeadTimer();
					cancelKeepAlive();
					timer.cancel();
					try {
						in.close();
						out.close();
					} catch (Exception e1) {
						log.warn("AYAYAYYA Domain PCE Session");
					}
					log.warn("Finishing PCEP Session abruptly!");
					return;
				}
				if (this.msg != null) {//If null, it is not a valid PCEP message								
					boolean pceMsg = true;//By now, we assume a valid PCEP message has arrived
					//Depending on the type a different action is performed
					switch(PCEPMessage.getMessageType(this.msg)) {

					case PCEPMessageTypes.MESSAGE_OPEN:
						log.debug("OPEN message received");
						//After the session has been started, ignore subsequent OPEN messages
						log.warn("OPEN message ignored");
						break;

					case PCEPMessageTypes.MESSAGE_KEEPALIVE:
						log.debug("KEEPALIVE message received");
						//The Keepalive message allows to reset the deadtimer
						break;

					case PCEPMessageTypes.MESSAGE_CLOSE:
						log.debug("CLOSE message received");

						try {
							PCEPClose m_close=new PCEPClose(this.msg);		
							log.warn("Closing due to reason "+m_close.getReason());
							this.killSession();
						} catch (PCEPProtocolViolationException e1) {
							log.warn("Problem decoding message, closing session"+e1.getMessage());
							this.killSession();
							return;
						}					
						return;

					case PCEPMessageTypes.MESSAGE_ERROR:
						log.debug("ERROR message received");
						//Up to now... we do not do anything in the server side
						break;

					case PCEPMessageTypes.MESSAGE_NOTIFY:
						log.info("Received NOTIFY message");			
						PCEPNotification m_not;
						try {
							m_not=new PCEPNotification(this.msg);		
							notificationDispatcher.dispatchNotification(m_not);
						} catch (PCEPProtocolViolationException e1) {
							log.warn("Problem decoding notify message, ignoring message"+e1.getMessage());
							e1.printStackTrace();
						}						
						break;
						
					case PCEPMessageTypes.MESSAGE_REPORT:
						log.info("Received Report message");			
						PCEPReport m_report = null;
						try {
							m_report=new PCEPReport(this.msg);
							log.info("Report from "+this.remotePeerIP+": "+m_report);
							
							if (this.localPcepCapability.isStateful()) {
								log.info("Received report from "+this.remotePeerIP);
								PCEPReport pcrpt;
								try {
									pcrpt=new PCEPReport(msg);
									Iterator<StateReport> it= pcrpt.getStateReportList().iterator();
									while (it.hasNext()){
										StateReport sr=it.next();
										SRP srp=sr.getSRP();
										if (srp!=null) {
											log.info("SRP Id: "+ sr.getSRP().getSRP_ID_number());
											Object lock=iniManager.inilocks.get(sr.getSRP().getSRP_ID_number());
											if (lock!=null){
												synchronized (lock) {
													iniManager.notifyReport(sr);
												}	
											}else {
												if (reportDispatcher != null)
												{
													reportDispatcher.dispatchReport(m_report);
												}
											}
										}
										
									}
									
									
									
								} catch (PCEPProtocolViolationException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
									break;
								}					
							}else {
								log.warn("PCE is NOT stateful, ignored report from "+this.remotePeerIP);		
							}
								
						
						} catch (PCEPProtocolViolationException e1) {
							log.warn("Problem decoding report message, ignoring message"+e1.getMessage());
							e1.printStackTrace();
						}
						log.info("Report message decoded!");
						
						//This means it was a report received from an initiate
						//and that a report to the sender should be send
						//This is the whole point of the initiate_report_pair variable
						if (initiate_report_pair.get(out) != null)
						{
							DataOutputStream out_master = initiate_report_pair.get(out);
							try 
							{
								m_report.encode();
								out_master.write(m_report.getBytes());
								out_master.flush();
							} catch (Exception e) {
								log.error(UtilsFunctions.exceptionToString(e));
								killSession();
							}
						}
						break;

					case PCEPMessageTypes.MESSAGE_PCREP:
						log.info("Received PC RESPONSE message");
						break;

					case PCEPMessageTypes.MESSAGE_PCREQ:
						log.info("PCREQ message received");
						PCEPRequest p_req;
						try {						
							p_req=new PCEPRequest(msg);

						} catch (PCEPProtocolViolationException e) {
							e.printStackTrace();
							break;
						}
						//RequestProcessor rp=new RequestProcessor(p_req,out, ted,null);	
						//req.execute(rp);
						requestDispatcher.dispathRequests(p_req,out);
					
						break;
					case PCEPMessageTypes.MESSAGE_PCMONREQ:
						log.info("PCMonREQ message received");
						PCEPMonReq p_mon_req=new PCEPMonReq();
						try {
							p_mon_req.decode(msg);
						} catch (PCEPProtocolViolationException e) {
							e.printStackTrace();
							break;
						}
					case PCEPMessageTypes.MESSAGE_INITIATE:
						
						log.info("INITIATE RECEIVED");
						PCEPInitiate pcepInitiate = null;
						try 
						{
							pcepInitiate = new PCEPInitiate(msg);
							log.info("Initiate from "+this.remotePeerIP+": "+pcepInitiate);
						} 
						catch (PCEPProtocolViolationException e) 
						{
							log.info(UtilsFunctions.exceptionToString(e));
						}
						if (iniDispatcher!=null){
							iniDispatcher.dispathInitiate(pcepInitiate, this.out, this.remotePeerIP);
						}
						
						break;
					case PCEPMessageTypes.MESSAGE_PCMONREP:
						log.info("PCMonREP message received");
						break;	
					default:
						log.warn("ERROR: unexpected message received");
						pceMsg = false;
					}

					if (pceMsg) {
						log.debug("Reseting Dead Timer as PCEP Message has arrived");
						resetDeadTimer();
					}
				} 
			}
		}finally{
			log.error("SESSION "+ internalSessionID+" IS KILLED");
			this.FSMstate=PCEPValues.PCEP_STATE_IDLE;
			endSession();
		}

	}

	private void processOpen(OPEN open) {
		params.getLspDB().proccessOpen(open, remotePCEId);
	}

	public void endSession(){
		if (rm!=null){
			log.error("Cancelling all pending reservations of session "+ internalSessionID);
			rm.cancelAllReservations();	
		}

	}
	
	public synchronized long getNewInternalSessionID(){
		lastInternalSessionID+=1;
		long num=lastInternalSessionID;
		return num;
	}

	public long getInternalSessionID() {
		return internalSessionID;
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
	
public String getDestinationIP(Object endPoint) {
		
		String destinationIP=null;
		
		if (endPoint == null){
			log.info("jm endPoint es null");
			
		}else if (endPoint instanceof EndPointsIPv4){
			log.info("jm endPoint es de tipo EndPointsIPv4");
			destinationIP = ((EndPointsIPv4) endPoint).getDestIP().toString();
			
			
		}else if (endPoint instanceof EndPointsUnnumberedIntf){
			log.info("jm endPoint es de tipo EndPointsUnnumberedIntf");
			destinationIP = ((EndPointsUnnumberedIntf) endPoint).getDestIP().toString();
			
		}else if (endPoint instanceof GeneralizedEndPoints){
			log.info("jm endPoint es de tipo GeneralizedEndPoints");
			destinationIP = ((GeneralizedEndPoints) endPoint).getP2PEndpoints().getDestinationEndPoint().toString();
			
		}else log.info("jm endPoint NO es de tipo conocido");
		
		return destinationIP;
	}
}
