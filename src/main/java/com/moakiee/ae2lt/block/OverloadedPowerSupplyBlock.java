package com.moakiee.ae2lt.block;

import appeng.api.orientation.IOrientationStrategy;
import appeng.api.orientation.OrientationStrategies;
import appeng.menu.locator.MenuLocators;
import appeng.util.InteractionUtil;
import com.moakiee.ae2lt.blockentity.OverloadedPowerSupplyBlockEntity;
import java.util.EnumMap;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class OverloadedPowerSupplyBlock extends AE2LTBaseEntityBlock<OverloadedPowerSupplyBlockEntity> {
   public static final BooleanProperty POWERED = BooleanProperty.m_61465_("powered");
   public static final BooleanProperty OVERLOADED = BooleanProperty.m_61465_("overloaded");
   public static final DirectionProperty FACING = BlockStateProperties.f_61372_;
   private static final ResourceLocation BLOCK_ITEM_DROP = new ResourceLocation("ae2lt", "overloaded_power_supply");
   private static final VoxelShape UP_SHAPE = BlockShapeHelper.or(
      Block.m_49796_(2.0, 0.0, 2.0, 14.0, 7.0, 14.0), Block.m_49796_(6.0, 7.0, 6.0, 10.0, 14.0, 10.0), Block.m_49796_(7.0, 10.0, 7.0, 9.0, 16.0, 9.0)
   );
   private static final EnumMap<Direction, VoxelShape> SHAPES = BlockShapeHelper.createAllFacingShapes(UP_SHAPE);

   public OverloadedPowerSupplyBlock() {
      super(metalProps().m_60955_().m_280606_());
      this.m_49959_((BlockState)((BlockState)((BlockState)this.m_49966_().m_61124_(POWERED, false)).m_61124_(OVERLOADED, false)).m_61124_(FACING, Direction.UP));
   }

   protected void m_7926_(Builder<Block, BlockState> builder) {
      super.m_7926_(builder);
      builder.m_61104_(new Property[]{POWERED, OVERLOADED});
   }

   public IOrientationStrategy getOrientationStrategy() {
      return OrientationStrategies.facing();
   }

   public VoxelShape m_5940_(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return SHAPES.get(state.m_61143_(FACING));
   }

   public VoxelShape m_5939_(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return SHAPES.get(state.m_61143_(FACING));
   }

   public VoxelShape m_7952_(BlockState state, BlockGetter level, BlockPos pos) {
      return Shapes.m_83040_();
   }

   public boolean m_7420_(BlockState state, BlockGetter level, BlockPos pos) {
      return true;
   }

   public List<ItemStack> m_49635_(BlockState state, net.minecraft.world.level.storage.loot.LootParams.Builder builder) {
      builder.m_287145_(BLOCK_ITEM_DROP, output -> output.accept(new ItemStack(this)));
      return super.m_49635_(state, builder);
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
      if (InteractionUtil.isInAlternateUseMode(player)) {
         return InteractionResult.PASS;
      } else {
         OverloadedPowerSupplyBlockEntity be = (OverloadedPowerSupplyBlockEntity)this.getBlockEntity(level, pos);
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
