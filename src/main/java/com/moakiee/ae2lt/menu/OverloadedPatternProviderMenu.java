package com.moakiee.ae2lt.menu;

import appeng.api.config.Settings;
import appeng.api.config.YesNo;
import appeng.helpers.patternprovider.PatternProviderLogic;
import appeng.helpers.patternprovider.PatternProviderLogicHost;
import appeng.menu.SlotSemantics;
import appeng.menu.guisync.GuiSync;
import appeng.menu.implementations.MenuTypeBuilder;
import appeng.menu.implementations.PatternProviderMenu;
import appeng.menu.slot.AppEngSlot;
import com.moakiee.ae2lt.api.pattern.PatternProviderUiProfile;
import com.moakiee.ae2lt.blockentity.OverloadedPatternProviderBlockEntity;
import com.moakiee.ae2lt.logic.OverloadedPatternProviderLogic;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;

public class OverloadedPatternProviderMenu extends PatternProviderMenu implements FrequencyBindingMenu {
   public static final MenuType<OverloadedPatternProviderMenu> TYPE = Ae2ltMenuBuilder.buildUnregistered(
      MenuTypeBuilder.create((id, playerInventory, host) -> new OverloadedPatternProviderMenu(id, playerInventory, host), PatternProviderLogicHost.class),
      new ResourceLocation("ae2lt", "overloaded_pattern_provider")
   );
   private static final int SLOTS_PER_PAGE = 36;
   private static final int PROFILE_PACKAGED = 1;
   private static final int PROFILE_MODE_SWITCH = 2;
   private static final int PROFILE_FILTERED_IMPORT = 4;
   private static final int PROFILE_WIRELESS_TUNING = 8;
   private static final int PROFILE_BLOCKING_MODE = 16;
   private static final int DEFAULT_PROFILE_FLAGS = 30;
   @GuiSync(22000)
   public int providerMode;
   @GuiSync(22001)
   public int returnMode;
   @GuiSync(22002)
   public int filteredImport;
   @GuiSync(22003)
   public int wirelessDispatchMode;
   @GuiSync(22006)
   public int wirelessSpeedMode;
   @GuiSync(22007)
   public int uiProfileFlags = 30;
   @GuiSync(22008)
   public String titleTranslationKey = "ae2lt.gui.title.overloaded_pattern_provider";
   @GuiSync(22009)
   public int blockingMode;
   @GuiSync(22010)
   public int adaptiveBatchEnabled;
   @GuiSync(22004)
   public int currentPage;
   @GuiSync(22005)
   public int totalPages;
   private final PatternProviderLogicHost host;
   private int lastShownPage = -1;

   public OverloadedPatternProviderMenu(int id, Inventory playerInventory, PatternProviderLogicHost host) {
      this(TYPE, id, playerInventory, host);
   }

   protected OverloadedPatternProviderMenu(
      MenuType<? extends OverloadedPatternProviderMenu> menuType, int id, Inventory playerInventory, PatternProviderLogicHost host
   ) {
      super(menuType, id, playerInventory, host);
      this.host = host;
      this.registerClientAction("toggleMode", this::toggleMode);
      this.registerClientAction("toggleAutoReturn", this::toggleAutoReturn);
      this.registerClientAction("toggleWirelessDispatchMode", this::toggleWirelessDispatchMode);
      this.registerClientAction("toggleWirelessSpeedMode", this::toggleWirelessSpeedMode);
      this.registerClientAction("toggleFilteredImport", this::toggleFilteredImport);
      this.registerClientAction("toggleAdaptiveBatch", this::toggleAdaptiveBatch);
      this.registerClientAction("cycleBlockingMode", this::cycleBlockingMode);
      this.registerClientAction("nextPage", this::nextPage);
      this.registerClientAction("prevPage", this::prevPage);
      this.showPage(0);
      this.lastShownPage = -1;
   }

   public void showPage(int page) {
      if (page != this.lastShownPage) {
         this.lastShownPage = page;
         List<Slot> patternSlots = this.getSlots(SlotSemantics.ENCODED_PATTERN);
         int totalSlots = patternSlots.size();
         int start = page * 36;
         int end = Math.min(start + 36, totalSlots);

         for (int i = 0; i < totalSlots; i++) {
            Slot slot = patternSlots.get(i);
            if (slot instanceof AppEngSlot aeSlot) {
               aeSlot.setActive(i >= start && i < end);
            }
         }
      }
   }

   public void m_38946_() {
      if (this.isServerSide() && this.host instanceof OverloadedPatternProviderBlockEntity be) {
         this.providerMode = be.getProviderMode().ordinal();
         this.returnMode = be.getReturnMode().ordinal();
         this.filteredImport = be.isFilteredImport() ? 1 : 0;
         this.wirelessDispatchMode = be.getWirelessDispatchMode().ordinal();
         this.wirelessSpeedMode = be.getWirelessSpeedMode().ordinal();
         this.blockingMode = getBlockingState(be);
         this.adaptiveBatchEnabled = be.isAdaptiveBatchEnabled() ? 1 : 0;
         this.syncUiProfile(be);
         OverloadedPatternProviderLogic logic = (OverloadedPatternProviderLogic)be.getLogic();
         this.currentPage = logic.getCurrentPage();
         this.totalPages = logic.getTotalPages();
         logic.syncReturnPageViewFromFull();
      }

      this.showPage(this.currentPage);
      super.m_38946_();
   }

   private void toggleMode() {
      if (this.isServerSide() && this.host instanceof OverloadedPatternProviderBlockEntity be) {
         if (!isModeSwitchVisible(be)) {
            return;
         }

         OverloadedPatternProviderBlockEntity.ProviderMode current = be.getProviderMode();
         be.setProviderMode(
            current == OverloadedPatternProviderBlockEntity.ProviderMode.NORMAL
               ? OverloadedPatternProviderBlockEntity.ProviderMode.WIRELESS
               : OverloadedPatternProviderBlockEntity.ProviderMode.NORMAL
         );
      }
   }

   private void toggleAutoReturn() {
      if (this.isServerSide() && this.host instanceof OverloadedPatternProviderBlockEntity be) {
         OverloadedPatternProviderBlockEntity.ReturnMode[] values = OverloadedPatternProviderBlockEntity.ReturnMode.values();
         OverloadedPatternProviderBlockEntity.ReturnMode current = be.getReturnMode();
         be.setReturnMode(values[(current.ordinal() + 1) % values.length]);
      }
   }

   private void toggleWirelessDispatchMode() {
      if (this.isServerSide() && this.host instanceof OverloadedPatternProviderBlockEntity be) {
         if (!isWirelessTuningVisible(be)) {
            return;
         }

         OverloadedPatternProviderBlockEntity.WirelessDispatchMode[] values = OverloadedPatternProviderBlockEntity.WirelessDispatchMode.values();
         OverloadedPatternProviderBlockEntity.WirelessDispatchMode current = be.getWirelessDispatchMode();
         be.setWirelessDispatchMode(values[(current.ordinal() + 1) % values.length]);
      }
   }

   private void toggleWirelessSpeedMode() {
      if (this.isServerSide() && this.host instanceof OverloadedPatternProviderBlockEntity be) {
         if (!isWirelessTuningVisible(be)) {
            return;
         }

         OverloadedPatternProviderBlockEntity.WirelessSpeedMode[] values = OverloadedPatternProviderBlockEntity.WirelessSpeedMode.values();
         OverloadedPatternProviderBlockEntity.WirelessSpeedMode current = be.getWirelessSpeedMode();
         be.setWirelessSpeedMode(values[(current.ordinal() + 1) % values.length]);
      }
   }

   private void toggleFilteredImport() {
      if (this.isServerSide() && this.host instanceof OverloadedPatternProviderBlockEntity be) {
         if (!isFilteredImportVisible(be)) {
            return;
         }

         be.setFilteredImport(!be.isFilteredImport());
      }
   }

   private void toggleAdaptiveBatch() {
      if (this.isServerSide() && this.host instanceof OverloadedPatternProviderBlockEntity be) {
         be.setAdaptiveBatchEnabled(!be.isAdaptiveBatchEnabled());
      }
   }

   private void cycleBlockingMode() {
      if (this.isServerSide() && this.host instanceof OverloadedPatternProviderBlockEntity be) {
         if (!isBlockingModeVisible(be)) {
            return;
         }

         int next = (getBlockingState(be) + 1) % 3;
         PatternProviderLogic logic = be.getLogic();
         logic.getConfigManager().putSetting(Settings.BLOCKING_MODE, next == 0 ? YesNo.NO : YesNo.YES);
         be.setBlockingMode(
            next == 2 ? OverloadedPatternProviderBlockEntity.BlockingMode.SAME_PATTERN : OverloadedPatternProviderBlockEntity.BlockingMode.NORMAL
         );
      }
   }

   private static int getBlockingState(OverloadedPatternProviderBlockEntity be) {
      if (!be.getLogic().isBlocking()) {
         return 0;
      } else {
         return be.getBlockingMode() == OverloadedPatternProviderBlockEntity.BlockingMode.SAME_PATTERN ? 2 : 1;
      }
   }

   private void syncUiProfile(OverloadedPatternProviderBlockEntity be) {
      PatternProviderUiProfile profile = be instanceof PatternProviderUiProfile uiProfile ? uiProfile : null;
      int flags = 0;
      if (profile != null && profile.ae2lt$isPackagedProviderUi()) {
         flags |= 1;
      }

      if (profile == null || profile.ae2lt$isModeSwitchVisible()) {
         flags |= 2;
      }

      if (profile == null || profile.ae2lt$isFilteredImportVisible()) {
         flags |= 4;
      }

      if (profile == null || profile.ae2lt$isWirelessTuningVisible()) {
         flags |= 8;
      }

      if (profile == null || profile.ae2lt$isBlockingModeVisible()) {
         flags |= 16;
      }

      this.uiProfileFlags = flags;
      this.titleTranslationKey = profile == null ? "ae2lt.gui.title.overloaded_pattern_provider" : profile.ae2lt$titleTranslationKey();
      if (this.titleTranslationKey == null || this.titleTranslationKey.isBlank()) {
         this.titleTranslationKey = "ae2lt.gui.title.overloaded_pattern_provider";
      }
   }

   private static boolean isModeSwitchVisible(OverloadedPatternProviderBlockEntity be) {
      if (be instanceof PatternProviderUiProfile profile && !profile.ae2lt$isModeSwitchVisible()) {
         return false;
      }

      return true;
   }

   private static boolean isFilteredImportVisible(OverloadedPatternProviderBlockEntity be) {
      if (be instanceof PatternProviderUiProfile profile && !profile.ae2lt$isFilteredImportVisible()) {
         return false;
      }

      return true;
   }

   private static boolean isWirelessTuningVisible(OverloadedPatternProviderBlockEntity be) {
      if (be instanceof PatternProviderUiProfile profile && !profile.ae2lt$isWirelessTuningVisible()) {
         return false;
      }

      return true;
   }

   private static boolean isBlockingModeVisible(OverloadedPatternProviderBlockEntity be) {
      if (be instanceof PatternProviderUiProfile profile && !profile.ae2lt$isBlockingModeVisible()) {
         return false;
      }

      return true;
   }

   private void nextPage() {
      if (this.isServerSide() && this.host instanceof OverloadedPatternProviderBlockEntity be) {
         OverloadedPatternProviderLogic logic = (OverloadedPatternProviderLogic)be.getLogic();
         logic.setCurrentPage(logic.getCurrentPage() + 1);
      }
   }

   private void prevPage() {
      if (this.isServerSide() && this.host instanceof OverloadedPatternProviderBlockEntity be) {
         OverloadedPatternProviderLogic logic = (OverloadedPatternProviderLogic)be.getLogic();
         logic.setCurrentPage(logic.getCurrentPage() - 1);
      }
   }

   public void clientToggleMode() {
      this.sendClientAction("toggleMode");
   }

   public void clientToggleAutoReturn() {
      this.sendClientAction("toggleAutoReturn");
   }

   public void clientToggleWirelessDispatchMode() {
      this.sendClientAction("toggleWirelessDispatchMode");
   }

   public void clientToggleWirelessSpeedMode() {
      this.sendClientAction("toggleWirelessSpeedMode");
   }

   public void clientToggleFilteredImport() {
      this.sendClientAction("toggleFilteredImport");
   }

   public void clientToggleAdaptiveBatch() {
      this.sendClientAction("toggleAdaptiveBatch");
   }

   public void clientCycleBlockingMode() {
      this.sendClientAction("cycleBlockingMode");
   }

   public boolean isWirelessMode() {
      return this.providerMode == OverloadedPatternProviderBlockEntity.ProviderMode.WIRELESS.ordinal();
   }

   public boolean isAutoReturnEnabled() {
      return this.returnMode != OverloadedPatternProviderBlockEntity.ReturnMode.OFF.ordinal();
   }

   public int getReturnModeOrdinal() {
      return this.returnMode;
   }

   public boolean isFilteredImport() {
      return this.isFilteredImportVisible() && this.filteredImport != 0;
   }

   public boolean isEvenDistributionMode() {
      return this.wirelessDispatchMode == OverloadedPatternProviderBlockEntity.WirelessDispatchMode.EVEN_DISTRIBUTION.ordinal();
   }

   public boolean isFastSpeedMode() {
      return this.isWirelessTuningVisible() && this.wirelessSpeedMode == OverloadedPatternProviderBlockEntity.WirelessSpeedMode.FAST.ordinal();
   }

   public boolean isAdaptiveBatchEnabled() {
      return this.adaptiveBatchEnabled != 0;
   }

   public boolean isPackagedProviderUi() {
      return (this.uiProfileFlags & 1) != 0;
   }

   public boolean isModeSwitchVisible() {
      return (this.uiProfileFlags & 2) != 0;
   }

   public boolean isFilteredImportVisible() {
      return (this.uiProfileFlags & 4) != 0;
   }

   public boolean isWirelessTuningVisible() {
      return (this.uiProfileFlags & 8) != 0;
   }

   public boolean isBlockingModeVisible() {
      return (this.uiProfileFlags & 16) != 0;
   }

   public int getBlockingModeOrdinal() {
      return this.blockingMode;
   }

   public String getTitleTranslationKey() {
      return this.titleTranslationKey;
   }

   public void clientNextPage() {
      this.sendClientAction("nextPage");
   }

   public void clientPrevPage() {
      this.sendClientAction("prevPage");
   }

   public int getCurrentPage() {
      return this.currentPage;
   }

   public int getTotalPages() {
      return this.totalPages;
   }
}
