package tid.pce.computingEngine.algorithms.sson;

import java.net.Inet4Address;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

import org.jgrapht.GraphPath;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;

import tid.ospf.ospfv2.lsa.tlv.subtlv.complexFields.BitmapLabelSet;
import tid.pce.computingEngine.ComputingRequest;
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
import tid.pce.pcep.messages.PCEPResponse;
import tid.pce.pcep.objects.Bandwidth;
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
import tid.pce.pcep.objects.tlvs.NoPathTLV;
import tid.pce.server.wson.ReservationManager;
import tid.pce.tedb.DomainTEDB;
import tid.pce.tedb.IntraDomainEdge;
import tid.pce.tedb.SSONInformation;
import tid.pce.tedb.SimpleTEDB;
import tid.pce.tedb.TEDB;
import tid.pce.tedb.WSONInformation;
import tid.rsvp.RSVPProtocolViolationException;
import tid.rsvp.constructs.gmpls.DWDMWavelengthLabel;
import tid.rsvp.objects.SSONSenderTSpec;
import tid.rsvp.objects.subobjects.GeneralizedLabelEROSubobject;
import tid.rsvp.objects.subobjects.IPv4prefixEROSubobject;
import tid.rsvp.objects.subobjects.UnnumberIfIDEROSubobject;
import tid.util.UtilsFunctions;
import tid.pce.computingEngine.algorithms.utilities.*;

public class SVEC_Dynamic_RSA implements ComputingAlgorithm{

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
	private SVEC_Dynamic_RSAPreComputation preComp;
	
	/**
	 * Access to the Reservation Manager to make reservations of Wavalengths/labels.
	 */
	private ReservationManager reservationManager;
	
	private Lock graphLock;
	
	private SimpleDirectedWeightedGraph<Object,IntraDomainEdge> networkGraph;
	
	private SSONInformation SSONInfo;
	
//	/**
//	 * Number of wavelenghts (labels).
//	 */
	//private int num_lambdas;
	private ArrayList<BitmapLabelSet> setChannels;
	
	/**
	 * Required number of consecutive frequency slots
	 */
	private int num_slots = 0;
	/**
	 * The traffic engineering database
	 */
	private DomainTEDB ted;
	
	int number_rejections=0;
	private GenericLambdaReservation  reserv;
	/**
	 * Constructor
	 * @param pathReq
	 * @param ted
	 * @param reservationManager
	 * @param mf (modulation format)
	 */
	public SVEC_Dynamic_RSA(ComputingRequest pathReq,TEDB ted, ReservationManager reservationManager, int mf ){
		//this.num_lambdas=((DomainTEDB)ted).getWSONinfo().getNumLambdas();
		this.pathReq=pathReq;
		this.reservationManager=reservationManager;
		this.ted=(DomainTEDB)ted;
	}

	/**
	 * Exectutes the path computation and returns the PCEP Response
	 */
	public PCEPResponse call(){ 
		//Timestamp of the start of the algorithm;
		long tiempoini =System.nanoTime();
		long tiempoMSPtotal = 0;
		long tiempoReservaTotal = 0;
		int num_responses=0;
		graphLock=new ReentrantLock();
		log.finest("Starting Dynamic_RSA Algorithm");
		//Create the response message
		//It will contain either the path or noPath
		PCEPResponse m_resp=new PCEPResponse();
		
		networkGraph=PCEPUtils.duplicateTEDDB(((SimpleTEDB)ted).getNetworkGraph());
		
		for (int k=0;k<pathReq.getRequestList().size();++k){
			//The request that needs to be solved
			Request req=pathReq.getRequestList().get(k);
			//Request Id, needed for the response
			long reqId=req.getRequestParameters().getRequestID();
			log.info("Request id: "+reqId+", getting endpoints");
			//Start creating the response
			Response response=new Response();
			RequestParameters rp = new RequestParameters();
			rp.setRequestID(reqId);
			response.setRequestParameters(rp);
			SSONInfo= new SSONInformation();
			SSONInfo=((DomainTEDB)ted).getSSONinfo();

			EndPoints  EP= req.getEndPoints();
			Bandwidth  Bw= req.getBandwidth(); // Objeto bandwidth para saber la demanda de la peticion.
			Object source_router_id_addr = null;
			Object dest_router_id_addr = null;
			
			boolean end=false;//The search has not ended yet
			GraphPath<Object,IntraDomainEdge> gp_chosen=null;
			
			// Conversión Bw a numero de slots en función de la grid.
			int cs;
			int num_labels=0;
			int m=0;
			int Bmod=2; // Spectrum efficiency b/s/Hz (Modulation formats: 16-QAM, QPSK)
	
			log.info("Request bandwidth: "+Bw.getBw());
			num_labels = SSONInfo.getNumLambdas();
			log.info("Num_lambdas "+num_labels);
			// Conversión Bw a numero de slots en función de la grid.

			bandwidthToSlotConversion conversion= new bandwidthToSlotConversion();
			
			// Conversión Bw a numero de slots en función de la grid.	
			if (Bw.getBw()!=0){
				SSONInfo=((DomainTEDB)ted).getSSONinfo();
				cs = SSONInfo.getCs();
				num_slots=conversion.getNumSlots(Bw.getBw(), cs);
			}
				
			/* The set of request-channels will be bounded by the total number of frequency slots
			*  less the number of contiguous frequency slot required for serve the demand plus one.
			*/
			log.info("Request num_slots: "+num_slots);
			setChannels = preComp.getTotalSetChannels().get(num_slots-1);
	
			
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
				
				long tiempofin =System.nanoTime();
				long tiempotot=tiempofin-tiempoini;
//				log.info("Ha tardado "+tiempotot+" nanosegundos");
//				log.info("Number of paths stablished: "+num_responses+", Total servered Bandwidth = "+preComp.getTotalBandwidth());
				preComp.setTotalRejectedBandwidth(Bw.getBw()+ preComp.getTotalRejectedBandwidth());
				number_rejections=number_rejections+1;
				m_resp.addResponse(response);
				end=true;
				//return m_resp;
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
				end=true;
				//return m_resp;
				
			}
	
	
			boolean nopath=true;//Initially, we still have no path
	
			int central_freq=0; // It represents the central frequency slot n.
			
			double max_metric=Integer.MAX_VALUE;
			log.info("Starting the computation");
			
			/**
			 * Comprobacion del estado de los enlaces antes de cada peticion.
			 */
//			Set<IntraDomainEdge> fiberEdges= networkGraph.edgeSet();
//			Iterator<IntraDomainEdge> Iterator = fiberEdges.iterator();
//			while(Iterator.hasNext()){
//			IntraDomainEdge fiberEdge =Iterator.next();
//			FuncionesUtiles.printByte(((BitmapLabelSet)fiberEdge.getTE_info().getAvailableLabels().getLabelSet()).getBytesBitMap(), "Bitmap edge "+fiberEdge.toString()+".", log);
//			}
			
			preComp.getGraphLock().lock();
			try{
				while (!end){
					SimpleDirectedWeightedGraph<Object,IntraDomainEdge> graphLambda=networkGraph;
					long tiempoini_msp =System.nanoTime();
					ModifiedDijkstraSP dsp=new ModifiedDijkstraSP(graphLambda, source_router_id_addr, dest_router_id_addr, Double.POSITIVE_INFINITY, setChannels, num_slots);
					long tiempofin_msp =System.nanoTime();
					tiempoMSPtotal=tiempoMSPtotal+(tiempofin_msp-tiempoini_msp);
					log.info("Fin MDSP");
					GraphPath<Object,IntraDomainEdge> gp=dsp.getPath();
					
					
					if (gp==null){
						log.info("No path found");
						NoPath noPath= new NoPath();
						noPath.setNatureOfIssue(ObjectParameters.NOPATH_NOPATH_SAT_CONSTRAINTS);
						NoPathTLV noPathTLV=new NoPathTLV();
						noPath.setNoPathTLV(noPathTLV);				
						response.setNoPath(noPath);
						nopath=true;
						num_responses++;
						long tiempofin =System.nanoTime();
						long tiempotot=tiempofin-tiempoini;
						preComp.setTotalRejectedBandwidth(Bw.getBw()+ preComp.getTotalRejectedBandwidth());
						number_rejections=number_rejections+1;
						m_resp.addResponse(response);
						
						end=true;
						//return m_resp;
					}
					else{
						BitmapChannelState valid_channels = new BitmapChannelState(dsp.getVertexSpectrumState().get(dest_router_id_addr).getLength());
						valid_channels.setBytesBitmap(dsp.getVertexSpectrumState().get(dest_router_id_addr).getBytesBitmap());
						
						for (int i=0; i<valid_channels.getLength(); i++){
							if ((valid_channels.getBytesBitmap()[i/8]&(0x80>>(i%8))) == (0x80>>i%8)){
								BitmapLabelSet chosen_channel=setChannels.get(i);
								UtilsFunctions.printByte(chosen_channel.getBytesBitMap(),"Bitmap Channel "+i+">>", log);
								central_freq=i+(num_slots+1)/2;
								break;
							}
						}
						
						if (central_freq!=0){
							gp_chosen=gp;
							max_metric=gp.getWeight();
							m=(num_slots)/2;
							log.info("Central Frequency"+central_freq);
							log.info("Frequency width"+m);
							log.info("Path"+gp_chosen);
							nopath=false;
							end=true;
						}else{
							throw new IllegalArgumentException("Invalid central frequency for this request");
						}
					}
				}
			}
			finally{
				preComp.getGraphLock().unlock();	
			}
	
			if (nopath==false){
				num_responses++;
				Path path=new Path();
				ExplicitRouteObject ero= new ExplicitRouteObject();
				List<IntraDomainEdge> edge_list= gp_chosen.getEdgeList();
				int i;
				
				for (i=0;i<edge_list.size();i++){
					UnnumberIfIDEROSubobject eroso= new UnnumberIfIDEROSubobject();
					eroso.setRouterID((Inet4Address)edge_list.get(i).getSource());
					eroso.setInterfaceID(edge_list.get(i).getSrc_if_id());
					eroso.setLoosehop(false);
					ero.addEROSubobject(eroso);
					
					
					//ITU-T Format
					DWDMWavelengthLabel WDMlabel=new DWDMWavelengthLabel();
					WDMlabel.setGrid(preComp.getWSONInfo().getGrid());
					WDMlabel.setChannelSpacing(preComp.getWSONInfo().getCs());
					WDMlabel.setN((central_freq)+preComp.getWSONInfo().getnMin());
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
				
				//log.info("Label bit map: "+ted.getWSONinfo().getCommonAvailableLabels().getLabelSet().toString());
				GeneralizedBandwidthSSON GB_SSON = new GeneralizedBandwidthSSON ();
				
				preComp.setTotalBandwidth((m*Bmod*6.25)+preComp.getTotalBandwidth());
				
//				SSONSenderTSpec mLabel = new SSONSenderTSpec();
//	        	mLabel.setM(m);
//	          	mLabel.encode();
	          
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
				response.addPath(path);
				m_resp.addResponse(response);
			
				LinkedList<Object> sourceVertexList=new LinkedList<Object>();
				LinkedList<Object> targetVertexList=new LinkedList<Object>();
				
				for (i=0;i<edge_list.size();i++){
					sourceVertexList.add(edge_list.get(i).getSource());
					targetVertexList.add(edge_list.get(i).getTarget());
				}	
				sourceVertexList.add(edge_list.get(i-1).getSource());
				targetVertexList.add(edge_list.get(i-1).getTarget());
				
				for (int r=(preComp.getWSONInfo().getnMin()+central_freq)-m; r<(preComp.getWSONInfo().getnMin()+central_freq)+m;r++){
					long tiempoIniReserv = System.nanoTime();
					notifyWavelengthReservation(sourceVertexList,targetVertexList, r);
					long tiempoFinReserv = System.nanoTime();
					tiempoReservaTotal = tiempoReservaTotal + (tiempoFinReserv - tiempoIniReserv);
					
				}
			
			}
		}
		long tiempofin =System.nanoTime();
		long tiempotot=tiempofin-tiempoini;
		log.info("Ha tardado "+tiempotot+" nanosegundos");
		log.info("Ha tardado "+tiempoMSPtotal+" nanosegundos en ejecutar el MSP");
		log.info("Ha tardado "+tiempoReservaTotal+" nanosegundos en ejecutar la reserva");
		log.info("Number of paths rejected: "+number_rejections+", Total unservered Bandwidth = "+preComp.getTotalRejectedBandwidth());
		log.info("Number of total requests: "+num_responses+", Total servered Bandwidth = "+preComp.getTotalBandwidth());
		return m_resp;
	}

	public AlgorithmReservation getReserv() {
		return reserv;
	}
	
	public void notifyWavelengthReservation(
			LinkedList<Object> sourceVertexList,
			LinkedList<Object> targetVertexList, int wavelength) {

		graphLock.lock();
		try{
			//SimpleDirectedWeightedGraph<Object, IntraDomainEdge> networkGraph_=networkGraph;
			for (int i=0;i<sourceVertexList.size()-1;++i){		
				IntraDomainEdge edge=networkGraph.getEdge(sourceVertexList.get(i), targetVertexList.get(i));
				IntraDomainEdge edge_op=networkGraph.getEdge(targetVertexList.get(i),sourceVertexList.get(i));
				edge.getTE_info().setWavelengthOccupied(wavelength);
				edge_op.getTE_info().setWavelengthOccupied(wavelength);
//				FuncionesUtiles.printByte(((BitmapLabelSet)networkGraph.getEdge(sourceVertexList.get(i), targetVertexList.get(i)).getTE_info().getAvailableLabels().getLabelSet()).getBytesBitMap(), "BYtesBitmap", log);
//				if (bidirectional == true){
//					edge=networkGraph.getEdge(targetVertexList.get(i), sourceVertexList.get(i));
//					edge.getTE_info().setWavelengthReserved(wavelength);
//				}
			}}finally{
				graphLock.unlock();	
			}
			

	}
	
	public void notifyWavelengthEndReservation(
			LinkedList<Object> sourceVertexList,
			LinkedList<Object> targetVertexList, int wavelength) {		
		graphLock.lock();
		try{
			for (int i=0;i<sourceVertexList.size();++i){
				IntraDomainEdge edge=networkGraph.getEdge(sourceVertexList.get(i),targetVertexList.get(i) );
				edge.getTE_info().setWavelengthFree(wavelength);

				IntraDomainEdge edge_op=networkGraph.getEdge(targetVertexList.get(i), sourceVertexList.get(i));
				edge_op.getTE_info().setWavelengthUnReserved(wavelength);

		}}finally{
			graphLock.unlock();	
		}

	}
	
	public void setPreComp(SVEC_Dynamic_RSAPreComputation preComp) {
		this.preComp = preComp;
	}
	public static String toHexString(byte [] packetBytes){
		 StringBuffer sb=new StringBuffer(packetBytes.length*2);
		 for (int i=0; i<packetBytes.length;++i){
		  if ((packetBytes[i]&0xFF)<=0x0F){
		   sb.append('0');
		   sb.append(Integer.toHexString((packetBytes[i]&0xFF))); 
		  }
		  else {
		   sb.append(Integer.toHexString((packetBytes[i]&0xFF)));
		  }
		 }
		 return sb.toString();
		 
		 }


}
