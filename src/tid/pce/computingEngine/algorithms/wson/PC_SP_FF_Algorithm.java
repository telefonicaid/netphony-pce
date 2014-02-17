package tid.pce.computingEngine.algorithms.wson;

import java.util.concurrent.Callable;
import java.util.logging.Logger;

import tid.pce.computingEngine.ComputingRequest;
import tid.pce.computingEngine.algorithms.AlgorithmReservation;
import tid.pce.computingEngine.algorithms.ComputingAlgorithm;
import tid.pce.pcep.messages.PCEPResponse;
import tid.pce.server.wson.ReservationManager;
import tid.pce.tedb.DomainTEDB;

public class PC_SP_FF_Algorithm implements ComputingAlgorithm{
	
	private Logger log=Logger.getLogger("PCEServer");
	private ComputingRequest pathReq;
	private ReservationManager reservationManager;

	public PC_SP_FF_Algorithm(ComputingRequest pathReq,DomainTEDB ted ,ReservationManager reservationManager){
		//this.networkGraph= ted.getDuplicatedNetworkGraph();
		this.pathReq=pathReq;
		this.reservationManager=reservationManager;
		
	}
	@Override
	public PCEPResponse call() throws Exception {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public AlgorithmReservation getReserv() {
		// TODO Auto-generated method stub
		return null;
	}

}
