package tid.netManager.emulated;


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
import es.tid.ospf.ospfv2.lsa.tlv.subtlv.LocalInterfaceIPAddress;
import es.tid.ospf.ospfv2.lsa.tlv.subtlv.RemoteInterfaceIPAddress;
import es.tid.ospf.ospfv2.lsa.tlv.subtlv.complexFields.BitmapLabelSet;
import es.tid.pce.pcep.objects.BandwidthRequestedGeneralizedBandwidth;
import es.tid.rsvp.constructs.gmpls.DWDMWavelengthLabel;
import es.tid.rsvp.objects.subobjects.EROSubobject;
import es.tid.rsvp.objects.subobjects.GeneralizedLabelEROSubobject;
import es.tid.rsvp.objects.subobjects.IPv4prefixEROSubobject;
import es.tid.rsvp.objects.subobjects.SubObjectValues;
import es.tid.rsvp.objects.subobjects.UnnumberIfIDEROSubobject;
import tid.netManager.NetworkLSPManager;
import tid.netManager.NetworkLSPManagerTypes;
import tid.pce.tedb.IntraDomainEdge;
import tid.pce.tedb.SimpleTEDB;
import tid.pce.tedb.TE_Information;


/**
 * 
 * @author Oscar Gonzalez de Dios
 * @author Marta Cuaresma Saturio
 * @author Fernando Mu�oz del Nuevo
 *
 */

public class SimpleEmulatedNetworkLSPManager extends NetworkLSPManager{
	Logger log= Logger.getLogger("PCCClient");
	public SimpleEmulatedNetworkLSPManager(LinkedBlockingQueue<OSPFv2LinkStateUpdatePacket> sendingQueue, String file ){
		this.setEmulatorType(NetworkLSPManagerTypes.SIMPLE_EMULATED_NETWORK);
		this.setDomainTEDB(new SimpleTEDB());
		this.setFile(file);
		if (file !=null){
			log.info("Initializing TEDB de "+file);
			this.getDomainTEDB().initializeFromFile(file);
		}
		else {
			log.severe("Network file NOT included!!!");
		}

		this.setSendingQueue(sendingQueue);

	}

	/**
	 * Recorrer toda la red y enviar todos los OSPF
	 * @return
	 */
	@Override
	public void sendAllTopology(){
		SimpleDirectedWeightedGraph<Object,IntraDomainEdge> networkGraph = ((SimpleTEDB)this.getDomainTEDB()).getNetworkGraph();

		Set<IntraDomainEdge> edgeSet= networkGraph.edgeSet();
		Iterator <IntraDomainEdge> edgeIterator=edgeSet.iterator();
		LinkedList<EROSubobject> erolist= new LinkedList<EROSubobject>();
		UnnumberIfIDEROSubobject unnumberIfScr = new UnnumberIfIDEROSubobject();
		IPv4prefixEROSubobject ipv4Dst = new IPv4prefixEROSubobject();
//		DWDMWavelengthLabel dwdmWavelengthLabel = new DWDMWavelengthLabel();
		while (edgeIterator.hasNext()){
			IntraDomainEdge edge= edgeIterator.next();		
			//Source
			unnumberIfScr.setRouterID((Inet4Address)edge.getSource());
			unnumberIfScr.setInterfaceID(edge.getSrc_if_id());
			erolist.add(unnumberIfScr);
			//Lambda
			
			//Destiny
			ipv4Dst.setIpv4address((Inet4Address)edge.getTarget());
			erolist.add(ipv4Dst);
			setLSP(erolist,false, null);
		
		}		
	}

	/**
	 * REcorre el ERO y actualiza las propiedades de TE
	 * setLSP send the LSP for multiDomain Networks.
	 * En la Erolist me viene: 
	 *  - interfaces no numeradas
	 *  - IPv4Address
	 *  -  lambdas: (Objeto DWDMWavelengthLabel)
	 *     0                   1                   2                   3
    		0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
   			+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   			|Grid | C.S.  |    Identifier   |              n                |
   			+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 * @param erolist
	 */
	@Override
	public boolean setLSP(LinkedList<EROSubobject> erolist,boolean bidirect, BandwidthRequestedGeneralizedBandwidth GB){
		log.info("Setting LSP with ERO: "+erolist.toString());
		
		for (int i=0;i<erolist.size()-1;++i){
			log.info("Tamanno del ERO: "+erolist.size()+"i:"+i);
			Inet4Address src=null;
			Inet4Address dst=null;			
			DWDMWavelengthLabel dwdmWavelengthLabel=new DWDMWavelengthLabel();
			
			if (erolist.get(i).getType()==SubObjectValues.ERO_SUBOBJECT_IPV4PREFIX){
				src=((IPv4prefixEROSubobject)erolist.get(i)).getIpv4address();
			}else if (erolist.get(i).getType()==SubObjectValues.ERO_SUBOBJECT_UNNUMBERED_IF_ID){
				src=((UnnumberIfIDEROSubobject)erolist.get(i)).getRouterID();
			}
			if (erolist.get(i+1).getType()==SubObjectValues.ERO_SUBOBJECT_IPV4PREFIX){
				dst=((IPv4prefixEROSubobject)erolist.get(i+1)).getIpv4address();
			}else if (erolist.get(i+1).getType()==SubObjectValues.ERO_SUBOBJECT_UNNUMBERED_IF_ID){
				dst=((UnnumberIfIDEROSubobject)erolist.get(i+1)).getRouterID();
			}else if (erolist.get(i+1).getType()==SubObjectValues.ERO_SUBOBJECT_LABEL){
				dwdmWavelengthLabel = ((GeneralizedLabelEROSubobject) erolist.get(i+1)).getDwdmWavelengthLabel();
				if (erolist.get(i+2).getType()==SubObjectValues.ERO_SUBOBJECT_IPV4PREFIX){
					dst=((IPv4prefixEROSubobject)erolist.get(i+2)).getIpv4address();
				}else if (erolist.get(i+2).getType()==SubObjectValues.ERO_SUBOBJECT_UNNUMBERED_IF_ID){
					dst=((UnnumberIfIDEROSubobject)erolist.get(i+2)).getRouterID();
				}
			}
			IntraDomainEdge edge=((SimpleTEDB)this.getDomainTEDB()).getNetworkGraph().getEdge(src, dst);
			if (edge != null){
				TE_Information TE_info=edge.getTE_info();				
				(TE_info.getUnreservedBandwidth().getUnreservedBandwidth())[0]=(TE_info.getUnreservedBandwidth().getUnreservedBandwidth())[0]-10;
				int lambda = 0;	
				if (dwdmWavelengthLabel!= null){//hay lambda
					lambda = ((SimpleTEDB)this.getDomainTEDB()).getWSONinfo().getnMin()+ dwdmWavelengthLabel.getN();
					log.info("offset N -----> "+ ((SimpleTEDB)this.getDomainTEDB()).getWSONinfo().getnMin());
					log.info("HAY LAMBDA -----> "+ lambda);

			
					(((BitmapLabelSet) TE_info.getAvailableLabels().getLabelSet()).getBytesBitMap())[lambda]=1;
					
				}
				sendMessageOSPF(src,dst);
				//Y ahora el contrario
				IntraDomainEdge edge_op=((SimpleTEDB)this.getDomainTEDB()).getNetworkGraph().getEdge(dst, src);
				TE_Information TE_info_op=edge_op.getTE_info();
				(TE_info_op.getUnreservedBandwidth().getUnreservedBandwidth())[0]=(TE_info_op.getUnreservedBandwidth().getUnreservedBandwidth())[0]-10;
				sendMessageOSPF(dst,src);

			}
			else {
				log.info("Error en setMLLSP. Edge null");
			}		

			}
		return true;
	}
/**
 * For MultiLayer Networks
 * @param erolist
 */
	@Override
	public boolean setMLLSP(LinkedList<EROSubobject> erolist,boolean bidirect, BandwidthRequestedGeneralizedBandwidth GB){
		log.info("Setting ML LSP with ERO: "+erolist.toString());
		
		for (int i=1;i<erolist.size()-2;++i){
			Inet4Address src=null;
			Inet4Address dst=null;
			DWDMWavelengthLabel dwdmWavelengthLabel=null;
			if (erolist.get(i).getType()==SubObjectValues.ERO_SUBOBJECT_IPV4PREFIX){
				src=((IPv4prefixEROSubobject)erolist.get(i)).getIpv4address();
			}else if (erolist.get(i).getType()==SubObjectValues.ERO_SUBOBJECT_UNNUMBERED_IF_ID){
				src=((UnnumberIfIDEROSubobject)erolist.get(i)).getRouterID();
			}
			if (erolist.get(i+1).getType()==SubObjectValues.ERO_SUBOBJECT_IPV4PREFIX){
				dst=((IPv4prefixEROSubobject)erolist.get(i+1)).getIpv4address();
			}else if (erolist.get(i+1).getType()==SubObjectValues.ERO_SUBOBJECT_UNNUMBERED_IF_ID){
				dst=((UnnumberIfIDEROSubobject)erolist.get(i+1)).getRouterID();
			}else if (erolist.get(i+1).getType()==SubObjectValues.ERO_SUBOBJECT_LABEL){
				dwdmWavelengthLabel = ((GeneralizedLabelEROSubobject) erolist.get(i+1)).getDwdmWavelengthLabel();
				if (erolist.get(i+2).getType()==SubObjectValues.ERO_SUBOBJECT_IPV4PREFIX){
					dst=((IPv4prefixEROSubobject)erolist.get(i+2)).getIpv4address();
				}else if (erolist.get(i+2).getType()==SubObjectValues.ERO_SUBOBJECT_UNNUMBERED_IF_ID){
					dst=((UnnumberIfIDEROSubobject)erolist.get(i+2)).getRouterID();
				}
			}
			IntraDomainEdge edge=((SimpleTEDB)this.getDomainTEDB()).getNetworkGraph().getEdge(src, dst);
			if (edge != null){
				TE_Information TE_info=edge.getTE_info();				
				(TE_info.getUnreservedBandwidth().getUnreservedBandwidth())[0]=(TE_info.getUnreservedBandwidth().getUnreservedBandwidth())[0]-10;
				int lambda = 0;	
				if (dwdmWavelengthLabel!= null){//hay lambda
					lambda = ((SimpleTEDB)this.getDomainTEDB()).getWSONinfo().getnMin()+ dwdmWavelengthLabel.getN();
					log.info("Hay LAMBDA: "+lambda);
					
					(((BitmapLabelSet) TE_info.getAvailableLabels().getLabelSet()).getBytesBitMap())[lambda]=1;
					
				}

				sendMessageOSPF(src,dst);
				//Y ahora el contrario
				IntraDomainEdge edge_op=((SimpleTEDB)this.getDomainTEDB()).getNetworkGraph().getEdge(dst, src);
				
				(edge_op.getTE_info().getUnreservedBandwidth().getUnreservedBandwidth())[0]=(edge_op.getTE_info().getUnreservedBandwidth().getUnreservedBandwidth())[0]-10;
				sendMessageOSPF(dst,src);
		
//				lsa.encode();
//				try {
//					log.info("LSA ready ... "+lsa.toString());
//					out.write(lsa.getLSAbytes());
//				} catch (IOException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
			}
			else {
				log.info("Error en setMLLSP. Edge null");
			}		

		}
		return true;
	}
	
	/**
	 * For MultiDomain Networks
	 * @param erolist
	 */
	@Override
	public void removeLSP(LinkedList<EROSubobject> erolist,boolean bidirect, BandwidthRequestedGeneralizedBandwidth GB){
		log.info("Setting ML LSP with ERO: "+erolist.toString());
		
		for (int i=0;i<erolist.size()-1;++i){
			Inet4Address src=null;
			Inet4Address dst=null;
			if (erolist.get(i).getType()==SubObjectValues.ERO_SUBOBJECT_IPV4PREFIX){
				src=((IPv4prefixEROSubobject)erolist.get(i)).getIpv4address();
			}else if (erolist.get(i).getType()==SubObjectValues.ERO_SUBOBJECT_UNNUMBERED_IF_ID){
				src=((UnnumberIfIDEROSubobject)erolist.get(i)).getRouterID();
			}
			if (erolist.get(i+1).getType()==SubObjectValues.ERO_SUBOBJECT_IPV4PREFIX){
				dst=((IPv4prefixEROSubobject)erolist.get(i+1)).getIpv4address();
			}else if (erolist.get(i+1).getType()==SubObjectValues.ERO_SUBOBJECT_UNNUMBERED_IF_ID){
				dst=((UnnumberIfIDEROSubobject)erolist.get(i+1)).getRouterID();
			}
			IntraDomainEdge edge=((SimpleTEDB)this.getDomainTEDB()).getNetworkGraph().getEdge(src, dst);
			if (edge != null){
				TE_Information TE_info=edge.getTE_info();
				OSPFv2LinkStateUpdatePacket ospfv2Packet = new OSPFv2LinkStateUpdatePacket();
				ospfv2Packet.setRouterID(src);
				LinkedList<LSA> lsaList = new LinkedList<LSA>();
				OSPFTEv2LSA lsa = new OSPFTEv2LSA();
				LinkTLV linkTLV=new LinkTLV();
				lsa.setLinkTLV(linkTLV);
				linkTLV.setMaximumBandwidth(TE_info.getMaximumBandwidth());
				linkTLV.setUnreservedBandwidth(TE_info.getUnreservedBandwidth());

				LocalInterfaceIPAddress localInterfaceIPAddress= new LocalInterfaceIPAddress();
				LinkedList<Inet4Address> lista =localInterfaceIPAddress.getLocalInterfaceIPAddressList();
				lista.add(src);
				linkTLV.setLocalInterfaceIPAddress(localInterfaceIPAddress);
				RemoteInterfaceIPAddress remoteInterfaceIPAddress= new RemoteInterfaceIPAddress();
				LinkedList<Inet4Address> listar = remoteInterfaceIPAddress.getRemoteInterfaceIPAddressList();
				listar.add(dst);
				linkTLV.setRemoteInterfaceIPAddress(remoteInterfaceIPAddress);
				lsaList.add(lsa);
				this.getSendingQueue().add(ospfv2Packet);
				
				//Y ahora el contrario
				IntraDomainEdge edge_op=((SimpleTEDB)this.getDomainTEDB()).getNetworkGraph().getEdge(dst, src);
				if (edge_op!=null){
					TE_Information TE_info_op=edge_op.getTE_info();
					OSPFv2LinkStateUpdatePacket ospfv2Packet_op = new OSPFv2LinkStateUpdatePacket();
					ospfv2Packet_op.setRouterID(src);
					LinkedList<LSA> lsaList_op = new LinkedList<LSA>();
					OSPFTEv2LSA lsa_op = new OSPFTEv2LSA();
					LinkTLV linkTLV_op=new LinkTLV();
					lsa_op.setLinkTLV(linkTLV_op);
					linkTLV_op.setMaximumBandwidth(TE_info_op.getMaximumBandwidth());
					linkTLV_op.setUnreservedBandwidth(TE_info_op.getUnreservedBandwidth());

					LocalInterfaceIPAddress localInterfaceIPAddress_op= new LocalInterfaceIPAddress();
					LinkedList<Inet4Address> lista_op =localInterfaceIPAddress_op.getLocalInterfaceIPAddressList();
					lista_op.add(dst);
					linkTLV_op.setLocalInterfaceIPAddress(localInterfaceIPAddress_op);
					RemoteInterfaceIPAddress remoteInterfaceIPAddress_op= new RemoteInterfaceIPAddress();
					LinkedList<Inet4Address> listar_op = remoteInterfaceIPAddress_op.getRemoteInterfaceIPAddressList();
					listar_op.add(src);
					linkTLV_op.setRemoteInterfaceIPAddress(remoteInterfaceIPAddress_op);
					lsaList_op.add(lsa_op);
					this.getSendingQueue().add(ospfv2Packet_op);
				}

			}
			else {
				log.severe("Error en setMLLSP. Edge null");
			}		

		}
	}
	/**
	 * For MultiLayer Networks
	 * @param erolist
	 */
	@Override
	public void removeMLLSP(LinkedList<EROSubobject> erolist, boolean bidirect, BandwidthRequestedGeneralizedBandwidth GB){
		log.info("Setting ML LSP with ERO: "+erolist.toString());
		
		for (int i=1;i<erolist.size()-2;++i){
			Inet4Address src=null;
			Inet4Address dst=null;
			if (erolist.get(i).getType()==SubObjectValues.ERO_SUBOBJECT_IPV4PREFIX){
				src=((IPv4prefixEROSubobject)erolist.get(i)).getIpv4address();
			}else if (erolist.get(i).getType()==SubObjectValues.ERO_SUBOBJECT_UNNUMBERED_IF_ID){
				src=((UnnumberIfIDEROSubobject)erolist.get(i)).getRouterID();
			}
			if (erolist.get(i+1).getType()==SubObjectValues.ERO_SUBOBJECT_IPV4PREFIX){
				dst=((IPv4prefixEROSubobject)erolist.get(i+1)).getIpv4address();
			}else if (erolist.get(i+1).getType()==SubObjectValues.ERO_SUBOBJECT_UNNUMBERED_IF_ID){
				dst=((UnnumberIfIDEROSubobject)erolist.get(i+1)).getRouterID();
			}
			IntraDomainEdge edge=((SimpleTEDB)this.getDomainTEDB()).getNetworkGraph().getEdge(src, dst);
			if (edge != null){
				TE_Information TE_info=edge.getTE_info();
				OSPFv2LinkStateUpdatePacket ospfv2Packet = new OSPFv2LinkStateUpdatePacket();
				ospfv2Packet.setRouterID(src);
				LinkedList<LSA> lsaList = new LinkedList<LSA>();
				OSPFTEv2LSA lsa = new OSPFTEv2LSA();
				LinkTLV linkTLV=new LinkTLV();
				lsa.setLinkTLV(linkTLV);
				linkTLV.setMaximumBandwidth(TE_info.getMaximumBandwidth());
				linkTLV.setUnreservedBandwidth(TE_info.getUnreservedBandwidth());

				LocalInterfaceIPAddress localInterfaceIPAddress= new LocalInterfaceIPAddress();
				LinkedList<Inet4Address> lista =localInterfaceIPAddress.getLocalInterfaceIPAddressList();
				lista.add(src);
				linkTLV.setLocalInterfaceIPAddress(localInterfaceIPAddress);
				RemoteInterfaceIPAddress remoteInterfaceIPAddress= new RemoteInterfaceIPAddress();
				LinkedList<Inet4Address> listar = remoteInterfaceIPAddress.getRemoteInterfaceIPAddressList();
				listar.add(dst);
				linkTLV.setRemoteInterfaceIPAddress(remoteInterfaceIPAddress);
				lsaList.add(lsa);
				this.getSendingQueue().add(ospfv2Packet);
				
				//Y ahora el contrario
				IntraDomainEdge edge_op=((SimpleTEDB)this.getDomainTEDB()).getNetworkGraph().getEdge(dst, src);
				if (edge_op!=null){
					TE_Information TE_info_op=edge_op.getTE_info();
					OSPFv2LinkStateUpdatePacket ospfv2Packet_op = new OSPFv2LinkStateUpdatePacket();
					ospfv2Packet_op.setRouterID(src);
					LinkedList<LSA> lsaList_op = new LinkedList<LSA>();
					OSPFTEv2LSA lsa_op = new OSPFTEv2LSA();
					LinkTLV linkTLV_op=new LinkTLV();
					lsa_op.setLinkTLV(linkTLV_op);
					linkTLV_op.setMaximumBandwidth(TE_info_op.getMaximumBandwidth());
					linkTLV_op.setUnreservedBandwidth(TE_info_op.getUnreservedBandwidth());

					LocalInterfaceIPAddress localInterfaceIPAddress_op= new LocalInterfaceIPAddress();
					LinkedList<Inet4Address> lista_op =localInterfaceIPAddress_op.getLocalInterfaceIPAddressList();
					lista_op.add(dst);
					linkTLV_op.setLocalInterfaceIPAddress(localInterfaceIPAddress_op);
					RemoteInterfaceIPAddress remoteInterfaceIPAddress_op= new RemoteInterfaceIPAddress();
					LinkedList<Inet4Address> listar_op = remoteInterfaceIPAddress_op.getRemoteInterfaceIPAddressList();
					listar_op.add(src);
					linkTLV_op.setRemoteInterfaceIPAddress(remoteInterfaceIPAddress_op);
					lsaList_op.add(lsa_op);
					this.getSendingQueue().add(ospfv2Packet_op);
				}

			}
			else {
				log.severe("Error en setMLLSP. Edge null");
			}		

		}
	}


	public boolean reset(){
		//Volver a leer la topologia 
		this.setDomainTEDB(new SimpleTEDB());		
		if (this.getFile() !=null){
			log.info("Initializing TEDB de "+this.getFile());
			this.getDomainTEDB().initializeFromFile(this.getFile());
		}
		else {
			log.severe("Network file NOT included!!!");
			return false;
		}

		

		//enviar por ospf
		return true;
		
	}

	@Override
	public void removeLSP(LinkedList<EROSubobject> erolist, boolean bidirect,
			BandwidthRequestedGeneralizedBandwidth GB, float bw) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean setLSP_UpperLayer(
			LinkedList<EROSubobject> eROSubobjectListIP, float bw,
			boolean bidirect) {
		// TODO Auto-generated method stub
		return false;
	}

}
