package org.dragonet.proxy.utilities;

import io.netty.util.CharsetUtil;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class HTTP {
   public static String performGetRequest(String url) {
      if (url == null) {
         throw new IllegalArgumentException("URL cannot be null.");
      } else {
         InputStream in = null;

         try {
            HttpURLConnection connection = (HttpURLConnection)(new URL(url)).openConnection();
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);
            connection.setUseCaches(false);
            connection.setDoInput(true);
            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
               in = connection.getInputStream();
            } else {
               in = connection.getErrorStream();
            }

            if (in == null) {
               Object var21 = null;
               return (String)var21;
            } else {
               int data = true;
               ByteArrayOutputStream bos = new ByteArrayOutputStream();

               int data;
               while((data = in.read()) != -1) {
                  bos.write(data);
               }

               String var6 = new String(bos.toByteArray(), CharsetUtil.UTF_8);
               return var6;
            }
         } catch (Exception var17) {
            var17.printStackTrace();
            Object var3 = null;
            return (String)var3;
         } finally {
            if (in != null) {
               try {
                  in.close();
               } catch (IOException var16) {
               }
            }

         }
      }
   }
}
