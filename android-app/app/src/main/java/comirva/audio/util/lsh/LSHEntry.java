/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package comirva.audio.util.lsh;

/**
 *
 * @author reini
 */
public class LSHEntry {
  
  private long identifier;
  private float[] vector;
  
  public LSHEntry(long ident, float[] vect){
    this.identifier = ident;
    this.vector = vect;
  }
  
  public long getIdentifier() {
    return this.identifier;
  }
  
  public float[] getVector() {
    return this.vector;
  }
  
  @Override
  public String toString(){
    String tmp = "(";
    for(int i = 0; i < vector.length; i++){
      tmp += vector[i]+" ";
    }
    tmp += ")";
    return tmp;
  }

}
