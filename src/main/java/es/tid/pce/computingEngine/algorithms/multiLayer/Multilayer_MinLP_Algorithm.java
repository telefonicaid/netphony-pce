package es.tid.pce.computingEngine.algorithms.multiLayer;

import java.net.Inet4Address;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.jgrapht.GraphPath;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;

import es.tid.pce.computingEngine.ComputingRequest;
import es.tid.pce.computingEngine.ComputingResponse;
import es.tid.pce.computingEngine.algorithms.AlgorithmReservation;
import es.tid.pce.computingEngine.algorithms.ComputingAlgorithm;
import es.tid.pce.computingEngine.algorithms.PCEPUtils;
import es.tid.pce.computingEngine.algorithms.wson.GenericLambdaReservation;
import es.tid.pce.pcep.constructs.EndPoint;
import es.tid.pce.pcep.constructs.EndPointAndRestrictions;
import es.tid.pce.pcep.constructs.IPv4AddressEndPoint;
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
import es.tid.pce.pcep.objects.P2MPGeneralizedEndPoints;
import es.tid.pce.pcep.objects.P2PGeneralizedEndPoints;
import es.tid.pce.pcep.objects.RequestParameters;
import es.tid.pce.pcep.objects.tlvs.NoPathTLV;
import es.tid.pce.server.wson.ReservationManager;
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

public class Multilayer_MinLP_Algorithm implements ComputingAlgorithm {
	
	/**
	 * The Logger.
	 */
	private Logger log=LoggerFactory.getLogger("PCEServer");
	private Logger log_OP=LoggerFactory.getLogger("OpMultiLayer");
	
	/**
	 * The Path Computing Request to calculate.
	 */
	private ComputingRequest pathReq;

	/**
	 * Access to the Pre computation part of the algorithm.
	 */
	private Multilayer_MinLP_AlgorithmPreComputation preComp;
	
	static Operacion3 op3;
	static BFS_from_src Op4a;
	static BFS_from_dst Op4b;	
	
	/*private int num_op1;
	private int num_op2;
	private int num_op3;
	private int num_op4;*/
	//
	//Access to the Reservation Manager to make reservations of Wavelengths/labels.
	private ReservationManager reservationManager;
	
	//	/********************************
	//	 * Number of wavelenghts (labels).
	//	 ********************************/

	//	private int num_lambdas;
	
	/**
	 * The traffic engineering database
	 */
	private DomainTEDB ted;
	
	private GenericLambdaReservation  reserv;
	
	private OperationsCounter OP_Counter;
	
	private Lock graphLock;
	
	private ArrayList<SimpleDirectedWeightedGraph<Object, IntraDomainEdge>> networkGraphs_precomp;
	
	public Multilayer_MinLP_Algorithm(ComputingRequest pathReq,TEDB ted, ReservationManager reservationManager, OperationsCounter OPcounter){
		//this.num_lambdas=((DomainTEDB)ted).getNumLambdas();
		this.pathReq=pathReq;
		this.reservationManager=reservationManager;
		this.ted=(DomainTEDB)ted;
		this.OP_Counter=OPcounter;
	}

	public ComputingResponse call(){ 
		//Time stamp of the start of the algorithm;
		log.debug("Starting Multilayer Min Ligth Paths Algorithm");
		graphLock = preComp.getGraphLock();
		
		if (pathReq.getSvec()!=null){
			log.info("Numero de requests -- > "+pathReq.getSvec().getSvec().getRequestIDlist().size());
		}
										///////////////////////
										// PETICION CON SVEC //
										///////////////////////
		int u=0;
		if (pathReq.getSvec()!=null){
			ComputingResponse m_resp = new ComputingResponse();
			m_resp.setEncodingType(pathReq.getEcodingType());
			
			networkGraphs_precomp= new ArrayList <SimpleDirectedWeightedGraph <Object,IntraDomainEdge>>(preComp.getWSONInfo().getNumLambdas());
			int t;
			for (t=0;t<=preComp.getWSONInfo().getNumLambdas()-1;t++){
                networkGraphs_precomp.add(t,((MultiLayerTEDB)ted).getDuplicatedLowerLayerkGraph());
			}
			
			
			while (u<(pathReq.getSvec().getSvec().getRequestIDlist().size()) ){
		
				long tiempoini =System.nanoTime();
			
				//Create the response message
				//It will contain either the path or noPath
				ComputingResponse m_resp_individual = new ComputingResponse();
				//The request that needs to be solved
				Request req=pathReq.getRequestList().get(u);
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
				
				BandwidthRequested bandwidth = new BandwidthRequested();
				bandwidth.setBw(((BandwidthRequested)req.getBandwidth()).getBw());
				response.setBandwidth(bandwidth);
				m_resp_individual.addResponse(response);
				
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
						P2PGeneralizedEndPoints p2pep= (P2PGeneralizedEndPoints)gep;		
						source_router_id_addr = ((IPv4AddressEndPoint)p2pep.getSourceEndpoint().getEndPoint()).getEndPointIPv4().getIPv4address();
						dest_router_id_addr = ((IPv4AddressEndPoint)p2pep.getDestinationEndpoint().getEndPoint()).getEndPointIPv4().getIPv4address();
					}
					if(gep.getGeneralizedEndPointsType()==ObjectParameters.PCEP_GENERALIZED_END_POINTS_TYPE_P2MP_NEW_LEAVES){
						P2MPGeneralizedEndPoints p2mpep= (P2MPGeneralizedEndPoints)gep;
						EndPointAndRestrictions epandrest=p2mpep.getEndpointAndRestrictions();
						EndPoint sourceep=epandrest.getEndPoint();
						source_router_id_addr=((IPv4AddressEndPoint)sourceep).getEndPointIPv4().IPv4address;
						int cont=0;
						while (cont<=p2mpep.getEndpointAndRestrictionsList().size()){ //esto est� mal
							epandrest=p2mpep.getEndpointAndRestrictionsList().get(cont);
							EndPoint destep=epandrest.getEndPoint();
							dest_router_id_addr=((IPv4AddressEndPoint)destep).getEndPointIPv4().IPv4address;

						}
					}
				}
				//aqu� acaba lo que he a�adido
		
				//Now, check if the source and destination are in the TED.
				log.error("Source: "+source_router_id_addr+"; Destination:"+dest_router_id_addr);
				if (!((((ted).containsVertex(source_router_id_addr))&&((ted).containsVertex(dest_router_id_addr))))){
					log.error("Source or destination are NOT in the TED");	
					NoPath noPath= new NoPath();
					noPath.setNatureOfIssue(ObjectParameters.NOPATH_NOPATH_SAT_CONSTRAINTS);
					NoPathTLV noPathTLV=new NoPathTLV();
					if (!((ted.containsVertex(source_router_id_addr)))){
						log.error("Unknown source");	
						noPathTLV.setUnknownSource(true);	
					}
					if (!((ted.containsVertex(dest_router_id_addr)))){
						log.error("Unknown destination");
						noPathTLV.setUnknownDestination(true);	
					}
		
					noPath.setNoPathTLV(noPathTLV);				
					response.setNoPath(noPath);
					return m_resp;
				}
						
				//grafo con los ligth paths
				SimpleDirectedWeightedGraph<Object, IntraDomainEdge> GraphIP = preComp.getnetworkGraphIP();
				
				//grafo con las conexiones interlayer
				SimpleDirectedWeightedGraph<Object, IntraDomainEdge> InterlayerGraph =preComp.getInterLayerGraph();
				
				//Grago Óptico
				ArrayList<SimpleDirectedWeightedGraph<Object, IntraDomainEdge>> networkGraphs = preComp.getNetworkGraphs();
				
				int grooming = 0;
				
				boolean nopath=true;//Initially, we still have no path
				
				//SimpleDirectedWeightedGraph<Object,IntraDomainEdge> OpticalGraph = preComp.getOpticalnetworkGraph();
				GraphPath<Object,IntraDomainEdge> gp_chosen=null;
				
				GraphPath<Object,IntraDomainEdge> gp_chosen_a=null;
				GraphPath<Object,IntraDomainEdge> gp_chosen_b=null;
				
				GraphPath<Object,IntraDomainEdge> gp_chosen_a2=null;
				GraphPath<Object,IntraDomainEdge> gp_chosen_b2=null;
				
				int lambda_chosen = 0;
				boolean EROMultilayer_op3 = false;
				boolean EROMultilayer_op4 = false;
				boolean terminado = false;
				int numLambdas = ted.getWSONinfo().getNumLambdas();
				int numberHops = 0;
				boolean NoLambda = false;
				boolean prueba_demo = false;
				
				boolean op4a_flag = false;
				boolean op4b_flag = false;
						
				float bwt_req = ((BandwidthRequested)req.getBandwidth()).getBw(); // ancho de banda de la request
		
				/*System.out.println("ESTAMOS DENTRO DEL ALGORITMO VAMOS A CALCULAR LA RUTA -->" + "numLambdas:"+numLambdas);
				
				System.out.println("VAMOS A PINTAR LA TOPOLOGA VIRTUAL A VER COMO ESTA LA COSA\n");
				System.out.println("PINTAME ESTOOO!!");
				Set<IntraDomainEdge> fiberEdges= GraphIP.edgeSet();
				Iterator<IntraDomainEdge> iterFiberLink;
				
				iterFiberLink = fiberEdges.iterator();
				while (iterFiberLink.hasNext()){
					IntraDomainEdge fiberEdge =iterFiberLink.next();
					System.out.println(fiberEdge.toString()+"  BW -->"+fiberEdge.getTE_info().getUnreservedBandwidth().getUnreservedBandwidth()[0]);
					
				}
				System.out.println("\nTERMINAMOS DE PINTAR LA TOPOLOG VIRTUAL\n");*/
				GraphPath<Object,IntraDomainEdge> gp = Operacion1.get_op1(GraphIP, source_router_id_addr, dest_router_id_addr
						, graphLock, bwt_req);
					
				if (gp != null)
				{
					if (gp.getEdgeList().size() == 1)   // Path correctly found at operation 1 --> 1 hop
					{
						nopath = false;
						gp_chosen = gp;
						terminado = true;
						NoLambda = true;
					}
					else
						log.info("Hay PATH pero > 1 salto at Operation 1");
				}
							
				else if (gp==null){
					log.info("No ligthpath available: Continue to Operation 3");
						// go to OPERATION 3
				}
				
				
				// TENGO QUE DEVOLVER ERO MULTILAYER
				if (terminado == false){
					
					// OPERATION 2
					GraphPath<Object,IntraDomainEdge> gp2 = Operacion2.get_op2(GraphIP, 
							source_router_id_addr, dest_router_id_addr, bwt_req, graphLock);
								
					if (gp2 != null){
						log.warn("Multiple LigthPath found at Operation 2");	
						nopath = false;
						gp_chosen = gp2;
						numberHops = gp2.getEdgeList().size();
						NoLambda = true;
						terminado=true;
					}
										
				}
				if (terminado == false){
										
					op3 = new Operacion3(networkGraphs_precomp);
					GraphPath<Object,IntraDomainEdge> gp3 = op3.get_op3(source_router_id_addr,
							dest_router_id_addr, InterlayerGraph, numLambdas);
									
					if (gp3 != null){  // PATH FOUND
						nopath = false;
						gp_chosen = gp3;
						EROMultilayer_op3 = true;
						
						lambda_chosen = op3.getLambda_chosen();	
					}						
					else {
						log.info("No hay camino Óptico disponible: NO PATH");
					}
				}
					
					
				// SHORTEST PATH BETWEEN OPERAION 2  AND OPERATION 4
				// OPERATION 2
			
						
					// En caso de tener camino con la operación 2 tenemos que ver si se encuentra otro
					// camino más corto combinando capas --> BFS (Operación 4)
					// Si no hay camino en la Op2 simplemente buscamos camino combinando capas
						
					// BFS desde el origen
					/*Op4a = new BFS_from_src();
						
					GraphPath<Object,IntraDomainEdge> gp4a = Op4a.getBFS(GraphIP, InterlayerGraph,
							networkGraphs, source_router_id_addr, numberHops, dest_router_id_addr,
							numLambdas);
						
					Object nodo_auxiliar = null;
						
					if (gp4a != null){  // tenemos un camino más corto
							
						NoLambda = false;
						numberHops = (Op4a.getNumberHops() + 1);
						nodo_auxiliar = gp4a.getStartVertex();
						op4a_flag = true;
						nopath = false;
						EROMultilayer_op4 = true;
						gp_chosen_a = gp4a;
							
						GraphPath<Object,IntraDomainEdge> gp4a2 = Operacion2.get_op2(GraphIP,
								source_router_id_addr, nodo_auxiliar, bwt_req);
							
						if (gp4a2 == null){
							System.out.println("Si entramos aquí es que pinta chunga la cosa");
						}
							 
						gp_chosen_a2 = gp4a2;
					}
						
					Op4b = new BFS_from_dst();
						
					GraphPath<Object,IntraDomainEdge> gp4b = Op4a.getBFS(GraphIP, InterlayerGraph,
							networkGraphs, source_router_id_addr, numberHops, dest_router_id_addr,
							numLambdas);
						
					if (gp4b != null){  // tenemos un camino más corto que en el caso a
						
						NoLambda = false;
						op4b_flag = true;
						nopath = false;
						EROMultilayer_op4 = true;
						op4a_flag = false;
						gp_chosen_b = gp4b;
						nodo_auxiliar = gp4b.getEndVertex();
							
						GraphPath<Object,IntraDomainEdge> gp4b2 = Operacion2.get_op2(GraphIP,
								nodo_auxiliar, dest_router_id_addr, bwt_req);
							
						if (gp4b2 == null){
							System.out.println("Si entramos aquí es que pinta chunga la cosa");
						}
						gp_chosen_b2 = gp4b2;
					}*/		
				if (nopath==true){
					log.info("No path found"); // NO PATH FOUND
					NoPath noPath= new NoPath();
					noPath.setNatureOfIssue(ObjectParameters.NOPATH_NOPATH_SAT_CONSTRAINTS);
					NoPathTLV noPathTLV=new NoPathTLV();
					noPath.setNoPathTLV(noPathTLV);				
					response.setNoPath(noPath);
					return m_resp;
				}
				
				else if (nopath==false){  // PATH FOUND
					Path path=new Path();
					ExplicitRouteObject ero= new ExplicitRouteObject();
					List<IntraDomainEdge> edge_list_pre = null;
					if (EROMultilayer_op4 == false){
						edge_list_pre = gp_chosen.getEdgeList();
					}
					LinkedList<IntraDomainEdge> edge_list = new LinkedList<IntraDomainEdge>();
								
					if (EROMultilayer_op3 == true){  // Operation 3 --> ERO MULTILAYER
						edge_list.add(op3.getEdge_ini());
						int m;
						for (m=0;m<edge_list_pre.size();m++){
							edge_list.add(edge_list_pre.get(m));
						}
						edge_list.add(op3.getEdge_end());
					}
					else if (EROMultilayer_op3 == false && EROMultilayer_op4 == false){
						int m;
						for (m=0;m<edge_list_pre.size();m++){
							edge_list.add(edge_list_pre.get(m));
						}
					}
					
					else if (EROMultilayer_op3 == false && EROMultilayer_op4 == true){
						if (op4a_flag == true){
							int m;
							for (m=0;m<gp_chosen_a2.getEdgeList().size(); m++){
								edge_list.add(gp_chosen_a2.getEdgeList().get(m));
							}
							int i;
							for (i=0;i < gp_chosen_a.getEdgeList().size(); i++, m++){
								edge_list.add(gp_chosen_a.getEdgeList().get(i));
							}
						}
						else if (op4b_flag == true){
							int m;
							for (m=0;m<gp_chosen_b2.getEdgeList().size(); m++){
								edge_list.add(gp_chosen_b2.getEdgeList().get(m));
							}
							int n;
							for (n=0;n < gp_chosen_b.getEdgeList().size(); n++, m++){
								edge_list.add(gp_chosen_b.getEdgeList().get(n));
							}
						}
					}
					
					//tengo un listado con todos los edges del camino a lo largo de todas las capas
					int i;
					for (i=0;i<edge_list.size();i++){
						if (NoLambda == true){
							IPv4prefixEROSubobject eroso= new IPv4prefixEROSubobject();
							eroso.setIpv4address((Inet4Address)edge_list.get(i).getSource());
							eroso.setLoosehop(false);
							ero.addEROSubobject(eroso);
						}
										
						else if (NoLambda == false){
							if (op4a_flag == true){
							}
							
							if (op4b_flag == true){
							}
							
							if (EROMultilayer_op3 == true && (i == 0)){
								IPv4prefixEROSubobject eroso= new IPv4prefixEROSubobject();
								eroso.setIpv4address((Inet4Address)edge_list.get(i).getSource());
								eroso.setLoosehop(false);
								ero.addEROSubobject(eroso);
								ServerLayerInfo eroso1 = new ServerLayerInfo();
								eroso1.setEncoding(8);
								eroso1.setSwitchingCap(150);
								ero.addEROSubobject(eroso1);
							}
							
							else if(EROMultilayer_op3 == true && i!=0){
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
							if (EROMultilayer_op3 == true && (i==(edge_list.size() - 1))){
								ServerLayerInfo eroso1 = new ServerLayerInfo();
								eroso1.setEncoding(8);
								eroso1.setSwitchingCap(150);
								ero.addEROSubobject(eroso1);
							}
						}
					}
					
					IPv4prefixEROSubobject eroso= new IPv4prefixEROSubobject();
					eroso.setIpv4address((Inet4Address)edge_list.get(edge_list.size()-1).getTarget());
					eroso.setPrefix(32);
					ero.addEROSubobject(eroso);
					path.setEro(ero);
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
						reserv.setResp(m_resp_individual);
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
					else
						log.info("Reservation is FALSE");
					
					notifyWavelengthReservation(sourceVertexList,targetVertexList, lambda_chosen);
				}
				long tiempofin =System.nanoTime();
				long tiempotot=tiempofin-tiempoini;
				log.info("Ha tardado "+tiempotot+" nanosegundos");
				u++;
			}
			return m_resp;
		}
		else {
									/////////////////////////
									// PETICION INDIVIDUAL //
									/////////////////////////

			ComputingResponse m_resp = new ComputingResponse();
			
			long tiempoini =System.nanoTime();
			
			//Create the response message
			//It will contain either the path or noPath
			ComputingResponse m_resp_individual = new ComputingResponse();
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
			
			BandwidthRequested bandwidth = new BandwidthRequested();
			bandwidth.setBw(((BandwidthRequested)req.getBandwidth()).getBw());
			response.setBandwidth(bandwidth);
						
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
					P2PGeneralizedEndPoints p2pep= (P2PGeneralizedEndPoints)gep;		
					source_router_id_addr = ((IPv4AddressEndPoint)p2pep.getSourceEndpoint().getEndPoint()).getEndPointIPv4().getIPv4address();
					dest_router_id_addr = ((IPv4AddressEndPoint)p2pep.getDestinationEndpoint().getEndPoint()).getEndPointIPv4().getIPv4address();
				}
				if(gep.getGeneralizedEndPointsType()==ObjectParameters.PCEP_GENERALIZED_END_POINTS_TYPE_P2MP_NEW_LEAVES){
					P2MPGeneralizedEndPoints p2mpep= (P2MPGeneralizedEndPoints)gep;
					EndPointAndRestrictions epandrest=p2mpep.getEndpointAndRestrictions();
					EndPoint sourceep=epandrest.getEndPoint();
					source_router_id_addr=((IPv4AddressEndPoint)sourceep).getEndPointIPv4().IPv4address;
					int cont=0;
					while (cont<=p2mpep.getEndpointAndRestrictionsList().size()){ //esto est� mal
						epandrest=p2mpep.getEndpointAndRestrictionsList().get(cont);
						EndPoint destep=epandrest.getEndPoint();
						dest_router_id_addr=((IPv4AddressEndPoint)destep).getEndPointIPv4().IPv4address;

					}
				}
			}
			//aqu� acaba lo que he a�adido
	
			//Now, check if the source and destination are in the TED.
			log.error("Source: "+source_router_id_addr+"; Destination:"+dest_router_id_addr);
			if (!((((ted).containsVertex(source_router_id_addr))&&((ted).containsVertex(dest_router_id_addr))))){
				log.error("Source or destination are NOT in the TED");	
				NoPath noPath= new NoPath();
				noPath.setNatureOfIssue(ObjectParameters.NOPATH_NOPATH_SAT_CONSTRAINTS);
				NoPathTLV noPathTLV=new NoPathTLV();
				if (!((ted.containsVertex(source_router_id_addr)))){
					log.error("Unknown source");	
					noPathTLV.setUnknownSource(true);	
				}
				if (!((ted.containsVertex(dest_router_id_addr)))){
					log.error("Unknown destination");
					noPathTLV.setUnknownDestination(true);	
				}
	
				noPath.setNoPathTLV(noPathTLV);				
				response.setNoPath(noPath);
				return m_resp;
			}
					
			//grafo con los ligth paths
			SimpleDirectedWeightedGraph<Object, IntraDomainEdge> GraphIP = preComp.getnetworkGraphIP();
			
			//grafo con las conexiones interlayer
			SimpleDirectedWeightedGraph<Object, IntraDomainEdge> InterlayerGraph =preComp.getInterLayerGraph();
			
			//Grago Óptico
			ArrayList<SimpleDirectedWeightedGraph<Object, IntraDomainEdge>> networkGraphs = preComp.getNetworkGraphs();
			
			int grooming = 0;
			
			boolean nopath=true;//Initially, we still have no path
			
			//SimpleDirectedWeightedGraph<Object,IntraDomainEdge> OpticalGraph = preComp.getOpticalnetworkGraph();
			GraphPath<Object,IntraDomainEdge> gp_chosen=null;
			
			GraphPath<Object,IntraDomainEdge> gp_chosen_a=null;
			GraphPath<Object,IntraDomainEdge> gp_chosen_b=null;
			
			GraphPath<Object,IntraDomainEdge> gp_chosen_a2=null;
			GraphPath<Object,IntraDomainEdge> gp_chosen_b2=null;
			
			int lambda_chosen = 0;
			boolean EROMultilayer_op3 = false;
			boolean EROMultilayer_op4 = false;
			boolean terminado = false;
			int numLambdas = ted.getWSONinfo().getNumLambdas();
			int numberHops = 0;
			boolean NoLambda = false;
			boolean prueba_demo = false;
			
			boolean op4a_flag = false;
			boolean op4b_flag = false;
					
			float bwt_req = ((BandwidthRequested)req.getBandwidth()).getBw(); // ancho de banda de la request
	
			//System.out.println("ESTAMOS DENTRO DEL ALGORITMO VAMOS A CALCULAR LA RUTA -->" + "numLambdas:"+numLambdas);
			
			/*System.out.println("VAMOS A PINTAR LA TOPOLOA VIRTUAL A VER COMO ESTA LA COSA\n");
			System.out.println("PINTAME ESTOOO!!");
			Set<IntraDomainEdge> fiberEdges= GraphIP.edgeSet();
			Iterator<IntraDomainEdge> iterFiberLink;
			
			iterFiberLink = fiberEdges.iterator();
			while (iterFiberLink.hasNext()){
				IntraDomainEdge fiberEdge =iterFiberLink.next();
				System.out.println(fiberEdge.toString()+"  BW -->"+fiberEdge.getTE_info().getUnreservedBandwidth().getUnreservedBandwidth()[0]);
				
			}
			System.out.println("\nTERMINAMOS DE PINTAR LA TOPOLOA VIRTUAL\n");*/
			
			GraphPath<Object,IntraDomainEdge> gp = Operacion1.get_op1(GraphIP, source_router_id_addr, dest_router_id_addr
					, graphLock, bwt_req);
				
			if (gp != null)
			{
				if (gp.getEdgeList().size() == 1)   // Path correctly found at operation 1 --> 1 hop
				{
					nopath = false;
					gp_chosen = gp;
					terminado = true;
					NoLambda = true;
					OP_Counter.setNum_op1();
				}
				else
					log.info("Hay PATH pero > 1 salto at Operation 1");
			}
						
			else if (gp==null){
				//System.out.println("No ligthpath available: Continue to Operation 2");
					// go to OPERATION 2
			}								
			// OPERATION2
			// TENGO QUE DEVOLVER ERO MULTILAYER
			if (terminado == false){
				
				// OPERATION 2
				GraphPath<Object,IntraDomainEdge> gp2 = Operacion2.get_op2(GraphIP, 
						source_router_id_addr, dest_router_id_addr, bwt_req, graphLock);
							
				if (gp2 != null){
					log.warn("Multiple LigthPath found at Operation 2");	
					nopath = false;
					gp_chosen = gp2;
					numberHops = gp2.getEdgeList().size();
					NoLambda = true;
					terminado=true;
					OP_Counter.setNum_op2();
				}
									
			}
			if (terminado == false){
									
				op3 = new Operacion3(networkGraphs);
				GraphPath<Object,IntraDomainEdge> gp3 = op3.get_op3(source_router_id_addr,
						dest_router_id_addr, InterlayerGraph, numLambdas);
								
				if (gp3 != null){  // PATH FOUND
					nopath = false;
					gp_chosen = gp3;
					EROMultilayer_op3 = true;
					terminado = true;
					lambda_chosen = op3.getLambda_chosen();	
					OP_Counter.setNum_op3();
				}						
				
			}
			/*if (terminado == false){
				
				
				//	OPERATION 4
		
				// BFS desde el origen
				Op4a = new BFS_from_src();
				GraphPath<Object,IntraDomainEdge> gp4a = Op4a.getBFS(GraphIP, InterlayerGraph,
						networkGraphs, source_router_id_addr, 20, dest_router_id_addr,
						numLambdas);
					
				Object nodo_auxiliar = null;
					
				if (gp4a != null){  // tenemos un camino más corto
						
					NoLambda = false;
					numberHops = (Op4a.getNumberHops() + 1);
					nodo_auxiliar = gp4a.getStartVertex();
					op4a_flag = true;
					nopath = false;
					EROMultilayer_op4 = true;
					gp_chosen_a = gp4a;
						
					GraphPath<Object,IntraDomainEdge> gp4a2 = Operacion2.get_op2(GraphIP, 
							source_router_id_addr, nodo_auxiliar, bwt_req, graphLock);
						
					if (gp4a2 == null){
						System.out.println("Si entramos aquí es que pinta chunga la cosa");
						System.exit(-1);
					}
						 
					gp_chosen_a2 = gp4a2;
				}
				
				Op4b =	new BFS_from_dst();
				GraphPath<Object,IntraDomainEdge> gp4b = Op4b.getBFS(GraphIP, InterlayerGraph,
						networkGraphs, source_router_id_addr, numberHops, dest_router_id_addr,
						numLambdas);
				
								
				if (gp4b != null){  // tenemos un camino más corto que en el caso a
					
					NoLambda = false;
					op4b_flag = true;
					nopath = false;
					EROMultilayer_op4 = true;
					op4a_flag = false;
					gp_chosen_b = gp4b;
					nodo_auxiliar = gp4b.getEndVertex();
						
					GraphPath<Object,IntraDomainEdge> gp4b2 = Operacion2.get_op2(GraphIP,
							nodo_auxiliar, dest_router_id_addr, bwt_req, graphLock);
						
					if (gp4b2 == null){
						System.out.println("Si entramos aquí es que pinta chunga la cosa");
						System.exit(-1);
					}
					gp_chosen_b2 = gp4b2;
					OP_Counter.setNum_op4();
					
				}else{
					if (gp_chosen_a2 != null){
						OP_Counter.setNum_op4();
					}
				}
			}*/
			/*if (op4b_flag==true || op4a_flag==true){
				LinkedList<IntraDomainEdge> edge_list_prueba = new LinkedList<IntraDomainEdge>();
				if (op4a_flag == true){
					int m;
					for (m=0;m<gp_chosen_a2.getEdgeList().size(); m++){
						//edge_list_prueba.add(gp_chosen_a2.getEdgeList().get(m));
						//reservar camino IP/MPLS
						TE_Information informationTEDB=gp_chosen_a2.getEdgeList().get(m).getTE_info();
						if (informationTEDB == null){
							informationTEDB = new TE_Information();
						}
						
						float bw = gp_chosen_a2.getEdgeList().get(m).getTE_info().getMaximumReservableBandwidth().getMaximumReservableBandwidth() - bwt_req;
						float[] bw_un = new float[8];
						bw_un[0] = bw;
						UnreservedBandwidth unreservedBandwidth = new UnreservedBandwidth();
						unreservedBandwidth.setUnreservedBandwidth(bw_un);
						informationTEDB.setUnreservedBandwidth(unreservedBandwidth);
						
						MaximumReservableBandwidth maximumReservableBandwidth = new MaximumReservableBandwidth();
						maximumReservableBandwidth.setMaximumReservableBandwidth(bw);
						informationTEDB.setMaximumReservableBandwidth(maximumReservableBandwidth);
						
						gp_chosen_a2.getEdgeList().get(m).setTE_info(informationTEDB);
					}
					int i;
					for (i=0;i < gp_chosen_a.getEdgeList().size(); i++, m++){
						//edge_list_prueba.add(gp_chosen_a.getEdgeList().get(i));
						SimpleDirectedWeightedGraph<Object, IntraDomainEdge> networkGraph=networkGraphs.get(Op4a.getLambda_chosen());
						networkGraph.removeEdge(gp_chosen_a.getEdgeList().get(i).getSource(), gp_chosen_a.getEdgeList().get(i).getTarget());
					}
				}
				else if (op4b_flag == true){
					int m;
					for (m=0;m<gp_chosen_b.getEdgeList().size(); m++){
						//edge_list_prueba.add(gp_chosen_b.getEdgeList().get(m));
						
						SimpleDirectedWeightedGraph<Object, IntraDomainEdge> networkGraph=networkGraphs.get(Op4b.getLambda_chosen());
						networkGraph.removeEdge(gp_chosen_b.getEdgeList().get(m).getSource(), gp_chosen_b.getEdgeList().get(m).getTarget());
					}
					int n;
					for (n=0;n < gp_chosen_b2.getEdgeList().size(); n++, m++){
						//edge_list_prueba.add(gp_chosen_b2.getEdgeList().get(n));
						
						TE_Information informationTEDB=gp_chosen_b2.getEdgeList().get(n).getTE_info();
						if (informationTEDB == null){
							informationTEDB = new TE_Information();
						}
						
						float bw = gp_chosen_b2.getEdgeList().get(n).getTE_info().getMaximumReservableBandwidth().getMaximumReservableBandwidth() - bwt_req;
						float[] bw_un = new float[8];
						bw_un[0] = bw;
						UnreservedBandwidth unreservedBandwidth = new UnreservedBandwidth();
						unreservedBandwidth.setUnreservedBandwidth(bw_un);
						informationTEDB.setUnreservedBandwidth(unreservedBandwidth);
						
						MaximumReservableBandwidth maximumReservableBandwidth = new MaximumReservableBandwidth();
						maximumReservableBandwidth.setMaximumReservableBandwidth(bw);
						informationTEDB.setMaximumReservableBandwidth(maximumReservableBandwidth);
						
						gp_chosen_b2.getEdgeList().get(n).setTE_info(informationTEDB);
					}
				}
				
				nopath=true;
			}*/
			if (nopath==true){
				log.debug("No path found"); // NO PATH FOUND
				NoPath noPath= new NoPath();
				noPath.setNatureOfIssue(ObjectParameters.NOPATH_NOPATH_SAT_CONSTRAINTS);
				NoPathTLV noPathTLV=new NoPathTLV();
				noPath.setNoPathTLV(noPathTLV);				
				response.setNoPath(noPath);
				
				log_OP.info("NumOP_1: "+OP_Counter.getNum_op1());
				log_OP.info("NumOP_2: "+OP_Counter.getNum_op2());
				log_OP.info("NumOP_3: "+OP_Counter.getNum_op3());
				log_OP.info("NumOP_4: "+OP_Counter.getNum_op4());
				
				return m_resp;
			}
			
			else if (nopath==false){  // PATH FOUND
				
				
				log_OP.info("NumOP_1: "+OP_Counter.getNum_op1());
				log_OP.info("NumOP_2: "+OP_Counter.getNum_op2());
				log_OP.info("NumOP_3: "+OP_Counter.getNum_op3());
				log_OP.info("NumOP_4: "+OP_Counter.getNum_op4());
				
				Path path=new Path();
				ExplicitRouteObject ero= new ExplicitRouteObject();
				List<IntraDomainEdge> edge_list_pre = null;
				if (EROMultilayer_op4 == false){
					edge_list_pre = gp_chosen.getEdgeList();
				}
				LinkedList<IntraDomainEdge> edge_list = new LinkedList<IntraDomainEdge>();
							
				if (EROMultilayer_op3 == true){  // Operation 3 --> ERO MULTILAYER
					//System.out.println("CAMINO CON ERO MULTILAYER");
					edge_list.add(op3.getEdge_ini());
					int m;
					for (m=0;m<edge_list_pre.size();m++){
						edge_list.add(edge_list_pre.get(m));
					}
					edge_list.add(op3.getEdge_end());
				}
				else if (EROMultilayer_op3 == false && EROMultilayer_op4 == false){
					int m;
					for (m=0;m<edge_list_pre.size();m++){
						edge_list.add(edge_list_pre.get(m));
					}
				}
				
				else if (EROMultilayer_op3 == false && EROMultilayer_op4 == true){
					if (op4a_flag == true){
						int m;
						for (m=0;m<gp_chosen_a2.getEdgeList().size(); m++){
							edge_list.add(gp_chosen_a2.getEdgeList().get(m));
						}
						int i;
						for (i=0;i < gp_chosen_a.getEdgeList().size(); i++, m++){
							edge_list.add(gp_chosen_a.getEdgeList().get(i));
						}
					}
					else if (op4b_flag == true){
						int m;
						for (m=0;m<gp_chosen_b2.getEdgeList().size(); m++){
							edge_list.add(gp_chosen_b2.getEdgeList().get(m));
						}
						int n;
						for (n=0;n < gp_chosen_b.getEdgeList().size(); n++, m++){
							edge_list.add(gp_chosen_b.getEdgeList().get(n));
						}
					}
				}
				
				//tengo un listado con todos los edges del camino a lo largo de todas las capas
				int i;
				for (i=0;i<edge_list.size();i++){
					if (NoLambda == true){
						IPv4prefixEROSubobject eroso= new IPv4prefixEROSubobject();
						eroso.setIpv4address((Inet4Address)edge_list.get(i).getSource());
						eroso.setLoosehop(false);
						ero.addEROSubobject(eroso);
					}
									
					else if (NoLambda == false){
						if (op4a_flag == true){
						}
						
						if (op4b_flag == true){
						}
						
						if (EROMultilayer_op3 == true && (i == 0)){
							IPv4prefixEROSubobject eroso= new IPv4prefixEROSubobject();
							eroso.setIpv4address((Inet4Address)edge_list.get(i).getSource());
							eroso.setLoosehop(false);
							ero.addEROSubobject(eroso);
							//System.out.println("SERVER LAYER INFO");
							ServerLayerInfo eroso1 = new ServerLayerInfo();
							eroso1.setEncoding(8);
							eroso1.setSwitchingCap(150);
							ero.addEROSubobject(eroso1);
						}
						
						else if(EROMultilayer_op3 == true && i!=0){
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
						if (EROMultilayer_op3 == true && (i==(edge_list.size() - 1))){
							//System.out.println("SERVER LAYER INFO");
							ServerLayerInfo eroso1 = new ServerLayerInfo();
							eroso1.setEncoding(8);
							eroso1.setSwitchingCap(150);
							ero.addEROSubobject(eroso1);
						}
					}
				}
				
				IPv4prefixEROSubobject eroso= new IPv4prefixEROSubobject();
				eroso.setIpv4address((Inet4Address)edge_list.get(edge_list.size()-1).getTarget());
				eroso.setPrefix(32);
				ero.addEROSubobject(eroso);
				path.setEro(ero);
				PCEPUtils.completeMetric(path, req, edge_list);
				response.addPath(path);
				
				//System.out.println("ERO QUE ENVIAMOS DESDE EL PCE  :"+ero);
	
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
				else
					log.info("Reservation is FALSE");
			}
			
			long tiempofin =System.nanoTime();
			long tiempotot=tiempofin-tiempoini;
			log.info("Ha tardado "+tiempotot+" nanosegundos");
					
			return m_resp;
		}
	}
	
	public void setPreComp(Multilayer_MinLP_AlgorithmPreComputation preComp) {
		this.preComp = preComp;
	}
	
	public AlgorithmReservation getReserv() {
		return reserv;
	}
	public void notifyWavelengthReservation(LinkedList<Object> sourceVertexList,
	        LinkedList<Object> targetVertexList, int wavelength) {
		//System.out.println("LLEGO AQUI"); 
		graphLock.lock();
		try{
			//System.out.println("DENTRO DE LA RESERVA DEL ALGORITMO CON LAMBDA --> "+wavelength);
		    SimpleDirectedWeightedGraph<Object, IntraDomainEdge> networkGraph=networkGraphs_precomp.get(wavelength);
		    for (int i=0;i<sourceVertexList.size();++i){                                
		    	networkGraph.removeEdge(sourceVertexList.get(i), targetVertexList.get(i));
		        //System.out.println("EDGE OUT");
		    }
		}finally{
		  	graphLock.unlock();        
		}
	}
}
