package tid.pce.computingEngine;

import java.util.concurrent.FutureTask;
import java.util.logging.Logger;

import tid.pce.computingEngine.algorithms.AlgorithmReservation;
import tid.pce.computingEngine.algorithms.ComputingAlgorithm;
import tid.pce.pcep.messages.PCEPResponse;

public class ComputingTask extends FutureTask<PCEPResponse> {
	ComputingAlgorithm algorithm;
	private Logger log;
	
	public ComputingTask(ComputingAlgorithm algorithm) {
		super(algorithm);
		this.log=Logger.getLogger("PCEServer.log");
		this.algorithm=algorithm;
	}
	
	public PCEPResponse executeReservation(){
		
		AlgorithmReservation res=algorithm.getReserv();
		if (res!=null){
			log.info("Comienza la reserva");
			PCEPResponse resp;
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
