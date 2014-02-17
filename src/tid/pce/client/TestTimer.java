package tid.pce.client;

import java.util.Date;
import java.util.Timer;

import cern.jet.random.Exponential;
import cern.jet.random.engine.MersenneTwister;

public class TestTimer {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		 MersenneTwister mersenneTwisterSendRequest = new MersenneTwister();
		 double lambdaSendRequest=1/(double)10000;
		 Exponential expSendRequest = new Exponential(lambdaSendRequest, mersenneTwisterSendRequest);
		double timeNextReqD=expSendRequest.nextDouble();
		Timer timer=new Timer();
		long timeNextReq =(long)timeNextReqD;		
		TestTimerTask exponentialTester = new TestTimerTask(System.currentTimeMillis(),timeNextReq,expSendRequest,timer);
		Date date = new Date(System.currentTimeMillis()+ timeNextReq); 
		//log.info("Scheduling next request in "+timeNextReq+" MS ("+timeNextReqD+" )");
		timer.schedule(exponentialTester,date);
		// TODO Auto-generated method stub
		
	}

}
