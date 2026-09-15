package com.moakiee.ae2lt.mixin;

import appeng.menu.me.items.PatternEncodingTermMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(
   value = {PatternEncodingTermMenu.class},
   remap = false
)
public interface PatternEncodingTermMenuAccessor {
   @Invoker("encodePattern")
   ItemStack ae2lt$encodePatternCandidate();
}
