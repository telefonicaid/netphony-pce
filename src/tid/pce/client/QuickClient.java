package tid.pce.client;


import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import tid.pce.pcep.constructs.EndPoint;
import tid.pce.pcep.constructs.GeneralizedBandwidthSSON;
import tid.pce.pcep.constructs.P2PEndpoints;
import tid.pce.pcep.constructs.PCEPIntiatedLSP;
import tid.pce.pcep.constructs.Request;
import tid.pce.pcep.messages.PCEPInitiate;
import tid.pce.pcep.messages.PCEPMessage;
import tid.pce.pcep.messages.PCEPMessageTypes;
import tid.pce.pcep.messages.PCEPRequest;
import tid.pce.pcep.messages.PCEPResponse;
import tid.pce.pcep.objects.BandwidthRequestedGeneralizedBandwidth;
import tid.pce.pcep.objects.EndPoints;
import tid.pce.pcep.objects.EndPointsIPv4;
import tid.pce.pcep.objects.GeneralizedEndPoints;
import tid.pce.pcep.objects.LSP;
import tid.pce.pcep.objects.ObjectiveFunction;
import tid.pce.pcep.objects.P2MPEndPointsIPv4;
import tid.pce.pcep.objects.RequestParameters;
import tid.pce.pcep.objects.SRP;
import tid.pce.pcep.objects.tlvs.EndPointIPv4TLV;
import tid.pce.pcep.objects.tlvs.EndPointsIPv4TLV;
import tid.pce.pcep.objects.tlvs.SymbolicPathNameTLV;
import tid.pce.pcep.objects.tlvs.UnnumberedEndpointTLV;
import tid.pce.pcepsession.PCEPSessionsInformation;

public class QuickClient {

	private static UserInterface ui;
	
	
	public static final Logger Log =Logger.getLogger("PCCClient");

	public static void main(String[] args) {
		FileHandler fh;
		FileHandler fh2;
		PCCPCEPSession PCEsession;
		try {
			fh=new FileHandler("PCCClient2.log");
			fh2=new FileHandler("PCEPClientParser2.log");
			//fh.setFormatter(new SimpleFormatter());
			Log.addHandler(fh);
			Log.setLevel(Level.ALL);
			Logger log2=Logger.getLogger("PCEPParser");
			log2.addHandler(fh2);
			log2.setLevel(Level.ALL);
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			System.exit(1);
		}

		if (args.length < 4) {
			Log.info("Usage: ClientTester <host> <port>");
			return;
		}


		String ip;
		int port;


		ip = args[0];
		port = Integer.valueOf(args[1]).intValue();
		PCEPSessionsInformation pcepSessionManager=new PCEPSessionsInformation();
		PCEsession = new PCCPCEPSession(ip, port,false,pcepSessionManager);
		PCEsession.start();
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		
		int offset=2;
		LinkedList<PCEPMessage> messageList=new LinkedList<PCEPMessage>();
		System.out.println("A preparar y enviar");
		
		offset=createAndSendMessage(PCEsession,offset, args,messageList);
		System.out.println("Enviado!!!");
		String ip2;
		int port2;
		if (offset<args.length){
			System.out.println("Hbra PCE!!!");
			if (args[offset].equals("-pce")){
				ip2 = args[offset+1];
				port2 = Integer.valueOf(args[offset+2]).intValue();
				offset=offset+3;
				System.out.println("En un rato llamamos a "+ip2+": "+port2);
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				System.out.println("WGO ");

				PCCPCEPSession PCEsession2 = new PCCPCEPSession(ip2, port2,false,pcepSessionManager);
				System.out.println("RANCANDO ");
				PCEsession2.start();
				System.out.println("ALEEE ");
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				offset=createAndSendMessage(PCEsession2,offset, args, messageList);
				
			}
			
		}
		System.exit(-1);
		
		
		

	}
	
	public  static int createAndSendMessage(PCCPCEPSession PCEsession, int offset,String[] args, LinkedList<PCEPMessage> messageList ){
		ClientRequestManager crm;
		crm=PCEsession.crm;
		int message_type;
		if (args[offset].equals("-ini")){
			message_type=PCEPMessageTypes.MESSAGE_INITIATE;
			//System.out.println(" Single Initiate from "+args[offset+1]+" to "+args[offset+1]);
			System.out.println(" Single Initiate");
			offset=offset+1;
		}else {
			message_type=PCEPMessageTypes.MESSAGE_PCREQ;
			System.out.println(" Single Request with OF =1002 and OF bit = 1, from "+args[2]+" to "+args[2]);

		}
		if (message_type==PCEPMessageTypes.MESSAGE_PCREQ){
			String src= args[2];
			String src_ip="";;
			long src_if=0;
			long dst_if=0;
			String dst_ip="";
			String dst=args[3];
			if (src.contains(":")){
				String[] parts = src.split(":");
				src_ip=parts[0];
				src_if=Long.valueOf(parts[1]).longValue();
			}else {
				src_ip=args[2];
			}
			if (dst.contains(":")){
				String[] parts = dst.split(":");
				dst_ip=parts[0];
				dst_if=Long.valueOf(parts[1]).longValue();
			}else {
				dst_ip=args[3];
			}
			boolean gen=false;
			offset=4;
			if (args.length>=5) {
				if (args[offset].equals("-g")){
					offset=offset+1;
					gen=true;
				}	
			}


			PCEPRequest p_r = new PCEPRequest();
			Request req = new Request();
			RequestParameters rp= new RequestParameters();
			rp.setPbit(true);
			req.setRequestParameters(rp);		
			rp.setRequestID(PCCPCEPSession.getNewReqIDCounter());
			System.out.println("Creating test Request");

			int prio = 1;;
			rp.setPrio(prio);
			boolean reo = false;
			rp.setReopt(reo);
			boolean bi = false;
			rp.setBidirect(bi);
			boolean lo = false;
			rp.setLoose(lo);
			if (gen==true){
				GeneralizedEndPoints ep=new GeneralizedEndPoints();
				req.setEndPoints(ep);
				P2PEndpoints p2pEndpoints = new P2PEndpoints();	
				EndPoint ep_s =new EndPoint();
				p2pEndpoints.setSourceEndPoints(ep_s);
				EndPoint ep_d =new EndPoint();
				p2pEndpoints.setDestinationEndPoints(ep_d);
				ep.setP2PEndpoints(p2pEndpoints);
				if (src_if!=0){
					UnnumberedEndpointTLV un = new UnnumberedEndpointTLV();
					Inet4Address ipp;
					try {
						ipp = (Inet4Address)Inet4Address.getByName(src_ip);
						un.setIPv4address(ipp);
						un.setIfID(src_if);
						ep_s.setUnnumberedEndpoint(un);


					} catch (UnknownHostException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}else {
					EndPointIPv4TLV ipv4tlv = new EndPointIPv4TLV();
					Inet4Address ipp;
					try {
						ipp = (Inet4Address)Inet4Address.getByName(src_ip);
						ipv4tlv.setIPv4address(ipp);
						ep_s.setEndPointIPv4TLV(ipv4tlv);

					} catch (UnknownHostException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}
				if (dst_if!=0){
					UnnumberedEndpointTLV un = new UnnumberedEndpointTLV();
					Inet4Address ipp;
					try {
						ipp = (Inet4Address)Inet4Address.getByName(dst_ip);
						un.setIPv4address(ipp);
						un.setIfID(dst_if);
						ep_d.setUnnumberedEndpoint(un);


					} catch (UnknownHostException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}else {
					EndPointIPv4TLV ipv4tlv = new EndPointIPv4TLV();
					Inet4Address ipp;
					try {
						ipp = (Inet4Address)Inet4Address.getByName(dst_ip);
						ipv4tlv.setIPv4address(ipp);
						ep_d.setEndPointIPv4TLV(ipv4tlv);

					} catch (UnknownHostException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}

			} else {
				EndPointsIPv4 ep=new EndPointsIPv4();				
				req.setEndPoints(ep);
				//String src_ip= "1.1.1.1";
				Inet4Address ipp;
				try {
					ipp = (Inet4Address)Inet4Address.getByName(src_ip);
					ep.setSourceIP(ipp);								
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				System.out.println(" - Destination IP address: ");
				//br2 = new BufferedReader(new InputStreamReader(System.in));
				//String dst_ip="172.16.101.101";
				Inet4Address i_d;
				try {
					i_d = (Inet4Address)Inet4Address.getByName(dst_ip);
					ep.setDestIP(i_d);
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}



			//req.getRequestParameters().s
			int of_code=-1;
			if (args.length>offset) {
				if (args[offset].equals("-of")){		
					offset=offset+1;
					if (args.length>offset) {
						of_code=Integer.parseInt(args[offset]);	
							offset=offset+1;												
					}
				}	
			}
			if (of_code!=-1){
				ObjectiveFunction of=new ObjectiveFunction();
				of.setOFcode(1002);
				of.setPbit(true);
				req.setObjectiveFunction(of);
			}
				
			
			
			p_r.addRequest(req);
			BandwidthRequestedGeneralizedBandwidth gw = new BandwidthRequestedGeneralizedBandwidth();
			GeneralizedBandwidthSSON gwsson = new GeneralizedBandwidthSSON();
			gwsson.setM(2);
			gw.setGeneralizedBandwidth(gwsson);
			req.setBandwidth(gw);
			System.out.println("Peticion "+req.toString());
			PCEPResponse pr=crm.newRequest(p_r);
			messageList.add(pr);
			System.out.println("Respuesta "+pr.toString());
		} else if (message_type==PCEPMessageTypes.MESSAGE_INITIATE) {
			System.out.println("A por initiate"); 
			String src= args[offset];
			String src_ip="";;
			long src_if=0;
			long dst_if=0;
			String dst_ip="";
			String dst=args[offset+1];
			if (src.contains(":")){
				String[] parts = src.split(":");
				src_ip=parts[0];
				src_if=Long.valueOf(parts[1]).longValue();
			}else {
				src_ip=args[offset];
			}
			if (dst.contains(":")){
				String[] parts = dst.split(":");
				dst_ip=parts[0];
				dst_if=Long.valueOf(parts[1]).longValue();
			}else {
				dst_ip=args[offset+1];
			}
			boolean gen=false;
			boolean p2mp=false;
			offset=offset+2;
			System.out.println("origen: "+src_ip+" destino: "+dst_ip);
			System.out.println("offset vale "+offset+" y la length "+args.length);
			
			if (args.length>offset) {
				
				if (args[offset].equals("-g")){
					offset=offset+1;
					gen=true;
				}	
				else if (args[offset].equals("-p2mp")){
					offset=offset+1;
					p2mp=true;
				}
			}

			System.out.println("OLEEEE");
			
			PCEPInitiate p_i = new PCEPInitiate();
			PCEPIntiatedLSP lsp_ini = new PCEPIntiatedLSP();
			p_i.getPcepIntiatedLSPList().add(lsp_ini);
			EndPoints ep;
			if (gen==true){
				ep=new GeneralizedEndPoints();
				P2PEndpoints p2pEndpoints = new P2PEndpoints();	
				EndPoint ep_s =new EndPoint();
				p2pEndpoints.setSourceEndPoints(ep_s);
				EndPoint ep_d =new EndPoint();
				p2pEndpoints.setDestinationEndPoints(ep_d);
				((GeneralizedEndPoints) ep).setP2PEndpoints(p2pEndpoints);
				if (src_if!=0){
					UnnumberedEndpointTLV un = new UnnumberedEndpointTLV();
					Inet4Address ipp;
					try {
						ipp = (Inet4Address)Inet4Address.getByName(src_ip);
						un.setIPv4address(ipp);
						un.setIfID(src_if);
						ep_s.setUnnumberedEndpoint(un);


					} catch (UnknownHostException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}else {
					EndPointIPv4TLV ipv4tlv = new EndPointIPv4TLV();
					Inet4Address ipp;
					try {
						ipp = (Inet4Address)Inet4Address.getByName(src_ip);
						ipv4tlv.setIPv4address(ipp);
						ep_s.setEndPointIPv4TLV(ipv4tlv);

					} catch (UnknownHostException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}
				if (dst_if!=0){
					UnnumberedEndpointTLV un = new UnnumberedEndpointTLV();
					Inet4Address ipp;
					try {
						ipp = (Inet4Address)Inet4Address.getByName(dst_ip);
						un.setIPv4address(ipp);
						un.setIfID(dst_if);
						ep_d.setUnnumberedEndpoint(un);


					} catch (UnknownHostException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}else {
					EndPointIPv4TLV ipv4tlv = new EndPointIPv4TLV();
					Inet4Address ipp;
					try {
						ipp = (Inet4Address)Inet4Address.getByName(dst_ip);
						ipv4tlv.setIPv4address(ipp);
						ep_d.setEndPointIPv4TLV(ipv4tlv);

					} catch (UnknownHostException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}

			} else if (p2mp==true){ 
				ep=new P2MPEndPointsIPv4();
				LinkedList<Inet4Address> destIPList= new LinkedList<Inet4Address>();
				Inet4Address sourceIPP;
				Inet4Address destIPP1;
				Inet4Address destIPP2;
				Inet4Address destIPP3;
				try {
					sourceIPP = (Inet4Address)Inet4Address.getByName(src_ip);
					((P2MPEndPointsIPv4)ep).setSourceIP(sourceIPP);
					((P2MPEndPointsIPv4)ep).setLeafType(1);
					destIPP1 = (Inet4Address)Inet4Address.getByName(dst_ip);
					destIPList.add(destIPP1);
					destIPP2 = (Inet4Address)Inet4Address.getByName(args[offset]);
					destIPP3 = (Inet4Address)Inet4Address.getByName(args[offset+1]);
					offset=offset+2;
					destIPList.add(destIPP2);
					destIPList.add(destIPP3);
					((P2MPEndPointsIPv4)ep).setDestIPList(destIPList);
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			} else {
				System.out.println("END POINTS NORMALES");
				ep=new EndPointsIPv4();				
				//String src_ip= "1.1.1.1";
				Inet4Address ipp;
				try {
					ipp = (Inet4Address)Inet4Address.getByName(src_ip);
					((EndPointsIPv4) ep).setSourceIP(ipp);								
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				System.out.println(" - Destination IP address: ");
				//br2 = new BufferedReader(new InputStreamReader(System.in));
				//String dst_ip="172.16.101.101";
				Inet4Address i_d;
				try {
					i_d = (Inet4Address)Inet4Address.getByName(dst_ip);
					((EndPointsIPv4) ep).setDestIP(i_d);
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			lsp_ini.setEndPoint(ep);
			SRP srp= new SRP();
			if (args[offset].equals("-srpid")){
				long srp_id = Long.valueOf(args[offset+1]).intValue();
				offset=offset+2;
				srp.setSRP_ID_number(srp_id);

			}else {
				srp.setSRP_ID_number(1);

			}
			lsp_ini.setRsp(srp);
			
			LSP lsp = new LSP();
			lsp_ini.setLsp(lsp);
			SymbolicPathNameTLV symPathName = new SymbolicPathNameTLV();
			symPathName.setSymbolicPathNameID("HOLA".getBytes());
			lsp.setSymbolicPathNameTLV_tlv(symPathName);
			
			if (args[offset].equals("-ero")){
				offset=offset+1;
				if (args[offset].equals("-prev")){
					offset=offset+1;
					PCEPResponse pr=(PCEPResponse)messageList.getLast();
					lsp_ini.setEro(pr.getResponse(0).getPath(0).geteRO());
				}	
			}	
			
			
			
			System.out.println("Peticion "+p_i.toString());
			PCEPMessage pr=crm.newRequest(p_i);
			messageList.add(pr);
			System.out.println("Respuesta "+p_i.toString());
			
			
			
		}
		return offset;
	}

}
