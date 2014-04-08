package tid.pce.server.lspdb;


/**
 * Interface for a database of lsps
 * 
 * @author Ayk
 *
 */

import tid.pce.pcep.objects.LSP;

public interface LSPDB 
{
	
	public static enum status{OK,ERROR};
	
	public void addLSP(LSP lsp);
	
	public LSP removeLSP(LSP lsp) throws NullPointerException;
	
	public LSP removeLSP(long lspId) throws NullPointerException;
	
	public void clearLSPs();
	
	public void updateLSP(LSP lsp);
	
	public LSP getLSP(long lspId);
	
	public int getVersion();
	


}
