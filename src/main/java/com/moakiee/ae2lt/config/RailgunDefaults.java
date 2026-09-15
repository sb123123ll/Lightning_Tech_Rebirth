package com.moakiee.ae2lt.config;

public final class RailgunDefaults {
   public static final int BEAM_SETTLE_INTERVAL_TICKS = 2;
   public static final int BEAM_RANGE = 64;
   public static final int BEAM_CHAIN_THROTTLE_TICKS = 5;
   public static final int BEAM_MAX_CONCURRENT = 16;
   public static final double ENTITY_QUERY_SEGMENT_LENGTH = 16.0;
   public static final double MAX_RANGE_MULTIPLIER = 4.0;
   public static final double CHAIN_DECAY = 0.92;
   public static final int CHAIN_BASE = 3;
   public static final int CHAIN_HARD_CAP = 50;
   public static final double CHAIN_RADIUS = 16.0;
   public static final int CHAIN_FORK_BASE_EHV1 = 2;
   public static final int CHAIN_FORK_BASE_EHV2 = 3;
   public static final int CHAIN_FORK_BASE_EHV3 = 4;
   public static final int CHAIN_FORK_PER_COMPUTE = 1;
   public static final int CHAIN_FORK_STORM_BONUS = 1;
   public static final int CHAIN_FORK_HARD_CAP = 8;
   public static final int CHARGED_RANGE = 64;
   public static final int PULSE_RADIUS = 10;
   public static final double PULSE_DAMAGE_RATIO = 0.6;
   public static final int PENETRATION_MAX_TARGETS = 5;
   public static final double IMPACT_RADIUS_TIER1 = 5.5;
   public static final double IMPACT_RADIUS_TIER2 = 8.0;
   public static final double IMPACT_RADIUS_TIER3 = 12.0;
   public static final double IMPACT_DAMAGE_RATIO_TIER1 = 0.45;
   public static final double IMPACT_DAMAGE_RATIO_TIER2 = 0.55;
   public static final double IMPACT_DAMAGE_RATIO_TIER3 = 0.65;
   public static final double OVERLOAD_EXECUTION_SPLASH_RADIUS_RATIO = 0.5;
   public static final int CHARGE_TICKS_TIER1 = 10;
   public static final int CHARGE_TICKS_TIER2 = 24;
   public static final int CHARGE_TICKS_TIER3 = 40;
   public static final double STORM_DAMAGE_MUL = 1.25;
   public static final int STORM_CHAIN_BONUS = 2;
   public static final double STORM_CHAIN_RADIUS_BONUS = 4.0;
   public static final int PARALYSIS_DURATION_TICKS = 40;
   public static final double RECOIL_SPEED_TIER1 = 0.6;
   public static final double RECOIL_SPEED_TIER2 = 1.2;
   public static final double RECOIL_SPEED_TIER3 = 2.0;
   public static final double RECOIL_CROUCH_MUL = 0.5;
   public static final double RECOIL_AIRBORNE_MUL = 1.5;
   public static final double TERRAIN_RADIUS_TIER1 = 4.0;
   public static final double TERRAIN_RADIUS_TIER2 = 6.0;
   public static final double TERRAIN_RADIUS_TIER3 = 10.0;
   public static final double TERRAIN_HARDNESS_TIER1 = 5.0;
   public static final double TERRAIN_HARDNESS_TIER2 = 25.0;
   public static final double TERRAIN_HARDNESS_TIER3 = 50.0;
   public static final double PENETRATION_DESTROY_RADIUS = 3.0;
   public static final double PENETRATION_DESTROY_HARDNESS = 25.0;

   private RailgunDefaults() {
   }
}
