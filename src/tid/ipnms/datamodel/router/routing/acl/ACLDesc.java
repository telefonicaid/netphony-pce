package tid.ipnms.datamodel.router.routing.acl;

public class ACLDesc {
	
	/**ACL ID */
	private String aclID;
	
	/**Operation (add delete) ACL*/
	private String operation;
	
	/**Source IP for ACL filtering*/
    private String sourceIP;
    
    /**Source Subnet for ACL filtering*/
    private String sourceSubnet;
    
    /**Destination IP for subnet filtering*/
    private String destIP;
    /**Destination Subnet for ACL filtering*/
    private String destSubnet;
    
    /**protocol used */
    private String protocol;
    
    /**destination port to be filtered*/
    private int port;

	/**
	 * @return the aclID
	 */
	public String getAclID() {
		return aclID;
	}

	/**
	 * @param aclID the aclID to set
	 */
	public void setAclID(String aclID) {
		this.aclID = aclID;
	}

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
	 * @return the sourceIP
	 */
	public String getSourceIP() {
		return sourceIP;
	}

	/**
	 * @param sourceIP the sourceIP to set
	 */
	public void setSourceIP(String sourceIP) {
		this.sourceIP = sourceIP;
	}

	/**
	 * @return the sourceSubnet
	 */
	public String getSourceSubnet() {
		return sourceSubnet;
	}

	/**
	 * @param sourceSubnet the sourceSubnet to set
	 */
	public void setSourceSubnet(String sourceSubnet) {
		this.sourceSubnet = sourceSubnet;
	}

	/**
	 * @return the destIP
	 */
	public String getDestIP() {
		return destIP;
	}

	/**
	 * @param destIP the destIP to set
	 */
	public void setDestIP(String destIP) {
		this.destIP = destIP;
	}

	/**
	 * @return the destSubnet
	 */
	public String getDestSubnet() {
		return destSubnet;
	}

	/**
	 * @param destSubnet the destSubnet to set
	 */
	public void setDestSubnet(String destSubnet) {
		this.destSubnet = destSubnet;
	}

	/**
	 * @return the protocol
	 */
	public String getProtocol() {
		return protocol;
	}

	/**
	 * @param protocol the protocol to set
	 */
	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	/**
	 * @return the port
	 */
	public int getPort() {
		return port;
	}

	/**
	 * @param port the port to set
	 */
	public void setPort(int port) {
		this.port = port;
	}
    
    
}
