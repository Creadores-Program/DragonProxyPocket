package io.netty.handler.ssl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

final class ApplicationProtocolUtil {
   private static final int DEFAULT_LIST_SIZE = 2;

   private ApplicationProtocolUtil() {
   }

   static List<String> toList(Iterable<String> protocols) {
      return toList(2, (Iterable)protocols);
   }

   static List<String> toList(int initialListSize, Iterable<String> protocols) {
      if (protocols == null) {
         return null;
      } else {
         List<String> result = new ArrayList(initialListSize);
         Iterator i$ = protocols.iterator();

         while(i$.hasNext()) {
            String p = (String)i$.next();
            if (p == null || p.isEmpty()) {
               throw new IllegalArgumentException("protocol cannot be null or empty");
            }

            result.add(p);
         }

         if (result.isEmpty()) {
            throw new IllegalArgumentException("protocols cannot empty");
         } else {
            return result;
         }
      }
   }

   static List<String> toList(String... protocols) {
      return toList(2, (String[])protocols);
   }

   static List<String> toList(int initialListSize, String... protocols) {
      if (protocols == null) {
         return null;
      } else {
         List<String> result = new ArrayList(initialListSize);
         String[] arr$ = protocols;
         int len$ = protocols.length;

         for(int i$ = 0; i$ < len$; ++i$) {
            String p = arr$[i$];
            if (p == null || p.isEmpty()) {
               throw new IllegalArgumentException("protocol cannot be null or empty");
            }

            result.add(p);
         }

         if (result.isEmpty()) {
            throw new IllegalArgumentException("protocols cannot empty");
         } else {
            return result;
         }
      }
   }
}
