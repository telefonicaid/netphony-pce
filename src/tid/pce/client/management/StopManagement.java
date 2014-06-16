package tid.pce.client.management;

import java.util.TimerTask;

import tid.pce.client.emulator.AutomaticTesterStatistics;
import tid.pce.client.emulator.Emulator;

public class StopManagement extends TimerTask {

	private long blocksNumber;

	private AutomaticTesterStatistics ats;

		private int numberIterations;
		private Emulator emulator;
		
	public StopManagement(Emulator emulator,int numberIterations, long blocksNumber,AutomaticTesterStatistics ats){
		this.blocksNumber=blocksNumber;
		this.numberIterations=numberIterations;
		this.ats=ats;
		this.emulator=emulator;
	}
	
	public void run(){	
		if (/*(stopConditionBlockProbabilityConvergence())||*/(maxNumberIterations()))/*(stopCondition(Long.parseLong(ats.getBlockProbability().result())))||*/{			
			emulator.stop();
		}
	}
//	public boolean stopCondition(Long blocksNumber){
//		Long longs = Long.valueOf(this.blocksNumber);
//		if (blocksNumber.compareTo(longs) ){			
//			return true;
//		}
//		return false;
//	}
	public boolean stopConditionBlockProbabilityConvergence(){
		if (ats.getBlockProbability().getConverge() ==  1){
			return true;
		}
		return false;
	}
	public boolean stopConditionMeanReqTimeConvergence(){
		if (ats.getMeanReqTime().getConverge() == 1){
			return true;
		}
		return false;
	}
	
	
	public boolean maxNumberIterations(){
		if (this.numberIterations < ats.getNumRequests()){
			return true;
		}
		return false;
	}

	 
}
