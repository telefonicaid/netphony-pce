package tid.pce.computingEngine.algorithms.multiLayer;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.BellmanFordShortestPath;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.graph.DirectedMultigraph;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;
import org.jgrapht.alg.KShortestPaths;

import tid.ospf.ospfv2.lsa.tlv.subtlv.MaximumBandwidth;
import tid.ospf.ospfv2.lsa.tlv.subtlv.MaximumReservableBandwidth;
import tid.ospf.ospfv2.lsa.tlv.subtlv.UnreservedBandwidth;
import tid.ospf.ospfv2.lsa.tlv.subtlv.complexFields.BitmapLabelSet;
import tid.pce.computingEngine.ComputingRequest;
import tid.pce.computingEngine.algorithms.ComputingAlgorithmManager;
import tid.pce.computingEngine.algorithms.ComputingAlgorithmPreComputation;
import tid.pce.pcep.messages.PCEPResponse;
import tid.pce.server.wson.ReservationManager;
import tid.pce.tedb.DomainTEDB;
import tid.pce.tedb.InterDomainEdge;
import tid.pce.tedb.IntraDomainEdge;
import tid.pce.tedb.MultiLayerTEDB;
import tid.pce.tedb.TEDB;
import tid.pce.tedb.MDTEDB;
import tid.pce.tedb.TE_Information;
import tid.pce.tedb.WSONInformation;

public class Multilayer_MinWL_AlgorithmPreComputation implements ComputingAlgorithmPreComputation{
		
	private ArrayList<SimpleDirectedWeightedGraph<Object, IntraDomainEdge>> networkGraphs;
	
	private int numLambdas;

	private Logger log;

	private Lock graphLock;

	private DomainTEDB ted;
	
	private WSONInformation WSONInfo;

	private Integer MaxNodesOptical;
	
	public Integer getMaxNodesOptical() {
		return MaxNodesOptical;
	}
	
	public class InfoNodo{ //Nodes --> (layer, I/0, nodeIP)
		private int numLayer;
		private int I_O;   // 1 -->
		private Object IPnode;
		
		public int getnumLayer(){
			return this.numLayer;
		}
		
		public void setnumLayer(int numLayer){
			this.numLayer = numLayer;
		}
		
		public int getI_O(){
			return this.I_O;
		}
		
		public void setI_O(int I_O){
			this.I_O = I_O;
		}
		
		public Object getIPnode(){
			return this.IPnode;
		}
		
		public void setIPnode(Object IPnode){
			this.IPnode = IPnode;
		}
	}
		
	//table of relation of nodes and info in the Optical_networkGraph
	public Hashtable<Integer, InfoNodo> infoTable;
	
	//graph of the network IP
	private SimpleDirectedWeightedGraph<Object, IntraDomainEdge> NetworkGraphIP;
	
	//2 nodes for each real node and N + 1 replicas (layers)
	private SimpleDirectedWeightedGraph<Object, IntraDomainEdge> OpticalnetworkGraph;
	
	private SimpleDirectedWeightedGraph<Object, IntraDomainEdge> InterLayerGraph;
	

	public Hashtable<Object,Hashtable<Object,List<GraphPath<Object,IntraDomainEdge>>>> routeTable;

	public Multilayer_MinWL_AlgorithmPreComputation(){
		log=Logger.getLogger("PCEServer");
	}
	
	public void initialize(){
		log.info("Resgistramos el algoritmo Multilayer MinWL");
		
		numLambdas = ted.getWSONinfo().getNumLambdas(); // number of lambdas
		
		graphLock=new ReentrantLock();
		Set<Object> nodes1 = OpticalnetworkGraph.vertexSet();
		Iterator<Object> iter;
		Set<IntraDomainEdge> fiberEdges= OpticalnetworkGraph.edgeSet();
		Iterator<IntraDomainEdge> iterFiberLink;
		if (numLambdas>0){
			networkGraphs=new ArrayList<SimpleDirectedWeightedGraph<Object,IntraDomainEdge>>(numLambdas);
			SimpleDirectedWeightedGraph<Object,IntraDomainEdge> graph_lambda;
			for (int i=0;i<numLambdas;++i){
				graph_lambda=new SimpleDirectedWeightedGraph<Object,IntraDomainEdge>(IntraDomainEdge.class);
				networkGraphs.add(i, graph_lambda);
				iter = nodes1.iterator();
				while (iter.hasNext()){
					graph_lambda.addVertex( iter.next());			
				}
				iterFiberLink=fiberEdges.iterator();
				while (iterFiberLink.hasNext()){
					IntraDomainEdge fiberEdge =iterFiberLink.next();
					IntraDomainEdge edge=new IntraDomainEdge();
					edge.setDelay_ms(fiberEdge.getDelay_ms());
					graph_lambda.addEdge(fiberEdge.getSource(),fiberEdge.getTarget(),edge);			
				}
			}	
		}else {
			log.severe("REGISTERING MULTILAYER ALGORITHM WITHOUT KNOWN NUMBER OF LAMBDAS");
			System.exit(-1);
		}
	}
	
	@Override
	public void setTEDB(TEDB ted) {
		try {
			//NetworkGraphIP=((MultiLayerTEDB)ted).getDuplicatedUpperLayerkGraph();
			NetworkGraphIP=((MultiLayerTEDB)ted).getUpperLayerGraph();
			//getDuplicatedUpperLayerkGraph(); //getNetworkGraph();	
			OpticalnetworkGraph=((MultiLayerTEDB)ted).getDuplicatedLowerLayerkGraph();
			//.getDuplicatedLowerLayerkGraph(); //getNetworkGraph();	
			InterLayerGraph = ((MultiLayerTEDB)ted).getDuplicatedInterLayerGraph();
			//.getDuplicatedInterLayerGraph();
			log.info("Using MultiLayerTEDB");				
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
		WSONInfo=((DomainTEDB)ted).getWSONinfo();
		this.ted=(DomainTEDB)ted;
		this.numLambdas=WSONInfo.getNumLambdas();
	}
	
	public ArrayList<SimpleDirectedWeightedGraph<Object, IntraDomainEdge>> getNetworkGraphs() {
		return networkGraphs;
	}
	
	public SimpleDirectedWeightedGraph<Object, IntraDomainEdge> getnetworkGraphIP() {
		return NetworkGraphIP;
	}
	
	public SimpleDirectedWeightedGraph<Object, IntraDomainEdge> getOpticalnetworkGraph() {
		return OpticalnetworkGraph;
	}

	public void setOpticalnetworkGraph(
			SimpleDirectedWeightedGraph<Object, IntraDomainEdge> opticalNetworkGraph) {
		OpticalnetworkGraph = opticalNetworkGraph;
	}
	
	public void setnetworkGraphIP(
			SimpleDirectedWeightedGraph<Object, IntraDomainEdge> networkGraphIP) {
		NetworkGraphIP = networkGraphIP;
	}
	
	public SimpleDirectedWeightedGraph<Object, IntraDomainEdge> getInterLayerGraph() {
		return InterLayerGraph;
	}
	
	public Hashtable<Integer, InfoNodo> getInfoTable() {
		return infoTable;
	}
	
	public void notifyWavelengthReservation(
			LinkedList<Object> sourceVertexList,
			LinkedList<Object> targetVertexList, int wavelength) {

		graphLock.lock();
		try{
			SimpleDirectedWeightedGraph<Object, IntraDomainEdge> networkGraph=networkGraphs.get(wavelength);
			for (int i=0;i<sourceVertexList.size()-1;++i){				
				networkGraph.removeEdge(sourceVertexList.get(i), targetVertexList.get(i));
				
			}
		}finally{
			graphLock.unlock();	
		}
	}

	public void notifyWavelengthEndReservation(
			LinkedList<Object> sourceVertexList,
			LinkedList<Object> targetVertexList, int wavelength) {		
		graphLock.lock();
		try{
			SimpleDirectedWeightedGraph<Object, IntraDomainEdge> networkGraph=networkGraphs.get(wavelength);
			for (int i=0;i<sourceVertexList.size()-1;++i){
				//SOLO VOLVER A PONER COMO LIBRE SI EN OSPF NO ESTA RESERVADA
				if (OpticalnetworkGraph.getEdge(sourceVertexList.get(i), targetVertexList.get(i)).getTE_info().isWavelengthFree(wavelength)){
					//Add edge with delay
					IntraDomainEdge edge=new IntraDomainEdge();
					edge.setDelay_ms(OpticalnetworkGraph.getEdge(sourceVertexList.get(i), targetVertexList.get(i)).getDelay_ms());
					networkGraph.addEdge(sourceVertexList.get(i), targetVertexList.get(i),edge);
				}
			}
		}finally{
			graphLock.unlock();	
		}
	}
	
	public Lock getGraphLock() {
		return graphLock;
	}

	public void setGraphLock(Lock graphLock) {
		this.graphLock = graphLock;
	}

	public WSONInformation getWSONInfo() {
		return WSONInfo;
	}

	public void setWSONInfo(WSONInformation wSONInfo) {
		WSONInfo = wSONInfo;
	}

	public void notifyNewVertex(Object vertex) {
		SimpleDirectedWeightedGraph<Object,IntraDomainEdge> graph_lambda;
		for (int i=0;i<numLambdas;++i){
			log.info("Adding graph of lambda "+i);			
			graph_lambda=networkGraphs.get(i);
			graph_lambda.addVertex(vertex);						
		}		
	}

	public void notifyTEDBFullUpdate() {
		this.graphLock.lock();
		try{
			Set<Object> nodes= OpticalnetworkGraph.vertexSet();
			Iterator<Object> iter;
			Set<IntraDomainEdge> fiberEdges= OpticalnetworkGraph.edgeSet();
			Iterator<IntraDomainEdge> iterFiberLink;
			if (numLambdas>0){
				networkGraphs=new ArrayList<SimpleDirectedWeightedGraph<Object,IntraDomainEdge>>(numLambdas);
				SimpleDirectedWeightedGraph<Object,IntraDomainEdge> graph_lambda;
				for (int i=0;i<numLambdas;++i){
					log.info("Looking at graph of lambda "+i);
					graph_lambda=networkGraphs.get(i);
					iter=nodes.iterator();
					//					while (iter.hasNext()){
					//						if (graph_lambda getEdge(sourceVertex, targetVertex))
					//						graph_lambda.addVertex( iter.next());			
					//					}
					iterFiberLink=fiberEdges.iterator();
					while (iterFiberLink.hasNext()){
						IntraDomainEdge fiberEdge =iterFiberLink.next();
						if (!(graph_lambda.containsEdge(fiberEdge.getSource(), fiberEdge.getTarget()))){
							//If the edge is not there... we look at the reservation status of the wavelength
							if ((fiberEdge.getTE_info().isWavelengthFree(i))&&(fiberEdge.getTE_info().isWavelengthUnreserved(i))){
								//IntraDomainEdge edge=new IntraDomainEdge();
								graph_lambda.addEdge(fiberEdge.getSource(),fiberEdge.getTarget(),fiberEdge);
							}

						}
					}	
				}

			}
		}finally{
			this.graphLock.unlock();
		}
	}
	
	public void notifyNewEdge(Object source, Object destination) {
		SimpleDirectedWeightedGraph<Object,IntraDomainEdge> graph_lambda;
		for (int i=0;i<numLambdas;++i){
			log.info("Adding graph of lambda "+i);			
			graph_lambda=networkGraphs.get(i);
			//IntraDomainEdge edge=new IntraDomainEdge();

			//edge.setDelay_ms(OpticalnetworkGraph.getEdge(source, destination).getDelay_ms());
			graph_lambda.addEdge(source,destination,OpticalnetworkGraph.getEdge(source, destination));
		}		
	}
	
	public void notifyNewLSP(Object source, Object destination) {
		SimpleDirectedWeightedGraph<Object,IntraDomainEdge> graph_lambda;
		for (int i=0;i<numLambdas;++i){
			log.info("Adding graph of lambda "+i);			
			graph_lambda=networkGraphs.get(i);
			IntraDomainEdge edge=new IntraDomainEdge();

			edge.setDelay_ms(OpticalnetworkGraph.getEdge(source, destination).getDelay_ms());
			graph_lambda.addEdge(source,destination,edge);
		}		
	}
	
	public void notifyWavelengthStatusChange(Object source, Object destination, 
			BitmapLabelSet previousBitmapLabelSet, BitmapLabelSet newBitmapLabelSet) {

		previousBitmapLabelSet.getNumLabels();
		int num_bytes=previousBitmapLabelSet.getBytesBitMap().length;
		int wavelength_to_occupy=-1;
		int wavelength_to_free=-1;
		try{
			graphLock.lock();
			for (int i=0;i<num_bytes;++i){
				if (previousBitmapLabelSet.getBytesBitMap()[i]!=newBitmapLabelSet.getBytesBitMap()[i]){
					for (int k=0;k<8;++k){
						if ((newBitmapLabelSet.getBytesBitMap()[i]&(0x80>>>k))>(previousBitmapLabelSet.getBytesBitMap()[i]&(0x80>>>k))){
							wavelength_to_occupy=k+(i*8);
							SimpleDirectedWeightedGraph<Object, IntraDomainEdge> networkGraph=networkGraphs.get(wavelength_to_occupy);
							networkGraph.removeEdge(source, destination);		

						}else if ((newBitmapLabelSet.getBytesBitMap()[i]&(0x80>>>k))<(previousBitmapLabelSet.getBytesBitMap()[i]&(0x80>>>k))){
							if ((newBitmapLabelSet.getBytesBitmapReserved()[i]&(0x80>>>k))==0){
								wavelength_to_free=k+(i*8);	
								SimpleDirectedWeightedGraph<Object, IntraDomainEdge> networkGraph=networkGraphs.get(wavelength_to_free);
								networkGraph.addEdge(source, destination);
							}

						}
					}
				}
			}
		}finally{
			graphLock.unlock();	
		}
	}

	@Override
	public void notifyNewEdgeIP(Object source, Object destination, TE_Information informationTEDB) {
		try{
			graphLock.lock();
		
			IntraDomainEdge edge=new IntraDomainEdge();
								
			edge.setTE_info(informationTEDB);
			
			NetworkGraphIP.addEdge(source, destination, edge);
			
			/*System.out.println("VAMOS A PINTAR LA TOPOLOGÍA VIRTUAL A VER COMO ESTA LA COSA\n");
			Set<IntraDomainEdge> fiberEdges= NetworkGraphIP.edgeSet();
			Iterator<IntraDomainEdge> iterFiberLink;
			
			iterFiberLink = fiberEdges.iterator();
			while (iterFiberLink.hasNext()){
				IntraDomainEdge fiberEdge =iterFiberLink.next();
				System.out.println(fiberEdge.toString()+" BWS : "+fiberEdge.getTE_info().getMaximumBandwidth().getMaximumBandwidth()+" "+
				fiberEdge.getTE_info().getMaximumReservableBandwidth().maximumReservableBandwidth+" "+
				fiberEdge.getTE_info().getUnreservedBandwidth().getUnreservedBandwidth()[0]);
			}*/
		}finally{
			graphLock.unlock();	
		}
	}

	@Override
	public void notificationEdgeIP_AuxGraph(Object src, Object dst, TE_Information informationTEDB) {
	}

	@Override
	public void notificationEdgeOPTICAL_AuxGraph(Object src, Object dst, int lambda) {
	}

	@Override
	public void setGrooming_policie(int groomingPolicie) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public ArrayList<DirectedMultigraph<Object, IntraDomainEdge>> getNetworkMultiGraphs() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isMultifiber() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String printBaseTopology() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String printTopology(int lambda) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setMultifiber(boolean multifiber) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setNetworkMultiGraphs(
			ArrayList<DirectedMultigraph<Object, IntraDomainEdge>> networkMultiGraphs) {
		// TODO Auto-generated method stub
		
	}
}
