package tid.pce.computingEngine;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.logging.Logger;

import tid.pce.parentPCE.ReachabilityManager;
import tid.pce.pcep.PCEPProtocolViolationException;
import tid.pce.pcep.constructs.Response;
import tid.pce.pcep.messages.PCEPInitiate;
import tid.pce.pcep.messages.PCEPMessageTypes;
import tid.pce.pcep.messages.PCEPResponse;
import tid.pce.pcep.objects.ExplicitRouteObject;
import tid.pce.pcep.objects.LSP;
import tid.pce.pcep.objects.PCEPIntiatedLSP;
import tid.pce.pcep.objects.SRP;
import tid.rsvp.objects.subobjects.EROSubobject;
import tid.rsvp.objects.subobjects.UnnumberIfIDEROSubobject;
import tid.util.UtilsFunctions;

/**
 * It's a copy of PCEPResponse. It's an interface and it can be encoded as a PCEPResponse
 * or as a PCEPInitiate, asi que meritos a ogondio, nuestro master and commander y el espejo
 * en el que reflejarnos. Cuando el ejercito desvanece, sus odas al honor arengan
 * al equipo y hace que nos demos cuenta que no solo luchamos por nosotros mismos.
 * 
 * @author el treball dur, la humiltat, el seny, el pit i els collons
 *
 */

public class ComputingResponse 
{	
	private byte messageBytes[];//The bytes of the message 
	private int encodingType;
	
	public LinkedList<Response> ResponseList;
	private Logger log=Logger.getLogger("PCEPParser");
	
	private ReachabilityManager reachabilityManager;
	
	/**
	 * Construct new PCEP Request from scratch
	 */
	public ComputingResponse()
	{
		super();
		ResponseList=new LinkedList<Response>();
	}
	
	public void addResponse(Response response)
	{		
		this.ResponseList.add(response);
	}
	public Response getResponse(int index)
	{
		return this.ResponseList.get(index);
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
			
			case PCEPMessageTypes.MESSAGE_INTIATE:
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
				log.info(UtilsFunctions.exceptionToString(e));
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
	
	
	
}