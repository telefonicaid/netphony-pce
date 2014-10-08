package tid.netManager.emulated;

import java.util.LinkedList;

import es.tid.pce.pcep.objects.BandwidthRequestedGeneralizedBandwidth;
import es.tid.rsvp.objects.subobjects.EROSubobject;
import tid.netManager.NetworkLSPManager;
import tid.netManager.NetworkLSPManagerTypes;

public class EmulatedUniNetworkLSPManager extends NetworkLSPManager{
	public EmulatedUniNetworkLSPManager(){
		this.setEmulatorType(NetworkLSPManagerTypes.UNI_EMULATED_NETWORK);		
	}
	@Override
	public boolean setLSP(LinkedList<EROSubobject> erolist, boolean bidirect, BandwidthRequestedGeneralizedBandwidth GB) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean setMLLSP(LinkedList<EROSubobject> erolist, boolean bidirect, BandwidthRequestedGeneralizedBandwidth GB) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void removeLSP(LinkedList<EROSubobject> erolist, boolean bidirect, BandwidthRequestedGeneralizedBandwidth GB) {
		// TODO Auto-generated method stub
		
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
