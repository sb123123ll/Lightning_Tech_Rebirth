package com.moakiee.ae2lt.block;

import appeng.api.orientation.IOrientationStrategy;
import appeng.api.orientation.OrientationStrategies;
import appeng.menu.locator.MenuLocators;
import appeng.util.InteractionUtil;
import com.moakiee.ae2lt.blockentity.OverloadProcessingFactoryBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.SoundActions;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;

public class OverloadProcessingFactoryBlock extends AE2LTBaseEntityBlock<OverloadProcessingFactoryBlockEntity> {
   public static final BooleanProperty WORKING = BooleanProperty.m_61465_("working");

   public OverloadProcessingFactoryBlock() {
      super(metalProps().m_60955_().m_280606_());
      this.m_49959_((BlockState)((BlockState)this.m_49966_().m_61124_(WORKING, false)).m_61124_(BlockStateProperties.f_61374_, Direction.NORTH));
   }

   protected void m_7926_(Builder<Block, BlockState> builder) {
      super.m_7926_(builder);
      builder.m_61104_(new Property[]{WORKING});
   }

   public IOrientationStrategy getOrientationStrategy() {
      return OrientationStrategies.horizontalFacing();
   }

   public void m_6861_(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
      OverloadProcessingFactoryBlockEntity be = (OverloadProcessingFactoryBlockEntity)this.getBlockEntity(level, pos);
      if (be != null) {
         be.onNeighborChanged(fromPos);
      }
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
      if (InteractionUtil.isInAlternateUseMode(player)) {
         return InteractionResult.PASS;
      } else {
         OverloadProcessingFactoryBlockEntity be = (OverloadProcessingFactoryBlockEntity)this.getBlockEntity(level, pos);
         if (be == null) {
            return InteractionResult.PASS;
         } else {
            if (!level.m_5776_()) {
               be.openMenu(player, MenuLocators.forBlockEntity(be));
            }

            return InteractionResult.m_19078_(level.m_5776_());
         }
      }
   }

   @Override
   protected InteractionResult useItemOn(
      ItemStack heldItem, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit
   ) {
      return heldItem.m_41720_() instanceof BucketItem && this.useBucket(player, level, pos, heldItem, hand)
         ? InteractionResult.m_19078_(level.m_5776_())
         : super.useItemOn(heldItem, state, level, pos, player, hand, hit);
   }

   private boolean useBucket(Player player, Level level, BlockPos pos, ItemStack stack, InteractionHand hand) {
      IFluidHandlerItem itemFluid = (IFluidHandlerItem)stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).orElse(null);
      BlockEntity blockEntity = level.m_7702_(pos);
      IFluidHandler blockFluid = blockEntity != null ? (IFluidHandler)blockEntity.getCapability(ForgeCapabilities.FLUID_HANDLER).orElse(null) : null;
      if (itemFluid == null || blockFluid == null) {
         return false;
      } else if (itemFluid.getFluidInTank(0).isEmpty()) {
         FluidStack extracted = blockFluid.drain(1000, FluidAction.SIMULATE);
         if (!extracted.isEmpty() && extracted.getAmount() == 1000) {
            blockFluid.drain(1000, FluidAction.EXECUTE);
            if (itemFluid.getContainer().m_41613_() == 1) {
               itemFluid.fill(extracted, FluidAction.EXECUTE);
               player.m_21008_(hand, itemFluid.getContainer());
            } else {
               ItemStack newBucket = new ItemStack(Items.f_42446_, 1);
               IFluidHandlerItem newBucketFluid = (IFluidHandlerItem)newBucket.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).orElse(null);
               if (newBucketFluid == null) {
                  return false;
               }

               newBucketFluid.fill(extracted, FluidAction.EXECUTE);
               player.m_21008_(hand, newBucketFluid.getContainer());
               player.m_36356_(new ItemStack(stack.m_41720_(), stack.m_41613_() - 1));
            }

            this.playBucketSound(player, level, pos, extracted, true);
            return true;
         } else {
            return false;
         }
      } else {
         FluidStack drained = itemFluid.drain(1000, FluidAction.SIMULATE);
         if (drained.isEmpty()) {
            return false;
         } else {
            int inserted = blockFluid.fill(drained, FluidAction.SIMULATE);
            if (inserted != 1000) {
               return false;
            } else {
               drained = itemFluid.drain(1000, FluidAction.EXECUTE);
               blockFluid.fill(drained, FluidAction.EXECUTE);
               player.m_21008_(hand, itemFluid.getContainer());
               this.playBucketSound(player, level, pos, drained, false);
               return true;
            }
         }
      }
   }

   private void playBucketSound(Player player, Level level, BlockPos pos, FluidStack fluid, boolean fillBucket) {
      SoundEvent sound = fluid.getFluid().getFluidType().getSound(player, level, pos, fillBucket ? SoundActions.BUCKET_FILL : SoundActions.BUCKET_EMPTY);
      if (sound == null) {
         sound = fillBucket
            ? (fluid.getFluid().m_205067_(FluidTags.f_13132_) ? SoundEvents.f_11783_ : SoundEvents.f_11781_)
            : (fluid.getFluid().m_205067_(FluidTags.f_13132_) ? SoundEvents.f_11780_ : SoundEvents.f_11778_);
      }

      level.m_5594_(player, pos, sound, SoundSource.BLOCKS, 1.0F, 1.0F);
   }
}
