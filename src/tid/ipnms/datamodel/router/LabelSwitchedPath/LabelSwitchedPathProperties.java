package tid.ipnms.datamodel.router.LabelSwitchedPath;

public class LabelSwitchedPathProperties {
	
	/** LSP signal bandwidth
	 * 
	 *  10gigether
	 *  gigether
	 *  
	 *  
	 */
	
	private String signalBandwidth;
	
	/** LSP switching type 
	 * 
	 *  fiber
	 *  lambda-switching
	 * 
	 */
	
	private String switchingType;
	
	/** LSP encoding type
	 * 
	 * ethernet
	 * 
	 */
	
	private String encodingType;
	
	/** LSP gpid
	 * 
	 * ethernet
	 * 
	 */
		
	private String gpid;
	
	/** Constructor with parameters */
	public LabelSwitchedPathProperties(){}
	
	
	public LabelSwitchedPathProperties(String signalBandwidth, String switchingType, String encodingType, String gpid){
		
		this.signalBandwidth = signalBandwidth;
		this.switchingType = switchingType;
		this.encodingType = encodingType;
		this.gpid = gpid;
		
	}

	public String getSignalBandwidth() {
		return signalBandwidth;
	}

	public void setSignalBandwidth(String signalBandwidth) {
		this.signalBandwidth = signalBandwidth;
	}

	public String getSwitchingType() {
		return switchingType;
	}

	public void setSwitchingType(String switchingType) {
		this.switchingType = switchingType;
	}

	public String getEncodingType() {
		return encodingType;
	}

	public void setEncodingType(String encodingType) {
		this.encodingType = encodingType;
	}

	public String getGpid() {
		return gpid;
	}

	public void setGpid(String gpid) {
		this.gpid = gpid;
	}
	
	
	

}
