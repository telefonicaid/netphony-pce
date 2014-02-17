package tid.pce.server.wlan;

import org.eclipse.jetty.util.log.Log;

import tid.pce.server.PCEServer;

/**
 * 
 * Id for switch, should be the equivalent of IP for non-wlan networks. In String id
 * MAC will be stored eventually.
 *    
 * @author jaume
 *
 */

public class SwitchID 
{
	String id;
	public SwitchID(String id)
	{
		this.id = id;
	}
	
	@Override
	public int hashCode() 
	{
		final int prime = 31;
		int result = 1;
		if (id == null)
		{
			return 0;
		}
		String idAux = id.substring(0, 7);
		//PCEServer.Log.info("idAux::"+idAux);
		result = prime * result + ((idAux == null) ? 0 : idAux.hashCode());
		return result;
	}
	
	@Override
	public boolean equals(Object obj) 
	{
		//PCEServer.Log.info("Not entering??");
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SwitchID other = (SwitchID) obj;
		//PCEServer.Log.info("id::"+id+",other::"+other);
		//PCEServer.Log.info("(id.contains(other.getId())) || (other.getId().contains(id))::"+((id.contains(other.getId())) || (other.getId().contains(id))));
		if ( (id.contains(other.getId())) || (other.getId().contains(id)))
		{
			return true;
		}
		
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "SwitchID [id=" + id + "]";
	}
	public String getId() 
	{
		return id;
	}
	public void setId(String id) 
	{
		this.id = id;
	}
}
