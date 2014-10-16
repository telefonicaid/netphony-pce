package tid.pce.client.tester;

import java.io.File;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.CharacterData;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import tid.pce.client.tester.restoration.RestorationCaseInformation;
import tid.pce.computingEngine.MapAlgoRule;
import cern.jet.random.engine.MersenneTwister;
import es.tid.ospf.ospfv2.lsa.tlv.subtlv.MaximumReservableBandwidth;

/**
 * Parametros de configuracion del cliente
 * @author mcs
 *
 */
public class InformationRequest {
	private PCCPCEPSessionParameters PCCPCEPsessionParams;
	//Ip VNTM
	private String ipVNTM;
	private int counter;
	private boolean isExponential=false;
	private LinkedList<MapAlgoRule> algorithmRuleList;
	private MaximumReservableBandwidth maximumReservableBandwidth; 
	private boolean isVariableBandwidth=false;

	private int seed=0;
	private long stopCondition=0;

	private float bandwidth=0;
	private float bandwidthMax=0;
	private float bandwidthMin =0;
	//Variable Svec
	private boolean svec=false;
	
	private String baseIP="172.20.1";
	private String classToExecute;
	private boolean networkEmulator=false;
	private boolean VNTMSession=false;
	private String VNTMFile = "VNTMConfiguration.xml";
	private LinkedList<RequestToSend> requestToSendList;
	private Logger log=Logger.getLogger("PCCClient");
	private int maxNumberIterations;
	//Variable para poner las trazas del cliente
	private long printStatisticsTime=10000;
	private long maxTimeWaitingForResponse_ms = -1;
	private long semillaBW = -1;
	private long semillaTiempos = -1;
	private int NumfileBW=0;
	
	//Varias ejecuciones, cambiando el tiempo entre Rquests
	private boolean timeBetweenRequestChange=false;
	private double timeBetweenRequestIni;
	private double timeBetweenRequestMax;
	private double timeBetweenRequestStep;
	private double meanTimeBetweenRequest;//En miliseconds
	private boolean staticConnections = false;
	
	//Varias ejecuciones, cambiando el tiempo de conexion
	private boolean conectionTimeChange=false;
	private double conectionTimeIni;
	private double conectionTimeMax;
	private double conectionTimeStep;
	private double meanConectionTime;
	
	//Varias ejecuciones, cambiando load
	private boolean loadChange=false;
	private double loadIni;
	private double loadMax;
	private double loadStep;

	//Para listas
	private ArrayList<Double> meanTimeBetweenRequestList;
	private ArrayList<Double> meanConectionTimeList;
	//Number nodes
	private int numberNodes;
	//Port Management Client
	private int managementClientPort=8899;

	private boolean reservation;
	private boolean loadBalancing=false;
	private boolean setTraces=true;

	/**
	 * Case emulate restoration
	 */	
	private RestorationCaseInformation restorationCaseInformation;
	private String nameRestorationCaseFile;
	private String executionNumber="1";
	/**
	 * Option to emulate a control plane
	 */
	private boolean controlPlaneOption = false;
	public InformationRequest() {
		requestToSendList = new  LinkedList<RequestToSend>();
		//meanTimeBetweenRequestList = new ArrayList<Double>();
		algorithmRuleList = new LinkedList<MapAlgoRule>();
		maximumReservableBandwidth= new MaximumReservableBandwidth();
		//meanConectionTimeList = new ArrayList<Double>();
		//reqParams = new RequestParameters();
		meanTimeBetweenRequestList=new ArrayList<Double>();
		meanConectionTimeList=new ArrayList<Double>();
	}

	public ArrayList<Double> getMeanTimeBetweenRequestList() {
		return meanTimeBetweenRequestList;
	}

	public void setMeanTimeBetweenRequestList(
			ArrayList<Double> meanTimeBetweenRequestList) {
		this.meanTimeBetweenRequestList = meanTimeBetweenRequestList;
	}

	public ArrayList<Double> getMeanConectionTimeList() {
		return meanConectionTimeList;
	}

	public void setMeanConectionTimeList(ArrayList<Double> meanConectionTimeList) {
		this.meanConectionTimeList = meanConectionTimeList;
	}

	public boolean isReservation() {
		return reservation;
	}

	public void setReservation(boolean reservation) {
		this.reservation = reservation;
	}

	public String getIpVNTM() {
		return ipVNTM;
	}

	public void setIpVNTM(String ipVNTM) {
		this.ipVNTM = ipVNTM;
	}

	public LinkedList<RequestToSend> getRequestToSendList() {
		return requestToSendList;
	}

	public void setRequestToSendList(LinkedList<RequestToSend> requestToSendList) {
		this.requestToSendList = requestToSendList;
	}

	public int getMaxNumberIterations() {
		return maxNumberIterations;
	}

	public void setMaxNumberIterations(int maxNumberIterations) {
		this.maxNumberIterations = maxNumberIterations;
	}

	public String getVNTMFile() {
		return VNTMFile;
	}

	public void setVNTMFile(String vNTMFile) {
		VNTMFile = vNTMFile;
	}

	public boolean isNetworkEmulator() {
		return networkEmulator;
	}

	public void setNetworkEmulator(boolean networkEmulator) {
		this.networkEmulator = networkEmulator;
	}

	public double getLoadIni() {
		return loadIni;
	}

	public void setLoadIni(double loadIni) {
		this.loadIni = loadIni;
	}

	public double getLoadMax() {
		return loadMax;
	}

	public float getBandwidth() {
		return bandwidth;
	}
	
	public boolean isStaticConnections() {
		return staticConnections;
	}

	public void setStaticConnections(boolean staticConnections) {
		this.staticConnections = staticConnections;
	}

	public void setBandwidth(float bandwidth) {
		this.bandwidth = bandwidth;
	}
	public float getBandwidthMax() {
		return bandwidthMax;
	}
	public void setBandwidthMax(float bandwidthMax) {
		this.bandwidthMax = bandwidthMax;
	}


	public float getBandwidthMin() {
		return bandwidthMin;
	}


	public void setBandwidthMin(float bandwidthMin) {
		this.bandwidthMin = bandwidthMin;
	}


	public boolean isVariableBandwidth() {
		return isVariableBandwidth;
	}


	public void setVariableBandwidth(boolean isVariableBandwidth) {
		this.isVariableBandwidth = isVariableBandwidth;
	}
	public void setLoadMax(double loadMax) {
		this.loadMax = loadMax;
	}

	public double getLoadStep() {
		return loadStep;
	}

	public void setLoadStep(double loadStep) {
		this.loadStep = loadStep;
	}

	public long getPrintStatisticsTime() {
		return printStatisticsTime;
	}

	public boolean isTimeBetweenRequestChange() {
		return timeBetweenRequestChange;
	}

	public void setTimeBetweenRequestChange(boolean timeBetweenRequestChange) {
		this.timeBetweenRequestChange = timeBetweenRequestChange;
	}

	public double getTimeBetweenRequestIni() {
		return timeBetweenRequestIni;
	}

	public void setTimeBetweenRequestIni(double timeBetweenRequestIni) {
		this.timeBetweenRequestIni = timeBetweenRequestIni;
	}

	public double getTimeBetweenRequestMax() {
		return timeBetweenRequestMax;
	}

	public void setTimeBetweenRequestMax(double timeBetweenRequestMax) {
		this.timeBetweenRequestMax = timeBetweenRequestMax;
	}

	public double getTimeBetweenRequestStep() {
		return timeBetweenRequestStep;
	}

	public void setTimeBetweenRequestStep(double timeBetweenRequestStep) {
		this.timeBetweenRequestStep = timeBetweenRequestStep;
	}

	public boolean isConectionTimeChange() {
		return conectionTimeChange;
	}

	public void setConectionTimeChange(boolean conectionTimeChange) {
		this.conectionTimeChange = conectionTimeChange;
	}



	public double getConectionTimeIni() {
		return conectionTimeIni;
	}

	public void setConectionTimeIni(double conectionTimeIni) {
		this.conectionTimeIni = conectionTimeIni;
	}

	public double getConectionTimeMax() {
		return conectionTimeMax;
	}

	public void setConectionTimeMax(double conectionTimeMax) {
		this.conectionTimeMax = conectionTimeMax;
	}

	public boolean isVNTMSession() {
		return VNTMSession;
	}

	public void setVNTMSession(boolean vNTMSession) {
		VNTMSession = vNTMSession;
	}

	public double getConectionTimeStep() {
		return conectionTimeStep;
	}

	public void setConectionTimeStep(double conectionTimeStep) {
		this.conectionTimeStep = conectionTimeStep;
	}

	public boolean isLoadChange() {
		return loadChange;
	}

	public void setLoadChange(boolean loadChange) {
		this.loadChange = loadChange;
	}

	public void setPrintStatisticsTime(long printStatisticsTime) {
		this.printStatisticsTime = printStatisticsTime;
	}

	public double calculateLoad(){	
		//load=lambda/u;
		double load;
		load=meanConectionTime/meanTimeBetweenRequest;//*numberNodes*(numberNodes-1));
		return load;
	}

	public long getStopCondition() {
		return stopCondition;
	}

	public void setStopCondition(long stopCondition) {
		this.stopCondition = stopCondition;
	}

	public void addAlgorithmRuleListElement(MapAlgoRule time){
		algorithmRuleList.add(time);
	}

	public int getCounter() {
		return counter;
	}

	public boolean getIsExponential() {
		return isExponential;
	}

	public LinkedList<MapAlgoRule> getAlgorithmRuleList() {
		return algorithmRuleList;
	}

	public void setAlgorithmRuleList(LinkedList<MapAlgoRule> algorithmRuleList) {
		this.algorithmRuleList = algorithmRuleList;
	}
	
	public boolean isLoadBalancing() {
		return loadBalancing;
	}

	public double getMeanTimeBetweenRequest() {
		return meanTimeBetweenRequest;
	}

	public void setMeanTimeBetweenRequest(double meanTimeBetweenRequest) {
		this.meanTimeBetweenRequest = meanTimeBetweenRequest;
	}

	public double getMeanConectionTime() {
		return meanConectionTime;
	}

	public void setMeanConectionTime(double meanConectionTime) {
		this.meanConectionTime = meanConectionTime;
	}

	public int getSeed() {
		return seed;
	}

	public void setSeed(int seed) {
		this.seed = seed;
	}

	public String getBaseIP() {
		return baseIP;
	}

	public void setBaseIP(String baseIP) {
		this.baseIP = baseIP;
	}

	public long getMaxTimeWaitingForResponse_ms() {
		return maxTimeWaitingForResponse_ms;
	}

	public void setMaxTimeWaitingForResponse_ms(long maxTimeWaitingForResponse_ms) {
		this.maxTimeWaitingForResponse_ms = maxTimeWaitingForResponse_ms;
	}

	public long getSemillaBW() {
		return semillaBW;
	}


	public void setSemillaBW(long semillaBW) {
		this.semillaBW = semillaBW;
	}
	
	public long getSemillaTiempos() {
		return semillaTiempos;
	}


	public void setSemillaTiempos(long semillaTiempos) {
		this.semillaTiempos = semillaTiempos;
	}
	
	public int getNumfileBW() {
		return NumfileBW;
	}

	public void setNumfileBW(int numfileBW){
		NumfileBW = numfileBW;
	}

	public PCCPCEPSessionParameters getPCCPCEPsessionParams(){
		return PCCPCEPsessionParams;
	}

	public void setPCCPCEPsessionParams(
			PCCPCEPSessionParameters pCCPCEPsessionParams){
		PCCPCEPsessionParams = pCCPCEPsessionParams;
	}

	public int getManagementClientPort(){
		return managementClientPort;
	}

	public void readFile (String readFile){
		PCCPCEPsessionParams =  new PCCPCEPSessionParameters();
		File file = new File(readFile);
		try {
			DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document doc = builder.parse(file);
			NodeList PCCPCEPSession_nl = doc.getElementsByTagName("PCCPCEPSession");
			if (PCCPCEPSession_nl!=null){
				Element PCCPCEPSession_el = (Element) PCCPCEPSession_nl.item(0);				
				
				NodeList noDelay_nl = PCCPCEPSession_el.getElementsByTagName("noDelay");
				if (noDelay_nl!=null){
					if (noDelay_nl.getLength()>0){					
						Element noDelay_el = (Element) noDelay_nl.item(0);
						String noDelay_s = getCharacterDataFromElement(noDelay_el);
						PCCPCEPsessionParams.setNoDelay(Boolean.valueOf(noDelay_s));
					}
				}
				NodeList session_nl = PCCPCEPSession_el.getElementsByTagName("Session");
				if (session_nl!=null){	
					Hashtable<Integer,Integer> PCEServerPortList = new Hashtable<Integer,Integer>();
					Hashtable<Integer,String> ipPCEList = new Hashtable<Integer,String>();
					PCCPCEPsessionParams.setNumSessions(session_nl.getLength());					
					for (int i=0; i<session_nl.getLength();i++){
						Element session_el = (Element) session_nl.item(i);
						//timeBetweenRequestIni
						NodeList ipPCE_nl = session_el.getElementsByTagName("ipPCE");
						if (ipPCE_nl!=null){
							if (ipPCE_nl.getLength()>0){					
								Element ipPCE_el = (Element) ipPCE_nl.item(0);
								String ipPCE_s = getCharacterDataFromElement(ipPCE_el);
								ipPCEList.put(i,ipPCE_s);
							}
						}
						NodeList PCEServerPort_nl = session_el.getElementsByTagName("PCEServerPort");
						if (PCEServerPort_nl!=null){
							if (PCEServerPort_nl.getLength()>0){					
								Element PCEServerPort_el = (Element) PCEServerPort_nl.item(0);
								String PCEServerPort_s = getCharacterDataFromElement(PCEServerPort_el);
								PCEServerPortList.put(i,Integer.valueOf(PCEServerPort_s));
							}
						}
					}
					PCCPCEPsessionParams.setIpPCEList(ipPCEList);
					PCCPCEPsessionParams.setPCEServerPortList(PCEServerPortList);
				}
		

			}
			NodeList managementClientPort_nl = doc.getElementsByTagName("managementClientPort");
			if (managementClientPort_nl!=null){
				if (managementClientPort_nl.getLength()>0){					
					Element managementClientPort_el = (Element) managementClientPort_nl.item(0);
					String managementClientPort_s = getCharacterDataFromElement(managementClientPort_el);
					managementClientPort=Integer.parseInt(managementClientPort_s);
					}
			}
			
			
			NodeList load_nl = doc.getElementsByTagName("load");
			if (load_nl!=null){
				Element load_el = (Element) load_nl.item(0);
				if (load_el.getAttributeNode("type").getValue().equals("Fixed")){//Lista creada de Request con source y destino

					NodeList meanTimeBetweenRequest_nl = load_el.getElementsByTagName("meanTimeBetweenRequest");
					if (meanTimeBetweenRequest_nl!=null){
						if (meanTimeBetweenRequest_nl.getLength()>0){					
							Element meanTimeBetweenRequest_el = (Element) meanTimeBetweenRequest_nl.item(0);
							String meanTimeBetweenRequest_s = getCharacterDataFromElement(meanTimeBetweenRequest_el);
							meanTimeBetweenRequest=Double.valueOf(meanTimeBetweenRequest_s);
						}
					}
					NodeList meanConectionTime_nl = load_el.getElementsByTagName("meanConectionTime");
					if (meanConectionTime_nl!=null){
						if (meanConectionTime_nl.getLength()>0){					
							Element meanConectionTime_el = (Element) meanConectionTime_nl.item(0);
							String meanConectionTime_s = getCharacterDataFromElement(meanConectionTime_el);
							meanConectionTime=Double.valueOf(meanConectionTime_s);
						}
					}
					NodeList staticConnections_nl = load_el.getElementsByTagName("staticConnections");
					if (staticConnections_nl!=null){
						if (staticConnections_nl.getLength()>0){					
							Element staticConnections_el = (Element) staticConnections_nl.item(0);
							String staticConnections_s = getCharacterDataFromElement(staticConnections_el);
							staticConnections=Boolean.parseBoolean(staticConnections_s);
						}
					}
				}
				if (load_el.getAttributeNode("type").getValue().equals("List")){
				NodeList session_nl = load_el.getElementsByTagName("Session");
				if (session_nl!=null){						
					for (int i=0; i<session_nl.getLength();i++){
						Element session_el = (Element) session_nl.item(i);
						//timeBetweenRequestIni
						NodeList meanTimeBetweenRequest_nl = session_el.getElementsByTagName("meanTimeBetweenRequest");
						if (meanTimeBetweenRequest_nl!=null){
							if (meanTimeBetweenRequest_nl.getLength()>0){					
								Element meanTimeBetweenRequest_el = (Element) meanTimeBetweenRequest_nl.item(0);
								String meanTimeBetweenRequest_s = getCharacterDataFromElement(meanTimeBetweenRequest_el);
								meanTimeBetweenRequestList.add(Double.valueOf(meanTimeBetweenRequest_s));
							}
						}
						NodeList meanConectionTime_nl = session_el.getElementsByTagName("meanConectionTime");
						if (meanConectionTime_nl!=null){
							if (meanConectionTime_nl.getLength()>0){					
								Element meanConectionTime_el = (Element) meanConectionTime_nl.item(0);
								String meanConectionTime_s = getCharacterDataFromElement(meanConectionTime_el);
								meanConectionTimeList.add(Double.valueOf(meanConectionTime_s));
							}
						}
						
			
					
					}
				}
				}
				else if (load_el.getAttributeNode("type").getValue().equals("VaryingTimeBetweenRequest")){
					NodeList timeBetweenRequestChange_nl = load_el.getElementsByTagName("timeBetweenRequestChange");
					if (timeBetweenRequestChange_nl!=null){				
						timeBetweenRequestChange=true;
						Element timeBetweenRequestChange_el = (Element) timeBetweenRequestChange_nl.item(0);
						//timeBetweenRequestIni
						NodeList timeBetweenRequestIni_nl = timeBetweenRequestChange_el.getElementsByTagName("timeBetweenRequestIni");
						Element timeBetweenRequestIni_el = (Element) timeBetweenRequestIni_nl.item(0);
						timeBetweenRequestIni=Double.valueOf( getCharacterDataFromElement(timeBetweenRequestIni_el));
						//timeBetweenRequestMax
						NodeList timeBetweenRequestMax_nl = timeBetweenRequestChange_el.getElementsByTagName("timeBetweenRequestMax");
						Element timeBetweenRequestMax_el = (Element) timeBetweenRequestMax_nl.item(0);
						timeBetweenRequestMax=Double.valueOf(getCharacterDataFromElement(timeBetweenRequestMax_el));

						//timeBetweenRequestStep
						NodeList timeBetweenRequestStep_nl = timeBetweenRequestChange_el.getElementsByTagName("timeBetweenRequestStep");
						Element timeBetweenRequestStep_el = (Element) timeBetweenRequestStep_nl.item(0);								
						timeBetweenRequestStep=Double.valueOf(getCharacterDataFromElement(timeBetweenRequestStep_el));
						meanTimeBetweenRequest=timeBetweenRequestIni;
					}
					NodeList meanConectionTime_nl = load_el.getElementsByTagName("meanConectionTime");
					if (meanConectionTime_nl!=null){
						if (meanConectionTime_nl.getLength()>0){					
							Element meanConectionTime_el = (Element) meanConectionTime_nl.item(0);
							String meanConectionTime_s = getCharacterDataFromElement(meanConectionTime_el);
							meanConectionTime=Double.valueOf(meanConectionTime_s);
						}
					}
				}
				else if (load_el.getAttributeNode("type").getValue().equals("VaryingConnectionTime")){
					NodeList conectionTimeChange_nl = load_el.getElementsByTagName("conectionTimeChange");
					if (conectionTimeChange_nl!=null){				
						conectionTimeChange=true;
						Element conectionTimeChange_el = (Element) conectionTimeChange_nl.item(0);
						//timeBetweenRequestIni
						NodeList conectionTimeIni_nl = conectionTimeChange_el.getElementsByTagName("conectionTimeIni");
						Element conectionTimeIni_el = (Element) conectionTimeIni_nl.item(0);
						conectionTimeIni=Double.valueOf( getCharacterDataFromElement(conectionTimeIni_el));
						//timeBetweenRequestMax
						NodeList conectionTimeMax_nl = conectionTimeChange_el.getElementsByTagName("conectionTimeMax");
						Element conectionTimeMax_el = (Element) conectionTimeMax_nl.item(0);
						conectionTimeMax=Double.valueOf(getCharacterDataFromElement(conectionTimeMax_el));
						DocumentBuilder builder2 = DocumentBuilderFactory.newInstance().newDocumentBuilder();
//						Document doc2 = builder2.parse(file);
//						NodeList num_execution_node = doc2.getElementsByTagName("executionNumber");
//						if(algorithm!=null){
//							Element num_execution_el = (Element) num_execution_node.item(0);
//							String num_execution=num_execution_el.getAttribute("cod");
//							setNum_execution(Integer.parseInt(num_execution));
//							
//						}
						//timeBetweenRequestStep
						NodeList conectionTimeStep_nl = conectionTimeChange_el.getElementsByTagName("conectionTimeStep");
						Element conectionTimeStep_el = (Element) conectionTimeStep_nl.item(0);								
						conectionTimeStep=Double.valueOf(getCharacterDataFromElement(conectionTimeStep_el));
						meanConectionTime=conectionTimeIni;
					}
					NodeList meanTimeBetweenRequest_nl = load_el.getElementsByTagName("meanTimeBetweenRequest");
					if (meanTimeBetweenRequest_nl!=null){
						if (meanTimeBetweenRequest_nl.getLength()>0){					
							Element meanTimeBetweenRequest_el = (Element) meanTimeBetweenRequest_nl.item(0);
							String meanTimeBetweenRequest_s = getCharacterDataFromElement(meanTimeBetweenRequest_el);
							meanTimeBetweenRequest=Double.valueOf(meanTimeBetweenRequest_s);
						}
					}
				}
				else if (load_el.getAttributeNode("type").getValue().equals("VaryingLoad")){
					NodeList loadVary_nl = load_el.getElementsByTagName("load");
					if (load_nl!=null){				
						loadChange=true;
						Element loadVary_el = (Element) loadVary_nl.item(0);
						//loadIni
						NodeList loadIni_nl = loadVary_el.getElementsByTagName("loadIni");
						Element loadIni_el = (Element) loadIni_nl.item(0);
						loadIni=Double.valueOf( getCharacterDataFromElement(loadIni_el));
						//loadMax
						NodeList loadMax_nl = load_el.getElementsByTagName("loadMax");
						Element loadMax_el = (Element) loadMax_nl.item(0);
						loadMax=Double.valueOf(getCharacterDataFromElement(loadMax_el));
						//loadMax=Long.valueOf(getCharacterDataFromElement(loadMax_el));
						//loadStep
						NodeList loadStep_nl = load_el.getElementsByTagName("loadMax");
						Element loadStep_el = (Element) loadStep_nl.item(0);								
						loadStep=Double.valueOf(getCharacterDataFromElement(loadStep_el));

					}
				}
			}
			/* TRACES: Run PCC with or without traces */
			NodeList setTraces_nl = doc.getElementsByTagName("setTraces");
			if (setTraces_nl!=null){
				if (setTraces_nl.getLength()>0){					
					Element setTraces_el = (Element) setTraces_nl.item(0);
					String setTraces_s = getCharacterDataFromElement(setTraces_el);
					setTraces=Boolean.parseBoolean(setTraces_s);
					}
			}
			/**
			 * CONTROL PLANE OPTION
			 */
			NodeList controlPlaneOption_nl = doc.getElementsByTagName("controlPlaneOption");
			if (controlPlaneOption_nl!=null){
				if (controlPlaneOption_nl.getLength()>0){					
					Element controlPlaneOption_el = (Element) controlPlaneOption_nl.item(0);
					String controlPlaneOption_s = getCharacterDataFromElement(controlPlaneOption_el);
					controlPlaneOption=Boolean.parseBoolean(controlPlaneOption_s);
					}
			}
			
			if (setTraces == false){
				log.setLevel(Level.SEVERE);		
				Logger log2=Logger.getLogger("PCEPClientParser");
				log2.setLevel(Level.SEVERE);
				Logger log3= Logger.getLogger("OSPFParser");
				log3.setLevel(Level.SEVERE);
				log.info("traces off!\r\n");
			}
			NodeList isExponential_nl = doc.getElementsByTagName("isExponential");
			if (isExponential_nl!=null){
				if (isExponential_nl.getLength()>0){					
					Element isExponential_el = (Element) isExponential_nl.item(0);
					String isExponential_s = getCharacterDataFromElement(isExponential_el);
					isExponential=Boolean.parseBoolean(isExponential_s);
					}
			}

			/* PERIOD OF TIME TO PRINT STATISTICS*/		
			NodeList printStatisticsTime_nl = doc.getElementsByTagName("printStatisticsTime");
			if (printStatisticsTime_nl!=null){
				if (printStatisticsTime_nl.getLength()>0){					
					Element printStatisticsTime_el = (Element) printStatisticsTime_nl.item(0);
					printStatisticsTime = Long.valueOf(getCharacterDataFromElement(printStatisticsTime_el));
					
					}
			}			
			/* ACTIVITY */
			NodeList Activity_nl = doc.getElementsByTagName("Activity");
			if (Activity_nl!=null){
				if (Activity_nl.getLength()>0){	
					Element activity_el = (Element) Activity_nl.item(0);						
					/* NETWORK EMULATOR: Run PCC with a network Emulator */
					NodeList networkEmulator_nl = activity_el.getElementsByTagName("networkEmulator");
					if (networkEmulator_nl!=null){
						if (networkEmulator_nl.getLength()>0){					
							Element networkEmulator_el = (Element) networkEmulator_nl.item(0);
							String networkEmulator_s = getCharacterDataFromElement(networkEmulator_el);
							networkEmulator=Boolean.parseBoolean(networkEmulator_s);
						}
					}
					/* VNTM: Run the PCC with an VNTM */
					NodeList VNTMSession_nl = activity_el.getElementsByTagName("VNTMSession");
					if (VNTMSession_nl!=null){
						if (VNTMSession_nl.getLength()>0){					
							Element VNTMSession_el = (Element) VNTMSession_nl.item(0);
							String VNTMSession_s = getCharacterDataFromElement(VNTMSession_el);
							VNTMSession=Boolean.parseBoolean(VNTMSession_s);
						}
					}
					NodeList VNTMFile_nl = activity_el.getElementsByTagName("VNTMFile");
					if (VNTMFile_nl!=null){
						if (VNTMFile_nl.getLength()>0){					
							Element VNTMFile_el = (Element) VNTMFile_nl.item(0);
							VNTMFile = getCharacterDataFromElement(VNTMFile_el);
						}
					}

				}
			}
			
			/* STRONGEST: Option PCC chooses PCE with Load Balancing */
			NodeList loadBalancing_nl = doc.getElementsByTagName("loadBalancing");
			if (loadBalancing_nl!=null){
				if (loadBalancing_nl.getLength()>0){					
					Element loadBalancing_el = (Element) loadBalancing_nl.item(0);
					String loadBalancing_s = getCharacterDataFromElement(loadBalancing_el);
					loadBalancing=Boolean.parseBoolean(loadBalancing_s);
					}
			}		
			
			/* RESTORATION CASE */
			NodeList restorationCase_nl = doc.getElementsByTagName("restorationCase");
			if (restorationCase_nl!=null){
				if (restorationCase_nl.getLength()>0){	
						Element restorationCase_el = (Element) restorationCase_nl.item(0);		
						restorationCaseInformation = new RestorationCaseInformation();
						//timeConnectionFallDown
						NodeList timeConnectionFallDown_nl = restorationCase_el.getElementsByTagName("timeConnectionFallDown");
						if (timeConnectionFallDown_nl!=null){
							if (timeConnectionFallDown_nl.getLength()>0){	
								Element timeConnectionFallDown_el = (Element) timeConnectionFallDown_nl.item(0);						
								restorationCaseInformation.setTimeToWait( Long.parseLong(getCharacterDataFromElement(timeConnectionFallDown_el)));
								
							}
						}
						//whichConnectionFallDown
						NodeList whichConnectionFallDown_nl = restorationCase_el.getElementsByTagName("whichConnectionFallDown");
						if (whichConnectionFallDown_nl!=null){
							if (whichConnectionFallDown_nl.getLength()>0){
								Element whichConnectionFallDown_el = (Element) whichConnectionFallDown_nl.item(0);
								//source
								NodeList source_nl = whichConnectionFallDown_el.getElementsByTagName("source_restoration");
								Element source_el = (Element) source_nl.item(0);
								String source_s = getCharacterDataFromElement(source_el);								
								restorationCaseInformation.setSource((Inet4Address) Inet4Address.getByName(getCharacterDataFromElement(source_el)));

								//destination
								NodeList destination_nl = whichConnectionFallDown_el.getElementsByTagName("destination_restoration");
								Element destination_el = (Element) destination_nl.item(0);
								String destination_s = getCharacterDataFromElement(destination_el);
								restorationCaseInformation.setDestination((Inet4Address) Inet4Address.getByName( getCharacterDataFromElement(destination_el)));
								nameRestorationCaseFile = source_s+"_"+destination_s;
							}
						}
						//executionNumber
						NodeList executionNumber_nl = restorationCase_el.getElementsByTagName("executionNumber");
						if (executionNumber_nl!=null){
							if (executionNumber_nl.getLength()>0){
								Element executionNumber_el = (Element) executionNumber_nl.item(0);						
								executionNumber= getCharacterDataFromElement(executionNumber_el);	
							}
						
						}
				}
			}
			NodeList classToExecute_nl = doc.getElementsByTagName("classToExecute");
			if (classToExecute_nl!=null){
				if (classToExecute_nl.getLength()>0){					
					Element classToExecute_el = (Element) classToExecute_nl.item(0);
					classToExecute = getCharacterDataFromElement(classToExecute_el);
					
					}
			}
			NodeList maxNumberIterations_nl = doc.getElementsByTagName("maxNumberIterations");
			if (maxNumberIterations_nl!=null){
				if (maxNumberIterations_nl.getLength()>0){					
					Element maxNumberIterations_el = (Element) maxNumberIterations_nl.item(0);
					String maxNumberIterations_s = getCharacterDataFromElement(maxNumberIterations_el);
					maxNumberIterations=Integer.parseInt(maxNumberIterations_s);
					}
			}
			
			NodeList numberNodes_nl = doc.getElementsByTagName("numberNodes");
			if (numberNodes_nl!=null){
				if (numberNodes_nl.getLength()>0){					
					Element numberNodes_el = (Element) numberNodes_nl.item(0);
					String numberNodes_s = getCharacterDataFromElement(numberNodes_el);
					numberNodes=Integer.parseInt(numberNodes_s);
					}
			}
			NodeList maxTimeWaitingForResponse_ms_nl = doc.getElementsByTagName("maxTimeWaitingForResponse_ms");
			if (maxTimeWaitingForResponse_ms_nl!=null){
				if (maxTimeWaitingForResponse_ms_nl.getLength()>0){					
					Element maxTimeWaitingForResponse_ms_el = (Element) maxTimeWaitingForResponse_ms_nl.item(0);
					String maxTimeWaitingForResponse_ms_s = getCharacterDataFromElement(maxTimeWaitingForResponse_ms_el);
					maxTimeWaitingForResponse_ms=Long.parseLong(maxTimeWaitingForResponse_ms_s);
				}
			}
			
			NodeList NumfileBW_nl = doc.getElementsByTagName("NumfileBW");
			if (NumfileBW_nl!=null){
				if (NumfileBW_nl.getLength()>0){					
					Element NumfileBW_el = (Element) NumfileBW_nl.item(0);
					String NumfileBW_s = getCharacterDataFromElement(NumfileBW_el);
					NumfileBW=Integer.parseInt(NumfileBW_s);
					}
			}
			
			NodeList semillaTiempos_nl = doc.getElementsByTagName("semillaTiempos");
			if (semillaTiempos_nl!=null){
				if (semillaTiempos_nl.getLength()>0){					
					Element semillaTiempos_el = (Element) semillaTiempos_nl.item(0);
					String semillaTiempos_s = getCharacterDataFromElement(semillaTiempos_el);
					semillaTiempos=Long.parseLong(semillaTiempos_s);
				}
			}
			
			boolean commonRequestParameters =false;
			RequestParametersConfiguration reqParams = null;
			NodeList commonRequestParameters_nl = doc.getElementsByTagName("commonRequestParameters");	
			if (commonRequestParameters_nl!=null){
				if (commonRequestParameters_nl.getLength()>0){	
					reqParams=new RequestParametersConfiguration();
					commonRequestParameters =true;				
					Element commonRequestParameters_el = (Element) commonRequestParameters_nl.item(0);
					//of
					NodeList of_nl = commonRequestParameters_el.getElementsByTagName("of");
					if (of_nl!=null){
						Element of_el = (Element) of_nl.item(0);
						String of_s = getCharacterDataFromElement(of_el);
						int of = Integer.parseInt(of_s);
						reqParams.setOf(true);
						reqParams.setOfCode(of);
					}
					//SVEC
					NodeList isSvec_nl = doc.getElementsByTagName("isSvec");
					if (isSvec_nl!=null){
						if (isSvec_nl.getLength()>0){					
							Element isSvec_el = (Element) isSvec_nl.item(0);
							String isSvec_s = getCharacterDataFromElement(isSvec_el);
							svec=Boolean.parseBoolean(isSvec_s);
							}
					}
										
					//reservation
					NodeList reservation_nl = commonRequestParameters_el.getElementsByTagName("reservation");
					if (reservation_nl!=null){
						if (reservation_nl.getLength()>0){
							Element reservation_el = (Element) reservation_nl.item(0);
							reservation=Boolean.parseBoolean(getCharacterDataFromElement(reservation_el));
							log.info("Reservation: " +reservation);
							reqParams.setReservation(reservation);
						}
					}
					//Varying Bandwidth
					NodeList variyingBandwidth_nl = commonRequestParameters_el.getElementsByTagName("isVariyingBandwidth");
					if (variyingBandwidth_nl!=null){
						if (variyingBandwidth_nl.getLength()>0){
							Element variyingBandwidth_el = (Element) variyingBandwidth_nl.item(0);
							boolean variyingBw=Boolean.parseBoolean(getCharacterDataFromElement(variyingBandwidth_el));
							reqParams.setVariableBandwidth(variyingBw);
						}
					}
					if (reqParams.isVariableBandwidth==false){
						//Bandwidth
						NodeList bandwidth_nl = commonRequestParameters_el.getElementsByTagName("bandwidth");
						if (bandwidth_nl!=null){
							if (bandwidth_nl.getLength()>0){	
								Element bandwidth_el = (Element) bandwidth_nl.item(0);
								bandwidth=Float.parseFloat(getCharacterDataFromElement(bandwidth_el));
								log.info("Bandwidth: " +bandwidth);
								reqParams.setBW(bandwidth);
								reqParams.setBandwidth(true);
							}
						}
					}
					if (reqParams.isVariableBandwidth==true){
						setVariableBandwidth(true);
					}
					//BandwidthMax
					NodeList bandwidthMax_nl = commonRequestParameters_el.getElementsByTagName("bandwidthMax");
					if (bandwidthMax_nl!=null){
						if (bandwidthMax_nl.getLength()>0){	
							Element bandwidthMax_el = (Element) bandwidthMax_nl.item(0);
							bandwidthMax = Float.parseFloat(getCharacterDataFromElement(bandwidthMax_el));
							log.info("Bandwidth: " +bandwidthMax);					
							reqParams.setBandwidth(true);
						}
					}
					//BandwidthMin
					NodeList bandwidthMin_nl = commonRequestParameters_el.getElementsByTagName("bandwidthMin");
					if (bandwidthMin_nl!=null){
						if (bandwidthMin_nl.getLength()>0){	
							Element bandwidthMin_el = (Element) bandwidthMin_nl.item(0);
							bandwidthMin=Float.parseFloat(getCharacterDataFromElement(bandwidthMin_el));
							log.info("Bandwidth: " +bandwidthMin);						
							reqParams.setBandwidth(true);
						}
					}
					
					//delayMetric
					NodeList delayMetric_nl = commonRequestParameters_el.getElementsByTagName("delayMetric");
					if (delayMetric_nl!=null){
						Element delayMetric_el = (Element) delayMetric_nl.item(0);
						boolean delayMetric=Boolean.parseBoolean(getCharacterDataFromElement(delayMetric_el));
						reqParams.setDelayMetric(delayMetric);
					}
					//timeReserved
					NodeList timeReserved_nl = commonRequestParameters_el.getElementsByTagName("timeReserved");
					if (timeReserved_nl!=null){
						Element timeReserved_el = (Element) timeReserved_nl.item(0);
						String timeReserved_s= getCharacterDataFromElement(timeReserved_el);
						reqParams.setTimeReserved(Long.parseLong(timeReserved_s));
						nameRestorationCaseFile = nameRestorationCaseFile +"_TR_"+timeReserved_s;
					}
					//priority
					NodeList priority_nl = commonRequestParameters_el.getElementsByTagName("priority");
					if (priority_nl!=null){
						Element priority_el = (Element) priority_nl.item(0);
						reqParams.setPriority(Integer.parseInt(getCharacterDataFromElement(priority_el)));
					}
					//bidirectional
					NodeList bidirectional_nl = commonRequestParameters_el.getElementsByTagName("bidirectional");
					if (bidirectional_nl!=null){
						Element bidirectional_el = (Element) bidirectional_nl.item(0);
						boolean bidirectional=Boolean.parseBoolean(getCharacterDataFromElement(bidirectional_el));
						reqParams.setBidirectional(bidirectional);
					}
					//Reoptimization
					NodeList Reop_nl = commonRequestParameters_el.getElementsByTagName("Reopt");
					if (Reop_nl!=null){
						Element Reop_el = (Element) Reop_nl.item(0);
						boolean Reop=Boolean.parseBoolean(getCharacterDataFromElement(Reop_el));
						reqParams.setReoptimization(Reop);
					}
					//Loose
					NodeList Loose_nl = commonRequestParameters_el.getElementsByTagName("Loose");
					if (Loose_nl!=null){
						Element Loose_el = (Element) Loose_nl.item(0);
						boolean loose=Boolean.parseBoolean(getCharacterDataFromElement(Loose_el));
						reqParams.setLoose(loose);
					}
				}
			}
			int contador=0;
			NodeList request_nl = doc.getElementsByTagName("Request");
			if (request_nl!=null){

				Element request_el = (Element) request_nl.item(0);
				if (request_el.getAttributeNode("type").getValue().equals("Fixed")){//Lista creada de Request con source y destino

					NodeList newRequest_nl = request_el.getElementsByTagName("newRequest");
					for (int k = 0; k < newRequest_nl.getLength(); k++) {
						RequestToSend request = new RequestToSend();
						if (commonRequestParameters)
							request.setRequestParameters(reqParams);
						Element newRequest_el = (Element) newRequest_nl.item(k);
						//source
						NodeList source_nl = newRequest_el.getElementsByTagName("source");
						Element source_el = (Element) source_nl.item(0);
						request.setSource((Inet4Address) Inet4Address.getByName(getCharacterDataFromElement(source_el)));

						//sourceList.add(k, (Inet4Address) Inet4Address.getByName(getCharacterDataFromElement(source_el)));
						//destination
						NodeList destination_nl = newRequest_el.getElementsByTagName("destination");
						Element destination_el = (Element) destination_nl.item(0);
						request.setDestiny((Inet4Address) Inet4Address.getByName( getCharacterDataFromElement(destination_el)));
						
						if (reqParams.isVariableBandwidth==true){ 
							//Bandwidth
							NodeList bandwidth_nl = newRequest_el.getElementsByTagName("bandwidth");
							if (bandwidth_nl!=null){
								if (bandwidth_nl.getLength()>0){	
									Element bandwidth_el = (Element) bandwidth_nl.item(0);
									bandwidth=Float.parseFloat(getCharacterDataFromElement(bandwidth_el));
									log.info("Bandwidth: " +bandwidth);
									reqParams.setVariableBandwidth(true);
									reqParams.setBW(bandwidth);
									reqParams.setBandwidth(true);
								}
							}
						}
						//destinationList.add(k, (Inet4Address) Inet4Address.getByName( getCharacterDataFromElement(destination_el)));

						NodeList requestnewParameters_nl = newRequest_el.getElementsByTagName("requestParameters");	
						if (requestnewParameters_nl!=null){		
							if (requestnewParameters_nl.getLength()>0){
							Element requestParameters_el = (Element) requestnewParameters_nl.item(0);
							reqParams=new RequestParametersConfiguration();
											
							//of
							NodeList of_nl = requestParameters_el.getElementsByTagName("of");
							if (of_nl!=null){
								Element of_el = (Element) of_nl.item(0);
								String of_s = getCharacterDataFromElement(of_el);
								int of = Integer.parseInt(of_s);
								reqParams.setOf(true);
								reqParams.setOfCode(of);
							}
							//reservation
							NodeList reservation_nl = requestParameters_el.getElementsByTagName("reservation");
							if (reservation_nl!=null){
								Element reservation_el = (Element) reservation_nl.item(0);
								reservation=Boolean.parseBoolean(getCharacterDataFromElement(reservation_el));
								log.info("Reservation: " +reservation);
								reqParams.setReservation(reservation);
							}
							//Bandwidth
							NodeList bandwidth_nl = requestParameters_el.getElementsByTagName("bandwidth");
							if (bandwidth_nl!=null){
								if (bandwidth_nl.getLength()>0){	
									Element bandwidth_el = (Element) bandwidth_nl.item(0);
									bandwidth=Float.parseFloat(getCharacterDataFromElement(bandwidth_el));
									log.info("Bandwidth: " +bandwidth);
									reqParams.setBW(bandwidth);
									reqParams.setBandwidth(true);
								}
							}
							//delayMetric
							NodeList delayMetric_nl = requestParameters_el.getElementsByTagName("delayMetric");
							if (delayMetric_nl!=null){
								Element delayMetric_el = (Element) delayMetric_nl.item(0);
								boolean delayMetric=Boolean.parseBoolean(getCharacterDataFromElement(delayMetric_el));
								reqParams.setDelayMetric(delayMetric);
							}
							//timeReserved
							NodeList timeReserved_nl = requestParameters_el.getElementsByTagName("timeReserved");
							if (timeReserved_nl!=null){
								Element timeReserved_el = (Element) timeReserved_nl.item(0);
								reqParams.setTimeReserved(Long.parseLong(getCharacterDataFromElement(timeReserved_el)));
							}
							//priority
							NodeList priority_nl = requestParameters_el.getElementsByTagName("priority");
							if (priority_nl!=null){
								Element priority_el = (Element) priority_nl.item(0);
								reqParams.setPriority(Integer.parseInt(getCharacterDataFromElement(priority_el)));
							}
							//bidirectional
							NodeList bidirectional_nl = requestParameters_el.getElementsByTagName("bidirectional");
							if (bidirectional_nl!=null){
								Element bidirectional_el = (Element) bidirectional_nl.item(0);
								boolean bidirectional=Boolean.parseBoolean(getCharacterDataFromElement(bidirectional_el));
								reqParams.setBidirectional(bidirectional);
							}
							//Reoptimization
							NodeList Reop_nl = requestParameters_el.getElementsByTagName("Reopt");
							if (Reop_nl!=null){
								Element Reop_el = (Element) Reop_nl.item(0);
								boolean Reop=Boolean.parseBoolean(getCharacterDataFromElement(Reop_el));
								reqParams.setReoptimization(Reop);
							}
							//Loose
							NodeList Loose_nl = requestParameters_el.getElementsByTagName("Loose");
							if (Loose_nl!=null){
								Element Loose_el = (Element) Loose_nl.item(0);
								boolean loose=Boolean.parseBoolean(getCharacterDataFromElement(Loose_el));
								reqParams.setLoose(loose);
							}
							request.setRequestParameters(reqParams);
							}
						}
						requestToSendList.add(contador,request);
						contador++;
					}

					}
					else if(request_el.getAttributeNode("type").getValue().equals("List")){
						//Crear la lista						
					LinkedList<Inet4Address> sourceList=new LinkedList<Inet4Address>();
					LinkedList<Inet4Address> destinationList = new LinkedList<Inet4Address>();
					int numberRequests=0;
					
						NodeList srcs_nl = request_el.getElementsByTagName("srcList");
						Element srcs_el = (Element) srcs_nl.item(0);
						//source
						NodeList src_nl = srcs_el.getElementsByTagName("src");
						for (int k = 0; k < src_nl.getLength(); k++) {
							Element src_el = (Element) src_nl.item(k);
							sourceList.add((Inet4Address) Inet4Address.getByName(getCharacterDataFromElement(src_el)));
						}
						NodeList dsts_nl = request_el.getElementsByTagName("dstList");
						Element dsts_el = (Element) dsts_nl.item(0);
						//source
						NodeList dst_nl = dsts_el.getElementsByTagName("dst");
						for (int k = 0; k < dst_nl.getLength(); k++) {
							Element dst_el = (Element) dst_nl.item(k);
							destinationList.add((Inet4Address) Inet4Address.getByName(getCharacterDataFromElement(dst_el)));
						}
						
						if (svec==true){
							
							NodeList numberRequests_nl = request_el.getElementsByTagName("numberRequests");
							if (numberRequests_nl!=null){												
								Element numberRequests_el = (Element) numberRequests_nl.item(0);
								String numberRequests_s = getCharacterDataFromElement(numberRequests_el);
								numberRequests=Integer.parseInt(numberRequests_s);
								
							}
							createListRequestToSend(sourceList,destinationList, reqParams, numberRequests);
						}else{
							createListRequestToSend(sourceList,destinationList, reqParams);
						}						
					}else if (request_el.getAttributeNode("type").getValue().equals("Random")){
						//Crear la lista
//						int numberNodes=0;
						int numberRequests=0;
						String baseIP=null;
						
//						NodeList numberNodes_nl = request_el.getElementsByTagName("numberNodes");
//						if (numberNodes_nl!=null){												
//							Element numberNodes_el = (Element) numberNodes_nl.item(0);
//							String numberNodes_s = getCharacterDataFromElement(numberNodes_el);
//							numberNodes=Integer.parseInt(numberNodes_s);
//							
//						}
						NodeList baseIP_nl = request_el.getElementsByTagName("baseIP");
						if (baseIP_nl!=null){												
							Element baseIP_el = (Element) baseIP_nl.item(0);
							baseIP = getCharacterDataFromElement(baseIP_el);							
						}
						createListRequestToSend(numberNodes,baseIP, reqParams);
					}
				//** Collaborative PCEs
			
	}
		}	catch (Exception e) {
			e.printStackTrace();
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

		public void createListRequestToSend(int numberNodes,String baseIP,RequestParametersConfiguration reqParams){
			RequestToSend requestToSend=null;

			if (numberNodes!=0){
				for (int k=2;k<numberNodes+1 ;k++){
					for (int i=1;i<k;i++){
						requestToSend = new RequestToSend();
						int num_origen= k;
						int num_destino= i;
						if(num_destino!=num_origen){

							String source_s =new String();

							source_s=baseIP+ String.valueOf(num_origen);//+"0";
							String destiny_s = new String();
							destiny_s=baseIP+String.valueOf(num_destino);//+"0";
							Inet4Address src_ip=null;
							Inet4Address dst_ip=null;
							try {
								src_ip = (Inet4Address) Inet4Address.getByName(source_s);
								dst_ip=(Inet4Address) Inet4Address.getByName(destiny_s);
							} catch (UnknownHostException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							requestToSend.setSource(src_ip);
							requestToSend.setDestiny(dst_ip);
							requestToSend.setRequestParameters(reqParams);
							requestToSendList.add(requestToSend);
							
//							requestToSend = new RequestToSend();
//							requestToSend.setSource(src_ip);
//							requestToSend.setDestiny(dst_ip);
//							requestToSend.setRequestParameters(reqParams);
//							requestToSendList.add(requestToSend);
							
							requestToSend = new RequestToSend();
							try {
								src_ip = (Inet4Address) Inet4Address.getByName(destiny_s);
								dst_ip=(Inet4Address) Inet4Address.getByName(source_s);
							} catch (UnknownHostException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							requestToSend.setSource(src_ip);
							requestToSend.setDestiny(dst_ip);
							requestToSend.setRequestParameters(reqParams);
							requestToSendList.add(requestToSend);
							
//							requestToSend = new RequestToSend();
//							requestToSend.setSource(src_ip);
//							requestToSend.setDestiny(dst_ip);
//							requestToSend.setRequestParameters(reqParams);
//							requestToSendList.add(requestToSend);

						}
					}
				}
			}		 	

		}


		public void createListRequestToSend(int numberNodes,int numberRequests,String baseIP,RequestParametersConfiguration reqParams){
			RequestToSend requestToSend=null;
			MersenneTwister mt = new MersenneTwister();
			if (numberNodes!=0){
				for (int i=0;i<numberRequests;i++){
				  requestToSend = new RequestToSend();
				int num_origen=(int) (mt.nextDouble()*numberNodes)+1;
				int num_destino=(int) (mt.nextDouble()*(numberNodes-1)+1);
				if(num_destino>=num_origen){
					num_destino=num_destino+1;
				}
				String source_s =new String();

				source_s=baseIP+ String.valueOf(num_origen);//+"0";
				String destiny_s = new String();
				destiny_s=baseIP+String.valueOf(num_destino);//+"0";
				Inet4Address src_ip=null;
				Inet4Address dst_ip=null;
				try {
					src_ip = (Inet4Address) Inet4Address.getByName(source_s);
					dst_ip=(Inet4Address) Inet4Address.getByName(destiny_s);
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				requestToSend.setSource(src_ip);
				requestToSend.setDestiny(dst_ip);
				requestToSend.setRequestParameters(reqParams);
					requestToSendList.set(i, requestToSend);
				}
			}

			 	

		}
		
		/* TRACES */
		public boolean isSetTraces() {
			return setTraces;
		}
		public void setSetTraces(boolean setTraces) {
			this.setTraces = setTraces;
		}

		/* RESTORATION CASE */
		public RestorationCaseInformation getRestorationCaseInformation() {
			return restorationCaseInformation;
		}


		public String getNameRestorationCaseFile() {
			return nameRestorationCaseFile;
		}


		public void setNameRestorationCaseFile(String nameRestorationCaseFile) {
			this.nameRestorationCaseFile = nameRestorationCaseFile;
		}



		public String getExecutionNumber() {
			return executionNumber;
		}


		/* NUMBER TOPOLOGY NODES */
		public int getNumberNodes(){
			return numberNodes;
		}
		
		/**
		 * Create a list of requests to send
		 * @param sourceList
		 * @param destinationList
		 * @param reqParams
		 */
		public void createListRequestToSend(LinkedList<Inet4Address> sourceList,LinkedList<Inet4Address> destinationList,RequestParametersConfiguration reqParams){
			RequestToSend requestToSend=null;
			if ((sourceList!=null)&&(destinationList!=null)){				
				int num_srcs = sourceList.size();
				int num_dsts =  destinationList.size();			
					for (int i=0;i<num_srcs;++i){
					   for (int j=0;j<num_dsts;++j){				    	
						   if (sourceList.get(i).equals(destinationList.get(j))){
							   log.info("Origen y destino coinciden, paso");
						   }else {
							   requestToSend = new RequestToSend();
							   try {
								   Thread.sleep(1);
							   } catch (InterruptedException e) {
								   // TODO Auto-generated catch block
								   e.printStackTrace();
							   }
							   requestToSend.setSource(sourceList.get(i));
							   requestToSend.setDestiny(destinationList.get(j));
							   requestToSend.setRequestParameters(reqParams);
							   requestToSendList.add(requestToSend);
				     
							   
							   log.info("Creo un request: "+ requestToSend.toString());
						   }
					   }
				   }
			}
		}
		
		public void createListRequestToSend(LinkedList<Inet4Address> sourceList,LinkedList<Inet4Address> destinationList,RequestParametersConfiguration reqParams, int numberRequests){
			RequestToSend requestToSend=null;
			if ((sourceList!=null)&&(destinationList!=null)){				
				int num_srcs = sourceList.size();
				int num_dsts =  destinationList.size();	
				
				for (int k=0; k<numberRequests;k++){
					for (int i=0;i<num_srcs;++i){
					   for (int j=0;j<num_dsts;++j){				    	
						   if (sourceList.get(i).equals(destinationList.get(j))){
							   log.info("Origen y destino coinciden, paso");
						   }else {
							   requestToSend = new RequestToSend();
							   try {
								   Thread.sleep(1);
							   } catch (InterruptedException e) {
								   // TODO Auto-generated catch block
								   e.printStackTrace();
							   }
							   requestToSend.setSource(sourceList.get(i));
							   requestToSend.setDestiny(destinationList.get(j));
							   requestToSend.setRequestParameters(reqParams);
							   requestToSendList.add(requestToSend);

							   log.info("Creo la request número "+requestToSendList.indexOf(requestToSend)+" :"+ requestToSend.toString());
						   }
					   }
				   }
				}
			}
		}

		public boolean isSvec() {
			return svec;
		}

		public boolean isControlPlaneOption() {
			return controlPlaneOption;
		}
		
	/*	public void readFile (String readFile){	
		try{
		     SAXParserFactory factory = SAXParserFactory.newInstance();
		     SAXParser saxParser = factory.newSAXParser();
		     SaxHandler handler = new SaxHandler();
		     counter = 0;
		 	saxParser.parse(readFile, handler);
			 } 
			   catch (Exception e) {
			   e.printStackTrace();
			   } 
	}
	
	class SaxHandler extends DefaultHandler  {
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
				if (qName.equalsIgnoreCase("ipPCE")){
					ipPCE = tempVal.trim();
				}
				else if (qName.equalsIgnoreCase("PCEServerPort")){
					PCEServerPort = Integer.parseInt(tempVal.trim());
				}
				else if (qName.equalsIgnoreCase("newRequest")){
					counter++;
				}
				else if (qName.equalsIgnoreCase("source")) {
					sourceList.add(counter, (Inet4Address) Inet4Address.getByName(tempVal.trim()));
				}
				else if (qName.equalsIgnoreCase("destination")) {    				
					destinationList.add(counter,(Inet4Address) Inet4Address.getByName(tempVal.trim()));
				}
				else  if (qName.equalsIgnoreCase("meanTimeBetweenRequest")){        			
					meanTimeBetweenRequest=Double.parseDouble(tempVal.trim());
				}
				else if (qName.equalsIgnoreCase("maximumReservableBandwidth")){
					maximumReservableBandwidth.setMaximumReservableBandwidth(Long.parseLong(tempVal.trim()));
				}else if (qName.equalsIgnoreCase("meanConectionTime")){
					//meanConectionTimeList.add(counter,Double.parseDouble(tempVal.trim()));
					meanConectionTime=Double.parseDouble(tempVal.trim());
				}else if (qName.equalsIgnoreCase("seed")){
					//meanConectionTimeList.add(counter,Double.parseDouble(tempVal.trim()));
					seed=Integer.parseInt(tempVal.trim());
				}
				else if (qName.equalsIgnoreCase("isExponential")){
					isExponential= Boolean.parseBoolean(tempVal.trim());
				}
				else if (qName.equalsIgnoreCase("printRequestTime")){
					printRequestTime= Boolean.parseBoolean(tempVal.trim());
				}
				else if (qName.equalsIgnoreCase("random")){
					random= Boolean.parseBoolean(tempVal.trim());
				}
				else if (qName.equalsIgnoreCase("fileStatistics")){
					fileStatistics=tempVal.trim();
				}
				else if (qName.equalsIgnoreCase("of")){
				      int of = Integer.parseInt(tempVal.trim());
				      reqParams.setOf(true);
				      reqParams.setOfCode(of);
				     }
				else if (qName.equalsIgnoreCase("reservation")) { 
				      boolean isReservation=Boolean.parseBoolean(tempVal.trim());
				      reqParams.setReservation(isReservation);
				     }
				else if (qName.equalsIgnoreCase("delayMetric")) { 
				      boolean delayMetric=Boolean.parseBoolean(tempVal.trim());
				      reqParams.setDelayMetric(delayMetric);
				     }
				else if (qName.equalsIgnoreCase("stopCondition")){
					stopCondition=Long.parseLong(tempVal.trim());
				}
				else if (qName.equalsIgnoreCase("timeReserved")){
					timeReserved=Long.parseLong(tempVal.trim());
				}
				else if (qName.equalsIgnoreCase("classToExecute")){
					classToExecute=tempVal.trim();
				}
				else if (qName.equalsIgnoreCase("baseIP")){
					baseIP=tempVal.trim();
				}
				else if (qName.equalsIgnoreCase("numSrcs")){
					sourceList =new LinkedList<Inet4Address>();
					int numSrcs = Integer.parseInt(tempVal.trim());
					Inet4Address source=null;
					for (int k=1;k<=numSrcs;++k ){
						try {
							source = (Inet4Address)Inet4Address.getByName(baseIP+k);
						} catch (UnknownHostException e) {
							e.printStackTrace();
						}
						sourceList.add(source);
					}
				}
				else if (qName.equalsIgnoreCase("numDsts")){
					destinationList=new LinkedList<Inet4Address>();
					int numDsts = Integer.parseInt(tempVal.trim());
					Inet4Address destination=null;
					for (int k=1;k<=numDsts;++k ){
						try {
							destination = (Inet4Address)Inet4Address.getByName(baseIP+k);
						} catch (UnknownHostException e) {
							e.printStackTrace();
						}
						destinationList.add(destination);
					}
					System.out.println("Hemos creado destinations "+numDsts);
				}
				else if (qName.equalsIgnoreCase("src")){
					Inet4Address source;
					try {
						String srcs = tempVal.trim();  
						source = (Inet4Address)Inet4Address.getByName(srcs);
						sourceList.remove(tempSrc);
						sourceList.add(tempSrc,source);
						tempSrc=tempSrc+1;
						System.out.println("Hemos a�adido "+source);
					} catch (UnknownHostException e) {
						e.printStackTrace();
					}

				}else if (qName.equalsIgnoreCase("dst")){
					Inet4Address destination;
					try {
						String dsts = tempVal.trim();  
						destination = (Inet4Address)Inet4Address.getByName(dsts);
						destinationList.remove(tempDst);
						destinationList.add(tempDst,destination);
						tempDst=tempDst+1;
						System.out.println("Hemos a�adido "+destination);
					} catch (UnknownHostException e) {
						e.printStackTrace();
					}
				}

			}catch (Exception e) {
				e.printStackTrace();
			}

		 
			}
		 
			public void characters(char ch[], int start, int length) throws SAXException {
				tempVal = new String(ch,start,length);

		 
			}
		}//End DefaultHandler
*/
//	public RequestParameters getReqParams() {
//		return reqParams;
//	}
//
//
//
//	public void setReqParams(RequestParameters reqParams) {
//		this.reqParams = reqParams;
//	}
}
