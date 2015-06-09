package tid.pce.computingEngine;

import java.io.DataOutputStream;
import java.net.Inet4Address;
import es.tid.pce.pcep.constructs.PCEPIntiatedLSP;

public class InitiationRequest {

	
	private PCEPIntiatedLSP lspIniRequest;
	
	private Inet4Address remotePeerIP;
	
	private DataOutputStream out=null; 
	
	public DataOutputStream getOut()
	{
		return out;
	}

	public void setOut(DataOutputStream out)
	{
		this.out = out;
	}

	public PCEPIntiatedLSP getLspIniRequest() {
		return lspIniRequest;
	}

	public void setLspIniRequest(PCEPIntiatedLSP lspIniRequest) {
		this.lspIniRequest = lspIniRequest;
	}

	public Inet4Address getRemotePeerIP() {
		return remotePeerIP;
	}

	public void setRemotePeerIP(Inet4Address remotePeerIP) {
		this.remotePeerIP = remotePeerIP;
	}
	

}
