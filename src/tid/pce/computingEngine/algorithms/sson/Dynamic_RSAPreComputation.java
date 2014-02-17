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
import tid.pce.computingEngine.algorithms.ComputingAlgorithmPreComputation;
import tid.pce.computingEngine.algorithms.ComputingAlgorithmPreComputationSSON;
import tid.pce.pcep.constructs.Request;
import tid.pce.tedb.DomainTEDB;
import tid.pce.tedb.IntraDomainEdge;
import tid.pce.tedb.SSONInformation;
import tid.pce.tedb.SimpleTEDB;
import tid.pce.tedb.TEDB;
import tid.pce.tedb.TE_Information;
import tid.pce.tedb.WSONInformation;
import tid.pce.computingEngine.algorithms.utilities.*;
import tid.protocol.commons.ByteHandler;

public class Dynamic_RSAPreComputation  implements ComputingAlgorithmPreComputationSSON{

	private ArrayList<SimpleDirectedWeightedGraph<Object, IntraDomainEdge>> networkGraphs;

	private SimpleDirectedWeightedGraph<Object, IntraDomainEdge> baseSimplegraph;

	private int numLambdas;

	private Logger log;

	private Lock graphLock;
	
	private double totalBandwidth=0;
	
	private ArrayList<ArrayList<BitmapLabelSet>> totalSetChannels;

	private DomainTEDB ted;

	private SSONInformation SSONInfo;

	public Dynamic_RSAPreComputation(){
		log=Logger.getLogger("PCEServer");
	}

	public void initialize(){
		log.info("initializing Dynamic_RSA Algorithm");
		graphLock=new ReentrantLock();
		/*try {
			Thread.sleep(20000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		Set<Object> nodes= baseSimplegraph.vertexSet();
		Iterator<Object> iter;
		Set<IntraDomainEdge> fiberEdges= baseSimplegraph.edgeSet();
		Iterator<IntraDomainEdge> iterFiberLink;
		TE_Information tE_Info=null;
		
		networkGraphs=new ArrayList<SimpleDirectedWeightedGraph<Object,IntraDomainEdge>>(numLambdas);
		SimpleDirectedWeightedGraph<Object,IntraDomainEdge> graphLambda;
		graphLambda=new SimpleDirectedWeightedGraph<Object,IntraDomainEdge>(IntraDomainEdge.class);
		networkGraphs.add(0, graphLambda);
		
		iter=nodes.iterator();
		while (iter.hasNext()){
			graphLambda.addVertex( iter.next());			
		}
		iterFiberLink=fiberEdges.iterator();
		
		while (iterFiberLink.hasNext()){
			IntraDomainEdge fiberEdge =iterFiberLink.next();
			IntraDomainEdge edge=new IntraDomainEdge();
			edge.setDelay_ms(fiberEdge.getDelay_ms());
			edge.setSrc_if_id(fiberEdge.getSrc_if_id());
			edge.setDst_if_id(fiberEdge.getDst_if_id());
			
			tE_Info=new TE_Information();
			tE_Info.createBitmapLabelSet(SSONInfo.getNumLambdas(), SSONInfo.getGrid(), SSONInfo.getCs(), 0, SSONInfo.getnMin(), SSONInfo.getnMin()+SSONInfo.getNumLambdas());
			edge.setTE_info(tE_Info);
			graphLambda.addEdge(fiberEdge.getSource(),fiberEdge.getTarget(),edge);
			//FuncionesUtiles.printByte(((BitmapLabelSet)fiberEdge.getTE_info().getAvailableLabels().getLabelSet()).getBytesBitMap(),"TE_Info",log);
			//log.info(" "+((BitmapLabelSet)fiberEdge.getTE_info().getAvailableLabels().getLabelSet()).getBytesBitMap()toString()+" \n");
		}
		
		/* To solve the RSA problem by defining channels, i.e. sets of
		*  spectrum contiguous frequency slots; Channels were introduced
		*  in [7] as a concept to simplify the offline RSA problem.
		*  The use of connection request-tailored channels allows
		*  removing the spectrum contiguity problem from mathematical
		*  formulations. Channels can be grouped as a function of the number of slots,
		*  e.g., the set of channels C2 = {{1, 1, 0, 0, 0, 0, 0, 0}, {0, 1, 1, 0, 0, 0, 0, 0}, 
		*  {0, 0, 1, 1, 0, 0, 0, 0}, . . . {0, 0, 0, 0, 0, 0, 1, 1}} includes every channel
		*  using two contiguous slots, where each position is 1 if a given channel uses that slot.
		*/
		totalSetChannels =  new ArrayList<ArrayList<BitmapLabelSet>>();
		int counter = 16;
		for (int i=0;i<counter;i++){
			ArrayList<BitmapLabelSet> setChannels = new ArrayList<BitmapLabelSet>(SSONInfo.getNumLambdas()-i + 1);
			channel_generator genChannels=new channel_generator();
			genChannels.getSetChannels(SSONInfo.getNumLambdas(), (i+1), setChannels);
			totalSetChannels.add(i, setChannels);
		}
		
		log.info(""+printTopology(0));
	}
	@Override
	public void setTEDB(TEDB ted) {
		try {
			baseSimplegraph=((SimpleTEDB)ted).getNetworkGraph();
			log.info("Using SimpleTEDB");
			log.info(baseSimplegraph.toString());
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
		SSONInfo=((DomainTEDB)ted).getSSONinfo();
		this.ted=(DomainTEDB)ted;
		this.numLambdas=SSONInfo.getNumLambdas();
		log.info(">>>>>Viendo informacion de copia:");
		log.info(this.ted.printTopology());
	}

	public ArrayList<SimpleDirectedWeightedGraph<Object, IntraDomainEdge>> getNetworkGraphs() {
		return networkGraphs;
	}

	
	public void notifyWavelengthReservationSSON(
			LinkedList<Object> sourceVertexList,
			LinkedList<Object> targetVertexList, int wavelength, int m) {

		graphLock.lock();
		try{
			for (int j=0;j<m*2;j++){
				SimpleDirectedWeightedGraph<Object, IntraDomainEdge> networkGraph=baseSimplegraph;
				for (int i=0;i<sourceVertexList.size();++i){		
					IntraDomainEdge edge=networkGraph.getEdge(sourceVertexList.get(i), targetVertexList.get(i));	
					edge.getTE_info().setWavelengthReserved(wavelength-m+j);
					
				}
			}}finally{
//				for (int i=0;i<sourceVertexList.size();++i){
//							log.info("Reserving in Precomp: "+networkGraphs.get(0).getEdge(sourceVertexList.get(i), targetVertexList.get(i)).toString());
//						}
				graphLock.unlock();	
			}

	}
	
	
	@Override
	public void notifyWavelengthEndReservationSSON(
			LinkedList<Object> sourceVertexList,
			LinkedList<Object> targetVertexList, int wavelength,int m) {		
		graphLock.lock();
		try{
			for (int j=0;j<m*2;j++){
				SimpleDirectedWeightedGraph<Object, IntraDomainEdge> networkGraph=baseSimplegraph;
				for (int i=0;i<sourceVertexList.size();++i){
					IntraDomainEdge edge=networkGraph.getEdge(sourceVertexList.get(i),targetVertexList.get(i));
					edge.getTE_info().setWavelengthUnReserved(wavelength-m +j);
				}
		}}finally{
//			for (int i=0;i<sourceVertexList.size();++i){
//				log.info("Derreserving in Precomp: "+networkGraphs.get(0).getEdge(sourceVertexList.get(i), targetVertexList.get(i)).toString());
//			}
			graphLock.unlock();	
		}

	}
	public double getTotalBandwidth() {
		return totalBandwidth;
	}
	
	public SimpleDirectedWeightedGraph<Object, IntraDomainEdge> getbaseSimplegraph() {
		return baseSimplegraph;
	}

	public void setTotalBandwidth(double totalBandwidth) {
		this.totalBandwidth = totalBandwidth;
	}

	public Lock getGraphLock() {
		return graphLock;
	}
	public ArrayList<ArrayList<BitmapLabelSet>> getTotalSetChannels() {
		return totalSetChannels;
	}

	public void setTotalSetChannels(
			ArrayList<ArrayList<BitmapLabelSet>> totalSetChannels) {
		this.totalSetChannels = totalSetChannels;
	}
	
	public void setGraphLock(Lock graphLock) {
		this.graphLock = graphLock;
	}


	@Override
	public void notifyWavelengthStatusChange(Object source,
			Object destination, BitmapLabelSet previousBitmapLabelSet,
			BitmapLabelSet newBitmapLabelSet) {
		log.info("notifyWavelengthStatusChange in precomp");
		previousBitmapLabelSet.getNumLabels();
		int num_bytes=previousBitmapLabelSet.getBytesBitMap().length;
		int wavelength_to_occupy=-1;
		int wavelength_to_free=-1;
		SimpleDirectedWeightedGraph<Object, IntraDomainEdge> networkGraph=baseSimplegraph;
		log.info("Link antes de actualizar en el precomputo del algoritmo: "+networkGraph.getEdge(source, destination).toString());
		try{
			graphLock.lock();
			for (int i=0;i<num_bytes;++i){
				if (previousBitmapLabelSet.getBytesBitMap()[i]!=newBitmapLabelSet.getBytesBitMap()[i]){
					for (int k=0;k<8;++k){
						if ((newBitmapLabelSet.getBytesBitMap()[i]&(0x80>>>k))>(previousBitmapLabelSet.getBytesBitMap()[i]&(0x80>>>k))){
							wavelength_to_occupy=k+(i*8);
							IntraDomainEdge edge=networkGraph.getEdge(source,destination);		
							edge.getTE_info().setWavelengthOccupied(wavelength_to_occupy);
						}else if ((newBitmapLabelSet.getBytesBitMap()[i]&(0x80>>>k))<(previousBitmapLabelSet.getBytesBitMap()[i]&(0x80>>>k))){
								wavelength_to_free=k+(i*8);
								IntraDomainEdge edge=networkGraph.getEdge(source,destination);
								//log.info("Lambda a dereservar: "+wavelength_to_free+" \n");
								edge.getTE_info().setWavelengthFree(wavelength_to_free);
						}
					}
				}
				
			}
			
			log.info("Link actualizado en el precomputo del algoritmo: "+networkGraph.getEdge(source, destination).toString());

		}finally{
			graphLock.unlock();	
		}
	}

	/**
	 * This function is called when a new Vertex is added
	 */
	public void notifyNewVertex(Object vertex) {
		SimpleDirectedWeightedGraph<Object,IntraDomainEdge> graph_lambda;
		graph_lambda=baseSimplegraph;
		graph_lambda.addVertex(vertex);						
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
			SimpleDirectedWeightedGraph<Object, IntraDomainEdge> networkGraph=baseSimplegraph;
			for (int i=0;i<sourceVertexList.size();++i){		
				IntraDomainEdge edge=networkGraph.getEdge(sourceVertexList.get(i), targetVertexList.get(i));	
				edge.getTE_info().setWavelengthReserved(wavelength);

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
			SimpleDirectedWeightedGraph<Object, IntraDomainEdge> networkGraph=baseSimplegraph;
			for (int i=0;i<sourceVertexList.size();++i){
				IntraDomainEdge edge=networkGraph.getEdge(sourceVertexList.get(i),targetVertexList.get(i) );
				edge.getTE_info().setWavelengthUnReserved(wavelength);

		}}finally{
			graphLock.unlock();	
		}

	}
	
	public void setReservation(int M, int N,Object source, Object dest) {
		
		((BitmapLabelSet) baseSimplegraph.getEdge(source, dest).getTE_info().getAvailableLabels().getLabelSet()).setReservation(M, N);
	}

	public SSONInformation getWSONInfo() {
		return SSONInfo;
	}

	public void setWSONInfo(SSONInformation SSONInfo) {
		this.SSONInfo = SSONInfo;
	}

	@Override
	public void notifyTEDBFullUpdate() {
		this.graphLock.lock();
		log.info("Entramos aqui????");
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
					graph_lambda=baseSimplegraph;
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
	public DirectedMultigraph<Object, IntraDomainEdge> getBaseMultigraph() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setBaseMultigraph(
			DirectedMultigraph<Object, IntraDomainEdge> baseMultigraph) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public SimpleDirectedWeightedGraph<Object, IntraDomainEdge> getBaseSimplegraph() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setBaseSimplegraph(
			SimpleDirectedWeightedGraph<Object, IntraDomainEdge> baseSimplegraph) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String printEdge(Object source, Object destination) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String printBaseTopology() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String printTopology( int lambda){
		String topoString;
		Set<Object> vertexSet=null;
		vertexSet=baseSimplegraph.vertexSet();
		
		Iterator <Object> vertexIterator=vertexSet.iterator();
		topoString="Nodes: \r\n";
		while (vertexIterator.hasNext()){
			Object vertex= vertexIterator.next();
			topoString=topoString+"\t"+vertex.toString()+"\r\n";
		}
		topoString=topoString+"Intradomain Link list: \r\n";
		Set<IntraDomainEdge> edgeSet=null;
		edgeSet= baseSimplegraph.edgeSet();
		Iterator <IntraDomainEdge> edgeIterator=edgeSet.iterator();
		while (edgeIterator.hasNext()){
			IntraDomainEdge edge= edgeIterator.next();
			topoString=topoString+"\t"+edge.toString()+"\r\n";
		}
		return topoString;
	}

	@Override
	public void notifyNewEdge_multiLink(Object source,
			Object destination, long srcIfId, long dstIfId) {
		// TODO Auto-generated method stub
		
	}	



}
