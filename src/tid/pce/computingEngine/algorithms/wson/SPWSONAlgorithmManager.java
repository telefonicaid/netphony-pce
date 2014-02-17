package tid.pce.computingEngine.algorithms.wson;

import java.util.concurrent.Callable;

import tid.pce.computingEngine.ComputingRequest;
import tid.pce.computingEngine.algorithms.ComputingAlgorithm;
import tid.pce.computingEngine.algorithms.ComputingAlgorithmManager;
import tid.pce.computingEngine.algorithms.ComputingAlgorithmPreComputation;
import tid.pce.computingEngine.algorithms.multiLayer.OperationsCounter;
import tid.pce.pcep.messages.PCEPResponse;
import tid.pce.server.wson.ReservationManager;
import tid.pce.tedb.TEDB;

public class SPWSONAlgorithmManager implements ComputingAlgorithmManager {

	SPWSONAlgorithmPreComputation preComp;
	
	private ReservationManager reservationManager;
	
	@Override
	public ComputingAlgorithm getComputingAlgorithm(
			ComputingRequest pathReq, TEDB ted) {
		SPWSONAlgorithm algo = new SPWSONAlgorithm(pathReq, ted,reservationManager);
		algo.setPreComp(preComp);
		
		return algo;
	}

	@Override
	public void setReservationManager(ReservationManager reservationManager) {
		this.reservationManager=reservationManager;
		
	}

	@Override
	public void setPreComputation(ComputingAlgorithmPreComputation pc) {
		// TODO Auto-generated method stub
		this.preComp=(SPWSONAlgorithmPreComputation) pc;		
	
	}

	@Override
	public ComputingAlgorithm getComputingAlgorithm(ComputingRequest pathReq,
			TEDB ted, OperationsCounter OPcounter) {
		// TODO Auto-generated method stub
		return null;
	}

}
