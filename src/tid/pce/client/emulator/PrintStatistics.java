package tid.pce.client.emulator;

import java.util.TimerTask;
import java.util.logging.Logger;



/**
 * Print statistics to write in a file the emulation statistics 
 * Es una tarea que se ejecutara externamente
 * @author mcs
 *
 */
public class PrintStatistics   extends TimerTask {

	AutomaticTesterStatistics ats;
	private Logger statsLog;

	public PrintStatistics(AutomaticTesterStatistics ats){
		statsLog=Logger.getLogger("stats");
		this.ats=ats;
	}

	public void run(){
		statsLog.info(ats.print());
	}
	
}


