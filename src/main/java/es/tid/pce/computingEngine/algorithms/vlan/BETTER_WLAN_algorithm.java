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
import es.tid.pce.pcep.objects.EndPoints;
import es.tid.pce.pcep.objects.EndPointsIPv4;
import es.tid.pce.pcep.objects.ExplicitRouteObject;
import es.tid.pce.pcep.objects.NoPath;
import es.tid.pce.pcep.objects.ObjectParameters;
import es.tid.pce.pcep.objects.RequestParameters;
import es.tid.pce.pcep.objects.XifiUniCastEndPoints;
import es.tid.pce.pcep.objects.tlvs.NoPathTLV;
import es.tid.pce.server.wson.ReservationManager;
import es.tid.protocol.commons.ByteHandler;
import es.tid.rsvp.RSVPProtocolViolationException;
import es.tid.rsvp.constructs.WLANLabel;
import es.tid.rsvp.objects.subobjects.GeneralizedLabelEROSubobject;
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

public class BETTER_WLAN_algorithm implements ComputingAlgorithm {

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
	public BETTER_WLAN_algorithm(ComputingRequest pathReq,TEDB ted, ReservationManager reservationManager ){
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
		Object source_router_addr = null;
		Object dest_router_addr = null;
		
		Integer source_interface = null;
		Integer destination_interface = null;
		String source_mac = null;
		String dest_mac = null;
		
		XifiUniCastEndPoints xifiEp = null;

		if (EP.getOT()==ObjectParameters.PCEP_OBJECT_TYPE_ENDPOINTS_IPV4){
			log.info("Fix the line below and pray for working");
			log.info("En PCEServer.java hay un lio con iniciar la topologia, ahora se mete informacion de configuracion y de");
			EndPointsIPv4  ep=(EndPointsIPv4) req.getEndPoints();
			source_router_addr=ep.getSourceIP();
			dest_router_addr=ep.getDestIP();
		}else if (EP.getOT()==ObjectParameters.PCEP_OBJECT_TYPE_ENDPOINTS_IPV6){

		}else if (EP.getOT()==ObjectParameters.PCEP_OBJECT_TYPE_GENERALIZED_ENDPOINTS){
			log.warning("Error: Not supported yet");
		}else if (EP.getOT()==ObjectParameters.PCEP_OBJECT_TYPE_ENDPOINTS_MAC){
			log.info("EP.getOT()::"+EP.getOT());
			xifiEp = (XifiUniCastEndPoints) req.getEndPoints();
			source_router_addr = new RouterInfoPM(xifiEp.getSwitchSourceID());
			dest_router_addr = new RouterInfoPM(xifiEp.getSwitchDestinationID());
			log.info("ep.getSwitchSourceID()::"+xifiEp.getSwitchSourceID());
			log.info("ep.getSwitchDestinationID()::"+xifiEp.getSwitchDestinationID());
			log.info("ep.getSource_port()::"+xifiEp.getSource_port());
			log.info("ep.getDestination_port()::"+xifiEp.getDestination_port());
			
			source_interface = xifiEp.getSource_port();
			destination_interface = xifiEp.getDestination_port();
			
			source_mac = xifiEp.getSourceMAC();
			dest_mac = xifiEp.getDestinationMAC();
		}
		//aqu� acaba lo que he a�adido
		log.info("ted.containsVertex(source_router_addr):"+ted.containsVertex(source_router_addr));
		log.info("ted.containsVertex(dest_router_addr):"+ted.containsVertex(dest_router_addr));
		//Now, check if the source and destination are in the TED.
		log.info("source_router_id_addr:"+source_router_addr+",dest_router_id_addr:"+dest_router_addr);
		if (!(((ted.containsVertex(source_router_addr))&&(ted.containsVertex(dest_router_addr))))){
			log.warning("Source or destination are NOT in the TED");	
			NoPath noPath= new NoPath();
			noPath.setNatureOfIssue(ObjectParameters.NOPATH_NOPATH_SAT_CONSTRAINTS);
			NoPathTLV noPathTLV=new NoPathTLV();
			if (!((ted.containsVertex(source_router_addr)))){
				log.finest("Unknown source");	
				noPathTLV.setUnknownSource(true);	
			}
			if (!((ted.containsVertex(dest_router_addr)))){
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
					log.info("Source SwitchID:::"+xifiEp.getSwitchSourceID());
					eroso.setSwitchID(ByteHandler.MACFormatStringtoByteArray(xifiEp.getSwitchSourceID()));
					eroso.setSource_int(source_interface);
					eroso.setAssociated_mac(ByteHandler.MACFormatStringtoByteArray(source_mac));
					eroso.setSecond_associated_mac(ByteHandler.MACFormatStringtoByteArray(dest_mac));
					eroso.setDest_int(destination_interface);
					ero.addEROSubobject(eroso);
				}
				
				else
				{
					log.info("Edge list size::"+edge_list.size());
					log.info("(int)edge_list.get(0).getSrc_if_id()::"+(int)edge_list.get(0).getSrc_if_id());
					//TODO There should be source MAC and dest MAC, two associated MACs,
					SwitchIDEROSubobject eroso = new SwitchIDEROSubobject();
					eroso.setSwitchID(ByteHandler.MACFormatStringtoByteArray(((RouterInfoPM)edge_list.get(0).getSource()).getRouterID()));
					eroso.setSource_int(source_interface);
					eroso.setDest_int((int)edge_list.get(0).getSrc_if_id());
					eroso.setAssociated_mac(ByteHandler.MACFormatStringtoByteArray(source_mac));
					
					if (edge_list.get(0).TE_info.isVlanLink())
					{
						eroso.setVlan(edge_list.get(0).TE_info.getVlan());
					}
					
					ero.addEROSubobject(eroso);
					//Configuring all jumps except 0 and the last one
					for (i=0 ; i<edge_list.size()-1 ; i++)
					{
						eroso = new SwitchIDEROSubobject();
						eroso.setSwitchID(ByteHandler.MACFormatStringtoByteArray(((RouterInfoPM)edge_list.get(i+1).getSource()).getRouterID()));
						eroso.setSource_int((int)(edge_list.get(i).getDst_if_id()));
						eroso.setDest_int((int)(edge_list.get(i+1).getSrc_if_id()));
						eroso.setLoosehop(false);
						ero.addEROSubobject(eroso);
						
						if (edge_list.get(i+1).TE_info.isVlanLink())
						{
							eroso.setVlan(edge_list.get(i+1).TE_info.getVlan());
						}
						
						log.info("Setting in switich: "+((RouterInfoPM)edge_list.get(i).getSource()).getRouterID()+",sourceInt: "+(int)(edge_list.get(i).getSrc_if_id())+",destInt: "+((int)(edge_list.get(i).getDst_if_id())));
						// If it's the first iteration set source port. This information comes from the Endpoint
						
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
					eroso= new SwitchIDEROSubobject();
					eroso.setSource_int((int)(edge_list.get(edge_list.size()-1).getDst_if_id()));
					eroso.setSwitchID(ByteHandler.MACFormatStringtoByteArray(((RouterInfoPM)edge_list.get(edge_list.size()-1).getTarget()).getRouterID()));
					
					//Set the destination port. This information comes from the Endpoint
					eroso.setDest_int(destination_interface);
					eroso.setAssociated_mac(ByteHandler.MACFormatStringtoByteArray(dest_mac));
					
					ero.addEROSubobject(eroso);
				}
			
				
				path.setEro(ero);
				PCEPUtils.completeMetric(path, req, edge_list);
				response.addPath(path);
									
				if (req.getReservation()!=null){
					reserv= new GenericWLANReservation();
					reserv.setResp(m_resp);
					reserv.setReservation(req.getReservation());
					reserv.setSourceVertexList(sourceVertexList);
					reserv.setTargetVertexList(targetVertexList);
					reserv.setWLANList(wlans);
					
					if (rp.isBidirect() == true){
						reserv.setBidirectional(true);
					}
					else{
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
