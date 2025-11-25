package com.google.common.base;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import java.io.IOException;
import java.util.AbstractList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;

@GwtCompatible
public class Joiner {
   private final String separator;

   @CheckReturnValue
   public static Joiner on(String separator) {
      return new Joiner(separator);
   }

   @CheckReturnValue
   public static Joiner on(char separator) {
      return new Joiner(String.valueOf(separator));
   }

   private Joiner(String separator) {
      this.separator = (String)Preconditions.checkNotNull(separator);
   }

   private Joiner(Joiner prototype) {
      this.separator = prototype.separator;
   }

   public <A extends Appendable> A appendTo(A appendable, Iterable<?> parts) throws IOException {
      return this.appendTo(appendable, parts.iterator());
   }

   public <A extends Appendable> A appendTo(A appendable, Iterator<?> parts) throws IOException {
      Preconditions.checkNotNull(appendable);
      if (parts.hasNext()) {
         appendable.append(this.toString(parts.next()));

         while(parts.hasNext()) {
            appendable.append(this.separator);
            appendable.append(this.toString(parts.next()));
         }
      }

      return appendable;
   }

   public final <A extends Appendable> A appendTo(A appendable, Object[] parts) throws IOException {
      return this.appendTo((Appendable)appendable, (Iterable)Arrays.asList(parts));
   }

   public final <A extends Appendable> A appendTo(A appendable, @Nullable Object first, @Nullable Object second, Object... rest) throws IOException {
      return this.appendTo(appendable, iterable(first, second, rest));
   }

   public final StringBuilder appendTo(StringBuilder builder, Iterable<?> parts) {
      return this.appendTo(builder, parts.iterator());
   }

   public final StringBuilder appendTo(StringBuilder builder, Iterator<?> parts) {
      try {
         this.appendTo((Appendable)builder, (Iterator)parts);
         return builder;
      } catch (IOException var4) {
         throw new AssertionError(var4);
      }
   }

   public final StringBuilder appendTo(StringBuilder builder, Object[] parts) {
      return this.appendTo((StringBuilder)builder, (Iterable)Arrays.asList(parts));
   }

   public final StringBuilder appendTo(StringBuilder builder, @Nullable Object first, @Nullable Object second, Object... rest) {
      return this.appendTo(builder, iterable(first, second, rest));
   }

   @CheckReturnValue
   public final String join(Iterable<?> parts) {
      return this.join(parts.iterator());
   }

   @CheckReturnValue
   public final String join(Iterator<?> parts) {
      return this.appendTo(new StringBuilder(), parts).toString();
   }

   @CheckReturnValue
   public final String join(Object[] parts) {
      return this.join((Iterable)Arrays.asList(parts));
   }

   @CheckReturnValue
   public final String join(@Nullable Object first, @Nullable Object second, Object... rest) {
      return this.join(iterable(first, second, rest));
   }

   @CheckReturnValue
   public Joiner useForNull(final String nullText) {
      Preconditions.checkNotNull(nullText);
      return new Joiner(this) {
         CharSequence toString(@Nullable Object part) {
            return (CharSequence)(part == null ? nullText : Joiner.this.toString(part));
         }

         public Joiner useForNull(String nullTextx) {
            throw new UnsupportedOperationException("already specified useForNull");
         }

         public Joiner skipNulls() {
            throw new UnsupportedOperationException("already specified useForNull");
         }
      };
   }

   @CheckReturnValue
   public Joiner skipNulls() {
      return new Joiner(this) {
         public <A extends Appendable> A appendTo(A appendable, Iterator<?> parts) throws IOException {
            Preconditions.checkNotNull(appendable, "appendable");
            Preconditions.checkNotNull(parts, "parts");

            Object part;
            while(parts.hasNext()) {
               part = parts.next();
               if (part != null) {
                  appendable.append(Joiner.this.toString(part));
                  break;
               }
            }

            while(parts.hasNext()) {
               part = parts.next();
               if (part != null) {
                  appendable.append(Joiner.this.separator);
                  appendable.append(Joiner.this.toString(part));
               }
            }

            return appendable;
         }

         public Joiner useForNull(String nullText) {
            throw new UnsupportedOperationException("already specified skipNulls");
         }

         public Joiner.MapJoiner withKeyValueSeparator(String kvs) {
            throw new UnsupportedOperationException("can't use .skipNulls() with maps");
         }
      };
   }

   @CheckReturnValue
   public Joiner.MapJoiner withKeyValueSeparator(String keyValueSeparator) {
      return new Joiner.MapJoiner(this, keyValueSeparator);
   }

   CharSequence toString(Object part) {
      Preconditions.checkNotNull(part);
      return (CharSequence)(part instanceof CharSequence ? (CharSequence)part : part.toString());
   }

   private static Iterable<Object> iterable(final Object first, final Object second, final Object[] rest) {
      Preconditions.checkNotNull(rest);
      return new AbstractList<Object>() {
         public int size() {
            return rest.length + 2;
         }

         public Object get(int index) {
            switch(index) {
            case 0:
               return first;
            case 1:
               return second;
            default:
               return rest[index - 2];
            }
         }
      };
   }

   // $FF: synthetic method
   Joiner(Joiner x0, Object x1) {
      this(x0);
   }

   public static final class MapJoiner {
      private final Joiner joiner;
      private final String keyValueSeparator;

      private MapJoiner(Joiner joiner, String keyValueSeparator) {
         this.joiner = joiner;
         this.keyValueSeparator = (String)Preconditions.checkNotNull(keyValueSeparator);
      }

      public <A extends Appendable> A appendTo(A appendable, Map<?, ?> map) throws IOException {
         return this.appendTo((Appendable)appendable, (Iterable)map.entrySet());
      }

      public StringBuilder appendTo(StringBuilder builder, Map<?, ?> map) {
         return this.appendTo((StringBuilder)builder, (Iterable)map.entrySet());
      }

      @CheckReturnValue
      public String join(Map<?, ?> map) {
         return this.join((Iterable)map.entrySet());
      }

      @Beta
      public <A extends Appendable> A appendTo(A appendable, Iterable<? extends Entry<?, ?>> entries) throws IOException {
         return this.appendTo(appendable, entries.iterator());
      }

      @Beta
      public <A extends Appendable> A appendTo(A appendable, Iterator<? extends Entry<?, ?>> parts) throws IOException {
         Preconditions.checkNotNull(appendable);
         if (parts.hasNext()) {
            Entry<?, ?> entry = (Entry)parts.next();
            appendable.append(this.joiner.toString(entry.getKey()));
            appendable.append(this.keyValueSeparator);
            appendable.append(this.joiner.toString(entry.getValue()));

            while(parts.hasNext()) {
               appendable.append(this.joiner.separator);
               Entry<?, ?> e = (Entry)parts.next();
               appendable.append(this.joiner.toString(e.getKey()));
               appendable.append(this.keyValueSeparator);
               appendable.append(this.joiner.toString(e.getValue()));
            }
         }

         return appendable;
      }

      @Beta
      public StringBuilder appendTo(StringBuilder builder, Iterable<? extends Entry<?, ?>> entries) {
         return this.appendTo(builder, entries.iterator());
      }

      @Beta
      public StringBuilder appendTo(StringBuilder builder, Iterator<? extends Entry<?, ?>> entries) {
         try {
            this.appendTo((Appendable)builder, (Iterator)entries);
            return builder;
         } catch (IOException var4) {
            throw new AssertionError(var4);
         }
      }

      @CheckReturnValue
      @Beta
      public String join(Iterable<? extends Entry<?, ?>> entries) {
         return this.join(entries.iterator());
      }

      @CheckReturnValue
      @Beta
      public String join(Iterator<? extends Entry<?, ?>> entries) {
         return this.appendTo(new StringBuilder(), entries).toString();
      }

      @CheckReturnValue
      public Joiner.MapJoiner useForNull(String nullText) {
         return new Joiner.MapJoiner(this.joiner.useForNull(nullText), this.keyValueSeparator);
      }

      // $FF: synthetic method
      MapJoiner(Joiner x0, String x1, Object x2) {
         this(x0, x1);
      }
   }
}
