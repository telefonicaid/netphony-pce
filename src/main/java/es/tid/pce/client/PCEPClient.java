package es.tid.pce.client;

import java.util.Vector;

import es.tid.pce.pcepsession.PCEPSessionsInformation;

public class PCEPClient {
	
	private Vector<PCCPCEPSession> sesiones; 
	@SuppressWarnings("unused")
	private int sessionIdCounter;

	public PCEPClient(){
		sessionIdCounter=0;
		sesiones=new Vector<PCCPCEPSession>();
	}
	public void addPCE(String ip_pce, int port_pce){
		PCCPCEPSession PCEsession = new PCCPCEPSession(ip_pce, port_pce,false, new PCEPSessionsInformation());
		sesiones.add(PCEsession);
		//PCEsession.open();
	}
	
	
	public void getRoute(){
		
	}
	
//	public PCCConnect getPCCConnect(){
//		
//		return PCESessions.elementAt(0);
//	}
	
//	private int getNewSessionId(){
//		sessionIdCounter=sessionIdCounter+1;
//		if (sessionIdCounter>100000){
//			sessionIdCounter=1;
//		}
//		return sessionIdCounter;
//	}
//	
		

}
