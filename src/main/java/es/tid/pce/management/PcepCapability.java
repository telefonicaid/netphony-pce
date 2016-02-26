package es.tid.pce.management;

public class PcepCapability {

	private boolean gmpls=false;
	private boolean stateful=false;
	private boolean lspUpdate=false;

	/**
	 * If the role is a PCC, it indicates that the PCC
	 * allows instantiation of an LSP by a PCE.
     * If the role is a PCE, it indicates that the PCE may
     * attempt to instantiate LSPs.
	 */
	private boolean instantiationCapability=false;

	private boolean parentPCE=false;
	private boolean childPCE=false;

	public boolean isGmpls() {
		return gmpls;
	}
	public void setGmpls(boolean gmpls) {
		this.gmpls = gmpls;
	}
	public boolean isStateful() {
		return stateful;
	}
	public void setStateful(boolean stateful) {
		this.stateful = stateful;
	}
	public boolean isLspUpdate() {
		return lspUpdate;
	}
	public void setLspUpdate(boolean lspUpdate) {
		this.lspUpdate = lspUpdate;
	}
	public boolean isParentPCE() {
		return parentPCE;
	}
	public void setParentPCE(boolean parentPCE) {
		this.parentPCE = parentPCE;
	}
	public boolean isChildPCE() {
		return childPCE;
	}
	public void setChildPCE(boolean childPCE) {
		this.childPCE = childPCE;
	}
	public boolean isInstantiationCapability() {
		return instantiationCapability;
	}
	public void setInstantiationCapability(boolean instantiationCapability) {
		this.instantiationCapability = instantiationCapability;
	}

	public String toString(){
		StringBuffer sb=new StringBuffer(200);
		sb.append("\nCapabilites: ");
		if (gmpls){
			sb.append("gmls ");
		}
		if (stateful){
			sb.append("stateful ");
		}
		if (lspUpdate){
			sb.append("lspUpdate ");
		}
		if (parentPCE){
			sb.append("parentPCE ");
		}
		if (childPCE){
			sb.append("childPCE ");
		}
		if (instantiationCapability){
			sb.append("instantiationCapability ");
		}
		return sb.toString();
	}

}
