package org.fusesource.jansi.internal;

import java.io.IOException;

public class WindowsSupport {
   public static String getLastErrorMessage() {
      int errorCode = Kernel32.GetLastError();
      int bufferSize = 160;
      byte[] data = new byte[bufferSize];
      Kernel32.FormatMessageW(Kernel32.FORMAT_MESSAGE_FROM_SYSTEM, 0L, errorCode, 0, data, bufferSize, (long[])null);
      return new String(data);
   }

   public static int readByte() {
      return Kernel32._getch();
   }

   public static int getConsoleMode() {
      long hConsole = Kernel32.GetStdHandle(Kernel32.STD_INPUT_HANDLE);
      if (hConsole == (long)Kernel32.INVALID_HANDLE_VALUE) {
         return -1;
      } else {
         int[] mode = new int[1];
         return Kernel32.GetConsoleMode(hConsole, mode) == 0 ? -1 : mode[0];
      }
   }

   public static void setConsoleMode(int mode) {
      long hConsole = Kernel32.GetStdHandle(Kernel32.STD_INPUT_HANDLE);
      if (hConsole != (long)Kernel32.INVALID_HANDLE_VALUE) {
         Kernel32.SetConsoleMode(hConsole, mode);
      }
   }

   public static int getWindowsTerminalWidth() {
      long outputHandle = Kernel32.GetStdHandle(Kernel32.STD_OUTPUT_HANDLE);
      Kernel32.CONSOLE_SCREEN_BUFFER_INFO info = new Kernel32.CONSOLE_SCREEN_BUFFER_INFO();
      Kernel32.GetConsoleScreenBufferInfo(outputHandle, info);
      return info.windowWidth();
   }

   public static int getWindowsTerminalHeight() {
      long outputHandle = Kernel32.GetStdHandle(Kernel32.STD_OUTPUT_HANDLE);
      Kernel32.CONSOLE_SCREEN_BUFFER_INFO info = new Kernel32.CONSOLE_SCREEN_BUFFER_INFO();
      Kernel32.GetConsoleScreenBufferInfo(outputHandle, info);
      return info.windowHeight();
   }

   public static int writeConsole(String msg) {
      long hConsole = Kernel32.GetStdHandle(Kernel32.STD_OUTPUT_HANDLE);
      if (hConsole == (long)Kernel32.INVALID_HANDLE_VALUE) {
         return 0;
      } else {
         char[] chars = msg.toCharArray();
         int[] written = new int[1];
         return Kernel32.WriteConsoleW(hConsole, chars, chars.length, written, 0L) != 0 ? written[0] : 0;
      }
   }

   public static Kernel32.INPUT_RECORD[] readConsoleInput(int count) throws IOException {
      long hConsole = Kernel32.GetStdHandle(Kernel32.STD_INPUT_HANDLE);
      return hConsole == (long)Kernel32.INVALID_HANDLE_VALUE ? null : Kernel32.readConsoleKeyInput(hConsole, count, false);
   }

   public static Kernel32.INPUT_RECORD[] peekConsoleInput(int count) throws IOException {
      long hConsole = Kernel32.GetStdHandle(Kernel32.STD_INPUT_HANDLE);
      return hConsole == (long)Kernel32.INVALID_HANDLE_VALUE ? null : Kernel32.readConsoleKeyInput(hConsole, count, true);
   }

   public static void flushConsoleInputBuffer() {
      long hConsole = Kernel32.GetStdHandle(Kernel32.STD_INPUT_HANDLE);
      if (hConsole != (long)Kernel32.INVALID_HANDLE_VALUE) {
         Kernel32.FlushConsoleInputBuffer(hConsole);
      }
   }
}
