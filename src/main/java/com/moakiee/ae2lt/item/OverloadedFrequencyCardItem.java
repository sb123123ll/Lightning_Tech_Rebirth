package com.moakiee.ae2lt.item;

import appeng.items.materials.UpgradeCardItem;
import com.moakiee.ae2lt.client.FrequencyCardClientNames;
import com.moakiee.ae2lt.grid.FrequencyDisplayName;
import com.moakiee.ae2lt.network.FrequencyCardUsePacket;
import com.moakiee.ae2lt.network.NetworkInit;
import com.moakiee.ae2lt.util.ItemStackTagSupport;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.jetbrains.annotations.Nullable;

public class OverloadedFrequencyCardItem extends UpgradeCardItem {
   private static final String TAG_FREQUENCY_ID = "FrequencyId";
   private static final String TAG_AUTO_CONNECT = "AutoConnect";
   private static final String TAG_BOUND_CONTROLLER_DIM = "BoundControllerDim";
   private static final String TAG_BOUND_CONTROLLER_POS = "BoundControllerPos";
   private static final String TAG_OWNER_UUID = "OwnerUuid";

   public OverloadedFrequencyCardItem(Properties properties) {
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
         if (level.m_5776_()) {
            Vec3 hit = context.m_43720_();
            NetworkInit.sendToServer(
               new FrequencyCardUsePacket(
                  context.m_43724_(), context.m_8083_(), context.m_43719_(), hit.f_82479_, hit.f_82480_, hit.f_82481_, Screen.m_96638_()
               )
            );
            return InteractionResult.SUCCESS;
         } else {
            return InteractionResult.SUCCESS;
         }
      }
   }

   public InteractionResultHolder<ItemStack> m_7203_(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.m_21120_(hand);
      if (!player.m_6144_()) {
         return InteractionResultHolder.m_19098_(stack);
      } else {
         if (!level.m_5776_()) {
            OverloadedFrequencyCardData data = getData(stack);
            if (data.isBound() && !data.canBeUsedBy(player.m_20148_())) {
               player.m_5661_(Component.m_237115_("ae2lt.frequency_card.card_owner_mismatch").m_130940_(ChatFormatting.RED), true);
               return InteractionResultHolder.m_19100_(stack);
            }

            setData(stack, data.clearFrequency());
            player.m_5661_(Component.m_237115_("ae2lt.frequency_card.cleared").m_130940_(ChatFormatting.GREEN), true);
         }

         return InteractionResultHolder.m_19092_(stack, level.m_5776_());
      }
   }

   public void m_7373_(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag tooltipFlag) {
      OverloadedFrequencyCardData data = getData(stack);
      if (data.isBound()) {
         tooltip.add(
            Component.m_237110_("tooltip.ae2lt.frequency_card.frequency", new Object[]{tooltipFrequencyName(data.frequencyId())})
               .m_130940_(ChatFormatting.AQUA)
         );
      } else {
         tooltip.add(Component.m_237115_("tooltip.ae2lt.frequency_card.unbound").m_130940_(ChatFormatting.GRAY));
      }

      tooltip.add(
         Component.m_237115_(data.autoConnect() ? "tooltip.ae2lt.frequency_card.auto_on" : "tooltip.ae2lt.frequency_card.auto_off")
            .m_130940_(data.autoConnect() ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY)
      );
      if (data.isBound()) {
         tooltip.add(Component.m_237115_("tooltip.ae2lt.frequency_card.bound_hint").m_130940_(ChatFormatting.GRAY));
      } else {
         tooltip.add(Component.m_237115_("tooltip.ae2lt.frequency_card.bind_hint").m_130940_(ChatFormatting.GRAY));
      }

      tooltip.add(Component.m_237115_("tooltip.ae2lt.frequency_card.controls").m_130940_(ChatFormatting.DARK_GRAY));
      super.m_7373_(stack, level, tooltip, tooltipFlag);
   }

   public static OverloadedFrequencyCardData getData(ItemStack stack) {
      CompoundTag tag = stack.m_41783_();
      return fromTag(tag == null ? new CompoundTag() : tag);
   }

   public static void setData(ItemStack stack, OverloadedFrequencyCardData data) {
      ItemStackTagSupport.setTag(stack, toTag(data));
   }

   public static void bindFrequency(ItemStack stack, int frequencyId, ResourceKey<Level> dimension, BlockPos pos, UUID ownerUuid) {
      setData(stack, getData(stack).bindFrequency(frequencyId, dimension.m_135782_().toString(), pos.m_121878_(), ownerUuid));
   }

   public static boolean toggleAutoConnect(ItemStack stack) {
      OverloadedFrequencyCardData data = getData(stack).toggleAutoConnect();
      setData(stack, data);
      return data.autoConnect();
   }

   public static Optional<ItemStack> findAutoConnectCard(Player player) {
      return selectAutoConnectCard(player).selected();
   }

   private static String tooltipFrequencyName(int frequencyId) {
      String frequencyName = FMLEnvironment.dist == Dist.CLIENT ? FrequencyCardClientNames.frequencyName(frequencyId) : null;
      return FrequencyDisplayName.of(frequencyId, frequencyName);
   }

   public static boolean hasMultipleAutoConnectCandidates(Player player) {
      return selectAutoConnectCard(player).ambiguous();
   }

   public static FrequencyCardCandidateSelector.Selection<ItemStack> selectToggleCard(Player player) {
      return selectCard(player, false);
   }

   private static FrequencyCardCandidateSelector.Selection<ItemStack> selectAutoConnectCard(Player player) {
      return selectCard(player, true);
   }

   private static FrequencyCardCandidateSelector.Selection<ItemStack> selectCard(Player player, boolean requireAutoConnect) {
      UUID playerUuid = player.m_20148_();
      ArrayList<FrequencyCardCandidateSelector.Candidate<ItemStack>> candidates = new ArrayList<>();
      ItemStack main = player.m_21205_();
      addCandidate(candidates, FrequencyCardCandidateSelector.Source.MAIN_HAND, main, playerUuid, requireAutoConnect);
      ItemStack offhand = player.m_21206_();
      addCandidate(candidates, FrequencyCardCandidateSelector.Source.OFF_HAND, offhand, playerUuid, requireAutoConnect);

      for (ItemStack stack : CuriosFrequencyCardFinder.findFrequencyCards(player)) {
         addCandidate(candidates, FrequencyCardCandidateSelector.Source.CURIOS, stack, playerUuid, requireAutoConnect);
      }

      Inventory inventory = player.m_150109_();

      for (int slot = 0; slot < 9 && slot < inventory.f_35974_.size(); slot++) {
         ItemStack stack = (ItemStack)inventory.f_35974_.get(slot);
         addCandidate(candidates, FrequencyCardCandidateSelector.Source.HOTBAR, stack, playerUuid, requireAutoConnect);
      }

      for (int slot = 9; slot < inventory.f_35974_.size(); slot++) {
         ItemStack stack = (ItemStack)inventory.f_35974_.get(slot);
         addCandidate(candidates, FrequencyCardCandidateSelector.Source.BACKPACK, stack, playerUuid, requireAutoConnect);
      }

      if (requireAutoConnect) {
         for (ItemStack stack : TerminalFrequencyCardFinder.findFrequencyCards(player)) {
            addCandidate(candidates, FrequencyCardCandidateSelector.Source.WIRELESS_TERMINAL, stack, playerUuid, requireAutoConnect);
         }
      }

      return FrequencyCardCandidateSelector.select(candidates);
   }

   private static void addCandidate(
      List<FrequencyCardCandidateSelector.Candidate<ItemStack>> candidates,
      FrequencyCardCandidateSelector.Source source,
      ItemStack stack,
      UUID playerUuid,
      boolean requireAutoConnect
   ) {
      if (requireAutoConnect ? isUsableAutoCard(stack, playerUuid) : isToggleCandidate(stack, playerUuid)) {
         candidates.add(new FrequencyCardCandidateSelector.Candidate<>(source, stack));
      }
   }

   private static boolean isUsableAutoCard(ItemStack stack, UUID playerUuid) {
      if (!(stack.m_41720_() instanceof OverloadedFrequencyCardItem)) {
         return false;
      } else {
         OverloadedFrequencyCardData data = getData(stack);
         return data.isBound() && data.autoConnect() && data.canBeUsedBy(playerUuid);
      }
   }

   private static boolean isToggleCandidate(ItemStack stack, UUID playerUuid) {
      if (!(stack.m_41720_() instanceof OverloadedFrequencyCardItem)) {
         return false;
      } else {
         OverloadedFrequencyCardData data = getData(stack);
         return !data.isBound() || data.canBeUsedBy(playerUuid);
      }
   }

   private static OverloadedFrequencyCardData fromTag(CompoundTag tag) {
      int frequencyId = tag.m_128441_("FrequencyId") ? tag.m_128451_("FrequencyId") : -1;
      boolean autoConnect = tag.m_128471_("AutoConnect");
      Optional<String> dim = tag.m_128441_("BoundControllerDim") ? Optional.of(tag.m_128461_("BoundControllerDim")) : Optional.empty();
      Optional<Long> pos = tag.m_128441_("BoundControllerPos") ? Optional.of(tag.m_128454_("BoundControllerPos")) : Optional.empty();
      Optional<UUID> owner = Optional.empty();
      if (tag.m_128441_("OwnerUuid")) {
         try {
            owner = Optional.of(UUID.fromString(tag.m_128461_("OwnerUuid")));
         } catch (IllegalArgumentException var7) {
            owner = Optional.empty();
         }
      }

      return new OverloadedFrequencyCardData(frequencyId, autoConnect, dim, pos, owner);
   }

   private static CompoundTag toTag(OverloadedFrequencyCardData data) {
      CompoundTag tag = new CompoundTag();
      if (data.isBound()) {
         tag.m_128405_("FrequencyId", data.frequencyId());
      }

      if (data.autoConnect()) {
         tag.m_128379_("AutoConnect", true);
      }

      data.boundControllerDimension().ifPresent(value -> tag.m_128359_("BoundControllerDim", value));
      data.boundControllerPos().ifPresent(value -> tag.m_128356_("BoundControllerPos", value));
      data.ownerUuid().ifPresent(value -> tag.m_128359_("OwnerUuid", value.toString()));
      return tag;
   }
}
