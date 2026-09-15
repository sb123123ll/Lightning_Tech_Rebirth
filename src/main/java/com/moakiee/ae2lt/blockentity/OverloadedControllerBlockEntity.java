package com.moakiee.ae2lt.blockentity;

import appeng.api.config.Actionable;
import appeng.api.networking.IManagedGridNode;
import appeng.api.util.AECableType;
import appeng.blockentity.networking.ControllerBlockEntity;
import appeng.helpers.ForgeEnergyAdapter;
import com.moakiee.ae2lt.block.OverloadedControllerBlock;
import com.moakiee.ae2lt.config.AE2LTCommonConfig;
import com.moakiee.ae2lt.logic.PassiveAeCharger;
import com.moakiee.ae2lt.registry.ModBlockEntities;
import com.moakiee.ae2lt.registry.ModBlocks;
import com.moakiee.thunderbolt.api.channel.HighCapacityChannelOwner;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

public class OverloadedControllerBlockEntity extends ControllerBlockEntity implements HighCapacityChannelOwner, PassiveAeCharger.Storage {
   private static final double INTERNAL_MAX_POWER = 1.6E7;

   public OverloadedControllerBlockEntity(BlockPos pos, BlockState blockState) {
      this((BlockEntityType<?>)ModBlockEntities.OVERLOADED_CONTROLLER.get(), pos, blockState);
   }

   protected OverloadedControllerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
      super(type, pos, blockState);
      this.setInternalMaxPower(1.6E7);
   }

   public static void serverTick(Level level, BlockPos pos, BlockState state, OverloadedControllerBlockEntity be) {
      if (!level.f_46443_) {
         be.injectAEPower(AE2LTCommonConfig.overloadedControllerPassiveAePerTick(), Actionable.MODULATE);
      }
   }

   public IEnergyStorage getEnergyStorageCapability(Direction side) {
      return new ForgeEnergyAdapter(this);
   }

   protected IManagedGridNode createMainNode() {
      return super.createMainNode().setTagName("overloaded_controller").setVisualRepresentation((ItemLike)ModBlocks.OVERLOADED_CONTROLLER.get());
   }

   public AECableType getCableConnectionType(Direction dir) {
      return AECableType.DENSE_SMART;
   }

   public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
      return cap == ForgeCapabilities.ENERGY ? LazyOptional.of(() -> this.getEnergyStorageCapability(side)).cast() : super.getCapability(cap, side);
   }

   protected Item getItemFromBlockEntity() {
      return ((OverloadedControllerBlock)ModBlocks.OVERLOADED_CONTROLLER.get()).m_5456_();
   }
}
