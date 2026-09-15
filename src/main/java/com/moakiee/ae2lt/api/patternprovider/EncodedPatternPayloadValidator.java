package com.moakiee.ae2lt.api.patternprovider;

import net.minecraft.world.item.ItemStack;

@FunctionalInterface
public interface EncodedPatternPayloadValidator {
   boolean hasEncodedPatternPayload(ItemStack var1);
}
