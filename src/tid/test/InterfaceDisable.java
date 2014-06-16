package tid.test;

import tid.ipnms.wsimpl.impl.IdaTelnetClientPrueba;


public class InterfaceDisable {

	/**
	 * Info: Este código desabilita la interfaz especificada dentro del Juniper especificado
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String router=args[0];
		String intf=args[1];
		String operation=args[2];
		IdaTelnetClientPrueba telnetClient = new IdaTelnetClientPrueba(true, new String[] {"#", ":", ">"});
		
		
		String ip= returnManagmentAddress(router);
		if (ip!=null){
			System.out.println("Router: "+router+" IP: "+ip+" Interface: "+intf);
		try {
			telnetClient.connect(ip, 23);
			//login to the router
			System.out.println("JuniperIpConfigurationClient:configure Response get: " + telnetClient.send("tid" + "\r"));
			System.out.println("JuniperIpConfigurationClient:configure Response get: " + telnetClient.send("Juniper" + "\r"));
			
			String response = telnetClient.send("\n\r");
			System.out.println("JuniperIpConfigurationClient:configure Response get: " + response );
	
			System.out.println("JuniperIpConfigurationClient:configure Response get: " + telnetClient.send("configure \r"));
			if (operation.equals("enable"))
				System.out.println("JuniperIpConfigurationClient:configure Response get: " + telnetClient.send("delete interfaces "+intf+" disable \r"));	
			else if (operation.equals("disable"))	
				System.out.println("JuniperIpConfigurationClient:configure Response get: " + telnetClient.send("set interfaces "+intf+" disable \r"));
			
			System.out.println("JuniperIpConfigurationClient:configure Response get: " + telnetClient.send("commit \r"));
			System.out.println("JuniperIpConfigurationClient:configure Response get: " + telnetClient.send("exit \r"));
			//System.out.println("JuniperIpConfigurationClient:configure Response get: " + telnetClient.send("exit \r"));
			telnetClient.close();
		}catch (Exception e){
			e.printStackTrace();
		}
		}
	}

	private static String returnManagmentAddress(String router) {
		if (router.equals("MX240-1"))
			return "10.95.73.72";
		if (router.equals("MX240-2"))
			return "10.95.73.73";
		if (router.equals("MX240-3"))
			return "10.95.73.74";	
		System.out.println("Error en el nombre del router");
		return null;
	}

}
