package es.tid.pce.computingEngine.algorithms.sson;

import java.net.Inet4Address;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import es.tid.pce.pcep.objects.Metric;
import es.tid.pce.pcep.objects.Monitoring;
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
import es.tid.tedb.SimpleTEDB;
import es.tid.tedb.TEDB;

/**
 * Svec version of Dynamic_RSA Algorithm".
 * 
 * ref: A. Castro et al., Dynamic routing and spectrum (re)allocation in future flexgrid optical networks, Comput.
 * Netw. (2012), http://dx.doi.org/10.1016/j.comnet.2012.05.001
 *
 * @author arturo mayoral
 *
 */
public class SVEC_AURE_SSON_algorithm implements ComputingAlgorithm {

	/**
	 * The Logger.
	 */
	private Logger log=LoggerFactory.getLogger("PCEServer");
	
	/**
	 * The Path Computing Request to calculate.
	 */
	private ComputingRequest pathReq;

	/**
	 * Access to the Precomputation part of the algorithm.
	 */
	private SVEC_AURE_SSON_algorithmPreComputation preComp;
	
	/**
	 * Access to the Reservation Manager to make reservations of Wavalengths/labels.
	 */
	private ReservationManager reservationManager;
	
	/**
	 * Guardamos el grafo de la red original y hacemos una copia sobre la ejecutaremos la computación de nuestro
	 * algoritmo y donde efectuaremos la consecuentes reservas, de cara a no "ensuciar" nuestro grafo original.
	 */
	private ArrayList<SimpleDirectedWeightedGraph<Object, IntraDomainEdge>> networkGraphs_precomp;
	
	/**
	 * Lock to protect the graph during the computation.
	 */
	private Lock graphLock;
	
	/**
	 * Traffic engineering information. 
	 */
	
	private SSONInformation SSONInfo;
	
	/**
	 * The traffic engineering database
	 */
	private DomainTEDB ted;
	
	
	private GenericLambdaReservation  reserv;
	
	public SVEC_AURE_SSON_algorithm(ComputingRequest pathReq,TEDB ted, ReservationManager reservationManager, int mf ){
		//this.num_lambdas=((DomainTEDB)ted).getWSONinfo().getNumLambdas();
		this.pathReq=pathReq;
		this.reservationManager=reservationManager;
		this.ted=(DomainTEDB)ted;
	}

	public ComputingResponse call(){ 
		//Timestamp of the start of the algorithm;
		long tiempoini =System.nanoTime();
		int num_slots = 0;
		int cs;
		int m=0,t=0;
		int num_responses=0;
		graphLock=new ReentrantLock();
		
		log.info("Starting SVEC AURE_SSON Algorithm");
		//Create the response message
		//It will contain either the path or noPath
		ComputingResponse m_resp=new ComputingResponse();
		m_resp.setEncodingType(pathReq.getEcodingType());
		networkGraphs_precomp= new ArrayList <SimpleDirectedWeightedGraph <Object,IntraDomainEdge>>(preComp.getSSONInfo().getNumLambdas());
		SimpleDirectedWeightedGraph<Object,IntraDomainEdge> networkGraph=new SimpleDirectedWeightedGraph<Object,IntraDomainEdge>(IntraDomainEdge.class);
		for (t=0;t<=preComp.getSSONInfo().getNumLambdas()-1;t++){
			networkGraph=PCEPUtils.duplicateTEDDB(((SimpleTEDB)ted).getNetworkGraph());
			networkGraphs_precomp.add(t, networkGraph);
		}
		for (int k=0;k<pathReq.getRequestList().size();++k){
				//The request that needs to be solved
				Request req=pathReq.getRequestList().get(k);
				//Request Id, needed for the response
				long reqId=req.getRequestParameters().getRequestID();
				//Start creating the response
				Response response=new Response();
				RequestParameters rp = new RequestParameters();
				rp.setRequestID(reqId);
				response.setRequestParameters(rp);
				EndPoints  EP= req.getEndPoints();
				
				// Objeto bandwidth para saber la demanda de la peticion.
				BandwidthRequested  Bw= (BandwidthRequested)req.getBandwidth(); 
				
				Inet4Address source_router_id_addr = null;
				Inet4Address dest_router_id_addr = null;
				graphs_comparator grc = new graphs_comparator ();
		
				// Conversión Bw a numero de slots en función de la grid.

				bandwidthToSlotConversion conversion= new bandwidthToSlotConversion();
				
				boolean nopath=true;//Initially, we still have no path
				boolean end=false;//The search has not ended yet
				int lambda=0;//We begin with lambda index 0
				int lambda_chosen=0;//We begin with lambda index 0
				int central_freq=0; // It represents the central frequency slot n.
				int counter=0;
				boolean is_equal = false;
				
				// Conversión Bw a numero de slots en función de la grid.	
				if (Bw.getBw()!=0){
					SSONInfo=preComp.getSSONInfo();
					cs = SSONInfo.getCs();
					num_slots=conversion.getNumSlots(Bw.getBw(), cs);
				}
				
				if (EP.getOT()==ObjectParameters.PCEP_OBJECT_TYPE_ENDPOINTS_IPV4){
					EndPointsIPv4  ep=(EndPointsIPv4) req.getEndPoints();
					source_router_id_addr=ep.getSourceIP();
					dest_router_id_addr=ep.getDestIP();
				}
				else if (EP.getOT()==ObjectParameters.PCEP_OBJECT_TYPE_ENDPOINTS_IPV6){
		
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
					log.warn("Source or destination are NOT in the TED");	
					NoPath noPath= new NoPath();
					noPath.setNatureOfIssue(ObjectParameters.NOPATH_NOPATH_SAT_CONSTRAINTS);
					NoPathTLV noPathTLV=new NoPathTLV();
					if (!((ted.containsVertex(source_router_id_addr)))){
						log.debug("Unknown source");	
						noPathTLV.setUnknownSource(true);	
					}
					if (!((ted.containsVertex(dest_router_id_addr)))){
						log.debug("Unknown destination");
						noPathTLV.setUnknownDestination(true);	
					}
		
					noPath.setNoPathTLV(noPathTLV);				
					response.setNoPath(noPath);
					m_resp.addResponse(response);
					end=true;
					nopath=true;
					//return m_resp;
				}
				// check if src and dst are the same 
				if (source_router_id_addr.equals(dest_router_id_addr)){
					log.info("Source and destination are the same!");
					Path path=new Path();
					ExplicitRouteObject ero= new ExplicitRouteObject();
					IPv4prefixEROSubobject eroso= new IPv4prefixEROSubobject();
					eroso.setIpv4address(source_router_id_addr);
					eroso.setPrefix(32);
					ero.addEROSubobject(eroso);
					path.setEro(ero);
					
					if (req.getMetricList().size()!=0){
						Metric metric=new Metric();
						metric.setMetricType(req.getMetricList().get(0).getMetricType() );
						log.debug("Number of hops "+0);
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
					end=true;
					nopath=true;
					//return m_resp;
					
				}
		
						
				double max_metric=Integer.MAX_VALUE;
				log.debug("Starting the computation");
				GraphPath<Object,IntraDomainEdge> gp_chosen=null;
				GraphPath<Object,IntraDomainEdge> gp_trully_chosen=null;
				
				try{
					while (!end){
					
						SimpleDirectedWeightedGraph<Object,IntraDomainEdge> graphLambda=networkGraphs_precomp.get(lambda); 
						DijkstraShortestPath<Object,IntraDomainEdge>  dsp=new DijkstraShortestPath<Object,IntraDomainEdge> (graphLambda, source_router_id_addr, dest_router_id_addr);
						GraphPath<Object,IntraDomainEdge> gp=dsp.getPath();
						
						if (gp==null){				
							//There is no path here
							counter=0;
							if (lambda>=preComp.getSSONInfo().getNumLambdas()-1){
								if (nopath==true){
									log.info("No path found!");
									NoPath noPath= new NoPath();
									noPath.setNatureOfIssue(ObjectParameters.NOPATH_NOPATH_SAT_CONSTRAINTS);
									NoPathTLV noPathTLV=new NoPathTLV();
									noPath.setNoPathTLV(noPathTLV);				
									response.setNoPath(noPath);
//									end=true;
									
									long tiempofin =System.nanoTime();
									long tiempotot=tiempofin-tiempoini;
									log.info("Ha tardado "+tiempotot+" nanosegundos");
									m_resp.addResponse(response);
									end=true;
									nopath=true;
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
										log.info("No path found");
										NoPath noPath= new NoPath();
										noPath.setNatureOfIssue(ObjectParameters.NOPATH_NOPATH_SAT_CONSTRAINTS);
										NoPathTLV noPathTLV=new NoPathTLV();
										noPath.setNoPathTLV(noPathTLV);				
										response.setNoPath(noPath);
										
										long tiempofin =System.nanoTime();
										long tiempotot=tiempofin-tiempoini;
										log.info("Ha tardado "+tiempotot+" nanosegundos");
										m_resp.addResponse(response);
										end=true;
										nopath=true;
									}else {
										end=true;
									}
			
								}
								else{
									if (gp.getWeight()<max_metric){
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
											if(nopath ==  true){
												log.info("No path found");
												NoPath noPath= new NoPath();
												noPath.setNatureOfIssue(ObjectParameters.NOPATH_NOPATH_SAT_CONSTRAINTS);
												NoPathTLV noPathTLV=new NoPathTLV();
												noPath.setNoPathTLV(noPathTLV);				
												response.setNoPath(noPath);
												
												long tiempofin =System.nanoTime();
												long tiempotot=tiempofin-tiempoini;
												log.info("Ha tardado "+tiempotot+" nanosegundos");
												m_resp.addResponse(response);
												end=true;
												nopath=true;
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
									if (lambda>=preComp.getSSONInfo().getNumLambdas()-1){
										end=true;
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
				}
		
				if (nopath==false){
					num_responses++;
					Path path=new Path();
					ExplicitRouteObject ero= new ExplicitRouteObject();
					List<IntraDomainEdge> edge_list=gp_trully_chosen.getEdgeList();
					int i,j;
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
						WDMlabel.setM(m);
						WDMlabel.setIdentifier((int)reqId);
						try {
							WDMlabel.encode();
						} catch (RSVPProtocolViolationException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						
						GeneralizedLabelEROSubobject genLabel= new GeneralizedLabelEROSubobject();

						genLabel.setLabel(WDMlabel.getBytes());
//						for (j= (preComp.getWSONInfo().getnMin()+central_freq)-m; j<(preComp.getWSONInfo().getnMin()+central_freq)+m;j++){
//							if (edge_list.get(i).getTE_info().isWavelengthFree(j)){	
//								edge_list.get(i).getTE_info().setWavelengthOccupied(j);
//							}		
//						}
						ero.addEROSubobject(genLabel);
						genLabel.setLabel(WDMlabel.getBytes());	
					}
		
					
					IPv4prefixEROSubobject eroso= new IPv4prefixEROSubobject();
					eroso.setIpv4address((Inet4Address)edge_list.get(edge_list.size()-1).getTarget());
					eroso.setPrefix(32);
					ero.addEROSubobject(eroso);
					path.setEro(ero);
					PCEPUtils.completeMetric(path, req, edge_list);
					response.addPath(path);
					m_resp.addResponse(response);
					
					//FIXME: RESERVATION NEEDS TO BE IMPROVED!!!
					LinkedList<Inet4Address> sourceVertexList=new LinkedList<Inet4Address>();
					LinkedList<Inet4Address> targetVertexList=new LinkedList<Inet4Address>();
					for (i=0;i<edge_list.size();i++){
						sourceVertexList.add((Inet4Address)edge_list.get(i).getSource());
						targetVertexList.add((Inet4Address)edge_list.get(i).getTarget());
					}	
					//sourceVertexList.add(edge_list.get(i-1).getSource());
					//targetVertexList.add(edge_list.get(i-1).getTarget());
						
					for (int r=(preComp.getSSONInfo().getnMin()+central_freq)-m; r<(preComp.getSSONInfo().getnMin()+central_freq)+m;r++){
						notifyWavelengthReservation(sourceVertexList,targetVertexList, r);
					}
									
				}
			}
		long tiempofin =System.nanoTime();
		long tiempotot=tiempofin-tiempoini;
		log.info("Ha tardado "+tiempotot+" nanosegundos");
		return m_resp;
	}
	
	public void setPreComp(SVEC_AURE_SSON_algorithmPreComputation preComp) {
		this.preComp = preComp;
	}

	public AlgorithmReservation getReserv() {
		return reserv;
	}
	
	public void notifyWavelengthReservation(
			LinkedList<Inet4Address> sourceVertexList,
			LinkedList<Inet4Address> targetVertexList, int wavelength) {
		graphLock.lock();
		try{
			SimpleDirectedWeightedGraph<Object, IntraDomainEdge> networkGraph=networkGraphs_precomp.get(wavelength);
			for (int i=0;i<sourceVertexList.size();++i){				
				networkGraph.removeEdge(sourceVertexList.get(i), targetVertexList.get(i));
				
			}}finally{
				graphLock.unlock();	
			}
	}

	public Lock getGraphLock() {
		return graphLock;
	}

	public void setGraphLock(Lock graphLock) {
		this.graphLock = graphLock;
	}
//	
//	public void notifyWavelengthStatusChange(Inet4Address source,
//			Inet4Address destination, BitmapLabelSet previousBitmapLabelSet,
//			BitmapLabelSet newBitmapLabelSet) {
//
//		previousBitmapLabelSet.getNumLabels();
//		int num_bytes=previousBitmapLabelSet.getBytesBitMap().length;
//		int wavelength_to_occupy=-1;
//		int wavelength_to_free=-1;
//		try{
//			graphLock.lock();
//			for (int i=0;i<num_bytes;++i){
//				if (previousBitmapLabelSet.getBytesBitMap()[i]!=newBitmapLabelSet.getBytesBitMap()[i]){
//					for (int k=0;k<8;++k){
//						if ((newBitmapLabelSet.getBytesBitMap()[i]&(0x80>>>k))>(previousBitmapLabelSet.getBytesBitMap()[i]&(0x80>>>k))){
//							wavelength_to_occupy=k+(i*8);
//							SimpleDirectedWeightedGraph<Inet4Address, IntraDomainEdge> networkGraph=networkGraphs_precomp.get(wavelength_to_occupy);
//							networkGraph.removeEdge(source, destination);		
//
//						}else if ((newBitmapLabelSet.getBytesBitMap()[i]&(0x80>>>k))<(previousBitmapLabelSet.getBytesBitMap()[i]&(0x80>>>k))){
//							if ((newBitmapLabelSet.getBytesBitmapReserved()[i]&(0x80>>>k))==0){
//								wavelength_to_free=k+(i*8);	
//								SimpleDirectedWeightedGraph<Inet4Address, IntraDomainEdge> networkGraph=networkGraphs_precomp.get(wavelength_to_free);
//								networkGraph.addEdge(source, destination);
//							}
//
//						}
//					}
//				}
//			}
//
//		}finally{
//			graphLock.unlock();	
//		}
//	}

}
