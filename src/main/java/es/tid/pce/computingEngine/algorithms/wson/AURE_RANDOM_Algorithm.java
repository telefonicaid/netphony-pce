package es.tid.pce.computingEngine.algorithms.wson;

import java.net.Inet4Address;
import java.util.ArrayList;
import java.util.Hashtable;
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
import es.tid.pce.server.wson.ReservationManager;
import es.tid.rsvp.RSVPProtocolViolationException;
import es.tid.rsvp.constructs.gmpls.DWDMWavelengthLabel;
import es.tid.rsvp.objects.subobjects.GeneralizedLabelEROSubobject;
import es.tid.rsvp.objects.subobjects.IPv4prefixEROSubobject;
import es.tid.rsvp.objects.subobjects.UnnumberIfIDEROSubobject;
import es.tid.tedb.DomainTEDB;
import es.tid.tedb.IntraDomainEdge;
import es.tid.tedb.TEDB;

public class AURE_RANDOM_Algorithm implements ComputingAlgorithm {
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
	private AURE_RANDOM_AlgorithmPreComputation preComp;
	
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
	
	public AURE_RANDOM_Algorithm(ComputingRequest pathReq,TEDB ted, ReservationManager reservationManager ){
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
		log.finest("Starting AURE-RANDOM Algorithm");
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
		rp.setBidirect(req.getRequestParameters().isBidirect());
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
			
		int lambda_chosen=0;//We begin with lambda index 0
		Integer lambda=0;//We begin with lambda index 0
		boolean end=false;//The search has not ended yet
		Hashtable<Integer,GraphPath<Object,IntraDomainEdge>> LambdaRouteTable = new Hashtable<Integer,GraphPath<Object,IntraDomainEdge>>();
		ArrayList<Integer> Lambda_list = new ArrayList<Integer>();
				
		log.fine("Starting the computation");
		
		/*GraphPath<Object,IntraDomainEdge> gp_chosen=null;
		
		SimpleDirectedWeightedGraph<Object, IntraDomainEdge> Graph=preComp.getNetworkGraphs();
				
		DijkstraShortestPath<Object,IntraDomainEdge>  dsp=new DijkstraShortestPath<Object,IntraDomainEdge> (Graph, source_router_id_addr, dest_router_id_addr);
		
		GraphPath<Object,IntraDomainEdge> gp=dsp.getPath();
		
		if (gp == null){
			log.fine("No path found");
		}
		
		else if (gp != null) {
			nopath = false;		
			gp_chosen = gp; //graph path
		}*/
		int k=0;
		
		GraphPath<Object,IntraDomainEdge> gp_chosen=null;
		preComp.getGraphLock().lock();
		try{
			while (!end){
				SimpleDirectedWeightedGraph<Object,IntraDomainEdge> graphLambda=preComp.getNetworkGraphs().get(lambda); 
				DijkstraShortestPath<Object,IntraDomainEdge>  dsp=new DijkstraShortestPath<Object,IntraDomainEdge> (graphLambda, source_router_id_addr, dest_router_id_addr);
				GraphPath<Object,IntraDomainEdge> gp=dsp.getPath();
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
			
			
			
			/*//set of available lambdas
			int max_lambdas=((BitmapLabelSet)edge_list.get(0).getTE_info().getAvailableLabels().getLabelSet()).getNumLabels();
			int num_bytes=((BitmapLabelSet)edge_list.get(0).getTE_info().getAvailableLabels().getLabelSet()).getBytesBitMap().length;
			byte [] bitmap=new byte[num_bytes];
			for (int i=0;i<num_bytes;++i){
				bitmap[i]=(byte)(((BitmapLabelSet)edge_list.get(0).getTE_info().getAvailableLabels().getLabelSet()).getBytesBitMap()[i]|((BitmapLabelSet)edge_list.get(0).getTE_info().getAvailableLabels().getLabelSet()).getBytesBitmapReserved()[i]);	
			}
			
			for (int j=1;j<edge_list.size();++j){
				for (int i=0;i<num_bytes;++i){
					bitmap[i]=(byte)(bitmap[i] | (((BitmapLabelSet)edge_list.get(j).getTE_info().getAvailableLabels().getLabelSet()).getBytesBitMap()[i]|((BitmapLabelSet)edge_list.get(j).getTE_info().getAvailableLabels().getLabelSet()).getBytesBitmapReserved()[i]));	
				}
			}
			//System.out.println("lambdas disponibles:");
			int[] lista= new int[max_lambdas];
			int k=0;
			for (int i=0; i<max_lambdas;++i){
				int num_byte=i/8;
				if ((bitmap[num_byte]&(0x80>>>(i%8)))==0){
					lista[k] = i; // list of lambdas available in the whole path
					k++;
					//System.out.println("lambda id:"+i);
				}			
			}
			//System.out.println("Num lambdas:"+num_lambdas);
			*/
			
			int semilla_aleatoria = (int) Math.floor(Math.random()*k);
			//lambda_chosen = lista[semilla_aleatoria];
			lambda_chosen = Lambda_list.get(semilla_aleatoria);
			
			gp_chosen = LambdaRouteTable.get(lambda_chosen);
			
			
			Path path=new Path();
			
			ExplicitRouteObject ero= new ExplicitRouteObject();
			List<IntraDomainEdge> edge_list=gp_chosen.getEdgeList();
						
			int i;
						
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
	
	public void setPreComp(AURE_RANDOM_AlgorithmPreComputation preComp) {
		this.preComp = preComp;
	}
	
	public AlgorithmReservation getReserv() {
		return reserv;
	}
}
