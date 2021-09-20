package es.tid.pce.utils;

import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.StringTokenizer;

import es.tid.pce.pcep.constructs.NAIIPv4Adjacency;
import es.tid.pce.pcep.constructs.NAIIPv4NodeID;
import es.tid.pce.pcep.objects.EndPoints;
import es.tid.pce.pcep.objects.EndPointsIPv4;
import es.tid.pce.pcep.objects.ExplicitRouteObject;
import es.tid.pce.pcep.objects.subobjects.SREROSubobject;
import es.tid.rsvp.objects.subobjects.IPv4prefixEROSubobject;

/**
 * Set of useful methods to convert from String to PCEP Objects such as Explicit
 * Route Object or End Points
 * 
 * @author ogondio
 *
 */
public class StringToPCEP {

	public StringToPCEP() {

	}

	public static EndPoints stringToEndPoints(String endPointsString) {
		StringTokenizer st = new StringTokenizer(endPointsString, " ");
		EndPointsIPv4 ep = new EndPointsIPv4();
		String src_ip = st.nextToken();
		// String src_ip= "1.1.1.1";
		Inet4Address ipp;
		try {
			ipp = (Inet4Address) Inet4Address.getByName(src_ip);
			((EndPointsIPv4) ep).setSourceIP(ipp);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// System.out.println(" - Destination IP address: ");
		// br2 = new BufferedReader(new InputStreamReader(System.in));
		// String dst_ip="172.16.101.101";
		Inet4Address i_d;
		String dst_ip = st.nextToken();
		try {
			i_d = (Inet4Address) Inet4Address.getByName(dst_ip);
			((EndPointsIPv4) ep).setDestIP(i_d);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return ep;
	}

	public static ExplicitRouteObject stringToExplicitRouteObject(String eroString) {
		SREROSubobject ero2 = null;
		StringTokenizer st = new StringTokenizer(eroString, " ");

		ExplicitRouteObject ero = new ExplicitRouteObject();

		while (st.hasMoreTokens()) {
			try {
				String ero1 = st.nextToken();
				Inet4Address eru;
				if(ero1.startsWith("nn")) {
					
					if(ero2.equals(null)) {
						ero2 = new SREROSubobject();
						ero.getEROSubobjectList().add(ero2);
						ero2.setSflag(true);
					}
					
					ero2.setNT(1);
					eru = (Inet4Address) Inet4Address.getByName(ero1.substring(2));
					NAIIPv4NodeID nai= new NAIIPv4NodeID();
					
					nai.setNodeID(eru);
					ero2.setFflag(false);
					ero2.setNai(nai);
					ero2 = null;
					
				}else if(ero1.startsWith("na")) {
					NAIIPv4Adjacency naia = new NAIIPv4Adjacency();
					
				
					String dirs[]= ero1.substring(2).split("-");
					eru = (Inet4Address) Inet4Address.getByName(dirs[0]);
					naia.setLocalNodeAddress(eru);
					eru = (Inet4Address) Inet4Address.getByName(dirs[1]);
					naia.setRemoteNodeAddress(eru);
					
					ero2.setFflag(false);
					naia.setNaiType(3);
					
					ero2.setNai(naia);
					ero2.setNT(0);
					
					
				}else if (ero1.contains("/")) {
					// There is / hence, prefix might be present
					StringTokenizer st2 = new StringTokenizer(ero1, "/");
					String ip = st2.nextToken();
					eru = (Inet4Address) Inet4Address.getByName(ip);
					IPv4prefixEROSubobject eroso = new IPv4prefixEROSubobject();
					eroso.setIpv4address(eru);
					int prefix = Integer.parseInt(st2.nextToken());
					eroso.setPrefix(prefix);
					// TODO: HACER LOOSE OF FALSE
					eroso.setLoosehop(false);
					ero.getEROSubobjectList().add(eroso);
				}else if (ero1.contains(".")) {

					eru = (Inet4Address) Inet4Address.getByName(ero1);
					IPv4prefixEROSubobject eroso = new IPv4prefixEROSubobject();
					eroso.setIpv4address(eru);
					eroso.setPrefix(32);
					eroso.setLoosehop(false);
					ero.getEROSubobjectList().add(eroso);
				}else {
				
					ero2 = new SREROSubobject();
					if(ero1.startsWith("m")) {
						long sid = Long.parseLong(ero1.substring(1));
						
//						ero2.setNT(1);
						
						ero2.setMflag(true);
						ero2.setSID(sid);
						ero2.setLoosehop(false);
						ero.getEROSubobjectList().add(ero2);
					}else {
						long sid = Long.parseLong(ero1);
					ero2.setSID(sid);
					ero2.setLoosehop(false);
					ero.getEROSubobjectList().add(ero2);
					}
				}

			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return ero;
	}
}
