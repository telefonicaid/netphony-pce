package es.tid.pce.computingEngine.algorithms;

import es.tid.pce.computingEngine.ComputingRequest;
import es.tid.pce.computingEngine.algorithms.multiLayer.OperationsCounter;
import es.tid.pce.parentPCE.ChildPCERequestManager;
import es.tid.pce.server.wson.ReservationManager;
import es.tid.tedb.DomainTEDB;
import es.tid.tedb.ReachabilityManager;
import es.tid.tedb.TEDB;

import java.net.Inet4Address;
import java.util.Hashtable;

public class MDHPCEMinNumberDomainsAlgorithmManager implements
ParentPCEComputingAlgorithmManager {

	private ChildPCERequestManager childPCERequestManager;
	private ReachabilityManager reachabilityManager;
	@Override
	public ComputingAlgorithm getComputingAlgorithm(
			ComputingRequest pathReq,
			TEDB ted) {
		MDHPCEMinNumberDomainsAlgorithm sdwg=new MDHPCEMinNumberDomainsAlgorithm(pathReq,ted,childPCERequestManager, reachabilityManager);
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
	public ComputingAlgorithm getComputingAlgorithm(ComputingRequest pathReq,
													TEDB ted, Hashtable<Inet4Address,DomainTEDB> intraTEDBs) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public void setLocalChildRequestManager(
			LocalChildRequestManager localChildRequestManager) {
		// TODO Auto-generated method stub
		
	}

}
