package javax.xml.stream;

import javax.xml.namespace.NamespaceContext;

public interface XMLStreamWriter {
   void writeStartElement(String var1) throws XMLStreamException;

   void writeStartElement(String var1, String var2) throws XMLStreamException;

   void writeStartElement(String var1, String var2, String var3) throws XMLStreamException;

   void writeEmptyElement(String var1, String var2) throws XMLStreamException;

   void writeEmptyElement(String var1, String var2, String var3) throws XMLStreamException;

   void writeEmptyElement(String var1) throws XMLStreamException;

   void writeEndElement() throws XMLStreamException;

   void writeEndDocument() throws XMLStreamException;

   void close() throws XMLStreamException;

   void flush() throws XMLStreamException;

   void writeAttribute(String var1, String var2) throws XMLStreamException;

   void writeAttribute(String var1, String var2, String var3, String var4) throws XMLStreamException;

   void writeAttribute(String var1, String var2, String var3) throws XMLStreamException;

   void writeNamespace(String var1, String var2) throws XMLStreamException;

   void writeDefaultNamespace(String var1) throws XMLStreamException;

   void writeComment(String var1) throws XMLStreamException;

   void writeProcessingInstruction(String var1) throws XMLStreamException;

   void writeProcessingInstruction(String var1, String var2) throws XMLStreamException;

   void writeCData(String var1) throws XMLStreamException;

   void writeDTD(String var1) throws XMLStreamException;

   void writeEntityRef(String var1) throws XMLStreamException;

   void writeStartDocument() throws XMLStreamException;

   void writeStartDocument(String var1) throws XMLStreamException;

   void writeStartDocument(String var1, String var2) throws XMLStreamException;

   void writeCharacters(String var1) throws XMLStreamException;

   void writeCharacters(char[] var1, int var2, int var3) throws XMLStreamException;

   String getPrefix(String var1) throws XMLStreamException;

   void setPrefix(String var1, String var2) throws XMLStreamException;

   void setDefaultNamespace(String var1) throws XMLStreamException;

   void setNamespaceContext(NamespaceContext var1) throws XMLStreamException;

   NamespaceContext getNamespaceContext();

   Object getProperty(String var1) throws IllegalArgumentException;
}
