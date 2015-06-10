package tid.vntm;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Timer;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import tid.netManager.emulated.LayerTypes;
import tid.pce.client.ClientRequestManager;
import tid.pce.client.PCCPCEPSession;
import tid.pce.pcepsession.DeadTimerThread;
import tid.pce.pcepsession.GenericPCEPSession;
import tid.pce.pcepsession.KeepAliveThread;
import tid.pce.pcepsession.PCEPSessionsInformation;
import tid.pce.pcepsession.PCEPValues;
import tid.pce.tedb.IntraDomainEdge;
import tid.pce.tedb.MultiLayerTEDB;
import tid.pce.tedb.SimpleTEDB;
import tid.util.UtilsFunctions;
import tid.vntm.emulator.TCPOSPFSender;
import tid.vntm.topology.VNTMGraph;
import tid.vntm.topology.elements.Link;
import tid.vntm.topology.elements.Node;

import com.google.gson.JsonObject;

import es.tid.ospf.ospfv2.OSPFv2LinkStateUpdatePacket;
import es.tid.ospf.ospfv2.lsa.LSA;
import es.tid.ospf.ospfv2.lsa.OSPFTEv2LSA;
import es.tid.ospf.ospfv2.lsa.tlv.LinkTLV;
import es.tid.ospf.ospfv2.lsa.tlv.subtlv.LinkID;
import es.tid.ospf.ospfv2.lsa.tlv.subtlv.LocalInterfaceIPAddress;
import es.tid.ospf.ospfv2.lsa.tlv.subtlv.MaximumBandwidth;
import es.tid.ospf.ospfv2.lsa.tlv.subtlv.RemoteInterfaceIPAddress;
import es.tid.ospf.ospfv2.lsa.tlv.subtlv.complexFields.BitmapLabelSet;
import es.tid.pce.pcep.PCEPProtocolViolationException;
import es.tid.pce.pcep.constructs.EndPoint;
import es.tid.pce.pcep.constructs.EndPointAndRestrictions;
import es.tid.pce.pcep.constructs.P2MPEndpoints;
import es.tid.pce.pcep.constructs.P2PEndpoints;
import es.tid.pce.pcep.constructs.PCEPIntiatedLSP;
import es.tid.pce.pcep.constructs.Path;
import es.tid.pce.pcep.constructs.Request;
import es.tid.pce.pcep.constructs.SVECConstruct;
import es.tid.pce.pcep.constructs.StateReport;
import es.tid.pce.pcep.constructs.SwitchEncodingType;
import es.tid.pce.pcep.messages.PCEPClose;
import es.tid.pce.pcep.messages.PCEPInitiate;
import es.tid.pce.pcep.messages.PCEPMessage;
import es.tid.pce.pcep.messages.PCEPMessageTypes;
import es.tid.pce.pcep.messages.PCEPReport;
import es.tid.pce.pcep.messages.PCEPRequest;
import es.tid.pce.pcep.messages.PCEPResponse;
import es.tid.pce.pcep.messages.PCEPTELinkConfirmation;
import es.tid.pce.pcep.messages.PCEPTELinkTearDownSuggestion;
import es.tid.pce.pcep.objects.BandwidthRequested;
import es.tid.pce.pcep.objects.EndPoints;
import es.tid.pce.pcep.objects.EndPointsIPv4;
import es.tid.pce.pcep.objects.EndPointsUnnumberedIntf;
import es.tid.pce.pcep.objects.ExplicitRouteObject;
import es.tid.pce.pcep.objects.GeneralizedEndPoints;
import es.tid.pce.pcep.objects.IncludeRouteObject;
import es.tid.pce.pcep.objects.InterLayer;
import es.tid.pce.pcep.objects.LSP;
import es.tid.pce.pcep.objects.ObjectParameters;
import es.tid.pce.pcep.objects.ObjectiveFunction;
import es.tid.pce.pcep.objects.P2MPEndPointsDataPathID;
import es.tid.pce.pcep.objects.P2MPEndPointsIPv4;
import es.tid.pce.pcep.objects.RequestParameters;
import es.tid.pce.pcep.objects.Reservation;
import es.tid.pce.pcep.objects.SRP;
import es.tid.pce.pcep.objects.Svec;
import es.tid.pce.pcep.objects.SwitchLayer;
import es.tid.pce.pcep.objects.XifiUniCastEndPoints;
import es.tid.pce.pcep.objects.tlvs.BandwidthTLV;
import es.tid.pce.pcep.objects.tlvs.EndPointIPv4TLV;
import es.tid.pce.pcep.objects.tlvs.UnnumberedEndpointTLV;
import es.tid.rsvp.objects.subobjects.IPv4prefixEROSubobject;
import es.tid.rsvp.objects.subobjects.OpenFlowUnnumberIfIDEROSubobject;
import es.tid.rsvp.objects.subobjects.UnnumberIfIDEROSubobject;

public class VNTMSession extends GenericPCEPSession{
	LSPManager lspmanager;
	private VNTMGraph vntmGraph;
//	NetworkEmulatorThread net;
	private VNTMParameters vntmparams;
	private HashMap<Long,OpTable> oPtable=new HashMap<Long,OpTable>();
	private static AtomicInteger oPcounter=new AtomicInteger(1);
	private long idToDelete=-1;


	public VNTMSession(Socket s,LSPManager lspmanager,PCEPSessionsInformation pcepSessionManager, VNTMGraph vntmGraph, VNTMParameters vntmparams, HashMap<Long, OpTable> oPtable){
		super(pcepSessionManager);
		this.socket=s;
		this.lspmanager=lspmanager;
		log=Logger.getLogger("VNTMServer");
		timer=new Timer();
		this.oPtable=oPtable;
		this.keepAliveLocal=30;
		this.deadTimerLocal=180;
		this.vntmGraph=vntmGraph;
		this.vntmparams=vntmparams;
	}

	public void run() {

		initializePCEPSession(false,30,1000,false,false,null,null,0);
		//Session is UP now, start timers
		log.info("VNTM Session succesfully established!!");				
		this.deadTimerT=new DeadTimerThread(this, this.deadTimerLocal);
		startDeadTimer();	
		this.keepAliveT=new KeepAliveThread(out, this.keepAliveLocal);
		startKeepAlive();

		//Listen to new messages
		this.setFSMstate(PCEPValues.PCEP_STATE_SESSION_UP);
		while(this.FSMstate==PCEPValues.PCEP_STATE_SESSION_UP) {
			try {
				this.msg = readMsg(in);//Read a new message
			}catch (IOException e)
			{
				cancelDeadTimer();
				cancelKeepAlive();
				timer.cancel();
				log.info(UtilsFunctions.exceptionToString(e));
				try 
				{
					in.close();
					out.close();
				} 
				catch (IOException e1) 
				{
				}
				log.warning("Finishing PCEP Session abruptly");
				return;
			}
			log.info(this.msg.toString());
			if (this.msg != null) {//If null, it is not a valid PCEP message		
				log.info("new PCEP msg arrived"+PCEPMessage.getMessageType(this.msg));
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
					log.fine("Received NOTIFY message");
					break;

				case PCEPMessageTypes.MESSAGE_FULL_TOPOLOGY:
					log.fine("Full topology. Just a testing case..");
					String requestToDo = null;
					//Abro el socket!
					Socket socket = null;
					DataOutputStream out = null;
					DataInputStream in = null;
					JsonObject json= new JsonObject();
					json.addProperty("domainID", "1");

					requestToDo= json.toString();
					try {
						socket= new Socket("localhost", 9876);    

						out = new DataOutputStream(socket.getOutputStream());
						in = new DataInputStream(socket.getInputStream());
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					//Mando la informacion
					int length = requestToDo.length();
					byte[] bytesToSend = new byte[(length+4)];
					bytesToSend[0]= (byte) (1 << 4  );
					bytesToSend[0]=(byte) (bytesToSend[0] | 0);
					bytesToSend[1] = 0x00;
					bytesToSend[2]=(byte)(length >>> 8 & 0xff);
					bytesToSend[3]=(byte)(length & 0xff);
					System.arraycopy(requestToDo.getBytes(), 0, bytesToSend, 4, length);

					try {
						out.write(bytesToSend);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					String response = null;	
					boolean readMessage=true;
					while (readMessage) {
						try {
							response = readMsgTM(in);
							//System.out.println(response);
							if (response != null){
								readMessage=false;
							}
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					System.out.println(response.toString());
					break;

				case PCEPMessageTypes.MESSAGE_INITIATE:
					log.info("Intitate message received");
					//ESTABLISH TE LINK
					//ADD TE LINK TO THE LIST
					try {
						PCEPInitiate p_init = new PCEPInitiate(this.msg);
						log.info("p_init.getPcepIntiatedLSPList().get(0).getEndPoint():"+p_init.getPcepIntiatedLSPList().get(0).getEndPoint());
						if (p_init.getPcepIntiatedLSPList().get(0).getEndPoint() instanceof P2MPEndPointsDataPathID)
						{
							callMultiPCE0(p_init);
						}
						else if (p_init.getPcepIntiatedLSPList().get(0).getEndPoint() instanceof P2MPEndPointsIPv4)
						{
							callMultiPCE02(p_init);
						}
						//{
							//boolean state = createVLANLink();
							//boolean state=createIPLink();
//							if (!state)
//							{
//								log.info("Error al crear el IPLink");
//							}
		//				}			

					} catch (PCEPProtocolViolationException e) {
						System.out.println("PCEP Violation");
						e.printStackTrace();
					}
//					 catch (UnknownHostException e) {
//							// TODO Auto-generated catch block
//							System.out.println("Unknown Host");
//							e.printStackTrace();
//						} catch (InterruptedException e) {
//							// TODO Auto-generated catch block
//							System.out.println("Interrupcion");
//							e.printStackTrace();
//						} catch (IOException e) {
//							// TODO Auto-generated catch block
//							System.out.println("Error al enviar paquete");
//							e.printStackTrace();
//						}
					//NOTIFY THE CHANGE TO THE NETWORK EMULATOR
					break;

				case PCEPMessageTypes.MESSAGE_PCREQ:
					try 
					{
						//boolean state = createVLANLink();
					}
					catch (Exception e) 
					{
						log.info(UtilsFunctions.exceptionToString(e));
					}
					break;
					/*
				case PCEPMessageTypes.MESSAGE_INTERLAYER_NODES:
					try 
					{
						returninterlayernodes();
					}
					catch (Exception e) 
					{
						log.info(UtilsFunctions.exceptionToString(e));
					}
					break;
					*/
				case PCEPMessageTypes.MESSAGE_TE_LINK_TEAR_DOWN_SUGGESTION:
					log.info("TE_LINK TEAR DOWN SUGGESTION message received");
					//ESTABLISH TE LINK
					//REMOVE TE LINK TO THE LIST
					PCEPTELinkTearDownSuggestion telinkTD;
					try {
						telinkTD = new PCEPTELinkTearDownSuggestion(this.msg);
						lspmanager.removeLSP(((EndPointsIPv4)telinkTD.getEndPoints()).getSourceIP(),((EndPointsIPv4)telinkTD.getEndPoints()).getDestIP());
					} catch (PCEPProtocolViolationException e) {
						e.printStackTrace();
					}
					//NOTIFY THE CHANGE TO THE NETWORK EMULATOR
					break;

					//This case must not be considered
					/*	case PCEPMessageTypes.MESSAGE_MPLS_SUGGESTION:
					//Realizamos la creación de un TeLink (es lo que sale en el caso de uso)
					try {
						boolean state=createIPLink();
						if (!state) {
							log.info("No se puede crear el IPLink, procedemos a buscar otro camino");
							// Aquí tenemos que ver distintas opciones para crear caminos... Buscar otros IPLinks, LSPs, etc
							 OPCIONES
							 -Buscamos en el lspmanager lsp que previamente se hayan guardado...
							 -Preguntamos al PCEL3 por IPLinks ya creados previamente...

						}
					} catch (PCEPProtocolViolationException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

					//Si no se puede, miramos otras opciones...


					break;*/

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

	private void returninterlayernodes() {
		PCEPReport rpt= new PCEPReport();
		rpt.setStateReportList(new LinkedList<StateReport>());
		rpt.getStateReportList().add(new StateReport());
		rpt.getStateReportList().get(0).setLSP(new LSP());
		rpt.getStateReportList().get(0).setSRP(new SRP());
		rpt.getStateReportList().get(0).getLSP().setLspId(0);
		rpt.getStateReportList().get(0).setPath(new Path());

		ExplicitRouteObject ero=new ExplicitRouteObject();
		Iterator<Node>iternodes= vntmGraph.getNodes().iterator();
		while (iternodes.hasNext()){
			Node node=iternodes.next();
			if (!node.getLayer().equals("transport")){
				OpenFlowUnnumberIfIDEROSubobject ofsubobj=new OpenFlowUnnumberIfIDEROSubobject();
				ofsubobj.setSwitchID(node.getNodeID()); ofsubobj.setInterfaceID(0);
				ero.getEROSubobjectList().add(ofsubobj);
			}
		}

		rpt.getStateReportList().get(0).getPath().seteRO(ero);

		try {
			rpt.encode();
		} catch (PCEPProtocolViolationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (out== null)
			System.out.println("No se crea bien el out");
		else{
			try {
				this.out.write(rpt.getBytes());
				this.out.flush();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private void callMultiPCE0(PCEPInitiate pceInit)
	{
		log.info("DEF: Creating MultiPCEL0 Link!!?¿");
		log.info("vntmparams.getPCEL0Address():"+vntmparams.getPCEL0Address()+",vntmparams.getPCEL0Port():"+vntmparams.getPCEL0Port());

		PCEPSessionsInformation pcepSessionManagerPCE=new PCEPSessionsInformation();
		PCCPCEPSession PCEsession = new PCCPCEPSession(vntmparams.getPCEL0Address(), vntmparams.getPCEL0Port() ,false,pcepSessionManagerPCE);
		log.info("vntmparams.getPCEL0Address():"+vntmparams.getPCEL0Address()+",vntmparams.getPCEL0Port():"+vntmparams.getPCEL0Port());
	    PCEsession.start();
	    sleep(1000);
		ClientRequestManager crm = PCEsession.crm;
		if (PCEsession.getOut()==null)
		{
			log.info("La salida esta a null, algo raro pasa...");
		}
		crm.setDataOutputStream(PCEsession.getOut());	

		P2MPEndPointsDataPathID eD = (P2MPEndPointsDataPathID)pceInit.getPcepIntiatedLSPList().get(0).getEndPoint();


		log.info("P2MPWorkflow.pcep_to_l0");
		PCEPRequest p_r = new PCEPRequest();

		Inet4Address ipSource = IPFromDataPath(eD.getSourceDatapathID());


		Request req = new Request();
		RequestParameters rp= new RequestParameters();
		rp.setPbit(true);
		rp.setNbit(true);
		rp.setRequestID(PCCPCEPSession.getNewReqIDCounter());

		req.setRequestParameters(rp);

		P2MPEndPointsIPv4 eIP = new P2MPEndPointsIPv4();				
		eIP.setLeafType(1);
		eIP.setSourceIP(ipSource);
		for (int i=0; i < eD.getDestDatapathIDList().size();i++)
		{
			eIP.setDestIP(IPFromDataPath(eD.getDestDatapathIDList().get(i)));	
		}

		req.setEndPoints(eIP);

		ObjectiveFunction of = new ObjectiveFunction();
		of.setOFcode(40);
		req.setObjectiveFunction(of);	

		BandwidthRequested bandwidth = new BandwidthRequested();
		bandwidth.setBw(100);
		req.setBandwidth(bandwidth);					

		p_r.addRequest(req);

		try {
			p_r.encode();
		} catch (PCEPProtocolViolationException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

	    crm.newRequest(p_r);

		//We suppose everything is OK and procceed to call PCE-L2 and instantiate path

		for (int i = 0; i < eD.getDestDatapathIDList().size(); i++)
		{
			telnetInformPCE(eD.getSourceDatapathID(), eD.getDestDatapathIDList().get(i),
					getConnectingPort(eIP.getSourceIP(), eD.getSourceDatapathID()), 
					getConnectingPort(eIP.getDestIP(i),eD.getDestDatapathIDList().get(i)));

			telnetInformPCE(eD.getDestDatapathIDList().get(i), eD.getSourceDatapathID(),  
					getConnectingPort(eIP.getDestIP(i),eD.getDestDatapathIDList().get(i)),
					getConnectingPort(eIP.getSourceIP(), eD.getSourceDatapathID()));
		}

	    PCEsession.close(NORM_PRIORITY);



		//Sleep a while for PCE to update topology

	    /*
	    sleep(5000);

		log.info("Starting session with PCELevel 2");
		log.info("vntmparams.getPCEL2Address():"+vntmparams.getPCEL2Address()+",vntmparams.getPCEL2Port():"+vntmparams.getPCEL2Port());
		pcepSessionManagerPCE = new PCEPSessionsInformation();
		PCEsession = new PCCPCEPSession(vntmparams.getPCEL2Address(), vntmparams.getPCEL2Port() ,false,pcepSessionManagerPCE);
		PCEsession.start();	
		sleep(1000);
		crm = PCEsession.crm;
		if (PCEsession.getOut()==null)
		{
			log.info("La salida esta a null, algo raro pasa...");
		}
		crm.setDataOutputStream(PCEsession.getOut());

		PCEPResponse pcepReponse = crm.newRequest(createPCEPRequest(pceInit));
		
		
		PCEPInitiate pInit_PM = new PCEPInitiate();
		
		pInit_PM.setPcepIntiatedLSPList(new LinkedList<PCEPIntiatedLSP>());
		PCEPIntiatedLSP ilsp=new PCEPIntiatedLSP();
		
		ilsp.setEro(pcepReponse.getResponse(0).getPath(0).geteRO());
		ilsp.setLsp(new LSP());
		ilsp.setRsp(new SRP());
		ilsp.setEndPoint(eD);
		
		
		pInit_PM.getPcepIntiatedLSPList().add(ilsp);
		//pr2.getPcepIntiatedLSPList().get(0).setBandwidth(telink.getPcepIntiatedLSPList().get(0).getBandwidth());
		
		
		try 
		{	
			//Sending message to PM
			
			Socket clientSocket = new Socket(vntmparams.getPMAddress(), vntmparams.getPMPort());			
			log.info("Socket opened");	
			DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());

			pInit_PM.encode();
			log.info("Sending message At LAST pInit_PM.encode()!");
			outToServer.write(pInit_PM.getBytes());
 
		}
		catch (Exception e) 
		{
			log.info(UtilsFunctions.exceptionToString(e));
		}


		sleep(1000);
		*/
		sendReport();

		return;
	}
	
	//FIXME: Oscar: Organizar estos workflows algun dias...
	private void callMultiPCE02(PCEPInitiate pceInit)
	{
		log.info("OSCAR Initating P2MP request processing");
		log.info("Using Multi-layer Backend PCE"+vntmparams.getMLPCEAddress()+",vntmparams.getPCEL0Port():"+vntmparams.getMLPCEPort());
		

		PCEPSessionsInformation pcepSessionManagerPCE=new PCEPSessionsInformation();
		PCCPCEPSession MLPCEsession = new PCCPCEPSession(vntmparams.getMLPCEAddress(), vntmparams.getMLPCEPort() ,false,pcepSessionManagerPCE);
	    MLPCEsession.start();
	    sleep(1000);
		ClientRequestManager crm = MLPCEsession.crm;
		if (MLPCEsession.getOut()==null) {
			log.info("P2MP: La salida esta a null, algo raro pasa...");
		}
		crm.setDataOutputStream(MLPCEsession.getOut());	

		P2MPEndPointsIPv4 endPoints = (P2MPEndPointsIPv4)pceInit.getPcepIntiatedLSPList().get(0).getEndPoint();


		log.info("P2MPWorkflow.pcep_to_ML_PCE");
		PCEPRequest p_r = new PCEPRequest();
		Request req = new Request();
		p_r.addRequest(req);
		RequestParameters rp= new RequestParameters();
		rp.setPbit(true);
		req.setRequestParameters(rp);		
		rp.setRequestID(PCCPCEPSession.getNewReqIDCounter());
		
		int prio = 1;;
		rp.setPrio(prio);
		boolean reo = false;
		rp.setReopt(reo);
		boolean bi = false;
		rp.setBidirect(bi);
		boolean lo = false;
		rp.setLoose(lo);
		rp.setPbit(true);
		rp.setNbit(true);	
		req.setEndPoints(endPoints);

		Svec svec = new Svec();
		svec.addRequestID(rp.getRequestID());
		SVECConstruct sc = new SVECConstruct();
		
		sc.setSvec(svec);
		ObjectiveFunction of1=new ObjectiveFunction();
		of1.setOFcode(41);
		ObjectiveFunction of2=new ObjectiveFunction();
		of2.setOFcode(42);
		sc.getObjectiveFunctionList().add(of1);
		sc.getObjectiveFunctionList().add(of2);
		BandwidthTLV bwTLV = new BandwidthTLV();
		float bw=(float) 400.0;
		bwTLV.setBw(bw);
		of2.setBwTLV(bwTLV);
		p_r.addSvec(sc);
		InterLayer il =new InterLayer();
		il.setIFlag(true);
		SwitchLayer sl = new SwitchLayer();
		SwitchEncodingType sw1 = new SwitchEncodingType();
		sw1.setLSPEncodingType(2);
		sw1.setSwitchingType(51);
		sw1.setIflag(true);
		SwitchEncodingType sw2 = new SwitchEncodingType();
		sw2.setLSPEncodingType(8);
		sw2.setSwitchingType(150);
		sw2.setIflag(true);
		
		sl.getSwitchLayers().add(sw1);
		sl.getSwitchLayers().add(sw2);
		req.setInterLayer(il);
		req.setSwitchLayer(sl);
		BandwidthRequested bww= new BandwidthRequested();
		float bw_pmp=(float) 100.0;
		bww.setBw(bw_pmp);
		req.setBandwidth(bww);			
		log.info("A LLAMARRRR");
		PCEPResponse pr= crm.newRequest(p_r);
		log.info("Response "+pr.toString());
	    MLPCEsession.close(NORM_PRIORITY);
	    int i;
	    
		log.info("Now using L0 PCE :"+vntmparams.getPCEL0Address()+",vntmparams.getPCEL0Port():"+vntmparams.getPCEL0Port());
		
		PCEPRequest preq = new PCEPRequest();
	    for (i=0;i<pr.ResponseList.get(0).getPathList().size();++i){
	    	Path path=pr.ResponseList.get(0).getPathList().get(i);
	    	if (path.getServerIndication()!=null){
	    		Request request=createRequest(PCCPCEPSession.getNewReqIDCounter(),pr.ResponseList.get(0).getPathList().get(i).geteRO()) ;
	    		preq.addRequest(request);
	    		ObjectiveFunction of=new ObjectiveFunction();
	    		of.setOFcode(42);
	    		request.setObjectiveFunction(of);
	    	}
	    }
	    log.info("LLAMANDO A PCE DE CAPA CEROOOO");
	    PCCPCEPSession L0PCEsession = new PCCPCEPSession(vntmparams.getPCEL0Address(), vntmparams.getPCEL0Port() ,false,pcepSessionManagerPCE);
		L0PCEsession.start();
	    sleep(1000);
		ClientRequestManager crmL0 = L0PCEsession.crm;
		log.info("Asking L0 PCE "+preq.toString());
		PCEPResponse pr2= crmL0.newRequest(preq);
		log.info("Response from L0 PCE "+pr2.toString());
	    MLPCEsession.close(NORM_PRIORITY);
		
	    
	    
	    //We suppose everything is OK and call L0-PCE
	    //ACTUALIZANDO POR OSPF �ASPA TOTAL
	    //LinkedList<Inet4Address> dirPCEList=new  LinkedList<Inet4Address>();
	    //dirPCEList.add((Inet4Address) Inet4Address.getByName("172.16.104.201"));
	    //LinkedList<Integer> portList= new  LinkedList<Integer>();
	    //portList.add(7749);
//	    log.info("AL OSPF RICO");
//
//	    try {
//			TCPOSPFSender tcpospf= new TCPOSPFSender((Inet4Address) Inet4Address.getByName("172.16.104.201"),7749);
//			tcpospf.start();
//			log.info("SEND OSPF");
//			
//			//changes for multilayer OSPF (UpperLayer and LowerLayer)
//			IntraDomainEdge edge = null;
//			Link link1=null;
//			Link link2=null;
//			log.info("OPTICO VA DE "+this.getSourceRouter(preq.getRequest(0).getEndPoints()).getHostAddress());
//			link1=vntmGraph.getLink(this.getSourceRouter(preq.getRequest(0).getEndPoints()).getHostAddress());
//			link2=vntmGraph.getLink(this.getDestRouter(preq.getRequest(0).getEndPoints()).getHostAddress());
//				
//			OSPFv2LinkStateUpdatePacket ospfv2Packet = new OSPFv2LinkStateUpdatePacket();
//			Inet4Address src= (Inet4Address)Inet4Address.getByName(link1.getSource().getNode());
//			log.info("SOURCE es "+src);
//			ospfv2Packet.setRouterID(src);
//			Inet4Address dst= (Inet4Address)Inet4Address.getByName(link2.getSource().getNode());
//			log.info("DEST es "+dst);
//			//ospfv2Packet.((Inet4Address)Inet4Address.getByName(link1.getSource().getNode()));
//
//			LinkedList<LSA> lsaList = new LinkedList<LSA>();
//			OSPFTEv2LSA lsa = new OSPFTEv2LSA();
//			LinkTLV linkTLV=new LinkTLV();
//			lsa.setLinkTLV(linkTLV);
//			MaximumBandwidth mb = new MaximumBandwidth();
//			mb.setMaximumBandwidth(400);
//			linkTLV.setMaximumBandwidth(new MaximumBandwidth());
//			
//			
//			LocalInterfaceIPAddress localInterfaceIPAddress= new LocalInterfaceIPAddress();
//			LinkedList<Inet4Address> lista =localInterfaceIPAddress.getLocalInterfaceIPAddressList();
//			lista.add(src);
//			linkTLV.setLocalInterfaceIPAddress(localInterfaceIPAddress);
//			RemoteInterfaceIPAddress remoteInterfaceIPAddress= new RemoteInterfaceIPAddress();
//			LinkedList<Inet4Address> listar = remoteInterfaceIPAddress.getRemoteInterfaceIPAddressList();
//			listar.add(dst);
//			linkTLV.setRemoteInterfaceIPAddress(remoteInterfaceIPAddress);
//			LinkID linkID = new LinkID();
//			linkID.setLinkID(dst);
//			linkTLV.setLinkID(linkID);
//						
//			lsaList.add(lsa);
//			ospfv2Packet.setLSAlist(lsaList);
//			tcpospf.getSendingQueue().add((LSA)lsa);
//			
//			
//		} catch (UnknownHostException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
	    
		//sendMessageOSPF(getNodeByName(preq.getRequestList().get(0), uil.getTopology()), getNodeByName(uil.getIpDest(), uil.getTopology()), bw, uil.isDelete());
		sendReport();

		return;
	}
	
	public Inet4Address getSourceRouter(EndPoints  EP) {
		Inet4Address source_router_id_addr=null;
		if (EP.getOT()==ObjectParameters.PCEP_OBJECT_TYPE_ENDPOINTS_IPV4){
			EndPointsIPv4  ep=(EndPointsIPv4) EP;
			source_router_id_addr=ep.getSourceIP();
		}else if (EP.getOT()==ObjectParameters.PCEP_OBJECT_TYPE_ENDPOINTS_IPV6){

		}

		if (EP.getOT()==ObjectParameters.PCEP_OBJECT_TYPE_GENERALIZED_ENDPOINTS){
			GeneralizedEndPoints  gep=(GeneralizedEndPoints) EP;
			if(gep.getGeneralizedEndPointsType()==ObjectParameters.PCEP_GENERALIZED_END_POINTS_TYPE_P2P){
				P2PEndpoints p2pep= gep.getP2PEndpoints();
				EndPoint sourceep=p2pep.getSourceEndPoint();
				if (sourceep.getEndPointIPv4TLV()!=null){
					source_router_id_addr=sourceep.getEndPointIPv4TLV().getIPv4address();
				}else if (sourceep.getUnnumberedEndpoint()!=null){
					source_router_id_addr=sourceep.getUnnumberedEndpoint().getIPv4address();
				}			
			}
			if(gep.getGeneralizedEndPointsType()==ObjectParameters.PCEP_GENERALIZED_END_POINTS_TYPE_P2MP_NEW_LEAVES){
				P2MPEndpoints p2mpep= gep.getP2MPEndpoints();
				EndPointAndRestrictions epandrest=p2mpep.getEndPointAndRestrictions();
				EndPoint sourceep=epandrest.getEndPoint();
				source_router_id_addr=sourceep.getEndPointIPv4TLV().IPv4address;
				int cont=0;
				while (cont<=p2mpep.getEndPointAndRestrictionsList().size()){ //esto est� mal
					epandrest=p2mpep.getEndPointAndRestrictionsList().get(cont);
					source_router_id_addr=sourceep.getEndPointIPv4TLV().IPv4address;

				}
			}
		}
		return source_router_id_addr;
	}

	public long getSourceIfID(EndPoints  EP) {
		long if_id=-1;
		if (EP.getOT()==ObjectParameters.PCEP_OBJECT_TYPE_GENERALIZED_ENDPOINTS){
			GeneralizedEndPoints  gep=(GeneralizedEndPoints) EP;
			if(gep.getGeneralizedEndPointsType()==ObjectParameters.PCEP_GENERALIZED_END_POINTS_TYPE_P2P){
				if (gep.getP2PEndpoints().getSourceEndPoint().getUnnumberedEndpoint()!=null){
					if_id =gep.getP2PEndpoints().getSourceEndPoint().getUnnumberedEndpoint().getIfID() ;
				}			
			}

		}
		return if_id;
	}

	public long getDestIfID(EndPoints  EP) {
		long if_id=-1;
		if (EP.getOT()==ObjectParameters.PCEP_OBJECT_TYPE_GENERALIZED_ENDPOINTS){
			GeneralizedEndPoints  gep=(GeneralizedEndPoints) EP;
			if(gep.getGeneralizedEndPointsType()==ObjectParameters.PCEP_GENERALIZED_END_POINTS_TYPE_P2P){
				if (gep.getP2PEndpoints().getDestinationEndPoint().getUnnumberedEndpoint()!=null){
					if_id =gep.getP2PEndpoints().getDestinationEndPoint().getUnnumberedEndpoint().getIfID() ;
				}			
			}

		}
		return if_id;
	}


	public Inet4Address getDestRouter(EndPoints  EP) {
		Inet4Address dest_router_id_addr=null;
		if (EP.getOT()==ObjectParameters.PCEP_OBJECT_TYPE_ENDPOINTS_IPV4){
			EndPointsIPv4  ep=(EndPointsIPv4) EP;
			dest_router_id_addr=ep.getDestIP();
		}else if (EP.getOT()==ObjectParameters.PCEP_OBJECT_TYPE_ENDPOINTS_IPV6){

		}

		if (EP.getOT()==ObjectParameters.PCEP_OBJECT_TYPE_GENERALIZED_ENDPOINTS){
			GeneralizedEndPoints  gep=(GeneralizedEndPoints) EP;
			if(gep.getGeneralizedEndPointsType()==ObjectParameters.PCEP_GENERALIZED_END_POINTS_TYPE_P2P){
				P2PEndpoints p2pep= gep.getP2PEndpoints();
				EndPoint sourceep=p2pep.getSourceEndPoint();
				EndPoint destep=p2pep.getDestinationEndPoint();

				if (destep.getEndPointIPv4TLV()!=null){
					dest_router_id_addr=destep.getEndPointIPv4TLV().getIPv4address();
				}else if (destep.getUnnumberedEndpoint()!=null){
					dest_router_id_addr=destep.getUnnumberedEndpoint().getIPv4address();
				}

			}

		}

		return dest_router_id_addr;

	}

	
	private Request createRequest(long id_request, ExplicitRouteObject ero) {
		Request req = new Request();
		RequestParameters rp= new RequestParameters();
		rp.setPbit(true);
		req.setRequestParameters(rp);		
		rp.setRequestID(id_request);

		int prio = 1;;
		rp.setPrio(prio);
		boolean reo = false;
		rp.setReopt(reo);
		boolean bi = false;
		rp.setBidirect(bi);
		boolean lo = false;
		rp.setLoose(lo);
		boolean gen=false;
		int src_if=0;
		int dst_if=0;
		Inet4Address src_ip=null;
		Inet4Address dst_ip=null;
		if (ero.getEROSubobjectList().getFirst() instanceof IPv4prefixEROSubobject){
			log.info("OSCAR: SOURCE IS IPv4prefixEROSubobject");
			src_ip=((IPv4prefixEROSubobject)ero.getEROSubobjectList().getFirst()).getIpv4address();
		} else if (ero.getEROSubobjectList().getFirst() instanceof UnnumberIfIDEROSubobject) {
			log.info("OSCAR: SOURCE IS UnnumberIfIDEROSubobject");
			src_ip=((UnnumberIfIDEROSubobject)ero.getEROSubobjectList().getFirst()).getRouterID();

		}
		if (ero.getEROSubobjectList().getLast() instanceof IPv4prefixEROSubobject){
			log.info("OSCAR: DEST IS IPv4prefixEROSubobject");
			dst_ip=((IPv4prefixEROSubobject)ero.getEROSubobjectList().getLast()).getIpv4address();
		} else if (ero.getEROSubobjectList().getLast() instanceof UnnumberIfIDEROSubobject) {
			log.info("OSCAR: DEST IS UnnumberIfIDEROSubobject");
			dst_ip=((UnnumberIfIDEROSubobject)ero.getEROSubobjectList().getLast()).getRouterID();

		}
		
		if (gen==true){
			GeneralizedEndPoints ep=new GeneralizedEndPoints();
			req.setEndPoints(ep);
			P2PEndpoints p2pEndpoints = new P2PEndpoints();	
			EndPoint ep_s =new EndPoint();
			p2pEndpoints.setSourceEndPoints(ep_s);
			EndPoint ep_d =new EndPoint();
			p2pEndpoints.setDestinationEndPoints(ep_d);
			ep.setP2PEndpoints(p2pEndpoints);
			if (src_if!=0){
				UnnumberedEndpointTLV un = new UnnumberedEndpointTLV();
				
					un.setIPv4address(src_ip);
					un.setIfID(src_if);
					ep_s.setUnnumberedEndpoint(un);


			}else {
				EndPointIPv4TLV ipv4tlv = new EndPointIPv4TLV();
				
					ipv4tlv.setIPv4address(src_ip);
					ep_s.setEndPointIPv4TLV(ipv4tlv);


			}
			if (dst_if!=0){
				UnnumberedEndpointTLV un = new UnnumberedEndpointTLV();
				Inet4Address ipp;
			
					un.setIPv4address(dst_ip);
					un.setIfID(dst_if);
					ep_d.setUnnumberedEndpoint(un);

			}else {
				EndPointIPv4TLV ipv4tlv = new EndPointIPv4TLV();
				Inet4Address ipp;
			
					ipv4tlv.setIPv4address(src_ip);
					ep_d.setEndPointIPv4TLV(ipv4tlv);

	

			}

		} else {
			EndPointsIPv4 ep=new EndPointsIPv4();				
			req.setEndPoints(ep);
		
				ep.setSourceIP(src_ip);								
	
				ep.setDestIP(dst_ip);
	
		}
		IncludeRouteObject iRO=new IncludeRouteObject();
		iRO.setIROList(ero.getEROSubobjectList());
		req.setiRO(iRO);

		return req;

	}


	private int getConnectingPort(Inet4Address adress, String switch_id)
	{
		return 3;
	}

	private Inet4Address IPFromDataPath(String switchID)
	{
		Inet4Address int4a = null;
		log.info("switchID:" + switchID);
		try
		{
			/*
			 * 	[RouterInfoPM]->00:00:66:64:22:a0:c0:48
	[RouterInfoPM]->10:00:2c:59:e5:66:ed:00
	[RouterInfoPM]->00:00:1e:7c:02:77:44:41
	[RouterInfoPM]->10:00:2c:59:e5:5e:2b:00

			 */

			if (switchID.equals("00:00:66:64:22:a0:c0:48"))
			{
				int4a = (Inet4Address) InetAddress.getByName("10.0.0.1");
			}
			else if (switchID.equals("10:00:2c:59:e5:66:ed:00"))
			{
				int4a = (Inet4Address) InetAddress.getByName("10.0.0.2");
			}
			else if (switchID.equals("00:00:1e:7c:02:77:44:41"))
			{
				int4a = (Inet4Address) InetAddress.getByName("10.0.0.3");
			}
			else if (switchID.equals("10:00:2c:59:e5:5e:2b:00"))
			{
				int4a = (Inet4Address) InetAddress.getByName("10.0.0.4");
			}
			else
			{
				log.info("BOOM BOOM Error");
			}
		}
		catch (UnknownHostException e) 
		{
			log.info(UtilsFunctions.exceptionToString(e));
		}
		log.info("switchID:" + int4a);
		return int4a;
	}

	private PCEPRequest createPCEPRequest(PCEPInitiate pceInit)
	{
		PCEPRequest p_r = new PCEPRequest();
		try
		{		
			P2MPEndPointsDataPathID eD = (P2MPEndPointsDataPathID)pceInit.getPcepIntiatedLSPList().get(0).getEndPoint();

			String source = eD.getSourceDatapathID();
			Request req = new Request();
			p_r.addRequest(req);
			RequestParameters rp= new RequestParameters();
			rp.setPbit(true);
			rp.setNbit(true);
			req.setRequestParameters(rp);
			rp.setRequestID(PCCPCEPSession.getNewReqIDCounter());
			P2MPEndPointsDataPathID ep=new P2MPEndPointsDataPathID();				
			req.setEndPoints(ep);
			ep.setLeafType(1);
			ep.setSourceDatapathID(source);


			for (int i=0; i < eD.getDestDatapathIDList().size(); i++)
			{
				ep.getDestDatapathIDList().add(eD.getDestDatapathIDList().get(i));	
			}

			ObjectiveFunction of=new ObjectiveFunction();
			of.setOFcode(1004);
			req.setObjectiveFunction(of);


			BandwidthRequested bandwidth = new BandwidthRequested();
			bandwidth.setBw(100);
			req.setBandwidth(bandwidth);					

			return p_r;

		}
		catch(Exception e)
		{
			log.info("Exception");
			log.info(UtilsFunctions.exceptionToString(e));
			return null;
		}
	}

	private boolean createVLANLink()throws PCEPProtocolViolationException, IOException, InterruptedException 
	{
		log.info("Creating VLAN Link!");
		log.info("vntmparams.getPCEL0Address():"+vntmparams.getPCEL0Address()+",vntmparams.getPCEL0Port():"+vntmparams.getPCEL0Port());
		PCEPSessionsInformation pcepSessionManagerPCE=new PCEPSessionsInformation();
		PCCPCEPSession PCEsession = new PCCPCEPSession(vntmparams.getPCEL0Address(), vntmparams.getPCEL0Port() ,false,pcepSessionManagerPCE);
		log.info("vntmparams.getPCEL0Address():"+vntmparams.getPCEL0Address()+",vntmparams.getPCEL0Port():"+vntmparams.getPCEL0Port());
		PCEsession.start();	
		Thread.currentThread().sleep(1000);
		ClientRequestManager crm = PCEsession.crm;
		if (PCEsession.getOut()==null)
		{
			log.info("La salida esta a null, algo raro pasa...");
		}
		crm.setDataOutputStream(PCEsession.getOut());

		PCEPInitiate p_init = new PCEPInitiate(this.msg);

		PCEPRequest p_req = new PCEPRequest();
		Request req = new Request();
		req.setEndPoints(p_init.getPcepIntiatedLSPList().get(0).getEndPoint());


		float bw = 100;
		BandwidthRequested bandwidth=new BandwidthRequested();
		bandwidth.setBw(bw);

		req.setBandwidth(bandwidth);

		Reservation reservation = new Reservation();
		req.setReservation(reservation);

		RequestParameters reqParams = new RequestParameters();
		reqParams.setBidirect(true);
		reqParams.setRequestID(12465);

		req.setRequestParameters(reqParams);

		ObjectiveFunction obFunc = new ObjectiveFunction();
		obFunc.setOFcode(1002);

		XifiUniCastEndPoints endP = (XifiUniCastEndPoints)req.getEndPoints();

		EndPointsIPv4 endP_IP = new EndPointsIPv4();

		String source_switch = endP.getSwitchSourceID();
		String dest_switch = endP.getSwitchDestinationID();

		Inet4Address ip_dest = (Inet4Address)InetAddress.getByName(get_corresponding_gmpl_switch(dest_switch));
		Inet4Address ip_source = (Inet4Address)InetAddress.getByName(get_corresponding_gmpl_switch(source_switch));

		endP_IP.setDestIP(ip_dest);
		endP_IP.setSourceIP(ip_source);

		req.setEndPoints(endP_IP);
		req.setObjectiveFunction(obFunc);

		LinkedList<Request> requestList=new LinkedList<Request>();

		requestList.add(req);

		p_req.setRequestList(requestList);



		PCEPResponse p_response = crm.newRequest(p_req);

		PCEPInitiate pceInit = new PCEPInitiate();

		SRP rsp = new SRP();
		LSP lsp = new LSP();

		ExplicitRouteObject ero;
		ero = (p_response.getResponse(0).getPath(0).geteRO());

		PCEPIntiatedLSP pcepIntiatedLSPList = new PCEPIntiatedLSP();
		pcepIntiatedLSPList.setEro(ero);
		pcepIntiatedLSPList.setRsp(rsp);
		pcepIntiatedLSPList.setLsp(lsp);
		//pcepIntiatedLSPList.setEndPoint(endP);


		endP_IP = new EndPointsIPv4();
		endP_IP.setSourceIP(ip_source);
		endP_IP.setDestIP(ip_dest);

		pcepIntiatedLSPList.setEndPoint(endP_IP);

		pceInit.getPcepIntiatedLSPList().add(pcepIntiatedLSPList);	

		/*
		socket=new Socket(vntmparams.getPMAddress(), vntmparams.getPMPort());
		DataOutputStream out = new DataOutputStream(socket.getOutputStream());
		DataInputStream in = new DataInputStream(socket.getInputStream());

		out.write(p_response.getBytes());
		out.flush();

		//Recibimos respuesta del PM y enviamos al ABNO sin mirar.
		respondABNO();
		 */


		try 
		{	
			//Sending message to tn1
			Socket clientSocket = new Socket(vntmparams.getPMAddress(), vntmparams.getPMPort());			
			log.info("Socket opened");	
			DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
			try 
			{
				pceInit.encode();
				log.info("Sending message At LAST!");
				outToServer.write(pceInit.getBytes());
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
			log.severe("Couldn't get I/O for connection to port" + vntmparams.getPMPort());
		}


		telnetInformPCE(source_switch, dest_switch, get_corresponding_interface(source_switch), get_corresponding_interface(dest_switch));

		try 
		{
			Thread.sleep(3500);
		}
		catch (InterruptedException e) 
		{
			UtilsFunctions.exceptionToString(e);
		}

		sendReport();
		//respondABNO();

		return true;
	}

	private boolean createIPLink() throws PCEPProtocolViolationException, IOException, InterruptedException {
		log.info("Creating IP Link!");
		PCEPInitiate telink = new PCEPInitiate(this.msg);
		telink.decode();
		Inet4Address source= ((EndPointsIPv4) telink.getPcepIntiatedLSPList().get(0).getEndPoint()).getSourceIP();
		Inet4Address dest= ((EndPointsIPv4) telink.getPcepIntiatedLSPList().get(0).getEndPoint()).getDestIP();

		System.out.println(vntmGraph.getNode(source));


		System.out.println("query from: "+source.toString()+" to: "+dest.toString());
		if (vntmGraph.getNode(dest)==null)
			System.out.println("Dest mal leidom guardado o algo asin");

		if (vntmGraph.getNode(source)==null)
			System.out.println("Source mal leido, guardado o algo asin");
		Link linkSource = vntmGraph.getLink(vntmGraph.getNode(source).getNodeID());
		Link linkDest = vntmGraph.getLink(vntmGraph.getNode(dest).getNodeID());

		Inet4Address ipOpcticSource= (Inet4Address)Inet4Address.getByName(vntmGraph.getNode(linkSource.getDest().getNode()).getAddress().get(0)); //String con la direccion ip de salida

		Inet4Address ipOpticDest =(Inet4Address)Inet4Address.getByName(vntmGraph.getNode(linkDest.getDest().getNode()).getAddress().get(0));//String con la direccion ip de destino

		//Sending pcep message
		ExplicitRouteObject ero=new ExplicitRouteObject();
		if (!telink.getPcepIntiatedLSPList().get(0).getRsp().isrFlag()) {
			PCEPSessionsInformation pcepSessionManagerPCE=new PCEPSessionsInformation();
			PCCPCEPSession PCEsession = new PCCPCEPSession(vntmparams.getPCEL0Address(), vntmparams.getPCEL0Port() ,false,pcepSessionManagerPCE);
			PCEsession.start();	
			Thread.currentThread().sleep(1000);
			ClientRequestManager crm = PCEsession.crm;
			if (PCEsession.getOut()==null)
				System.out.println("La salida esta a null, algo raro pasa...");
			crm.setDataOutputStream(PCEsession.getOut());

			System.out.println("Enviamos de: "+ipOpcticSource+" a "+ipOpticDest);

			PCEPRequest p_r = new PCEPRequest();
			Request req = new Request();
			p_r.addRequest(req);
			RequestParameters rp= new RequestParameters();
			rp.setPbit(true);
			req.setRequestParameters(rp);
			rp.setRequestID(PCCPCEPSession.getNewReqIDCounter());
			//EndPointsIPv4 ep=new EndPointsIPv4();
			EndPointsUnnumberedIntf ep= new EndPointsUnnumberedIntf();
			req.setEndPoints(ep);
			ep.setSourceIP(ipOpcticSource);	
			ep.setDestIP(ipOpticDest);

			//This set is on fireeeee...
			int nodesource=source.getHostAddress().equals("10.95.73.72")?1:source.getHostAddress().equals("10.95.73.73")?2:source.getHostAddress().equals("10.95.73.74")?3:0;
			int nodedest=source.getHostAddress().equals("10.95.73.72")?1:source.getHostAddress().equals("10.95.73.73")?2:source.getHostAddress().equals("10.95.73.74")?3:0;
			ep.setDestIF(getUnnumberIfFIRE(nodedest, nodesource));
			ep.setSourceIF(getUnnumberIfFIRE(nodesource, nodedest));
			//

			ObjectiveFunction of=new ObjectiveFunction();
			of.setOFcode(1200);
			req.setObjectiveFunction(of);


			float bw = 100;
			BandwidthRequested bandwidth=new BandwidthRequested();
			bandwidth.setBw(bw);
			req.setBandwidth(bandwidth);					


			PCEPResponse pr=crm.newRequest(p_r);
			log.info("Respuesta de PCE-l0 : "+pr.toString());


			//Enviamos la petición al ProvisioningManager
			IPv4prefixEROSubobject eroSubSource = new IPv4prefixEROSubobject();
			eroSubSource.setIpv4address(source);
			IPv4prefixEROSubobject eroSubDest = new IPv4prefixEROSubobject();
			eroSubDest.setIpv4address(dest);

			pr.getResponse(0).getPath(0).geteRO().getEROSubobjectList().add(0, eroSubSource);
			pr.getResponse(0).getPath(0).geteRO().getEROSubobjectList().add(eroSubDest);
			ero=pr.getResponse(0).getPath(0).geteRO();
		} else {
			idToDelete=telink.getPcepIntiatedLSPList().get(0).getLsp().getLspId();
			IPv4prefixEROSubobject eroSubSource = new IPv4prefixEROSubobject();
			eroSubSource.setIpv4address(source);
			IPv4prefixEROSubobject eroSubDest = new IPv4prefixEROSubobject();
			eroSubDest.setIpv4address(dest);
			ero.getEROSubobjectList().add(eroSubSource);
			ero.getEROSubobjectList().add(eroSubDest);
			//mapping id to be delete
			System.out.println("VNTM: Vamos a borrar la id="+idToDelete);
			telink.getPcepIntiatedLSPList().get(0).getLsp().setLspId((int)oPtable.get(idToDelete).getPCCoperationID());
		}

		socket=new Socket(vntmparams.getPMAddress(), vntmparams.getPMPort());
		DataOutputStream out = new DataOutputStream(socket.getOutputStream());
		DataInputStream in = new DataInputStream(socket.getInputStream());
		PCEPInitiate pr2=new PCEPInitiate();
		pr2.setPcepIntiatedLSPList(new LinkedList<PCEPIntiatedLSP>());
		PCEPIntiatedLSP ilsp=new PCEPIntiatedLSP();
		pr2.getPcepIntiatedLSPList().add(ilsp);
		pr2.getPcepIntiatedLSPList().get(0).setEro(ero);
		pr2.getPcepIntiatedLSPList().get(0).setLsp(telink.getPcepIntiatedLSPList().get(0).getLsp());
		pr2.getPcepIntiatedLSPList().get(0).setRsp(telink.getPcepIntiatedLSPList().get(0).getRsp());
		pr2.getPcepIntiatedLSPList().get(0).setEndPoint(telink.getPcepIntiatedLSPList().get(0).getEndPoint());
		//pr2.getPcepIntiatedLSPList().get(0).setBandwidth(telink.getPcepIntiatedLSPList().get(0).getBandwidth());
		Thread.currentThread().sleep(1000);
		pr2.encode();

		out.write(pr2.getBytes());
		out.flush();



		//Recibimos respuesta del PM y enviamos al ABNO sin mirar.
		respondABNO(socket, in);



		return true;
	}


	private void telnetInformPCE(String sourceSwitchID, String destSwitchID, Integer source_interface, Integer destination_interface)
	{
		try
		{
			log.info("Calling PCE to add link by telnet");
			Socket connectionToTheServer = new Socket("localhost", 6666);
            OutputStream out = connectionToTheServer.getOutputStream();

            PrintStream ps = new PrintStream(out, true);

            ps.println("add xifi link");
            BufferedReader br = new BufferedReader(new InputStreamReader(connectionToTheServer.getInputStream()));
            log.info(br.readLine());
            log.info("commmand::"+sourceSwitchID+"-"+destSwitchID+"-"+source_interface+"-" + destination_interface);
            ps.println(sourceSwitchID+"-"+destSwitchID+"-"+source_interface+"-" + destination_interface);
            //ps.println("10:00:2c:59:e5:66:ed:00:19-10:00:2c:59:e5:5e:2b:00:19-2-4");
            connectionToTheServer.close();

		} 
		catch (Exception e) 
		{
			log.info(UtilsFunctions.exceptionToString(e));
		}
	}


	private void respondABNO(Socket socket, DataInputStream in)
	{
		try
		{
			byte[] salida2=null;
			int counter=0;
			while ((counter<200)&&(salida2==null)){	
				salida2=this.readMsg2(in);
				if (PCEPMessage.getMessageType(salida2)!=PCEPMessageTypes.MESSAGE_REPORT){
					salida2=null;
					System.out.println("No Report");
					in = new DataInputStream(socket.getInputStream());
				}

				Thread.currentThread().sleep(1000);
				counter++;
			}
			//PCEPReport pcepReport = new PCEPReport(salida2);
			log.info("Ouput(Sending Report):"+salida2.toString());
			if ((out== null)||(salida2==null))
			{
				System.out.println("No se crea bien el out");
			}
			else{
				PCEPReport pceprep=new PCEPReport(salida2);
				if (idToDelete==-1){
					this.oPtable.put((long)oPcounter.incrementAndGet(), new OpTable("ABNOController", vntmparams.getPMAddress(), String.valueOf(vntmparams.getPMPort()), pceprep.getStateReportList().get(0).getLSP().getLspId(), null));
					pceprep.getStateReportList().get(0).getLSP().setLspId((int)oPcounter.get());
					System.out.println("VNTM: Guardamos con la id="+oPcounter.get());
				} else {
					this.oPtable.remove(idToDelete);
					pceprep.getStateReportList().get(0).getLSP().setLspId((int)idToDelete);
				}
				log.info("Sending PCEP message");
				pceprep.encode();
				this.out.write(pceprep.getBytes());
				this.out.flush();
				this.printOPTable();
			}
		}
		catch (Exception e)
		{
			log.info(UtilsFunctions.exceptionToString(e));
		}
	}
	private void sendLSP(DataOutputStream out, PCEPTELinkConfirmation telinkconf) {
		try {  
			out.write(telinkconf.getBytes());
			out.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}   
	}
	/**
	 * This function assumes a 1-1 correspondence between switch layer 2 and IP layer
	 * @param switch_id
	 * @return
	 */

	private Integer get_corresponding_interface(String switch_id)
	{
		Link link =  vntmGraph.getLink(switch_id);
		Node node = vntmGraph.getNode(link.getSource().getNode());
		return Integer.parseInt(node.getIntfList().get(0).getAddress().get(0));
	}

	private String get_corresponding_gmpl_switch(String switch_id)
	{	

		Link link =  vntmGraph.getLink(switch_id);

		log.info("link.getDest().getNode()::"+link.getDest().getNode());

		return link.getDest().getNode();

		/*
		if (switch_id.equals("10:00:2c:59:e5:66:ed:00"))
		{
			return "192.168.1.1";
		}
		if (switch_id.equals("10:00:2c:59:e5:64:21:00"))
		{
			return "192.168.1.2";
		}
		if (switch_id.equals("10:00:2c:59:e5:5e:2b:00"))
		{
			return "192.168.1.4";
		}
		log.info("Error: Soldado! BOOOOOOOOM");
		return "Error";
		 */
	}

	private int getUnnumberIfFIRE(int from, int to) {
		if ((from==1)&&(to==2)){
			return 3;
		} else if ((from==2)&&(to==1)){
			return 4;
		} else if ((from==1)&&(to==3)){
			return 4;
		} else if ((from==3)&&(to==1)){
			return 2;
		} else if ((from==2)&&(to==3)){
			return 3;
		} else if ((from==3)&&(to==2)){
			return 3;
		}

		return 0;
	}

	private String readMsgTM(DataInputStream in) throws IOException{
		byte[] hdr = new byte[4];
		byte[] temp = null;
		boolean endHdr = false;
		int r = 0;
		int length = 0;
		boolean endMsg = false;
		int offsetHdr = 0;            	
		int offset = -1;
		while (!endMsg) {
			try {
				if (endHdr) { 
					r = in.read(temp, offset, 1);
					if (r == -1){
						return null;
					}
				}
				else {		
					if (hdr != null){
						r = in.read(hdr, offsetHdr, 1);
						if (r == -1){
							return null;
						}
					}
				}
			}catch (IOException e){
				//System.out.println("Salgo por excepcion");
				throw e;
			}catch (Exception e) {		
				throw new IOException();
			}
			if (r > 0) {
				if (offsetHdr == 2) {
					length = ((int)hdr[offsetHdr]&0xFF) << 8;
				}
				if (offsetHdr == 3) {
					length = length | (((int)hdr[offsetHdr]&0xFF));                					
					temp = new byte[length];
					endHdr = true;
					offsetHdr++;
				}
				if ((length > 0) && (offset == length - 1)) {
					endMsg = true;
				}
				if (endHdr){                				
					offset++;
				}
				else {
					offsetHdr++;
				}
			}
			else if (r==-1){
				//log.warning("End of stream has been reached");
				throw new IOException();
			}
		}
		if (length > 0) {                			
			String response = new String(temp);
			return response;	 //Respuesta sin la cabecera               			
		}	
		else return null;

	}

	protected void endSession() {
		// TODO Auto-generated method stub
	}

	protected byte[] readMsg2(DataInputStream in) throws IOException{
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
				log.warning("Error reading data: "+ e.getMessage());
				throw e;
			}catch (Exception e) {
				log.warning("readMsg Oops: " + e.getMessage());
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
				log.warning("End of stream has been reached");
				throw new IOException();
			}
		}
		if (length > 0) {
			ret = new byte[length];
			System.arraycopy(temp, 0, ret, 0, length);
		}		
		return ret;
	}

	protected void printOPTable() {

		Iterator<Long> intit=this.oPtable.keySet().iterator();
		System.out.println("-----------------------------------------------------------------");
		System.out.println("ID\tOwner\t\tProvIP\t\tProvID");
		System.out.println("-----------------------------------------------------------------");
		while(intit.hasNext()){
			long id=intit.next();
			OpTable opt=this.oPtable.get(id);
			System.out.println(id+"\t"+opt.getAppIP()+"\t"+opt.getPCCIP()+"\t"+opt.getPCCoperationID());
		}
		System.out.println("-----------------------------------------------------------------");
	}

	private void sendReport()
	{
		PCEPReport rpt= new PCEPReport();

		try 
		{
			rpt.encode();
		} catch (PCEPProtocolViolationException e) 
		{
			log.info(UtilsFunctions.exceptionToString(e));
		}
		if (out== null)
		{
			log.info("No se crea bien el out");
		}
		else
		{
			try 
			{
				log.info("Enviamos Report hacia atras :"+rpt.getBytes());
				this.out.write(rpt.getBytes());
				this.out.flush();
			} 
			catch (IOException e) 
			{
				log.info(UtilsFunctions.exceptionToString(e));
			}
		}
	}
	private void sleep(int s)
	{
		try 
		{
			Thread.currentThread().sleep(s);
		} 
		catch (InterruptedException e) 
		{
			log.info(UtilsFunctions.exceptionToString(e));
		}
	}
}