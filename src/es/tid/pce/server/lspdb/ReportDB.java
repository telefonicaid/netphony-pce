package es.tid.pce.server.lspdb;


/**
 * Interface for a database of rpts
 * 
 * @author Ayk
 *
 */

import es.tid.pce.pcep.constructs.StateReport;

public interface ReportDB 
{
	
	public static enum status{OK,ERROR};
	
	public void add(StateReport rpt);
	
	public StateReport remove(StateReport rpt) throws NullPointerException;
	
	public StateReport remove(int rptId) throws NullPointerException;
	
	public void clearReports();
	
	public void update(StateReport rpt);
	
	public StateReport get(int rptId);
	
	public int getVersion();
	
	public int getKey(StateReport rpt);

}
