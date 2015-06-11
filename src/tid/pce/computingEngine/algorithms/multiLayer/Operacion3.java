package tid.pce.computingEngine.algorithms.multiLayer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Logger;

import org.jgrapht.GraphPath;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;

import es.tid.tedb.IntraDomainEdge;


public class Operacion3 {
	
	private ArrayList<SimpleDirectedWeightedGraph<Object, IntraDomainEdge>> networkGraphs;
	
	public Operacion3(ArrayList<SimpleDirectedWeightedGraph<Object, IntraDomainEdge>> networkGraphs_precomp){
	
		this.networkGraphs=networkGraphs_precomp;
	}
	
	private Multilayer_MinTH_AlgorithmPreComputation preComp;
	
	private Logger log;
	
	private int lambda_chosen;
	
	public int getLambda_chosen() {
		return lambda_chosen;
	}
	
	private IntraDomainEdge edge_ini;

	public IntraDomainEdge getEdge_ini() {
		return edge_ini;
	}

	private IntraDomainEdge edge_end;

	public IntraDomainEdge getEdge_end() {
		return edge_end;
	}
	
	GraphPath<Object,IntraDomainEdge> get_op3 (Object src, 
			Object dst, SimpleDirectedWeightedGraph<Object, 
			IntraDomainEdge> InterlayerGraph, int NumLambdas){
				
		GraphPath<Object,IntraDomainEdge> gp_chosen = null;
		
		////////////////////////////////////////////////////////////////////////////////////////////
		// NO LigthPath found --> to OPERATION 3 --> create a NEW LIGTH PATH (KSP Optical Graph) //
		// LLAMAR A AL ALGORITMO AURE PARA CALCULAR EL CAMINO EN LA CAPA (LAMBDA) QUE CONVENGA //
		boolean nopath=true;//Initially, we still have no path
		boolean end=false;//The search has not ended yet
			
		// directions IP from Optical graph
		Object src_Optical=null;
		Object dst_Optical=null;
		Iterator<IntraDomainEdge> iter = InterlayerGraph.edgeSet().iterator();
		IntraDomainEdge edge;
		
		while (iter.hasNext()){
			edge = iter.next();
			
			if (edge.getSource().equals(src)){
				src_Optical = edge.getTarget();	
				edge_ini = edge;
			}
			
			else if (edge.getTarget().equals(dst)){
				dst_Optical = edge.getSource();
				edge_end = edge;
			}
		}	
				
		if (src_Optical == null || dst_Optical==null){
			return null;
		}
		
		lambda_chosen = 0;
		double max_metric=Integer.MAX_VALUE;
		int lambda=0;//We begin with lambda index 0
		/*preComp.getGraphLock().lock();*/
		/////// COMPROBAR SI ESTO ES IGUAL QUE EL AURE - EXHAUSTIVE //////////
		
		while (!end){
			
			SimpleDirectedWeightedGraph<Object,IntraDomainEdge> graphLambda= networkGraphs.get(lambda);
			DijkstraShortestPath<Object,IntraDomainEdge>  dsp=new DijkstraShortestPath<Object,IntraDomainEdge> (graphLambda, src_Optical, dst_Optical);
			GraphPath<Object,IntraDomainEdge> gp=dsp.getPath();
				
				
			if (gp==null){ // no PATH for actual LAMBDA
					
				if (lambda==(NumLambdas - 1)){
					if (nopath==true){
						return null;
					}
					else{
						end = true;
					}
				}else {
					lambda=lambda+1;
				}
			}
				
			else { // PATH found for actual LAMBDA
				if (gp.getWeight()<max_metric){   // shortest path FOUND
					gp_chosen=gp;
					lambda_chosen=lambda;
					nopath=false;
					max_metric=gp.getWeight();
				}
				if (lambda==(NumLambdas - 1)){
					end=true;	
				}else {
					lambda=lambda+1;
				}
			}
		}
		
		////////////////////////      FIN OPERACION 3       ///////////////////////
		
		
		return gp_chosen;
		

	}
}
