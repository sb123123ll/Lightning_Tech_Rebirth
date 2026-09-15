package com.moakiee.ae2lt.celestweave;

import com.moakiee.ae2lt.celestweave.service.ArmorTickService;
import com.moakiee.ae2lt.client.CelestweaveArmorRenderExtensions;
import com.moakiee.ae2lt.device.DeviceItem;
import com.moakiee.ae2lt.device.DeviceKind;
import com.moakiee.ae2lt.util.DeviceHubTooltip;
import com.moakiee.ae2lt.util.EnergyText;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.ArmorItem.Type;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.Nullable;

public abstract class BaseCelestweaveArmorItem extends ArmorItem implements DeviceItem {
   private final ArmorPart armorPart;

   protected BaseCelestweaveArmorItem(ArmorPart armorPart, Properties properties) {
      super(CelestweaveArmorMaterials.CELESTWEAVE, armorType(armorPart), properties.m_41487_(1).m_41486_().setNoRepair());
      this.armorPart = armorPart;
   }

   public void initializeClient(Consumer<IClientItemExtensions> consumer) {
      consumer.accept(CelestweaveArmorRenderExtensions.INSTANCE);
   }

   public ArmorPart armorPart() {
      return this.armorPart;
   }

   @Override
   public DeviceKind deviceKind() {
      return this.armorPart.deviceKind();
   }

   public boolean m_8120_(ItemStack stack) {
      return true;
   }

   public InteractionResultHolder<ItemStack> m_7203_(Level level, Player player, InteractionHand hand) {
      return this.m_269277_(this, level, player, hand);
   }

   public void m_6883_(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
      super.m_6883_(stack, level, entity, slotId, isSelected);
      if (entity instanceof Player player) {
         CelestweaveArmorState.ensureArmorId(stack);
         if (!level.m_5776_()) {
            boolean equipped = player.m_6844_(equipmentSlot(this.armorPart)) == stack;
            ArmorTickService.tickEquipped(player, stack, equipped, player.m_9236_().m_9598_(), resolveDist(level));
         }
      }
   }

   public void m_7373_(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag tooltipFlag) {
      super.m_7373_(stack, level, tooltip, tooltipFlag);
      long current = ArmorEnergyBuffer.read(stack, level == null ? null : level.m_9598_());
      long capacity = ArmorEnergyBuffer.capacity(stack, level == null ? null : level.m_9598_());
      tooltip.add(EnergyText.storedFe(current, capacity));
      tooltip.add(Component.m_237115_("ae2lt.celestweave.tooltip.workbench"));
      tooltip.add(DeviceHubTooltip.openConfigHint());
   }

   public boolean m_142522_(ItemStack stack) {
      return true;
   }

   public int m_142158_(ItemStack stack) {
      long capacity = ArmorEnergyBuffer.capacity(stack);
      if (capacity <= 0L) {
         return 0;
      } else {
         double filled = (double)ArmorEnergyBuffer.read(stack) / (double)capacity;
         return Mth.m_14045_((int)Math.round(filled * 13.0), 0, 13);
      }
   }

   public int m_142159_(ItemStack stack) {
      return Mth.m_14169_(0.33333334F, 1.0F, 1.0F);
   }

   private static Dist resolveDist(Level level) {
      return level.m_5776_() ? Dist.CLIENT : Dist.DEDICATED_SERVER;
   }

   private static EquipmentSlot equipmentSlot(ArmorPart part) {
      return switch (part) {
         case HEAD -> EquipmentSlot.HEAD;
         case CHEST -> EquipmentSlot.CHEST;
         case LEGS -> EquipmentSlot.LEGS;
         case FEET -> EquipmentSlot.FEET;
      };
   }

   private static Type armorType(ArmorPart part) {
      return switch (part) {
         case HEAD -> Type.HELMET;
         case CHEST -> Type.CHESTPLATE;
         case LEGS -> Type.LEGGINGS;
         case FEET -> Type.BOOTS;
      };
   }
}
