package tid.pce.computingEngine.algorithms;

import java.util.concurrent.Callable;

import tid.pce.computingEngine.ComputingResponse;

public interface ComputingAlgorithm extends Callable<ComputingResponse> {

	public AlgorithmReservation getReserv();
	
//	public boolean run();
//	
//	public Hashtable<Integer, LinkedList<LinkedList<Inet4Address>>> getPaths();
//	
//	public Hashtable<LinkedList<LinkedList<Inet4Address>>, LinkedList<Long>> getIdSrcTable();
//	
//	public void setIdSrcTable(Hashtable<LinkedList<LinkedList<Inet4Address>>, LinkedList<Long>> idSrcTable);
//	
//	public Hashtable<LinkedList<LinkedList<Inet4Address>>, LinkedList<Long>> getIdDstTable();
//	
//	public void setIdDstTable(Hashtable<LinkedList<LinkedList<Inet4Address>>, LinkedList<Long>> idDstTable);
	
}
