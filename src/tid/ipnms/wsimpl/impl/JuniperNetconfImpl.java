package tid.ipnms.wsimpl.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Logger;

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

public class JuniperNetconfImpl implements IPnmsWs {
	private Logger log=Logger.getLogger("JuniperOFImpl");
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
		log.info("CONFIGURING ROUTE");
		try {
    		String osName = System.getProperty("os.name");
    		String[] cmd = new String[7];
    		cmd[0] = "/bin/sh"; // should exist on all POSIX systems
    		/*Script Name*/
    		cmd[1] = "netconf_script.sh"; 
    		cmd[2] = desc.getAuthInfo().getUserID();
    		cmd[3] =desc.getAuthInfo().getPasswd();    		
    		cmd[4] = rDesc.getDestIP() + "/" + rDesc.getDestSubnet();
    		cmd[5] = rDesc.getNextHopIP();
    		cmd[6] = desc.getManagementAddress().getHostAddress();    		
    		log.info("Execing " + cmd[0] + " " + cmd[1] + " " + cmd[2] + " " +cmd[3] + " " +cmd[4] + " " +cmd[5] + " " + cmd[6]);
    		Runtime rt = Runtime.getRuntime();    		
    		Process proc = rt.exec(cmd);
    		// any error message?
    		StreamGobbler errorGobbler = new StreamGobbler(proc
    				.getErrorStream(), "ERROR");

    		// any output?
    		StreamGobbler outputGobbler = new StreamGobbler(proc
    				.getInputStream(), "OUTPUT");

    		// kick them off
    		errorGobbler.start();
    		outputGobbler.start();

    		// any error???
    		int exitVal = proc.waitFor();
    		System.out.println("ExitValue: " + exitVal);
    	} catch (Throwable t) {
    		t.printStackTrace();
    	}
	
				
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
	static class StreamGobbler extends Thread {
    	InputStream is;

    	String type;

    	StreamGobbler(InputStream is, String type) {
    		this.is = is;
    		this.type = type;
    	}

    	public void run() {
    		try {
    			InputStreamReader isr = new InputStreamReader(is);
    			BufferedReader br = new BufferedReader(isr);
    			String line = null;
    			while ((line = br.readLine()) != null)
    				System.out.println(type + ">" + line);
    		} catch (IOException ioe) {
    			ioe.printStackTrace();
    		}
    	}
    }
}