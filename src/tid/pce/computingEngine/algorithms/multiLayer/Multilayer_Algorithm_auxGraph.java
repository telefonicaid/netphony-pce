package tid.pce.computingEngine.algorithms.multiLayer;

import java.net.Inet4Address;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;
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
import es.tid.pce.pcep.objects.BandwidthRequested;
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
import es.tid.rsvp.objects.subobjects.ServerLayerInfo;
import es.tid.rsvp.objects.subobjects.UnnumberIfIDEROSubobject;
import es.tid.tedb.DomainTEDB;
import es.tid.tedb.IntraDomainEdge;
import es.tid.tedb.MultiLayerTEDB;
import es.tid.tedb.TEDB;
import tid.pce.computingEngine.ComputingRequest;
import tid.pce.computingEngine.ComputingResponse;
import tid.pce.computingEngine.algorithms.AlgorithmReservation;
import tid.pce.computingEngine.algorithms.ComputingAlgorithm;
import tid.pce.computingEngine.algorithms.PCEPUtils;
import tid.pce.computingEngine.algorithms.multiLayer.Multilayer_Algorithm_auxGraphPreComputation.InfoNodo;
import tid.pce.computingEngine.algorithms.wson.GenericLambdaReservation;
import tid.pce.server.wson.ReservationManager;

public class Multilayer_Algorithm_auxGraph implements ComputingAlgorithm{
	/**
	 * The Logger.
	 */
	private Logger log=Logger.getLogger("PCEServer");
	private Logger log_OP=Logger.getLogger("OpMultiLayer");
	/**
	 * The Path Computing Request to calculate.
	 */
	private ComputingRequest pathReq;
	/**
	 * Access to the Pre computation part of the algorithm.
	 */
	private Multilayer_Algorithm_auxGraphPreComputation preComp;
	
	private Lock graphLock;
	/**
	 * Access to the Reservation Manager to make reservations of Wavelengths/labels.
	 */
	private ReservationManager reservationManager;
	
//	/**
//	 * Number of wavelenghts (labels).
//	 */
//	private int num_lambdas;
	
	private Lock contadoresLock;
	
	/**
	 * The traffic engineering database
	 */
	private DomainTEDB ted;
	
	private GenericLambdaReservation  reserv;
	
	private OperationsCounter OP_Counter;
	
	/**
	 * Constructor
	 * @param pathReq
	 * @param ted
	 * @param reservationManager
	 */
	public Multilayer_Algorithm_auxGraph(ComputingRequest pathReq,TEDB ted, ReservationManager reservationManager, OperationsCounter OPcounter ){
		//this.num_lambdas=((DomainTEDB)ted).getNumLambdas();
		this.pathReq=pathReq;
		this.reservationManager=reservationManager;
		this.ted=(DomainTEDB)ted;
		this.OP_Counter=OPcounter;
	}

	/**
	 * Exectutes the path computation and returns the PCEP Response
	 */
	public ComputingResponse call(){ 
		
		graphLock = preComp.getGraphLock();
		int num_Layer_changes=0;
		//Time stamp of the start of the algorithm;
		long tiempoini =System.nanoTime();
		log.info("Starting AURE Algorithm, new path Request!");
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
		
		BandwidthRequested bandwidth = new BandwidthRequested();
		bandwidth.setBw(((BandwidthRequested)req.getBandwidth()).getBw());
		response.setBandwidth(bandwidth);
		
		response.setRequestParameters(rp);
		m_resp.addResponse(response);
		
		//esto hay que cambiarlo para poder leer del GENERALIZED END POINTS
		//if (getObjectType(req.getEndPoints()))
		EndPoints  EP= req.getEndPoints();	
		Object source_router_id_addr = null;
		Object dest_router_id_addr = null;
		
		Object source_aux_graph = null;
		Object dest_aux_graph = null;
		
		SimpleDirectedWeightedGraph<Object,IntraDomainEdge> graph=preComp.getbaseSimplegraph();
		
		/*Set<Object> nodes= graph.vertexSet();
		Iterator<Object> iternodes = nodes.iterator();
		Object Node;
		
		while (iternodes.hasNext())
		{
			Node = iternodes.next();  //IP of the current node
		}
		
		Set<IntraDomainEdge> links = graph.edgeSet();
		Iterator<IntraDomainEdge> iteredges = links.iterator();
		IntraDomainEdge link;
		while (iteredges.hasNext())
		{
			link = iteredges.next();  //IP of the current node
			
			// borramos los links que no tengan suficiente ancho de banda
			if (link.getTE_info() != null){
			}
			else
		}*/
			
		if (EP.getOT()==ObjectParameters.PCEP_OBJECT_TYPE_ENDPOINTS_IPV4){
			EndPointsIPv4  ep=(EndPointsIPv4) req.getEndPoints();
			source_router_id_addr=ep.getSourceIP();
			dest_router_id_addr=ep.getDestIP();
			
			preComp.getRelation_access_nodes();		
			source_aux_graph = preComp.getRelation_access_nodes().get(source_router_id_addr);
			
			Integer number_src_out = (preComp.getInfo_IPv4_IntNodes().get(source_aux_graph)) + 1;
			source_aux_graph = preComp.getinfo_IntNodes_IPv4().get(number_src_out);
			dest_aux_graph = preComp.getRelation_access_nodes().get(dest_router_id_addr);
									
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
		log.fine("Source: "+source_aux_graph+"; Destination:"+dest_aux_graph);
		if (!(((ted.containsVertex(source_aux_graph))&&(ted.containsVertex(dest_aux_graph))))){
			log.warning("Source or destination are NOT in the TED");	
			NoPath noPath= new NoPath();
			noPath.setNatureOfIssue(ObjectParameters.NOPATH_NOPATH_SAT_CONSTRAINTS);
			NoPathTLV noPathTLV=new NoPathTLV();
			if (!((MultiLayerTEDB)ted).containsVertex(source_aux_graph)){
				log.info("Unknown source");
				noPathTLV.setUnknownSource(true);	
			}
			if (!((MultiLayerTEDB)ted).containsVertex(dest_aux_graph)){
				log.info("Unknown destination");
				noPathTLV.setUnknownDestination(true);	
			}

			noPath.setNoPathTLV(noPathTLV);				
			response.setNoPath(noPath);
			return m_resp;
		}
						
		boolean nopath=true;//Initially, we still have no path
		int lambda_chosen=0;//We begin with lambda index 0
		GraphPath<Object,IntraDomainEdge> gp_chosen=null;
		//grafo duplicado con restricciones
		SimpleDirectedWeightedGraph<Object,IntraDomainEdge> constrained_graph = new SimpleDirectedWeightedGraph<Object,IntraDomainEdge>(IntraDomainEdge.class);
		GraphPath<Object,IntraDomainEdge> gp;
		graphLock.lock();
		try{
			constrained_graph= (SimpleDirectedWeightedGraph<Object, IntraDomainEdge>) graph.clone();
						
			/*Set<Object> nodes1= graph.vertexSet();
			Iterator<Object> iternodes1 = nodes1.iterator();
			Object Node1;
			
			while (iternodes1.hasNext())
			{
				Node1 = iternodes1.next();  //IP of the current node
			}*/
			
			Set<IntraDomainEdge> links1 = graph.edgeSet();
			Iterator<IntraDomainEdge> iteredges1 = links1.iterator();
			IntraDomainEdge link1;
					
			while (iteredges1.hasNext())
			{
				link1 = iteredges1.next();  //IP of the current node
				// borramos los links que no tengan suficiente ancho de banda
				if (link1.getTE_info() != null){
					if ((link1.getTE_info().getMaximumReservableBandwidth().maximumReservableBandwidth)<(((BandwidthRequested)req.getBandwidth()).getBw())){
						constrained_graph.removeEdge(link1);
					}
				}
			}			
			DijkstraShortestPath<Object,IntraDomainEdge>  dsp=new DijkstraShortestPath<Object,IntraDomainEdge> (constrained_graph, source_aux_graph, dest_aux_graph);
			gp=dsp.getPath();
		} finally{
			graphLock.unlock();
		}
		if (gp != null){
			nopath = false;
			gp_chosen = gp;
		} if (nopath==true){
			log.info("No path found"); // NO PATH FOUND
			NoPath noPath= new NoPath();
			noPath.setNatureOfIssue(ObjectParameters.NOPATH_NOPATH_SAT_CONSTRAINTS);
			NoPathTLV noPathTLV=new NoPathTLV();
			noPath.setNoPathTLV(noPathTLV);				
			response.setNoPath(noPath);
			
			log_OP.info("NumOP_1: "+OP_Counter.getNum_op1());
			log_OP.info("NumOP_2: "+OP_Counter.getNum_op2());
			log_OP.info("NumOP_3: "+OP_Counter.getNum_op3());
			log_OP.info("NumOP_4: "+OP_Counter.getNum_op4());
			log_OP.info("MeanLayerChanges: "+OP_Counter.getMean_layer_changes()+"\n\n");
			
			return m_resp;
		}
		
		int NumLambdas = ted.getWSONinfo().getNumLambdas();
				
		Hashtable<Object, Integer> Info_IPv4_IntNodes = preComp.getInfo_IPv4_IntNodes();
		Hashtable<Integer, InfoNodo> infoTable = preComp.getInfoTable();
				
		if (nopath == false){
			Path path=new Path();
			int traffic_hops = 0;
			ExplicitRouteObject ero= new ExplicitRouteObject();
			
			List<IntraDomainEdge> edge_list = gp_chosen.getEdgeList();
			boolean flag = false;
			int j=0;
			Object Nodo_aux_actual_IP;
			Integer Nodo_aux_actual; 
			
			Object node_aux_target_IP;
			Integer node_aux_target;
			
			InfoNodo info_actual;
			InfoNodo info_next;
			
			for (j=0; j<edge_list.size(); j++){
				IntraDomainEdge link_aux = edge_list.get(j);
				
				Nodo_aux_actual_IP = edge_list.get(j).getSource();
				Nodo_aux_actual = Info_IPv4_IntNodes.get(Nodo_aux_actual_IP);
				
				node_aux_target_IP = link_aux.getTarget();
				node_aux_target = Info_IPv4_IntNodes.get(node_aux_target_IP);
				info_actual = infoTable.get(Nodo_aux_actual);		
				info_next = infoTable.get(node_aux_target);
							
				if (info_actual.getnumLayer() == (NumLambdas+2)){ //Nodo actual capa IP/MPLS
					if ((info_next.getnumLayer()<=NumLambdas)&&(info_next.getnumLayer()>=1)){
						//cambiamos a la capa ÓPTICA
						num_Layer_changes++;
						IPv4prefixEROSubobject eroso= new IPv4prefixEROSubobject();
						eroso.setIpv4address((Inet4Address)info_actual.getIPnode());
						eroso.setLoosehop(false);
						ero.addEROSubobject(eroso);
						ServerLayerInfo eroso1 = new ServerLayerInfo();
						eroso1.setEncoding(8);
						eroso1.setSwitchingCap(150);
						ero.addEROSubobject(eroso1);
						lambda_chosen = (info_next.getnumLayer() - 1);
					}
					else if ((info_next.getnumLayer() == (NumLambdas+1))){
						if (flag == false){
							//camino con LigthPath --> Ver qué hacer
							// pasamos a la CAPA de LIGTH PATHS
							IPv4prefixEROSubobject eroso= new IPv4prefixEROSubobject();
							eroso.setIpv4address((Inet4Address)info_actual.getIPnode());
							eroso.setLoosehop(false);
							ero.addEROSubobject(eroso);	
						}else{
							flag = false;
							continue;
						}
					}
					else if (info_next.getnumLayer() == (NumLambdas+2)){
						Nodo_aux_actual = node_aux_target;
						continue;
					}
				}
				else if ((info_actual.getnumLayer()>=1) && (info_actual.getnumLayer()<=NumLambdas)){
					//NODO CAPA OPTICA
					
					if ((info_actual.getIPnode() != info_next.getIPnode()) && (info_actual.getnumLayer()==info_next.getnumLayer())){
						//SEGUIMOS CAPA OPTICA
						
						UnnumberIfIDEROSubobject eroso= new UnnumberIfIDEROSubobject();
						eroso.setRouterID((Inet4Address)info_actual.getIPnode());
						eroso.setInterfaceID(preComp.getOpticalgraph().getEdge(info_actual.getIPnode(), info_next.getIPnode()).getSrc_if_id());
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
					
					else if ((info_actual.getIPnode() != info_next.getIPnode()) && (info_actual.getnumLayer()!=info_next.getnumLayer())){
						//CAMBIAMOS a la CAPA IP/MPLS
						num_Layer_changes++;
						UnnumberIfIDEROSubobject eroso= new UnnumberIfIDEROSubobject();
						eroso.setRouterID((Inet4Address)info_actual.getIPnode());
						eroso.setInterfaceID(0);
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
						
						ServerLayerInfo eroso1 = new ServerLayerInfo();
						eroso1.setEncoding(8);
						eroso1.setSwitchingCap(150);
						ero.addEROSubobject(eroso1);
					}
					
					if (j==(edge_list.size() - 1)){
						IPv4prefixEROSubobject eroso= new IPv4prefixEROSubobject();
						eroso.setIpv4address((Inet4Address)info_next.getIPnode());
						eroso.setPrefix(32);
						ero.addEROSubobject(eroso);
					}
				}
				
				else if (info_actual.getnumLayer() == (NumLambdas+1)){
					if (info_next.getnumLayer() == (NumLambdas+1)){
						//ESTAMOS EN LA CAPA DE LIGTH PATHS						
						continue;
					}
					
					else if (info_next.getnumLayer() == (NumLambdas+2)){
						IPv4prefixEROSubobject eroso= new IPv4prefixEROSubobject();
						eroso.setIpv4address((Inet4Address)info_next.getIPnode());
						eroso.setPrefix(32);
						ero.addEROSubobject(eroso);
						flag = true;
					}
				}
			}
			
			path.seteRO(ero);
			PCEPUtils.completeMetric(path, req, edge_list);
			response.addPath(path);
						
			OP_Counter.setTotal_layer_changes(num_Layer_changes);
			
			if (num_Layer_changes==0){
				//operacion 1 o 2
				if (edge_list.size()>3){
					OP_Counter.setNum_op2();
					OP_Counter.setTraffic_hops((ero.getEROSubobjectList().size()) - 1);
				}else{
					OP_Counter.setNum_op1();
					OP_Counter.setTraffic_hops(1);
				}
			}else if (num_Layer_changes == 2){
				//Operacion 3
				OP_Counter.setNum_op3();
				OP_Counter.setTraffic_hops(1);
			}else if(num_Layer_changes>2){
				//Operacion 4
				OP_Counter.setNum_op4();
				OP_Counter.setTraffic_hops(7.3);
			}
			
			OP_Counter.setMean_layer_changes();
			
			log_OP.info("NumOP_1: "+OP_Counter.getNum_op1());
			log_OP.info("NumOP_2: "+OP_Counter.getNum_op2());
			log_OP.info("NumOP_3: "+OP_Counter.getNum_op3());
			log_OP.info("NumOP_4: "+OP_Counter.getNum_op4());
			log_OP.info("MeanLayerChanges: "+OP_Counter.getMean_layer_changes()+"\n\n");
			log_OP.info("Total Traffic Hops: "+OP_Counter.getTraffic_hops());
			
								//  DE MOMENTO SIN PRE-RESERVA  //
			
			/*//FIXME: RESERVATION NEEDS TO BE IMPROVED!!!
			LinkedList<Object> sourceVertexList=new LinkedList<Object>();
			LinkedList<Object> targetVertexList=new LinkedList<Object>();
			for (i=0;i<edge_list.size();i++){
				sourceVertexList.add(edge_list.get(i).getSource());
				targetVertexList.add(edge_list.get(i).getTarget());
			}*/
		
			/*if (req.getReservation()!=null){
				
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
			}*/

		}
		return m_resp;
	}
	
	public void setPreComp(Multilayer_Algorithm_auxGraphPreComputation preComp) {
		this.preComp = preComp;
	}

	public AlgorithmReservation getReserv() {
		return reserv;
	}
}