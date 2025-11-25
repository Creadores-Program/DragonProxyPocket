package org.spacehq.mc.protocol.data.message;

import java.util.ArrayList;
import java.util.List;

public class MessageStyle implements Cloneable {
   private static final MessageStyle DEFAULT = new MessageStyle();
   private ChatColor color;
   private List<ChatFormat> formats;
   private ClickEvent click;
   private HoverEvent hover;
   private String insertion;
   private MessageStyle parent;

   public MessageStyle() {
      this.color = ChatColor.WHITE;
      this.formats = new ArrayList();
      this.parent = DEFAULT;
   }

   public boolean isDefault() {
      return this.equals(DEFAULT);
   }

   public ChatColor getColor() {
      return this.color;
   }

   public List<ChatFormat> getFormats() {
      return new ArrayList(this.formats);
   }

   public ClickEvent getClickEvent() {
      return this.click;
   }

   public HoverEvent getHoverEvent() {
      return this.hover;
   }

   public String getInsertion() {
      return this.insertion;
   }

   public MessageStyle getParent() {
      return this.parent;
   }

   public MessageStyle setColor(ChatColor color) {
      this.color = color;
      return this;
   }

   public MessageStyle setFormats(List<ChatFormat> formats) {
      this.formats = new ArrayList(formats);
      return this;
   }

   public MessageStyle addFormat(ChatFormat format) {
      this.formats.add(format);
      return this;
   }

   public MessageStyle removeFormat(ChatFormat format) {
      this.formats.remove(format);
      return this;
   }

   public MessageStyle clearFormats() {
      this.formats.clear();
      return this;
   }

   public MessageStyle setClickEvent(ClickEvent event) {
      this.click = event;
      return this;
   }

   public MessageStyle setHoverEvent(HoverEvent event) {
      this.hover = event;
      return this;
   }

   public MessageStyle setInsertion(String insertion) {
      this.insertion = insertion;
      return this;
   }

   protected MessageStyle setParent(MessageStyle parent) {
      if (parent == null) {
         parent = DEFAULT;
      }

      this.parent = parent;
      return this;
   }

   public String toString() {
      return "MessageStyle{color=" + this.color + ",formats=" + this.formats + ",clickEvent=" + this.click + ",hoverEvent=" + this.hover + ",insertion=" + this.insertion + "}";
   }

   public MessageStyle clone() {
      return (new MessageStyle()).setParent(this.parent).setColor(this.color).setFormats(this.formats).setClickEvent(this.click != null ? this.click.clone() : null).setHoverEvent(this.hover != null ? this.hover.clone() : null).setInsertion(this.insertion);
   }

   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         MessageStyle style;
         label57: {
            style = (MessageStyle)o;
            if (this.click != null) {
               if (this.click.equals(style.click)) {
                  break label57;
               }
            } else if (style.click == null) {
               break label57;
            }

            return false;
         }

         if (this.color != style.color) {
            return false;
         } else if (!this.formats.equals(style.formats)) {
            return false;
         } else {
            if (this.hover != null) {
               if (!this.hover.equals(style.hover)) {
                  return false;
               }
            } else if (style.hover != null) {
               return false;
            }

            label40: {
               if (this.insertion != null) {
                  if (this.insertion.equals(style.insertion)) {
                     break label40;
                  }
               } else if (style.insertion == null) {
                  break label40;
               }

               return false;
            }

            if (!this.parent.equals(style.parent)) {
               return false;
            } else {
               return true;
            }
         }
      } else {
         return false;
      }
   }

   public int hashCode() {
      int result = this.color != null ? this.color.hashCode() : 0;
      result = 31 * result + this.formats.hashCode();
      result = 31 * result + (this.click != null ? this.click.hashCode() : 0);
      result = 31 * result + (this.hover != null ? this.hover.hashCode() : 0);
      result = 31 * result + (this.insertion != null ? this.insertion.hashCode() : 0);
      result = 31 * result + this.parent.hashCode();
      return result;
   }
}
