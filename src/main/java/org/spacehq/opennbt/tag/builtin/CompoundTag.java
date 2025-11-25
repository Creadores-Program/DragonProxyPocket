package org.spacehq.opennbt.tag.builtin;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import org.spacehq.opennbt.NBTIO;

public class CompoundTag extends Tag implements Iterable<Tag> {
   private Map<String, Tag> value;

   public CompoundTag(String name) {
      this(name, new LinkedHashMap());
   }

   public CompoundTag(String name, Map<String, Tag> value) {
      super(name);
      this.value = new LinkedHashMap(value);
   }

   public Map<String, Tag> getValue() {
      return new LinkedHashMap(this.value);
   }

   public void setValue(Map<String, Tag> value) {
      this.value = new LinkedHashMap(value);
   }

   public boolean isEmpty() {
      return this.value.isEmpty();
   }

   public boolean contains(String tagName) {
      return this.value.containsKey(tagName);
   }

   public <T extends Tag> T get(String tagName) {
      return (Tag)this.value.get(tagName);
   }

   public <T extends Tag> T put(T tag) {
      return (Tag)this.value.put(tag.getName(), tag);
   }

   public <T extends Tag> T remove(String tagName) {
      return (Tag)this.value.remove(tagName);
   }

   public Set<String> keySet() {
      return this.value.keySet();
   }

   public Collection<Tag> values() {
      return this.value.values();
   }

   public int size() {
      return this.value.size();
   }

   public void clear() {
      this.value.clear();
   }

   public Iterator<Tag> iterator() {
      return this.values().iterator();
   }

   public void read(DataInputStream in) throws IOException {
      ArrayList tags = new ArrayList();

      Tag tag;
      try {
         while((tag = NBTIO.readTag(in)) != null) {
            tags.add(tag);
         }
      } catch (EOFException var5) {
         throw new IOException("Closing EndTag was not found!");
      }

      Iterator var6 = tags.iterator();

      while(var6.hasNext()) {
         Tag tag = (Tag)var6.next();
         this.put(tag);
      }

   }

   public void write(DataOutputStream out) throws IOException {
      Iterator var2 = this.value.values().iterator();

      while(var2.hasNext()) {
         Tag tag = (Tag)var2.next();
         NBTIO.writeTag(out, tag);
      }

      out.writeByte(0);
   }

   public CompoundTag clone() {
      Map<String, Tag> newMap = new LinkedHashMap();
      Iterator var2 = this.value.entrySet().iterator();

      while(var2.hasNext()) {
         Entry<String, Tag> entry = (Entry)var2.next();
         newMap.put(entry.getKey(), ((Tag)entry.getValue()).clone());
      }

      return new CompoundTag(this.getName(), newMap);
   }
}
