package com.google.common.hash;

import com.google.common.base.Preconditions;
import java.nio.charset.Charset;

abstract class AbstractCompositeHashFunction extends AbstractStreamingHashFunction {
   final HashFunction[] functions;
   private static final long serialVersionUID = 0L;

   AbstractCompositeHashFunction(HashFunction... functions) {
      HashFunction[] arr$ = functions;
      int len$ = functions.length;

      for(int i$ = 0; i$ < len$; ++i$) {
         HashFunction function = arr$[i$];
         Preconditions.checkNotNull(function);
      }

      this.functions = functions;
   }

   abstract HashCode makeHash(Hasher[] var1);

   public Hasher newHasher() {
      final Hasher[] hashers = new Hasher[this.functions.length];

      for(int i = 0; i < hashers.length; ++i) {
         hashers[i] = this.functions[i].newHasher();
      }

      return new Hasher() {
         public Hasher putByte(byte b) {
            Hasher[] arr$ = hashers;
            int len$ = arr$.length;

            for(int i$ = 0; i$ < len$; ++i$) {
               Hasher hasher = arr$[i$];
               hasher.putByte(b);
            }

            return this;
         }

         public Hasher putBytes(byte[] bytes) {
            Hasher[] arr$ = hashers;
            int len$ = arr$.length;

            for(int i$ = 0; i$ < len$; ++i$) {
               Hasher hasher = arr$[i$];
               hasher.putBytes(bytes);
            }

            return this;
         }

         public Hasher putBytes(byte[] bytes, int off, int len) {
            Hasher[] arr$ = hashers;
            int len$ = arr$.length;

            for(int i$ = 0; i$ < len$; ++i$) {
               Hasher hasher = arr$[i$];
               hasher.putBytes(bytes, off, len);
            }

            return this;
         }

         public Hasher putShort(short s) {
            Hasher[] arr$ = hashers;
            int len$ = arr$.length;

            for(int i$ = 0; i$ < len$; ++i$) {
               Hasher hasher = arr$[i$];
               hasher.putShort(s);
            }

            return this;
         }

         public Hasher putInt(int i) {
            Hasher[] arr$ = hashers;
            int len$ = arr$.length;

            for(int i$ = 0; i$ < len$; ++i$) {
               Hasher hasher = arr$[i$];
               hasher.putInt(i);
            }

            return this;
         }

         public Hasher putLong(long l) {
            Hasher[] arr$ = hashers;
            int len$ = arr$.length;

            for(int i$ = 0; i$ < len$; ++i$) {
               Hasher hasher = arr$[i$];
               hasher.putLong(l);
            }

            return this;
         }

         public Hasher putFloat(float f) {
            Hasher[] arr$ = hashers;
            int len$ = arr$.length;

            for(int i$ = 0; i$ < len$; ++i$) {
               Hasher hasher = arr$[i$];
               hasher.putFloat(f);
            }

            return this;
         }

         public Hasher putDouble(double d) {
            Hasher[] arr$ = hashers;
            int len$ = arr$.length;

            for(int i$ = 0; i$ < len$; ++i$) {
               Hasher hasher = arr$[i$];
               hasher.putDouble(d);
            }

            return this;
         }

         public Hasher putBoolean(boolean b) {
            Hasher[] arr$ = hashers;
            int len$ = arr$.length;

            for(int i$ = 0; i$ < len$; ++i$) {
               Hasher hasher = arr$[i$];
               hasher.putBoolean(b);
            }

            return this;
         }

         public Hasher putChar(char c) {
            Hasher[] arr$ = hashers;
            int len$ = arr$.length;

            for(int i$ = 0; i$ < len$; ++i$) {
               Hasher hasher = arr$[i$];
               hasher.putChar(c);
            }

            return this;
         }

         public Hasher putUnencodedChars(CharSequence chars) {
            Hasher[] arr$ = hashers;
            int len$ = arr$.length;

            for(int i$ = 0; i$ < len$; ++i$) {
               Hasher hasher = arr$[i$];
               hasher.putUnencodedChars(chars);
            }

            return this;
         }

         public Hasher putString(CharSequence chars, Charset charset) {
            Hasher[] arr$ = hashers;
            int len$ = arr$.length;

            for(int i$ = 0; i$ < len$; ++i$) {
               Hasher hasher = arr$[i$];
               hasher.putString(chars, charset);
            }

            return this;
         }

         public <T> Hasher putObject(T instance, Funnel<? super T> funnel) {
            Hasher[] arr$ = hashers;
            int len$ = arr$.length;

            for(int i$ = 0; i$ < len$; ++i$) {
               Hasher hasher = arr$[i$];
               hasher.putObject(instance, funnel);
            }

            return this;
         }

         public HashCode hash() {
            return AbstractCompositeHashFunction.this.makeHash(hashers);
         }
      };
   }
}
