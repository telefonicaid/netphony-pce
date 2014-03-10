
package tid.pce.client.multiLayer;

import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.TimerTask;
import java.util.logging.Logger;



import tid.pce.client.ClientRequestManager;
import tid.pce.client.PCCPCEPSession;
import tid.pce.computingEngine.ComputingResponse;
import tid.pce.pcep.PCEPProtocolViolationException;
import tid.pce.pcep.constructs.Path;
import tid.pce.pcep.messages.PCEPMessage;
import tid.pce.pcep.messages.PCEPMonReq;
import tid.pce.pcep.messages.PCEPRequest;
import tid.pce.pcep.messages.PCEPTELinkSuggestion;
import tid.pce.pcep.objects.ExplicitRouteObject;
import tid.rsvp.objects.subobjects.EROSubobject;
import tid.rsvp.objects.subobjects.SubObjectValues;
import tid.rsvp.objects.subobjects.IPv4prefixEROSubobject;


public class AutomaticTesterMLNetworkTask  extends TimerTask {

	private ClientRequestManager crm;
	private Logger log;
	static long requestID=123;
	private PCCPCEPSession psVNTM;
	
	/*Variable used for counter how many requests there are*/

	PCEPRequest request;
	
	
	public AutomaticTesterMLNetworkTask(PCEPRequest request,PCCPCEPSession ps,PCCPCEPSession psVNTM ){
		this.request = request;
		log=Logger.getLogger("PCCClient");
		this.crm=ps.crm;
		this.psVNTM=psVNTM;
	}
	

	
	@Override
	public void run() {
		log.info("Starting Automatic Client Interface");
		ComputingResponse pr;
		request.getRequestList().getFirst().getRequestParameters().setRequestID(requestID);
		requestID=requestID+1;
		pr=crm.newRequest(this.request);
	
		System.out.println("Respuesta "+pr.toString());
		
		String strPrev=null;
		if (pr.getResponseList().isEmpty()){
			System.out.println("ERROR");
			return;
		}else {
			if (pr.getResponseList().get(0).getNoPath()!=null){
				System.out.println("NO PATH");
				return;	
			}else {
				Path path=pr.getResponseList().get(0).getPath(0);
				ExplicitRouteObject ero=path.geteRO();
				LinkedList<EROSubobject> eroSubObjList=ero.getEROSubobjectList();
				boolean layerInfoFound=false;
				int numNewLinks=0;
				ArrayList<String> sourceList = new ArrayList<String>();
				ArrayList<String> destinationList = new ArrayList<String>();
				//cREAR LISTA DE EROS
				LinkedList<ExplicitRouteObject> eroList=new LinkedList<ExplicitRouteObject>();
				LinkedList<EROSubobject> eroSubObjList2=null;
				int counterArray=0;
				for (int i=0;i<eroSubObjList.size();++i){
					if ((eroSubObjList.get(i)).getType()==SubObjectValues.ERO_SUBOBJECT_LAYER_INFO){
						if (layerInfoFound==false){
							layerInfoFound=true;							
							//Create a new ERO to add at the list
							ExplicitRouteObject newERO =  new ExplicitRouteObject();							
							eroSubObjList2=newERO.getEROSubobjectList();	
							eroSubObjList2.add(eroSubObjList.get(i-1));							
							eroList.add(newERO);
							numNewLinks=numNewLinks+1;
														
						}else {
							System.out.println("Acabo pongo layerInfoEnded a true");
							layerInfoFound=false;
							eroSubObjList2.add(eroSubObjList.get(i+1));
						}
					}
					else if (layerInfoFound==true){
						eroSubObjList2.add(eroSubObjList.get(i));
					}else {		
						String str1=((IPv4prefixEROSubobject)(eroSubObjList.get(i))).getIpv4address().toString().substring(1);
						if (strPrev==null){
							strPrev=str1;
						}else {
							sourceList.add(counterArray, strPrev);							
							destinationList.add(counterArray, str1);
							strPrev=str1;
							counterArray++;
						}															
					}
				}
				for (int i=0;i<numNewLinks;++i){
					PCEPTELinkSuggestion telinksug=new PCEPTELinkSuggestion();
					Path path2=new Path();
					path2.seteRO(eroList.get(i));
					telinksug.setPath(path2);	
					try {
						telinksug.encode();
					} catch (PCEPProtocolViolationException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					try {
						System.out.println("Sending TE LINK SUGGESTION message i:"+i);
						DataOutputStream out=psVNTM.getOut();
						out.write(telinksug.getBytes());
						out.flush();					
						String str1=((IPv4prefixEROSubobject)(eroList.get(i).getEROSubobjectList().getFirst())).getIpv4address().toString().substring(1);
						String str2=((IPv4prefixEROSubobject)(eroList.get(i).getEROSubobjectList().getLast())).getIpv4address().toString().substring(1);
						String str = createLink(str1,str2,String.valueOf(10));
						System.out.println(str);
						sendUpdate("172.16.3.1", 4190, str);
						
					} catch (IOException e) {
						System.out.println("Error sending REQ: " + e.getMessage());
					}
					
					
				}
			//Recorrer el ERO MPLS y enviar updates a Marek
				String s1;
				for (int i =0; i<sourceList.size();i++){
					System.out.println("Reservo capacity y mando el update");
					s1 = reserveCapacity(sourceList.get(i), destinationList.get(i),String.valueOf(1));
					System.out.println(s1);

					sendUpdate("172.16.3.1", 4190, s1);
				}
			
			}
			
		}
		
	

	}//End run

	  public static String reserveCapacity(String sourceVector, String destVector, String bw) {
			String s = "RESERVE:";
			s += sourceVector + ":" + destVector + ":" + bw;

			return s;
		    }

public static void sendUpdate(String address, int port, String update) {
	try {
	    Socket socket = new Socket(address, port);
	    BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
	    bufferedWriter.write(update);
	    bufferedWriter.flush();
	    socket.close();
	} catch (UnknownHostException e) {
	    e.printStackTrace();
	} catch (IOException e) {
	    e.printStackTrace();
	}
   }

public static String createLink(String sourceVector, String destVector, String bw) {
	String s = "CREATE:";
	s += sourceVector + ":" + destVector + ":" + bw;

	return s;
    }
}