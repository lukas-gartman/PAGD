package comirva.audio.util;

import java.io.InputStream;
import java.io.IOException;
import java.io.FilterInputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * <p>berschrift: </p>
 *
 * <p>Beschreibung: </p>
 * This class provides a way to split metadata from audiostream, which support
 * the <i>Shoutcast Metadata Protocol</i>. The resulting stream equals the
 * InputStream handed in, except that all metadata infos have been removed.
 * Normally internet radio streams are encoded. Therefore these streams should
 * be passed on to the <i>Java Sound API</i>, which will try to decode the given
 * stream using the installed codecs.
 * <br>
 * <b>Attention:</b> Wrap this Stream with a <code>BufferedInputStream</code> to
 * yield a smooth decoding into a pcm audio stream.
 * <br>
 * To create an MetaDataInputStream directly based on an URL use the factory
 * method <code>createMetaDataInputStream()</code>. You might also wrap the
 * <code>BufferedInputStream</code> with a <code>ReducedAudioInputStream</code>
 * if you want to analyze this stream using the <code>comirva.audio</code>
 * package.
 *
 * @author Klaus Seyerlehner, Florian Jger
 * @version 1.0
 */
public class MetaDataInputStream extends FilterInputStream
{
    /* Some statistics... */
    public String currentMeta = "";
    public String lastMeta = "";
    public int zeroMetasLast = 0;
    public int zeroMetasCurrent = 0;

    /* meta data positions */
    int metaint = -1;  //number of bytes between the metadata messages
    int next = -1;  //number of bytes still to read up to the next metadata message

    /* stream identification */
    static int nextStreamID = 0;
    public int streamID = 0;
    public String name = "";

    int metapos = 0;


    private MetaDataInputStream(InputStream in) throws IOException
    {
            super(in);
            String head = "";

            // setting stream id info
            streamID = nextStreamID;
            nextStreamID++;
            name = "Stream " + streamID + "." + System.currentTimeMillis() + ".log";

            // get the metadata infos
            char[] fld = new char[2560];

            do
            {
                //read next message line
                for(int j = 0; j < fld.length; j++)
                {
                    fld[j] = (char)in.read();
                    if(fld[j] == '\n')
                    {
                        head = new String(fld,0,j-1);
                        break;
                    }
                }

                //print message text
                System.out.println(head);


                //check if this message contains the metadata interval
                if (head.startsWith("icy-metaint:"))
                {
                    String s = head.substring(12);
                    s = s.trim();
                    metaint = Integer.parseInt(s);
                }
                //check if this message contains the channel name
                else if (head.startsWith("icy-name:"))
                {
                    name = head.substring(9);
                }
            }
            while (head.length() > 2); //the last message is empty, so stop

            //compute the next position, where we will receive the next metadata message
            next = metaint;
    }

    public int read() throws IOException
    {
       if(metaint <= 100)
            return in.read();

        next --;

        if (next <= 0)
            processHeader();

        return in.read();
    }

    public int read(byte[] b) throws IOException
    {
       return this.read(b,0,b.length);
    }


    /**
     * This read method splits the metadata from the mp3 stream.
     * The metadata infos are sent every <code>metaint</code> bytes. The first
     * byte after <code>metaint</code> bytes of mp3 data indicates the length of
     * the metadata message.
     */
    public int read(byte[] b, int off, int len) throws IOException
    {
        //if the metadata interval is smaller the 100 bytes, metadata is not supported..
        if(metaint <= 100)
            return in.read(b,off,len);

        if (next < len)
        {
            //read up to the metadata position
            int part1 = in.read(b,off,next);

            if (part1 < 0)
                return part1;

            //bytes to receive up to the next metadata position (if everything works correctly "next" should be 0)
            next -= part1;
            if (next > 0)
                return part1;

            //extract the metadata from the stream
            processHeader();

            //read the rest of the stream into the buffer as requested
            //recursion, in case sb. tries to read a block greater than meta-data interval..
            int part2 = this.read(b,off + part1,len-part1);

            //well, in the case of an error / end => -1 return still the result of part1
            if (part2 < 0)
                part2 = 0;

            return part1 + part2;
        }


        int i = in.read(b, off, len);
        next -= i;
        return i;
    }

    public String readHeader() throws IOException
    {
            next = metaint;
            try
            {
                while (in.available() < 1) {
					Thread.sleep(100);
				}
            }
            catch(InterruptedException e){};
            int i = in.read();

            i *= 16;

            //String head;
            byte[] b = new byte[i];

            //wait till enough bytes are available
            try
            {
                while (in.available() < i)
                    Thread.sleep(100);
            }
            catch(InterruptedException e){};

            //read the thing...
            in.read(b);

            return new String(b);
    }

    public void processHeader() throws IOException
    {
            String s = readHeader();
            if (s.length()>0)
            {
                    //metachanged = true;
                    lastMeta = currentMeta;
                    metapos += metaint;
                    currentMeta = s;
                    System.out.println(s + "---"+metapos);
                    //if(!currentMeta.startsWith("StreamTitle"))
                    //    currentMeta = currentMeta;
                    zeroMetasLast = zeroMetasCurrent;
            }
            else
            {
                zeroMetasCurrent++;
            }
    }

    public static MetaDataInputStream createMetaDataInputStream(URL source) throws IOException
    {
        //URL source = new URL("http://64.236.34.106:80/stream/1040");
        URLConnection con = source.openConnection();

        //connnect to the url requesting meta data
        con.addRequestProperty("User-Agent", "AnAlien0.0"); //Ultravox server wants this
        con.addRequestProperty("icy-metadata", "1");
        con.connect();

        //create an metadata inputstream
        MetaDataInputStream meta = new MetaDataInputStream(con.getInputStream());

        return meta;
    }
}
