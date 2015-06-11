package tid.pce.server.management;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.logging.Logger;

import es.tid.pce.pcep.PCEPProtocolViolationException;
import es.tid.pce.pcep.constructs.PCEPIntiatedLSP;
import es.tid.pce.pcep.constructs.Path;
import es.tid.pce.pcep.constructs.Request;
import es.tid.pce.pcep.constructs.SVECConstruct;
import es.tid.pce.pcep.constructs.StateReport;
import es.tid.pce.pcep.constructs.UpdateRequest;
import es.tid.pce.pcep.messages.PCEPInitiate;
import es.tid.pce.pcep.messages.PCEPReport;
import es.tid.pce.pcep.messages.PCEPRequest;
import es.tid.pce.pcep.messages.PCEPUpdate;
import es.tid.pce.pcep.objects.BandwidthRequested;
import es.tid.pce.pcep.objects.EndPointsIPv4;
import es.tid.pce.pcep.objects.ExplicitRouteObject;
import es.tid.pce.pcep.objects.LSP;
import es.tid.pce.pcep.objects.ObjectParameters;
import es.tid.pce.pcep.objects.ObjectiveFunction;
import es.tid.pce.pcep.objects.RequestParameters;
import es.tid.pce.pcep.objects.SRP;
import es.tid.pce.pcep.objects.XifiUniCastEndPoints;
import es.tid.pce.pcep.objects.tlvs.LSPDatabaseVersionTLV;
import es.tid.pce.pcep.objects.tlvs.IPv4LSPIdentifiersTLV;
import es.tid.pce.pcep.objects.tlvs.SymbolicPathNameTLV;
import es.tid.rsvp.objects.subobjects.IPv4prefixEROSubobject;
import es.tid.rsvp.objects.subobjects.UnnumberIfIDEROSubobject;
import es.tid.tedb.DomainTEDB;
import tid.pce.computingEngine.RequestDispatcher;
import tid.pce.computingEngine.RequestProcessorThread;
import tid.pce.computingEngine.algorithms.ComputingAlgorithmManager;
import tid.pce.server.DomainPCESession;
import tid.pce.server.PCEServerParameters;
import tid.pce.server.communicationpce.CollaborationPCESessionManager;
import tid.pce.server.wson.ReservationManager;
import tid.util.UtilsFunctions;

/**
 * Session to manage the PCE
 * @author ogondio
 *
 */
public class PCEManagementSession extends Thread {

	private boolean started = false;
	/**
	 * The socket of the management session
	 */
	private Socket socket;

	/**
	 * Logger
	 */
	private Logger log;

	/**
	 * The request Dispatcher
	 */
	private RequestDispatcher requestDispatcher;

	/**
	 * Output Stream of the managament session, to write the answers.
	 */
	private PrintStream out;

	/**
	 * The TEDB 
	 */
	private DomainTEDB tedb;

	/**
	 * The reservation manager. 
	 */
	private ReservationManager reservationManager;

	/**
	 * STRONGEST: Collaborative PCEs
	 */
	CollaborationPCESessionManager collaborationPCESessionManager;

	PCEServerParameters params;

	public static ArrayList<DomainPCESession> oneSession = new ArrayList<DomainPCESession>();

	public PCEManagementSession(Socket s,RequestDispatcher requestDispatcher, DomainTEDB tedb, ReservationManager reservationManager,CollaborationPCESessionManager collaborationPCESessionManager){
		this.socket=s;
		this.requestDispatcher=requestDispatcher;
		this.tedb=tedb;
		log=Logger.getLogger("PCEServer");
		this.reservationManager=reservationManager;
		this.collaborationPCESessionManager=collaborationPCESessionManager;
	}

	public PCEManagementSession(Socket s,RequestDispatcher requestDispatcher, DomainTEDB tedb, ReservationManager reservationManager,CollaborationPCESessionManager collaborationPCESessionManager,PCEServerParameters params){
		this.socket=s;
		this.requestDispatcher=requestDispatcher;
		this.tedb=tedb;
		log=Logger.getLogger("PCEServer");
		this.reservationManager=reservationManager;
		this.collaborationPCESessionManager=collaborationPCESessionManager;
		this.params = params;
	}

	public void run(){
		log.info("Starting Management session");
		boolean running=true;

		try {
			out=new PrintStream(socket.getOutputStream());
		} catch (IOException e) {
			log.warning("Management session cancelled: "+e.getMessage());
			return;
		}
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			while (running) {
				if (!started)
				{
					out.print("\n");
					out.print("   P               ,\r\n");
					out.print("   C               |'.             ,\r\n");
					out.print("   E               |  '-._        / )\r\n");
					out.print("                 .'  .._  ',     /_'-,\r\n");
					out.print("   C            '   /  _'.'_\\   /._)')\r\n");
					out.print("   O           :   /  '_' '_'  /  _.'\r\n");
					out.print("   N           |E |   |Q| |Q| /   /\r\n");
					out.print("   T          .'  _\\  '-' '-'    /\r\n");
					out.print("   R        .'--.(S     ,__` )  /\r\n");
					out.print("   O              '-.     _.'  /\r\n");
					out.print("   L            __.--'----(   /\r\n");
					out.print("   L        _.-'     :   __\\ /\r\n");
					out.print("   E       (      __.' :'  :Y\r\n");
					out.print("   R        '.   '._,  :   :|\r\n");
					out.print("              '.     ) :.__:|\r\n");
					out.print("                \\    \\______/\r\n");
					out.print("                 '._L/_H____]\r\n");
					out.print("                  /_        /\r\n");
					out.print("                 /  '-.__.-')\r\n");
					out.print("                :      /   /\r\n");
					out.print("                :     /   /\r\n");
					out.print("              ,/_____/----;\r\n");
					out.print("              '._____)----'\r\n");
					out.print("              /     /   /\r\n");
					out.print("             /     /   /\r\n");
					out.print("           .'     /    \\\r\n");
					out.print("          (______(-.____)\r\n");
					out.print("***********************************************\n");					
					started = true;
				}
				out.print("Available commands:\r\n\n");
				out.print("\t1) show parent pce\r\n");
				out.print("\t2) show algorithms list\r\n");
				out.print("\t3) show topology\r\n");
				out.print("\t4) queue size\r\n");
				out.print("\t5) res size\r\n");
				out.print("\t6) show reachability\r\n");
				out.print("\t7) show lsps\r\n");
				out.print("\t8) send FAKE initiate\r\n");
				out.print("\t9) send report\r\n");
				out.print("\t10) send update \r\n");
				out.print("\n\tENTER) quit\r\n");	


										out.print("PCE:>");
								String command = null;
								try {
									command = br.readLine();
								} catch (IOException ioe) {
									log.warning("IO error trying to read your command");
									return;
								}
								if(command == null)
								{
									continue;
								}
								if (command.equals("quit") || command.equals("")) {
									log.info("Ending Management Session");
									out.println("bye!");
									try {
										out.close();						
									} catch (Exception e){
										e.printStackTrace();
									}
									try {
										br.close();						
									} catch (Exception e){
										e.printStackTrace();
									}					
									return;
								}				
								if (command.equals("show parent pce") || command.equals("1")) {
									//					Enumeration<Inet4Address> pceIds= cprm.getDomainIdpceId().elements();
									//					Enumeration<Inet4Address> domains = cprm.getDomainIdpceId().keys();
									//					while (pceIds.hasMoreElements()){
									//						out.print("PCE Id: "+pceIds.nextElement()+ " Domain Id: "+domains.nextElement()+"\r\n");
									//					}
								} 
								else if (command.equals("show algorithms list")||command.equals("show algo list") || command.equals("2") ) {
									RequestProcessorThread[] threads=requestDispatcher.getThreads();
									String info="";										
									if (threads.length>0){
										Hashtable<Integer, ComputingAlgorithmManager> htcaSingle= threads[0].getSingleAlgorithmList();
										Enumeration<Integer> keys = htcaSingle.keys();
										while (keys.hasMoreElements()){
											Integer inte=keys.nextElement();
											info=info+"OF ="+inte+"; ";
										}		
										Hashtable<Integer, ComputingAlgorithmManager> htcaSvec= threads[0].getSvecAlgorithmList();
										Enumeration<Integer> keys2 = htcaSvec.keys();
										while (keys2.hasMoreElements()){
											Integer inte=keys2.nextElement();
											info=info+"OF ="+inte+"and SVEC; ";
										}			
									}
									out.print(info+"\r\n");
								}
								else if (command.equals("show topology") || command.equals("3")){
									//Print intradomain and interDomain links
									out.print(tedb.printTopology());


								}else if (command.equals("queue size") || command.equals("4")){
									out.println("num pets "+requestDispatcher.queueSize());
									out.println("num petsR "+requestDispatcher.retryQueueSize());

								}else if (command.equals("res size") || command.equals("5")){
									out.println("num perm res "+reservationManager.getReservationQueueSize());

								}else if (command.equals("show reachability") || command.equals("6")){
									//tedb.getDomainReachabilityIPv4Prefix();
									out.println( tedb.getReachabilityEntry().getPrefix());

								}
								else if (command.equals("show lsps") || command.equals("7")){
									out.println("Enjoy watching the LSPs the PCE has in his database");

								/*	Hashtable<LSPKey, LSPTEInfo> LSPTEList = ((SimpleLSP_DB)params.getLspDB()).getLSPTEList();
									Enumeration<LSPKey> enumKey = LSPTEList.keys();
									Integer j = 0;
									while(enumKey.hasMoreElements()) 
									{
										LSPKey key = enumKey.nextElement();
										LSPTEInfo val = LSPTEList.get(key);
										String address = val.pcepReport.getStateReportList().get(0).getLSP().getLspIdentifiers_tlv().getTunnelSenderIPAddress().toString();
										out.println(" "+j+") "+"Orig:"+address+",ERO: "+val.pcepReport.getStateReportList().get(0).getPath().geteRO().toString());
										j++;
									}*/

								}
								else if (command.equals("send initiate") || command.equals("8")){
									out.println("Sending between 192.168.1.1 and 192.168.1.4");
									//out.println("Choose origin IP:");

									//String string_ip_source = requestInput(br);

									//out.println("Choose dest IP:");

									//String string_ip_dest = requestInput(br);
									String string_ip_source = "192.168.1.1";
									String string_ip_dest = "192.168.1.3";

									Inet4Address ip_source = (Inet4Address)InetAddress.getByName(string_ip_source);
									Inet4Address ip_dest = (Inet4Address)InetAddress.getByName(string_ip_dest);

									PCEPInitiate pceInit = new PCEPInitiate();
									PCEPIntiatedLSP pcepIntiatedLSPList = new PCEPIntiatedLSP();
									
									
									SRP rsp = new SRP();
									pcepIntiatedLSPList.setRsp(rsp);
									
									LSP lsp = new LSP();
									lsp.setLspId(0);
									pcepIntiatedLSPList.setLsp(lsp);
									
									EndPointsIPv4 endP_IP = new EndPointsIPv4();
									endP_IP.setSourceIP(ip_source);
									endP_IP.setDestIP(ip_dest);
									pcepIntiatedLSPList.setEndPoint(endP_IP);
									
									
									ExplicitRouteObject ero = new ExplicitRouteObject();									
									/*IPv4prefixEROSubobject eroso1 = new IPv4prefixEROSubobject();
									IPv4prefixEROSubobject eroso2 = new IPv4prefixEROSubobject();
									
									eroso1.setIpv4address(ip_source);
									eroso2.setIpv4address(ip_dest);									
									
									ero.addEROSubobject(eroso1);
									ero.addEROSubobject(eroso2);*/
									UnnumberIfIDEROSubobject eroso1 = new UnnumberIfIDEROSubobject();
									eroso1.setInterfaceID(1);
									eroso1.setRouterID(ip_source);
									
									UnnumberIfIDEROSubobject eroso2 = new UnnumberIfIDEROSubobject();
									eroso2.setInterfaceID(3);
									eroso2.setRouterID(ip_dest);
									

									IPv4prefixEROSubobject eroso3 = new IPv4prefixEROSubobject();
									eroso3.setIpv4address((Inet4Address)InetAddress.getByName("192.168.1.4"));
									eroso3.setPrefix(32);
									
									ero.addEROSubobject(eroso1);
									ero.addEROSubobject(eroso2);
									ero.addEROSubobject(eroso3);
									
									pcepIntiatedLSPList.setEro(ero);
									
									pceInit.getPcepIntiatedLSPList().add(pcepIntiatedLSPList);	
									

									

									


									//					ExplicitRouteObject ero;
									//					ero = new ExplicitRouteObject();

/**********************SR ERO INITIATE
 * 
									int sid_source = 1001;
									int sid_dest = 1003; 
 									LSP lsp = new LSP();
									SRP rsp = new SRP();
									PathSetupTLV pstlv = new PathSetupTLV();
									pstlv.setPST(PathSetupTLV.SR);
									rsp.setPathSetupTLV(pstlv);
									SRERO srero = new SRERO();
									SREROSubobject srero1 = new SREROSubobject();
									srero1.setSID(sid_source);
									srero1.setfFlag(true);

									srero.addSREROSubobject(srero1);

									SREROSubobject srero2 = new SREROSubobject();
									srero2.setSID(sid_dest);
									srero2.setfFlag(true);

									srero.addSREROSubobject(srero2);




									PCEPIntiatedLSP pcepIntiatedLSPList = new PCEPIntiatedLSP();
									pcepIntiatedLSPList.setSrero(srero);
									pcepIntiatedLSPList.setRsp(rsp);
									pcepIntiatedLSPList.setLsp(lsp);
									pceInit.getPcepIntiatedLSPList().add(pcepIntiatedLSPList);						

									EndPointsIPv4 endP_IP = new EndPointsIPv4();
									endP_IP.setSourceIP(ip_source);
									endP_IP.setDestIP(ip_dest);
									
									*/
									//					
									//					pcepIntiatedLSPList.setEndPoint(endP_IP);


									/**************************************

					ExplicitRouteObject ero;
					ero = new ExplicitRouteObject();
					SREROSubobject srero1 = new SREROSubobject();
					srero1.setSID(sid_source);
					srero1.setfFlag(true);

					ero.addEROSubobject(srero1);

					SREROSubobject srero2 = new SREROSubobject();
					srero2.setSID(sid_dest);
					srero2.setfFlag(true);

					ero.addEROSubobject(srero2);					
					PCEPIntiatedLSP pcepIntiatedLSPList = new PCEPIntiatedLSP();
					pcepIntiatedLSPList.setEro(ero);
					pcepIntiatedLSPList.setLsp(lsp);
					//pcepIntiatedLSPList.setEndPoint(endP);
					pceInit.getPcepIntiatedLSPList().add(pcepIntiatedLSPList);						

					EndPointsIPv4 endP_IP = new EndPointsIPv4();
					endP_IP.setSourceIP(ip_source);
					endP_IP.setDestIP(ip_dest);
//					
//					pcepIntiatedLSPList.setEndPoint(endP_IP);					


									 */
									/*
									try 
									{	
										//Sending message to tn1
										Socket clientSocket = new Socket(endP_IP.getSourceIP(), 2222);			
										log.info("Socket opened");	
										//SENDING PCEP MESSAGE
										DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
										try 
										{
											log.info("Encoding pce");
											pceInit.encode();
											log.info("Sending message At LAST!");
											outToServer.write(pceInit.getBytes());
											outToServer.flush();
										} 
										catch (Exception e) 
										{
											log.info("woops");
											log.info(UtilsFunctions.exceptionToString(e));
										}
									}
									catch (IOException e) 
									{
										log.info(UtilsFunctions.exceptionToString(e));
										log.severe("Couldn't get I/O for connection to port" + 2222);
									}
								*/
									oneSession.get(0).sendPCEPMessage(pceInit);

								}
								else if (command.equals("send report")){


									log.info("Sending Repoooort");
									log.info("Sending to :"+oneSession.get(0).getSocket().getInetAddress());

									PCEPReport rpt= new PCEPReport();
									rpt.setStateReportList(new LinkedList<StateReport>());
									rpt.getStateReportList().add(new StateReport());
									rpt.getStateReportList().get(0).setLSP(new LSP());
									rpt.getStateReportList().get(0).setSRP(new SRP());
									rpt.getStateReportList().get(0).setPath(new Path());
									rpt.getStateReportList().get(0).getPath().seteRO(new ExplicitRouteObject());

									try {
										rpt.encode();

										oneSession.get(0).sendPCEPMessage(rpt);

									} catch (PCEPProtocolViolationException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
								}
								/*else if (command.equals("send update")  || command.equals("9")){
									out.println("Choose an available IP to send the update");
									for (int i = 0; i < oneSession.size(); i++)
									{
										out.println(" "+i+") "+oneSession.get(i).getSocket().getInetAddress());
									}

									String number = requestInput(br);

									String sendAddress = oneSession.get(Integer.parseInt(number)).getSocket().getInetAddress().toString();

									out.println("Choose LSP you want to remove:");

									out.println("-1) I don't want to remove anything, I want to do another things");

									Hashtable<LSPKey, LSPTEInfo> LSPTEList = ((SimpleLSP_DB)params.getLspDB()).getLSPTEList();

									Enumeration<LSPKey> enumKey = LSPTEList.keys();
									Integer j = 0;
									while(enumKey.hasMoreElements()) 
									{
										LSPKey key = enumKey.nextElement();
										LSPTEInfo val = LSPTEList.get(key);
										String address = val.pcepReport.getStateReportList().get(0).getLSP().getLspIdentifiers_tlv().getTunnelSenderIPAddress().toString();
										if (sendAddress.equals(address))
										{
											out.println(" "+j+") "+"Orig:"+address+",ERO: "+val.pcepReport.getStateReportList().get(0).getPath().geteRO().toString());
											j++;
										}
									}

									String numberSecond = requestInput(br);
									if (numberSecond.equals("-1"))
									{
										out.println("Choose LSP you want to modify:");
										enumKey = LSPTEList.keys();
										j = 0;
										while(enumKey.hasMoreElements()) 
										{
											LSPKey key = enumKey.nextElement();
											LSPTEInfo val = LSPTEList.get(key);
											String address = val.pcepReport.getStateReportList().get(0).getLSP().getLspIdentifiers_tlv().getTunnelSenderIPAddress().toString();
											if (sendAddress.equals(address))
											{
												out.println(" "+j+") "+"Orig:"+address+",ERO: "+val.pcepReport.getStateReportList().get(0).getPath().geteRO().toString());
												j++;
											}
										}
										numberSecond = requestInput(br);
										out.println("OK, choose new bandwith:");

										float bw = Float.parseFloat(requestInput(br));

										j = 0;
										enumKey = LSPTEList.keys();
										while(enumKey.hasMoreElements())
										{
											LSPKey key = enumKey.nextElement();
											LSPTEInfo val = LSPTEList.get(key);
											String address = val.pcepReport.getStateReportList().get(0).getLSP().getLspIdentifiers_tlv().getTunnelSenderIPAddress().toString();
											if (sendAddress.equals(address))
											{
												if(numberSecond.equals(j.toString()))
												{
													Bandwidth bwObject = new Bandwidth();
													bwObject.setBw(bw);
													val.pcepReport.getStateReportList().get(0).getPath().setBandwidth(bwObject);
													easySendUpdate(val,oneSession.get(Integer.parseInt(number)));
												}
												j++;
											}
										}
										out.println("");
										out.println("Yeah.., you can only change the bandwith");
										out.println("");

									}
									else
									{
										j = 0;
										enumKey = LSPTEList.keys();
										while(enumKey.hasMoreElements())
										{
											LSPKey key = enumKey.nextElement();
											LSPTEInfo val = LSPTEList.get(key);
											String address = val.pcepReport.getStateReportList().get(0).getLSP().getLspIdentifiers_tlv().getTunnelSenderIPAddress().toString();
											if (sendAddress.equals(address))
											{
												if(numberSecond.equals(j.toString()))
												{
													val.pcepReport.getStateReportList().get(0).getLSP().setrFlag(true);
													easySendUpdate(val,oneSession.get(Integer.parseInt(number)));
												}
												j++;
											}
										}
									}
									
								}
								*/
								else if (command.equals("send wlan") || command.equals("10")){

									PCEPRequest pReq = new PCEPRequest();
									LinkedList<SVECConstruct> svecList = new LinkedList<SVECConstruct>();
									pReq.setSvecList(svecList);

									Request req = new Request();

									RequestParameters reqParams = new RequestParameters();
									reqParams.setBidirect(false);
									reqParams.setPrio(1);
									reqParams.setRequestID(1);

									req.setRequestParameters(reqParams);

									XifiUniCastEndPoints endP = new XifiUniCastEndPoints();
									endP.setSwitchSourceID("00:14:2c:59:e5:5e");
									endP.setSwitchDestinationID("00:14:2c:59:e5:66");

									req.setEndPoints(endP);

									ObjectiveFunction obFunc = new ObjectiveFunction();
									obFunc.setOFcode(1003);

									req.setObjectiveFunction(obFunc);

									LinkedList<Request> reqList = new LinkedList<Request>();
									reqList.add(req);


									Socket socket = new Socket("localhost", 4444);

									pReq.setRequestList(reqList);

									requestDispatcher.dispathRequests(pReq,new DataOutputStream(socket.getOutputStream()));

									out.print("PCEPRequest sent to dispatcher!\r\n");

								}
								else if (command.equals("help")){
									out.print("1) show parent pce\r\n");
									out.print("2) show algorithms list\r\n");
									out.print("3) show topology\r\n");
									out.print("4) queue size\r\n");
									out.print("5) res size\r\n");
									out.print("6) show reachability\r\n");
									out.print("7) show lsps\r\n");
									out.print("8) send initiate\r\n");
									out.print("9) send report\r\n");
									out.print("10) send update \r\n");
									out.print("0) quit\r\n");				

								}
								/*	else if (command.equals("add xifi link")) {
					out.print("Format:switch_id_1-switch_id_2-source_port-dest_port\r\n");
					log.info("Adding Xifi Link!");
					String line;
					line = br.readLine();
					String[] parts = line.split("-");

					RouterInfoPM source = new RouterInfoPM();
					source.setRouterID(parts[0]);

					RouterInfoPM dest = new RouterInfoPM();
					dest.setRouterID(parts[1]);

					IntraDomainEdge edge= new IntraDomainEdge();
					out.print(parts[0]+"\r\n");
					out.print(parts[1]+"\r\n");
					out.print(parts[2]+"\r\n");
					out.print(parts[3]+"\r\n");
					edge.setSrc_if_id(Long.parseLong(parts[2]));
					edge.setDst_if_id(Long.parseLong(parts[3]));

					TE_Information tE_info = new TE_Information();
					tE_info.setNumberWLANs(15);
					tE_info.initWLANs();

					edge.setTE_info(tE_info);

					((SimpleTEDB)tedb).getNetworkGraph().addEdge(source, dest, edge);
					((SimpleTEDB)tedb).notifyNewEdge(source, dest);

				} 
				else if (command.equals("set traces on")) {
					log.setLevel(Level.ALL);		
					Logger log2=Logger.getLogger("PCEPParser");
					log2.setLevel(Level.ALL);
					Logger log3= Logger.getLogger("OSPFParser");
					log3.setLevel(Level.ALL);
					out.print("traces on!\r\n");
				} 
				else if (command.equals("set traces off")) {
					log.setLevel(Level.SEVERE);		
					Logger log2=Logger.getLogger("PCEPParser");
					log2.setLevel(Level.SEVERE);
					Logger log3= Logger.getLogger("OSPFParser");
					log3.setLevel(Level.SEVERE);
					out.print("traces off!\r\n");
				} 
				else if (command.equals("change topology")){
					//TODO: no estoy segura de esto
					out.print("Introduce the name of the file where is the tology you want to change:\r\n");	
					String file;
					file = br.readLine();
					//TODO: Comprobar que el file no exista?
					tedb.initializeFromFile(file);					
				}
				else if (command.equals("show interDomain links")){
					out.print(tedb.printInterDomainLinks());
				}else if (command.equals("stats")){
					out.println("procTime "+requestDispatcher.getThreads()[0].getProcTime().result());
					out.println("maxTime "+requestDispatcher.getThreads()[0].getMaxProcTime());
					out.println("idleTime "+requestDispatcher.getThreads()[0].getIdleTime().result());

				}
				else if (command.equals("show sessions")){
					if (collaborationPCESessionManager==null){

					}else if (collaborationPCESessionManager.getOpenedSessionsManager() == null){
					}
					else{
					out.println("Showing "+ collaborationPCESessionManager.getOpenedSessionsManager().getSessionInfoList().size() +" sessions...");

					if (collaborationPCESessionManager.getOpenedSessionsManager().getSessionInfoList().size()==0){
						out.println("No hay ninguna sesion abierta todavia");
					}else {
						int size=collaborationPCESessionManager.getOpenedSessionsManager().getSessionInfoList().size();
						int roll=10;
						for (int i=0;i<size ;i++){
							roll=collaborationPCESessionManager.getOpenedSessionsManager().getSessionInfoList().get(i).getRollSession();
							switch (roll){
							case RollSessionType.PCC:
								out.println("Session de PCC");
								break;
							case RollSessionType.COLLABORATIVE_PCE:
								out.println("Session de PCE BACKUP");
								break;
							case RollSessionType.PCE_PARENT:
								out.println("Session de PCE PARENT");
								break;
							}

						}
					}
					}
				}*/

								else{
									out.print("invalid command\n");	
									out.print("\n");
								}

			}
		} catch (Exception e) {
			log.info(UtilsFunctions.exceptionToString(e));
			return;
		}
	}

	private void easySendUpdate(LSPTEInfo val, DomainPCESession dm) throws UnknownHostException
	{
		PCEPUpdate update = new PCEPUpdate();
		for (int i = 0 ; i < val.pcepReport.getStateReportList().size(); i++)
		{
			StateReport report = val.pcepReport.getStateReportList().get(i);

			update.getUpdateRequestList().add(new UpdateRequest());
			update.getUpdateRequestList().get(i).setLSP(report.getLSP());
			update.getUpdateRequestList().get(i).setRSP(report.getSRP());
			update.getUpdateRequestList().get(i).setPath(report.getPath());
		}
		dm.sendPCEPMessage(update);
		out.println("Update message sent!");
	}


	private void sendUpdate(LSPTEInfo val) throws UnknownHostException
	{
		SRP rsp = new SRP();
		rsp.setSRP_ID_number(1);

		SymbolicPathNameTLV symPathName= new SymbolicPathNameTLV();

		symPathName.setSymbolicPathNameID(ObjectParameters.redundancyID);
		rsp.setSymPathName(symPathName);

		//tedb.getDomainReachabilityIPv4Prefix();
		PCEPUpdate m_update = new PCEPUpdate();
		UpdateRequest state_report = new UpdateRequest();
		LSP lsp = new LSP();
		//Delegate the LSP
		lsp.setdFlag(true);
		//No sync
		lsp.setsFlag(false);
		//Is LSP operational?
		lsp.setOpFlags(ObjectParameters.LSP_OPERATIONAL_UP);

		lsp.setLspId(1);


		IPv4LSPIdentifiersTLV lspIdTLV = new IPv4LSPIdentifiersTLV();
		//lspIdTLV.setLspID((lspte.getIdLSP().intValue()));
		lspIdTLV.setTunnelID(1234);
		lspIdTLV.setTunnelSenderIPAddress((Inet4Address)Inet4Address.getLocalHost());
		//FIXME    	 
		//lspIdTLV.setExtendedTunnelID(extendedTunnelID);

		lsp.setLspIdentifiers_tlv(lspIdTLV);


		SymbolicPathNameTLV symbPathName = new SymbolicPathNameTLV();
		/*This id should be unique within the PCC*/
		symbPathName.setSymbolicPathNameID(ByteBuffer.allocate(8).putLong(lsp.getLspId()).array());
		lsp.setSymbolicPathNameTLV_tlv(symbPathName);



		LSPDatabaseVersionTLV lspdDTLV = new LSPDatabaseVersionTLV();
		/*A change has been made so the database version is aumented*/
		lspdDTLV.setLSPStateDBVersion(2);

		state_report.setLSP(lsp);
		state_report.setRSP(rsp);

		//Do the Path thing well.

		Path path = new Path();

		ExplicitRouteObject auxERO = new ExplicitRouteObject();

		path.seteRO(auxERO);

		BandwidthRequested bw = new BandwidthRequested();

		bw.setBw(1);

		path.setBandwidth(bw);

		/*
		LinkedList<Metric> metricList = new LinkedList<Metric>();
		Metric metric = new Metric();
		metric.setPbit(false);
		metric.setBoundBit(false); 
		 */

		state_report.setPath(path);
		m_update.addStateReport(state_report);	
		out.println("Sending First PCEPUpdate message");

		//oneSession.sendPCEPMessage(m_update);
	}
	
	public class LSPTEInfo
	{
		public PCEPReport pcepReport;
		LSPTEInfo( PCEPReport pcepReport)
		{
			this.pcepReport = pcepReport;
		}
	}
	
	
	
}