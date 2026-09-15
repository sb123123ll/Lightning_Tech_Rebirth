package com.moakiee.ae2lt.device.capability;

import com.moakiee.ae2lt.celestweave.ArmorPart;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;

public sealed interface DeviceCapability
   permits DeviceCapability.LastStandTuning,
   DeviceCapability.StagedMitigation,
   DeviceCapability.AccelerationFactor,
   DeviceCapability.EnergyCapacity,
   DeviceCapability.PassiveDrain,
   DeviceCapability.ActiveCost,
   DeviceCapability.LightningCompensation,
   DeviceCapability.ChainTuning,
   DeviceCapability.PulseTuning,
   DeviceCapability.RangeMultiplier,
   DeviceCapability.OverloadExecutionTuning,
   DeviceCapability.ReflectTuning,
   DeviceCapability.DamageTypeImmunity,
   DeviceCapability.PurificationTuning,
   DeviceCapability.DashEffect,
   DeviceCapability.FlightMode,
   DeviceCapability.ElytraFlight,
   DeviceCapability.PhaseTraversal,
   DeviceCapability.StatusEffectGrant,
   DeviceCapability.FoodSustain,
   DeviceCapability.DigAffinity,
   DeviceCapability.InteractionRange,
   DeviceCapability.MovementAssist,
   DeviceCapability.FallProtection,
   DeviceCapability.JumpBoost,
   DeviceCapability.Vision,
   DeviceCapability.EnergyEfficiency,
   DeviceCapability.ArmorPartTag,
   DeviceCapability.DeviceSpecific {
   public static record AccelerationFactor(double factor) implements DeviceCapability {
   }

   public static record ActiveCost(String trigger, long fe) implements DeviceCapability {
   }

   public static record ArmorPartTag(ArmorPart part) implements DeviceCapability {
   }

   public static record ChainTuning(int extraSegments, int extraForks, int hardCapBonus) implements DeviceCapability {
   }

   public static record DamageTypeImmunity(ResourceKey<DamageType> damageType) implements DeviceCapability {
   }

   public static record DashEffect(double impulse, int cooldownTicks) implements DeviceCapability {
   }

   public static record DeviceSpecific(String id, CompoundTag data) implements DeviceCapability {
   }

   public static record DigAffinity(String env, double speedMul) implements DeviceCapability {
   }

   public static record ElytraFlight() implements DeviceCapability {
   }

   public static record EnergyCapacity(long fe) implements DeviceCapability {
   }

   public static record EnergyEfficiency(double drainMul) implements DeviceCapability {
   }

   public static record FallProtection(double damageReduction) implements DeviceCapability {
   }

   public static record FlightMode(FlightKind kind) implements DeviceCapability {
   }

   public static record FoodSustain(int targetFood, float targetSaturation, int checkIntervalTicks) implements DeviceCapability {
   }

   public static record InteractionRange() implements DeviceCapability {
   }

   public static record JumpBoost(int amplifier) implements DeviceCapability {
   }

   public static record LastStandTuning(long feCost, int comboWindowTicks) implements DeviceCapability {
   }

   public static record LightningCompensation(int highVoltagePerExtremeHighVoltage) implements DeviceCapability {
      public LightningCompensation(int highVoltagePerExtremeHighVoltage) {
         highVoltagePerExtremeHighVoltage = Math.max(0, highVoltagePerExtremeHighVoltage);
         this.highVoltagePerExtremeHighVoltage = highVoltagePerExtremeHighVoltage;
      }
   }

   public static record MovementAssist() implements DeviceCapability {
   }

   public static record OverloadExecutionTuning(double decayRate, int decayDelayTicks, int maxTrackedTargets) implements DeviceCapability {
   }

   public static record PassiveDrain(long fePerTick) implements DeviceCapability {
   }

   public static record PhaseTraversal(long activeFePerTick) implements DeviceCapability {
      public PhaseTraversal(long activeFePerTick) {
         activeFePerTick = Math.max(0L, activeFePerTick);
         this.activeFePerTick = activeFePerTick;
      }
   }

   public static record PulseTuning(double radiusMul, double dmgMul) implements DeviceCapability {
   }

   public static record PurificationTuning(int periodTicks, int strength) implements DeviceCapability {
   }

   public static record RangeMultiplier(double factor) implements DeviceCapability {
      public RangeMultiplier(double factor) {
         if (!Double.isFinite(factor) || factor < 1.0) {
            factor = 1.0;
         }

         this.factor = factor;
      }
   }

   public static record ReflectTuning(double reflectPct, long fePerDamage) implements DeviceCapability {
   }

   public static record StagedMitigation(String stage) implements DeviceCapability {
   }

   public static record StatusEffectGrant(Holder<MobEffect> effect, int amplifier) implements DeviceCapability {
   }

   public static record Vision(String kind, int amplifier) implements DeviceCapability {
   }
}
