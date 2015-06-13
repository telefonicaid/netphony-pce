package es.tid.pce.computingEngine.algorithms.mpls;

import es.tid.pce.computingEngine.ComputingRequest;
import es.tid.pce.computingEngine.algorithms.ComputingAlgorithm;
import es.tid.pce.computingEngine.algorithms.ComputingAlgorithmManager;
import es.tid.pce.computingEngine.algorithms.ComputingAlgorithmPreComputation;
import es.tid.pce.computingEngine.algorithms.multiLayer.OperationsCounter;
import es.tid.pce.server.wson.ReservationManager;
import es.tid.tedb.TEDB;

public class MPLS_MinTH_AlgorithmManager implements ComputingAlgorithmManager{
	
	MPLS_MinTH_AlgorithmPreComputation preComp;
	
	private ReservationManager reservationManager;
	
	public ComputingAlgorithm getComputingAlgorithm(
			ComputingRequest pathReq, TEDB ted) {
		MPLS_MinTH_Algorithm algo = new MPLS_MinTH_Algorithm(pathReq, ted,reservationManager, null);
		algo.setPreComp(preComp);		
		return algo;
	}

	public void setReservationManager(ReservationManager reservationManager) {
		this.reservationManager=reservationManager;
	}

	public void setPreComputation(ComputingAlgorithmPreComputation pc) {
		// TODO Auto-generated method stub
		this.preComp=(MPLS_MinTH_AlgorithmPreComputation) pc;		
	}

	public ComputingAlgorithm getComputingAlgorithm(ComputingRequest pathReq,
			TEDB ted, OperationsCounter OPcounter) {
		MPLS_MinTH_Algorithm algo = new MPLS_MinTH_Algorithm(pathReq, ted,reservationManager, OPcounter);
		algo.setPreComp(preComp);
		return algo;
	}
}