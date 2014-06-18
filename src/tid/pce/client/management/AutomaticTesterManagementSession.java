package tid.pce.client.management;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.util.Timer;
import java.util.logging.Level;
import java.util.logging.Logger;

import tid.pce.client.emulator.AutomaticTesterStatistics;
import tid.pce.client.emulator.Emulator;
import tid.pce.client.tester.InformationRequest;


public class AutomaticTesterManagementSession extends Thread {
	private Socket socket;
//	
	private Logger log;
	private Timer printStatisticsTimer;
	private AutomaticTesterStatistics stats;
	private boolean isRunning;
//	
//	private ChildPCERequestManager cprm;
//	
//	private RequestDispatcher requestDispatcher;
//	
	private PrintStream out;
//	
//	private DomainTEDB tedb;
//	
//	private ReachabilityManager rm;
	
	//Timer of sending the Task TesterMLNetwork
	private Timer timer;
	private InformationRequest informationRequest;
	private double load;
//	//Timer of writing in the file AllRequestTimeStatistics.txt
//	private Timer planificationTimer;
//	private PCCPCEPSession PCEsession;
//	private PCCPCEPSession PCEsessionVNTM;

	private Emulator emulator;

	public AutomaticTesterManagementSession(Socket s,Timer timer,  Timer printStatisticsTimer,/* Timer planificationTimer,*/AutomaticTesterStatistics stats,/* PCCPCEPSession PCEsession,PCCPCEPSession PCEsessionVNTM, */InformationRequest info, double load){
		this.socket=s;
		this.timer=timer;
		this.stats=stats;

		this.printStatisticsTimer=printStatisticsTimer;
		this.informationRequest=info;
		log=Logger.getLogger("PCCClient");
		isRunning=true;
		this.load=load;
//		this.planificationTimer=planificationTimer;
//		this.PCEsession=PCEsession;
//		this.PCEsessionVNTM=PCEsessionVNTM;	
		log.info("Creamos el AutomaticTesterManagementSession");
		
	}
	
	public AutomaticTesterManagementSession(Socket s,Emulator emulator){
		log=Logger.getLogger("PCCClient");
		log.info("Creamos el AutomaticTesterManagementSession");
		this.socket=s;
		this.emulator=emulator;		
	}
	
	
	public void run(){
		log.info("Starting AutomaticTester Management session");
		boolean running=true;
		try {
			out=new PrintStream(socket.getOutputStream());

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		try {
			//out.print("-------------\n");
			BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			while (running) {
				out.print("tester:>");
				String command = null;
				try {
					command = br.readLine();
				} catch (IOException ioe) {
					out.print("IO error trying to read your command");
					return;
				}if (command.equals("stats")) {
					//out.println(stats.print());
					out.println(emulator.getStats().print());
				}if (command.equals("load")) {
					//out.println(stats.print());
					//out.println(emulator.getLoad());
				}
				
			/*	if (command.equals("start")){					  
					if (isRunning==true){
						out.println("It's already running. You should write the command 'stop' to stop it.");
					}else{
						isRunning=true;
						if (pw_file == null){
							if (informationRequest.getFileStatistics() == null){							
								pw_file = new FileWriter("/root/tester/Statistics.txt",true);
							}
							else
							{
								pw_file = new FileWriter(informationRequest.getFileStatistics(),true);
							}
							pw = new PrintWriter(pw_file);
						}
						//Reseteo las estadisticas
						 stats= new AutomaticTesterStatistics(informationRequest.getLoad());
						 out.println("Create new statistics");
						 stats.setPrintRequestTime(informationRequest.getPrintRequestTime());
						 
						 PrintStatistics printStatistics = new PrintStatistics(pw,stats);
						 out.println("Create new print statistics");
						 printStatisticsTimer=new Timer();
						 printStatisticsTimer.schedule(printStatistics,0,10000);
						 //Creo una nueva tarea de envio  
						 if (informationRequest.getIsExponential()){
							   double lambdaSendRequest = 1/(informationRequest.getMeanTimeBetweenRequest());
							   double lambdaConnectionTime = 1/(informationRequest.getMeanConectionTime());
							// double lambdaSendRequest = 1/(informationRequest.getMeanTimeBetweenRequestList().get(0));
							// double lambdaConnectionTime = 1/(informationRequest.getMeanConectionTimeList().get(0));
							 MersenneTwister mersenneTwisterSendRequest = new MersenneTwister();
							 MersenneTwister mersenneTwisterConnectionTime = new MersenneTwister();

							 System.out.println("Lambda Send Request "+ lambdaSendRequest);
							 System.out.println("Lambda Connection Time "+ lambdaConnectionTime);
							 Exponential expSendRequest = new Exponential(lambdaSendRequest, mersenneTwisterSendRequest); 

							 Exponential connectionTime = new Exponential(lambdaConnectionTime, mersenneTwisterConnectionTime);
							   double timeNextReqD=expSendRequest.nextDouble(); 
								long timeNextReq =(long)timeNextReqD;
							 if (informationRequest.isRandom() == true){
								 out.println("Create new tester task");
								 MersenneTwister mt = new MersenneTwister();
								 timer=new Timer();
								 AutomaticTesterMLNetworkRandomTask task = new AutomaticTesterMLNetworkRandomTask(expSendRequest,connectionTime, timer,planificationTimer,PCEsession.crm,PCEsessionVNTM,stats,pw, apw,mt);
								
								 timer.schedule(task, timeNextReq);
							 }else{
								   for (int i=0;i<informationRequest.getCounter();i++){
									   out.println(" Creating path between "+informationRequest.getSourceList(i).toString()+" and "+informationRequest.getDestinationList(i).toString());
									   PCEPRequest p_r = createRequestMessage(i);	
									   timer=new Timer();
									   AutomaticExponentialTesterMLNetworkTask task = new AutomaticExponentialTesterMLNetworkTask(expSendRequest,connectionTime, timer,planificationTimer, p_r,PCEsession.crm,PCEsessionVNTM,stats,pw, apw);
									   //period - time in milliseconds between successive task executions
									   
									   timer.schedule(task,timeNextReq);
								   }
						 }
							 }else//El tiempo no es exponencial
							 {
								 for (int i=0;i<informationRequest.getCounter();i++){
									   System.out.println(" Creating path between "+informationRequest.getSourceList(i).toString()+" and "+informationRequest.getDestinationList(i).toString());
									   //				if (PCMonReqBool){
									   //					PCEPMonReq p_m_r= createMonRequestMessage(i);
									   //					AutomaticTesterNetworkTask task = new AutomaticTesterNetworkTask(p_m_r,PCEsession,PCMonReqBool);
									   //					timer=new Timer();
									   //					timer.schedule(task, 0,timeProcessingList.get(i));}
									   //				else{
									   PCEPRequest p_r = createRequestMessage(i);	
									   AutomaticTesterMLNetworkTask task = new AutomaticTesterMLNetworkTask(p_r,PCEsession,PCEsessionVNTM);				
									   //timer.schedule(task, 0,Math.round (informationRequest.getMeanTimeBetweenRequestList().get(i)));
									   timer.schedule(task, 0,Math.round (informationRequest.getMeanTimeBetweenRequest()));
								   }
							 }
					}
					
				}*/
				if (command.equals("quit")) {
					log.info("Ending Management Session");
					out.println("bye!");
					try {
						out.close();						
					} catch (Exception e){
						e.printStackTrace();
					}
					try {
						br.close();						
					} catch (Exception e){
						e.printStackTrace();
					}			
					
				
					return;
				}	
				if (command.equals("change load")){
					if (isRunning==true){
						out.println("It's running. You should write the command 'stop' to stop it before changing the load.");
					}else{					
					try {
						String load_s = br.readLine();
						out.println("timeBetweenRequest: "+load_s);
						load=Double.parseDouble(load_s);
						//load = beta/(N(N-1)u) --> timeBetweenRequest=1/(load*N*(N-1)*u)=meanConnectionTime/load*N*(N-1)
						int N=15;//Nodes number
						//double timeBetweenRequest=informationRequest.getMeanConectionTimeList().get(0)/(load*N*(N-1));
						double timeBetweenRequest=informationRequest.getMeanConectionTime()/(load*N*(N-1));
						informationRequest.setMeanTimeBetweenRequest(timeBetweenRequest);
						out.println("timeBetweenRequest: "+timeBetweenRequest);
					} catch (IOException ioe) {
						out.print("IO error trying to read parameter");
						System.exit(1);
					}
					}
					
				}
				if (command.equals("stop")){
					//Variable a false: esta acabado
					isRunning=false;
					timer.cancel();
			
					
					printStatisticsTimer.cancel();			
					
				}
				else if (command.equals("help")){
					out.print("Available commands:\r\n");
					out.print("stats\r\n");
//					out.print("start\r\n");
//					out.print("change load\r\n");
					out.print("stop\r\n");
					out.print("quit\r\n");
					out.print("set traces on");
					out.print("set traces off");
					
				}
				else if (command.equals("set traces on")) {
					log.setLevel(Level.ALL);		
					Logger log2=Logger.getLogger("PCEPParser");
					log2.setLevel(Level.ALL);
					Logger log3= Logger.getLogger("OSPFParser");
					log3.setLevel(Level.ALL);
					out.print("traces on!\r\n");
				} 
				else if (command.equals("set traces off")) {
					log.setLevel(Level.SEVERE);		
					Logger log2=Logger.getLogger("PCEPParser");
					log2.setLevel(Level.SEVERE);
					Logger log3= Logger.getLogger("OSPFParser");
					log3.setLevel(Level.SEVERE);
					out.print("traces off!\r\n");
				} 	else if (command.equals("quit")){
					out.print("byte\r\n");
					System.exit(0);
					
				}
	
				else{
					//out.print("invalid command\n");	
					out.print("\n");
				}

			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
	}

			
}
