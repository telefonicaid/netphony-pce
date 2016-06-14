package es.tid.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.LinkedList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.w3c.dom.CharacterData;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import es.tid.pce.computingEngine.ComputingResponse;
import es.tid.rsvp.objects.subobjects.EROSubobject;
import es.tid.rsvp.objects.subobjects.GeneralizedLabelEROSubobject;
import es.tid.rsvp.objects.subobjects.SubObjectValues;


public class UtilsFunctions {

	public static void printByte(byte[] bytes,String name){
		System.out.print(name +":  ");
		for (int i =0;i<bytes.length;i++){
			if((bytes[i]&0xFF)<=0x0F){
				System.out.print("0"+Integer.toHexString(bytes[i]&0xFF));
			
			}else{
			System.out.print(Integer.toHexString(bytes[i]&0xFF));
			}
		}
	}

	public static void imprimeLambdas(ComputingResponse rep, Logger log){
		LinkedList<EROSubobject> erolist=rep.getResponseList().getFirst().getPathList().getFirst().geteRO().getEROSubobjectList();
		int size = erolist.size();
		for (int i=0;i< size-1;i++){
		if (erolist.get(i).getType()==SubObjectValues.ERO_SUBOBJECT_LABEL){								
				byte [] bytes = (((GeneralizedLabelEROSubobject) erolist.get(i)).getLabel());									
				int n=((bytes[2]&0xFF)<<8)|(bytes[3]&0xFF);															
				log.info("lambda devuelta:"+n);
			}
		}
	}

	public static void imprimeRespuesta(ComputingResponse rep, Logger log, long id){
		LinkedList<EROSubobject> erolist=rep.getResponseList().getFirst().getPathList().getFirst().geteRO().getEROSubobjectList();
		int size = erolist.size();
		int n=-1;
		boolean noEncontrada=true;
		for (int i=0;((i< size-1)&&(noEncontrada));i++){
		if (erolist.get(i).getType()==SubObjectValues.ERO_SUBOBJECT_LABEL){								
				byte [] bytes = (((GeneralizedLabelEROSubobject) erolist.get(i)).getLabel());									
				n=((bytes[2]&0xFF)<<8)|(bytes[3]&0xFF);														
				noEncontrada=false;
			}
		}
		
		log.info("REQUEST - ID: "+ id +" RESPUESTA: "+erolist.toString()+" LAMDBA: "+n);
	
	}

	public static void printByte(byte[] bytes,String name, Logger log){
		
		String s= name +":  ";
		for (int i =0;i<bytes.length;i++){
			if((bytes[i]&0xFF)<=0x0F){
				s=s+("0"+Integer.toHexString(bytes[i]&0xFF));
			
			}else{
				s=s+(Integer.toHexString(bytes[i]&0xFF));
			}
		}
			log.info(s);
	}

	public static String getCharacterDataFromElement(Element e)
	{
		Node child = e.getFirstChild();
		if (child instanceof CharacterData) 
		{
			CharacterData cd = (CharacterData) child;
			return cd.getData();
		} 
		else 
		{
			return "?";
		}
	}

	public static String exceptionToString(Exception e)
	{
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		return sw.toString();
	}


}
