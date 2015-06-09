package tid.pce.computingEngine.algorithms.vlan;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import org.jgrapht.GraphPath;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.alg.KruskalMinimumSpanningTree;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;

import es.tid.pce.pcep.constructs.EndPointAndRestrictions;
import es.tid.pce.pcep.constructs.Path;
import es.tid.pce.pcep.constructs.Request;
import es.tid.pce.pcep.constructs.Response;
import es.tid.pce.pcep.objects.EndPoints;
import es.tid.pce.pcep.objects.ExplicitRouteObject;
import es.tid.pce.pcep.objects.GeneralizedEndPoints;
import es.tid.pce.pcep.objects.NoPath;
import es.tid.pce.pcep.objects.ObjectParameters;
import es.tid.pce.pcep.objects.P2MPEndPointsDataPathID;
import es.tid.pce.pcep.objects.RequestParameters;
import es.tid.pce.pcep.objects.tlvs.NoPathTLV;
import es.tid.protocol.commons.ByteHandler;
import es.tid.rsvp.objects.subobjects.GeneralizedLabelEROSubobject;
import es.tid.rsvp.objects.subobjects.SwitchIDEROSubobject;
import es.tid.rsvp.objects.subobjects.SwitchIDEROSubobjectEdge;
import tid.pce.computingEngine.ComputingRequest;
import tid.pce.computingEngine.ComputingResponse;
import tid.pce.computingEngine.algorithms.AlgorithmReservation;
import tid.pce.computingEngine.algorithms.ComputingAlgorithm;
import tid.pce.server.wson.ReservationManager;
import tid.pce.tedb.DomainTEDB;
import tid.pce.tedb.IntraDomainEdge;
import tid.pce.tedb.TEDB;
import tid.provisioningManager.objects.RouterInfoPM;

/**
 * 
 * @author jaume
 * Si lees esto, hazlo con valors, pero sobre todo , con humiltat.
 */

public class VLAN_Multicast_algorithm implements ComputingAlgorithm 
{
	static AtomicInteger atI = new AtomicInteger(0);
	static public String BYTE_TAG = "1111000011110001";

	/**
	* The Logger.
	*/
	private Logger log=Logger.getLogger("PCEServer");
	
	/**
	 * The Path Computing Request to calculate.
	 */
	private ComputingRequest pathReq;

	/**
	 * Access to the Precomputation part of the algorithm.
	 */
	private VLAN_Multicast_algorithmPreComputation preComp;
	
	/**
	 * Access to the Reservation Manager to make reservations of Wavalengths/labels.
	 */
	private ReservationManager reservationManager;
	
	/**
	 * Default Vlan for untagged ports_
	 * 
	 */
	
	private Integer Default_Vlan_Ports = 20;
	
//		/**
//		 * Number of wavelenghts (labels).
//		 */
//		private int num_lambdas;
	
	/**
	 * The traffic engineering database
	 */
	private DomainTEDB ted;
	
	private GenericWLANReservation  reserv;
	/**
	 * Constructor
	 * @param pathReq
	 * @param ted
	 * @param reservationManager
	 */
	public VLAN_Multicast_algorithm(ComputingRequest pathReq,TEDB ted, ReservationManager reservationManager )
	{
		//this.num_lambdas=((DomainTEDB)ted).getNumLambdas();
		this.pathReq=pathReq;
		this.reservationManager=reservationManager;
		this.ted=(DomainTEDB)ted;
	}

	/**
	 * Exectutes the path computation and returns the PCEP Response
	 */
	public ComputingResponse call()
	{ 
		
		
		
		//Timestamp of the start of the algorithm;
		long tiempoini =System.nanoTime();
		//Create the response message
		//It will contain either the path or noPath
		ComputingResponse m_resp=new ComputingResponse();
		m_resp.setEncodingType(pathReq.getEcodingType());
		//The request that needs to be solved
		Request req=pathReq.getRequestList().get(0);
		//Request Id, needed for the response
		long reqId=req.getRequestParameters().getRequestID();
		log.info("Request id: "+reqId+", getting endpoints");
		//Start creating the response
		Response response=new Response();
		RequestParameters rp = new RequestParameters();
		rp.setBidirect(req.getRequestParameters().isBidirect());
		rp.setRequestID(reqId);
		response.setRequestParameters(rp);
		m_resp.addResponse(response);
		
		//esto hay que cambiarlo para poder leer del GENERALIZED END POINTS
		//if (getObjectType(req.getEndPoints()))
		
		//log.info("Bw: "+req.getBandwidth().getBw());
		EndPoints  EP= req.getEndPoints();	
				
		ArrayList<RouterInfoPM> switchList = null;
		ArrayList<Integer> portList = null;
		RouterInfoPM source = null;
		String source_mac = null;

		if (EP.getOT()==ObjectParameters.PCEP_OBJECT_TYPE_ENDPOINTS_IPV4)
		{
			log.info("Error : PCEP_OBJECT_TYPE_ENDPOINTS_IPV4");
		}
		else if (EP.getOT()==ObjectParameters.PCEP_OBJECT_TYPE_ENDPOINTS_IPV6)
		{
			log.info("Error : PCEP_OBJECT_TYPE_ENDPOINTS_IPV6");
		}
		else if (EP.getOT()==ObjectParameters.PCEP_OBJECT_TYPE_GENERALIZED_ENDPOINTS)
		{
			log.info("OK : PCEP_OBJECT_TYPE_GENERALIZED_ENDPOINTS");
			
			GeneralizedEndPoints endP = (GeneralizedEndPoints)req.getEndPoints();
			LinkedList<EndPointAndRestrictions> EandRList = endP.getP2MPEndpoints().getEndPointAndRestrictionsList();
			
			switchList = new ArrayList<RouterInfoPM>();
			portList = new ArrayList<Integer>();
			for (int i = 0; i < EandRList.size(); i++) 
			{
				String switchId = EandRList.get(i).getEndPoint().getXifiEndPointTLV().getSwitchID();
				int port_number = EandRList.get(i).getEndPoint().getXifiEndPointTLV().getPort();
				log.info("XifiEndPoint"+switchId+",port_number:"+port_number);
				portList.add(port_number);
				switchList.add(new RouterInfoPM(switchId));
			}
			
			source = new RouterInfoPM(endP.getP2MPEndpoints().getEndPointAndRestrictions().getEndPoint().getXifiEndPointTLV().getSwitchID());
			source_mac = endP.getP2MPEndpoints().getEndPointAndRestrictions().getEndPoint().getXifiEndPointTLV().getMac();
		}
		else if (EP.getOT() == ObjectParameters.PCEP_OBJECT_TYPE_P2MP_ENDPOINTS_DATAPATHID)
		{
			log.info("OK : PCEP_OBJECT_TYPE_P2MP_ENDPOINTS_DATAPATHID");
			switchList = new ArrayList<RouterInfoPM>();
			portList = new ArrayList<Integer>();
			
			P2MPEndPointsDataPathID endP = (P2MPEndPointsDataPathID)req.getEndPoints();
			
			for (int i = 0; i < endP.getDestDatapathIDList().size(); i++)
			{
				switchList.add(new RouterInfoPM(endP.getDestDatapathIDList().get(i)));
				portList.add(0);
			}
			
			source = new RouterInfoPM(endP.getSourceDatapathID());
			source_mac = "00:00:00:00:00:00";
		}
		else if (EP.getOT()==ObjectParameters.PCEP_OBJECT_TYPE_ENDPOINTS_MAC)
		{
			log.info("Error : PCEP_OBJECT_TYPE_ENDPOINTS_MAC");
		}
		/*
		if (atI.intValue() % 2 == 0)
		{
			atI.incrementAndGet();
			return sendNoPath(response, m_resp);		
		}
		
		atI.incrementAndGet();
		*/
		
		log.info("ted::" + ted.printTopology());
		
		//Check if all vertex are in graph
		for (int i = 0; i < switchList.size(); i++) 
		{
			if (!(ted.containsVertex(switchList.get(i))))
			{
				return sendNoPath(response, m_resp);
			}
		}

		Set<IntraDomainEdge> edges = null;
		preComp.getGraphLock().lock();
		try
		{
			SimpleDirectedWeightedGraph<Object,IntraDomainEdge> graphLambda = preComp.getNetworkGraphs().get(0); 
			KruskalMinimumSpanningTree<Object,IntraDomainEdge>  kmst = new KruskalMinimumSpanningTree<Object,IntraDomainEdge> (graphLambda);
			edges = kmst.getEdgeSet();
			
			
			log.info("graphLambda::::"+graphLambda);
			log.info("kmst.getEdgeSet()::::"+kmst.getEdgeSet());
			/*	
			if ((edges==null) || (edges.size() == 0))
			{				
				return sendNoPath(response, m_resp);	
			}
			*/
			SimpleDirectedWeightedGraph<Object,IntraDomainEdge> sdwg = new SimpleDirectedWeightedGraph<Object,IntraDomainEdge>(IntraDomainEdge.class);
			
			log.info("edges.size():1::"+edges.size());
			
			
			for (IntraDomainEdge ide : edges) 
			{	
				log.info("ide.getSource():"+ide.getSource()+",ide.getTarget():"+ide.getTarget());
				log.info("graphLambda.getEdge(ide.getTarget(), ide.getSource()):"+graphLambda.getEdge(ide.getTarget(), ide.getSource()));
				sdwg.addVertex(ide.getSource());
				sdwg.addVertex(ide.getTarget());
				sdwg.addEdge(ide.getSource(), ide.getTarget(), ide);
				
				sdwg.addEdge(ide.getTarget(), ide.getSource(), graphLambda.getEdge(ide.getTarget(), ide.getSource()));
			}
			log.info("sdwg::"+sdwg);
			
			HashSet<IntraDomainEdge> edge_list = new HashSet<IntraDomainEdge>();
			Set<RouterInfoPM> node_set = new HashSet<RouterInfoPM>();
			
			log.info("source::"+source);
			log.info("source_mac::"+source_mac);
			
			boolean is_there_only_one_swith = true;
			
			for (int i = 0; i < switchList.size() && edges.size() > 0; i++) 
			{
				log.info("switchList.get(i)::"+switchList.get(i));
				
				if (!sdwg.containsVertex(switchList.get(i)))
				{
					log.info("Probably only one switch in the query");
					break;
				}
				
				//Case with only one node
				if (!(sdwg.containsVertex(source)))
				{
					break;
				}
				
				DijkstraShortestPath<Object,IntraDomainEdge>  dsp = new DijkstraShortestPath<Object,IntraDomainEdge> (sdwg, source, switchList.get(i));
				GraphPath<Object,IntraDomainEdge> result = dsp.getPath();
				
				if (result == null)
				{
					log.info("Sending No Paath");
				}
				
				log.info("Iteration i: "+i);
				for (IntraDomainEdge ide_result : result.getEdgeList())
				{
					log.info("Source-->After:"+ide_result.getSource());
					log.info("Target-->After:"+ide_result.getTarget());
					edge_list.add(ide_result);
					node_set.add((RouterInfoPM)ide_result.getSource());
					node_set.add((RouterInfoPM)ide_result.getTarget());
					is_there_only_one_swith = false;
				}
			}
			
			if (is_there_only_one_swith)
			{
				node_set.add(source);
			}
			

			Path path=new Path();
			ExplicitRouteObject ero= new ExplicitRouteObject();
			
			// Ñapa maxima para distinguir en el PM que este WF
			// Asi son las cosas, si juegas en la champions puedes caer en la primera ronda
			
			GeneralizedLabelEROSubobject geL = new GeneralizedLabelEROSubobject();
			byte[] array = new BigInteger(VLAN_Multicast_algorithm.BYTE_TAG, 2).toByteArray();
			geL.setLabel(array);
			
			ero.addEROSubobject(geL);
			
			for (RouterInfoPM rout : node_set) 
			{
				log.info("Switch id switchList.get(i).getRouterID():"+rout.getRouterID());
				SwitchIDEROSubobject eroso= new SwitchIDEROSubobject();
				eroso.setSwitchID(ByteHandler.MACFormatStringtoByteArray(rout.getRouterID()));
				eroso.setDest_int(0);
				eroso.setSource_int(0);
				eroso.setVlan(0);
				eroso.setAssociated_mac(ByteHandler.MACFormatStringtoByteArray(source_mac));
				
				ero.addEROSubobject(eroso);
		    }
			
			for (int i = 0; i < switchList.size(); i++)
			{
				log.info("Adding link to VM!!");
				SwitchIDEROSubobjectEdge eroso= new SwitchIDEROSubobjectEdge();
				eroso.setSource_SwitchID(ByteHandler.MACFormatStringtoByteArray(switchList.get(i).getRouterID()));
				eroso.setDest_SwitchID(ByteHandler.MACFormatStringtoByteArray("00:00:00:00:00:00:00:00"));
				eroso.setSource_int(portList.get(i));
				ero.addEROSubobject(eroso);
			}
			
			for (IntraDomainEdge ide_def : edge_list)
			{
				log.info("((RouterInfoPM)ide_def.getSource()).getRouterID()):"+((RouterInfoPM)ide_def.getSource()).getRouterID());
				log.info("((RouterInfoPM)ide_def.getTarget()).getRouterID()):"+((RouterInfoPM)ide_def.getTarget()).getRouterID());
				SwitchIDEROSubobjectEdge eroso= new SwitchIDEROSubobjectEdge();
				eroso.setSource_SwitchID(ByteHandler.MACFormatStringtoByteArray(((RouterInfoPM)ide_def.getSource()).getRouterID()));
				eroso.setDest_SwitchID(ByteHandler.MACFormatStringtoByteArray(((RouterInfoPM)ide_def.getTarget()).getRouterID()));
				eroso.setAssociated_mac(ByteHandler.MACFormatStringtoByteArray(source_mac));
				eroso.setSource_int((int)ide_def.getSrc_if_id());
				eroso.setDest_int((int)ide_def.getDst_if_id());
				eroso.setVlan(ide_def.getTE_info().getVlan());
				ero.addEROSubobject(eroso);
			}
		
			
			path.seteRO(ero);
			//PCEPUtils.completeMetric(path, req, edge_list);
			response.addPath(path);
								
			if (req.getReservation()!=null)
			{
				/*
				reserv= new GenericWLANReservation();
				reserv.setResp(m_resp);
				reserv.setReservation(req.getReservation());
				reserv.setSourceVertexList(sourceVertexList);
				reserv.setTargetVertexList(targetVertexList);
				reserv.setWLANList(wlans);
				*/
				
				if (rp.isBidirect() == true){
					reserv.setBidirectional(true);
				}
				else{
					reserv.setBidirectional(false);
				}
				
				reserv.setReservationManager(reservationManager);
			}

		}
		finally
		{
			preComp.getGraphLock().unlock();	
		}
		
		long tiempofin =System.nanoTime();
		long tiempotot=tiempofin-tiempoini;
		log.info("Ha tardado "+tiempotot+" nanosegundos");
		return m_resp;
	}

	public void setPreComp(VLAN_Multicast_algorithmPreComputation preComp) {
		this.preComp = preComp;
	}

	public AlgorithmReservation getReserv() {
		return reserv;
	}
	
	private ComputingResponse sendNoPath(Response response, ComputingResponse m_resp)
	{
		log.warning("Big Warning: Source or destination are NOT in the TED, sending NO PATH");
		NoPath noPath= new NoPath();
		noPath.setNatureOfIssue(ObjectParameters.NOPATH_NOPATH_SAT_CONSTRAINTS);
		NoPathTLV noPathTLV=new NoPathTLV();
		
		noPathTLV.setUnknownSource(true);	
		noPathTLV.setUnknownDestination(true);	

		noPath.setNoPathTLV(noPathTLV);				
		response.setNoPath(noPath);
		return m_resp;
	}
}
