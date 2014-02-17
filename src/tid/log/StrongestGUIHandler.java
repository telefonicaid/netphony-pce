package tid.log;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;


public class StrongestGUIHandler extends Handler {

	private String host;
	private int port;


	
	public void publish(LogRecord logRecord) {
		Socket socket;
		try {
			socket = new Socket(host, port);
		} catch (Exception e1) {
			return;
		}
		// TODO Auto-generated method stub
		if (this.getFormatter() != null){
			try {
				BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
				((StrongestGUIFormatter)this.getFormatter()).setHost(host);				
				bufferedWriter.append(this.getFormatter().format(logRecord));
				bufferedWriter.flush();			
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		try {
			socket.close();
		} catch (Exception e1) {
			return;
		}
		
	}
	public String getHost() {
		return host;
	}
	public void setHost(String host) {
		this.host = host;
	}
	@Override
	public void close() throws SecurityException {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void flush() {
		// TODO Auto-generated method stub
		
	}
	public int getPort() {
		return port;
	}
	public void setPort(int port) {
		this.port = port;
	}
	

}
