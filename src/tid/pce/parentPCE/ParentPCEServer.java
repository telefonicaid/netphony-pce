package tid.pce.parentPCE;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import tid.bgp.bgp4Peer.pruebas.BGPPeer;
import tid.log.StrongestGUIFormatter;
import tid.log.StrongestGUIHandler;
import tid.pce.computingEngine.RequestDispatcher;
import tid.pce.computingEngine.algorithms.ComputingAlgorithmManager;
import tid.pce.computingEngine.algorithms.LocalChildRequestManager;
import tid.pce.computingEngine.algorithms.ParentPCEComputingAlgorithmManager;
import tid.pce.parentPCE.management.ParentPCEManagementSever;
import tid.pce.pcepsession.PCEPSessionsInformation;
import tid.pce.tedb.FileTEDBUpdater;
import tid.pce.tedb.ITMDTEDB;
import tid.pce.tedb.MDTEDB;
import tid.pce.tedb.MultiDomainTEDB;
import tid.pce.tedb.SimpleITTEDB;
import tid.pce.tedb.SimpleTEDB;
import tid.pce.tedb.TEDB;

/**
 * Parent PCE Server
 * @author ogondio
 *
 */
public class ParentPCEServer {

	/**
	 * Main class
	 * @param args
	 */
	public static void main(String[] args) {
		//First, get the parameters from the configuration file
		ParentPCEServerParameters params=new ParentPCEServerParameters();
		params.initialize();
		//Initiate the Loggers (general, PCEP Parsing, OSPF Parsing, GUI)		
		FileHandler fh;
		FileHandler fh2,fh3,fh4;
		Logger log;
		Logger log2,log3,log4;
		log=Logger.getLogger("PCEServer");
		log2=Logger.getLogger("PCEPParser");
		log3=Logger.getLogger("OSPFParser");
		log4=Logger.getLogger("MultiDomainTologyUpdater");
		PCEPSessionsInformation pcepSessionManager=new PCEPSessionsInformation();
		
		try {
			fh=new FileHandler(params.getParentPCEServerLogFile());
			fh2=new FileHandler(params.getParentPCEPParserLogFile());
			fh3=new FileHandler("OSPFParser.log");
			fh4=new FileHandler("MultiDomainTologyUpdater.log");
			SimpleFormatter sf4=new SimpleFormatter();
			log.addHandler(fh);
			log.setLevel(Level.ALL);			
			log2.addHandler(fh2);
			log2.setLevel(Level.ALL);
			log3.addHandler(fh3);
			log3.setLevel(Level.ALL);
			log4.addHandler(fh4);
			log4.setLevel(Level.ALL);
			fh4.setFormatter(sf4);
			if (params.isStrongestLog()){
				Logger logGUI=Logger.getLogger("GUILogger");
				log.info("Adding GUI Logger");
				StrongestGUIFormatter sgf=new StrongestGUIFormatter();
				sgf.setHost(params.getGUIHost());
				log.info("RemoteHost: "+sgf.getHost());
				sgf.setLocalAddress(params.getParentPCEServerAddress());
				log.info("Localhost: "+sgf.getLocalAddress());
				StrongestGUIHandler sgh=new StrongestGUIHandler();				
				sgh.setHost(params.getGUIHost());
				log.info("RemoteHost: "+sgh.getHost());
				sgh.setPort(params.getGUIPort());
				log.info("RemotePort: "+sgh.getPort());
				logGUI.addHandler(sgh);
				sgh.setFormatter(sgf);				
				logGUI.setLevel(Level.ALL);				
			}else {
				Logger logGUI=Logger.getLogger("GUILogger");
				logGUI.setLevel(Level.SEVERE);
			}
			
		} catch (Exception e1) {
			e1.printStackTrace();
			System.exit(-1);
		}
		log.info("Inizializing TID ParentPCE Server");
		//Create the Elements of the Parent PCE Server
			
		//The Traffic Engineering Database (TED)
		TEDB ted;//TEDB is generic, depending on the type of PCE, it will get a different specific TEDB
		TEDB simple_ted = null;
		LocalChildRequestManager localChildRequestManager=null;
		if(!params.isITCapable()){			
			if ((params.isMultiDomain())&&(!params.isKnowsWholeTopology())){
				//The PCE is multidomain
				log.info("The PCE is multidomain");
				ted=new MDTEDB();
				if (params.isReadMDTEDFromFile()){
					ted.initializeFromFile(params.getMDnetworkDescriptionFile());
					((MDTEDB)ted).initializeFullTEDFromFile(params.getNetworkDescriptionFile() );

				}
			}else if (params.isKnowsWholeTopology()){
				log.info("The PCE knows the whole topology. Interdomain and intradomain");
				ted=new MDTEDB();
				if (params.isReadMDTEDFromFile()){
					ted.initializeFromFile(params.getMDnetworkDescriptionFile());
					((MDTEDB)ted).initializeFullTEDFromFile(params.getNetworkDescriptionFile() );

				}
				simple_ted=new SimpleTEDB();
				((SimpleTEDB)simple_ted).createGraph();
				localChildRequestManager = new LocalChildRequestManager();
				log.info("Adding Intradomain topologies to multidomain tedb");
				
				SimpleTEDB intraDomainTopologies = (SimpleTEDB)simple_ted;
				((MDTEDB)ted).setSimple_ted(intraDomainTopologies);
				
				log.info("Intradomain topologies added to multidomain tedb");
				
			}
			else{
				log.info("The PCE is single domain");
				ted=new SimpleTEDB();
				ted.initializeFromFile(params.getNetworkDescriptionFile());
				
			}
			//Read the database from a file
			MultiDomainTopologyUpdater mdt= null;
	
			if (params.isActingAsBGP4Peer()) {//BGP
				log.info("Acting as BBGP peer");
				BGPPeer bgpPeer = new BGPPeer();		
				bgpPeer.configure(params.getBGP4File());
				if (params.isMultiDomain())
					bgpPeer.setWriteMultiTEDB((MultiDomainTEDB)ted);				
				if (params.isKnowsWholeTopology())
					bgpPeer.setSimpleTEDB((SimpleTEDB)simple_ted);
				bgpPeer.createUpdateDispatcher();
				bgpPeer.startClient();		
				bgpPeer.startServer();
				bgpPeer.startManagementServer();
				bgpPeer.startSendTopology();
				
			}else{
				//Create the multidomain topology updater
				mdt=new MultiDomainTopologyUpdater((MDTEDB)ted,params.isActingAsBGP4Peer());
				mdt.initialize();
			}
			ReachabilityManager rm=new ReachabilityManager();
			
			if (params.isMultiDomain()){
				if (params.isReadMDTEDFromFile()){
					FileTEDBUpdater.initializeReachabilityFromFile(params.getMDnetworkDescriptionFile(), rm);
				}
			}
			
			ChildPCERequestManager childPCERequestManager = new ChildPCERequestManager();
			
			//The Request Dispatcher. Incoming Requests are sent here
			//RequestQueue pathRequestsQueue;
			RequestDispatcher requestDispatcher;
			log.info("Inizializing "+ params.getChildPCERequestsProcessors()+" Path Request Processor Threads");
			//pathRequestsQueue=new RequestQueue(params.getChildPCERequestsProcessors());
			requestDispatcher=new  RequestDispatcher(params.getChildPCERequestsProcessors(),ted,null,false);
			for (int i=0;i<params.algorithmRuleList.size();++i){
				 try {
					Class aClass = Class.forName("tid.pce.computingEngine.algorithms."+params.algorithmRuleList.get(i).algoName+"Manager");
		        	log.info("Registering algorithm "+ params.algorithmRuleList.get(i).algoName+" for of = "+params.algorithmRuleList.get(i).ar.of+" and svec = "+params.algorithmRuleList.get(i).ar.svec);            
					
					if (params.algorithmRuleList.get(i).isParentPCEAlgorithm==false){
						ComputingAlgorithmManager cam= ( ComputingAlgorithmManager)aClass.newInstance();
						requestDispatcher.registerAlgorithm(params.algorithmRuleList.get(i).ar, cam);
					}
					else {
						ParentPCEComputingAlgorithmManager cam= ( ParentPCEComputingAlgorithmManager)aClass.newInstance();
						cam.setChildPCERequestManager(childPCERequestManager);
						if (params.isKnowsWholeTopology()){
							log.info("Introducimos el localChildRequestManager en la cam");
							cam.setLocalChildRequestManager(localChildRequestManager);
						}
						cam.setReachabilityManager(rm);
						requestDispatcher.registerAlgorithm(params.algorithmRuleList.get(i).ar, cam);
					}
					
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
			
			

			log.info("Initializing Management Server");
			ParentPCEManagementSever pms=new ParentPCEManagementSever(childPCERequestManager,requestDispatcher,(MDTEDB)ted,(SimpleTEDB)simple_ted,rm,pcepSessionManager,mdt,params.getParentPCEManagementPort());		
			pms.start();
			
			//
			//System.setSecurityManager(new ParentPCESessionsControler());		
	        ServerSocket serverSocket = null;
	        boolean listening = true;
	        try {
	        	log.info("Listening on port: "+params.getParentPCEServerPort());	
	            serverSocket = new ServerSocket(params.getParentPCEServerPort());
	            //If you want to reuse the address:
	        	//InetSocketAddress local =new InetSocketAddress(Inet4Address.getByName("10.95.162.97"),params.getParentPCEServerPort() );
	        	//InetSocketAddress local =new InetSocketAddress((Inet4Address)null,params.getParentPCEServerPort() );
	        	//serverSocket = new ServerSocket();
	        	//serverSocket.setReuseAddress(true);
	        	//serverSocket.bind(local);        	
	        } catch (IOException e) {
	            System.err.println("Could not listen on port: "+params.getParentPCEServerPort());
	            System.exit(-1);
	        }

	        try {
	        	pcepSessionManager.setStateful(false);
	        	while (listening) {
	        		new ParentPCESession(serverSocket.accept(),params, requestDispatcher,ted,mdt,childPCERequestManager,rm,pcepSessionManager).start();
	        	}
	        	serverSocket.close();
	        } catch (Exception e) {
	        	e.printStackTrace();
	        }						

			
			
				
		}else{
			log.info("The PCE is IT capable");
			
			
			if (params.isMultiDomain()){
				//The PCE is multidomain
				log.info("The PCE is multidomain");
				ted=new ITMDTEDB();
				((ITMDTEDB)ted).initializeFullTEDFromFile(params.getITNetworkDescriptionFile() );
				if (params.isReadMDTEDFromFile()){
					((ITMDTEDB)ted).initializeFromFile(params.getITMDnetworkDescriptionFile());
				}
			}else {
				log.info("The PCE is single domain");
				ted=new SimpleITTEDB();
				ted.initializeFromFile(params.getITNetworkDescriptionFile());
			}
			//Read the database from a file
			
			//Create the multidomain topology updater
			MultiDomainTopologyUpdater mdt=new MultiDomainTopologyUpdater((ITMDTEDB)ted);
			mdt.ITinitialize();
			
			ReachabilityManager rm=new ReachabilityManager();
			
			if (params.isMultiDomain()){
				if (params.isReadMDTEDFromFile()){
					FileTEDBUpdater.initializeReachabilityFromFile(params.getITMDnetworkDescriptionFile(), rm);
				}
			}
			
			ChildPCERequestManager childPCERequestManager = new ChildPCERequestManager();
			
			//The Request Dispatcher. Incoming Requests are sent here
			//RequestQueue pathRequestsQueue;
			RequestDispatcher requestDispatcher;
			log.info("Inizializing "+ params.getChildPCERequestsProcessors()+" Path Request Processor Threads");
			//pathRequestsQueue=new RequestQueue(params.getChildPCERequestsProcessors());
			requestDispatcher=new  RequestDispatcher(params.getChildPCERequestsProcessors(),ted,null,false);
			for (int i=0;i<params.algorithmRuleList.size();++i){
				 try {
					Class aClass = Class.forName("tid.pce.computingEngine.algorithms."+params.algorithmRuleList.get(i).algoName+"Manager");
		        	log.info("Registering algorithm "+ params.algorithmRuleList.get(i).algoName+" for of = "+params.algorithmRuleList.get(i).ar.of+" and svec = "+params.algorithmRuleList.get(i).ar.svec);            
					
					if (params.algorithmRuleList.get(i).isParentPCEAlgorithm==false){
						ComputingAlgorithmManager cam= ( ComputingAlgorithmManager)aClass.newInstance();
						requestDispatcher.registerAlgorithm(params.algorithmRuleList.get(i).ar, cam);
					}
					else {
						ParentPCEComputingAlgorithmManager cam= ( ParentPCEComputingAlgorithmManager)aClass.newInstance();
						cam.setChildPCERequestManager(childPCERequestManager);
						cam.setReachabilityManager(rm);
						requestDispatcher.registerAlgorithm(params.algorithmRuleList.get(i).ar, cam);
					}
					
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
			

			log.info("Initializing Management Server");
			ParentPCEManagementSever pms=new ParentPCEManagementSever(childPCERequestManager,requestDispatcher,(ITMDTEDB)ted,rm,pcepSessionManager,mdt,params.getParentPCEManagementPort());		
			pms.start();
			
			//
			//System.setSecurityManager(new ParentPCESessionsControler());		
	        ServerSocket serverSocket = null;
	        boolean listening = true;
	        try {
	        	log.info("Listening on port: "+params.getParentPCEServerPort());	
	            serverSocket = new ServerSocket(params.getParentPCEServerPort());
	            //If you want to reuse the address:
	        	//InetSocketAddress local =new InetSocketAddress(Inet4Address.getByName("10.95.162.97"),params.getParentPCEServerPort() );
	        	//InetSocketAddress local =new InetSocketAddress((Inet4Address)null,params.getParentPCEServerPort() );
	        	//serverSocket = new ServerSocket();
	        	//serverSocket.setReuseAddress(true);
	        	//serverSocket.bind(local);        	
	        } catch (IOException e) {
	            System.err.println("Could not listen on port: "+params.getParentPCEServerPort());
	            System.exit(-1);
	        }

	        try {
	        	pcepSessionManager.setStateful(true);
	        	while (listening) {
	        		new ParentPCESession(serverSocket.accept(),params, requestDispatcher,(ITMDTEDB)ted,mdt,childPCERequestManager,rm,pcepSessionManager).start();
	        	}
	        	serverSocket.close();
	        } catch (Exception e) {
	        	e.printStackTrace();
	        }
		}		
	}
}



