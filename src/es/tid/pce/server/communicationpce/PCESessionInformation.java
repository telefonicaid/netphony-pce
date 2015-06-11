package es.tid.pce.server.communicationpce;

import java.io.DataOutputStream;

public class PCESessionInformation {
	int rollSession;//PCEBackup, 
	//Datos de la sesion que nos interesen
	 DataOutputStream out;
	 
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
	
	
	
}
