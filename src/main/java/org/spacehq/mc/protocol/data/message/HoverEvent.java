package org.spacehq.mc.protocol.data.message;

public class HoverEvent implements Cloneable {
   private HoverAction action;
   private Message value;

   public HoverEvent(HoverAction action, Message value) {
      this.action = action;
      this.value = value;
   }

   public HoverAction getAction() {
      return this.action;
   }

   public Message getValue() {
      return this.value;
   }

   public HoverEvent clone() {
      return new HoverEvent(this.action, this.value.clone());
   }

   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         HoverEvent that = (HoverEvent)o;
         if (this.action != that.action) {
            return false;
         } else {
            return this.value.equals(that.value);
         }
      } else {
         return false;
      }
   }

   public int hashCode() {
      int result = this.action.hashCode();
      result = 31 * result + this.value.hashCode();
      return result;
   }
}
