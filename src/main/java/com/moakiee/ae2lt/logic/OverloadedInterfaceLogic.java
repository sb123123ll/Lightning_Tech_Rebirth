package com.moakiee.ae2lt.logic;

import appeng.api.config.Actionable;
import appeng.api.config.FuzzyMode;
import appeng.api.config.Settings;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.storage.IStorageService;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.AEKeyTypes;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.UpgradeInventories;
import appeng.core.definitions.AEItems;
import appeng.helpers.InterfaceLogic;
import appeng.helpers.MultiCraftingTracker;
import appeng.helpers.externalstorage.GenericStackInv.Mode;
import appeng.util.ConfigInventory;
import appeng.util.ConfigMenuInventory;
import com.google.common.collect.Sets;
import com.moakiee.ae2lt.blockentity.OverloadedInterfaceBlockEntity;
import com.moakiee.ae2lt.logic.energy.PowerCostUtil;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2LongMap.Entry;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public class OverloadedInterfaceLogic extends InterfaceLogic {
   private static final Logger LOG = LogUtils.getLogger();
   private static final long OVERLOADED_BYTES = 1024L;
   private static final Field F_CONFIG;
   private static final Field F_STORAGE;
   private static final Field F_UPGRADES;
   private static final Field F_CRAFTING_TRACKER;
   private static final Method M_ON_CONFIG_CHANGED;
   private static final Method M_ON_STORAGE_CHANGED;
   private static final Method M_ON_UPGRADES_CHANGED;
   private final OverloadedInterfaceBlockEntity owner;
   private final OverloadedInterfaceLogic.ProxiedStorageInv proxiedStorage;
   private final int slotCount;
   private final IUpgradeInventory ourUpgrades;
   private final MultiCraftingTracker craftingTrackerRef;
   private static final long REACTIVE_COOLDOWN_TICKS = 5L;
   private long lastReactiveTick = -1L;

   public OverloadedInterfaceLogic(IManagedGridNode gridNode, OverloadedInterfaceBlockEntity host, Item is, int slots) {
      super(gridNode, host, is, slots);
      this.owner = host;
      this.slotCount = slots;

      try {
         this.craftingTrackerRef = (MultiCraftingTracker)F_CRAFTING_TRACKER.get(this);
      } catch (IllegalAccessException var8) {
         throw new IllegalStateException("Failed to read craftingTracker", var8);
      }

      OverloadedInterfaceLogic.OverloadedConfigInv newConfig = new OverloadedInterfaceLogic.OverloadedConfigInv(
         Sets.newHashSet(AEKeyTypes.getAll()), null, Mode.CONFIG_STACKS, slots, () -> invokeQuietly(M_ON_CONFIG_CHANGED, this)
      );
      newConfig.owner = host;
      this.setField(F_CONFIG, newConfig);
      newConfig.useRegisteredCapacities();

      for (AEKeyType type : AEKeyTypes.getAll()) {
         newConfig.setCapacity(type, overloadedCap(type));
      }

      this.proxiedStorage = new OverloadedInterfaceLogic.ProxiedStorageInv(
         this, Sets.newHashSet(AEKeyTypes.getAll()), null, slots, () -> invokeQuietly(M_ON_STORAGE_CHANGED, this)
      );
      this.setField(F_STORAGE, this.proxiedStorage);
      this.proxiedStorage.useRegisteredCapacities();

      for (AEKeyType type : AEKeyTypes.getAll()) {
         this.proxiedStorage.setCapacity(type, overloadedCap(type));
      }

      IUpgradeInventory newUpgrades = UpgradeInventories.forMachine(is, 4, () -> {
         invokeQuietly(M_ON_UPGRADES_CHANGED, this);
         host.invalidateInductionCardCache();
      });
      this.setField(F_UPGRADES, newUpgrades);
      this.ourUpgrades = newUpgrades;
      this.mainNode.addService(IGridTickable.class, new OverloadedInterfaceLogic.ProxyTicker());
   }

   OverloadedInterfaceBlockEntity getOwner() {
      return this.owner;
   }

   public OverloadedInterfaceLogic.ProxiedStorageInv getProxiedStorage() {
      return this.proxiedStorage;
   }

   public void setConfigStackSuppressed(int slot, @Nullable GenericStack stack) {
      ConfigInventory cfg = this.getConfig();
      if (cfg instanceof OverloadedInterfaceLogic.OverloadedConfigInv oci) {
         oci.suppressUnlimitedCancel = true;

         try {
            oci.setStack(slot, stack);
         } finally {
            oci.suppressUnlimitedCancel = false;
         }
      } else {
         cfg.setStack(slot, stack);
      }
   }

   public void addDrops(List<ItemStack> drops) {
      for (ItemStack is : this.getUpgrades()) {
         if (!is.m_41619_()) {
            drops.add(is);
         }
      }

      ItemStack filterStack = this.owner.getFilterInv().getStackInSlot(0);
      if (!filterStack.m_41619_()) {
         drops.add(filterStack);
      }

      this.owner.addImportBufferDrops(drops);
   }

   public void clearContent() {
      this.getUpgrades().clear();
      this.owner.getFilterInv().setItemDirect(0, ItemStack.f_41583_);
      this.owner.clearImportBuffer();
   }

   private boolean invokeCrafting(int slot, AEKey what, long amount) {
      IGrid grid = this.mainNode.getGrid();
      return grid != null && what != null
         ? this.craftingTrackerRef.handleCrafting(slot, what, amount, this.host.getBlockEntity().m_58904_(), grid.getCraftingService(), this.actionSource)
         : false;
   }

   void onExtractDeficit(AEKey what) {
      if (this.ourUpgrades.isInstalled(AEItems.CRAFTING_CARD)) {
         Level level = this.host.getBlockEntity().m_58904_();
         if (level != null) {
            long now = level.m_46467_();
            if (now - this.lastReactiveTick >= 5L) {
               this.lastReactiveTick = now;
               ConfigInventory cfg = this.getConfig();

               for (int i = 0; i < this.slotCount; i++) {
                  AEKey key = cfg.getKey(i);
                  if (key != null && key.equals(what)) {
                     long cap = this.owner.isSlotUnlimited(i) ? overloadedCap(what) : cfg.getAmount(i);
                     this.invokeCrafting(i, what, cap);
                     break;
                  }
               }
            }
         }
      }
   }

   private static long overloadedCap(AEKey what) {
      return overloadedCap(what.getType());
   }

   private static long overloadedCap(AEKeyType type) {
      return 1024L * (long)type.getAmountPerByte();
   }

   private void setField(Field f, Object value) {
      try {
         f.set(this, value);
      } catch (IllegalAccessException var4) {
         throw new IllegalStateException("Reflection set failed", var4);
      }
   }

   private static void invokeQuietly(Method m, Object target) {
      try {
         m.invoke(target);
      } catch (Exception var3) {
         LOG.warn("Reflection invoke failed: {}.{}", new Object[]{m.getDeclaringClass().getSimpleName(), m.getName(), var3});
      }
   }

   static {
      try {
         F_CONFIG = InterfaceLogic.class.getDeclaredField("config");
         F_CONFIG.setAccessible(true);
         F_STORAGE = InterfaceLogic.class.getDeclaredField("storage");
         F_STORAGE.setAccessible(true);
         F_UPGRADES = InterfaceLogic.class.getDeclaredField("upgrades");
         F_UPGRADES.setAccessible(true);
         F_CRAFTING_TRACKER = InterfaceLogic.class.getDeclaredField("craftingTracker");
         F_CRAFTING_TRACKER.setAccessible(true);
         M_ON_CONFIG_CHANGED = InterfaceLogic.class.getDeclaredMethod("onConfigRowChanged");
         M_ON_CONFIG_CHANGED.setAccessible(true);
         M_ON_STORAGE_CHANGED = InterfaceLogic.class.getDeclaredMethod("onStorageChanged");
         M_ON_STORAGE_CHANGED.setAccessible(true);
         M_ON_UPGRADES_CHANGED = InterfaceLogic.class.getDeclaredMethod("onUpgradesChanged");
         M_ON_UPGRADES_CHANGED.setAccessible(true);
      } catch (ReflectiveOperationException var1) {
         throw new IllegalStateException("Failed to init reflection for OverloadedInterfaceLogic", var1);
      }
   }

   static class OverloadedConfigInv extends ConfigInventory {
      @Nullable
      OverloadedInterfaceBlockEntity owner;
      boolean suppressUnlimitedCancel;
      private final Set<AEKeyType> supportedTypes;
      @Nullable
      private final BiPredicate<Integer, AEKey> slotFilter;

      OverloadedConfigInv(Set<AEKeyType> supportedTypes, @Nullable BiPredicate<Integer, AEKey> slotFilter, Mode mode, int size, @Nullable Runnable listener) {
         super(key -> supportedTypes.contains(key.getType()), mode, size, listener, false);
         this.supportedTypes = supportedTypes;
         this.slotFilter = slotFilter;
      }

      public boolean isSupportedType(AEKeyType type) {
         return this.supportedTypes.contains(type);
      }

      public boolean isSupportedKey(AEKey what) {
         return what != null && this.isSupportedType(what.getType());
      }

      public boolean isAllowedIn(int slot, AEKey what) {
         return this.isSupportedKey(what) && (this.slotFilter == null || this.slotFilter.test(slot, what));
      }

      public long getMaxAmount(AEKey key) {
         return this.getCapacity(key.getType());
      }

      public long insert(int slot, AEKey what, long amount, Actionable mode) {
         return !this.isAllowedIn(slot, what) ? 0L : super.insert(slot, what, amount, mode);
      }

      public void setStack(int slot, @Nullable GenericStack stack) {
         if (stack == null || this.isAllowedIn(slot, stack.what())) {
            super.setStack(slot, stack);
            if (!this.suppressUnlimitedCancel && this.owner != null && this.owner.isSlotUnlimited(slot)) {
               Level level = this.owner.m_58904_();
               if (level != null && !level.m_5776_()) {
                  this.owner.setSlotUnlimited(slot, false);
               }
            }

            if (this.owner != null) {
               Level level = this.owner.m_58904_();
               if (level != null && !level.m_5776_()) {
                  this.owner.onGridIoConfigChanged();
               }
            }
         }
      }
   }

   static class ProxiedMenuWrapper extends ConfigMenuInventory {
      private final OverloadedInterfaceLogic.ProxiedStorageInv proxy;

      ProxiedMenuWrapper(OverloadedInterfaceLogic.ProxiedStorageInv proxy) {
         super(proxy);
         this.proxy = proxy;
      }

      public ItemStack extractItem(int slot, int amount, boolean simulate) {
         return ItemStack.f_41583_;
      }

      public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
         return stack;
      }

      public void setItemDirect(int slotIndex, ItemStack stack) {
         if (stack.m_41619_()) {
            this.proxy.setDisplayStack(slotIndex, null);
         } else {
            this.proxy.setDisplayStack(slotIndex, this.convertToSuitableStack(stack));
         }
      }

      public boolean isItemValid(int slot, ItemStack stack) {
         return false;
      }
   }

   public static class ProxiedStorageInv extends OverloadedInterfaceLogic.OverloadedConfigInv {
      private final OverloadedInterfaceLogic logic;
      private boolean proxying = false;
      private KeyCounter availableStacksCache = new KeyCounter();
      private long availableStacksCacheTick = Long.MIN_VALUE;
      private final long[] displayAmountCache;
      private long displayAmountCacheTick = Long.MIN_VALUE;

      ProxiedStorageInv(
         OverloadedInterfaceLogic logic, Set<AEKeyType> supportedTypes, @Nullable BiPredicate<Integer, AEKey> slotFilter, int size, @Nullable Runnable listener
      ) {
         super(supportedTypes, slotFilter, Mode.STORAGE, size, listener);
         this.logic = logic;
         this.displayAmountCache = new long[size];
      }

      public long getCapacity(AEKeyType keyType) {
         return keyType == AEKeyType.items() ? 2147483647L : super.getCapacity(keyType);
      }

      private IActionSource src() {
         return IActionSource.ofMachine(this.logic.owner);
      }

      public ConfigInventory cfg() {
         return this.logic.getConfig();
      }

      public long capForSlot(int slot) {
         if (this.logic.owner.isSlotUnlimited(slot)) {
            return Long.MAX_VALUE;
         } else {
            long amt = this.cfg().getAmount(slot);
            if (amt > 0L) {
               return amt;
            } else {
               AEKey key = this.cfg().getKey(slot);
               return key != null ? (long)key.getType().getAmountPerUnit() : Long.MAX_VALUE;
            }
         }
      }

      private long displayAmount(int slot, AEKey key) {
         BlockEntity be = this.logic.host.getBlockEntity();
         Level level = be != null ? be.m_58904_() : null;
         long now = level != null ? level.m_46467_() : Long.MIN_VALUE;
         boolean cacheable = level != null && slot >= 0 && slot < this.displayAmountCache.length;
         if (cacheable) {
            if (now != this.displayAmountCacheTick) {
               Arrays.fill(this.displayAmountCache, -1L);
               this.displayAmountCacheTick = now;
            }

            long cached = this.displayAmountCache[slot];
            if (cached >= 0L) {
               return cached;
            }
         }

         long amt = this.visibleNetworkAmount(key, this.capForSlot(slot));
         if (cacheable) {
            this.displayAmountCache[slot] = amt;
         }

         return amt;
      }

      private long visibleNetworkAmount(AEKey key, long cap) {
         IGrid grid = this.logic.mainNode.getGrid();
         if (grid != null && key != null) {
            IStorageService storageService = grid.getStorageService();
            long reported = storageService.getCachedInventory().get(key);
            long simulated = this.simulateNetworkExtraction(storageService.getInventory(), key, cap);
            return OverloadedAmountMath.mergeReportedAndSimulatedAmount(reported, simulated, cap);
         } else {
            return 0L;
         }
      }

      private long simulateNetworkExtraction(MEStorage network, AEKey key, long cap) {
         if (network != null && key != null && cap > 0L) {
            boolean wasProxying = this.proxying;
            this.proxying = true;

            long var6;
            try {
               var6 = network.extract(key, cap, Actionable.SIMULATE, this.src());
            } finally {
               this.proxying = wasProxying;
            }

            return var6;
         } else {
            return 0L;
         }
      }

      private boolean matchesConfiguredSlot(int slot, AEKey what) {
         AEKey configured = this.cfg().getKey(slot);
         if (configured == null) {
            return false;
         } else if (configured.equals(what)) {
            return true;
         } else if (!this.logic.ourUpgrades.isInstalled(AEItems.FUZZY_CARD)) {
            return false;
         } else if (!configured.supportsFuzzyRangeSearch()) {
            return false;
         } else {
            FuzzyMode fuzzyMode = (FuzzyMode)this.logic.getConfigManager().getSetting(Settings.FUZZY_MODE);
            return configured.fuzzyEquals(what, fuzzyMode);
         }
      }

      private long exposedAmount(AEKey what) {
         IGrid grid = this.logic.mainNode.getGrid();
         if (grid == null) {
            return 0L;
         } else {
            long capSum = 0L;

            for (int i = 0; i < this.size(); i++) {
               if (this.matchesConfiguredSlot(i, what)) {
                  capSum = OverloadedAmountMath.saturatingAdd(capSum, this.capForSlot(i));
                  if (capSum == Long.MAX_VALUE) {
                     break;
                  }
               }
            }

            return capSum > 0L ? this.visibleNetworkAmount(what, capSum) : 0L;
         }
      }

      @Nullable
      public GenericStack getStack(int slot) {
         if (this.logic.mainNode.getGrid() == null) {
            return super.getStack(slot);
         } else {
            AEKey key = this.cfg().getKey(slot);
            if (key == null) {
               this.stacks[slot] = null;
               return null;
            } else {
               long amt = this.displayAmount(slot, key);
               GenericStack result = amt > 0L ? new GenericStack(key, amt) : null;
               this.stacks[slot] = result;
               return result;
            }
         }
      }

      @Nullable
      public AEKey getKey(int slot) {
         return this.logic.mainNode.getGrid() == null ? super.getKey(slot) : this.cfg().getKey(slot);
      }

      public long getAmount(int slot) {
         if (this.logic.mainNode.getGrid() == null) {
            return super.getAmount(slot);
         } else {
            AEKey key = this.cfg().getKey(slot);
            return key == null ? 0L : this.displayAmount(slot, key);
         }
      }

      @Override
      public boolean isAllowedIn(int slot, AEKey what) {
         return this.isSupportedKey(what);
      }

      public ConfigMenuInventory createMenuWrapper() {
         return new OverloadedInterfaceLogic.ProxiedMenuWrapper(this);
      }

      @Override
      public long insert(int slot, AEKey what, long amount, Actionable mode) {
         return what != null && amount > 0L ? this.proxyInsert(what, amount, mode) : 0L;
      }

      public long extract(int slot, AEKey what, long amount, Actionable mode) {
         if (what != null && amount > 0L) {
            AEKey key = this.cfg().getKey(slot);
            if (key != null && key.equals(what)) {
               long capped = Math.min(amount, this.capForSlot(slot));
               long extracted = this.proxyExtract(what, capped, mode);
               if (extracted > 0L && mode == Actionable.MODULATE) {
                  this.logic.onExtractDeficit(what);
               }

               return extracted;
            } else {
               return 0L;
            }
         } else {
            return 0L;
         }
      }

      public long extract(AEKey what, long amount, Actionable mode, IActionSource source) {
         if (this.proxying) {
            return 0L;
         } else if (what != null && amount > 0L) {
            long exposed = this.exposedAmount(what);
            if (exposed <= 0L) {
               return 0L;
            } else {
               long extracted = this.proxyExtract(what, Math.min(amount, exposed), mode);
               if (extracted > 0L && mode == Actionable.MODULATE) {
                  this.logic.onExtractDeficit(what);
               }

               return extracted;
            }
         } else {
            return 0L;
         }
      }

      public long insert(AEKey what, long amount, Actionable mode, IActionSource source) {
         if (this.proxying) {
            return 0L;
         } else {
            return what != null && amount > 0L ? this.proxyInsert(what, amount, mode) : 0L;
         }
      }

      public long proxyExtract(AEKey what, long amount, Actionable mode) {
         if (this.proxying) {
            return 0L;
         } else {
            this.proxying = true;

            long extracted;
            try {
               IGrid grid = this.logic.mainNode.getGrid();
               if (grid == null) {
                  return 0L;
               }

               MEStorage network = grid.getStorageService().getInventory();
               long affordable = PowerCostUtil.maxAffordable(grid, what, amount);
               if (affordable > 0L) {
                  extracted = network.extract(what, affordable, mode, this.src());
                  if (extracted > 0L && mode == Actionable.MODULATE) {
                     PowerCostUtil.consume(grid, what, extracted);
                  }

                  return extracted;
               }

               extracted = 0L;
            } finally {
               this.proxying = false;
            }

            return extracted;
         }
      }

      public long proxyInsert(AEKey what, long amount, Actionable mode) {
         if (this.proxying) {
            return 0L;
         } else {
            this.proxying = true;

            long inserted;
            try {
               IGrid grid = this.logic.mainNode.getGrid();
               if (grid == null) {
                  return 0L;
               }

               MEStorage network = grid.getStorageService().getInventory();
               long affordable = PowerCostUtil.maxAffordable(grid, what, amount);
               if (affordable > 0L) {
                  inserted = network.insert(what, affordable, mode, this.src());
                  if (inserted > 0L && mode == Actionable.MODULATE) {
                     PowerCostUtil.consume(grid, what, inserted);
                  }

                  return inserted;
               }

               inserted = 0L;
            } finally {
               this.proxying = false;
            }

            return inserted;
         }
      }

      public void setDisplayStack(int slot, @Nullable GenericStack stack) {
         this.stacks[slot] = stack;
      }

      public void getAvailableStacks(KeyCounter out) {
         if (!this.proxying) {
            IGrid grid = this.logic.mainNode.getGrid();
            if (grid != null) {
               BlockEntity be = this.logic.host.getBlockEntity();
               Level level = be != null ? be.m_58904_() : null;
               long now = level != null ? level.m_46467_() : Long.MIN_VALUE;
               if (level != null && now == this.availableStacksCacheTick) {
                  for (Entry<AEKey> entry : this.availableStacksCache) {
                     out.add((AEKey)entry.getKey(), entry.getLongValue());
                  }
               } else {
                  this.proxying = true;

                  try {
                     KeyCounter fresh = new KeyCounter();
                     KeyCounter cache = grid.getStorageService().getCachedInventory();
                     boolean fuzzy = this.logic.ourUpgrades.isInstalled(AEItems.FUZZY_CARD);
                     FuzzyMode fuzzyMode = fuzzy ? (FuzzyMode)this.logic.getConfigManager().getSetting(Settings.FUZZY_MODE) : null;
                     LinkedHashMap<AEKey, Long> capByKey = new LinkedHashMap<>();

                     for (int slot = 0; slot < this.size(); slot++) {
                        AEKey key = this.cfg().getKey(slot);
                        if (key != null) {
                           capByKey.merge(key, Long.valueOf(this.capForSlot(slot)), OverloadedAmountMath::saturatingAdd);
                        }
                     }

                     LinkedHashMap<AEKey, Long> capByVariant = new LinkedHashMap<>();
                     LinkedHashMap<AEKey, Long> amountByVariant = new LinkedHashMap<>();

                     for (java.util.Map.Entry<AEKey, Long> capEntry : capByKey.entrySet()) {
                        AEKey key = capEntry.getKey();
                        long cap = capEntry.getValue();
                        long configuredAmount = this.visibleNetworkAmount(key, Long.MAX_VALUE);
                        OverloadedAmountMath.mergeSharedExposure(capByVariant, amountByVariant, key, cap, configuredAmount);
                        if (fuzzy && key.supportsFuzzyRangeSearch()) {
                           for (Entry<AEKey> entry : cache.findFuzzy(key, fuzzyMode)) {
                              AEKey variant = (AEKey)entry.getKey();
                              if (!variant.equals(key)) {
                                 OverloadedAmountMath.mergeSharedExposure(capByVariant, amountByVariant, variant, cap, entry.getLongValue());
                              }
                           }
                        }
                     }

                     for (java.util.Map.Entry<AEKey, Long> exposedEntry : capByVariant.entrySet()) {
                        AEKey variant = exposedEntry.getKey();
                        long cap = exposedEntry.getValue();
                        long networkAmount = amountByVariant.getOrDefault(variant, 0L);
                        long amount = OverloadedAmountMath.capVisibleAmount(networkAmount, cap);
                        if (amount > 0L) {
                           fresh.add(variant, amount);
                        }
                     }

                     this.availableStacksCache = fresh;
                     this.availableStacksCacheTick = now;

                     for (Entry<AEKey> entryx : fresh) {
                        out.add((AEKey)entryx.getKey(), entryx.getLongValue());
                     }
                  } finally {
                     this.proxying = false;
                  }
               }
            }
         }
      }

      public void writeToChildTag(CompoundTag tag, String name) {
         tag.m_128473_(name);
      }

      public void readFromChildTag(CompoundTag tag, String name) {
      }
   }

   private class ProxyTicker implements IGridTickable {
      private static final int MIN_TICKS = 1;
      private static final int MAX_TICKS = 5;

      public TickingRequest getTickingRequest(IGridNode node) {
         return new TickingRequest(1, 5, false, true);
      }

      public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
         if (!OverloadedInterfaceLogic.this.mainNode.isActive()) {
            return TickRateModulation.IDLE;
         } else {
            boolean hasItemIoWork = OverloadedInterfaceLogic.this.owner.hasGridItemIoWork();
            if (hasItemIoWork) {
               OverloadedInterfaceLogic.this.owner.tickGridItemIo();
            }

            boolean craftingCardInstalled = OverloadedInterfaceLogic.this.ourUpgrades.isInstalled(AEItems.CRAFTING_CARD);
            if (!craftingCardInstalled) {
               return OverloadedInterfaceTickDecider.gridTickModulation(hasItemIoWork, false, false);
            } else {
               IGrid grid = OverloadedInterfaceLogic.this.mainNode.getGrid();
               if (grid == null) {
                  return TickRateModulation.IDLE;
               } else {
                  KeyCounter cache = grid.getStorageService().getCachedInventory();
                  ConfigInventory cfg = OverloadedInterfaceLogic.this.getConfig();
                  boolean didWork = false;

                  for (int i = 0; i < OverloadedInterfaceLogic.this.slotCount; i++) {
                     GenericStack cfgStack = cfg.getStack(i);
                     if (cfgStack != null) {
                        long cap = OverloadedInterfaceLogic.this.owner.isSlotUnlimited(i) ? Long.MAX_VALUE : cfgStack.amount();
                        if (cap == Long.MAX_VALUE) {
                           didWork |= OverloadedInterfaceLogic.this.invokeCrafting(i, cfgStack.what(), OverloadedInterfaceLogic.overloadedCap(cfgStack.what()));
                        } else {
                           long deficit = cap - cache.get(cfgStack.what());
                           if (deficit > 0L) {
                              didWork |= OverloadedInterfaceLogic.this.invokeCrafting(i, cfgStack.what(), deficit);
                           }
                        }
                     }
                  }

                  return OverloadedInterfaceTickDecider.gridTickModulation(hasItemIoWork, true, didWork);
               }
            }
         }
      }
   }
}
