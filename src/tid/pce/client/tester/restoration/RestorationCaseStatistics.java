package tid.pce.client.tester.restoration;

import java.util.ArrayList;

public class RestorationCaseStatistics {

	/**
	 * Tiempo de restauración por conexión (guardar todos)
	 */
	private ArrayList<Double> restorationTime;
	/**
	 * Numero de reintentos (guardar todos)
	 */
	private ArrayList<Integer> numberRetries;
	
	/**
	 * Tiempo máximo de restauracion por ejecucion
	 */
	private double maxTime;
	/**
	 * Tiempo medio de restauracion por ejecucion
	 */
	private double meanTime;
	/**
	 * Numero maximo de reintentos que una request tiene que enviarse para conseguir establecerse
	 * por ejecucion. 
	 */
	private int maxRestries;
	/**
	 * Numero medio de reintentos que una request tiene que enviarse para conseguir establecerse
	 * por ejecucion. 
	 */
	private double meanRestries;
	/**
	 * Guarda todos los tiempos de LSP, tiempo en que tarda en configurarse un camino.
	 */
	private ArrayList<Double> LSPTimeList;
	/**
	 * Guarda la media de los LSP time
	 */
	double meanLSPTime;
	
	/**
	 * Constructor
	 */
	public RestorationCaseStatistics(){
		restorationTime = new ArrayList<Double>();
		numberRetries	= new ArrayList<Integer>();
		LSPTimeList= new ArrayList<Double>();
	}
	
	/***********************************************
	 * Add elements. Syncronized methods
	 **********************************************/
	public synchronized void addRestorationTime(double elem) {
		restorationTime.add(elem);
	}

	public synchronized void addNumberRetries(int elem) {
		this.numberRetries.add(elem);
	}
	public synchronized void addLSPTime(Double elem){
		LSPTimeList.add(elem);
	}
	
	/*********************************************
	 * Getters and setters
	 **********************************************/
	public double getMaxTime() {
		return maxTime;
	}
	public void setMaxTime(double maxTime) {
		this.maxTime = maxTime;
	}

	public double getMeanTime() {
		return meanTime;
	}

	public void setMeanTime(double meanTime) {
		this.meanTime = meanTime;
	}

	public int getMaxRestries() {
		return maxRestries;
	}

	public void setMaxRestries(int maxRestries) {
		this.maxRestries = maxRestries;
	}

	public double getMeanRestries() {
		return meanRestries;
	}

	public void setMeanRestries(int meanRestries) {
		this.meanRestries = meanRestries;
	}
	
	/***********************************************
	 * Calculos
	 **********************************************/
	public void calculateTime(){
		maxTime = restorationTime.get(0);

		for (int i =0; i<restorationTime.size();i++){
			meanTime = meanTime + restorationTime.get(i);
			if (maxTime < restorationTime.get(i)){
				maxTime = restorationTime.get(i);
			}
		}
		meanTime = (meanTime / ((double)restorationTime.size()));
	}
	public void calculateRetries(){
		maxRestries = numberRetries.get(0);
		for (int i =0; i<numberRetries.size();i++){
			meanRestries = meanRestries + numberRetries.get(i);
			if (maxRestries < numberRetries.get(i)){
				maxRestries = numberRetries.get(i);
			}
		}
		meanRestries = (meanRestries / numberRetries.size());
	}
	public synchronized void calculateLSPTime(){
		meanLSPTime = LSPTimeList.get(0);
		for (int i =0; i<LSPTimeList.size();i++){
			meanLSPTime = meanLSPTime + LSPTimeList.get(i);			
		}
		meanLSPTime = (meanLSPTime / LSPTimeList.size());
	}
	/***********************************************
	 * Imprimir
	 **********************************************/
	public String printTimes(){
		StringBuffer sb=new StringBuffer(1000);
		for (double i: restorationTime){
			sb.append(Double.toString(i/1000000)+"\t");
		}		
		return sb.toString();		
	}
	
	public String printAttemps(){
		StringBuffer sb=new StringBuffer(1000);
		for (int i: numberRetries){
			sb.append(i+"\t");
		}		
		return sb.toString();		
	}
	public String printStatistics(){
		StringBuffer sb=new StringBuffer(1000);
		sb.append("maxTime\t");
		sb.append((maxTime/1000000));
		sb.append("\t meanTime\t");
		sb.append((meanTime/1000000));
		sb.append("\tmaxRestries\t");
		sb.append(maxRestries);	
		sb.append("\tmeanRestries\t");
		sb.append(meanRestries);	
		sb.append("\tLSPTime\t");
		sb.append(meanLSPTime);

		
		return sb.toString();
	}
	
}
