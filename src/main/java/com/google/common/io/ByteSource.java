package com.google.common.io;

import com.google.common.annotations.Beta;
import com.google.common.base.Ascii;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.hash.Funnels;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Iterator;

public abstract class ByteSource {
   protected ByteSource() {
   }

   public CharSource asCharSource(Charset charset) {
      return new ByteSource.AsCharSource(charset);
   }

   public abstract InputStream openStream() throws IOException;

   public InputStream openBufferedStream() throws IOException {
      InputStream in = this.openStream();
      return in instanceof BufferedInputStream ? (BufferedInputStream)in : new BufferedInputStream(in);
   }

   public ByteSource slice(long offset, long length) {
      return new ByteSource.SlicedByteSource(offset, length);
   }

   public boolean isEmpty() throws IOException {
      Optional<Long> sizeIfKnown = this.sizeIfKnown();
      if (sizeIfKnown.isPresent() && (Long)sizeIfKnown.get() == 0L) {
         return true;
      } else {
         Closer closer = Closer.create();

         boolean var4;
         try {
            InputStream in = (InputStream)closer.register(this.openStream());
            var4 = in.read() == -1;
         } catch (Throwable var8) {
            throw closer.rethrow(var8);
         } finally {
            closer.close();
         }

         return var4;
      }
   }

   @Beta
   public Optional<Long> sizeIfKnown() {
      return Optional.absent();
   }

   public long size() throws IOException {
      Optional<Long> sizeIfKnown = this.sizeIfKnown();
      if (sizeIfKnown.isPresent()) {
         return (Long)sizeIfKnown.get();
      } else {
         Closer closer = Closer.create();

         InputStream in;
         long var4;
         try {
            in = (InputStream)closer.register(this.openStream());
            var4 = this.countBySkipping(in);
            return var4;
         } catch (IOException var18) {
         } finally {
            closer.close();
         }

         closer = Closer.create();

         try {
            in = (InputStream)closer.register(this.openStream());
            var4 = this.countByReading(in);
         } catch (Throwable var16) {
            throw closer.rethrow(var16);
         } finally {
            closer.close();
         }

         return var4;
      }
   }

   private long countBySkipping(InputStream in) throws IOException {
      long count;
      long skipped;
      for(count = 0L; (skipped = ByteStreams.skipUpTo(in, 2147483647L)) > 0L; count += skipped) {
      }

      return count;
   }

   private long countByReading(InputStream in) throws IOException {
      long count;
      long read;
      for(count = 0L; (read = (long)in.read(ByteStreams.skipBuffer)) != -1L; count += read) {
      }

      return count;
   }

   public long copyTo(OutputStream output) throws IOException {
      Preconditions.checkNotNull(output);
      Closer closer = Closer.create();

      long var4;
      try {
         InputStream in = (InputStream)closer.register(this.openStream());
         var4 = ByteStreams.copy(in, output);
      } catch (Throwable var9) {
         throw closer.rethrow(var9);
      } finally {
         closer.close();
      }

      return var4;
   }

   public long copyTo(ByteSink sink) throws IOException {
      Preconditions.checkNotNull(sink);
      Closer closer = Closer.create();

      long var5;
      try {
         InputStream in = (InputStream)closer.register(this.openStream());
         OutputStream out = (OutputStream)closer.register(sink.openStream());
         var5 = ByteStreams.copy(in, out);
      } catch (Throwable var10) {
         throw closer.rethrow(var10);
      } finally {
         closer.close();
      }

      return var5;
   }

   public byte[] read() throws IOException {
      Closer closer = Closer.create();

      byte[] var3;
      try {
         InputStream in = (InputStream)closer.register(this.openStream());
         var3 = ByteStreams.toByteArray(in);
      } catch (Throwable var7) {
         throw closer.rethrow(var7);
      } finally {
         closer.close();
      }

      return var3;
   }

   @Beta
   public <T> T read(ByteProcessor<T> processor) throws IOException {
      Preconditions.checkNotNull(processor);
      Closer closer = Closer.create();

      Object var4;
      try {
         InputStream in = (InputStream)closer.register(this.openStream());
         var4 = ByteStreams.readBytes(in, processor);
      } catch (Throwable var8) {
         throw closer.rethrow(var8);
      } finally {
         closer.close();
      }

      return var4;
   }

   public HashCode hash(HashFunction hashFunction) throws IOException {
      Hasher hasher = hashFunction.newHasher();
      this.copyTo(Funnels.asOutputStream(hasher));
      return hasher.hash();
   }

   public boolean contentEquals(ByteSource other) throws IOException {
      Preconditions.checkNotNull(other);
      byte[] buf1 = new byte[8192];
      byte[] buf2 = new byte[8192];
      Closer closer = Closer.create();

      try {
         InputStream in1 = (InputStream)closer.register(this.openStream());
         InputStream in2 = (InputStream)closer.register(other.openStream());

         int read1;
         boolean var9;
         do {
            read1 = ByteStreams.read(in1, buf1, 0, 8192);
            int read2 = ByteStreams.read(in2, buf2, 0, 8192);
            if (read1 != read2 || !Arrays.equals(buf1, buf2)) {
               var9 = false;
               return var9;
            }
         } while(read1 == 8192);

         var9 = true;
         return var9;
      } catch (Throwable var13) {
         throw closer.rethrow(var13);
      } finally {
         closer.close();
      }
   }

   public static ByteSource concat(Iterable<? extends ByteSource> sources) {
      return new ByteSource.ConcatenatedByteSource(sources);
   }

   public static ByteSource concat(Iterator<? extends ByteSource> sources) {
      return concat((Iterable)ImmutableList.copyOf(sources));
   }

   public static ByteSource concat(ByteSource... sources) {
      return concat((Iterable)ImmutableList.copyOf((Object[])sources));
   }

   public static ByteSource wrap(byte[] b) {
      return new ByteSource.ByteArrayByteSource(b);
   }

   public static ByteSource empty() {
      return ByteSource.EmptyByteSource.INSTANCE;
   }

   private static final class ConcatenatedByteSource extends ByteSource {
      final Iterable<? extends ByteSource> sources;

      ConcatenatedByteSource(Iterable<? extends ByteSource> sources) {
         this.sources = (Iterable)Preconditions.checkNotNull(sources);
      }

      public InputStream openStream() throws IOException {
         return new MultiInputStream(this.sources.iterator());
      }

      public boolean isEmpty() throws IOException {
         Iterator i$ = this.sources.iterator();

         ByteSource source;
         do {
            if (!i$.hasNext()) {
               return true;
            }

            source = (ByteSource)i$.next();
         } while(source.isEmpty());

         return false;
      }

      public Optional<Long> sizeIfKnown() {
         long result = 0L;

         Optional sizeIfKnown;
         for(Iterator i$ = this.sources.iterator(); i$.hasNext(); result += (Long)sizeIfKnown.get()) {
            ByteSource source = (ByteSource)i$.next();
            sizeIfKnown = source.sizeIfKnown();
            if (!sizeIfKnown.isPresent()) {
               return Optional.absent();
            }
         }

         return Optional.of(result);
      }

      public long size() throws IOException {
         long result = 0L;

         ByteSource source;
         for(Iterator i$ = this.sources.iterator(); i$.hasNext(); result += source.size()) {
            source = (ByteSource)i$.next();
         }

         return result;
      }

      public String toString() {
         return "ByteSource.concat(" + this.sources + ")";
      }
   }

   private static final class EmptyByteSource extends ByteSource.ByteArrayByteSource {
      static final ByteSource.EmptyByteSource INSTANCE = new ByteSource.EmptyByteSource();

      EmptyByteSource() {
         super(new byte[0]);
      }

      public CharSource asCharSource(Charset charset) {
         Preconditions.checkNotNull(charset);
         return CharSource.empty();
      }

      public byte[] read() {
         return this.bytes;
      }

      public String toString() {
         return "ByteSource.empty()";
      }
   }

   private static class ByteArrayByteSource extends ByteSource {
      final byte[] bytes;
      final int offset;
      final int length;

      ByteArrayByteSource(byte[] bytes) {
         this(bytes, 0, bytes.length);
      }

      ByteArrayByteSource(byte[] bytes, int offset, int length) {
         this.bytes = bytes;
         this.offset = offset;
         this.length = length;
      }

      public InputStream openStream() {
         return new ByteArrayInputStream(this.bytes, this.offset, this.length);
      }

      public InputStream openBufferedStream() throws IOException {
         return this.openStream();
      }

      public boolean isEmpty() {
         return this.length == 0;
      }

      public long size() {
         return (long)this.length;
      }

      public Optional<Long> sizeIfKnown() {
         return Optional.of((long)this.length);
      }

      public byte[] read() {
         return Arrays.copyOfRange(this.bytes, this.offset, this.offset + this.length);
      }

      public long copyTo(OutputStream output) throws IOException {
         output.write(this.bytes, this.offset, this.length);
         return (long)this.length;
      }

      public <T> T read(ByteProcessor<T> processor) throws IOException {
         processor.processBytes(this.bytes, this.offset, this.length);
         return processor.getResult();
      }

      public HashCode hash(HashFunction hashFunction) throws IOException {
         return hashFunction.hashBytes(this.bytes, this.offset, this.length);
      }

      public ByteSource slice(long offset, long length) {
         Preconditions.checkArgument(offset >= 0L, "offset (%s) may not be negative", offset);
         Preconditions.checkArgument(length >= 0L, "length (%s) may not be negative", length);
         offset = Math.min(offset, (long)this.length);
         length = Math.min(length, (long)this.length - offset);
         int newOffset = this.offset + (int)offset;
         return new ByteSource.ByteArrayByteSource(this.bytes, newOffset, (int)length);
      }

      public String toString() {
         return "ByteSource.wrap(" + Ascii.truncate(BaseEncoding.base16().encode(this.bytes, this.offset, this.length), 30, "...") + ")";
      }
   }

   private final class SlicedByteSource extends ByteSource {
      final long offset;
      final long length;

      SlicedByteSource(long offset, long length) {
         Preconditions.checkArgument(offset >= 0L, "offset (%s) may not be negative", offset);
         Preconditions.checkArgument(length >= 0L, "length (%s) may not be negative", length);
         this.offset = offset;
         this.length = length;
      }

      public InputStream openStream() throws IOException {
         return this.sliceStream(ByteSource.this.openStream());
      }

      public InputStream openBufferedStream() throws IOException {
         return this.sliceStream(ByteSource.this.openBufferedStream());
      }

      private InputStream sliceStream(InputStream in) throws IOException {
         if (this.offset > 0L) {
            long skipped;
            try {
               skipped = ByteStreams.skipUpTo(in, this.offset);
            } catch (Throwable var10) {
               Throwable e = var10;
               Closer closer = Closer.create();
               closer.register(in);

               try {
                  throw closer.rethrow(e);
               } finally {
                  closer.close();
               }
            }

            if (skipped < this.offset) {
               in.close();
               return new ByteArrayInputStream(new byte[0]);
            }
         }

         return ByteStreams.limit(in, this.length);
      }

      public ByteSource slice(long offset, long length) {
         Preconditions.checkArgument(offset >= 0L, "offset (%s) may not be negative", offset);
         Preconditions.checkArgument(length >= 0L, "length (%s) may not be negative", length);
         long maxLength = this.length - offset;
         return ByteSource.this.slice(this.offset + offset, Math.min(length, maxLength));
      }

      public boolean isEmpty() throws IOException {
         return this.length == 0L || super.isEmpty();
      }

      public Optional<Long> sizeIfKnown() {
         Optional<Long> optionalUnslicedSize = ByteSource.this.sizeIfKnown();
         if (optionalUnslicedSize.isPresent()) {
            long unslicedSize = (Long)optionalUnslicedSize.get();
            long off = Math.min(this.offset, unslicedSize);
            return Optional.of(Math.min(this.length, unslicedSize - off));
         } else {
            return Optional.absent();
         }
      }

      public String toString() {
         return ByteSource.this.toString() + ".slice(" + this.offset + ", " + this.length + ")";
      }
   }

   private final class AsCharSource extends CharSource {
      private final Charset charset;

      private AsCharSource(Charset charset) {
         this.charset = (Charset)Preconditions.checkNotNull(charset);
      }

      public Reader openStream() throws IOException {
         return new InputStreamReader(ByteSource.this.openStream(), this.charset);
      }

      public String toString() {
         return ByteSource.this.toString() + ".asCharSource(" + this.charset + ")";
      }

      // $FF: synthetic method
      AsCharSource(Charset x1, Object x2) {
         this(x1);
      }
   }
}
