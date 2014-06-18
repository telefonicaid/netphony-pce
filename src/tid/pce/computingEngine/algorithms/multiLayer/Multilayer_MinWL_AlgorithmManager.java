package tid.pce.computingEngine.algorithms.multiLayer;

import tid.pce.computingEngine.ComputingRequest;
import tid.pce.computingEngine.algorithms.ComputingAlgorithm;
import tid.pce.computingEngine.algorithms.ComputingAlgorithmManager;
import tid.pce.computingEngine.algorithms.ComputingAlgorithmPreComputation;
import tid.pce.server.wson.ReservationManager;
import tid.pce.tedb.TEDB;

public class Multilayer_MinWL_AlgorithmManager implements ComputingAlgorithmManager{
	
	Multilayer_MinTH_AlgorithmPreComputation preComp;
	
	private ReservationManager reservationManager;
	
	public ComputingAlgorithm getComputingAlgorithm(
			ComputingRequest pathReq, TEDB ted) {
		Multilayer_MinTH_Algorithm algo = new Multilayer_MinTH_Algorithm(pathReq, ted,reservationManager, null);
		algo.setPreComp(preComp);		
		return algo;
	}

	public void setReservationManager(ReservationManager reservationManager) {
		this.reservationManager=reservationManager;
	}

	public void setPreComputation(ComputingAlgorithmPreComputation pc) {
		// TODO Auto-generated method stub
		this.preComp=(Multilayer_MinTH_AlgorithmPreComputation) pc;		
	}

	@Override
	public ComputingAlgorithm getComputingAlgorithm(ComputingRequest pathReq,
			TEDB ted, OperationsCounter OPcounter) {
		Multilayer_MinWL_Algorithm algo = new Multilayer_MinWL_Algorithm(pathReq, ted,reservationManager, OPcounter);
		algo.setPreComp(preComp);
		return algo;
	}
}

