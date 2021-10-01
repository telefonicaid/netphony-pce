package es.tid.pce.server;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
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
import es.tid.pce.pcep.constructs.NAIIPv4NodeID;
import es.tid.pce.pcep.constructs.PCEPIntiatedLSP;
import es.tid.pce.pcep.constructs.Path;
import es.tid.pce.pcep.constructs.StateReport;
import es.tid.pce.pcep.messages.PCEPInitiate;
import es.tid.pce.pcep.messages.PCEPMessage;
import es.tid.pce.pcep.messages.PCEPReport;
import es.tid.pce.pcep.messages.PCEPRequest;
import es.tid.pce.pcep.messages.PCEPResponse;
import es.tid.pce.pcep.messages.PCEPUpdate;
import es.tid.pce.pcep.objects.Association;
import es.tid.pce.pcep.objects.AssociationIPv4;
import es.tid.pce.pcep.objects.BandwidthRequested;
import es.tid.pce.pcep.objects.EndPoints;
import es.tid.pce.pcep.objects.EndPointsIPv4;
import es.tid.pce.pcep.objects.ExplicitRouteObject;
import es.tid.pce.pcep.objects.LSP;
import es.tid.pce.pcep.objects.Metric;
import es.tid.pce.pcep.objects.SRP;
import es.tid.pce.pcep.objects.subobjects.SREROSubobject;
import es.tid.pce.pcep.objects.tlvs.ExtendedAssociationIDTLV;
import es.tid.pce.pcep.objects.tlvs.IPv4LSPIdentifiersTLV;
import es.tid.pce.pcep.objects.tlvs.PathSetupTLV;
import es.tid.pce.pcep.objects.tlvs.SRPolicyCandidatePathIdentifiersTLV;
import es.tid.pce.pcep.objects.tlvs.SRPolicyCandidatePathNameTLV;
import es.tid.pce.pcep.objects.tlvs.SRPolicyCandidatePathPreferenceTLV;
import es.tid.pce.pcep.objects.tlvs.SRPolicyName;
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
		//long idSRP= DelegationManager.getNextSRPID();
		log.warn("SRP ID: " + idSRP);
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
	

	
	public synchronized void initiateLSP(EndPoints endPoints, ExplicitRouteObject ero, Object node,int signalingType, String name) {
		
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
		lsp.setOpFlags(1);
		lsp.setLspId(0);
		
		PathSetupTLV path2 = new PathSetupTLV();
		path2.setPST(signalingType);
		srp.setPathSetupTLV(path2);
		
		inilsp.setLsp(lsp);
		
//		metric.setMetricType(2);
//		metric.setMetricValue((float)100);
//		inilsp.getMetricList().add(metric);
//
//	
//		bandwith.setBw((float)4096);
//		inilsp.setBandwidth(bandwith);
		
		name = name+getNextId();
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

	public void createCandidatePath(Object node,int color, Inet4Address ip_dest,int lsp_id,String policyName, String candidatePathName, String preference, ExplicitRouteObject ero) {
		PCEPInitiate initiate = new PCEPInitiate();
		PCEPIntiatedLSP inlsp = new PCEPIntiatedLSP();
		
		
		
		/**SRP**/
		SRP srp= new SRP();
		srp.setSRP_ID_number(DelegationManager.getNextSRPID());
		PathSetupTLV path = new PathSetupTLV();
		path.setPST(1);
		srp.setPathSetupTLV(path);
		inlsp.setSrp(srp);
		
		log.warn("SRP: " + srp.toString());
		/**LSP**/
		LSP lsp= new LSP();
		lsp.setAdministrativeFlag(true);
		lsp.setOpFlags(1);
		
		lsp_id = 0;
		lsp.setLspId(lsp_id);
		SymbolicPathNameTLV spn= new SymbolicPathNameTLV();
		String name = "PCE-INIPOL-LSP-NAME-" + srp.getSRP_ID_number();
		spn.setSymbolicPathNameID(name.getBytes());
		lsp.setSymbolicPathNameTLV_tlv(spn);
		log.warn("LSP: " + lsp.toString());
		
		inlsp.setLsp(lsp);
		/**Endpoints**/
		EndPointsIPv4 ep=new EndPointsIPv4();
		String src_ip= "1.1.1.1";
		
		Inet4Address ipp;
		try {
			ipp = (Inet4Address)Inet4Address.getByName(src_ip);
			((EndPointsIPv4) ep).setSourceIP(ipp);								
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		String dst_ip= "1.1.1.2";
		//String src_ip= "1.1.1.1";
		try {
			ipp = (Inet4Address)Inet4Address.getByName(dst_ip);
			((EndPointsIPv4) ep).setDestIP(ipp);								
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		inlsp.setEndPoint(ep);
		/**Association Object**/
		AssociationIPv4 aso=new AssociationIPv4();
		String string_ip_source = "10.95.43.175";
		Inet4Address ip_source = null;
		SRPolicyCandidatePathIdentifiersTLV policyIds=new SRPolicyCandidatePathIdentifiersTLV();
		ExtendedAssociationIDTLV extended_aso=new ExtendedAssociationIDTLV();
		
		try {
			ip_source = (Inet4Address) InetAddress.getByName(string_ip_source);

		} catch (UnknownHostException e) {

			e.printStackTrace();
		}
		
		aso.setAssociationSource(ip_source);
		aso.setAssocType(6);
		aso.setAssocID(1);
		
		
		//TLVs
		//Mandatories
		extended_aso.setColor(color);
		extended_aso.setEndpoint(ip_dest);
		
		aso.setExtended_ssociation_id_tlv(extended_aso);
		
		policyIds.setOriginatorAddress(ip_source);
		policyIds.setDiscriminator(0);
		policyIds.setOriginatorASN(0);
		policyIds.setProtocol(10); //Value of PCEP
		aso.setSr_policy_candidate_path_identifiers_tlv(policyIds);
		
		inlsp.getAssociationList().add(aso);
		
		candidatePathName = "PCE-INIPOL-CANDIDATEPATHNAME-" + srp.getSRP_ID_number();
		//Optionals
		if(policyName!=null) {
			SRPolicyName srPolicyName = new SRPolicyName();
			srPolicyName.setPolicyName(policyName);
			aso.setSr_policy_name(srPolicyName);
		}
		if(candidatePathName!=null) {
			SRPolicyCandidatePathNameTLV srPolicyCandidatePathName = new SRPolicyCandidatePathNameTLV();
			srPolicyCandidatePathName.setSRPolicyCandidatePathName(candidatePathName);
			aso.setSr_policy_candidate_path_tlv(srPolicyCandidatePathName);
		}
		if(preference!=null) {
			SRPolicyCandidatePathPreferenceTLV pathPreference = new SRPolicyCandidatePathPreferenceTLV();
			pathPreference.setPreference(Long.parseLong(preference));
			aso.setSr_policy_candidate_path_preference_tlv(pathPreference);
		}
		log.warn("ASO: " + aso.toString());
		
		//ERO
		inlsp.setEro(ero);
		log.warn("ERO: " + ero.toString());
		
		
		//Send Initiate
		initiate.getPcepIntiatedLSPList().add(inlsp);
		this.newIni(initiate, node);
		
		
	}

	public void deleteCandidatePath(Object node, int lsp_id) {
		PCEPInitiate initiate = new PCEPInitiate();
		PCEPIntiatedLSP inlsp = new PCEPIntiatedLSP();
		
		/**SRP**/
		SRP srp= new SRP();
		srp.setSRP_ID_number(DelegationManager.getNextSRPID());
		srp.setRFlag(true);
		
		/**LSP**/
		LSP lsp= new LSP();
		lsp.setRemoveFlag(true);
		lsp.setAdministrativeFlag(true);
		lsp.setLspId(lsp_id);
		
		inlsp.setLsp(lsp);
		inlsp.setSrp(srp);
		
		this.newIni(initiate, node);
		
	}
	

	
}
