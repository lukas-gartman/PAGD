/*
 * author: Reinhold Taucher (0455386)
 */
package comirva.audio.util.lsh;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;


public class LSH {

  // LSH Parameter
  private int k;
  private int L;
  private int omega;
  
  private int amount;
  private int dimension;

  private long prime = 4294967291L;
  
  //LSH Table Paramter
  private int tableSize;
  private Hashtable<Integer, ArrayList>[] hastTable_container;
  private int[] randomNumbersh1;
  private int[] randomNumbersh2;
  private LSHInitEntry[][] buckets_init_values;
  
  //statistical parameters
  private int comparison; //amount of comparsion of vectors to be done by NN
  private long searchTime;  //in nanoseconds
  private long buildTime;
  private int foundNeighbors;
  private int actualNeighbors;
  
 
  private int debugLevel = 0;

  public LSH(int debugLevel) {
    this.debugLevel = debugLevel;
  }
  
  public LSH (){
    this.debugLevel = 0;
  }
  
  public void initLSHValues(int omega, int k, int dimension, double delta, int amount){
     this.buildTime = 0;
     if(debugLevel == 1) System.out.println("Starting init LSH");
     long beginInit = System.nanoTime();
     this.omega = omega;
     this.k = k;
     if(delta == 0) delta = 0.90;
     this.L = calcL(delta);
     this.amount = amount;
     this.dimension = dimension;
     init_bucket_values(omega);
     this.tableSize = amount;
     hastTable_container = new Hashtable[L];
     init_Hashtables();
     randomNumbersh1 = new int[k];
     randomNumbersh2 = new int[k];
     initrandomNumbers();
     long endInit = System.nanoTime();
     buildTime += (endInit - beginInit);
     if(debugLevel == 1) {
         System.out.println("Ending init LSH");
         showInitValues(1);
     }
     if(debugLevel == 2){
       showInitValues(1);
       showHashVectors();
       showRandomNumbers();
     }
  }
  
  public void add(float[] vector) {
    long beginAdd = System.nanoTime();
    // get a vector and calculate all Hs
    for (int hashTable = 0; hashTable < L; hashTable++) {
      int[] hashResultsOfTable = calcHashFunctions(vector, hashTable); 
      int currentPos = calcTablePosition(hashResultsOfTable);
      if(hastTable_container[hashTable].containsKey(currentPos)){
        hastTable_container[hashTable].get(currentPos).add(new LSHEntry(calcVectorFingerprint(hashResultsOfTable),vector));
      }
      else {
        ArrayList<LSHEntry> list = new ArrayList<LSHEntry>();
        list.add(new LSHEntry(calcVectorFingerprint(hashResultsOfTable), vector));
        hastTable_container[hashTable].put(currentPos,list);
      }
    }
    long endAdd = System.nanoTime();
    buildTime += (endAdd - beginAdd);
  }
  
  public boolean delete(float[] vector) {
    for( int hashTable = 0; hashTable < L; hashTable ++){
      int[] hashResultsOfTable = calcHashFunctions(vector, hashTable); 
      int currentPos = calcTablePosition(hashResultsOfTable);
      if(hastTable_container[hashTable].containsKey(currentPos)){
        ArrayList<LSHEntry> tmp = hastTable_container[hashTable].get(currentPos);
        long h2 = calcVectorFingerprint(hashResultsOfTable);
        for(Iterator<LSHEntry> iter = tmp.iterator(); iter.hasNext();){
          LSHEntry lshEntry = iter.next();
          if( h2 == lshEntry.getIdentifier()) {
            tmp.remove(lshEntry);
          }
        }
      }
      else return false;
    }
    return true;
  }
  
  /*
   * this method finds all near foundNeighbors within a given radius and a querypoint
   */
  
  public Map findNN(float[] vector, int cR) {
    if(debugLevel == 1){
         System.out.println("Starting NN Search");
         System.out.print("Querypoint: (");
         for(int i =0; i < vector.length; i ++){
           System.out.print(vector[i]+" ");
         }
         System.out.println(")\n");
    }

    if(debugLevel == 2) showHashTables();
    Map results = new HashMap();
    this.comparison = 0; //counts the amount of comparisons while searching
    long startTime = System.nanoTime();
    for(int hashTable = 0; hashTable < L; hashTable ++){
      int currentPos = calcTablePosition(calcHashFunctions(vector, hashTable));
      if(hastTable_container[hashTable].containsKey(currentPos)){
        ArrayList<LSHEntry> tmp = hastTable_container[hashTable].get(currentPos);
        Iterator<LSHEntry> iter = tmp.iterator();
        while(iter.hasNext()){
          LSHEntry next = iter.next();
          if(!results.containsKey(next.getVector())){
            this.comparison ++;
            if(calcDistance(vector, next.getVector()) <= cR ) results.put(next.getVector(),next.getVector());
          }
        }
      }  
    }
    this.searchTime = System.nanoTime() - startTime;
    if(debugLevel != 0) showResult(results);
    foundNeighbors = results.size();
    if(debugLevel == 1) System.out.println("Ended Search for NN");
    return results;
  }
  
  /*
   * statistic output
   */
  
  public void showStat() {
    System.out.println("\n+++ Statistic for NN +++");
    showInitValues(0);
    System.out.println("BuildTime: "+getRuntim(this.buildTime));
    System.out.println("Runtime: "+getRuntim(this.searchTime));
    System.out.println("Amount of comparison: "+this.comparison);
    System.out.println("Amount of Neibhbors found: "+this.foundNeighbors);
    System.out.println("Amount of actual Neighbors: " +this.actualNeighbors);
    System.out.println("Percentage of found neighbors: "+ ((double) this.foundNeighbors / (double)this.actualNeighbors) * 100 +"%");
    System.out.println("+++ End Statistic for NN +++ \n");
  }
  
  public int getComparison() {
    return this.comparison;
  }
  
  public String getRuntim(long time){
    long nanoSec = time % 1000;
    long microSec = time / 1000;
    long milliSec = microSec / 1000;
    long sec = milliSec / 1000;
    long min = sec / 60;
    return (min)%60+"min: "+(sec) % 60+"s: "+(milliSec)%1000+"ms: "+(microSec)%1000+"mcs: "+nanoSec+"ns";
  }
  
  /*
   * this method is just for other classes to use all vectors without beeing stored twice
   */
  
  public Hashtable<Integer, ArrayList>[] getDataStructure(){
    return this.hastTable_container;
  }
  
  public void setActualNeighbor(int neighbors) {
    this.actualNeighbors = neighbors;
  }
  
  public int getDebugLevel(){
    return this.debugLevel;
  }
  
  /*
   * Begin of the private Part 
   */
  
  private double calcDistance(float[] queryPoint, float[] anyPoint ){
    int tmp = 0;
    for(int i = 0; i < dimension; i++){
      tmp += Math.pow((queryPoint[i] - anyPoint[i]),2);
    }
    return Math.sqrt(tmp);
  }  

  private int[] calcHashFunctions(float[] vector, int hashTable) {
    // returns an array with the results of all Hashfunctions of a Hashtable
    int[] result = new int[k];
    for (int i = 0; i < k; i++) {
      // calculate dotProduct for vektors.
      int dotproduct = 0;
      for (int j = 0; j < dimension; j++) {
        dotproduct +=  buckets_init_values[hashTable][i].getA()[j] * vector[j];
      }
      result[i] = (int) Math.floor((dotproduct + buckets_init_values[hashTable][i].getB()) / omega);
    }  
    return result;
  }

  private int calcL(double delta) {
    // calculate the amount of Hastables in which buckets are stored
    // the value is rounded up to integer    
    int c = 1;
    double p1 = (StatUtil.erf(omega / Math.sqrt(2)*c) - (c * (1 - Math.pow(Math.E,( - (omega * omega)/ (2 * (c * c))))) * (Math.sqrt(2 / Math.PI)/omega)));
    return (int) Math.ceil((Math.log10(delta))/ (Math.log10(1 - Math.pow(p1, k))));
  }

  private int calcTablePosition(int[] hashResults) {
    //calculate the position in the hashtable h1  
    int h1 = 0;
    //calculate the position in hashtable
     for(int i = 0; i < k; i ++){
      h1 += (hashResults[i] * randomNumbersh1[i] );
    }
    return (int)(( h1 % prime ) % tableSize);
  }
  
   private long calcVectorFingerprint(int[] hashResults) {
    long h2= 0;
    for(int i = 0; i < k; i++){
      h2 += (hashResults[i] * randomNumbersh2[i]);
    }
    return h2 % prime;
  }

  private void init_Hashtables() {
    //creates L Hashtables, which are managed by an array.
    for (int i = 0; i < L; i++) {
      hastTable_container[i] = new Hashtable<Integer, ArrayList>(tableSize);
    }
  }

  private void init_bucket_values(int omega) {
    /*
     *calculation of the hasfunction value to calculate buckets.
     * lengh is represented by the amount of hashfunktions (k)
     * and the height is the amount of hashtables (L)
     * for each hashtable are k different a vektors needed.
    */
     this.buckets_init_values = new LSHInitEntry[L][k];

    for (int i = 0; i < L; i++) {
      for (int j = 0; j < k; j++) {
        // store vectore a and value b to a Array with Bucket_Values       
        buckets_init_values[i][j] = new LSHInitEntry(dimension, omega);
      }
    }
  }

  private void initrandomNumbers() {
    // init random numbers for position calculation in hashtable.
    // init random numbers for bucket identification.
    Random rand = new Random();
    for (int i = 0; i < k; i++){
      randomNumbersh1[i] = rand.nextInt();
      randomNumbersh2[i] = rand.nextInt();
    }
  }

  /*
   * the following methods server just for output.
   * see the different levels above
   */
  
   private void showInitValues(int beginning) {
    if(beginning == 1) System.out.println("\n+++ Basic Values +++");
    System.out.println("Debug level: "+this.debugLevel);
    System.out.println("Amount of vectors (n): "+this.amount);
    System.out.println("Amount of dimension (d): "+this.dimension);
    System.out.println("Amount of Hashtables (L): "+this.L);
    System.out.println("Amount of Hashfunktions per Table (k): "+this.k);
    System.out.println("Size of Hashtable (s): "+this.tableSize);
    System.out.println("Primzahl für H1 und H1 (prime): "+this.prime);
    if(beginning == 1) System.out.println("+++ End Basic Values +++\n");
  }
   
  private void showResult(Map results){
    System.out.println("\n+++ NN Results Found +++");
     Collection list = results.values();
     Iterator iter = list.iterator();
     while(iter.hasNext()){
       float[] resultVector = (float[]) iter.next();
       System.out.print("(");
       for(int i = 0; i < resultVector.length; i++){
         System.out.print(resultVector[i]+" ");
       }
       System.out.println(")");
     }
     System.out.println("+++ END of NN Result List +++ \n");
  }

  public void showHashTables() {
    System.out.println("\n+++ Hashtable Output +++");    
    for(int i = 0; i < this.L; i ++){
      Hashtable table = hastTable_container[i];
      System.out.println("\n Values of Hashtable "+i+":");
      for(Enumeration<Integer> keyEnum = table.keys(); keyEnum.hasMoreElements();){
        int key = keyEnum.nextElement();
        ArrayList<LSHEntry> bucketList = (ArrayList<LSHEntry>) table.get(key);
        System.out.println(" Bucket: "+key+" with "+ bucketList.size()+" elements");
        for(Iterator<LSHEntry> iter = bucketList.iterator(); iter.hasNext();){
          System.out.println("  "+iter.next());
        }
      }
    }    
    System.out.println("\n+++ End Hashtable Output +++ \n");
  }

  private void showHashVectors() {
    System.out.println("+++Vectors for Hashfunction Calculation+++");
    for(int i = 0; i < buckets_init_values.length; i++){
      System.out.println("Initial Values for Hashtable: "+i);
      for(int j = 0; j < buckets_init_values[j].length; j++){
        System.out.println("  "+buckets_init_values[i][j]);
      }
    }
    System.out.println("\n+++ End of Hashfunction Calculation Vectors +++\n");
  }
  
  private void showRandomNumbers() {
    System.out.println("\n+++ Random Numbers H1 and H2 +++");
    System.out.println("Values for H1:");
    for(int i = 0; i < randomNumbersh1.length; i++){
      if(i % 3 == 0) System.out.println(" "+randomNumbersh1[i]);
      else System.out.print(" "+randomNumbersh1[i]);
    }
    
    System.out.println("Values for H2:");
    for(int i = 0; i < randomNumbersh1.length; i++){
      if(i % 4 == 0) System.out.println(" "+randomNumbersh2[i]);
      else System.out.print(" "+randomNumbersh2[i]);
    }
    System.out.println("\n+++ End Random Numbers H1 and H2 +++\n");
  }
}
