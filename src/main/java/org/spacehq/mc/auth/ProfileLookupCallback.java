package org.spacehq.mc.auth;

public interface ProfileLookupCallback {
   void onProfileLookupSucceeded(GameProfile var1);

   void onProfileLookupFailed(GameProfile var1, Exception var2);
}
