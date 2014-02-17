package tid.pce.computingEngine.algorithms.multiLayer;

import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.locks.Lock;

import org.jgrapht.GraphPath;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;
import tid.pce.tedb.IntraDomainEdge;

public class Operacion1 {
	
	public Operacion1(){
		
	}
	public static GraphPath<Object,IntraDomainEdge> get_op1 (SimpleDirectedWeightedGraph<Object
			, IntraDomainEdge> Graph, Object src, Object dst, Lock graphLock, float bwt_req){
		
		SimpleDirectedWeightedGraph<Object,IntraDomainEdge> constrained_graph = new SimpleDirectedWeightedGraph<Object,IntraDomainEdge>(IntraDomainEdge.class);
		GraphPath<Object,IntraDomainEdge> gp = null;
		graphLock.lock();
		try{
			constrained_graph= (SimpleDirectedWeightedGraph<Object, IntraDomainEdge>) Graph.clone();
				
			Set<IntraDomainEdge> links1 = Graph.edgeSet();
			Iterator<IntraDomainEdge> iteredges1 = links1.iterator();
			IntraDomainEdge link1;
					
			while (iteredges1.hasNext())
			{
				link1 = iteredges1.next();  //IP of the current node
				// borramos los links que no tengan suficiente ancho de banda
				
				if ((link1.getTE_info().getMaximumReservableBandwidth().maximumReservableBandwidth)<bwt_req){
					constrained_graph.removeEdge(link1);
				}
			}
			DijkstraShortestPath<Object,IntraDomainEdge>  dsp=new DijkstraShortestPath<Object,IntraDomainEdge> (constrained_graph, src, dst);
			gp=dsp.getPath();
		} finally{
			graphLock.unlock();
		}
		
		return gp;
	}
}
