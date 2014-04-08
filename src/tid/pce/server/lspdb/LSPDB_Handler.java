package tid.pce.server.lspdb;

import java.net.Inet4Address;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Logger;

import redis.clients.jedis.Jedis;
import tid.pce.pcep.constructs.StateReport;
import tid.pce.pcep.messages.PCEPReport;
import tid.pce.pcep.objects.LSP;
import tid.pce.pcep.objects.OPEN;


public class LSPDB_Handler {
	protected Hashtable<String, LSPDB> moduleList;

	private Logger log;

	private Jedis jedis;

	private String dbHost="";	
	private boolean dbActive = false;

	protected String handlerId="";

	public static final String DEF_MODULE = "DEFAULT";

	public LSPDB_Handler ()
	{   

		log = Logger.getLogger("PCEPParser");
		moduleList = new Hashtable<String, LSPDB>();
	}


	public LSPDB_Handler(String handlerId, String dbHost)
	{
		this();
		this.dbHost = dbHost;
		this.handlerId = handlerId;

		jedis = new Jedis(dbHost,6379);
		jedis.connect();		
		if (jedis.isConnected())
		{
			this.setDbActive(true);			
			log.info("redis: connection stablished");
		}	
	}

	public void fillFromDB(String handlerId, String dbHost)
	{
		Jedis j = new Jedis(dbHost);
		log.info("redis: filling from db host="+dbHost+" id="+handlerId);
		if (j.isConnected())
		{

			Set<String> modules = j.keys(getModuleList(handlerId));
			log.info("modules: "+modules.toString());
			for (Iterator iterator = modules.iterator(); iterator.hasNext();) 
			{
				String modId = (String) iterator.next();	
				log.info("redis: module found: "+modId);
				LSPDB_Redis lspdb = new LSPDB_Redis(modId,dbHost);
				lspdb.fillFromDB();
				moduleList.put(modId, lspdb);
			}
		}
		else
		{
			log.info("redis: couldn't establish connection to fill from db");
		}
	}

	public void fillFromDB()
	{
		if (handlerId.length()==0 || dbHost.length()==0 || !dbActive || !jedis.isConnected())
		{
			log.info("redis: couldn't fill from db, check your configuration");
			return;
		}
		Set<String> modules = jedis.keys(getModuleList(handlerId));
		log.info("redis: modules: "+modules.toString());
		for (Iterator iterator = modules.iterator(); iterator.hasNext();) 
		{
			String modId = ((String) iterator.next()).replace("_LSP","");	
			log.info("redis: module found: "+modId);
			LSPDB_Redis lspdb = new LSPDB_Redis(modId,dbHost);
			lspdb.fillFromDB();
			moduleList.put(modId, lspdb);
		}		
	}

	public void fillFromXML()
	{
		//TODO: por hacer
	}

	public String getModuleList(String handlerId)
	{
		return handlerId+"_*_LSP";
	}

	public String getLSPDBList(Inet4Address ad)
	{
		return handlerId+"_"+ad.toString();
	}

	public String getModuleList()
	{
		return handlerId+"_"+"MODULES";
	}

	public LSPDB getLSPDB(String key)
	{
		return moduleList.get(key);
	}

	public void setLSPDB(String key, LSPDB lspdb)
	{
		moduleList.put(key, lspdb);
	}


	synchronized public void processReport(PCEPReport pcepReport)
	{
		log.info("Adding PCEPReport to database,lsps:"+pcepReport.getStateReportList().size());


		for (int i = 0; i < pcepReport.getStateReportList().size(); i++)
		{
			StateReport stateReport = pcepReport.getStateReportList().get(i);
			LSP lsp = stateReport.getLSP();
			if (lsp.getLspId() ==0)
			{
				log.info("sync lsp received, ignoring..");
				return; 
			}
			Inet4Address adress = lsp.getLspIdentifiers_tlv().getTunnelSenderIPAddress();
			String dbId = getLSPDBList(adress);
			LSPDB lspdb = moduleList.get(dbId);
			if (lspdb == null)
			{

				if (dbActive)
				{
					log.info("redis: created new redis lspdb: "+dbId);
					lspdb = new LSPDB_Redis(dbId, dbHost);
					//jedis.hset(getModuleList(), adress.toString(), "0");
				}
				else
				{
					log.info("created new simple lspdb: "+dbId);
					lspdb = new LSPDB_Simple(dbId);
				}
				moduleList.put(adress.toString(),lspdb);
			}
			if (lsp.isrFlag())
			{
				lspdb.removeLSP(lsp);
			}
			else
			{
				lspdb.addLSP(lsp);
			}
		}
	}

	synchronized public void proccessOpen(OPEN open, Inet4Address address) 
	{
		log.info("PCC database sync");
		long dataBaseId = open.getLsp_database_version_tlv().getLSPStateDBVersion();
		LSPDB 	lspdb = new LSPDB_Simple(address.toString());
		//moduleList.put(address.toString(),lspdb);
	}


	public boolean isDbActive() {
		return dbActive;
	}


	public void setDbActive(boolean dbActive) {
		if (dbActive && (handlerId.length()==0 || dbHost.length()==0))
		{
			log.info("redis: can't set the db to active, there's no dbhost or/and handlerId");
			return;
		}
		this.dbActive = dbActive;
	}

	public int getPCCDatabaseVersion(Inet4Address address)
	{
		LSPDB lspdb = moduleList.get(address.toString());
		if (lspdb != null)
			return lspdb.getVersion();
		else
			return -1;
	}

}
