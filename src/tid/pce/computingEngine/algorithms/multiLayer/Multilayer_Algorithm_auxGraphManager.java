package tid.pce.computingEngine.algorithms.multiLayer;

import tid.pce.computingEngine.ComputingRequest;
import tid.pce.computingEngine.algorithms.ComputingAlgorithm;
import tid.pce.computingEngine.algorithms.ComputingAlgorithmManager;
import tid.pce.computingEngine.algorithms.ComputingAlgorithmPreComputation;
import tid.pce.computingEngine.algorithms.wson.AURE_Algorithm;
import tid.pce.computingEngine.algorithms.wson.AURE_AlgorithmPreComputation;
import tid.pce.server.wson.ReservationManager;
import tid.pce.tedb.TEDB;
import tid.pce.computingEngine.algorithms.multiLayer.Multilayer_Algorithm_auxGraph;
import tid.pce.computingEngine.algorithms.multiLayer.Multilayer_Algorithm_auxGraphPreComputation;

public class Multilayer_Algorithm_auxGraphManager implements ComputingAlgorithmManager{
	
	Multilayer_Algorithm_auxGraphPreComputation preComp;
	
	private ReservationManager reservationManager;
	
	public ComputingAlgorithm getComputingAlgorithm(
			ComputingRequest pathReq, TEDB ted) {
		Multilayer_Algorithm_auxGraph algo = new Multilayer_Algorithm_auxGraph(pathReq, ted,reservationManager, null);
		algo.setPreComp(preComp);
		return algo;
	}
	
	public void setReservationManager(ReservationManager reservationManager) {
		this.reservationManager=reservationManager;
	}
	public void setPreComputation(ComputingAlgorithmPreComputation pc) {
		// TODO Auto-generated method stub
		this.preComp=(Multilayer_Algorithm_auxGraphPreComputation) pc;		
	}

	@Override
	public ComputingAlgorithm getComputingAlgorithm(ComputingRequest pathReq,
			TEDB ted, OperationsCounter OPcounter) {
		Multilayer_Algorithm_auxGraph algo = new Multilayer_Algorithm_auxGraph(pathReq, ted,reservationManager, OPcounter);
		algo.setPreComp(preComp);
		return algo;
	}
}