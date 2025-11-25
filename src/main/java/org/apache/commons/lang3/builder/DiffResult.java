package org.apache.commons.lang3.builder;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class DiffResult implements Iterable<Diff<?>> {
   public static final String OBJECTS_SAME_STRING = "";
   private static final String DIFFERS_STRING = "differs from";
   private final List<Diff<?>> diffs;
   private final Object lhs;
   private final Object rhs;
   private final ToStringStyle style;

   DiffResult(Object lhs, Object rhs, List<Diff<?>> diffs, ToStringStyle style) {
      if (lhs == null) {
         throw new IllegalArgumentException("Left hand object cannot be null");
      } else if (rhs == null) {
         throw new IllegalArgumentException("Right hand object cannot be null");
      } else if (diffs == null) {
         throw new IllegalArgumentException("List of differences cannot be null");
      } else {
         this.diffs = diffs;
         this.lhs = lhs;
         this.rhs = rhs;
         if (style == null) {
            this.style = ToStringStyle.DEFAULT_STYLE;
         } else {
            this.style = style;
         }

      }
   }

   public List<Diff<?>> getDiffs() {
      return Collections.unmodifiableList(this.diffs);
   }

   public int getNumberOfDiffs() {
      return this.diffs.size();
   }

   public ToStringStyle getToStringStyle() {
      return this.style;
   }

   public String toString() {
      return this.toString(this.style);
   }

   public String toString(ToStringStyle style) {
      if (this.diffs.size() == 0) {
         return "";
      } else {
         ToStringBuilder lhsBuilder = new ToStringBuilder(this.lhs, style);
         ToStringBuilder rhsBuilder = new ToStringBuilder(this.rhs, style);
         Iterator i$ = this.diffs.iterator();

         while(i$.hasNext()) {
            Diff<?> diff = (Diff)i$.next();
            lhsBuilder.append(diff.getFieldName(), diff.getLeft());
            rhsBuilder.append(diff.getFieldName(), diff.getRight());
         }

         return String.format("%s %s %s", lhsBuilder.build(), "differs from", rhsBuilder.build());
      }
   }

   public Iterator<Diff<?>> iterator() {
      return this.diffs.iterator();
   }
}
