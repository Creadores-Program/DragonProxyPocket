package org.dragonet.proxy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Logger;
import org.dragonet.proxy.commands.CommandRegister;
import org.dragonet.proxy.configuration.Lang;
import org.dragonet.proxy.configuration.ServerConfig;
import org.dragonet.proxy.network.RaknetInterface;
import org.dragonet.proxy.network.SessionRegister;
import org.mcstats.Metrics;
import org.yaml.snakeyaml.Yaml;

public class DragonProxy {
   public static final boolean IS_RELEASE = true;
   private final Logger logger = Logger.getLogger("DragonProxy");
   private final TickerThread ticker = new TickerThread(this);
   private ServerConfig config;
   private Lang lang;
   private SessionRegister sessionRegister;
   private RaknetInterface network;
   private boolean shuttingDown;
   private ScheduledExecutorService generalThreadPool;
   private CommandRegister commandRegister;
   private String authMode;
   private ConsoleManager console;
   private Metrics metrics;
   private String motd;
   private boolean isDebug = false;

   public static void main(String[] args) {
      (new DragonProxy()).run(args);
   }

   public void run(String[] args) {
      try {
         File fileConfig = new File("config.yml");
         if (!fileConfig.exists()) {
            FileOutputStream fos = new FileOutputStream(fileConfig);
            InputStream ins = DragonProxy.class.getResourceAsStream("/config.yml");
            boolean var5 = true;

            while(true) {
               int data;
               if ((data = ins.read()) == -1) {
                  ins.close();
                  fos.close();
                  break;
               }

               fos.write(data);
            }
         }

         this.config = (ServerConfig)(new Yaml()).loadAs((InputStream)(new FileInputStream(fileConfig)), ServerConfig.class);
      } catch (IOException var7) {
         this.logger.severe("Failed to load configuration file! Make sure the file is writable.");
         var7.printStackTrace();
         return;
      }

      this.console = new ConsoleManager(this);
      this.console.startConsole();
      this.checkArguments(args);
      if (this.config.isLog_console()) {
         this.console.startFile("console.log");
         this.logger.info("Saving console output enabled");
      } else {
         this.logger.info("Saving console output disabled");
      }

      try {
         this.lang = new Lang(this.config.getLang());
      } catch (IOException var6) {
         this.logger.severe("Failed to load language file: " + this.config.getLang() + "!");
         var6.printStackTrace();
         return;
      }

      this.logger.info(this.lang.get("init_loading", "0.0.5"));
      this.logger.info(this.lang.get("init_mc_pc_support", "1.8.9"));
      this.logger.info(this.lang.get("init_mc_pe_support", "0.15.10"));
      this.authMode = this.config.getMode().toLowerCase();
      if (!this.authMode.equals("cls") && !this.authMode.equals("online") && !this.authMode.equals("offline")) {
         this.logger.severe("Invalid login 'mode' option detected, must be cls/online/offline. You set it to '" + this.authMode + "'! ");
      } else {
         this.sessionRegister = new SessionRegister(this);
         this.commandRegister = new CommandRegister(this);
         this.logger.info(this.lang.get("init_creating_thread_pool", this.config.getThread_pool_size()));
         this.generalThreadPool = Executors.newScheduledThreadPool(this.config.getThread_pool_size());
         this.logger.info(this.lang.get("init_binding", this.config.getUdp_bind_ip(), this.config.getUdp_bind_port()));
         this.network = new RaknetInterface(this, this.config.getUdp_bind_ip(), this.config.getUdp_bind_port());
         this.motd = this.config.getMotd();
         this.motd = this.motd.replace("&", "§");
         this.network.setBroadcastName(this.motd, -1, -1);
         this.ticker.start();
         this.logger.info(this.lang.get("init_done"));
      }
   }

   public boolean isDebug() {
      return this.isDebug;
   }

   public void onTick() {
      this.network.onTick();
      this.sessionRegister.onTick();
   }

   public void checkArguments(String[] args) {
      String[] var2 = args;
      int var3 = args.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         String arg = var2[var4];
         if (arg.toLowerCase().contains("--debug")) {
            this.isDebug = true;
            this.logger.info("\u001b[36mProxy running in debug mode.");
         }
      }

   }

   public void shutdown() {
      this.logger.info(this.lang.get("shutting_down"));
      this.isDebug = false;
      this.shuttingDown = true;
      this.network.shutdown();

      try {
         Thread.sleep(2000L);
      } catch (Exception var2) {
      }

      System.out.println("Goodbye!");
      System.exit(0);
   }

   public Logger getLogger() {
      return this.logger;
   }

   public ServerConfig getConfig() {
      return this.config;
   }

   public Lang getLang() {
      return this.lang;
   }

   public SessionRegister getSessionRegister() {
      return this.sessionRegister;
   }

   public RaknetInterface getNetwork() {
      return this.network;
   }

   public boolean isShuttingDown() {
      return this.shuttingDown;
   }

   public ScheduledExecutorService getGeneralThreadPool() {
      return this.generalThreadPool;
   }

   public CommandRegister getCommandRegister() {
      return this.commandRegister;
   }

   public String getAuthMode() {
      return this.authMode;
   }
}
