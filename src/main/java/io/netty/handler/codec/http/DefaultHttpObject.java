package io.netty.handler.codec.http;

import io.netty.handler.codec.DecoderResult;

public class DefaultHttpObject implements HttpObject {
   private static final int HASH_CODE_PRIME = 31;
   private DecoderResult decoderResult;

   protected DefaultHttpObject() {
      this.decoderResult = DecoderResult.SUCCESS;
   }

   public DecoderResult decoderResult() {
      return this.decoderResult;
   }

   public void setDecoderResult(DecoderResult decoderResult) {
      if (decoderResult == null) {
         throw new NullPointerException("decoderResult");
      } else {
         this.decoderResult = decoderResult;
      }
   }

   public int hashCode() {
      int result = 1;
      int result = 31 * result + this.decoderResult.hashCode();
      return result;
   }

   public boolean equals(Object o) {
      if (!(o instanceof DefaultHttpObject)) {
         return false;
      } else {
         DefaultHttpObject other = (DefaultHttpObject)o;
         return this.decoderResult().equals(other.decoderResult());
      }
   }
}
