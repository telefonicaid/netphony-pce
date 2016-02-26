package es.tid.pce.server;


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
