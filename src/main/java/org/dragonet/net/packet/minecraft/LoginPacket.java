package org.dragonet.net.packet.minecraft;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;
import org.apache.commons.lang3.ArrayUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.dragonet.net.inf.mcpe.NetworkChannel;
import org.dragonet.proxy.utilities.MCPESkin;
import org.dragonet.proxy.utilities.io.PEBinaryReader;
import org.dragonet.proxy.utilities.io.PEBinaryWriter;

public class LoginPacket extends PEPacket {
   public String username;
   public int protocol;
   public UUID clientUuid;
   public long clientID;
   public String publicKey;
   public String serverAddress;
   public String skinName;
   public MCPESkin skin;

   public LoginPacket(byte[] data) {
      this.setData(data);
   }

   public LoginPacket() {
   }

   public int pid() {
      return 1;
   }

   public void encode() {
      try {
         this.setChannel(NetworkChannel.CHANNEL_PRIORITY);
         ByteArrayOutputStream bos = new ByteArrayOutputStream();
         PEBinaryWriter writer = new PEBinaryWriter(bos);
         writer.writeByte((byte)(this.pid() & 255));
         writer.writeInt(this.protocol);
         this.setData(bos.toByteArray());
      } catch (IOException var3) {
      }

   }

   public void decode() {
      try {
         PEBinaryReader reader = new PEBinaryReader(new ByteArrayInputStream(this.getData()));
         reader.readByte();
         this.protocol = reader.readInt();
         byte[] buff = new byte['ꀀ'];
         int len = reader.readInt();
         Inflater inf = new Inflater();
         inf.setInput(reader.read(len));
         int out = inf.inflate(buff);
         inf.end();
         buff = ArrayUtils.subarray((byte[])buff, 0, out);
         PEBinaryReader readerPayload = new PEBinaryReader(new ByteArrayInputStream(buff));
         readerPayload.switchEndianness();
         int jsonLen = readerPayload.readInt();
         readerPayload.switchEndianness();
         String strJsonData = new String(readerPayload.read(jsonLen), "UTF-8");
         readerPayload.switchEndianness();
         int restLen = readerPayload.readInt();
         readerPayload.switchEndianness();
         String strMetaData = new String(readerPayload.read(restLen), "UTF-8");
         JSONObject map = new JSONObject(strJsonData);
         if (map.length() <= 0 || !map.has("chain") || map.optJSONArray("chain") == null) {
            return;
         }

         String[] chains = this.decodeJsonStringArray(map.getJSONArray("chain"));
         String[] var19 = chains;
         int var11 = chains.length;

         for(int var12 = 0; var12 < var11; ++var12) {
            String token = var19[var12];
            JSONObject map = this.decodeToken(token);
            if (map != null && map.length() != 0) {
               if (map.has("extraData")) {
                  JSONObject extras = map.getJSONObject("extraData");
                  if (extras.has("displayName")) {
                     this.username = extras.getString("displayName");
                  }

                  if (extras.has("identity")) {
                     this.clientUuid = UUID.fromString(extras.getString("identity"));
                  }
               }

               if (map.has("identityPublicKey")) {
                  this.publicKey = map.getString("identityPublicKey");
               }
            }
         }

         map = this.decodeToken(strMetaData);
         if (map.has("ClientRandomId")) {
            this.clientID = map.getLong("ClientRandomId");
         }

         if (map.has("ServerAddress")) {
            this.serverAddress = map.getString("ServerAddress");
         }

         if (map.has("SkinId")) {
            this.skinName = map.getString("SkinId");
         }

         if (map.has("SkinData")) {
            this.skin = new MCPESkin(map.getString("SkinData"), this.skinName);
         }
      } catch (DataFormatException | JSONException | IOException var16) {
         Logger.getLogger(LoginPacket.class.getName()).log(Level.SEVERE, (String)null, var16);
      }

   }

   private JSONObject decodeToken(String token) throws JSONException, IOException {
      String[] base = token.split("\\.");
      String strToken = new String(Base64.getDecoder().decode(base[1]), "UTF-8");
      return base.length < 2 ? null : new JSONObject(strToken);
   }

   private String[] decodeJsonStringArray(JSONArray arr) {
      List<String> lst = new ArrayList();

      for(int i = 0; i < arr.length(); ++i) {
         String s = arr.optString(i);
         if (s != null && !s.isEmpty()) {
            lst.add(s);
         }
      }

      return (String[])lst.toArray(new String[0]);
   }
}
