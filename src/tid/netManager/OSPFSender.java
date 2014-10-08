package tid.netManager;

import static com.savarese.rocksaw.net.RawSocket.PF_INET;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

import com.savarese.rocksaw.net.RawSocket;

import es.tid.ospf.ospfv2.OSPFv2LinkStateUpdatePacket;

public class OSPFSender extends Thread {
	
	// Timeout para el socket
	private static final int TIMEOUT = 0;
	
	/**
	 * Queue to read the messages to send to the PCE peer
	 */
	private LinkedBlockingQueue<OSPFv2LinkStateUpdatePacket> sendingQueue;
	Inet4Address address;
	LinkedList<Inet4Address> PCETEDBAddressList;
	Logger log=Logger.getLogger("OSPFParser");
	public OSPFSender(LinkedList<Inet4Address> PCETEDBAddress, Inet4Address address){
		sendingQueue= new LinkedBlockingQueue<OSPFv2LinkStateUpdatePacket>();		
		this.address=address;
		this.PCETEDBAddressList=PCETEDBAddress;
	}
	
	public void run(){
		OSPFv2LinkStateUpdatePacket OSPF_msg;
		RawSocket socket = new RawSocket();
		try{
			socket.open(PF_INET, 89);
			socket.setUseSelectTimeout(true);
			socket.setSendTimeout(TIMEOUT);
			socket.setReceiveTimeout(TIMEOUT);
			//FIXME: ESTE BIND ESTA A FUEGO
			socket.bind(InetAddress.getByName("PCETEDBAddressList"));  
		}catch(IOException e){

		}
		while (true){
			try {
				OSPF_msg=sendingQueue.take();
			} catch (InterruptedException e) {	
				log.severe("Exception tying to take a OSPF message from the sendingQueue in OSPFSender.");
				return;
			}
			try {
				OSPF_msg.encode();
				for (int i=0;i<PCETEDBAddressList.size();i++){
				socket.write(PCETEDBAddressList.get(i),OSPF_msg.getBytes());
				log.info(" OSPF Packet sent to "+ PCETEDBAddressList.get(i)+"!!");
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return;
			}
		}
	}

	public LinkedBlockingQueue<OSPFv2LinkStateUpdatePacket> getSendingQueue() {
		return sendingQueue;
	}

	public void setSendingQueue(LinkedBlockingQueue<OSPFv2LinkStateUpdatePacket> sendingQueue) {
		this.sendingQueue = sendingQueue;
	}
}
