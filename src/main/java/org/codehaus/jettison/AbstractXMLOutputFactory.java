package org.codehaus.jettison;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamResult;

public abstract class AbstractXMLOutputFactory extends XMLOutputFactory {
   public XMLEventWriter createXMLEventWriter(OutputStream out, String charset) throws XMLStreamException {
      return new AbstractXMLEventWriter(this.createXMLStreamWriter(out, charset));
   }

   public XMLEventWriter createXMLEventWriter(OutputStream out) throws XMLStreamException {
      return new AbstractXMLEventWriter(this.createXMLStreamWriter(out));
   }

   public XMLEventWriter createXMLEventWriter(Result result) throws XMLStreamException {
      return new AbstractXMLEventWriter(this.createXMLStreamWriter(result));
   }

   public XMLEventWriter createXMLEventWriter(Writer writer) throws XMLStreamException {
      return new AbstractXMLEventWriter(this.createXMLStreamWriter(writer));
   }

   public XMLStreamWriter createXMLStreamWriter(OutputStream out, String charset) throws XMLStreamException {
      if (charset == null) {
         charset = "UTF-8";
      }

      try {
         return this.createXMLStreamWriter((Writer)(new OutputStreamWriter(out, charset)));
      } catch (UnsupportedEncodingException var4) {
         throw new XMLStreamException(var4);
      }
   }

   public XMLStreamWriter createXMLStreamWriter(OutputStream out) throws XMLStreamException {
      return this.createXMLStreamWriter(out, (String)null);
   }

   public XMLStreamWriter createXMLStreamWriter(Result result) throws XMLStreamException {
      if (result instanceof StreamResult) {
         StreamResult sr = (StreamResult)result;
         OutputStream out = sr.getOutputStream();
         if (out != null) {
            return this.createXMLStreamWriter(out);
         } else {
            Writer w = sr.getWriter();
            if (w != null) {
               return this.createXMLStreamWriter(w);
            } else {
               throw new UnsupportedOperationException("Only those javax.xml.transform.stream.StreamResult instances supported that have an OutputStream or Writer");
            }
         }
      } else {
         throw new UnsupportedOperationException("Only javax.xml.transform.stream.StreamResult type supported");
      }
   }

   public abstract XMLStreamWriter createXMLStreamWriter(Writer var1) throws XMLStreamException;

   public Object getProperty(String arg0) throws IllegalArgumentException {
      return null;
   }

   public boolean isPropertySupported(String arg0) {
      return false;
   }

   public void setProperty(String arg0, Object arg1) throws IllegalArgumentException {
   }
}
