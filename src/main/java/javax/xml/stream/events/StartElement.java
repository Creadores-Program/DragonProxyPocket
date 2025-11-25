package javax.xml.stream.events;

import java.util.Iterator;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;

public interface StartElement extends XMLEvent {
   QName getName();

   Iterator getAttributes();

   Iterator getNamespaces();

   Attribute getAttributeByName(QName var1);

   NamespaceContext getNamespaceContext();

   String getNamespaceURI(String var1);
}
