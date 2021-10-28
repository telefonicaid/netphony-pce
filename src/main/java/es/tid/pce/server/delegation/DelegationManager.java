package es.tid.pce.server.delegation;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.tid.pce.pcep.PCEPProtocolViolationException;
import es.tid.pce.pcep.constructs.Path;
import es.tid.pce.pcep.constructs.StateReport;
import es.tid.pce.pcep.constructs.UpdateRequest;
import es.tid.pce.pcep.messages.PCEPUpdate;
import es.tid.pce.pcep.objects.Bandwidth;
import es.tid.pce.pcep.objects.BandwidthExistingLSP;
import es.tid.pce.pcep.objects.BandwidthUtilization;
import es.tid.pce.pcep.objects.ExplicitRouteObject;
import es.tid.pce.pcep.objects.LSP;
import es.tid.pce.pcep.objects.LSPA;
import es.tid.pce.pcep.objects.Metric;
import es.tid.pce.pcep.objects.SRP;
import es.tid.pce.pcep.objects.tlvs.PathSetupTLV;
import es.tid.pce.pcep.objects.tlvs.SymbolicPathNameTLV;
import es.tid.pce.server.SD_LSP;
import es.tid.pce.server.lspdb.SingleDomainLSPDB;
import es.tid.pce.utils.StringToPCEP;
import es.tid.rsvp.objects.subobjects.IPv4prefixEROSubobject;

public class DelegationManager {

	private static int next_srp_id = 0;

	private SingleDomainLSPDB lsp_database;

	private boolean compute_path=false; //True if the delegations are really computed
	
	private boolean getPathFromFile=true; // True if the path is get from a file
	
	private String file="C:\\Users\\b.lcm\\eclipse-workspace\\FORK\\netphony-network-protocols\\update_path.txt"; // True if the path is get from a file
	
	/**
	 * Logger
	 */
	private Logger log;

	public DelegationManager(SingleDomainLSPDB lsp_database) {
		this.lsp_database = lsp_database;
		log = LoggerFactory.getLogger("PCEServer");
	}

	public void processReport(StateReport sr, DataOutputStream out) {
		int lsp_id = sr.getLsp().getLspId();
		boolean accept__delegations = true;
		
		log.debug("Processing report of LSP ID " + lsp_id);
		if (lsp_id == 0) {
			log.debug("LSP ID 0, no need to process");
			return;
		}
		
		if(accept__delegations) {
			if (sr.getLsp().isDelegateFlag()) {
				// FIXME: By now, we accept delegation by default
				log.debug("The report has delegation flag true, checking if we need to delegate");
				// Check if we have LSP in database
				SD_LSP lsp = lsp_database.getSingleDomain_LSP_list().get(Integer.valueOf(lsp_id));
				boolean answer_delegation = false;
				if (lsp != null) {
					log.debug("LSP in database");
					if (!lsp.isDelegated()) {
						log.debug("LSP not yet delegated. Proceed to accept delegation");
						answer_delegation = true;
						lsp.setDelegated(true);
					}
				} else if (sr.getLsp().isCreateFlag()) {
					log.debug("LSP not yet in database, but created by the PCE... add to db and do nothing");
					answer_delegation = false;
					lsp = new SD_LSP();
					lsp.setStateRport(sr);
					lsp.setCreated(true);
					lsp.setDelegated(true);
					lsp_database.getSingleDomain_LSP_list().put(Integer.valueOf(lsp_id), lsp);
				} else {
					log.debug("LSP not yet in database and it is not created by the PCE. Proceed to accept delegation and add to database");
					lsp = new SD_LSP();
					lsp.setStateRport(sr);
					lsp.setDelegated(true);
					lsp_database.getSingleDomain_LSP_list().put(Integer.valueOf(lsp_id), lsp);
					answer_delegation = true;
				}
				if (answer_delegation) {
					PCEPUpdate pup = new PCEPUpdate();
					UpdateRequest ur = new UpdateRequest();
					SRP srp = new SRP();
					srp.setSRP_ID_number(DelegationManager.getNextSRPID());
					//First thing, check type of LSP (if RSVP or SR)
					log.info("Checking LSP signalling type");
					if (lsp.getStateRport().getSrp()!=null) {
						if (lsp.getStateRport().getSrp().getPathSetupTLV()!=null) {
							PathSetupTLV pst = new PathSetupTLV();
							if (lsp.getStateRport().getSrp().getPathSetupTLV().isSR()==true) {
								lsp.setSegmentRouting(true);
								log.info("The LSP is Segment Routing signalled");								
								pst.setPST(PathSetupTLV.SR);								
							}else {
								lsp.setSegmentRouting(false);
								log.info("The LSP is RSVP-TE signalled");								
								pst.setPST(PathSetupTLV.DEFAULT);				
							}
						}
						
					}
					ur.setSrp(srp);
					
					PathSetupTLV sym = new PathSetupTLV();
					sym.setPST(1);
					sym.setTLVType(28);
					srp.setPathSetupTLV(sym);
					
					
					
					LSP ls = new LSP();
					
					
					//Copy the LSP ID
					ls.setLspId(sr.getLsp().getLspId());
					//Set deletation flag to true
					ls.setDelegateFlag(true);
					//Set Admin flag to true
					ls.setAdministrativeFlag(true);
					//Set Operation Flags to UP
					ls.setOpFlags(1);
					//Read symbolic path name and copy it
					String name=null;
					if (sr.getLsp().getLspIdentifiers_tlv() != null) {
						
						ls.setLspIdentifiers_tlv(sr.getLsp().getLspIdentifiers_tlv());
					}
					if (sr.getLsp().getSymbolicPathNameTLV_tlv() != null) {
						name = new String(sr.getLsp().getSymbolicPathNameTLV_tlv().getSymbolicPathNameID());
						log.info("LSP Name "+name);
						ls.setSymbolicPathNameTLV_tlv(sr.getLsp().getSymbolicPathNameTLV_tlv());
					}
					ur.setLsp(ls);
					log.debug("Prepare answer with the Path");
					Path path = null;
					if (this.getPathFromFile) {
						log.debug("Getting path from a file");
						path=this.getEroFromFile();
					}else if (this.compute_path) {
						path=this.computePath();
					}else {
						//Copy the path
					}
					
					LSPA lspa = new LSPA();
					lspa.setExcludeAny(2);
					lspa.setHoldingPrio(0);
					lspa.setSetupPrio(7);
					path.setLspa(lspa);
						
//					Metric metric1 = new Metric();
//					
//					metric1.setBoundBit(true);
//					metric1.setMetricType(3);
//					metric1.setMetricValue(4);
//					
//					path.getMetricList().add(metric1);
//					
//					Metric metric = new Metric();
//					metric.setBoundBit(true);
//					metric.setMetricType(2);
//					metric.setMetricValue(30);
//					
//					path.getMetricList().add(metric);
//			
//					BandwidthExistingLSP bw = new BandwidthExistingLSP();
//					
//					bw.setOT(1);
//					bw.setBw(100);
//					
//					path.setBandwidth(bw);
					

					ur.setPath(path);	
//					List<Path> pathList = null;
//					if (this.getPathFromFile) {
//						log.debug("Getting path from a file");
//						pathList=this.getEroFromFile();
//					}else if (this.compute_path) {
//						path=this.computePath();
//					}else {
//						//Copy the path
//					}
//					
//					if(pathList != null) {
//						path = pathList.get(0);
//						if(pathList.size()>1) {
//							pathList.remove(0);
//						}
//					}
					
//					path.getMetricList().add(metric);
					ur.setPath(path);					
					//Copy association
				
					for (int i=0;i<lsp.getStateRport().getAssociationList().size();++i ) {
						log.info("adding association");
						ur.getAssociationList().add(lsp.getStateRport().getAssociationList().get(i));
					}
					log.info(sr.toString());


					pup.getUpdateRequestList().add(ur);

					try {
						log.info(pup.toString());
						pup.encode();
					} catch (PCEPProtocolViolationException e) {
						// TODO Auto-generated catch block
						log.error("PROBLEM ENCONDING UPDATE, CHECK CODE!!" + e.getMessage());
					}
					try {

						log.info("DELEGATION processeed, about to send update with empty ero");
						out.write(pup.getBytes());
						out.flush();
					} catch (IOException e) {
						log.warn("Could not send the UPDATE " + e.getMessage());

					}

				}

			}
		}else {
			UpdateRequest ur = new UpdateRequest();
			PCEPUpdate pup = new PCEPUpdate();
			LSP ls = new LSP();
			
			ls.setDelegateFlag(false);
			
			ur.setLsp(ls);			
			
			
		}
		
	}

	public static synchronized int getNextSRPID() {
		next_srp_id += 1;
		//log.info("THE SRP ID IS " + next_srp_id);
		return next_srp_id;
	}

	public void updateDelegatedPath(int lsp_id, boolean compute, ExplicitRouteObject ero, DataOutputStream out) {
		SD_LSP lsp = lsp_database.getSingleDomain_LSP_list().get(Integer.valueOf(lsp_id));
		StateReport sr=null;
		if (lsp!=null) {
		   sr= lsp.getStateRport();	
		}
		
		PCEPUpdate pup = new PCEPUpdate();
		UpdateRequest ur = new UpdateRequest();
		SRP srp = new SRP();
		LSP ls = new LSP();
		
		
	
		ls.setAdministrativeFlag(true);
		ls.setOpFlags(1);
		

		log.debug("Prepare answer with intended path in the ERO");
//		if (this.getPathFromFile) {
//			
//		}else if (this.compute_path) {
//			
//		}else {
//			
//		}
		
		srp.setSRP_ID_number(this.getNextSRPID());
		if (sr!=null) {
			ls.setLspId(sr.getLsp().getLspId());	
		}else {
			ls.setLspId(lsp_id);
		}
		
		ls.setDelegateFlag(true);
		
		ur.setSrp(srp);
		
		if (sr!=null) {
			log.info(sr.toString());
			if (sr.getLsp().getLspIdentifiers_tlv() != null) {
				ls.setLspIdentifiers_tlv(sr.getLsp().getLspIdentifiers_tlv());
			}
			if (sr.getLsp().getSymbolicPathNameTLV_tlv() != null) {
				ls.setSymbolicPathNameTLV_tlv(sr.getLsp().getSymbolicPathNameTLV_tlv());
			}
			
		}

		ur.setLsp(ls);
		// ur.setLsp(sr.getLsp());
		Path path = new Path();
		
		if (compute==false) {
			path.setEro(ero);	
		}
		

		// ur.setPath(sr.getPath());
		ur.setPath(path);
		pup.getUpdateRequestList().add(ur);

		try {
			log.info(pup.toString());
			pup.encode();
		} catch (PCEPProtocolViolationException e) {
			// TODO Auto-generated catch block
			log.error("PROBLEM ENCONDING UPDATE, CHECK CODE!!" + e.getMessage());
		}
		try {

			log.info("DELEGATION processeed, about to send update with empty ero");
			out.write(pup.getBytes());
			out.flush();
		} catch (IOException e) {
			log.warn("Could not send the UPDATE " + e.getMessage());

		}
	}

	public SingleDomainLSPDB getLsp_database() {
		return lsp_database;
	}

	public void setLsp_database(SingleDomainLSPDB lsp_database) {
		this.lsp_database = lsp_database;
	}

//	public List<Path> getEroFromFile() {
//		FileReader fr;
//		List<Path> pathList =new ArrayList();
//		Path path = new Path();
//		try {
//			fr = new FileReader(this.file);
//			BufferedReader reader = new BufferedReader(fr);
//			String line = reader.readLine();
//			while(line != null ) {
//				log.info("Creating ERO "+line);
//				ExplicitRouteObject ero=StringToPCEP.stringToExplicitRouteObject(line);
//				path.setEro(ero);
//				pathList.add(path);
//				
//				line = reader.readLine();
//			}
//			
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	return pathList;
//	}
		
	public Path getEroFromFile() {
		FileReader fr;
		Path path =new Path();
		try {
			fr = new FileReader(this.file);
			BufferedReader reader = new BufferedReader(fr);
			String line = reader.readLine();
			log.info("Creating ERO "+line);
			ExplicitRouteObject ero=StringToPCEP.stringToExplicitRouteObject(line);
			path.setEro(ero);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return path;
	}
		
	
	
	public Path computePath() {
		Path path =new Path();
		ExplicitRouteObject ero = new ExplicitRouteObject();
		path.setEro(ero);
		return path;
	}


}
