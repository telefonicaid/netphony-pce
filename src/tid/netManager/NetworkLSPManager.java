package tid.netManager;

import java.net.Inet4Address;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

import org.jgrapht.graph.SimpleDirectedWeightedGraph;

import es.tid.ospf.ospfv2.OSPFv2LinkStateUpdatePacket;
import es.tid.ospf.ospfv2.lsa.LSA;
import es.tid.ospf.ospfv2.lsa.OSPFTEv2LSA;
import es.tid.ospf.ospfv2.lsa.tlv.LinkTLV;
import es.tid.ospf.ospfv2.lsa.tlv.subtlv.LinkID;
import es.tid.ospf.ospfv2.lsa.tlv.subtlv.LocalInterfaceIPAddress;
import es.tid.ospf.ospfv2.lsa.tlv.subtlv.RemoteInterfaceIPAddress;
import es.tid.ospf.ospfv2.lsa.tlv.subtlv.complexFields.BitmapLabelSet;
import es.tid.pce.pcep.objects.BandwidthRequestedGeneralizedBandwidth;
import es.tid.rsvp.constructs.gmpls.DWDMWavelengthLabel;
import es.tid.rsvp.objects.subobjects.EROSubobject;
import tid.netManager.emulated.LayerTypes;
import tid.pce.tedb.DomainTEDB;
import tid.pce.tedb.IntraDomainEdge;
import tid.pce.tedb.MultiLayerTEDB;
import tid.pce.tedb.SimpleTEDB;

public abstract class NetworkLSPManager {
	Logger log= Logger.getLogger("PCCClient");
	private DomainTEDB domainTEDB;
	private LinkedBlockingQueue<OSPFv2LinkStateUpdatePacket> sendingQueue;
//	DataOutputStream out;
	private String file;
	private int emulatorType;
	private boolean multilayer=false;	

	public DomainTEDB getDomainTEDB() {
		return domainTEDB;
	}

	public void setDomainTEDB(DomainTEDB domainTEDB) {
		this.domainTEDB = domainTEDB;
	}
	
	
	/**
	 * Recorrer toda la red y enviar todos los OSPF
	 * @return
	 */
	public void sendAllTopology(){
		int layer;
		if (multilayer){
			//UPPER_LAYER
			SimpleDirectedWeightedGraph<Object,IntraDomainEdge> graphIP = ((MultiLayerTEDB)domainTEDB).getUpperLayerGraph();
			
			Set<IntraDomainEdge> edgeSet= graphIP.edgeSet();
			Iterator <IntraDomainEdge> edgeIterator=edgeSet.iterator();
			layer = LayerTypes.UPPER_LAYER;
			while (edgeIterator.hasNext()){
				IntraDomainEdge edge= edgeIterator.next();
				sendMessageOSPF((Inet4Address)edge.getSource(),(Inet4Address)edge.getTarget(), multilayer, layer, null);
			}
			//LOWER LAYER
			SimpleDirectedWeightedGraph<Object,IntraDomainEdge> graphOP = ((MultiLayerTEDB)domainTEDB).getLowerLayerGraph();
			
			Set<IntraDomainEdge> edgeSet1= graphOP.edgeSet();
			Iterator <IntraDomainEdge> edgeIterator1=edgeSet1.iterator();
			layer = LayerTypes.LOWER_LAYER;
			while (edgeIterator1.hasNext()){
				IntraDomainEdge edge= edgeIterator1.next();
				sendMessageOSPF((Inet4Address)edge.getSource(),(Inet4Address)edge.getTarget(), multilayer, layer, null);
			}
			
		}
		else {
			SimpleDirectedWeightedGraph<Object,IntraDomainEdge> networkGraph = ((SimpleTEDB)domainTEDB).getNetworkGraph();
	
			Set<IntraDomainEdge> edgeSet= networkGraph.edgeSet();
			Iterator <IntraDomainEdge> edgeIterator=edgeSet.iterator();
			layer = LayerTypes.SIMPLE_NETWORK;
			
			while (edgeIterator.hasNext()){
				IntraDomainEdge edge= edgeIterator.next();
				sendMessageOSPF((Inet4Address)edge.getSource(),(Inet4Address)edge.getTarget(), multilayer, layer, null);
			}	
		}
	}
	/**
	 * Send a OSPF message 
	 * @param TE_info Information of the link to send
	 * @param src Source address of the link
	 * @param dst Destiny address of the link 
	 * @param dwdmWavelengthLabel lambda used in the link (where to send), it can be null.
	 */
	
	public void sendMessageOSPF(Inet4Address src,Inet4Address dst){
		sendMessageOSPF(src, dst, multilayer, LayerTypes.SIMPLE_NETWORK, null);
	}
	
	/**
	 * Send a OSPF message 
	 * @param TE_info Information of the link to send
	 * @param src Source address of the link
	 * @param dst Destiny address of the link 
	 * @param dwdmWavelengthLabel lambda used in the link (where to send), it can be null.
	 */
	
	public void sendMessageOSPF(Inet4Address src,Inet4Address dst, boolean isMultilayer, int layer, DWDMWavelengthLabel dwdmWavelengthLabel){
		
		log.info("SEND OSPF");
		
		//changes for multilayer OSPF (UpperLayer and LowerLayer)
		IntraDomainEdge edge = null;
		
		if (isMultilayer){
			if (layer == LayerTypes.UPPER_LAYER){
				edge=((MultiLayerTEDB)domainTEDB).getUpperLayerGraph().getEdge(src, dst);
			}
			else if (layer == LayerTypes.LOWER_LAYER){
				edge=((MultiLayerTEDB)domainTEDB).getLowerLayerGraph().getEdge(src, dst);
			}
		}		
		else
			edge=((SimpleTEDB)domainTEDB).getNetworkGraph().getEdge(src, dst);
		
		OSPFv2LinkStateUpdatePacket ospfv2Packet = new OSPFv2LinkStateUpdatePacket();
		ospfv2Packet.setRouterID(src);
		LinkedList<LSA> lsaList = new LinkedList<LSA>();
		OSPFTEv2LSA lsa = new OSPFTEv2LSA();
		LinkTLV linkTLV=new LinkTLV();
		lsa.setLinkTLV(linkTLV);
		
		if (layer == LayerTypes.UPPER_LAYER){
			if (edge.getTE_info().getMaximumBandwidth()!=null)
				linkTLV.setMaximumBandwidth(edge.getTE_info().getMaximumBandwidth());
			if (edge.getTE_info().getUnreservedBandwidth() != null)
				linkTLV.setUnreservedBandwidth(edge.getTE_info().getUnreservedBandwidth());
			if (edge.getTE_info().getMaximumReservableBandwidth()!=null){	
				linkTLV.setMaximumReservableBandwidth(edge.getTE_info().getMaximumReservableBandwidth());
			}
		}
		
		LocalInterfaceIPAddress localInterfaceIPAddress= new LocalInterfaceIPAddress();
		LinkedList<Inet4Address> lista =localInterfaceIPAddress.getLocalInterfaceIPAddressList();
		lista.add(src);
		linkTLV.setLocalInterfaceIPAddress(localInterfaceIPAddress);
		RemoteInterfaceIPAddress remoteInterfaceIPAddress= new RemoteInterfaceIPAddress();
		LinkedList<Inet4Address> listar = remoteInterfaceIPAddress.getRemoteInterfaceIPAddressList();
		listar.add(dst);
		linkTLV.setRemoteInterfaceIPAddress(remoteInterfaceIPAddress);
		LinkID linkID = new LinkID();
		linkID.setLinkID(dst);
		linkTLV.setLinkID(linkID);
		if (edge.getTE_info().getAvailableLabels() != null){
			linkTLV.setAvailableLabels(edge.getTE_info().getAvailableLabels());	
			((BitmapLabelSet)linkTLV.getAvailableLabels().getLabelSet()).setDwdmWavelengthLabel(dwdmWavelengthLabel);
		}
		
		lsaList.add(lsa);
		ospfv2Packet.setLSAlist(lsaList);
		sendingQueue.add(ospfv2Packet);
	}
	
	public abstract boolean setLSP(LinkedList<EROSubobject> erolist,boolean bidirect, BandwidthRequestedGeneralizedBandwidth GB);
	public abstract boolean setMLLSP(LinkedList<EROSubobject> erolist,boolean bidirect, BandwidthRequestedGeneralizedBandwidth GB);
	public abstract boolean setLSP_UpperLayer(LinkedList<EROSubobject> eROSubobjectList_IP, float bw, boolean bidirect);
	public abstract void removeLSP(LinkedList<EROSubobject> erolist, boolean bidirect, BandwidthRequestedGeneralizedBandwidth GB);
	public abstract void removeLSP(LinkedList<EROSubobject> erolist, boolean bidirect, BandwidthRequestedGeneralizedBandwidth GB, float bw);
	public abstract void removeMLLSP(LinkedList<EROSubobject> erolist, boolean bidirect, BandwidthRequestedGeneralizedBandwidth GB);

	public LinkedBlockingQueue<OSPFv2LinkStateUpdatePacket> getSendingQueue() {
		return sendingQueue;
	}

	public void setSendingQueue(
			LinkedBlockingQueue<OSPFv2LinkStateUpdatePacket> sendingQueue) {
		this.sendingQueue = sendingQueue;
	}

	public String getFile() {
		return file;
	}

	public void setFile(String file) {
		this.file = file;
	}

	public void setEmulatorType(int emulatorType) {
		this.emulatorType = emulatorType;		
	}
	public int getEmulatorType() {
		return emulatorType;
	}

	public boolean isMultilayer() {
		return multilayer;
	}

	public void setMultilayer(boolean multilayer) {
		this.multilayer = multilayer;
	}

}
