package io.netty.handler.codec.http.multipart;

import io.netty.handler.codec.http.HttpConstants;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.util.internal.PlatformDependent;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class DefaultHttpDataFactory implements HttpDataFactory {
   public static final long MINSIZE = 16384L;
   public static final long MAXSIZE = -1L;
   private final boolean useDisk;
   private final boolean checkSize;
   private long minSize;
   private long maxSize;
   private Charset charset;
   private final Map<HttpRequest, List<HttpData>> requestFileDeleteMap;

   public DefaultHttpDataFactory() {
      this.maxSize = -1L;
      this.charset = HttpConstants.DEFAULT_CHARSET;
      this.requestFileDeleteMap = PlatformDependent.newConcurrentHashMap();
      this.useDisk = false;
      this.checkSize = true;
      this.minSize = 16384L;
   }

   public DefaultHttpDataFactory(Charset charset) {
      this();
      this.charset = charset;
   }

   public DefaultHttpDataFactory(boolean useDisk) {
      this.maxSize = -1L;
      this.charset = HttpConstants.DEFAULT_CHARSET;
      this.requestFileDeleteMap = PlatformDependent.newConcurrentHashMap();
      this.useDisk = useDisk;
      this.checkSize = false;
   }

   public DefaultHttpDataFactory(boolean useDisk, Charset charset) {
      this(useDisk);
      this.charset = charset;
   }

   public DefaultHttpDataFactory(long minSize) {
      this.maxSize = -1L;
      this.charset = HttpConstants.DEFAULT_CHARSET;
      this.requestFileDeleteMap = PlatformDependent.newConcurrentHashMap();
      this.useDisk = false;
      this.checkSize = true;
      this.minSize = minSize;
   }

   public DefaultHttpDataFactory(long minSize, Charset charset) {
      this(minSize);
      this.charset = charset;
   }

   public void setMaxLimit(long maxSize) {
      this.maxSize = maxSize;
   }

   private List<HttpData> getList(HttpRequest request) {
      List<HttpData> list = (List)this.requestFileDeleteMap.get(request);
      if (list == null) {
         list = new ArrayList();
         this.requestFileDeleteMap.put(request, list);
      }

      return (List)list;
   }

   public Attribute createAttribute(HttpRequest request, String name) {
      List fileToDelete;
      if (this.useDisk) {
         Attribute attribute = new DiskAttribute(name, this.charset);
         attribute.setMaxSize(this.maxSize);
         fileToDelete = this.getList(request);
         fileToDelete.add(attribute);
         return attribute;
      } else if (this.checkSize) {
         Attribute attribute = new MixedAttribute(name, this.minSize, this.charset);
         attribute.setMaxSize(this.maxSize);
         fileToDelete = this.getList(request);
         fileToDelete.add(attribute);
         return attribute;
      } else {
         MemoryAttribute attribute = new MemoryAttribute(name);
         attribute.setMaxSize(this.maxSize);
         return attribute;
      }
   }

   private static void checkHttpDataSize(HttpData data) {
      try {
         data.checkSize(data.length());
      } catch (IOException var2) {
         throw new IllegalArgumentException("Attribute bigger than maxSize allowed");
      }
   }

   public Attribute createAttribute(HttpRequest request, String name, String value) {
      List fileToDelete;
      if (this.useDisk) {
         Object attribute;
         try {
            attribute = new DiskAttribute(name, value, this.charset);
            ((Attribute)attribute).setMaxSize(this.maxSize);
         } catch (IOException var6) {
            attribute = new MixedAttribute(name, value, this.minSize, this.charset);
            ((Attribute)attribute).setMaxSize(this.maxSize);
         }

         checkHttpDataSize((HttpData)attribute);
         fileToDelete = this.getList(request);
         fileToDelete.add(attribute);
         return (Attribute)attribute;
      } else if (this.checkSize) {
         Attribute attribute = new MixedAttribute(name, value, this.minSize, this.charset);
         attribute.setMaxSize(this.maxSize);
         checkHttpDataSize(attribute);
         fileToDelete = this.getList(request);
         fileToDelete.add(attribute);
         return attribute;
      } else {
         try {
            MemoryAttribute attribute = new MemoryAttribute(name, value, this.charset);
            attribute.setMaxSize(this.maxSize);
            checkHttpDataSize(attribute);
            return attribute;
         } catch (IOException var7) {
            throw new IllegalArgumentException(var7);
         }
      }
   }

   public FileUpload createFileUpload(HttpRequest request, String name, String filename, String contentType, String contentTransferEncoding, Charset charset, long size) {
      List fileToDelete;
      if (this.useDisk) {
         FileUpload fileUpload = new DiskFileUpload(name, filename, contentType, contentTransferEncoding, charset, size);
         fileUpload.setMaxSize(this.maxSize);
         checkHttpDataSize(fileUpload);
         fileToDelete = this.getList(request);
         fileToDelete.add(fileUpload);
         return fileUpload;
      } else if (this.checkSize) {
         FileUpload fileUpload = new MixedFileUpload(name, filename, contentType, contentTransferEncoding, charset, size, this.minSize);
         fileUpload.setMaxSize(this.maxSize);
         checkHttpDataSize(fileUpload);
         fileToDelete = this.getList(request);
         fileToDelete.add(fileUpload);
         return fileUpload;
      } else {
         MemoryFileUpload fileUpload = new MemoryFileUpload(name, filename, contentType, contentTransferEncoding, charset, size);
         fileUpload.setMaxSize(this.maxSize);
         checkHttpDataSize(fileUpload);
         return fileUpload;
      }
   }

   public void removeHttpDataFromClean(HttpRequest request, InterfaceHttpData data) {
      if (data instanceof HttpData) {
         List<HttpData> fileToDelete = this.getList(request);
         fileToDelete.remove(data);
      }

   }

   public void cleanRequestHttpData(HttpRequest request) {
      List<HttpData> fileToDelete = (List)this.requestFileDeleteMap.remove(request);
      if (fileToDelete != null) {
         Iterator i$ = fileToDelete.iterator();

         while(i$.hasNext()) {
            HttpData data = (HttpData)i$.next();
            data.delete();
         }

         fileToDelete.clear();
      }

   }

   public void cleanAllHttpData() {
      Iterator i = this.requestFileDeleteMap.entrySet().iterator();

      while(true) {
         List fileToDelete;
         do {
            if (!i.hasNext()) {
               return;
            }

            Entry<HttpRequest, List<HttpData>> e = (Entry)i.next();
            i.remove();
            fileToDelete = (List)e.getValue();
         } while(fileToDelete == null);

         Iterator i$ = fileToDelete.iterator();

         while(i$.hasNext()) {
            HttpData data = (HttpData)i$.next();
            data.delete();
         }

         fileToDelete.clear();
      }
   }
}
