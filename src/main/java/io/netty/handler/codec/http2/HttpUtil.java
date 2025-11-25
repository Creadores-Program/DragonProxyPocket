package io.netty.handler.codec.http2;

import io.netty.handler.codec.AsciiString;
import io.netty.handler.codec.BinaryHeaders;
import io.netty.handler.codec.TextHeaders;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpMessage;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderUtil;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.internal.ObjectUtil;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Pattern;

public final class HttpUtil {
   private static final Set<CharSequence> HTTP_TO_HTTP2_HEADER_BLACKLIST = new HashSet<CharSequence>() {
      private static final long serialVersionUID = -5678614530214167043L;

      {
         this.add(HttpHeaderNames.CONNECTION);
         this.add(HttpHeaderNames.KEEP_ALIVE);
         this.add(HttpHeaderNames.PROXY_CONNECTION);
         this.add(HttpHeaderNames.TRANSFER_ENCODING);
         this.add(HttpHeaderNames.HOST);
         this.add(HttpHeaderNames.UPGRADE);
         this.add(HttpUtil.ExtensionHeaderNames.STREAM_ID.text());
         this.add(HttpUtil.ExtensionHeaderNames.AUTHORITY.text());
         this.add(HttpUtil.ExtensionHeaderNames.SCHEME.text());
         this.add(HttpUtil.ExtensionHeaderNames.PATH.text());
      }
   };
   public static final HttpMethod OUT_OF_MESSAGE_SEQUENCE_METHOD;
   public static final String OUT_OF_MESSAGE_SEQUENCE_PATH = "";
   public static final HttpResponseStatus OUT_OF_MESSAGE_SEQUENCE_RETURN_CODE;
   private static final Pattern AUTHORITY_REPLACEMENT_PATTERN;

   private HttpUtil() {
   }

   public static HttpResponseStatus parseStatus(AsciiString status) throws Http2Exception {
      try {
         HttpResponseStatus result = HttpResponseStatus.parseLine(status);
         if (result == HttpResponseStatus.SWITCHING_PROTOCOLS) {
            throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "Invalid HTTP/2 status code '%d'", result.code());
         } else {
            return result;
         }
      } catch (Http2Exception var3) {
         throw var3;
      } catch (Throwable var4) {
         throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, var4, "Unrecognized HTTP status code '%s' encountered in translation to HTTP/1.x", status);
      }
   }

   public static FullHttpResponse toHttpResponse(int streamId, Http2Headers http2Headers, boolean validateHttpHeaders) throws Http2Exception {
      HttpResponseStatus status = parseStatus(http2Headers.status());
      FullHttpResponse msg = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, validateHttpHeaders);
      addHttp2ToHttpHeaders(streamId, http2Headers, msg, false);
      return msg;
   }

   public static FullHttpRequest toHttpRequest(int streamId, Http2Headers http2Headers, boolean validateHttpHeaders) throws Http2Exception {
      AsciiString method = (AsciiString)ObjectUtil.checkNotNull(http2Headers.method(), "method header cannot be null in conversion to HTTP/1.x");
      AsciiString path = (AsciiString)ObjectUtil.checkNotNull(http2Headers.path(), "path header cannot be null in conversion to HTTP/1.x");
      FullHttpRequest msg = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.valueOf(method.toString()), path.toString(), validateHttpHeaders);
      addHttp2ToHttpHeaders(streamId, http2Headers, msg, false);
      return msg;
   }

   public static void addHttp2ToHttpHeaders(int streamId, Http2Headers sourceHeaders, FullHttpMessage destinationMessage, boolean addToTrailer) throws Http2Exception {
      HttpHeaders headers = addToTrailer ? destinationMessage.trailingHeaders() : destinationMessage.headers();
      boolean request = destinationMessage instanceof HttpRequest;
      HttpUtil.Http2ToHttpHeaderTranslator visitor = new HttpUtil.Http2ToHttpHeaderTranslator(streamId, headers, request);

      try {
         sourceHeaders.forEachEntry(visitor);
      } catch (Http2Exception var8) {
         throw var8;
      } catch (Throwable var9) {
         throw Http2Exception.streamError(streamId, Http2Error.PROTOCOL_ERROR, var9, "HTTP/2 to HTTP/1.x headers conversion error");
      }

      headers.remove(HttpHeaderNames.TRANSFER_ENCODING);
      headers.remove(HttpHeaderNames.TRAILER);
      if (!addToTrailer) {
         headers.setInt(HttpUtil.ExtensionHeaderNames.STREAM_ID.text(), streamId);
         HttpHeaderUtil.setKeepAlive(destinationMessage, true);
      }

   }

   public static Http2Headers toHttp2Headers(FullHttpMessage in) throws Exception {
      final Http2Headers out = new DefaultHttp2Headers();
      HttpHeaders inHeaders = in.headers();
      if (in instanceof HttpRequest) {
         HttpRequest request = (HttpRequest)in;
         out.path(new AsciiString(request.uri()));
         out.method(new AsciiString(request.method().toString()));
         String value = (String)inHeaders.getAndConvert(HttpHeaderNames.HOST);
         if (value != null) {
            URI hostUri = URI.create(value);
            value = hostUri.getAuthority();
            if (value != null) {
               out.authority(new AsciiString(AUTHORITY_REPLACEMENT_PATTERN.matcher(value).replaceFirst("")));
            }

            value = hostUri.getScheme();
            if (value != null) {
               out.scheme(new AsciiString(value));
            }
         }

         CharSequence cValue = (CharSequence)inHeaders.get(HttpUtil.ExtensionHeaderNames.AUTHORITY.text());
         if (cValue != null) {
            out.authority(AsciiString.of(cValue));
         }

         cValue = (CharSequence)inHeaders.get(HttpUtil.ExtensionHeaderNames.SCHEME.text());
         if (cValue != null) {
            out.scheme(AsciiString.of(cValue));
         }
      } else if (in instanceof HttpResponse) {
         HttpResponse response = (HttpResponse)in;
         out.status(new AsciiString(Integer.toString(response.status().code())));
      }

      inHeaders.forEachEntry(new TextHeaders.EntryVisitor() {
         public boolean visit(Entry<CharSequence, CharSequence> entry) throws Exception {
            AsciiString aName = AsciiString.of((CharSequence)entry.getKey()).toLowerCase();
            if (!HttpUtil.HTTP_TO_HTTP2_HEADER_BLACKLIST.contains(aName)) {
               AsciiString aValue = AsciiString.of((CharSequence)entry.getValue());
               if (!aName.equalsIgnoreCase(HttpHeaderNames.TE) || aValue.equalsIgnoreCase(HttpHeaderValues.TRAILERS)) {
                  out.add(aName, aValue);
               }
            }

            return true;
         }
      });
      return out;
   }

   static {
      OUT_OF_MESSAGE_SEQUENCE_METHOD = HttpMethod.OPTIONS;
      OUT_OF_MESSAGE_SEQUENCE_RETURN_CODE = HttpResponseStatus.OK;
      AUTHORITY_REPLACEMENT_PATTERN = Pattern.compile("^.*@");
   }

   private static final class Http2ToHttpHeaderTranslator implements BinaryHeaders.EntryVisitor {
      private static final Map<AsciiString, AsciiString> REQUEST_HEADER_TRANSLATIONS = new HashMap();
      private static final Map<AsciiString, AsciiString> RESPONSE_HEADER_TRANSLATIONS = new HashMap();
      private final int streamId;
      private final HttpHeaders output;
      private final Map<AsciiString, AsciiString> translations;

      Http2ToHttpHeaderTranslator(int streamId, HttpHeaders output, boolean request) {
         this.streamId = streamId;
         this.output = output;
         this.translations = request ? REQUEST_HEADER_TRANSLATIONS : RESPONSE_HEADER_TRANSLATIONS;
      }

      public boolean visit(Entry<AsciiString, AsciiString> entry) throws Http2Exception {
         AsciiString name = (AsciiString)entry.getKey();
         AsciiString value = (AsciiString)entry.getValue();
         AsciiString translatedName = (AsciiString)this.translations.get(name);
         if (translatedName != null || !Http2Headers.PseudoHeaderName.isPseudoHeader(name)) {
            if (translatedName == null) {
               translatedName = name;
            }

            if (translatedName.isEmpty() || translatedName.charAt(0) == ':') {
               throw Http2Exception.streamError(this.streamId, Http2Error.PROTOCOL_ERROR, "Invalid HTTP/2 header '%s' encountered in translation to HTTP/1.x", translatedName);
            }

            this.output.add(translatedName, (CharSequence)value);
         }

         return true;
      }

      static {
         RESPONSE_HEADER_TRANSLATIONS.put(Http2Headers.PseudoHeaderName.AUTHORITY.value(), HttpUtil.ExtensionHeaderNames.AUTHORITY.text());
         RESPONSE_HEADER_TRANSLATIONS.put(Http2Headers.PseudoHeaderName.SCHEME.value(), HttpUtil.ExtensionHeaderNames.SCHEME.text());
         REQUEST_HEADER_TRANSLATIONS.putAll(RESPONSE_HEADER_TRANSLATIONS);
         RESPONSE_HEADER_TRANSLATIONS.put(Http2Headers.PseudoHeaderName.PATH.value(), HttpUtil.ExtensionHeaderNames.PATH.text());
      }
   }

   public static enum ExtensionHeaderNames {
      STREAM_ID("x-http2-stream-id"),
      AUTHORITY("x-http2-authority"),
      SCHEME("x-http2-scheme"),
      PATH("x-http2-path"),
      STREAM_PROMISE_ID("x-http2-stream-promise-id"),
      STREAM_DEPENDENCY_ID("x-http2-stream-dependency-id"),
      STREAM_WEIGHT("x-http2-stream-weight");

      private final AsciiString text;

      private ExtensionHeaderNames(String text) {
         this.text = new AsciiString(text);
      }

      public AsciiString text() {
         return this.text;
      }
   }
}
