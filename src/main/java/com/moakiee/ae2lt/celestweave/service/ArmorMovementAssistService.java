package com.moakiee.ae2lt.celestweave.service;

import com.moakiee.ae2lt.celestweave.FlightSneakMovement;
import com.moakiee.ae2lt.celestweave.MovementAssistRules;
import com.moakiee.ae2lt.celestweave.module.MovementAssistSubmodule;
import com.moakiee.ae2lt.device.capability.DeviceCapability;
import java.util.List;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraftforge.common.ForgeMod;

public final class ArmorMovementAssistService {
   private static final UUID SPEED_MODIFIER_ID = UUID.fromString("a1b2c3d4-5e6f-4a7b-8c9d-0e1f2a3b4c5d");
   private static final UUID STEP_HEIGHT_MODIFIER_ID = UUID.fromString("b2c3d4e5-6f7a-4b8c-9d0e-1f2a3b4c5d6e");
   private static final double EPSILON = 1.0E-6;

   private ArmorMovementAssistService() {
   }

   public static void tick(ServerPlayer player, List<ArmorCapabilityCollector.ActiveCapability> capabilities) {
      boolean active = false;
      double movementMultiplier = 1.0;
      double stepHeight = 0.6;
      boolean flightSneaking = FlightSneakMovement.isActive(player);
      boolean suppressGroundMovement = player.m_150110_().f_35935_ && !flightSneaking || player.m_21255_() || player.m_6069_();

      for (ArmorCapabilityCollector.ActiveCapability capability : capabilities) {
         if (capability.capability() instanceof DeviceCapability.MovementAssist) {
            double candidateMovementMultiplier = MovementAssistRules.movementMultiplier(
               suppressGroundMovement,
               flightSneaking || player.m_6047_(),
               player.m_20142_(),
               MovementAssistSubmodule.walkSpeedMultiplier(capability.armor()),
               MovementAssistSubmodule.sprintSpeedMultiplier(capability.armor()),
               MovementAssistSubmodule.sneakSpeedMultiplier(capability.armor())
            );
            double candidateStepHeight = MovementAssistSubmodule.automaticStepHeight(capability.armor());
            if (!active) {
               movementMultiplier = candidateMovementMultiplier;
               stepHeight = candidateStepHeight;
               active = true;
            } else {
               movementMultiplier = Math.max(movementMultiplier, candidateMovementMultiplier);
               stepHeight = Math.max(stepHeight, candidateStepHeight);
            }
         }
      }

      updateModifier(
         player, Attributes.f_22279_, SPEED_MODIFIER_ID, active ? MovementAssistRules.speedModifierAmount(movementMultiplier) : 0.0, Operation.MULTIPLY_TOTAL
      );
      updateModifier(
         player,
         (Attribute)ForgeMod.STEP_HEIGHT_ADDITION.get(),
         STEP_HEIGHT_MODIFIER_ID,
         active ? MovementAssistRules.stepHeightModifierAmount(stepHeight) : 0.0,
         Operation.ADDITION
      );
   }

   private static void updateModifier(ServerPlayer player, Attribute attribute, UUID id, double amount, Operation operation) {
      AttributeInstance instance = player.m_21051_(attribute);
      if (instance != null) {
         AttributeModifier existing = instance.m_22111_(id);
         if (Math.abs(amount) < 1.0E-6) {
            if (existing != null) {
               instance.m_22130_(existing);
            }
         } else if (existing == null || !(Math.abs(existing.m_22218_() - amount) < 1.0E-6) || existing.m_22217_() != operation) {
            if (existing != null) {
               instance.m_22130_(existing);
            }

            instance.m_22118_(new AttributeModifier(id, "celestweave_movement_assist", amount, operation));
         }
      }
   }
}
