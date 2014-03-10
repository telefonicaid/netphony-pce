package tid.pce.computingEngine.algorithms;

import java.util.concurrent.Callable;

import tid.pce.computingEngine.ComputingResponse;
import tid.pce.parentPCE.ChildPCERequestManager;
import tid.pce.pcep.messages.PCEPRequest;
import tid.pce.pcep.messages.PCEPResponse;

public class ChildPCERequest implements Callable<ComputingResponse> {
	
	private ChildPCERequestManager childPCERequestManager;
	private PCEPRequest pcreq;
	private Object domain;
	
	public ChildPCERequest(ChildPCERequestManager childPCERequestManager, PCEPRequest pcreq, Object domain){
		this.childPCERequestManager=childPCERequestManager;
		this.pcreq=pcreq;
		this.domain=domain;
	}

	
	public ComputingResponse call() throws Exception {
		ComputingResponse compResp = new ComputingResponse();
		PCEPResponse p_rep = childPCERequestManager.newRequest(pcreq, domain);
		compResp.setResponsetList(p_rep.getResponseList());
		return compResp;
	}

}
