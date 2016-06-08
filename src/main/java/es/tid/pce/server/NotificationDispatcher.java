package es.tid.pce.server;

import java.util.concurrent.LinkedBlockingQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.tid.pce.pcep.constructs.Notify;
import es.tid.pce.pcep.messages.PCEPNotification;
import es.tid.pce.server.wson.ReservationManager;

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
		log=LoggerFactory.getLogger("PCEServer");
	}
	
	public void dispatchNotification(PCEPNotification m_not){
		log.info("Adding notifications");
		notificationList.addAll(m_not.getNotifyList());
	}



	

}
