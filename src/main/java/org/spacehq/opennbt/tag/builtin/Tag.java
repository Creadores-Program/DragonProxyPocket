package org.spacehq.opennbt.tag.builtin;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;

public abstract class Tag implements Cloneable {
   private String name;

   public Tag(String name) {
      this.name = name;
   }

   public final String getName() {
      return this.name;
   }

   public abstract Object getValue();

   public abstract void read(DataInputStream var1) throws IOException;

   public abstract void write(DataOutputStream var1) throws IOException;

   public abstract Tag clone();

   public boolean equals(Object obj) {
      if (!(obj instanceof Tag)) {
         return false;
      } else {
         Tag tag = (Tag)obj;
         if (!this.getName().equals(tag.getName())) {
            return false;
         } else if (this.getValue() == null) {
            return tag.getValue() == null;
         } else if (tag.getValue() == null) {
            return false;
         } else if (this.getValue().getClass().isArray() && tag.getValue().getClass().isArray()) {
            int length = Array.getLength(this.getValue());
            if (Array.getLength(tag.getValue()) != length) {
               return false;
            } else {
               for(int index = 0; index < length; ++index) {
                  Object o = Array.get(this.getValue(), index);
                  Object other = Array.get(tag.getValue(), index);
                  if (o == null && other != null || o != null && !o.equals(other)) {
                     return false;
                  }
               }

               return true;
            }
         } else {
            return this.getValue().equals(tag.getValue());
         }
      }
   }

   public String toString() {
      String name = this.getName() != null && !this.getName().equals("") ? "(" + this.getName() + ")" : "";
      String value = "";
      if (this.getValue() != null) {
         value = this.getValue().toString();
         if (this.getValue().getClass().isArray()) {
            StringBuilder build = new StringBuilder();
            build.append("[");

            for(int index = 0; index < Array.getLength(this.getValue()); ++index) {
               if (index > 0) {
                  build.append(", ");
               }

               build.append(Array.get(this.getValue(), index));
            }

            build.append("]");
            value = build.toString();
         }
      }

      return this.getClass().getSimpleName() + name + " { " + value + " }";
   }
}
