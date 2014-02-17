package tid.pce.server;

import java.util.Collection;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

import tid.pce.pcep.constructs.Notify;
import tid.pce.pcep.messages.PCEPMessage;
import tid.pce.pcep.messages.PCEPNotification;
import tid.pce.server.wson.ReservationManager;

/**
 * Esta funcion sera usada por el PCE para incluir una nueva notificacion a ser enviada
 * @author mcs
 *
 */
public class NotificationDispatcher {
	private NotificationProcessorThread npt;
	private LinkedBlockingQueue<Notify> notificationList;

	private Logger log;
	
	public NotificationDispatcher(ReservationManager reservationManager){
		this.notificationList=new LinkedBlockingQueue<Notify>();
		this.npt=new NotificationProcessorThread(notificationList, reservationManager);
		
		npt.start();
		log=Logger.getLogger("PCEServer");
	}
	
	public void dispatchNotification(PCEPNotification m_not){
		log.info("Adding notifications");
		notificationList.addAll(m_not.getNotifyList());
	}



	

}
