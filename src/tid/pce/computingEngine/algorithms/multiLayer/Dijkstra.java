package tid.pce.computingEngine.algorithms.multiLayer;

import java.util.*;

import org.jgrapht.*;
import org.jgrapht.graph.*;
import org.jgrapht.traverse.*;
 
 
/**
 * An implementation of <a
 * href="http://mathworld.wolfram.com/DijkstrasAlgorithm.html">Dijkstra's
 * shortest path algorithm</a> using <code>ClosestFirstIterator</code>.
 *
 * @author John V. Sichi
 * @since Sep 2, 2003
 */
public final class Dijkstra<V, E>
{
    //~ Instance fields --------------------------------------------------------
 
    private GraphPath<V, E> path;
 
    //~ Constructors -----------------------------------------------------------
 
    /**
     * Creates and executes a new DijkstraShortestPath algorithm instance. An
     * instance is only good for a single search; after construction, it can be
     * accessed to retrieve information about the path found.
     *
     * @param graph the graph to be searched
     * @param startVertex the vertex at which the path should start
     * @param endVertex the vertex at which the path should end
     */
    public Dijkstra(Graph<V, E> graph,
        V startVertex,
        V endVertex)
    {
        this(graph, startVertex, endVertex, Double.POSITIVE_INFINITY);
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
     */
    public Dijkstra(
        Graph<V, E> graph,
        V startVertex,
        V endVertex,
        double radius)
    {
        if (!graph.containsVertex(endVertex)) {
            throw new IllegalArgumentException(
                "graph must contain the end vertex");
        }
 
        ClosestFirstIterator<V, E> iter =
            new ClosestFirstIterator<V, E>(graph, startVertex, radius);
 
        while (iter.hasNext()) {
            V vertex = iter.next();
 
            if (vertex.equals(endVertex)) {
                createEdgeList(graph, iter, startVertex, endVertex);
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
    public List<E> getPathEdgeList()
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
    public GraphPath<V, E> getPath()
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
    public static <V, E> List<E> findPathBetween(
        Graph<V, E> graph,
        V startVertex,
        V endVertex)
    {
        Dijkstra<V, E> alg =
            new Dijkstra<V, E>(
                graph,
                startVertex,
                endVertex);
 
        return alg.getPathEdgeList();
    }
 
    private void createEdgeList(
        Graph<V, E> graph,
        ClosestFirstIterator<V, E> iter,
        V startVertex,
        V endVertex)
    {
        List<E> edgeList = new ArrayList<E>();
 
        V v = endVertex;
 
        while (true) {
            E edge = iter.getSpanningTreeEdge(v);
 
            if (edge == null) {
                break;
            }
 
            edgeList.add(edge);
            v = Graphs.getOppositeVertex(graph, edge, v);
        }
 
        Collections.reverse(edgeList);
        double pathLength = iter.getShortestPathLength(endVertex);
        path =
            new GraphPathImpl<V, E>(
                graph,
                startVertex,
                endVertex,
                edgeList,
                pathLength);
    }
}
 
// End DijkstraShortestPath.java