package tid.topology.topologymodule.ospfRead;

import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

import es.tid.ospf.ospfv2.OSPFv2LinkStateUpdatePacket;
import es.tid.ospf.ospfv2.lsa.LSA;
import es.tid.ospf.ospfv2.lsa.LSATypes;
import es.tid.ospf.ospfv2.lsa.OSPFTEv2LSA;
import es.tid.ospf.ospfv2.lsa.tlv.LinkTLV;
import es.tid.ospf.ospfv2.lsa.tlv.subtlv.LocalInterfaceIPAddress;
import es.tid.ospf.ospfv2.lsa.tlv.subtlv.RemoteInterfaceIPAddress;
import es.tid.ospf.ospfv2.lsa.tlv.subtlv.SwitchingCapabilityType;
import tid.topology.elements.EndPoint;
import tid.topology.elements.Intf;
import tid.topology.elements.Link;
import tid.topology.elements.Location;
import tid.topology.elements.Node;
import tid.topology.elements.ipnode.IPNodeParams;
import tid.topology.topologymodule.TopologyUpdater;

/**
 * This class is a thread running continuously which process the OSPF messages obtained from sniffing the network
 *  
 * @author mcs
 *
 */
public class OneGprahUpdaterThread extends Thread{

	private LinkedBlockingQueue<OSPFv2LinkStateUpdatePacket> ospfv2PacketQueue;
	private Logger log;
	private TopologyUpdater topologyUpdater;

	public  OneGprahUpdaterThread(LinkedBlockingQueue<OSPFv2LinkStateUpdatePacket> ospfv2PacketQueue,TopologyUpdater topologyUpdater) {
		
		this.ospfv2PacketQueue=ospfv2PacketQueue;
	
		this.topologyUpdater=topologyUpdater;
	}


	public void run() {
		//log.info("Starting Topology Upadater Thread");
		System.out.println("Starting Topology Upadater Thread");
		LinkedList<LSA> lsaList;
		OSPFTEv2LSA lsa;
		OSPFv2LinkStateUpdatePacket ospfv2Packet;
		while(true){
			try {
				ospfv2Packet = ospfv2PacketQueue.take();
				//log.info("OSPF PACKET READ");
				System.out.println("-------------------");
				System.out.println("| OSPF PACKET READ |");
				System.out.println("-------------------");
				lsaList = ((OSPFv2LinkStateUpdatePacket)ospfv2Packet).getLSAlist();
				for (int i =0;i< lsaList.size();i++){					
					if (lsaList.get(i).getLStype() == LSATypes.TYPE_10_OPAQUE_LSA){
						
						lsa=(OSPFTEv2LSA)lsaList.get(i);
						System.out.println("Starting to process LSA OCAPA:"+lsa.toString());
					//	log.info("Starting to process LSA");
						LinkTLV linkTLV = lsa.getLinkTLV();
						System.out.println("LinkTLV");
						if (linkTLV!=null){
							//log.info("Link TLV ha llegado "+lsa.toString());
							//System.out.println(linkTLV.toString());
							if (linkTLV.getInterfaceSwitchingCapabilityDescriptor().getSwitchingCap()==SwitchingCapabilityType.LAMBDA_SWITCH_CAPABLE){
								System.out.println("SwitchingCapabilityDescriptor");
								//nodes
								Inet4Address routerId= ospfv2Packet.getRouterID();
								Inet4Address srcIp= lsa.getAdvertisingRouter();	
								Inet4Address dstIp = linkTLV.getLinkID().getLinkID();
								Inet4Address localInterfaceIPAddress = null;
								Inet4Address remoteInterfaceIPAddress=null;
								long linkLocalIdentifiers;
								long linkRemoteIdentifiers;								
								//Interfaces
								if (linkTLV.getLocalInterfaceIPAddress() != null){
//									 localInterfaceIPAddress=linkTLV.getLocalInterfaceIPAddress().getLocalInterfaceIPAddress(0);										
//									 remoteInterfaceIPAddress = linkTLV.getRemoteInterfaceIPAddress().getRemoteInterfaceIPAddress(0);
//										System.out.println("Local InterfaceIPAddress: "+localInterfaceIPAddress);
//										System.out.println("Remote InterfaceIPAddress: "+remoteInterfaceIPAddress);
//										
										
										
								}
								else if (linkTLV.getLinkLocalRemoteIdentifiers() != null){
									
									linkLocalIdentifiers = linkTLV.getLinkLocalRemoteIdentifiers().getLinkLocalIdentifier();
									linkRemoteIdentifiers = linkTLV.getLinkLocalRemoteIdentifiers().getLinkRemoteIdentifier();
									System.out.println("Local linkLocalIdentifiers: "+linkLocalIdentifiers);
									System.out.println("Remote linkRemoteIdentifiers: "+linkRemoteIdentifiers);
									
									
									byte[] ipadd=new byte[4]; 
									ipadd[0]=(byte)(linkLocalIdentifiers >>> 24);
									ipadd[1]=(byte)(linkLocalIdentifiers >>> 16 & 0xff);
									ipadd[2]=(byte)(linkLocalIdentifiers >>> 8 & 0xff);
									ipadd[3]=(byte)(linkLocalIdentifiers & 0xff);	
									try {
										localInterfaceIPAddress=(Inet4Address)Inet4Address.getByAddress(ipadd);
									} catch (UnknownHostException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}		
									byte[] ipaddRemote=new byte[4]; 
									ipaddRemote[0]=(byte)(linkRemoteIdentifiers >>> 24);
									ipaddRemote[1]=(byte)(linkRemoteIdentifiers >>> 16 & 0xff);
									ipaddRemote[2]=(byte)(linkRemoteIdentifiers >>> 8 & 0xff);
									ipaddRemote[3]=(byte)(linkRemoteIdentifiers & 0xff);
									try {
										remoteInterfaceIPAddress=(Inet4Address)Inet4Address.getByAddress(ipaddRemote);
									} catch (UnknownHostException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}		
									System.out.println("Local InterfaceIPAddress: "+localInterfaceIPAddress);
									System.out.println("Remote InterfaceIPAddress: "+localInterfaceIPAddress);
									
								}//log.finest("Local InterfaceIPAddress: "+localInterfaceIPAddress);							
								//log.finest("Remote InterfaceIPAddress: "+remoteInterfaceIPAddress);
								if ((localInterfaceIPAddress!=null)&&(remoteInterfaceIPAddress!=null)){
									//Comprobar si los nodos existen en el grafo
									System.out.println("---> TRANSPORT: ("+srcIp.toString()+"-"+dstIp.toString()+")");
									update(srcIp,dstIp,"transport",localInterfaceIPAddress,remoteInterfaceIPAddress);
								}
								
								else{//Interlayer
									System.out.println("LAMBDAAAAAAAAA pero es interlayer");
									//nodes
									srcIp = lsa.getAdvertisingRouter();									
									System.out.println("src: "+ srcIp + " dst: "+dstIp);
									LocalInterfaceIPAddress localIntIPAddress = linkTLV.getLocalInterfaceIPAddress();
									RemoteInterfaceIPAddress remoteIntIPAddress = linkTLV.getRemoteInterfaceIPAddress();
									if ((localIntIPAddress != null)&&(remoteIntIPAddress != null)){
										localInterfaceIPAddress=localIntIPAddress.getLocalInterfaceIPAddress(0);
										remoteInterfaceIPAddress=remoteIntIPAddress.getRemoteInterfaceIPAddress(0);
										System.out.println("IntfSrc: "+ localIntIPAddress.getLocalInterfaceIPAddress(0) + " IntfDst: "+remoteIntIPAddress.getRemoteInterfaceIPAddress(0));
									}
									
									update(srcIp,dstIp,"interlayer",localInterfaceIPAddress,remoteInterfaceIPAddress);
								}
							}
							else
								System.out.println("NO LAMBDAAAAAAAAA!!!");
						
						
						}else {
							System.out.println("Link TLV de OSPFTEv2LSA esta a null");
							//log.finest("Link TLV de OSPFTEv2LSA esta a null");
						}
						//log.finest("Processing LSA to update topology");
					}
				}


			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}


		}

	}
	
	private void update(Inet4Address srcIp, Inet4Address dstIp, String layer,Inet4Address localInterfaceIPAddress,Inet4Address remoteInterfaceIPAddress){
		Link link=null;
		Node nodeSrc = null;
		Node nodeDst = null;
		if (topologyUpdater.isNodeInGraph(srcIp.toString())){
			//Source y destino en el grafo
			if (topologyUpdater.isNodeInGraph(dstIp.toString())){
				//Ver si las interfaces estan
				System.out.println("Source ("+srcIp.toString()+")y destino("+dstIp.toString()+") en el grafo");
				if ((topologyUpdater.getPositionIntfInNode(localInterfaceIPAddress.toString(),srcIp.toString())) != -1){
					System.out.println("Creamos intf:"+ localInterfaceIPAddress);
					ArrayList<String> addressSrcIntf =  new ArrayList<String>();
					addressSrcIntf.add(localInterfaceIPAddress.toString());
					ArrayList <String> layeringSrc =  new ArrayList<String>();
					layeringSrc.add(getLayer(layer,srcIp));
					
					Intf intfSrc = createIntf(localInterfaceIPAddress.toString(),addressSrcIntf,layeringSrc,null,true,null);
					topologyUpdater.updateIntf(srcIp.toString(), intfSrc);
				}
				if ((topologyUpdater.getPositionIntfInNode(remoteInterfaceIPAddress.toString(),dstIp.toString()))!= -1){
					System.out.println("Creamos intf:"+ remoteInterfaceIPAddress);
					ArrayList<String> addressDstIntf =  new ArrayList<String>();
					addressDstIntf.add(remoteInterfaceIPAddress.toString());
					ArrayList <String> layeringDst =  new ArrayList<String>();
					layeringDst.add(getLayer(layer,dstIp));
					Intf intfDst = createIntf(remoteInterfaceIPAddress.toString(),addressDstIntf,layeringDst,null,true,null);
					topologyUpdater.updateIntf(dstIp.toString(), intfDst);
				}
				
				link = createLink(srcIp.toString(),dstIp.toString(),localInterfaceIPAddress.toString(),remoteInterfaceIPAddress.toString(),"Intradomain");
				topologyUpdater.updateLink(layer, link);
			}else{//Source si, destino no	
				System.out.println("Source ("+srcIp.toString()+") si, destino ("+dstIp.toString()+") no");
				ArrayList<String> addressDst = new ArrayList<String>();
				addressDst.add(dstIp.toString());
				ArrayList<String> addressDstIntf =  new ArrayList<String>();
				addressDstIntf.add(remoteInterfaceIPAddress.toString());
				ArrayList <String> layeringDst =  new ArrayList<String>();
				layeringDst.add(getLayer(layer,dstIp));
				ArrayList<Intf> intfDstList = new ArrayList<Intf> ();
				Intf intfDst = createIntf(remoteInterfaceIPAddress.toString(),addressDstIntf,layeringDst,null,true,null);
				intfDstList.add(intfDst);
				int domain=1;
				
				nodeDst = createNode(dstIp.toString(),addressDst,true,intfDstList,domain,null,null,null,getLayer(layer,dstIp));
				//Meto la nueva interfaz
				if ((topologyUpdater.getPositionIntfInNode(localInterfaceIPAddress.toString(),srcIp.toString())) != -1){
					ArrayList<String> addressSrcIntf =  new ArrayList<String>();
					addressSrcIntf.add(localInterfaceIPAddress.toString());
					ArrayList <String> layeringSrc =  new ArrayList<String>();
					layeringSrc.add(getLayer(layer,srcIp));
					Intf intfSrc = createIntf(localInterfaceIPAddress.toString(),addressSrcIntf,layeringSrc,null,true,null);
					topologyUpdater.updateIntf(srcIp.toString(), intfSrc);
				}			
				link = createLink(srcIp.toString(),dstIp.toString(),localInterfaceIPAddress.toString(),remoteInterfaceIPAddress.toString(),"Intradomain");
				//Existe source
				topologyUpdater.updateNode(nodeDst);
				topologyUpdater.updateLink(layer, link);
			}
				
		}
		else if (topologyUpdater.isNodeInGraph(dstIp.toString())){//Destiny si, source no
			System.out.println("Destiny ("+dstIp.toString()+") si, source ("+srcIp.toString()+") no");
			//Creo nodo source
			ArrayList<String> addressSrc = new ArrayList<String>();
			addressSrc.add(srcIp.toString());
			ArrayList<String> addressSrcIntf =  new ArrayList<String>();
			addressSrcIntf.add(localInterfaceIPAddress.toString());
			ArrayList <String> layeringSrc =  new ArrayList<String>();
			layeringSrc.add(getLayer(layer,srcIp));
			ArrayList<Intf> intfSrcList = new ArrayList<Intf> ();
			Intf intfSrc = createIntf(localInterfaceIPAddress.toString(),addressSrcIntf,layeringSrc,null,true,null);
			intfSrcList.add(intfSrc);
			int domain=1;
			nodeSrc = createNode(srcIp.toString(),addressSrc,true,intfSrcList,domain,null,null,null,getLayer(layer,srcIp));
			//Meto la interfaz en el destino
			if ((topologyUpdater.getPositionIntfInNode(remoteInterfaceIPAddress.toString(),dstIp.toString())) != -1){
				ArrayList<String> addressDstIntf =  new ArrayList<String>();
				addressDstIntf.add(remoteInterfaceIPAddress.toString());
				ArrayList <String> layeringDst =  new ArrayList<String>();
				layeringDst.add(getLayer(layer,dstIp));
				Intf intfDst = createIntf(remoteInterfaceIPAddress.toString(),addressDstIntf,layeringDst,null,true,null);
				topologyUpdater.updateIntf(dstIp.toString(), intfDst);
			}
			
			link = createLink(srcIp.toString(),dstIp.toString(),localInterfaceIPAddress.toString(),remoteInterfaceIPAddress.toString(),"Intradomain");
			topologyUpdater.updateNode(nodeSrc);									
			topologyUpdater.updateLink(layer, link);
		}else{//ningun nodo en el grafo
		
			System.out.println("source ("+srcIp.toString()+") no Destiny ("+dstIp.toString()+") no");
		
		//Creo nodo source
		ArrayList<String> addressSrc = new ArrayList<String>();
		addressSrc.add(srcIp.toString());
		ArrayList<String> addressSrcIntf =  new ArrayList<String>();
		addressSrcIntf.add(localInterfaceIPAddress.toString());
		ArrayList <String> layeringSrc =  new ArrayList<String>();
		layeringSrc.add(getLayer(layer,srcIp));
		ArrayList<Intf> intfSrcList = new ArrayList<Intf> ();
		Intf intfSrc = createIntf(localInterfaceIPAddress.toString(),addressSrcIntf,layeringSrc,null,true,null);
		intfSrcList.add(intfSrc);
		int domain=1;
		nodeSrc = createNode(srcIp.toString(),addressSrc,true,intfSrcList,domain,null,null,null,getLayer(layer,srcIp));

		//Creo nodo destino										
		ArrayList<String> addressDst = new ArrayList<String>();
		addressDst.add(dstIp.toString());
		ArrayList<String> addressDstIntf =  new ArrayList<String>();
		addressDstIntf.add(remoteInterfaceIPAddress.toString());
		ArrayList <String> layeringDst =  new ArrayList<String>();
		layeringDst.add(getLayer(layer,dstIp));
		ArrayList<Intf> intfDstList = new ArrayList<Intf> ();
		Intf intfDst = createIntf(remoteInterfaceIPAddress.toString(),addressDstIntf,layeringDst,null,true,null);
		intfDstList.add(intfDst);
		
		nodeDst = createNode(dstIp.toString(),addressDst,true,intfDstList,domain,null,null,null,getLayer(layer,dstIp));
		link = createLink(srcIp.toString(),dstIp.toString(),localInterfaceIPAddress.toString(),remoteInterfaceIPAddress.toString(),"Intradomain");

			topologyUpdater.updateNode(nodeSrc);
			//Existe source
			topologyUpdater.updateNode(nodeDst);
			topologyUpdater.updateLink(layer, link);
		}
	}
	

	private String getLayer(String layer, Inet4Address ip){
		if (layer.equals("interlayer")){
			//Hay que ver si el src es de capa IP o capa transport
			Node node = topologyUpdater.getOneGraph().getNode(ip.toString());
			if (node != null)
				return node.getLayer();
			else
				return "SinLayer";
		}
		else {						
			return layer;
		}
	}
	private Node createNode(String nodeID,ArrayList<String> address,boolean isPhysical,ArrayList<Intf> intfList,int domain,Location location,IPNodeParams ipParams,String parentRouter,String layer){
		
		Node node = new Node();
		node.setNodeID(nodeID);
		node.setAddress(address);
		node.setPhysical(isPhysical);
		node.setIntfList(intfList);
		node.setDomain(domain);
		node.setIpParams(ipParams);
		node.setLayer(layer);
		node.setLocation(location);
		node.setParentRouter(parentRouter);
		return node;
	}
	
	private Link createLink(String srcName,String dstName,String localInterface, String  remoteInterface,String type){
		//Creo link
		Link link = new Link();	
		System.out.println("Source: "+srcName+"  Local InterfaceIPAddress: "+localInterface);
		System.out.println("Destiny: "+dstName+ "  Remote InterfaceIPAddress: "+remoteInterface);

		EndPoint src = new EndPoint(srcName,localInterface);
		EndPoint dst = new EndPoint(dstName,remoteInterface);
		link.setSource(src);
		link.setDest(dst);
		link.setType(type);
		return link;
	}
	private Intf createIntf(String name,ArrayList<String> address,ArrayList <String> layering,ArrayList<String> supportedCounters,boolean isPhysical,String parentInterfaceName){
		Intf intf= new Intf();
		intf.setName(name);
		intf.setAddress(address);
		intf.setLayering(layering);
		intf.setSupportedCounters(supportedCounters);
		intf.setParentInterfaceName(parentInterfaceName);
		intf.setPhysical(isPhysical);
		return intf;
	}
	public int getNumberBytes(int lambda){
		int numberBytes = lambda/8;
		if ((numberBytes*8)<lambda){
			numberBytes++;
		}
		return numberBytes;
	}



}
