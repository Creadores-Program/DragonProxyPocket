package jline.console;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Stack;
import jline.Terminal;
import jline.TerminalFactory;
import jline.UnixTerminal;
import jline.console.completer.CandidateListCompletionHandler;
import jline.console.completer.Completer;
import jline.console.completer.CompletionHandler;
import jline.console.history.History;
import jline.console.history.MemoryHistory;
import jline.internal.Configuration;
import jline.internal.InputStreamReader;
import jline.internal.Log;
import jline.internal.NonBlockingInputStream;
import jline.internal.Nullable;
import jline.internal.Preconditions;
import jline.internal.Urls;
import org.fusesource.jansi.AnsiOutputStream;

public class ConsoleReader {
   public static final String JLINE_NOBELL = "jline.nobell";
   public static final String JLINE_ESC_TIMEOUT = "jline.esc.timeout";
   public static final String JLINE_INPUTRC = "jline.inputrc";
   public static final String INPUT_RC = ".inputrc";
   public static final String DEFAULT_INPUT_RC = "/etc/inputrc";
   public static final char BACKSPACE = '\b';
   public static final char RESET_LINE = '\r';
   public static final char KEYBOARD_BELL = '\u0007';
   public static final char NULL_MASK = '\u0000';
   public static final int TAB_WIDTH = 4;
   private static final ResourceBundle resources = ResourceBundle.getBundle(CandidateListCompletionHandler.class.getName());
   private final Terminal terminal;
   private final Writer out;
   private final CursorBuffer buf;
   private String prompt;
   private int promptLen;
   private boolean expandEvents;
   private boolean bellEnabled;
   private boolean handleUserInterrupt;
   private Character mask;
   private Character echoCharacter;
   private StringBuffer searchTerm;
   private String previousSearchTerm;
   private int searchIndex;
   private int parenBlinkTimeout;
   private NonBlockingInputStream in;
   private long escapeTimeout;
   private Reader reader;
   private boolean isUnitTestInput;
   private char charSearchChar;
   private char charSearchLastInvokeChar;
   private char charSearchFirstInvokeChar;
   private String yankBuffer;
   private String encoding;
   private boolean recording;
   private String macro;
   private String appName;
   private URL inputrcUrl;
   private ConsoleKeys consoleKeys;
   private String commentBegin;
   private boolean skipLF;
   private boolean copyPasteDetection;
   private ConsoleReader.State state;
   public static final String JLINE_COMPLETION_THRESHOLD = "jline.completion.threshold";
   private final List<Completer> completers;
   private CompletionHandler completionHandler;
   private int autoprintThreshold;
   private boolean paginationEnabled;
   private History history;
   private boolean historyEnabled;
   public static final String CR = Configuration.getLineSeparator();
   private final Map<Character, ActionListener> triggeredActions;
   private Thread maskThread;

   public ConsoleReader() throws IOException {
      this((String)null, new FileInputStream(FileDescriptor.in), System.out, (Terminal)null);
   }

   public ConsoleReader(InputStream in, OutputStream out) throws IOException {
      this((String)null, in, out, (Terminal)null);
   }

   public ConsoleReader(InputStream in, OutputStream out, Terminal term) throws IOException {
      this((String)null, in, out, term);
   }

   public ConsoleReader(@Nullable String appName, InputStream in, OutputStream out, @Nullable Terminal term) throws IOException {
      this(appName, in, out, term, (String)null);
   }

   public ConsoleReader(@Nullable String appName, InputStream in, OutputStream out, @Nullable Terminal term, @Nullable String encoding) throws IOException {
      this.buf = new CursorBuffer();
      this.expandEvents = true;
      this.bellEnabled = !Configuration.getBoolean("jline.nobell", true);
      this.handleUserInterrupt = false;
      this.searchTerm = null;
      this.previousSearchTerm = "";
      this.searchIndex = -1;
      this.parenBlinkTimeout = 500;
      this.charSearchChar = 0;
      this.charSearchLastInvokeChar = 0;
      this.charSearchFirstInvokeChar = 0;
      this.yankBuffer = "";
      this.macro = "";
      this.commentBegin = null;
      this.skipLF = false;
      this.copyPasteDetection = false;
      this.state = ConsoleReader.State.NORMAL;
      this.completers = new LinkedList();
      this.completionHandler = new CandidateListCompletionHandler();
      this.autoprintThreshold = Configuration.getInteger("jline.completion.threshold", 100);
      this.history = new MemoryHistory();
      this.historyEnabled = true;
      this.triggeredActions = new HashMap();
      this.appName = appName != null ? appName : "JLine";
      this.encoding = encoding != null ? encoding : Configuration.getEncoding();
      this.terminal = term != null ? term : TerminalFactory.get();
      this.out = new OutputStreamWriter(this.terminal.wrapOutIfNeeded(out), this.encoding);
      this.setInput(in);
      this.inputrcUrl = this.getInputRc();
      this.consoleKeys = new ConsoleKeys(appName, this.inputrcUrl);
   }

   private URL getInputRc() throws IOException {
      String path = Configuration.getString("jline.inputrc");
      if (path == null) {
         File f = new File(Configuration.getUserHome(), ".inputrc");
         if (!f.exists()) {
            f = new File("/etc/inputrc");
         }

         return f.toURI().toURL();
      } else {
         return Urls.create(path);
      }
   }

   public KeyMap getKeys() {
      return this.consoleKeys.getKeys();
   }

   void setInput(InputStream in) throws IOException {
      this.escapeTimeout = Configuration.getLong("jline.esc.timeout", 100L);
      this.isUnitTestInput = in instanceof ByteArrayInputStream;
      boolean nonBlockingEnabled = this.escapeTimeout > 0L && this.terminal.isSupported() && in != null;
      if (this.in != null) {
         this.in.shutdown();
      }

      InputStream wrapped = this.terminal.wrapInIfNeeded(in);
      this.in = new NonBlockingInputStream(wrapped, nonBlockingEnabled);
      this.reader = new InputStreamReader(this.in, this.encoding);
   }

   public void shutdown() {
      if (this.in != null) {
         this.in.shutdown();
      }

   }

   protected void finalize() throws Throwable {
      try {
         this.shutdown();
      } finally {
         super.finalize();
      }

   }

   public InputStream getInput() {
      return this.in;
   }

   public Writer getOutput() {
      return this.out;
   }

   public Terminal getTerminal() {
      return this.terminal;
   }

   public CursorBuffer getCursorBuffer() {
      return this.buf;
   }

   public void setExpandEvents(boolean expand) {
      this.expandEvents = expand;
   }

   public boolean getExpandEvents() {
      return this.expandEvents;
   }

   public void setCopyPasteDetection(boolean onoff) {
      this.copyPasteDetection = onoff;
   }

   public boolean isCopyPasteDetectionEnabled() {
      return this.copyPasteDetection;
   }

   public void setBellEnabled(boolean enabled) {
      this.bellEnabled = enabled;
   }

   public boolean getBellEnabled() {
      return this.bellEnabled;
   }

   public void setHandleUserInterrupt(boolean enabled) {
      this.handleUserInterrupt = enabled;
   }

   public boolean getHandleUserInterrupt() {
      return this.handleUserInterrupt;
   }

   public void setCommentBegin(String commentBegin) {
      this.commentBegin = commentBegin;
   }

   public String getCommentBegin() {
      String str = this.commentBegin;
      if (str == null) {
         str = this.consoleKeys.getVariable("comment-begin");
         if (str == null) {
            str = "#";
         }
      }

      return str;
   }

   public void setPrompt(String prompt) {
      this.prompt = prompt;
      this.promptLen = prompt == null ? 0 : this.stripAnsi(this.lastLine(prompt)).length();
   }

   public String getPrompt() {
      return this.prompt;
   }

   public void setEchoCharacter(Character c) {
      this.echoCharacter = c;
   }

   public Character getEchoCharacter() {
      return this.echoCharacter;
   }

   protected final boolean resetLine() throws IOException {
      if (this.buf.cursor == 0) {
         return false;
      } else {
         this.backspaceAll();
         return true;
      }
   }

   int getCursorPosition() {
      return this.promptLen + this.buf.cursor;
   }

   private String lastLine(String str) {
      if (str == null) {
         return "";
      } else {
         int last = str.lastIndexOf("\n");
         return last >= 0 ? str.substring(last + 1, str.length()) : str;
      }
   }

   private String stripAnsi(String str) {
      if (str == null) {
         return "";
      } else {
         try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            AnsiOutputStream aos = new AnsiOutputStream(baos);
            aos.write(str.getBytes());
            aos.flush();
            return baos.toString();
         } catch (IOException var4) {
            return str;
         }
      }
   }

   public final boolean setCursorPosition(int position) throws IOException {
      if (position == this.buf.cursor) {
         return true;
      } else {
         return this.moveCursor(position - this.buf.cursor) != 0;
      }
   }

   private void setBuffer(String buffer) throws IOException {
      if (!buffer.equals(this.buf.buffer.toString())) {
         int sameIndex = 0;
         int diff = 0;
         int l1 = buffer.length();

         for(int l2 = this.buf.buffer.length(); diff < l1 && diff < l2 && buffer.charAt(diff) == this.buf.buffer.charAt(diff); ++diff) {
            ++sameIndex;
         }

         diff = this.buf.cursor - sameIndex;
         if (diff < 0) {
            this.moveToEnd();
            diff = this.buf.buffer.length() - sameIndex;
         }

         this.backspace(diff);
         this.killLine();
         this.buf.buffer.setLength(sameIndex);
         this.putString(buffer.substring(sameIndex));
      }
   }

   private void setBuffer(CharSequence buffer) throws IOException {
      this.setBuffer(String.valueOf(buffer));
   }

   public final void drawLine() throws IOException {
      String prompt = this.getPrompt();
      if (prompt != null) {
         this.print((CharSequence)prompt);
      }

      this.print((CharSequence)this.buf.buffer.toString());
      if (this.buf.length() != this.buf.cursor) {
         this.back(this.buf.length() - this.buf.cursor - 1);
      }

      this.drawBuffer();
   }

   public final void redrawLine() throws IOException {
      this.print(13);
      this.drawLine();
   }

   final String finishBuffer() throws IOException {
      String str = this.buf.buffer.toString();
      String historyLine = str;
      if (this.expandEvents) {
         str = this.expandEvents(str);
         historyLine = str.replace("!", "\\!");
         historyLine = historyLine.replaceAll("^\\^", "\\\\^");
      }

      if (str.length() > 0) {
         if (this.mask == null && this.isHistoryEnabled()) {
            this.history.add(historyLine);
         } else {
            this.mask = null;
         }
      }

      this.history.moveToEnd();
      this.buf.buffer.setLength(0);
      this.buf.cursor = 0;
      return str;
   }

   protected String expandEvents(String str) throws IOException {
      StringBuilder sb = new StringBuilder();

      for(int i = 0; i < str.length(); ++i) {
         char c = str.charAt(i);
         String sc;
         switch(c) {
         case '!':
            if (i + 1 >= str.length()) {
               sb.append(c);
            } else {
               ++i;
               c = str.charAt(i);
               boolean neg = false;
               String rep = null;
               int i1;
               int idx;
               switch(c) {
               case '\t':
               case ' ':
                  sb.append('!');
                  sb.append(c);
                  break;
               case '\n':
               case '\u000b':
               case '\f':
               case '\r':
               case '\u000e':
               case '\u000f':
               case '\u0010':
               case '\u0011':
               case '\u0012':
               case '\u0013':
               case '\u0014':
               case '\u0015':
               case '\u0016':
               case '\u0017':
               case '\u0018':
               case '\u0019':
               case '\u001a':
               case '\u001b':
               case '\u001c':
               case '\u001d':
               case '\u001e':
               case '\u001f':
               case '"':
               case '$':
               case '%':
               case '&':
               case '\'':
               case '(':
               case ')':
               case '*':
               case '+':
               case ',':
               case '.':
               case '/':
               case ':':
               case ';':
               case '<':
               case '=':
               case '>':
               default:
                  String ss = str.substring(i);
                  i = str.length();
                  idx = this.searchBackwards(ss, this.history.index(), true);
                  if (idx < 0) {
                     throw new IllegalArgumentException("!" + ss + ": event not found");
                  }

                  rep = this.history.get(idx).toString();
                  break;
               case '!':
                  if (this.history.size() == 0) {
                     throw new IllegalArgumentException("!!: event not found");
                  }

                  rep = this.history.get(this.history.index() - 1).toString();
                  break;
               case '#':
                  sb.append(sb.toString());
                  break;
               case '-':
                  neg = true;
                  ++i;
               case '0':
               case '1':
               case '2':
               case '3':
               case '4':
               case '5':
               case '6':
               case '7':
               case '8':
               case '9':
                  for(i1 = i; i < str.length(); ++i) {
                     c = str.charAt(i);
                     if (c < '0' || c > '9') {
                        break;
                     }
                  }

                  boolean var18 = false;

                  try {
                     idx = Integer.parseInt(str.substring(i1, i));
                  } catch (NumberFormatException var11) {
                     throw new IllegalArgumentException((neg ? "!-" : "!") + str.substring(i1, i) + ": event not found");
                  }

                  if (neg) {
                     if (idx <= 0 || idx > this.history.size()) {
                        throw new IllegalArgumentException((neg ? "!-" : "!") + str.substring(i1, i) + ": event not found");
                     }

                     rep = this.history.get(this.history.index() - idx).toString();
                  } else {
                     if (idx <= this.history.index() - this.history.size() || idx > this.history.index()) {
                        throw new IllegalArgumentException((neg ? "!-" : "!") + str.substring(i1, i) + ": event not found");
                     }

                     rep = this.history.get(idx - 1).toString();
                  }
                  break;
               case '?':
                  i1 = str.indexOf(63, i + 1);
                  if (i1 < 0) {
                     i1 = str.length();
                  }

                  sc = str.substring(i + 1, i1);
                  i = i1;
                  idx = this.searchBackwards(sc);
                  if (idx < 0) {
                     throw new IllegalArgumentException("!?" + sc + ": event not found");
                  }

                  rep = this.history.get(idx).toString();
               }

               if (rep != null) {
                  sb.append(rep);
               }
            }
            break;
         case '\\':
            if (i + 1 < str.length()) {
               char nextChar = str.charAt(i + 1);
               if (nextChar == '!' || nextChar == '^' && i == 0) {
                  c = nextChar;
                  ++i;
               }
            }

            sb.append(c);
            break;
         case '^':
            if (i == 0) {
               int i1 = str.indexOf(94, i + 1);
               int i2 = str.indexOf(94, i1 + 1);
               if (i2 < 0) {
                  i2 = str.length();
               }

               if (i1 > 0 && i2 > 0) {
                  String s1 = str.substring(i + 1, i1);
                  String s2 = str.substring(i1 + 1, i2);
                  sc = this.history.get(this.history.index() - 1).toString().replace(s1, s2);
                  sb.append(sc);
                  i = i2 + 1;
                  break;
               }
            }

            sb.append(c);
            break;
         default:
            sb.append(c);
         }
      }

      String result = sb.toString();
      if (!str.equals(result)) {
         this.print((CharSequence)result);
         this.println();
         this.flush();
      }

      return result;
   }

   public final void putString(CharSequence str) throws IOException {
      this.buf.write(str);
      if (this.mask == null) {
         this.print(str);
      } else if (this.mask != 0) {
         this.print(this.mask, str.length());
      }

      this.drawBuffer();
   }

   private void drawBuffer(int clear) throws IOException {
      if (this.buf.cursor != this.buf.length() || clear != 0) {
         char[] chars = this.buf.buffer.substring(this.buf.cursor).toCharArray();
         if (this.mask != null) {
            Arrays.fill(chars, this.mask);
         }

         if (this.terminal.hasWeirdWrap()) {
            int width = this.terminal.getWidth();
            int pos = this.getCursorPosition();

            for(int i = 0; i < chars.length; ++i) {
               this.print(chars[i]);
               if ((pos + i + 1) % width == 0) {
                  this.print(32);
                  this.print(13);
               }
            }
         } else {
            this.print(chars);
         }

         this.clearAhead(clear, chars.length);
         if (this.terminal.isAnsiSupported()) {
            if (chars.length > 0) {
               this.back(chars.length);
            }
         } else {
            this.back(chars.length);
         }
      }

      if (this.terminal.hasWeirdWrap()) {
         int width = this.terminal.getWidth();
         if (this.getCursorPosition() > 0 && this.getCursorPosition() % width == 0 && this.buf.cursor == this.buf.length() && clear == 0) {
            this.print(32);
            this.print(13);
         }
      }

   }

   private void drawBuffer() throws IOException {
      this.drawBuffer(0);
   }

   private void clearAhead(int num, int delta) throws IOException {
      if (num != 0) {
         if (!this.terminal.isAnsiSupported()) {
            this.print(' ', num);
            this.back(num);
         } else {
            int width = this.terminal.getWidth();
            int screenCursorCol = this.getCursorPosition() + delta;
            this.printAnsiSequence("K");
            int curCol = screenCursorCol % width;
            int endCol = (screenCursorCol + num - 1) % width;
            int lines = num / width;
            if (endCol < curCol) {
               ++lines;
            }

            int i;
            for(i = 0; i < lines; ++i) {
               this.printAnsiSequence("B");
               this.printAnsiSequence("2K");
            }

            for(i = 0; i < lines; ++i) {
               this.printAnsiSequence("A");
            }

         }
      }
   }

   protected void back(int num) throws IOException {
      if (num != 0) {
         if (this.terminal.isAnsiSupported()) {
            int width = this.getTerminal().getWidth();
            int cursor = this.getCursorPosition();
            int realCursor = cursor + num;
            int realCol = realCursor % width;
            int newCol = cursor % width;
            int moveup = num / width;
            int delta = realCol - newCol;
            if (delta < 0) {
               ++moveup;
            }

            if (moveup > 0) {
               this.printAnsiSequence(moveup + "A");
            }

            this.printAnsiSequence(1 + newCol + "G");
         } else {
            this.print('\b', num);
         }
      }
   }

   public void flush() throws IOException {
      this.out.flush();
   }

   private int backspaceAll() throws IOException {
      return this.backspace(Integer.MAX_VALUE);
   }

   private int backspace(int num) throws IOException {
      if (this.buf.cursor == 0) {
         return 0;
      } else {
         int count = false;
         int termwidth = this.getTerminal().getWidth();
         int lines = this.getCursorPosition() / termwidth;
         int count = this.moveCursor(-1 * num) * -1;
         this.buf.buffer.delete(this.buf.cursor, this.buf.cursor + count);
         if (this.getCursorPosition() / termwidth != lines && this.terminal.isAnsiSupported()) {
            this.printAnsiSequence("K");
         }

         this.drawBuffer(count);
         return count;
      }
   }

   public boolean backspace() throws IOException {
      return this.backspace(1) == 1;
   }

   protected boolean moveToEnd() throws IOException {
      if (this.buf.cursor == this.buf.length()) {
         return true;
      } else {
         return this.moveCursor(this.buf.length() - this.buf.cursor) > 0;
      }
   }

   private boolean deleteCurrentCharacter() throws IOException {
      if (this.buf.length() != 0 && this.buf.cursor != this.buf.length()) {
         this.buf.buffer.deleteCharAt(this.buf.cursor);
         this.drawBuffer(1);
         return true;
      } else {
         return false;
      }
   }

   private Operation viDeleteChangeYankToRemap(Operation op) {
      switch(op) {
      case VI_EOF_MAYBE:
      case ABORT:
      case BACKWARD_CHAR:
      case FORWARD_CHAR:
      case END_OF_LINE:
      case VI_MATCH:
      case VI_BEGNNING_OF_LINE_OR_ARG_DIGIT:
      case VI_ARG_DIGIT:
      case VI_PREV_WORD:
      case VI_END_WORD:
      case VI_CHAR_SEARCH:
      case VI_NEXT_WORD:
      case VI_FIRST_PRINT:
      case VI_GOTO_MARK:
      case VI_COLUMN:
      case VI_DELETE_TO:
      case VI_YANK_TO:
      case VI_CHANGE_TO:
         return op;
      default:
         return Operation.VI_MOVEMENT_MODE;
      }
   }

   private boolean viRubout(int count) throws IOException {
      boolean ok = true;

      for(int i = 0; ok && i < count; ++i) {
         ok = this.backspace();
      }

      return ok;
   }

   private boolean viDelete(int count) throws IOException {
      boolean ok = true;

      for(int i = 0; ok && i < count; ++i) {
         ok = this.deleteCurrentCharacter();
      }

      return ok;
   }

   private boolean viChangeCase(int count) throws IOException {
      boolean ok = true;

      for(int i = 0; ok && i < count; ++i) {
         ok = this.buf.cursor < this.buf.buffer.length();
         if (ok) {
            char ch = this.buf.buffer.charAt(this.buf.cursor);
            if (Character.isUpperCase(ch)) {
               ch = Character.toLowerCase(ch);
            } else if (Character.isLowerCase(ch)) {
               ch = Character.toUpperCase(ch);
            }

            this.buf.buffer.setCharAt(this.buf.cursor, ch);
            this.drawBuffer(1);
            this.moveCursor(1);
         }
      }

      return ok;
   }

   private boolean viChangeChar(int count, int c) throws IOException {
      if (c >= 0 && c != 27 && c != 3) {
         boolean ok = true;

         for(int i = 0; ok && i < count; ++i) {
            ok = this.buf.cursor < this.buf.buffer.length();
            if (ok) {
               this.buf.buffer.setCharAt(this.buf.cursor, (char)c);
               this.drawBuffer(1);
               if (i < count - 1) {
                  this.moveCursor(1);
               }
            }
         }

         return ok;
      } else {
         return true;
      }
   }

   private boolean viPreviousWord(int count) throws IOException {
      boolean ok = true;
      if (this.buf.cursor == 0) {
         return false;
      } else {
         int pos = this.buf.cursor - 1;

         for(int i = 0; pos > 0 && i < count; ++i) {
            while(pos > 0 && this.isWhitespace(this.buf.buffer.charAt(pos))) {
               --pos;
            }

            while(pos > 0 && !this.isDelimiter(this.buf.buffer.charAt(pos - 1))) {
               --pos;
            }

            if (pos > 0 && i < count - 1) {
               --pos;
            }
         }

         this.setCursorPosition(pos);
         return ok;
      }
   }

   private boolean viDeleteTo(int startPos, int endPos) throws IOException {
      if (startPos == endPos) {
         return true;
      } else {
         if (endPos < startPos) {
            int tmp = endPos;
            endPos = startPos;
            startPos = tmp;
         }

         this.setCursorPosition(startPos);
         this.buf.cursor = startPos;
         this.buf.buffer.delete(startPos, endPos);
         this.drawBuffer(endPos - startPos);
         return true;
      }
   }

   private boolean viYankTo(int startPos, int endPos) throws IOException {
      int cursorPos = startPos;
      if (endPos < startPos) {
         int tmp = endPos;
         endPos = startPos;
         startPos = tmp;
      }

      if (startPos == endPos) {
         this.yankBuffer = "";
         return true;
      } else {
         this.yankBuffer = this.buf.buffer.substring(startPos, endPos);
         this.setCursorPosition(cursorPos);
         return true;
      }
   }

   private boolean viPut(int count) throws IOException {
      if (this.yankBuffer.length() == 0) {
         return true;
      } else {
         if (this.buf.cursor < this.buf.buffer.length()) {
            this.moveCursor(1);
         }

         for(int i = 0; i < count; ++i) {
            this.putString(this.yankBuffer);
         }

         this.moveCursor(-1);
         return true;
      }
   }

   private boolean viCharSearch(int count, int invokeChar, int ch) throws IOException {
      if (ch >= 0 && invokeChar >= 0) {
         char searchChar = (char)ch;
         if (invokeChar != 59 && invokeChar != 44) {
            this.charSearchChar = searchChar;
            this.charSearchFirstInvokeChar = (char)invokeChar;
         } else {
            if (this.charSearchChar == 0) {
               return false;
            }

            if (this.charSearchLastInvokeChar != ';' && this.charSearchLastInvokeChar != ',') {
               if (invokeChar == 44) {
                  this.charSearchFirstInvokeChar = this.switchCase(this.charSearchFirstInvokeChar);
               }
            } else if (this.charSearchLastInvokeChar != invokeChar) {
               this.charSearchFirstInvokeChar = this.switchCase(this.charSearchFirstInvokeChar);
            }

            searchChar = this.charSearchChar;
         }

         this.charSearchLastInvokeChar = (char)invokeChar;
         boolean isForward = Character.isLowerCase(this.charSearchFirstInvokeChar);
         boolean stopBefore = Character.toLowerCase(this.charSearchFirstInvokeChar) == 't';
         boolean ok = false;
         int pos;
         if (isForward) {
            while(true) {
               while(count-- > 0) {
                  for(pos = this.buf.cursor + 1; pos < this.buf.buffer.length(); ++pos) {
                     if (this.buf.buffer.charAt(pos) == searchChar) {
                        this.setCursorPosition(pos);
                        ok = true;
                        break;
                     }
                  }
               }

               if (ok) {
                  if (stopBefore) {
                     this.moveCursor(-1);
                  }

                  if (this.isInViMoveOperationState()) {
                     this.moveCursor(1);
                  }
               }
               break;
            }
         } else {
            while(true) {
               while(count-- > 0) {
                  for(pos = this.buf.cursor - 1; pos >= 0; --pos) {
                     if (this.buf.buffer.charAt(pos) == searchChar) {
                        this.setCursorPosition(pos);
                        ok = true;
                        break;
                     }
                  }
               }

               if (ok && stopBefore) {
                  this.moveCursor(1);
               }
               break;
            }
         }

         return ok;
      } else {
         return false;
      }
   }

   private char switchCase(char ch) {
      return Character.isUpperCase(ch) ? Character.toLowerCase(ch) : Character.toUpperCase(ch);
   }

   private final boolean isInViMoveOperationState() {
      return this.state == ConsoleReader.State.VI_CHANGE_TO || this.state == ConsoleReader.State.VI_DELETE_TO || this.state == ConsoleReader.State.VI_YANK_TO;
   }

   private boolean viNextWord(int count) throws IOException {
      int pos = this.buf.cursor;
      int end = this.buf.buffer.length();

      for(int i = 0; pos < end && i < count; ++i) {
         while(pos < end && !this.isDelimiter(this.buf.buffer.charAt(pos))) {
            ++pos;
         }

         if (i < count - 1 || this.state != ConsoleReader.State.VI_CHANGE_TO) {
            while(pos < end && this.isDelimiter(this.buf.buffer.charAt(pos))) {
               ++pos;
            }
         }
      }

      this.setCursorPosition(pos);
      return true;
   }

   private boolean viEndWord(int count) throws IOException {
      int pos = this.buf.cursor;
      int end = this.buf.buffer.length();

      for(int i = 0; pos < end && i < count; ++i) {
         if (pos < end - 1 && !this.isDelimiter(this.buf.buffer.charAt(pos)) && this.isDelimiter(this.buf.buffer.charAt(pos + 1))) {
            ++pos;
         }

         while(pos < end && this.isDelimiter(this.buf.buffer.charAt(pos))) {
            ++pos;
         }

         while(pos < end - 1 && !this.isDelimiter(this.buf.buffer.charAt(pos + 1))) {
            ++pos;
         }
      }

      this.setCursorPosition(pos);
      return true;
   }

   private boolean previousWord() throws IOException {
      while(this.isDelimiter(this.buf.current()) && this.moveCursor(-1) != 0) {
      }

      while(!this.isDelimiter(this.buf.current()) && this.moveCursor(-1) != 0) {
      }

      return true;
   }

   private boolean nextWord() throws IOException {
      while(this.isDelimiter(this.buf.nextChar()) && this.moveCursor(1) != 0) {
      }

      while(!this.isDelimiter(this.buf.nextChar()) && this.moveCursor(1) != 0) {
      }

      return true;
   }

   private boolean unixWordRubout(int count) throws IOException {
      while(count > 0) {
         if (this.buf.cursor == 0) {
            return false;
         }

         while(this.isWhitespace(this.buf.current()) && this.backspace()) {
         }

         while(!this.isWhitespace(this.buf.current()) && this.backspace()) {
         }

         --count;
      }

      return true;
   }

   private String insertComment(boolean isViMode) throws IOException {
      String comment = this.getCommentBegin();
      this.setCursorPosition(0);
      this.putString(comment);
      if (isViMode) {
         this.consoleKeys.setKeyMap("vi-insert");
      }

      return this.accept();
   }

   private boolean insert(int count, CharSequence str) throws IOException {
      for(int i = 0; i < count; ++i) {
         this.buf.write(str);
         if (this.mask == null) {
            this.print(str);
         } else if (this.mask != 0) {
            this.print(this.mask, str.length());
         }
      }

      this.drawBuffer();
      return true;
   }

   private int viSearch(char searchChar) throws IOException {
      boolean isForward = searchChar == '/';
      CursorBuffer origBuffer = this.buf.copy();
      this.setCursorPosition(0);
      this.killLine();
      this.putString(Character.toString(searchChar));
      this.flush();
      boolean isAborted = false;
      boolean isComplete = false;

      int ch;
      for(ch = -1; !isAborted && !isComplete && (ch = this.readCharacter()) != -1; this.flush()) {
         switch(ch) {
         case 8:
         case 127:
            this.backspace();
            if (this.buf.cursor == 0) {
               isAborted = true;
            }
            break;
         case 10:
         case 13:
            isComplete = true;
            break;
         case 27:
            isAborted = true;
            break;
         default:
            this.putString(Character.toString((char)ch));
         }
      }

      if (ch != -1 && !isAborted) {
         String searchTerm = this.buf.buffer.substring(1);
         int idx = -1;
         int end = this.history.index();
         int start = end <= this.history.size() ? 0 : end - this.history.size();
         int i;
         if (isForward) {
            for(i = start; i < end; ++i) {
               if (this.history.get(i).toString().contains(searchTerm)) {
                  idx = i;
                  break;
               }
            }
         } else {
            for(i = end - 1; i >= start; --i) {
               if (this.history.get(i).toString().contains(searchTerm)) {
                  idx = i;
                  break;
               }
            }
         }

         if (idx == -1) {
            this.setCursorPosition(0);
            this.killLine();
            this.putString(origBuffer.buffer);
            this.setCursorPosition(0);
            return -1;
         } else {
            this.setCursorPosition(0);
            this.killLine();
            this.putString(this.history.get(idx));
            this.setCursorPosition(0);
            this.flush();

            for(isComplete = false; !isComplete && (ch = this.readCharacter()) != -1; this.flush()) {
               boolean forward = isForward;
               switch(ch) {
               case 80:
               case 112:
                  forward = !isForward;
               case 78:
               case 110:
                  boolean isMatch = false;
                  int i;
                  if (forward) {
                     for(i = idx + 1; !isMatch && i < end; ++i) {
                        if (this.history.get(i).toString().contains(searchTerm)) {
                           idx = i;
                           isMatch = true;
                        }
                     }
                  } else {
                     for(i = idx - 1; !isMatch && i >= start; --i) {
                        if (this.history.get(i).toString().contains(searchTerm)) {
                           idx = i;
                           isMatch = true;
                        }
                     }
                  }

                  if (isMatch) {
                     this.setCursorPosition(0);
                     this.killLine();
                     this.putString(this.history.get(idx));
                     this.setCursorPosition(0);
                  }
                  break;
               default:
                  isComplete = true;
               }
            }

            return ch;
         }
      } else {
         this.setCursorPosition(0);
         this.killLine();
         this.putString(origBuffer.buffer);
         this.setCursorPosition(origBuffer.cursor);
         return -1;
      }
   }

   public void setParenBlinkTimeout(int timeout) {
      this.parenBlinkTimeout = timeout;
   }

   private void insertClose(String s) throws IOException {
      this.putString(s);
      int closePosition = this.buf.cursor;
      this.moveCursor(-1);
      this.viMatch();
      if (this.in.isNonBlockingEnabled()) {
         this.in.peek((long)this.parenBlinkTimeout);
      }

      this.setCursorPosition(closePosition);
   }

   private boolean viMatch() throws IOException {
      int pos = this.buf.cursor;
      if (pos == this.buf.length()) {
         return false;
      } else {
         int type = this.getBracketType(this.buf.buffer.charAt(pos));
         int move = type < 0 ? -1 : 1;
         int count = 1;
         if (type == 0) {
            return false;
         } else {
            while(count > 0) {
               pos += move;
               if (pos < 0 || pos >= this.buf.buffer.length()) {
                  return false;
               }

               int curType = this.getBracketType(this.buf.buffer.charAt(pos));
               if (curType == type) {
                  ++count;
               } else if (curType == -type) {
                  --count;
               }
            }

            if (move > 0 && this.isInViMoveOperationState()) {
               ++pos;
            }

            this.setCursorPosition(pos);
            return true;
         }
      }
   }

   private int getBracketType(char ch) {
      switch(ch) {
      case '(':
         return 3;
      case ')':
         return -3;
      case '[':
         return 1;
      case ']':
         return -1;
      case '{':
         return 2;
      case '}':
         return -2;
      default:
         return 0;
      }
   }

   private boolean deletePreviousWord() throws IOException {
      while(this.isDelimiter(this.buf.current()) && this.backspace()) {
      }

      while(!this.isDelimiter(this.buf.current()) && this.backspace()) {
      }

      return true;
   }

   private boolean deleteNextWord() throws IOException {
      while(this.isDelimiter(this.buf.nextChar()) && this.delete()) {
      }

      while(!this.isDelimiter(this.buf.nextChar()) && this.delete()) {
      }

      return true;
   }

   private boolean capitalizeWord() throws IOException {
      boolean first = true;

      int i;
      char c;
      for(i = 1; this.buf.cursor + i - 1 < this.buf.length() && !this.isDelimiter(c = this.buf.buffer.charAt(this.buf.cursor + i - 1)); ++i) {
         this.buf.buffer.setCharAt(this.buf.cursor + i - 1, first ? Character.toUpperCase(c) : Character.toLowerCase(c));
         first = false;
      }

      this.drawBuffer();
      this.moveCursor(i - 1);
      return true;
   }

   private boolean upCaseWord() throws IOException {
      int i;
      char c;
      for(i = 1; this.buf.cursor + i - 1 < this.buf.length() && !this.isDelimiter(c = this.buf.buffer.charAt(this.buf.cursor + i - 1)); ++i) {
         this.buf.buffer.setCharAt(this.buf.cursor + i - 1, Character.toUpperCase(c));
      }

      this.drawBuffer();
      this.moveCursor(i - 1);
      return true;
   }

   private boolean downCaseWord() throws IOException {
      int i;
      char c;
      for(i = 1; this.buf.cursor + i - 1 < this.buf.length() && !this.isDelimiter(c = this.buf.buffer.charAt(this.buf.cursor + i - 1)); ++i) {
         this.buf.buffer.setCharAt(this.buf.cursor + i - 1, Character.toLowerCase(c));
      }

      this.drawBuffer();
      this.moveCursor(i - 1);
      return true;
   }

   private boolean transposeChars(int count) throws IOException {
      while(true) {
         if (count > 0) {
            if (this.buf.cursor != 0 && this.buf.cursor != this.buf.buffer.length()) {
               int first = this.buf.cursor - 1;
               int second = this.buf.cursor;
               char tmp = this.buf.buffer.charAt(first);
               this.buf.buffer.setCharAt(first, this.buf.buffer.charAt(second));
               this.buf.buffer.setCharAt(second, tmp);
               this.moveInternal(-1);
               this.drawBuffer();
               this.moveInternal(2);
               --count;
               continue;
            }

            return false;
         }

         return true;
      }
   }

   public boolean isKeyMap(String name) {
      KeyMap map = this.consoleKeys.getKeys();
      KeyMap mapByName = (KeyMap)this.consoleKeys.getKeyMaps().get(name);
      if (mapByName == null) {
         return false;
      } else {
         return map == mapByName;
      }
   }

   public String accept() throws IOException {
      this.moveToEnd();
      this.println();
      this.flush();
      return this.finishBuffer();
   }

   private void abort() throws IOException {
      this.beep();
      this.buf.clear();
      this.println();
      this.redrawLine();
   }

   public int moveCursor(int num) throws IOException {
      int where = num;
      if (this.buf.cursor == 0 && num <= 0) {
         return 0;
      } else if (this.buf.cursor == this.buf.buffer.length() && num >= 0) {
         return 0;
      } else {
         if (this.buf.cursor + num < 0) {
            where = -this.buf.cursor;
         } else if (this.buf.cursor + num > this.buf.buffer.length()) {
            where = this.buf.buffer.length() - this.buf.cursor;
         }

         this.moveInternal(where);
         return where;
      }
   }

   private void moveInternal(int where) throws IOException {
      CursorBuffer var10000 = this.buf;
      var10000.cursor += where;
      int len;
      int i;
      if (this.terminal.isAnsiSupported()) {
         if (where < 0) {
            this.back(Math.abs(where));
         } else {
            int width = this.getTerminal().getWidth();
            len = this.getCursorPosition();
            i = (len - where) / width;
            int newLine = len / width;
            if (newLine > i) {
               this.printAnsiSequence(newLine - i + "B");
            }

            this.printAnsiSequence(1 + len % width + "G");
         }

      } else if (where < 0) {
         len = 0;

         for(i = this.buf.cursor; i < this.buf.cursor - where; ++i) {
            if (this.buf.buffer.charAt(i) == '\t') {
               len += 4;
            } else {
               ++len;
            }
         }

         char[] chars = new char[len];
         Arrays.fill(chars, '\b');
         this.out.write(chars);
      } else if (this.buf.cursor != 0) {
         if (this.mask != null) {
            char c = this.mask;
            if (this.mask != 0) {
               this.print(c, Math.abs(where));
            }
         } else {
            this.print(this.buf.buffer.substring(this.buf.cursor - where, this.buf.cursor).toCharArray());
         }
      }
   }

   public final boolean replace(int num, String replacement) {
      this.buf.buffer.replace(this.buf.cursor - num, this.buf.cursor, replacement);

      try {
         this.moveCursor(-num);
         this.drawBuffer(Math.max(0, num - replacement.length()));
         this.moveCursor(replacement.length());
         return true;
      } catch (IOException var4) {
         var4.printStackTrace();
         return false;
      }
   }

   public final int readCharacter() throws IOException {
      int c = this.reader.read();
      if (c >= 0) {
         Log.trace("Keystroke: ", c);
         if (this.terminal.isSupported()) {
            this.clearEcho(c);
         }
      }

      return c;
   }

   private int clearEcho(int c) throws IOException {
      if (!this.terminal.isEchoEnabled()) {
         return 0;
      } else {
         int num = this.countEchoCharacters(c);
         this.back(num);
         this.drawBuffer(num);
         return num;
      }
   }

   private int countEchoCharacters(int c) {
      if (c == 9) {
         int tabStop = 8;
         int position = this.getCursorPosition();
         return tabStop - position % tabStop;
      } else {
         return this.getPrintableCharacters(c).length();
      }
   }

   private StringBuilder getPrintableCharacters(int ch) {
      StringBuilder sbuff = new StringBuilder();
      if (ch >= 32) {
         if (ch < 127) {
            sbuff.append(ch);
         } else if (ch == 127) {
            sbuff.append('^');
            sbuff.append('?');
         } else {
            sbuff.append('M');
            sbuff.append('-');
            if (ch >= 160) {
               if (ch < 255) {
                  sbuff.append((char)(ch - 128));
               } else {
                  sbuff.append('^');
                  sbuff.append('?');
               }
            } else {
               sbuff.append('^');
               sbuff.append((char)(ch - 128 + 64));
            }
         }
      } else {
         sbuff.append('^');
         sbuff.append((char)(ch + 64));
      }

      return sbuff;
   }

   public final int readCharacter(char... allowed) throws IOException {
      Arrays.sort(allowed);

      char c;
      while(Arrays.binarySearch(allowed, c = (char)this.readCharacter()) < 0) {
      }

      return c;
   }

   public String readLine() throws IOException {
      return this.readLine((String)null);
   }

   public String readLine(Character mask) throws IOException {
      return this.readLine((String)null, mask);
   }

   public String readLine(String prompt) throws IOException {
      return this.readLine(prompt, (Character)null);
   }

   public boolean setKeyMap(String name) {
      return this.consoleKeys.setKeyMap(name);
   }

   public String getKeyMap() {
      return this.consoleKeys.getKeys().getName();
   }

   public String readLine(String prompt, Character mask) throws IOException {
      int repeatCount = 0;
      this.mask = mask;
      if (prompt != null) {
         this.setPrompt(prompt);
      } else {
         prompt = this.getPrompt();
      }

      try {
         if (!this.terminal.isSupported()) {
            this.beforeReadLine(prompt, mask);
         }

         if (prompt != null && prompt.length() > 0) {
            this.out.write(prompt);
            this.out.flush();
         }

         String originalPrompt;
         if (!this.terminal.isSupported()) {
            originalPrompt = this.readLineSimple();
            return originalPrompt;
         } else {
            if (this.handleUserInterrupt && this.terminal instanceof UnixTerminal) {
               ((UnixTerminal)this.terminal).disableInterruptCharacter();
            }

            originalPrompt = this.prompt;
            this.state = ConsoleReader.State.NORMAL;
            boolean success = true;
            StringBuilder sb = new StringBuilder();
            Stack pushBackChar = new Stack();

            while(true) {
               int c = pushBackChar.isEmpty() ? this.readCharacter() : (Character)pushBackChar.pop();
               Object o;
               if (c == -1) {
                  o = null;
                  return (String)o;
               }

               sb.appendCodePoint(c);
               if (this.recording) {
                  this.macro = this.macro + new String(new int[]{c}, 0, 1);
               }

               o = this.getKeys().getBound(sb);
               if (o == Operation.DO_LOWERCASE_VERSION) {
                  sb.setLength(sb.length() - 1);
                  sb.append(Character.toLowerCase((char)c));
                  o = this.getKeys().getBound(sb);
               }

               if (o instanceof KeyMap) {
                  if (c != 27 || !pushBackChar.isEmpty() || !this.in.isNonBlockingEnabled() || this.in.peek(this.escapeTimeout) != -2) {
                     continue;
                  }

                  o = ((KeyMap)o).getAnotherKey();
                  if (o == null || o instanceof KeyMap) {
                     continue;
                  }

                  sb.setLength(0);
               }

               while(o == null && sb.length() > 0) {
                  c = sb.charAt(sb.length() - 1);
                  sb.setLength(sb.length() - 1);
                  Object o2 = this.getKeys().getBound(sb);
                  if (o2 instanceof KeyMap) {
                     o = ((KeyMap)o2).getAnotherKey();
                     if (o != null) {
                        pushBackChar.push((char)c);
                     }
                  }
               }

               if (o != null) {
                  Log.trace("Binding: ", o);
                  int count;
                  if (o instanceof String) {
                     String macro = (String)o;

                     for(count = 0; count < macro.length(); ++count) {
                        pushBackChar.push(macro.charAt(macro.length() - 1 - count));
                     }

                     sb.setLength(0);
                  } else if (o instanceof ActionListener) {
                     ((ActionListener)o).actionPerformed((ActionEvent)null);
                     sb.setLength(0);
                  } else {
                     if (this.state == ConsoleReader.State.SEARCH || this.state == ConsoleReader.State.FORWARD_SEARCH) {
                        int cursorDest = -1;
                        switch((Operation)o) {
                        case ABORT:
                           this.state = ConsoleReader.State.NORMAL;
                           this.buf.clear();
                           this.buf.buffer.append(this.searchTerm);
                           break;
                        case BACKWARD_CHAR:
                        case FORWARD_CHAR:
                        case END_OF_LINE:
                        case VI_MATCH:
                        case VI_BEGNNING_OF_LINE_OR_ARG_DIGIT:
                        case VI_ARG_DIGIT:
                        case VI_PREV_WORD:
                        case VI_END_WORD:
                        case VI_CHAR_SEARCH:
                        case VI_NEXT_WORD:
                        case VI_FIRST_PRINT:
                        case VI_GOTO_MARK:
                        case VI_COLUMN:
                        case VI_DELETE_TO:
                        case VI_YANK_TO:
                        case VI_CHANGE_TO:
                        default:
                           if (this.searchIndex != -1) {
                              this.history.moveTo(this.searchIndex);
                              cursorDest = this.history.current().toString().indexOf(this.searchTerm.toString());
                           }

                           this.state = ConsoleReader.State.NORMAL;
                           break;
                        case REVERSE_SEARCH_HISTORY:
                        case HISTORY_SEARCH_BACKWARD:
                           this.state = ConsoleReader.State.SEARCH;
                           if (this.searchTerm.length() == 0) {
                              this.searchTerm.append(this.previousSearchTerm);
                           }

                           if (this.searchIndex > 0) {
                              this.searchIndex = this.searchBackwards(this.searchTerm.toString(), this.searchIndex);
                           }
                           break;
                        case FORWARD_SEARCH_HISTORY:
                        case HISTORY_SEARCH_FORWARD:
                           this.state = ConsoleReader.State.FORWARD_SEARCH;
                           if (this.searchTerm.length() == 0) {
                              this.searchTerm.append(this.previousSearchTerm);
                           }

                           if (this.searchIndex > -1 && this.searchIndex < this.history.size() - 1) {
                              this.searchIndex = this.searchForwards(this.searchTerm.toString(), this.searchIndex);
                           }
                           break;
                        case BACKWARD_DELETE_CHAR:
                           if (this.searchTerm.length() > 0) {
                              this.searchTerm.deleteCharAt(this.searchTerm.length() - 1);
                              if (this.state == ConsoleReader.State.SEARCH) {
                                 this.searchIndex = this.searchBackwards(this.searchTerm.toString());
                              } else {
                                 this.searchIndex = this.searchForwards(this.searchTerm.toString());
                              }
                           }
                           break;
                        case SELF_INSERT:
                           this.searchTerm.appendCodePoint(c);
                           if (this.state == ConsoleReader.State.SEARCH) {
                              this.searchIndex = this.searchBackwards(this.searchTerm.toString());
                           } else {
                              this.searchIndex = this.searchForwards(this.searchTerm.toString());
                           }
                        }

                        if (this.state != ConsoleReader.State.SEARCH && this.state != ConsoleReader.State.FORWARD_SEARCH) {
                           this.restoreLine(originalPrompt, cursorDest);
                        } else if (this.searchTerm.length() == 0) {
                           if (this.state == ConsoleReader.State.SEARCH) {
                              this.printSearchStatus("", "");
                           } else {
                              this.printForwardSearchStatus("", "");
                           }

                           this.searchIndex = -1;
                        } else if (this.searchIndex == -1) {
                           this.beep();
                           this.printSearchStatus(this.searchTerm.toString(), "");
                        } else if (this.state == ConsoleReader.State.SEARCH) {
                           this.printSearchStatus(this.searchTerm.toString(), this.history.get(this.searchIndex).toString());
                        } else {
                           this.printForwardSearchStatus(this.searchTerm.toString(), this.history.get(this.searchIndex).toString());
                        }
                     }

                     if (this.state != ConsoleReader.State.SEARCH && this.state != ConsoleReader.State.FORWARD_SEARCH) {
                        boolean isArgDigit = false;
                        count = repeatCount == 0 ? 1 : repeatCount;
                        success = true;
                        if (o instanceof Operation) {
                           Operation op = (Operation)o;
                           int cursorStart = this.buf.cursor;
                           ConsoleReader.State origState = this.state;
                           if (this.state == ConsoleReader.State.VI_CHANGE_TO || this.state == ConsoleReader.State.VI_YANK_TO || this.state == ConsoleReader.State.VI_DELETE_TO) {
                              op = this.viDeleteChangeYankToRemap(op);
                           }

                           int lastChar;
                           String partialLine;
                           switch(op) {
                           case VI_EOF_MAYBE:
                              if (this.buf.buffer.length() == 0) {
                                 partialLine = null;
                                 return partialLine;
                              }

                              partialLine = this.accept();
                              return partialLine;
                           case ABORT:
                              if (this.searchTerm == null) {
                                 this.abort();
                              }
                              break;
                           case BACKWARD_CHAR:
                              success = this.moveCursor(-count) != 0;
                              break;
                           case FORWARD_CHAR:
                              success = this.moveCursor(count) != 0;
                              break;
                           case END_OF_LINE:
                              success = this.moveToEnd();
                              break;
                           case VI_MATCH:
                              success = this.viMatch();
                              break;
                           case VI_BEGNNING_OF_LINE_OR_ARG_DIGIT:
                              if (repeatCount > 0) {
                                 repeatCount = repeatCount * 10 + sb.charAt(0) - 48;
                                 isArgDigit = true;
                              } else {
                                 success = this.setCursorPosition(0);
                              }
                              break;
                           case VI_ARG_DIGIT:
                              repeatCount = repeatCount * 10 + sb.charAt(0) - 48;
                              isArgDigit = true;
                              break;
                           case VI_PREV_WORD:
                              success = this.viPreviousWord(count);
                              break;
                           case VI_END_WORD:
                              success = this.viEndWord(count);
                              break;
                           case VI_CHAR_SEARCH:
                              int searchChar = c != 59 && c != 44 ? (pushBackChar.isEmpty() ? this.readCharacter() : (Character)pushBackChar.pop()) : 0;
                              success = this.viCharSearch(count, c, searchChar);
                              break;
                           case VI_NEXT_WORD:
                              success = this.viNextWord(count);
                           case VI_FIRST_PRINT:
                           case VI_GOTO_MARK:
                           case VI_COLUMN:
                           default:
                              break;
                           case VI_DELETE_TO:
                              if (this.state != ConsoleReader.State.VI_DELETE_TO) {
                                 this.state = ConsoleReader.State.VI_DELETE_TO;
                                 break;
                              }

                              success = this.setCursorPosition(0) && this.killLine();
                              this.state = origState = ConsoleReader.State.NORMAL;
                              break;
                           case VI_YANK_TO:
                              if (this.state == ConsoleReader.State.VI_YANK_TO) {
                                 this.yankBuffer = this.buf.buffer.toString();
                                 this.state = origState = ConsoleReader.State.NORMAL;
                              } else {
                                 this.state = ConsoleReader.State.VI_YANK_TO;
                              }
                              break;
                           case VI_CHANGE_TO:
                              if (this.state != ConsoleReader.State.VI_CHANGE_TO) {
                                 this.state = ConsoleReader.State.VI_CHANGE_TO;
                                 break;
                              }

                              success = this.setCursorPosition(0) && this.killLine();
                              this.state = origState = ConsoleReader.State.NORMAL;
                              this.consoleKeys.setKeyMap("vi-insert");
                              break;
                           case REVERSE_SEARCH_HISTORY:
                           case HISTORY_SEARCH_BACKWARD:
                              if (this.searchTerm != null) {
                                 this.previousSearchTerm = this.searchTerm.toString();
                              }

                              this.searchTerm = new StringBuffer(this.buf.buffer);
                              this.state = ConsoleReader.State.SEARCH;
                              if (this.searchTerm.length() > 0) {
                                 this.searchIndex = this.searchBackwards(this.searchTerm.toString());
                                 if (this.searchIndex == -1) {
                                    this.beep();
                                 }

                                 this.printSearchStatus(this.searchTerm.toString(), this.searchIndex > -1 ? this.history.get(this.searchIndex).toString() : "");
                              } else {
                                 this.searchIndex = -1;
                                 this.printSearchStatus("", "");
                              }
                              break;
                           case FORWARD_SEARCH_HISTORY:
                           case HISTORY_SEARCH_FORWARD:
                              if (this.searchTerm != null) {
                                 this.previousSearchTerm = this.searchTerm.toString();
                              }

                              this.searchTerm = new StringBuffer(this.buf.buffer);
                              this.state = ConsoleReader.State.FORWARD_SEARCH;
                              if (this.searchTerm.length() > 0) {
                                 this.searchIndex = this.searchForwards(this.searchTerm.toString());
                                 if (this.searchIndex == -1) {
                                    this.beep();
                                 }

                                 this.printForwardSearchStatus(this.searchTerm.toString(), this.searchIndex > -1 ? this.history.get(this.searchIndex).toString() : "");
                              } else {
                                 this.searchIndex = -1;
                                 this.printForwardSearchStatus("", "");
                              }
                              break;
                           case BACKWARD_DELETE_CHAR:
                              success = this.backspace();
                              break;
                           case SELF_INSERT:
                              this.putString(sb);
                              break;
                           case COMPLETE:
                              boolean isTabLiteral = false;
                              if (this.copyPasteDetection && c == 9 && (!pushBackChar.isEmpty() || this.in.isNonBlockingEnabled() && this.in.peek(this.escapeTimeout) != -2)) {
                                 isTabLiteral = true;
                              }

                              if (!isTabLiteral) {
                                 success = this.complete();
                              } else {
                                 this.putString(sb);
                              }
                              break;
                           case POSSIBLE_COMPLETIONS:
                              this.printCompletionCandidates();
                              break;
                           case BEGINNING_OF_LINE:
                              success = this.setCursorPosition(0);
                              break;
                           case KILL_LINE:
                              success = this.killLine();
                              break;
                           case KILL_WHOLE_LINE:
                              success = this.setCursorPosition(0) && this.killLine();
                              break;
                           case CLEAR_SCREEN:
                              success = this.clearScreen();
                              break;
                           case OVERWRITE_MODE:
                              this.buf.setOverTyping(!this.buf.isOverTyping());
                              break;
                           case ACCEPT_LINE:
                              partialLine = this.accept();
                              return partialLine;
                           case INTERRUPT:
                              if (this.handleUserInterrupt) {
                                 this.println();
                                 this.flush();
                                 partialLine = this.buf.buffer.toString();
                                 this.buf.clear();
                                 throw new UserInterruptException(partialLine);
                              }
                              break;
                           case VI_MOVE_ACCEPT_LINE:
                              this.consoleKeys.setKeyMap("vi-insert");
                              partialLine = this.accept();
                              return partialLine;
                           case BACKWARD_WORD:
                              success = this.previousWord();
                              break;
                           case FORWARD_WORD:
                              success = this.nextWord();
                              break;
                           case PREVIOUS_HISTORY:
                              success = this.moveHistory(false);
                              break;
                           case VI_PREVIOUS_HISTORY:
                              success = this.moveHistory(false, count) && this.setCursorPosition(0);
                              break;
                           case NEXT_HISTORY:
                              success = this.moveHistory(true);
                              break;
                           case VI_NEXT_HISTORY:
                              success = this.moveHistory(true, count) && this.setCursorPosition(0);
                              break;
                           case EXIT_OR_DELETE_CHAR:
                              if (this.buf.buffer.length() == 0) {
                                 partialLine = null;
                                 return partialLine;
                              }

                              success = this.deleteCurrentCharacter();
                              break;
                           case DELETE_CHAR:
                              success = this.deleteCurrentCharacter();
                              break;
                           case UNIX_LINE_DISCARD:
                              success = this.resetLine();
                              break;
                           case UNIX_WORD_RUBOUT:
                              success = this.unixWordRubout(count);
                              break;
                           case BACKWARD_KILL_WORD:
                              success = this.deletePreviousWord();
                              break;
                           case KILL_WORD:
                              success = this.deleteNextWord();
                              break;
                           case BEGINNING_OF_HISTORY:
                              success = this.history.moveToFirst();
                              if (success) {
                                 this.setBuffer(this.history.current());
                              }
                              break;
                           case END_OF_HISTORY:
                              success = this.history.moveToLast();
                              if (success) {
                                 this.setBuffer(this.history.current());
                              }
                              break;
                           case CAPITALIZE_WORD:
                              success = this.capitalizeWord();
                              break;
                           case UPCASE_WORD:
                              success = this.upCaseWord();
                              break;
                           case DOWNCASE_WORD:
                              success = this.downCaseWord();
                              break;
                           case TAB_INSERT:
                              this.putString("\t");
                              break;
                           case RE_READ_INIT_FILE:
                              this.consoleKeys.loadKeys(this.appName, this.inputrcUrl);
                              break;
                           case START_KBD_MACRO:
                              this.recording = true;
                              break;
                           case END_KBD_MACRO:
                              this.recording = false;
                              this.macro = this.macro.substring(0, this.macro.length() - sb.length());
                              break;
                           case CALL_LAST_KBD_MACRO:
                              for(lastChar = 0; lastChar < this.macro.length(); ++lastChar) {
                                 pushBackChar.push(this.macro.charAt(this.macro.length() - 1 - lastChar));
                              }

                              sb.setLength(0);
                              break;
                           case VI_EDITING_MODE:
                              this.consoleKeys.setKeyMap("vi-insert");
                              break;
                           case VI_MOVEMENT_MODE:
                              ConsoleReader.State var10001 = this.state;
                              if (this.state == ConsoleReader.State.NORMAL) {
                                 this.moveCursor(-1);
                              }

                              this.consoleKeys.setKeyMap("vi-move");
                              break;
                           case VI_INSERTION_MODE:
                              this.consoleKeys.setKeyMap("vi-insert");
                              break;
                           case VI_APPEND_MODE:
                              this.moveCursor(1);
                              this.consoleKeys.setKeyMap("vi-insert");
                              break;
                           case VI_APPEND_EOL:
                              success = this.moveToEnd();
                              this.consoleKeys.setKeyMap("vi-insert");
                              break;
                           case TRANSPOSE_CHARS:
                              success = this.transposeChars(count);
                              break;
                           case INSERT_COMMENT:
                              partialLine = this.insertComment(false);
                              return partialLine;
                           case INSERT_CLOSE_CURLY:
                              this.insertClose("}");
                              break;
                           case INSERT_CLOSE_PAREN:
                              this.insertClose(")");
                              break;
                           case INSERT_CLOSE_SQUARE:
                              this.insertClose("]");
                              break;
                           case VI_INSERT_COMMENT:
                              partialLine = this.insertComment(true);
                              return partialLine;
                           case VI_SEARCH:
                              lastChar = this.viSearch(sb.charAt(0));
                              if (lastChar != -1) {
                                 pushBackChar.push((char)lastChar);
                              }
                              break;
                           case VI_INSERT_BEG:
                              success = this.setCursorPosition(0);
                              this.consoleKeys.setKeyMap("vi-insert");
                              break;
                           case VI_RUBOUT:
                              success = this.viRubout(count);
                              break;
                           case VI_DELETE:
                              success = this.viDelete(count);
                              break;
                           case VI_PUT:
                              success = this.viPut(count);
                              break;
                           case VI_CHANGE_CASE:
                              success = this.viChangeCase(count);
                              break;
                           case VI_CHANGE_CHAR:
                              success = this.viChangeChar(count, pushBackChar.isEmpty() ? this.readCharacter() : (Character)pushBackChar.pop());
                              break;
                           case EMACS_EDITING_MODE:
                              this.consoleKeys.setKeyMap("emacs");
                           }

                           if (origState != ConsoleReader.State.NORMAL) {
                              if (origState == ConsoleReader.State.VI_DELETE_TO) {
                                 success = this.viDeleteTo(cursorStart, this.buf.cursor);
                              } else if (origState == ConsoleReader.State.VI_CHANGE_TO) {
                                 success = this.viDeleteTo(cursorStart, this.buf.cursor);
                                 this.consoleKeys.setKeyMap("vi-insert");
                              } else if (origState == ConsoleReader.State.VI_YANK_TO) {
                                 success = this.viYankTo(cursorStart, this.buf.cursor);
                              }

                              this.state = ConsoleReader.State.NORMAL;
                           }

                           if (this.state == ConsoleReader.State.NORMAL && !isArgDigit) {
                              repeatCount = 0;
                           }

                           if (this.state != ConsoleReader.State.SEARCH && this.state != ConsoleReader.State.FORWARD_SEARCH) {
                              this.previousSearchTerm = "";
                              this.searchTerm = null;
                              this.searchIndex = -1;
                           }
                        }
                     }

                     if (!success) {
                        this.beep();
                     }

                     sb.setLength(0);
                     this.flush();
                  }
               }
            }
         }
      } finally {
         if (!this.terminal.isSupported()) {
            this.afterReadLine();
         }

         if (this.handleUserInterrupt && this.terminal instanceof UnixTerminal) {
            ((UnixTerminal)this.terminal).enableInterruptCharacter();
         }

      }
   }

   private String readLineSimple() throws IOException {
      StringBuilder buff = new StringBuilder();
      int i;
      if (this.skipLF) {
         this.skipLF = false;
         i = this.readCharacter();
         if (i == -1 || i == 13) {
            return buff.toString();
         }

         if (i != 10) {
            buff.append((char)i);
         }
      }

      while(true) {
         i = this.readCharacter();
         if (i == -1 && buff.length() == 0) {
            return null;
         }

         if (i == -1 || i == 10) {
            return buff.toString();
         }

         if (i == 13) {
            this.skipLF = true;
            return buff.toString();
         }

         buff.append((char)i);
      }
   }

   public boolean addCompleter(Completer completer) {
      return this.completers.add(completer);
   }

   public boolean removeCompleter(Completer completer) {
      return this.completers.remove(completer);
   }

   public Collection<Completer> getCompleters() {
      return Collections.unmodifiableList(this.completers);
   }

   public void setCompletionHandler(CompletionHandler handler) {
      this.completionHandler = (CompletionHandler)Preconditions.checkNotNull(handler);
   }

   public CompletionHandler getCompletionHandler() {
      return this.completionHandler;
   }

   protected boolean complete() throws IOException {
      if (this.completers.size() == 0) {
         return false;
      } else {
         List<CharSequence> candidates = new LinkedList();
         String bufstr = this.buf.buffer.toString();
         int cursor = this.buf.cursor;
         int position = -1;
         Iterator i$ = this.completers.iterator();

         while(i$.hasNext()) {
            Completer comp = (Completer)i$.next();
            if ((position = comp.complete(bufstr, cursor, candidates)) != -1) {
               break;
            }
         }

         return candidates.size() != 0 && this.getCompletionHandler().complete(this, candidates, position);
      }
   }

   protected void printCompletionCandidates() throws IOException {
      if (this.completers.size() != 0) {
         List<CharSequence> candidates = new LinkedList();
         String bufstr = this.buf.buffer.toString();
         int cursor = this.buf.cursor;
         Iterator i$ = this.completers.iterator();

         while(i$.hasNext()) {
            Completer comp = (Completer)i$.next();
            if (comp.complete(bufstr, cursor, candidates) != -1) {
               break;
            }
         }

         CandidateListCompletionHandler.printCandidates(this, candidates);
         this.drawLine();
      }
   }

   public void setAutoprintThreshold(int threshold) {
      this.autoprintThreshold = threshold;
   }

   public int getAutoprintThreshold() {
      return this.autoprintThreshold;
   }

   public void setPaginationEnabled(boolean enabled) {
      this.paginationEnabled = enabled;
   }

   public boolean isPaginationEnabled() {
      return this.paginationEnabled;
   }

   public void setHistory(History history) {
      this.history = history;
   }

   public History getHistory() {
      return this.history;
   }

   public void setHistoryEnabled(boolean enabled) {
      this.historyEnabled = enabled;
   }

   public boolean isHistoryEnabled() {
      return this.historyEnabled;
   }

   private boolean moveHistory(boolean next, int count) throws IOException {
      boolean ok = true;

      for(int i = 0; i < count && (ok = this.moveHistory(next)); ++i) {
      }

      return ok;
   }

   private boolean moveHistory(boolean next) throws IOException {
      if (next && !this.history.next()) {
         return false;
      } else if (!next && !this.history.previous()) {
         return false;
      } else {
         this.setBuffer(this.history.current());
         return true;
      }
   }

   private void print(int c) throws IOException {
      if (c == 9) {
         char[] chars = new char[4];
         Arrays.fill(chars, ' ');
         this.out.write(chars);
      } else {
         this.out.write(c);
      }
   }

   private void print(char... buff) throws IOException {
      int len = 0;
      char[] chars = buff;
      int pos = buff.length;

      for(int i$ = 0; i$ < pos; ++i$) {
         char c = chars[i$];
         if (c == '\t') {
            len += 4;
         } else {
            ++len;
         }
      }

      if (len == buff.length) {
         chars = buff;
      } else {
         chars = new char[len];
         pos = 0;
         char[] arr$ = buff;
         int len$ = buff.length;

         for(int i$ = 0; i$ < len$; ++i$) {
            char c = arr$[i$];
            if (c == '\t') {
               Arrays.fill(chars, pos, pos + 4, ' ');
               pos += 4;
            } else {
               chars[pos] = c;
               ++pos;
            }
         }
      }

      this.out.write(chars);
   }

   private void print(char c, int num) throws IOException {
      if (num == 1) {
         this.print(c);
      } else {
         char[] chars = new char[num];
         Arrays.fill(chars, c);
         this.print(chars);
      }

   }

   public final void print(CharSequence s) throws IOException {
      this.print(((CharSequence)Preconditions.checkNotNull(s)).toString().toCharArray());
   }

   public final void println(CharSequence s) throws IOException {
      this.print(((CharSequence)Preconditions.checkNotNull(s)).toString().toCharArray());
      this.println();
   }

   public final void println() throws IOException {
      this.print((CharSequence)CR);
   }

   public final boolean delete() throws IOException {
      return this.delete(1) == 1;
   }

   private int delete(int num) throws IOException {
      this.buf.buffer.delete(this.buf.cursor, this.buf.cursor + 1);
      this.drawBuffer(1);
      return 1;
   }

   public boolean killLine() throws IOException {
      int cp = this.buf.cursor;
      int len = this.buf.buffer.length();
      if (cp >= len) {
         return false;
      } else {
         int num = this.buf.buffer.length() - cp;
         this.clearAhead(num, 0);

         for(int i = 0; i < num; ++i) {
            this.buf.buffer.deleteCharAt(len - i - 1);
         }

         return true;
      }
   }

   public boolean clearScreen() throws IOException {
      if (!this.terminal.isAnsiSupported()) {
         return false;
      } else {
         this.printAnsiSequence("2J");
         this.printAnsiSequence("1;1H");
         this.redrawLine();
         return true;
      }
   }

   public void beep() throws IOException {
      if (this.bellEnabled) {
         this.print(7);
         this.flush();
      }

   }

   public boolean paste() throws IOException {
      Clipboard clipboard;
      try {
         clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
      } catch (Exception var8) {
         return false;
      }

      if (clipboard == null) {
         return false;
      } else {
         Transferable transferable = clipboard.getContents((Object)null);
         if (transferable == null) {
            return false;
         } else {
            try {
               Object content = transferable.getTransferData(DataFlavor.plainTextFlavor);
               if (content == null) {
                  try {
                     content = (new DataFlavor()).getReaderForText(transferable);
                  } catch (Exception var7) {
                  }
               }

               if (content == null) {
                  return false;
               } else {
                  String value;
                  if (content instanceof Reader) {
                     value = "";

                     String line;
                     for(BufferedReader read = new BufferedReader((Reader)content); (line = read.readLine()) != null; value = value + line) {
                        if (value.length() > 0) {
                           value = value + "\n";
                        }
                     }
                  } else {
                     value = content.toString();
                  }

                  if (value == null) {
                     return true;
                  } else {
                     this.putString(value);
                     return true;
                  }
               }
            } catch (UnsupportedFlavorException var9) {
               Log.error("Paste failed: ", var9);
               return false;
            }
         }
      }
   }

   public void addTriggeredAction(char c, ActionListener listener) {
      this.triggeredActions.put(c, listener);
   }

   public void printColumns(Collection<? extends CharSequence> items) throws IOException {
      if (items != null && !items.isEmpty()) {
         int width = this.getTerminal().getWidth();
         int height = this.getTerminal().getHeight();
         int maxWidth = 0;

         CharSequence item;
         for(Iterator i$ = items.iterator(); i$.hasNext(); maxWidth = Math.max(maxWidth, item.length())) {
            item = (CharSequence)i$.next();
         }

         maxWidth += 3;
         Log.debug("Max width: ", maxWidth);
         int showLines;
         if (this.isPaginationEnabled()) {
            showLines = height - 1;
         } else {
            showLines = Integer.MAX_VALUE;
         }

         StringBuilder buff = new StringBuilder();
         Iterator i$ = items.iterator();

         while(i$.hasNext()) {
            CharSequence item = (CharSequence)i$.next();
            int c;
            if (buff.length() + maxWidth > width) {
               this.println(buff);
               buff.setLength(0);
               --showLines;
               if (showLines == 0) {
                  this.print((CharSequence)resources.getString("DISPLAY_MORE"));
                  this.flush();
                  c = this.readCharacter();
                  if (c != 13 && c != 10) {
                     if (c != 113) {
                        showLines = height - 1;
                     }
                  } else {
                     showLines = 1;
                  }

                  this.back(resources.getString("DISPLAY_MORE").length());
                  if (c == 113) {
                     break;
                  }
               }
            }

            buff.append(item.toString());

            for(c = 0; c < maxWidth - item.length(); ++c) {
               buff.append(' ');
            }
         }

         if (buff.length() > 0) {
            this.println(buff);
         }

      }
   }

   private void beforeReadLine(String prompt, Character mask) {
      if (mask != null && this.maskThread == null) {
         final String fullPrompt = "\r" + prompt + "                 " + "                 " + "                 " + "\r" + prompt;
         this.maskThread = new Thread() {
            public void run() {
               while(!interrupted()) {
                  try {
                     Writer out = ConsoleReader.this.getOutput();
                     out.write(fullPrompt);
                     out.flush();
                     sleep(3L);
                  } catch (IOException var2) {
                     return;
                  } catch (InterruptedException var3) {
                     return;
                  }
               }

            }
         };
         this.maskThread.setPriority(10);
         this.maskThread.setDaemon(true);
         this.maskThread.start();
      }

   }

   private void afterReadLine() {
      if (this.maskThread != null && this.maskThread.isAlive()) {
         this.maskThread.interrupt();
      }

      this.maskThread = null;
   }

   public void resetPromptLine(String prompt, String buffer, int cursorDest) throws IOException {
      this.moveToEnd();
      this.buf.buffer.append(this.prompt);
      int promptLength = 0;
      if (this.prompt != null) {
         promptLength = this.prompt.length();
      }

      CursorBuffer var10000 = this.buf;
      var10000.cursor += promptLength;
      this.setPrompt("");
      this.backspaceAll();
      this.setPrompt(prompt);
      this.redrawLine();
      this.setBuffer(buffer);
      if (cursorDest < 0) {
         cursorDest = buffer.length();
      }

      this.setCursorPosition(cursorDest);
      this.flush();
   }

   public void printSearchStatus(String searchTerm, String match) throws IOException {
      this.printSearchStatus(searchTerm, match, "(reverse-i-search)`");
   }

   public void printForwardSearchStatus(String searchTerm, String match) throws IOException {
      this.printSearchStatus(searchTerm, match, "(i-search)`");
   }

   private void printSearchStatus(String searchTerm, String match, String searchLabel) throws IOException {
      String prompt = searchLabel + searchTerm + "': ";
      int cursorDest = match.indexOf(searchTerm);
      this.resetPromptLine(prompt, match, cursorDest);
   }

   public void restoreLine(String originalPrompt, int cursorDest) throws IOException {
      String prompt = this.lastLine(originalPrompt);
      String buffer = this.buf.buffer.toString();
      this.resetPromptLine(prompt, buffer, cursorDest);
   }

   public int searchBackwards(String searchTerm, int startIndex) {
      return this.searchBackwards(searchTerm, startIndex, false);
   }

   public int searchBackwards(String searchTerm) {
      return this.searchBackwards(searchTerm, this.history.index());
   }

   public int searchBackwards(String searchTerm, int startIndex, boolean startsWith) {
      ListIterator it = this.history.entries(startIndex);

      while(it.hasPrevious()) {
         History.Entry e = (History.Entry)it.previous();
         if (startsWith) {
            if (e.value().toString().startsWith(searchTerm)) {
               return e.index();
            }
         } else if (e.value().toString().contains(searchTerm)) {
            return e.index();
         }
      }

      return -1;
   }

   public int searchForwards(String searchTerm, int startIndex) {
      return this.searchForwards(searchTerm, startIndex, false);
   }

   public int searchForwards(String searchTerm) {
      return this.searchForwards(searchTerm, this.history.index());
   }

   public int searchForwards(String searchTerm, int startIndex, boolean startsWith) {
      ListIterator<History.Entry> it = this.history.entries(startIndex);
      if (this.searchIndex != -1 && it.hasNext()) {
         it.next();
      }

      while(it.hasNext()) {
         History.Entry e = (History.Entry)it.next();
         if (startsWith) {
            if (e.value().toString().startsWith(searchTerm)) {
               return e.index();
            }
         } else if (e.value().toString().contains(searchTerm)) {
            return e.index();
         }
      }

      return -1;
   }

   private boolean isDelimiter(char c) {
      return !Character.isLetterOrDigit(c);
   }

   private boolean isWhitespace(char c) {
      return Character.isWhitespace(c);
   }

   private void printAnsiSequence(String sequence) throws IOException {
      this.print(27);
      this.print(91);
      this.print((CharSequence)sequence);
      this.flush();
   }

   private static enum State {
      NORMAL,
      SEARCH,
      FORWARD_SEARCH,
      VI_YANK_TO,
      VI_DELETE_TO,
      VI_CHANGE_TO;
   }
}
