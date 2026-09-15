package com.moakiee.ae2lt.event;

import com.moakiee.ae2lt.blockentity.LightningCollectorBlockEntity;
import com.moakiee.ae2lt.lightning.strike.LightningStrikeRecipe;
import com.moakiee.ae2lt.lightning.strike.StructureRequirement;
import com.moakiee.ae2lt.registry.ModBlocks;
import com.moakiee.ae2lt.registry.ModRecipeTypes;
import com.moakiee.ae2lt.util.RecipeManagerByTypeAccess;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.slf4j.Logger;

public final class NaturalLightningTransformationHandler {
   public static final String NATURAL_WEATHER_LIGHTNING_TAG = "ae2lt.natural_weather_lightning";
   private static final String TRANSFORMATION_CHECKED_TAG = "ae2lt.natural_transform_checked";
   private static final String MAIN_HANDLED_TAG = "ae2lt.main_lightning_handled";
   private static final Logger LOG = LogUtils.getLogger();
   private static volatile boolean warnedTakeover;
   private static final DustParticleOptions PINK_DUST = new DustParticleOptions(new Vector3f(1.0F, 0.45F, 0.78F), 1.6F);
   private static final DustParticleOptions PURPLE_DUST = new DustParticleOptions(new Vector3f(0.78F, 0.34F, 1.0F), 1.4F);
   private static final DustParticleOptions CERTUS_DUST = new DustParticleOptions(new Vector3f(0.85F, 0.92F, 1.0F), 1.4F);
   private static final List<BlockPos> CORNER_OFFSETS = List.of(new BlockPos(-1, 0, -1), new BlockPos(1, 0, -1), new BlockPos(-1, 0, 1), new BlockPos(1, 0, 1));
   private static final List<BlockPos> EDGE_OFFSETS = List.of(new BlockPos(0, 0, -1), new BlockPos(-1, 0, 0), new BlockPos(1, 0, 0), new BlockPos(0, 0, 1));
   private static final List<BlockPos> OUTER_RING_OFFSETS = List.of(
      new BlockPos(-2, 0, -2),
      new BlockPos(-1, 0, -2),
      new BlockPos(0, 0, -2),
      new BlockPos(1, 0, -2),
      new BlockPos(2, 0, -2),
      new BlockPos(-2, 0, -1),
      new BlockPos(2, 0, -1),
      new BlockPos(-2, 0, 0),
      new BlockPos(2, 0, 0),
      new BlockPos(-2, 0, 1),
      new BlockPos(2, 0, 1),
      new BlockPos(-2, 0, 2),
      new BlockPos(-1, 0, 2),
      new BlockPos(0, 0, 2),
      new BlockPos(1, 0, 2),
      new BlockPos(2, 0, 2)
   );

   private NaturalLightningTransformationHandler() {
   }

   public static void handleLightningTick(LightningBolt lightningBolt) {
      if (lightningBolt.m_9236_() instanceof ServerLevel serverLevel) {
         CompoundTag data = lightningBolt.getPersistentData();
         if (data.m_128471_("ae2lt.natural_transform_checked")) {
            if (!data.m_128471_("ae2lt.main_lightning_handled") && !warnedTakeover) {
               warnedTakeover = true;
               LOG.warn(
                  "AE2 Lightning Tech: a LightningBolt arrived with {} already set but {} unset. Another mod is intercepting natural lightning before this mod's handler runs; LightningCollectorBlockEntity.captureLightning and the public LightningCollectedEvent will not fire for those strikes. This warning logs only once per JVM.",
                  "ae2lt.natural_transform_checked",
                  "ae2lt.main_lightning_handled"
               );
            }
         } else {
            data.m_128379_("ae2lt.natural_transform_checked", true);
            data.m_128379_("ae2lt.main_lightning_handled", true);
            boolean naturalWeatherLightning = data.m_128471_("ae2lt.natural_weather_lightning");
            tryCaptureLightning(serverLevel, lightningBolt.m_20183_(), naturalWeatherLightning);
            tryTransformFromNearbyLightningRod(serverLevel, lightningBolt.m_20183_(), naturalWeatherLightning);
         }
      }
   }

   private static void tryCaptureLightning(ServerLevel level, BlockPos lightningPos, boolean naturalWeatherLightning) {
      for (int yOffset = 0; yOffset <= 2; yOffset++) {
         BlockPos rodPos = lightningPos.m_6625_(yOffset);
         if (level.m_8055_(rodPos).m_60713_(Blocks.f_152587_)
            && level.m_7702_(rodPos.m_7495_()) instanceof LightningCollectorBlockEntity collector
            && collector.canCaptureLightning()) {
            collector.captureLightning(naturalWeatherLightning);
            return;
         }
      }
   }

   private static void tryTransformFromNearbyLightningRod(ServerLevel level, BlockPos lightningPos, boolean naturalWeather) {
      List<LightningStrikeRecipe> allRecipes = new ArrayList<>(
         RecipeManagerByTypeAccess.byType(level.m_7465_(), (RecipeType)ModRecipeTypes.LIGHTNING_STRIKE_TYPE.get()).values()
      );
      if (!allRecipes.isEmpty()) {
         for (int yOffset = 0; yOffset <= 2; yOffset++) {
            BlockPos rodPos = lightningPos.m_6625_(yOffset);
            BlockState rodState = level.m_8055_(rodPos);
            if (rodState.m_60713_(Blocks.f_152587_)) {
               BlockPos centerPos = rodPos.m_7495_();

               for (LightningStrikeRecipe recipe : allRecipes) {
                  if ((!recipe.requiresNaturalLightning() || naturalWeather) && tryApplyRecipe(level, recipe, centerPos, rodPos)) {
                     return;
                  }
               }
            }
         }
      }
   }

   private static boolean tryApplyRecipe(ServerLevel level, LightningStrikeRecipe recipe, BlockPos centerPos, BlockPos rodPos) {
      BlockState centerState = level.m_8055_(centerPos);
      if (!centerState.m_60713_(recipe.centerInput())) {
         return false;
      } else {
         for (StructureRequirement req : recipe.requirements()) {
            BlockPos worldPos = centerPos.m_121955_(req.offset());
            if (!level.m_8055_(worldPos).m_60713_(req.block())) {
               return false;
            }
         }

         spawnTransformationParticles(level, recipe, centerPos, rodPos);

         for (StructureRequirement reqx : recipe.requirements()) {
            if (reqx.consume()) {
               level.m_46597_(centerPos.m_121955_(reqx.offset()), Blocks.f_50016_.m_49966_());
            }
         }

         level.m_46597_(centerPos, recipe.centerOutput().m_49966_());
         spawnCompletionParticles(level, centerPos);
         return true;
      }
   }

   private static void spawnTransformationParticles(ServerLevel level, LightningStrikeRecipe recipe, BlockPos centerPos, BlockPos rodPos) {
      boolean rich = hasCornerBlock(recipe, (Block)ModBlocks.OVERLOAD_CRYSTAL_BLOCK.get());
      if (rich) {
         spawnRichTransformationParticles(level, rodPos, centerPos);
      } else {
         spawnSimpleTransformationParticles(level, rodPos, centerPos);
      }
   }

   private static boolean hasCornerBlock(LightningStrikeRecipe recipe, Block target) {
      for (StructureRequirement req : recipe.requirements()) {
         BlockPos off = req.offset();
         if (Math.abs(off.m_123341_()) == 1 && Math.abs(off.m_123343_()) == 1 && off.m_123342_() == 0 && req.block() == target) {
            return true;
         }
      }

      return false;
   }

   private static void spawnRichTransformationParticles(ServerLevel level, BlockPos rodPos, BlockPos centerPos) {
      Vec3 rodVec = Vec3.m_82512_(rodPos).m_82520_(0.0, -0.2, 0.0);
      Vec3 centerVec = Vec3.m_82512_(centerPos).m_82520_(0.0, 0.55, 0.0);

      for (int i = 0; i < 7; i++) {
         double progress = (double)i / 6.0;
         Vec3 point = rodVec.m_165921_(centerVec, progress);
         level.m_8767_(ParticleTypes.f_175830_, point.f_82479_, point.f_82480_, point.f_82481_, 10, 0.12, 0.1, 0.12, 0.03);
         level.m_8767_(PINK_DUST, point.f_82479_, point.f_82480_, point.f_82481_, 8, 0.08, 0.08, 0.08, 0.01);
      }

      for (BlockPos offset : EDGE_OFFSETS) {
         Vec3 from = Vec3.m_82512_(centerPos.m_121955_(offset)).m_82520_(0.0, 0.55, 0.0);
         Vec3 toward = from.m_82505_(centerVec).m_82490_(0.18);
         level.m_8767_(PURPLE_DUST, from.f_82479_, from.f_82480_, from.f_82481_, 24, 0.18, 0.14, 0.18, 0.01);
         level.m_8767_(ParticleTypes.f_123771_, from.f_82479_, from.f_82480_, from.f_82481_, 20, toward.f_82479_, 0.06, toward.f_82481_, 0.18);
      }

      for (BlockPos offset : CORNER_OFFSETS) {
         Vec3 from = Vec3.m_82512_(centerPos.m_121955_(offset)).m_82520_(0.0, 0.55, 0.0);
         Vec3 toward = from.m_82505_(centerVec).m_82490_(0.16);
         level.m_8767_(PINK_DUST, from.f_82479_, from.f_82480_, from.f_82481_, 26, 0.2, 0.16, 0.2, 0.01);
         level.m_8767_(ParticleTypes.f_123809_, from.f_82479_, from.f_82480_, from.f_82481_, 20, toward.f_82479_, 0.06, toward.f_82481_, 0.22);
      }

      for (int i = 0; i < OUTER_RING_OFFSETS.size(); i++) {
         BlockPos offset = OUTER_RING_OFFSETS.get(i);
         Vec3 from = Vec3.m_82512_(centerPos.m_121955_(offset)).m_82520_(0.0, 0.2 + (double)(i % 3) * 0.12, 0.0);
         Vec3 toward = from.m_82505_(centerVec).m_82490_(0.08);
         DustParticleOptions ringDust = (i & 1) == 0 ? PURPLE_DUST : PINK_DUST;
         level.m_8767_(ringDust, from.f_82479_, from.f_82480_, from.f_82481_, 12, 0.14, 0.06, 0.14, 0.01);
         level.m_8767_(ParticleTypes.f_123809_, from.f_82479_, from.f_82480_, from.f_82481_, 8, toward.f_82479_, 0.03, toward.f_82481_, 0.1);
      }
   }

   private static void spawnSimpleTransformationParticles(ServerLevel level, BlockPos rodPos, BlockPos centerPos) {
      Vec3 rodVec = Vec3.m_82512_(rodPos).m_82520_(0.0, -0.2, 0.0);
      Vec3 centerVec = Vec3.m_82512_(centerPos).m_82520_(0.0, 0.55, 0.0);

      for (int i = 0; i < 5; i++) {
         double progress = (double)i / 4.0;
         Vec3 point = rodVec.m_165921_(centerVec, progress);
         level.m_8767_(ParticleTypes.f_175830_, point.f_82479_, point.f_82480_, point.f_82481_, 6, 0.1, 0.08, 0.1, 0.02);
         level.m_8767_(CERTUS_DUST, point.f_82479_, point.f_82480_, point.f_82481_, 5, 0.07, 0.07, 0.07, 0.01);
      }

      for (BlockPos offset : CORNER_OFFSETS) {
         Vec3 from = Vec3.m_82512_(centerPos.m_121955_(offset)).m_82520_(0.0, 0.55, 0.0);
         Vec3 toward = from.m_82505_(centerVec).m_82490_(0.16);
         level.m_8767_(CERTUS_DUST, from.f_82479_, from.f_82480_, from.f_82481_, 20, 0.18, 0.14, 0.18, 0.01);
         level.m_8767_(ParticleTypes.f_175830_, from.f_82479_, from.f_82480_, from.f_82481_, 14, toward.f_82479_, 0.06, toward.f_82481_, 0.18);
      }

      for (BlockPos offset : EDGE_OFFSETS) {
         Vec3 from = Vec3.m_82512_(centerPos.m_121955_(offset)).m_82520_(0.0, 0.55, 0.0);
         Vec3 toward = from.m_82505_(centerVec).m_82490_(0.16);
         level.m_8767_(PURPLE_DUST, from.f_82479_, from.f_82480_, from.f_82481_, 18, 0.18, 0.14, 0.18, 0.01);
         level.m_8767_(ParticleTypes.f_123809_, from.f_82479_, from.f_82480_, from.f_82481_, 14, toward.f_82479_, 0.06, toward.f_82481_, 0.18);
      }
   }

   private static void spawnCompletionParticles(ServerLevel level, BlockPos centerPos) {
      Vec3 centerVec = Vec3.m_82512_(centerPos).m_82520_(0.0, 0.7, 0.0);

      for (int i = 0; i < 4; i++) {
         double y = centerVec.f_82480_ + (double)i * 0.18;
         level.m_8767_(PINK_DUST, centerVec.f_82479_, y, centerVec.f_82481_, 18, 0.24, 0.04, 0.24, 0.01);
         level.m_8767_(ParticleTypes.f_123810_, centerVec.f_82479_, y, centerVec.f_82481_, 10, 0.18, 0.1, 0.18, 0.03);
      }

      level.m_8767_(ParticleTypes.f_175830_, centerVec.f_82479_, centerVec.f_82480_ + 0.2, centerVec.f_82481_, 24, 0.28, 0.28, 0.28, 0.02);
      level.m_8767_(ParticleTypes.f_123809_, centerVec.f_82479_, centerVec.f_82480_ + 0.25, centerVec.f_82481_, 32, 0.35, 0.25, 0.35, 0.12);
   }
}
