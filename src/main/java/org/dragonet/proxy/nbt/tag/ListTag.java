package org.dragonet.proxy.nbt.tag;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.dragonet.proxy.nbt.stream.NBTInputStream;
import org.dragonet.proxy.nbt.stream.NBTOutputStream;

public class ListTag<T extends Tag> extends Tag {
   private List<T> list = new ArrayList();
   public byte type;

   public ListTag() {
      super("");
   }

   public ListTag(String name) {
      super(name);
   }

   void write(NBTOutputStream dos) throws IOException {
      if (this.list.size() > 0) {
         this.type = ((Tag)this.list.get(0)).getId();
      } else {
         this.type = 1;
      }

      dos.writeByte(this.type);
      dos.writeInt(this.list.size());
      Iterator var2 = this.list.iterator();

      while(var2.hasNext()) {
         T aList = (T) (Tag)var2.next();
         aList.write(dos);
      }

   }

   void load(NBTInputStream dis) throws IOException {
      this.type = dis.readByte();
      int size = dis.readInt();
      this.list = new ArrayList();

      for(int i = 0; i < size; ++i) {
         Tag tag = Tag.newTag(this.type, null);
         tag.load(dis);
         this.list.add((T) tag);
      }

   }

   public byte getId() {
      return 9;
   }

   public String toString() {
      return "ListTag " + this.getName() + " [" + this.list.size() + " entries of type " + Tag.getTagName(this.type) + "]";
   }

   public void print(String prefix, PrintStream out) {
      super.print(prefix, out);
      out.println(prefix + "{");
      String orgPrefix = prefix;
      prefix = prefix + "   ";
      Iterator var4 = this.list.iterator();

      while(var4.hasNext()) {
         T aList = (T) (Tag)var4.next();
         aList.print(prefix, out);
      }

      out.println(orgPrefix + "}");
   }

   public ListTag<T> add(T tag) {
      this.type = tag.getId();
      this.list.add(tag);
      return this;
   }

   public ListTag<T> add(int index, T tag) {
      this.type = tag.getId();
      this.list.add(index, tag);
      return this;
   }

   public T get(int index) {
      return (T) (Tag)this.list.get(index);
   }

   public List<T> getAll() {
      return new ArrayList(this.list);
   }

   public void setAll(List<T> tags) {
      this.list = new ArrayList(tags);
   }

   public void remove(T tag) {
      this.list.remove(tag);
   }

   public void remove(int index) {
      this.list.remove(index);
   }

   public void removeAll(Collection<T> tags) {
      this.list.remove(tags);
   }

   public int size() {
      return this.list.size();
   }

   public Tag copy() {
      ListTag<T> res = new ListTag(this.getName());
      res.type = this.type;
      Iterator var2 = this.list.iterator();

      while(var2.hasNext()) {
         T t = (T) (Tag)var2.next();
         T copy = (T) t.copy();
         res.list.add(copy);
      }

      return res;
   }

   public boolean equals(Object obj) {
      if (super.equals(obj)) {
         ListTag o = (ListTag)obj;
         if (this.type == o.type) {
            return this.list.equals(o.list);
         }
      }

      return false;
   }
}
