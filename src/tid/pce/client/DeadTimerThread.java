package tid.pce.client;

import java.util.logging.Logger;

import tid.pce.pcep.objects.ObjectParameters;

/*
 * PCEP DeadTimer management Thread
 * 
 * @author Oscar Gonzalez de Dios (Dec 2010)
 * @author Carlos Garcia Argos (cgarcia@novanotio.es) (Feb. 11 2010)
 * 
 */
public class DeadTimerThread extends Thread {
	private PCCPCEPSession pceSession = null;
	private int deadTimer = 0;
	private Logger log;
	private boolean running;
	
	public DeadTimerThread(PCCPCEPSession p, int d) {
		this.deadTimer = d;
		this.pceSession = p;
		log=Logger.getLogger("PCEServer");
	}
	
	public void run() {
		running=true;
		while (running) {
			try {
				sleep(deadTimer * 1000);
				/*
				 * Time's over, close PCEP Session
				 */
				log.warning("DeadTimer OVER");
				this.pceSession.close(ObjectParameters.REASON_DEADTIMER);
				return;
			} catch (InterruptedException e) {
				//return;
				if (running==false){
					log.warning("Ending DeadTimerThread");
					return;
				}
				else {
					log.fine("Reseting Dead Timer");
				}
			} catch (Exception e) {
				//FIXME: Ver que hacer aqui, por ahora, solo saco un log
			    log.warning("Unhandled exception: " + e.getMessage());
			}

		}
		
		
	}
	
	public void stopRunning(){
		running=false;
	}
}
