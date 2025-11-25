package org.codehaus.jettison.mapped;

import org.codehaus.jettison.AbstractDOMDocumentParser;

public class MappedDOMDocumentParser extends AbstractDOMDocumentParser {
   public MappedDOMDocumentParser(Configuration con) {
      super(new MappedXMLInputFactory(con));
   }
}
