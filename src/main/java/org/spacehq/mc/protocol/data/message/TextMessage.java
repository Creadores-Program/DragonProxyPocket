package org.spacehq.mc.protocol.data.message;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class TextMessage extends Message {
   private String text;

   public TextMessage(String text) {
      this.text = text;
   }

   public String getText() {
      return this.text;
   }

   public TextMessage clone() {
      return (TextMessage)(new TextMessage(this.getText())).setStyle(this.getStyle().clone()).setExtra(this.getExtra());
   }

   public JsonElement toJson() {
      if (this.getStyle().isDefault() && this.getExtra().isEmpty()) {
         return new JsonPrimitive(this.text);
      } else {
         JsonElement e = super.toJson();
         if (e.isJsonObject()) {
            JsonObject json = e.getAsJsonObject();
            json.addProperty("text", this.text);
            return json;
         } else {
            return e;
         }
      }
   }

   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         if (!super.equals(o)) {
            return false;
         } else {
            TextMessage that = (TextMessage)o;
            return this.text.equals(that.text);
         }
      } else {
         return false;
      }
   }

   public int hashCode() {
      int result = super.hashCode();
      result = 31 * result + this.text.hashCode();
      return result;
   }
}
