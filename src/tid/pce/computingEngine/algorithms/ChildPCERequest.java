package tid.pce.computingEngine.algorithms;

import java.net.Inet4Address;
import java.util.concurrent.Callable;

import tid.pce.parentPCE.ChildPCERequestManager;
import tid.pce.pcep.messages.PCEPRequest;
import tid.pce.pcep.messages.PCEPResponse;

public class ChildPCERequest implements Callable<PCEPResponse> {
	
	private ChildPCERequestManager childPCERequestManager;
	private PCEPRequest pcreq;
	private Object domain;
	
	public ChildPCERequest(ChildPCERequestManager childPCERequestManager, PCEPRequest pcreq, Object domain){
		this.childPCERequestManager=childPCERequestManager;
		this.pcreq=pcreq;
		this.domain=domain;
	}

	
	public PCEPResponse call() throws Exception {
		return childPCERequestManager.newRequest(pcreq, domain);
	}

}
