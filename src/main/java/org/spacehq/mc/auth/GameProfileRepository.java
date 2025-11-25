package org.spacehq.mc.auth;

import java.net.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.spacehq.mc.auth.exception.AuthenticationException;
import org.spacehq.mc.auth.exception.ProfileNotFoundException;
import org.spacehq.mc.auth.response.ProfileSearchResultsResponse;
import org.spacehq.mc.auth.util.URLUtils;

public class GameProfileRepository {
   private static final String BASE_URL = "https://api.mojang.com/";
   private static final URL SEARCH_URL = URLUtils.constantURL("https://api.mojang.com/profiles/minecraft");
   private static final int MAX_FAIL_COUNT = 3;
   private static final int DELAY_BETWEEN_PAGES = 100;
   private static final int DELAY_BETWEEN_FAILURES = 750;
   private static final int PROFILES_PER_REQUEST = 100;
   private Proxy proxy;

   public GameProfileRepository() {
      this(Proxy.NO_PROXY);
   }

   public GameProfileRepository(Proxy proxy) {
      if (proxy == null) {
         throw new IllegalArgumentException("Proxy cannot be null.");
      } else {
         this.proxy = proxy;
      }
   }

   public void findProfilesByNames(String[] names, ProfileLookupCallback callback) {
      Set<String> criteria = new HashSet();
      String[] var4 = names;
      int var5 = names.length;

      for(int var6 = 0; var6 < var5; ++var6) {
         String name = var4[var6];
         if (name != null && !name.isEmpty()) {
            criteria.add(name.toLowerCase());
         }
      }

      Iterator var18 = partition(criteria, 100).iterator();

      while(var18.hasNext()) {
         Set<String> request = (Set)var18.next();
         Exception error = null;
         int failCount = 0;
         boolean tryAgain = true;

         while(failCount < 3 && tryAgain) {
            tryAgain = false;

            try {
               ProfileSearchResultsResponse response = (ProfileSearchResultsResponse)URLUtils.makeRequest(this.proxy, SEARCH_URL, request, ProfileSearchResultsResponse.class);
               failCount = 0;
               error = null;
               Set<String> missing = new HashSet(request);
               GameProfile[] var23 = response.getProfiles();
               int var12 = var23.length;

               for(int var13 = 0; var13 < var12; ++var13) {
                  GameProfile profile = var23[var13];
                  missing.remove(profile.getName().toLowerCase());
                  callback.onProfileLookupSucceeded(profile);
               }

               Iterator var24 = missing.iterator();

               while(var24.hasNext()) {
                  String name = (String)var24.next();
                  callback.onProfileLookupFailed(new GameProfile((UUID)null, name), new ProfileNotFoundException("Server could not find the requested profile."));
               }

               try {
                  Thread.sleep(100L);
               } catch (InterruptedException var16) {
               }
            } catch (AuthenticationException var17) {
               error = var17;
               ++failCount;
               if (failCount >= 3) {
                  Iterator var10 = request.iterator();

                  while(var10.hasNext()) {
                     String name = (String)var10.next();
                     callback.onProfileLookupFailed(new GameProfile((UUID)null, name), error);
                  }
               } else {
                  try {
                     Thread.sleep(750L);
                  } catch (InterruptedException var15) {
                  }

                  tryAgain = true;
               }
            }
         }
      }

   }

   private static Set<Set<String>> partition(Set<String> set, int size) {
      List<String> list = new ArrayList(set);
      Set<Set<String>> ret = new HashSet();

      for(int i = 0; i < list.size(); i += size) {
         Set<String> s = new HashSet();
         s.addAll(list.subList(i, Math.min(i + size, list.size())));
         ret.add(s);
      }

      return ret;
   }
}
