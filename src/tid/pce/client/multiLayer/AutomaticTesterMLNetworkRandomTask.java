package tid.pce.client.multiLayer;

import java.io.BufferedWriter;
import java.io.DataOutputStream;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Inet4Address;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Calendar;

import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;



import tid.pce.client.ClientRequestManager;
import tid.pce.client.PCCPCEPSession;
import tid.pce.client.emulator.AutomaticTesterStatistics;
import tid.pce.pcep.PCEPProtocolViolationException;
import tid.pce.pcep.constructs.Path;
import tid.pce.pcep.constructs.Request;
import tid.pce.pcep.messages.PCEPMessage;
import tid.pce.pcep.messages.PCEPMonReq;
import tid.pce.pcep.messages.PCEPRequest;
import tid.pce.pcep.messages.PCEPResponse;
import tid.pce.pcep.messages.PCEPTELinkSuggestion;
import tid.pce.pcep.objects.EndPointsIPv4;
import tid.pce.pcep.objects.ExplicitRouteObject;
import tid.pce.pcep.objects.RequestParameters;
import tid.rsvp.objects.subobjects.EROSubobject;
import tid.rsvp.objects.subobjects.SubObjectValues;
import tid.rsvp.objects.subobjects.IPv4prefixEROSubobject;
import cern.jet.random.engine.MersenneTwister;
import cern.jet.random.Exponential;

public class AutomaticTesterMLNetworkRandomTask  extends TimerTask {
	private static int counter=0;
	private ClientRequestManager crm;
	private Logger log;
	static long requestID=123;
	private PCCPCEPSession psVNTM;
	/*Variable used for counter how many requests there are*/
	private Timer timer;
	PCEPRequest request;
	Exponential expSendRequest;
	Exponential connectionTime;
	
	private AutomaticTesterStatistics stats;
	private Timer planificationTimer;
	PrintWriter pw;
	PrintWriter apw;
	MersenneTwister mt;
	
	public AutomaticTesterMLNetworkRandomTask(Exponential expSendRequest,Exponential connectionTime, Timer timer,Timer planificationTimer,ClientRequestManager crm,PCCPCEPSession psVNTM,AutomaticTesterStatistics stats,PrintWriter pw, PrintWriter apw,MersenneTwister mt ){
		this.mt=mt;
		log=Logger.getLogger("PCCClient");
		this.crm=crm;
		this.psVNTM=psVNTM;
		this.expSendRequest=expSendRequest;
		this.connectionTime = connectionTime;
		this.timer=timer;
		this.stats=stats;
		this.planificationTimer=planificationTimer;
		this.pw=pw;
		this.apw=apw;
		
		this.request = createRequestMessage();
	}
	

	
	@Override
	public void run() {
    
		//First, we schedule the next request
		double timeNextReqD=expSendRequest.nextDouble(); 
		long timeNextReq =(long)timeNextReqD;
		AutomaticTesterMLNetworkRandomTask exponentialTester = new AutomaticTesterMLNetworkRandomTask(expSendRequest,connectionTime,timer,planificationTimer, crm, psVNTM,stats,pw,apw,mt);
		log.info("Scheduling next request in "+timeNextReq+" MS ("+timeNextReqD+" )");
		timer.schedule(exponentialTester,timeNextReq);
		//Measure initial time
		long timeIni=System.nanoTime();
		PCEPResponse pr;
		
		request.getRequestList().getFirst().getRequestParameters().setRequestID(requestID);
		requestID=requestID+1;
		//Count one request
		stats.addRequest();
		//FIXME: COGER POSIBLES FALLOS EN EL REQUEST
		pr=crm.newRequest(this.request);
		long timeIni2=System.nanoTime();
		log.info("Response "+pr.toString());
		double reqTime_ms=(timeIni2-timeIni)/1000000;
		double reqTime_us=(timeIni2-timeIni)/1000;
		stats.analyzeReqTime(reqTime_us);
		log.info("Request Time "+reqTime_us+" us");
		String strPrev=null;
		if (pr.getResponseList().isEmpty()){
			log.severe("ERROR in response");
			//FIXME: QUE HACEMOS? CANCELAMOS SIMULACION?
			//stats.addNoPathResponse();
			System.exit(1);
			return;
		}else {
			if (pr.getResponseList().get(0).getNoPath()!=null){
				log.info("NO PATH");
				stats.addNoPathResponse();
				stats.analyzeBlockingProbability(1);
				return;	
			}else {
				Path path=pr.getResponseList().get(0).getPath(0);
				ExplicitRouteObject ero=path.geteRO();
				LinkedList<EROSubobject> eroSubObjList=ero.getEROSubobjectList();
				boolean layerInfoFound=false;
				int numNewLinks=0;
				ArrayList<String> sourceList = new ArrayList<String>();
				ArrayList<String> destinationList = new ArrayList<String>();
				//cREAR LISTA DE EROS
				LinkedList<ExplicitRouteObject> eroList=new LinkedList<ExplicitRouteObject>();
				LinkedList<EROSubobject> eroSubObjList2=null;
				int counterArray=0;
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
							System.out.println("Acabo pongo layerInfoEnded a true");
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
				if (numNewLinks>0){
					stats.addMLResponse();
				}else {
					stats.addSLResponse();
				}
				stats.analyzeBlockingProbability(0);
				for (int i=0;i<numNewLinks;++i){
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
					try {
						log.info("Sending TE LINK SUGGESTION message i:"+i);
						DataOutputStream out=psVNTM.getOut();
						out.write(telinksug.getBytes());
						out.flush();					
						String str1=((IPv4prefixEROSubobject)(eroList.get(i).getEROSubobjectList().getFirst())).getIpv4address().toString().substring(1);
						String str2=((IPv4prefixEROSubobject)(eroList.get(i).getEROSubobjectList().getLast())).getIpv4address().toString().substring(1);
						String str = createLink(str1,str2,String.valueOf(10));
						log.info(str);
						sendUpdate("172.16.3.1", 4190, str);
						stats.addNumberActiveLSP();
						
					} catch (IOException e) {
						log.info("Error sending REQ: " + e.getMessage());
					}
					
					
				}
			//Recorrer el ERO MPLS y enviar updates a Marek
				log.info("Reserving LSP and sending capacity update");
				stats.addNumberActiveLSP();
				String s1;
				for (int i =0; i<sourceList.size();i++){
					
					s1 = reserveCapacity(sourceList.get(i), destinationList.get(i),String.valueOf(1));
					System.out.println(s1);

					sendUpdate("172.16.3.1", 4190, s1);					
				}								   			
				 RealiseMLCapacityTask realiseCapacity = new RealiseMLCapacityTask(sourceList,destinationList,stats);	
				 long duration =Math.round(connectionTime.nextDouble());
				 log.info("LSP duration: "+duration);
				 planificationTimer.schedule(realiseCapacity,duration);
			
			}
			
		}
		

		 
	}//End run

	  public static String reserveCapacity(String sourceVector, String destVector, String bw) {
			String s = "RESERVE:";
			s += sourceVector + ":" + destVector + ":" + bw;

			return s;
		    }

public static void sendUpdate(String address, int port, String update) {
	try {
	    Socket socket = new Socket(address, port);
	    BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
	    bufferedWriter.write(update);
	    bufferedWriter.flush();
	    socket.close();
	} catch (UnknownHostException e) {
	    e.printStackTrace();
	} catch (IOException e) {
	    e.printStackTrace();
	}
   }

public static String createLink(String sourceVector, String destVector, String bw) {
	String s = "CREATE:";
	s += sourceVector + ":" + destVector + ":" + bw;

	return s;
    }


/**
 * Create a PC Request message including Monitoring, PCC-Id-Req and Request
 * @param i index of the request  
 * @return
 */
PCEPRequest createRequestMessage(){
	PCEPRequest p_r = new PCEPRequest();
	//Creamos el objecto monitoring
	//Monitoring monitoring=createMonitoring();
	//Creamos el objeto PCCIdReq
	//PccReqId pccReqId = createPccReqId();
	//Creamos el object Request 
	String base="172.10.1.";
	 int num_nodes=15;
	  int num_origen=(int) (mt.nextDouble()*num_nodes)+1;
	  int num_destino=(int) (mt.nextDouble()*(num_nodes-1)+1);
	  if(num_destino>=num_origen){
	   num_destino=num_destino+1;
	  }
	  String source_s =new String();
	  
	  source_s=base+ String.valueOf(num_origen)+"0";
	  String destiny_s = new String();
	  destiny_s=base+String.valueOf(num_destino)+"0";
	  System.out.println("source_s "+source_s);
	  System.out.println("destiny "+destiny_s);
	  Inet4Address src_ip=null;
	  Inet4Address dst_ip=null;
	try {
		src_ip = (Inet4Address) Inet4Address.getByName(source_s);
		dst_ip=(Inet4Address) Inet4Address.getByName(destiny_s);
	} catch (UnknownHostException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	
	 Request req = createRequest(src_ip,dst_ip);		
	//ObjectiveFunction of=new ObjectiveFunction();
	//of.setOFcode(algorithmRuleList.get(0).ar.of);
	//req.setObjectiveFunction(of);
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
Request createRequest(Inet4Address src_ip, Inet4Address dst_ip){
	Request req = new Request();
	RequestParameters rp= new RequestParameters();
	rp.setPbit(true);
	req.setRequestParameters(rp);		
	rp.setRequestID(PCCPCEPSession.getNewReqIDCounter());
	System.out.println("Creating test Request");
	
	int prio = 1;
	rp.setPrio(prio);
	boolean reo = false;
	rp.setReopt(reo);
	boolean bi = false;
	rp.setBidirect(bi);
	boolean lo = false;
	rp.setLoose(lo);
	EndPointsIPv4 ep=new EndPointsIPv4();				
	req.setEndPoints(ep);
	ep.setSourceIP(src_ip);	
	ep.setDestIP(dst_ip);
	
	return req;
}
}