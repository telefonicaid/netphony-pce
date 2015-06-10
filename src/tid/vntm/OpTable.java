package tid.vntm;

/**
 * 
 * @author baam
 *
 */
public class OpTable {
	
	private String AppIP;
	private String PCCIP;
	private String PCCPort;
	private long PCCoperationID;
	private String workflow;
	
	public OpTable (String appString, String pccipString, String PCCport, long pccopidString, String workflow){
		this.AppIP=appString;
		this.PCCIP=pccipString;
		this.PCCPort=PCCport;
		this.PCCoperationID=pccopidString;
		this.workflow=workflow;
	}

	public String getAppIP() {
		return AppIP;
	}

	public void setAppIP(String appIP) {
		AppIP = appIP;
	}

	public String getPCCIP() {
		return PCCIP;
	}

	public void setPCCIP(String pCCIP) {
		PCCIP = pCCIP;
	}

	public long getPCCoperationID() {
		return PCCoperationID;
	}

	public void setPCCoperationID(long pCCoperationID) {
		PCCoperationID = pCCoperationID;
	}
	
	public String getPCCPort() {
		return PCCPort;
	}

	public void setPCCPort(String pCCPort) {
		PCCPort = pCCPort;
	}

	public String getWorkflow() {
		return workflow;
	}

	public void setWorkflow(String workflow) {
		this.workflow = workflow;
	}

	
}
