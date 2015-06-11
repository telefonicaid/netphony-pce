package tid.pce.computingEngine.algorithms;

import es.tid.tedb.TEDB;
import tid.pce.computingEngine.ComputingRequest;
import tid.pce.server.wson.ReservationManager;

public interface ComputingAlgorithmManagerSSON {
		
	public ComputingAlgorithm getComputingAlgorithm(ComputingRequest pathReq,TEDB ted, int mf);
	
	public void setPreComputation (ComputingAlgorithmPreComputationSSON pc);

	void setReservationManager(ReservationManager reservationManager);

}