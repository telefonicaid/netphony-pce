package es.tid.pce.parentPCE;

import java.net.Inet4Address;
import java.util.Enumeration;
import java.util.Hashtable;

import es.tid.pce.pcep.constructs.StateReport;

public class MD_LSP {
	
	public  int pLSPID;
	
	public Hashtable<Inet4Address,Integer> domainLSPIDMap;//mapping of domain - pLSPID in the domain
	
	public Hashtable<Inet4Address,StateReport>domainLSRMpa; //mapping of domain - State Report 
	
	public MD_LSP(){
		domainLSPIDMap=new Hashtable<Inet4Address,Integer>();
		domainLSRMpa=new Hashtable<Inet4Address,StateReport>();
		
	}

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
	
	public String toString() {
		String ret="";
		Enumeration<Inet4Address> enu=domainLSPIDMap.keys();
		while (enu.hasMoreElements()){
			ret=ret+"Dom: "+enu.nextElement();
		}
		return ret;
	}
	
	

}
