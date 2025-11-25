package org.spacehq.mc.protocol.data.message;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public abstract class Message implements Cloneable {
   private MessageStyle style = new MessageStyle();
   private List<Message> extra = new ArrayList();

   public abstract String getText();

   public String getFullText() {
      StringBuilder build = new StringBuilder(this.getText());
      Iterator var2 = this.extra.iterator();

      while(var2.hasNext()) {
         Message msg = (Message)var2.next();
         build.append(msg.getFullText());
      }

      return build.toString();
   }

   public MessageStyle getStyle() {
      return this.style;
   }

   public List<Message> getExtra() {
      return new ArrayList(this.extra);
   }

   public Message setStyle(MessageStyle style) {
      this.style = style;
      return this;
   }

   public Message setExtra(List<Message> extra) {
      this.extra = new ArrayList(extra);
      Iterator var2 = this.extra.iterator();

      while(var2.hasNext()) {
         Message msg = (Message)var2.next();
         msg.getStyle().setParent(this.style);
      }

      return this;
   }

   public Message addExtra(Message message) {
      this.extra.add(message);
      message.getStyle().setParent(this.style);
      return this;
   }

   public Message removeExtra(Message message) {
      this.extra.remove(message);
      message.getStyle().setParent((MessageStyle)null);
      return this;
   }

   public Message clearExtra() {
      Iterator var1 = this.extra.iterator();

      while(var1.hasNext()) {
         Message msg = (Message)var1.next();
         msg.getStyle().setParent((MessageStyle)null);
      }

      this.extra.clear();
      return this;
   }

   public String toString() {
      return this.getFullText();
   }

   public abstract Message clone();

   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         Message message = (Message)o;
         if (!this.extra.equals(message.extra)) {
            return false;
         } else {
            return this.style.equals(message.style);
         }
      } else {
         return false;
      }
   }

   public int hashCode() {
      int result = this.style.hashCode();
      result = 31 * result + this.extra.hashCode();
      return result;
   }

   public String toJsonString() {
      return this.toJson().toString();
   }

   public JsonElement toJson() {
      JsonObject json = new JsonObject();
      json.addProperty("color", this.style.getColor().toString());
      Iterator var2 = this.style.getFormats().iterator();

      while(var2.hasNext()) {
         ChatFormat format = (ChatFormat)var2.next();
         json.addProperty(format.toString(), true);
      }

      JsonObject hover;
      if (this.style.getClickEvent() != null) {
         hover = new JsonObject();
         hover.addProperty("action", this.style.getClickEvent().getAction().toString());
         hover.addProperty("value", this.style.getClickEvent().getValue());
         json.add("clickEvent", hover);
      }

      if (this.style.getHoverEvent() != null) {
         hover = new JsonObject();
         hover.addProperty("action", this.style.getHoverEvent().getAction().toString());
         hover.add("value", this.style.getHoverEvent().getValue().toJson());
         json.add("hoverEvent", hover);
      }

      if (this.style.getInsertion() != null) {
         json.addProperty("insertion", this.style.getInsertion());
      }

      if (this.extra.size() > 0) {
         JsonArray extra = new JsonArray();
         Iterator var7 = this.extra.iterator();

         while(var7.hasNext()) {
            Message msg = (Message)var7.next();
            extra.add(msg.toJson());
         }

         json.add("extra", extra);
      }

      return json;
   }

   public static Message fromString(String str) {
      try {
         return fromJson((new JsonParser()).parse(str));
      } catch (Exception var2) {
         return new TextMessage(str);
      }
   }

   public static Message fromJson(JsonElement e) {
      if (e.isJsonPrimitive()) {
         return new TextMessage(e.getAsString());
      } else if (!e.isJsonObject()) {
         throw new IllegalArgumentException("Cannot convert " + e.getClass().getSimpleName() + " to a message.");
      } else {
         JsonObject json = e.getAsJsonObject();
         Message msg = null;
         JsonArray extraJson;
         int index;
         if (json.has("text")) {
            msg = new TextMessage(json.get("text").getAsString());
         } else {
            if (!json.has("translate")) {
               throw new IllegalArgumentException("Unknown message type in json: " + json.toString());
            }

            Message[] with = new Message[0];
            if (json.has("with")) {
               extraJson = json.get("with").getAsJsonArray();
               with = new Message[extraJson.size()];

               for(index = 0; index < extraJson.size(); ++index) {
                  JsonElement el = extraJson.get(index);
                  if (el.isJsonPrimitive()) {
                     with[index] = new TextMessage(el.getAsString());
                  } else {
                     with[index] = fromJson(el.getAsJsonObject());
                  }
               }
            }

            msg = new TranslationMessage(json.get("translate").getAsString(), with);
         }

         MessageStyle style = new MessageStyle();
         if (json.has("color")) {
            style.setColor(ChatColor.byName(json.get("color").getAsString()));
         }

         ChatFormat[] var9 = ChatFormat.values();
         index = var9.length;

         int index;
         for(index = 0; index < index; ++index) {
            ChatFormat format = var9[index];
            if (json.has(format.toString()) && json.get(format.toString()).getAsBoolean()) {
               style.addFormat(format);
            }
         }

         JsonObject hover;
         if (json.has("clickEvent")) {
            hover = json.get("clickEvent").getAsJsonObject();
            style.setClickEvent(new ClickEvent(ClickAction.byName(hover.get("action").getAsString()), hover.get("value").getAsString()));
         }

         if (json.has("hoverEvent")) {
            hover = json.get("hoverEvent").getAsJsonObject();
            style.setHoverEvent(new HoverEvent(HoverAction.byName(hover.get("action").getAsString()), fromJson(hover.get("value"))));
         }

         if (json.has("insertion")) {
            style.setInsertion(json.get("insertion").getAsString());
         }

         ((Message)msg).setStyle(style);
         if (json.has("extra")) {
            extraJson = json.get("extra").getAsJsonArray();
            List<Message> extra = new ArrayList();

            for(index = 0; index < extraJson.size(); ++index) {
               JsonElement el = extraJson.get(index);
               if (el.isJsonPrimitive()) {
                  extra.add(new TextMessage(el.getAsString()));
               } else {
                  extra.add(fromJson(el.getAsJsonObject()));
               }
            }

            ((Message)msg).setExtra(extra);
         }

         return (Message)msg;
      }
   }
}
