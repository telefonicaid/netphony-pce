package tid.pce.client.multiDomain;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Inet4Address;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.TimerTask;
import java.util.logging.Logger;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import es.tid.pce.pcep.constructs.Request;
import es.tid.pce.pcep.messages.PCEPMonReq;
import es.tid.pce.pcep.messages.PCEPRequest;
import es.tid.pce.pcep.messages.PCEPResponse;
import es.tid.pce.pcep.objects.EndPointsIPv4;
import es.tid.pce.pcep.objects.Monitoring;
import es.tid.pce.pcep.objects.ObjectiveFunction;
import es.tid.pce.pcep.objects.PccReqId;
import es.tid.pce.pcep.objects.RequestParameters;
import tid.pce.client.ClientRequestManager;
import tid.pce.client.PCCPCEPSession;
import tid.pce.computingEngine.AlgorithmRule;
import tid.pce.computingEngine.MapAlgoRule;

public class AutomaticTesterSpainNetworkTask  extends TimerTask {
	private final int portDomainA=4183;
	private final int portDomainB=4184;
	private final int portDomainC=4185;
	private final int portDomainD=4186;
	private final int portDomainE=4187;
	
	private ArrayList<Inet4Address> source;
	private ArrayList<Inet4Address> destination;
	private ClientRequestManager crm;
	private int PCEServerPort;
	private Logger log;
	private LinkedList<MapAlgoRule> algorithmRuleList;
	
	private static long monitoringIdNumber = 1;
	private PCCPCEPSession ps;
	/*Variable used for counter how many requests there are*/
	private int counter;
	
	AutomaticTesterSpainNetworkTask(PCCPCEPSession ps, int port){

		algorithmRuleList = new LinkedList<MapAlgoRule>();
		destination = new ArrayList<Inet4Address>();
		source  = new ArrayList<Inet4Address>();
		log=Logger.getLogger("PCCClient");
		this.crm=ps.crm;
		this.PCEServerPort=port;
		this.ps = ps;
	}
	

	
	@Override
	public void run() {
		log.info("Starting Automatic Client Interface");
		try{
			counter=0;
		     SAXParserFactory factory = SAXParserFactory.newInstance();
		     SAXParser saxParser = factory.newSAXParser();
		     SaxHandler handler = new SaxHandler();

		    
		        
				switch(PCEServerPort){
				case portDomainA:
					saxParser.parse("AutomaticClientTesterA.xml", handler);
					break;
				case portDomainB:
					saxParser.parse("AutomaticClientTesterB.xml", handler);
					break;
				case portDomainC:
					saxParser.parse("AutomaticClientTesterC.xml", handler);
					break;
				case portDomainD:
					saxParser.parse("AutomaticClientTesterD.xml", handler);
					break;
				case portDomainE:
					saxParser.parse("AutomaticClientTesterE.xml", handler);
					break;
				default:
					saxParser.parse("AutomaticClientTester.xml", handler);
					break;

				}

	 } 
	   catch (Exception e) {
	   e.printStackTrace();
	   }     
		System.out.println("Click 1 to start");
		//
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

		String command = null;

		try {
			command = br.readLine();
		} catch (IOException ioe) {
			System.out.println("IO error trying to read your command");
			System.exit(1);
		}
		boolean PCMonReqBool=true;
		if (command.equals("1")) {
	for (int i=0;i<counter;i++){
		System.out.println(" Creating path between "+source.get(i).toString()+" and "+destination.get(i).toString());
		if (PCMonReqBool){
			PCEPRequest p_r = createRequestMessage(i);	
			PCEPResponse pr=crm.newRequest(p_r);
			System.out.println("Respuesta "+pr.toString());
		}
		else{
			PCEPMonReq p_m_r= createMonRequestMessage(i);
			PCEPResponse pr=crm.newRequest(p_m_r);
			System.out.println("Respuesta "+pr.toString());
		}
		
		
		}
	}

	}//End run
	
	private class SaxHandler extends DefaultHandler  {
		String tempVal;
		public void startElement(String uri, String localName,String qName, Attributes attrs) 
				   throws SAXParseException,SAXException {
 
   		 if (qName.equalsIgnoreCase("algorithmRule")) {
			  MapAlgoRule mar= new MapAlgoRule();
			  AlgorithmRule ar=new AlgorithmRule();
			  String aname=attrs.getValue("name");
			  ar.of=Integer.parseInt(attrs.getValue("of"));
   		  ar.svec=Boolean.parseBoolean(attrs.getValue("svec"));		        		  
   		  mar.ar=ar;
   		  mar.algoName=aname;
   		  mar.isParentPCEAlgorithm=Boolean.parseBoolean(attrs.getValue("isParentPCEAlgorithm"));
   		  mar.isWSONAlgorithm=Boolean.parseBoolean(attrs.getValue("isWSONAlgorithm"));
   		  algorithmRuleList.add(mar);
		  }

    	 
    		}
    	public void endElement(String uri, String localName,String qName) throws SAXException {
    		try{
    			System.out.println("End Element :" + qName);
    			if (qName.equalsIgnoreCase("Request")){
    				counter++;
    			}
    			else if (qName.equalsIgnoreCase("source")) {    				
    				source.add(counter, (Inet4Address) Inet4Address.getByName(tempVal.trim()));		
    				
    			}
    			else if (qName.equalsIgnoreCase("destination")) {    				
    				destination.add(counter,(Inet4Address) Inet4Address.getByName(tempVal.trim()));
    			}
    		

    		}catch (Exception e) {
    			e.printStackTrace();
    		}

    	 
    		}
    	 
    		public void characters(char ch[], int start, int length) throws SAXException {
    			tempVal = new String(ch,start,length);
 
    	 
    		}
    		}//End DefaultHandler
	
	/**
	 * Create a PC Request message including Monitoring, PCC-Id-Req and Request
	 * @param i index of the request  
	 * @return
	 */
	PCEPRequest createRequestMessage(int i){
		PCEPRequest p_r = new PCEPRequest();
		//Creamos el objecto monitoring
		Monitoring monitoring=createMonitoring();
		//Creamos el objeto PCCIdReq
		PccReqId pccReqId = createPccReqId();
		//Creamos el object Request 
		Request req = createRequest(source.get(i),destination.get(i));		
		ObjectiveFunction of=new ObjectiveFunction();
		of.setOFcode(algorithmRuleList.get(0).ar.of);
		req.setObjectiveFunction(of);
		p_r.setMonitoring(monitoring);
		p_r.setPccReqId(pccReqId);
		p_r.addRequest(req);
		return p_r;
	}
	/**
	 * Create a PC Monitoring Request message including Monitoring, PCC-Id-Req
	 * @param i index of the request  
	 * @return
	 */
	PCEPMonReq createMonRequestMessage(int i){
		PCEPMonReq p_m_r = new PCEPMonReq();
		//Creamos el objecto monitoring
		Monitoring monitoring=createMonitoring();
		//Creamos el objeto PCCIdReq
		PccReqId pccReqId = createPccReqId();
		p_m_r.setMonitoring(monitoring);
		p_m_r.setPccReqId(pccReqId);
		return p_m_r;
	}
	/**
	 * Create a request object
	 * @param src_ip
	 * @param dst_ip
	 * @return
	 */
	public Request createRequest(Inet4Address src_ip, Inet4Address dst_ip){
		Request req = new Request();
		RequestParameters rp= new RequestParameters();
		rp.setPbit(true);
		req.setRequestParameters(rp);		
		rp.setRequestID(PCCPCEPSession.getNewReqIDCounter());
		System.out.println("Creating test Request");
		
		int prio = 1;
		rp.setPrio(prio);
		boolean reo = false;
		rp.setReopt(reo);
		boolean bi = false;
		rp.setBidirect(bi);
		boolean lo = false;
		rp.setLoose(lo);
		EndPointsIPv4 ep=new EndPointsIPv4();				
		req.setEndPoints(ep);
		ep.setSourceIP(src_ip);	
		ep.setDestIP(dst_ip);
		
		return req;
	}
	/**
	 * Create message PCMonReq to send
	 * @param src_ip
	 * @param dst_ip
	 * @return
	 */
	public Request createMonRequest(Inet4Address src_ip, Inet4Address dst_ip){
		Request req = new Request();
		RequestParameters rp= new RequestParameters();
		rp.setPbit(true);
		req.setRequestParameters(rp);		
		rp.setRequestID(PCCPCEPSession.getNewReqIDCounter());
		System.out.println("Creating test Request");
		
		int prio = 1;
		rp.setPrio(prio);
		boolean reo = false;
		rp.setReopt(reo);
		boolean bi = false;
		rp.setBidirect(bi);
		boolean lo = false;
		rp.setLoose(lo);
		EndPointsIPv4 ep=new EndPointsIPv4();				
		req.setEndPoints(ep);
		ep.setSourceIP(src_ip);	
		ep.setDestIP(dst_ip);
		
		return req;
	}

	public Monitoring createMonitoring(){
		Monitoring m = new Monitoring();
		//Liveness
		m.setLivenessBit(false);
		//General
		m.setGeneralBit(true);
		//Processing Time
		m.setProcessingTimeBit(true);
		//Overload
		m.setOverloadBit(false);
		//Incomplete
		m.setIncompleteBit(false);
		//add monitoring Id number
		m.setMonitoringIdNumber(monitoringIdNumber);
		//SET THE P FLAG
		m.setPbit(true);
		monitoringIdNumber++;
		//m.encode();
		
		
		return m;
	}
	public PccReqId createPccReqId(){
		PccReqId p_r_i = new PccReqId();
		//Add PCC Ip Address
		if (ps != null){
		if (ps.getSocket()!=null)
			p_r_i.setPCCIpAddress((Inet4Address)ps.getSocket().getInetAddress());
		else
			System.out.println("El Socket es null!!");
		}
		else
			System.out.println("ps es null!!");
		return p_r_i;
		
	}
}