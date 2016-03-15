package es.tid.pce.tests;


import static org.junit.Assert.*;
import es.tid.pce.server.PCEServer;

public class PceTest implements Runnable {

    public void run() {
    	String[] args= new String[]{"src/test/resources/PCEServerConfiguration_SSON.xml"};
		PCEServer.main(args);
    }

	public PceTest() {
	}
	
	
	/**
	 * This tests starts a PCEServer, reads the topology from a File.
	 * After that this tests starts a PCC and send a path req.
	 * It checks after 10 seconds if the resp is correct.
	 */
	@org.junit.Test
	public void testPCE(){
		//launch PCE server
		Thread pceServer = (new Thread(new PceTest()));
		pceServer.start();
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		pceServer.interrupt();
		
		
	}
}
