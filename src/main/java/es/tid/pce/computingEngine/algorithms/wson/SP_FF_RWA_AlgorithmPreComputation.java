package es.tid.pce.computingEngine.algorithms.wson;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.logging.Logger;

import org.jgrapht.graph.DirectedMultigraph;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;

import es.tid.ospf.ospfv2.lsa.tlv.subtlv.complexFields.BitmapLabelSet;
import es.tid.pce.computingEngine.algorithms.ComputingAlgorithmPreComputation;
import es.tid.pce.computingEngine.algorithms.wson.wa.FirstFitBBDD;
import es.tid.tedb.IntraDomainEdge;
import es.tid.tedb.TEDB;
import es.tid.tedb.TE_Information;

public class SP_FF_RWA_AlgorithmPreComputation implements ComputingAlgorithmPreComputation{

	private FirstFitBBDD firstFitBBDD;
	
    private String host = "autoslocos";

    // Servidor (puerto): el puerto por defecto es el 3306
    private String port = "3306";

    // Usuario
    private String user = "root";

    // Contrase�a
    private String passwd = "josy1";

    // Base de datos a utilizar
    private String db = "strongest_topo";

    private Logger log;
	
	@Override
	public void initialize() {
		log=Logger.getLogger("PCEServer");
		log.info("INICIALIZANDO SP_FF_RWA_AlgorithmPreComputation ");
		firstFitBBDD=new FirstFitBBDD(host,port,user,passwd,db);
		
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

	public FirstFitBBDD getFirstFitBBDD() {
		return firstFitBBDD;
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
