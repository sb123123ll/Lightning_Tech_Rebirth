package com.moakiee.ae2lt.logic.railgun;

import com.moakiee.ae2lt.config.AE2LTCommonConfig;
import com.moakiee.ae2lt.item.railgun.RailgunChargeTier;
import com.moakiee.ae2lt.item.railgun.RailgunSettings;
import com.moakiee.ae2lt.registry.ModSounds;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.entity.BedBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(
   modid = "ae2lt"
)
public final class RailgunTerrainService {
   private static final Deque<RailgunTerrainService.DestroyJob> PENDING = new ArrayDeque<>();
   private static volatile Set<Block> PROTECTED_BLOCKS;

   private RailgunTerrainService() {
   }

   public static void queueDestroy(ServerLevel level, Vec3 center, RailgunChargeTier tier, ServerPlayer player, ItemStack stack) {
      if (AE2LTCommonConfig.railgunTerrainDestructionEnabled()) {
         if (tier != RailgunChargeTier.HV) {
            double radius = switch (tier) {
               case EHV1 -> 4.0;
               case EHV2 -> 6.0;
               case EHV3 -> 10.0;
               default -> 0.0;
            };

            float maxHardness = switch (tier) {
               case EHV1 -> 5.0F;
               case EHV2 -> 25.0F;
               case EHV3 -> 50.0F;
               default -> 0.0F;
            };
            BlockPos centerPos = BlockPos.m_274446_(center);
            if (RailgunSettings.soundEnabled(stack)) {
               level.m_5594_(null, centerPos, (SoundEvent)ModSounds.RAILGUN_FIRE_IMPACT.get(), SoundSource.BLOCKS, 1.5F + (float)tier.ordinal() * 0.3F, 0.5F);
            }

            List<BlockPos> candidates = collectSphere(centerPos, radius);

            for (int i = candidates.size() - 1; i > 0; i--) {
               int j = level.f_46441_.m_188503_(i + 1);
               BlockPos tmp = candidates.get(i);
               candidates.set(i, candidates.get(j));
               candidates.set(j, tmp);
            }

            PENDING.add(new RailgunTerrainService.DestroyJob(level.m_46472_(), player.m_20148_(), new ArrayDeque<>(candidates), maxHardness, stack.m_41777_()));
         }
      }
   }

   public static void queueDestroyAlongPath(ServerLevel level, List<Vec3> hitPoints, ServerPlayer player, ItemStack stack) {
      if (AE2LTCommonConfig.railgunTerrainDestructionEnabled()) {
         double r = 3.0;
         float maxHardness = 25.0F;

         for (Vec3 p : hitPoints) {
            List<BlockPos> candidates = collectSphere(BlockPos.m_274446_(p), r);
            PENDING.add(new RailgunTerrainService.DestroyJob(level.m_46472_(), player.m_20148_(), new ArrayDeque<>(candidates), maxHardness, stack.m_41777_()));
         }
      }
   }

   @SubscribeEvent
   public static void onServerTick(ServerTickEvent e) {
      if (e.phase == Phase.END) {
         if (!PENDING.isEmpty()) {
            MinecraftServer server = e.getServer();
            int budget = AE2LTCommonConfig.railgunTerrainBlocksPerTick();
            boolean dropItems = AE2LTCommonConfig.railgunTerrainDropItems();
            Iterator<RailgunTerrainService.DestroyJob> it = PENDING.iterator();

            while (it.hasNext() && budget > 0) {
               RailgunTerrainService.DestroyJob job = it.next();
               ServerLevel level = server.m_129880_(job.dim());
               if (level == null) {
                  it.remove();
               } else {
                  ServerPlayer player = server.m_6846_().m_11259_(job.playerId());
                  Deque<BlockPos> queue = job.queue();

                  while (budget > 0 && !queue.isEmpty()) {
                     BlockPos pos = queue.pollFirst();
                     BlockState state = level.m_8055_(pos);
                     if (state.m_60795_()) {
                        budget--;
                     } else if (!canBreak(level, pos, state, job.maxHardness())) {
                        budget--;
                     } else {
                        if (dropItems && player != null) {
                           Block.m_49881_(state, level, pos, level.m_7702_(pos), player, job.toolStack());
                        }

                        level.m_7731_(pos, Blocks.f_50016_.m_49966_(), 35);
                        if (level.f_46441_.m_188503_(12) == 0) {
                           level.m_8767_(
                              ParticleTypes.f_175830_,
                              (double)pos.m_123341_() + 0.5,
                              (double)pos.m_123342_() + 0.5,
                              (double)pos.m_123343_() + 0.5,
                              2,
                              0.3,
                              0.3,
                              0.3,
                              0.05
                           );
                        }

                        budget--;
                     }
                  }

                  if (queue.isEmpty()) {
                     it.remove();
                  }
               }
            }
         }
      }
   }

   private static boolean canBreak(Level level, BlockPos pos, BlockState state, float maxHardness) {
      if (state.m_204336_(BlockTags.f_13070_)) {
         return false;
      } else {
         BlockEntity be = level.m_7702_(pos);
         if (be instanceof Container) {
            return false;
         } else if (!(be instanceof SignBlockEntity) && !(be instanceof BannerBlockEntity) && !(be instanceof BedBlockEntity)) {
            if (protectedBlocks().contains(state.m_60734_())) {
               return false;
            } else {
               float hardness = state.m_60800_(level, pos);
               return hardness < 0.0F ? false : hardness <= maxHardness;
            }
         } else {
            return false;
         }
      }
   }

   private static Set<Block> protectedBlocks() {
      Set<Block> cached = PROTECTED_BLOCKS;
      if (cached != null) {
         return cached;
      } else {
         Set<Block> set = Collections.newSetFromMap(new IdentityHashMap<>());

         for (Entry<ResourceKey<Block>, Block> entry : BuiltInRegistries.f_256975_.m_6579_()) {
            String ns = entry.getKey().m_135782_().m_135827_();
            if (ns.equals("ae2lt") || ns.equals("ae2") || ns.equals("appliedenergistics2")) {
               set.add(entry.getValue());
            }
         }

         PROTECTED_BLOCKS = set;
         return set;
      }
   }

   private static List<BlockPos> collectSphere(BlockPos center, double radius) {
      int r = (int)Math.ceil(radius);
      double r2 = radius * radius;
      List<BlockPos> list = new ArrayList<>();

      for (int dx = -r; dx <= r; dx++) {
         for (int dy = -r; dy <= r; dy++) {
            for (int dz = -r; dz <= r; dz++) {
               if (!((double)(dx * dx + dy * dy + dz * dz) > r2)) {
                  list.add(center.m_7918_(dx, dy, dz).m_7949_());
               }
            }
         }
      }

      return list;
   }

   public static record DestroyJob(ResourceKey<Level> dim, UUID playerId, Deque<BlockPos> queue, float maxHardness, ItemStack toolStack) {
   }
}
