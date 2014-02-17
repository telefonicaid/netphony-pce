package tid.pce.computingEngine.algorithms.sson;

import java.util.concurrent.Callable;

import tid.pce.computingEngine.ComputingRequest;
import tid.pce.computingEngine.algorithms.ComputingAlgorithm;
import tid.pce.computingEngine.algorithms.ComputingAlgorithmManagerSSON;
import tid.pce.computingEngine.algorithms.ComputingAlgorithmPreComputation;
import tid.pce.computingEngine.algorithms.ComputingAlgorithmPreComputationSSON;
import tid.pce.pcep.messages.PCEPResponse;
import tid.pce.server.wson.ReservationManager;
import tid.pce.tedb.TEDB;

public class AURE_FF_SSON_algorithmManager implements ComputingAlgorithmManagerSSON {

	AURE_FF_SSON_algorithmPreComputation preComp;
	
	private ReservationManager reservationManager;
	
	@Override
	public ComputingAlgorithm getComputingAlgorithm(
			ComputingRequest pathReq, TEDB ted, int mf) {
		AURE_FF_SSON_algorithm algo = new AURE_FF_SSON_algorithm(pathReq, ted, reservationManager, mf);
		algo.setPreComp(preComp);
		
		return algo;
	}

	@Override
	public void setReservationManager(ReservationManager reservationManager) {
		this.reservationManager=reservationManager;
		
	}

	@Override
	public void setPreComputation(ComputingAlgorithmPreComputationSSON pc) {
		// TODO Auto-generated method stub
		
	}

}
