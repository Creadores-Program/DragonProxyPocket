package org.spacehq.mc.protocol.data.game.attribute;

import java.util.UUID;
import org.spacehq.mc.protocol.data.game.values.MagicValues;
import org.spacehq.mc.protocol.data.game.values.entity.ModifierOperation;
import org.spacehq.mc.protocol.data.game.values.entity.ModifierType;

public class AttributeModifier {
   private ModifierType type;
   private UUID uuid;
   private double amount;
   private ModifierOperation operation;

   public AttributeModifier(ModifierType type, double amount, ModifierOperation operation) {
      this.type = type;
      this.uuid = (UUID)MagicValues.value(UUID.class, type);
      this.amount = amount;
      this.operation = operation;
   }

   public AttributeModifier(UUID uuid, double amount, ModifierOperation operation) {
      this.type = (ModifierType)MagicValues.key(ModifierType.class, uuid);
      this.uuid = uuid;
      this.amount = amount;
      this.operation = operation;
   }

   public ModifierType getType() {
      return this.type;
   }

   public UUID getUUID() {
      return this.uuid;
   }

   public double getAmount() {
      return this.amount;
   }

   public ModifierOperation getOperation() {
      return this.operation;
   }

   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         AttributeModifier that = (AttributeModifier)o;
         if (Double.compare(that.amount, this.amount) != 0) {
            return false;
         } else if (this.operation != that.operation) {
            return false;
         } else {
            return this.type == that.type;
         }
      } else {
         return false;
      }
   }

   public int hashCode() {
      int result = this.type.hashCode();
      long temp = Double.doubleToLongBits(this.amount);
      result = 31 * result + (int)(temp ^ temp >>> 32);
      result = 31 * result + this.operation.hashCode();
      return result;
   }
}
