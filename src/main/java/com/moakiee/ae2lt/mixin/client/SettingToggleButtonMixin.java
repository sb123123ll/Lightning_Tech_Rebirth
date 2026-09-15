package com.moakiee.ae2lt.mixin.client;

import appeng.api.config.Setting;
import appeng.client.gui.widgets.SettingToggleButton;
import com.moakiee.ae2lt.client.SettingToggleButtonAccess;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(
   value = {SettingToggleButton.class},
   remap = false
)
public abstract class SettingToggleButtonMixin implements SettingToggleButtonAccess {
   @Shadow
   @Final
   private Setting<?> buttonSetting;

   @Override
   public Setting<?> ae2lt$getSetting() {
      return this.buttonSetting;
   }
}
