package tid.netManager.emulated;

import java.net.Inet4Address;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

import es.tid.ospf.ospfv2.OSPFv2LinkStateUpdatePacket;
import es.tid.ospf.ospfv2.lsa.tlv.subtlv.MaximumReservableBandwidth;
import es.tid.ospf.ospfv2.lsa.tlv.subtlv.UnreservedBandwidth;
import es.tid.pce.pcep.constructs.GeneralizedBandwidthSSON;
import es.tid.pce.pcep.objects.BandwidthRequestedGeneralizedBandwidth;
import es.tid.rsvp.constructs.gmpls.DWDMWavelengthLabel;
import es.tid.rsvp.objects.subobjects.EROSubobject;
import es.tid.rsvp.objects.subobjects.GeneralizedLabelEROSubobject;
import es.tid.rsvp.objects.subobjects.IPv4prefixEROSubobject;
import es.tid.rsvp.objects.subobjects.SubObjectValues;
import es.tid.rsvp.objects.subobjects.UnnumberIfIDEROSubobject;
import tid.netManager.NetworkLSPManager;
import tid.netManager.NetworkLSPManagerTypes;
import tid.pce.client.emulator.AutomaticTesterStatistics;
import tid.pce.tedb.IntraDomainEdge;
import tid.pce.tedb.MultiLayerTEDB;
import tid.pce.tedb.SimpleTEDB;
import tid.vntm.LSPManager;
import tid.vntm.LigthPathCreateIP;

/**
 * Network Emulator.
 * 
 * Clase encargada de mandar los mensajes OSPF.
 * 
 * Actualiza la topologia con las LSP que recibe, ya sea para establecer un
 * camino (setLSP) como para eliminar un camino (removeLSP).
 * 
 * @author mcs
 */
public class CompletedEmulatedNetworkLSPManager extends NetworkLSPManager{

	/**
	 * Variable usada para bloquear la lectura y escritura en la TEDB
	 */
	private ReentrantLock lock = new ReentrantLock();

	private LigthPathCreateIP LSPcreateIP;

	private LSPManager lsp_manger;

	/**
	 * Estadisticas. Usada para analizar el tiempo teorico que tardamos en
	 * configurar la red para un LSP recivido.
	 */
	private AutomaticTesterStatistics stats;
	// loggers
	private Logger log= Logger.getLogger("PCCClient");
	/**
	 * Variable usada para saber si la red es multilayer o no
	 */
	private boolean multilayer=false;
	/**
	 * Time the ROADM lasts to configure
	 */
	private long ROADMTime=10;
	/**
	 * Constructor
	 * @param sendingQueue cola donde incluimos los mensaje ospf
	 * @param file fichero de la topologia, de donde leemos la topologia
	 * @param stats estadisticas
	 * @param isMultilayer boolean indicando si la red es multilayer o no
	 */
	public CompletedEmulatedNetworkLSPManager(
			LinkedBlockingQueue<OSPFv2LinkStateUpdatePacket> sendingQueue,
			String file,AutomaticTesterStatistics stats, boolean isMultilayer) {

		this.setEmulatorType(NetworkLSPManagerTypes.COMPLETED_EMULATED_NETWORK);
		this.multilayer=isMultilayer;
		if (isMultilayer)
			this.setDomainTEDB(new MultiLayerTEDB());
		else
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
		this.stats = stats;
	}

	/**
	 * Recorre el ERO y actualiza las propiedades de TE
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

   		Idea: Compruebas que puedas reservar todo y si es asi, lo reservas y mandas OSPF
	 * @param erolist
	 */
	@Override
	public boolean setLSP_UpperLayer(LinkedList<EROSubobject> eROSubobjectList_IP, float bw_req, boolean bidirect) {
		log.info("Setting LSP IP");
		ArrayList<Inet4Address> src=new ArrayList<Inet4Address>();
		ArrayList<Inet4Address> dst=new ArrayList<Inet4Address>();
		ArrayList<DWDMWavelengthLabel> dwdmWavelengthLabel=null;
		ArrayList<IntraDomainEdge> edge = new ArrayList<IntraDomainEdge>();
		ArrayList<IntraDomainEdge> edge_op = new ArrayList<IntraDomainEdge>();
		ArrayList<Integer> lambda = new ArrayList<Integer>();
		lambda = null;
		int number_lambdas=0;
		int reserved =0;
		int reserved_op=0;
		int layer = LayerTypes.UPPER_LAYER;
		BandwidthRequestedGeneralizedBandwidth GB = null;
		long controlPlaneDelay=0;
		int j=0;
		int number_hops =(eROSubobjectList_IP.size()-1);
		boolean error = false;
		boolean inicialize=false;
		for (int i=0;i<eROSubobjectList_IP.size()-1;++i){
			lock.lock();
			try{
				if (initializeVariables(eROSubobjectList_IP,i,src,dst,dwdmWavelengthLabel,number_lambdas, layer)){
					inicialize=true;
					if (updateEdge(edge,dwdmWavelengthLabel,src,dst,lambda, (i-number_lambdas),true, GB, layer, j,bw_req)){
						reserved++;

						// Mirar si bidireccional
						if (bidirect){
							if (updateEdge(edge_op,dwdmWavelengthLabel, dst, src, lambda, (i - number_lambdas), true, GB, layer, j, bw_req))
								reserved_op++;
							else{
								error = true;
								break;
							}
						}
						j++;
					}else{
						error = true;
						break;
					}
				}else{
					inicialize=false;
				}
			}finally{
				lock.unlock();
			}//END

			if (inicialize){//Si no se ha inicializado no sumamos control plane delay porque erolist(i) se trata de una lambda 
				if (edge.get(i-number_lambdas).getDelay_ms() > 0){
					controlPlaneDelay=controlPlaneDelay+sleepControlPlane((long) edge.get(i-number_lambdas).getDelay_ms(),i);
				}else {				
					controlPlaneDelay=controlPlaneDelay+sleepControlPlane(ROADMTime,i);						
					
				}
			}
		}
		

		//Envio los OSPFs cuando llego al final, de vuelta, esperandome los retardos
		if (!(bidirect))
			reserved_op=reserved;
		if ((reserved == number_hops)&&(reserved_op == number_hops)){
			for (int i=reserved-1;i>=0;--i){
				sendMessageOSPF(src.get(i),dst.get(i), multilayer, layer, null);
				if (edge.get(i).getDelay_ms() > 0){
					controlPlaneDelay=controlPlaneDelay+sleepControlPlane((long) edge.get(i).getDelay_ms(),i);
				}else {				
					controlPlaneDelay=controlPlaneDelay+sleepControlPlane(ROADMTime,i);
				}
				if (bidirect){
					sendMessageOSPF(dst.get(i),src.get(i), multilayer, layer, null);
				}
			}
		}else
		{	//Desreservar los recursos que hemos reservado
			
			lock.lock();
			try{
				for (int i=0;i<reserved;++i){

					float bw1 = edge.get(i).getTE_info().getMaximumReservableBandwidth().maximumReservableBandwidth;
					if (bw1 < edge.get(i).getTE_info().getMaximumBandwidth().getMaximumBandwidth()){
						bw1 = bw1 + bw_req;
						float[] bw_un = new float[8];
						bw_un[0] = bw1;
						MaximumReservableBandwidth maximumReservableBandwidth = new MaximumReservableBandwidth();
						UnreservedBandwidth unreservedBandwidth = new UnreservedBandwidth();
						
						maximumReservableBandwidth.setMaximumReservableBandwidth(bw1);
						unreservedBandwidth.setUnreservedBandwidth(bw_un);
						
						edge.get(i).getTE_info().setMaximumReservableBandwidth(maximumReservableBandwidth);
						edge.get(i).getTE_info().setUnreservedBandwidth(unreservedBandwidth);
						
						if (edge.get(i).getDelay_ms() > 0){
							controlPlaneDelay=controlPlaneDelay+sleepControlPlane((long) edge.get(i).getDelay_ms(),i);
						}else {				
							controlPlaneDelay=controlPlaneDelay+sleepControlPlane(ROADMTime,i);
						}
					}
					/*if (bw1 == 0) {
						LSPcreateIP.deleteLigthPath(edge.get(i).getSource(), edge.get(i).getTarget());
					}*/
				}
				/* Mirar si bidireccional */
				if (bidirect){
					for (int i=0;i<reserved_op;++i){
						float bw2 = edge_op.get(i).getTE_info().getMaximumReservableBandwidth().maximumReservableBandwidth;
						
						if (bw2<edge_op.get(i).getTE_info().getMaximumBandwidth().getMaximumBandwidth()){
							bw2 = bw2 + bw_req;
							float[] bw_un2 = new float[8];
							bw_un2[0] = bw2;
							MaximumReservableBandwidth maximumReservableBandwidth = new MaximumReservableBandwidth();
							UnreservedBandwidth unreservedBandwidth = new UnreservedBandwidth();
							
							maximumReservableBandwidth.setMaximumReservableBandwidth(bw2);
							unreservedBandwidth.setUnreservedBandwidth(bw_un2);
							
							edge_op.get(i).getTE_info().setMaximumReservableBandwidth(maximumReservableBandwidth);
							edge_op.get(i).getTE_info().setUnreservedBandwidth(unreservedBandwidth);
						}
					}
				}
			}finally{
				lock.unlock();
			}
			log.info("Not reserved. OSPF is not sent");
			
			return false;
		}
		if (stats!=null)
			stats.analyzeTheoricLSPTime((double) controlPlaneDelay);
		return true;
	}

	@Override
	public boolean setLSP(LinkedList<EROSubobject> erolist, boolean bidirect,
			BandwidthRequestedGeneralizedBandwidth GB) {
		ArrayList<Inet4Address> src=new ArrayList<Inet4Address>();
		ArrayList<Inet4Address> dst=new ArrayList<Inet4Address>();
		ArrayList<DWDMWavelengthLabel> dwdmWavelengthLabel = null;
		ArrayList<IntraDomainEdge> edge = new ArrayList<IntraDomainEdge>();
		ArrayList<IntraDomainEdge> edge_op = new ArrayList<IntraDomainEdge>();
		ArrayList<Integer> lambda = new ArrayList<Integer>();
		int reserved =0;
		int reserved_op=0;
		int number_lambdas=0;
		int layer = LayerTypes.SIMPLE_NETWORK;
		if (multilayer){
			layer = LayerTypes.UPPER_LAYER; // MPLS layer
		}
		else{
			layer = LayerTypes.SIMPLE_NETWORK; // WSON single layer
		}
		int j=0;
		//Numero de enlaces que tiene el camino, que tenemos que comprobar que estan disponibles.
		int number_hops =(erolist.size()-1)/2;
		dwdmWavelengthLabel=new ArrayList< DWDMWavelengthLabel>();
		long controlPlaneDelay=0;
		boolean inicialize=false;
		for (int i=0;i<erolist.size()-1;++i){
			lock.lock();
			try{
				if (initializeVariables(erolist,i,src,dst,dwdmWavelengthLabel,number_lambdas, layer)){
					inicialize=true;
					if (updateEdge(edge,dwdmWavelengthLabel,src,dst,lambda,
						(i-number_lambdas),multilayer, GB, layer, j,0)){
						reserved++;

						/*Mirar si bidireccional*/
						if (bidirect){
							if (updateEdge(edge_op,dwdmWavelengthLabel,dst,src,lambda, (i - number_lambdas),
									multilayer, GB, layer, j,0))
								reserved_op++;
							else
								break;
						}
						j++;
					}else{
						break;
					}
				}else{
					inicialize=false;
					number_lambdas++;// Hay una lambda
				}
			}finally{
				lock.unlock();
			}// End

			if (inicialize){//Si no se ha inicializado no sumamos control plane delay porque erolist(i) se trata de una lambda 
				if (edge.get(i-number_lambdas).getDelay_ms() > 0){
					controlPlaneDelay=controlPlaneDelay+sleepControlPlane((long) edge.get(i-number_lambdas).getDelay_ms(),i);
				}else {				
					controlPlaneDelay=controlPlaneDelay+sleepControlPlane(ROADMTime,i);						
				}
			}			
		}	
		
		//Envio los OSPFs cuando llego al final, de vuelta, esperandome los retardos
		if (!(bidirect))
			reserved_op = reserved;
		if ((reserved == number_hops) && (reserved_op == number_hops)) {
			for (int i = reserved - 1; i >= 0; --i) {
				sendMessageOSPF(src.get(i), dst.get(i), multilayer, layer, null);
				if (edge.get(i).getDelay_ms() > 0){
					controlPlaneDelay=controlPlaneDelay+sleepControlPlane((long) edge.get(i).getDelay_ms(),i);
				}else {				
					controlPlaneDelay=controlPlaneDelay+sleepControlPlane(ROADMTime,i);
				}
				if (bidirect) {
					sendMessageOSPF(dst.get(i), src.get(i), multilayer, layer, null);
				}
			}
		} else
		{// Desreservar los recursos que hemos reservado
			lock.lock();
			try{
				for (int i=0;i<reserved;++i){
					if (dwdmWavelengthLabel != null) {// hay lambda
						edge.get(i).getTE_info().setWavelengthFree(lambda.get(i));
						if (edge.get(i).getDelay_ms() > 0){
							controlPlaneDelay=controlPlaneDelay+sleepControlPlane((long) edge.get(i).getDelay_ms(),i);
						}else {				
							controlPlaneDelay=controlPlaneDelay+sleepControlPlane(ROADMTime,i);							
						}
					}
				}
				/* Mirar si bidireccional */
				if (bidirect){
					for (int i=0;i<reserved_op;++i){
						if (dwdmWavelengthLabel != null){// hay lambda
							edge_op.get(i).getTE_info().setWavelengthFree(lambda.get(i));
						}
					}
				}
			}finally{
				lock.unlock();
			}
			// log.info("Not reserved. OSPF is not sent");
			// logPrueba.info("Setting LSP with ERO: "+erolist.toString()+"\t>ERROR");
			return false;
		}
		if (stats!=null)
			stats.analyzeTheoricLSPTime((double) controlPlaneDelay);
		return true;

	}
	
	/**
	 * Recorre el ERO y actualiza las propiedades de TE setMLLSP send the LSP
	 * for multiLayer Networks. En la Erolist me viene: - interfaces no
	 * numeradas - IPv4Address - lambdas: (Objeto DWDMWavelengthLabel) con el
	 * siguiente formato 0 1 2 3 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3
	 * 4 5 6 7 8 9 0 1
	 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+ |Grid |
	 * C.S. | Identifier | n |
	 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 * 
	 * Idea: Compruebas que puedas reservar todo y si es as�, lo reservas y
	 * mandas OSPF
	 * 
	 * @param erolist
	 */
	@Override
	public boolean setMLLSP(LinkedList<EROSubobject> erolist, boolean bidirect,
			BandwidthRequestedGeneralizedBandwidth GB) {
		// log.info("Setting LSP with ERO: "+erolist.toString());
		ArrayList<Inet4Address> src=new ArrayList<Inet4Address>();
		ArrayList<Inet4Address> dst=new ArrayList<Inet4Address>();
		ArrayList<DWDMWavelengthLabel> dwdmWavelengthLabel = null;
		ArrayList<IntraDomainEdge> edge = new ArrayList<IntraDomainEdge>();
		ArrayList<IntraDomainEdge> edge_op = new ArrayList<IntraDomainEdge>();
		ArrayList<Integer> lambda = new ArrayList<Integer>();
		int reserved =0;
		int reserved_op=0;
		int number_lambdas=0;
		int layer = LayerTypes.LOWER_LAYER;
		boolean error = false;

		// Numero de enlaces que tiene el camino, que tenemos que comprobar que
		// estan disponibles.
		
		dwdmWavelengthLabel = new ArrayList<DWDMWavelengthLabel>();

		long controlPlaneDelay=0;
		boolean inicialize=false;
		
		LinkedList<EROSubobject> erolist_op = new LinkedList<EROSubobject>();
		erolist_op = (LinkedList<EROSubobject>)erolist.clone();
		
		
		erolist_op.removeFirst();
		erolist_op.removeLast();
		erolist_op.removeLast();
		int number_hops = (erolist_op.size() - 1) / 2;
		
		int j=0;
		for (int i=0;i<erolist_op.size()-1;++i){
			lock.lock();
			try{
				if (initializeVariables(erolist_op,i,src,dst,dwdmWavelengthLabel,number_lambdas, layer)){
					inicialize=true;
					if (updateEdge(edge,dwdmWavelengthLabel,src,dst,lambda,
						(i-number_lambdas),multilayer, GB, layer, j,0)){
						reserved++;

						/*Mirar si bidireccional*/
						if (bidirect){
							if (updateEdge(edge_op,dwdmWavelengthLabel,dst,src,lambda, (i - number_lambdas),
									multilayer, GB, layer, j,0))
								reserved_op++;
							else
								break;
						}
						j++;
					}else{
						error = true;
						break;
					}
				}else{
					inicialize=false;
					number_lambdas++;// Hay una lambda
				}
			}finally{
				lock.unlock();
			}// End

			if (inicialize){//Si no se ha inicializado no sumamos control plane delay porque erolist(i) se trata de una lambda 
				if (edge.get(i-number_lambdas).getDelay_ms() > 0){
					controlPlaneDelay=controlPlaneDelay+sleepControlPlane((long) edge.get(i-number_lambdas).getDelay_ms(),i);
				}else {				
					controlPlaneDelay=controlPlaneDelay+sleepControlPlane(ROADMTime,i);						
					
				}
			}			

		}
		
		// Envio los OSPFs cuando llego al final, de vuelta, esperandome los
		// retardos
		if (((reserved) == number_hops) && ((reserved_op) == number_hops)){
			for (int i = reserved-1; i >= 0; --i) {
				sendMessageOSPF(src.get(i), dst.get(i), multilayer, layer, dwdmWavelengthLabel.get(i));
				if (bidirect) {
					sendMessageOSPF(dst.get(i), src.get(i), multilayer, layer, dwdmWavelengthLabel.get(i));
				}
			}
		}
		else {// Desreservar los recursos que hemos reservado
			lock.lock();
			try {
				for (int i = 0; i < reserved; ++i) {
					if (dwdmWavelengthLabel != null) {// hay lambda
						edge.get(i).getTE_info().setWavelengthFree(lambda.get(i));
					}
				}
				/* Mirar si bidireccional */
				if (bidirect) {
					for (int i = 0; i < reserved_op; ++i) {
						if (dwdmWavelengthLabel != null) {// hay lambda
							edge_op.get(i).getTE_info().setWavelengthFree(lambda.get(i));
						}
					}
				}
			} finally {
				lock.unlock();
			}
			log.info("Not reserved. OSPF is not sent");
			// logPrueba.info("Setting LSP with ERO: "+erolist.toString()+"\t>ERROR");
			return false;
		}
		
		if (stats!=null)
			stats.analyzeTheoricLSPTime((double) controlPlaneDelay);
		return true;

	}
	/**
		Delete LSP WSON normal
	 */
	
	@Override
	public  void removeLSP(LinkedList<EROSubobject> erolist, boolean bidirect, BandwidthRequestedGeneralizedBandwidth GB) {
		//log.info("Removing LSP with ERO: "+erolist.toString());
		boolean isLambdaLoop=false;
		int layer;
		for (int i=0;i<erolist.size()-1;++i){
			Inet4Address src=null;
			Inet4Address dst=null;
			DWDMWavelengthLabel dwdmWavelengthLabel=null;
			if (erolist.get(i).getType()==SubObjectValues.ERO_SUBOBJECT_LABEL){
				isLambdaLoop = true;	
			}else{
				isLambdaLoop= false;
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
			}
			try{
				lock.lock();
				if (!isLambdaLoop){
					IntraDomainEdge edge=null;
					if (multilayer){
						edge=((MultiLayerTEDB)this.getDomainTEDB()).getUpperLayerGraph().getEdge(src, dst);
						layer = LayerTypes.UPPER_LAYER;
					}else{
						edge=((SimpleTEDB)this.getDomainTEDB()).getNetworkGraph().getEdge(src, dst);
						layer = LayerTypes.SIMPLE_NETWORK;
					}
	
					if (edge != null){
						int lambda = 0;
						if (GB != null){
							int m=((GeneralizedBandwidthSSON)GB.getGeneralizedBandwidth()).getM();
							if (dwdmWavelengthLabel!= null){//hay lambda
								if (multilayer){
									lambda = dwdmWavelengthLabel.getN() - ((MultiLayerTEDB)this.getDomainTEDB()).getWSONinfo().getnMin();
								}
								else{
									lambda = dwdmWavelengthLabel.getN() - ((SimpleTEDB)this.getDomainTEDB()).getWSONinfo().getnMin();
								}
												
								for (int j=(lambda - m);j<(lambda+m);j++){
									//log.info("Removing lambda: "+j);
									edge.getTE_info().setWavelengthFree(j);
								}
							}
		
							sendMessageOSPF(src,dst);			
							if (bidirect){				
								//Y ahora el contrario
								IntraDomainEdge edge_op;
								if (multilayer){
									edge_op=((MultiLayerTEDB)this.getDomainTEDB()).getUpperLayerGraph().getEdge(dst, src);
									layer = LayerTypes.UPPER_LAYER;
								}else{
									edge_op=((SimpleTEDB)this.getDomainTEDB()).getNetworkGraph().getEdge(dst, src);
									layer = LayerTypes.SIMPLE_NETWORK;
								}
								if (edge_op != null){
									if (dwdmWavelengthLabel!= null){
										for (int j=(lambda - m);j<(lambda+m);j++){
											edge_op.getTE_info().setWavelengthFree(j);
										}
									}
									sendMessageOSPF(dst,src);
								}
							}
						}
						else{
							if (dwdmWavelengthLabel!= null){//hay lambda
								if (multilayer){
									lambda = dwdmWavelengthLabel.getN() - ((MultiLayerTEDB)this.getDomainTEDB()).getWSONinfo().getnMin();
								}
								else{
									lambda = dwdmWavelengthLabel.getN() - ((SimpleTEDB)this.getDomainTEDB()).getWSONinfo().getnMin();
								}
								//log.info("Removing lambda: "+lambda);
								edge.getTE_info().setWavelengthFree(lambda);
							}
							
							if (multilayer){
								float bw_un = (edge.getTE_info().getUnreservedBandwidth().unreservedBandwidth)[0];
							}
		
							sendMessageOSPF(src,dst);			
							if (bidirect){				
								//Y ahora el contrario
								IntraDomainEdge edge_op;
								if (multilayer){
									edge_op=((MultiLayerTEDB)this.getDomainTEDB()).getUpperLayerGraph().getEdge(dst, src);
									
								}else{
									edge_op=((SimpleTEDB)this.getDomainTEDB()).getNetworkGraph().getEdge(dst, src);								
								}
								if (edge_op != null){
									if (dwdmWavelengthLabel!= null)
										edge_op.getTE_info().setWavelengthFree(lambda);
									sendMessageOSPF(dst,src);
								}
							}
						}
						
					}
					else {
						log.warning("Error en removeLSP. Edge null");
					
					}		
				}
			}
			finally{
				lock.unlock();
			}
		}
	}
	
	/**
		Sergio Remove LSP Multilayer
	 */
	@Override
	public void removeLSP(LinkedList<EROSubobject> erolist, boolean bidirect,
			BandwidthRequestedGeneralizedBandwidth GB, float bw_delete) {
		// log.info("Removing LSP with ERO: "+erolist.toString());
		boolean isLambdaLoop = false;
		int layer;
		for (int i=0;i<erolist.size()-1; ++i){
			Inet4Address src = null;
			Inet4Address dst = null;
			DWDMWavelengthLabel dwdmWavelengthLabel = null;
			if (erolist.get(i).getType() == SubObjectValues.ERO_SUBOBJECT_LABEL) {
				isLambdaLoop = true;
			}else {
				isLambdaLoop = false;
				if (erolist.get(i).getType() == SubObjectValues.ERO_SUBOBJECT_IPV4PREFIX) {
					src = ((IPv4prefixEROSubobject) erolist.get(i)).getIpv4address();
				} else if (erolist.get(i).getType() == SubObjectValues.ERO_SUBOBJECT_UNNUMBERED_IF_ID) {
					src = ((UnnumberIfIDEROSubobject) erolist.get(i)).getRouterID();
				} if (erolist.get(i + 1).getType() == SubObjectValues.ERO_SUBOBJECT_IPV4PREFIX) {
					dst = ((IPv4prefixEROSubobject) erolist.get(i + 1)).getIpv4address();
				} else if (erolist.get(i + 1).getType() == SubObjectValues.ERO_SUBOBJECT_UNNUMBERED_IF_ID) {
					dst = ((UnnumberIfIDEROSubobject) erolist.get(i + 1)).getRouterID();
				} else if (erolist.get(i + 1).getType() == SubObjectValues.ERO_SUBOBJECT_LABEL) {
					dwdmWavelengthLabel = ((GeneralizedLabelEROSubobject)erolist.get(i + 1)).getDwdmWavelengthLabel();
					if (erolist.get(i + 2).getType() == SubObjectValues.ERO_SUBOBJECT_IPV4PREFIX) {
						dst = ((IPv4prefixEROSubobject) erolist.get(i + 2)).getIpv4address();
					} else if (erolist.get(i + 2).getType() == SubObjectValues.ERO_SUBOBJECT_UNNUMBERED_IF_ID) {
						dst = ((UnnumberIfIDEROSubobject) erolist.get(i + 2)).getRouterID();
					}
				}
			}

			if (!isLambdaLoop) {
				IntraDomainEdge edge = null;
				if (multilayer) {
					edge = ((MultiLayerTEDB)this.getDomainTEDB()).getUpperLayerGraph().getEdge(src, dst);
					layer = LayerTypes.UPPER_LAYER;
				} else {
					edge = ((SimpleTEDB) this.getDomainTEDB()).getNetworkGraph().getEdge(src, dst);
					layer = LayerTypes.SIMPLE_NETWORK;
				}
				
				if (edge != null) {
					int lambda = 0;
					
					if (GB != null) {
						int m=((GeneralizedBandwidthSSON)GB.getGeneralizedBandwidth()).getM();
						if (dwdmWavelengthLabel != null) {// hay lambda
							if (multilayer) {
								lambda = dwdmWavelengthLabel.getN()	- ((MultiLayerTEDB)this.getDomainTEDB()).getWSONinfo().getnMin();
							} else {
								lambda = dwdmWavelengthLabel.getN()	- ((SimpleTEDB)this.getDomainTEDB()).getWSONinfo().getnMin();
							}

							for (int j = (lambda - m); j < (lambda + m); j++) {
								// log.info("Removing lambda: "+j);
								edge.getTE_info().setWavelengthFree(j);
							}
						}

						sendMessageOSPF(src, dst, multilayer, layer, null);
						if (bidirect) {
							// Y ahora el contrario
							IntraDomainEdge edge_op;
							if (multilayer) {
								edge_op = ((MultiLayerTEDB) this.getDomainTEDB()).getUpperLayerGraph().getEdge(dst, src);
								layer = LayerTypes.UPPER_LAYER;
							} else {
								edge_op = ((SimpleTEDB) this.getDomainTEDB()).getNetworkGraph().getEdge(dst, src);
								layer = LayerTypes.SIMPLE_NETWORK;
							}
							if (edge_op != null) {
								if (dwdmWavelengthLabel != null) {
									for (int j = (lambda - m); j < (lambda + m); j++) {
										edge_op.getTE_info().setWavelengthFree(j);
									}
								}
								sendMessageOSPF(dst, src, multilayer, layer, null);
							}
						}
					} else {
						if (dwdmWavelengthLabel != null) {// hay lambda
							if (multilayer) {
								lambda = dwdmWavelengthLabel.getN()
										- ((MultiLayerTEDB)this.getDomainTEDB()).getWSONinfo().getnMin();
							} else {
								lambda = dwdmWavelengthLabel.getN()-((SimpleTEDB) this.getDomainTEDB()).getWSONinfo().getnMin();
							}
							// log.info("Removing lambda: "+lambda);
							edge.getTE_info().setWavelengthFree(lambda);
						}
						
						else{
							float[] bw_un = new float[8];
							
							bw_un[0] = (edge.getTE_info().getUnreservedBandwidth().unreservedBandwidth)[0];
							float bw = edge.getTE_info().getMaximumReservableBandwidth().maximumReservableBandwidth;
							float bw_max = edge.getTE_info().getMaximumBandwidth().getMaximumBandwidth();
							
							bw_un[0] = bw_un[0] + bw_delete;
							bw = bw + bw_delete;
							MaximumReservableBandwidth maximumReservableBandwidth = new MaximumReservableBandwidth();
							UnreservedBandwidth unreservedBandwidth = new UnreservedBandwidth();
														
							maximumReservableBandwidth.setMaximumReservableBandwidth(bw);
							unreservedBandwidth.setUnreservedBandwidth(bw_un);
							
							edge.getTE_info().setMaximumReservableBandwidth(maximumReservableBandwidth);	
							edge.getTE_info().setUnreservedBandwidth(unreservedBandwidth);							
						}
						sendMessageOSPF(src, dst, multilayer, layer, null);
						
						if (bidirect) {
							// Y ahora el contrario
							IntraDomainEdge edge_op;
							if (multilayer) {
								edge_op = ((MultiLayerTEDB)this.getDomainTEDB()).getUpperLayerGraph().getEdge(dst, src);

							} else {
								edge_op = ((SimpleTEDB) this.getDomainTEDB()).getNetworkGraph().getEdge(dst, src);
							}
							if (edge_op != null) {
								if (dwdmWavelengthLabel != null)
									edge_op.getTE_info().setWavelengthFree(lambda);
								sendMessageOSPF(dst, src, multilayer, layer, null);
							}
						}
					}
				}
			}
		}
	}

	@Override
	public void removeMLLSP(LinkedList<EROSubobject> erolist, boolean bidirect,
			BandwidthRequestedGeneralizedBandwidth GB) {
		// log.info("Removing ML LSP with ERO: "+erolist.toString());
		int layer;
		boolean flag = false;
		for (int i=1;i<erolist.size()-3;++i){
			Inet4Address src = null;
			Inet4Address dst = null;

			DWDMWavelengthLabel dwdmWavelengthLabel = null;
			if (erolist.get(i).getType() == SubObjectValues.ERO_SUBOBJECT_IPV4PREFIX) {
				src = ((IPv4prefixEROSubobject) erolist.get(i)).getIpv4address();

			}else if (erolist.get(i).getType() == SubObjectValues.ERO_SUBOBJECT_UNNUMBERED_IF_ID) {
				src = ((UnnumberIfIDEROSubobject) erolist.get(i)).getRouterID();
			}
			else if (erolist.get(i).getType()==SubObjectValues.ERO_SUBOBJECT_LABEL) {
				continue;
			}
			log.info("Source :"+src);
			if (erolist.get(i+1).getType()==SubObjectValues.ERO_SUBOBJECT_IPV4PREFIX) {
				dst=((IPv4prefixEROSubobject)erolist.get(i+1)).getIpv4address();
			}
			else if (erolist.get(i+1).getType()==SubObjectValues.ERO_SUBOBJECT_UNNUMBERED_IF_ID) {
				dst=((UnnumberIfIDEROSubobject)erolist.get(i+1)).getRouterID();
			}
			else if (erolist.get(i+1).getType()==SubObjectValues.ERO_SUBOBJECT_LABEL) {
				dwdmWavelengthLabel = ((GeneralizedLabelEROSubobject)erolist.get(i + 1)).getDwdmWavelengthLabel();
				if (erolist.get(i+2).getType()==SubObjectValues.ERO_SUBOBJECT_IPV4PREFIX) {
					dst=((IPv4prefixEROSubobject)erolist.get(i+2)).getIpv4address();
				}
				else if (erolist.get(i+2).getType()==SubObjectValues.ERO_SUBOBJECT_UNNUMBERED_IF_ID) {
					dst=((UnnumberIfIDEROSubobject)erolist.get(i+2)).getRouterID();
				}
			}
			IntraDomainEdge edge = null;
			log.info("Removing LSP link: from "+src+" to "+dst);
			edge = ((MultiLayerTEDB) this.getDomainTEDB()).getLowerLayerGraph().getEdge(src, dst);
			layer = LayerTypes.LOWER_LAYER;
			if (edge != null) {
				int lambda = 0;
				if (GB != null) {
					int m=((GeneralizedBandwidthSSON)GB.getGeneralizedBandwidth()).getM();
					if (dwdmWavelengthLabel != null) {// hay lambda
						lambda = dwdmWavelengthLabel.getN() - this.getDomainTEDB().getWSONinfo().getnMin();
						for (int j = (lambda - m); j < (lambda + m); j++) {
							// log.info("Removing lambda: "+j);
							edge.getTE_info().setWavelengthFree(j);
						}
					}
					sendMessageOSPF(src, dst, multilayer, layer, null);
					if (bidirect) {
						// Y ahora el contrario
						IntraDomainEdge edge_op = null;
						edge_op = ((MultiLayerTEDB) this.getDomainTEDB()).getLowerLayerGraph().getEdge(dst, src);
						for (int j = (lambda - m); j < (lambda + m); j++) {
							edge_op.getTE_info().setWavelengthFree(j);
						}
						sendMessageOSPF(dst, src, multilayer, layer, null);
					} else {
						if (dwdmWavelengthLabel != null) {// hay lambda
							lambda = dwdmWavelengthLabel.getN() - this.getDomainTEDB().getWSONinfo().getnMin();
							log.info("Removing lambda: "+lambda);
						}
						edge.getTE_info().setWavelengthFree(lambda);
						sendMessageOSPF(src, dst, multilayer, layer, null);
						if (bidirect){
							//Y ahora el contrario
							IntraDomainEdge edge_op=null;
							edge_op=((MultiLayerTEDB)this.getDomainTEDB()).getLowerLayerGraph().getEdge(dst, src);
							edge_op.getTE_info().setWavelengthFree(lambda);
							sendMessageOSPF(dst, src, multilayer, layer, null);
						}
					}
				} else {
					if (dwdmWavelengthLabel != null) {// hay lambda
						lambda = dwdmWavelengthLabel.getN()	- ((MultiLayerTEDB) this.getDomainTEDB()).getWSONinfo().getnMin();
						log.info("Removing lambda: "+lambda);
						edge.getTE_info().setWavelengthFree(lambda);
					}
					log.info("Envío OSPF");
					sendMessageOSPF(src, dst, multilayer, layer, dwdmWavelengthLabel);
					if (bidirect) {
						// Y ahora el contrario
						IntraDomainEdge edge_op;
						edge_op = ((MultiLayerTEDB) this.getDomainTEDB()).getLowerLayerGraph().getEdge(dst, src);

						if (edge_op != null) {
							edge_op.getTE_info().setWavelengthFree(lambda);
							sendMessageOSPF(dst, src, multilayer, layer, dwdmWavelengthLabel);
						}
					}
				}
			} else {
				log.warning("Error en removeMLLSP. Edge null");
			}
		}
	}

	/**
	 * Function which initialize the variable src, dst and dwdmWavelengthLabel
	 * with the erolist
	 * @param erolist: EROSubobject list
	 * @param i: index in which save the variables
	 * @param src: list of source address
	 * @param dst: list of destination address
	 * @param dwdmWavelengthLabel: lambda
	 * @param number_lambdas: number of lambdas read, this is needed to store the scr, dst
	 *            and dwdmWavelengthLabel in the correct position of the array
	 */
	private boolean initializeVariables(LinkedList<EROSubobject> erolist,int i, 
			ArrayList<Inet4Address> src, ArrayList<Inet4Address> dst,
			ArrayList<DWDMWavelengthLabel> dwdmWavelengthLabel,	int number_lambdas, int layer) {

		if (layer == LayerTypes.UPPER_LAYER) {
			if (erolist.get(i).getType() == SubObjectValues.ERO_SUBOBJECT_IPV4PREFIX) {
				src.add(i,((IPv4prefixEROSubobject)erolist.get(i)).getIpv4address());
			}else if (erolist.get(i).getType()==SubObjectValues.ERO_SUBOBJECT_UNNUMBERED_IF_ID) {
				src.add(i,((UnnumberIfIDEROSubobject)erolist.get(i)).getRouterID());
			}

			if (erolist.get(i + 1).getType() == SubObjectValues.ERO_SUBOBJECT_IPV4PREFIX) {
				dst.add(i, ((IPv4prefixEROSubobject) erolist.get(i + 1)).getIpv4address());

			}else if (erolist.get(i+1).getType()==SubObjectValues.ERO_SUBOBJECT_UNNUMBERED_IF_ID) {
				dst.add(i,((UnnumberIfIDEROSubobject)erolist.get(i+1)).getRouterID());
			}
			return true;
		}

		else if (layer == LayerTypes.SIMPLE_NETWORK) {

			if (erolist.get(i).getType() == SubObjectValues.ERO_SUBOBJECT_LABEL) {
				return false;
			}
			if (erolist.get(i).getType() == SubObjectValues.ERO_SUBOBJECT_IPV4PREFIX) {
				src.add(i-number_lambdas, ((IPv4prefixEROSubobject)erolist.get(i)).getIpv4address());
			} else if (erolist.get(i).getType() == SubObjectValues.ERO_SUBOBJECT_UNNUMBERED_IF_ID) {
				src.add(i - number_lambdas, ((UnnumberIfIDEROSubobject)erolist.get(i)).getRouterID());
			}
			if (erolist.get(i + 1).getType() == SubObjectValues.ERO_SUBOBJECT_IPV4PREFIX) {
				dst.add(i - number_lambdas, ((IPv4prefixEROSubobject)erolist.get(i + 1)).getIpv4address());
			} else if (erolist.get(i + 1).getType() == SubObjectValues.ERO_SUBOBJECT_UNNUMBERED_IF_ID) {
				dst.add(i - number_lambdas, ((UnnumberIfIDEROSubobject)erolist.get(i + 1)).getRouterID());
			} else if (erolist.get(i + 1).getType() == SubObjectValues.ERO_SUBOBJECT_LABEL) {
				if (dwdmWavelengthLabel == null) {
					dwdmWavelengthLabel = new ArrayList<DWDMWavelengthLabel>();
					dwdmWavelengthLabel.add(number_lambdas, ((GeneralizedLabelEROSubobject) erolist.get(i + 1)).getDwdmWavelengthLabel());
				} else {
					dwdmWavelengthLabel.add(number_lambdas, ((GeneralizedLabelEROSubobject) erolist.get(i + 1)).getDwdmWavelengthLabel());
				}
				if (erolist.get(i + 2).getType() == SubObjectValues.ERO_SUBOBJECT_IPV4PREFIX) {
					dst.add(i - number_lambdas, ((IPv4prefixEROSubobject) erolist.get(i + 2)).getIpv4address());
				} else if (erolist.get(i + 2).getType() == SubObjectValues.ERO_SUBOBJECT_UNNUMBERED_IF_ID) {
					dst.add(i - number_lambdas, ((UnnumberIfIDEROSubobject) erolist.get(i + 2)).getRouterID());
				}
			}return true;
		} else if (layer == LayerTypes.LOWER_LAYER) {
			if (erolist.get(i).getType() == SubObjectValues.ERO_SUBOBJECT_LABEL) {
				return false;
			}
			if (erolist.get(i).getType() == SubObjectValues.ERO_SUBOBJECT_IPV4PREFIX) {
				src.add(i - number_lambdas, ((IPv4prefixEROSubobject)erolist.get(i)).getIpv4address());
			} else if (erolist.get(i).getType() == SubObjectValues.ERO_SUBOBJECT_UNNUMBERED_IF_ID) {
				src.add(i - number_lambdas, ((UnnumberIfIDEROSubobject)erolist.get(i)).getRouterID());
			}
			if (erolist.get(i + 1).getType() == SubObjectValues.ERO_SUBOBJECT_IPV4PREFIX) {
				dst.add(i - number_lambdas, ((IPv4prefixEROSubobject)erolist.get(i + 1)).getIpv4address());
			} else if (erolist.get(i + 1).getType() == SubObjectValues.ERO_SUBOBJECT_UNNUMBERED_IF_ID) {
				dst.add(i - number_lambdas, ((UnnumberIfIDEROSubobject)erolist.get(i + 1)).getRouterID());
			} else if (erolist.get(i + 1).getType() == SubObjectValues.ERO_SUBOBJECT_LABEL) {
				if (dwdmWavelengthLabel == null) {
					dwdmWavelengthLabel = new ArrayList<DWDMWavelengthLabel>();
					dwdmWavelengthLabel.add(number_lambdas, ((GeneralizedLabelEROSubobject)erolist.get(i + 1)).getDwdmWavelengthLabel());
				} else {
					dwdmWavelengthLabel.add(number_lambdas, ((GeneralizedLabelEROSubobject)erolist.get(i + 1)).getDwdmWavelengthLabel());
				}
				if (erolist.get(i + 2).getType() == SubObjectValues.ERO_SUBOBJECT_IPV4PREFIX) {
					dst.add(i - number_lambdas, ((IPv4prefixEROSubobject) erolist.get(i + 2)).getIpv4address());
				} else if (erolist.get(i + 2).getType() == SubObjectValues.ERO_SUBOBJECT_UNNUMBERED_IF_ID) {
					dst.add(i - number_lambdas, ((UnnumberIfIDEROSubobject) erolist.get(i + 2)).getRouterID());
				}
			}return true;
		}return false;
	}
	/**
	 * 
	 * @param edge
	 * @param dwdmWavelengthLabel
	 * @param src
	 * @param dst
	 * @param lambda
	 * @param i
	 * @param multilayer
	 * @return
	 */
	private boolean updateEdge(ArrayList<IntraDomainEdge> edge, ArrayList<DWDMWavelengthLabel> dwdmWavelengthLabel,
			ArrayList<Inet4Address> src, ArrayList<Inet4Address> dst, ArrayList<Integer> lambda, int i, boolean multilayer,
			BandwidthRequestedGeneralizedBandwidth GB, int layer, int k, float bw_req) {

		IntraDomainEdge edge1=null;
		int flag;
		

		if (multilayer) {
			if (layer == LayerTypes.LOWER_LAYER){
				edge1=((MultiLayerTEDB) this.getDomainTEDB()).getLowerLayerGraph().getEdge(src.get(i), dst.get(i));
			}else if (layer == LayerTypes.UPPER_LAYER) {
				edge1=((MultiLayerTEDB) this.getDomainTEDB()).getUpperLayerGraph().getEdge(src.get(i), dst.get(i));
			}
			flag = 1;
		}
		else {
			edge1 =((SimpleTEDB)this.getDomainTEDB()).getNetworkGraph().getEdge(src.get(i), dst.get(i));
			flag = 0;
		}
		
		if (edge1 != null) {
			edge.add(k, edge1);
			
			
			if (layer == LayerTypes.UPPER_LAYER) {
				float bw = edge.get(k).getTE_info().getUnreservedBandwidth().getUnreservedBandwidth()[0];
				float[] bw_un = new float[8];
				if (bw >= bw_req) {
					bw = bw - bw_req;
					bw_un[0]=bw;
					MaximumReservableBandwidth maximumReservableBandwidth = new MaximumReservableBandwidth();
					UnreservedBandwidth unreservedBandwidth = new UnreservedBandwidth();
										
					maximumReservableBandwidth.setMaximumReservableBandwidth(bw);
					unreservedBandwidth.setUnreservedBandwidth(bw_un);
					
					edge.get(k).getTE_info().setMaximumReservableBandwidth(maximumReservableBandwidth);
					edge.get(k).getTE_info().setUnreservedBandwidth(unreservedBandwidth);
					
					return true;
				}else{
					log.info("No hay suficiente Bandwidth en el edge");
					return false;
				}
			}
			if (dwdmWavelengthLabel != null){
				int min = 0;
				if (flag == 1){
					min = ((MultiLayerTEDB)this.getDomainTEDB()).getWSONinfo().getnMin();
				}else if (flag == 0) {
					min = ((SimpleTEDB) this.getDomainTEDB()).getWSONinfo().getnMin();
				}if (dwdmWavelengthLabel.size()!=0){
					if (GB != null){
						int m=((GeneralizedBandwidthSSON)GB.getGeneralizedBandwidth()).getM();
						for (int j=((dwdmWavelengthLabel.get(i).getN() - min)) - m; j < ((dwdmWavelengthLabel.get(i).getN() - min)) + m; j++) {
							lambda.add(i,j);
							if (edge.get(i).getTE_info().isWavelengthFree(lambda.get(i))) {
								//log.info("It's possible to reserve that lambda.");
								//edge.get(i).setWavelengthUnReserved(TE_info.getUnreservedBandwidth().getUnreservedBandwidth())[0]-10));

								//Actualizo los recursos
								edge.get(i).getTE_info().setWavelengthOccupied(lambda.get(i));
								if (j==((dwdmWavelengthLabel.get(i).getN() - min)) + m - 1) {
									return true;
								}
							}
						}
					}else{
						if (dwdmWavelengthLabel != null){
							if (dwdmWavelengthLabel.size()!=0){
								lambda.add(k,dwdmWavelengthLabel.get(k).getN() - min);
								/*Hay un ancho de banda a reservar */
								if (edge.get(k).getTE_info().isWavelengthFree(lambda.get(k))) {
									//Actualizo los recursos
									edge.get(k).getTE_info().setWavelengthOccupied(lambda.get(k));
									return true;
								}
							}
						}
					}
					log.info("ROBO DE LAMBDA en edge --> "+edge1.toString());
					return false;
				}return true;
			}return true;
		}else {
			log.info("EDGE ("+ src.get(i).toString() + "-" + dst.get(i).toString() + ") NULLLLLL");
			return false;
		}
	}
	private long sleepControlPlane(long num, int iteration){
		long controlPlaneDelay = 0;
		try {
			if (iteration == 0){
				Thread.sleep(num);
				controlPlaneDelay = num;
			}
			else {
				Thread.sleep(num+1);
				controlPlaneDelay = num+1;
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return controlPlaneDelay;
	}
	public AutomaticTesterStatistics getStats() {
		return stats;
	}

	public void setStats(AutomaticTesterStatistics stats) {
		this.stats = stats;
	}
	
	public long getROADMTime() {
		return ROADMTime;
	}
	public void setROADMTime(long rOADMTime) {
		ROADMTime = rOADMTime;
	}
}
