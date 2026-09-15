package com.moakiee.ae2lt.blockentity;

import appeng.api.networking.IManagedGridNode;
import com.moakiee.ae2lt.block.AdvancedWirelessOverloadedControllerBlock;
import com.moakiee.ae2lt.registry.ModBlockEntities;
import com.moakiee.ae2lt.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class AdvancedWirelessOverloadedControllerBlockEntity extends WirelessOverloadedControllerBlockEntity {
   public AdvancedWirelessOverloadedControllerBlockEntity(BlockPos pos, BlockState blockState) {
      super((BlockEntityType<?>)ModBlockEntities.ADVANCED_WIRELESS_OVERLOADED_CONTROLLER.get(), pos, blockState);
   }

   @Override
   public boolean isAdvanced() {
      return true;
   }

   @Override
   protected IManagedGridNode createMainNode() {
      return super.createMainNode()
         .setTagName("advanced_wireless_overloaded_controller")
         .setVisualRepresentation((ItemLike)ModBlocks.ADVANCED_WIRELESS_OVERLOADED_CONTROLLER.get());
   }

   @Override
   protected Item getItemFromBlockEntity() {
      return ((AdvancedWirelessOverloadedControllerBlock)ModBlocks.ADVANCED_WIRELESS_OVERLOADED_CONTROLLER.get()).m_5456_();
   }

   public static void advancedWirelessServerTick(Level level, BlockPos pos, BlockState state, AdvancedWirelessOverloadedControllerBlockEntity be) {
      OverloadedControllerBlockEntity.serverTick(level, pos, state, be);
   }
}
