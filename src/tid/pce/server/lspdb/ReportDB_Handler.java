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


public class ReportDB_Handler {
	protected Hashtable<String, ReportDB> moduleList;

	private Logger log;

	private Jedis jedis;

	private String dbHost="";	
	private boolean dbActive = false;

	protected String handlerId="";



	public ReportDB_Handler ()
	{   

		log = Logger.getLogger("PCEPParser");
		moduleList = new Hashtable<String, ReportDB>();
	}


	public ReportDB_Handler(String handlerId, String dbHost)
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
				ReportDB_Redis rptdb = new ReportDB_Redis(modId,dbHost);
				rptdb.fillFromDB();
				moduleList.put(modId, rptdb);
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
			String modId = ((String) iterator.next()).replace("_StateReport","");	
			log.info("redis: module found: "+modId);
			ReportDB_Redis rptdb = new ReportDB_Redis(modId,dbHost);
			rptdb.fillFromDB();
			moduleList.put(modId, rptdb);
		}		
	}

	public void fillFromXML()
	{
		//TODO: por hacer
	}

	public String getModuleList(String handlerId)
	{
		return handlerId+"_*_StateReport";
	}

	public String getStateReportDBList(Inet4Address ad)
	{
		return handlerId+"_"+ad.toString();
	}

	public String getModuleList()
	{
		return handlerId+"_"+"MODULES";
	}

	public ReportDB getStateReportDB(String key)
	{
		return moduleList.get(key);
	}

	public void setStateReportDB(String key, ReportDB rptdb)
	{
		moduleList.put(key, rptdb);
	}


	synchronized public void processReport(PCEPReport pcepReport)
	{
		log.info("Adding PCEPReport to database,rpts:"+pcepReport.getStateReportList().size());


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
			String dbId = getStateReportDBList(adress);
			ReportDB rptdb = moduleList.get(dbId);
			if (rptdb == null)
			{

				if (dbActive)
				{
					log.info("redis: created new redis rptdb: "+dbId);
					rptdb = new ReportDB_Redis(dbId, dbHost);
					//jedis.hset(getModuleList(), adress.toString(), "0");
				}
				else
				{
					log.info("created new simple rptdb: "+dbId);
					rptdb = new ReportDB_Simple(dbId);
				}
				moduleList.put(adress.toString(),rptdb);
			}
			if (lsp.isrFlag())
			{
				rptdb.remove(stateReport);
			}
			else
			{
				rptdb.add(stateReport);
			}
		}
	}

	synchronized public void proccessOpen(OPEN open, Inet4Address address) 
	{
		log.info("PCC database sync");
		//long dataBaseId = open.getLsp_database_version_tlv().getLSPStateDBVersion();
		ReportDB 	rptdb = new ReportDB_Simple(address.toString());
		//moduleList.put(address.toString(),rptdb);
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
		ReportDB rptdb = moduleList.get(getStateReportDBList(address));
		if (rptdb != null)
			return rptdb.getVersion();
		else
			return 0;
	}

}
