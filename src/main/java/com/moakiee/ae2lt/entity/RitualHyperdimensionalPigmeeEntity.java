package com.moakiee.ae2lt.entity;

import com.moakiee.ae2lt.network.NetworkHandler;
import com.moakiee.ae2lt.network.RitualItemBurstPacket;
import com.moakiee.ae2lt.registry.ModEntities;
import net.minecraft.advancements.Advancement;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class RitualHyperdimensionalPigmeeEntity extends ItemEntity {
   private static final ResourceLocation PICKUP_ADVANCEMENT = new ResourceLocation("ae2lt", "main/hyperdimensional_pigmee");
   private static final String PICKUP_CRITERION = "claim_ritual_pigmee";
   private static final String TAG_CEREMONY_END = "CeremonyEndGameTime";
   private static final EntityDataAccessor<Long> DATA_CEREMONY_END = SynchedEntityData.m_135353_(
      RitualHyperdimensionalPigmeeEntity.class, EntityDataSerializers.f_244073_
   );
   public static final int CEREMONY_TICKS = 100;
   private static final int FIRST_BURST_REMAINING = 80;
   private static final int SECOND_BURST_REMAINING = 50;
   private static final int THIRD_BURST_REMAINING = 20;
   private static final float LARGE_SCALE = 3.0F;

   public RitualHyperdimensionalPigmeeEntity(EntityType<? extends RitualHyperdimensionalPigmeeEntity> type, Level level) {
      super(type, level);
   }

   public RitualHyperdimensionalPigmeeEntity(Level level, double x, double y, double z, ItemStack stack) {
      super((EntityType)ModEntities.RITUAL_HYPERDIMENSIONAL_PIGMEE.get(), level);
      this.m_6034_(x, y, z);
      this.m_32045_(stack);
      this.m_20256_(Vec3.f_82478_);
      this.m_20242_(true);
      this.m_32010_(100);
      this.f_19804_.m_135381_(DATA_CEREMONY_END, level.m_46467_() + 100L);
   }

   protected void m_8097_() {
      super.m_8097_();
      this.f_19804_.m_135372_(DATA_CEREMONY_END, 0L);
   }

   public void m_8119_() {
      boolean ceremonyActive = this.getCeremonyTicksRemaining(0.0F) > 0.0F;
      this.m_20242_(ceremonyActive);
      if (ceremonyActive) {
         this.m_20256_(Vec3.f_82478_);
      }

      super.m_8119_();
      if (!this.m_9236_().m_5776_()) {
         long remaining = (Long)this.f_19804_.m_135370_(DATA_CEREMONY_END) - this.m_9236_().m_46467_();
         if (remaining == 80L) {
            this.broadcastItemBurst((byte)0);
         } else if (remaining == 50L) {
            this.broadcastItemBurst((byte)1);
         } else if (remaining == 20L) {
            this.broadcastItemBurst((byte)2);
         } else if ((Long)this.f_19804_.m_135370_(DATA_CEREMONY_END) != 0L && remaining <= 0L) {
            this.f_19804_.m_135381_(DATA_CEREMONY_END, 0L);
            this.m_20242_(false);
            this.m_32061_();
         }
      }
   }

   public float getCeremonyScale(float partialTick) {
      float remaining = this.getCeremonyTicksRemaining(partialTick);
      if (remaining > 20.0F) {
         return 3.0F;
      } else {
         return remaining <= 0.0F ? 1.0F : Mth.m_14179_(1.0F - remaining / 20.0F, 3.0F, 1.0F);
      }
   }

   public void m_6123_(Player player) {
      if (!(this.getCeremonyTicksRemaining(0.0F) > 0.0F)) {
         int countBefore = this.m_32055_().m_41613_();
         super.m_6123_(player);
         if (player instanceof ServerPlayer serverPlayer && (this.m_213877_() || this.m_32055_().m_41613_() < countBefore)) {
            awardPickupAdvancement(serverPlayer);
         }
      }
   }

   public void m_7380_(CompoundTag tag) {
      super.m_7380_(tag);
      long ceremonyEnd = (Long)this.f_19804_.m_135370_(DATA_CEREMONY_END);
      if (ceremonyEnd != 0L) {
         tag.m_128356_("CeremonyEndGameTime", ceremonyEnd);
      }
   }

   public void m_7378_(CompoundTag tag) {
      super.m_7378_(tag);
      this.f_19804_.m_135381_(DATA_CEREMONY_END, tag.m_128454_("CeremonyEndGameTime"));
   }

   private float getCeremonyTicksRemaining(float partialTick) {
      long ceremonyEnd = (Long)this.f_19804_.m_135370_(DATA_CEREMONY_END);
      return ceremonyEnd == 0L ? 0.0F : Math.max(0.0F, (float)ceremonyEnd - ((float)this.m_9236_().m_46467_() + partialTick));
   }

   private void broadcastItemBurst(byte stage) {
      if (this.m_9236_() instanceof ServerLevel serverLevel) {
         NetworkHandler.sendToTrackingChunk(serverLevel, this.m_146902_(), new RitualItemBurstPacket(this.m_19879_(), stage));
      }
   }

   private static void awardPickupAdvancement(ServerPlayer player) {
      Advancement advancement = player.f_8924_.m_129889_().m_136041_(PICKUP_ADVANCEMENT);
      if (advancement != null) {
         player.m_8960_().m_135988_(advancement, "claim_ritual_pigmee");
      }
   }
}
