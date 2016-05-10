package es.tid.pce.computingEngine.algorithms.vlan;

import es.tid.pce.computingEngine.ComputingRequest;
import es.tid.pce.computingEngine.algorithms.ComputingAlgorithm;
import es.tid.pce.computingEngine.algorithms.ComputingAlgorithmManager;
import es.tid.pce.computingEngine.algorithms.ComputingAlgorithmPreComputation;
import es.tid.pce.computingEngine.algorithms.multiLayer.OperationsCounter;
import es.tid.pce.server.wson.ReservationManager;
import es.tid.tedb.DomainTEDB;
import es.tid.tedb.TEDB;

import java.net.Inet4Address;
import java.util.Hashtable;

/**
 * 
 * @author jaume
 * Si lees esto, hazlo con valors, pero sobre todo , con humiltat.
 */
public class VLAN_Multicast_algorithmManager implements ComputingAlgorithmManager 
{

	VLAN_Multicast_algorithmPreComputation preComp;
	
	private ReservationManager reservationManager;
	
	@Override
	public ComputingAlgorithm getComputingAlgorithm(
			ComputingRequest pathReq, TEDB ted) 
	{
		VLAN_Multicast_algorithm algo = new VLAN_Multicast_algorithm(pathReq, ted,reservationManager);
		algo.setPreComp(preComp);
		
		return algo;
	}

	@Override
	public void setReservationManager(ReservationManager reservationManager) 
	{
		this.reservationManager=reservationManager;
		
	}

	@Override
	public void setPreComputation(ComputingAlgorithmPreComputation pc) 
	{
		// TODO Auto-generated method stub
		this.preComp=(VLAN_Multicast_algorithmPreComputation) pc;		
	
	}

	@Override
	public ComputingAlgorithm getComputingAlgorithm(ComputingRequest pathReq,
			TEDB ted, OperationsCounter OPcounter)
	{
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public ComputingAlgorithm getComputingAlgorithm(ComputingRequest pathReq,
													TEDB ted, Hashtable<Inet4Address,DomainTEDB> intraTEDBs) {
		// TODO Auto-generated method stub
		return null;
	}
}
