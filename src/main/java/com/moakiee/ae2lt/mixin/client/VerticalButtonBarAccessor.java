package com.moakiee.ae2lt.mixin.client;

import appeng.client.gui.widgets.VerticalButtonBar;
import java.util.List;
import net.minecraft.client.gui.components.Button;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(
   value = {VerticalButtonBar.class},
   remap = false
)
public interface VerticalButtonBarAccessor {
   @Accessor("buttons")
   List<Button> ae2lt$getButtons();
}
