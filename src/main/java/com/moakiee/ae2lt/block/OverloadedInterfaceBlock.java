package com.moakiee.ae2lt.block;

import appeng.menu.locator.MenuLocators;
import appeng.util.InteractionUtil;
import com.moakiee.ae2lt.blockentity.OverloadedInterfaceBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class OverloadedInterfaceBlock extends AE2LTBaseEntityBlock<OverloadedInterfaceBlockEntity> {
   public OverloadedInterfaceBlock() {
      super(metalProps().m_280606_());
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
      if (InteractionUtil.isInAlternateUseMode(player)) {
         return InteractionResult.PASS;
      } else {
         OverloadedInterfaceBlockEntity be = (OverloadedInterfaceBlockEntity)this.getBlockEntity(level, pos);
         if (be != null) {
            if (!level.m_5776_()) {
               be.openMenu(player, MenuLocators.forBlockEntity(be));
            }

            return InteractionResult.m_19078_(level.m_5776_());
         } else {
            return InteractionResult.PASS;
         }
      }
   }
}
