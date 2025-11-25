package org.dragonet.proxy.nbt.tag;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.dragonet.proxy.nbt.stream.NBTInputStream;
import org.dragonet.proxy.nbt.stream.NBTOutputStream;

public class CompoundTag extends Tag {
   private Map<String, Tag> tags = new HashMap();

   public CompoundTag() {
      super("");
   }

   public CompoundTag(String name) {
      super(name);
   }

   void write(NBTOutputStream dos) throws IOException {
      Iterator var2 = this.tags.values().iterator();

      while(var2.hasNext()) {
         Tag tag = (Tag)var2.next();
         Tag.writeNamedTag(tag, dos);
      }

      dos.writeByte(0);
   }

   void load(NBTInputStream dis) throws IOException {
      this.tags.clear();

      Tag tag;
      while((tag = Tag.readNamedTag(dis)).getId() != 0) {
         this.tags.put(tag.getName(), tag);
      }

   }

   public Collection<Tag> getAllTags() {
      return this.tags.values();
   }

   public byte getId() {
      return 10;
   }

   public CompoundTag put(String name, Tag tag) {
      this.tags.put(name, tag.setName(name));
      return this;
   }

   public CompoundTag putByte(String name, byte value) {
      this.tags.put(name, new ByteTag(name, value));
      return this;
   }

   public CompoundTag putShort(String name, int value) {
      this.tags.put(name, new ShortTag(name, value));
      return this;
   }

   public CompoundTag putInt(String name, int value) {
      this.tags.put(name, new IntTag(name, value));
      return this;
   }

   public CompoundTag putLong(String name, long value) {
      this.tags.put(name, new LongTag(name, value));
      return this;
   }

   public CompoundTag putFloat(String name, float value) {
      this.tags.put(name, new FloatTag(name, value));
      return this;
   }

   public CompoundTag putDouble(String name, double value) {
      this.tags.put(name, new DoubleTag(name, value));
      return this;
   }

   public CompoundTag putString(String name, String value) {
      this.tags.put(name, new StringTag(name, value));
      return this;
   }

   public CompoundTag putByteArray(String name, byte[] value) {
      this.tags.put(name, new ByteArrayTag(name, value));
      return this;
   }

   public CompoundTag putIntArray(String name, int[] value) {
      this.tags.put(name, new IntArrayTag(name, value));
      return this;
   }

   public CompoundTag putList(ListTag<? extends Tag> listTag) {
      this.tags.put(listTag.getName(), listTag);
      return this;
   }

   public CompoundTag putCompound(String name, CompoundTag value) {
      this.tags.put(name, value.setName(name));
      return this;
   }

   public CompoundTag putBoolean(String string, boolean val) {
      this.putByte(string, (byte)(val ? 1 : 0));
      return this;
   }

   public Tag get(String name) {
      return (Tag)this.tags.get(name);
   }

   public boolean contains(String name) {
      return this.tags.containsKey(name);
   }

   public CompoundTag remove(String name) {
      this.tags.remove(name);
      return this;
   }

   public byte getByte(String name) {
      return !this.tags.containsKey(name) ? 0 : ((NumberTag)this.tags.get(name)).getData().byteValue();
   }

   public int getShort(String name) {
      return !this.tags.containsKey(name) ? 0 : ((NumberTag)this.tags.get(name)).getData().intValue();
   }

   public int getInt(String name) {
      return !this.tags.containsKey(name) ? 0 : ((NumberTag)this.tags.get(name)).getData().intValue();
   }

   public long getLong(String name) {
      return !this.tags.containsKey(name) ? 0L : ((NumberTag)this.tags.get(name)).getData().longValue();
   }

   public float getFloat(String name) {
      return !this.tags.containsKey(name) ? 0.0F : ((NumberTag)this.tags.get(name)).getData().floatValue();
   }

   public double getDouble(String name) {
      return !this.tags.containsKey(name) ? 0.0D : ((NumberTag)this.tags.get(name)).getData().doubleValue();
   }

   public String getString(String name) {
      return !this.tags.containsKey(name) ? "" : ((StringTag)this.tags.get(name)).data;
   }

   public byte[] getByteArray(String name) {
      return !this.tags.containsKey(name) ? new byte[0] : ((ByteArrayTag)this.tags.get(name)).data;
   }

   public int[] getIntArray(String name) {
      return !this.tags.containsKey(name) ? new int[0] : ((IntArrayTag)this.tags.get(name)).data;
   }

   public CompoundTag getCompound(String name) {
      return !this.tags.containsKey(name) ? new CompoundTag(name) : (CompoundTag)this.tags.get(name);
   }

   public ListTag<? extends Tag> getList(String name) {
      return !this.tags.containsKey(name) ? new ListTag(name) : (ListTag)this.tags.get(name);
   }

   public <T extends Tag> ListTag<T> getList(String name, Class<T> type) {
      return this.tags.containsKey(name) ? (ListTag)this.tags.get(name) : new ListTag(name);
   }

   public Map<String, Tag> getTags() {
      return new HashMap(this.tags);
   }

   public boolean getBoolean(String string) {
      return this.getByte(string) != 0;
   }

   public String toString() {
      return "CompoundTag " + this.getName() + " (" + this.tags.size() + " entries)";
   }

   public void print(String prefix, PrintStream out) {
      super.print(prefix, out);
      out.println(prefix + "{");
      String orgPrefix = prefix;
      prefix = prefix + "   ";
      Iterator var4 = this.tags.values().iterator();

      while(var4.hasNext()) {
         Tag tag = (Tag)var4.next();
         tag.print(prefix, out);
      }

      out.println(orgPrefix + "}");
   }

   public boolean isEmpty() {
      return this.tags.isEmpty();
   }

   public CompoundTag copy() {
      CompoundTag tag = new CompoundTag(this.getName());
      Iterator var2 = this.tags.keySet().iterator();

      while(var2.hasNext()) {
         String key = (String)var2.next();
         tag.put(key, ((Tag)this.tags.get(key)).copy());
      }

      return tag;
   }

   public boolean equals(Object obj) {
      if (super.equals(obj)) {
         CompoundTag o = (CompoundTag)obj;
         return this.tags.entrySet().equals(o.tags.entrySet());
      } else {
         return false;
      }
   }
}
