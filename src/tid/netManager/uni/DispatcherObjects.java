package tid.netManager.uni;

public class DispatcherObjects {
	
	/**
	 * Type = 1 ---	Change Route
	 * Type = 2 --- Create UNI
	 * Type = 3 --- Eliminate UNI
	 * Type = 4 --- Create LSP
	 */
	
	private int type;

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}
		
}
