package jline.console.completer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;
import jline.console.ConsoleReader;
import jline.console.CursorBuffer;

public class CandidateListCompletionHandler implements CompletionHandler {
   public boolean complete(ConsoleReader reader, List<CharSequence> candidates, int pos) throws IOException {
      CursorBuffer buf = reader.getCursorBuffer();
      if (candidates.size() == 1) {
         CharSequence value = (CharSequence)candidates.get(0);
         if (value.equals(buf.toString())) {
            return false;
         } else {
            setBuffer(reader, value, pos);
            return true;
         }
      } else {
         if (candidates.size() > 1) {
            String value = this.getUnambiguousCompletions(candidates);
            setBuffer(reader, value, pos);
         }

         printCandidates(reader, candidates);
         reader.drawLine();
         return true;
      }
   }

   public static void setBuffer(ConsoleReader reader, CharSequence value, int offset) throws IOException {
      while(reader.getCursorBuffer().cursor > offset && reader.backspace()) {
      }

      reader.putString(value);
      reader.setCursorPosition(offset + value.length());
   }

   public static void printCandidates(ConsoleReader reader, Collection<CharSequence> candidates) throws IOException {
      Set<CharSequence> distinct = new HashSet((Collection)candidates);
      if (distinct.size() > reader.getAutoprintThreshold()) {
         reader.print((CharSequence)CandidateListCompletionHandler.Messages.DISPLAY_CANDIDATES.format(((Collection)candidates).size()));
         reader.flush();
         String noOpt = CandidateListCompletionHandler.Messages.DISPLAY_CANDIDATES_NO.format();
         String yesOpt = CandidateListCompletionHandler.Messages.DISPLAY_CANDIDATES_YES.format();
         char[] allowed = new char[]{yesOpt.charAt(0), noOpt.charAt(0)};

         int c;
         while((c = reader.readCharacter(allowed)) != -1) {
            String tmp = new String(new char[]{(char)c});
            if (noOpt.startsWith(tmp)) {
               reader.println();
               return;
            }

            if (yesOpt.startsWith(tmp)) {
               break;
            }

            reader.beep();
         }
      }

      if (distinct.size() != ((Collection)candidates).size()) {
         Collection<CharSequence> copy = new ArrayList();
         Iterator i$ = ((Collection)candidates).iterator();

         while(i$.hasNext()) {
            CharSequence next = (CharSequence)i$.next();
            if (!copy.contains(next)) {
               copy.add(next);
            }
         }

         candidates = copy;
      }

      reader.println();
      reader.printColumns((Collection)candidates);
   }

   private String getUnambiguousCompletions(List<CharSequence> candidates) {
      if (candidates != null && !candidates.isEmpty()) {
         String[] strings = (String[])candidates.toArray(new String[candidates.size()]);
         String first = strings[0];
         StringBuilder candidate = new StringBuilder();

         for(int i = 0; i < first.length() && this.startsWith(first.substring(0, i + 1), strings); ++i) {
            candidate.append(first.charAt(i));
         }

         return candidate.toString();
      } else {
         return null;
      }
   }

   private boolean startsWith(String starts, String[] candidates) {
      String[] arr$ = candidates;
      int len$ = candidates.length;

      for(int i$ = 0; i$ < len$; ++i$) {
         String candidate = arr$[i$];
         if (!candidate.startsWith(starts)) {
            return false;
         }
      }

      return true;
   }

   private static enum Messages {
      DISPLAY_CANDIDATES,
      DISPLAY_CANDIDATES_YES,
      DISPLAY_CANDIDATES_NO;

      private static final ResourceBundle bundle = ResourceBundle.getBundle(CandidateListCompletionHandler.class.getName(), Locale.getDefault());

      public String format(Object... args) {
         return bundle == null ? "" : String.format(bundle.getString(this.name()), args);
      }
   }
}
