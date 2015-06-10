package tid.vntm.client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Timer;
import java.util.logging.Logger;

import es.tid.pce.pcep.PCEPProtocolViolationException;
import es.tid.pce.pcep.constructs.PCEPIntiatedLSP;
import es.tid.pce.pcep.messages.PCEPInitiate;
import es.tid.pce.pcep.messages.PCEPMessage;
import es.tid.pce.pcep.messages.PCEPReport;
import es.tid.pce.pcep.objects.EndPointsIPv4;
import es.tid.pce.pcep.objects.ExplicitRouteObject;
import es.tid.pce.pcep.objects.LSP;
import es.tid.pce.pcep.objects.SRP;
import es.tid.pce.pcep.objects.XifiUniCastEndPoints;
import es.tid.rsvp.objects.subobjects.IPv4prefixEROSubobject;
import tid.pce.client.PCCPCEPSession;
import tid.pce.pcepsession.DeadTimerThread;
import tid.pce.pcepsession.GenericPCEPSession;
import tid.pce.pcepsession.KeepAliveThread;
import tid.pce.pcepsession.PCEPSessionsInformation;
import tid.util.UtilsFunctions;
import tid.vntm.LSPManager;
import tid.vntm.topology.VNTMGraph;
/**
 * 
 *
 * Comments for GIT test
 */

public class VNTMClientSession extends GenericPCEPSession 
{
	LSPManager lspmanager;
	private PCCPCEPSession NMSSession;
	private VNTMGraph vntmGraph;
	String source = null;
	String dest = null;
	private PCEPReport telink;
	private int messagetype;
	private String operation;
	private String sourceMAC;
	private String destMAC;
	private int source_interface;
	private int destination_interface;
	private long lspid;
	private PCEPInitiate pcepInit;
	
	
	
	public PCEPReport getTelink()
	{
		return telink;
	}

	public void setTelink(PCEPReport telink) 
	{
		this.telink = telink;
	}

	public VNTMClientSession(Socket s,PCEPSessionsInformation pcepSessionManager, String source, String dest, int message_type, String op, long lspid)
	{
		super(pcepSessionManager);
		this.socket=s;
		log=Logger.getLogger("ABNO Controller");
		timer=new Timer();
		this.keepAliveLocal=30;
		this.deadTimerLocal=180;
		this.source = source;
		this.dest = dest;
		this.messagetype=message_type;
		this.operation=op;
		this.lspid=lspid;
	}
	
	public VNTMClientSession(Socket s,PCEPSessionsInformation pcepSessionManager)
	{
		super(pcepSessionManager);
		this.socket=s;
		log=Logger.getLogger("ABNO Controller");
		timer=new Timer();
		this.keepAliveLocal=30;
		this.deadTimerLocal=180;
	}
	
	public VNTMClientSession(Socket s,PCEPSessionsInformation pcepSessionManager,String SourceString, String DestString, String sourceMAC, String destMAC, int source_interface, int destination_interface, String operation)
	{
		super(pcepSessionManager);
		this.socket=s;
		log=Logger.getLogger("ABNO Controller");
		timer=new Timer();
		this.keepAliveLocal=30;
		this.deadTimerLocal=180;
		this.source = SourceString;
		this.dest = DestString;
		this.sourceMAC = sourceMAC;
		this.destMAC = destMAC;
		this.source_interface = source_interface;
		this.destination_interface = destination_interface;
		this.operation = operation;
	}
	
	public VNTMClientSession(Socket s,PCEPSessionsInformation pcepSessionManager, PCEPInitiate pcepInit, String operation)
	{
		super(pcepSessionManager);
		this.socket=s;
		log=Logger.getLogger("ABNO Controller");
		timer=new Timer();
		this.keepAliveLocal=30;
		this.deadTimerLocal=180;
		this.operation = operation;
		this.pcepInit = pcepInit;
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
		
		//FIXME: This could be useful
		//if (operation.equals(MPLSProvisioningWF.add_mult_vlan))
		//{
		//	addMultilayerVlan();
		//}else if (operation.equals(P2MPWorkflow.operation))
		//{
		//	callL0PCE();
		//}
		//else
		//{
			normalWF();
		//}
		
	}
	
	private void callL0PCE()
	{
		sendRequest(out, pcepInit);
	}
	
	private void addMultilayerVlan()
	{
		log.info("source:::||:::"+source);
		//PCEPRequest p_req =  Path_Computation.craftPackage(source, dest, sourceMAC, destMAC, source_interface, destination_interface);
		
		PCEPInitiate p_ini = new PCEPInitiate();
		PCEPIntiatedLSP p_lsp = new PCEPIntiatedLSP();
		
		
		XifiUniCastEndPoints endP = new XifiUniCastEndPoints();
		endP.setDestinationMAC(destMAC);
		endP.setSourceMAC(sourceMAC);
		endP.setSwitchSourceID(source);
		endP.setSwitchDestinationID(dest);
		endP.setSource_port(source_interface);
		endP.setDestination_port(destination_interface);
		
		p_lsp.setEndPoint(endP);
		
		p_ini.getPcepIntiatedLSPList().add(p_lsp);
		
		SRP rsp = new SRP();
		LSP lsp = new LSP();
		ExplicitRouteObject ero = new ExplicitRouteObject();
		
		p_lsp.setRsp(rsp);
		p_lsp.setLsp(lsp);
		p_lsp.setEro(ero);
		
		try 
		{
			p_ini.encode();
		} 
		catch (PCEPProtocolViolationException e) 
		{
			log.info(UtilsFunctions.exceptionToString(e));
		}
		sendRequest(out, p_ini);
		
		try 
		{

			System.out.println("VNTMClient: Waiting for Response...");
			byte[] msg=readMsg(in);

			System.out.println("VNTMClient: DONE!");
			this.telink = new PCEPReport(msg);
			//System.out.println(telink.toString());
			log.info("Report Receive: "+this.telink.toString());
		} 
		catch (Exception e)
		{
			log.info("It wasn't a report, well that's too bad!");
		}
		
		close(0);
	}
	
	private void normalW1F()
	{
		/*
		 * Alex perdona esto lo he tocado yo y no funciona. Habria que cambiar el Request por el Iniciate en cuanto
		 * tenga un seg lo arreglo
		 */
		PCEPInitiate pr = new PCEPInitiate();

		pr.setMessageType(messagetype);
	
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
			pr.getPcepIntiatedLSPList().get(0).getLsp().setLspId((int)lspid);			
			EndPointsIPv4 ep=new EndPointsIPv4();
			ep.setDestIP((Inet4Address)Inet4Address.getByName(dest == null ? "10.95.73.73": dest));
			ep.setSourceIP((Inet4Address)Inet4Address.getByName(source == null ? "10.95.73.72" : source));
			 pr.getPcepIntiatedLSPList().get(0).setEndPoint(ep);
			System.out.println("Vamos a enviar"+pr.toString());
			System.out.println("VNTMClient: Sending request...");
			pr.encode();
			sendRequest(out, pr);
			try {

				System.out.println("VNTMClient: Waiting for Response...");
				byte[] msg=readMsg(in);

				System.out.println("VNTMClient: DONE!");
				this.telink = new PCEPReport(msg);
				//System.out.println(telink.toString());
				log.info("Report Receive: "+this.telink.toString());
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			endConnections();
		
		} catch (Exception e)
		{
			System.out.println("something happens");
			log.info(UtilsFunctions.exceptionToString(e));
		} 
	}
	
	
	private void normalWF()
	{
		/*
		 * Alex perdona esto lo he tocado yo y no funciona. Habria que cambiar el Request por el Iniciate en cuanto
		 * tenga un seg lo arreglo
		 */
		PCEPInitiate pr = this.pcepInit;
	
			log.info("Vamos a enviar"+pr.toString());
			System.out.println("VNTMClient: Sending request...");
			try {
				pr.encode();
				sendRequest(out, pr);
				try {

					System.out.println("VNTMClient: Waiting for Response...");
					byte[] msg=readMsg(in);

					System.out.println("VNTMClient: DONE!");
					this.telink = new PCEPReport(msg);
					//System.out.println(telink.toString());
					log.info("Report Receive: "+this.telink.toString());
				} catch (IOException e) {
					e.printStackTrace();
				}
			} catch (PCEPProtocolViolationException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			
			endConnections();
				
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


	@Override
	protected void endSession() {
		
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
