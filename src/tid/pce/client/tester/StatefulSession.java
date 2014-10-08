package tid.pce.client.tester;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

import es.tid.pce.pcep.messages.PCEPMessageTypes;
import tid.pce.pcepsession.PCEPSessionsInformation;

public class StatefulSession {
	
	
	public static void main (String[] args) {

		try {
				Runtime.getRuntime().exec("clear");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	      BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

	      System.out.println("Statefull PCE Session");
	      System.out.println("Please get PCEPSession Info:");
	      System.out.print("PCE IP:");



	      String ip = null;
	      try {
	         ip = br.readLine();
	      } catch (IOException ioe) {
	         System.out.println("IO error");
	         System.exit(1);
	      }

	      System.out.print("PCE Port:");



	      String port = null;
	      int portnumber=0;
	      try {
	         port = br.readLine();
	         portnumber=Integer.parseInt(port);
	      } catch (IOException ioe) {
	         System.out.println("IO error");
	         System.exit(1);
	      }

	      System.out.print("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n");
	      StatefulPCEPSession vntmsession=null;
			
			try {
				Socket s = new Socket(ip, portnumber);
				PCEPSessionsInformation pcepsessioninfo=new PCEPSessionsInformation();
				pcepsessioninfo.setActive(true);
				pcepsessioninfo.setStateful(false);
				vntmsession = new StatefulPCEPSession( s,pcepsessioninfo, PCEPMessageTypes.MESSAGE_INITIATE);
				vntmsession.start();
				}
			catch(Exception e){
				e.printStackTrace();
			}
	      
	   }

	}


