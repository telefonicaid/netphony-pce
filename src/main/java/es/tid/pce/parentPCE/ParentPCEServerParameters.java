package es.tid.pce.parentPCE;
import es.tid.pce.computingEngine.AlgorithmRule;
import es.tid.pce.computingEngine.MapAlgoRule;
import es.tid.pce.management.PcepCapability;
import es.tid.tedb.Layer;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

//import java.util.concurrent.Callable;


public class ParentPCEServerParameters {
	
	/**
	 * PCE Server Port of the Parent PCE. Default value 4189
	 */
	private int ParentPCEServerPort = 4189;
	/**
	 * TCP port to connect to manage the Parent PCE
	 */
	private int parentPCEManagementPort = 8888;
	/**
	 * Number of Path Request Processor Threads. Default value 1
	 */
	private int ChildPCERequestsProcessors=1;
	
	/**
	 * File to send the logs. By default ParentPCEServer.log	
	 */
	private String ParentPCEServerLogFile="ParentPCEServer.log";
	
	/**
	 * File to send logs of the PCEP Parser of the Parent PCE. By default ParentPCEPPareserServer.log 
	 */
	private String ParentPCEPParserLogFile="ParentPCEPPareserServer.log";
	
	/**
	 * 
	 */
	private String networkDescriptionFile="MDnetwork.xml";

	private String reachFile="ReachFile.xml";
	private String totalFile="total.xml";
	/**
	 * 
	 */
	private String ITnetworkDescriptionFile="network_IT_102.xml";
	
	/**
	 * 
	 */
	private String MDnetworkDescriptionFile="network_MD.xml";
	
	/**
	 * 
	 */
	private String ITMDnetworkDescriptionFile="network_IT_MD.xml";
	
	/**
	 * 
	 */
	private int initialSessionID=0;
	
	/**
	 * 
	 */
	private AtomicInteger sessionIDCounter; 
	
	/**
	 * 
	 */
	private int KeepAliveTimer=30;
	
	/**
	 * 
	 */
	private int minKeepAliveTimerPCCAccepted=2;
	
	
	/**
	 * 
	 */
	private int maxDeadTimerPCCAccepted=30000;
	/**
	 * 
	 */
	private boolean zeroDeadTimerPCCAccepted=false;


	private boolean testflag=false;


	/**
	 * Dead Timer 
	 */
	private int DeadTimer=120;
	
	private Layer defaultPCELayer;
	
	private boolean multiDomain=true;
	
	private boolean ITcapable=false;
	
	public LinkedList<Layer> PCElayers;
	
	private LinkedList<String> algorithmList;
	
	public LinkedList<MapAlgoRule> algorithmRuleList;
	
	private Level ParentPCELogLevel=Level.SEVERE;
	
	private Level PCEPParserLogLevel=Level.SEVERE;
	
	private boolean readMDTEDFromFile=false;
	
	private boolean strongestLog=false;
	
	private String GUIHost;
	
	private int GUIPort;
	
	private String ParentPCEServerAddress;
	
	private boolean fullTopologyMode=false;
	
	private PcepCapability localPcepCapability;

	/**
	 * BGP. This variable indeicates if the PCE has a BGP module 
	 */
	private boolean actingAsBGP4Peer;
	/**
	 * BGP. File where read the BGP parameters to configure
	 */
	private String BGP4File = "BGP4Parameters.xml";
	/**
	 * The Parent PCE has a database with the whole topology. The interdomain and intradomain links.
	 */
	private boolean knowsWholeTopology = false;
	
	/**
	 * Name of the configuration file
	 */
	private String confFile;

	
	public String getParentPCEServerAddress() {
		return ParentPCEServerAddress;
	}

	public void setParentPCEServerAddress(String parentPCEServerAddress) {
		ParentPCEServerAddress = parentPCEServerAddress;
	}

	public String getGUIHost() {
		return GUIHost;
	}

	public void setGUIHost(String gUIHost) {
		GUIHost = gUIHost;
	}

	public int getGUIPort() {
		return GUIPort;
	}

	public void setGUIPort(int gUIPort) {
		GUIPort = gUIPort;
	}

	public boolean isStrongestLog() {
		return strongestLog;
	}

	public void setStrongestLog(boolean strongestLog) {
		this.strongestLog = strongestLog;
	}

	public boolean isReadMDTEDFromFile() {
		return readMDTEDFromFile;
	}

	public void setReadMDTEDFromFile(boolean readMDTEDFromFile) {
		this.readMDTEDFromFile = readMDTEDFromFile;
	}

	public int getParentPCEServerPort() {
		return ParentPCEServerPort;
	}

	public void setParentPCEServerPort(int parentPCEServerPort) {
		ParentPCEServerPort = parentPCEServerPort;
	}

	public String getNetworkDescriptionFile() {
		return networkDescriptionFile;
	}

	public void setNetworkDescriptionFile(String networkDescriptionFile) {
		this.networkDescriptionFile = networkDescriptionFile;
	}

	public String getReachFile() {
		return reachFile;
	}

	public void setReachFile(String File) {
		this.reachFile = File;
	}

	public String getTotalFile() {
		return totalFile;
	}

	public void setTotalFile(String File) {
		this.totalFile = File;
	}


	public String getITNetworkDescriptionFile() {
		return ITnetworkDescriptionFile;
	}

	public void setITNetworkDescriptionFile(String ITnetworkDescriptionFile) {
		this.ITnetworkDescriptionFile = ITnetworkDescriptionFile;
	}
	
	public String getMDnetworkDescriptionFile() {
		return MDnetworkDescriptionFile;
	}

	public void setMDnetworkDescriptionFile(String mDnetworkDescriptionFile) {
		MDnetworkDescriptionFile = mDnetworkDescriptionFile;
	}
	
	public String getITMDnetworkDescriptionFile() {
		return ITMDnetworkDescriptionFile;
	}

	public void setITMDnetworkDescriptionFile(String ITmDnetworkDescriptionFile) {
		ITMDnetworkDescriptionFile = ITmDnetworkDescriptionFile;
	}

	public LinkedList<String> getAlgorithmList() {
		return algorithmList;
	}

	public void setAlgorithmList(LinkedList<String> algorithmList) {
		this.algorithmList = algorithmList;
	}

	public AtomicInteger getSessionIDCounter() {
		return sessionIDCounter;
	}

	public void setSessionIDCounter(AtomicInteger sessionIDCounter) {
		this.sessionIDCounter = sessionIDCounter;
	}
	
	public int getChildPCERequestsProcessors() {
		return ChildPCERequestsProcessors;
	}

	public void setChildPCERequestsProcessors(int chidPCERequestsProcessors) {
		ChildPCERequestsProcessors = chidPCERequestsProcessors;
	}

	public String getParentPCEServerLogFile() {
		return ParentPCEServerLogFile;
	}

	public void setParentPCEServerLogFile(String parentPCEServerLogFile) {
		ParentPCEServerLogFile = parentPCEServerLogFile;
	}

	public String getParentPCEPParserLogFile() {
		return ParentPCEPParserLogFile;
	}

	public void setParentPCEPParserLogFile(String parentPCEPParserLogFile) {
		ParentPCEPParserLogFile = parentPCEPParserLogFile;
	}

	public int getInitialSessionID() {
		return initialSessionID;
	}

	public void setInitialSessionID(int initialSessionID) {
		this.initialSessionID = initialSessionID;
	}

	public int getKeepAliveTimer() {
		return KeepAliveTimer;
	}

	public void setKeepAliveTimer(int keepAliveTimer) {
		KeepAliveTimer = keepAliveTimer;
	}

	public int getDeadTimer() {
		return DeadTimer;
	}

	public void setDeadTimer(int deadTimer) {
		DeadTimer = deadTimer;
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
	
	

	public Layer getDefaultPCELayer() {
		return defaultPCELayer;
	}

	public void setDefaultPCELayer(Layer defaultPCELayer) {
		this.defaultPCELayer = defaultPCELayer;
	}

	public boolean isMultiDomain() {
		return multiDomain;
	}

	public void setMultiDomain(boolean multiDomain) {
		this.multiDomain = multiDomain;
	}

	public boolean isITCapable() {
		return ITcapable;
	}

	public void setITCapable(boolean itCapable) {
		this.ITcapable = itCapable;
	}
	

	public Level getParentPCELogLevel() {
		return ParentPCELogLevel;
	}

	public void setParentPCELogLevel(Level parentPCELogLevel) {
		ParentPCELogLevel = parentPCELogLevel;
	}

	public Level getPCEPParserLogLevel() {
		return PCEPParserLogLevel;
	}

	public void setPCEPParserLogLevel(Level pCEPParserLogLevel) {
		PCEPParserLogLevel = pCEPParserLogLevel;
	}
	
	


	public int getParentPCEManagementPort() {
		return parentPCEManagementPort;
	}

	public boolean isKnowsWholeTopology() {
		return knowsWholeTopology;
	}

	public boolean isTest() {
		return testflag;
	}

	public PcepCapability getLocalPcepCapability() {
		return localPcepCapability;
	}
	
	/**
	 * Default Constructor.
	 */
	public ParentPCEServerParameters(){
			this.confFile="ParentPCEConf.xml";
	}

	
	/**
	 * Constructor with the name of the configuration file.
	 * @param confFile Name of the configuration file.
	 */
	public ParentPCEServerParameters(String confFile){
		if (confFile!=null){
			this.confFile=confFile;
		}else {
			confFile="PCEServerConfiguration.xml";
		}
	}


	public void initialize(){
		algorithmList=new LinkedList<String>();
		PCElayers=new LinkedList<Layer>();
		 algorithmRuleList=	new LinkedList<MapAlgoRule>();
		 localPcepCapability= new PcepCapability();
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
    		        	  String layer=attributes.getValue("type");    		        	  
    		        	  if (layer.equals("gmpls")){
    		        		  lay.gmpls=true;
    		        		  lay.encodingType=Integer.parseInt(attributes.getValue("encodingType"));
    		        		  lay.switchingType=Integer.parseInt(attributes.getValue("switchingType"));
    		        	  }
    		        	  
    		        	  boolean defaultL=Boolean.parseBoolean(attributes.getValue("default"));
    		        	  if (defaultL==true) {
							  defaultPCELayer = lay;
						  }
    		        	  PCElayers.add(lay);
    		        	  
    		          }	  
		    		  else if (qName.equalsIgnoreCase("algorithm")) {
		    			  String aname=attributes.getValue("name");
		    			  /*
		    		  try {
		    			  
		    			  //ClassLoader classLoader = ParentPCEServerParameters.class.getClassLoader();
		    		        //Class aClass = classLoader.loadClass("tid.pce.computingEngine.algorithms.CPLEXOptimizedPathComputing");
		    			  Class aClass = Class.forName("tid.pce.computingEngine.algorithms.CPLEXOptimizedPathComputing");
		    		        //Callable<PCEPResponse> aCass2=(Callable<PCEPResponse>)aClass;
		    		        Callable<PCEPResponse> cpr= ( Callable<PCEPResponse>)aClass.newInstance();
		    		    } catch (ClassNotFoundException e) {
		    		        e.printStackTrace();
		    		    } catch (InstantiationException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (IllegalAccessException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
		    		 */
		    		  }
		    		  //Andrea
					  // not properly done
					  //I had problem loading the parameters from the configuration file
					  //I configured statically here (of)
		    		  else if (qName.equalsIgnoreCase("algorithmRule")) {
		    			  MapAlgoRule mar= new MapAlgoRule();
		    			  AlgorithmRule ar=new AlgorithmRule();
		    			  String aname=attributes.getValue("name");
						  ar.svec=Boolean.parseBoolean(attributes.getValue("svec"));
		    			  ar.of=3000;

		        		  mar.ar=ar;
		        		  mar.algoName=aname;
						  mar.isParentPCEAlgorithm=true;

						  //mar.isParentPCEAlgorithm=Boolean.parseBoolean(attributes.getValue("isParentPCEAlgorithm"));
		        		  algorithmRuleList.add(mar);
		    		  }

		    	  }
		    	  
		    	   public void endElement(String uri, String localName,
		    		          String qName)
		    		          throws SAXException {
		    		   		  if(qName.equalsIgnoreCase("ParentPCEServerAddress")) {
		    		   			ParentPCEServerAddress=tempVal.trim();
		    		   		  }		    		   
		    		   		  else if(qName.equalsIgnoreCase("ParentPCEServerPort")) {
		    		        	  ParentPCEServerPort=Integer.parseInt(tempVal.trim());
		    		          }

		    		          else if (qName.equalsIgnoreCase("ChildPCERequestsProcessors")) {
		    		        	  ChildPCERequestsProcessors=Integer.parseInt(tempVal.trim());
		    		          }
		    		          else if (qName.equalsIgnoreCase("ParentPCEServerLogFile")) {
		    		        	  ParentPCEServerLogFile=tempVal.trim();
		    		          }
		    		          else if (qName.equalsIgnoreCase("ParentPCEPParserLogFile")) {
		    		        	  ParentPCEPParserLogFile=tempVal.trim();
		    		          }	       
		    		          else if (qName.equalsIgnoreCase("networkDescriptionFile")) {
		    		        	  networkDescriptionFile=tempVal.trim();
		    		          }
		    		          else if (qName.equalsIgnoreCase("MDnetworkDescriptionFile")) {
		    		        	  MDnetworkDescriptionFile=tempVal.trim();
		    		          }
		    		          else if (qName.equalsIgnoreCase("ITnetworkDescriptionFile")) {
		    		        	  ITnetworkDescriptionFile=tempVal.trim();
		    		          }
		    		          else if (qName.equalsIgnoreCase("ITMDnetworkDescriptionFile")) {
		    		        	  ITMDnetworkDescriptionFile=tempVal.trim();
		    		          }
		    		          else if (qName.equalsIgnoreCase("initialSessionID")) {
		    		        	  initialSessionID=Integer.parseInt(tempVal.trim());
		    		          }	  
		    		          else if (qName.equalsIgnoreCase("KeepAliveTimer")) {
		    		        	  KeepAliveTimer=Integer.parseInt(tempVal.trim());
		    		          }	  
		    		          else if (qName.equalsIgnoreCase("DeadTimer")) {
		    		        	  DeadTimer=Integer.parseInt(tempVal.trim());
		    		          }
		    		          else if (qName.equalsIgnoreCase("ParentPCELogLevel")) {
		    		        	  ParentPCELogLevel=Level.parse(tempVal.trim());
		    		          }
		    		          else if (qName.equalsIgnoreCase("PCEPParserLogLevel")) {
		    		        	  PCEPParserLogLevel=Level.parse(tempVal.trim());
		    		          }
		    		          else if (qName.equalsIgnoreCase("OSPFParserLogLevel")) {
		    		        	  //PCEPParserLogLevel=Level.parse(tempVal.trim());
		    		          }else if (qName.equalsIgnoreCase("strongestLog")) {
		    		        	  strongestLog=Boolean.parseBoolean(tempVal.trim());
		    		          } else if (qName.equalsIgnoreCase("GUIHost")) {
		    		        	  GUIHost=tempVal.trim();		    		        	  
		    		          }else if (qName.equalsIgnoreCase("GUIPort")) {
		    		        	  GUIPort=Integer.parseInt(tempVal.trim());
		    		          }else if (qName.equalsIgnoreCase("fullTopologyMode")) {
		    		        	  fullTopologyMode=Boolean.parseBoolean(tempVal.trim());
		    		          }
		    		          else if (qName.equalsIgnoreCase("parentPCEManagementPort")){
		  						parentPCEManagementPort = Integer.parseInt(tempVal.trim());
		  					  }
		    		          else if (qName.equalsIgnoreCase("readMDTEDFromFile")) {
		    		        	  readMDTEDFromFile=Boolean.parseBoolean(tempVal.trim());
		    		          }
		    		  		  else if (qName.equalsIgnoreCase("actingAsBGP4Peer")) {
								actingAsBGP4Peer=Boolean.parseBoolean(tempVal.trim());
						      }
							  else if (qName.equalsIgnoreCase("BGP4File")) {
								BGP4File=tempVal.trim();					
							  }
							  else if (qName.equalsIgnoreCase("multiDomain")) {
								multiDomain=Boolean.parseBoolean(tempVal.trim());
							  }
							  else if (qName.equalsIgnoreCase("knowsWholeTopology")) {
								knowsWholeTopology=Boolean.parseBoolean(tempVal.trim());
							  }
							  else if (qName.equalsIgnoreCase("gmpls")) {
								localPcepCapability.setGmpls(Boolean.parseBoolean(tempVal.trim()));
							  }
						 	  else if (qName.equalsIgnoreCase("stateful")) {
								localPcepCapability.setStateful(Boolean.parseBoolean(tempVal.trim()));
							  }
							  else if (qName.equalsIgnoreCase("testAlgo")) {
								  testflag = Boolean.parseBoolean(tempVal.trim());
							  }

							  else if (qName.equalsIgnoreCase("lspUpdate")) {
								localPcepCapability.setLspUpdate(Boolean.parseBoolean(tempVal.trim()));
							  }
							  else if (qName.equalsIgnoreCase("parentPCE")) {
								localPcepCapability.setParentPCE(Boolean.parseBoolean(tempVal.trim()));
							  }
							  else if (qName.equalsIgnoreCase("childPCE")) {
								localPcepCapability.setChildPCE(Boolean.parseBoolean(tempVal.trim()));
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
			
		sessionIDCounter=new AtomicInteger(initialSessionID);
		
	}
	
	public boolean isActingAsBGP4Peer() {
		return actingAsBGP4Peer;
	}

	public String getBGP4File() {
		return BGP4File;
	}

}
