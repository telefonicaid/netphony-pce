package tid.pce.client.tester.restoration;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import es.tid.ospf.ospfv2.OSPFv2LinkStateUpdatePacket;
import es.tid.ospf.ospfv2.lsa.tlv.subtlv.complexFields.BitmapLabelSet;
import es.tid.pce.pcep.constructs.Response;
import es.tid.rsvp.objects.subobjects.EROSubobject;
import es.tid.rsvp.objects.subobjects.IPv4prefixEROSubobject;
import es.tid.rsvp.objects.subobjects.SubObjectValues;
import es.tid.rsvp.objects.subobjects.UnnumberIfIDEROSubobject;
import tid.netManager.NetworkLSPManager;
import tid.netManager.NetworkLSPManagerParameters;
import tid.netManager.OSPFSender;
import tid.netManager.TCPOSPFSender;
import tid.netManager.emulated.AdvancedEmulatedNetworkLSPManager;
import tid.netManager.emulated.CompletedEmulatedNetworkLSPManager;
import tid.netManager.emulated.DummyEmulatedNetworkLSPManager;
import tid.netManager.emulated.SimpleEmulatedNetworkLSPManager;
import tid.pce.client.PCCPCEPSession;
import tid.pce.pcepsession.PCEPSessionsInformation;
import tid.pce.tedb.IntraDomainEdge;
import tid.pce.tedb.SimpleTEDB;


public class RestorationCaseClient {
	private static String networkEmulatorFile="NetworkEmulatorConfiguration.xml";
	private static  RestorationCaseParameters testerParams;
	private static PCCPCEPSession PCEsession;
	/**
	 * Restoration case
	 */
		private static LinkedList<RestorationCaseTable> restorationCaseTable;
		/**
		 * 
		 */
//		private static Logger log;
		private static NetworkLSPManager networkLSPManager = null;
		private static Logger log=Logger.getLogger("PCCClient");
		private static Logger log2=Logger.getLogger("PCEPParser");
		private static Logger log3=Logger.getLogger("OSPFParser");
		private static Logger log4=Logger.getLogger("NetworkLSPManager");
		
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		testerParams = new RestorationCaseParameters();		
		testerParams.readFile(args[0]);
		//Initialize loggers
		FileHandler fh;
		FileHandler fh2;
		FileHandler fh3;
		FileHandler fh4;
		
		try {
			fh=new FileHandler("PCCClient.log");
			fh2=new FileHandler("PCEPClientParser.log");
			fh3=new FileHandler("OSPFParser.log");
			fh4 = new  FileHandler("NetworkLSPManager.log");

			log.addHandler(fh);
			log2.addHandler(fh2);				
			log3.addHandler(fh3);
			log4.addHandler(fh4);
			if (testerParams.isSetTraces() == false){		    	
				log.setLevel(Level.SEVERE);
				log2.setLevel(Level.SEVERE);	
				log3.setLevel(Level.SEVERE);
				log4.setLevel(Level.SEVERE);
			}				
			else{
				log.setLevel(Level.ALL);
				log2.setLevel(Level.ALL);
				log3.setLevel(Level.ALL);
				log4.setLevel(Level.ALL);
			}

		} catch (Exception e1) {
			e1.printStackTrace();
			System.exit(1);
		}
		
		PCEPSessionsInformation pcepSessionManager = new PCEPSessionsInformation();
		PCEsession = new PCCPCEPSession(testerParams.getPCCPCEPsessionParams().getIpPCEList().get(0), testerParams.getPCCPCEPsessionParams().getPCEServerPortList().get(0), testerParams.getPCCPCEPsessionParams().isNoDelay(), pcepSessionManager);
		PCEsession.start();
		//AutomaticTesterStatistics stats = new AutomaticTesterStatistics(testerParams.getLoadIni());;
		networkLSPManager = createNetworkLSPManager();
		sendRestaurationCaseEROList();
		
	}
	
	
	private static void sendRestaurationCaseEROList(){
		File readFile = null;
		FileReader fr_readFile = null;
		BufferedReader br = null;
		FileWriter writeFile = null;
		try {
			// Apertura del fichero y creacion de BufferedReader para poder
			// hacer una lectura comoda (disponer del metodo readLine()).
			String name = "requestsToDo.txt";
			readFile = new File (name);
			fr_readFile = new FileReader (readFile);
			br = new BufferedReader(fr_readFile);
			String linea;
			//Leo la primera linea
			restorationCaseTable = new LinkedList<RestorationCaseTable>();
			int idPath = 0;
			while ((linea=br.readLine())!=null){				
				//Leo palabra por palabra y creo un array de string por cada palabra
				StringTokenizer st = new StringTokenizer(linea);
				String  lambdaString = st.nextToken();
				int lambda_int= Integer.parseInt(lambdaString);
				int i=0;
				ArrayList<String> sourceList = new ArrayList<String> ();
				ArrayList<String> destinyList = new ArrayList<String> ();
				sourceList.add(i,st.nextToken());
				 while (st.hasMoreTokens()) {					 
					 destinyList.add(i,st.nextToken());					 
					 sendEROList(testerParams.getNumberNodes(),testerParams.getBaseIP(),sourceList.get(i),destinyList.get(i),lambda_int);						 
					 sendEROList(testerParams.getNumberNodes(),testerParams.getBaseIP(),destinyList.get(i),sourceList.get(i),lambda_int);
//					 log.info("Envio: ("+ sourceList.get(i)+","+destinyList.get(i)+"). Lambda"+lambdaString);
					 if (st.hasMoreTokens()){
						 sourceList.add(destinyList.get(i));
					 }
					 i++;
					
				 }
				 keepConections(testerParams.getBaseIP(),sourceList,destinyList,idPath);
				 idPath= idPath +2;
			}}
		catch(Exception e){
			e.printStackTrace();
		}finally{
			// En el finally cerramos el fichero, para asegurarnos
			// que se cierra tanto si todo va bien como si salta 
			// una excepcion.
			try{                    
				if( null != fr_readFile ){   
					fr_readFile.close();
					if (null != writeFile)
						writeFile.close();
				}                  
			}catch (Exception e2){ 
				e2.printStackTrace();
			}
		}
		disconnetLink();

	}
	
	
	private static void sendEROList(int numberNodes,String baseIp, String sourceString,String destinationString,int lambda){
		sourceString = baseIp +sourceString;
		destinationString = baseIp + destinationString;
		Inet4Address source=null;
		Inet4Address destination=null;
		try {
			source = (Inet4Address) Inet4Address.getByName(sourceString);
			 destination=(Inet4Address) Inet4Address.getByName(destinationString);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		IntraDomainEdge edge=((SimpleTEDB)networkLSPManager.getDomainTEDB()).getNetworkGraph().getEdge(source, destination);		
		
		byte[] bytesToChange = createBytesBitmap(((BitmapLabelSet) edge.getTE_info().getAvailableLabels().getLabelSet()).getBytesBitMap(),numberNodes,lambda);		
		((BitmapLabelSet) edge.getTE_info().getAvailableLabels().getLabelSet()).arraycopyBytesBitMap(bytesToChange);			
		networkLSPManager.sendMessageOSPF(source, destination);
		//FuncionesUtiles.printByte(bytesToChange, "Enviando...",log);
	}
	/**
	 * This function creates a Logger 
	 * @param nameLogger
	 * @param nameFileHandler
	 * @return Logger
	 */
	private static Logger createLogger(String nameLogger,String nameFileHandler){
		FileHandler fh;
		//String name =testerParams.getNameRestorationCaseFile();
		Logger log = Logger.getLogger(nameLogger);
		try {
			fh=new FileHandler(nameFileHandler);
			fh.setFormatter(new SimpleFormatter());
			log.addHandler(fh);
			log.setLevel(Level.ALL);
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return log;
	}
	private static void disconnetLink(){
		String name = testerParams.getNameRestorationCaseFile();
		/*Creamos el logger donde vamos a escribir las estadisticas*/			
		String nameLogger = "RestorationStatistics_"+name;
		String nameFileHandler = "RestorationStatistics_"+name+"_"+testerParams.getExecutionNumber()+"_.log";
		Logger logStats = createLogger(nameLogger,nameFileHandler);	
		
		/*Creamos el logger donde vamos a escribir los attemps*/
		nameLogger = "attemps_"+name;
		nameFileHandler = "attemps_"+name+"_"+testerParams.getExecutionNumber()+".log";
		Logger logAttemps = createLogger(nameLogger,nameFileHandler);
		
		/*Creamos el logger donde vamos a escribir el tiempo*/
		nameLogger = "Times_"+name;
		nameFileHandler = "Times_"+name;
		Logger logTimes = createLogger(nameLogger,nameFileHandler);		

		/* Programamos la tarea de tirar el enlace en el tiempo indicado */
		Timer disconnectingLinksTimer = new Timer();		
		DisconnectingLinkTask disconnectingLinkTask = new DisconnectingLinkTask(restorationCaseTable,networkLSPManager,testerParams,PCEsession,logStats,logAttemps,logTimes);
		disconnectingLinkTask.setSourceDisconnected(testerParams.getRestorationCaseInformation().getSource());
		disconnectingLinkTask.setDestinationDisconnected(testerParams.getRestorationCaseInformation().getDestination());
		
		disconnectingLinksTimer.schedule(disconnectingLinkTask, testerParams.getRestorationCaseInformation().getTimeToWait());
		
		
		
	}
	private static byte[] createBytesBitmap(byte[] bytesBitMap,int num, int lambda){
		int numberBytes = getNumberBytes(num);
		int lambdaBytes = getNumberBytesLambda(lambda);		
		byte [] bytesBitmap = bytesBitMap.clone();		
//		for (int i =0;i<numberBytes;i++){
//			bytesBitmap[i]=0x00;
//			
//		}
		int lambdaToModify =lambda%8;		
		if (lambdaBytes < numberBytes)
		switch (lambdaToModify){
		case 0:{
			bytesBitmap[lambdaBytes] =(byte) (0x80 | bytesBitmap[lambdaBytes]);				
			break;
		}
		case 1:
			bytesBitmap[lambdaBytes] = (byte)(0x40 | bytesBitmap[lambdaBytes]);
			break;
		case 2:
			bytesBitmap[lambdaBytes] =(byte)( 0x20| bytesBitmap[lambdaBytes]);
			break;
		case 3:
			bytesBitmap[lambdaBytes] =(byte)( 0x10| bytesBitmap[lambdaBytes]);
			break;
		case 4:
			bytesBitmap[lambdaBytes] =(byte)( 0x08| bytesBitmap[lambdaBytes]);
			break;
		case 5:
			bytesBitmap[lambdaBytes] =(byte)( 0x04| bytesBitmap[lambdaBytes]);
			break;
		case 6:
			bytesBitmap[lambdaBytes] =(byte)( 0x02| bytesBitmap[lambdaBytes]);
			break;
		case 7:
			bytesBitmap[lambdaBytes] = (byte)(0x01| bytesBitmap[lambdaBytes]);
			break;
		}
		return bytesBitmap;
	}
	/**
	 * Funcion que transforma una cantidad de bits en el numero de bytes que necesita 
	 * @param numBit
	 * @return
	 */
	private static int getNumberBytes(int numBits){
		int numberBytes = numBits/8;
		if ((numberBytes*8)<numBits){
			numberBytes++;
		}
		return numberBytes;
	}
	private static int getNumberBytesLambda(int numBitsLambda){		
		return numBitsLambda/8;
	}
	 /**
	  * Create a Simple, advanced or complicated NetworkLSPManager 
	  * @param networkEmulatorParams 
	  * @return
	  */
	static NetworkLSPManager createNetworkLSPManager(){
		LinkedBlockingQueue<OSPFv2LinkStateUpdatePacket>  sendingQueue=null;
		NetworkLSPManagerParameters networkEmulatorParams= new NetworkLSPManagerParameters();
		networkEmulatorParams.initialize(networkEmulatorFile);
		NetworkLSPManager networkLSPManager=null;
		
		if (networkEmulatorParams.isOSPF_RAW_SOCKET()){
			OSPFSender ospfsender = new OSPFSender( networkEmulatorParams.getPCETEDBAddressList(), networkEmulatorParams.getAddress());
			ospfsender.start();	
			sendingQueue=ospfsender.getSendingQueue();
		}
		else {
			TCPOSPFSender TCPOSPFsender = new TCPOSPFSender(networkEmulatorParams.getPCETEDBAddressList(),networkEmulatorParams.getOSPF_TCP_PORTList());
			TCPOSPFsender.start();
			sendingQueue=TCPOSPFsender.getSendingQueue();
		}

		if (networkEmulatorParams.getNetworkLSPtype().equals("Simple")){
			networkLSPManager = new SimpleEmulatedNetworkLSPManager(sendingQueue, networkEmulatorParams.getNetworkFile() );
			
		} else if (networkEmulatorParams.getNetworkLSPtype().equals("Advanced")){
			networkLSPManager= new AdvancedEmulatedNetworkLSPManager(sendingQueue, networkEmulatorParams.getNetworkFile() );
		}
		else if (networkEmulatorParams.getNetworkLSPtype().equals("Completed")){
			networkLSPManager = new CompletedEmulatedNetworkLSPManager(sendingQueue, networkEmulatorParams.getNetworkFile(), null,networkEmulatorParams.isMultilayer());
			((CompletedEmulatedNetworkLSPManager)networkLSPManager).setROADMTime(testerParams.getROADMTime());
		}
		else if (networkEmulatorParams.getNetworkLSPtype().equals("dummy")){
			networkLSPManager = new DummyEmulatedNetworkLSPManager();
		}
		return networkLSPManager;
	}
	
	/**
	 * RESTORATION CASE
	 * Funcion que guarda todos los links que estan activos. Todas las respuestas de caminos que nos 
	 * ha devuelto el PCE 
	 * @param eroSubObjList
	 */
	private void keepConections(LinkedList<EROSubobject> eroSubObjList, Response response){				
		for (int i=0; i<eroSubObjList.size()-1;i++){
			Inet4Address src = null;
			Inet4Address dst = null;
			if (eroSubObjList.get(i).getType()==SubObjectValues.ERO_SUBOBJECT_IPV4PREFIX){
				src=((IPv4prefixEROSubobject)eroSubObjList.get(i)).getIpv4address();				
			}else if (eroSubObjList.get(i).getType()==SubObjectValues.ERO_SUBOBJECT_UNNUMBERED_IF_ID){
				src=((UnnumberIfIDEROSubobject)eroSubObjList.get(i)).getRouterID();				
			}
			if (eroSubObjList.get(i+1).getType()==SubObjectValues.ERO_SUBOBJECT_IPV4PREFIX){
				dst=((IPv4prefixEROSubobject)eroSubObjList.get(i+1)).getIpv4address();	
			}else if (eroSubObjList.get(i+1).getType()==SubObjectValues.ERO_SUBOBJECT_UNNUMBERED_IF_ID){
				dst=((UnnumberIfIDEROSubobject)eroSubObjList.get(i+1)).getRouterID();				
			}else if (eroSubObjList.get(i+1).getType()==SubObjectValues.ERO_SUBOBJECT_LABEL){
				if (eroSubObjList.get(i+2).getType()==SubObjectValues.ERO_SUBOBJECT_IPV4PREFIX){				
					dst=((IPv4prefixEROSubobject)eroSubObjList.get(i+2)).getIpv4address();
			}else if (eroSubObjList.get(i+2).getType()==SubObjectValues.ERO_SUBOBJECT_UNNUMBERED_IF_ID){					
					dst=((UnnumberIfIDEROSubobject)eroSubObjList.get(i+2)).getRouterID();					
				}
			}
			if ((src !=null)&&(dst!=null)){
				RestorationCaseTable restorationCase = new RestorationCaseTable();
				restorationCase.setSource(src);
				restorationCase.setDestination(dst);		
			//	restorationCase.setResponse(response);
				restorationCaseTable.add(restorationCase);
			}
		}

	}
	/**
	 * RESTORATION CASE
	 * Funcion que guarda todos los links que estan activos. Todas las respuestas de caminos que nos 
	 * ha devuelto el PCE 
	 * @param eroSubObjList
	 */
	private static void keepConections( String IPbase,ArrayList<String> src, ArrayList<String> dst , int idPath){	
		Inet4Address iniNodePath = null;
		Inet4Address finNodePath = null;
		try {
			iniNodePath = (Inet4Address) Inet4Address.getByName(IPbase+src.get(0));
			finNodePath = (Inet4Address) Inet4Address.getByName(IPbase+dst.get(dst.size()-1));
		} catch (UnknownHostException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		ArrayList<Inet4Address> pathOneDirection = createPath(IPbase,src,  dst);
		ArrayList<Inet4Address> pathOtherDirection = createPath(IPbase,dst,  src);
		
		
		for (int i=0;i< src.size();i++)	{
		if ((src !=null)&&(dst!=null)){
				Inet4Address source = null;
				Inet4Address destination = null;
				RestorationCaseTable restorationCase = new RestorationCaseTable();
				try {
					source = (Inet4Address) Inet4Address.getByName(IPbase+src.get(i));
					destination = (Inet4Address) Inet4Address.getByName(IPbase+dst.get(i));
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}				
				restorationCase.setSource(source);			
				restorationCase.setDestination(destination);
				restorationCase.setIdPath(idPath);
				restorationCase.setInitialNodePath(iniNodePath);
				restorationCase.setFinalNodePath(finNodePath);
				//restorationCase.setResponse(response);
				restorationCase.setPath(pathOneDirection);
				restorationCaseTable.add(restorationCase);
			}
		}
		idPath++;
		for (int i=(dst.size()-1);i>=0;i--)	{
			if ((src !=null)&&(dst!=null)){
				RestorationCaseTable restorationCase= new RestorationCaseTable();
					Inet4Address source = null;
					Inet4Address destination = null;
					try {
						source = (Inet4Address) Inet4Address.getByName(IPbase+dst.get(i));
						destination = (Inet4Address) Inet4Address.getByName(IPbase+src.get(i));
					} catch (UnknownHostException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					restorationCase.setSource(source);
					restorationCase.setDestination(destination);
					restorationCase.setIdPath(idPath);
					restorationCase.setInitialNodePath(finNodePath);
					restorationCase.setFinalNodePath(iniNodePath);
					//restorationCase.setResponse(response);
					restorationCase.setPath(pathOtherDirection);
					restorationCaseTable.add(restorationCase);
				}
			}
//		System.out.println();
//		

//		
//		
		
		
		}

	static ArrayList<Inet4Address> createPath(String IPbase,ArrayList<String> src, ArrayList<String> dst){
	
		ArrayList<Inet4Address> path = new ArrayList<Inet4Address>();
		try {
			for (int i=0;i< src.size();i++)	{

				path.add((Inet4Address) Inet4Address.getByName(IPbase+src.get(i)));

			}
			path.add((Inet4Address) Inet4Address.getByName(IPbase+dst.get(dst.size()-1)));
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return path;
	}


}
