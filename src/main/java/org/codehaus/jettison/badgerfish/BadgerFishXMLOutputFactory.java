package org.codehaus.jettison.badgerfish;

import java.io.Writer;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.codehaus.jettison.AbstractXMLOutputFactory;

public class BadgerFishXMLOutputFactory extends AbstractXMLOutputFactory {
   public XMLStreamWriter createXMLStreamWriter(Writer writer) throws XMLStreamException {
      return new BadgerFishXMLStreamWriter(writer);
   }
}
