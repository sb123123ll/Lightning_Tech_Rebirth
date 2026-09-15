package com.moakiee.ae2lt.mixin;

import appeng.menu.AEBaseMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({AEBaseMenu.class})
public interface AEBaseMenuAccessor {
   @Invoker(
      value = "isPlayerSideSlot",
      remap = false
   )
   boolean ae2lt$isPlayerSideSlot(Slot var1);
}
