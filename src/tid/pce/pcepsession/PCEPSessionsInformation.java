package tid.pce.pcepsession;

import java.io.DataOutputStream;
import java.util.Enumeration;
import java.util.Hashtable;

import tid.pce.server.lspdb.LSP_DB;



public class PCEPSessionsInformation {
	
	public Hashtable<Long,GenericPCEPSession> sessionList;
	
	private boolean isStateful = false;
	
	private boolean isActive = false;
		
	//FIXME: 
	int rollSession;//PCEBackup, 
	//Datos de la sesion que nos interesen
	//FIXME: ya lo tenemos a trav�s de la lista de sesiones
	 DataOutputStream out;
	
	public PCEPSessionsInformation(){
		sessionList=new Hashtable<Long,GenericPCEPSession>();
	}
	public void addSession(long sessionId, GenericPCEPSession session){
		sessionList.put(new Long(sessionId),session);
	}
	
	public void deleteSession(long sessionId){
		sessionList.remove(new Long(sessionId));
	}
	
	public String toString(){
		StringBuffer sb=new StringBuffer(20+sessionList.size()*100);
		sb.append(sessionList.size());
		sb.append(" active sessions\n");
		Enumeration<GenericPCEPSession> enumSessionList=sessionList.elements();
		while (enumSessionList.hasMoreElements()){
			sb.append(enumSessionList.nextElement().shortInfo());
			sb.append("\r\n");
		}
		return sb.toString();
	}
	
	public String printSession(long sessionId){
		GenericPCEPSession ses=sessionList.get(new Long(sessionId));
		if (ses!=null){
			return ses.toString();
		}else {
			return "session "+sessionId+" does not exist";
		}
	}
	
	
	 
	public DataOutputStream getOut() {
		return out;
	}

	public void setOut(DataOutputStream out) {
		this.out = out;
	}

	public int getRollSession() {
		return rollSession;
	}

	public void setRollSession(int rollSession) {
		this.rollSession = rollSession;
	}
	
	public boolean isStateful() {
		return isStateful;
	}
	
	public void setStateful(boolean isStateful) {
		this.isStateful = isStateful;
	}
	
	public boolean isActive() {
		return isActive;
	}
	
	public void setActive(boolean isActive) {
		this.isActive = isActive;
	}
}
