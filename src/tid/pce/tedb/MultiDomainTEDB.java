package tid.pce.tedb;


public interface MultiDomainTEDB extends TEDB {

	public void addInterdomainLink( Object localDomainID, Object localRouterASBR, long localRouterASBRIf, Object remoteDomainID, Object remoteRouterASBR, long remoteRouterASBRIf, TE_Information te_info );
	
}
