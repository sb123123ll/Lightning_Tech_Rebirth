package com.moakiee.ae2lt.mixin.client;

import com.moakiee.ae2lt.client.railgun.RailgunClientExtensions;
import com.moakiee.ae2lt.item.railgun.ElectromagneticRailgunItem;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({HumanoidModel.class})
public class RailgunHumanoidModelMixin<T extends LivingEntity> {
   @Inject(
      method = {"setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V"},
      at = {@At("RETURN")}
   )
   private void ae2lt$poseRailgun(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
      InteractionHand hand = null;
      if (entity.m_21205_().m_41720_() instanceof ElectromagneticRailgunItem) {
         hand = InteractionHand.MAIN_HAND;
      } else if (entity.m_21206_().m_41720_() instanceof ElectromagneticRailgunItem) {
         hand = InteractionHand.OFF_HAND;
      }

      if (hand != null) {
         HumanoidArm arm = hand == InteractionHand.MAIN_HAND ? entity.m_5737_() : entity.m_5737_().m_20828_();
         RailgunClientExtensions.poseRailgunArms((HumanoidModel<?>)this, entity, arm);
      }
   }
}
