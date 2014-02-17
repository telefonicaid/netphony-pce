package tid.pce.client.tester;

import java.util.Hashtable;

public class PCCPCEPSessionParameters {
	private Hashtable<Integer,Integer> PCEServerPortList;
	private Hashtable<Integer,String> ipPCEList;
	private boolean noDelay = true;
	/**
	 * Numero de PCEP sesiones que abrimos entre clientes y PCES 
	 */
	private int numSessions=1;

	
	public Hashtable<Integer, Integer> getPCEServerPortList() {
		return PCEServerPortList;
	}
	public void setPCEServerPortList(Hashtable<Integer, Integer> pCEServerPortList) {
		PCEServerPortList = pCEServerPortList;
	}
	public Hashtable<Integer, String> getIpPCEList() {
		return ipPCEList;
	}
	public void setIpPCEList(Hashtable<Integer, String> ipPCEList) {
		this.ipPCEList = ipPCEList;
	}
	public boolean isNoDelay() {
		return noDelay;
	}
	public void setNoDelay(boolean noDelay) {
		this.noDelay = noDelay;
	}
	public int getNumSessions() {
		return numSessions;
	}
	public void setNumSessions(int numSessions) {
		this.numSessions = numSessions;
	}
	

	
}
