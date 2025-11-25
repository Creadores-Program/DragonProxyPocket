package org.codehaus.jettison.badgerfish;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.codehaus.jettison.AbstractXMLInputFactory;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.json.JSONTokener;

public class BadgerFishXMLInputFactory extends AbstractXMLInputFactory {
   public XMLStreamReader createXMLStreamReader(JSONTokener tokener) throws XMLStreamException {
      try {
         JSONObject root = new JSONObject(tokener);
         return new BadgerFishXMLStreamReader(root);
      } catch (JSONException var3) {
         throw new XMLStreamException(var3);
      }
   }
}
