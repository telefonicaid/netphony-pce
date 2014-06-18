package tid.topology.topologymodule;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import tid.topology.ONEGraph;
import tid.topology.elements.Intf;
import tid.topology.elements.Link;
import tid.topology.elements.Node;

import com.google.gson.Gson;


/**
 * The Topology Module is responsible for providing network topology information, both per-layer topologies as well as inter-layer topology, and providing such topological information to the ONE adapter�s internal modules. 
 * 
 * @author Telefonica I+D
 *
 */
public class InformationRetriever {
	
	Logger log;
	/**
	 * Topology graph
	 */
	private ONEGraph oneGraph;
	/**
	 * Constructor.
	 * @param oneGraph
	 */
	public InformationRetriever(ONEGraph oneGraph){
		this.oneGraph=oneGraph;
		log=Logger.getLogger("InformationRetriever");
		
	}
	
	/**
	 * Gets all the topology information of the three layers
	 * In case the layer has several domains, they can be identified by a domainID.
	 * @param layer
	 * @param domainID
	 * @return topology
	 */
	public String getFullTopology(String domainID){		
		String topology;
		ArrayList<String> layer = new ArrayList<String>();
		layer.add(LayerType.INTERLAYER);
		layer.add(LayerType.IP);
		layer.add(LayerType.TRANSPORT);
		topology = "{\"Topology\":[{\"nodeList\":[";
		
		for (int i = 0; i<layer.size();i++){
			ArrayList<Node> nodesSet = oneGraph.getNodes();

			Iterator <Node> nodesIterator=nodesSet.iterator();
			//Interlayer
			ArrayList<Link> linksSet = oneGraph.getLinks(layer.get(i));
			Iterator <Link> linksIterator=linksSet.iterator();

			while (nodesIterator.hasNext()){
				Node node = nodesIterator.next();
				if (node.getDomain() == Integer.parseInt(domainID)){
					if (node.getLayer().equals(layer.get(i))){	
						Gson gson = new Gson();
						if (nodesIterator.hasNext())
							topology = topology +  gson.toJson(node)+",";
						else
							topology = topology +  gson.toJson(node);
					}
				}
			}
			topology = topology + "]},";
			topology =  topology +"{\"linkList\":[";
			while (linksIterator.hasNext()){
				Link link = linksIterator.next();
				Gson gson = new Gson();
				if (linksIterator.hasNext())
					topology = topology+ gson.toJson(link) +  ",";
				else
					topology = topology+ gson.toJson(link);

			}

		}
		topology = topology + "]}]}";
		return topology;
	}
	

	/**
	 * Gets all the topology information of a given layer. 
	 * In case the layer has several domains, they can be identified by a domainID.
	 * @param layer
	 * @param domainID
	 * @return topology
	 */
	public String getFullTopology(String layer, String domainID){
		Topology mytopology = new Topology();
		
		
		ArrayList<Node> nodesSet = oneGraph.getNodes();

		Iterator <Node> nodesIterator=nodesSet.iterator();
		ArrayList<Link> linksSet = oneGraph.getLinks(layer);
		
		ArrayList<Node> nodesAux = new ArrayList<Node>();
		while (nodesIterator.hasNext()){
			Node node = nodesIterator.next();
			if (node.getDomain() == Integer.parseInt(domainID)){
				if (node.getLayer().equals(layer)){
					nodesAux.add(node);
				}
			}
		}
		mytopology.setNodeList(nodesAux);
		mytopology.setLinkList(linksSet);
		Gson gson = new Gson();
		gson.toJson(mytopology);		
		
		return gson.toJson(mytopology);
	}
	
	/**
	 * Gets all the topology information of a given layer. 
	 * In case the layer has several domains, they can be identified by a domainID.
	 * @param layer
	 * @param domainID
	 * @return topology
	 */
	/*public String getFullTopology(String layer, String domainID){		
		String topology;
		ArrayList<Node> nodesSet = oneGraph.getNodes();

		Iterator <Node> nodesIterator=nodesSet.iterator();
		ArrayList<Link> linksSet = oneGraph.getLinks(layer);
		Iterator <Link> linksIterator=linksSet.iterator();
		topology = "{\"Topology\":[{\"nodeList\":[";
		while (nodesIterator.hasNext()){
			Node node = nodesIterator.next();
			if (node.getDomain() == Integer.parseInt(domainID)){
				if (node.getLayer().equals(layer)){
					Gson gson = new Gson();
					if (nodesIterator.hasNext())
						topology = topology +  gson.toJson(node)+",";
					else
						topology = topology +  gson.toJson(node);
				}
			}
		}
		topology = topology + "]},";
		topology =  topology +"{\"linkList\":[";
		while (linksIterator.hasNext()){
			Link link = linksIterator.next();
			Gson gson = new Gson();
			if (linksIterator.hasNext())
				topology = topology+ gson.toJson(link) +  ",";
			else
				topology = topology+ gson.toJson(link);
		}
		topology = topology + "]}]}";
		return topology;
	}*/
	
			
	
	/**
	 * Retrieves the node in the other layer to which the interface is connected
	 * @param nodeInterface
	 * @return nodeId
	 */
	public String getOppositeNode(Intf nodeInterface){			
		Node finalNode=null;
		ArrayList<Node> nodesSet = oneGraph.getNodes();
		Iterator <Node> nodesIterator=nodesSet.iterator();
		while (nodesIterator.hasNext()){
			Node node = nodesIterator.next();
			Iterator <Intf> interfacesIterator = node.getIntfList().iterator();
			while (interfacesIterator.hasNext()){
				Intf intf = interfacesIterator.next();					 
				if (intf.equals(nodeInterface)){
					finalNode= node;
					break; 						 
				}				 
			}				 
		}
		if (finalNode != null){
			ArrayList<Link> links= oneGraph.getLinks(LayerType.INTERLAYER);
			Iterator <Link> linksIterator=links.iterator();
			while (linksIterator.hasNext()){
				Link link= linksIterator.next();				
					if ( (link.getSource().getNode()).equals(finalNode.getNodeID()) && (link.getSource().getIntf().equals(nodeInterface.getName()))){
						Gson gson = new Gson();
						return gson.toJson(link.getDest());
					}else if ((link.getDest().getNode()).equals(finalNode.getNodeID()) && (link.isDirectional())&& (link.getDest().getIntf().equals(nodeInterface.getName()))){
						Gson gson = new Gson();
						return gson.toJson(link.getDest());
					}
				
			}
		}
		return "null";
	}
	
	/**
	 * Retrieves the node in the other layer to which the interface is connected
	 * @param nodeInterface
	 * @return interfaceName
	 */
	public String getOppositeInterface(Intf nodeInterface){				 
		
		Node finalNode=null;
		ArrayList<Node> nodesSet = oneGraph.getNodes();
		Iterator <Node> nodesIterator=nodesSet.iterator();
		while (nodesIterator.hasNext()){
			Node node = nodesIterator.next();
			Iterator <Intf> interfacesIterator = node.getIntfList().iterator();
			while (interfacesIterator.hasNext()){
				Intf intf = interfacesIterator.next();					 
				if (intf.equals(nodeInterface)){
					finalNode= node;
					break; 						 
				}				 
			}				 
		}
		if (finalNode != null){
			ArrayList<Link> links= oneGraph.getLinks(LayerType.INTERLAYER);
			Iterator <Link> linksIterator=links.iterator();
			while (linksIterator.hasNext()){
				Link link= linksIterator.next();				
					if ( (link.getSource().getNode()).equals(finalNode.getNodeID()) && (link.getSource().getIntf().equals(nodeInterface.getName()))){
						Gson gson = new Gson();
						return gson.toJson(link.getDest().getIntf());
						
					}else if ((link.getDest().getNode()).equals(finalNode.getNodeID()) && (link.isDirectional())&& (link.getDest().getIntf().equals(nodeInterface.getName()))){
						Gson gson = new Gson();
						return gson.toJson(link.getSource().getIntf());
						
					}
				
			}
		}
		return "null";
	}
	/**
	 * Retrieves the neighbour nodes of a given node in a layer
	 * @param node
	 * @return list of nodes 
	 */
	public ArrayList<Node> getNeighbourNodesOf(String nodeID){
		ArrayList<Node> neighbours = new ArrayList<Node>();
		Node node =  oneGraph.getNode(nodeID);
		if (node != null){
			String layer = node.getLayer();
			ArrayList<Link> linkSet = oneGraph.getLinks(layer);
			Iterator <Link> linksIterator=linkSet.iterator();
			while (linksIterator.hasNext()){
				Link link = linksIterator.next();
				if (link.getSource().getNode().equals((node.getNodeID()))){				
					String neighbourID = link.getDest().getNode();
					ArrayList<Node> nodesSet = oneGraph.getNodes();
					Iterator <Node> nodesIterator=nodesSet.iterator();
					while (nodesIterator.hasNext()){
						Node possibleNeighbourNode = nodesIterator.next();
						if (possibleNeighbourNode.getNodeID().equals(neighbourID)){
							neighbours.add(possibleNeighbourNode);
						}			 
					}				 
				}else if (link.getDest().getNode().equals((node.getNodeID()))){
					String neighbourID = link.getSource().getNode();
					ArrayList<Node> nodesSet = oneGraph.getNodes();
					Iterator <Node> nodesIterator=nodesSet.iterator();
					while (nodesIterator.hasNext()){
						Node possibleNeighbourNode = nodesIterator.next();
						if (possibleNeighbourNode.getNodeID().equals(neighbourID)){
							neighbours.add(possibleNeighbourNode);
						}					 
					}
				}

			}
		}
		return neighbours;
	}
	/**
	 * It returns the layer of the interfaceName received by argument
	 * @param interfaceName
	 * @return layer "interlayer", "transport", "IP"
	 */
	public String measurementModule(String interfaceName){
		Intf intf=getIntfByName(interfaceName);
		String layer = "null";
		if (intf!= null){
			ArrayList<String> layering=intf.getLayering();
			layer=layering.get(0);
		}
		return layer;


	}
	 

	 /**
	  * Returns the interface given the name
	  * @param intfName
	  * @return interface object
	  */
	 public Intf getIntfByName(String intfName){
		 ArrayList<Node> nodesSet = oneGraph.getNodes();
		 Iterator <Node> nodesIterator=nodesSet.iterator();
		 while (nodesIterator.hasNext()){
			 Node node = nodesIterator.next();
			 Iterator <Intf> interfacesIterator = node.getIntfList().iterator();
			 while (interfacesIterator.hasNext()){
				 Intf intf = interfacesIterator.next();					 
				 if (intf.getName().equals(intfName)){					 
					return intf;					  						 
				 }				 
			 }				 
		 }
		 return null;

		}
	 /**
	  * Returns the interface an address
	  * @param intfName
	  * @return interface object
	  */
	 public Intf getIntfByAddress(String intfAddress){
		 ArrayList<Node> nodesSet = oneGraph.getNodes();
		 Iterator <Node> nodesIterator=nodesSet.iterator();
		 while (nodesIterator.hasNext()){
			 Node node = nodesIterator.next();
			 Iterator <Intf> interfacesIterator = node.getIntfList().iterator();
			 while (interfacesIterator.hasNext()){
				 Intf intf = interfacesIterator.next();					 
				 if (intf.getAddress().contains(intfAddress)){					 
					return intf;					  						 
				 }				 
			 }				 
		 }
		 return null;
		}
	 /**
	  * Returns the Node that contains the input Management Address
	  * @param intfName
	  * @return interface object
	  */
	 public Node getNodeByAddress(String nodeAddress){
		 ArrayList<Node> nodesSet = oneGraph.getNodes();
		 Iterator <Node> nodesIterator=nodesSet.iterator();
		 while (nodesIterator.hasNext()){
			 Node node = nodesIterator.next();
			 if (node.getAddress().contains(nodeAddress)){
				 return node;					  						 
			 }
		 }
		 return null;
		}
	 /**
	  * Returns the node of the nodeName
	  * @param nodeName
	  * @return node object
	  */
	 public Node getNodeByName(String nodeName){
		 return oneGraph.getNode(nodeName);		

		}
	 
		 	 
	 /**
	  * Returns the Node that contains the input interface address
	  * @param intfName
	  * @return
	  */
	 public Node getNodeByIntfAddress(String intfAddress){		 
		 ArrayList<Node> nodesSet = oneGraph.getNodes();
		 Iterator <Node> nodesIterator=nodesSet.iterator();
		 while (nodesIterator.hasNext()){
			 Node node = nodesIterator.next();
			 Iterator <Intf> interfacesIterator = node.getIntfList().iterator();
			 while (interfacesIterator.hasNext()){
				 Intf intf = interfacesIterator.next();					 
				 if (intf.getAddress().contains(intfAddress)){					 
					return node;					  						 
				 }				 
			 }				 
		 }
		 return null;						 
	 }	
	 /**
	  * Returns the Node that contains the input interface name
	  * @param intfName
	  * @return
	  */
	 public Node getNodeByIntfName(String intfName){		 
		 ArrayList<Node> nodesSet = oneGraph.getNodes();
		 Iterator <Node> nodesIterator=nodesSet.iterator();
		 while (nodesIterator.hasNext()){
			 Node node = nodesIterator.next();
			 Iterator <Intf> interfacesIterator = node.getIntfList().iterator();
			 while (interfacesIterator.hasNext()){
				 Intf intf = interfacesIterator.next();					 
				 if (intf.getName().equals(intfName)){					 
					return node;					  						 
				 }				 
			 }				 
		 }
		 return null;						 
	 }

	 
	 /**
	  * Read the topology received by argument
	  * @param networkName
	  * @return topology
	  */
	 String readTopology(String networkName){	 
	    	String networkString = "";	    	
	    		File readFile = null;
	    		FileReader fr_readFile = null;
	    		BufferedReader br = null;	    

	    		try {
	    			// Apertura del fichero y creacion de BufferedReader para poder
	    			// hacer una lectura comoda (disponer del metodo readLine()).
	    			readFile = new File (networkName);
	    			fr_readFile = new FileReader (readFile);
	    			br = new BufferedReader(fr_readFile);		
	    			
	    			String linea=null;
	    			
	    			while ((linea=br.readLine())!=null){
	    				
	    				StringTokenizer st = new StringTokenizer(linea);
	    			     while (st.hasMoreTokens()) {
	    			    	 networkString=networkString+st.nextToken();
	    			     }
	    			     networkString=networkString+"\n";
	    			}
	    		}
	    		catch(Exception e){
	    			e.printStackTrace();
	    		}finally{    			
	    			// En el finally cerramos el fichero, para asegurarnos
	    			// que se cierra tanto si todo va bien como si salta 
	    			// una excepcion.
	    			try{                    
	    				if( null != fr_readFile ){   
	    					fr_readFile.close();
	    				}                  
	    			}catch (Exception e2){ 
	    				e2.printStackTrace();
	    			}
	    		}
	    	
	    	return networkString;
	    }
	 
	 
	public ONEGraph getOneGraph() {
		return oneGraph;
	}
	public void setOneGraph(ONEGraph oneGraph) {
		this.oneGraph = oneGraph;
	}
	    
}
