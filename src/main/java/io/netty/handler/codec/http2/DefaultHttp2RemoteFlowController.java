package io.netty.handler.codec.http2;

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.internal.ObjectUtil;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Queue;

public class DefaultHttp2RemoteFlowController implements Http2RemoteFlowController {
   private static final Comparator<Http2Stream> WEIGHT_ORDER = new Comparator<Http2Stream>() {
      public int compare(Http2Stream o1, Http2Stream o2) {
         return o2.weight() - o1.weight();
      }
   };
   private final Http2Connection connection;
   private int initialWindowSize = 65535;
   private ChannelHandlerContext ctx;
   private boolean needFlush;

   public DefaultHttp2RemoteFlowController(Http2Connection connection) {
      this.connection = (Http2Connection)ObjectUtil.checkNotNull(connection, "connection");
      connection.connectionStream().setProperty(DefaultHttp2RemoteFlowController.FlowState.class, new DefaultHttp2RemoteFlowController.FlowState(connection.connectionStream(), this.initialWindowSize));
      connection.addListener(new Http2ConnectionAdapter() {
         public void streamAdded(Http2Stream stream) {
            stream.setProperty(DefaultHttp2RemoteFlowController.FlowState.class, DefaultHttp2RemoteFlowController.this.new FlowState(stream, 0));
         }

         public void streamActive(Http2Stream stream) {
            DefaultHttp2RemoteFlowController.state(stream).window(DefaultHttp2RemoteFlowController.this.initialWindowSize);
         }

         public void streamInactive(Http2Stream stream) {
            DefaultHttp2RemoteFlowController.state(stream).clear();
         }

         public void priorityTreeParentChanged(Http2Stream stream, Http2Stream oldParent) {
            Http2Stream parent = stream.parent();
            if (parent != null) {
               int delta = DefaultHttp2RemoteFlowController.state(stream).streamableBytesForTree();
               if (delta != 0) {
                  DefaultHttp2RemoteFlowController.state(parent).incrementStreamableBytesForTree(delta);
               }
            }

         }

         public void priorityTreeParentChanging(Http2Stream stream, Http2Stream newParent) {
            Http2Stream parent = stream.parent();
            if (parent != null) {
               int delta = -DefaultHttp2RemoteFlowController.state(stream).streamableBytesForTree();
               if (delta != 0) {
                  DefaultHttp2RemoteFlowController.state(parent).incrementStreamableBytesForTree(delta);
               }
            }

         }
      });
   }

   public void initialWindowSize(int newWindowSize) throws Http2Exception {
      if (newWindowSize < 0) {
         throw new IllegalArgumentException("Invalid initial window size: " + newWindowSize);
      } else {
         int delta = newWindowSize - this.initialWindowSize;
         this.initialWindowSize = newWindowSize;
         Iterator i$ = this.connection.activeStreams().iterator();

         while(i$.hasNext()) {
            Http2Stream stream = (Http2Stream)i$.next();
            state(stream).incrementStreamWindow(delta);
         }

         if (delta > 0) {
            this.writePendingBytes();
         }

      }
   }

   public int initialWindowSize() {
      return this.initialWindowSize;
   }

   public int windowSize(Http2Stream stream) {
      return state(stream).window();
   }

   public void incrementWindowSize(ChannelHandlerContext ctx, Http2Stream stream, int delta) throws Http2Exception {
      if (stream.id() == 0) {
         this.connectionState().incrementStreamWindow(delta);
         this.writePendingBytes();
      } else {
         DefaultHttp2RemoteFlowController.FlowState state = state(stream);
         state.incrementStreamWindow(delta);
         state.writeBytes(state.writableWindow());
         this.flush();
      }

   }

   public void sendFlowControlled(ChannelHandlerContext ctx, Http2Stream stream, Http2RemoteFlowController.FlowControlled payload) {
      ObjectUtil.checkNotNull(ctx, "ctx");
      ObjectUtil.checkNotNull(payload, "payload");
      if (this.ctx != null && this.ctx != ctx) {
         throw new IllegalArgumentException("Writing data from multiple ChannelHandlerContexts is not supported");
      } else {
         this.ctx = ctx;

         try {
            DefaultHttp2RemoteFlowController.FlowState state = state(stream);
            state.newFrame(payload);
            state.writeBytes(state.writableWindow());
            this.flush();
         } catch (Throwable var5) {
            payload.error(var5);
         }

      }
   }

   int streamableBytesForTree(Http2Stream stream) {
      return state(stream).streamableBytesForTree();
   }

   private static DefaultHttp2RemoteFlowController.FlowState state(Http2Stream stream) {
      ObjectUtil.checkNotNull(stream, "stream");
      return (DefaultHttp2RemoteFlowController.FlowState)stream.getProperty(DefaultHttp2RemoteFlowController.FlowState.class);
   }

   private DefaultHttp2RemoteFlowController.FlowState connectionState() {
      return state(this.connection.connectionStream());
   }

   private int connectionWindow() {
      return this.connectionState().window();
   }

   private void flush() {
      if (this.needFlush) {
         this.ctx.flush();
         this.needFlush = false;
      }

   }

   private void writePendingBytes() {
      Http2Stream connectionStream = this.connection.connectionStream();
      int connectionWindow = state(connectionStream).window();
      if (connectionWindow > 0) {
         this.writeChildren(connectionStream, connectionWindow);
         Iterator i$ = this.connection.activeStreams().iterator();

         while(i$.hasNext()) {
            Http2Stream stream = (Http2Stream)i$.next();
            writeChildNode(state(stream));
         }

         this.flush();
      }

   }

   private int writeChildren(Http2Stream parent, int connectionWindow) {
      DefaultHttp2RemoteFlowController.FlowState state = state(parent);
      if (state.streamableBytesForTree() <= 0) {
         return 0;
      } else {
         int bytesAllocated = 0;
         int tail;
         int head;
         if (state.streamableBytesForTree() <= connectionWindow) {
            for(Iterator i$ = parent.children().iterator(); i$.hasNext(); connectionWindow -= head) {
               Http2Stream child = (Http2Stream)i$.next();
               state = state(child);
               tail = state.streamableBytes();
               if (tail > 0 || state.hasFrame()) {
                  state.allocate(tail);
                  writeChildNode(state);
                  bytesAllocated += tail;
                  connectionWindow -= tail;
               }

               head = this.writeChildren(child, connectionWindow);
               bytesAllocated += head;
            }

            return bytesAllocated;
         } else {
            Http2Stream[] children = (Http2Stream[])parent.children().toArray(new Http2Stream[parent.numChildren()]);
            Arrays.sort(children, WEIGHT_ORDER);
            int totalWeight = parent.totalChildWeights();

            int nextTail;
            for(tail = children.length; tail > 0; tail = nextTail) {
               head = 0;
               nextTail = 0;
               int nextTotalWeight = 0;

               int nextConnectionWindow;
               for(nextConnectionWindow = connectionWindow; head < tail && nextConnectionWindow > 0; ++head) {
                  Http2Stream child = children[head];
                  state = state(child);
                  int weight = child.weight();
                  double weightRatio = (double)weight / (double)totalWeight;
                  int bytesForTree = Math.min(nextConnectionWindow, (int)Math.ceil((double)connectionWindow * weightRatio));
                  int bytesForChild = Math.min(state.streamableBytes(), bytesForTree);
                  if (bytesForChild > 0 || state.hasFrame()) {
                     state.allocate(bytesForChild);
                     bytesAllocated += bytesForChild;
                     nextConnectionWindow -= bytesForChild;
                     bytesForTree -= bytesForChild;
                     if (state.streamableBytesForTree() - bytesForChild > 0) {
                        children[nextTail++] = child;
                        nextTotalWeight += weight;
                     }

                     if (state.streamableBytes() - bytesForChild == 0) {
                        writeChildNode(state);
                     }
                  }

                  if (bytesForTree > 0) {
                     int childBytesAllocated = this.writeChildren(child, bytesForTree);
                     bytesAllocated += childBytesAllocated;
                     nextConnectionWindow -= childBytesAllocated;
                  }
               }

               connectionWindow = nextConnectionWindow;
               totalWeight = nextTotalWeight;
            }

            return bytesAllocated;
         }
      }
   }

   private static void writeChildNode(DefaultHttp2RemoteFlowController.FlowState state) {
      state.writeBytes(state.allocated());
      state.resetAllocated();
   }

   // $FF: synthetic method
   static boolean access$476(DefaultHttp2RemoteFlowController x0, int x1) {
      return x0.needFlush = (boolean)((byte)(x0.needFlush | x1));
   }

   final class FlowState {
      private final Queue<DefaultHttp2RemoteFlowController.FlowState.Frame> pendingWriteQueue;
      private final Http2Stream stream;
      private int window;
      private int pendingBytes;
      private int streamableBytesForTree;
      private int allocated;

      FlowState(Http2Stream stream, int initialWindowSize) {
         this.stream = stream;
         this.window(initialWindowSize);
         this.pendingWriteQueue = new ArrayDeque(2);
      }

      int window() {
         return this.window;
      }

      void window(int initialWindowSize) {
         this.window = initialWindowSize;
      }

      void allocate(int bytes) {
         this.allocated += bytes;
      }

      int allocated() {
         return this.allocated;
      }

      void resetAllocated() {
         this.allocated = 0;
      }

      int incrementStreamWindow(int delta) throws Http2Exception {
         if (delta > 0 && Integer.MAX_VALUE - delta < this.window) {
            throw Http2Exception.streamError(this.stream.id(), Http2Error.FLOW_CONTROL_ERROR, "Window size overflow for stream: %d", this.stream.id());
         } else {
            int previouslyStreamable = this.streamableBytes();
            this.window += delta;
            int streamableDelta = this.streamableBytes() - previouslyStreamable;
            if (streamableDelta != 0) {
               this.incrementStreamableBytesForTree(streamableDelta);
            }

            return this.window;
         }
      }

      int writableWindow() {
         return Math.min(this.window, DefaultHttp2RemoteFlowController.this.connectionWindow());
      }

      int streamableBytes() {
         return Math.max(0, Math.min(this.pendingBytes, this.window));
      }

      int streamableBytesForTree() {
         return this.streamableBytesForTree;
      }

      DefaultHttp2RemoteFlowController.FlowState.Frame newFrame(Http2RemoteFlowController.FlowControlled payload) {
         DefaultHttp2RemoteFlowController.FlowState.Frame frame = new DefaultHttp2RemoteFlowController.FlowState.Frame(payload);
         this.pendingWriteQueue.offer(frame);
         return frame;
      }

      boolean hasFrame() {
         return !this.pendingWriteQueue.isEmpty();
      }

      DefaultHttp2RemoteFlowController.FlowState.Frame peek() {
         return (DefaultHttp2RemoteFlowController.FlowState.Frame)this.pendingWriteQueue.peek();
      }

      void clear() {
         while(true) {
            DefaultHttp2RemoteFlowController.FlowState.Frame frame = (DefaultHttp2RemoteFlowController.FlowState.Frame)this.pendingWriteQueue.poll();
            if (frame == null) {
               return;
            }

            frame.writeError(Http2Exception.streamError(this.stream.id(), Http2Error.INTERNAL_ERROR, "Stream closed before write could take place"));
         }
      }

      int writeBytes(int bytes) {
         int bytesAttempted = 0;

         while(this.hasFrame()) {
            int maxBytes = Math.min(bytes - bytesAttempted, this.writableWindow());
            bytesAttempted += this.peek().write(maxBytes);
            if (bytes - bytesAttempted <= 0) {
               break;
            }
         }

         return bytesAttempted;
      }

      void incrementStreamableBytesForTree(int numBytes) {
         this.streamableBytesForTree += numBytes;
         if (!this.stream.isRoot()) {
            DefaultHttp2RemoteFlowController.state(this.stream.parent()).incrementStreamableBytesForTree(numBytes);
         }

      }

      private final class Frame {
         final Http2RemoteFlowController.FlowControlled payload;

         Frame(Http2RemoteFlowController.FlowControlled payload) {
            this.payload = payload;
            this.incrementPendingBytes(payload.size());
         }

         private void incrementPendingBytes(int numBytes) {
            int previouslyStreamable = FlowState.this.streamableBytes();
            FlowState.this.pendingBytes = numBytes;
            int delta = FlowState.this.streamableBytes() - previouslyStreamable;
            if (delta != 0) {
               FlowState.this.incrementStreamableBytesForTree(delta);
            }

         }

         int write(int allowedBytes) {
            int before = this.payload.size();
            DefaultHttp2RemoteFlowController.access$476(DefaultHttp2RemoteFlowController.this, this.payload.write(Math.max(0, allowedBytes)));
            int writtenBytes = before - this.payload.size();

            try {
               DefaultHttp2RemoteFlowController.this.connectionState().incrementStreamWindow(-writtenBytes);
               FlowState.this.incrementStreamWindow(-writtenBytes);
            } catch (Http2Exception var5) {
               throw new RuntimeException("Invalid window state when writing frame: " + var5.getMessage(), var5);
            }

            this.decrementPendingBytes(writtenBytes);
            if (this.payload.size() == 0) {
               FlowState.this.pendingWriteQueue.remove();
            }

            return writtenBytes;
         }

         void writeError(Http2Exception cause) {
            this.decrementPendingBytes(this.payload.size());
            this.payload.error(cause);
         }

         void decrementPendingBytes(int bytes) {
            this.incrementPendingBytes(-bytes);
         }
      }
   }
}
