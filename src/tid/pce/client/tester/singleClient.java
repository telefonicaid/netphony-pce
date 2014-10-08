package tid.pce.client.tester;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Timer;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import es.tid.ospf.ospfv2.OSPFv2LinkStateUpdatePacket;
import es.tid.pce.pcep.PCEPProtocolViolationException;
import es.tid.pce.pcep.constructs.Path;
import es.tid.pce.pcep.constructs.Request;
import es.tid.pce.pcep.messages.PCEPRequest;
import es.tid.pce.pcep.messages.PCEPResponse;
import es.tid.pce.pcep.messages.PCEPTELinkSuggestion;
import es.tid.pce.pcep.objects.BandwidthRequested;
import es.tid.pce.pcep.objects.EndPointsIPv4;
import es.tid.pce.pcep.objects.ExplicitRouteObject;
import es.tid.pce.pcep.objects.Metric;
import es.tid.pce.pcep.objects.ObjectParameters;
import es.tid.pce.pcep.objects.ObjectiveFunction;
import es.tid.pce.pcep.objects.RequestParameters;
import es.tid.pce.pcep.objects.Reservation;
import es.tid.rsvp.objects.subobjects.EROSubobject;
import es.tid.rsvp.objects.subobjects.IPv4prefixEROSubobject;
import es.tid.rsvp.objects.subobjects.SubObjectValues;
import tid.netManager.NetworkLSPManager;
import tid.netManager.NetworkLSPManagerParameters;
import tid.netManager.OSPFSender;
import tid.netManager.TCPOSPFSender;
import tid.netManager.emulated.AdvancedEmulatedNetworkLSPManager;
import tid.netManager.emulated.CompletedEmulatedNetworkLSPManager;
import tid.netManager.emulated.DummyEmulatedNetworkLSPManager;
import tid.netManager.emulated.SimpleEmulatedNetworkLSPManager;
import tid.pce.client.PCCPCEPSession;
import tid.pce.client.multiLayer.RealiseMLCapacityTask;
import tid.pce.pcepsession.PCEPSessionsInformation;
import tid.vntm.VNTMParameters;

public class singleClient {

	private static Hashtable<Integer,PCCPCEPSession> PCEsessionList;
	private static Logger log=Logger.getLogger("PCCClient");
	private static Logger log2=Logger.getLogger("PCEPParser");
	private static Logger log3=Logger.getLogger("OSPFParser");
	private static String networkEmulatorFile="NetworkEmulatorConfiguration.xml";
	private static InformationRequest testerParams;
	private static PCCPCEPSession VNTMSession;
	static long id=1234;
	/*Variable used for counter how many requests there are*/
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		/*If there are arguments, read the PCEServerPort and ipPCE*/
		  if (args.length < 1) {
			log.info("Usage: ClientTester <XML File>");
			return;
		}
		//Initialize loggers
			FileHandler fh;
			FileHandler fh2;
			FileHandler fh3;
			try {
				fh=new FileHandler("PCCClient.log");
				fh2=new FileHandler("PCEPClientParser.log");
				fh3=new FileHandler("OSPFParser.log");
				//fh.setFormatter(new SimpleFormatter());
				//fh2.setFormatter(new SimpleFormatter());
				log.addHandler(fh);
				log.setLevel(Level.ALL);			
				log2.addHandler(fh2);
				log2.setLevel(Level.ALL);
				log3.setLevel(Level.ALL);
				log3.addHandler(fh3);
			} catch (Exception e1) {
				e1.printStackTrace();
				System.exit(1);
			}
			long requestID=1234;

			testerParams = new InformationRequest();
			testerParams.readFile(args[0]);
			PCEsessionList= new Hashtable<Integer,PCCPCEPSession>();
			for (int i =0;i<testerParams.getPCCPCEPsessionParams().getNumSessions();i++){
				PCCPCEPSession PCEsession = new PCCPCEPSession(testerParams.getPCCPCEPsessionParams().getIpPCEList().get(i), testerParams.getPCCPCEPsessionParams().getPCEServerPortList().get(i), testerParams.getPCCPCEPsessionParams().isNoDelay(), new PCEPSessionsInformation());
				PCEsessionList.put(i, PCEsession);
				PCEsession.start();
			}
			
			NetworkLSPManager networkLSPManager = null;
			
			if (testerParams.isNetworkEmulator()){
				networkLSPManager = createNetworkLSPManager();
			}
			else if (testerParams.isVNTMSession()){
				VNTMSession = createVNTMSession();
			}
		
			
			for (int i =0; i<testerParams.getRequestToSendList().size();i++){
				PCEPRequest request=createRequestMessage(i);
				//Dormir para que no se lancen las peticiones a la vez
				try {
					Thread.sleep(3500);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				//Measure initial time
				PCEPResponse response;
				request.getRequestList().getFirst().getRequestParameters().setRequestID(requestID);
				requestID=requestID+1;
				//Count one request

				//FIXME: COGER POSIBLES FALLOS EN EL REQUEST
				try{
					if (testerParams.getMaxTimeWaitingForResponse_ms() != -1)
						response=PCEsessionList.get(0).crm.newRequest(request,testerParams.getMaxTimeWaitingForResponse_ms());
					else
						response=PCEsessionList.get(0).crm.newRequest(request);

					log.info("Respuesta "+response.toString());
					if (testerParams.isNetworkEmulator()){			
						handleResponse( request,response,networkLSPManager);
						
					}
					else{
						handleResponse(request,response,VNTMSession);
				
				
			
		
			
//			else{
//			//Abro una sesion con el VNTM
//			  PCEsessionVNTM = new PCCPCEPSession(testerParams.getIpVNTM(), 4000,false);
//			  PCEsessionVNTM.start();	
//			}
	      }
				}		catch(Exception e){
					e.printStackTrace();
					System.exit(1);
					
				}
			}
			try {
				Thread.sleep(94567);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}

	/**
	 * Create a PC Request message including Monitoring, PCC-Id-Req and Request
	 * @param i index of the request  
	 * @return
	 */
	static PCEPRequest createRequestMessage(int i){
		PCEPRequest p_r = new PCEPRequest();
		//Creamos el objecto monitoring
		//Monitoring monitoring=createMonitoring();
		//Creamos el objeto PCCIdReq
		//PccReqId pccReqId = createPccReqId();
		//Creamos el object Request 
		Request req = createRequest(i);
		if (testerParams.getRequestToSendList().get(i).getRequestParameters().isDelayMetric()){
		     Metric metric = new Metric();
		     metric.setMetricType(ObjectParameters.PCEP_METRIC_TYPE_LATENCY_METRIC);
		     metric.setComputedMetricBit(true);
		     req.getMetricList().add(metric);
		   
		  }
		  if (testerParams.getRequestToSendList().get(i).getRequestParameters().isOf()){
		   ObjectiveFunction of = new ObjectiveFunction();
		   req.setObjectiveFunction(of);
		   of.setOFcode(testerParams.getRequestToSendList().get(i).getRequestParameters().getOfCode()); 
		  }

		  if (testerParams.getRequestToSendList().get(i).getRequestParameters().isReservation()){
		   Reservation res= new Reservation();
		   req.setReservation(res);
		   res.setTimer(testerParams.getRequestToSendList().get(i).getRequestParameters().getTimeReserved());
		  }
		  if (testerParams.getRequestToSendList().get(i).getRequestParameters().Is_bandwidth()){
			   BandwidthRequested bw= new BandwidthRequested();
			   bw.setBw(testerParams.getRequestToSendList().get(i).getRequestParameters().getBW());
			   req.setBandwidth(bw);
			  }
		//p_r.setMonitoring(monitoring);
		//p_r.setPccReqId(pccReqId);
		p_r.addRequest(req);
		return p_r;
	}
	/**
	 * Create a request object
	 * @param src_ip
	 * @param dst_ip
	 * @return
	 */
	private static Request createRequest(int i){
		Request req = new Request();
		//RequestParameters
		RequestParameters rp= new RequestParameters();
		rp.setPbit(true);				
		rp.setRequestID(PCCPCEPSession.getNewReqIDCounter());		
		rp.setPrio(testerParams.getRequestToSendList().get(i).getRequestParameters().getPriority());		
		rp.setReopt(testerParams.getRequestToSendList().get(i).getRequestParameters().isReoptimization());	
		rp.setBidirect(testerParams.getRequestToSendList().get(i).getRequestParameters().isBidirectional());
		rp.setLoose(testerParams.getRequestToSendList().get(i).getRequestParameters().isLoose());
		req.setRequestParameters(rp);
		//EndPoints
		EndPointsIPv4 ep=new EndPointsIPv4();				
		req.setEndPoints(ep);
		ep.setSourceIP(testerParams.getRequestToSendList().get(i).getSource());	
		ep.setDestIP(testerParams.getRequestToSendList().get(i).getDestiny());
		
		return req;
	}

	 /**
	  * Create a Simple, advanced or complicated NetworkLSPManager 
	  * @param networkEmulatorParams 
	  * @return
	  */
	static NetworkLSPManager createNetworkLSPManager(){
		LinkedBlockingQueue<OSPFv2LinkStateUpdatePacket>  sendingQueue=null;
		NetworkLSPManagerParameters networkEmulatorParams= new NetworkLSPManagerParameters();
		networkEmulatorParams.initialize(networkEmulatorFile);
		NetworkLSPManager networkLSPManager=null;
		
		if (networkEmulatorParams.isOSPF_RAW_SOCKET()){
			OSPFSender ospfsender = new OSPFSender( networkEmulatorParams.getPCETEDBAddressList() , networkEmulatorParams.getAddress());
			ospfsender.start();	
			sendingQueue=ospfsender.getSendingQueue();
		}
		else {
			TCPOSPFSender TCPOSPFsender = new TCPOSPFSender(networkEmulatorParams.getPCETEDBAddressList(),networkEmulatorParams.getOSPF_TCP_PORTList());
			TCPOSPFsender.start();
			sendingQueue=TCPOSPFsender.getSendingQueue();
		}

		if (networkEmulatorParams.getNetworkLSPtype().equals("Simple")){
			networkLSPManager = new SimpleEmulatedNetworkLSPManager(sendingQueue, networkEmulatorParams.getNetworkFile() );
			
		} else if (networkEmulatorParams.getNetworkLSPtype().equals("Advanced")){
			networkLSPManager= new AdvancedEmulatedNetworkLSPManager(sendingQueue, networkEmulatorParams.getNetworkFile() );
		}
		else if (networkEmulatorParams.getNetworkLSPtype().equals("Completed")){
			networkLSPManager = new CompletedEmulatedNetworkLSPManager(sendingQueue, networkEmulatorParams.getNetworkFile(), null,networkEmulatorParams.isMultilayer() );
		}
		else if (networkEmulatorParams.getNetworkLSPtype().equals("dummy")){
			networkLSPManager = new DummyEmulatedNetworkLSPManager();
		}
		return networkLSPManager;
	}
	
	 /**
	  * Create the VNTM Session 
	  * @param networkEmulatorParams 
	  * @return
	  */
	static PCCPCEPSession createVNTMSession(){
		VNTMParameters VNTMParams = new VNTMParameters();
		VNTMParams.initialize(testerParams.getVNTMFile());
		PCCPCEPSession PCEsessionVNTM = new PCCPCEPSession(VNTMParams.getVNTMAddress(),VNTMParams.getVNTMPort(),false, new PCEPSessionsInformation());
		PCEsessionVNTM.start();
		return PCEsessionVNTM;
	}
	

static int createEROList(LinkedList<EROSubobject> eroSubObjList,LinkedList<ExplicitRouteObject> eroList,ArrayList<String> sourceList,ArrayList<String> destinationList ){
	boolean layerInfoFound=false;
	int numNewLinks=0;
	int counterArray=0;
	sourceList = new ArrayList<String>();
	destinationList = new ArrayList<String>();
	//cREAR LISTA DE EROS
	LinkedList<EROSubobject> eroSubObjList2=null;
	String strPrev=null;
	for (int i=0;i<eroSubObjList.size();++i){
		if ((eroSubObjList.get(i)).getType()==SubObjectValues.ERO_SUBOBJECT_LAYER_INFO){
			if (layerInfoFound==false){
				layerInfoFound=true;							
				//Create a new ERO to add at the list
				ExplicitRouteObject newERO =  new ExplicitRouteObject();							
				eroSubObjList2=newERO.getEROSubobjectList();	
				eroSubObjList2.add(eroSubObjList.get(i-1));							
				eroList.add(newERO);
				numNewLinks=numNewLinks+1;
											
			}else {
				log.info("Acabo pongo layerInfoEnded a true");
				layerInfoFound=false;
				eroSubObjList2.add(eroSubObjList.get(i+1));
			}
		}
		else if (layerInfoFound==true){
			eroSubObjList2.add(eroSubObjList.get(i));
		}else {		
			String str1=((IPv4prefixEROSubobject)(eroSubObjList.get(i))).getIpv4address().toString().substring(1);
			if (strPrev==null){
				strPrev=str1;
			}else {
				sourceList.add(counterArray, strPrev);							
				destinationList.add(counterArray, str1);
				strPrev=str1;
				counterArray++;
			}															
		}
	}
	return numNewLinks;
}

static void handleResponse(PCEPRequest request,PCEPResponse response,NetworkLSPManager networkLSPManager ){
	if (response == null){
		log.warning("Response null");			
		return;
	}
	if (response.getResponseList().isEmpty()){
		log.severe("ERROR in response");
		//FIXME: QUE HACEMOS? CANCELAMOS SIMULACION?
		//stats.addNoPathResponse();
		System.exit(1);

	}else {

		if (response.getResponseList().get(0).getNoPath()!=null){
			log.info("NO PATH");
			return;	
		}else{
			Path path=response.getResponseList().get(0).getPath(0);

			ExplicitRouteObject ero=path.geteRO();
			LinkedList<EROSubobject> eroSubObjList=ero.getEROSubobjectList();

			ArrayList<String> sourceList = new ArrayList<String>();
			ArrayList<String> destinationList = new ArrayList<String>();

			//CREAR LISTA DE EROS
			LinkedList<ExplicitRouteObject> eroList=new LinkedList<ExplicitRouteObject>();
			int numNewLinks = createEROList(eroSubObjList,eroList,sourceList,destinationList);
			
			
			if (numNewLinks>0){
			if (networkLSPManager.setMLLSP(eroSubObjList,request.getRequestList().getFirst().getRequestParameters().isBidirect(), null)){
				Timer planificationTimer= new Timer();
				RealiseCapacityTask realiseCapacity = new RealiseCapacityTask(networkLSPManager,eroSubObjList,null,request.getRequestList().getFirst().getRequestParameters().isBidirect(), null);	
				long duration =Math.round(testerParams.getMeanConectionTime());
				log.info("LSP duration: "+duration);
				planificationTimer.schedule(realiseCapacity,duration);
			}
			}
			else{
				if (networkLSPManager.setLSP(eroSubObjList,request.getRequestList().getFirst().getRequestParameters().isBidirect(), null)){
					Timer planificationTimer= new Timer();					
					RealiseCapacityTask realiseCapacity = new RealiseCapacityTask(networkLSPManager,eroSubObjList,null,request.getRequestList().getFirst().getRequestParameters().isBidirect(), null);	
					long duration =Math.round(testerParams.getMeanConectionTime());
					log.info("LSP duration: "+duration);
					planificationTimer.schedule(realiseCapacity,duration);
				}
				
			}
		}
	}
}

			
	
	static void handleResponse(PCEPRequest request,PCEPResponse response,PCCPCEPSession VNTMSession ){
		if (response.getResponseList().isEmpty()){
			log.severe("ERROR in response");
			//stats.addNoPathResponse();
			System.exit(1);
			return;
		}else {
			if (response.getResponseList().get(0).getNoPath()!=null){
				log.info("NO PATH");
//				stats.addNoPathResponse();
//				stats.analyzeBlockingProbability(1);
				return;	
			}else {
				Path path=response.getResponseList().get(0).getPath(0);
//				stats.analyzeBlockingProbability(0);

				ExplicitRouteObject ero=path.geteRO();
				LinkedList<EROSubobject> eroSubObjList=ero.getEROSubobjectList();
				//CREAR LISTA DE EROS
				LinkedList<ExplicitRouteObject> eroList=new LinkedList<ExplicitRouteObject>();			
				
				int numNewLinks = createEROList(eroSubObjList,eroList);
				//Si hay camino multilayer
				if (numNewLinks>0){					
//					stats.addMLResponse();
					for (int i=0;i<numNewLinks;++i){
						
						//Mandar solicitud a Fer
						log.info("Reserving LSP and sending capacity update");
						PCEPTELinkSuggestion telinksug=new PCEPTELinkSuggestion();
						Path path2=new Path();
						path2.seteRO(eroList.get(i));
						telinksug.setPath(path2);	
						try {
							telinksug.encode();
						} catch (PCEPProtocolViolationException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
						
							log.info("Sending TE LINK SUGGESTION message i:"+i);
							
//						stats.addNumberActiveLSP();
						//Mandar el LSP por PCCPCEPSession al VNTM
						sendLSP(VNTMSession.getOut(),telinksug);
						Timer planificationTimer= new Timer();
						RealiseMLCapacityTask realiseCapacity = new RealiseMLCapacityTask(null,VNTMSession.getOut(),telinksug );	
						long duration =Math.round(testerParams.getMeanConectionTime());
						log.info("LSP duration: "+duration);
						planificationTimer.schedule(realiseCapacity,duration);

					}
				}
			}

	}
	
		
				
		
	}
	 static void sendLSP(DataOutputStream out,PCEPTELinkSuggestion telinksug) {
		 
		 //		try {
		 //		Socket socket = new Socket(address, port);
		 //	    BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
		 //		bufferedWriter.write(update);
		 //		bufferedWriter.flush();
		 //		socket.close();
		 //		} catch (UnknownHostException e) {
		 //		e.printStackTrace();
		 //		} catch (IOException e) {
		 //		e.printStackTrace();
		 //	}
		 int bytesToReserve = telinksug.getBytes().length;
		
			byte[] bytes = new byte[bytesToReserve]; 
			System.arraycopy(telinksug.getBytes(), 0, bytes, 0, bytesToReserve);
			try {		
				out.write(bytes);
				out.flush();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
		 }
	static int createEROList(LinkedList<EROSubobject> eroSubObjList,LinkedList<ExplicitRouteObject> eroList){

		boolean layerInfoFound=false;
		int numNewLinks=0;
		int counterArray=0;
//		sourceList = new ArrayList<String>();
//		destinationList = new ArrayList<String>();
		//cREAR LISTA DE EROS
		LinkedList<EROSubobject> eroSubObjList2=null;
		String strPrev=null;
		for (int i=0;i<eroSubObjList.size();++i){
			if ((eroSubObjList.get(i)).getType()==SubObjectValues.ERO_SUBOBJECT_LAYER_INFO){
				if (layerInfoFound==false){
					layerInfoFound=true;							
					//Create a new ERO to add at the list
					ExplicitRouteObject newERO =  new ExplicitRouteObject();							
					eroSubObjList2=newERO.getEROSubobjectList();	
					eroSubObjList2.add(eroSubObjList.get(i-1));							
					eroList.add(newERO);
					numNewLinks=numNewLinks+1;
												
				}else {
					log.info("Acabo pongo layerInfoEnded a true");
					layerInfoFound=false;
					eroSubObjList2.add(eroSubObjList.get(i+1));
				}
			}
			else if (layerInfoFound==true){
				eroSubObjList2.add(eroSubObjList.get(i));
			}
//			else {		
//				String str1=((IPv4prefixEROSubobject)(eroSubObjList.get(i))).getIpv4address().toString().substring(1);
//				if (strPrev==null){
//					strPrev=str1;
//				}else {
//					sourceList.add(counterArray, strPrev);							
//					destinationList.add(counterArray, str1);
//					strPrev=str1;
//					counterArray++;
//				}															
//			}
		}
		return numNewLinks;
	}
//	static int createPathLSP(LinkedList<EROSubobject> eroSubObjList,LinkedList<Inet4Address> path){
//		boolean layerInfoFound=false;
//		int numNewLinks=0;		
//		path=new LinkedList<Inet4Address>();
//		for (int i=0;i<eroSubObjList.size();++i){
//			if ((eroSubObjList.get(i)).getType()==SubObjectValues.ERO_SUBOBJECT_LAYER_INFO){
//				if (layerInfoFound==false){
//					layerInfoFound=true;							
//					//Create a new ERO to add at the list				
//					numNewLinks=numNewLinks+1;												
//				}else {
//					layerInfoFound=false;
//					path.add(((IPv4prefixEROSubobject)(eroSubObjList.get(i+1))).getIpv4address());
//				}
//			}
////			else if (layerInfoFound==true){
////				eroSubObjList2.add(eroSubObjList.get(i));
////			}
//			else {		
//				path.add(((IPv4prefixEROSubobject)(eroSubObjList.get(i))).getIpv4address());
//			}
//		}
//		return numNewLinks;
//	}
	
}
