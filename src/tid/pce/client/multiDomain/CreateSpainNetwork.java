package tid.pce.client.multiDomain;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.net.Inet4Address;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.CharacterData;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class CreateSpainNetwork {
	 public static void main(String[] args){
	File readFile = new File(args[0]);
    FileWriter writeFile = null;
    PrintWriter pw = null;
	try {
		writeFile = new FileWriter("D:/PCE/Spain_Network.txt");
        pw = new PrintWriter(writeFile);

		DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		Document doc = builder.parse(readFile);
		
		NodeList nodes_domains = doc.getElementsByTagName("domain");

		for (int j = 0; j < nodes_domains.getLength(); j++) {
			
			NodeList edges = doc.getElementsByTagName("edge");
			for (int i = 0; i < edges.getLength(); i++) {

				Element element = (Element) edges.item(i);

				NodeList source = element.getElementsByTagName("source");
				Element source_router_el = (Element) source.item(0);
				NodeList source_router_id = source_router_el.getElementsByTagName("router_id");
				Element source_router_id_el = (Element) source_router_id.item(0);
				String s_r_id = getCharacterDataFromElement(source_router_id_el);
				Inet4Address s_router_id_addr = (Inet4Address) Inet4Address.getByName(s_r_id);
				pw.append("("+String.valueOf(i)+",");
				pw.append(String.valueOf(s_router_id_addr.hashCode())+",");

				NodeList dest_nl = element
						.getElementsByTagName("destination");
				Element dest_el = (Element) dest_nl.item(0);
				NodeList dest_router_id_nl = dest_el
						.getElementsByTagName("router_id");
				Element dest_router_id_el = (Element) dest_router_id_nl
						.item(0);
				String d_r_id = getCharacterDataFromElement(dest_router_id_el);

				Inet4Address d_router_id_addr = (Inet4Address) Inet4Address.getByName(d_r_id);
				pw.append(String.valueOf(d_router_id_addr.hashCode())+",");

				pw.println("1,0,0,0,80),");
			

			}
		}
	} catch (Exception e) {
		e.printStackTrace();
	}
	finally {
        try {
        // Nuevamente aprovechamos el finally para 
        // asegurarnos que se cierra el fichero.
        if (null != writeFile)
           writeFile.close();
        } catch (Exception e2) {
           e2.printStackTrace();
        }
	}
}
		public static String getCharacterDataFromElement(Element e) {
			Node child = e.getFirstChild();
			if (child instanceof CharacterData) {
				CharacterData cd = (CharacterData) child;
				return cd.getData();
			} else {
				return "?";
			}
		}
		}
