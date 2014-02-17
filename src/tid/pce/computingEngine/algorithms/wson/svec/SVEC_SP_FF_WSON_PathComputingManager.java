package tid.pce.computingEngine.algorithms.wson.svec;

import java.util.concurrent.Callable;

import tid.pce.computingEngine.ComputingRequest;
import tid.pce.computingEngine.algorithms.ComputingAlgorithm;
import tid.pce.computingEngine.algorithms.ComputingAlgorithmManager;
import tid.pce.computingEngine.algorithms.ComputingAlgorithmPreComputation;
import tid.pce.computingEngine.algorithms.multiLayer.OperationsCounter;
import tid.pce.computingEngine.algorithms.wson.AURE_Algorithm;
import tid.pce.computingEngine.algorithms.wson.AURE_AlgorithmPreComputation;
import tid.pce.pcep.messages.PCEPResponse;
import tid.pce.server.wson.ReservationManager;
import tid.pce.tedb.TEDB;

public class SVEC_SP_FF_WSON_PathComputingManager implements ComputingAlgorithmManager {

	SVEC_SP_FF_WSON_PathComputingPreComputation preComp;
	
	private ReservationManager reservationManager;
	
	@Override
	public ComputingAlgorithm getComputingAlgorithm(
			ComputingRequest pathReq, TEDB ted) {
		SVEC_SP_FF_WSON_PathComputing algo = new SVEC_SP_FF_WSON_PathComputing(pathReq, ted);
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
		this.preComp=(SVEC_SP_FF_WSON_PathComputingPreComputation) pc;		
	
	}

	@Override
	public ComputingAlgorithm getComputingAlgorithm(ComputingRequest pathReq,
			TEDB ted, OperationsCounter OPcounter) {
		// TODO Auto-generated method stub
		return null;
	}

}
