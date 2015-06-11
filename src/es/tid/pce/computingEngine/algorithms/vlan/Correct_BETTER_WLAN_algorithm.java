package es.tid.pce.computingEngine.algorithms.vlan;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import org.jgrapht.GraphPath;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;

import es.tid.pce.computingEngine.ComputingRequest;
import es.tid.pce.computingEngine.ComputingResponse;
import es.tid.pce.computingEngine.algorithms.AlgorithmReservation;
import es.tid.pce.computingEngine.algorithms.ComputingAlgorithm;
import es.tid.pce.computingEngine.algorithms.PCEPUtils;
import es.tid.pce.pcep.constructs.Path;
import es.tid.pce.pcep.constructs.Request;
import es.tid.pce.pcep.constructs.Response;
import es.tid.pce.pcep.objects.EndPointDataPathID;
import es.tid.pce.pcep.objects.EndPoints;
import es.tid.pce.pcep.objects.ExplicitRouteObject;
import es.tid.pce.pcep.objects.NoPath;
import es.tid.pce.pcep.objects.ObjectParameters;
import es.tid.pce.pcep.objects.RequestParameters;
import es.tid.pce.pcep.objects.tlvs.NoPathTLV;
import es.tid.pce.server.wson.ReservationManager;
import es.tid.rsvp.RSVPProtocolViolationException;
import es.tid.rsvp.constructs.WLANLabel;
import es.tid.rsvp.objects.subobjects.GeneralizedLabelEROSubobject;
import es.tid.rsvp.objects.subobjects.OpenFlowUnnumberIfIDEROSubobject;
import es.tid.rsvp.objects.subobjects.SwitchIDEROSubobject;
import es.tid.tedb.DomainTEDB;
import es.tid.tedb.IntraDomainEdge;
import es.tid.tedb.TEDB;
import es.tid.tedb.elements.RouterInfoPM;


/**
 * This algorithm is similar to this one:
 * 
 * Implementation of the algorithm "Adaptive Unconstrained Routing Exhaustive".
 * 
 * <p>Reference: A. Mokhtar y M. Azizoglu, "Adaptive wavelength routing in all-optical networks",
 * IEEE/ACM Transactions on Networking, vol. 6, no.2 pp. 197 - 201, abril 1998</p>
 * 
 * But it allow an LSP to change wlan number between switches.
 * 
 * 
 * @author jaume
 * Si lees esto, hazlo con valors, pero sobre todo , con humiltat.
 */

public class Correct_BETTER_WLAN_algorithm implements ComputingAlgorithm {

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
	private BETTER_WLAN_algorithmPreComputation preComp;
	
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
	public Correct_BETTER_WLAN_algorithm(ComputingRequest pathReq,TEDB ted, ReservationManager reservationManager ){
		//this.num_lambdas=((DomainTEDB)ted).getNumLambdas();
		this.pathReq=pathReq;
		this.reservationManager=reservationManager;
		this.ted=(DomainTEDB)ted;
	}

	/**
	 * Exectutes the path computation and returns the PCEP Response
	 */
	public ComputingResponse call(){ 
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
		
		EndPointDataPathID endPDataId = null;

		log.info("EP.getOT()::"+EP.getOT());
		
		RouterInfoPM source_router_addr = null;
		RouterInfoPM dest_router_addr = null;
		
		if (EP.getOT()==ObjectParameters.PCEP_OBJECT_TYPE_ENDPOINTS_DATAPATH_ID)
		{
			endPDataId = (EndPointDataPathID) req.getEndPoints();
			source_router_addr = new RouterInfoPM(endPDataId.getSourceSwitchID());
			dest_router_addr = new RouterInfoPM(endPDataId.getDestSwitchID());
		}
		else
		{
			log.warning("Error: Unsupported Endpoint for this Algorithm");
		}
		
		//aqu� acaba lo que he a�adido
		log.info("ted.containsVertex(source_router_addr):"+ted.containsVertex(source_router_addr));
		log.info("ted.containsVertex(dest_router_addr):"+ted.containsVertex(dest_router_addr));
		//Now, check if the source and destination are in the TED.
		log.info("source_router_id_addr:"+source_router_addr+",dest_router_id_addr:"+dest_router_addr);
		
		if (!(((ted.containsVertex(source_router_addr))&&(ted.containsVertex(dest_router_addr)))))
		{
			log.warning("Source or destination are NOT in the TED");	
			NoPath noPath= new NoPath();
			noPath.setNatureOfIssue(ObjectParameters.NOPATH_NOPATH_SAT_CONSTRAINTS);
			NoPathTLV noPathTLV=new NoPathTLV();
			if (!((ted.containsVertex(source_router_addr))))
			{
				log.finest("Unknown source");	
				noPathTLV.setUnknownSource(true);	
			}
			if (!((ted.containsVertex(dest_router_addr))))
			{
				log.finest("Unknown destination");
				noPathTLV.setUnknownDestination(true);	
			}

			noPath.setNoPathTLV(noPathTLV);				
			response.setNoPath(noPath);
			return m_resp;
		}

		boolean nopath=true;//Initially, we still have no path

		log.fine("Starting the computation");
		GraphPath<Object,IntraDomainEdge> gp_chosen=null;
		preComp.getGraphLock().lock();
		try
		{
			SimpleDirectedWeightedGraph<Object,IntraDomainEdge> graphLambda = preComp.getNetworkGraphs().get(0); 
			DijkstraShortestPath<Object,IntraDomainEdge>  dsp = new DijkstraShortestPath<Object,IntraDomainEdge> (graphLambda, source_router_addr, dest_router_addr);
			gp_chosen = dsp.getPath();
			
			log.info("graphLambda::"+graphLambda);
	
			if (gp_chosen==null)
			{				
				log.info("No path found");
				NoPath noPath= new NoPath();
				noPath.setNatureOfIssue(ObjectParameters.NOPATH_NOPATH_SAT_CONSTRAINTS);
				NoPathTLV noPathTLV=new NoPathTLV();
				noPath.setNoPathTLV(noPathTLV);				
				response.setNoPath(noPath);
				return m_resp;		
			}
			
			nopath = false;
			
			List<IntraDomainEdge> edge_list=gp_chosen.getEdgeList();
			for (int i = 0 ; i < edge_list.size() ; i++)
			{
				if (!(edge_list.get(i).getTE_info().isWLANFree()))
				{
					nopath = true;
				}
			}
			
			if (nopath == true)
			{
				log.info("No path found");
				NoPath noPath= new NoPath();
				noPath.setNatureOfIssue(ObjectParameters.NOPATH_NOPATH_SAT_CONSTRAINTS);
				NoPathTLV noPathTLV=new NoPathTLV();
				noPath.setNoPathTLV(noPathTLV);				
				response.setNoPath(noPath);
				return m_resp;	
			}
			
			if (nopath==false)
			{

				Path path=new Path();
				ExplicitRouteObject ero= new ExplicitRouteObject();
				edge_list=gp_chosen.getEdgeList();
				log.info("edge_list.size():"+edge_list.size());
				for (int i=0;i<edge_list.size();i++)
				{
					log.info("New Router Info");
					log.info(((RouterInfoPM)edge_list.get(0).getSource()).getRouterID());
					log.info(((RouterInfoPM)edge_list.get(0).getTarget()).getRouterID());
					log.info(((Long)edge_list.get(i).getSrc_if_id()).toString());
					log.info(((Long)edge_list.get(i).getDst_if_id()).toString());
				}
				int i;
				
				LinkedList<Object> sourceVertexList=new LinkedList<Object>();
				LinkedList<Object> targetVertexList=new LinkedList<Object>();
				LinkedList<Integer> wlans=new LinkedList<Integer>();
				
				//Configuring jump 0
				
				if (edge_list.size() == 0)
				{
					SwitchIDEROSubobject eroso= new SwitchIDEROSubobject();
					log.info("Source SourceSwitchID:::"+endPDataId.getSourceSwitchID());
					log.info("Source DestSwitchID:::"+endPDataId.getDestSwitchID());
					
					//De momento se va a asumir que nadie va a preguntar por esto
				}
				
				else
				{
					log.info("Edge list size::"+edge_list.size());
					log.info("(int)edge_list.get(0).getSrc_if_id()::"+(int)edge_list.get(0).getSrc_if_id());

					OpenFlowUnnumberIfIDEROSubobject opFlw;
					
					for (i=0 ;  i < edge_list.size(); i++)
					{
						opFlw = new OpenFlowUnnumberIfIDEROSubobject();
						
						opFlw.setSwitchID(((RouterInfoPM)edge_list.get(i).getSource()).getRouterID()); 
						opFlw.setInterfaceID(edge_list.get(i).getSrc_if_id());
						
						ero.addEROSubobject(opFlw);
						
						GeneralizedLabelEROSubobject genLabel= new GeneralizedLabelEROSubobject();
						ero.addEROSubobject(genLabel);
						//ITU-T Format
						WLANLabel wlanlabel=new WLANLabel();
						wlanlabel.setIdentifier(0);
						int wlan = edge_list.get(i).getTE_info().getFreeWLAN();
						wlanlabel.setN(wlan);
						try 
						{
							wlanlabel.encode();
						} 
						catch (RSVPProtocolViolationException e) 
						{
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						genLabel.setLabel(wlanlabel.getBytes());
						
		
						sourceVertexList.add(edge_list.get(i).getSource());
						targetVertexList.add(edge_list.get(i).getTarget());
						wlans.add(wlan);	
					}
					//Configuring the last jump
					opFlw= new OpenFlowUnnumberIfIDEROSubobject();
					opFlw.setSwitchID(((RouterInfoPM)edge_list.get(edge_list.size()-1).getTarget()).getRouterID());
					opFlw.setInterfaceID(0);
					
					ero.addEROSubobject(opFlw);
				}
			
				
				path.seteRO(ero);
				PCEPUtils.completeMetric(path, req, edge_list);
				response.addPath(path);
									
				if (req.getReservation()!=null){
					reserv= new GenericWLANReservation();
					reserv.setResp(m_resp);
					reserv.setReservation(req.getReservation());
					reserv.setSourceVertexList(sourceVertexList);
					reserv.setTargetVertexList(targetVertexList);
					reserv.setWLANList(wlans);
					
					if (rp.isBidirect() == true)
					{
						reserv.setBidirectional(true);
					}
					else
					{
						reserv.setBidirectional(false);
					}
					
					reserv.setReservationManager(reservationManager);
				}
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
	
	

	public void setPreComp(BETTER_WLAN_algorithmPreComputation preComp) {
		this.preComp = preComp;
	}

	public AlgorithmReservation getReserv() {
		return reserv;
	}
}
