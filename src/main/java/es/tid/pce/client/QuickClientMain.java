package es.tid.pce.client;


import static org.junit.Assert.assertTrue;

import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.tid.pce.pcep.constructs.EndPoint;
import es.tid.pce.pcep.constructs.GeneralizedBandwidthSSON;
import es.tid.pce.pcep.constructs.P2PEndpoints;
import es.tid.pce.pcep.constructs.PCEPIntiatedLSP;
import es.tid.pce.pcep.constructs.Request;
import es.tid.pce.pcep.messages.PCEPInitiate;
import es.tid.pce.pcep.messages.PCEPMessage;
import es.tid.pce.pcep.messages.PCEPMessageTypes;
import es.tid.pce.pcep.messages.PCEPRequest;
import es.tid.pce.pcep.messages.PCEPResponse;
import es.tid.pce.pcep.objects.BandwidthRequested;
import es.tid.pce.pcep.objects.BandwidthRequestedGeneralizedBandwidth;
import es.tid.pce.pcep.objects.EndPoints;
import es.tid.pce.pcep.objects.EndPointsIPv4;
import es.tid.pce.pcep.objects.GeneralizedEndPoints;
import es.tid.pce.pcep.objects.LSP;
import es.tid.pce.pcep.objects.ObjectiveFunction;
import es.tid.pce.pcep.objects.P2MPEndPointsIPv4;
import es.tid.pce.pcep.objects.RequestParameters;
import es.tid.pce.pcep.objects.SRP;
import es.tid.pce.pcep.objects.tlvs.EndPointIPv4TLV;
import es.tid.pce.pcep.objects.tlvs.SymbolicPathNameTLV;
import es.tid.pce.pcep.objects.tlvs.UnnumberedEndpointTLV;
import es.tid.pce.pcepsession.PCEPSessionsInformation;
import org.apache.commons.cli.*;

public class QuickClientMain {
	
	
	public static final Logger log =LoggerFactory.getLogger("PCCClient");

	public static void main(String[] args) {
		
		
		
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
		if (args.length < 4) {
			//Log.info("Usage: ClientTester <host> <port> <src> <dst> [options]");
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp( "PCC-QuickClient <host> <port> <src> <dst> [options]", options );
			return;
		}
		CommandLineParser parser = new DefaultParser();
		
		try {
			CommandLine line = parser.parse( options, args );
			
			//FileHandler fh;
			//FileHandler fh2;
			try {
				//fh=new FileHandler("PCCClient2.log");
				//fh2=new FileHandler("PCEPClientParser2.log");
				//fh.setFormatter(new SimpleFormatter());
				//fh2.setFormatter(new SimpleFormatter());
				//Log.addHandler(fh);
				//Log.setLevel(Level.ALL);
				Logger log2=LoggerFactory.getLogger("PCEPParser");
				//log2.addHandler(fh2);
				//log2.setLevel(Level.ALL);
			} catch (Exception e1) {
			// TODO Auto-generated catch block
				e1.printStackTrace();
				System.exit(1);
			}

			
			String ip = args[0];
			int port = Integer.valueOf(args[1]).intValue();
			
			QuickClientObj qcObj = new QuickClientObj(log, ip,  port);
			if(line.hasOption("li")){
				qcObj.setLocalAddress(line.getOptionValue("li"));
			}
			
			qcObj.start();		
	
			log.debug("Creating the message");
			Request req = qcObj.createReqMessage(args[2], args[3], line);
			System.out.println("Peticion "+req.toString());
			PCEPRequest p_r = new PCEPRequest();
			p_r.addRequest(req);
			
			LinkedList<PCEPMessage> messageList=new LinkedList<PCEPMessage>();
			System.out.println("Enviando mensaje");
			PCEPResponse res = qcObj.sendReqMessage(p_r, messageList);
			System.out.println("Enviado!!!");
			System.out.println("Respuesta "+res.toString());
			
		}
		catch( ParseException exp ) {
			System.err.println( "Parsing failed.  Reason: " + exp.getMessage() );
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp( "PCC-QuickClient <host> <port> <src> <dst> [options]", options );
			
		}
		System.exit(-1);
	}
	
	
}
