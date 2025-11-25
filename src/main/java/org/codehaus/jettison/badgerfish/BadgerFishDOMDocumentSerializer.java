package org.codehaus.jettison.badgerfish;

import java.io.OutputStream;
import org.codehaus.jettison.AbstractDOMDocumentSerializer;

public class BadgerFishDOMDocumentSerializer extends AbstractDOMDocumentSerializer {
   public BadgerFishDOMDocumentSerializer(OutputStream output) {
      super(output, new BadgerFishXMLOutputFactory());
   }
}
