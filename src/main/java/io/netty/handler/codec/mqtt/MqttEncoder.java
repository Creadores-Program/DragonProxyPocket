package io.netty.handler.codec.mqtt;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.util.CharsetUtil;
import java.util.Iterator;
import java.util.List;

public class MqttEncoder extends MessageToMessageEncoder<MqttMessage> {
   public static final MqttEncoder DEFAUL_ENCODER = new MqttEncoder();
   private static final byte[] EMPTY = new byte[0];

   protected void encode(ChannelHandlerContext ctx, MqttMessage msg, List<Object> out) throws Exception {
      out.add(doEncode(ctx.alloc(), msg));
   }

   static ByteBuf doEncode(ByteBufAllocator byteBufAllocator, MqttMessage message) {
      switch(message.fixedHeader().messageType()) {
      case CONNECT:
         return encodeConnectMessage(byteBufAllocator, (MqttConnectMessage)message);
      case CONNACK:
         return encodeConnAckMessage(byteBufAllocator, (MqttConnAckMessage)message);
      case PUBLISH:
         return encodePublishMessage(byteBufAllocator, (MqttPublishMessage)message);
      case SUBSCRIBE:
         return encodeSubscribeMessage(byteBufAllocator, (MqttSubscribeMessage)message);
      case UNSUBSCRIBE:
         return encodeUnsubscribeMessage(byteBufAllocator, (MqttUnsubscribeMessage)message);
      case SUBACK:
         return encodeSubAckMessage(byteBufAllocator, (MqttSubAckMessage)message);
      case UNSUBACK:
      case PUBACK:
      case PUBREC:
      case PUBREL:
      case PUBCOMP:
         return encodeMessageWithOnlySingleByteFixedHeaderAndMessageId(byteBufAllocator, message);
      case PINGREQ:
      case PINGRESP:
      case DISCONNECT:
         return encodeMessageWithOnlySingleByteFixedHeader(byteBufAllocator, message);
      default:
         throw new IllegalArgumentException("Unknown message type: " + message.fixedHeader().messageType().value());
      }
   }

   private static ByteBuf encodeConnectMessage(ByteBufAllocator byteBufAllocator, MqttConnectMessage message) {
      int payloadBufferSize = 0;
      MqttFixedHeader mqttFixedHeader = message.fixedHeader();
      MqttConnectVariableHeader variableHeader = message.variableHeader();
      MqttConnectPayload payload = message.payload();
      MqttVersion mqttVersion = MqttVersion.fromProtocolNameAndLevel(variableHeader.name(), (byte)variableHeader.version());
      String clientIdentifier = payload.clientIdentifier();
      if (!MqttCodecUtil.isValidClientId(mqttVersion, clientIdentifier)) {
         throw new MqttIdentifierRejectedException("invalid clientIdentifier: " + clientIdentifier);
      } else {
         byte[] clientIdentifierBytes = encodeStringUtf8(clientIdentifier);
         int payloadBufferSize = payloadBufferSize + 2 + clientIdentifierBytes.length;
         String willTopic = payload.willTopic();
         byte[] willTopicBytes = willTopic != null ? encodeStringUtf8(willTopic) : EMPTY;
         String willMessage = payload.willMessage();
         byte[] willMessageBytes = willMessage != null ? encodeStringUtf8(willMessage) : EMPTY;
         if (variableHeader.isWillFlag()) {
            payloadBufferSize += 2 + willTopicBytes.length;
            payloadBufferSize += 2 + willMessageBytes.length;
         }

         String userName = payload.userName();
         byte[] userNameBytes = userName != null ? encodeStringUtf8(userName) : EMPTY;
         if (variableHeader.hasUserName()) {
            payloadBufferSize += 2 + userNameBytes.length;
         }

         String password = payload.password();
         byte[] passwordBytes = password != null ? encodeStringUtf8(password) : EMPTY;
         if (variableHeader.hasPassword()) {
            payloadBufferSize += 2 + passwordBytes.length;
         }

         byte[] protocolNameBytes = mqttVersion.protocolNameBytes();
         int variableHeaderBufferSize = 2 + protocolNameBytes.length + 4;
         int variablePartSize = variableHeaderBufferSize + payloadBufferSize;
         int fixedHeaderBufferSize = 1 + getVariableLengthInt(variablePartSize);
         ByteBuf buf = byteBufAllocator.buffer(fixedHeaderBufferSize + variablePartSize);
         buf.writeByte(getFixedHeaderByte1(mqttFixedHeader));
         writeVariableLengthInt(buf, variablePartSize);
         buf.writeShort(protocolNameBytes.length);
         buf.writeBytes(protocolNameBytes);
         buf.writeByte(variableHeader.version());
         buf.writeByte(getConnVariableHeaderFlag(variableHeader));
         buf.writeShort(variableHeader.keepAliveTimeSeconds());
         buf.writeShort(clientIdentifierBytes.length);
         buf.writeBytes((byte[])clientIdentifierBytes, 0, clientIdentifierBytes.length);
         if (variableHeader.isWillFlag()) {
            buf.writeShort(willTopicBytes.length);
            buf.writeBytes((byte[])willTopicBytes, 0, willTopicBytes.length);
            buf.writeShort(willMessageBytes.length);
            buf.writeBytes((byte[])willMessageBytes, 0, willMessageBytes.length);
         }

         if (variableHeader.hasUserName()) {
            buf.writeShort(userNameBytes.length);
            buf.writeBytes((byte[])userNameBytes, 0, userNameBytes.length);
         }

         if (variableHeader.hasPassword()) {
            buf.writeShort(passwordBytes.length);
            buf.writeBytes((byte[])passwordBytes, 0, passwordBytes.length);
         }

         return buf;
      }
   }

   private static int getConnVariableHeaderFlag(MqttConnectVariableHeader variableHeader) {
      int flagByte = 0;
      if (variableHeader.hasUserName()) {
         flagByte |= 128;
      }

      if (variableHeader.hasPassword()) {
         flagByte |= 64;
      }

      if (variableHeader.isWillRetain()) {
         flagByte |= 32;
      }

      flagByte |= (variableHeader.willQos() & 3) << 3;
      if (variableHeader.isWillFlag()) {
         flagByte |= 4;
      }

      if (variableHeader.isCleanSession()) {
         flagByte |= 2;
      }

      return flagByte;
   }

   private static ByteBuf encodeConnAckMessage(ByteBufAllocator byteBufAllocator, MqttConnAckMessage message) {
      ByteBuf buf = byteBufAllocator.buffer(4);
      buf.writeByte(getFixedHeaderByte1(message.fixedHeader()));
      buf.writeByte(2);
      buf.writeByte(0);
      buf.writeByte(message.variableHeader().connectReturnCode().byteValue());
      return buf;
   }

   private static ByteBuf encodeSubscribeMessage(ByteBufAllocator byteBufAllocator, MqttSubscribeMessage message) {
      int variableHeaderBufferSize = 2;
      int payloadBufferSize = 0;
      MqttFixedHeader mqttFixedHeader = message.fixedHeader();
      MqttMessageIdVariableHeader variableHeader = message.variableHeader();
      MqttSubscribePayload payload = message.payload();

      for(Iterator i$ = payload.topicSubscriptions().iterator(); i$.hasNext(); ++payloadBufferSize) {
         MqttTopicSubscription topic = (MqttTopicSubscription)i$.next();
         String topicName = topic.topicName();
         byte[] topicNameBytes = encodeStringUtf8(topicName);
         payloadBufferSize += 2 + topicNameBytes.length;
      }

      int variablePartSize = variableHeaderBufferSize + payloadBufferSize;
      int fixedHeaderBufferSize = 1 + getVariableLengthInt(variablePartSize);
      ByteBuf buf = byteBufAllocator.buffer(fixedHeaderBufferSize + variablePartSize);
      buf.writeByte(getFixedHeaderByte1(mqttFixedHeader));
      writeVariableLengthInt(buf, variablePartSize);
      int messageId = variableHeader.messageId();
      buf.writeShort(messageId);
      Iterator i$ = payload.topicSubscriptions().iterator();

      while(i$.hasNext()) {
         MqttTopicSubscription topic = (MqttTopicSubscription)i$.next();
         String topicName = topic.topicName();
         byte[] topicNameBytes = encodeStringUtf8(topicName);
         buf.writeShort(topicNameBytes.length);
         buf.writeBytes((byte[])topicNameBytes, 0, topicNameBytes.length);
         buf.writeByte(topic.qualityOfService().value());
      }

      return buf;
   }

   private static ByteBuf encodeUnsubscribeMessage(ByteBufAllocator byteBufAllocator, MqttUnsubscribeMessage message) {
      int variableHeaderBufferSize = 2;
      int payloadBufferSize = 0;
      MqttFixedHeader mqttFixedHeader = message.fixedHeader();
      MqttMessageIdVariableHeader variableHeader = message.variableHeader();
      MqttUnsubscribePayload payload = message.payload();

      byte[] topicNameBytes;
      for(Iterator i$ = payload.topics().iterator(); i$.hasNext(); payloadBufferSize += 2 + topicNameBytes.length) {
         String topicName = (String)i$.next();
         topicNameBytes = encodeStringUtf8(topicName);
      }

      int variablePartSize = variableHeaderBufferSize + payloadBufferSize;
      int fixedHeaderBufferSize = 1 + getVariableLengthInt(variablePartSize);
      ByteBuf buf = byteBufAllocator.buffer(fixedHeaderBufferSize + variablePartSize);
      buf.writeByte(getFixedHeaderByte1(mqttFixedHeader));
      writeVariableLengthInt(buf, variablePartSize);
      int messageId = variableHeader.messageId();
      buf.writeShort(messageId);
      Iterator i$ = payload.topics().iterator();

      while(i$.hasNext()) {
         String topicName = (String)i$.next();
         byte[] topicNameBytes = encodeStringUtf8(topicName);
         buf.writeShort(topicNameBytes.length);
         buf.writeBytes((byte[])topicNameBytes, 0, topicNameBytes.length);
      }

      return buf;
   }

   private static ByteBuf encodeSubAckMessage(ByteBufAllocator byteBufAllocator, MqttSubAckMessage message) {
      int variableHeaderBufferSize = 2;
      int payloadBufferSize = message.payload().grantedQoSLevels().size();
      int variablePartSize = variableHeaderBufferSize + payloadBufferSize;
      int fixedHeaderBufferSize = 1 + getVariableLengthInt(variablePartSize);
      ByteBuf buf = byteBufAllocator.buffer(fixedHeaderBufferSize + variablePartSize);
      buf.writeByte(getFixedHeaderByte1(message.fixedHeader()));
      writeVariableLengthInt(buf, variablePartSize);
      buf.writeShort(message.variableHeader().messageId());
      Iterator i$ = message.payload().grantedQoSLevels().iterator();

      while(i$.hasNext()) {
         int qos = (Integer)i$.next();
         buf.writeByte(qos);
      }

      return buf;
   }

   private static ByteBuf encodePublishMessage(ByteBufAllocator byteBufAllocator, MqttPublishMessage message) {
      MqttFixedHeader mqttFixedHeader = message.fixedHeader();
      MqttPublishVariableHeader variableHeader = message.variableHeader();
      ByteBuf payload = message.payload().duplicate();
      String topicName = variableHeader.topicName();
      byte[] topicNameBytes = encodeStringUtf8(topicName);
      int variableHeaderBufferSize = 2 + topicNameBytes.length + (mqttFixedHeader.qosLevel().value() > 0 ? 2 : 0);
      int payloadBufferSize = payload.readableBytes();
      int variablePartSize = variableHeaderBufferSize + payloadBufferSize;
      int fixedHeaderBufferSize = 1 + getVariableLengthInt(variablePartSize);
      ByteBuf buf = byteBufAllocator.buffer(fixedHeaderBufferSize + variablePartSize);
      buf.writeByte(getFixedHeaderByte1(mqttFixedHeader));
      writeVariableLengthInt(buf, variablePartSize);
      buf.writeShort(topicNameBytes.length);
      buf.writeBytes(topicNameBytes);
      if (mqttFixedHeader.qosLevel().value() > 0) {
         buf.writeShort(variableHeader.messageId());
      }

      buf.writeBytes(payload);
      return buf;
   }

   private static ByteBuf encodeMessageWithOnlySingleByteFixedHeaderAndMessageId(ByteBufAllocator byteBufAllocator, MqttMessage message) {
      MqttFixedHeader mqttFixedHeader = message.fixedHeader();
      MqttMessageIdVariableHeader variableHeader = (MqttMessageIdVariableHeader)message.variableHeader();
      int msgId = variableHeader.messageId();
      int variableHeaderBufferSize = 2;
      int fixedHeaderBufferSize = 1 + getVariableLengthInt(variableHeaderBufferSize);
      ByteBuf buf = byteBufAllocator.buffer(fixedHeaderBufferSize + variableHeaderBufferSize);
      buf.writeByte(getFixedHeaderByte1(mqttFixedHeader));
      writeVariableLengthInt(buf, variableHeaderBufferSize);
      buf.writeShort(msgId);
      return buf;
   }

   private static ByteBuf encodeMessageWithOnlySingleByteFixedHeader(ByteBufAllocator byteBufAllocator, MqttMessage message) {
      MqttFixedHeader mqttFixedHeader = message.fixedHeader();
      ByteBuf buf = byteBufAllocator.buffer(2);
      buf.writeByte(getFixedHeaderByte1(mqttFixedHeader));
      buf.writeByte(0);
      return buf;
   }

   private static int getFixedHeaderByte1(MqttFixedHeader header) {
      int ret = 0;
      int ret = ret | header.messageType().value() << 4;
      if (header.isDup()) {
         ret |= 8;
      }

      ret |= header.qosLevel().value() << 1;
      if (header.isRetain()) {
         ret |= 1;
      }

      return ret;
   }

   private static void writeVariableLengthInt(ByteBuf buf, int num) {
      do {
         int digit = num % 128;
         num /= 128;
         if (num > 0) {
            digit |= 128;
         }

         buf.writeByte(digit);
      } while(num > 0);

   }

   private static int getVariableLengthInt(int num) {
      int count = 0;

      do {
         num /= 128;
         ++count;
      } while(num > 0);

      return count;
   }

   private static byte[] encodeStringUtf8(String s) {
      return s.getBytes(CharsetUtil.UTF_8);
   }
}
