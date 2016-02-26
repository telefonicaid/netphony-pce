package es.tid.pce.computingEngine.algorithms;

import es.tid.pce.computingEngine.ComputingRequest;
import es.tid.pce.computingEngine.algorithms.multiLayer.OperationsCounter;
import es.tid.pce.server.wson.ReservationManager;
import es.tid.tedb.TEDB;

public interface ComputingAlgorithmManager {
	
	public ComputingAlgorithm getComputingAlgorithm(ComputingRequest pathReq,TEDB ted);
	
	//SERGIO
	public ComputingAlgorithm getComputingAlgorithm(ComputingRequest pathReq,TEDB ted, OperationsCounter OPcounter);
	
	public void setReservationManager(ReservationManager reservationManager);
	public void setPreComputation (ComputingAlgorithmPreComputation pc);

}
