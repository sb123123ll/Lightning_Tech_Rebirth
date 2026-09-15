package com.moakiee.ae2lt.client.railgun;

import com.mojang.blaze3d.platform.GlStateManager.DestFactor;
import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.BufferBuilder.RenderedBuffer;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent.Stage;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import org.joml.Matrix4f;

@EventBusSubscriber(
   modid = "ae2lt",
   value = {Dist.CLIENT}
)
public final class RailgunShockwaveRenderer {
   private static final int RING_SEGMENTS = 64;
   private static final int OCCLUSION_REFRESH_TICKS = 1;
   private static final float SHOCKWAVE_RADIUS_JITTER = 0.095F;
   private static final List<RailgunShockwaveRenderer.Burst> ACTIVE = new ArrayList<>();

   private RailgunShockwaveRenderer() {
   }

   public static void spawn(Vec3 center, float maxRadius, int lifetime) {
      if (RailgunArcRenderer.isFinite(center) && Float.isFinite(maxRadius) && !(maxRadius <= 0.0F) && lifetime > 0) {
         ACTIVE.add(new RailgunShockwaveRenderer.Burst(center, maxRadius, lifetime, 1.0F, 0.5F, 0.75F));
      }
   }

   public static void clear() {
      ACTIVE.clear();
   }

   @SubscribeEvent
   public static void onRender(RenderLevelStageEvent e) {
      if (e.getStage() == Stage.AFTER_TRANSLUCENT_BLOCKS) {
         Minecraft mc = Minecraft.m_91087_();
         if (mc.f_91073_ != null && !ACTIVE.isEmpty()) {
            ACTIVE.removeIf(b -> --b.remaining <= 0);
            if (!ACTIVE.isEmpty()) {
               Camera cam = e.getCamera();
               Vec3 camPos = cam.m_90583_();
               long gameTick = mc.f_91073_.m_46467_();
               PoseStack stack = e.getPoseStack();
               stack.m_85836_();
               stack.m_85837_(-camPos.f_82479_, -camPos.f_82480_, -camPos.f_82481_);
               RenderSystem.enableDepthTest();
               RenderSystem.depthFunc(515);
               RenderSystem.depthMask(false);
               RenderSystem.disableCull();
               RenderSystem.enableBlend();
               RenderSystem.blendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE, SourceFactor.ONE, DestFactor.ZERO);
               RenderSystem.setShader(GameRenderer::m_172811_);
               BufferBuilder bb = Tesselator.m_85913_().m_85915_();
               bb.m_166779_(Mode.QUADS, DefaultVertexFormat.f_85815_);
               Matrix4f matrix = stack.m_85850_().m_252922_();

               for (RailgunShockwaveRenderer.Burst burst : ACTIVE) {
                  float t = 1.0F - (float)burst.remaining / (float)burst.totalLifetime;
                  float radius = burst.maxRadius * (1.0F - (1.0F - t) * (1.0F - t));
                  float ringFade = 1.0F - t;
                  float ringWidth = burst.maxRadius * 0.18F * (1.0F - t * 0.5F);
                  if (gameTick - burst.lastOcclusionTick >= 1L) {
                     refreshOcclusion(mc.f_91073_, burst, radius, ringWidth, t, gameTick);
                  }

                  addRing(bb, matrix, burst, radius, ringWidth, t, burst.r, burst.g, burst.b, 0.85F * ringFade);
               }

               RenderedBuffer built = bb.m_231175_();
               if (built != null) {
                  BufferUploader.m_231202_(built);
               }

               RenderSystem.disableBlend();
               RenderSystem.defaultBlendFunc();
               RenderSystem.enableCull();
               RenderSystem.depthMask(true);
               RenderSystem.enableDepthTest();
               stack.m_85849_();
            }
         }
      }
   }

   private static void refreshOcclusion(Level level, RailgunShockwaveRenderer.Burst burst, float radius, float thickness, float progress, long gameTick) {
      burst.lastOcclusionTick = gameTick;
      if (level != null && !(radius <= 0.0F) && !(thickness <= 0.0F)) {
         Vec3 center = burst.center;
         double cy = center.f_82480_ + 0.05;
         Vec3 castFrom = new Vec3(center.f_82479_, cy, center.f_82481_);

         for (int i = 0; i < 64; i++) {
            double a0 = (double)i / 64.0 * Math.PI * 2.0;
            double a1 = (double)(i + 1) / 64.0 * Math.PI * 2.0;
            float radius0 = irregularRadius(center, radius, i, progress);
            float radius1 = irregularRadius(center, radius, i + 1, progress);
            float outer0 = radius0 + thickness * 0.5F;
            float outer1 = radius1 + thickness * 0.5F;
            double midA = (a0 + a1) * 0.5;
            double testR = (double)Math.max(outer0, outer1) + 0.05;
            double mx = center.f_82479_ + Math.cos(midA) * testR;
            double mz = center.f_82481_ + Math.sin(midA) * testR;
            HitResult hr = level.m_45547_(new ClipContext(castFrom, new Vec3(mx, cy, mz), Block.COLLIDER, Fluid.NONE, null));
            burst.occludedSegments[i] = hr.m_6662_() != Type.MISS;
         }
      } else {
         Arrays.fill(burst.occludedSegments, false);
      }
   }

   private static void addRing(
      BufferBuilder bb,
      Matrix4f matrix,
      RailgunShockwaveRenderer.Burst burst,
      float radius,
      float thickness,
      float progress,
      float r,
      float g,
      float b,
      float alpha
   ) {
      if (!(radius <= 0.0F) && !(thickness <= 0.0F)) {
         Vec3 center = burst.center;
         double cy = center.f_82480_ + 0.05;
         float y = (float)cy;

         for (int i = 0; i < 64; i++) {
            if (!burst.occludedSegments[i]) {
               double a0 = (double)i / 64.0 * Math.PI * 2.0;
               double a1 = (double)(i + 1) / 64.0 * Math.PI * 2.0;
               float radius0 = irregularRadius(center, radius, i, progress);
               float radius1 = irregularRadius(center, radius, i + 1, progress);
               float inner0 = Math.max(0.0F, radius0 - thickness * 0.5F);
               float inner1 = Math.max(0.0F, radius1 - thickness * 0.5F);
               float outer0 = radius0 + thickness * 0.5F;
               float outer1 = radius1 + thickness * 0.5F;
               double cos0 = Math.cos(a0);
               double sin0 = Math.sin(a0);
               double cos1 = Math.cos(a1);
               double sin1 = Math.sin(a1);
               float ix0 = (float)(center.f_82479_ + cos0 * (double)inner0);
               float iz0 = (float)(center.f_82481_ + sin0 * (double)inner0);
               float ix1 = (float)(center.f_82479_ + cos1 * (double)inner1);
               float iz1 = (float)(center.f_82481_ + sin1 * (double)inner1);
               float ox0 = (float)(center.f_82479_ + cos0 * (double)outer0);
               float oz0 = (float)(center.f_82481_ + sin0 * (double)outer0);
               float ox1 = (float)(center.f_82479_ + cos1 * (double)outer1);
               float oz1 = (float)(center.f_82481_ + sin1 * (double)outer1);
               bb.m_252986_(matrix, ix0, y, iz0).m_85950_(r, g, b, alpha).m_5752_();
               bb.m_252986_(matrix, ix1, y, iz1).m_85950_(r, g, b, alpha).m_5752_();
               bb.m_252986_(matrix, ox1, y, oz1).m_85950_(r, g, b, alpha * 0.15F).m_5752_();
               bb.m_252986_(matrix, ox0, y, oz0).m_85950_(r, g, b, alpha * 0.15F).m_5752_();
            }
         }
      }
   }

   private static float irregularRadius(Vec3 center, float radius, int segment, float progress) {
      if (radius <= 0.0F) {
         return radius;
      } else {
         double phase = center.f_82479_ * 0.17 + center.f_82480_ * 0.11 + center.f_82481_ * 0.13;
         double slow = Math.sin((double)segment * 0.59 + phase + (double)progress * 2.4);
         double fast = Math.sin((double)segment * 1.37 + phase * 0.43 - (double)progress * 3.1);
         float offset = (float)(slow * 0.68 + fast * 0.32);
         return radius * (1.0F + offset * 0.095F);
      }
   }

   public static final class Burst {
      final Vec3 center;
      final float maxRadius;
      final int totalLifetime;
      int remaining;
      final float r;
      final float g;
      final float b;
      final boolean[] occludedSegments = new boolean[64];
      long lastOcclusionTick = Long.MIN_VALUE;

      Burst(Vec3 center, float maxRadius, int lifetime, float r, float g, float b) {
         this.center = center;
         this.maxRadius = maxRadius;
         this.totalLifetime = lifetime;
         this.remaining = lifetime;
         this.r = r;
         this.g = g;
         this.b = b;
      }
   }
}
