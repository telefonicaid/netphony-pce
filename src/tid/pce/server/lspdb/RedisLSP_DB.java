package tid.pce.server.lspdb;

import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

import redis.clients.jedis.Jedis;
import tid.emulator.node.transport.lsp.LSPKey;
import tid.emulator.node.transport.lsp.te.LSPTE;
import tid.pce.pcep.constructs.StateReport;
import tid.pce.pcep.messages.PCEPReport;
import tid.pce.pcep.objects.LSP;
import tid.pce.pcep.objects.MalformedPCEPObjectException;
import tid.pce.pcep.objects.OPEN;
import tid.pce.pcep.objects.tlvs.LSPDatabaseVersionTLV;

import com.google.gson.Gson;
/**
 * LSP_DB implementation.
 * 
 * This database will save all delegated LSP to this PCE.
 * 
 * @author jaume
 */

public class RedisLSP_DB implements LSP_DB
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
	
	private Hashtable<LSPKey, LSP> LSPList;
	
	private Logger log;
	
	private Jedis jedis;
	
	private String dbId;
	
	AtomicLong DBVersion;
	
	public String getPCCListKey()
	{
		return dbId+"_PCC";
	}
	
	public String getPCCLSPListKey(String PCCId)
	{
		return dbId+"_"+PCCId+"_LSP";
	}
	
	public RedisLSP_DB(String id)
	{


		
   	 	
   	 	dbId = id;
		log = Logger.getLogger("PCEPParser");
		PCCList = new Hashtable<Inet4Address, PCCInfo>();
		LSPList = new Hashtable<LSPKey, LSP>();
		DBVersion = new AtomicLong();
		PCCListLock = new ReentrantLock();
		
   	 	jedis = new Jedis("10.95.164.222",6379);
   	 	jedis.connect();		
   	 	if (jedis.isConnected())
   	 	{
   	 		log.info("redis: connection stablished");
   	 		fillFromDB();
   	 	}
	}
	
	
	public void fillFromDB() 
	{
		log.info("redis: filling from DB");
   	 	if (jedis.isConnected())
   	 	{
   	 		log.info("redis: connection stablished");
   	 		Set<String> PCCs = jedis.hkeys(getPCCListKey());
   	 		for (Iterator iterator = PCCs.iterator(); iterator.hasNext();) {
				String PCCId = ((String) iterator.next());
				log.info("redis: PCC found: "+PCCId);
				try {
					Inet4Address address =(Inet4Address) Inet4Address.getByName(PCCId.replace("/", ""));
					List<String> LSPs = jedis.hvals(getPCCLSPListKey(PCCId));
					
					int PCCDBVersion = Integer.parseInt(jedis.hget(getPCCListKey(),PCCId));//= LSPs.size();
					
					
					
					log.info("redis: found PCC: "+PCCId+" with db version: "+PCCDBVersion +"==? "+LSPs.size());
					//TODO: issyncover??
					addPCC(address,false,PCCDBVersion);
					
					for (Iterator iterator2 = LSPs.iterator(); iterator2.hasNext();) {
						String LSPString = (String) iterator2.next();
						log.info("redis: found LSP: "+LSPString);
						//TODO: transformar y meter
						Gson gson = new Gson();
						byte[] lspBytes = gson.fromJson(LSPString,byte[].class);
						try {
							LSP lsp = new LSP(lspBytes,0);
							log.info("redis: LSP:::with Id"+lsp.getLspId()+" flags: "+lsp.getObjectClass()+" "+lsp.issFlag());
							
							LSPList.put(new LSPKey(address,lsp.getLspId()),lsp);
							
						} catch (MalformedPCEPObjectException e) {
							e.printStackTrace();
						}
					}				
					
					
					
				} catch (UnknownHostException e) {
					e.printStackTrace();
				}	
			}
   	 	}
   	 	else
   	 	{
   	 		log.info("redis: couldn't stablish connection, aborting...");
   	 		//TODO: terminar de alguna forma
   	 	}
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
		
		log.info("redis: added new PCC");
		/***REDIS***/
		jedis.hset(getPCCListKey(), adrss.toString(), Long.toString(dataBaseVersion));
		
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
			lsp.encode();
			byte[] lspBytes = lsp.getBytes();
			
			
			
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
				
				/*********REDIS*********/
				jedis.hset(getPCCListKey(), adress.toString(), Long.toString(lspDB.getLSPStateDBVersion()));
				
				log.info("redis: updated database version of PCC: "+PCCList.get(adress)+" to v."+lspDB.getLSPStateDBVersion());
			}
			
			LSP lspFromDB = LSPList.get(new LSPKey(adress,lspId));
			
			boolean rFlag = lsp.isrFlag();
			
			if ((lspFromDB!=null)&&(rFlag == false))
			{
				log.info("Overriding previous information from database");
				LSPList.put(new LSPKey(adress,lspId), lsp);
				
				/******REDIS*************/
				Gson gson = new Gson();
				String lspInfoString = gson.toJson(lspBytes);
				
				jedis.hset(getPCCLSPListKey(adress.toString()),Integer.toString(lspId),lspInfoString);
				
			}
			else
			{
				if (rFlag)
				{
					LSPList.remove(new LSPKey(adress,lspId));
					
					/****REDIS*********/
					log.info("redis: Removing from database lsp with id "+lspId +" and adress "+adress);				
					jedis.hdel(getPCCLSPListKey(adress.toString()), Integer.toString(lspId));
				}
				else
				{
					log.info("Really Adding PCEPReport to database");
					log.info("Address"+adress);
					log.info("lspId"+lspId);
					//log.info();
					LSPList.put(new LSPKey(adress,lspId),lsp);
					
					
					/****REDIS*****/
					Gson gson = new Gson();
					String lspInfoString = gson.toJson(lspBytes);					
					log.info("redis: adding PCEReport");					
					jedis.hset(getPCCLSPListKey(adress.toString()),Integer.toString(lspId),lspInfoString);					
				}
			}
		}
	}
	
	@Override
	synchronized public void proccessOpen(OPEN open, Inet4Address address) 
	{
		log.info("PCC database sync");
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
		return null;
	}

	public Hashtable<LSPKey, LSP> getLSPList() 
	{
		return LSPList;
	}

	public void setLSPTEList(Hashtable<LSPKey, LSP> lSPTEList) 
	{
		LSPList = lSPTEList;
	}

	public Jedis getJedis() {
		return jedis;
	}

	public void setJedis(Jedis jedis) {
		this.jedis = jedis;
	}

	public String getDbId() {
		return dbId;
	}

	public void setDbId(String dbId) {
		this.dbId = dbId;
	}

	
}
