package org.codehaus.jettison;

import java.util.Iterator;
import javax.xml.namespace.NamespaceContext;
import org.codehaus.jettison.util.FastStack;

public class XsonNamespaceContext implements NamespaceContext {
   private FastStack nodes;

   public XsonNamespaceContext(FastStack nodes) {
      this.nodes = nodes;
   }

   public String getNamespaceURI(String prefix) {
      Iterator itr = this.nodes.iterator();

      String uri;
      do {
         if (!itr.hasNext()) {
            return null;
         }

         Node node = (Node)itr.next();
         uri = node.getNamespaceURI(prefix);
      } while(uri == null);

      return uri;
   }

   public String getPrefix(String namespaceURI) {
      Iterator itr = this.nodes.iterator();

      String prefix;
      do {
         if (!itr.hasNext()) {
            return null;
         }

         Node node = (Node)itr.next();
         prefix = node.getNamespacePrefix(namespaceURI);
      } while(prefix == null);

      return prefix;
   }

   public Iterator getPrefixes(String namespaceURI) {
      return null;
   }
}
