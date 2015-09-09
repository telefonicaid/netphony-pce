package es.tid.pce.parentPCE;

import java.net.Inet4Address;
import java.util.Hashtable;

import es.tid.pce.pcep.constructs.StateReport;

public class MD_LSP {
	
	public  int pLSPID;
	
	public Hashtable<Inet4Address,Integer> domainLSPIDMap;//mapping of domain - pLSPID in the domain
	
	public Hashtable<Inet4Address,StateReport>domainLSRMpa; //mapping of domain - State Report 

	public int getpLSPID() {
		return pLSPID;
	}

	public void setpLSPID(int pLSPID) {
		this.pLSPID = pLSPID;
	}

	public Hashtable<Inet4Address, Integer> getDomainLSPIDMap() {
		return domainLSPIDMap;
	}

	public void setDomainLSPIDMap(Hashtable<Inet4Address, Integer> domainLSPIDMap) {
		this.domainLSPIDMap = domainLSPIDMap;
	}

	public Hashtable<Inet4Address, StateReport> getDomainLSRMpa() {
		return domainLSRMpa;
	}

	public void setDomainLSRMpa(Hashtable<Inet4Address, StateReport> domainLSRMpa) {
		this.domainLSRMpa = domainLSRMpa;
	}
	
	

}
