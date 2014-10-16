package tid.test;

import java.net.Inet4Address;

import es.tid.pce.pcep.constructs.Request;
import es.tid.pce.pcep.messages.PCEPRequest;
import es.tid.pce.pcep.messages.PCEPResponse;
import es.tid.pce.pcep.objects.BandwidthRequested;
import es.tid.pce.pcep.objects.EndPointsIPv4;
import es.tid.pce.pcep.objects.ObjectiveFunction;
import es.tid.pce.pcep.objects.RequestParameters;
import tid.pce.client.ClientRequestManager;
import tid.pce.client.PCCPCEPSession;
import tid.pce.pcepsession.PCEPSessionsInformation;

public class PCEClient {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		try{
		String ipOpcticSourceString=args[0];
		String ipOpticDestString=args[1];
		String bandw=args[2];
		Inet4Address ipOpcticSource=(Inet4Address) Inet4Address.getByName(ipOpcticSourceString);
		Inet4Address ipOpticDest=(Inet4Address) Inet4Address.getByName(ipOpticDestString);

		PCEPSessionsInformation pcepSessionManagerPCE=new PCEPSessionsInformation();
		PCCPCEPSession PCEsession = new PCCPCEPSession("localhost", 4189 ,false,pcepSessionManagerPCE);
		PCEsession.start();	
		Thread.currentThread().sleep(1000);
		ClientRequestManager crm = PCEsession.crm;
		if (PCEsession.getOut()==null)
			System.out.println("La salida esta a null, algo raro pasa...");
		crm.setDataOutputStream(PCEsession.getOut());
		
		System.out.println("Enviamos de: "+ipOpcticSource+" a "+ipOpticDest);
		
		PCEPRequest p_r = new PCEPRequest();
		Request req = new Request();
		p_r.addRequest(req);
		RequestParameters rp= new RequestParameters();
		rp.setPbit(true);
		req.setRequestParameters(rp);
		rp.setRequestID(PCCPCEPSession.getNewReqIDCounter());
		//EndPointsIPv4 ep=new EndPointsIPv4();
		EndPointsIPv4 ep= new EndPointsIPv4();
		req.setEndPoints(ep);
		ep.setSourceIP(ipOpcticSource);	
		ep.setDestIP(ipOpticDest);
		
		
		ObjectiveFunction of=new ObjectiveFunction();
		of.setOFcode(1200);
		req.setObjectiveFunction(of);

		
		float bw = Float.parseFloat(bandw);
		BandwidthRequested bandwidth=new BandwidthRequested();
		bandwidth.setBw(bw);
		req.setBandwidth(bandwidth);					
		
		
		PCEPResponse pr=crm.newRequest(p_r);
		System.out.println("OJO!! Respuesta de PCE: "+pr.toString());
		}catch (Exception e) {
			e.printStackTrace();
		}
		
	}

}
