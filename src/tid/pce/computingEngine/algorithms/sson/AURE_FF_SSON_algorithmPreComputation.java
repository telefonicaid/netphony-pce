package tid.pce.computingEngine.algorithms.sson;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

import org.jgrapht.graph.DirectedMultigraph;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;

import tid.ospf.ospfv2.lsa.tlv.subtlv.complexFields.BitmapLabelSet;
import tid.pce.computingEngine.algorithms.ComputingAlgorithmPreComputationSSON;
import tid.pce.tedb.DomainTEDB;
import tid.pce.tedb.IntraDomainEdge;
import tid.pce.tedb.SSONInformation;
import tid.pce.tedb.SimpleTEDB;
import tid.pce.tedb.TEDB;

public class AURE_FF_SSON_algorithmPreComputation  implements ComputingAlgorithmPreComputationSSON{

	private ArrayList<SimpleDirectedWeightedGraph<Object, IntraDomainEdge>> networkGraphs;

	private SimpleDirectedWeightedGraph<Object, IntraDomainEdge> baseSimplegraph;

	private int numLambdas;

	private Logger log;

	private Lock graphLock;

	private DomainTEDB ted;

	private SSONInformation SSONInfo;

	public AURE_FF_SSON_algorithmPreComputation(){
		log=Logger.getLogger("PCEServer");
	}

	public void initialize(){
		log.info("initializing AURE_FF_SSON Algorithm");
		graphLock=new ReentrantLock();

		Set<Object> nodes= baseSimplegraph.vertexSet();
		Iterator<Object> iter;
		Set<IntraDomainEdge> fiberEdges= baseSimplegraph.edgeSet();
		Iterator<IntraDomainEdge> iterFiberLink;
		if (numLambdas>0){
			networkGraphs=new ArrayList<SimpleDirectedWeightedGraph<Object,IntraDomainEdge>>(numLambdas);
			SimpleDirectedWeightedGraph<Object,IntraDomainEdge> graph_lambda;
			for (int i=0;i<numLambdas;++i){
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
					IntraDomainEdge edge=new IntraDomainEdge();
					edge.setDelay_ms(fiberEdge.getDelay_ms());
					graph_lambda.addEdge((Object)fiberEdge.getSource(),(Object)fiberEdge.getTarget(),edge);			
				}
			}	
		}else {
			log.severe("REGISTERING AURE ALGORITHM WITHOUT KNOWN NUMBER OF LAMBDAS");
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
		SSONInfo=((DomainTEDB)ted).getSSONinfo();
		this.ted=(DomainTEDB)ted;
		this.numLambdas=SSONInfo.getNumLambdas();
	}

	public ArrayList<SimpleDirectedWeightedGraph<Object, IntraDomainEdge>> getNetworkGraphs() {
		return networkGraphs;
	}

	@Override
	public void notifyWavelengthReservationSSON(
			LinkedList<Object> sourceVertexList,
			LinkedList<Object> targetVertexList, int wavelength, int m) {

		graphLock.lock();
		try{
			for (int j=0; j<m*2; j++){
				SimpleDirectedWeightedGraph<Object, IntraDomainEdge> networkGraph=networkGraphs.get(wavelength-m+j);
			
				for (int i=0;i<sourceVertexList.size();++i){				
					networkGraph.removeEdge(sourceVertexList.get(i), targetVertexList.get(i));
				//System.out.println("EDGE OUT");
				}
			}}finally{
				graphLock.unlock();	
			}
			

	}
	
	@Override
	public void notifyWavelengthEndReservationSSON(
			LinkedList<Object> sourceVertexList,
			LinkedList<Object> targetVertexList, int wavelength, int m) {		
		graphLock.lock();
		try{
			for (int j=0; j<m*2; j++){
				SimpleDirectedWeightedGraph<Object, IntraDomainEdge> networkGraph=networkGraphs.get(wavelength-m+j);
				for (int i=0;i<sourceVertexList.size()-1;++i){
					//SOLO VOLVER A PONER COMO LIBRE SI EN OSPF NO ESTA RESERVADA
					if (baseSimplegraph.getEdge(sourceVertexList.get(i), targetVertexList.get(i)).getTE_info().isWavelengthFree(wavelength-m+j)){
						//log.info("adding new graph, reservation ends");
						networkGraph.addEdge(sourceVertexList.get(i), targetVertexList.get(i),baseSimplegraph.getEdge(sourceVertexList.get(i), targetVertexList.get(i)));
						
					}
				}
			}
		}finally{
			graphLock.unlock();	
		}

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
		for (int i=0;i<numLambdas;++i){
			log.info("Adding graph of lambda "+i);			
			graph_lambda=networkGraphs.get(i);
			graph_lambda.addVertex(vertex);						
		}		

	}

	/**
	 * 
	 */
	public void notifyNewEdge(Object source, Object destination) {
		SimpleDirectedWeightedGraph<Object,IntraDomainEdge> graph_lambda;
		for (int i=0;i<numLambdas;++i){
			log.info("Adding graph of lambda "+i);			
			graph_lambda=networkGraphs.get(i);
			IntraDomainEdge edge=new IntraDomainEdge();

			edge.setDelay_ms(baseSimplegraph.getEdge(source, destination).getDelay_ms());


			graph_lambda.addEdge(source,destination,edge);

		}		

	}
	
	
	@Override
	public void notifyWavelengthReservation(
			LinkedList<Object> sourceVertexList,
			LinkedList<Object> targetVertexList, int wavelength) {

		graphLock.lock();
		try{
			SimpleDirectedWeightedGraph<Object, IntraDomainEdge> networkGraph=networkGraphs.get(wavelength);
			for (int i=0;i<sourceVertexList.size();++i){				
				networkGraph.removeEdge(sourceVertexList.get(i), targetVertexList.get(i));
				//System.out.println("EDGE OUT");
				
			}}finally{
				graphLock.unlock();	
			}
			

	}
	@Override
	public void notifyWavelengthEndReservation(
			LinkedList<Object> sourceVertexList,
			LinkedList<Object> targetVertexList, int wavelength) {		
		graphLock.lock();
		try{
			
			SimpleDirectedWeightedGraph<Object, IntraDomainEdge> networkGraph=networkGraphs.get(wavelength);
			for (int i=0;i<sourceVertexList.size()-1;++i){
				//SOLO VOLVER A PONER COMO LIBRE SI EN OSPF NO ESTA RESERVADA
				if (baseSimplegraph.getEdge(sourceVertexList.get(i), targetVertexList.get(i)).getTE_info().isWavelengthFree(wavelength)){
					//log.info("adding new graph, reservation ends");
					//Add edge with delay
//					IntraDomainEdge edge=new IntraDomainEdge();
//					edge.setDelay_ms(baseSimplegraph.getEdge(sourceVertexList.get(i), targetVertexList.get(i)).getDelay_ms());
					networkGraph.addEdge(sourceVertexList.get(i), targetVertexList.get(i),baseSimplegraph.getEdge(sourceVertexList.get(i), targetVertexList.get(i)));
					
				}
			}
		}finally{
			graphLock.unlock();	
		}

	}
	
	public SSONInformation getSSONInfo() {
		return SSONInfo;
	}

	public void setSSONInfo(SSONInformation sSONInfo) {
		SSONInfo = sSONInfo;
	}
	

	public Lock getGraphLock() {
		return graphLock;
	}

	public void setGraphLock(Lock graphLock) {
		this.graphLock = graphLock;
	}


	@Override
	public void notifyTEDBFullUpdate() {
		this.graphLock.lock();
		try{
			Set<Object> nodes= baseSimplegraph.vertexSet();
			Iterator<Object> iter;
			Set<IntraDomainEdge> fiberEdges= baseSimplegraph.edgeSet();
			Iterator<IntraDomainEdge> iterFiberLink;
			if (numLambdas>0){
				networkGraphs=new ArrayList<SimpleDirectedWeightedGraph<Object,IntraDomainEdge>>(numLambdas);
				SimpleDirectedWeightedGraph<Object,IntraDomainEdge> graph_lambda;
				for (int i=0;i<numLambdas;++i){
					log.info("Looking at graph of lambda "+i);
					graph_lambda=networkGraphs.get(i);
					iter=nodes.iterator();
					//					while (iter.hasNext()){
					//						if (graph_lambda getEdge(sourceVertex, targetVertex))
					//						graph_lambda.addVertex( iter.next());			
					//					}
					iterFiberLink=fiberEdges.iterator();
					while (iterFiberLink.hasNext()){
						IntraDomainEdge fiberEdge =iterFiberLink.next();
						if (!(graph_lambda.containsEdge(fiberEdge.getSource(), fiberEdge.getTarget()))){
							//If the edge is not there... we look at the reservation status of the wavelength
							if ((fiberEdge.getTE_info().isWavelengthFree(i))&&(fiberEdge.getTE_info().isWavelengthUnreserved(i))){
								IntraDomainEdge edge=new IntraDomainEdge();
								graph_lambda.addEdge(fiberEdge.getSource(),fiberEdge.getTarget(),edge);		
							}

						}
					}	
				}

			}


		}finally{
			this.graphLock.unlock();
		}

	}

	@Override
	public DirectedMultigraph<Object, IntraDomainEdge> getBaseMultigraph() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SimpleDirectedWeightedGraph<Object, IntraDomainEdge> getBaseSimplegraph() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ArrayList<DirectedMultigraph<Object, IntraDomainEdge>> getNetworkMultiGraphs() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isMultifiber() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void notifyNewEdge_multiLink(Object source,
			Object destination, long srcIfId, long dstIfId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String printBaseTopology() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String printEdge(Object source, Object destination) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String printTopology(int lambda) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setBaseMultigraph(
			DirectedMultigraph<Object, IntraDomainEdge> baseMultigraph) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setBaseSimplegraph(
			SimpleDirectedWeightedGraph<Object, IntraDomainEdge> baseSimplegraph) {
		// TODO Auto-generated method stub
		
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




}
