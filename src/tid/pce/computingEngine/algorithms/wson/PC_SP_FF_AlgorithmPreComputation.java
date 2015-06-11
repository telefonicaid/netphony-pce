package tid.pce.computingEngine.algorithms.wson;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.jgrapht.Graph;
import org.jgrapht.alg.BellmanFordShortestPath;
import org.jgrapht.graph.DirectedMultigraph;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;

import es.tid.ospf.ospfv2.lsa.tlv.subtlv.complexFields.BitmapLabelSet;
import es.tid.tedb.IntraDomainEdge;
import es.tid.tedb.TEDB;
import es.tid.tedb.TE_Information;
import tid.pce.computingEngine.algorithms.ComputingAlgorithmPreComputation;

public class PC_SP_FF_AlgorithmPreComputation implements ComputingAlgorithmPreComputation{
	
	private SimpleDirectedWeightedGraph<Object, IntraDomainEdge> networkGraph;
	
	private Hashtable<Object,Hashtable<Object,List<IntraDomainEdge>>> routeList;
	
	public void initialize(){
		
		Set<Object> edgeList= networkGraph.vertexSet();
		routeList=new Hashtable<Object,Hashtable<Object,List<IntraDomainEdge>>> (edgeList.size());
		Iterator<Object> iter=edgeList.iterator();
		while (iter.hasNext()){
			Object addr=iter.next();
			BellmanFordShortestPath<Object,IntraDomainEdge> bfsp=new BellmanFordShortestPath<Object,IntraDomainEdge>((Graph<Object,IntraDomainEdge>)networkGraph, addr);
			Hashtable<Object,List<IntraDomainEdge>> routeInterList= new Hashtable<Object,List<IntraDomainEdge>>();
			//routeList.add(addr,routeInterList);
			Iterator<Object> iterDest=edgeList.iterator();
			while (iterDest.hasNext()){
				Object dest= iterDest.next();
				List<IntraDomainEdge> route=bfsp.getPathEdgeList(dest);
				//routeInterList.add(route);
			}
		}
	}

	@Override
	public void setTEDB(TEDB ted) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void notifyWavelengthReservation(
			LinkedList<Object> sourceVertexList,
			LinkedList<Object> targetVertexList, int wavelength) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void notifyWavelengthEndReservation(
			LinkedList<Object> sourceVertexList,
			LinkedList<Object> targetVertexList, int wavelength) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void notifyWavelengthStatusChange(Object source,
			Object destination, BitmapLabelSet previousBitmapLabelSet,
			BitmapLabelSet newBitmapLabelSet) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void notifyNewVertex(Object vertex) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void notifyNewEdge(Object source, Object destination) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void notifyTEDBFullUpdate() {
		// TODO Auto-generated method stub
		
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
