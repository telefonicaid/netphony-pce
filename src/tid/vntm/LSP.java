package tid.vntm;

import java.util.Iterator;
import java.util.LinkedList;

import es.tid.rsvp.objects.subobjects.EROSubobject;

public class LSP {
	private int id;
	private LinkedList<EROSubobject> EROSubobjectList;
	
	
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public LinkedList<EROSubobject> getEROSubobjectList() {
		return EROSubobjectList;
	}
	public void setEROSubobjectList(LinkedList<EROSubobject> eROSubobjectList) {
		EROSubobjectList = eROSubobjectList;
	}
	
	@Override
	public String toString() {
		// TODO Auto-generated method stub
		Iterator<EROSubobject> iteratorERO  = EROSubobjectList.iterator();
		String string="";
		EROSubobject eROSubobject;
		while (iteratorERO.hasNext()){
			eROSubobject = iteratorERO.next();
			string = string + eROSubobject.toString()+"\n";
		}
		return string;
	}
	
	
	
	
}
