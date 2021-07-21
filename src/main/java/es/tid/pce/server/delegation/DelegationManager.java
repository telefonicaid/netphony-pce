package es.tid.pce.server.delegation;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.tid.pce.pcep.PCEPProtocolViolationException;
import es.tid.pce.pcep.constructs.Path;
import es.tid.pce.pcep.constructs.StateReport;
import es.tid.pce.pcep.constructs.UpdateRequest;
import es.tid.pce.pcep.messages.PCEPUpdate;
import es.tid.pce.pcep.objects.ExplicitRouteObject;
import es.tid.pce.pcep.objects.LSP;
import es.tid.pce.pcep.objects.SRP;
import es.tid.pce.server.SD_LSP;
import es.tid.pce.server.lspdb.SingleDomainLSPDB;
import es.tid.rsvp.objects.subobjects.IPv4prefixEROSubobject;

public class DelegationManager {

	private static int next_srp_id = 0;

	private SingleDomainLSPDB lsp_database;

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
					LSP ls = new LSP();
					ExplicitRouteObject ero;
					//ExplicitRouteObject ero = new ExplicitRouteObject();
					//Get original ero
					log.debug("Prepare answer with intended path in the ERO");
					ero= sr.getPath().getEro();
					
					
					
				
					srp.setSRP_ID_number(DelegationManager.getNextSRPID());
					ls.setLspId(sr.getLsp().getLspId());
					ls.setDelegateFlag(true);
					
					ls.setAdministrativeFlag(true);
					ls.setOpFlags(1);
					
					
					ur.setSrp(srp);
					log.info(sr.toString());
					if (sr.getLsp().getLspIdentifiers_tlv() != null) {
						ls.setLspIdentifiers_tlv(sr.getLsp().getLspIdentifiers_tlv());
					}
					if (sr.getLsp().getSymbolicPathNameTLV_tlv() != null) {
						ls.setSymbolicPathNameTLV_tlv(sr.getLsp().getSymbolicPathNameTLV_tlv());
					}

					ur.setLsp(ls);
					// ur.setLsp(sr.getLsp());
					Path path = new Path();
					path.setEro(ero);

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
		//ero= sr.getPath().getEro();
		
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



}
