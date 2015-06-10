package tid.vntm;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

import es.tid.ospf.ospfv2.OSPFv2LinkStateUpdatePacket;
import tid.netManager.NetworkLSPManager;
import tid.netManager.NetworkLSPManagerParameters;
import tid.netManager.OSPFSender;
import tid.netManager.TCPOSPFSender;
import tid.netManager.emulated.AdvancedEmulatedNetworkLSPManager;
import tid.netManager.emulated.CompletedEmulatedNetworkLSPManager;
import tid.netManager.emulated.DummyEmulatedNetworkLSPManager;
import tid.netManager.emulated.EmulatedUniNetworkLSPManager;
import tid.netManager.emulated.SimpleEmulatedNetworkLSPManager;
import tid.netManager.uni.UniNetworkLSPManager;
import tid.pce.pcepsession.PCEPSessionsInformation;
//import tid.vntm.emulator.OSPFSender;
//import tid.vntm.emulator.TCPOSPFSender;
import tid.vntm.management.VNTMManagementSever;
import tid.vntm.topology.VNTMGraph;


/**
 * Virtual Network Topology Manager.
 * @author ogondio, 
 */
public class VNTMServer {

	static LSPManager lspManager;
	
	/**
	 * Logger
	 */
	public static final Logger Log =Logger.getLogger("VNTMServer");
	
	static String networkEmulatorFile="NetworkEmulatorConfiguration.xml";
	static String VNTMFile = "VNTMConfiguration.xml";
	private static VNTMParameters VNTMParams = new VNTMParameters();
	private static HashMap<Long,OpTable> oPtable=new HashMap<Long,OpTable>();

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		/*
		//Initialize loggers
		FileHandler fh;
		FileHandler fh2;
		try {
			fh=new FileHandler("VNTMServer.log");
			fh2=new FileHandler("VNTMPCEPParserServer.log");
			fh.setFormatter(new SimpleFormatter());
			fh2.setFormatter(new SimpleFormatter());
			Log.addHandler(fh);
			Log.setLevel(Level.SEVERE);
			Logger log2=Logger.getLogger("PCEPParser");
			log2.addHandler(fh2);
			log2.setLevel(Level.SEVERE);
		} catch (Exception e1) {
			e1.printStackTrace();
			System.exit(1);
		}
		*/
		
	
		/*Load parameters*/
		if (args.length >=1 ){
			VNTMParams.initialize(args[0]);	
		}else{
			VNTMParams.initialize(VNTMFile);
		}
	
//		LinkedBlockingQueue<LSA> sendingQueue;
//		if (NetworkEmulatorParams.isOSPF_RAW_SOCKET()){
//			OSPFSender ospfsender = new OSPFSender(NetworkEmulatorParams.getPCETEDBAddress());
//			ospfsender.start();	
//			sendingQueue=ospfsender.getSendingQueue();
//		}else {
//			TCPOSPFSender TCPOSPFsender = new TCPOSPFSender(NetworkEmulatorParams.getPCETEDBAddress(),NetworkEmulatorParams.getOSPF_TCP_PORT());
//			TCPOSPFsender.start();
//			sendingQueue=TCPOSPFsender.getSendingQueue();
//		}
			
//		// TODO Auto-generated method stub
//		SimpleEmulatedNetworkLSPManager net=null;
//		try {
//			net = new SimpleEmulatedNetworkLSPManager(sendingQueue,NetworkEmulatorParams.getNetworkFile());	
//		}catch(Exception e) {
//			e.printStackTrace();
//		}
		VNTMGraph vntmGraph =new VNTMGraph(); 
		vntmGraph.readNetwork(VNTMParams.getInterLayerFile());
		System.out.println(vntmGraph.toString());
		
		
		//NetworkLSPManager networkLSPManager = createNetworkLSPManager();	
		lspManager = new LSPManager();
		//lspManager.setNetLSPManager(networkLSPManager);
		Log.info(lspManager.toString());

		
		VNTMManagementSever vntmmanagementSever= new VNTMManagementSever(lspManager,VNTMParams.getVNTMManagementPort());
		vntmmanagementSever.start();
		
		ServerSocket serverSocket = null;
		boolean listening = true;

  	  System.out.println("Puerto: "+VNTMParams.getVNTMPort());
		try {
			Log.info("Listening on port: "+VNTMParams.getVNTMPort());	
			serverSocket = new ServerSocket(VNTMParams.getVNTMPort());
		} catch (IOException e) {
			System.err.println("Could not listen on port: "+VNTMParams.getVNTMPort());
			System.exit(-1);
		}

		try {
			while (listening) {
				//new PCESession(serverSocket.accept(), params, PCCRequestsQueue,ted,pcm.getChildPCERequestManager()).start();
				//null,ted,pcm.getChildPCERequestManager()).start(
				Log.info("LLamamos a la sesion");
				
				Socket s=serverSocket.accept();
				new VNTMSession(s,lspManager, new PCEPSessionsInformation(), vntmGraph, VNTMParams, oPtable).start();
				
			}
			serverSocket.close();
		} catch (Exception e) {
			e.printStackTrace();
		}						
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

		if (networkEmulatorParams.getNetworkLSPtype().equals("Simple")){
			sendingQueue=createSender(networkEmulatorParams);
			networkLSPManager = new SimpleEmulatedNetworkLSPManager(sendingQueue, networkEmulatorParams.getNetworkFile() );
			
		} else if (networkEmulatorParams.getNetworkLSPtype().equals("Advanced")){
			sendingQueue=createSender(networkEmulatorParams);
			networkLSPManager= new AdvancedEmulatedNetworkLSPManager(sendingQueue, networkEmulatorParams.getNetworkFile() );
		}
		else if (networkEmulatorParams.getNetworkLSPtype().equals("Completed")){
			sendingQueue=createSender(networkEmulatorParams);
			networkLSPManager = new CompletedEmulatedNetworkLSPManager(sendingQueue, networkEmulatorParams.getNetworkFile(), null, networkEmulatorParams.isMultilayer());
			
		}
		else if (networkEmulatorParams.getNetworkLSPtype().equals("dummy")){
			networkLSPManager = new DummyEmulatedNetworkLSPManager();
		}
		else if  (networkEmulatorParams.getNetworkLSPtype().equals("Uni")){
			networkLSPManager = new UniNetworkLSPManager();
		}
		else if  (networkEmulatorParams.getNetworkLSPtype().equals("UniEmulated")){			
			networkLSPManager = new EmulatedUniNetworkLSPManager();
		}
		networkLSPManager.setMultilayer(networkEmulatorParams.isMultilayer());
		return networkLSPManager;
	}
	
	static LinkedBlockingQueue<OSPFv2LinkStateUpdatePacket> createSender(NetworkLSPManagerParameters networkEmulatorParams){
		LinkedBlockingQueue<OSPFv2LinkStateUpdatePacket>  sendingQueue=null;
		if (networkEmulatorParams.isOSPF_RAW_SOCKET()){
			OSPFSender ospfsender = new OSPFSender( networkEmulatorParams.getPCETEDBAddressList() , networkEmulatorParams.getAddress());
			ospfsender.start();	
			sendingQueue=ospfsender.getSendingQueue();
		}
		else {
			TCPOSPFSender TCPOSPFsender = new TCPOSPFSender(networkEmulatorParams.getPCETEDBAddressList(),networkEmulatorParams.getOSPF_TCP_PORTList());
			TCPOSPFsender.start();
			sendingQueue=TCPOSPFsender.getSendingQueue();
		}
		return sendingQueue;

	}
	

}
