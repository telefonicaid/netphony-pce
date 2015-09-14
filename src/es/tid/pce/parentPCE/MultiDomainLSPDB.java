package es.tid.pce.parentPCE;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;

import es.tid.tedb.IntraDomainEdge;

public class MultiDomainLSPDB {
	
	private Hashtable <Integer,MD_LSP> multiDomain_LSP_list;
	
	public MultiDomainLSPDB () {
		multiDomain_LSP_list= new  Hashtable <Integer,MD_LSP>();
	}

	public Hashtable<Integer, MD_LSP> getMultiDomain_LSP_list() {
		return multiDomain_LSP_list;
	}

	public void setMultiDomain_LSP_list(
			Hashtable<Integer, MD_LSP> multiDomain_LSP_list) {
		this.multiDomain_LSP_list = multiDomain_LSP_list;
	}
	
	public String toString() {
		String dbString;
		Enumeration <Integer> iter=this.multiDomain_LSP_list.keys();
		dbString="Multi Domain LSPs: \r\n";
		while (iter.hasMoreElements()){
			Integer id= iter.nextElement();
			dbString=dbString+"\tid: "+id;
			MD_LSP mdlsp= this.multiDomain_LSP_list.get(id);
			dbString=dbString+mdlsp.toString();
		}
		return dbString;
	}

}
