package tid.pce.client;

import java.io.IOException;
import java.net.Socket;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Timer;
import java.util.logging.Level;
import java.util.logging.Logger;

import tid.emulator.node.transport.lsp.LSPKey;
import tid.emulator.node.transport.lsp.LSPManager;
import tid.emulator.node.transport.lsp.te.LSPTE;
import tid.pce.client.tester.LSPConfirmationDispatcher;
import tid.pce.computingEngine.ComputingResponse;
import tid.pce.pcep.PCEPProtocolViolationException;
import tid.pce.pcep.messages.PCEPMessage;
import tid.pce.pcep.messages.PCEPMessageTypes;
import tid.pce.pcep.messages.PCEPResponse;
import tid.pce.pcep.messages.PCEPTELinkConfirmation;
import tid.pce.pcep.messages.PCEPUpdate;
import tid.pce.pcep.objects.OPEN;
import tid.pce.pcep.objects.ObjectParameters;
import tid.pce.pcepsession.DeadTimerThread;
import tid.pce.pcepsession.GenericPCEPSession;
import tid.pce.pcepsession.KeepAliveThread;
import tid.pce.pcepsession.PCEPSessionsInformation;
import tid.pce.pcepsession.PCEPValues;
import tid.rsvp.objects.ERO;
import tid.rsvp.objects.subobjects.EROSubobject;
import tid.util.UtilsFunctions;

/**
 * <p>PCEP Session initiated from the PCC side</p>
 * 
 * @author Oscar Gonzalez de Dios
 *
 */
public class PCCPCEPSession extends GenericPCEPSession{
	
	/**
	 * Manager where the responses of the PCE should be sent
	 */
	public  ClientRequestManager crm;
	/**
	 * Address of the peer PCE
	 */
	private String peerPCE_IPaddress;
	/**
	 * Port of the peer PCE
	 */
	private int peerPCE_port;
	/**
	 * Used to obtain new session IDs 
	 */
	private static long sessionIDCounter=0;
	/**
	 * Used to obtain new request IDs 
	 */
	private static long reqIDCounter=0;
	
	/**
	 * Flag to indicate that the session is up
	 */
	private boolean running = true;
	
	private boolean no_delay=false;

	private  LSPConfirmationDispatcher LSPDispatcher;
	
	private PCEPSessionsInformation pcepSessionManager;
	
	private LSPManager lspManager;
	
	
	/**
	 * Constructor of the PCE Session
	 * @param ip IP Address of the peer PCE
	 * @param port Port of the peer PCE
	 */
	public PCCPCEPSession(String ip, int port, boolean no_delay, PCEPSessionsInformation pcepSessionManager) {
		super(pcepSessionManager);
		this.setFSMstate(PCEPValues.PCEP_STATE_IDLE);
		log=Logger.getLogger("PCCClient");
		log.setLevel(Level.ALL);
		this.peerPCE_IPaddress=ip;
		this.peerPCE_port=port;
		crm= new ClientRequestManager();
		this.keepAliveLocal=60;
		this.deadTimerLocal=240;
		timer=new Timer();
		this.no_delay=no_delay;
		this.pcepSessionManager=pcepSessionManager;
		this.lspManager = null;
		
	}
	
	/**
	 * This construcutor also has the LSPManager
	 * It should be used for stateful PCE
	 * Constructor of the PCE Session
	 * @param ip IP Address of the peer PCE
	 * @param port Port of the peer PCE
	 */
	public PCCPCEPSession(String ip, int port, boolean no_delay, PCEPSessionsInformation pcepSessionManager, LSPManager lspManager) {
		super(pcepSessionManager);
		this.setFSMstate(PCEPValues.PCEP_STATE_IDLE);
		log=Logger.getLogger("PCCClient");
		log.setLevel(Level.ALL);
		this.peerPCE_IPaddress=ip;
		this.peerPCE_port=port;
		crm= new ClientRequestManager();
		this.keepAliveLocal=30;
		this.deadTimerLocal=120;
		timer=new Timer();
		this.no_delay=no_delay;
		this.pcepSessionManager=pcepSessionManager;
		this.lspManager = lspManager;
		this.lspManager.setOut(out);
		
	}
	
	
	/**
	 * Constructor of the PCE Session
	 * @param ip IP Address of the peer PCE
	 * @param port Port of the peer PCE
	 */
	public PCCPCEPSession(String ip, int port, boolean no_delay, LSPConfirmationDispatcher LSPDispatcher, PCEPSessionsInformation pcepSessionManager) {
		super(pcepSessionManager);
		this.setFSMstate(PCEPValues.PCEP_STATE_IDLE);
		log=Logger.getLogger("PCCClient");
		log.setLevel(Level.ALL);
		this.peerPCE_IPaddress=ip;
		this.peerPCE_port=port;
		crm= new ClientRequestManager();
		this.keepAliveLocal=30;
		this.deadTimerLocal=120;
		timer=new Timer();
		this.no_delay=no_delay;
		//LSPcreateIP = new LigthPathCreateIP();
		this.LSPDispatcher = LSPDispatcher;
		
	}
	/**
	 * Opens a PCEP session sending an OPEN Message
	 * Then, it launches the Keepalive process, the Deadtimer process and
	 * the listener of PCC messages. 
	 */
	public void run() {
		running=true;
		log.info("Opening new PCEP Session with host "+ peerPCE_IPaddress + " on port " + peerPCE_port);
		try {
			this.socket = new Socket(peerPCE_IPaddress, peerPCE_port);
			if (no_delay){
				this.socket.setTcpNoDelay(true);
				log.info("No delay activated");
			}
			log.info("Socket opened");
		} catch (IOException e) {
			log.info(UtilsFunctions.exceptionToString(e));
			log.severe("Couldn't get I/O for connection to " + peerPCE_IPaddress + " in port "+ peerPCE_port);
			//FIXME: Salir de manera limpia
			System.exit(1);
		} 

		initializePCEPSession(false, 15, 200,false,false,null,null, pcepSessionManager.isStateful()?lspManager.getDataBaseVersion():((long)0));
		
		crm.setDataOutputStream(out);
		log.info("PCE Session "+this.toString()+" succesfully established!!");
		this.deadTimerT=new DeadTimerThread(this, this.deadTimerLocal);
		startDeadTimer();	
		this.keepAliveT=new KeepAliveThread(out, this.keepAliveLocal);
		startKeepAlive();
		
		log.info("Now PCE will be informed of all our LSPs");
		log.info("open:"+open);
		if ((pcepSessionManager.isStateful()) && (open != null) && (!(avoidSync(this.open))))
		{
			log.info("Actually sending params");
			sendPCELSPParameters(true, ObjectParameters.LSP_OPERATIONAL_UP, false);
		}
		
		//Listen to new messages
		while(running) {
			try {
				log.info("Waiting for new PCEP Messages!");
			   this.msg = readMsg(in);//Read a new message
			}catch (IOException e){
				cancelDeadTimer();
				cancelKeepAlive();
				try {
					in.close();
					out.close();
				} catch (IOException e1) {
				}
				log.warning("Finishing PCEP Session abruptly!");
				return;
			}
			if (this.msg != null) {//If null, it is not a valid PCEP message								
				log.info("New PCEP Message Read!");
				boolean pceMsg = true;//By now, we assume a valid PCEP message has arrived
				//Depending on the type a different action is performed
				switch(PCEPMessage.getMessageType(this.msg)) {
				
				case PCEPMessageTypes.MESSAGE_OPEN:
					log.info("OPEN message received");
					//After the session has been started, ignore subsequent OPEN messages
					log.warning("OPEN message ignored");
					break;
					
				case PCEPMessageTypes.MESSAGE_KEEPALIVE:
					log.info("KEEPALIVE message received");
					//The Keepalive message allows to reset the deadtimer
					break;
					
				case PCEPMessageTypes.MESSAGE_CLOSE:
					log.info("CLOSE message received");
					killSession();
					return;
					/**************************************************************************/
					/*                CONFIRMACION MULTILAYER                         */
					
					// CONFIRMATION FROM THE VNTM LSP ESTABLISHEMENT
				case PCEPMessageTypes.MESSAGE_TE_LINK_SUGGESTION_CONFIRMATION:
					log.fine("Confirmation from the VNMT received!!!");
					//Establish the TE LINK in the UPPER LAYER
					PCEPTELinkConfirmation telinkconf;
					
					try {
						telinkconf = new PCEPTELinkConfirmation(this.msg);
						//LSPcreateIP.createLigthPath(telinkconf.getPath().geteRO().getEROSubobjectList());
						LSPDispatcher.dispatchLSPConfirmation(telinkconf.getPath(), telinkconf.getLSPid());
					}catch (PCEPProtocolViolationException e) {
						e.printStackTrace();
					}
					//NOTIFY THE CANGE TO THE NETWORK EMULATOR
					break;
					
					
				/**********************************************************************/	
									
				case PCEPMessageTypes.MESSAGE_ERROR:
					log.info("ERROR message received");
					break;
					
				case PCEPMessageTypes.MESSAGE_NOTIFY:
					log.info("Received NOTIFY message");
					break;
					
				case PCEPMessageTypes.MESSAGE_UPDATE:
					log.info("Received Message Update");
					if (pcepSessionManager.isStateful())
					{
						try 
						{
							PCEPUpdate pupdt=new PCEPUpdate(msg);
							lspManager.updateLSP(pupdt);
						} 
						catch (PCEPProtocolViolationException e) 
						{
							// TODO Auto-generated catch block
							e.printStackTrace();
							break;
						}	
						break;
					}
					else
					{
						log.warning("Received Update message and sessions is not stateful");
						break;
					}
					
				case PCEPMessageTypes.MESSAGE_PCREP:
					log.info("Received PCE RESPONSE message");
					long timeIni=System.nanoTime();
					PCEPResponse pcres=new PCEPResponse();
					try {
						pcres.decode(msg);
						
						log.info("IdResponse: "+pcres.getResponse(0).getRequestParameters().getRequestID());
						Object lock=crm.locks.get(new Long(pcres.getResponse(0).getRequestParameters().getRequestID()));
						if (lock!=null){
							synchronized (lock) {
								crm.notifyResponse(pcres, timeIni);
							}							
						}
						else{
							log.warning("Ha llegado la response con ID: "+pcres.getResponse(0).getRequestParameters().getRequestID()+" Y el lock era null.");
						}
						
					} catch (PCEPProtocolViolationException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						break;
					}					
					break;
					
				case PCEPMessageTypes.MESSAGE_PCREQ:
					log.info("PCREQ message received");
					break;

				default:
					log.info("ERROR: unexpected message PCCCEPSession with type : "+PCEPMessage.getMessageType(this.msg));
					pceMsg = false;
				}
				
				if (pceMsg) {
					log.info("Reseting Dead Timer as PCEP Message has arrived");
					resetDeadTimer();
				}
			}
		}
	}
	
	public int getPeerPCE_port() {
		return peerPCE_port;
	}

	public String getPeerPCE_IPaddress() {
		return peerPCE_IPaddress;
	}
	synchronized public long getNewSessionIDCounter(){
		long newSessionId;
		if (sessionIDCounter==0){
			Calendar rightNow = Calendar.getInstance();
			newSessionId=rightNow.get(Calendar.SECOND);		
			sessionIDCounter=newSessionId;
		}
		else {
			if (sessionIDCounter>=0xFFFFFFFDL){
				newSessionId=1;
				sessionIDCounter=newSessionId;
			}else{
				newSessionId=sessionIDCounter+1;
				sessionIDCounter=newSessionId;	
			}
		}	
		return newSessionId;
	}
	
	synchronized static public long getNewReqIDCounter(){
		long newReqId;
		if (reqIDCounter==0){
			Calendar rightNow = Calendar.getInstance();
			newReqId=rightNow.get(Calendar.SECOND);		
			reqIDCounter=newReqId;
		}
		else {
			if (reqIDCounter>=0xFFFFFFFDL){
				newReqId=1;
				reqIDCounter=newReqId;
			}else {
				newReqId=reqIDCounter+1;
				reqIDCounter=newReqId;	
			}
			
		}	
		return newReqId;
	}
	
	public void endSession(){
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return "("+peerPCE_IPaddress+" - "+ peerPCE_port+")";
	}
	
	/**
	 * Initiates a Session between the Domain PCE and the peer PCC
	 */
	private void sendPCELSPParameters(boolean dFlag, int opFlags, boolean rFlag)
	{	
		Hashtable<LSPKey, LSPTE> lsps = lspManager.getLSPList();
		Enumeration<LSPKey> keys = lsps.keys();
		log.info("Thought this node there are "+lsps.size()+" LSPs");
		while( keys.hasMoreElements() )
	    {
			 LSPKey key = keys.nextElement();
		     LSPTE lspte = lsps.get(key);
		     //Means this PCC owns this LSP and must send information to PCE
		     if (lspte.getIdSource().equals(lspManager.getLocalIP()))
		     {
		    	 lspManager.getNotiLSP().notify(lspte, true, true, false, true);
	    	 }
	     }
		log.info("Sending las PCEPReport to stop sync");
		
		LSPTE lspte = new LSPTE(0, lspManager.getLocalIP(), lspManager.getLocalIP(), false, 0, 0, 0);
		ERO ero = new ERO();
		ero.setEroSubobjects(new LinkedList<EROSubobject>());
		lspte.setEro(ero);
		
		lspManager.getNotiLSP().notify(lspte, true, true, false, true);
		 
	}
	
	private boolean avoidSync(OPEN open) 
	{
		log.info("Open :"+open.toString());
		long dataBaseId = open.getLsp_database_version_tlv().getLSPStateDBVersion();
		log.info("dataBaseId:"+dataBaseId);
		log.info("lspManager.getDataBaseVersion() :"+lspManager.getDataBaseVersion() );
		if ((lspManager.getDataBaseVersion() == dataBaseId)&&(lspManager.getDataBaseVersion()!=0))
		{
			return true;
		}
		else
		{
			return false;
		}
	}
}
