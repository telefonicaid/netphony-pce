package tid.netManager;
import java.io.File;
import java.net.Inet4Address;
import java.util.LinkedList;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.CharacterData;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
public class NetworkLSPManagerParameters {
		private LinkedList<Inet4Address> PCETEDBAddressList;
		
		private boolean OSPF_RAW_SOCKET;
		
		private boolean OSPF_TCP_SOCKET;
		
		private LinkedList<Integer> OSPF_TCP_PORTList;
		/**
		 * Parametro utilizado para saber si la red es de tipo multilayer o no
		 */
		private boolean multilayer;
		private String networkFile;
		private String networkLSPtype;
		/**
		 * Address where we are executing the program that create a NetworkEmulator
		 */
		private Inet4Address address;
		
		Logger log=Logger.getLogger("PCCClient");


		public Inet4Address getAddress() {
			if (address==null){
				//Ver c�mo puedo sacar la direccion donde estoy
				//FIXME:
			}
			return address;
		}


		public void setAddress(Inet4Address address) {
			this.address = address;
		}


		public String getNetworkLSPtype() {
			return networkLSPtype;
		}


		public void setNetworkLSPtype(String networkLSPtype) {
			this.networkLSPtype = networkLSPtype;
		}


		public String getNetworkFile() {
			return networkFile;
		}
		
		public boolean isMultilayer() {
			return multilayer;
		}


		public boolean isOSPF_RAW_SOCKET() {
			return OSPF_RAW_SOCKET;
		}

		public void setOSPF_RAW_SOCKET(boolean oSPF_RAW_SOCKET) {
			OSPF_RAW_SOCKET = oSPF_RAW_SOCKET;
		}

		public boolean isOSPF_TCP_SOCKET() {
			return OSPF_TCP_SOCKET;
		}

		public void setOSPF_TCP_SOCKET(boolean oSPF_TCP_SOCKET) {
			OSPF_TCP_SOCKET = oSPF_TCP_SOCKET;
		}

		public LinkedList<Inet4Address> getPCETEDBAddressList() {
			return PCETEDBAddressList;
		}

		public void setPCETEDBAddressList(LinkedList<Inet4Address> pCETEDBAddressList) {
			PCETEDBAddressList = pCETEDBAddressList;
		}

		public LinkedList<Integer> getOSPF_TCP_PORTList() {
			return OSPF_TCP_PORTList;
		}

		public void setOSPF_TCP_PORTList(LinkedList<Integer> oSPF_TCP_PORTList) {
			OSPF_TCP_PORTList = oSPF_TCP_PORTList;
		}

		public void initialize(String readFile){
			File file = new File(readFile);
			log.info("Inicializando parameteros NetworkEmulator");
			try {
				DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
				Document doc = builder.parse(file);
				
				NodeList session_nl = doc.getElementsByTagName("Session");
				if (session_nl!=null){	
					PCETEDBAddressList = new LinkedList<Inet4Address>();
					OSPF_TCP_PORTList = new LinkedList<Integer>();
						
					for (int i=0; i<session_nl.getLength();i++){
						Element session_el = (Element) session_nl.item(i);
						//timeBetweenRequestIni
						NodeList PCETEDBAddress_nl = session_el.getElementsByTagName("PCETEDBAddress");
						if (PCETEDBAddress_nl!=null){
							if (PCETEDBAddress_nl.getLength()>0){					
								Element PCETEDBAddress_el = (Element) PCETEDBAddress_nl.item(0);
								String PCETEDBAddress_s = getCharacterDataFromElement(PCETEDBAddress_el);
								PCETEDBAddressList.add((Inet4Address) Inet4Address.getByName(PCETEDBAddress_s));
							}
						}
						NodeList OSPF_TCP_PORT_nl = session_el.getElementsByTagName("OSPF_TCP_PORT");
						if (OSPF_TCP_PORT_nl!=null){
							if (OSPF_TCP_PORT_nl.getLength()>0){					
								Element OSPF_TCP_PORT_el = (Element) OSPF_TCP_PORT_nl.item(0);
								String OSPF_TCP_PORT_s = getCharacterDataFromElement(OSPF_TCP_PORT_el);
								OSPF_TCP_PORTList.add(Integer.valueOf(OSPF_TCP_PORT_s));
							}
						}
					}
				}
				NodeList OSPF_RAW_SOCKET_nl = doc.getElementsByTagName("OSPF_RAW_SOCKET");
				if (OSPF_RAW_SOCKET_nl!=null){
					if (OSPF_RAW_SOCKET_nl.getLength()>0){					
						Element OSPF_RAW_SOCKET_el = (Element) OSPF_RAW_SOCKET_nl.item(0);
						String OSPF_RAW_SOCKET_s = getCharacterDataFromElement(OSPF_RAW_SOCKET_el);
						OSPF_RAW_SOCKET=Boolean.parseBoolean(OSPF_RAW_SOCKET_s);
					}
				}
				NodeList OSPF_TCP_SOCKET_nl = doc.getElementsByTagName("OSPF_TCP_SOCKET");
				if (OSPF_TCP_SOCKET_nl!=null){
					if (OSPF_TCP_SOCKET_nl.getLength()>0){					
						Element OSPF_TCP_SOCKET_el = (Element) OSPF_TCP_SOCKET_nl.item(0);
						String OSPF_TCP_SOCKET_s = getCharacterDataFromElement(OSPF_TCP_SOCKET_el);
						OSPF_TCP_SOCKET=Boolean.parseBoolean(OSPF_TCP_SOCKET_s);
					}
				}
						
				NodeList multilayer_nl = doc.getElementsByTagName("multilayer");
				if (multilayer_nl!=null){
					if (multilayer_nl.getLength()>0){					
						Element multilayer_el = (Element)multilayer_nl.item(0);
						String multilayer_s = getCharacterDataFromElement(multilayer_el);
						multilayer=Boolean.parseBoolean(multilayer_s);
					}
				}
						
				NodeList networkFile_nl = doc.getElementsByTagName("networkFile");
				if (networkFile_nl!=null){
					if (networkFile_nl.getLength()>0){					
						Element networkFile_el = (Element) networkFile_nl.item(0);
						networkFile = getCharacterDataFromElement(networkFile_el);
					}
				}
						
				NodeList NetworkLSPtype_nl = doc.getElementsByTagName("NetworkLSPtype");
				if (NetworkLSPtype_nl!=null){
					if (NetworkLSPtype_nl.getLength()>0){					
						Element NetworkLSPtype_el = (Element) NetworkLSPtype_nl.item(0);
						networkLSPtype = getCharacterDataFromElement(NetworkLSPtype_el);
					}
				}
				/*Sacar mi direccion: getLocalHost...mirar si puedo sacarla*/
						
				NodeList Address_nl = doc.getElementsByTagName("Address");
				if (Address_nl!=null){
					if (Address_nl.getLength()>0){					
						Element Address_el = (Element) Address_nl.item(0);
						String Address_s = getCharacterDataFromElement(Address_el);
						address= (Inet4Address) Inet4Address.getByName(Address_s);
						
					}
				}
						
						
//				 SAXParserFactory factory = SAXParserFactory.newInstance();
//				     SAXParser saxParser = factory.newSAXParser();
//				 
//				     DefaultHandler handler = new DefaultHandler() {
//				    	 
//				    	  String tempVal;
//				    	 
//				    	  public void startElement(String uri, String localName,
//				    		        String qName, Attributes attributes)
//				    		        throws SAXException {
//
//
//				    	  }
//				    	  
//				    	   public void endElement(String uri, String localName,
//				    		          String qName)
//				    		          throws SAXException {
//				    		   try{
//				    		          if(qName.equalsIgnoreCase("PCETEDBAddress")) {
//				    		        	  PCETEDBAddress=(Inet4Address) Inet4Address.getByName(tempVal.trim());
//				    		          }
//				    		          else if (qName.equalsIgnoreCase("OSPF_RAW_SOCKET")){				    		      
//				    		     
//				    		        	  OSPF_RAW_SOCKET = Boolean.parseBoolean(tempVal.trim());
//				    		          }
//				    		          else if (qName.equalsIgnoreCase("OSPF_TCP_SOCKET")){
//				    		        	  OSPF_TCP_SOCKET = Boolean.parseBoolean(tempVal.trim());
//				    		          }
//				    		          else if (qName.equalsIgnoreCase("OSPF_TCP_PORT")){
//				    		        	  OSPF_TCP_PORT = Integer.parseInt(tempVal.trim());
//				    		        	  
//				    		          }
//				    		          else if (qName.equalsIgnoreCase("networkFile")){
//				    		        	  networkFile= tempVal.trim();
//				    		          }
//				    		          else if (qName.equalsIgnoreCase("NetworkLSPtype")){
//				    		        	  networkLSPtype = tempVal.trim();
//				    		          }
//				    		          /*Sacar mi direccion: getLocalHost...mirar si puedo sacarla*/
//				    		          else if (qName.equalsIgnoreCase("Address")){
//				    		        	  address=(Inet4Address) Inet4Address.getByName(tempVal.trim());
//				    		          }
//				    		      	}catch (Exception e) {
//				    	    			e.printStackTrace();
//				    	    		}
//				    		        
//				    		          
//				    		    		 
//				    	   }		   
//				    	   
//				    	   public void characters(char[] ch, int start, int length) throws SAXException {
//				    			tempVal = new String(ch,start,length);
//				    		}
//				    	   };
//				
//				saxParser.parse("NetworkEmulatorConfiguration.xml", handler);     
				     
				}catch (Exception e) {
					System.err.println("Problemas al leer la configuracion");	
				    e.printStackTrace();
				    System.exit(1);
				}
		}
			
		public static String getCharacterDataFromElement(Element e) {
		Node child = e.getFirstChild();
		if (child instanceof CharacterData) {
			CharacterData cd = (CharacterData) child;
			return cd.getData();
		} else {
			return "?";
		}
	}
}


