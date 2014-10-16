package tid.topology.topologymodule;
import java.util.concurrent.LinkedBlockingQueue;

import es.tid.ospf.ospfv2.OSPFv2LinkStateUpdatePacket;
import tid.topology.ONEGraph;
import tid.topology.topologymodule.ospfRead.OneGprahUpdaterThread;
import tid.topology.topologymodule.ospfRead.ReadRawSocketServer;
import tid.topology.topologymodule.session.TopologyModuleSessionServer;

/**
 * Main. Create the Topology Graph, an InformationRetriever and TopologyUpdater instances and open the session to read the messages from the web service.
 * @author Telefonica I+D
 *
 */
public class TopologyModuleMain {

	/**
	 * @param args
	 */
	public static void main(String[] args) {		
		//First of all, read the parameters
		TopologyModuleParameters params= new TopologyModuleParameters();	
		params.initialize();
		ONEGraph oneGraph =new ONEGraph(); 
		
		//params.setLearnTopology("fromFloodLight");
		
		if (params.getLearnTopology().equals("fromXML")){		
			oneGraph.readNetwork(params.getTopologyFile());		
			InformationRetriever informationRetriever = new InformationRetriever(oneGraph);	
			TopologyUpdater topologyUpdater = new TopologyUpdater(oneGraph);
			TopologyModuleSessionServer topologyModuleSessionServer = new TopologyModuleSessionServer(informationRetriever,topologyUpdater,/*messageQueue,*/params.getTopologyModulePort());
			topologyModuleSessionServer.start();
		}
		else if (params.getLearnTopology().equals("fromFloodLight"))
		{
			System.out.println("Learning topology from floodlight. Not implemented yet");
			//TopologyUpdater topologyUpdater = new TopologyUpdater(oneGraph);
		}
		else
		{
			LinkedBlockingQueue<OSPFv2LinkStateUpdatePacket> ospfv2PacketQueue = new LinkedBlockingQueue<OSPFv2LinkStateUpdatePacket>();
			TopologyUpdater topologyUpdater = new TopologyUpdater(oneGraph);
			OneGprahUpdaterThread oneGprahUpdaterThread = new OneGprahUpdaterThread(ospfv2PacketQueue,topologyUpdater);
			oneGprahUpdaterThread.start();
			ReadRawSocketServer readRawSocketServer = new ReadRawSocketServer(ospfv2PacketQueue);
			readRawSocketServer.start();
			try {
				Thread.sleep(350000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println(oneGraph.toString());
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.exit(0);
		}
	}


}
