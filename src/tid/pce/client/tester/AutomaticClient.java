package tid.pce.client.tester;


import java.util.Hashtable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import es.tid.ospf.ospfv2.OSPFv2LinkStateUpdatePacket;
import tid.netManager.NetworkLSPManager;
import tid.netManager.NetworkLSPManagerParameters;
import tid.netManager.OSPFSender;
import tid.netManager.TCPOSPFSender;
import tid.netManager.emulated.AdvancedEmulatedNetworkLSPManager;
import tid.netManager.emulated.CompletedEmulatedNetworkLSPManager;
import tid.netManager.emulated.DummyEmulatedNetworkLSPManager;
import tid.netManager.emulated.SimpleEmulatedNetworkLSPManager;
import tid.pce.client.PCCPCEPSession;
import tid.pce.client.emulator.AutomaticTesterStatistics;
import tid.pce.client.emulator.Emulator;
import tid.pce.pcepsession.PCEPSessionsInformation;
import tid.vntm.VNTMParameters;

/**
 * Testeador de caminos.
 * Crea una Sesion PCEP con el PCE. Y llama a un emulador de red.
 *  Va lanzando peticiones periodicamente al PCE. Controla las estadisticas.
 * @author Marta Cuaresma Saturio
 *
 */
public class AutomaticClient{

	private static Hashtable<Integer,PCCPCEPSession> PCEsessionList;
	private static Logger log=Logger.getLogger("PCCClient");
	private static Logger log2=Logger.getLogger("PCEPParser");
	private static Logger log3=Logger.getLogger("OSPFParser");
	private static Logger log4=Logger.getLogger("NetworkLSPManager");
//	private static Logger log5=Logger.getLogger("NetworkLSPManager");
//	private static Logger log6=Logger.getLogger("requestID");
//	private static Logger log7=Logger.getLogger("mmmerrores");
	private static String networkEmulatorFile="NetworkEmulatorConfiguration.xml";
	private static InformationRequest testerParams;
	static AutomaticTesterStatistics stats;
	
	//Timer ligthPathManagementTimer;
	/*Variable used for counter how many requests there are*/
	/**
	 * @param args
	 */
	
	public static void main(String[] args) {
		
		/*If there are arguments, read the PCEServerPort and ipPCE*/
		if (args.length < 1) {
			log.info("Usage: ClientTester <XML File>");
			return;
		}
		//Initialize loggers
		FileHandler fh;
		FileHandler fh2;
		FileHandler fh3;
		FileHandler fh4;
//		FileHandler fh5;
//		FileHandler fh6;
//		FileHandler fh7;
		testerParams = new InformationRequest();
		testerParams.readFile(args[0]);
		try {
			fh=new FileHandler("PCCClient.log");
			fh2=new FileHandler("PCEPClientParser.log");
			fh3=new FileHandler("OSPFParser.log");
//			fh4=new FileHandler("PruebaLambdas.log");
			fh4 = new  FileHandler("NetworkLSPManager.log");
//			fh6 = new  FileHandler("requestID.log");
//			fh7 = new  FileHandler("mmmerrores.log");
			//fh.setFormatter(new SimpleFormatter());
//			fh6.setFormatter(new SimpleFormatter());
//			fh7.setFormatter(new SimpleFormatter());
//			fh4.setFormatter(new SimpleFormatter());
			log.addHandler(fh);
			log2.addHandler(fh2);				
			log3.addHandler(fh3);
			log4.addHandler(fh4);
			if (testerParams.isSetTraces() == false){		    	
				log.setLevel(Level.SEVERE);
				log2.setLevel(Level.SEVERE);	
				log3.setLevel(Level.SEVERE);
				log4.setLevel(Level.SEVERE);
			}				
			else{
				log.setLevel(Level.ALL);
				log2.setLevel(Level.ALL);
				log3.setLevel(Level.ALL);
				log4.setLevel(Level.ALL);
			}
//			log5.setLevel(Level.ALL);
//			log5.addHandler(fh5);
//			log6.setLevel(Level.ALL);
//			log6.addHandler(fh6);
//			log7.setLevel(Level.ALL);
//			log7.addHandler(fh6);
		} catch (Exception e1) {
			e1.printStackTrace();
			System.exit(1);
		}
		log.info("Create PCE Session List");
		PCEsessionList= new Hashtable<Integer,PCCPCEPSession>();
		for (int i =0;i<testerParams.getPCCPCEPsessionParams().getNumSessions();i++){
			log.info("PCE IP : "+testerParams.getPCCPCEPsessionParams().getIpPCEList().get(i));
			PCEPSessionsInformation pcepSessionManager = new PCEPSessionsInformation();
			PCCPCEPSession PCEsession = new PCCPCEPSession(testerParams.getPCCPCEPsessionParams().getIpPCEList().get(i), testerParams.getPCCPCEPsessionParams().getPCEServerPortList().get(i), testerParams.getPCCPCEPsessionParams().isNoDelay(), pcepSessionManager);
			PCEsessionList.put(i, PCEsession);
			log.info("Start PCE Session "+i);
			PCEsession.start();
		}
		NetworkLSPManager networkLSPManager = null;
		PCCPCEPSession VNTMSession=null;
		
		//Creamos las estadísticas
		stats = new AutomaticTesterStatistics(testerParams.getLoadIni());
		LSPConfirmationDispatcher lspDispatcher=null;
		if (testerParams.isNetworkEmulator() && testerParams.isVNTMSession()){
			networkLSPManager = createNetworkLSPManager();
			lspDispatcher = new LSPConfirmationDispatcher(networkLSPManager.getDomainTEDB(), networkLSPManager, stats);
		    VNTMSession = createVNTMSession(lspDispatcher, networkLSPManager);
		}
		else if (testerParams.isVNTMSession()){
			VNTMSession = createVNTMSession();    
		}
			
		else if (testerParams.isNetworkEmulator()){
			networkLSPManager = createNetworkLSPManager();
		}
		Emulator emulator = null;
		if (testerParams.isNetworkEmulator() && testerParams.isVNTMSession()){			
			emulator = new Emulator( testerParams, PCEsessionList,networkLSPManager, VNTMSession, stats, lspDispatcher);
		}
		else{
			if (PCEsessionList.isEmpty()){
				log.info("PCE Session list is empty!");
				System.exit(-1);
			}
			log.info("Create the emulator!");
			emulator= new Emulator( testerParams, PCEsessionList,networkLSPManager, VNTMSession, stats);
		}
		emulator.start();
//		else{
//		//Abro una sesion con el VNTM
//		PCEsessionVNTM = new PCCPCEPSession(testerParams.getIpVNTM(), 4000,false);
//		 PCEsessionVNTM.start();	
//		}
	}
	
	 /**
	  * Create a Simple, advanced or complicated NetworkLSPManager 
	  * @param networkEmulatorParams 
	  * @return
	  */
	static NetworkLSPManager createNetworkLSPManager(){
		LinkedBlockingQueue<OSPFv2LinkStateUpdatePacket>  sendingQueue=null;
		NetworkLSPManagerParameters networkEmulatorParams= new NetworkLSPManagerParameters();
		networkEmulatorParams.initialize(networkEmulatorFile);
		NetworkLSPManager networkLSPManager=null;
		
		if (networkEmulatorParams.isOSPF_RAW_SOCKET()){
			OSPFSender ospfsender = new OSPFSender( networkEmulatorParams.getPCETEDBAddressList(), networkEmulatorParams.getAddress());
			ospfsender.start();	
			sendingQueue=ospfsender.getSendingQueue();
		}
		else {
			TCPOSPFSender TCPOSPFsender = new TCPOSPFSender(networkEmulatorParams.getPCETEDBAddressList(),networkEmulatorParams.getOSPF_TCP_PORTList());
			TCPOSPFsender.start();
			sendingQueue=TCPOSPFsender.getSendingQueue();
		}

		if (networkEmulatorParams.getNetworkLSPtype().equals("Simple")){
			networkLSPManager = new SimpleEmulatedNetworkLSPManager(sendingQueue, networkEmulatorParams.getNetworkFile() );
			
		} else if (networkEmulatorParams.getNetworkLSPtype().equals("Advanced")){
			networkLSPManager= new AdvancedEmulatedNetworkLSPManager(sendingQueue, networkEmulatorParams.getNetworkFile() );
		}
		else if (networkEmulatorParams.getNetworkLSPtype().equals("Completed")){
			networkLSPManager = new CompletedEmulatedNetworkLSPManager(sendingQueue, networkEmulatorParams.getNetworkFile(), null,networkEmulatorParams.isMultilayer());
		}
		else if (networkEmulatorParams.getNetworkLSPtype().equals("dummy")){
			networkLSPManager = new DummyEmulatedNetworkLSPManager();
		}
		networkLSPManager.setMultilayer(networkEmulatorParams.isMultilayer());
		return networkLSPManager;
	}
	
	  /**
	  * Create the VNTM Session 
	  * @param networkEmulatorParams 
	  * @return
	  */
	static PCCPCEPSession createVNTMSession(){
		VNTMParameters VNTMParams = new VNTMParameters();
		VNTMParams.initialize(testerParams.getVNTMFile());
		
		PCEPSessionsInformation VNTMpcepSessionManager=new PCEPSessionsInformation();
		PCCPCEPSession PCEsessionVNTM = new PCCPCEPSession(VNTMParams.getVNTMAddress(),VNTMParams.getVNTMPort(),false,VNTMpcepSessionManager);
		PCEsessionVNTM.start();
		return PCEsessionVNTM;
	}
	
	 /**
	  * Create the VNTM Session 
	  * @param networkEmulatorParams 
	  * @return
	  */
	static PCCPCEPSession createVNTMSession(LSPConfirmationDispatcher lspDispatcher, 
			NetworkLSPManager networkLSPManager){
		/*************************************/
		
		//Hastable para los LSPs IDs
		
		/*************************************/
		
		VNTMParameters VNTMParams = new VNTMParameters();
		VNTMParams.initialize(testerParams.getVNTMFile());
		PCEPSessionsInformation VNTMpcepSessionManager=new PCEPSessionsInformation();
		PCCPCEPSession PCEsessionVNTM = null;
		if (lspDispatcher!=null){
			PCEsessionVNTM = new PCCPCEPSession(VNTMParams.getVNTMAddress(),VNTMParams.getVNTMPort(),false, lspDispatcher, VNTMpcepSessionManager);
		}else{
			PCEsessionVNTM = new PCCPCEPSession(VNTMParams.getVNTMAddress(),VNTMParams.getVNTMPort(),false/*,lspDispatcher, networkLSPManager*/,VNTMpcepSessionManager);
		}		
		PCEsessionVNTM.start();
		return PCEsessionVNTM;
	}

}
