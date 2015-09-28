package es.tid.pce.parentPCE.management;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Inet4Address;
import java.net.Socket;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

import es.tid.pce.computingEngine.RequestDispatcher;
import es.tid.pce.computingEngine.RequestProcessorThread;
import es.tid.pce.computingEngine.algorithms.ComputingAlgorithmManager;
import es.tid.pce.parentPCE.ChildPCERequestManager;
import es.tid.pce.parentPCE.MultiDomainTopologyUpdater;
import es.tid.pce.parentPCE.MDLSPDB.MultiDomainLSPDB;
import es.tid.pce.pcepsession.PCEPSessionsInformation;
import es.tid.tedb.ITMDTEDB;
import es.tid.tedb.MDTEDB;
import es.tid.tedb.ReachabilityManager;
import es.tid.tedb.SimpleTEDB;

public class ParentPCEManagementSession extends Thread {
	
	private Socket socket;
	
	private Logger log;
	
	private ChildPCERequestManager cprm;
	
	private RequestDispatcher requestDispatcher;
	
	private PrintStream out;
	
	private MDTEDB mdtedb;
	
	private SimpleTEDB simpleTedb;
	
	private ITMDTEDB ITmdtedb;
	
	private ReachabilityManager rm;
	
	private boolean isITcapable=false;
	
	private PCEPSessionsInformation pcepSessionManager;
	
	private MultiDomainTopologyUpdater mdtu;
	
	private MultiDomainLSPDB multiDomainLSPDB;
	
	public ParentPCEManagementSession(Socket s, ChildPCERequestManager cprm, RequestDispatcher requestDispatcher, MDTEDB mdtedb,SimpleTEDB simpleTedb, ReachabilityManager rm, PCEPSessionsInformation pcepSessionManager,MultiDomainTopologyUpdater mdtu, MultiDomainLSPDB multiDomainLSPDB){
		this.socket=s;
		this.cprm=cprm;
		this.requestDispatcher=requestDispatcher;
		this.mdtedb=mdtedb;
		this.rm=rm;
		log=Logger.getLogger("PCEServer");
		isITcapable=false;
		this.pcepSessionManager=pcepSessionManager;
		this.mdtu=mdtu;
		this.simpleTedb=simpleTedb;
		this.multiDomainLSPDB=multiDomainLSPDB;
	}

	
	public ParentPCEManagementSession(Socket s, ChildPCERequestManager cprm, RequestDispatcher requestDispatcher, ITMDTEDB ITmdtedb, ReachabilityManager rm, PCEPSessionsInformation pcepSessionManager,MultiDomainTopologyUpdater mdtu){
		this.socket=s;
		this.cprm=cprm;
		this.requestDispatcher=requestDispatcher;
		this.ITmdtedb=ITmdtedb;
		this.rm=rm;
		log=Logger.getLogger("PCEServer");
		isITcapable=true;
		this.pcepSessionManager=pcepSessionManager;
		this.mdtu=mdtu;
	}
	
	
	public void run(){
		log.info("Starting Management session");
		boolean running=true;
		try {
			out=new PrintStream(socket.getOutputStream());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		try {
			//out.print("-------------\n");
			BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			while (running) {
				out.print("PCE:>");
				String command = null;
				try {
					command = br.readLine();
					if (command==null){
						try {
							out.close();						
						} catch (Exception e){
							e.printStackTrace();
						}
						try {
							br.close();	
							return;
						} catch (Exception e){
							e.printStackTrace();
						}					
					}
				} catch (IOException ioe) {
					out.print("IO error trying to read your command");
					log.warning("IO error trying to read your command");
					return;
				}
				if (command.equals("quit")) {
					log.info("Ending Management Session");
					out.println("bye!");
					try {
						out.close();						
					} catch (Exception e){
						e.printStackTrace();
					}
					try {
						br.close();						
					} catch (Exception e){
						e.printStackTrace();
					}					
					return;
				}				
				if (command.equals("show child pces")) {
					Enumeration<Inet4Address> pceIds= cprm.getDomainIdpceId().elements();
					Enumeration<Inet4Address> domains = cprm.getDomainIdpceId().keys();
					while (pceIds.hasMoreElements()){
						out.print("PCE Id: "+pceIds.nextElement()+ " Domain Id: "+domains.nextElement()+"\r\n");
					}
				} 
				else if (command.equals("show algorithms list")||command.equals("show algo list") ) {
					RequestProcessorThread[] threads=requestDispatcher.getThreads();
					String info="";										
					if (threads.length>0){
						Hashtable<Integer, ComputingAlgorithmManager> htcaSingle= threads[0].getSingleAlgorithmList();
						Enumeration<Integer> keys = htcaSingle.keys();
						while (keys.hasMoreElements()){
							Integer inte=keys.nextElement();
							info=info+"OF ="+inte+"; ";
						}		
						Hashtable<Integer, ComputingAlgorithmManager> htcaSvec= threads[0].getSvecAlgorithmList();
						Enumeration<Integer> keys2 = htcaSvec.keys();
						while (keys2.hasMoreElements()){
							Integer inte=keys2.nextElement();
							info=info+"OF ="+inte+"and SVEC; ";
						}			
					}
					out.print(info+"\r\n");
				}
				else if (command.equals("show topology")&&(!isITcapable)){
					out.print(mdtedb.printMDTopology());
					if (simpleTedb != null){
						out.print("\nSIMPLE TEDB1:\n");
						out.print(simpleTedb.printTopology()); 
					}
					
				}else if (command.equals("show topology")&&(isITcapable)){
					out.print(ITmdtedb.printTopology());
					if (simpleTedb != null){
						out.print("\nSIMPLE TEDB2:\n");
						out.print(simpleTedb.printTopology());
					}
					
				}else if (command.equals("show reachability")){
					out.print( rm.printReachability());
					
				}else if (command.equals("show mdlspdb")){
					if (this.multiDomainLSPDB!=null) {
						out.print( this.multiDomainLSPDB.toString());
					}
					
				}else if (command.equals("queue size")){
					out.print("num pets "+requestDispatcher.queueSize());
					
				}else if (command.equals("show sessions")){
					out.print(pcepSessionManager.toString());
					out.print("\r\n");
				}else if (command.equals("show lsas")){
					out.print(mdtu.printLSAList());
					out.print("\r\n");
				}else if (command.equals("show lsas short")){
					out.print(mdtu.printLSAShortList());
					out.print("\r\n");
				}else if (command.equals("show number lsas")){
					out.print(mdtu.sizeLSAList());
					out.print("\r\n");
				}else if (command.equals("show capabilities")){
					out.print(pcepSessionManager.getLocalPcepCapability().toString());
					out.print("\r\n");
				}else if (command.equals("show peers")){
					out.print(pcepSessionManager.printPeersInfo());
					out.print("\r\n");
				}else if (command.equals("show peers detail")){
					out.print(pcepSessionManager.printFullPeersInfo());
					out.print("\r\n");
				}
				
				else if (command.equals("help")){
					out.print("Available commands:\r\n");
					out.print("show child pces\r\n");
					out.print("show topology\r\n");
					out.print("show sessions\r\n");
					out.print("show lsas short\r\n");
					out.print("show number lsas\r\n");
					out.print("quit\r\n");
					
				}
				else if (command.equals("set traces on")) {
					log.setLevel(Level.ALL);		
					Logger log2=Logger.getLogger("PCEPParser");
					log2.setLevel(Level.ALL);
					Logger log3= Logger.getLogger("OSPFParser");
					log3.setLevel(Level.ALL);
					out.print("traces on!\r\n");
				} 
				else if (command.equals("set traces off")) {
					log.setLevel(Level.SEVERE);		
					Logger log2=Logger.getLogger("PCEPParser");
					log2.setLevel(Level.SEVERE);
					Logger log3= Logger.getLogger("OSPFParser");
					log3.setLevel(Level.SEVERE);
					out.print("traces off!\r\n");
				} else if (command.equals("set ospf traces off")) {
					Logger log3= Logger.getLogger("OSPFParser");
					log3.setLevel(Level.SEVERE);
					out.print("ospf traces off!\r\n");
				} 
				else{
					//out.print("invalid command\n");	
					out.print("\n");
				}

			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
	}

}
