package tid.pce.management;

import java.net.Inet4Address;

/**
 * PCEP Entity
 * 
 * @author ogondio
 *
 */
public class Entity {

	/**
	 * Address of the LOCAL entity
	 */
	private Inet4Address addr;

	/**
	 * If the entity is enabled or not
	 */
	private boolean enabled;

	public enum Role {
		UNKNOWN(0), PCC(1), PCE(2), PCCPCE(3);
		private int value;

		private Role(int value) {
			this.value = value;
		}
		public String toString(){
			if (value==0){
				return "Unknown";
			}else {
				return "";
			}
		}
	}



	/**
	 * role of the entity
	 */
	private Role role;

	/**
	 * 
	 */
	private String description;

	public Inet4Address getAddr() {
		return addr;
	}

	public void setAddr(Inet4Address addr) {
		this.addr = addr;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public Role getRole() {
		return role;
	}

	public void setRole(Role role) {
		this.role = role;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
	
	
	

}
