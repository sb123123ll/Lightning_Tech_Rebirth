package com.moakiee.ae2lt.item;

import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
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

public final class InfiniteStorageCellItem extends AE2LTItem implements IIndexedStorageCellItem {
   private static final ResourceLocation STORAGE_TYPE = new ResourceLocation("ae2lt", "infinite_cell");
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
      if (bytes < 1000L) {
         return String.format("%,d B", bytes);
      } else if (bytes < 1000000L) {
         return String.format("%.1f KB", (double)bytes / 1000.0);
      } else if (bytes < 1000000000L) {
         return String.format("%.1f MB", (double)bytes / 1000000.0);
      } else {
         return bytes < 1000000000000L ? String.format("%.1f GB", (double)bytes / 1.0E9) : String.format("%.1f TB", (double)bytes / 1.0E12);
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

   public ByteTracker createByteTracker(ItemStack stack, IndexedStorage storage) {
      ByteTracker tracker = new ByteTracker(storage::getTotalTypes);
      tracker.configure(this.bytesPerType, this.maxTypes, this.capacityLo, this.capacityHi);
      return tracker;
   }

   public double idleDrain(ItemStack stack) {
      return this.idleDrain;
   }

   public boolean accepts(ItemStack stack, AEKey key, IActionSource source) {
      return true;
   }

   public void writeSummary(ItemStack stack, IndexedCellSummary summary) {
      ItemStackTagSupport.updateTag(stack, tag -> {
         tag.m_128405_("ae2lt:types", summary.totalTypes());
         tag.m_128356_("ae2lt:bytes", summary.usedBytes());
      });
   }
}
