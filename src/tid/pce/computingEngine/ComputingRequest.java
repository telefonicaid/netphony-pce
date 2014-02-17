package tid.pce.computingEngine;

import java.io.DataOutputStream;
import java.net.Inet4Address;
import java.util.LinkedList;

import tid.pce.pcep.constructs.Request;
import tid.pce.pcep.constructs.SVECConstruct;
import tid.pce.pcep.messages.PCEPRequest;
import tid.pce.pcep.objects.*;

public class ComputingRequest {
	
	private SVECConstruct svec;
	
	private LinkedList<SVECConstruct> SvecList;
	
	private LinkedList<Request> requestList;
	
	private Monitoring monitoring;
	
	private PccReqId pccReqId;
	
	private Inet4Address remotePCEId;
	
	private long timeStampNs;
	
	private long maxTimeInPCE=120000;//Por defecto, dos minutos...
	
	/**
	 * DataOutputStream to send the response to the peer PCC
	 */

	private DataOutputStream out=null; 
	
	public SVECConstruct getSvec() {
		return svec;
	}

	public void setSvec(SVECConstruct svec) {
		this.svec = svec;
	}

	public LinkedList<SVECConstruct> getSvecList() {
		return SvecList;
	}

	public void setSvecList(LinkedList<SVECConstruct> svecList) {
		SvecList = svecList;
	}

	public LinkedList<Request> getRequestList() {
		return requestList;
	}

	public void setRequestList(LinkedList<Request> requestList) {
		this.requestList = requestList;
	}

	public DataOutputStream getOut() {
		return out;
	}

	public void setOut(DataOutputStream out) {
		this.out = out;
	}

	public Monitoring getMonitoring() {
		return monitoring;
	}

	public void setMonitoring(Monitoring monitoring) {
		this.monitoring = monitoring;
	}

	public PccReqId getPccReqId() {
		return pccReqId;
	}

	public void setPccReqId(PccReqId pccReqId) {
		this.pccReqId = pccReqId;
	}

	public long getTimeStampNs() {
		return timeStampNs;
	}

	public void setTimeStampNs(long timeStampNs) {
		this.timeStampNs = timeStampNs;
	}

	public long getMaxTimeInPCE() {
		return maxTimeInPCE;
	}

	public void setMaxTimeInPCE(long maxTimeInPCE) {
		this.maxTimeInPCE = maxTimeInPCE;
	}

	public Inet4Address getRemotePCEId() {
		return remotePCEId;
	}

	public void setRemotePCEId(Inet4Address remotePCEId) {
		this.remotePCEId = remotePCEId;
	}
	
	

}
