package tid.pce.client.tester;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Inet4Address;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Timer;
import java.util.logging.Level;
import java.util.logging.Logger;

import es.tid.pce.pcep.constructs.PCEPIntiatedLSP;
import es.tid.pce.pcep.messages.PCEPInitiate;
import es.tid.pce.pcep.messages.PCEPMessage;
import es.tid.pce.pcep.messages.PCEPReport;
import es.tid.pce.pcep.objects.BandwidthRequested;
import es.tid.pce.pcep.objects.EndPointsIPv4;
import es.tid.pce.pcep.objects.ExplicitRouteObject;
import es.tid.pce.pcep.objects.LSP;
import es.tid.pce.pcep.objects.SRP;
import es.tid.rsvp.objects.subobjects.IPv4prefixEROSubobject;
import tid.pce.pcepsession.DeadTimerThread;
import tid.pce.pcepsession.GenericPCEPSession;
import tid.pce.pcepsession.KeepAliveThread;
import tid.pce.pcepsession.PCEPSessionsInformation;
import tid.util.UtilsFunctions;
import tid.vntm.LSPManager;

public class StatefulPCEPSession extends GenericPCEPSession 
{
	LSPManager lspmanager;
	String source = null;
	String dest = null;
	private PCEPReport telink;
	private int messagetype;
	private String operation;
	private long lspid=1;
	
	
	
	public PCEPReport getTelink()
	{
		return telink;
	}

	public void setTelink(PCEPReport telink) 
	{
		this.telink = telink;
	}

	public StatefulPCEPSession(Socket s,PCEPSessionsInformation pcepSessionManager, int message_type)
	{
		super(pcepSessionManager);
		this.socket=s;
		log=Logger.getLogger("Stateful");
		log.setLevel(Level.OFF);
		timer=new Timer();
		this.keepAliveLocal=30;
		this.deadTimerLocal=180;
		this.messagetype=message_type;
		this.operation="add";
	}
	
	
	public void run()
	{
		
		initializePCEPSession(false,30,1000,false,false,null,null,0);
		this.deadTimerT=new DeadTimerThread(this, this.deadTimerLocal);
		startDeadTimer();	
		this.keepAliveT=new KeepAliveThread(out, this.keepAliveLocal);
		startKeepAlive();
	    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));


		boolean end=false;
		while (!end){
			System.out.println("Choose an option:\n\n");
			System.out.println("1) Send Initiate Message");
			System.out.println("2) Quit\n");
			
		      String op = null;
		      try {
		         op = br.readLine();
		      } catch (IOException ioe) {
		         System.out.println("IO error");
		         System.exit(1);
		      }
		      
		      if (op.equals("1")){
			
			
				PCEPInitiate pr = new PCEPInitiate();

				pr.setMessageType(messagetype);
				System.out.print("Destination: ");
			      String dest = null;
			      try {
			         dest = br.readLine();
			      } catch (IOException ioe) {
			         System.out.println("IO error");
			         System.exit(1);
			      }
			      
			      System.out.print("Source: ");
			      String source = null;
			      try {
			         source = br.readLine();
			      } catch (IOException ioe) {
			         System.out.println("IO error");
			         System.exit(1);
			      }
			      
			      System.out.print("BW: ");
			      String bwth = null;
			      try {
			    	  bwth = br.readLine();
			      } catch (IOException ioe) {
			         System.out.println("IO error");
			         System.exit(1);
			      }
			      float bandwidth=Float.parseFloat(bwth);
			      
			      
				try {
					IPv4prefixEROSubobject eroSubSource = new IPv4prefixEROSubobject();
					eroSubSource.setIpv4address((Inet4Address)Inet4Address.getByName(source == null ? "10.95.73.72" : source));
			
					IPv4prefixEROSubobject eroSubDest = new IPv4prefixEROSubobject();
					eroSubDest.setIpv4address((Inet4Address)Inet4Address.getByName(dest == null ? "10.95.73.73": dest));
					
					pr.setPcepIntiatedLSPList(new LinkedList<PCEPIntiatedLSP>());
					PCEPIntiatedLSP ilsp=new PCEPIntiatedLSP();
					pr.getPcepIntiatedLSPList().add(ilsp);
					pr.getPcepIntiatedLSPList().get(0).setEro(new ExplicitRouteObject());
					pr.getPcepIntiatedLSPList().get(0).setLsp(new LSP());
					pr.getPcepIntiatedLSPList().get(0).setRsp(new SRP());
					if (this.operation.equals("add")){
						pr.getPcepIntiatedLSPList().get(0).getRsp().setrFlag(false);
					} else if (this.operation.equals("del")){
						pr.getPcepIntiatedLSPList().get(0).getRsp().setrFlag(true);
					} else {
						log.warning("Tipo de operacion no soportada");
					}
					BandwidthRequested bw= new BandwidthRequested();
					bw.setBw(bandwidth);
					pr.getPcepIntiatedLSPList().get(0).setBandwidth(bw);
					pr.getPcepIntiatedLSPList().get(0).getLsp().setLspId((int)lspid);			
					EndPointsIPv4 ep=new EndPointsIPv4();
					ep.setDestIP((Inet4Address)Inet4Address.getByName(dest == null ? "10.95.73.73": dest));
					ep.setSourceIP((Inet4Address)Inet4Address.getByName(source == null ? "10.95.73.72" : source));
					 pr.getPcepIntiatedLSPList().get(0).setEndPoint(ep);
					pr.encode();
					sendRequest(out, pr);
					try {
						byte[] msg=readMsg(in);
						this.telink = new PCEPReport(msg);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
									
				} catch (Exception e)
				{
					e.printStackTrace();
					System.out.println("something happens");
					log.info(UtilsFunctions.exceptionToString(e));
				} 
		      }else if (op.equals("2") || op.equals("quit") || op.equals("q")){
					endConnections();
					end=true;
		      } else {
		    	  System.out.println("\n\nUnknown command. Please, try again!\n\n");
		      }
		      System.out.print("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n");

		}
	}

	private void sendRequest(DataOutputStream out, PCEPMessage telinkconf) {
		try 
		{  
			if (out==null)
				System.out.println("El out es null!!!");
			else{
			log.info("Sending request to VNTM");
			log.info("telinkconf::"+telinkconf.toString());
			out.write(telinkconf.getBytes());
			out.flush();
			}
		} catch (IOException e)
		{
			log.info(UtilsFunctions.exceptionToString(e));
		}   
	}

	
	protected byte[] readMsg(DataInputStream in) throws IOException{
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
				log.warning("Error reading data: "+ e.getMessage());
				throw e;
		    }catch (Exception e) {
				log.warning("readMsg Oops: " + e.getMessage());
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
				log.warning("End of stream has been reached");
				throw new IOException();
			}
		}
		if (length > 0) {
			ret = new byte[length];
			System.arraycopy(temp, 0, ret, 0, length);
		}		
		return ret;
	}

	@Override
	protected void endSession() {
		// TODO Auto-generated method stub
		
	}


}
