package tid.topology.topologymodule.session;



import java.io.IOException;
import java.net.ServerSocket;
import java.util.logging.Logger;

import tid.topology.topologymodule.InformationRetriever;
import tid.topology.topologymodule.TopologyUpdater;



/**
 * This is the class in charge of maintain the session between the web service and the Topology Module 
 * 
 * @author Telefonica I+D
 *
 */
public class TopologyModuleSessionServer extends Thread {
	/**
	 * Port the topology module is listenning to receive messages from web service 
	 */
	private int topologyModulePort;
	/**
	 * Topology Module logger
	 */
	private Logger log;
	/**
	 * Information retriever class 
	 */
	private InformationRetriever informationRetriever;
	/**
	 * Topology updater class
	 */
	private TopologyUpdater topologyUpdater;
	/**
	 * Constructor It receives the instances of informationRetriever and topologyUpdater
	 */
	public TopologyModuleSessionServer(InformationRetriever informationRetriever,TopologyUpdater topologyUpdater,/*LinkedBlockingQueue<byte[]> topologyModulePacketQueue,*/int topologyModulePort){
		log=Logger.getLogger("topologyModuleParser");
		this.topologyModulePort=topologyModulePort;
		this.informationRetriever=informationRetriever;
		this.topologyUpdater=topologyUpdater;
	}
	/**
	 * Run checks if the topology session is oppened
	 */
	public void run(){
		ServerSocket serverSocket = null;
		boolean listening=true;
		try {
			log.info("Listening on port: "+topologyModulePort);	
			serverSocket = new ServerSocket(topologyModulePort);			
		} catch (IOException e) {
			System.err.println("Could not listen on port: "+topologyModulePort);
			System.exit(-1);
		}

		try {
			while (listening) {
				new TopologyModuleSession(serverSocket.accept(),informationRetriever,topologyUpdater).start();
				
			}
			System.out.println("Se cierra");
			serverSocket.close();

		} catch (Exception e) {

			e.printStackTrace();

		}

	}

	public int getTopologyModulePort() {
		return topologyModulePort;
	}

	public void setTopologyModulePort(int topologyModulePort) {
		this.topologyModulePort = topologyModulePort;
	}

}
