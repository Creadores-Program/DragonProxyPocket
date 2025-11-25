package org.codehaus.jettison.badgerfish;

import org.codehaus.jettison.AbstractDOMDocumentParser;

public class BadgerFishDOMDocumentParser extends AbstractDOMDocumentParser {
   public BadgerFishDOMDocumentParser() {
      super(new BadgerFishXMLInputFactory());
   }
}
