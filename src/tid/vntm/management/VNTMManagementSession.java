package tid.vntm.management;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import es.tid.ospf.ospfv2.OSPFv2LinkStateUpdatePacket;
import tid.netManager.emulated.SimpleEmulatedNetworkLSPManager;
import tid.vntm.LSPManager;

public class VNTMManagementSession extends Thread {
	
	private Socket socket;
	private PrintStream out;
	private Logger log;
	
	private LSPManager lspmanager;
	
	public VNTMManagementSession(Socket s, LSPManager lspmanager){
		this.socket=s;
		this.lspmanager=lspmanager;
		log=Logger.getLogger("PCEServer");
	}
	
	public void run(){
		log.info("Starting VNTM Management session");
		boolean running=true;
		try {
			out=new PrintStream(socket.getOutputStream());
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		try {
			//out.print("-------------\n");
			BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			while (running) {
				out.print("VNTM:>");
				String command = null;
				try {
					command = br.readLine();
				} catch (IOException ioe) {
					log.warning("IO error trying to read your command");
					return;
				}
				if (command.equals("quit")) {
					log.info("Ending VNTM Management Session");
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
				if (command.equals("show TE Links")) {
					out.println("TE Links:");
					out.println(lspmanager.printLSPs());
					out.println("Number of TE Links : "+ lspmanager.countTELinks());
					out.println("Number of Reserve Transport Links: "+lspmanager.countCapacity() );

				} 
				else if (command.equals("help")){
					out.print("Available commands:\r\n");
					out.print("show TE Links\r\n");
					out.print("quit\r\n");
					
				}
				else if (command.equals("reset")){
					//Recorrer y Borrar todas las lsps que haya
					lspmanager.removeAllLSPs();
					//Llamar al network emulator thread: recargar la topologia
					LinkedBlockingQueue<OSPFv2LinkStateUpdatePacket> sendingQueue = new LinkedBlockingQueue<OSPFv2LinkStateUpdatePacket>(); 
					SimpleEmulatedNetworkLSPManager net = new SimpleEmulatedNetworkLSPManager(sendingQueue,"hola");
					//net.reset();
					//rester ok
				}
				else if (command.equals("hard reset")){
					
				}
				else if (command.equals("set traces on")) {
					log.setLevel(Level.ALL);		
					Logger log2=Logger.getLogger("VNTMServer");
					log2.setLevel(Level.ALL);
					Logger log3= Logger.getLogger("PCEPParser");
					log3.setLevel(Level.ALL);
					out.print("traces on!\r\n");
				} 
				else if (command.equals("set traces off")) {
					log.setLevel(Level.SEVERE);		
					Logger log2=Logger.getLogger("VNTMServer");
					log2.setLevel(Level.SEVERE);
					Logger log3= Logger.getLogger("PCEPParser");
					log3.setLevel(Level.SEVERE);
					out.print("traces off!\r\n");
				} 

				else{
					//out.print("invalid command\n");	
					out.print("\n");
				}

			}
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
	}
}
