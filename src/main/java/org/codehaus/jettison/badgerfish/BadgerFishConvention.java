package org.codehaus.jettison.badgerfish;

import java.util.Iterator;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import org.codehaus.jettison.Convention;
import org.codehaus.jettison.Node;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

public class BadgerFishConvention implements Convention {
   public void processAttributesAndNamespaces(Node n, JSONObject object) throws JSONException, XMLStreamException {
      Iterator itr = object.keys();

      while(true) {
         String k;
         do {
            if (!itr.hasNext()) {
               return;
            }

            k = (String)itr.next();
         } while(!k.startsWith("@"));

         Object o = object.opt(k);
         k = k.substring(1);
         if (k.equals("xmlns")) {
            if (o instanceof JSONObject) {
               JSONObject jo = (JSONObject)o;

               String prefix;
               String uri;
               for(Iterator pitr = jo.keys(); pitr.hasNext(); n.setNamespace(prefix, uri)) {
                  prefix = (String)pitr.next();
                  uri = jo.getString(prefix);
                  if (prefix.equals("$")) {
                     prefix = "";
                  }
               }
            }
         } else {
            String strValue = (String)o;
            QName name = null;
            if (k.contains(":")) {
               name = this.createQName(k, n);
            } else {
               name = new QName("", k);
            }

            n.setAttribute(name, strValue);
         }

         itr.remove();
      }
   }

   public QName createQName(String rootName, Node node) throws XMLStreamException {
      int idx = rootName.indexOf(58);
      String prefix;
      if (idx != -1) {
         prefix = rootName.substring(0, idx);
         String local = rootName.substring(idx + 1);
         String uri = node.getNamespaceURI(prefix);
         if (uri == null) {
            throw new XMLStreamException("Invalid prefix " + prefix + " on element " + rootName);
         } else {
            return new QName(uri, local, prefix);
         }
      } else {
         prefix = node.getNamespaceURI("");
         return prefix != null ? new QName(prefix, rootName) : new QName(rootName);
      }
   }
}
