package tid.pce.computingEngine.algorithms.wson.wa;


import java.util.List;

import es.tid.ospf.ospfv2.lsa.tlv.subtlv.complexFields.BitmapLabelSet;
import tid.pce.tedb.IntraDomainEdge;

public class FirstFit {
		
	public FirstFit(){
		
	}
	
	public static int getLambda(List<IntraDomainEdge> edge_list){
		int max_lambdas=((BitmapLabelSet)edge_list.get(0).getTE_info().getAvailableLabels().getLabelSet()).getNumLabels();
		
		int num_bytes=((BitmapLabelSet)edge_list.get(0).getTE_info().getAvailableLabels().getLabelSet()).getBytesBitMap().length;
		byte [] bitmap=new byte[num_bytes];
		for (int i=0;i<num_bytes;++i){
			bitmap[i]=(byte)(((BitmapLabelSet)edge_list.get(0).getTE_info().getAvailableLabels().getLabelSet()).getBytesBitMap()[i]|((BitmapLabelSet)edge_list.get(0).getTE_info().getAvailableLabels().getLabelSet()).getBytesBitmapReserved()[i]);	
		}
		
		for (int j=1;j<edge_list.size();++j){
			for (int i=0;i<num_bytes;++i){
				bitmap[i]=(byte)(bitmap[i] | (((BitmapLabelSet)edge_list.get(j).getTE_info().getAvailableLabels().getLabelSet()).getBytesBitMap()[i]|((BitmapLabelSet)edge_list.get(j).getTE_info().getAvailableLabels().getLabelSet()).getBytesBitmapReserved()[i]));	
			}
		}
		for (int i=0; i<max_lambdas;++i){
			int num_byte=i/8;
			if ( (bitmap[num_byte]&(0x80>>>(i%8)))==0){
				return i;
			}			
		}
			
		return -1;
		
	}
	    


		

}
