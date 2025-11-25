package org.dragonet.proxy.utilities.io;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class SkinDownloader {
   public static byte[] download(String username) {
      try {
         URL url = new URL(String.format("http://s3.amazonaws.com/MinecraftSkins/%s.png", username));
         HttpURLConnection connection = (HttpURLConnection)url.openConnection();
         DataInputStream in = new DataInputStream(connection.getInputStream());
         ByteArrayOutputStream out = new ByteArrayOutputStream();
         byte[] buffer = new byte[4096];
         boolean var6 = false;

         int count;
         while((count = in.read(buffer)) > 0) {
            out.write(buffer, 0, count);
         }

         out.close();
         in.close();
         connection.disconnect();
         return out.toByteArray();
      } catch (Exception var7) {
         return null;
      }
   }
}
