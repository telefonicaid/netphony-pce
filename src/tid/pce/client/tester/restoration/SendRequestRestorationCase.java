package tid.pce.client.tester.restoration;

import java.util.LinkedList;
import java.util.TimerTask;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

import es.tid.pce.pcep.constructs.Path;
import es.tid.pce.pcep.messages.PCEPRequest;
import es.tid.pce.pcep.messages.PCEPResponse;
import es.tid.pce.pcep.objects.BandwidthRequestedGeneralizedBandwidth;
import es.tid.pce.pcep.objects.ExplicitRouteObject;
import es.tid.rsvp.objects.subobjects.EROSubobject;
import tid.netManager.NetworkLSPManager;
import tid.pce.client.PCCPCEPSession;

public class SendRequestRestorationCase  extends TimerTask {
	private PCEPRequest request;
	private PCCPCEPSession PCEsession;
	private long initialTime;
	private int numberRetries=0;
	RestorationCaseStatistics restorationCaseStatistics;	
	NetworkLSPManager networkLSPManager;
	Logger logTiemposVsNumberSaltos;
	Logger logReplies;
	Logger logSendResponse;
	long timeProgramed;
	long exactTimeToSend;
	/**
	 * Variable usada para bloquear la variable newRestorationCaseTable
	 */
	private static ReentrantLock lock = new ReentrantLock();
	/**
	 * Variable usada para guardar los nuevos caminos que ha devuelto el PCE y se han
	 * podido reservar. 
	 */
	LinkedList<RestorationCaseTable> newRestorationCaseTable;
	 ScheduledThreadPoolExecutor requestExecutor;
	public SendRequestRestorationCase(RestorationCaseStatistics restorationCaseStatistics, LinkedList<RestorationCaseTable> newRestorationCaseTable,NetworkLSPManager networkLSPManager, long timeProgramed,long exactTimeToSend, ScheduledThreadPoolExecutor requestExecutor ){
		this.restorationCaseStatistics=restorationCaseStatistics;
		this.newRestorationCaseTable=newRestorationCaseTable;
		logReplies= Logger.getLogger("logReplies");
		logSendResponse= Logger.getLogger("logSendResponse");
		logTiemposVsNumberSaltos =  Logger.getLogger("logTiemposVsNumberSaltos");
		this.networkLSPManager=networkLSPManager;
		this.timeProgramed=timeProgramed;
		this.exactTimeToSend=exactTimeToSend;
		this.requestExecutor= requestExecutor;
	}
	@Override
	public void run() {
		// TODO Auto-generated method stub
		/*Espero la respuesta*/		
		PCEPResponse response = null; 
		boolean retry=true;
		while (retry){
			long timeSendResponse = System.nanoTime();
			//logSendResponse.info("timeSendResponse: "+System.currentTimeMillis()+"ms. ID: "+request.getRequestList().get(0).getRequestParameters().requestID+" Expected time:("+exactTimeToSend+") -  Real Time: ("+((double)timeSendResponse-(double)timeProgramed)/(double)1000000+")");
			if (numberRetries != 0)
				changeIdRequest(request);
			response=PCEsession.crm.newRequest(request);
			
			long finalTime = System.nanoTime();

			if (response == null){//retry
				//ERROR??
				numberRetries++;
				System.out.println("ERROR: RESPONSE NULL");
				System.out.println(1);
			}
			else if (response.getResponseList().isEmpty()){//retry
				//ERROR o REINTENTO
				numberRetries++;
				System.out.println("ERROR: RESPONSE EMPTY");
			}else {
				try{
					if (response.getResponseList().get(0).getNoPath()!=null){//retry
						numberRetries++;
						}
					else {


						Path path=response.getResponseList().get(0).getPath(0);
						ExplicitRouteObject ero=path.geteRO();

						LinkedList<EROSubobject> eroSubObjList=ero.getEROSubobjectList();

						long time_LSP1 = System.nanoTime();					
						if (networkLSPManager.setLSP(eroSubObjList,request.getRequestList().get(0).getRequestParameters().isBidirect(),(BandwidthRequestedGeneralizedBandwidth)path.getBandwidth())){
							long time_LSP2= System.nanoTime();
							double LSPTime = (time_LSP2-time_LSP1)/1000000;
							//Numero de enlaces que tiene el camino, que tenemos que comprobar que estan disponibles.	
							int number_hops =(eroSubObjList.size()-1)/2;		
							logTiemposVsNumberSaltos.info(LSPTime+"\t"+number_hops);
							//practicao teorico error relativo 
							restorationCaseStatistics.addLSPTime(LSPTime);
							restorationCaseStatistics.addRestorationTime(finalTime-initialTime);
							restorationCaseStatistics.addNumberRetries(numberRetries);
							RestorationCaseTable restorationCaseTable = new RestorationCaseTable();
							restorationCaseTable.setResponse(response.getResponseList().get(0));
							lock.lock();
							newRestorationCaseTable.add(restorationCaseTable);
							lock.unlock();
							//logReplies.info("Response: "+ response.getResponseList().get(0).toString()+ " Time to processed: "+ (finalTime-initialTime) );
							retry=false;
						}else{
							numberRetries++;
						}
					}


				}catch(Exception e){
					e.printStackTrace();
					System.exit(1);

				}
			}
		}

		
	}
	
	private void changeIdRequest(PCEPRequest request){		
		request.getRequestList().get(0).getRequestParameters().setRequestID(PCCPCEPSession.getNewReqIDCounter());
	}
	public PCEPRequest getRequest() {
		return request;
	}
	public void setRequest(PCEPRequest request) {
		this.request = request;
	}
	public PCCPCEPSession getPCEsession() {
		return PCEsession;
	}
	public void setPCEsession(PCCPCEPSession pCEsession) {
		PCEsession = pCEsession;
	}
	public long getInitialTime() {
		return initialTime;
	}
	public void setInitialTime(long initialTime) {
		this.initialTime = initialTime;
	}
	public int getNumberRetries() {
		return numberRetries;
	}
	public void setNumberRetries(int numberRetries) {
		this.numberRetries = numberRetries;
	}
	
	
}
