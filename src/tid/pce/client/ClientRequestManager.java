package tid.pce.client;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Random;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import es.tid.pce.pcep.PCEPProtocolViolationException;
import es.tid.pce.pcep.messages.PCEPInitiate;
import es.tid.pce.pcep.messages.PCEPMessage;
import es.tid.pce.pcep.messages.PCEPMonReq;
import es.tid.pce.pcep.messages.PCEPReport;
import es.tid.pce.pcep.messages.PCEPRequest;
import es.tid.pce.pcep.messages.PCEPResponse;
import tid.util.UtilsFunctions;

/**
 * 
 *
 */


public class ClientRequestManager {
	//private PCCPCEPSession session;
	public Hashtable<Long,Object> locks;
	private Hashtable<Long,PCEPResponse> responses;
	private Hashtable<Long,PCEPReport> responsesInit;
	private Logger log;
	private DataOutputStream out=null; //Use this to send messages to PCC
	private long lastTime;
	
	public Hashtable<Long,Semaphore> semaphores;
	
	public ClientRequestManager(){
		log=Logger.getLogger("PCCClient");
		//this.out=out;
		locks = new Hashtable<Long, Object>();
		responses=new Hashtable<Long, PCEPResponse>();
		responsesInit=new Hashtable<Long, PCEPReport>();
	
		semaphores=new Hashtable<Long,Semaphore>();
	}
	public void notifyResponse(PCEPResponse pcres, long timeIni){
		lastTime=timeIni;
		long idRequest=pcres.getResponse(0).getRequestParameters().getRequestID();
		log.finer("Entrando en Notify Response");
		Object object_lock=locks.remove(new Long(idRequest));
		if (object_lock!=null){
			responses.put(new Long(idRequest), pcres);
			object_lock.notifyAll();	
		}
	}
	
	public void notifyResponseInit(PCEPReport pcres, long timeIni){
		System.out.println("Entrando en Notify Resp Init");
		log.info("Entrando en Notify Resp Init");
		lastTime=timeIni;
		
		
		long idIni=pcres.getStateReportList().get(0).getSRP().getSRP_ID_number();
		System.out.println("Nos llega un" +idIni);
		responsesInit.put(new Long(idIni), pcres);
		
	}

	public void setDataOutputStream(DataOutputStream out)
	{
		this.out=out;
	}
	
	public PCEPMessage newRequest( PCEPMessage pcreq)
	{
		return newRequest(pcreq,60000);
	}
	
	
	//Generic function in case instead of PCEPRequest/PCEPResponse it's PCEPInitate/PCEPReport
	public PCEPResponse newRequest( PCEPRequest pcreq)
	{
		return newRequest(pcreq,60000);
	}
	
	
	
	//Generic function in case instead of PCEPRequest/PCEPResponse it's PCEPInitate/PCEPReport
	public PCEPMessage newRequest( PCEPMessage pcreq, long maxTimeMs)
	{
		log.info("New Request. Request:"+pcreq.toString());
		Object object_lock = new Object();		
		long idRequest = generateRandomID();

		Long idReqLong = new Long(idRequest);
		long timeIni = System.nanoTime();
		locks.put(idReqLong, object_lock);
		sendPCEPMessage(pcreq);
		
		synchronized (object_lock) 
		{ 
			try 
			{				
				log.info("ESPERAREMOS "+maxTimeMs);
				object_lock.wait(maxTimeMs);
			} 
			catch (InterruptedException e)
			{
				UtilsFunctions.exceptionToString(e);
			}
		}
		
		long timeIni2=System.nanoTime();
		double reqTime_ms=(timeIni2-timeIni)/1000000;
		log.fine("Request or timeout");
		
		PCEPMessage resp = responses.remove(new Long(idRequest));
		if (resp==null)
		{
			log.warning("NO RESPONSE!!!!! me deshago del lock... con idReqLong "+idRequest);
			locks.remove(idReqLong);
		}
		return resp;
	}
	
	public PCEPResponse newRequest( PCEPRequest pcreq, long maxTimeMs)
	{
		log.info("New Request. Request:"+pcreq.toString());
		log.info("pcreq.getRequest(0).getXro(): "+pcreq.getRequest(0).getXro());
		Object object_lock=new Object();		
		long idRequest=pcreq.getRequest(0).getRequestParameters().getRequestID();

		Long idReqLong=new Long(idRequest);
		long timeIni=System.nanoTime();
		locks.put(idReqLong, object_lock);
		sendRequest(pcreq);
		synchronized (object_lock) 
		{ 
			try 
			{				
				log.info("ESPERAREMOS "+maxTimeMs);
				object_lock.wait(maxTimeMs);
			} 
			catch (InterruptedException e)
			{
				UtilsFunctions.exceptionToString(e);
			}
		}
		long timeIni2=System.nanoTime();
		//log.info("Response "+pr.toString());
		double reqTime_ms=(timeIni2-timeIni)/1000000;
		log.fine("Request or timeout");
		
		PCEPResponse resp=responses.remove(new Long(idRequest));
		if (resp==null){
			log.warning("NO RESPONSE!!!!! me deshago del lock... con idReqLong "+idRequest);
			locks.remove(idReqLong);
		}
		return resp;
	}
	
	public void sendRequest(PCEPRequest req)
	{
		log.fine("Sending Request: :" + req);
		log.info("Sending PCEP Request");
		sendPCEPMessage(req);
	}
	
	synchronized public  void sendPCEPMessage(PCEPMessage msg){
		try {
			msg.encode();
		} catch (PCEPProtocolViolationException e1) {
			// TODO Auto-generated catch block
			log.info(UtilsFunctions.exceptionToString(e1));
		}
		try {
			log.info("Sending message ::"+msg);
			out.write(msg.getBytes());
			out.flush();
			log.info("Sending message finish");
		} catch (IOException e) {
			log.info(UtilsFunctions.exceptionToString(e));
			log.warning("Error sending msg: " + e.getMessage());

		}
	}
	
	public PCEPResponse newRequest(PCEPMonReq pcreq){
		log.fine("New Request");
		Object object_lock=new Object();
		//RequestLock rl=new RequestLock();
		//((RequestParameters)(((Request)pcreq.getRequest(0)).getReqObject(0))).getRequestID();
		//long idRequest=((RequestParameters)(((Request)pcreq.getRequest(0)).getReqObject(0))).getRequestID();
		long idMonitoring=pcreq.getMonitoring().getMonitoringIdNumber();

		locks.put(new Long(idMonitoring), object_lock);
		sendRequest(pcreq);
		synchronized (object_lock) { 
			try {
				log.fine("Request sent, waiting for response");
				object_lock.wait(60000);
			} catch (InterruptedException e){
			//	FIXME: Ver que hacer
			}
		}
		log.fine("Request or timeout");
		
		PCEPResponse resp=responses.get(new Long(idMonitoring));
		if (resp==null){
			log.warning("NO RESPONSE!!!!!");
		}
		return resp;
	}

	synchronized public  void sendRequest(PCEPMonReq req){
		try {
			req.encode();
		} catch (PCEPProtocolViolationException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try {
			log.fine("Sending Request message");
			out.write(req.getBytes());
			out.flush();
		} catch (IOException e) {
			log.warning("Error sending REQ: " + e.getMessage());
		}
	}
	
	private int generateRandomID()
	{
		Random generator = new Random(); 
		return generator.nextInt(Integer.MAX_VALUE);
	}
	
	public PCEPMessage initiate ( PCEPInitiate pcini, long maxTimeMs)
	{
		log.info("Sending Initiate:"+pcini.toString());
		
		
		
		byte[] LSPname=pcini.getPcepIntiatedLSPList().get(0).getLsp().getSymbolicPathNameTLV_tlv().getSymbolicPathNameID();
		long idIni=pcini.getPcepIntiatedLSPList().get(0).getRsp().getSRP_ID_number();
		System.out.println("Sending with id " +idIni);
		Long idReqLong=new Long(idIni);
		long timeIni=System.nanoTime();
		//System.out.println("id ini es "+)
		Semaphore semapohore=new Semaphore(0);
		semaphores.put(idIni, semapohore);
		sendPCEPMessage(pcini);
			
		try 
			{				
			semapohore.tryAcquire(maxTimeMs,TimeUnit.MILLISECONDS);
				
			} 
			catch (InterruptedException e)
			{
				UtilsFunctions.exceptionToString(e);
			}
		long timeIni2=System.nanoTime();
		//log.info("Response "+pr.toString());
		double reqTime_ms=(timeIni2-timeIni)/1000000;
		log.fine("Time: "+reqTime_ms );
		
		PCEPMessage resp=responsesInit.remove(new Long(idIni));
		if (resp==null){
			log.warning("NO RESPONSE!!!!! me deshago del lock... con idIni "+idIni);
			locks.remove(idReqLong);
		}
		return resp;
	}
}
