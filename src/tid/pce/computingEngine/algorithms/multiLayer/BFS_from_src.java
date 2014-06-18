package tid.pce.computingEngine.algorithms.multiLayer;

import java.net.Inet4Address;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

import org.jgrapht.GraphPath;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;

import tid.pce.tedb.IntraDomainEdge;

public class BFS_from_src {
		
	public BFS_from_src(){
		
	}
	private int lambda_chosen;
	private int NumberHops;
	
	public int getNumberHops() {
		return NumberHops;
	}

	public void setNumberHops(int numberHops) {
		NumberHops = numberHops;
	}
	
	

	public int getLambda_chosen() {
		return lambda_chosen;
	}

	public void setLambda_chosen(int lambdaChosen) {
		lambda_chosen = lambdaChosen;
	}

	GraphPath<Object,IntraDomainEdge> getBFS(SimpleDirectedWeightedGraph<Inet4Address,IntraDomainEdge> IPGraph,
			SimpleDirectedWeightedGraph<Object,IntraDomainEdge> InterlayerGraph,
			ArrayList<SimpleDirectedWeightedGraph<Object, IntraDomainEdge>> networkGraphs,
			Inet4Address node_src, int max_Hops, Inet4Address node_dst, 
			int NumLambdas){
		
								// RECORRIDO EN ANCHURA // 
		
		// recorremos todos los vértices del grafo inicializándolos a NO_VISITADO,
	    // distancia INFINITA y padre de cada nodo NULL
		
		if (max_Hops == 0){  // no habia camino MultiHop LigthPath
			max_Hops = 2147483647;
		}
		
		Set<Inet4Address> nodes= IPGraph.vertexSet();
		Iterator<Inet4Address> iter;
		
		iter=nodes.iterator();
		
		Hashtable<Inet4Address,Integer> estado = new Hashtable<Inet4Address,Integer>();
		Hashtable<Inet4Address,Integer> distancia = new Hashtable<Inet4Address,Integer>();
		Hashtable<Inet4Address,Inet4Address> padre = new Hashtable<Inet4Address,Inet4Address>();
		
		int number_Hops=0;
		
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
			
			// look for the adjacency nodes
			Set<IntraDomainEdge> edges = IPGraph.outgoingEdgesOf(actual_node);
			
			Iterator<IntraDomainEdge> iter2;
			
			iter2 = edges.iterator();
			
			while (iter2.hasNext()){
				IntraDomainEdge fiberEdge =iter2.next();
				Inet4Address node = (Inet4Address)fiberEdge.getTarget();
				
				if (estado.get(node) == 0){
					estado.put(node, 1);
					number_Hops = distancia.get(actual_node)+1; //nivel del arbol en el que estamos,
					// que equivale a la distancia al nodo orgigen (padre del árbol)
					
					distancia.put(node, (distancia.get(actual_node)+1));
					padre.put(node, actual_node);
					cola.add(node);
					
					if (number_Hops < (max_Hops - 1)){  // nodo valido para buscar un camino
						
						setNumberHops(number_Hops);
						
						Operacion3 op3 = new Operacion3(networkGraphs);
						GraphPath<Object,IntraDomainEdge> gp3 = op3.get_op3(node,
								node_dst, InterlayerGraph, NumLambdas);
						
						if (gp3 != null){  // PATH FOUND
							lambda_chosen = op3.getLambda_chosen();
							return gp3;
						}
					}
				}
			}
		}
		
		return null; // no path 
	}
}