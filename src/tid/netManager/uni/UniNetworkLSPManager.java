package tid.netManager.uni;

import java.net.Inet4Address;
import java.util.LinkedList;
import java.util.logging.Logger;

import es.tid.pce.pcep.objects.BandwidthRequestedGeneralizedBandwidth;
import es.tid.rsvp.objects.subobjects.EROSubobject;
import es.tid.rsvp.objects.subobjects.IPv4prefixEROSubobject;
import es.tid.rsvp.objects.subobjects.SubObjectValues;
import tid.ipnms.datamodel.router.LabelSwitchedPath.LabelSwitchedPathProperties;
import tid.ipnms.datamodel.router.LabelSwitchedPath.LabelSwitchedPathWithUnnumIf;
import tid.netManager.NetworkLSPManager;
import tid.netManager.NetworkLSPManagerTypes;

public class UniNetworkLSPManager extends NetworkLSPManager {
	private long id;
	/**
	 * Logger
	 */
	 Logger log =Logger.getLogger("UniNetworkLSPManager");
	 
	public UniNetworkLSPManager(){
		this.setEmulatorType(NetworkLSPManagerTypes.UNI_NETWORK);		
	}
	@Override
	public boolean setLSP(LinkedList<EROSubobject> erolist, boolean bidirect, BandwidthRequestedGeneralizedBandwidth GB) {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public boolean setMLLSP(LinkedList<EROSubobject> erolist, boolean bidirect, BandwidthRequestedGeneralizedBandwidth GB) {
		log.info("Reserving LSP and sending capacity update");
		
/*		LabelSwitchedPathWithUnnumIf label=new LabelSwitchedPathWithUnnumIf();
		LabelSwitchedPathProperties lspProperties= new LabelSwitchedPathProperties("gigether","lambda-switching","ethernet","grid");
		
		label.setLspProperties(lspProperties);
		label.setPathName("LSP"+String.valueOf(id));
		label.setLspId("LSP"+String.valueOf(id));
		label.setSource("192.168.8.3");
		label.setDestination("192.168.8.1");

		label.setPath(erolist);
		NoIPNMSDispatcher noIPNMSDispatcher=new NoIPNMSDispatcher();	
		noIPNMSDispatcher.createLSP(label);
		*/
		
		
		NoIPNMSDispatcher disp = new NoIPNMSDispatcher();
		disp.enableLSP("3to1");
		
		return true;
	}

	public boolean delMLLSP(LinkedList<EROSubobject> erolist, boolean bidirect, BandwidthRequestedGeneralizedBandwidth GB) {
		log.info("Reserving LSP and sending capacity update");
		
/*		LabelSwitchedPathWithUnnumIf label=new LabelSwitchedPathWithUnnumIf();
		LabelSwitchedPathProperties lspProperties= new LabelSwitchedPathProperties("gigether","lambda-switching","ethernet","grid");
		
		label.setLspProperties(lspProperties);
		label.setPathName("LSP"+String.valueOf(id));
		label.setLspId("LSP"+String.valueOf(id));
		label.setSource("192.168.8.3");
		label.setDestination("192.168.8.1");

		label.setPath(erolist);
		NoIPNMSDispatcher noIPNMSDispatcher=new NoIPNMSDispatcher();	
		noIPNMSDispatcher.createLSP(label);
		*/
		
		
		NoIPNMSDispatcher disp = new NoIPNMSDispatcher();
		disp.disableLSP("3to1");
		
		return true;
	}
	
		
				
		
	




	@Override
	public void removeLSP(LinkedList<EROSubobject> erolist, boolean bidirect, BandwidthRequestedGeneralizedBandwidth GB) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeMLLSP(LinkedList<EROSubobject> erolist, boolean bidirect, BandwidthRequestedGeneralizedBandwidth GB) {

/*		log.info("Delete LSP");
		
		LabelSwitchedPathWithUnnumIf label=new LabelSwitchedPathWithUnnumIf();
		LabelSwitchedPathProperties lspProperties= new LabelSwitchedPathProperties("gigether","lambda-switching","ethernet","grid");
		
		label.setLspProperties(lspProperties);
		label.setPathName("LSP"+String.valueOf(id));
		label.setLspId("LSP"+String.valueOf(id));
		label.setSource("192.168.8.3");
		label.setDestination("192.168.8.1");

		label.setPath(erolist);
		NoIPNMSDispatcher noIPNMSDispatcher=new NoIPNMSDispatcher();	
		noIPNMSDispatcher.deleteLSP(label);*/
	
		NoIPNMSDispatcher disp = new NoIPNMSDispatcher();
		disp.disableLSP("3to1");
		
	}
	
	private int createPathLSP(LinkedList<EROSubobject> eroSubObjList,LabelSwitchedPathWithUnnumIf label){
		boolean layerInfoFound=false;
		int numNewLinks=0;		
		 LinkedList<EROSubobject> path= new  LinkedList<EROSubobject> ();
		
		LabelSwitchedPathProperties lspProperties= new LabelSwitchedPathProperties("gigether","lambda-switching","ethernet","grid");
		label.setLspProperties(lspProperties);
		
		for (int i=0;i<eroSubObjList.size();++i){
			
			if ((eroSubObjList.get(i)).getType()==SubObjectValues.ERO_SUBOBJECT_LAYER_INFO){
				System.out.println("ServerLayerInfo");
				if (layerInfoFound==false){
					layerInfoFound=true;							
					//Create a new ERO to add at the list	
					path.add(eroSubObjList.get(i-1));
					numNewLinks=numNewLinks+1;												
				}else {
					System.out.println("Acabo pongo layerInfoEnded a true");
					layerInfoFound=false;
					path.add(eroSubObjList.get(i+1));
				//	path.add(((IPv4prefixEROSubobject)(eroSubObjList.get(i+1))).getIpv4address());
				}
			}
			else if (layerInfoFound==true){
				path.add(eroSubObjList.get(i));
				//path.add(((IPv4prefixEROSubobject)(eroSubObjList.get(i))).getIpv4address());
			}
			else {	
				path.add(eroSubObjList.get(i));
			//	path.add(((IPv4prefixEROSubobject)(eroSubObjList.get(i))).getIpv4address());
			}
		}
		label.setPath(path);
		return numNewLinks;
	}
	
	/**
	 * arriba=capa IP/MPLS
	 * abajo=capa optica
	 * Funcion que crea un path LSP de la forma: IP(capa de arriba)-IP(capa de abajo)-IP(capa de abajo)-IP(capa de arriba)
	 * Si ocurre que el camino se establece por abajo, por arriba y otra vez por abajo, el path LSP ser�:
	 * IP(capa de arriba)-IP(capa de abajo)-IP(capa de abajo)-IP(capa de arriba)-IP(capa de arriba)-IP(capa de abajo)-IP(capa de abajo)-IP(capa de arriba)
	 * Para establecer dos caminos por abajo, es decir, pasar al plano de control que queremos establecer esos caminos por abajo  
	 * @param eroSubObjList
	 * @param path
	 * @return
	 */
	private int createPathLSP(LinkedList<EROSubobject> eroSubObjList,LinkedList<Inet4Address> path){
		boolean layerInfoFound=false;
		int numNewLinks=0;		
		path=new LinkedList<Inet4Address>();
		for (int i=0;i<eroSubObjList.size();++i){
			if ((eroSubObjList.get(i)).getType()==SubObjectValues.ERO_SUBOBJECT_LAYER_INFO){
				if (layerInfoFound==false){
					layerInfoFound=true;							
					//Create a new ERO to add at the list	
					path.add(((IPv4prefixEROSubobject)(eroSubObjList.get(i-1))).getIpv4address());
					numNewLinks=numNewLinks+1;												
				}else {
					System.out.println("Acabo pongo layerInfoEnded a true");
					layerInfoFound=false;
					path.add(((IPv4prefixEROSubobject)(eroSubObjList.get(i+1))).getIpv4address());
				}
			}
			else if (layerInfoFound==true){
				path.add(((IPv4prefixEROSubobject)(eroSubObjList.get(i))).getIpv4address());
			}
			else {		
				path.add(((IPv4prefixEROSubobject)(eroSubObjList.get(i))).getIpv4address());
			}
		}
		return numNewLinks;
	}
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	@Override
	public boolean setLSP_UpperLayer(
			LinkedList<EROSubobject> eROSubobjectList_IP, float bw,
			boolean bidirect) {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public void removeLSP(LinkedList<EROSubobject> erolist, boolean bidirect,
			BandwidthRequestedGeneralizedBandwidth GB, float bw) {
		// TODO Auto-generated method stub
		
	}
	
	
}