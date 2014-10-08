package tid.pce.client.tester.restoration;

import java.net.Inet4Address;
import java.util.ArrayList;

import es.tid.pce.pcep.constructs.Response;

public class RestorationCaseTable {

	Inet4Address source;
	Inet4Address destination;
	int idPath;
	Response response;
	ArrayList<Inet4Address> path;
	Inet4Address initialNodePath;
	Inet4Address finalNodePath;

	
	public Inet4Address getInitialNodePath() {
		return initialNodePath;
	}
	public void setInitialNodePath(Inet4Address initialNodePath) {
		this.initialNodePath = initialNodePath;
	}
	public Inet4Address getFinalNodePath() {
		return finalNodePath;
	}
	public void setFinalNodePath(Inet4Address finalNodePath) {
		this.finalNodePath = finalNodePath;
	}
	public int getIdPath() {
		return idPath;
	}
	public void setIdPath(int idPath) {
		this.idPath = idPath;
	}
	public Inet4Address getSource() {
		return source;
	}
	public void setSource(Inet4Address source) {
		this.source = source;
	}
	public Inet4Address getDestination() {
		return destination;
	}
	public void setDestination(Inet4Address destination) {
		this.destination = destination;
	}
	
	public boolean isTheSameLink(Inet4Address source, Inet4Address destination){
		return (this.source.equals(source)) && (this.destination.equals(destination));
	}
	
	public Response getResponse() {
		return response;
	}
	public void setResponse(Response response) {
		this.response = response;
	}
	
	public ArrayList<Inet4Address> getPath() {
		return path;
	}
	public void setPath(ArrayList<Inet4Address> path) {
		this.path = path;
	}
	@Override
	public boolean equals(Object obj) {
		// TODO Auto-generated method stub
		return ((((RestorationCaseTable)obj).getSource().equals(source)) && (((RestorationCaseTable)obj).getDestination().equals(destination)));
	
	}
	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return "Path number: "+ idPath +" Link: "+source.toString()+" - "+destination.toString();
	}
	
	
	
}
