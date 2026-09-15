package com.moakiee.ae2lt.client.railgun;

import com.moakiee.ae2lt.item.railgun.ElectromagneticRailgunItem;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import org.joml.Vector3f;

public final class RailgunVisuals {
   private static final double FP_SIDE_OFFSET = 0.56;
   private static final double FP_FORWARD_OFFSET = 1.4;
   private static final double FP_VERTICAL_OFFSET = -0.4;
   private static final double TP_SHOULDER_HEIGHT_FACTOR = 0.764;
   private static final double TP_SHOULDER_SIDE = 0.3125;
   private static final double TP_BARREL_OUTWARD = 0.0625;
   private static final double TP_BARREL_ALONG_ARM = 1.331;
   private static final double TP_BARREL_PERP_UP = 0.18;
   private static final float TP_ARM_PITCH_OFFSET_DEG = (float)Math.toDegrees(0.09079630772141023);

   private RailgunVisuals() {
   }

   public static Vec3 computeBarrelOrigin(Player player, float partialTick) {
      return isLocalFirstPerson(player) ? computeFirstPersonBarrelOrigin(player, partialTick) : computeThirdPersonBarrelOrigin(player, partialTick);
   }

   public static Vec3 computeBarrelDirection(Player player, float partialTick) {
      if (isLocalFirstPerson(player)) {
         Camera camera = Minecraft.m_91087_().f_91063_.m_109153_();
         Vec3 look = fromJoml(camera.m_253058_());
         if (look.m_82556_() < 1.0E-9) {
            return new Vec3(1.0, 0.0, 0.0);
         } else {
            look = look.m_82541_();
            Vec3 right = fromJoml(camera.m_252775_()).m_82490_(-1.0).m_82541_();
            Vec3 up = right.m_82537_(look).m_82541_();
            float pitchOff = fpHandPitchOffset(player, partialTick);
            float yawOff = fpHandYawOffset(player, partialTick);
            if (Math.abs(pitchOff) > 0.001F || Math.abs(yawOff) > 0.001F) {
               look = rotateDegrees(look, right, pitchOff);
               look = rotateDegrees(look, up, yawOff);
            }

            return look.m_82541_();
         }
      } else {
         float yaw = Mth.m_14189_(partialTick, player.f_20886_, player.f_20885_);
         float pitch = Mth.m_14179_(partialTick, player.f_19860_, player.m_146909_());
         return Vec3.m_82498_(pitch, yaw);
      }
   }

   public static Vec3 computeBarrelEndpoint(Player player, Vec3 origin, Vec3 shotFrom, Vec3 shotTo, float partialTick) {
      Vec3 dir = computeBarrelDirection(player, partialTick);
      double length = shotTo.m_82546_(shotFrom).m_82553_();
      if (length < 0.001) {
         return origin;
      } else {
         Minecraft mc = Minecraft.m_91087_();
         if (player != mc.f_91074_ && mc.f_91073_ != null) {
            Vec3 maxEnd = origin.m_82549_(dir.m_82490_(64.0));
            HitResult hit = mc.f_91073_.m_45547_(new ClipContext(origin, maxEnd, Block.COLLIDER, Fluid.NONE, player));
            if (hit.m_6662_() != Type.MISS) {
               double clipLen = hit.m_82450_().m_82546_(origin).m_82553_();
               length = Math.min(length, clipLen);
            }
         }

         return origin.m_82549_(dir.m_82490_(length));
      }
   }

   public static float currentPartialTick() {
      return Minecraft.m_91087_().m_91296_();
   }

   private static float fpHandPitchOffset(Player player, float partialTick) {
      if (player instanceof LocalPlayer lp) {
         float xBobLerp = Mth.m_14179_(partialTick, lp.f_108588_, lp.f_108586_);
         return (lp.m_5686_(partialTick) - xBobLerp) * 0.1F;
      } else {
         return 0.0F;
      }
   }

   private static float fpHandYawOffset(Player player, float partialTick) {
      if (player instanceof LocalPlayer lp) {
         float yBobLerp = Mth.m_14179_(partialTick, lp.f_108587_, lp.f_108585_);
         return (lp.m_5675_(partialTick) - yBobLerp) * 0.1F;
      } else {
         return 0.0F;
      }
   }

   private static boolean isLocalFirstPerson(Player player) {
      Minecraft mc = Minecraft.m_91087_();
      return player == mc.f_91074_ && mc.f_91066_.m_92176_().m_90612_();
   }

   private static Vec3 computeFirstPersonBarrelOrigin(Player player, float partialTick) {
      Camera camera = Minecraft.m_91087_().f_91063_.m_109153_();
      Vec3 eye = camera.m_90583_();
      Vec3 look = fromJoml(camera.m_253058_()).m_82541_();
      Vec3 right = fromJoml(camera.m_252775_()).m_82490_(-1.0).m_82541_();
      Vec3 up = right.m_82537_(look).m_82541_();
      double sideMul = holdingArm(player) == HumanoidArm.LEFT ? -1.0 : 1.0;
      Vec3 offset = look.m_82490_(1.4).m_82549_(right.m_82490_(0.56 * sideMul)).m_82549_(up.m_82490_(-0.4));
      float pitchOff = fpHandPitchOffset(player, partialTick);
      float yawOff = fpHandYawOffset(player, partialTick);
      if (Math.abs(pitchOff) > 0.001F || Math.abs(yawOff) > 0.001F) {
         offset = rotateDegrees(offset, right, pitchOff);
         offset = rotateDegrees(offset, up, yawOff);
      }

      return eye.m_82549_(offset);
   }

   private static Vec3 computeThirdPersonBarrelOrigin(Player player, float partialTick) {
      float bodyYaw = Mth.m_14189_(partialTick, player.f_20884_, player.f_20883_);
      float bodyYawRad = bodyYaw * (float) (Math.PI / 180.0);
      Vec3 bodyRight = new Vec3(-Math.cos((double)bodyYawRad), 0.0, -Math.sin((double)bodyYawRad));
      double sideMul = holdingArm(player) == HumanoidArm.LEFT ? -1.0 : 1.0;
      float headYaw = Mth.m_14189_(partialTick, player.f_20886_, player.f_20885_);
      float viewPitch = Mth.m_14179_(partialTick, player.f_19860_, player.m_146909_());
      float armPitch = viewPitch + TP_ARM_PITCH_OFFSET_DEG;
      Vec3 armDir = Vec3.m_82498_(armPitch, headYaw);
      Vec3 armPerpUp = Vec3.m_82498_(armPitch - 90.0F, headYaw);
      return player.m_20318_(partialTick)
         .m_82520_(0.0, (double)player.m_20206_() * 0.764, 0.0)
         .m_82549_(bodyRight.m_82490_(0.375 * sideMul))
         .m_82549_(armDir.m_82490_(1.331))
         .m_82549_(armPerpUp.m_82490_(0.18));
   }

   private static Vec3 rotateDegrees(Vec3 v, Vec3 axis, float deg) {
      double rad = Math.toRadians((double)deg);
      double cos = Math.cos(rad);
      double sin = Math.sin(rad);
      double dot = axis.m_82526_(v);
      Vec3 cross = axis.m_82537_(v);
      return v.m_82490_(cos).m_82549_(cross.m_82490_(sin)).m_82549_(axis.m_82490_(dot * (1.0 - cos)));
   }

   private static Vec3 fromJoml(Vector3f v) {
      return new Vec3((double)v.x(), (double)v.y(), (double)v.z());
   }

   private static HumanoidArm holdingArm(Player player) {
      if (player.m_21205_().m_41720_() instanceof ElectromagneticRailgunItem) {
         return player.m_5737_();
      } else if (player.m_21206_().m_41720_() instanceof ElectromagneticRailgunItem) {
         return player.m_5737_().m_20828_();
      } else {
         return player.m_7655_() == InteractionHand.OFF_HAND ? player.m_5737_().m_20828_() : player.m_5737_();
      }
   }
}
