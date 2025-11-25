package org.dragonet.proxy;

import java.io.File;
import java.io.IOException;
import org.mcstats.Metrics;

public class ServerMetrics extends Metrics {
   private final DragonProxy proxy;

   public ServerMetrics(final DragonProxy proxy) throws IOException {
      super("DragonProxy", "0.0.5");
      this.proxy = proxy;
      Metrics.Graph g = this.createGraph("Extra Data");
      g.addPlotter(new Metrics.Plotter("OnlineMode") {
         public int getValue() {
            if (proxy.getAuthMode().equals("cls")) {
               return 0;
            } else {
               return proxy.getAuthMode().equals("online") ? 1 : 2;
            }
         }
      });
   }

   public String getFullServerVersion() {
      return "0.0.5";
   }

   public int getPlayersOnline() {
      return this.proxy.getSessionRegister().getOnlineCount();
   }

   public File getConfigFile() {
      return new File("statistic.properties");
   }
}
