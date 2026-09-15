package com.moakiee.ae2lt.integration.ae2wtlib;

import com.moakiee.ae2lt.menu.TianshuWirelessPatternEncodingTermMenu;
import com.moakiee.ae2lt.registry.ModItems;
import de.mari_023.ae2wtlib.UpgradeHelper;
import de.mari_023.ae2wtlib.wut.WTDefinition;
import de.mari_023.ae2wtlib.wut.WUTHandler;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;

public final class Ae2wtlibIntegration {
   public static final String TIANSHU_TERMINAL_NAME = "tianshu_pattern_encoding";
   public static final String TIANSHU_TERMINAL_DESCRIPTION_ID = "item.ae2lt.wireless_tianshu_pattern_encoding_terminal";
   private static TianshuWTItem tianshuTerminal;
   private static boolean terminalRegistrationRequested;

   private Ae2wtlibIntegration() {
   }

   public static synchronized TianshuWTItem terminal() {
      if (tianshuTerminal == null) {
         tianshuTerminal = new TianshuWTItem();
      }

      return tianshuTerminal;
   }

   public static void onRegister(RegisterEvent event) {
      if (event.getRegistryKey().equals(ForgeRegistries.ITEMS.getRegistryKey())) {
         registerTerminal();
      }
   }

   public static synchronized void registerTerminal() {
      if (!terminalRegistrationRequested) {
         WUTHandler.addTerminal(
            "tianshu_pattern_encoding",
            terminal()::tryOpen,
            TianshuWTMenuHost::new,
            TianshuWirelessPatternEncodingTermMenu.TYPE,
            terminal(),
            "item.ae2lt.wireless_tianshu_pattern_encoding_terminal"
         );
         terminalRegistrationRequested = true;
      }
   }

   public static void verifyTerminalRegistration() {
      WTDefinition definition = (WTDefinition)WUTHandler.wirelessTerminals.get("tianshu_pattern_encoding");
      int tianshuIndex = WUTHandler.terminalNames.indexOf("tianshu_pattern_encoding");
      if (definition == null || tianshuIndex < 0 || definition.item() != ModItems.TIANSHU_WIRELESS_PATTERN_ENCODING_TERMINAL.get()) {
         throw new IllegalStateException("AE2WTLib did not register the wireless Tianshu terminal");
      }
   }

   public static void register() {
      UpgradeHelper.addUpgradeToAllTerminals((ItemLike)ModItems.OVERLOADED_FREQUENCY_CARD.get(), 1);
   }
}
