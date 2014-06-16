package tid.ipnms.datamodel.router.routing;

import tid.ipnms.datamodel.misc.IPAddressUtils;

public class StaticRouteDesc {
    /**Operation to add/del an IP route*/
	private String operation; //add / delete   
	
	/**Destination Network address */
    private String destIP;
    /**Destination Subnet mask */
    private String destSubnet;
    
    /**Next hop Interface ID*/
    private String destIFID; //use one of the two i.e. IFID or IFIP
    
    /**Next Hop IP address */
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
	 * @return the destSubnet
	 */
	public String getDestSubnetDotFormat() {
		return IPAddressUtils.parseSubnetDotFormat(destSubnet);
	}

	/**
	 * @param destSubnet the destSubnet to set
	 */
	public void setDestSubnet(String destSubnet) {
		this.destSubnet = IPAddressUtils.parseSubnet(destSubnet);
	}

	/**
	 * @return the destIFID
	 */
	public String getDestIFID() {
		return destIFID;
	}

	/**
	 * @param destIFID the destIFID to set
	 */
	public void setDestIFID(String destIFID) {
		this.destIFID = destIFID;
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
