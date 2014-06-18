package tid.ipnms.datamodel.router.LabelSwitchedPath;

import java.net.Inet4Address;
import java.util.LinkedList;

public class LabelSwitchedPath {
	
	/**LSP Identifier which is later on used as the Interface ID*/
	private String lspId;
	
	/**Source IP address for the LSP*/
	private String source;
	
	/**Destination IP address for the LSP*/
	private String destination;
	
	/**Path Identifier*/
	private String pathName;
		
	/**Path nodes and interfaces IPs*/
	private LinkedList<Inet4Address> path;
	
	/**LSP Properties*/
	private LabelSwitchedPathProperties lspProperties;
	
	/**Operation*/
	private String operation;
	
	/**Default constructor*/
	public LabelSwitchedPath(){}
	
	
	/**Constructor with parameters that initializes to an empty path*/
	public LabelSwitchedPath(String lspId, String source, String destination, String pathName, LabelSwitchedPathProperties lspProperties){
		
		this.lspId = lspId;
		this.source = source;
		this.destination = destination;
		this.path = new LinkedList<Inet4Address>();
		this.lspProperties = lspProperties;		
		this.pathName = pathName;
	}
		
	/**
	 * @return the tunnelID
	 */
	public String lspId() {
		return lspId;
	}

	/**
	 * @param tunnelID the tunnelID to set
	 */
	public void setTunnelID(String lspId) {
		this.lspId = lspId;
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

	public String getLspId() {
		return lspId;
	}

	public void setLspId(String lspId) {
		this.lspId = lspId;
	}

	public String getPathName() {
		return pathName;
	}

	public void setPathName(String pathName) {
		this.pathName = pathName;
	}

	public LinkedList<Inet4Address> getPath() {
		return path;
	}

	public void setPath(LinkedList<Inet4Address> path) {
		this.path = path;
	}

	public LabelSwitchedPathProperties getLspProperties() {
		return lspProperties;
	}

	public void setLspProperties(LabelSwitchedPathProperties lspProperties) {
		this.lspProperties = lspProperties;
	}

	public String getOperation() {
		return operation;
	}


	public void setOperation(String operation) {
		this.operation = operation;
	}

	

}
