package org.spacehq.opennbt.tag.builtin;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.spacehq.opennbt.tag.TagCreateException;
import org.spacehq.opennbt.tag.TagRegistry;

public class ListTag extends Tag implements Iterable<Tag> {
   private Class<? extends Tag> type;
   private List<Tag> value;

   private ListTag(String name) {
      super(name);
   }

   public ListTag(String name, Class<? extends Tag> type) {
      super(name);
      this.type = type;
      this.value = new ArrayList();
   }

   public ListTag(String name, List<Tag> value) throws IllegalArgumentException {
      super(name);
      Class<? extends Tag> type = null;
      Iterator var4 = value.iterator();

      while(var4.hasNext()) {
         Tag tag = (Tag)var4.next();
         if (tag == null) {
            throw new IllegalArgumentException("List cannot contain null tags.");
         }

         if (type == null) {
            type = tag.getClass();
         } else if (tag.getClass() != type) {
            throw new IllegalArgumentException("All tags must be of the same type.");
         }
      }

      this.type = type;
      this.value = new ArrayList(value);
   }

   public List<Tag> getValue() {
      return new ArrayList(this.value);
   }

   public void setValue(List<Tag> value) {
      Iterator var2 = value.iterator();

      Tag tag;
      do {
         if (!var2.hasNext()) {
            this.value = new ArrayList(value);
            return;
         }

         tag = (Tag)var2.next();
      } while(tag.getClass() == this.type);

      throw new IllegalArgumentException("Tag type cannot differ from ListTag type.");
   }

   public Class<? extends Tag> getElementType() {
      return this.type;
   }

   public boolean add(Tag tag) {
      if (tag.getClass() != this.type) {
         throw new IllegalArgumentException("Tag type cannot differ from ListTag type.");
      } else {
         return this.value.add(tag);
      }
   }

   public boolean remove(Tag tag) {
      return this.value.remove(tag);
   }

   public <T extends Tag> T get(int index) {
      return (Tag)this.value.get(index);
   }

   public int size() {
      return this.value.size();
   }

   public Iterator<Tag> iterator() {
      return this.value.iterator();
   }

   public void read(DataInputStream in) throws IOException {
      int id = in.readUnsignedByte();
      this.type = TagRegistry.getClassFor(id);
      this.value = new ArrayList();
      if (id != 0 && this.type == null) {
         throw new IOException("Unknown tag ID in ListTag: " + id);
      } else {
         int count = in.readInt();

         for(int index = 0; index < count; ++index) {
            Tag tag = null;

            try {
               tag = TagRegistry.createInstance(id, "");
            } catch (TagCreateException var7) {
               throw new IOException("Failed to create tag.", var7);
            }

            tag.read(in);
            this.add(tag);
         }

      }
   }

   public void write(DataOutputStream out) throws IOException {
      if (this.value.isEmpty()) {
         out.writeByte(0);
      } else {
         int id = TagRegistry.getIdFor(this.type);
         if (id == -1) {
            throw new IOException("ListTag contains unregistered tag class.");
         }

         out.writeByte(id);
      }

      out.writeInt(this.value.size());
      Iterator var4 = this.value.iterator();

      while(var4.hasNext()) {
         Tag tag = (Tag)var4.next();
         tag.write(out);
      }

   }

   public ListTag clone() {
      List<Tag> newList = new ArrayList();
      Iterator var2 = this.value.iterator();

      while(var2.hasNext()) {
         Tag value = (Tag)var2.next();
         newList.add(value.clone());
      }

      return new ListTag(this.getName(), newList);
   }
}
