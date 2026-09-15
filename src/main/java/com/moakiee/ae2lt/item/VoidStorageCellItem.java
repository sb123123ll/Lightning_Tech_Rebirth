package com.moakiee.ae2lt.item;

import appeng.api.config.FuzzyMode;
import appeng.api.implementations.menuobjects.IMenuItem;
import appeng.api.implementations.menuobjects.ItemMenuHost;
import appeng.api.storage.cells.ICellWorkbenchItem;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.UpgradeInventories;
import appeng.items.AEBaseItem;
import appeng.items.contents.CellConfig;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import appeng.util.ConfigInventory;
import com.moakiee.ae2lt.me.cell.VoidCellData;
import com.moakiee.ae2lt.me.cell.VoidCellHandler;
import com.moakiee.ae2lt.me.cell.VoidCellInventory;
import com.moakiee.ae2lt.me.cell.VoidCellMode;
import com.moakiee.ae2lt.menu.VoidCellMenu;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class VoidStorageCellItem extends AEBaseItem implements ICellWorkbenchItem, IMenuItem {
   private static final String FUZZY_MODE_TAG = "FuzzyMode";

   public VoidStorageCellItem(Properties properties) {
      super(properties.m_41487_(1));
   }

   public void m_7373_(ItemStack stack, @Nullable Level level, List<Component> lines, TooltipFlag advanced) {
      super.m_7373_(stack, level, lines, advanced);
      VoidCellMode mode = VoidCellData.readMode(stack);
      lines.add(Component.m_237115_("gui.ae2lt.void_cell.mode." + mode.ordinal()).m_130940_(ChatFormatting.GREEN));
      if (!(VoidCellHandler.INSTANCE.getCellInventory(stack, null) instanceof VoidCellInventory voidInventory) || !voidInventory.isPartitioned()) {
         lines.add(Component.m_237115_("tooltip.ae2lt.void_cell.partition_required").m_130940_(ChatFormatting.RED));
      }
   }

   public Optional<TooltipComponent> m_142422_(ItemStack stack) {
      return VoidCellHandler.INSTANCE.getCellInventory(stack, null) instanceof VoidCellInventory voidInventory
         ? voidInventory.getTooltipImage()
         : Optional.empty();
   }

   public IUpgradeInventory getUpgrades(ItemStack stack) {
      return UpgradeInventories.forItem(stack, 2);
   }

   public ConfigInventory getConfigInventory(ItemStack stack) {
      return CellConfig.create(stack);
   }

   public FuzzyMode getFuzzyMode(ItemStack stack) {
      CompoundTag tag = stack.m_41783_();
      if (tag == null) {
         return FuzzyMode.IGNORE_ALL;
      } else {
         try {
            return FuzzyMode.valueOf(tag.m_128461_("FuzzyMode"));
         } catch (IllegalArgumentException var4) {
            return FuzzyMode.IGNORE_ALL;
         }
      }
   }

   public void setFuzzyMode(ItemStack stack, FuzzyMode mode) {
      stack.m_41784_().m_128359_("FuzzyMode", mode.name());
   }

   public InteractionResultHolder<ItemStack> m_7203_(Level level, Player player, InteractionHand hand) {
      if (!level.m_5776_()) {
         MenuOpener.open(VoidCellMenu.TYPE, player, MenuLocators.forHand(player, hand));
      }

      return new InteractionResultHolder(InteractionResult.m_19078_(level.m_5776_()), player.m_21120_(hand));
   }

   @Nullable
   public ItemMenuHost getMenuHost(Player player, int inventorySlot, ItemStack stack, @Nullable BlockPos pos) {
      return new ItemMenuHost(player, inventorySlot, stack);
   }
}
