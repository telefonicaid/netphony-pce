package es.tid.pce.pcepsession;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.tid.pce.management.PcepCapability;
import es.tid.pce.pcep.PCEPProtocolViolationException;
import es.tid.pce.pcep.constructs.ErrorConstruct;
import es.tid.pce.pcep.messages.PCEPClose;
import es.tid.pce.pcep.messages.PCEPError;
import es.tid.pce.pcep.messages.PCEPKeepalive;
import es.tid.pce.pcep.messages.PCEPMessage;
import es.tid.pce.pcep.messages.PCEPMessageTypes;
import es.tid.pce.pcep.messages.PCEPOpen;
import es.tid.pce.pcep.objects.Association;
import es.tid.pce.pcep.objects.AssociationIPv4;
import es.tid.pce.pcep.objects.OPEN;
import es.tid.pce.pcep.objects.ObjectParameters;
import es.tid.pce.pcep.objects.PCEPErrorObject;
import es.tid.pce.pcep.objects.tlvs.ASSOCTypeListTLV;
import es.tid.pce.pcep.objects.tlvs.DomainIDTLV;
import es.tid.pce.pcep.objects.tlvs.GMPLSCapabilityTLV;
import es.tid.pce.pcep.objects.tlvs.LSPDatabaseVersionTLV;
import es.tid.pce.pcep.objects.tlvs.OSPFDomainIDTLV;
import es.tid.pce.pcep.objects.tlvs.PCE_ID_TLV;
import es.tid.pce.pcep.objects.tlvs.PCE_Redundancy_Group_Identifier_TLV;
import es.tid.pce.pcep.objects.tlvs.PathSetupCapabilityTLV;
import es.tid.pce.pcep.objects.tlvs.SRCapabilityTLV;
import es.tid.pce.pcep.objects.tlvs.StatefulCapabilityTLV;
import es.tid.pce.server.RequestQueue;

/**
 * Generic PCEP Session.
 * Implements the basics of a PCEP Session
 * Any particular session must inherit this one
 * 
 * @author ogondio
 *
 */
public abstract class GenericPCEPSession extends Thread implements PCEPSession {
	
	/**
	 * Capabilities of the LOCAL Entity
	 */
	
	protected PcepCapability localPcepCapability;
	
	/**
	 * Capabilities of the Peer Entity
	 */
	
	protected PcepCapability peerPcepCapability;
	

	/**
	 * PCEP Session Manager
	 */
	protected PCEPSessionsInformation pcepSessionManager;

	/**
	 * Thread to send periodic Keepalives
	 */
	protected KeepAliveThread keepAliveT = null;

	/**
	 * Value of the Keepalive timer set by the Local PCE. Used to send keepalives
	 */

	protected int keepAliveLocal;

	/**
	 * Value of the Keepalive timer of the peer PCC. It is not used in the server!!!
	 */
	protected int keepAlivePeer;

	/**
	 * Thread to check if the connection is still alive. 
	 * If in this time the PCE has not received anything, it closes the session
	 * It is set by the PCC (in this case, the remote peer)
	 */
	protected DeadTimerThread deadTimerT = null; 

	/**
	 *  Value of the deadtimer that the PCE sends, so the PCC uses it
	 *  If in this time the PCC has not received anything, it closes the session
	 *  It is set by the PCE (in this case, the local peer)
	 */
	protected int deadTimerLocal;

	/**
	 * Value of the deadtimer that the PCC sends. It is used in the PCC in the thread
	 */
	protected int deadTimerPeer;

	/**
	 * Socket of the communication between PCC and PCE
	 */
	protected Socket socket = null; 

	/**
	 * IP Address of the remote PCEP Peer
	 */
	protected Inet4Address remotePeerIP;
	
	/**
	 * DataOutputStream to send messages to the peer PCC
	 */
	protected DataOutputStream out=null; 



	/**
	 * DataInputStream to receive messages from PCC
	 */
	protected DataInputStream in=null;//
	/**
	 * Queue to send the Computing Path Requests
	 */
	protected RequestQueue req;

	/**
	 * Logger to write the Parent PCE server log
	 */
	protected Logger log;

	/**
	 * Timer to schedule KeepWait and OpenWait Timers
	 */
	protected Timer timer;

	/**
	 * Finite State Machine of the PCEP protocol
	 */
	protected int FSMstate;

	/**
	 * Remote PCE ID Address
	 * null if not sent
	 */
	protected Inet4Address remotePCEId=null;

	/**
	 * Remote Domain ID
	 * null if not sent
	 */
	protected Inet4Address remoteDomainId=null;

	/**
	 * Remote List of OF Codes
	 * If sent by the peer PCE
	 */
	protected LinkedList<Integer> remoteOfCodes;//FIME: What do we do with them?

	/**
	 * RemoteOK:  a boolean that is set to 1 if the system has received an
      acceptable Open message.
	 */
	protected boolean remoteOK=false;

	/**
	 * 
	 */
	protected boolean localOK=false;

	/**
	 * 
	 */
	protected int openRetry=0;

	/**
	 * Byte array to store the last PCEP message read.
	 */
	protected byte[] msg = null;

	/**
	 * Initial number of the session ID (internal use only)
	 */
	public static long sessionIdCounter=0;

	/**
	 * Session ID (internal use only)
	 */
	private long sessionId;

	/**
	 * OPEN object of PCEPOpen message. It is used for stateful operations
	 */

	protected OPEN open;


	private boolean sendErrorStateful = false;


	protected boolean isSessionStateful = false;
	protected boolean isSessionSRCapable = false;
	protected int sessionMSD = 0;
	
	private long dbVersion;


	public GenericPCEPSession(PCEPSessionsInformation pcepSessionManager){
		this.pcepSessionManager=pcepSessionManager;
		this.newSessionId();
		this.localPcepCapability=pcepSessionManager.getLocalPcepCapability();
		this.pcepSessionManager.addSession(this.sessionId, this);
		log=LoggerFactory.getLogger("PCCClient");
	}

	/*
	  Read PCE message from TCP stream
	  @param in InputStream
	  @return message
	 */
	protected byte[] readMsg(DataInputStream in) throws IOException{
		byte[] ret = null;

		byte[] hdr = new byte[4];
		byte[] temp = null;
		boolean endHdr = false;
		int r = 0;
		int length = 0;
		boolean endMsg = false;
		int offset = 0;

		while (!endMsg) {
			try {
				if (endHdr) {
					r = in.read(temp, offset, 1);
				}
				else {
					r = in.read(hdr, offset, 1);
				}
			} catch (IOException e){
				log.info("Mistake reading data: "+ e.getMessage());
				throw e;
			}catch (Exception e) {
				log.info("readMsg Oops: " + e.getMessage());
				throw new IOException();
			}

			if (r > 0) {
				if (offset == 2) {
					length = ((int)hdr[offset]&0xFF) << 8;
				}
				if (offset == 3) {
					length = length | (((int)hdr[offset]&0xFF));
					temp = new byte[length];
					endHdr = true;
					System.arraycopy(hdr, 0, temp, 0, 4);
				}
				if ((length > 0) && (offset == length - 1)) {
					endMsg = true;
				}
				offset++;
			}
			else if (r==-1){
				log.info("End of stream has been reached from "+this.remotePeerIP);
				throw new IOException();
			}
		}
		if (length > 0) {
			ret = new byte[length];
			System.arraycopy(temp, 0, ret, 0, length);
		}		
		return ret;
	}

	/*
	  Read PCE message from TCP stream
	  @param in InputStream
	*/
	protected byte[] readMsgOptimized(DataInputStream in) throws IOException{
		byte[] ret = null;

		byte[] hdr = new byte[4];
		byte[] temp = null;
		boolean endHdr = false;
		int r = 0;
		int length = 0;
		boolean endMsg = false;
		int offset = 0;


		while (!endMsg) {
			try {
				if (endHdr) {
					//log.info("Vamos a leer datos ");
					r = in.read(temp, offset, length-offset);
					if (r>0){
						if ((offset+r)>=length){
							//log.info("Bien ");
							endMsg=true;	
						}else {
							offset=offset+r;
						}

					}
					else if (r<0){
						log.error("End of stream has been reached reading data");
						throw new IOException();
					}
				}
				else {
					//log.info("Vamos a leer la cabecera ");
					r = in.read(hdr, offset, 4-offset);
					if (r < 0) {
						log.error("End of stream has been reached reading header");
						throw new IOException();
					}else if (r >0){
						if ((offset+r)>=4){
							length = ( (hdr[offset+2]&0xFF) << 8)  | ((hdr[offset+3]&0xFF));
							offset=4;
							temp = new byte[length];
							endHdr = true;
							System.arraycopy(hdr, 0, temp, 0, 4);
							if (length==4){
								endMsg=true;
							}		
						}else {
							offset=offset+r;
						}

					}

				}
			} catch (IOException e){
				log.error("Error reading data: "+ e.getMessage());
				throw e;
			}catch (Exception e) {
				log.error("readMsg Oops: " + e.getMessage());
				log.error("FALLO POR : "+e.getStackTrace());
				throw new IOException();
			}

		}
		if (length > 0) {
			ret = new byte[length];
			System.arraycopy(temp, 0, ret, 0, length);
		}		
		return ret;
	}


	/**
	 * <p>Close the PCE session</p>
	 * <p>List of reasons (RFC 5440):</p>
	 *  Value        Meaning
          1          No explanation provided
          2          DeadTimer expired
          3          Reception of a malformed PCEP message
          4          Reception of an unacceptable number of unknown
                     requests/replies
          5          Reception of an unacceptable number of unrecognized
                     PCEP messages
	 * @param reason Reason for closing the PCEP Session
	 */
	//* @return PCEP Session closed OK
	public void close(int reason){
		log.info("Closing PCEP Session with "+this.remotePeerIP); 
		PCEPClose p_close=new PCEPClose();
		p_close.setReason(reason);
		sendPCEPMessage(p_close);
		killSession();
	}
	public DataOutputStream getOut() {
		return out;
	}

	public void setOut(DataOutputStream out) {
		this.out = out;
	}

	/**
	 * Starts the deadTimerThread
	 */
	protected void startDeadTimer() {
		this.deadTimerT.start();
	}
	/**
	 * Resets the DeadTimerThread
	 * To be called every time a message in the session is received
	 */
	protected void resetDeadTimer() {
		if (this.deadTimerT != null) {
			this.deadTimerT.interrupt();
		}
	}

	public Socket getSocket() {
		return socket;
	}

	/**
	 * Ends the DeadTimer Thread
	 */
	protected void cancelDeadTimer() {
		log.debug("Cancelling DeadTimer from "+this.remotePeerIP);
		if (this.deadTimerT != null) {
			this.deadTimerT.stopRunning();
			this.deadTimerT.interrupt();
			this.deadTimerT=null;
		}
	}

	/**
	 * Starts the Keep Alive Thread
	 */
	public void startKeepAlive() {
		this.keepAliveT.start();		
	}

	/**
	 * Ends the KeepAlive Thread
	 */
	public void cancelKeepAlive() {
		log.debug("Cancelling KeepAliveTimer from "+this.remotePeerIP);
		if (this.keepAliveT != null) {
			this.keepAliveT.stopRunning();
			this.keepAliveT.interrupt();
			this.keepAliveT=null;	
		}
	}

	/**
	 * Ends current connections
	 */
	protected void endConnections(){

		try {
			if (in != null) {
				in.close();
			}
			if (out != null) {
				out.close();
			}
			if (this.socket != null) {
				log.info("Closing socket with "+this.remotePeerIP);
				this.socket.close();
			}

		} catch (Exception e) {
			log.info("Error closing connections: " + e.getMessage());
		}
	}

	public int getFSMstate() {
		return FSMstate;
	}

	protected void setFSMstate(int fSMstate) {
		FSMstate = fSMstate;
	}

	public void killSession(){	
		log.info("Killing Session with "+this.remotePeerIP);
		pcepSessionManager.notifyPeerSessionInactive((Inet4Address)this.socket.getInetAddress());
		timer.cancel();
		this.endConnections();
		this.cancelDeadTimer();
		this.cancelKeepAlive();
		this.endSession();
		this.pcepSessionManager.deleteSession(this.sessionId);
		this.interrupt();				
	}

	/**
	 * DO HERE ANYTHING NEEDED AT CLOSING??
	 * STATISTICS, ETC
	 */
	protected abstract void endSession();

	/*
	  
	 * @param zeroDeadTimerAccepted
	 * @param minimumKeepAliveTimerAccepted
	 * @param maxDeadTimerAccepted
	 * @param isParentPCE
	 * @param requestsParentPCE
	 * @param domainId
	 * @param pceId
	 */

	protected void initializePCEPSession(boolean zeroDeadTimerAccepted, int minimumKeepAliveTimerAccepted, int maxDeadTimerAccepted, boolean isParentPCE, boolean requestsParentPCE, Inet4Address domainId, Inet4Address pceId, int databaseVersion){
		remotePeerIP=(Inet4Address)socket.getInetAddress();
		//private void initializePCEPSession(){
		/**
		 * Byte array to store the last PCEP message read.
		 */
		byte[] msg = null;
		//First get the input and output stream
		try {
			out = new DataOutputStream(socket.getOutputStream());
			in = new DataInputStream(socket.getInputStream());
		} catch (IOException e) {
			log.info("Problem in the sockets, ending PCEPSession");
			killSession();
			return;
		}
		//FIXME: Ojo si no es IPv4, cambiar por InetAddress generico
		pcepSessionManager.notifyPeer((Inet4Address)socket.getInetAddress());
		//STARTING PCEP SESSION ESTABLISHMENT PHASE
		//It begins in Open Wait State
		this.setFSMstate(PCEPValues.PCEP_STATE_OPEN_WAIT);
		log.info("Entering PCEP_STATE_OPEN_WAIT");
		log.info("Scheduling Open Wait Timer");
		//Create the 60 seconds Open Wait Timer to wait for an OPEN message
		OpenWaitTimerTask owtt= new OpenWaitTimerTask(this);
		this.timer.schedule(owtt, 60000);
		//Define (Not use yet), the keepwait timer
		KeepWaitTimerTask kwtt=new KeepWaitTimerTask(this);	
		log.info("Sending OPEN Message");
		PCEPOpen p_open_snd;
		p_open_snd=new PCEPOpen();
		p_open_snd.setKeepalive(this.keepAliveLocal);
		p_open_snd.setDeadTimer(this.deadTimerLocal);
		if (isParentPCE){
			p_open_snd.getOpen().setParentPCEIndicationBit(true);
		}
		if (requestsParentPCE==true){
			p_open_snd.getOpen().setParentPCERequestBit(true);
			OSPFDomainIDTLV domain_id_tlv=new OSPFDomainIDTLV();
			domain_id_tlv.setDomainId(domainId);
			p_open_snd.getOpen().setDomain_id_tlv(domain_id_tlv);
			PCE_ID_TLV pce_id_tlv=new PCE_ID_TLV();
			pce_id_tlv.setPceId(pceId);
			p_open_snd.getOpen().setPce_id_tlv(pce_id_tlv);
		}
		if (pcepSessionManager.getLocalPcepCapability().isStateful())
		{
			log.info("Stateful: "+pcepSessionManager.isStateful()+" Active: "+pcepSessionManager.isActive()+
					 " Trigger sync: "+pcepSessionManager.isStatefulTFlag()+ " Incremental sync: "+pcepSessionManager.isStatefulDFlag()+
					 " include the LSP-DB-VERSION: "+pcepSessionManager.isStatefulSFlag());
			
			
			StatefulCapabilityTLV stateful_capability_tlv = new StatefulCapabilityTLV();

			stateful_capability_tlv.setUFlag(true);
			stateful_capability_tlv.setDFlag(pcepSessionManager.isStatefulDFlag());
			stateful_capability_tlv.setTFlag(pcepSessionManager.isStatefulTFlag());
			stateful_capability_tlv.setSFlag(pcepSessionManager.isStatefulSFlag());
			
			stateful_capability_tlv.setIFlag(pcepSessionManager.getLocalPcepCapability().isInstantiationCapability());
			
			p_open_snd.getOpen().setStateful_capability_tlv(stateful_capability_tlv);
			
			/*PCE REDUNDANCY GROUP IDENTIFIER TLV*/
			// OSCAR FIXME
			/*PCE_Redundancy_Group_Identifier_TLV pce_redundancy_tlv = new PCE_Redundancy_Group_Identifier_TLV();
			pce_redundancy_tlv.setRedundancyId(ObjectParameters.redundancyID);
			p_open_snd.getOpen().setRedundancy_indetifier_tlv(pce_redundancy_tlv);*/

			
			/*LSP DATABASE VERSION TLV*/
			//For the time being, we put a value but it's not used so synchronization
			//won't be avoided.
			//Only send database version if S flag is active
			if (pcepSessionManager.isStatefulSFlag())
			{
				
				LSPDatabaseVersionTLV lsp_database_version = new LSPDatabaseVersionTLV();
				lsp_database_version.setLSPStateDBVersion(databaseVersion);
				p_open_snd.getOpen().setLsp_database_version_tlv(lsp_database_version);
			}
		}

//		if (pcepSessionManager.isSRCapable())
//		{
//			PathSetupCapabilityTLV pathSetup = new PathSetupCapabilityTLV();
//			pathSetup.getPathSetupTypes().add(Integer.valueOf(1));
//			
//			SRCapabilityTLV SR_capability_tlv = new SRCapabilityTLV();
//			//TODO: get?
//			SR_capability_tlv.setMSD(pcepSessionManager.getMSD());
//			pathSetup.setSrCapabilitySubTLV(SR_capability_tlv);
//			//p_open_snd.getOpen().setSR_capability_tlv(SR_capability_tlv);
//			p_open_snd.getOpen().setPathSetupCababiity(pathSetup);
//			
//			log.info("SR: "+pcepSessionManager.isSRCapable()+" MSD: "+pcepSessionManager.getMSD());
//
//		}
		if (pcepSessionManager.isSRCapable())
			{
				SRCapabilityTLV SR_capability_tlv = new SRCapabilityTLV();
				//TODO: get?
				SR_capability_tlv.setMSD(pcepSessionManager.getMSD());
				p_open_snd.getOpen().setSR_capability_tlv(SR_capability_tlv);
				
				log.info("SR: "+pcepSessionManager.isSRCapable()+" MSD: "+pcepSessionManager.getMSD());
	
			}
		if(pcepSessionManager.isRsvpCapable()){
				SRCapabilityTLV Rsvp_capability_tlv = new SRCapabilityTLV();
				Rsvp_capability_tlv.setMSD(pcepSessionManager.getMSD());
			
				
	}
		if (pcepSessionManager.getLocalPcepCapability().isGmpls()){
			GMPLSCapabilityTLV gmplsCapabilityTLV=new GMPLSCapabilityTLV();
			p_open_snd.getOpen().setGmplsCapabilityTLV(gmplsCapabilityTLV);
		}
		//FIXME: HACER BIEN
//		PathSetupCapabilityTLV pathSetupCapability=new PathSetupCapabilityTLV();
//		pathSetupCapability.getPathSetupTypes().add(Integer.valueOf(0));		
//		p_open_snd.getOpen().setPathSetupCababiity(pathSetupCapability);
		
		/**
		 * PARA SEGMENT ROUTING
		 */
		
		ASSOCTypeListTLV aso = new ASSOCTypeListTLV();
		aso.getAssociation_types().add(1);
		aso.getAssociation_types().add(6);
		p_open_snd.getOpen().setAssoc_type_list_tlv(aso);
		
		
		//Send the OPEN message
		sendPCEPMessage(p_open_snd);

		//Now, read messages until we are in SESSION UP
		while (this.FSMstate!=PCEPValues.PCEP_STATE_SESSION_UP){
			try {
				//Read a new message
				msg = readMsg(in);
			}catch (IOException e){
				log.info("Error reading message, ending session"+e.getMessage());
				killSession();
				return;
			}
			if (msg != null) {//If null, it is not a valid PCEP message								
				switch(PCEPMessage.getMessageType(msg)) {
				case PCEPMessageTypes.MESSAGE_OPEN:
					log.info("OPEN Message Received");
					if (this.FSMstate==PCEPValues.PCEP_STATE_OPEN_WAIT){
						PCEPOpen p_open;
						try {
							p_open=new PCEPOpen(msg);
							log.debug(p_open.toString());
							owtt.cancel();
							//Check parameters
							if (openRetry==1){
								boolean checkOK=true;
								boolean stateFulOK = true;
								boolean SROK = true;
								boolean updateEffective = false;
								int MSD = -1;

								this.deadTimerPeer=p_open.getDeadTimer();
								this.keepAlivePeer=p_open.getKeepalive();

								if (this.deadTimerPeer>maxDeadTimerAccepted){
									checkOK=false;
								}	
								if (this.deadTimerPeer==0){
									if(zeroDeadTimerAccepted==false){
										checkOK=false;
									}
								}
								if (this.keepAlivePeer<minimumKeepAliveTimerAccepted){
									checkOK=false;
								}
								//If PCE is stateless and there are statefull tlv send error
								if ((!pcepSessionManager.isStateful())&&
										((p_open.getOpen().getRedundancy_indetifier_tlv()!=null)||
												(p_open.getOpen().getLsp_database_version_tlv()!=null)||
												(p_open.getOpen().getStateful_capability_tlv()!=null)))
								{
									log.info("I'm not expeting Stateful session");
									stateFulOK = false;
								}
								else if ((pcepSessionManager.isStateful())&&(p_open.getOpen().getStateful_capability_tlv()==null))
								{
									log.info("I'm expeting Stateful session");
									stateFulOK = false;
								}
								else if (pcepSessionManager.isStateful() && p_open.getOpen().getStateful_capability_tlv()!=null)
								{
									updateEffective = p_open.getOpen().getStateful_capability_tlv().isUFlag();
									log.info("Other PCEP speaker is also stateful");
								}
								else
								{
									log.info("Both PCEP speakers aren't stateful");									
								}

								if (!(pcepSessionManager.isSRCapable()) && (p_open.getOpen().getSR_capability_tlv()!=null))
								{
									log.info("I'm not expecting SR session");
									SROK = false;
								}
								else if ((pcepSessionManager.isSRCapable()) && (p_open.getOpen().getSR_capability_tlv()==null))
								{
									log.info("I'm expeting SR capable session");
									SROK = false;
								}
								else if ((pcepSessionManager.isSRCapable()) && (p_open.getOpen().getSR_capability_tlv()!=null))
								{
									MSD = p_open.getOpen().getSR_capability_tlv().getMSD();
									this.sessionMSD = MSD; //We will only look at this value in the session of a PCE
									//TODO: que hago con esto?
									log.info("Other component is also SR capable with MSD= "+p_open.getOpen().getSR_capability_tlv().getMSD());
								}				
								else
								{
									log.info("Neither of us are SR capable :(");									
								}


								//TODO: Hay que cambiar esto
								if (stateFulOK == false)
								{
									//processNotStateful(p_open, kwtt);
								}
								if ((pcepSessionManager.isStateful())&&(updateEffective == false))
								{
								//	log.info("This PCE operates right now as if the LSPs are delegated");
								}



								if (checkOK==false){
									log.info("Dont accept deadTimerPeer "+deadTimerPeer+"keepAlivePeer "+keepAlivePeer);
									PCEPError perror=new PCEPError();
									PCEPErrorObject perrorObject=new PCEPErrorObject();
									perrorObject.setErrorType(ObjectParameters.ERROR_ESTABLISHMENT);
									perrorObject.setErrorValue(ObjectParameters.ERROR_ESTABLISHMENT_SECOND_OPEN_MESSAGE_UNACCEPTABLE_SESSION_CHARACTERISTICS);
									ErrorConstruct error_c=new ErrorConstruct();
									error_c.getErrorObjList().add(perrorObject);
									 LinkedList<ErrorConstruct> ec=new LinkedList<ErrorConstruct>();
									 ec.add(error_c);
									perror.setErrorList(ec);
									log.info("Sending Error and ending PCEPSession");
									sendPCEPMessage(perror);										
								}
								else {
									/**
									 *  If no errors are detected, and the session characteristics are
									 *	acceptable to the local system, the system:

   												o  Sends a Keepalive message to the PCEP peer,
   												o  Starts the Keepalive timer,
   												o  Sets the RemoteOK variable to 1.
   											If LocalOK=1, the system clears the OpenWait timer and moves to the
   											UP state.
   											If LocalOK=0, the system clears the OpenWait timer, starts the
   											KeepWait timer, and moves to the KeepWait state.
									 */
									log.info("Accept deadTimerPeer "+deadTimerPeer+"keepAlivePeer "+keepAlivePeer);
									if (p_open.getOpen().getPce_id_tlv()!=null){
										this.remotePCEId=p_open.getOpen().getPce_id_tlv().getPceId();	
									}
									if (p_open.getOpen().getDomain_id_tlv()!=null){
										
										if (p_open.getOpen().getDomain_id_tlv() instanceof es.tid.pce.pcep.objects.tlvs.EmptyDomainIDTLV) {
											//Ignore 
										}else if (p_open.getOpen().getDomain_id_tlv() instanceof es.tid.pce.pcep.objects.tlvs.OSPFDomainIDTLV) {
											this.remoteDomainId=((OSPFDomainIDTLV)p_open.getOpen().getDomain_id_tlv()).getDomainId();	
										}
											
									}
									if (p_open.getOpen().getOf_list_tlv()!=null){
										this.remoteOfCodes=p_open.getOpen().getOf_list_tlv().getOfCodes();
									}

									log.debug("Sending KA to confirm");
									PCEPKeepalive p_ka= new PCEPKeepalive();
									log.debug("Sending Keepalive message");
									sendPCEPMessage(p_ka);										//Creates the Keep Wait Timer to wait for a KA to acknowledge the OPEN sent
									//FIXME: START KA TIMER!
									this.deadTimerPeer=p_open.getDeadTimer();
									this.keepAlivePeer=p_open.getKeepalive();
									this.remoteOK=true;										
									if(this.localOK==true){
										log.info("Entering STATE_SESSION_UP");
										this.setFSMstate(PCEPValues.PCEP_STATE_SESSION_UP);							
									}
									else {
										log.info("Entering STATE_KEEP_WAIT");
										log.debug("Scheduling KeepwaitTimer");
										timer.schedule(kwtt, 60000);
										this.setFSMstate(PCEPValues.PCEP_STATE_KEEP_WAIT);
									}

									if (pcepSessionManager.isStateful())
									{

										log.info("open object saved: "+p_open.getOpen());
										this.open = p_open.getOpen();
										isSessionStateful = true;
									}
									if (pcepSessionManager.isSRCapable())
									{
									//	this.open = p_open.getOpen();
										isSessionSRCapable = true;		
										sessionMSD = pcepSessionManager.getMSD();
									}
								}
							}
							else {//Open retry is 0
								boolean dtOK=true;
								boolean kaOK=true;
								boolean stateFulOK = true;
								boolean SROK = true;
								boolean updateEffective = false;
								int MSD = -1;

								//If PCE is stateless and there are statefull tlv send error
								if ((!pcepSessionManager.isStateful())&&
										((p_open.getOpen().getRedundancy_indetifier_tlv()!=null)||
												(p_open.getOpen().getLsp_database_version_tlv()!=null)||
												(p_open.getOpen().getStateful_capability_tlv()!=null)))
								{
									log.info("I'm not expeting Stateful session");
									stateFulOK = false;
								}
								else if ((pcepSessionManager.isStateful())&&(p_open.getOpen().getStateful_capability_tlv()==null))
								{
									log.info("I'm expeting Stateful session");
									stateFulOK = false;
								}
								else if (pcepSessionManager.isStateful() && p_open.getOpen().getStateful_capability_tlv()!=null)
								{
									updateEffective = p_open.getOpen().getStateful_capability_tlv().isUFlag();
									log.info("Other PCEP speaker is also stateful");
								}
								else
								{
									log.info("Both PCEP speakers aren't stateful");									
								}

								if (!(pcepSessionManager.isSRCapable()) && (p_open.getOpen().getSR_capability_tlv()!=null))
								{
									log.info("I'm not expecting SR session");
									SROK = false;
								}
								else if ((pcepSessionManager.isSRCapable()) && (p_open.getOpen().getSR_capability_tlv()==null))
								{
									log.info("I'm expeting SR capable session");
									SROK = false;
								}
								else if ((pcepSessionManager.isSRCapable()) && (p_open.getOpen().getSR_capability_tlv()!=null))
								{
									MSD = p_open.getOpen().getSR_capability_tlv().getMSD();
									this.sessionMSD = MSD; //We will only look at this value in the session of a PCE
									//TODO: que hago con esto?
									log.info("Other component is also SR capable with MSD= "+p_open.getOpen().getSR_capability_tlv().getMSD());
								}				
								else
								{
									log.info("Neither of us are SR capable :(");									
								}


								//TODO: Hay que cambiar esto
								if (stateFulOK == false)
								{
									//processNotStateful(p_open, kwtt);
								}
								if ((pcepSessionManager.isStateful())&&(updateEffective == false))
								{
								//	log.info("This PCE operates right now as if the LSPs are delegated");
								}
								
								
								
								if (p_open.getDeadTimer()>maxDeadTimerAccepted){
									dtOK=false;
								}	
								if (p_open.getDeadTimer()==0){
									if(zeroDeadTimerAccepted==false){
										dtOK=false;
									}
								}
								if (p_open.getKeepalive()<minimumKeepAliveTimerAccepted){
									kaOK=false;
								}
								if ((kaOK == false) || (dtOK == false)){
									///Parameters are unacceptable but negotiable
									log.info("PEER PCC Open parameters are unaccpetable, but negotiable");
									PCEPError perror=new PCEPError();
									PCEPErrorObject perrorObject=new PCEPErrorObject();
									perrorObject.setErrorType(ObjectParameters.ERROR_ESTABLISHMENT);
									perrorObject.setErrorValue(ObjectParameters.ERROR_ESTABLISHMENT_UNACCEPTABLE_NEGOTIABLE_SESSION_CHARACTERISTICS);
									if (dtOK==false){
										p_open.setDeadTimer(this.deadTimerLocal);	
									}
									if (kaOK==false) {
										p_open.setKeepalive(this.keepAliveLocal);
									}
									if (stateFulOK==false) {
										p_open.setKeepalive(this.keepAliveLocal);
									}
									if (SROK == false) {
										p_open.setKeepalive(this.keepAliveLocal);
									}
									
									LinkedList<PCEPErrorObject> perrobjlist=new LinkedList<PCEPErrorObject>(); 
									perrobjlist.add(perrorObject);
									perror.setErrorObjList(perrobjlist);
									perror.setOpen(p_open.getOpen());
									log.info("Sending Error with new proposal");
									this.sendPCEPMessage(perror);
									this.openRetry=this.openRetry+1;
									/**
									 * o  If LocalOK=1, the system restarts the OpenWait timer and stays in
     											the OpenWait state.
     											o  If LocalOK=0, the system clears the OpenWait timer, starts the
     											KeepWait timer, and moves to the KeepWait state.
									 */
									if (localOK==true){
										log.info("Local ok esta a true, vamos a open wait");
										owtt.cancel();
										owtt= new OpenWaitTimerTask(this);
										this.timer.schedule(owtt, 60000);
										this.setFSMstate(PCEPValues.PCEP_STATE_OPEN_WAIT);
									}
									else {
										log.info("Local ok esta a false, vamos a keep wait");
										owtt.cancel();
										this.setFSMstate(PCEPValues.PCEP_STATE_KEEP_WAIT);
										this.timer.schedule(kwtt, 60000);
									}
								}
								else {
									/*
									 * If no errors are detected, and the session characteristics are
   											acceptable to the local system, the system:
   											o  Sends a Keepalive message to the PCEP peer,
   											o  Starts the Keepalive timer,
   											o  Sets the RemoteOK variable to 1.
   											If LocalOK=1, the system clears the OpenWait timer and moves to the
   											UP state.
   											If LocalOK=0, the system clears the OpenWait timer, starts the
   											KeepWait timer, and moves to the KeepWait state.
									 */
									if (p_open.getOpen().getPce_id_tlv()!=null){
										this.remotePCEId=p_open.getOpen().getPce_id_tlv().getPceId();	
									}
									if (p_open.getOpen().getDomain_id_tlv()!=null){
										if (p_open.getOpen().getDomain_id_tlv() instanceof es.tid.pce.pcep.objects.tlvs.EmptyDomainIDTLV) {
											//Ignore 
										}else if (p_open.getOpen().getDomain_id_tlv() instanceof es.tid.pce.pcep.objects.tlvs.OSPFDomainIDTLV) {
											this.remoteDomainId=((OSPFDomainIDTLV)p_open.getOpen().getDomain_id_tlv()).getDomainId();	
										}
												
									}
									if (p_open.getOpen().getOf_list_tlv()!=null){
										this.remoteOfCodes=p_open.getOpen().getOf_list_tlv().getOfCodes();
									}
									log.debug("Sending KA to confirm");
									PCEPKeepalive p_ka= new PCEPKeepalive();
									log.debug("Sending Keepalive message");
									sendPCEPMessage(p_ka);										//Creates the Keep Wait Timer to wait for a KA to acknowledge the OPEN sent
									//FIXME: START KA TIMER!
									this.deadTimerPeer=p_open.getDeadTimer();
									this.keepAlivePeer=p_open.getKeepalive();
									this.remoteOK=true;										
									if(this.localOK==true){
										log.info("Entering STATE_SESSION_UP");
										this.setFSMstate(PCEPValues.PCEP_STATE_SESSION_UP);							
									}
									else {
										log.info("Entering STATE_KEEP_WAIT");
										log.debug("Scheduling KeepwaitTimer");
										timer.schedule(kwtt, 60000);
										this.setFSMstate(PCEPValues.PCEP_STATE_KEEP_WAIT);
									}

									if (pcepSessionManager.isStateful())
									{
										/* The open object is stored for later proccessing */
										log.info("open object saved: "+p_open.getOpen());
										this.open = p_open.getOpen();
										isSessionStateful = true;
									}
									if (pcepSessionManager.isSRCapable())
									{
										/* The open object is stored for later proccessing */
										//this.open = p_open.getOpen();
										isSessionSRCapable= true;
										sessionMSD = pcepSessionManager.getMSD();
									}
								}
							}								
						} catch (PCEPProtocolViolationException e1) {
							log.info("Malformed OPEN, INFORM ERROR and close");
							e1.printStackTrace();
							PCEPError perror=new PCEPError();
							PCEPErrorObject perrorObject=new PCEPErrorObject();
							perrorObject.setErrorType(ObjectParameters.ERROR_ESTABLISHMENT);
							perrorObject.setErrorValue(ObjectParameters.ERROR_ESTABLISHMENT_INVALID_OPEN_MESSAGE);
							perror.getErrorObjList().add(perrorObject);
							log.info("Sending Error and ending PCEPSession");
							sendPCEPMessage(perror);	
							pcepSessionManager.notifyPeerSessionFail((Inet4Address)this.socket.getInetAddress());
							killSession();
						}//Fin del catch de la exception PCEP
					}
					else{
						log.info("Ignore OPEN message, already one received!!");
					}

					break;
				case PCEPMessageTypes.MESSAGE_KEEPALIVE:
					log.info("KeepAlive Message Received");
					this.localOK=true;
					if(this.FSMstate==PCEPValues.PCEP_STATE_KEEP_WAIT){
						// If RemoteOK=1, the system clears the KeepWait timer and moves to
						//  the UP state.
						// If RemoteOK=0, the system clears the KeepWait timer, starts the
						//  OpenWait timer, and moves to the OpenWait State. 

						if (remoteOK==true){
							kwtt.cancel();
							log.info("Entering STATE_SESSION_UP");
							this.setFSMstate(PCEPValues.PCEP_STATE_SESSION_UP);								
						}
						else{
							kwtt.cancel();
							log.info("Entering OPEN WAIT STATE");
							owtt=new OpenWaitTimerTask(this);
							this.timer.schedule(owtt, 60000);
							this.setFSMstate(PCEPValues.PCEP_STATE_OPEN_WAIT);
						}

					}
					//If not... seguimos igual que estabamos
					//Mas KA no hacen mal...
					break;
				case PCEPMessageTypes.MESSAGE_ERROR:
					log.info("ERROR Message Received");

					try {
						PCEPError msg_error=new PCEPError(msg);
						if (this.FSMstate==PCEPValues.PCEP_STATE_KEEP_WAIT){
							int errorValue;
							//TODO: CHECK
							errorValue=msg_error.getErrorObjList().get(0).getErrorValue();
							if (errorValue==ObjectParameters.ERROR_ESTABLISHMENT_UNACCEPTABLE_NEGOTIABLE_SESSION_CHARACTERISTICS)
							{
								log.info("ERROR_ESTABLISHMENT_UNACCEPTABLE_NEGOTIABLE_SESSION_CHARACTERISTICS");	
								/**
								 * If a PCErr message is received before the expiration of the KeepWait
								   timer:

									   1.  If the proposed values are unacceptable, the PCEP peer sends a
									       PCErr message with Error-Type=1 and Error-value=6, and the system
									       releases the PCEP resources for that PCEP peer, closes the TCP
									       connection, and moves to the Idle state.

									   2.  If the proposed values are acceptable, the system adjusts its
									       PCEP session characteristics according to the proposed values
									       received in the PCErr message, restarts the KeepWait timer, and
									       sends a new Open message.  If RemoteOK=1, the system restarts the
									       KeepWait timer and stays in the KeepWait state.  If RemoteOK=0,
									       the system clears the KeepWait timer, starts the OpenWait timer,
									       and moves to the OpenWait state.

								 */
								boolean checkOK=true;
								if (msg_error.getOpen().getDeadtimer()>maxDeadTimerAccepted){
									checkOK=false;
								}	
								if (msg_error.getOpen().getDeadtimer()==0){
									if(zeroDeadTimerAccepted==false){
										checkOK=false;
									}
								}
								if (msg_error.getOpen().getKeepalive()<minimumKeepAliveTimerAccepted){
									checkOK=false;
								}
								if (checkOK==false){
									//SendErrorAndClose
									PCEPError perror=new PCEPError();
									PCEPErrorObject perrorObject=new PCEPErrorObject();
									perrorObject.setErrorType(ObjectParameters.ERROR_ESTABLISHMENT);
									perrorObject.setErrorValue(ObjectParameters.ERROR_ESTABLISHMENT_PCERR_UNACCEPTABLE_SESSION_CHARACTERISTICS);
									perror.getErrorObjList().add(perrorObject);
									log.info("Sending Error and ending PCEPSession");
									sendPCEPMessage(perror);	
									killSession();
									return;
								}
								else{
									this.deadTimerLocal=msg_error.getOpen().getDeadtimer();
									this.keepAliveLocal=msg_error.getOpen().getKeepalive();
									PCEPOpen p_open_snd2=new PCEPOpen();
									p_open_snd2.setKeepalive(this.keepAliveLocal);
									p_open_snd2.setDeadTimer(this.deadTimerLocal);
									sendPCEPMessage(p_open_snd2);								
									/**
									 * If RemoteOK=1, the system restarts the
									       KeepWait timer and stays in the KeepWait state.  If RemoteOK=0,
									       the system clears the KeepWait timer, starts the OpenWait timer,
									       and moves to the OpenWait state.

									 */
									if (remoteOK==true){
										kwtt.cancel();
										this.setFSMstate(PCEPValues.PCEP_STATE_KEEP_WAIT);
									}
									else {
										kwtt.cancel();
										this.timer.schedule(owtt, 60000);
										this.setFSMstate(PCEPValues.PCEP_STATE_OPEN_WAIT);
									}

								}
							}
						}
						else
						{
							log.info("Received PCErr and paramaters cant't be negotiated. PCEP Channel will be closed");
						}

					} catch (PCEPProtocolViolationException e1) {
						log.info("problem decoding error, finishing PCEPSession "+e1);
						killSession();
					}

					break;
				default:
					log.info("UNEXPECTED Message Received");
					if (this.FSMstate!=PCEPValues.PCEP_STATE_OPEN_WAIT){
						log.info("Ignore OPEN message, already one received!!");
					}
					else {
						log.info("Unexpected message RECEIVED, closing");
						PCEPError perror=new PCEPError();
						PCEPErrorObject perrorObject=new PCEPErrorObject();
						perrorObject.setErrorType(ObjectParameters.ERROR_ESTABLISHMENT);
						perrorObject.setErrorValue(ObjectParameters.ERROR_ESTABLISHMENT_INVALID_OPEN_MESSAGE);
						log.info("Sending Error and ending PCEPSession");
						perror.getErrorObjList().add(perrorObject);
						sendPCEPMessage(perror);							
					}
					break;
				}
			}
			else {
				if (this.FSMstate!=PCEPValues.PCEP_STATE_OPEN_WAIT){
					log.info("Ignore message, already one received!!");
				}
				else {
					log.info("Unexpected message RECEIVED, closing");
					PCEPError perror=new PCEPError();
					PCEPErrorObject perrorObject=new PCEPErrorObject();
					perrorObject.setErrorType(ObjectParameters.ERROR_ESTABLISHMENT);
					perrorObject.setErrorValue(ObjectParameters.ERROR_ESTABLISHMENT_INVALID_OPEN_MESSAGE);
					perror.getErrorObjList().add(perrorObject);
					sendPCEPMessage(perror);		
				}
			}//Fin del else
		}//Fin del WHILE
		pcepSessionManager.notifyPeerSessionOK((Inet4Address)this.socket.getInetAddress());


	}

	public void sendPCEPMessage(PCEPMessage message) {
		try {
			message.encode();
		} catch (PCEPProtocolViolationException e11) {
			log.error("ERROR ENCODING ERROR OBJECT, BUG DETECTED, INFORM!!! "+e11.getMessage());
			log.error("Ending Session");
			e11.printStackTrace();
			killSession();
		}
		try {			
			out.write(message.getBytes());
			out.flush();
		} catch (IOException e) {
			log.error("Problem writing message, finishing session "+e.getMessage());
			killSession();
		}
	}

	public Inet4Address getRemotePCEId() {
		return remotePCEId;
	}

	public void setRemotePCEId(Inet4Address remotePCEId) {
		this.remotePCEId = remotePCEId;
	}

	public LinkedList<Integer> getRemoteOfCodes() {
		return remoteOfCodes;
	}

	public String shortInfo(){
		StringBuffer sb=new StringBuffer(1000);
		if (this.socket!=null){
			sb.append("remAddr: ");
			sb.append(this.socket.getRemoteSocketAddress());
			sb.append(" state: ");
			if (this.FSMstate==PCEPValues.PCEP_STATE_OPEN_WAIT){
				sb.append("OPEN_WAIT");
			}else if (this.FSMstate==PCEPValues.PCEP_STATE_IDLE){
				sb.append("IDLE");
			}else if (this.FSMstate==PCEPValues.PCEP_STATE_KEEP_WAIT){
				sb.append("KEEP_WAIT");
			}else if (this.FSMstate==PCEPValues.PCEP_STATE_SESSION_UP){
				sb.append("SESSION_UP");
			}else if (this.FSMstate==PCEPValues.PCEP_STATE_SESSION_UP){
				sb.append("TCP_PENDING");
			}else {
				sb.append("UNKNOWN");
			}			

		}

		return sb.toString();
	}

	public String toString(){
		StringBuffer sb=new StringBuffer(1000);
		return sb.toString();
	}

	public void newSessionId(){
		this.sessionId=GenericPCEPSession.sessionIdCounter+1;
		sessionIdCounter=sessionIdCounter+1;
	}

	private void processNotStateful(PCEPOpen p_open, KeepWaitTimerTask kwtt)
	{
		if (sendErrorStateful)
		{
			PCEPError perror=new PCEPError();
			PCEPErrorObject perrorObject=new PCEPErrorObject();
			perrorObject.setErrorType(ObjectParameters.ERROR_INVALID_OPERATION);
			perrorObject.setErrorValue(ObjectParameters.ERROR_STATEFUL_CAPABILITY_NOT_SUPPORTED);
			perror.getErrorObjList().add(perrorObject);
			this.sendPCEPMessage(perror);
		}
		else
		{
			log.debug("Sending KA to confirm");
			PCEPKeepalive p_ka= new PCEPKeepalive();
			log.debug("Sending Keepalive message");
			sendPCEPMessage(p_ka);										//Creates the Keep Wait Timer to wait for a KA to acknowledge the OPEN sent
			//FIXME: START KA TIMER!
			this.deadTimerPeer=p_open.getDeadTimer();
			this.keepAlivePeer=p_open.getKeepalive();
			this.remoteOK=true;										
			if(this.localOK==true){
				log.info("Entering STATE_SESSION_UP");
				this.setFSMstate(PCEPValues.PCEP_STATE_SESSION_UP);							
			}
			else {
				log.info("Entering STATE_KEEP_WAIT");
				log.debug("Scheduling KeepwaitTimer");
				timer.schedule(kwtt, 60000);
				this.setFSMstate(PCEPValues.PCEP_STATE_KEEP_WAIT);
			}
		}
		isSessionStateful = false;
	}
	
	private boolean checkLSPsync(PCEPOpen p_open)
	{
		if (pcepSessionManager.isStatefulSFlag() && p_open.getOpen().getStateful_capability_tlv().isSFlag())
		{
			//if (data)
		}
		return false;
		
	}
	
	
	private void processNotSRCapable(PCEPOpen p_open, KeepWaitTimerTask kwtt)
	{
		//TODO: por hacer
	}
	
	
}
