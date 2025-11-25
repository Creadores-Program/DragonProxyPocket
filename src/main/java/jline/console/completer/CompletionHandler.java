package jline.console.completer;

import java.io.IOException;
import java.util.List;
import jline.console.ConsoleReader;

public interface CompletionHandler {
   boolean complete(ConsoleReader var1, List<CharSequence> var2, int var3) throws IOException;
}
