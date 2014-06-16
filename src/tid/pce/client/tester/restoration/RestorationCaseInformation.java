package tid.pce.client.tester.restoration;

import java.net.Inet4Address;

public class RestorationCaseInformation {
	private Inet4Address source;
	private Inet4Address destination;
	private long timeToWait;
	public Inet4Address getSource() {
		return source;
	}
	public void setSource(Inet4Address source) {
		this.source = source;
	}
	public Inet4Address getDestination() {
		return destination;
	}
	public void setDestination(Inet4Address destination) {
		this.destination = destination;
	}
	public long getTimeToWait() {
		return timeToWait;
	}
	public void setTimeToWait(long timeToWait) {
		this.timeToWait = timeToWait;
	}


}
