package tid.pce.server;

import java.util.logging.Logger;

public class PCEPServerUserInterface extends Thread {
	
	PCEServerParameters ps;
	boolean running;
	
	public PCEPServerUserInterface (PCEServerParameters ps){
		this.ps=ps;
	}
	
	public void run() {
		running=true;
		while (running) {
			
		}
	}

}
