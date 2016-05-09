package es.tid.pce.parentPCE;

import java.io.IOException;
import java.net.Socket;
import java.util.Calendar;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Timer;
import java.util.logging.Logger;

import es.tid.pce.computingEngine.RequestDispatcher;
import es.tid.pce.pcep.PCEPProtocolViolationException;
import es.tid.pce.pcep.constructs.StateReport;
import es.tid.pce.pcep.messages.PCEPClose;
import es.tid.pce.pcep.messages.PCEPInitiate;
import es.tid.pce.pcep.messages.PCEPMessage;
import es.tid.pce.pcep.messages.PCEPMessageTypes;
import es.tid.pce.pcep.messages.PCEPNotification;
import es.tid.pce.pcep.messages.PCEPReport;
import es.tid.pce.pcep.messages.PCEPRequest;
import es.tid.pce.pcep.messages.PCEPResponse;
import es.tid.pce.pcep.objects.ObjectParameters;
import es.tid.pce.pcep.objects.SRP;
import es.tid.pce.pcep.objects.tlvs.ReachabilityTLV;
import es.tid.pce.pcepsession.DeadTimerThread;
import es.tid.pce.pcepsession.GenericPCEPSession;
import es.tid.pce.pcepsession.KeepAliveThread;
import es.tid.pce.pcepsession.PCEPSessionsInformation;
import es.tid.pce.pcepsession.PCEPValues;
import es.tid.rsvp.objects.subobjects.EROSubobject;
import es.tid.tedb.ReachabilityManager;
import es.tid.tedb.TEDB;

/** Thread that maintains a PCEP Session with one PCC Client. 
 * <p> Reads the first message, and if it is a valid OPEN Message, initiates a new 
 * PCEP Session </p>
 * <p> Then, listens for incoming PCEP messages in a PCEP session and distributes
 * them to the appropiate module </p>
 * <p> Requests will be forwarded to the RequestQueue, which puts them in a queue
 * and are processing by the set of Request Processor Threads </p>
 * 
 * @author Oscar Gonz�lez de Dios
 * @author Eduardo Aza��n Teruel
*/
public class ParentPCESession extends GenericPCEPSession{
	
	
	/**
	 * Processes topology updates
	 */
	private MultiDomainTopologyUpdater mdt;
	/**
	 * Timer to schedule KeepWait and OpenWait Timers
	 */
	
	/**
	 * Parent PCE Server General Parameters
	 */
	private ParentPCEServerParameters params;
	
	private RequestDispatcher requestDispatcher;
	
	private ChildPCERequestManager childPCERequestManager;
	
	private ReachabilityManager rm;
	
	/**
	 * Used to obtain new request IDs 
	 */
	private static int reqIDCounter=0;
		
	private MultiDomainInitiateDispatcher mdiniDispatcher;
	
	/**
	 * Constructor of the PCE Session
	 * @param s Socket of the PCC-PCE Communication
	 * @param req RequestQueue to send path requests
	 */
	public ParentPCESession(Socket s, ParentPCEServerParameters params, RequestDispatcher requestDispatcher, MultiDomainInitiateDispatcher mdiniDispatcher, TEDB ted, MultiDomainTopologyUpdater mdt, ChildPCERequestManager childPCERequestManager, ReachabilityManager rm, PCEPSessionsInformation pcepSessionManager){
		super(pcepSessionManager);
		//super("ParentPCESession");
		this.setFSMstate(PCEPValues.PCEP_STATE_IDLE);
		log=Logger.getLogger("PCEServer");
		log.info("New Parent PCESession: "+s);
		this.socket = s;
		this.requestDispatcher=requestDispatcher;
		this.mdt=mdt;
		this.rm=rm;
		this.params = params;
		timer=new Timer();
		this.keepAliveLocal=params.getKeepAliveTimer();
		this.deadTimerLocal=params.getDeadTimer();
		this.childPCERequestManager=childPCERequestManager;
		this.mdiniDispatcher=mdiniDispatcher;
	}

	/**
	 * Initiates a Session between the Parent PCE and the peer PCE
	 */
	public void run() {
		initializePCEPSession(params.isZeroDeadTimerPCCAccepted(),params.getMinKeepAliveTimerPCCAccepted(),params.getMaxDeadTimerPCCAccepted(),true,false,null,null,0);
		String domain ="hola";//FIXME de donde saco el domain?... SOLO SE puede sacar de la sesion...
		childPCERequestManager.registerDomainSession(this.remoteDomainId,this.remotePCEId, out);
		//Session is UP now, start timers
		log.info("PCE Session succesfully established with "+this.remotePeerIP+" and deadTimerLocal "+this.deadTimerLocal);				
		this.deadTimerT=new DeadTimerThread(this, this.deadTimerLocal);
		startDeadTimer();	
		this.keepAliveT=new KeepAliveThread(out, this.keepAliveLocal);
		startKeepAlive();
		
		//Listen to new messages
		while(this.FSMstate==PCEPValues.PCEP_STATE_SESSION_UP) {
			try {
			   this.msg = readMsg(in);//Read a new message
			}catch (IOException e){
//				cancelDeadTimer();
//				cancelKeepAlive();
//				timer.cancel();
//				try {
//					in.close();
//					out.close();
//				} catch (IOException e1) {
//				}
				log.warning("Finishing PCEP Session abruptly");
				this.killSession();
				return;
			}
			if (this.msg != null) {//If null, it is not a valid PCEP message								
				boolean pceMsg = true;//By now, we assume a valid PCEP message has arrived
				//Depending on the type a different action is performed
				switch(PCEPMessage.getMessageType(this.msg)) {
				
				case PCEPMessageTypes.MESSAGE_OPEN:
					log.fine("OPEN message received");
					//After the session has been started, ignore subsequent OPEN messages
					log.warning("OPEN message ignored");
					break;
					
				case PCEPMessageTypes.MESSAGE_KEEPALIVE:
					log.fine("PCEP KEEPALIVE message received from "+this.remotePeerIP);
					//The Keepalive message allows to reset the deadtimer
					break;
					
				case PCEPMessageTypes.MESSAGE_CLOSE:
					log.fine("CLOSE message received from "+this.remotePeerIP);
					try {
						PCEPClose m_close=new PCEPClose(this.msg);
						log.warning("Closing due to reason "+m_close.getReason());
						this.killSession();
					} catch (PCEPProtocolViolationException e1) {
						log.warning("Problem decoding message, closing session"+e1.getMessage());
						this.killSession();
						return;
					}
					return;
					
				case PCEPMessageTypes.MESSAGE_REPORT:
					if (this.localPcepCapability.isStateful()) {
						log.info("Received report from "+this.remotePeerIP);
						PCEPReport pcrpt;
						//log.info("Message: "+()msg.toString());

						try {
							//log.info("Report before");
							pcrpt=new PCEPReport(msg);
							//log.info("Report after");
							Iterator<StateReport> it= pcrpt.getStateReportList().iterator();
							while (it.hasNext()){
								StateReport sr=it.next();
								SRP srp=sr.getSRP();
								if (srp!=null) {
									//log.info("SRP Id: "+ sr.getSRP().getSRP_ID_number());
									Object lock=childPCERequestManager.inilocks.get(sr.getSRP().getSRP_ID_number());
									if (lock!=null){
										synchronized (lock) {
											childPCERequestManager.notifyReport(sr);
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
						log.warning("PCE is NOT stateful, ignored report from "+this.remotePeerIP);		
					}
						
					break;
					
				case PCEPMessageTypes.MESSAGE_ERROR:
					log.fine("ERROR message received");
					//Up to now... we do not do anything in the server side
					break;
					
				case PCEPMessageTypes.MESSAGE_NOTIFY:
					PCEPNotification m_not;
					try {
						m_not=new PCEPNotification(this.msg);
						for (int i=0;i<m_not.getNotifyList().size();++i){
							int notifType=m_not.getNotifyList().get(i).getNotificationList().get(0).getNotificationType();
							if (notifType==ObjectParameters.PCEP_NOTIFICATION_TYPE_PENDING_REQUEST_CANCELLED){
								//Need to cancel request...
							}
							else if (notifType==ObjectParameters.PCEP_NOTIFICATION_TYPE_REACHABILITY){
								log.info("Reachability Notification received");
								LinkedList<ReachabilityTLV>  reachabilityTLVList=m_not.getNotifyList().get(i).getNotificationList().get(0).getReachabilityTLVList();
								//DomainIDTLV domainIDTLV ==m_not.getNotifyList().get(i).getNotificationList().get(0).g
								if (reachabilityTLVList!=null){
									log.fine("Reachability TLV List not null and size "+reachabilityTLVList.size());

									for (int ii=0;ii<reachabilityTLVList.size();++ii){
										LinkedList<EROSubobject> EROSubobjectList= reachabilityTLVList.get(ii).getEROSubobjectList();
										log.fine("EROSubobjectList size "+EROSubobjectList.size());
										for (int jj=0;jj< EROSubobjectList.size();++jj){
										
											//rm.addEROSubobject(remoteDomainId, reachabilityTLVList.get(ii).EROSubobjectList.get(jj));											
										}
									}
																			
								}else {
									log.info("Reachability TLV NULL");
								}
								
							}
							else if (notifType==ObjectParameters.PCEP_NOTIFICATION_TYPE_TOPOLOGY){
								log.finest("Topology Notification received");
								//mdt.processNotification(m_not.getNotifyList().get(i).getNotificationList().get(0),this.remotePCEId,this.remoteDomainId);
							}
							else if (notifType==ObjectParameters.PCEP_NOTIFICATION_TYPE_IT_RESOURCE_INFORMATION){
								mdt.processNotification(m_not.getNotifyList().get(i).getNotificationList().get(0),this.remotePCEId,this.remoteDomainId);
							}
							
						}
					} catch (PCEPProtocolViolationException e1) {
						log.warning("Problem decoding notify message, ignoring message"+e1.getMessage());
						e1.printStackTrace();
					}
					//FIXME: COMPLETARRRRR
					
					break;
					
				case PCEPMessageTypes.MESSAGE_PCREP:
					log.info("Received PC RESPONSE message");
					PCEPResponse pcres;
					try {
						pcres=new PCEPResponse(msg);
						log.info("IdResponse: "+pcres.getResponse(0).getRequestParameters().getRequestID());
						synchronized (childPCERequestManager.locks.get(pcres.getResponse(0).getRequestParameters().getRequestID())) {
							childPCERequestManager.notifyResponse(pcres);
						}
						
					} catch (PCEPProtocolViolationException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						break;
					}					
					break;
					
				case PCEPMessageTypes.MESSAGE_PCREQ:
					log.info("PCREQ message received");
					PCEPRequest p_req;
					try {
						p_req=new PCEPRequest(msg);
						log.info(p_req.toString());
					} catch (PCEPProtocolViolationException e) {
						e.printStackTrace();
						break;
					}
					requestDispatcher.dispathRequests(p_req,out,this.remotePCEId);
					break;
					
				case PCEPMessageTypes.MESSAGE_INITIATE:
					log.info("PCINI message received from "+this.remotePeerIP);
					PCEPInitiate p_ini;
					try {
						p_ini=new PCEPInitiate(msg);
						log.info(p_ini.toString());
					} catch (PCEPProtocolViolationException e) {
						e.printStackTrace();
						break;
					}
                     this.mdiniDispatcher.dispathInitiate(p_ini,out,this.remotePeerIP );
					break;
				default:
					log.warning("ERROR: unexpected message from "+this.remotePeerIP);
					pceMsg = false;
				}
				
				if (pceMsg) {
					log.fine("Reseting Dead Timer as PCEP Message has arrived in "+this.remotePeerIP);
					resetDeadTimer();
				}
			} 
		}

	}
	
//	public void killSession(){
//		log.warning("Killing Session");
//		childPCERequestManager.removeDomain(this.remoteDomainId);
//		timer.cancel();
//		this.endConnections();
//		this.cancelDeadTimer();
//		this.cancelKeepAlive();
//		this.endSession();
//		log.warning("Interrupting thread!!!!");
//		this.interrupt();				
//	}
//	
	public void endSession(){
		log.severe("Removing domain "+this.remoteDomainId+" due to endSession from "+this.remotePeerIP);
		childPCERequestManager.removeDomain(this.remoteDomainId);
	}
	
	/**
	 * USE THIS COUNTER TO GET NEW IDS IN THE REQUESTS
	 * @return
	 */
	synchronized static public int getNewReqIDCounter(){
		int newReqId;
		if (reqIDCounter==0){
			Calendar rightNow = Calendar.getInstance();
			newReqId=rightNow.get(Calendar.SECOND);		
			reqIDCounter=newReqId;
		}
		else {
			newReqId=reqIDCounter+1;
			reqIDCounter=newReqId;
		}	
		return newReqId;
	}
	
}