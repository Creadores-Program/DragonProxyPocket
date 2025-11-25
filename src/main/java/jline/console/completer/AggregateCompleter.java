package jline.console.completer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import jline.internal.Preconditions;

public class AggregateCompleter implements Completer {
   private final List<Completer> completers;

   public AggregateCompleter() {
      this.completers = new ArrayList();
   }

   public AggregateCompleter(Collection<Completer> completers) {
      this.completers = new ArrayList();
      Preconditions.checkNotNull(completers);
      this.completers.addAll(completers);
   }

   public AggregateCompleter(Completer... completers) {
      this((Collection)Arrays.asList(completers));
   }

   public Collection<Completer> getCompleters() {
      return this.completers;
   }

   public int complete(String buffer, int cursor, List<CharSequence> candidates) {
      Preconditions.checkNotNull(candidates);
      List<AggregateCompleter.Completion> completions = new ArrayList(this.completers.size());
      int max = -1;
      Iterator i$ = this.completers.iterator();

      while(i$.hasNext()) {
         Completer completer = (Completer)i$.next();
         AggregateCompleter.Completion completion = new AggregateCompleter.Completion(candidates);
         completion.complete(completer, buffer, cursor);
         max = Math.max(max, completion.cursor);
         completions.add(completion);
      }

      i$ = completions.iterator();

      while(i$.hasNext()) {
         AggregateCompleter.Completion completion = (AggregateCompleter.Completion)i$.next();
         if (completion.cursor == max) {
            candidates.addAll(completion.candidates);
         }
      }

      return max;
   }

   public String toString() {
      return this.getClass().getSimpleName() + "{" + "completers=" + this.completers + '}';
   }

   private class Completion {
      public final List<CharSequence> candidates;
      public int cursor;

      public Completion(List<CharSequence> candidates) {
         Preconditions.checkNotNull(candidates);
         this.candidates = new LinkedList(candidates);
      }

      public void complete(Completer completer, String buffer, int cursor) {
         Preconditions.checkNotNull(completer);
         this.cursor = completer.complete(buffer, cursor, this.candidates);
      }
   }
}
