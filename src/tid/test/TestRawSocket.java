package tid.test;

import static com.savarese.rocksaw.net.RawSocket.PF_INET;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import tid.pce.server.TopologyUpdaterThread;
import tid.pce.tedb.DomainTEDB;
import tid.pce.tedb.SimpleTEDB;

import com.savarese.rocksaw.net.RawSocket;
//import tid.util.Utils;

import es.tid.ospf.ospfv2.OSPFPacketTypes;
import es.tid.ospf.ospfv2.OSPFv2HelloPacket;
import es.tid.ospf.ospfv2.OSPFv2LinkStateUpdatePacket;
import es.tid.ospf.ospfv2.OSPFv2Packet;

public class TestRawSocket {
	  // Timeout para el socket
	private static final int TIMEOUT = 0;
	
	private static Logger log=Logger.getLogger("OSPFParser");;
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		//LSA LSA_msg
		String mCast;
		String ipLocal;	
		String ipLocal2;
		String bindAddr;
		
		FileHandler fh=null;
		try {
			fh=new FileHandler("OSPFParser.log");
		} catch (SecurityException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		log.addHandler(fh);
		log.setLevel(Level.ALL);
		if (args.length >=4 ){
			bindAddr=args[0];
			mCast=args[1];
			ipLocal=args[2];
			ipLocal2=args[3];
			log.info("Fijamos bind a "+bindAddr+" mCast "+mCast+" y ipLocal1 "+ipLocal+" y ipLocal2 "+ipLocal2);
		}else {
			ipLocal="193.145.240.8";
			//ipLocal="172.16.1.1";
			ipLocal2="10.95.73.65";
			mCast="224.0.0.5";
			bindAddr="0.0.0.0";
			log.info("Asumimos bind a "+bindAddr+" mCast "+mCast+" y ipLocal "+ipLocal);			
		}
		DomainTEDB ted=new SimpleTEDB();
		LinkedBlockingQueue<OSPFv2LinkStateUpdatePacket> ospfv2PacketQueue = new LinkedBlockingQueue<OSPFv2LinkStateUpdatePacket>();		
		TopologyUpdaterThread tut = new TopologyUpdaterThread(ospfv2PacketQueue, ted,0,32);		
		tut.start();
		
		
		int tipo = 1;
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

		
		log.info("OSPF RAW Socket opened");
		
					
//		try {
//			log.info("Creating OSPF Packet");
//			OSPFTEv2LSA LSA_msg=new OSPFTEv2LSA();
//			LinkTLV linkTLV= new LinkTLV();
//			LSA_msg.setLinkTLV(linkTLV);
//			OSPFv2LinkStateUpdatePacket ospf_packet= new OSPFv2LinkStateUpdatePacket();
//			(ospf_packet.getLSAlist()).add(LSA_msg);
//			ospf_packet.encode();
////			byte[] test=new  byte[4];
////			test[0]=3;
////			test[1]=5;
////			test[2]=7;
////			test[3]=9;
//			byte[] test2=ospf_packet.getBytes();
////			log.info(" tam "+test2.length);
////			log.info(" byte[0] "+test2[0]);
//			socket.write(dirPCE,test2);
//			log.info(" OSPF Packet sent!!");
//
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//			return;
//		}
//		
		int i=0;
		while(i<50000) {
			i++;
			try {
				
				OSPFv2Packet ospfv2Packet =readOSPFv2Packet(socket);
				
				log.info("PACKET READ");
				if (ospfv2Packet != null){
					log.info("OSPFv2 PACKET READ");
					if (ospfv2Packet.getType() == OSPFPacketTypes.OSPFv2_LINK_STATE_UPDATE){
						log.info("OSPFv2_LINK_STATE_UPDATE READ");		
						log.info("--------------------------------");	
						//Imprimo el paquete:
						log.info(ospfv2Packet.toString());
						ospfv2PacketQueue.add((OSPFv2LinkStateUpdatePacket)ospfv2Packet);
						
						
					}
				}
				
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
				log.info("OSPF Socket ends");
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
		//log.info("Hemos leido "+r+" bytes: "+Utils.toHexString(temp));
		//FuncionesUtiles.printByte(temp, "Hemos leido "+r+" bytes: ");
		}catch (IOException e){
			log.info("Salgo por excepcion");
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
//					log.info("Hemos leido "+r+" bytes: "+Utils.toHexString(temp));
////					printByte(temp, "temp");
//					//log.info("Final de la cabecera:"+ r+ " "+ temp[offset]);
//					endMsg=true;
//				}
//				else {
//
//					log.info("Waiting a leer cabecera.");
//					r = socket.read(hdr, offset, 20);
//					log.info("leido "+r+" bytes: "+Utils.toHexString(hdr));
//					//printByte(hdr, "hdr");
//					
//					//endHdr=true;
//				}
//			}catch (IOException e){
//				log.info("Salgo por excepcion");
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
//					log.info("Longitud de la cabecera parece ser "+length);
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
//			if (r > 0) {
////				log.info("r>0: "+r+" offset: "+offset);
////			//	log.info("hdr "+ (hdr[offset]&0xFF));
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
////				log.info("Lenght: "+length);
////				if ((length > 0) && (offset == length - 1)) {
////					log.info("Final del mensaje: Longitud -1: "+ (length-1) +"offset: "+ offset );
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
////				log.info("r=-1");
////				//log.warning("End of stream has been reached");
////				throw new IOException();
////
////			}
//
//		}
//		if (length > 0) {
//			//log.info("lenght >0 ");
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
			OSPFv2HelloPacket hello= new OSPFv2HelloPacket(bytes, offset);
		}
		if (type ==  OSPFPacketTypes.OSPFv2_DATABASE_DESCRIPTION){
			
		}
		if (type == OSPFPacketTypes.OSPFv2_LINK_STATE_REQUEST){
			
		}
		if (type == OSPFPacketTypes.OSPFv2_LINK_STATE_UPDATE){
			log.info(" LINK_STATE_UPDATE PACKET READ");
			ospfv2Packet = new OSPFv2LinkStateUpdatePacket(bytes,offset);
			
		}
		if (type==OSPFPacketTypes.OSPFv2_LINK_STATE_ACKNOWLEDGEMENT){
			
		}
		return ospfv2Packet;
		
	}
}