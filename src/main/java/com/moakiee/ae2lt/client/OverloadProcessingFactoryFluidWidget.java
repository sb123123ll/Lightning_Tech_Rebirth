package com.moakiee.ae2lt.client;

import appeng.client.gui.style.Blitter;
import appeng.client.gui.widgets.ITooltip;
import appeng.core.localization.Tooltips;
import com.moakiee.ae2lt.menu.OverloadProcessingFactoryMenu;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.ModList;

public class OverloadProcessingFactoryFluidWidget extends AbstractWidget implements ITooltip {
   private final Supplier<FluidStack> fluidSupplier;
   private final IntSupplier capacitySupplier;
   private final OverloadProcessingFactoryMenu menu;
   private final int tankIndex;
   private Fluid cachedFluid;
   private TextureAtlasSprite cachedSprite;

   public OverloadProcessingFactoryFluidWidget(
      OverloadProcessingFactoryMenu menu, int tankIndex, Supplier<FluidStack> fluidSupplier, IntSupplier capacitySupplier
   ) {
      super(0, 0, 16, 54, Component.m_237119_());
      this.menu = menu;
      this.tankIndex = tankIndex;
      this.fluidSupplier = fluidSupplier;
      this.capacitySupplier = capacitySupplier;
   }

   public boolean m_6375_(double mouseX, double mouseY, int button) {
      return this.f_93623_ && this.f_93624_ && this.m_5953_(mouseX, mouseY) && button == 0 ? this.handleClick(button) : false;
   }

   public boolean handleClick(int button) {
      if (!this.f_93623_ || !this.f_93624_) {
         return false;
      } else if (Screen.m_96638_()) {
         this.menu.clientClearFluidTank(this.tankIndex);
         this.playClickSound();
         return true;
      } else if (button == 0) {
         this.menu.clientExtractFluid(this.tankIndex);
         this.playClickSound();
         return true;
      } else if (button == 1) {
         this.menu.clientInsertFluid(this.tankIndex);
         this.playClickSound();
         return true;
      } else {
         return false;
      }
   }

   private void playClickSound() {
      Minecraft.m_91087_().m_91106_().m_120367_(SimpleSoundInstance.m_119752_((SoundEvent)SoundEvents.f_12490_.m_203334_(), 1.0F));
   }

   protected void m_87963_(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      FluidStack fluid = this.fluidSupplier.get();
      if (!fluid.isEmpty()) {
         int capacity = Math.max(1, this.capacitySupplier.getAsInt());
         int filled = (int)Math.round((double)this.f_93619_ * (double)fluid.getAmount() / (double)capacity);
         if (filled > 0) {
            filled = Math.min(filled, this.f_93619_);
            TextureAtlasSprite sprite = this.resolveSprite(fluid);
            if (sprite != null) {
               IClientFluidTypeExtensions attributes = IClientFluidTypeExtensions.of(fluid.getFluid());
               Blitter blitter = Blitter.sprite(sprite).colorRgb(attributes.getTintColor(fluid)).blending(true);
               int x = this.m_252754_();
               int yBottom = this.m_252907_() + this.f_93619_;
               int drawn = 0;

               while (drawn < filled) {
                  int sliceH = Math.min(this.f_93618_, filled - drawn);
                  blitter.dest(x, yBottom - drawn - sliceH, this.f_93618_, sliceH).blit(guiGraphics);
                  drawn += sliceH;
               }
            }
         }
      }
   }

   private TextureAtlasSprite resolveSprite(FluidStack stack) {
      Fluid fluid = stack.getFluid();
      if (fluid != this.cachedFluid) {
         this.cachedFluid = fluid;
         IClientFluidTypeExtensions attributes = IClientFluidTypeExtensions.of(fluid);
         this.cachedSprite = (TextureAtlasSprite)Minecraft.m_91087_().m_91258_(InventoryMenu.f_39692_).apply(attributes.getStillTexture(stack));
      }

      return this.cachedSprite;
   }

   public List<Component> getTooltipMessage() {
      FluidStack fluid = this.fluidSupplier.get();
      int capacity = this.capacitySupplier.getAsInt();
      List<Component> lines = new ArrayList<>();
      if (fluid.isEmpty()) {
         lines.add(Component.m_237110_("ae2lt.gui.overload_factory.fluid.tooltip", new Object[]{0, capacity}).m_130948_(Tooltips.NUMBER_TEXT));
      } else {
         lines.add(fluid.getDisplayName());
         lines.add(Component.m_237119_());
         lines.add(Component.m_237110_("ae2lt.gui.overload_factory.fluid.tooltip", new Object[]{fluid.getAmount(), capacity}).m_130948_(Tooltips.NUMBER_TEXT));
         lines.add(Component.m_237119_());
         lines.add(Component.m_237113_(getModDisplayName(fluid)).m_130944_(new ChatFormatting[]{ChatFormatting.BLUE, ChatFormatting.ITALIC}));
      }

      lines.add(Component.m_237119_());
      lines.add(Component.m_237115_("ae2lt.gui.fluid_tank.action.insert").m_130940_(ChatFormatting.DARK_GRAY));
      lines.add(Component.m_237115_("ae2lt.gui.fluid_tank.action.extract").m_130940_(ChatFormatting.DARK_GRAY));
      lines.add(Component.m_237115_("ae2lt.gui.fluid_tank.action.clear").m_130940_(ChatFormatting.DARK_GRAY));
      return lines;
   }

   public Rect2i getTooltipArea() {
      return new Rect2i(this.m_252754_(), this.m_252907_(), this.f_93618_, this.f_93619_);
   }

   public boolean isTooltipAreaVisible() {
      return true;
   }

   protected void m_168797_(NarrationElementOutput narrationElementOutput) {
   }

   private static String getModDisplayName(FluidStack fluid) {
      ResourceLocation key = fluid.getFluid() == Fluids.f_76191_ ? null : fluid.getFluid().m_205069_().m_205785_().m_135782_();
      if (key == null) {
         return "Minecraft";
      } else {
         String namespace = key.m_135827_();
         return "c".equals(namespace)
            ? "Common"
            : ModList.get()
               .getModContainerById(namespace)
               .map(container -> container.getModInfo().getDisplayName())
               .orElseGet(() -> namespace.replace('_', ' '));
      }
   }
}
