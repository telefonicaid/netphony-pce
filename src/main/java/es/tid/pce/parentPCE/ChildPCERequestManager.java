package es.tid.pce.parentPCE;

import es.tid.pce.computingEngine.ComputingResponse;
import es.tid.pce.computingEngine.algorithms.ChildPCEInitiate;
import es.tid.pce.computingEngine.algorithms.ChildPCERequest;
import es.tid.pce.pcep.PCEPProtocolViolationException;
import es.tid.pce.pcep.constructs.StateReport;
import es.tid.pce.pcep.messages.PCEPInitiate;
import es.tid.pce.pcep.messages.PCEPMessage;
import es.tid.pce.pcep.messages.PCEPRequest;
import es.tid.pce.pcep.messages.PCEPResponse;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.concurrent.*;
import java.util.logging.Logger;

/**
 * Manages the requests to the Child PCEs
 * Child PCE Session must register using the registerDomainSession method
 * The Parent PCE when asking a PCE must call the method newRequest
 * @author ogondio
 *
 */
public class ChildPCERequestManager {
	
	/**
	 * Relates Child PCEs with its domain with 
	 */
	//private Hashtable<String,ClientRequestManager> domain_pce;
	/**
	 * Relates domain_id with the output stream of the Child PCEs  
	 */
	private Hashtable<Inet4Address,DataOutputStream> domainIdOutputStream;
	private Hashtable<Inet4Address,Inet4Address> domainIdpceId;
	
	public Hashtable<Long,Object> locks;
	public Hashtable<Long,Object> inilocks;
	private Hashtable<Long,PCEPMessage> responses;
	private Hashtable<Long,StateReport> reports;
	
	
	
	
	private ThreadPoolExecutor executor;
	
	private  LinkedBlockingQueue<Runnable> workQueue;
	//private  BlockingQueue<ChildPCERequest> workQueue;
	
	private Logger log;
	private Logger logGUI;
	
	
	public ChildPCERequestManager(){
		locks = new Hashtable<Long, Object>();	
		inilocks = new Hashtable<Long, Object>();
		
		responses=new Hashtable<Long, PCEPMessage>();
		domainIdOutputStream=new Hashtable<Inet4Address,DataOutputStream>();
		domainIdpceId=new Hashtable<Inet4Address,Inet4Address>();
		reports = new Hashtable<Long,StateReport>();
		int corePoolSize=5;
		int maximumPoolSize=10;
		long keepAliveTime=120;
		workQueue=new LinkedBlockingQueue<Runnable>();
		executor= new ThreadPoolExecutor(corePoolSize, maximumPoolSize,keepAliveTime, TimeUnit.SECONDS,workQueue);
		
		log = Logger.getLogger("PCEServer");
		logGUI=Logger.getLogger("GUILogger");
		
	}
	
	public LinkedList<ComputingResponse> executeRequests(LinkedList<PCEPRequest> requestList, LinkedList<Object> domainList){
		LinkedList<ComputingResponse> response= new  LinkedList<ComputingResponse>();
		ChildPCERequest cpr;
		LinkedList<FutureTask<ComputingResponse>> ftList=new LinkedList<FutureTask<ComputingResponse>>();
		FutureTask<ComputingResponse> ft;
		for (int i=0;i<requestList.size();++i){
			 cpr=new ChildPCERequest(this, requestList.get(i), domainList.get(i));
			 ft=new FutureTask<ComputingResponse>(cpr);
			 ftList.add(ft);
			 executor.execute(ft);
		}
		long time=120000;
		log.fine("The time is "+time+" miliseconds");
		long timeIni=System.currentTimeMillis();
		long time2;
		ComputingResponse resp;
		for (int i=0;i<requestList.size();++i){
			
			try {
				log.fine("Waiting "+time+" miliseconds for domain "+domainList.get(i));
				resp=ftList.get(i).get(time, TimeUnit.MILLISECONDS);
				time2=System.currentTimeMillis();
				long timePassed=time2-timeIni;
				if (timePassed>=120000){
					time=0;
				}
				else {
					time=time-timePassed;	
				}
				 response.add(resp);
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			} catch (ExecutionException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
			} catch (TimeoutException e) {
				resp=null;
				time2=System.currentTimeMillis();
				time=time-time2-timeIni;
				// TODO Auto-generated catch block
				e.printStackTrace();
//			}
			} catch (Exception e){
				return null; //FIXME: REPARAR PARA MANDAR MAS!!!!
				
			}
			
			
		}
		
		return response;
	}
	
	public LinkedList<ComputingResponse> executeInitiates(LinkedList<PCEPInitiate> initiateList, LinkedList<Object> domainList){
		
		LinkedList<ComputingResponse> response= new  LinkedList<ComputingResponse>();
		ChildPCEInitiate cpr;
		LinkedList<FutureTask<ComputingResponse>> ftList=new LinkedList<FutureTask<ComputingResponse>>();
		FutureTask<ComputingResponse> ft;
		for (int i=0;i<initiateList.size();++i){
			 cpr=new ChildPCEInitiate(this, initiateList.get(i), domainList.get(i));
			 ft=new FutureTask<ComputingResponse>(cpr);
			 ftList.add(ft);
			 executor.execute(ft);
		}
		long time=120000;
		//log.info("The time is "+time+" miliseconds");
		long timeIni=System.currentTimeMillis();
		long time2;
		ComputingResponse resp;
		for (int i=0;i<initiateList.size();++i){
			
			try {
				log.info("Waiting "+time+" miliseconds for initiating in domain "+domainList.get(i));
				resp=ftList.get(i).get(time, TimeUnit.MILLISECONDS);
				time2=System.currentTimeMillis();
				long timePassed=time2-timeIni;
				if (timePassed>=120000){
					time=0;
				}
				else {
					time=time-timePassed;	
				}
				 response.add(resp);

			} catch (TimeoutException e) {
				resp=null;
				time2=System.currentTimeMillis();
				time=time-time2-timeIni;
				// TODO Auto-generated catch block
				e.printStackTrace();
//			}
			} catch (Exception e){
				return null; //FIXME: REPARAR PARA MANDAR MAS!!!!
				
			}
			
			
		}
		
		return response;
	}
	
//	public void addRequest(PCEPRequest pcreq, Inet4Address domain){
//		ChildPCERequest cpr=new ChildPCERequest(this, pcreq, domain);
//		FutureTask<PCEPResponse> ft=new FutureTask<PCEPResponse>(cpr);
//		//workQueue.add((Runnable)cpr);
//		log.info("Vamos a ejecutar");
//		executor.execute(ft);
//		log.info("Salimos de ejecutar");
//	}
	
	public void notifyResponse(PCEPResponse pcres){
		long idRequest=pcres.getResponse(0).getRequestParameters().getRequestID();
		log.fine("Entrando en Notify Response de idRequest "+idRequest);
		Object object_lock=locks.get(new Long(idRequest));
		responses.put(new Long(idRequest), pcres);
		if (object_lock!=null){
			object_lock.notifyAll();	
		}
		locks.remove(object_lock);
	}
	
	public void notifyReport(StateReport sr){
		long idRequest=sr.getSRP().getSRP_ID_number();
		log.fine("Entrando en Notify Report de id "+idRequest);
		Object object_lock=inilocks.get(new Long(idRequest));
		reports.put(new Long(idRequest), sr);
		if (object_lock!=null){
			object_lock.notifyAll();	
		}
		inilocks.remove(object_lock);
	}
	
	public PCEPResponse newRequest( PCEPRequest pcreq, Object domain){
		log.info("New Request to Child PCE");
		Object object_lock=new Object();
//		RequestLock rl=new RequestLock();
		
		
		//((RequestParameters)(((Request)pcreq.getRequest(0)).getReqObject(0))).getRequestID();
		//long idRequest=((RequestParameters)(((Request)pcreq.getRequest(0)).getReqObject(0))).getRequestID();
		long idRequest=pcreq.getRequest(0).getRequestParameters().getRequestID();
		log.fine("Creo lock con idRequest "+idRequest);
		locks.put(new Long(idRequest), object_lock);
		try {
			if (pcreq.getPccReqId()!=null){
				if (pcreq.getMonitoring()!=null){
					logGUI.info(domainIdpceId.get(domain).getHostAddress()+"&file="+pcreq.getPccReqId().getPCCIpAddress().getHostAddress()+":"+pcreq.getMonitoring().getMonitoringIdNumber());	
				}
					
			}
			
			sendRequest(pcreq,domain);
		} catch (IOException e1) {
			locks.remove(object_lock); 
			return null;
		}
		synchronized (object_lock) { 
			try {
				log.fine("Request sent, waiting for response");
				object_lock.wait(30000);
			} catch (InterruptedException e){
			//	FIXME: Ver que hacer
			}
		}
		log.fine("Request or timeout");
		
		PCEPResponse resp=(PCEPResponse)responses.get(new Long(idRequest));
		if (resp==null){
			log.warning("NO RESPONSE!!!!!");
		}
		return resp;
		
	}
	
	
	public StateReport newIni( PCEPInitiate pcini, Object domain){
		Object object_lock=new Object();
		
		long idSRP=pcini.getPcepIntiatedLSPList().get(0).getRsp().getSRP_ID_number();
		log.info("Sending PCEPInitiate to domain "+domain+" srp_id "+idSRP+" : "+pcini.toString());
		inilocks.put(new Long(idSRP), object_lock);
		try {		
			sendInitiate(pcini,domain);
		} catch (IOException e1) {
			log.warning("Problem with response from domain "+domain+" to initiate with srp_id "+idSRP);
			inilocks.remove(object_lock); 
			return null;
		}
		synchronized (object_lock) { 
			try {
				log.fine("Request sent, waiting for response");
				object_lock.wait(30000);
			} catch (InterruptedException e){
			//	FIXME: Ver que hacer
			}
		}	
		StateReport resp=reports.get(new Long(idSRP));
		if (resp==null){
			log.warning("No response from domain "+domain+" to initiate with srp_id "+idSRP);
		}else {
			log.info("Domain "+domain+" replied to Initiate with srp_id "+idSRP+" : "+resp.toString());
		}
		return resp;
		
	}
	
	synchronized public  void sendRequest(PCEPRequest req, Object domain) throws IOException{
		try {
			req.encode();
		} catch (PCEPProtocolViolationException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		DataOutputStream out= domainIdOutputStream.get(domain);
		if (out==null){
			log.warning("There is no PCE for domain "+domain);
			throw new IOException();
		}
		try {
			log.info("Sending Request message to domain "+domain);
						out.write(req.getBytes());
			out.flush();
		} catch (IOException e) {
			log.warning("Error sending REQ: " + e.getMessage());
			throw e;
		}
	}

	synchronized public  void sendInitiate(PCEPInitiate ini, Object domain) throws IOException{
		try {
			ini.encode();
		} catch (PCEPProtocolViolationException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		DataOutputStream out= domainIdOutputStream.get(domain);
		if (out==null){
			log.warning("There is no PCE for domain "+domain);
			throw new IOException();
		}
		try {
			log.info("Sending Initiate message to domain "+domain);
			out.write(ini.getBytes());
			out.flush();
		} catch (IOException e) {
			log.warning("Error sending REQ: " + e.getMessage());
			throw e;
		}
	}

	void registerDomainSession(Inet4Address domain, Inet4Address pceId, DataOutputStream out){
		if (domain!=null){
			log.info("domain "+domain+ "pceId "+pceId);
			if (domainIdOutputStream.containsKey(domain)){
				domainIdOutputStream.remove(domain);
			}
			domainIdOutputStream.put(domain, out);
			if (domainIdpceId.containsKey(domain)){
				domainIdpceId.remove(domain);
			}
			
			domainIdpceId.put(domain, pceId);			
		}
		else {
			log.warning("domain is null, PCE not registered as child");
		}
	}
	
	void removeDomain(Inet4Address domain){
		if (domain!=null){
			domainIdOutputStream.remove(domain);
			domainIdpceId.remove(domain);			
		}
	}

	public Hashtable<Inet4Address, DataOutputStream> getDomainIdOutputStream() {
		return domainIdOutputStream;
	}

	public Hashtable<Inet4Address, Inet4Address> getDomainIdpceId() {
		return domainIdpceId;
	}
	
	

}
