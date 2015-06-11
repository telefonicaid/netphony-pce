package tid.pce.computingEngine.algorithms;

import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Logger;

import org.jgrapht.graph.DirectedWeightedMultigraph;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;

import es.tid.pce.pcep.objects.ExcludeRouteObject;
import es.tid.pce.pcep.objects.subobjects.UnnumberIfIDXROSubobject;
import es.tid.pce.pcep.objects.subobjects.XROSubObjectValues;
import es.tid.pce.pcep.objects.subobjects.XROSubobject;
import tid.pce.tedb.InterDomainEdge;
import tid.pce.tedb.IntraDomainEdge;
import tid.pce.tedb.ReachabilityManager;

public class GraphFunctions {
	
	 	
	public static void processXRO(ExcludeRouteObject xro, SimpleDirectedWeightedGraph<Object, IntraDomainEdge> graphLambda){
		Logger log=Logger.getLogger("PCEServer");
		try{
		if (xro!=null){
			log.info("XRO: Thre are "+xro.getEROSubobjectList().size()+" exclusions");
			for (int i=0;i<xro.getEROSubobjectList().size();++i){
				XROSubobject eroso=xro.getEROSubobjectList().get(i);
				if (eroso.getType()==XROSubObjectValues.XRO_SUBOBJECT_UNNUMBERED_IF_ID){					
					UnnumberIfIDXROSubobject eros=(UnnumberIfIDXROSubobject)eroso;					
					log.info("XRO: UNNUMBERED_IF_ID TO EXCLUDE: "+eros.getRouterID()+":"+eros.getInterfaceID());
					//Inet4Address source_domain_id=reachabilityManager.getDomain(eros.getRouterID());
					Inet4Address source_node_ip= eros.getRouterID();
					log.info("XRO: IT BELONGS TO NODE "+source_node_ip);
					boolean hasVertex=graphLambda.containsVertex(source_node_ip);
					log.info("XRO: EL GRAPH TIENE VERTEX "+hasVertex);
					
					if (hasVertex){
						
						log.info("%% 1: graphLambda:: "+graphLambda.toString());
						graphLambda.removeVertex(source_node_ip);
						log.info("%% 2: graphLambda:: "+graphLambda.toString());
						
//						Set<IntraDomainEdge> setEdges=graphLambda.edgesOf(source_node_ip);
//						log.info("%% setEdges:: "+ setEdges.toString());
//										
//						Iterator<IntraDomainEdge> iter=setEdges.iterator();
//										
//						while (iter.hasNext()){
//							IntraDomainEdge edge=iter.next();
//							log.info("XRO: LOOKING AT EDGE "+edge.getSrc_if_id());
//							if (edge.getSrc_if_id()==eros.getInterfaceID()){
//								if (edge.getSrc_if_id()==eros.getInterfaceID()){
//								graphLambda.removeEdge(edge);				
//								log.info("XRO: REMOVE EDGE "+eros.getRouterID()+":"+edge.getSrc_if_id());
//								}
//							}
//						}
						
					}
				}
			}
		}
		}catch (Exception e){
			e.printStackTrace();
		}
		
	}
}

