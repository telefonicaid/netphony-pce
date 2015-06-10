package tid.vntm;

import java.net.Inet4Address;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import es.tid.rsvp.objects.subobjects.EROSubobject;
import es.tid.rsvp.objects.subobjects.IPv4prefixEROSubobject;
import tid.netManager.NetworkLSPManager;
import tid.netManager.NetworkLSPManagerTypes;
import tid.netManager.uni.UniNetworkLSPManager;

public class LSPManager {
	private  Hashtable<Integer, LSP> lspList;
	private static int id = 0;
	
	private boolean bidirect=true;
	
	private  Hashtable<Integer, LSP> lspListIP;
	
	public Hashtable<Integer, LSP> getLspListIP() {
		return lspListIP;
	}
	public void setLspListIP(Hashtable<Integer, LSP> lspListIP) {
		this.lspListIP = lspListIP;
	}
	private static int id_IP = 0;
	
	public static int getId_IP() {
		return id_IP;
	}
	public static void setId_IP(int idIP) {
		id_IP = idIP;
	}
	
	private NetworkLSPManager netLSPManager;
	private Logger log;
	
	LSPManager(){
		lspList = new Hashtable<Integer, LSP> ();
		lspListIP = new Hashtable<Integer, LSP> ();
		
		log=Logger.getLogger("VNTMServer");
		
	}
	/**
	 * Aniade un LSP a la lista de LSP activas y llama al network manager para que la gestione
	 * @param eROSubobjectList
	 * @return
	 */
	public int addLSP(LinkedList<EROSubobject> eROSubobjectList){
		log.info("Adding New TE Link");
		//FIXME: hay que poner si es bidireccional  o no
		boolean bidirect=true;
		LSP lsp = new LSP();
		lsp.setId(getIdNewLSP());
		lsp.setEROSubobjectList(eROSubobjectList);
		lspList.put(id, lsp);
		log.info("Establish the TE Link");
		
		if (netLSPManager.getEmulatorType() == NetworkLSPManagerTypes.UNI_NETWORK){
			((UniNetworkLSPManager)netLSPManager).setId(id);
		}
					///////////////// CAMBIOS SERGIO ////////////////////////
					/////////////// ESTABLECER LSP IP ////////////////////////
			
		boolean SetMLL = netLSPManager.setMLLSP(eROSubobjectList,bidirect, null); 
		
		if (SetMLL==false){
			// Stolen Lambda
			return -1;
		}
		else
			return id;
		
		/***********************************************************************/
		//mirar si meto aquí lo del RealiseMLCapacityTask 
		/***********************************************************************/
	}
	
    /**
     * 
     * @param idLSP
     * @return
     */
	public synchronized int getIdNewLSP() {
		LSPManager.id=LSPManager.id+1;
		int newLSP=LSPManager.id;
		if (LSPManager.id>=Integer.MAX_VALUE){
			LSPManager.id=0;
		}
		return newLSP;
	}
	
	public void removeLSP( Inet4Address source, Inet4Address destination){
		log.info("Removing updated BW in TE Link from "+source+" to "+destination);
		//FIXME: Mirar de donde saco si es bidirect o no
		//Collection<LSP> lsps=lspList.values();
		Set<Map.Entry<Integer, LSP>> lspSet=lspList.entrySet();
		//Iterator<LSP>  itlsps =lsps.iterator();
		Iterator<Map.Entry<Integer, LSP>>  itlsps =lspSet.iterator();
		while (itlsps.hasNext()){
			Map.Entry<Integer, LSP> lspMap=itlsps.next(); 
			LSP lsp= lspMap.getValue();				
			if (((IPv4prefixEROSubobject)(lsp.getEROSubobjectList().getFirst())).ipv4address.equals(source)){
				if (((IPv4prefixEROSubobject)(lsp.getEROSubobjectList().getLast())).ipv4address.equals(destination)){
					LinkedList<EROSubobject> eROSubobjectList = lsp.getEROSubobjectList();
					if (netLSPManager.getEmulatorType() == NetworkLSPManagerTypes.UNI_NETWORK){
						((UniNetworkLSPManager)netLSPManager).setId(lspMap.getKey());
					}
					netLSPManager.removeMLLSP(eROSubobjectList,bidirect, null); //CAMPO BANDWIDTH
					//lspList.remove(lspMap.getKey());
					log.info("TELINK Removed");
					return;
				}
			}
		}
	}
	public void removeAllLSPs(){
		//Collection<LSP> lsps=lspList.values();
		Set<Map.Entry<Integer, LSP>> lspSet=lspList.entrySet();
	
		Iterator<Map.Entry<Integer, LSP>>  itlsps =lspSet.iterator();
		while (itlsps.hasNext()){
			Map.Entry<Integer, LSP> lspMap=itlsps.next(); 
			LSP lsp= lspMap.getValue();			
			LinkedList<EROSubobject> eROSubobjectList = lsp.getEROSubobjectList();
			netLSPManager.removeMLLSP(eROSubobjectList,false, null); //CAMPO BANDWIDTH !!!
			lspList.remove(lspMap.getKey());
		}
	}
	
	public long countCapacity(){
		long numberLSPs=0;
		Enumeration<LSP> enumerationLSP = lspList.elements();
		if ( lspList.size()==0 ){
			return 0;
		}
		LSP lsp;
		while (enumerationLSP.hasMoreElements()){
			lsp = enumerationLSP.nextElement();
			numberLSPs =numberLSPs + (lsp.getEROSubobjectList().size() - 3);
		}
		return numberLSPs;
	}
	void removeLSP(int id){
		lspList.remove(id);
		return;
	}

	
	public String printLSPs() {
		// TODO Auto-generated method stub
		Enumeration<LSP> enumerationLSP = lspList.elements();
		if ( lspList.size()==0 ){
			return "TE Link table empty";
		}
		String string="";
		LSP lsp;
		while (enumerationLSP.hasMoreElements()){
			lsp = enumerationLSP.nextElement();
			string =string +"LSP id: "+ String.valueOf(lsp.getId())+"->\t"+lsp.getEROSubobjectList().toString()+"\n\t";
		}
		return string;
	}
	public long countTELinks(){
		return lspList.size();
	}
	public NetworkLSPManager getNetLSPManager() {
		return netLSPManager;
	}
	public void setNetLSPManager(NetworkLSPManager net) {
		this.netLSPManager = net;
	}
}
