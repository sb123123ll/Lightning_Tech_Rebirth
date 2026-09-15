package com.moakiee.ae2lt.client.core;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;

final class CoreEffectGeometry {
   private CoreEffectGeometry() {
   }

   static void renderTianshu(PoseStack stack, MultiBufferSource buffers, CoreEffectPalette palette, double stepPhase, double spinDegrees) {
      boolean shaderPackActive = CoreEffectBackend.useShaderPackFallback();
      VertexConsumer consumer = buffers.m_6299_(CoreEffectRenderTypes.tianshu(shaderPackActive));
      stack.m_85836_();
      stack.m_85841_(1.8F, 1.8F, 1.8F);
      renderCubeCore(stack, consumer, palette, stepPhase, spinDegrees);
      stack.m_85849_();
   }

   static void renderMatrix(PoseStack stack, MultiBufferSource buffers, CoreEffectPalette palette, CoreEffectAnimationState.Sample animation) {
      boolean shaderPackActive = CoreEffectBackend.useShaderPackFallback();
      VertexConsumer coreConsumer = buffers.m_6299_(CoreEffectRenderTypes.matrixCore(shaderPackActive));
      float activity = (float)animation.activity();
      float ambientTime = (float)animation.ambientTime();
      float corePhase = (float)animation.primaryPhase();
      float ringPhase = (float)animation.secondaryPhase();
      float contraction = 1.0F - activity * (0.07F + 0.02F * (float)Math.sin((double)(ambientTime * 4.5F)));
      float pulse = 1.0F + activity * 0.035F * (float)Math.sin((double)(ambientTime * 6.0F));
      float glowPulse = 0.94F + 0.06F * (float)Math.sin(animation.glowPhase());
      float coreBrightness = shaderPackActive ? 0.42F : 0.18F;
      float primaryR = brighten(palette.primaryR(), 0.34F);
      float primaryG = brighten(palette.primaryG(), 0.34F);
      float primaryB = brighten(palette.primaryB(), 0.34F);
      float accentR = brighten(palette.accentR(), 0.22F);
      float accentG = brighten(palette.accentG(), 0.22F);
      float accentB = brighten(palette.accentB(), 0.22F);
      stack.m_85836_();
      stack.m_85841_(1.5F, 1.5F, 1.5F);
      stack.m_85836_();
      stack.m_252781_(Axis.f_252436_.m_252977_(corePhase * 0.42F));
      stack.m_252781_(Axis.f_252529_.m_252977_(corePhase * -0.27F));
      CoreEffectMesh.sphere(
         stack,
         coreConsumer,
         0.72F * pulse,
         palette.primaryR() * coreBrightness,
         palette.primaryG() * coreBrightness,
         palette.primaryB() * coreBrightness,
         0.98F
      );
      stack.m_85849_();
      VertexConsumer glowConsumer = buffers.m_6299_(CoreEffectRenderTypes.matrixGlow(shaderPackActive));
      float ringRadius = 1.22F * contraction;
      float constraintRadius = 1.12F * contraction;
      float diskAlpha = lerp(0.22F, 0.34F, activity) * glowPulse;
      float innerYaw = ringPhase * 0.55F;
      renderMatrixRing(stack, glowConsumer, ringRadius, 0.055F, 0.006F, innerYaw, 10.0F, -6.0F, primaryR, primaryG, primaryB, diskAlpha);
      renderMatrixRing(stack, glowConsumer, ringRadius, 0.008F, 0.011F, innerYaw, 10.0F, -6.0F, accentR, accentG, accentB, diskAlpha * 1.65F);
      float ringAlpha = lerp(0.42F, 0.66F, activity) * glowPulse;
      float middleYaw = -ringPhase * 0.82F;
      float outerYaw = ringPhase * 0.68F;
      renderMatrixRing(stack, glowConsumer, constraintRadius, 0.018F, 0.014F, middleYaw, 61.0F, 24.0F, accentR, accentG, accentB, ringAlpha);
      renderMatrixRing(stack, glowConsumer, constraintRadius, 0.016F, 0.011F, outerYaw, 118.0F, -20.0F, primaryR, primaryG, primaryB, ringAlpha * 0.82F);
      float nodePulse = 1.0F + lerp(0.06F, 0.16F, activity) * (float)Math.sin((double)(ambientTime * 5.5F));
      renderOrbitNodes(
         stack, glowConsumer, constraintRadius, middleYaw, 61.0F, 24.0F, ringPhase * 1.65F, 2, 0.074F * nodePulse, 1.0F, 0.64F, 0.16F, ringAlpha * 1.18F
      );
      renderOrbitNodes(
         stack,
         glowConsumer,
         constraintRadius,
         outerYaw,
         118.0F,
         -20.0F,
         35.0F - ringPhase * 1.25F,
         3,
         0.064F * nodePulse,
         1.0F,
         0.64F,
         0.16F,
         ringAlpha * 1.02F
      );
      stack.m_85849_();
   }

   private static void renderMatrixRing(
      PoseStack stack,
      VertexConsumer consumer,
      float radius,
      float halfWidth,
      float halfThickness,
      float yaw,
      float pitch,
      float roll,
      float red,
      float green,
      float blue,
      float alpha
   ) {
      stack.m_85836_();
      stack.m_252781_(Axis.f_252436_.m_252977_(yaw));
      stack.m_252781_(Axis.f_252529_.m_252977_(pitch));
      stack.m_252781_(Axis.f_252403_.m_252977_(roll));
      CoreEffectMesh.ringBand(stack, consumer, radius - halfWidth, radius + halfWidth, halfThickness, red, green, blue, alpha);
      stack.m_85849_();
   }

   private static void renderOrbitNodes(
      PoseStack stack,
      VertexConsumer consumer,
      float radius,
      float yaw,
      float pitch,
      float roll,
      float orbitDegrees,
      int count,
      float nodeRadius,
      float red,
      float green,
      float blue,
      float alpha
   ) {
      stack.m_85836_();
      stack.m_252781_(Axis.f_252436_.m_252977_(yaw));
      stack.m_252781_(Axis.f_252529_.m_252977_(pitch));
      stack.m_252781_(Axis.f_252403_.m_252977_(roll));

      for (int index = 0; index < count; index++) {
         stack.m_85836_();
         stack.m_252781_(Axis.f_252436_.m_252977_(orbitDegrees + (float)index * 360.0F / (float)count));
         stack.m_252880_(radius, 0.0F, 0.0F);
         stack.m_252781_(Axis.f_252436_.m_252977_(45.0F));
         stack.m_252781_(Axis.f_252403_.m_252977_(45.0F));
         CoreEffectMesh.octahedron(stack, consumer, nodeRadius, red, green, blue, alpha);
         CoreEffectMesh.octahedron(stack, consumer, nodeRadius * 0.52F, 1.0F, 0.94F, 0.72F, Math.min(1.0F, alpha * 1.18F));
         stack.m_85849_();
      }

      stack.m_85849_();
   }

   private static float brighten(float color, float amount) {
      return color + (1.0F - color) * amount;
   }

   private static float lerp(float start, float end, float progress) {
      return start + (end - start) * progress;
   }

   private static void renderCubeCore(PoseStack stack, VertexConsumer consumer, CoreEffectPalette palette, double stepPhase, double spinDegrees) {
      int step = (int)Math.floor(stepPhase);
      float progress = (float)(stepPhase - (double)step);
      float turn = smoothStep(clamp((progress - 0.12F) / 0.7F)) * 90.0F;
      int axis = Math.floorMod(step, 3);
      int layer = Math.floorMod(step / 3, 3) - 1;
      float direction = (step & 1) == 0 ? 1.0F : -1.0F;
      stack.m_85836_();
      stack.m_252781_(Axis.f_252436_.m_252977_((float)spinDegrees));
      stack.m_252781_(Axis.f_252529_.m_252977_(24.0F));
      stack.m_252781_(Axis.f_252403_.m_252977_(-8.0F));

      for (int x = -1; x <= 1; x++) {
         for (int y = -1; y <= 1; y++) {
            for (int z = -1; z <= 1; z++) {
               if (x != 0 || y != 0 || z != 0) {
                  renderCubelet(stack, consumer, palette, x, y, z, axis, layer, turn * direction);
               }
            }
         }
      }

      stack.m_85849_();
   }

   private static void renderCubelet(PoseStack stack, VertexConsumer consumer, CoreEffectPalette palette, int x, int y, int z, int axis, int layer, float turn) {
      stack.m_85836_();
      int coordinate = axis == 0 ? x : (axis == 1 ? y : z);
      if (coordinate == layer) {
         if (axis == 0) {
            stack.m_252781_(Axis.f_252529_.m_252977_(turn));
         } else if (axis == 1) {
            stack.m_252781_(Axis.f_252436_.m_252977_(turn));
         } else {
            stack.m_252781_(Axis.f_252403_.m_252977_(turn));
         }
      }

      float spacing = 0.56F;
      stack.m_252880_((float)x * spacing, (float)y * spacing, (float)z * spacing);
      int shell = Math.abs(x) + Math.abs(y) + Math.abs(z);
      float red;
      float green;
      float blue;
      float alpha;
      if (shell == 3) {
         red = palette.accentR();
         green = palette.accentG();
         blue = palette.accentB();
         alpha = 0.92F;
      } else if (shell == 1) {
         red = lerp(palette.primaryR(), palette.accentR(), 0.55F);
         green = lerp(palette.primaryG(), palette.accentG(), 0.55F);
         blue = lerp(palette.primaryB(), palette.accentB(), 0.55F);
         alpha = 0.9F;
      } else {
         red = palette.primaryR();
         green = palette.primaryG();
         blue = palette.primaryB();
         alpha = 0.86F;
      }

      CoreEffectMesh.cube(stack, consumer, 0.245F, red, green, blue, alpha);
      stack.m_85849_();
   }

   private static float clamp(float value) {
      return Math.max(0.0F, Math.min(1.0F, value));
   }

   private static float smoothStep(float value) {
      return value * value * (3.0F - 2.0F * value);
   }
}
