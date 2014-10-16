package tid.pce.client.tester;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Timer;
import java.util.logging.Logger;

import tid.netManager.NetworkLSPManager;
import tid.pce.client.PCCPCEPSession;
import tid.pce.client.emulator.AutomaticTesterStatistics;
import tid.vntm.LigthPathManagement;
import cern.jet.random.Exponential;
import es.tid.pce.pcep.PCEPProtocolViolationException;
import es.tid.pce.pcep.constructs.Path;
import es.tid.pce.pcep.messages.PCEPRequest;
import es.tid.pce.pcep.messages.PCEPResponse;
import es.tid.pce.pcep.messages.PCEPTELinkSuggestion;
import es.tid.pce.pcep.objects.BandwidthRequested;
import es.tid.pce.pcep.objects.ExplicitRouteObject;
import es.tid.rsvp.objects.subobjects.EROSubobject;
import es.tid.rsvp.objects.subobjects.IPv4prefixEROSubobject;
import es.tid.rsvp.objects.subobjects.SubObjectValues;

public class VNTMActivity implements Activity{
	
	private PCCPCEPSession VNTMSession;
	private NetworkLSPManager networkLSPManager;
	private AutomaticTesterStatistics stats;
	private PCEPRequest request;
	private PCEPResponse response;
	private Logger log = Logger.getLogger("PCCClient");
	private Exponential connectionTime;
	private Timer planificationTimer;
	private double TrafficHops=0;
	private long NumWL=0;
	static long id=1;
	private int id_response_svec=1;
	private LigthPathManagement ligthPathManagement;

	public VNTMActivity (Exponential connectionTime,Timer planificationTimer, AutomaticTesterStatistics stats){
		this.planificationTimer = planificationTimer;
		this.connectionTime=connectionTime;
		this.stats=stats;
	}
	
	public double getTrafficHops() {
		return TrafficHops;
	}

	public void setTrafficHops(double trafficHops) {
		TrafficHops = trafficHops;
	}
	
	@Override
	public void run() {
		if (response.getResponseList().isEmpty()){
			log.severe("ERROR in response");
			//stats.addNoPathResponse();
			System.exit(1);
			return;
		}
		if (response.getResponseList().size()>1){
			int j;
			for (j=0; j<response.getResponseList().size();j++){
				if (response.getResponseList().get(j).getNoPath()!=null){
					log.info("NO PATH");
					stats.addNoPathResponse();
					stats.analyzeBlockingProbability(1);
					stats.analyzeblockProbabilityWithoutStolenLambda(1);
					return;	
				}
			}
			for (j=0; j<response.getResponseList().size();j++){
				Path path=response.getResponseList().get(j).getPath(0);
				ExplicitRouteObject ero=path.geteRO();
				LinkedList<EROSubobject> eroSubObjList=ero.getEROSubobjectList();
				//CREAR LISTA DE EROS
				LinkedList<ExplicitRouteObject> eroList=new LinkedList<ExplicitRouteObject>();			
				LinkedList<ExplicitRouteObject> eroListIP = new LinkedList<ExplicitRouteObject>();
				int numNewLinks = createEROList(eroSubObjList, eroList, eroListIP);
				//Si hay camino multilayer
				if (numNewLinks>0){					
					//stats.addMLResponse();
					for (int i=0;i<numNewLinks;++i){
						// PETICIÓN MULTILAYER
						TrafficHops = TrafficHops + numNewLinks;
						
						log.info("Reserving LSP and sending capacity update");
						PCEPTELinkSuggestion telinksug=new PCEPTELinkSuggestion();
						Path path2=new Path();
						path2.seteRO(eroList.get(i));
						BandwidthRequested bandwidth = new BandwidthRequested();
						float bw=((BandwidthRequested)(response.getResponseList().get(0).getBandwidth())).getBw();
						bandwidth.setBw(bw);
						path2.setBandwidth(bandwidth);
						telinksug.setPath(path2);	
						//telinksug.setMessageType(id_response_svec);
						
						try {
							telinksug.encode();
						} catch (PCEPProtocolViolationException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
						
						log.info("Sending TE LINK SUGGESTION message i:"+i);
						
						//Mandar el LSP Multilayer por PCCPCEPSession al VNTM
						sendLSP(VNTMSession.getOut(), telinksug);
																			
						/*RealiseMLCapacityTask realiseCapacity = new RealiseMLCapacityTask(null,VNTMSession.getOut(),telinksug );	
						long duration = Math.round(connectionTime.nextDouble());
						log.info("LSP duration: "+duration);
						planificationTimer.schedule(realiseCapacity,duration);*/
					}
				}
				
				// No hay camino multilayer --> Utilizamos TE Link de la capa Virtual
				else if (numNewLinks == 0){
					log.info("Reserving LSP and sending capacity update");
					float bw=((BandwidthRequested)(response.getResponseList().get(0).getBandwidth())).getBw();
								
					long time1 = System.nanoTime();
					if (networkLSPManager.setLSP_UpperLayer(eroSubObjList, bw, false)){
						long time2= System.nanoTime();
						double LSPTime = (time2-time1)/1000000;
						stats.analyzeLSPTime(LSPTime);
						stats.addIPResponse();
						stats.addNumberActiveLSP();
						stats.analyzeBlockingProbability(0);
						stats.analyzeLambdaBlockingProbability(0);
						stats.analyzeblockProbabilityWithoutStolenLambda(0);
						RealiseCapacityTask realiseCapacity = new RealiseCapacityTask(networkLSPManager,eroSubObjList,null,false,null, bw, true);
						
						long duration =Math.round(connectionTime.nextDouble());
						log.info("LSP duration: "+duration);
						planificationTimer.schedule(realiseCapacity,duration);
					}
					else {
						stats.addStolenBWLSP();							
						stats.analyzeBlockingProbability(1);
					}
				}
			}
			id_response_svec++;
		} else{
			
			// Respuesta sin SVEC --> Individual //
			if (response.getResponseList().get(0).getNoPath()!=null){
				log.info("NO PATH");
				stats.addNoPathResponse();
				stats.analyzeBlockingProbability(1);
				stats.analyzeblockProbabilityWithoutStolenLambda(1);
				return;	
			}else {
				Path path=response.getResponseList().get(0).getPath(0);
				//stats.analyzeBlockingProbability(0);
				ExplicitRouteObject ero=path.geteRO();
				LinkedList<EROSubobject> eroSubObjList=ero.getEROSubobjectList();
				//CREAR LISTA DE EROS
				LinkedList<ExplicitRouteObject> eroList=new LinkedList<ExplicitRouteObject>();			
				LinkedList<ExplicitRouteObject> eroListIP = new LinkedList<ExplicitRouteObject>();
				eroListIP = null;
				int numNewLinks = createEROList(eroSubObjList, eroList, eroListIP);
				//Si hay camino multilayer
				if (numNewLinks>0){
					//stats.addMLResponse();
					for (int i=0;i<numNewLinks;++i){
						log.info("Reserving LSP and sending capacity update");
						PCEPTELinkSuggestion telinksug=new PCEPTELinkSuggestion();
						Path path2=new Path();
						path2.seteRO(eroList.get(i));
						NumWL = (eroList.get(i).getEROSubobjectList().size() - 4)/2;
						stats.addNumWL(NumWL);
						
						BandwidthRequested bandwidth = new BandwidthRequested();
						float bw=((BandwidthRequested)(response.getResponseList().get(0).getBandwidth())).getBw();
						bandwidth.setBw(bw);
						path2.setBandwidth(bandwidth);
						telinksug.setPath(path2);	

						try {
							telinksug.encode();
						} catch (PCEPProtocolViolationException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
						
						log.info("Sending TE LINK SUGGESTION message i:"+i);
						
						//Mandar el LSP Multilayer por PCCPCEPSession al VNTM
						sendLSP(VNTMSession.getOut(), telinksug);
																			
						/*RealiseMLCapacityTask realiseCapacity = new RealiseMLCapacityTask(null,VNTMSession.getOut(),telinksug );	
						long duration = Math.round(connectionTime.nextDouble());
						log.info("LSP duration: "+duration);
						planificationTimer.schedule(realiseCapacity,duration);*/
					}
					
					/*if (eroListIP != null){
						//Respuesta con partes IP y óptica
						System.exit(0);
					}*/
				}
				
				// No hay camino multilayer --> Utilizamos TE Link de la capa IP/MPLS
				else if (numNewLinks == 0){
					log.info("Reserving LSP and sending capacity update");
					float bw=((BandwidthRequested)(response.getResponseList().get(0).getBandwidth())).getBw();
					TrafficHops=(eroSubObjList.size()-1);	
					long time1 = System.nanoTime();
					if (networkLSPManager.setLSP_UpperLayer(eroSubObjList, bw, false)){
						long time2= System.nanoTime();
						double LSPTime = (time2-time1)/1000000;
						stats.analyzeLSPTime(LSPTime);
						stats.addIPResponse();
						stats.addNumberActiveLSP();
						stats.analyzeBlockingProbability(0);
						stats.analyzeLambdaBlockingProbability(0);
						stats.analyzeblockProbabilityWithoutStolenLambda(0);
						stats.addTrafficHops(TrafficHops);
						RealiseCapacityTask realiseCapacity = new RealiseCapacityTask(networkLSPManager,eroSubObjList,null,false,null, bw, true);
						long duration =Math.round(connectionTime.nextDouble());
						log.info("LSP duration: "+duration);
						planificationTimer.schedule(realiseCapacity,duration);
					}
					else {
						stats.addStolenBWLSP();							
						stats.analyzeBlockingProbability(1);
					}
				}
			}
		}
	}
	private void sendLSP(DataOutputStream out,PCEPTELinkSuggestion telinksug) {
		try {  
			out.write(telinksug.getBytes());
			out.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}   
	}
	
	public int createPathLSP(LinkedList<EROSubobject> eroSubObjList,LinkedList<Inet4Address> path){
		boolean layerInfoFound=false;
		int numNewLinks=0;		
		path=new LinkedList<Inet4Address>();
		for (int i=0;i<eroSubObjList.size();++i){
			if ((eroSubObjList.get(i)).getType()==SubObjectValues.ERO_SUBOBJECT_LAYER_INFO){
				if (layerInfoFound==false){
					layerInfoFound=true;							
					//Create a new ERO to add at the list		
					
					numNewLinks=numNewLinks+1;										
				}else {
					
					layerInfoFound=false;
					path.add(((IPv4prefixEROSubobject)(eroSubObjList.get(i+1))).getIpv4address());
				}
			}
//			else if (layerInfoFound==true){
//				eroSubObjList2.add(eroSubObjList.get(i));
//			}
			else {		
				path.add(((IPv4prefixEROSubobject)(eroSubObjList.get(i))).getIpv4address());
			}
		}
		return numNewLinks;
	}
	int createEROList(LinkedList<EROSubobject> eroSubObjList,LinkedList<ExplicitRouteObject> eroList,
			LinkedList<ExplicitRouteObject> eroListIP){
		
		boolean layerInfoFound=false;
		boolean caminoIP=false;
		int numNewLinks=0;
		LinkedList<EROSubobject> eroSubObjList2=null;
		LinkedList<EROSubobject> eroSubObjList_IP=null;
		
		for (int i=0;i<eroSubObjList.size();++i){
			if ((eroSubObjList.get(i)).getType()==SubObjectValues.ERO_SUBOBJECT_LAYER_INFO){
				if (layerInfoFound==false){
					layerInfoFound=true;
					caminoIP=false;
					//Create a new ERO to add at the list
					ExplicitRouteObject newERO = new ExplicitRouteObject();							
					eroSubObjList2=newERO.getEROSubobjectList();	
					eroSubObjList2.add(eroSubObjList.get(i-1));	
					eroList.add(newERO);
					numNewLinks=numNewLinks+1;
				}else {
					layerInfoFound=false;
					eroSubObjList2.add(eroSubObjList.get(i+1));
				}
			}
			else if (layerInfoFound==true){
				eroSubObjList2.add(eroSubObjList.get(i));
				
				//FuncionesUtiles.printByte(eroSubObjList.get(i).getSubobject_bytes(), "Subojeto 1b");
			}
			/*else if ((eroSubObjList.get(i)).getType()==SubObjectValues.RRO_SUBOBJECT_IPV4ADDRESS && (IPFound == true)){
				
			}*/
			
			/*if (((eroSubObjList.get(i)).getType()==SubObjectValues.ERO_SUBOBJECT_IPV4PREFIX) 
					&& (layerInfoFound==false)){
				if (caminoIP==false){
					if ((eroSubObjList.get(i+1)).getType()==SubObjectValues.ERO_SUBOBJECT_LAYER_INFO){
						continue;
					}
					ExplicitRouteObject newERO_IP = new ExplicitRouteObject();
					eroSubObjList_IP = newERO_IP.getEROSubobjectList();
					eroSubObjList_IP.add(eroSubObjList.get(i));
					eroListIP.add(newERO_IP);
					caminoIP = true;
				}
				else {
					eroSubObjList_IP.add(eroSubObjList.get(i));
				}
			}*/
		}
		return numNewLinks;
	}
	public int createEROList(LinkedList<EROSubobject> eroSubObjList,LinkedList<ExplicitRouteObject> eroList,
			ArrayList<String> sourceList,ArrayList<String> destinationList){
		boolean layerInfoFound=false;
		int numNewLinks=0;
		int counterArray=0;
		sourceList = new ArrayList<String>();
		destinationList = new ArrayList<String>();
		//CREAR LISTA DE EROS
		LinkedList<EROSubobject> eroSubObjList2=null;
		String strPrev=null;
		for (int i=0;i<eroSubObjList.size();++i){
			if ((eroSubObjList.get(i)).getType()==SubObjectValues.ERO_SUBOBJECT_LAYER_INFO){
				if (layerInfoFound==false){
					layerInfoFound=true;							
					//Create a new ERO to add at the list
					ExplicitRouteObject newERO =  new ExplicitRouteObject();							
					eroSubObjList2=newERO.getEROSubobjectList();	
					eroSubObjList2.add(eroSubObjList.get(i-1));							
					eroList.add(newERO);
					numNewLinks=numNewLinks+1;
				}else {
					layerInfoFound=false;
					eroSubObjList2.add(eroSubObjList.get(i+1));
				}
			}
			else if (layerInfoFound==true){
				eroSubObjList2.add(eroSubObjList.get(i));
			}else {		
				String str1=((IPv4prefixEROSubobject)(eroSubObjList.get(i))).getIpv4address().toString().substring(1);
				if (strPrev==null){
					strPrev=str1;
				}else {
					sourceList.add(counterArray, strPrev);							
					destinationList.add(counterArray, str1);
					strPrev=str1;
					counterArray++;
				}															
			}
		}
		return numNewLinks;
	}
	public static String reserveCapacity(String sourceVector, String destVector, String bw){
		String s = "RESERVE:";
		s += sourceVector + ":" + destVector + ":" + bw;

		return s;
	}

	public static void sendUpdate(DataOutputStream out,PCEPTELinkSuggestion telinksug, String update) {
	//	try {
	//	    Socket socket = new Socket(address, port);
	//	    BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
	//	    bufferedWriter.write(update);
	//	    bufferedWriter.flush();
	//	    socket.close();
	//	} catch (UnknownHostException e) {
	//	    e.printStackTrace();
	//	} catch (IOException e) {
	//	    e.printStackTrace();
	//	}
		int bytesToReserve = telinksug.getBytes().length + update.length();
		byte[] bytes = new byte[bytesToReserve]; 
		System.arraycopy(telinksug, 0, bytes, 0, telinksug.getBytes().length);
		System.arraycopy(update, 0,  bytes, telinksug.getBytes().length, update.length());
		try {		
			out.write(bytes);
			out.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static String createLink(String sourceVector, String destVector, String bw) {
		String s = "CREATE:";
		s += sourceVector + ":" + destVector + ":" + bw;
	
		return s;
	}

	@Override
	public void addNetworkEmulator(NetworkLSPManager networkLSPManager) {
		// TODO Auto-generated method stub
		this.networkLSPManager=networkLSPManager;
	}

	@Override
	public void addStatistics(AutomaticTesterStatistics stats) {
		// TODO Auto-generated method stub
		this.stats = stats; 
	}

	@Override
	public void addRequest(PCEPRequest request) {
		// TODO Auto-generated method stub
		this.request = request;
	}

	@Override
	public void addResponse(PCEPResponse response) {
		// TODO Auto-generated method stub
		this.response=response;
	}

	@Override
	public void addVNTMSession(PCCPCEPSession VNTMSession) {
		// TODO Auto-generated method stub
		this.VNTMSession=VNTMSession;
	}

	@Override
	public void addPCEsessionVNTM(PCCPCEPSession vNTMSession) {
		// TODO Auto-generated method stub
	}

	@Override
	public void addLigthPathManagement(LigthPathManagement ligthPathManagement) {
		this.ligthPathManagement=ligthPathManagement;
		
	}
}
