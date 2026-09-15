package com.moakiee.ae2lt.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.mojang.logging.LogUtils;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

public final class AE2LTConfigMigration {
   private static final Logger LOG = LogUtils.getLogger();
   private static final String CONFIG_FILE_NAME = "ae2lt-common.toml";
   private static final String VERSION_KEY = "configVersion";
   private static boolean migrationOccurred;

   private AE2LTConfigMigration() {
   }

   public static void runIfNeeded() {
      Path configFile = FMLPaths.CONFIGDIR.get().resolve("ae2lt-common.toml");
      if (Files.exists(configFile)) {
         int version = readVersion(configFile);
         if (version < 3) {
            try {
               Files.delete(configFile);
               migrationOccurred = true;
               LOG.info("[ae2lt] detected legacy {} (version {} < {}); deleted, defaults will be regenerated.", new Object[]{"ae2lt-common.toml", version, 3});
            } catch (Exception var3) {
               LOG.warn("[ae2lt] failed to delete legacy config {}: {}", "ae2lt-common.toml", var3.toString());
            }
         }
      }
   }

   public static boolean migrationOccurred() {
      return migrationOccurred;
   }

   private static int readVersion(Path file) {
      try {
         CommentedFileConfig raw = (CommentedFileConfig)CommentedFileConfig.builder(file).sync().preserveInsertionOrder().build();

         int var4;
         label49: {
            try {
               raw.load();
               if (raw.get("configVersion") instanceof Number n) {
                  var4 = n.intValue();
                  break label49;
               }
            } catch (Throwable var6) {
               if (raw != null) {
                  try {
                     raw.close();
                  } catch (Throwable var5) {
                     var6.addSuppressed(var5);
                  }
               }

               throw var6;
            }

            if (raw != null) {
               raw.close();
            }

            return 1;
         }

         if (raw != null) {
            raw.close();
         }

         return var4;
      } catch (Exception var7) {
         LOG.warn("[ae2lt] failed to read {} during migration probe: {}", "ae2lt-common.toml", var7.toString());
         return 1;
      }
   }
}
