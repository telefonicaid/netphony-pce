package tid.netManager.uni;

import java.net.Inet4Address;
import java.util.LinkedList;

public class CreateLSP extends DispatcherObjects{
	
	private Inet4Address nodeToChange;
	private String interfaz;
	private String lspName;
	private LinkedList<Inet4Address> path;
	
	
	public CreateLSP(Inet4Address nodeToChange, String interfaz, String lspName, LinkedList<Inet4Address> path){
		
		this.nodeToChange = nodeToChange;
		this.interfaz = interfaz;
		this.lspName = lspName;
		this.path = path;
		super.setType(4);
				
	}
	
	public boolean executeChange(){
		
		return true;
		
	}

	public Inet4Address getNodeToChange() {
		return nodeToChange;
	}

	public void setNodeToChange(Inet4Address nodeToChange) {
		this.nodeToChange = nodeToChange;
	}

	public String getInterfaz() {
		return interfaz;
	}

	public void setInterfaz(String interfaz) {
		this.interfaz = interfaz;
	}

	public String getLspName() {
		return lspName;
	}

	public void setLspName(String lspName) {
		this.lspName = lspName;
	}

	public LinkedList<Inet4Address> getPath() {
		return path;
	}

	public void setPath(LinkedList<Inet4Address> path) {
		this.path = path;
	}


	
	
}
