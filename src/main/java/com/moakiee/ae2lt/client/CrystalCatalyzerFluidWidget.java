package com.moakiee.ae2lt.client;

import appeng.client.gui.style.Blitter;
import appeng.client.gui.style.FluidBlitter;
import appeng.client.gui.widgets.ITooltip;
import appeng.core.localization.Tooltips;
import com.moakiee.ae2lt.menu.CrystalCatalyzerMenu;
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
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.ModList;

public class CrystalCatalyzerFluidWidget extends AbstractWidget implements ITooltip {
   public static final int TANK_INNER_WIDTH = 16;
   public static final int TANK_INNER_HEIGHT = 53;
   private static final int TANK_SRC_X = 26;
   private static final int TANK_SRC_Y = 18;
   private static final int MINOR_TICK_SRC_X = 39;
   private static final int MINOR_TICK_WIDTH = 3;
   private static final int[] MINOR_TICK_SRC_YS = new int[]{21, 31, 41, 51, 61};
   private static final int MAJOR_TICK_SRC_X = 37;
   private static final int MAJOR_TICK_WIDTH = 5;
   private static final int[] MAJOR_TICK_SRC_YS = new int[]{26, 36, 46, 56, 66};
   private static final ResourceLocation BACKGROUND_TEXTURE = new ResourceLocation("ae2lt", "textures/guis/crystal_catalyzer.png");
   private final Supplier<FluidStack> fluidSupplier;
   private final IntSupplier capacitySupplier;
   private final CrystalCatalyzerMenu menu;

   public CrystalCatalyzerFluidWidget(CrystalCatalyzerMenu menu, Supplier<FluidStack> fluidSupplier, IntSupplier capacitySupplier) {
      super(0, 0, 16, 53, Component.m_237119_());
      this.menu = menu;
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
         this.menu.clientClearFluidTank();
         this.playClickSound();
         return true;
      } else if (button == 0) {
         this.menu.clientExtractFluid();
         this.playClickSound();
         return true;
      } else if (button == 1) {
         this.menu.clientInsertFluid();
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
         filled = Math.max(0, Math.min(filled, this.f_93619_));
         if (filled > 0) {
            int y = this.m_252907_() + this.f_93619_;
            int remaining = filled;

            while (remaining > 0) {
               int chunk = Math.min(16, remaining);
               y -= chunk;
               FluidBlitter.create(fluid).dest(this.m_252754_(), y, this.f_93618_, chunk).blit(guiGraphics);
               remaining -= chunk;
            }
         }
      }

      for (int srcY : MINOR_TICK_SRC_YS) {
         this.drawTick(guiGraphics, 39, srcY, 3);
      }

      for (int srcY : MAJOR_TICK_SRC_YS) {
         this.drawTick(guiGraphics, 37, srcY, 5);
      }
   }

   private void drawTick(GuiGraphics guiGraphics, int srcX, int srcY, int tickWidth) {
      int offsetX = srcX - 26;
      int offsetY = srcY - 18;
      Blitter.texture(BACKGROUND_TEXTURE)
         .src(srcX, srcY, tickWidth, 1)
         .dest(this.m_252754_() + offsetX, this.m_252907_() + offsetY, tickWidth, 1)
         .blit(guiGraphics);
   }

   public List<Component> getTooltipMessage() {
      FluidStack fluid = this.fluidSupplier.get();
      int capacity = this.capacitySupplier.getAsInt();
      List<Component> lines = new ArrayList<>();
      if (fluid.isEmpty()) {
         lines.add(Component.m_237110_("ae2lt.gui.crystal_catalyzer.fluid.tooltip", new Object[]{0, capacity}).m_130948_(Tooltips.NUMBER_TEXT));
      } else {
         lines.add(fluid.getDisplayName());
         lines.add(Component.m_237119_());
         lines.add(Component.m_237110_("ae2lt.gui.crystal_catalyzer.fluid.tooltip", new Object[]{fluid.getAmount(), capacity}).m_130948_(Tooltips.NUMBER_TEXT));
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
