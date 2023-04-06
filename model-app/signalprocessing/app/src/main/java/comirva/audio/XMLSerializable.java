package comirva.audio;

import java.io.IOException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.XMLStreamException;


/**
 * <b>XML Serialization</b>
 *
 * <p>Description: </p>
 * Allows object xml serialization. To make an object implementing this
 * interface persistent, it is enough to simply call <code>writeXML()</code> with
 * a valid <code>XMLStreamWriter</code>. To load an object again it suffices to
 * create an empty object of this type and call <code>readXML()</code>.<br>
 * <br>
 * It is recommended to set protected access to the empty object constructor and
 * provide a static <code>read()</code> method, such that one can only construct
 * vaild objects.<br>
 * <br>
 * <b>Please note</b>, that there is no mechanism to detect cycles during the
 * serialization process. Due to the recursive design of this interface deep
 * object structures might raise a <code>java.lang.StackOverflowError</code>.
 *
 * @author Klaus Seyerlehner
 * @version 1.0
 */
public interface XMLSerializable
{
    /**
     * Writes the xml representation of this object to the xml output stream.<br>
     * <br>
     * There is the convention, that each call to a <code>writeXML()</code> method
     * results in one xml element in the output stream.
     *
     * @param writer XMLStreamWriter the xml output stream
     *
     * @throws IOException raised, if there are any io troubles
     * @throws XMLStreamException raised, if there are any parsing errors
     */
    public void writeXML(XMLStreamWriter writer) throws IOException, XMLStreamException;


    /**
     * Reads the xml representation of an object form the xml input stream.<br>
     * <br>
     * There is the convention, that <code>readXML()</code> starts parsing by
     * checking the start tag of this object and finishes parsing by checking the
     * end tag. The caller has to ensure, that at method entry the current token
     * is the start tag. After the method call it's the callers responsibility to
     * move from the end tag to the next token.
     *
     * @param parser XMLStreamReader the xml input stream
     *
     * @throws IOException raised, if there are any io troubles
     * @throws XMLStreamException raised, if there are any parsing errors
     */
    public void readXML(XMLStreamReader parser) throws IOException, XMLStreamException;
}
