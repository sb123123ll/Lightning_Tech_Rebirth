package com.moakiee.ae2lt.item;

import appeng.api.config.FuzzyMode;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.storage.cells.ICellWorkbenchItem;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.UpgradeInventories;
import appeng.items.contents.CellConfig;
import appeng.util.ConfigInventory;
import com.moakiee.ae2lt.util.ItemStackTagSupport;
import com.moakiee.thunderbolt.core.storage.cell.ByteTracker;
import com.moakiee.thunderbolt.core.storage.cell.IIndexedStorageCellItem;
import com.moakiee.thunderbolt.core.storage.cell.IndexedCellSummary;
import com.moakiee.thunderbolt.core.storage.cell.IndexedStorage;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;

public final class InfiniteStorageCellItem extends AE2LTItem implements IIndexedStorageCellItem, ICellWorkbenchItem {
   private static final ResourceLocation STORAGE_TYPE = new ResourceLocation("ae2lt", "infinite_cell");
   private static final String FUZZY_MODE_TAG = "FuzzyMode";
   private final long capacityLo;
   private final long capacityHi;
   private final int bytesPerType;
   private final int maxTypes;
   private final double idleDrain;

   public InfiniteStorageCellItem(Properties props, long capacityLo, long capacityHi, int bytesPerType, int maxTypes, double idleDrain) {
      super(props.m_41487_(1));
      this.capacityLo = capacityLo;
      this.capacityHi = capacityHi;
      this.bytesPerType = bytesPerType;
      this.maxTypes = maxTypes;
      this.idleDrain = idleDrain;
   }

   @Override
   public void appendHoverText(ItemStack stack, AE2LTItem.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
      CompoundTag tag = ItemStackTagSupport.getTagCopy(stack);
      if (tag.m_128441_("ae2lt:types") || tag.m_128441_("ae2lt:bytes")) {
         int types = tag.m_128451_("ae2lt:types");
         long bytes = tag.m_128454_("ae2lt:bytes");
         tooltipComponents.add(
            Component.m_237110_("tooltip.ae2lt.infinite_cell.types", new Object[]{String.format("%,d", types)}).m_130940_(ChatFormatting.GRAY)
         );
         tooltipComponents.add(Component.m_237110_("tooltip.ae2lt.infinite_cell.bytes", new Object[]{formatBytes(bytes)}).m_130940_(ChatFormatting.GRAY));
      }
   }

   private static String formatBytes(long bytes) {
      if (bytes < 0L) {
         return "0 B";
      } else if (bytes < 1024L) {
         return String.format("%,d B", bytes);
      } else if (bytes < 1048576L) {
         return String.format("%.1f KiB", (double)bytes / 1024.0);
      } else if (bytes < 1073741824L) {
         return String.format("%.1f MiB", (double)bytes / 1048576.0);
      } else if (bytes < 1099511627776L) {
         return String.format("%.2f GiB", (double)bytes / 1073741824.0);
      } else if (bytes < 1125899906842624L) {
         return String.format("%.2f TiB", (double)bytes / 1099511627776.0);
      } else {
         return String.format("%.2f PiB", (double)bytes / 1125899906842624.0);
      }
   }

   public long getCapacityLo() {
      return this.capacityLo;
   }

   public long getCapacityHi() {
      return this.capacityHi;
   }

   public int getBytesPerType() {
      return this.bytesPerType;
   }

   public int getMaxTypes() {
      return this.maxTypes;
   }

   public double getIdleDrain() {
      return this.idleDrain;
   }

   public ResourceLocation storageType(ItemStack stack) {
      return STORAGE_TYPE;
   }

   public String cellIdTag(ItemStack stack) {
      return "ae2lt:cell_id";
   }

   public boolean isPreferred(ItemStack stack, AEKey key, IndexedStorage storage, IActionSource source) {
      ConfigInventory config = this.getConfigInventory(stack);
      if (!config.keySet().isEmpty()) {
         IUpgradeInventory upgrades = this.getUpgrades(stack);
         boolean isBlacklist = upgrades.isInstalled(appeng.core.definitions.AEItems.INVERTER_CARD);
         appeng.util.prioritylist.IPartitionList.Builder builder = appeng.util.prioritylist.IPartitionList.builder();
         if (upgrades.isInstalled(appeng.core.definitions.AEItems.FUZZY_CARD)) {
            builder.fuzzyMode(this.getFuzzyMode(stack));
         }
         builder.addAll(config.keySet());
         appeng.util.prioritylist.IPartitionList partitionList = builder.build();
         return partitionList.matchesFilter(key, isBlacklist ? appeng.api.config.IncludeExclude.BLACKLIST : appeng.api.config.IncludeExclude.WHITELIST);
      }
      return storage.containsKey(key);
   }

   public ByteTracker createByteTracker(ItemStack stack, IndexedStorage storage) {
      ByteTracker tracker = new ByteTracker(storage::getTotalTypes);
      tracker.configure(this.bytesPerType, this.maxTypes, this.capacityLo, this.capacityHi);
      return tracker;
   }

   public double idleDrain(ItemStack stack) {
      return this.idleDrain;
   }

   public boolean accepts(ItemStack stack, AEKey key, IActionSource source) {
      ConfigInventory config = this.getConfigInventory(stack);
      if (config.keySet().isEmpty()) {
         return true;
      }
      IUpgradeInventory upgrades = this.getUpgrades(stack);
      boolean isBlacklist = upgrades.isInstalled(appeng.core.definitions.AEItems.INVERTER_CARD);
      appeng.util.prioritylist.IPartitionList.Builder builder = appeng.util.prioritylist.IPartitionList.builder();
      if (upgrades.isInstalled(appeng.core.definitions.AEItems.FUZZY_CARD)) {
         builder.fuzzyMode(this.getFuzzyMode(stack));
      }
      builder.addAll(config.keySet());
      appeng.util.prioritylist.IPartitionList partitionList = builder.build();
      return partitionList.matchesFilter(key, isBlacklist ? appeng.api.config.IncludeExclude.BLACKLIST : appeng.api.config.IncludeExclude.WHITELIST);
   }

   public void writeSummary(ItemStack stack, IndexedCellSummary summary) {
      int types = summary.totalTypes();
      long bytes = types == 0 ? 0L : summary.usedBytes();
      ItemStackTagSupport.updateTag(stack, tag -> {
         tag.m_128405_("ae2lt:types", types);
         tag.m_128356_("ae2lt:bytes", bytes);
      });
   }

   @Override
   public boolean isEditable(ItemStack stack) {
      return true;
   }

   @Override
   public IUpgradeInventory getUpgrades(ItemStack stack) {
      return UpgradeInventories.forItem(stack, 2);
   }

   @Override
   public ConfigInventory getConfigInventory(ItemStack stack) {
      return CellConfig.create(stack);
   }

   @Override
   public FuzzyMode getFuzzyMode(ItemStack stack) {
      CompoundTag tag = stack.m_41783_();
      if (tag == null) {
         return FuzzyMode.IGNORE_ALL;
      } else {
         try {
            return FuzzyMode.valueOf(tag.m_128461_(FUZZY_MODE_TAG));
         } catch (IllegalArgumentException var4) {
            return FuzzyMode.IGNORE_ALL;
         }
      }
   }

   @Override
   public void setFuzzyMode(ItemStack stack, FuzzyMode mode) {
      stack.m_41784_().m_128359_(FUZZY_MODE_TAG, mode.name());
   }

   @Override
   public java.util.Optional<net.minecraft.world.inventory.tooltip.TooltipComponent> m_142422_(ItemStack stack) {
      java.util.List<ItemStack> upgradeStacks = new java.util.ArrayList<>();
      if (appeng.core.AEConfig.instance().isTooltipShowCellUpgrades()) {
         for (ItemStack upgrade : this.getUpgrades(stack)) {
            upgradeStacks.add(upgrade);
         }
      }
      return upgradeStacks.isEmpty()
         ? java.util.Optional.empty()
         : java.util.Optional.of(new appeng.items.storage.StorageCellTooltipComponent(upgradeStacks, java.util.Collections.emptyList(), false));
   }
}
