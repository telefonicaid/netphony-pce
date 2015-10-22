package es.tid.pce.server;

import java.io.DataOutputStream;
import java.net.Inet4Address;
import java.util.Iterator;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

import es.tid.pce.computingEngine.IniProcessorThread;
import es.tid.pce.computingEngine.InitiationRequest;
import es.tid.pce.computingEngine.SingleDomainIniProcessorThread;
import es.tid.pce.parentPCE.ChildPCERequestManager;
import es.tid.pce.parentPCE.MDLSPDB.MultiDomainLSPDB;
import es.tid.pce.pcep.constructs.PCEPIntiatedLSP;
import es.tid.pce.pcep.messages.PCEPInitiate;
import es.tid.pce.server.lspdb.SingleDomainLSPDB;
import es.tid.tedb.ReachabilityManager;

public class SingleDomainInitiateDispatcher {
	 /**
	  * Queue to add Initiate.
	  * This queue is read by the request processor threads. 
	  */
	 private LinkedBlockingQueue<InitiationRequest> lspIniRequestQueue;
	 
	 private Thread singleDomainIniProcessorThread; 

	 private IniPCCManager iniManager;
	 
	 /**
	  * The logger
	  */
	 private Logger log;
	 

	public SingleDomainInitiateDispatcher(SingleDomainLSPDB singleDomainLSPDB,  IniPCCManager iniManager) {
		log=Logger.getLogger("PCEServer");
	    lspIniRequestQueue = new LinkedBlockingQueue<InitiationRequest>();
	    singleDomainIniProcessorThread= new SingleDomainIniProcessorThread(lspIniRequestQueue,singleDomainLSPDB,iniManager);
	    singleDomainIniProcessorThread.start();
	    this.iniManager=iniManager;
	}
	
	
	public void dispathInitiate(PCEPInitiate iniMessage, DataOutputStream out, Inet4Address remotePeerIP)
	{	    	
		log.info("Dispatching Initiate message from "+remotePeerIP);	
		Iterator<PCEPIntiatedLSP> it=iniMessage.getPcepIntiatedLSPList().iterator();
		while (it.hasNext()){
			log.info("Dispaaaatch");
			InitiationRequest ir=new InitiationRequest();
			ir.setOut(out);
			ir.setRemotePeerIP(remotePeerIP);
			ir.setLspIniRequest(it.next());
			lspIniRequestQueue.add(ir);
		}
		
	}


	public IniPCCManager getIniManager() {
		return iniManager;
	}


	public void setIniManager(IniPCCManager iniManager) {
		this.iniManager = iniManager;
	}
	 
	 

}
