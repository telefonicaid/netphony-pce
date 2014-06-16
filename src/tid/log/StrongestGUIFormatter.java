package tid.log;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class StrongestGUIFormatter extends Formatter{
	private String host;
	private String localAddress;
	public String getLocalAddress() {
		return localAddress;
	}


	public void setLocalAddress(String localAddress) {
		this.localAddress = localAddress;
	}


	@Override
	public String format(LogRecord logRecord) {		
			StringBuffer buf = new StringBuffer(1000);
			// Bold any levels >= WARNING			
			buf.append("POST ");
			buf.append("/pce_message.php?arrow=");
			buf.append(localAddress);
			buf.append("-");
			buf.append(logRecord.getMessage());			
			buf.append(" HTTP/1.0\r\n");
			buf.append("Host: "+host+"\r\n");
			buf.append("Accept: */*\r\n");
			buf.append("Connection: close\r\n\r\n");

			return buf.toString();



	}
	

	public String getHost() {
		return host;
	}
	public void setHost(String host) {
		this.host = host;
	}
	
	
	

}
