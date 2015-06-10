package tid.vntm.management;

import java.net.ServerSocket;
import java.util.logging.Logger;

import tid.vntm.LSPManager;

public class VNTMManagementSever extends Thread {
	
	private Logger log;
	private LSPManager lspmanager;
	int port;
	public VNTMManagementSever( LSPManager lspmanager,int port){
		log =Logger.getLogger("PCEServer");
		this.lspmanager=lspmanager;
	}
	
	public void run(){
	    ServerSocket serverSocket = null;
	    boolean listening=true;
		try {
	      	  log.info("Listening on port "+port);	
	          serverSocket = new ServerSocket(port);
		  }
		catch (Exception e){
			 log.severe("Could not listen management on port "+port);
			e.printStackTrace();
			return;
		}
		
		   try {
	        	while (listening) {
	        		new VNTMManagementSession(serverSocket.accept(),lspmanager).start();
	        	}
	        	serverSocket.close();
	        } catch (Exception e) {
	        	e.printStackTrace();
	        }				
	}
	  
}
