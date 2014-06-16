package tid.pce.server.lspdb;

import java.net.Inet4Address;
import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

import tid.emulator.node.transport.lsp.LSPKey;
import tid.emulator.node.transport.lsp.te.LSPTE;
import tid.pce.pcep.constructs.StateReport;
import tid.pce.pcep.messages.PCEPReport;
import tid.pce.pcep.objects.LSP;
import tid.pce.pcep.objects.OPEN;
import tid.pce.pcep.objects.tlvs.LSPDatabaseVersionTLV;

/**
 * LSP_DB implementation.
 * 
 * This database will save all delegated LSP to this PCE.
 * 
 * @author jaume
 */

public class SimpleLSP_DB implements LSP_DB
{
	
	
	
	/**
	 * List of LSPs 
	 */
		
	/*
	 * I don't know why LSPKey is used instead of long.
	 * I'm doing it this way because this is the way it's done in
	 * tid.emulator.node.transport.lsp.LSPManager
	 */
		
	
	private ReentrantLock PCCListLock;
	private Hashtable<Inet4Address, PCCInfo> PCCList;
	
	private Hashtable<LSPKey, LSPTEInfo> LSPTEList;
	
	private Logger log;
	
	
	AtomicLong DBVersion;
	
	public SimpleLSP_DB()
	{
		log = Logger.getLogger("PCEPParser");
		PCCList = new Hashtable<Inet4Address, PCCInfo>();
		LSPTEList = new Hashtable<LSPKey, LSPTEInfo>();
		DBVersion = new AtomicLong();
		PCCListLock = new ReentrantLock();
	}
	
	@Override
	synchronized public long getDBIdentifier(Inet4Address address)
	{
		PCCInfo info = PCCList.get(address);
		if (info == null)
		{
			return 0;
		}
		else
		{
			return info.dataBaseVersion;
		}
	}

	@Override
	synchronized public void addPCC(Inet4Address adrss, boolean isSyncOver ,long dataBaseVersion) 
	{
		PCCList.put(adrss, new PCCInfo(isSyncOver, dataBaseVersion));
	}
	
	@Override
	synchronized public long getPCCDatabaseVersion(Inet4Address adrss) 
	{
		log.info("PCCList :"+PCCList);
		PCCInfo info =  PCCList.get(adrss);
		if (info == null)
		{
			return 0;
		}
		else
		{
			return info.dataBaseVersion;
		}
	}
	
	@Override
	synchronized public Boolean isPCCSyncOver(Inet4Address adrss) 
	{
		PCCInfo info = PCCList.get(adrss);
		if (info == null)
		{
			log.warning("This should never be executed");
			return false;
		}
		else
		{
			return PCCList.get(adrss).isSyncOver;
		}
	}
	
	@Override
	synchronized public void setPCCSyncOver(Inet4Address adrss) 
	{
		PCCInfo info = PCCList.get(adrss);
		info.isSyncOver = true;
	}
	@Override
	synchronized public void addLSP(LSPTE lsp) 
	{
		log.info("Adding New LSP with id "+lsp.getIdLSP());
    	log.warning("Uninplemented method");
		//LSPList.put(new LSPKey(lsp.getIdSource(), lsp.getIdLSP()), lsp);
	}
	
	@Override
	synchronized public void addMessageToDatabase(PCEPReport pcepReport) 
	{
		log.info("Adding PCEPReport to database,lsps:"+pcepReport.getStateReportList().size());
		for (int i = 0; i < pcepReport.getStateReportList().size(); i++)
		{
			StateReport stateReport = pcepReport.getStateReportList().get(i);
			LSP lsp = stateReport.getLSP();
			int lspId = new Long(lsp.getLspId()).intValue();
			
			if (lspId == 0)
			{
				log.info("Trying to add an LSP with id 0. This is normal in sync");
				return;
			}
			
			Inet4Address adress = lsp.getLspIdentifiers_tlv().getTunnelSenderIPAddress();
			
			PCCInfo pccInfo = PCCList.get(adress);
			
			LSPDatabaseVersionTLV lspDB;
			if ((lspDB = lsp.getLspDBVersion_tlv())!=null)
			{
				pccInfo.dataBaseVersion = lspDB.getLSPStateDBVersion();
				log.info("updated database version of PCC: "+PCCList.get(pccInfo)+" to v."+lspDB.getLSPStateDBVersion());
			}
			
			LSPTEInfo lspInfo = LSPTEList.get(new LSPKey(adress,lspId));
			
			boolean rFlag = lsp.isrFlag();
			
			if ((lspInfo!=null)&&(rFlag == false))
			{
				log.info("Overriding previous information from database");
				lspInfo.pcepReport = pcepReport;
			}
			else
			{
				if (rFlag)
				{
					log.info("Removing from database lsp with id "+lspId +" and adress "+adress);
					LSPTEList.remove(new LSPKey(adress,lspId));
				}
				else
				{
					log.info("Really Adding PCEPReport to database");
					log.info("Address"+adress);
					log.info("lspId"+lspId);
					//log.info();
					LSPTEList.remove(new LSPKey(adress,lspId));
					lspInfo = new LSPTEInfo(pcepReport);
					
					LSPTEList.put(new LSPKey(adress,lspId),lspInfo);
				}
			}
		}
	}
	
	@Override
	synchronized public void proccessOpen(OPEN open, Inet4Address address) 
	{
		long dataBaseId = open.getLsp_database_version_tlv().getLSPStateDBVersion();
		PCCInfo info = PCCList.get(address);
		if ((info == null) || (info.dataBaseVersion != dataBaseId))
		{
			addPCC(address,false,dataBaseId);
		}
		else
		{
			log.info("Avoiding sync???");
			info.isSyncOver = true;
		}
	}
	
	public class LSPTEInfo
	{
		public PCEPReport pcepReport;
		LSPTEInfo( PCEPReport pcepReport)
		{
			this.pcepReport = pcepReport;
		}
	}
	
	private class PCCInfo
	{
		private boolean isSyncOver;
		private long dataBaseVersion;
		
		PCCInfo(boolean isSyncOver, long dataBaseVersion)
		{
			this.isSyncOver = isSyncOver;
			this.dataBaseVersion = dataBaseVersion;
		}
	}
	
	

	@Override
	public LSPTE getLSP(LSPKey keyLSP)
	{
		// TODO Auto-generated method stub
		return null;
	}

	public Hashtable<LSPKey, LSPTEInfo> getLSPTEList() 
	{
		return LSPTEList;
	}

	public void setLSPTEList(Hashtable<LSPKey, LSPTEInfo> lSPTEList) 
	{
		LSPTEList = lSPTEList;
	}

	
}
