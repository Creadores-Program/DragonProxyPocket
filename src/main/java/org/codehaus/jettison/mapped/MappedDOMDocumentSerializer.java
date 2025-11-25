package org.codehaus.jettison.mapped;

import java.io.OutputStream;
import org.codehaus.jettison.AbstractDOMDocumentSerializer;

public class MappedDOMDocumentSerializer extends AbstractDOMDocumentSerializer {
   public MappedDOMDocumentSerializer(OutputStream output, Configuration con) {
      super(output, new MappedXMLOutputFactory(con));
   }
}
