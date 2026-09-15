package com.moakiee.ae2lt.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.moakiee.ae2lt.celestweave.BaseCelestweaveArmorItem;
import com.moakiee.ae2lt.celestweave.CelestweaveArmorState;
import com.moakiee.ae2lt.celestweave.phase.PhaseLockService;
import com.moakiee.ae2lt.item.PhaseLockProjectionItem;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({ServerGamePacketListenerImpl.class})
public abstract class ServerGamePacketListenerCelestweaveCreativeSyncMixin {
   @WrapOperation(
      method = {"handleSetCreativeModeSlot"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/inventory/Slot;setByPlayer(Lnet/minecraft/world/item/ItemStack;)V"
      )}
   )
   private void ae2lt$rejectStaleCelestweaveCreativeEcho(Slot slot, ItemStack uploaded, Operation<Void> original) {
      ServerGamePacketListenerImpl listener = (ServerGamePacketListenerImpl)this;
      ItemStack authoritative = slot.m_7993_();
      if (!ae2lt$isSameCelestweaveEquipmentEcho(listener.f_9743_, authoritative, uploaded)) {
         original.call(new Object[]{slot, uploaded});
      } else {
         listener.f_9743_.f_36095_.m_150404_(slot.f_40219_, ItemStack.f_41583_);
      }
   }

   @Unique
   private static boolean ae2lt$isSameCelestweaveEquipmentEcho(ServerPlayer player, ItemStack authoritative, ItemStack uploaded) {
      if (authoritative.m_41619_() || uploaded.m_41619_() || authoritative.m_41720_() != uploaded.m_41720_()) {
         return false;
      } else if (!(authoritative.m_41720_() instanceof BaseCelestweaveArmorItem)) {
         return authoritative.m_41720_() instanceof PhaseLockProjectionItem projection
            ? PhaseLockService.hasPrivateArmor(player, projection.equipmentSlot())
            : false;
      } else {
         UUID authoritativeId = CelestweaveArmorState.getArmorId(authoritative);
         UUID uploadedId = CelestweaveArmorState.getArmorId(uploaded);
         return authoritativeId != null && authoritativeId.equals(uploadedId);
      }
   }
}
