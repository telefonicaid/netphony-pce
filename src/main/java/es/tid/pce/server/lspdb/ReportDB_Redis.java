package es.tid.pce.server.lspdb;


/**
 * Interface for a database of rpts
 * 
 * @author Ayk
 *
 */

import java.util.Iterator;
import java.util.List;

import redis.clients.jedis.Jedis;

import com.google.gson.Gson;

import es.tid.pce.pcep.PCEPProtocolViolationException;
import es.tid.pce.pcep.constructs.StateReport;

public class ReportDB_Redis extends ReportDB_Simple
{

	public static final String DEF_MODULE = "DEFAULT";
	private Jedis jedis;
	private boolean dbActive = true;

	
	public ReportDB_Redis()
	{
		super(ReportDB_Redis.DEF_MODULE);	
		jedis = new Jedis("localhost");
		jedis.connect();		
		if (jedis.isConnected())
		{
			log.info("redis: connection stablished");
		}

	}	
	
	public ReportDB_Redis(String moduleId)
	{
		super(moduleId);	
		jedis = new Jedis("localhost");
		jedis.connect();		
		if (jedis.isConnected())
		{
			log.info("redis: connection stablished");
		}

	}

	public ReportDB_Redis(String moduleId,String host)
	{
		super(moduleId);	
		log.info("redis: connecting to database..");
		jedis = new Jedis(host,6379);
		jedis.connect();		
		if (jedis.isConnected())
		{
			log.info("redis: connection stablished");
		}		
	}



	public void fillFromDB()
	{
		if (!jedis.isConnected())
		{
			log.info("redis: couldn't establish connection...");
			return;
		}
			log.info("redis: filling from db");

		List<String> StateReports = jedis.hvals(getStateReportListKey());
		for (Iterator iterator2 = StateReports.iterator(); iterator2.hasNext();) {
			String StateReportString = (String) iterator2.next();
			log.info("redis: found StateReport: "+StateReportString);
			//TODO: transformar y meter
			Gson gson = new Gson();
			byte[] rptBytes = gson.fromJson(StateReportString,byte[].class);
			try {
				StateReport rpt = new StateReport(rptBytes,0);
				log.info("redis: StateReport:::with Id"+getKey(rpt));

				StateReportList.put(getKey(rpt),rpt);

			} catch (Exception e) {
				e.printStackTrace();
			}
		}				
	}

	public void fillFromXML(String path)
	{
		//TODO: sin hacer
	}


	public void add(StateReport rpt)
	{
		super.add(rpt);
		if (dbActive && jedis.isConnected())
		{
			int key = getKey(rpt);
			log.info("redis: key: "+getKey(rpt));
			try {
				rpt.encode();
				Gson gson = new Gson();
				byte[] rptBytes = rpt.getBytes();
				log.info("redis: bytes: "+gson.toJson(rptBytes));
				String rptInfoString = gson.toJson(rptBytes);			
				jedis.hset(getStateReportListKey(), Integer.toString(key), rptInfoString);
				log.info("redis: added StateReport");
				
			} catch (PCEPProtocolViolationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		
	};


	public StateReport remove(int rptId) throws NullPointerException {
		if (dbActive)
		{
			jedis.hdel(getStateReportListKey(), Integer.toString(rptId));
			log.info("redis: removed StateReport with id: "+rptId);
		}
		return (super.remove(rptId));	
	};

	public StateReport remove(StateReport rpt) throws NullPointerException
	{
		return (remove(getKey(rpt)));
	};	

	public void clearStateReports()
	{
		super.clearReports();
		if (dbActive)
		{
			log.info("redis: clearing db "+getStateReportListKey());
			jedis.del(getStateReportListKey());
		}
	};

	public void update(StateReport rpt)
	{
		add(rpt);
	};

	public StateReport get(int rptId)
	{
		return StateReportList.get(rptId);
	};

	public String getStateReportListKey()
	{
		return moduleId+"_StateReport";
	}

	public boolean isDbActive() {
		return dbActive;
	}

	public void setDbActive(boolean dbActive) {
		this.dbActive = dbActive;
	}	


}
