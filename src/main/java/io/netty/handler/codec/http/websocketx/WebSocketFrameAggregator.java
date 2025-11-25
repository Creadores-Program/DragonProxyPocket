package io.netty.handler.codec.http.websocketx;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.MessageAggregator;

public class WebSocketFrameAggregator extends MessageAggregator<WebSocketFrame, WebSocketFrame, ContinuationWebSocketFrame, WebSocketFrame> {
   public WebSocketFrameAggregator(int maxContentLength) {
      super(maxContentLength);
   }

   protected boolean isStartMessage(WebSocketFrame msg) throws Exception {
      return msg instanceof TextWebSocketFrame || msg instanceof BinaryWebSocketFrame;
   }

   protected boolean isContentMessage(WebSocketFrame msg) throws Exception {
      return msg instanceof ContinuationWebSocketFrame;
   }

   protected boolean isLastContentMessage(ContinuationWebSocketFrame msg) throws Exception {
      return this.isContentMessage((WebSocketFrame)msg) && msg.isFinalFragment();
   }

   protected boolean isAggregated(WebSocketFrame msg) throws Exception {
      if (msg.isFinalFragment()) {
         return !this.isContentMessage(msg);
      } else {
         return !this.isStartMessage(msg) && !this.isContentMessage(msg);
      }
   }

   protected boolean hasContentLength(WebSocketFrame start) throws Exception {
      return false;
   }

   protected long contentLength(WebSocketFrame start) throws Exception {
      throw new UnsupportedOperationException();
   }

   protected Object newContinueResponse(WebSocketFrame start) throws Exception {
      return null;
   }

   protected WebSocketFrame beginAggregation(WebSocketFrame start, ByteBuf content) throws Exception {
      if (start instanceof TextWebSocketFrame) {
         return new TextWebSocketFrame(true, start.rsv(), content);
      } else if (start instanceof BinaryWebSocketFrame) {
         return new BinaryWebSocketFrame(true, start.rsv(), content);
      } else {
         throw new Error();
      }
   }
}
