package tid.pce.client.tester;

import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Timer;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

import tid.netManager.NetworkLSPManager;
import tid.pce.client.emulator.AutomaticTesterStatistics;
import tid.pce.tedb.DomainTEDB;
import tid.vntm.LSP;
import tid.vntm.LigthPathCreateIP;
import cern.jet.random.Exponential;
import es.tid.pce.pcep.constructs.Path;
import es.tid.pce.pcep.objects.BandwidthRequested;
import es.tid.rsvp.objects.subobjects.EROSubobject;
import es.tid.rsvp.objects.subobjects.SubObjectValues;

public class LSPConfirmationProcessorThread extends Thread {

	private boolean running;
	private  Hashtable<Integer, LSP> lspListIP;
	private LinkedBlockingQueue<Path> pathList;
	private LigthPathCreateIP LPcreateIP;
	private NetworkLSPManager netLSPManager;
	private AutomaticTesterStatistics stats;
	private Exponential connectionTime;
	private Timer planificationTimer;
	
	//	private ReservationManager reservationManager;
	DomainTEDB ted;
	public DomainTEDB getTed() {
		return ted;
	}
	private Logger log;
	
	public LSPConfirmationProcessorThread(LinkedBlockingQueue<Path>  pathList,DomainTEDB tedb,
			NetworkLSPManager networkLSPManager, AutomaticTesterStatistics stats,
			Exponential connectionTime, Timer planificationTimer){//ReservationManager 
		running=true;
		this.pathList=pathList;
		log=Logger.getLogger("PCEServer");
		this.ted=(DomainTEDB)tedb;
		LPcreateIP = new LigthPathCreateIP(tedb);
		this.netLSPManager=networkLSPManager;
		lspListIP = new Hashtable<Integer, LSP> ();
		this.stats=stats;
		this.connectionTime=connectionTime;
		this.planificationTimer=planificationTimer;
	}
	
	public void run(){	
		Path path = null;
		while (running) {
			try {
					path=pathList.take();
			} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					System.exit(0);
					break;
				}
				
				if (path!=null){
					boolean createIP = LPcreateIP.createLigthPath(path.geteRO().getEROSubobjectList());
					float bw = ((BandwidthRequested)path.getBandwidth()).getBw();
					if (createIP== true){
							// Creamos el ERO IP
						LinkedList<EROSubobject> eroIP = new LinkedList<EROSubobject>();
												
						eroIP.add(path.geteRO().getEROSubobjectList().getFirst());
						eroIP.add(path.geteRO().getEROSubobjectList().getLast());
								
						
								
						long time1 = System.nanoTime();
						// bidirect esta puesto a false
						if (netLSPManager.setLSP_UpperLayer(eroIP, bw, false)) { //campo de bandwidth METERLO !!!
							long time2= System.nanoTime();
							double LSPTime = (time2-time1)/1000000;
								
									
							stats.analyzeLSPTime(LSPTime);
							stats.addMLResponse();	
							stats.addNumberActiveLSP();
							stats.analyzeBlockingProbability(0);
							stats.analyzeLambdaBlockingProbability(0);
							stats.analyzeblockProbabilityWithoutStolenLambda(0);
							stats.addTrafficHops(1);
							
							RealiseCapacityTask realiseCapacity = new RealiseCapacityTask(netLSPManager,eroIP,null,false,null, bw, true);	
								
							long duration = Math.round(connectionTime.nextDouble());
							log.info("LSP duration: "+duration);
									
							planificationTimer.schedule(realiseCapacity,duration);
						}
						else{
							stats.addStolenBWLSP();							
							stats.analyzeBlockingProbability(1);
						}
						//path = null;
									
						// ya hemos establecido el camino LSP en la capa superior confirmar respuesta
						// multi-layer
						// mirar el NetworkEmulatorActivity
						
					}
					
				}
				// voy sacando de aqui los LSPs Confirmation del VNTM
				
			/*}catch (InterruptedException e) {
				// TODO Auto-generated catch block
				System.out.println("********** NO HAY PATHS PARA SACAR ***********");
				e.printStackTrace();
			}*/
		}
	}
	int createEROListIP(LinkedList<EROSubobject> eroSubObjList, LinkedList<EROSubobject> eroList){
		
		int numNewLinks=0;
		int counterArray=0;
//		sourceList = new ArrayList<String>();
//		destinationList = new ArrayList<String>();
		//cREAR LISTA DE EROS
		LinkedList<EROSubobject> eroSubObjList2=null;
		String strPrev=null;
		for (int i=0; i<(eroSubObjList.size()-1); ++i){
			if ((eroSubObjList.get(i)).getType()==SubObjectValues.ERO_SUBOBJECT_IPV4PREFIX){
				//Create a new ERO to add at the list
				if ((eroSubObjList.get(i+1)).getType()==SubObjectValues.ERO_SUBOBJECT_IPV4PREFIX){
					eroList.add(eroSubObjList.get(i));
					numNewLinks++;
				}
			}else 
				continue;			
		}
		return numNewLinks;
	}

	public Exponential getConnectionTime() {
		return connectionTime;
	}

	public void setConnectionTime(Exponential connectionTime) {
		this.connectionTime = connectionTime;
	}

	public Timer getPlanificationTimer() {
		return planificationTimer;
	}

	public void setPlanificationTimer(Timer planificationTimer) {
		this.planificationTimer = planificationTimer;
	}
	
	
}


