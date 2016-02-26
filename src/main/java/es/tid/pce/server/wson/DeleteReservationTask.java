package es.tid.pce.server.wson;

import java.util.LinkedList;
import java.util.logging.Logger;

import es.tid.tedb.DomainTEDB;

public class DeleteReservationTask implements Runnable {
	
	private int wavelength;
	boolean bidirectional;
	private int m = 0;
	boolean isMultipleLambdas;
	private Logger log;
	private LinkedList<Integer> wlans;
	private boolean isWLAN = false;
	
	public boolean isBidirectional() {
		return bidirectional;
	}

	public void setBidirectional(boolean bidirectional) {
		this.bidirectional = bidirectional;
	}

	private LinkedList<Object> sourceVertexList;
	private LinkedList<Object> targetVertexList;
	
	
	private DomainTEDB ted; 

	@Override
	public void run() {
		log=Logger.getLogger("PCEServer");
		if (isWLAN)
		{
			
		}
		else if (isMultipleLambdas()==false){
			ted.notifyWavelengthEndReservation(sourceVertexList, targetVertexList, wavelength, bidirectional);
		}else{
			ted.notifyWavelengthEndReservationSSON(sourceVertexList, targetVertexList, wavelength, bidirectional, m);
		}
	}

	public int getWavelength() {
		return wavelength;
	}

	public void setWavelength(int wavelength) {
		this.wavelength = wavelength;
		
	}

	public LinkedList<Object> getSourceVertexList() {
		return sourceVertexList;
	}

	public void setSourceVertexList(LinkedList<Object> sourceVertexList) {
		this.sourceVertexList = sourceVertexList;
	}

	public LinkedList<Object> getTargetVertexList() {
		return targetVertexList;
	}

	public void setTargetVertexList(LinkedList<Object> targetVertexList) {
		this.targetVertexList = targetVertexList;
	}

	public DomainTEDB getTed() {
		return ted;
	}

	public void setTed(DomainTEDB ted) {
		this.ted = ted;
	}
	
	public void setWLANs(LinkedList<Integer> wlans) {
		this.wlans = wlans;
		this.isWLAN = true;
	}

	public boolean isMultipleLambdas() {
		if (m==0){
			this.isMultipleLambdas = false;
	}else{
			this.isMultipleLambdas = true;
		}
		return isMultipleLambdas;
	}

	public void setM(int m) {
		this.m = m;
	}
	
	

}
