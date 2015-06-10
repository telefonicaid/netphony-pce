package tid.vntm.emulator;

import static com.savarese.rocksaw.net.RawSocket.PF_INET;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

import com.savarese.rocksaw.net.RawSocket;

import es.tid.ospf.ospfv2.OSPFv2LinkStateUpdatePacket;
import es.tid.ospf.ospfv2.lsa.LSA;


public class OSPFSender extends Thread {
	
	  // Timeout para el socket
	private static final int TIMEOUT = 0;
	
	/**
	 * Queue to read the messages to send to the PCE peer
	 */
	private LinkedBlockingQueue<LSA> sendingQueue;
	Inet4Address dirPCE;
	private Logger log;
	public OSPFSender(Inet4Address dirPCE){
		log=Logger.getLogger("OSPFParser");
		sendingQueue= new LinkedBlockingQueue<LSA>();
		this.dirPCE=dirPCE;
	}
	
	public void run(){
		LSA LSA_msg;
		int tipo = 1;
		RawSocket socket = new RawSocket();
		Inet4Address dirPCE=null;
		try{
			socket.open(PF_INET, 89);
			socket.setUseSelectTimeout(true);
			socket.setSendTimeout(TIMEOUT);
			socket.setReceiveTimeout(TIMEOUT);
			//FIXME: ESTE BIND ESTA A FUEGO
			socket.bind(InetAddress.getByName("172.16.1.1"));        	
			dirPCE= (Inet4Address)Inet4Address.getByName("172.16.1.3");
			
		}catch(IOException e){

		}

		
		log.info("OSPF RAW Socket opened");

		while (true){
			try {
				LSA_msg=sendingQueue.take();
			} catch (InterruptedException e) {			
				return;
			}
			
			try {
				OSPFv2LinkStateUpdatePacket ospf_packet= new OSPFv2LinkStateUpdatePacket();
				(ospf_packet.getLSAlist()).add(LSA_msg);
				ospf_packet.encode();
				socket.write(dirPCE,ospf_packet.getBytes());
				log.info(" OSPF Packet sent!!");

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return;
			}
		}
	}

	public LinkedBlockingQueue<LSA> getSendingQueue() {
		return sendingQueue;
	}

	public void setSendingQueue(LinkedBlockingQueue<LSA> sendingQueue) {
		this.sendingQueue = sendingQueue;
	}
	
	

}
