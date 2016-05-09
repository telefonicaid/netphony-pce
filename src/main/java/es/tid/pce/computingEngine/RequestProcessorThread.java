package es.tid.pce.computingEngine;

import es.tid.pce.computingEngine.algorithms.*;
import es.tid.pce.computingEngine.algorithms.multiLayer.OperationsCounter;
import es.tid.pce.pcep.PCEPProtocolViolationException;
import es.tid.pce.pcep.constructs.*;
import es.tid.pce.pcep.messages.PCEPError;
import es.tid.pce.pcep.messages.PCEPNotification;
import es.tid.pce.pcep.messages.PCEPRequest;
import es.tid.pce.pcep.messages.PCEPResponse;
import es.tid.pce.pcep.objects.*;
import es.tid.pce.pcep.objects.tlvs.NoPathTLV;
import es.tid.pce.pcep.objects.tlvs.PathReservationTLV;
import es.tid.pce.server.ParentPCERequestManager;
import es.tid.pce.server.communicationpce.CollaborationPCESessionManager;
import es.tid.pce.server.wson.ReservationManager;
import es.tid.rsvp.objects.subobjects.EROSubobject;
import es.tid.rsvp.objects.subobjects.IPv4prefixEROSubobject;
import es.tid.rsvp.objects.subobjects.UnnumberIfIDEROSubobject;
import es.tid.tedb.DomainTEDB;
import es.tid.tedb.TEDB;
import es.tid.util.Analysis;
import es.tid.util.UtilsFunctions;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

//import tid.pce.pcep.objects.tlvs.NotificationTLV;
//import tid.pce.pcep.objects.tlvs.subtlvs.NotificationSubTLV;

/**
 * RequestProcessorThread is devoted to Path Computation
 * It chooses the right algorithm based on the Algorithm rules
 * and parameters of the requests.
 * Algorithms can be registered and deleted on demand
 * @author ogondio
 *
 */
public class RequestProcessorThread extends Thread{
	private Analysis idleTime;
	private Analysis procTime;
	/**
	 * The queue to read the requests
	 */
	private LinkedBlockingQueue<ComputingRequest> pathComputingRequestQueue;

	private Hashtable<Inet4Address,DomainTEDB> intraTEDBs;

	private LinkedBlockingQueue<ComputingRequest> pathComputingRequestRetryQueue;

	/**
	 * Indicates whether the thread is running
	 */
	private boolean running;

	/**
	 * List of algorithms for individual requests 
	 */
	private Hashtable<Integer,ComputingAlgorithmManager> singleAlgorithmList;

	/**
	 * List of algorithms for individual requests 
	 */
	private Hashtable<Integer,ComputingAlgorithmManagerSSON> singleAlgorithmListsson;

	/**
	 * List of algorithms for synchronized requests
	 */
	private Hashtable<Integer,ComputingAlgorithmManager> svecAlgorithmList;

	/**
	 * List of algorithms for synchronized requests
	 */
	private Hashtable<Integer,ComputingAlgorithmManagerSSON> svecAlgorithmListsson;

	/**
	 * Logger
	 */
	private Logger log;

	/**
	 * Traffic engineering database (NOTE it is generic)
	 */
	private TEDB ted;

	/**
	 * The computing task being executed. in case it needs to be canceled externally
	 */
	private ComputingTask ft;

	/**
	 * Used to send requests to child PCEs from the Parent PCE. Only used in H-PCE.
	 */
	private ParentPCERequestManager cpcerm;

	/**
	 * The source can be also extracted from a Generalized EndPoint
	 */
	private Inet4Address source;
	private Inet4Address dest;

	private OperationsCounter opCounter;

	private boolean isChildPCE;

	//private boolean isCompleteAuxGraph;

	private boolean isMultilayer=false;

	//private PrintWriter pw;
	private boolean analyzeRequestTime;

	private boolean useMaxReqTime;

	private double maxProcTime;

	private long numReqProcessed=0;
	private long numAnswersSent=0;
	private long numNoPathOLSent=0;

	private ReservationManager reservationManager;

	/* STRONGEST: Collaborative PCEs */
	CollaborationPCESessionManager collaborationPCESessionManager;


	/*
	 * Constructor
	 * @param queue
	 * @param ted
	 * @param cpcerm
	 * @param pathComputingRequestRetryQueue
	 * @param analyzeRequestTime
	 */
	public RequestProcessorThread(LinkedBlockingQueue<ComputingRequest> queue,TEDB ted,ParentPCERequestManager cpcerm, LinkedBlockingQueue<ComputingRequest> pathComputingRequestRetryQueue, boolean analyzeRequestTime){
		useMaxReqTime=false;
		this.pathComputingRequestQueue=queue;
		running=true;
		this.ted=ted;
		log=Logger.getLogger("PCEServer");
		singleAlgorithmList=new Hashtable<Integer,ComputingAlgorithmManager>();
		svecAlgorithmList=new Hashtable<Integer,ComputingAlgorithmManager>();
		singleAlgorithmListsson=new Hashtable<Integer,ComputingAlgorithmManagerSSON>();
		this.cpcerm=cpcerm;
		if (cpcerm!=null){
			this.isChildPCE=true;	
		}
		this.pathComputingRequestRetryQueue=pathComputingRequestRetryQueue;

		if (analyzeRequestTime){
			idleTime=new Analysis();
			procTime=new Analysis();
			this.analyzeRequestTime=analyzeRequestTime;
		}else {
			analyzeRequestTime=false;
		}
		//		try {
		//			pw= new PrintWriter("Test");
		//		} catch (FileNotFoundException e) {
		//			// TODO Auto-generated catch block
		//			e.printStackTrace();
		//		}

	}

	/*
	 * Constructor 
	 * @param queue
	 * @param ted
	 */
	public RequestProcessorThread(LinkedBlockingQueue<ComputingRequest> queue,TEDB ted,ParentPCERequestManager cpcerm, LinkedBlockingQueue<ComputingRequest> pathComputingRequestRetryQueue, boolean analyzeRequestTime, boolean useMaxReqTime, ReservationManager reservationManager){
		this.pathComputingRequestQueue=queue;
		running=true;
		this.ted=ted;
		log=Logger.getLogger("PCEServer");	
		singleAlgorithmList=new Hashtable<Integer,ComputingAlgorithmManager>();
		svecAlgorithmList=new Hashtable<Integer,ComputingAlgorithmManager>();
		singleAlgorithmListsson=new Hashtable<Integer,ComputingAlgorithmManagerSSON>();
		svecAlgorithmListsson=new Hashtable<Integer,ComputingAlgorithmManagerSSON>();
		this.cpcerm=cpcerm;
		if (cpcerm!=null){
			this.isChildPCE=true;	
		}
		this.pathComputingRequestRetryQueue=pathComputingRequestRetryQueue;

		if (analyzeRequestTime){
			idleTime=new Analysis();
			procTime=new Analysis();
			this.analyzeRequestTime=analyzeRequestTime;
			maxProcTime=0;
		}else {
			analyzeRequestTime=false;
		}
		//		try {
		//			pw= new PrintWriter("Test");
		//		} catch (FileNotFoundException e) {
		//			// TODO Auto-generated catch block
		//			e.printStackTrace();
		//		}
		this.useMaxReqTime=useMaxReqTime;
		this.reservationManager=reservationManager;
		if (useMaxReqTime==true){
			log.info("USING MAX REQ TIME");
		}

	}

	/*
	 * SERGIO
	 * Constructor 
	 * @param queue
	 * @param ted
	 */
	public RequestProcessorThread(LinkedBlockingQueue<ComputingRequest> queue,TEDB ted,ParentPCERequestManager cpcerm, LinkedBlockingQueue<ComputingRequest> pathComputingRequestRetryQueue, boolean analyzeRequestTime, boolean useMaxReqTime, ReservationManager reservationManager, OperationsCounter OPcounter, boolean isMult){
		this.pathComputingRequestQueue=queue;
		running=true;
		this.ted=ted;
		log=Logger.getLogger("PCEServer");	
		singleAlgorithmList=new Hashtable<Integer,ComputingAlgorithmManager>();
		svecAlgorithmList=new Hashtable<Integer,ComputingAlgorithmManager>();
		singleAlgorithmListsson=new Hashtable<Integer,ComputingAlgorithmManagerSSON>();
		svecAlgorithmListsson=new Hashtable<Integer,ComputingAlgorithmManagerSSON>();
		this.cpcerm=cpcerm;
		this.opCounter=OPcounter;
		this.isMultilayer=isMult;
		if (cpcerm!=null){
			this.isChildPCE=true;	
		}
		this.pathComputingRequestRetryQueue=pathComputingRequestRetryQueue;

		if (analyzeRequestTime){
			idleTime=new Analysis();
			procTime=new Analysis();
			this.analyzeRequestTime=analyzeRequestTime;
			maxProcTime=0;
		}else {
			analyzeRequestTime=false;
		}
		//		try {
		//			pw= new PrintWriter("Test");
		//		} catch (FileNotFoundException e) {
		//			// TODO Auto-generated catch block
		//			e.printStackTrace();
		//		}
		this.useMaxReqTime=useMaxReqTime;
		this.reservationManager=reservationManager;
		if (useMaxReqTime==true){
			log.info("USING MAX REQ TIME");
		}

	}

	/**
	 * Constructor
	 * @param queue
	 * @param ted
	 * @param cpcerm
	 * @param pathComputingRequestRetryQueue
	 * @param analyzeRequestTime
	 * @param intraTEDBs
	 */
	public RequestProcessorThread(LinkedBlockingQueue<ComputingRequest> queue,TEDB ted,ParentPCERequestManager cpcerm, LinkedBlockingQueue<ComputingRequest> pathComputingRequestRetryQueue, boolean analyzeRequestTime, Hashtable<Inet4Address,DomainTEDB> intraTEDBs){
		useMaxReqTime=false;
		this.pathComputingRequestQueue=queue;
		running=true;
		this.ted=ted;
		log=Logger.getLogger("PCEServer");
		singleAlgorithmList=new Hashtable<Integer,ComputingAlgorithmManager>();
		svecAlgorithmList=new Hashtable<Integer,ComputingAlgorithmManager>();
		singleAlgorithmListsson=new Hashtable<Integer,ComputingAlgorithmManagerSSON>();
		this.cpcerm=cpcerm;
		if (cpcerm!=null){
			this.isChildPCE=true;
		}
		this.intraTEDBs = intraTEDBs;
		this.pathComputingRequestRetryQueue=pathComputingRequestRetryQueue;

		if (analyzeRequestTime){
			idleTime=new Analysis();
			procTime=new Analysis();
			this.analyzeRequestTime=analyzeRequestTime;
		}else {
			analyzeRequestTime=false;
		}
		//		try {
		//			pw= new PrintWriter("Test");
		//		} catch (FileNotFoundException e) {
		//			// TODO Auto-generated catch block
		//			e.printStackTrace();
		//		}

	}



	/*
	 * Constructor para collaborative PCEs
	 * @param queue
	 * @param ted
	 */
	public RequestProcessorThread(LinkedBlockingQueue<ComputingRequest> queue,TEDB ted,ParentPCERequestManager cpcerm, LinkedBlockingQueue<ComputingRequest> pathComputingRequestRetryQueue, boolean analyzeRequestTime,CollaborationPCESessionManager collaborationPCESessionManager){
		useMaxReqTime=false;
		this.pathComputingRequestQueue=queue;
		running=true;
		this.ted=ted;
		log=Logger.getLogger("PCEServer");	
		singleAlgorithmList=new Hashtable<Integer,ComputingAlgorithmManager>();
		svecAlgorithmList=new Hashtable<Integer,ComputingAlgorithmManager>();
		this.cpcerm=cpcerm;
		if (cpcerm!=null){
			this.isChildPCE=true;	
		}
		this.pathComputingRequestRetryQueue=pathComputingRequestRetryQueue;

		if (analyzeRequestTime){
			idleTime=new Analysis();
			procTime=new Analysis();
			this.analyzeRequestTime=analyzeRequestTime;
		}else {
			analyzeRequestTime=false;
		}
		//		try {
		//			pw= new PrintWriter("Test");
		//		} catch (FileNotFoundException e) {
		//			// TODO Auto-generated catch block
		//			e.printStackTrace();
		//		}

		this.collaborationPCESessionManager=collaborationPCESessionManager;
	}

	/*
	 * Constructor para collaborative PCEs
	 * @param queue
	 * @param ted
	 */
	public RequestProcessorThread(LinkedBlockingQueue<ComputingRequest> queue,TEDB ted,ParentPCERequestManager cpcerm, LinkedBlockingQueue<ComputingRequest> pathComputingRequestRetryQueue, boolean analyzeRequestTime, boolean useMaxReqTime, ReservationManager reservationManager,CollaborationPCESessionManager collaborationPCESessionManager){
		this.pathComputingRequestQueue=queue;
		running=true;
		this.ted=ted;
		log=Logger.getLogger("PCEServer");
		singleAlgorithmList=new Hashtable<Integer,ComputingAlgorithmManager>();
		svecAlgorithmList=new Hashtable<Integer,ComputingAlgorithmManager>();
		singleAlgorithmListsson=new Hashtable<Integer,ComputingAlgorithmManagerSSON>();
		this.cpcerm=cpcerm;
		if (cpcerm!=null){
			this.isChildPCE=true;	
		}
		this.pathComputingRequestRetryQueue=pathComputingRequestRetryQueue;

		if (analyzeRequestTime){
			idleTime=new Analysis();
			procTime=new Analysis();
			this.analyzeRequestTime=analyzeRequestTime;
			maxProcTime=0;
		}else {
			analyzeRequestTime=false;
		}
		//		try {
		//			pw= new PrintWriter("Test");
		//		} catch (FileNotFoundException e) {
		//			// TODO Auto-generated catch block
		//			e.printStackTrace();
		//		}
		this.useMaxReqTime=useMaxReqTime;
		this.reservationManager=reservationManager;
		if (useMaxReqTime==true){
			log.info("USING MAX REQ TIME");
		}

		this.collaborationPCESessionManager=collaborationPCESessionManager;
	}


	/**
	 * Method to register an algorithm
	 * @param rule -
	 * @param algortithmManager -
	 */
	public void registerAlgorithm(AlgorithmRule rule, ComputingAlgorithmManager algortithmManager ){
		if (rule.svec==true){
			svecAlgorithmList.put(new Integer(rule.of), algortithmManager);
		}
		else {
			singleAlgorithmList.put(new Integer(rule.of), algortithmManager);
		}

	}

	/**
	 * Method to register an algorithm
	 * @param rule -
	 * @param algortithmManager -
	 */
	public void registerAlgorithmSSON(AlgorithmRule rule, ComputingAlgorithmManagerSSON algortithmManager ){
		if (rule.svec==true){
			svecAlgorithmListsson.put(new Integer(rule.of), algortithmManager);
		}
		else {
			singleAlgorithmListsson.put(new Integer(rule.of), algortithmManager);
		}
	}
	/**
	 * Method to delete an algorithm
	 * @param rule - 
	 * @param algortithmManager -
	 */
	public void removeAlgorithm(AlgorithmRule rule, ComputingAlgorithmManager algortithmManager ){		

		singleAlgorithmList.remove(new Integer(rule.of));
		svecAlgorithmList.remove(new Integer(rule.of));
	}

	/**
	 * The "core" of the Request Processor
	 */
	public void run(){	
		ComputingRequest pathCompReq;
		long timeIniNanos;
		long timeEndNanos;
		long timePreNanos=System.nanoTime();
		while (running) {
			log.info("Waiting for a new Computing Request to process");
			try {
				pathCompReq=pathComputingRequestQueue.take();

				if (analyzeRequestTime){
					double idleTimeV=(System.nanoTime()-timePreNanos)/(double)1000000;
					if (idleTimeV<20000){
						idleTime.analyze(idleTimeV);	
					}	
				}

			} catch (InterruptedException e) {
				log.warning("There is no path to compute");
				e.printStackTrace();
				break;
			}
			timeIniNanos=System.nanoTime();

			if (pathCompReq.getRequestList().size()==1){
				log.info("Processing New Path Computing request, id: "+pathCompReq.getRequestList().get(0).toString());	
			}
			//FIXME: ESTA PARTE PUEDE FALLAR SI MANDAN OTRA COSA QUE NO SEAN IPV4 o GEN END POINTS
			//POR AHORA PONGO TRY CATH Y MANDO NOPATH
			long sourceIF=0;
			long destIF=0;
			
			
			
			
			
			P2PEndpoints p2pep=null;
			
			
			
			
			
			try{
				//For the IT case
				if (ted.isITtedb()){
					log.info("Processing New Path Computing request, id: "+pathCompReq.getRequestList().get(0).toString());		
					source =(((GeneralizedEndPoints)pathCompReq.getRequestList().get(0).getEndPoints()).getP2PEndpoints().getSourceEndPoint().getEndPointIPv4TLV().getIPv4address());
					dest =(((GeneralizedEndPoints)pathCompReq.getRequestList().get(0).getEndPoints()).getP2PEndpoints().getDestinationEndPoint().getEndPointIPv4TLV().getIPv4address());
				}else {
					try {  //EndPointsIPv4
						if (pathCompReq.getRequestList().get(0).getEndPoints() instanceof GeneralizedEndPoints){
							source = ((EndPointsUnnumberedIntf)pathCompReq.getRequestList().get(0).getEndPoints()).getSourceIP();
							dest = ((EndPointsUnnumberedIntf)pathCompReq.getRequestList().get(0).getEndPoints()).getDestIP();
							sourceIF=((EndPointsUnnumberedIntf)pathCompReq.getRequestList().get(0).getEndPoints()).getSourceIF();
							destIF=((EndPointsUnnumberedIntf)pathCompReq.getRequestList().get(0).getEndPoints()).getDestIF();
							log.info("SubObjeto: EP-Unnumbered Interface: "+((EndPointsUnnumberedIntf)pathCompReq.getRequestList().get(0).getEndPoints()).toString());
							EndPointsIPv4 ep= new EndPointsIPv4();
							ep.setDestIP(dest);
							ep.setSourceIP(source);
							pathCompReq.getRequestList().get(0).setEndPoints(ep);
						}
						source = ((EndPointsIPv4)pathCompReq.getRequestList().get(0).getEndPoints()).getSourceIP();
						dest = ((EndPointsIPv4)pathCompReq.getRequestList().get(0).getEndPoints()).getDestIP();
						log.info(" XXXX try source: "+source);
						log.info(" XXXX try dest: "+dest);
						
					} catch (Exception e) {  //GeneralizedEndPoints
						if (pathCompReq.getRequestList().get(0).getEndPoints() instanceof GeneralizedEndPoints){
							
							p2pep = ((GeneralizedEndPoints)pathCompReq.getRequestList().get(0).getEndPoints()).getP2PEndpoints();			

							//P2PEndpoints p2pep = ((GeneralizedEndPoints)pathCompReq.getRequestList().get(0).getEndPoints()).getP2PEndpoints();			
							log.info("RequestProcessorThread GeneralizedEndPoints -> sourceDataPath:: "+p2pep.getSourceEndPoint()+" destDataPath :: "+p2pep.getDestinationEndPoint());

							GeneralizedEndPoints ep= new GeneralizedEndPoints();
							ep.setP2PEndpoints(p2pep); 	
							pathCompReq.getRequestList().get(0).setEndPoints(ep);
							
							source = p2pep.getSourceEndPoint().getEndPointIPv4TLV().getIPv4address();
							dest = p2pep.getDestinationEndPoint().getEndPointIPv4TLV().getIPv4address();
						}
					}
				}
			}catch (Exception e){
				//If fails, we send NoPath, by now (reasons later...)
				//FIXME
				log.info("Shouldn't be here except in WLANs");
				//log.info(FuncionesUtiles.exceptionToString(e));
				//this.sendNoPath(pathCompReq);
			}
			//In case it is a child PCE with a parent, requestToParent = true
			boolean requestToParent = false;
		
			if (this.isChildPCE==true){
				//Before sending to the parent, check that the source and destinations don't belong to the domain
				
				if((!(((DomainTEDB)ted).belongsToDomain(source))||(!(((DomainTEDB)ted).belongsToDomain(dest))))){					
					requestToParent = true;
				}
				
				
				
			}
			//In case we need to send the request to the parent... this way...
			if (requestToParent == true) {
				log.info("Child PCE: Request is going to be fowarded to the Parent PCE");
				PCEPRequest pcreq = new PCEPRequest();
				Request request=pathCompReq.getRequestList().get(0).duplicate();
				//FIXME: hay que poner un nuevo requestID, si no... la podemos liar
				pcreq.addRequest(request);
				PCEPResponse p_rep = cpcerm.newRequest(pcreq);


				if (p_rep==null){
					log.warning("Parent doesn't answer");
					this.sendNoPath(pathCompReq);
				}else {
					log.info("RESP: "+p_rep.toString());
				}

				ComputingResponse pcepresp =  new ComputingResponse();
				pcepresp.setResponsetList(p_rep.getResponseList());
				try 
				{
					log.info("Encoding Computing Request");
					pcepresp.encode();
				} 
				catch (PCEPProtocolViolationException e1)
				{
					log.info(UtilsFunctions.exceptionToString(e1));
				}


				try {
					log.info("oNE OF THE NODES IS NOT IN THE DOMAIN. Send Request to parent PCE,pcepresp:"+pcepresp+",pathCompReq.getOut():"+pathCompReq.getOut());
					pathCompReq.getOut().write(p_rep.getBytes());
					pathCompReq.getOut().flush();
				} catch (IOException e) {
					log.warning("Parent doesn't answer");
					ComputingResponse m_resp=new ComputingResponse();
					Response response=new Response();
					RequestParameters rp = new RequestParameters();
					rp.setRequestID(request.getRequestParameters().requestID);
					response.setRequestParameters(rp);
					NoPath noPath= new NoPath();
					noPath.setNatureOfIssue(ObjectParameters.NOPATH_NOPATH_SAT_CONSTRAINTS);
					NoPathTLV noPathTLV=new NoPathTLV();
					noPath.setNoPathTLV(noPathTLV);				
					response.setNoPath(noPath);
					m_resp.addResponse(response);
					try {
						m_resp.encode();
						pathCompReq.getOut().write(m_resp.getBytes());
						pathCompReq.getOut().flush();
					} catch (IOException e2) {
						// TODO Auto-generated catch block
						e2.printStackTrace();
					} catch (PCEPProtocolViolationException e3) {
						// TODO Auto-generated catch block
						e3.printStackTrace();
					}
					log.info("Send NO PATH");
					System.exit(0);
				}
				log.info("Response sent!!");
				//	}
				//}
				ft=null;

			} else {
				int of=0;
				ComputingAlgorithmManager algortithmManager=null;
				ComputingAlgorithmManagerSSON algortithmManagerSSON=null;
				if (pathCompReq.getSvec()!=null){
					log.info("SVEC Request ");
					ObjectiveFunction objectiveFunctionObject=pathCompReq.getSvec().getObjectiveFunction();
					if (objectiveFunctionObject!=null){
						of=objectiveFunctionObject.getOFcode();
						log.info("ObjectiveFunction code "+of);
						algortithmManager =svecAlgorithmList.get(new Integer(of));
						if (algortithmManager==null){
							if (objectiveFunctionObject.isPbit()==true){
								log.warning("OF not supported");
								//Send error
								PCEPError msg_error= new PCEPError();
								ErrorConstruct error_c=new ErrorConstruct();
								PCEPErrorObject error= new PCEPErrorObject();
								error.setErrorType(ObjectParameters.ERROR_UNSUPPORTEDOBJECT);
								error.setErrorValue(ObjectParameters.ERROR_UNSUPPORTEDOBJECT_UNSUPPORTED_PARAMETER);
								error_c.getErrorObjList().add(error);
								msg_error.setError(error_c);
								try {
									msg_error.encode();
									pathCompReq.getOut().write(msg_error.getBytes());
									pathCompReq.getOut().flush();
								} catch (IOException e) {
									log.warning("IOException sending error to PCC: "+pathCompReq.getRequestList().get(0).toString());
									e.printStackTrace();
									break;								
								} catch (PCEPProtocolViolationException e) {
									log.severe("Malformed ERROR MESSAGE, CHECK PCE CODE:"+pathCompReq.getRequestList().get(0).toString());
									e.printStackTrace();
									break;	
								}
								break;
							}else {
								log.warning("USING Default SVEC ");
								DefaultSVECPathComputing dspc=new DefaultSVECPathComputing(pathCompReq,ted);
								ft=new ComputingTask(dspc);	
							}
						}else {
							log.info("Custom SVEC OF "+of);
							ComputingAlgorithm cpr=algortithmManager.getComputingAlgorithm(pathCompReq, ted);
							ft=new ComputingTask(cpr);
						}						
					}
					else {
						log.info("Default SVEC ");
						DefaultSVECPathComputing dspc=new DefaultSVECPathComputing(pathCompReq,ted);
						ft=new ComputingTask(dspc);	

					}


				}//aqui se acaba el de svec!=null
				else {
					boolean nopath=false;
					log.fine("Non-svec request");
					double totalTimeNs=System.nanoTime()-pathCompReq.getTimeStampNs();
					double totalTimeMs=totalTimeNs/1000000L;
					if (useMaxReqTime==true){
						if (totalTimeMs>pathCompReq.getMaxTimeInPCE()){
							log.info("Request execeeded time, sending nopath");
							ft=null;
							log.info("Mando  no path request execeeded time.totalTimeMs "+totalTimeMs+"pathCompReq.getMaxTimeInPCE()");
							sendNoPath(pathCompReq);
							nopath=true;
						}	
					}

					if (nopath==false){
						ObjectiveFunction objectiveFunctionObject=pathCompReq.getRequestList().get(0).getObjectiveFunction();
						if (objectiveFunctionObject!=null){    				
							of=objectiveFunctionObject.getOFcode();

							log.fine("ObjectiveFunction code "+of);
							algortithmManager =singleAlgorithmList.get(new Integer(of));
							if (singleAlgorithmListsson != null){
								algortithmManagerSSON = singleAlgorithmListsson.get(new Integer(of));
								
							}
							
							if (algortithmManager==null && algortithmManagerSSON==null){
								if (objectiveFunctionObject.isPbit()==true){
									log.warning("OF not supported!!");
									//Send error
									PCEPError msg_error= new PCEPError();
									ErrorConstruct error_c=new ErrorConstruct();
									PCEPErrorObject error= new PCEPErrorObject();
									error.setErrorType(ObjectParameters.ERROR_UNSUPPORTEDOBJECT);
									error.setErrorValue(ObjectParameters.ERROR_UNSUPPORTEDOBJECT_UNSUPPORTED_PARAMETER);
									error_c.getErrorObjList().add(error);
									error_c.getRequestIdList().add(pathCompReq.getRequestList().get(0).getRequestParameters());
									msg_error.setError(error_c);
									try {
										msg_error.encode();
										pathCompReq.getOut().write(msg_error.getBytes());
										pathCompReq.getOut().flush();
									} catch (IOException e) {
										log.warning("IOException sending error to PCC: nons"+pathCompReq.getRequestList().get(0).toString());
										e.printStackTrace();																	
									} catch (PCEPProtocolViolationException e) {
										log.severe("Malformed ERROR MESSAGE, CHECK PCE CODE. nons"+pathCompReq.getRequestList().get(0).toString());
										e.printStackTrace();											
									}
									nopath=true;
									ft=null;
									log.warning("error message informing sent."+pathCompReq.getRequestList().get(0).toString());
								}
								else {
									log.info("Choosing default algotithm 1");
									log.info("pathCompReq:: "+pathCompReq.toString());
									//log.info("ted:: "+ted.printTopology());
									DefaultSinglePathComputing dspc=new DefaultSinglePathComputing(pathCompReq,ted);
									ft=new ComputingTask(dspc);
								}
						}else {
								log.info("Choosing algorithm of OF "+of);
								boolean ssonAlgorithm = false;
								if (singleAlgorithmListsson != null){
									if (singleAlgorithmListsson.size()!=0){
										ssonAlgorithm = true;
										//FIXME: Hay que declarar el parametro "modulation format".
										int mf=0;
										ComputingAlgorithm cpr=algortithmManagerSSON.getComputingAlgorithm(pathCompReq, ted, mf);
										ft=new ComputingTask(cpr);
									}
								}
								if (!ssonAlgorithm){
									if (isMultilayer==true){
										ComputingAlgorithm cpr=algortithmManager.getComputingAlgorithm(pathCompReq, ted, opCounter);
										ft=new ComputingTask(cpr);										
									}else{
										ComputingAlgorithm cpr=algortithmManager.getComputingAlgorithm(pathCompReq, ted);
										ft=new ComputingTask(cpr);
									}

								}
							}
						}
						else {
							log.info("Choosing default algotithm 2");
							DefaultSinglePathComputing dspc=new DefaultSinglePathComputing(pathCompReq,ted);
							ft=new ComputingTask(dspc);
						}
					}

				}
			}
			if (ft!=null)	{
				//Here the task will be executed. n
				ComputingResponse rep;
				try {
					ft.run();
					rep=ft.get(pathCompReq.getMaxTimeInPCE(),TimeUnit.MILLISECONDS);
					
				}
				catch(Exception e){
					log.warning("Computation failed: "+e.getMessage()+" || "+UtilsFunctions.exceptionToString(e)+"  || " +",MAXTIME: "+pathCompReq.getMaxTimeInPCE());
					rep=null;
				}
				log.info("ReppPP:::"+rep);
				//FIXME: There's a trap here. We change Response to send an unnumbered interface
				if ((sourceIF!=0)&&(destIF!=0))//Esto ocurre en el caso de recibir UnnumberedInterface EndPoints (Caso VNTM)
					trappingResponse(rep, sourceIF, destIF);
				try {
					//FIXME: WE ARE USING THE MAX TIME IN PCE, REGARDLESS THE TIME IN THE PCE
					//log.severe("Esperamos "+pathCompReq.getMaxTimeInPCE());
					//FIXME: 				
					if (rep!=null){
						//log.info("rep.getPathList().get(0)"+rep.getResponse(0).getPathList().get(0));
						ComputingResponse repRes=ft.executeReservation();
						if (repRes!=null){
							rep=repRes;							
						}
						timeEndNanos=System.nanoTime();

						double compTimeMicroSec=(timeEndNanos-timeIniNanos)/(double)1000;
						double toTimeMicroSec=(timeEndNanos-pathCompReq.getTimeStampNs())/(double)1000;
						double toTimeMiliSec=(timeEndNanos-pathCompReq.getTimeStampNs())/(double)1000000;
						//In some no path cases, we can retry
						//here it is the right place
						boolean retry=false;
						if ((rep.ResponseList.getFirst().getNoPath()!=null)&&(pathCompReq.getRequestList().getFirst().getRequestParameters().isRetry())){
							double totalTimeMs=(System.nanoTime()-pathCompReq.getTimeStampNs())/1000000L;
							if (pathCompReq.getRequestList().getFirst().getRequestParameters().getMaxRequestTimeTLV()!=null){
								long maxReqTimeMs=pathCompReq.getRequestList().getFirst().getRequestParameters().getMaxRequestTimeTLV().getMaxRequestTime();
								if (totalTimeMs<=maxReqTimeMs){
									if (totalTimeMs<60000){//FIXME: LIMITE DE 1 MINUTO, PARA EVITAR ATAQUE MALINTENCIONADO
										log.info("Re-queueing comp req");
										pathComputingRequestRetryQueue.add(pathCompReq);	
										retry=true;
									}
								}
							}
						}
						if (retry==false) {
							if (pathCompReq.getPccReqId()!=null){
								rep.getResponse(0).setPccIdreq(pathCompReq.getPccReqId());
							}
							if (pathCompReq.getMonitoring()!=null){
								log.info("Monitoring Info is requested");
								MetricPCE metricPCE=new MetricPCE();
								PceIdIPv4 pceId=new PceIdIPv4();
								Inet4Address pceIPAddress=null;
								try {
									pceIPAddress = (Inet4Address) Inet4Address.getByName("0.0.0.0");
								} catch (UnknownHostException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								pceId.setPceIPAddress(pceIPAddress);
								metricPCE.setPceId(pceId);
								ProcTime procTime=new ProcTime();
								metricPCE.setProcTime(procTime);
								//FIXME: Ahora lo pongo en us para unas pruebas
								//en la RFC esta en ms
								procTime.setCurrentProcessingTime((long)toTimeMiliSec);
								//procTime.setMaxProcessingTime((long)toTimeMiliSec);
								rep.getResponse(0).getMetricPCEList().add(metricPCE);
							}
							try {    		
								log.info(rep.toString());
								rep.encode();
							} catch (PCEPProtocolViolationException e) {
								// TODO Auto-generated catch block
								log.severe("PROBLEM ENCONDING RESPONSE, CHECK CODE!!"+e.getMessage());
								break;
							}
							try {

								log.info("Request processeed, about to send response");
								pathCompReq.getOut().write(rep.getBytes());
								pathCompReq.getOut().flush();
							} catch (IOException e) {
								log.warning("Could not send the response "+e.getMessage());
								if (rep.getResponse(0).getResConf()!=null){
									//FIXME
									log.warning("If your are using WLANs this is not going to work!!");
									this.reservationManager.cancelReservation(rep.getResponse(0).getResConf().getReservationID());
								}
							}
							//log.info("Response sent number "+rep.getResponseList().getFirst().getRequestParameters().getRequestID()+",rep.getPathList().get(0)"+rep.getResponse(0).getPathList().get(0));

							/*** STRONGEST: Collaborative PCEs ***/	
							//FIXME: pasarlo al reservation manager							
							if (collaborationPCESessionManager!=null){
								if (!(rep.getResponseList().isEmpty())){
									if (!(rep.getResponseList().get(0).getNoPath()!=null)){
										PCEPNotification m_not = createNotificationMessage(rep,pathCompReq.getRequestList().get(0).getReservation().getTimer());				

										collaborationPCESessionManager.sendNotifyMessage(m_not);
									}
								}
							}
						}
					}else {
						log.info("COMPUTING TIME execeeded time, sending NOPATH");
						sendNoPath(pathCompReq);
					}
				} catch (Exception e){
					e.printStackTrace();
				}
			}
			if (analyzeRequestTime){

				double comp=(System.nanoTime()-timeIniNanos)/(double)1000000;
				procTime.analyze(comp);
				timePreNanos=System.nanoTime();
				if (comp>maxProcTime){
					maxProcTime=comp;
				}
			}
		}        
	}

	/**
	 * Stop (at least, try to stop) current computation
	 */
	public void cancelCurrentComputation(){
		ft.cancel(true);
	}

	public Hashtable<Integer, ComputingAlgorithmManager> getSingleAlgorithmList() {
		return singleAlgorithmList;
	}

	public Hashtable<Integer, ComputingAlgorithmManager> getSvecAlgorithmList() {
		return svecAlgorithmList;
	}

	public Hashtable<Integer, ComputingAlgorithmManagerSSON> getSingleAlgorithmListsson() {
		return singleAlgorithmListsson;
	}

	public void setSingleAlgorithmListsson(
			Hashtable<Integer, ComputingAlgorithmManagerSSON> singleAlgorithmListsson) {
		this.singleAlgorithmListsson = singleAlgorithmListsson;
	}

	public Hashtable<Integer, ComputingAlgorithmManagerSSON> getSvecAlgorithmListsson() {
		return svecAlgorithmListsson;
	}

	public void setSvecAlgorithmListsson(
			Hashtable<Integer, ComputingAlgorithmManagerSSON> svecAlgorithmListsson) {
		this.svecAlgorithmListsson = svecAlgorithmListsson;
	}

	public Analysis getIdleTime() {
		return idleTime;
	}

	public void setIdleTime(Analysis idleTime) {
		this.idleTime = idleTime;
	}

	public Analysis getProcTime() {
		return procTime;
	}

	public void setProcTime(Analysis procTime) {
		this.procTime = procTime;
	}

	private void trappingResponse(ComputingResponse resp, long sourceIF, long destIF){
		//Ancora no fa niente

		log.info("First ERO SubObject type "+resp.getResponseList().get(0).getPath(0).geteRO().getEROSubobjectList().getFirst().getClass());
		log.info("Second ERO SubObject type "+resp.getResponseList().get(0).getPath(0).geteRO().getEROSubobjectList().get(1).getClass());
		log.info("Last ERO SubObject type "+resp.getResponseList().get(0).getPath(0).geteRO().getEROSubobjectList().getLast().getClass());
		Inet4Address firstIP=((UnnumberIfIDEROSubobject)resp.getResponseList().get(0).getPath(0).geteRO().getEROSubobjectList().getFirst()).getRouterID();

		EROSubobject label= resp.getResponseList().get(0).getPath(0).geteRO().getEROSubobjectList().get(1);
		resp.getResponseList().get(0).getPath(0).geteRO().getEROSubobjectList().add(0, label);

		UnnumberIfIDEROSubobject firsteroso= new UnnumberIfIDEROSubobject();
		firsteroso.setRouterID(firstIP);
		firsteroso.setInterfaceID(sourceIF);
		firsteroso.setLoosehop(false);
		resp.getResponseList().get(0).getPath(0).geteRO().getEROSubobjectList().add(0, firsteroso);


		int size=resp.getResponseList().get(0).getPath(0).geteRO().getEROSubobjectList().size();
		Inet4Address lastIP=((IPv4prefixEROSubobject)resp.getResponseList().get(0).getPath(0).geteRO().getEROSubobjectList().getLast()).getIpv4address();
		resp.getResponseList().get(0).getPath(0).geteRO().getEROSubobjectList().removeLast();
		UnnumberIfIDEROSubobject lasteroso= new UnnumberIfIDEROSubobject();
		lasteroso.setRouterID(lastIP);
		lasteroso.setInterfaceID(destIF);
		lasteroso.setLoosehop(false);
		resp.getResponseList().get(0).getPath(0).geteRO().getEROSubobjectList().add(lasteroso);
		resp.getResponseList().get(0).getPath(0).geteRO().getEROSubobjectList().add(label);
	}

	private void sendNoPath(ComputingRequest pathCompReq){
		log.info("SENDInd no path ID"+pathCompReq.getRequestList().getFirst().getRequestParameters().requestID);
		ComputingResponse m_resp=new ComputingResponse();
		Response response=new Response();
		RequestParameters rp = new RequestParameters();
		rp.setRequestID(pathCompReq.getRequestList().getFirst().getRequestParameters().requestID);
		response.setRequestParameters(rp);
		NoPath noPath= new NoPath();
		noPath.setNatureOfIssue(ObjectParameters.NOPATH_NOPATH_SAT_CONSTRAINTS);
		NoPathTLV noPathTLV=new NoPathTLV();
		noPath.setNoPathTLV(noPathTLV);			
		noPathTLV.setPCEunavailable(true);
		response.setNoPath(noPath);
		m_resp.addResponse(response);
		try {
			m_resp.encode();
			pathCompReq.getOut().write(m_resp.getBytes());
			pathCompReq.getOut().flush();
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		} catch (PCEPProtocolViolationException e3) {
			// TODO Auto-generated catch block
			e3.printStackTrace();
		}

		log.severe("No path sent... should we send");

	}

	public double getMaxProcTime() {
		return maxProcTime;
	}

	/* STRONGEST: Collaborative PCEs */

	public PCEPNotification createNotificationMessage(ComputingResponse resp,long timer ){
		log.info("Timer "+timer);
		PCEPNotification notificationMsg = new PCEPNotification();
		Notify notify=new Notify();
		Notification notif=new Notification();
		notif.setNotificationType(ObjectParameters.PCEP_NOTIFICATION_TYPE_PRERESERVE);
		LinkedList<Notification> notificationList=new LinkedList<Notification>();
		PathReservationTLV pathReservationTLV=new PathReservationTLV();			
		pathReservationTLV.setERO(resp.getResponseList().getFirst().getPathList().getFirst().geteRO());					
		boolean bidirect = resp.getResponseList().getFirst().getRequestParameters().isBidirect();		
		pathReservationTLV.setTime(timer);
		pathReservationTLV.setBidirectional(bidirect);
		notif.setNotificationTLV(pathReservationTLV);
		notificationList.add(notif);
		notify.setNotificationList(notificationList);
		notificationMsg.addNotify(notify);
		return notificationMsg;	
	}	
}
