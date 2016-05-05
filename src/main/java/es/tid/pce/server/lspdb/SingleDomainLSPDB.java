package es.tid.pce.server.lspdb;

import java.util.Enumeration;
import java.util.Hashtable;

import es.tid.pce.pcep.objects.LSP;
import es.tid.pce.server.SD_LSP;

public class SingleDomainLSPDB {
	
	private Hashtable <Integer,SD_LSP> LSP_list;
	
	private boolean exportDb = true;
	
	
	public boolean isExportDb() {
		return exportDb;
	}


	public void setExportDb(boolean exportDb) {
		this.exportDb = exportDb;
	}

	public SingleDomainLSPDB () {
		LSP_list = new  Hashtable <Integer,SD_LSP>();
	}

	public Hashtable<Integer, SD_LSP> getSingleDomain_LSP_list() {
		return LSP_list;
	}

	public void setSingleDomain_LSP_list(
			Hashtable<Integer, SD_LSP> LSP_list) {
		this.LSP_list = LSP_list;
	}
	
	public String toString() {
		String dbString;
		Enumeration <Integer> iter=this.LSP_list.keys();
		dbString="Single Domain LSPs: \r\n";
		while (iter.hasMoreElements()){
			Integer id= iter.nextElement();
			dbString=dbString+"\tid: "+id;
			SD_LSP mdlsp= this.LSP_list.get(id);
			dbString=dbString+mdlsp.toString();
		}
		return dbString;
	}
	

}
