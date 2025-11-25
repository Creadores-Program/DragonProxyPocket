package javax.xml.stream;

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.util.XMLEventConsumer;

public interface XMLEventWriter extends XMLEventConsumer {
   void flush() throws XMLStreamException;

   void close() throws XMLStreamException;

   void add(XMLEvent var1) throws XMLStreamException;

   void add(XMLEventReader var1) throws XMLStreamException;

   String getPrefix(String var1) throws XMLStreamException;

   void setPrefix(String var1, String var2) throws XMLStreamException;

   void setDefaultNamespace(String var1) throws XMLStreamException;

   void setNamespaceContext(NamespaceContext var1) throws XMLStreamException;

   NamespaceContext getNamespaceContext();
}
