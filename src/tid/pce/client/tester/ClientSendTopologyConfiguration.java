package tid.pce.client.tester;


import java.util.logging.Logger;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Parametros de configuracion 
 * @author mcs
 *
 */
public class ClientSendTopologyConfiguration {
	private int PCEServerPort;
	private String ipPCE = "localhost";
	private long time_ms;
	
	private Logger log;
	
	ClientSendTopologyConfiguration(String readFile){
		try{
		     SAXParserFactory factory = SAXParserFactory.newInstance();
		     SAXParser saxParser = factory.newSAXParser();
		     SaxHandler handler = new SaxHandler();
		     
		 	saxParser.parse(readFile, handler);
			 } 
			   catch (Exception e) {
			   e.printStackTrace();
			   } 
	}
	public int getPCEServerPort() {
		return PCEServerPort;
	}

	public void setPCEServerPort(int pCEServerPort) {
		PCEServerPort = pCEServerPort;
	}

	public String getIpPCE() {
		return ipPCE;
	}

	public void setIpPCE(String ipPCE) {
		this.ipPCE = ipPCE;
	}



	public long getTime_ms() {
		return time_ms;
	}

	public void setTime_ms(long time_ms) {
		this.time_ms = time_ms;
	}


	class SaxHandler extends DefaultHandler  {
		String tempVal;
		public void startElement(String uri, String localName,String qName, Attributes attrs) 
				   throws SAXParseException,SAXException {

		
			}
		public void endElement(String uri, String localName,String qName) throws SAXException {
			log = Logger.getLogger("PCCClient");			
			try{
				log.info("End Element :" + qName);
				if (qName.equalsIgnoreCase("ipPCE")){
					ipPCE = tempVal.trim();
				}
				else if (qName.equalsIgnoreCase("PCEServerPort")){
					PCEServerPort = Integer.parseInt(tempVal.trim());
				}

				else if (qName.equalsIgnoreCase("time_ms")){
					time_ms=Long.valueOf(tempVal.trim());
				}


			}catch (Exception e) {
				e.printStackTrace();
			}

		 
			}
		 
			public void characters(char ch[], int start, int length) throws SAXException {
				tempVal = new String(ch,start,length);

		 
			}
			}//End DefaultHandler



	


}
