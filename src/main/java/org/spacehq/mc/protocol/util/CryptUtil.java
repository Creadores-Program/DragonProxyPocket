package org.spacehq.mc.protocol.util;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class CryptUtil {
   public static SecretKey generateSharedKey() {
      try {
         KeyGenerator gen = KeyGenerator.getInstance("AES");
         gen.init(128);
         return gen.generateKey();
      } catch (NoSuchAlgorithmException var1) {
         throw new Error("Failed to generate shared key.", var1);
      }
   }

   public static KeyPair generateKeyPair() {
      try {
         KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
         gen.initialize(1024);
         return gen.generateKeyPair();
      } catch (NoSuchAlgorithmException var1) {
         throw new Error("Failed to generate key pair.", var1);
      }
   }

   public static PublicKey decodePublicKey(byte[] bytes) throws IOException {
      try {
         return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(bytes));
      } catch (GeneralSecurityException var2) {
         throw new IOException("Could not decrypt public key.", var2);
      }
   }

   public static SecretKey decryptSharedKey(PrivateKey privateKey, byte[] sharedKey) {
      return new SecretKeySpec(decryptData(privateKey, sharedKey), "AES");
   }

   public static byte[] encryptData(Key key, byte[] data) {
      return runEncryption(1, key, data);
   }

   public static byte[] decryptData(Key key, byte[] data) {
      return runEncryption(2, key, data);
   }

   private static byte[] runEncryption(int mode, Key key, byte[] data) {
      try {
         Cipher cipher = Cipher.getInstance(key.getAlgorithm());
         cipher.init(mode, key);
         return cipher.doFinal(data);
      } catch (GeneralSecurityException var4) {
         throw new Error("Failed to run encryption.", var4);
      }
   }

   public static byte[] getServerIdHash(String serverId, PublicKey publicKey, SecretKey secretKey) {
      try {
         return encrypt("SHA-1", serverId.getBytes("ISO_8859_1"), secretKey.getEncoded(), publicKey.getEncoded());
      } catch (UnsupportedEncodingException var4) {
         throw new Error("Failed to generate server id hash.", var4);
      }
   }

   private static byte[] encrypt(String encryption, byte[]... data) {
      try {
         MessageDigest digest = MessageDigest.getInstance(encryption);
         byte[][] var3 = data;
         int var4 = data.length;

         for(int var5 = 0; var5 < var4; ++var5) {
            byte[] array = var3[var5];
            digest.update(array);
         }

         return digest.digest();
      } catch (NoSuchAlgorithmException var7) {
         throw new Error("Failed to encrypt data.", var7);
      }
   }
}
