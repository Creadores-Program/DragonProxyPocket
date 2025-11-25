package org.spacehq.mc.auth.request;

import org.spacehq.mc.auth.UserAuthentication;

public class AuthenticationRequest {
   private Agent agent = new Agent("Minecraft", 1);
   private String username;
   private String password;
   private String clientToken;
   private boolean requestUser = true;

   public AuthenticationRequest(UserAuthentication auth, String username, String password) {
      this.username = username;
      this.clientToken = auth.getClientToken();
      this.password = password;
   }
}
