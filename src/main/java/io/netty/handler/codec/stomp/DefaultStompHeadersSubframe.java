package io.netty.handler.codec.stomp;

import io.netty.handler.codec.DecoderResult;

public class DefaultStompHeadersSubframe implements StompHeadersSubframe {
   protected final StompCommand command;
   protected DecoderResult decoderResult;
   protected final StompHeaders headers;

   public DefaultStompHeadersSubframe(StompCommand command) {
      this.decoderResult = DecoderResult.SUCCESS;
      this.headers = new DefaultStompHeaders();
      if (command == null) {
         throw new NullPointerException("command");
      } else {
         this.command = command;
      }
   }

   public StompCommand command() {
      return this.command;
   }

   public StompHeaders headers() {
      return this.headers;
   }

   public DecoderResult decoderResult() {
      return this.decoderResult;
   }

   public void setDecoderResult(DecoderResult decoderResult) {
      this.decoderResult = decoderResult;
   }

   public String toString() {
      return "StompFrame{command=" + this.command + ", headers=" + this.headers + '}';
   }
}
