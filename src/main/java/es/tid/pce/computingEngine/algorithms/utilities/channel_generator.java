package es.tid.pce.computingEngine.algorithms.utilities;

import java.util.ArrayList;
import java.util.logging.Logger;

import es.tid.ospf.ospfv2.lsa.tlv.subtlv.complexFields.BitmapLabelSet;

public class channel_generator{
	
	private Logger log;
	private static byte[] bytesBitmap;
	private static byte[] bytesBitmapInit;
	private static byte[] bytesBitmapInit2;
	private static int numberBytes;
	public channel_generator(){
		log=Logger.getLogger("PCEServer");
		}
	
	/** This function is the responsible of generate the channelSet required for the
	 * Dynamic_RSA algorithm, for flexigrid-based networks.
	 * Basically it generates a set of bitmaps with all possible combination of "num_slots"
	 * empty slots.
	 * 
	 * An example: If you want to generate the ChannelSet of two slots in a spectrum of a total
	 * amount of 8 slots:
	 * 
	 * The set will be as follows:
	 * 00111111
	 * 10011111
	 * 11001111
	 * 11100111
	 * 11110011
	 * 11111001
	 * 11111100
	 * 
	 * @author: Amll
	 */
	
	public void getSetChannels(int num_labels, int num_slots, ArrayList<BitmapLabelSet> setChannels){
		
		int bitCounter=0;
		int num_bytes_empty = (int) Math.floor(num_slots/8);
		numberBytes = getNumberBytes(num_labels);
		bytesBitmapInit = new byte[numberBytes];
		bytesBitmapInit2 = new byte[numberBytes];
		
		if(num_slots>num_labels){
			throw new IllegalArgumentException("Number of requested slots greater then the total number of slots");
		}
		for (int i=0; i<(num_labels-num_slots + 1);i++){
			BitmapLabelSet channel = new BitmapLabelSet();
			bytesBitmap = new byte[numberBytes];
			// In case of multiple of 8 required slots there is a slight difference in the programming.
			if (num_slots<8){
				// First channel generation.
				if (i==0){
					for (int k=0; k<num_labels;k++){
						if ( Math.floor(k/8)==0){
							bytesBitmapInit[k/8]=(byte) (bytesBitmapInit[k/8]|(byte)((0xFF)&(((byte)(0x40))>>bitCounter)));
							
							if(bitCounter<(num_slots-1)){
								bitCounter++;
							}
						}
						else{
							bytesBitmapInit[k/8]=(byte) 0x00;
						}
					}
					for (int k=0; k<(numberBytes);k++){
						if (k==i/8){
							bytesBitmap[k] =(byte) (~(byte)(bytesBitmapInit[0]<<(1)));
						}
						else{
							bytesBitmap[k]=(byte)(~bytesBitmapInit[k]);
						}
					}
				}
			
				else{
					for (int k=0; k<(numberBytes);k++){
						if (k==i/8){
							if(i%8==0){
								bytesBitmap[k] =(byte) (bytesBitmapInit[0]<<(1));
								bytesBitmap[k]=(byte) (~(bytesBitmap[k]));
								}
							else{
								bytesBitmap[k] =(byte) (bytesBitmapInit[0]>>>(i%8-1));
								bytesBitmap[k]=(byte) (~(bytesBitmap[k]));
							}
						}
						else if(k==i/8+1){
							if(i%8>(8-num_slots)){
								bytesBitmap[k] =(byte) (bytesBitmapInit[0]<<(8-(i%8-1)));
								bytesBitmap[k]=(byte) (~bytesBitmap[k]);
							}
							else{
								bytesBitmap[k]=(byte) (0xFF);
							}
							
						}
						else{
							bytesBitmap[k]=(byte) (0xFF);
						}
										
					}
				}
			}
	
			/**********************************************************************************/

			//If num_slots>8:		
			else{
				// First channel generation.
				if (i==0){
					if (num_slots%8==0){
						bytesBitmapInit2[num_bytes_empty]= (byte) (bytesBitmapInit2[num_bytes_empty] | (0x80));
					}
					for (int k=0; k<num_labels;k++){
						if(k/8<num_bytes_empty){
							bytesBitmapInit[k/8]= (byte) 0xFF;
							bytesBitmapInit2[k/8]= (byte) 0xFF;
						}
						else if(k/8==num_bytes_empty){
							for (int r=0;r<num_slots%8;r++){
								bytesBitmapInit[k/8]= (byte) (bytesBitmapInit[k/8] | (0x40)>>>r);
								bytesBitmapInit2[k/8]= (byte) (bytesBitmapInit2[k/8] | (0x80));
							}
						}
						else{
							bytesBitmapInit[k/8]= (byte) 0x00;
							bytesBitmapInit2[k/8]= (byte) 0x00;
						}
					}
					for (int k=0; k<numberBytes; k++){
						if (k==num_bytes_empty){
							bytesBitmap[k]=(byte) (~(byte) ((bytesBitmapInit[k])<<(1)));
						}else{
							bytesBitmap[k]=(byte) ~(bytesBitmapInit[k]);
						}
					}
				}
				else{
					for (int k=0; k<(num_labels);k++){
						if (k/8<(i/8)){
							bytesBitmap[k/8]=(byte) 0xFF;
						}
						else if (((i/8)<=(k/8))&&((k/8)<(num_bytes_empty+(i/8)))){
							if (k/8==i/8){
								if (i%8==0){
									bytesBitmap[k/8]=(byte) (bytesBitmapInit2[num_bytes_empty]<<1);
								}else{	
									bytesBitmap[k/8]=(byte) (bytesBitmapInit2[num_bytes_empty]>>>(i%8-1));
								}
							}
							else{
								bytesBitmap[k/8]=(byte) 0x00;
							}
						}
						else if((k/8)==(num_bytes_empty+(i/8))){
							if ((i%8==0)&&(num_slots%8==0)){
								bytesBitmap[k/8]=(byte) 0xFF;
							}else{	
								bytesBitmap[k/8]=(byte)(~(byte)((bytesBitmapInit2[num_bytes_empty])>>>(i%8+num_slots%8 - 1)));
							}
						}
						else{
							if ((k/8)==(num_bytes_empty+(i/8))+1){
								if (i%8>(8-num_slots%8)){
									bytesBitmap[k/8]=(byte)(~(byte)((bytesBitmapInit2[num_bytes_empty])>>>(i%8-(8-num_slots%8)-1)));
								}
								else{
									bytesBitmap[k/8]=(byte) 0xFF;
								}
							}
							else{
								bytesBitmap[k/8]=(byte) 0xFF;
							}
						}
					}
				}
			}
			channel.setBytesBitmap(bytesBitmap);
			setChannels.add(i,channel);
		}
//		for(int p=0;p<(SetChannels.size());p++){
//			log.info("Channel:"+p+" "+toHexString(SetChannels.get(p).getBytesBitMap())+"\n");
//		}
	}
	
	
	private int getNumberBytes(int num){
		int numberBytes = num/8;
		if ((numberBytes*8)<num){
			numberBytes++;
		}
		return numberBytes;
	}
	
	public static String toHexString(byte [] packetBytes){
		 StringBuffer sb=new StringBuffer(packetBytes.length*2);
		 for (int i=0; i<packetBytes.length;++i){
		  if ((packetBytes[i]&0xFF)<=0x0F){
		   sb.append('0');
		   sb.append(Integer.toHexString((packetBytes[i]&0xFF))); 
		  }
		  else {
		   sb.append(Integer.toHexString((packetBytes[i]&0xFF)));
		  }
		 }
		 return sb.toString();
		 
		 }
}
