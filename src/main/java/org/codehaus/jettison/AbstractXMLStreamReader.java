package org.codehaus.jettison;

import java.util.Iterator;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

public abstract class AbstractXMLStreamReader implements XMLStreamReader {
   protected int event;
   protected Node node;

   public boolean isAttributeSpecified(int arg0) {
      return false;
   }

   public boolean isCharacters() {
      return this.event == 4;
   }

   public boolean isEndElement() {
      return this.event == 2;
   }

   public boolean isStandalone() {
      return false;
   }

   public boolean isStartElement() {
      return this.event == 1;
   }

   public boolean isWhiteSpace() {
      return false;
   }

   public int nextTag() throws XMLStreamException {
      int event;
      for(event = this.next(); event != 1 && event != 2; event = this.next()) {
      }

      return event;
   }

   public int getEventType() {
      return this.event;
   }

   public void require(int arg0, String arg1, String arg2) throws XMLStreamException {
   }

   public int getAttributeCount() {
      return this.node.getAttributes().size();
   }

   public String getAttributeLocalName(int n) {
      return this.getAttributeName(n).getLocalPart();
   }

   public QName getAttributeName(int n) {
      Iterator itr = this.node.getAttributes().keySet().iterator();
      QName name = null;

      for(int i = 0; i <= n; ++i) {
         name = (QName)itr.next();
      }

      return name;
   }

   public String getAttributeNamespace(int n) {
      return this.getAttributeName(n).getNamespaceURI();
   }

   public String getAttributePrefix(int n) {
      return this.getAttributeName(n).getPrefix();
   }

   public String getAttributeValue(int n) {
      Iterator itr = this.node.getAttributes().values().iterator();
      String name = null;

      for(int i = 0; i <= n; ++i) {
         name = (String)itr.next();
      }

      return name;
   }

   public String getAttributeValue(String ns, String local) {
      return (String)this.node.getAttributes().get(new QName(ns, local));
   }

   public String getAttributeType(int arg0) {
      return null;
   }

   public String getLocalName() {
      return this.getName().getLocalPart();
   }

   public QName getName() {
      return this.node.getName();
   }

   public String getNamespaceURI() {
      return this.getName().getNamespaceURI();
   }

   public int getNamespaceCount() {
      return this.node.getNamespaceCount();
   }

   public String getNamespacePrefix(int n) {
      return this.node.getNamespacePrefix(n);
   }

   public String getNamespaceURI(int n) {
      return this.node.getNamespaceURI(n);
   }

   public String getNamespaceURI(String prefix) {
      return this.node.getNamespaceURI(prefix);
   }

   public boolean hasName() {
      return false;
   }

   public boolean hasNext() throws XMLStreamException {
      return this.event != 8;
   }

   public boolean hasText() {
      return this.event == 4;
   }

   public boolean standaloneSet() {
      return false;
   }

   public String getCharacterEncodingScheme() {
      return null;
   }

   public String getEncoding() {
      return null;
   }

   public Location getLocation() {
      return new Location() {
         public int getCharacterOffset() {
            return 0;
         }

         public int getColumnNumber() {
            return 0;
         }

         public int getLineNumber() {
            return -1;
         }

         public String getPublicId() {
            return null;
         }

         public String getSystemId() {
            return null;
         }
      };
   }

   public String getPIData() {
      return null;
   }

   public String getPITarget() {
      return null;
   }

   public String getPrefix() {
      return this.getName().getPrefix();
   }

   public Object getProperty(String arg0) throws IllegalArgumentException {
      return null;
   }

   public String getVersion() {
      return null;
   }

   public char[] getTextCharacters() {
      String text = this.getText();
      return text != null ? text.toCharArray() : new char[0];
   }

   public int getTextCharacters(int sourceStart, char[] target, int targetStart, int length) throws XMLStreamException {
      String text = this.getText();
      if (text != null) {
         text.getChars(sourceStart, sourceStart + length, target, targetStart);
         return length;
      } else {
         return 0;
      }
   }

   public int getTextLength() {
      String text = this.getText();
      return text != null ? text.length() : 0;
   }

   public int getTextStart() {
      return 0;
   }
}
