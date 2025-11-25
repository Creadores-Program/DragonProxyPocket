package org.codehaus.jettison;

import java.util.ArrayList;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

public abstract class AbstractXMLStreamWriter implements XMLStreamWriter {
   ArrayList serializedAsArrays = new ArrayList();

   public void writeCData(String text) throws XMLStreamException {
      this.writeCharacters(text);
   }

   public void writeCharacters(char[] arg0, int arg1, int arg2) throws XMLStreamException {
      this.writeCharacters(new String(arg0, arg1, arg2));
   }

   public void writeEmptyElement(String prefix, String local, String ns) throws XMLStreamException {
      this.writeStartElement(prefix, local, ns);
      this.writeEndElement();
   }

   public void writeEmptyElement(String ns, String local) throws XMLStreamException {
      this.writeStartElement(local, ns);
      this.writeEndElement();
   }

   public void writeEmptyElement(String local) throws XMLStreamException {
      this.writeStartElement(local);
      this.writeEndElement();
   }

   public void writeStartDocument(String arg0, String arg1) throws XMLStreamException {
      this.writeStartDocument();
   }

   public void writeStartDocument(String arg0) throws XMLStreamException {
      this.writeStartDocument();
   }

   public void writeStartElement(String ns, String local) throws XMLStreamException {
      this.writeStartElement("", local, ns);
   }

   public void writeStartElement(String local) throws XMLStreamException {
      this.writeStartElement("", local, "");
   }

   public void writeComment(String arg0) throws XMLStreamException {
   }

   public void writeDTD(String arg0) throws XMLStreamException {
   }

   public void writeEndDocument() throws XMLStreamException {
   }

   public void serializeAsArray(String name) {
      this.serializedAsArrays.add(name);
   }

   /** @deprecated */
   @Deprecated
   public void seriliazeAsArray(String name) {
      this.serializedAsArrays.add(name);
   }

   public ArrayList getSerializedAsArrays() {
      return this.serializedAsArrays;
   }
}
