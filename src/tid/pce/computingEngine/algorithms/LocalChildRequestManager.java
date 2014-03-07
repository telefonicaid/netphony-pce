package tid.pce.computingEngine.algorithms;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;


import tid.pce.computingEngine.algorithms.sson.Dynamic_RSA;
import tid.pce.client.ClientRequestManager;
import tid.pce.computingEngine.ComputingRequest;
import tid.pce.computingEngine.algorithms.ChildPCERequest;
import tid.pce.computingEngine.algorithms.sson.AURE_SSON_algorithm;
import tid.pce.pcep.PCEPProtocolViolationException;
import tid.pce.pcep.constructs.Request;
import tid.pce.pcep.messages.PCEPRequest;
import tid.pce.pcep.messages.PCEPResponse;
import tid.pce.tedb.TEDB;

/**
 * Manages the requests to the Child PCEs
 * Child PCE Session must register using the registerDomainSession method
 * The Parent PCE when asking a PCE must call the method newRequest
 * @author ogondio
 *
 */
public class LocalChildRequestManager {
	
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
	
	
	public LocalChildRequestManager(){
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
	
	public LinkedList<PCEPResponse> executeRequests(LinkedList<PCEPRequest> requestList, LinkedList<Inet4Address> domainList, ComputingAlgorithmManagerSSON cam_sson, TEDB ted){
		LinkedList<PCEPResponse> response= new  LinkedList<PCEPResponse>();
		int mf=0;
		LinkedList<FutureTask<PCEPResponse>> ftList=new LinkedList<FutureTask<PCEPResponse>>();
		FutureTask<PCEPResponse> ft;
		for (int i=0;i<requestList.size();++i){
			ComputingRequest compRquest = new ComputingRequest();
			LinkedList<Request> requestList2=new LinkedList<Request>();
			compRquest.setRequestList(requestList2);
			compRquest.getRequestList().add(requestList.get(i).getRequestList().get(0));
			ComputingAlgorithm cpr=cam_sson.getComputingAlgorithm(compRquest,ted,mf);
			 ft=new FutureTask<PCEPResponse>(cpr);
			 ftList.add(ft);
			 executor.execute(ft);
		}
		long time=120000;
		System.out.println("The time is "+time+" miliseconds");
		long timeIni=System.currentTimeMillis();
		long time2;
		PCEPResponse resp;
		for (int i=0;i<requestList.size();++i){
			
			try {
				System.out.println("Waiting "+time+" miliseconds for domain "+domainList.get(i));
				resp=ftList.get(i).get(time, TimeUnit.MILLISECONDS);
				System.out.println("Response: "+resp.getResponse(0).toString());
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
				//System.out.
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
	
	

}
