package es.tid.pce.server.communicationpce;

import java.io.DataOutputStream;
import java.net.Inet4Address;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.logging.Logger;

import es.tid.pce.pcepsession.GenericPCEPSession;

public class OpenedSessionsManager {
	private Logger log;
	/**
	 * Relates remote PCE-PCC address with the Session information  
	 */
	private Hashtable<Inet4Address,PCESessionInformation> addressPCESessionInfo;

	/**
	 * Prueba inicial
	 */
	private LinkedList<PCESessionInformation> sessionInfoList;
	public OpenedSessionsManager(){
		addressPCESessionInfo=new Hashtable<Inet4Address,PCESessionInformation>();
		sessionInfoList=new LinkedList<PCESessionInformation>();
		log=Logger.getLogger("PCEServer");
	}

	
	public void registerNewSession(GenericPCEPSession pceSession, int roll){
		PCESessionInformation info= new PCESessionInformation();
		info.setRollSession(roll);
		info.setOut(pceSession.getOut());
		sessionInfoList.add(info);
		//addressPCESessionInfo.put(pceSession.getRemotePCEId(),info);
	}
	
	public void registerNewSession(/*Inet4Address domain,*/ Inet4Address address, DataOutputStream out,int roll){
	
		PCESessionInformation info= new PCESessionInformation();
		info.setRollSession(roll);
		info.setOut(out);
		
		if (address!=null){
			log.info("pceId "+address);
			if (addressPCESessionInfo.containsKey(address)){
				addressPCESessionInfo.remove(address);
			}
			addressPCESessionInfo.put(address, info);
					
		}
		else {
			
			log.warning("Address is null, impossible to register a new session");
		}
	}
	
	public void registerNewSession( DataOutputStream out,int roll){
		log.info("Register new session");
		PCESessionInformation info= new PCESessionInformation();
		info.setRollSession(roll);
		info.setOut(out);
		sessionInfoList.add(info);
	}
	public void removeSession(GenericPCEPSession pceSession){
		
	//	PCESessionInfoList.remove(info);		
	}


	public Hashtable<Inet4Address, PCESessionInformation> getAddressPCESessionInfo() {
		return addressPCESessionInfo;
	}


	public void setAddressPCESessionInfo(
			Hashtable<Inet4Address, PCESessionInformation> addressPCESessionInfo) {
		this.addressPCESessionInfo = addressPCESessionInfo;
	}


	public LinkedList<PCESessionInformation> getSessionInfoList() {
		return sessionInfoList;
	}


	public void setSessionInfoList(LinkedList<PCESessionInformation> sessionInfoList) {
		this.sessionInfoList = sessionInfoList;
	}

	
	
	
}
