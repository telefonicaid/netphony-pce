package tid.pce.client.multiLayer;

import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Inet4Address;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.TimerTask;
import java.util.logging.Logger;

import es.tid.pce.pcep.PCEPProtocolViolationException;
import es.tid.pce.pcep.messages.PCEPRequest;
import es.tid.pce.pcep.messages.PCEPTELinkSuggestion;
import es.tid.pce.pcep.messages.PCEPTELinkTearDownSuggestion;
import es.tid.pce.pcep.objects.EndPointsIPv4;
import es.tid.rsvp.objects.subobjects.IPv4prefixEROSubobject;
import tid.pce.client.emulator.AutomaticTesterStatistics;

public class RealiseMLCapacityTask  extends TimerTask {

	private Logger log;
	private ArrayList<String> sourceList;
	private ArrayList<String> destinationList;
	private AutomaticTesterStatistics stats;
	private LinkedList<Inet4Address> path;
	/*Variable used for counter how many requests there are*/

	PCEPRequest request;
	private DataOutputStream out;
	private PCEPTELinkSuggestion telinksug;
	
	public RealiseMLCapacityTask(ArrayList<String> sourceList, ArrayList<String> destinationList,AutomaticTesterStatistics stats ){
		log=Logger.getLogger("PCCClient");
		this.sourceList = sourceList;
		this.destinationList=destinationList;
		this.stats=stats;
	}
	
	public RealiseMLCapacityTask(AutomaticTesterStatistics stats,DataOutputStream out,PCEPTELinkSuggestion telinksug){
		log=Logger.getLogger("PCCClient");
		this.telinksug=telinksug;
		this.out=out;
		this.stats=stats;
	}
	
	@Override
	public void run(){
		log.info("Deleting LSP, releasing capacity");
		if (stats!=null)
			stats.releaseNumberActiveLSP();
		String s1;
		if (sourceList!=null){
			for (int i =0; i<sourceList.size();i++){
				s1 = releaseCapacity(sourceList.get(i), destinationList.get(i),String.valueOf(1));
				System.out.println(s1);
				sendUpdate("172.16.3.1", 4190, s1);
			}
		}
		
		else {
			PCEPTELinkTearDownSuggestion telinkdown = new PCEPTELinkTearDownSuggestion();
			EndPointsIPv4 endPoints = new EndPointsIPv4();
			
			IPv4prefixEROSubobject ip_src = (IPv4prefixEROSubobject)telinksug.getPath().geteRO().getEROSubobjectList().getFirst();
			IPv4prefixEROSubobject ip_dst = (IPv4prefixEROSubobject)telinksug.getPath().geteRO().getEROSubobjectList().getLast();			
			
			endPoints.setSourceIP(ip_src.getIpv4address());
			endPoints.setDestIP(ip_dst.getIpv4address());
						
			telinkdown.setEndPoints(endPoints);
			
			try {
				telinkdown.encode();
			} catch (PCEPProtocolViolationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			//enviamos el deleto del LSP óptico al VNTM
			System.out.println("ENVIAMOS EL MENSAJE DE DELETEO DEL LSP CAPA ÓPTICA");
			sendDelete(out,telinkdown);
		}
	}//End run

	public static void sendUpdate(String address, int port, String update) {
    	try {
    	    Socket socket = new Socket(address, port);
    	    BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
    	    bufferedWriter.write(update);
    	    bufferedWriter.flush();
    	    socket.close();
    	} catch (UnknownHostException e) {
    	    e.printStackTrace();
    	} catch (IOException e) {
    	    e.printStackTrace();
    	}
    }
    public void sendDelete(DataOutputStream out,PCEPTELinkTearDownSuggestion telinksug) {
//		int bytesToReserve = telinksug.getBytes().length;
//		byte[] bytes = new byte[bytesToReserve]; 
//		System.arraycopy(telinksug.getBytes(), 0, bytes, 0, bytesToReserve);
		try {		
			out.write(telinksug.getBytes());
			out.flush();
		} catch (IOException e){
			// TODO Auto-generated catch block
			e.printStackTrace();
		}			
			
    }
    
    public static String releaseCapacity(String sourceVector, String destVector, String bw) {
    	String s = "RELEASE:";
    	s += sourceVector + ":" + destVector + ":" + bw;

    	return s;
        }
}