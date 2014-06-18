package tid.ipnms.wsimpl.impl;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;

import org.apache.commons.net.telnet.InvalidTelnetOptionException;
import org.apache.commons.net.telnet.TelnetClient;

/**
 * 
 * Telnet protocol client
 * 
 * @author Telefonica I+D
 *
 */

public class IdaTelnetClientPrueba {
	private TelnetClient tc = null;
	private OutputStream outstr = null;
	private InputStream instr = null;
	private String response = "";
	private Thread responseThread = null; 
	private boolean checkResponseString = false;
	private String [] tokensToCheck = null;
	
//	TerminalTypeOptionHandler ttopt = new TerminalTypeOptionHandler("VT100", false, false, true, false);
//    EchoOptionHandler echoopt = new EchoOptionHandler(true, false, true, false);
//    SuppressGAOptionHandler gaopt = new SuppressGAOptionHandler(true, true, true, true);

	/**
	 * SLEEP_PERIOD is duration to wait before reading the @see InputStream after sending a command.
	 * @value 250
	 */
	public static int SLEEP_PERIOD = 250;
	
	public IdaTelnetClientPrueba(int sleepPeriod){
		SLEEP_PERIOD = sleepPeriod;
		checkResponseString = false;
		tokensToCheck = null;
	}
	
	public IdaTelnetClientPrueba(boolean _checkResponseString, String []_tokensToCheck){
		checkResponseString = _checkResponseString;
		tokensToCheck = _tokensToCheck;
	}
	
	public IdaTelnetClientPrueba(){
	}

	/**
	 * Connect to the Telnet server using given hostname and port number.
	 * 
	 * @param  hostname             Telnet hostname to connect, either hostname or IP.
	 * @param  port                 Telnet port number to connect
	 * @throws IOException 
	 * @throws SocketException 
	 * @throws InterruptedException 
	 * @throws InvalidTelnetOptionException 
	 */
	public void connect(String hostname, int port) throws SocketException, IOException, InterruptedException
	{		
		System.out.println("IdaTelnetClient:connect Connecting to host: " + hostname + " via port: " + port);
		
		tc = new TelnetClient();
//		try {
//			tc.addOptionHandler(ttopt);
//	        tc.addOptionHandler(echoopt);
//	        tc.addOptionHandler(gaopt);
//		} catch (InvalidTelnetOptionException e) {
//			throw new IOException(e.getCause());
//		}


		tc.connect(hostname, port);

		outstr = tc.getOutputStream();
		instr = tc.getInputStream();

		responseThread = new Thread(new ResponseReader());
//		tc.registerNotifHandler(new ResponseReader());
		
		responseThread.start();
		
		if(!checkResponseString)
			Thread.sleep(SLEEP_PERIOD);
		else {
			done: while(true) {
				for (int i = 0 ; i < tokensToCheck.length ; i++) {					
					if (response.contains(tokensToCheck[i])){
						break done;
					}
				}
				Thread.sleep(SLEEP_PERIOD);
			}				
		}
		
		synchronized(response){
			response = "";
		}
	}

	/**
	 * Disconnect the telnet connection.
	 * 
	 * @throws IOException 
	 */
	public void close() throws IOException {
		outstr.close();
		instr.close();
		tc.disconnect();
		responseThread.interrupt();		
	}

	public String send(String telnetCommand) throws IOException, InterruptedException {  
		return send(telnetCommand, true);
	}

		
	/**
	 * Sends the given telnet command.
	 * 
	 * @param  telnetCommand        Telnet command to be sent, must be \n terminated.
	 * @return                      Response from Telnet server.
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	public String send(String telnetCommand, boolean waitForResponse) throws IOException, InterruptedException {        	

		byte[] sendBuffer = telnetCommand.getBytes();
		int commandLength = sendBuffer.length;	
		String localResponse = null;

		if(commandLength > 0)
		{
			System.out.println("IdaTelnetClient:send Sending the command: " + telnetCommand);
			
			outstr.write(sendBuffer, 0 , commandLength);
			outstr.flush();

			if(waitForResponse){
				if(!checkResponseString)
					Thread.sleep(SLEEP_PERIOD);
				else {
					done: while(true) {
						for (int i = 0 ; i < tokensToCheck.length ; i++) {					
							if (response.contains(tokensToCheck[i])){
								break done;
							}
						}
						Thread.sleep(SLEEP_PERIOD);
					}				
				}
			}	
			else
				Thread.sleep(SLEEP_PERIOD);
		}

		synchronized(response){
			localResponse = response;
			response = "";
		}
		return localResponse;  
	}

	/**
	 * Thread responsible for reading response from telnet server
	 */
	private class ResponseReader implements Runnable {  
		public void run() {
			try {
				byte[] buff = new byte[1024];
				int ret_read = 0;

				do {
					ret_read = instr.read(buff, 0, 1024);    
					if(ret_read > 0) {
						System.out.println(new String(buff, 0, ret_read));
						response += new String(buff, 0, ret_read);  	    	         	
					}	    	 
				}
				while(ret_read >= 0);      
			}
			catch (IOException e) {
				System.err.println("Exception while reading socket:" + e.getMessage());
			} catch (Exception e) {
				System.out.println("Thread interrupted stop execution");
			}
		}

		/***
	     * Callback method called when TelnetClient receives an option
	     * negotiation command.
	     * <p>
	     * @param negotiation_code - type of negotiation command received
	     * (RECEIVED_DO, RECEIVED_DONT, RECEIVED_WILL, RECEIVED_WONT)
	     * <p>
	     * @param option_code - code of the option negotiated
	     * <p>
	     ***/
//		public void receivedNegotiation(int negotiation_code, int option_code)
//	    {
//	        String command = null;
//	        if(negotiation_code == TelnetNotificationHandler.RECEIVED_DO)
//	        {
//	            command = "DO";
//	        }
//	        else if(negotiation_code == TelnetNotificationHandler.RECEIVED_DONT)
//	        {
//	            command = "DONT";
//	        }
//	        else if(negotiation_code == TelnetNotificationHandler.RECEIVED_WILL)
//	        {
//	            command = "WILL";
//	        }
//	        else if(negotiation_code == TelnetNotificationHandler.RECEIVED_WONT)
//	        {
//	            command = "WONT";
//	        }
//	        System.out.println("IdaTelnetClient:receivedNegotiation Received " + command + " for option code " + option_code);
//	   }
//
//	}
	
	}

	/**
	 * Receive the response from telnetServer
	 * 
	 * @return                      Response from server.
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	public String receive() throws IOException, InterruptedException {
		// TODO Auto-generated method stub
		return null;
	}
	}


