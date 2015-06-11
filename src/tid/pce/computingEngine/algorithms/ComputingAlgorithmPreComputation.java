package tid.pce.computingEngine.algorithms;

import java.util.ArrayList;
import java.util.LinkedList;

import org.jgrapht.graph.DirectedMultigraph;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;

import es.tid.ospf.ospfv2.lsa.tlv.subtlv.complexFields.BitmapLabelSet;
import tid.pce.tedb.IntraDomainEdge;
import tid.pce.tedb.TEDB;
import tid.pce.tedb.TEDListener;
import tid.pce.tedb.TE_Information;

public interface ComputingAlgorithmPreComputation extends TEDListener{

	public void initialize();
	
	public void setTEDB(TEDB ted);

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
