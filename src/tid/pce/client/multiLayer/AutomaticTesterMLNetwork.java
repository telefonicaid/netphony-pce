
package tid.pce.client.multiLayer;
import java.util.logging.Logger;

import tid.pce.client.ClientRequestManager;
import tid.pce.client.PCCPCEPSession;
import tid.pce.client.tester.InformationRequest;



/**
 * Testeador de caminos 
 * @author Marta Cuaresma Saturio
 *
 */
public class AutomaticTesterMLNetwork {

	private static PCCPCEPSession PCEsession;
	private static ClientRequestManager crm;
	private static Logger log;
	private static long monitoringIdNumber = 1;
	private static InformationRequest informationRequest;
	/*Variable used for counter how many requests there are*/
	/**
	 * @param args
	 */
	public static void main(String[] args) {
//FIXME: THIS CLASS DIDNT WORK WITH THE ACTUAL INFORMATIONREQUEST CLASS, UNCOMMENT IT WHEN NECESSARY @Ayk		
//		/*If there are arguments, read the PCEServerPort and ipPCE*/
//	
//		if (args.length < 1) {
//			System.out.println("Usage: ClientTester <XML File>");
//			return;
//		}
//		  
//		FileHandler fh;
//		FileHandler fh2;
//		Logger log=Logger.getLogger("PCCClient");
//		try {
//			fh=new FileHandler("PCCClient.log");
//			fh2=new FileHandler("PCEPClientParser.log");
//			//fh.setFormatter(new SimpleFormatter());
//				
//			log.addHandler(fh);
//			log.setLevel(Level.ALL);
//			Logger log2=Logger.getLogger("PCEPParser");
//			log2.addHandler(fh2);
//			log2.setLevel(Level.ALL);
//		} catch (Exception e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//			System.exit(1);
//		}
//		String readFile = args[0];
//		informationRequest = new InformationRequest();
//		informationRequest.readFile(readFile);
//		PCEsession = new PCCPCEPSession(informationRequest.getPCCPCEPsessionParams().getIpPCEList().get(0), informationRequest.getPCCPCEPsessionParams().getPCEServerPortList().get(0),false, new PCEPSessionsInformation());
//		PCEsession.start();
//		PCCPCEPSession PCEsessionVNTM = new PCCPCEPSession("localhost", 4000,false, new PCEPSessionsInformation());
//		PCEsessionVNTM.start();
//		
//		MersenneTwister mt = new MersenneTwister(informationRequest.getSeed());
//	    boolean PCMonReqBool=false;
//		/*BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
//
//		try {
//			 br.readLine();
//		} catch (IOException ioe) {
//			System.out.println("IO error trying to read your command");
//			System.exit(1);
//		}*/
//		/*Fichero que guarda las estadisticas cada cierto tiempo*/
//		FileWriter pw_file = null;		
//	    PrintWriter pw = null;
//		/*Fichero que guarda todas las estadisticas*/
//	    FileWriter apw_file = null;
//		PrintWriter apw = null;
//		if (informationRequest.getFileStatistics() == null ){
//			System.out.println("Not included file Statistics in .xml");
//			System.out.println("We create the file '/root/tester/Statistics.txt'");
//			try {
//				pw_file = new FileWriter("/root/tester/Statistics.txt");
//				pw = new PrintWriter(pw_file);
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}else {
//			try{
//			pw_file = new FileWriter(informationRequest.getFileStatistics());
//			pw = new PrintWriter(pw_file);
//
//			if (informationRequest.getPrintRequestTime() == true){
//				apw_file = new FileWriter("/root/tester/AllRequestTimeStatistics.txt");
//				apw = new PrintWriter(apw_file);
//			}
//		} catch (Exception e) {
//			
//			e.printStackTrace();
//		}
//	}
//	Timer timer=new Timer();
//	Timer planificationTimer = new Timer();
//	Timer printStatisticsTimer = new Timer(); 
//	   
//	AutomaticTesterStatistics stats= new AutomaticTesterStatistics(informationRequest.getLoad());
//	stats.setPrintRequestTime(informationRequest.getPrintRequestTime());
//
//	AutomaticTesterManagementSever atms= new AutomaticTesterManagementSever(PCEsession,PCEsessionVNTM,timer,printStatisticsTimer ,planificationTimer,stats,pw,apw, informationRequest,pw_file, apw_file);
//	atms.start();
//	MersenneTwister mersenneTwisterSendRequest = new MersenneTwister(informationRequest.getSeed()+10);
//	MersenneTwister mersenneTwisterConnectionTime = new MersenneTwister(informationRequest.getSeed()+20);
//	/*Creo mi testeador*/
//	if (informationRequest.getIsExponential()){
//		PrintStatistics printStatistics = new PrintStatistics(stats);
//		printStatisticsTimer.schedule(printStatistics,0,10000);
//
//		   //			if (PCMonReqBool){
//		   //				PCEPMonReq p_m_r= createMonRequestMessage(i);
//		   //				AutomaticTesterNetworkTask task = new AutomaticTesterNetworkTask(p_m_r,PCEsession,PCMonReqBool);
//		   //				timer=new Timer();
//		   //				timer.schedule(task, 0,timeProcessingList.get(i));}
//		   //			else{
//
//		 //  double lambdaSendRequest = 1/(informationRequest.getMeanTimeBetweenRequestList().get(0));
//		 //  double lambdaConnectionTime = 1/(informationRequest.getMeanConectionTimeList().get(0));
//		double lambdaSendRequest = 1/(informationRequest.getMeanTimeBetweenRequest());
//		double lambdaConnectionTime = 1/(informationRequest.getMeanConectionTime());
//		System.out.println("Lambda Send Request "+ lambdaSendRequest);
//		System.out.println("Lambda Connection Time "+ lambdaConnectionTime);
//		Exponential expSendRequest = new Exponential(lambdaSendRequest, mersenneTwisterSendRequest);
//		Exponential connectionTime = new Exponential(lambdaConnectionTime, mersenneTwisterConnectionTime);
//		double timeNextReqD=expSendRequest.nextDouble(); 
//		long timeNextReq =(long)timeNextReqD;
//		if (informationRequest.isRandom() == true){
//			AutomaticTesterMLNetworkRandomTask task = new AutomaticTesterMLNetworkRandomTask(expSendRequest,connectionTime, timer,planificationTimer, PCEsession.crm,PCEsessionVNTM,stats,pw, apw,mt);
//			timer.schedule(task, timeNextReq+2000);
//		}else{
//			for (int i=0;i<informationRequest.getCounter();i++){
//				System.out.println(" Creating path between "+informationRequest.getSourceList(i).toString()+" and "+informationRequest.getDestinationList(i).toString());
//				PCEPRequest p_r = createRequestMessage(i);	
//				AutomaticExponentialTesterMLNetworkTask task = new AutomaticExponentialTesterMLNetworkTask(expSendRequest,connectionTime, timer,planificationTimer, p_r,PCEsession.crm,PCEsessionVNTM,stats,pw, apw);
//				   //period - time in milliseconds between successive task executions
//				timer.schedule(task,timeNextReq);
//			}
//		}
//
//
//	}
//	else {
//		for (int i=0;i<informationRequest.getCounter();i++){
//			   System.out.println(" Creating path between "+informationRequest.getSourceList(i).toString()+" and "+informationRequest.getDestinationList(i).toString());
//			   //				if (PCMonReqBool){
//			   //					PCEPMonReq p_m_r= createMonRequestMessage(i);
//			   //					AutomaticTesterNetworkTask task = new AutomaticTesterNetworkTask(p_m_r,PCEsession,PCMonReqBool);
//			   //					timer=new Timer();
//			   //					timer.schedule(task, 0,timeProcessingList.get(i));}
//			   //				else{
//			   PCEPRequest p_r = createRequestMessage(i);	
//			   AutomaticTesterMLNetworkTask task = new AutomaticTesterMLNetworkTask(p_r,PCEsession,PCEsessionVNTM);				
//			   //timer.schedule(task, 0,Math.round (informationRequest.getMeanTimeBetweenRequestList().get(i)));
//			   timer.schedule(task, 0,Math.round (informationRequest.getMeanTimeBetweenRequest()));
//		   }
//	   }
//		
//	      }
//
//	 
//	
//	
//
//	/**
//	 * Create a PC Request message including Monitoring, PCC-Id-Req and Request
//	 * @param i index of the request  
//	 * @return
//	 */
//	static PCEPRequest createRequestMessage(int i){
//		PCEPRequest p_r = new PCEPRequest();
//		//Creamos el objecto monitoring
//		//Monitoring monitoring=createMonitoring();
//		//Creamos el objeto PCCIdReq
//		//PccReqId pccReqId = createPccReqId();
//		//Creamos el object Request 
//		Request req = createRequest(informationRequest.getSourceList(i),informationRequest.getDestinationList(i));		
//		//ObjectiveFunction of=new ObjectiveFunction();
//		//of.setOFcode(algorithmRuleList.get(0).ar.of);
//		//req.setObjectiveFunction(of);
//		//p_r.setMonitoring(monitoring);
//		//p_r.setPccReqId(pccReqId);
//		p_r.addRequest(req);
//		return p_r;
//	}
//	
//	/**
//	 * Create a PC Monitoring Request message including Monitoring, PCC-Id-Req
//	 * @param i index of the request  
//	 * @return
//	 */
////	static PCEPMonReq createMonRequestMessage(int i){
////		PCEPMonReq p_m_r = new PCEPMonReq();
////		//Creamos el objecto monitoring
////		Monitoring monitoring=createMonitoring();
////		//Creamos el objeto PCCIdReq
////		PccReqId pccReqId = createPccReqId();
////		p_m_r.setMonitoring(monitoring);
////		p_m_r.setPccReqId(pccReqId);
////		return p_m_r;
////	}
//	/**
//	 * Create a request object
//	 * @param src_ip
//	 * @param dst_ip
//	 * @return
//	 */
//	static Request createRequest(Inet4Address src_ip, Inet4Address dst_ip){
//		Request req = new Request();
//		RequestParameters rp= new RequestParameters();
//		rp.setPbit(true);
//		req.setRequestParameters(rp);		
//		rp.setRequestID(PCCPCEPSession.getNewReqIDCounter());
//		System.out.println("Creating test Request");
//		
//		int prio = 1;
//		rp.setPrio(prio);
//		boolean reo = false;
//		rp.setReopt(reo);
//		boolean bi = false;
//		rp.setBidirect(bi);
//		boolean lo = false;
//		rp.setLoose(lo);
//		EndPointsIPv4 ep=new EndPointsIPv4();				
//		req.setEndPoints(ep);
//		ep.setSourceIP(src_ip);	
//		ep.setDestIP(dst_ip);
//		
//		return req;
//	}
//	/**
//	 * Create message PCMonReq to send
//	 * @param src_ip
//	 * @param dst_ip
//	 * @return
//	 */
//	static Request createMonRequest(Inet4Address src_ip, Inet4Address dst_ip){
//		Request req = new Request();
//		RequestParameters rp= new RequestParameters();
//		rp.setPbit(true);
//		req.setRequestParameters(rp);		
//		rp.setRequestID(PCCPCEPSession.getNewReqIDCounter());
//		System.out.println("Creating test Request");
//		
//		int prio = 1;
//		rp.setPrio(prio);
//		boolean reo = false;
//		rp.setReopt(reo);
//		boolean bi = false;
//		rp.setBidirect(bi);
//		boolean lo = false;
//		rp.setLoose(lo);
//		EndPointsIPv4 ep=new EndPointsIPv4();				
//		req.setEndPoints(ep);
//		ep.setSourceIP(src_ip);	
//		ep.setDestIP(dst_ip);
//		
//		return req;
//	}
//
//	static Monitoring createMonitoring(){
//		Monitoring m = new Monitoring();
//		//Liveness
//		m.setLivenessBit(false);
//		//General
//		m.setGeneralBit(true);
//		//Processing Time
//		m.setProcessingTimeBit(true);
//		//Overload
//		m.setOverloadBit(false);
//		//Incomplete
//		m.setIncompleteBit(false);
//		//add monitoring Id number
//		m.setMonitoringIdNumber(monitoringIdNumber);
//		//SET THE P FLAG
//		m.setPbit(true);
//		monitoringIdNumber++;
//		//m.encode();
//		
//		
//		return m;
//	}
//	static PccReqId createPccReqId(){
//		PccReqId p_r_i = new PccReqId();
//		//Add PCC Ip Address
//		if (PCEsession != null){
//		if (PCEsession.getSocket()!=null)
//			p_r_i.setPCCIpAddress((Inet4Address)PCEsession.getSocket().getInetAddress());
//		else
//			System.out.println("El Socket es null!!");
//		}
//		else
//			System.out.println("ps es null!!");
//		return p_r_i;
//		
//	}

	
	}
}
