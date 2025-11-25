package com.google.common.io;

import com.google.common.annotations.Beta;
import com.google.common.base.Ascii;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

public abstract class CharSource {
   protected CharSource() {
   }

   public abstract Reader openStream() throws IOException;

   public BufferedReader openBufferedStream() throws IOException {
      Reader reader = this.openStream();
      return reader instanceof BufferedReader ? (BufferedReader)reader : new BufferedReader(reader);
   }

   @Beta
   public Optional<Long> lengthIfKnown() {
      return Optional.absent();
   }

   @Beta
   public long length() throws IOException {
      Optional<Long> lengthIfKnown = this.lengthIfKnown();
      if (lengthIfKnown.isPresent()) {
         return (Long)lengthIfKnown.get();
      } else {
         Closer closer = Closer.create();

         long var4;
         try {
            Reader reader = (Reader)closer.register(this.openStream());
            var4 = this.countBySkipping(reader);
         } catch (Throwable var9) {
            throw closer.rethrow(var9);
         } finally {
            closer.close();
         }

         return var4;
      }
   }

   private long countBySkipping(Reader reader) throws IOException {
      long count;
      long read;
      for(count = 0L; (read = reader.skip(Long.MAX_VALUE)) != 0L; count += read) {
      }

      return count;
   }

   public long copyTo(Appendable appendable) throws IOException {
      Preconditions.checkNotNull(appendable);
      Closer closer = Closer.create();

      long var4;
      try {
         Reader reader = (Reader)closer.register(this.openStream());
         var4 = CharStreams.copy(reader, appendable);
      } catch (Throwable var9) {
         throw closer.rethrow(var9);
      } finally {
         closer.close();
      }

      return var4;
   }

   public long copyTo(CharSink sink) throws IOException {
      Preconditions.checkNotNull(sink);
      Closer closer = Closer.create();

      long var5;
      try {
         Reader reader = (Reader)closer.register(this.openStream());
         Writer writer = (Writer)closer.register(sink.openStream());
         var5 = CharStreams.copy(reader, writer);
      } catch (Throwable var10) {
         throw closer.rethrow(var10);
      } finally {
         closer.close();
      }

      return var5;
   }

   public String read() throws IOException {
      Closer closer = Closer.create();

      String var3;
      try {
         Reader reader = (Reader)closer.register(this.openStream());
         var3 = CharStreams.toString(reader);
      } catch (Throwable var7) {
         throw closer.rethrow(var7);
      } finally {
         closer.close();
      }

      return var3;
   }

   @Nullable
   public String readFirstLine() throws IOException {
      Closer closer = Closer.create();

      String var3;
      try {
         BufferedReader reader = (BufferedReader)closer.register(this.openBufferedStream());
         var3 = reader.readLine();
      } catch (Throwable var7) {
         throw closer.rethrow(var7);
      } finally {
         closer.close();
      }

      return var3;
   }

   public ImmutableList<String> readLines() throws IOException {
      Closer closer = Closer.create();

      try {
         BufferedReader reader = (BufferedReader)closer.register(this.openBufferedStream());
         ArrayList result = Lists.newArrayList();

         String line;
         while((line = reader.readLine()) != null) {
            result.add(line);
         }

         ImmutableList var5 = ImmutableList.copyOf((Collection)result);
         return var5;
      } catch (Throwable var9) {
         throw closer.rethrow(var9);
      } finally {
         closer.close();
      }
   }

   @Beta
   public <T> T readLines(LineProcessor<T> processor) throws IOException {
      Preconditions.checkNotNull(processor);
      Closer closer = Closer.create();

      Object var4;
      try {
         Reader reader = (Reader)closer.register(this.openStream());
         var4 = CharStreams.readLines(reader, processor);
      } catch (Throwable var8) {
         throw closer.rethrow(var8);
      } finally {
         closer.close();
      }

      return var4;
   }

   public boolean isEmpty() throws IOException {
      Optional<Long> lengthIfKnown = this.lengthIfKnown();
      if (lengthIfKnown.isPresent() && (Long)lengthIfKnown.get() == 0L) {
         return true;
      } else {
         Closer closer = Closer.create();

         boolean var4;
         try {
            Reader reader = (Reader)closer.register(this.openStream());
            var4 = reader.read() == -1;
         } catch (Throwable var8) {
            throw closer.rethrow(var8);
         } finally {
            closer.close();
         }

         return var4;
      }
   }

   public static CharSource concat(Iterable<? extends CharSource> sources) {
      return new CharSource.ConcatenatedCharSource(sources);
   }

   public static CharSource concat(Iterator<? extends CharSource> sources) {
      return concat((Iterable)ImmutableList.copyOf(sources));
   }

   public static CharSource concat(CharSource... sources) {
      return concat((Iterable)ImmutableList.copyOf((Object[])sources));
   }

   public static CharSource wrap(CharSequence charSequence) {
      return new CharSource.CharSequenceCharSource(charSequence);
   }

   public static CharSource empty() {
      return CharSource.EmptyCharSource.INSTANCE;
   }

   private static final class ConcatenatedCharSource extends CharSource {
      private final Iterable<? extends CharSource> sources;

      ConcatenatedCharSource(Iterable<? extends CharSource> sources) {
         this.sources = (Iterable)Preconditions.checkNotNull(sources);
      }

      public Reader openStream() throws IOException {
         return new MultiReader(this.sources.iterator());
      }

      public boolean isEmpty() throws IOException {
         Iterator i$ = this.sources.iterator();

         CharSource source;
         do {
            if (!i$.hasNext()) {
               return true;
            }

            source = (CharSource)i$.next();
         } while(source.isEmpty());

         return false;
      }

      public Optional<Long> lengthIfKnown() {
         long result = 0L;

         Optional lengthIfKnown;
         for(Iterator i$ = this.sources.iterator(); i$.hasNext(); result += (Long)lengthIfKnown.get()) {
            CharSource source = (CharSource)i$.next();
            lengthIfKnown = source.lengthIfKnown();
            if (!lengthIfKnown.isPresent()) {
               return Optional.absent();
            }
         }

         return Optional.of(result);
      }

      public long length() throws IOException {
         long result = 0L;

         CharSource source;
         for(Iterator i$ = this.sources.iterator(); i$.hasNext(); result += source.length()) {
            source = (CharSource)i$.next();
         }

         return result;
      }

      public String toString() {
         return "CharSource.concat(" + this.sources + ")";
      }
   }

   private static final class EmptyCharSource extends CharSource.CharSequenceCharSource {
      private static final CharSource.EmptyCharSource INSTANCE = new CharSource.EmptyCharSource();

      private EmptyCharSource() {
         super("");
      }

      public String toString() {
         return "CharSource.empty()";
      }
   }

   private static class CharSequenceCharSource extends CharSource {
      private static final Splitter LINE_SPLITTER = Splitter.on(Pattern.compile("\r\n|\n|\r"));
      private final CharSequence seq;

      protected CharSequenceCharSource(CharSequence seq) {
         this.seq = (CharSequence)Preconditions.checkNotNull(seq);
      }

      public Reader openStream() {
         return new CharSequenceReader(this.seq);
      }

      public String read() {
         return this.seq.toString();
      }

      public boolean isEmpty() {
         return this.seq.length() == 0;
      }

      public long length() {
         return (long)this.seq.length();
      }

      public Optional<Long> lengthIfKnown() {
         return Optional.of((long)this.seq.length());
      }

      private Iterable<String> lines() {
         return new Iterable<String>() {
            public Iterator<String> iterator() {
               return new AbstractIterator<String>() {
                  Iterator<String> lines;

                  {
                     this.lines = CharSource.CharSequenceCharSource.LINE_SPLITTER.split(CharSequenceCharSource.this.seq).iterator();
                  }

                  protected String computeNext() {
                     if (this.lines.hasNext()) {
                        String next = (String)this.lines.next();
                        if (this.lines.hasNext() || !next.isEmpty()) {
                           return next;
                        }
                     }

                     return (String)this.endOfData();
                  }
               };
            }
         };
      }

      public String readFirstLine() {
         Iterator<String> lines = this.lines().iterator();
         return lines.hasNext() ? (String)lines.next() : null;
      }

      public ImmutableList<String> readLines() {
         return ImmutableList.copyOf(this.lines());
      }

      public <T> T readLines(LineProcessor<T> processor) throws IOException {
         Iterator i$ = this.lines().iterator();

         while(i$.hasNext()) {
            String line = (String)i$.next();
            if (!processor.processLine(line)) {
               break;
            }
         }

         return processor.getResult();
      }

      public String toString() {
         return "CharSource.wrap(" + Ascii.truncate(this.seq, 30, "...") + ")";
      }
   }
}
