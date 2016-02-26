package es.tid.pce.server.management;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.logging.Logger;

import es.tid.pce.computingEngine.RequestDispatcher;
import es.tid.pce.server.PCEServerParameters;
import es.tid.pce.server.communicationpce.CollaborationPCESessionManager;
import es.tid.pce.server.wson.ReservationManager;
import es.tid.tedb.DomainTEDB;


public class PCEManagementSever extends Thread {
	
	private Logger log;
		
	private RequestDispatcher requestDispatcher;
	
	private DomainTEDB tedb;
	 
	private PCEServerParameters params;
	
	private ReservationManager reservationManager;
	private CollaborationPCESessionManager collaborationPCESessionManager;

	
	public PCEManagementSever(RequestDispatcher requestDispatcher, DomainTEDB tedb, PCEServerParameters params, ReservationManager reservationManager){
		log =Logger.getLogger("PCEServer");
		this.requestDispatcher=requestDispatcher;
		this.tedb=tedb;
		this.params = params;
		this.reservationManager=reservationManager;
		
	}
	
	public PCEManagementSever(RequestDispatcher requestDispatcher, DomainTEDB tedb, PCEServerParameters params, ReservationManager reservationManager,CollaborationPCESessionManager collaborationPCESessionManager){
		log =Logger.getLogger("PCEServer");
		this.requestDispatcher=requestDispatcher;
		this.tedb=tedb;
		this.params = params;
		this.reservationManager=reservationManager;
		this.collaborationPCESessionManager= collaborationPCESessionManager;
	}
	
	public void run(){
	    ServerSocket serverSocket = null;
	    boolean listening=true;
		try {
	      	  log.info("Listening on port "+params.getPCEManagementPort());	
	          serverSocket = new ServerSocket(params.getPCEManagementPort(), 0,(Inet4Address) InetAddress.getByName(params.getLocalPceAddress()));
		  }
		catch (Exception e){
			 log.severe("Could not listen management on port "+params.getPCEManagementPort());
			e.printStackTrace();
			return;
		}
		
		try {
	       	while (listening) {
	       		new PCEManagementSession(serverSocket.accept(),requestDispatcher, tedb,reservationManager,collaborationPCESessionManager, params).start();
	       	}	    
	       	serverSocket.close();
		} catch (Exception e) {
	       	e.printStackTrace();
		}				
	}
}