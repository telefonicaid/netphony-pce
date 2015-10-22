package es.tid.pce.server;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import es.tid.pce.computingEngine.ComputingResponse;
import es.tid.pce.computingEngine.algorithms.ChildPCEInitiate;
import es.tid.pce.computingEngine.algorithms.ChildPCERequest;
import es.tid.pce.pcep.PCEPProtocolViolationException;
import es.tid.pce.pcep.constructs.StateReport;
import es.tid.pce.pcep.messages.PCEPInitiate;
import es.tid.pce.pcep.messages.PCEPMessage;
import es.tid.pce.pcep.messages.PCEPReport;
import es.tid.pce.pcep.messages.PCEPRequest;
import es.tid.pce.pcep.messages.PCEPResponse;


public class IniPCCManager {

	private Hashtable<Inet4Address,DataOutputStream> pccOutputStream;
	
	public Hashtable<Long,Object> inilocks;
	
	private Hashtable<Long,StateReport> reports;
	
	private Logger log;

	
	public IniPCCManager(){
		
		inilocks = new Hashtable<Long, Object>();
		
	
		pccOutputStream=new Hashtable<Inet4Address,DataOutputStream>();
	
		reports = new Hashtable<Long,StateReport>();
	
		log = Logger.getLogger("PCEServer");
		
	}
	
	public StateReport newIni( PCEPInitiate pcini, Object domain){
		log.info("New Request to Child PCE");
		Object object_lock=new Object();

		long idRequest=pcini.getPcepIntiatedLSPList().get(0).getRsp().getSRP_ID_number();
		log.info("Creo lock con srp_id "+idRequest);
		inilocks.put(new Long(idRequest), object_lock);
		try {		
			sendInitiate(pcini,domain);
		} catch (IOException e1) {
			inilocks.remove(object_lock); 
			return null;
		}
		synchronized (object_lock) { 
			try {
				log.fine("Request sent, waiting for response");
				object_lock.wait(30000);
			} catch (InterruptedException e){
			//	FIXME: Ver que hacer
			}
		}	
		StateReport resp=reports.get(new Long(idRequest));
		if (resp==null){
			log.warning("NO RESPONSE!!!!!");
		}else {
			log.info("HA respondido LA ID "+idRequest);
		}
		return resp;
		
	}
	
	synchronized public  void sendInitiate(PCEPInitiate ini, Object node) throws IOException{
		try {
			ini.encode();
		} catch (PCEPProtocolViolationException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		DataOutputStream out= this.pccOutputStream.get(node);
		if (out==null){
			log.warning("There is no PCE for node "+node);
			throw new IOException();
		}
		try {
			log.info("Sending Initiate message to node "+node);
						out.write(ini.getBytes());
			out.flush();
		} catch (IOException e) {
			log.warning("Error sending Init: " + e.getMessage());
			throw e;
		}
	}
	
	public void notifyReport(StateReport sr){
		long idRequest=sr.getSRP().getSRP_ID_number();
		log.info("Entrando en Notify Report de id "+idRequest);
		Object object_lock=inilocks.get(new Long(idRequest));
		reports.put(new Long(idRequest), sr);
		if (object_lock!=null){
			object_lock.notifyAll();	
		}
		inilocks.remove(object_lock);
	}

	public Hashtable<Inet4Address, DataOutputStream> getPccOutputStream() {
		return pccOutputStream;
	}

	public void setPccOutputStream(
			Hashtable<Inet4Address, DataOutputStream> pccOutputStream) {
		this.pccOutputStream = pccOutputStream;
	}

	
}
