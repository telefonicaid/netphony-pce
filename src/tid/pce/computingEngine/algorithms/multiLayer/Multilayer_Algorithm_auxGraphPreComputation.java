package tid.pce.computingEngine.algorithms.multiLayer;

import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Hashtable;
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
import es.tid.tedb.MultiLayerTEDB;
import es.tid.tedb.TEDB;
import es.tid.tedb.TE_Information;
import es.tid.tedb.WSONInformation;
import tid.pce.computingEngine.algorithms.ComputingAlgorithmPreComputation;

public class Multilayer_Algorithm_auxGraphPreComputation implements ComputingAlgorithmPreComputation{
	
	private SimpleDirectedWeightedGraph<Object, IntraDomainEdge> baseSimplegraph;
	
	private SimpleDirectedWeightedGraph<Object, IntraDomainEdge> Opticalgraph;
	
	private SimpleDirectedWeightedGraph<Object, IntraDomainEdge> IPGraph;
	
	public SimpleDirectedWeightedGraph<Object, IntraDomainEdge> getIPGraph() {
		return IPGraph;
	}

	public void setIPGraph(
			SimpleDirectedWeightedGraph<Object, IntraDomainEdge> iPGraph) {
		IPGraph = iPGraph;
	}

	public SimpleDirectedWeightedGraph<Object, IntraDomainEdge> getOpticalgraph() {
		return Opticalgraph;
	}

	public void setOpticalgraph(SimpleDirectedWeightedGraph<Object, IntraDomainEdge> opticalgraph) {
		Opticalgraph = opticalgraph;
	}

	private SimpleDirectedWeightedGraph<Object, IntraDomainEdge> Interlayer;
	
	public SimpleDirectedWeightedGraph<Object, IntraDomainEdge> getInterlayer() {
		return Interlayer;
	}

	public void setInterlayer(SimpleDirectedWeightedGraph<Object, IntraDomainEdge> interlayer) {
		Interlayer = interlayer;
	}

	private SimpleDirectedWeightedGraph<Object, IntraDomainEdge> aux_graph;

	private int numLambdas;
	
	private double peso_WLE=0;

	private Logger log;

	private Lock graphLock;

	private DomainTEDB ted;

	private WSONInformation WSONInfo;
	
	private double peso_LPE = 1;

	public Multilayer_Algorithm_auxGraphPreComputation(){
		log=Logger.getLogger("PCEServer");
	}
	
	private int grooming_policie=2;   // 0 --> MinTH: Minimizing the Number of Traffic Hops
									// 1 --> MinLP: Minimizing the number of LigthPaths
	 								// 2 --> MinWL: Minimizing the number of Wavelength-Links (WLE)
	private Integer MaxNodesOptical;

	public Integer getMaxNodesOptical() {
		return MaxNodesOptical;
	}

	public int getGrooming_policie() {
		return grooming_policie;
	}

	public class InfoNodo{ //Nodes --> (layer, I/0, nodeIP)
		private int numLayer;
		private int I_O;   // 1 -->
		private Object IPnode;
	
		public int getnumLayer(){
			return this.numLayer;
		}
		public void setnumLayer(int numLayer){
			this.numLayer = numLayer;
		}	
		public int getI_O(){
			return this.I_O;
		}	
		public void setI_O(int I_O){
			this.I_O = I_O;
		}		
		public Object getIPnode(){
			return this.IPnode;
		}		
		public void setIPnode(Object IPnode){
			this.IPnode = IPnode;
		}
	}

	//table of relation of nodes and info in the AuxGraph
	public Hashtable<Integer, InfoNodo> infoTable;
	
	public Hashtable<Integer, InfoNodo> getInfoTable() {
		return infoTable;
	}

	public void setInfoTable(Hashtable<Integer, InfoNodo> infoTable) {
		this.infoTable = infoTable;
	}

	public Hashtable<Integer, Object> info_IntNodes_IPv4;
	
	public Hashtable<Object, Object> Interlayer_relation_IP;
	public Hashtable<Object, Object> Interlayer_relation_OPTICAL;
	
	public Hashtable<Object, Object> getInterlayer_relation_OPTICAL() {
		return Interlayer_relation_OPTICAL;
	}

	public void setInterlayer_relation_OPTICAL(
			Hashtable<Object, Object> interlayerRelationOPTICAL) {
		Interlayer_relation_OPTICAL = interlayerRelationOPTICAL;
	}

	public Hashtable<Object, Object> getInterlayer_relation() {
		return Interlayer_relation_IP;
	}

	public void setInterlayer_relation(Hashtable<Object, Object> interlayerRelation) {
		Interlayer_relation_IP = interlayerRelation;
	}

	public Hashtable<Object, Integer> info_IPv4_IntNodes;
	//relación Nodos reales --> Nodos de acceso en el Auxiliar Graph
	public Hashtable<Object, Object> relation_access_nodes;

	public Hashtable<Object, Object> getRelation_access_nodes() {
		return relation_access_nodes;
	}

	public void setRelation_access_nodes(Hashtable<Object, Object> relationAccessNodes) {
		relation_access_nodes = relationAccessNodes;
	}

	public Hashtable<Object, Integer> getInfo_IPv4_IntNodes() {
		return info_IPv4_IntNodes;
	}
	
	public void setInfo_IPv4_IntNodes(Hashtable<Object, Integer> infoIPv4IntNodes) {
		info_IPv4_IntNodes = infoIPv4IntNodes;
	}

	public Hashtable<Integer, Object> getinfo_IntNodes_IPv4() {
		return info_IntNodes_IPv4;
	}

	public void setinfo_IntNodes_IPv4(Hashtable<Integer, Object> infoNodes) {
		this.info_IntNodes_IPv4 = infoNodes;
	}

	public void initialize(){
		graphLock=new ReentrantLock();
		double peso_GrmE=0, peso_TxE=0, peso_RxE=0, peso_MuxE = 0,
				peso_DmxE = 0, peso_WBE = 0;
		
		if(grooming_policie == 0){
			log.info("Grooming polocy: Minimizing the Number of Traffic Hops");
			peso_WLE = 10;
			peso_GrmE = 1000;
			peso_TxE = 20;
			peso_RxE = 20;
		}		
		else if(grooming_policie == 1){
			log.info("Grooming polocy: Minimizing the number of Lightpaths");
			peso_WLE = 10;
			peso_GrmE = 20;
			peso_TxE = 200;
			peso_RxE = 200;
		}
		else if(grooming_policie == 2){
			log.info("Grooming polocy: Minimizing the number of Wavelength_links");
			peso_WLE = 1000;
			peso_GrmE = 0;
			peso_TxE = 20;
			peso_RxE = 20;
		}
		int num_nodes;
				
		Set<Object> nodes= baseSimplegraph.vertexSet();
		num_nodes = nodes.size(); // number of nodes
		
						
		//Create the  Optical_networkGraph
		//number of layer, input / output, number of node
		int layer=1;
		int numNode = 0;
		Object Nodeaux = null;
		String baseIP = "172.20.1.";
		String cadena_sumar = "0.0.1.0";
		aux_graph=new SimpleDirectedWeightedGraph<Object,IntraDomainEdge>(IntraDomainEdge.class);	
		infoTable = new Hashtable<Integer, InfoNodo>();
		info_IntNodes_IPv4 = new Hashtable<Integer, Object>();
		Interlayer_relation_IP = new Hashtable<Object, Object>();
		Interlayer_relation_OPTICAL = new Hashtable<Object, Object>();
		info_IPv4_IntNodes = new Hashtable<Object, Integer>();
		relation_access_nodes = new Hashtable<Object, Object>();
		numLambdas = ((MultiLayerTEDB)ted).getWSONinfo().getNumLambdas();
		int I_O = 0;
		Object Node;
		int numNode_ip = 0;
		
		//relación interlayer
		
		Iterator<Object> iter_op = Opticalgraph.vertexSet().iterator();
		Iterator<Object> iter_ip = baseSimplegraph.vertexSet().iterator();
		
		while ((iter_ip.hasNext()) && (iter_op.hasNext())){
			Object node_IP = iter_ip.next();
			Object node_OP = iter_op.next();
			Interlayer_relation_IP.put(node_OP, node_IP);
			Interlayer_relation_OPTICAL.put(node_IP, node_OP);
			log.info("node_IP : "+node_IP+" node_OP :"+node_OP);
		}
										
		//asociar a cada Object de cada nodo, una string con la info Nodes --> (l, I/0, N_node), para saber 
		//a qué pertenece cada nodo --> un mapeo
		
		//////////////////////////////////////////////////////////////////////////////////////
		//				 CREAMOS EL AUXILIARY GRAPH --> number Lambdas + 2 layers
		//////////////////////////////////////////////////////////////////////////////////////
		
		Iterator<Object> iternodes = nodes.iterator(); // iterator for the node source
		
		for (layer=1; layer<=numLambdas+2; layer++) //iteration with wavelengths
		{
			numNode_ip = 0;
			baseIP = "172.20."+layer+".";
			while (iternodes.hasNext())
			{
				Node = iternodes.next();  //IP of the current node
				String source =new String();
						
				if (I_O == 1){
					I_O = 0;
				}				
				source = baseIP+ String.valueOf(numNode_ip);
				try {
					Nodeaux = (Inet4Address)Inet4Address.getByName(source);
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
												
				if (layer == numLambdas+2)
					relation_access_nodes.put(Node, Nodeaux);
				
				info_IntNodes_IPv4.put(numNode, Nodeaux);
				info_IPv4_IntNodes.put(Nodeaux, numNode);
				
				log.info("Nodo :"+Nodeaux+" "+info_IPv4_IntNodes.get(Nodeaux));
				InfoNodo info1=new InfoNodo();						
				//input --> 0 
				aux_graph.addVertex(Nodeaux);
				info1.setI_O(I_O);		//input / output
				if ((layer>=1) && (layer<=numLambdas)){
					info1.setIPnode(Interlayer_relation_OPTICAL.get(Node));
				}
				else 
					info1.setIPnode(Node);
				
				info1.setnumLayer(layer);
				infoTable.put(numNode, info1);
				numNode_ip++;
				numNode++;
				
				String source2 =new String();
				source2 = baseIP+ String.valueOf(numNode_ip);
				
				try {
					Nodeaux = (Inet4Address) Inet4Address.getByName(source2);
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				InfoNodo info2=new InfoNodo();
				//output --> 1
				I_O = 1;
				aux_graph.addVertex(Nodeaux); 
				info2.setI_O(I_O);     //input / output
				
				if ((layer>=1) && (layer<=numLambdas)){
					info2.setIPnode(Interlayer_relation_OPTICAL.get(Node));
				}
				else 
					info2.setIPnode(Node);
				
				info2.setnumLayer(layer);
				infoTable.put(numNode, info2);
				info_IntNodes_IPv4.put(numNode, Nodeaux);
				info_IPv4_IntNodes.put(Nodeaux, numNode);
				
				numNode++;
				numNode_ip++;
			}
			iternodes = nodes.iterator();	
			
		}
		
		///////////////////////////////////////////////////////////////////////////////
		//		 	ya tenemos creado el grafo con todos los nodos     
		// 		 	hay que añadir todas las conexiones entre nodos		
		/////////////////////////////////////////////////////////////////////////////
				
							// ADD  the RxE and TxE Links //
		int numNodes = numNode;
		MaxNodesOptical = numNode; //max number of nodes in the aux graph
		int max;
				
		//primer nodo de la capa de acceso (input)
		Integer NodeAccesLayer_input = ((MaxNodesOptical/(numLambdas + 2))*(numLambdas + 1));
		Object IP_NodeAccesLayer_input;
		IP_NodeAccesLayer_input = info_IntNodes_IPv4.get(NodeAccesLayer_input);
				
		//primer nodo de la capa de acceso (output)
		Integer NodeAccesLayer_output = NodeAccesLayer_input + 1;
		Object IP_NodeAccesLayer_output;
		IP_NodeAccesLayer_output = info_IntNodes_IPv4.get(NodeAccesLayer_output);
				
		max = NodeAccesLayer_input;  // numero del primer nodo de capa de acceso IP
		int node_actual_OP = 0;  // empezamos por el nodo 1 del grafo auxiliar
		Object IP_node_actual_OP = info_IntNodes_IPv4.get(node_actual_OP);
				
		int ligthPath_layer = ((MaxNodesOptical/(numLambdas + 2))*(numLambdas));
				
		while (node_actual_OP < ligthPath_layer) //hasta que lleguemos a la capa de LSPs
		{
			if (NodeAccesLayer_input > (MaxNodesOptical-1)) // llegamos al ultimo nodo capa de acceso
			{
				NodeAccesLayer_input = ((MaxNodesOptical/(numLambdas + 2))*(numLambdas + 1));
				IP_NodeAccesLayer_input = info_IntNodes_IPv4.get(NodeAccesLayer_input);
				
				NodeAccesLayer_output = NodeAccesLayer_input + 1;
				IP_NodeAccesLayer_output = info_IntNodes_IPv4.get(NodeAccesLayer_output);
			}
			IntraDomainEdge link1=new IntraDomainEdge();
			try{
				//add the RxE
				aux_graph.addEdge(IP_node_actual_OP, IP_NodeAccesLayer_input); 
				link1 = aux_graph.getEdge(IP_node_actual_OP, IP_NodeAccesLayer_input);
				aux_graph.setEdgeWeight(link1, peso_RxE);
			}catch(Exception e){
				e.printStackTrace();
			}
			
			//TE_Information tE_info=link.getTE_info().set;
			//link.setTE_info(tE_info)
			
			node_actual_OP++;
			IP_node_actual_OP = info_IntNodes_IPv4.get(node_actual_OP);
			
			//add the TxE
			IntraDomainEdge link2=new IntraDomainEdge();
			aux_graph.addEdge(IP_NodeAccesLayer_output, IP_node_actual_OP); 
			link2 = aux_graph.getEdge(IP_NodeAccesLayer_output, IP_node_actual_OP);
			aux_graph.setEdgeWeight(link2, peso_TxE);
			
			node_actual_OP = node_actual_OP + 1;
			IP_node_actual_OP = info_IntNodes_IPv4.get(node_actual_OP);
			NodeAccesLayer_input=NodeAccesLayer_input+2;
			IP_NodeAccesLayer_input = info_IntNodes_IPv4.get(NodeAccesLayer_input);
			NodeAccesLayer_output= NodeAccesLayer_output + 2;
			IP_NodeAccesLayer_output = info_IntNodes_IPv4.get(NodeAccesLayer_output);
		} 						// RxE and TxE Links CREATED
		///////////////////////////////////////////////////////////////////////////////
				
							   //add the WBE and GrmE Links //
		int node_actual_input = 0, node_actual_output = 1;
		Object IP_node_actual_input = info_IntNodes_IPv4.get(node_actual_input);
		Object IP_node_actual_output = info_IntNodes_IPv4.get(node_actual_output);
		
		int number_nodes_layer = MaxNodesOptical/(numLambdas + 2);
						
		while ((node_actual_input<=(MaxNodesOptical - 1)) && (node_actual_output<=MaxNodesOptical))
		{
			if ((node_actual_input / number_nodes_layer) == (numLambdas)){ // LigthPath Layer
				node_actual_input = node_actual_input + number_nodes_layer;
				node_actual_output = node_actual_output + number_nodes_layer;
				IP_node_actual_input = info_IntNodes_IPv4.get(node_actual_input);
				IP_node_actual_output = info_IntNodes_IPv4.get(node_actual_output);
				continue;
			}
						
			if 	(node_actual_input<max){
				IntraDomainEdge link3=new IntraDomainEdge();			
				aux_graph.addEdge(IP_node_actual_input, IP_node_actual_output); //add the WBE
				link3 = aux_graph.getEdge(IP_node_actual_input, IP_node_actual_output);
				aux_graph.setEdgeWeight(link3, peso_WBE);
			}
			
			else if (node_actual_input >= max)  // max =>> First node input access layer
			{
				aux_graph.addEdge(IP_node_actual_input, IP_node_actual_output); //add the GrmE
				IntraDomainEdge link4=new IntraDomainEdge();
				link4 = aux_graph.getEdge(IP_node_actual_input, IP_node_actual_output);
				aux_graph.setEdgeWeight(link4, peso_GrmE);
			}
			node_actual_input = node_actual_input + 2;
			node_actual_output = node_actual_output + 2;
			IP_node_actual_input = info_IntNodes_IPv4.get(node_actual_input);
			IP_node_actual_output = info_IntNodes_IPv4.get(node_actual_output);
		} 							// WBE and GrmE links CREATED //
		///////////////////////////////////////////////////////////////////
		
										// ADD the WLE Links //
		InfoNodo info3=new InfoNodo();
		int node_src= 0;
		int node_dst= 0;
		Set<IntraDomainEdge> edges = baseSimplegraph.edgeSet();
			
		Object Src_IP = null;
		Object Dst_IP = null;
		
		Iterator<IntraDomainEdge> iterEdges = edges.iterator();
			
		while (iterEdges.hasNext())
		{
			IntraDomainEdge Edge = iterEdges.next();
			
			Object Src = Edge.getSource();   // Nodo Real origen del lINK
			Object Dst = Edge.getTarget();   // Nodo Real destino del LINK
			
			node_src = info_IPv4_IntNodes.get(relation_access_nodes.get(Src));
			node_dst = info_IPv4_IntNodes.get(relation_access_nodes.get(Dst));
			
			node_src = node_src - (number_nodes_layer*(numLambdas + 1)) + 1;
			node_dst = node_dst - (number_nodes_layer*(numLambdas + 1));

			
			for (layer = 1; layer<=numLambdas ; layer++)
			{
				Src_IP = info_IntNodes_IPv4.get(node_src);
				Dst_IP = info_IntNodes_IPv4.get(node_dst);
				
				IntraDomainEdge link5=new IntraDomainEdge();
										
				aux_graph.addEdge(Src_IP, Dst_IP); //add the WLE link
				link5 = aux_graph.getEdge(Src_IP, Dst_IP);
				aux_graph.setEdgeWeight(link5, peso_WLE);
				
				node_src = node_src + number_nodes_layer;
				node_dst = node_dst + number_nodes_layer;
			}
		}							// WLE Links CREATED //
		///////////////////////////////////////////////////////////////////
							   // ADD the DmxE and MuxE Links //
		
		// layer --> num_lambdas + 1
		
		int actual_LP_input = ((MaxNodesOptical/(numLambdas+2))*(numLambdas));
		Object IP_actual_LP_input;
		IP_actual_LP_input = info_IntNodes_IPv4.get(actual_LP_input);
		
		int actual_LP_output = actual_LP_input + 1;
		Object IP_actual_LP_output;
		IP_actual_LP_output = info_IntNodes_IPv4.get(actual_LP_output);
		
		//primer nodo de la capa de acceso (output)
		int Node_AccesLayer_input = ((MaxNodesOptical/(numLambdas + 2))*(numLambdas + 1));
		Object IP_Node_AccesLayer_input = info_IntNodes_IPv4.get(Node_AccesLayer_input);
		int Node_AccesLayer_output = Node_AccesLayer_input + 1;
		Object IP_Node_AccesLayer_output;
		IP_Node_AccesLayer_output = info_IntNodes_IPv4.get(Node_AccesLayer_output);
		
		while (Node_AccesLayer_output <= MaxNodesOptical)
		{
			IntraDomainEdge link6=new IntraDomainEdge();
			aux_graph.addEdge(IP_actual_LP_input, IP_Node_AccesLayer_input);
			link6 = aux_graph.getEdge(IP_actual_LP_input, IP_Node_AccesLayer_input);
			aux_graph.setEdgeWeight(link6, peso_DmxE);
			
			IntraDomainEdge link7=new IntraDomainEdge();
			aux_graph.addEdge(IP_Node_AccesLayer_output, IP_actual_LP_output);
			link7 = aux_graph.getEdge(IP_Node_AccesLayer_output, IP_actual_LP_output);
			aux_graph.setEdgeWeight(link7, peso_MuxE);
			
			actual_LP_input = actual_LP_input + 2;
			actual_LP_output = actual_LP_output + 2;
			Node_AccesLayer_input = Node_AccesLayer_input + 2;
			Node_AccesLayer_output = Node_AccesLayer_output + 2;
			
			IP_actual_LP_input = info_IntNodes_IPv4.get(actual_LP_input);
			IP_actual_LP_output = info_IntNodes_IPv4.get(actual_LP_output);
			IP_Node_AccesLayer_input = info_IntNodes_IPv4.get(Node_AccesLayer_input);
			IP_Node_AccesLayer_output = info_IntNodes_IPv4.get(Node_AccesLayer_output);
		}
		
						
		((MultiLayerTEDB)ted).setUpperLayerGraph(aux_graph);
		baseSimplegraph=((MultiLayerTEDB)ted).getUpperLayerGraph();
		aux_graph = ((MultiLayerTEDB)ted).getUpperLayerGraph();
		
		// DmuxE and MuxE Links CREATED //
		
		/////////////////////////////////////////////////////////////////////
		// AUXILIARY GRAPH --> CREATED !!
		/////////////////////////////////////////////////////////////////////
	}

	@Override
	public void setTEDB(TEDB ted) {
		try {
			baseSimplegraph=((MultiLayerTEDB)ted).getUpperLayerGraph();
			Opticalgraph=((MultiLayerTEDB)ted).getDuplicatedLowerLayerkGraph();
			Interlayer = ((MultiLayerTEDB)ted).getDuplicatedInterLayerGraph();
			log.info("Using SimpleTEDB");	
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
		WSONInfo=((DomainTEDB)ted).getWSONinfo();
		this.ted=(DomainTEDB)ted;
		this.numLambdas=WSONInfo.getNumLambdas();
	}
	@Override
	public void setGrooming_policie(int groomingPolicie) {
		grooming_policie = groomingPolicie;
	}
	
	public SimpleDirectedWeightedGraph<Object, IntraDomainEdge> getbaseSimplegraph() {
		return baseSimplegraph;
	}
	
	public Lock getGraphLock() {
		return graphLock;
	}

	public void setGraphLock(Lock graphLock) {
		this.graphLock = graphLock;
	}

	public WSONInformation getWSONInfo() {
		return WSONInfo;
	}

	public void setWSONInfo(WSONInformation wSONInfo) {
		WSONInfo = wSONInfo;
	}

	@Override
	public void notifyNewEdge(Object source, Object destination) {
	}

	@Override
	public void notifyNewVertex(Object vertex) {
	}

	@Override
	public void notifyTEDBFullUpdate() {		
	}

	@Override
	public void notifyWavelengthEndReservation(
			LinkedList<Object> sourceVertexList,
			LinkedList<Object> targetVertexList, int wavelength) {		
	}

	@Override
	public void notifyWavelengthReservation(
			LinkedList<Object> sourceVertexList,
			LinkedList<Object> targetVertexList, int wavelength) {
	}

	@Override
	public void notifyWavelengthStatusChange(Object source,
			Object destination, BitmapLabelSet previousBitmapLabelSet,
			BitmapLabelSet newBitmapLabelSet) {
	}

	@Override
	public void notifyNewEdgeIP(Object source, Object destination, TE_Information informationTEDB) {
	}

	@Override
	public void notificationEdgeIP_AuxGraph(Object src, Object dst, TE_Information informationTEDB) {
		int number_nodes_layer = MaxNodesOptical/(numLambdas + 2);
		Object src_aux = null;
		Object dst_aux = null;
		
		src_aux = relation_access_nodes.get(src);
		dst_aux = relation_access_nodes.get(dst);
		
		Integer src_aux_id, dst_aux_id;
		src_aux_id = info_IPv4_IntNodes.get(src_aux);
		dst_aux_id = info_IPv4_IntNodes.get(dst_aux);
		
		src_aux_id = src_aux_id - number_nodes_layer + 1;
		dst_aux_id = dst_aux_id - number_nodes_layer;
		
		src_aux = info_IntNodes_IPv4.get(src_aux_id);
		dst_aux = info_IntNodes_IPv4.get(dst_aux_id);
		
		graphLock.lock();
		try{
				
			IntraDomainEdge edge = null;
			edge = baseSimplegraph.getEdge(src_aux, dst_aux);
			if (edge==null){
				edge = new IntraDomainEdge();
				//add the new link (Light Path Edge)
				
				edge.setTE_info(informationTEDB);
				baseSimplegraph.addEdge(src_aux, dst_aux, edge);
				edge = baseSimplegraph.getEdge(src_aux, dst_aux);
				baseSimplegraph.setEdgeWeight(edge, peso_LPE);
			}
			else{
				//the LPE already exist
				log.info("El light Path ya existe --> actualizamos su BW");
				edge.setTE_info(informationTEDB);
			}
		}finally{
			graphLock.unlock();	
		}
	}

	@Override
	public void notificationEdgeOPTICAL_AuxGraph(Object src, Object dst, int lambda) {
		//Para verlo en el puerto de mantenimiento
		Opticalgraph.getEdge(src, dst).getTE_info().setWavelengthOccupied(lambda);
		
		//tengo que saber la correspondencia de nodos!
		int number_nodes_layer = MaxNodesOptical/(numLambdas + 2);
		
		Object src_IP = Interlayer_relation_IP.get(src);
		Object dst_IP = Interlayer_relation_IP.get(dst);
		
		Object src_aux_relation = relation_access_nodes.get(src_IP);
		Object dst_aux_relation = relation_access_nodes.get(dst_IP);
		
		Integer src_aux_id = info_IPv4_IntNodes.get(src_aux_relation);
		Integer dst_aux_id = info_IPv4_IntNodes.get(dst_aux_relation);
		
		src_aux_id = (src_aux_id - number_nodes_layer*((numLambdas + 1)-(lambda))) + 1;
		dst_aux_id = dst_aux_id - number_nodes_layer*((numLambdas + 1)-(lambda));
		
		Object src_aux = info_IntNodes_IPv4.get(src_aux_id);
		Object dst_aux = info_IntNodes_IPv4.get(dst_aux_id);
		
		graphLock.lock();
		try{
			IntraDomainEdge edge = null;
			edge = baseSimplegraph.getEdge(src_aux, dst_aux);
			if (edge==null){
				//add the WLE 
				baseSimplegraph.addEdge(src_aux, dst_aux);
				edge = baseSimplegraph.getEdge(src_aux, dst_aux);
				baseSimplegraph.setEdgeWeight(edge, peso_WLE);	
			}
			else{
				//delete the WLE
				baseSimplegraph.removeEdge(edge);	
			}
		}finally{
			graphLock.unlock();	
		}
	}

	@Override
	public ArrayList<SimpleDirectedWeightedGraph<Object, IntraDomainEdge>> getNetworkGraphs() {
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
	public void setMultifiber(boolean multifiber) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setNetworkMultiGraphs(
			ArrayList<DirectedMultigraph<Object, IntraDomainEdge>> networkMultiGraphs) {
		// TODO Auto-generated method stub
		
	}
}