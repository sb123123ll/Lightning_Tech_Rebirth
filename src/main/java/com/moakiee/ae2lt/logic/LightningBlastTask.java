package com.moakiee.ae2lt.logic;

import it.unimi.dsi.fastutil.longs.Long2BooleanOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.level.BlockEvent.BreakEvent;

public class LightningBlastTask {
   public static final int DEFAULT_RADIUS = 48;
   public static final int DEFAULT_BLOCKS_PER_TICK = 1800;
   public static final int DEFAULT_INITIAL_LIGHTNING_COUNT = 16;
   public static final int DEFAULT_AFTERSHOCK_TICKS = 8;
   public static final int DEFAULT_AFTERSHOCK_MIN_LIGHTNING = 3;
   public static final int DEFAULT_AFTERSHOCK_MAX_LIGHTNING = 6;
   public static final int DEFAULT_SHORT_THUNDERSTORM_TICKS = 160;
   public static final int DEFAULT_LIGHTNING_HORIZONTAL_RADIUS = 24;
   public static final int DEFAULT_SHELLS_PER_TICK = 3;
   private static final float INITIAL_BLAST_DAMAGE_CORE = 100.0F;
   private static final float INITIAL_BLAST_DAMAGE_EDGE = 50.0F;
   private static final double CORE_RADIUS_RATIO = 0.28;
   private static final double INNER_BLAST_SCORE = 24.0;
   private static final double OUTER_BLAST_SCORE = 3.5;
   private static final double CORE_BLAST_BONUS = 10.0;
   private static final double HARDNESS_WEIGHT = 1.05;
   private static final double RESISTANCE_WEIGHT = 0.14;
   private static final double CHANCE_FLOOR = 3.0;
   private static final double CHANCE_DIVISOR = 9.0;
   private static final int QUEUE_LOOKAHEAD_SHELLS = 8;
   private static final double SHELL_PRIORITY_JITTER = 2.75;
   private static final Set<Block> PROTECTED_BLOCKS = Set.of(
      Blocks.f_50752_, Blocks.f_50375_, Blocks.f_50258_, Blocks.f_50272_, Blocks.f_50448_, Blocks.f_50447_
   );
   private final ServerLevel level;
   private final BlockPos center;
   private final int radius;
   private final int radiusSquared;
   private final int blocksPerTick;
   private final int shellsPerTick;
   private final PriorityQueue<LightningBlastTask.BlastCandidate> pendingBlastBlocks;
   private final Long2BooleanOpenHashMap chunkProtectionCache = new Long2BooleanOpenHashMap();
   private FakePlayer breakerPlayer;
   private int nextShellRadiusToQueue;
   private boolean scanFinished;
   private int destroyedBlocks;
   private boolean strikeSequenceStarted;
   private int remainingAftershockTicks;
   private int pendingLightningStrikes;
   private boolean centerStrikeSpawned;
   private boolean completed;

   public LightningBlastTask(ServerLevel level, BlockPos center) {
      this(level, center, 48, 1800, 3);
   }

   public LightningBlastTask(ServerLevel level, BlockPos center, int radius, int blocksPerTick) {
      this(level, center, radius, blocksPerTick, 3);
   }

   public LightningBlastTask(ServerLevel level, BlockPos center, int radius, int blocksPerTick, int shellsPerTick) {
      this.level = level;
      this.center = center.m_7949_();
      this.radius = Math.max(0, radius);
      this.radiusSquared = this.radius * this.radius;
      this.blocksPerTick = Math.max(1, blocksPerTick);
      this.shellsPerTick = Math.max(1, shellsPerTick);
      this.pendingBlastBlocks = new PriorityQueue<>();
      this.nextShellRadiusToQueue = 0;
   }

   public ServerLevel getLevel() {
      return this.level;
   }

   public BlockPos getCenter() {
      return this.center;
   }

   public int getRadius() {
      return this.radius;
   }

   public int getBlocksPerTick() {
      return this.blocksPerTick;
   }

   public int getShellsPerTick() {
      return this.shellsPerTick;
   }

   public int getDestroyedBlocks() {
      return this.destroyedBlocks;
   }

   public boolean isCompleted() {
      return this.completed;
   }

   public LightningBlastTask.TickResult tick(int blockBudget, int lightningBudget) {
      if (this.completed) {
         return new LightningBlastTask.TickResult(0, 0);
      } else {
         int consumedLightning = this.processLightning(lightningBudget);
         int consumedBlocks = this.processShells(blockBudget);
         if (this.scanFinished && this.remainingAftershockTicks <= 0 && this.pendingLightningStrikes <= 0) {
            this.completed = true;
         }

         return new LightningBlastTask.TickResult(consumedBlocks, consumedLightning);
      }
   }

   private int processLightning(int lightningBudget) {
      if (lightningBudget <= 0) {
         return 0;
      } else {
         if (!this.strikeSequenceStarted) {
            this.startStrikeSequence();
         }

         int spawned = 0;

         while (spawned < lightningBudget) {
            if (this.pendingLightningStrikes > 0) {
               BlockPos strikePos = this.centerStrikeSpawned
                  ? this.getRandomStrikePos()
                  : this.resolveStrikePos(this.center.m_123341_(), this.center.m_123343_(), this.center.m_123342_());
               this.spawnLightningAt(strikePos);
               this.centerStrikeSpawned = true;
               this.pendingLightningStrikes--;
               spawned++;
            } else {
               if (this.remainingAftershockTicks <= 0) {
                  break;
               }

               this.queueAftershockBurst();
               this.remainingAftershockTicks--;
            }
         }

         return spawned;
      }
   }

   private int processShells(int blockBudget) {
      if (!this.scanFinished && blockBudget > 0) {
         this.queuePendingShells();
         int destroyLimit = Math.min(this.blocksPerTick, blockBudget);
         int destroyedThisTick = 0;

         while (!this.scanFinished && destroyedThisTick < destroyLimit) {
            if (this.pendingBlastBlocks.isEmpty()) {
               this.queuePendingShells();
               if (this.pendingBlastBlocks.isEmpty()) {
                  this.scanFinished = true;
                  break;
               }
            }

            BlockPos pos = this.pendingBlastBlocks.poll().pos();
            if (this.level.m_46739_(pos) && this.level.m_46805_(pos)) {
               BlockState state = this.level.m_8055_(pos);
               if (!this.isBreakProtected(pos, state) && shouldDestroy(pos, state, this.level, this.center, this.radius)) {
                  this.destroyBlockFast(pos, state);
                  destroyedThisTick++;
                  this.destroyedBlocks++;
                  if (this.pendingBlastBlocks.size() < destroyLimit) {
                     this.queuePendingShells();
                  }
               }
            }
         }

         if (this.nextShellRadiusToQueue > this.radius && this.pendingBlastBlocks.isEmpty()) {
            this.scanFinished = true;
         }

         return destroyedThisTick;
      } else {
         return 0;
      }
   }

   private void queuePendingShells() {
      for (int queuedShells = 0; this.nextShellRadiusToQueue <= this.radius && queuedShells < 8; queuedShells++) {
         int shellRadius = this.nextShellRadiusToQueue++;

         for (BlockPos pos : this.buildShell(shellRadius)) {
            double jitter = (this.level.f_46441_.m_188500_() * 2.0 - 1.0) * 2.75;
            this.pendingBlastBlocks.add(new LightningBlastTask.BlastCandidate(pos, (double)shellRadius + jitter));
         }
      }
   }

   private void startStrikeSequence() {
      this.strikeSequenceStarted = true;
      this.remainingAftershockTicks = 8;
      this.pendingLightningStrikes = 16;
      this.applyInitialBlastDamage();
      this.tryStartShortThunderstorm();
   }

   private void applyInitialBlastDamage() {
      double r = (double)Math.max(1, this.radius);
      AABB aabb = new AABB(
         (double)this.center.m_123341_() - r,
         (double)this.center.m_123342_() - r,
         (double)this.center.m_123343_() - r,
         (double)this.center.m_123341_() + r + 1.0,
         (double)this.center.m_123342_() + r + 1.0,
         (double)this.center.m_123343_() + r + 1.0
      );
      Vec3 centerVec = Vec3.m_82512_(this.center);
      double radiusSq = r * r;
      DamageSource source = this.level.m_269111_().m_269548_();

      for (LivingEntity entity : this.level.m_45976_(LivingEntity.class, aabb)) {
         double dSq = entity.m_20182_().m_82557_(centerVec);
         if (!(dSq > radiusSq)) {
            double falloff = 1.0 - Math.sqrt(dSq) / r;
            float damage = (float)(50.0 + 50.0 * falloff);
            entity.m_6469_(source, damage);
         }
      }
   }

   private void queueAftershockBurst() {
      this.pendingLightningStrikes = this.pendingLightningStrikes + 3 + this.level.f_46441_.m_188503_(4);
   }

   private BlockPos getRandomStrikePos() {
      int horizontalRadius = Math.min(this.radius, 24);

      int dx;
      int dz;
      do {
         dx = this.level.f_46441_.m_188503_(horizontalRadius * 2 + 1) - horizontalRadius;
         dz = this.level.f_46441_.m_188503_(horizontalRadius * 2 + 1) - horizontalRadius;
      } while (dx * dx + dz * dz > horizontalRadius * horizontalRadius);

      return this.resolveStrikePos(this.center.m_123341_() + dx, this.center.m_123343_() + dz, this.center.m_123342_());
   }

   private BlockPos resolveStrikePos(int x, int z, int fallbackY) {
      int minY = this.level.m_141937_();
      int maxY = this.level.m_151558_() - 1;
      int y = this.level.m_6924_(Types.MOTION_BLOCKING_NO_LEAVES, x, z);
      if (y <= minY) {
         y = fallbackY;
      } else {
         y = Math.min(y, maxY);
      }

      BlockPos candidate = new BlockPos(x, clamp(y, minY, maxY), z);
      if (!this.level.m_46739_(candidate)) {
         candidate = new BlockPos(this.center.m_123341_(), clamp(fallbackY, minY, maxY), this.center.m_123343_());
      }

      if (!this.level.m_46805_(candidate)) {
         return new BlockPos(this.center.m_123341_(), clamp(fallbackY, minY, maxY), this.center.m_123343_());
      } else {
         if (this.level.m_8055_(candidate).m_60795_()) {
            for (int dy = 1; dy <= 6 && candidate.m_123342_() - dy >= minY; dy++) {
               BlockPos lower = candidate.m_6625_(dy);
               if (!this.level.m_8055_(lower).m_60795_()) {
                  return lower.m_7494_();
               }
            }
         }

         return candidate;
      }
   }

   private void spawnLightningAt(BlockPos strikePos) {
      LightningBolt lightningBolt = (LightningBolt)EntityType.f_20465_.m_20615_(this.level);
      if (lightningBolt != null) {
         Vec3 centerPos = Vec3.m_82539_(strikePos);
         lightningBolt.m_6027_(centerPos.f_82479_, (double)strikePos.m_123342_(), centerPos.f_82481_);
         lightningBolt.m_20874_(false);
         this.level.m_7967_(lightningBolt);
      }
   }

   private void tryStartShortThunderstorm() {
      if (WeatherControlHelper.supportsWeather(this.level)) {
         WeatherControlHelper.setThunderstorm(this.level, 160);
      }
   }

   private static boolean shouldDestroy(BlockPos pos, BlockState state, ServerLevel level, BlockPos center, int radius) {
      if (state.m_60795_()) {
         return false;
      } else if (isProtectedBlock(state)) {
         return false;
      } else if (!state.m_60819_().m_76178_()) {
         return false;
      } else {
         double hardness = (double)state.m_60800_(level, pos);
         if (hardness < 0.0) {
            return false;
         } else {
            double safeRadius = Math.max(1.0, (double)radius);
            double distanceRatio = Math.min(1.0, Math.sqrt(center.m_123331_(pos)) / safeRadius);
            double blastScore = computeBlastScore(distanceRatio, radius);
            double blockScore = hardness * 1.05 + (double)state.m_60734_().m_7325_() * 0.14;
            if (distanceRatio <= 0.28) {
               return blockScore <= blastScore + 10.0;
            } else {
               double chance = clampDouble((blastScore - blockScore + 3.0) / 9.0, 0.0, 1.0);
               return chance > 0.0 && level.f_46441_.m_188500_() < chance;
            }
         }
      }
   }

   private static double computeBlastScore(double distanceRatio, int radius) {
      double radialStrength = 1.0 - clampDouble(distanceRatio, 0.0, 1.0);
      double radiusBonus = Math.min(4.0, (double)radius / 16.0);
      return 3.5 + radialStrength * (24.0 + radiusBonus);
   }

   private void destroyBlockFast(BlockPos pos, BlockState state) {
      state.m_60734_().m_6786_(this.level, pos, state);
      this.level.m_6933_(pos, Blocks.f_50016_.m_49966_(), 34, 0);
   }

   private static boolean isProtectedBlock(BlockState state) {
      for (Block block : PROTECTED_BLOCKS) {
         if (state.m_60713_(block)) {
            return true;
         }
      }

      return false;
   }

   private List<BlockPos> buildShell(int shellRadius) {
      if (shellRadius <= 0) {
         return List.of(this.center);
      } else {
         int outer = shellRadius * shellRadius;
         int inner = (shellRadius - 1) * (shellRadius - 1);
         List<BlockPos> shell = new ArrayList<>(shellRadius * shellRadius * 8);

         for (int dx = -shellRadius; dx <= shellRadius; dx++) {
            int dx2 = dx * dx;

            for (int dy = -shellRadius; dy <= shellRadius; dy++) {
               int dy2 = dy * dy;

               for (int dz = -shellRadius; dz <= shellRadius; dz++) {
                  int distanceSq = dx2 + dy2 + dz * dz;
                  if (distanceSq <= outer && distanceSq > inner) {
                     shell.add(this.center.m_7918_(dx, dy, dz).m_7949_());
                  }
               }
            }
         }

         return shell;
      }
   }

   private static int clamp(int value, int min, int max) {
      return Math.max(min, Math.min(max, value));
   }

   private static double clampDouble(double value, double min, double max) {
      return Math.max(min, Math.min(max, value));
   }

   private boolean isBreakProtected(BlockPos pos, BlockState state) {
      long key = ChunkPos.m_45589_(pos.m_123341_() >> 4, pos.m_123343_() >> 4);
      if (this.chunkProtectionCache.containsKey(key)) {
         return this.chunkProtectionCache.get(key);
      } else {
         Player breaker = this.getBreakerPlayer();

         boolean cancelled;
         try {
            BreakEvent event = new BreakEvent(this.level, pos, state, breaker);
            MinecraftForge.EVENT_BUS.post(event);
            cancelled = event.isCanceled();
         } catch (Throwable var8) {
            cancelled = true;
         }

         this.chunkProtectionCache.put(key, cancelled);
         return cancelled;
      }
   }

   private Player getBreakerPlayer() {
      if (this.breakerPlayer == null) {
         this.breakerPlayer = OverloadBlastFakePlayer.get(this.level);
      }

      return this.breakerPlayer;
   }

   private static record BlastCandidate(BlockPos pos, double priority) implements Comparable<LightningBlastTask.BlastCandidate> {
      public int compareTo(LightningBlastTask.BlastCandidate other) {
         return Double.compare(this.priority, other.priority);
      }
   }

   public static record TickResult(int consumedBlocks, int consumedLightning) {
   }
}
