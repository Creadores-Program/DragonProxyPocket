package org.spacehq.mc.auth.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.util.UUID;
import org.spacehq.mc.auth.exception.request.InvalidCredentialsException;
import org.spacehq.mc.auth.exception.request.RequestException;
import org.spacehq.mc.auth.exception.request.ServiceUnavailableException;
import org.spacehq.mc.auth.exception.request.UserMigratedException;

public class HTTP {
   private static final Gson GSON = (new GsonBuilder()).registerTypeAdapter(UUID.class, new UUIDSerializer()).create();

   private HTTP() {
   }

   public static void makeRequest(Proxy proxy, String url, Object input) throws RequestException {
      makeRequest(proxy, url, input, (Class)null);
   }

   public static <T> T makeRequest(Proxy proxy, String url, Object input, Class<T> clazz) throws RequestException {
      JsonElement response = null;

      try {
         String jsonString = input == null ? performGetRequest(proxy, url) : performPostRequest(proxy, url, GSON.toJson(input), "application/json");
         response = (JsonElement)GSON.fromJson(jsonString, JsonElement.class);
      } catch (Exception var9) {
         throw new ServiceUnavailableException("Could not make request to '" + url + "'.", var9);
      }

      if (response != null) {
         if (response.isJsonObject()) {
            JsonObject object = response.getAsJsonObject();
            if (object.has("error")) {
               String error = object.get("error").getAsString();
               String cause = object.has("cause") ? object.get("cause").getAsString() : "";
               String errorMessage = object.has("errorMessage") ? object.get("errorMessage").getAsString() : "";
               if (!error.equals("")) {
                  if (error.equals("ForbiddenOperationException")) {
                     if (cause != null && cause.equals("UserMigratedException")) {
                        throw new UserMigratedException(errorMessage);
                     }

                     throw new InvalidCredentialsException(errorMessage);
                  }

                  throw new RequestException(errorMessage);
               }
            }
         }

         if (clazz != null) {
            return GSON.fromJson(response, clazz);
         }
      }

      return null;
   }

   private static HttpURLConnection createUrlConnection(Proxy proxy, String url) throws IOException {
      if (proxy == null) {
         throw new IllegalArgumentException("Proxy cannot be null.");
      } else if (url == null) {
         throw new IllegalArgumentException("URL cannot be null.");
      } else {
         HttpURLConnection connection = (HttpURLConnection)(new URL(url)).openConnection(proxy);
         connection.setConnectTimeout(15000);
         connection.setReadTimeout(15000);
         connection.setUseCaches(false);
         return connection;
      }
   }

   private static String performGetRequest(Proxy proxy, String url) throws IOException {
      if (proxy == null) {
         throw new IllegalArgumentException("Proxy cannot be null.");
      } else if (url == null) {
         throw new IllegalArgumentException("URL cannot be null.");
      } else {
         HttpURLConnection connection = createUrlConnection(proxy, url);
         connection.setDoInput(true);
         InputStream in = null;

         try {
            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
               in = connection.getInputStream();
            } else {
               in = connection.getErrorStream();
            }

            if (in == null) {
               String var18 = "";
               return var18;
            } else {
               BufferedReader reader = new BufferedReader(new InputStreamReader(in));
               StringBuilder result = new StringBuilder();
               String line = null;

               while((line = reader.readLine()) != null) {
                  result.append(line).append("\n");
               }

               String var8 = result.toString();
               return var8;
            }
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

   private static String performPostRequest(Proxy proxy, String url, String post, String type) throws IOException {
      if (proxy == null) {
         throw new IllegalArgumentException("Proxy cannot be null.");
      } else if (url == null) {
         throw new IllegalArgumentException("URL cannot be null.");
      } else if (post == null) {
         throw new IllegalArgumentException("Post cannot be null.");
      } else if (type == null) {
         throw new IllegalArgumentException("Type cannot be null.");
      } else {
         byte[] bytes = post.getBytes("UTF-8");
         HttpURLConnection connection = createUrlConnection(proxy, url);
         connection.setRequestProperty("Content-Type", type + "; charset=utf-8");
         connection.setRequestProperty("Content-Length", String.valueOf(bytes.length));
         connection.setDoInput(true);
         connection.setDoOutput(true);
         OutputStream out = null;

         try {
            out = connection.getOutputStream();
            out.write(bytes);
         } finally {
            if (out != null) {
               try {
                  out.close();
               } catch (IOException var28) {
               }
            }

         }

         InputStream in = null;

         String var9;
         try {
            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
               in = connection.getInputStream();
            } else {
               in = connection.getErrorStream();
            }

            if (in != null) {
               BufferedReader reader = new BufferedReader(new InputStreamReader(in));
               StringBuilder result = new StringBuilder();
               String line = null;

               while((line = reader.readLine()) != null) {
                  result.append(line).append("\n");
               }

               String var12 = result.toString();
               return var12;
            }

            var9 = "";
         } finally {
            if (in != null) {
               try {
                  in.close();
               } catch (IOException var29) {
               }
            }

         }

         return var9;
      }
   }
}
