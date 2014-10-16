package tid.pce.client.tester;

import java.util.LinkedList;
import java.util.Timer;
import java.util.logging.Logger;

import tid.netManager.NetworkLSPManager;
import tid.pce.client.PCCPCEPSession;
import tid.pce.client.emulator.AutomaticTesterStatistics;
import tid.vntm.LigthPathManagement;
import cern.jet.random.Exponential;
import es.tid.pce.pcep.constructs.Path;
import es.tid.pce.pcep.messages.PCEPRequest;
import es.tid.pce.pcep.messages.PCEPResponse;
import es.tid.pce.pcep.objects.BandwidthRequestedGeneralizedBandwidth;
import es.tid.pce.pcep.objects.ExplicitRouteObject;
import es.tid.rsvp.objects.subobjects.EROSubobject;

public class NetworkEmulatorActivity implements Activity{
	private PCCPCEPSession VNTMSession;
	private NetworkLSPManager networkLSPManager;
	private AutomaticTesterStatistics stats;
	private PCEPRequest request;
	private PCEPResponse response;
	private Logger log;
	private Exponential connectionTime;	
	private Timer planificationTimer;	
//	private Logger logPrueba;
//	private Logger logErrores;
	
	public NetworkEmulatorActivity(Exponential connectionTime,Timer planificationTimer){
		this.connectionTime=connectionTime;
		this.planificationTimer=planificationTimer;
		log=Logger.getLogger("PCCClient");
//		logErrores=Logger.getLogger("PruebaLambdas");
//		logPrueba=Logger.getLogger("mmmerrores");
	}

	public void addNetworkEmulator(NetworkLSPManager networkLSPManager){
		this.networkLSPManager=networkLSPManager;
	}
	
	public void addStatistics(AutomaticTesterStatistics stats){
		this.stats = stats; 
	}
	
	public void addRequest(PCEPRequest request){
		this.request = request;		
//		logPrueba.info("ADDING REquest: "+this.request.getRequestList().getFirst().getRequestParameters().getRequestID()+"--->eroSubObjList.toString():"+request.getRequestList().get(0).toString());
	}
	
	public void addResponse(PCEPResponse response){	
		log.info("Añadimos la response para actualizar estadisticas");
		this.response=response;
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		if (response == null){
			stats.addNoResponse();
			log.severe("Response null");			
			return;
		}
		if (response.getResponseList().isEmpty()){
			log.severe("ERROR in response");
			//FIXME: QUE HACEMOS? CANCELAMOS SIMULACION?
			//stats.addNoPathResponse();
			System.exit(1);
			
		}else {
			try{
				if (response.getResponseList().get(0).getNoPath()!=null){
					log.info("NO PATH");
					stats.addNoPathResponse();
					stats.analyzeBlockingProbability(1);
					stats.analyzeblockProbabilityWithoutStolenLambda(1);
					return;	
				}else {
					log.info("Response actualizamos estadisticas");
					Path path=response.getResponseList().get(0).getPath(0);
					ExplicitRouteObject ero=path.geteRO();
					LinkedList<EROSubobject> eroSubObjList=ero.getEROSubobjectList();
					long time1 = System.nanoTime();
					if (networkLSPManager.setLSP(eroSubObjList,request.getRequestList().getFirst().getRequestParameters().isBidirect(),(BandwidthRequestedGeneralizedBandwidth)path.getBandwidth())){
						long time2= System.nanoTime();
						double LSPTime = (time2-time1)/1000000;
						stats.analyzeLSPTime(LSPTime);
						stats.addSLResponse();	
						stats.addNumberActiveLSP();
						stats.analyzeBlockingProbability(0);
						stats.analyzeLambdaBlockingProbability(0);
						stats.analyzeblockProbabilityWithoutStolenLambda(0);
	
	//					log.info("connectionTime 2: ");
	//					log.info("connectionTime.nextDouble() "+connectionTime.nextDouble());
						RealiseCapacityTask realiseCapacity = new RealiseCapacityTask(networkLSPManager,eroSubObjList,stats,request.getRequestList().getFirst().getRequestParameters().isBidirect(),(BandwidthRequestedGeneralizedBandwidth)path.getBandwidth());
						long duration =Math.round(connectionTime.nextDouble());
						log.info("LSP duration: "+duration);
						planificationTimer.schedule(realiseCapacity,duration);
					}else
					{
						//Logger logPrueba=Logger.getLogger("LogPrueba");
						stats.addStolenLambdasLSP();
						stats.analyzeLambdaBlockingProbability(1);
						stats.analyzeBlockingProbability(1);
					}			
				}
			}catch(Exception e){
				e.printStackTrace();
				System.exit(1);
			}
		}
	}

	@Override
	public void addVNTMSession(PCCPCEPSession VNTMSession) {
		this.VNTMSession=VNTMSession;
	}

	@Override
	public void addPCEsessionVNTM(PCCPCEPSession vNTMSession) {
		// TODO Auto-generated method stub
	}

	@Override
	public void addLigthPathManagement(LigthPathManagement ligthPathManagement) {
		// TODO Auto-generated method stub
	}
}
