/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tid.pce.client.lsp.te;

import java.net.Inet4Address;

import es.tid.pce.pcep.constructs.Response;
import es.tid.pce.pcep.objects.SRERO;
import es.tid.rsvp.objects.ERO;
import tid.emulator.node.transport.RequestedLSPinformation;
import tid.pce.client.lsp.LSP;

/**
 * This class represents all the needed LSP information to completely characterize it.
 * 
 * Parameters:
 * 
 * 	1.-	Tunnel ID:			The unique LSP tunnel identifier
 * 	2.-	Source ID:			The source node address
 * 	3.-	Destination ID:		The destination node address
 * 	4.-	Characteristics:	The LSP main characteristics used to compose a correct RSVP message.
 * @author fmn
 */
public class LSPTE extends LSP{

	//Initial parameters for requested LSP
	private RequestedLSPinformation requestedLSPinformation;
	//Meter campo con el estado del LSP
    //private TEPathState pathState;
    private int pathState;
	
	private long tunnelId;
	private long automatic_tunnelId;
    private Inet4Address idSource;
    private Inet4Address idDestination;

    private float bw;
    private boolean bidirectional;
    private int OFcode;
    private int lambda;
    private int m;
    private boolean bandwidth;
    private Response pcepResponse;
    private int technologyType;
    private ERO ero;
    private SRERO srero;
    private boolean interDomain = false;
    
    private boolean delegated;
    private String delegatedAdress;

    /**
     * @param idSource
     * @param idDestination
     * @param characteristics
     */

    public LSPTE(long tunnelId,Inet4Address idSource, Inet4Address idDestination, boolean bidirect, 
    		int OFcode, float bw, int pathState){
    	super(new Long(tunnelId), idSource, idDestination, pathState);
    	//this.pathState = new TEPathState();
    	this.pathState=pathState;
    	this.tunnelId = tunnelId;
    	this.idSource = idSource;
        this.idDestination = idDestination;
        this.bidirectional=bidirect;
        this.OFcode=OFcode;
        this.bw=bw;
        //this.technologyType=technologyType;
    }

	public Inet4Address getIdSource(){
        return idSource;
    }

	public Inet4Address getIdDestination(){
        return idDestination;
    }

	public long getTunnelId() {
		return tunnelId;
	}

	public void setTunnelId(long tunnelId) {
		this.tunnelId = tunnelId;
	}
	
	public void setIdSource(Inet4Address idSource) {
		this.idSource = idSource;
	}

	public int getPathState() {
		return pathState;
	}

	public void setPathState(int pathState) {
		this.pathState = pathState;
	}

	public void setIdDestination(Inet4Address idDestination) {
		this.idDestination = idDestination;
	}
	
	 public boolean isBidirectional() {
			return bidirectional;
	}

	public void setBidirectional(boolean bidirectional) {
		this.bidirectional = bidirectional;
	}
	
	public int getOFcode() {
		return OFcode;
	}

	public void setOFcode(int oFcode) {
		OFcode = oFcode;
	}

	public Response getPcepResponse() {
		return pcepResponse;
	}

	public void setPcepResponse(Response pcepResponse) {
		this.pcepResponse = pcepResponse;
	}

	public float getBw() {
		return bw;
	}

	public void setBw(float bw) {
		this.bw = bw;
	}

	public int getLambda() {
		return lambda;
	}

	public void setLambda(int lambda) {
		this.lambda = lambda;
	}
	
	public int getM() {
		return m;
	}

	public void setM(int m) {
		this.m = m;
	}

	public boolean isBandwidth() {
		return bandwidth;
	}

	public void setBandwidth(boolean bandwidth) {
		this.bandwidth = bandwidth;
	}

	public int getTechnologyType() {
		return technologyType;
	}

	public void setTechnologyType(int technologyType) {
		this.technologyType = technologyType;
	}

	public ERO getEro() {
		return ero;
	}

	public void setEro(ERO ero) {
		this.ero = ero;
	}
	
	public void setSRERO(SRERO srero){
		this.srero = srero;
	}

	public SRERO getSRERO() {
		return srero;
	}
	
	public long getAutomatic_tunnelId() {
		return automatic_tunnelId;
	}

	public void setAutomatic_tunnelId(long automaticTunnelId) {
		automatic_tunnelId = automaticTunnelId;
	}

	public boolean isInterDomain() {
		return interDomain;
	}

	public void setInterDomain(boolean interDomain) {
		this.interDomain = interDomain;
	}

	public boolean isDelegated() {
		return delegated;
	}
	public void setDelegated(boolean delegated) {
		this.delegated = delegated;
	}

	public String getDelegatedAdress() {
		return delegatedAdress;
	}

	public void setDelegatedAdress(String delegatedAdress) {
		this.delegatedAdress = delegatedAdress;
	}
}