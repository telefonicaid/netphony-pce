package es.tid.pce.parentPCE.management;

import java.net.ServerSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.tid.pce.computingEngine.RequestDispatcher;
import es.tid.pce.parentPCE.ChildPCERequestManager;
import es.tid.pce.parentPCE.MultiDomainTopologyUpdater;
import es.tid.pce.parentPCE.MDLSPDB.MultiDomainLSPDB;
import es.tid.pce.pcepsession.PCEPSessionsInformation;
import es.tid.tedb.ITMDTEDB;
import es.tid.tedb.MDTEDB;
import es.tid.tedb.ReachabilityManager;
import es.tid.tedb.SimpleTEDB;

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
	
	private MultiDomainLSPDB multiDomainLSPDB;
	
	private SimpleTEDB simpleTedb;
	
	private ITMDTEDB ITmdtedb;
	
	private ReachabilityManager rm;
	
	private boolean isITcapable=false;
	
	private PCEPSessionsInformation pcepSessionManager;
	
	private MultiDomainTopologyUpdater mdtu;
	
	private int parentPCEManagementPort;
	
	public ParentPCEManagementSever(ChildPCERequestManager cprm, RequestDispatcher requestDispatcher, MDTEDB mdtedb,SimpleTEDB simpleTEDB, ReachabilityManager rm, PCEPSessionsInformation pcepSessionManager,MultiDomainTopologyUpdater mdtu,int parentPCEManagementPort, MultiDomainLSPDB multiDomainLSPDB){
		log =LoggerFactory.getLogger("PCEServer");
		this.cprm=cprm;
		this.requestDispatcher=requestDispatcher;
		this.mdtedb=mdtedb;
		this.simpleTedb=simpleTEDB;
		this.rm=rm;
		isITcapable=false;
		this.pcepSessionManager=pcepSessionManager;
		this.mdtu=mdtu;
		this.parentPCEManagementPort=parentPCEManagementPort;
		this.multiDomainLSPDB=multiDomainLSPDB;
	}
	
	
	public ParentPCEManagementSever(ChildPCERequestManager cprm, RequestDispatcher requestDispatcher, ITMDTEDB ITmdtedb, ReachabilityManager rm,PCEPSessionsInformation pcepSessionManager,MultiDomainTopologyUpdater mdtu,int parentPCEManagementPort){
		log =LoggerFactory.getLogger("PCEServer");
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
			 log.error("Could not listen management on port"+ parentPCEManagementPort);
			e.printStackTrace();
			return;
		}
		
		   try {
	        	while (listening) {
	        		if(isITcapable){
	        			new ParentPCEManagementSession(serverSocket.accept(),cprm,requestDispatcher, ITmdtedb,rm,pcepSessionManager,mdtu).start();
	        		}else{
	        			new ParentPCEManagementSession(serverSocket.accept(),cprm,requestDispatcher, mdtedb,simpleTedb,rm,pcepSessionManager,mdtu, multiDomainLSPDB).start();	
	        		}
	        	}
	        	serverSocket.close();
	        } catch (Exception e) {
	        	e.printStackTrace();
	        }				
	}
	  
	  

}
