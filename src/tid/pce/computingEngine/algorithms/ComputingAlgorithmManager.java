package tid.pce.computingEngine.algorithms;

import tid.pce.computingEngine.ComputingRequest;
import tid.pce.computingEngine.algorithms.multiLayer.OperationsCounter;
import tid.pce.server.wson.ReservationManager;
import tid.pce.tedb.TEDB;

public interface ComputingAlgorithmManager {
	
	public ComputingAlgorithm getComputingAlgorithm(ComputingRequest pathReq,TEDB ted);
	
	//SERGIO
	public ComputingAlgorithm getComputingAlgorithm(ComputingRequest pathReq,TEDB ted, OperationsCounter OPcounter);
	
	public void setReservationManager(ReservationManager reservationManager);
	public void setPreComputation (ComputingAlgorithmPreComputation pc);

}
