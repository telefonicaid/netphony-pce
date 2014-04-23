package tid.pce.server.lspdb;


/**
 * Interface for a database of rpts
 * 
 * @author Ayk
 *
 */

import java.util.Hashtable;
import java.util.logging.Logger;

import tid.pce.pcep.constructs.StateReport;

public class ReportDB_Simple implements ReportDB
{
	protected Hashtable<Integer, StateReport> StateReportList;
	protected Logger log;
	protected String moduleId="";
	
	public ReportDB_Simple(String moduleId)
	{
		this.moduleId = moduleId;
		StateReportList = new Hashtable<Integer, StateReport>();
		log = Logger.getLogger("Roadm");		
	}
	
	public void add(StateReport rpt)
	{
		int key = getKey(rpt);
		StateReportList.put(key, rpt);
	};
	
	public StateReport remove(StateReport rpt) throws NullPointerException
	{
		int key = getKey(rpt);
		return StateReportList.remove(key);
	};
	

	public StateReport remove(int lspId) throws NullPointerException {
		return StateReportList.remove(lspId);
	};
	
	public void clearReports()
	{
		StateReportList = new Hashtable<Integer, StateReport>();
	};
	
	public void update(StateReport rpt)
	{
		add(rpt);
	};
	
	public StateReport get(int lspId)
	{
		return StateReportList.get(lspId);
	}

	public String getModuleId() {
		return moduleId;
	}

	public void setModuleId(String moduleId) {
		this.moduleId = moduleId;
	}

	@Override
	public int getVersion() {
		return StateReportList.size();
	}
	
	public int getKey(StateReport rpt)
	{
		return rpt.getLSP().getLspId();
	}
	


}
