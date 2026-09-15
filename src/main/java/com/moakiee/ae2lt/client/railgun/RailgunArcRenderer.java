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
import java.util.List;
import java.util.Random;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.Vec3;
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
public final class RailgunArcRenderer {
   private static final double MAX_ARC_SPAN = 512.0;
   private static final int MAX_SEGMENTS = 384;
   private static final int MAX_ACTIVE_ARCS = 512;
   private static final List<RailgunArcRenderer.Arc> ACTIVE = new ArrayList<>();
   private static final Random RAND = new Random();

   private RailgunArcRenderer() {
   }

   public static void spawnPlasma(Vec3 from, Vec3 to, int segments, float spread, int lifetime) {
      spawn(from, to, segments, spread, lifetime, 1.0F, 0.8F, 0.9F, 1.0F, 0.3F, 0.6F, 0.05F, 0.16F);
   }

   public static void spawnChain(Vec3 from, Vec3 to, int lifetime) {
      Vec3 dir = to.m_82546_(from);
      double len = dir.m_82553_();
      if (!(len < 0.001)) {
         int segments = Math.max(6, Math.min(28, (int)Math.round(len * 1.5)));
         float spread = (float)Math.min(0.55, 0.1 + len * 0.02);
         spawnPlasma(from, to, segments, spread, lifetime);
      }
   }

   public static void spawnHighVoltageChain(Vec3 from, Vec3 to, int lifetime) {
      Vec3 dir = to.m_82546_(from);
      double len = dir.m_82553_();
      if (!(len < 0.001)) {
         int segments = Math.max(6, Math.min(28, (int)Math.round(len * 1.5)));
         float spread = (float)Math.min(0.55, 0.1 + len * 0.02);
         spawn(from, to, segments, spread, lifetime, 0.95F, 1.0F, 1.0F, 0.35F, 0.78F, 1.0F, 0.05F, 0.16F);
      }
   }

   public static void spawnImpactSpark(Vec3 from, Vec3 to, int lifetime) {
      Vec3 dir = to.m_82546_(from);
      double len = dir.m_82553_();
      int segments = Math.max(4, Math.min(14, (int)Math.round(len * 2.5)));
      float spread = (float)Math.min(0.35, 0.06 + len * 0.04);
      spawn(from, to, segments, spread, lifetime, 1.0F, 0.82F, 0.88F, 0.9F, 0.25F, 0.55F, 0.04F, 0.13F);
   }

   public static void spawnBeamSpark(Vec3 from, Vec3 to, int lifetime) {
      Vec3 dir = to.m_82546_(from);
      double len = dir.m_82553_();
      int segments = Math.max(4, Math.min(14, (int)Math.round(len * 2.5)));
      float spread = (float)Math.min(0.35, 0.06 + len * 0.04);
      spawn(from, to, segments, spread, lifetime, 0.95F, 1.0F, 1.0F, 0.52F, 0.88F, 1.0F, 0.04F, 0.13F);
   }

   public static void spawn(
      Vec3 from,
      Vec3 to,
      int segments,
      float spread,
      int lifetime,
      float coreR,
      float coreG,
      float coreB,
      float glowR,
      float glowG,
      float glowB,
      float coreWidth,
      float glowWidth
   ) {
      if (isRenderableSegment(from, to)
         && Float.isFinite(spread)
         && !(spread < 0.0F)
         && Float.isFinite(coreWidth)
         && !(coreWidth <= 0.0F)
         && Float.isFinite(glowWidth)
         && !(glowWidth <= 0.0F)
         && lifetime > 0) {
         segments = Math.max(2, Math.min(384, segments));
         Vec3 axis = to.m_82546_(from);
         double len = axis.m_82553_();
         if (!(len < 0.001)) {
            Vec3 dir = axis.m_82541_();
            Vec3 perpA = Math.abs(dir.f_82480_) > 0.95 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
            Vec3 sideX = dir.m_82537_(perpA).m_82541_();
            Vec3 sideY = dir.m_82537_(sideX).m_82541_();
            List<Vec3> pts = new ArrayList<>(segments + 1);
            pts.add(from);

            for (int i = 1; i < segments; i++) {
               double t = (double)i / (double)segments;
               double attenuation = 4.0 * t * (1.0 - t);
               double jx = (RAND.nextDouble() - 0.5) * 2.0 * (double)spread * attenuation;
               double jy = (RAND.nextDouble() - 0.5) * 2.0 * (double)spread * attenuation;
               Vec3 base = from.m_82549_(axis.m_82490_(t));
               pts.add(base.m_82549_(sideX.m_82490_(jx)).m_82549_(sideY.m_82490_(jy)));
            }

            pts.add(to);
            int overflow = ACTIVE.size() - 512 + 1;
            if (overflow > 0) {
               ACTIVE.subList(0, overflow).clear();
            }

            ACTIVE.add(new RailgunArcRenderer.Arc(pts, lifetime, coreR, coreG, coreB, glowR, glowG, glowB, coreWidth, glowWidth));
         }
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
            ACTIVE.removeIf(a -> --a.remaining <= 0);
            if (!ACTIVE.isEmpty()) {
               Camera cam = e.getCamera();
               Vec3 camPos = cam.m_90583_();
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

               for (RailgunArcRenderer.Arc arc : ACTIVE) {
                  float lifeT = (float)arc.remaining / (float)arc.totalLifetime;
                  float alpha = (float)Math.sqrt((double)Math.max(0.0F, lifeT));
                  drawArc(bb, matrix, arc, camPos, alpha);
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

   private static void drawArc(BufferBuilder bb, Matrix4f matrix, RailgunArcRenderer.Arc arc, Vec3 camPos, float alpha) {
      for (int i = 0; i < arc.points.size() - 1; i++) {
         Vec3 a = arc.points.get(i);
         Vec3 b = arc.points.get(i + 1);
         addSegmentBillboard(bb, matrix, a, b, camPos, arc.glowWidth, arc.glowR, arc.glowG, arc.glowB, 0.45F * alpha);
         addSegmentBillboard(
            bb,
            matrix,
            a,
            b,
            camPos,
            arc.glowWidth * 0.55F,
            (arc.glowR + arc.coreR) * 0.5F,
            (arc.glowG + arc.coreG) * 0.5F,
            (arc.glowB + arc.coreB) * 0.5F,
            0.65F * alpha
         );
         addSegmentBillboard(bb, matrix, a, b, camPos, arc.coreWidth, arc.coreR, arc.coreG, arc.coreB, 0.95F * alpha);
      }
   }

   private static void addSegmentBillboard(
      BufferBuilder bb, Matrix4f matrix, Vec3 a, Vec3 b, Vec3 camPos, float width, float r, float g, float bCol, float alpha
   ) {
      if (isRenderableSegment(a, b)
         && isFinite(camPos)
         && Float.isFinite(width)
         && !(width <= 0.0F)
         && Float.isFinite(r)
         && Float.isFinite(g)
         && Float.isFinite(bCol)
         && Float.isFinite(alpha)) {
         Vec3 axis = b.m_82546_(a);
         double axisLengthSqr = axis.m_82556_();
         if (Double.isFinite(axisLengthSqr) && !(axisLengthSqr < 1.0E-9)) {
            Vec3 dir = axis.m_82541_();
            Vec3 mid = a.m_82549_(b).m_82490_(0.5);
            Vec3 toCam = camPos.m_82546_(mid);
            Vec3 side = dir.m_82537_(toCam);
            double sideLengthSqr = side.m_82556_();
            if (!Double.isFinite(sideLengthSqr) || sideLengthSqr < 1.0E-9) {
               side = dir.m_82537_(new Vec3(0.0, 1.0, 0.0));
               sideLengthSqr = side.m_82556_();
               if (!Double.isFinite(sideLengthSqr) || sideLengthSqr < 1.0E-9) {
                  side = dir.m_82537_(new Vec3(1.0, 0.0, 0.0));
               }
            }

            side = side.m_82541_().m_82490_((double)width);
            if (isFinite(side)) {
               Vec3 a1 = a.m_82549_(side);
               Vec3 a2 = a.m_82546_(side);
               Vec3 b1 = b.m_82549_(side);
               Vec3 b2 = b.m_82546_(side);
               bb.m_252986_(matrix, (float)a1.f_82479_, (float)a1.f_82480_, (float)a1.f_82481_).m_85950_(r, g, bCol, alpha).m_5752_();
               bb.m_252986_(matrix, (float)a2.f_82479_, (float)a2.f_82480_, (float)a2.f_82481_).m_85950_(r, g, bCol, alpha).m_5752_();
               bb.m_252986_(matrix, (float)b2.f_82479_, (float)b2.f_82480_, (float)b2.f_82481_).m_85950_(r, g, bCol, alpha).m_5752_();
               bb.m_252986_(matrix, (float)b1.f_82479_, (float)b1.f_82480_, (float)b1.f_82481_).m_85950_(r, g, bCol, alpha).m_5752_();
            }
         }
      }
   }

   static boolean isRenderableSegment(Vec3 from, Vec3 to) {
      if (isFinite(from) && isFinite(to)) {
         double lengthSqr = from.m_82557_(to);
         return Double.isFinite(lengthSqr) && lengthSqr <= 262144.0;
      } else {
         return false;
      }
   }

   static boolean isFinite(Vec3 value) {
      return value != null && Double.isFinite(value.f_82479_) && Double.isFinite(value.f_82480_) && Double.isFinite(value.f_82481_);
   }

   public static final class Arc {
      final List<Vec3> points;
      final int totalLifetime;
      int remaining;
      final float coreR;
      final float coreG;
      final float coreB;
      final float glowR;
      final float glowG;
      final float glowB;
      final float coreWidth;
      final float glowWidth;

      Arc(List<Vec3> points, int lifetime, float coreR, float coreG, float coreB, float glowR, float glowG, float glowB, float coreWidth, float glowWidth) {
         this.points = points;
         this.totalLifetime = lifetime;
         this.remaining = lifetime;
         this.coreR = coreR;
         this.coreG = coreG;
         this.coreB = coreB;
         this.glowR = glowR;
         this.glowG = glowG;
         this.glowB = glowB;
         this.coreWidth = coreWidth;
         this.glowWidth = glowWidth;
      }
   }
}
