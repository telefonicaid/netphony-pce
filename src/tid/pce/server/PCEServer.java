package tid.pce.server;

/** 
 * PCE Domain Server.
 * 
 * It is the main class of a PCE that is responsible of a domain.
 * 
 * By default listens on port 4189
 * 
 * @author Oscar, Eduardo
 */

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.Timer;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import tid.bgp.bgp4Peer.pruebas.BGPPeer;
import tid.pce.computingEngine.ReportDispatcher;
import tid.pce.computingEngine.RequestDispatcher;
import tid.pce.computingEngine.algorithms.ComputingAlgorithmManager;
import tid.pce.computingEngine.algorithms.ComputingAlgorithmManagerSSON;
import tid.pce.computingEngine.algorithms.ComputingAlgorithmPreComputation;
import tid.pce.computingEngine.algorithms.ComputingAlgorithmPreComputationSSON;
import tid.pce.computingEngine.algorithms.multiLayer.OperationsCounter;
import tid.pce.pcepsession.PCEPSessionsInformation;
import tid.pce.server.communicationpce.BackupSessionManagerTask;
import tid.pce.server.communicationpce.CollaborationPCESessionManager;
import tid.pce.server.lspdb.LSP_DB;
import tid.pce.server.lspdb.SimpleLSP_DB;
import tid.pce.server.management.PCEManagementSever;
import tid.pce.server.wson.ReservationManager;
import tid.pce.tedb.DomainTEDB;
import tid.pce.tedb.MultiLayerTEDB;
import tid.pce.tedb.SimpleITTEDB;
import tid.pce.tedb.SimpleTEDB;
import tid.provisioningManager.modules.PMController;


public class PCEServer {
 
	/**
	 * Logger
	 */
	public static final Logger Log =Logger.getLogger("PCEServer");
	public static Logger log5;
	private static OperationsCounter OPcounter;
	
	private static LSP_DB lspDB;
	
	/**
	 * LSP database. It should only be necessary if PCE is stateful
	 * 
	 */

	/**
	 * Main class
	 * @param args
	 */
	public static void main(String[] args) {
		
		//First of all, read the parameters
		PCEServerParameters params;
		if (args.length >=1 ){
			params=new PCEServerParameters(args[0]);
		}else{
			params=new PCEServerParameters();
		}
		params.initialize();

		//Initialize loggers
		FileHandler fh;
		FileHandler fh2;
		FileHandler fh3;
		FileHandler fh4;
		FileHandler fh5;
		//FileHandler fh6;
		try {
			fh=new FileHandler(params.getPCEServerLogFile());
			fh2=new FileHandler(params.getPCEPParserLogFile());
			fh3=new FileHandler(params.getOSPFParserLogFile());
			fh4=new FileHandler(params.getTEDBParserLogFile());
			fh5=new FileHandler("OpMultiLayer.log", false);
			fh5.setFormatter(new SimpleFormatter());
			//fh.setFormatter(new SimpleFormatter());
			//fh2.setFormatter(new SimpleFormatter());			
			Logger log2=Logger.getLogger("PCEPParser");
			Logger log3=Logger.getLogger("OSPFParser");
			Logger log4=Logger.getLogger("TEDBParser");
			//Logger log6=Logger.getLogger("BGPParser");
			log5=Logger.getLogger("OpMultiLayer");
			Log.addHandler(fh);
			log2.addHandler(fh2);
			log3.addHandler(fh3);
			log4.addHandler(fh4);
			//logTimePCE.addHandler(fh5);
			fh4.setFormatter(new SimpleFormatter());
			log5.addHandler(fh5);
			log5.setLevel(Level.ALL);
			if (params.isSetTraces() == false){		    	
				Log.setLevel(Level.SEVERE);
				log2.setLevel(Level.SEVERE);	
				log3.setLevel(Level.SEVERE);
				log4.setLevel(Level.SEVERE);
				//log6.setLevel(Level.SEVERE);
				//log5.setLevel(Level.SEVERE);
			}
			
			else{
				Log.setLevel(Level.ALL);
				log2.setLevel(Level.ALL);
				log3.setLevel(Level.ALL);
				log4.setLevel(Level.ALL);
				//log6.setLevel(Level.ALL);
			}

		} catch (Exception e1) {
			e1.printStackTrace();
			System.exit(1);
		}

		Log.info("Inizializing TID PCE Server");
		//Elements of the PCE Server

		// Information about all the sessions of the PCE
		PCEPSessionsInformation pcepSessionsInformation = new PCEPSessionsInformation();
		pcepSessionsInformation.setStateful(params.isStateful());
		pcepSessionsInformation.setActive(params.isActive());
		pcepSessionsInformation.setSRCapable(params.isSRCapable());
		pcepSessionsInformation.setMSD(params.getMSD());
		Log.info("PCEServer: PCE is SR capable with MSD="+pcepSessionsInformation.getMSD());
	
		//The Traffic Engineering Database
		DomainTEDB ted;
		if(params.ITcapable==true){
			//IT CAPABLE PCE
			Log.info("IT capable Domain PCE");
			ted=new SimpleITTEDB();
		}else{
			//GENERIC PCE
			Log.info("GENERIC PCE");
			if (params.isMultilayer()){
				ted=new MultiLayerTEDB();
				Log.info("is multilayer");
			}else{
				ted=new SimpleTEDB();
			}
		}
		
		/***/
		
		TopologyManager topologyManager = new TopologyManager(params, ted, Log);
		topologyManager.initTopology();
		
		OPcounter = new OperationsCounter();
		
		ChildPCESessionManager pcm=null;
		
		RequestDispatcher PCCRequestDispatcherChild = null;
		if (params.getParentPCEAddress()!=null){
			//ChildPCERequestDispatcherParentPCE childPCERequestDispatcherParentPCE = new ChildPCERequestDispatcherParentPCE();
			//For the session between the Domain (Child) PCE and the parent PCE
			Log.info("Initializing Manager of the ChildPCE - Parent PCE Session");	
			PCCRequestDispatcherChild=new  RequestDispatcher(1,ted,null,params.isAnalyzeRequestTime());
			pcm=new ChildPCESessionManager(PCCRequestDispatcherChild, params,ted,ted.getReachabilityEntry().getDomainId(),pcepSessionsInformation);
		}else {
			Log.info("There is no parent PCE");
		
		}
		
		//The Request Dispatcher, needed to dispatch the requests coming from the PCCs
		Log.info("Initializing Request Dispatcher");
		RequestDispatcher PCCRequestDispatcher;
		
		ReservationManager reservationManager=null;
		if (params.isReservation()){
			Log.info("Launching Reservation Manager");
			reservationManager= new ReservationManager(ted);//FIXME: he hecho el casting
		}
		CollaborationPCESessionManager	collaborationPCESessionManager=null;
		if ((params.getParentPCEAddress()!=null)){			
			if (params.isCollaborativePCEs()){//STRONGEST: Collaborative PCEs						
				collaborationPCESessionManager = new CollaborationPCESessionManager();			
				PCCRequestDispatcher=new  RequestDispatcher(params.getPCCRequestsProcessors(),ted,pcm.getChildPCERequestManager(),params.isAnalyzeRequestTime(),collaborationPCESessionManager);
			}
			else
				PCCRequestDispatcher=new  RequestDispatcher(params.getPCCRequestsProcessors(),ted,pcm.getChildPCERequestManager(),params.isAnalyzeRequestTime());
		}else {
			if (params.isMultilayer()== true){
				PCCRequestDispatcher=new  RequestDispatcher(params.getPCCRequestsProcessors(),ted,null,params.isAnalyzeRequestTime(),params.isUseMaxReqTime(), reservationManager, OPcounter, params.isMultilayer());
			}
			else if (params.isCollaborativePCEs()){//STRONGEST: Collabotarive PCEs	
				collaborationPCESessionManager = new CollaborationPCESessionManager();	
				PCCRequestDispatcher=new  RequestDispatcher(params.getPCCRequestsProcessors(),ted,null,params.isAnalyzeRequestTime(),params.isUseMaxReqTime(), reservationManager,collaborationPCESessionManager);
			}else
				PCCRequestDispatcher=new  RequestDispatcher(params.getPCCRequestsProcessors(),ted,null,params.isAnalyzeRequestTime(),params.isUseMaxReqTime(), reservationManager);
		}
		
		if(params.ITcapable==false){
			if (params.isMultilayer() == false)
			{
				((SimpleTEDB)ted).setRequestDispatcher(PCCRequestDispatcher);
			}
			
		}
		
		//The Reservation Manager,		
		NotificationDispatcher nd=new NotificationDispatcher(reservationManager);
		
		
		if(params.algorithmRuleList.size()==0){
			
		Log.warning("No hay algoritmos registrados!");
			System.exit(1);
			
		}
		//FIXME: cambiar esto de orden
		
		
		Timer timer=new Timer();
		if (params.getParentPCEAddress()!=null){
			Log.info("Inizializing Session with Parent PCE");
			timer.schedule(pcm, 0, 1000000);
		}	
		
		PCEManagementSever pms=new PCEManagementSever(PCCRequestDispatcher,ted,params, reservationManager,collaborationPCESessionManager);	
		pms.start();

		SendTopologyTask stg=null;
		ITSendTopologyTask ITstg=null;
		if (params.getParentPCEAddress()!=null){			
			if(params.ITcapable==true){
				ITstg=new ITSendTopologyTask((SimpleITTEDB)ted,pcm);

				Timer timer2=new Timer();
				if (params.getParentPCEAddress()!=null){
					Log.info("Changing topology");
					timer2.schedule(ITstg, 0, 100000);
				}
			}else if (!(params.isActingAsBGP4Peer())){
				stg=new SendTopologyTask((DomainTEDB)ted,pcm);

				Timer timer2=new Timer();
				if (params.getParentPCEAddress()!=null){
					Log.info("Changing topology");
					timer2.schedule(stg, 0, 100000);
				}
			}
		}

		SendReachabilityTask srt= new SendReachabilityTask(ted,pcm);
		Timer timer3=new Timer();
		if (params.getParentPCEAddress()!=null){
			timer3.schedule(srt, 0, params.getTimeSendReachabilityTime());
		}
		//STRONGEST: Collaborative PCEs
		if (params.isCollaborativePCEs()){				
			if (!(params.isPrimary())){	//If Backup PCE 	
				BackupSessionManagerTask backupSessionTask =null;	
				backupSessionTask= new BackupSessionManagerTask(params,ted,collaborationPCESessionManager,nd,pcepSessionsInformation);
				//backupSessionTask.manageBackupPCESession();
				Timer timerBackupSession=new Timer();
				Log.info("Inizializing Session with Primary PCE");
				timerBackupSession.schedule(backupSessionTask, 0, 100000);
			}		
		}
		else
			Log.info("There are no collaborative PCEs");
		
		ServerSocket serverSocket = null;
		boolean listening = true;
		try {
			Log.info("Listening on port: "+params.getPCEServerPort());
			
			// Local PCE address for multiple network interfaces in a single computer
			
			Log.info("Listening on address: "+params.getLocalPceAddress());
			serverSocket = new ServerSocket(params.getPCEServerPort(),0,(Inet4Address) InetAddress.getByName(params.getLocalPceAddress()));
		} catch (IOException e) {
			System.err.println("Could not listen on port: "+params.getPCEServerPort());
			System.exit(-1);
		}

		try {
			//FIXME: lo he cambiado aqu�
			//Registration of Algorithms
			for (int i=0;i<params.algorithmRuleList.size();++i){
				try {
					Class<?> aClass = Class.forName("tid.pce.computingEngine.algorithms."+params.algorithmRuleList.get(i).algoName+"Manager");
					Log.info("Registering algorithm "+ params.algorithmRuleList.get(i).algoName+" for of = "+params.algorithmRuleList.get(i).ar.of+" and svec = "+params.algorithmRuleList.get(i).ar.svec);            
					if (params.algorithmRuleList.get(i).isParentPCEAlgorithm==false){
						if(params.algorithmRuleList.get(i).isSSSONAlgorithm==false){
							ComputingAlgorithmManager cam= ( ComputingAlgorithmManager)aClass.newInstance();
							PCCRequestDispatcher.registerAlgorithm(params.algorithmRuleList.get(i).ar, cam);
							if ((params.getParentPCEAddress()!=null)){	
								PCCRequestDispatcherChild.registerAlgorithm(params.algorithmRuleList.get(i).ar, cam);
							}
							try{
									
								Class<?> aClass2 = Class.forName("tid.pce.computingEngine.algorithms."+params.algorithmRuleList.get(i).algoName+"PreComputation");
								ComputingAlgorithmPreComputation cam2= ( ComputingAlgorithmPreComputation)aClass2.newInstance();
								cam2.setTEDB(ted);
								cam2.initialize();
								cam.setPreComputation(cam2);
								((DomainTEDB) ted).register(cam2);
								cam.setReservationManager(reservationManager);
							}
							catch (Exception e2){
							e2.printStackTrace();
							Log.warning("No precomputation in "+"tid.pce.computingEngine.algorithms."+params.algorithmRuleList.get(i).algoName+"PreComputation");						
							}
						}
						else{
							ComputingAlgorithmManagerSSON cam_sson= (ComputingAlgorithmManagerSSON)aClass.newInstance();
							PCCRequestDispatcher.registerAlgorithmSSON(params.algorithmRuleList.get(i).ar, cam_sson);
							if ((params.getParentPCEAddress()!=null)){	
								PCCRequestDispatcherChild.registerAlgorithmSSON(params.algorithmRuleList.get(i).ar, cam_sson);

							}
							try{
								Class<?> aClass2 = Class.forName("tid.pce.computingEngine.algorithms."+params.algorithmRuleList.get(i).algoName+"PreComputation");
								ComputingAlgorithmPreComputationSSON cam3= ( ComputingAlgorithmPreComputationSSON)aClass2.newInstance();
								cam3.setTEDB(ted);
								cam3.initialize();
								cam_sson.setPreComputation(cam3);
								((DomainTEDB) ted).registerSSON(cam3);
								cam_sson.setReservationManager(reservationManager);
							}
							catch (Exception e2){
								e2.printStackTrace();
								Log.warning("No precomputation in "+"tid.pce.computingEngine.algorithms."+params.algorithmRuleList.get(i).algoName+"PreComputation");						
							}
						}
					}

					//No registro los algoritmos que sean de parentPCE
					} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InstantiationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			if ((params.getLambdaEnd()!=Integer.MAX_VALUE)&&(params.ITcapable==false)&&((params.isMultilayer())==false))
				((SimpleTEDB)ted).notifyAlgorithms( params.getLambdaIni(),params.getLambdaEnd());

			//pcepSessionsInformation.setStateful(true);
			//pcepSessionsInformation.setActive(true);
			
			lspDB = new SimpleLSP_DB();
			params.setLspDB(lspDB);
			
			// This parameter tells the dispatcher that sync will be avoided.
			// In better future times sync should be implemented
			
			ReportDispatcher PCCReportDispatcher = null;
			if (pcepSessionsInformation.isStateful())
			{
				PCCReportDispatcher = new ReportDispatcher(false, lspDB, 2);
			}
			
			/*
			(new Thread()
			{
				@Override
				public void run()
				{
					String []args = new String[0];
					PMController.main(args);
				}
				
			}).start();
			*/
			
			
			while (listening) {
				//new PCESession(serverSocket.accept(),params, PCCRequestsQueue,ted,pcm.getChildPCERequestManager()).start();
				//null,ted,pcm.getChildPCERequestManager()).start(
				if (params.isCollaborativePCEs())
					new DomainPCESession(serverSocket.accept(),params,PCCRequestDispatcher,ted,nd,reservationManager,collaborationPCESessionManager,pcepSessionsInformation,PCCReportDispatcher).start();
				else {
					new DomainPCESession(serverSocket.accept(),params,PCCRequestDispatcher,ted,nd,reservationManager,pcepSessionsInformation,PCCReportDispatcher).start();
				}
			}
			serverSocket.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

}