package tid.pce.computingEngine.algorithms.wson;

import tid.pce.computingEngine.ComputingRequest;
import tid.pce.computingEngine.algorithms.ComputingAlgorithm;
import tid.pce.computingEngine.algorithms.ComputingAlgorithmManager;
import tid.pce.computingEngine.algorithms.ComputingAlgorithmPreComputation;
import tid.pce.computingEngine.algorithms.multiLayer.OperationsCounter;
import tid.pce.server.wson.ReservationManager;
import tid.pce.tedb.TEDB;

public class AURE_FF_AlgorithmManager implements ComputingAlgorithmManager {

	AURE_FF_AlgorithmPreComputation preComp;
	
	private ReservationManager reservationManager;
	
	@Override
	public ComputingAlgorithm getComputingAlgorithm(
			ComputingRequest pathReq, TEDB ted) {
		AURE_FF_Algorithm algo = new AURE_FF_Algorithm(pathReq, ted,reservationManager);
		algo.setPreComp(preComp);
		
		return algo;
	}

	@Override
	public void setReservationManager(ReservationManager reservationManager) {
		this.reservationManager=reservationManager;
		
	}

	@Override
	public void setPreComputation(ComputingAlgorithmPreComputation pc) {
		// TODO Auto-generated method stub
		this.preComp=(AURE_FF_AlgorithmPreComputation) pc;		
	
	}

	@Override
	public ComputingAlgorithm getComputingAlgorithm(ComputingRequest pathReq,
			TEDB ted, OperationsCounter OPcounter) {
		// TODO Auto-generated method stub
		return null;
	}

}
