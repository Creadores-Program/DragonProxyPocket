package org.codehaus.jettison.mapped;

import java.util.Map;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.codehaus.jettison.AbstractXMLInputFactory;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.json.JSONTokener;

public class MappedXMLInputFactory extends AbstractXMLInputFactory {
   private MappedNamespaceConvention convention;

   public MappedXMLInputFactory(Map nstojns) {
      this(new Configuration(nstojns));
   }

   public MappedXMLInputFactory(Configuration config) {
      this.convention = new MappedNamespaceConvention(config);
   }

   public XMLStreamReader createXMLStreamReader(JSONTokener tokener) throws XMLStreamException {
      try {
         JSONObject root = this.createJSONObject(tokener);
         return new MappedXMLStreamReader(root, this.convention);
      } catch (JSONException var4) {
         int column = var4.getColumn();
         if (column == -1) {
            throw new XMLStreamException(var4);
         } else {
            throw new XMLStreamException(var4.getMessage(), new MappedXMLInputFactory.ErrorLocation(var4.getLine(), var4.getColumn()), var4);
         }
      }
   }

   protected JSONObject createJSONObject(JSONTokener tokener) throws JSONException {
      return new JSONObject(tokener);
   }

   private static class ErrorLocation implements Location {
      private int line = -1;
      private int column = -1;

      public ErrorLocation(int line, int column) {
         this.line = line;
         this.column = column;
      }

      public int getCharacterOffset() {
         return 0;
      }

      public int getColumnNumber() {
         return this.column;
      }

      public int getLineNumber() {
         return this.line;
      }

      public String getPublicId() {
         return null;
      }

      public String getSystemId() {
         return null;
      }
   }
}
