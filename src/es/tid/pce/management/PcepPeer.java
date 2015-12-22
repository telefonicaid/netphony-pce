package es.tid.pce.management;

import java.net.Inet4Address;

/**
 *  +--ro discontinuity-time?         yang:timestamp
                 +--ro initiate-session?           boolean
                 +--ro session-exists?             boolean
                 +--ro num-sess-setup-ok?          yang:counter32
                 +--ro num-sess-setup-fail?        yang:counter32
                 +--ro session-up-time?            yang:timestamp
                 +--ro session-fail-time?          yang:timestamp
                 +--ro session-fail-up-time?       yang:timestamp
                 +--ro avg-rsp-time?               uint32
                 +--ro lwm-rsp-time?               uint32
                 +--ro hwm-rsp-time?               uint32
                 +--ro num-pcreq-sent?             yang:counter32
                 +--ro num-pcreq-rcvd?             yang:counter32
                 +--ro num-pcrep-sent?             yang:counter32
                 +--ro num-pcrep-rcvd?             yang:counter32
                 +--ro num-pcerr-sent?             yang:counter32
                 +--ro num-pcerr-rcvd?             yang:counter32
                 +--ro num-pcntf-sent?             yang:counter32
                 +--ro num-pcntf-rcvd?             yang:counter32
                 +--ro num-keepalive-sent?         yang:counter32
                 +--ro num-keepalive-rcvd?         yang:counter32
                 +--ro num-unknown-rcvd?           yang:counter32
                 +--ro num-corrupt-rcvd?           yang:counter32
                 +--ro num-req-sent?               yang:counter32
                 +--ro num-svec-sent?              yang:counter32
                 +--ro num-svec-req-sent?          yang:counter32
                 +--ro num-req-sent-pend-rep?      yang:counter32
                 +--ro num-req-sent-ero-rcvd?      yang:counter32
                 +--ro num-req-sent-nopath-rcvd?   yang:counter32
                 +--ro num-req-sent-cancel-rcvd?   yang:counter32
                 +--ro num-req-sent-error-rcvd?    yang:counter32
                 +--ro num-req-sent-timeout?       yang:counter32

                 +--ro num-req-sent-cancel-sent?   yang:counter32
                 +--ro num-req-rcvd?               yang:counter32
                 +--ro num-svec-rcvd?              yang:counter32
                 +--ro num-svec-req-rcvd?          yang:counter32
                 +--ro num-req-rcvd-pend-rep?      yang:counter32
                 +--ro num-req-rcvd-ero-sent?      yang:counter32
                 +--ro num-req-rcvd-nopath-sent?   yang:counter32
                 +--ro num-req-rcvd-cancel-sent?   yang:counter32
                 +--ro num-req-rcvd-error-sent?    yang:counter32
                 +--ro num-req-rcvd-cancel-rcvd?   yang:counter32
                 +--ro num-rep-rcvd-unknown?       yang:counter32
                 +--ro num-req-rcvd-unknown?       yang:counter32
                 +--ro num-req-sent-closed?        yang:counter32
                 +--ro num-req-rcvd-closed?        yang:counter32
 * @author ogondio
 *
 */

public class PcepPeer {
	
	private Inet4Address addr;
	
	private boolean sessionExists=false;
	
	private int num_sess_setup_ok=0;
	
	private int num_sess_setup_fail=0;

	public Inet4Address getAddr() {
		return addr;
	}

	public void setAddr(Inet4Address addr) {
		this.addr = addr;
	}

	public boolean isSessionExists() {
		return sessionExists;
	}

	public void setSessionExists(boolean sessionExists) {
		this.sessionExists = sessionExists;
	}
	
	public void notifyNewSessSetupOK(){
		num_sess_setup_ok+=1;
	}
	
	public void notifyNewSessSetupFail(){
		num_sess_setup_fail+=1;
	}
	
	public String toString(){
		return "\n" + addr.getHostAddress();
	}
	
	public String fullInfo(){
		return "\n"+addr.getHostAddress() +" -----> " + "num_sess_setup_ok: "+num_sess_setup_ok
				+ "   num_sess_setup_fail: "+num_sess_setup_fail;
	}
}
