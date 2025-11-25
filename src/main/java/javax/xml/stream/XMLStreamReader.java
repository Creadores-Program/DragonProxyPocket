package javax.xml.stream;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;

public interface XMLStreamReader extends XMLStreamConstants {
   Object getProperty(String var1) throws IllegalArgumentException;

   int next() throws XMLStreamException;

   void require(int var1, String var2, String var3) throws XMLStreamException;

   String getElementText() throws XMLStreamException;

   int nextTag() throws XMLStreamException;

   boolean hasNext() throws XMLStreamException;

   void close() throws XMLStreamException;

   String getNamespaceURI(String var1);

   boolean isStartElement();

   boolean isEndElement();

   boolean isCharacters();

   boolean isWhiteSpace();

   String getAttributeValue(String var1, String var2);

   int getAttributeCount();

   QName getAttributeName(int var1);

   String getAttributeNamespace(int var1);

   String getAttributeLocalName(int var1);

   String getAttributePrefix(int var1);

   String getAttributeType(int var1);

   String getAttributeValue(int var1);

   boolean isAttributeSpecified(int var1);

   int getNamespaceCount();

   String getNamespacePrefix(int var1);

   String getNamespaceURI(int var1);

   NamespaceContext getNamespaceContext();

   int getEventType();

   String getText();

   char[] getTextCharacters();

   int getTextCharacters(int var1, char[] var2, int var3, int var4) throws XMLStreamException;

   int getTextStart();

   int getTextLength();

   String getEncoding();

   boolean hasText();

   Location getLocation();

   QName getName();

   String getLocalName();

   boolean hasName();

   String getNamespaceURI();

   String getPrefix();

   String getVersion();

   boolean isStandalone();

   boolean standaloneSet();

   String getCharacterEncodingScheme();

   String getPITarget();

   String getPIData();
}
