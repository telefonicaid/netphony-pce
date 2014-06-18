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
//import com.tubs.ida.one.utilities.connectors.telnet.IdaTelnetClientPrueba;

public class IPnmsALUCliImpl implements IPnmsWs {

	
	public int checkConnectivity(RouterDesc desc){
		
		IdaTelnetClientPrueba telnetClient = new IdaTelnetClientPrueba(true, new String[] {"#", ":", ">"});
		
		try {
			
			telnetClient.connect(desc.getManagementAddress().getHostAddress(), (int) desc.getConfigurationPort());
			
			//login to the router
			
			//System.out.println("Setting login\r");
			
/*			telnetClient.send(desc.getAuthInfo().getUserID());
			System.out.println("\n");
			telnetClient.send("\r\n");
			telnetClient.send(desc.getAuthInfo().getPasswd());*/
			
			
			
			//System.out.println(telnetClient.send(desc.getAuthInfo().getUserID()));
			
			System.out.println("Command Sent to ALU:\t\t Response from ALU: " + telnetClient.send(desc.getAuthInfo().getUserID() + "\r"));
			System.out.println("Command Sent to ALU:\t\t Response from ALU: " + telnetClient.send(desc.getAuthInfo().getPasswd() + "\r"));
			
			String response = telnetClient.send("\n\r");
			
			System.out.println("Command Sent to ALU:\t\t Response from ALU: " + response );
			System.out.println("Command Sent to ALU:\t\t Response from ALU: " + telnetClient.send("admin display-config \r"));
			
			
			//telnetClient.close();

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

	public int ePipeShutdown(RouterDesc desc, int id){
		
		IdaTelnetClientPrueba telnetClient = new IdaTelnetClientPrueba(true, new String[] {"#", ":", ">"});
		
		try {
			
			telnetClient.connect(desc.getManagementAddress().getHostAddress(), (int) desc.getConfigurationPort());
			
			//login to the router
			
			
			
			System.out.println("Command Sent to ALU:\t\t Response from ALU: " + telnetClient.send(desc.getAuthInfo().getUserID() + "\r"));
			System.out.println("Command Sent to ALU:\t\t Response from ALU: " + telnetClient.send(desc.getAuthInfo().getPasswd() + "\r"));
			
			String response = telnetClient.send("\n\r");
			
			System.out.println("Command Sent to ALU:\t\t Response from ALU: " + response );
			System.out.println("Command Sent to ALU:\t\t Response from ALU: " + telnetClient.send("configure service epipe "+id+" shutdown \r"));
			
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

	public int ePipeWakeUp(RouterDesc desc, int id){
		
		IdaTelnetClientPrueba telnetClient = new IdaTelnetClientPrueba(true, new String[] {"#", ":", ">"});
		
		try {
			
			telnetClient.connect(desc.getManagementAddress().getHostAddress(), (int) desc.getConfigurationPort());
			
			//login to the router
			
			
			
			System.out.println("Command Sent to ALU:\t\t Response from ALU: " + telnetClient.send(desc.getAuthInfo().getUserID() + "\r"));
			System.out.println("Command Sent to ALU:\t\t Response from ALU: " + telnetClient.send(desc.getAuthInfo().getPasswd() + "\r"));
			
			String response = telnetClient.send("\n\r");
			
			System.out.println("Command Sent to ALU:\t\t Response from ALU: " + response );
			System.out.println("Command Sent to ALU:\t\t Response from ALU: " + telnetClient.send("configure service epipe "+id+" no shutdown \r"));
			
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
	public int configureIPInterface(RouterDesc desc, IPInterfaceDesc ifDesc,
			IPInterfaceConfig config) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int createGREInterface(RouterDesc desc, GRETunnelDesc tunnDesc) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int configureRoutingProtocol(RouterDesc desc, RProtocolDesc rDesc) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int configureStaticRoute(RouterDesc desc, StaticRouteDesc rDesc) {
		// TODO Auto-generated method stub
		return 0;
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
