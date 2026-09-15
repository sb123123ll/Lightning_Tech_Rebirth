package com.moakiee.ae2lt.item;

import appeng.core.localization.PlayerMessages;
import appeng.util.InteractionUtil;
import com.moakiee.ae2lt.registry.ModItems;
import com.moakiee.ae2lt.util.ItemStackTagSupport;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class BulkLightningStorageCellItem extends Item {
   private static final String TAG_HIGH_VOLTAGE = "ae2lt:bulk_lightning_high_voltage";
   private static final String TAG_EXTREME_HIGH_VOLTAGE = "ae2lt:bulk_lightning_extreme_high_voltage";
   private final double idleDrain;

   public BulkLightningStorageCellItem(Properties properties, double idleDrain) {
      super(properties.m_41487_(1));
      this.idleDrain = idleDrain;
   }

   public double getIdleDrain() {
      return this.idleDrain;
   }

   public static BulkLightningStorageCellItem.StoredAmounts readStoredAmounts(ItemStack stack) {
      CompoundTag tag = ItemStackTagSupport.getTagCopy(stack);
      return new BulkLightningStorageCellItem.StoredAmounts(
         sanitize(tag.m_128454_("ae2lt:bulk_lightning_high_voltage")), sanitize(tag.m_128454_("ae2lt:bulk_lightning_extreme_high_voltage"))
      );
   }

   public static void writeStoredAmounts(ItemStack stack, long highVoltage, long extremeHighVoltage) {
      long sanitizedHighVoltage = sanitize(highVoltage);
      long sanitizedExtremeHighVoltage = sanitize(extremeHighVoltage);
      ItemStackTagSupport.updateTag(stack, tag -> {
         putOrRemove(tag, "ae2lt:bulk_lightning_high_voltage", sanitizedHighVoltage);
         putOrRemove(tag, "ae2lt:bulk_lightning_extreme_high_voltage", sanitizedExtremeHighVoltage);
      });
   }

   public InteractionResultHolder<ItemStack> m_7203_(Level level, Player player, InteractionHand hand) {
      this.disassembleCell(player.m_21120_(hand), level, player);
      return new InteractionResultHolder(InteractionResult.m_19078_(level.m_5776_()), player.m_21120_(hand));
   }

   public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
      return this.disassembleCell(stack, context.m_43725_(), context.m_43723_())
         ? InteractionResult.m_19078_(context.m_43725_().m_5776_())
         : InteractionResult.PASS;
   }

   public void m_7373_(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
      BulkLightningStorageCellItem.StoredAmounts amounts = readStoredAmounts(stack);
      tooltipComponents.add(Component.m_237115_("tooltip.ae2lt.bulk_lightning_storage.capacity").m_130940_(ChatFormatting.DARK_PURPLE));
      tooltipComponents.add(
         Component.m_237110_("tooltip.ae2lt.bulk_lightning_storage.high_voltage", new Object[]{String.format("%,d", amounts.highVoltage())})
            .m_130940_(ChatFormatting.GRAY)
      );
      tooltipComponents.add(
         Component.m_237110_("tooltip.ae2lt.bulk_lightning_storage.extreme_high_voltage", new Object[]{String.format("%,d", amounts.extremeHighVoltage())})
            .m_130940_(ChatFormatting.GRAY)
      );
   }

   private boolean disassembleCell(ItemStack stack, Level level, Player player) {
      if (player != null && InteractionUtil.isInAlternateUseMode(player)) {
         List<ItemStack> disassembledStacks = List.of(
            new ItemStack((ItemLike)ModItems.LIGHTNING_ITEM_CELL_HOUSING.get()), new ItemStack((ItemLike)ModItems.BULK_LIGHTNING_CELL_COMPONENT.get())
         );
         if (disassembledStacks.isEmpty()) {
            return false;
         } else {
            Inventory playerInventory = player.m_150109_();
            if (playerInventory.m_36056_() != stack) {
               return false;
            } else {
               BulkLightningStorageCellItem.StoredAmounts storedAmounts = readStoredAmounts(stack);
               if (storedAmounts.highVoltage() == 0L && storedAmounts.extremeHighVoltage() == 0L) {
                  playerInventory.m_6836_(playerInventory.f_35977_, ItemStack.f_41583_);

                  for (ItemStack disassembledStack : disassembledStacks) {
                     playerInventory.m_150079_(disassembledStack.m_41777_());
                  }

                  return true;
               } else {
                  player.m_5661_(PlayerMessages.OnlyEmptyCellsCanBeDisassembled.text(), true);
                  return false;
               }
            }
         }
      } else {
         return false;
      }
   }

   private static long sanitize(long amount) {
      return Math.max(0L, amount);
   }

   private static void putOrRemove(CompoundTag tag, String key, long amount) {
      if (amount == 0L) {
         tag.m_128473_(key);
      } else {
         tag.m_128356_(key, amount);
      }
   }

   public static record StoredAmounts(long highVoltage, long extremeHighVoltage) {
   }
}
