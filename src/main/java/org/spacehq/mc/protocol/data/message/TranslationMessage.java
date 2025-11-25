package org.spacehq.mc.protocol.data.message;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Arrays;

public class TranslationMessage extends Message {
   private String translationKey;
   private Message[] translationParams;

   public TranslationMessage(String translationKey, Message... translationParams) {
      this.translationKey = translationKey;
      this.translationParams = translationParams;
      this.translationParams = this.getTranslationParams();
      Message[] var3 = this.translationParams;
      int var4 = var3.length;

      for(int var5 = 0; var5 < var4; ++var5) {
         Message param = var3[var5];
         param.getStyle().setParent(this.getStyle());
      }

   }

   public String getTranslationKey() {
      return this.translationKey;
   }

   public Message[] getTranslationParams() {
      Message[] copy = (Message[])Arrays.copyOf(this.translationParams, this.translationParams.length);

      for(int index = 0; index < copy.length; ++index) {
         copy[index] = copy[index].clone();
      }

      return copy;
   }

   public Message setStyle(MessageStyle style) {
      super.setStyle(style);
      Message[] var2 = this.translationParams;
      int var3 = var2.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         Message param = var2[var4];
         param.getStyle().setParent(this.getStyle());
      }

      return this;
   }

   public String getText() {
      return this.translationKey;
   }

   public TranslationMessage clone() {
      return (TranslationMessage)(new TranslationMessage(this.getTranslationKey(), this.getTranslationParams())).setStyle(this.getStyle().clone()).setExtra(this.getExtra());
   }

   public JsonElement toJson() {
      JsonElement e = super.toJson();
      if (!e.isJsonObject()) {
         return e;
      } else {
         JsonObject json = e.getAsJsonObject();
         json.addProperty("translate", this.translationKey);
         JsonArray params = new JsonArray();
         Message[] var4 = this.translationParams;
         int var5 = var4.length;

         for(int var6 = 0; var6 < var5; ++var6) {
            Message param = var4[var6];
            params.add(param.toJson());
         }

         json.add("with", params);
         return json;
      }
   }

   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         if (!super.equals(o)) {
            return false;
         } else {
            TranslationMessage that = (TranslationMessage)o;
            if (!this.translationKey.equals(that.translationKey)) {
               return false;
            } else {
               return Arrays.equals(this.translationParams, that.translationParams);
            }
         }
      } else {
         return false;
      }
   }

   public int hashCode() {
      int result = super.hashCode();
      result = 31 * result + this.translationKey.hashCode();
      result = 31 * result + Arrays.hashCode(this.translationParams);
      return result;
   }
}
