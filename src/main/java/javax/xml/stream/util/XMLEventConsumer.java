package javax.xml.stream.util;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

public interface XMLEventConsumer {
   void add(XMLEvent var1) throws XMLStreamException;
}
