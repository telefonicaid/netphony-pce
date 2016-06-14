package es.tid.pce.server;



import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Timer;
import java.util.concurrent.LinkedBlockingQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.tid.pce.computingEngine.RequestDispatcher;
import es.tid.pce.pcep.PCEPProtocolViolationException;
import es.tid.pce.pcep.messages.PCEPInitiate;
import es.tid.pce.pcep.messages.PCEPMessage;
import es.tid.pce.pcep.messages.PCEPMessageTypes;
import es.tid.pce.pcep.messages.PCEPRequest;
import es.tid.pce.pcep.messages.PCEPResponse;
import es.tid.pce.pcepsession.DeadTimerThread;
import es.tid.pce.pcepsession.GenericPCEPSession;
import es.tid.pce.pcepsession.KeepAliveThread;
import es.tid.pce.pcepsession.PCEPSessionsInformation;
import es.tid.pce.pcepsession.PCEPValues;
import es.tid.tedb.TEDB;
import es.tid.util.UtilsFunctions;

/**
 * PCEP Session between the Child PCE and the parent PCE. 
 * View from the Child PCE
 * @author Oscar Gonzalez de Dios
 *
 */
public class ChildPCESession  extends GenericPCEPSession{
	
	
	/**
	 * Used to store the incoming requests
	 */
	private RequestQueue parentPCERequestQueue;
	
//	private 
	
	//public  ClientRequestManager crm;
	
	private int sessionID;
	
	PCEServerParameters params;
	
	private static long sessionIDCounter=0;
	
	private RequestDispatcher requestDispatcher;
	/**
	 * Flag to indicate that the MessageListener is running
	 */
	boolean running = true;
	
	private TEDB ted;
	
	private Inet4Address domainId;
	
	private Inet4Address pceId;
	
	private Sender sendingThread;
	private LinkedBlockingQueue<PCEPMessage> parentPCESendingQueue; 
	private ParentPCERequestManager childPCERequestManager;
	
	private int state;
	
	private RequestDispatcher PCCRequestDispatcherChild;
	
	private SingleDomainInitiateDispatcher iniDispatcher;

			
	
	//* Constructor of the PCE Session to implement a new RequestDispatcher for the incoming pceprequest from the ParentPCE. * @param s Socket of the PCC-PCE Communication * @param req RequestQueue to send path requests
	public ChildPCESession(RequestDispatcher PCCRequestDispatcherChild, PCEServerParameters params, RequestQueue parentPCERequestQueue, TEDB ted, Timer timer,LinkedBlockingQueue<PCEPMessage> parentPCESendingQueue,ParentPCERequestManager childPCERequestManager, Inet4Address domainId, PCEPSessionsInformation pcepSessionInformation, SingleDomainInitiateDispatcher iniDispatcher) {
		super(pcepSessionInformation);
		this.PCCRequestDispatcherChild=PCCRequestDispatcherChild;
		this.state=PCEPValues.PCEP_STATE_IDLE;
		this.parentPCERequestQueue=parentPCERequestQueue;
		log=LoggerFactory.getLogger(params.getPCEServerLogFile());
		this.requestDispatcher=requestDispatcher;
		this.ted=ted;
		this.params = params;
		this.timer = timer;
		this.parentPCESendingQueue=parentPCESendingQueue;
		this.childPCERequestManager=childPCERequestManager;	
		this.keepAliveLocal=30;
		this.deadTimerLocal=120;
		this.iniDispatcher=iniDispatcher;
		//this.sendingThread = new Sender(new LinkedBlockingQueue<PCEPMessage>(), output);
		try {
			this.domainId=domainId;
			this.pceId=(Inet4Address) Inet4Address.getLocalHost();
			log.info("pceId"+pceId);
			log.info("domainId"+domainId);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	
	
	//* Constructor of the PCE Session * @param s Socket of the PCC-PCE Communication * @param req RequestQueue to send path requests
	
	public ChildPCESession(PCEServerParameters params, RequestQueue parentPCERequestQueue, TEDB ted, Timer timer,LinkedBlockingQueue<PCEPMessage> parentPCESendingQueue,ParentPCERequestManager childPCERequestManager, Inet4Address domainId, PCEPSessionsInformation pcepSessionInformation, SingleDomainInitiateDispatcher iniDispatcher) {
		super(pcepSessionInformation);
		this.state=PCEPValues.PCEP_STATE_IDLE;
		this.parentPCERequestQueue=parentPCERequestQueue;
		log=LoggerFactory.getLogger(params.getPCEServerLogFile());
		this.ted=ted;
		this.params = params;
		this.timer = timer;
		this.parentPCESendingQueue=parentPCESendingQueue;
		this.childPCERequestManager=childPCERequestManager;	
		this.keepAliveLocal=30;
		this.deadTimerLocal=120;
		this.iniDispatcher=iniDispatcher;
		//this.sendingThread = new Sender(new LinkedBlockingQueue<PCEPMessage>(), output);
		try {
			this.domainId=domainId;
			this.pceId=(Inet4Address) Inet4Address.getLocalHost();
			log.info("pceId"+pceId);
			log.info("domainId"+domainId);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Opens a PCEP session sending an OPEN Message
	 * Then, it launches the Keepalive process, the Deadtimer process and
	 * the listener of PCC messages. 
	 */
	public void run() {
		running=true;
		log.info("Opening new PCEP Session with parent PCE "+ params.getParentPCEAddress() + " on port " + params.getParentPCEPort());
		log.info("Local IP address --> "+params.getLocalPceAddress().toString()+" Port --> "+params.getPCEServerPort());
		try {
			Inet4Address addr = (Inet4Address)InetAddress.getByName(params.getLocalPceAddress());
			// Metemos el puerto a 0 para que coja por defecto
			this.socket = new Socket(params.getParentPCEAddress(), params.getParentPCEPort(), addr, 0);
			log.info("Socket opened");
		} catch (IOException e) {
			log.error("Couldn't get I/O for connection to Parent PCE" + params.getParentPCEAddress() );
			killSession();
			return;			
		} 
		initializePCEPSession(false, 15, 200,false,true,domainId,pceId,0);
		log.info("PCE Session succesfully established!!");				
		this.deadTimerT=new DeadTimerThread(this, this.deadTimerLocal);
		startDeadTimer();	
		this.keepAliveT=new KeepAliveThread(out, this.keepAliveLocal);
		startKeepAlive();
		this.sendingThread = new Sender(parentPCESendingQueue, out);	
		this.sendingThread.start();
		//Listen to new messages
		while(running) {
			try {
			   this.msg = readMsg(in);//Read a new message
			}catch (IOException e){
				cancelDeadTimer();
				cancelKeepAlive();
				timer.cancel();
				try {
					in.close();
					out.close();
				} catch (IOException e1) {
				}
				log.warn("Finishing PCEP Session abruptly");
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
					log.warn("Finishing PCEParentSession due to CLOSE");
					killSession();
					return;
					
				case PCEPMessageTypes.MESSAGE_ERROR:
					log.debug("ERROR message received");
					break;
					
				case PCEPMessageTypes.MESSAGE_NOTIFY:
					log.debug("Received NOTIFY message");
					break;
				
				case PCEPMessageTypes.MESSAGE_PCREP:
					log.debug("Received PC RESPONSE message");
					PCEPResponse pcres;
					try {
						pcres=new PCEPResponse(msg);
						Long idr= new Long(pcres.getResponse(0).getRequestParameters().getRequestID());
						log.info("Llega un response del IdRequest "+idr);
						synchronized (childPCERequestManager.locks.get(idr)) {
							childPCERequestManager.notifyResponse(pcres);
						}
						
					} catch (PCEPProtocolViolationException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						break;
					}					
					break;
					
				case PCEPMessageTypes.MESSAGE_PCREQ:
					log.info("PCREQ message from Parent PCE received");
					PCEPRequest p_req;
					try {
						p_req=new PCEPRequest(msg);
					} catch (PCEPProtocolViolationException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						break;
					}
					
					/*RequestProcessor rp=new RequestProcessor(p_req,out, ted,null);	
					this.parentPCERequestQueue.execute(rp);*/
					PCCRequestDispatcherChild.dispathRequests(p_req, out);
					break;
					
				case PCEPMessageTypes.MESSAGE_INITIATE:
					
					log.info("INITIATE RECEIVED");
					PCEPInitiate pcepInitiate = null;
					try 
					{
						log.info(" XXXX try");
						pcepInitiate = new PCEPInitiate(msg);
						log.info("pcepInitiate: "+pcepInitiate);
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

				default:
					log.warn("ERROR: unexpected message, Unknown message received!");
					pceMsg = false;
				}
				
				if (pceMsg) {
					log.info("Reseting Dead Timer as PCEP Message has arrived");
					resetDeadTimer();
				}
			}
		}
	}
		
	public void endSession(){
	}
}
