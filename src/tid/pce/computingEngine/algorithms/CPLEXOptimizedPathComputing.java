package tid.pce.computingEngine.algorithms;
public class CPLEXOptimizedPathComputing{}


//FIXME: Class not working, uncomment to fix it
//package tid.pce.computingEngine.algorithms;
//
//import java.net.Inet4Address;
//import java.util.Hashtable;
//import java.util.Iterator;
//import java.util.logging.Logger;
//
//import org.jgrapht.graph.SimpleDirectedWeightedGraph;
//
//import tid.pce.computingEngine.ComputingRequest;
//import tid.pce.computingEngine.ComputingResponse;
//import tid.pce.pcep.constructs.Request;
//import tid.pce.pcep.constructs.Response;
//import tid.pce.pcep.objects.EndPointsIPv4;
//import tid.pce.pcep.objects.NoPath;
//import tid.pce.pcep.objects.ObjectParameters;
//import tid.pce.pcep.objects.RequestParameters;
//import tid.pce.pcep.objects.tlvs.NoPathTLV;
//import tid.pce.tedb.IntraDomainEdge;
//import tid.pce.tedb.SimpleTEDB;
//import tid.pce.tedb.TEDB;
//
//public class CPLEXOptimizedPathComputing implements  ComputingAlgorithm{
//
//	private SimpleDirectedWeightedGraph<O,IntraDomainEdge> networkGraph;
//	private Logger log=Logger.getLogger("PCEServer");
//	private ComputingRequest pathReq;
//	
//	public CPLEXOptimizedPathComputing(ComputingRequest pathReq,TEDB ted ){
//		this.networkGraph=((SimpleTEDB)ted).getDuplicatedNetworkGraph();
//		this.pathReq=pathReq;
//	}
//	
//	
//	
//	public ComputingResponse call(){
//		ComputingResponse m_resp=new ComputingResponse();
//			
//		log.info("CPLEX OPTIMIZED Processing SVEC Path Computing Request: "+pathReq.getSvec().toString());
//		Iterator<IntraDomainEdge> iteIDE=networkGraph.edgeSet().iterator();
//		Hashtable<IntraDomainEdge,Integer> htIDE=new Hashtable<IntraDomainEdge,Integer>();
//		Hashtable<Integer,IntraDomainEdge> htIDE2=new Hashtable<Integer,IntraDomainEdge>();
//		int kk=0;
//		while (iteIDE.hasNext()){
//			IntraDomainEdge ie=iteIDE.next();
//			System.out.println("El link "+ie.getSource()+" a "+ie.getTarget()+" es el "+kk);
//			htIDE.put(ie,new Integer(kk));
//			htIDE2.put(new Integer(kk), ie);
//			kk=kk+1;
//		}
//		
//		try {
//			IloCplex model = new IloCplex();
//			int numEdges=networkGraph.edgeSet().size();
//			log.info("tama�o de edgeset es "+networkGraph.edgeSet().size());
//			IloNumVar[] flowVars;
//			int numRequests=pathReq.getSvec().getSvec().getRequestIDlist().size();
//			int numVars=numRequests*numEdges;
//			System.out.println("Hay "+numVars+" vars");
//			flowVars=model.boolVarArray(numVars);
//			log.info("Hay "+numRequests+" requests");
//			for (int i=0;i<numRequests;++i){
//				Request req=pathReq.getRequestList().get(i);
//				long reqId=req.getRequestParameters().getRequestID();
//				RequestParameters rp = new RequestParameters();
//				rp.setRequestID(reqId);
//				Response response=new Response();
//				response.setRequestParameters(rp);
//				
//				EndPointsIPv4  ep=(EndPointsIPv4) req.getEndPoints();
//				Inet4Address source_router_id_addr=ep.getSourceIP();
//				System.out.println("Source: "+source_router_id_addr);
//				Inet4Address dest_router_id_addr=ep.getDestIP();
//				System.out.println("Destination: "+dest_router_id_addr);
//				//flowVars[i]=model.boolVarArray(networkGraph.edgeSet().size());
//				Iterator<Inet4Address> ite=networkGraph.vertexSet().iterator();
//				while (ite.hasNext()){
//					Inet4Address add=ite.next();
//					int num_edges_inc=networkGraph.incomingEdgesOf(add).size() ;
//					int num_edges_out=networkGraph.outgoingEdgesOf(add).size() ;
//					IloNumVar[] xx1=new IloNumVar[num_edges_inc+num_edges_out];
//					Iterator<IntraDomainEdge> iteIDE2 =networkGraph.outgoingEdgesOf(add).iterator();
//					int jj=0;
//					while (iteIDE2.hasNext()){
//						int linkindex=htIDE.get(iteIDE2.next()).intValue();
//						System.out.println("El "+linkindex+" sale, y es la "+jj);
//						xx1[jj]=flowVars[i*numEdges+linkindex];
//						jj=jj+1;
//					}
//					iteIDE2 =networkGraph.incomingEdgesOf(add).iterator();
//					while (iteIDE2.hasNext()){
//						int pp=htIDE.get(iteIDE2.next()).intValue();
//						System.out.println("El "+pp+" entray es la "+jj);
//						xx1[jj]=flowVars[i*numEdges+pp];
//						jj=jj+1;
//					}
//					double[] vals1=new double[num_edges_inc+num_edges_out];
//					for (int j=0;j<num_edges_out;++j){
//						vals1[j]=1;
//					}
//					for (int j=0;j<num_edges_inc;++j){
//						vals1[j+num_edges_out]=-1;
//					}
//					
//					if (add.equals(source_router_id_addr)){
//						System.out.println(add+ " es el origen");
//						model.addEq(model.scalProd(xx1,vals1),1);
//					}
//					else if (add.equals(dest_router_id_addr)){
//						System.out.println(add+ " es el destino");
//						model.addEq(model.scalProd(xx1,vals1),-1);
//					}
//				 else {
//					 System.out.println(add+ " es intermedio");
//					 model.addEq(model.scalProd(xx1,vals1),0);
//				 }
//				}
//				
//				
//			
//
//			}
//			int vals3[]=new int[numEdges*numRequests];
//			for (int j=0;j<vals3.length;++j){
//				vals3[j]=1;
//			}
//			model.addMinimize(model.scalProd(flowVars,vals3));
//			
//			 if ( model.solve() ) {
//		            double xx[]     = model.getValues(flowVars);
//		            for (int hhh=0;hhh<xx.length;++hhh){
//		            	log.info("xx["+hhh+"] vale "+xx[hhh]);   	
//		            }
//		         
//			 }
//			for (int i=0;i<pathReq.getSvec().getSvec().getRequestIDlist().size();++i){
//				Request req=pathReq.getRequestList().get(i);
//				long reqId=req.getRequestParameters().getRequestID();
//				RequestParameters rp = new RequestParameters();
//				rp.setRequestID(reqId);
//				Response response=new Response();
//				response.setRequestParameters(rp);
//			NoPath noPath= new NoPath();
//			noPath.setNatureOfIssue(ObjectParameters.NOPATH_NOPATH_SAT_CONSTRAINTS);
//			NoPathTLV noPathTLV=new NoPathTLV();
//			noPath.setNoPathTLV(noPathTLV);				
//			response.setNoPath(noPath);
//			m_resp.addResponse(response);
//			}
//			
//		} catch (IloException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} 
//		
//		
////		if (pathReq.getSvec().islDiverseBit()|pathReq.getSvec().isnDiverseBit()| pathReq.getSvec().issRLGDiverseBit()){
////			log.info("Diverse Path Computation");
////			for (int i=0;i<pathReq.getSvec().getRequestIDlist().size();++i){
////				Request req=pathReq.getRequestList().get(i);
////				long reqId=req.getRequestParameters().getRequestID();
////				RequestParameters rp = new RequestParameters();
////				rp.setRequestID(reqId);
////				Response response=new Response();
////				response.setRequestParameters(rp);
////				
////				EndPointsIPv4  ep=(EndPointsIPv4) req.getEndPoints();
////				Inet4Address source_router_id_addr=ep.getSourceIP();
////				log.info("Source: "+source_router_id_addr);
////				Inet4Address dest_router_id_addr=ep.getDestIP();
////				log.info("Destination: "+dest_router_id_addr);
////				if (!((networkGraph.containsVertex(source_router_id_addr))&&(networkGraph.containsVertex(dest_router_id_addr)))){
////					log.warning("Source or destination are NOT in the TED");	
////					NoPath noPath= new NoPath();
////					noPath.setNatureOfIssue(ObjectParameters.NOPATH_NOPATH_SAT_CONSTRAINTS);
////					NoPathTLV noPathTLV=new NoPathTLV();
////					if (!((networkGraph.containsVertex(source_router_id_addr)))){
////						log.finest("Unknown source");	
////						noPathTLV.setUnknownSource(true);	
////					}
////					if (!((networkGraph.containsVertex(dest_router_id_addr)))){
////						log.finest("Unknown destination");
////						noPathTLV.setUnknownDestination(true);	
////					}
////					
////					noPath.setNoPathTLV(noPathTLV);				
////					response.setNoPath(noPath);
////					m_resp.addResponse(response);					
////				}
////				else {
////					log.info("Computing path");
////					long tiempoini =System.currentTimeMillis();
////					DijkstraShortestPath<Inet4Address,IntraDomainEdge>  dsp=new DijkstraShortestPath<Inet4Address,IntraDomainEdge> (networkGraph, source_router_id_addr, dest_router_id_addr);
////					GraphPath<Inet4Address,IntraDomainEdge> gp=dsp.getPath();
////					long tiempofin =System.currentTimeMillis();
////					long tiempotot=tiempofin-tiempoini;
////					log.info("Ha tardado "+tiempotot+" milisegundos");
////					m_resp.addResponse(response);
////					if (gp==null){
////						log.info("NO PATH FOUND!!!!");
////						NoPath noPath= new NoPath();
////						noPath.setNatureOfIssue(ObjectParameters.NOPATH_NOPATH_SAT_CONSTRAINTS);
////						response.setNoPath(noPath);
////						
////					}
////					else {
////						log.info("PATH FOUND!!!!");
////						Path path=new Path();
////						ExplicitRouteObject ero= new ExplicitRouteObject();
////						List<IntraDomainEdge> edge_list=gp.getEdgeList();
////						int j;
////						for (j=0;j<edge_list.size();j++){
////							UnnumberIfIDEROSubobject eroso= new UnnumberIfIDEROSubobject();
////							eroso.setRouterID(edge_list.get(j).getSource());
////							eroso.setInterfaceID(edge_list.get(j).getSrc_if_id());
////							eroso.setLoosehop(false);
////							ero.addEROSubobject(eroso);
////						 }
////						IPv4prefixEROSubobject eroso= new IPv4prefixEROSubobject();
////						eroso.setIpv4address(edge_list.get(edge_list.size()-1).getTarget());
////						eroso.setPrefix(32);
////						ero.addEROSubobject(eroso);
////						path.seteRO(ero);
////						response.addPath(path);
////						if (pathReq.getSvec().islDiverseBit()){
////							log.info("Removing edges from graph!");
////							for (j=0;j<edge_list.size();j++){
////								networkGraph.removeEdge(edge_list.get(j));
////							 }
////							
////						}
////						else if (pathReq.getSvec().issRLGDiverseBit()){
////							log.info("Removing edges (NOW SRLGs are the links) from graph!");
////							for (j=0;j<edge_list.size();j++){
////								networkGraph.removeEdge(edge_list.get(j));
////							 }
////						}
////						else {
////							log.info("Removing nodes from graph!");
////							for (j=1;j<edge_list.size();j++){
////								networkGraph.removeVertex(edge_list.get(j).getSource());
////							 }
////						}
////						
////					}
////					
////					
////				}
////				
////				
////			}
////		}else {
////			log.info("Sincronized Path Computation");
////			for (int i=0;i<pathReq.getSvec().getRequestIDlist().size();++i){
////				Request req=pathReq.getRequestList().get(i);
////				long reqId=req.getRequestParameters().getRequestID();
////				RequestParameters rp = new RequestParameters();
////				rp.setRequestID(reqId);
////				Response response=new Response();
////				response.setRequestParameters(rp);
////				EndPointsIPv4  ep=(EndPointsIPv4) req.getEndPoints();
////				Inet4Address source_router_id_addr=ep.getSourceIP();
////				log.info("Source: "+source_router_id_addr);
////				Inet4Address dest_router_id_addr=ep.getDestIP();
////				log.info("Destination: "+dest_router_id_addr);
////				if (!((networkGraph.containsVertex(source_router_id_addr))&&(networkGraph.containsVertex(dest_router_id_addr)))){
////					log.warning("Source or destination are NOT in the TED");	
////					NoPath noPath= new NoPath();
////					noPath.setNatureOfIssue(ObjectParameters.NOPATH_NOPATH_SAT_CONSTRAINTS);
////					NoPathTLV noPathTLV=new NoPathTLV();
////					if (!((networkGraph.containsVertex(source_router_id_addr)))){
////						log.finest("Unknown source");	
////						noPathTLV.setUnknownSource(true);	
////					}
////					if (!((networkGraph.containsVertex(dest_router_id_addr)))){
////						log.finest("Unknown destination");
////						noPathTLV.setUnknownDestination(true);	
////					}
////					
////					noPath.setNoPathTLV(noPathTLV);				
////					response.setNoPath(noPath);
////					m_resp.addResponse(response);					
////				}
////				else {
////					log.info("Computing path");
////					long tiempoini =System.currentTimeMillis();
////					DijkstraShortestPath<Inet4Address,IntraDomainEdge>  dsp=new DijkstraShortestPath<Inet4Address,IntraDomainEdge> (networkGraph, source_router_id_addr, dest_router_id_addr);
////					GraphPath<Inet4Address,IntraDomainEdge> gp=dsp.getPath();
////					long tiempofin =System.currentTimeMillis();
////					long tiempotot=tiempofin-tiempoini;
////					log.info("Ha tardado "+tiempotot+" milisegundos");
////					m_resp.addResponse(response);
////					if (gp==null){
////						log.info("NO PATH FOUND!!!!");
////						NoPath noPath= new NoPath();
////						noPath.setNatureOfIssue(ObjectParameters.NOPATH_NOPATH_SAT_CONSTRAINTS);
////						response.setNoPath(noPath);					
////					}
////					else {
////						log.info("PATH FOUND!!!!");
////						Path path=new Path();
////						ExplicitRouteObject ero= new ExplicitRouteObject();
////						List<IntraDomainEdge> edge_list=gp.getEdgeList();
////						int j;
////						for (j=0;j<edge_list.size();j++){
////							System.out.println("edge "+j);
////							System.out.println("size es "+edge_list.size());
////							System.out.println("src es "+edge_list.get(j));
////							UnnumberIfIDEROSubobject eroso= new UnnumberIfIDEROSubobject();
////							eroso.setRouterID(edge_list.get(j).getSource());
////							eroso.setInterfaceID(edge_list.get(j).getSrc_if_id());
////							eroso.setLoosehop(false);
////							ero.addEROSubobject(eroso);
////						 }
////						IPv4prefixEROSubobject eroso= new IPv4prefixEROSubobject();
////						eroso.setIpv4address(edge_list.get(edge_list.size()-1).getTarget());
////						eroso.setPrefix(32);
////						ero.addEROSubobject(eroso);
////						path.seteRO(ero);
////						response.addPath(path);
////						
////					}			
////				}
////			}
////		}
//		return m_resp;
//	}
//
//
//
//	@Override
//	public AlgorithmReservation getReserv() {
//		// TODO Auto-generated method stub
//		return null;
//	}
//	
//	
//	
//}
