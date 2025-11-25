package io.netty.handler.codec.http;

import java.util.Set;

public interface Cookie extends Comparable<Cookie> {
   String name();

   String value();

   void setValue(String var1);

   String rawValue();

   void setRawValue(String var1);

   String domain();

   void setDomain(String var1);

   String path();

   void setPath(String var1);

   String comment();

   void setComment(String var1);

   long maxAge();

   void setMaxAge(long var1);

   int version();

   void setVersion(int var1);

   boolean isSecure();

   void setSecure(boolean var1);

   boolean isHttpOnly();

   void setHttpOnly(boolean var1);

   String commentUrl();

   void setCommentUrl(String var1);

   boolean isDiscard();

   void setDiscard(boolean var1);

   Set<Integer> ports();

   void setPorts(int... var1);

   void setPorts(Iterable<Integer> var1);
}
