package com.moakiee.ae2lt.item.railgun;

import appeng.api.implementations.menuobjects.IMenuItem;
import appeng.api.implementations.menuobjects.ItemMenuHost;
import com.moakiee.ae2lt.device.DeviceItem;
import com.moakiee.ae2lt.device.DeviceKind;
import com.moakiee.ae2lt.logic.railgun.RailgunEnergyBuffer;
import com.moakiee.ae2lt.logic.railgun.RailgunFireService;
import com.moakiee.ae2lt.menu.railgun.RailgunHost;
import com.moakiee.ae2lt.registry.ModDataComponents;
import com.moakiee.ae2lt.util.DeviceHubTooltip;
import com.moakiee.ae2lt.util.EnergyText;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class ElectromagneticRailgunItem extends Item implements IMenuItem, DeviceItem {
   private static final int USE_DURATION = 72000;

   public ElectromagneticRailgunItem(Properties properties) {
      super(properties);
   }

   @Override
   public DeviceKind deviceKind() {
      return DeviceKind.RAILGUN;
   }

   public InteractionResultHolder<ItemStack> m_7203_(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.m_21120_(hand);
      if (!RailgunStructuralCore.hasCore(stack)) {
         if (!level.f_46443_) {
            player.m_5661_(Component.m_237115_("ae2lt.railgun.structural_core_required"), true);
         }

         return InteractionResultHolder.m_19100_(stack);
      } else if (!hasOverloadCoreModule(stack)) {
         if (!level.f_46443_) {
            player.m_5661_(Component.m_237115_("ae2lt.railgun.core_required"), true);
         }

         return InteractionResultHolder.m_19100_(stack);
      } else {
         player.m_6672_(hand);
         if (!level.f_46443_) {
            ModDataComponents.RAILGUN_CHARGE_TICKS.set(stack, 0L);
         }

         return new InteractionResultHolder(InteractionResult.CONSUME, stack);
      }
   }

   public UseAnim m_6164_(ItemStack stack) {
      return UseAnim.NONE;
   }

   public int m_8105_(ItemStack stack) {
      return 72000;
   }

   public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
      return oldStack.m_41720_() != newStack.m_41720_() || slotChanged;
   }

   public void m_6883_(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
      super.m_6883_(stack, level, entity, slotId, isSelected);
      if (!level.f_46443_ && entity instanceof Player player) {
         boolean inHand = isSelected || player.m_21206_() == stack;
         if (inHand && player instanceof ServerPlayer serverPlayer) {
            refillFromBoundNetwork(stack, serverPlayer);
         }
      }
   }

   public void m_5929_(Level level, LivingEntity user, ItemStack stack, int remaining) {
      if (!level.f_46443_) {
         if (user instanceof ServerPlayer player) {
            if (!hasOverloadCoreModule(stack)) {
               ModDataComponents.RAILGUN_CHARGE_TICKS.remove(stack);
               player.m_5810_();
               player.m_5661_(Component.m_237115_("ae2lt.railgun.core_required"), true);
            } else {
               long current = ModDataComponents.RAILGUN_CHARGE_TICKS.getOrDefault(stack, 0L);
               RailgunChargeTier chargingTier = chargingCostTier(current);
               long chargeCost = RailgunEnergyRules.chargeCostPerTickFe(chargingTier);
               refillMissingFe(stack, player, chargeCost);
               if (!RailgunEnergyBuffer.tryConsume(stack, player, chargeCost)) {
                  ModDataComponents.RAILGUN_CHARGE_TICKS.remove(stack);
                  player.m_5810_();
                  player.m_5661_(Component.m_237115_("ae2lt.railgun.fail.no_fe"), true);
               } else {
                  RailgunModuleEntries mods = ModDataComponents.RAILGUN_MODULE_ENTRIES.getOrDefault(stack, RailgunModuleEntries.EMPTY);
                  long step = 1L + (long)RailgunFireService.countAccelerationModules(mods);
                  ModDataComponents.RAILGUN_CHARGE_TICKS.set(stack, current + step);
               }
            }
         }
      }
   }

   public void m_5551_(ItemStack stack, Level level, LivingEntity user, int timeLeft) {
      if (!level.f_46443_ && user instanceof ServerPlayer player && level instanceof ServerLevel sl) {
         long charged = ModDataComponents.RAILGUN_CHARGE_TICKS.getOrDefault(stack, 0L);
         ModDataComponents.RAILGUN_CHARGE_TICKS.remove(stack);
         RailgunModuleEntries mods = ModDataComponents.RAILGUN_MODULE_ENTRIES.getOrDefault(stack, RailgunModuleEntries.EMPTY);
         RailgunChargeTier tier = RailgunFireService.tierForCharge(charged, mods);
         if (tier == RailgunChargeTier.HV) {
            return;
         }

         RailgunFireService.fireCharged(sl, player, stack, tier, charged);
         return;
      }

      ModDataComponents.RAILGUN_CHARGE_TICKS.remove(stack);
   }

   @Nullable
   public ItemMenuHost getMenuHost(Player player, int inventorySlot, ItemStack stack, @Nullable BlockPos pos) {
      return new RailgunHost(player, inventorySlot, stack);
   }

   public void m_7373_(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag tooltipFlag) {
      super.m_7373_(stack, level, tooltip, tooltipFlag);
      long current = RailgunEnergyBuffer.read(stack);
      long capacity = RailgunEnergyBuffer.capacity(stack);
      tooltip.add(EnergyText.storedFe(current, capacity));
      tooltip.add(DeviceHubTooltip.openConfigHint());
   }

   public boolean m_142522_(ItemStack stack) {
      return true;
   }

   public int m_142158_(ItemStack stack) {
      long capacity = RailgunEnergyBuffer.capacity(stack);
      if (capacity <= 0L) {
         return 0;
      } else {
         double filled = (double)RailgunEnergyBuffer.read(stack) / (double)capacity;
         return Mth.m_14045_((int)Math.round(filled * 13.0), 0, 13);
      }
   }

   public int m_142159_(ItemStack stack) {
      return Mth.m_14169_(0.33333334F, 1.0F, 1.0F);
   }

   private static boolean hasOverloadCoreModule(ItemStack stack) {
      return ModDataComponents.RAILGUN_MODULE_ENTRIES.getOrDefault(stack, RailgunModuleEntries.EMPTY).hasCore();
   }

   private static RailgunChargeTier chargingCostTier(long current) {
      if (current >= 40L) {
         return RailgunChargeTier.EHV3;
      } else {
         return current >= 24L ? RailgunChargeTier.EHV2 : RailgunChargeTier.EHV1;
      }
   }

   private static void refillFromBoundNetwork(ItemStack stack, ServerPlayer player) {
      RailgunEnergyBuffer.refillFromNetwork(stack, player, Long.MAX_VALUE);
   }

   private static void refillMissingFe(ItemStack stack, ServerPlayer player, long required) {
      if (required > 0L) {
         RailgunEnergyBuffer.refillFromNetwork(stack, player, Math.max(0L, required - RailgunEnergyBuffer.read(stack)));
      }
   }
}
