package tid.pce.server;

import java.io.File;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import tid.bgp.bgp4Peer.pruebas.BGPPeer;
import tid.ospf.ospfv2.OSPFv2LinkStateUpdatePacket;
import tid.pce.tedb.DomainTEDB;
import tid.pce.tedb.SimpleTEDB;
import tid.pce.tedb.ospfv2.OSPFSessionServer;
import tid.pce.tedb.ospfv2.OSPFTCPSessionServer;
import tid.util.UtilsFunctions;


/**
 * Topology server, receives a TED, initializes it and maintains it
 *
 */

public class TopologyManager 
{
	PCEServerParameters params;
	DomainTEDB ted;
	Logger log;
	
	public TopologyManager(PCEServerParameters params,DomainTEDB ted, Logger log)
	{
		this.params = params;
		this.ted = ted;
		this.log = log;
	}
	
	public void initTopology()
	{
		if (params.isWLAN())
		{
			initFromWLANController();
			params.initFromFile = false;
		}
		if (params.initFromFile)
		{
			initFromFile();
		}
		if (params.isOSPFSession())
		{
			log.info("Initializing from OSPF");
			initFromOSPF();
		}
		
		if (params.isActingAsBGP4Peer()) {//BGP
			log.info("Acting as BGP Peer");
			BGPPeer bgpPeer = new BGPPeer();		
			bgpPeer.configure(params.getBGP4File());			
			//bgpPeer.configure("PCEServerConfiguration.xml");			
			bgpPeer.setReadDomainTEDB(ted);
			bgpPeer.createUpdateDispatcher();
			bgpPeer.startClient();		
			bgpPeer.startServer();
			bgpPeer.startManagementServer();
			bgpPeer.startSendTopology();
		}
	}
	
	private void initFromFile() 
	{
		//Initialize Traffic Engineering Database
		if(params.ITcapable==true){
			log.info("Initializing IT capable TEDB");
			ted.initializeFromFile(params.getITNetworkDescriptionFile());
		}else{
			
			if (params.isMultilayer()){
				log.info("Initializing Multilayer TEDB");
				ted.initializeFromFile(params.getNetworkDescriptionFile());
			}else {
				if (params.isMultidomain()){
					log.info("Initializing TEDB joining all domains in a single one");
				}else {
					log.info("Initializing Single Layer TEDB");	
				}
				//STRONGEST: Independent PCE's with lambdas' sub-set assignment
				if (params.getLambdaEnd()!=Integer.MAX_VALUE){
					log.info("Entro en Max Value");
					((SimpleTEDB)ted).initializeFromFile(params.getNetworkDescriptionFile(),params.getLayer() ,params.isMultidomain(),params.getLambdaIni(),params.getLambdaEnd(),params.isSSOn(),false);
				}else if (params.isActingAsBGP4Peer()&&(params.isOSPFSession())){
					((SimpleTEDB)ted).initializeFromFile(params.getNetworkDescriptionFile(),params.getLayer() ,params.isMultidomain(),params.getLambdaIni(),params.getLambdaEnd(),true,false);
					log.info("Entro en OSPF y BGP");
				}
				else{
					log.info("Entro en el else:::"+params.isSSOn()+",params.isWLAN()"+params.isWLAN());
					log.info("params.getNetworkDescriptionFile()::"+params.getNetworkDescriptionFile());
					//params.setNetworkDescriptionFile("/usr/local/nodeConfig/topologia2.xml");
					((SimpleTEDB)ted).initializeFromFile(params.getNetworkDescriptionFile(),params.getLayer() ,params.isMultidomain(),0,Integer.MAX_VALUE ,params.isSSOn(),false, params.isWLAN());
				}
			}
		}
	}

	private void initFromOSPF()
	{	
		LinkedBlockingQueue<OSPFv2LinkStateUpdatePacket> ospfv2PacketQueue = new LinkedBlockingQueue<OSPFv2LinkStateUpdatePacket>();		
		TopologyUpdaterThread tut = null;
		
		if (params.isMultilayer()==true)
		{
			tut = new TopologyUpdaterThread(ospfv2PacketQueue, ted,params.getLambdaIni(),params.getLambdaEnd(), params.isCompletedAuxGraph(), params.isMultilayer());		
		}
		else
		{
			tut = new TopologyUpdaterThread(ospfv2PacketQueue, ted,params.getLambdaIni(),params.getLambdaEnd());
		}
		tut.start();
		
		/*
		 * Code added to handle the redis topology update
		 * 
		 */
		
		boolean redisDatabase = true;
				
		LinkedBlockingQueue<OSPFv2LinkStateUpdatePacket> redisOspfv2PacketQueue = new LinkedBlockingQueue<OSPFv2LinkStateUpdatePacket>();
		if(redisDatabase)
		{
			RedisTEDUpdaterThread rtut = new RedisTEDUpdaterThread(redisOspfv2PacketQueue);
			rtut.start();			
		}
		
		log.info("params.isOSPFTCPSession():::"+params.isOSPFTCPSession());
		log.info("params.isOSPFSession():::"+params.isOSPFSession());
		if (params.isOSPFTCPSession())
		{
			OSPFTCPSessionServer OSPFsessionserver= new OSPFTCPSessionServer(ospfv2PacketQueue,redisOspfv2PacketQueue);
			OSPFsessionserver.setOSPFTCPPort(params.getOSPFTCPPort());
			OSPFsessionserver.start();	
		}
		else if (params.isOSPFSession())
		{
			System.out.println("Es OSPF Session");
			OSPFSessionServer OSPFsessionserver = null;
			try {
				
				/*
				 * Redis database
				 */
				
				if(redisDatabase){
				
					OSPFsessionserver = new OSPFSessionServer(ospfv2PacketQueue, redisOspfv2PacketQueue, ((Inet4Address) InetAddress.getByName(params.getOSPFListenerIP())));
					OSPFsessionserver.setMulticast(params.isOSPFSession());
					OSPFsessionserver.start();
				
					
				}
				else{
				
					OSPFsessionserver = new OSPFSessionServer(ospfv2PacketQueue, ((Inet4Address) InetAddress.getByName(params.getOSPFListenerIP())));
					OSPFsessionserver.setMulticast(params.isOSPFSession());
					OSPFsessionserver.start();
				}
			} 
			catch (UnknownHostException e) 
			{
				log.info(UtilsFunctions.exceptionToString(e));
			}
		}
	}
	
	private void initFromWLANController()
	{
		log.info("Initializing TED from WLAN Controller");
		
		ArrayList<String> ips = new ArrayList<String>();
		ArrayList<String> ports = new ArrayList<String>();
		
		parseControllerFile(params.getControllerListFile(), ips, ports);
		
		TopologyUpdaterVLAN thread = new TopologyUpdaterVLAN(ips, ports, params.getTopologyPath(),"/wm/core/controller/switches/json", ted, log);
		thread.setInterDomainFile(params.getInterDomainFile());
		thread.run();		
	}
	
	private void parseControllerFile(String controllerFile, ArrayList<String> ips, ArrayList<String> ports)
	{		
		try 
		{
			DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			File confFile = new File(controllerFile);		
			Document doc = builder.parse(confFile);
			
			NodeList list_nodes_Edges = doc.getElementsByTagName("controller");
			
			for (int i = 0; i < list_nodes_Edges.getLength(); i++) 
			{
				Element nodes_servers = (Element) list_nodes_Edges.item(i);
				String ip = UtilsFunctions.getCharacterDataFromElement((Element) nodes_servers.getElementsByTagName("ip").item(0));
				String port = UtilsFunctions.getCharacterDataFromElement((Element) nodes_servers.getElementsByTagName("port").item(0));
				
				ips.add(ip);
				ports.add(port);
				
				log.info("Adding controller with IP: " + ip + " and port: "+port);
			}
		} 
		catch (Exception e) 
		{
			log.info(UtilsFunctions.exceptionToString(e));
		}	
	}
}

