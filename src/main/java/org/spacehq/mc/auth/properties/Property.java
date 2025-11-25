package org.spacehq.mc.auth.properties;

import java.security.PublicKey;
import java.security.Signature;
import org.spacehq.mc.auth.exception.SignatureValidateException;
import org.spacehq.mc.auth.util.Base64;

public class Property {
   private String name;
   private String value;
   private String signature;

   public Property(String value, String name) {
      this(value, name, (String)null);
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
