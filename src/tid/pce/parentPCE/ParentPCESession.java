package tid.pce.parentPCE;

import java.io.IOException;
import java.net.Socket;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.Timer;
import java.util.logging.Logger;

import tid.pce.computingEngine.RequestDispatcher;
import tid.pce.pcep.PCEPProtocolViolationException;
import tid.pce.pcep.messages.PCEPClose;
import tid.pce.pcep.messages.PCEPMessage;
import tid.pce.pcep.messages.PCEPMessageTypes;
import tid.pce.pcep.messages.PCEPNotification;
import tid.pce.pcep.messages.PCEPRequest;
import tid.pce.pcep.messages.PCEPResponse;
import tid.pce.pcep.objects.ObjectParameters;
import tid.pce.pcep.objects.tlvs.ReachabilityTLV;
import tid.pce.pcepsession.DeadTimerThread;
import tid.pce.pcepsession.GenericPCEPSession;
import tid.pce.pcepsession.KeepAliveThread;
import tid.pce.pcepsession.PCEPSessionsInformation;
import tid.pce.pcepsession.PCEPValues;
import tid.pce.tedb.TEDB;
import tid.rsvp.objects.subobjects.EROSubobject;

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
		
	
	
	/**
	 * Constructor of the PCE Session
	 * @param s Socket of the PCC-PCE Communication
	 * @param req RequestQueue to send path requests
	 */
	public ParentPCESession(Socket s, ParentPCEServerParameters params, RequestDispatcher requestDispatcher, TEDB ted, MultiDomainTopologyUpdater mdt, ChildPCERequestManager childPCERequestManager, ReachabilityManager rm, PCEPSessionsInformation pcepSessionManager){
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
	}

	/**
	 * Initiates a Session between the Parent PCE and the peer PCE
	 */
	public void run() {
		initializePCEPSession(params.isZeroDeadTimerPCCAccepted(),params.getMinKeepAliveTimerPCCAccepted(),params.getMaxDeadTimerPCCAccepted(),true,false,null,null,0);
		String domain ="hola";//FIXME de donde saco el domain?... SOLO SE puede sacar de la sesion...
		childPCERequestManager.registerDomainSession(this.remoteDomainId,this.remotePCEId, out);
		//Session is UP now, start timers
		log.info("PCE Session succesfully established!!");				
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
					log.fine("KEEPALIVE message received");
					//The Keepalive message allows to reset the deadtimer
					break;
					
				case PCEPMessageTypes.MESSAGE_CLOSE:
					log.fine("CLOSE message received");
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
					log.warning("We should not receive this kind of message");			
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
									log.info("Reachability TLV List not null and size "+reachabilityTLVList.size());

									for (int ii=0;ii<reachabilityTLVList.size();++ii){
										LinkedList<EROSubobject> EROSubobjectList= reachabilityTLVList.get(ii).getEROSubobjectList();
										log.info("EROSubobjectList size "+EROSubobjectList.size());
										for (int jj=0;jj< EROSubobjectList.size();++jj){
											log.info("ADD EROSO ");
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
					PCEPResponse pcres=new PCEPResponse();
					try {
						pcres.decode(msg);
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
					PCEPRequest p_req=new PCEPRequest();
					try {
						p_req.decode(msg);
						log.info(p_req.toString());
					} catch (PCEPProtocolViolationException e) {
						e.printStackTrace();
						break;
					}
					requestDispatcher.dispathRequests(p_req,out,this.remotePCEId);
					break;

				default:
					log.warning("ERROR: unexpected message received");
					pceMsg = false;
				}
				
				if (pceMsg) {
					log.fine("Reseting Dead Timer as PCEP Message has arrived");
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
		log.info("Removing domain "+this.remoteDomainId);
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