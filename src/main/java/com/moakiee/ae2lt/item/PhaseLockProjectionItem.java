package com.moakiee.ae2lt.item;

import com.moakiee.ae2lt.celestweave.CelestweaveArmorMaterials;
import com.moakiee.ae2lt.celestweave.PhaseWingFlight;
import com.moakiee.ae2lt.celestweave.phase.PhaseLockProjectionRules;
import com.moakiee.ae2lt.celestweave.phase.PhaseLockService;
import com.moakiee.ae2lt.client.CelestweaveArmorRenderExtensions;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.ArmorItem.Type;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.Nullable;

public final class PhaseLockProjectionItem extends ArmorItem {
   private final EquipmentSlot equipmentSlot;

   public PhaseLockProjectionItem(Properties properties, EquipmentSlot equipmentSlot) {
      super(CelestweaveArmorMaterials.CELESTWEAVE, armorType(equipmentSlot), properties.m_41487_(1).m_41486_());
      this.equipmentSlot = equipmentSlot;
   }

   public EquipmentSlot getEquipmentSlot(ItemStack stack) {
      return this.equipmentSlot;
   }

   public EquipmentSlot equipmentSlot() {
      return this.equipmentSlot;
   }

   public SoundEvent m_150681_() {
      return SoundEvents.f_271165_;
   }

   public void initializeClient(Consumer<IClientItemExtensions> consumer) {
      consumer.accept(CelestweaveArmorRenderExtensions.INSTANCE);
   }

   public boolean canElytraFly(ItemStack stack, LivingEntity entity) {
      return this.equipmentSlot == EquipmentSlot.CHEST && PhaseWingFlight.canElytraFly(entity);
   }

   public boolean elytraFlightTick(ItemStack stack, LivingEntity entity, int flightTicks) {
      return this.equipmentSlot == EquipmentSlot.CHEST && PhaseWingFlight.elytraFlightTick(entity);
   }

   public String m_5524_() {
      return "item.ae2lt.phase_lock_projection";
   }

   public void m_6883_(ItemStack stack, Level level, Entity entity, int slotId, boolean selected) {
      super.m_6883_(stack, level, entity, slotId, selected);
      if (!level.m_5776_() && entity instanceof ServerPlayer player) {
         if (!PhaseLockProjectionRules.isExpectedSlot(this.equipmentSlot, slotId)) {
            stack.m_41764_(0);
         } else {
            if (!PhaseLockService.hasPrivateArmor(player, this.equipmentSlot)) {
               stack.m_41764_(0);
               level.m_6263_(
                  null, player.m_20185_(), player.m_20186_(), player.m_20189_(), (SoundEvent)SoundEvents.f_12377_.m_203334_(), SoundSource.PLAYERS, 0.8F, 0.7F
               );
            }
         }
      }
   }

   public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
      entity.m_146870_();
      return true;
   }

   public boolean m_5812_(ItemStack stack) {
      return false;
   }

   public void m_7373_(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
      tooltip.add(Component.m_237115_("item.ae2lt.phase_lock_projection.desc"));
   }

   private static Type armorType(EquipmentSlot slot) {
      return switch (slot) {
         case HEAD -> Type.HELMET;
         case CHEST -> Type.CHESTPLATE;
         case LEGS -> Type.LEGGINGS;
         case FEET -> Type.BOOTS;
         default -> throw new IllegalArgumentException("Phase-lock projections require an armor slot");
      };
   }
}
