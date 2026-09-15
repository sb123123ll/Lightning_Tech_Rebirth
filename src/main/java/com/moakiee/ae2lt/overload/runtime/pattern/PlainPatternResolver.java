package com.moakiee.ae2lt.overload.runtime.pattern;

import net.minecraft.world.item.ItemStack;

@FunctionalInterface
public interface PlainPatternResolver {
   ParsedPatternDefinition resolve(ItemStack var1);
}
