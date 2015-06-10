package tid.vntm.client;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import tid.pce.pcepsession.PCEPSessionsInformation;

public class VNTMClient 
{
	public static void main(String[] args)
	{
		
		try {
			Socket s = new Socket("localhost", 4189);
			VNTMClientSession vntmsession = new VNTMClientSession( s,new PCEPSessionsInformation());
			vntmsession.start();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}			
	}
}
