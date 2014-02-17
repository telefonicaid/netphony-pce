package tid.pce.tedb;

import java.util.LinkedList;
import java.util.Set;

import tid.ospf.ospfv2.lsa.tlv.subtlv.complexFields.BitmapLabelSet;
import tid.pce.computingEngine.algorithms.ComputingAlgorithmPreComputation;
import tid.pce.parentPCE.ReachabilityEntry;

public interface WlanTEDB 
{
	//public String getDomainId();
	public ReachabilityEntry getReachabilityEntry();

	//public byte[]  getDomainReachabilityIPv4Prefix();

	public LinkedList<WLANEdge> getInterDomainLinks();
	public Set<WLANEdge> getIntraDomainLinks();
	public String printInterDomainLinks();

	public boolean containsVertex(String vertex);
	public void notifyWLANReservation(LinkedList<String> sourceVertexList, LinkedList<String> targetVertexList, int wavelength, boolean bidirectional);
	public void notifyWLANEndReservation(LinkedList<String> sourceVertexList, LinkedList<String> targetVertexList, int wavelength, boolean bidirectional);
	public void notifyWLANChange(String localInterfaceIPAddress,String remoteInterfaceIPAddress,BitmapLabelSet previousBitmapLabelSet,BitmapLabelSet newBitmapLabelSet );
	public void notifyNewEdgeIP(String source, String destination, TE_Information informationTEDB);
	public void register(ComputingAlgorithmPreComputation compAlgPreComp);
	
	public void notifyNewVertex(String vertex);
	
	public void notifyNewEdge (String source, String destination);
	
	public void clearAllReservations();
	
}
