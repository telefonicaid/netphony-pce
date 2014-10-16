package tid.pce.server.comunicationvntm;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Timer;
import java.util.logging.Logger;

import es.tid.pce.pcep.constructs.PCEPIntiatedLSP;
import es.tid.pce.pcep.messages.PCEPInitiate;
import es.tid.pce.pcep.messages.PCEPMessage;
import es.tid.pce.pcep.messages.PCEPReport;
import es.tid.pce.pcep.objects.EndPointDataPathID;
import es.tid.pce.pcep.objects.ExplicitRouteObject;
import es.tid.pce.pcep.objects.LSP;
import es.tid.pce.pcep.objects.SRP;
import es.tid.rsvp.objects.subobjects.OpenFlowUnnumberIfIDEROSubobject;
import tid.pce.client.PCCPCEPSession;
import tid.pce.pcepsession.DeadTimerThread;
import tid.pce.pcepsession.GenericPCEPSession;
import tid.pce.pcepsession.KeepAliveThread;
import tid.pce.pcepsession.PCEPSessionsInformation;
import tid.util.UtilsFunctions;
import tid.vntm.LSPManager;
import tid.vntm.topology.VNTMGraph;



//This class is used to send an Initiate message to the vntm


public class PCEPClientSession extends GenericPCEPSession 
{
	LSPManager lspmanager;
	private PCCPCEPSession NMSSession;
	private VNTMGraph vntmGraph;
	String source = null;
	String dest = null;
	private PCEPReport report;
	private int messagetype;
	private String operation;
	private String sourceMAC;
	private String destMAC;
	private int source_interface;
	private int destination_interface;
	private long lspid;
	
	
	
	public PCEPReport getReport()
	{
		return report;
	}


	
	public PCEPClientSession(Socket s,PCEPSessionsInformation pcepSessionManager)
	{
		super(pcepSessionManager);
		this.socket=s;
		log=Logger.getLogger("ABNO Controller");
		timer=new Timer();
		this.keepAliveLocal=30;
		this.deadTimerLocal=180;
	}
	
	public PCEPClientSession(Socket s,PCEPSessionsInformation pcepSessionManager, String sourceMAC, String destMAC, int source_interface, int destination_interface, String operation, int messagetype)
	{
		super(pcepSessionManager);
		this.socket=s;
		log=Logger.getLogger("ABNO Controller");
		timer=new Timer();
		this.keepAliveLocal=30;
		this.deadTimerLocal=180;
		this.sourceMAC = sourceMAC;
		this.destMAC = destMAC;
		this.source_interface = source_interface;
		this.destination_interface = destination_interface;
		this.operation = operation;
		this.messagetype= messagetype;
	}
	
	public void run()
	{
		
		initializePCEPSession(false,30,1000,false,false,null,null,0);
		//Session is UP now, start timers
		log.info("VNTM Session succesfully established!!");				
		this.deadTimerT=new DeadTimerThread(this, this.deadTimerLocal);
		startDeadTimer();	
		this.keepAliveT=new KeepAliveThread(out, this.keepAliveLocal);
		startKeepAlive();
		
		normalWF();

		
	}

	
	private void normalWF()
	{
		/*
		 * Alex perdona esto lo he tocado yo y no funciona. Habria que cambiar el Request por el Iniciate en cuanto
		 * tenga un seg lo arreglo
		 */
		PCEPInitiate pr = new PCEPInitiate();

		pr.setMessageType(this.messagetype);
	
		try {
			OpenFlowUnnumberIfIDEROSubobject eroSubSource = new OpenFlowUnnumberIfIDEROSubobject();
			eroSubSource.setSwitchID(sourceMAC);eroSubSource.setInterfaceID(0);
	
			OpenFlowUnnumberIfIDEROSubobject eroSubDest = new OpenFlowUnnumberIfIDEROSubobject();
			eroSubDest.setSwitchID(destMAC); eroSubDest.setInterfaceID(0);
			
			pr.setPcepIntiatedLSPList(new LinkedList<PCEPIntiatedLSP>());
			PCEPIntiatedLSP ilsp=new PCEPIntiatedLSP();
			pr.getPcepIntiatedLSPList().add(ilsp);
			pr.getPcepIntiatedLSPList().get(0).setEro(new ExplicitRouteObject());
			pr.getPcepIntiatedLSPList().get(0).setLsp(new LSP());
			pr.getPcepIntiatedLSPList().get(0).setRsp(new SRP());
			pr.getPcepIntiatedLSPList().get(0).getRsp().setrFlag(false);
			pr.getPcepIntiatedLSPList().get(0).getLsp().setLspId((int)lspid);			
			EndPointDataPathID ep=new EndPointDataPathID();
			ep.setSourceSwitchID(sourceMAC);
			ep.setDestSwitchID(destMAC);
			 pr.getPcepIntiatedLSPList().get(0).setEndPoint(ep);
			System.out.println("Vamos a enviar"+pr.toString());
			System.out.println("VNTMClient: Sending request...");
			pr.encode();
			sendRequest(out, pr);
			try {

				System.out.println("VNTMClient: Waiting for Response...");
				byte[] msg=readMsg(in);

				System.out.println("VNTMClient: DONE!");
				this.report = new PCEPReport(msg);
				//System.out.println(report.toString());
				log.info("Report Receive: "+this.report.toString());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			endConnections();
		
		} catch (Exception e)
		{
			System.out.println("something happens");
			log.info(UtilsFunctions.exceptionToString(e));
		} 
	}

	private void sendRequest(DataOutputStream out, PCEPMessage reportconf) {
		try 
		{  
			if (out==null)
				System.out.println("El out es null!!!");
			else{
			log.info("Sending request to VNTM");
			log.info("reportconf::"+reportconf.toString());
			out.write(reportconf.getBytes());
			out.flush();
			}
		} catch (IOException e)
		{
			log.info(UtilsFunctions.exceptionToString(e));
		}   
	}


	@Override
	protected void endSession() {
		// TODO Auto-generated method stub
		
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

}
