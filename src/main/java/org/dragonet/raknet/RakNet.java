package org.dragonet.raknet;

public abstract class RakNet {
   public static final String VERSION = "1.1.0";
   public static final byte PROTOCOL = 6;
   public static final byte[] MAGIC = new byte[]{0, -1, -1, 0, -2, -2, -2, -2, -3, -3, -3, -3, 18, 52, 86, 120};
   public static final byte PRIORITY_NORMAL = 0;
   public static final byte PRIORITY_IMMEDIATE = 1;
   public static final byte FLAG_NEED_ACK = 8;
   public static final byte PACKET_ENCAPSULATED = 1;
   public static final byte PACKET_OPEN_SESSION = 2;
   public static final byte PACKET_CLOSE_SESSION = 3;
   public static final byte PACKET_INVALID_SESSION = 4;
   public static final byte PACKET_SEND_QUEUE = 5;
   public static final byte PACKET_ACK_NOTIFICATION = 6;
   public static final byte PACKET_SET_OPTION = 7;
   public static final byte PACKET_RAW = 8;
   public static final byte PACKET_BLOCK_ADDRESS = 9;
   public static final byte PACKET_SHUTDOWN = 126;
   public static final byte PACKET_EMERGENCY_SHUTDOWN = 127;
}
