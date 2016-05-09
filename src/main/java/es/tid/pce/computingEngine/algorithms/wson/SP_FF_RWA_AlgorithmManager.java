package es.tid.pce.computingEngine.algorithms.wson;

import es.tid.pce.computingEngine.ComputingRequest;
import es.tid.pce.computingEngine.algorithms.ComputingAlgorithm;
import es.tid.pce.computingEngine.algorithms.ComputingAlgorithmManager;
import es.tid.pce.computingEngine.algorithms.ComputingAlgorithmPreComputation;
import es.tid.pce.computingEngine.algorithms.multiLayer.OperationsCounter;
import es.tid.pce.server.wson.ReservationManager;
import es.tid.tedb.DomainTEDB;
import es.tid.tedb.TEDB;

import java.net.Inet4Address;
import java.util.Hashtable;

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
	@Override
	public ComputingAlgorithm getComputingAlgorithm(ComputingRequest pathReq,
													TEDB ted, Hashtable<Inet4Address,DomainTEDB> intraTEDBs) {
		// TODO Auto-generated method stub
		return null;
	}
	
}
