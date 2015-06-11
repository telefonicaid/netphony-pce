package es.tid.pce.server;

import java.util.LinkedList;
import java.util.logging.Logger;

import es.tid.pce.pcep.messages.PCEPRequest;

public class RequestQueue {

	 private final int nThreads;
	 private final RequestProcessorThread[] threads;
	 private final LinkedList queue;
	 private Logger log;

	public RequestQueue(int nThreads)
	    {
		log=Logger.getLogger("PCEServer");
	    this.nThreads = nThreads;
	    queue = new LinkedList();
	    threads = new RequestProcessorThread[nThreads];

	        for (int i=0; i<nThreads; i++) {
	            threads[i] = new RequestProcessorThread(queue);
	            threads[i].start();
	        }
	    }

	    public void execute(Runnable r) {
	    	log.info("Executing");
	        synchronized(queue) {
	            queue.addLast(r);
	            queue.notify();
	        }
	    }
	    
	    public void dispathRequests(PCEPRequest reqMessage){
	    	if (reqMessage.getSvecList().size()!=0){
	    		for (int i=0;i<reqMessage.getSvecList().size();++i){
	    			
	    		}
	    	}
	    	
	    }

}
