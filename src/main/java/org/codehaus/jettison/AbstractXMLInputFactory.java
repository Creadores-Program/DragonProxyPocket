package org.codehaus.jettison;

import java.io.ByteArrayOutputStream;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import javax.xml.stream.EventFilter;
import javax.xml.stream.StreamFilter;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLReporter;
import javax.xml.stream.XMLResolver;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.util.XMLEventAllocator;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import org.codehaus.jettison.json.JSONTokener;

public abstract class AbstractXMLInputFactory extends XMLInputFactory {
   private static final int INPUT_BUF_SIZE = 1024;
   private int bufSize = 1024;

   protected AbstractXMLInputFactory() {
   }

   protected AbstractXMLInputFactory(int bufSize) {
      this.bufSize = bufSize;
   }

   public XMLEventReader createFilteredReader(XMLEventReader arg0, EventFilter arg1) throws XMLStreamException {
      return null;
   }

   public XMLStreamReader createFilteredReader(XMLStreamReader arg0, StreamFilter arg1) throws XMLStreamException {
      return null;
   }

   public XMLEventReader createXMLEventReader(InputStream arg0, String encoding) throws XMLStreamException {
      return null;
   }

   public XMLEventReader createXMLEventReader(InputStream arg0) throws XMLStreamException {
      return null;
   }

   public XMLEventReader createXMLEventReader(Reader arg0) throws XMLStreamException {
      return null;
   }

   public XMLEventReader createXMLEventReader(Source arg0) throws XMLStreamException {
      return null;
   }

   public XMLEventReader createXMLEventReader(String systemId, InputStream arg1) throws XMLStreamException {
      return null;
   }

   public XMLEventReader createXMLEventReader(String systemId, Reader arg1) throws XMLStreamException {
      return null;
   }

   public XMLEventReader createXMLEventReader(XMLStreamReader arg0) throws XMLStreamException {
      return null;
   }

   public XMLStreamReader createXMLStreamReader(InputStream is) throws XMLStreamException {
      return this.createXMLStreamReader((InputStream)is, (String)null);
   }

   public XMLStreamReader createXMLStreamReader(InputStream is, String charset) throws XMLStreamException {
      if (charset == null) {
         charset = "UTF-8";
      }

      try {
         String doc = this.readAll(is, charset);
         return this.createXMLStreamReader(this.createNewJSONTokener(doc));
      } catch (IOException var4) {
         throw new XMLStreamException(var4);
      }
   }

   protected JSONTokener createNewJSONTokener(String doc) {
      return new JSONTokener(doc);
   }

   private String readAll(InputStream in, String encoding) throws IOException {
      byte[] buffer = new byte[this.bufSize];
      ByteArrayOutputStream bos = null;

      while(true) {
         int count = in.read(buffer);
         if (count < 0) {
            return bos == null ? "" : bos.toString(encoding);
         }

         if (bos == null) {
            int cap;
            if (count < 64) {
               cap = 64;
            } else if (count == this.bufSize) {
               cap = this.bufSize * 4;
            } else {
               cap = count;
            }

            bos = new ByteArrayOutputStream(cap);
         }

         bos.write(buffer, 0, count);
      }
   }

   public abstract XMLStreamReader createXMLStreamReader(JSONTokener var1) throws XMLStreamException;

   public XMLStreamReader createXMLStreamReader(Reader reader) throws XMLStreamException {
      try {
         return this.createXMLStreamReader(new JSONTokener(this.readAll(reader)));
      } catch (IOException var3) {
         throw new XMLStreamException(var3);
      }
   }

   private String readAll(Reader r) throws IOException {
      char[] buf = new char[this.bufSize];
      int len = 0;

      do {
         int count = r.read(buf, len, buf.length - len);
         if (count < 0) {
            return len == 0 ? "" : new String(buf, 0, len);
         }

         len += count;
      } while(len < buf.length);

      CharArrayWriter wrt = new CharArrayWriter(this.bufSize * 4);
      wrt.write(buf, 0, len);

      while((len = r.read(buf)) != -1) {
         wrt.write(buf, 0, len);
      }

      return wrt.toString();
   }

   public XMLStreamReader createXMLStreamReader(Source src) throws XMLStreamException {
      if (src instanceof StreamSource) {
         StreamSource ss = (StreamSource)src;
         InputStream in = ss.getInputStream();
         String systemId = ss.getSystemId();
         if (in != null) {
            return systemId != null ? this.createXMLStreamReader(systemId, in) : this.createXMLStreamReader(in);
         } else {
            Reader r = ss.getReader();
            if (r != null) {
               return systemId != null ? this.createXMLStreamReader(systemId, r) : this.createXMLStreamReader(r);
            } else {
               throw new UnsupportedOperationException("Only those javax.xml.transform.stream.StreamSource instances supported that have an InputStream or Reader");
            }
         }
      } else {
         throw new UnsupportedOperationException("Only javax.xml.transform.stream.StreamSource type supported");
      }
   }

   public XMLStreamReader createXMLStreamReader(String systemId, InputStream arg1) throws XMLStreamException {
      return this.createXMLStreamReader((InputStream)arg1, (String)null);
   }

   public XMLStreamReader createXMLStreamReader(String systemId, Reader r) throws XMLStreamException {
      return this.createXMLStreamReader(r);
   }

   public XMLEventAllocator getEventAllocator() {
      return null;
   }

   public Object getProperty(String arg0) throws IllegalArgumentException {
      throw new IllegalArgumentException();
   }

   public XMLReporter getXMLReporter() {
      return null;
   }

   public XMLResolver getXMLResolver() {
      return null;
   }

   public boolean isPropertySupported(String arg0) {
      return false;
   }

   public void setEventAllocator(XMLEventAllocator arg0) {
   }

   public void setProperty(String arg0, Object arg1) throws IllegalArgumentException {
      throw new IllegalArgumentException();
   }

   public void setXMLReporter(XMLReporter arg0) {
   }

   public void setXMLResolver(XMLResolver arg0) {
   }
}
