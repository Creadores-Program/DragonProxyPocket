package io.netty.channel;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ChannelHandlerAppender extends ChannelHandlerAdapter {
   private final boolean selfRemoval;
   private final List<ChannelHandlerAppender.Entry> handlers;
   private boolean added;

   protected ChannelHandlerAppender() {
      this(true);
   }

   protected ChannelHandlerAppender(boolean selfRemoval) {
      this.handlers = new ArrayList();
      this.selfRemoval = selfRemoval;
   }

   public ChannelHandlerAppender(Iterable<? extends ChannelHandler> handlers) {
      this(true, handlers);
   }

   public ChannelHandlerAppender(ChannelHandler... handlers) {
      this(true, handlers);
   }

   public ChannelHandlerAppender(boolean selfRemoval, Iterable<? extends ChannelHandler> handlers) {
      this.handlers = new ArrayList();
      this.selfRemoval = selfRemoval;
      this.add(handlers);
   }

   public ChannelHandlerAppender(boolean selfRemoval, ChannelHandler... handlers) {
      this.handlers = new ArrayList();
      this.selfRemoval = selfRemoval;
      this.add(handlers);
   }

   protected final ChannelHandlerAppender add(String name, ChannelHandler handler) {
      if (handler == null) {
         throw new NullPointerException("handler");
      } else if (this.added) {
         throw new IllegalStateException("added to the pipeline already");
      } else {
         this.handlers.add(new ChannelHandlerAppender.Entry(name, handler));
         return this;
      }
   }

   protected final ChannelHandlerAppender add(ChannelHandler handler) {
      return this.add((String)null, handler);
   }

   protected final ChannelHandlerAppender add(Iterable<? extends ChannelHandler> handlers) {
      if (handlers == null) {
         throw new NullPointerException("handlers");
      } else {
         Iterator i$ = handlers.iterator();

         while(i$.hasNext()) {
            ChannelHandler h = (ChannelHandler)i$.next();
            if (h == null) {
               break;
            }

            this.add(h);
         }

         return this;
      }
   }

   protected final ChannelHandlerAppender add(ChannelHandler... handlers) {
      if (handlers == null) {
         throw new NullPointerException("handlers");
      } else {
         ChannelHandler[] arr$ = handlers;
         int len$ = handlers.length;

         for(int i$ = 0; i$ < len$; ++i$) {
            ChannelHandler h = arr$[i$];
            if (h == null) {
               break;
            }

            this.add(h);
         }

         return this;
      }
   }

   protected final <T extends ChannelHandler> T handlerAt(int index) {
      return ((ChannelHandlerAppender.Entry)this.handlers.get(index)).handler;
   }

   public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
      this.added = true;
      AbstractChannelHandlerContext dctx = (AbstractChannelHandlerContext)ctx;
      DefaultChannelPipeline pipeline = (DefaultChannelPipeline)dctx.pipeline();
      String name = dctx.name();

      ChannelHandlerAppender.Entry e;
      String oldName;
      try {
         for(Iterator i$ = this.handlers.iterator(); i$.hasNext(); pipeline.addAfter(dctx.invoker, oldName, name, e.handler)) {
            e = (ChannelHandlerAppender.Entry)i$.next();
            oldName = name;
            if (e.name == null) {
               name = pipeline.generateName(e.handler);
            } else {
               name = e.name;
            }
         }
      } finally {
         if (this.selfRemoval) {
            pipeline.remove((ChannelHandler)this);
         }

      }

   }

   private static final class Entry {
      final String name;
      final ChannelHandler handler;

      Entry(String name, ChannelHandler handler) {
         this.name = name;
         this.handler = handler;
      }
   }
}
