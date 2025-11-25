package org.dragonet.proxy.nbt.tag;

import java.io.IOException;
import java.io.PrintStream;
import org.dragonet.proxy.nbt.stream.NBTInputStream;
import org.dragonet.proxy.nbt.stream.NBTOutputStream;

public abstract class Tag {
   public static final byte TAG_End = 0;
   public static final byte TAG_Byte = 1;
   public static final byte TAG_Short = 2;
   public static final byte TAG_Int = 3;
   public static final byte TAG_Long = 4;
   public static final byte TAG_Float = 5;
   public static final byte TAG_Double = 6;
   public static final byte TAG_Byte_Array = 7;
   public static final byte TAG_String = 8;
   public static final byte TAG_List = 9;
   public static final byte TAG_Compound = 10;
   public static final byte TAG_Int_Array = 11;
   private String name;

   abstract void write(NBTOutputStream var1) throws IOException;

   abstract void load(NBTInputStream var1) throws IOException;

   public abstract String toString();

   public abstract byte getId();

   protected Tag(String name) {
      if (name == null) {
         this.name = "";
      } else {
         this.name = name;
      }

   }

   public boolean equals(Object obj) {
      if (obj != null && obj instanceof Tag) {
         Tag o = (Tag)obj;
         return this.getId() == o.getId() && (this.name != null || o.name == null) && (this.name == null || o.name != null) && (this.name == null || this.name.equals(o.name));
      } else {
         return false;
      }
   }

   public void print(PrintStream out) {
      this.print("", out);
   }

   public void print(String prefix, PrintStream out) {
      String name = this.getName();
      out.print(prefix);
      out.print(getTagName(this.getId()));
      if (name.length() > 0) {
         out.print("(\"" + name + "\")");
      }

      out.print(": ");
      out.println(this.toString());
   }

   public Tag setName(String name) {
      if (name == null) {
         this.name = "";
      } else {
         this.name = name;
      }

      return this;
   }

   public String getName() {
      return this.name == null ? "" : this.name;
   }

   public static Tag readNamedTag(NBTInputStream dis) throws IOException {
      byte type = dis.readByte();
      if (type == 0) {
         return new EndTag();
      } else {
         String name = dis.readUTF();
         Tag tag = newTag(type, name);
         tag.load(dis);
         return tag;
      }
   }

   public static void writeNamedTag(Tag tag, NBTOutputStream dos) throws IOException {
      dos.writeByte(tag.getId());
      if (tag.getId() != 0) {
         dos.writeUTF(tag.getName());
         tag.write(dos);
      }
   }

   public static Tag newTag(byte type, String name) {
      switch(type) {
      case 0:
         return new EndTag();
      case 1:
         return new ByteTag(name);
      case 2:
         return new ShortTag(name);
      case 3:
         return new IntTag(name);
      case 4:
         return new LongTag(name);
      case 5:
         return new FloatTag(name);
      case 6:
         return new DoubleTag(name);
      case 7:
         return new ByteArrayTag(name);
      case 8:
         return new StringTag(name);
      case 9:
         return new ListTag(name);
      case 10:
         return new CompoundTag(name);
      case 11:
         return new IntArrayTag(name);
      default:
         return new EndTag();
      }
   }

   public static String getTagName(byte type) {
      switch(type) {
      case 0:
         return "TAG_End";
      case 1:
         return "TAG_Byte";
      case 2:
         return "TAG_Short";
      case 3:
         return "TAG_Int";
      case 4:
         return "TAG_Long";
      case 5:
         return "TAG_Float";
      case 6:
         return "TAG_Double";
      case 7:
         return "TAG_Byte_Array";
      case 8:
         return "TAG_String";
      case 9:
         return "TAG_List";
      case 10:
         return "TAG_Compound";
      case 11:
         return "TAG_Int_Array";
      default:
         return "UNKNOWN";
      }
   }

   public abstract Tag copy();
}
