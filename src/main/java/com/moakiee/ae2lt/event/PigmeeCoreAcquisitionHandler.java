package com.moakiee.ae2lt.event;

import com.moakiee.ae2lt.registry.ModBlocks;
import com.moakiee.ae2lt.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(
   modid = "ae2lt"
)
public final class PigmeeCoreAcquisitionHandler {
   private PigmeeCoreAcquisitionHandler() {
   }

   @SubscribeEvent
   public static void onIncomingDamage(LivingHurtEvent event) {
      if (event.getEntity() instanceof Pig pig
         && !pig.m_6162_()
         && !pig.m_213877_()
         && event.getSource().m_276093_(DamageTypes.f_268526_)
         && isAnvilLandingOnOverloadCrystal(pig)
         && pig.m_9236_() instanceof ServerLevel level) {
         ItemEntity drop = new ItemEntity(
            level, pig.m_20185_(), pig.m_20186_() + (double)pig.m_20206_() * 0.5, pig.m_20189_(), new ItemStack((ItemLike)ModItems.PIGMEE_CORE.get())
         );
         drop.m_32060_();
         if (!level.m_7967_(drop)) {
            return;
         }

         event.setCanceled(true);
         pig.m_146870_();
         level.m_8767_(
            ParticleTypes.f_123759_,
            pig.m_20185_(),
            pig.m_20186_() + (double)pig.m_20206_() * 0.5,
            pig.m_20189_(),
            12,
            (double)pig.m_20205_() * 0.25,
            (double)pig.m_20206_() * 0.25,
            (double)pig.m_20205_() * 0.25,
            0.02
         );
         return;
      }
   }

   private static boolean isAnvilLandingOnOverloadCrystal(Pig pig) {
      return pig.m_20075_().m_60713_((Block)ModBlocks.OVERLOAD_CRYSTAL_BLOCK.get());
   }
}
