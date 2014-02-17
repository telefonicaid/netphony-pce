package tid.pce.computingEngine.algorithms;

import java.util.ArrayList;
import java.util.LinkedList;

import org.jgrapht.graph.DirectedMultigraph;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;

import tid.ospf.ospfv2.lsa.tlv.subtlv.complexFields.BitmapLabelSet;
import tid.pce.tedb.IntraDomainEdge;
import tid.pce.tedb.TEDB;
import tid.pce.tedb.TE_Information;

public interface ComputingAlgorithmPreComputation {

	public void initialize();
	
	public void setTEDB(TEDB ted);
	
	public void notifyWavelengthReservation(LinkedList<Object> sourceVertexList, LinkedList<Object> targetVertexList, int wavelength);
	public void notifyWavelengthEndReservation(LinkedList<Object> sourceVertexList, LinkedList<Object> targetVertexList, int wavelength);
	
	public void notifyWavelengthStatusChange(Object source, Object destination, BitmapLabelSet previousBitmapLabelSet, BitmapLabelSet newBitmapLabelSet);

	public void notifyNewVertex(Object vertex);

	public void notifyNewEdge(Object source, Object destination);
	
	public void notifyTEDBFullUpdate();

	public void notifyNewEdgeIP(Object source, Object destination, TE_Information informationTEDB);

	public void notificationEdgeIP_AuxGraph(Object src, Object dst, TE_Information informationTEDB);

	public void notificationEdgeOPTICAL_AuxGraph(Object src, Object dst, int lambda);

	public void setGrooming_policie(int groomingPolicie);
	
	public ArrayList<SimpleDirectedWeightedGraph<Object, IntraDomainEdge>> getNetworkGraphs();
	
	public boolean isMultifiber();
	public void setMultifiber(boolean multifiber);
	
	public void setNetworkMultiGraphs(
			ArrayList<DirectedMultigraph<Object, IntraDomainEdge>> networkMultiGraphs);

	public ArrayList<DirectedMultigraph<Object, IntraDomainEdge>> getNetworkMultiGraphs();
	public String printBaseTopology();
	public String printTopology( int lambda);
}
