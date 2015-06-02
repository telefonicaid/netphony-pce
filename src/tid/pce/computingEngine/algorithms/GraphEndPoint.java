package tid.pce.computingEngine.algorithms;

/**
 * 
 * @author b.mvas
 *
 */

public class GraphEndPoint {
	private Object vertex; //return
	private long interfaceID;
	
	public void GraphEndpoint(){
		
	}
	
	public long getInterfaceID() {
		return interfaceID;
	}

	public void setInterfaceID(long interfaceID) {
		this.interfaceID = interfaceID;
	}

	public void setVertex(Object vertex) {
		this.vertex = vertex;
	}
	
	public Object getVertex() {
		return vertex;
	}
}
