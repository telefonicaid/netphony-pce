package tid.pce.computingEngine;

import java.io.DataOutputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.logging.Logger;
import es.tid.pce.pcep.constructs.ErrorConstruct;
import es.tid.pce.pcep.constructs.StateReport;
import es.tid.pce.pcep.messages.PCEPError;
import es.tid.pce.pcep.messages.PCEPReport;
import es.tid.pce.pcep.objects.PCEPErrorObject;
import es.tid.pce.pcep.PCEPProtocolViolationException;
import es.tid.pce.pcep.constructs.PCEPIntiatedLSP;
import es.tid.pce.pcep.constructs.Response;
import es.tid.pce.pcep.messages.PCEPInitiate;
import es.tid.pce.pcep.messages.PCEPMessageTypes;
import es.tid.pce.pcep.messages.PCEPResponse;
import es.tid.pce.pcep.objects.ExplicitRouteObject;
import es.tid.pce.pcep.objects.LSP;
import es.tid.pce.pcep.objects.SRP;
import es.tid.rsvp.objects.subobjects.DataPathIDEROSubobject;
import es.tid.rsvp.objects.subobjects.EROSubobject;
import es.tid.rsvp.objects.subobjects.UnnumberIfIDEROSubobject;
import tid.pce.parentPCE.ReachabilityManager;
import tid.pce.tedb.DataPathID;
import tid.util.UtilsFunctions;

/**
 * It's a copy of PCEPResponse. It's an interface and it can be encoded as a PCEPResponse
 * or as a PCEPInitiate
 *
 */

public class ComputingResponse 
{	
	private byte messageBytes[];//The bytes of the message 
	private int encodingType = PCEPMessageTypes.MESSAGE_PCREP;
	
	public LinkedList<Response> ResponseList;
	private Logger log=Logger.getLogger("PCEP listener");
	public LinkedList<StateReport> ReportList;
	
	public LinkedList<StateReport> getReportList() {
		return ReportList;
	}

	public void setReportList(LinkedList<StateReport> reportList) {
		ReportList = reportList;
	}
	
	private ReachabilityManager reachabilityManager;
	
	/**
	 * Construct new PCEP Request from scratch
	 */
	public ComputingResponse()
	{
		super();
		ResponseList=new LinkedList<Response>();
		ReportList = new LinkedList<StateReport>();
	}
	
	public void addResponse(Response response)
	{		
		this.ResponseList.add(response);
	}
	public Response getResponse(int index)
	{
		return this.ResponseList.get(index);
	}
	
	public void addReport(StateReport report)
	{		
		this.ReportList.add(report);
	}
	public StateReport getResport(int index)
	{
		return this.ReportList.get(index);
	}
	
	

	public LinkedList<Response> getResponseList() 
	{
		return ResponseList;
	}

	public void setResponsetList(LinkedList<Response> responseList) 
	{
		ResponseList = responseList;
	}

	/**
	 * Encodes the PCEP Response Message
	 */
	
	public void encode(boolean isFather) throws PCEPProtocolViolationException
	{
		switch (encodingType)
		{
			case PCEPMessageTypes.MESSAGE_PCREP:
				PCEPResponse p_resp = new PCEPResponse();
				p_resp.setResponsetList(ResponseList);
				p_resp.encode();
				setMessageBytes(p_resp.getBytes());
			break;
			
			case PCEPMessageTypes.MESSAGE_INITIATE:
				if (isFather)
				{
					//This part will suppose the Initiate has only one Path
					
					ExplicitRouteObject ero = ResponseList.get(0).getPath(0).geteRO();
					
					EroAndIP EaIP;
					
					//This is something that should be corrected in the future
					//This class is to supposed to leave some bytes prepared for sending
					//The bytes from n=1 will be left for the outer class to encode
					//But the rest have to be encoded and sent from inside the class
					//It's a structural problem. The calling class should be changed
					
					int n = 2;
					while ((EaIP = getNthSubERO(ero,n))!=null)
					{	
						createInitAndSend(EaIP);
						n++;
					}
					
					EaIP = getNthSubERO(ero,1);
					PCEPInitiate pInit = new PCEPInitiate();
					LinkedList<PCEPIntiatedLSP> pcepIntiatedLSPList = new LinkedList<PCEPIntiatedLSP>();
					
					PCEPIntiatedLSP pILSP = new PCEPIntiatedLSP();
					LSP lsp = new LSP();
					SRP rsp = new SRP();
					
					ExplicitRouteObject ero_lsp = EaIP.ero;
					
					pILSP.setLsp(lsp);
					pILSP.setRsp(rsp);
					pILSP.setEro(ero_lsp);
					
					pcepIntiatedLSPList.add(pILSP);
					
					
					try 
					{
						pInit.encode();
					}
					catch (Exception e)
					{
						log.info(UtilsFunctions.exceptionToString(e));
					}
					setMessageBytes(pInit.getBytes());
				}
				else
				{
					PCEPInitiate pInit = new PCEPInitiate();
					LinkedList<PCEPIntiatedLSP> pcepIntiatedLSPList = new LinkedList<PCEPIntiatedLSP>();
					
					for (int i = 0; i < ResponseList.size(); i++) 
					{
						PCEPIntiatedLSP pILSP = new PCEPIntiatedLSP();
						LSP lsp = new LSP();
						SRP rsp = new SRP();
						ExplicitRouteObject ero = ResponseList.get(i).getPath(0).geteRO();
						
						pILSP.setLsp(lsp);
						pILSP.setRsp(rsp);
						pILSP.setEro(ero);
						
						pcepIntiatedLSPList.add(pILSP);
					}
					pInit.setPcepIntiatedLSPList(pcepIntiatedLSPList);
					pInit.encode();
					
					
					log.info("Encoding Son Finished, pInit.getBytes(): " + pInit.getBytes());
					setMessageBytes(pInit.getBytes());
				}

			break;
			case PCEPMessageTypes.MESSAGE_REPORT:
				if (ReportList.isEmpty()){
					PCEPError perror = new PCEPError();
					ErrorConstruct error = new ErrorConstruct();
					PCEPErrorObject e = new PCEPErrorObject();
					e.setErrorType(24);
					e.setErrorValue(3);
					error.getErrorObjList().add(e);
					perror.setError(error);
					
				}
				else {
					PCEPReport p_rep = new PCEPReport();
					p_rep.setStateReportList(ReportList);
					p_rep.encode();
					setMessageBytes(p_rep.getBytes());
				}
				
				
				break;
				
		}
	}
	
	private void createInitAndSend(EroAndIP EaIP)
	{
		PCEPInitiate pInit = new PCEPInitiate();
		LinkedList<PCEPIntiatedLSP> pcepIntiatedLSPList = new LinkedList<PCEPIntiatedLSP>();
		
		PCEPIntiatedLSP pILSP = new PCEPIntiatedLSP();
		LSP lsp = new LSP();
		SRP rsp = new SRP();
		
		ExplicitRouteObject ero_lsp = EaIP.ero;
		
		pILSP.setLsp(lsp);
		pILSP.setRsp(rsp);
		pILSP.setEro(ero_lsp);
		
		pcepIntiatedLSPList.add(pILSP);
		
		
		try 
		{
			pInit.encode();
			Socket clientSocket;
			clientSocket = new Socket(EaIP.address, 2222);
			DataOutputStream out_to_node = new DataOutputStream(clientSocket.getOutputStream());
			out_to_node.write(pInit.getBytes());
			out_to_node.flush();
		} 
		catch (Exception e) 
		{	
			log.info(UtilsFunctions.exceptionToString(e));
		}
	}
	
	private EroAndIP getNthSubERO(ExplicitRouteObject ero, int n)
	{
		// reachabilityManager should not be null if this is a fathers algorithm
		// If it is, make sure the algorithms set the reachabilityManager
		
		
		
		Inet4Address firstID = null;
		for (int i = 0; i < ero.getEROSubobjectList().size(); i++) 
		{
			if ((ero.getEROSubobjectList().get(i)) instanceof UnnumberIfIDEROSubobject)
			{
				UnnumberIfIDEROSubobject unAux = (UnnumberIfIDEROSubobject)ero.getEROSubobjectList().get(i);
				firstID = reachabilityManager.getDomain(unAux.getRouterID());
				break;
			}
		}
		
		int offset = 0, i = 0;		
		Inet4Address currentID = firstID;
		
		ExplicitRouteObject subERO = null;
		EroAndIP EaIP = new EroAndIP();
		EaIP.ero = subERO;
		
		for (int j = 0; j < n; j++) 
		{
			subERO = new ExplicitRouteObject();
			boolean hasDomainChanged = false;
			
			
			if (offset >= ero.getEROSubobjectList().size())
			{
				return null;
			}
			
			while(offset < ero.getEROSubobjectList().size() && !hasDomainChanged)
			{
				EROSubobject currentERO = ero.getEROSubobjectList().get(offset);
				
//				if ((ero.getEROSubobjectList().get(offset)) instanceof DataPathIDEROSubobject)
//				{
//					DataPathIDEROSubobject unAux = (DataPathIDEROSubobject)ero.getEROSubobjectList().get(i);
//					currentID = reachabilityManager.getDomain(unAux.getRouterID());
//					
//					
//					if (currentID!=null && firstID.equals(currentID))
//					{
//						subERO.addEROSubobject(currentERO);
//					}
//					else
//					{
//						hasDomainChanged = true;
//					}
//				}
				
				
				if ((ero.getEROSubobjectList().get(offset)) instanceof UnnumberIfIDEROSubobject)
				{
					UnnumberIfIDEROSubobject unAux = (UnnumberIfIDEROSubobject)ero.getEROSubobjectList().get(i);
					currentID = reachabilityManager.getDomain(unAux.getRouterID());
					
					
					if (currentID!=null && firstID.equals(currentID))
					{
						subERO.addEROSubobject(currentERO);
					}
					else
					{
						hasDomainChanged = true;
					}
				}

				
				else
				{
					subERO.addEROSubobject(currentERO);
				}
				
				
				
				offset++;		
			}
			
			
			
			
			try 
			{
				//Make a copy if the Inet4Address variable so there are no problems with pointer
				EaIP.address = (Inet4Address) InetAddress.getByAddress(firstID.getAddress());
			} 
			catch (UnknownHostException e)
			{
				log.info("NO EaIP.address; try EaIP.dataPathID");
				
				EaIP.dataPath = (DataPathID) DataPathID.getByNameBytes(firstID.getAddress());
				//log.info(UtilsFunctions.exceptionToString(e));
			}
			firstID = currentID;
			offset--;
		}	
		
		return EaIP;
	}
	
	private class EroAndIP
	{
		ExplicitRouteObject ero;
		Inet4Address address;
		DataPathID dataPath;
	}
	
	public void encode() throws PCEPProtocolViolationException 
	{
		encode(false);
	}

	public int getEncodingType() 
	{
		return encodingType;
	}
	public void setEncodingType(int encodingType)
	{
		this.encodingType = encodingType;
	}
	
	public byte[] getBytes()
	{
		return messageBytes;
	}

	public void setMessageBytes(byte[] messageBytes)
	{
		this.messageBytes = messageBytes;
	}

	public ReachabilityManager getReachabilityManager()
	{
		return reachabilityManager;
	}

	public void setReachabilityManager(ReachabilityManager reachabilityManager) 
	{
		this.reachabilityManager = reachabilityManager;
	}
	
	public String toString(){
		StringBuffer sb=new StringBuffer(ResponseList.size()*100);
		sb.append("RESP: ");
		for (int i=0;i<ResponseList.size();++i){
			sb.append(ResponseList.get(i).toString());
		}
		return sb.toString();
	}
	
	
}