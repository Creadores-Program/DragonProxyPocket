package org.fusesource.jansi;

import java.io.IOException;
import java.io.OutputStream;
import org.fusesource.jansi.internal.Kernel32;
import org.fusesource.jansi.internal.WindowsSupport;

public final class WindowsAnsiOutputStream extends AnsiOutputStream {
   private static final long console;
   private static final short FOREGROUND_BLACK = 0;
   private static final short FOREGROUND_YELLOW;
   private static final short FOREGROUND_MAGENTA;
   private static final short FOREGROUND_CYAN;
   private static final short FOREGROUND_WHITE;
   private static final short BACKGROUND_BLACK = 0;
   private static final short BACKGROUND_YELLOW;
   private static final short BACKGROUND_MAGENTA;
   private static final short BACKGROUND_CYAN;
   private static final short BACKGROUND_WHITE;
   private static final short[] ANSI_FOREGROUND_COLOR_MAP;
   private static final short[] ANSI_BACKGROUND_COLOR_MAP;
   private final Kernel32.CONSOLE_SCREEN_BUFFER_INFO info = new Kernel32.CONSOLE_SCREEN_BUFFER_INFO();
   private final short originalColors;
   private boolean negative;
   private short savedX = -1;
   private short savedY = -1;

   public WindowsAnsiOutputStream(OutputStream os) throws IOException {
      super(os);
      this.getConsoleInfo();
      this.originalColors = this.info.attributes;
   }

   private void getConsoleInfo() throws IOException {
      this.out.flush();
      if (Kernel32.GetConsoleScreenBufferInfo(console, this.info) == 0) {
         throw new IOException("Could not get the screen info: " + WindowsSupport.getLastErrorMessage());
      } else {
         if (this.negative) {
            this.info.attributes = this.invertAttributeColors(this.info.attributes);
         }

      }
   }

   private void applyAttribute() throws IOException {
      this.out.flush();
      short attributes = this.info.attributes;
      if (this.negative) {
         attributes = this.invertAttributeColors(attributes);
      }

      if (Kernel32.SetConsoleTextAttribute(console, attributes) == 0) {
         throw new IOException(WindowsSupport.getLastErrorMessage());
      }
   }

   private short invertAttributeColors(short attibutes) {
      int fg = 15 & attibutes;
      fg <<= 8;
      int bg = 240 * attibutes;
      bg >>= 8;
      attibutes = (short)(attibutes & '\uff00' | fg | bg);
      return attibutes;
   }

   private void applyCursorPosition() throws IOException {
      if (Kernel32.SetConsoleCursorPosition(console, this.info.cursorPosition.copy()) == 0) {
         throw new IOException(WindowsSupport.getLastErrorMessage());
      }
   }

   protected void processEraseScreen(int eraseOption) throws IOException {
      this.getConsoleInfo();
      int[] written = new int[1];
      switch(eraseOption) {
      case 0:
         int lengthToEnd = (this.info.window.bottom - this.info.cursorPosition.y) * this.info.size.x + (this.info.size.x - this.info.cursorPosition.x);
         Kernel32.FillConsoleOutputCharacterW(console, ' ', lengthToEnd, this.info.cursorPosition.copy(), written);
         break;
      case 1:
         Kernel32.COORD topLeft2 = new Kernel32.COORD();
         topLeft2.x = 0;
         topLeft2.y = this.info.window.top;
         int lengthToCursor = (this.info.cursorPosition.y - this.info.window.top) * this.info.size.x + this.info.cursorPosition.x;
         Kernel32.FillConsoleOutputCharacterW(console, ' ', lengthToCursor, topLeft2, written);
         break;
      case 2:
         Kernel32.COORD topLeft = new Kernel32.COORD();
         topLeft.x = 0;
         topLeft.y = this.info.window.top;
         int screenLength = this.info.window.height() * this.info.size.x;
         Kernel32.FillConsoleOutputCharacterW(console, ' ', screenLength, topLeft, written);
      }

   }

   protected void processEraseLine(int eraseOption) throws IOException {
      this.getConsoleInfo();
      int[] written = new int[1];
      switch(eraseOption) {
      case 0:
         int lengthToLastCol = this.info.size.x - this.info.cursorPosition.x;
         Kernel32.FillConsoleOutputCharacterW(console, ' ', lengthToLastCol, this.info.cursorPosition.copy(), written);
         break;
      case 1:
         Kernel32.COORD leftColCurrRow2 = this.info.cursorPosition.copy();
         leftColCurrRow2.x = 0;
         Kernel32.FillConsoleOutputCharacterW(console, ' ', this.info.cursorPosition.x, leftColCurrRow2, written);
         break;
      case 2:
         Kernel32.COORD leftColCurrRow = this.info.cursorPosition.copy();
         leftColCurrRow.x = 0;
         Kernel32.FillConsoleOutputCharacterW(console, ' ', this.info.size.x, leftColCurrRow, written);
      }

   }

   protected void processCursorLeft(int count) throws IOException {
      this.getConsoleInfo();
      this.info.cursorPosition.x = (short)Math.max(0, this.info.cursorPosition.x - count);
      this.applyCursorPosition();
   }

   protected void processCursorRight(int count) throws IOException {
      this.getConsoleInfo();
      this.info.cursorPosition.x = (short)Math.min(this.info.window.width(), this.info.cursorPosition.x + count);
      this.applyCursorPosition();
   }

   protected void processCursorDown(int count) throws IOException {
      this.getConsoleInfo();
      this.info.cursorPosition.y = (short)Math.min(this.info.size.y, this.info.cursorPosition.y + count);
      this.applyCursorPosition();
   }

   protected void processCursorUp(int count) throws IOException {
      this.getConsoleInfo();
      this.info.cursorPosition.y = (short)Math.max(this.info.window.top, this.info.cursorPosition.y - count);
      this.applyCursorPosition();
   }

   protected void processCursorTo(int row, int col) throws IOException {
      this.getConsoleInfo();
      this.info.cursorPosition.y = (short)Math.max(this.info.window.top, Math.min(this.info.size.y, this.info.window.top + row - 1));
      this.info.cursorPosition.x = (short)Math.max(0, Math.min(this.info.window.width(), col - 1));
      this.applyCursorPosition();
   }

   protected void processCursorToColumn(int x) throws IOException {
      this.getConsoleInfo();
      this.info.cursorPosition.x = (short)Math.max(0, Math.min(this.info.window.width(), x - 1));
      this.applyCursorPosition();
   }

   protected void processSetForegroundColor(int color) throws IOException {
      this.info.attributes = (short)(this.info.attributes & -8 | ANSI_FOREGROUND_COLOR_MAP[color]);
      this.applyAttribute();
   }

   protected void processSetBackgroundColor(int color) throws IOException {
      this.info.attributes = (short)(this.info.attributes & -113 | ANSI_BACKGROUND_COLOR_MAP[color]);
      this.applyAttribute();
   }

   protected void processAttributeRest() throws IOException {
      this.info.attributes = (short)(this.info.attributes & -256 | this.originalColors);
      this.negative = false;
      this.applyAttribute();
   }

   protected void processSetAttribute(int attribute) throws IOException {
      switch(attribute) {
      case 1:
         this.info.attributes |= Kernel32.FOREGROUND_INTENSITY;
         this.applyAttribute();
         break;
      case 4:
         this.info.attributes |= Kernel32.BACKGROUND_INTENSITY;
         this.applyAttribute();
         break;
      case 7:
         this.negative = true;
         this.applyAttribute();
         break;
      case 22:
         this.info.attributes = (short)(this.info.attributes & ~Kernel32.FOREGROUND_INTENSITY);
         this.applyAttribute();
         break;
      case 24:
         this.info.attributes = (short)(this.info.attributes & ~Kernel32.BACKGROUND_INTENSITY);
         this.applyAttribute();
         break;
      case 27:
         this.negative = false;
         this.applyAttribute();
      }

   }

   protected void processSaveCursorPosition() throws IOException {
      this.getConsoleInfo();
      this.savedX = this.info.cursorPosition.x;
      this.savedY = this.info.cursorPosition.y;
   }

   protected void processRestoreCursorPosition() throws IOException {
      if (this.savedX != -1 && this.savedY != -1) {
         this.out.flush();
         this.info.cursorPosition.x = this.savedX;
         this.info.cursorPosition.y = this.savedY;
         this.applyCursorPosition();
      }

   }

   protected void processChangeWindowTitle(String label) {
      Kernel32.SetConsoleTitle(label);
   }

   static {
      console = Kernel32.GetStdHandle(Kernel32.STD_OUTPUT_HANDLE);
      FOREGROUND_YELLOW = (short)(Kernel32.FOREGROUND_RED | Kernel32.FOREGROUND_GREEN);
      FOREGROUND_MAGENTA = (short)(Kernel32.FOREGROUND_BLUE | Kernel32.FOREGROUND_RED);
      FOREGROUND_CYAN = (short)(Kernel32.FOREGROUND_BLUE | Kernel32.FOREGROUND_GREEN);
      FOREGROUND_WHITE = (short)(Kernel32.FOREGROUND_RED | Kernel32.FOREGROUND_GREEN | Kernel32.FOREGROUND_BLUE);
      BACKGROUND_YELLOW = (short)(Kernel32.BACKGROUND_RED | Kernel32.BACKGROUND_GREEN);
      BACKGROUND_MAGENTA = (short)(Kernel32.BACKGROUND_BLUE | Kernel32.BACKGROUND_RED);
      BACKGROUND_CYAN = (short)(Kernel32.BACKGROUND_BLUE | Kernel32.BACKGROUND_GREEN);
      BACKGROUND_WHITE = (short)(Kernel32.BACKGROUND_RED | Kernel32.BACKGROUND_GREEN | Kernel32.BACKGROUND_BLUE);
      ANSI_FOREGROUND_COLOR_MAP = new short[]{0, Kernel32.FOREGROUND_RED, Kernel32.FOREGROUND_GREEN, FOREGROUND_YELLOW, Kernel32.FOREGROUND_BLUE, FOREGROUND_MAGENTA, FOREGROUND_CYAN, FOREGROUND_WHITE};
      ANSI_BACKGROUND_COLOR_MAP = new short[]{0, Kernel32.BACKGROUND_RED, Kernel32.BACKGROUND_GREEN, BACKGROUND_YELLOW, Kernel32.BACKGROUND_BLUE, BACKGROUND_MAGENTA, BACKGROUND_CYAN, BACKGROUND_WHITE};
   }
}
