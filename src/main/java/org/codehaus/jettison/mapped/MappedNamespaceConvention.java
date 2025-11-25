package org.codehaus.jettison.mapped;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import org.codehaus.jettison.Convention;
import org.codehaus.jettison.Node;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

public class MappedNamespaceConvention implements Convention, NamespaceContext {
   private Map xnsToJns = new HashMap();
   private Map jnsToXns = new HashMap();
   private List attributesAsElements;
   private List jsonAttributesAsElements;
   private boolean supressAtAttributes;
   private boolean ignoreNamespaces;
   private String attributeKey = "@";
   private TypeConverter typeConverter;
   private Set primitiveArrayKeys;

   public MappedNamespaceConvention() {
      this.typeConverter = Configuration.newDefaultConverterInstance();
   }

   public MappedNamespaceConvention(Configuration config) {
      this.xnsToJns = config.getXmlToJsonNamespaces();
      this.attributesAsElements = config.getAttributesAsElements();
      this.supressAtAttributes = config.isSupressAtAttributes();
      this.ignoreNamespaces = config.isIgnoreNamespaces();
      this.attributeKey = config.getAttributeKey();
      this.primitiveArrayKeys = config.getPrimitiveArrayKeys();
      Iterator itr = this.xnsToJns.entrySet().iterator();

      while(itr.hasNext()) {
         Entry entry = (Entry)itr.next();
         this.jnsToXns.put(entry.getValue(), entry.getKey());
      }

      this.jsonAttributesAsElements = new ArrayList();
      if (this.attributesAsElements != null) {
         itr = this.attributesAsElements.iterator();

         while(itr.hasNext()) {
            QName q = (QName)itr.next();
            this.jsonAttributesAsElements.add(this.createAttributeKey(q.getPrefix(), q.getNamespaceURI(), q.getLocalPart()));
         }
      }

      this.typeConverter = config.getTypeConverter();
   }

   public void processAttributesAndNamespaces(Node n, JSONObject object) throws JSONException {
      Iterator itr = object.keys();

      while(true) {
         while(itr.hasNext()) {
            String k = (String)itr.next();
            if (this.supressAtAttributes) {
               if (k.startsWith(this.attributeKey)) {
                  k = k.substring(1);
               }

               if (null == this.jsonAttributesAsElements) {
                  this.jsonAttributesAsElements = new ArrayList();
               }

               if (!this.jsonAttributesAsElements.contains(k)) {
                  this.jsonAttributesAsElements.add(k);
               }
            }

            String strValue;
            String xns;
            if (k.startsWith(this.attributeKey)) {
               Object o = object.opt(k);
               k = k.substring(1);
               if (k.equals("xmlns")) {
                  if (o instanceof JSONObject) {
                     JSONObject jo = (JSONObject)o;
                     Iterator pitr = jo.keys();

                     while(pitr.hasNext()) {
                        String prefix = (String)pitr.next();
                        String uri = jo.getString(prefix);
                        n.setNamespace(prefix, uri);
                     }
                  }
               } else {
                  strValue = o == null ? null : o.toString();
                  xns = null;
                  QName name;
                  if (k.contains(".")) {
                     name = this.createQName(k, n);
                  } else {
                     name = new QName("", k);
                  }

                  n.setAttribute(name, strValue);
               }

               itr.remove();
            } else {
               int dot = k.lastIndexOf(46);
               if (dot != -1) {
                  strValue = k.substring(0, dot);
                  xns = this.getNamespaceURI(strValue);
                  n.setNamespace("", xns);
               }
            }
         }

         return;
      }
   }

   public String getNamespaceURI(String prefix) {
      return this.ignoreNamespaces ? "" : (String)this.jnsToXns.get(prefix);
   }

   public String getPrefix(String namespaceURI) {
      return this.ignoreNamespaces ? "" : (String)this.xnsToJns.get(namespaceURI);
   }

   public Iterator getPrefixes(String arg0) {
      return this.ignoreNamespaces ? Collections.EMPTY_SET.iterator() : this.jnsToXns.keySet().iterator();
   }

   public QName createQName(String rootName, Node node) {
      return this.createQName(rootName);
   }

   private void readAttribute(Node n, String k, JSONArray array) throws JSONException {
      for(int i = 0; i < array.length(); ++i) {
         this.readAttribute(n, k, array.getString(i));
      }

   }

   private void readAttribute(Node n, String name, String value) throws JSONException {
      QName qname = this.createQName(name);
      n.getAttributes().put(qname, value);
   }

   private QName createQName(String name) {
      int dot = name.lastIndexOf(46);
      QName qname = null;
      String local = name;
      if (dot == -1) {
         dot = 0;
      } else {
         local = name.substring(dot + 1);
      }

      String jns = name.substring(0, dot);
      String xns = this.getNamespaceURI(jns);
      if (xns == null) {
         qname = new QName(name);
      } else {
         qname = new QName(xns, local);
      }

      return qname;
   }

   public String createAttributeKey(String p, String ns, String local) {
      StringBuilder builder = new StringBuilder();
      if (!this.supressAtAttributes) {
         builder.append(this.attributeKey);
      }

      String jns = this.getJSONNamespace(p, ns);
      if (jns != null && jns.length() != 0) {
         builder.append(jns).append('.');
      }

      return builder.append(local).toString();
   }

   private String getJSONNamespace(String providedPrefix, String ns) {
      if (ns != null && ns.length() != 0 && !this.ignoreNamespaces) {
         String jns = (String)this.xnsToJns.get(ns);
         if (jns == null && providedPrefix != null && providedPrefix.length() > 0) {
            jns = providedPrefix;
         }

         if (jns == null) {
            throw new IllegalStateException("Invalid JSON namespace: " + ns);
         } else {
            return jns;
         }
      } else {
         return "";
      }
   }

   public String createKey(String p, String ns, String local) {
      StringBuilder builder = new StringBuilder();
      String jns = this.getJSONNamespace(p, ns);
      if (jns != null && jns.length() != 0) {
         builder.append(jns).append('.');
      }

      return builder.append(local).toString();
   }

   public boolean isElement(String p, String ns, String local) {
      if (this.attributesAsElements == null) {
         return false;
      } else {
         Iterator itr = this.attributesAsElements.iterator();

         QName q;
         do {
            if (!itr.hasNext()) {
               return false;
            }

            q = (QName)itr.next();
         } while(!q.getNamespaceURI().equals(ns) || !q.getLocalPart().equals(local));

         return true;
      }
   }

   public Object convertToJSONPrimitive(String text) {
      return this.typeConverter.convertToJSONPrimitive(text);
   }

   public Set getPrimitiveArrayKeys() {
      return this.primitiveArrayKeys;
   }
}
