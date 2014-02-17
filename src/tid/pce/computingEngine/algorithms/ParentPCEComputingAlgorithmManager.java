package tid.pce.computingEngine.algorithms;

import tid.pce.parentPCE.ChildPCERequestManager;
import tid.pce.parentPCE.ReachabilityManager;

public interface ParentPCEComputingAlgorithmManager extends ComputingAlgorithmManager{
	
	public void setChildPCERequestManager(ChildPCERequestManager childPCERequestManager);
	
	public void setReachabilityManager(ReachabilityManager reachabilityManager);

	public void setLocalChildRequestManager(
			LocalChildRequestManager localChildRequestManager);

}
