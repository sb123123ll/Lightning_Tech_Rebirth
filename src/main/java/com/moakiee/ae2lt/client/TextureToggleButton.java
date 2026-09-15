package com.moakiee.ae2lt.client;

import appeng.client.gui.Icon;
import appeng.client.gui.style.Blitter;
import appeng.client.gui.widgets.IconButton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class TextureToggleButton extends IconButton {
   private final List<ResourceLocation> textures;
   private final List<List<Component>> tooltips;
   private final TextureToggleButton.Listener listener;
   private int stateIndex;

   public TextureToggleButton(TextureToggleButton.ButtonType type, TextureToggleButton.Listener listener) {
      super(btn -> listener.onChange(0));
      this.setDisableBackground(true);
      this.textures = type.textures;
      this.tooltips = new ArrayList<>(type.textures.size());

      for (int i = 0; i < type.textures.size(); i++) {
         this.tooltips.add(Collections.emptyList());
      }

      this.listener = listener;
   }

   private static ResourceLocation texture(String path) {
      return new ResourceLocation("ae2lt", "textures/gui/buttons/" + path + ".png");
   }

   public int getStateCount() {
      return this.textures.size();
   }

   public void setStateIndex(int index) {
      if (this.textures.isEmpty()) {
         this.stateIndex = 0;
      } else {
         if (index < 0) {
            index = 0;
         }

         if (index >= this.textures.size()) {
            index = this.textures.size() - 1;
         }

         this.stateIndex = index;
      }
   }

   public int getStateIndex() {
      return this.stateIndex;
   }

   public void setTooltipAt(int index, List<Component> lines) {
      if (index >= 0 && index < this.tooltips.size()) {
         this.tooltips.set(index, lines == null ? Collections.emptyList() : lines);
      }
   }

   public void setState(boolean isOn) {
      this.setStateIndex(isOn ? 1 : 0);
   }

   public void setTooltipOn(List<Component> lines) {
      this.setTooltipAt(1, lines);
   }

   public void setTooltipOff(List<Component> lines) {
      this.setTooltipAt(0, lines);
   }

   public void setEjectState() {
      this.setStateIndex(2);
   }

   public boolean isEjectState() {
      return this.stateIndex == 2 && this.textures.size() >= 3;
   }

   public void setTooltipEject(List<Component> lines) {
      this.setTooltipAt(2, lines);
   }

   protected Icon getIcon() {
      return Icon.TOOLBAR_BUTTON_BACKGROUND;
   }

   public void m_5691_() {
      this.listener.onChange(this.stateIndex);
   }

   public void m_87963_(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      if (this.f_93624_) {
         boolean wasActive = this.f_93623_;
         this.f_93623_ = true;
         super.m_87963_(guiGraphics, mouseX, mouseY, partialTick);
         this.f_93623_ = wasActive;
         if (!this.textures.isEmpty()) {
            int idx = Math.min(this.stateIndex, this.textures.size() - 1);
            Blitter blitter = Blitter.texture(this.textures.get(idx), 16, 16).src(0, 0, 16, 16);
            if (!this.f_93623_) {
               blitter.opacity(0.5F);
            }

            blitter.dest(this.m_252754_(), this.m_252907_()).blit(guiGraphics);
         }
      }
   }

   public List<Component> getTooltipMessage() {
      if (this.tooltips.isEmpty()) {
         return Collections.emptyList();
      } else {
         int idx = Math.min(this.stateIndex, this.tooltips.size() - 1);
         return this.tooltips.get(idx);
      }
   }

   public static enum ButtonType {
      MODE(TextureToggleButton.texture("wired_mode"), TextureToggleButton.texture("wireless_mode")),
      AUTO_RETURN(
         TextureToggleButton.texture("auto_input_off"), TextureToggleButton.texture("auto_input_on"), TextureToggleButton.texture("auto_input_ejection")
      ),
      WIRELESS_STRATEGY(TextureToggleButton.texture("single_target"), TextureToggleButton.texture("even_distribution")),
      FILTERED_IMPORT(TextureToggleButton.texture("filtered_import_off"), TextureToggleButton.texture("filtered_import_on")),
      SPEED(TextureToggleButton.texture("speed_normal"), TextureToggleButton.texture("speed_fast")),
      AUTO_EXPORT(TextureToggleButton.texture("auto_export_off"), TextureToggleButton.texture("auto_export_on")),
      AUTO_IMPORT(
         TextureToggleButton.texture("auto_input_off"), TextureToggleButton.texture("auto_input_on"), TextureToggleButton.texture("auto_input_ejection")
      ),
      OVERLOAD_MODE(TextureToggleButton.texture("overloaded_off"), TextureToggleButton.texture("overloaded_on")),
      CRYSTAL_CATALYZER_MODE(TextureToggleButton.texture("catalyzer_crystal_mode"), TextureToggleButton.texture("catalyzer_dust_mode")),
      FREQUENCY_BIND(TextureToggleButton.texture("frequency_select")),
      INVENTORY_MAINTENANCE(TextureToggleButton.texture("inventory_maintenance")),
      QUICK_BUILD(TextureToggleButton.texture("quick_build")),
      CPU_SELECTION(TextureToggleButton.texture("quick_build")),
      QUICK_COMPUTE(TextureToggleButton.texture("quick_compute_off"), TextureToggleButton.texture("quick_compute_on")),
      PATTERN_STORAGE_UPGRADE(TextureToggleButton.texture("pattern_storage_upgrade")),
      ADAPTIVE_BATCH(TextureToggleButton.texture("adaptive_batch_off"), TextureToggleButton.texture("adaptive_batch_on"));

      private final List<ResourceLocation> textures;

      private ButtonType(ResourceLocation texture) {
         this.textures = List.of(texture);
      }

      private ButtonType(ResourceLocation textureOff, ResourceLocation textureOn) {
         this.textures = List.of(textureOff, textureOn);
      }

      private ButtonType(ResourceLocation textureOff, ResourceLocation textureOn, ResourceLocation textureEject) {
         this.textures = List.of(textureOff, textureOn, textureEject);
      }
   }

   @FunctionalInterface
   public interface Listener {
      void onChange(int var1);
   }
}
