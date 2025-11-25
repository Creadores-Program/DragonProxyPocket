package org.spacehq.mc.auth.data;

import java.security.PublicKey;
import java.security.Signature;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.spacehq.mc.auth.exception.property.SignatureValidateException;
import org.spacehq.mc.auth.util.Base64;

public class GameProfile {
   private UUID id;
   private String name;
   private List<GameProfile.Property> properties;
   private Map<GameProfile.TextureType, GameProfile.Texture> textures;

   public GameProfile(String id, String name) {
      this(id != null && !id.equals("") ? UUID.fromString(id) : null, name);
   }

   public GameProfile(UUID id, String name) {
      if (id != null || name != null && !name.equals("")) {
         this.id = id;
         this.name = name;
      } else {
         throw new IllegalArgumentException("Name and ID cannot both be blank");
      }
   }

   public boolean isComplete() {
      return this.id != null && this.name != null && !this.name.equals("");
   }

   public UUID getId() {
      return this.id;
   }

   public String getIdAsString() {
      return this.id != null ? this.id.toString() : "";
   }

   public String getName() {
      return this.name;
   }

   public List<GameProfile.Property> getProperties() {
      if (this.properties == null) {
         this.properties = new ArrayList();
      }

      return this.properties;
   }

   public GameProfile.Property getProperty(String name) {
      Iterator var2 = this.getProperties().iterator();

      GameProfile.Property property;
      do {
         if (!var2.hasNext()) {
            return null;
         }

         property = (GameProfile.Property)var2.next();
      } while(!property.getName().equals(name));

      return property;
   }

   public Map<GameProfile.TextureType, GameProfile.Texture> getTextures() {
      if (this.textures == null) {
         this.textures = new HashMap();
      }

      return this.textures;
   }

   public GameProfile.Texture getTexture(GameProfile.TextureType type) {
      return (GameProfile.Texture)this.getTextures().get(type);
   }

   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         boolean var10000;
         label44: {
            label30: {
               GameProfile that = (GameProfile)o;
               if (this.id != null) {
                  if (!this.id.equals(that.id)) {
                     break label30;
                  }
               } else if (that.id != null) {
                  break label30;
               }

               if (this.name != null) {
                  if (this.name.equals(that.name)) {
                     break label44;
                  }
               } else if (that.name == null) {
                  break label44;
               }
            }

            var10000 = false;
            return var10000;
         }

         var10000 = true;
         return var10000;
      } else {
         return false;
      }
   }

   public int hashCode() {
      int result = this.id != null ? this.id.hashCode() : 0;
      result = 31 * result + (this.name != null ? this.name.hashCode() : 0);
      return result;
   }

   public String toString() {
      return "GameProfile{id=" + this.id + ", name=" + this.name + ", properties=" + this.getProperties() + ", textures=" + this.getTextures() + "}";
   }

   public static class Texture {
      private String url;
      private Map<String, String> metadata;

      public Texture(String url, Map<String, String> metadata) {
         this.url = url;
         this.metadata = metadata;
      }

      public String getURL() {
         return this.url;
      }

      public GameProfile.TextureModel getModel() {
         String model = this.metadata != null ? (String)this.metadata.get("model") : null;
         return model != null && model.equals("slim") ? GameProfile.TextureModel.SLIM : GameProfile.TextureModel.NORMAL;
      }

      public String getHash() {
         String url = this.url.endsWith("/") ? this.url.substring(0, this.url.length() - 1) : this.url;
         int slash = url.lastIndexOf("/");
         int dot = url.lastIndexOf(".");
         if (dot < slash) {
            dot = url.length();
         }

         return url.substring(slash + 1, dot != -1 ? dot : url.length());
      }

      public String toString() {
         return "ProfileTexture{url=" + this.url + ", model=" + this.getModel() + ", hash=" + this.getHash() + "}";
      }
   }

   public static enum TextureModel {
      NORMAL,
      SLIM;
   }

   public static enum TextureType {
      SKIN,
      CAPE;
   }

   public static class Property {
      private String name;
      private String value;
      private String signature;

      public Property(String name, String value) {
         this(name, value, (String)null);
      }

      public Property(String name, String value, String signature) {
         this.name = name;
         this.value = value;
         this.signature = signature;
      }

      public String getName() {
         return this.name;
      }

      public String getValue() {
         return this.value;
      }

      public String getSignature() {
         return this.signature;
      }

      public boolean hasSignature() {
         return this.signature != null;
      }

      public boolean isSignatureValid(PublicKey key) throws SignatureValidateException {
         if (!this.hasSignature()) {
            return false;
         } else {
            try {
               Signature sig = Signature.getInstance("SHA1withRSA");
               sig.initVerify(key);
               sig.update(this.value.getBytes());
               return sig.verify(Base64.decode(this.signature.getBytes("UTF-8")));
            } catch (Exception var3) {
               throw new SignatureValidateException("Could not validate property signature.", var3);
            }
         }
      }

      public String toString() {
         return "Property{name=" + this.name + ", value=" + this.value + ", signature=" + this.signature + "}";
      }
   }
}
