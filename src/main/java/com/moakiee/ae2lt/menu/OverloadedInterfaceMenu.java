package com.moakiee.ae2lt.menu;

import appeng.api.config.Actionable;
import appeng.api.inventories.InternalInventory;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.upgrades.Upgrades;
import appeng.helpers.InterfaceLogic;
import appeng.helpers.InterfaceLogicHost;
import appeng.menu.AEBaseMenu;
import appeng.menu.SlotSemantic;
import appeng.menu.SlotSemantics;
import appeng.menu.guisync.GuiSync;
import appeng.menu.implementations.InterfaceMenu;
import appeng.menu.implementations.MenuTypeBuilder;
import appeng.menu.implementations.SetStockAmountMenu;
import appeng.menu.implementations.MenuTypeBuilder.MenuFactory;
import appeng.menu.slot.AppEngSlot;
import com.google.common.collect.ArrayListMultimap;
import com.moakiee.ae2lt.blockentity.OverloadedInterfaceBlockEntity;
import com.moakiee.ae2lt.item.OverloadedFilterComponentItem;
import com.moakiee.ae2lt.logic.OverloadedInterfaceLogic;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.slf4j.LoggerFactory;

public class OverloadedInterfaceMenu extends InterfaceMenu implements FrequencyBindingMenu {
   private static final MenuFactory<OverloadedInterfaceMenu, InterfaceLogicHost> FACTORY = OverloadedInterfaceMenu::new;
   public static final MenuType<OverloadedInterfaceMenu> TYPE = Ae2ltMenuBuilder.buildUnregistered(
      MenuTypeBuilder.create(FACTORY, InterfaceLogicHost.class), new ResourceLocation("ae2lt", "overloaded_interface")
   );
   private static final int SLOTS_PER_PAGE = 18;
   private static final Field F_SEMANTIC_BY_SLOT;
   private static final Field F_SLOTS_BY_SEMANTIC;
   @GuiSync(20)
   public int currentPage;
   @GuiSync(21)
   public int totalPages;
   @GuiSync(22)
   public int interfaceMode;
   @GuiSync(23)
   public int exportMode;
   @GuiSync(24)
   public int importMode;
   @GuiSync(25)
   public int energyDirOrdinal;
   @GuiSync(26)
   public int ioSpeedMode;
   @GuiSync(27)
   public long unlimitedBits;
   private final InterfaceLogicHost host;
   private final Set<Slot> storageSlotSet;
   private final Set<Slot> containerSlotSet;
   private final List<Slot> allConfigSlots;
   private Slot filterSlot;
   private int lastShownPage = -1;

   public OverloadedInterfaceMenu(int id, Inventory playerInventory, InterfaceLogicHost host) {
      super(TYPE, id, playerInventory, host);
      this.host = host;
      this.remapSlotSemantics();
      if (host instanceof OverloadedInterfaceBlockEntity be) {
         OverloadedInterfaceMenu.OverloadedFilterSlot filterSlot = new OverloadedInterfaceMenu.OverloadedFilterSlot(be.getFilterInv(), 0);
         filterSlot.setNotDraggable();
         this.filterSlot = this.addSlot(filterSlot, Ae2ltSlotSemantics.OVERLOADED_INTERFACE_FILTER);
         Ae2ltSlotBackgrounds.withBackground(this.filterSlot, Ae2ltSlotBackgrounds.FILTER_COMPONENT);
      }

      InterfaceLogic logic = host.getInterfaceLogic();
      int configSize = logic.getConfig().size();
      this.totalPages = Math.max(1, (configSize + 18 - 1) / 18);
      this.allConfigSlots = new ArrayList<>();

      for (SlotSemantic sem : OverloadedSlotSemantics.CONFIG_PATTERN) {
         this.allConfigSlots.addAll(this.getSlots(sem));
      }

      this.storageSlotSet = new HashSet<>();

      for (SlotSemantic sem : OverloadedSlotSemantics.STORAGE_PATTERN) {
         this.storageSlotSet.addAll(this.getSlots(sem));
      }

      HashSet<Slot> cSlots = new HashSet<>(this.storageSlotSet);
      cSlots.addAll(this.allConfigSlots);
      cSlots.addAll(this.getSlots(SlotSemantics.UPGRADE));
      if (this.filterSlot != null) {
         cSlots.add(this.filterSlot);
      }

      this.containerSlotSet = cSlots;
      this.registerClientAction("nextPage", this::nextPage);
      this.registerClientAction("prevPage", this::prevPage);
      this.registerClientAction("cycleInterfaceMode", this::cycleInterfaceMode);
      this.registerClientAction("cycleExportMode", this::cycleExportMode);
      this.registerClientAction("cycleImportMode", this::cycleImportMode);
      this.registerClientAction("cycleEnergyDir", this::cycleEnergyDir);
      this.registerClientAction("cycleIOSpeed", this::cycleIOSpeed);
      this.registerClientAction("toggleUnlimited", Integer.class, this::toggleUnlimited);
      this.syncFromBE();
      int startPage = host instanceof OverloadedInterfaceBlockEntity be ? Math.min(be.getLastViewedPage(), this.totalPages - 1) : 0;
      this.showPage(Math.max(0, startPage));
   }

   private void remapSlotSemantics() {
      try {
         Map<Slot, SlotSemantic> semanticBySlot = (Map<Slot, SlotSemantic>)F_SEMANTIC_BY_SLOT.get(this);
         ArrayListMultimap<SlotSemantic, Slot> slotsBySemantic = (ArrayListMultimap<SlotSemantic, Slot>)F_SLOTS_BY_SEMANTIC.get(this);
         ArrayList<Slot> configSlots = new ArrayList<>(slotsBySemantic.get(SlotSemantics.CONFIG));
         ArrayList<Slot> storageSlots = new ArrayList<>(slotsBySemantic.get(SlotSemantics.STORAGE));
         slotsBySemantic.removeAll(SlotSemantics.CONFIG);
         slotsBySemantic.removeAll(SlotSemantics.STORAGE);

         for (int i = 0; i < configSlots.size(); i++) {
            int page = i / 18;
            int row = i % 18 / 9;
            SlotSemantic semantic = OverloadedSlotSemantics.CONFIG_PATTERN[2 * page + row];
            Slot slot = configSlots.get(i);
            semanticBySlot.put(slot, semantic);
            slotsBySemantic.put(semantic, slot);
         }

         for (int i = 0; i < storageSlots.size(); i++) {
            int page = i / 18;
            int row = i % 18 / 9;
            SlotSemantic semantic = OverloadedSlotSemantics.STORAGE_PATTERN[2 * page + row];
            Slot slot = storageSlots.get(i);
            semanticBySlot.put(slot, semantic);
            slotsBySemantic.put(semantic, slot);
         }
      } catch (ReflectiveOperationException var10) {
         throw new IllegalStateException("Failed to remap slot semantics", var10);
      }
   }

   public List<Slot> getAllConfigSlots() {
      return this.allConfigSlots;
   }

   public Slot getFilterSlot() {
      return this.filterSlot;
   }

   private void syncFromBE() {
      if (this.host instanceof OverloadedInterfaceBlockEntity be) {
         this.interfaceMode = be.getInterfaceMode().ordinal();
         this.ioSpeedMode = be.getIOSpeedMode().ordinal();
         this.exportMode = be.getExportMode().ordinal();
         this.importMode = be.getImportMode().ordinal();
         Direction dir = be.getEnergyOutputDir();
         this.energyDirOrdinal = dir != null ? dir.m_122411_() : -1;
         long bits = 0L;

         for (int i = 0; i < 36; i++) {
            if (be.isSlotUnlimited(i)) {
               bits |= 1L << i;
            }
         }

         this.unlimitedBits = bits;
      }
   }

   public void showPage(int page) {
      if (page >= 0 && page < this.totalPages) {
         this.lastShownPage = page;
         this.currentPage = page;
         if (this.host instanceof OverloadedInterfaceBlockEntity be) {
            be.setLastViewedPage(page);
         }

         for (int group = 0; group < 4; group++) {
            boolean active = page == group / 2;
            List<Slot> cfgSlots = this.getSlots(OverloadedSlotSemantics.CONFIG_PATTERN[group]);
            List<Slot> stoSlots = this.getSlots(OverloadedSlotSemantics.STORAGE_PATTERN[group]);

            for (Slot s : cfgSlots) {
               if (s instanceof AppEngSlot as) {
                  as.setActive(active);
               }
            }

            for (Slot sx : stoSlots) {
               if (sx instanceof AppEngSlot as) {
                  as.setActive(active);
               }
            }
         }
      }
   }

   public void nextPage() {
      if (this.isClientSide()) {
         this.sendClientAction("nextPage");
      }

      this.showPage(this.currentPage + 1);
   }

   public void prevPage() {
      if (this.isClientSide()) {
         this.sendClientAction("prevPage");
      }

      this.showPage(this.currentPage - 1);
   }

   public void cycleInterfaceMode() {
      if (this.isClientSide()) {
         this.sendClientAction("cycleInterfaceMode");
      } else {
         if (this.host instanceof OverloadedInterfaceBlockEntity be) {
            OverloadedInterfaceBlockEntity.InterfaceMode[] modes = OverloadedInterfaceBlockEntity.InterfaceMode.values();
            be.setInterfaceMode(modes[(this.interfaceMode + 1) % modes.length]);
            this.interfaceMode = be.getInterfaceMode().ordinal();
         }
      }
   }

   public void cycleExportMode() {
      if (this.isClientSide()) {
         this.sendClientAction("cycleExportMode");
      } else {
         if (this.host instanceof OverloadedInterfaceBlockEntity be) {
            OverloadedInterfaceBlockEntity.ExportMode[] modes = OverloadedInterfaceBlockEntity.ExportMode.values();
            be.setExportMode(modes[(this.exportMode + 1) % modes.length]);
            this.exportMode = be.getExportMode().ordinal();
         }
      }
   }

   public void cycleImportMode() {
      if (this.isClientSide()) {
         this.sendClientAction("cycleImportMode");
      } else {
         if (this.host instanceof OverloadedInterfaceBlockEntity be) {
            OverloadedInterfaceBlockEntity.ImportMode[] modes = OverloadedInterfaceBlockEntity.ImportMode.values();
            be.setImportMode(modes[(this.importMode + 1) % modes.length]);
            this.importMode = be.getImportMode().ordinal();
         }
      }
   }

   public void cycleEnergyDir() {
      if (this.isClientSide()) {
         this.sendClientAction("cycleEnergyDir");
      } else {
         if (this.host instanceof OverloadedInterfaceBlockEntity be) {
            int next = this.energyDirOrdinal + 1;
            if (next >= 6) {
               next = -1;
            }

            be.setEnergyOutputDir(next >= 0 ? Direction.m_122376_(next) : null);
            Direction dir = be.getEnergyOutputDir();
            this.energyDirOrdinal = dir != null ? dir.m_122411_() : -1;
         }
      }
   }

   public void cycleIOSpeed() {
      if (this.isClientSide()) {
         this.sendClientAction("cycleIOSpeed");
      } else {
         if (this.host instanceof OverloadedInterfaceBlockEntity be) {
            OverloadedInterfaceBlockEntity.IOSpeedMode[] modes = OverloadedInterfaceBlockEntity.IOSpeedMode.values();
            be.setIOSpeedMode(modes[(this.ioSpeedMode + 1) % modes.length]);
            this.ioSpeedMode = be.getIOSpeedMode().ordinal();
         }
      }
   }

   public void toggleUnlimited(int slot) {
      if (this.isClientSide()) {
         this.sendClientAction("toggleUnlimited", slot);
      } else {
         if (this.host instanceof OverloadedInterfaceBlockEntity be) {
            boolean nowUnlimited = !be.isSlotUnlimited(slot);
            be.setSlotUnlimited(slot, nowUnlimited);
            if (nowUnlimited) {
               OverloadedInterfaceLogic logic = (OverloadedInterfaceLogic)this.host.getInterfaceLogic();
               AEKey key = logic.getConfig().getKey(slot);
               if (key != null) {
                  logic.setConfigStackSuppressed(slot, new GenericStack(key, 1L));
               }
            }

            long bits = 0L;

            for (int i = 0; i < 36; i++) {
               if (be.isSlotUnlimited(i)) {
                  bits |= 1L << i;
               }
            }

            this.unlimitedBits = bits;
         }
      }
   }

   public boolean isSlotUnlimited(int slot) {
      return (this.unlimitedBits & 1L << slot) != 0L;
   }

   public void m_150399_(int slotId, int button, ClickType clickType, Player player) {
      if (slotId >= 0 && slotId < this.f_38839_.size() && this.storageSlotSet.contains(this.f_38839_.get(slotId))) {
         if (!this.isClientSide()) {
            this.handleStorageInteraction((Slot)this.f_38839_.get(slotId), button, clickType, player);
         }
      } else {
         super.m_150399_(slotId, button, clickType, player);
      }
   }

   private OverloadedInterfaceLogic.ProxiedStorageInv getProxy() {
      return this.host.getInterfaceLogic() instanceof OverloadedInterfaceLogic ol ? ol.getProxiedStorage() : null;
   }

   private void handleStorageInteraction(Slot slot, int button, ClickType clickType, Player player) {
      OverloadedInterfaceLogic.ProxiedStorageInv proxy = this.getProxy();
      if (proxy != null) {
         int idx = slot.m_150661_();
         switch (clickType) {
            case PICKUP:
               this.handlePickup(proxy, idx, button, player);
               break;
            case QUICK_MOVE:
               this.handleQuickMove(proxy, idx, player);
               break;
            case THROW:
               this.handleThrow(proxy, idx, button, player);
               break;
            case SWAP:
               this.handleSwap(proxy, idx, button, player);
               break;
            case CLONE:
               this.handleClone(proxy, idx, player);
         }
      }
   }

   private void handlePickup(OverloadedInterfaceLogic.ProxiedStorageInv proxy, int idx, int button, Player player) {
      ItemStack carried = this.m_142621_();
      if (carried.m_41619_()) {
         AEKey key = proxy.cfg().getKey(idx);
         if (!(key instanceof AEItemKey itemKey)) {
            return;
         }

         long inserted = proxy.capForSlot(idx);
         int maxStack = itemKey.getMaxStackSize();
         long maxExtract = Math.min(inserted, (long)maxStack);
         if (button == 1) {
            maxExtract = Math.max(1L, (maxExtract + 1L) / 2L);
         }

         long extracted = proxy.proxyExtract(key, maxExtract, Actionable.MODULATE);
         if (extracted > 0L) {
            this.m_142503_(itemKey.toStack((int)Math.min(extracted, 2147483647L)));
         }
      } else {
         AEItemKey itemKey = AEItemKey.of(carried);
         if (itemKey == null) {
            return;
         }

         int toInsert = button == 1 ? 1 : carried.m_41613_();
         long insertedx = proxy.proxyInsert(itemKey, (long)toInsert, Actionable.MODULATE);
         if (insertedx > 0L) {
            carried.m_41774_((int)insertedx);
            if (carried.m_41619_()) {
               this.m_142503_(ItemStack.f_41583_);
            }
         }
      }
   }

   private void handleQuickMove(OverloadedInterfaceLogic.ProxiedStorageInv proxy, int idx, Player player) {
      AEKey key = proxy.cfg().getKey(idx);
      if (key instanceof AEItemKey itemKey) {
         long cap = proxy.capForSlot(idx);
         int maxStack = itemKey.getMaxStackSize();
         long maxExtract = Math.min(cap, (long)maxStack);
         long extracted = proxy.proxyExtract(key, maxExtract, Actionable.MODULATE);
         if (extracted > 0L) {
            ItemStack stack = itemKey.toStack((int)Math.min(extracted, 2147483647L));
            if (!player.m_150109_().m_36054_(stack)) {
               player.m_36176_(stack, false);
            }
         }
      }
   }

   private void handleThrow(OverloadedInterfaceLogic.ProxiedStorageInv proxy, int idx, int button, Player player) {
      AEKey key = proxy.cfg().getKey(idx);
      if (key instanceof AEItemKey itemKey) {
         long maxExtract = button == 1 ? Math.min(proxy.capForSlot(idx), (long)itemKey.getMaxStackSize()) : 1L;
         long extracted = proxy.proxyExtract(key, maxExtract, Actionable.MODULATE);
         if (extracted > 0L) {
            player.m_36176_(itemKey.toStack((int)Math.min(extracted, 2147483647L)), true);
         }
      }
   }

   private void handleSwap(OverloadedInterfaceLogic.ProxiedStorageInv proxy, int idx, int button, Player player) {
      if (button >= 0 && button <= 8) {
         ItemStack hotbarStack = player.m_150109_().m_8020_(button);
         AEKey key = proxy.cfg().getKey(idx);
         boolean wantInsert = !hotbarStack.m_41619_();
         boolean wantExtract = key instanceof AEItemKey;
         long simInserted = 0L;
         AEItemKey hbKey = null;
         long simExtracted = 0L;
         AEItemKey itemKey = null;
         if (wantInsert) {
            hbKey = AEItemKey.of(hotbarStack);
            if (hbKey != null) {
               simInserted = proxy.proxyInsert(hbKey, (long)hotbarStack.m_41613_(), Actionable.SIMULATE);
            }
         }

         if (wantExtract) {
            itemKey = (AEItemKey)key;
            long cap = proxy.capForSlot(idx);
            long maxExtract = Math.min(cap, (long)itemKey.getMaxStackSize());
            simExtracted = proxy.proxyExtract(key, maxExtract, Actionable.SIMULATE);
         }

         if (simInserted > 0L || simExtracted > 0L) {
            ItemStack extractedStack = ItemStack.f_41583_;
            if (simExtracted > 0L) {
               long extracted = proxy.proxyExtract(key, simExtracted, Actionable.MODULATE);
               if (extracted > 0L) {
                  extractedStack = itemKey.toStack((int)Math.min(extracted, 2147483647L));
               }
            }

            if (simInserted > 0L && hbKey != null) {
               long inserted = proxy.proxyInsert(hbKey, (long)hotbarStack.m_41613_(), Actionable.MODULATE);
               if (inserted > 0L) {
                  hotbarStack.m_41774_((int)inserted);
               }
            }

            if (!extractedStack.m_41619_()) {
               if (hotbarStack.m_41619_()) {
                  player.m_150109_().m_6836_(button, extractedStack);
               } else if (!player.m_150109_().m_36054_(extractedStack)) {
                  player.m_36176_(extractedStack, false);
               }
            }
         }
      }
   }

   private void handleClone(OverloadedInterfaceLogic.ProxiedStorageInv proxy, int idx, Player player) {
      if (player.m_7500_()) {
         if (proxy.cfg().getKey(idx) instanceof AEItemKey itemKey) {
            this.m_142503_(itemKey.toStack(itemKey.getMaxStackSize()));
         }
      }
   }

   public ItemStack m_7648_(Player player, int idx) {
      if (this.isClientSide()) {
         return ItemStack.f_41583_;
      } else if (idx >= 0 && idx < this.f_38839_.size()) {
         Slot slot = (Slot)this.f_38839_.get(idx);
         if (this.storageSlotSet.contains(slot)) {
            return ItemStack.f_41583_;
         } else if (this.containerSlotSet.contains(slot)) {
            return super.m_7648_(player, idx);
         } else {
            if (slot.m_6657_()) {
               ItemStack stack = slot.m_7993_();
               if (stack.m_41720_() instanceof OverloadedFilterComponentItem) {
                  int beforeCount = stack.m_41613_();
                  ItemStack superResult = super.m_7648_(player, idx);
                  int afterCount = slot.m_6657_() ? slot.m_7993_().m_41613_() : 0;
                  if (afterCount < beforeCount) {
                     return superResult;
                  }
               }

               if (Upgrades.isUpgradeCardItem(stack)) {
                  int beforeCount = stack.m_41613_();
                  ItemStack superResult = super.m_7648_(player, idx);
                  int afterCount = slot.m_6657_() ? slot.m_7993_().m_41613_() : 0;
                  if (afterCount < beforeCount) {
                     return superResult;
                  }
               }

               if (slot.m_6657_()) {
                  stack = slot.m_7993_();
                  OverloadedInterfaceLogic.ProxiedStorageInv proxy = this.getProxy();
                  if (proxy != null) {
                     AEItemKey key = AEItemKey.of(stack);
                     if (key != null) {
                        long inserted = proxy.proxyInsert(key, (long)stack.m_41613_(), Actionable.MODULATE);
                        if (inserted > 0L) {
                           slot.m_6201_((int)inserted);
                           slot.m_6654_();
                        }
                     }
                  }
               }
            }

            return ItemStack.f_41583_;
         }
      } else {
         return ItemStack.f_41583_;
      }
   }

   public void openSetAmountMenu(int configSlot) {
      if (this.isClientSide()) {
         this.sendClientAction("setAmount", configSlot);
      } else {
         GenericStack stack = ((InterfaceLogicHost)this.getHost()).getConfig().getStack(configSlot);
         if (stack != null) {
            SetStockAmountMenu.open((ServerPlayer)this.getPlayer(), this.getLocator(), configSlot, stack.what(), (int)stack.amount());
            if (this.getPlayer().f_36096_ instanceof SetStockAmountMenu sam) {
               long cap = (long)stack.what().getType().getAmountPerByte() * 1024L;

               try {
                  Field f = SetStockAmountMenu.class.getDeclaredField("maxAmount");
                  f.setAccessible(true);
                  f.setInt(sam, (int)Math.min(cap, 2147483647L));
                  sam.m_38946_();
               } catch (ReflectiveOperationException var7) {
                  LoggerFactory.getLogger(OverloadedInterfaceMenu.class).warn("Failed to set maxAmount on SetStockAmountMenu", var7);
               }
            }
         }
      }
   }

   public void m_38946_() {
      if (this.host instanceof OverloadedInterfaceBlockEntity be) {
         this.interfaceMode = be.getInterfaceMode().ordinal();
         this.ioSpeedMode = be.getIOSpeedMode().ordinal();
         this.exportMode = be.getExportMode().ordinal();
         this.importMode = be.getImportMode().ordinal();
         Direction dir = be.getEnergyOutputDir();
         this.energyDirOrdinal = dir != null ? dir.m_122411_() : -1;
         long bits = 0L;

         for (int i = 0; i < 36; i++) {
            if (be.isSlotUnlimited(i)) {
               bits |= 1L << i;
            }
         }

         this.unlimitedBits = bits;
      }

      super.m_38946_();
      if (this.lastShownPage != this.currentPage) {
         this.showPage(this.currentPage);
      }
   }

   static {
      OverloadedSlotSemantics.init();

      try {
         F_SEMANTIC_BY_SLOT = AEBaseMenu.class.getDeclaredField("semanticBySlot");
         F_SEMANTIC_BY_SLOT.setAccessible(true);
         F_SLOTS_BY_SEMANTIC = AEBaseMenu.class.getDeclaredField("slotsBySemantic");
         F_SLOTS_BY_SEMANTIC.setAccessible(true);
      } catch (ReflectiveOperationException var1) {
         throw new IllegalStateException("Failed to reflect AEBaseMenu slot maps", var1);
      }
   }

   private static final class OverloadedFilterSlot extends AppEngSlot {
      private OverloadedFilterSlot(InternalInventory inv, int invSlot) {
         super(inv, invSlot);
      }

      public boolean m_5857_(ItemStack stack) {
         return !stack.m_41619_() && stack.m_41720_() instanceof OverloadedFilterComponentItem;
      }

      public int m_6641_() {
         return 1;
      }
   }
}
