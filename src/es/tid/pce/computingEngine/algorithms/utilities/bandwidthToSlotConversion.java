package es.tid.pce.computingEngine.algorithms.utilities;

import java.util.logging.Logger;

public class bandwidthToSlotConversion {
	
	private Logger log=Logger.getLogger("PCEServer");
	private int numSlots; 
	private int mf; // 1 - NRZ-OOK
					// 2 - DP-QPSK
					// 3 - DP-16-QAM
	
	public int getNumSlots (float Bw, int cs){
		boolean exact=false;
		int spectralEfficiency = 4; // 2 - DP-QPSK
		long guardBand = 7000000000L;
		double fec = 0.12;
		
		if (Bw!=0){
			//log.info("Cs= "+cs);
			if (cs==1){
				numSlots=(int) (Math.floor(Bw/(spectralEfficiency*100000000000L)));
				if (((Bw/spectralEfficiency)%100000000000L)==0){
					exact=true;
				}
			}
			else if (cs==2){
				numSlots=(int) (Math.floor(Bw/(spectralEfficiency*50000000000L)));
				if (((Bw/spectralEfficiency)%50000000000L)==0){
					exact=true;
				}
			}
			else if (cs==3){
				numSlots=(int) (Math.floor(Bw/(spectralEfficiency*25000000000L)));
				if (((Bw/spectralEfficiency)%25000000000L)==0){
					exact=true;
				}
			}
			else if (cs==4){
				numSlots=(int) (Math.floor(Bw/(spectralEfficiency*12500000000L)));
				if (((Bw/spectralEfficiency)%12500000000L)==0){
					exact=true;
				}
			}
			else if (cs==5){
				//log.info("Guardband: "+guardband);
				numSlots=(int) (Math.floor((((Bw*(1+fec))/spectralEfficiency)+guardBand)/6250000000L));
				//log.info("Num_slots: "+((Math.round(Bw*(1+fec))/spectralEfficiency)+guardBand));
				if ((((Math.round(Bw*(1+fec))/spectralEfficiency)+guardBand)%6250000000L)==0){
					exact=true;
				}
			}
		}
		
		if (exact==false ){
			if (cs==5){
				if(numSlots%2!=0){
					numSlots = numSlots + 1;
				}
				else{
					numSlots = numSlots + 2;
				}
			}
			else{
				numSlots=numSlots + 1;
			}
		}
		return numSlots;
	}
}
