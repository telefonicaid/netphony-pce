package tid.pce.computingEngine.algorithms;

import es.tid.tedb.ReachabilityManager;
import tid.pce.parentPCE.ChildPCERequestManager;

public interface ParentPCEComputingAlgorithmManager extends ComputingAlgorithmManager{
	
	public void setChildPCERequestManager(ChildPCERequestManager childPCERequestManager);
	
	public void setReachabilityManager(ReachabilityManager reachabilityManager);

	public void setLocalChildRequestManager(
			LocalChildRequestManager localChildRequestManager);

}
