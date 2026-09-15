package com.moakiee.ae2lt.client.core;

import com.moakiee.ae2lt.block.MatrixControllerBlock;
import com.moakiee.ae2lt.blockentity.MatrixControllerBlockEntity;
import com.moakiee.ae2lt.config.AE2LTClientConfig;
import com.moakiee.ae2lt.logic.craft.MatrixMultiblockComponent;
import com.moakiee.ae2lt.logic.craft.MatrixMultiblockScanner;
import com.moakiee.ae2lt.logic.craft.MatrixMultiblockTemplate;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public final class MatrixCoreEffectRenderer implements BlockEntityRenderer<MatrixControllerBlockEntity> {
   private static final CoreEffectAnimationState.MotionProfile MOTION = new CoreEffectAnimationState.MotionProfile(
      8.0, 48.0, 36000.0, 22.0, 84.0, 36000.0, 1.2, 4.2
   );
   private final Map<MatrixControllerBlockEntity, CoreEffectAnimationState> animations = new WeakHashMap<>();

   public MatrixCoreEffectRenderer(Context context) {
   }

   public void render(MatrixControllerBlockEntity controller, float partialTick, PoseStack stack, MultiBufferSource buffers, int packedLight, int packedOverlay) {
      BlockState state = controller.m_58900_();
      if (AE2LTClientConfig.useCoreShaderRendering()
         && AE2LTClientConfig.renderMultiblockCoreEffects()
         && state.m_61138_(MatrixControllerBlock.FORMED)
         && (Boolean)state.m_61143_(MatrixControllerBlock.FORMED)
         && controller.m_58904_() != null) {
         Direction facing = controller.getOrientation();
         BlockPos center = MatrixMultiblockScanner.worldPos(controller.m_58899_(), MatrixMultiblockTemplate.CRAFTING_CENTER_LOCAL, facing);
         MatrixMultiblockComponent component = MatrixMultiblockScanner.componentAt(controller.m_58904_(), center);
         CoreEffectPalette palette = palette(component);
         boolean working = state.m_61138_(MatrixControllerBlock.WORKING) && (Boolean)state.m_61143_(MatrixControllerBlock.WORKING);
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
         CoreEffectGeometry.renderMatrix(stack, buffers, palette, animation);
         stack.m_85849_();
      }
   }

   private static CoreEffectPalette palette(MatrixMultiblockComponent component) {
      return switch (component) {
         case QUANTUM_MAIN_CORE -> new CoreEffectPalette(0.22F, 0.62F, 0.78F, 0.58F, 0.86F, 0.92F);
         case OVERLOAD_MAIN_CORE -> new CoreEffectPalette(0.78F, 0.26F, 0.1F, 0.96F, 0.6F, 0.22F);
         case MULTIDIMENSIONAL_MAIN_CORE -> new CoreEffectPalette(0.54F, 0.2F, 0.66F, 0.18F, 0.7F, 0.62F);
         default -> new CoreEffectPalette(0.36F, 0.58F, 0.7F, 0.72F, 0.82F, 0.86F);
      };
   }
}
