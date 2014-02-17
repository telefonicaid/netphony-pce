package tid.pce.client.tester;

import java.net.Inet4Address;


public class RequestToSend {
	Inet4Address source;
	Inet4Address destiny;
	RequestParametersConfiguration requestParameters;
	
	
	public Inet4Address getSource() {
		return source;
	}
	public void setSource(Inet4Address source) {
		this.source = source;
	}
	public Inet4Address getDestiny() {
		return destiny;
	}
	public void setDestiny(Inet4Address destiny) {
		this.destiny = destiny;
	}
	public RequestParametersConfiguration getRequestParameters() {
		return requestParameters;
	}
	public void setRequestParameters(RequestParametersConfiguration requestParameters) {
		this.requestParameters = requestParameters;
	}
	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return "Source: "+source.toString()+"Destiny: "+destiny.toString();
	}
	
	
	
}
