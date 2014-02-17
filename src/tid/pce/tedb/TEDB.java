package tid.pce.tedb;

import java.net.Inet4Address;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;

import org.jgrapht.*;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.graph.*;

/**
 * Base de datos de ingenieria de trafico
 * CLASE DE PRUEBA REESTRUCTURAR DESPUES!!!!!!!!!!
 * @author ogondio
 *
 */
public interface TEDB {
//	//private long graphId;
//	//private SimpleDirectedWeightedGraph<Inet4Address,IntraDomainEdge> networkGraph;
//	//public SimpleDirectedWeightedGraph<Integer,IntraDomainEdge> grafo;
//	
//	public SimpleDirectedWeightedGraph<Inet4Address, IntraDomainEdge> getDuplicatedNetworkGraph();
//	
//	public DirectedWeightedMultigraph<Inet4Address, InterDomainEdge> getDuplicatedMDNetworkGraph();
//
//	
	public void initializeFromFile(String file);

	public boolean isITtedb();
	
	public String printTopology();

	public LinkedList<InterDomainEdge> getInterDomainLinks();
	
//	{
//		networkGraph=FileTEDBUpdater.readNetwork(file);		
//	}
	
//	public long getGraphId();
////	{
////		return graphId;
////	}
//	public void setGraphId(long graphId);
////	{
////		this.graphId = graphId;
////	}
//	
//	public boolean belongsToDomain(Inet4Address addr);
//	
	
	

}
