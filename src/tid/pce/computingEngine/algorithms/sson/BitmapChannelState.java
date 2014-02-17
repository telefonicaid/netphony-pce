package tid.pce.computingEngine.algorithms.sson;

import java.util.Arrays;

import tid.protocol.commons.ByteHandler;

public class BitmapChannelState {
	
	private byte[] bytesBitmap;
	private int length;
	
	private int sumBitsBitmap;
	
	public BitmapChannelState(){
	}
	
	public BitmapChannelState(int num_channels){
		this.setLength(num_channels);
		this.bytesBitmap = new byte[length/8+1];
	}
	

	public byte[] getBytesBitmap() {
		return bytesBitmap;
	}
	
	public void arraycopyBytesBitmap(byte[] bytesBitmap) {		
		System.arraycopy(bytesBitmap, 0, this.bytesBitmap, 0, bytesBitmap.length);
	}
	public void setBytesBitmap(byte[] bytesBitmap) {
		this.bytesBitmap = bytesBitmap;
		
	}
	public int getNumberBytes(){
		return bytesBitmap.length;
	}
	
	public int getNumberBytes(int num){
		int numberBytes = num/8;
		if ((numberBytes*8)<num){
			numberBytes++;
		}
		return numberBytes;
	}
	/*
	 * Initialize function sets all bits in the bitmap to '1', meaning
	 * that all channels are available for the connection request.
	 */
	
	public void Initialize(){
		for (int i=0; i<((length/8)+1)*8; i++){
			if (i>=length){
				if(i%8>length%8){
					bytesBitmap[i/8]=(byte)((bytesBitmap[i/8])&((0xFE)<<(8-(i%8))));
				}
				else{
					bytesBitmap[i/8]=(byte)0xFF;
				}
			}else{
				bytesBitmap[i/8]=(byte) 0xFF;
			}
		}
	}
	
	public void setLength(int length) {
		this.length = length;
	}

	public int getLength() {
		return length;
	}
	
	private boolean equalsBytes(byte[] bytes1, byte[] bytes2){		
		for (int i =0;i<bytes1.length;i++){
			if ((bytes1[i] | bytes2[i]) != (bytes1[i])){
				return false;
			}				
		}
		return true;
	}
	
	public int getSumaBits(){
		sumBitsBitmap=0;
		for (int i=0; i<bytesBitmap.length*8; i++){
			if ((bytesBitmap[i/8]&(0x80>>(i%8))) == (0x80>>i%8)){
				sumBitsBitmap = sumBitsBitmap + 1;
			}
		}
		
		return sumBitsBitmap;
	}

	@Override
	public String toString() {
		return "BitmapChannelState [bytesBitmap="
				+ Arrays.toString(bytesBitmap) + ", length=" + length + "]";
	}
	
}
