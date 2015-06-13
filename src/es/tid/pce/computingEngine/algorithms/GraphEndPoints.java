package es.tid.pce.computingEngine.algorithms;

/**
 * 
 * @author b.mvas
 */
import es.tid.pce.pcep.constructs.EndPoint;
import es.tid.pce.pcep.constructs.EndPointAndRestrictions;
import es.tid.pce.pcep.constructs.P2MPEndpoints;
import es.tid.pce.pcep.constructs.P2PEndpoints;
import es.tid.pce.pcep.objects.EndPoints;
import es.tid.pce.pcep.objects.EndPointsIPv4;
import es.tid.pce.pcep.objects.GeneralizedEndPoints;
import es.tid.pce.pcep.objects.ObjectParameters;

public class GraphEndPoints {
	private GraphEndPoint source;
	private GraphEndPoint destination;
	
	public GraphEndPoints (EndPoints EP){
		
		source = new GraphEndPoint() ;
		destination = new GraphEndPoint();

//		Object source_router_id_addr = null;
//		Object dest_router_id_addr = null;
		
		if (EP.getOT()==ObjectParameters.PCEP_OBJECT_TYPE_ENDPOINTS_IPV4){
			EndPointsIPv4 ep =(EndPointsIPv4) EP;
			source.setVertex(ep.getSourceIP());
			destination.setVertex(ep.getDestIP());
		}else if (EP.getOT()==ObjectParameters.PCEP_OBJECT_TYPE_ENDPOINTS_IPV6){
			//FIXME complete this part
		}
		
		if (EP.getOT()==ObjectParameters.PCEP_OBJECT_TYPE_GENERALIZED_ENDPOINTS){
			GeneralizedEndPoints gep =(GeneralizedEndPoints) EP;
			
			if(gep.getGeneralizedEndPointsType()==ObjectParameters.PCEP_GENERALIZED_END_POINTS_TYPE_P2P){
				P2PEndpoints p2pep = gep.getP2PEndpoints();
				EndPoint sourceep = p2pep.getSourceEndPoint();
				EndPoint destep = p2pep.getDestinationEndPoint();
				source.setVertex(sourceep.getEndPointDataPathTLV().switchID);
				destination.setVertex(destep.getEndPointDataPathTLV().switchID);
			}
			else if(gep.getGeneralizedEndPointsType()==ObjectParameters.PCEP_GENERALIZED_END_POINTS_TYPE_P2MP_NEW_LEAVES){
				P2MPEndpoints p2mpep = gep.getP2MPEndpoints();
				EndPointAndRestrictions epandrest = p2mpep.getEndPointAndRestrictions();
				EndPoint sourceep = epandrest.getEndPoint();
				source.setVertex(sourceep.getEndPointIPv4TLV().IPv4address);
				int cont=0;
				while (cont<=p2mpep.getEndPointAndRestrictionsList().size()){
					epandrest = p2mpep.getEndPointAndRestrictionsList().get(cont);
					EndPoint destep = epandrest.getEndPoint();
					source.setVertex(sourceep.getEndPointIPv4TLV().IPv4address);
					destination.setVertex(destep.getEndPointIPv4TLV().IPv4address);

				}
			}
		}
	}

	/*
	 * GETTERS AND SETTERS
	 */
	public GraphEndPoint getSource() {
		return source;
	}

	public void setSource(GraphEndPoint source) {
		this.source = source;
	}

	public GraphEndPoint getDestination() {
		return destination;
	}

	public void setDestination(GraphEndPoint destination) {
		this.destination = destination;
	}
}
