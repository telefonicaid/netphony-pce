package es.tid.pce.computingEngine.algorithms;

import es.tid.pce.computingEngine.ComputingRequest;
import es.tid.pce.server.wson.ReservationManager;
import es.tid.tedb.TEDB;

public interface ComputingAlgorithmManagerSSON {
		
	public ComputingAlgorithm getComputingAlgorithm(ComputingRequest pathReq,TEDB ted, int mf);
	
	public void setPreComputation (ComputingAlgorithmPreComputationSSON pc);

	void setReservationManager(ReservationManager reservationManager);

}