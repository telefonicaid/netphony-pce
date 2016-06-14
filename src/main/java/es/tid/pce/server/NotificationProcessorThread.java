package es.tid.pce.server;

import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.tid.pce.pcep.constructs.Notify;
import es.tid.pce.pcep.objects.Notification;
import es.tid.pce.pcep.objects.ObjectParameters;
import es.tid.pce.server.wson.ReservationManager;
import es.tid.rsvp.constructs.gmpls.DWDMWavelengthLabel;
import es.tid.rsvp.objects.subobjects.EROSubobject;
import es.tid.rsvp.objects.subobjects.GeneralizedLabelEROSubobject;
import es.tid.rsvp.objects.subobjects.IPv4prefixEROSubobject;
import es.tid.rsvp.objects.subobjects.SubObjectValues;
import es.tid.rsvp.objects.subobjects.UnnumberIfIDEROSubobject;
import es.tid.tedb.SimpleTEDB;

public class NotificationProcessorThread extends Thread {

	private boolean running;
	
	private LinkedBlockingQueue<Notify> notificationList;
	
	private ReservationManager reservationManager;
	
	private Logger log;

	
	public NotificationProcessorThread(LinkedBlockingQueue<Notify> notificationList,ReservationManager reservationManager){
		running=true;
		this.notificationList=notificationList;
		this.reservationManager=reservationManager;

		log=LoggerFactory.getLogger("PCEServer");
		
	}
	public void run(){	
		//Notify notify;
		Notify notify;
		while (running) {
			try {
					notify= notificationList.take();
					LinkedList<Notification> notificationList = notify.getNotificationList();
						for (int i=0;i<notificationList.size();i++){
							Notification notif=notificationList.get(i);
							switch (notif.getNotificationType()){
							case ObjectParameters.PCEP_NOTIFICATION_TYPE_CANCEL_RESERVATION:{
								if (reservationManager!=null){
									if (notif.getNotificationValue()==ObjectParameters.PCEP_NOTIFICATION_VALUE_CANCEL_RESERVATION){
										if (notif.getReservationIDTLV()!=null){
											long idReservation=notif.getReservationIDTLV().getReservationID();
											log.info("Processing CANCEL RESERVATION NOTIFICATION");
											reservationManager.cancelReservation(idReservation);	
										}else {
											log.info("RESERVATION ID TLV NO VIENE");
										}

									}else if (notif.getNotificationValue()==ObjectParameters.PCEP_NOTIFICATION_VALUE_CANCEL_ALL_RESERVATIONS){
										reservationManager.cancelAllReservations();
									}

						}else {
							log.info("RESERVATION MANAGER ES NULL");
						}
						break;
					}
							case ObjectParameters.PCEP_NOTIFICATION_TYPE_PRERESERVE:	{
								log.info("PCEP NOTIFICATION TYPE: PRERESERVE");														
								//Crear una lista de source vertex, otra de target, wavelenght, time, bidirectional
								LinkedList<Object> sourceVertexList= new LinkedList<Object>();
								LinkedList<Object> targetVertexList= new LinkedList<Object>();
								DWDMWavelengthLabel dwdmWavelengthLabel= new DWDMWavelengthLabel();							
								int lambdaToModify =0;
								initializeVariables(notificationList.get(i).getNotificationTLV().geteRO().getEROSubobjectList(),sourceVertexList,targetVertexList,dwdmWavelengthLabel);
								lambdaToModify = dwdmWavelengthLabel.getN() - ((SimpleTEDB)reservationManager.getTed()).getWSONinfo().getnMin();
														
								//FIXME: aqui estamos suponiendo que todo el camino tiene la misma lambda!!!
								long time=notificationList.get(i).getNotificationTLV().getTime();						
								boolean bidirectional=notificationList.get(i).getNotificationTLV().isBidirectional();
								reservationManager.reserve(sourceVertexList, targetVertexList, lambdaToModify,time, bidirectional);
								break;
							}
							default:
								log.info("ERROR: unexpected message");
							}
						}

					
				
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
	
	
	private boolean initializeVariables(LinkedList<EROSubobject> erolist,LinkedList<Object> src,LinkedList<Object> dst,
			DWDMWavelengthLabel dwdmWavelengthLabel){
		int number_lambdas=0;
		boolean labelFound=false;
		for (int i=0;i<erolist.size()-1;++i){
		if (erolist.get(i).getType()==SubObjectValues.ERO_SUBOBJECT_LABEL){
			if (!labelFound){
				labelFound=true;
				dwdmWavelengthLabel=((GeneralizedLabelEROSubobject) erolist.get(i)).getDwdmWavelengthLabel();
			}
			number_lambdas++;
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
			if (erolist.get(i+2).getType()==SubObjectValues.ERO_SUBOBJECT_IPV4PREFIX){
				dst.add(i-number_lambdas,((IPv4prefixEROSubobject)erolist.get(i+2)).getIpv4address());
			}else if (erolist.get(i+2).getType()==SubObjectValues.ERO_SUBOBJECT_UNNUMBERED_IF_ID){
				dst.add(i-number_lambdas,((UnnumberIfIDEROSubobject)erolist.get(i+2)).getRouterID());
			}

			return true;
		}
		}
		return false;
			
	}
	
}

