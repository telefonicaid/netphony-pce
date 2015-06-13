package es.tid.pce.server;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;

import es.tid.pce.pcep.PCEPProtocolViolationException;
import es.tid.pce.pcep.messages.PCEPMessage;

public class Sender extends Thread {
	
	/**
	 * Queue to read the messages to send to the PCE peer
	 */
	private LinkedBlockingQueue<PCEPMessage> sendingQueue;
	/**
	 * Used to send messages to the PCE peer
	 */
	private DataOutputStream out=null; 
	
	public Sender(LinkedBlockingQueue<PCEPMessage> sendingQueue,DataOutputStream out ){
		this.out=out;
		this.sendingQueue=sendingQueue;
	}
	
	public void run(){
		PCEPMessage msg;
		while (true){
			try {
				msg=sendingQueue.take();
			} catch (InterruptedException e) {			
				return;
			}
			
			try {
				msg.encode();
				out.write(msg.getBytes());
			} catch (PCEPProtocolViolationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();			
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return;
			}
		}
	}

}
