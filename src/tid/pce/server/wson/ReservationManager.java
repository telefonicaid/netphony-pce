package tid.pce.server.wson;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import es.tid.tedb.DomainTEDB;

public class ReservationManager {
	
	//private Timer timer;
	
	ScheduledThreadPoolExecutor executor;
	
	private DomainTEDB ted;
	private Logger log;
	
	private long reservationID;
	
	private Hashtable<Long,DeleteReservationTask> permanentReservationList;
	
	private Hashtable<Long,ScheduledFuture> scheduledReservationList;
	
	public ReservationManager(DomainTEDB ted){
		executor =new ScheduledThreadPoolExecutor(1);
		this.ted=ted;
		log=Logger.getLogger("PCEServer");
		reservationID=0;
		permanentReservationList=new Hashtable<Long,DeleteReservationTask>();
	}
	
	/**
	 * 
	 * @param time Time in ms of the reservation
	 */
	public long reserve(LinkedList<Object> sourceVertexList, LinkedList<Object> targetVertexList, int wavelength, long time, boolean bidirectional){
		log.info("Reserving lambda "+wavelength+" for "+time+" miliseconds");
		//SimpleDirectedWeightedGraph<Object,FiberLinkEdge> networkGraph=ted.getNetworkGraph();
		ted.notifyWavelengthReservation(sourceVertexList, targetVertexList, wavelength, bidirectional);
		DeleteReservationTask drt= new DeleteReservationTask();
		drt.setSourceVertexList(sourceVertexList);
		drt.setTargetVertexList(targetVertexList);
		drt.setWavelength(wavelength);
		drt.setBidirectional(bidirectional);
		drt.setTed(ted);
		long idReservation=getReservationID();
		if (time>=0xFFFFFFFFL){
			log.info("Permanent Reservation");
			permanentReservationList.put(idReservation, drt);
			
		}else{
			//timer.schedule(drt, time);
			log.info("Something will be reserved");
			ScheduledFuture<?> sf=executor.schedule(drt, time, TimeUnit.MILLISECONDS);
			//scheduledReservationList.put(idReservation, sf);
		}
		return idReservation;	
		
	}
	public long reserve( LinkedList<Object> sourceVertexList, LinkedList<Object> targetVertexList, LinkedList<Integer> wlans, long time, boolean bidirectional, int m){
		//log.info("Reserving lambda "+wavelength+" for "+time+" miliseconds");
		ted.notifyWavelengthReservationWLAN(sourceVertexList, targetVertexList, wlans, bidirectional);
		
		DeleteReservationTask drt= new DeleteReservationTask();
		drt.setSourceVertexList(sourceVertexList);
		drt.setTargetVertexList(targetVertexList);
		drt.setM(m);
		drt.setWLANs(wlans);
		drt.setBidirectional(bidirectional);
		drt.setTed(ted);
		long idReservation=getReservationID();
		if (time>=0xFFFFFFFFL){
			log.info("Permanent Reservation");
			permanentReservationList.put(idReservation, drt);
			
		}else{
			log.info("Derreservar lambda en:"+time);
			//timer.schedule(drt, time);
			ScheduledFuture<?> sf=executor.schedule(drt, time, TimeUnit.MILLISECONDS);
			//scheduledReservationList.put(idReservation, sf);
		}
		return idReservation;	
		
	}
	public long reserve( LinkedList<Object> sourceVertexList, LinkedList<Object> targetVertexList, int wavelength, long time, boolean bidirectional, int m){
		//log.info("Reserving lambda "+wavelength+" for "+time+" miliseconds");
		ted.notifyWavelengthReservationSSON(sourceVertexList, targetVertexList, wavelength, bidirectional, m);
		
		DeleteReservationTask drt= new DeleteReservationTask();
		drt.setSourceVertexList(sourceVertexList);
		drt.setTargetVertexList(targetVertexList);
		drt.setM(m);
		drt.setWavelength(wavelength);
		drt.setBidirectional(bidirectional);
		drt.setTed(ted);
		long idReservation=getReservationID();
		if (time>=0xFFFFFFFFL){
			log.info("Permanent Reservation");
			permanentReservationList.put(idReservation, drt);
			
		}else{
			log.info("Derreservar lambda en:"+time);
			//timer.schedule(drt, time);
			ScheduledFuture<?> sf=executor.schedule(drt, time, TimeUnit.MILLISECONDS);
			//scheduledReservationList.put(idReservation, sf);
		}
		return idReservation;	
		
	}
	public void cancelReservation(long idReservation){
		DeleteReservationTask drt=permanentReservationList.remove(idReservation);
		//executor.execute(drt);
		if (drt!=null){
			drt.run();	
		}
	
	}
	
	//public void permanentRe
	
	public synchronized long getReservationID(){
		reservationID+=1;
		return reservationID;
	}
	
	public void cancelAllReservations(){
		Enumeration<DeleteReservationTask> enu=permanentReservationList.elements();
		
		
		while (enu.hasMoreElements()){
			DeleteReservationTask drt= enu.nextElement();
			if (drt!=null){
				executor.execute(drt);	
			}			
		}
		permanentReservationList.clear();
	}
	
	public long getReservationQueueSize(){
		return permanentReservationList.size();
	}

	public DomainTEDB getTed() {
		return ted;
	}

	public void setTed(DomainTEDB ted) {
		this.ted = ted;
	}
	
}
