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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.tid.pce.computingEngine.ComputingResponse;
import es.tid.pce.computingEngine.algorithms.ChildPCEInitiate;
import es.tid.pce.computingEngine.algorithms.ChildPCERequest;
import es.tid.pce.pcep.PCEPProtocolViolationException;
import es.tid.pce.pcep.constructs.PCEPIntiatedLSP;
import es.tid.pce.pcep.constructs.Path;
import es.tid.pce.pcep.constructs.StateReport;
import es.tid.pce.pcep.messages.PCEPInitiate;
import es.tid.pce.pcep.messages.PCEPMessage;
import es.tid.pce.pcep.messages.PCEPReport;
import es.tid.pce.pcep.messages.PCEPRequest;
import es.tid.pce.pcep.messages.PCEPResponse;
import es.tid.pce.pcep.messages.PCEPUpdate;
import es.tid.pce.pcep.objects.BandwidthRequested;
import es.tid.pce.pcep.objects.EndPoints;
import es.tid.pce.pcep.objects.EndPointsIPv4;
import es.tid.pce.pcep.objects.ExplicitRouteObject;
import es.tid.pce.pcep.objects.LSP;
import es.tid.pce.pcep.objects.Metric;
import es.tid.pce.pcep.objects.SRP;
import es.tid.pce.pcep.objects.tlvs.IPv4LSPIdentifiersTLV;
import es.tid.pce.pcep.objects.tlvs.PathSetupTLV;
import es.tid.pce.pcep.objects.tlvs.SymbolicPathNameTLV;
import es.tid.pce.server.delegation.DelegationManager;


public class IniPCCManager {
	
	private int nextId=0;

	private Hashtable<Inet4Address,DataOutputStream> pccOutputStream;
	
	public Hashtable<Long,Object> inilocks;
	
	private Hashtable<Long,StateReport> reports;
	
	private Logger log;

	
	public IniPCCManager(){
		
		inilocks = new Hashtable<Long, Object>();
		
	
		pccOutputStream=new Hashtable<Inet4Address,DataOutputStream>();
	
		reports = new Hashtable<Long,StateReport>();
	
		log = LoggerFactory.getLogger("PCEServer");
		
	}
	
	public StateReport newIni( PCEPInitiate pcini, Object node){
		Object object_lock=new Object();

		long idSRP=pcini.getPcepIntiatedLSPList().get(0).getRsp().getSRP_ID_number();
		log.info("Sending PCEPInitiate to node "+node+"srp_id "+idSRP+" : "+pcini.toString());
		inilocks.put(new Long(idSRP), object_lock);
		try {		
			sendInitiate(pcini,node);
		} catch (IOException e1) {
			log.warn("Problem with response from node "+node+" to initiate with srp_id "+idSRP);
			inilocks.remove(object_lock); 
			return null;
		}
		synchronized (object_lock) { 
			try {
				log.debug("Request sent, waiting for response");
				object_lock.wait(30000);
			} catch (InterruptedException e){
			//	FIXME: Ver que hacer
			}
		}	
		StateReport resp=reports.get(new Long(idSRP));
		if (resp==null){
			log.warn("No response from node "+node+" to initiate with srp_id "+idSRP);
		}else {
			log.info("Node "+node+" replied to Initiate with srp_id "+idSRP+" : "+resp.toString());
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
			log.warn("There is no PCE for node "+node);
			throw new IOException();
		}
		try {
			log.info("Sending Initiate message to node "+node);
						out.write(ini.getBytes());
			out.flush();
		} catch (IOException e) {
			log.warn("Error sending Init: " + e.getMessage());
			throw e;
		}
	}
	
	public void notifyReport(StateReport sr){
		long idRequest=sr.getSrp().getSRP_ID_number();
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
	
	public synchronized void initiateLSP(EndPoints endPoints, ExplicitRouteObject ero, Object node) {
		
		PCEPInitiate ini = new PCEPInitiate();
		PCEPIntiatedLSP inilsp = new PCEPIntiatedLSP();
		
		Metric metric= new Metric();
		BandwidthRequested bandwith = new BandwidthRequested();
		ini.getPcepIntiatedLSPList().add(inilsp);
		SRP srp= new SRP();
		srp.setSRP_ID_number(DelegationManager.getNextSRPID());
		Path path = new Path();
		inilsp.setEro(ero);
		inilsp.setEndPoint(endPoints);
		inilsp.setSrp(srp);
		LSP lsp= new LSP();
		lsp.setAdministrativeFlag(true);
		lsp.setLspId(0);
		
		PathSetupTLV path2 = new PathSetupTLV();
		path2.setTLVType(28);
		srp.setPathSetupTLV(path2);
		
		inilsp.setLsp(lsp);
		
		
		metric.setMetricType(3);
		metric.setMetricValue((float)4);
		inilsp.getMetricList().add(metric);
		
		
		bandwith.setBw((float)0);
		inilsp.setBandwidth(bandwith);
		
		String name="Oscar-initated LSP-id "+getNextId();
		SymbolicPathNameTLV spn= new SymbolicPathNameTLV();
		spn.setSymbolicPathNameID(name.getBytes());
		lsp.setSymbolicPathNameTLV_tlv(spn);
		this.newIni(ini, node); 
	}
	
	public synchronized int getNextId() {
		nextId+=1;
		return nextId;
	}

	public void terminateLSP(int lsp_number,Object node) {
		PCEPInitiate terminate = new PCEPInitiate();
		PCEPIntiatedLSP inlsp = new PCEPIntiatedLSP();
		SRP srp= new SRP();
		srp.setSRP_ID_number(DelegationManager.getNextSRPID());
		srp.setRFlag(true);
		LSP lsp= new LSP();
		lsp.setRemoveFlag(true);
		lsp.setAdministrativeFlag(true);
		lsp.setLspId(lsp_number);
		inlsp.setLsp(lsp);
		inlsp.setSrp(srp);
		terminate.getPcepIntiatedLSPList().add(inlsp);
		
		this.newIni(terminate, node);
		

	}
	

	
}
