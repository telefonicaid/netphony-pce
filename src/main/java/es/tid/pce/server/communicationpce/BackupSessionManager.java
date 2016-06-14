package es.tid.pce.server.communicationpce;


import java.util.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.tid.pce.pcepsession.PCEPSessionsInformation;
import es.tid.pce.server.NotificationDispatcher;
import es.tid.pce.server.PCEServerParameters;
import es.tid.tedb.TEDB;

public class BackupSessionManager{
	private Logger log;
	private PCEServerParameters params;
	private TEDB ted;
	CollaborationPCESessionManager collaborationPCESessionManager;
	NotificationDispatcher notificationDispatcher;
	PCEPSessionsInformation pcepSessionInformation;
	
	public BackupSessionManager(PCEServerParameters params, TEDB ted,CollaborationPCESessionManager collaborationPCESessionManager,NotificationDispatcher notificationDispatcher, PCEPSessionsInformation pcepSessionInformation){
		this.params=params;
		this.ted=ted;
		this.collaborationPCESessionManager=collaborationPCESessionManager;
		log=LoggerFactory.getLogger("PCCClient");
		this.notificationDispatcher= notificationDispatcher;
	}
	public void manageBackupPCESession(){	
		BackupSessionManagerTask backupSessionManagerThread;
		//For the session between the Domain (Child) PCE and the parent PCE
		log.info("Initializing Manager of the primary PCE - Backup PCE Session");			
		backupSessionManagerThread=new BackupSessionManagerTask(params, ted,collaborationPCESessionManager,notificationDispatcher, pcepSessionInformation);
		Timer timer=new Timer();
		log.info("Inizializing Session with Primary PCE");
		timer.schedule(backupSessionManagerThread, 0, 100000);
	}
}
