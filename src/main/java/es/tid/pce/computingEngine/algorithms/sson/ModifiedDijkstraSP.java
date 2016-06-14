/* ==========================================
 * JGraphT : a free Java graph-theory library
 * ==========================================
 *
 * Project Info:  http://jgrapht.sourceforge.net/
 * Project Creator:  Barak Naveh (http://sourceforge.net/users/barak_naveh)
 *
 * (C) Copyright 2003-2008, by Barak Naveh and Contributors.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 */
/* -------------------------
 * DijkstraShortestPath.java
 * -------------------------
 * (C) Copyright 2003-2008, by John V. Sichi and Contributors.
 *
 * Original Author:  John V. Sichi
 * Contributor(s):   Christian Hammer
 *
 * $Id: DijkstraShortestPath.java 689 2009-07-04 06:40:29Z perfecthash $
 *
 * Changes
 * -------
 * 02-Sep-2003 : Initial revision (JVS);
 * 29-May-2005 : Make non-static and add radius support (JVS);
 * 07-Jun-2005 : Made generic (CH);
 *
 */
package es.tid.pce.computingEngine.algorithms.sson;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.jgrapht.GraphPath;
import org.jgrapht.Graphs;
import org.jgrapht.graph.GraphPathImpl;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;

import es.tid.ospf.ospfv2.lsa.tlv.subtlv.complexFields.BitmapLabelSet;
import es.tid.tedb.IntraDomainEdge;

/**
 * An implementation of <a
 * href="http://mathworld.wolfram.com/DijkstrasAlgorithm.html">Dijkstra's
 * shortest path algorithm</a> using <code>ClosestFirstIterator</code>.
 *
 * @author John V. Sichi
 * @since Sep 2, 2003
 */

public class ModifiedDijkstraSP
{
    //~ Instance fields --------------------------------------------------------

	private Logger log=LoggerFactory.getLogger("PCEServer");
	
    private GraphPath<Object,IntraDomainEdge> path;
    
    private Hashtable<Object, BitmapChannelState> spectrumVertexState;
    
    private static int num_slots;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates and executes a new DijkstraShortestPath algorithm instance. An
     * instance is only good for a single search; after construction, it can be
     * accessed to retrieve information about the path found.
     *
     * @param graph the graph to be searched
     * @param startVertex the vertex at which the path should start
     * @param endVertex the vertex at which the path should end
     * @param SetChannels SetChannels
     * @param num_slots num_slots
     */
    public ModifiedDijkstraSP(SimpleDirectedWeightedGraph<Object,IntraDomainEdge> graph, Object startVertex, Object endVertex, ArrayList<BitmapLabelSet> SetChannels, int num_slots)
    {
        this(graph, startVertex, endVertex, Double.POSITIVE_INFINITY, SetChannels, num_slots);
        this.num_slots=num_slots;
    }


	/**
     * Creates and executes a new DijkstraShortestPath algorithm instance. An
     * instance is only good for a single search; after construction, it can be
     * accessed to retrieve information about the path found.
     *
     * @param graph the graph to be searched
     * @param startVertex the vertex at which the path should start
     * @param endVertex the vertex at which the path should end
     * @param radius limit on path length, or Double.POSITIVE_INFINITY for
     * unbounded search
     * @param SetChannels SetChannels
     * @param num_slots num_slots
     */
    
	public ModifiedDijkstraSP(SimpleDirectedWeightedGraph<Object,IntraDomainEdge> graph,Object startVertex,Object endVertex, double radius, ArrayList<BitmapLabelSet> SetChannels, int num_slots)
    {
        if (!graph.containsVertex(endVertex)) {
            throw new IllegalArgumentException("graph must contain the end vertex");
        }
		ClosestFirstIteratorModified<Object,IntraDomainEdge> iter = new ClosestFirstIteratorModified<Object,IntraDomainEdge> (graph, startVertex, radius, SetChannels, this.num_slots);
       
        while (iter.hasNext()) {
        	Object vertex = iter.next();
        	
            if (vertex.equals(endVertex)) {
                createEdgeList(graph, iter, startVertex, endVertex);
                this.spectrumVertexState=iter.getVertexSpectrumState();
                return;
            }
        }

        path = null;
    }

	//~ Methods ----------------------------------------------------------------

   	/**
     * Return the edges making up the path found.
     *
     * @return List of Edges, or null if no path exists
     */
    public List<IntraDomainEdge> getPathEdgeList()
    {
        if (path == null) {
            return null;
        } else {
            return path.getEdgeList();
        }
    }

    /**
     * Return the path found.
     *
     * @return path representation, or null if no path exists
     */
    public GraphPath<Object,IntraDomainEdge> getPath()
    {
        return path;
    }

    /**
     * Return the length of the path found.
     *
     * @return path length, or Double.POSITIVE_INFINITY if no path exists
     */
    public double getPathLength()
    {
        if (path == null) {
            return Double.POSITIVE_INFINITY;
        } else {
            return path.getWeight();
        }
    }

    /**
     * Convenience method to find the shortest path via a single static method
     * call. If you need a more advanced search (e.g. limited by radius, or
     * computation of the path length), use the constructor instead.
     *
     * @param graph the graph to be searched
     * @param startVertex the vertex at which the path should start
     * @param endVertex the vertex at which the path should end
     *
     * @return List of Edges, or null if no path exists
     */
    public static List<IntraDomainEdge>  findPathBetween(
    	SimpleDirectedWeightedGraph<Object,IntraDomainEdge>  graph,
        Object startVertex,
        Object endVertex)
    {
    	ModifiedDijkstraSP alg = new ModifiedDijkstraSP(graph, startVertex, endVertex, null, num_slots);

        return alg.getPathEdgeList();
    }

    private void createEdgeList(SimpleDirectedWeightedGraph<Object,IntraDomainEdge> graph, ClosestFirstIteratorModified<Object,IntraDomainEdge> iter, Object startVertex, Object endVertex)
    {
        List<IntraDomainEdge> edgeList = new ArrayList<IntraDomainEdge>();
        Object v = endVertex;

        while (true) {
        	IntraDomainEdge edge = iter.getSpanningTreeEdge(v);

            if (edge == null) {
                break;
            }
            
            edgeList.add(edge);
            v = Graphs.getOppositeVertex(graph, edge, v);
          
        }

        Collections.reverse(edgeList);
        double pathLength = iter.getShortestPathLength(endVertex);
        path =
            new GraphPathImpl<Object,IntraDomainEdge>(
                graph,
                startVertex,
                endVertex,
                edgeList,
                pathLength);
    }
    
    public Hashtable<Object, BitmapChannelState> getVertexSpectrumState() {
    	return spectrumVertexState;
    }
    
}

// End DijkstraShortestPath.java
