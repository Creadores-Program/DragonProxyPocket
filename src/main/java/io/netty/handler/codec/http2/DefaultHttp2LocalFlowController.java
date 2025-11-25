package io.netty.handler.codec.http2;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.internal.ObjectUtil;
import java.util.Iterator;

public class DefaultHttp2LocalFlowController implements Http2LocalFlowController {
   private static final int DEFAULT_COMPOSITE_EXCEPTION_SIZE = 4;
   public static final float DEFAULT_WINDOW_UPDATE_RATIO = 0.5F;
   private final Http2Connection connection;
   private final Http2FrameWriter frameWriter;
   private volatile float windowUpdateRatio;
   private volatile int initialWindowSize;

   public DefaultHttp2LocalFlowController(Http2Connection connection, Http2FrameWriter frameWriter) {
      this(connection, frameWriter, 0.5F);
   }

   public DefaultHttp2LocalFlowController(Http2Connection connection, Http2FrameWriter frameWriter, float windowUpdateRatio) {
      this.initialWindowSize = 65535;
      this.connection = (Http2Connection)ObjectUtil.checkNotNull(connection, "connection");
      this.frameWriter = (Http2FrameWriter)ObjectUtil.checkNotNull(frameWriter, "frameWriter");
      this.windowUpdateRatio(windowUpdateRatio);
      Http2Stream connectionStream = connection.connectionStream();
      connectionStream.setProperty(DefaultHttp2LocalFlowController.FlowState.class, new DefaultHttp2LocalFlowController.FlowState(connectionStream, this.initialWindowSize));
      connection.addListener(new Http2ConnectionAdapter() {
         public void streamAdded(Http2Stream stream) {
            stream.setProperty(DefaultHttp2LocalFlowController.FlowState.class, DefaultHttp2LocalFlowController.this.new FlowState(stream, 0));
         }

         public void streamActive(Http2Stream stream) {
            DefaultHttp2LocalFlowController.this.state(stream).window(DefaultHttp2LocalFlowController.this.initialWindowSize);
         }
      });
   }

   public void initialWindowSize(int newWindowSize) throws Http2Exception {
      int delta = newWindowSize - this.initialWindowSize;
      this.initialWindowSize = newWindowSize;
      Http2Exception.CompositeStreamException compositeException = null;
      Iterator i$ = this.connection.activeStreams().iterator();

      while(i$.hasNext()) {
         Http2Stream stream = (Http2Stream)i$.next();

         try {
            DefaultHttp2LocalFlowController.FlowState state = this.state(stream);
            state.incrementFlowControlWindows(delta);
            state.incrementInitialStreamWindow(delta);
         } catch (Http2Exception.StreamException var7) {
            if (compositeException == null) {
               compositeException = new Http2Exception.CompositeStreamException(var7.error(), 4);
            }

            compositeException.add(var7);
         }
      }

      if (compositeException != null) {
         throw compositeException;
      }
   }

   public int initialWindowSize() {
      return this.initialWindowSize;
   }

   public int windowSize(Http2Stream stream) {
      return this.state(stream).window();
   }

   public void incrementWindowSize(ChannelHandlerContext ctx, Http2Stream stream, int delta) throws Http2Exception {
      ObjectUtil.checkNotNull(ctx, "ctx");
      DefaultHttp2LocalFlowController.FlowState state = this.state(stream);
      state.incrementInitialStreamWindow(delta);
      state.writeWindowUpdateIfNeeded(ctx);
   }

   public void consumeBytes(ChannelHandlerContext ctx, Http2Stream stream, int numBytes) throws Http2Exception {
      this.state(stream).consumeBytes(ctx, numBytes);
   }

   public int unconsumedBytes(Http2Stream stream) {
      return this.state(stream).unconsumedBytes();
   }

   private static void checkValidRatio(float ratio) {
      if (Double.compare((double)ratio, 0.0D) <= 0 || Double.compare((double)ratio, 1.0D) >= 0) {
         throw new IllegalArgumentException("Invalid ratio: " + ratio);
      }
   }

   public void windowUpdateRatio(float ratio) {
      checkValidRatio(ratio);
      this.windowUpdateRatio = ratio;
   }

   public float windowUpdateRatio() {
      return this.windowUpdateRatio;
   }

   public void windowUpdateRatio(ChannelHandlerContext ctx, Http2Stream stream, float ratio) throws Http2Exception {
      checkValidRatio(ratio);
      DefaultHttp2LocalFlowController.FlowState state = this.state(stream);
      state.windowUpdateRatio(ratio);
      state.writeWindowUpdateIfNeeded(ctx);
   }

   public float windowUpdateRatio(Http2Stream stream) throws Http2Exception {
      return this.state(stream).windowUpdateRatio();
   }

   public void receiveFlowControlledFrame(ChannelHandlerContext ctx, Http2Stream stream, ByteBuf data, int padding, boolean endOfStream) throws Http2Exception {
      int dataLength = data.readableBytes() + padding;
      this.connectionState().receiveFlowControlledFrame(dataLength);
      DefaultHttp2LocalFlowController.FlowState state = this.state(stream);
      state.endOfStream(endOfStream);
      state.receiveFlowControlledFrame(dataLength);
   }

   private DefaultHttp2LocalFlowController.FlowState connectionState() {
      return this.state(this.connection.connectionStream());
   }

   private DefaultHttp2LocalFlowController.FlowState state(Http2Stream stream) {
      ObjectUtil.checkNotNull(stream, "stream");
      return (DefaultHttp2LocalFlowController.FlowState)stream.getProperty(DefaultHttp2LocalFlowController.FlowState.class);
   }

   private final class FlowState {
      private final Http2Stream stream;
      private int window;
      private int processedWindow;
      private volatile int initialStreamWindowSize;
      private volatile float streamWindowUpdateRatio;
      private int lowerBound;
      private boolean endOfStream;

      FlowState(Http2Stream stream, int initialWindowSize) {
         this.stream = stream;
         this.window(initialWindowSize);
         this.streamWindowUpdateRatio = DefaultHttp2LocalFlowController.this.windowUpdateRatio;
      }

      int window() {
         return this.window;
      }

      void window(int initialWindowSize) {
         this.window = this.processedWindow = this.initialStreamWindowSize = initialWindowSize;
      }

      void endOfStream(boolean endOfStream) {
         this.endOfStream = endOfStream;
      }

      float windowUpdateRatio() {
         return this.streamWindowUpdateRatio;
      }

      void windowUpdateRatio(float ratio) {
         this.streamWindowUpdateRatio = ratio;
      }

      void incrementInitialStreamWindow(int delta) {
         int newValue = (int)Math.min(2147483647L, Math.max(0L, (long)this.initialStreamWindowSize + (long)delta));
         delta = newValue - this.initialStreamWindowSize;
         this.initialStreamWindowSize += delta;
      }

      void incrementFlowControlWindows(int delta) throws Http2Exception {
         if (delta > 0 && this.window > Integer.MAX_VALUE - delta) {
            throw Http2Exception.streamError(this.stream.id(), Http2Error.FLOW_CONTROL_ERROR, "Flow control window overflowed for stream: %d", this.stream.id());
         } else {
            this.window += delta;
            this.processedWindow += delta;
            this.lowerBound = delta < 0 ? delta : 0;
         }
      }

      void receiveFlowControlledFrame(int dataLength) throws Http2Exception {
         assert dataLength >= 0;

         this.window -= dataLength;
         if (this.window < this.lowerBound) {
            throw Http2Exception.streamError(this.stream.id(), Http2Error.FLOW_CONTROL_ERROR, "Flow control window exceeded for stream: %d", this.stream.id());
         }
      }

      void returnProcessedBytes(int delta) throws Http2Exception {
         if (this.processedWindow - delta < this.window) {
            throw Http2Exception.streamError(this.stream.id(), Http2Error.INTERNAL_ERROR, "Attempting to return too many bytes for stream %d", this.stream.id());
         } else {
            this.processedWindow -= delta;
         }
      }

      void consumeBytes(ChannelHandlerContext ctx, int numBytes) throws Http2Exception {
         if (this.stream.id() == 0) {
            throw new UnsupportedOperationException("Returning bytes for the connection window is not supported");
         } else if (numBytes <= 0) {
            throw new IllegalArgumentException("numBytes must be positive");
         } else {
            DefaultHttp2LocalFlowController.FlowState connectionState = DefaultHttp2LocalFlowController.this.connectionState();
            connectionState.returnProcessedBytes(numBytes);
            connectionState.writeWindowUpdateIfNeeded(ctx);
            this.returnProcessedBytes(numBytes);
            this.writeWindowUpdateIfNeeded(ctx);
         }
      }

      int unconsumedBytes() {
         return this.processedWindow - this.window;
      }

      void writeWindowUpdateIfNeeded(ChannelHandlerContext ctx) throws Http2Exception {
         if (!this.endOfStream && this.initialStreamWindowSize > 0) {
            int threshold = (int)((float)this.initialStreamWindowSize * this.streamWindowUpdateRatio);
            if (this.processedWindow <= threshold) {
               this.writeWindowUpdate(ctx);
            }

         }
      }

      void writeWindowUpdate(ChannelHandlerContext ctx) throws Http2Exception {
         int deltaWindowSize = this.initialStreamWindowSize - this.processedWindow;

         try {
            this.incrementFlowControlWindows(deltaWindowSize);
         } catch (Throwable var4) {
            throw Http2Exception.connectionError(Http2Error.INTERNAL_ERROR, var4, "Attempting to return too many bytes for stream %d", this.stream.id());
         }

         DefaultHttp2LocalFlowController.this.frameWriter.writeWindowUpdate(ctx, this.stream.id(), deltaWindowSize, ctx.newPromise());
         ctx.flush();
      }
   }
}
