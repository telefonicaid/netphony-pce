package es.tid.pce.computingEngine.algorithms.sson;

import es.tid.pce.computingEngine.ComputingRequest;
import es.tid.pce.computingEngine.algorithms.ComputingAlgorithm;
import es.tid.pce.computingEngine.algorithms.ComputingAlgorithmManagerSSON;
import es.tid.pce.computingEngine.algorithms.ComputingAlgorithmPreComputationSSON;
import es.tid.pce.server.wson.ReservationManager;
import es.tid.tedb.TEDB;

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
