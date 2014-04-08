package tid.pce.server.lspdb;


/**
 * Interface for a database of lsps
 * 
 * @author Ayk
 *
 */

import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import redis.clients.jedis.Jedis;
import tid.pce.pcep.objects.LSP;
import tid.pce.pcep.objects.MalformedPCEPObjectException;

import com.google.gson.Gson;

public class LSPDB_Redis extends LSPDB_Simple
{


	//private String dbId = "";
	private Jedis jedis;
	private boolean dbActive = true;

	
	public LSPDB_Redis()
	{
		super(LSPDB_Handler.DEF_MODULE);	
		jedis = new Jedis("localhost");
		jedis.connect();		
		if (jedis.isConnected())
		{
			log.info("redis: connection stablished");
		}

	}	
	
	public LSPDB_Redis(String moduleId)
	{
		super(moduleId);	
		jedis = new Jedis("localhost");
		jedis.connect();		
		if (jedis.isConnected())
		{
			log.info("redis: connection stablished");
		}

	}

	public LSPDB_Redis(String moduleId,String host)
	{
		super(moduleId);	
		//this.dbId = dbId;			
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

		List<String> LSPs = jedis.hvals(getLSPListKey());
		for (Iterator iterator2 = LSPs.iterator(); iterator2.hasNext();) {
			String LSPString = (String) iterator2.next();
			log.info("redis: found LSP: "+LSPString);
			//TODO: transformar y meter
			Gson gson = new Gson();
			byte[] lspBytes = gson.fromJson(LSPString,byte[].class);
			try {
				LSP lsp = new LSP(lspBytes,0);
				log.info("redis: LSP:::with Id"+lsp.getLspId()+" flags: "+lsp.getObjectClass()+" "+lsp.issFlag());

				LSPList.put(lsp.getLspId(),lsp);

			} catch (MalformedPCEPObjectException e) {
				e.printStackTrace();
			}
		}				
	}

	public void fillFromXML(String path)
	{
		//TODO: sin hacer
	}


	public void addLSP(LSP lsp)
	{
		super.addLSP(lsp);
		if (dbActive)
		{
			long key = lsp.getLspId();
			lsp.encode();
			Gson gson = new Gson();
			byte[] lspBytes = lsp.getBytes();
			String lspInfoString = gson.toJson(lspBytes);			
			jedis.hset(getLSPListKey(), Long.toString(key), lspInfoString);
			log.info("redis: added LSP");
		}
	};


	public LSP removeLSP(long lspId) throws NullPointerException {
		if (dbActive)
		{
			jedis.hdel(getLSPListKey(), Long.toString(lspId));
			log.info("redis: removed LSP with id: "+lspId);
		}
		return (super.removeLSP(lspId));	
	};

	public LSP removeLSP(LSP lsp) throws NullPointerException
	{
		return (removeLSP(lsp.getLspId()));
	};	

	public void clearLSPs()
	{
		super.clearLSPs();
		if (dbActive)
		{
			log.info("redis: clearing db "+getLSPListKey());
			jedis.del(getLSPListKey());
		}
	};

	public void updateLSP(LSP lsp)
	{
		addLSP(lsp);
	};

	public LSP getLSP(long lspId)
	{
		return LSPList.get(lspId);
	};

	public String getLSPListKey()
	{
		return moduleId+"_LSP";
	}

	public boolean isDbActive() {
		return dbActive;
	}

	public void setDbActive(boolean dbActive) {
		this.dbActive = dbActive;
	}	


}
