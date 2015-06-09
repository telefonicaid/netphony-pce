package tid.pce.computingEngine.algorithms.vlan;

/**
 * This algorithms doesn't let that an LSP changes wlan so the wlan of entrace in a swith is the same that
 * goes out
 * 
 * @author jaume
 */

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
import tid.pce.computingEngine.algorithms.ComputingAlgorithmPreComputation;
import tid.pce.tedb.DomainTEDB;
import tid.pce.tedb.IntraDomainEdge;
import tid.pce.tedb.SimpleTEDB;
import tid.pce.tedb.TEDB;
import tid.pce.tedb.TE_Information;


public class AURE_WLAN_algorithmPreComputation  implements ComputingAlgorithmPreComputation{

	private ArrayList<SimpleDirectedWeightedGraph<Object, IntraDomainEdge>> networkGraphs;

	private SimpleDirectedWeightedGraph<Object, IntraDomainEdge> baseSimplegraph;

	private int numWLANs;

	private Logger log;

	private Lock graphLock;

	private DomainTEDB ted;

	public AURE_WLAN_algorithmPreComputation(){
		log=Logger.getLogger("PCEServer");
	}

	public void initialize(){
		log.info("initializing AURE Algorithm WLAN");
		graphLock=new ReentrantLock();

		Set<Object> nodes= baseSimplegraph.vertexSet();
		
		Iterator<Object> iter;
		Set<IntraDomainEdge> fiberEdges= baseSimplegraph.edgeSet();
		Iterator<IntraDomainEdge> iterFiberLink;
		if (numWLANs>0){
			networkGraphs=new ArrayList<SimpleDirectedWeightedGraph<Object,IntraDomainEdge>>(numWLANs);
			SimpleDirectedWeightedGraph<Object,IntraDomainEdge> graph_lambda;
			for (int i=0;i<numWLANs;++i){
				log.info("Adding graph of lambda "+i);
				graph_lambda=new SimpleDirectedWeightedGraph<Object,IntraDomainEdge>(IntraDomainEdge.class);
				networkGraphs.add(i, graph_lambda);
				iter=nodes.iterator();
				while (iter.hasNext()){
					graph_lambda.addVertex( iter.next());			
				}
				iterFiberLink=fiberEdges.iterator();
				while (iterFiberLink.hasNext()){
					IntraDomainEdge fiberEdge =iterFiberLink.next();
					//IntraDomainEdge edge=new IntraDomainEdge();
					//edge.setDelay_ms(fiberEdge.getDelay_ms());
					graph_lambda.addEdge(fiberEdge.getSource(),fiberEdge.getTarget(),fiberEdge);			
				}
			}	
		}else {
			log.severe("REGISTERING AURE ALGORITHM WITHOUT KNOWN NUMBER OF WLANS");
			System.exit(-1);
		}
	}

	@Override
	public void setTEDB(TEDB ted) {
		try {
			baseSimplegraph=((SimpleTEDB)ted).getNetworkGraph();
			log.info("Using SimpleTEDB");				
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
		this.ted=(DomainTEDB)ted;
		Set<IntraDomainEdge> allEdges = baseSimplegraph.edgeSet();
		
		numWLANs = -1;
		for (IntraDomainEdge edge : allEdges)
		{
			if (numWLANs == -1)
			{
				numWLANs = edge.getTE_info().getNumberWLANs();
			}
			else
			{
				numWLANs = Math.min(edge.getTE_info().getNumberWLANs(),numWLANs);
			}
		}
		
		if (numWLANs < 0)
		{
			log.warning("Something wrong with the xml and the number of wlans");
		}
		log.info("Number of wlans:  "+numWLANs);
	}

	public ArrayList<SimpleDirectedWeightedGraph<Object, IntraDomainEdge>> getNetworkGraphs() 
	{
		return networkGraphs;
	}

	@Override
	public void notifyWavelengthReservation(
			LinkedList<Object> sourceVertexList,
			LinkedList<Object> targetVertexList, int wavelength) {
		graphLock.lock();
		try{
			SimpleDirectedWeightedGraph<Object, IntraDomainEdge> networkGraph=networkGraphs.get(wavelength);
		}finally{
				graphLock.unlock();	
		}
	}
	
	@Override
	public void notifyWavelengthEndReservation(
			LinkedList<Object> sourceVertexList,
			LinkedList<Object> targetVertexList, int wavelength) 
	{		
		graphLock.lock();
		try{
			SimpleDirectedWeightedGraph<Object, IntraDomainEdge> networkGraph=networkGraphs.get(wavelength);
			for (int i=0;i<sourceVertexList.size()-1;++i)
			{
				//SOLO VOLVER A PONER COMO LIBRE SI EN OSPF NO ESTA RESERVADA
				if (baseSimplegraph.getEdge(sourceVertexList.get(i), targetVertexList.get(i)).getTE_info().isWavelengthFree(wavelength)){
					//Add edge with delay
					IntraDomainEdge edge=new IntraDomainEdge();
					edge.setDelay_ms(baseSimplegraph.getEdge(sourceVertexList.get(i), targetVertexList.get(i)).getDelay_ms());
					networkGraph.addEdge(sourceVertexList.get(i), targetVertexList.get(i),edge);
					
				}
			}
		}
		finally
		{
			graphLock.unlock();	
		}
	}

	public Lock getGraphLock() 
	{
		return graphLock;
	}

	public void setGraphLock(Lock graphLock) 
	{
		this.graphLock = graphLock;
	}


	@Override
	public void notifyWavelengthStatusChange(Object source,
			Object destination, BitmapLabelSet previousBitmapLabelSet,
			BitmapLabelSet newBitmapLabelSet) {

		previousBitmapLabelSet.getNumLabels();
		int num_bytes=previousBitmapLabelSet.getBytesBitMap().length;
		int wavelength_to_occupy=-1;
		int wavelength_to_free=-1;
		
		try{
			graphLock.lock();
			for (int i=0;i<num_bytes;++i){
				if (previousBitmapLabelSet.getBytesBitMap()[i]!=newBitmapLabelSet.getBytesBitMap()[i]){
					for (int k=0;k<8;++k){
						if ((newBitmapLabelSet.getBytesBitMap()[i]&(0x80>>>k))>(previousBitmapLabelSet.getBytesBitMap()[i]&(0x80>>>k))){
							wavelength_to_occupy=k+(i*8);
							SimpleDirectedWeightedGraph<Object, IntraDomainEdge> networkGraph=networkGraphs.get(wavelength_to_occupy);
							networkGraph.removeEdge(source, destination);		

						}else if ((newBitmapLabelSet.getBytesBitMap()[i]&(0x80>>>k))<(previousBitmapLabelSet.getBytesBitMap()[i]&(0x80>>>k))){
							if ((newBitmapLabelSet.getBytesBitmapReserved()[i]&(0x80>>>k))==0){
								wavelength_to_free=k+(i*8);	
								SimpleDirectedWeightedGraph<Object, IntraDomainEdge> networkGraph=networkGraphs.get(wavelength_to_free);
								networkGraph.addEdge(source, destination,baseSimplegraph.getEdge(source, destination));
							}
						}
					}
				}
			}
		}finally{
			graphLock.unlock();	
		}
	}

	/**
	 * This function is called when a new Vertex is added
	 */
	public void notifyNewVertex(Object vertex) {
		SimpleDirectedWeightedGraph<Object,IntraDomainEdge> graph_lambda;
		for (int i=0;i<numWLANs;++i){
			log.info("Adding graph of lambda "+i);			
			graph_lambda=networkGraphs.get(i);
			graph_lambda.addVertex(vertex);						
		}		
	}

	/**
	 * 
	 */
	public void notifyNewEdge(Object source, Object destination) 
	{
		SimpleDirectedWeightedGraph<Object,IntraDomainEdge> graph_lambda;
		for (int i=0;i<numWLANs;++i){
			log.info("Adding graph of lambda "+i);			
			graph_lambda=networkGraphs.get(i);
//			IntraDomainEdge edge=new IntraDomainEdge();
//			edge.setDelay_ms(baseSimplegraph.getEdge(source, destination).getDelay_ms());
			graph_lambda.addEdge(source,destination,baseSimplegraph.getEdge(source, destination));
		}		
	}

	

	public int getNumWLANs() 
	{
		return numWLANs;
	}

	public void setNumWLANs(int numWLANs) 
	{
		this.numWLANs = numWLANs;
	}

	@Override
	public void notifyTEDBFullUpdate()
	{
		this.graphLock.lock();
		try{
			Set<Object> nodes= baseSimplegraph.vertexSet();
			Iterator<Object> iter;
			Set<IntraDomainEdge> fiberEdges= baseSimplegraph.edgeSet();
			Iterator<IntraDomainEdge> iterFiberLink;
			if (numWLANs>0){
				networkGraphs=new ArrayList<SimpleDirectedWeightedGraph<Object,IntraDomainEdge>>(numWLANs);
				SimpleDirectedWeightedGraph<Object,IntraDomainEdge> graph_lambda;
				for (int i=0;i<numWLANs;++i){
					log.info("Looking at graph of lambda "+i);
					graph_lambda=networkGraphs.get(i);
					iter=nodes.iterator();
					iterFiberLink=fiberEdges.iterator();
					while (iterFiberLink.hasNext()){
						IntraDomainEdge fiberEdge =iterFiberLink.next();
						if (!(graph_lambda.containsEdge(fiberEdge.getSource(), fiberEdge.getTarget()))){
							//If the edge is not there... we look at the reservation status of the wavelength
							if ((fiberEdge.getTE_info().isWavelengthFree(i))&&(fiberEdge.getTE_info().isWavelengthUnreserved(i))){
								//IntraDomainEdge edge=new IntraDomainEdge();
								graph_lambda.addEdge(fiberEdge.getSource(),fiberEdge.getTarget(),fiberEdge);		
							}

						}
					}	
				}

			}


		}finally{
			this.graphLock.unlock();
		}

	}	


	public String printBaseTopology()
	{
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

	public String printTopology( int lambda)
	{
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
			TE_Information informationTEDB) 
	{

		
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
