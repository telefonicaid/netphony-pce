package tid.pce.computingEngine.algorithms.wson.svec;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.logging.Logger;

import org.jgrapht.graph.DirectedMultigraph;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;

import tid.ospf.ospfv2.lsa.tlv.subtlv.complexFields.BitmapLabelSet;
import tid.pce.computingEngine.algorithms.ComputingAlgorithmPreComputation;
import tid.pce.tedb.DomainTEDB;
import tid.pce.tedb.IntraDomainEdge;
import tid.pce.tedb.TEDB;
import tid.pce.tedb.TE_Information;
import tid.pce.tedb.WSONInformation;

public class SVEC_SP_FF_WSON_PathComputingPreComputation  implements ComputingAlgorithmPreComputation{

	
	private WSONInformation WSONInfo;

	int numLambdas;
	
	private Logger log;
	
	public SVEC_SP_FF_WSON_PathComputingPreComputation(){
		log=Logger.getLogger("PCEServer");
	}

	public void initialize(){
		log.info("initializing SVEC_SP_FF_WSON Algorithm");
	
	}

	@Override
	public void setTEDB(TEDB ted) {
	

		WSONInfo=((DomainTEDB)ted).getWSONinfo();
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



	@Override
	public void notifyWavelengthStatusChange(Object source,
			Object destination, BitmapLabelSet previousBitmapLabelSet,
			BitmapLabelSet newBitmapLabelSet) {

	}

	/**
	 * This function is called when a new Vertex is added
	 */
	public void notifyNewVertex(Object vertex) {
		SimpleDirectedWeightedGraph<Object,IntraDomainEdge> graph_lambda;
	

	}

	/**
	 * 
	 */
	public void notifyNewEdge(Object source, Object destination) {
		SimpleDirectedWeightedGraph<Object,IntraDomainEdge> graph_lambda;
	
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
	public void notifyNewEdgeIP(Object source, Object destination,
			TE_Information informationTEDB) {
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
