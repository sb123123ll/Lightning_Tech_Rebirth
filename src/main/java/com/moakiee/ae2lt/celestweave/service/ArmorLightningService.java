package com.moakiee.ae2lt.celestweave.service;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import com.moakiee.ae2lt.device.energy.LightningCompensationPolicy;
import com.moakiee.ae2lt.device.network.ArmorNetworkBinding;
import com.moakiee.ae2lt.device.network.BindingResolveResult;
import com.moakiee.ae2lt.me.key.LightningKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class ArmorLightningService {
   private ArmorLightningService() {
   }

   public static boolean hasCost(ServerPlayer player, ItemStack armor, LightningKey key, long amount) {
      if (amount <= 0L) {
         return true;
      } else {
         return key == null
            ? false
            : hasCost(
               player,
               armor,
               key == LightningKey.EXTREME_HIGH_VOLTAGE ? ArmorLightningService.LightningCost.ehv(amount) : ArmorLightningService.LightningCost.hv(amount)
            );
      }
   }

   public static boolean hasCost(ServerPlayer player, ItemStack armor, ArmorLightningService.LightningCost cost) {
      return cost.isEmpty() ? true : plan(player, armor, cost).canPay();
   }

   public static boolean consume(ServerPlayer player, ItemStack armor, LightningKey key, long amount) {
      if (amount <= 0L) {
         return true;
      } else {
         return key == null
            ? false
            : consume(
               player,
               armor,
               key == LightningKey.EXTREME_HIGH_VOLTAGE ? ArmorLightningService.LightningCost.ehv(amount) : ArmorLightningService.LightningCost.hv(amount)
            );
      }
   }

   public static boolean consume(ServerPlayer player, ItemStack armor, ArmorLightningService.LightningCost cost) {
      if (cost.isEmpty()) {
         return true;
      } else {
         LightningCompensationPolicy.Plan plan = plan(player, armor, cost);
         if (!plan.canPay()) {
            return false;
         } else {
            long gotEhv = extract(player, armor, LightningKey.EXTREME_HIGH_VOLTAGE, plan.extremeHighVoltageToConsume(), Actionable.MODULATE);
            if (gotEhv < plan.extremeHighVoltageToConsume()) {
               refund(player, armor, LightningKey.EXTREME_HIGH_VOLTAGE, gotEhv);
               return false;
            } else {
               long gotHv = extract(player, armor, LightningKey.HIGH_VOLTAGE, plan.highVoltageToConsume(), Actionable.MODULATE);
               if (gotHv < plan.highVoltageToConsume()) {
                  refund(player, armor, LightningKey.HIGH_VOLTAGE, gotHv);
                  refund(player, armor, LightningKey.EXTREME_HIGH_VOLTAGE, gotEhv);
                  return false;
               } else {
                  return true;
               }
            }
         }
      }
   }

   private static LightningCompensationPolicy.Plan plan(ServerPlayer player, ItemStack armor, ArmorLightningService.LightningCost cost) {
      long availableEhv = cost.extremeHighVoltage() <= 0L ? 0L : extract(player, armor, LightningKey.EXTREME_HIGH_VOLTAGE, Long.MAX_VALUE, Actionable.SIMULATE);
      boolean needsCompensation = availableEhv < cost.extremeHighVoltage();
      int compensationRatio = needsCompensation && player != null
         ? LightningCompensationPolicy.bestRatio(
            ArmorCapabilityCollector.collectPerInstalledStack(player).stream().map(ArmorCapabilityCollector.ActiveCapability::capability).toList()
         )
         : 0;
      boolean needsHighVoltage = cost.highVoltage() > 0L || needsCompensation && compensationRatio > 0;
      long availableHv = needsHighVoltage ? extract(player, armor, LightningKey.HIGH_VOLTAGE, Long.MAX_VALUE, Actionable.SIMULATE) : 0L;
      return LightningCompensationPolicy.plan(cost.highVoltage(), cost.extremeHighVoltage(), availableHv, availableEhv, compensationRatio);
   }

   private static long extract(ServerPlayer player, ItemStack armor, LightningKey key, long amount, Actionable action) {
      if (player != null && armor != null && !armor.m_41619_() && key != null && amount > 0L) {
         BindingResolveResult bound = ArmorNetworkBinding.INSTANCE.resolve(armor, player);
         return bound.success() && bound.grid() != null
            ? bound.grid().getStorageService().getInventory().extract(key, amount, action, IActionSource.ofPlayer(player))
            : 0L;
      } else {
         return 0L;
      }
   }

   private static void refund(ServerPlayer player, ItemStack armor, LightningKey key, long amount) {
      if (player != null && armor != null && !armor.m_41619_() && key != null && amount > 0L) {
         BindingResolveResult bound = ArmorNetworkBinding.INSTANCE.resolve(armor, player);
         if (bound.success() && bound.grid() != null) {
            bound.grid().getStorageService().getInventory().insert(key, amount, Actionable.MODULATE, IActionSource.ofPlayer(player));
         }
      }
   }

   public static record LightningCost(long highVoltage, long extremeHighVoltage) {
      public static final ArmorLightningService.LightningCost NONE = new ArmorLightningService.LightningCost(0L, 0L);

      public LightningCost(long highVoltage, long extremeHighVoltage) {
         highVoltage = Math.max(0L, highVoltage);
         extremeHighVoltage = Math.max(0L, extremeHighVoltage);
         this.highVoltage = highVoltage;
         this.extremeHighVoltage = extremeHighVoltage;
      }

      public static ArmorLightningService.LightningCost hv(long amount) {
         return new ArmorLightningService.LightningCost(amount, 0L);
      }

      public static ArmorLightningService.LightningCost ehv(long amount) {
         return new ArmorLightningService.LightningCost(0L, amount);
      }

      public ArmorLightningService.LightningCost plus(ArmorLightningService.LightningCost other) {
         return other != null && !other.isEmpty()
            ? new ArmorLightningService.LightningCost(
               saturatingAdd(this.highVoltage, other.highVoltage), saturatingAdd(this.extremeHighVoltage, other.extremeHighVoltage)
            )
            : this;
      }

      public ArmorLightningService.LightningCost times(long multiplier) {
         return multiplier > 0L && !this.isEmpty()
            ? new ArmorLightningService.LightningCost(saturatingMultiply(this.highVoltage, multiplier), saturatingMultiply(this.extremeHighVoltage, multiplier))
            : NONE;
      }

      public boolean isEmpty() {
         return this.highVoltage <= 0L && this.extremeHighVoltage <= 0L;
      }

      private static long saturatingAdd(long left, long right) {
         return left > Long.MAX_VALUE - right ? Long.MAX_VALUE : left + right;
      }

      private static long saturatingMultiply(long value, long multiplier) {
         if (value <= 0L || multiplier <= 0L) {
            return 0L;
         } else {
            return value > Long.MAX_VALUE / multiplier ? Long.MAX_VALUE : value * multiplier;
         }
      }
   }
}
