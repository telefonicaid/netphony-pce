package tid.pce.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.jgrapht.graph.SimpleDirectedWeightedGraph;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import tid.pce.tedb.DomainTEDB;
import tid.pce.tedb.IntraDomainEdge;
import tid.pce.tedb.SimpleTEDB;
import tid.pce.tedb.TE_Information;
import tid.provisioningManager.objects.RouterInfoPM;
import tid.util.UtilsFunctions;

/**
 * Thread that reads topology from Openflow controller and from XML if there are intradomain links
 * @author jaume
 *
 */


public class TopologyUpdaterVLAN extends Thread
{
	private ArrayList<String> ips = null;
	private ArrayList<String> ports = null;
	private String topologyPathNodes = "";
	private String topologyPathLinks = "";
	private SimpleTEDB TEDB;
	private Logger log;
	private Lock lock = null;
	private String interDomainFile = null;
	
	private Hashtable<Integer,MyEdge> interDomainLinks = new Hashtable<Integer,MyEdge>();
	
	public TopologyUpdaterVLAN(String ip,String port, String topologyPathLinks, String topologyPathNodes,DomainTEDB ted, Logger log)
	{
		ips = new ArrayList<String>();
		ports = new ArrayList<String>();
		
		ips.add(ip);
		ports.add(port);

		this.topologyPathLinks = topologyPathLinks;
		this.topologyPathNodes = topologyPathNodes;
		this.TEDB = (SimpleTEDB)ted;
		this.log = log;
	}
	
	public TopologyUpdaterVLAN(String ip, String port, String topologyPathLinks, String topologyPathNodes,DomainTEDB ted, Logger log, Lock lock)
	{
		ips = new ArrayList<String>();
		ports = new ArrayList<String>();
		
		ips.add(ip);
		ports.add(port);
		
		this.topologyPathLinks = topologyPathLinks;
		this.topologyPathNodes = topologyPathNodes;
		this.TEDB = (SimpleTEDB)ted;
		this.log = log;
		this.lock = lock;
	}
	
	public TopologyUpdaterVLAN(ArrayList<String> ips, ArrayList<String>ports , String topologyPathLinks, String topologyPathNodes,DomainTEDB ted, Logger log)
	{
		this.ips = ips;
		this.ports = ports;
		this.topologyPathLinks = topologyPathLinks;
		this.topologyPathNodes = topologyPathNodes;
		this.TEDB = (SimpleTEDB)ted;
		this.log = log;
	}
	
	
	
	private class MyEdge
	{
		String source;
		String dest;
		Integer source_port;
		Integer dest_port;
		Integer vlan;
		
		MyEdge(String source, String dest)
		{
			this.source = source;
			this.dest = dest;
		}
		
		MyEdge(String source, String dest, Integer vlan, Integer source_port, Integer dest_port)
		{
			this.source = source;
			this.dest = dest;
			this.source_port = source_port;
			this.dest_port = dest_port; 
			this.vlan = vlan;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((dest == null) ? 0 : dest.hashCode());
			result = prime * result
					+ ((source == null) ? 0 : source.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			MyEdge other = (MyEdge) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (dest == null) {
				if (other.dest != null)
					return false;
			} else if (!dest.equals(other.dest))
				return false;
			if (source == null) {
				if (other.source != null)
					return false;
			} else if (!source.equals(other.source))
				return false;
			return true;
		}

		private TopologyUpdaterVLAN getOuterType() {
			return TopologyUpdaterVLAN.this;
		}		
		
		
	}
	
	
	@Override
	public void run()
	{	
		
		if(interDomainFile != null)
		{
			interDomainLinks = readInterDomainFile();
		}
		
		String responseLinks = "";
		String responseNodes = "";
		
		try
		{
			//log.info("TopologyUpdaterWLAN thread, Updating TEDB");
			SimpleDirectedWeightedGraph<Object, IntraDomainEdge> networkGraph = new SimpleDirectedWeightedGraph<Object, IntraDomainEdge>(IntraDomainEdge.class);
			TEDB.setNetworkGraph(networkGraph);
			
			Hashtable<String,RouterInfoPM> nodes = new Hashtable<String,RouterInfoPM>();
			
			for (int i = 0; i < ips.size(); i++)
			{
				responseNodes = queryForNodes(ips.get(i), ports.get(i));
				parseNodes(responseNodes, nodes, ips.get(i), ports.get(i));
				log.info("responseNodes:::"+responseNodes);
			}
			
			for (int i = 0; i < ips.size(); i++)
			{			
				responseLinks = queryForLinks(ips.get(i), ports.get(i));	
						
		        log.info("responseLinks:::"+responseLinks);
		        
		        lock();
		        parseLinks(responseLinks, nodes);
		        unlock();
	        
			}
	        
	        parseRemainingLinksFromXML(nodes);
			
	        //parseJSON("[{\"src-switch\":\"00:14:2c:59:e5:5e:2b:00\",\"src-port\":20,\"src-port-state\":0,\"dst-switch\":\"00:14:2c:59:e5:64:21:00\",\"dst-port\":19,\"dst-port-state\":0,\"type\":\"internal\"},{\"src-switch\":\"00:14:2c:59:e5:64:21:00\",\"src-port\":19,\"src-port-state\":0,\"dst-switch\":\"00:14:2c:59:e5:5e:2b:00\",\"dst-port\":20,\"dst-port-state\":0,\"type\":\"internal\"},{\"src-switch\":\"00:14:2c:59:e5:66:ed:00\",\"src-port\":9,\"src-port-state\":0,\"dst-switch\":\"00:14:2c:59:e5:64:21:00\",\"dst-port\":20,\"dst-port-state\":0,\"type\":\"internal\"},{\"src-switch\":\"00:14:2c:59:e5:64:21:00\",\"src-port\":20,\"src-port-state\":0,\"dst-switch\":\"00:14:2c:59:e5:66:ed:00\",\"dst-port\":9,\"dst-port-state\":0,\"type\":\"internal\"}]");
	        //System.out.println(response);
		}
		catch (Exception e)
		{
			log.info(UtilsFunctions.exceptionToString(e));
		}
	}
	
	private Hashtable<Integer, MyEdge> readInterDomainFile() 
	{
		log.info("Parsing intradomain File");
		interDomainLinks = new Hashtable<Integer,MyEdge>();
		try 
		{
			DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			File confFile = new File(this.interDomainFile);		
			Document doc = builder.parse(confFile);
			
			NodeList list_nodes_Edges = doc.getElementsByTagName("edge");
			log.info("num edges: " + list_nodes_Edges.getLength());
			for (int i = 0; i < list_nodes_Edges.getLength(); i++) 
			{
				Element nodes_servers = (Element) list_nodes_Edges.item(i);
				String source = getCharacterDataFromElement((Element) nodes_servers.getElementsByTagName("source").item(0));
				String dest = getCharacterDataFromElement((Element) nodes_servers.getElementsByTagName("dest").item(0));
				Integer vlan = Integer.parseInt(getCharacterDataFromElement((Element) nodes_servers.getElementsByTagName("vlan").item(0)));
				String direction = getCharacterDataFromElement((Element) nodes_servers.getElementsByTagName("direction").item(0));
				int source_port = Integer.parseInt(getCharacterDataFromElement((Element) nodes_servers.getElementsByTagName("source_port").item(0)));
				int dest_port = Integer.parseInt(getCharacterDataFromElement((Element) nodes_servers.getElementsByTagName("dest_port").item(0)));
				
				log.info("Adding IntraDomain Link! source: "+source+", dest: "+dest+", source_port: "+source_port+", dest_port: "+dest_port);
				
				MyEdge auxEdge = new MyEdge(source, dest, vlan, source_port, dest_port);
				interDomainLinks.put(auxEdge.hashCode(), auxEdge);
				
				if (direction.equals("bidirectional"))
				{
					MyEdge reverseEdge = new MyEdge(dest, source, vlan, source_port, dest_port);
					interDomainLinks.put(reverseEdge.hashCode(), reverseEdge);
				}
			}
		} 
		catch (Exception e) 
		{
			log.info(UtilsFunctions.exceptionToString(e));
		}
		
		return interDomainLinks;
	}

	private void parseNodes(String response, Hashtable<String,RouterInfoPM> routerInfoList, String ip, String port)
	{	
		try
		{
			JSONParser parser = new JSONParser();
			Object obj = parser.parse(response);
		
			JSONArray msg = (JSONArray) obj;
			Iterator<JSONObject> iterator = msg.iterator();
			while (iterator.hasNext()) 
			{
				JSONObject jsonObject = (JSONObject) iterator.next();
				
				RouterInfoPM rInfo = new RouterInfoPM();
				rInfo.setMacAddress((String)jsonObject.get("mac"));
				
				rInfo.setRouterID((String)jsonObject.get("dpid"));
				
				
				JSONArray ports = (JSONArray) jsonObject.get("ports");
				Iterator<JSONObject> portIterator = ports.iterator();
				while (portIterator.hasNext()) 
				{
					JSONObject jsonPortObject = (JSONObject) portIterator.next();
					rInfo.setMacAddress((String)jsonPortObject.get("hardwareAddress"));
				}
				
				log.info("(String)((JSONObject)jsonObject.get(description)).get(manufacturer)::"+(String)((JSONObject)jsonObject.get("description")).get("manufacturer"));
				rInfo.setRouterType((String)((JSONObject)jsonObject.get("description")).get("manufacturer"));
				rInfo.setConfigurationMode("Openflow");
				
				rInfo.setControllerIdentifier(ip, port);
				rInfo.setControllerIP(ip);
				rInfo.setControllerPort(port);
				rInfo.setHardware((String)((JSONObject)jsonObject.get("description")).get("hardware"));
				
				routerInfoList.put(rInfo.getRouterID(),rInfo);
				
				
				log.info("Adding Vertex::"+rInfo);
				((SimpleTEDB)TEDB).getNetworkGraph().addVertex(rInfo);
			}
		}
		catch (Exception e)
		{
			log.info(UtilsFunctions.exceptionToString(e));
		}
	}
	
	private void parseLinks(String links,Hashtable<String,RouterInfoPM> nodes)
	{
		try {
			//log.info("Inside parseJSON");
			JSONParser parser = new JSONParser();
			Object obj = parser.parse(links);
		
			JSONArray msg = (JSONArray) obj;
			Iterator<JSONObject> iterator = msg.iterator();
			while (iterator.hasNext()) 
			{
				JSONObject jsonObject = (JSONObject) iterator.next();
				//System.out.println(jsonObject.get("src-switch"));
				IntraDomainEdge edge= new IntraDomainEdge();

				RouterInfoPM source = nodes.get(jsonObject.get("src-switch"));
				RouterInfoPM dest = nodes.get(jsonObject.get("dst-switch"));
				
				
				//((SimpleTEDB)TEDB).getNetworkGraph().addVertex(source);
				//((SimpleTEDB)TEDB).getNetworkGraph().addVertex(dest);

				log.info("Adding Vertex->"+source+" hashcode:"+source.hashCode());
				log.info("Adding Vertex->"+dest+" hashcode:"+dest.hashCode());
				
				edge.setSrc_if_id((Long)jsonObject.get("src-port"));
				edge.setDst_if_id((Long)jsonObject.get("dst-port"));
				
				
				  // This is a big problem because info is not initialized from file
				  // and the controller doesn't give information about how many wlans
				  // the are
				
				TE_Information tE_info = new TE_Information();
				tE_info.setNumberWLANs(15);
				tE_info.initWLANs();
				
				if (interDomainFile != null)
				{
					completeTE_Information(tE_info, source.getRouterID(), dest.getRouterID());
				}
				
				edge.setTE_info(tE_info);
				
				String isBidirectional = (String)jsonObject.get("direction");
				
				
				
				//log.info("isBidirectional::"+isBidirectional);
				
				if ((1==1)||(isBidirectional != null) && (isBidirectional.equals("bidirectional")))
				{
					//((SimpleTEDB)TEDB).getNetworkGraph().addEdge(source, dest, edge);
					
					TE_Information tE_infoOtherWay = new TE_Information();
					tE_infoOtherWay.setNumberWLANs(15);
					tE_infoOtherWay.initWLANs();
					IntraDomainEdge edgeOtherWay= new IntraDomainEdge();
					
					edgeOtherWay.setSrc_if_id((Long)jsonObject.get("dst-port"));
					edgeOtherWay.setDst_if_id((Long)jsonObject.get("src-port"));
					edgeOtherWay.setTE_info(tE_infoOtherWay);
					
					((SimpleTEDB)TEDB).getNetworkGraph().addEdge(source, dest, edge);
					((SimpleTEDB)TEDB).getNetworkGraph().addEdge(dest, source, edgeOtherWay);
					
					completeTE_Information(tE_info, dest.getRouterID(), source.getRouterID());
					
					log.info("source::"+source);
					log.info("dest::"+dest);
					log.info("edgeOtherWay::"+edgeOtherWay);
					log.info("edge::"+edge);
					//log.info("Adding two!");
				}
				else
				{
					((SimpleTEDB)TEDB).getNetworkGraph().addEdge(source, dest, edge);
				}
				
				//log.info("Edge added:"+edge);
				//log.info(((SimpleTEDB)TEDB).getIntraDomainLinks().toString());
			}
			//parseRemainingLinksFromXML(nodes);
	 
		} 
		catch (Exception e)
		{
			log.info(UtilsFunctions.exceptionToString(e));
		}
	}
	
	private void parseRemainingLinksFromXML(Hashtable<String,RouterInfoPM> nodes) 
	{
		Map<Integer, MyEdge> map = interDomainLinks;
		Iterator<Map.Entry<Integer, MyEdge>> it = map.entrySet().iterator();
		while (it.hasNext()) 
		{
			Map.Entry<Integer, MyEdge> entry = it.next();

			MyEdge edgeAux = entry.getValue(); 
			
			IntraDomainEdge edge= new IntraDomainEdge();
			edge.setSrc_if_id(new Long(edgeAux.source_port));
			edge.setDst_if_id(new Long(edgeAux.dest_port));
			
			TE_Information tE_info = new TE_Information();
			tE_info.setNumberWLANs(15);
			tE_info.initWLANs();
			
			tE_info.setVlanLink(true);
			tE_info.setVlan(edgeAux.vlan);
			
			edge.setTE_info(tE_info);
			
			log.info("nodes.get(edgeAux.source)::"+nodes.get(edgeAux.source));
			log.info("edgeAux.source::"+edgeAux.source);
			log.info("nodes.get(edgeAux.dest)::"+nodes.get(edgeAux.dest));
			log.info("edgeAux.dest::"+edgeAux.dest);
			
			log.info("Adding InterDomain Edge!!::Vlan::"+edgeAux.vlan);
			((SimpleTEDB)TEDB).getNetworkGraph().addEdge(nodes.get(edgeAux.source), nodes.get(edgeAux.dest), edge);
		}
	}

	private void completeTE_Information(TE_Information tE_info, String source, String dest) 
	{
		MyEdge auxEdge = new MyEdge(source, dest);
		MyEdge completEdge = interDomainLinks.get(auxEdge.hashCode());
		if ((completEdge != null)&&(completEdge.vlan != null))
		{
			tE_info.setVlanLink(true);
			tE_info.setVlan(completEdge.vlan);
			//If it has been found it will be removed so the rest can be proccessed later
			interDomainLinks.remove(completEdge.vlan);
		}
		else
		{
			tE_info.setVlanLink(false);
		}
	}

	private String queryForLinks(String ip, String port)
	{
		String response = ""; 
		try
		{
			URL topoplogyURL = new URL("http://"+ip+":"+port+topologyPathLinks);
			
			//log.info("URL::"+"http://"+ip+":"+port+topologyPathLinks);
			
	        URLConnection yc = topoplogyURL.openConnection();
	        BufferedReader in = new BufferedReader(new InputStreamReader(yc.getInputStream()));
	        String inputLine;
	
	        while ((inputLine = in.readLine()) != null) 
	        {
	        	response = response + inputLine;
	        }
		}
		catch (Exception e)
		{
			log.info(UtilsFunctions.exceptionToString(e));
		}
        return response;
	}
	
	private String queryForNodes(String ip, String port)
	{
        String response = "";
		try
		{
			URL topoplogyURL = new URL("http://"+ip+":"+port+topologyPathNodes);
			
			log.info("http://+port+topologyPathNodes:::"+"http://"+ip+":"+port+topologyPathNodes);
			
	        URLConnection yc = topoplogyURL.openConnection();
	        BufferedReader in = new BufferedReader(new InputStreamReader(yc.getInputStream()));
	        
	        String inputLine;	
	        while ((inputLine = in.readLine()) != null) 
	        {
	        	response = response + inputLine;
	        }
	        in.close();
		}
		catch (Exception e)
		{
			log.info(UtilsFunctions.exceptionToString(e));
		}
        return response;
	}
	
	public String getInterDomainFile() 
	{
		return interDomainFile;
	}

	public void setInterDomainFile(String interDomainFile) 
	{
		this.interDomainFile = interDomainFile;
	}
	
	
	private void lock()
	{
		if (lock != null)
		{
			lock.lock();
		}
	}
	
	private void unlock()
	{
		if (lock != null)
		{
			lock.unlock();
		}
	}
	
	private String getCharacterDataFromElement(Element e) {
		Node child = e.getFirstChild();
		if (child instanceof CharacterData) {
			CharacterData cd = (CharacterData) child;
			return cd.getData();
		} else {
			return "?";
		}
	}
}
