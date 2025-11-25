package io.netty.handler.codec.http;

public class DefaultHttpRequest extends DefaultHttpMessage implements HttpRequest {
   private static final int HASH_CODE_PRIME = 31;
   private HttpMethod method;
   private String uri;

   public DefaultHttpRequest(HttpVersion httpVersion, HttpMethod method, String uri) {
      this(httpVersion, method, uri, true);
   }

   public DefaultHttpRequest(HttpVersion httpVersion, HttpMethod method, String uri, boolean validateHeaders) {
      super(httpVersion, validateHeaders, false);
      if (method == null) {
         throw new NullPointerException("method");
      } else if (uri == null) {
         throw new NullPointerException("uri");
      } else {
         this.method = method;
         this.uri = uri;
      }
   }

   public HttpMethod method() {
      return this.method;
   }

   public String uri() {
      return this.uri;
   }

   public HttpRequest setMethod(HttpMethod method) {
      if (method == null) {
         throw new NullPointerException("method");
      } else {
         this.method = method;
         return this;
      }
   }

   public HttpRequest setUri(String uri) {
      if (uri == null) {
         throw new NullPointerException("uri");
      } else {
         this.uri = uri;
         return this;
      }
   }

   public HttpRequest setProtocolVersion(HttpVersion version) {
      super.setProtocolVersion(version);
      return this;
   }

   public int hashCode() {
      int result = 1;
      int result = 31 * result + this.method.hashCode();
      result = 31 * result + this.uri.hashCode();
      result = 31 * result + super.hashCode();
      return result;
   }

   public boolean equals(Object o) {
      if (!(o instanceof DefaultHttpRequest)) {
         return false;
      } else {
         DefaultHttpRequest other = (DefaultHttpRequest)o;
         return this.method().equals(other.method()) && this.uri().equalsIgnoreCase(other.uri()) && super.equals(o);
      }
   }

   public String toString() {
      return HttpMessageUtil.appendRequest(new StringBuilder(256), this).toString();
   }
}
