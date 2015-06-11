package tid.pce.parentPCE.management;

import java.net.ServerSocket;
import java.util.logging.Logger;

import tid.pce.computingEngine.RequestDispatcher;
import tid.pce.parentPCE.ChildPCERequestManager;
import tid.pce.parentPCE.MultiDomainTopologyUpdater;
import tid.pce.pcepsession.PCEPSessionsInformation;
import tid.pce.tedb.ITMDTEDB;
import tid.pce.tedb.MDTEDB;
import tid.pce.tedb.ReachabilityManager;
import tid.pce.tedb.SimpleTEDB;

/**
 * Parent PCE Management Server.
 * Creates a new management session for each incoming connection to the management port.
 * @author ogondio
 *
 */
public class ParentPCEManagementSever extends Thread {
	
	private Logger log;
	
	private ChildPCERequestManager cprm;
	
	private RequestDispatcher requestDispatcher;
	
	private MDTEDB mdtedb;
	
	private SimpleTEDB simpleTedb;
	
	private ITMDTEDB ITmdtedb;
	
	private ReachabilityManager rm;
	
	private boolean isITcapable=false;
	
	private PCEPSessionsInformation pcepSessionManager;
	
	private MultiDomainTopologyUpdater mdtu;
	
	private int parentPCEManagementPort;
	
	public ParentPCEManagementSever(ChildPCERequestManager cprm, RequestDispatcher requestDispatcher, MDTEDB mdtedb,SimpleTEDB simpleTEDB, ReachabilityManager rm, PCEPSessionsInformation pcepSessionManager,MultiDomainTopologyUpdater mdtu,int parentPCEManagementPort){
		log =Logger.getLogger("PCEServer");
		this.cprm=cprm;
		this.requestDispatcher=requestDispatcher;
		this.mdtedb=mdtedb;
		this.simpleTedb=simpleTEDB;
		this.rm=rm;
		isITcapable=false;
		this.pcepSessionManager=pcepSessionManager;
		this.mdtu=mdtu;
		this.parentPCEManagementPort=parentPCEManagementPort;
	}
	
	
	public ParentPCEManagementSever(ChildPCERequestManager cprm, RequestDispatcher requestDispatcher, ITMDTEDB ITmdtedb, ReachabilityManager rm,PCEPSessionsInformation pcepSessionManager,MultiDomainTopologyUpdater mdtu,int parentPCEManagementPort){
		log =Logger.getLogger("PCEServer");
		this.cprm=cprm;
		this.requestDispatcher=requestDispatcher;
		this.ITmdtedb=ITmdtedb;
		this.rm=rm;
		isITcapable=true;
		this.pcepSessionManager=pcepSessionManager;
		this.mdtu=mdtu;
		this.parentPCEManagementPort=parentPCEManagementPort;
	}
	
	
	public void run(){
	    ServerSocket serverSocket = null;
	    boolean listening=true;
		try {
	      	  log.info("Listening on port "+ parentPCEManagementPort);	
	          serverSocket = new ServerSocket(parentPCEManagementPort);
		  }
		catch (Exception e){
			 log.severe("Could not listen management on port"+ parentPCEManagementPort);
			e.printStackTrace();
			return;
		}
		
		   try {
	        	while (listening) {
	        		if(isITcapable){
	        			new ParentPCEManagementSession(serverSocket.accept(),cprm,requestDispatcher, ITmdtedb,rm,pcepSessionManager,mdtu).start();
	        		}else{
	        			new ParentPCEManagementSession(serverSocket.accept(),cprm,requestDispatcher, mdtedb,simpleTedb,rm,pcepSessionManager,mdtu).start();	
	        		}
	        	}
	        	serverSocket.close();
	        } catch (Exception e) {
	        	e.printStackTrace();
	        }				
	}
	  
	  

}
