package org.spacehq.packetlib.crypt;

public interface PacketEncryption {
   int getDecryptOutputSize(int var1);

   int getEncryptOutputSize(int var1);

   int decrypt(byte[] var1, int var2, int var3, byte[] var4, int var5) throws Exception;

   int encrypt(byte[] var1, int var2, int var3, byte[] var4, int var5) throws Exception;
}
