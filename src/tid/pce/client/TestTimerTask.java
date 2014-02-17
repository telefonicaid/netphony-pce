package tid.pce.client;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import cern.jet.random.Exponential;

public class TestTimerTask extends TimerTask{

	long prevTime;
	long expecedTime;
	Exponential expSendRequest;
	Timer timer;
	
	public TestTimerTask(long time, long expecedTime, Exponential expSendRequest, Timer timer){
		prevTime=time;
		this.expecedTime=expecedTime;
		this.expSendRequest=expSendRequest;
		this.timer=timer;
	}
	@Override
	public void run() {
		double timeNextReqD=expSendRequest.nextDouble(); 
		long timeNextReq =(long)timeNextReqD;		
		TestTimerTask exponentialTester = new TestTimerTask(System.currentTimeMillis(),timeNextReq,expSendRequest,timer);
		Date date = new Date(System.currentTimeMillis()+ timeNextReq); 
		//log.info("Scheduling next request in "+timeNextReq+" MS ("+timeNextReqD+" )");
		timer.schedule(exponentialTester,date);
		// TODO Auto-generated method stub
		long newTime=System.currentTimeMillis();
		long timet=newTime-prevTime;
		
		System.out.println("expecedTime "+expecedTime +" realTime "+timet);
	}
	

}
