package com.moakiee.ae2lt.me.cell;

import appeng.api.config.Actionable;
import appeng.api.config.FuzzyMode;
import appeng.api.config.IncludeExclude;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.cells.CellState;
import appeng.api.storage.cells.ISaveProvider;
import appeng.api.storage.cells.StorageCell;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.core.AEConfig;
import appeng.core.definitions.AEItems;
import appeng.items.storage.StorageCellTooltipComponent;
import appeng.util.ConfigInventory;
import appeng.util.prioritylist.IPartitionList;
import appeng.util.prioritylist.IPartitionList.Builder;
import com.moakiee.ae2lt.item.VoidStorageCellItem;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.Object2LongMap.Entry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class VoidCellInventory implements StorageCell {
   private final ItemStack stack;
   @Nullable
   private final ISaveProvider saveProvider;
   private final VoidStorageCellItem item;
   private final IPartitionList partitionList;
   private final IncludeExclude partitionMode;
   private final VoidCellMode voidMode;
   private final Object2LongMap<AEKey> storedAmounts;
   private double voidEnergy;
   private boolean persisted = true;

   public VoidCellInventory(ItemStack stack, @Nullable ISaveProvider saveProvider) {
      if (stack.m_41720_() instanceof VoidStorageCellItem voidCellItem) {
         this.stack = stack;
         this.saveProvider = saveProvider;
         this.item = voidCellItem;
         VoidCellData.State var7 = VoidCellData.read(stack);
         this.voidMode = var7.mode();
         this.voidEnergy = var7.energy();
         this.storedAmounts = var7.inventory();
         Builder builder = IPartitionList.builder();
         IUpgradeInventory upgrades = this.getUpgradesInventory();
         if (upgrades.isInstalled(AEItems.FUZZY_CARD)) {
            builder.fuzzyMode(this.getFuzzyMode());
         }

         builder.addAll(this.getConfigInventory().keySet());
         this.partitionMode = upgrades.isInstalled(AEItems.INVERTER_CARD) ? IncludeExclude.BLACKLIST : IncludeExclude.WHITELIST;
         this.partitionList = builder.build();
      } else {
         throw new IllegalArgumentException("Cell is not an ME Void Cell");
      }
   }

   public boolean isPartitioned() {
      return !this.partitionList.isEmpty();
   }

   public CellState getStatus() {
      return CellState.NOT_EMPTY;
   }

   public double getIdleDrain() {
      return 1.0;
   }

   public void persist() {
      if (!this.persisted) {
         VoidCellData.write(this.stack, this.voidMode, this.voidEnergy, this.storedAmounts);
         this.persisted = true;
      }
   }

   public void getAvailableStacks(KeyCounter out) {
      ObjectIterator var2 = this.storedAmounts.object2LongEntrySet().iterator();

      while (var2.hasNext()) {
         Entry<AEKey> entry = (Entry<AEKey>)var2.next();
         if (entry.getLongValue() > 0L) {
            out.add((AEKey)entry.getKey(), entry.getLongValue());
         }
      }
   }

   public long insert(AEKey what, long amount, Actionable mode, IActionSource source) {
      if (amount > 0L && !this.partitionList.isEmpty() && this.partitionList.matchesFilter(what, this.partitionMode)) {
         if (mode == Actionable.MODULATE) {
            this.voidEnergy = this.voidEnergy + (double)amount / (double)what.getAmountPerUnit();
            this.fillOutput();
            this.saveChanges();
         }

         return amount;
      } else {
         return 0L;
      }
   }

   public long extract(AEKey what, long amount, Actionable mode, IActionSource source) {
      if (amount <= 0L) {
         return 0L;
      } else {
         long currentAmount = this.storedAmounts.getLong(what);
         if (currentAmount <= 0L) {
            return 0L;
         } else {
            long extracted = Math.min(amount, currentAmount);
            if (mode == Actionable.MODULATE) {
               if (extracted == currentAmount) {
                  this.storedAmounts.removeLong(what);
               } else {
                  this.storedAmounts.put(what, currentAmount - extracted);
               }

               this.saveChanges();
            }

            return extracted;
         }
      }
   }

   public Component getDescription() {
      return this.stack.m_41786_();
   }

   public boolean isPreferredStorageFor(AEKey what, IActionSource source) {
      return !this.partitionList.isEmpty() && this.partitionList.matchesFilter(what, this.partitionMode);
   }

   private void fillOutput() {
      int requiredPower = this.voidMode.getRequiredPower();
      if (this.voidMode != VoidCellMode.TRASH && requiredPower != 0) {
         AEItemKey output = AEItemKey.of(this.voidMode.getOutput());
         long amount = (long)(this.voidEnergy / (double)requiredPower);
         if (output != null && amount > 0L) {
            this.storedAmounts.put(output, this.storedAmounts.getLong(output) + amount);
            this.voidEnergy -= (double)(amount * (long)requiredPower);
         }
      } else {
         this.voidEnergy = 0.0;
      }
   }

   private ConfigInventory getConfigInventory() {
      return this.item.getConfigInventory(this.stack);
   }

   private IUpgradeInventory getUpgradesInventory() {
      return this.item.getUpgrades(this.stack);
   }

   private FuzzyMode getFuzzyMode() {
      return this.item.getFuzzyMode(this.stack);
   }

   private void saveChanges() {
      this.persisted = false;
      if (this.saveProvider != null) {
         this.saveProvider.saveChanges();
      } else {
         this.persist();
      }
   }

   public Optional<TooltipComponent> getTooltipImage() {
      ArrayList<ItemStack> upgradeStacks = new ArrayList<>();
      if (AEConfig.instance().isTooltipShowCellUpgrades()) {
         for (ItemStack upgrade : this.getUpgradesInventory()) {
            upgradeStacks.add(upgrade);
         }
      }

      boolean hasMoreContent;
      List<GenericStack> content;
      if (AEConfig.instance().isTooltipShowCellContent()) {
         content = new ArrayList<>();
         ObjectIterator maxShown = this.storedAmounts.object2LongEntrySet().iterator();

         while (maxShown.hasNext()) {
            Entry<AEKey> entry = (Entry<AEKey>)maxShown.next();
            if (entry.getLongValue() > 0L) {
               content.add(new GenericStack((AEKey)entry.getKey(), entry.getLongValue()));
            }
         }

         content.sort(Comparator.comparingLong(GenericStack::amount).reversed());
         int maxShownx = AEConfig.instance().getTooltipMaxCellContentShown();
         hasMoreContent = content.size() > maxShownx;
         if (hasMoreContent) {
            content.subList(maxShownx, content.size()).clear();
         }
      } else {
         hasMoreContent = false;
         content = Collections.emptyList();
      }

      return Optional.of(new StorageCellTooltipComponent(upgradeStacks, content, hasMoreContent, true));
   }
}
