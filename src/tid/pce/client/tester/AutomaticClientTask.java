package tid.pce.client.tester;


import java.util.Hashtable;
import java.util.Random;
import java.util.TimerTask;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import tid.pce.client.PCCPCEPSession;
import tid.pce.client.emulator.AutomaticTesterStatistics;
import tid.vntm.VNTMSession;
import cern.jet.random.Exponential;
import es.tid.pce.pcep.constructs.Request;
import es.tid.pce.pcep.constructs.SVECConstruct;
import es.tid.pce.pcep.messages.PCEPRequest;
import es.tid.pce.pcep.messages.PCEPResponse;
import es.tid.pce.pcep.objects.BandwidthRequested;
import es.tid.pce.pcep.objects.EndPointsIPv4;
import es.tid.pce.pcep.objects.Metric;
import es.tid.pce.pcep.objects.Monitoring;
import es.tid.pce.pcep.objects.ObjectParameters;
import es.tid.pce.pcep.objects.ObjectiveFunction;
import es.tid.pce.pcep.objects.RequestParameters;
import es.tid.pce.pcep.objects.Reservation;
import es.tid.pce.pcep.objects.Svec;

/**
 * Client task.
 * Crea una request con un origen, destino y par�metros determinados y la manda al PCE. Espera su respuesta,
 * calculando el que �ste ha tardado en responder, y llama a la actividad programada.
 * Tambi�n crea una nueva tarea con sus mismas caracter�sticas y la programa para ser lanzada en un tiempo determinado.
 * 
 * Client task. 
 * This task creates a request with a specific origin, specific destination and specific parameters
 * and sends it to the PCE. Waits for its response, and calls to the activity programmed.  
 * This task also creates a new task with its same characteristics and schedules it to be simulated.
 * @author mcs
 *
 */
public class AutomaticClientTask  extends TimerTask {


	/**
	 * Sesi�nes con los PCE. 
	 * Balanceo de carga
	 */
	private Hashtable<Integer,PCCPCEPSession> PCEsessionList;
	private int currentNumberPCESession;
	
	private Logger log;
	static long requestID=123;
	VNTMSession VNTMsession;

	private static long monitoringIdNumber = 1;
//	/*Variable used for counter how many requests there are*/
//	PCEPRequest request;
	private ScheduledThreadPoolExecutor requestExecutor;
	Exponential expSendRequest;
	private AutomaticTesterStatistics stats;

	private InformationRequest testerParams;
	private Activity thingsToDo;
	private int positionRequestList;
	
	private float [] BW;
//	private long timeNanoCreated;
//	private long timeExpected;
//	Logger logPrueba;
//	Logger logLista;
	
	 /*File archivo = null;
     FileReader fr = null;
     BufferedReader br = null;*/
     
    
	/**
	 * 
	 * @param request
	 * @param expSendRequest
	 * @param requestExecutor
	 * @param crm
	 * @param informationRequest
	 * @param stats
	 * @param positionRequestList identifica el par origen destino en la lista requestToSendList
	 */
	public AutomaticClientTask(Exponential expSendRequest,ScheduledThreadPoolExecutor requestExecutor,Hashtable<Integer,PCCPCEPSession> PCEsessionList,InformationRequest informationRequest,AutomaticTesterStatistics stats,int positionRequestList,int currentNumberPCESession/*,long timeNanoCreated, long timeExpected*/
			, float [] cadenaBW){
		this.stats= stats;
		log=Logger.getLogger("PCCClient");		
		BW = new float [5100];
		this.expSendRequest=expSendRequest;
		//this.connectionTime = connectionTime;
		this.requestExecutor=requestExecutor;
		//this.pw=pw;		
		this.testerParams=informationRequest;
		this.positionRequestList=positionRequestList;
		
//		logLista=Logger.getLogger("requestID");
//		logPrueba = Logger.getLogger("logPrueba");
//		this.timeNanoCreated=timeNanoCreated;
//		this.timeExpected=timeExpected;
		this.PCEsessionList=PCEsessionList;
		this.currentNumberPCESession=currentNumberPCESession;
		this.BW=cadenaBW;
		
		/*		
		try {
			fichero = new DataInputStream(new FileInputStream("secuenciaBW_semilla"+id+".txt"));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		int i=0;
		String linea;
		
		try {
			if ((linea=fichero.readLine())!=null){
			
			}
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		StringTokenizer st = new StringTokenizer(linea);

		try {
		      while (fichero.readFloat() != -1){
		    	  cadenaBW[i] = fichero.readFloat();
		    	  i++;
		      }
		}catch (EOFException e) {} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		try {
			fichero.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
	}

	public Activity getThingsToDo() {
		return thingsToDo;
	}

	public void setThingsToDo(Activity thingsToDo) {
		this.thingsToDo = thingsToDo;
	}

	public void inicializeNumberReqProccesingList(Hashtable<Integer,PCCPCEPSession> PCEsessionList){
	}
//	private synchronized long getIdRequest(){
//		requestID++;
//		if (requestID >= 0xFFFFFFFL){
//			requestID=0;
//		}
//		
//		return requestID;
//	}
	@Override
	public void run() {
		log.info("Starting Automatic Client Interface");		
		//long timeTotal= System.nanoTime() - this.timeNanoCreated;
		//logPrueba.info("Starting Automatic Client Task after "+timeTotal/(double)1000000+" ms, (expecting "+this.timeExpected+")");
		//First, we schedule the next request
		if (testerParams.isLoadBalancing()){
			currentNumberPCESession++;
			currentNumberPCESession =currentNumberPCESession%PCEsessionList.size();
		}
		long timeNextReq;
		if (expSendRequest!=null){//Es exponencial
			double timeNextReqD=expSendRequest.nextDouble(); 
			timeNextReq =(long)timeNextReqD;			
			//Meterle los atributos y cosas
			log.info("Scheduling next request in "+timeNextReq+" MS ("+timeNextReqD+" )");
		}else
			timeNextReq = Math.round (testerParams.getMeanTimeBetweenRequest());
		
		AutomaticClientTask automaticClientTask = new AutomaticClientTask(expSendRequest, requestExecutor,PCEsessionList,testerParams,stats,positionRequestList,currentNumberPCESession/*,System.nanoTime(), timeNextReq*/, BW);
		automaticClientTask.setThingsToDo(thingsToDo);		
		try {
			requestExecutor.schedule(automaticClientTask,timeNextReq,TimeUnit.MILLISECONDS);
		}
		catch(Exception e){				
			log.info("No se van a lanzar mas nuevas request para el par origen destino: ()");
		}
		
		PCEPRequest request = createRequestMessage(positionRequestList);
		
		thingsToDo.addRequest(request);
		//Count one request
		stats.addRequest();
		PCEPResponse pr;
		//Measure initial time
		long timeIni=System.nanoTime();	
		//FIXME: COGER POSIBLES FALLOS EN EL REQUEST
		try{
		if (testerParams.getMaxTimeWaitingForResponse_ms() != -1)
			pr=PCEsessionList.get(currentNumberPCESession).crm.newRequest(request,testerParams.getMaxTimeWaitingForResponse_ms());
		else
			pr=PCEsessionList.get(currentNumberPCESession).crm.newRequest(request);

		long timeIni2=System.nanoTime();
		//log.info("Response "+pr.toString());
		double reqTime_ms=(timeIni2-timeIni)/1000000;
		stats.analyzeReqTime(reqTime_ms);	
		log.info("Request Time "+reqTime_ms+" ms");
		//FIXME: para controlar estabilidad del control plane
		if (reqTime_ms>10000)
			System.exit(0);
		thingsToDo.addResponse(pr);
		thingsToDo.run();		
		
		}catch (Exception e){
			e.printStackTrace();
		}
	}//End run
	
	/**
	 * Create a PC Request message including Monitoring, PCC-Id-Req and Request
	 * @param i index of the request  
	 * @return
	 */
	private PCEPRequest createRequestMessage(int i){
		PCEPRequest p_r = new PCEPRequest();
		//Creamos el objecto monitoring
		//Monitoring monitoring=createMonitoring();
		//Creamos el objeto PCCIdReq
		//PccReqId pccReqId = createPccReqId();
		//Creamos el object Request 
		Request req = null;
		if (testerParams.getRequestToSendList().get(i).getRequestParameters().Is_bandwidth()){
			BandwidthRequested bw= new BandwidthRequested();			
			if (testerParams.getBandwidthMin() != 0){/*Hay que generar el valor de bandwidth aleatorio*/
				Random rnd = new Random();
				float number = (float)rnd.nextInt((int)testerParams.getBandwidthMax());
				while (number < (int)testerParams.getBandwidthMin()){
					number = (float)rnd.nextInt((int)testerParams.getBandwidthMax());
				}
				
				//float number=BW[(int) stats.getNumRequests()];
	
				
				if (number>10){ //creamos svec
					//System.out.println("Tenemos que dividir en varias peticiones!");
										
					SVECConstruct sveco = new SVECConstruct();
					ObjectiveFunction Ofcode = new ObjectiveFunction();
					Ofcode.setOFcode(testerParams.getRequestToSendList().get(i).getRequestParameters().getOfCode());
					Svec svec = new Svec();
					sveco.setObjectiveFunction(Ofcode);
					sveco.setSvec(svec);
					
									
					while (number > 10){
						req = createRequest(i);
						svec.addRequestID(req.getRequestParameters().getRequestID());
						bw.setBw(10);
						req.setBandwidth(bw);
						p_r.addRequest(req);
						number = number - 10;
												
					}
					if (number != 0){
						req = createRequest(i);
						svec.addRequestID(req.getRequestParameters().getRequestID());
						bw.setBw(number);
						req.setBandwidth(bw);
						p_r.addRequest(req);
					}
					p_r.addSvec(sveco);
				}
				else{
				
					req = createRequest(i);
					bw.setBw(number);
					req.setBandwidth(bw);
					p_r.addRequest(req);
				}
			}
			else {
				req = createRequest(i);
				bw.setBw(testerParams.getRequestToSendList().get(i).getRequestParameters().getBW());
				req.setBandwidth(bw);
				p_r.addRequest(req);
			}
		}
		else {
			req = createRequest(i);
			p_r.addRequest(req);
		}

		return p_r;
	}
	/**
	 * Create a request object
	 * @param src_ip
	 * @param dst_ip
	 * @return
	 */
	private Request createRequest(int i){
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
		//Metric
		if (testerParams.getRequestToSendList().get(i).getRequestParameters().isDelayMetric()){
		     Metric metric = new Metric();
		     metric.setMetricType(ObjectParameters.PCEP_METRIC_TYPE_LATENCY_METRIC);
		     metric.setComputedMetricBit(true);
		     req.getMetricList().add(metric);
		   
		  }
		//Offset Algorithmn
		  if (testerParams.getRequestToSendList().get(i).getRequestParameters().isOf()){
		   ObjectiveFunction of = new ObjectiveFunction();
		   req.setObjectiveFunction(of);
		   of.setOFcode(testerParams.getRequestToSendList().get(i).getRequestParameters().getOfCode()); 
		  }
		  //reservation
		  if (testerParams.getRequestToSendList().get(i).getRequestParameters().isReservation()){
		   Reservation res= new Reservation();
		   req.setReservation(res);
		   res.setTimer(testerParams.getRequestToSendList().get(i).getRequestParameters().getTimeReserved());
		  }
		return req;
	}

	
	//MONITORING ----------------------------------------------------------------------------------------------	
		/**
		 * Create a PC Monitoring Request message including Monitoring, PCC-Id-Req
		 * @param i index of the request  
		 * @return
		 */
//		 PCEPMonReq createMonRequestMessage(int i){
//			PCEPMonReq p_m_r = new PCEPMonReq();
//			//Creamos el objecto monitoring
//			Monitoring monitoring=createMonitoring();
//			//Creamos el objeto PCCIdReq
//			PccReqId pccReqId = createPccReqId();
//			p_m_r.setMonitoring(monitoring);
//			p_m_r.setPccReqId(pccReqId);
//			return p_m_r;
//		}

		/**
		 * Create message PCMonReq to send
		 * @param src_ip
		 * @param dst_ip
		 * @return
		 */
		 Request createMonRequest(int i){
			Request req = new Request();
			RequestParameters rp= new RequestParameters();
			//RequestParameters

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

		 Monitoring createMonitoring(){
			Monitoring m = new Monitoring();
			//Liveness
			m.setLivenessBit(false);
			//General
			m.setGeneralBit(true);
			//Processing Time
			m.setProcessingTimeBit(true);
			//Overload
			m.setOverloadBit(false);
			//Incomplete
			m.setIncompleteBit(false);
			//add monitoring Id number
			m.setMonitoringIdNumber(monitoringIdNumber);
			//SET THE P FLAG
			m.setPbit(true);
			monitoringIdNumber++;
			//m.encode();
			
			return m;
		}
		 
}