package tid.pce.client.tester;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Inet4Address;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.TimerTask;
import java.util.logging.Logger;

import es.tid.pce.pcep.messages.PCEPRequest;
import es.tid.pce.pcep.messages.PCEPTELinkTearDownSuggestion;
import es.tid.pce.pcep.objects.EndPointsIPv4;
import tid.pce.client.emulator.AutomaticTesterStatistics;



public class RealiseMLCapacityTask  extends TimerTask {

	private Logger log;
	private ArrayList<String> sourceList;
	private ArrayList<String> destinationList;
	private AutomaticTesterStatistics stats;
	
	/*Variable used for counter how many requests there are*/

	PCEPRequest request;
	
	
	public RealiseMLCapacityTask(ArrayList<String> sourceList, ArrayList<String> destinationList,AutomaticTesterStatistics stats ){
		log=Logger.getLogger("PCCClient");
		this.sourceList = sourceList;
		this.destinationList=destinationList;
		this.stats=stats;
	}
	
	@Override
	public void run() {
		log.info("Deleting LSP, releasing capacity");
		if (stats != null){
			stats.releaseNumberActiveLSP();
		}
		String s1;
		EndPointsIPv4 endPointsIPv4 = new EndPointsIPv4();
		for (int i =0; i<sourceList.size();i++){
			PCEPTELinkTearDownSuggestion telinkTD = new PCEPTELinkTearDownSuggestion();
			
			try {
				endPointsIPv4.setSourceIP((Inet4Address)Inet4Address.getByName(sourceList.get(i)));
				endPointsIPv4.setDestIP((Inet4Address)Inet4Address.getByName(destinationList.get(i)));
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			telinkTD.setEndPoints(endPointsIPv4);
//			s1 = releaseCapacity(sourceList.get(i), destinationList.get(i),String.valueOf(1));
//			System.out.println(s1);
//			sendUpdate("172.16.3.1", 4190, s1);
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
    public static String releaseCapacity(String sourceVector, String destVector, String bw) {
    	String s = "RELEASE:";
    	s += sourceVector + ":" + destVector + ":" + bw;

    	return s;
        }
}