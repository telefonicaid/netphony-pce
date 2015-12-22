package es.tid.pce.server;

import es.tid.pce.pcep.constructs.StateReport;
import es.tid.pce.pcep.objects.EndPoints;
import es.tid.pce.pcep.objects.ExplicitRouteObject;

public class SD_LSP {
	
	public  int pLSPID;
	
	public StateReport stateRport; //mapping of domain - State Report 
	
	public ExplicitRouteObject fullERO;
	
	public EndPoints endpoints;
	
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

	public EndPoints getEndpoints() {
		return endpoints;
	}

	public void setEndpoints(EndPoints endpoints) {
		this.endpoints = endpoints;
	}
	
	
	
	

}
