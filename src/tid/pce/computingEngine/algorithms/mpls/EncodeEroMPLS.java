package tid.pce.computingEngine.algorithms.mpls;

import java.net.Inet4Address;
import java.util.List;

import es.tid.pce.pcep.objects.ExplicitRouteObject;
import es.tid.rsvp.objects.subobjects.IPv4prefixEROSubobject;
import tid.pce.tedb.IntraDomainEdge;

public class EncodeEroMPLS {
	public static void createEroMpls(ExplicitRouteObject ero, List<IntraDomainEdge> edge_list){
		int i;
		for (i=0;i<edge_list.size();i++){
			IPv4prefixEROSubobject eroso= new IPv4prefixEROSubobject();
			eroso.setIpv4address((Inet4Address)edge_list.get(i).getSrc_Numif_id());			
			eroso.setPrefix(30);
			eroso.setLoosehop(false);
			ero.addEROSubobject(eroso);
		}
		IPv4prefixEROSubobject eroso= new IPv4prefixEROSubobject();
		eroso.setIpv4address((Inet4Address)edge_list.get(edge_list.size()-1).getTarget());
		eroso.setPrefix(32);
		ero.addEROSubobject(eroso);
	}
}
