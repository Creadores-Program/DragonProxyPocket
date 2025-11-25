package io.netty.handler.codec.stomp;

import io.netty.handler.codec.AsciiString;
import io.netty.handler.codec.TextHeaders;

public interface StompHeaders extends TextHeaders {
   AsciiString ACCEPT_VERSION = new AsciiString("accept-version");
   AsciiString HOST = new AsciiString("host");
   AsciiString LOGIN = new AsciiString("login");
   AsciiString PASSCODE = new AsciiString("passcode");
   AsciiString HEART_BEAT = new AsciiString("heart-beat");
   AsciiString VERSION = new AsciiString("version");
   AsciiString SESSION = new AsciiString("session");
   AsciiString SERVER = new AsciiString("server");
   AsciiString DESTINATION = new AsciiString("destination");
   AsciiString ID = new AsciiString("id");
   AsciiString ACK = new AsciiString("ack");
   AsciiString TRANSACTION = new AsciiString("transaction");
   AsciiString RECEIPT = new AsciiString("receipt");
   AsciiString MESSAGE_ID = new AsciiString("message-id");
   AsciiString SUBSCRIPTION = new AsciiString("subscription");
   AsciiString RECEIPT_ID = new AsciiString("receipt-id");
   AsciiString MESSAGE = new AsciiString("message");
   AsciiString CONTENT_LENGTH = new AsciiString("content-length");
   AsciiString CONTENT_TYPE = new AsciiString("content-type");

   StompHeaders add(CharSequence var1, CharSequence var2);

   StompHeaders add(CharSequence var1, Iterable<? extends CharSequence> var2);

   StompHeaders add(CharSequence var1, CharSequence... var2);

   StompHeaders addObject(CharSequence var1, Object var2);

   StompHeaders addObject(CharSequence var1, Iterable<?> var2);

   StompHeaders addObject(CharSequence var1, Object... var2);

   StompHeaders addBoolean(CharSequence var1, boolean var2);

   StompHeaders addByte(CharSequence var1, byte var2);

   StompHeaders addChar(CharSequence var1, char var2);

   StompHeaders addShort(CharSequence var1, short var2);

   StompHeaders addInt(CharSequence var1, int var2);

   StompHeaders addLong(CharSequence var1, long var2);

   StompHeaders addFloat(CharSequence var1, float var2);

   StompHeaders addDouble(CharSequence var1, double var2);

   StompHeaders addTimeMillis(CharSequence var1, long var2);

   StompHeaders add(TextHeaders var1);

   StompHeaders set(CharSequence var1, CharSequence var2);

   StompHeaders set(CharSequence var1, Iterable<? extends CharSequence> var2);

   StompHeaders set(CharSequence var1, CharSequence... var2);

   StompHeaders setObject(CharSequence var1, Object var2);

   StompHeaders setObject(CharSequence var1, Iterable<?> var2);

   StompHeaders setObject(CharSequence var1, Object... var2);

   StompHeaders setBoolean(CharSequence var1, boolean var2);

   StompHeaders setByte(CharSequence var1, byte var2);

   StompHeaders setChar(CharSequence var1, char var2);

   StompHeaders setShort(CharSequence var1, short var2);

   StompHeaders setInt(CharSequence var1, int var2);

   StompHeaders setLong(CharSequence var1, long var2);

   StompHeaders setFloat(CharSequence var1, float var2);

   StompHeaders setDouble(CharSequence var1, double var2);

   StompHeaders setTimeMillis(CharSequence var1, long var2);

   StompHeaders set(TextHeaders var1);

   StompHeaders setAll(TextHeaders var1);

   StompHeaders clear();
}
