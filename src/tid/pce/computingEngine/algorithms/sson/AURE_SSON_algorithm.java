package tid.pce.computingEngine.algorithms.sson;

import java.net.Inet4Address;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Logger;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;
import tid.pce.computingEngine.ComputingRequest;
import tid.pce.computingEngine.ComputingResponse;
import tid.pce.computingEngine.algorithms.AlgorithmReservation;
import tid.pce.computingEngine.algorithms.ComputingAlgorithm;
import tid.pce.computingEngine.algorithms.PCEPUtils;
import tid.pce.pcep.constructs.EndPoint;
import tid.pce.pcep.constructs.EndPointAndRestrictions;
import tid.pce.pcep.constructs.P2MPEndpoints;
import tid.pce.pcep.constructs.P2PEndpoints;
import tid.pce.pcep.constructs.Path;
import tid.pce.pcep.constructs.Request;
import tid.pce.pcep.constructs.Response;
import tid.pce.pcep.objects.Bandwidth;
import tid.pce.pcep.objects.GeneralizedBandwidth;
import tid.pce.pcep.objects.EndPoints;
import tid.pce.pcep.objects.EndPointsIPv4;
import tid.pce.pcep.objects.ExplicitRouteObject;
import tid.pce.pcep.objects.GeneralizedBandwidthSSON;
import tid.pce.pcep.objects.GeneralizedEndPoints;
import tid.pce.pcep.objects.Metric;
import tid.pce.pcep.objects.Monitoring;
import tid.pce.pcep.objects.NoPath;
import tid.pce.pcep.objects.ObjectParameters;
import tid.pce.pcep.objects.RequestParameters;
import tid.pce.pcep.objects.ReservationConf;
import tid.pce.pcep.objects.tlvs.NoPathTLV;
import tid.pce.server.wson.ReservationManager;
import tid.pce.tedb.IntraDomainEdge;
import tid.pce.tedb.SSONInformation;
import tid.pce.tedb.TEDB;
import tid.pce.tedb.DomainTEDB;
import tid.rsvp.RSVPProtocolViolationException;
import tid.rsvp.constructs.gmpls.DWDMWavelengthLabel;
import tid.rsvp.objects.SSONSenderTSpec;
import tid.rsvp.constructs.gmpls.DWDMWavelengthLabelValues;
import tid.rsvp.objects.subobjects.GeneralizedLabelEROSubobject;
import tid.rsvp.objects.subobjects.IPv4prefixEROSubobject;
import tid.rsvp.objects.subobjects.UnnumberIfIDEROSubobject;
import tid.pce.computingEngine.algorithms.utilities.*;
import tid.ospf.ospfv2.lsa.tlv.subtlv.complexFields.BitmapLabelSet;

/**
 * Implementation of the algorithm "Adaptive Unconstrained Routing Exhaustive".
 * 
 * <p>Reference: A. Mokhtar y M. Azizoglu, "Adaptive wavelength routing in all-optical networks",
 * IEEE/ACM Transactions on Networking, vol. 6, no.2 pp. 197 - 201, abril 1998</p>
 * @author arturo mayoral
 *
 */
public class AURE_SSON_algorithm implements ComputingAlgorithm {

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
	private AURE_SSON_algorithmPreComputation preComp;
	
	/**
	 * Access to the Reservation Manager to make reservations of Wavalengths/labels.
	 */
	private ReservationManager reservationManager;
	
	
	private SSONInformation SSONInfo;
	
//	/**
//	 * Number of wavelenghts (labels).
//	 */
	//private int num_lambdas;
	
	/**
	 * The traffic engineering database
	 */
	private DomainTEDB ted;
	
	
	private GenericLambdaReservation  reserv;
	/**
	 * Constructor
	 * @param pathReq
	 * @param ted
	 * @param reservationManager
	 */
	public AURE_SSON_algorithm(ComputingRequest pathReq,TEDB ted, ReservationManager reservationManager, int mf){
		//this.num_lambdas=((DomainTEDB)ted).getWSONinfo().getNumLambdas();
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
		Bandwidth  Bw= req.getBandwidth(); // Objeto bandwidth para saber la demanda de la peticion.
		Object source_router_id_addr = null;
		Object dest_router_id_addr = null;
		graphs_comparator grc = new graphs_comparator ();
		
		log.info("BW: "+Bw.getBw());
		
		int num_slots = 0;
		int cs;
		int m=0;
		// Conversión Bw a numero de slots en función de la grid.

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
		log.info("Source: "+source_router_id_addr+"; Destination:"+dest_router_id_addr);
		if (!(((ted.containsVertex(source_router_id_addr))&&(ted.containsVertex(dest_router_id_addr))))){
			log.info("Source or destination are NOT in the TED");	
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
		// check if src and dst are the same 
		if (source_router_id_addr.equals(dest_router_id_addr)){
			log.info("Source and destination are the same!");
			Path path=new Path();
			ExplicitRouteObject ero= new ExplicitRouteObject();
			IPv4prefixEROSubobject eroso= new IPv4prefixEROSubobject();
			eroso.setIpv4address((Inet4Address)source_router_id_addr);
			eroso.setPrefix(32);
			ero.addEROSubobject(eroso);
			path.seteRO(ero);
			
			if (req.getMetricList().size()!=0){
				Metric metric=new Metric();
				metric.setMetricType(req.getMetricList().get(0).getMetricType() );
				log.fine("Number of hops "+0);
				float metricValue=0;
				metric.setMetricValue(metricValue);
				path.getMetricList().add(metric);
			}
			response.addPath(path);
			long tiempofin =System.nanoTime();
			long tiempotot=tiempofin-tiempoini;
			log.info("Ha tardado "+tiempotot+" nanosegundos");
			Monitoring monitoring=pathReq.getMonitoring();
			if (monitoring!=null){
				if (monitoring.isProcessingTimeBit()){
					
				}
			}
			m_resp.addResponse(response);
			return m_resp;
			
		}


		boolean nopath=true;//Initially, we still have no path
		boolean end=false;//The search has not ended yet
		int lambda=0;//We begin with lambda index 0
		int lambda_chosen=0;//We begin with lambda index 0
		int central_freq=0; // It represents the central frequency slot n.
		int counter=0;
		boolean is_equal = false;
		
		double max_metric=Integer.MAX_VALUE;
		log.info("Starting the computation");
		GraphPath<Object,IntraDomainEdge> gp_chosen=null;
		GraphPath<Object,IntraDomainEdge> gp_trully_chosen=null;
		
		preComp.getGraphLock().lock();
		try{
			while (!end){
				SimpleDirectedWeightedGraph<Object,IntraDomainEdge> graphLambda=preComp.getNetworkGraphs().get(lambda);
				
				//log.info("Grafo ok "+lambda);
				//log.info("Grafo : "+preComp.printBaseTopology());
				DijkstraShortestPath<Object,IntraDomainEdge>  dsp=new DijkstraShortestPath<Object,IntraDomainEdge> (graphLambda, source_router_id_addr, dest_router_id_addr);
				GraphPath<Object,IntraDomainEdge> gp=dsp.getPath();
				//log.info("DSP completed");
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
							if (gp.getWeight()<max_metric){
								log.info("LAMBDA "+lambda +" with metric "+gp.getWeight());
								gp_chosen=gp;
								counter=counter+1;
								lambda=lambda+1;
								lambda_chosen=lambda;
								end=false;
							}
							else{
								lambda=lambda+1;
								counter=0;
							}
						}
					}
					
					else{
						is_equal=grc.edges_comparator(gp, gp_chosen);
						if (is_equal == true){
							counter=counter+1;
							if (counter==num_slots){
									gp_trully_chosen=gp;
									max_metric=gp.getWeight();
									central_freq=lambda-(int)((num_slots-1)/2);
									m=(num_slots)/2;
									counter=0;
									log.info("Central Frequency"+central_freq);
									log.info("Frequency width"+m);
									log.info("Path"+gp_trully_chosen);
									nopath=false;
									//end=true;
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
				preComp.getGraphLock().lock();
				//FIXME: Temp, only for HPCE algorithms
				log.info("Lets remove: "+m+(central_freq)+preComp.getSSONInfo().getnMin()+edge_list.get(i).getSource()+edge_list.get(i).getTarget());
				preComp.setReservation(m, (central_freq)+preComp.getSSONInfo().getnMin(), (Inet4Address)edge_list.get(i).getSource(), (Inet4Address)edge_list.get(i).getTarget());
			
				for (int p=(central_freq)+preComp.getSSONInfo().getnMin()-m; p<(central_freq)+preComp.getSSONInfo().getnMin()+m; p++) {
					log.info("Borramos: "+p);
					preComp.getNetworkGraphs().get(p).removeEdge(edge_list.get(i).getSource(), edge_list.get(i).getTarget());
				}
				
				preComp.getGraphLock().unlock();
				

				//ITU-T Format
				DWDMWavelengthLabel WDMlabel=new DWDMWavelengthLabel();
				WDMlabel.setGrid(preComp.getSSONInfo().getGrid());
				WDMlabel.setChannelSpacing(preComp.getSSONInfo().getCs());
				WDMlabel.setN((central_freq)+preComp.getSSONInfo().getnMin());
				WDMlabel.setM(m);
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
				
				if(req.getRequestParameters().isBidirect()==true){
					//ITU-T Format
					DWDMWavelengthLabel WDMlabel_bidirect=new DWDMWavelengthLabel();
					WDMlabel_bidirect.setGrid(preComp.getSSONInfo().getGrid());
					WDMlabel_bidirect.setChannelSpacing(preComp.getSSONInfo().getCs());
					WDMlabel_bidirect.setN((central_freq)+preComp.getSSONInfo().getnMin());
					WDMlabel_bidirect.setM(m);
					WDMlabel_bidirect.setIdentifier(0);
					try {
						WDMlabel_bidirect.encode();
					} catch (RSVPProtocolViolationException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					GeneralizedLabelEROSubobject genLabel_bidirect= new GeneralizedLabelEROSubobject();
					short Ubit = 1;
					genLabel_bidirect.setUbit(Ubit);
					ero.addEROSubobject(genLabel_bidirect);
					genLabel_bidirect.setLabel(WDMlabel_bidirect.getBytes());	
				}
				
			}
			
			//log.info("Label bit map: "+ted.getWSONinfo().getCommonAvailableLabels().getLabelSet().toString());
			GeneralizedBandwidthSSON GB_SSON = new GeneralizedBandwidthSSON ();
			
//			SSONSenderTSpec mLabel = new SSONSenderTSpec();
//          mLabel.setM(m);
//          mLabel.encode();
          
	        GB_SSON.setM(m);
	        GB_SSON.setRreverse(false);
	        GB_SSON.setOreopt(false);
			
			IPv4prefixEROSubobject eroso= new IPv4prefixEROSubobject();
			eroso.setIpv4address((Inet4Address)edge_list.get(edge_list.size()-1).getTarget());
			eroso.setPrefix(32);
			ero.addEROSubobject(eroso);
			path.seteRO(ero);
			path.setGeneralizedbandwidth(GB_SSON);
			PCEPUtils.completeMetric(path, req, edge_list);
			response.setBandwidth(Bw);
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

	public void setPreComp(AURE_SSON_algorithmPreComputation preComp) {
		this.preComp = preComp;
	}

	public AlgorithmReservation getReserv() {
		return reserv;
	}	


}
