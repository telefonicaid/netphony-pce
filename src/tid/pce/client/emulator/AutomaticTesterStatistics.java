package tid.pce.client.emulator;

import java.util.ArrayList;

import tid.util.Analysis;

public class AutomaticTesterStatistics {
	
	private long numRequests=0;
	
	private long numMLResponses=0;
	
	private long numSLResponses=0;
	
	private long numIPResponses=0;
	
	private double TrafficHops=0;
	
	private long NumWL=0;
	
	private long numNoPathResponses=0;
	private long numNoResponses=0;
	private Analysis blockProbability;
	private Analysis lambdaBlockingProbability;
	private Analysis meanReqTime;
	private Analysis blockProbabilityWithoutStolenLambda;
	//private boolean printRequestTime;

	private double load;
	private long numberActiveLSP;
	private long numberStolenLambdasLSP;
	private long numberStolenBWLSP;
	private boolean bidirectional;
	private int numberNodes;
	private ArrayList<Double> reqTimeList;
	private long numElems;
	private double meanReqTimePrev;
	private double meanReqTimeArray;
	
	public AutomaticTesterStatistics( ){
		blockProbability=new Analysis();
		meanReqTime=new Analysis();	
		lambdaBlockingProbability= new Analysis();
	
		numRequests=0;
		numMLResponses=0;
		numSLResponses=0;
		numIPResponses=0;
		numNoPathResponses=0;
		numberActiveLSP=0;
		numberStolenLambdasLSP=0;
		numberStolenBWLSP=0;
		numNoResponses=0;
		TrafficHops=0;
		numElems=0;
		meanReqTimePrev=0;
		LSPTime=new Analysis();
		theoricLSPTime=new Analysis();
		blockProbabilityWithoutStolenLambda=new Analysis();
		reqTimeList=new ArrayList<Double>();
		NumWL=0;
	}
	
	public AutomaticTesterStatistics( double load){
		blockProbability=new Analysis();
		meanReqTime=new Analysis();	
		lambdaBlockingProbability= new Analysis();
		this.load=load;
		numRequests=0;
		numMLResponses=0;
		numSLResponses=0;
		numIPResponses=0;
		numNoPathResponses=0;
		numberActiveLSP=0;
		numberStolenLambdasLSP=0;
		numberStolenBWLSP=0;
		numNoResponses=0;
		TrafficHops=0;
		numElems=0;
		meanReqTimePrev=0;
		LSPTime=new Analysis();
		theoricLSPTime=new Analysis();
		blockProbabilityWithoutStolenLambda=new Analysis();
		reqTimeList=new ArrayList<Double>();
		NumWL=0;
	}
	
	
	public void setNumberNodes(int numberNodes) {
		this.numberNodes = numberNodes;
	}


	public boolean isBidirectional() {
		return bidirectional;
	}


	public void setBidirectional(boolean bidirectional) {
		this.bidirectional = bidirectional;
	}

	//pruebas
	private Analysis LSPTime;
	private Analysis theoricLSPTime;
	
	public Analysis getBlockProbabilityWithoutStolenLambda() {
		return blockProbabilityWithoutStolenLambda;
	}


	public void setBlockProbabilityWithoutStolenLambda(
			Analysis blockProbabilityWithoutStolenLambda) {
		this.blockProbabilityWithoutStolenLambda = blockProbabilityWithoutStolenLambda;
	}

	public long getNumNoResponses() {
		return numNoResponses;
	}


	public void setNumNoResponses(long numNoResponses) {
		this.numNoResponses = numNoResponses;
	}

	public long getNumberStolenLambdasLSP() {
		return numberStolenLambdasLSP;
	}

	public void setNumberStolenLambdasLSP(long numberStolenLambdasLSP) {
		this.numberStolenLambdasLSP = numberStolenLambdasLSP;
	}

	public void addNumberActiveLSP(){
		numberActiveLSP+=1;
	}
	public void releaseNumberActiveLSP(){
		numberActiveLSP-=1;
	}
	public long getNumberActiveLSP() {
		return numberActiveLSP;
	}

	public void setNumberActiveLSP(long numberActiveLSP) {
		this.numberActiveLSP = numberActiveLSP;
	}

	public Double getLoad() {
		return load;
	}

	public void setLoad(Double load) {
		this.load = load;
	}
	public synchronized void analyzeLSPTime(Double elem){
		this.LSPTime.analyze(elem);
	}
	public synchronized void analyzeTheoricLSPTime(Double elem){
		this.theoricLSPTime.analyze(elem);
	}

//	public boolean isPrintRequestTime() {
//		return printRequestTime;
//	}
//
//	public void setPrintRequestTime(boolean printRequestTime) {
//		this.printRequestTime = printRequestTime;
//	}

	public long getNumRequests() {
		return numRequests;
	}

	public void setNumRequests(long numRequests) {
		this.numRequests = numRequests;
	}

	public Analysis getBlockProbability() {
		return blockProbability;
	}

	public void setBlockProbability(Analysis blockProbability) {
		this.blockProbability = blockProbability;
	}

	public Analysis getLambdaBlockingProbability() {
		return lambdaBlockingProbability;
	}

	public void setLambdaBlockingProbability(Analysis lambdaBlockingProbability) {
		this.lambdaBlockingProbability = lambdaBlockingProbability;
	}

	public Analysis getMeanReqTime() {
		return meanReqTime;
	}

	public void setMeanReqTime(Analysis meanReqTime) {
		this.meanReqTime = meanReqTime;
	}

	public long getNumMLResponses() {
		return numMLResponses;
	}

	public void setNumMLResponses(long numMLResponses) {
		this.numMLResponses = numMLResponses;
	}

	public long getNumSLResponses() {
		return numSLResponses;
	}

	public void setNumSLResponses(long numSLResponses) {
		this.numSLResponses = numSLResponses;
	}

	public long getNumNoPathResponses() {
		return numNoPathResponses;
	}

	public void setNumNoPathResponses(long numNoPathResponses) {
		this.numNoPathResponses = numNoPathResponses;
	}
	
	 public synchronized void addNoResponse(){
		 numNoResponses+=1;
	}
	 
	 public synchronized void addTrafficHops(double trafficHops){
		 TrafficHops=TrafficHops+trafficHops;
	}
	 public synchronized void addNumWL(long numWL){
		 NumWL=NumWL+numWL;
	}
	 

	 public synchronized void addStolenLambdasLSP(){
		 numberStolenLambdasLSP+=1;
	}
	 public void addStolenBWLSP() {
		 numberStolenBWLSP+=1;			
	}
	public synchronized void addMLResponse(){
		numMLResponses+=1;
	}
	
	public synchronized void addSLResponse(){
		numSLResponses+=1;
	}
	
	public synchronized void addNoPathResponse(){
		numNoPathResponses+=1;
	}
	
	public synchronized void addIPResponse() {
		numIPResponses+=1;
	}	
	public synchronized void analyzeReqTime (double elem){
		reqTimeList.add(elem);
		numElems++;
		meanReqTimePrev=((((numElems-1)/numElems))*meanReqTimePrev)+(elem/(double)numElems);
		double meanReqTimeArrayPrev=0;
		for (Double d:reqTimeList)
			meanReqTimeArrayPrev=meanReqTimeArrayPrev+d;
		meanReqTimeArrayPrev=meanReqTimeArrayPrev/((double)reqTimeList.size());
		meanReqTimeArray= (meanReqTimeArray+meanReqTimeArrayPrev)/((double)2);
		meanReqTime.analyze(elem);
	}
	
	public synchronized void analyzeBlockingProbability (double elem){
		blockProbability.analyze(elem);
	}
	
	public synchronized void addRequest(){
		numRequests+=1;
	}
	public synchronized void analyzeLambdaBlockingProbability (double elem){
		lambdaBlockingProbability.analyze(elem);
	}
	public synchronized void analyzeblockProbabilityWithoutStolenLambda(double elem){
		blockProbabilityWithoutStolenLambda.analyze(elem);
	}
	public String print(){
		StringBuffer sb=new StringBuffer(1000);

		sb.append("numRequests\t");
		sb.append(numRequests);
		sb.append("\tnumMLResponses\t");
		sb.append(numMLResponses);
		sb.append("\tnumSLResponses\t");
		sb.append(numSLResponses);
		sb.append("\tnumIPResponses\t");
		sb.append(numIPResponses);
		sb.append("\tnumNoPathResponses\t");
		sb.append(numNoPathResponses);	
		sb.append("\tnumStolenLambdasLSP\t");
		sb.append(numberStolenLambdasLSP);
		sb.append("\tnumberStolenBWLSP\t");
		sb.append(numberStolenBWLSP);
		sb.append("\tnumNoResponses\t");
		sb.append(numNoResponses);
		sb.append("\nSUMA:\t");		
		sb.append(numMLResponses+numSLResponses+numNoPathResponses+numberStolenLambdasLSP+
				numNoResponses+numIPResponses+numberStolenBWLSP);
		sb.append("\tTrafficHops:\t");
		sb.append(TrafficHops);
		sb.append("\tMedia TrafficHops:\t");
		sb.append((TrafficHops/(numMLResponses+numIPResponses))*100);
		sb.append("\tNumber WL:\t");
		sb.append(NumWL);
		sb.append("\nblockProbability\t");
		sb.append(blockProbability.result());	
		sb.append("\tblockProbability converge:\t");
		sb.append(blockProbability.getConverge());
		sb.append("\nStolenLambdaBlockProbability\t");
		sb.append(lambdaBlockingProbability.result());	
		sb.append("\tStolenLambdaBlockProbability convergence:\t");
		sb.append(lambdaBlockingProbability.getConverge());		
//		sb.append("\nmeanReqTime2\t");
//		sb.append(meanReqTime.result());
//		sb.append("\nmeanReqTime\t");
//		sb.append(meanReqTimePrev);
		sb.append("\nmeanReqTime\t");
		sb.append(meanReqTimeArray);
		sb.append("\tmeanReqTime converge:\t");
		sb.append(meanReqTime.getConverge());
		sb.append("\nLSPTime:\t");
		sb.append(LSPTime.result());
		sb.append("\ttheoricLSPTime ");
		sb.append(theoricLSPTime.result());
		sb.append("\nnumberActiveLSP ");		
		sb.append(numberActiveLSP);
		sb.append("\nbidirectional ");		
		sb.append(bidirectional);

		return sb.toString();
	}
	public String printEnd(){
		StringBuffer sb=new StringBuffer(1000);
		printMLResponses(sb);
		printSLResponses(sb);
		printIPResponses(sb);
		printNoPathResponses(sb);
		printStolenLambdasLSP(sb);
		printNoResponses(sb);
		printStolenBWLSP(sb);
		printBlockProbability(sb);
		printLambdaBlockingProbability(sb);
		printBlockProbabilityWithoutLambda(sb);
		printMeanReqTime(sb);
		
		sb.append("\nnumBloqueadas/numRequest\t");
		double bloqueo= numberStolenLambdasLSP+numNoPathResponses+numberStolenBWLSP;
		bloqueo=(double)(bloqueo/numRequests);
		sb.append(String.valueOf(bloqueo));
		sb.append("\nnumStolenBloqueadas/numRequest\t");
		bloqueo=(double)(numberStolenLambdasLSP/numRequests);
		sb.append(String.valueOf(bloqueo));
		return sb.toString();
	}
	private void printMLResponses(StringBuffer sb){
		sb.append("MLResponses: ");
		sb.append(load*(numberNodes-1)*numberNodes);
		sb.append(",");
		sb.append(numMLResponses+"\n");
	}
	private void printSLResponses(StringBuffer sb){
		sb.append("SLResponses: ");
		sb.append(load);
		sb.append(",");
		sb.append(numSLResponses+"\n");
	}
	private void printIPResponses(StringBuffer sb){
		sb.append("IPResponses: ");
		sb.append(load);
		sb.append(",");
		sb.append(numIPResponses+"\n");
	}
	private void printNoPathResponses(StringBuffer sb){
		sb.append("NoPathResponses: ");
		sb.append(load);
		sb.append(",");
		sb.append(numNoPathResponses+"\n");
	}
	private void printStolenLambdasLSP(StringBuffer sb){
		sb.append("StolenLambdasLSP: ");
		sb.append(load);
		sb.append(",");
		sb.append(numberStolenLambdasLSP+"\n");
	}
	private void printNoResponses(StringBuffer sb){
		sb.append("NoResponses: ");
		sb.append(load);
		sb.append(",");
		sb.append(numNoResponses+"\n");
	}
	private void printStolenBWLSP(StringBuffer sb){
		sb.append("NoResponses: ");
		sb.append(load);
		sb.append(",");
		sb.append(numNoResponses+"\n");
	}
	private void printBlockProbability(StringBuffer sb){
		sb.append("BlockProbability: ");
		sb.append(load);
		sb.append(",");
		sb.append(blockProbability.result()+"\n");
	}
	private void printBlockProbabilityWithoutLambda(StringBuffer sb){
		sb.append("BlockProbabilityWithoutLambda: ");
		sb.append(load);
		sb.append(",");
		sb.append(blockProbabilityWithoutStolenLambda.result()+"\n");
	}
	
	private void printLambdaBlockingProbability(StringBuffer sb){
		sb.append("LambdaBlockingProbability: ");
		sb.append(load);
		sb.append(",");
		sb.append(lambdaBlockingProbability.result()+"\n");
	}
	private void printMeanReqTime(StringBuffer sb){
		sb.append("MeanReqTime: ");
		sb.append(load);
		sb.append(",");
		sb.append(meanReqTime.result()+"\n");
	}


	


	
	

}
