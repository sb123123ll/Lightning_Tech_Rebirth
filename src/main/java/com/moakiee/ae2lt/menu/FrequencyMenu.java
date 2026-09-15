package com.moakiee.ae2lt.menu;

import appeng.api.implementations.menuobjects.ItemMenuHost;
import appeng.api.storage.ISubMenuHost;
import appeng.menu.AEBaseMenu;
import appeng.menu.ISubMenu;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocator;
import com.moakiee.ae2lt.api.frequency.FrequencyBindingHost;
import com.moakiee.ae2lt.api.frequency.FrequencyBindingMenuHost;
import com.moakiee.ae2lt.blockentity.WirelessOverloadedControllerBlockEntity;
import com.moakiee.ae2lt.grid.WirelessFrequencyManager;
import com.moakiee.ae2lt.item.OverloadedFrequencyCardData;
import com.moakiee.ae2lt.item.TerminalCardAccess;
import com.moakiee.ae2lt.network.NetworkInit;
import com.moakiee.ae2lt.network.SyncFrequencyDetailPacket;
import com.moakiee.ae2lt.network.SyncFrequencyListPacket;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.network.NetworkHooks;

public class FrequencyMenu extends AEBaseMenu implements ISubMenu {
   public static final MenuType<FrequencyMenu> TYPE = IForgeMenuType.create(FrequencyMenu::clientCreate);
   private static final String ACTION_TOGGLE_AUTO_CONNECT = "toggleAutoConnect";
   private final BlockPos blockPos;
   private final boolean isController;
   private final boolean isAdvanced;
   private final String deviceName;
   @Nullable
   private final BlockEntity backingBlockEntity;
   private final boolean cardMode;
   @Nullable
   private final MenuLocator terminalLocator;
   @Nullable
   private final ServerPlayer cardPlayer;
   @Nullable
   private ISubMenuHost cachedSubMenuHost;
   private final boolean hasParentMenu;
   private final DataSlot freqIdSlot = DataSlot.m_39401_();
   private final DataSlot linkActiveSlot = DataSlot.m_39401_();
   private final DataSlot usedChannelsSlot = DataSlot.m_39401_();
   private final DataSlot maxChannelsSlot = DataSlot.m_39401_();
   private final DataSlot autoConnectSlot = DataSlot.m_39401_();
   private static final int CHANNEL_REFRESH_INTERVAL = 10;
   private int channelRefreshCountdown = 0;

   public FrequencyMenu(int containerId, Inventory playerInv, BlockEntity be) {
      this(containerId, playerInv, be, null, null);
   }

   public FrequencyMenu(int containerId, Inventory playerInv, BlockEntity be, @Nullable MenuType<?> parentType, @Nullable MenuLocator parentLocator) {
      super(TYPE, containerId, playerInv, be);
      this.blockPos = be.m_58899_();
      this.backingBlockEntity = be;
      this.cardMode = false;
      this.terminalLocator = null;
      this.cardPlayer = null;
      if (parentType != null && parentLocator != null) {
         this.cachedSubMenuHost = new FrequencyMenu.DeviceMenuReturnHost(new ItemStack(be.m_58900_().m_60734_()), parentType, parentLocator);
      }

      this.hasParentMenu = this.cachedSubMenuHost != null;
      if (be instanceof WirelessOverloadedControllerBlockEntity ctrl) {
         this.isController = true;
         this.isAdvanced = ctrl.isAdvanced();
         this.deviceName = ctrl.isAdvanced() ? "block.ae2lt.advanced_wireless_overloaded_controller" : "block.ae2lt.wireless_overloaded_controller";
         this.freqIdSlot.m_6422_(ctrl.getFrequencyId());
         this.linkActiveSlot.m_6422_(ctrl.isFrequencyActive() ? 1 : 0);
         this.usedChannelsSlot.m_6422_(ctrl.getGridUsedChannels());
         this.maxChannelsSlot.m_6422_(ctrl.getGridMaxChannels());
      } else if (be instanceof FrequencyBindingHost bindingHost) {
         this.isController = false;
         this.isAdvanced = false;
         this.deviceName = bindingHost.getFrequencyBindingDeviceName();
         this.freqIdSlot.m_6422_(bindingHost.getFrequencyId());
         this.linkActiveSlot.m_6422_(bindingHost.isFrequencyConnected() ? 1 : 0);
         this.usedChannelsSlot.m_6422_(bindingHost.getGridUsedChannels());
         this.maxChannelsSlot.m_6422_(bindingHost.getGridMaxChannels());
      } else {
         this.isController = false;
         this.isAdvanced = false;
         this.deviceName = "block.ae2lt.wireless_receiver";
         this.freqIdSlot.m_6422_(-1);
         this.linkActiveSlot.m_6422_(0);
         this.usedChannelsSlot.m_6422_(0);
         this.maxChannelsSlot.m_6422_(0);
      }

      this.autoConnectSlot.m_6422_(0);
      this.registerDataSlots();
      this.registerClientActions();
      if (playerInv.f_35978_ instanceof ServerPlayer sp) {
         NetworkInit.sendToPlayer(sp, SyncFrequencyListPacket.fromServer());
         SyncFrequencyDetailPacket.sendInitialMembersIfNeeded(sp, this.freqIdSlot.m_6501_());
         SyncFrequencyDetailPacket.sendInitialConnectionsIfNeeded(sp, this.freqIdSlot.m_6501_());
      }
   }

   public FrequencyMenu(int containerId, Inventory playerInv, MenuLocator terminalLocator) {
      super(TYPE, containerId, playerInv, null);
      this.setLocator(terminalLocator);
      this.blockPos = BlockPos.f_121853_;
      this.backingBlockEntity = null;
      this.cardMode = true;
      this.terminalLocator = terminalLocator;
      this.cardPlayer = playerInv.f_35978_ instanceof ServerPlayer sp ? sp : null;
      this.isController = false;
      this.isAdvanced = false;
      this.deviceName = "item.ae2lt.overloaded_frequency_card";
      this.cachedSubMenuHost = this.resolveSubMenuHost();
      this.hasParentMenu = this.cachedSubMenuHost != null;
      OverloadedFrequencyCardData cardData = this.readCardData();
      int freqId = cardData.isBound() ? cardData.frequencyId() : -1;
      this.freqIdSlot.m_6422_(freqId);
      this.linkActiveSlot.m_6422_(this.readCardLinkActive(freqId));
      this.usedChannelsSlot.m_6422_(0);
      this.maxChannelsSlot.m_6422_(0);
      this.autoConnectSlot.m_6422_(cardData.autoConnect() ? 1 : 0);
      this.registerDataSlots();
      this.registerClientActions();
      if (this.cardPlayer != null) {
         NetworkInit.sendToPlayer(this.cardPlayer, SyncFrequencyListPacket.fromServer());
         SyncFrequencyDetailPacket.sendInitialMembersIfNeeded(this.cardPlayer, freqId);
         SyncFrequencyDetailPacket.sendInitialConnectionsIfNeeded(this.cardPlayer, freqId);
      }
   }

   private static FrequencyMenu clientCreate(int containerId, Inventory playerInv, FriendlyByteBuf buf) {
      boolean cardMode = buf.readBoolean();
      BlockPos pos = buf.m_130135_();
      boolean controller = buf.readBoolean();
      boolean advanced = buf.readBoolean();
      String deviceName = buf.m_130136_(256);
      int freqId = buf.readInt();
      boolean linkActive = buf.readBoolean();
      int used = buf.readInt();
      int max = buf.readInt();
      boolean autoConnect = buf.readBoolean();
      boolean hasParentMenu = buf.readBoolean();
      return new FrequencyMenu(
         containerId, playerInv, cardMode, pos, controller, advanced, deviceName, freqId, linkActive, used, max, autoConnect, hasParentMenu
      );
   }

   private FrequencyMenu(
      int containerId,
      Inventory playerInv,
      boolean cardMode,
      BlockPos pos,
      boolean isController,
      boolean isAdvanced,
      String deviceName,
      int freqId,
      boolean linkActive,
      int used,
      int max,
      boolean autoConnect,
      boolean hasParentMenu
   ) {
      super(TYPE, containerId, playerInv, null);
      this.blockPos = pos;
      this.isController = isController;
      this.isAdvanced = isAdvanced;
      this.deviceName = deviceName;
      this.backingBlockEntity = null;
      this.cardMode = cardMode;
      this.terminalLocator = null;
      this.cardPlayer = null;
      this.hasParentMenu = hasParentMenu;
      this.freqIdSlot.m_6422_(freqId);
      this.linkActiveSlot.m_6422_(linkActive ? 1 : 0);
      this.usedChannelsSlot.m_6422_(used);
      this.maxChannelsSlot.m_6422_(max);
      this.autoConnectSlot.m_6422_(autoConnect ? 1 : 0);
      this.registerDataSlots();
      this.registerClientActions();
   }

   private void registerDataSlots() {
      this.m_38895_(this.freqIdSlot);
      this.m_38895_(this.linkActiveSlot);
      this.m_38895_(this.usedChannelsSlot);
      this.m_38895_(this.maxChannelsSlot);
      this.m_38895_(this.autoConnectSlot);
   }

   private void registerClientActions() {
      this.registerClientAction("toggleAutoConnect", this::toggleAutoConnect);
   }

   private static boolean openWithAe2MenuOpener(Player player, MenuLocator locator, boolean returning) {
      if (player instanceof ServerPlayer serverPlayer) {
         if (!(serverPlayer.f_36096_ instanceof FrequencyBindingMenuHost)) {
            ItemMenuHost terminalHost = (ItemMenuHost)locator.locate(serverPlayer, ItemMenuHost.class);
            if (terminalHost == null) {
               return false;
            } else {
               ItemStack terminal = terminalHost.getItemStack();
               if (!TerminalCardAccess.hasCard(terminal)) {
                  return false;
               } else {
                  NetworkHooks.openScreen(
                     serverPlayer, new FrequencyMenu.Ae2StyleFrequencyMenuProvider(Component.m_237115_("item.ae2lt.overloaded_frequency_card"), (id, inv) -> {
                        FrequencyMenu menu = new FrequencyMenu(id, inv, locator);
                        menu.setLocator(locator);
                        menu.setReturnedFromSubScreen(returning);
                        return menu;
                     }, player.f_36096_ instanceof AEBaseMenu), buf -> writeCardExtraData(buf, terminal)
                  );
                  return true;
               }
            }
         } else {
            FrequencyBindingHost bindingHost = (FrequencyBindingHost)locator.locate(serverPlayer, FrequencyBindingHost.class);
            if (bindingHost == null) {
               return false;
            } else {
               BlockEntity be = bindingHost.getFrequencyBindingBlockEntity();
               MenuType<?> parentType = null;
               MenuLocator parentLocator = null;
               if (serverPlayer.f_36096_ instanceof AEBaseMenu parentMenu) {
                  parentType = parentMenu.m_6772_();
                  parentLocator = locator;
               }

               MenuType<?> fParentType = parentType;
               MenuLocator fParentLocator = parentLocator;
               boolean hasParent = fParentType != null && fParentLocator != null;
               NetworkHooks.openScreen(serverPlayer, new FrequencyMenu.Ae2StyleFrequencyMenuProvider(be.m_58900_().m_60734_().m_49954_(), (id, inv) -> {
                  FrequencyMenu menu = new FrequencyMenu(id, inv, be, fParentType, fParentLocator);
                  menu.setLocator(locator);
                  menu.setReturnedFromSubScreen(returning);
                  return menu;
               }, player.f_36096_ instanceof AEBaseMenu), buf -> writeExtraData(buf, be, hasParent));
               return true;
            }
         }
      } else {
         return false;
      }
   }

   public static void writeExtraData(FriendlyByteBuf buf, BlockEntity be, boolean hasParentMenu) {
      buf.writeBoolean(false);
      buf.m_130064_(be.m_58899_());
      if (be instanceof WirelessOverloadedControllerBlockEntity ctrl) {
         buf.writeBoolean(true);
         buf.writeBoolean(ctrl.isAdvanced());
         buf.m_130072_(ctrl.isAdvanced() ? "block.ae2lt.advanced_wireless_overloaded_controller" : "block.ae2lt.wireless_overloaded_controller", 256);
         buf.writeInt(ctrl.getFrequencyId());
         buf.writeBoolean(ctrl.isFrequencyActive());
         buf.writeInt(ctrl.getGridUsedChannels());
         buf.writeInt(ctrl.getGridMaxChannels());
      } else if (be instanceof FrequencyBindingHost bindingHost) {
         buf.writeBoolean(false);
         buf.writeBoolean(false);
         buf.m_130072_(bindingHost.getFrequencyBindingDeviceName(), 256);
         buf.writeInt(bindingHost.getFrequencyId());
         buf.writeBoolean(bindingHost.isFrequencyConnected());
         buf.writeInt(bindingHost.getGridUsedChannels());
         buf.writeInt(bindingHost.getGridMaxChannels());
      } else {
         buf.writeBoolean(false);
         buf.writeBoolean(false);
         buf.m_130072_("block.ae2lt.wireless_receiver", 256);
         buf.writeInt(-1);
         buf.writeBoolean(false);
         buf.writeInt(0);
         buf.writeInt(0);
      }

      buf.writeBoolean(false);
      buf.writeBoolean(hasParentMenu);
   }

   public static void writeCardExtraData(FriendlyByteBuf buf, ItemStack terminalStack) {
      OverloadedFrequencyCardData cardData = TerminalCardAccess.readCardData(terminalStack);
      int freqId = cardData.isBound() ? cardData.frequencyId() : -1;
      buf.writeBoolean(true);
      buf.m_130064_(BlockPos.f_121853_);
      buf.writeBoolean(false);
      buf.writeBoolean(false);
      buf.m_130072_("item.ae2lt.overloaded_frequency_card", 256);
      buf.writeInt(freqId);
      buf.writeBoolean(false);
      buf.writeInt(0);
      buf.writeInt(0);
      buf.writeBoolean(cardData.autoConnect());
      buf.writeBoolean(true);
   }

   public void m_38946_() {
      if (this.cardMode) {
         OverloadedFrequencyCardData cardData = this.readCardData();
         int real = cardData.isBound() ? cardData.frequencyId() : -1;
         if (this.freqIdSlot.m_6501_() != real) {
            this.freqIdSlot.m_6422_(real);
            if (this.cardPlayer != null && this.cardPlayer.f_36096_ == this) {
               SyncFrequencyDetailPacket.sendInitialMembersIfNeeded(this.cardPlayer, real);
               SyncFrequencyDetailPacket.sendInitialConnectionsIfNeeded(this.cardPlayer, real);
            }
         }

         int autoConnect = cardData.autoConnect() ? 1 : 0;
         if (this.autoConnectSlot.m_6501_() != autoConnect) {
            this.autoConnectSlot.m_6422_(autoConnect);
         }

         if (--this.channelRefreshCountdown <= 0) {
            this.channelRefreshCountdown = 10;
            int active = this.readCardLinkActive(real);
            if (this.linkActiveSlot.m_6501_() != active) {
               this.linkActiveSlot.m_6422_(active);
            }
         }

         super.m_38946_();
      } else {
         if (this.backingBlockEntity != null) {
            int realx = this.readFreqIdFromBE();
            if (this.freqIdSlot.m_6501_() != realx) {
               this.freqIdSlot.m_6422_(realx);
               Level lvl = this.backingBlockEntity.m_58904_();
               if (lvl != null && !lvl.m_5776_()) {
                  for (Player p : lvl.m_6907_()) {
                     if (p instanceof ServerPlayer) {
                        ServerPlayer sp = (ServerPlayer)p;
                        if (sp.f_36096_ == this) {
                           SyncFrequencyDetailPacket.sendInitialMembersIfNeeded(sp, realx);
                           SyncFrequencyDetailPacket.sendInitialConnectionsIfNeeded(sp, realx);
                        }
                     }
                  }
               }
            }

            int active = this.readLinkActiveFromBE();
            if (this.linkActiveSlot.m_6501_() != active) {
               this.linkActiveSlot.m_6422_(active);
            }

            if (--this.channelRefreshCountdown <= 0) {
               this.channelRefreshCountdown = 10;
               int used = this.readUsedChannelsFromBE();
               if (this.usedChannelsSlot.m_6501_() != used) {
                  this.usedChannelsSlot.m_6422_(used);
               }

               int max = this.readMaxChannelsFromBE();
               if (this.maxChannelsSlot.m_6501_() != max) {
                  this.maxChannelsSlot.m_6422_(max);
               }
            }
         }

         super.m_38946_();
      }
   }

   private int readFreqIdFromBE() {
      if (this.backingBlockEntity instanceof WirelessOverloadedControllerBlockEntity ctrl) {
         return ctrl.getFrequencyId();
      } else {
         return this.backingBlockEntity instanceof FrequencyBindingHost bindingHost ? bindingHost.getFrequencyId() : -1;
      }
   }

   private int readLinkActiveFromBE() {
      if (this.backingBlockEntity instanceof WirelessOverloadedControllerBlockEntity ctrl) {
         return ctrl.isFrequencyActive() ? 1 : 0;
      } else if (this.backingBlockEntity instanceof FrequencyBindingHost bindingHost) {
         return bindingHost.isFrequencyConnected() ? 1 : 0;
      } else {
         return 0;
      }
   }

   private int readUsedChannelsFromBE() {
      if (this.backingBlockEntity instanceof WirelessOverloadedControllerBlockEntity ctrl) {
         return ctrl.getGridUsedChannels();
      } else {
         return this.backingBlockEntity instanceof FrequencyBindingHost bindingHost ? bindingHost.getGridUsedChannels() : 0;
      }
   }

   private int readMaxChannelsFromBE() {
      if (this.backingBlockEntity instanceof WirelessOverloadedControllerBlockEntity ctrl) {
         return ctrl.getGridMaxChannels();
      } else {
         return this.backingBlockEntity instanceof FrequencyBindingHost bindingHost ? bindingHost.getGridMaxChannels() : 0;
      }
   }

   public boolean m_6875_(@Nonnull Player player) {
      if (this.cardMode) {
         return TerminalCardAccess.hasCard(this.resolveTerminalStack());
      } else {
         if (this.backingBlockEntity != null) {
            if (this.backingBlockEntity.m_58901_() || this.backingBlockEntity.m_58904_() == null) {
               return false;
            }

            if (player.m_9236_() != this.backingBlockEntity.m_58904_()) {
               return false;
            }

            if (this.backingBlockEntity.m_58904_().m_7702_(this.blockPos) != this.backingBlockEntity) {
               return false;
            }
         }

         return player.m_20275_((double)this.blockPos.m_123341_() + 0.5, (double)this.blockPos.m_123342_() + 0.5, (double)this.blockPos.m_123343_() + 0.5)
            <= 64.0;
      }
   }

   @Nonnull
   public ItemStack m_7648_(@Nonnull Player player, int index) {
      return ItemStack.f_41583_;
   }

   public BlockPos getBlockPos() {
      return this.blockPos;
   }

   public boolean isController() {
      return this.isController;
   }

   public boolean isAdvanced() {
      return this.isAdvanced;
   }

   public String getDeviceName() {
      return this.deviceName;
   }

   public int getCurrentFrequencyId() {
      return this.freqIdSlot.m_6501_();
   }

   public boolean isCardMode() {
      return this.cardMode;
   }

   public boolean hasParentMenu() {
      return this.hasParentMenu;
   }

   public boolean isAutoConnect() {
      return this.autoConnectSlot.m_6501_() != 0;
   }

   public void clientToggleAutoConnect() {
      this.sendClientAction("toggleAutoConnect");
   }

   public boolean isLinkActive() {
      return this.linkActiveSlot.m_6501_() != 0;
   }

   public ItemStack resolveTerminalStack() {
      if (this.terminalLocator != null && this.cardPlayer != null) {
         ItemMenuHost host = (ItemMenuHost)this.terminalLocator.locate(this.cardPlayer, ItemMenuHost.class);
         return host != null ? host.getItemStack() : ItemStack.f_41583_;
      } else {
         return ItemStack.f_41583_;
      }
   }

   @Nullable
   public ISubMenuHost getHost() {
      return this.cachedSubMenuHost;
   }

   @Nullable
   private ISubMenuHost resolveSubMenuHost() {
      return this.terminalLocator != null && this.cardPlayer != null ? (ISubMenuHost)this.terminalLocator.locate(this.cardPlayer, ISubMenuHost.class) : null;
   }

   private OverloadedFrequencyCardData readCardData() {
      return TerminalCardAccess.readCardData(this.resolveTerminalStack());
   }

   private void toggleAutoConnect() {
      if (this.cardMode && this.isServerSide() && this.cardPlayer != null && this.m_6875_(this.cardPlayer)) {
         OverloadedFrequencyCardData cardData = this.readCardData();
         if (!cardData.isBound() || cardData.canBeUsedBy(this.cardPlayer.m_20148_())) {
            if (TerminalCardAccess.updateCard(this.resolveTerminalStack(), OverloadedFrequencyCardData::toggleAutoConnect)) {
               this.autoConnectSlot.m_6422_(this.readCardData().autoConnect() ? 1 : 0);
               this.m_38946_();
            }
         }
      }
   }

   private int readCardLinkActive(int freqId) {
      if (freqId > 0 && this.cardPlayer != null) {
         WirelessFrequencyManager manager = WirelessFrequencyManager.get();
         if (manager == null) {
            return 0;
         } else {
            return manager.resolveAdvancedNode(freqId, this.cardPlayer.m_284548_().m_7654_()) != null ? 1 : 0;
         }
      } else {
         return 0;
      }
   }

   public int getUsedChannels() {
      return this.usedChannelsSlot.m_6501_();
   }

   public int getMaxChannels() {
      return this.maxChannelsSlot.m_6501_();
   }

   @Nullable
   public static FrequencyMenu validateToken(ServerPlayer player, int token) {
      if (player.f_36096_ instanceof FrequencyMenu fm && fm.f_38840_ == token) {
         return fm.m_6875_(player) ? fm : null;
      }

      return null;
   }

   static {
      MenuOpener.addOpener(TYPE, FrequencyMenu::openWithAe2MenuOpener);
   }

   private static record Ae2StyleFrequencyMenuProvider(Component title, FrequencyMenu.FrequencyMenuFactory factory, boolean openedFromAe2Menu)
      implements MenuProvider {
      public Component m_5446_() {
         return this.title;
      }

      public AbstractContainerMenu m_7208_(int containerId, Inventory playerInventory, Player player) {
         return this.factory.create(containerId, playerInventory);
      }
   }

   private static record DeviceMenuReturnHost(ItemStack icon, MenuType<?> parentType, MenuLocator parentLocator) implements ISubMenuHost {
      public ItemStack getMainMenuIcon() {
         return this.icon;
      }

      public void returnToMainMenu(Player player, ISubMenu subMenu) {
         MenuOpener.open(this.parentType, player, this.parentLocator);
      }
   }

   @FunctionalInterface
   private interface FrequencyMenuFactory {
      FrequencyMenu create(int var1, Inventory var2);
   }
}
