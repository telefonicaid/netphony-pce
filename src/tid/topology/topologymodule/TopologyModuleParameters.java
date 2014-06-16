package tid.topology.topologymodule;


import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Parameters to configure the Topology Module
 * 
 * @author  Telefonica I+D
 *
 */
public class TopologyModuleParameters {
	/**
	 * Port the topology module is listenning to receive messages from web service 
	 */
	private int topologyModulePort;
	/**
	 * Log File of the Topology Module
	 */
	private String topologyModuleLogFile;
	/**
	 * XML File to read and generate the topology
	 */
	private String topologyFile;
	/**
	 * This parameter can have three options: fromXML, fromOSPF and both
	 * Explain the way the topology module learns the topology
	 */
	private String learnTopology="fromXML";
	/**
	 * Name of the configuration file
	 */
	private String confFile;
	
	/**
	 * Default Constructor. The configuration file is TopologyModuleParameters.xml.
	 */
	public TopologyModuleParameters(){
		confFile="TopologyModuleParameters.xml";
	}
	/**
	 * Constructor. Specify a file to read the parameters.
	 */
	public TopologyModuleParameters(String file){
		if (file != null)
			confFile= file;
		else 
			confFile="TopologyModuleParameters.xml";
	}

	/**
	 * Read the configuration file and initialize all configuration parameters.
	 */
	public void initialize(){
		
		try {

			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser saxParser = factory.newSAXParser();

			DefaultHandler handler = new DefaultHandler() {

				String tempVal;

				public void startElement(String uri, String localName,
						String qName, Attributes attributes)
								throws SAXException {
					
				}

				public void endElement(String uri, String localName,
						String qName)
								throws SAXException {
					if(qName.equalsIgnoreCase("TopologyModulePort")) {
						topologyModulePort=Integer.parseInt(tempVal.trim());
					}
					
					else if (qName.equalsIgnoreCase("TopologyModuleLogFile")) {
						topologyModuleLogFile=tempVal.trim();
					}
					
					else if (qName.equalsIgnoreCase("TopologyFile")) {
						topologyFile=tempVal.trim();
					}
					else if (qName.equalsIgnoreCase("LearnTopology")) {
						learnTopology=tempVal.trim();
					}
					
					
				
				}		   

				public void characters(char[] ch, int start, int length) throws SAXException {
					tempVal = new String(ch,start,length);
				}
			};

			saxParser.parse(confFile, handler);     

		}catch (Exception e) {
			System.err.println("Problemas al leer la configuracion");	
			e.printStackTrace();
			System.exit(1);
		}

	}
	/**
	 * Get the topologyModulePort
	 */
	public int getTopologyModulePort() {
		return topologyModulePort;
	}
	
	/**
	 * Set the topologyModulePort
	 */
	public void setTopologyModulePort(int topologyModulePort) {
		this.topologyModulePort = topologyModulePort;
	}
	/**
	 * Get topologyModuleLogFile
	 */
	public String getTopologyModuleLogFile() {
		return topologyModuleLogFile;
	}
	/**
	 * Set topologyModuleLogFile
	 */
	public void setTopologyModuleLogFile(String topologyModuleLogFile) {
		this.topologyModuleLogFile = topologyModuleLogFile;
	}
	/**
	 * Get topologyFile
	 */
	public String getTopologyFile() {
		return topologyFile;
	}
	/**
	 * Set topologyFile
	 */
	public void setTopologyFile(String topologyFile) {
		this.topologyFile = topologyFile;
	}
	/**
	 * Get learnTopology
	 */
	public String getLearnTopology() {
		return learnTopology;
	}
	/**
	 * Set learnTopology
	 */
	public void setLearnTopology(String learnTopology) {
		this.learnTopology = learnTopology;
	}
	
	
}
