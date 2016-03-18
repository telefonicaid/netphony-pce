package es.tid.pce.tests;


import static org.junit.Assert.*;

import java.util.LinkedList;
import java.util.logging.Logger;

import es.tid.pce.client.QuickClientObj;
import es.tid.pce.pcep.constructs.Request;
import es.tid.pce.pcep.constructs.Response;
import es.tid.pce.pcep.messages.PCEPMessage;
import es.tid.pce.pcep.messages.PCEPRequest;
import es.tid.pce.pcep.messages.PCEPResponse;
import es.tid.pce.server.PCEServer;
import org.apache.commons.cli.*;

public class PceTest implements Runnable {

    public void run() {
    	String[] args= new String[]{"src/test/resources/PCEServerConfiguration_SSON.xml"};
		PCEServer.main(args);
    }

	public PceTest() {
	}
	
	
	/**
	 * This tests starts a PCEServer, reads the topology from a File.
	 * After that this tests starts a PCC and send a path req.
	 * It checks after 10 seconds if the resp is correct.
	 */
	//@org.junit.Test
	public void testPCE(){
		//launch PCE server
		Thread pceServer = (new Thread(new PceTest()));
		pceServer.start();
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//launch PCC 
		Option gOpt = new Option("g", "Generalized end points");
		Option eroOpt = new Option("ero", "Explicit Route Object");
		Option iniOpt= new Option("ini", "Send init message");
		Option ofOpt= OptionBuilder.withArgName( "value" ).hasArg().withDescription(  "set of value" ).create( "of" );
		Option rgbwOpt= OptionBuilder.withArgName( "value" ).hasArg().withDescription(  "set rgbw value" ).create( "rgbw" );
		Option liOpt= OptionBuilder.withArgName( "value" ).hasArg().withDescription(  "local interface" ).create( "li" );
		Options options = new Options();
		options.addOption(liOpt);
		options.addOption(gOpt);
		options.addOption(eroOpt);
		options.addOption(iniOpt);
		options.addOption(ofOpt);
		options.addOption(rgbwOpt);
		
		try {
			String [] args=new String[]{"-g", "-of", "1002", "-rgbw", "2"};
			CommandLineParser parser = new DefaultParser();
			CommandLine line = parser.parse( options, args );
			Logger log =Logger.getLogger("PCCClient");
			QuickClientObj qcObj = new QuickClientObj(log, "localhost",  4189);
			qcObj.start();		
			
			System.out.println("Creando peticion");
			Request req = qcObj.createReqMessage("192.168.1.2","192.168.1.5", line);
			System.out.println("Peticion "+req.toString());
			PCEPRequest p_r = new PCEPRequest();
			p_r.addRequest(req);
			
			LinkedList<PCEPMessage> messageList=new LinkedList<PCEPMessage>();
			System.out.println("Enviando mensaje");
			PCEPResponse res = qcObj.sendReqMessage(p_r, messageList);
			System.out.println("Respuesta Obtenida (NOPATH) "+res.toString());
			assertTrue("Check NOPATH",res.getResponse(0).getNoPath()!=null);
			
			
			System.out.println("Creando peticion");
			req = qcObj.createReqMessage("192.168.1.1","192.168.1.3", line);
			System.out.println("Peticion "+req.toString());
			PCEPRequest p_r2 = new PCEPRequest();
			p_r2.addRequest(req);
			
			
			System.out.println("Enviando mensaje");
			res = qcObj.sendReqMessage(p_r2, messageList);
			
			
			
			System.out.println("Respuesta Obtenida "+res.toString());
			//System.out.println("RR "+res.getResponse(0).getNoPath());
			assertTrue("Checkin ERO in resp",res.getResponseList().getFirst().getPath(0).geteRO().toString().equals("<ERO: /192.168.1.1:1 /ELC: Grid 3 n:2 m:2 /192.168.1.2:2 /ELC: Grid 3 n:2 m:2 /192.168.1.3/32 >"));
			assertTrue("Checkin Bandwidth in resp",res.getResponseList().getFirst().getPath(0).getBandwidth().toString().equals("<GENW= GenBW SSON m: 2>"));
			//assertTrue("Checking if resp is correct",res.toString().equals("RESP: <RP ReqID: 32 Prio: 0 Reopt: 0 Bid: 0 Loose: 0 SupOF: false retry false><BW>PATH={ <ERO: /192.168.1.1:1 /ELC: Grid 3 n:10 m:2 /192.168.1.2:2 /ELC: Grid 3 n:10 m:2 /192.168.1.3/32 ><GENW= GenBW SSON m: 2> }"));
			
			
		}catch( ParseException exp ) {
			System.out.println( "Parsing failed.  Reason: " + exp.getMessage() );
			assertTrue("Parsing failed", false);
		}
		pceServer.interrupt();
		
		
	}
}
