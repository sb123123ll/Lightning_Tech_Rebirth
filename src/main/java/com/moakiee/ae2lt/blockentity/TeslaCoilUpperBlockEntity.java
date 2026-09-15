package com.moakiee.ae2lt.blockentity;

import appeng.blockentity.AEBaseBlockEntity;
import com.moakiee.ae2lt.block.TeslaCoilBlock;
import com.moakiee.ae2lt.block.TeslaCoilHalfHelper;
import com.moakiee.ae2lt.registry.ModBlockEntities;
import com.moakiee.ae2lt.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.Nullable;

public class TeslaCoilUpperBlockEntity extends AEBaseBlockEntity {
   public TeslaCoilUpperBlockEntity(BlockPos pos, BlockState state) {
      super((BlockEntityType)ModBlockEntities.TESLA_COIL_UPPER.get(), pos, state);
   }

   public static void ensurePresent(Level level, BlockPos lowerPos) {
      BlockPos upperPos = lowerPos.m_7494_();
      BlockState upperState = level.m_8055_(upperPos);
      if (isUpperTeslaCoilState(upperState)) {
         if (!(level.m_7702_(upperPos) instanceof TeslaCoilUpperBlockEntity)) {
            level.m_151523_(new TeslaCoilUpperBlockEntity(upperPos, upperState));
         }
      }
   }

   public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
      if (capability != ForgeCapabilities.ITEM_HANDLER && capability != ForgeCapabilities.ENERGY) {
         return super.getCapability(capability, side);
      } else {
         TeslaCoilBlockEntity host = this.getHost();
         return host != null ? host.getCapability(capability, side) : LazyOptional.empty();
      }
   }

   protected Item getItemFromBlockEntity() {
      return ((TeslaCoilBlock)ModBlocks.TESLA_COIL.get()).m_5456_();
   }

   @Nullable
   private TeslaCoilBlockEntity getHost() {
      if (this.f_58857_ == null) {
         return null;
      } else {
         BlockPos hostPos = TeslaCoilHalfHelper.getCapabilityHostPos(DoubleBlockHalf.UPPER, this.f_58858_);
         BlockState hostState = this.f_58857_.m_8055_(hostPos);
         if (!isLowerTeslaCoilState(hostState)) {
            return null;
         } else {
            return this.f_58857_.m_7702_(hostPos) instanceof TeslaCoilBlockEntity teslaCoil ? teslaCoil : null;
         }
      }
   }

   private static boolean isUpperTeslaCoilState(BlockState state) {
      return state.m_60734_() instanceof TeslaCoilBlock && state.m_61138_(TeslaCoilBlock.HALF) && state.m_61143_(TeslaCoilBlock.HALF) == DoubleBlockHalf.UPPER;
   }

   private static boolean isLowerTeslaCoilState(BlockState state) {
      return state.m_60734_() instanceof TeslaCoilBlock && state.m_61138_(TeslaCoilBlock.HALF) && state.m_61143_(TeslaCoilBlock.HALF) == DoubleBlockHalf.LOWER;
   }
}
