package org.spacehq.mc.auth;

import java.net.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.spacehq.mc.auth.exception.AuthenticationException;
import org.spacehq.mc.auth.exception.InvalidCredentialsException;
import org.spacehq.mc.auth.exception.PropertyDeserializeException;
import org.spacehq.mc.auth.properties.Property;
import org.spacehq.mc.auth.properties.PropertyMap;
import org.spacehq.mc.auth.request.AuthenticationRequest;
import org.spacehq.mc.auth.request.RefreshRequest;
import org.spacehq.mc.auth.response.AuthenticationResponse;
import org.spacehq.mc.auth.response.RefreshResponse;
import org.spacehq.mc.auth.response.User;
import org.spacehq.mc.auth.serialize.UUIDSerializer;
import org.spacehq.mc.auth.util.URLUtils;

public class UserAuthentication {
   private static final String BASE_URL = "https://authserver.mojang.com/";
   private static final URL ROUTE_AUTHENTICATE = URLUtils.constantURL("https://authserver.mojang.com/authenticate");
   private static final URL ROUTE_REFRESH = URLUtils.constantURL("https://authserver.mojang.com/refresh");
   private static final String STORAGE_KEY_PROFILE_NAME = "displayName";
   private static final String STORAGE_KEY_PROFILE_ID = "uuid";
   private static final String STORAGE_KEY_PROFILE_PROPERTIES = "profileProperties";
   private static final String STORAGE_KEY_USER_NAME = "username";
   private static final String STORAGE_KEY_USER_ID = "userid";
   private static final String STORAGE_KEY_USER_PROPERTIES = "userProperties";
   private static final String STORAGE_KEY_ACCESS_TOKEN = "accessToken";
   private String clientToken;
   private Proxy proxy;
   private PropertyMap userProperties;
   private String userId;
   private String username;
   private String password;
   private String accessToken;
   private boolean isOnline;
   private List<GameProfile> profiles;
   private GameProfile selectedProfile;
   private UserType userType;

   public UserAuthentication(String clientToken) {
      this(clientToken, Proxy.NO_PROXY);
   }

   public UserAuthentication(String clientToken, Proxy proxy) {
      this.userProperties = new PropertyMap();
      this.profiles = new ArrayList();
      if (clientToken == null) {
         throw new IllegalArgumentException("ClientToken cannot be null.");
      } else if (proxy == null) {
         throw new IllegalArgumentException("Proxy cannot be null.");
      } else {
         this.clientToken = clientToken;
         this.proxy = proxy;
      }
   }

   public String getClientToken() {
      return this.clientToken;
   }

   public Proxy getProxy() {
      return this.proxy;
   }

   public String getUserID() {
      return this.userId;
   }

   public String getAccessToken() {
      return this.accessToken;
   }

   public List<GameProfile> getAvailableProfiles() {
      return this.profiles;
   }

   public GameProfile getSelectedProfile() {
      return this.selectedProfile;
   }

   public UserType getUserType() {
      return this.isLoggedIn() ? (this.userType == null ? UserType.LEGACY : this.userType) : null;
   }

   public PropertyMap getUserProperties() {
      return this.isLoggedIn() ? new PropertyMap(this.userProperties) : new PropertyMap();
   }

   public boolean isLoggedIn() {
      return this.accessToken != null && !this.accessToken.equals("");
   }

   public boolean canPlayOnline() {
      return this.isLoggedIn() && this.getSelectedProfile() != null && this.isOnline;
   }

   public boolean canLogIn() {
      return !this.canPlayOnline() && this.username != null && !this.username.equals("") && (this.password != null && !this.password.equals("") || this.accessToken != null && !this.accessToken.equals(""));
   }

   public void setUsername(String username) {
      if (this.isLoggedIn() && this.canPlayOnline()) {
         throw new IllegalStateException("Cannot change username whilst logged in & online");
      } else {
         this.username = username;
      }
   }

   public void setPassword(String password) {
      if (this.isLoggedIn() && this.canPlayOnline() && this.password != null && !this.password.equals("")) {
         throw new IllegalStateException("Cannot set password whilst logged in & online");
      } else {
         this.password = password;
      }
   }

   public void setAccessToken(String accessToken) {
      if (this.isLoggedIn() && this.canPlayOnline()) {
         throw new IllegalStateException("Cannot change accessToken whilst logged in & online");
      } else {
         this.accessToken = accessToken;
      }
   }

   public void loadFromStorage(Map<String, Object> credentials) throws PropertyDeserializeException {
      this.logout();
      this.setUsername((String)credentials.get("username"));
      if (credentials.containsKey("userid")) {
         this.userId = (String)credentials.get("userid");
      } else {
         this.userId = this.username;
      }

      String name;
      String value;
      if (credentials.containsKey("userProperties")) {
         try {
            List<Map<String, String>> list = (List)credentials.get("userProperties");
            Iterator var3 = list.iterator();

            while(var3.hasNext()) {
               Map<String, String> propertyMap = (Map)var3.next();
               String name = (String)propertyMap.get("name");
               name = (String)propertyMap.get("value");
               value = (String)propertyMap.get("signature");
               if (value == null) {
                  this.userProperties.put(name, new Property(name, name));
               } else {
                  this.userProperties.put(name, new Property(name, name, value));
               }
            }
         } catch (Throwable var10) {
            throw new PropertyDeserializeException("Couldn't deserialize user properties", var10);
         }
      }

      if (credentials.containsKey("displayName") && credentials.containsKey("uuid")) {
         GameProfile profile = new GameProfile(UUIDSerializer.fromString((String)credentials.get("uuid")), (String)credentials.get("displayName"));
         if (credentials.containsKey("profileProperties")) {
            try {
               List<Map<String, String>> list = (List)credentials.get("profileProperties");
               Iterator var13 = list.iterator();

               while(var13.hasNext()) {
                  Map<String, String> propertyMap = (Map)var13.next();
                  name = (String)propertyMap.get("name");
                  value = (String)propertyMap.get("value");
                  String signature = (String)propertyMap.get("signature");
                  if (signature == null) {
                     profile.getProperties().put(name, new Property(name, value));
                  } else {
                     profile.getProperties().put(name, new Property(name, value, signature));
                  }
               }
            } catch (Throwable var9) {
               throw new PropertyDeserializeException("Couldn't deserialize profile properties", var9);
            }
         }

         this.selectedProfile = profile;
      }

      this.accessToken = (String)credentials.get("accessToken");
   }

   public Map<String, Object> saveForStorage() {
      Map<String, Object> result = new HashMap();
      if (this.username != null) {
         result.put("username", this.username);
      }

      if (this.getUserID() != null) {
         result.put("userid", this.userId);
      }

      if (!this.getUserProperties().isEmpty()) {
         List<Map<String, String>> properties = new ArrayList();
         Iterator var3 = this.getUserProperties().values().iterator();

         while(var3.hasNext()) {
            Property userProperty = (Property)var3.next();
            Map<String, String> property = new HashMap();
            property.put("name", userProperty.getName());
            property.put("value", userProperty.getValue());
            property.put("signature", userProperty.getSignature());
            properties.add(property);
         }

         result.put("userProperties", properties);
      }

      GameProfile selectedProfile = this.getSelectedProfile();
      if (selectedProfile != null) {
         result.put("displayName", selectedProfile.getName());
         result.put("uuid", selectedProfile.getId());
         List<Map<String, String>> properties = new ArrayList();
         Iterator var9 = selectedProfile.getProperties().values().iterator();

         while(var9.hasNext()) {
            Property profileProperty = (Property)var9.next();
            Map<String, String> property = new HashMap();
            property.put("name", profileProperty.getName());
            property.put("value", profileProperty.getValue());
            property.put("signature", profileProperty.getSignature());
            properties.add(property);
         }

         if (!properties.isEmpty()) {
            result.put("profileProperties", properties);
         }
      }

      if (this.accessToken != null && !this.accessToken.equals("")) {
         result.put("accessToken", this.accessToken);
      }

      return result;
   }

   public void login() throws AuthenticationException {
      if (this.username != null && !this.username.equals("")) {
         if (this.accessToken != null && !this.accessToken.equals("")) {
            this.loginWithToken();
         } else {
            if (this.password == null || this.password.equals("")) {
               throw new InvalidCredentialsException("Invalid password");
            }

            this.loginWithPassword();
         }

      } else {
         throw new InvalidCredentialsException("Invalid username");
      }
   }

   private void loginWithPassword() throws AuthenticationException {
      if (this.username != null && !this.username.equals("")) {
         if (this.password != null && !this.password.equals("")) {
            AuthenticationRequest request = new AuthenticationRequest(this, this.username, this.password);
            AuthenticationResponse response = (AuthenticationResponse)URLUtils.makeRequest(this.proxy, ROUTE_AUTHENTICATE, request, AuthenticationResponse.class);
            if (!response.getClientToken().equals(this.getClientToken())) {
               throw new AuthenticationException("Server requested we change our client token. Don't know how to handle this!");
            } else {
               if (response.getSelectedProfile() != null) {
                  this.userType = response.getSelectedProfile().isLegacy() ? UserType.LEGACY : UserType.MOJANG;
               } else if (response.getAvailableProfiles() != null && response.getAvailableProfiles().length != 0) {
                  this.userType = response.getAvailableProfiles()[0].isLegacy() ? UserType.LEGACY : UserType.MOJANG;
               }

               if (response.getUser() != null && response.getUser().getId() != null) {
                  this.userId = response.getUser().getId();
               } else {
                  this.userId = this.username;
               }

               this.isOnline = true;
               this.accessToken = response.getAccessToken();
               this.profiles = Arrays.asList(response.getAvailableProfiles());
               this.selectedProfile = response.getSelectedProfile();
               this.updateProperties(response.getUser());
            }
         } else {
            throw new InvalidCredentialsException("Invalid password");
         }
      } else {
         throw new InvalidCredentialsException("Invalid username");
      }
   }

   private void loginWithToken() throws AuthenticationException {
      if (this.userId == null || this.userId.equals("")) {
         if (this.username == null || this.username.equals("")) {
            throw new InvalidCredentialsException("Invalid uuid & username");
         }

         this.userId = this.username;
      }

      if (this.accessToken != null && !this.accessToken.equals("")) {
         RefreshRequest request = new RefreshRequest(this);
         RefreshResponse response = (RefreshResponse)URLUtils.makeRequest(this.proxy, ROUTE_REFRESH, request, RefreshResponse.class);
         if (!response.getClientToken().equals(this.getClientToken())) {
            throw new AuthenticationException("Server requested we change our client token. Don't know how to handle this!");
         } else {
            if (response.getSelectedProfile() != null) {
               this.userType = response.getSelectedProfile().isLegacy() ? UserType.LEGACY : UserType.MOJANG;
            } else if (response.getAvailableProfiles() != null && response.getAvailableProfiles().length != 0) {
               this.userType = response.getAvailableProfiles()[0].isLegacy() ? UserType.LEGACY : UserType.MOJANG;
            }

            if (response.getUser() != null && response.getUser().getId() != null) {
               this.userId = response.getUser().getId();
            } else {
               this.userId = this.username;
            }

            this.isOnline = true;
            this.accessToken = response.getAccessToken();
            this.profiles = Arrays.asList(response.getAvailableProfiles());
            this.selectedProfile = response.getSelectedProfile();
            this.updateProperties(response.getUser());
         }
      } else {
         throw new InvalidCredentialsException("Invalid access token");
      }
   }

   public void logout() {
      this.password = null;
      this.userId = null;
      this.selectedProfile = null;
      this.userProperties.clear();
      this.accessToken = null;
      this.profiles = null;
      this.isOnline = false;
      this.userType = null;
   }

   public void selectGameProfile(GameProfile profile) throws AuthenticationException {
      if (!this.isLoggedIn()) {
         throw new AuthenticationException("Cannot change game profile whilst not logged in");
      } else if (this.getSelectedProfile() != null) {
         throw new AuthenticationException("Cannot change game profile. You must log out and back in.");
      } else if (profile != null && this.profiles.contains(profile)) {
         RefreshRequest request = new RefreshRequest(this, profile);
         RefreshResponse response = (RefreshResponse)URLUtils.makeRequest(this.proxy, ROUTE_REFRESH, request, RefreshResponse.class);
         if (!response.getClientToken().equals(this.getClientToken())) {
            throw new AuthenticationException("Server requested we change our client token. Don't know how to handle this!");
         } else {
            this.isOnline = true;
            this.accessToken = response.getAccessToken();
            this.selectedProfile = response.getSelectedProfile();
         }
      } else {
         throw new IllegalArgumentException("Invalid profile '" + profile + "'");
      }
   }

   public String toString() {
      return "UserAuthentication{profiles=" + this.profiles + ", selectedProfile=" + this.getSelectedProfile() + ", username=" + this.username + ", isLoggedIn=" + this.isLoggedIn() + ", canPlayOnline=" + this.canPlayOnline() + ", accessToken=" + this.accessToken + ", clientToken=" + this.getClientToken() + "}";
   }

   private void updateProperties(User user) {
      this.userProperties.clear();
      if (user != null && user.getProperties() != null) {
         this.userProperties.putAll(user.getProperties());
      }

   }
}
