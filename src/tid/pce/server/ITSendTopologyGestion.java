package tid.pce.server;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.TimerTask;
import java.util.logging.Logger;

import es.tid.pce.pcep.objects.tlvs.StorageTLV;
import es.tid.tedb.InterDomainEdge;
import es.tid.tedb.SimpleITTEDB;

public class ITSendTopologyGestion extends TimerTask {
	   private SimpleITTEDB ITtedb;
	   private Logger log; //Este es el output de ChildPCESessionManager?? o hay que pasarle el ChildPCESessionManager???
	   
	   //oooo
	   private ChildPCESessionManager pcm;
	   
	   ITSendTopologyGestion(SimpleITTEDB ITtedb,ChildPCESessionManager pcm) {
           this.ITtedb = ITtedb;
           this.log =Logger.getLogger("PCEServer");
       }


    public void run() {
    	log.info("Showing interDomain links");
    	LinkedList<InterDomainEdge> interDomainLinks= ITtedb.getInterDomainLinks();
    	//if (interDomainLinks!=null){
    	int size = interDomainLinks.size();
    	if (size == 0){
    		log.warning("Size 0. There is not interdomain links");
    	}
    	for (int i=0;i<size;i++){
    		log.info("Source: "+interDomainLinks.get(i).getSrc_router_id()+"\tInterface id: "+interDomainLinks.get(i).getSrc_if_id()
    				+"\nDestiny: "+ interDomainLinks.get(i).getDst_router_id()+"\tInterface id: "+interDomainLinks.get(i).getDst_if_id());
    	}	
    	//}
    	
    	log.info("Showing IT resources");
    	Hashtable<StorageTLV,Object> storageanddomain= ITtedb.getStorageCharacteristics();
    	Enumeration <StorageTLV> storages = storageanddomain.keys();
    	if (storages.hasMoreElements()){
    		StorageTLV storage = storages.nextElement();
    		log.info("Storage "+storage.getResourceIDSubTLV().getResourceID().toString()+" has TotalSize:"+
    				storage.storageSize.getTotalSize()+" and AvailableSize"+storage.storageSize.getAvailableSize()
    				+" and UsageUnit:"+storage.getCostList().getFirst().getUsageUnit().toString()+" and UnitaryCost:"+
    				storage.getCostList().getFirst().getUnitaryPrice().toString());
    	}
    	
    	return;
    }

}
