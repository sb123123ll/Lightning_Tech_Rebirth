package com.moakiee.ae2lt.item;

import appeng.client.render.effects.ParticleTypes;
import com.moakiee.ae2lt.event.ArtificialLightningHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class OverloadCrystalItem extends AE2LTItem {
   private static final String DROPPED_TICKS_TAG = "ae2lt.overload_dropped_ticks";
   private static final int SUMMON_DELAY_TICKS = 200;
   private static final int DROPPED_TICK_INTERVAL = 4;

   public OverloadCrystalItem(Properties properties) {
      super(properties);
   }

   public void m_6883_(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
      super.m_6883_(stack, level, entity, slotId, isSelected);
      if (level.f_46443_ && entity instanceof Player player) {
         boolean inMainHand = player.m_21205_() == stack;
         boolean inOffHand = player.m_21206_() == stack;
         if (inMainHand || inOffHand) {
            spawnHeldLightning(level, player, inMainHand);
         }
      }
   }

   public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
      if (entity.m_9236_().f_46443_) {
         spawnDroppedLightning(entity);
      } else if (entity.m_9236_() instanceof ServerLevel serverLevel) {
         if (entity.f_19797_ % 4 != 0) {
            return false;
         }

         int droppedTicks = entity.getPersistentData().m_128451_("ae2lt.overload_dropped_ticks") + 4;
         if (droppedTicks >= 200) {
            entity.getPersistentData().m_128405_("ae2lt.overload_dropped_ticks", 0);
            ArtificialLightningHandler.spawnArtificialLightning(serverLevel, entity.m_20182_(), null);
         } else {
            entity.getPersistentData().m_128405_("ae2lt.overload_dropped_ticks", droppedTicks);
         }
      }

      return false;
   }

   @OnlyIn(Dist.CLIENT)
   private static void spawnHeldLightning(Level level, Player player, boolean mainHand) {
      RandomSource random = level.f_46441_;
      if (random.m_188503_(12) == 0) {
         Vec3 eyePos = player.m_146892_();
         Vec3 look = player.m_20154_().m_82541_();
         Vec3 right = look.m_82537_(new Vec3(0.0, 1.0, 0.0));
         if (right.m_82556_() < 1.0E-4) {
            right = new Vec3(mainHand ? 1.0 : -1.0, 0.0, 0.0);
         } else {
            right = right.m_82541_().m_82490_(mainHand ? 0.28 : -0.28);
         }

         Vec3 handPos = eyePos.m_82549_(look.m_82490_(0.35)).m_82549_(right).m_82520_(0.0, -0.22, 0.0);
         double x = handPos.f_82479_ + (random.m_188500_() - 0.5) * 0.12;
         double y = handPos.f_82480_ + (random.m_188500_() - 0.5) * 0.12;
         double z = handPos.f_82481_ + (random.m_188500_() - 0.5) * 0.12;
         spawnParticle(x, y, z);
      }
   }

   @OnlyIn(Dist.CLIENT)
   private static void spawnDroppedLightning(ItemEntity entity) {
      RandomSource random = entity.m_9236_().f_46441_;
      if (random.m_188503_(12) == 0) {
         double x = entity.m_20185_() + (random.m_188500_() - 0.5) * 0.3;
         double y = entity.m_20186_() + 0.1 + (random.m_188500_() - 0.5) * 0.2;
         double z = entity.m_20189_() + (random.m_188500_() - 0.5) * 0.3;
         spawnParticle(x, y, z);
      }
   }

   @OnlyIn(Dist.CLIENT)
   private static void spawnParticle(double x, double y, double z) {
      Particle particle = Minecraft.m_91087_().f_91061_.m_107370_(ParticleTypes.LIGHTNING, x, y, z, 0.0, 0.0, 0.0);
      if (particle != null) {
         particle.m_107253_(1.0F, 0.95F, 0.45F);
      }
   }
}
