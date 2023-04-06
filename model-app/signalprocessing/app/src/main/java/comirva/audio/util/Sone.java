package comirva.audio.util;

import java.io.IOException;
import java.util.Vector;


/**
 * <b>Specific Loudness Sensation - Sone</b>
 *
 * <p>Description: </p>
 * Computes sonogram from a pcm signal. A sonogram of an audio segment consists
 * of the specific loudness sensation (Sone) per critical-band (bark) in short
 * time intervals[1]. One object supports only one sample rate and a given
 * window size.<br>
 *<br>
 * [1] Rauber, Pampalk, Merkl "Using Psycho-Acoustic Models and Self-Organizing
 *     Maps to Create a Hierarchical Structuring of Music by Sound Similarity",
 *     in Proceedings of ISMIR, 2002.<br>
 *<br>
 * [2] Schroeder, Atal, Hall "Optimizing digital speech coders by exploiting
 *      masking properties of the human ear", JASA, 1979.<br>
 *<br>
 * [3] Terhardt "Calculation virtual pitch", Hearing Research, 1979.<br>
 *<br>
 * [4] Zwicker, Fastl "Psychoacoustics, Facts and Models", Springer, 2nd edition.<br>
 *<br>
 * [5] Bladon, Lindblom "Modeling the judgment of vowel quality differences",
 *     JASA, 1981.<br>
 *<br>
 * [6] Pampalk, Dixon, Widmer "Exploring Music Collections by Browsing
 *     Different Views", Computer Music Journal, Vol. 28, Issue 2, 2004.<br>
 *
 * @author Klaus Seyerlehner
 * @version 1.2
 */

public class Sone
{
  //fields
  protected int windowSize;
  protected int hopSize;
  protected float sampleRate;
  protected double baseFreq;
  protected FFT normalizedPowerFFT;

  //implementation: constant matrices, vectors and values
  private double[] terhardtWeight;
  //private double[][] spreadMatrix;
  private int bark_upper[];

  //implementation: buffers
  private double[] inputData;
  private double[] buffer;
  private double scale;


  /**
   * Creates a Sone object with default window size of 256 for the given sample
   * rate. The overleap of the windows is fixed at 50 percent.
   *
   * @param sampleRate float samples per second, must be greater than zero; not
   *                         whole-numbered values get rounded
   * @throws IllegalArgumentException raised if method contract is violated
   */
  public Sone(float sampleRate) throws IllegalArgumentException
  {
    //initialize
    this(256, sampleRate);
  }


  /**
   * Ceates a Sone object with given window size and sample rate. The overleap
   * of the windows is fixed at 50 percent. The window size must be 2^n and at
   * least 32. The sample rate must be at least 1.
   *
   * @param windowSize int size of a window
   * @param sampleRate float samples per second, must be greater than zero; not
   *                         whole-numbered values get rounded
   * @throws IllegalArgumentException raised if method contract is violated
   */
  public Sone(int windowSize, float sampleRate) throws IllegalArgumentException
  {
    //check for correct window size
    if(windowSize < 32)
    {
        throw new IllegalArgumentException("window size must be at least 32");
    }
    else
    {
        int i = 32;
        while(i < windowSize && i < Integer.MAX_VALUE)
          i = i << 1;

        if(i != windowSize)
            throw new IllegalArgumentException("window size must be 2^n");
    }

    //check sample rate
    sampleRate = Math.round(sampleRate);
    if(sampleRate < 1)
      throw new IllegalArgumentException("sample rate must be at least 1");

    //initialize fields
    this.windowSize = windowSize;
    this.hopSize = windowSize/2; //50% Overleap
    this.sampleRate = sampleRate;
    this.baseFreq = sampleRate/windowSize;

    //create buffers
    inputData = new double[windowSize];
    buffer = new double[windowSize];

    //get upper boundaries for the used bark bands
    bark_upper = getBarkUpperBoundaries(sampleRate);

    //create normalized power fft object
    normalizedPowerFFT = new FFT(FFT.FFT_NORMALIZED_POWER, windowSize, FFT.WND_HANNING);

     //get weights for simulation of the perception of the outer ear
    terhardtWeight = getTerhardtWeights(baseFreq, windowSize);

    //create spread matrix for spectral masking
    //spreadMatrix = getSpreadMatrix(bark_upper.length);
    
    //compute rescale factor to rescale and normalize at once (default is 96dB = 2^16)
    this.scale = (Math.pow(10, 96 / 20));    
  }


  /**
   * Returns the number of samples skipped between two windows.
   * Since the overleap of 50 percent is fixed, the hop size is half the window
   * size.
   *
   * @return int hop size
   */
  public int getHopSize()
  {
    return hopSize;
  }

  /**
   * Performs the transformation of the input data to Sone.
   * This is done by splitting the given data into windows and processing
   * each of these windows with processWindow().
   *
   * @param input double[] input data is an array of samples, must be a multiple
   *                       of the hop size, must not be a null value
   * @return double[][] an array of arrays contains a double array of Sone value
   *                    for each window
   * @throws IOException if there are any problems regarding the inputstream
   * @throws IllegalArgumentException raised if method contract is violated
   */
  public double[][] process(double[] input) throws IllegalArgumentException, IOException
  {
    //check for null
    if(input == null)
      throw new IllegalArgumentException("input data must not be a null value");

    //check for correct array length
    if((input.length % hopSize) != 0)
        throw new IllegalArgumentException("Input data must be multiple of hop size (windowSize/2).");

    //create return array with appropriate size
    double[][] sone = new double[(input.length/hopSize)-1][bark_upper.length];

    //process each window of this audio segment
    for(int i = 0, pos = 0; pos < input.length - hopSize; i++, pos+=hopSize)
      sone[i] = processWindow(input, pos);

    return sone;
  }


  /**
   * Transforms one window of samples to Sone. The following steps are
   * performed: <br>
   * <br>
   * (1) normalized power fft with hanning window function<br>
   * <br>
   * (2) compute influence of the outer ear by emphasizing some frequencies
   *     (model by Terhardt[3])<br>
   * <br>
   * (3) Conversion to bark scale to reduce the data to the critical bands of
   *     human hearing[4].<br>
   * <br>
   * (4) calculate the influence of spectral masking effects, since the human
   *     hear needs some regeneration time and can't perceive similar short
   *     delayed tones[2]. Also conversion to db is done in this step<br>
   * <br>
   * (5) Finally the db values are converted to loudness values (Sone, a
   *     psychoacoustic scale). This loudness scale better represent the human
   *     perception of loudness than the db scale does[5].
   * <br>
   *
   * @param window double[] data to be converted,  must contain enough data for
   *                        one window
   * @param start int start index of the window data
   * @return double[] the window representation in Sone
   * @throws IllegalArgumentException raised if method contract is violated
   */
  public double[] processWindow(double[] window, int start) throws IllegalArgumentException
  {
    double value;
    int fftSize = (windowSize / 2) + 1;
    int barkSize = bark_upper.length;
    double[] output = new double[barkSize];

    //check start
    if(start < 0)
      throw new IllegalArgumentException("start must be a positve value");

    //check window size
    if(window == null || window.length - start < windowSize)
      throw new IllegalArgumentException("the given data array must not be a null value and must contain data for one window");

    //just copy to buffer and rescaled the input samples according to the original matlab implementation to 96dB
    for (int j = 0; j < windowSize; j++)
      buffer[j] = window[j + start] * scale;

    //perform power fft
    normalizedPowerFFT.transform(buffer, null);

    //calculate outer era model
    for (int i = 0; i < fftSize; i++)
      buffer[i] = buffer[i] * terhardtWeight[i];

    //calculate bark scale
    double freq = 0;
    value = 0;
    int band = 0;
    for (int i = 0; i < fftSize && band < bark_upper.length; i++)
    {
      if (freq <= bark_upper[band])
      {
        value += buffer[i];
      }
      else
      {
        buffer[band] = value;
        band++;
        value = buffer[i];
      }
      freq += baseFreq;
    }

    if(band < bark_upper.length)
        buffer[band] = value;

    //calculate spectral masking (in db)
    double log = 10 * (1 / Math.log(10)); // log for base 10 and scale by factor 10
    //matrix vector multiplication
    /*for (int j = 0; j < barkSize; j++)
    {
      value = 0;
      for (int i = 0; i < barkSize; i++)
      {
        value += spreadMatrix[j][i] * buffer[i];
      }

      if(value < 1)
          value = 1;

      output[j] = log * Math.log(value);
    }*/

    //calculate loudness Sone (from db)
    for (int i = 0; i < barkSize; i++)
    {
      //corrected version: without spectral masking step
      if(buffer[i] < 1)
          output[i] = 0.0d;
      else
        output[i] = log * Math.log(buffer[i]);

      if (output[i] >= 40d)
        output[i] = Math.pow(2d, (output[i] - 40d) / 10d);
      else
        output[i] = Math.pow(output[i] / 40d, 2.642d);
    }

    return output;
  }


  /**
   * Creates a weight vector according to the outer ear formula of Terhardt.
   * The k-th component of the weight vector is the weight for the frequency
   * k*baseFrequency. For details take a look at [3].
   *
   * @param baseFrequency The base frequency (Hz) the weights are based on. The
   *                      frequency of the first component of the weight vector.
   *                      The base frequency must be a positive value.
   * @param vectorSize dimension of the vector to compute, must be a positive
   *                   value or zero
   * @return a vector with weights for multiples of the base frequency.
   * @throws IllegalArgumentException raised if method contract is violated
   */
  public double[] getTerhardtWeights(double baseFrequency, int vectorSize) throws IllegalArgumentException
  {
    double freq = 0;

    //check baseFrequency
    if(baseFrequency <= 0)
      throw new IllegalArgumentException("the base frequency must be greater than zero");

    //check vectorSize
    if(vectorSize < 0)
      throw new IllegalArgumentException("the vectorSize must be greater or equa to zero");

    double weights[] = new double[vectorSize];

    for (int j = 0; j < vectorSize; j++)
    {
      //compute frequency of the j-th component
      freq = (j * baseFrequency) / 1000;
      //compute weight using Terhard formula
      weights[j] = Math.pow(10, ( -3.64 * Math.pow(freq, -0.8) + 6.5 * Math.exp( -0.6 * Math.pow(freq - 3.3, 2)) - 0.001 * (Math.pow(freq, 4))) / 20);
      //take the power of the computed weight
      weights[j] = weights[j] * weights[j];
    }
    return weights;
  }


  /**
   * Creates a matrix for computation of spectral masking effects for the used
   * bark bands. Masking effects can be calculated by matrix multiplication. For
   * details take a look at [2].
   *
   * @param barkSize the number of bark bands in use for the calculation of the
   *                 masking effects, must be a positive value
   * @return quadratic matrix with dimensions according to the given number of
   *         bark bands
   * @throws IllegalArgumentException raised if method contract is violated
   */
  public double[][] getSpreadMatrix(int barkSize) throws IllegalArgumentException
  {
    //check barkSize
    if(barkSize < 0)
      throw new IllegalArgumentException("the bark size must be a positve value");

    double[][] matrix = new double[barkSize][barkSize];

    for (int i = 0; i < barkSize; i++)
    {
      for (int j = 0; j < barkSize; j++)
      {
        int z = j - i;
        matrix[j][i] = Math.pow(10d, (15.81d + 7.5d * (z + 0.474d) - 17.5d * Math.pow(1d + Math.pow(z + 0.474d, 2d), 0.5d)) / 10d);
      }
    }

    return matrix;
  }


  /**
   * Returns an array with the upper boundaries of the bark bands. Only bark
   * bands with a lower frequency than the sampling frequency are considered.
   * For details take a look at [4].
   *
   * @param sampleRate sample rate (Hz), must be a positive value
   * @return an array containing the upper boundaries of the bark bands, the
   *    number of bark bands to consider defines the length of the array
   * @throws IllegalArgumentException raised if method contract is violated
   */
  public int[] getBarkUpperBoundaries(double sampleRate) throws IllegalArgumentException
  {
    int bark_upper[] = {100, 200, 300, 400, 510, 630, 770, 920, 1080, 1270, 1480, 1720, 2000, 2320, 2700, 3150, 3700, 4400, 5300, 6400, 7700, 9500, 12000, 15500}; // Hz
    int max = 0;
    int boundaries[];

    //check sampleRate
    if(sampleRate <= 0.0f)
      throw new IllegalArgumentException("the sample rate must be a positive value");


    // ignore critical bands higher than the sampling frequency
    for (max = bark_upper.length-1; max >= 0 && bark_upper[max] > sampleRate/2; max--);

    //create new array of appropriate size
    boundaries = new int[max + 2];

    //copy upper boundaries
    for (int i = 0; i < boundaries.length; i++)
      boundaries[i] = bark_upper[i];

    return boundaries;
  }

}
