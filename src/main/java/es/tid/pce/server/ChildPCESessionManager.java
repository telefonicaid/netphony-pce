package es.tid.pce.server;

import java.net.Inet4Address;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

import es.tid.pce.computingEngine.RequestDispatcher;
import es.tid.pce.pcep.messages.PCEPMessage;
import es.tid.pce.pcepsession.PCEPSessionsInformation;
import es.tid.tedb.TEDB;

/**
 * Child PCE Session Manager. It is in charge of managing the communication between the domain PCE and the Parent PCE
 * @author ogondio
 *
 */
public class ChildPCESessionManager extends TimerTask{
	
//	private Timer timer;	
	
	private PCEServerParameters params;
	
	private ChildPCESession childPCEParentPCESession;
	
	private RequestDispatcher requestDispatcher;

	private RequestQueue parentPCERequestQueue;
	
	private TEDB ted;
	
	private LinkedBlockingQueue<PCEPMessage> sendingQueue;
	
	private ParentPCERequestManager childPCERequestManager;
	
	private Inet4Address domainId;
	
	private Logger log;
	
	private RequestDispatcher PCCRequestDispatcherChild;
	
	PCEPSessionsInformation pcepSessionInformation;
	
	private SingleDomainInitiateDispatcher iniDispatcher;
	
	public ChildPCESessionManager(RequestDispatcher PCCRequestDispatcherChild, PCEServerParameters params, TEDB ted, Inet4Address domainId, PCEPSessionsInformation pcepSessionInformation, SingleDomainInitiateDispatcher iniDispatcher){
		this.params=params;  
		this.ted=ted;
		this.PCCRequestDispatcherChild=PCCRequestDispatcherChild;
		this.pcepSessionInformation=pcepSessionInformation;
//		this.timer=new Timer();
		this.parentPCERequestQueue=new RequestQueue(params.getParentPCERequestProcessors());
		sendingQueue=new LinkedBlockingQueue<PCEPMessage>();
		childPCERequestManager=new ParentPCERequestManager(sendingQueue);
		this.domainId=domainId;
		this.iniDispatcher=iniDispatcher;
		log=Logger.getLogger("PCEServer");
	}
	
	
	public ChildPCESessionManager(PCEServerParameters params, TEDB ted, Inet4Address domainId, PCEPSessionsInformation pcepSessionInformation, SingleDomainInitiateDispatcher iniDispatcher){
		this.params=params;  
		this.ted=ted;
		this.pcepSessionInformation=pcepSessionInformation;
		this.parentPCERequestQueue=new RequestQueue(params.getParentPCERequestProcessors());
		sendingQueue=new LinkedBlockingQueue<PCEPMessage>();
		childPCERequestManager=new ParentPCERequestManager(sendingQueue);
		this.domainId=domainId;
		this.iniDispatcher=iniDispatcher;
		log=Logger.getLogger("PCEServer");
	}
	public ChildPCESession getChildPCEParentPCESession() {
		return childPCEParentPCESession;
	}

	public void setChildPCEParentPCESession(ChildPCESession childPCEParentPCESession) {
		this.childPCEParentPCESession = childPCEParentPCESession;
	}

	/**
	 * Starts new session with the Parent PCE
	 * 
	 * @return true if the session was launched, false if the session was already established
	 */
	public void run(){
		
		//pcepSessionInformation.setStateful(true);
		
		if(childPCEParentPCESession != null){
			log.warning("There is a session with Parent PCE!");
			if (childPCEParentPCESession.isAlive()){
				if (childPCEParentPCESession.isInterrupted()){
					log.severe("THREAD VIVO... SESION MUERTA");
				}
				return;	
			}
			else{
				log.warning("Session with parent PCE dead, trying to establish new session");
				Timer timer=new Timer();
				childPCEParentPCESession= new ChildPCESession(PCCRequestDispatcherChild, params, parentPCERequestQueue,ted,timer,sendingQueue,childPCERequestManager,domainId,pcepSessionInformation, iniDispatcher);
				childPCEParentPCESession.start();
				return;
			}
		}else{
			log.warning("No Session with parent PCE, trying to establish new session");
			sendingQueue.clear();//Borramos lo que haya????
			Timer timer=new Timer();
			childPCEParentPCESession= new ChildPCESession(PCCRequestDispatcherChild, params, parentPCERequestQueue,ted,timer,sendingQueue,childPCERequestManager,domainId,pcepSessionInformation,iniDispatcher);
			childPCEParentPCESession.start();

			return;
		}

	   
	}
	
	public boolean PPCESessionStatus(){
		return childPCEParentPCESession.isAlive();
	}
	
	public int PPCESessionState(){
		return childPCEParentPCESession.getFSMstate();
	}
	
	public void killPPCESession(){
		childPCEParentPCESession.killSession();
	}
	
	public void closePPceSession(){
		if (childPCEParentPCESession.isAlive()){
			//FIXME reason for close????
			childPCEParentPCESession.close(1);
		}
		
	}
	
	public RequestQueue getParentPCERequestQueue() {
		return parentPCERequestQueue;
	}

	public LinkedBlockingQueue<PCEPMessage> getSendingQueue() {
		return sendingQueue;
	}

	public ParentPCERequestManager getChildPCERequestManager() {
		return childPCERequestManager;
	}
	
	
	public Logger getLogger(){
		return log;
	}
	
}
