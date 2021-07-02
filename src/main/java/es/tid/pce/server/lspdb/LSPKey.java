package es.tid.pce.server.lspdb;

import java.net.Inet4Address;

public class LSPKey {
	
	private Inet4Address pccAddress;
	
	private long lspId;

	public LSPKey(Inet4Address pccAddress,long lspId ) {
		this.pccAddress=pccAddress;
		this.lspId=lspId;
	}

	public Inet4Address getPccAddress() {
		return pccAddress;
	}

	public void setPccAddress(Inet4Address pccAddress) {
		this.pccAddress = pccAddress;
	}

	public long getLspId() {
		return lspId;
	}

	public void setLspId(long lspId) {
		this.lspId = lspId;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (lspId ^ (lspId >>> 32));
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
		return true;
	}

	@Override
	public String toString() {
		return "LSPKey [lspId=" + lspId + "]";
	}
	
	

}
