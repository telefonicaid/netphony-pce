package tid.ipnms.wsimpl;

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

public interface IPnmsWs {
	
	/**Service endpoint to configure IP interface*/
	public int configureIPInterface(RouterDesc desc, IPInterfaceDesc ifDesc, IPInterfaceConfig config);
	
	
	/**Service endpoint to create a GRE tunnel Interface*/
	public int createGREInterface (RouterDesc desc, GRETunnelDesc tunnDesc);
	
	/**Service endpoint to configure the routing protocol*/
	public int configureRoutingProtocol(RouterDesc desc, RProtocolDesc rDesc);

	/**Service endpoint to configure a Static Route*/
	public int configureStaticRoute(RouterDesc desc, StaticRouteDesc rDesc);

	
	/**Service endpoint to configure an ACL based static forwarding rule*/
	public int configureACLStaticRoute(RouterDesc desc, ACLDesc aclDesc, ForwardingRuleDesc ruleDesc);


	/**Service endpoint to configure a Label Switched Path*/
	public int configureLabelSwitchedPath(RouterDesc desc, LabelSwitchedPath lsp);
	
	/**Service endpoint to configure a Label Switched Path*/
	public int configureLabelSwitchedPathWithUnnIf(RouterDesc desc, LabelSwitchedPathWithUnnumIf lsp);
	
}
