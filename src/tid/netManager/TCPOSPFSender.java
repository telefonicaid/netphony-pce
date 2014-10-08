package tid.netManager;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.Socket;
import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

import es.tid.ospf.ospfv2.OSPFv2LinkStateUpdatePacket;

public class TCPOSPFSender extends Thread {

	/**
	 * Queue to read the messages to send to the PCE peer
	 */
	private LinkedBlockingQueue<OSPFv2LinkStateUpdatePacket> sendingQueue;
	private LinkedList<Socket> sockList;
	private LinkedList<DataOutputStream> out;
	Logger log=Logger.getLogger("OSPFParser");

	public TCPOSPFSender(LinkedList<Inet4Address> dirPCEList, LinkedList<Integer> portList){
		if ((dirPCEList ==null)||(portList==null))
			log.severe("Error: Empty list (dirPCEList or portList) in TCPOSPFSender.");
		if (dirPCEList.size() != portList.size())
			log.warning("Error: dirPCEList and portList with different size in TCPOSPFSender.");
		sendingQueue= new LinkedBlockingQueue<OSPFv2LinkStateUpdatePacket>();
		sockList=new LinkedList<Socket>();
		out=new LinkedList<DataOutputStream>();
		
		for (int i=0;i<dirPCEList.size();i++){
			try {
				this.sockList.add(new Socket(dirPCEList.get(i), portList.get(i)));
				log.info("SOCKET CREADO. Dir: "+dirPCEList.get(i)+" port: "+portList.get(i));
				out.add(new DataOutputStream(sockList.get(i).getOutputStream()));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
	

	public void run(){
		
		OSPFv2LinkStateUpdatePacket OSPF_msg;
		while (true){
			try {
				OSPF_msg=sendingQueue.take();
			} catch (InterruptedException e) {		
				log.severe("Exception tying to take a OSPF message from the sendingQueue in TCPOSPFSender.");
				return;
			}
			
			try {
				OSPF_msg.encode();
				log.info("OSPF_msg ready ... "+OSPF_msg.toString());
				for (int i=0; i<out.size();i++){
					out.get(i).write(OSPF_msg.getBytes());		
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


