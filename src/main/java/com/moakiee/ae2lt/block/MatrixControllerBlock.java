package com.moakiee.ae2lt.block;

import com.moakiee.ae2lt.blockentity.MatrixControllerBlockEntity;
import com.moakiee.ae2lt.logic.craft.MatrixMultiblockComponent;
import com.moakiee.ae2lt.logic.persistence.ControllerMachineIdentity;
import com.moakiee.ae2lt.menu.MatrixControllerMenu;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

public class MatrixControllerBlock extends MatrixMultiblockDirectionalBlock implements EntityBlock {
   public static final BooleanProperty FORMED = BooleanProperty.m_61465_("formed");
   public static final BooleanProperty WORKING = BooleanProperty.m_61465_("working");

   public MatrixControllerBlock(Properties properties) {
      super(properties, MatrixMultiblockComponent.MATRIX_CONTROLLER);
      this.m_49959_((BlockState)((BlockState)this.m_49966_().m_61124_(FORMED, Boolean.FALSE)).m_61124_(WORKING, Boolean.FALSE));
   }

   @Override
   protected void m_7926_(Builder<Block, BlockState> builder) {
      super.m_7926_(builder);
      builder.m_61104_(new Property[]{FORMED, WORKING});
   }

   @Nullable
   public BlockEntity m_142194_(BlockPos pos, BlockState state) {
      return new MatrixControllerBlockEntity(pos, state);
   }

   @Nullable
   public <T extends BlockEntity> BlockEntityTicker<T> m_142354_(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
      return level.f_46443_ ? null : (tickLevel, pos, tickState, blockEntity) -> {
         if (blockEntity instanceof MatrixControllerBlockEntity controller) {
            MatrixControllerBlockEntity.serverTick(tickLevel, pos, tickState, controller);
         }
      };
   }

   public void m_6402_(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
      super.m_6402_(level, pos, state, placer, stack);
      if (level.m_7702_(pos) instanceof MatrixControllerBlockEntity controller) {
         controller.initializeIdentityFromItem(stack);
      }
   }

   @Override
   public void m_6810_(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
      if (!state.m_60713_(newState.m_60734_()) && level.m_7702_(pos) instanceof MatrixControllerBlockEntity controller) {
         controller.prepareForControllerRemoval();
         controller.clearStructureBindings();
      }

      super.m_6810_(state, level, pos, newState, movedByPiston);
   }

   public InteractionResult m_6227_(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
      if (level.m_7702_(pos) instanceof MatrixControllerBlockEntity be) {
         if (!level.f_46443_ && player instanceof ServerPlayer serverPlayer) {
            NetworkHooks.openScreen(
               serverPlayer,
               new SimpleMenuProvider((id, inv, p) -> new MatrixControllerMenu(id, inv, be), state.m_60734_().m_49954_()),
               buf -> MatrixControllerMenu.writeExtraData(buf, be)
            );
         }

         return InteractionResult.m_19078_(level.f_46443_);
      } else {
         return InteractionResult.PASS;
      }
   }

   public List<ItemStack> m_49635_(BlockState state, net.minecraft.world.level.storage.loot.LootParams.Builder builder) {
      ArrayList<ItemStack> drops = new ArrayList<>(super.m_49635_(state, builder));
      BlockEntity blockEntity = (BlockEntity)builder.m_287159_(LootContextParams.f_81462_);
      if (blockEntity instanceof MatrixControllerBlockEntity controller) {
         for (ItemStack drop : drops) {
            if (drop.m_150930_(this.m_5456_())) {
               ControllerMachineIdentity.write(drop, controller.getMachineId());
            }
         }
      }

      return drops;
   }

   public ItemStack m_7397_(BlockGetter level, BlockPos pos, BlockState state) {
      return new ItemStack(this.m_5456_());
   }
}
