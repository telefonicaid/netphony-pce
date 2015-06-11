package tid.pce.computingEngine.algorithms;

import tid.pce.computingEngine.ComputingRequest;
import tid.pce.computingEngine.algorithms.multiLayer.OperationsCounter;
import tid.pce.parentPCE.ChildPCERequestManager;
import tid.pce.server.wson.ReservationManager;
import tid.pce.tedb.ReachabilityManager;
import tid.pce.tedb.TEDB;

public class MDHPCEMinNumberDomainsKSPAlgorithmManager implements
ParentPCEComputingAlgorithmManager {

	private ChildPCERequestManager childPCERequestManager;
	private ReachabilityManager reachabilityManager;
	@Override
	public ComputingAlgorithm getComputingAlgorithm(
			ComputingRequest pathReq,
			TEDB ted) {
		MDHPCEMinNumberDomainsKSPAlgorithm sdwg=new MDHPCEMinNumberDomainsKSPAlgorithm(pathReq,ted,childPCERequestManager, reachabilityManager);
		return sdwg;
	}
	public ChildPCERequestManager getChildPCERequestManager() {
		return childPCERequestManager;
	}
	public void setChildPCERequestManager(
			ChildPCERequestManager childPCERequestManager) {
		this.childPCERequestManager = childPCERequestManager;
	}
	public ReachabilityManager getReachabilityManager() {
		return reachabilityManager;
	}
	public void setReachabilityManager(ReachabilityManager reachabilityManager) {
		this.reachabilityManager = reachabilityManager;
	}
	@Override
	public void setReservationManager(ReservationManager reservationManager) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void setPreComputation(ComputingAlgorithmPreComputation pc) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public ComputingAlgorithm getComputingAlgorithm(ComputingRequest pathReq,
			TEDB ted, OperationsCounter OPcounter) {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public void setLocalChildRequestManager(
			LocalChildRequestManager localChildRequestManager) {
		// TODO Auto-generated method stub
		
	}

}
