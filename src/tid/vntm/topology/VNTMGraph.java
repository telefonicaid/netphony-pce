package tid.vntm.topology;




	import java.io.File;
import java.net.Inet4Address;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.CharacterData;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import tid.vntm.topology.elements.EndPoint;
import tid.vntm.topology.elements.IPNodeParams;
import tid.vntm.topology.elements.Intf;
import tid.vntm.topology.elements.Link;
import tid.vntm.topology.elements.Location;
import tid.vntm.topology.elements.Node;


	public class VNTMGraph {
		
		HashMap<String, Node> nodes;
		
		HashMap<String, ArrayList<Link>> links;

		/**
		 * @return the nodes
		 */
		public ArrayList<Node> getNodes() {
			return new ArrayList<Node> (nodes.values());
		}

		public void addNode(Node node){
			nodes.put(node.getNodeID(), node);
		
		}
		public void removeNode(String nodeID){
			nodes.remove(nodeID);
		}
		public void removeLink(String layer, Link link){
			links.get(layer).remove(link);			
		}
		public void addLink(String layer,Link link){
			if (links.containsKey(layer)){
				links.get(layer).add(link);			
			}
			else
			{
				ArrayList<Link> linksArray = new ArrayList<Link>();
				linksArray.add(link);
				links.put(layer,linksArray);
			}
			
		}
		public Node getNode(String nodeID){
			if (nodes.containsKey(nodeID))
				return nodes.get(nodeID);
			else
				return null;		
		}
		
	/*	public Node getNode(Inet4Address address){
			if (nodes.containsKey(address))
				return nodes.get(address);
			else
				return null;		
		}*/

		public Node getNode(Inet4Address address){
				Iterator<Entry<String, Node>> it = nodes.entrySet().iterator();
				String aux = address.toString().substring(1);
				while (it.hasNext()){
					Entry<String, Node> e = it.next();
					Node n = e.getValue();
					System.out.println("Comparando "+aux+" y "+n.getAddress().get(0));
					if (aux.equals(n.getAddress().get(0)))
						return n;
				}
				return null;
		}
		
		public Link getLink(String layer, EndPoint src, EndPoint dest){
			if (links.containsKey(layer)){
				Iterator<Link> iter = links.get(layer).iterator();
				while(iter.hasNext()){
					Link link = iter.next();
					if (src.compareTo(link.getSource()) ==0)
						if (dest.compareTo(link.getDest())==0)
							return link;
					if (link.isDirectional()==false)
						if (src.compareTo(link.getDest()) ==0)
							if (dest.compareTo(link.getSource())==0)
								return link;
				}
			}
			return null;
		}
		
		public Link getLink(String source){
				Iterator<Link> iter = links.get("interlayer").iterator();
				while(iter.hasNext()){
					Link link = iter.next();
					if (source.equals((link.getSource().getNode())))
							return link;

				}
			return null;
		}
		
		/**
		 * @return the links
		 */
		public ArrayList<Link> getLinks(String layer) {
			if (links.containsKey(layer)==false){
				links.put(layer, new ArrayList<Link>());
			}
			
			return links.get(layer);

			}

		public VNTMGraph(){
			nodes = new HashMap<String, Node> ();
			links = new HashMap<String, ArrayList<Link>> ();
		}

//		public void addLink(Link link){
//			links.add(link);
//		}
		

		/**
		 * Reads and create the topology from a Network XML file. 
		 * @param fileName
		 * @return
		 */
		public void readNetwork(String fileName) {
			//First, create the graph		
			File file = new File(fileName);
			try {
				DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
				Document doc = builder.parse(file);		
				NodeList nodes_domains = doc.getElementsByTagName("domain");

				//First pass to get all the nodes

				for (int j = 0; j < nodes_domains.getLength(); j++) {

					Element element1 = (Element) nodes_domains.item(j);

					Element element_domain = (Element) nodes_domains.item(j);
					NodeList nodes_domain_id = element_domain.getElementsByTagName("domain_id");
					for (int k = 0; k < nodes_domain_id.getLength(); k++) {
						Element domain_id_e = (Element) nodes_domain_id.item(0);
						String domain_id = getCharacterDataFromElement(domain_id_e);						
					}
					NodeList nodes = element1.getElementsByTagName("node");
					System.out.println("nodes.getLength"+nodes.getLength());
					if (nodes.getLength()>0){
						for (int i = 0; i < nodes.getLength(); i++) {
							Element element = (Element) nodes.item(i);
							//Node Name
							NodeList nodeName_node = element.getElementsByTagName("nodeName");
							Element nodeName_e = (Element) nodeName_node.item(0);
							String nodeName = getCharacterDataFromElement(nodeName_e);
							NodeList addressList_node = element.getElementsByTagName("addressList");
							ArrayList<String> addresses = null;
							if (addressList_node.getLength()>0){
								addresses= new ArrayList<String>();
								for (int k = 0; k < addressList_node.getLength(); k++) {
									Element addressList_e = (Element) addressList_node.item(k);
									NodeList address_node = addressList_e.getElementsByTagName("address");
									Element address_el = (Element) address_node.item(0);
									String address = getCharacterDataFromElement(address_el);
									addresses.add(address);
								}
							}
							//Is physical Node
							boolean isPhysical_node = true;
							NodeList isPhysical_nodeList = element.getElementsByTagName("isPhysical");
							if (isPhysical_nodeList.getLength()>0){
								Element isPhysical_el = (Element) isPhysical_nodeList.item(0);									
								String isPhysical_s = getCharacterDataFromElement(isPhysical_el);
								isPhysical_node = Boolean.parseBoolean(isPhysical_s);
							}
							//Is physical Node
							String layerNode = null;
							NodeList layerNode_nodeList = element.getElementsByTagName("layerNode");
							if (layerNode_nodeList.getLength()>0){
								Element layerNode_el = (Element) layerNode_nodeList.item(0);									
								layerNode = getCharacterDataFromElement(layerNode_el);
								
							}
							
							//Interface List
							ArrayList<Intf> intfList = null;
							NodeList interfacesList_node = element.getElementsByTagName("interfacesList");
							if (interfacesList_node.getLength()>0){
								ArrayList<String> layer= null;
								ArrayList<String> addressesIntf = null;
								ArrayList<String> supportedCounters = null;
								intfList = new ArrayList<Intf>();							
								Element interface_e = (Element) interfacesList_node.item(0);
								NodeList interface_node = interface_e.getElementsByTagName("interface");
								if (interface_node.getLength()>0){									
									for (int k = 0; k < interface_node.getLength(); k++) {
										Element interface_el = (Element) interface_node.item(k);
										//name interface
										NodeList nameIntf_node = interface_el.getElementsByTagName("nameIntf");
										Element nameIntf_el = (Element) nameIntf_node.item(0);										
										String nameIntf = getCharacterDataFromElement(nameIntf_el);
										//addressesIntf
										NodeList addressIntfList_node = interface_el.getElementsByTagName("addressIntfList");
										if (addressIntfList_node.getLength()>0){
											addressesIntf = new ArrayList<String>();										
											for (int it = 0; it < addressIntfList_node.getLength(); it++) {
												Element addressIntf_e = (Element) addressIntfList_node.item(it);
												NodeList addressIntf_node = addressIntf_e.getElementsByTagName("addressIntf");
												Element addressIntf_el = (Element) addressIntf_node.item(0);										
												String addressIntf = getCharacterDataFromElement(addressIntf_el);												
												addressesIntf.add(addressIntf);
											}
										}
										//IntfUp
										//name interface
										NodeList intfUp_node = interface_el.getElementsByTagName("intfUp");
										boolean intfUp = true;
										if (intfUp_node.getLength()>0){
											Element intfUp_el = (Element) intfUp_node.item(0);										
											String intfUp_s = getCharacterDataFromElement(intfUp_el);
											intfUp = Boolean.parseBoolean(intfUp_s);

										}

										//layeringList
										NodeList layeringList_node = interface_el.getElementsByTagName("layeringList");										
										if (layeringList_node.getLength()>0){
											layer = new ArrayList<String>();										
											for (int it = 0; it < layeringList_node.getLength(); it++) {
												Element layering_e = (Element) layeringList_node.item(it);
												NodeList layering_node = layering_e.getElementsByTagName("layering");
												Element layering_el = (Element) layering_node.item(0);										
												String layering = getCharacterDataFromElement(layering_el);
												layer.add(layering);
											}
										}
										//supportedCountersList
										NodeList supportedCountersList_node = interface_el.getElementsByTagName("supportedCountersList");										
										if (supportedCountersList_node.getLength()>0){
											supportedCounters = new ArrayList<String>();										
											for (int it = 0; it < supportedCountersList_node.getLength(); it++) {
												Element  supportedCounters_e = (Element) supportedCountersList_node.item(it);
												NodeList supportedCounters_node = supportedCounters_e.getElementsByTagName("supportedCounters");
												Element supportedCounters_el = (Element) supportedCounters_node.item(0);										
												String supportedCounters_s = getCharacterDataFromElement(supportedCounters_el);
												supportedCounters.add(supportedCounters_s);
											}
										}
										//isPhysical
										//Is physical Node
										NodeList isPhysicalIntf_node = interface_el.getElementsByTagName("isPhysical");
										boolean isPhysical_intf=true;
										if (isPhysicalIntf_node.getLength()>0){
											Element isPhysical_el = (Element) isPhysicalIntf_node.item(0);									
											String isPhysical_s = getCharacterDataFromElement(isPhysical_el);
											isPhysical_intf= Boolean.parseBoolean(isPhysical_s);
										}
										//Is physical Node
										NodeList parentInterfaceName_node = interface_el.getElementsByTagName("parentInterfaceName");
										String parentInterfaceName=null;
										if (parentInterfaceName_node.getLength()>0){
											Element parentInterfaceName_el = (Element) parentInterfaceName_node.item(0);									
											parentInterfaceName = getCharacterDataFromElement(parentInterfaceName_el);												
										}

										Intf intf=new Intf();										
										intf.setName(nameIntf);
										intf.setPhysical(isPhysical_intf);												
										intf.setLayering(layer);
										intf.setAddress(addressesIntf);
										intf.setParentInterfaceName(parentInterfaceName);
										intf.setSupportedCounters(supportedCounters);		
										intf.setIntfUp(intfUp);
										intfList.add(intf);					

									}
								}
							}
							//domain
							int domain=1;
							NodeList domain_node = element.getElementsByTagName("domain");
							if (domain_node.getLength()>0){
								Element domain_e = (Element)domain_node.item(0);
								String domain_s = getCharacterDataFromElement(domain_e);
								domain= Integer.parseInt(domain_s);
							}
							//location
							NodeList location_node = element.getElementsByTagName("location");
							Location location= null;
							if (location_node.getLength()>0){							
								Element location_el = (Element) location_node.item(0);
								//Xcord
								NodeList Xcord_node = location_el.getElementsByTagName("Xcord");
								Element Xcord_el = (Element) Xcord_node.item(0);										
								String Xcord_s = getCharacterDataFromElement(Xcord_el);
								double Xcord = Double.parseDouble(Xcord_s);

								//Xcord
								NodeList Ycord_node = location_el.getElementsByTagName("Ycord");
								Element Ycord_el = (Element) Ycord_node.item(0);										
								String Ycord_s = getCharacterDataFromElement(Ycord_el);
								double Ycord = Double.parseDouble(Ycord_s);
								location= new Location(Xcord,Ycord);
							}
							//ipParams
							NodeList ipParams_node = element.getElementsByTagName("ipParams");
							IPNodeParams ipNodeParams = null;
							if (ipParams_node.getLength()>0){
								ipNodeParams= new IPNodeParams();
							}
							//parentRouter
							NodeList parentRouter_node = element.getElementsByTagName("parentRouter");
							String parentRouter = null;
							if (parentRouter_node.getLength()>0){
								Element parentRouter_e = (Element)parentRouter_node.item(0);
								parentRouter = getCharacterDataFromElement(parentRouter_e);
							}


							Node node = new Node();	
							node.setNodeID(nodeName);
							node.setDomain(domain);
							//ip.setLocation(new Location(1, 1));
							node.setPhysical(isPhysical_node);
							node.setParentRouter(parentRouter);
							node.setIpParams(ipNodeParams);
							node.setAddress(addresses);
							node.setIntfList(intfList);
							node.setLayer(layerNode);
							this.addNode(node);
						}
					}

					NodeList links = element1.getElementsByTagName("link");
					if (links.getLength()>0){
						for (int i = 0; i < links.getLength(); i++) {
							Element element = (Element) links.item(i);
							//linkID
							NodeList linkID_node = element.getElementsByTagName("linkID");
							Element linkID_e = (Element) linkID_node.item(0);
							String linkID = getCharacterDataFromElement(linkID_e);
							//isDirectional
							NodeList isDirectional_node = element.getElementsByTagName("isDirectional");
							Element isDirectional_e = (Element) isDirectional_node.item(0);
							boolean isDirectional = Boolean.parseBoolean(getCharacterDataFromElement(isDirectional_e));

							//source
							NodeList source_node = element.getElementsByTagName("source");
							EndPoint source = null;
							if (source_node.getLength()>0){		
								Element source_e = (Element) source_node.item(0);
								//node
								String node = null;
								NodeList node_node = source_e.getElementsByTagName("nodeLink");

								if (node_node.getLength()>0){		
									Element node_el = (Element) node_node.item(0);
									node = getCharacterDataFromElement(node_el);
								}
								//intf
								NodeList intf_node = source_e.getElementsByTagName("intfLink");
								String intf =null;	 
								if (intf_node.getLength()>0){	
									Element intf_el = (Element) intf_node.item(0);
									intf= getCharacterDataFromElement(intf_el);
								}
								if ((intf != null)&&(node != null))
									source=new EndPoint(node,intf);



							}

							//destination
							NodeList destination_node = element.getElementsByTagName("destination");
							EndPoint destination = null;
							if (destination_node.getLength()>0){							
								Element destination_e = (Element) destination_node.item(0);
								//node
								NodeList node_node = destination_e.getElementsByTagName("nodeLink");
								Element node_el = (Element) node_node.item(0);
								String node = getCharacterDataFromElement(node_el);
								//intf
								NodeList intf_node = destination_e.getElementsByTagName("intfLink");
								Element intf_el = (Element) intf_node.item(0);
								String intf = getCharacterDataFromElement(intf_el);
								destination=new EndPoint(node,intf);							
							}

							//type
							String type_link = null;
							NodeList type_nodeList = element.getElementsByTagName("typeLink");
							if (type_nodeList.getLength()>0){
								Element type_el = (Element) type_nodeList.item(0);									
								type_link = getCharacterDataFromElement(type_el);

							}
							//layerLink
							String layerLink = null;
							NodeList layerLink_nodeList = element.getElementsByTagName("layerLink");
							if (layerLink_nodeList.getLength()>0){
								Element layerLink_el = (Element) layerLink_nodeList.item(0);									
								layerLink = getCharacterDataFromElement(layerLink_el);

							}
							
							Link link = new Link();
							link.setLinkID(linkID);
							link.setSource(source);
							link.setDest(destination);	
							link.setDirectional(isDirectional);
							link.setType(type_link);
							this.addLink(layerLink,link);

						}
					}


				}

			} catch (Exception e) {
				e.printStackTrace();
			}

		}

		public static String getCharacterDataFromElement(Element e) {
			org.w3c.dom.Node child = e.getFirstChild();
			if (child instanceof CharacterData) {
				CharacterData cd = (CharacterData) child;
				return cd.getData();
			} else {
				return "?";
			}
		}

		public 	void createSimpleONEGraph(){


			//Create a simple graph for the following topology
			/*	_______                            _______
	    |	IP1	|__________________________|  IP2  |__________________________|  IP3  |
		|		|                          |       |
		 -------                            -------
	        |                                  |
		 _______                            _______
	    |		|__________________________|       |
		|OT1	|                          | OT2   |
		 -------                            -------
			 */

			/*----------------- Node IP1 --------------------*/
			Node ip1 = new Node();

			ip1.setNodeID("IP1");
			ip1.setDomain(1);
			ip1.setLocation(new Location(1, 1));
			ip1.setPhysical(true);
			ip1.setParentRouter(null);
			ip1.setIpParams(new IPNodeParams());
			ip1.setAddress(new ArrayList<String>());
			ip1.getAddress().add("192.168.0.1");
			//Create a physical Interface for IP1 
			Intf ip1If1 = new Intf();
			ip1If1.setName("ip1If1");
			ip1If1.setPhysical(true);
			ArrayList<String> layer11 = new ArrayList<String>();
			layer11.add("IP");

			ip1If1.setLayering(layer11);
			ArrayList<String> address11 = new ArrayList<String>();
			address11.add("192.168.1.11");
			ip1If1.setAddress(address11);

			//Create a physical Interface for IP1
			Intf ip1If2 = new Intf();
			ip1If2.setName("ip1If2");
			ip1If2.setPhysical(true);
			ArrayList<String> layer12 = new ArrayList<String>();
			layer12.add("IP");

			ip1If2.setLayering(layer12);
			ArrayList<String> address12 = new ArrayList<String>();
			address12.add("192.168.1.12");
			ip1If2.setAddress(address12);
			//Create a physical Interface for IP1
			Intf ip1If3 = new Intf();
			ip1If3.setName("ip1If3");
			ip1If3.setPhysical(true);
			ArrayList<String> layer13 = new ArrayList<String>();
			layer13.add("IP");

			ip1If3.setLayering(layer13);
			ArrayList<String> address13 = new ArrayList<String>();
			address13.add("192.168.1.13");
			ip1If3.setAddress(address13);

			//Add list of interfaces
			ArrayList<Intf> interfacesIP1 = new ArrayList<Intf>();
			interfacesIP1.add(ip1If1);
			interfacesIP1.add(ip1If2);
			interfacesIP1.add(ip1If3);
			ip1.setIntfList(interfacesIP1);




			/*----------------- Node IP2 --------------------*/
			Node ip2 = new Node();	
			ip2.setNodeID("IP2");
			ip2.setDomain(1);
			ip2.setLocation(new Location(1, 2));
			ip2.setPhysical(true);
			ip2.setParentRouter(null);
			ip2.setIpParams(new IPNodeParams());
			ip2.setAddress(new ArrayList<String>());
			ip2.getAddress().add("192.168.0.2");

			//Create a physical Interface for IP2
			Intf ip2If1 = new Intf();
			ip2If1.setName("ip2If1");	
			ip2If1.setPhysical(true);
			ArrayList<String> layer21 = new ArrayList<String>();
			layer21.add("IP");	
			ip2If1.setLayering(layer21);	
			ArrayList<String> address21 = new ArrayList<String>();
			address21.add("192.168.1.21");
			ip2If1.setAddress(address21);


			//Create a physical Interface for IP2
			Intf ip2If2 = new Intf();
			ip2If2.setName("ip2If2");	
			ip2If2.setPhysical(true);
			ArrayList<String> layer22 = new ArrayList<String>();
			layer22.add("IP");	
			ip2If2.setLayering(layer22);	
			ArrayList<String> address22 = new ArrayList<String>();
			address22.add("192.168.1.22");
			ip2If2.setAddress(address22);

			//Create a physical Interface for IP2
			Intf ip2If3 = new Intf();
			ip2If3.setName("ip2If3");	
			ip2If3.setPhysical(true);
			ArrayList<String> layer23 = new ArrayList<String>();
			layer23.add("IP");	
			ip2If3.setLayering(layer23);	
			ArrayList<String> address23 = new ArrayList<String>();
			address23.add("192.168.1.23");
			ip2If3.setAddress(address23);

			ArrayList<Intf> interfaces2 = new ArrayList<Intf>();
			interfaces2.add(ip2If1);	
			interfaces2.add(ip2If2);
			ip2.setIntfList(interfaces2);

			/*----------------- Node IP3 --------------------*/
			Node ip3 = new Node();

			ip3.setNodeID("IP3");
			ip3.setDomain(1);
			ip3.setLocation(new Location(1, 3));
			ip3.setPhysical(true);
			ip3.setParentRouter(null);
			ip3.setIpParams(new IPNodeParams());
			ip3.setAddress(new ArrayList<String>());
			ip3.getAddress().add("192.168.0.3");

			//Create a physical Interface for IP3 --- 1
			Intf ip3If1 = new Intf();
			ip3If1.setName("ip3If1");	
			ip3If1.setPhysical(true);
			ArrayList<String> layer31 = new ArrayList<String>();
			layer31.add("IP");
			ip3If1.setLayering(layer31);
			ArrayList<String> address31 = new ArrayList<String>();
			address31.add("192.168.1.31");
			ip3If1.setAddress(address31);

			//Create a physical Interface for IP3 --- 2
			Intf ip3If2 = new Intf();
			ip3If2.setName("ip3If2");
			ip3If2.setPhysical(true);
			ArrayList<String> layer32 = new ArrayList<String>();
			layer32.add("IP");
			ip3If2.setLayering(layer32);
			ArrayList<String> address32 = new ArrayList<String>();
			address32.add("192.168.1.32");
			ip3If2.setAddress(address32);

			//Create a physical Interface for IP3 --- 3
			Intf ip3If3 = new Intf();
			ip3If3.setName("ip3If3");
			ip3If3.setPhysical(true);
			ArrayList<String> layer33 = new ArrayList<String>();
			layer33.add("IP");
			ip3If3.setLayering(layer33);
			ArrayList<String> address33 = new ArrayList<String>();
			address33.add("192.168.1.33");
			ip3If3.setAddress(address33);

			ArrayList<Intf> interfaces3 = new ArrayList<Intf>();
			interfaces3.add(ip3If1);
			interfaces3.add(ip3If2);
			interfaces3.add(ip3If3);
			ip3.setIntfList(interfaces3);


			/*----------------- Node IP4 --------------------*/	
			Node ip4 = new Node();	
			ip4.setNodeID("IP4");
			ip4.setDomain(1);
			ip4.setLocation(new Location(1, 4));
			ip4.setPhysical(true);
			ip4.setParentRouter(null);
			ip4.setIpParams(new IPNodeParams());
			ip4.setAddress(new ArrayList<String>());
			ip4.getAddress().add("192.168.0.4");

			//Create a physical Interface for IP4 --- 1
			Intf ip4If1 = new Intf();
			ip4If1.setName("ip4If1");	
			ip4If1.setPhysical(true);
			ArrayList<String> layer41 = new ArrayList<String>();
			layer41.add("IP");	
			ip4If1.setLayering(layer41);	
			ArrayList<String> address41 = new ArrayList<String>();
			address41.add("192.168.1.41");
			ip4If1.setAddress(address41);
			//Create a physical Interface for IP4 --- 2
			Intf ip4If2 = new Intf();
			ip4If2.setName("ip4If2");
			ip4If2.setPhysical(true);
			ArrayList<String> layer42 = new ArrayList<String>();
			layer42.add("IP");	
			ip4If2.setLayering(layer42);	
			ArrayList<String> address42 = new ArrayList<String>();
			address42.add("192.168.1.42");
			ip4If2.setAddress(address42);
			//Create a physical Interface for IP4 --- 3
			Intf ip4If3 = new Intf();
			ip4If3.setName("ip4If3");	
			ip4If3.setPhysical(true);
			ArrayList<String> layer43 = new ArrayList<String>();
			layer43.add("IP");	
			ip4If3.setLayering(layer43);	
			ArrayList<String> address43 = new ArrayList<String>();
			address43.add("192.168.1.43");
			ip4If3.setAddress(address43);

			ArrayList<Intf> interfaces4 = new ArrayList<Intf>();
			interfaces4.add(ip4If1);
			interfaces4.add(ip4If2);
			interfaces4.add(ip4If3);
			ip4.setIntfList(interfaces4);

			/*----------------- Node O1 --------------------*/	
			Node o1 = new Node();	
			o1.setNodeID("O1");
			o1.setDomain(1);
			o1.setLocation(new Location(2, 1));
			o1.setPhysical(true);
			o1.setParentRouter(null);
			o1.setIpParams(new IPNodeParams());
			o1.setAddress(new ArrayList<String>());
			o1.getAddress().add("1.1.1.1");
			//Create a physical Interface for O1 --- 1
			Intf o1If1 = new Intf();
			o1If1.setName("o1If1");	
			o1If1.setPhysical(true);
			ArrayList<String> layer51 = new ArrayList<String>();
			layer51.add("Transport");	
			o1If1.setLayering(layer51);	
			ArrayList<String> address51 = new ArrayList<String>();
			address51.add("1.1.1.11");	
			o1If1.setAddress(address51);
			//Create a physical Interface for O1 --- 2
			Intf o1If2 = new Intf();
			o1If2.setName("o1If2");	
			o1If2.setPhysical(true);
			ArrayList<String> layer52 = new ArrayList<String>();
			layer52.add("Transport");	
			o1If2.setLayering(layer52);	
			ArrayList<String> address52 = new ArrayList<String>();
			address52.add("1.1.1.12");
			o1If2.setAddress(address52);
			//Create a physical Interface for O1 --- 3
			Intf o1If3 = new Intf();
			o1If3.setName("o1If3");	
			o1If3.setPhysical(true);
			ArrayList<String> layer53 = new ArrayList<String>();
			layer53.add("Transport");	
			o1If3.setLayering(layer53);	
			ArrayList<String> address53 = new ArrayList<String>();
			address53.add("1.1.1.13");
			o1If3.setAddress(address53);

			ArrayList<Intf> interfaces5 = new ArrayList<Intf>();
			interfaces5.add(o1If1);
			interfaces5.add(o1If2);
			interfaces5.add(o1If3);
			o1.setIntfList(interfaces5);
			/*----------------- Node O2 --------------------*/	
			Node o2 = new Node();	
			o2.setNodeID("O2");
			o2.setDomain(1);
			o2.setLocation(new Location(2, 2));
			o2.setPhysical(true);
			o2.setParentRouter(null);
			o2.setIpParams(new IPNodeParams());
			o2.setAddress(new ArrayList<String>());
			o2.getAddress().add("1.1.1.2");
			//Create a physical Interface for O2 --- 1
			Intf o2If1 = new Intf();
			o2If1.setName("o2If1");	
			o2If1.setPhysical(true);
			ArrayList<String> layer61 = new ArrayList<String>();
			layer61.add("Transport");	
			o2If1.setLayering(layer61);	
			ArrayList<String> address61 = new ArrayList<String>();
			address61.add("1.1.1.21");
			o2If1.setAddress(address61);
			//Create a physical Interface for O2 --- 2
			Intf o2If2 = new Intf();
			o2If2.setName("o2If2");	
			o2If2.setPhysical(true);
			ArrayList<String> layer62 = new ArrayList<String>();
			layer62.add("Transport");	
			o2If2.setLayering(layer62);	
			ArrayList<String> address62 = new ArrayList<String>();
			address62.add("1.1.1.22");
			o2If2.setAddress(address62);
			//Create a physical Interface for O2 --- 3
			Intf o2If3 = new Intf();
			o2If3.setName("o2If3");	
			o2If3.setPhysical(true);
			ArrayList<String> layer63 = new ArrayList<String>();
			layer63.add("Transport");	
			o2If3.setLayering(layer63);	
			ArrayList<String> address63 = new ArrayList<String>();
			address63.add("1.1.1.23");
			o2If3.setAddress(address63);
			ArrayList<Intf> interfaces6 = new ArrayList<Intf>();
			interfaces6.add(o2If1);
			interfaces6.add(o2If2);
			interfaces6.add(o2If3);
			o2.setIntfList(interfaces6);
			/*----------------- Node O3 --------------------*/	
			Node o3 = new Node();	
			o3.setNodeID("O3");
			o3.setDomain(1);
			o3.setLocation(new Location(2, 3));
			o3.setPhysical(true);
			o3.setParentRouter(null);
			o3.setIpParams(new IPNodeParams());
			o3.setAddress(new ArrayList<String>());
			o3.getAddress().add("1.1.1.3");
			//Create a physical Interface for O3 --- 1
			Intf o3If1 = new Intf();
			o3If1.setName("o3If1");	
			o3If1.setPhysical(true);
			ArrayList<String> layer71 = new ArrayList<String>();
			layer71.add("Transport");	
			o3If1.setLayering(layer71);	
			ArrayList<String> address71 = new ArrayList<String>();
			address71.add("1.1.1.31");
			o3If1.setAddress(address71);
			//Create a physical Interface for O3 --- 2
			Intf o3If2 = new Intf();
			o3If2.setName("o3If2");	
			o3If2.setPhysical(true);
			ArrayList<String> layer72 = new ArrayList<String>();
			layer72.add("Transport");	
			o3If2.setLayering(layer72);	
			ArrayList<String> address72 = new ArrayList<String>();
			address72.add("1.1.1.32");
			o3If2.setAddress(address72);
			//Create a physical Interface for O3 --- 3
			Intf o3If3 = new Intf();
			o3If3.setName("o3If3");	
			o3If3.setPhysical(true);
			ArrayList<String> layer73 = new ArrayList<String>();
			layer73.add("Transport");	
			o3If3.setLayering(layer73);	
			ArrayList<String> address73 = new ArrayList<String>();
			address73.add("1.1.1.33");
			o3If3.setAddress(address73);
			//Create a physical Interface for O3 --- 4
			Intf o3If4 = new Intf();
			o3If4.setName("o3If4");	
			o3If4.setPhysical(true);
			ArrayList<String> layer74 = new ArrayList<String>();
			layer74.add("Transport");	
			o3If4.setLayering(layer74);	
			ArrayList<String> address74 = new ArrayList<String>();
			address74.add("1.1.1.34");
			o3If4.setAddress(address74);
			ArrayList<Intf> interfaces7 = new ArrayList<Intf>();
			interfaces7.add(o3If1);
			interfaces7.add(o3If2);
			interfaces7.add(o3If3);
			interfaces7.add(o3If4);
			o3.setIntfList(interfaces7);
			/*----------------- Node O4 --------------------*/	
			Node o4 = new Node();	
			o4.setNodeID("O4");
			o4.setDomain(1);
			o4.setLocation(new Location(2, 4));
			o4.setPhysical(true);
			o4.setParentRouter(null);
			o4.setIpParams(new IPNodeParams());
			o4.setAddress(new ArrayList<String>());
			o4.getAddress().add("1.1.1.4");

			//Create a physical Interface for O4 --- 1
			Intf o4If1 = new Intf();
			o4If1.setName("o4If1");	
			o4If1.setPhysical(true);
			ArrayList<String> layer81 = new ArrayList<String>();
			layer81.add("Transport");	
			o4If1.setLayering(layer81);	
			ArrayList<String> address81 = new ArrayList<String>();
			address81.add("1.1.1.41");
			o4If1.setAddress(address81);
			//Create a physical Interface for O4 --- 2
			Intf o4If2 = new Intf();
			o4If2.setName("o4If2");	
			o4If2.setPhysical(true);
			ArrayList<String> layer82 = new ArrayList<String>();
			layer82.add("Transport");	
			o4If2.setLayering(layer82);	

			ArrayList<String> address82 = new ArrayList<String>();
			address82.add("1.1.1.42");
			o4If2.setAddress(address82);

			ArrayList<Intf> interfaces8 = new ArrayList<Intf>();
			interfaces8.add(o4If1);
			interfaces8.add(o4If2);
			o4.setIntfList(interfaces8);
			/*-------------- ADD NODES -----------------*/
			nodes.put(ip1.getNodeID(), ip1);
			nodes.put(ip2.getNodeID(), ip2);	
			nodes.put(ip3.getNodeID(), ip3);
			nodes.put(ip4.getNodeID(), ip4);
			nodes.put(o1.getNodeID(), o1);
			nodes.put(o2.getNodeID(), o2);
			nodes.put(o3.getNodeID(), o3);
			nodes.put(o4.getNodeID(), o4);
			/*--------------------- CREATE Links --------------------------*/
			ArrayList<Link> linksIP = new ArrayList<Link>();
			ArrayList<Link> linksTransport = new ArrayList<Link>();
			ArrayList<Link> linksInterlayer = new ArrayList<Link>();
			/*IP1 - IP2*/
			Link link1 = new Link();
			EndPoint source = new EndPoint("IP1","192.168.0.1");
			EndPoint dest = new EndPoint("IP2","192.168.0.2");
			link1.setDirectional(true);
			link1.setSource(source);
			link1.setDest(dest);
			link1.setType("intradomain");
			linksIP.add(link1);

			/*IP1 - IP3*/
			Link link2 = new Link();
			EndPoint source2 = new EndPoint("IP1","192.168.0.1");
			EndPoint dest2 = new EndPoint("IP3","192.168.0.3");
			link2.setDirectional(true);
			link2.setSource(source2);
			link2.setDest(dest2);	
			link2.setType("intradomain");
			linksIP.add(link2);
			
			/*IP1 - O1*/
			Link link3 = new Link();
			EndPoint source3 = new EndPoint("IP1","192.168.0.1");
			EndPoint dest3 = new EndPoint("O1","1.1.1.1");
			link3.setSource(source3);
			link3.setDest(dest3);	
			link3.setDirectional(true);
			link3.setType("interlayer");
			linksInterlayer.add(link3);

			/*IP2 - IP4*/
			Link link4 = new Link();
			EndPoint source4 = new EndPoint("IP2","192.168.0.2");
			EndPoint dest4 = new EndPoint("IP4","192.168.0.4");
			link4.setSource(source4);
			link4.setDest(dest4);	
			link4.setDirectional(true);
			link4.setType("intradomain");
			linksIP.add(link4);

			/*IP3 - IP4*/
			Link link5 = new Link();
			EndPoint source5 = new EndPoint("IP3","192.168.0.3");
			EndPoint dest5 = new EndPoint("IP4","192.168.0.4");
			link5.setSource(source5);
			link5.setDest(dest5);	
			link5.setDirectional(true);
			link5.setType("intradomain");
			linksIP.add(link5);



			/*IP3 - O3*/
			Link link6 = new Link();
			EndPoint source6 = new EndPoint("IP3","192.168.0.3");
			EndPoint dest6 = new EndPoint("O3","1.1.1.3");
			link6.setSource(source6);
			link6.setDest(dest6);	
			link6.setDirectional(true);
			link6.setType("interlayer");
			linksInterlayer.add(link6);

			/*IP4 - O4*/
			Link link7 = new Link();
			EndPoint source7 = new EndPoint("IP4","192.168.0.4");
			EndPoint dest7 = new EndPoint("O4","1.1.1.4");
			link7.setSource(source7);
			link7.setDest(dest7);	
			link7.setDirectional(true);
			link7.setType("interlayer");
			linksInterlayer.add(link7);

			/*IP2 - O2*/
			Link link8 = new Link();
			EndPoint source8 = new EndPoint("IP2","192.168.0.2");
			EndPoint dest8 = new EndPoint("O2","1.1.1.2");
			link8.setSource(source8);
			link8.setDest(dest8);	
			link8.setDirectional(true);
			link8.setType("interlayer");
			linksInterlayer.add(link8);

			/*O1 - O2*/
			Link link9 = new Link();
			EndPoint source9 = new EndPoint("O1","1.1.1.1");
			EndPoint dest9 = new EndPoint("O2","1.1.1.2");
			link9.setSource(source9);
			link9.setDest(dest9);	
			link9.setDirectional(true);
			link9.setType("intradomain");
			linksTransport.add(link9);
			/*O1 - O3*/
			Link link10 = new Link();
			EndPoint source10 = new EndPoint("O1","1.1.1.1");
			EndPoint dest10 = new EndPoint("O3","1.1.1.3");
			link10.setSource(source10);
			link10.setDest(dest10);	
			link10.setDirectional(true);
			link10.setType("intradomain");
			linksTransport.add(link10);
			/*O2 - O3*/
			Link link11 = new Link();
			EndPoint source11 = new EndPoint("O2","1.1.1.2");
			EndPoint dest11 = new EndPoint("O3","1.1.1.3");
			link11.setSource(source11);
			link11.setDest(dest11);	
			link11.setDirectional(true);
			link11.setType("intradomain");
			linksTransport.add(link11);
			/*O3 - O4*/
			Link link12 = new Link();
			EndPoint source12 = new EndPoint("O3","1.1.1.3");
			EndPoint dest12 = new EndPoint("O4","1.1.1.4");
			link12.setSource(source12);
			link12.setDest(dest12);	
			link12.setDirectional(true);
			link12.setType("intradomain");
			linksTransport.add(link12);
			//	ip1If1.


		}

		@Override
		public String toString() {
			String topology;			 
			Iterator <Node> nodesIterator=nodes.values().iterator();
			topology = "NODES: \n\t*  ";
			while (nodesIterator.hasNext()){
				Node node = nodesIterator.next();
				topology = topology + node.toString();
				if(nodesIterator.hasNext())
					topology = topology + "\n*  ";


			}
			Set<String> keys=  links.keySet();
			
			Iterator <String> keysIterator=keys.iterator();
			while (keysIterator.hasNext()){
				topology = topology+ "\n******************************************************************\n";
				topology = topology + "LINKS: \n\t*  ";		
				String layer = keysIterator.next();
				ArrayList<Link> linkSet = links.get(layer);
				Iterator <Link> linksIterator = null;
				if (linkSet != null){
					topology = topology + layer+" Layer\n* ";
					linksIterator=linkSet.iterator();
					while (linksIterator.hasNext()){
						Link link = linksIterator.next();
						//if (link.getType().equals("intradomain")){
							topology = topology + link.toString();
							if (linksIterator.hasNext())
								topology = topology + "\n\t*  ";
						
					//}
					}
				}
			}

			return topology;
		}
		

		

	}


