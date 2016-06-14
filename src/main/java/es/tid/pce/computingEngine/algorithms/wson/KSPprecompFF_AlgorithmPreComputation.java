package es.tid.pce.computingEngine.algorithms.wson;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.jgrapht.GraphPath;
import org.jgrapht.alg.KShortestPaths;
import org.jgrapht.graph.DirectedMultigraph;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;

import es.tid.ospf.ospfv2.lsa.tlv.subtlv.complexFields.BitmapLabelSet;
import es.tid.pce.computingEngine.algorithms.ComputingAlgorithmPreComputation;
import es.tid.tedb.DomainTEDB;
import es.tid.tedb.IntraDomainEdge;
import es.tid.tedb.SimpleTEDB;
import es.tid.tedb.TEDB;
import es.tid.tedb.TE_Information;
import es.tid.tedb.WSONInformation;

public class KSPprecompFF_AlgorithmPreComputation implements ComputingAlgorithmPreComputation{

	//private SimpleDirectedWeightedGraph<Object, IntraDomainEdge> baseSimplegraph;
	
	private int numLambdas;
	
	private Logger log;
	
	private Lock tableLock;
	
	private Lock graphLock;
	
	private DomainTEDB ted;
	
	private WSONInformation WSONInfo;
	
	//graph of the network
	private SimpleDirectedWeightedGraph<Object, IntraDomainEdge> networkGraph;
	
	public Hashtable<Object,Hashtable<Object,List<GraphPath<Object,IntraDomainEdge>>>> routeTable;
	
	public KSPprecompFF_AlgorithmPreComputation(){
		log=LoggerFactory.getLogger("PCEServer");
	}
	
	// initialize pre-computation with K-ShortestPath (Dijkstra)
	public void initialize(){
		
		//boolean same_node = true; //case nodes are equal
		log.info("Initializing KSPprecomp Algorithm");
		int k = 1;
		tableLock=new ReentrantLock();
		
		//graph
		Set<Object> nodes= networkGraph.vertexSet();
				
		Iterator<Object> iterSrc = nodes.iterator(); // iterator for the node source
				
		// table for preset routes
		routeTable=new Hashtable<Object,Hashtable<Object,List<GraphPath<Object,IntraDomainEdge>>>> (nodes.size());
				
		while (iterSrc.hasNext()){
			
			Object src=iterSrc.next();
						
			Hashtable<Object,List<GraphPath<Object,IntraDomainEdge>>> routeInterList= new Hashtable<Object,List<GraphPath<Object,IntraDomainEdge>>>();
			KShortestPaths<Object,IntraDomainEdge> ksp = new KShortestPaths<Object,IntraDomainEdge> (networkGraph, src, k);					
			Iterator<Object> iterDst = nodes.iterator(); // iterator for the node destination
			
			while (iterDst.hasNext()){
				
				Object dst= iterDst.next();
				
				// list of the paths between "source" and "destination"
				if (!(dst.equals(src))){
					List<GraphPath<Object,IntraDomainEdge>> routeList = ksp.getPaths(dst); //list of the path edges
					
					routeInterList.put(dst, routeList);					
				}
			}
			
			routeTable.put(src,routeInterList);
		}
	}
			
	public void setTEDB(TEDB ted) {
		try {
			networkGraph=((SimpleTEDB)ted).getNetworkGraph();	
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
