package tid.pce.server.lspdb;


/**
 * Interface for a database of lsps
 * 
 * @author jaume
 *
 */

import java.net.Inet4Address;

import tid.emulator.node.transport.lsp.LSPKey;
import tid.emulator.node.transport.lsp.te.LSPTE;
import tid.pce.pcep.messages.PCEPReport;
import tid.pce.pcep.objects.OPEN;

public interface LSP_DB 
{
	
	public void addLSP(LSPTE lsp);
	
	public LSPTE getLSP(LSPKey keyLSP);
	
	public void addPCC(Inet4Address adrss, boolean isSyncOver ,long dataBaseVersion);
	
	public long getPCCDatabaseVersion(Inet4Address adrss);
	
	public Boolean isPCCSyncOver(Inet4Address adrss);

	void addMessageToDatabase(PCEPReport pcepReport);
	
	long getDBIdentifier(Inet4Address address);
	
	void proccessOpen(OPEN open, Inet4Address adress);
	
	public void setPCCSyncOver(Inet4Address adrss);

}
