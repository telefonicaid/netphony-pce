package es.tid.pce.server;

import java.net.Inet4Address;
import java.util.Enumeration;
import java.util.Hashtable;

import es.tid.pce.pcep.constructs.StateReport;
import es.tid.pce.pcep.objects.ExplicitRouteObject;

public class SD_LSP {
	
	public  int pLSPID;
	
	public StateReport stateRport; //mapping of domain - State Report 
	
	public ExplicitRouteObject fullERO;
	
	public SD_LSP(){
	}

	public int getpLSPID() {
		return pLSPID;
	}

	public void setpLSPID(int pLSPID) {
		this.pLSPID = pLSPID;
	}

	
	public StateReport getStateRport() {
		return stateRport;
	}

	public void setStateRport(StateReport stateRport) {
		this.stateRport = stateRport;
	}

	public String toString() {
		String ret="";
		
		return ret;
	}

	public ExplicitRouteObject getFullERO() {
		return fullERO;
	}

	public void setFullERO(ExplicitRouteObject fullERO) {
		this.fullERO = fullERO;
	}
	
	
	
	

}
