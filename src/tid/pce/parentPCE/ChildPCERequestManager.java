package tid.pce.parentPCE;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import tid.pce.computingEngine.algorithms.ChildPCERequest;
import tid.pce.pcep.PCEPProtocolViolationException;
import tid.pce.pcep.messages.PCEPRequest;
import tid.pce.pcep.messages.PCEPResponse;

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
	private Hashtable<Long,PCEPResponse> responses;
	
	
	private ThreadPoolExecutor executor;
	
	private  LinkedBlockingQueue<Runnable> workQueue;
	//private  BlockingQueue<ChildPCERequest> workQueue;
	
	private Logger log;
	private Logger logGUI;
	
	
	public ChildPCERequestManager(){
		locks = new Hashtable<Long, Object>();	
		responses=new Hashtable<Long, PCEPResponse>();
		domainIdOutputStream=new Hashtable<Inet4Address,DataOutputStream>();
		domainIdpceId=new Hashtable<Inet4Address,Inet4Address>();
		int corePoolSize=5;
		int maximumPoolSize=10;
		long keepAliveTime=120;
		workQueue=new LinkedBlockingQueue<Runnable>();
		executor= new ThreadPoolExecutor(corePoolSize, maximumPoolSize,keepAliveTime, TimeUnit.SECONDS,workQueue);
		
		log = Logger.getLogger("PCEServer");
		logGUI=Logger.getLogger("GUILogger");
		
	}
	
	public LinkedList<PCEPResponse> executeRequests(LinkedList<PCEPRequest> requestList, LinkedList<Object> domainList){
		LinkedList<PCEPResponse> response= new  LinkedList<PCEPResponse>();
		ChildPCERequest cpr;
		LinkedList<FutureTask<PCEPResponse>> ftList=new LinkedList<FutureTask<PCEPResponse>>();
		FutureTask<PCEPResponse> ft;
		for (int i=0;i<requestList.size();++i){
			 cpr=new ChildPCERequest(this, requestList.get(i), domainList.get(i));
			 ft=new FutureTask<PCEPResponse>(cpr);
			 ftList.add(ft);
			 executor.execute(ft);
		}
		long time=120000;
		log.info("The time is "+time+" miliseconds");
		long timeIni=System.currentTimeMillis();
		long time2;
		PCEPResponse resp;
		for (int i=0;i<requestList.size();++i){
			
			try {
				log.info("Waiting "+time+" miliseconds for domain "+domainList.get(i));
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
		log.info("Entrando en Notify Response de idRequest "+idRequest);
		Object object_lock=locks.get(new Long(idRequest));
		responses.put(new Long(idRequest), pcres);
		if (object_lock!=null){
			object_lock.notifyAll();	
		}
		locks.remove(object_lock);
	}
	
	public PCEPResponse newRequest( PCEPRequest pcreq, Object domain){
		log.info("New Request to Child PCE");
		Object object_lock=new Object();
//		RequestLock rl=new RequestLock();
		
		
		//((RequestParameters)(((Request)pcreq.getRequest(0)).getReqObject(0))).getRequestID();
		//long idRequest=((RequestParameters)(((Request)pcreq.getRequest(0)).getReqObject(0))).getRequestID();
		long idRequest=pcreq.getRequest(0).getRequestParameters().getRequestID();
		log.info("Creo lock con idRequest "+idRequest);
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
		
		PCEPResponse resp=responses.get(new Long(idRequest));
		if (resp==null){
			log.warning("NO RESPONSE!!!!!");
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
