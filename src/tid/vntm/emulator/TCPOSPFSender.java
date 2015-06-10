package tid.vntm.emulator;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

import es.tid.ospf.ospfv2.lsa.LSA;
import tid.vntm.VNTMParameters;

public class TCPOSPFSender extends Thread {

	/**
	 * Queue to read the messages to send to the PCE peer
	 */
	private LinkedBlockingQueue<LSA> sendingQueue;
	private Socket sock;
	private DataOutputStream out;
	private VNTMParameters vntmParams;
	private Logger log;

	public TCPOSPFSender(Inet4Address dirPCE, int port){
		log=Logger.getLogger("OSPFParser");
		sendingQueue= new LinkedBlockingQueue<LSA>();
		try {
			this.sock = new Socket(dirPCE, port);
			log.info("SOCKET CREADO");
			out=new DataOutputStream(sock.getOutputStream());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	//GUARDAS EL OUTPUTSTREAM
	}
	

	public void run(){
		LSA lsa;
		while (true){
			try {
				lsa=sendingQueue.take();
			} catch (InterruptedException e) {			
				return;
			}
			
			try {
				lsa.encode();
				out.write(lsa.getLSAbytes());
				log.info(" OSPF LSA Packet sent!!");
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
	
//	public Inet4Address readPCEAddress(){
//
//		try {
//			return (Inet4Address) Inet4Address.getByName(vntmParams.getNetworkEmulatorPCEAddress().getHostAddress());
//		} catch (UnknownHostException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//
//		return null;
//	}

//	public int readOSPFTCPPort(){
//
//		// FIXME: Definir procedimiento de lectura de puerto
//
//		return vntmParams.getVNTMPort();
//
//	}
}