package com.moakiee.ae2lt.config;

import com.moakiee.ae2lt.blockentity.ExtendedPatternProviderCapacity;
import com.moakiee.thunderbolt.core.util.FastWildcardMatcher;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.Builder;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.common.ForgeConfigSpec.DoubleValue;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;
import net.minecraftforge.common.ForgeConfigSpec.LongValue;

public final class AE2LTCommonConfig {
   public static final int CURRENT_CONFIG_VERSION = 3;
   private static final List<String> DEFAULT_EASTER_EGG_WEIGHTS = List.of(
      "avaritia:infinity_ingot=1200",
      "mekanism_extras:qio_drive_singularity=1200",
      "modern_industrialization:quantum_upgrade=1200",
      "bigreactors:inanite_block=776",
      "draconicevolution:chaotic_core=760",
      "occultism:celestial_chalice=744",
      "appflux:core_256m=460",
      "advanced_ae:data_entangler=455",
      "advanced_ae:quantum_multi_threader=445",
      "megacells:cell_component_256m=440",
      "ae2omnicells:quantum_omni_cell_component_256m=435",
      "mekanism_extras:infinite_induction_cell=415",
      "mekanism_extras:infinite_induction_provider=415",
      "mekanism:pellet_antimatter=252",
      "mekanism_extras:infinite_control_circuit=240",
      "ars_nouveau:wilden_tribute=204",
      "minecraft:elytra=138",
      "minecraft:heavy_core=132",
      "minecraft:dragon_head=114",
      "ae2:256k_crafting_storage=98",
      "mekanism:ultimate_induction_cell=76",
      "mekanism:ultimate_induction_provider=76",
      "pneumaticcraft:micromissiles=72",
      "minecraft:dragon_egg=54",
      "minecraft:heart_of_the_sea=40",
      "minecraft:echo_shard=36",
      "minecraft:nether_star=32",
      "minecraft:torchflower=8",
      "minecraft:recovery_compass=7",
      "minecraft:slime_ball=4"
   );
   private static final Set<ResourceLocation> DEFAULT_EASTER_EGG_CANDIDATE_IDS = Set.copyOf(
      DEFAULT_EASTER_EGG_WEIGHTS.stream().map(entry -> ResourceLocation.m_135820_(entry.substring(0, entry.lastIndexOf(61)))).filter(Objects::nonNull).toList()
   );
   private static final List<String> DEFAULT_BATCH_COPY_LIMITED_BLOCKS = List.of("neoecoae:crafting_pattern_bus", "sophisticated*:*");
   public static final ForgeConfigSpec SPEC;
   private static final AE2LTCommonConfig.Values VALUES;

   private AE2LTCommonConfig() {
   }

   public static int lightningCollectorCooldownTicks() {
      return (Integer)VALUES.lightningCollectorCooldownTicks.get();
   }

   public static int electroChimeMaxCatalysis() {
      return (Integer)VALUES.electroChimeMaxCatalysis.get();
   }

   public static boolean overloadTntEnableTerrainDamage() {
      return (Boolean)VALUES.overloadTntEnableTerrainDamage.get();
   }

   public static boolean easterEggEnabled() {
      return (Boolean)VALUES.easterEggEnabled.get();
   }

   public static String easterEggItem() {
      return (String)VALUES.easterEggItem.get();
   }

   public static int easterEggWeight() {
      return (Integer)VALUES.easterEggWeight.get();
   }

   public static Map<ResourceLocation, Integer> easterEggWeights() {
      Map<ResourceLocation, Integer> parsed = new LinkedHashMap<>();

      for (String entry : (List)VALUES.easterEggWeights.get()) {
         int separator = entry.lastIndexOf(61);
         if (separator > 0 && separator != entry.length() - 1) {
            ResourceLocation id = ResourceLocation.m_135820_(entry.substring(0, separator).strip());
            if (id != null) {
               try {
                  parsed.put(id, Integer.parseInt(entry.substring(separator + 1).strip()));
               } catch (NumberFormatException var6) {
               }
            }
         }
      }

      return Map.copyOf(parsed);
   }

   public static boolean isDefaultEasterEggCandidate(ResourceLocation id) {
      return DEFAULT_EASTER_EGG_CANDIDATE_IDS.contains(id);
   }

   public static int overloadTntGlobalBlockBudgetPerTick() {
      return (Integer)VALUES.overloadTntGlobalBlockBudgetPerTick.get();
   }

   public static int overloadTntGlobalLightningBudgetPerTick() {
      return (Integer)VALUES.overloadTntGlobalLightningBudgetPerTick.get();
   }

   public static boolean shulkerBulletCollectionEnabled() {
      return (Boolean)VALUES.shulkerBulletCollectionEnabled.get();
   }

   public static double floatingMatterRiseSpeed() {
      return (Double)VALUES.floatingMatterRiseSpeed.get();
   }

   public static double floatingMatterDespawnHeightMultiplier() {
      return (Double)VALUES.floatingMatterDespawnHeightMultiplier.get();
   }

   public static int overloadedControllerChannelsPerController() {
      return (Integer)VALUES.overloadedControllerChannelsPerController.get();
   }

   public static double overloadedControllerPassiveAePerTick() {
      return (Double)VALUES.overloadedControllerPassiveAePerTick.get();
   }

   public static int wirelessConnectorMaxDistance() {
      return (Integer)VALUES.wirelessConnectorMaxDistance.get();
   }

   public static int extendedPatternProviderPages() {
      return ExtendedPatternProviderCapacity.clampPages((Integer)VALUES.extendedPatternProviderPages.get());
   }

   public static List<? extends String> batchCopyLimitedBlocks() {
      return (List<? extends String>)VALUES.batchCopyLimitedBlocks.get();
   }

   public static int overloadFactoryParallelPerMatrix() {
      return (Integer)VALUES.overloadFactoryParallelPerMatrix.get();
   }

   public static long overloadFactoryEnergyCapacity() {
      return (Long)VALUES.overloadFactoryEnergyCapacity.get();
   }

   public static long overloadFactoryFePerTickNoSpeedCard() {
      return (Long)VALUES.overloadFactoryFePerTickNoSpeedCard.get();
   }

   public static long overloadFactoryFePerTickOneSpeedCard() {
      return (Long)VALUES.overloadFactoryFePerTickOneSpeedCard.get();
   }

   public static long overloadFactoryFePerTickTwoSpeedCards() {
      return (Long)VALUES.overloadFactoryFePerTickTwoSpeedCards.get();
   }

   public static long overloadFactoryFePerTickThreeSpeedCards() {
      return (Long)VALUES.overloadFactoryFePerTickThreeSpeedCards.get();
   }

   public static long overloadFactoryFePerTickFourSpeedCards() {
      return (Long)VALUES.overloadFactoryFePerTickFourSpeedCards.get();
   }

   public static boolean artificialLightningTriggerFromHotbar() {
      return (Boolean)VALUES.artificialLightningTriggerFromHotbar.get();
   }

   public static boolean artificialLightningTriggerFromBackpack() {
      return (Boolean)VALUES.artificialLightningTriggerFromBackpack.get();
   }

   public static int lightningCollectorHvBaseMin() {
      return (Integer)VALUES.lightningCollectorHvBaseMin.get();
   }

   public static int lightningCollectorHvBaseMax() {
      return (Integer)VALUES.lightningCollectorHvBaseMax.get();
   }

   public static int lightningCollectorEhvBaseMin() {
      return (Integer)VALUES.lightningCollectorEhvBaseMin.get();
   }

   public static int lightningCollectorEhvBaseMax() {
      return (Integer)VALUES.lightningCollectorEhvBaseMax.get();
   }

   public static int lightningCollectorHvCrystalStart() {
      return (Integer)VALUES.lightningCollectorHvCrystalStart.get();
   }

   public static int lightningCollectorHvCrystalEnd() {
      return (Integer)VALUES.lightningCollectorHvCrystalEnd.get();
   }

   public static int lightningCollectorEhvCrystalStart() {
      return (Integer)VALUES.lightningCollectorEhvCrystalStart.get();
   }

   public static int lightningCollectorEhvCrystalEnd() {
      return (Integer)VALUES.lightningCollectorEhvCrystalEnd.get();
   }

   public static int lightningCollectorPerfectHvOutput() {
      return (Integer)VALUES.lightningCollectorPerfectHvOutput.get();
   }

   public static int lightningCollectorPerfectEhvOutput() {
      return (Integer)VALUES.lightningCollectorPerfectEhvOutput.get();
   }

   public static int electroChimeCatalysisPerStrikeMin() {
      return (Integer)VALUES.electroChimeCatalysisPerStrikeMin.get();
   }

   public static int electroChimeCatalysisPerStrikeMax() {
      return (Integer)VALUES.electroChimeCatalysisPerStrikeMax.get();
   }

   public static double lightningCollectorSpreadRatio() {
      return (Double)VALUES.lightningCollectorSpreadRatio.get();
   }

   public static int teslaCoilHighVoltageDustCost() {
      return (Integer)VALUES.teslaCoilHighVoltageDustCost.get();
   }

   public static int teslaCoilHighVoltageFe() {
      return (Integer)VALUES.teslaCoilHighVoltageFe.get();
   }

   public static int teslaCoilExtremeHighVoltageInput() {
      return (Integer)VALUES.teslaCoilExtremeHighVoltageInput.get();
   }

   public static int teslaCoilExtremeHighVoltageFe() {
      return (Integer)VALUES.teslaCoilExtremeHighVoltageFe.get();
   }

   public static boolean pigmeeFumoGiftOnFirstJoin() {
      return (Boolean)VALUES.pigmeeFumoGiftOnFirstJoin.get();
   }

   public static int overloadArmorPurificationPeriodTicks() {
      return (Integer)VALUES.overloadArmorPurificationPeriodTicks.get();
   }

   public static boolean overloadArmorPurificationBeneficialEffects() {
      return (Boolean)VALUES.overloadArmorPurificationBeneficialEffects.get();
   }

   public static boolean overloadArmorPurificationNeutralEffects() {
      return (Boolean)VALUES.overloadArmorPurificationNeutralEffects.get();
   }

   public static boolean overloadArmorPurificationHarmfulEffects() {
      return (Boolean)VALUES.overloadArmorPurificationHarmfulEffects.get();
   }

   public static int overloadArmorSaturationCheckIntervalTicks() {
      return (Integer)VALUES.overloadArmorSaturationCheckIntervalTicks.get();
   }

   public static double overloadArmorUnderwaterDigMultiplier() {
      return (Double)VALUES.overloadArmorUnderwaterDigMultiplier.get();
   }

   public static double overloadArmorAirborneDigMultiplier() {
      return (Double)VALUES.overloadArmorAirborneDigMultiplier.get();
   }

   public static boolean overloadArmorPhaseFlightEnabled() {
      return (Boolean)VALUES.overloadArmorPhaseFlightEnabled.get();
   }

   public static PhaseLockTeleportMode overloadArmorPhaseLockTeleportMode() {
      return PhaseLockTeleportMode.fromConfigValue((String)VALUES.overloadArmorPhaseLockTeleportMode.get());
   }

   public static double overloadArmorPhaseFlightSpeedMultiplier() {
      return (Double)VALUES.overloadArmorPhaseFlightSpeedMultiplier.get();
   }

   public static long overloadArmorPassiveHvPerTick() {
      return (Long)VALUES.overloadArmorPassiveHvPerTick.get();
   }

   public static long overloadArmorFlightHvPerTick() {
      return (Long)VALUES.overloadArmorFlightHvPerTick.get();
   }

   public static long overloadArmorPhaseFlightHvPerTick() {
      return (Long)VALUES.overloadArmorPhaseFlightHvPerTick.get();
   }

   public static int overloadArmorShieldComboWindowTicks() {
      return (Integer)VALUES.overloadArmorShieldComboWindowTicks.get();
   }

   public static int overloadArmorUndyingComboWindowTicks() {
      return (Integer)VALUES.overloadArmorUndyingComboWindowTicks.get();
   }

   public static int railgunBeamDamagePerSettle() {
      return (Integer)VALUES.railgunBeamDamagePerSettle.get();
   }

   public static double railgunBeamBypass() {
      return (Double)VALUES.railgunBeamBypass.get();
   }

   public static int railgunBaseDamageEhv1() {
      return (Integer)VALUES.railgunBaseDamageEhv1.get();
   }

   public static int railgunBaseDamageEhv2() {
      return (Integer)VALUES.railgunBaseDamageEhv2.get();
   }

   public static int railgunBaseDamageEhv3() {
      return (Integer)VALUES.railgunBaseDamageEhv3.get();
   }

   public static double railgunChargedBypass() {
      return (Double)VALUES.railgunChargedBypass.get();
   }

   public static long railgunBeamFeCostPerSettle() {
      return (Long)VALUES.railgunBeamFeCostPerSettle.get();
   }

   public static long railgunFeCostTier1() {
      return (Long)VALUES.railgunFeCostTier1.get();
   }

   public static long railgunFeCostTier2() {
      return (Long)VALUES.railgunFeCostTier2.get();
   }

   public static long railgunFeCostTier3() {
      return (Long)VALUES.railgunFeCostTier3.get();
   }

   public static int railgunBeamHvCostInterval() {
      return (Integer)VALUES.railgunBeamHvCostInterval.get();
   }

   public static long railgunEhvCostTier1() {
      return (Long)VALUES.railgunEhvCostTier1.get();
   }

   public static long railgunEhvCostTier2() {
      return (Long)VALUES.railgunEhvCostTier2.get();
   }

   public static long railgunEhvCostTier3() {
      return (Long)VALUES.railgunEhvCostTier3.get();
   }

   public static long railgunBufferCapacity() {
      return (Long)VALUES.railgunBufferCapacity.get();
   }

   public static boolean railgunDamagePlayers() {
      return (Boolean)VALUES.railgunDamagePlayers.get();
   }

   public static boolean railgunParalysisOnPlayers() {
      return (Boolean)VALUES.railgunParalysisOnPlayers.get();
   }

   public static boolean railgunTerrainDestructionEnabled() {
      return (Boolean)VALUES.railgunTerrainDestructionEnabled.get();
   }

   public static boolean railgunTerrainDropItems() {
      return (Boolean)VALUES.railgunTerrainDropItems.get();
   }

   public static int railgunTerrainBlocksPerTick() {
      return (Integer)VALUES.railgunTerrainBlocksPerTick.get();
   }

   public static boolean overloadExecutionEnabled() {
      return (Boolean)VALUES.overloadExecutionEnabled.get();
   }

   public static int overloadExecutionDecayWindowTicks() {
      return (Integer)VALUES.overloadExecutionDecayWindowTicks.get();
   }

   public static double overloadExecutionDecayPower() {
      return (Double)VALUES.overloadExecutionDecayPower.get();
   }

   public static int overloadExecutionMaxTracked() {
      return (Integer)VALUES.overloadExecutionMaxTracked.get();
   }

   public static boolean frequencyCardEnableAutoCleanup() {
      return (Boolean)VALUES.frequencyCardEnableAutoCleanup.get();
   }

   public static int frequencyCardCleanupIntervalSeconds() {
      return (Integer)VALUES.frequencyCardCleanupIntervalSeconds.get();
   }

   public static int frequencyCardInvalidCleanupDelaySeconds() {
      return (Integer)VALUES.frequencyCardInvalidCleanupDelaySeconds.get();
   }

   public static int frequencyCardInvalidCleanupRequiredChecks() {
      return (Integer)VALUES.frequencyCardInvalidCleanupRequiredChecks.get();
   }

   public static int frequencyCardCleanupBatchSize() {
      return (Integer)VALUES.frequencyCardCleanupBatchSize.get();
   }

   private static boolean isEasterEggWeightEntry(Object value) {
      if (!(value instanceof String text)) {
         return false;
      } else {
         int separator = text.lastIndexOf(61);
         if (separator > 0 && separator != text.length() - 1 && ResourceLocation.m_135820_(text.substring(0, separator).strip()) != null) {
            try {
               int weight = Integer.parseInt(text.substring(separator + 1).strip());
               return weight >= 1 && weight <= 1000000;
            } catch (NumberFormatException var4) {
               return false;
            }
         } else {
            return false;
         }
      }
   }

   static {
      Builder builder = new Builder();
      VALUES = new AE2LTCommonConfig.Values(builder);
      SPEC = builder.build();
   }

   private static final class Values {
      private final IntValue configVersion;
      private final IntValue lightningCollectorCooldownTicks;
      private final IntValue electroChimeMaxCatalysis;
      private final BooleanValue overloadTntEnableTerrainDamage;
      private final BooleanValue easterEggEnabled;
      private final ConfigValue<String> easterEggItem;
      private final IntValue easterEggWeight;
      private final ConfigValue<List<? extends String>> easterEggWeights;
      private final IntValue overloadTntGlobalBlockBudgetPerTick;
      private final IntValue overloadTntGlobalLightningBudgetPerTick;
      private final BooleanValue shulkerBulletCollectionEnabled;
      private final DoubleValue floatingMatterRiseSpeed;
      private final DoubleValue floatingMatterDespawnHeightMultiplier;
      private final IntValue overloadedControllerChannelsPerController;
      private final DoubleValue overloadedControllerPassiveAePerTick;
      private final IntValue wirelessConnectorMaxDistance;
      private final IntValue extendedPatternProviderPages;
      private final ConfigValue<List<? extends String>> batchCopyLimitedBlocks;
      private final IntValue overloadFactoryParallelPerMatrix;
      private final LongValue overloadFactoryEnergyCapacity;
      private final LongValue overloadFactoryFePerTickNoSpeedCard;
      private final LongValue overloadFactoryFePerTickOneSpeedCard;
      private final LongValue overloadFactoryFePerTickTwoSpeedCards;
      private final LongValue overloadFactoryFePerTickThreeSpeedCards;
      private final LongValue overloadFactoryFePerTickFourSpeedCards;
      private final BooleanValue artificialLightningTriggerFromHotbar;
      private final BooleanValue artificialLightningTriggerFromBackpack;
      private final IntValue lightningCollectorHvBaseMin;
      private final IntValue lightningCollectorHvBaseMax;
      private final IntValue lightningCollectorEhvBaseMin;
      private final IntValue lightningCollectorEhvBaseMax;
      private final IntValue lightningCollectorHvCrystalStart;
      private final IntValue lightningCollectorHvCrystalEnd;
      private final IntValue lightningCollectorEhvCrystalStart;
      private final IntValue lightningCollectorEhvCrystalEnd;
      private final IntValue lightningCollectorPerfectHvOutput;
      private final IntValue lightningCollectorPerfectEhvOutput;
      private final IntValue electroChimeCatalysisPerStrikeMin;
      private final IntValue electroChimeCatalysisPerStrikeMax;
      private final DoubleValue lightningCollectorSpreadRatio;
      private final IntValue teslaCoilHighVoltageDustCost;
      private final IntValue teslaCoilHighVoltageFe;
      private final IntValue teslaCoilExtremeHighVoltageInput;
      private final IntValue teslaCoilExtremeHighVoltageFe;
      private final BooleanValue pigmeeFumoGiftOnFirstJoin;
      private final BooleanValue frequencyCardEnableAutoCleanup;
      private final IntValue frequencyCardCleanupIntervalSeconds;
      private final IntValue frequencyCardInvalidCleanupDelaySeconds;
      private final IntValue frequencyCardInvalidCleanupRequiredChecks;
      private final IntValue frequencyCardCleanupBatchSize;
      private final IntValue overloadArmorPurificationPeriodTicks;
      private final BooleanValue overloadArmorPurificationBeneficialEffects;
      private final BooleanValue overloadArmorPurificationNeutralEffects;
      private final BooleanValue overloadArmorPurificationHarmfulEffects;
      private final IntValue overloadArmorSaturationCheckIntervalTicks;
      private final DoubleValue overloadArmorUnderwaterDigMultiplier;
      private final DoubleValue overloadArmorAirborneDigMultiplier;
      private final BooleanValue overloadArmorPhaseFlightEnabled;
      private final ConfigValue<String> overloadArmorPhaseLockTeleportMode;
      private final DoubleValue overloadArmorPhaseFlightSpeedMultiplier;
      private final LongValue overloadArmorPassiveHvPerTick;
      private final LongValue overloadArmorFlightHvPerTick;
      private final LongValue overloadArmorPhaseFlightHvPerTick;
      private final IntValue overloadArmorShieldComboWindowTicks;
      private final IntValue overloadArmorUndyingComboWindowTicks;
      private final IntValue railgunBeamDamagePerSettle;
      private final DoubleValue railgunBeamBypass;
      private final IntValue railgunBaseDamageEhv1;
      private final IntValue railgunBaseDamageEhv2;
      private final IntValue railgunBaseDamageEhv3;
      private final DoubleValue railgunChargedBypass;
      private final LongValue railgunBeamFeCostPerSettle;
      private final LongValue railgunFeCostTier1;
      private final LongValue railgunFeCostTier2;
      private final LongValue railgunFeCostTier3;
      private final IntValue railgunBeamHvCostInterval;
      private final LongValue railgunEhvCostTier1;
      private final LongValue railgunEhvCostTier2;
      private final LongValue railgunEhvCostTier3;
      private final LongValue railgunBufferCapacity;
      private final BooleanValue railgunDamagePlayers;
      private final BooleanValue railgunParalysisOnPlayers;
      private final BooleanValue railgunTerrainDestructionEnabled;
      private final BooleanValue railgunTerrainDropItems;
      private final IntValue railgunTerrainBlocksPerTick;
      private final BooleanValue overloadExecutionEnabled;
      private final IntValue overloadExecutionDecayWindowTicks;
      private final DoubleValue overloadExecutionDecayPower;
      private final IntValue overloadExecutionMaxTracked;

      private Values(Builder builder) {
         this.configVersion = builder.comment("Internal config schema version. Do not edit; used by the mod for upgrade migrations.")
            .defineInRange("configVersion", 3, 1, Integer.MAX_VALUE);
         builder.push("lightningCollector");
         this.lightningCollectorCooldownTicks = builder.comment("Cooldown in ticks after each captured lightning strike.")
            .defineInRange("cooldownTicks", 0, 0, Integer.MAX_VALUE);
         builder.push("outputProfile");
         this.lightningCollectorHvBaseMin = builder.comment("Minimum HV output before crystal bonuses are applied.")
            .defineInRange("hvBaseMin", 1, 0, Integer.MAX_VALUE);
         this.lightningCollectorHvBaseMax = builder.comment("Maximum HV output before crystal bonuses are applied.")
            .defineInRange("hvBaseMax", 2, 0, Integer.MAX_VALUE);
         this.lightningCollectorEhvBaseMin = builder.comment("Minimum EHV output before crystal bonuses are applied.")
            .defineInRange("ehvBaseMin", 1, 0, Integer.MAX_VALUE);
         this.lightningCollectorEhvBaseMax = builder.comment("Maximum EHV output before crystal bonuses are applied.")
            .defineInRange("ehvBaseMax", 4, 0, Integer.MAX_VALUE);
         this.lightningCollectorHvCrystalStart = builder.comment("HV crystal count where bonus scaling starts.")
            .defineInRange("hvCrystalStart", 2, 0, Integer.MAX_VALUE);
         this.lightningCollectorHvCrystalEnd = builder.comment("HV crystal count where bonus scaling ends.")
            .defineInRange("hvCrystalEnd", 16, 0, Integer.MAX_VALUE);
         this.lightningCollectorEhvCrystalStart = builder.comment("EHV crystal count where bonus scaling starts.")
            .defineInRange("ehvCrystalStart", 2, 0, Integer.MAX_VALUE);
         this.lightningCollectorEhvCrystalEnd = builder.comment("EHV crystal count where bonus scaling ends.")
            .defineInRange("ehvCrystalEnd", 16, 0, Integer.MAX_VALUE);
         this.lightningCollectorPerfectHvOutput = builder.comment("Fixed HV output for a perfect crystal.")
            .defineInRange("perfectHvOutput", 16, 0, Integer.MAX_VALUE);
         this.lightningCollectorPerfectEhvOutput = builder.comment("Fixed EHV output for a perfect crystal.")
            .defineInRange("perfectEhvOutput", 16, 0, Integer.MAX_VALUE);
         this.lightningCollectorSpreadRatio = builder.comment("Fraction of output used as random spread. Range: > 0.")
            .defineInRange("spreadRatio", 0.12, 1.0E-6, Double.MAX_VALUE);
         builder.pop();
         builder.pop();
         builder.push("electroChimeCrystal");
         this.electroChimeMaxCatalysis = builder.comment("Catalysis value needed to transform an electro chime crystal into its perfect form.")
            .defineInRange("maxCatalysis", 180, 1, Integer.MAX_VALUE);
         this.electroChimeCatalysisPerStrikeMin = builder.comment("Minimum catalysis gained per natural (EHV) lightning strike on the collector.")
            .defineInRange("catalysisPerStrikeMin", 8, 1, Integer.MAX_VALUE);
         this.electroChimeCatalysisPerStrikeMax = builder.comment("Maximum catalysis gained per natural (EHV) lightning strike on the collector.")
            .defineInRange("catalysisPerStrikeMax", 12, 1, Integer.MAX_VALUE);
         builder.pop();
         builder.push("overloadTnt");
         this.overloadTntEnableTerrainDamage = builder.comment("Controls whether overload TNT can damage terrain with the custom blast task.")
            .define("enableTerrainDamage", true);
         this.overloadTntGlobalBlockBudgetPerTick = builder.comment("Maximum blocks processed per tick across all overload TNT tasks.")
            .defineInRange("globalBlockBudgetPerTick", 2400, 0, Integer.MAX_VALUE);
         this.overloadTntGlobalLightningBudgetPerTick = builder.comment("Maximum lightning strikes processed per tick across all overload TNT tasks.")
            .defineInRange("globalLightningBudgetPerTick", 8, 0, Integer.MAX_VALUE);
         builder.pop();
         builder.push("easterEgg");
         this.easterEggEnabled = builder.comment("Enables easter eggs.").define("enabled", true);
         this.easterEggItem = builder.comment("Easter egg item id.").define("eastereggitem", "ae2lt:lightning_collapse_matrix", value -> {
            if (value instanceof String text && ResourceLocation.m_135820_(text) != null) {
               return true;
            }

            return false;
         });
         this.easterEggWeight = builder.comment("Easter egg weight.").defineInRange("eastereggweight", 50, 0, 10000);
         Supplier<List<? extends String>> easterEggWeightsDefault = () -> AE2LTCommonConfig.DEFAULT_EASTER_EGG_WEIGHTS;
         this.easterEggWeights = builder.comment("Easter egg weights. Format: item_id=weight.")
            .defineList("eastereggweights", easterEggWeightsDefault, AE2LTCommonConfig::isEasterEggWeightEntry);
         builder.pop();
         builder.push("floatingMatter");
         this.shulkerBulletCollectionEnabled = builder.comment(
               new String[]{
                  "Whether a Silk Touch enchanted AE2 Annihilation Plane collects vanilla Shulker bullets",
                  "that reach it, consuming the bullet and inserting one Floating Matter into the ME network.",
                  "The bullet does not need to be shot down; reaching the plane is enough."
               }
            )
            .define("shulkerBulletCollection", true);
         this.floatingMatterRiseSpeed = builder.comment("Blocks per tick that a dropped Floating Matter item rises.")
            .defineInRange("riseSpeed", 0.08, 0.001, 2.0);
         this.floatingMatterDespawnHeightMultiplier = builder.comment(
               "Floating Matter despawns once it climbs above this multiple of the world max build height."
            )
            .defineInRange("despawnHeightMultiplier", 2.0, 1.0, 16.0);
         builder.pop();
         builder.push("network");
         builder.push("overloadedController");
         this.overloadedControllerChannelsPerController = builder.comment("Extra channels provided by each overloaded controller.")
            .defineInRange("channelsPerController", 128, 0, Integer.MAX_VALUE);
         this.overloadedControllerPassiveAePerTick = builder.comment("Passive AE injected per tick by an overloaded controller.")
            .defineInRange("passiveAePerTick", 100.0, 0.0, Double.MAX_VALUE);
         builder.pop();
         builder.push("wirelessConnector");
         this.wirelessConnectorMaxDistance = builder.comment(
               new String[]{
                  "Maximum block distance for Overloaded Wireless Connect Tool links.",
                  "Only limits links from overloaded providers, interfaces, and power supplies to target machines.",
                  "Set to 0 to disable this distance limit."
               }
            )
            .defineInRange("maxDistance", 128, 0, Integer.MAX_VALUE);
         builder.pop();
         builder.push("extendedPatternProvider");
         this.extendedPatternProviderPages = builder.comment("Number of 36-slot pattern pages in the Extended Overloaded Pattern Provider.")
            .defineInRange("pages", 4, 1, 64);
         builder.pop();
         builder.push("batchDispatch");
         Supplier<List<? extends String>> batchCopyLimitedBlocksDefault = () -> AE2LTCommonConfig.DEFAULT_BATCH_COPY_LIMITED_BLOCKS;
         this.batchCopyLimitedBlocks = builder.comment(
               new String[]{
                  "Block ids whose single-target pushBatch calls are capped at 1024 copies.",
                  "Matches both batch-provider blocks and physical machines targeted by overloaded providers.",
                  "Supports '*' and '?' wildcards; exact ids and namespace:* use constant-time lookup."
               }
            )
            .defineList("copyLimitedBlocks", batchCopyLimitedBlocksDefault, FastWildcardMatcher::isValidPattern);
         builder.pop();
         builder.pop();
         builder.push("overloadProcessingFactory");
         this.overloadFactoryParallelPerMatrix = builder.comment("Parallel operations provided by each Lightning Collapse Matrix.")
            .defineInRange("parallelPerMatrix", 8, 0, 67108863);
         this.overloadFactoryEnergyCapacity = builder.comment("Internal FE buffer capacity of the Overload Processing Factory.")
            .defineInRange("energyCapacity", 640000000L, 1L, Long.MAX_VALUE);
         this.overloadFactoryFePerTickNoSpeedCard = builder.comment("Maximum FE consumed per tick with no Speed Cards installed.")
            .defineInRange("fePerTickBase", 400000L, 0L, Long.MAX_VALUE);
         this.overloadFactoryFePerTickOneSpeedCard = builder.comment("Maximum FE consumed per tick with 1 Speed Card installed.")
            .defineInRange("fePerTick1SpeedCard", 2000000L, 0L, Long.MAX_VALUE);
         this.overloadFactoryFePerTickTwoSpeedCards = builder.comment("Maximum FE consumed per tick with 2 Speed Cards installed.")
            .defineInRange("fePerTick2SpeedCards", 8000000L, 0L, Long.MAX_VALUE);
         this.overloadFactoryFePerTickThreeSpeedCards = builder.comment("Maximum FE consumed per tick with 3 Speed Cards installed.")
            .defineInRange("fePerTick3SpeedCards", 32000000L, 0L, Long.MAX_VALUE);
         this.overloadFactoryFePerTickFourSpeedCards = builder.comment("Maximum FE consumed per tick with 4 Speed Cards installed.")
            .defineInRange("fePerTick4SpeedCards", 128000000L, 0L, Long.MAX_VALUE);
         builder.pop();
         builder.push("artificialLightning");
         this.artificialLightningTriggerFromHotbar = builder.comment(
               "Controls whether Overload Crystals in the hotbar or offhand can trigger artificial lightning."
            )
            .define("triggerFromHotbar", true);
         this.artificialLightningTriggerFromBackpack = builder.comment(
               "Controls whether Overload Crystals in the main inventory can trigger artificial lightning."
            )
            .define("triggerFromBackpack", false);
         builder.pop();
         builder.push("teslaCoil");
         builder.push("modeCosts");
         this.teslaCoilHighVoltageDustCost = builder.comment("Overload Crystal Dust cost for High Voltage mode.")
            .defineInRange("highVoltageDustCost", 2, 0, Integer.MAX_VALUE);
         this.teslaCoilHighVoltageFe = builder.comment("FE cost for High Voltage mode. Range: >= 1.")
            .defineInRange("highVoltageFe", 25000, 1, Integer.MAX_VALUE);
         this.teslaCoilExtremeHighVoltageInput = builder.comment("High Voltage Lightning input cost for Extreme High Voltage mode.")
            .defineInRange("extremeHighVoltageInput", 8, 0, Integer.MAX_VALUE);
         this.teslaCoilExtremeHighVoltageFe = builder.comment("FE cost for Extreme High Voltage mode. Range: >= 1.")
            .defineInRange("extremeHighVoltageFe", 500000, 1, Integer.MAX_VALUE);
         builder.pop();
         builder.pop();
         builder.push("pigmeeFumo");
         this.pigmeeFumoGiftOnFirstJoin = builder.comment("Controls whether players receive a Pigmee Fumo as a gift on their first login.")
            .define("giftOnFirstJoin", true);
         builder.pop();
         builder.push("overloadArmor");
         builder.push("movement");
         this.overloadArmorPhaseFlightEnabled = builder.comment(
               "Server master switch for the phase module's no-clip mode. Creative flight and movement guards remain available."
            )
            .define("phaseFlightEnabled", true);
         this.overloadArmorPhaseLockTeleportMode = builder.comment(
               new String[]{
                  "Server policy for the phase-lock module's external-teleport protection.",
                  "ignore-all: disable teleport protection entirely.",
                  "ignore-command: allow player-self commands and permission-level-2 management commands; block other external teleports.",
                  "ignore-none: allow player-self commands only; block every external teleport source."
               }
            )
            .define("phaseLockTeleportMode", PhaseLockTeleportMode.IGNORE_COMMAND.configValue(), PhaseLockTeleportMode::isValidConfigValue);
         this.overloadArmorPhaseFlightSpeedMultiplier = builder.comment("Movement multiplier applied while phase flight is active.")
            .defineInRange("phaseFlightSpeedMultiplier", 0.35, 0.0, 4.0);
         builder.pop();
         builder.push("defense");
         this.overloadArmorPurificationPeriodTicks = builder.comment("Ticks between automatic status effect purification attempts.")
            .defineInRange("purificationPeriodTicks", 40, 1, 72000);
         this.overloadArmorPurificationBeneficialEffects = builder.comment("Whether purification can remove beneficial effects.")
            .define("purificationBeneficialEffects", false);
         this.overloadArmorPurificationNeutralEffects = builder.comment("Whether purification can remove neutral effects.")
            .define("purificationNeutralEffects", false);
         this.overloadArmorPurificationHarmfulEffects = builder.comment("Whether purification can remove harmful effects.")
            .define("purificationHarmfulEffects", true);
         builder.pop();
         builder.push("utility");
         this.overloadArmorSaturationCheckIntervalTicks = builder.comment("Ticks between saturation sustain checks.")
            .defineInRange("saturationCheckIntervalTicks", 20, 1, 72000);
         this.overloadArmorUnderwaterDigMultiplier = builder.comment("Break-speed multiplier applied by underwater dig affinity.")
            .defineInRange("underwaterDigMultiplier", 5.0, 1.0, 64.0);
         this.overloadArmorAirborneDigMultiplier = builder.comment("Break-speed multiplier applied by airborne dig affinity.")
            .defineInRange("airborneDigMultiplier", 5.0, 1.0, 64.0);
         builder.pop();
         builder.push("lightningCosts");
         this.overloadArmorPassiveHvPerTick = builder.comment("HV lightning consumed each tick by the active reach extension armor module.")
            .defineInRange("passiveHvPerTick", 1L, 0L, Long.MAX_VALUE);
         this.overloadArmorFlightHvPerTick = builder.comment("HV lightning consumed each tick while creative flight is active.")
            .defineInRange("flightHvPerTick", 2L, 0L, Long.MAX_VALUE);
         this.overloadArmorPhaseFlightHvPerTick = builder.comment("HV lightning consumed each tick while phase flight is active.")
            .defineInRange("phaseFlightHvPerTick", 8L, 0L, Long.MAX_VALUE);
         builder.pop();
         builder.push("penalty");
         this.overloadArmorShieldComboWindowTicks = builder.comment("Ticks in the linear combo window for matrix shield lightning cost scaling.")
            .defineInRange("shieldComboWindowTicks", 200, 1, 72000);
         this.overloadArmorUndyingComboWindowTicks = builder.comment("Ticks in the linear combo window for undying FE and EHV cost scaling.")
            .defineInRange("undyingComboWindowTicks", 200, 1, 72000);
         builder.pop();
         builder.pop();
         builder.push("railgun");
         builder.push("damage");
         this.railgunBeamDamagePerSettle = builder.comment("High Voltage beam damage per 2-tick settle.")
            .defineInRange("beamDamagePerSettle", 20, 0, Integer.MAX_VALUE);
         this.railgunBeamBypass = builder.comment("High Voltage beam armor bypass (0.0 = fully blocked by armor, 1.0 = ignore armor).")
            .defineInRange("beamBypass", 0.4, 0.0, 1.0);
         this.railgunBaseDamageEhv1 = builder.comment("Charge tier 1 base damage.").defineInRange("baseDamageEhv1", 100, 0, Integer.MAX_VALUE);
         this.railgunBaseDamageEhv2 = builder.comment("Charge tier 2 base damage.").defineInRange("baseDamageEhv2", 300, 0, Integer.MAX_VALUE);
         this.railgunBaseDamageEhv3 = builder.comment("Charge tier 3 (max) base damage.").defineInRange("baseDamageEhv3", 600, 0, Integer.MAX_VALUE);
         this.railgunChargedBypass = builder.comment("Charged-shot armor bypass for all tiers (single dial — replaces per-tier 0.4/0.6/0.8).")
            .defineInRange("chargedBypass", 0.8, 0.0, 1.0);
         builder.pop();
         builder.push("energy");
         this.railgunBeamFeCostPerSettle = builder.comment("FE energy consumed per beam settle.")
            .defineInRange("beamFeCostPerSettle", 400L, 0L, Long.MAX_VALUE);
         this.railgunFeCostTier1 = builder.comment("FE energy consumed per tier-1 charged shot.").defineInRange("feCostTier1", 8000L, 0L, Long.MAX_VALUE);
         this.railgunFeCostTier2 = builder.comment("FE energy consumed per tier-2 charged shot.").defineInRange("feCostTier2", 40000L, 0L, Long.MAX_VALUE);
         this.railgunFeCostTier3 = builder.comment("FE energy consumed per tier-3 (max) charged shot.")
            .defineInRange("feCostTier3", 200000L, 0L, Long.MAX_VALUE);
         this.railgunBeamHvCostInterval = builder.comment("HV beam consumes 1 HV every N settles (settle = 2 ticks). N=8 means ~1.25 HV/sec.")
            .defineInRange("beamHvCostInterval", 8, 1, 64);
         this.railgunEhvCostTier1 = builder.comment("EHV consumed per tier-1 charged shot.").defineInRange("ehvCostTier1", 32L, 0L, Long.MAX_VALUE);
         this.railgunEhvCostTier2 = builder.comment("EHV consumed per tier-2 charged shot.").defineInRange("ehvCostTier2", 96L, 0L, Long.MAX_VALUE);
         this.railgunEhvCostTier3 = builder.comment("EHV consumed per tier-3 (max) charged shot.").defineInRange("ehvCostTier3", 256L, 0L, Long.MAX_VALUE);
         this.railgunBufferCapacity = builder.comment(
               new String[]{
                  "Base FE stored in the railgun when no structural energy module is installed.",
                  "Installing an energy module makes the railgun capacity equal to that module's capacity."
               }
            )
            .defineInRange("bufferCapacity", 1000000L, 0L, Long.MAX_VALUE);
         builder.pop();
         builder.push("misc");
         this.railgunDamagePlayers = builder.comment("Whether the railgun damages other players.").define("damagePlayers", true);
         this.railgunParalysisOnPlayers = builder.comment("Whether paralysis applies to players.").define("paralysisOnPlayers", true);
         builder.pop();
         builder.push("terrain");
         this.railgunTerrainDestructionEnabled = builder.comment(
               new String[]{"Master switch for railgun terrain destruction.", "When false, no railgun shot can break blocks even if the item setting is ON."}
            )
            .define("enableTerrainDestruction", true);
         this.railgunTerrainDropItems = builder.comment("Whether terrain destruction produces drops (drops auto-despawn after 60s).")
            .define("dropItems", false);
         this.railgunTerrainBlocksPerTick = builder.comment("Block break budget per tick across all railgun terrain jobs.")
            .defineInRange("blocksPerTick", 200, 1, 8192);
         builder.pop();
         builder.push("overloadExecution");
         this.overloadExecutionEnabled = builder.comment("Master switch for the Overload Execution module (EHv3-charged forced-kill).").define("enabled", true);
         this.overloadExecutionDecayWindowTicks = builder.comment(
               new String[]{
                  "Decay window in ticks. After this many ticks since the last hit, the recorded HP fully resets to current HP.", "Default 1200 = 60 seconds."
               }
            )
            .defineInRange("decayWindowTicks", 1200, 1, Integer.MAX_VALUE);
         this.overloadExecutionDecayPower = builder.comment(
               new String[]{
                  "Decay curve exponent (slow-start, fast-finish). recovery_fraction = (elapsed / window)^power.",
                  "  1.0 = linear",
                  "  2.0 = quadratic (default — early ticks barely heal, last ticks restore fast)",
                  "  3.0 = cubic (even slower start)"
               }
            )
            .defineInRange("decayPower", 2.0, 0.1, 10.0);
         this.overloadExecutionMaxTracked = builder.comment("Maximum number of targets whose recorded HP is kept simultaneously on a single railgun.")
            .defineInRange("maxTracked", 8, 1, 64);
         builder.pop();
         builder.pop();
         builder.push("frequencyCard");
         builder.push("cleanup");
         this.frequencyCardEnableAutoCleanup = builder.comment(
               "Controls whether invalid Overloaded Frequency Card wireless link records are cleaned up automatically."
            )
            .define("enableAutoCleanup", true);
         this.frequencyCardCleanupIntervalSeconds = builder.comment("Seconds between cleanup passes for Overloaded Frequency Card wireless links.")
            .defineInRange("cleanupIntervalSeconds", 300, 1, Integer.MAX_VALUE);
         this.frequencyCardInvalidCleanupDelaySeconds = builder.comment(
               "Seconds an invalid Overloaded Frequency Card wireless link must remain invalid before removal."
            )
            .defineInRange("invalidCleanupDelaySeconds", 300, 0, Integer.MAX_VALUE);
         this.frequencyCardInvalidCleanupRequiredChecks = builder.comment(
               "Number of cleanup passes that must confirm an invalid Overloaded Frequency Card wireless link before removal."
            )
            .defineInRange("invalidCleanupRequiredChecks", 3, 1, Integer.MAX_VALUE);
         this.frequencyCardCleanupBatchSize = builder.comment("Maximum Overloaded Frequency Card wireless links checked per cleanup pass.")
            .defineInRange("cleanupBatchSize", 128, 1, Integer.MAX_VALUE);
         builder.pop();
         builder.pop();
      }
   }
}
