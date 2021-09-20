package es.tid.pce.server;

import es.tid.pce.pcep.constructs.StateReport;
import es.tid.pce.pcep.objects.EndPoints;
import es.tid.pce.pcep.objects.ExplicitRouteObject;

/**
 * Record of the LSP in the database 
 * @author ogondio
 *
 */
public class SD_LSP {
	
	public  int pLSPID;
	
	public StateReport stateRport; //mapping of domain - State Report 
	
	public ExplicitRouteObject fullERO;
	
	public EndPoints endpoints;
	
	boolean segmentRouting = false;
	
	/**
	 * True if the LSP has been delegated to the PCE
	 */
	public boolean delegated; 
	
	/**
	 * True if the LSP has been created by the PCE
	 */
	public boolean created;
	
	public SD_LSP(){
		//By default, the LSP is not delegated
		delegated=false;
		//By default the LSP is RSVP-TE
		segmentRouting = false;
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

	@Override
	public String toString() {
		return "SD_LSP [pLSPID=" + pLSPID + ", stateRport=" + stateRport + ", fullERO=" + fullERO + ", endpoints="
				+ endpoints + ", segmentRouting=" + segmentRouting + ", delegated=" + delegated + ", created=" + created
				+ "]";
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

	public boolean isDelegated() {
		return delegated;
	}

	public void setDelegated(boolean delegated) {
		this.delegated = delegated;
	}

	public boolean isCreated() {
		return created;
	}

	public void setCreated(boolean created) {
		this.created = created;
	}

	public boolean isSegmentRouting() {
		return segmentRouting;
	}

	public void setSegmentRouting(boolean segmentRouting) {
		this.segmentRouting = segmentRouting;
	}


	
	
	

}
