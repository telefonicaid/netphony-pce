package tid.ipnms.datamodel.router.routing.routingprotocol;

public class RProtocolDesc {
    /**Type of protocol used */
	private String protocolType; //ospf RIP BGP
    
	/**Operation */
	private String operation;
	
	/**process ID to be used for the configure operation*/
    private int processID;
    
    /**network in which protocol should run*/
    private String network;
    
    /**Subnet mask over which protocol should run*/
    private String mask;
    
    /**Area ID {OSPF Specific}*/
    private int areaID;

	/**
	 * @return the protocolType
	 */
	public String getProtocolType() {
		return protocolType;
	}

	/**
	 * @param protocolType the protocolType to set
	 */
	public void setProtocolType(String protocolType) {
		this.protocolType = protocolType;
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
	 * @return the processID
	 */
	public int getProcessID() {
		return processID;
	}

	/**
	 * @param processID the processID to set
	 */
	public void setProcessID(int processID) {
		this.processID = processID;
	}

	/**
	 * @return the network
	 */
	public String getNetwork() {
		return network;
	}

	/**
	 * @param network the network to set
	 */
	public void setNetwork(String network) {
		this.network = network;
	}

	/**
	 * @return the mask
	 */
	public String getMask() {
		return mask;
	}

	/**
	 * @param mask the mask to set
	 */
	public void setMask(String mask) {
		this.mask = mask;
	}

	/**
	 * @return the areaID
	 */
	public int getAreaID() {
		return areaID;
	}

	/**
	 * @param areaID the areaID to set
	 */
	public void setAreaID(int areaID) {
		this.areaID = areaID;
	}
}
