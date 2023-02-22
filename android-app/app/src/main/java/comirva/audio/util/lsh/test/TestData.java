/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package comirva.audio.util.lsh.test;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import comirva.audio.util.lsh.LSHEntry;

/**
 *
 * @author reini
 */
public class TestData {
  
  private int distance;
  private int dimension;
  private int border;
  private boolean debug;
  Hashtable space;
  Random rand;
  
  // statistic values
  private int comparison;
  private int neighbors;
  private long searchTime;
  private int actNeighbors;
  private int amount;
  
  
  public TestData(int dimension, int distance, int border, Hashtable table, boolean debug){
    this.dimension = dimension;
    this.distance = distance;
    this.border = border;
    this.space = table;
    this.rand = new Random();
    this.debug = debug;
    this.actNeighbors = 0;
  }
  
  
  
  public void compare(Map nnResult, ArrayList<float[]> resultsBF) {   
    int equal = 0;
    ArrayList<float[]> notFoundByNN = new ArrayList<float[]>();
    ArrayList<float[]> tooMuchInNN = new ArrayList<float[]>();
    Iterator brutIter = resultsBF.iterator();
    while(brutIter.hasNext()){
      float[] current = (float[]) brutIter.next();
      if(nnResult.containsKey(current)) equal++;
      else{
        notFoundByNN.add(current);
      }
    }   
    Iterator nnInter = nnResult.values().iterator();
    while(nnInter.hasNext()){
      float[] current = (float[]) nnInter.next();
      if(!resultsBF.contains(current)){
        tooMuchInNN.add(current);
      }
    }
    
    System.out.println("\n"+equal +" Elements found by both, "+notFoundByNN.size()+" elements not found by NN and "+tooMuchInNN.size()+" were found by NN but not BruteForce\n");
    
    if(notFoundByNN.size() != 0){
      Iterator iter = notFoundByNN.iterator();
      System.out.println("\n+++ Results not found by NN +++");
      while(iter.hasNext()){
        System.out.print("(");
        float[] tmp = (float[]) iter.next();
        for(int i = 0; i < tmp.length; i++){
          System.out.print(tmp[i]+" ");
        }
        System.out.println(")");
      }
      System.out.println("+++ End Results not found by NN +++\n");
    }
    
    if(tooMuchInNN.size() != 0){
      Iterator iter = tooMuchInNN.iterator();
      System.out.println("\n+++ Results not in Brute but found by NN +++");
      while(iter.hasNext()){
        System.out.print("(");
        float[] tmp = (float[]) iter.next();
        for(int i = 0; i < tmp.length; i++){
          System.out.print(tmp[i]+" ");
        }
        System.out.println(")");
      }
      System.out.println("+++ End Results not in Brute but found by NN +++\n");
    }
   
  }
  
  public float[] createOuterTestData(float[] queryPoint){
    float[] result = createVector();   
    while(calcDistance(queryPoint, result) < distance){
      result = createVector();
    }
    if(debug) printAdded(result,0);
    amount ++;
    return result;
  }
  
   public float[] createInnerTestData(float[] queryPoint){
     float[] randomVector =  new float[queryPoint.length];
     float sum = 0;
     for(int i = 0; i < randomVector.length; i++) {
       randomVector[i] = rand.nextFloat() % border;
       sum += randomVector[i];
     }
     for(int i = 0; i < randomVector.length; i++) {
       randomVector[i] = randomVector[i] / sum;
     }
     for(int i = 0; i < randomVector.length; i++) {
       randomVector[i] = queryPoint[i] + randomVector[i];
     }
     if(debug)printAdded(randomVector, 1);
     actNeighbors ++;
     amount ++;
     return randomVector;    
  }
  
   public int getActualNeighbor(){
     return this.actNeighbors;
   }
   
  private void printAdded(float[] vector, int location){
    if(location == 0) System.out.print("outside vector: (");
    else if(location == 1) System.out.print("inside vector: (");
    for(int i = 0; i < vector.length; i++){
      System.out.print(vector[i]+" ");
    }
    System.out.println(")");
  }
   
  private float[] createVector(){
    float[] result = new float[this.dimension];
    for(int i = 0; i < dimension; i ++){
       result[i] = (rand.nextFloat() - 0.5f ) * border;
    }
    
    return result;
  }
  
   public ArrayList<float[]> findClosestBruteForce(float[] vector){ 
     ArrayList result = new ArrayList();
     this.comparison = 0;
     long startTime = System.nanoTime();
     for (Enumeration<ArrayList> e = space.elements(); e.hasMoreElements();){
        Iterator<LSHEntry> iter = e.nextElement().iterator();
        while(iter.hasNext()){
          LSHEntry next = iter.next();
          this.comparison ++;
          if(calcDistance(vector, next.getVector()) <= this.distance){
            neighbors ++;
            result.add(next.getVector());
          }
        }
     }
     this.searchTime = System.nanoTime() - startTime;
     return result;
  }
   
  public void showStat(){
    System.out.println("\n+++ Statistic for Brute Force +++");
    System.out.println(getRuntim());
    System.out.println("Amount of Vektors: "+ this.amount);
    System.out.println("Amount of comparison: "+this.comparison);
    System.out.println("Amount of Neibhbors found: "+this.neighbors);
    System.out.println("Amount of Actual Neighbors: " +this.actNeighbors);
    System.out.println("Percentage of found neigbors: " + ((double)this.neighbors / (double)this.actNeighbors) *100 +"%");
    System.out.println("+++ End Statistic for Brute Force +++\n");
  } 
  
 public String getRuntim(){
    long nanoSec = this.searchTime % 1000;
    long microSec = this.searchTime / 1000;
    long milliSec = microSec / 1000;
    long sec = milliSec / 1000;
    long min = sec / 60;
    return "Runtime: "+(min)%60+"min: "+(sec) % 60+"s: "+(milliSec)%1000+"ms: "+(microSec)%1000+"mcs: "+nanoSec+"ns";
  }
  
  @Override
  public String toString() {
    String returnVal = "TestData: \n";
    for (Enumeration<ArrayList> e = space.elements(); e.hasMoreElements();) {
      ArrayList<LSHEntry> tmp = e.nextElement();
      Iterator<LSHEntry> iter = tmp.iterator();
      while (iter.hasNext()) {
        float[] toPrint = iter.next().getVector();
        returnVal += "(";
        for (int i = 0; i < toPrint.length; i++) {
          returnVal += toPrint[i] + " ";
        }
        returnVal += ") \n";
      }
    }
    return returnVal;
  }

  private double calcDistance(float[] vector, float[] anotherVector) {
    int tmp = 0;
    for(int i = 0; i < vector.length; i++){
      tmp += Math.pow((vector[i] - anotherVector[i]),2);
    }
    return Math.sqrt(tmp);
  }

}
