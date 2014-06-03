package tid.pce.server;

import java.util.Hashtable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

import tid.pce.computingEngine.ComputingResponse;
import tid.pce.pcep.messages.PCEPMessage;
import tid.pce.pcep.messages.PCEPRequest;
import tid.pce.pcep.messages.PCEPResponse;

public class ParentPCERequestManager {
	
	/**
	 * Queue to send the request to the Parent PCE
	 */
	private LinkedBlockingQueue<PCEPMessage> sendingQueue;
	
	public Hashtable<Long,Object> locks;
	private Hashtable<Long,PCEPResponse> responses;
	private Logger log;

	//Constructors
	
	/**
	 * @param sendingQueue Queue to send the request to the Parent PCE
	 */
	public ParentPCERequestManager(LinkedBlockingQueue<PCEPMessage> sendingQueue){
		locks = new Hashtable<Long, Object>();	
		responses=new Hashtable<Long, PCEPResponse>();
		log=Logger.getLogger("PCEServer");
		this.sendingQueue=sendingQueue;
	}

	
	/**
	 * Notify that a new Response from the parent PCE has arrived. 
	 * It is called by the Child PCE-Parent PCE session when a response from the parent PCE arrives
	 * @param pcres
	 */
	public void notifyResponse(PCEPResponse pcres){
		long idRequest=pcres.getResponse(0).getRequestParameters().getRequestID();
		log.finer("Notifying Response with idRequest "+idRequest);
		Object object_lock=locks.get(new Long(idRequest));
		responses.put(new Long(idRequest), pcres);
		if (object_lock!=null){
			object_lock.notifyAll();	
		}			
	}
	
	/**
	 * Send new request to the PCE and wait for its response.
	 * It blocks until a response is received.
	 * @param pcreq
	 * @return
	 */
	public PCEPResponse newRequest( PCEPRequest pcreq){
		log.info("New Request");
		Object object_lock=new Object();
		long idRequest=pcreq.getRequest(0).getRequestParameters().getRequestID();
		log.info("Id request es "+idRequest);
		locks.put(new Long(idRequest), object_lock);
		sendingQueue.add(pcreq);
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
	
}
