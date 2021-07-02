package es.tid.pce.computingEngine;

import java.io.DataOutputStream;

import es.tid.pce.pcep.messages.PCEPReport;

public class ReportProcessTask {
	
	/**
	 * DataOutputStream to send the response to the peer PCC
	 */

	private DataOutputStream out=null; 
	
	private PCEPReport reportMessage;

	public ReportProcessTask() {
		// TODO Auto-generated constructor stub
	}

	public DataOutputStream getOut() {
		return out;
	}

	public void setOut(DataOutputStream out) {
		this.out = out;
	}

	public PCEPReport getReportMessage() {
		return reportMessage;
	}

	public void setReportMessage(PCEPReport reportMessage) {
		this.reportMessage = reportMessage;
	}
	
	

}
