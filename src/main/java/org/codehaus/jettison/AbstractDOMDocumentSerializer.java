package org.codehaus.jettison;

import java.io.IOException;
import java.io.OutputStream;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import org.w3c.dom.Element;

public class AbstractDOMDocumentSerializer {
   private OutputStream output;
   private AbstractXMLOutputFactory writerFactory;

   public AbstractDOMDocumentSerializer(OutputStream output, AbstractXMLOutputFactory writerFactory) {
      this.output = output;
      this.writerFactory = writerFactory;
   }

   public void serialize(Element el) throws IOException {
      if (this.output == null) {
         throw new IllegalStateException("OutputStream cannot be null");
      } else {
         try {
            DOMSource source = new DOMSource(el);
            XMLInputFactory readerFactory = XMLInputFactory.newInstance();
            XMLStreamReader streamReader = readerFactory.createXMLStreamReader((Source)source);
            XMLEventReader eventReader = readerFactory.createXMLEventReader(streamReader);
            XMLEventWriter eventWriter = this.writerFactory.createXMLEventWriter(this.output);
            eventWriter.add(eventReader);
            eventWriter.close();
         } catch (XMLStreamException var7) {
            IOException ioex = new IOException("Cannot serialize: " + el);
            ioex.initCause(var7);
            throw ioex;
         }
      }
   }
}
