package org.spacehq.mc.auth.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import org.spacehq.mc.auth.GameProfile;
import org.spacehq.mc.auth.exception.AuthenticationException;
import org.spacehq.mc.auth.exception.AuthenticationUnavailableException;
import org.spacehq.mc.auth.exception.InvalidCredentialsException;
import org.spacehq.mc.auth.exception.UserMigratedException;
import org.spacehq.mc.auth.properties.PropertyMap;
import org.spacehq.mc.auth.response.ProfileSearchResultsResponse;
import org.spacehq.mc.auth.response.Response;
import org.spacehq.mc.auth.serialize.GameProfileSerializer;
import org.spacehq.mc.auth.serialize.ProfileSearchResultsSerializer;
import org.spacehq.mc.auth.serialize.PropertyMapSerializer;

public class URLUtils {
   private static final Gson GSON;

   public static URL constantURL(String url) {
      try {
         return new URL(url);
      } catch (MalformedURLException var2) {
         throw new Error("Malformed constant url: " + url);
      }
   }

   public static URL concatenateURL(URL url, String query) {
      try {
         return url.getQuery() != null && url.getQuery().length() > 0 ? new URL(url.getProtocol(), url.getHost(), url.getFile() + "&" + query) : new URL(url.getProtocol(), url.getHost(), url.getFile() + "?" + query);
      } catch (MalformedURLException var3) {
         throw new IllegalArgumentException("Concatenated URL was malformed: " + url.toString() + ", " + query);
      }
   }

   public static String buildQuery(Map<String, Object> query) {
      if (query == null) {
         return "";
      } else {
         StringBuilder builder = new StringBuilder();
         Iterator it = query.entrySet().iterator();

         while(it.hasNext()) {
            Entry<String, Object> entry = (Entry)it.next();
            if (builder.length() > 0) {
               builder.append("&");
            }

            try {
               builder.append(URLEncoder.encode((String)entry.getKey(), "UTF-8"));
            } catch (UnsupportedEncodingException var6) {
            }

            if (entry.getValue() != null) {
               builder.append("=");

               try {
                  builder.append(URLEncoder.encode(entry.getValue().toString(), "UTF-8"));
               } catch (UnsupportedEncodingException var5) {
               }
            }
         }

         return builder.toString();
      }
   }

   public static <T extends Response> T makeRequest(Proxy proxy, URL url, Object input, Class<T> clazz) throws AuthenticationException {
      Response result = null;

      try {
         String jsonString = input == null ? performGetRequest(proxy, url) : performPostRequest(proxy, url, GSON.toJson(input), "application/json");
         result = (Response)GSON.fromJson(jsonString, clazz);
      } catch (Exception var6) {
         throw new AuthenticationUnavailableException("Could not make request to auth server.", var6);
      }

      if (result == null) {
         return null;
      } else if (result.getError() != null && !result.getError().equals("")) {
         if (result.getCause() != null && result.getCause().equals("UserMigratedException")) {
            throw new UserMigratedException(result.getErrorMessage());
         } else if (result.getError().equals("ForbiddenOperationException")) {
            throw new InvalidCredentialsException(result.getErrorMessage());
         } else {
            throw new AuthenticationException(result.getErrorMessage());
         }
      } else {
         return result;
      }
   }

   private static HttpURLConnection createUrlConnection(Proxy proxy, URL url) throws IOException {
      if (proxy == null) {
         throw new IllegalArgumentException("Proxy cannot be null.");
      } else if (url == null) {
         throw new IllegalArgumentException("URL cannot be null.");
      } else {
         HttpURLConnection connection = (HttpURLConnection)url.openConnection(proxy);
         connection.setConnectTimeout(15000);
         connection.setReadTimeout(15000);
         connection.setUseCaches(false);
         return connection;
      }
   }

   private static String performPostRequest(Proxy proxy, URL url, String post, String type) throws IOException {
      if (proxy == null) {
         throw new IllegalArgumentException("Proxy cannot be null.");
      } else if (url == null) {
         throw new IllegalArgumentException("URL cannot be null.");
      } else if (post == null) {
         throw new IllegalArgumentException("Post cannot be null.");
      } else if (type == null) {
         throw new IllegalArgumentException("Type cannot be null.");
      } else {
         HttpURLConnection connection = createUrlConnection(proxy, url);
         byte[] bytes = post.getBytes("UTF-8");
         connection.setRequestProperty("Content-Type", type + "; charset=utf-8");
         connection.setRequestProperty("Content-Length", String.valueOf(bytes.length));
         connection.setDoOutput(true);
         OutputStream outputStream = null;

         try {
            outputStream = connection.getOutputStream();
            outputStream.write(bytes);
         } finally {
            IOUtils.closeQuietly(outputStream);
         }

         InputStream inputStream = null;

         String var9;
         try {
            inputStream = connection.getInputStream();
            String var8 = IOUtils.toString(inputStream, "UTF-8");
            return var8;
         } catch (IOException var18) {
            IOUtils.closeQuietly(inputStream);
            inputStream = connection.getErrorStream();
            if (inputStream == null) {
               throw var18;
            }

            var9 = IOUtils.toString(inputStream, "UTF-8");
         } finally {
            IOUtils.closeQuietly(inputStream);
         }

         return var9;
      }
   }

   private static String performGetRequest(Proxy proxy, URL url) throws IOException {
      if (proxy == null) {
         throw new IllegalArgumentException("Proxy cannot be null.");
      } else if (url == null) {
         throw new IllegalArgumentException("URL cannot be null.");
      } else {
         HttpURLConnection connection = createUrlConnection(proxy, url);
         InputStream inputStream = null;

         String var5;
         try {
            inputStream = connection.getInputStream();
            String var4 = IOUtils.toString(inputStream, "UTF-8");
            return var4;
         } catch (IOException var9) {
            IOUtils.closeQuietly(inputStream);
            inputStream = connection.getErrorStream();
            if (inputStream == null) {
               throw var9;
            }

            var5 = IOUtils.toString(inputStream, "UTF-8");
         } finally {
            IOUtils.closeQuietly(inputStream);
         }

         return var5;
      }
   }

   static {
      GsonBuilder builder = new GsonBuilder();
      builder.registerTypeAdapter(GameProfile.class, new GameProfileSerializer());
      builder.registerTypeAdapter(PropertyMap.class, new PropertyMapSerializer());
      builder.registerTypeAdapter(UUID.class, new org.spacehq.mc.auth.serialize.UUIDSerializer());
      builder.registerTypeAdapter(ProfileSearchResultsResponse.class, new ProfileSearchResultsSerializer());
      GSON = builder.create();
   }
}
