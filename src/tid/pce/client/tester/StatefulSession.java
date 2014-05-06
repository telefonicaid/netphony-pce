package tid.pce.client.tester;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Inet4Address;
import java.net.Socket;
import java.util.LinkedList;

import tid.pce.client.PCCPCEPSession;
import tid.pce.pcep.messages.PCEPInitiate;
import tid.pce.pcep.messages.PCEPMessage;
import tid.pce.pcep.messages.PCEPMessageTypes;
import tid.pce.pcep.messages.PCEPReport;
import tid.pce.pcep.objects.EndPointsIPv4;
import tid.pce.pcep.objects.ExplicitRouteObject;
import tid.pce.pcep.objects.LSP;
import tid.pce.pcep.objects.PCEPIntiatedLSP;
import tid.pce.pcep.objects.SRP;
import tid.pce.pcepsession.PCEPSessionsInformation;
import tid.rsvp.objects.subobjects.IPv4prefixEROSubobject;
import tid.util.UtilsFunctions;
import tid.vntm.client.VNTMClientSession;

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
				vntmsession = new StatefulPCEPSession( s,new PCEPSessionsInformation(), PCEPMessageTypes.MESSAGE_INTIATE);
				vntmsession.start();
				}
			catch(Exception e){
				e.printStackTrace();
			}
	      
	   }
	private static void sendRequest(DataOutputStream out, PCEPMessage telinkconf) {
		try 
		{  
			if (out==null)
				System.out.println("El out es null!!!");
			else{
			out.write(telinkconf.getBytes());
			out.flush();
			}
		} catch (IOException e)
		{
e.printStackTrace();		}   
	}


	protected void endSession() {
		// TODO Auto-generated method stub
		
	}
	
	protected static byte[] readMsg(DataInputStream in) throws IOException{
		byte[] ret = null;
		
		byte[] hdr = new byte[4];
		byte[] temp = null;
		boolean endHdr = false;
		int r = 0;
		int length = 0;
		boolean endMsg = false;
		int offset = 0;
		
		while (!endMsg) {
			try {
				if (endHdr) {
					r = in.read(temp, offset, 1);
				}
				else {
					r = in.read(hdr, offset, 1);
				}
			} catch (IOException e){
				throw e;
		    }catch (Exception e) {
				throw new IOException();
			}
		    
			if (r > 0) {
				if (offset == 2) {
					length = ((int)hdr[offset]&0xFF) << 8;
				}
				if (offset == 3) {
					length = length | (((int)hdr[offset]&0xFF));
					temp = new byte[length];
					endHdr = true;
					System.arraycopy(hdr, 0, temp, 0, 4);
				}
				if ((length > 0) && (offset == length - 1)) {
					endMsg = true;
				}
				offset++;
			}
			else if (r==-1){
				throw new IOException();
			}
		}
		if (length > 0) {
			ret = new byte[length];
			System.arraycopy(temp, 0, ret, 0, length);
		}		
		return ret;
	}

	}


