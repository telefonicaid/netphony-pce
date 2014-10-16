package tid.netManager.uni;


import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

import es.tid.rsvp.objects.subobjects.UnnumberIfIDEROSubobject;
import tid.ipnms.datamodel.misc.AuthInfo;
import tid.ipnms.datamodel.router.RouterDesc;
import tid.ipnms.datamodel.router.LabelSwitchedPath.LabelSwitchedPath;
import tid.ipnms.datamodel.router.LabelSwitchedPath.LabelSwitchedPathProperties;
import tid.ipnms.datamodel.router.LabelSwitchedPath.LabelSwitchedPathWithUnnumIf;
import tid.ipnms.datamodel.router.routing.StaticRouteDesc;
import tid.ipnms.wsimpl.IPnmsWs;
import tid.ipnms.wsimpl.impl.IPnmsJuniperCliImpl;

public class NoIPNMSDispatcher{

/*	public NoIPNMSDispatcher(Core core){
		
		super(core);
				
	}*/
	
	public int changeRoute(ChangeIPRoute cr){
		
		IPnmsWs ws = new IPnmsJuniperCliImpl();
		
		RouterDesc desc = new RouterDesc();
		desc.setRouterID("1");
		desc.setRouterType("juniper");
		desc.setIosVersion("1");
		//Address confAddress = new Address();
		//System.out.println("Node to configure: "+cr.getNodeToChange().getHostName());
		//confAddress.setIpv4Address(cr.getNodeToChange().getHostName());
		//confAddress.setPort(23);
		desc.setConfigurationPort(23);
		desc.setManagementAddress(cr.getNodeToChange());
		AuthInfo info = new AuthInfo();
		info.setPasswd("Juniper");
		info.setUserID("tid");
		desc.setAuthInfo(info);
		desc.setPhyDesc("");
		
		StaticRouteDesc srd = new StaticRouteDesc();
		srd.setDestIP(cr.getDestination().getHostName());
		srd.setDestSubnet("24");
		srd.setOperation("change");
		
		srd.setNextHopIP(cr.getNextNextHop().toString().substring(1));

		ws.configureStaticRoute(desc,srd);
			
		/*srd.setOperation("drop");
		
		System.out.println("NEXT HOP: "+cr.getNextNextHop());
	
		System.out.println("Starting ws");
	*/	//return 1;
		return ws.configureStaticRoute(desc, srd);
		
				
	}
		
	public int createLSP(CreateLSP clsp){
		
		IPnmsWs ws = new IPnmsJuniperCliImpl();
		
		//Router Description 
		RouterDesc desc = new RouterDesc();
		desc.setRouterID("1");
		desc.setRouterType("juniper");
		desc.setIosVersion("1");
		//Address confAddress = new Address();
		//System.out.println("Node to configure: "+clsp.getNodeToChange().getHostName());
		//confAddress.setIpv4Address(clsp.getNodeToChange().getHostName());
		//confAddress.setPort(23);
		//desc.setConfAddress(confAddress);
		desc.setConfigurationPort(23);
		desc.setManagementAddress(clsp.getNodeToChange());
		AuthInfo info = new AuthInfo();
		info.setPasswd("Juniper");
		info.setUserID("tid");
		desc.setAuthInfo(info);
		desc.setPhyDesc("");
		
		
		String signalBandwidth = "10gigether";
		String switchingType = "lambda";
		String encodingType = "";
		String gpid = "";
		
		LabelSwitchedPathProperties lspProp = new LabelSwitchedPathProperties(signalBandwidth, switchingType, encodingType, gpid);
		
		String lspId = clsp.getLspName();
		String source = clsp.getPath().getFirst().toString();
		source = source.substring(source.indexOf("/")+1);
		String destination = clsp.getPath().getLast().toString();
		destination = destination.substring(destination.indexOf("/")+1);
		String pathName = new String("path_"+clsp.getLspName());
		
		LabelSwitchedPath lsp = new LabelSwitchedPath(lspId,source,destination,pathName,lspProp);
		
		lsp.setOperation("add");
		
		for(int i = 1; i < clsp.getPath().size()-1; i++){
			
			lsp.getPath().add(clsp.getPath().get(i));			
			
		}
		
		return ws.configureLabelSwitchedPath(desc, lsp);
			
	}

	public int deleteLSP(DeleteLSP dlsp){
		
		IPnmsWs ws = new IPnmsJuniperCliImpl();
		
		//Router Description 
		RouterDesc desc = new RouterDesc();
		desc.setRouterID("1");
		desc.setRouterType("juniper");
		desc.setIosVersion("1");
		//Address confAddress = new Address();
		//System.out.println("Node to configure: "+dlsp.getNodeToChange().getHostName());
		//confAddress.setIpv4Address(dlsp.getNodeToChange().getHostName());
		//confAddress.setPort(23);
		//desc.setConfAddress(confAddress);
		desc.setConfigurationPort(23);
		desc.setManagementAddress(dlsp.getNodeToChange());
		AuthInfo info = new AuthInfo();
		info.setPasswd("Juniper");
		info.setUserID("tid");
		desc.setAuthInfo(info);
		desc.setPhyDesc("");
		
		
		String signalBandwidth = "10gigether";
		String switchingType = "lambda";
		String encodingType = "";
		String gpid = "";
		
		LabelSwitchedPathProperties lspProp = new LabelSwitchedPathProperties(signalBandwidth, switchingType, encodingType, gpid);
		
		String lspId = dlsp.getLspName();
		String source = dlsp.getPath().getFirst().toString();
		source = source.substring(source.indexOf("/")+1);
		String destination = dlsp.getPath().getLast().toString();
		destination = destination.substring(destination.indexOf("/")+1);
		String pathName = new String("path_"+dlsp.getLspName());
		
		LabelSwitchedPath lsp = new LabelSwitchedPath(lspId,source,destination,pathName,lspProp);
		
		lsp.setOperation("delete");
		
		for(int i = 1; i < dlsp.getPath().size()-1; i++){
			
			lsp.getPath().add(dlsp.getPath().get(i));			
			
		}
		
		return ws.configureLabelSwitchedPath(desc, lsp);
			
	}
	
	
	public int createLSP(LabelSwitchedPathWithUnnumIf lsp){
		
		IPnmsWs ws = new IPnmsJuniperCliImpl();
		RouterDesc desc = new RouterDesc();
		desc.setRouterID("1");
		desc.setRouterType("juniper");
		desc.setIosVersion("1");
		desc.setConfigurationPort(23);
		
		// Transformar de IP Control a IP Gestion
		
		try {
			desc.setManagementAddress((Inet4Address)InetAddress.getByName("10.95.73.74"));
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		AuthInfo info = new AuthInfo();
		info.setPasswd("Juniper");
		info.setUserID("tid");
		desc.setAuthInfo(info);
		desc.setPhyDesc("");
		
		for (int i=0;i<lsp.getPath().size();i++){
			
			System.out.println(lsp.getPath().get(i));
			
			
		}
		

		// Código provisional para adaptacion
		
		String signalBandwidth = "gigether";
		String switchingType = "lambda";
		String encodingType = "";
		String gpid = "";
		
		LabelSwitchedPathProperties lspProp = new LabelSwitchedPathProperties(signalBandwidth, switchingType, encodingType, gpid);
				
		String lspId = lsp.getLspId();
		String source = lsp.getSource();
		source = source.substring(source.indexOf("/")+1);
		String destination = lsp.getDestination();
		destination = destination.substring(destination.indexOf("/")+1);
		String pathName = new String("path_"+lsp.getLspId());
		
		LabelSwitchedPath lsp2 = new LabelSwitchedPath(lspId,source,destination,pathName,lspProp);
		
		lsp2.setOperation("add");
		
		try{
			lsp2.getPath().add((Inet4Address)InetAddress.getByName("20.20.20.2"));
			lsp2.getPath().add((Inet4Address)InetAddress.getByName("20.20.20.1"));
		}catch(Exception e){
			e.printStackTrace();
			
		}		
		for(int i = 1; i < lsp.getPath().size()-1; i++){
			
			lsp2.getPath().add(((UnnumberIfIDEROSubobject)lsp.getPath().get(i)).getRouterID());			
			
		}
		try{
			lsp2.getPath().add((Inet4Address)InetAddress.getByName("41.41.41.1"));
			lsp2.getPath().add((Inet4Address)InetAddress.getByName("41.41.41.2"));
		}catch(Exception e){
			e.printStackTrace();
			
		}
		
		ws.configureLabelSwitchedPath(desc,lsp2);
			
		//ws.configureLabelSwitchedPathWithUnnIf(desc, lsp);
		
		return 0;
		
		
	}
	
	public int deleteLSP(LabelSwitchedPathWithUnnumIf lsp){
		
		IPnmsWs ws = new IPnmsJuniperCliImpl();
		RouterDesc desc = new RouterDesc();
		desc.setRouterID("1");
		desc.setRouterType("juniper");
		desc.setIosVersion("1");
		desc.setConfigurationPort(23);
		
		// Transformar de IP Control a IP Gestion
		
		try {
			desc.setManagementAddress((Inet4Address)InetAddress.getByName("10.95.73.74"));
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		AuthInfo info = new AuthInfo();
		info.setPasswd("Juniper");
		info.setUserID("tid");
		desc.setAuthInfo(info);
		desc.setPhyDesc("");
		
		for (int i=0;i<lsp.getPath().size();i++){
			
			System.out.println(lsp.getPath().get(i));
			
			
		}
		

		// Código provisional para adaptacion
		
		String signalBandwidth = "gigether";
		String switchingType = "lambda";
		String encodingType = "";
		String gpid = "";
		
		LabelSwitchedPathProperties lspProp = new LabelSwitchedPathProperties(signalBandwidth, switchingType, encodingType, gpid);
				
		String lspId = lsp.getLspId();
		String source = lsp.getSource();
		source = source.substring(source.indexOf("/")+1);
		String destination = lsp.getDestination();
		destination = destination.substring(destination.indexOf("/")+1);
		String pathName = new String("path_"+lsp.getLspId());
		
		LabelSwitchedPath lsp2 = new LabelSwitchedPath(lspId,source,destination,pathName,lspProp);
		
		lsp2.setOperation("delete");
		
		try{
			lsp2.getPath().add((Inet4Address)InetAddress.getByName("20.20.20.2"));
			lsp2.getPath().add((Inet4Address)InetAddress.getByName("20.20.20.1"));
		}catch(Exception e){
			e.printStackTrace();
			
		}		
		for(int i = 1; i < lsp.getPath().size()-1; i++){
			
			lsp2.getPath().add(((UnnumberIfIDEROSubobject)lsp.getPath().get(i)).getRouterID());			
			
		}
		try{
			lsp2.getPath().add((Inet4Address)InetAddress.getByName("41.41.41.1"));
			lsp2.getPath().add((Inet4Address)InetAddress.getByName("41.41.41.2"));
		}catch(Exception e){
			e.printStackTrace();
			
		}
		
		ws.configureLabelSwitchedPath(desc,lsp2);
			
		//ws.configureLabelSwitchedPathWithUnnIf(desc, lsp);
		
		return 0;
		
		
	}
	
	public void enableLSP(String lspName){
		
		IPnmsJuniperCliImpl ws = new IPnmsJuniperCliImpl();
		RouterDesc desc = new RouterDesc();
		desc.setRouterID("1");
		desc.setRouterType("juniper");
		desc.setIosVersion("1");
		desc.setConfigurationPort(23);
		
		// Transformar de IP Control a IP Gestion
		
		try {
			desc.setManagementAddress((Inet4Address)InetAddress.getByName("10.95.73.74"));
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		AuthInfo info = new AuthInfo();
		info.setPasswd("Juniper");
		info.setUserID("tid");
		desc.setAuthInfo(info);
		desc.setPhyDesc("");
		ws.enableLSP(desc,lspName);
		
	}

	public void disableLSP(String lspName){
		
		IPnmsJuniperCliImpl ws = new IPnmsJuniperCliImpl();
		RouterDesc desc = new RouterDesc();
		desc.setRouterID("1");
		desc.setRouterType("juniper");
		desc.setIosVersion("1");
		desc.setConfigurationPort(23);
		
		// Transformar de IP Control a IP Gestion
		
		try {
			desc.setManagementAddress((Inet4Address)InetAddress.getByName("10.95.73.74"));
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		AuthInfo info = new AuthInfo();
		info.setPasswd("Juniper");
		info.setUserID("tid");
		desc.setAuthInfo(info);
		desc.setPhyDesc("");
		ws.disableLSP(desc,lspName);
		
	}
		
	
	public void increaseLDPWeightMX1(){
		
		IPnmsJuniperCliImpl ws = new IPnmsJuniperCliImpl();
		RouterDesc desc = new RouterDesc();
		desc.setRouterID("1");
		desc.setRouterType("juniper");
		desc.setIosVersion("1");
		desc.setConfigurationPort(23);
		
		// Transformar de IP Control a IP Gestion
		
		try {
			desc.setManagementAddress((Inet4Address)InetAddress.getByName("10.95.73.72"));
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		AuthInfo info = new AuthInfo();
		info.setPasswd("Juniper");
		info.setUserID("tid");
		desc.setAuthInfo(info);
		desc.setPhyDesc("");
		ws.increaseLDPWeightMX1(desc);
		
	}

	public void decreaseLDPWeightMX1(){
		
		IPnmsJuniperCliImpl ws = new IPnmsJuniperCliImpl();
		RouterDesc desc = new RouterDesc();
		desc.setRouterID("1");
		desc.setRouterType("juniper");
		desc.setIosVersion("1");
		desc.setConfigurationPort(23);
		
		// Transformar de IP Control a IP Gestion
		
		try {
			desc.setManagementAddress((Inet4Address)InetAddress.getByName("10.95.73.72"));
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		AuthInfo info = new AuthInfo();
		info.setPasswd("Juniper");
		info.setUserID("tid");
		desc.setAuthInfo(info);
		desc.setPhyDesc("");
		ws.decreaseLDPWeightMX1(desc);
		
	}

	public void increaseLDPWeightMX3(){
		
		IPnmsJuniperCliImpl ws = new IPnmsJuniperCliImpl();
		RouterDesc desc = new RouterDesc();
		desc.setRouterID("1");
		desc.setRouterType("juniper");
		desc.setIosVersion("1");
		desc.setConfigurationPort(23);
		
		// Transformar de IP Control a IP Gestion
		
		try {
			desc.setManagementAddress((Inet4Address)InetAddress.getByName("10.95.73.74"));
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		AuthInfo info = new AuthInfo();
		info.setPasswd("Juniper");
		info.setUserID("tid");
		desc.setAuthInfo(info);
		desc.setPhyDesc("");
		ws.increaseLDPWeightMX3(desc);
		
	}

	public void decreaseLDPWeightMX3(){
		
		IPnmsJuniperCliImpl ws = new IPnmsJuniperCliImpl();
		RouterDesc desc = new RouterDesc();
		desc.setRouterID("1");
		desc.setRouterType("juniper");
		desc.setIosVersion("1");
		desc.setConfigurationPort(23);
		
		// Transformar de IP Control a IP Gestion
		
		try {
			desc.setManagementAddress((Inet4Address)InetAddress.getByName("10.95.73.74"));
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		AuthInfo info = new AuthInfo();
		info.setPasswd("Juniper");
		info.setUserID("tid");
		desc.setAuthInfo(info);
		desc.setPhyDesc("");
		ws.decreaseLDPWeightMX3(desc);
		
	}
	
}
