package tid.pce.computingEngine.algorithms.wlan;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import org.jgrapht.GraphPath;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;

import tid.pce.computingEngine.ComputingRequest;
import tid.pce.computingEngine.ComputingResponse;
import tid.pce.computingEngine.algorithms.AlgorithmReservation;
import tid.pce.computingEngine.algorithms.ComputingAlgorithm;
import tid.pce.computingEngine.algorithms.PCEPUtils;
import tid.pce.pcep.constructs.Path;
import tid.pce.pcep.constructs.Request;
import tid.pce.pcep.constructs.Response;
import tid.pce.pcep.objects.EndPoints;
import tid.pce.pcep.objects.EndPointsIPv4;
import tid.pce.pcep.objects.ExplicitRouteObject;
import tid.pce.pcep.objects.NoPath;
import tid.pce.pcep.objects.ObjectParameters;
import tid.pce.pcep.objects.RequestParameters;
import tid.pce.pcep.objects.tlvs.NoPathTLV;
import tid.pce.server.wson.ReservationManager;
import tid.pce.tedb.DomainTEDB;
import tid.pce.tedb.IntraDomainEdge;
import tid.pce.tedb.TEDB;
import tid.protocol.commons.ByteHandler;
import tid.rsvp.RSVPProtocolViolationException;
import tid.rsvp.constructs.WLANLabel;
import tid.rsvp.objects.subobjects.GeneralizedLabelEROSubobject;
import tid.rsvp.objects.subobjects.SwitchIDEROSubobject;

/**
 * Implementation of the algorithm "Adaptive Unconstrained Routing Exhaustive".
 * 
 * <p>Reference: A. Mokhtar y M. Azizoglu, "Adaptive wavelength routing in all-optical networks",
 * IEEE/ACM Transactions on Networking, vol. 6, no.2 pp. 197 - 201, abril 1998</p>
 * @author jaume
 *
 */
public class AURE_WLAN_algorithm implements ComputingAlgorithm {

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
	private AURE_WLAN_algorithmPreComputation preComp;
	
	/**
	 * Access to the Reservation Manager to make reservations of Wavalengths/labels.
	 */
	private ReservationManager reservationManager;
	
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
	public AURE_WLAN_algorithm(ComputingRequest pathReq,TEDB ted, ReservationManager reservationManager ){
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
		Object source_router_id_addr = null;
		Object dest_router_id_addr = null;

		if (EP.getOT()==ObjectParameters.PCEP_OBJECT_TYPE_ENDPOINTS_IPV4){
			EndPointsIPv4  ep=(EndPointsIPv4) req.getEndPoints();
			source_router_id_addr=ep.getSourceIP();
			dest_router_id_addr=ep.getDestIP();
		}else if (EP.getOT()==ObjectParameters.PCEP_OBJECT_TYPE_ENDPOINTS_IPV6){

		}else if (EP.getOT()==ObjectParameters.PCEP_OBJECT_TYPE_GENERALIZED_ENDPOINTS){
			log.warning("Error: Not supported yet");
		}
		//aqu� acaba lo que he a�adido

		//Now, check if the source and destination are in the TED.
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

		double max_metric=Integer.MAX_VALUE;
		log.fine("Starting the computation");
		GraphPath<Object,IntraDomainEdge> gp_chosen=null;
		preComp.getGraphLock().lock();
		try{
			while (!end){
				SimpleDirectedWeightedGraph<Object,IntraDomainEdge> graphLambda=preComp.getNetworkGraphs().get(lambda); 
				DijkstraShortestPath<Object,IntraDomainEdge>  dsp=new DijkstraShortestPath<Object,IntraDomainEdge> (graphLambda, source_router_id_addr, dest_router_id_addr);
				GraphPath<Object,IntraDomainEdge> gp=dsp.getPath();
				if (gp==null){				
					//There is no path here
					if (lambda>=preComp.getNumWLANs() -1){
						if (nopath==true){
							log.info("No path found");
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
				else {
					if (gp.getWeight()<max_metric){
						log.info("LAMBDA "+lambda+" with metric "+gp.getWeight());
						gp_chosen=gp;
						lambda_chosen=lambda;
						nopath=false;
						max_metric=gp.getWeight();

					}
					if (lambda>=preComp.getNumWLANs()-1){
						end=true;	
					}else {
						lambda=lambda+1;
					}
				}
			}

		}finally{
			preComp.getGraphLock().unlock();	
		}

		if (nopath==false)
		{

			Path path=new Path();
			ExplicitRouteObject ero= new ExplicitRouteObject();
			List<IntraDomainEdge> edge_list=gp_chosen.getEdgeList();
			int i;
			for (i=0;i<edge_list.size();i++){
				SwitchIDEROSubobject eroso= new SwitchIDEROSubobject();
				eroso.setSwitchID(ByteHandler.MACFormatStringtoByteArray((String)edge_list.get(i).getSource()));
				eroso.setLoosehop(false);
				ero.addEROSubobject(eroso);
				
				GeneralizedLabelEROSubobject genLabel= new GeneralizedLabelEROSubobject();
				ero.addEROSubobject(genLabel);
				//ITU-T Format
				WLANLabel wlanlabel=new WLANLabel();
				wlanlabel.setIdentifier(0);
				wlanlabel.setN(lambda_chosen);
				try {
					wlanlabel.encode();
				} catch (RSVPProtocolViolationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				genLabel.setLabel(wlanlabel.getBytes());		
				
			}
			SwitchIDEROSubobject eroso= new SwitchIDEROSubobject();
			eroso.setSwitchID(ByteHandler.MACFormatStringtoByteArray((String)edge_list.get(edge_list.size()-1).getTarget()));
			ero.addEROSubobject(eroso);
			path.seteRO(ero);
			PCEPUtils.completeMetric(path, req, edge_list);
			response.addPath(path);
			
			//log.info("Resp: "+response.toString());

			//FIXME: RESERVATION NEEDS TO BE IMPROVED!!!
			LinkedList<Object> sourceVertexList=new LinkedList<Object>();
			LinkedList<Object> targetVertexList=new LinkedList<Object>();
			LinkedList<Integer> wlans=new LinkedList<Integer>();
			
			for (i=0;i<edge_list.size();i++){
				sourceVertexList.add(edge_list.get(i).getSource());
				targetVertexList.add(edge_list.get(i).getTarget());
				wlans.add(lambda_chosen);
			}		
			
			
			if (req.getReservation()!=null){
				reserv= new GenericWLANReservation();
				reserv.setResp(m_resp);
				reserv.setWLANList(wlans);
				reserv.setReservation(req.getReservation());
				reserv.setSourceVertexList(sourceVertexList);
				reserv.setTargetVertexList(targetVertexList);
				
				if (rp.isBidirect() == true){
					reserv.setBidirectional(true);
				}
				else{
					reserv.setBidirectional(false);
				}
				
				reserv.setReservationManager(reservationManager);
			}
		}
		long tiempofin =System.nanoTime();
		long tiempotot=tiempofin-tiempoini;
		log.info("Ha tardado "+tiempotot+" nanosegundos");
		return m_resp;
	}

	public void setPreComp(AURE_WLAN_algorithmPreComputation preComp) {
		this.preComp = preComp;
	}

	public AlgorithmReservation getReserv() {
		return reserv;
	}
}
