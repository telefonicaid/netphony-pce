package tid.pce.computingEngine.algorithms;

import es.tid.tedb.TEDB;
import tid.pce.computingEngine.ComputingRequest;
import tid.pce.computingEngine.algorithms.multiLayer.OperationsCounter;
import tid.pce.server.wson.ReservationManager;

public interface ComputingAlgorithmManager {
	
	public ComputingAlgorithm getComputingAlgorithm(ComputingRequest pathReq,TEDB ted);
	
	//SERGIO
	public ComputingAlgorithm getComputingAlgorithm(ComputingRequest pathReq,TEDB ted, OperationsCounter OPcounter);
	
	public void setReservationManager(ReservationManager reservationManager);
	public void setPreComputation (ComputingAlgorithmPreComputation pc);

}
