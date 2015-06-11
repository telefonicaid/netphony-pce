package tid.pce.computingEngine.algorithms.vlan;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

import org.jgrapht.graph.DirectedMultigraph;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;

import es.tid.ospf.ospfv2.lsa.tlv.subtlv.complexFields.BitmapLabelSet;
import es.tid.tedb.DomainTEDB;
import es.tid.tedb.IntraDomainEdge;
import es.tid.tedb.SimpleTEDB;
import es.tid.tedb.TEDB;
import es.tid.tedb.TE_Information;
import tid.pce.computingEngine.algorithms.ComputingAlgorithmPreComputation;

/**
 * 
 * The main difference with AURE_SSON_algorithm is that here there is only one graph and in this graph each edge
 * has several wlans.
 * 
 * In AURE_SSON_algorithm there are several graphs and in each graph there is only one lambda.
 * 
 * @author jaume
 *
 */

public class BETTER_WLAN_algorithmPreComputation  implements ComputingAlgorithmPreComputation{

	private ArrayList<SimpleDirectedWeightedGraph<Object, IntraDomainEdge>> networkGraphs;

	private SimpleDirectedWeightedGraph<Object, IntraDomainEdge> baseSimplegraph;

	private Logger log;

	private Lock graphLock;

	private DomainTEDB ted;
	
	private boolean existsPath = true;

	public BETTER_WLAN_algorithmPreComputation(){
		log=Logger.getLogger("PCEServer");
	}

	public void initialize(){
		log.info("initializing AURE Algorithm WLAN");
		graphLock=new ReentrantLock();

		Set<Object> nodes= baseSimplegraph.vertexSet();
		
		Iterator<Object> iter;
		Set<IntraDomainEdge> fiberEdges= baseSimplegraph.edgeSet();
		Iterator<IntraDomainEdge> iterFiberLink;

		networkGraphs=new ArrayList<SimpleDirectedWeightedGraph<Object,IntraDomainEdge>>(1);
		SimpleDirectedWeightedGraph<Object,IntraDomainEdge> graph_lambda;

		log.info("Adding graph of lambda ");
		graph_lambda=new SimpleDirectedWeightedGraph<Object,IntraDomainEdge>(IntraDomainEdge.class);
		networkGraphs.add(graph_lambda);
		iter=nodes.iterator();
		while (iter.hasNext()){
			log.info("Adding vertex!");
			graph_lambda.addVertex( iter.next());			
		}
		iterFiberLink=fiberEdges.iterator();
		while (iterFiberLink.hasNext()){
			IntraDomainEdge fiberEdge =iterFiberLink.next();
			log.info("Adding edge!::"+fiberEdge);
			log.info("fiberEdge.getSource()::"+fiberEdge.getSource());
			log.info("fiberEdge.getTarget()::"+fiberEdge.getTarget());
			//IntraDomainEdge edge=new IntraDomainEdge();
			//edge.setDelay_ms(fiberEdge.getDelay_ms());
			graph_lambda.addEdge(fiberEdge.getSource(),fiberEdge.getTarget(),fiberEdge);			
		}
	}

	@Override
	public void setTEDB(TEDB ted) 
	{
		try 
		{
			baseSimplegraph=((SimpleTEDB)ted).getNetworkGraph();
			log.info("Using SimpleTEDB");				
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
			System.exit(0);
		}
		this.ted=(DomainTEDB)ted;
		Set<IntraDomainEdge> allEdges = baseSimplegraph.edgeSet();
		
		existsPath = true;
		for (IntraDomainEdge edge : allEdges)
		{
			if (edge.getTE_info().getNumberWLANs()<=0)
			{
				existsPath = false;
			}
		}
		if (!(existsPath))
		{
			log.warning("Some links are down!!");
		}
	}

	public ArrayList<SimpleDirectedWeightedGraph<Object, IntraDomainEdge>> getNetworkGraphs() {
		return networkGraphs;
	}

	@Override
	public void notifyWavelengthReservation(
			LinkedList<Object> sourceVertexList,
			LinkedList<Object> targetVertexList, int wlan) {
		graphLock.lock();
		try{
			SimpleDirectedWeightedGraph<Object, IntraDomainEdge> networkGraph=networkGraphs.get(0);
			for (int i=0;i<sourceVertexList.size();++i){	
				IntraDomainEdge edge = networkGraph.getEdge(sourceVertexList.get(i), targetVertexList.get(i));
				edge.getTE_info().setWavelengthReserved(wlan);
			}
		}finally{
				graphLock.unlock();	
		}
	}
	
	@Override
	public void notifyWavelengthEndReservation(
			LinkedList<Object> sourceVertexList,
			LinkedList<Object> targetVertexList, int wlan) {		
		graphLock.lock();
		try{
			SimpleDirectedWeightedGraph<Object, IntraDomainEdge> networkGraph=networkGraphs.get(wlan);
			for (int i=0;i<sourceVertexList.size()-1;++i){
				IntraDomainEdge edge = networkGraph.getEdge(sourceVertexList.get(i), targetVertexList.get(i));
				edge.getTE_info().setWavelengthUnReserved(wlan);
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


	@Override
	public void notifyWavelengthStatusChange(Object source,
			Object destination, BitmapLabelSet previousBitmapLabelSet,
			BitmapLabelSet newBitmapLabelSet) {

		log.warning("ERROR, Unimplemented at the moment");
	}

	/**
	 * This function is called when a new Vertex is added
	 */
	@Override
	public void notifyNewVertex(Object vertex) {
		SimpleDirectedWeightedGraph<Object,IntraDomainEdge> graph_lambda;
		log.info("Adding graph of lambda "+0);			
		graph_lambda=networkGraphs.get(0);
		graph_lambda.addVertex(vertex);								
	}

	/**
	 * 
	 */
	@Override
	public void notifyNewEdge(Object source, Object destination) {
		for (int i = 0; i < networkGraphs.size(); i++)
		{
			SimpleDirectedWeightedGraph<Object,IntraDomainEdge> graph_lambda;
			log.info("Adding graph of lambda ::"+i);			
			graph_lambda=networkGraphs.get(i);
			graph_lambda.addEdge(source,destination,baseSimplegraph.getEdge(source, destination));
		}
		
		
		//initialize();
	}

	


	@Override
	public void notifyTEDBFullUpdate() {
		this.graphLock.lock();
		try{
			Set<Object> nodes= baseSimplegraph.vertexSet();
			Iterator<Object> iter;
			Set<IntraDomainEdge> fiberEdges= baseSimplegraph.edgeSet();
			Iterator<IntraDomainEdge> iterFiberLink;
			
			networkGraphs=new ArrayList<SimpleDirectedWeightedGraph<Object,IntraDomainEdge>>(1);
			SimpleDirectedWeightedGraph<Object,IntraDomainEdge> graph_lambda;
			
			log.info("Looking at graph of lambda "+0);
			graph_lambda=networkGraphs.get(0);
			iter=nodes.iterator();
			iterFiberLink=fiberEdges.iterator();
			while (iterFiberLink.hasNext()){
				IntraDomainEdge fiberEdge =iterFiberLink.next();
				if (!(graph_lambda.containsEdge(fiberEdge.getSource(), fiberEdge.getTarget()))){				
					graph_lambda.addEdge(fiberEdge.getSource(),fiberEdge.getTarget(),fiberEdge);
					IntraDomainEdge edge = graph_lambda.getEdge(fiberEdge.getSource(),fiberEdge.getTarget());
					edge.getTE_info().setFreeWLANS(fiberEdge.getTE_info().getCopyUnreservedWLANs());
				}
			}	

		}finally{
			this.graphLock.unlock();
		}

	}	


	public String printBaseTopology(){
		String topoString;
		Set<Object> vetexSet= baseSimplegraph.vertexSet();
		Iterator <Object> vertexIterator=vetexSet.iterator();
		topoString="Nodes: \r\n";
		while (vertexIterator.hasNext()){
			Object vertex= vertexIterator.next();
			topoString=topoString+"\t"+vertex.toString()+"\r\n";
		}
		topoString=topoString+"Intradomain Link list: \r\n";
		Set<IntraDomainEdge> edgeSet= baseSimplegraph.edgeSet();
		Iterator <IntraDomainEdge> edgeIterator=edgeSet.iterator();
		while (edgeIterator.hasNext()){
			IntraDomainEdge edge= edgeIterator.next();
			topoString=topoString+"\t"+edge.toString()+"\r\n";
		}		
		return topoString;
	}

	public String printTopology( int lambda){
		String topoString;
		Set<Object> vetexSet=networkGraphs.get(lambda).vertexSet();
		Iterator <Object> vertexIterator=vetexSet.iterator();
		topoString="Nodes: \r\n";
		while (vertexIterator.hasNext()){
			Object vertex= vertexIterator.next();
			topoString=topoString+"\t"+vertex.toString()+"\r\n";
		}
		topoString=topoString+"Intradomain Link list: \r\n";
		Set<IntraDomainEdge> edgeSet= networkGraphs.get(lambda).edgeSet();
		Iterator <IntraDomainEdge> edgeIterator=edgeSet.iterator();
		while (edgeIterator.hasNext()){
			IntraDomainEdge edge= edgeIterator.next();
			topoString=topoString+"\t"+edge.toString()+"\r\n";
		}		
		return topoString;
	}


	@Override
	public void notificationEdgeIP_AuxGraph(Object src, Object dst,
			TE_Information informationTEDB) {

		
	}

	@Override
	public void notifyNewEdgeIP(Object source, Object destination,
			TE_Information informationTEDB) {

		
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
}