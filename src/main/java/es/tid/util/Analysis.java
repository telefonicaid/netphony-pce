package es.tid.util;

/**
 * Class for analysis of simulation results
 * Port of the analysis.cpp class of Ignacio de Miguel and David Rodriguez Alfayate
 
* This class is used to analyze the simulation results.
* The method employed is that partially explained in the book:
*
* A.M. Law, W. Kelton, Simulation Modeling and Analysis, 
* 2nd ed. McGraw-Hill, 1991.
*
* and fully described in:
*
* A.M. Law, J.S. Carson, A sequential procedure for 
* determining the length of a steady-state simulation, 
* Operations Research, vol. 27, no. 5, pp. 1011-1025, 1979.
 * @author ogondio
 *
 */
//*  @brief Provides the simulation analysis
public class Analysis {
	
	//static final values
	private static final int NUM_BATCHES = 400;
	private static final int EVEN =1;
	private static final int ODD = 0;
	private static final int HALF = 2;
    /* By decreasing THRESHOLD and GAMMA more
	// strict requirements are set in order to
	// declare that the estimation of the parameter 
	// has converged.
	// Hence, by decreasing these values, simulations
	// run longer but provide more accurate estimates of
	// the averages and a smaller confidence interval*/
	private static final double  THRESHOLD = 0.4;
	private static final double   GAMMA    = 0.075;
	private static final double   t_STUDENT= 1.960;	

	private double[][] averages =new double[2][NUM_BATCHES];	// It stores the averages with the result of the analysis.
	private double rohat;			// Correlation.
	private int[] position_batch= new int[2];		// Posicion of the batch where we are
	private long[] size_batch= new long[2];		// Size of the batch
	private int[] batch= new int[2];			// Current batch
	private long[] samples= new long[2];		// Total samples per batch
	private int iter;			// Number of iteration
	private int converge;			// Converge or not.
	private double total_average;	// Average of all batches.
	private double numerator_sy2;	// Numerator used in calculation of conf. interval
	private long[] Max_samples= new long[2];   // Maximum number of samples in even and odd iterations

	public Analysis(){
		size_batch[EVEN]=3;
		size_batch[ODD]=2;
		converge=0;
		Max_samples[EVEN]=1200;
		Max_samples[ODD]=800;
		samples[EVEN]=0;
		samples[ODD]=0;
		position_batch[EVEN]=0;
		position_batch[ODD]=0;
		averages[EVEN][0]=0;
		averages[ODD][0]=0;
		batch[EVEN]=batch[ODD]=0;
		total_average=0;
		numerator_sy2=0;
		iter=0;
	}
	
	// This function implements the (approximately) simultaneous analysis of the
	// samples that are being collected.
	public boolean analyze(double elem)
	{
		// elem is the element to analyze.
		// We calculate the averages corresponding to 
		// even and odd iterations
		boolean finalized_iteration=false;
		double rohatold;
		boolean end;
		averages[EVEN][batch[EVEN]]+=elem;
		averages[ODD][batch[ODD]]+=elem;
		position_batch[EVEN]+=1;
		position_batch[ODD]+=1;
		// We check whether we have completed any batch.
		if(position_batch[EVEN]==size_batch[EVEN]) {
			// We divide by the size of the batch:
			averages[EVEN][batch[EVEN]]/=size_batch[EVEN];
			// We increase the batch in one unit
			batch[EVEN]+=1;
			position_batch[EVEN]=0;
			if(batch[EVEN]<NUM_BATCHES) averages[EVEN][batch[EVEN]]=0; // We put this element to 0.
		}
		if(position_batch[ODD]==size_batch[ODD]) {
			averages[ODD][batch[ODD]]/=size_batch[ODD];
			batch[ODD]+=1;
			position_batch[ODD]=0;
			if(batch[ODD]<NUM_BATCHES) averages[ODD][batch[ODD]]=0;
		}
		samples[EVEN]+=1;
		samples[ODD]+=1;

		// We check whether we have reached the maximum number of samples
		// that made the block that we are analyzing.
		if(samples[EVEN]==Max_samples[EVEN]) {
			// End of the batch. 
			iter++;
			calculate_rohat(EVEN,1);
			// The number of samples for the next even iteration is
			// twice the current number:
			Max_samples[EVEN]*=2;
			size_batch[EVEN]=Max_samples[EVEN]/NUM_BATCHES;
			calculate_averages(EVEN); // Comment 1. /* <1> */
			// We have to divide by 2 the batch where we are
			batch[EVEN]/=2;
			averages[EVEN][batch[EVEN]]=0;
			// We check the calculated value of the correlation.
			if(rohat<=0) {
				// If we are below the confidence threshold, everything is OK
				if(check_gamma()) {
					end=true;
				}
				else end=false;
			}
			else {
				if(0<rohat && rohat<THRESHOLD) {
					// We have to reorganized the array with the averages.
					// The first half of the array must contain averages of 2*l samples
					// Regarding the second half, it does not matter.
					/* <1> */
					// But this operation has to be done for the next even block
					// and that is why we do it at the beggining of the check
					// and we earn time. Comment 1.
					// So, we only have to check the correlation
					rohatold=rohat;	
					calculate_rohat(EVEN,HALF); // But only of the first half
								    // of the averages.
					if(rohat<rohatold && check_gamma()) {
						// Confidence threshold, we have finished the task.
						end=true;
					}
					else {
						end=false;
					}
				}
				else {
					end=false;
				}
			}
		        finalized_iteration=true;
			// If end equals to 1, we have finished the task,
			// so that we set converge to 1 and return to the calling program.
			if(end) {
				converge=1;
				return finalized_iteration;
			}
		}
		// Now, exactly the same procedure but for the ODD iteration
		if(samples[ODD]==Max_samples[ODD]) {
			iter++;
			calculate_rohat(ODD,1);
			Max_samples[ODD]*=2;
			size_batch[ODD]=Max_samples[ODD]/NUM_BATCHES;
			calculate_averages(ODD);
			batch[ODD]/=2;
			averages[ODD][batch[ODD]]=0;
			if(rohat<=0) {
				if(check_gamma()) {
					end=true;
				}
				else end=false;
			}		
			else {
				if(0<rohat && rohat<THRESHOLD) {
					rohatold=rohat;
					calculate_rohat(ODD,HALF);
					if(rohat<rohatold && check_gamma()) {
						end=true;
					}
					else end=false;
				}
				else end=false;
			}
		        finalized_iteration=true;
			if(end) {
				converge=1;
				return finalized_iteration;
			}
		}
		return finalized_iteration;
	}
	
	void calculate_averages(int iteration)
	{
		// The iteration parameter indicates whether we are
		// in an EVEN or ODD operation.
		int i;
		int k=0;
		// Special case
		for(i=0;i<NUM_BATCHES/2;i++) {
			// As it can be seen, we go from 2 blocks in 2 blocks
			averages[iteration][i]=averages[iteration][k]+averages[iteration][k+1];
			averages[iteration][i]/=2;
			k+=2;
		}
		return;
	}

	int calculate_rohat(int iteration,int which_part)
	{
		// The iteration parameter tells us whether we work with even
		// or odd iterations. The which_part parameter tells us
		// whether we are going to deal with all the batches (NUM_BATCHES)
		// or only with half of them
		int number_batches=NUM_BATCHES/which_part;
		rohat=-0.5*(calculate_ro(iteration,number_batches,1)+calculate_ro(iteration,number_batches,2))+2*calculate_ro(iteration,number_batches,0);
		return 1;
		// The imposed order is crution in order to keep the correct value
		// of parameters such as total_average
	}

	double calculate_ro(int iteration,int number_batches,int indicator)
	{
		int from=0;
		int to=number_batches;
		double numerator=0;
		double denominator=0;
		double old=0;
		double aux=0;
		int i;
		switch (indicator) {
			case 1:
				to >>= 1;
				break;
			case 2:
				from = number_batches >> 1;
				break;
		}
		// We introduce in total_average the average value of the averages
		// by means of the average_from_to function.
		average_from_to(iteration,from,to);
		denominator=old=averages[iteration][from]-total_average;
		denominator*=denominator;
		for(i=from+1;i<=to-1;i++) {
			aux=averages[iteration][i]-total_average;
			numerator+=old*aux;
			denominator+=aux*aux;
			old=aux;
		}
		numerator_sy2=denominator;
		if((int)denominator==0) {
			return 0;
		}
		numerator/=denominator;
		return numerator;
	}

	void average_from_to(int iteration,int from, int to)
	{
		int i=0;
		total_average=0;
		for(i=from;i<to;i++) {
			total_average+=averages[iteration][i];
		}
		total_average/=(to-from);
	}	

	public boolean check_gamma()
	{
		double delta;
		delta=t_STUDENT*Math.sqrt(numerator_sy2/(NUM_BATCHES*(NUM_BATCHES-1)));
		if(total_average==0) {
			return false; // If the total average is 0 we say that it has NOT converged.
				// (This is required for evaluating, e.g., low blocking probabilities,
				// we force the simulation to continue running even if we have
				// a long sequence of samples being 0.)
		}
		delta/=Math.abs(total_average);
		// We compare delta with the gamma threshold.
		if(delta>GAMMA) return false;
		// If we get to this point, the gamma threshold has been exceeded.
		return true;
	}

	public String result()
	{
		String ret=total_average + "+-" + t_STUDENT*Math.sqrt(numerator_sy2/(NUM_BATCHES*(NUM_BATCHES-1))/1000);
		return ret;
	}

	public int getConverge() {
		return converge;
	}	
	
	

}
