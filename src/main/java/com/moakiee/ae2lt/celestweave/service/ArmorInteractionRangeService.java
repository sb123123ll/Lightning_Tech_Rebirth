package com.moakiee.ae2lt.celestweave.service;

import com.moakiee.ae2lt.celestweave.module.ReachSubmodule;
import com.moakiee.ae2lt.device.capability.DeviceCapability;
import java.util.List;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraftforge.common.ForgeMod;

public final class ArmorInteractionRangeService {
   private static final UUID BLOCK_RANGE_MODIFIER_ID = UUID.fromString("7f4b3a2c-1d5e-4a6b-8c9d-0e1f2a3b4c5d");
   private static final UUID ENTITY_RANGE_MODIFIER_ID = UUID.fromString("8a5c4b3d-2e6f-4b7c-9d0e-1f2a3b4c5d6e");

   private ArmorInteractionRangeService() {
   }

   public static void tick(ServerPlayer player, List<ArmorCapabilityCollector.ActiveCapability> capabilities) {
      double blockBonus = 0.0;
      double entityBonus = 0.0;

      for (ArmorCapabilityCollector.ActiveCapability active : capabilities) {
         if (active.capability() instanceof DeviceCapability.InteractionRange) {
            blockBonus = Math.max(blockBonus, ReachSubmodule.blockBonus(active.armor()));
            entityBonus = Math.max(entityBonus, ReachSubmodule.entityBonus(active.armor()));
         }
      }

      updateModifier(player, (Attribute)ForgeMod.BLOCK_REACH.get(), BLOCK_RANGE_MODIFIER_ID, blockBonus);
      updateModifier(player, (Attribute)ForgeMod.ENTITY_REACH.get(), ENTITY_RANGE_MODIFIER_ID, entityBonus);
   }

   private static void updateModifier(ServerPlayer player, Attribute attribute, UUID id, double amount) {
      AttributeInstance instance = player.m_21051_(attribute);
      if (instance != null) {
         AttributeModifier existing = instance.m_22111_(id);
         if (amount <= 0.0) {
            if (existing != null) {
               instance.m_22130_(existing);
            }
         } else if (existing == null || !(Math.abs(existing.m_22218_() - amount) < 1.0E-6) || existing.m_22217_() != Operation.ADDITION) {
            if (existing != null) {
               instance.m_22130_(existing);
            }

            instance.m_22118_(new AttributeModifier(id, "celestweave_reach_extension", amount, Operation.ADDITION));
         }
      }
   }
}
