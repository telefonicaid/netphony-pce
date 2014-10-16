package tid.pce.client.tester.restoration;

import java.io.IOException;
import java.net.Inet4Address;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.TimerTask;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import es.tid.ospf.ospfv2.lsa.tlv.subtlv.complexFields.BitmapLabelSet;
import es.tid.pce.pcep.constructs.Path;
import es.tid.pce.pcep.constructs.Request;
import es.tid.pce.pcep.constructs.Response;
import es.tid.pce.pcep.messages.PCEPRequest;
import es.tid.pce.pcep.objects.BandwidthRequested;
import es.tid.pce.pcep.objects.EndPointsIPv4;
import es.tid.pce.pcep.objects.ExplicitRouteObject;
import es.tid.pce.pcep.objects.Metric;
import es.tid.pce.pcep.objects.ObjectParameters;
import es.tid.pce.pcep.objects.ObjectiveFunction;
import es.tid.pce.pcep.objects.RequestParameters;
import es.tid.pce.pcep.objects.Reservation;
import es.tid.rsvp.objects.subobjects.EROSubobject;
import es.tid.rsvp.objects.subobjects.IPv4prefixEROSubobject;
import es.tid.rsvp.objects.subobjects.SubObjectValues;
import es.tid.rsvp.objects.subobjects.UnnumberIfIDEROSubobject;
import tid.netManager.NetworkLSPManager;
import tid.pce.client.PCCPCEPSession;
import tid.pce.tedb.IntraDomainEdge;
import tid.pce.tedb.SimpleTEDB;

/**
 * Class in charge of simulate a link failure. It is simulated filling the hole link with occupied lambdas
 * @author mcs
 *
 */
public class DisconnectingLinkTask extends TimerTask {

	/**
	 * Links established at the beginning.  
	 */
	LinkedList<RestorationCaseTable>  restorationCaseTableList;
	/**
	 * Links established when the requests have to be sent several times.
	 */
	LinkedList<RestorationCaseTable>  newRestorationCaseTableList;
	/**
	 * Source of the link which is disconnected
	 */
	Inet4Address sourceDisconnected;
	/**
	 * Destination of the link which is disconnected
	 */
	Inet4Address destinationDisconnected;
	/**
	 * Network emulator in charge of check if the new path replied by PCE is available or not
	 */
	NetworkLSPManager networkLSPManager;
	/**
	 * Loggers
	 */
	Logger log;
	Logger logStats;
	Logger logAttemps;
	Logger logTimes;
	Logger logTiemposVsNumberSaltos;
	Logger logSendResponse;
	Logger logRequestSent;
	/**
	 * Number of responses affected in the link failure
	 */
	int nummberResponsedAffected;

	/**
	 * Ejecutor de los casos de restauracion
	 */
	private ScheduledThreadPoolExecutor requestExecutor;

	/**
	 * Parametros de la emulaci�n (como son las peticiones, par origen destino, carga, etc)
	 */
	private RestorationCaseParameters testerParams;
	/**
	 * Session with PCE
	 */
	PCCPCEPSession PCEsession;
	/**
	 * Constructor
	 * @param restorationCaseTable
	 * @param networkLSPManager
	 * @param testerParams
	 * @param PCEsession
	 * @param logStats
	 * @param logAttemps
	 * @param logTimes
	 */
	public DisconnectingLinkTask (LinkedList<RestorationCaseTable> restorationCaseTable,NetworkLSPManager networkLSPManager,RestorationCaseParameters testerParams,PCCPCEPSession PCEsession, Logger logStats, Logger logAttemps,Logger logTimes){
		log = Logger.getLogger("PCCClient");
		this.logStats=logStats;
		this.logAttemps=logAttemps;
		this.logTimes=logTimes;
		nummberResponsedAffected=0;
		FileHandler fh1;
		logSendResponse = Logger.getLogger("logSendResponse");
		try {
			fh1=new FileHandler("logSendResponse.log");
			fh1.setFormatter(new SimpleFormatter());
			logSendResponse.addHandler(fh1);
			logSendResponse.setLevel(Level.ALL);			
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		FileHandler fh2;
		logRequestSent= Logger.getLogger("logRequestSent");
		try {
			fh2=new FileHandler("logRequestSent.log");
			fh2.setFormatter(new SimpleFormatter());
			logRequestSent.addHandler(fh2);
			logRequestSent.setLevel(Level.ALL);			
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		FileHandler fh3;
		logTiemposVsNumberSaltos= Logger.getLogger("logTiemposVsNumberSaltos");
		try {
			fh3=new FileHandler("logTiemposVsNumberSaltos"+testerParams.getRequestToSendList().get(0).getRequestParameters().getTimeReserved()+"_"+testerParams.getExecutionNumber()+".log");
			fh3.setFormatter(new SimpleFormatter());
			logTiemposVsNumberSaltos.addHandler(fh3);
			logTiemposVsNumberSaltos.setLevel(Level.ALL);			
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.restorationCaseTableList=restorationCaseTable;
		this.networkLSPManager=networkLSPManager;
		this.testerParams=testerParams;
		this.PCEsession=PCEsession;
	}
	@Override
	public void run() {
		// TODO Auto-generated method stub
//		log.info("---------------------------------------------------------------------------------------------------");
//		log.info("----------------------------------- Desconectamos enlace!! ------------------------------------------");
//		log.info("---------------------------------------------------------------------------------------------------");
//		/* Desconecta el enlace. Se envian en el update de OSPF todo 111s en el enlace ca�do, mientras dure la caida.  */	
//		log.info("Source: "+this.sourceDisconnected.toString());
//		log.info("destination: "+this.destinationDisconnected.toString());
		IntraDomainEdge edge=((SimpleTEDB)networkLSPManager.getDomainTEDB()).getNetworkGraph().getEdge(sourceDisconnected, destinationDisconnected);		
		byte[] bytesToChange = createBytesFilled(getNumberBytes(edge.getTE_info().getAvailableLabels().getLabelSet().getNumLabels()), (byte)0xFF);		
		((BitmapLabelSet) edge.getTE_info().getAvailableLabels().getLabelSet()).arraycopyBytesBitMap(bytesToChange);			
	
		networkLSPManager.sendMessageOSPF(sourceDisconnected, destinationDisconnected);	
		/*Bidirectional */	
		IntraDomainEdge edge_op=((SimpleTEDB)networkLSPManager.getDomainTEDB()).getNetworkGraph().getEdge(destinationDisconnected,sourceDisconnected);
		((BitmapLabelSet) edge_op.getTE_info().getAvailableLabels().getLabelSet()).arraycopyBytesBitMap(bytesToChange);
		networkLSPManager.sendMessageOSPF(destinationDisconnected,sourceDisconnected);
		log.info("Mandando OSPF todo a 1111s para simular una caida de enlace.");	
		
		
		/* Enviar solicitudes PCEP con las demandas que pasan por el enlace X (previamente guardadsd).*/		
		RestorationCaseStatistics restorationCaseStatistics = new RestorationCaseStatistics();
		sendPCEPRequestAffected(restorationCaseStatistics);
		
		try {
			Thread.sleep(200000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (nummberResponsedAffected !=0){
			restorationCaseStatistics.calculateRetries();
			restorationCaseStatistics.calculateTime();
			restorationCaseStatistics.calculateLSPTime();
			logStats.info("Stats:\t"+ restorationCaseStatistics.printStatistics());
			logAttemps.info(restorationCaseStatistics.printAttemps());
			logTimes.info(restorationCaseStatistics.printTimes());
		}
		else {
			restorationCaseStatistics.setMaxRestries(0);
			restorationCaseStatistics.setMaxTime(0);
			restorationCaseStatistics.setMeanRestries(0);
			restorationCaseStatistics.setMeanTime(0);
			logStats.info("Stats:\t"+ restorationCaseStatistics.printStatistics());
		}
		//Mandar al PCE removeLSP
		//Previas
		int i=0;

		System.exit(0);
	}
	/**
	 * This function schedule new tasks to send again the requests affected by the connection fallen.
	 * @param restorationCaseStatistics
	 */
	private void sendPCEPRequestAffected(RestorationCaseStatistics restorationCaseStatistics ){				
		//logStats.info("Link affected:"+sourceDisconnected +" : "+this.destinationDisconnected);
		newRestorationCaseTableList = new LinkedList<RestorationCaseTable>();
		requestExecutor = new ScheduledThreadPoolExecutor(3*28);
		long time_start = System.nanoTime();
		//logRequestSent.info("1. "+System.currentTimeMillis());
		ArrayList<SendRequestRestorationCase> sendRequestList = new ArrayList<SendRequestRestorationCase> ();
		ArrayList<Long> timeList = new ArrayList<Long>();
		for (RestorationCaseTable restorationCaseTable : restorationCaseTableList ){			
			if (isLinkAffected(restorationCaseTable)){
				//System.out.println("Link affected: ("+restorationCaseTable.getSource()+"-"+ restorationCaseTable.getDestination()+")"+ restorationCaseTable.getIdPath());
				//Enviar PCEP. 
				//PCEPRequest request =calculateRequest(restorationCaseTable.getResponse());
				PCEPRequest request = calculateRequest(restorationCaseTable.getIdPath());		
				long time= calculateTimeToSendRequest(restorationCaseTable.getPath());
				timeList.add(nummberResponsedAffected,time);	
				SendRequestRestorationCase sendRequest = new SendRequestRestorationCase(restorationCaseStatistics,newRestorationCaseTableList,networkLSPManager,System.nanoTime(),time,requestExecutor);			
				sendRequest.setPCEsession(PCEsession);
				sendRequest.setRequest(request);	
				sendRequest.setNumberRetries(0);
				//long time = 0;//calculateTimeToSendRequest(request);
				Long initialTime = System.nanoTime();
				sendRequest.setInitialTime(initialTime);
				sendRequestList.add(nummberResponsedAffected,sendRequest);
				nummberResponsedAffected++;
			}
		}
		
		//logRequestSent.info("2. "+System.currentTimeMillis());
		for (int i=0;i<sendRequestList.size();i++){
				//long time=1;
				//logStats.info("Response affected:\t" + restorationCaseTable.getResponse().toString() +" Time to send it: "+time);
			//	Timer timerToSendRequest = new Timer(); 			
				requestExecutor.schedule(sendRequestList.get(i),timeList.get(i),TimeUnit.MILLISECONDS);
			
			}
		
		//logRequestSent.info("3. "+System.currentTimeMillis());
		logStats.info("nummberResponsedAffected:\t"+nummberResponsedAffected);

	}
	
	
	public PCEPRequest createPCEPRequestMessage(Inet4Address source,Inet4Address destination ){
		PCEPRequest requestMessage= new PCEPRequest();
		requestMessage.addRequest(createRequest(source,destination));
		return requestMessage;
	}

	
	/**
	 * Create a request object
	 * @param src_ip
	 * @param dst_ip
	 * @return
	 */
	private Request createRequest(Inet4Address source,Inet4Address destination){
		Request req = new Request();
		System.out.println("Createing request: "+source +" - " + destination);
		//RequestParameters
		RequestParameters rp= new RequestParameters();
		rp.setPbit(true);				
		rp.setRequestID(PCCPCEPSession.getNewReqIDCounter());		
		rp.setPrio(testerParams.getRequestToSendList().get(0).getRequestParameters().getPriority());		
		rp.setReopt(testerParams.getRequestToSendList().get(0).getRequestParameters().isReoptimization());	
		rp.setBidirect(testerParams.getRequestToSendList().get(0).getRequestParameters().isBidirectional());
		rp.setLoose(testerParams.getRequestToSendList().get(0).getRequestParameters().isLoose());
		req.setRequestParameters(rp);
		//EndPoints
		EndPointsIPv4 ep=new EndPointsIPv4();				
		req.setEndPoints(ep);
		ep.setSourceIP(source);	
		ep.setDestIP(destination);
		
		if (testerParams.getRequestToSendList().get(0).getRequestParameters().isDelayMetric()){
		     Metric metric = new Metric();
		     metric.setMetricType(ObjectParameters.PCEP_METRIC_TYPE_LATENCY_METRIC);
		     metric.setComputedMetricBit(true);
		     req.getMetricList().add(metric);
		   
		  }
		//  if (testerParams.getRequestToSendList().get(0).getRequestParameters().isOf()){
		
		   ObjectiveFunction of = new ObjectiveFunction();
		   req.setObjectiveFunction(of);
		   of.setOFcode(1001); //AURE!!
		  //System.out.println("Code: "+testerParams.getRequestToSendList().get(0).getRequestParameters().getOfCode());
		  //}

		  if (testerParams.getRequestToSendList().get(0).getRequestParameters().isReservation()){
		   Reservation res= new Reservation();
		   log.info("Metemos reserva!!!!");
		   req.setReservation(res);
		   res.setTimer(testerParams.getRequestToSendList().get(0).getRequestParameters().getTimeReserved());
		  }
		  if (testerParams.getRequestToSendList().get(0).getRequestParameters().Is_bandwidth()){
			   BandwidthRequested bw= new BandwidthRequested();
			   bw.setBw(testerParams.getRequestToSendList().get(0).getRequestParameters().getBW());
			   req.setBandwidth(bw);
			  }
		
		return req;
	}
	private boolean isLinkAffected(RestorationCaseTable restorationCaseTable) {
		return (restorationCaseTable.isTheSameLink(sourceDisconnected, destinationDisconnected)||restorationCaseTable.isTheSameLink(destinationDisconnected,sourceDisconnected));
	}
	public byte[]  createBytesFilled(int num, byte content){
		byte [] bytesBitmap = new byte[num];
		for (int i =0;i<num;i++){
			bytesBitmap[i]= content;

		}	
		return bytesBitmap;
	}

	public Long calculateTimeToSendRequest(ArrayList<Inet4Address> path){
		Long time = (long) 0;
		boolean addDelay=true;
		for (int i=0;i<path.size();i++){
			if (sourceDisconnected.equals(path.get(i))){
				addDelay=false;				
			}
			if (addDelay){
				time = (long) (time+ 1);
			}
		}
		return time;
	
	}
	public Long calculateTimeToSendRequest(Response response){
		Long time = (long) 0;
		Inet4Address src=null;
		Inet4Address dst=null;
		boolean addDelay=true;
		
		
		//Mirar el retardo desde el enlace donde esta el fallo hasta el origen.  
		LinkedList<EROSubobject>  eroSubObjList = response.getPath(0).geteRO().getEROSubobjectList();
		for (int i=0; i<eroSubObjList.size()-1;i++){			
			if (eroSubObjList.get(i).getType()==SubObjectValues.ERO_SUBOBJECT_IPV4PREFIX){
				src= ((IPv4prefixEROSubobject)eroSubObjList.get(i)).getIpv4address();
				if (sourceDisconnected.equals(((IPv4prefixEROSubobject)eroSubObjList.get(i)).getIpv4address())){					
					addDelay=false;
				}						
			}else if (eroSubObjList.get(i).getType()==SubObjectValues.ERO_SUBOBJECT_UNNUMBERED_IF_ID){
				src = ((UnnumberIfIDEROSubobject)eroSubObjList.get(i)).getRouterID();
				if (sourceDisconnected.equals(((UnnumberIfIDEROSubobject)eroSubObjList.get(i)).getRouterID())){
					addDelay=false;
				}				
				
			}
			if (eroSubObjList.get(i+1).getType()==SubObjectValues.ERO_SUBOBJECT_IPV4PREFIX){
				dst = ((IPv4prefixEROSubobject)eroSubObjList.get(i+1)).getIpv4address();				
			}else if (eroSubObjList.get(i+1).getType()==SubObjectValues.ERO_SUBOBJECT_UNNUMBERED_IF_ID){
				dst= ((UnnumberIfIDEROSubobject)eroSubObjList.get(i+1)).getRouterID();				
			}else if (eroSubObjList.get(i+1).getType()==SubObjectValues.ERO_SUBOBJECT_LABEL){

				if (eroSubObjList.get(i+2).getType()==SubObjectValues.ERO_SUBOBJECT_IPV4PREFIX){
					dst = ((IPv4prefixEROSubobject)eroSubObjList.get(i+2)).getIpv4address();					
				}else if (eroSubObjList.get(i+2).getType()==SubObjectValues.ERO_SUBOBJECT_UNNUMBERED_IF_ID){
					dst = ((UnnumberIfIDEROSubobject)eroSubObjList.get(i+2)).getRouterID();				
				}
			}
			if (addDelay){
				IntraDomainEdge edge=((SimpleTEDB)networkLSPManager.getDomainTEDB()).getNetworkGraph().getEdge(src, dst);				
				if ( edge.getDelay_ms() !=0){
					time = (long) (time+ edge.getDelay_ms());
				}
				else {
					time = (long) (time+ 1);
					}
				}
		}
			
		return time;
	}
public RestorationCaseTable findOneRestorationCaseTable(int idPath){
	for (int i=0; i< restorationCaseTableList.size();i++){
		if (restorationCaseTableList.get(i).getIdPath() ==  idPath) {
			return restorationCaseTableList.get(i);
		}
	}
	return null;
	
}
	public PCEPRequest calculateRequest(int idPath){
		PCEPRequest request = null;
		RestorationCaseTable restorationCaseTable = findOneRestorationCaseTable(idPath);
		if (restorationCaseTable == null){
			System.out.println("Error.");
			System.exit(3);
		}
		request= createPCEPRequestMessage(restorationCaseTable.getInitialNodePath(),restorationCaseTable.getFinalNodePath());
		return request;
	}
	public PCEPRequest calculateRequest(Response response){	
		Inet4Address initialSrc=null;
		Inet4Address finalDst=null;
		PCEPRequest request = null;
		
		//Mirar el retardo desde el enlace donde esta el fallo hasta el origen.  
		LinkedList<EROSubobject>  eroSubObjList = response.getPath(0).geteRO().getEROSubobjectList();
		for (int i=0; i<eroSubObjList.size()-1;i++){			
			if (eroSubObjList.get(i).getType()==SubObjectValues.ERO_SUBOBJECT_IPV4PREFIX){				
				if (i==0){
					initialSrc=((IPv4prefixEROSubobject)eroSubObjList.get(i)).getIpv4address();
				}
			}else if (eroSubObjList.get(i).getType()==SubObjectValues.ERO_SUBOBJECT_UNNUMBERED_IF_ID){						
				if (i==0){
					initialSrc=((UnnumberIfIDEROSubobject)eroSubObjList.get(i)).getRouterID();
				}
			}
			if (eroSubObjList.get(i+1).getType()==SubObjectValues.ERO_SUBOBJECT_IPV4PREFIX){
				if (i==(eroSubObjList.size()-2)){
					finalDst=((IPv4prefixEROSubobject)eroSubObjList.get(i+1)).getIpv4address();
				}

			}else if (eroSubObjList.get(i+1).getType()==SubObjectValues.ERO_SUBOBJECT_UNNUMBERED_IF_ID){
				if (i==(eroSubObjList.size()-2)){
					finalDst=((UnnumberIfIDEROSubobject)eroSubObjList.get(i+1)).getRouterID();
				}
			}else if (eroSubObjList.get(i+1).getType()==SubObjectValues.ERO_SUBOBJECT_LABEL){

				if (eroSubObjList.get(i+2).getType()==SubObjectValues.ERO_SUBOBJECT_IPV4PREFIX){
					if (i==(eroSubObjList.size()-3)){
						finalDst=((IPv4prefixEROSubobject)eroSubObjList.get(i+2)).getIpv4address();
					}
				}else if (eroSubObjList.get(i+2).getType()==SubObjectValues.ERO_SUBOBJECT_UNNUMBERED_IF_ID){
					
					if (i==(eroSubObjList.size()-3)){
						finalDst=((UnnumberIfIDEROSubobject)eroSubObjList.get(i+2)).getRouterID();
					}
				}
			}			
		}
			request= createPCEPRequestMessage(initialSrc,finalDst);

		return request;
	}
	
	private void sendPCERemoveLSPs(){		
		IntraDomainEdge edge= null;
		IntraDomainEdge edge_op= null;
		byte[] bytesToChange;
		if (restorationCaseTableList != null){
			for (RestorationCaseTable restorationCaseTable : restorationCaseTableList ){				
				Path path=restorationCaseTable.getResponse().getPath(0);
				ExplicitRouteObject ero=path.geteRO();
				LinkedList<EROSubobject> eroSubObjList=ero.getEROSubobjectList();	
				networkLSPManager.removeLSP(eroSubObjList,testerParams.getRequestToSendList().get(0).getRequestParameters().isBidirectional(),null);
				edge=((SimpleTEDB)networkLSPManager.getDomainTEDB()).getNetworkGraph().getEdge(restorationCaseTable.getSource(), restorationCaseTable.getDestination());		
			
				bytesToChange = createBytesFilled(getNumberBytes(edge.getTE_info().getAvailableLabels().getLabelSet().getNumLabels()), (byte)0x00);		
				((BitmapLabelSet) edge.getTE_info().getAvailableLabels().getLabelSet()).arraycopyBytesBitMap(bytesToChange);			
				networkLSPManager.sendMessageOSPF(restorationCaseTable.getSource(), restorationCaseTable.getDestination());
			
		}
		}
		//Despues de la restauracion
		int i=0;
		if (newRestorationCaseTableList != null){
			//for (RestorationCaseTable restorationCaseTable : newRestorationCaseTableList ){
			for (int j = 0;j< newRestorationCaseTableList.size();j++){
				System.out.println("newRestorationCaseTableList. "+newRestorationCaseTableList.size()+"-"+ j);
				//Path path=restorationCaseTable.getResponse().getPath(0);
				if (newRestorationCaseTableList.get(j) != null){
					if (newRestorationCaseTableList.get(j).getResponse() != null){
						if (newRestorationCaseTableList.get(j).getResponse().getNoPath() != null){
							System.out.println("No path");
						}
						else {
						Path path=newRestorationCaseTableList.get(j).getResponse().getPath(0);
						ExplicitRouteObject ero=path.geteRO();
						LinkedList<EROSubobject> eroSubObjList=ero.getEROSubobjectList();	
						networkLSPManager.removeLSP(eroSubObjList,testerParams.getRequestToSendList().get(0).getRequestParameters().isBidirectional(),null);
						//			edge=((SimpleTEDB)networkLSPManager.getDomainTEDB()).getNetworkGraph().getEdge(restorationCaseTable.getSource(), restorationCaseTable.getDestination());		
						//			
						//			bytesToChange = createBytesFilled(getNumberBytes(edge.getTE_info().getAvailableLabels().getLabelSet().getNumLabels()), (byte)0x00);		
						//			((BitmapLabelSet) edge.getTE_info().getAvailableLabels().getLabelSet()).changeBytesBitMap(bytesToChange);			
						//			networkLSPManager.sendMessageOSPF(restorationCaseTable.getSource(), restorationCaseTable.getDestination());
						}
					}
				}
			}

		}

		//Link de la restauracion
		bytesToChange = createBytesFilled(getNumberBytes(edge.getTE_info().getAvailableLabels().getLabelSet().getNumLabels()), (byte)0x00);		
		((BitmapLabelSet) edge.getTE_info().getAvailableLabels().getLabelSet()).arraycopyBytesBitMap(bytesToChange);
		networkLSPManager.sendMessageOSPF(sourceDisconnected, destinationDisconnected);	
		/*Bidirectional */			
		((BitmapLabelSet) edge_op.getTE_info().getAvailableLabels().getLabelSet()).arraycopyBytesBitMap(bytesToChange);
		networkLSPManager.sendMessageOSPF(destinationDisconnected,sourceDisconnected);
		System.out.println("restorationCaseTableList.size() --> "+restorationCaseTableList.size());
		System.out.println("newRestorationCaseTableList.size() --> "+newRestorationCaseTableList.size());
		try {
			System.out.println("Antes de dormir");
			Thread.sleep(20000);
			System.out.println("Despues de dormir");
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	/**
	 * Funcion que transforma una cantidad de bits en el numero de bytes que necesita 
	 * @param numBit
	 * @return
	 */
	private int getNumberBytes(int numBits){
		int numberBytes = numBits/8;
		if ((numberBytes*8)<numBits){
			numberBytes++;
		}
		return numberBytes;
	}

	public Inet4Address getSourceDisconnected() {
		
		return sourceDisconnected;
	}
	public void setSourceDisconnected(Inet4Address sourceDisconnected) {
	
		this.sourceDisconnected = sourceDisconnected;
	}
	public Inet4Address getDestinationDisconnected() {
		return destinationDisconnected;
	}
	public void setDestinationDisconnected(Inet4Address destinationDisconnected) {
		
		this.destinationDisconnected = destinationDisconnected;
	}
	
	

}
