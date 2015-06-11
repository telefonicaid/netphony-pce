package tid.pce.computingEngine.algorithms.utilities;

import java.util.List;
import java.util.logging.Logger;

import org.jgrapht.GraphPath;

import es.tid.tedb.IntraDomainEdge;

/**
 * Esta clase compara dos caminos para ver si son iguales.
 * Recibe dos GraphPaths GP1, GP2
 * y comprueba si el tamaño de los caminos es igual, si se cumple, recorre interativamente el nodo destino de cada enlace
 * para comprobar si son iguales, hasta llegar al final del camino.
 * 
 * @autor: arturo mayoral
 * 
 * **/

public class graphs_comparator {
	
	boolean is_equal;
	
	private Logger log=Logger.getLogger("PCEServer");
	
	public graphs_comparator (){
		this.is_equal = false;
	}

	public boolean edges_comparator (GraphPath<Object,IntraDomainEdge> gp1, GraphPath<Object,IntraDomainEdge> gp2){
		
		List <IntraDomainEdge> edgeList1;
		List <IntraDomainEdge> edgeList2;
		
		edgeList1 = gp1.getEdgeList();
		edgeList2 = gp2.getEdgeList();
		
		if (edgeList1.isEmpty()== true){
			is_equal = false;
			return is_equal;
		}
		
		if (edgeList2.isEmpty()== true){
			is_equal = false;
			return is_equal;
		}
		
		if (edgeList1.size() != edgeList2.size()){
			is_equal = false;
		}else{
			is_equal=true;
			for(int i=0;i<edgeList1.size();i++){
				if (edgeList1.get(i).getTarget().equals(edgeList2.get(i).getTarget()) == false){
					is_equal = false;
					return is_equal;
				}
			}
			
		}
		return is_equal;
	}
	
}
