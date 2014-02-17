package tid.pce.client.multiDomain;
import java.util.Timer;

import tid.pce.client.PCCPCEPSession;
import tid.pce.pcepsession.PCEPSessionsInformation;


/**
 * Testeador de caminos 
 * @author Marta Cuaresma Saturio
 *
 */
public class AutomaticTesterSpainNetwork {
	
	private static int PCEServerPort = 4183;
	private static String ipPCE = "localhost";
	private static PCCPCEPSession PCEsession;
	private static long timeProcessing = 10000;	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		/*If there are arguments, read the PCEServerPort and ipPCE*/
		  if (args.length < 2) {
			System.out.println("Usage: ClientTester <host> <port>");
			return;
		}

		ipPCE = args[0];
		PCEServerPort = Integer.valueOf(args[1]).intValue();
		PCEsession = new PCCPCEPSession(ipPCE, PCEServerPort,false, new PCEPSessionsInformation());
	   PCEsession.start();
	   /*Creo mi testeador*/
	   AutomaticTesterSpainNetworkTask srt= new AutomaticTesterSpainNetworkTask(PCEsession,PCEsession.getPeerPCE_port());
	   Timer timer=new Timer();
	   timer.schedule(srt, 0,timeProcessing);
	 
	}
	}
