package tid.pce.client.tester.restoration;

import java.io.File;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.CharacterData;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import tid.pce.client.tester.PCCPCEPSessionParameters;
import tid.pce.client.tester.RequestParametersConfiguration;
import tid.pce.client.tester.RequestToSend;
import cern.jet.random.engine.MersenneTwister;

public class RestorationCaseParameters {
	
	private PCCPCEPSessionParameters PCCPCEPsessionParams;

	//private LinkedList<MapAlgoRule> algorithmRuleList;


	private Long ROADMTime;
	private String baseIP="172.20.1";
//	private boolean networkEmulator=false;

	private LinkedList<RequestToSend> requestToSendList;
	private Logger log=Logger.getLogger("PCCClient");
	//private int maxNumberIterations;
	//Variable para poner las trazas del cliente
	//private long printStatisticsTime=10000;
	//private long maxTimeWaitingForResponse_ms = -1;

	private float bandwidth=0;
	//Para listas
	private ArrayList<Double> meanTimeBetweenRequestList;
	private ArrayList<Double> meanConectionTimeList;
	//Number nodes
	private int numberNodes;
	//Port Management Client
	//private int managementClientPort=8890;

	private boolean reservation;
	//private boolean loadBalancing=false;
	private boolean setTraces=false;

	/**
	 * Case emulate restoration
	 */	
	private RestorationCaseInformation restorationCaseInformation;
	private String nameRestorationCaseFile="";
	private String executionNumber="1";
	

	
	public RestorationCaseParameters() {
		requestToSendList = new  LinkedList<RequestToSend>();
		//meanTimeBetweenRequestList = new ArrayList<Double>();
		//algorithmRuleList = new LinkedList<MapAlgoRule>();
		//maximumReservableBandwidth= new MaximumReservableBandwidth();
		//meanConectionTimeList = new ArrayList<Double>();
		//reqParams = new RequestParameters();
		meanTimeBetweenRequestList=new ArrayList<Double>();
		meanConectionTimeList=new ArrayList<Double>();
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
			NodeList ROADMTime_nl = doc.getElementsByTagName("ROADMTime");
			if (ROADMTime_nl!=null){
				if (ROADMTime_nl.getLength()>0){					
					Element ROADMTime_el = (Element) ROADMTime_nl.item(0);
					String ROADMTime_s = getCharacterDataFromElement(ROADMTime_el);
					ROADMTime=Long.parseLong(ROADMTime_s);
					//nameRestorationCaseFile ="_ROADM_"+ROADMTime_s;
					
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
								nameRestorationCaseFile = nameRestorationCaseFile +"_"+ source_s+"_"+destination_s;
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

			
			NodeList numberNodes_nl = doc.getElementsByTagName("numberNodes");
			if (numberNodes_nl!=null){
				if (numberNodes_nl.getLength()>0){					
					Element numberNodes_el = (Element) numberNodes_nl.item(0);
					String numberNodes_s = getCharacterDataFromElement(numberNodes_el);
					numberNodes=Integer.parseInt(numberNodes_s);
					}
			}			
			
			NodeList setTraces_nl = doc.getElementsByTagName("setTraces");
			if (setTraces_nl!=null){
				if (setTraces_nl.getLength()>0){					
					Element setTraces_el = (Element) setTraces_nl.item(0);
					String setTraces_s = getCharacterDataFromElement(setTraces_el);
					setTraces=Boolean.parseBoolean(setTraces_s);
					
					}
			}
//			NodeList maxTimeWaitingForResponse_ms_nl = doc.getElementsByTagName("maxTimeWaitingForResponse_ms");
//			if (maxTimeWaitingForResponse_ms_nl!=null){
//				if (maxTimeWaitingForResponse_ms_nl.getLength()>0){					
//					Element maxTimeWaitingForResponse_ms_el = (Element) maxTimeWaitingForResponse_ms_nl.item(0);
//					String maxTimeWaitingForResponse_ms_s = getCharacterDataFromElement(maxTimeWaitingForResponse_ms_el);
//					maxTimeWaitingForResponse_ms=Long.parseLong(maxTimeWaitingForResponse_ms_s);
//				}
//			}

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
					//reservation
					NodeList reservation_nl = commonRequestParameters_el.getElementsByTagName("reservation");
					if (reservation_nl!=null){
						Element reservation_el = (Element) reservation_nl.item(0);
						reservation=Boolean.parseBoolean(getCharacterDataFromElement(reservation_el));
						log.info("Reservation: " +reservation);
						reqParams.setReservation(reservation);
					}
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
						requestToSendList.add(request);
					}

					}
					else if(request_el.getAttributeNode("type").getValue().equals("List")){
						//Crear la lista						
					LinkedList<Inet4Address> sourceList=new LinkedList<Inet4Address>();
					LinkedList<Inet4Address> destinationList = new LinkedList<Inet4Address>();
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
						
						createListRequestToSend(sourceList,destinationList, reqParams);
						
					}else if (request_el.getAttributeNode("type").getValue().equals("Random")){
						//Crear la lista
//						int numberNodes=0;
						int numberRequests=0;
						baseIP=null;

//						NodeList numberRequests_nl = request_el.getElementsByTagName("numberRequests");
//						if (numberRequests_nl!=null){												
//							Element numberRequests_el = (Element) numberRequests_nl.item(0);
//							String numberRequests_s = getCharacterDataFromElement(numberRequests_el);
//							numberRequests=Integer.parseInt(numberRequests_s);
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
		
		public PCCPCEPSessionParameters getPCCPCEPsessionParams() {
			return PCCPCEPsessionParams;
		}


		public void setPCCPCEPsessionParams(
				PCCPCEPSessionParameters pCCPCEPsessionParams) {
			PCCPCEPsessionParams = pCCPCEPsessionParams;
		}


		public Long getROADMTime() {
			return ROADMTime;
		}


		public void setROADMTime(Long rOADMTime) {
			ROADMTime = rOADMTime;
		}


		public LinkedList<RequestToSend> getRequestToSendList() {
			return requestToSendList;
		}


		public void setRequestToSendList(LinkedList<RequestToSend> requestToSendList) {
			this.requestToSendList = requestToSendList;
		}


		public String getBaseIP() {
			return baseIP;
		}


		public void setBaseIP(String baseIP) {
			this.baseIP = baseIP;
		}


		public void setRestorationCaseInformation(
				RestorationCaseInformation restorationCaseInformation) {
			this.restorationCaseInformation = restorationCaseInformation;
		}


		public void setExecutionNumber(String executionNumber) {
			this.executionNumber = executionNumber;
		}


		/* TRACES */
//		public boolean isSetTraces() {
//			return setTraces;
//		}
//		public void setSetTraces(boolean setTraces) {
//			this.setTraces = setTraces;
//		}

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


		public boolean isSetTraces() {
			return setTraces;
		}


		public void setSetTraces(boolean setTraces) {
			this.setTraces = setTraces;
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

}
