package tid.pce.tedb;


import org.jgrapht.graph.DefaultEdge;

public class InterDomainEdge extends DefaultEdge {
	
	/**
	 * Interface ID of the outgoing interface from the source
	 */
	public long src_if_id;
	
	/**
	 * Interface ID of the incoming interface from the destination
	 */
	public long dst_if_id;
	
	
	public Object src_router_id;
	
	public Object dst_router_id;
	/**
	 * Destination router domain
	 */
	public Object domain_dst_router;
	
	public TE_Information TE_info;
	InterDomainEdge(){
		
	}
	public InterDomainEdge(Object src, Object dst){
		src_router_id= src;
		dst_router_id=dst;
	}
	public Object getDomain_dst_router() {
		return domain_dst_router;
	}

	public void setDomain_dst_router(Object domain_dst_router) {
		this.domain_dst_router = domain_dst_router;
	}

	public Object getSource(){
		Object source= (Object)super.getSource();
		return source;
	}
	
	public TE_Information getTE_info() {
		return TE_info;
	}
	public void setTE_info(TE_Information tE_info) {
		TE_info = tE_info;
	}
	public Object getTarget(){
		Object destination= (Object)super.getTarget();
		return destination;
	}

	public long getSrc_if_id() {
		return src_if_id;
	}

	public void setSrc_if_id(long src_if_id) {
		this.src_if_id = src_if_id;
	}

	public long getDst_if_id() {
		return dst_if_id;
	}

	public void setDst_if_id(long dst_if_id) {
		this.dst_if_id = dst_if_id;
	}

	public Object getSrc_router_id() {
		return src_router_id;
	}

	public void setSrc_router_id(Object src_router_id) {
		this.src_router_id = src_router_id;
	}

	public Object getDst_router_id() {
		return dst_router_id;
	}

	public void setDst_router_id(Object dst_router_id) {
		this.dst_router_id = dst_router_id;
	}
	
	
	
	@Override
	public boolean equals(Object obj) {
		if ((((InterDomainEdge)obj).getDst_router_id()).equals(dst_router_id)
				&& (((InterDomainEdge)obj).getSrc_router_id()).equals(src_router_id)){
			return true;
		}
		return false;
	}
	@Override
	public String toString(){
		String ideString;
		//TODO: he cambiado esta linea,...porq no me funcionaba super.getSource...Hayq ue mirarlo!!
		//ideString=src_router_id.toString()+":"+src_if_id+" ("+((Object)super.getSource()).toString()+")  --> "+dst_router_id.toString()+":"+dst_if_id+" ("+((Object)super.getTarget()).toString()+")";
		ideString=src_router_id.toString()+":"+src_if_id+" --> "+dst_router_id.toString()+":"+dst_if_id;
		
		return ideString;
	}

}
