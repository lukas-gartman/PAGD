package comirva.audio.util.kmeans;

/**
 * <b>Invalid Clustering Exception</b>
 *
 * <p>Description: </p>
 * This exception indicates, that no valid result for the called method exists,
 * because the clustering is not valid. In most cases this means, that there is
 * no clustering of the data up to now.
 *
 * @author Klaus Seyerlehner
 * @version 1.0
 */
public class InvalidClusteringException extends RuntimeException
{

  /**
	 * 
	 */
	private static final long serialVersionUID = 1L;


/**
   * Creates a InvalidClustering Exception.
   */
  public InvalidClusteringException()
  {
    super();
  }


  /**
   * Creates a InvalidClustering Exception with a specific message.
   *
   * @param msg String the message
   */
  public InvalidClusteringException(String msg)
  {
    super(msg);
  }


}
