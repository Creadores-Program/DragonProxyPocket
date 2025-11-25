package io.netty.handler.codec.http;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufProcessor;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.util.internal.AppendableCharSequence;
import java.util.List;

public abstract class HttpObjectDecoder extends ByteToMessageDecoder {
   private static final String EMPTY_VALUE = "";
   private final int maxChunkSize;
   private final boolean chunkedSupported;
   protected final boolean validateHeaders;
   private final HttpObjectDecoder.HeaderParser headerParser;
   private final HttpObjectDecoder.LineParser lineParser;
   private HttpMessage message;
   private long chunkSize;
   private long contentLength;
   private volatile boolean resetRequested;
   private CharSequence name;
   private CharSequence value;
   private LastHttpContent trailer;
   private HttpObjectDecoder.State currentState;

   protected HttpObjectDecoder() {
      this(4096, 8192, 8192, true);
   }

   protected HttpObjectDecoder(int maxInitialLineLength, int maxHeaderSize, int maxChunkSize, boolean chunkedSupported) {
      this(maxInitialLineLength, maxHeaderSize, maxChunkSize, chunkedSupported, true);
   }

   protected HttpObjectDecoder(int maxInitialLineLength, int maxHeaderSize, int maxChunkSize, boolean chunkedSupported, boolean validateHeaders) {
      this.contentLength = Long.MIN_VALUE;
      this.currentState = HttpObjectDecoder.State.SKIP_CONTROL_CHARS;
      if (maxInitialLineLength <= 0) {
         throw new IllegalArgumentException("maxInitialLineLength must be a positive integer: " + maxInitialLineLength);
      } else if (maxHeaderSize <= 0) {
         throw new IllegalArgumentException("maxHeaderSize must be a positive integer: " + maxHeaderSize);
      } else if (maxChunkSize <= 0) {
         throw new IllegalArgumentException("maxChunkSize must be a positive integer: " + maxChunkSize);
      } else {
         this.maxChunkSize = maxChunkSize;
         this.chunkedSupported = chunkedSupported;
         this.validateHeaders = validateHeaders;
         AppendableCharSequence seq = new AppendableCharSequence(128);
         this.lineParser = new HttpObjectDecoder.LineParser(seq, maxInitialLineLength);
         this.headerParser = new HttpObjectDecoder.HeaderParser(seq, maxHeaderSize);
      }
   }

   protected void decode(ChannelHandlerContext ctx, ByteBuf buffer, List<Object> out) throws Exception {
      if (this.resetRequested) {
         this.resetNow();
      }

      int toRead;
      int toRead;
      AppendableCharSequence line;
      switch(this.currentState) {
      case SKIP_CONTROL_CHARS:
         if (!skipControlCharacters(buffer)) {
            return;
         }

         this.currentState = HttpObjectDecoder.State.READ_INITIAL;
      case READ_INITIAL:
         try {
            line = this.lineParser.parse(buffer);
            if (line == null) {
               return;
            }

            String[] initialLine = splitInitialLine(line);
            if (initialLine.length < 3) {
               this.currentState = HttpObjectDecoder.State.SKIP_CONTROL_CHARS;
               return;
            }

            this.message = this.createMessage(initialLine);
            this.currentState = HttpObjectDecoder.State.READ_HEADER;
         } catch (Exception var9) {
            out.add(this.invalidMessage(var9));
            return;
         }
      case READ_HEADER:
         try {
            HttpObjectDecoder.State nextState = this.readHeaders(buffer);
            if (nextState == null) {
               return;
            }

            this.currentState = nextState;
            switch(nextState) {
            case SKIP_CONTROL_CHARS:
               out.add(this.message);
               out.add(LastHttpContent.EMPTY_LAST_CONTENT);
               this.resetNow();
               return;
            case READ_CHUNK_SIZE:
               if (!this.chunkedSupported) {
                  throw new IllegalArgumentException("Chunked messages not supported");
               }

               out.add(this.message);
               return;
            default:
               long contentLength = this.contentLength();
               if (contentLength != 0L && (contentLength != -1L || !this.isDecodingRequest())) {
                  assert nextState == HttpObjectDecoder.State.READ_FIXED_LENGTH_CONTENT || nextState == HttpObjectDecoder.State.READ_VARIABLE_LENGTH_CONTENT;

                  out.add(this.message);
                  if (nextState == HttpObjectDecoder.State.READ_FIXED_LENGTH_CONTENT) {
                     this.chunkSize = contentLength;
                  }

                  return;
               }

               out.add(this.message);
               out.add(LastHttpContent.EMPTY_LAST_CONTENT);
               this.resetNow();
               return;
            }
         } catch (Exception var10) {
            out.add(this.invalidMessage(var10));
            return;
         }
      case READ_CHUNK_SIZE:
         try {
            line = this.lineParser.parse(buffer);
            if (line == null) {
               return;
            }

            toRead = getChunkSize(line.toString());
            this.chunkSize = (long)toRead;
            if (toRead == 0) {
               this.currentState = HttpObjectDecoder.State.READ_CHUNK_FOOTER;
               return;
            }

            this.currentState = HttpObjectDecoder.State.READ_CHUNKED_CONTENT;
         } catch (Exception var8) {
            out.add(this.invalidChunk(var8));
            return;
         }
      case READ_CHUNKED_CONTENT:
         assert this.chunkSize <= 2147483647L;

         toRead = Math.min((int)this.chunkSize, this.maxChunkSize);
         toRead = Math.min(toRead, buffer.readableBytes());
         if (toRead == 0) {
            return;
         }

         HttpContent chunk = new DefaultHttpContent(buffer.readSlice(toRead).retain());
         this.chunkSize -= (long)toRead;
         out.add(chunk);
         if (this.chunkSize != 0L) {
            return;
         }

         this.currentState = HttpObjectDecoder.State.READ_CHUNK_DELIMITER;
      case READ_CHUNK_DELIMITER:
         toRead = buffer.writerIndex();
         toRead = buffer.readerIndex();

         while(toRead > toRead) {
            byte next = buffer.getByte(toRead++);
            if (next == 10) {
               this.currentState = HttpObjectDecoder.State.READ_CHUNK_SIZE;
               break;
            }
         }

         buffer.readerIndex(toRead);
         return;
      case READ_VARIABLE_LENGTH_CONTENT:
         toRead = Math.min(buffer.readableBytes(), this.maxChunkSize);
         if (toRead > 0) {
            ByteBuf content = buffer.readSlice(toRead).retain();
            out.add(new DefaultHttpContent(content));
         }

         return;
      case READ_FIXED_LENGTH_CONTENT:
         toRead = buffer.readableBytes();
         if (toRead == 0) {
            return;
         }

         toRead = Math.min(toRead, this.maxChunkSize);
         if ((long)toRead > this.chunkSize) {
            toRead = (int)this.chunkSize;
         }

         ByteBuf content = buffer.readSlice(toRead).retain();
         this.chunkSize -= (long)toRead;
         if (this.chunkSize == 0L) {
            out.add(new DefaultLastHttpContent(content, this.validateHeaders));
            this.resetNow();
         } else {
            out.add(new DefaultHttpContent(content));
         }

         return;
      case READ_CHUNK_FOOTER:
         try {
            LastHttpContent trailer = this.readTrailingHeaders(buffer);
            if (trailer == null) {
               return;
            }

            out.add(trailer);
            this.resetNow();
            return;
         } catch (Exception var7) {
            out.add(this.invalidChunk(var7));
            return;
         }
      case BAD_MESSAGE:
         buffer.skipBytes(buffer.readableBytes());
         break;
      case UPGRADED:
         toRead = buffer.readableBytes();
         if (toRead > 0) {
            out.add(buffer.readBytes(toRead));
         }
      }

   }

   protected void decodeLast(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
      this.decode(ctx, in, out);
      if (this.message != null) {
         boolean chunked = HttpHeaderUtil.isTransferEncodingChunked(this.message);
         if (this.currentState == HttpObjectDecoder.State.READ_VARIABLE_LENGTH_CONTENT && !in.isReadable() && !chunked) {
            out.add(LastHttpContent.EMPTY_LAST_CONTENT);
            this.reset();
            return;
         }

         boolean prematureClosure;
         if (!this.isDecodingRequest() && !chunked) {
            prematureClosure = this.contentLength() > 0L;
         } else {
            prematureClosure = true;
         }

         this.resetNow();
         if (!prematureClosure) {
            out.add(LastHttpContent.EMPTY_LAST_CONTENT);
         }
      }

   }

   protected boolean isContentAlwaysEmpty(HttpMessage msg) {
      if (msg instanceof HttpResponse) {
         HttpResponse res = (HttpResponse)msg;
         int code = res.status().code();
         if (code >= 100 && code < 200) {
            return code != 101 || res.headers().contains(HttpHeaderNames.SEC_WEBSOCKET_ACCEPT);
         }

         switch(code) {
         case 204:
         case 205:
         case 304:
            return true;
         }
      }

      return false;
   }

   public void reset() {
      this.resetRequested = true;
   }

   private void resetNow() {
      HttpMessage message = this.message;
      this.message = null;
      this.name = null;
      this.value = null;
      this.contentLength = Long.MIN_VALUE;
      this.lineParser.reset();
      this.headerParser.reset();
      this.trailer = null;
      if (!this.isDecodingRequest()) {
         HttpResponse res = (HttpResponse)message;
         if (res != null && res.status().code() == 101) {
            this.currentState = HttpObjectDecoder.State.UPGRADED;
            return;
         }
      }

      this.currentState = HttpObjectDecoder.State.SKIP_CONTROL_CHARS;
   }

   private HttpMessage invalidMessage(Exception cause) {
      this.currentState = HttpObjectDecoder.State.BAD_MESSAGE;
      if (this.message != null) {
         this.message.setDecoderResult(DecoderResult.failure(cause));
      } else {
         this.message = this.createInvalidMessage();
         this.message.setDecoderResult(DecoderResult.failure(cause));
      }

      HttpMessage ret = this.message;
      this.message = null;
      return ret;
   }

   private HttpContent invalidChunk(Exception cause) {
      this.currentState = HttpObjectDecoder.State.BAD_MESSAGE;
      HttpContent chunk = new DefaultLastHttpContent(Unpooled.EMPTY_BUFFER);
      chunk.setDecoderResult(DecoderResult.failure(cause));
      this.message = null;
      this.trailer = null;
      return chunk;
   }

   private static boolean skipControlCharacters(ByteBuf buffer) {
      boolean skiped = false;
      int wIdx = buffer.writerIndex();
      int rIdx = buffer.readerIndex();

      while(wIdx > rIdx) {
         int c = buffer.getUnsignedByte(rIdx++);
         if (!Character.isISOControl(c) && !Character.isWhitespace(c)) {
            --rIdx;
            skiped = true;
            break;
         }
      }

      buffer.readerIndex(rIdx);
      return skiped;
   }

   private HttpObjectDecoder.State readHeaders(ByteBuf buffer) {
      HttpMessage message = this.message;
      HttpHeaders headers = message.headers();
      AppendableCharSequence line = this.headerParser.parse(buffer);
      if (line == null) {
         return null;
      } else {
         if (line.length() > 0) {
            do {
               char firstChar = line.charAt(0);
               if (this.name != null && (firstChar == ' ' || firstChar == '\t')) {
                  StringBuilder buf = new StringBuilder(this.value.length() + line.length() + 1);
                  buf.append(this.value).append(' ').append(line.toString().trim());
                  this.value = buf.toString();
               } else {
                  if (this.name != null) {
                     headers.add(this.name, this.value);
                  }

                  this.splitHeader(line);
               }

               line = this.headerParser.parse(buffer);
               if (line == null) {
                  return null;
               }
            } while(line.length() > 0);
         }

         if (this.name != null) {
            headers.add(this.name, this.value);
         }

         this.name = null;
         this.value = null;
         HttpObjectDecoder.State nextState;
         if (this.isContentAlwaysEmpty(message)) {
            HttpHeaderUtil.setTransferEncodingChunked(message, false);
            nextState = HttpObjectDecoder.State.SKIP_CONTROL_CHARS;
         } else if (HttpHeaderUtil.isTransferEncodingChunked(message)) {
            nextState = HttpObjectDecoder.State.READ_CHUNK_SIZE;
         } else if (this.contentLength() >= 0L) {
            nextState = HttpObjectDecoder.State.READ_FIXED_LENGTH_CONTENT;
         } else {
            nextState = HttpObjectDecoder.State.READ_VARIABLE_LENGTH_CONTENT;
         }

         return nextState;
      }
   }

   private long contentLength() {
      if (this.contentLength == Long.MIN_VALUE) {
         this.contentLength = HttpHeaderUtil.getContentLength(this.message, -1L);
      }

      return this.contentLength;
   }

   private LastHttpContent readTrailingHeaders(ByteBuf buffer) {
      AppendableCharSequence line = this.headerParser.parse(buffer);
      if (line == null) {
         return null;
      } else {
         CharSequence lastHeader = null;
         if (line.length() <= 0) {
            return LastHttpContent.EMPTY_LAST_CONTENT;
         } else {
            LastHttpContent trailer = this.trailer;
            if (trailer == null) {
               trailer = this.trailer = new DefaultLastHttpContent(Unpooled.EMPTY_BUFFER, this.validateHeaders);
            }

            do {
               char firstChar = line.charAt(0);
               if (lastHeader == null || firstChar != ' ' && firstChar != '\t') {
                  this.splitHeader(line);
                  CharSequence headerName = this.name;
                  if (!HttpHeaderNames.CONTENT_LENGTH.equalsIgnoreCase(headerName) && !HttpHeaderNames.TRANSFER_ENCODING.equalsIgnoreCase(headerName) && !HttpHeaderNames.TRAILER.equalsIgnoreCase(headerName)) {
                     trailer.trailingHeaders().add(headerName, this.value);
                  }

                  lastHeader = this.name;
                  this.name = null;
                  this.value = null;
               } else {
                  List<CharSequence> current = trailer.trailingHeaders().getAll(lastHeader);
                  if (!current.isEmpty()) {
                     int lastPos = current.size() - 1;
                     String lineTrimmed = line.toString().trim();
                     CharSequence currentLastPos = (CharSequence)current.get(lastPos);
                     StringBuilder b = new StringBuilder(currentLastPos.length() + lineTrimmed.length());
                     b.append(currentLastPos).append(lineTrimmed);
                     current.set(lastPos, b.toString());
                  }
               }

               line = this.headerParser.parse(buffer);
               if (line == null) {
                  return null;
               }
            } while(line.length() > 0);

            this.trailer = null;
            return trailer;
         }
      }
   }

   protected abstract boolean isDecodingRequest();

   protected abstract HttpMessage createMessage(String[] var1) throws Exception;

   protected abstract HttpMessage createInvalidMessage();

   private static int getChunkSize(String hex) {
      hex = hex.trim();

      for(int i = 0; i < hex.length(); ++i) {
         char c = hex.charAt(i);
         if (c == ';' || Character.isWhitespace(c) || Character.isISOControl(c)) {
            hex = hex.substring(0, i);
            break;
         }
      }

      return Integer.parseInt(hex, 16);
   }

   private static String[] splitInitialLine(AppendableCharSequence sb) {
      int aStart = findNonWhitespace(sb, 0);
      int aEnd = findWhitespace(sb, aStart);
      int bStart = findNonWhitespace(sb, aEnd);
      int bEnd = findWhitespace(sb, bStart);
      int cStart = findNonWhitespace(sb, bEnd);
      int cEnd = findEndOfString(sb);
      return new String[]{sb.substring(aStart, aEnd), sb.substring(bStart, bEnd), cStart < cEnd ? sb.substring(cStart, cEnd) : ""};
   }

   private void splitHeader(AppendableCharSequence sb) {
      int length = sb.length();
      int nameStart = findNonWhitespace(sb, 0);

      int nameEnd;
      for(nameEnd = nameStart; nameEnd < length; ++nameEnd) {
         char ch = sb.charAt(nameEnd);
         if (ch == ':' || Character.isWhitespace(ch)) {
            break;
         }
      }

      int colonEnd;
      for(colonEnd = nameEnd; colonEnd < length; ++colonEnd) {
         if (sb.charAt(colonEnd) == ':') {
            ++colonEnd;
            break;
         }
      }

      this.name = sb.substring(nameStart, nameEnd);
      int valueStart = findNonWhitespace(sb, colonEnd);
      if (valueStart == length) {
         this.value = "";
      } else {
         int valueEnd = findEndOfString(sb);
         this.value = sb.substring(valueStart, valueEnd);
      }

   }

   private static int findNonWhitespace(CharSequence sb, int offset) {
      int result;
      for(result = offset; result < sb.length() && Character.isWhitespace(sb.charAt(result)); ++result) {
      }

      return result;
   }

   private static int findWhitespace(CharSequence sb, int offset) {
      int result;
      for(result = offset; result < sb.length() && !Character.isWhitespace(sb.charAt(result)); ++result) {
      }

      return result;
   }

   private static int findEndOfString(CharSequence sb) {
      int result;
      for(result = sb.length(); result > 0 && Character.isWhitespace(sb.charAt(result - 1)); --result) {
      }

      return result;
   }

   private static final class LineParser extends HttpObjectDecoder.HeaderParser {
      LineParser(AppendableCharSequence seq, int maxLength) {
         super(seq, maxLength);
      }

      public AppendableCharSequence parse(ByteBuf buffer) {
         this.reset();
         return super.parse(buffer);
      }

      protected TooLongFrameException newException(int maxLength) {
         return new TooLongFrameException("An HTTP line is larger than " + maxLength + " bytes.");
      }
   }

   private static class HeaderParser implements ByteBufProcessor {
      private final AppendableCharSequence seq;
      private final int maxLength;
      private int size;

      HeaderParser(AppendableCharSequence seq, int maxLength) {
         this.seq = seq;
         this.maxLength = maxLength;
      }

      public AppendableCharSequence parse(ByteBuf buffer) {
         this.seq.reset();
         int i = buffer.forEachByte(this);
         if (i == -1) {
            return null;
         } else {
            buffer.readerIndex(i + 1);
            return this.seq;
         }
      }

      public void reset() {
         this.size = 0;
      }

      public boolean process(byte value) throws Exception {
         char nextByte = (char)value;
         if (nextByte == '\r') {
            return true;
         } else if (nextByte == '\n') {
            return false;
         } else if (this.size >= this.maxLength) {
            throw this.newException(this.maxLength);
         } else {
            ++this.size;
            this.seq.append(nextByte);
            return true;
         }
      }

      protected TooLongFrameException newException(int maxLength) {
         return new TooLongFrameException("HTTP header is larger than " + maxLength + " bytes.");
      }
   }

   private static enum State {
      SKIP_CONTROL_CHARS,
      READ_INITIAL,
      READ_HEADER,
      READ_VARIABLE_LENGTH_CONTENT,
      READ_FIXED_LENGTH_CONTENT,
      READ_CHUNK_SIZE,
      READ_CHUNKED_CONTENT,
      READ_CHUNK_DELIMITER,
      READ_CHUNK_FOOTER,
      BAD_MESSAGE,
      UPGRADED;
   }
}
