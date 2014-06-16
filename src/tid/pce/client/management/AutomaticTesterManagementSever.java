package tid.pce.client.management;

import java.net.ServerSocket;
import java.util.Timer;
import java.util.logging.Logger;

import tid.pce.client.emulator.AutomaticTesterStatistics;
import tid.pce.client.emulator.Emulator;
import tid.pce.client.tester.InformationRequest;

public class AutomaticTesterManagementSever extends Thread {
	
	private Logger log;
		
	private Timer timer;
	private Timer printStatisticsTimer;
//	private Timer planificationTimer;
	private AutomaticTesterStatistics stats;
	

	private InformationRequest informationRequest;
	
	private Emulator emulator;

//	private PCCPCEPSession PCEsession;
//	private PCCPCEPSession PCEsessionVNTM;
	

	
	public AutomaticTesterManagementSever(/*PCCPCEPSession PCEsession,PCCPCEPSession PCEsessionVNTM,*/Timer timer, Timer printStatisticsTimer, /*Timer planificationTimer,*/AutomaticTesterStatistics stats,  InformationRequest info){
		log =Logger.getLogger("PCCClient");		
		this.timer = timer;
		this.stats=stats;
		this.informationRequest=info;	
		this.printStatisticsTimer =printStatisticsTimer;
//		this.planificationTimer=planificationTimer;
//		this.PCEsession=PCEsession;
//		this.PCEsessionVNTM=PCEsessionVNTM;
		
	}
	
	public AutomaticTesterManagementSever(Emulator emulator){
		log =Logger.getLogger("PCCClient");	
		this.emulator=emulator;
	}
	
	
	public void run(){
		ServerSocket serverSocket = null;
		boolean listening=true;
		int port =emulator.getTesterParams().getManagementClientPort();
		try {			
			log.info("Listening on port "+port);	
			serverSocket = new ServerSocket(port);
		}
		catch (Exception e){
			log.severe("Could not listen management on port "+port);
			e.printStackTrace();
			return;
		}

		try {
			while (listening) {
				if (emulator==null)
					new AutomaticTesterManagementSession(serverSocket.accept(),timer,printStatisticsTimer,stats,/*PCEsession,PCEsessionVNTM,*/informationRequest,informationRequest.calculateLoad()).start();
				else
					new AutomaticTesterManagementSession(serverSocket.accept(),emulator);
			}
			serverSocket.close();
		} catch (Exception e) {
			e.printStackTrace();
		}				
	}

}
