package com.moakiee.ae2lt.blockentity;

import com.moakiee.ae2lt.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class FumoBlockEntity extends BlockEntity {
   public static final float SPIN_DEGREES_PER_TICK = 6.0F;
   private static final String TAG_SPINNING = "Spinning";
   private boolean spinning;
   private float yRot;
   private float prevYRot;

   public FumoBlockEntity(BlockPos pos, BlockState state) {
      super((BlockEntityType)ModBlockEntities.FUMO.get(), pos, state);
   }

   public boolean isSpinning() {
      return this.spinning;
   }

   public float getRenderYRot(float partialTick) {
      return this.prevYRot + (this.yRot - this.prevYRot) * partialTick;
   }

   public void toggleSpinning() {
      this.spinning = !this.spinning;
      this.m_6596_();
      if (this.f_58857_ != null && !this.f_58857_.m_5776_()) {
         BlockState state = this.m_58900_();
         this.f_58857_.m_7260_(this.f_58858_, state, state, 2);
      }
   }

   public static void clientTick(Level level, BlockPos pos, BlockState state, FumoBlockEntity be) {
      be.prevYRot = be.yRot;
      if (be.spinning) {
         be.yRot += 6.0F;
      }
   }

   protected void m_183515_(CompoundTag tag) {
      super.m_183515_(tag);
      tag.m_128379_("Spinning", this.spinning);
   }

   public void m_142466_(CompoundTag tag) {
      super.m_142466_(tag);
      this.spinning = tag.m_128471_("Spinning");
   }

   public CompoundTag m_5995_() {
      CompoundTag tag = super.m_5995_();
      tag.m_128379_("Spinning", this.spinning);
      return tag;
   }

   public Packet<ClientGamePacketListener> m_58483_() {
      return ClientboundBlockEntityDataPacket.m_195640_(this);
   }
}
