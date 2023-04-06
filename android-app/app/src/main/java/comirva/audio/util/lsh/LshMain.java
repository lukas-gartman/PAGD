/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package comirva.audio.util.lsh;

import java.util.ArrayList;
import java.util.Map;
import java.util.Random;
import comirva.audio.util.lsh.test.TestData;

/**
 *
 * @author reini
 */
public class LshMain {
  
  /**
   * Daten hier statisch angenommen noch keine Daten eingelesen
   */
    
  public static void main (String[] args) {
    //amount of vectores must be counted;
    int n = 1000;
    int modulo = 200;
        
     // laut paper, länge des intervalls 
    int omega = 4;
     
     //wahrscheinlichkeit dass ein nn nicht reported wird
     double delta = 0.10;
     
     // value between 1 and 10 laut "LSH based on p-stable"
     // amonut of different hashfunctions
     int k = 4; 
     
     //here static, but as paramter for input or read 
     
     int dimension = 100;
     
     int border = 50;
     
     LSH  lsh = new LSH(0);
     lsh.initLSHValues(omega,k, dimension, delta, n);
    
     TestData testdata =  new TestData(dimension, 1, border, lsh.getDataStructure()[1],false);

   
     float[] search = new float[dimension];
     
     Random rand = new Random();
     
     for(int i=0 ; i< dimension; i ++ ){
       search[i] = (rand.nextFloat() - 0.5f) * border;
     }
    
 
     for(int i = 0; i < n; i ++){
      
       if(i% modulo == 0){
         // System.out.println("current index : " +i);
         lsh.add(testdata.createInnerTestData(search));
       }
       else  lsh.add(testdata.createOuterTestData(search));
     }
       
     Map nnResult = lsh.findNN(search, 1);
     lsh.setActualNeighbor(testdata.getActualNeighbor());
     lsh.showStat();
     
     ArrayList<float[]> resultsBF = testdata.findClosestBruteForce(search);
     testdata.showStat();
    
     
     testdata.compare(nnResult, resultsBF);
     
     
     
     System.out.println("prog over");
     
  }

   
}
