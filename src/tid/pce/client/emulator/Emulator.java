package tid.pce.client.emulator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import tid.netManager.NetworkLSPManager;
import tid.netManager.NetworkLSPManagerTypes;
import tid.netManager.emulated.CompletedEmulatedNetworkLSPManager;
import tid.pce.client.PCCPCEPSession;
import tid.pce.client.management.AutomaticTesterManagementSever;
import tid.pce.client.management.StopManagement;
import tid.pce.client.tester.Activity;
import tid.pce.client.tester.AutomaticClientTask;
import tid.pce.client.tester.DummyActivity;
import tid.pce.client.tester.InformationRequest;
import tid.pce.client.tester.LSPConfirmationDispatcher;
import tid.pce.client.tester.NetworkEmulatorActivity;
import tid.pce.client.tester.VNTMActivity;
import tid.vntm.LigthPathManagement;
import cern.jet.random.Exponential;
import cern.jet.random.engine.MersenneTwister;

/**
 * Clase que emula el comportamiento del Cliente, PCC. 
 * Tenemos varias sesiones PCC-PCEP entre Clientes y PCEs.
 * Consideramos que un cliente es una petici�n con un determinado par origen destino, a esta petici�n le asignamos su sesi�n PCCPCEP.
 * 
 * @author mcs
 *
 */
public class Emulator {
	/**
	 * Parametros de la emulaci�n (como son las peticiones, par origen destino, carga, etc)
	 */
	private InformationRequest testerParams;

	/**
	 * Sesi�nes con los PCE. 
	 */
	private Hashtable<Integer,PCCPCEPSession> PCEsessionList;
	
	/**
	 * Session con el VNTM. Es posible que sea null, si no lanzamos el programa con la condicion de VNTM
	 */
	private PCCPCEPSession VNTMSession;
	
	private LSPConfirmationDispatcher LSPConfDist;

	/**
	 * networkLSPManager: Encargado de gestionar la red.  
	 */
	private NetworkLSPManager networkLSPManager;
	
	/**
	 * Ejecutor de las peticiones de los clientes
	 */
	private ScheduledThreadPoolExecutor requestExecutor;

	//Management
	/**
	 * Management server to connect via telnet
	 */
	AutomaticTesterManagementSever automaticTesterManagementServerTask;

	
	//Statistics	
	/**
	 * Statistics of current test
	 */
	AutomaticTesterStatistics stats;

	
	//Timers
	/**
	 * planificationTimer: timer con el que programamos el release capacity.
	 */
	Timer planificationTimer;
	/**
	 * Timer con el que programamos la escritura en el fichero de log de las estadisticas.
	 */
	Timer printStatisticsTimer; 
	/**
	 * Timer correspondiente al StopManagement, periodicamente comprueba si se ha llegado a la condici�n de parada de la ejecucion
	 */
	Timer stopManagementTimer;
	
	Timer ligthPathManagementTimer;


	//Loggers
	/**
	 * Logger donde guardamos las estadisticas
	 */
	private Logger statsLog;
	/**
	 * Logger del Cliente, guardamos informacion de la ejecuci�n del cliente
	 */
	private Logger log;
	
	//Parametros propios de la emulacion
	/**
	 * Variable que indica si estamos en la primera ejecucion o no. Puede ser que queramos varias ejecuciones
	 * que se indica en el fichero de configuracion del clienta haciendo que varie la carga, el tiempo entre peticiones o el tiempo de conexion 
	 */
	private boolean firstExecution =true;
	/**
	 * Carga actual con la que lanzamos la emulacion. Puede ir variando si queremos lanzar varias emulaciones.
	 */
	private double currentLoad;
	/**
	 * Tiempo medio entre peticiones actual con el que lanzamos la emulaci�n. Puede ir variando si queremos lanzar varias emulaciones.
	 */
	private double currentMeanTimeBetweenRequest=-1;
	/**
	 * Tiempo de conexion actual con el que lanzamos la emulacion.  Puede ir variando si queremos lanzar varias emulaciones.
	 */
	private double currentMeanConnectionTime=-1;
	/**
	 * Numero de la ejecucion actual. Este parametro sirve solo si queremos programar varias ejecuciones.
	 */
	private int numberExecution;
	
	//para los ficheros de lectura y escritura
	 FileWriter fichero_out = null;
	 
	 File fichero = null;
	 FileReader fr_fichero = null;
	 BufferedReader br = null;
	    
     private float [] cadenaBW;
     
     //private int [] cadenaSemilla_Tiempos;
    
              
     public Emulator (InformationRequest informationRequest,Hashtable<Integer,PCCPCEPSession> PCEsessionList,
			NetworkLSPManager networkLSPManager,PCCPCEPSession VNTMSession, AutomaticTesterStatistics stats){
		this.testerParams=informationRequest;
		this.PCEsessionList = PCEsessionList;
		statsLog=Logger.getLogger("stats");
		numberExecution=0;
		log = Logger.getLogger("PCCClient");
		this.networkLSPManager=networkLSPManager;
		this.VNTMSession=VNTMSession;
		automaticTesterManagementServerTask =  new AutomaticTesterManagementSever(this);
		automaticTesterManagementServerTask.start();
		this.stats=stats;
	}
	public Emulator (InformationRequest informationRequest,Hashtable<Integer,PCCPCEPSession> PCEsessionList,
			NetworkLSPManager networkLSPManager,PCCPCEPSession VNTMSession, AutomaticTesterStatistics stats, LSPConfirmationDispatcher LSPConfDist){
		this.testerParams=informationRequest;
		this.PCEsessionList = PCEsessionList;
		statsLog=Logger.getLogger("stats");
		numberExecution=0;
		log = Logger.getLogger("PCCClient");
		this.networkLSPManager=networkLSPManager;
		this.VNTMSession=VNTMSession;
		automaticTesterManagementServerTask =  new AutomaticTesterManagementSever(this);
		automaticTesterManagementServerTask.start();
		this.stats=stats;
		this.LSPConfDist=LSPConfDist;
		
		cadenaBW = new float [testerParams.getMaxNumberIterations() + 100];
		
		String linea;
			
		int id = testerParams.getNumfileBW();
		
		try {
			
			// Apertura del fichero y creacion de BufferedReader para poder
			// hacer una lectura comoda (disponer del metodo readLine()).
			fichero = new File ("secuenciaBW_semilla"+id+".txt");
						
			fr_fichero = new FileReader (fichero);
			br = new BufferedReader(fr_fichero);
									
						
			/*while (br.read() != -1){
				cadenaBW[i] = br.read();
		    	i++;
				
			}*/
			int i = 0;
			if ((linea=br.readLine())!=null){
			    //Leo palabra por palabra y creo un array de string por cada palabra
			    StringTokenizer st = new StringTokenizer(linea);
			        while (st.hasMoreTokens()) {
			        	cadenaBW[i] =  Float.parseFloat(st.nextToken());
			            i++; 
			        }
			   }

		}catch(Exception e){
			e.printStackTrace();
		}finally{
			
		    // En el finally cerramos el fichero, para asegurarnos
		    // que se cierra tanto si todo va bien como si salta 
		    // una excepcion.
	
			try {
				fr_fichero.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	/**
	 * Cambia el parametro de carga actual (currentLoad) de la emulacion.
	 * @return true si el parametro ha sido cambiado. False si no ha podido ser cambiado 
	 * (porque hemos llegado a la carga maxima que habiamos programado en el fichero de configuracion, AutomaticTester.xml) 
	 */
	
	public boolean changeLoad(){
		boolean paramChanged=false;
		if (this.firstExecution){
			log.info("Primera Ejecucion");
			currentLoad=testerParams.getLoadIni();
			//testerParams.setLoad(currentLoad);
			paramChanged=true;
			this.firstExecution=false;
		}else {
			log.info("It is not the first Execution");				
			if (testerParams.getLoadIni()!=testerParams.getLoadMax()){
				if (testerParams.getLoadStep()>0){
					if (testerParams.calculateLoad()<=testerParams.getLoadMax()){
						this.currentLoad=testerParams.calculateLoad()+testerParams.getLoadStep();
						//testerParams.setLoad(this.currentLoad);
						paramChanged=true;
					}
				}
			}
		}
		return paramChanged;
	}
	
	/**
	 * Cambia el parametro de tiempo de conexion (currentMeanConnectionTime) de la emulacion.
	 * @return true si el parametro ha sido cambiado. False si no ha podido ser cambiado 
	 * (porque hemos llegado al tiempo de conexion maximo que habiamos programado en el fichero de configuracion, AutomaticTester.xml) 
	 */	
	public boolean changeConnectionTime(){
		boolean paramChanged=false;
		if (this.firstExecution){
			log.info("Primera Ejecucion");
			currentMeanConnectionTime=testerParams.getConectionTimeIni();
			testerParams.setMeanConectionTime(currentMeanConnectionTime);
			if (currentMeanTimeBetweenRequest == -1){
				currentMeanTimeBetweenRequest=testerParams.getMeanTimeBetweenRequest();
			}
			paramChanged=true;
			currentLoad=testerParams.calculateLoad();
			//testerParams.setLoad(currentLoad);
			this.firstExecution=false;
		}else {
			log.info("It is not the first Execution");					
			if (testerParams.getConectionTimeIni()!=testerParams.getConectionTimeMax()){
				if (testerParams.getConectionTimeStep()>0){
					currentMeanConnectionTime=testerParams.getMeanConectionTime()+testerParams.getConectionTimeStep();
					if (currentMeanConnectionTime<=testerParams.getConectionTimeMax()){						
						testerParams.setMeanConectionTime(currentMeanConnectionTime);
						currentLoad=testerParams.calculateLoad();						
						//testerParams.setLoad(this.currentLoad);
						paramChanged=true;
					}
				}
			}	
		}		
		return paramChanged;
	}

	/**
	 * Cambia el parametro de tiempo entre peticiones (currentMeanTimeBetweenRequest) de la emulacion.
	 * @return true si el parametro ha sido cambiado. False si no ha podido ser cambiado 
	 * (porque hemos llegado al tiempo entre peticiones maximo que habiamos programado en el fichero de configuracion, AutomaticTester.xml) 
	 */	
	public boolean changeTimeBetweenRequest(){
		boolean paramChanged=false;		
		if (this.firstExecution){
			log.info("Primera Ejecucion");
			currentMeanTimeBetweenRequest=testerParams.getTimeBetweenRequestIni();
			testerParams.setMeanTimeBetweenRequest(currentMeanTimeBetweenRequest);
			if (currentMeanConnectionTime == -1){
				currentMeanConnectionTime=testerParams.getMeanConectionTime();
			}
			paramChanged=true;
			currentLoad=testerParams.calculateLoad();
//			testerParams.setLoad(currentLoad);
			this.firstExecution=false;
		}else {
			log.info("It is not the first Execution");				
			if (testerParams.getTimeBetweenRequestIni()!=testerParams.getTimeBetweenRequestMax()){
				if (testerParams.getTimeBetweenRequestStep()>0){	
					currentMeanTimeBetweenRequest=testerParams.getMeanTimeBetweenRequest()+testerParams.getTimeBetweenRequestStep();
					if (currentMeanTimeBetweenRequest<=testerParams.getTimeBetweenRequestMax()){						
						testerParams.setMeanTimeBetweenRequest(currentMeanTimeBetweenRequest);
						currentLoad=testerParams.calculateLoad();	
						paramChanged=true;
					}
				}
			}	
		}		
		return paramChanged;
	}
	
	
	public boolean getLoad(){
		//SACAR EL NUEVO PARAM DE LA SIMULACION			
		log.info("Emulation Parameters");				
		if (testerParams.isLoadChange()){
			return changeLoad();
		}
		else if (testerParams.isConectionTimeChange()){
			return changeConnectionTime();					
		}else if ((testerParams.isTimeBetweenRequestChange())){
			return changeTimeBetweenRequest();
		}
		else if(testerParams.getMeanTimeBetweenRequestList().size() != 0){
			this.firstExecution=false;
			this.currentMeanTimeBetweenRequest=testerParams.getMeanTimeBetweenRequestList().get(numberExecution);
			this.currentMeanConnectionTime=testerParams.getMeanConectionTimeList().get(numberExecution);
			testerParams.setMeanTimeBetweenRequest(currentMeanTimeBetweenRequest);
			testerParams.setMeanConectionTime(currentMeanConnectionTime);
			this.currentLoad= testerParams.calculateLoad();
			return true;
		}
		else{
			this.firstExecution=false;
			this.currentMeanTimeBetweenRequest=testerParams.getMeanTimeBetweenRequest();
			this.currentMeanConnectionTime=testerParams.getMeanConectionTime();
			this.currentLoad= testerParams.calculateLoad();
			return true;
		}
	}

	/**
	 * Funcion que empieza una nueva emulacion por parte del cliente. Se actualizan los parametros: de carga, las peticiones, etc, para esta
	 * nueva emulacion y se lanzan las tareas que mandan peticiones.
	 */
	public void start(){
		//Timers
		try{
			planificationTimer = new Timer();
			printStatisticsTimer = new Timer(); 
			stopManagementTimer = new Timer();
			ligthPathManagementTimer =new Timer();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		log.info("Start the emulator!");
		if (PCEsessionList.isEmpty()){
			log.info("PCE Session List is Empty");
		}
		if (moreExecutionsLeft()){
			if (!getLoad()){
				log.severe("Problem trying to get the current Load");
				return;
			}

			//Creo las nuevas estadisticas
			//stats = new AutomaticTesterStatistics(currentLoad);
			if (testerParams.isNetworkEmulator()){
				if (networkLSPManager.getEmulatorType() == NetworkLSPManagerTypes.COMPLETED_EMULATED_NETWORK)
					((CompletedEmulatedNetworkLSPManager)networkLSPManager).setStats(stats);
			}
			stats.setNumberNodes(testerParams.getNumberNodes());
			FileHandler fh=null;
			try {
				if (testerParams.getRequestToSendList().get(0).getRequestParameters().isReservation())
					fh=new FileHandler("stats_ConReserva_load_"+currentLoad*(testerParams.getNumberNodes())*(testerParams.getNumberNodes()-1)+"_TimeReserved"+(testerParams.getRequestToSendList().get(0).getRequestParameters().getTimeReserved())+".log");
				else 
					fh=new FileHandler("stats_SinReserva_load_"+testerParams.getNumfileBW()+"_"+currentLoad*(testerParams.getNumberNodes())*(testerParams.getNumberNodes()-1)+".log");
			} catch (SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			int numberRequest = testerParams.getRequestToSendList().size();
			
						
			//Para el logger de las estadisticas
			fh.setFormatter(new SimpleFormatter());			
			PrintStatistics printStatistics = new PrintStatistics(stats);
			statsLog.setLevel(Level.ALL);
			statsLog.addHandler(fh);
			statsLog.info("-------------------------------------------------------------------------");
			statsLog.info("LOAD: "+currentLoad);
			statsLog.info("MeanTimeBetweenRequest:"+currentMeanTimeBetweenRequest+"\t MeanConnectionTime"+currentMeanConnectionTime);
			statsLog.info("-------------------------------------------------------------------------");
			statsLog.info("numberRequest: "+numberRequest);
			printStatisticsTimer.schedule(printStatistics,0,testerParams.getPrintStatisticsTime());
			
//			//Prueba
//			FileHandler fhPrueba=null;
//			try {
//				fhPrueba=new FileHandler("Prueba_load_"+currentLoad+".log");
//				logPrueba.addHandler(fhPrueba);
//				logPrueba.setLevel(Level.ALL);
//				logPrueba.info("Hola!!!");
//			} catch (SecurityException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//			
//			//EndPrueba
			if (testerParams.isControlPlaneOption()) {
				log.info("Create Task From Source - Control plane option");
				createTaskFromSource(numberRequest);

			}
			else {
				
				createTasks(numberRequest);
			}
			
			StopManagement stopManagementTask =  new StopManagement(this,testerParams.getMaxNumberIterations(),testerParams.getStopCondition(),stats);
			stopManagementTimer.schedule(stopManagementTask, 0,10000);
			
		}
		else
		{
			log.info("There are no more executions");
		}
		return;
	}
	public void createTaskFromSource(int numberRequest){

		int semilla = (int) testerParams.getSemillaTiempos();
		//MersenneTwister
		MersenneTwister mersenneTwisterSendRequest = new MersenneTwister(semilla);
		MersenneTwister mersenneTwisterConnectionTime = new MersenneTwister(semilla);
		MersenneTwister mersenneTwisterFirstTBR = new MersenneTwister(semilla);
		//Exponential
		//Usada para programar cada cuanto de envia la misma peticion
		Exponential expSendRequest=null;
		//Usada para el tiempo de conexion, cu�nto estaran las peticiones activas
		Exponential expConnectionTime = null;
		//Usada para programar en que momento se enviara cada peticion que hemos programado
		Exponential exponentialTime=null;
		long expTime=0;
		//Creo mi testeador
		
		if (testerParams.getIsExponential()){
			double lambdaExpTime = 1/(testerParams.getMeanTimeBetweenRequest());
			double lambdaSendRequest = 1/(testerParams.getMeanTimeBetweenRequest());
			double lambdaConnectionTime = 1/(testerParams.getMeanConectionTime());
			expSendRequest = new Exponential(lambdaSendRequest, mersenneTwisterSendRequest);
			expConnectionTime = new Exponential(lambdaConnectionTime, mersenneTwisterConnectionTime);
			exponentialTime= new Exponential(lambdaExpTime, mersenneTwisterFirstTBR);
		}
		if (LSPConfDist != null){
			LSPConfDist.setPlanificationTimer(planificationTimer);
			LSPConfDist.setConnectionTime(expConnectionTime);
			LigthPathManagement ligthPathManagement = null;		
		}	
		requestExecutor = new ScheduledThreadPoolExecutor(3*numberRequest);		
		for (int i=0;i< numberRequest; i++){   
			Activity activity = null;   
			if ((testerParams.isNetworkEmulator())&&(testerParams.isVNTMSession())){ 
				activity = new VNTMActivity(expConnectionTime, planificationTimer, stats);         
				activity.addVNTMSession(VNTMSession);
				activity.addNetworkEmulator(networkLSPManager);
				/*ligthPathManagement = new LigthPathManagement();
				ligthPathManagementTimer.schedule(ligthPathManagement, 0, 100);*/
				//activity.addLigthPathManagement(ligthPathManagement);
			}
			else if (testerParams.isNetworkEmulator()){
				activity = new NetworkEmulatorActivity(expConnectionTime,planificationTimer); 
				activity.addNetworkEmulator(networkLSPManager);
				activity.addStatistics(stats);
			}
			else if(testerParams.isVNTMSession()){
				activity = new VNTMActivity(expConnectionTime, planificationTimer, stats);
				//activity.addStatistics(stats);
				activity.addVNTMSession(VNTMSession);
			}
			else{//Crear otra actividad
				//Actividad para el caso con Plano de Control
				activity = new DummyActivity(expConnectionTime,planificationTimer, PCEsessionList, testerParams.isStaticConnections());		
				activity.addStatistics(stats);
			}

			int numberPCESession=findPCEPSessionToSendRequest(testerParams.getRequestToSendList().get(i).getSource());
			if (numberPCESession != -1){
				double expTimeD=exponentialTime.nextDouble(); 
				expTime =(long)expTimeD;
				AutomaticClientTask automaticClientTask = new AutomaticClientTask(expSendRequest, requestExecutor, PCEsessionList,testerParams,stats,i,numberPCESession/*,System.nanoTime(), expTime*/, cadenaBW);
				automaticClientTask.setThingsToDo(activity);
				if (expSendRequest!=null){
					//				try {
					//					Thread.sleep(100);
					//				} catch (InterruptedException e) {
					//					// TODO Auto-generated catch block
					//					e.printStackTrace();
					//				}

					//				logPrueba.info("Empieza en "+expTime);
					requestExecutor.schedule(automaticClientTask, expTime,TimeUnit.MILLISECONDS);
				}
				else{
					requestExecutor.schedule(automaticClientTask,Math.round (testerParams.getMeanTimeBetweenRequest()),TimeUnit.MILLISECONDS);
				}
			}
		}
		return;
	}
	public int findPCEPSessionToSendRequest(Inet4Address source){		
		int size = PCEsessionList.size();		
		for (int i=0;i<size;i++){
			log.info("Source:"+source.toString()+" - PCE address " +PCEsessionList.get(i).getPeerPCE_IPaddress());
			Inet4Address sourceSocket;
			try {
				sourceSocket = ((Inet4Address)Inet4Address.getByName(PCEsessionList.get(i).getPeerPCE_IPaddress()));
				if (source.equals(sourceSocket))
					return i;
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
		}
		log.info("return -1");
		return -1;
	
	}
	/**
	 * Funcion encargada de crear las tareas encargadas de mandar las peticiones de caminos al PCE
	 * @param numberRequest
	 */
	public void createTasks(int numberRequest){
		//Random rnd = new Random();
		//rnd.setSeed(testerParams.getSemillaTiempos());
		int semilla = (int) testerParams.getSemillaTiempos();
		//int semilla = cadenaSemilla_Tiempos[contador_2];
			//(int)rnd.nextInt(5000);
		
			
		//MersenneTwister
		MersenneTwister mersenneTwisterSendRequest = new MersenneTwister(semilla);
		MersenneTwister mersenneTwisterConnectionTime = new MersenneTwister(semilla);
		MersenneTwister mersenneTwisterFirstTBR = new MersenneTwister(semilla);
		//Exponential
		//Usada para programar cada cuanto de envia la misma peticion
		Exponential expSendRequest=null;
		//Usada para el tiempo de conexion, cu�nto estaran las peticiones activas
		Exponential expConnectionTime = null;
		//Usada para programar en que momento se enviara cada peticion que hemos programado
		Exponential exponentialTime=null;
		long expTime=0;
		//Creo mi testeador
		
		if (testerParams.getIsExponential()){
			double lambdaExpTime = 1/(testerParams.getMeanTimeBetweenRequest());
			double lambdaSendRequest = 1/(testerParams.getMeanTimeBetweenRequest());
			double lambdaConnectionTime = 1/(testerParams.getMeanConectionTime());
			expSendRequest = new Exponential(lambdaSendRequest, mersenneTwisterSendRequest);
			expConnectionTime = new Exponential(lambdaConnectionTime, mersenneTwisterConnectionTime);
			exponentialTime= new Exponential(lambdaExpTime, mersenneTwisterFirstTBR);
		}
		if (LSPConfDist != null){
			LSPConfDist.setPlanificationTimer(planificationTimer);
			LSPConfDist.setConnectionTime(expConnectionTime);
			LigthPathManagement ligthPathManagement = null;		
		}
		requestExecutor = new ScheduledThreadPoolExecutor(3*numberRequest);
		int currentNumberPCESession=0;
		int PCEsessionListSize = PCEsessionList.size();
		for (int i=0;i< numberRequest; i++){   
			Activity activity = null;   
			if ((testerParams.isNetworkEmulator())&&(testerParams.isVNTMSession())){ 
				activity = new VNTMActivity(expConnectionTime, planificationTimer, stats);         
				activity.addVNTMSession(VNTMSession);
				activity.addNetworkEmulator(networkLSPManager);
				/*ligthPathManagement = new LigthPathManagement();
				ligthPathManagementTimer.schedule(ligthPathManagement, 0, 100);*/
				//activity.addLigthPathManagement(ligthPathManagement);
			}
			else if (testerParams.isNetworkEmulator()){
				activity = new NetworkEmulatorActivity(expConnectionTime,planificationTimer); 
			    activity.addNetworkEmulator(networkLSPManager);
			    activity.addStatistics(stats);
			}
			else if(testerParams.isVNTMSession()){
			    activity = new VNTMActivity(expConnectionTime, planificationTimer, stats);
			    //activity.addStatistics(stats);
			    activity.addVNTMSession(VNTMSession);
			}
			else{//Crear otra actividad
				
			}
			currentNumberPCESession =i%PCEsessionListSize;
			double expTimeD=exponentialTime.nextDouble(); 
			expTime =(long)expTimeD;
			AutomaticClientTask automaticClientTask = new AutomaticClientTask(expSendRequest, requestExecutor, PCEsessionList,testerParams,stats,i,currentNumberPCESession/*,System.nanoTime(), expTime*/, cadenaBW);
			automaticClientTask.setThingsToDo(activity);
			if (expSendRequest!=null){
//				try {
//					Thread.sleep(100);
//				} catch (InterruptedException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}

//				logPrueba.info("Empieza en "+expTime);
				requestExecutor.schedule(automaticClientTask, expTime,TimeUnit.MILLISECONDS);
			}
			else{
				requestExecutor.schedule(automaticClientTask,Math.round (testerParams.getMeanTimeBetweenRequest()),TimeUnit.MILLISECONDS);
			}
		}
		return;
	}
	/**
	 * Funcion encargada de parar la emulacion. Lanzara otra si es que hay varias programadas y aun no 
	 * se ha llegado a la ultima
	 */
	public void stop(){
		log.info("Stopping emulation");
		stopManagementTimer.cancel();
		requestExecutor.shutdown();
		printStatisticsTimer.cancel();	
		statsLog.info("Emulation Ended, waiting 120 seconds.");
		try {
			Thread.sleep(120000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		statsLog.info("---------------------------------------------");
		statsLog.info("Emulation ENDS. Final Stats:");
		statsLog.info("---------------------------------------------");
		statsLog.info(stats.print());
		statsLog.info(stats.printEnd());
		
//		LogManager.getLogManager().reset();	
		if (moreExecutionsLeft()){	
			numberExecution++;
			statsLog.removeHandler(statsLog.getHandlers()[0]);
			this.start();
		}
		else{
			LogManager.getLogManager().reset();					
			//planificationTimer.cancel();
			System.exit(0);
		}
	}

	/**
	 * Check if there are more simulations left.
	 * @return true if there are more simulations left and false if there are not 			
	 */
	public boolean moreExecutionsLeft(){
		boolean hasMoreExectutions=false;
		if (this.firstExecution)
			return true;		
		else
		{
			if (testerParams.isLoadChange()){
				if (testerParams.getLoadIni()!=testerParams.getLoadMax()){
					if ((testerParams.calculateLoad()+testerParams.getLoadStep())<=testerParams.getLoadMax())
						hasMoreExectutions=true;	
				}
			}
			else if (testerParams.isConectionTimeChange()){
				if (testerParams.getConectionTimeIni()!=testerParams.getConectionTimeMax()){
					if ((currentMeanConnectionTime+testerParams.getConectionTimeStep()) <= testerParams.getConectionTimeMax())
						hasMoreExectutions=true;
				}					

			}else if ((testerParams.isTimeBetweenRequestChange())){
				if (testerParams.getTimeBetweenRequestIni() != testerParams.getTimeBetweenRequestMax()){
					if ((currentMeanTimeBetweenRequest+testerParams.getTimeBetweenRequestStep())  <= testerParams.getTimeBetweenRequestMax())
						hasMoreExectutions=true;
				}
			}
			else if ((testerParams.getMeanTimeBetweenRequestList().size() != 0)){
				if (testerParams.getMeanTimeBetweenRequestList().size() > numberExecution)
					hasMoreExectutions=true;
			}
		}
		return hasMoreExectutions;
	}

	public AutomaticTesterStatistics getStats() {
		return stats;
	}
	public InformationRequest getTesterParams() {
		return testerParams;
	}
	//	 PccReqId createPccReqId(){
	//		PccReqId p_r_i = new PccReqId();
	//		//Add PCC Ip Address
	//		if (PCEsession != null){
	//		if (PCEsession.getSocket()!=null)
	//			p_r_i.setPCCIpAddress((Inet4Address)PCEsession.getSocket().getInetAddress());
	//		else
	//			System.out.println("El Socket es null!!");
	//		}
	//		else
	//			System.out.println("ps es null!!");
	//		return p_r_i;
	//		
	//	}
	
//MONITORING ----------------------------------------------------------------------------------------------	
}

