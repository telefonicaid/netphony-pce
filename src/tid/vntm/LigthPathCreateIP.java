package tid.vntm;

import java.net.Inet4Address;
import java.util.LinkedList;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

import org.jgrapht.graph.SimpleDirectedWeightedGraph;

import es.tid.ospf.ospfv2.lsa.tlv.subtlv.MaximumBandwidth;
import es.tid.ospf.ospfv2.lsa.tlv.subtlv.MaximumReservableBandwidth;
import es.tid.ospf.ospfv2.lsa.tlv.subtlv.UnreservedBandwidth;
import es.tid.rsvp.objects.subobjects.EROSubobject;
import es.tid.rsvp.objects.subobjects.IPv4prefixEROSubobject;
import tid.pce.tedb.DomainTEDB;
import tid.pce.tedb.IntraDomainEdge;
import tid.pce.tedb.MultiLayerTEDB;
import tid.pce.tedb.TE_Information;

public class LigthPathCreateIP {
	
	private DomainTEDB ted;
	/**
	 * Variable usada para bloquear la lectura y escritura en la TEDB
	 */
	private ReentrantLock graphlock;
	
	private int NumLigthPaths = 0;
	
	private Logger log;
			
	public LigthPathCreateIP(DomainTEDB tedb){
		log = Logger.getLogger("PCCClient.log");
		this.ted=(DomainTEDB)tedb;
		graphlock = new ReentrantLock();
	}
	
	public boolean createLigthPath (LinkedList<EROSubobject> ERO){
		SimpleDirectedWeightedGraph<Object, IntraDomainEdge> graph_IP = null;
				
		try {	
			graph_IP = ((MultiLayerTEDB)ted).getUpperLayerGraph();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
				
		Inet4Address src = null;
		Inet4Address dst = null;
				
		src=((IPv4prefixEROSubobject)ERO.getFirst()).getIpv4address();
		dst=((IPv4prefixEROSubobject)ERO.getLast()).getIpv4address();
		
		
		if (graph_IP == null){
			log.warning("Graph IP is Null!");
			return false;
		}
		
		graphlock.lock();
		try{
			float bw = 10;
			float[] bw_un = new float[8];
			bw_un[0] = bw;
			IntraDomainEdge new_edge;
			
			TE_Information TE_INFO = new TE_Information();
			MaximumBandwidth maximumBandwidth = new MaximumBandwidth();
			MaximumReservableBandwidth maximumReservableBandwidth = new MaximumReservableBandwidth();
			UnreservedBandwidth unreservedBandwidth = new UnreservedBandwidth();
			try{			
				if (graph_IP.getEdge(src, dst) == null){
					new_edge=new IntraDomainEdge();
									
					maximumBandwidth.setMaximumBandwidth(bw);
					maximumReservableBandwidth.setMaximumReservableBandwidth(bw);
					unreservedBandwidth.setUnreservedBandwidth(bw_un);
					
					TE_INFO.setMaximumBandwidth(maximumBandwidth);
					TE_INFO.setMaximumReservableBandwidth(maximumReservableBandwidth);
					TE_INFO.setUnreservedBandwidth(unreservedBandwidth);
					
					new_edge.setTE_info(TE_INFO);
					graph_IP.addEdge(src, dst, new_edge);
					//System.out.print(graph_IP.getEdge(src, dst).getTE_info().getMaximumBandwidth().maximumBandwidth);
				}
				else {
					//System.out.println("El edge ya existe --> "+graph_IP.getEdge(src, dst).toString());
					//new_edge = graph_IP.getEdge(src, dst);
					float bw_max = bw;
					float bw_new = bw;
					log.info("Unreserved :"+graph_IP.getEdge(src, dst).getTE_info().getUnreservedBandwidth().getUnreservedBandwidth()[0]);
					bw_un[0] = bw_un[0] + graph_IP.getEdge(src, dst).getTE_info().getUnreservedBandwidth().getUnreservedBandwidth()[0];
					bw_new = bw_new + graph_IP.getEdge(src, dst).getTE_info().getMaximumBandwidth().maximumBandwidth;
					bw_max = bw_max + graph_IP.getEdge(src, dst).getTE_info().getMaximumReservableBandwidth().maximumReservableBandwidth;
					
					maximumBandwidth.setMaximumBandwidth(bw_new);
					maximumReservableBandwidth.setMaximumReservableBandwidth(bw_max);
					unreservedBandwidth.setUnreservedBandwidth(bw_un);
					
					graph_IP.getEdge(src, dst).getTE_info().setMaximumBandwidth(maximumBandwidth);
					graph_IP.getEdge(src, dst).getTE_info().setMaximumReservableBandwidth(maximumReservableBandwidth);
					graph_IP.getEdge(src, dst).getTE_info().setUnreservedBandwidth(unreservedBandwidth);
				}
				((MultiLayerTEDB)ted).setUpperLayerGraph(graph_IP);
			}catch (Exception e){
				e.printStackTrace();
				System.exit(0);
			}
		}finally{
			graphlock.unlock();	
		}
		
		return true;
	}
	
	public boolean deleteLigthPath (Inet4Address source, Inet4Address destination){
		
		//borrar el ligthPath creado pasando el origen y el destino
		SimpleDirectedWeightedGraph<Object,IntraDomainEdge> graph_IP;
		IntraDomainEdge edge=new IntraDomainEdge();
		
		graph_IP = ((MultiLayerTEDB)ted).getDuplicatedUpperLayerkGraph();
		
		graph_IP.removeEdge(source, destination);
		
		((MultiLayerTEDB)ted).setUpperLayerGraph(graph_IP);
		
		return true;
	}
	
	public boolean reserveLigthPath (Inet4Address source, Inet4Address destination, float bw){
		
		//Reservar Bandwidth en el Ligth Path
		SimpleDirectedWeightedGraph<Object,IntraDomainEdge> graph_IP;
		graph_IP = ((MultiLayerTEDB)ted).getDuplicatedUpperLayerkGraph();
		
		IntraDomainEdge edge = new IntraDomainEdge();
		
		edge = graph_IP.getEdge(source, destination);
		
		float bandwidth = edge.getTE_info().getUnreservedBandwidth().getUnreservedBandwidth()[0];
		
		TE_Information tE_info = null;
		UnreservedBandwidth unreservedBandwidth = null;
		tE_info.setUnreservedBandwidth(unreservedBandwidth);
		
		if (bw <= bandwidth){
			edge.setTE_info(tE_info);
		}
				
		return true;
	}
}
