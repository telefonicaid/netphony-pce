package tid.pce.computingEngine.algorithms;

import java.util.ArrayList;
import java.util.LinkedList;

import org.jgrapht.graph.DirectedMultigraph;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;

import es.tid.ospf.ospfv2.lsa.tlv.subtlv.complexFields.BitmapLabelSet;
import tid.pce.tedb.IntraDomainEdge;
import tid.pce.tedb.SSONListener;
import tid.pce.tedb.TEDB;
import tid.pce.tedb.TEDListener;

public interface ComputingAlgorithmPreComputationSSON extends SSONListener{

	public void initialize();
	
	public void setTEDB(TEDB ted);
	
	public void notifyNewEdge_multiLink(Object source, Object destination, long srcIfId, long dstIfId);	




	
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
