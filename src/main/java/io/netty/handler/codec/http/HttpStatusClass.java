package io.netty.handler.codec.http;

import io.netty.handler.codec.AsciiString;

public enum HttpStatusClass {
   INFORMATIONAL(100, 200, "Informational"),
   SUCCESS(200, 300, "Success"),
   REDIRECTION(300, 400, "Redirection"),
   CLIENT_ERROR(400, 500, "Client Error"),
   SERVER_ERROR(500, 600, "Server Error"),
   UNKNOWN(0, 0, "Unknown Status") {
      public boolean contains(int code) {
         return code < 100 || code >= 600;
      }
   };

   private final int min;
   private final int max;
   private final AsciiString defaultReasonPhrase;

   public static HttpStatusClass valueOf(int code) {
      if (INFORMATIONAL.contains(code)) {
         return INFORMATIONAL;
      } else if (SUCCESS.contains(code)) {
         return SUCCESS;
      } else if (REDIRECTION.contains(code)) {
         return REDIRECTION;
      } else if (CLIENT_ERROR.contains(code)) {
         return CLIENT_ERROR;
      } else {
         return SERVER_ERROR.contains(code) ? SERVER_ERROR : UNKNOWN;
      }
   }

   private HttpStatusClass(int min, int max, String defaultReasonPhrase) {
      this.min = min;
      this.max = max;
      this.defaultReasonPhrase = new AsciiString(defaultReasonPhrase);
   }

   public boolean contains(int code) {
      return code >= this.min && code < this.max;
   }

   AsciiString defaultReasonPhrase() {
      return this.defaultReasonPhrase;
   }

   // $FF: synthetic method
   HttpStatusClass(int x2, int x3, String x4, Object x5) {
      this(x2, x3, x4);
   }
}
