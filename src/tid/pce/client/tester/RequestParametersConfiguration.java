package tid.pce.client.tester;

import java.util.LinkedList;

import es.tid.pce.pcep.objects.Metric;

public class RequestParametersConfiguration {
	boolean isReservation = false;
	boolean of = false ;
	int ofCode;
	boolean delayMetric=false;
	private LinkedList<Metric> metricList;
	private float bw;
	private boolean bandwidth = false;
	private long timeReserved;
	boolean bidirectional=false;
	boolean isVariableBandwidth=false;

	int priority;
	boolean reoptimization;
	boolean loose;
	
	
	public boolean isBidirectional() {
		return bidirectional;
	}
	public void setBidirectional(boolean bidirectional) {
		this.bidirectional = bidirectional;
	}
	public int getPriority() {
		return priority;
	}
	public void setPriority(int priority) {
		this.priority = priority;
	}
	public boolean isReoptimization() {
		return reoptimization;
	}
	public void setReoptimization(boolean reoptimization) {
		this.reoptimization = reoptimization;
	}
	public boolean isLoose() {
		return loose;
	}
	public void setLoose(boolean loose) {
		this.loose = loose;
	}
	public LinkedList<Metric> getMetricList() {
		return metricList;
	}
	public void setMetricList(LinkedList<Metric> metricList) {
		this.metricList = metricList;
	}
	public float getBW() {
		return bw;
	}
	public void setBW(float bw) {
		this.bw = bw;
	}
	public long getTimeReserved() {
		return timeReserved;
	}
	public boolean isVariableBandwidth() {
		return isVariableBandwidth;
	}
	public void setVariableBandwidth(boolean isVariableBanwidth) {
		this.isVariableBandwidth = isVariableBanwidth;
	}
	public boolean Is_bandwidth() {
		return bandwidth;
	}
	public void setBandwidth(boolean Bandwidth) {
		bandwidth = Bandwidth;
	}
	public void setTimeReserved(long timeReserved) {
		this.timeReserved = timeReserved;
	}
	public boolean isOf() {
		return of;
	}
	public void setOf(boolean of) {
		this.of = of;
	}
	public boolean isReservation() {
		return isReservation;
	}
	public void setReservation(boolean isReservation) {
		this.isReservation = isReservation;
	}

	public int getOfCode() {
		return ofCode;
	}
	public void setOfCode(int ofCode) {
		this.ofCode = ofCode;
	}
	public boolean isDelayMetric() {
		return delayMetric;
	}
	public void setDelayMetric(boolean delayMetric) {
		this.delayMetric = delayMetric;
	}
	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return "bidirectional:"+bidirectional;
	}
	
	
	
}
