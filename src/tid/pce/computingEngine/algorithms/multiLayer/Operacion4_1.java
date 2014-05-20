package tid.pce.computingEngine.algorithms.multiLayer;

import tid.pce.computingEngine.algorithms.multiLayer.Operacion1;
import tid.pce.computingEngine.algorithms.multiLayer.Operacion2;
import tid.pce.computingEngine.algorithms.multiLayer.Operacion3;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import org.jgrapht.GraphPath;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.alg.KShortestPaths;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;

import tid.pce.computingEngine.algorithms.multiLayer.Multilayer_MinTH_AlgorithmPreComputation.InfoNodo;
import tid.pce.pcep.messages.PCEPMessageTypes;
import tid.pce.pcep.objects.ExplicitRouteObject;
import tid.pce.pcepsession.PCEPSessionsInformation;
import tid.pce.server.comunicationvntm.PCEPClientSession;
import tid.pce.tedb.IntraDomainEdge;
import tid.rsvp.objects.subobjects.OpenFlowUnnumberIfIDEROSubobject;
import tid.vntm.client.VNTMClientSession;
public class Operacion4_1  {
	
	public Operacion4_1(){
		
	}
	
	private static Multilayer_MinTH_AlgorithmPreComputation preComp;
	
	private static int number_hops = 0;
	
	public static int getNumber_hops() {
		return number_hops;
	}
	
	private static Logger log=Logger.getLogger("PCEServer");
	
	ExplicitRouteObject get_op4mas1 
	(SimpleDirectedWeightedGraph<Inet4Address,IntraDomainEdge> graph, 
		SimpleDirectedWeightedGraph<Inet4Address,IntraDomainEdge> interlayer,
		ArrayList<SimpleDirectedWeightedGraph<Inet4Address, IntraDomainEdge>> networkGraphs, 
		String src, String dst, int maxHops, GraphPath<Inet4Address,IntraDomainEdge> gp,
		int numLambdas){
				
		//There's no way to go from source to destination, let's play with VNTM
		LinkedList<String> srcneighborhood=new LinkedList<String>(); 
		LinkedList<String> dstneighborhood=new LinkedList<String>();
		srcneighborhood=getNodeNeighborhood(src, graph);
		dstneighborhood=getNodeNeighborhood(dst, graph);
		boolean end=false;
		boolean error=false;
		ExplicitRouteObject ero=null;
		
		if (srcneighborhood.size()==0 || dstneighborhood.size()==0)
			return null;
		
		while (!end){
			
			String srcaux=srcneighborhood.removeFirst();
			
		for (int i=0; i< dstneighborhood.size(); i++){
			String dstaux=dstneighborhood.get(i);
			Socket s=null;
			try {
				s = new Socket("localhost", 4190);

				PCEPClientSession vntmsession = new PCEPClientSession( s,new PCEPSessionsInformation(), srcaux, dstaux, 0, 0, "add");
				vntmsession.start();
				while(vntmsession.isAlive()){
					System.out.println("Waiting for response...");
					Thread.sleep(100);
				}
				System.out.println("Response: "+vntmsession.getReport().toString());
				if (vntmsession.getReport().getStateReportList().get(0).getPath()!=null) {
					
					//BUILD ERO!!!!!!!!!!!!!!!!!!!
					ero=buildero(src,srcaux,dst,dstaux);
					
					end=true;
					break;
				} else if (srcneighborhood.size()==0 || dstneighborhood.size()-1==i){
					end=true;
					error=true;
				}
			
			
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
			//	public PCEPClientSession(Socket s,PCEPSessionsInformation pcepSessionManager, String sourceMAC, String destMAC, int source_interface, int destination_interface, String operation)

			
		}	
		}
		
		
		
		return null;
		
	}

	private ExplicitRouteObject buildero(String src, String srcaux, String dst,
			String dstaux) {
		ExplicitRouteObject ero=new ExplicitRouteObject();

		OpenFlowUnnumberIfIDEROSubobject erosubobj=new OpenFlowUnnumberIfIDEROSubobject();
		erosubobj.setSwitchID(src); 
		//Get interface

		OpenFlowUnnumberIfIDEROSubobject erosubobj1=new OpenFlowUnnumberIfIDEROSubobject();
		erosubobj1.setSwitchID(srcaux);
		//Get interface
		
		OpenFlowUnnumberIfIDEROSubobject erosubobj2=new OpenFlowUnnumberIfIDEROSubobject();
		erosubobj2.setSwitchID(dst);
		//Get interface
		
		OpenFlowUnnumberIfIDEROSubobject erosubobj3=new OpenFlowUnnumberIfIDEROSubobject();
		erosubobj3.setSwitchID(dstaux);
		//No interface???
		
		return ero;
	}

	private LinkedList<String> getNodeNeighborhood(String node, SimpleDirectedWeightedGraph<Inet4Address, IntraDomainEdge> graph) {

		LinkedList<String> neighborhood=new LinkedList<String>();
		Iterator<IntraDomainEdge> edgeiter=graph.edgeSet().iterator();
		while (edgeiter.hasNext()){
			IntraDomainEdge edge=edgeiter.next();
			if (edge.getSource().equals(node)) { //FIXME: Be careful, it can be wrong
				neighborhood.add((String)edge.getTarget());
			} else if (edge.getTarget().equals(node)) {
				neighborhood.add((String)edge.getSource());
			}		
		}
		return neighborhood;
	}

}
