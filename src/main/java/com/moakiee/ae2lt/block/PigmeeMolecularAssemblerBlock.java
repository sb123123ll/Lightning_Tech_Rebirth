package com.moakiee.ae2lt.block;

import appeng.block.AEBaseEntityBlock;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import com.moakiee.ae2lt.blockentity.PigmeeMolecularAssemblerBlockEntity;
import com.moakiee.ae2lt.menu.PigmeeMolecularAssemblerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;

public final class PigmeeMolecularAssemblerBlock extends AEBaseEntityBlock<PigmeeMolecularAssemblerBlockEntity> {
   public static final BooleanProperty POWERED = BooleanProperty.m_61465_("powered");

   public PigmeeMolecularAssemblerBlock() {
      super(metalProps().m_60955_());
      this.m_49959_((BlockState)this.m_49966_().m_61124_(POWERED, false));
   }

   protected void m_7926_(Builder<Block, BlockState> builder) {
      super.m_7926_(builder);
      builder.m_61104_(new Property[]{POWERED});
   }

   protected BlockState updateBlockStateFromBlockEntity(BlockState currentState, PigmeeMolecularAssemblerBlockEntity blockEntity) {
      return (BlockState)currentState.m_61124_(POWERED, blockEntity.isPowered());
   }

   public InteractionResult m_6227_(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
      PigmeeMolecularAssemblerBlockEntity blockEntity = (PigmeeMolecularAssemblerBlockEntity)this.getBlockEntity(level, pos);
      if (blockEntity == null) {
         return InteractionResult.PASS;
      } else {
         if (!level.m_5776_()) {
            MenuOpener.open(PigmeeMolecularAssemblerMenu.TYPE, player, MenuLocators.forBlockEntity(blockEntity));
         }

         return InteractionResult.m_19078_(level.m_5776_());
      }
   }
}
