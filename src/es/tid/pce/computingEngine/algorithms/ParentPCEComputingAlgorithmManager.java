package es.tid.pce.computingEngine.algorithms;

import es.tid.pce.parentPCE.ChildPCERequestManager;
import es.tid.tedb.ReachabilityManager;

public interface ParentPCEComputingAlgorithmManager extends ComputingAlgorithmManager{
	
	public void setChildPCERequestManager(ChildPCERequestManager childPCERequestManager);
	
	public void setReachabilityManager(ReachabilityManager reachabilityManager);

	public void setLocalChildRequestManager(
			LocalChildRequestManager localChildRequestManager);

}
