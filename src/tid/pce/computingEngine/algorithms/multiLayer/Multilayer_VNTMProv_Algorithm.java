package tid.pce.computingEngine.algorithms.multiLayer;

import java.net.Inet4Address;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.logging.Logger;

import org.jgrapht.GraphPath;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;

import tid.pce.computingEngine.ComputingRequest;
import tid.pce.computingEngine.ComputingResponse;
import tid.pce.computingEngine.algorithms.AlgorithmReservation;
import tid.pce.computingEngine.algorithms.ComputingAlgorithm;
import tid.pce.computingEngine.algorithms.PCEPUtils;
import tid.pce.computingEngine.algorithms.wson.GenericLambdaReservation;
import tid.pce.pcep.constructs.EndPoint;
import tid.pce.pcep.constructs.EndPointAndRestrictions;
import tid.pce.pcep.constructs.P2MPEndpoints;
import tid.pce.pcep.constructs.P2PEndpoints;
import tid.pce.pcep.constructs.Path;
import tid.pce.pcep.constructs.Request;
import tid.pce.pcep.constructs.Response;
import tid.pce.pcep.objects.Bandwidth;
import tid.pce.pcep.objects.EndPointDataPathID;
import tid.pce.pcep.objects.EndPoints;
import tid.pce.pcep.objects.EndPointsIPv4;
import tid.pce.pcep.objects.ExplicitRouteObject;
import tid.pce.pcep.objects.GeneralizedEndPoints;
import tid.pce.pcep.objects.NoPath;
import tid.pce.pcep.objects.ObjectParameters;
import tid.pce.pcep.objects.RequestParameters;
import tid.pce.pcep.objects.tlvs.NoPathTLV;
import tid.pce.server.wson.ReservationManager;
import tid.pce.tedb.DomainTEDB;
import tid.pce.tedb.IntraDomainEdge;
import tid.pce.tedb.TEDB;
import tid.rsvp.RSVPProtocolViolationException;
import tid.rsvp.constructs.gmpls.DWDMWavelengthLabel;
import tid.rsvp.objects.subobjects.GeneralizedLabelEROSubobject;
import tid.rsvp.objects.subobjects.IPv4prefixEROSubobject;
import tid.rsvp.objects.subobjects.OpenFlowUnnumberIfIDEROSubobject;
import tid.rsvp.objects.subobjects.ServerLayerInfo;
import tid.rsvp.objects.subobjects.UnnumberIfIDEROSubobject;

public class Multilayer_VNTMProv_Algorithm implements ComputingAlgorithm {
	
	/**
	 * The Logger.
	 */
	private Logger log=Logger.getLogger("PCEServer");
	
	/**
	 * The Path Computing Request to calculate.
	 */
	private ComputingRequest pathReq;
	
	private Lock graphLock;
	
	private ArrayList<SimpleDirectedWeightedGraph<Object, IntraDomainEdge>> networkGraphs_precomp;

	/**
	 * Access to the Pre computation part of the algorithm.
	 */
	private Multilayer_VNTMProv_AlgorithmPreComputation preComp;
	
	static Operacion3 op3;
	static BFS_from_src Op4a;
	static BFS_from_dst Op4b;
	
	/*
	 * Access to the Reservation Manager to make reservations of Wavelengths/labels.
	 */
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
	
	/**
	 * Constructor
	 * @param pathReq
	 * @param ted
	 * @param reservationManager
	 */
	public Multilayer_VNTMProv_Algorithm(ComputingRequest pathReq,TEDB ted, ReservationManager reservationManager, OperationsCounter OPcounter){
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
		
		log.info("Starting Multilayer Min Traffic Hops Algorithm, New Request!");
		//Create the response message
		//It will contain either the path or noPath
		
		graphLock = preComp.getGraphLock();
		
		int u=0;
		if (pathReq.getSvec()!=null){
			ComputingResponse m_resp = new ComputingResponse();
			m_resp.setEncodingType(pathReq.getEcodingType());
			log.info("Request with SVEC!");
			int t;

			
			
			while (u<(pathReq.getSvec().getSvec().getRequestIDlist().size()) ){
				
				//Time stamp of the start of the algorithm;
				long tiempoini =System.nanoTime();
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
				
				Bandwidth bandwidth = new Bandwidth();
				bandwidth.setBw(req.getBandwidth().getBw());
				response.setBandwidth(bandwidth);
				
				m_resp.addResponse(response);
				m_resp_individual.addResponse(response);
				
				//esto hay que cambiarlo para poder leer del GENERALIZED END POINTS
				//if (getObjectType(req.getEndPoints()))
				EndPoints  EP= req.getEndPoints();	
				Object source_router_id_addr = null;
				Object dest_router_id_addr = null;
				
				boolean isdatapathid=false;
				
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
				} else	if (EP.getOT()==ObjectParameters.PCEP_OBJECT_TYPE_ENDPOINTS_DATAPATH_ID){ //CASO QUE NOS INTERESA (
					EndPointDataPathID ep=(EndPointDataPathID) req.getEndPoints();
					source_router_id_addr=ep.getSourceSwitchID();
					dest_router_id_addr=ep.getDestSwitchID();
					isdatapathid=true;
				}
				//aqu� acaba lo que he a�adido
	
				//Now, check if the source and destination are in the TED.
				log.severe("Source: "+source_router_id_addr+"; Destination:"+dest_router_id_addr);
				if (!((((ted).containsVertex(source_router_id_addr))&&((ted).containsVertex(dest_router_id_addr))))){
					log.severe("Source or destination are NOT in the TED");	
					NoPath noPath= new NoPath();
					noPath.setNatureOfIssue(ObjectParameters.NOPATH_NOPATH_SAT_CONSTRAINTS);
					NoPathTLV noPathTLV=new NoPathTLV();
					if (!((ted.containsVertex(source_router_id_addr)))){
						log.severe("Unknown source");	
						noPathTLV.setUnknownSource(true);	
					}
					if (!((ted.containsVertex(dest_router_id_addr)))){
						log.severe("Unknown destination");
						noPathTLV.setUnknownDestination(true);	
					}
	
					noPath.setNoPathTLV(noPathTLV);				
					response.setNoPath(noPath);
					//return m_resp;
				}
						
				SimpleDirectedWeightedGraph<Object, IntraDomainEdge> graph = preComp.getbaseSimplegraph();
				
						
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
				boolean op4a_flag = false;
				boolean op4b_flag = false;
				boolean eroreturned = false;
				ExplicitRouteObject eroml=null;
				
						
				float bwt_req = req.getBandwidth().getBw(); // ancho de banda de la request
					
				GraphPath<Object,IntraDomainEdge> gp = Operacion12.get_op1(graph, source_router_id_addr, dest_router_id_addr
						, graphLock, bwt_req);
					
				if (gp != null)
				{
				//	if (gp.getEdgeList().size() == 1)   // Path correctly found at operation 1 --> 1 hop
				//	{
						nopath = false;
						gp_chosen = gp;
						terminado = true;
						NoLambda = true;
						OP_Counter.setNum_op1();
				//	}
				} else {
					eroml=Operacion34_Initiate.get_op43(graph, source_router_id_addr, dest_router_id_addr, 0);
					if (eroml==null){
						log.fine("No path found"); // NO PATH FOUND
						NoPath noPath= new NoPath();
						noPath.setNatureOfIssue(ObjectParameters.NOPATH_NOPATH_SAT_CONSTRAINTS);
						NoPathTLV noPathTLV=new NoPathTLV();
						noPath.setNoPathTLV(noPathTLV);				
						response.setNoPath(noPath);
						return m_resp;
					}
					eroreturned=true;
					nopath=false;
					
				}
							
				
											
				if (nopath==true){ //FIXME: Llamar al VNTM Aquí
					log.fine("No path found"); // NO PATH FOUND
					NoPath noPath= new NoPath();
					noPath.setNatureOfIssue(ObjectParameters.NOPATH_NOPATH_SAT_CONSTRAINTS);
					NoPathTLV noPathTLV=new NoPathTLV();
					noPath.setNoPathTLV(noPathTLV);				
					response.setNoPath(noPath);
					//return m_resp;
				}
				
				else if (nopath==false){  // PATH FOUND
					Path path=new Path();
					ExplicitRouteObject ero= new ExplicitRouteObject();
					if (!eroreturned){
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
									DWDMWavelengthLabel WDMlabel=new DWDMWavelengthLabel(); //FIXME: layer 2 ERO, no wdmlabel....
									//WDMlabel.setGrid(preComp.getWSONInfo().getGrid());
									//WDMlabel.setChannelSpacing(preComp.getWSONInfo().getCs());
									//WDMlabel.setN(lambda_chosen+preComp.getWSONInfo().getnMin());
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
								} if (isdatapathid){
									OpenFlowUnnumberIfIDEROSubobject eroso= new OpenFlowUnnumberIfIDEROSubobject();
									eroso.setSwitchID((String)edge_list.get(i).getSource()); 
									eroso.setInterfaceID(edge_list.get(i).getSrc_if_id());
									ero.addEROSubobject(eroso);
								}
							}
						}
						if (isdatapathid){
							OpenFlowUnnumberIfIDEROSubobject eroso= new OpenFlowUnnumberIfIDEROSubobject();
							eroso.setSwitchID((String)edge_list.get(edge_list.size()-1).getTarget()); 
							eroso.setInterfaceID(0); //FIXME: Interfaces set on the request...
							ero.addEROSubobject(eroso);
						} else {
							IPv4prefixEROSubobject eroso= new IPv4prefixEROSubobject();
							eroso.setIpv4address((Inet4Address)edge_list.get(edge_list.size()-1).getTarget());
							eroso.setPrefix(32);
							ero.addEROSubobject(eroso);
						}
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
											
						notifyWavelengthReservation(sourceVertexList,targetVertexList, lambda_chosen);
					} else {
						ero = eroml;
						path.seteRO(ero);
						response.addPath(path);
					}
					long tiempofin =System.nanoTime();
					long tiempotot=tiempofin-tiempoini;
					log.info("Ha tardado "+tiempotot+" nanosegundos");
					u++;
				}
			}
			return m_resp;
		}
		
		else {
			//Hacer lo mismo
			ComputingResponse m_resp = new ComputingResponse();
			m_resp.setEncodingType(pathReq.getEcodingType());
			int t;

			
							
				//Time stamp of the start of the algorithm;
				long tiempoini =System.nanoTime();
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
				
				Bandwidth bandwidth = new Bandwidth();
				bandwidth.setBw(req.getBandwidth().getBw());
				response.setBandwidth(bandwidth);
				
				m_resp.addResponse(response);
				m_resp_individual.addResponse(response);
				
				//esto hay que cambiarlo para poder leer del GENERALIZED END POINTS
				//if (getObjectType(req.getEndPoints()))
				EndPoints  EP= req.getEndPoints();	
				Object source_router_id_addr = null;
				Object dest_router_id_addr = null;
				boolean isdatapathid=false;
				
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
				} else	if (EP.getOT()==ObjectParameters.PCEP_OBJECT_TYPE_ENDPOINTS_DATAPATH_ID){ //CASO QUE NOS INTERESA (
					EndPointDataPathID ep=(EndPointDataPathID) req.getEndPoints();
					source_router_id_addr=ep.getSourceSwitchID();
					dest_router_id_addr=ep.getDestSwitchID();
					isdatapathid=true;
				}
				//aqu� acaba lo que he a�adido
	
				//Now, check if the source and destination are in the TED.
				log.severe("Source: "+source_router_id_addr+"; Destination:"+dest_router_id_addr);
				if (!((((ted).containsVertex(source_router_id_addr))&&((ted).containsVertex(dest_router_id_addr))))){
					log.severe("Source or destination are NOT in the TED");	
					NoPath noPath= new NoPath();
					noPath.setNatureOfIssue(ObjectParameters.NOPATH_NOPATH_SAT_CONSTRAINTS);
					NoPathTLV noPathTLV=new NoPathTLV();
					if (!((ted.containsVertex(source_router_id_addr)))){
						log.severe("Unknown source");	
						noPathTLV.setUnknownSource(true);	
					}
					if (!((ted.containsVertex(dest_router_id_addr)))){
						log.severe("Unknown destination");
						noPathTLV.setUnknownDestination(true);	
					}
	
					noPath.setNoPathTLV(noPathTLV);				
					response.setNoPath(noPath);
					//return m_resp;
				}
						
				SimpleDirectedWeightedGraph<Object, IntraDomainEdge> graph = preComp.getbaseSimplegraph();
				
						
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
				boolean op4a_flag = false;
				boolean op4b_flag = false;
				boolean eroreturned = false;
				ExplicitRouteObject eroml=null;
				
						
				float bwt_req = req.getBandwidth().getBw(); // ancho de banda de la request
					
				GraphPath<Object,IntraDomainEdge> gp = Operacion12.get_op1(graph, source_router_id_addr, dest_router_id_addr
						, graphLock, bwt_req);
					
				if (gp != null)
				{
				//	if (gp.getEdgeList().size() == 1)   // Path correctly found at operation 1 --> 1 hop
				//	{
						nopath = false;
						gp_chosen = gp;
						terminado = true;
						NoLambda = true;
						OP_Counter.setNum_op1();
				//	}
				} else {
					eroml=Operacion34_Initiate.get_op43(graph, source_router_id_addr, dest_router_id_addr, 0);
					if (eroml==null){
						log.fine("No path found"); // NO PATH FOUND
						NoPath noPath= new NoPath();
						noPath.setNatureOfIssue(ObjectParameters.NOPATH_NOPATH_SAT_CONSTRAINTS);
						NoPathTLV noPathTLV=new NoPathTLV();
						noPath.setNoPathTLV(noPathTLV);				
						response.setNoPath(noPath);
						return m_resp;
					}
					eroreturned=true;
					nopath=false;
					
				}
							
				
											
				if (nopath==true){ //FIXME: Llamar al VNTM Aquí
					log.fine("No path found"); // NO PATH FOUND
					NoPath noPath= new NoPath();
					noPath.setNatureOfIssue(ObjectParameters.NOPATH_NOPATH_SAT_CONSTRAINTS);
					NoPathTLV noPathTLV=new NoPathTLV();
					noPath.setNoPathTLV(noPathTLV);				
					response.setNoPath(noPath);
					//return m_resp;
				}
				
				else if (nopath==false){  // PATH FOUND
					Path path=new Path();
					ExplicitRouteObject ero= new ExplicitRouteObject();
					if (!eroreturned){
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
									//WDMlabel.setGrid(preComp.getWSONInfo().getGrid());
									//WDMlabel.setChannelSpacing(preComp.getWSONInfo().getCs());
									//WDMlabel.setN(lambda_chosen+preComp.getWSONInfo().getnMin());
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
								if (isdatapathid){
									OpenFlowUnnumberIfIDEROSubobject eroso= new OpenFlowUnnumberIfIDEROSubobject();
									eroso.setSwitchID((String)edge_list.get(i).getSource()); 
									eroso.setInterfaceID(edge_list.get(i).getSrc_if_id());
									ero.addEROSubobject(eroso);
								}
							}
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
											
						notifyWavelengthReservation(sourceVertexList,targetVertexList, lambda_chosen);
					} else {
						ero = eroml;
						path.seteRO(ero);
						response.addPath(path);
					}
					long tiempofin =System.nanoTime();
					long tiempotot=tiempofin-tiempoini;
					log.info("Ha tardado "+tiempotot+" nanosegundos");
					u++;
				}
			return m_resp;
		}
	}
			
	public void setPreComp(Multilayer_VNTMProv_AlgorithmPreComputation preComp) {
		this.preComp = preComp;
	}
	
	public AlgorithmReservation getReserv() {
		return reserv;
	}
	
	public void notifyWavelengthReservation(LinkedList<Object> sourceVertexList,
        LinkedList<Object> targetVertexList, int wavelength) {
		
		graphLock.lock();
		try{
	        SimpleDirectedWeightedGraph<Object, IntraDomainEdge> networkGraph=networkGraphs_precomp.get(wavelength);
	        for (int i=0;i<sourceVertexList.size();++i){                                
	        	networkGraph.removeEdge(sourceVertexList.get(i), targetVertexList.get(i));
	        }
	    }finally{
	    	graphLock.unlock();        
        }
	}
}
