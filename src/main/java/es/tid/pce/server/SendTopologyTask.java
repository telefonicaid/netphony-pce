package es.tid.pce.server;
import java.net.Inet4Address;
import java.util.LinkedList;
import java.util.TimerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.tid.ospf.ospfv2.lsa.InterASTEv2LSA;
import es.tid.ospf.ospfv2.lsa.tlv.LinkTLV;
import es.tid.ospf.ospfv2.lsa.tlv.subtlv.IPv4RemoteASBRID;
import es.tid.ospf.ospfv2.lsa.tlv.subtlv.LinkLocalRemoteIdentifiers;
import es.tid.ospf.ospfv2.lsa.tlv.subtlv.RemoteASNumber;
import es.tid.pce.pcep.constructs.Notify;
import es.tid.pce.pcep.messages.PCEPNotification;
import es.tid.pce.pcep.objects.Notification;
import es.tid.pce.pcep.objects.ObjectParameters;
import es.tid.pce.pcep.objects.tlvs.OSPFTE_LSA_TLV;
import es.tid.tedb.DomainTEDB;
import es.tid.tedb.InterDomainEdge;

/**
 * TimerTask in which the child PCE sends the inter-domain links to the Parent PCE  
 * @author mcs
 *
 */
public class SendTopologyTask extends TimerTask {
	   private DomainTEDB tedb;
	   private Logger log;	   
	   private ChildPCESessionManager pcm;
	   /**
		 * Construct new TimeTask which sends the inter-domain links
		 * Domain TEDB, traffic Engineering Database
		 * ChildPCESessionManager
		 */
	   SendTopologyTask(DomainTEDB tedb,ChildPCESessionManager pcm) {
           this.tedb = tedb;
           this.log =LoggerFactory.getLogger("PCEServer");
           this.pcm = pcm;         
       }

	  /**
	   * Method run sends the inter-domain links to the Parent PCE
	   * 
	   * The message is sent in a PCEPNotification message with a topology notification Type.
	   * It is used a OSPFTE_LSA_TLV with a interASTEv2LSA linkTLV which describes a single link.   
	   */
    public void run() {
    	log.info("Showing interDomain links");
    	LinkedList<InterDomainEdge> interDomainLinks= tedb.getInterDomainLinks();
    	int size = interDomainLinks.size();
    	if (size == 0){
    		log.warn("Size 0. There is not interdomain links");
    	}
    	for (int i=0;i<size;i++){
    		log.info("Source: "+interDomainLinks.get(i).getSrc_router_id()+"\tInterface id: "+interDomainLinks.get(i).getSrc_if_id()
    				+"\nDestiny: "+ interDomainLinks.get(i).getDst_router_id()+"\tInterface id: "+interDomainLinks.get(i).getDst_if_id());
    	}	
    	//Send the interdomain-links to the Parent PCE
    	//Para todos los nodos de borde del dominio
    	for (int i=0;i<size;i++){
    	//Parte nueva: Enviar los enlaces inter dominio al padre.
    	//1.- Create a notification message:  NOTIFICATION Object-Class is 12 and NOTIFICATION Object-Type is 1.
    	PCEPNotification notificationMessage = new PCEPNotification();
    	//2.- Create Notify Object
    	Notify notify = new Notify();
    	LinkedList<Notification> notificationList = new LinkedList<Notification>();
    	//3.- Create Object Notification
    	Notification notification=new Notification();
    	//3.1.- Set Notification type = PCEP_NOTIFICATION_TYPE_TOPOLOGY=101
    	notification.setNotificationType(ObjectParameters.PCEP_NOTIFICATION_TYPE_TOPOLOGY);
    	//4.- Create a LinkTLV
    	LinkTLV linkTLV = new LinkTLV();
    	//4.1.- Configurate the Header of the linkTLV
    	//LinkLocalRemoteIdentifiers: interfaces de red
    	LinkLocalRemoteIdentifiers linkLocalRemoteId = new LinkLocalRemoteIdentifiers();
    	linkLocalRemoteId.setLinkLocalIdentifier(interDomainLinks.get(i).getSrc_if_id());
    	linkLocalRemoteId.setLinkRemoteIdentifier(interDomainLinks.get(i).getDst_if_id());
    	linkTLV.setLinkLocalRemoteIdentifiers(linkLocalRemoteId);
    	//RemoteASNumber: identifica el dominio remoto 
    	RemoteASNumber remoteASNumber = new RemoteASNumber();
    	log.info("Remote AS nNumner "+interDomainLinks.get(i).getDomain_dst_router());
    	remoteASNumber.setRemoteASNumber((Inet4Address)interDomainLinks.get(i).getDomain_dst_router());
    	linkTLV.setRemoteASNumber(remoteASNumber);
    	//IPv4RemoteASBRID: direccion IP del Router Remoto
    	IPv4RemoteASBRID iPv4RemoteASBRID = new IPv4RemoteASBRID();
    	iPv4RemoteASBRID.setIPv4RemoteASBRID((Inet4Address)interDomainLinks.get(i).getDst_router_id());
    	linkTLV.setIPv4RemoteASBRID(iPv4RemoteASBRID);
    	
    	//5.- Create a interASTEv2LSA 
    	InterASTEv2LSA interASTEv2LSA = new InterASTEv2LSA(); 
    	//5.1.- Advertising Router (del LSA): IP del router que manda el notify (son routers de borde)
    	interASTEv2LSA.setAdvertisingRouter((Inet4Address)interDomainLinks.get(i).getSrc_router_id());
    	//5.2.- Add LinkTLV
    	interASTEv2LSA.setLinkTLV(linkTLV);
    	    	
    	//6.- Create the TLV
    	OSPFTE_LSA_TLV ospfte_lsa_tlv = new OSPFTE_LSA_TLV();
    	ospfte_lsa_tlv.setInterASTEv2LSA(interASTEv2LSA);
    	
    	//7.- Add the TLV and the notification object
    	notification.addOSPFTE_LSA_TLV(ospfte_lsa_tlv);
    	notificationList.add(notification);
    	notify.setNotificationList(notificationList);
    	notificationMessage.addNotify(notify);
    	//8.- Send the notification to the PCE parent
    	pcm.getSendingQueue().add(notificationMessage);
   	}
    	return;
    }

}
