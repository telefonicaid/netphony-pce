package tid.pce.computingEngine.algorithms.sson;

import tid.pce.computingEngine.ComputingRequest;
import tid.pce.computingEngine.algorithms.ComputingAlgorithm;
import tid.pce.computingEngine.algorithms.ComputingAlgorithmManagerSSON;
import tid.pce.computingEngine.algorithms.ComputingAlgorithmPreComputationSSON;
import tid.pce.server.wson.ReservationManager;
import tid.pce.tedb.TEDB;

public class Dynamic_RSAManager implements ComputingAlgorithmManagerSSON {
	Dynamic_RSAPreComputation preComp;
	private ReservationManager reservationManager;
	
	@Override
	public ComputingAlgorithm getComputingAlgorithm(
			ComputingRequest pathReq, TEDB ted, int mf) {
		Dynamic_RSA algo = new Dynamic_RSA(pathReq, ted, reservationManager, mf);
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
		this.preComp=(Dynamic_RSAPreComputation) pc;		
	
	}
}
