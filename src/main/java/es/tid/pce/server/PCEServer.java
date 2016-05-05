package es.tid.pce.server;

public class PCEServer {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		DomainPCEServer  pceserver = new DomainPCEServer();
		if(args.length > 0)
			pceserver.configure(args[0]);
		else 
			pceserver.configure(null);
		
		pceserver.run();
	}

}
