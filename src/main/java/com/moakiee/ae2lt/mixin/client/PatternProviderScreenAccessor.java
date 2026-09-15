package com.moakiee.ae2lt.mixin.client;

import appeng.api.config.YesNo;
import appeng.client.gui.implementations.PatternProviderScreen;
import appeng.client.gui.widgets.SettingToggleButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(
   value = {PatternProviderScreen.class},
   remap = false
)
public interface PatternProviderScreenAccessor {
   @Accessor("blockingModeButton")
   SettingToggleButton<YesNo> ae2lt$getBlockingModeButton();
}
