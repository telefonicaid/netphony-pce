package es.tid.pce.computingEngine.algorithms.sson;

import es.tid.pce.computingEngine.ComputingRequest;
import es.tid.pce.computingEngine.algorithms.ComputingAlgorithm;
import es.tid.pce.computingEngine.algorithms.ComputingAlgorithmManagerSSON;
import es.tid.pce.computingEngine.algorithms.ComputingAlgorithmPreComputationSSON;
import es.tid.pce.server.wson.ReservationManager;
import es.tid.tedb.TEDB;

public class SVEC_Dynamic_RSAManager implements ComputingAlgorithmManagerSSON {
	SVEC_Dynamic_RSAPreComputation preComp;
	private ReservationManager reservationManager;
	
	@Override
	public ComputingAlgorithm getComputingAlgorithm(
			ComputingRequest pathReq, TEDB ted, int mf) {
		SVEC_Dynamic_RSA algo = new SVEC_Dynamic_RSA(pathReq, ted, reservationManager, mf);
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
		this.preComp=(SVEC_Dynamic_RSAPreComputation) pc;		
	
	}
}
