package tid.pce.client.lsp;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.logging.Logger;

import es.tid.pce.pcep.constructs.Path;
import es.tid.pce.pcep.constructs.StateReport;
import es.tid.pce.pcep.messages.PCEPMessage;
import es.tid.pce.pcep.messages.PCEPReport;
import es.tid.pce.pcep.objects.BandwidthRequested;
import es.tid.pce.pcep.objects.ExplicitRouteObject;
import es.tid.pce.pcep.objects.LSP;
import es.tid.pce.pcep.objects.ObjectParameters;
import es.tid.pce.pcep.objects.SRP;
import es.tid.pce.pcep.objects.tlvs.LSPDatabaseVersionTLV;
import es.tid.pce.pcep.objects.tlvs.IPv4LSPIdentifiersTLV;
import es.tid.pce.pcep.objects.tlvs.SymbolicPathNameTLV;
import tid.pce.client.lsp.te.LSPTE;
import tid.pce.server.lspdb.ReportDB;


/**
 * Clase que envia un PCEPReport cuando se le dice. Lo suyo seria en el futuro
 * poner los mensajes que se quieren enviar en una cola, y que haya un proceso que de vez en cuando
 * mire lo que hay en la cola y lo envie todo, de esta forma se pueden notificar varios LSPs en un
 * mismo mensaje
 * 
 * 
 * @author jaume
 *
 */

public class NotifyLSP 
{		
	LSPManager lspManager;

	Logger log;

	public NotifyLSP(LSPManager lspManager) 
	{
		this.lspManager = lspManager;
		log = lspManager.getLog();
	}

	public void notify(LSPTE lspte, boolean operational, boolean dFlag, boolean rFlag, boolean rSync, DataOutputStream out) 
	{

		if (lspte == null)
		{
			lspManager.getLog().info("lspte is NULL!!");
			return;

		}

		lspte.setDelegated(true);
		lspte.setDelegatedAdress(lspManager.getPCESession().getPeerPCE_IPaddress());

		PCEPReport m_report = new PCEPReport();
		StateReport state_report = new StateReport();

		SRP rsp = new SRP();

		/* Reserved value because we are not responding to an update */
		rsp.setSRP_ID_number(0);

		SymbolicPathNameTLV symPathName= new SymbolicPathNameTLV();

		symPathName.setSymbolicPathNameID(ByteBuffer.allocate(8).putLong(lspManager.getNextSymbolicPatheIdentifier()).array());
		rsp.setSymPathName(symPathName);


		LSP lsp = new LSP();

		/* LSP is active */
		lsp.setaFlag(true);
		/* Delegate the LSP*/
		lsp.setdFlag(dFlag);
		/* No sync */
		lsp.setsFlag(rSync);
		/* LSP has been removed */
		lsp.setrFlag(rFlag);

		/* Is LSP operational? */
		lsp.setOpFlags(ObjectParameters.LSP_OPERATIONAL_UP);

		lsp.setLspId(lspte.getIdLSP().intValue());

		IPv4LSPIdentifiersTLV lspIdTLV = new IPv4LSPIdentifiersTLV();

		lspIdTLV.setTunnelID((int)lspte.getTunnelId());
		lspIdTLV.setTunnelSenderIPAddress(lspte.getIdSource());   	 
		lspIdTLV.setExtendedTunnelID(0);

		log.info("Address: "+ lspIdTLV.getTunnelSenderIPAddress()+"lspID: "+ lsp.getLspId()+"sync flag: "+rSync+"actual db version: "+ lspManager.getDataBaseVersion());

		lsp.setLspIdentifiers_tlv(lspIdTLV);


		SymbolicPathNameTLV symbPathName = new SymbolicPathNameTLV();
		/* This id should be unique within the PCC */
		symbPathName.setSymbolicPathNameID(ByteBuffer.allocate(8).putLong(lsp.getLspId()).array());
		lsp.setSymbolicPathNameTLV_tlv(symbPathName);



		LSPDatabaseVersionTLV lspdDTLV = new LSPDatabaseVersionTLV();
		/* A change has been made so the database version is aumented */
		lspdDTLV.setLSPStateDBVersion(lspManager.getDataBaseVersion());
		//lspdDTLV.setLSPStateDBVersion(lspManager.getNextdataBaseVersion());

		lsp.setLspDBVersion_tlv(lspdDTLV);
		state_report.setLSP(lsp);
		state_report.setSRP(rsp); 
		/* Set the path */
		Path path = new Path();

		ExplicitRouteObject auxERO = new ExplicitRouteObject();
		auxERO.setEROSubobjectList(lspte.getEro().getEroSubobjects());

		path.seteRO(auxERO);

		BandwidthRequested bw = new BandwidthRequested();

		bw.setBw(lspte.getBw());

		path.setBandwidth(bw);

		/* For the time being, no metrics are added to the path object*/

		state_report.setPath(path);

		m_report.addStateReport(state_report);	

		ReportDB rptdb = lspManager.getRptdb();
		if (rptdb != null)
		{
			try {
				log.info("Adding State report to database");
				rptdb.add(state_report);
				log.info("Adding State report to database: finish");		
			} catch (Exception e) {
				log.info("Warning: a redis le ha dado un chungazo");
				e.printStackTrace();
			}

		}


		log.info("Sending PCEPReport message");




		//lspManager.getPCESession().sendPCEPMessage(m_report);
		sendPCEPMessage(m_report, out);

	}

	public void sendPCEPMessage(PCEPMessage message, DataOutputStream out) 
	{
		try 
		{
			message.encode();
		} 
		catch (Exception e11) 
		{
			log.severe("ERROR ENCODING ERROR OBJECT, BUG DETECTED, INFORM!!! "+e11.getMessage());
			log.severe("Ending Session");
		}
		try 
		{			
			out.write(message.getBytes());
			out.flush();
		} 
		catch (IOException e) 
		{
			log.severe("Problem writing message, finishing session "+e.getMessage());
		}
	}
}
