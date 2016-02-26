package es.tid.pce.computingEngine.algorithms.multiLayer;

public class OperationsCounter {
	
	private int num_op1;
	private int num_op2;
	private int num_op3;
	private int num_op4;
	private double mean_layer_changes;
	private int total_layer_changes;
	private double mean_layer_changes_OP4;
	private int total_layer_changes_OP4;
	private double traffic_hops;
	private double mean_traffic_hops;
	
	public OperationsCounter(){
		num_op1 = 0;
		num_op2 = 0;
		num_op3 = 0;
		num_op4 = 0;
		mean_layer_changes = 0;
		total_layer_changes = 0;
		traffic_hops = 0;
		mean_traffic_hops = 0;
	}
	
	public int getNum_op1() {
		return num_op1;
	}

	public void setNum_op1() {
		num_op1 = num_op1 + 1;
	}

	public int getNum_op2() {
		return num_op2;
	}

	public void setNum_op2() {
		num_op2 = num_op2 + 1;
	}

	public int getNum_op3() {
		return num_op3;
	}

	public void setNum_op3() {
		num_op3 = num_op3 + 1;
	}

	public int getNum_op4() {
		return num_op4;
	}

	public void setNum_op4() {
		num_op4 = num_op4 + 1;
	}

	public double getMean_layer_changes() {
		return mean_layer_changes;
	}

	public void setMean_layer_changes() {
		mean_layer_changes = (double)((double)total_layer_changes/((double)(num_op1 + num_op2 + num_op3 + num_op4)));
	}

	public int getTotal_layer_changes() {
		return total_layer_changes;
	}

	public void setTotal_layer_changes(int LayerChanges) {
		total_layer_changes = total_layer_changes + LayerChanges;
	}

	public double getTraffic_hops() {
		return traffic_hops;
	}

	public void setTraffic_hops(double trafficHops) {
		traffic_hops = traffic_hops + trafficHops;
	}

	public double getMean_traffic_hops() {
		return mean_traffic_hops;
	}

	public void setMean_traffic_hops(double meanTrafficHops) {
		mean_traffic_hops = (double)((double)traffic_hops/((double)(num_op1 + num_op2 + num_op3 + num_op4)));
	}

	public double getMean_layer_changes_OP4() {
		return mean_layer_changes_OP4;
	}

	public void setMean_layer_changes_OP4(double meanLayerChangesOP4) {
		mean_layer_changes_OP4 = (double)((double)total_layer_changes_OP4/((double)(num_op4)));
	}

	public int getTotal_layer_changes_OP4() {
		return total_layer_changes_OP4;
	}

	public void setTotal_layer_changes_OP4(int totalLayerChangesOP4) {
		total_layer_changes_OP4 = total_layer_changes_OP4 + totalLayerChangesOP4;
	}
	
	
}
