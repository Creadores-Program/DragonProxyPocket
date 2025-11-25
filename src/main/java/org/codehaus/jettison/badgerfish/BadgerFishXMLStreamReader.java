package org.codehaus.jettison.badgerfish;

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLStreamException;
import org.codehaus.jettison.AbstractXMLStreamReader;
import org.codehaus.jettison.Node;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.util.FastStack;

public class BadgerFishXMLStreamReader extends AbstractXMLStreamReader {
   private static final BadgerFishConvention CONVENTION = new BadgerFishConvention();
   private FastStack nodes;
   private String currentText;

   public BadgerFishXMLStreamReader(JSONObject obj) throws JSONException, XMLStreamException {
      String rootName = (String)obj.keys().next();
      this.node = new Node((Node)null, rootName, obj.getJSONObject(rootName), CONVENTION);
      this.nodes = new FastStack();
      this.nodes.push(this.node);
      this.event = 7;
   }

   public int next() throws XMLStreamException {
      if (this.event == 7) {
         this.event = 1;
      } else {
         if (this.event == 2 && this.nodes.size() != 0) {
            this.node = (Node)this.nodes.peek();
         }

         if (this.node.getArray() != null && this.node.getArray().length() > this.node.getArrayIndex()) {
            Node arrayNode = this.node;
            int idx = arrayNode.getArrayIndex();

            try {
               Object o = arrayNode.getArray().get(idx);
               this.processKey(this.node.getCurrentKey(), o);
            } catch (JSONException var4) {
               throw new XMLStreamException(var4);
            }

            ++idx;
            arrayNode.setArrayIndex(idx);
         } else if (this.node.getKeys() != null && this.node.getKeys().hasNext()) {
            this.processElement();
         } else if (this.nodes.size() != 0) {
            this.event = 2;
            this.node = (Node)this.nodes.pop();
         } else {
            this.event = 8;
         }
      }

      return this.event;
   }

   private void processElement() throws XMLStreamException {
      try {
         String nextKey = (String)this.node.getKeys().next();
         Object newObj = this.node.getObject().get(nextKey);
         this.processKey(nextKey, newObj);
      } catch (JSONException var3) {
         throw new XMLStreamException(var3);
      }
   }

   private void processKey(String nextKey, Object newObj) throws JSONException, XMLStreamException {
      JSONArray arr;
      if (!nextKey.equals("$")) {
         if (newObj instanceof JSONObject) {
            this.node = new Node((Node)this.nodes.peek(), nextKey, (JSONObject)newObj, CONVENTION);
            this.nodes.push(this.node);
            this.event = 1;
         } else if (newObj instanceof JSONArray) {
            arr = (JSONArray)newObj;
            if (arr.length() == 0) {
               this.next();
            } else {
               this.node.setArray(arr);
               this.node.setArrayIndex(1);
               this.node.setCurrentKey(nextKey);
               this.processKey(nextKey, arr.get(0));
            }
         } else {
            throw new JSONException("Element [" + nextKey + "] did not contain object, array or text content.");
         }
      } else {
         this.event = 4;
         if (newObj instanceof JSONArray) {
            arr = (JSONArray)newObj;
            StringBuilder buf = new StringBuilder();

            for(int i = 0; i < arr.length(); ++i) {
               buf.append(arr.get(i));
            }

            this.currentText = buf.toString();
         } else {
            this.currentText = newObj == null ? null : newObj.toString();
         }

      }
   }

   public void close() throws XMLStreamException {
   }

   public String getAttributeType(int arg0) {
      return null;
   }

   public String getCharacterEncodingScheme() {
      return null;
   }

   public String getElementText() throws XMLStreamException {
      return this.currentText;
   }

   public NamespaceContext getNamespaceContext() {
      return null;
   }

   public String getText() {
      return this.currentText;
   }
}
