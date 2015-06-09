package tid.pce.computingEngine.algorithms;

import java.util.concurrent.Callable;

import es.tid.pce.pcep.constructs.StateReport;
import es.tid.pce.pcep.messages.PCEPInitiate;
import es.tid.pce.pcep.messages.PCEPReport;
import es.tid.pce.pcep.messages.PCEPRequest;
import es.tid.pce.pcep.messages.PCEPResponse;
import tid.pce.computingEngine.ComputingResponse;
import tid.pce.parentPCE.ChildPCERequestManager;

public class ChildPCEInitiate implements Callable<ComputingResponse> {
	
	private ChildPCERequestManager childPCERequestManager;
	private PCEPInitiate pcini;
	private Object domain;
	
	public ChildPCEInitiate(ChildPCERequestManager childPCERequestManager, PCEPInitiate pcini, Object domain){
		this.childPCERequestManager=childPCERequestManager;
		this.pcini=pcini;
		this.domain=domain;
	}

	
	public ComputingResponse call() throws Exception {
		ComputingResponse compResp = new ComputingResponse();
		StateReport rep = childPCERequestManager.newIni(pcini, domain);
		compResp.getReportList().add(rep);
		return compResp;
	}

}
