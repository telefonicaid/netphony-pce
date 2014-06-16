package tid.netManager.uni;

import java.net.Inet4Address;

public class ChangeIPRoute extends DispatcherObjects{
	
	private Inet4Address nodeToChange;
	private Inet4Address destination;
	private Inet4Address previousNextHop;
	private Inet4Address nextNextHop;
	
	public ChangeIPRoute(Inet4Address nodeToChange, Inet4Address destination, Inet4Address previousNextHop, Inet4Address nextNextHop){
		
		this.nodeToChange = nodeToChange;
		this.destination = destination;
		this.previousNextHop = previousNextHop;
		this.nextNextHop = nextNextHop;
		super.setType(1);
				
	}
	
	public boolean executeChange(){
		
		return true;
		
	}

	public Inet4Address getDestination() {
		return destination;
	}

	public void setDestination(Inet4Address destination) {
		this.destination = destination;
	}

	public Inet4Address getPreviousNextHop() {
		return previousNextHop;
	}

	public void setPreviousNextHop(Inet4Address previousNextHop) {
		this.previousNextHop = previousNextHop;
	}

	public Inet4Address getNextNextHop() {
		return nextNextHop;
	}

	public void setNextNextHop(Inet4Address nextNextHop) {
		this.nextNextHop = nextNextHop;
	}

	public Inet4Address getNodeToChange() {
		return nodeToChange;
	}

	public void setNodeToChange(Inet4Address nodeToChange) {
		this.nodeToChange = nodeToChange;
	}

	
	
}
