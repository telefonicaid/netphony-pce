package tid.pce.parentPCE;

import java.net.Inet4Address;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

import tid.bgp.bgp4.messages.BGP4Update;
import tid.bgp.bgp4Peer.updateTEDB.UpdateProccesorThread;
import tid.pce.pcep.objects.Notification;
import tid.pce.tedb.ITMDTEDB;
import tid.pce.tedb.MDTEDB;

public class MultiDomainTopologyUpdater {
	
	private Logger log;
	
	private MultiDomainTopologyUpdaterThread mdtuThread;
	
	private LinkedBlockingQueue<MultiDomainUpdate> multiDomainUpdateQueue;
	
//	private LinkedBlockingQueue<ITMultiDomainUpdate> ITmultiDomainUpdateQueue;
	
	private MDTEDB multiDomainTEDB;
	
	private ITMDTEDB ITmultiDomainTEDB;
	/**
	 * BGP
	 */
	 private boolean bgpActivated=false;
	private LinkedBlockingQueue<BGP4Update> updateList;
	private UpdateProccesorThread updateProccesorThread;
	
	
	public MultiDomainTopologyUpdater(MDTEDB multiDomainTEDB, boolean bgpActivated){
		log=Logger.getLogger("MultiDomainTologyUpdater");
		multiDomainUpdateQueue= new LinkedBlockingQueue<MultiDomainUpdate>();
		this.multiDomainTEDB=multiDomainTEDB;
		this.bgpActivated=bgpActivated;
	}
	
	public MultiDomainTopologyUpdater(ITMDTEDB ITmultiDomainTEDB){
		log=Logger.getLogger("PCEServer");
		multiDomainUpdateQueue= new LinkedBlockingQueue<MultiDomainUpdate>();
		this.updateList=new LinkedBlockingQueue<BGP4Update>();
		this.ITmultiDomainTEDB=ITmultiDomainTEDB;
	}
	
	public void initialize(){
		log.info("Multidomain Topology Updater initiated");
//		if (bgpActivated){
//			this.updateProccesorThread=new UpdateProccesorThread(updateList, multiDomainTEDB);		
//			updateProccesorThread.start();
//		}else{
		mdtuThread= new MultiDomainTopologyUpdaterThread(multiDomainUpdateQueue,multiDomainTEDB);
		mdtuThread.start();
		//}
	}
	
	public void ITinitialize(){
		log.info("IT Multidomain Topology Updater initiated");

		mdtuThread= new MultiDomainTopologyUpdaterThread(multiDomainUpdateQueue,ITmultiDomainTEDB);
		mdtuThread.start();		
	}
	
	public void processNotification(Notification notif, Inet4Address pceId, Inet4Address domainID){
		MultiDomainUpdate multiDomainUpdate= new MultiDomainUpdate();
		multiDomainUpdate.setDomainID(domainID);
		multiDomainUpdate.setPCEID(pceId);
		multiDomainUpdate.setNotif(notif);
		multiDomainUpdateQueue.add(multiDomainUpdate);
	}
	public void dispathRequests(BGP4Update updateMessage){
		log.info("Adding update message to the queue");
		updateList.add(updateMessage);
	}

	public void stopMultiDomainTopologyUpdater(){
		if (mdtuThread!=null){
			mdtuThread.interrupt();
		}
	}
	
	public String printLSAList(){
		return mdtuThread.printLSAList();
	}
	
	public String printLSAShortList(){
		return mdtuThread.printLSAShortList();
	}
	
	public int sizeLSAList(){
		return mdtuThread.sizeLSAList();
	}

	public UpdateProccesorThread getUpdateProccesorThread() {
		return updateProccesorThread;
	}

}
