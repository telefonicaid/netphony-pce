package tid.pce.computingEngine.algorithms.multiLayer;

import java.net.Inet4Address;
import java.util.ArrayList;
import java.util.logging.Logger;

import org.jgrapht.GraphPath;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;

import es.tid.tedb.IntraDomainEdge;

public class Operacion4 {
	
	public Operacion4(){
		
	}
	
	private static Multilayer_MinTH_AlgorithmPreComputation preComp;
	
	private static int number_hops = 0;
	
	public static int getNumber_hops() {
		return number_hops;
	}
	
	private static Logger log=Logger.getLogger("PCEServer");
	
	GraphPath<Inet4Address,IntraDomainEdge> get_op4 
	(SimpleDirectedWeightedGraph<Inet4Address,IntraDomainEdge> IPGraph, 
		SimpleDirectedWeightedGraph<Inet4Address,IntraDomainEdge> interlayer,
		ArrayList<SimpleDirectedWeightedGraph<Inet4Address, IntraDomainEdge>> networkGraphs, 
		Inet4Address src, Inet4Address dst, int maxHops, GraphPath<Inet4Address,IntraDomainEdge> gp,
		int numLambdas){
				
		/*
		IntraDomainEdge Link;
		Inet4Address node;
		
		if (maxHops > 2){
			List<IntraDomainEdge> edgeList = gp.getEdgeList();
					
			int i;
			for (i = 0; i<(maxHops - 1); i++){
				
				Link = edgeList.get(i);
				node = Link.getTarget();
				
				List<IntraDomainEdge> edge_list;
				edge_list.add(Link);
				
				// llamar a la operacion 3
				GraphPath<Inet4Address,IntraDomainEdge> gp3 = op3.get_op3(node,
						dst, interlayer, networkGraphs, numLambdas);
				
				if (gp3 != null){ 		//camino con menos saltos
					for (int m=0;m<gp3.getEdgeList().size();m++){
						
						edgeList.add(gp3.getEdgeList().get(m));
					}										
				}
			}
		}
		*/
		
		
		
		
		return null;
		
	}

}
