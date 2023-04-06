/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package comirva.audio.util.lsh;

import java.util.Random;

/**
 *
 * @author reini
 */
public class LSHInitEntry {
  private double[] a;
  private int b;

  public LSHInitEntry(int dimension, int omega) {
    a = new double[dimension];
    Random rand = new Random();
    for (int i = 0; i < dimension; i++) {
      a[i] = rand.nextGaussian();
    }
    this.b = rand.nextInt(omega + 1);
  }

  public double[] getA() {
    return this.a;
  }

  public int getB() {
    return this.b;
  }

  @Override
  public String toString() {
    String output = "(";
    for (int i = 0; i < a.length; i++) {
      output += a[i] + " ";
    }
    output += ")";
    return "[ "+this.b +" - " + output +"]";
  }
}

