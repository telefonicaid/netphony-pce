package tid.pce.computingEngine.algorithms.wlan;


import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.logging.Logger;

import tid.pce.computingEngine.ComputingResponse;
import tid.pce.computingEngine.algorithms.AlgorithmReservation;
import tid.pce.pcep.objects.Reservation;
import tid.pce.pcep.objects.ReservationConf;
import tid.pce.server.wson.ReservationManager;

/**
 * Similar class GenericLambdaReservation but for WLANs
 * @author jaume
 *
 */

public class GenericWLANReservation implements AlgorithmReservation
{

	private ComputingResponse resp;
	private LinkedList<Object> sourceVertexList=new LinkedList<Object>();
	private LinkedList<Object> targetVertexList=new LinkedList<Object>();
	private LinkedList<Integer> WLANList = new LinkedList<Integer>();
	private Reservation reservation;
	private Logger log;
	private ReservationManager reservationManager;
	private boolean bidirectional;
	
	public boolean isBidirectional() 
	{
		return bidirectional;
	}

	public void setBidirectional(boolean bidirectional) 
	{
		this.bidirectional = bidirectional;
	}

	public GenericWLANReservation(){
		log=Logger.getLogger("PCEServer");
	}
	
	public ComputingResponse call() throws Exception 
	{
		if (reservation!=null){
			
			long reservationID = 0;
			
			Hashtable<Integer,ReserveStruct> hTable = new Hashtable<Integer,ReserveStruct>();
			
			for (int i = 0 ; i < sourceVertexList.size() ; i++ )
			{
				log.info("Reserving lambda: "+WLANList.get(i));
				
				if (hTable.get(WLANList.get(i)) == null)
				{
					ReserveStruct reserve = new ReserveStruct();
					reserve.sourceVertexList.add(sourceVertexList.get(i));
					reserve.targetVertexList.add(targetVertexList.get(i));
					reserve.wlan = WLANList.get(i);
				}
				else
				{
					ReserveStruct reserve = hTable.get(WLANList.get(i));
					reserve.sourceVertexList.add(sourceVertexList.get(i));
					reserve.targetVertexList.add(targetVertexList.get(i));
				}
			}

			Enumeration<Integer> enumKey = hTable.keys();
			while(enumKey.hasMoreElements()) 
			{
			    Integer key = enumKey.nextElement();
			    ReserveStruct val = hTable.get(key);			    
			    reservationManager.reserve(val.sourceVertexList, val.targetVertexList, val.wlan, reservation.getTimer(), this.bidirectional);
			    log.info("Reservin value for WLAN:"+val.wlan);
			}
			
			
			ReservationConf resConf= new ReservationConf();
			resConf.setReservationID(reservationID);
			resp.getResponse(0).setResConf(resConf);
			return resp;
		}
		else 
		{
			return null;	
		}
			
	}

	public ComputingResponse getResp() 
	{
		return resp;
	}

	public void setResp(ComputingResponse resp) 
	{
		
		this.resp = resp;
	}

	public LinkedList<Object> getSourceVertexList() 
	{
		return sourceVertexList;
	}

	public void setSourceVertexList(LinkedList<Object> sourceVertexList) 
	{
		this.sourceVertexList = sourceVertexList;
	}

	public LinkedList<Object> getTargetVertexList()
	{
		return targetVertexList;
	}

	public void setTargetVertexList(LinkedList<Object> targetVertexList) 
	{
		this.targetVertexList = targetVertexList;
	}
	
	public void setWLANList(LinkedList<Integer> WLANList) 
	{
		this.WLANList = WLANList;
	}

	public Reservation getReservation() 
	{
		return reservation;
	}

	public void setReservation(Reservation reservation)
	{
		this.reservation = reservation;
	}


	public ReservationManager getReservationManager() 
	{
		return reservationManager;
	}

	public void setReservationManager(ReservationManager reservationManager)
	{
		this.reservationManager = reservationManager;
	}
	
	private class ReserveStruct
	{
		private LinkedList<Object> sourceVertexList=new LinkedList<Object>();
		private LinkedList<Object> targetVertexList=new LinkedList<Object>();
		int wlan;
	}

}
