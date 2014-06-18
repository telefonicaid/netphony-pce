package tid.pce.computingEngine.algorithms.wson;

import tid.pce.computingEngine.ComputingRequest;
import tid.pce.computingEngine.algorithms.ComputingAlgorithm;
import tid.pce.computingEngine.algorithms.ComputingAlgorithmManager;
import tid.pce.computingEngine.algorithms.ComputingAlgorithmPreComputation;
import tid.pce.computingEngine.algorithms.multiLayer.OperationsCounter;
import tid.pce.server.wson.ReservationManager;
import tid.pce.tedb.DomainTEDB;
import tid.pce.tedb.TEDB;

public class SP_FF_RWA_AlgorithmManager implements ComputingAlgorithmManager{
	
	private ReservationManager reservationManager;
	
	private ComputingAlgorithmPreComputation preComputation;
	
	public ComputingAlgorithm getComputingAlgorithm(ComputingRequest pathReq,TEDB ted) {
		SP_FF_RWA_Algorithm sdwg=new SP_FF_RWA_Algorithm(pathReq,(DomainTEDB)ted,reservationManager);
		sdwg.setPreComp(preComputation);
		return sdwg;
	}


	public ReservationManager getReservationManager() {
		return reservationManager;
	}


	public void setReservationManager(ReservationManager reservationManager) {
		this.reservationManager = reservationManager;
	}


	@Override
	public void setPreComputation(ComputingAlgorithmPreComputation pc) {
		this.preComputation=(SP_FF_RWA_AlgorithmPreComputation) pc;	
		
	}


	@Override
	public ComputingAlgorithm getComputingAlgorithm(ComputingRequest pathReq,
			TEDB ted, OperationsCounter OPcounter) {
		// TODO Auto-generated method stub
		return null;
	}
	
	
}
