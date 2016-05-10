package es.tid.pce.computingEngine.algorithms.multiLayer;

import es.tid.pce.computingEngine.ComputingRequest;
import es.tid.pce.computingEngine.algorithms.ComputingAlgorithm;
import es.tid.pce.computingEngine.algorithms.ComputingAlgorithmManager;
import es.tid.pce.computingEngine.algorithms.ComputingAlgorithmPreComputation;
import es.tid.pce.server.wson.ReservationManager;
import es.tid.tedb.DomainTEDB;
import es.tid.tedb.TEDB;

import java.net.Inet4Address;
import java.util.Hashtable;

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
	@Override
	public ComputingAlgorithm getComputingAlgorithm(ComputingRequest pathReq,
													TEDB ted, Hashtable<Inet4Address,DomainTEDB> intraTEDBs) {
		// TODO Auto-generated method stub
		return null;
	}

}