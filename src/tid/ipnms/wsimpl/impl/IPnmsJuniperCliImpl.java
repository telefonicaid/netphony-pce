package tid.ipnms.wsimpl.impl;

import java.io.IOException;
import java.net.SocketException;

import tid.ipnms.datamodel.router.RouterDesc;
import tid.ipnms.datamodel.router.GRETunnel.GRETunnelDesc;
import tid.ipnms.datamodel.router.IPinterface.IPInterfaceConfig;
import tid.ipnms.datamodel.router.IPinterface.IPInterfaceDesc;
import tid.ipnms.datamodel.router.LabelSwitchedPath.LabelSwitchedPath;
import tid.ipnms.datamodel.router.LabelSwitchedPath.LabelSwitchedPathWithUnnumIf;
import tid.ipnms.datamodel.router.routing.StaticRouteDesc;
import tid.ipnms.datamodel.router.routing.acl.ACLDesc;
import tid.ipnms.datamodel.router.routing.acl.ForwardingRuleDesc;
import tid.ipnms.datamodel.router.routing.routingprotocol.RProtocolDesc;
import tid.ipnms.wsimpl.IPnmsWs;

public class IPnmsJuniperCliImpl implements IPnmsWs {

	@Override
	public int configureIPInterface(RouterDesc desc, IPInterfaceDesc ifDesc, IPInterfaceConfig config) {
		IdaTelnetClientPrueba telnetClient = new IdaTelnetClientPrueba(true, new String[] {"#", ":", ">"});
		try {
			telnetClient.connect(desc.getManagementAddress().getHostAddress(), (int) desc.getConfigurationPort());
			//login to the router
			System.out.println("JuniperIpConfigurationClient:configure Response get: " + telnetClient.send(desc.getAuthInfo().getUserID() + "\r"));
			System.out.println("JuniperIpConfigurationClient:configure Response get: " + telnetClient.send(desc.getAuthInfo().getPasswd() + "\r"));
			
			String response = telnetClient.send("\n\r");
			System.out.println("JuniperIpConfigurationClient:configure Response get: " + response );

//			System.out.println("JuniperIpConfigurationClient:configure Response get: " + telnetClient.send("configure \r"));
//			System.out.println("JuniperIpConfigurationClient:configure Response get: " + telnetClient.send("commit \r"));
			System.out.println("JuniperIpConfigurationClient:configure Response get: " + telnetClient.send("configure \r"));
			
			if (config.getOperation().contains("add")) {
				//System.out.println("JuniperIpConfigurationClient:configure Response get: " + telnetClient.send("delete interface " + ifDesc.getInterfaceID() +  " family inet \r")); //falta poner el unit X
				System.out.println("JuniperIpConfigurationClient:configure Response get: " + telnetClient.send("set interfaces " + ifDesc.getInterfaceID() +  " unit 0 family inet address " + config.getIpAddress() + "/" + config.getSubnet()  + "\r"));
			} else if (config.getOperation().contains("del")) {
				System.out.println("JuniperIpConfigurationClient:configure Response get: " + telnetClient.send("delete interface " + ifDesc.getInterfaceID() + " unit 0 family inet address " + config.getIpAddress() + "/" + config.getSubnet()  + "\r"));
			} else {
				System.out.println("No command found"); 
				telnetClient.close();
				return -1;
			}
			
			System.out.println("JuniperIpConfigurationClient:configure Response get: " + telnetClient.send("commit \r"));
			System.out.println("JuniperIpConfigurationClient:configure Response get: " + telnetClient.send("exit \r"));
			telnetClient.close();

			return 0;
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return -1;
	}

	@Override
	public int createGREInterface(RouterDesc desc, GRETunnelDesc tunnDesc) {
		IdaTelnetClientPrueba telnetClient = new IdaTelnetClientPrueba(true, new String[] {"#", ":", ">"});
		try {
			telnetClient.connect(desc.getManagementAddress().getHostAddress(), (int) desc.getConfigurationPort());
			//login to the router
			System.out.println("JuniperIpConfigurationClient:configure Response get: " + telnetClient.send(desc.getAuthInfo().getUserID() + "\r"));
			System.out.println("JuniperIpConfigurationClient:configure Response get: " + telnetClient.send(desc.getAuthInfo().getPasswd() + "\r"));

			String response = telnetClient.send("\n\r");
			System.out.println("JuniperIpConfigurationClient:configure Response get: " + response );
			System.out.println("JuniperIpConfigurationClient:configure Response get: " + telnetClient.send("configure \r"));
			System.out.println("JuniperIpConfigurationClient:configure Response get: " + telnetClient.send("delete interface " + tunnDesc.getTunnelID() + " tunnel \r"));
//			System.out.println("JuniperIpConfigurationClient:configure Response get: " + telnetClient.send("set interfaces " + tunnDesc.getTunnelID() + " tunnel \r"));
//			System.out.println("JuniperIpConfigurationClient:configure Response get: " + telnetClient.send("source " + tunnDesc.getSource() + " \r"));
//			System.out.println("JuniperIpConfigurationClient:configure Response get: " + telnetClient.send("destination " + tunnDesc.getDestination() + " \r"));

			System.out.println("JuniperIpConfigurationClient:configure Response get: " + telnetClient.send("set interfaces " + tunnDesc.getTunnelID() + " tunnel source " + tunnDesc.getSource() + " destination " + tunnDesc.getDestination() + " \r"));

			System.out.println("JuniperIpConfigurationClient:configure Response get: " + telnetClient.send("commit \r"));
			System.out.println("JuniperIpConfigurationClient:configure Response get: " + telnetClient.send("exit \r"));
			telnetClient.close();
			return 0;
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return -1;
	}

	@Override
	public int configureRoutingProtocol(RouterDesc desc, RProtocolDesc rDesc) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int configureStaticRoute(RouterDesc desc, StaticRouteDesc rDesc) {
		IdaTelnetClientPrueba telnetClient = new IdaTelnetClientPrueba(true, new String[] {"#", ":", ">"});
		try {
			telnetClient.connect(desc.getManagementAddress().getHostAddress(), (int) desc.getConfigurationPort());

			//login to the router
			System.out.println("JuniperIpConfigurationClient:configure Response get: " + telnetClient.send(desc.getAuthInfo().getUserID() + "\r"));
			System.out.println("JuniperIpConfigurationClient:configure Response get: " + telnetClient.send(desc.getAuthInfo().getPasswd() + "\r"));

			String response = telnetClient.send("\n\r");
			System.out.println("JuniperIpConfigurationClient:configure Response get: " + response );
			System.out.println("JuniperIpConfigurationClient:configure Response get: " + telnetClient.send("configure \r"));
			if (rDesc.getOperation().toLowerCase().compareTo("add") == 0) {
				System.out.println("JuniperIpConfigurationClient:configure Response get: " + telnetClient.send("set routing-options static route " + rDesc.getDestIP() + "/" + rDesc.getDestSubnet() + "  next-hop  " +rDesc.getNextHopIP() +  "\r"));
			}
			else if (rDesc.getOperation().toLowerCase().compareTo("drop") == 0){
				System.out.println("JuniperIpConfigurationClient:configure Response get: " + telnetClient.send("delete routing-options static route " + rDesc.getDestIP() + "/" + rDesc.getDestSubnet() + " \r"));
			}
			else if (rDesc.getOperation().toLowerCase().compareTo("change") == 0){
				
				System.out.println("JuniperIpConfigurationClient:configure Response get: " + telnetClient.send("delete routing-options static route " + rDesc.getDestIP() + "/" + rDesc.getDestSubnet() + " \r"));
				System.out.println("JuniperIpConfigurationClient:configure Response get: " + telnetClient.send("set routing-options static route " + rDesc.getDestIP() + "/" + rDesc.getDestSubnet() + "  next-hop  " +rDesc.getNextHopIP() +  "\r"));
				
			}else{
											
				System.out.println("No command found"); 
				telnetClient.close();
				return -1;
			}
			

			System.out.println("JuniperIpConfigurationClient:configure Response get: " + telnetClient.send("commit \r"));
			System.out.println("JuniperIpConfigurationClient:configure Response get: " + telnetClient.send("exit \r"));
			telnetClient.close();
			
			System.out.println("COMANDO ENVIADO: "+"set routing-options static route " + rDesc.getDestIP() + "/" + rDesc.getDestSubnet() + "  next-hop  " +rDesc.getNextHopIP() +  "\r");
			
			return 0;
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return -1;

	}

	@Override
	public int configureACLStaticRoute(RouterDesc desc, ACLDesc aclDesc,
			ForwardingRuleDesc ruleDesc) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int configureLabelSwitchedPath(RouterDesc desc, LabelSwitchedPath lsp) {
		// TODO Auto-generated method stub
		IdaTelnetClientPrueba telnetClient = new IdaTelnetClientPrueba(true, new String[] {"#", ":", ">"});
		try {
			telnetClient.connect(desc.getManagementAddress().getHostAddress(), (int) desc.getConfigurationPort());
			//login to the router
			System.out.println("JuniperIpConfigurationClient:configure Response get: " + telnetClient.send(desc.getAuthInfo().getUserID() + "\r"));
			System.out.println("JuniperIpConfigurationClient:configure Response get: " + telnetClient.send(desc.getAuthInfo().getPasswd() + "\r"));

			String response = telnetClient.send("\n\r");
			System.out.println("JuniperIpConfigurationClient:configure Response get: " + response );
			System.out.println("JuniperIpConfigurationClient:configure Response get: " + telnetClient.send("configure \r"));
			System.out.println("*******Operation type******");
			if (lsp.getOperation().toLowerCase().compareTo("add") == 0) {
				System.out.println("*******add******" + lsp.getPath().size());	
				for(int i = 0; i < lsp.getPath().size(); i++){
					
					String addrToString = lsp.getPath().get(i).toString();
					//System.out.println("Salto 1:" + addrToString);
					int indice = addrToString.indexOf('/');
					String jump = addrToString.substring(indice+1);
					//System.out.println("Salto 11:" + jump);
					
					System.out.println("JuniperIpConfigurationClient:configure Response get: " + telnetClient.send("set protocols mpls path " + lsp.getPathName() + " " + jump + "\r"));
					
				}
				System.out.println("JuniperIpConfigurationClient:configure Response get: " + telnetClient.send("set protocols mpls label-switched-path " + lsp.getLspId() + " primary " + lsp.getPathName() +"\r"));
				System.out.println("JuniperIpConfigurationClient:configure Response get: " + telnetClient.send("set protocols mpls label-switched-path " + lsp.getLspId() + " from " + lsp.getSource() + " to " + lsp.getDestination() + " no-cspf\r"));
				System.out.println("JuniperIpConfigurationClient:configure Response get: " + telnetClient.send("set protocols mpls label-switched-path " + lsp.getLspId() + " lsp-attributes encoding-type " + lsp.getLspProperties().getEncodingType() + " switching-type " + lsp.getLspProperties().getSwitchingType() + "\r"));//+ " encoding-type " + lsp.getLspProperties().getEncodingType() + " gpid " + lsp.getLspProperties().getGpid() + "\r"));
				
			}
			else if (lsp.getOperation().toLowerCase().compareTo("delete") == 0){
				
				
				System.out.println("JuniperIpConfigurationClient:configure Response get: " + telnetClient.send("set protocols mpls label-switched-path " + lsp.getLspId() +" disable" + " \r"));
				System.out.println("JuniperIpConfigurationClient:configure Response get: " + telnetClient.send("commit \r"));
				
				
				System.out.println("JuniperIpConfigurationClient:configure Response get: " + telnetClient.send("delete protocols mpls label-switched-path " + lsp.getLspId() +" \r"));
				System.out.println("JuniperIpConfigurationClient:configure Response get: " + telnetClient.send("delete protocols mpls path " + lsp.getPathName() +" \r"));
				
			}
			else
			{
				System.out.println("No command found"); 
				telnetClient.close();
				return -1;
			}

			System.out.println("JuniperIpConfigurationClient:configure Response get: " + telnetClient.send("commit \r"));
			System.out.println("JuniperIpConfigurationClient:configure Response get: " + telnetClient.send("exit \r"));
			telnetClient.close();
			return 0;
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return -1;

	}

	public int disableLSP(RouterDesc desc, String lspName) {
		// TODO Auto-generated method stub
		IdaTelnetClientPrueba telnetClient = new IdaTelnetClientPrueba(true, new String[] {"#", ":", ">"});
		try {
			telnetClient.connect(desc.getManagementAddress().getHostAddress(), (int) desc.getConfigurationPort());
			//login to the router
			System.out.println("JuniperIpConfigurationClient:configure Response get: " + telnetClient.send(desc.getAuthInfo().getUserID() + "\r"));
			System.out.println("JuniperIpConfigurationClient:configure Response get: " + telnetClient.send(desc.getAuthInfo().getPasswd() + "\r"));

			String response = telnetClient.send("\n\r");
			System.out.println("JuniperIpConfigurationClient:configure Response get: " + response );
			System.out.println("JuniperIpConfigurationClient:configure Response get: " + telnetClient.send("configure \r"));
			System.out.println("JuniperIpConfigurationClient:configure Response get: " + telnetClient.send("set protocols mpls label-switched-path "+lspName+" disable\r"));

			System.out.println("JuniperIpConfigurationClient:configure Response get: " + telnetClient.send("commit \r"));
			System.out.println("JuniperIpConfigurationClient:configure Response get: " + telnetClient.send("exit \r"));
			telnetClient.close();
			return 0;
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return -1;

	}

	public int enableLSP(RouterDesc desc, String lspName) {
		// TODO Auto-generated method stub
		IdaTelnetClientPrueba telnetClient = new IdaTelnetClientPrueba(true, new String[] {"#", ":", ">"});
		try {
			telnetClient.connect(desc.getManagementAddress().getHostAddress(), (int) desc.getConfigurationPort());
			//login to the router
			System.out.println("JuniperIpConfigurationClient:configure Response get: " + telnetClient.send(desc.getAuthInfo().getUserID() + "\r"));
			System.out.println("JuniperIpConfigurationClient:configure Response get: " + telnetClient.send(desc.getAuthInfo().getPasswd() + "\r"));

			String response = telnetClient.send("\n\r");
			System.out.println("JuniperIpConfigurationClient:configure Response get: " + response );
			System.out.println("JuniperIpConfigurationClient:configure Response get: " + telnetClient.send("configure \r"));
			System.out.println("JuniperIpConfigurationClient:configure Response get: " + telnetClient.send("delete protocols mpls label-switched-path "+lspName+" disable\r"));

			System.out.println("JuniperIpConfigurationClient:configure Response get: " + telnetClient.send("commit \r"));
			System.out.println("JuniperIpConfigurationClient:configure Response get: " + telnetClient.send("exit \r"));
			telnetClient.close();
			return 0;
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return -1;

	}
	
	
	public int increaseLDPWeightMX1(RouterDesc desc){
		
		// TODO Auto-generated method stub
		IdaTelnetClientPrueba telnetClient = new IdaTelnetClientPrueba(true, new String[] {"#", ":", ">"});
		try {
			telnetClient.connect(desc.getManagementAddress().getHostAddress(), (int) desc.getConfigurationPort());
			//login to the router
			System.out.println("JuniperIpConfigurationClient:configure Response get: " + telnetClient.send(desc.getAuthInfo().getUserID() + "\r"));
			System.out.println("JuniperIpConfigurationClient:configure Response get: " + telnetClient.send(desc.getAuthInfo().getPasswd() + "\r"));

			
			String response = telnetClient.send("\n\r");
			System.out.println("JuniperIpConfigurationClient:configure Response get: " + response );
			System.out.println("JuniperIpConfigurationClient:configure Response get: " + telnetClient.send("configure \r"));
			System.out.println("JuniperIpConfigurationClient:configure Response get: " + telnetClient.send("set logical-system PIC_2012 protocols isis interface ge-2/1/8.0 level 2 metric 30\r"));

			System.out.println("JuniperIpConfigurationClient:configure Response get: " + telnetClient.send("commit \r"));
			System.out.println("JuniperIpConfigurationClient:configure Response get: " + telnetClient.send("exit \r"));
			telnetClient.close();
			return 0;
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return -1;
		
	}

	public int decreaseLDPWeightMX1(RouterDesc desc){
		
		// TODO Auto-generated method stub
		IdaTelnetClientPrueba telnetClient = new IdaTelnetClientPrueba(true, new String[] {"#", ":", ">"});
		try {
			telnetClient.connect(desc.getManagementAddress().getHostAddress(), (int) desc.getConfigurationPort());
			//login to the router
			System.out.println("JuniperIpConfigurationClient:configure Response get: " + telnetClient.send(desc.getAuthInfo().getUserID() + "\r"));
			System.out.println("JuniperIpConfigurationClient:configure Response get: " + telnetClient.send(desc.getAuthInfo().getPasswd() + "\r"));

			
			String response = telnetClient.send("\n\r");
			System.out.println("JuniperIpConfigurationClient:configure Response get: " + response );
			System.out.println("JuniperIpConfigurationClient:configure Response get: " + telnetClient.send("configure \r"));
			System.out.println("JuniperIpConfigurationClient:configure Response get: " + telnetClient.send("delete logical-system PIC_2012 protocols isis interface ge-2/1/8.0 level 2 metric 30\r"));

			System.out.println("JuniperIpConfigurationClient:configure Response get: " + telnetClient.send("commit \r"));
			System.out.println("JuniperIpConfigurationClient:configure Response get: " + telnetClient.send("exit \r"));
			telnetClient.close();
			return 0;
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return -1;
		
	}
	
	public int increaseLDPWeightMX3(RouterDesc desc){
		
		// TODO Auto-generated method stub
		IdaTelnetClientPrueba telnetClient = new IdaTelnetClientPrueba(true, new String[] {"#", ":", ">"});
		try {
			telnetClient.connect(desc.getManagementAddress().getHostAddress(), (int) desc.getConfigurationPort());
			//login to the router
			System.out.println("JuniperIpConfigurationClient:configure Response get: " + telnetClient.send(desc.getAuthInfo().getUserID() + "\r"));
			System.out.println("JuniperIpConfigurationClient:configure Response get: " + telnetClient.send(desc.getAuthInfo().getPasswd() + "\r"));

			
			String response = telnetClient.send("\n\r");
			System.out.println("JuniperIpConfigurationClient:configure Response get: " + response );
			System.out.println("JuniperIpConfigurationClient:configure Response get: " + telnetClient.send("configure \r"));
			System.out.println("JuniperIpConfigurationClient:configure Response get: " + telnetClient.send("set logical-system PIC_2012 protocols isis interface ge-2/1/9.0 level 2 metric 30\r"));

			System.out.println("JuniperIpConfigurationClient:configure Response get: " + telnetClient.send("commit \r"));
			System.out.println("JuniperIpConfigurationClient:configure Response get: " + telnetClient.send("exit \r"));
			telnetClient.close();
			return 0;
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return -1;
	}
	
	public int decreaseLDPWeightMX3(RouterDesc desc){
			
		// TODO Auto-generated method stub
		IdaTelnetClientPrueba telnetClient = new IdaTelnetClientPrueba(true, new String[] {"#", ":", ">"});
		try {
			telnetClient.connect(desc.getManagementAddress().getHostAddress(), (int) desc.getConfigurationPort());
			//login to the router
			System.out.println("JuniperIpConfigurationClient:configure Response get: " + telnetClient.send(desc.getAuthInfo().getUserID() + "\r"));
			System.out.println("JuniperIpConfigurationClient:configure Response get: " + telnetClient.send(desc.getAuthInfo().getPasswd() + "\r"));
			
			String response = telnetClient.send("\n\r");
			System.out.println("JuniperIpConfigurationClient:configure Response get: " + response );
			System.out.println("JuniperIpConfigurationClient:configure Response get: " + telnetClient.send("configure \r"));
			System.out.println("JuniperIpConfigurationClient:configure Response get: " + telnetClient.send("delete logical-system PIC_2012 protocols isis interface ge-2/1/9.0 level 2 metric 30\r"));
			System.out.println("JuniperIpConfigurationClient:configure Response get: " + telnetClient.send("commit \r"));
			System.out.println("JuniperIpConfigurationClient:configure Response get: " + telnetClient.send("exit \r"));
			telnetClient.close();
			return 0;
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return -1;
			
	}
		
	
	
	@Override
	public int configureLabelSwitchedPathWithUnnIf(RouterDesc desc,
			LabelSwitchedPathWithUnnumIf lsp) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	
}
