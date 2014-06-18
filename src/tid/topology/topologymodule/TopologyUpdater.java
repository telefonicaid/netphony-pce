package tid.topology.topologymodule;


import java.util.ArrayList;
import java.util.Iterator;

import tid.topology.ONEGraph;
import tid.topology.elements.EndPoint;
import tid.topology.elements.Intf;
import tid.topology.elements.Link;
import tid.topology.elements.Node;


/**
 * This class is responsible for storing network topology information,
 *  both per-layer topologies as well as inter-layer topology. This module is devoted 
 *  to getting and maintaining up to date the topology from the available sources, 
 *  which can be the IP-NMS Module or the T-NMS Module.
 *  
 * @author Telefonica I+D
 *
 */
public class TopologyUpdater {
	private ONEGraph oneGraph;
	/**
	 * Constructor
	 * @param oNEGraph Topology Graph
	 */
	public TopologyUpdater(ONEGraph oNEGraph){
		this.oneGraph=oNEGraph;
	}
	
	/***********************************************************************
	 * UPDATE NODE
	 ***********************************************************************/
	/**
	 * This function inserts the node in the graph.
	 *  - If the node is not in the graph it is inserted
	 *  - If the node is in the graph, it is replaced for the new one
	 * @param node Node to be inserted
	 * @return boolean true if the node is inserted false if it cannot be inserted
	 */
	public boolean  updateNode(Node node){
		Node nodeInGraph = oneGraph.getNode(node.getNodeID());
		if (nodeInGraph != null){
			updateExistedNode(node,nodeInGraph);			
		}
		else 
			oneGraph.addNode(node);		
		return true;
	}
	/**
	 * This function updates a node which is already in the graph. The fields to be updated are: address, isPhysical, domain and layer.
	 * The interface list continue being the same.
	 * @param node Node to update
	 * @param nodeInGraph
	 */
	private void updateExistedNode(Node node, Node nodeInGraph ){
		//Mandatory Fields
		//Address
		nodeInGraph.setAddress(node.getAddress());
		//Domain
		nodeInGraph.setDomain(node.getDomain());
		//Layer
		nodeInGraph.setLayer(node.getLayer());
		
		//Optional fields
		//IsPhysical
		nodeInGraph.setPhysical(node.isPhysical());
		//Location
		if (node.getLocation() != null){
			nodeInGraph.setLocation(node.getLocation());
		}
		if (node.getParentRouter() != null){
			nodeInGraph.setParentRouter(node.getParentRouter());
		}
	}

	/**
	 * Update the address list of a node which is in the graph.
	 * @param node
	 * @param nodeInGraph
	 */
	private void updateAddress (Node node, Node nodeInGraph){
		for (int i = 0; i<node.getAddress().size();i++){
			if (! (nodeInGraph.getAddress().contains(node.getAddress().get(i))))
				nodeInGraph.getAddress().add(node.getAddress().get(i));
		}
	}
	
	
	/***********************************************************************
	 * UPDATE INTERFACE
	 ***********************************************************************/
	
	/**
	 * This function inserts the interface in the node. If the interface exists it is replaced.
	 * @param nodeID Node where the interface is inserted
	 * @param interf Interface to insert in the node
	 * @return boolean true if the interface is inserted, false if not
	 */
	public boolean updateIntf(String nodeID, Intf interf){
		Node node = oneGraph.getNode(nodeID);	
		if (node == null)
			return false;
		
		int positionIntf = getPositionIntfInNode(interf.getName(),nodeID);
		if (positionIntf != -1){
			node.getIntfList().remove(positionIntf);		
		}	

		node.getIntfList().add(interf);
		return true;

	}
	/***********************************************************************
	 * UPDATE LINK
	 ***********************************************************************/
	/**
	 * This function inserts the link in the graph. It is possible to update a link between the same node with different interface.
	 * If the link is in the interlayer and the node source and destiny are not in different layers, the link won't be inserted.
	 * If the link is not in the same layer as the node source and destiny, it won't be inserted.
	 * @param layer Layer in which the link is inserted
	 * @param link Link to insert 
	 * @return boolean true if the link is inserted, false if it is not
	 */
	public boolean updateLink(String layer,Link link){		
		//The source and the destiny are in the graph
		if  ((isNodeInGraph (link.getSource().getNode())) && (isNodeInGraph (link.getDest().getNode()))){
			//Both interfaces are in the respective nodes
			if ((getPositionIntfInNode(link.getSource().getIntf(), link.getSource().getNode()) != -1) && (getPositionIntfInNode(link.getDest().getIntf(), link.getDest().getNode()) != -1)){
				Link linkInGraph = getLinkInGraph(layer, link.getLinkID());
				if (linkInGraph !=null){	
					//Fill the optional parameteres
					updateLinkOptionalParameters(link, linkInGraph);
					//Eliminate the link of the graph			
					oneGraph.removeLink(layer,linkInGraph);					
				}
				//Check if the insertion is correct
				if (checkLinkUpdateisCorrect(layer,link)){
					oneGraph.addLink(layer,link);					
					return true;
				}
			}			
		}
		return false;
	}
	/**
	 * This function updates the optional parameters of the link
	 * @param newLink
	 * @param linkInGraph
	 */
	private void updateLinkOptionalParameters(Link newLink, Link linkInGraph){
		//Fill the optional parameteres
		if (newLink.isDirectional() == null){
			newLink.setDirectional(linkInGraph.isDirectional());
		}
		if (newLink.getTeMetric() == -1)
			newLink.setTeMetric(linkInGraph.getTeMetric());
		if (newLink.getType() == null)
			newLink.setType(linkInGraph.getType());
	}
	/**
	 * This function check if the link insertion is possible. 
	 * It will be possible if source and destination node are in the same layer as the link or if the are in different layer and the link is interlayer
	 * @param layer Layer of the link
	 * @param link Link to be inserted
	 * @return true if it is possible to insert the link or false if it is not possible
	 */
	private boolean checkLinkUpdateisCorrect(String layer,Link link){
		String layerSource =oneGraph.getNode(link.getSource().getNode()).getLayer();
		String layerDest = oneGraph.getNode(link.getDest().getNode()).getLayer();
		if (layer.equals(LayerType.INTERLAYER)){
			if (layerSource.equals(layerDest))
				return false;
			return true;
		}
		else if (layer.equals(LayerType.IP)){
			if (layerSource.equals(LayerType.IP) && layerDest.equals(LayerType.IP))
				return true;
		}
		else if (layer.equals(LayerType.TRANSPORT)){
			if (layerSource.equals(LayerType.TRANSPORT) && layerDest.equals(LayerType.TRANSPORT))
				return true;
		}
		return false;
	}
	/**
	 * Not implemented correctly yet
	 * @param node 
	 * @param interfSource
	 * @param interfDest
	 * @param nodeDestination
	 * @param link
	 * @return
	 */
	public boolean updateAll(Node node,Intf interfSource,Intf interfDest,String nodeDestination, Link link){
		if (updateNode(node)){
			String layer=node.getLayer();
			if (updateIntf(node.getNodeID(),interfSource))
				if (updateIntf(nodeDestination,interfDest ))
					if (updateLink(layer,link))
						return true;
		}
		//FIXME: remove los que hayas metido antes del error.
		return false;
	}
	
	/**
	 * Checks if the node is in the graph.
	 * @param node Node to check if is in the graph
	 * @return boolean true if the node is in the graph, false if it is not
	 */
	public boolean isNodeInGraph(String nodeID){
		Node node = oneGraph.getNode(nodeID);	
		if (node == null)
			return false;		
		return true;

	}
	/**
	 * Returns the link if the specific identifier
	 * @param layer Layer where the link is 
	 * @param linkID Link identifier, it is unique 
	 * @return  The link if it is in the graph.
	 */
	private Link getLinkInGraph(String layer, String linkID){		
		ArrayList<Link> linkSet = oneGraph.getLinks(layer);
		Iterator <Link> linkIterator = null;
		if (linkSet != null){
			linkIterator =linkSet.iterator();
			while (linkIterator.hasNext()){
				Link possibleLink = linkIterator.next();		
				if  (possibleLink.getLinkID().equals(linkID))
					return possibleLink;		
			}
		}		
		return null;
	}
	/**
	 * Returns the position of the interface list where the interfaces is.
	 * @param nameIntf Interface to find the position
	 * @param nodeID Node where the interface is
	 * @return The position of the interface in the list or -1 if the interface is not in the list
	 */
	public int getPositionIntfInNode(String nameIntf, String nodeID){
		int counter = -1;
		Node node = oneGraph.getNode(nodeID);
		if (node == null)
			return -1;
		else {			
			ArrayList<Intf> intfSet = node.getIntfList();
			Iterator <Intf> intfIterator=intfSet.iterator();
			while (intfIterator.hasNext()){
				counter++;
				Intf possibleIntf = intfIterator.next();
				if  (possibleIntf.getName().equals(nameIntf)){						
					return counter;
				}
			}
		}
		//The list has been got down and the interface was not found
		return -1;
	}
	/**
	 * This function deletes the node and the links from or to that node 
	 * @param nodeID
	 * @return
	 */
	public boolean deleteNode(String nodeID){
		if (isNodeInGraph(nodeID)){
			int size =  oneGraph.getNode(nodeID).getIntfList().size();
			//The links from or to that node have to be removed 
			for (int i = 0; i<size;i++){
				EndPoint EP = new EndPoint(nodeID,oneGraph.getNode(nodeID).getIntfList().get(i).getName());
				removeLinks(EP);
			}
			oneGraph.removeNode(nodeID);
			return true;
		}
		return false;
	}
	
	/**
	 * Delete the link
	 * @param nodeID
	 * @return true if the link was deleted and false if not
	 */
	public boolean deleteLink(String linkID){
		ArrayList<String> layer = new ArrayList<String>();
		layer.add(LayerType.INTERLAYER);
		layer.add(LayerType.IP);
		layer.add(LayerType.TRANSPORT);
		
		for (int i = 0; i<layer.size();i++){
			Link link = getLinkInGraph(layer.get(i),linkID);
			if (link != null){
				oneGraph.removeLink(layer.get(i),link);
				return true;
			}
			
		}
		return false;
		
	}
	
	/**
	 * This function delete the interface. It also remove the links connected to that interface.
	 * The node and interface must exist. 
	 * @param nodeID
	 * @return true if the interface was removed and false if not
	 */
	public boolean deleteIntf(String nodeName, String intfName){
		if (isNodeInGraph(nodeName)){
			int positionIntf = getPositionIntfInNode(intfName,nodeName);
			if (positionIntf != -1){
				//Hay que eliminar los links que tenga esa interface.
				EndPoint EP = new EndPoint(nodeName,intfName);
				removeLinks(EP);
				oneGraph.getNode(nodeName).getIntfList().remove(positionIntf);
				return true;
			}	
			
		}
		return false;
	}
	/**
	 * This function remove the links where the endPoint is the source or destination
	 * @param EP
	 */
	private void removeLinks(EndPoint EP){
		Node node = oneGraph.getNode(EP.getNode());
		ArrayList<Integer> positionsToDelete = new ArrayList<Integer>();
		//In the same layer as the node
		ArrayList<Link> links = oneGraph.getLinks(node.getLayer());
		int size = links.size();
		for (int i = 0; i<size;i++){
			if (isEndPointInLink (links.get(i),EP)){
				//Remove link
				positionsToDelete.add(i);
		}
		}
		for (int i = 0; i< positionsToDelete.size(); i++){
			links.remove(links.get(i));		
		}
		positionsToDelete =  new ArrayList<Integer>();			
		//Interlayer
		links  = oneGraph.getLinks(LayerType.INTERLAYER);
		size = links.size();
		
		for (int i = 0; i<size;i++){
			if (isEndPointInLink (links.get(i),EP)){
				//Remove link
				positionsToDelete.add(i);
				}
		}
		for (int i = 0; i< positionsToDelete.size(); i++){
			links.remove(links.get(i));		
		}
	}
	/**
	 * This function returns if the End Point (node, interface) is in the link
	 * @param link
	 * @param EP
	 * @return true or false
	 */
	private boolean isEndPointInLink(Link link, EndPoint EP){
		if ((link.getSource().compareTo(EP) == 0) ||(link.getDest().compareTo(EP) == 0))
			return true;
		return false;
	}
	/**
	 * Get the one Graph
	 * @return ONEGraph 
	 */
	public ONEGraph getOneGraph() {
		return oneGraph;
	}
	/**
	 * Set the one Graph
	 * @param oneGraph
	 */
	public void setOneGraph(ONEGraph oneGraph) {
		this.oneGraph = oneGraph;
	}
	
}

