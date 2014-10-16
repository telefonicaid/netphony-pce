package tid.pce.computingEngine.algorithms;

import java.util.ArrayList;
import java.util.LinkedList;

import org.jgrapht.graph.DirectedMultigraph;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;

import es.tid.ospf.ospfv2.lsa.tlv.subtlv.complexFields.BitmapLabelSet;
import tid.pce.tedb.IntraDomainEdge;
import tid.pce.tedb.TEDB;

public interface ComputingAlgorithmPreComputationSSON {

	public void initialize();
	
	public void setTEDB(TEDB ted);
	
	public void notifyWavelengthReservation(LinkedList<Object> sourceVertexList, LinkedList<Object> targetVertexList, int wavelength);
	public void notifyWavelengthEndReservation(LinkedList<Object> sourceVertexList, LinkedList<Object> targetVertexList, int wavelength);
	
	public void notifyWavelengthStatusChange(Object source, Object destination, BitmapLabelSet previousBitmapLabelSet, BitmapLabelSet newBitmapLabelSet);

	public void notifyNewVertex(Object vertex);

	public void notifyNewEdge(Object source, Object destination);
	public void notifyNewEdge_multiLink(Object source, Object destination, long srcIfId, long dstIfId);
	
	public void notifyTEDBFullUpdate();
	

	public void notifyWavelengthReservationSSON(LinkedList<Object> sourceVertexList,
			LinkedList<Object> targetVertexList, int wavelength, int m);

	void notifyWavelengthEndReservationSSON(
			LinkedList<Object> sourceVertexList,
			LinkedList<Object> targetVertexList, int wavelength, int m);
	
	public ArrayList<SimpleDirectedWeightedGraph<Object, IntraDomainEdge>> getNetworkGraphs();
	
	public boolean isMultifiber();
	public void setMultifiber(boolean multifiber);

	public void setNetworkMultiGraphs(
			ArrayList<DirectedMultigraph<Object, IntraDomainEdge>> networkMultiGraphs);

	public ArrayList<DirectedMultigraph<Object, IntraDomainEdge>> getNetworkMultiGraphs();
	
	public DirectedMultigraph<Object, IntraDomainEdge> getBaseMultigraph();
	public void setBaseMultigraph(
			DirectedMultigraph<Object, IntraDomainEdge> baseMultigraph);
	
	public SimpleDirectedWeightedGraph<Object, IntraDomainEdge> getBaseSimplegraph();
	public void setBaseSimplegraph(SimpleDirectedWeightedGraph<Object, IntraDomainEdge> baseSimplegraph);
	public String printEdge(Object source, Object destination);
	public String printBaseTopology();
	public String printTopology( int lambda);

	//public WSONInformation getWSONInfo();
}
