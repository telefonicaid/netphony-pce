package es.tid.pce.computingEngine;

import es.tid.pce.computingEngine.algorithms.ComputingAlgorithmManager;
import es.tid.pce.computingEngine.algorithms.ComputingAlgorithmManagerSSON;
import es.tid.pce.computingEngine.algorithms.multiLayer.OperationsCounter;
import es.tid.pce.pcep.constructs.Request;
import es.tid.pce.pcep.constructs.SVECConstruct;
import es.tid.pce.pcep.messages.PCEPInitiate;
import es.tid.pce.pcep.messages.PCEPMessageTypes;
import es.tid.pce.pcep.messages.PCEPRequest;
import es.tid.pce.pcep.objects.ObjectiveFunction;
import es.tid.pce.pcep.objects.RequestParameters;
import es.tid.pce.pcep.objects.tlvs.MaxRequestTimeTLV;
import es.tid.pce.server.ParentPCERequestManager;
import es.tid.pce.server.communicationpce.CollaborationPCESessionManager;
import es.tid.pce.server.wson.ReservationManager;
import es.tid.tedb.DomainTEDB;
import es.tid.tedb.TEDB;

import java.io.DataOutputStream;
import java.net.Inet4Address;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

/**
 * The Request Dispatcher receives the PCEP Request messages and distribute the
 * individual requests in the computing threads. It creates Computing request, 
 * which contain the list of Request objects that need to be computed jointly, together
 * with its associated. SVEC object. In case of individual requests, the ComputingRequest 
 * just contains a single Request.
 * If not all the requests referred in the SVEC 
 * @author Oscar Gonzalez de Dios (ogondio@tid.es)
 *
 */

public class RequestDispatcher {

	/**
	 * Number of processing threads
	 */
	 private final int nThreads;
	 
	 /**
	  * Array of RequestProcessor Threads
	  */
	 private final RequestProcessorThread[] threads;
	 
	 private Lock numOPsLock;
	 
	 /**
	  * The logger
	  */
	 private Logger log;
	 
	 /**
	  * For synchronized requests, to temporary store the requests until all the requests arrive
	  */
	 private Hashtable<Long,ComputingRequest> pendingRequestList;
	 
	 /**
	  * Queue to add path computing requests.
	  * This queue is read by the request processor threads. 
	  */
	 private LinkedBlockingQueue<ComputingRequest> pathComputingRequestQueue;
	 
	 /**
	  * Queue to insert path computing requests that failed. 
	  */
	 private LinkedBlockingQueue<ComputingRequest> pathComputingRequestRetryQueue;	 


	 
	 /*
	  * Constructor
	  * @param nThreads
	  * @param ted
	  * @param cpcerm
	  * @param analyzeRequestTime
	  */
	 public RequestDispatcher(int nThreads,TEDB ted,ParentPCERequestManager cpcerm, boolean analyzeRequestTime)
	    {
		log=Logger.getLogger("PCEServer");
	    this.nThreads = nThreads;
	    pathComputingRequestQueue = new LinkedBlockingQueue<ComputingRequest>();
	    pathComputingRequestRetryQueue= new LinkedBlockingQueue<ComputingRequest>();
	    pendingRequestList=new Hashtable<Long,ComputingRequest>();
	    threads = new RequestProcessorThread[nThreads];
	    numOPsLock = new ReentrantLock();
	        for (int i=0; i<this.nThreads; i++) {
	        	log.info("Starting Request Processor Thread");	        	
	            threads[i] = new RequestProcessorThread(pathComputingRequestQueue,ted,cpcerm,pathComputingRequestRetryQueue,analyzeRequestTime);
	            threads[i].setPriority(Thread.MAX_PRIORITY);
	            threads[i].start();
	            
	        }
	        
	    }


	/**
	 * Constructor
	 * @param nThreads
	 * @param ted
	 * @param cpcerm
	 * @param analyzeRequestTime
	 * @param intraTEDBs
	 */

	public RequestDispatcher(int nThreads,TEDB ted,ParentPCERequestManager cpcerm, boolean analyzeRequestTime,Hashtable<Inet4Address,DomainTEDB> intraTEDBs)
	{
		log=Logger.getLogger("PCEServer");
		this.nThreads = nThreads;
		pathComputingRequestQueue = new LinkedBlockingQueue<ComputingRequest>();
		pathComputingRequestRetryQueue= new LinkedBlockingQueue<ComputingRequest>();
		pendingRequestList=new Hashtable<Long,ComputingRequest>();
		threads = new RequestProcessorThread[nThreads];
		numOPsLock = new ReentrantLock();
		for (int i=0; i<this.nThreads; i++) {
			log.info("Starting Request Processor Thread");
			threads[i] = new RequestProcessorThread(pathComputingRequestQueue,ted,cpcerm,pathComputingRequestRetryQueue,analyzeRequestTime, intraTEDBs);
			threads[i].setPriority(Thread.MAX_PRIORITY);
			threads[i].start();

		}

	}


	public RequestDispatcher(int nThreads,TEDB ted,ParentPCERequestManager cpcerm, boolean analyzeRequestTime, boolean useMaxReqTime, ReservationManager reservationManager)
    {
		log=Logger.getLogger("PCEServer");
	    this.nThreads = nThreads;
	    pathComputingRequestQueue = new LinkedBlockingQueue<ComputingRequest>();
	    pathComputingRequestRetryQueue= new LinkedBlockingQueue<ComputingRequest>();
	    pendingRequestList=new Hashtable<Long,ComputingRequest>();
	    threads = new RequestProcessorThread[nThreads];

        for (int i=0; i<this.nThreads; i++) {
        	//log.info("TEEED:: "+ted.printTopology());
        	log.info("1. Starting Request Processor Thread!");	        	
            threads[i] = new RequestProcessorThread(pathComputingRequestQueue,ted,cpcerm,pathComputingRequestRetryQueue,analyzeRequestTime,useMaxReqTime, reservationManager);
            threads[i].setPriority(Thread.MAX_PRIORITY);
            threads[i].start();
            
        }
    }

	public RequestDispatcher(int nThreads,TEDB ted,ParentPCERequestManager cpcerm, boolean analyzeRequestTime, boolean useMaxReqTime, ReservationManager reservationManager, OperationsCounter OPcounter, boolean isMult)
    	{
			log=Logger.getLogger("PCEServer");
		    this.nThreads = nThreads;
		    pathComputingRequestQueue = new LinkedBlockingQueue<ComputingRequest>();
		    pathComputingRequestRetryQueue= new LinkedBlockingQueue<ComputingRequest>();
		    pendingRequestList=new Hashtable<Long,ComputingRequest>();
		    threads = new RequestProcessorThread[nThreads];
		   
	        for (int i=0; i<this.nThreads; i++) {
	        	log.info("Starting Request Processor Thread!");	        	
	            threads[i] = new RequestProcessorThread(pathComputingRequestQueue,ted,cpcerm,pathComputingRequestRetryQueue,analyzeRequestTime,useMaxReqTime, reservationManager, OPcounter, isMult);
	            threads[i].setPriority(Thread.MAX_PRIORITY);
	            threads[i].start();
	        }
    	}


	public RequestDispatcher(int nThreads,TEDB ted,ParentPCERequestManager cpcerm, boolean analyzeRequestTime,CollaborationPCESessionManager collaborationPCESessionManager)
	    {
			log=Logger.getLogger("PCEServer");
		    this.nThreads = nThreads;
		    pathComputingRequestQueue = new LinkedBlockingQueue<ComputingRequest>();
		    pathComputingRequestRetryQueue= new LinkedBlockingQueue<ComputingRequest>();
		    pendingRequestList=new Hashtable<Long,ComputingRequest>();
		    threads = new RequestProcessorThread[nThreads];

	        for (int i=0; i<this.nThreads; i++) {
	        	log.info("Starting Request Processor Thread!");        	
	            threads[i] = new RequestProcessorThread(pathComputingRequestQueue,ted,cpcerm,pathComputingRequestRetryQueue,analyzeRequestTime, collaborationPCESessionManager);
	            threads[i].setPriority(Thread.MAX_PRIORITY);
	            threads[i].start();
	            
	        }
	    }

	public RequestDispatcher(int nThreads,TEDB ted,ParentPCERequestManager cpcerm, boolean analyzeRequestTime, boolean useMaxReqTime, ReservationManager reservationManager,CollaborationPCESessionManager collaborationPCESessionManager)
    {
		log=Logger.getLogger("PCEServer");
	    this.nThreads = nThreads;
	    pathComputingRequestQueue = new LinkedBlockingQueue<ComputingRequest>();
	    pathComputingRequestRetryQueue= new LinkedBlockingQueue<ComputingRequest>();
	    pendingRequestList=new Hashtable<Long,ComputingRequest>();
	    threads = new RequestProcessorThread[nThreads];

        for (int i=0; i<this.nThreads; i++) {
        	log.info("Starting Request Processor Thread!");       	
            threads[i] = new RequestProcessorThread(pathComputingRequestQueue,ted,cpcerm,pathComputingRequestRetryQueue,analyzeRequestTime,useMaxReqTime, reservationManager,collaborationPCESessionManager);
            threads[i].setPriority(Thread.MAX_PRIORITY);
            threads[i].start();
            
        }
    }
	
	public void registerAlgorithm(AlgorithmRule rule, ComputingAlgorithmManager algortithmManager ){
		for (int i=0; i<this.nThreads; i++) {
        	log.info("Registering algorithm im processor");            
            threads[i].registerAlgorithm(rule,algortithmManager);
        }
					
	}
	public void registerAlgorithmSSON(AlgorithmRule rule, ComputingAlgorithmManagerSSON algortithmManager ){
		for (int i=0; i<this.nThreads; i++) {
        	log.info("Registering algorithm im processor");            
            threads[i].registerAlgorithmSSON(rule,algortithmManager);
        }
					
	}
	    
	public void dispathRequests(PCEPRequest reqMessage, DataOutputStream out){
		dispathRequests(reqMessage,  out,null);
	}
	
	
	public void dispathRequests(PCEPInitiate iniMessage, DataOutputStream out)
	{	    	
		log.info("Dispatching Request from Initiate message!");
		
		ComputingRequest cr=new ComputingRequest();
		cr.setOut(out);

		LinkedList<Request> requestList = new LinkedList<Request>();
		Request req = new Request();
		ObjectiveFunction of=new ObjectiveFunction();//FIXME: FIRE!!!
		of.setOFcode(1002);
		req.setObjectiveFunction(of);
		req.setEndPoints(iniMessage.getPcepIntiatedLSPList().get(0).getEndPoint());
		req.setBandwidth(iniMessage.getPcepIntiatedLSPList().get(0).getBandwidth());
		RequestParameters reqparams=new RequestParameters();
		reqparams.setRequestID(iniMessage.getPcepIntiatedLSPList().get(0).getLsp().getLspId());
		req.setRequestParameters(reqparams);
		requestList.add(req);
		
		cr.setRequestList(requestList);
		
		cr.setTimeStampNs(System.nanoTime());
		cr.setMaxTimeInPCE(120000);
		cr.getEcodingType(PCEPMessageTypes.MESSAGE_INITIATE);
		
		pathComputingRequestQueue.add(cr);
	}
	
	public void dispathRequests(PCEPInitiate iniMessage, DataOutputStream out, Inet4Address remotePCEId)
	{	    	
		log.info("Dispatching Initiate message");
		
		ComputingRequest cr=new ComputingRequest();
		cr.setOut(out);

		LinkedList<Request> requestList = new LinkedList<Request>();
		Request req = new Request();
		ObjectiveFunction of=new ObjectiveFunction();//FIXME: FIRE!!!
		of.setOFcode(1002);
		req.setObjectiveFunction(of);
		req.setEndPoints(iniMessage.getPcepIntiatedLSPList().get(0).getEndPoint());
		req.setBandwidth(iniMessage.getPcepIntiatedLSPList().get(0).getBandwidth());
		RequestParameters reqparams=new RequestParameters();
		reqparams.setRequestID(iniMessage.getPcepIntiatedLSPList().get(0).getLsp().getLspId());
		req.setRequestParameters(reqparams);
		requestList.add(req);
		cr.setIniLSP(iniMessage.getPcepIntiatedLSPList().get(0));
		cr.setRequestList(requestList);
	    
		cr.setTimeStampNs(System.nanoTime());
		cr.setMaxTimeInPCE(120000);
		cr.getEcodingType(PCEPMessageTypes.MESSAGE_INITIATE);
		
		pathComputingRequestQueue.add(cr);
	}
	

	
    public void dispathRequests(PCEPRequest reqMessage, DataOutputStream out, Inet4Address remotePCEId){	    
    	if (out==null){
    		log.severe("OUT ESTA A NULL!!!!");
    	}
    	//Obtain the request list
    	LinkedList<Request> reqList=reqMessage.getRequestList();
    	log.finest("There are "+reqMessage.getRequestList().size()+" requests");
    	//If there are request to sincronize...
    	if (reqMessage.getSvecList().size()!=0){
    		log.finest("SVEC is present!");
    		int numRequests=0;
    		Hashtable<Long,Request> hashReqList=new Hashtable<Long,Request>();
    		for (int i=0;i<reqList.size();++i){
    			hashReqList.put(new Long(reqList.get(i).getRequestParameters().getRequestID()), reqList.get(i));
    		}
    		log.finest("TAM DE  hashReqList ES "+hashReqList.size());
    		for (int i=0;i<reqMessage.getSvecList().size();++i){
    			log.finest("SVEC begins");
    			SVECConstruct svecc=reqMessage.getSvecList().get(i);
    			log.finest("SVEC TIENE "+svecc.getSvec().getRequestIDlist().size());
    			if (svecc.getSvec().getRequestIDlist().size()!=0){
    				
    				ComputingRequest cr=new ComputingRequest();
    				cr.setRequestList(new LinkedList<Request>());
    				cr.setOut(out);
    				cr.setSvec(svecc);
	    			ArrayList<Long> reqIDlist=reqMessage.getSvecList().get(i).getSvec().getRequestIDlist();
	    			for (int j=0;j<reqIDlist.size();++j){
	    				long reqId=reqIDlist.get(j);
	    				Request req=hashReqList.remove(reqId);
	    				if (req!=null){
	    					cr.getRequestList().add(req);
	    					
	    					numRequests=numRequests+1;
	    				}
	    				else {
	    					pendingRequestList.put(reqId, cr);
	    				}	    				
	    				
	    			}
	    			pathComputingRequestQueue.add(cr);
    			}	    				    			
    		}
    		if (hashReqList.size()!=0){
    			Enumeration<Request> enumReq=hashReqList.elements();
    			while(enumReq.hasMoreElements()){
    				ComputingRequest cr=new ComputingRequest();
    				cr.setOut(out);
    				LinkedList<Request> req=new LinkedList<Request>();
    				req.add(enumReq.nextElement());
    				cr.setRequestList(req);
    				pathComputingRequestQueue.add(cr);
    			}
    		}	    		
    	}
    	else {
    		if (reqMessage.getRequestList().size()==1){
    			log.info("Dispatching Request!");
    			ComputingRequest cr=new ComputingRequest();
    			cr.setOut(out);
    			if (remotePCEId!=null){
    				cr.setRemotePCEId(remotePCEId);
    			}
    			cr.setRequestList(reqMessage.getRequestList());
    			cr.setTimeStampNs(System.nanoTime());
    			if(reqMessage.getMonitoring()!=null){
    				cr.setMonitoring(reqMessage.getMonitoring());
    				
    			}
    			if(reqMessage.getPccReqId()!=null){
    				cr.setPccReqId(reqMessage.getPccReqId());
    				
    			}
    			
    			MaxRequestTimeTLV reqTLV=reqMessage.getRequestList().getFirst().getRequestParameters().getMaxRequestTimeTLV();
    			if (reqTLV!=null){	    				
    				cr.setMaxTimeInPCE(reqTLV.getMaxRequestTime());
    			}else{
    				cr.setMaxTimeInPCE(120000);
    			}
    			pathComputingRequestQueue.add(cr);
    		}
    		else {
    			log.info("The number of requests is "+reqMessage.getRequestList().size());
    			for (int i=0;i<reqMessage.getRequestList().size();i++){
    				log.info("Dispatching Request "+i+"th request");
    				ComputingRequest cr=new ComputingRequest();
    				cr.setOut(out);
    				LinkedList<Request> requestList=new LinkedList<Request>();
    				requestList.add(reqMessage.getRequestList().get(i));
	    			cr.setRequestList(requestList);
	    			pathComputingRequestQueue.add(cr);
    			}
    		}
		}
    }

	public RequestProcessorThread[] getThreads() {
		return threads;
	}
    
    /*
     * Insert a computing request in the retry Queue
     */
	public void addComputingRequestToRetryQueue(ComputingRequest compreq){
		pathComputingRequestRetryQueue.add(compreq);
	}
    
	/*
	 * Move all the elements in the retry queue to the 
	 * computing request queue in the same order
	 */
	public synchronized void moveRetryQueueToComputingRequestQueue(){
		
		int numRetryReq=pathComputingRequestRetryQueue.size();
		for(int i=0;i<numRetryReq;++i){
			try {
				pathComputingRequestQueue.add(pathComputingRequestRetryQueue.take());
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public int queueSize(){
		return pathComputingRequestQueue.size() ;
	}
	public int retryQueueSize(){
		return pathComputingRequestRetryQueue.size() ;
	}
}
