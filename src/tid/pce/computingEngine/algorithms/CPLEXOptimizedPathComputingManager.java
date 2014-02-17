package tid.pce.computingEngine.algorithms;

import java.net.Inet4Address;
import java.util.concurrent.Callable;

import org.jgrapht.graph.SimpleDirectedWeightedGraph;

import tid.pce.computingEngine.ComputingRequest;
import tid.pce.computingEngine.algorithms.multiLayer.OperationsCounter;
import tid.pce.pcep.messages.PCEPResponse;
import tid.pce.server.wson.ReservationManager;
import tid.pce.tedb.IntraDomainEdge;
import tid.pce.tedb.TEDB;

public class CPLEXOptimizedPathComputingManager implements
		ComputingAlgorithmManager {

	
	public ComputingAlgorithm getComputingAlgorithm(
			ComputingRequest pathReq,
			TEDB ted) {
		CPLEXOptimizedPathComputing sdwg=new CPLEXOptimizedPathComputing(pathReq,ted);
		return sdwg;
	}

	@Override
	public void setReservationManager(ReservationManager reservationManager) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setPreComputation(ComputingAlgorithmPreComputation pc) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public ComputingAlgorithm getComputingAlgorithm(ComputingRequest pathReq,
			TEDB ted, OperationsCounter OPcounter) {
		// TODO Auto-generated method stub
		return null;
	}

}
