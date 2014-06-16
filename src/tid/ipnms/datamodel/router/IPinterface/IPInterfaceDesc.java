package tid.ipnms.datamodel.router.IPinterface;

public class IPInterfaceDesc {
   
	/**router ID of the router associated with the interface*/
	private String routerID;
	
	/**Interface ID associated with the interface*/
    private String interfaceID;
    
    /**Interface type to differentiate between physical and GRE tunnel Interfaces*/
    private String interfaceType;
    
	/**
	 * @return the routerID
	 */
	public String getRouterID() {
		return routerID;
	}
	/**
	 * @param routerID the routerID to set
	 */
	public void setRouterID(String routerID) {
		this.routerID = routerID;
	}
	/**
	 * @return the interfaceID
	 */
	public String getInterfaceID() {
		return interfaceID;
	}
	/**
	 * @param interfaceID the interfaceID to set
	 */
	public void setInterfaceID(String interfaceID) {
		this.interfaceID = interfaceID;
	}
	/**
	 * @return the interfaceType
	 */
	public String getInterfaceType() {
		return interfaceType;
	}
	/**
	 * @param interfaceType the interfaceType to set
	 */
	public void setInterfaceType(String interfaceType) {
		this.interfaceType = interfaceType;
	} 
    
    
}
