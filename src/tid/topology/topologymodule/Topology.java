package tid.topology.topologymodule;

import java.util.ArrayList;

import tid.topology.elements.Link;
import tid.topology.elements.Node;

public class Topology {
	private ArrayList<Node> nodeList;
	private ArrayList<Link> linkList;
	
	public ArrayList<Node> getNodeList() {
		return nodeList;
	}
	public void setNodeList(ArrayList<Node> nodeList) {
		this.nodeList = nodeList;
	}
	public ArrayList<Link> getLinkList() {
		return linkList;
	}
	public void setLinkList(ArrayList<Link> linkList) {
		this.linkList = linkList;
	}
	
}
