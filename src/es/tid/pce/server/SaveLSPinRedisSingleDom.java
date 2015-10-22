package es.tid.pce.server;

import java.net.Inet4Address;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Logger;

import com.google.gson.Gson;

import redis.clients.jedis.Jedis;



import es.tid.pce.parentPCE.MD_LSP;
import es.tid.pce.parentPCE.MDLSPDB.SimpleLSP;
import es.tid.pce.parentPCE.MDLSPDB.SimpleLSPhop;
import es.tid.pce.pcep.objects.ExplicitRouteObject;
import es.tid.pce.pcep.objects.LSP;
import es.tid.pce.server.RedisDatabaseHandler;
import es.tid.pce.server.lspdb.SingleDomainLSPDB;
import es.tid.rsvp.objects.subobjects.EROSubobject;
import es.tid.rsvp.objects.subobjects.ETCEROSubobject;
import es.tid.rsvp.objects.subobjects.GeneralizedLabelEROSubobject;
import es.tid.rsvp.objects.subobjects.LabelEROSubobject;
import es.tid.rsvp.objects.subobjects.UnnumberIfIDEROSubobject;
import es.tid.tedb.IntraDomainEdge;
import es.tid.tedb.MultiDomainTEDB;
import es.tid.tedb.SimpleTEDB;

public class SaveLSPinRedisSingleDom implements Runnable {
	
	
	private Hashtable <Integer,SD_LSP> LSP_list;
	
	private Hashtable <Integer,SD_LSP> LSP_list_old;
	
	private Hashtable <Integer,SD_LSP> LSP_list_to_add;
	
	private Hashtable <Integer,SD_LSP> LSP_list_to_del;
	
	private Logger log;
	
	Jedis jedis;
	
	
	
	public SaveLSPinRedisSingleDom () {
		
		log = Logger.getLogger("BGP4Parser");
	
	}

	
	public void configure( SingleDomainLSPDB singleDomainLSPDB, String host, int port){
	
		jedis = new Jedis(host,port);
		jedis.connect();
		this.LSP_list=singleDomainLSPDB.getSingleDomain_LSP_list();
			
	}
	
	public void run(){	
		log.info("Going to save LSP in DB");
		//LSPs to add
		LSP_list_to_add=new Hashtable <Integer,SD_LSP>(); 
		LSP_list_to_del=new Hashtable <Integer,SD_LSP>(); 
		if (LSP_list!=null){
			
			if (LSP_list_old==null){
				LSP_list_old= new Hashtable <Integer,SD_LSP>();
				
				Enumeration<Integer>ids=LSP_list.keys();
				while (ids.hasMoreElements()){	
					Integer id=ids.nextElement();
					LSP_list_old.put(id, LSP_list.get(id));
					LSP_list_to_add.put(id,  LSP_list.get(id));		
				}
			}else {
				Enumeration<Integer>ids=LSP_list.keys();
				while (ids.hasMoreElements()){	
					Integer id=ids.nextElement();
					if (!(LSP_list_old.containsKey(id))){
						LSP_list_to_add.put(id,  LSP_list.get(id));	
						LSP_list_old.put(id, LSP_list.get(id));
					}			
				}
				ids=LSP_list_old.keys();
				while (ids.hasMoreElements()){	
					Integer id=ids.nextElement();
					if (!(LSP_list.containsKey(id))){
						LSP_list_to_del.put(id, LSP_list.get(id));	
						//multiDomain_LSP_list_old.remove(id);
					}			
				}
				ids=LSP_list_to_del.keys();
				while (ids.hasMoreElements()){	
					Integer id=ids.nextElement();
					LSP_list_old.remove(id);			
				}			
			}
			
			Enumeration<Integer>ids=LSP_list_to_add.keys();
			while (ids.hasMoreElements()){	
				Integer id=ids.nextElement();
				String key;
				String value;
				key="LSP:"+id;
				value=lspToJSON(LSP_list_to_add.get(id));
				jedis.set(key,value);
				jedis.sadd("lsps",key);
			}
			
			//LSPs to delete
			
			ids=LSP_list_to_del.keys();
			while (ids.hasMoreElements()){	
				Integer id=ids.nextElement();
				String key;
				key="LSP:"+id;
				jedis.del(key);
				jedis.srem("lsps",key);
			}

		}
		

	}
	
	
	public String lspToJSON(SD_LSP lsp){
		
		Gson gson = new Gson();
		
		SimpleLSP slsp=new SimpleLSP();
		
		// ¿Tengo que añadir el fullERO en la clase LSP?
		ExplicitRouteObject ero=lsp.getFullERO();
		Iterator <EROSubobject> erosolist= ero.getEROSubobjectList().iterator();		
		int num=0;
		while (erosolist.hasNext()){
			EROSubobject eroso= erosolist.next();
			if (eroso instanceof UnnumberIfIDEROSubobject){
				num+=1;
			}
		}
		slsp.data=new SimpleLSPhop[num];
		
		erosolist= ero.getEROSubobjectList().iterator();		
		int i=-1;
		while (erosolist.hasNext()){
			EROSubobject eroso= erosolist.next();
			if (eroso instanceof UnnumberIfIDEROSubobject){
				i+=1;
				slsp.data[i]=new SimpleLSPhop();
				slsp.data[i].routerID= ((UnnumberIfIDEROSubobject)eroso).routerID.getHostAddress();
				slsp.data[i].ifID= ""+((UnnumberIfIDEROSubobject)eroso).interfaceID;				
			}else if (eroso instanceof GeneralizedLabelEROSubobject){
				if (slsp.data[i]!=null){
					slsp.data[i].n=""+ ((GeneralizedLabelEROSubobject)eroso).getDwdmWavelengthLabel().getN();
					slsp.data[i].m=""+ ((GeneralizedLabelEROSubobject)eroso).getDwdmWavelengthLabel().getM();		
				}
						
			}else if (eroso instanceof ETCEROSubobject){
				if (slsp.data[i]!=null){
					slsp.data[i].transponder="TX "+((ETCEROSubobject)eroso).getSubTransponderList().get(0).getST_TLV_ModFormat().toString();
				}
				
			}
		}
		
   	 	String json = gson.toJson(slsp);
   	 	
  	 	
   	 	return json;
		
	}
	
	
	
	
	public Hashtable<Integer, SD_LSP> getMultiDomain_LSP_list() {
		return LSP_list;
	}
	
	
	public void setMultiDomain_LSP_list(Hashtable<Integer, SD_LSP> LSP_list) {
		this.LSP_list = LSP_list;
	}
	
	

}
