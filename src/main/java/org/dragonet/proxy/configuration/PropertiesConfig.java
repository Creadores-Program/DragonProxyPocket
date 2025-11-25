package org.dragonet.proxy.configuration;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropertiesConfig {
   private final Properties config;

   public PropertiesConfig(String defaultResourcePath, String fileName, boolean saveDefault) throws IOException {
      Properties defaultConfig = new Properties();
      defaultConfig.load(PropertiesConfig.class.getResourceAsStream(defaultResourcePath));
      this.config = new Properties(defaultConfig);
      File file = new File(fileName);
      if (file.exists()) {
         this.config.load(new FileInputStream(fileName));
      } else if (saveDefault) {
         FileOutputStream fos = new FileOutputStream(fileName);
         InputStream ris = PropertiesConfig.class.getResourceAsStream(defaultResourcePath);
         boolean var8 = true;

         int d;
         while((d = ris.read()) != -1) {
            fos.write(d);
         }

         fos.close();
         ris.close();
      }

   }

   public Properties getConfig() {
      return this.config;
   }
}
