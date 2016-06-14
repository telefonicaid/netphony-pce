package es.tid.pce.computingEngine;

import java.util.concurrent.FutureTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.tid.pce.computingEngine.algorithms.AlgorithmReservation;
import es.tid.pce.computingEngine.algorithms.ComputingAlgorithm;

public class ComputingTask extends FutureTask<ComputingResponse> {
	ComputingAlgorithm algorithm;
	private Logger log;
	
	public ComputingTask(ComputingAlgorithm algorithm) {
		super(algorithm);
		this.log=LoggerFactory.getLogger("PCEServer.log");
		this.algorithm=algorithm;
	}
	
	public ComputingResponse executeReservation(){
		
		AlgorithmReservation res=algorithm.getReserv();
		if (res!=null){
			log.info("Comienza la reserva");
			ComputingResponse resp;
			try {
				resp = algorithm.getReserv().call();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				return null;
			}
			log.info("Acaba la reserva");
			return resp;
		}else {
			return null;
		}
		
	}

}
