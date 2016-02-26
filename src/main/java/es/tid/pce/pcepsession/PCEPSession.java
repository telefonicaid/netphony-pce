package es.tid.pce.pcepsession;

import es.tid.pce.pcep.messages.PCEPMessage;

public interface PCEPSession {
	
	/**
	 * Send close message and finish the PCEP Session
	 * @param reason
	 */
	public void close(int reason);
	/**
	 * Finish the PCEP Session abruptly, 
	 */
	public void killSession();
	/**
	 * Encodes and sends PCEP Message
	 * If the message is bad encoded, the session is closed
	 */
	public void sendPCEPMessage(PCEPMessage message);

}
