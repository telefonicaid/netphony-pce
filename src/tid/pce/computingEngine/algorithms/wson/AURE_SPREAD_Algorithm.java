package tid.pce.computingEngine.algorithms.wson;

import java.net.Inet4Address;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.jgrapht.GraphPath;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;

import es.tid.pce.pcep.constructs.EndPoint;
import es.tid.pce.pcep.constructs.EndPointAndRestrictions;
import es.tid.pce.pcep.constructs.P2MPEndpoints;
import es.tid.pce.pcep.constructs.P2PEndpoints;
import es.tid.pce.pcep.constructs.Path;
import es.tid.pce.pcep.constructs.Request;
import es.tid.pce.pcep.constructs.Response;
import es.tid.pce.pcep.objects.EndPoints;
import es.tid.pce.pcep.objects.EndPointsIPv4;
import es.tid.pce.pcep.objects.ExplicitRouteObject;
import es.tid.pce.pcep.objects.GeneralizedEndPoints;
import es.tid.pce.pcep.objects.NoPath;
import es.tid.pce.pcep.objects.ObjectParameters;
import es.tid.pce.pcep.objects.RequestParameters;
import es.tid.pce.pcep.objects.tlvs.NoPathTLV;
import es.tid.rsvp.RSVPProtocolViolationException;
import es.tid.rsvp.constructs.gmpls.DWDMWavelengthLabel;
import es.tid.rsvp.objects.subobjects.GeneralizedLabelEROSubobject;
import es.tid.rsvp.objects.subobjects.IPv4prefixEROSubobject;
import es.tid.rsvp.objects.subobjects.UnnumberIfIDEROSubobject;
import tid.pce.computingEngine.ComputingRequest;
import tid.pce.computingEngine.ComputingResponse;
import tid.pce.computingEngine.algorithms.AlgorithmReservation;
import tid.pce.computingEngine.algorithms.ComputingAlgorithm;
import tid.pce.computingEngine.algorithms.PCEPUtils;
import tid.pce.server.wson.ReservationManager;
import tid.pce.tedb.DomainTEDB;
import tid.pce.tedb.IntraDomainEdge;
import tid.pce.tedb.TEDB;

public class AURE_SPREAD_Algorithm implements ComputingAlgorithm {
	/**
	 * The Logger.
	 */
	private Logger log=Logger.getLogger("PCEServer");
	/**
	 * The Path Computing Request to calculate.
	 */
	private ComputingRequest pathReq;
	/**
	 * Access to the Pre computation part of the algorithm.
	 */
	private AURE_SPREAD_AlgorithmPreComputation preComp;
	
	/**
	 * Access to the Reservation Manager to make reservations of Wavelengths/labels.
	 */
	private ReservationManager reservationManager;
	
//	/**
//	 * Number of wavelengths (labels).
//	 */
//	private int num_lambdas;
	
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
	public AURE_SPREAD_Algorithm(ComputingRequest pathReq,TEDB ted, ReservationManager reservationManager ){
		//this.num_lambdas=((DomainTEDB)ted).getNumLambdas();
		this.pathReq=pathReq;
		this.reservationManager=reservationManager;
		this.ted=(DomainTEDB)ted;
	}

	/**
	 * Exectutes the path computation and returns the PCEP Response
	 */
	public ComputingResponse call(){ 
		//Time stamp of the start of the algorithm;
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
		EndPoints  EP= req.getEndPoints();	
		Object source_router_id_addr = null;
		Object dest_router_id_addr = null;

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
		//aqui acaba lo que he añadido

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
		
		int lambda_chosen=0;//We begin with lambda index 0
		Integer lambda=0;//We begin with lambda index 0
		boolean end=false;//The search has not ended yet
		Hashtable<Integer,GraphPath<Object,IntraDomainEdge>> LambdaRouteTable = new Hashtable<Integer,GraphPath<Object,IntraDomainEdge>>();
		ArrayList<Integer> Lambda_list = new ArrayList<Integer>();
		
		int num_lambdas = ted.getWSONinfo().getNumLambdas();
				
		log.fine("Starting the computation");
		
		/*GraphPath<Object,IntraDomainEdge> gp_chosen=null;
		
		//SimpleDirectedWeightedGraph<Object, IntraDomainEdge> Graph=preComp.getNetworkGraphs();
				
		DijkstraShortestPath<Object,IntraDomainEdge>  dsp=new DijkstraShortestPath<Object,IntraDomainEdge> (Graph, source_router_id_addr, dest_router_id_addr);
		
		GraphPath<Object,IntraDomainEdge> gp=dsp.getPath();
		
		if (gp == null){
			log.fine("No path found");
		}
		
		else {
			nopath = false;		
			gp_chosen = gp; //graph path
		}*/
		
		int k=0;
		//log.severe("Peticion nueva");
		GraphPath<Object,IntraDomainEdge> gp_chosen=null;
		preComp.getGraphLock().lock();
		try{
			while (!end){
				
				SimpleDirectedWeightedGraph<Object,IntraDomainEdge> graphLambda=preComp.getNetworkGraphs().get(lambda); 
				DijkstraShortestPath<Object,IntraDomainEdge>  dsp=new DijkstraShortestPath<Object,IntraDomainEdge> (graphLambda, source_router_id_addr, dest_router_id_addr);
				GraphPath<Object,IntraDomainEdge> gp=dsp.getPath();
				//log.severe("Entro");
				if (gp==null){				
					//There is no path here
					if (lambda>=preComp.getWSONInfo().getNumLambdas()-1){
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
				else {
					/*if (lambda == 0){
						log.severe("Hay camino para lambda : 0");
					}
					if (lambda == 1){
						log.severe("Hay camino para lambda : 1");
					}*/
					k++;
					nopath=false;
					Lambda_list.add(lambda);
					LambdaRouteTable.put(lambda, gp);
				
					if (lambda>=preComp.getWSONInfo().getNumLambdas()-1){
						end=true;	
					}else {
						lambda=lambda+1;
					}
				}
			}

		}finally{
			preComp.getGraphLock().unlock();	
		}
		
		if (nopath == false){
			
			//iterate all graph edges
			Set<IntraDomainEdge> edge= preComp.getBaseSimplegraph().edgeSet();
			Iterator<IntraDomainEdge> iter_edges = edge.iterator();
			int num_links = 68;
			
			int[][] network_state = new int[num_links][num_lambdas];
			int[] lambda_use= new int[num_lambdas];
			int i;
			for(i=0; i<num_lambdas; i++){
				lambda_use[i] = 0;
			}
			
			int u=0; //lambda
			int lambda_max_use = 69;
			int lambda_max_use_id;
			int j;
			for (i=0; i<num_links; i++)
			{
				IntraDomainEdge actual_edge=iter_edges.next();
				
				for (j=0; j<k;j++)
				{	
					u = Lambda_list.get(j);
					if (actual_edge.getTE_info().isWavelengthFree(u)==true)
					{
						network_state[i][u] = 0;
					}
					else{
						network_state[i][u] = 1;
					}
					lambda_use[u] = lambda_use[u] +  network_state[i][u];
					if ((lambda_use[u] < lambda_max_use) && (actual_edge.getTE_info().isWavelengthUnreserved(u)== true))
					{
						//System.out.println("Entro aquí");
						lambda_max_use_id = u;
						lambda_chosen = lambda_max_use_id;
						lambda_max_use = lambda_use[u];
						
					}
				}
				i++;
			}
			
			gp_chosen = LambdaRouteTable.get(lambda_chosen);
			
			Path path=new Path();
			
			ExplicitRouteObject ero= new ExplicitRouteObject();
			List<IntraDomainEdge> edge_list=gp_chosen.getEdgeList();
			
						
			for (i=0;i<edge_list.size();i++){
				UnnumberIfIDEROSubobject eroso= new UnnumberIfIDEROSubobject();
				eroso.setRouterID((Inet4Address)edge_list.get(i).getSource());
				eroso.setInterfaceID(edge_list.get(i).getSrc_if_id());
				eroso.setLoosehop(false);
				ero.addEROSubobject(eroso);
				
				GeneralizedLabelEROSubobject genLabel= new GeneralizedLabelEROSubobject();
				ero.addEROSubobject(genLabel);
				//ITU-T Format
				DWDMWavelengthLabel WDMlabel=new DWDMWavelengthLabel();
				WDMlabel.setGrid(preComp.getWSONInfo().getGrid());
				WDMlabel.setChannelSpacing(preComp.getWSONInfo().getCs());
				WDMlabel.setN(lambda_chosen+preComp.getWSONInfo().getnMin());
				WDMlabel.setIdentifier(0);
				try {
					WDMlabel.encode();
				} catch (RSVPProtocolViolationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				genLabel.setLabel(WDMlabel.getBytes());		
			}

			IPv4prefixEROSubobject eroso= new IPv4prefixEROSubobject();
			eroso.setIpv4address((Inet4Address)edge_list.get(edge_list.size()-1).getTarget());
			eroso.setPrefix(32);
			ero.addEROSubobject(eroso);
			path.seteRO(ero);
			PCEPUtils.completeMetric(path, req, edge_list);
			response.addPath(path);

			//FIXME: RESERVATION NEEDS TO BE IMPROVED!!!
			LinkedList<Object> sourceVertexList=new LinkedList<Object>();
			LinkedList<Object> targetVertexList=new LinkedList<Object>();
			for (i=0;i<edge_list.size();i++){
				sourceVertexList.add(edge_list.get(i).getSource());
				targetVertexList.add(edge_list.get(i).getTarget());
			}
			
			if (req.getReservation()!=null){
				  reserv= new GenericLambdaReservation();
				  reserv.setResp(m_resp);
				  reserv.setLambda_chosen(lambda_chosen);
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
	
	public void setPreComp(AURE_SPREAD_AlgorithmPreComputation preComp) {
		this.preComp = preComp;
	}
	
	public AlgorithmReservation getReserv() {
		return reserv;
	}
}
