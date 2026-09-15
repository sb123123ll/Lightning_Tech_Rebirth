package com.moakiee.ae2lt.client.railgun;

import com.moakiee.ae2lt.item.railgun.ElectromagneticRailgunItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.HumanoidModel.ArmPose;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

public final class RailgunClientExtensions implements IClientItemExtensions {
   public static final RailgunClientExtensions INSTANCE = new RailgunClientExtensions();
   public static final float MAIN_ARM_X_ROT_BASE = -1.48F;
   public static final float SUPPORT_ARM_X_ROT_BASE = -1.42F;

   private RailgunClientExtensions() {
   }

   public ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
      return stack.m_41720_() instanceof ElectromagneticRailgunItem ? ArmPose.CROSSBOW_HOLD : null;
   }

   public boolean applyForgeHandTransform(
      PoseStack poseStack, LocalPlayer player, HumanoidArm arm, ItemStack itemInHand, float partialTick, float equipProcess, float swingProcess
   ) {
      if (!(itemInHand.m_41720_() instanceof ElectromagneticRailgunItem)) {
         return false;
      } else {
         int side = arm == HumanoidArm.RIGHT ? 1 : -1;
         boolean charging = player.m_6117_() && player.m_21211_().m_41720_() instanceof ElectromagneticRailgunItem;
         float equipDip = charging ? 0.0F : equipProcess;
         poseStack.m_252880_((float)side * 0.48F, -0.49F - 0.6F * equipDip, -0.98F);
         poseStack.m_252781_(Axis.f_252529_.m_252977_(-4.0F));
         poseStack.m_252781_(Axis.f_252436_.m_252977_((float)side * -2.0F));
         poseStack.m_252781_(Axis.f_252403_.m_252977_((float)side * -3.0F));
         return true;
      }
   }

   public static void poseRailgunArms(HumanoidModel<?> model, LivingEntity entity, HumanoidArm activeArm) {
      boolean right = activeArm == HumanoidArm.RIGHT;
      ModelPart main = right ? model.f_102811_ : model.f_102812_;
      ModelPart support = right ? model.f_102812_ : model.f_102811_;
      float mirror = right ? 1.0F : -1.0F;
      main.f_104203_ = -1.48F + model.f_102808_.f_104203_;
      main.f_104204_ = model.f_102808_.f_104204_;
      main.f_104205_ = 0.0F;
      support.f_104203_ = -1.42F + model.f_102808_.f_104203_;
      support.f_104204_ = model.f_102808_.f_104204_ + mirror * 0.42F;
      support.f_104205_ = -mirror * 0.14F;
   }
}
