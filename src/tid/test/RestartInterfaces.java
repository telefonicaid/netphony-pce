package tid.test;

import tid.ipnms.wsimpl.impl.IdaTelnetClientPrueba;

public class RestartInterfaces {

	/**
	 * Info: Este código borra las IPs establecidas para el One de las interfaces de los Juniper
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		String machine[] = {"MX240-1","MX240-1","MX240-2","MX240-2","MX240-3","MX240-3"};
		String intf[] = {"ge-2/1/8","ge-2/1/9","ge-2/1/8","ge-2/1/9","ge-2/1/8","ge-2/1/9"};
		String ip[] = {"23.23.23.2/30","43.43.43.2/30","43.43.43.1/30","33.33.33.2/30","33.33.33.1/30","23.23.23.1/30"};
		for (int i=0; i<6; i++){
			IdaTelnetClientPrueba telnetClient = new IdaTelnetClientPrueba(true, new String[] {"#", ":", ">"});
			
			
			String managementip= returnManagmentAddress(machine[i]);
			if (ip!=null){
				System.out.println("Router: "+machine[i]+" IP: "+ip+" Interface: "+intf);
			try {
				telnetClient.connect(managementip, 23);
				//login to the router
				System.out.println("JuniperIpConfigurationClient:configure Response get: " + telnetClient.send("tid" + "\r"));
				System.out.println("JuniperIpConfigurationClient:configure Response get: " + telnetClient.send("Juniper" + "\r"));
				
				String response = telnetClient.send("\n\r");
				System.out.println("JuniperIpConfigurationClient:configure Response get: " + response );
		
				System.out.println("JuniperIpConfigurationClient:configure Response get: " + telnetClient.send("configure \r"));
				System.out.println("JuniperIpConfigurationClient:configure Response get: " + telnetClient.send("delete interfaces "+intf[i]+" unit 0 family inet address "+ip[i]+"\r"));
				
				System.out.println("JuniperIpConfigurationClient:configure Response get: " + telnetClient.send("commit \r"));
				System.out.println("JuniperIpConfigurationClient:configure Response get: " + telnetClient.send("exit \r"));
				//System.out.println("JuniperIpConfigurationClient:configure Response get: " + telnetClient.send("exit \r"));
				telnetClient.close();
			}catch (Exception e){
				e.printStackTrace();
			}
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
