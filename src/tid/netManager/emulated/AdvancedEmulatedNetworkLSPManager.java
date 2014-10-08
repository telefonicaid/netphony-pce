package tid.netManager.emulated;

import java.net.Inet4Address;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

import es.tid.ospf.ospfv2.OSPFv2LinkStateUpdatePacket;
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


/**
 * - Comprueba que hay recursos: si no hay fallo. DONE
 * - Introducir un ancho de banda a reservar. DONE
 * - Introducir bidireccional o no bidireccional. DONE
 * - Sincronizar el acceso a la base de datos, sincronizar los metodos para que no se pisen (al comprobar recursos y actualizarlos)DONE
 * 
 * @author mcs
 *
 */
public class AdvancedEmulatedNetworkLSPManager extends NetworkLSPManager{
	 private ReentrantLock lock = new ReentrantLock();
	 Logger log= Logger.getLogger("PCCClient");
	public AdvancedEmulatedNetworkLSPManager(LinkedBlockingQueue<OSPFv2LinkStateUpdatePacket> sendingQueue, String file ){
		this.setEmulatorType(NetworkLSPManagerTypes.ADVANCED_EMULATED_NETWORK);
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
   			
   		Idea: Compruebas que puedas reservar todo y si es as�, lo reservas y mandas OSPF
	 * @param erolist
	 */
	@Override
	public synchronized boolean setLSP(LinkedList<EROSubobject> erolist,boolean bidirect, BandwidthRequestedGeneralizedBandwidth GB) {
		log.info("Setting LSP with ERO: "+erolist.toString());
		ArrayList<Inet4Address> src=new ArrayList<Inet4Address>();
		ArrayList<Inet4Address> dst=new ArrayList<Inet4Address>();
		ArrayList<DWDMWavelengthLabel> dwdmWavelengthLabel=new ArrayList< DWDMWavelengthLabel>();
		ArrayList<IntraDomainEdge > edge = new ArrayList<IntraDomainEdge >();
		ArrayList<IntraDomainEdge > edge_op = new ArrayList<IntraDomainEdge >();
		ArrayList<Integer> lambda = new ArrayList<Integer>();	//Considero que la lambda NO es la misma para todos los enlaces
		int number_lambdas=0;
		//Check if the path is possible from the src to the dst.
		lock.lock();
		try{
			for (int i=0;i<erolist.size()-1;++i){
				if (initializeVariables(erolist,i,src,dst,dwdmWavelengthLabel,number_lambdas)){
					if (!(checkLambdaFree(edge,dwdmWavelengthLabel,src,dst,lambda,(i-number_lambdas)))){
						return false;
					}
					if (bidirect){
						if (!(checkLambdaFree(edge_op,dwdmWavelengthLabel,dst,src,lambda,(i-number_lambdas)))){
							return false;

						}
					}
				}else{
					number_lambdas++;//There is lambda in the EROLIST
				}
			}
			//If the path is possible, reserve the resources and send the OSPF message
			for (int i=0;i<edge.size();++i){
				if (lambda.size()!=0){
					edge.get(i).getTE_info().setWavelengthOccupied(lambda.get(i));
				}
				sendMessageOSPF(src.get(i),dst.get(i));
				if (bidirect){
					if (lambda.size()!=0){
						edge_op.get(i).getTE_info().setWavelengthOccupied(lambda.get(i));
					}
					sendMessageOSPF(dst.get(i),src.get(i));
				}
			}
		}finally{
			lock.unlock();
		}//End 
		return true;		
	}



	@Override
	public boolean setMLLSP(LinkedList<EROSubobject> erolist,boolean bidirect, BandwidthRequestedGeneralizedBandwidth GB) {
		log.info("Setting ML LSP with ERO: "+erolist.toString());
		ArrayList<Inet4Address> src=new ArrayList<Inet4Address>();
		ArrayList<Inet4Address> dst=new ArrayList<Inet4Address>();
		ArrayList<DWDMWavelengthLabel> dwdmWavelengthLabel=new ArrayList< DWDMWavelengthLabel>();
		ArrayList<IntraDomainEdge > edge = new ArrayList<IntraDomainEdge >();
		ArrayList<IntraDomainEdge > edge_op = new ArrayList<IntraDomainEdge >();
		ArrayList<Integer> lambda = new ArrayList<Integer>();	//Considero que la lambda NO es la misma para todos los enlaces
		int number_lambdas=0;
		lock.lock();
		try{
		for (int i=0;i<erolist.size()-2;++i){
			if (initializeVariables(erolist,i,src,dst,dwdmWavelengthLabel,number_lambdas)){
				if (!(checkLambdaFree(edge,dwdmWavelengthLabel,src,dst,lambda,(i-number_lambdas)))){
					return false;
				}
				if (bidirect){
					if (!(checkLambdaFree(edge_op,dwdmWavelengthLabel,dst,src,lambda,(i-number_lambdas)))){
						return false;
									
					}
				}
			}else{
				number_lambdas++;//There is lambda in the EROLIST
			}
		}
		//If the path is possible, reserve the resources and send the OSPF message
		for (int i=0;i<edge.size();++i){
			if (lambda.size()!=0){
				edge.get(i).getTE_info().setWavelengthOccupied(lambda.get(i));
			}
			sendMessageOSPF(src.get(i),dst.get(i));
			if (bidirect){
				if (lambda.size()!=0){
					edge_op.get(i).getTE_info().setWavelengthOccupied(lambda.get(i));
				}
				sendMessageOSPF(dst.get(i),src.get(i));
			}
		}		
		}finally{
			lock.unlock();
		}//End 
		return true;
	}

	@Override
	public synchronized void removeLSP(LinkedList<EROSubobject> erolist, boolean bidirect, BandwidthRequestedGeneralizedBandwidth GB) {
 log.info("Removing ML LSP with ERO: "+erolist.toString());
		
		for (int i=0;i<erolist.size()-1;++i){
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
				int lambda = 0;	
				if (dwdmWavelengthLabel!= null){//hay lambda
					lambda = dwdmWavelengthLabel.getN()- ((SimpleTEDB)this.getDomainTEDB()).getWSONinfo().getnMin();
					log.info("Removing Lambda: "+lambda);	
					edge.getTE_info().setWavelengthFree(lambda);
				}						
				sendMessageOSPF(src,dst);			
				if (bidirect){
					//Y ahora el contrario
					IntraDomainEdge edge_op=((SimpleTEDB)this.getDomainTEDB()).getNetworkGraph().getEdge(dst, src);
					if (edge_op != null){
						if (dwdmWavelengthLabel!= null)
							edge_op.getTE_info().setWavelengthFree(lambda);
					sendMessageOSPF(dst,src);
					}
				}
			}
			else {
				log.severe("Error en setMLLSP. Edge null");
			}		
		}
	}

	@Override
	public void removeMLLSP(LinkedList<EROSubobject> erolist,boolean bidirect, BandwidthRequestedGeneralizedBandwidth GB) {
	log.info("Removing ML LSP with ERO: "+erolist.toString());
		
		for (int i=0;i<erolist.size()-2;++i){
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

				int lambda = 0;	
				if (dwdmWavelengthLabel!= null){//hay lambda
					lambda = dwdmWavelengthLabel.getN()- ((SimpleTEDB)this.getDomainTEDB()).getWSONinfo().getnMin();
					log.info("Removing Lambda: "+lambda);	
				
				}	
				edge.getTE_info().setWavelengthFree(lambda);
				sendMessageOSPF(src,dst);			
					if (bidirect){				
						//Y ahora el contrario
						IntraDomainEdge edge_op=((SimpleTEDB)this.getDomainTEDB()).getNetworkGraph().getEdge(dst, src);											
						edge_op.getTE_info().setWavelengthFree(lambda);
						sendMessageOSPF(dst,src);
						}
					

			}
			else {
				log.severe("Error en removeMLLSP. Edge null");
			}		

		}
		
	}


	/**
	 * Function which initialize the variable src, dst and dwdmWavelengthLabel with the erolist
	 * @param erolist EROSubobject list
	 * @param i index in which save the variables
	 * @param src list of source address 
	 * @param dst list of destination address
	 * @param dwdmWavelengthLabel lambda
	 */
	private boolean initializeVariables(LinkedList<EROSubobject> erolist,int i,ArrayList<Inet4Address> src,ArrayList<Inet4Address> dst,ArrayList<DWDMWavelengthLabel> dwdmWavelengthLabel, int number_lambdas){
		if (erolist.get(i).getType()==SubObjectValues.ERO_SUBOBJECT_LABEL){
			return false;
		}
		if (erolist.get(i).getType()==SubObjectValues.ERO_SUBOBJECT_IPV4PREFIX){
			src.add(i-number_lambdas,((IPv4prefixEROSubobject)erolist.get(i)).getIpv4address());
		}else if (erolist.get(i).getType()==SubObjectValues.ERO_SUBOBJECT_UNNUMBERED_IF_ID){
			src.add(i-number_lambdas,((UnnumberIfIDEROSubobject)erolist.get(i)).getRouterID());
		}
		if (erolist.get(i+1).getType()==SubObjectValues.ERO_SUBOBJECT_IPV4PREFIX){
			dst.add(i-number_lambdas,((IPv4prefixEROSubobject)erolist.get(i+1)).getIpv4address());		
			
		}else if (erolist.get(i+1).getType()==SubObjectValues.ERO_SUBOBJECT_UNNUMBERED_IF_ID){
			dst.add(i-number_lambdas,((UnnumberIfIDEROSubobject)erolist.get(i+1)).getRouterID());
		}else if (erolist.get(i+1).getType()==SubObjectValues.ERO_SUBOBJECT_LABEL){
			
			if (dwdmWavelengthLabel==null){
				dwdmWavelengthLabel=new ArrayList<DWDMWavelengthLabel>();
				dwdmWavelengthLabel.add(i,((GeneralizedLabelEROSubobject) erolist.get(i+1)).getDwdmWavelengthLabel());
			}else{
				dwdmWavelengthLabel.add(i-number_lambdas,((GeneralizedLabelEROSubobject) erolist.get(i+1)).getDwdmWavelengthLabel());
			}						
			if (erolist.get(i+2).getType()==SubObjectValues.ERO_SUBOBJECT_IPV4PREFIX){
				dst.add(i-number_lambdas,((IPv4prefixEROSubobject)erolist.get(i+2)).getIpv4address());
			}else if (erolist.get(i+2).getType()==SubObjectValues.ERO_SUBOBJECT_UNNUMBERED_IF_ID){
				dst.add(i-number_lambdas,((UnnumberIfIDEROSubobject)erolist.get(i+2)).getRouterID());
			}
		}
	return true;
	}
	private boolean checkLambdaFree(ArrayList<IntraDomainEdge> edge,ArrayList<DWDMWavelengthLabel> dwdmWavelengthLabel,ArrayList<Inet4Address> src,ArrayList<Inet4Address> dst,ArrayList<Integer> lambda, int i){
		IntraDomainEdge edge1 =((SimpleTEDB)this.getDomainTEDB()).getNetworkGraph().getEdge(src.get(i), dst.get(i));  

		log.info("Check lambda Free in edge :"+ src.get(i).toString()+"-"+dst.get(i).toString());
		if (edge1 != null){
			edge.add(i,edge1);
			if (dwdmWavelengthLabel!= null){
				if (dwdmWavelengthLabel.size()!=0){	
					lambda.add(i,dwdmWavelengthLabel.get(i).getN() - ((SimpleTEDB)this.getDomainTEDB()).getWSONinfo().getnMin());
					log.info("There is lambda ("+lambda.get(i)+").");
					/*Hay un ancho de banda a reservar*/
					if (!(edge.get(i).getTE_info().isWavelengthFree(lambda.get(i)))){	
						log.info("La lambda ya esta ocupada!!!");
						//FIXME: C�mo acabar
						return false;	
					}		
				}
			}
		}
		else {			
			log.severe("Error en setMLLSP. Edge null");
			return false;
		}
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
