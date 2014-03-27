package tid.pce.server;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Timer;
import java.util.logging.Logger;

import tid.pce.computingEngine.ReportDispatcher;
import tid.pce.computingEngine.RequestDispatcher;
import tid.pce.pcep.PCEPProtocolViolationException;
import tid.pce.pcep.messages.PCEPClose;
import tid.pce.pcep.messages.PCEPInitiate;
import tid.pce.pcep.messages.PCEPMessage;
import tid.pce.pcep.messages.PCEPMessageTypes;
import tid.pce.pcep.messages.PCEPMonReq;
import tid.pce.pcep.messages.PCEPNotification;
import tid.pce.pcep.messages.PCEPReport;
import tid.pce.pcep.messages.PCEPRequest;
import tid.pce.pcep.objects.EndPointsIPv4;
import tid.pce.pcep.objects.OPEN;
import tid.pce.pcepsession.DeadTimerThread;
import tid.pce.pcepsession.GenericPCEPSession;
import tid.pce.pcepsession.KeepAliveThread;
import tid.pce.pcepsession.PCEPSessionsInformation;
import tid.pce.pcepsession.PCEPValues;
import tid.pce.server.communicationpce.CollaborationPCESessionManager;
import tid.pce.server.communicationpce.RollSessionType;
import tid.pce.server.management.PCEManagementSession;
import tid.pce.server.wson.ReservationManager;
import tid.pce.tedb.TEDB;
import tid.util.UtilsFunctions;

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
		
	ReportDispatcher reportDispatcher = null;
	/**
	 * Constructor of the PCE Session
	 * @param s Socket of the PCC-PCE Communication
	 * @param req RequestQueue to send path requests
	 */
	public DomainPCESession(Socket s, PCEServerParameters params, 
			RequestDispatcher requestDispatcher, TEDB ted,NotificationDispatcher notificationDispatcher, 
			ReservationManager rm, PCEPSessionsInformation pcepSessionInformation,
			ReportDispatcher reportDispatcher){
		super(pcepSessionInformation);
		this.setFSMstate(PCEPValues.PCEP_STATE_IDLE);
		log=Logger.getLogger("PCEServer");
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
	}

	/**
	 * Constructor of the PCE Session
	 * @param s Socket of the PCC-PCE Communication
	 * @param req RequestQueue to send path requests
	 */
	public DomainPCESession(Socket s, PCEServerParameters params, RequestDispatcher requestDispatcher, 
			TEDB ted,NotificationDispatcher notificationDispatcher, ReservationManager rm, 
			CollaborationPCESessionManager collaborationPCESessionManager, 
			PCEPSessionsInformation pcepSessionInformation, ReportDispatcher reportDispatcher){
		super(pcepSessionInformation);
		this.setFSMstate(PCEPValues.PCEP_STATE_IDLE);
		log=Logger.getLogger("PCEServer");
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
		
		log.info("Database version we are concerned right now: "+params.getLspDB().getPCCDatabaseVersion(remotePCEId)+"destIp address:"+socket.getRemoteSocketAddress());
		initializePCEPSession(params.isZeroDeadTimerPCCAccepted(),params.getMinKeepAliveTimerPCCAccepted(),params.getMaxDeadTimerPCCAccepted(),false,false,null,null, params.getLspDB().getPCCDatabaseVersion(remotePCEId));
		
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
						log.warning("AYAYAYYA Domain PCE Session");
					}
					log.warning("Finishing PCEP Session abruptly!");
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

					case PCEPMessageTypes.MESSAGE_ERROR:
						log.fine("ERROR message received");
						//Up to now... we do not do anything in the server side
						break;

					case PCEPMessageTypes.MESSAGE_NOTIFY:
						log.info("Received NOTIFY message");			
						PCEPNotification m_not;
						try {
							m_not=new PCEPNotification(this.msg);		
							notificationDispatcher.dispatchNotification(m_not);
						} catch (PCEPProtocolViolationException e1) {
							log.warning("Problem decoding notify message, ignoring message"+e1.getMessage());
							e1.printStackTrace();
						}						
						break;
						
					case PCEPMessageTypes.MESSAGE_REPORT:
						log.info("Received Report message");			
						PCEPReport m_report = null;
						try {
							m_report=new PCEPReport(this.msg);
							if (reportDispatcher != null)
							{
								reportDispatcher.dispatchReport(m_report);
							}
							else
							{
								log.info("Received Report Message when session wasn't stateful");
							}
						} catch (PCEPProtocolViolationException e1) {
							log.warning("Problem decoding report message, ignoring message"+e1.getMessage());
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
								log.severe(UtilsFunctions.exceptionToString(e));
								killSession();
							}
						}
						break;

					case PCEPMessageTypes.MESSAGE_PCREP:
						log.info("Received PC RESPONSE message");
						break;

					case PCEPMessageTypes.MESSAGE_PCREQ:
						log.info("PCREQ message received");
						PCEPRequest p_req=new PCEPRequest();
						try {						
							p_req.decode(msg);

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
					case PCEPMessageTypes.MESSAGE_INTIATE:
						
						PCEPInitiate pcepInitiate = null;
						try 
						{
							pcepInitiate = new PCEPInitiate(msg);
						} 
						catch (PCEPProtocolViolationException e) 
						{
							log.info(UtilsFunctions.exceptionToString(e));
						}
						
						//In this case there is not an LSP in the PCEPInitiate
						//It must be resolved in the PCE, afterwards the PCEPInitiate is completed and
						//sent to the corresponding node
						if ((pcepInitiate.getPcepIntiatedLSPList() == null) || (pcepInitiate.getPcepIntiatedLSPList().get(0) == null) || (pcepInitiate.getPcepIntiatedLSPList().get(0).getLsp() == null))
						{
							EndPointsIPv4 endP_IP = (EndPointsIPv4)pcepInitiate.getPcepIntiatedLSPList().get(0).getEndPoint();
							Socket clientSocket;
							try 
							{
								clientSocket = new Socket(endP_IP.getSourceIP(), 2222);
							
								DataOutputStream out_to_node = new DataOutputStream(clientSocket.getOutputStream());
								initiate_report_pair.put(out_to_node, out);
								
								requestDispatcher.dispathRequests(pcepInitiate, out_to_node);
							}
							catch (IOException e) 
							{
								log.info(UtilsFunctions.exceptionToString(e));
							}
							
						}
						else
						{
							EndPointsIPv4 endP_IP = (EndPointsIPv4)pcepInitiate.getPcepIntiatedLSPList().get(0).getEndPoint();

							try 
							{	
								//Sending message to tn1
								Socket clientSocket = new Socket(endP_IP.getSourceIP(), 2222);			
								log.info("Socket opened");	
								DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
								try 
								{
									pcepInitiate.encode();
									log.info("Sending message At LAST!");
									outToServer.write(pcepInitiate.getBytes());
									outToServer.flush();
								} 
								catch (Exception e) 
								{
									log.info(UtilsFunctions.exceptionToString(e));
								}
							} 
							catch (IOException e) 
							{
								log.info(UtilsFunctions.exceptionToString(e));
								log.severe("Couldn't get I/O for connection to port" + 2222);
							}
							
						}
						
						break;
					case PCEPMessageTypes.MESSAGE_PCMONREP:
						log.info("PCMonREP message received");
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
		}finally{
			log.severe("SESSION "+ internalSessionID+" IS KILLED");
			this.FSMstate=PCEPValues.PCEP_STATE_IDLE;
			endSession();
		}

	}

	private void processOpen(OPEN open) {
		params.getLspDB().proccessOpen(open, remotePCEId);
	}

	public void endSession(){
		if (rm!=null){
			log.severe("Cancelling all pending reservations of session "+ internalSessionID);
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
}