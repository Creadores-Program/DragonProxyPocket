package io.netty.handler.codec.http.multipart;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.EmptyHttpHeaders;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpConstants;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderUtil;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.stream.ChunkedInput;
import io.netty.util.internal.ThreadLocalRandom;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

public class HttpPostRequestEncoder implements ChunkedInput<HttpContent> {
   private static final Map<Pattern, String> percentEncodings = new HashMap();
   private final HttpDataFactory factory;
   private final HttpRequest request;
   private final Charset charset;
   private boolean isChunked;
   private final List<InterfaceHttpData> bodyListDatas;
   final List<InterfaceHttpData> multipartHttpDatas;
   private final boolean isMultipart;
   String multipartDataBoundary;
   String multipartMixedBoundary;
   private boolean headerFinalized;
   private final HttpPostRequestEncoder.EncoderMode encoderMode;
   private boolean isLastChunk;
   private boolean isLastChunkSent;
   private FileUpload currentFileUpload;
   private boolean duringMixedMode;
   private long globalBodySize;
   private long globalProgress;
   private ListIterator<InterfaceHttpData> iterator;
   private ByteBuf currentBuffer;
   private InterfaceHttpData currentData;
   private boolean isKey;

   public HttpPostRequestEncoder(HttpRequest request, boolean multipart) throws HttpPostRequestEncoder.ErrorDataEncoderException {
      this(new DefaultHttpDataFactory(16384L), request, multipart, HttpConstants.DEFAULT_CHARSET, HttpPostRequestEncoder.EncoderMode.RFC1738);
   }

   public HttpPostRequestEncoder(HttpDataFactory factory, HttpRequest request, boolean multipart) throws HttpPostRequestEncoder.ErrorDataEncoderException {
      this(factory, request, multipart, HttpConstants.DEFAULT_CHARSET, HttpPostRequestEncoder.EncoderMode.RFC1738);
   }

   public HttpPostRequestEncoder(HttpDataFactory factory, HttpRequest request, boolean multipart, Charset charset, HttpPostRequestEncoder.EncoderMode encoderMode) throws HttpPostRequestEncoder.ErrorDataEncoderException {
      this.isKey = true;
      if (factory == null) {
         throw new NullPointerException("factory");
      } else if (request == null) {
         throw new NullPointerException("request");
      } else if (charset == null) {
         throw new NullPointerException("charset");
      } else {
         HttpMethod method = request.method();
         if (!method.equals(HttpMethod.POST) && !method.equals(HttpMethod.PUT) && !method.equals(HttpMethod.PATCH) && !method.equals(HttpMethod.OPTIONS)) {
            throw new HttpPostRequestEncoder.ErrorDataEncoderException("Cannot create a Encoder if not a POST");
         } else {
            this.request = request;
            this.charset = charset;
            this.factory = factory;
            this.bodyListDatas = new ArrayList();
            this.isLastChunk = false;
            this.isLastChunkSent = false;
            this.isMultipart = multipart;
            this.multipartHttpDatas = new ArrayList();
            this.encoderMode = encoderMode;
            if (this.isMultipart) {
               this.initDataMultipart();
            }

         }
      }
   }

   public void cleanFiles() {
      this.factory.cleanRequestHttpData(this.request);
   }

   public boolean isMultipart() {
      return this.isMultipart;
   }

   private void initDataMultipart() {
      this.multipartDataBoundary = getNewMultipartDelimiter();
   }

   private void initMixedMultipart() {
      this.multipartMixedBoundary = getNewMultipartDelimiter();
   }

   private static String getNewMultipartDelimiter() {
      return Long.toHexString(ThreadLocalRandom.current().nextLong()).toLowerCase();
   }

   public List<InterfaceHttpData> getBodyListAttributes() {
      return this.bodyListDatas;
   }

   public void setBodyHttpDatas(List<InterfaceHttpData> datas) throws HttpPostRequestEncoder.ErrorDataEncoderException {
      if (datas == null) {
         throw new NullPointerException("datas");
      } else {
         this.globalBodySize = 0L;
         this.bodyListDatas.clear();
         this.currentFileUpload = null;
         this.duringMixedMode = false;
         this.multipartHttpDatas.clear();
         Iterator i$ = datas.iterator();

         while(i$.hasNext()) {
            InterfaceHttpData data = (InterfaceHttpData)i$.next();
            this.addBodyHttpData(data);
         }

      }
   }

   public void addBodyAttribute(String name, String value) throws HttpPostRequestEncoder.ErrorDataEncoderException {
      if (name == null) {
         throw new NullPointerException("name");
      } else {
         String svalue = value;
         if (value == null) {
            svalue = "";
         }

         Attribute data = this.factory.createAttribute(this.request, name, svalue);
         this.addBodyHttpData(data);
      }
   }

   public void addBodyFileUpload(String name, File file, String contentType, boolean isText) throws HttpPostRequestEncoder.ErrorDataEncoderException {
      if (name == null) {
         throw new NullPointerException("name");
      } else if (file == null) {
         throw new NullPointerException("file");
      } else {
         String scontentType = contentType;
         String contentTransferEncoding = null;
         if (contentType == null) {
            if (isText) {
               scontentType = HttpPostBodyUtil.DEFAULT_TEXT_CONTENT_TYPE;
            } else {
               scontentType = HttpPostBodyUtil.DEFAULT_BINARY_CONTENT_TYPE;
            }
         }

         if (!isText) {
            contentTransferEncoding = HttpPostBodyUtil.TransferEncodingMechanism.BINARY.value();
         }

         FileUpload fileUpload = this.factory.createFileUpload(this.request, name, file.getName(), scontentType, contentTransferEncoding, (Charset)null, file.length());

         try {
            fileUpload.setContent(file);
         } catch (IOException var9) {
            throw new HttpPostRequestEncoder.ErrorDataEncoderException(var9);
         }

         this.addBodyHttpData(fileUpload);
      }
   }

   public void addBodyFileUploads(String name, File[] file, String[] contentType, boolean[] isText) throws HttpPostRequestEncoder.ErrorDataEncoderException {
      if (file.length != contentType.length && file.length != isText.length) {
         throw new NullPointerException("Different array length");
      } else {
         for(int i = 0; i < file.length; ++i) {
            this.addBodyFileUpload(name, file[i], contentType[i], isText[i]);
         }

      }
   }

   public void addBodyHttpData(InterfaceHttpData data) throws HttpPostRequestEncoder.ErrorDataEncoderException {
      if (this.headerFinalized) {
         throw new HttpPostRequestEncoder.ErrorDataEncoderException("Cannot add value once finalized");
      } else if (data == null) {
         throw new NullPointerException("data");
      } else {
         this.bodyListDatas.add(data);
         FileUpload fileUpload;
         if (!this.isMultipart) {
            String key;
            Attribute newattribute;
            String value;
            if (data instanceof Attribute) {
               Attribute attribute = (Attribute)data;

               try {
                  key = this.encodeAttribute(attribute.getName(), this.charset);
                  value = this.encodeAttribute(attribute.getValue(), this.charset);
                  newattribute = this.factory.createAttribute(this.request, key, value);
                  this.multipartHttpDatas.add(newattribute);
                  this.globalBodySize += (long)(newattribute.getName().length() + 1) + newattribute.length() + 1L;
               } catch (IOException var7) {
                  throw new HttpPostRequestEncoder.ErrorDataEncoderException(var7);
               }
            } else if (data instanceof FileUpload) {
               fileUpload = (FileUpload)data;
               key = this.encodeAttribute(fileUpload.getName(), this.charset);
               value = this.encodeAttribute(fileUpload.getFilename(), this.charset);
               newattribute = this.factory.createAttribute(this.request, key, value);
               this.multipartHttpDatas.add(newattribute);
               this.globalBodySize += (long)(newattribute.getName().length() + 1) + newattribute.length() + 1L;
            }

         } else {
            if (data instanceof Attribute) {
               InternalAttribute internal;
               if (this.duringMixedMode) {
                  internal = new InternalAttribute(this.charset);
                  internal.addValue("\r\n--" + this.multipartMixedBoundary + "--");
                  this.multipartHttpDatas.add(internal);
                  this.multipartMixedBoundary = null;
                  this.currentFileUpload = null;
                  this.duringMixedMode = false;
               }

               internal = new InternalAttribute(this.charset);
               if (!this.multipartHttpDatas.isEmpty()) {
                  internal.addValue("\r\n");
               }

               internal.addValue("--" + this.multipartDataBoundary + "\r\n");
               Attribute attribute = (Attribute)data;
               internal.addValue(HttpHeaderNames.CONTENT_DISPOSITION + ": " + HttpHeaderValues.FORM_DATA + "; " + HttpHeaderValues.NAME + "=\"" + attribute.getName() + "\"\r\n");
               Charset localcharset = attribute.getCharset();
               if (localcharset != null) {
                  internal.addValue(HttpHeaderNames.CONTENT_TYPE + ": " + HttpPostBodyUtil.DEFAULT_TEXT_CONTENT_TYPE + "; " + HttpHeaderValues.CHARSET + '=' + localcharset + "\r\n");
               }

               internal.addValue("\r\n");
               this.multipartHttpDatas.add(internal);
               this.multipartHttpDatas.add(data);
               this.globalBodySize += attribute.length() + (long)internal.size();
            } else if (data instanceof FileUpload) {
               fileUpload = (FileUpload)data;
               InternalAttribute internal = new InternalAttribute(this.charset);
               if (!this.multipartHttpDatas.isEmpty()) {
                  internal.addValue("\r\n");
               }

               boolean localMixed;
               if (this.duringMixedMode) {
                  if (this.currentFileUpload != null && this.currentFileUpload.getName().equals(fileUpload.getName())) {
                     localMixed = true;
                  } else {
                     internal.addValue("--" + this.multipartMixedBoundary + "--");
                     this.multipartHttpDatas.add(internal);
                     this.multipartMixedBoundary = null;
                     internal = new InternalAttribute(this.charset);
                     internal.addValue("\r\n");
                     localMixed = false;
                     this.currentFileUpload = fileUpload;
                     this.duringMixedMode = false;
                  }
               } else if (this.encoderMode != HttpPostRequestEncoder.EncoderMode.HTML5 && this.currentFileUpload != null && this.currentFileUpload.getName().equals(fileUpload.getName())) {
                  this.initMixedMultipart();
                  InternalAttribute pastAttribute = (InternalAttribute)this.multipartHttpDatas.get(this.multipartHttpDatas.size() - 2);
                  this.globalBodySize -= (long)pastAttribute.size();
                  StringBuilder replacement = (new StringBuilder(139 + this.multipartDataBoundary.length() + this.multipartMixedBoundary.length() * 2 + fileUpload.getFilename().length() + fileUpload.getName().length())).append("--").append(this.multipartDataBoundary).append("\r\n").append(HttpHeaderNames.CONTENT_DISPOSITION).append(": ").append(HttpHeaderValues.FORM_DATA).append("; ").append(HttpHeaderValues.NAME).append("=\"").append(fileUpload.getName()).append("\"\r\n").append(HttpHeaderNames.CONTENT_TYPE).append(": ").append(HttpHeaderValues.MULTIPART_MIXED).append("; ").append(HttpHeaderValues.BOUNDARY).append('=').append(this.multipartMixedBoundary).append("\r\n\r\n").append("--").append(this.multipartMixedBoundary).append("\r\n").append(HttpHeaderNames.CONTENT_DISPOSITION).append(": ").append(HttpHeaderValues.ATTACHMENT).append("; ").append(HttpHeaderValues.FILENAME).append("=\"").append(fileUpload.getFilename()).append("\"\r\n");
                  pastAttribute.setValue(replacement.toString(), 1);
                  pastAttribute.setValue("", 2);
                  this.globalBodySize += (long)pastAttribute.size();
                  localMixed = true;
                  this.duringMixedMode = true;
               } else {
                  localMixed = false;
                  this.currentFileUpload = fileUpload;
                  this.duringMixedMode = false;
               }

               if (localMixed) {
                  internal.addValue("--" + this.multipartMixedBoundary + "\r\n");
                  internal.addValue(HttpHeaderNames.CONTENT_DISPOSITION + ": " + HttpHeaderValues.ATTACHMENT + "; " + HttpHeaderValues.FILENAME + "=\"" + fileUpload.getFilename() + "\"\r\n");
               } else {
                  internal.addValue("--" + this.multipartDataBoundary + "\r\n");
                  internal.addValue(HttpHeaderNames.CONTENT_DISPOSITION + ": " + HttpHeaderValues.FORM_DATA + "; " + HttpHeaderValues.NAME + "=\"" + fileUpload.getName() + "\"; " + HttpHeaderValues.FILENAME + "=\"" + fileUpload.getFilename() + "\"\r\n");
               }

               internal.addValue(HttpHeaderNames.CONTENT_TYPE + ": " + fileUpload.getContentType());
               String contentTransferEncoding = fileUpload.getContentTransferEncoding();
               if (contentTransferEncoding != null && contentTransferEncoding.equals(HttpPostBodyUtil.TransferEncodingMechanism.BINARY.value())) {
                  internal.addValue("\r\n" + HttpHeaderNames.CONTENT_TRANSFER_ENCODING + ": " + HttpPostBodyUtil.TransferEncodingMechanism.BINARY.value() + "\r\n\r\n");
               } else if (fileUpload.getCharset() != null) {
                  internal.addValue("; " + HttpHeaderValues.CHARSET + '=' + fileUpload.getCharset() + "\r\n\r\n");
               } else {
                  internal.addValue("\r\n\r\n");
               }

               this.multipartHttpDatas.add(internal);
               this.multipartHttpDatas.add(data);
               this.globalBodySize += fileUpload.length() + (long)internal.size();
            }

         }
      }
   }

   public HttpRequest finalizeRequest() throws HttpPostRequestEncoder.ErrorDataEncoderException {
      if (!this.headerFinalized) {
         if (this.isMultipart) {
            InternalAttribute internal = new InternalAttribute(this.charset);
            if (this.duringMixedMode) {
               internal.addValue("\r\n--" + this.multipartMixedBoundary + "--");
            }

            internal.addValue("\r\n--" + this.multipartDataBoundary + "--\r\n");
            this.multipartHttpDatas.add(internal);
            this.multipartMixedBoundary = null;
            this.currentFileUpload = null;
            this.duringMixedMode = false;
            this.globalBodySize += (long)internal.size();
         }

         this.headerFinalized = true;
         HttpHeaders headers = this.request.headers();
         List contentTypes = headers.getAllAndConvert(HttpHeaderNames.CONTENT_TYPE);
         List transferEncoding = headers.getAll(HttpHeaderNames.TRANSFER_ENCODING);
         if (contentTypes != null) {
            headers.remove(HttpHeaderNames.CONTENT_TYPE);
            Iterator i$ = contentTypes.iterator();

            while(i$.hasNext()) {
               String contentType = (String)i$.next();
               String lowercased = contentType.toLowerCase();
               if (!lowercased.startsWith(HttpHeaderValues.MULTIPART_FORM_DATA.toString()) && !lowercased.startsWith(HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED.toString())) {
                  headers.add(HttpHeaderNames.CONTENT_TYPE, (CharSequence)contentType);
               }
            }
         }

         if (this.isMultipart) {
            String value = HttpHeaderValues.MULTIPART_FORM_DATA + "; " + HttpHeaderValues.BOUNDARY + '=' + this.multipartDataBoundary;
            headers.add(HttpHeaderNames.CONTENT_TYPE, (CharSequence)value);
         } else {
            headers.add(HttpHeaderNames.CONTENT_TYPE, (CharSequence)HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED);
         }

         long realSize = this.globalBodySize;
         if (this.isMultipart) {
            this.iterator = this.multipartHttpDatas.listIterator();
         } else {
            --realSize;
            this.iterator = this.multipartHttpDatas.listIterator();
         }

         headers.set(HttpHeaderNames.CONTENT_LENGTH, (CharSequence)String.valueOf(realSize));
         if (realSize <= 8096L && !this.isMultipart) {
            HttpContent chunk = this.nextChunk();
            if (this.request instanceof FullHttpRequest) {
               FullHttpRequest fullRequest = (FullHttpRequest)this.request;
               ByteBuf chunkContent = chunk.content();
               if (fullRequest.content() != chunkContent) {
                  fullRequest.content().clear().writeBytes(chunkContent);
                  chunkContent.release();
               }

               return fullRequest;
            } else {
               return new HttpPostRequestEncoder.WrappedFullHttpRequest(this.request, chunk);
            }
         } else {
            this.isChunked = true;
            if (transferEncoding != null) {
               headers.remove(HttpHeaderNames.TRANSFER_ENCODING);
               Iterator i$ = transferEncoding.iterator();

               while(i$.hasNext()) {
                  CharSequence v = (CharSequence)i$.next();
                  if (!HttpHeaderValues.CHUNKED.equalsIgnoreCase(v)) {
                     headers.add(HttpHeaderNames.TRANSFER_ENCODING, (CharSequence)v);
                  }
               }
            }

            HttpHeaderUtil.setTransferEncodingChunked(this.request, true);
            return new HttpPostRequestEncoder.WrappedHttpRequest(this.request);
         }
      } else {
         throw new HttpPostRequestEncoder.ErrorDataEncoderException("Header already encoded");
      }
   }

   public boolean isChunked() {
      return this.isChunked;
   }

   private String encodeAttribute(String s, Charset charset) throws HttpPostRequestEncoder.ErrorDataEncoderException {
      if (s == null) {
         return "";
      } else {
         try {
            String encoded = URLEncoder.encode(s, charset.name());
            Entry entry;
            String replacement;
            if (this.encoderMode == HttpPostRequestEncoder.EncoderMode.RFC3986) {
               for(Iterator i$ = percentEncodings.entrySet().iterator(); i$.hasNext(); encoded = ((Pattern)entry.getKey()).matcher(encoded).replaceAll(replacement)) {
                  entry = (Entry)i$.next();
                  replacement = (String)entry.getValue();
               }
            }

            return encoded;
         } catch (UnsupportedEncodingException var7) {
            throw new HttpPostRequestEncoder.ErrorDataEncoderException(charset.name(), var7);
         }
      }
   }

   private ByteBuf fillByteBuf() {
      int length = this.currentBuffer.readableBytes();
      ByteBuf slice;
      if (length > 8096) {
         slice = this.currentBuffer.slice(this.currentBuffer.readerIndex(), 8096);
         this.currentBuffer.retain();
         this.currentBuffer.skipBytes(8096);
         return slice;
      } else {
         slice = this.currentBuffer;
         this.currentBuffer = null;
         return slice;
      }
   }

   private HttpContent encodeNextChunkMultipart(int sizeleft) throws HttpPostRequestEncoder.ErrorDataEncoderException {
      if (this.currentData == null) {
         return null;
      } else {
         ByteBuf buffer;
         if (this.currentData instanceof InternalAttribute) {
            buffer = ((InternalAttribute)this.currentData).toByteBuf();
            this.currentData = null;
         } else {
            if (this.currentData instanceof Attribute) {
               try {
                  buffer = ((Attribute)this.currentData).getChunk(sizeleft);
               } catch (IOException var5) {
                  throw new HttpPostRequestEncoder.ErrorDataEncoderException(var5);
               }
            } else {
               try {
                  buffer = ((HttpData)this.currentData).getChunk(sizeleft);
               } catch (IOException var4) {
                  throw new HttpPostRequestEncoder.ErrorDataEncoderException(var4);
               }
            }

            if (buffer.capacity() == 0) {
               this.currentData = null;
               return null;
            }
         }

         if (this.currentBuffer == null) {
            this.currentBuffer = buffer;
         } else {
            this.currentBuffer = Unpooled.wrappedBuffer(this.currentBuffer, buffer);
         }

         if (this.currentBuffer.readableBytes() < 8096) {
            this.currentData = null;
            return null;
         } else {
            buffer = this.fillByteBuf();
            return new DefaultHttpContent(buffer);
         }
      }
   }

   private HttpContent encodeNextChunkUrlEncoded(int sizeleft) throws HttpPostRequestEncoder.ErrorDataEncoderException {
      if (this.currentData == null) {
         return null;
      } else {
         int size = sizeleft;
         ByteBuf buffer;
         if (this.isKey) {
            String key = this.currentData.getName();
            buffer = Unpooled.wrappedBuffer(key.getBytes());
            this.isKey = false;
            if (this.currentBuffer == null) {
               this.currentBuffer = Unpooled.wrappedBuffer(buffer, Unpooled.wrappedBuffer("=".getBytes()));
               size = sizeleft - (buffer.readableBytes() + 1);
            } else {
               this.currentBuffer = Unpooled.wrappedBuffer(this.currentBuffer, buffer, Unpooled.wrappedBuffer("=".getBytes()));
               size = sizeleft - (buffer.readableBytes() + 1);
            }

            if (this.currentBuffer.readableBytes() >= 8096) {
               buffer = this.fillByteBuf();
               return new DefaultHttpContent(buffer);
            }
         }

         try {
            buffer = ((HttpData)this.currentData).getChunk(size);
         } catch (IOException var5) {
            throw new HttpPostRequestEncoder.ErrorDataEncoderException(var5);
         }

         ByteBuf delimiter = null;
         if (buffer.readableBytes() < size) {
            this.isKey = true;
            delimiter = this.iterator.hasNext() ? Unpooled.wrappedBuffer("&".getBytes()) : null;
         }

         if (buffer.capacity() == 0) {
            this.currentData = null;
            if (this.currentBuffer == null) {
               this.currentBuffer = delimiter;
            } else if (delimiter != null) {
               this.currentBuffer = Unpooled.wrappedBuffer(this.currentBuffer, delimiter);
            }

            if (this.currentBuffer.readableBytes() >= 8096) {
               buffer = this.fillByteBuf();
               return new DefaultHttpContent(buffer);
            } else {
               return null;
            }
         } else {
            if (this.currentBuffer == null) {
               if (delimiter != null) {
                  this.currentBuffer = Unpooled.wrappedBuffer(buffer, delimiter);
               } else {
                  this.currentBuffer = buffer;
               }
            } else if (delimiter != null) {
               this.currentBuffer = Unpooled.wrappedBuffer(this.currentBuffer, buffer, delimiter);
            } else {
               this.currentBuffer = Unpooled.wrappedBuffer(this.currentBuffer, buffer);
            }

            if (this.currentBuffer.readableBytes() < 8096) {
               this.currentData = null;
               this.isKey = true;
               return null;
            } else {
               buffer = this.fillByteBuf();
               return new DefaultHttpContent(buffer);
            }
         }
      }
   }

   public void close() throws Exception {
   }

   public HttpContent readChunk(ChannelHandlerContext ctx) throws Exception {
      if (this.isLastChunkSent) {
         return null;
      } else {
         HttpContent nextChunk = this.nextChunk();
         this.globalProgress += (long)nextChunk.content().readableBytes();
         return nextChunk;
      }
   }

   private HttpContent nextChunk() throws HttpPostRequestEncoder.ErrorDataEncoderException {
      if (this.isLastChunk) {
         this.isLastChunkSent = true;
         return LastHttpContent.EMPTY_LAST_CONTENT;
      } else {
         int size = 8096;
         if (this.currentBuffer != null) {
            size -= this.currentBuffer.readableBytes();
         }

         ByteBuf buffer;
         if (size <= 0) {
            buffer = this.fillByteBuf();
            return new DefaultHttpContent(buffer);
         } else {
            HttpContent chunk;
            if (this.currentData != null) {
               if (this.isMultipart) {
                  chunk = this.encodeNextChunkMultipart(size);
                  if (chunk != null) {
                     return chunk;
                  }
               } else {
                  chunk = this.encodeNextChunkUrlEncoded(size);
                  if (chunk != null) {
                     return chunk;
                  }
               }

               size = 8096 - this.currentBuffer.readableBytes();
            }

            if (!this.iterator.hasNext()) {
               this.isLastChunk = true;
               buffer = this.currentBuffer;
               this.currentBuffer = null;
               return new DefaultHttpContent(buffer);
            } else {
               while(size > 0 && this.iterator.hasNext()) {
                  this.currentData = (InterfaceHttpData)this.iterator.next();
                  if (this.isMultipart) {
                     chunk = this.encodeNextChunkMultipart(size);
                  } else {
                     chunk = this.encodeNextChunkUrlEncoded(size);
                  }

                  if (chunk != null) {
                     return chunk;
                  }

                  size = 8096 - this.currentBuffer.readableBytes();
               }

               this.isLastChunk = true;
               if (this.currentBuffer == null) {
                  this.isLastChunkSent = true;
                  return LastHttpContent.EMPTY_LAST_CONTENT;
               } else {
                  buffer = this.currentBuffer;
                  this.currentBuffer = null;
                  return new DefaultHttpContent(buffer);
               }
            }
         }
      }
   }

   public boolean isEndOfInput() throws Exception {
      return this.isLastChunkSent;
   }

   public long length() {
      return this.isMultipart ? this.globalBodySize : this.globalBodySize - 1L;
   }

   public long progress() {
      return this.globalProgress;
   }

   static {
      percentEncodings.put(Pattern.compile("\\*"), "%2A");
      percentEncodings.put(Pattern.compile("\\+"), "%20");
      percentEncodings.put(Pattern.compile("%7E"), "~");
   }

   private static final class WrappedFullHttpRequest extends HttpPostRequestEncoder.WrappedHttpRequest implements FullHttpRequest {
      private final HttpContent content;

      private WrappedFullHttpRequest(HttpRequest request, HttpContent content) {
         super(request);
         this.content = content;
      }

      public FullHttpRequest setProtocolVersion(HttpVersion version) {
         super.setProtocolVersion(version);
         return this;
      }

      public FullHttpRequest setMethod(HttpMethod method) {
         super.setMethod(method);
         return this;
      }

      public FullHttpRequest setUri(String uri) {
         super.setUri(uri);
         return this;
      }

      private FullHttpRequest copy(boolean copyContent, ByteBuf newContent) {
         DefaultFullHttpRequest copy = new DefaultFullHttpRequest(this.protocolVersion(), this.method(), this.uri(), copyContent ? this.content().copy() : (newContent == null ? Unpooled.buffer(0) : newContent));
         copy.headers().set(this.headers());
         copy.trailingHeaders().set(this.trailingHeaders());
         return copy;
      }

      public FullHttpRequest copy(ByteBuf newContent) {
         return this.copy(false, newContent);
      }

      public FullHttpRequest copy() {
         return this.copy(true, (ByteBuf)null);
      }

      public FullHttpRequest duplicate() {
         DefaultFullHttpRequest duplicate = new DefaultFullHttpRequest(this.protocolVersion(), this.method(), this.uri(), this.content().duplicate());
         duplicate.headers().set(this.headers());
         duplicate.trailingHeaders().set(this.trailingHeaders());
         return duplicate;
      }

      public FullHttpRequest retain(int increment) {
         this.content.retain(increment);
         return this;
      }

      public FullHttpRequest retain() {
         this.content.retain();
         return this;
      }

      public FullHttpRequest touch() {
         this.content.touch();
         return this;
      }

      public FullHttpRequest touch(Object hint) {
         this.content.touch(hint);
         return this;
      }

      public ByteBuf content() {
         return this.content.content();
      }

      public HttpHeaders trailingHeaders() {
         return (HttpHeaders)(this.content instanceof LastHttpContent ? ((LastHttpContent)this.content).trailingHeaders() : EmptyHttpHeaders.INSTANCE);
      }

      public int refCnt() {
         return this.content.refCnt();
      }

      public boolean release() {
         return this.content.release();
      }

      public boolean release(int decrement) {
         return this.content.release(decrement);
      }

      // $FF: synthetic method
      WrappedFullHttpRequest(HttpRequest x0, HttpContent x1, Object x2) {
         this(x0, x1);
      }
   }

   private static class WrappedHttpRequest implements HttpRequest {
      private final HttpRequest request;

      WrappedHttpRequest(HttpRequest request) {
         this.request = request;
      }

      public HttpRequest setProtocolVersion(HttpVersion version) {
         this.request.setProtocolVersion(version);
         return this;
      }

      public HttpRequest setMethod(HttpMethod method) {
         this.request.setMethod(method);
         return this;
      }

      public HttpRequest setUri(String uri) {
         this.request.setUri(uri);
         return this;
      }

      public HttpMethod method() {
         return this.request.method();
      }

      public String uri() {
         return this.request.uri();
      }

      public HttpVersion protocolVersion() {
         return this.request.protocolVersion();
      }

      public HttpHeaders headers() {
         return this.request.headers();
      }

      public DecoderResult decoderResult() {
         return this.request.decoderResult();
      }

      public void setDecoderResult(DecoderResult result) {
         this.request.setDecoderResult(result);
      }
   }

   public static class ErrorDataEncoderException extends Exception {
      private static final long serialVersionUID = 5020247425493164465L;

      public ErrorDataEncoderException() {
      }

      public ErrorDataEncoderException(String msg) {
         super(msg);
      }

      public ErrorDataEncoderException(Throwable cause) {
         super(cause);
      }

      public ErrorDataEncoderException(String msg, Throwable cause) {
         super(msg, cause);
      }
   }

   public static enum EncoderMode {
      RFC1738,
      RFC3986,
      HTML5;
   }
}
