package com.google.common.io;

import com.google.common.annotations.Beta;
import com.google.common.base.Preconditions;
import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.List;

@Beta
public final class CharStreams {
   private static final int BUF_SIZE = 2048;

   private CharStreams() {
   }

   public static long copy(Readable from, Appendable to) throws IOException {
      Preconditions.checkNotNull(from);
      Preconditions.checkNotNull(to);
      CharBuffer buf = CharBuffer.allocate(2048);
      long total = 0L;

      while(from.read(buf) != -1) {
         buf.flip();
         to.append(buf);
         total += (long)buf.remaining();
         buf.clear();
      }

      return total;
   }

   public static String toString(Readable r) throws IOException {
      return toStringBuilder(r).toString();
   }

   private static StringBuilder toStringBuilder(Readable r) throws IOException {
      StringBuilder sb = new StringBuilder();
      copy(r, sb);
      return sb;
   }

   public static List<String> readLines(Readable r) throws IOException {
      List<String> result = new ArrayList();
      LineReader lineReader = new LineReader(r);

      String line;
      while((line = lineReader.readLine()) != null) {
         result.add(line);
      }

      return result;
   }

   public static <T> T readLines(Readable readable, LineProcessor<T> processor) throws IOException {
      Preconditions.checkNotNull(readable);
      Preconditions.checkNotNull(processor);
      LineReader lineReader = new LineReader(readable);

      String line;
      while((line = lineReader.readLine()) != null && processor.processLine(line)) {
      }

      return processor.getResult();
   }

   public static void skipFully(Reader reader, long n) throws IOException {
      Preconditions.checkNotNull(reader);

      while(n > 0L) {
         long amt = reader.skip(n);
         if (amt == 0L) {
            throw new EOFException();
         }

         n -= amt;
      }

   }

   public static Writer nullWriter() {
      return CharStreams.NullWriter.INSTANCE;
   }

   public static Writer asWriter(Appendable target) {
      return (Writer)(target instanceof Writer ? (Writer)target : new AppendableWriter(target));
   }

   static Reader asReader(final Readable readable) {
      Preconditions.checkNotNull(readable);
      return readable instanceof Reader ? (Reader)readable : new Reader() {
         public int read(char[] cbuf, int off, int len) throws IOException {
            return this.read(CharBuffer.wrap(cbuf, off, len));
         }

         public int read(CharBuffer target) throws IOException {
            return readable.read(target);
         }

         public void close() throws IOException {
            if (readable instanceof Closeable) {
               ((Closeable)readable).close();
            }

         }
      };
   }

   private static final class NullWriter extends Writer {
      private static final CharStreams.NullWriter INSTANCE = new CharStreams.NullWriter();

      public void write(int c) {
      }

      public void write(char[] cbuf) {
         Preconditions.checkNotNull(cbuf);
      }

      public void write(char[] cbuf, int off, int len) {
         Preconditions.checkPositionIndexes(off, off + len, cbuf.length);
      }

      public void write(String str) {
         Preconditions.checkNotNull(str);
      }

      public void write(String str, int off, int len) {
         Preconditions.checkPositionIndexes(off, off + len, str.length());
      }

      public Writer append(CharSequence csq) {
         Preconditions.checkNotNull(csq);
         return this;
      }

      public Writer append(CharSequence csq, int start, int end) {
         Preconditions.checkPositionIndexes(start, end, csq.length());
         return this;
      }

      public Writer append(char c) {
         return this;
      }

      public void flush() {
      }

      public void close() {
      }

      public String toString() {
         return "CharStreams.nullWriter()";
      }
   }
}
