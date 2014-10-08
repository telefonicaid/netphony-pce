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

import es.tid.ospf.ospfv2.lsa.tlv.subtlv.complexFields.BitmapLabelSet;
import tid.pce.computingEngine.algorithms.ComputingAlgorithmPreComputationSSON;
import tid.pce.computingEngine.algorithms.utilities.channel_generator;
import tid.pce.tedb.DomainTEDB;
import tid.pce.tedb.IntraDomainEdge;
import tid.pce.tedb.SimpleTEDB;
import tid.pce.tedb.TEDB;
import tid.pce.tedb.TE_Information;
import tid.pce.tedb.WSONInformation;

public class SVEC_Dynamic_RSAPreComputation implements ComputingAlgorithmPreComputationSSON{

	private ArrayList<SimpleDirectedWeightedGraph<Object, IntraDomainEdge>> networkGraphs;

	private SimpleDirectedWeightedGraph<Object, IntraDomainEdge> baseSimplegraph;

	private int numLambdas;

	private Logger log;
	
	private double totalBandwidth=0;
	private double totalRejectedBandwidth=0;

	private Lock graphLock;

	private DomainTEDB ted;
	
	private ArrayList<ArrayList<BitmapLabelSet>> totalSetChannels;

	private WSONInformation WSONInfo;

	public SVEC_Dynamic_RSAPreComputation(){
		log=Logger.getLogger("PCEServer");
	}

	public void initialize(){
		log.info("initializing SVEC_Dynamic_RSA Algorithm");
		graphLock=new ReentrantLock();
		
		
		Set<Object> nodes= baseSimplegraph.vertexSet();
		Iterator<Object> iter;
		Set<IntraDomainEdge> fiberEdges= baseSimplegraph.edgeSet();
		Iterator<IntraDomainEdge> iterFiberLink;
		TE_Information tE_Info= null;
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
			tE_Info=new TE_Information();
			tE_Info.createBitmapLabelSet(WSONInfo.getNumLambdas(), WSONInfo.getGrid(), WSONInfo.getCs(), 0, WSONInfo.getnMin(), WSONInfo.getnMin()+WSONInfo.getNumLambdas());
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
		for (int i=0;i<(16);i++){
			ArrayList<BitmapLabelSet> setChannels = new ArrayList<BitmapLabelSet>(tE_Info.getAvailableLabels().getLabelSet().getNumLabels()-i + 1);
			channel_generator genChannels=new channel_generator();
			genChannels.getSetChannels(tE_Info.getAvailableLabels().getLabelSet().getNumLabels(), (i+1), setChannels);
			totalSetChannels.add(i, setChannels);
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
		WSONInfo=((DomainTEDB)ted).getWSONinfo();
		this.ted=(DomainTEDB)ted;
		this.numLambdas=WSONInfo.getNumLambdas();
	}

	public ArrayList<SimpleDirectedWeightedGraph<Object, IntraDomainEdge>> getNetworkGraphs() {
		return networkGraphs;
	}

	public ArrayList<ArrayList<BitmapLabelSet>> getTotalSetChannels() {
		return totalSetChannels;
	}

	public void setTotalSetChannels(
			ArrayList<ArrayList<BitmapLabelSet>> totalSetChannels) {
		this.totalSetChannels = totalSetChannels;
	}
	public static String toHexString(byte [] packetBytes){
		 StringBuffer sb=new StringBuffer(packetBytes.length*2);
		 for (int i=0; i<packetBytes.length;++i){
		  if ((packetBytes[i]&0xFF)<=0x0F){
		   sb.append('0');
		   sb.append(Integer.toHexString((packetBytes[i]&0xFF))); 
		  }
		  else {
		   sb.append(Integer.toHexString((packetBytes[i]&0xFF)));
		  }
		 }
		 return sb.toString();
		 
		 }

	@Override
	public void notifyWavelengthReservation(
			LinkedList<Object> sourceVertexList,
			LinkedList<Object> targetVertexList, int wavelength) {

		graphLock.lock();
		try{
			SimpleDirectedWeightedGraph<Object, IntraDomainEdge> networkGraph=networkGraphs.get(0);
			for (int i=0;i<sourceVertexList.size();++i){		
				IntraDomainEdge edge=networkGraph.getEdge(sourceVertexList.get(i), targetVertexList.get(i));
				edge.getTE_info().setWavelengthReserved(wavelength);
//				FuncionesUtiles.printByte(((BitmapLabelSet)networkGraph.getEdge(sourceVertexList.get(i), targetVertexList.get(i)).getTE_info().getAvailableLabels().getLabelSet()).getBytesBitMap(), "BYtesBitmap", log);
//				if (bidirectional == true){
//					edge=networkGraph.getEdge(targetVertexList.get(i), sourceVertexList.get(i));
//					edge.getTE_info().setWavelengthReserved(wavelength);
//				}
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
			SimpleDirectedWeightedGraph<Object, IntraDomainEdge> networkGraph=networkGraphs.get(0);
			for (int i=0;i<sourceVertexList.size();++i){
				//			SimpleDirectedWeightedGraph<Object,FiberLinkEdge> graph= ted.getNetworkGraph();
				IntraDomainEdge edge=networkGraph.getEdge(sourceVertexList.get(i),targetVertexList.get(i) );
				edge.getTE_info().setWavelengthUnReserved(wavelength);
//				if (bidirectional == true)
//				{
//					edge=networkGraph.getEdge(targetVertexList.get(i), sourceVertexList.get(i));
//					edge.getTE_info().setWavelengthUnReserved(wavelength);
//				}
		}}finally{
			graphLock.unlock();	
		}

	}
	

	public Lock getGraphLock() {
		return graphLock;
	}

	public double getTotalBandwidth() {
		return totalBandwidth;
	}

	public void setTotalBandwidth(double totalBandwidth) {
		this.totalBandwidth = totalBandwidth;
	}
	
	public double getTotalRejectedBandwidth() {
		return totalRejectedBandwidth;
	}

	public void setTotalRejectedBandwidth(double totalRejectedBandwidth) {
		this.totalRejectedBandwidth = totalRejectedBandwidth;
	}

	public void setGraphLock(Lock graphLock) {
		this.graphLock = graphLock;
	}


	@Override
	public void notifyWavelengthStatusChange(Object source,
			Object destination, BitmapLabelSet previousBitmapLabelSet,
			BitmapLabelSet newBitmapLabelSet) {
		//log.info("notifyWavelengthStatusChange");
		previousBitmapLabelSet.getNumLabels();
		int num_bytes=previousBitmapLabelSet.getBytesBitMap().length;
		int wavelength_to_occupy=-1;
		int wavelength_to_free=-1;
		SimpleDirectedWeightedGraph<Object, IntraDomainEdge> networkGraph=networkGraphs.get(0);
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
							if ((newBitmapLabelSet.getBytesBitmapReserved()[i]&(0x80>>>k))==0){
								wavelength_to_free=k+(i*8);
								IntraDomainEdge edge=networkGraph.getEdge(source,destination);		
								edge.getTE_info().setWavelengthFree(wavelength_to_free);
								
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

	public WSONInformation getWSONInfo() {
		return WSONInfo;
	}

	public void setWSONInfo(WSONInformation wSONInfo) {
		WSONInfo = wSONInfo;
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
	public void notifyWavelengthEndReservationSSON(
			LinkedList<Object> sourceVertexList,
			LinkedList<Object> targetVertexList, int wavelength, int m) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void notifyWavelengthReservationSSON(
			LinkedList<Object> sourceVertexList,
			LinkedList<Object> targetVertexList, int wavelength, int m) {
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
	public String printTopology(int lambda) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void notifyNewEdge_multiLink(Object source,
			Object destination, long srcIfId, long dstIfId) {
		// TODO Auto-generated method stub
		
	}	



}
