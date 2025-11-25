package org.codehaus.jettison.mapped;

import java.io.IOException;
import java.io.Writer;
import java.util.Stack;
import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLStreamException;
import org.codehaus.jettison.AbstractXMLStreamWriter;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

public class MappedXMLStreamWriter extends AbstractXMLStreamWriter {
   private static final String MIXED_CONTENT_VALUE_KEY = "$";
   private MappedNamespaceConvention convention;
   protected Writer writer;
   private NamespaceContext namespaceContext;
   private String valueKey = "$";
   private Stack<MappedXMLStreamWriter.JSONProperty> stack = new Stack();
   private MappedXMLStreamWriter.JSONProperty current;

   public MappedXMLStreamWriter(MappedNamespaceConvention convention, Writer writer) {
      this.convention = convention;
      this.writer = writer;
      this.namespaceContext = convention;
   }

   public NamespaceContext getNamespaceContext() {
      return this.namespaceContext;
   }

   public void setNamespaceContext(NamespaceContext context) throws XMLStreamException {
      this.namespaceContext = context;
   }

   public String getTextKey() {
      return this.valueKey;
   }

   public void setValueKey(String valueKey) {
      this.valueKey = valueKey;
   }

   public void writeStartDocument() throws XMLStreamException {
      this.current = new MappedXMLStreamWriter.JSONPropertyObject((String)null, new JSONObject());
      this.stack.clear();
   }

   public void writeStartElement(String prefix, String local, String ns) throws XMLStreamException {
      this.stack.push(this.current);
      String key = this.convention.createKey(prefix, ns, local);
      this.current = new MappedXMLStreamWriter.JSONPropertyString(key);
   }

   public void writeAttribute(String prefix, String ns, String local, String value) throws XMLStreamException {
      String key = this.convention.isElement(prefix, ns, local) ? this.convention.createKey(prefix, ns, local) : this.convention.createAttributeKey(prefix, ns, local);
      MappedXMLStreamWriter.JSONPropertyString prop = new MappedXMLStreamWriter.JSONPropertyString(key);
      prop.addText(value);
      this.current = this.current.withProperty(prop, false);
   }

   public void writeAttribute(String ns, String local, String value) throws XMLStreamException {
      this.writeAttribute((String)null, ns, local, value);
   }

   public void writeAttribute(String local, String value) throws XMLStreamException {
      this.writeAttribute((String)null, local, value);
   }

   public void writeCharacters(String text) throws XMLStreamException {
      this.current.addText(text);
   }

   public void writeEndElement() throws XMLStreamException {
      if (this.stack.isEmpty()) {
         throw new XMLStreamException("Too many closing tags.");
      } else {
         this.current = ((MappedXMLStreamWriter.JSONProperty)this.stack.pop()).withProperty(this.current);
      }
   }

   public void writeEndDocument() throws XMLStreamException {
      if (!this.stack.isEmpty()) {
         throw new XMLStreamException("Missing some closing tags.");
      } else {
         this.writeJSONObject((JSONObject)this.current.getValue());

         try {
            this.writer.flush();
         } catch (IOException var2) {
            throw new XMLStreamException(var2);
         }
      }
   }

   protected void writeJSONObject(JSONObject root) throws XMLStreamException {
      try {
         if (root == null) {
            this.writer.write("null");
         } else {
            root.write(this.writer);
         }

      } catch (JSONException var3) {
         throw new XMLStreamException(var3);
      } catch (IOException var4) {
         throw new XMLStreamException(var4);
      }
   }

   public void close() throws XMLStreamException {
   }

   public void flush() throws XMLStreamException {
   }

   public String getPrefix(String arg0) throws XMLStreamException {
      return null;
   }

   public Object getProperty(String arg0) throws IllegalArgumentException {
      return null;
   }

   public void setDefaultNamespace(String arg0) throws XMLStreamException {
   }

   public void setPrefix(String arg0, String arg1) throws XMLStreamException {
   }

   public void writeDefaultNamespace(String arg0) throws XMLStreamException {
   }

   public void writeEntityRef(String arg0) throws XMLStreamException {
   }

   public void writeNamespace(String arg0, String arg1) throws XMLStreamException {
   }

   public void writeProcessingInstruction(String arg0) throws XMLStreamException {
   }

   public void writeProcessingInstruction(String arg0, String arg1) throws XMLStreamException {
   }

   private final class JSONPropertyObject extends MappedXMLStreamWriter.JSONProperty {
      private JSONObject object;

      public JSONPropertyObject(String key, JSONObject object) {
         super(key);
         this.object = object;
      }

      public Object getValue() {
         return this.object;
      }

      public void addText(String text) {
         if ("$" == MappedXMLStreamWriter.this.valueKey) {
            text = text.trim();
            if (text.length() == 0) {
               return;
            }
         }

         try {
            text = this.object.getString(MappedXMLStreamWriter.this.valueKey) + text;
         } catch (JSONException var4) {
         }

         try {
            if (MappedXMLStreamWriter.this.valueKey != null) {
               this.object.put(MappedXMLStreamWriter.this.valueKey, (Object)text);
            }

         } catch (JSONException var3) {
            throw new AssertionError(var3);
         }
      }

      public MappedXMLStreamWriter.JSONPropertyObject withProperty(MappedXMLStreamWriter.JSONProperty property, boolean add) {
         Object value = property.getValue();
         if (add && value instanceof String) {
            value = MappedXMLStreamWriter.this.convention.convertToJSONPrimitive((String)value);
         }

         Object old = this.object.opt(property.getKey());

         try {
            JSONArray values;
            if (old != null) {
               if (old instanceof JSONArray) {
                  values = (JSONArray)old;
               } else {
                  values = new JSONArray();
                  values.put(old);
               }

               values.put(value);
               this.object.put(property.getKey(), (Object)values);
            } else if (MappedXMLStreamWriter.this.getSerializedAsArrays().contains(property.getKey())) {
               values = new JSONArray();
               values.put(value);
               this.object.put(property.getKey(), (Object)values);
            } else {
               this.object.put(property.getKey(), value);
            }
         } catch (JSONException var6) {
            var6.printStackTrace();
         }

         return this;
      }
   }

   private final class JSONPropertyString extends MappedXMLStreamWriter.JSONProperty {
      private StringBuilder object = new StringBuilder();

      public JSONPropertyString(String key) {
         super(key);
      }

      public Object getValue() {
         return this.object.toString();
      }

      public void addText(String text) {
         this.object.append(text);
      }

      public MappedXMLStreamWriter.JSONPropertyObject withProperty(MappedXMLStreamWriter.JSONProperty property, boolean add) {
         JSONObject jo = new JSONObject();

         try {
            String strValue = this.getValue().toString();
            if ("$" == MappedXMLStreamWriter.this.valueKey) {
               strValue = strValue.trim();
            }

            if (strValue.length() > 0) {
               jo.put(MappedXMLStreamWriter.this.valueKey, (Object)strValue);
            }

            Object value = property.getValue();
            if (add && value instanceof String) {
               value = MappedXMLStreamWriter.this.convention.convertToJSONPrimitive((String)value);
            }

            if (MappedXMLStreamWriter.this.getSerializedAsArrays().contains(property.getKey())) {
               JSONArray values = new JSONArray();
               values.put(value);
               value = values;
            }

            jo.put(property.getKey(), value);
         } catch (JSONException var7) {
            throw new AssertionError(var7);
         }

         return MappedXMLStreamWriter.this.new JSONPropertyObject(this.getKey(), jo);
      }
   }

   private abstract class JSONProperty {
      private String key;

      public JSONProperty(String key) {
         this.key = key;
      }

      public String getKey() {
         return this.key;
      }

      public abstract Object getValue();

      public abstract void addText(String var1);

      public abstract MappedXMLStreamWriter.JSONPropertyObject withProperty(MappedXMLStreamWriter.JSONProperty var1, boolean var2);

      public MappedXMLStreamWriter.JSONPropertyObject withProperty(MappedXMLStreamWriter.JSONProperty property) {
         return this.withProperty(property, true);
      }
   }
}
