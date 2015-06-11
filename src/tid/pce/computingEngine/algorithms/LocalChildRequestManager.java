package tid.pce.computingEngine.algorithms;

import java.io.DataOutputStream;
import java.net.Inet4Address;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import es.tid.pce.pcep.constructs.Request;
import es.tid.pce.pcep.messages.PCEPInitiate;
import es.tid.pce.pcep.messages.PCEPRequest;
import es.tid.tedb.TEDB;
import tid.pce.computingEngine.ComputingRequest;
import tid.pce.computingEngine.ComputingResponse;

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
	private Hashtable<Long,ComputingResponse> responses;
	
	
	private ThreadPoolExecutor executor;
	
	private  LinkedBlockingQueue<Runnable> workQueue;
	//private  BlockingQueue<ChildPCERequest> workQueue;
	
	private Logger log;
	private Logger logGUI;
	
	
	public LocalChildRequestManager(){
		locks = new Hashtable<Long, Object>();	
		responses=new Hashtable<Long, ComputingResponse>();
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
	
	public LinkedList<ComputingResponse> executeRequests(LinkedList<PCEPRequest> requestList, LinkedList<Inet4Address> domainList, ComputingAlgorithmManagerSSON cam_sson, TEDB ted){
		LinkedList<ComputingResponse> response= new  LinkedList<ComputingResponse>();
		int mf=0;
		LinkedList<FutureTask<ComputingResponse>> ftList=new LinkedList<FutureTask<ComputingResponse>>();
		FutureTask<ComputingResponse> ft;
		for (int i=0;i<requestList.size();++i){
			ComputingRequest compRquest = new ComputingRequest();
			LinkedList<Request> requestList2=new LinkedList<Request>();
			compRquest.setRequestList(requestList2);
			compRquest.getRequestList().add(requestList.get(i).getRequestList().get(0));
			ComputingAlgorithm cpr=cam_sson.getComputingAlgorithm(compRquest,ted,mf);
			 ft=new FutureTask<ComputingResponse>(cpr);
			 ftList.add(ft);
			 executor.execute(ft);
		}
		long time=120000;
		System.out.println("The time is "+time+" miliseconds");
		long timeIni=System.currentTimeMillis();
		long time2;
		ComputingResponse resp;
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
	
//	public LinkedList<ComputingResponse> executeInitiates(LinkedList<PCEPInitiate> iniList, LinkedList<Inet4Address> domainList, ComputingAlgorithmManagerSSON cam_sson, TEDB ted){
//		LinkedList<ComputingResponse> response= new  LinkedList<ComputingResponse>();
//		int mf=0;
//		LinkedList<FutureTask<ComputingResponse>> ftList=new LinkedList<FutureTask<ComputingResponse>>();
//		FutureTask<ComputingResponse> ft;
//		for (int i=0;i<iniList.size();++i){
//			ComputingRequest compRquest = new ComputingRequest();
//			LinkedList<Request> requestList2=new LinkedList<Request>();
//			compRquest.setRequestList(requestList2);
//			compRquest.getRequestList().add(iniList.get(i).getRequestList().get(0));
//			ComputingAlgorithm cpr=cam_sson.getComputingAlgorithm(compRquest,ted,mf);
//			 ft=new FutureTask<ComputingResponse>(cpr);
//			 ftList.add(ft);
//			 executor.execute(ft);
//		}
//		long time=120000;
//		System.out.println("The time is "+time+" miliseconds");
//		long timeIni=System.currentTimeMillis();
//		long time2;
//		ComputingResponse resp;
//		for (int i=0;i<requestList.size();++i){
//			
//			try {
//				System.out.println("Waiting "+time+" miliseconds for domain "+domainList.get(i));
//				resp=ftList.get(i).get(time, TimeUnit.MILLISECONDS);
//				System.out.println("Response: "+resp.getResponse(0).toString());
//				time2=System.currentTimeMillis();
//				long timePassed=time2-timeIni;
//				if (timePassed>=120000){
//					time=0;
//				}
//				else {
//					time=time-timePassed;	
//				}
//				 response.add(resp);
////			} catch (InterruptedException e) {
////				// TODO Auto-generated catch block
////				e.printStackTrace();
////			} catch (ExecutionException e) {
////				// TODO Auto-generated catch block
////				e.printStackTrace();
//			} catch (TimeoutException e) {
//				//System.out.
//				resp=null;
//				time2=System.currentTimeMillis();
//				time=time-time2-timeIni;
//				// TODO Auto-generated catch block
//				e.printStackTrace();
////			}
//			} catch (Exception e){
//				return null; //FIXME: REPARAR PARA MANDAR MAS!!!!
//				
//			}
//			
//		}
//		
//		return response;
//	}
	

}
