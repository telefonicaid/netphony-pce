package tid.pce.parentPCE;

import java.net.Inet4Address;

import tid.pce.pcep.objects.Notification;

public class MultiDomainUpdate {
	
	private Inet4Address PCEID;
	
	private Inet4Address domainID;
	
	private Notification notif;

	public Inet4Address getPCEID() {
		return PCEID;
	}

	public void setPCEID(Inet4Address pCEID) {
		PCEID = pCEID;
	}

	public Inet4Address getDomainID() {
		return domainID;
	}

	public void setDomainID(Inet4Address domainID) {
		this.domainID = domainID;
	}

	public Notification getNotif() {
		return notif;
	}

	public void setNotif(Notification notif) {
		this.notif = notif;
	}
	
	

}
