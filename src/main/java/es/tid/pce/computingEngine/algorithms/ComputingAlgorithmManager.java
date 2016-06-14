package es.tid.pce.computingEngine.algorithms;

import es.tid.pce.computingEngine.ComputingRequest;
import es.tid.pce.computingEngine.algorithms.multiLayer.OperationsCounter;
import es.tid.pce.server.wson.ReservationManager;
import es.tid.tedb.DomainTEDB;
import es.tid.tedb.TEDB;

import java.net.Inet4Address;
import java.util.Hashtable;

public interface ComputingAlgorithmManager {
	
	public ComputingAlgorithm getComputingAlgorithm(ComputingRequest pathReq,TEDB ted);
	
	//SERGIO
	public ComputingAlgorithm getComputingAlgorithm(ComputingRequest pathReq,TEDB ted, OperationsCounter OPcounter);
	//Andrea
	public ComputingAlgorithm getComputingAlgorithm(ComputingRequest pathReq,TEDB ted, Hashtable<Inet4Address,DomainTEDB> intraTEDBs);
	public void setReservationManager(ReservationManager reservationManager);
	public void setPreComputation (ComputingAlgorithmPreComputation pc);

}
