package tid.pce.server;

import java.util.LinkedList;
import java.util.TimerTask;
import java.util.logging.Logger;

import tid.pce.parentPCE.ReachabilityEntry;
import tid.pce.pcep.constructs.Notify;
import tid.pce.pcep.messages.PCEPNotification;
import tid.pce.pcep.objects.Notification;
import tid.pce.pcep.objects.ObjectParameters;
import tid.pce.pcep.objects.tlvs.ReachabilityTLV;
import tid.pce.tedb.DomainTEDB;
import tid.rsvp.objects.subobjects.IPv4prefixEROSubobject;

public class SendReachabilityTask extends TimerTask{
	   private DomainTEDB tedb;
	   private Logger log;	   
	   private ChildPCESessionManager pcm;
	   /**
		 * Construct new TimeTask which sends the inter-domain links
		 * Domain TEDB, traffic Engineering Database
		 * ChildPCESessionManager
		 */
	   SendReachabilityTask(DomainTEDB tedb,ChildPCESessionManager pcm) {
           this.tedb = tedb;
           this.log =Logger.getLogger("PCEServer");
           this.pcm = pcm;         
       }
	@Override
	public void run() {
		ReachabilityEntry reachabilityEntry = tedb.getReachabilityEntry();
  		//1.- Create a notification message:  NOTIFICATION Object-Class is 12 and NOTIFICATION Object-Type is 1.
    	PCEPNotification notificationMessage = new PCEPNotification();
    	//2.- Create Notify Object
    	Notify notify = new Notify();
    	LinkedList<Notification> notificationList = new LinkedList<Notification>();
    	//3.- Create Object Notification
    	Notification notification=new Notification();
    	//3.1.- Set Notification type = PCEP_NOTIFICATION_TYPE_REACHABILITY=100
    	notification.setNotificationType(ObjectParameters.PCEP_NOTIFICATION_TYPE_REACHABILITY);
    	//4.- Create a IPv4prefixEROSubobject
    	IPv4prefixEROSubobject ipv4prefixEROSubobject = new IPv4prefixEROSubobject();
    	//4.1.- Configurate the Header of the IPv4prefixEROSubobject
    	//Inet4Address
    	ipv4prefixEROSubobject.setIpv4address(reachabilityEntry.getAggregatedIPRange());
    	//prefix
    	ipv4prefixEROSubobject.setPrefix(reachabilityEntry.getPrefix());
    		
    	//5.- Create a ReachabilityTLV
    	ReachabilityTLV reachabilityTLV = new ReachabilityTLV();
    	reachabilityTLV.addEROSubobject(ipv4prefixEROSubobject);
    	
    	//6.- Add the ReachabilityTLV and the notification object
    	notification.addReachabilityTLV(reachabilityTLV);
    	
    	notificationList.add(notification);
    	notify.setNotificationList(notificationList);
    	notificationMessage.addNotify(notify);
    	//7.- Send the notification to the PCE parent
    	pcm.getSendingQueue().add(notificationMessage);
   	
    	return;
		
	}
	

}
