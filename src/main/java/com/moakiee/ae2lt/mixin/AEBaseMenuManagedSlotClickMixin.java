package com.moakiee.ae2lt.mixin;

import appeng.menu.AEBaseMenu;
import appeng.menu.SlotSemantics;
import com.moakiee.ae2lt.menu.LargeStackAppEngSlot;
import com.moakiee.ae2lt.menu.OverloadedPatternProviderMenu;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({AEBaseMenu.class})
public abstract class AEBaseMenuManagedSlotClickMixin {
   @Inject(
      method = {"clicked"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void ae2lt$handleManagedSlotClick(int slotId, int button, ClickType clickType, Player player, CallbackInfo ci) {
      AEBaseMenu menu = (AEBaseMenu)this;
      if (LargeStackAppEngSlot.handleMenuInteraction(menu, slotId, button, clickType, player)) {
         if (!menu.isClientSide()) {
            menu.m_38946_();
         }

         ci.cancel();
      } else {
         if (isOverloadedReturnSlotOffhandSwap(menu, slotId, button, clickType)) {
            ci.cancel();
         }
      }
   }

   private static boolean isOverloadedReturnSlotOffhandSwap(AEBaseMenu menu, int slotId, int button, ClickType clickType) {
      return menu instanceof OverloadedPatternProviderMenu && clickType == ClickType.SWAP && button == 40 && slotId >= 0 && slotId < menu.f_38839_.size()
         ? menu.getSlotSemantic(menu.m_38853_(slotId)) == SlotSemantics.STORAGE
         : false;
   }
}
