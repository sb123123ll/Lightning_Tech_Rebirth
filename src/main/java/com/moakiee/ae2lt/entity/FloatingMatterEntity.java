package com.moakiee.ae2lt.entity;

import com.moakiee.ae2lt.config.AE2LTCommonConfig;
import com.moakiee.ae2lt.registry.ModEntities;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class FloatingMatterEntity extends ItemEntity {
   public FloatingMatterEntity(EntityType<? extends FloatingMatterEntity> type, Level level) {
      super(type, level);
   }

   public FloatingMatterEntity(Level level, double x, double y, double z, ItemStack stack) {
      super((EntityType)ModEntities.FLOATING_MATTER.get(), level);
      this.m_6034_(x, y, z);
      this.m_32045_(stack);
      this.m_20256_(Vec3.f_82478_);
   }

   public void m_8119_() {
      this.m_20242_(true);
      Vec3 motion = this.m_20184_();
      this.m_20334_(motion.f_82479_ * 0.98, AE2LTCommonConfig.floatingMatterRiseSpeed(), motion.f_82481_ * 0.98);
      super.m_8119_();
      if (!this.m_9236_().m_5776_()) {
         double ceiling = (double)this.m_9236_().m_151558_() * AE2LTCommonConfig.floatingMatterDespawnHeightMultiplier();
         if (this.m_20186_() > ceiling) {
            this.m_146870_();
         }
      }
   }
}
