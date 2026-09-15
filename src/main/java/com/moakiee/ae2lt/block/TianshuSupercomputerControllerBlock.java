package com.moakiee.ae2lt.block;

import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import com.moakiee.ae2lt.blockentity.TianshuSupercomputerControllerBlockEntity;
import com.moakiee.ae2lt.logic.persistence.ControllerMachineIdentity;
import com.moakiee.ae2lt.logic.tianshu.TianshuMultiblockUpdateScheduler;
import com.moakiee.ae2lt.menu.TianshuSupercomputerControllerMenu;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;

public class TianshuSupercomputerControllerBlock extends Block implements EntityBlock, WrenchDisassemblableBlock {
   public static final DirectionProperty FACING = HorizontalDirectionalBlock.f_54117_;
   public static final BooleanProperty FORMED = BooleanProperty.m_61465_("formed");
   public static final BooleanProperty WORKING = BooleanProperty.m_61465_("working");

   public TianshuSupercomputerControllerBlock(Properties properties) {
      super(properties);
      this.m_49959_((BlockState)((BlockState)((BlockState)this.m_49966_().m_61124_(FACING, Direction.NORTH)).m_61124_(FORMED, false)).m_61124_(WORKING, false));
   }

   protected void m_7926_(Builder<Block, BlockState> builder) {
      builder.m_61104_(new Property[]{FACING, FORMED, WORKING});
   }

   @Nullable
   public BlockState m_5573_(BlockPlaceContext context) {
      return (BlockState)this.m_49966_().m_61124_(FACING, context.m_8125_());
   }

   public BlockState m_6843_(BlockState state, Rotation rotation) {
      return (BlockState)state.m_61124_(FACING, rotation.m_55954_((Direction)state.m_61143_(FACING)));
   }

   public BlockState m_6943_(BlockState state, Mirror mirror) {
      return state.m_60717_(mirror.m_54846_((Direction)state.m_61143_(FACING)));
   }

   public void m_6402_(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
      super.m_6402_(level, pos, state, placer, stack);
      if (placer != null) {
         level.m_7731_(pos, (BlockState)state.m_61124_(FACING, placer.m_6350_()), 3);
      }

      if (level.m_7702_(pos) instanceof TianshuSupercomputerControllerBlockEntity controller) {
         controller.initializeIdentityFromItem(stack);
      }
   }

   @Nullable
   public BlockEntity m_142194_(BlockPos pos, BlockState state) {
      return new TianshuSupercomputerControllerBlockEntity(pos, state);
   }

   @Nullable
   public <T extends BlockEntity> BlockEntityTicker<T> m_142354_(Level level, BlockState state, BlockEntityType<T> type) {
      return level.f_46443_ ? null : (tickLevel, pos, tickState, be) -> {
         if (be instanceof TianshuSupercomputerControllerBlockEntity controller) {
            TianshuSupercomputerControllerBlockEntity.serverTick(tickLevel, pos, tickState, controller);
         }
      };
   }

   public InteractionResult m_6227_(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
      if (!level.f_46443_ && player instanceof ServerPlayer serverPlayer && level.m_7702_(pos) instanceof TianshuSupercomputerControllerBlockEntity controller) {
         controller.scanNow();
         MenuOpener.open(TianshuSupercomputerControllerMenu.TYPE, serverPlayer, MenuLocators.forBlockEntity(controller));
      }

      return InteractionResult.m_19078_(level.f_46443_);
   }

   public void m_6810_(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
      if (!state.m_60713_(newState.m_60734_()) && level.m_7702_(pos) instanceof TianshuSupercomputerControllerBlockEntity controller) {
         controller.prepareForControllerRemoval();
         controller.clearStructureBindings();
         TianshuMultiblockUpdateScheduler.scheduleNear(level, pos);
      }

      super.m_6810_(state, level, pos, newState, movedByPiston);
   }

   public List<ItemStack> m_49635_(BlockState state, net.minecraft.world.level.storage.loot.LootParams.Builder builder) {
      ArrayList<ItemStack> drops = new ArrayList<>(super.m_49635_(state, builder));
      BlockEntity blockEntity = (BlockEntity)builder.m_287159_(LootContextParams.f_81462_);
      if (blockEntity instanceof TianshuSupercomputerControllerBlockEntity controller) {
         for (ItemStack drop : drops) {
            if (drop.m_150930_(this.m_5456_())) {
               ControllerMachineIdentity.write(drop, controller.getMachineId());
            }
         }
      }

      return drops;
   }

   public ItemStack getCloneItemStack(BlockState state, HitResult target, BlockGetter level, BlockPos pos, Player player) {
      return new ItemStack(this.m_5456_());
   }
}
