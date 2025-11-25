package org.spacehq.mc.auth.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Proxy;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.spacehq.mc.auth.data.GameProfile;
import org.spacehq.mc.auth.exception.profile.ProfileException;
import org.spacehq.mc.auth.exception.profile.ProfileLookupException;
import org.spacehq.mc.auth.exception.profile.ProfileNotFoundException;
import org.spacehq.mc.auth.exception.property.ProfileTextureException;
import org.spacehq.mc.auth.exception.property.PropertyException;
import org.spacehq.mc.auth.exception.request.RequestException;
import org.spacehq.mc.auth.util.Base64;
import org.spacehq.mc.auth.util.HTTP;
import org.spacehq.mc.auth.util.UUIDSerializer;

public class SessionService {
   private static final String BASE_URL = "https://sessionserver.mojang.com/session/minecraft/";
   private static final String JOIN_URL = "https://sessionserver.mojang.com/session/minecraft/join";
   private static final String HAS_JOINED_URL = "https://sessionserver.mojang.com/session/minecraft/hasJoined";
   private static final String PROFILE_URL = "https://sessionserver.mojang.com/session/minecraft/profile";
   private static final PublicKey SIGNATURE_KEY;
   private static final Gson GSON;
   private Proxy proxy;

   public SessionService() {
      this(Proxy.NO_PROXY);
   }

   public SessionService(Proxy proxy) {
      if (proxy == null) {
         throw new IllegalArgumentException("Proxy cannot be null.");
      } else {
         this.proxy = proxy;
      }
   }

   public void joinServer(GameProfile profile, String authenticationToken, String serverId) throws RequestException {
      SessionService.JoinServerRequest request = new SessionService.JoinServerRequest(authenticationToken, profile.getId(), serverId);
      HTTP.makeRequest(this.proxy, "https://sessionserver.mojang.com/session/minecraft/join", request, (Class)null);
   }

   public GameProfile getProfileByServer(String name, String serverId) throws RequestException {
      SessionService.HasJoinedResponse response = (SessionService.HasJoinedResponse)HTTP.makeRequest(this.proxy, "https://sessionserver.mojang.com/session/minecraft/hasJoined?username=" + name + "&serverId=" + serverId, (Object)null, SessionService.HasJoinedResponse.class);
      if (response != null && response.id != null) {
         GameProfile result = new GameProfile(response.id, name);
         if (response.properties != null) {
            result.getProperties().addAll(response.properties);
         }

         return result;
      } else {
         return null;
      }
   }

   public GameProfile fillProfileProperties(GameProfile profile) throws ProfileException {
      if (profile.getId() == null) {
         return profile;
      } else {
         try {
            SessionService.MinecraftProfileResponse response = (SessionService.MinecraftProfileResponse)HTTP.makeRequest(this.proxy, "https://sessionserver.mojang.com/session/minecraft/profile/" + UUIDSerializer.fromUUID(profile.getId()) + "?unsigned=false", (Object)null, SessionService.MinecraftProfileResponse.class);
            if (response == null) {
               throw new ProfileNotFoundException("Couldn't fetch profile properties for " + profile + " as the profile does not exist.");
            } else {
               if (response.properties != null) {
                  profile.getProperties().addAll(response.properties);
               }

               return profile;
            }
         } catch (RequestException var3) {
            throw new ProfileLookupException("Couldn't look up profile properties for " + profile + ".", var3);
         }
      }
   }

   public GameProfile fillProfileTextures(GameProfile profile, boolean requireSecure) throws PropertyException {
      GameProfile.Property textures = profile.getProperty("textures");
      if (textures != null) {
         if (!textures.hasSignature()) {
            throw new ProfileTextureException("Signature is missing from textures payload.");
         } else if (!textures.isSignatureValid(SIGNATURE_KEY)) {
            throw new ProfileTextureException("Textures payload has been tampered with. (signature invalid)");
         } else {
            SessionService.MinecraftTexturesPayload result;
            try {
               String json = new String(Base64.decode(textures.getValue().getBytes("UTF-8")));
               result = (SessionService.MinecraftTexturesPayload)GSON.fromJson(json, SessionService.MinecraftTexturesPayload.class);
            } catch (Exception var7) {
               throw new ProfileTextureException("Could not decode texture payload.", var7);
            }

            if (result.profileId != null && result.profileId.equals(profile.getId())) {
               if (result.profileName != null && result.profileName.equals(profile.getName())) {
                  if (requireSecure) {
                     if (result.isPublic) {
                        throw new ProfileTextureException("Decrypted textures payload was public when secure data is required.");
                     }

                     Calendar limit = Calendar.getInstance();
                     limit.add(5, -1);
                     Date validFrom = new Date(result.timestamp);
                     if (validFrom.before(limit.getTime())) {
                        throw new ProfileTextureException("Decrypted textures payload is too old. (" + validFrom + ", needs to be at least " + limit + ")");
                     }
                  }

                  if (result.textures != null) {
                     profile.getTextures().putAll(result.textures);
                  }

                  return profile;
               } else {
                  throw new ProfileTextureException("Decrypted textures payload was for another user. (expected name " + profile.getName() + " but was for " + result.profileName + ")");
               }
            } else {
               throw new ProfileTextureException("Decrypted textures payload was for another user. (expected id " + profile.getId() + " but was for " + result.profileId + ")");
            }
         }
      } else {
         return profile;
      }
   }

   public String toString() {
      return "SessionService{}";
   }

   static {
      InputStream in = null;

      try {
         in = SessionService.class.getResourceAsStream("/yggdrasil_session_pubkey.der");
         ByteArrayOutputStream out = new ByteArrayOutputStream();
         byte[] buffer = new byte[4096];
         boolean var3 = true;

         while(true) {
            int length;
            if ((length = in.read(buffer)) == -1) {
               in.close();
               out.close();
               X509EncodedKeySpec spec = new X509EncodedKeySpec(out.toByteArray());
               KeyFactory keyFactory = KeyFactory.getInstance("RSA");
               SIGNATURE_KEY = keyFactory.generatePublic(spec);
               break;
            }

            out.write(buffer, 0, length);
         }
      } catch (Exception var13) {
         throw new ExceptionInInitializerError("Missing/invalid yggdrasil public key.");
      } finally {
         if (in != null) {
            try {
               in.close();
            } catch (IOException var12) {
            }
         }

      }

      GSON = (new GsonBuilder()).registerTypeAdapter(UUID.class, new UUIDSerializer()).create();
   }

   private static class MinecraftTexturesPayload {
      public long timestamp;
      public UUID profileId;
      public String profileName;
      public boolean isPublic;
      public Map<GameProfile.TextureType, GameProfile.Texture> textures;
   }

   private static class MinecraftProfileResponse {
      public UUID id;
      public String name;
      public List<GameProfile.Property> properties;
   }

   private static class HasJoinedResponse {
      public UUID id;
      public List<GameProfile.Property> properties;
   }

   private static class JoinServerRequest {
      private String accessToken;
      private UUID selectedProfile;
      private String serverId;

      protected JoinServerRequest(String accessToken, UUID selectedProfile, String serverId) {
         this.accessToken = accessToken;
         this.selectedProfile = selectedProfile;
         this.serverId = serverId;
      }
   }
}
