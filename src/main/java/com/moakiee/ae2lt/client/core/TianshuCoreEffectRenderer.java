package com.moakiee.ae2lt.client.core;

import com.moakiee.ae2lt.block.TianshuSupercomputerControllerBlock;
import com.moakiee.ae2lt.blockentity.TianshuSupercomputerControllerBlockEntity;
import com.moakiee.ae2lt.config.AE2LTClientConfig;
import com.moakiee.ae2lt.logic.tianshu.TianshuMultiblockScanner;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public final class TianshuCoreEffectRenderer implements BlockEntityRenderer<TianshuSupercomputerControllerBlockEntity> {
   private static final BlockPos CORE_LOCAL = new BlockPos(3, 3, 3);
   private static final CoreEffectPalette CORE_PALETTE = new CoreEffectPalette(0.3F, 0.12F, 0.5F, 0.8F, 0.48F, 1.0F);
   private static final CoreEffectAnimationState.MotionProfile MOTION = new CoreEffectAnimationState.MotionProfile(
      0.18181818181818182, 1.3888888888888888, 18.0, 3.0, 30.0, 360.0, 0.0, 0.0
   );
   private final Map<TianshuSupercomputerControllerBlockEntity, CoreEffectAnimationState> animations = new WeakHashMap<>();

   public TianshuCoreEffectRenderer(Context context) {
   }

   public void render(
      TianshuSupercomputerControllerBlockEntity controller, float partialTick, PoseStack stack, MultiBufferSource buffers, int packedLight, int packedOverlay
   ) {
      BlockState state = controller.m_58900_();
      if (AE2LTClientConfig.useCoreShaderRendering()
         && AE2LTClientConfig.renderMultiblockCoreEffects()
         && state.m_61138_(TianshuSupercomputerControllerBlock.FORMED)
         && (Boolean)state.m_61143_(TianshuSupercomputerControllerBlock.FORMED)
         && controller.m_58904_() != null) {
         Direction facing = (Direction)state.m_61143_(TianshuSupercomputerControllerBlock.FACING);
         BlockPos center = TianshuMultiblockScanner.worldPos(controller.m_58899_(), CORE_LOCAL, facing);
         boolean working = state.m_61138_(TianshuSupercomputerControllerBlock.WORKING) && (Boolean)state.m_61143_(TianshuSupercomputerControllerBlock.WORKING);
         double renderTick = (double)controller.m_58904_().m_46467_() + (double)partialTick;
         CoreEffectAnimationState.Sample animation = this.animations
            .computeIfAbsent(controller, ignored -> new CoreEffectAnimationState())
            .sample(renderTick, working, MOTION);
         stack.m_85836_();
         stack.m_85837_(
            (double)center.m_123341_() + 0.5 - (double)controller.m_58899_().m_123341_(),
            (double)center.m_123342_() + 0.5 - (double)controller.m_58899_().m_123342_(),
            (double)center.m_123343_() + 0.5 - (double)controller.m_58899_().m_123343_()
         );
         CoreEffectGeometry.renderTianshu(stack, buffers, CORE_PALETTE, animation.primaryPhase(), animation.secondaryPhase());
         stack.m_85849_();
      }
   }
}
