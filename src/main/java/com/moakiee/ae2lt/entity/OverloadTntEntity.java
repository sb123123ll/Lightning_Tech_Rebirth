package com.moakiee.ae2lt.entity;

import com.moakiee.ae2lt.config.AE2LTCommonConfig;
import com.moakiee.ae2lt.item.FixedInfiniteCellItem;
import com.moakiee.ae2lt.logic.LightningBlastTask;
import com.moakiee.ae2lt.logic.LightningBlastTaskManager;
import com.moakiee.ae2lt.registry.ModEntities;
import com.moakiee.ae2lt.registry.ModItems;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

public class OverloadTntEntity extends PrimedTnt {
   private static final String TAG_OWNER = "Owner";
   private static final double EASTER_EGG_SCAN_RADIUS = 4.0;
   private static final int EASTER_EGG_LIGHTNING_COUNT = 8;
   private static final double EASTER_EGG_LIGHTNING_SPREAD = 3.5;
   private static final double EASTER_EGG_CELL_DROP_HEIGHT = 8.0;
   @Nullable
   private LivingEntity owner;
   @Nullable
   private UUID ownerUuid;

   public OverloadTntEntity(EntityType<? extends OverloadTntEntity> entityType, Level level) {
      super(entityType, level);
   }

   public OverloadTntEntity(Level level, double x, double y, double z, @Nullable LivingEntity owner) {
      super((EntityType)ModEntities.OVERLOAD_TNT.get(), level);
      this.m_6034_(x, y, z);
      this.m_32085_(80);
      this.owner = owner;
      this.ownerUuid = owner != null ? owner.m_20148_() : null;
      double angle = level.f_46441_.m_188500_() * (Math.PI * 2);
      this.m_20334_(-Math.sin(angle) * 0.02, 0.2, -Math.cos(angle) * 0.02);
      this.f_19854_ = x;
      this.f_19855_ = y;
      this.f_19856_ = z;
   }

   @Nullable
   public LivingEntity m_19749_() {
      if (this.owner == null
         && this.ownerUuid != null
         && this.m_9236_() instanceof ServerLevel serverLevel
         && serverLevel.m_8791_(this.ownerUuid) instanceof LivingEntity livingEntity) {
         this.owner = livingEntity;
      }

      return this.owner != null ? this.owner : super.m_19749_();
   }

   protected void m_7380_(CompoundTag tag) {
      super.m_7380_(tag);
      if (this.ownerUuid != null) {
         tag.m_128362_("Owner", this.ownerUuid);
      }
   }

   protected void m_7378_(CompoundTag tag) {
      super.m_7378_(tag);
      this.owner = null;
      this.ownerUuid = tag.m_128403_("Owner") ? tag.m_128342_("Owner") : null;
   }

   protected void m_32103_() {
      if (this.m_9236_() instanceof ServerLevel serverLevel) {
         if (AE2LTCommonConfig.easterEggEnabled() && this.tryTriggerEasterEgg(serverLevel)) {
            return;
         }

         if (AE2LTCommonConfig.overloadTntEnableTerrainDamage()) {
            LightningBlastTaskManager.schedule(new LightningBlastTask(serverLevel, this.m_20183_()));
         }
      }
   }

   private boolean tryTriggerEasterEgg(ServerLevel serverLevel) {
      AABB scanBox = AABB.m_165882_(this.m_20182_(), 8.0, 8.0, 8.0);
      Item easterEggItem = configuredEasterEggItem();
      if (easterEggItem == null) {
         return false;
      } else {
         List<ItemEntity> triggerItems = serverLevel.m_6443_(ItemEntity.class, scanBox, e -> e.m_6084_() && e.m_32055_().m_150930_(easterEggItem));
         if (triggerItems.isEmpty()) {
            return false;
         } else {
            ItemEntity triggerEntity = triggerItems.get(0);
            ItemStack triggerStack = triggerEntity.m_32055_();
            triggerStack.m_41774_(1);
            if (triggerStack.m_41619_()) {
               triggerEntity.m_146870_();
            } else {
               triggerEntity.m_32045_(triggerStack);
            }

            for (int i = 0; i < 8; i++) {
               LightningBolt bolt = (LightningBolt)EntityType.f_20465_.m_20615_(serverLevel);
               if (bolt != null) {
                  double angle = serverLevel.f_46441_.m_188500_() * Math.PI * 2.0;
                  double dist = serverLevel.f_46441_.m_188500_() * 3.5;
                  bolt.m_6027_(this.m_20185_() + Math.cos(angle) * dist, this.m_20186_(), this.m_20189_() + Math.sin(angle) * dist);
                  bolt.m_20874_(true);
                  serverLevel.m_7967_(bolt);
               }
            }

            ItemStack cellStack = new ItemStack((ItemLike)ModItems.MYSTERIOUS_CELL.get());
            FixedInfiniteCellItem.initializeOuterCell(cellStack);
            ItemEntity cellEntity = new ItemEntity(serverLevel, this.m_20185_(), this.m_20186_() + 8.0, this.m_20189_(), cellStack);
            cellEntity.m_20334_(0.0, -0.1, 0.0);
            serverLevel.m_7967_(cellEntity);
            return true;
         }
      }
   }

   @Nullable
   private static Item configuredEasterEggItem() {
      ResourceLocation id = ResourceLocation.m_135820_(AE2LTCommonConfig.easterEggItem());
      return id == null ? null : (Item)BuiltInRegistries.f_257033_.m_6612_(id).orElse(null);
   }
}
