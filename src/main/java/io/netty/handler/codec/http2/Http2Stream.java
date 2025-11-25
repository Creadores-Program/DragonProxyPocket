package io.netty.handler.codec.http2;

import java.util.Collection;

public interface Http2Stream {
   int id();

   Http2Stream.State state();

   Http2Stream open(boolean var1) throws Http2Exception;

   Http2Stream close();

   Http2Stream closeLocalSide();

   Http2Stream closeRemoteSide();

   boolean isResetSent();

   Http2Stream resetSent();

   boolean remoteSideOpen();

   boolean localSideOpen();

   Object setProperty(Object var1, Object var2);

   <V> V getProperty(Object var1);

   <V> V removeProperty(Object var1);

   Http2Stream setPriority(int var1, short var2, boolean var3) throws Http2Exception;

   boolean isRoot();

   boolean isLeaf();

   short weight();

   int totalChildWeights();

   Http2Stream parent();

   boolean isDescendantOf(Http2Stream var1);

   int numChildren();

   boolean hasChild(int var1);

   Http2Stream child(int var1);

   Collection<? extends Http2Stream> children();

   public static enum State {
      IDLE,
      RESERVED_LOCAL,
      RESERVED_REMOTE,
      OPEN,
      HALF_CLOSED_LOCAL,
      HALF_CLOSED_REMOTE,
      CLOSED;
   }
}
