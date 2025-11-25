package org.spacehq.packetlib;

public interface SessionFactory {
   Session createClientSession(Client var1);

   ConnectionListener createServerListener(Server var1);
}
