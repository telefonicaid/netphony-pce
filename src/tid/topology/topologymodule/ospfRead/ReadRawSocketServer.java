package tid.topology.topologymodule.ospfRead;

import static com.savarese.rocksaw.net.RawSocket.PF_INET;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

import com.savarese.rocksaw.net.RawSocket;

import es.tid.ospf.ospfv2.OSPFPacketTypes;
import es.tid.ospf.ospfv2.OSPFv2HelloPacket;
import es.tid.ospf.ospfv2.OSPFv2LinkStateUpdatePacket;
import es.tid.ospf.ospfv2.OSPFv2Packet;

public class ReadRawSocketServer extends Thread {
	  // Timeout para el socket
	private static final int TIMEOUT = 0;
	private int OSPFTCPPort;
	private LinkedBlockingQueue<OSPFv2LinkStateUpdatePacket> ospfv2PacketQueue;
	/**
	 * OSPF logger
	 */
		private Logger log;
	
	public ReadRawSocketServer(LinkedBlockingQueue<OSPFv2LinkStateUpdatePacket> ospfv2PacketQueue){
		log=Logger.getLogger("OSPFParser");
		this.ospfv2PacketQueue = ospfv2PacketQueue;
		
	}
	
	public void run(){
		//ESto es a pelo para la prueba!!
		String ipLocal="172.16.1.71";
		
		String ipLocal2="172.16.1.71";
		String mCast="224.0.0.5";
		String bindAddr="0.0.0.0";
		System.out.println("Asumimos bind a "+bindAddr+" mCast "+mCast+" y ipLocal "+ipLocal);			
		//------HASTA AQUI --------
		
		RawSocket socket = new RawSocket();
		Inet4Address dirPCE=null;
		try{
			socket.open(PF_INET, 89);
			socket.setUseSelectTimeout(true);
			socket.setSendTimeout(TIMEOUT);
			socket.setReceiveTimeout(TIMEOUT);
			socket.setIPHeaderInclude(false);
			//socket.bind(InetAddress.getByName("172.16.1.1"));						
			//socket.bindDevice("eth1");
		//	socket.bind(InetAddress.getByName(bindAddr));
			socket.joinGroup(InetAddress.getByName(mCast), InetAddress.getByName(ipLocal));
			//socket.joinGroup(InetAddress.getByName(mCast), InetAddress.getByName(ipLocal2));
			//socket.bind(InetAddress.getByName(bindAddr));
			//socket.bindDevice("eth1");
			//dirPCE= (Inet4Address)Inet4Address.getByName("172.16.1.2");
			
		}catch(IOException e){

		}

		
		while(true) {
		
			try {
				
				OSPFv2Packet ospfv2Packet =readOSPFv2Packet(socket);
				
				System.out.println("PACKET READ");
				if (ospfv2Packet != null){
					//System.out.println("OSPFv2 PACKET READ");
					if (ospfv2Packet.getType() == OSPFPacketTypes.OSPFv2_LINK_STATE_UPDATE){
						//System.out.println("OSPFv2_LINK_STATE_UPDATE READ");		
						//System.out.println("--------------------------------");	
						//Imprimo el paquete:
						//System.out.println(ospfv2Packet.toString());
						ospfv2PacketQueue.add((OSPFv2LinkStateUpdatePacket)ospfv2Packet);
						
						
					}
				}
				
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
				System.out.println("OSPF Socket ends");
				return;
			}//Read a new message
		}
	    
	}
	
	/**
	 * Read PCE message from TCP stream
	 * @param in InputStream
	 */
	protected static OSPFv2Packet  readOSPFv2Packet(RawSocket socket) throws IOException{
		byte[] hdr = new byte[20];
		byte[] temp = null;
		boolean endHdr = false;
	
		int r = 0;
		int length = 0;
		boolean endMsg = false;
		int offset = 0;
	
		OSPFv2Packet ospfv2Packet= null ;
		length = 1500;//Max MTU size?
		temp=new byte[length];
		try {

		r = socket.read (temp, 0, length);
		//System.out.println("Hemos leido "+r+" bytes: "+Utils.toHexString(temp));
		//FuncionesUtiles.printByte(temp, "Hemos leido "+r+" bytes: ");
		}catch (IOException e){
			System.out.println("Salgo por excepcion");
			//log.warning("Error reading data: "+ e.getMessage());
			throw e;

		}catch (Exception e) {		
			throw new IOException();
		}
		offset=20;//bytes of IP header
		int type = OSPFv2Packet.getLStype(temp, offset);
		ospfv2Packet = createOSPFPacket(type,offset,temp);
		
//		while (!endMsg) {
//
//			try {
//
//				if (endHdr) {
//
//					r = socket.read (temp, 0, length);
//					System.out.println("Hemos leido "+r+" bytes: "+Utils.toHexString(temp));
////					printByte(temp, "temp");
//					//System.out.println("Final de la cabecera:"+ r+ " "+ temp[offset]);
//					endMsg=true;
//				}
//				else {
//
//					System.out.println("Waiting a leer cabecera.");
//					r = socket.read(hdr, offset, 20);
//					System.out.println("leido "+r+" bytes: "+Utils.toHexString(hdr));
//					//printByte(hdr, "hdr");
//					
//					//endHdr=true;
//				}
//			}catch (IOException e){
//				System.out.println("Salgo por excepcion");
//				//log.warning("Error reading data: "+ e.getMessage());
//				throw e;
//
//			}catch (Exception e) {		
//				throw new IOException();
//			}
//			if (endHdr==false){
//				if (r > 0) {
//					length = ((int)hdr[2]&0xFF) << 8;
//					length = length | (((int)hdr[3]&0xFF));
//					System.out.println("Longitud de la cabecera parece ser "+length);
//					temp = new byte[length];
//					endHdr=true;
//					offset=20;
//				}
//				
//				
//			}else {
//				if (r > 0) {
//					offset++;
//				}
//			}
////			if (r > 0) {
////				System.out.println("r>0: "+r+" offset: "+offset);
////			//	System.out.println("hdr "+ (hdr[offset]&0xFF));
////				if (offset == 2) {
////					
////					length = ((int)hdr[offset]&0xFF) << 8;
////
////				}
////				
////				if (offset == 3) {
////
////					length = length | (((int)hdr[offset]&0xFF));
////					temp = new byte[length];
////					//endHdr = true;
////					//System.arraycopy(hdr, 0, temp, 0, 20);
////
////				}
////				System.out.println("Lenght: "+length);
////				if ((length > 0) && (offset == length - 1)) {
////					System.out.println("Final del mensaje: Longitud -1: "+ (length-1) +"offset: "+ offset );
////					endMsg = true;
////
////				}
////				if (offset == 19) {					
////					endHdr = true;
////					System.arraycopy(hdr, 0, temp, 0, 20);
////
////				}
////
////				offset++;
////
////			}
////			else if (r==-1){
////				System.out.println("r=-1");
////				//log.warning("End of stream has been reached");
////				throw new IOException();
////
////			}
//
//		}
//		if (length > 0) {
//			//System.out.println("lenght >0 ");
//			offset=20;
//		
//			//while (offset < length){
//				//Recorrer el mensaje creando las lsas.
//				int type = OSPFv2Packet.getLStype(temp, offset);
//							
//				 ospfv2Packet = createOSPFPacket(type,offset,temp);
//				
//				//offset = offset +length_ospf_packet;
//				//lsas.addAll(((OSPFv2LinkStateUpdatePacket)ospfv2Packet).getLSAlist());
//			//}
//			//printByte(ret, "ret");
//		}		
////		printByte(ret, "ret");
		return ospfv2Packet;

	}
	
	public static OSPFv2Packet createOSPFPacket(int type,int offset, byte[] bytes){
		OSPFv2Packet ospfv2Packet = null;
		if (type == OSPFPacketTypes.OSPFv2_HELLO_PACKET){
			//System.out.println("HELLO PACKET READ");
			OSPFv2HelloPacket hello= new OSPFv2HelloPacket(bytes, offset);
		}
		if (type ==  OSPFPacketTypes.OSPFv2_DATABASE_DESCRIPTION){
			
		}
		if (type == OSPFPacketTypes.OSPFv2_LINK_STATE_REQUEST){
			
		}
		if (type == OSPFPacketTypes.OSPFv2_LINK_STATE_UPDATE){
			System.out.println(" LINK_STATE_UPDATE PACKET READ");
			ospfv2Packet = new OSPFv2LinkStateUpdatePacket(bytes,offset);
			
		}
		if (type==OSPFPacketTypes.OSPFv2_LINK_STATE_ACKNOWLEDGEMENT){
			
		}
		return ospfv2Packet;
		
	}
}
