package tid.pce.computingEngine.algorithms;

import tid.pce.computingEngine.*;
import tid.pce.server.wson.ReservationManager;
import tid.pce.tedb.*;

public interface ComputingAlgorithmManagerSSON {
		
	public ComputingAlgorithm getComputingAlgorithm(ComputingRequest pathReq,TEDB ted, int mf);
	
	public void setPreComputation (ComputingAlgorithmPreComputationSSON pc);

	void setReservationManager(ReservationManager reservationManager);

}