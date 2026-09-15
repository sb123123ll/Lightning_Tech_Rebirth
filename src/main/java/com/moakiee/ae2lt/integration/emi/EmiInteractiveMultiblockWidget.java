package com.moakiee.ae2lt.integration.emi;

import com.moakiee.ae2lt.integration.recipeviewer.multiblock.InteractiveMultiblockPreview;
import com.moakiee.ae2lt.integration.recipeviewer.multiblock.MultiblockStructureRecipe;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.Bounds;
import dev.emi.emi.api.widget.SlotWidget;
import dev.emi.emi.api.widget.Widget;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import org.joml.Vector3f;

final class EmiInteractiveMultiblockWidget extends Widget implements InteractiveMultiblockPreview.SlotDelegate {
   private static WeakReference<EmiInteractiveMultiblockWidget> activeWidget = new WeakReference<>(null);
   private final MultiblockStructureRecipe recipe;
   private final InteractiveMultiblockPreview preview;
   private final Bounds bounds;
   private final SlotWidget[] materialSlots;
   private final SlotWidget[] alternativeSlots = new SlotWidget[7];
   private SlotWidget selectedBlockSlot;
   private int lastMouseX;
   private int lastMouseY;
   private long lastTick = Long.MIN_VALUE;
   private Screen ownerScreen;
   private float screenOriginX;
   private float screenOriginY;

   EmiInteractiveMultiblockWidget(MultiblockStructureRecipe recipe, int width, int height) {
      this.recipe = recipe;
      this.bounds = new Bounds(0, 0, width, height);
      this.materialSlots = new SlotWidget[recipe.materials().size()];
      this.preview = new InteractiveMultiblockPreview(recipe, width, height, this);
   }

   public Bounds getBounds() {
      return this.bounds;
   }

   public void m_88315_(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
      this.lastMouseX = mouseX;
      this.lastMouseY = mouseY;
      this.ownerScreen = Minecraft.m_91087_().f_91080_;
      Vector3f origin = guiGraphics.m_280168_().m_85850_().m_252922_().transformPosition(new Vector3f(0.0F, 0.0F, 0.0F));
      this.screenOriginX = origin.x;
      this.screenOriginY = origin.y;
      if (activeWidget.get() != this) {
         activeWidget = new WeakReference<>(this);
      }

      long tick = Util.m_137550_() / 50L;
      if (tick != this.lastTick) {
         this.preview.tick();
         this.lastTick = tick;
      }

      this.preview.drawWidget(guiGraphics, (double)mouseX, (double)mouseY);
   }

   public List<ClientTooltipComponent> getTooltip(int mouseX, int mouseY) {
      SlotWidget slot = this.preview.getSlotUnderMouse((double)mouseX, (double)mouseY).map(this::slotFor).orElse(null);
      if (slot != null) {
         List<ClientTooltipComponent> tooltip = slot.getTooltip(mouseX, mouseY);
         if (!tooltip.isEmpty()) {
            return tooltip;
         }
      }

      return toClientTooltip(this.preview.getTooltip((double)mouseX, (double)mouseY));
   }

   public boolean mouseClicked(int mouseX, int mouseY, int button) {
      SlotWidget slot = this.preview.getSlotUnderMouse((double)mouseX, (double)mouseY).map(this::slotFor).orElse(null);
      return slot != null && slot.mouseClicked(mouseX, mouseY, button) ? true : this.preview.handleMouseClick((double)mouseX, (double)mouseY, button, false);
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      SlotWidget slot = this.preview.getSlotUnderMouse((double)this.lastMouseX, (double)this.lastMouseY).map(this::slotFor).orElse(null);
      return slot != null && slot.keyPressed(keyCode, scanCode, modifiers);
   }

   boolean handleMouseDragged(int mouseX, int mouseY, int button, double dragX, double dragY) {
      return this.preview.handleMouseDragged((double)mouseX, (double)mouseY, button, dragX, dragY);
   }

   boolean handleMouseScrolled(int mouseX, int mouseY, double scrollX, double scrollY) {
      return this.preview.handleMouseScrolled((double)mouseX, (double)mouseY, scrollX, scrollY);
   }

   static boolean routeMouseDragged(Screen screen, double mouseX, double mouseY, int button, double dragX, double dragY) {
      EmiInteractiveMultiblockWidget widget = activeWidget.get();
      if (widget != null && widget.ownerScreen == screen) {
         int localX = (int)Math.floor(mouseX - (double)widget.screenOriginX);
         int localY = (int)Math.floor(mouseY - (double)widget.screenOriginY);
         return widget.bounds.contains(localX, localY) && widget.handleMouseDragged(localX, localY, button, dragX, dragY);
      } else {
         return false;
      }
   }

   static boolean routeMouseScrolled(Screen screen, double mouseX, double mouseY, double scrollX, double scrollY) {
      EmiInteractiveMultiblockWidget widget = activeWidget.get();
      if (widget != null && widget.ownerScreen == screen) {
         int localX = (int)Math.floor(mouseX - (double)widget.screenOriginX);
         int localY = (int)Math.floor(mouseY - (double)widget.screenOriginY);
         return widget.bounds.contains(localX, localY) && widget.handleMouseScrolled(localX, localY, scrollX, scrollY);
      } else {
         return false;
      }
   }

   @Override
   public int materialSlotCount() {
      return this.materialSlots.length;
   }

   @Override
   public int alternativeSlotCount() {
      return this.alternativeSlots.length;
   }

   @Override
   public void drawMaterialSlot(GuiGraphics guiGraphics, int index, int x, int y) {
      MultiblockStructureRecipe.MaterialEntry material = this.recipe.materials().get(index);
      SlotWidget slot = new SlotWidget(EmiStack.of(material.block()), x, y)
         .appendTooltip(Component.m_237110_("jei.ae2lt.multiblock.count", new Object[]{material.count()}));
      if (!material.note().getString().isEmpty()) {
         slot.appendTooltip(material.note());
      }

      this.materialSlots[index] = slot;
      slot.m_88315_(guiGraphics, this.lastMouseX, this.lastMouseY, 0.0F);
   }

   @Override
   public void drawSelectedBlockSlot(GuiGraphics guiGraphics, Block block, int x, int y) {
      this.selectedBlockSlot = new SlotWidget(EmiStack.of(block), x, y);
      this.selectedBlockSlot.m_88315_(guiGraphics, this.lastMouseX, this.lastMouseY, 0.0F);
   }

   @Override
   public void drawAlternativeSlot(GuiGraphics guiGraphics, int index, Block block, int x, int y) {
      SlotWidget slot = new SlotWidget(EmiStack.of(block), x, y);
      this.alternativeSlots[index] = slot;
      slot.m_88315_(guiGraphics, this.lastMouseX, this.lastMouseY, 0.0F);
   }

   private SlotWidget slotFor(InteractiveMultiblockPreview.SlotReference reference) {
      return switch (reference.kind()) {
         case MATERIAL -> this.materialSlots[reference.index()];
         case SELECTED -> this.selectedBlockSlot;
         case ALTERNATIVE -> this.alternativeSlots[reference.index()];
      };
   }

   private static List<ClientTooltipComponent> toClientTooltip(List<Component> components) {
      List<ClientTooltipComponent> tooltip = new ArrayList<>(components.size());

      for (Component component : components) {
         tooltip.add(ClientTooltipComponent.m_169948_(component.m_7532_()));
      }

      return tooltip;
   }
}
