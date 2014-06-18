package tid.pce.computingEngine.algorithms.sson;

import java.util.LinkedList;
import java.util.logging.Logger;

import tid.pce.computingEngine.ComputingResponse;
import tid.pce.computingEngine.algorithms.AlgorithmReservation;
import tid.pce.pcep.objects.GeneralizedBandwidthSSON;
import tid.pce.pcep.objects.ObjectParameters;
import tid.pce.pcep.objects.Reservation;
import tid.pce.pcep.objects.ReservationConf;
import tid.pce.server.wson.ReservationManager;

public class GenericLambdaReservation implements AlgorithmReservation{

	private ComputingResponse resp;
	private LinkedList<Object> sourceVertexList=new LinkedList<Object>();
	private LinkedList<Object> targetVertexList=new LinkedList<Object>();
	private Reservation reservation;
	private Logger log;
	private int lambda_chosen;
	private ReservationManager reservationManager;
	private boolean bidirectional;
	
	public boolean isBidirectional() {
		return bidirectional;
	}

	public void setBidirectional(boolean bidirectional) {
		this.bidirectional = bidirectional;
	}

	public GenericLambdaReservation(){
		log=Logger.getLogger("PCEServer");
	}
	
	public ComputingResponse call() throws Exception {
		if (reservation!=null){
			if (resp.getResponse(0).getPath(0).getGeneralizedbandwidth()!=null){
				if (resp.getResponse(0).getPath(0).getGeneralizedbandwidth().getOT()==ObjectParameters.PCEP_OBJECT_TYPE_GB_SSON){
					int m=0;
					m=(((GeneralizedBandwidthSSON)resp.getResponse(0).getPath(0).getGeneralizedbandwidth()).getM());
					long reservationID=reservationManager.reserve(sourceVertexList, targetVertexList, lambda_chosen, reservation.getTimer(), this.bidirectional, m);
					ReservationConf resConf= new ReservationConf();
					resConf.setReservationID(reservationID);
					resp.getResponse(0).setResConf(resConf);
					
				}
				return resp;
			}
			else{
				log.info("Reserving lambda "+lambda_chosen);
				long reservationID=reservationManager.reserve(sourceVertexList, targetVertexList, lambda_chosen, reservation.getTimer(), this.bidirectional);
				ReservationConf resConf= new ReservationConf();
				resConf.setReservationID(reservationID);
				resp.getResponse(0).setResConf(resConf);
				return resp;
			}
		}else {
			return null;	
		}
			
	}

	public ComputingResponse getResp() {
		return resp;
	}

	public void setResp(ComputingResponse resp) {
		
		this.resp = resp;
	}

	public LinkedList<Object> getSourceVertexList() {
		return sourceVertexList;
	}

	public void setSourceVertexList(LinkedList<Object> sourceVertexList) {
		this.sourceVertexList = sourceVertexList;
	}

	public LinkedList<Object> getTargetVertexList() {
		return targetVertexList;
	}

	public void setTargetVertexList(LinkedList<Object> targetVertexList) {
		this.targetVertexList = targetVertexList;
	}

	public Reservation getReservation() {
		return reservation;
	}

	public void setReservation(Reservation reservation) {
		this.reservation = reservation;
	}
	public int getLambda_chosen() {
		return lambda_chosen;
	}
	public void setLambda_chosen(int lambda_chosen) {
		this.lambda_chosen = lambda_chosen;
	}

	public ReservationManager getReservationManager() {
		return reservationManager;
	}

	public void setReservationManager(ReservationManager reservationManager) {
		this.reservationManager = reservationManager;
	}

}
