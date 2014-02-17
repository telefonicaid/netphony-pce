package tid.pce.computingEngine.algorithms;

import java.net.Inet4Address;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

import tid.pce.pcep.messages.PCEPResponse;
import tid.pce.tedb.IntraDomainEdge;

public interface ComputingAlgorithm extends Callable<PCEPResponse> {

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
