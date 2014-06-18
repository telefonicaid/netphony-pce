package tid.pce.computingEngine.algorithms.multiLayer;

import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.logging.Logger;

import org.jgrapht.GraphPath;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;

import tid.pce.tedb.IntraDomainEdge;

public class Operacion2 {
	
	public Operacion2(){
		
	}	
	private static int number_hops;
	
	public static int getNumber_hops() {
		return number_hops;
	}

	private static Logger log=Logger.getLogger("PCEServer");
	
	public static GraphPath<Object,IntraDomainEdge> get_op2 
	(SimpleDirectedWeightedGraph<Object, IntraDomainEdge> Graph, 
			Object src, Object dst, float bwt_req, Lock graphLock){
		
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
			/*System.out.println("PINTAMOS LA TOPOLOGía IP con CAPACIDAD SUFICIENTE");
			Set<IntraDomainEdge> links = constrained_graph.edgeSet();
			Iterator<IntraDomainEdge> iteredges = links.iterator();
			IntraDomainEdge link;
			while (iteredges.hasNext())
			{
				link = iteredges.next();  //IP of the current node
				System.out.println(link.toString()+"  BW -->"+link.getTE_info().getUnreservedBandwidth().getUnreservedBandwidth()[0]);
			}
			System.out.println("TERMINAMOS DE PINTAR");*/
			DijkstraShortestPath<Object,IntraDomainEdge>  dsp=new DijkstraShortestPath<Object,IntraDomainEdge> (constrained_graph, src, dst);
			gp=dsp.getPath();
			
		}finally{
			graphLock.unlock();
		}
		
		return gp;
	}
}
