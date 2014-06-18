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


public class IPnmsCiscoCliImpl implements IPnmsWs {

	@Override
	public int configureIPInterface(RouterDesc desc, IPInterfaceDesc ifDesc,
			IPInterfaceConfig config) {
		IdaTelnetClientPrueba telnetClient = new IdaTelnetClientPrueba(true, new String[] {"#", ":", ">"});
		try {
			telnetClient.connect(desc.getManagementAddress().getHostAddress(), (int) desc.getConfigurationPort());

			//login to the router
			System.out.println("CiscoIpConfigurationClient:configure Response get: " + telnetClient.send(desc.getAuthInfo().getUserID() + "\r"));
			System.out.println("CiscoIpConfigurationClient:configure Response get: " + telnetClient.send(desc.getAuthInfo().getPasswd() + "\r"));
			
			String response = telnetClient.send("\n\r");
			System.out.println("CiscoIpConfigurationClient:configure Response get: " + response );

			System.out.println("CiscoIpConfigurationClient:configure Response get: " + telnetClient.send("enable \r"));
			System.out.println("CiscoIpConfigurationClient:configure Response get: " + telnetClient.send("configure terminal \r"));
			System.out.println("CiscoIpConfigurationClient:configure Response get: " + telnetClient.send("interface " + ifDesc.getInterfaceID() + "\r"));
			System.out.println("CiscoIpConfigurationClient:configure Response get: " + telnetClient.send("ip address " + config.getIpAddress() + " " + config.getSubnetDotFormat() + "\r"));
			System.out.println("CiscoIpConfigurationClient:configure Response get: " + telnetClient.send("no shutdown \r"));
			System.out.println("CiscoIpConfigurationClient:configure Response get: " + telnetClient.send("end \r"));
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
			System.out.println("CiscoIpConfigurationClient:configure Response get: " + telnetClient.send(desc.getAuthInfo().getUserID() + "\r"));
			System.out.println("CiscoIpConfigurationClient:configure Response get: " + telnetClient.send(desc.getAuthInfo().getPasswd() + "\r"));
			
			String response = telnetClient.send("\n\r");
			System.out.println("CiscoIpConfigurationClient:configure Response get: " + response );

			System.out.println("CiscoIpConfigurationClient:configure Response get: " + telnetClient.send("enable \r"));
			System.out.println("CiscoIpConfigurationClient:configure Response get: " + telnetClient.send("configure terminal \r"));
			System.out.println("CiscoIpConfigurationClient:configure Response get: " + telnetClient.send("interface " + tunnDesc.getTunnelID() + "\r"));
			System.out.println("CiscoIpConfigurationClient:configure Response get: " + telnetClient.send("tunnel source " + tunnDesc.getSource() + "\r"));
			System.out.println("CiscoIpConfigurationClient:configure Response get: " + telnetClient.send("tunnel destination " + tunnDesc.getDestination() + "\r"));
			System.out.println("CiscoIpConfigurationClient:configure Response get: " + telnetClient.send("tunnel mode gre ip \r"));
			System.out.println("CiscoIpConfigurationClient:configure Response get: " + telnetClient.send("end \r"));
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
			System.out.println("CiscoIpConfigurationClient:configure Response get: " + telnetClient.send(desc.getAuthInfo().getUserID() + "\r"));
			System.out.println("CiscoIpConfigurationClient:configure Response get: " + telnetClient.send(desc.getAuthInfo().getPasswd() + "\r"));
			
			String response = telnetClient.send("\n\r");
			System.out.println("CiscoIpConfigurationClient:configure Response get: " + response );

			System.out.println("CiscoIpConfigurationClient:configure Response get: " + telnetClient.send("enable \r"));
			System.out.println("CiscoIpConfigurationClient:configure Response get: " + telnetClient.send("configure terminal \r"));
			if (rDesc.getOperation().toLowerCase().compareTo("add") == 0) {
				if (rDesc.getNextHopIP().length()>4) {
				System.out.println("CiscoIpConfigurationClient:configure Response get: " + telnetClient.send("ip route " + rDesc.getDestIP() + " " + rDesc.getDestSubnetDotFormat() + " " +rDesc.getNextHopIP() +  "\r"));
			
				}
				else {
					System.out.println("CiscoIpConfigurationClient:configure Response get: " + telnetClient.send("ip route " + rDesc.getDestIP() + " " + rDesc.getDestSubnetDotFormat() + " " +rDesc.getDestIFID() +  "\r"));					
				}
			}
			else if (rDesc.getOperation().toLowerCase().compareTo("drop") == 0){
				System.out.println("CiscoIpConfigurationClient:configure Response get: " + telnetClient.send("no ip route " + rDesc.getDestIP() + " " + rDesc.getDestSubnetDotFormat() + "\r"));					
			}
			else
			{
				telnetClient.close();
				return -1;
			}
			System.out.println("CiscoIpConfigurationClient:configure Response get: " + telnetClient.send("end \r"));
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
	public int configureACLStaticRoute(RouterDesc desc, ACLDesc aclDesc,
			ForwardingRuleDesc ruleDesc) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int configureLabelSwitchedPath(RouterDesc desc, LabelSwitchedPath lsp) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int configureLabelSwitchedPathWithUnnIf(RouterDesc desc,
			LabelSwitchedPathWithUnnumIf lsp) {
		// TODO Auto-generated method stub
		return 0;
	}

}
