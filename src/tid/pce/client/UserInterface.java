package tid.pce.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.logging.Logger;

import tid.pce.pcep.constructs.EndPoint;
import tid.pce.pcep.constructs.P2PEndpoints;
import tid.pce.pcep.constructs.Request;
import tid.pce.pcep.constructs.SVECConstruct;
import tid.pce.pcep.messages.PCEPError;
import tid.pce.pcep.messages.PCEPKeepalive;
import tid.pce.pcep.messages.PCEPRequest;
import tid.pce.pcep.messages.PCEPResponse;
import tid.pce.pcep.objects.Bandwidth;
import tid.pce.pcep.objects.EndPointsIPv4;
import tid.pce.pcep.objects.GeneralizedEndPoints;
import tid.pce.pcep.objects.Metric;
import tid.pce.pcep.objects.ObjectParameters;
import tid.pce.pcep.objects.ObjectiveFunction;
import tid.pce.pcep.objects.PCEPErrorObject;
import tid.pce.pcep.objects.RequestParameters;
import tid.pce.pcep.objects.Reservation;
import tid.pce.pcep.objects.Svec;
import tid.pce.pcep.objects.tlvs.EndPointIPv4TLV;

public class UserInterface extends Thread {
	
	PCCPCEPSession ps;
	 ClientRequestManager crm;
	private Logger log;
	
	private boolean running=false;
	
	public UserInterface (PCCPCEPSession ps){
		this.ps=ps;
		log=Logger.getLogger("PCCClient");
		crm=ps.crm;
		
	}
	
	@Override
	public void run() {
		log.info("Starting User Interface");
		running=true;
		while (running) {
			System.out.println("-------------");
			System.out.println("Enter action: ");
			System.out.println("-------------");
			//System.out.println(" 1- OPEN Session");
			System.out.println(" 1- Send KEEPALIVE");
			System.out.println(" 2- Send Path Request");
			System.out.println(" 3- Send default Path Request");
			System.out.println(" 3333- Send default Path Request that will trigger NOPATH");
			System.out.println(" 31- Send default Path Request with OF = 5");
			System.out.println(" 33- Send default Mulitple Path Requests");
			System.out.println(" 34- Send default Synchronized Path Requests");
			System.out.println(" 35- Send default BIG Synchronized Path Requests");
			System.out.println(" 36- Send default Synchronized Path Requests with OF=7");
			System.out.println(" 4- Send ERROR");
			System.out.println(" 5- Send Notification");
			System.out.println(" 6- Send CLOSE message");
			System.out.println(" 7 - END PROGRAM");
			System.out.println(" 8 - Send Path Request using GENERALIZED END POINTS");
			System.out.println(" 88 - Send IT resource Notification");
			System.out.println(" 0 - Sergio prueba");
			System.out.println(" 01 - Sergio prueba 2");
			System.out.println(" 02 - Sergio prueba 3");
			System.out.println("1002 - SSON TEST");
			
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

			String command = null;

			try {
				command = br.readLine();
			} catch (IOException ioe) {
				System.out.println("IO error trying to read your command");
				System.exit(1);
			}
			
			if (command.equals("0")) {
				System.out.println("Single Request MULTILAYER!");
				PCEPRequest p_r = new PCEPRequest();
				Request req = createRequest("172.16.201.101", "172.16.201.103");
				ObjectiveFunction of=new ObjectiveFunction();
				of.setOFcode(1100);
				req.setObjectiveFunction(of);
				p_r.addRequest(req);
				PCEPResponse pr=crm.newRequest(p_r);
				System.out.println("Respuesta "+pr.toString());
			}
			
			if (command.equals("02")) {
				System.out.println("Single Request MULTILAYER!");
				PCEPRequest p_r = new PCEPRequest();
				Request req = createRequest("172.16.201.101", "172.16.201.103");
				ObjectiveFunction of=new ObjectiveFunction();
				of.setOFcode(1100);
				req.setObjectiveFunction(of);
				
				float bw = 8;
				Bandwidth bandwidth=new Bandwidth();
				bandwidth.setBw(bw);
				req.setBandwidth(bandwidth);
				
				p_r.addRequest(req);
				PCEPResponse pr=crm.newRequest(p_r);
				System.out.println("Respuesta "+pr.toString());
			}
			
			if (command.equals("01")) {
				System.out.println("Single Request MULTILAYER!");
				PCEPRequest p_r = new PCEPRequest();
				Request req = createRequest("172.20.1.54", "172.20.1.60");
				ObjectiveFunction of=new ObjectiveFunction();
				of.setOFcode(1101);
				req.setObjectiveFunction(of);
				
				
				p_r.addRequest(req);
				PCEPResponse pr=crm.newRequest(p_r);
				System.out.println("Respuesta "+pr.toString());
			}
			
			if (command.equals("1002")) {
				System.out.println("Single Request!");
				PCEPRequest p_r = new PCEPRequest();
				Request req2 = createRequest("172.20.1.1", "172.20.1.5");
				ObjectiveFunction of=new ObjectiveFunction();
				Reservation rs1=new Reservation();
				of.setOFcode(1002);
				req2.setObjectiveFunction(of);
				req2.setReservation(rs1);
				float bw2 = 100000000;
				Bandwidth bandwidth2=new Bandwidth();
				bandwidth2.setBw(bw2);
				req2.setBandwidth(bandwidth2);
				p_r.addRequest(req2);
				PCEPResponse pr=crm.newRequest(p_r,45000);
				System.out.println("Respuesta "+pr.toString());
				
				PCEPRequest p_r2 = new PCEPRequest();
				Request req_1 = createRequest("172.20.1.1", "172.20.1.2");
				ObjectiveFunction of2=new ObjectiveFunction();
				of2.setOFcode(1002);
				req_1.setObjectiveFunction(of2);
				Reservation rs2=new Reservation();
				req_1.setReservation(rs2);
				float bw_1 = 50000000;
				Bandwidth bandwidth_1=new Bandwidth();
				bandwidth_1.setBw(bw_1);
				req_1.setBandwidth(bandwidth_1);
				p_r2.addRequest(req_1);
				PCEPResponse pr_2=crm.newRequest(p_r2,45000);
				System.out.println("Respuesta "+pr_2.toString());
			}
			
			
			if (command.equals("1")) {
				PCEPKeepalive p_ka= new PCEPKeepalive();
				log.fine("Sending Keepalive message");
				ps.sendPCEPMessage(p_ka);			
			}
			
			if (command.equals("2")) {
				PCEPRequest p_r = new PCEPRequest();
				Request req = new Request();
				p_r.addRequest(req);
				RequestParameters rp= new RequestParameters();
				rp.setPbit(true);
				req.setRequestParameters(rp);		
				rp.setRequestID(PCCPCEPSession.getNewReqIDCounter());
				System.out.println("Enter PCReq parameters: ");
				System.out.println(" - Priority: ");
				BufferedReader br2 = new BufferedReader(new InputStreamReader(System.in));
				try {
					int prio = Integer.parseInt(br2.readLine());
					rp.setPrio(prio);
				} catch (IOException ioe) {
					System.out.println("IO error trying to read parameter");
					System.exit(1);
				}
				
				System.out.println(" - Reoptimization: ");
				//br2 = new BufferedReader(new InputStreamReader(System.in));
				try {
					int reo = Integer.parseInt(br2.readLine());
					rp.setReopt(reo==1);
					
				} catch (IOException ioe) {
					System.out.println("IO error trying to read parameter");
					System.exit(1);
				}
				
				System.out.println(" - Bidirectional LSP: ");
				//br2 = new BufferedReader(new InputStreamReader(System.in));
				try {
					int bi = Integer.parseInt(br2.readLine());
					rp.setBidirect(bi==1);
				} catch (IOException ioe) {
					System.out.println("IO error trying to read parameter");
					System.exit(1);
				}
								
				System.out.println(" - Strict/Loose (0/1): ");
				//br2 = new BufferedReader(new InputStreamReader(System.in));
				try {
					int lo = Integer.parseInt(br2.readLine());
					rp.setLoose(lo==1);
				} catch (IOException ioe) {
					System.out.println("IO error trying to read parameter");
					System.exit(1);
				}
				
//				System.out.println(" - Request ID: ");
//				br2 = new BufferedReader(new InputStreamReader(System.in));
//				try {
//					int lo = Integer.parseInt(br2.readLine());
//					rp.setLoose(lo);
//					
//				} catch (IOException ioe) {
//					System.out.println("IO error trying to read parameter");
//					System.exit(1);
//				}
//				params[4] = Integer.valueOf(param);
				EndPointsIPv4 ep=new EndPointsIPv4();				
				req.setEndPoints(ep);
				System.out.println(" - Source IP address: ");
				br2 = new BufferedReader(new InputStreamReader(System.in));
				try {
					String ip= br2.readLine();
					Inet4Address ipp=(Inet4Address)Inet4Address.getByName(ip);
					ep.setSourceIP(ipp);								
				} catch (IOException ioe) {
					System.out.println("IO error trying to read parameter");
					System.exit(1);
				}
				//params[5] = Integer.valueOf(param);
				
				System.out.println(" - Destination IP address: ");
				//br2 = new BufferedReader(new InputStreamReader(System.in));
				try {
					String ip= br2.readLine();
					Inet4Address ipp=(Inet4Address)Inet4Address.getByName(ip);
					ep.setDestIP(ipp);
				} catch (IOException ioe) {
					System.out.println("IO error trying to read parameter");
					System.exit(1);
				}
				
				PCEPResponse pr=crm.newRequest(p_r);
//				if (ps.sendRequest(params)) {
//					System.out.println("Request sent OK");
//				}
				System.out.println("Respuesta "+pr.toString());
				
			}
			
			if (command.equals("33")) {
				PCEPRequest p_r = new PCEPRequest();
				System.out.println(" Multiple Requests!!!!");
				Request req = createRequest("172.16.103.101", "172.16.101.102");
				Request req2 = createRequest("172.16.101.101", "172.16.101.102");
				Request req3 = createRequest("172.16.101.101", "172.16.103.103");
				Request req4 = createRequest("172.16.102.101", "172.16.103.104");
				Request req5 = createRequest("172.16.103.101", "172.16.101.102");
				Request req6 = createRequest("172.16.102.101", "172.16.101.102");
				p_r.addRequest(req);
				p_r.addRequest(req2);
				p_r.addRequest(req3);
				p_r.addRequest(req4);
				p_r.addRequest(req5);
				p_r.addRequest(req6);
				PCEPResponse pr=crm.newRequest(p_r);
				System.out.println("Respuesta "+pr.toString());
				}
			if (command.equals("34")) {
				PCEPRequest p_r = new PCEPRequest();
				System.out.println(" Synchronized Request!");
				Request req = createRequest("172.16.101.102", "172.16.102.104");
				Request req2 = createRequest("172.16.101.102", "172.16.102.104");
				SVECConstruct sveco=new SVECConstruct();
				
				Svec svec=new Svec();
				sveco.setSvec(svec);
				svec.setlDiverseBit(true);
				svec.addRequestID(req.getRequestParameters().getRequestID());
				svec.addRequestID(req2.getRequestParameters().getRequestID());
				p_r.addRequest(req);
				p_r.addRequest(req2);
				p_r.addSvec(sveco);
				PCEPResponse pr=crm.newRequest(p_r);
				System.out.println("Respuesta "+pr.toString());
				}
			if (command.equals("36")) {
				PCEPRequest p_r = new PCEPRequest();			
				System.out.println(" Synchronized Request with OF=7!");
				Request req = createRequest("172.16.101.102", "172.16.102.104");
				Request req2 = createRequest("172.16.101.102", "172.16.102.104");
				SVECConstruct sveco=new SVECConstruct();
				ObjectiveFunction objectiveFunction=new ObjectiveFunction();
				objectiveFunction.setOFcode(7);
				sveco.setObjectiveFunction(objectiveFunction);
				Svec svec=new Svec();
				sveco.setSvec(svec);
				svec.setlDiverseBit(true);
				svec.addRequestID(req.getRequestParameters().getRequestID());
				svec.addRequestID(req2.getRequestParameters().getRequestID());
				p_r.addRequest(req);
				p_r.addRequest(req2);
				p_r.addSvec(sveco);
				PCEPResponse pr=crm.newRequest(p_r);
				System.out.println("Respuesta "+pr.toString());
				}
			if (command.equals("35")) {
				PCEPRequest p_r = new PCEPRequest();
				SVECConstruct sveco=new SVECConstruct();

				System.out.println(" Synchronized Request very big!");
				Request req = createRequest("172.16.101.102", "172.16.102.104");
				Request req2 = createRequest("172.16.101.102", "172.16.102.104");
				Request req3 = createRequest("172.16.101.102", "172.16.102.104");
				Request req4 = createRequest("172.16.102.102", "172.16.102.104");
				Request req5 = createRequest("172.16.101.102", "172.16.102.104");
				Request req6 = createRequest("172.16.103.102", "172.16.102.102");
				Request req7 = createRequest("172.16.101.102", "172.16.102.104");
				Request req8 = createRequest("172.16.101.104", "172.16.102.103");
				Request req9 = createRequest("172.16.101.102", "172.16.102.104");
				Request req10 = createRequest("172.16.101.102", "172.16.102.104");
				Svec svec=new Svec();
				sveco.setSvec(svec);
				svec.setlDiverseBit(true);
				svec.addRequestID(req.getRequestParameters().getRequestID());
				svec.addRequestID(req2.getRequestParameters().getRequestID());
				svec.addRequestID(req3.getRequestParameters().getRequestID());
				svec.addRequestID(req4.getRequestParameters().getRequestID());
				svec.addRequestID(req5.getRequestParameters().getRequestID());
				svec.addRequestID(req6.getRequestParameters().getRequestID());
				svec.addRequestID(req7.getRequestParameters().getRequestID());
				svec.addRequestID(req8.getRequestParameters().getRequestID());
				svec.addRequestID(req9.getRequestParameters().getRequestID());
				svec.addRequestID(req10.getRequestParameters().getRequestID());
				
				p_r.addRequest(req);
				p_r.addRequest(req2);
				p_r.addRequest(req3);
				p_r.addRequest(req4);
				p_r.addRequest(req5);
				p_r.addRequest(req6);
				p_r.addRequest(req7);
				p_r.addRequest(req8);
				p_r.addRequest(req9);
				p_r.addRequest(req10);
				
				p_r.addSvec(sveco);
				PCEPResponse pr=crm.newRequest(p_r);
				System.out.println("Respuesta "+pr.toString());
				}
			if (command.equals("3")) {
				System.out.println(" Single Request!");
				PCEPRequest p_r = new PCEPRequest();
				Request req = createRequest("172.16.101.101", "172.16.103.103");
				p_r.addRequest(req);
				PCEPResponse pr=crm.newRequest(p_r);
				System.out.println("Respuesta "+pr.toString());
				}
			
			//////////////////////////////////////
			
			
			
			//////////////////////////////////////////////
			
			if (command.equals("3333")) {
				System.out.println(" Single Request!");
				PCEPRequest p_r = new PCEPRequest();
				Request req = createRequest("1.1.1.1", "127.16.101.102");
				p_r.addRequest(req);
				PCEPResponse pr=crm.newRequest(p_r);
				System.out.println("Respuesta "+pr.toString());
				}
			if (command.equals("32")) {
				System.out.println(" Single Request!");
				PCEPRequest p_r = new PCEPRequest();
				Request req = createRequest("1.1.1.1", "127.16.101.102");
				ObjectiveFunction of=new ObjectiveFunction();
				of.setOFcode(5);
				req.setObjectiveFunction(of);
				p_r.addRequest(req);
				PCEPResponse pr=crm.newRequest(p_r);
				System.out.println("Respuesta "+pr.toString());
				}
			if (command.equals("322")) {
				System.out.println(" Single Request with OF =5 and OF bit = 1!");
				PCEPRequest p_r = new PCEPRequest();
				Request req = createRequest("1.1.1.1", "127.16.101.102");
				//req.getRequestParameters().s
				ObjectiveFunction of=new ObjectiveFunction();
				of.setOFcode(5);
				of.setPbit(true);
				req.setObjectiveFunction(of);
				p_r.addRequest(req);
				PCEPResponse pr=crm.newRequest(p_r);
				System.out.println("Respuesta "+pr.toString());
				}
			if (command.equals("323")) {
				System.out.println(" Single Request with OF =10 and OF bit = 1!");
				PCEPRequest p_r = new PCEPRequest();
				Request req = createRequest("172.16.101.101", "172.16.102.104");
				//req.getRequestParameters().s
				ObjectiveFunction of=new ObjectiveFunction();
				of.setOFcode(10);
				of.setPbit(true);
				req.setObjectiveFunction(of);
				p_r.addRequest(req);
				PCEPResponse pr=crm.newRequest(p_r);
				System.out.println("Respuesta "+pr.toString());
				}
			if (command.equals("324")) {
				System.out.println(" Single Request with OF =10 and OF bit = 1!");
				PCEPRequest p_r = new PCEPRequest();
				Request req = createRequest("172.16.102.101", "172.16.103.103");
				//req.getRequestParameters().s
				ObjectiveFunction of=new ObjectiveFunction();
				of.setOFcode(10);
				of.setPbit(true);
				req.setObjectiveFunction(of);
				p_r.addRequest(req);
				PCEPResponse pr=crm.newRequest(p_r);
				System.out.println("Respuesta "+pr.toString());
				}
			if (command.equals("31")) {
				System.out.println(" Single Request with Metric!");
				PCEPRequest p_r = new PCEPRequest();
				Request req = createRequest("172.16.101.102", "172.16.102.102");
				Metric metric=new Metric();
				metric.setMetricType(3);
				metric.setMetricValue(0);
				metric.setBoundBit(false);
				req.getMetricList().add(metric);
				p_r.addRequest(req);
				PCEPResponse pr=crm.newRequest(p_r);
				System.out.println("Respuesta "+pr.toString());
				}

			
			if (command.equals("4")) {
				//System.out.println("Error Reason:");
				PCEPError perr=new PCEPError();
				//LinkedList<ErrorConstruct> errlist= new LinkedList<ErrorConstruct>
				LinkedList<PCEPErrorObject> errobjlist=new LinkedList<PCEPErrorObject>();
				PCEPErrorObject perrobj =new PCEPErrorObject();
				System.out.println("errorType: ");
				BufferedReader br2 = new BufferedReader(new InputStreamReader(System.in));
				try {
					int errorType = Integer.parseInt(br2.readLine());
					perrobj.setErrorType(errorType);
				} catch (IOException ioe) {
					System.out.println("IO error trying to read parameter");
					System.exit(1);
				}
				System.out.println("errorValue: ");
				try {
					int errorValue = Integer.parseInt(br2.readLine());
					perrobj.setErrorValue(errorValue);
				} catch (IOException ioe) {
					System.out.println("IO error trying to read parameter");
					System.exit(1);
				}
				errobjlist.add(perrobj);
				perr.setErrorObjList(errobjlist);
				
				
			}
			if (command.equals("5")) {
				ps.close(ObjectParameters.REASON_NOEXPLANATION);
				//end = true;
			}
			if (command.equals("6")) {
				ps.close(ObjectParameters.REASON_NOEXPLANATION);
				//end = true;
			}
			if (command.equals("7")) {
				try{
					ps.close(ObjectParameters.REASON_NOEXPLANATION);	
				}
				catch (Exception e){					
				}
				System.out.println("Have a nice day!");
				System.exit(1);	
								
				//end = true;
			}
			
			if (command.equals("8")) {
				PCEPRequest p_r = new PCEPRequest();
				Request req = new Request();
				p_r.addRequest(req);
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
				
				EndPointIPv4TLV sourceIPv4TLV=new EndPointIPv4TLV();
				EndPointIPv4TLV destIPv4TLV=new EndPointIPv4TLV();
				
				System.out.println(" - Source IP address: 1.1.1.1");
				Inet4Address sourceIPP;
				try {
					sourceIPP = (Inet4Address)Inet4Address.getByName ("1.1.1.1");
					sourceIPv4TLV.setIPv4address(sourceIPP);
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				System.out.println(" - Destination IP address: 172.16.101.102");
				Inet4Address destIPP;
				try {
					destIPP = (Inet4Address)Inet4Address.getByName ("172.16.101.102");
					destIPv4TLV.setIPv4address(destIPP);
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				EndPoint sourceEP=new EndPoint();
				EndPoint destEP=new EndPoint();
				sourceEP.setEndPointIPv4TLV(sourceIPv4TLV);
				destEP.setEndPointIPv4TLV(destIPv4TLV);
				
				P2PEndpoints p2pep=new P2PEndpoints();
				p2pep.setSourceEndPoints(sourceEP);
				p2pep.setDestinationEndPoints(destEP);
								
				GeneralizedEndPoints gep=new GeneralizedEndPoints();
				gep.setP2PEndpoints(p2pep);
				//EndPointsIPv4 ep=new EndPointsIPv4();				
				req.setEndPoints(gep);
				
				PCEPResponse pr=crm.newRequest(p_r);
				
			}
			
			if (command.equals("88")) {
				PCEPRequest p_r = new PCEPRequest();
				Request req = new Request();
				p_r.addRequest(req);
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
				
				EndPointIPv4TLV sourceIPv4TLV=new EndPointIPv4TLV();
				EndPointIPv4TLV destIPv4TLV=new EndPointIPv4TLV();
				
				System.out.println(" - Source IP address: 1.1.1.1");
				Inet4Address sourceIPP;
				try {
					sourceIPP = (Inet4Address)Inet4Address.getByName ("1.1.1.1");
					sourceIPv4TLV.setIPv4address(sourceIPP);
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				System.out.println(" - Destination IP address: 172.16.101.102");
				Inet4Address destIPP;
				try {
					destIPP = (Inet4Address)Inet4Address.getByName ("172.16.101.102");
					destIPv4TLV.setIPv4address(destIPP);
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				EndPoint sourceEP=new EndPoint();
				EndPoint destEP=new EndPoint();
				sourceEP.setEndPointIPv4TLV(sourceIPv4TLV);
				destEP.setEndPointIPv4TLV(destIPv4TLV);
				
				P2PEndpoints p2pep=new P2PEndpoints();
				p2pep.setSourceEndPoints(sourceEP);
				p2pep.setDestinationEndPoints(destEP);
								
				GeneralizedEndPoints gep=new GeneralizedEndPoints();
				gep.setP2PEndpoints(p2pep);
				//EndPointsIPv4 ep=new EndPointsIPv4();				
				req.setEndPoints(gep);
				
				PCEPResponse pr=crm.newRequest(p_r);
				
			}
			
		}
	}
	
	public Request createRequest(String src_ip, String dst_ip){
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
		return req;
	}

}
