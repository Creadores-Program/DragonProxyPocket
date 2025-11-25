package org.spacehq.mc.auth.service;

import java.net.Proxy;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.spacehq.mc.auth.data.GameProfile;
import org.spacehq.mc.auth.exception.profile.ProfileNotFoundException;
import org.spacehq.mc.auth.exception.request.RequestException;
import org.spacehq.mc.auth.util.HTTP;

public class ProfileService {
   private static final String BASE_URL = "https://api.mojang.com/profiles/";
   private static final String SEARCH_URL = "https://api.mojang.com/profiles/minecraft";
   private static final int MAX_FAIL_COUNT = 3;
   private static final int DELAY_BETWEEN_PAGES = 100;
   private static final int DELAY_BETWEEN_FAILURES = 750;
   private static final int PROFILES_PER_REQUEST = 100;
   private Proxy proxy;

   public ProfileService() {
      this(Proxy.NO_PROXY);
   }

   public ProfileService(Proxy proxy) {
      if (proxy == null) {
         throw new IllegalArgumentException("Proxy cannot be null.");
      } else {
         this.proxy = proxy;
      }
   }

   public void findProfilesByName(String[] names, ProfileService.ProfileLookupCallback callback) {
      this.findProfilesByName(names, callback, false);
   }

   public void findProfilesByName(String[] names, final ProfileService.ProfileLookupCallback callback, boolean async) {
      final Set<String> criteria = new HashSet();
      String[] var5 = names;
      int var6 = names.length;

      for(int var7 = 0; var7 < var6; ++var7) {
         String name = var5[var7];
         if (name != null && !name.isEmpty()) {
            criteria.add(name.toLowerCase());
         }
      }

      Runnable runnable = new Runnable() {
         public void run() {
            Iterator var1 = ProfileService.partition(criteria, 100).iterator();

            while(var1.hasNext()) {
               Set<String> request = (Set)var1.next();
               Exception error = null;
               int failCount = 0;
               boolean tryAgain = true;

               while(failCount < 3 && tryAgain) {
                  tryAgain = false;

                  try {
                     GameProfile[] profiles = (GameProfile[])HTTP.makeRequest(ProfileService.this.proxy, "https://api.mojang.com/profiles/minecraft", request, GameProfile[].class);
                     failCount = 0;
                     Set<String> missing = new HashSet(request);
                     GameProfile[] var16 = profiles;
                     int var9 = profiles.length;

                     for(int var10 = 0; var10 < var9; ++var10) {
                        GameProfile profile = var16[var10];
                        missing.remove(profile.getName().toLowerCase());
                        callback.onProfileLookupSucceeded(profile);
                     }

                     Iterator var17 = missing.iterator();

                     while(var17.hasNext()) {
                        String name = (String)var17.next();
                        callback.onProfileLookupFailed(new GameProfile((UUID)null, name), new ProfileNotFoundException("Server could not find the requested profile."));
                     }

                     try {
                        Thread.sleep(100L);
                     } catch (InterruptedException var13) {
                     }
                  } catch (RequestException var14) {
                     error = var14;
                     ++failCount;
                     if (failCount >= 3) {
                        Iterator var7 = request.iterator();

                        while(var7.hasNext()) {
                           String namex = (String)var7.next();
                           callback.onProfileLookupFailed(new GameProfile((UUID)null, namex), error);
                        }
                     } else {
                        try {
                           Thread.sleep(750L);
                        } catch (InterruptedException var12) {
                        }

                        tryAgain = true;
                     }
                  }
               }
            }

         }
      };
      if (async) {
         (new Thread(runnable, "ProfileLookupThread")).start();
      } else {
         runnable.run();
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

   public interface ProfileLookupCallback {
      void onProfileLookupSucceeded(GameProfile var1);

      void onProfileLookupFailed(GameProfile var1, Exception var2);
   }
}
