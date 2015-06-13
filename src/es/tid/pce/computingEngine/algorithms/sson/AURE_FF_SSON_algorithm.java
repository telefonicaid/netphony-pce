package es.tid.pce.computingEngine.algorithms.sson;

import java.net.Inet4Address;
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
import es.tid.pce.computingEngine.algorithms.utilities.bandwidthToSlotConversion;
import es.tid.pce.computingEngine.algorithms.utilities.graphs_comparator;
import es.tid.pce.pcep.constructs.EndPoint;
import es.tid.pce.pcep.constructs.EndPointAndRestrictions;
import es.tid.pce.pcep.constructs.GeneralizedBandwidthSSON;
import es.tid.pce.pcep.constructs.P2MPEndpoints;
import es.tid.pce.pcep.constructs.P2PEndpoints;
import es.tid.pce.pcep.constructs.Path;
import es.tid.pce.pcep.constructs.Request;
import es.tid.pce.pcep.constructs.Response;
import es.tid.pce.pcep.objects.BandwidthRequested;
import es.tid.pce.pcep.objects.BandwidthRequestedGeneralizedBandwidth;
import es.tid.pce.pcep.objects.EndPoints;
import es.tid.pce.pcep.objects.EndPointsIPv4;
import es.tid.pce.pcep.objects.ExplicitRouteObject;
import es.tid.pce.pcep.objects.GeneralizedEndPoints;
import es.tid.pce.pcep.objects.NoPath;
import es.tid.pce.pcep.objects.ObjectParameters;
import es.tid.pce.pcep.objects.RequestParameters;
import es.tid.pce.pcep.objects.tlvs.NoPathTLV;
import es.tid.pce.server.wson.ReservationManager;
import es.tid.rsvp.RSVPProtocolViolationException;
import es.tid.rsvp.constructs.gmpls.DWDMWavelengthLabel;
import es.tid.rsvp.objects.subobjects.GeneralizedLabelEROSubobject;
import es.tid.rsvp.objects.subobjects.IPv4prefixEROSubobject;
import es.tid.rsvp.objects.subobjects.UnnumberIfIDEROSubobject;
import es.tid.tedb.DomainTEDB;
import es.tid.tedb.IntraDomainEdge;
import es.tid.tedb.SSONInformation;
import es.tid.tedb.TEDB;

/**
 * Implementation of the algorithm "Adaptive Unconstrained Routing Exhaustive".
 * 
 * <p>Reference: A. Mokhtar y M. Azizoglu, "Adaptive wavelength routing in all-optical networks",
 * IEEE/ACM Transactions on Networking, vol. 6, no.2 pp. 197 - 201, abril 1998</p>
 * @author arturo mayoral
 *
 */
public class AURE_FF_SSON_algorithm implements ComputingAlgorithm {

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
	private AURE_FF_SSON_algorithmPreComputation preComp;
	
	/**
	 * Access to the Reservation Manager to make reservations of Wavalengths/labels.
	 */
	private ReservationManager reservationManager;
	
	/**
	 * Information about the labels in a SSON network.
	 */
	private SSONInformation SSONInfo;
	
//	/**
//	 * Number of wavelenghts (labels).
//	 */
	//private int num_lambdas;
	
	/**
	 * The traffic engineering database
	 */
	private DomainTEDB ted;
	
	/**
	 * Class for pre-reservation 
	 */
	
	private GenericLambdaReservation  reserv;
	/**
	 * Constructor
	 * @param pathReq
	 * @param ted
	 * @param reservationManager
	 */
	public AURE_FF_SSON_algorithm(ComputingRequest pathReq,TEDB ted, ReservationManager reservationManager, int mf ){
		//this.num_lambdas=((DomainTEDB)ted).getSSONinfo().getNumLambdas();
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
		log.finest("Starting AURE Algorithm");
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
		rp.setRequestID(reqId);
		response.setRequestParameters(rp);
		m_resp.addResponse(response);

		//esto hay que cambiarlo para poder leer del GENERALIZED END POINTS
		//if (getObjectType(req.getEndPoints()))
		EndPoints  EP= req.getEndPoints();
		BandwidthRequested  Bw= (BandwidthRequested) req.getBandwidth(); // Objeto bandwidth para saber la demanda de la peticion.
		Object source_router_id_addr = null;
		Object dest_router_id_addr = null;
		graphs_comparator grc = new graphs_comparator ();
		
		
		// Conversión Bw a numero de slots en función de la grid.
		int num_slots = 0;
		int cs;
		int m=0;
		bandwidthToSlotConversion conversion= new bandwidthToSlotConversion();
		
		// Conversión Bw a numero de slots en función de la grid.	
		if (Bw.getBw()!=0){
			SSONInfo=((DomainTEDB)ted).getSSONinfo();
			cs = SSONInfo.getCs();
			num_slots=conversion.getNumSlots(Bw.getBw(), cs);
		}
			
		log.info("Request num_slots: "+num_slots);
		
		if (EP.getOT()==ObjectParameters.PCEP_OBJECT_TYPE_ENDPOINTS_IPV4){
			EndPointsIPv4  ep=(EndPointsIPv4) req.getEndPoints();
			source_router_id_addr=ep.getSourceIP();
			dest_router_id_addr=ep.getDestIP();
		}else if (EP.getOT()==ObjectParameters.PCEP_OBJECT_TYPE_ENDPOINTS_IPV6){

		}else if (EP.getOT()==ObjectParameters.PCEP_OBJECT_TYPE_GENERALIZED_ENDPOINTS){
			GeneralizedEndPoints  gep=(GeneralizedEndPoints) req.getEndPoints();
			if(gep.getGeneralizedEndPointsType()==ObjectParameters.PCEP_GENERALIZED_END_POINTS_TYPE_P2P){
				P2PEndpoints p2pep= gep.getP2PEndpoints();
				EndPoint sourceep=p2pep.getSourceEndPoint();
				EndPoint destep=p2pep.getDestinationEndPoint();
				source_router_id_addr=sourceep.getEndPointIPv4TLV().IPv4address;
				dest_router_id_addr=destep.getEndPointIPv4TLV().IPv4address;
			}
			if(gep.getGeneralizedEndPointsType()==ObjectParameters.PCEP_GENERALIZED_END_POINTS_TYPE_P2MP_NEW_LEAVES){
				P2MPEndpoints p2mpep= gep.getP2MPEndpoints();
				EndPointAndRestrictions epandrest=p2mpep.getEndPointAndRestrictions();
				EndPoint sourceep=epandrest.getEndPoint();
				source_router_id_addr=sourceep.getEndPointIPv4TLV().IPv4address;
				int cont=0;
				while (cont<=p2mpep.getEndPointAndRestrictionsList().size()){ //esto est� mal
					epandrest=p2mpep.getEndPointAndRestrictionsList().get(cont);
					EndPoint destep=epandrest.getEndPoint();
					source_router_id_addr=sourceep.getEndPointIPv4TLV().IPv4address;
					dest_router_id_addr=destep.getEndPointIPv4TLV().IPv4address;

				}
			}
		}
		//aqu� acaba lo que he a�adido

		//Now, check if the source and destination are in the TED.
		log.fine("Source: "+source_router_id_addr+"; Destination:"+dest_router_id_addr);
		if (!(((ted.containsVertex(source_router_id_addr))&&(ted.containsVertex(dest_router_id_addr))))){
			log.warning("Source or destination are NOT in the TED");	
			NoPath noPath= new NoPath();
			noPath.setNatureOfIssue(ObjectParameters.NOPATH_NOPATH_SAT_CONSTRAINTS);
			NoPathTLV noPathTLV=new NoPathTLV();
			if (!((ted.containsVertex(source_router_id_addr)))){
				log.finest("Unknown source");	
				noPathTLV.setUnknownSource(true);	
			}
			if (!((ted.containsVertex(dest_router_id_addr)))){
				log.finest("Unknown destination");
				noPathTLV.setUnknownDestination(true);	
			}

			noPath.setNoPathTLV(noPathTLV);				
			response.setNoPath(noPath);
			return m_resp;
		}


		boolean nopath=true;//Initially, we still have no path
		boolean end=false;//The search has not ended yet
		int lambda=0;//We begin with lambda index 0
		int lambda_chosen=0;//We begin with lambda index 0
		int central_freq=0; // It represents the central frequency slot n.
		int counter=0;
		boolean is_equal = false;
		
		log.fine("Starting the computation");
		GraphPath<Object,IntraDomainEdge> gp_chosen=null;
		GraphPath<Object,IntraDomainEdge> gp_trully_chosen=null;
		
		preComp.getGraphLock().lock();
		try{
			while (!end){
			
				SimpleDirectedWeightedGraph<Object,IntraDomainEdge> graphLambda=preComp.getNetworkGraphs().get(lambda); 
				DijkstraShortestPath<Object,IntraDomainEdge>  dsp=new DijkstraShortestPath<Object,IntraDomainEdge> (graphLambda, source_router_id_addr, dest_router_id_addr);
				GraphPath<Object,IntraDomainEdge> gp=dsp.getPath();
				
				if (gp==null){				
					//There is no path here
					counter=0;
					if (lambda>=preComp.getSSONInfo().getNumLambdas()-1){
						if (nopath==true){
							log.fine("No path found");
							NoPath noPath= new NoPath();
							noPath.setNatureOfIssue(ObjectParameters.NOPATH_NOPATH_SAT_CONSTRAINTS);
							NoPathTLV noPathTLV=new NoPathTLV();
							noPath.setNoPathTLV(noPathTLV);				
							response.setNoPath(noPath);
							return m_resp;
						}else {
							end=true;
						}

					}else {
						lambda=lambda+1;
					}
					
				}
				else{
					if (counter==0){
						if (lambda>=preComp.getSSONInfo().getNumLambdas()-1){
							if (nopath==true){
								log.fine("No path found");
								NoPath noPath= new NoPath();
								noPath.setNatureOfIssue(ObjectParameters.NOPATH_NOPATH_SAT_CONSTRAINTS);
								NoPathTLV noPathTLV=new NoPathTLV();
								noPath.setNoPathTLV(noPathTLV);				
								response.setNoPath(noPath);
								return m_resp;
							}else {
								end=true;
							}

						}
						else{
							log.info("LAMBDA "+lambda +" with metric "+gp.getWeight());
							gp_chosen=gp;
							counter=counter+1;
							lambda=lambda+1;
							lambda_chosen=lambda;
							end=false;							
						}
					}
					
					else{
						is_equal=grc.edges_comparator(gp, gp_chosen);
						if (is_equal == true){
							counter=counter+1;
							if (counter==num_slots){
									gp_trully_chosen=gp;
									central_freq=lambda-(int)((num_slots-1)/2);
									m=(num_slots)/2;
									counter=0;
									log.info("Central Frequency"+central_freq);
									log.info("Frequency width"+m);
									log.info("Path"+gp_trully_chosen);
									nopath=false;
									end=true;
								if (lambda>=preComp.getSSONInfo().getNumLambdas()-1){
									nopath=false;
									end=true;	
								}
								else {
									lambda=lambda+1;
								}
							}
							else{
								if (lambda>=preComp.getSSONInfo().getNumLambdas()-1){
									if (nopath==true){
										log.fine("No path found");
										NoPath noPath= new NoPath();
										noPath.setNatureOfIssue(ObjectParameters.NOPATH_NOPATH_SAT_CONSTRAINTS);
										NoPathTLV noPathTLV=new NoPathTLV();
										noPath.setNoPathTLV(noPathTLV);				
										response.setNoPath(noPath);
										return m_resp;
									}else {
										end=true;
									}	
								}else {
									lambda=lambda+1;
									lambda_chosen=lambda;
								}
							}
						}
						else {
							counter=0;
							gp_chosen=null;
							log.info("Camino Distinto en lambda "+lambda);
							if (lambda>=preComp.getSSONInfo().getNumLambdas()-1){
								if (nopath==true){
									log.fine("No path found");
									NoPath noPath= new NoPath();
									noPath.setNatureOfIssue(ObjectParameters.NOPATH_NOPATH_SAT_CONSTRAINTS);
									NoPathTLV noPathTLV=new NoPathTLV();
									noPath.setNoPathTLV(noPathTLV);				
									response.setNoPath(noPath);
									return m_resp;
								}else {
									end=true;
								}
							}
							else {
								end=false;
							}
						}
					}
				}
			}
		}
		finally{
			preComp.getGraphLock().unlock();	
		}

		if (nopath==false){

			Path path=new Path();
			ExplicitRouteObject ero= new ExplicitRouteObject();
			List<IntraDomainEdge> edge_list=gp_trully_chosen.getEdgeList();
			int i;
			
			for (i=0;i<edge_list.size();i++){
				UnnumberIfIDEROSubobject eroso= new UnnumberIfIDEROSubobject();
				eroso.setRouterID((Inet4Address)edge_list.get(i).getSource());
				eroso.setInterfaceID(edge_list.get(i).getSrc_if_id());
				eroso.setLoosehop(false);
				ero.addEROSubobject(eroso);
				
				
				//ITU-T Format
				DWDMWavelengthLabel WDMlabel=new DWDMWavelengthLabel();
				WDMlabel.setGrid(preComp.getSSONInfo().getGrid());
				WDMlabel.setChannelSpacing(preComp.getSSONInfo().getCs());
				WDMlabel.setN((central_freq)+preComp.getSSONInfo().getnMin());
				WDMlabel.setIdentifier(0);
				try {
					WDMlabel.encode();
				} catch (RSVPProtocolViolationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				GeneralizedLabelEROSubobject genLabel= new GeneralizedLabelEROSubobject();
				ero.addEROSubobject(genLabel);
				genLabel.setLabel(WDMlabel.getBytes());	
			}
			
			BandwidthRequestedGeneralizedBandwidth gw = new BandwidthRequestedGeneralizedBandwidth();
			GeneralizedBandwidthSSON GB_SSON = new GeneralizedBandwidthSSON();
			GB_SSON.setM(m);
			
			IPv4prefixEROSubobject eroso= new IPv4prefixEROSubobject();
			eroso.setIpv4address((Inet4Address)edge_list.get(edge_list.size()-1).getTarget());
			eroso.setPrefix(32);
			ero.addEROSubobject(eroso);
			path.seteRO(ero);
			path.setBandwidth(gw);
			PCEPUtils.completeMetric(path, req, edge_list);
			response.addPath(path);
			
			//FIXME: RESERVATION NEEDS TO BE IMPROVED!!!
			LinkedList<Object> sourceVertexList=new LinkedList<Object>();
			LinkedList<Object> targetVertexList=new LinkedList<Object>();
			for (i=0;i<edge_list.size();i++){
				sourceVertexList.add(edge_list.get(i).getSource());
				targetVertexList.add(edge_list.get(i).getTarget());
			}	
			sourceVertexList.add(edge_list.get(i-1).getSource());
			targetVertexList.add(edge_list.get(i-1).getTarget());
			
			if (req.getReservation()!=null){
			  reserv= new GenericLambdaReservation();
			  reserv.setResp(m_resp);
			  reserv.setLambda_chosen(central_freq);
			  reserv.setBidirectional(req.getRequestParameters().isBidirect());
			  reserv.setReservation(req.getReservation());
			  reserv.setSourceVertexList(sourceVertexList);
			  reserv.setTargetVertexList(targetVertexList);
			  reserv.setReservationManager(reservationManager);
			}
		
		}
		long tiempofin =System.nanoTime();
		long tiempotot=tiempofin-tiempoini;
		log.info("Ha tardado "+tiempotot+" nanosegundos");
		return m_resp;
	}

	public void setPreComp(AURE_FF_SSON_algorithmPreComputation preComp) {
		this.preComp = preComp;
	}

	public AlgorithmReservation getReserv() {
		return reserv;
	}


	


}
