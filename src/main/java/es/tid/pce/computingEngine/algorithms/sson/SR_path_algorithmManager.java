package es.tid.pce.computingEngine.algorithms.sson;


import es.tid.pce.computingEngine.ComputingRequest;
import es.tid.pce.computingEngine.algorithms.ComputingAlgorithm;
import es.tid.pce.computingEngine.algorithms.ComputingAlgorithmManagerSSON;
import es.tid.pce.computingEngine.algorithms.ComputingAlgorithmPreComputationSSON;
import es.tid.pce.server.wson.ReservationManager;
import es.tid.tedb.TEDB;

public class SR_path_algorithmManager implements ComputingAlgorithmManagerSSON {

	SR_path_algorithmPreComputation preComp;
	
	private ReservationManager reservationManager;
	
	@Override
	public ComputingAlgorithm getComputingAlgorithm(
			ComputingRequest pathReq, TEDB ted, int mf) {
		SR_path_algorithm algo = new SR_path_algorithm(pathReq, ted, reservationManager, mf);
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
		this.preComp=(SR_path_algorithmPreComputation) pc;		
	
	}

}
