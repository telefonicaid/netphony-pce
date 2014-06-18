package tid.ipnms.datamodel.router.IPinterface;

import tid.ipnms.datamodel.misc.IPAddressUtils;

public class IPInterfaceConfig {
    /**String to definte the Operation on the interface*/
	private String operation;  // configure/enable/disable/shutdown �.
	
    /**String to input the IP address in case of configuring the IP address*/
	private String ipAddress;
    
	/**String to define the subnet in case of configuring an IP address*/
	private String subnet;

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
	 * @return the ipAddress
	 */
	public String getIpAddress() {
		return ipAddress;
	}

	/**
	 * @param ipAddress the ipAddress to set
	 */
	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	/**
	 * @return the subnet in the longest prefix format
	 */
	public String getSubnet() {
		return subnet;
	}
	
	public String getSubnetDotFormat(){
		return IPAddressUtils.parseSubnetDotFormat(subnet);
	}

	/**
	 * @param subnet the subnet to set in the longest prefix match format
	 */
	public void setSubnet(String subnet) {
		this.subnet = IPAddressUtils.parseSubnet(subnet);
	}
	
	
}
