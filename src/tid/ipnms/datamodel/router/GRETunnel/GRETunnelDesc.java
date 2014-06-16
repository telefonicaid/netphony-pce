package tid.ipnms.datamodel.router.GRETunnel;

public class GRETunnelDesc {
	/**Tunnel Identifier which is later on used as the Interface ID*/
	private String tunnelID;
	
	/**Source IP address for the tunnel*/
	private String source;
	
	/**Destination IP address for the tunnel*/
	private String destination;
	
	/**
	 * @return the tunnelID
	 */
	public String getTunnelID() {
		return tunnelID;
	}

	/**
	 * @param tunnelID the tunnelID to set
	 */
	public void setTunnelID(String tunnelID) {
		this.tunnelID = tunnelID;
	}

	/**
	 * @return the source
	 */
	public String getSource() {
		return source;
	}

	/**
	 * @param source the source to set
	 */
	public void setSource(String source) {
		this.source = source;
	}

	/**
	 * @return the destination
	 */
	public String getDestination() {
		return destination;
	}

	/**
	 * @param destination the destination to set
	 */
	public void setDestination(String destination) {
		this.destination = destination;
	}


}
