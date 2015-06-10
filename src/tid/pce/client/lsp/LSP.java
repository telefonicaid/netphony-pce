/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tid.pce.client.lsp;

import java.net.Inet4Address;

/**
 * This class represents all the needed LSP information to completely characterize it.
 * 
 * Parameters:
 * 
 * 	1.-	LSP ID:				The unique LSP identifier
 * 	2.-	Source ID:			The source node address
 * 	3.-	Destination ID:		The destination node address
 * 	4.-	Characteristics:	The LSP main characteristics used to compose a correct RSVP message.
 * @author fmn
 */
public class LSP {

	private long idLSP;
    private Inet4Address idSource;
    private Inet4Address idDestination;
    //private PathState pathState;
    private int pathState;

    /**
     *
     * @param idSource
     * @param idDestination
     * @param characteristics
     */

    public LSP(Long idLSP,Inet4Address idSource, Inet4Address idDestination, int pathState){

    	this.idLSP = idLSP;
        this.idSource = idSource;
        this.idDestination = idDestination;
        this.pathState=pathState;
    	this.idSource = idSource;
    	
        //this.pathState = new PathState();

    }

    /**
     *
     * @return
     */

    public Inet4Address getIdSource(){
    	return idSource;
    }

    /**
     *
     * @return
     */

    public Inet4Address getIdDestination(){
    	return idDestination;
    }

	public Long getIdLSP() {
		return idLSP;
	}

	public void setIdLSP(Long idLSP) {
		this.idLSP = idLSP;
	}

	public int getPathState() {
		return pathState;
	}

	public void setPathState(int pathState) {
		this.pathState = pathState;
	}

	public void setIdSource(Inet4Address idSource) {
		this.idSource = idSource;
	}

	public void setIdDestination(Inet4Address idDestination) {
		this.idDestination = idDestination;
	}
}
