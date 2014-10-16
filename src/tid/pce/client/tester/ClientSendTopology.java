package tid.pce.client.tester;

import java.util.Timer;
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
import tid.netManager.emulated.SimpleEmulatedNetworkLSPManager;
import tid.pce.client.PCCPCEPSession;

/**
 * Cada 30 segundos, lanza una tarea que envia OSPF's de toda la red
 * @author mcs
 *
 */
public class ClientSendTopology {
	private static PCCPCEPSession PCEsession;
	private static Logger log=Logger.getLogger("PCCClient");
	private static Logger log2=Logger.getLogger("PCEPParser");
	private static String networkEmulatorFile="NetworkEmulatorConfiguration.xml";
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		//OSPFSender
		//Create a new Task which send the topology
		/*If there are arguments, read the PCEServerPort and ipPCE*/
		  if (args.length < 1) {
			log.info("Usage: ClientTester <XMLFile>");
			return;
		}
		  
		FileHandler fh;
		FileHandler fh2;
		try {
			fh=new FileHandler("PCCClient.log");
			fh2=new FileHandler("PCEPClientParser.log");
			//fh.setFormatter(new SimpleFormatter());
			
			log.addHandler(fh);
			log.setLevel(Level.ALL);
			log2.addHandler(fh2);
			log2.setLevel(Level.ALL);
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			System.exit(1);
		}
		
		ClientSendTopologyConfiguration clientConf = new ClientSendTopologyConfiguration(args[0]);
		
		//PCEsession = new PCCPCEPSession(clientConf.getIpPCE(), clientConf.getPCEServerPort());
		//PCEsession.start();
		LinkedBlockingQueue<OSPFv2LinkStateUpdatePacket>  sendingQueue = null;
		NetworkLSPManagerParameters networkEmulatorParams= new NetworkLSPManagerParameters();
		networkEmulatorParams.initialize(networkEmulatorFile);
		
		
		
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
				
		NetworkLSPManager networkLSPManager= createNetworkLSPManager(networkEmulatorParams,sendingQueue);
		if (networkLSPManager==null){
			log.info("ERROR: You should write the network type you want: Simple, Advanced, Completed");
			return;
		}
		
		Timer timer=new Timer();
		SendTopologyClientTask updateTask = new SendTopologyClientTask(networkLSPManager);
		timer.schedule(updateTask, 0, clientConf.getTime_ms());

	}
	
	 /**
	  * Create a Simple, advanced or complicated NetworkLSPManager 
	  * @param networkEmulatorParams 
	  * @return
	  */
	static NetworkLSPManager createNetworkLSPManager(NetworkLSPManagerParameters networkEmulatorParams,LinkedBlockingQueue<OSPFv2LinkStateUpdatePacket> sendingQueue){
		NetworkLSPManager networkLSPManager=null;
		
		if (networkEmulatorParams.getNetworkLSPtype().equals("Simple")){
			networkLSPManager = new SimpleEmulatedNetworkLSPManager(sendingQueue, networkEmulatorParams.getNetworkFile() );
			
		} else if (networkEmulatorParams.getNetworkLSPtype().equals("Advanced")){
			networkLSPManager= new AdvancedEmulatedNetworkLSPManager(sendingQueue, networkEmulatorParams.getNetworkFile() );
		}
		else if (networkEmulatorParams.getNetworkLSPtype().equals("Completed")){
			networkLSPManager = new CompletedEmulatedNetworkLSPManager(sendingQueue, networkEmulatorParams.getNetworkFile(),null,networkEmulatorParams.isMultilayer());
		}
		return networkLSPManager;
	}

}
