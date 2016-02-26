package es.tid.pce.parentPCE.MDLSPDB;

public class SimpleLSP {
	
	public int LSP_Id;
	
	public SimpleLSPhop[] data;
	
	public String log="";
	
	public int getLSP_Id() {
		return LSP_Id;
	}

	public void setLSP_Id(int lSP_Id) {
		LSP_Id = lSP_Id;
	}

	public SimpleLSPhop[] getData() {
		return data;
	}

	public void setData(SimpleLSPhop[] data) {
		this.data = data;
	}

	public String getLog() {
		return log;
	}

	public void setLog(String log) {
		this.log = log;
	}

}
