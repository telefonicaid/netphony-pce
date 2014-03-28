package tid.pce.computingEngine;

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

import tid.abno.modules.PCEParameters;
import tid.pce.pcep.constructs.Request;
import tid.pce.pcep.constructs.SVECConstruct;
import tid.pce.pcep.messages.PCEPReport;
import tid.pce.pcep.messages.PCEPRequest;
import tid.pce.pcep.objects.tlvs.MaxRequestTimeTLV;
import tid.pce.server.PCEServerParameters;
import tid.pce.server.lspdb.LSP_DB;

/**
 * The ReportDispatcher receives PCEPReport messages and decides what to do with:
 * Add information to the database, generate an error,...
 * @author jaume, the farruk
 *
 */

public class ReportDispatcher 
{
	/**
	 * Number of processing threads
	 */
	 private final int nThreads;
	 
	 /**
	  * Array of RequestProcessor Threads
	  */
	 private final ReportProcessorThread[] threads;
	 
	 
	 
	 /**
	  * Queue to add path computing requests.
	  * This queue is read by the request processor threads. 
	  */
	 private LinkedBlockingQueue<PCEPReport> reportMessageQueue;

	
	/**
	  * The logger
	  */
	private Logger log;
	
			
	public ReportDispatcher(PCEServerParameters params, LSP_DB lspDB, int nThreads)
	{
		log=Logger.getLogger("PCEServer");
		
		
		this.nThreads = nThreads;
	    reportMessageQueue = new LinkedBlockingQueue<PCEPReport>();
	    threads = new ReportProcessorThread[nThreads];
        for (int i=0; i<this.nThreads; i++) {
        	log.info("Starting Request Processor Thread");	        	
            threads[i] = new ReportProcessorThread(params,reportMessageQueue, lspDB);
            threads[i].setPriority(Thread.MAX_PRIORITY);
            threads[i].start();
            
        }
	}
	
	public void dispatchReport(PCEPReport pcepReport)
	{	    
		reportMessageQueue.add(pcepReport);
    }
	

}
