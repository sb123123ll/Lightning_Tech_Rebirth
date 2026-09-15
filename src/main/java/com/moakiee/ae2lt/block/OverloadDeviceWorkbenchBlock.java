package com.moakiee.ae2lt.block;

import appeng.block.AEBaseEntityBlock;
import appeng.menu.locator.MenuLocators;
import com.moakiee.ae2lt.blockentity.OverloadDeviceWorkbenchBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class OverloadDeviceWorkbenchBlock extends AEBaseEntityBlock<OverloadDeviceWorkbenchBlockEntity> {
   public OverloadDeviceWorkbenchBlock() {
      super(metalProps().m_60955_().m_280606_());
   }

   public InteractionResult m_6227_(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
      OverloadDeviceWorkbenchBlockEntity blockEntity = (OverloadDeviceWorkbenchBlockEntity)this.getBlockEntity(level, pos);
      if (blockEntity != null) {
         if (!level.m_5776_()) {
            blockEntity.openMenu(player, MenuLocators.forBlockEntity(blockEntity));
         }

         return InteractionResult.m_19078_(level.m_5776_());
      } else {
         return InteractionResult.PASS;
      }
   }
}
