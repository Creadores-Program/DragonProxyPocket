package org.dragonet.proxy.utilities;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Base64;
import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;

public class MCPESkin {
   public static final int SINGLE_SKIN_SIZE = 8192;
   public static final int DOUBLE_SKIN_SIZE = 16384;
   public static final String MODEL_STEVE = "Standard_Steve";
   public static final String MODEL_ALEX = "Standard_Alex";
   private byte[] data;
   private String model;

   public MCPESkin(byte[] data) {
      this(data, "Standard_Steve");
   }

   public MCPESkin(InputStream inputStream) {
      this(inputStream, "Standard_Steve");
   }

   public MCPESkin(ImageInputStream inputStream) {
      this(inputStream, "Standard_Steve");
   }

   public MCPESkin(File file) {
      this(file, "Standard_Steve");
   }

   public MCPESkin(URL url) {
      this(url, "Standard_Steve");
   }

   public MCPESkin(BufferedImage image) {
      this(image, "Standard_Steve");
   }

   public MCPESkin(byte[] data, String model) {
      this.data = new byte[8192];
      this.setData(data);
      this.setModel(model);
   }

   public MCPESkin(InputStream inputStream, String model) {
      this.data = new byte[8192];

      BufferedImage image;
      try {
         image = ImageIO.read(inputStream);
      } catch (IOException var5) {
         throw new RuntimeException(var5);
      }

      this.parseBufferedImage(image);
      this.setModel(model);
   }

   public MCPESkin(ImageInputStream inputStream, String model) {
      this.data = new byte[8192];

      BufferedImage image;
      try {
         image = ImageIO.read(inputStream);
      } catch (IOException var5) {
         throw new RuntimeException(var5);
      }

      this.parseBufferedImage(image);
      this.setModel(model);
   }

   public MCPESkin(File file, String model) {
      this.data = new byte[8192];

      BufferedImage image;
      try {
         image = ImageIO.read(file);
      } catch (IOException var5) {
         throw new RuntimeException(var5);
      }

      this.parseBufferedImage(image);
      this.setModel(model);
   }

   public MCPESkin(URL url, String model) {
      this.data = new byte[8192];

      BufferedImage image;
      try {
         image = ImageIO.read(url);
      } catch (IOException var5) {
         throw new RuntimeException(var5);
      }

      this.parseBufferedImage(image);
      this.setModel(model);
   }

   public MCPESkin(BufferedImage image, String model) {
      this.data = new byte[8192];
      this.parseBufferedImage(image);
      this.setModel(model);
   }

   public MCPESkin(String base64) {
      this(Base64.getDecoder().decode(base64));
   }

   public MCPESkin(String base64, String model) {
      this(Base64.getDecoder().decode(base64), model);
   }

   public void parseBufferedImage(BufferedImage image) {
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

      for(int y = 0; y < image.getHeight(); ++y) {
         for(int x = 0; x < image.getWidth(); ++x) {
            Color color = new Color(image.getRGB(x, y), true);
            outputStream.write(color.getRed());
            outputStream.write(color.getGreen());
            outputStream.write(color.getBlue());
            outputStream.write(color.getAlpha());
         }
      }

      image.flush();
      this.setData(outputStream.toByteArray());
   }

   public byte[] getData() {
      return this.data;
   }

   public String getModel() {
      return this.model;
   }

   public void setData(byte[] data) {
      if (data.length != 8192 && data.length != 16384) {
         throw new IllegalArgumentException("Invalid skin");
      } else {
         this.data = data;
      }
   }

   public void setModel(String model) {
      if (model == null || model.trim().isEmpty()) {
         model = "Standard_Steve";
      }

      this.model = model;
   }

   public boolean isValid() {
      return this.data.length == 8192 || this.data.length == 16384;
   }
}
