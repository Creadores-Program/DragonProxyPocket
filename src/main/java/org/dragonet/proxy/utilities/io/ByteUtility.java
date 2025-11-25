package org.dragonet.proxy.utilities.io;

public class ByteUtility {
   public static String bytesToHexString(byte[] data) {
      StringBuilder sb = new StringBuilder();

      for(int i = 0; i < data.length; ++i) {
         String sTemp = Integer.toHexString(255 & data[i]);
         if (sTemp.length() < 2) {
            sb.append(0);
         }

         sb.append(sTemp.toUpperCase()).append(", ");
      }

      return sb.toString();
   }
}
