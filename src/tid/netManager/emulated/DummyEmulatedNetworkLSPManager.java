package tid.netManager.emulated;


import java.util.LinkedList;
import java.util.logging.Logger;

import es.tid.pce.pcep.objects.BandwidthRequestedGeneralizedBandwidth;
import es.tid.rsvp.objects.subobjects.EROSubobject;
import tid.netManager.NetworkLSPManager;
import tid.netManager.NetworkLSPManagerTypes;


public class DummyEmulatedNetworkLSPManager extends NetworkLSPManager{

	 boolean multilayer=false;
	 private Logger log= Logger.getLogger("PCCClient");
	public DummyEmulatedNetworkLSPManager(){
		this.setEmulatorType(NetworkLSPManagerTypes.DUMMY_EMULATED_NETWORK);	

		

		
	}
	@Override
	public boolean setLSP(LinkedList<EROSubobject> erolist, boolean bidirect, BandwidthRequestedGeneralizedBandwidth GB) {
		// TODO Auto-generated method stub
		log.info("Setting LSP with ERO: "+erolist.toString());		
	

		return true;
	}

	@Override
	public boolean setMLLSP(LinkedList<EROSubobject> erolist, boolean bidirect, BandwidthRequestedGeneralizedBandwidth GB) {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public void removeLSP(LinkedList<EROSubobject> erolist, boolean bidirect, BandwidthRequestedGeneralizedBandwidth GB) {
		// TODO Auto-generated method stub
		log.info("REmoving LSP with ERO: "+erolist.toString());
	}

	@Override
	public void removeMLLSP(LinkedList<EROSubobject> erolist, boolean bidirect, BandwidthRequestedGeneralizedBandwidth GB) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void removeLSP(LinkedList<EROSubobject> erolist, boolean bidirect,
			BandwidthRequestedGeneralizedBandwidth GB, float bw) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public boolean setLSP_UpperLayer(
			LinkedList<EROSubobject> eROSubobjectListIP, float bw,
			boolean bidirect) {
		// TODO Auto-generated method stub
		return false;
	}

}
