package javax.xml.stream;

import java.util.Iterator;
import javax.xml.stream.events.XMLEvent;

public interface XMLEventReader extends Iterator {
   XMLEvent nextEvent() throws XMLStreamException;

   boolean hasNext();

   XMLEvent peek() throws XMLStreamException;

   String getElementText() throws XMLStreamException;

   XMLEvent nextTag() throws XMLStreamException;

   Object getProperty(String var1) throws IllegalArgumentException;

   void close() throws XMLStreamException;
}
