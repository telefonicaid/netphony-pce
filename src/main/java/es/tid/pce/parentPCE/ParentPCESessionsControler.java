package es.tid.pce.parentPCE;

import java.util.HashSet;
import java.util.logging.Logger;

public class ParentPCESessionsControler extends SecurityManager  {
	
	private boolean acceptAll=true;
	
	private HashSet<String> acceptList;
	
	private HashSet<String> blackList;
	
	private HashSet<String> activeSessionList;
	
	private int activeSessions=0;
	
	private int maxSessions=1;
	
	private Logger log;
	
	public ParentPCESessionsControler(){
		super();
		log = Logger.getLogger("PCEServer");
		acceptList=new HashSet<String>();
		blackList=new HashSet<String>();
		activeSessionList=new HashSet<String>();		
	}
	
	/**
	 * Checks if the session is accepted
	 */
	public void checkAccept(String host,int port) throws SecurityException,NullPointerException{
		log.info("checkAccept host "+host+" port "+port);
		if (port==4189){
			//First, check by IP
			if (acceptAll){
				//If acceptAll is set (true by default...) then check the black List
				if (blackList.contains(host)){
					super.checkAccept(host,port);
				}
			}
			else {
				if (!(acceptList.contains(host))){
					super.checkAccept(host,port);
				}
			}
			if (activeSessions>=maxSessions){
				super.checkAccept(host,port);
			}
			else {
				if (activeSessionList.contains(host)){
					super.checkAccept(host,port);
				}
				log.info("Session admitted!!");
				activeSessionList.add(host);
				activeSessions=activeSessions+1;				
			}

		}
				
	}
	
	public void removeSession(String host){
		activeSessionList.remove(host);	
		activeSessions=activeSessions-1;
		
	}

}