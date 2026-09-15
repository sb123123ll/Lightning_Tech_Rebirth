package com.moakiee.ae2lt.item;

import com.moakiee.ae2lt.api.patternprovider.WirelessPatternProviderHost;
import com.moakiee.ae2lt.block.OverloadedInterfaceBlock;
import com.moakiee.ae2lt.block.OverloadedPowerSupplyBlock;
import com.moakiee.ae2lt.blockentity.OverloadedInterfaceBlockEntity;
import com.moakiee.ae2lt.blockentity.OverloadedPowerSupplyBlockEntity;
import com.moakiee.ae2lt.network.NetworkInit;
import com.moakiee.ae2lt.network.WirelessConnectorUsePacket;
import com.moakiee.ae2lt.util.ItemStackTagSupport;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class OverloadedWirelessConnectorItem extends AE2LTItem {
   private static final String TAG_SELECTED = "SelectedProvider";
   private static final String TAG_DIM = "Dim";
   private static final String TAG_POS = "Pos";
   private static final String TAG_HOST_TYPE = "HostType";
   public static final String HOST_PROVIDER = "provider";
   public static final String HOST_INTERFACE = "interface";
   public static final String HOST_POWER_SUPPLY = "power_supply";

   public OverloadedWirelessConnectorItem(Properties properties) {
      super(properties.m_41487_(1));
   }

   public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
      return this.handleBlockUse(context);
   }

   public InteractionResult m_6225_(UseOnContext context) {
      return this.handleBlockUse(context);
   }

   private InteractionResult handleBlockUse(UseOnContext context) {
      Player player = context.m_43723_();
      if (player == null) {
         return InteractionResult.PASS;
      } else {
         Level level = context.m_43725_();
         BlockPos pos = context.m_8083_();
         BlockState state = level.m_8055_(pos);
         BlockEntity targetBe = level.m_7702_(pos);
         boolean isHost = targetBe instanceof WirelessPatternProviderHost
            || state.m_60734_() instanceof OverloadedInterfaceBlock
            || state.m_60734_() instanceof OverloadedPowerSupplyBlock;
         boolean isMachine = targetBe != null;
         if (!isHost && !isMachine) {
            return InteractionResult.PASS;
         } else if (level.m_5776_()) {
            NetworkInit.sendToServer(new WirelessConnectorUsePacket(context.m_43724_(), pos, context.m_43719_(), Screen.m_96637_()));
            return InteractionResult.m_19078_(true);
         } else {
            return InteractionResult.SUCCESS;
         }
      }
   }

   public InteractionResultHolder<ItemStack> m_7203_(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.m_21120_(hand);
      if (level.m_5776_()) {
         return InteractionResultHolder.m_19098_(stack);
      } else if (hasSelection(stack)) {
         String hostType = getSelectedHostType(stack);
         clearSelection(stack);
         player.m_5661_(Component.m_237115_(getDeselectedTranslationKey(hostType)).m_130940_(ChatFormatting.GREEN), true);
         return InteractionResultHolder.m_19090_(stack);
      } else {
         return InteractionResultHolder.m_19098_(stack);
      }
   }

   public static void selectHost(ItemStack stack, Level level, BlockPos pos, String hostType) {
      ItemStackTagSupport.updateTag(stack, tag -> {
         CompoundTag sel = new CompoundTag();
         sel.m_128359_("Dim", level.m_46472_().m_135782_().toString());
         sel.m_128356_("Pos", pos.m_121878_());
         sel.m_128359_("HostType", hostType);
         tag.m_128365_("SelectedProvider", sel);
      });
   }

   public static boolean hasSelection(ItemStack stack) {
      CompoundTag tag = ItemStackTagSupport.getTagCopy(stack);
      return tag.m_128425_("SelectedProvider", 10);
   }

   @Nullable
   public static String getSelectedHostType(ItemStack stack) {
      CompoundTag tag = ItemStackTagSupport.getTagCopy(stack);
      if (!tag.m_128441_("SelectedProvider")) {
         return null;
      } else {
         CompoundTag sel = tag.m_128469_("SelectedProvider");
         return sel.m_128441_("HostType") ? sel.m_128461_("HostType") : "provider";
      }
   }

   public static boolean isSelectionInCurrentDimension(Level level, ItemStack stack) {
      CompoundTag tag = ItemStackTagSupport.getTagCopy(stack);
      if (!tag.m_128441_("SelectedProvider")) {
         return true;
      } else {
         CompoundTag sel = tag.m_128469_("SelectedProvider");
         return !sel.m_128441_("Dim") ? true : level.m_46472_().m_135782_().equals(ResourceLocation.m_135820_(sel.m_128461_("Dim")));
      }
   }

   public static void clearSelection(ItemStack stack) {
      CompoundTag tag = ItemStackTagSupport.getTagCopy(stack);
      tag.m_128473_("SelectedProvider");
      if (tag.m_128456_()) {
         stack.m_41751_(null);
      } else {
         ItemStackTagSupport.setTag(stack, tag);
      }
   }

   private static String getDeselectedTranslationKey(@Nullable String hostType) {
      if ("interface".equals(hostType)) {
         return "ae2lt.connector.deselected_interface";
      } else {
         return "power_supply".equals(hostType) ? "ae2lt.connector.deselected_power_supply" : "ae2lt.connector.deselected";
      }
   }

   @Nullable
   private static BlockEntity resolveSelectedHost(Level level, ItemStack stack) {
      CompoundTag tag = ItemStackTagSupport.getTagCopy(stack);
      if (!tag.m_128441_("SelectedProvider")) {
         return null;
      } else {
         CompoundTag sel = tag.m_128469_("SelectedProvider");
         ResourceKey<Level> dimKey = ResourceKey.m_135785_(Registries.f_256858_, ResourceLocation.m_135820_(sel.m_128461_("Dim")));
         BlockPos pos = BlockPos.m_122022_(sel.m_128454_("Pos"));
         return level.m_46472_().equals(dimKey) && level.m_46749_(pos) ? level.m_7702_(pos) : null;
      }
   }

   @Nullable
   public static WirelessPatternProviderHost getSelectedProvider(Level level, ItemStack stack) {
      return resolveSelectedHost(level, stack) instanceof WirelessPatternProviderHost provider ? provider : null;
   }

   @Nullable
   public static OverloadedInterfaceBlockEntity getSelectedInterface(Level level, ItemStack stack) {
      return resolveSelectedHost(level, stack) instanceof OverloadedInterfaceBlockEntity iface ? iface : null;
   }

   @Nullable
   public static OverloadedPowerSupplyBlockEntity getSelectedPowerSupply(Level level, ItemStack stack) {
      return resolveSelectedHost(level, stack) instanceof OverloadedPowerSupplyBlockEntity powerSupply ? powerSupply : null;
   }
}
