package tid.pce.server;

import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.LinkedList;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import tid.pce.computingEngine.AlgorithmRule;
import tid.pce.computingEngine.MapAlgoRule;
import tid.pce.server.lspdb.LSP_DB;
import tid.pce.tedb.Layer;

/**
 * Configuration Parameters of the PCE.
 * @author ogondio
 *
 */
public class PCEServerParameters {

	/**
	 * TCP port where the PCE is listening for incoming pcep connections
	 */
	private int PCEServerPort = 4189;
	
	/**
	 * TCP port to connect to manage the PCE
	 */
	private int PCEManagementPort = 8888;

	/**
	 * Time betweeen updates to parent PCE. NOT USED NOW!!!!
	 */
	private long timerOSPFupdatesToParentPCE = 10000;
	
	private boolean isCompletedAuxGraph=false;
	/**
	 * Paramter meaning SSON network computation
	 */
	private boolean isSSOn= false;
	
	/**
	 * Paramter meaning WLAN network computation
	 */
	private boolean isWLAN = false;

	/**
	 * Time between updates of reachability information to parent PCE. NOT USED NOW!!!!
	 */
	private long timeSendReachabilityTime = 100000;
	
	/**
	 * Objective Function code for the Partent PCE Algorithm
	 */
	private int OFCodeParentPCE=0;
	/**
	 * Address of the parent PCE. If it is null, there is no parent PCE
	 */
	private String parentPCEAddress=null;

	/**
	 * Port of the parent PCE
	 */
	private int parentPCEPort=4189;
	/**
	 * Port of the protocol OSPF over TCP
	 */
	private int OSPFTCPPort=7779;

	/**
	 * Number of computing processors to handle requests from PCCs
	 */
	private int PCCRequestsProcessors=1;

	/**
	 * Number of computing processors to handle requests from parent PCE
	 */
	private int ParentPCERequestProcessors=1;

	/**
	 * Log file
	 */
	private String PCEServerLogFile="PCEServer.log";
	/**
	 * Log file
	 */
	private String PCEPParserLogFile="PCEPParser.log";
	/**
	 * Log file
	 */
	private String TEDBParserLogFile = "TEDBParser.log"; 
	
	private String OSPFParserLogFile = "OSPFParser.log";
	
	private String networkDescriptionFile="network_101.xml";

	/**
	 * Name of the file that describes the IT network (if there is any)
	 */
	private String ITnetworkDescriptionFile="network_IT_101.xml";

	/**
	 * KeepAlive Timer of the pcep session
	 */
	private int KeepAliveTimer=30;

	/**
	 * Minimum keepalive timer accepted from the peer PCE/PCC 
	 */
	private int minKeepAliveTimerPCCAccepted=2;

	/**
	 * Maximum DeadTimer accepted from the peer PCE/PCC 
	 */
	private int maxDeadTimerPCCAccepted=30000;

	/**
	 * If a deadTimer of 0 is accepted from the peer PCE/PCC 
	 */
	private boolean zeroDeadTimerPCCAccepted=false;

	/**
	 * Dead timer of the pcep session 
	 */
	private int DeadTimer=120;

	/**
	 * Default layer of the PCE
	 */
	private Layer defaultPCELayer;

	/**
	 * If the PCE is IT capable
	 */
	public boolean ITcapable=false;

	/**
	 * Layers of the PCE
	 */
	public LinkedList<Layer> PCElayers;

	/**
	 * List of algorithms with rules to select them
	 */
	public LinkedList<MapAlgoRule> algorithmRuleList;

	/**
	 * Name of the configuration file
	 */
	private String confFile;

	/**
	 * If the tcp no delay option is used or not.
	 */
	private boolean nodelay=false;

	/**
	 * If the experimental optimized method to read is used. EXPERIMENTAL ONLY 
	 */
	private boolean optimizedRead=false;

	/**
	 * If OSPF with raw socket is used to receive topology.
	 */
	private boolean OSPFSession=false;
	
	/**
	 * If a TCP socket, sending OSPF packets over it is used to receive topology.
	 */
	private boolean OSPFTCPSession=false;
	/**
	 * IP Address from which the OSPF is listen 
	 */
	private String OSPFListenerIP = "localhost"; 
	/**
	 * If it is multicast OSPF
	 */
	private boolean OSPFMulticast=false;
	/**
	 * If it is unicast OSPF
	 */
	private boolean OSPFUnicast=false;
	/**
	 * If the request Time is analyzed (for statistics only)
	 */
	private boolean analyzeRequestTime=false;
	
	private boolean setTraces=true;

	/**
	 * If reservation is allowed
	 */
	private boolean reservation=false;

	/**
	 * If there is maximum Queing Time in the PCE. If a requests stays more time in the queue, it is rejected.
	 */
	private boolean useMaxQueingTime=false;

	private boolean multilayer=false;
	
	private boolean multidomain=false;
	
	private boolean isStateful=false;
	
	private boolean isSRCapable=false;
	
	private int MSD=0;
	
	private LSP_DB lspDB;
	
	private String controllerIP;
	
	private String controllerPORT;
	
	private String topologyPath;
	
	private String interDomainFile;
	
	private String controllerListFile;
	
	
	public boolean isSSOn() {
		return isSSOn;
	}

	public void setSSOn(boolean isSSOn) {
		this.isSSOn = isSSOn;
	}

	private String layer=null;
	/**
	 * STRONGEST: Variables used to indicate which lambda subset is being used. 
	 */
	private int lambdaIni=0;
	private int lambdaEnd=Integer.MAX_VALUE;
	
	/**
	 * STRONGEST: collabotarive PCEs
	 */
	private boolean collaborativePCEs=false;
	private boolean primary=false;
	private String ipPrimaryPCE;
	//Solo si estan los dos PCEs en el mismo equipo!! es para pruebas!!
	private int portPrimaryPCE=4191;
	private Inet4Address IPBackupPCE;
	
	private int idAlgo;
	
	/**
	 * ONE: Topology Module
	 */
	private boolean topologyModuleOption;
	/**
	 * BGP. This variable indeicates if the PCE has a BGP module 
	 */
	private boolean actingAsBGP4Peer;
	/**
	 * File where read the BGP parameters to configure
	 */
	private String BGP4File = "BGP4Parameters.xml";
	
	/**
	 * PCE Address if located in machine with more than one network interface
	 */
	
	private String localPceAddress = "127.0.0.1";
	
	/**
	 * If PCE is stateful
	 */
	//boolean isStatefulPCE = false;
	
	/**
	 * Initialize TED from file
	 */
	
	public boolean initFromFile = true;

	protected boolean isActive = false;
	
	
	/**
	 * Default Constructor. The configuration file is PCEServerConfiguration.xml.
	 */
	public PCEServerParameters(){
		confFile="PCEServerConfiguration.xml";
	}

	/**
	 * Constructor with the name of the configuration file.
	 * @param confFile Name of the configuration file.
	 */
	public PCEServerParameters(String confFile){
		if (confFile!=null){
			this.confFile=confFile;
		}else {
			confFile="PCEServerConfiguration.xml";
		}
	}


	/**
	 * Read the configuration file and initialize all configuration parameters.
	 */
	public void initialize(){
		//Create list of algorithm Rules
		algorithmRuleList=	new LinkedList<MapAlgoRule>();

		//Layers supported by the PCE
		PCElayers=new LinkedList<Layer>();

		try {

			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser saxParser = factory.newSAXParser();

			DefaultHandler handler = new DefaultHandler() {

				String tempVal;

				public void startElement(String uri, String localName,
						String qName, Attributes attributes)
								throws SAXException {
					if (qName.equalsIgnoreCase("layer")) {
						Layer lay= new Layer();
						String layer2=attributes.getValue("type");    		        	  
						if (layer2.equals("gmpls")){
							lay.gmpls=true;
							lay.encodingType=Integer.parseInt(attributes.getValue("encodingType"));
							lay.switchingType=Integer.parseInt(attributes.getValue("switchingType"));
						}
						boolean defaultL=Boolean.parseBoolean(attributes.getValue("default"));
						if (defaultL==true){
							defaultPCELayer=lay;
							layer=layer2;
						}
						PCElayers.add(lay);

					}	  

					else if (qName.equalsIgnoreCase("algorithmRule")) {
						MapAlgoRule mar= new MapAlgoRule();
						AlgorithmRule ar=new AlgorithmRule();
						String aname=attributes.getValue("name");
						ar.of=Integer.parseInt(attributes.getValue("of"));
						ar.svec=Boolean.parseBoolean(attributes.getValue("svec"));		        		  
						mar.ar=ar;
						mar.algoName=aname;
						mar.isParentPCEAlgorithm=Boolean.parseBoolean(attributes.getValue("isParentPCEAlgorithm"));
						mar.isWSONAlgorithm=Boolean.parseBoolean(attributes.getValue("isWSONAlgorithm"));
						mar.isSSSONAlgorithm=Boolean.parseBoolean(attributes.getValue("isSSONAlgorithm"));
						mar.isWLANAlgorithm = Boolean.parseBoolean(attributes.getValue("isWLANAlgorithm"));
						isSSOn = mar.isSSSONAlgorithm;
						isWLAN = mar.isWLANAlgorithm;
						algorithmRuleList.add(mar);
					}
					
					else if (qName.equalsIgnoreCase("controller")) {
						controllerIP = attributes.getValue("ip");
						controllerPORT = attributes.getValue("port");
						topologyPath = attributes.getValue("topologyPath");
						interDomainFile = attributes.getValue("interDomainFile");
						controllerListFile = attributes.getValue("controllerListFile");
					}

				}

				public void endElement(String uri, String localName,
						String qName)
								throws SAXException {
					if(qName.equalsIgnoreCase("PCEServerPort")) {
						PCEServerPort=Integer.parseInt(tempVal.trim());
					}else if(qName.equalsIgnoreCase("LocalPCEAddress")){
						localPceAddress=tempVal.trim();
					}else if (qName.equalsIgnoreCase("PCEManagementPort")){
						PCEManagementPort = Integer.parseInt(tempVal.trim());
					}else if (qName.equalsIgnoreCase("OFCodeParentPCE")){
						OFCodeParentPCE=Integer.parseInt(tempVal.trim());
					}
					else if (qName.equalsIgnoreCase("parentPCEAddress")) {
						parentPCEAddress=tempVal.trim();
					}
					else if (qName.equalsIgnoreCase("parentPCEPort")) {
						parentPCEPort=Integer.parseInt(tempVal.trim());
					}
					else if (qName.equalsIgnoreCase("PCCRequestsProcessors")) {
						PCCRequestsProcessors=Integer.parseInt(tempVal.trim());
					}
					else if (qName.equalsIgnoreCase("ParentPCERequestProcessors")) {
						ParentPCERequestProcessors=Integer.parseInt(tempVal.trim());
					}
					else if (qName.equalsIgnoreCase("PCEServerLogFile")) {
						PCEServerLogFile=tempVal.trim();
					}
					else if (qName.equalsIgnoreCase("nodelay")) {
						nodelay=Boolean.parseBoolean(tempVal.trim());
					}
					else if (qName.equalsIgnoreCase("multilayer")) {
						multilayer=Boolean.parseBoolean(tempVal.trim());
					}
					else if (qName.equalsIgnoreCase("multidomain")) {
						multidomain=Boolean.parseBoolean(tempVal.trim());
					}
					else if (qName.equalsIgnoreCase("useMaxReqTime")) {
						useMaxQueingTime=Boolean.parseBoolean(tempVal.trim());
					}
					else if (qName.equalsIgnoreCase("reservation")) {
						reservation=Boolean.parseBoolean(tempVal.trim());
					}
					else if (qName.equalsIgnoreCase("optimizedRead")) {
						optimizedRead=Boolean.parseBoolean(tempVal.trim());
					}else if (qName.equalsIgnoreCase("OSPFSession")) {
						OSPFSession=Boolean.parseBoolean(tempVal.trim());
					}else if (qName.equalsIgnoreCase("OSPFListenerIP")){
							OSPFListenerIP=tempVal.trim();							
					}
					else if (qName.equalsIgnoreCase("OSPFMulticast")){
							OSPFMulticast=Boolean.parseBoolean(tempVal.trim());						
					}
					else if (qName.equalsIgnoreCase("OSPFUnicast")){
							OSPFUnicast=Boolean.parseBoolean(tempVal.trim());					
					}
					else if (qName.equalsIgnoreCase("OSPFTCPSession")) {
						OSPFTCPSession=Boolean.parseBoolean(tempVal.trim());
					}					
					else if (qName.equalsIgnoreCase("OSPFTCPPort")) {
						OSPFTCPPort=Integer.parseInt(tempVal.trim());
					}
					else if (qName.equalsIgnoreCase("analyzeRequestTime")) {
						analyzeRequestTime=Boolean.parseBoolean(tempVal.trim());					}

					else if (qName.equalsIgnoreCase("PCEPParserLogFile")) {
						PCEPParserLogFile=tempVal.trim();
					}	
					else if (qName.equalsIgnoreCase("TEDBParserLogFile")) {
						PCEPParserLogFile=tempVal.trim();
					}
					else if (qName.equalsIgnoreCase("setTraces")) {
						setTraces=Boolean.parseBoolean(tempVal.trim());
					}					
					else if (qName.equalsIgnoreCase("networkDescriptionFile")) {
						networkDescriptionFile=tempVal.trim();
					}
					else if (qName.equalsIgnoreCase("isActive")) {
						isActive=Boolean.parseBoolean(tempVal.trim());
					}
					else if (qName.equalsIgnoreCase("isStateful")) {
						isStateful=Boolean.parseBoolean(tempVal.trim());
					}
					else if (qName.equalsIgnoreCase("isSRCapable")) {
						isSRCapable=Boolean.parseBoolean(tempVal.trim());
					}
					else if (qName.equalsIgnoreCase("MSD")){
						MSD = Integer.parseInt(tempVal.trim());
					}
					else if (qName.equalsIgnoreCase("timeSendTopologyTask")) {
						timerOSPFupdatesToParentPCE=Long.parseLong(tempVal.trim());
					}	
					else if (qName.equalsIgnoreCase("timeSendReachabilityTask")) {
						timeSendReachabilityTime=Long.parseLong(tempVal.trim());
					}	
					else if (qName.equalsIgnoreCase("lambdaIni")) {
						lambdaIni=Integer.parseInt(tempVal.trim());
					}	
					else if (qName.equalsIgnoreCase("lambdaEnd")) {
						lambdaEnd=Integer.parseInt(tempVal.trim());
					}
					else if (qName.equalsIgnoreCase("isCompletedAuxGraph")) {
						isCompletedAuxGraph=Boolean.parseBoolean(tempVal.trim());
					}					
					else if (qName.equalsIgnoreCase("actingAsBGP4Peer")) {
						actingAsBGP4Peer=Boolean.parseBoolean(tempVal.trim());
					}
					else if (qName.equalsIgnoreCase("BGP4File")) {					
						BGP4File=tempVal.trim();					
						}					
					else if (isCompletedAuxGraph==true){
						if (qName.equalsIgnoreCase("idAlgo")) {
							idAlgo=Integer.parseInt(tempVal.trim());
						}
					}
					else if (qName.equalsIgnoreCase("ipPrimaryPCE")) {							
						collaborativePCEs=true;
						ipPrimaryPCE=tempVal.trim();						
					}
					else if (qName.equalsIgnoreCase("isPrimaryPCE")) {		
						primary=Boolean.parseBoolean(tempVal.trim());
						collaborativePCEs=true;
											
					}
					else if (qName.equalsIgnoreCase("portPrimaryPCE")) {							
						portPrimaryPCE=Integer.parseInt(tempVal.trim());
											
					}
					else if (qName.equalsIgnoreCase("IPBackupPCE")){
						try {
							IPBackupPCE=(Inet4Address) Inet4Address.getByName(tempVal.trim());
						} catch (UnknownHostException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					/*else if(qName.equalsIgnoreCase("isStatefulPCE")) {		
						isStatefulPCE=Boolean.parseBoolean(tempVal.trim());
					}*/
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
	
	//////////////////////////////////////////////
	//  GETTERS AND SETTERS						// 
	//////////////////////////////////////////////
	
	public String getTEDBParserLogFile() {
		return TEDBParserLogFile;
	}

	public void setTEDBParserLogFile(String tEDBLogFile) {
		TEDBParserLogFile = tEDBLogFile;
	}

	public String getOSPFParserLogFile() {
		return OSPFParserLogFile;
	}

	public void setOSPFParserLogFile(String oSPFParserLogFile) {
		OSPFParserLogFile = oSPFParserLogFile;
	}

	public boolean isOptimizedRead() {
		return optimizedRead;
	}

	public void setOptimizedRead(boolean optimizedRead) {
		this.optimizedRead = optimizedRead;
	}

	public boolean isNodelay() {
		return nodelay;
	}

	public void setNodelay(boolean nodelay) {
		this.nodelay = nodelay;
	}

	public int getPCEServerPort() {
		return PCEServerPort;
	}

	public void setPCEServerPort(int pCEServerPort) {
		PCEServerPort = pCEServerPort;
	}

	public String getParentPCEAddress() {
		return parentPCEAddress;
	}

	public void setParentPCEAddress(String parentPCEAddress) {
		this.parentPCEAddress = parentPCEAddress;
	}

	public int getParentPCEPort() {
		return parentPCEPort;
	}

	public void setParentPCEPort(int parentPCEPort) {
		this.parentPCEPort = parentPCEPort;
	}


	public int getPCCRequestsProcessors() {
		return PCCRequestsProcessors;
	}

	public void setPCCRequestsProcessors(int pCCRequestsProcessors) {
		PCCRequestsProcessors = pCCRequestsProcessors;
	}

	public int getParentPCERequestProcessors() {
		return ParentPCERequestProcessors;
	}

	public void setParentPCERequestProcessors(int parentPCERequestProcessors) {
		ParentPCERequestProcessors = parentPCERequestProcessors;
	}

	public String getPCEServerLogFile() {
		return PCEServerLogFile;
	}

	public void setPCEServerLogFile(String pCEServerLogFile) {
		PCEServerLogFile = pCEServerLogFile;
	}

	public String getPCEPParserLogFile() {
		return PCEPParserLogFile;
	}

	public void setPCEPParserLogFile(String pCEPParserLogFile) {
		PCEPParserLogFile = pCEPParserLogFile;
	}

	public String getNetworkDescriptionFile() {
		return networkDescriptionFile;
	}

	public void setNetworkDescriptionFile(String networkDescriptionFile) {
		this.networkDescriptionFile = networkDescriptionFile;
	}

	public String getITNetworkDescriptionFile() {
		return ITnetworkDescriptionFile;
	}

	public void setITNetworkDescriptionFile(String ITnetworkDescriptionFile) {
		this.ITnetworkDescriptionFile = ITnetworkDescriptionFile;
	}

	public long getTimeSendTopologyTask() {
		return timerOSPFupdatesToParentPCE;
	}

	public long getTimeSendReachabilityTime() {
		return timeSendReachabilityTime;
	}

	public int getMinKeepAliveTimerPCCAccepted() {
		return minKeepAliveTimerPCCAccepted;
	}

	public void setMinKeepAliveTimerPCCAccepted(int minKeepAliveTimerPCCAccepted) {
		this.minKeepAliveTimerPCCAccepted = minKeepAliveTimerPCCAccepted;
	}

	public int getMaxDeadTimerPCCAccepted() {
		return maxDeadTimerPCCAccepted;
	}

	public void setMaxDeadTimerPCCAccepted(int maxDeadTimerPCCAccepted) {
		this.maxDeadTimerPCCAccepted = maxDeadTimerPCCAccepted;
	}

	public boolean isZeroDeadTimerPCCAccepted() {
		return zeroDeadTimerPCCAccepted;
	}

	public void setZeroDeadTimerPCCAccepted(boolean zeroDeadTimerPCCAccepted) {
		this.zeroDeadTimerPCCAccepted = zeroDeadTimerPCCAccepted;
	}

	public int getKeepAliveTimer() {
		return KeepAliveTimer;
	}

	public void setKeepAliveTimer(int keepAliveTimer) {
		KeepAliveTimer = keepAliveTimer;
	}

	public int getOSPFTCPPort() {
		return OSPFTCPPort;
	}

	public void setOSPFTCPPort(int oSPFTCPPort) {
		OSPFTCPPort = oSPFTCPPort;
	}

	public int getDeadTimer() {
		return DeadTimer;
	}

	public void setDeadTimer(int deadTimer) {
		DeadTimer = deadTimer;
	}

	public Layer getDefaultPCELayer() {
		return defaultPCELayer;
	}

	public void setDefaultPCELayer(Layer defaultPCELayer) {
		this.defaultPCELayer = defaultPCELayer;
	}
	public int getPCEManagementPort() {
		return PCEManagementPort;
	}

	public boolean isReservation() {
		return reservation;
	}

	public void setReservation(boolean reservation) {
		this.reservation = reservation;
	}

	public boolean isUseMaxReqTime() {
		return useMaxQueingTime;
	}

	public void setUseMaxReqTime(boolean useMaxReqTime) {
		this.useMaxQueingTime = useMaxReqTime;
	}
	
/** OSPF **/
	public boolean isOSPFSession() {
		return OSPFSession;
	}

	public void setOSPFSession(boolean oSPFSession) {
		OSPFSession = oSPFSession;
	}

	public boolean isOSPFTCPSession() {
		return OSPFTCPSession;
	}

	public void setOSPFTCPSession(boolean oSPFTCPSession) {
		OSPFTCPSession = oSPFTCPSession;
	}
	public String getOSPFListenerIP() {
		return OSPFListenerIP;
	}

	public void setOSPFListenerIP(String oSPFListenerIP) {
		OSPFListenerIP = oSPFListenerIP;
	}

	public boolean isOSPFMulticast() {
		return OSPFMulticast;
	}

	public void setOSPFMulticast(boolean oSPFMulticast) {
		OSPFMulticast = oSPFMulticast;
	}

	public boolean isOSPFUnicast() {
		return OSPFUnicast;
	}

	public void setOSPFUnicast(boolean oSPFUnicast) {
		OSPFUnicast = oSPFUnicast;
	}
	
	public boolean isAnalyzeRequestTime() {
		return analyzeRequestTime;
	}

	public void setAnalyzeRequestTime(boolean analyzeRequestTime) {
		this.analyzeRequestTime = analyzeRequestTime;
	}

	public boolean isSetTraces() {
		return setTraces;
	}

	public boolean isMultilayer() {
		return multilayer;
	}

	public void setMultilayer(boolean multilayer) {
		this.multilayer = multilayer;
	}

	public boolean isMultidomain() {
		return multidomain;
	}

	public void setMultidomain(boolean multidomain) {
		this.multidomain = multidomain;
	}

	public String getLayer() {
		return layer;
	}

	public void setLayer(String layer) {
		this.layer = layer;
	}

	public int getLambdaIni() {
		return lambdaIni;
	}

	public void setLambdaIni(int lambdaIni) {
		this.lambdaIni = lambdaIni;
	}

	public int getLambdaEnd() {
		return lambdaEnd;
	}

	public void setLambdaEnd(int lambdaEnd) {
		this.lambdaEnd = lambdaEnd;
	}
	
	/**** STRONGEST: Collaborative PCEs ****/
	public boolean isCollaborativePCEs() {
		return collaborativePCEs;
	}

	public void setCollaborativePCEs(boolean collaborativePCEs) {
		this.collaborativePCEs = collaborativePCEs;
	}

	public boolean isPrimary() {
		return primary;
	}

	public void setPrimary(boolean primary) {
		this.primary = primary;
	}

	public String getIpPrimaryPCE() {
		return ipPrimaryPCE;
	}

	public void setIpPrimaryPCE(String ipPrimaryPCE) {
		this.ipPrimaryPCE = ipPrimaryPCE;
	}

	public int getPortPrimaryPCE() {
		return portPrimaryPCE;
	}
	
	public void setPortPrimaryPCE(int portPrimaryPCE) {
		this.portPrimaryPCE = portPrimaryPCE;
	}

	public Inet4Address getIPBackupPCE() {
		return IPBackupPCE;
	}

	

	/**
	 * ONE
	 */
	public boolean isTopologyModuleOption() {
		return topologyModuleOption;
	}

	public boolean isCompletedAuxGraph() {
		return isCompletedAuxGraph;
	}

	public void setCompletedAuxGraph(boolean isCompletedAuxGraph) {
		this.isCompletedAuxGraph = isCompletedAuxGraph;
	}

	public int getIdAlgo() {
		return idAlgo;
	}

	public void setIdAlgo(int idAlgo) {
		this.idAlgo = idAlgo;
	}
	
	public boolean isStateful() {
		return isStateful;
	}

	public void setStateful(boolean isStateful) {
		this.isStateful = isStateful;
	}

	public boolean isSRCapable() {
		return isSRCapable;
	}
	
	public int getMSD() 
	{
		return MSD;
	}
	

	public void setSRCapable(boolean isSRCapable) {
		this.isSRCapable = isSRCapable;
	}
	
	public void setSRCapable(int MSD) {
		this.isSRCapable = (MSD>=0);
		this.MSD = MSD;
	}
	
	
	public boolean isActive() {
		return isActive;
	}

	public void setActive(boolean isActive) {
		this.isActive = isActive;
	}

	public boolean isActingAsBGP4Peer() {
		return actingAsBGP4Peer;
	}

	public String getBGP4File() {
		return BGP4File;
	}

	public String getLocalPceAddress() {
		return localPceAddress;
	}

	public void setLocalPceAddress(String localPceAddress) {
		this.localPceAddress = localPceAddress;
	}

	/*public boolean isStatefulPCE() {
		return isStatefulPCE;
	}*/

	/*public void setStatefulPCE(boolean isStatefulPCE) {
		this.isStatefulPCE = isStatefulPCE;
	}*/

	public LSP_DB getLspDB() {
		return lspDB;
	}

	public void setLspDB(LSP_DB lspDB) {
		this.lspDB = lspDB;
	}

	public boolean isWLAN() {
		return isWLAN;
	}

	public void setWLAN(boolean isWLAN) {
		this.isWLAN = isWLAN;
	}

	public String getControllerIP() {
		return controllerIP;
	}

	public void setControllerIP(String controllerIP) {
		this.controllerIP = controllerIP;
	}

	public String getControllerPORT() {
		return controllerPORT;
	}

	public void setControllerPORT(String controllerPORT) {
		this.controllerPORT = controllerPORT;
	}

	public String getTopologyPath() {
		return topologyPath;
	}

	public void setTopologyPath(String topologyPath) {
		this.topologyPath = topologyPath;
	}

	public String getInterDomainFile() {
		return interDomainFile;
	}

	public void setInterDomainFile(String interDomainFile) {
		this.interDomainFile = interDomainFile;
	}

	public String getControllerListFile() {
		return controllerListFile;
	}

	public void setControllerListFile(String controllerListFile) {
		this.controllerListFile = controllerListFile;
	}	
	
}
