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
	
	private boolean started = false;
	
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
				if (!started)
				{
					out.print("\n");
					out.print("   ---              ,\r\n");
					out.print("  | P |              ,\r\n");
					out.print("  | C |              |'.             ,\r\n");
					out.print("  | E |              |  '-._        / )\r\n");
					out.print("  |   |            .'  .._  ',     /_'-,\r\n");
					out.print("  | P |           '   /  _'.'_\\   /._)')\r\n");
					out.print("  | A |          :   /  '_' '_'  /  _.'\r\n");
					out.print("  | R |          |E |   |Q| |Q| /   /\r\n");
					out.print("  | E |         .'  _\\  '-' '-'    /\r\n");
					out.print("  | N |       .'--.(S     ,__` )  /\r\n");
					out.print("  | T |             '-.     _.'  /\r\n");
					out.print("  |   |           __.--'----(   /\r\n");
					out.print("  | C |       _.-'     :   __\\ /\r\n");
					out.print("  | O |      (      __.' :'  :Y\r\n");
					out.print("  | N |       '.   '._,  :   :|\r\n");
					out.print("  | T |          '.     ) :.__:|\r\n");
					out.print("  | R |            \\    \\______/\r\n");
					out.print("  | O |             '._L/_H____]\r\n");
					out.print("  | L |              /_        /\r\n");
					out.print("  | L |             /  '-.__.-')\r\n");
					out.print("  | E |            :      /   /\r\n");
					out.print("  | R |            :     /   /\r\n");
					out.print("   ---           ,/_____/----;\r\n");
					out.print("                '._____)----'\r\n");
					out.print("                /     /   /\r\n");
					out.print("               /     /   /\r\n");
					out.print("             .'     /    \\\r\n");
					out.print("            (______(-.____)\r\n");
					out.print("***********************************************\n");					
					started = true;
				}
				out.print("\nAvailable commands:\r\n\n");
				out.print("\t1)show child pces\r\n");
				out.print("\t2)show algorithms list\r\n");
				out.print("\t3)show topology\r\n");
				out.print("\t4)show reachability\r\n");
				out.print("\t5)show mdlspdb\r\n");
				out.print("\t6)queue size\r\n");
				out.print("\t7)show sessions\r\n");
//				out.print("\t8)show lsas\r\n");
//				out.print("\t9)show lsas short\r\n");
//				out.print("\t10)show number lsas\r\n");
				out.print("\t8)show capabilities\r\n");
				out.print("\t9)show peers\r\n");
				out.print("\t10)show peers detail\r\n");
				out.print("\t11)set traces on\r\n");
				out.print("\t12)set traces off\r\n");
//				out.print("\t16)set ospf traces off\r\n");
				out.print("\n\tENTER) quit\r\n");
				
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
				if (command.equals("quit") || command.equals("")) {
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
				if (command.equals("show child pces") || command.equals("1")) {
					Enumeration<Inet4Address> pceIds= cprm.getDomainIdpceId().elements();
					Enumeration<Inet4Address> domains = cprm.getDomainIdpceId().keys();
					while (pceIds.hasMoreElements()){
						out.print("PCE Id: "+pceIds.nextElement()+ " Domain Id: "+domains.nextElement()+"\r\n");
					}
				} 
				else if (command.equals("show algorithms list")||command.equals("show algo list") || command.equals("2")  ) {
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
				else if ((command.equals("show topology")&&(!isITcapable))||(command.equals("3")&&(!isITcapable))){
					out.print(mdtedb.printMDTopology());
					if (simpleTedb != null){
						out.print("\nSIMPLE TEDB1:\n");
						out.print(simpleTedb.printTopology()); 
					}
					
				}else if ((command.equals("show topology")&&(isITcapable)) ||(command.equals("3")&&(!isITcapable))){
					out.print(ITmdtedb.printTopology());
					if (simpleTedb != null){
						out.print("\nSIMPLE TEDB2:\n");
						out.print(simpleTedb.printTopology());
					}
					
				}else if ((command.equals("show reachability"))||(command.equals("4"))){
					out.print( rm.printReachability());
					
				}else if ((command.equals("show mdlspdb"))||(command.equals("5"))){
					if (this.multiDomainLSPDB!=null) {
						out.print( this.multiDomainLSPDB.toString());
					}
					
				}else if ((command.equals("queue size"))||(command.equals("6"))){
					out.print("num pets "+requestDispatcher.queueSize());
					
				}else if ((command.equals("show sessions"))||(command.equals("7"))){
					out.print(pcepSessionManager.toString());
					out.print("\r\n");
//				}else if ((command.equals("show lsas"))||(command.equals("8"))){
//					out.print(mdtu.printLSAList());
//					out.print("\r\n");
//				}else if ((command.equals("show lsas short"))||(command.equals("9"))){
//					out.print(mdtu.printLSAShortList());
//					out.print("\r\n");
//				}else if ((command.equals("show number lsas"))||(command.equals("10"))){
//					out.print(mdtu.sizeLSAList());
//					out.print("\r\n");
				}else if ((command.equals("show capabilities"))||(command.equals("8"))){
					out.print(pcepSessionManager.getLocalPcepCapability().toString());
					out.print("\r\n");
				}else if ((command.equals("show peers"))||(command.equals("9"))){
					out.print(pcepSessionManager.printPeersInfo());
					out.print("\r\n");
				}else if ((command.equals("show peers detail"))||(command.equals("10"))){
					out.print(pcepSessionManager.printFullPeersInfo());
					out.print("\r\n");
				}
				
//				else if (command.equals("help")){
//					out.print("Available commands:\r\n");
//					out.print("show child pces\r\n");
//					out.print("show topology\r\n");
//					out.print("show sessions\r\n");
//					out.print("show lsas short\r\n");
//					out.print("show number lsas\r\n");
//					out.print("quit\r\n");
//					
//				}
				else if ((command.equals("set traces on"))||(command.equals("11"))) {
					log.setLevel(Level.ALL);		
					Logger log2=Logger.getLogger("PCEPParser");
					log2.setLevel(Level.ALL);
					Logger log3= Logger.getLogger("OSPFParser");
					log3.setLevel(Level.ALL);
					out.print("traces on!\r\n");
				} 
				else if ((command.equals("set traces off"))||(command.equals("12"))) {
					log.setLevel(Level.SEVERE);		
					Logger log2=Logger.getLogger("PCEPParser");
					log2.setLevel(Level.SEVERE);
					Logger log3= Logger.getLogger("OSPFParser");
					log3.setLevel(Level.SEVERE);
					out.print("traces off!\r\n");
				} 
//					else if ((command.equals("set ospf traces off"))||(command.equals("16"))) {
//					Logger log3= Logger.getLogger("OSPFParser");
//					log3.setLevel(Level.SEVERE);
//					out.print("ospf traces off!\r\n");
//				} 
				else{
					out.print("invalid command\n");	
					//out.print("\n");
				}

			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
	}

}
