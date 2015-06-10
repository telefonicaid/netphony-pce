package tid.vntm;
import java.util.logging.Logger;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;


public class VNTMParameters {

	private int VNTMPort;
	private int VNTMManagementPort;
	private String VNTMAddress;
	private Logger log=Logger.getLogger("VNTMServer");
	private int PMPort;
	private int PCEL0Port;
	private String PMAddress;
	private String PCEL0Address;
	private int PCEL2Port;
	private String MLPCEAddress;
	private int MLPCEPort;
	private String PCEL2Address;
	private String confFile;
	private String interLayerFile = "interlayerTopology.xml";

	public VNTMParameters()
	{
		setConfFile("VNTMConfiguration.xml");
	}

	public VNTMParameters(String confFile)
	{
		if (confFile != null)
			setConfFile(confFile);
		else
			confFile = "VNTMConfiguration.xml";
	}


	public int getPMPort() {
		return PMPort;
	}


	public void setPMPort(int pMPort) {
		PMPort = pMPort;
	}


	public int getPCEL0Port() {
		return PCEL0Port;
	}


	public void setPCEL0Port(int pCEL0Port) {
		PCEL0Port = pCEL0Port;
	}


	public String getPMAddress() {
		return PMAddress;
	}


	public void setPMAddress(String pMAddress) {
		PMAddress = pMAddress;
	}


	public String getPCEL0Address() {
		return PCEL0Address;
	}


	public void setPCEL0Address(String pCEL0Address) {
		PCEL0Address = pCEL0Address;
	}


	public int getVNTMPort() {
		return VNTMPort;
	}


	public void setVNTMPort(int vNTMPort) {
		VNTMPort = vNTMPort;
	}


	public int getVNTMManagementPort() {
		return VNTMManagementPort;
	}


	public void setVNTMManagementPort(int vNTMManagementPort) {
		VNTMManagementPort = vNTMManagementPort;
	}



	public String getVNTMAddress() {
		return VNTMAddress;
	}


	public void setVNTMAddress(String vNTMAddress) {
		VNTMAddress = vNTMAddress;
	}



	public int getPCEL2Port() 
	{
		return PCEL2Port;
	}


	public void setPCEL2Port(int pCEL2Port) 
	{
		PCEL2Port = pCEL2Port;
	}


	public String getPCEL2Address() 
	{
		return PCEL2Address;
	}


	public void setPCEL2Address(String pCEL2Address) {
		PCEL2Address = pCEL2Address;
	}


	public void initialize(String file){

		log.info("Inizializing VNTMParameters from file:"+file);

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
					try{
						if(qName.equalsIgnoreCase("VNTMManagementPort")) {
							VNTMManagementPort=Integer.parseInt(tempVal.trim());
						}
						else if (qName.equalsIgnoreCase("VNTMPort")){
							System.out.println("Puerto: "+Integer.parseInt(tempVal.trim()));
							VNTMPort = Integer.parseInt(tempVal.trim());
							System.out.println("Puerto: "+VNTMPort);
						}			
						else if (qName.equalsIgnoreCase("VNTMAddress")){
							VNTMAddress = tempVal.trim();
						}	
						else if (qName.equalsIgnoreCase("PMPort")){
							PMPort= Integer.parseInt(tempVal.trim());
						}
						else if (qName.equalsIgnoreCase("PMAddress")){
							PMAddress= tempVal.trim();
						}
						else if (qName.equalsIgnoreCase("PCEL0Address")){
							PCEL0Address= tempVal.trim();
						}
						else if (qName.equalsIgnoreCase("PCEL0Port")){
							PCEL0Port= Integer.parseInt(tempVal.trim());
						}
						else if (qName.equalsIgnoreCase("PCEL2Address")){
							PCEL2Address= tempVal.trim();
						}
						else if (qName.equalsIgnoreCase("PCEL2Port")){
							PCEL2Port= Integer.parseInt(tempVal.trim());
						}
						else if (qName.equalsIgnoreCase("MLPCEAddress")){
							MLPCEAddress= tempVal.trim();
						}
						else if (qName.equalsIgnoreCase("MLPCEPort")){
							MLPCEPort= Integer.parseInt(tempVal.trim());
						}
						else if (qName.equalsIgnoreCase("interLayerFile")) {
							interLayerFile=tempVal.trim();
						}
					}catch (Exception e) {
						e.printStackTrace();
					}
				}		   

				public void characters(char[] ch, int start, int length) throws SAXException {
					tempVal = new String(ch,start,length);
				}
			};
			saxParser.parse(file, handler);          
		}catch (Exception e) {
			log.warning("Problemas al leer la configuracion");	
			e.printStackTrace();
			System.exit(1);
		}


	}
	public String getConfFile() {
		return this.confFile;
	}

	public void setConfFile(String confFile) {
		this.confFile = confFile;
	}

	public String getInterLayerFile() {
		// TODO Auto-generated method stub
		return this.interLayerFile;
	}
	
	public void setInterLayerFile(String interLayerFile) {
		this.interLayerFile = interLayerFile;
	}

	public String getMLPCEAddress() {
		return MLPCEAddress;
	}

	public void setMLPCEAddress(String mLPCEAddress) {
		MLPCEAddress = mLPCEAddress;
	}

	public int getMLPCEPort() {
		return MLPCEPort;
	}

	public void setMLPCEPort(int mLPCEPort) {
		MLPCEPort = mLPCEPort;
	}
	
	
	

}


