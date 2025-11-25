package javax.xml.stream.util;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;

public interface XMLEventAllocator {
   XMLEventAllocator newInstance();

   XMLEvent allocate(XMLStreamReader var1) throws XMLStreamException;

   void allocate(XMLStreamReader var1, XMLEventConsumer var2) throws XMLStreamException;
}
