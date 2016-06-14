package es.tid.pce.server.communicationpce;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.tid.pce.pcep.PCEPProtocolViolationException;
import es.tid.pce.pcep.messages.PCEPMessage;


public class CollaborationPCESessionManager {
	//Referencia a la sesion o al outputStream
	/**
	 * DataOutputStream to send messages to the backup PCE
	 */
	//private LinkedList<DataOutputStream> outList=null; 
	private OpenedSessionsManager openedSessionsManager=null;
//	private Logger log;
	public CollaborationPCESessionManager(){
		openedSessionsManager= new OpenedSessionsManager();
		/*Creamos el logger donde vamos a escribir los attemps*/
		//FileHandler fh1;
	
//		log = LoggerFactory.getLogger("CollaborationPCESessionManager");
//		try {
//			fh1=new FileHandler("CollaborationPCESessionManager.log");
//			fh1.setFormatter(new SimpleFormatter());
//			log.addHandler(fh1);
//			log.setLevel(Level.ALL);
//		} catch (SecurityException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
	}
	
	synchronized public  void sendNotifyMessage(PCEPMessage msg){
		try {
			msg.encode();
		} catch (PCEPProtocolViolationException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try {
			//log.fine("Sending message");			
			for (int i=0;i<openedSessionsManager.getSessionInfoList().size();i++){
				if (openedSessionsManager.getSessionInfoList().get(i).getRollSession() == RollSessionType.COLLABORATIVE_PCE){
					openedSessionsManager.getSessionInfoList().get(i).getOut().write(msg.getBytes());
					openedSessionsManager.getSessionInfoList().get(i).getOut().flush();
				}
			}
		} catch (IOException e) {
			//log.warn("Error sending msg: " + e.getMessage());
		
		}
	}


	public OpenedSessionsManager getOpenedSessionsManager() {
		return openedSessionsManager;
	}


	public void setOpenedSessionsManager(OpenedSessionsManager openedSessionsManager) {
		this.openedSessionsManager = openedSessionsManager;
	}
	
}
