package org.codehaus.jettison.badgerfish;

import java.io.IOException;
import java.io.Writer;
import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLStreamException;
import org.codehaus.jettison.AbstractXMLStreamWriter;
import org.codehaus.jettison.Node;
import org.codehaus.jettison.XsonNamespaceContext;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.util.FastStack;

public class BadgerFishXMLStreamWriter extends AbstractXMLStreamWriter {
   private JSONObject root;
   private JSONObject currentNode;
   private Writer writer;
   private FastStack nodes;
   private String currentKey;
   private NamespaceContext ctx;

   public BadgerFishXMLStreamWriter(Writer writer) {
      this(writer, new JSONObject());
   }

   public BadgerFishXMLStreamWriter(Writer writer, JSONObject currentNode) {
      this(writer, new JSONObject(), new FastStack());
   }

   public BadgerFishXMLStreamWriter(Writer writer, JSONObject currentNode, FastStack nodes) {
      this.currentNode = currentNode;
      this.root = currentNode;
      this.writer = writer;
      this.nodes = nodes;
      this.ctx = new XsonNamespaceContext(nodes);
   }

   public void close() throws XMLStreamException {
   }

   public void flush() throws XMLStreamException {
   }

   public NamespaceContext getNamespaceContext() {
      return this.ctx;
   }

   public String getPrefix(String ns) throws XMLStreamException {
      return this.getNamespaceContext().getPrefix(ns);
   }

   public Object getProperty(String arg0) throws IllegalArgumentException {
      return null;
   }

   public void setDefaultNamespace(String arg0) throws XMLStreamException {
   }

   public void setNamespaceContext(NamespaceContext context) throws XMLStreamException {
      this.ctx = context;
   }

   public void setPrefix(String arg0, String arg1) throws XMLStreamException {
   }

   public void writeAttribute(String p, String ns, String local, String value) throws XMLStreamException {
      String key = this.createAttributeKey(p, ns, local);

      try {
         this.getCurrentNode().put(key, (Object)value);
      } catch (JSONException var7) {
         throw new XMLStreamException(var7);
      }
   }

   private String createAttributeKey(String p, String ns, String local) {
      return "@" + this.createKey(p, ns, local);
   }

   private String createKey(String p, String ns, String local) {
      return p != null && !p.equals("") ? p + ":" + local : local;
   }

   public void writeAttribute(String ns, String local, String value) throws XMLStreamException {
      this.writeAttribute((String)null, ns, local, value);
   }

   public void writeAttribute(String local, String value) throws XMLStreamException {
      this.writeAttribute((String)null, local, value);
   }

   public void writeCharacters(String text) throws XMLStreamException {
      try {
         Object o = this.getCurrentNode().opt("$");
         if (o instanceof JSONArray) {
            ((JSONArray)o).put((Object)text);
         } else if (o instanceof String) {
            JSONArray arr = new JSONArray();
            arr.put(o);
            arr.put((Object)text);
            this.getCurrentNode().put("$", (Object)arr);
         } else {
            this.getCurrentNode().put("$", (Object)text);
         }

      } catch (JSONException var4) {
         throw new XMLStreamException(var4);
      }
   }

   public void writeDefaultNamespace(String ns) throws XMLStreamException {
      this.writeNamespace("", ns);
   }

   public void writeEndElement() throws XMLStreamException {
      if (this.getNodes().size() > 1) {
         this.getNodes().pop();
         this.currentNode = ((Node)this.getNodes().peek()).getObject();
      }

   }

   public void writeEntityRef(String arg0) throws XMLStreamException {
   }

   public void writeNamespace(String prefix, String ns) throws XMLStreamException {
      ((Node)this.getNodes().peek()).setNamespace(prefix, ns);

      try {
         JSONObject nsObj = this.getCurrentNode().optJSONObject("@xmlns");
         if (nsObj == null) {
            nsObj = new JSONObject();
            this.getCurrentNode().put("@xmlns", (Object)nsObj);
         }

         if (prefix.equals("")) {
            prefix = "$";
         }

         nsObj.put(prefix, (Object)ns);
      } catch (JSONException var4) {
         throw new XMLStreamException(var4);
      }
   }

   public void writeProcessingInstruction(String arg0, String arg1) throws XMLStreamException {
   }

   public void writeProcessingInstruction(String arg0) throws XMLStreamException {
   }

   public void writeStartDocument() throws XMLStreamException {
   }

   public void writeEndDocument() throws XMLStreamException {
      try {
         this.root.write(this.writer);
         this.writer.flush();
      } catch (JSONException var2) {
         throw new XMLStreamException(var2);
      } catch (IOException var3) {
         throw new XMLStreamException(var3);
      }
   }

   public void writeStartElement(String prefix, String local, String ns) throws XMLStreamException {
      try {
         this.currentKey = this.createKey(prefix, ns, local);
         Object existing = this.getCurrentNode().opt(this.currentKey);
         if (existing instanceof JSONObject) {
            JSONArray array = new JSONArray();
            array.put(existing);
            JSONObject newCurrent = new JSONObject();
            array.put((Object)newCurrent);
            this.getCurrentNode().put(this.currentKey, (Object)array);
            this.currentNode = newCurrent;
            Node node = new Node(this.currentNode);
            this.getNodes().push(node);
         } else {
            JSONObject newCurrent = new JSONObject();
            if (existing instanceof JSONArray) {
               ((JSONArray)existing).put((Object)newCurrent);
            } else {
               this.getCurrentNode().put(this.currentKey, (Object)newCurrent);
            }

            this.currentNode = newCurrent;
            Node node = new Node(this.currentNode);
            this.getNodes().push(node);
         }

      } catch (JSONException var8) {
         throw new XMLStreamException("Could not write start element!", var8);
      }
   }

   protected JSONObject getCurrentNode() {
      return this.currentNode;
   }

   protected FastStack getNodes() {
      return this.nodes;
   }
}
