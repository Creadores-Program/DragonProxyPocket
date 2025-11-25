package io.netty.handler.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.internal.EmptyArrays;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

public final class AsciiString implements CharSequence, Comparable<CharSequence> {
   public static final AsciiString EMPTY_STRING = new AsciiString("");
   public static final Comparator<AsciiString> CASE_INSENSITIVE_ORDER = new Comparator<AsciiString>() {
      public int compare(AsciiString o1, AsciiString o2) {
         return AsciiString.CHARSEQUENCE_CASE_INSENSITIVE_ORDER.compare(o1, o2);
      }
   };
   public static final Comparator<AsciiString> CASE_SENSITIVE_ORDER = new Comparator<AsciiString>() {
      public int compare(AsciiString o1, AsciiString o2) {
         return AsciiString.CHARSEQUENCE_CASE_SENSITIVE_ORDER.compare(o1, o2);
      }
   };
   public static final Comparator<CharSequence> CHARSEQUENCE_CASE_INSENSITIVE_ORDER = new Comparator<CharSequence>() {
      public int compare(CharSequence o1, CharSequence o2) {
         if (o1 == o2) {
            return 0;
         } else {
            AsciiString a1 = o1 instanceof AsciiString ? (AsciiString)o1 : null;
            AsciiString a2 = o2 instanceof AsciiString ? (AsciiString)o2 : null;
            int length1 = o1.length();
            int length2 = o2.length();
            int minLength = Math.min(length1, length2);
            int result;
            byte[] thatValuex;
            int c1;
            if (a1 != null && a2 != null) {
               thatValuex = a1.value;
               byte[] thatValue = a2.value;

               for(c1 = 0; c1 < minLength; ++c1) {
                  byte v1 = thatValuex[c1];
                  byte v2 = thatValue[c1];
                  if (v1 != v2) {
                     int c1x = AsciiString.toLowerCase(v1) & 255;
                     int c2x = AsciiString.toLowerCase(v2) & 255;
                     result = c1x - c2x;
                     if (result != 0) {
                        return result;
                     }
                  }
               }
            } else {
               int ix;
               if (a1 != null) {
                  thatValuex = a1.value;

                  for(ix = 0; ix < minLength; ++ix) {
                     c1 = AsciiString.toLowerCase(thatValuex[ix]) & 255;
                     int c2 = AsciiString.toLowerCase(o2.charAt(ix));
                     result = c1 - c2;
                     if (result != 0) {
                        return result;
                     }
                  }
               } else {
                  char c1xxx;
                  if (a2 != null) {
                     thatValuex = a2.value;

                     for(ix = 0; ix < minLength; ++ix) {
                        c1xxx = AsciiString.toLowerCase(o1.charAt(ix));
                        int c2xx = AsciiString.toLowerCase(thatValuex[ix]) & 255;
                        result = c1xxx - c2xx;
                        if (result != 0) {
                           return result;
                        }
                     }
                  } else {
                     for(int i = 0; i < minLength; ++i) {
                        int c1xx = AsciiString.toLowerCase(o1.charAt(i));
                        c1xxx = AsciiString.toLowerCase(o2.charAt(i));
                        result = c1xx - c1xxx;
                        if (result != 0) {
                           return result;
                        }
                     }
                  }
               }
            }

            return length1 - length2;
         }
      }
   };
   public static final Comparator<CharSequence> CHARSEQUENCE_CASE_SENSITIVE_ORDER = new Comparator<CharSequence>() {
      public int compare(CharSequence o1, CharSequence o2) {
         if (o1 == o2) {
            return 0;
         } else {
            AsciiString a1 = o1 instanceof AsciiString ? (AsciiString)o1 : null;
            AsciiString a2 = o2 instanceof AsciiString ? (AsciiString)o2 : null;
            int length1 = o1.length();
            int length2 = o2.length();
            int minLength = Math.min(length1, length2);
            int result;
            byte[] thatValuex;
            byte c2x;
            if (a1 != null && a2 != null) {
               thatValuex = a1.value;
               byte[] thatValue = a2.value;

               for(int ix = 0; ix < minLength; ++ix) {
                  c2x = thatValuex[ix];
                  byte v2 = thatValue[ix];
                  result = c2x - v2;
                  if (result != 0) {
                     return result;
                  }
               }
            } else {
               int ixx;
               if (a1 != null) {
                  thatValuex = a1.value;

                  for(ixx = 0; ixx < minLength; ++ixx) {
                     int c1 = thatValuex[ixx];
                     int c2 = o2.charAt(ixx);
                     result = c1 - c2;
                     if (result != 0) {
                        return result;
                     }
                  }
               } else {
                  char c1xx;
                  if (a2 != null) {
                     thatValuex = a2.value;

                     for(ixx = 0; ixx < minLength; ++ixx) {
                        c1xx = o1.charAt(ixx);
                        c2x = thatValuex[ixx];
                        result = c1xx - c2x;
                        if (result != 0) {
                           return result;
                        }
                     }
                  } else {
                     for(int i = 0; i < minLength; ++i) {
                        int c1x = o1.charAt(i);
                        c1xx = o2.charAt(i);
                        result = c1x - c1xx;
                        if (result != 0) {
                           return result;
                        }
                     }
                  }
               }
            }

            return length1 - length2;
         }
      }
   };
   private final byte[] value;
   private String string;
   private int hash;

   public static int caseInsensitiveHashCode(CharSequence value) {
      if (value instanceof AsciiString) {
         return value.hashCode();
      } else {
         int hash = 0;
         int end = value.length();

         for(int i = 0; i < end; ++i) {
            hash = hash * 31 ^ value.charAt(i) & 31;
         }

         return hash;
      }
   }

   public static boolean equalsIgnoreCase(CharSequence a, CharSequence b) {
      if (a == b) {
         return true;
      } else {
         AsciiString ab;
         if (a instanceof AsciiString) {
            ab = (AsciiString)a;
            return ab.equalsIgnoreCase(b);
         } else if (b instanceof AsciiString) {
            ab = (AsciiString)b;
            return ab.equalsIgnoreCase(a);
         } else {
            return a != null && b != null ? a.toString().equalsIgnoreCase(b.toString()) : false;
         }
      }
   }

   public static boolean equals(CharSequence a, CharSequence b) {
      if (a == b) {
         return true;
      } else {
         AsciiString ab;
         if (a instanceof AsciiString) {
            ab = (AsciiString)a;
            return ab.equals(b);
         } else if (b instanceof AsciiString) {
            ab = (AsciiString)b;
            return ab.equals(a);
         } else {
            return a != null && b != null ? a.equals(b) : false;
         }
      }
   }

   public static byte[] getBytes(CharSequence v, Charset charset) {
      if (v instanceof AsciiString) {
         return ((AsciiString)v).array();
      } else if (v instanceof String) {
         return ((String)v).getBytes(charset);
      } else if (v != null) {
         ByteBuf buf = Unpooled.copiedBuffer(v, charset);

         byte[] result;
         try {
            if (!buf.hasArray()) {
               result = new byte[buf.readableBytes()];
               buf.readBytes(result);
               byte[] var4 = result;
               return var4;
            }

            result = buf.array();
         } finally {
            buf.release();
         }

         return result;
      } else {
         return null;
      }
   }

   public static AsciiString of(CharSequence string) {
      return string instanceof AsciiString ? (AsciiString)string : new AsciiString(string);
   }

   public AsciiString(byte[] value) {
      this(value, true);
   }

   public AsciiString(byte[] value, boolean copy) {
      checkNull(value);
      if (copy) {
         this.value = (byte[])value.clone();
      } else {
         this.value = value;
      }

   }

   public AsciiString(byte[] value, int start, int length) {
      this(value, start, length, true);
   }

   public AsciiString(byte[] value, int start, int length, boolean copy) {
      checkNull(value);
      if (start >= 0 && start <= value.length - length) {
         if (!copy && start == 0 && length == value.length) {
            this.value = value;
         } else {
            this.value = Arrays.copyOfRange(value, start, start + length);
         }

      } else {
         throw new IndexOutOfBoundsException("expected: 0 <= start(" + start + ") <= start + length(" + length + ") <= " + "value.length(" + value.length + ')');
      }
   }

   public AsciiString(char[] value) {
      this((char[])((char[])checkNull(value)), 0, value.length);
   }

   public AsciiString(char[] value, int start, int length) {
      checkNull(value);
      if (start >= 0 && start <= value.length - length) {
         this.value = new byte[length];
         int i = 0;

         for(int j = start; i < length; ++j) {
            this.value[i] = c2b(value[j]);
            ++i;
         }

      } else {
         throw new IndexOutOfBoundsException("expected: 0 <= start(" + start + ") <= start + length(" + length + ") <= " + "value.length(" + value.length + ')');
      }
   }

   public AsciiString(CharSequence value) {
      this((CharSequence)((CharSequence)checkNull(value)), 0, value.length());
   }

   public AsciiString(CharSequence value, int start, int length) {
      if (value == null) {
         throw new NullPointerException("value");
      } else if (start >= 0 && length >= 0 && length <= value.length() - start) {
         this.value = new byte[length];

         for(int i = 0; i < length; ++i) {
            this.value[i] = c2b(value.charAt(start + i));
         }

      } else {
         throw new IndexOutOfBoundsException("expected: 0 <= start(" + start + ") <= start + length(" + length + ") <= " + "value.length(" + value.length() + ')');
      }
   }

   public AsciiString(ByteBuffer value) {
      this((ByteBuffer)checkNull(value), value.position(), value.remaining());
   }

   public AsciiString(ByteBuffer value, int start, int length) {
      if (value == null) {
         throw new NullPointerException("value");
      } else if (start >= 0 && length <= value.capacity() - start) {
         int baseOffset;
         if (value.hasArray()) {
            baseOffset = value.arrayOffset() + start;
            this.value = Arrays.copyOfRange(value.array(), baseOffset, baseOffset + length);
         } else {
            this.value = new byte[length];
            baseOffset = value.position();
            value.get(this.value, 0, this.value.length);
            value.position(baseOffset);
         }

      } else {
         throw new IndexOutOfBoundsException("expected: 0 <= start(" + start + ") <= start + length(" + length + ") <= " + "value.capacity(" + value.capacity() + ')');
      }
   }

   private static <T> T checkNull(T value) {
      if (value == null) {
         throw new NullPointerException("value");
      } else {
         return value;
      }
   }

   public int length() {
      return this.value.length;
   }

   public char charAt(int index) {
      return (char)(this.byteAt(index) & 255);
   }

   public byte byteAt(int index) {
      return this.value[index];
   }

   public byte[] array() {
      return this.value;
   }

   public int arrayOffset() {
      return 0;
   }

   private static byte c2b(char c) {
      return c > 255 ? 63 : (byte)c;
   }

   private static byte toLowerCase(byte b) {
      return 65 <= b && b <= 90 ? (byte)(b + 32) : b;
   }

   private static char toLowerCase(char c) {
      return 'A' <= c && c <= 'Z' ? (char)(c + 32) : c;
   }

   private static byte toUpperCase(byte b) {
      return 97 <= b && b <= 122 ? (byte)(b - 32) : b;
   }

   public AsciiString subSequence(int start) {
      return this.subSequence(start, this.length());
   }

   public AsciiString subSequence(int start, int end) {
      if (start >= 0 && start <= end && end <= this.length()) {
         byte[] value = this.value;
         if (start == 0 && end == value.length) {
            return this;
         } else {
            return end == start ? EMPTY_STRING : new AsciiString(value, start, end - start, false);
         }
      } else {
         throw new IndexOutOfBoundsException("expected: 0 <= start(" + start + ") <= end (" + end + ") <= length(" + this.length() + ')');
      }
   }

   public int hashCode() {
      int hash = this.hash;
      byte[] value = this.value;
      if (hash == 0 && value.length != 0) {
         for(int i = 0; i < value.length; ++i) {
            hash = hash * 31 ^ value[i] & 31;
         }

         return this.hash = hash;
      } else {
         return hash;
      }
   }

   public boolean equals(Object obj) {
      if (!(obj instanceof AsciiString)) {
         return false;
      } else if (this == obj) {
         return true;
      } else {
         AsciiString that = (AsciiString)obj;
         int thisHash = this.hashCode();
         int thatHash = that.hashCode();
         if (thisHash == thatHash && this.length() == that.length()) {
            byte[] thisValue = this.value;
            byte[] thatValue = that.value;
            int end = thisValue.length;
            int i = 0;

            for(int j = 0; i < end; ++j) {
               if (thisValue[i] != thatValue[j]) {
                  return false;
               }

               ++i;
            }

            return true;
         } else {
            return false;
         }
      }
   }

   public String toString() {
      String string = this.string;
      if (string != null) {
         return string;
      } else {
         byte[] value = this.value;
         return this.string = new String(value, 0, 0, value.length);
      }
   }

   public String toString(int start, int end) {
      byte[] value = this.value;
      if (start == 0 && end == value.length) {
         return this.toString();
      } else {
         int length = end - start;
         return length == 0 ? "" : new String(value, 0, start, length);
      }
   }

   public int compareTo(CharSequence string) {
      if (this == string) {
         return 0;
      } else {
         int length1 = this.length();
         int length2 = string.length();
         int minLength = Math.min(length1, length2);
         byte[] value = this.value;
         int i = 0;

         for(int j = 0; j < minLength; ++j) {
            int result = (value[i] & 255) - string.charAt(j);
            if (result != 0) {
               return result;
            }

            ++i;
         }

         return length1 - length2;
      }
   }

   public int compareToIgnoreCase(CharSequence string) {
      return CHARSEQUENCE_CASE_INSENSITIVE_ORDER.compare(this, string);
   }

   public AsciiString concat(CharSequence string) {
      int thisLen = this.length();
      int thatLen = string.length();
      if (thatLen == 0) {
         return this;
      } else {
         byte[] newValue;
         if (string instanceof AsciiString) {
            AsciiString that = (AsciiString)string;
            if (this.isEmpty()) {
               return that;
            } else {
               newValue = Arrays.copyOf(this.value, thisLen + thatLen);
               System.arraycopy(that.value, 0, newValue, thisLen, thatLen);
               return new AsciiString(newValue, false);
            }
         } else if (this.isEmpty()) {
            return new AsciiString(string);
         } else {
            int newLen = thisLen + thatLen;
            newValue = Arrays.copyOf(this.value, newLen);
            int i = thisLen;

            for(int j = 0; i < newLen; ++j) {
               newValue[i] = c2b(string.charAt(j));
               ++i;
            }

            return new AsciiString(newValue, false);
         }
      }
   }

   public boolean endsWith(CharSequence suffix) {
      int suffixLen = suffix.length();
      return this.regionMatches(this.length() - suffixLen, suffix, 0, suffixLen);
   }

   public boolean equalsIgnoreCase(CharSequence string) {
      if (string == this) {
         return true;
      } else if (string == null) {
         return false;
      } else {
         byte[] value = this.value;
         int thisLen = value.length;
         int thatLen = string.length();
         if (thisLen != thatLen) {
            return false;
         } else {
            for(int i = 0; i < thisLen; ++i) {
               char c1 = (char)(value[i] & 255);
               char c2 = string.charAt(i);
               if (c1 != c2 && toLowerCase(c1) != toLowerCase(c2)) {
                  return false;
               }
            }

            return true;
         }
      }
   }

   public byte[] toByteArray() {
      return this.toByteArray(0, this.length());
   }

   public byte[] toByteArray(int start, int end) {
      return Arrays.copyOfRange(this.value, start, end);
   }

   public char[] toCharArray() {
      return this.toCharArray(0, this.length());
   }

   public char[] toCharArray(int start, int end) {
      int length = end - start;
      if (length == 0) {
         return EmptyArrays.EMPTY_CHARS;
      } else {
         byte[] value = this.value;
         char[] buffer = new char[length];
         int i = 0;

         for(int j = start; i < length; ++j) {
            buffer[i] = (char)(value[j] & 255);
            ++i;
         }

         return buffer;
      }
   }

   public void copy(int srcIdx, ByteBuf dst, int dstIdx, int length) {
      if (dst == null) {
         throw new NullPointerException("dst");
      } else {
         byte[] value = this.value;
         int thisLen = value.length;
         if (srcIdx >= 0 && length <= thisLen - srcIdx) {
            dst.setBytes(dstIdx, value, srcIdx, length);
         } else {
            throw new IndexOutOfBoundsException("expected: 0 <= srcIdx(" + srcIdx + ") <= srcIdx + length(" + length + ") <= srcLen(" + thisLen + ')');
         }
      }
   }

   public void copy(int srcIdx, ByteBuf dst, int length) {
      if (dst == null) {
         throw new NullPointerException("dst");
      } else {
         byte[] value = this.value;
         int thisLen = value.length;
         if (srcIdx >= 0 && length <= thisLen - srcIdx) {
            dst.writeBytes(value, srcIdx, length);
         } else {
            throw new IndexOutOfBoundsException("expected: 0 <= srcIdx(" + srcIdx + ") <= srcIdx + length(" + length + ") <= srcLen(" + thisLen + ')');
         }
      }
   }

   public void copy(int srcIdx, byte[] dst, int dstIdx, int length) {
      if (dst == null) {
         throw new NullPointerException("dst");
      } else {
         byte[] value = this.value;
         int thisLen = value.length;
         if (srcIdx >= 0 && length <= thisLen - srcIdx) {
            System.arraycopy(value, srcIdx, dst, dstIdx, length);
         } else {
            throw new IndexOutOfBoundsException("expected: 0 <= srcIdx(" + srcIdx + ") <= srcIdx + length(" + length + ") <= srcLen(" + thisLen + ')');
         }
      }
   }

   public void copy(int srcIdx, char[] dst, int dstIdx, int length) {
      if (dst == null) {
         throw new NullPointerException("dst");
      } else {
         byte[] value = this.value;
         int thisLen = value.length;
         if (srcIdx >= 0 && length <= thisLen - srcIdx) {
            int dstEnd = dstIdx + length;
            int i = srcIdx;

            for(int j = dstIdx; j < dstEnd; ++j) {
               dst[j] = (char)(value[i] & 255);
               ++i;
            }

         } else {
            throw new IndexOutOfBoundsException("expected: 0 <= srcIdx(" + srcIdx + ") <= srcIdx + length(" + length + ") <= srcLen(" + thisLen + ')');
         }
      }
   }

   public int indexOf(int c) {
      return this.indexOf(c, 0);
   }

   public int indexOf(int c, int start) {
      byte[] value = this.value;
      int length = value.length;
      if (start < length) {
         if (start < 0) {
            start = 0;
         }

         for(int i = start; i < length; ++i) {
            if ((value[i] & 255) == c) {
               return i;
            }
         }
      }

      return -1;
   }

   public int indexOf(CharSequence string) {
      return this.indexOf(string, 0);
   }

   public int indexOf(CharSequence subString, int start) {
      if (start < 0) {
         start = 0;
      }

      byte[] value = this.value;
      int thisLen = value.length;
      int subCount = subString.length();
      if (subCount <= 0) {
         return start < thisLen ? start : thisLen;
      } else if (subCount > thisLen - start) {
         return -1;
      } else {
         char firstChar = subString.charAt(0);

         while(true) {
            int i = this.indexOf(firstChar, start);
            if (i == -1 || subCount + i > thisLen) {
               return -1;
            }

            int o1 = i;
            int o2 = 0;

            do {
               ++o2;
               if (o2 >= subCount) {
                  break;
               }

               ++o1;
            } while((value[o1] & 255) == subString.charAt(o2));

            if (o2 == subCount) {
               return i;
            }

            start = i + 1;
         }
      }
   }

   public int lastIndexOf(int c) {
      return this.lastIndexOf(c, this.length() - 1);
   }

   public int lastIndexOf(int c, int start) {
      if (start >= 0) {
         byte[] value = this.value;
         int length = value.length;
         if (start >= length) {
            start = length - 1;
         }

         for(int i = start; i >= 0; --i) {
            if ((value[i] & 255) == c) {
               return i;
            }
         }
      }

      return -1;
   }

   public int lastIndexOf(CharSequence string) {
      return this.lastIndexOf(string, this.length());
   }

   public int lastIndexOf(CharSequence subString, int start) {
      byte[] value = this.value;
      int thisLen = value.length;
      int subCount = subString.length();
      if (subCount <= thisLen && start >= 0) {
         if (subCount <= 0) {
            return start < thisLen ? start : thisLen;
         } else {
            start = Math.min(start, thisLen - subCount);
            char firstChar = subString.charAt(0);

            while(true) {
               int i = this.lastIndexOf(firstChar, start);
               if (i == -1) {
                  return -1;
               }

               int o1 = i;
               int o2 = 0;

               do {
                  ++o2;
                  if (o2 >= subCount) {
                     break;
                  }

                  ++o1;
               } while((value[o1] & 255) == subString.charAt(o2));

               if (o2 == subCount) {
                  return i;
               }

               start = i - 1;
            }
         }
      } else {
         return -1;
      }
   }

   public boolean isEmpty() {
      return this.value.length == 0;
   }

   public boolean regionMatches(int thisStart, CharSequence string, int start, int length) {
      if (string == null) {
         throw new NullPointerException("string");
      } else if (start >= 0 && string.length() - start >= length) {
         byte[] value = this.value;
         int thisLen = value.length;
         if (thisStart >= 0 && thisLen - thisStart >= length) {
            if (length <= 0) {
               return true;
            } else {
               int thisEnd = thisStart + length;
               int i = thisStart;

               for(int j = start; i < thisEnd; ++j) {
                  if ((value[i] & 255) != string.charAt(j)) {
                     return false;
                  }

                  ++i;
               }

               return true;
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public boolean regionMatches(boolean ignoreCase, int thisStart, CharSequence string, int start, int length) {
      if (!ignoreCase) {
         return this.regionMatches(thisStart, string, start, length);
      } else if (string == null) {
         throw new NullPointerException("string");
      } else {
         byte[] value = this.value;
         int thisLen = value.length;
         if (thisStart >= 0 && length <= thisLen - thisStart) {
            if (start >= 0 && length <= string.length() - start) {
               int thisEnd = thisStart + length;

               char c1;
               char c2;
               do {
                  if (thisStart >= thisEnd) {
                     return true;
                  }

                  c1 = (char)(value[thisStart++] & 255);
                  c2 = string.charAt(start++);
               } while(c1 == c2 || toLowerCase(c1) == toLowerCase(c2));

               return false;
            } else {
               return false;
            }
         } else {
            return false;
         }
      }
   }

   public AsciiString replace(char oldChar, char newChar) {
      int index = this.indexOf(oldChar, 0);
      if (index == -1) {
         return this;
      } else {
         byte[] value = this.value;
         int count = value.length;
         byte[] buffer = new byte[count];
         int i = 0;

         for(int j = 0; i < value.length; ++j) {
            byte b = value[i];
            if ((char)(b & 255) == oldChar) {
               b = (byte)newChar;
            }

            buffer[j] = b;
            ++i;
         }

         return new AsciiString(buffer, false);
      }
   }

   public boolean startsWith(CharSequence prefix) {
      return this.startsWith(prefix, 0);
   }

   public boolean startsWith(CharSequence prefix, int start) {
      return this.regionMatches(start, prefix, 0, prefix.length());
   }

   public AsciiString toLowerCase() {
      boolean lowercased = true;
      byte[] value = this.value;

      int i;
      for(i = 0; i < value.length; ++i) {
         byte b = value[i];
         if (b >= 65 && b <= 90) {
            lowercased = false;
            break;
         }
      }

      if (lowercased) {
         return this;
      } else {
         int length = value.length;
         byte[] newValue = new byte[length];
         i = 0;

         for(int j = 0; i < length; ++j) {
            newValue[i] = toLowerCase(value[j]);
            ++i;
         }

         return new AsciiString(newValue, false);
      }
   }

   public AsciiString toUpperCase() {
      byte[] value = this.value;
      boolean uppercased = true;

      int i;
      for(i = 0; i < value.length; ++i) {
         byte b = value[i];
         if (b >= 97 && b <= 122) {
            uppercased = false;
            break;
         }
      }

      if (uppercased) {
         return this;
      } else {
         int length = value.length;
         byte[] newValue = new byte[length];
         i = 0;

         for(int j = 0; i < length; ++j) {
            newValue[i] = toUpperCase(value[j]);
            ++i;
         }

         return new AsciiString(newValue, false);
      }
   }

   public AsciiString trim() {
      byte[] value = this.value;
      int start = 0;
      int last = value.length;

      int end;
      for(end = last; start <= end && value[start] <= 32; ++start) {
      }

      while(end >= start && value[end] <= 32) {
         --end;
      }

      return start == 0 && end == last ? this : new AsciiString(value, start, end - start + 1, false);
   }

   public boolean contentEquals(CharSequence cs) {
      if (cs == null) {
         throw new NullPointerException();
      } else {
         int length1 = this.length();
         int length2 = cs.length();
         if (length1 != length2) {
            return false;
         } else {
            return length1 == 0 && length2 == 0 ? true : this.regionMatches(0, cs, 0, length2);
         }
      }
   }

   public boolean matches(String expr) {
      return Pattern.matches(expr, this);
   }

   public AsciiString[] split(String expr, int max) {
      return toAsciiStringArray(Pattern.compile(expr).split(this, max));
   }

   private static AsciiString[] toAsciiStringArray(String[] jdkResult) {
      AsciiString[] res = new AsciiString[jdkResult.length];

      for(int i = 0; i < jdkResult.length; ++i) {
         res[i] = new AsciiString(jdkResult[i]);
      }

      return res;
   }

   public AsciiString[] split(char delim) {
      List<AsciiString> res = new ArrayList();
      int start = 0;
      byte[] value = this.value;
      int length = value.length;

      int i;
      for(i = start; i < length; ++i) {
         if (this.charAt(i) == delim) {
            if (start == i) {
               res.add(EMPTY_STRING);
            } else {
               res.add(new AsciiString(value, start, i - start, false));
            }

            start = i + 1;
         }
      }

      if (start == 0) {
         res.add(this);
      } else if (start != length) {
         res.add(new AsciiString(value, start, length - start, false));
      } else {
         for(i = res.size() - 1; i >= 0 && ((AsciiString)res.get(i)).isEmpty(); --i) {
            res.remove(i);
         }
      }

      return (AsciiString[])res.toArray(new AsciiString[res.size()]);
   }

   public boolean contains(CharSequence cs) {
      if (cs == null) {
         throw new NullPointerException();
      } else {
         return this.indexOf(cs) >= 0;
      }
   }

   public int parseInt() {
      return this.parseInt(0, this.length(), 10);
   }

   public int parseInt(int radix) {
      return this.parseInt(0, this.length(), radix);
   }

   public int parseInt(int start, int end) {
      return this.parseInt(start, end, 10);
   }

   public int parseInt(int start, int end, int radix) {
      if (radix >= 2 && radix <= 36) {
         if (start == end) {
            throw new NumberFormatException();
         } else {
            int i = start;
            boolean negative = this.charAt(start) == '-';
            if (negative) {
               i = start + 1;
               if (i == end) {
                  throw new NumberFormatException(this.subSequence(start, end).toString());
               }
            }

            return this.parseInt(i, end, radix, negative);
         }
      } else {
         throw new NumberFormatException();
      }
   }

   private int parseInt(int start, int end, int radix, boolean negative) {
      byte[] value = this.value;
      int max = Integer.MIN_VALUE / radix;
      int result = 0;

      int next;
      for(int offset = start; offset < end; result = next) {
         int digit = Character.digit((char)(value[offset++] & 255), radix);
         if (digit == -1) {
            throw new NumberFormatException(this.subSequence(start, end).toString());
         }

         if (max > result) {
            throw new NumberFormatException(this.subSequence(start, end).toString());
         }

         next = result * radix - digit;
         if (next > result) {
            throw new NumberFormatException(this.subSequence(start, end).toString());
         }
      }

      if (!negative) {
         result = -result;
         if (result < 0) {
            throw new NumberFormatException(this.subSequence(start, end).toString());
         }
      }

      return result;
   }

   public long parseLong() {
      return this.parseLong(0, this.length(), 10);
   }

   public long parseLong(int radix) {
      return this.parseLong(0, this.length(), radix);
   }

   public long parseLong(int start, int end) {
      return this.parseLong(start, end, 10);
   }

   public long parseLong(int start, int end, int radix) {
      if (radix >= 2 && radix <= 36) {
         if (start == end) {
            throw new NumberFormatException();
         } else {
            int i = start;
            boolean negative = this.charAt(start) == '-';
            if (negative) {
               i = start + 1;
               if (i == end) {
                  throw new NumberFormatException(this.subSequence(start, end).toString());
               }
            }

            return this.parseLong(i, end, radix, negative);
         }
      } else {
         throw new NumberFormatException();
      }
   }

   private long parseLong(int start, int end, int radix, boolean negative) {
      byte[] value = this.value;
      long max = Long.MIN_VALUE / (long)radix;
      long result = 0L;

      long next;
      for(int offset = start; offset < end; result = next) {
         int digit = Character.digit((char)(value[offset++] & 255), radix);
         if (digit == -1) {
            throw new NumberFormatException(this.subSequence(start, end).toString());
         }

         if (max > result) {
            throw new NumberFormatException(this.subSequence(start, end).toString());
         }

         next = result * (long)radix - (long)digit;
         if (next > result) {
            throw new NumberFormatException(this.subSequence(start, end).toString());
         }
      }

      if (!negative) {
         result = -result;
         if (result < 0L) {
            throw new NumberFormatException(this.subSequence(start, end).toString());
         }
      }

      return result;
   }

   public short parseShort() {
      return this.parseShort(0, this.length(), 10);
   }

   public short parseShort(int radix) {
      return this.parseShort(0, this.length(), radix);
   }

   public short parseShort(int start, int end) {
      return this.parseShort(start, end, 10);
   }

   public short parseShort(int start, int end, int radix) {
      int intValue = this.parseInt(start, end, radix);
      short result = (short)intValue;
      if (result != intValue) {
         throw new NumberFormatException(this.subSequence(start, end).toString());
      } else {
         return result;
      }
   }

   public float parseFloat() {
      return this.parseFloat(0, this.length());
   }

   public float parseFloat(int start, int end) {
      return Float.parseFloat(this.toString(start, end));
   }

   public double parseDouble() {
      return this.parseDouble(0, this.length());
   }

   public double parseDouble(int start, int end) {
      return Double.parseDouble(this.toString(start, end));
   }
}
