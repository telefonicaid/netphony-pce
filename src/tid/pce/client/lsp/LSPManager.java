/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package tid.pce.client.lsp;

import java.io.DataOutputStream;
import java.io.PrintStream;
import java.net.Inet4Address;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

import es.tid.pce.pcep.constructs.Path;
import es.tid.pce.pcep.constructs.Response;
import es.tid.pce.pcep.messages.PCEPRequest;
import es.tid.pce.pcep.messages.PCEPResponse;
import es.tid.pce.pcep.messages.PCEPUpdate;
import es.tid.pce.pcep.objects.Bandwidth;
import es.tid.pce.pcep.objects.BandwidthRequested;
import es.tid.pce.pcep.objects.ExplicitRouteObject;
import es.tid.pce.pcep.objects.SRERO;
import es.tid.pce.pcep.objects.subobjects.SREROSubobject;
import es.tid.rsvp.messages.RSVPMessage;
import es.tid.rsvp.messages.RSVPPathErrMessage;
import es.tid.rsvp.messages.RSVPPathTearMessage;
import es.tid.rsvp.messages.te.RSVPTEPathMessage;
import es.tid.rsvp.messages.te.RSVPTEResvMessage;
import es.tid.rsvp.objects.ERO;
import es.tid.rsvp.objects.subobjects.EROSubobject;
import es.tid.rsvp.objects.subobjects.GeneralizedLabelEROSubobject;
import es.tid.rsvp.objects.subobjects.IPv4prefixEROSubobject;
import es.tid.rsvp.objects.subobjects.SubObjectValues;
import es.tid.rsvp.objects.subobjects.UnnumberIfIDEROSubobject;
import tid.emulator.node.FastPCEPSession;
import tid.emulator.node.resources.ResourceManager;
import tid.emulator.node.transport.LSPCreationException;
import tid.emulator.node.transport.PathComputationClient;
import tid.emulator.node.transport.rsvp.RSVPManager;
import tid.pce.client.PCCPCEPSession;
import tid.pce.client.lsp.te.LSPTE;
import tid.pce.client.lsp.te.PathStateParameters;
import tid.pce.server.lspdb.ReportDB;
import tid.util.UtilsFunctions;
/**
 *	The LSP Manager role is getting all the LSP that are born in the ROADM and the ones
 *  which it is one of the middle nodes, in order to be capable of taking actions to
 *  restore connectivity in case of failure or path re-optimization.
 *  
 *  Its parameters are the following:
 *  
 *  
 *  	1.-	LSPList:		A table where every LSPConnection can be reached by its identifier
 *  	2.-	idNewLSP:		In order to never repeating the LSP identifier, its saved the next
 *  						identifier that will be used for the next LSP that will be created. 
 *  	
 * @author fmn
 */

public class LSPManager {

	private LinkedList<ExplicitRouteObject> eroList;
	/**
	 * List of LSPs 
	 */

	private LinkedList<SRERO> SREROList;
	/**
	 * List of LSP with SIDs
	 */

	private Hashtable<LSPKey, LSPTE> LSPList;
	/**
	 * PCEP Session with the PCE
	 * FIXME: accederemos a traves de la pcc
	 */
	private PathComputationClient pcc;
	/**
	 * Acces to the RSVP Manager
	 */
	private RSVPManager managerRSVP;

	private Logger log;
	private Hashtable<Long,Lock> lockList;
	private Hashtable<Long,Condition> conditionList;
	/**
	 * Identifier for the Established LSP
	 */
	private static long idNewLSP=0;

	/**
	 * Local IPv4 of the Node
	 */
	private Inet4Address localIP;

	/**
	 * Access to the specific resource Manager of the Node
	 */
	private ResourceManager resourceManager;

	private boolean rsvpMode = true;

	/**
	 * Variable to notify established path in case we avoid control plane rreservation
	 */
	private boolean established = false;

	/**
	 * Variables to control the LSP establishment time in the Control Plane
	 */
	private long timeIni;
	private long timeIni_Node;
	private long timeEnd;
	private long timeEnd_Node;    

	private boolean isStateful = false;

	private NotifyLSP notiLSP;

	private DataOutputStream out=null;

	private AtomicInteger dataBaseVersion;
	private AtomicLong symbolicPathIdentifier;

	private PCCPCEPSession PCESession;

	private FastPCEPSession fastSession;

	private ReportDB rptdb;
	
	public LSPManager(boolean isStateful){
		log = Logger.getLogger("ROADM");
		lockList=new Hashtable<Long,Lock>();
		conditionList=new Hashtable<Long,Condition>();
		LSPList = new Hashtable<LSPKey, LSPTE>();
		this.isStateful = isStateful;
		if (isStateful)
		{
			notiLSP = new NotifyLSP(this);
			dataBaseVersion = new AtomicInteger();
			symbolicPathIdentifier = new AtomicLong();
			symbolicPathIdentifier.incrementAndGet();
		}
	}

	public LSPManager(){
		dataBaseVersion = new AtomicInteger();
		log = Logger.getLogger("ROADM");
		lockList=new Hashtable<Long,Lock>();
		conditionList=new Hashtable<Long,Condition>();
		LSPList = new Hashtable<LSPKey, LSPTE>();
	}

	public void configureLSPManager(RSVPManager rsvpManager, Inet4Address localIP, PathComputationClient pcc, ResourceManager resourceManager, boolean rsvpMode){
		this.managerRSVP = rsvpManager;
		this.localIP=localIP;
		this.pcc=pcc;
		this.resourceManager=resourceManager;
		this.rsvpMode=rsvpMode;
	}

	/**
	 * Method to create a new TE LSP initiated in this node
	 * @param destinationId IP AddreStart LSP Errorss of the destination of the LSP
	 * @param bw Bandwidth requested
	 * @param bidirectional bidirectional
	 * @param OFcode
	 * @throws LSPCreationException 
	 */
	public long addnewLSP(Inet4Address destinationId, float bw, boolean bidirectional, int OFcode) throws LSPCreationException{
		log.info("Adding New LSP to "+destinationId);
		//FIXME: mirar esto
		//meter structura --> RequestedLSPinformation --> Dependiente de cada tecnologia
		//meter campo con el estado del LSP e ir cambiandolo
		LSPTE lsp = new LSPTE(this.getIdNewLSP(), localIP, destinationId, bidirectional, OFcode, bw, PathStateParameters.creatingLPS);
		LSPList.put(new LSPKey(localIP, lsp.getIdLSP()), lsp);
		ReentrantLock lock= new ReentrantLock();
		Condition lspEstablished =lock.newCondition();
		//log.info("Metemos en Lock list con ID: "+lsp.getIdLSP());
		lockList.put(lsp.getIdLSP(), lock);
		conditionList.put(lsp.getIdLSP(), lspEstablished);
		/*log.info("Size lockList : "+lockList.size());
    	log.info("Size conditionList : "+conditionList.size());*/
		timeIni = System.nanoTime();
		log.info("Start to establish path: "+System.nanoTime());
		try{
			startLSP(lsp);
		}catch(LSPCreationException e){
			log.info("Start LSP Error!");
			conditionList.remove(lsp.getIdLSP());
			lockList.remove(lsp.getIdLSP());
			LSPList.remove(lsp.getIdLSP());
			log.info(UtilsFunctions.exceptionToString(e));
			throw e;
		}    	
		return lsp.getIdLSP();
	}
	/**/
	public long addnewLSP(Inet4Address destinationId, float bw, boolean bidirectional, int OFcode, ERO eroRSVP) throws LSPCreationException{
		log.info("Adding New LSP to "+destinationId);
		//FIXME: mirar esto
		//meter structura --> RequestedLSPinformation --> Dependiente de cada tecnologia
		//meter campo con el estado del LSP e ir cambiandolo
		LSPTE lsp = new LSPTE(this.getIdNewLSP(), localIP, destinationId, bidirectional, OFcode, bw, PathStateParameters.creatingLPS);
		LSPList.put(new LSPKey(localIP, lsp.getIdLSP()), lsp);
		log.info("LSPList:: "+LSPList.size()+" "+(new LSPKey(localIP, lsp.getIdLSP()).toString()));
		log.info(LSPList.toString());
		ReentrantLock lock= new ReentrantLock();
		Condition lspEstablished =lock.newCondition();
		//log.info("Metemos en Lock list con ID: "+lsp.getIdLSP());
		lockList.put(lsp.getIdLSP(), lock);
		log.info("lsp.getIdLSP():" + lsp.getIdLSP());
		log.info("lspEstablished:" + lspEstablished);
		conditionList.put(lsp.getIdLSP(), lspEstablished);
		/*log.info("Size lockList : "+lockList.size());
    	log.info("Size conditionList : "+conditionList.size());*/
		timeIni = System.nanoTime();
		log.info("Start to establish path: "+System.nanoTime());

		startLSP(lsp,eroRSVP);

		return lsp.getIdLSP();
	}


	/**
	 * Method to create a new TE LSP initiated in this node
	 * @param destinationId IP AddreStart LSP Errorss of the destination of the LSP
	 * @param bw Bandwidth requested
	 * @param bidirectional bidirectional
	 * @param OFcode
	 * @throws LSPCreationException 
	 */
	public long addnewLSP(Inet4Address destinationId, float bw, boolean bidirectional, int OFcode, int lspID) throws LSPCreationException{
		log.info("Adding New LSP to "+destinationId);
		//FIXME: mirar esto
		//meter structura --> RequestedLSPinformation --> Dependiente de cada tecnologia
		//meter campo con el estado del LSP e ir cambiandolo
		LSPTE lsp = new LSPTE(lspID, localIP, destinationId, bidirectional, OFcode, bw, PathStateParameters.creatingLPS);
		LSPList.put(new LSPKey(localIP, lsp.getIdLSP()), lsp);
		ReentrantLock lock= new ReentrantLock();
		Condition lspEstablished =lock.newCondition();
		//log.info("Metemos en Lock list con ID: "+lsp.getIdLSP());
		lockList.put(lsp.getIdLSP(), lock);
		conditionList.put(lsp.getIdLSP(), lspEstablished);
		/*log.info("Size lockList : "+lockList.size());
    	log.info("Size conditionList : "+conditionList.size());*/
		timeIni = System.nanoTime();
		log.info("Start to establish path: "+System.nanoTime());
		try{
			startLSP(lsp);
		}catch(LSPCreationException e){
			log.info("Start LSP Error!");
			throw e;
		}    	
		return lsp.getIdLSP();
	}

	public void deleteLSP(Inet4Address sourceId, long lspId){
		log.info("Deleting LSP with "+sourceId+" and LSP Id "+lspId);
		LSPKey key = new LSPKey(sourceId, lspId);
		LSPTE lsp = LSPList.remove(key);
		teardownLSP(lsp);
	}

	public void waitForLSPaddition(long lspId, long timeWait){
		Lock lock;
		Condition lspEstablished;
		try {
			lock=lockList.get(lspId);
			if (lock == null)
				log.info("Lock is NULL!");
			lspEstablished=conditionList.get(lspId);	
		}catch (Exception e){
			return;
		} 

		lock.lock();
		try {
			if (established==false){
				log.info("Waiting "+timeWait+" ms  for LSP "+lspId+" to be established");
				lspEstablished.await(timeWait, TimeUnit.MILLISECONDS);
			}else{
				log.info("Inside waitForLSPaddition lockList.remove");
				//FIXME: Revisar esto
				lockList.remove(lspId);
				conditionList.remove(lspId);
			}
			log.info("LSP "+lspId+" has been established");
		} catch (InterruptedException e) {
			return;
		}finally {
			lock.unlock();
		}
	}

	public void notifyLPSEstablished(long lspId, Inet4Address src){

		//Lo pongo al principio de momento porque al final de la funcion nunca llega. 
		//No se si es un bug

		log.info("is Stateful??::" +isStateful);
		//if PCC is stateful the new LSP must be notified to the PCE
		if (isStateful)
		{
			log.info("LSPList: "+LSPList.size()+" "+(new LSPKey(src, lspId)).toString());
			this.getNextdataBaseVersion();
			notiLSP.notify(LSPList.get(new LSPKey(src, lspId)), true, true, false, false, getPCESession().getOut());
		}

		Lock lock;
		Condition lspEstablished;
		LSPTE lsp;
		timeEnd = System.nanoTime();
		log.info("Time to Procces RSVP Resv Mssg in Node (ms): "+((timeEnd_Node-timeIni_Node)/1000000));
		log.info("LSP total Time (ms): "+((timeEnd-timeIni)/1000000));
		try {
			lock=lockList.get(lspId);
			lspEstablished=conditionList.get(lspId);
			lsp=LSPList.get(new LSPKey(src, lspId));
		}catch (Exception e){
			log.info(UtilsFunctions.exceptionToString(e));
			return;
		}
		//Comento esto y la linea de abajo esto porque peta y estas que se porque
		log.info("lspId::" + lspId);
		lock.lock();
		try 
		{
			lspEstablished.signalAll();
		} finally {
			log.info("notifyLSPEstablished lockList.remove");
			lockList.remove(lspId);
			conditionList.remove(lspId);
			lock.unlock();
		}
	}

	public void notifyLPSEstablishmentFail(long lspId, Inet4Address src){
		Lock lock;
		Condition lspEstablished;
		try {
			lock=lockList.get(lspId);
			lspEstablished=conditionList.get(lspId);
		}catch (Exception e){
			return;
		}    	
		lock.lock();
		try {
			lspEstablished.signalAll();
			//lsp.set
		} finally {
			log.info("notifyLPSEstablishmentFail lockList.remove");
			lockList.remove(lspId);
			conditionList.remove(lspId);
			lock.unlock();
		}
		if (isStateful)
		{
			dataBaseVersion.incrementAndGet();
			notiLSP.notify(LSPList.get(lspId), false, false, false, false, getPCESession().getOut());
		}
	}

	/**
	 * 									startLSP()
	 * Envía y recibe la respuesta del PCE con la ruta calculada
	 * Se encarga de crear el mensaje RSVP path y enviarlo al primera nodo de destino   
	 * 
	 * Hay que comprobar si tenemos interfaz disponible en el roadm para que enviar los datos.     * 
	 * @throws LSPCreationException 
	 **/

	public void startLSP(LSPTE lsp) throws LSPCreationException{
		// Get specific request from Resource Manager
		PCEPRequest req = resourceManager.getPCEPRequest(lsp);
		// Send Request to the PCE
		PCEPResponse pr;

		try{
			pr = pcc.getCrm().newRequest(req);
		}catch (Exception e){
			log.info(UtilsFunctions.exceptionToString(e));
			throw new LSPCreationException(LSPCreationErrorTypes.ERROR_REQUEST);
		}
		// No Response from PCE
		if (pr == null){
			LSPList.remove(new LSPKey(localIP, lsp.getIdLSP()));
			throw new LSPCreationException(LSPCreationErrorTypes.NO_RESPONSE);
		}
		Response resp = pr.getResponse(0);
		// Response No Path from PCE
		if (resp.getNoPath()!=null){
			log.info("Response : No PATH: --> "+resp.getNoPath().toString());
			lsp.setPcepResponse(resp);
			LSPList.remove(new LSPKey(localIP, lsp.getIdLSP()));
			throw new LSPCreationException(LSPCreationErrorTypes.NO_PATH);
		}
		ERO eroRSVP = new ERO();
		if (rsvpMode==false){
			log.info("Response : "+pr.toString());
			log.info("RSVP Mode false --> enviamos Notify LSP Established");
			established = true;
			lsp.setPcepResponse(resp);

			LinkedList<EROSubobject> clone = (LinkedList<EROSubobject>) resp.getPathList().get(0).geteRO().getEROSubobjectList().clone();
			eroRSVP.setEroSubobjects(clone);
			lsp.setEro(eroRSVP);
		}else{
			log.info("Response : "+pr.toString());
			//saveEroStats(resp.getPath(0).geteRO());
			// Response OK
			lsp.setPcepResponse(resp);
			if(resp.getPathList().get(0).geteRO()!=null)
			{
				LinkedList<EROSubobject> clone = (LinkedList<EROSubobject>) resp.getPathList().get(0).geteRO().getEROSubobjectList().clone();
				eroRSVP.setEroSubobjects(clone);
				lsp.setEro(eroRSVP);

				boolean check = resourceManager.checkResources(lsp);
				if (check==false){
					// No Resources Available --> Remove LSP from List
					LSPList.remove(new LSPKey(localIP, lsp.getIdLSP()));

					//FIXME: Crear el pathErr para enviar
					//Creamos Path Error Message
					RSVPPathErrMessage PathErr = new RSVPPathErrMessage();
					log.warning("There are no resources available");
					throw new LSPCreationException(LSPCreationErrorTypes.NO_RESOURCES);
				}else{
					//FIXME: ver si añadimos la lambda al LSP en otro momento o no
					// de momento la información de lambda asociada la tenemos en el RSVP Manager
					//lsp.setLambda(resourceManager.getLambda());
					Inet4Address prox = null;
					RSVPTEPathMessage path = resourceManager.getRSVPTEPathMessageFromPCEPResponse(lsp);
					//prox = (resourceManager.getProxHopIPv4List()).get(new LSPKey((resourceManager.getProxHopIPv4List()).keys().nextElement().getSourceAddress(), (resourceManager.getProxHopIPv4List()).keys().nextElement().getLspId()));
					prox = (resourceManager.getProxHopIPv4List()).get(new LSPKey(lsp.getIdSource(), lsp.getIdLSP()));
					timeEnd_Node=System.nanoTime();
					log.info("LSP Time to Process RSVP Path Mssg in Node (ms): "+((timeEnd_Node-timeIni_Node)/1000000) + " --> Sending RSVP path Message to "+prox.toString()+" !");
					sendRSVPMessage(path,prox);
				}
			}
			else if(resp.getPathList().get(0).getSRERO()!=null)
			{
				SRERO srero = new SRERO();
				LinkedList<SREROSubobject> clone = (LinkedList<SREROSubobject>) resp.getPathList().get(0).getSRERO().getSREROSubobjectList().clone();
				srero.setSREROSubobjectList(clone);
				lsp.setSRERO(srero);
				log.info("SID encontrado: "+srero.toString());

				//				boolean check = resourceManager.checkResources(lsp);
				//				if (check==false){
				//					// No Resources Available --> Remove LSP from List
				//					LSPList.remove(new LSPKey(localIP, lsp.getIdLSP()));
				//
				//					//FIXME: Crear el pathErr para enviar
				//					//Creamos Path Error Message
				//					RSVPPathErrMessage PathErr = new RSVPPathErrMessage();
				//					log.warning("There are no resources available");
				//					throw new LSPCreationException(LSPCreationErrorTypes.NO_RESOURCES);
				//				}else{
				//					//FIXME: ver si añadimos la lambda al LSP en otro momento o no
				//					// de momento la información de lambda asociada la tenemos en el RSVP Manager
				//					//lsp.setLambda(resourceManager.getLambda());
				//					Inet4Address prox = null;
				//					RSVPTEPathMessage path = resourceManager.getRSVPTEPathMessageFromPCEPResponse(lsp);
				//					//prox = (resourceManager.getProxHopIPv4List()).get(new LSPKey((resourceManager.getProxHopIPv4List()).keys().nextElement().getSourceAddress(), (resourceManager.getProxHopIPv4List()).keys().nextElement().getLspId()));
				//					prox = (resourceManager.getProxHopIPv4List()).get(new LSPKey(lsp.getIdSource(), lsp.getIdLSP()));
				//					timeEnd_Node=System.nanoTime();
				//					log.info("LSP Time to Process RSVP Path Mssg in Node (ms): "+((timeEnd_Node-timeIni_Node)/1000000) + " --> Sending RSVP path Message to "+prox.toString()+" !");
				//					sendRSVPMessage(path,prox);
				//				}				

			}
		}
	}

	public void startLSP(LSPTE lsp, ERO eroRSVP)
	{		
		lsp.setEro(eroRSVP);

		boolean check = resourceManager.checkResources(lsp);
		if (check==false){
			// No Resources Available --> Remove LSP from List
			LSPList.remove(new LSPKey(localIP, lsp.getIdLSP()));

			//FIXME: Crear el pathErr para enviar
			//Creamos Path Error Message
			RSVPPathErrMessage PathErr = new RSVPPathErrMessage();
			log.warning("There are no resources available");
		}else{
			//FIXME: ver si añadimos la lambda al LSP en otro momento o no
			// de momento la información de lambda asociada la tenemos en el RSVP Manager
			//lsp.setLambda(resourceManager.getLambda());
			Inet4Address prox = null;
			RSVPTEPathMessage path = resourceManager.getRSVPTEPathMessageFromPCEPResponse(lsp);
			//prox = (resourceManager.getProxHopIPv4List()).get(new LSPKey((resourceManager.getProxHopIPv4List()).keys().nextElement().getSourceAddress(), (resourceManager.getProxHopIPv4List()).keys().nextElement().getLspId()));
			prox = (resourceManager.getProxHopIPv4List()).get(new LSPKey(lsp.getIdSource(), lsp.getIdLSP()));
			timeEnd_Node=System.nanoTime();
			log.info("LSP Time to Process RSVP Path Mssg in Node (ms): "+((timeEnd_Node-timeIni_Node)/1000000) + " --> Sending RSVP path Message to "+prox.toString()+" !");
			sendRSVPMessage(path,prox);
		}
	}

	public void sendRSVPMessage(RSVPMessage msg,Inet4Address addr){
		if (managerRSVP!=null){
			managerRSVP.sendRSVPMessage(msg,addr);
		}
		else
			log.info("managerRSVP not initialized");
	}

	public void saveEroStats(ExplicitRouteObject ero){
		if(eroList==null){
			eroList = new LinkedList<ExplicitRouteObject>();
			eroList.add(ero);
		}else{
			eroList.add(ero);
		}
	}
	public String printEroList(){

		StringBuffer sb=new StringBuffer(2000);

		for(int i=0;i<eroList.size();i++){
			LinkedList<EROSubobject> erosublist = eroList.get(i).getEROSubobjectList();
			for(int j=0;j<erosublist.size();j++){
				if (erosublist.get(j).getType()==SubObjectValues.ERO_SUBOBJECT_IPV4PREFIX){
					sb.append(((IPv4prefixEROSubobject)erosublist.get(j)).getIpv4address().getHostAddress().toString()+"\t");
				}else if (erosublist.get(j).getType()==SubObjectValues.ERO_SUBOBJECT_UNNUMBERED_IF_ID){
					sb.append(((UnnumberIfIDEROSubobject)erosublist.get(j)).getRouterID().getHostAddress().toString()+"\t");
				}else if (erosublist.get(j).getType()==SubObjectValues.ERO_SUBOBJECT_LABEL){
					sb.append(((GeneralizedLabelEROSubobject) erosublist.get(j)).getDwdmWavelengthLabel().toString()+"\t");
				}
			}
			sb.append("\n");
		}
		return sb.toString();
	}

	public String printSRSREROList(){
		StringBuffer sb=new StringBuffer(2000);

		for(int i=0;i<SREROList.size();i++){
			sb.append("SID path: {");
			LinkedList<SREROSubobject> srerosublist = SREROList.get(i).getSREROSubobjectList();
			sb.append(srerosublist.get(0).getSID());
			for(int j=1;j<srerosublist.size();j++){
				sb.append(" --> "+srerosublist.get(j).getSID());

			}
			sb.append("}\n");
		}
		return sb.toString();
	}

	public void teardownLSP(LSPTE lsp){
		resourceManager.freeResources(lsp);
		RSVPPathTearMessage tear = new RSVPPathTearMessage();
		tear = resourceManager.getRSVPPathTearMessage(lsp);
		Inet4Address prox = resourceManager.getProxHopIPv4List().get(new LSPKey(lsp.getIdSource(), lsp.getIdLSP()));
		log.info("Sending RSVP PATH Tear Message to "+prox.toString());
		sendRSVPMessage(tear,prox);
	}

	/**
	 * Method to completely eliminate an LSP that has been established.
	 * @param idLSP The LSP identifier.
	 */

	public void killLSP(Long idLSP, Inet4Address src){
		log.info("Killing LSP");
		LSPTE lsp = LSPList.get(new LSPKey(src, idLSP));
		if(lsp == null){
			log.info("No LSP with this identifier");
		}else{
			LSPList.remove(new LSPKey(src, idLSP));
			log.info("LSP Killed");
		}
	}

	/**
	 * Method to eliminate all LSPs. It is meant to be used in case of ROADM safe shut down.
	 */    

	public void killAllLSP(){
		log.info("Killing All LSPs");
		Enumeration<LSPKey> e = LSPList.keys();
		while(e.hasMoreElements()){
			LSPList.remove(e.nextElement());
		}
	}

	/**
	 * This method shows on the screen all LSP information.
	 */
	public void showLSPList(PrintStream out){
		log.info("Showing LSPList");
		Enumeration<LSPKey> e = LSPList.keys();
		while(e.hasMoreElements()){
			LSPTE lsp = LSPList.get(e.nextElement());
			out.print("\nLSP id: "+lsp.getIdLSP()+"  ---->  Source: "+lsp.getIdSource().toString()+" - Destination: "+lsp.getIdDestination().toString());
		}
	}

	/**
	 * Method that resolves if there is any LSP with the same identifier as idLSP
	 * @param idLSP The LSP identifier.
	 * @return True in case of existing any LSP with this identifier, false if not.
	 */
	public boolean existLSP(Long idLSP, Inet4Address src){
		if(LSPList.get(new LSPKey(src, idLSP))!=null){
			return true;
		}else{
			return false;
		}
	}

	/**
	 * Method to add a new TE LSP from a previous Path message received
	 * @param lsp
	 * @param path
	 */
	public void forwardRSVPpath(LSPTE lsp,RSVPTEPathMessage path) throws LSPCreationException{
		log.info("Forwarding and Processing RSVP Path Message");
		int nodeType = LSPParameters.LSP_NODE_TYPE_TRANSIT;
		
		
		if (lsp==null)
			log.info("Cosa Mala...");
		if (lsp.getIdDestination()==null)
			log.info("IPDest is null");
		if (localIP==null)
			log.info("IPLocal is null");
		log.info("Comparando: "+lsp.getIdDestination().getHostAddress()+" y "+localIP.getHostAddress());
		if((lsp.getIdDestination().getHostAddress()).equals(localIP.getHostAddress())){
			nodeType = LSPParameters.LSP_NODE_TYPE_DESTINATION;
			log.info("New LSP, I am node Destination!");
		}
		/*
    	log.info("localIP.getHostAddress()::"+localIP.getHostAddress());
    	if(localIP.getHostAddress().equals("192.168.1.8"))
    	{

    	}

    	if(localIP.getHostAddress().equals("192.168.1.9"))
    	{
    		nodeType = LSPParameters.LSP_NODE_TYPE_DESTINATION;
    	}
		 */
		// DESTINATION NODE //

		if(nodeType == LSPParameters.LSP_NODE_TYPE_DESTINATION){
			//crear el RSPV RESV y enviarlo de vuelta
			RSVPTEResvMessage resv = new RSVPTEResvMessage();

			resv = resourceManager.getRSVPResvMessageFromDestination(path, lsp);

			LSPKey key = new LSPKey(lsp.getIdSource(), lsp.getIdLSP());

			//Guardamos el LSP en la lista
			LSPList.put(key, lsp);

			//Enviamos el mensaje
			if (resv != null){
				Inet4Address prox = resourceManager.getPreviousHopIPv4List().get(key);
				timeEnd_Node=System.nanoTime();
				log.info("LSP Time Path Node Process (ms): "+((timeEnd_Node-timeIni_Node)/1000000));
				log.info("Sending RSVP Resv message to "+prox.toString()+" !");
				sendRSVPMessage(resv,prox);
			}
			// TRANSIT NODE //
		}else if(nodeType == LSPParameters.LSP_NODE_TYPE_TRANSIT){
			log.info("New LSP, I am transit node");

			ERO eroRSVP = new ERO();
			LinkedList<EROSubobject> clone = (LinkedList<EROSubobject>) path.getEro().getEroSubobjects().clone();
			eroRSVP.setEroSubobjects(clone);
			lsp.setEro(eroRSVP);

			boolean check = resourceManager.checkResources(lsp);
			if (check == false){
				log.info("Error! No Resources in the Node!");
				throw new LSPCreationException(LSPCreationErrorTypes.NO_RESOURCES);
			}else{
				LSPKey key = new LSPKey(lsp.getIdSource(), lsp.getIdLSP());

				//Put the LSP in the list
				LSPList.put(key, lsp);

				RSVPTEPathMessage NewPath = resourceManager.forwardRSVPpath(lsp, path);
				timeEnd_Node=System.nanoTime();
				log.info("LSP Time to Process RSVP Path in Node (ms): "+((timeEnd_Node-timeIni_Node)/1000000));
				Inet4Address prox = (resourceManager.getProxHopIPv4List()).get(key);
				log.info("Sending the RSVP PATH message to "+prox.toString()+" !");
				sendRSVPMessage(NewPath,prox);
			}
		}
	}

	public void updateLSP(PCEPUpdate pupdt) 
	{
		//There should be a better way to do this, but for the time being is OK
		log.info("Updating LSP!");
		Inet4Address addres = pupdt.getUpdateRequestList().get(0).getLSP().getLspIdentifiers_tlv().getTunnelSenderIPAddress();

		for (int i = 0; i < pupdt.getUpdateRequestList().size(); i++)
		{
			log.info("Address: "+ addres);
			log.info("lspID: "+ pupdt.getUpdateRequestList().get(i).getLSP().getLspId());

			final LSPTE previous = LSPList.get(new LSPKey(addres,pupdt.getUpdateRequestList().get(i).getLSP().getLspId()));

			if ((previous == null) ||(!previous.isDelegated()) || (!(previous.getDelegatedAdress().equals(PCESession.getPeerPCE_IPaddress()))))
			{
				log.warning("An align PCE is trying to delegate on us or the LSP to be updated was not found:"+(previous == null));
				log.info("PCEPErr message should be sent");
			}
			else
			{
				if (pupdt.getUpdateRequestList().get(i).getLSP().isrFlag())
				{
					log.info("Removing LSP due to PCEPUpdate received message");
					dataBaseVersion.incrementAndGet();
					deleteLSP(addres, pupdt.getUpdateRequestList().get(i).getLSP().getLspId());
					notiLSP.notify(previous, false, false, true, false, getPCESession().getOut());
				}
				else
				{
					log.info("Adding LSP due to PCEPUpdate received message");
					final Path path = pupdt.getUpdateRequestList().get(i).getPath();

					log.info("previous.getIdDestination()"+previous.getIdDestination());
					float bw=0;
					Bandwidth bww=path.getBandwidth();
					if (bww!=null){
						if (bww instanceof BandwidthRequested) {
							bw=((BandwidthRequested)bww).getBw();
						}
					}
					final LSPTE lsp = new LSPTE(previous.getTunnelId(), previous.getIdSource(), previous.getIdDestination(), 
							previous.isBidirectional(), previous.getOFcode(),bw, previous.getPathState());

					ERO ero = new ERO();
					ero.setEroSubobjects(path.geteRO().getEROSubobjectList());
					lsp.setEro(ero);
					dataBaseVersion.incrementAndGet();
					deleteLSP(addres, pupdt.getUpdateRequestList().get(i).getLSP().getLspId());
					log.info("previous.getIdDestination()" + previous.getIdDestination());
					log.info(" path.getBandwidth().getBw()" +  bw);

					class ThreadAux extends Thread
					{
						@Override
						public void run()
						{
							try 
							{
								sleep(2000);
								float bw=0;
								Bandwidth bww=path.getBandwidth();
								if (bww!=null){
									if (bww instanceof BandwidthRequested) {
										bw=((BandwidthRequested)bww).getBw();
									}
								}
								addnewLSP(previous.getIdDestination(), bw, 
										previous.isBidirectional(), previous.getOFcode(),lsp.getIdLSP().intValue());

								waitForLSPaddition(lsp.getIdLSP().intValue(), 10000);
								if (getLSP(lsp.getIdLSP().intValue(), previous.getIdSource()) == null)
								{
									log.info("Error creating LSP!!");
								}
							}
							catch (InterruptedException e) 
							{
								log.warning("Thread interrupted during the updating of a LSP");
								e.printStackTrace();
							}
							catch (LSPCreationException e) 
							{
								log.warning("Error updating LSP");
								e.printStackTrace();
							}
						}
					};

					new ThreadAux().start();
					notiLSP.notify(lsp, true, true, false, false, getPCESession().getOut());
				}
			}
		}
	}

	/**
	 * 
	 * @param idLSP
	 * @return
	 */
	public synchronized long getIdNewLSP() {
		LSPManager.idNewLSP=LSPManager.idNewLSP+1;
		long newLSP=LSPManager.idNewLSP;
		if (LSPManager.idNewLSP>=Integer.MAX_VALUE){
			LSPManager.idNewLSP=0;
		}
		return newLSP;
	}
	public LSPTE getLSP(long id, Inet4Address src){
		return LSPList.get(new LSPKey(src, id));
	}
	public RSVPManager getManagerRSVP() {
		return managerRSVP;
	}
	public void setManagerRSVP(RSVPManager managerRSVP) {
		this.managerRSVP = managerRSVP;
	}
	public Logger getLog() {
		return log;
	}
	public void setLog(Logger log) {
		this.log = log;
	}
	public ResourceManager getResourceManager() {
		return resourceManager;
	}
	public void setResourceManager(ResourceManager resourceManager) {
		this.resourceManager = resourceManager;
	}
	public Hashtable<LSPKey, LSPTE> getLSPList() {
		return LSPList;
	}

	public Inet4Address getLocalIP() {
		return localIP;
	}

	public void setLocalIP(Inet4Address localIP) {
		this.localIP = localIP;
	}

	public boolean isEstablished() {
		return established;
	}

	public void setEstablished(boolean established) {
		this.established = established;
	}

	public long getTimeIni() {
		return timeIni;
	}

	public void setTimeIni(long timeIni) {
		this.timeIni = timeIni;
	}

	public long getTimeIni_Node() {
		return timeIni_Node;
	}

	public void setTimeIni_Node(long timeIniNode) {
		timeIni_Node = timeIniNode;
	}

	public long getTimeEnd() {
		return timeEnd;
	}

	public void setTimeEnd(long timeEnd) {
		this.timeEnd = timeEnd;
	}

	public long getTimeEnd_Node() {
		return timeEnd_Node;
	}

	public void setTimeEnd_Node(long timeEndNode) {
		timeEnd_Node = timeEndNode;
	}

	public DataOutputStream getOut() {
		return out;
	}

	public void setOut(DataOutputStream out) {
		this.out = out;
	}

	public int getDataBaseVersion(){
		return dataBaseVersion.get();
	}

	public int getNextdataBaseVersion(){
		return dataBaseVersion.incrementAndGet() ;
	}
	
	public void setDataBaseVersion(int dbv){
		dataBaseVersion.set(dbv);
	}

	public long getSymbolicPatheIdentifier(){
		return symbolicPathIdentifier.get();
	}

	public long getNextSymbolicPatheIdentifier(){
		return symbolicPathIdentifier.incrementAndGet() ;
	}

	public PCCPCEPSession getPCESession() {
		return PCESession;
	}

	public void setPCESession(PCCPCEPSession pCESession) {
		PCESession = pCESession;
	}

	public NotifyLSP getNotiLSP() {
		return notiLSP;
	}

	public void setNotiLSP(NotifyLSP notiLSP) {
		this.notiLSP = notiLSP;
	}

	public boolean isStateful() {
		return isStateful;
	}

	public void setStateful(boolean isStateful) {
		this.isStateful = isStateful;
	}

	public FastPCEPSession getFastSession() {
		return fastSession;
	}

	public void setFastSession(FastPCEPSession fastSession) {
		this.fastSession = fastSession;
	}

	public ReportDB getRptdb() {
		return rptdb;
	}

	public void setRptdb(ReportDB lspdb) {
		this.rptdb = lspdb;
	}

}
