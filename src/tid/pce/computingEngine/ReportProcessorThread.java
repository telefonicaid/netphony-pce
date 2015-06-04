package tid.pce.computingEngine;

/**
 * Class made for managing report messages from PCCs. It only adds them to
 * the database
 * 
 * @author jimbo
 */

import java.net.Inet4Address;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

import es.tid.pce.pcep.messages.PCEPReport;
import tid.pce.server.PCEServerParameters;
import tid.pce.server.lspdb.ReportDB_Handler;

public class ReportProcessorThread extends Thread
{
	boolean running;
	
	
	LinkedBlockingQueue<PCEPReport> reportMessageQueue;
	
	
	ReportDB_Handler lspDB;
		
	Logger log;
		
	public ReportProcessorThread( LinkedBlockingQueue<PCEPReport> reportMessageQueue, ReportDB_Handler lspDB) 
	{
		log=Logger.getLogger("PCEServer");
		running = true;
		this.lspDB = lspDB;
		this.reportMessageQueue = reportMessageQueue;
	}
	
	public void run()
	{
		PCEPReport report_m;
		while (running)
		{
			try
			{
				report_m=reportMessageQueue.take();
				effectivelyDispatch(report_m);
			}
			catch(InterruptedException e)
			{
				log.warning("Interrupted Exception Captured in ReportProcessorThread");
				e.printStackTrace();
				break;
			}
			
		}
	}
	
	public void effectivelyDispatch(PCEPReport pcepReport)
	{
		log.info("Received new report message: "+pcepReport.toString());
		log.info("whith ID :"+pcepReport.getStateReportList().get(0).getLSP().getLspId());
		Inet4Address addres = pcepReport.getStateReportList().get(0).getLSP().getLspIdentifiers_tlv().getTunnelSenderIPAddress();
		
		Boolean isSyncOver = false;
		
		log.info("Size LSP:"+pcepReport.getStateReportList().size());
		//isSyncOver = lspDB.isPCCSyncOver(pcepReport.getStateReportList().get(0).getLSP().getLspIdentifiers_tlv().getTunnelSenderIPAddress());
		
		log.info("Package received from adress: "+pcepReport.getStateReportList().get(0).getLSP().getLspIdentifiers_tlv().getTunnelSenderIPAddress());
		
		
		if (!isSyncOver)
		{
			boolean syncFlag;
			int numLSPs = pcepReport.getStateReportList().size();
			for (int j = 0; j < numLSPs ; j++)
			{
				syncFlag = pcepReport.getStateReportList().get(j).getLSP().issFlag();
				if ((!syncFlag)&&(pcepReport.getStateReportList().get(j).getLSP().getLspId()==0))
				{
					isSyncOver = true;
					log.info("Sync is over");
		//			lspDB.setPCCSyncOver(addres);
				}
			}
		}
		
		for (int i = 0; i < pcepReport.getStateReportList().size(); i++)
		{			
			lspDB.processReport(pcepReport);
		}
	}
}
