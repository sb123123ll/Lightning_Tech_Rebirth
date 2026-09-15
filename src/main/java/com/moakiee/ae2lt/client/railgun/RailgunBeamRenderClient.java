package com.moakiee.ae2lt.client.railgun;

import com.moakiee.ae2lt.item.railgun.ElectromagneticRailgunItem;
import com.moakiee.ae2lt.network.railgun.RailgunBeamUpdatePacket;
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
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
public final class RailgunBeamRenderClient {
   private static final Map<UUID, RailgunBeamRenderClient.BeamState> ACTIVE = new ConcurrentHashMap<>();
   private static final long STALE_TICKS = 6L;
   private static final float OUTER_RADIUS = 0.2F;
   private static final float MID_RADIUS = 0.11F;
   private static final float CORE_RADIUS = 0.038F;
   private static final int BEAM_SEGMENTS = 12;
   private static final double PULSE_RATE = 0.3;
   private static final double FLOW_RATE = 0.22;
   private static final double SPIN_OUTER = 0.18;
   private static final double SPIN_MID = -0.32;
   private static final double SPIN_CORE = 0.55;
   private static final long ARC_INTERVAL_TICKS = 3L;
   private static volatile boolean localRequestedFiring = false;
   private static volatile boolean localFiring = false;

   private static RailgunBeamRenderClient.BeamGeometry resolveBeamGeometry(RailgunBeamRenderClient.BeamState s, Minecraft mc, float partialTick) {
      Player shooter = mc.f_91073_ == null ? null : mc.f_91073_.m_46003_(s.shooterId);
      if (shooter == null) {
         return new RailgunBeamRenderClient.BeamGeometry(s.from, s.to);
      } else {
         Vec3 origin = RailgunVisuals.computeBarrelOrigin(shooter, partialTick);
         Vec3 endpoint = RailgunVisuals.computeBarrelEndpoint(shooter, origin, s.from, s.to, partialTick);
         return new RailgunBeamRenderClient.BeamGeometry(origin, endpoint);
      }
   }

   private RailgunBeamRenderClient() {
   }

   public static void reset() {
      ACTIVE.clear();
      localRequestedFiring = false;
      localFiring = false;
   }

   public static void setLocalRequestedFiring(boolean firing) {
      localRequestedFiring = firing;
      if (!firing) {
         setLocalFiring(false);
      }
   }

   private static void setLocalFiring(boolean firing) {
      localFiring = firing;
      Minecraft mc = Minecraft.m_91087_();
      if (mc.f_91074_ != null) {
         if (!firing) {
            ACTIVE.remove(mc.f_91074_.m_20148_());
         }
      }
   }

   public static boolean isLocalFiring() {
      return localFiring;
   }

   public static void applyUpdate(RailgunBeamUpdatePacket p) {
      Minecraft mc = Minecraft.m_91087_();
      long tick = Minecraft.m_91087_().f_91073_ == null ? 0L : Minecraft.m_91087_().f_91073_.m_46467_();
      if (!p.active()) {
         ACTIVE.remove(p.shooterId());
         if (mc.f_91074_ != null && p.shooterId().equals(mc.f_91074_.m_20148_())) {
            localRequestedFiring = false;
            localFiring = false;
         }
      } else {
         boolean isLocal = mc.f_91074_ != null && p.shooterId().equals(mc.f_91074_.m_20148_());
         if (isLocal) {
            if (!localRequestedFiring) {
               return;
            }

            localFiring = true;
         }

         ACTIVE.compute(p.shooterId(), (k, prev) -> {
            if (prev == null) {
               return new RailgunBeamRenderClient.BeamState(p.shooterId(), p.from(), p.to(), tick);
            } else {
               prev.from = p.from();
               prev.to = p.to();
               prev.lastUpdateTick = tick;
               return (RailgunBeamRenderClient.BeamState)prev;
            }
         });
      }
   }

   @SubscribeEvent
   public static void onRender(RenderLevelStageEvent e) {
      if (e.getStage() == Stage.AFTER_TRANSLUCENT_BLOCKS) {
         Minecraft mc = Minecraft.m_91087_();
         if (mc.f_91073_ != null) {
            long now = mc.f_91073_.m_46467_();
            float partialTick = mc.m_91296_();
            refreshLocalBeam(mc, now, partialTick);
            ACTIVE.entrySet().removeIf(en -> now - en.getValue().lastUpdateTick > 6L);
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
               double smoothTime = (double)((float)now + partialTick);
               float pulse = 0.95F + 0.1F * (float)Math.sin(smoothTime * 0.3);
               BufferBuilder bb = Tesselator.m_85913_().m_85915_();
               bb.m_166779_(Mode.QUADS, DefaultVertexFormat.f_85815_);
               Matrix4f matrix = stack.m_85850_().m_252922_();

               for (RailgunBeamRenderClient.BeamState s : ACTIVE.values()) {
                  RailgunBeamRenderClient.BeamGeometry g = resolveBeamGeometry(s, mc, partialTick);
                  addBeam(bb, matrix, g.origin, g.endpoint, pulse, smoothTime);
                  addEndpointGlow(bb, matrix, g.endpoint, camPos, pulse);
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
               spawnCrackleArcs(mc, now, partialTick);
            }
         }
      }
   }

   private static void refreshLocalBeam(Minecraft mc, long now, float partialTick) {
      if (localFiring && mc.f_91074_ != null && mc.f_91073_ != null) {
         ItemStack stack = mc.f_91074_.m_21205_();
         if (!(stack.m_41720_() instanceof ElectromagneticRailgunItem) || mc.f_91080_ != null) {
            setLocalFiring(false);
         }
      }
   }

   private static void addBeam(BufferBuilder bb, Matrix4f matrix, Vec3 origin, Vec3 endpoint, float pulse, double smoothTime) {
      double ax = endpoint.f_82479_ - origin.f_82479_;
      double ay = endpoint.f_82480_ - origin.f_82480_;
      double az = endpoint.f_82481_ - origin.f_82481_;
      double len = Math.sqrt(ax * ax + ay * ay + az * az);
      if (!(len < 0.001)) {
         double inv = 1.0 / len;
         double dx = ax * inv;
         double dy = ay * inv;
         double dz = az * inv;
         double hx;
         double hy;
         double hz;
         if (Math.abs(dy) < 0.95) {
            hx = 0.0;
            hy = 1.0;
            hz = 0.0;
         } else {
            hx = 1.0;
            hy = 0.0;
            hz = 0.0;
         }

         double e1x = dy * hz - dz * hy;
         double e1y = dz * hx - dx * hz;
         double e1z = dx * hy - dy * hx;
         double e1l = Math.sqrt(e1x * e1x + e1y * e1y + e1z * e1z);
         e1x /= e1l;
         e1y /= e1l;
         e1z /= e1l;
         double e2x = dy * e1z - dz * e1y;
         double e2y = dz * e1x - dx * e1z;
         double e2z = dx * e1y - dy * e1x;
         double cO = Math.cos(smoothTime * 0.18);
         double sO = Math.sin(smoothTime * 0.18);
         double cM = Math.cos(smoothTime * -0.32);
         double sM = Math.sin(smoothTime * -0.32);
         double cC = Math.cos(smoothTime * 0.55);
         double sC = Math.sin(smoothTime * 0.55);
         double oUx = e1x * cO + e2x * sO;
         double oUy = e1y * cO + e2y * sO;
         double oUz = e1z * cO + e2z * sO;
         double oVx = -e1x * sO + e2x * cO;
         double oVy = -e1y * sO + e2y * cO;
         double oVz = -e1z * sO + e2z * cO;
         double mUx = e1x * cM + e2x * sM;
         double mUy = e1y * cM + e2y * sM;
         double mUz = e1z * cM + e2z * sM;
         double mVx = -e1x * sM + e2x * cM;
         double mVy = -e1y * sM + e2y * cM;
         double mVz = -e1z * sM + e2z * cM;
         double cUx = e1x * cC + e2x * sC;
         double cUy = e1y * cC + e2y * sC;
         double cUz = e1z * cC + e2z * sC;
         double cVx = -e1x * sC + e2x * cC;
         double cVy = -e1y * sC + e2y * cC;
         double cVz = -e1z * sC + e2z * cC;
         double ox = origin.f_82479_;
         double oy = origin.f_82480_;
         double oz = origin.f_82481_;

         for (int i = 0; i < 12; i++) {
            float t0 = (float)i / 12.0F;
            float t1 = (float)(i + 1) / 12.0F;
            double afx = ox + ax * (double)t0;
            double afy = oy + ay * (double)t0;
            double afz = oz + az * (double)t0;
            double atx = ox + ax * (double)t1;
            double aty = oy + ay * (double)t1;
            double atz = oz + az * (double)t1;
            float taper0 = 1.0F - 0.22F * t0;
            float taper1 = 1.0F - 0.22F * t1;
            float flow0 = 0.55F + 0.45F * (float)Math.sin((double)(t0 * 9.0F) - smoothTime * 0.22);
            float flow1 = 0.55F + 0.45F * (float)Math.sin((double)(t1 * 9.0F) - smoothTime * 0.22);
            float a0 = pulse * flow0;
            float a1 = pulse * flow1;
            addPrismSegment(
               bb, matrix, afx, afy, afz, atx, aty, atz, oUx, oUy, oUz, oVx, oVy, oVz, 0.2F * taper0, 0.2F * taper1, 0.22F, 0.58F, 1.0F, 0.18F * a0, 0.18F * a1
            );
            addPrismSegment(
               bb,
               matrix,
               afx,
               afy,
               afz,
               atx,
               aty,
               atz,
               mUx,
               mUy,
               mUz,
               mVx,
               mVy,
               mVz,
               0.11F * taper0,
               0.11F * taper1,
               0.52F,
               0.88F,
               1.0F,
               0.34F * a0,
               0.34F * a1
            );
            float coreA0 = Math.min(1.0F, 1.1F * a0);
            float coreA1 = Math.min(1.0F, 1.1F * a1);
            addPrismSegment(
               bb,
               matrix,
               afx,
               afy,
               afz,
               atx,
               aty,
               atz,
               cUx,
               cUy,
               cUz,
               cVx,
               cVy,
               cVz,
               0.038F * taper0,
               0.038F * taper1,
               0.95F,
               1.0F,
               1.0F,
               0.7F * coreA0,
               0.7F * coreA1
            );
         }
      }
   }

   private static void addEndpointGlow(BufferBuilder bb, Matrix4f matrix, Vec3 center, Vec3 cameraPos, float pulse) {
      double tcx = cameraPos.f_82479_ - center.f_82479_;
      double tcy = cameraPos.f_82480_ - center.f_82480_;
      double tcz = cameraPos.f_82481_ - center.f_82481_;
      double tcLenSqr = tcx * tcx + tcy * tcy + tcz * tcz;
      if (!(tcLenSqr < 1.0E-6)) {
         double tcInv = 1.0 / Math.sqrt(tcLenSqr);
         double fx = tcx * tcInv;
         double fy = tcy * tcInv;
         double fz = tcz * tcInv;
         double rx = fy * 0.0 - fz * 1.0;
         double ry = fz * 0.0 - fx * 0.0;
         double rz = fx * 1.0 - fy * 0.0;
         double rLenSqr = rx * rx + ry * ry + rz * rz;
         if (rLenSqr < 1.0E-9) {
            rx = 0.0;
            ry = fz;
            rz = -fy;
            rLenSqr = fz * fz + rz * rz;
         }

         float radius = 0.55F * pulse;
         double rNormScale = (double)radius / Math.sqrt(rLenSqr);
         rx *= rNormScale;
         ry *= rNormScale;
         rz *= rNormScale;
         double ux = ry * fz - rz * fy;
         double uy = rz * fx - rx * fz;
         double uz = rx * fy - ry * fx;
         double uLen = Math.sqrt(ux * ux + uy * uy + uz * uz);
         double uScale = (double)radius / uLen;
         ux *= uScale;
         uy *= uScale;
         uz *= uScale;
         double cx = center.f_82479_;
         double cy = center.f_82480_;
         double cz = center.f_82481_;
         emitGlowQuad(
            bb,
            matrix,
            cx + rx + ux,
            cy + ry + uy,
            cz + rz + uz,
            cx - rx + ux,
            cy - ry + uy,
            cz - rz + uz,
            cx - rx - ux,
            cy - ry - uy,
            cz - rz - uz,
            cx + rx - ux,
            cy + ry - uy,
            cz + rz - uz,
            0.65F,
            0.9F,
            1.0F,
            0.05F * pulse
         );
         double irx = rx * 0.45;
         double iry = ry * 0.45;
         double irz = rz * 0.45;
         double iux = ux * 0.45;
         double iuy = uy * 0.45;
         double iuz = uz * 0.45;
         emitGlowQuad(
            bb,
            matrix,
            cx + irx + iux,
            cy + iry + iuy,
            cz + irz + iuz,
            cx - irx + iux,
            cy - iry + iuy,
            cz - irz + iuz,
            cx - irx - iux,
            cy - iry - iuy,
            cz - irz - iuz,
            cx + irx - iux,
            cy + iry - iuy,
            cz + irz - iuz,
            1.0F,
            1.0F,
            1.0F,
            0.9F * pulse
         );
      }
   }

   private static void emitGlowQuad(
      BufferBuilder bb,
      Matrix4f matrix,
      double p0x,
      double p0y,
      double p0z,
      double p1x,
      double p1y,
      double p1z,
      double p2x,
      double p2y,
      double p2z,
      double p3x,
      double p3y,
      double p3z,
      float r,
      float g,
      float b,
      float a
   ) {
      bb.m_252986_(matrix, (float)p0x, (float)p0y, (float)p0z).m_85950_(r, g, b, a).m_5752_();
      bb.m_252986_(matrix, (float)p1x, (float)p1y, (float)p1z).m_85950_(r, g, b, a).m_5752_();
      bb.m_252986_(matrix, (float)p2x, (float)p2y, (float)p2z).m_85950_(r, g, b, a).m_5752_();
      bb.m_252986_(matrix, (float)p3x, (float)p3y, (float)p3z).m_85950_(r, g, b, a).m_5752_();
   }

   private static void addPrismSegment(
      BufferBuilder bb,
      Matrix4f matrix,
      double fx,
      double fy,
      double fz,
      double tx,
      double ty,
      double tz,
      double uX,
      double uY,
      double uZ,
      double vX,
      double vY,
      double vZ,
      float radiusFrom,
      float radiusTo,
      float r,
      float g,
      float b,
      float aFrom,
      float aTo
   ) {
      double uFx = uX * (double)radiusFrom;
      double uFy = uY * (double)radiusFrom;
      double uFz = uZ * (double)radiusFrom;
      double vFx = vX * (double)radiusFrom;
      double vFy = vY * (double)radiusFrom;
      double vFz = vZ * (double)radiusFrom;
      double uTx = uX * (double)radiusTo;
      double uTy = uY * (double)radiusTo;
      double uTz = uZ * (double)radiusTo;
      double vTx = vX * (double)radiusTo;
      double vTy = vY * (double)radiusTo;
      double vTz = vZ * (double)radiusTo;
      double f0x = fx + uFx + vFx;
      double f0y = fy + uFy + vFy;
      double f0z = fz + uFz + vFz;
      double f1x = fx - uFx + vFx;
      double f1y = fy - uFy + vFy;
      double f1z = fz - uFz + vFz;
      double f2x = fx - uFx - vFx;
      double f2y = fy - uFy - vFy;
      double f2z = fz - uFz - vFz;
      double f3x = fx + uFx - vFx;
      double f3y = fy + uFy - vFy;
      double f3z = fz + uFz - vFz;
      double t0x = tx + uTx + vTx;
      double t0y = ty + uTy + vTy;
      double t0z = tz + uTz + vTz;
      double t1x = tx - uTx + vTx;
      double t1y = ty - uTy + vTy;
      double t1z = tz - uTz + vTz;
      double t2x = tx - uTx - vTx;
      double t2y = ty - uTy - vTy;
      double t2z = tz - uTz - vTz;
      double t3x = tx + uTx - vTx;
      double t3y = ty + uTy - vTy;
      double t3z = tz + uTz - vTz;
      emitFace(bb, matrix, f0x, f0y, f0z, f1x, f1y, f1z, t1x, t1y, t1z, t0x, t0y, t0z, r, g, b, aFrom, aTo);
      emitFace(bb, matrix, f1x, f1y, f1z, f2x, f2y, f2z, t2x, t2y, t2z, t1x, t1y, t1z, r, g, b, aFrom, aTo);
      emitFace(bb, matrix, f2x, f2y, f2z, f3x, f3y, f3z, t3x, t3y, t3z, t2x, t2y, t2z, r, g, b, aFrom, aTo);
      emitFace(bb, matrix, f3x, f3y, f3z, f0x, f0y, f0z, t0x, t0y, t0z, t3x, t3y, t3z, r, g, b, aFrom, aTo);
   }

   private static void emitFace(
      BufferBuilder bb,
      Matrix4f matrix,
      double p0x,
      double p0y,
      double p0z,
      double p1x,
      double p1y,
      double p1z,
      double p2x,
      double p2y,
      double p2z,
      double p3x,
      double p3y,
      double p3z,
      float r,
      float g,
      float b,
      float aFrom,
      float aTo
   ) {
      bb.m_252986_(matrix, (float)p0x, (float)p0y, (float)p0z).m_85950_(r, g, b, aFrom).m_5752_();
      bb.m_252986_(matrix, (float)p1x, (float)p1y, (float)p1z).m_85950_(r, g, b, aFrom).m_5752_();
      bb.m_252986_(matrix, (float)p2x, (float)p2y, (float)p2z).m_85950_(r, g, b, aTo).m_5752_();
      bb.m_252986_(matrix, (float)p3x, (float)p3y, (float)p3z).m_85950_(r, g, b, aTo).m_5752_();
   }

   private static void spawnCrackleArcs(Minecraft mc, long now, float partialTick) {
      if (mc.f_91073_ != null) {
         for (RailgunBeamRenderClient.BeamState s : ACTIVE.values()) {
            if (now - s.lastArcTick >= 3L) {
               s.lastArcTick = now;
               RailgunBeamRenderClient.BeamGeometry g = resolveBeamGeometry(s, mc, partialTick);
               Vec3 axis = g.endpoint.m_82546_(g.origin);
               double len = axis.m_82553_();
               if (!(len < 1.0)) {
                  int n = 1 + mc.f_91073_.f_46441_.m_188503_(2);

                  for (int i = 0; i < n; i++) {
                     double t = 0.1 + mc.f_91073_.f_46441_.m_188500_() * 0.85;
                     Vec3 fromArc = g.origin.m_82549_(axis.m_82490_(t));
                     Vec3 randDir = new Vec3(
                        mc.f_91073_.f_46441_.m_188500_() - 0.5, mc.f_91073_.f_46441_.m_188500_() - 0.5, mc.f_91073_.f_46441_.m_188500_() - 0.5
                     );
                     if (!(randDir.m_82556_() < 1.0E-6)) {
                        randDir = randDir.m_82541_().m_82490_(0.4 + mc.f_91073_.f_46441_.m_188500_() * 0.7);
                        Vec3 toArc = fromArc.m_82549_(randDir);
                        RailgunArcRenderer.spawnBeamSpark(fromArc, toArc, 14 + mc.f_91073_.f_46441_.m_188503_(8));
                     }
                  }
               }
            }
         }
      }
   }

   private static record BeamGeometry(Vec3 origin, Vec3 endpoint) {
   }

   public static final class BeamState {
      public final UUID shooterId;
      public Vec3 from;
      public Vec3 to;
      public long lastUpdateTick;
      public long lastArcTick;

      BeamState(UUID shooterId, Vec3 f, Vec3 t, long tick) {
         this.shooterId = shooterId;
         this.from = f;
         this.to = t;
         this.lastUpdateTick = tick;
         this.lastArcTick = tick;
      }
   }
}
