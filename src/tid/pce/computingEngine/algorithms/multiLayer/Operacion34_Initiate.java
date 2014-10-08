package tid.pce.computingEngine.algorithms.multiLayer;

import java.net.Socket;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.logging.Logger;

import org.jgrapht.graph.SimpleDirectedWeightedGraph;

import es.tid.pce.pcep.messages.PCEPMessageTypes;
import es.tid.pce.pcep.messages.PCEPReport;
import es.tid.pce.pcep.objects.ExplicitRouteObject;
import es.tid.rsvp.objects.subobjects.OpenFlowUnnumberIfIDEROSubobject;
import tid.pce.pcepsession.PCEPSessionsInformation;
import tid.pce.server.comunicationvntm.PCEPClientSession;
import tid.pce.tedb.IntraDomainEdge;
public class Operacion34_Initiate  {
	
	public Operacion34_Initiate(){
		
	}
	
	private static Multilayer_MinTH_AlgorithmPreComputation preComp;
	
	private static int number_hops = 0;
	
	public static int getNumber_hops() {
		return number_hops;
	}
	
	private static Logger log=Logger.getLogger("PCEServer");
	
	public static ExplicitRouteObject get_op43 
	(SimpleDirectedWeightedGraph<Object,IntraDomainEdge> graph, 
		Object src, Object dst, int maxHops){
				
		//There's no way to go from source to destination, let's play with VNTM
		LinkedList<String> srcneighborhood=new LinkedList<String>(); 
		LinkedList<String> dstneighborhood=new LinkedList<String>();
		
		//We take the neighbor nodes
		srcneighborhood=getNodeNeighborhood((String)src, graph);
		dstneighborhood=getNodeNeighborhood((String)dst, graph);
		srcneighborhood.addFirst((String)src);
		dstneighborhood.addFirst((String)dst);
		
		boolean end=false;
		boolean error=false;
		ExplicitRouteObject ero=null;
		
		if (srcneighborhood.size()==0 || dstneighborhood.size()==0)
			return null;
		
		
		Socket s1=null;
		try {
			s1 = new Socket("localhost", 4190);
			PCEPClientSession vntmsessionil = new PCEPClientSession( s1,new PCEPSessionsInformation(), "00:00:00:00:00:00:00:00", "00:00:00:00:00:00:00:00", 0, 0, "add", PCEPMessageTypes.MESSAGE_INITIATE);
			vntmsessionil.start();
			while(vntmsessionil.isAlive()){
				System.out.println("Waiting for response...");
				Thread.sleep(100);
			}
			System.out.println("Response: "+vntmsessionil.getReport().toString());
			s1.close();
			deleteNotInterLayerNodes(srcneighborhood, dstneighborhood, vntmsessionil.getReport());
		}
		catch(Exception e){
			e.printStackTrace();
		}
		
		while (!end){
			
			String srcaux=srcneighborhood.removeFirst();
			
		for (int i=0; i< dstneighborhood.size(); i++){
			String dstaux=dstneighborhood.get(i);
			Socket s=null;
			try {
				s = new Socket("localhost", 4190);

				PCEPClientSession vntmsession = new PCEPClientSession( s,new PCEPSessionsInformation(), srcaux, dstaux, 0, 0, "add", PCEPMessageTypes.MESSAGE_INITIATE);
				vntmsession.start();
				while(vntmsession.isAlive()){
					System.out.println("Waiting for response...");
					Thread.sleep(100);
				}
				System.out.println("Response: "+vntmsession.getReport().toString());
				if (vntmsession.getReport().getStateReportList().get(0).getPath()!=null) {
					
					//BUILD ERO!!!!!!!!!!!!!!!!!!!
					ero=buildero((String)src,srcaux,(String)dst,dstaux);
					
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
		
		if (error){
			log.info("OP_3/4: No path found. There's no lightpath.");
			return null;
		}
		return ero;
		
	}

	private static void deleteNotInterLayerNodes(LinkedList<String> srcneighborhood,
			LinkedList<String> dstneighborhood, PCEPReport report) {

		for (int i=0; i<srcneighborhood.size(); i++){
			if (!isInReport(srcneighborhood.get(i), report)){
				srcneighborhood.remove(i);
				i--;
			}
		}

		for (int i=0; i<dstneighborhood.size(); i++){
			if (!isInReport(dstneighborhood.get(i), report)){
				dstneighborhood.remove(i);
				i--;
			}
		}
		
	}

	private static boolean isInReport(String node, PCEPReport report) {
		for (int i=0; i<report.getStateReportList().get(0).getPath().geteRO().getEROSubobjectList().size();i++){
			if (node.equals(((OpenFlowUnnumberIfIDEROSubobject)report.getStateReportList().get(0).getPath().geteRO().getEROSubobjectList().get(i)).getSwitchID())){
				return true;
			}
		}
		return false;
	}

	private static ExplicitRouteObject buildero(String src, String srcaux, String dst,
			String dstaux) {
		ExplicitRouteObject ero=new ExplicitRouteObject();

		if (src.equals(srcaux)){
			OpenFlowUnnumberIfIDEROSubobject erosubobj=new OpenFlowUnnumberIfIDEROSubobject();
			erosubobj.setSwitchID(src); 
			//Get interface

			ero.getEROSubobjectList().add(erosubobj);
		} else {
			OpenFlowUnnumberIfIDEROSubobject erosubobj=new OpenFlowUnnumberIfIDEROSubobject();
			erosubobj.setSwitchID(src); 
			//Get interface
	
			
			
			OpenFlowUnnumberIfIDEROSubobject erosubobj1=new OpenFlowUnnumberIfIDEROSubobject();
			erosubobj1.setSwitchID(srcaux);
			//Get interface
			
			ero.getEROSubobjectList().add(erosubobj);
			ero.getEROSubobjectList().add(erosubobj1);

			
		}if (dst.equals(dstaux)){
			OpenFlowUnnumberIfIDEROSubobject erosubobj2=new OpenFlowUnnumberIfIDEROSubobject();
			erosubobj2.setSwitchID(dst);
			//Get interface
			
			ero.getEROSubobjectList().add(erosubobj2);

		} else {
			OpenFlowUnnumberIfIDEROSubobject erosubobj2=new OpenFlowUnnumberIfIDEROSubobject();
			erosubobj2.setSwitchID(dst);
			//Get interface
			
			OpenFlowUnnumberIfIDEROSubobject erosubobj3=new OpenFlowUnnumberIfIDEROSubobject();
			erosubobj3.setSwitchID(dstaux);
			//No interface???
			
			ero.getEROSubobjectList().add(erosubobj2);
			ero.getEROSubobjectList().add(erosubobj3);

		}
		return ero;
	}

	private static LinkedList<String> getNodeNeighborhood(String node, SimpleDirectedWeightedGraph<Object, IntraDomainEdge> graph) {

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
