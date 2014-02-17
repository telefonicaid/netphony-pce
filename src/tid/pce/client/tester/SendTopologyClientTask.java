package tid.pce.client.tester;

import java.util.TimerTask;

import tid.netManager.NetworkLSPManager;

public class SendTopologyClientTask extends TimerTask {
	private NetworkLSPManager networkLSPManager;
	SendTopologyClientTask( NetworkLSPManager networkLSPManager){
		this.networkLSPManager=networkLSPManager;
	}
	@Override
	public void run() {
		// TODO Auto-generated method stub

		networkLSPManager.sendAllTopology();
	}

}
