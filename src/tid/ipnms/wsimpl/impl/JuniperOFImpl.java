package tid.ipnms.wsimpl.impl;

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
import tid.provisioningManager.objects.openflow.PushFlowController;
import tid.provisioningManager.objects.openflow.PushFlowFloodlight;
import tid.provisioningManager.objects.openflow.StaticFlow;

public class JuniperOFImpl  implements IPnmsWs {
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
		/*Comamands are sent to the controller*/
		/*We need to find the MAC adddress of each router*/
		/*Test lest send a curl*/
		StaticFlow staticFlow = new StaticFlow();
		staticFlow.setSwitchID("00:00:00:a0:a5:62:7b:4e");
		staticFlow.setName("flow-mod-2");
		staticFlow.setPriority(32768);
		staticFlow.setIngressPort("2");
		staticFlow.setActive(true);
		staticFlow.setActions("output=1");		
		StaticFlow staticFlow2 = new StaticFlow();
		staticFlow2.setSwitchID("00:00:00:a0:a5:62:7b:4e");
		staticFlow2.setName("flow-mod-1");
		staticFlow2.setPriority(32768);
		staticFlow2.setIngressPort("1");
		staticFlow2.setActive(true);
		staticFlow2.setActions("output=2");
		PushFlowController pushFlow = new PushFlowFloodlight(staticFlow,"172.16.1.3","8888");
		log.info("Sending first Request");
		pushFlow.sendRequest();
		PushFlowController pushFlow2 = new PushFlowFloodlight(staticFlow2,"172.16.1.3","8888");
		log.info("Sending second Request");
		pushFlow2.sendRequest();
				
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
