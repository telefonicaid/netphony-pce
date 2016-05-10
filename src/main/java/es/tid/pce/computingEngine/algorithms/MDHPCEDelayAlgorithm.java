package es.tid.pce.computingEngine.algorithms;

import es.tid.of.DataPathID;
import es.tid.pce.computingEngine.ComputingRequest;
import es.tid.pce.computingEngine.ComputingResponse;
import es.tid.pce.parentPCE.ChildPCERequestManager;
import es.tid.pce.pcep.constructs.*;
import es.tid.pce.pcep.messages.PCEPInitiate;
import es.tid.pce.pcep.messages.PCEPRequest;
import es.tid.pce.pcep.objects.*;
import es.tid.pce.pcep.objects.subobjects.UnnumberIfIDXROSubobject;
import es.tid.pce.pcep.objects.subobjects.XROSubObjectValues;
import es.tid.pce.pcep.objects.subobjects.XROSubobject;
import es.tid.pce.pcep.objects.tlvs.EndPointIPv4TLV;
import es.tid.pce.pcep.objects.tlvs.NoPathTLV;
import es.tid.rsvp.objects.subobjects.UnnumberIfIDEROSubobject;
import es.tid.rsvp.objects.subobjects.UnnumberedDataPathIDEROSubobject;
import es.tid.tedb.*;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.alg.KShortestPaths;
import org.jgrapht.graph.DirectedWeightedMultigraph;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.*;
import java.util.logging.Logger;

/**
 *
 * @author Andrea Sgambelluri
 *
 */




public class MDHPCEDelayAlgorithm implements ComputingAlgorithm{
	private DirectedWeightedMultigraph<Object,InterDomainEdge> Graph;
	Hashtable<Inet4Address,DomainTEDB> intraTEDBs = null;
	private Logger log=Logger.getLogger("PCEServer");
	private ComputingRequest pathReq;
	private ChildPCERequestManager childPCERequestManager;
	private ReachabilityManager reachabilityManager;
	FileWriter fstream = null;
	BufferedWriter bw = null;
	private boolean acceptIntraReq=true;

	public MDHPCEDelayAlgorithm(ComputingRequest pathReq, TEDB ted, ChildPCERequestManager cprm , ReachabilityManager rm){
		if(ted.isITtedb()){
			this.Graph=((ITMDTEDB)ted).getDuplicatedMDNetworkGraph();
		}else{
			this.Graph=((MDTEDB)ted).getDuplicatedMDNetworkGraph();
		}
		this.reachabilityManager=rm;
		this.pathReq=pathReq;		
		this.childPCERequestManager=cprm;
	}

	public MDHPCEDelayAlgorithm(ComputingRequest pathReq, TEDB ted, ChildPCERequestManager cprm , ReachabilityManager rm, Hashtable<Inet4Address,DomainTEDB> intra){
		if(ted.isITtedb()){
			this.Graph=((ITMDTEDB)ted).getDuplicatedMDNetworkGraph();
		}else{
			this.Graph=((MDTEDB)ted).getDuplicatedMDNetworkGraph();
		}

		this.intraTEDBs=intra;
		this.reachabilityManager=rm;
		this.pathReq=pathReq;
		this.childPCERequestManager=cprm;
	}
	
	public ComputingResponse call()throws Exception{

		
		long tiempoini =System.nanoTime();
		ComputingResponse m_resp=new ComputingResponse();
		m_resp.setReachabilityManager(reachabilityManager);
		m_resp.setEncodingType(pathReq.getEcodingType());
		Request req=pathReq.getRequestList().get(0);
		long reqId=req.getRequestParameters().getRequestID();
		//log.info("Processing MD Path Computing with MDHPCEDelayAlgorithm (Minimum e2e delay)with Request id: "+reqId);

		Response response=new Response();
		RequestParameters rp = new RequestParameters();
		rp.setRequestID(reqId);
		response.setRequestParameters(rp);
		EndPoints  EP= req.getEndPoints();

		Inet4Address source_router_id_addr = null;
		Inet4Address dest_router_id_addr = null;

		if (EP.getOT()==ObjectParameters.PCEP_OBJECT_TYPE_ENDPOINTS_IPV4){
			EndPointsIPv4  ep=(EndPointsIPv4) req.getEndPoints();
			source_router_id_addr=ep.getSourceIP();
			dest_router_id_addr=ep.getDestIP();
		}
		else if (EP.getOT()==ObjectParameters.PCEP_OBJECT_TYPE_ENDPOINTS_IPV6){

		}

		if (EP.getOT()==ObjectParameters.PCEP_OBJECT_TYPE_GENERALIZED_ENDPOINTS){
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

		//First, we obtain the domains of each endPoint
		Inet4Address source_domain_id=this.reachabilityManager.getDomain(source_router_id_addr);
		Inet4Address dest_domain_id=this.reachabilityManager.getDomain(dest_router_id_addr);

		//CHECK IF DOMAIN_ID ARE NULL!!!!!!
		log.info("Check if SRC and Dest domains are OK");
		if ((dest_domain_id==null)||(source_domain_id==null)){
			//ONE OF THEM IS NOT REACHABLE, SEND NOPATH!!!
			log.warning("One of the domains is not reachable, sending NOPATH");
			log.info( "Source domain = "+source_domain_id );
			log.info( "Dest domain = "+dest_domain_id );
			NoPath noPath= new NoPath();
			noPath.setNatureOfIssue(ObjectParameters.NOPATH_NOPATH_SAT_CONSTRAINTS);
			response.setNoPath(noPath);
			m_resp.addResponse(response);
			return m_resp;
		}
		LinkedList<PCEPRequest> reqList= new LinkedList<PCEPRequest>();
		LinkedList<PCEPInitiate> initList= new LinkedList<PCEPInitiate>();
		LinkedList<Object> domainList= new LinkedList<Object>();
		LinkedList <ComputingResponse> respList;

		if (source_domain_id.equals( dest_domain_id )) {
			if (!acceptIntraReq){
				log.warning( "Not MD path: the two end points are in the same domain ("+source_domain_id+")" );
				NoPath noPath2 = new NoPath();
				noPath2.setNatureOfIssue( ObjectParameters.NOPATH_NOPATH_SAT_CONSTRAINTS );
				NoPathTLV noPathTLV = new NoPathTLV();
				noPath2.setNoPathTLV( noPathTLV );
				response.setNoPath( noPath2 );
				m_resp.addResponse( response );
				return m_resp;
			}
			else{
				log.info( "Not MD path (src and dst in the same domain): just for test purpose computation of the shortest delay path in domain ("+source_domain_id+")" );


				//DELAY GRAPH for SRC domain
				SimpleDirectedWeightedGraph<Object, IntraDomainEdge> srcgraph = null;
				SimpleTEDB ssTED = (SimpleTEDB) intraTEDBs.get( source_domain_id );
				srcgraph = ssTED.getDuplicatedNetworkGraph();

				Iterator<IntraDomainEdge> siter = srcgraph.edgeSet().iterator();

				while (siter.hasNext()) {
					IntraDomainEdge saa = siter.next();

					srcgraph.setEdgeWeight( saa, saa.TE_info.getUndirLinkDelay().getDelay() );
				}

				log.info( "Computing path in src domain" );

				DijkstraShortestPath<Object, IntraDomainEdge> sdsp = new DijkstraShortestPath<Object, IntraDomainEdge>( srcgraph, source_router_id_addr, dest_router_id_addr );
				GraphPath<Object, IntraDomainEdge> sgp1 = sdsp.getPath();

				if (sgp1 == null) {
					log.warning( "No path between src and dst" );
					NoPath noPath2 = new NoPath();
					noPath2.setNatureOfIssue( ObjectParameters.NOPATH_NOPATH_SAT_CONSTRAINTS );
					NoPathTLV noPathTLV = new NoPathTLV();
					noPath2.setNoPathTLV( noPathTLV );
					response.setNoPath( noPath2 );
					m_resp.addResponse( response );
					return m_resp;
				}

				Path spath1 = new Path();
				ExplicitRouteObject serosrc = new ExplicitRouteObject();
				List<IntraDomainEdge> slinks = sgp1.getEdgeList();



				int si;
				for (si = 0; si < slinks.size(); si++) {
					if (slinks.get( si ).getSource() instanceof Inet4Address) {
						UnnumberIfIDEROSubobject eroso = new UnnumberIfIDEROSubobject();
						eroso.setRouterID( (Inet4Address) slinks.get( si ).getSource() );
						eroso.setLoosehop( false );
						serosrc.addEROSubobject( eroso );
					} else if (slinks.get( si ).getSource() instanceof DataPathID) {
						UnnumberedDataPathIDEROSubobject eroso = new UnnumberedDataPathIDEROSubobject();
						eroso.setDataPath( (DataPathID) slinks.get( si ).getSource() );
						eroso.setLoosehop( false );
						serosrc.addEROSubobject( eroso );
					} else {
						log.info( "Edge instance error" );
					}
				}

				// Add last hop in the ERO Object
				if (slinks.size() > 0) {
					if (slinks.get( slinks.size() - 1 ).getTarget() instanceof Inet4Address) {
						UnnumberIfIDEROSubobject eroso = new UnnumberIfIDEROSubobject();
						eroso.setRouterID( (Inet4Address) slinks.get( slinks.size() - 1 ).getTarget() );
						eroso.setLoosehop( false );
						serosrc.addEROSubobject( eroso );
					} else if (slinks.get( slinks.size() - 1 ).getTarget() instanceof DataPathID) {
						UnnumberedDataPathIDEROSubobject eroso = new UnnumberedDataPathIDEROSubobject();
						eroso.setDataPath( (DataPathID) slinks.get( slinks.size() - 1 ).getTarget() );
						eroso.setLoosehop( false );
						serosrc.addEROSubobject( eroso );
					}
				}
				else {
					//log.info( "no links on the path" );
					UnnumberIfIDEROSubobject eroso = new UnnumberIfIDEROSubobject();
					eroso.setRouterID( (Inet4Address) source_router_id_addr );
					eroso.setLoosehop( false );
					serosrc.addEROSubobject( eroso );
				}
				//log.info("Domain "+ source_domain_id+" ero: " +serosrc.toString());

				if (req.getMetricList().size()!=0){
					Metric metric=new Metric();
					metric.setMetricType(req.getMetricList().get(0).getMetricType() );
					log.fine("Number of hops "+slinks.size());
					float metricValue=(float)slinks.size();
					metric.setMetricValue(metricValue);
					spath1.getMetricList().add(metric);
				}

				int delay = (int) sgp1.getWeight();
				log.info("The end to end delay in  "+ source_domain_id+" is " +delay);


				PCEPInitiate sp_init_Src = new PCEPInitiate();
				PCEPIntiatedLSP slsp_inis = new PCEPIntiatedLSP();
				sp_init_Src.getPcepIntiatedLSPList().add(slsp_inis);
				EndPoints seps;

				// Creation of PCEPInit message for src domain

				log.info("Sending init");
				seps=new EndPointsIPv4();
				((EndPointsIPv4) seps).setSourceIP(source_router_id_addr);
				((EndPointsIPv4) seps).setDestIP(dest_router_id_addr);
				slsp_inis.setEndPoint(seps);
				SRP ssrps= new SRP();
				ssrps.setRFlag(false);
				slsp_inis.setRsp(ssrps);
				LSP slsps = new LSP();
				slsp_inis.setLsp(slsps);
				ssrps.setSRP_ID_number(1);
				slsp_inis.setEro(serosrc);
				initList.add( sp_init_Src );
				domainList.add( source_domain_id );


				//SEND INIT messages
				StateReport inireply = null;
				for (int skk=0;skk<initList.size();++skk) {
					try {

						if (domainList.get(skk)!=this.reachabilityManager.getDomain((Inet4Address) InetAddress.getByName( "10.4.1.1" ))) {

							inireply = childPCERequestManager.newIni( initList.get( skk ), domainList.get( skk ) );

						}
						else {
							//tricky solution for TID issue in childPCE
							log.info("TID domain");
							inireply= new StateReport();
						}

					} catch (Exception e) {
						log.severe( "PROBLEM SENDING THE INIT" );
						NoPath noPathx = new NoPath();
						noPathx.setNatureOfIssue( ObjectParameters.NOPATH_NOPATH_SAT_CONSTRAINTS );
						NoPathTLV snoPathTLV = new NoPathTLV();
						noPathx.setNoPathTLV( snoPathTLV );
						response.setNoPath( noPathx );
						m_resp.addResponse( response );
						return m_resp;
					}

					if (inireply == null) {
						log.warning("Child for init "+(skk+1)+" has failed");
						NoPath snoPath= new NoPath();
						response.setNoPath(snoPath);
						m_resp.addResponse(response);
						return m_resp;
					}
					else {
						log.info("Domain "+ domainList.get(skk)+" replied to Initiate: "+inireply.toString());
						//if state ok
						spath1.setEro(serosrc);
						response.addPath(spath1);
						m_resp.addResponse(response);
						return m_resp;

						//check Report ok
					}

				}





			}
		}

		log.info("MD Request from "+source_router_id_addr+" (domain "+source_domain_id+") to "+ dest_router_id_addr+" (domain "+dest_domain_id+")");

		//DELAY GRAPH example
		DirectedWeightedMultigraph<Object,InterDomainEdge> networkGraph = null;
		networkGraph= (DirectedWeightedMultigraph<Object, InterDomainEdge>) Graph.clone();
		//networkGraph= Graph;

		//log.info( Graph.toString() );
		Iterator<InterDomainEdge> iterator1 = networkGraph.edgeSet().iterator();
		InterDomainEdge edge;
		while (iterator1.hasNext()){
			edge = iterator1.next();
			networkGraph.setEdgeWeight(edge, edge.TE_info.getUndirLinkDelay().getDelay());
		}

		if (!((networkGraph.containsVertex(source_domain_id))&&(networkGraph.containsVertex(dest_domain_id)))){
			Iterator<Object> it = networkGraph.vertexSet().iterator();

			log.warning("Source or destination domains are NOT in the TED");
			//FIXME: VER ESTE CASO
			NoPath noPath= new NoPath();
			noPath.setNatureOfIssue(ObjectParameters.NOPATH_NOPATH_SAT_CONSTRAINTS);
			NoPathTLV noPathTLV=new NoPathTLV();
			if (!((networkGraph.containsVertex(source_router_id_addr)))){
				log.finest("Unknown source domain");
				noPathTLV.setUnknownSource(true);
			}
			if (!((networkGraph.containsVertex(dest_router_id_addr)))){
				log.finest("Unknown destination domain");
				noPathTLV.setUnknownDestination(true);
			}

			noPath.setNoPathTLV(noPathTLV);
			response.setNoPath(noPath);
			m_resp.addResponse(response);
			return m_resp;
		}


		//log.info("Processing XRO");
		//processXRO(req.getXro(),networkGraph);
		//MDFunctions.processXRO(req.getXro(),reachabilityManager, networkGraph);

		log.info("Computing MD Sequence of domains");
		//Now, compute the shortest sequence of domains
		//delay
		KShortestPaths dsp= new KShortestPaths(networkGraph, source_domain_id, 4);
		//hops
		//KShortestPaths dsp= new KShortestPaths(Graph, source_domain_id, 4);

		List<GraphPath<Inet4Address,InterDomainEdge>> gps=dsp.getPaths(dest_domain_id);
		if (gps==null){
			log.severe("Problem getting the domain sequence");
			NoPath noPath2= new NoPath();
			noPath2.setNatureOfIssue(ObjectParameters.NOPATH_NOPATH_SAT_CONSTRAINTS);
			NoPathTLV noPathTLV=new NoPathTLV();
			noPath2.setNoPathTLV(noPathTLV);
			response.setNoPath(noPath2);
			m_resp.addResponse(response);
			return m_resp;
		}
		log.info("Found "+ gps.size()+" paths");
		boolean pathfound=false;
		int k=0;
		int best_delay = 0;

		GraphPath<Inet4Address, InterDomainEdge> gp=null;

		Path path=new Path();


		ExplicitRouteObject final_srcero= new ExplicitRouteObject();
		ExplicitRouteObject final_dstero= new ExplicitRouteObject();
		ExplicitRouteObject final_ero= new ExplicitRouteObject();

		Inet4Address dstdom_srcaddr = null	;
		Inet4Address srcdom_dstaddr = null	;


		while( k <= gps.size()-1) {

			int current_delay = 0;

			path = new Path();
			reqList = new LinkedList<PCEPRequest>();
			initList = new LinkedList<PCEPInitiate>();

			gp = gps.get( k );
			k++;
			//log.info( "Path " + k );
			List<InterDomainEdge> edge_list = gp.getEdgeList();
			long tiempo2 = System.nanoTime();


			Inet4Address destIP = null;

			// End points

			EndPoints endpointsRequest = null;
			if (EP.getOT() == ObjectParameters.PCEP_OBJECT_TYPE_ENDPOINTS_IPV4) {
				endpointsRequest = new EndPointsIPv4();
				((EndPointsIPv4) endpointsRequest).setSourceIP( source_router_id_addr );
				destIP = (Inet4Address) edge_list.get( 0 ).getSrc_router_id();
				((EndPointsIPv4) endpointsRequest).setDestIP( destIP );

			} else if (EP.getOT() == ObjectParameters.PCEP_OBJECT_TYPE_ENDPOINTS_IPV6) {
				//NO IMPLEMENTADO
			}

			if (EP.getOT() == ObjectParameters.PCEP_OBJECT_TYPE_GENERALIZED_ENDPOINTS) {
				GeneralizedEndPoints gep = (GeneralizedEndPoints) req.getEndPoints();
				if (gep.getGeneralizedEndPointsType() == ObjectParameters.PCEP_GENERALIZED_END_POINTS_TYPE_P2P) {
					EndPointIPv4TLV sourceIPv4TLV = new EndPointIPv4TLV();
					EndPointIPv4TLV destIPv4TLV = new EndPointIPv4TLV();
					sourceIPv4TLV.setIPv4address( source_router_id_addr );
					destIP = (Inet4Address) edge_list.get( 0 ).getSrc_router_id();
					destIPv4TLV.setIPv4address( destIP );

					EndPoint sourceEP = new EndPoint();
					EndPoint destEP = new EndPoint();
					sourceEP.setEndPointIPv4TLV( sourceIPv4TLV );
					destEP.setEndPointIPv4TLV( destIPv4TLV );

					P2PEndpoints p2pep = new P2PEndpoints();
					p2pep.setSourceEndpoint( sourceEP );
					p2pep.setDestinationEndPoints( destEP );

					endpointsRequest = new GeneralizedEndPoints();
					((GeneralizedEndPoints) endpointsRequest).setP2PEndpoints( p2pep );

				}
				if (gep.getGeneralizedEndPointsType() == ObjectParameters.PCEP_GENERALIZED_END_POINTS_TYPE_P2MP_NEW_LEAVES) {

				}
			}

			/////////////////////////////////////////////////////////
			//SRC domain
			//////////////////////////////////////////////////////////


			Inet4Address domain = source_domain_id;
			//log.info( "First part of the LSP is in domain: " + domain + " from " + source_router_id_addr + " to " + destIP );


			boolean first_domain_equal = false;
			if (source_router_id_addr.equals( destIP )) {
				log.info( "Origin and destination are the same" );
				first_domain_equal = true;
			}

			//DELAY GRAPH for SRC domain
			SimpleDirectedWeightedGraph<Object, IntraDomainEdge> domaingraph = null;
			SimpleTEDB sTED = (SimpleTEDB) intraTEDBs.get( source_domain_id );
			//log.info( sTED.printTopology() );
			domaingraph = sTED.getDuplicatedNetworkGraph();

			Iterator<IntraDomainEdge> iter = domaingraph.edgeSet().iterator();

			while (iter.hasNext()) {
				IntraDomainEdge aa = iter.next();
				//log.info("weight before "+ domaingraph.getEdgeWeight( aa ));
				//log.info( String.valueOf( aa.TE_info.getUndirLinkDelay().getDelay() ) );

				domaingraph.setEdgeWeight( aa, aa.TE_info.getUndirLinkDelay().getDelay() );
				//log.info("weight after "+ domaingraph.getEdgeWeight( aa ));
			}

			log.info( "Computing path in src domain" );

			DijkstraShortestPath<Object, IntraDomainEdge> dsp1 = new DijkstraShortestPath<Object, IntraDomainEdge>( domaingraph, source_router_id_addr, destIP );
			GraphPath<Object, IntraDomainEdge> gp1 = dsp1.getPath();

			if (gp1 == null) {
				log.warning( "No path between src and end-point" );
				continue;
			}

			Path path1 = new Path();
			ExplicitRouteObject erosrc = new ExplicitRouteObject();
			ExplicitRouteObject totero = new ExplicitRouteObject();
			List<IntraDomainEdge> links = gp1.getEdgeList();


			int i;
			for (i = 0; i < links.size(); i++) {
				if (links.get( i ).getSource() instanceof Inet4Address) {
					UnnumberIfIDEROSubobject eroso = new UnnumberIfIDEROSubobject();
					eroso.setRouterID( (Inet4Address) links.get( i ).getSource() );
					eroso.setLoosehop( false );
					erosrc.addEROSubobject( eroso );
					totero.addEROSubobject( eroso );
				} else if (links.get( i ).getSource() instanceof DataPathID) {
					UnnumberedDataPathIDEROSubobject eroso = new UnnumberedDataPathIDEROSubobject();
					eroso.setDataPath( (DataPathID) links.get( i ).getSource() );
					eroso.setLoosehop( false );
					erosrc.addEROSubobject( eroso );
					totero.addEROSubobject( eroso );
				} else {
					log.info( "Edge instance error" );
				}
			}

			// Add last hop in the ERO Object
			if (links.size() > 0) {
				if (links.get( links.size() - 1 ).getTarget() instanceof Inet4Address) {
					UnnumberIfIDEROSubobject eroso = new UnnumberIfIDEROSubobject();
					eroso.setRouterID( (Inet4Address) links.get( links.size() - 1 ).getTarget() );
					eroso.setLoosehop( false );
					erosrc.addEROSubobject( eroso );
					totero.addEROSubobject( eroso );
				} else if (links.get( links.size() - 1 ).getTarget() instanceof DataPathID) {
					UnnumberedDataPathIDEROSubobject eroso = new UnnumberedDataPathIDEROSubobject();
					eroso.setDataPath( (DataPathID) links.get( links.size() - 1 ).getTarget() );
					eroso.setLoosehop( false );
					erosrc.addEROSubobject( eroso );
					totero.addEROSubobject( eroso );
				}
			}
			else {
				log.info( "no links on the path" );
				UnnumberIfIDEROSubobject eroso = new UnnumberIfIDEROSubobject();
				eroso.setRouterID( (Inet4Address) source_router_id_addr );
				eroso.setLoosehop( false );
				erosrc.addEROSubobject( eroso );
				totero.addEROSubobject( eroso );
			}
			log.info("Path "+ k +" src ero: " +erosrc.toString());
			if (req.getMetricList().size()!=0){
				Metric metric=new Metric();
				metric.setMetricType(req.getMetricList().get(0).getMetricType() );
				log.fine("Number of hops "+links.size());
				float metricValue=(float)links.size();
				metric.setMetricValue(metricValue);
				path1.getMetricList().add(metric);
			}

			current_delay = (int) gp1.getWeight();


			log.info("Path "+k+ ": delay src domain " +gp1.getWeight());

			//DELAY interlink
			current_delay += edge_list.get( 0 ).getTE_info().getUndirLinkDelay().getDelay();

			// check if the delay is greater or not respect current best path

			if ((best_delay != 0) && (current_delay>best_delay)){
				log.info("Delay of path "+k+" is greater than best delay. Go to next interdomain path!");
				continue;
			}

			/////////////////////////////////////////////////////////
			//DST domain
			//////////////////////////////////////////////////////////
			Inet4Address srcIP = null;
			srcIP = (Inet4Address) edge_list.get( edge_list.size()-1 ).getDst_router_id();
			// End points

			domain = dest_domain_id;

			log.info( "Second part of the LSP is in domain: " + domain + " from " + srcIP + " to " + dest_router_id_addr );

			//DELAY GRAPH for DST domain
			SimpleDirectedWeightedGraph<Object,IntraDomainEdge> domaingraphdest = null;
			SimpleTEDB dTED = (SimpleTEDB) intraTEDBs.get(dest_domain_id);
			domaingraphdest = dTED.getDuplicatedNetworkGraph() ;

			Iterator<IntraDomainEdge> iterdest = domaingraphdest.edgeSet().iterator();

			while (iterdest.hasNext()){
				IntraDomainEdge bb = iterdest.next();
				//log.info("weight before "+ domaingraphdest.getEdgeWeight( bb ));
				//log.info( String.valueOf( bb.TE_info.getUndirLinkDelay().getDelay() ) );
				domaingraphdest.setEdgeWeight(bb, bb.TE_info.getUndirLinkDelay().getDelay());
				//log.info("weight after "+ domaingraphdest.getEdgeWeight( bb ));
			}

			log.info("Computing path in dest domain");


			DijkstraShortestPath<Object,IntraDomainEdge> dsp2=new DijkstraShortestPath<Object,IntraDomainEdge> (domaingraphdest, srcIP, dest_router_id_addr);
			GraphPath<Object,IntraDomainEdge> gp2=dsp2.getPath();

			if (gp2==null){
				log.warning("No path between end-point and dest");
				continue;
			}
			pathfound = true;
			//log.info("Path "+k+ ": delay dst domain " +gp2.getWeight());


			ExplicitRouteObject erodest= new ExplicitRouteObject();
			List<IntraDomainEdge> links2=gp2.getEdgeList();


			int j;
			for (j=0;j<links2.size();j++){
				if (links2.get(j).getSource() instanceof Inet4Address){
					UnnumberIfIDEROSubobject eroso = new UnnumberIfIDEROSubobject();
					eroso.setRouterID((Inet4Address)links2.get(j).getSource());
					eroso.setLoosehop(false);
					erodest.addEROSubobject(eroso);
					totero.addEROSubobject(eroso);
				}else if (links2.get(j).getSource() instanceof DataPathID){
					UnnumberedDataPathIDEROSubobject eroso = new UnnumberedDataPathIDEROSubobject();
					eroso.setDataPath((DataPathID)links2.get(j).getSource());
					eroso.setLoosehop(false);
					erodest.addEROSubobject(eroso);
					totero.addEROSubobject(eroso);
				}else{
					log.info("Edge instance error");
				}
			}
			if (links2.size()>0){
				// Add last hop in the ERO Object
				if (links2.get(links2.size()-1).getTarget() instanceof Inet4Address){
					UnnumberIfIDEROSubobject eroso = new UnnumberIfIDEROSubobject();
					eroso.setRouterID((Inet4Address)links2.get(links2.size()-1).getTarget());
					eroso.setLoosehop(false);
					erodest.addEROSubobject(eroso);
					totero.addEROSubobject(eroso);
				}else if (links2.get(links2.size()-1).getTarget() instanceof DataPathID){
					UnnumberedDataPathIDEROSubobject eroso = new UnnumberedDataPathIDEROSubobject();
					eroso.setDataPath((DataPathID)links2.get(links2.size()-1).getTarget());
					eroso.setLoosehop(false);
					erodest.addEROSubobject(eroso);
					totero.addEROSubobject(eroso);
				}
			}
			else {
				log.info( "no links on the path" );
				UnnumberIfIDEROSubobject eroso = new UnnumberIfIDEROSubobject();
				eroso.setRouterID( (Inet4Address) dest_router_id_addr );
				eroso.setLoosehop( false );
				erodest.addEROSubobject( eroso );
				totero.addEROSubobject( eroso );
			}



			log.info("Path "+k+" dst ero: " +erodest.toString());


			if (req.getMetricList().size()!=0){
				Metric metric=new Metric();
				metric.setMetricType(req.getMetricList().get(0).getMetricType() );
				log.fine("Number of hops "+links2.size());
				float metricValue=(float)links2.size();
				metric.setMetricValue(metricValue);
				path.getMetricList().add(metric);
			}
			//response.addPath(path1);
			current_delay += (int) gp2.getWeight();
			if ((best_delay == 0) || (best_delay > current_delay)){
				//
				srcdom_dstaddr= destIP;
				//
				dstdom_srcaddr=srcIP;
				best_delay = current_delay;
				final_ero = totero;
				final_dstero =erodest;
				final_srcero = erosrc;
				log.info("Path "+k+": delay "+ current_delay);


			}
		}
		log.info("Final delay "+ best_delay);

		if (pathfound==false){
			log.warning("No paths between src and dest");
			NoPath noPath= new NoPath();
			response.setNoPath(noPath);
			m_resp.addResponse(response);
		}
		else {
			//SEND INIT messages
			log.info("Sending inits");
			long tiempofin =System.nanoTime();
			long tiempotot=tiempofin-tiempoini;
			log.info("Algo time "+tiempotot+" nsec");

			PCEPInitiate p_init_Src = new PCEPInitiate();
			PCEPIntiatedLSP lsp_inis = new PCEPIntiatedLSP();
			p_init_Src.getPcepIntiatedLSPList().add(lsp_inis);
			EndPoints eps;

			// Creation of PCEPInit message for src domain

			eps=new EndPointsIPv4();
			((EndPointsIPv4) eps).setSourceIP(source_router_id_addr);
			((EndPointsIPv4) eps).setDestIP(srcdom_dstaddr);
			lsp_inis.setEndPoint(eps);
			SRP srps= new SRP();
			srps.setRFlag(false);
			lsp_inis.setRsp(srps);
			LSP lsps = new LSP();
			lsp_inis.setLsp(lsps);
			srps.setSRP_ID_number(1);
			lsp_inis.setEro(final_srcero);
			initList.add( p_init_Src );
			domainList.add( source_domain_id );

			// Creation of PCEPInit message for dst domain
			PCEPInitiate p_init_Dst = new PCEPInitiate();
			PCEPIntiatedLSP lsp_inid = new PCEPIntiatedLSP();
			p_init_Dst.getPcepIntiatedLSPList().add(lsp_inid);
			EndPoints epd;

			epd=new EndPointsIPv4();
			((EndPointsIPv4) epd).setSourceIP(dstdom_srcaddr);
			((EndPointsIPv4) epd).setDestIP(dest_router_id_addr);
			lsp_inid.setEndPoint(epd);
			SRP srpd= new SRP();
			srpd.setRFlag(false);
			lsp_inid.setRsp(srpd);
			LSP lspd = new LSP();
			lsp_inid.setLsp(lspd);
			srpd.setSRP_ID_number(1);
			lsp_inid.setEro(final_dstero);
			initList.add( p_init_Dst );
			domainList.add( dest_domain_id );

			boolean childrenFailed = false;
			StateReport inireply2 = null;
			for (int lkk=0;lkk<initList.size();++lkk) {
				try {
					//Andrea
					//at this time just one init (the other domain is emulated)
					//TODO convert in executeInitiates
					if (domainList.get(lkk)!=this.reachabilityManager.getDomain((Inet4Address) InetAddress.getByName( "10.4.1.1" ))) {
						//respList = childPCERequestManager.executeInitiates( initList, domainList );
						inireply2 = childPCERequestManager.newIni( initList.get( lkk ), domainList.get( lkk ) );
					}
					else {
						//log.info("TID domain");
						inireply2 = new StateReport();
					}
				} catch (Exception e) {
					log.severe( "PROBLEM SENDING THE INIT" );
					NoPath noPathx = new NoPath();
					noPathx.setNatureOfIssue( ObjectParameters.NOPATH_NOPATH_SAT_CONSTRAINTS );
					NoPathTLV snoPathTLV = new NoPathTLV();
					noPathx.setNoPathTLV( snoPathTLV );
					response.setNoPath( noPathx );
					m_resp.addResponse( response );
					return m_resp;
				}


				if (inireply2 == null) {
					childrenFailed = true;
					log.warning("Child for init "+(lkk+1)+" has failed");
				}
				else {
					log.info("Domain "+ domainList.get(lkk)+" replied to Initiate: "+inireply2.toString());
					//if state ok
					//check Report ok
				}

			}

			if (childrenFailed==true){
				log.warning("Some child has failed");
				NoPath noPath= new NoPath();
				response.setNoPath(noPath);
				m_resp.addResponse(response);
			}
			else{
				path.setEro(final_ero);
				response.addPath(path);
				m_resp.addResponse(response);

			}


		}
		Monitoring monitoring=pathReq.getMonitoring();
		/*if (monitoring!=null){
			if (monitoring.isProcessingTimeBit()){
				
			}
		}*/
		//bw.close();
		//fstream.close();
		return m_resp;
	}



	@Override
	public AlgorithmReservation getReserv() {
		// TODO Auto-generated method stub
		return null;
	}
	
	
	public void addXRO(ExcludeRouteObject xro,Request req){
		req.setXro(xro);
	}
	
	
	public void processXRO(ExcludeRouteObject xro,DirectedWeightedMultigraph<Inet4Address,InterDomainEdge> networkGraph){
		if (xro!=null){
			for (int i=0;i<xro.getXROSubobjectList().size();++i){
				XROSubobject eroso=xro.getXROSubobjectList().get(i);
				if (eroso.getType()==XROSubObjectValues.XRO_SUBOBJECT_UNNUMBERED_IF_ID){
					UnnumberIfIDXROSubobject eros=(UnnumberIfIDXROSubobject)eroso;
					boolean hasVertex=networkGraph.containsVertex(eros.getRouterID());
					if (hasVertex){
						Set<InterDomainEdge> setEdges=networkGraph.edgesOf(eros.getRouterID());
						Iterator<InterDomainEdge> iter=setEdges.iterator();
						while (iter.hasNext()){
							InterDomainEdge edge=iter.next();
							if (edge.getSrc_if_id()==eros.getInterfaceID()){
								networkGraph.removeEdge(edge);																
								//InterDomainEdge edge2=networkGraph.getEdge(edge.getDst_router_id(), edge.getSrc_router_id());
							}
						}
						
					}
				}
			}
		}
		
	}
}


