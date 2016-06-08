package es.tid.pce.parentPCE.MDLSPDB;

import java.net.Inet4Address;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import redis.clients.jedis.Jedis;
import es.tid.pce.parentPCE.MD_LSP;
import es.tid.pce.pcep.objects.ExplicitRouteObject;
import es.tid.pce.server.RedisDatabaseHandler;
import es.tid.rsvp.objects.subobjects.EROSubobject;
import es.tid.rsvp.objects.subobjects.ETCEROSubobject;
import es.tid.rsvp.objects.subobjects.GeneralizedLabelEROSubobject;
import es.tid.rsvp.objects.subobjects.LabelEROSubobject;
import es.tid.rsvp.objects.subobjects.UnnumberIfIDEROSubobject;
import es.tid.tedb.IntraDomainEdge;
import es.tid.tedb.MultiDomainTEDB;
import es.tid.tedb.SimpleTEDB;

public class SaveLSPinRedis implements Runnable {
	
	
	private Hashtable <Integer,MD_LSP> multiDomain_LSP_list;
	
	private Hashtable <Integer,MD_LSP> multiDomain_LSP_list_old;
	
	private Hashtable <Integer,MD_LSP> multiDomain_LSP_list_to_add;
	
	private Hashtable <Integer,MD_LSP> multiDomain_LSP_list_to_del;
	
	private Logger log;
	
	Jedis jedis;
	
	
	
	public SaveLSPinRedis () {
		
		log = LoggerFactory.getLogger("BGP4Parser");
	
	}

	
	public void configure( MultiDomainLSPDB multiDomainLSPDB, String host, int port){
	
		jedis = new Jedis(host,port);
		jedis.connect();
		this.multiDomain_LSP_list=multiDomainLSPDB.getMultiDomain_LSP_list();
			
	}
	
	public void run(){	
		log.info("Going to save LSP in DB");
		//LSPs to add
		multiDomain_LSP_list_to_add=new Hashtable <Integer,MD_LSP>(); 
		multiDomain_LSP_list_to_del=new Hashtable <Integer,MD_LSP>(); 
		if (multiDomain_LSP_list!=null){
			
			if (multiDomain_LSP_list_old==null){
				multiDomain_LSP_list_old= new Hashtable <Integer,MD_LSP>();
				
				Enumeration<Integer>ids=multiDomain_LSP_list.keys();
				while (ids.hasMoreElements()){	
					Integer id=ids.nextElement();
					multiDomain_LSP_list_old.put(id, multiDomain_LSP_list.get(id));
					multiDomain_LSP_list_to_add.put(id,  multiDomain_LSP_list.get(id));		
				}
			}else {
				Enumeration<Integer>ids=multiDomain_LSP_list.keys();
				while (ids.hasMoreElements()){	
					Integer id=ids.nextElement();
					if (!(multiDomain_LSP_list_old.containsKey(id))){
						multiDomain_LSP_list_to_add.put(id,  multiDomain_LSP_list.get(id));	
						multiDomain_LSP_list_old.put(id, multiDomain_LSP_list.get(id));
					}			
				}
				ids=multiDomain_LSP_list_old.keys();
				while (ids.hasMoreElements()){	
					Integer id=ids.nextElement();
					if (!(multiDomain_LSP_list.containsKey(id))){
						multiDomain_LSP_list_to_del.put(id, multiDomain_LSP_list.get(id));	
						//multiDomain_LSP_list_old.remove(id);
					}			
				}
				ids=multiDomain_LSP_list_to_del.keys();
				while (ids.hasMoreElements()){	
					Integer id=ids.nextElement();
					multiDomain_LSP_list_old.remove(id);			
				}			
			}
			
			Enumeration<Integer>ids=multiDomain_LSP_list_to_add.keys();
			while (ids.hasMoreElements()){	
				Integer id=ids.nextElement();
				String key;
				String value;
				key="LSP:"+id;
				value=lspToJSON(multiDomain_LSP_list_to_add.get(id));
				jedis.set(key,value);
				jedis.sadd("lsps",key);
			}
			
			//LSPs to delete
			
			ids=multiDomain_LSP_list_to_del.keys();
			while (ids.hasMoreElements()){	
				Integer id=ids.nextElement();
				String key;
				key="LSP:"+id;
				jedis.del(key);
				jedis.srem("lsps",key);
			}

		}
		

	}
	
	
	public String lspToJSON(MD_LSP lsp){
		
		Gson gson = new Gson();
		
		SimpleLSP slsp=new SimpleLSP();
		
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
	
	
	
	
	public Hashtable<Integer, MD_LSP> getMultiDomain_LSP_list() {
		return multiDomain_LSP_list;
	}
	
	
	public void setMultiDomain_LSP_list(Hashtable<Integer, MD_LSP> multiDomain_LSP_list) {
		this.multiDomain_LSP_list = multiDomain_LSP_list;
	}
	
	

}
