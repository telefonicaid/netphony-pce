package tid.ipnms.datamodel.router.routing.acl;

public class ForwardingRuleDesc {
    /**Operation (add/del) */
	private String operation; // drop, forward 
    
	/**Destination Interface to be forwarded on*/
	private String destIF;
	
	/**nextHop IP to be forwarded on */
    private String nextHopIP;

	/**
	 * @return the operation
	 */
	public String getOperation() {
		return operation;
	}

	/**
	 * @param operation the operation to set
	 */
	public void setOperation(String operation) {
		this.operation = operation;
	}

	/**
	 * @return the destIF
	 */
	public String getDestIF() {
		return destIF;
	}

	/**
	 * @param destIF the destIF to set
	 */
	public void setDestIF(String destIF) {
		this.destIF = destIF;
	}

	/**
	 * @return the nextHopIP
	 */
	public String getNextHopIP() {
		return nextHopIP;
	}

	/**
	 * @param nextHopIP the nextHopIP to set
	 */
	public void setNextHopIP(String nextHopIP) {
		this.nextHopIP = nextHopIP;
	}
    
    
}
