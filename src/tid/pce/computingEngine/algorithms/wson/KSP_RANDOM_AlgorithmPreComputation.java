package tid.pce.computingEngine.algorithms.wson;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
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
import tid.ospf.ospfv2.lsa.tlv.subtlv.complexFields.BitmapLabelSet;
import tid.pce.computingEngine.ComputingRequest;
import tid.pce.computingEngine.ComputingResponse;
import tid.pce.computingEngine.algorithms.ComputingAlgorithmManager;
import tid.pce.computingEngine.algorithms.ComputingAlgorithmPreComputation;

import tid.pce.server.wson.ReservationManager;
import tid.pce.tedb.DomainTEDB;
import tid.pce.tedb.IntraDomainEdge;
import tid.pce.tedb.SimpleTEDB;
import tid.pce.tedb.TEDB;
import tid.pce.tedb.TE_Information;
import tid.pce.tedb.WSONInformation;

public class KSP_RANDOM_AlgorithmPreComputation implements ComputingAlgorithmPreComputation{

	//private SimpleDirectedWeightedGraph<Object, IntraDomainEdge> baseSimplegraph;
	
	private int numLambdas;
	
	private Logger log;
	
	private Lock tableLock;
	
	private Lock graphLock;
	
	private DomainTEDB ted;
	
	private WSONInformation WSONInfo;
	
	//graph of the network
	private SimpleDirectedWeightedGraph<Object, IntraDomainEdge> networkGraph;
		
	public SimpleDirectedWeightedGraph<Object, IntraDomainEdge> getNetworkGraph() {
		return networkGraph;
	}

	public KSP_RANDOM_AlgorithmPreComputation(){
		log=Logger.getLogger("PCEServer");
	}
	
	// initialize pre-computation with K-ShortestPath (Dijkstra)
	public void initialize(){
		
		//boolean same_node = true; //case nodes are equal
		log.info("Initializing KSP-RANDOM Algorithm");
		
	}
			
	public void setTEDB(TEDB ted) {
		try {
			networkGraph=((SimpleTEDB)ted).getNetworkGraph();	
			log.info("Using SimpleTEDB");				
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
		WSONInfo=((DomainTEDB)ted).getWSONinfo();
		this.ted=(DomainTEDB)ted;
		this.numLambdas=WSONInfo.getNumLambdas();
	}
	
	@Override
	public void notifyWavelengthReservation(
			LinkedList<Object> sourceVertexList,
			LinkedList<Object> targetVertexList, int wavelength) {
	}
	

	@Override
	public void notifyWavelengthEndReservation(
			LinkedList<Object> sourceVertexList,
			LinkedList<Object> targetVertexList, int wavelength) {		
	}
	
	public Lock getGraphLock() {
		return graphLock;
	}

	public void setGraphLock(Lock graphLock) {
		this.graphLock = graphLock;
	}
	


	@Override
	public void notifyWavelengthStatusChange(Object source,
			Object destination, BitmapLabelSet previousBitmapLabelSet,
			BitmapLabelSet newBitmapLabelSet) {
	}

	/**
	 * This function is called when a new Vertex is added
	 */
	public void notifyNewVertex(Object vertex) {
	}

	/**
	 * 
	 */
	public void notifyNewEdge(Object source, Object destination) {
		

	}
	
	public WSONInformation getWSONInfo() {
		return WSONInfo;
	}

	public void setWSONInfo(WSONInformation wSONInfo) {
		WSONInfo = wSONInfo;
	}

	@Override
	public void notifyTEDBFullUpdate() {
		

	}

	@Override
	public void notificationEdgeIP_AuxGraph(Object src, Object dst,
			TE_Information informationTEDB) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void notificationEdgeOPTICAL_AuxGraph(Object src,
			Object dst, int lambda) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void notifyNewEdgeIP(Object source, Object destination,
			TE_Information informationTEDB) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setGrooming_policie(int groomingPolicie) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public ArrayList<SimpleDirectedWeightedGraph<Object, IntraDomainEdge>> getNetworkGraphs() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isMultifiber() {
		// TODO Auto-generated method stub
		return false;
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

	@Override
	public ArrayList<DirectedMultigraph<Object, IntraDomainEdge>> getNetworkMultiGraphs() {
		// TODO Auto-generated method stub
		return null;
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




}