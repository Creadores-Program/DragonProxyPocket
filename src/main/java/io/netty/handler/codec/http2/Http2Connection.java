package io.netty.handler.codec.http2;

import java.util.Collection;

public interface Http2Connection {
   void addListener(Http2Connection.Listener var1);

   void removeListener(Http2Connection.Listener var1);

   Http2Stream requireStream(int var1) throws Http2Exception;

   Http2Stream stream(int var1);

   Http2Stream connectionStream();

   int numActiveStreams();

   Collection<Http2Stream> activeStreams();

   void deactivate(Http2Stream var1);

   boolean isServer();

   Http2Connection.Endpoint<Http2LocalFlowController> local();

   Http2Stream createLocalStream(int var1) throws Http2Exception;

   Http2Connection.Endpoint<Http2RemoteFlowController> remote();

   Http2Stream createRemoteStream(int var1) throws Http2Exception;

   boolean goAwayReceived();

   void goAwayReceived(int var1);

   boolean goAwaySent();

   void goAwaySent(int var1);

   boolean isGoAway();

   public interface Endpoint<F extends Http2FlowController> {
      int nextStreamId();

      boolean createdStreamId(int var1);

      boolean acceptingNewStreams();

      Http2Stream createStream(int var1) throws Http2Exception;

      Http2Stream reservePushStream(int var1, Http2Stream var2) throws Http2Exception;

      boolean isServer();

      void allowPushTo(boolean var1);

      boolean allowPushTo();

      int numActiveStreams();

      int maxStreams();

      void maxStreams(int var1);

      int lastStreamCreated();

      int lastKnownStream();

      F flowController();

      void flowController(F var1);

      Http2Connection.Endpoint<? extends Http2FlowController> opposite();
   }

   public interface Listener {
      void streamAdded(Http2Stream var1);

      void streamActive(Http2Stream var1);

      void streamHalfClosed(Http2Stream var1);

      void streamInactive(Http2Stream var1);

      void streamRemoved(Http2Stream var1);

      void priorityTreeParentChanged(Http2Stream var1, Http2Stream var2);

      void priorityTreeParentChanging(Http2Stream var1, Http2Stream var2);

      void onWeightChanged(Http2Stream var1, short var2);

      void goingAway();
   }
}
