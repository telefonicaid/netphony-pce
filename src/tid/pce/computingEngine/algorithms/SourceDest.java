package tid.pce.computingEngine.algorithms;

import java.net.Inet4Address;

public class SourceDest {
	
	public SourceDest(int source, int destination){
		this.source=source;
		this.destination=destination;
	}
	public int source;
	public int destination;
	
	public boolean equals(SourceDest sd){		
		if (this.source==sd.source){
			System.out.println("source igual");
			if (this.destination==sd.destination){
				return true;
			}
		}
		return false;
	}
	
	public String toString(){
		return source+" :: "+destination;
	}
	
}
