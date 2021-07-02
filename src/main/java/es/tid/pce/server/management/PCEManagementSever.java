package es.tid.pce.server.management;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.SocketException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.tid.pce.computingEngine.RequestDispatcher;
import es.tid.pce.pcepsession.PCEPSessionsInformation;
import es.tid.pce.server.DomainPCEServer;
import es.tid.pce.server.IniPCCManager;
import es.tid.pce.server.PCEServerParameters;
import es.tid.pce.server.communicationpce.CollaborationPCESessionManager;
import es.tid.pce.server.wson.ReservationManager;
import es.tid.tedb.DomainTEDB;


public class PCEManagementSever extends Thread {
	
	private Logger log;
	
	/**
	 * Reference to the domain PCE to access all components
	 */
	private DomainPCEServer domainPCEServer;

	/**
	 * Serversocket to receive management connections.
	 */
	private ServerSocket serverSocket;
	
	/**
	 * If the server is listening to management connections
	 */
	private boolean listening=true;
	
	
	public PCEManagementSever( DomainPCEServer domainPCEServer ){
		log =LoggerFactory.getLogger("PCEServer");
		this.domainPCEServer=domainPCEServer;
	}
		
	public void run(){
	    
	    
		try {
	      	  log.info("Listening on port "+domainPCEServer.getParams().getPCEManagementPort());	
	          serverSocket = new ServerSocket(domainPCEServer.getParams().getPCEManagementPort(), 0,(Inet4Address) InetAddress.getByName(domainPCEServer.getParams().getLocalPceAddress()));
		  }
		catch (Exception e){
			 log.error("Could not listen management on port "+domainPCEServer.getParams().getPCEManagementPort());
			e.printStackTrace();
			return;
		}
		
		try {
	       	while (listening) {
	       		new PCEManagementSession(serverSocket.accept(),domainPCEServer).start();
	       	}	    
	       	serverSocket.close();
		}catch (SocketException e) {
			if (listening==false){
				log.info("Socket closed due to controlled close");
			}else {
				log.error("Problem with the socket, exiting");
				e.printStackTrace();
			}
		}  catch (Exception e) {
	       	e.printStackTrace();
		}				
	}
	
	public void stopServer(){
		listening=false;
		if (serverSocket!=null){
			try {
				serverSocket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	
	}
}
