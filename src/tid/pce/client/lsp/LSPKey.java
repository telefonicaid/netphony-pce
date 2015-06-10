package tid.pce.client.lsp;

import java.net.Inet4Address;

public class LSPKey {
	
	public long lspId;
	public Inet4Address sourceAddress;
	
	public LSPKey (Inet4Address sourceIP, long Id){
		//LSPList = new Hashtable<Inet4Address, Hashtable<Long,LSPTE>>();
		this.sourceAddress=sourceIP;
		this.lspId=Id;		
	}

	public long getLspId() {
		return lspId;
	}

	public void setLspId(long lspId) {
		this.lspId = lspId;
	}

	public Inet4Address getSourceAddress() {
		return sourceAddress;
	}

	public void setSourceAddress(Inet4Address sourceAddress) {
		this.sourceAddress = sourceAddress;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (lspId ^ (lspId >>> 32));
		result = prime * result
				+ ((sourceAddress == null) ? 0 : sourceAddress.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		LSPKey other = (LSPKey) obj;
		if (lspId != other.lspId)
			return false;
		if (sourceAddress == null) {
			if (other.sourceAddress != null)
				return false;
		} else if (!sourceAddress.equals(other.sourceAddress))
			return false;
		return true;
	}
	
	public String toString()
	{
		return "<KEY Address: "+sourceAddress.toString()+ " lspId: "+lspId+" >";
	}
	
	
/*	public synchronized long getIdNewLSP(Inet4Address src) {
		LSPKey.idNewLSP=LSPKey.idNewLSP+1;
		long newLSP=LSPKey.idNewLSP;
		if (LSPKey.idNewLSP>=Integer.MAX_VALUE){
			LSPKey.idNewLSP=0;
		}
		return newLSP;
//		int id_LSP=0;
//		if (LSPList.get(src) == null){
//			id_from_source = new Hashtable<Inet4Address, Integer>();
//			id_LSP = 1;
//			id_from_source.put(src, id_LSP);
//		}
//		else {
//			id_LSP = id_from_source.get(src);
//			id_LSP++;
//			id_from_source.put(src, id_LSP);
//		}
		//return id_LSP;
	}*/
	
	/*public void setLSP (LSPTE lsp, long id, Inet4Address src){
		Hashtable<Long,LSPTE> LSPList_src;
		int ID = 0;
		if (LSPList.get(src) == null){
			LSPList_src = new Hashtable<Long,LSPTE>();
			LSPList_src.put(id, lsp);
		}
		else {
			LSPList_src = LSPList.get(src);
			LSPList_src.put	(id, lsp);	
		}
		LSPList.put(src, LSPList_src);		
	}
	public void removeLSP(long ID, Inet4Address src){
		Hashtable<Long,LSPTE> LSPList_dst = LSPList.get(src);	
		LSPList_dst.remove(ID);
		if (LSPList_dst.isEmpty()){
			LSPList.remove(src);
		}
	}*/
}
