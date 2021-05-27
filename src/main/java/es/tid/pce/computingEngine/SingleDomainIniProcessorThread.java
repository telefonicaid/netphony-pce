package es.tid.pce.computingEngine;

import java.net.Inet4Address;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import es.tid.pce.parentPCE.ParentPCESession;
import es.tid.pce.pcep.constructs.EndPointAndRestrictions;
import es.tid.pce.pcep.constructs.IPv4AddressEndPoint;
import es.tid.pce.pcep.constructs.PCEPIntiatedLSP;
import es.tid.pce.pcep.constructs.Path;
import es.tid.pce.pcep.constructs.StateReport;
import es.tid.pce.pcep.constructs.UnnumIfEndPoint;
import es.tid.pce.pcep.messages.PCEPInitiate;
import es.tid.pce.pcep.messages.PCEPReport;
import es.tid.pce.pcep.objects.Bandwidth;
import es.tid.pce.pcep.objects.EndPoints;
import es.tid.pce.pcep.objects.EndPointsIPv4;
import es.tid.pce.pcep.objects.EndPointsUnnumberedIntf;
import es.tid.pce.pcep.objects.ExplicitRouteObject;
import es.tid.pce.pcep.objects.GeneralizedEndPoints;
import es.tid.pce.pcep.objects.LSP;
import es.tid.pce.pcep.objects.P2PGeneralizedEndPoints;
import es.tid.pce.pcep.objects.SRP;
import es.tid.pce.pcep.objects.tlvs.EndPointIPv4TLV;
import es.tid.pce.pcep.objects.tlvs.SymbolicPathNameTLV;
import es.tid.pce.pcep.objects.tlvs.UnnumberedEndpointTLV;
import es.tid.pce.server.SD_LSP;
import es.tid.pce.server.SaveLSPinRedisSingleDom;
import es.tid.pce.server.IniPCCManager;
import es.tid.pce.server.lspdb.SingleDomainLSPDB;
import es.tid.rsvp.objects.subobjects.EROSubobject;
import es.tid.rsvp.objects.subobjects.IPv4prefixEROSubobject;
import es.tid.rsvp.objects.subobjects.UnnumberIfIDEROSubobject;
//import tid.pce.pcep.objects.tlvs.NotificationTLV;
//import tid.pce.pcep.objects.tlvs.subtlvs.NotificationSubTLV;

/**
 * IniProcessorThread is devoted to LSP Inititation from the PCE
 * 
 * @author ogondio
 *
 */
public class SingleDomainIniProcessorThread extends Thread {

	private static int lspIdSeq = 1;

	private SingleDomainLSPDB singleDomainLSPDB;

	/**
	 * The queue to read the initation requests
	 */
	private LinkedBlockingQueue<InitiationRequest> lspInitiationRequestQueue;

	private IniPCCManager iniManager;

	public LinkedBlockingQueue<InitiationRequest> getLspInitiationRequestQueue() {
		return lspInitiationRequestQueue;
	}

	public void setLspInitiationRequestQueue(LinkedBlockingQueue<InitiationRequest> lspInitiationRequestQueue) {
		this.lspInitiationRequestQueue = lspInitiationRequestQueue;
	}

	/**
	 * Indicates whether the thread is running
	 */
	private boolean running;

	SaveLSPinRedisSingleDom savelsp;

	/**
	 * Logger
	 */
	private Logger log;

	public SingleDomainIniProcessorThread(LinkedBlockingQueue<InitiationRequest> lspInitiationRequestQueue,
			SingleDomainLSPDB singleDomainLSPDB, IniPCCManager iniManager) {
		log = LoggerFactory.getLogger("PCEServer");
		this.lspInitiationRequestQueue = lspInitiationRequestQueue;
		this.singleDomainLSPDB = singleDomainLSPDB;
		this.iniManager = iniManager;
		// FIXME
		if (singleDomainLSPDB.isExportDb()) {
			savelsp = new SaveLSPinRedisSingleDom();
			savelsp.configure(singleDomainLSPDB, "127.0.0.1", 6379);
			running = true;
		}

	}

	/**
	 * The "core" of the Initiation Request
	 */
	public void run() {
		InitiationRequest iniReq;
		ExplicitRouteObject fullEro = null;
		while (running) {

			log.info("Waiting for a new Initiation Request");

			try {
				iniReq = lspInitiationRequestQueue.take();

				PCEPIntiatedLSP pini = iniReq.getLspIniRequest();
				PCEPIntiatedLSP piniToNode = new PCEPIntiatedLSP();
				SRP srp = new SRP();
				// fixme: sacar de otro sitio
				long sRP_ID_number_to_node = ParentPCESession.getNewReqIDCounter();
				srp.setSRP_ID_number(sRP_ID_number_to_node);
				piniToNode.setRsp(srp);
				srp.setRFlag(pini.getRsp().isRFlag());

				long sRP_ID_number = pini.getRsp().getSRP_ID_number();

				// Check if delete
				if (pini.getRsp().isRFlag()) {
					delete(pini.getLsp().getLspId(), sRP_ID_number, iniReq);
				} else {
					// Check if need to take path computation
					boolean needComputation = false;
					if (pini.getEro() != null) {
						needComputation = false;
						log.info("Provision Simple LSP  with ERO");
					} else {
						needComputation = true;
						log.info("Provision Simple LSP that needs computation");
					}
					if (needComputation) {
						fullEro = null;
					} else {
						fullEro = pini.getEro();
						log.info("XXXX fullEro=pini.getEro();" + fullEro);
					}
					piniToNode.setEro(fullEro);
					LSP lsp = new LSP();
					lsp.setSymbolicPathNameTLV_tlv(pini.getLsp().getSymbolicPathNameTLV_tlv());
					piniToNode.setLsp(lsp);
					LinkedList<PCEPInitiate> iniList = new LinkedList<PCEPInitiate>();
					// log.info("Creating the bandwidth");
					Bandwidth bw = null;

					// Get the Bandwidth
					if (pini.getBandwidth() != null) {
						bw = pini.getBandwidth().duplicate();
					}
					piniToNode.setBandwidth(bw);
					piniToNode.setEndPoint(pini.getEndPoint());

					PCEPInitiate pcepInitiate = new PCEPInitiate();
					pcepInitiate.getPcepIntiatedLSPList().add(piniToNode);

					// In this case there is not an LSP in the PCEPInitiate
					// It must be resolved in the PCE, afterwards the PCEPInitiate is completed and
					// sent to the corresponding node
					log.info("INITIATE with info, sending to node");
					String miIP;
					Inet4Address ip;
//					if (pini.getEndPoint()!= null){
//							log.info("jm endPoint NO es null");								
//						}
//					else {
//						log.info("jm endPoint es null");
//					}
					miIP = getSourceIP(pini.getEndPoint());
					log.info("ver ip to socket connect: " + miIP);

					ip = (Inet4Address) Inet4Address.getByName(miIP);

					StateReport sr = this.iniManager.newIni(pcepInitiate, ip);
					if (sr != null) {

						int lspId = sr.getLsp().getLspId();

						StateReport srmd = new StateReport();
						SRP srpp = new SRP();
						srpp.setSRP_ID_number(sRP_ID_number);
						srmd.setSrp(srpp);
						LSP lspp = new LSP();
						lspp.setLspId(lspId);
						srmd.setLsp(lspp);
						Path p = new Path();
						p.setEro(fullEro);
						srmd.setPath(p);

						SymbolicPathNameTLV symbolicPathNameTLV_tlv = new SymbolicPathNameTLV();
						if (sr.getLsp().getSymbolicPathNameTLV_tlv() != null) {
							symbolicPathNameTLV_tlv.setSymbolicPathNameID(
									sr.getLsp().getSymbolicPathNameTLV_tlv().getSymbolicPathNameID());
							lspp.setSymbolicPathNameTLV_tlv(symbolicPathNameTLV_tlv);
						} else {
							log.warn("NO SYMBOLIC PATH NAME TLV!!!");
						}

						PCEPReport rep = new PCEPReport();
						rep.getStateReportList().add(srmd);
						rep.encode();

						log.info("Send: " + rep.toString());
						iniReq.getOut().write(rep.getBytes());
						iniReq.getOut().flush();
						SD_LSP sd_lsp = new SD_LSP();
						sd_lsp.setpLSPID(lspId);
						sd_lsp.setFullERO(fullEro);
						sd_lsp.setStateRport(sr);
						sd_lsp.setEndpoints(pini.getEndPoint());

						singleDomainLSPDB.getSingleDomain_LSP_list().put(lspId, sd_lsp);
						if (singleDomainLSPDB.isExportDb()) {
							this.savelsp.run();
						}
					} else {
						// FIXME: Mandar error
					}
				}

			} catch (Exception e) {
				e.printStackTrace();
				log.error("PROBLEM SENDING THE INITIATES");
			}
		}

	}

	public String getSourceIP(Object endPoint) {
		
		String sourceIP=null;
		//String source=null;
		
		if (endPoint == null){
			log.info(" endPoint es null");
			
		}else 
			
			if (endPoint instanceof EndPointsIPv4){
				log.info(" endPoint es de tipo EndPointsIPv4");
				sourceIP = ((EndPointsIPv4) endPoint).getSourceIP().toString();
						
			}else 
				
				if (endPoint instanceof EndPointsUnnumberedIntf){
						log.info("endPoint es de tipo EndPointsUnnumberedIntf");
						sourceIP = ((EndPointsUnnumberedIntf) endPoint).getSourceIP().toString();
			
				}else{ 
					
					if (endPoint instanceof GeneralizedEndPoints){   
						log.info(" endPoint es de tipo GeneralizedEndPoints");
												
						//sourceIP 
						P2PGeneralizedEndPoints p2pep = (P2PGeneralizedEndPoints)endPoint;
						if (p2pep.getSourceEndpoint().getEndPoint() instanceof IPv4AddressEndPoint) {
						  sourceIP = ((IPv4AddressEndPoint)p2pep.getSourceEndpoint().getEndPoint()).getEndPointIPv4().getIPv4address().getHostAddress();
							} else if (p2pep.getSourceEndpoint().getEndPoint() instanceof UnnumIfEndPoint) {
							sourceIP = ((UnnumIfEndPoint)p2pep.getDestinationEndpoint().getEndPoint()).getUnnumberedEndpoint().getIPv4address().getHostAddress();
						}
//						
						
						
//						if( endPoint1 instanceof EndPointsIPv4){
//							log.info(" endPoint es de tipo GeneralizedEndPoints y EndPointsIPv4");
//							sourceIP = ((EndPointsIPv4) endPoint1).getSourceIP().toString();
//						}
//						else{
//							
//							if( endPoint1 instanceof EndPointsUnnumberedIntf){
//								
//								log.info(" endPoint es de tipo GeneralizedEndPoints y EndPointsUnnumberedIntf");
//								sourceIP = ((EndPointsUnnumberedIntf) endPoint1).getSourceIP().toString();
//							}else
//								log.info(" endPoint solo GeneralizedEndPoints");
//							
//						}
						
					}else log.info(" endPoint NO es de tipo conocido");
				}

	return sourceIP;

	}

	public EndPoints getEndPoints(ExplicitRouteObject ero) {
		log.info("Getting EndPoints");

		Iterator<EROSubobject> eroi = ero.getEROSubobjectList().iterator();
		EROSubobject eroso;
		P2PGeneralizedEndPoints gep = new P2PGeneralizedEndPoints();
		
		EndPointAndRestrictions sourceEndPoint = null;
		EndPointAndRestrictions destinationEndPoint = null;


		while (eroi.hasNext()) {
			eroso = eroi.next();
			if (eroso instanceof IPv4prefixEROSubobject) {
				if (sourceEndPoint == null) {
					sourceEndPoint = new EndPointAndRestrictions();
					IPv4AddressEndPoint sep=new IPv4AddressEndPoint();
					EndPointIPv4TLV epipv4 = new EndPointIPv4TLV();
					epipv4.setIPv4address(((IPv4prefixEROSubobject) eroso).getIpv4address());
					sep.setEndPointIPv4(epipv4);
					sourceEndPoint.setEndPoint(sep);
				} else if (!eroi.hasNext()) {
					IPv4AddressEndPoint dep=new IPv4AddressEndPoint();
					destinationEndPoint = new EndPointAndRestrictions();
					EndPointIPv4TLV epipv4 = new EndPointIPv4TLV();
					epipv4.setIPv4address(((IPv4prefixEROSubobject) eroso).getIpv4address());
					dep.setEndPointIPv4(epipv4);
					destinationEndPoint.setEndPoint(dep);
				}
			} else if (eroso instanceof UnnumberIfIDEROSubobject) {
				if (sourceEndPoint == null) {
					sourceEndPoint = new EndPointAndRestrictions();
					UnnumIfEndPoint sep=new UnnumIfEndPoint();
					UnnumberedEndpointTLV epipv4 = new UnnumberedEndpointTLV();
					epipv4.setIPv4address(((UnnumberIfIDEROSubobject) eroso).getRouterID());
					epipv4.setIfID(((UnnumberIfIDEROSubobject) eroso).getInterfaceID());
					sep.setUnnumberedEndpoint(epipv4);
					sourceEndPoint.setEndPoint(sep);
					
				} else {
					destinationEndPoint = new EndPointAndRestrictions();
					UnnumIfEndPoint dep=new UnnumIfEndPoint();
					UnnumberedEndpointTLV epipv4 = new UnnumberedEndpointTLV();
					epipv4.setIPv4address(((UnnumberIfIDEROSubobject) eroso).getRouterID());
					epipv4.setIfID(((UnnumberIfIDEROSubobject) eroso).getInterfaceID());
					dep.setUnnumberedEndpoint(epipv4);
					destinationEndPoint.setEndPoint(dep);
				}
			}
		}
		gep.setSourceEndpoint(sourceEndPoint);
		gep.setDestinationEndpoint(destinationEndPoint);
		return gep;

	}

	public void delete(int lspID, long sRP_ID_number, InitiationRequest iniReq) {
		log.info("GOING TO DELTE " + lspID);
		SD_LSP sdlsp = this.singleDomainLSPDB.getSingleDomain_LSP_list().get(lspID);
		if (sdlsp == null) {
			log.error("LSP is NULL!!");
		}
		LinkedList<PCEPInitiate> iniList = new LinkedList<PCEPInitiate>();
		// LinkedList<Object> domainList=new LinkedList<Object>();
		if (sdlsp != null) {

			log.info("LSP OK in DB!!!");

			PCEPInitiate ini = new PCEPInitiate();
			PCEPIntiatedLSP inilsp = new PCEPIntiatedLSP();
			ini.getPcepIntiatedLSPList().add(inilsp);
			SRP srp = new SRP();
			srp.setSRP_ID_number(ParentPCESession.getNewReqIDCounter());
			inilsp.setRsp(srp);
			srp.setRFlag(true);
			LSP lsp = new LSP();
			SymbolicPathNameTLV symbolicPathNameTLV_tlv = new SymbolicPathNameTLV();

			StateReport SR = sdlsp.getStateRport();

			symbolicPathNameTLV_tlv
					.setSymbolicPathNameID(SR.getLsp().getSymbolicPathNameTLV_tlv().getSymbolicPathNameID());
			lsp.setLspId(sdlsp.getpLSPID());
			lsp.setSymbolicPathNameTLV_tlv(symbolicPathNameTLV_tlv);
			inilsp.setLsp(lsp);
			iniList.add(ini);
			// domainList.add(domain);

			try {
				String miIP = getSourceIP(sdlsp.getEndpoints());
				log.info("ver ip to socket connect: " + miIP);

				ExplicitRouteObject ero = new ExplicitRouteObject();

				ero.addEROSubobjectList(sdlsp.getFullERO().getEROSubobjectList());

				Path path = new Path();
				path.setEro(ero);

				Inet4Address ip = (Inet4Address) Inet4Address.getByName(miIP);
				log.info("GOING TO send the deletes of " + lspID);
				StateReport sr = this.iniManager.newIni(ini, ip);
				log.info("Removing SD LSP " + lspID);
				this.singleDomainLSPDB.getSingleDomain_LSP_list().remove(lspID);
				log.info(" XXXX State Report");
				StateReport srmd = new StateReport();
				SRP srpp = new SRP();
				srpp.setSRP_ID_number(sRP_ID_number);
				srpp.setRFlag(true);
				srmd.setSrp(srpp);
				LSP lspp = new LSP();

				lspp.setLspId(lspID);
				srmd.setLsp(lspp);

				srmd.setPath(path);

				symbolicPathNameTLV_tlv = new SymbolicPathNameTLV();
				if (sr.getLsp().getSymbolicPathNameTLV_tlv() != null) {
					symbolicPathNameTLV_tlv
							.setSymbolicPathNameID(sr.getLsp().getSymbolicPathNameTLV_tlv().getSymbolicPathNameID());
					lspp.setSymbolicPathNameTLV_tlv(symbolicPathNameTLV_tlv);
				} else {
					log.warn("NO SYMBOLIC PATH NAME TLV!!!");
				}

				PCEPReport rep = new PCEPReport();
				rep.getStateReportList().add(srmd);
				rep.encode();

				log.info("Send Report to parent: " + rep.toString());
				iniReq.getOut().write(rep.getBytes());
				iniReq.getOut().flush();

			} catch (Exception e) {
				log.error("PROBLEM SENDING THE DELETES");
			}

		}
	}

	public static synchronized int getID() {
		SingleDomainIniProcessorThread.lspIdSeq += 1;
		return SingleDomainIniProcessorThread.lspIdSeq;
	}

}
