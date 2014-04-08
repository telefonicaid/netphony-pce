package tid.pce.server.lspdb;


/**
 * Interface for a database of lsps
 * 
 * @author Ayk
 *
 */

import java.util.Hashtable;
import java.util.logging.Logger;

import tid.pce.pcep.objects.LSP;

public class LSPDB_Simple implements LSPDB
{
	protected Hashtable<Long, LSP> LSPList;
	protected Logger log;
	protected String moduleId="";
	
	public LSPDB_Simple(String moduleId)
	{
		this.moduleId = moduleId;
		LSPList = new Hashtable<Long, LSP>();
		log = Logger.getLogger("PCEPParser");		
	}
	
	public void addLSP(LSP lsp)
	{
		long key = lsp.getLspId();
		LSPList.put(key, lsp);
	};
	
	public LSP removeLSP(LSP lsp) throws NullPointerException
	{
		long key = lsp.getLspId();
		return LSPList.remove(key);
	};
	

	public LSP removeLSP(long lspId) throws NullPointerException {
		return LSPList.remove(lspId);
	};
	
	public void clearLSPs()
	{
		LSPList = new Hashtable<Long, LSP>();
	};
	
	public void updateLSP(LSP lsp)
	{
		addLSP(lsp);
	};
	
	public LSP getLSP(long lspId)
	{
		return LSPList.get(lspId);
	}

	public String getModuleId() {
		return moduleId;
	}

	public void setModuleId(String moduleId) {
		this.moduleId = moduleId;
	}

	@Override
	public int getVersion() {
		return LSPList.size();
	}
	


}
