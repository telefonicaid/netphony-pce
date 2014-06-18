package tid.pce.computingEngine.algorithms.multiLayer;

import java.net.Inet4Address;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

import org.jgrapht.graph.SimpleDirectedWeightedGraph;

import tid.pce.tedb.IntraDomainEdge;

public class BFS_ShortestPath {
		
	public BFS_ShortestPath(){
		
	}
	
	public static int getBFS_ShortestPath(SimpleDirectedWeightedGraph<Inet4Address,IntraDomainEdge> IPGraph,
			Inet4Address node_src, Inet4Address node_src_BFS, int number_Hops_BFS){
		
		// recorremos todos los vértices del grafo inicializándolos a NO_VISITADO,
	    // distancia INFINITA y padre de cada nodo NULL
		
		Set<Inet4Address> nodes= IPGraph.vertexSet();
		Iterator<Inet4Address> iter;
		
		iter=nodes.iterator();
		
		Hashtable<Inet4Address,Integer> estado = new Hashtable<Inet4Address,Integer>();
		Hashtable<Inet4Address,Integer> distancia = new Hashtable<Inet4Address,Integer>();
		Hashtable<Inet4Address,Inet4Address> padre = new Hashtable<Inet4Address,Inet4Address>();
		
		while (iter.hasNext()){
			estado.put(iter.next(), 0);   // 0 --> NO VISITADO
			 							  // 1 --> VISITADO		
			distancia.put(iter.next(), 2147483647); // valor infinito
		    padre.put(iter.next(), null);	    
		}
		
		estado.put(node_src, 1); // nodo origen fijado a VISITADO
		
		distancia.put(node_src, 0);
		
		LinkedList<Inet4Address> cola = new LinkedList<Inet4Address>();
		
		cola.add(node_src); // queue the first node
			
		while (!(cola.isEmpty())){   // while the queue is NOT EMPTY
			Inet4Address actual_node = cola.pollFirst();
			//Vamos sacando nodos de la cola
			//vamos buscando caminos desde cada uno al nodo DESTINATION
			
			// exploramos los nodos adyacentes
			Set<IntraDomainEdge> edges = IPGraph.outgoingEdgesOf(actual_node);
			
			Iterator<IntraDomainEdge> iter2;
			
			iter2 = edges.iterator();
			
			while (iter2.hasNext()){
				IntraDomainEdge fiberEdge =iter2.next();
				Inet4Address node = (Inet4Address)fiberEdge.getTarget();
				
				if (estado.get(node) == 0){
					
					estado.put(node, 1);
					distancia.put(node, (distancia.get(actual_node)+1));
					padre.put(node, actual_node);
					cola.add(node);
					
				}
			}
		}
		
		return 0; //correcto 
		
	}
}