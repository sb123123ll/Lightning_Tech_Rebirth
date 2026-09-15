package com.moakiee.ae2lt.integration.jei.multiblock;

import com.moakiee.ae2lt.integration.recipeviewer.multiblock.InteractiveMultiblockPreview;
import com.moakiee.ae2lt.integration.recipeviewer.multiblock.MultiblockStructureRecipe;
import com.mojang.blaze3d.platform.InputConstants.Key;
import com.mojang.blaze3d.platform.InputConstants.Type;
import java.util.List;
import java.util.Optional;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawable;
import mezz.jei.api.gui.inputs.IJeiInputHandler;
import mezz.jei.api.gui.inputs.IJeiUserInput;
import mezz.jei.api.gui.inputs.RecipeSlotUnderMouse;
import mezz.jei.api.gui.widgets.ISlottedRecipeWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public final class InteractiveMultiblockWidget implements ISlottedRecipeWidget, IJeiInputHandler, InteractiveMultiblockPreview.SlotDelegate {
   public static final String MATERIAL_SLOT_PREFIX = "multiblock_material_";
   public static final String TRANSFER_SLOT_PREFIX = "multiblock_transfer_";
   public static final String SELECTED_BLOCK_SLOT = "multiblock_selected_block";
   public static final String ALTERNATIVE_SLOT_PREFIX = "multiblock_alternative_";
   public static final int MAX_ALTERNATIVE_SLOTS = 7;
   private static final ScreenPosition POSITION = new ScreenPosition(0, 0);
   private final InteractiveMultiblockPreview preview;
   private final ScreenRectangle area;
   private final List<IRecipeSlotDrawable> materialSlots;
   private final IRecipeSlotDrawable selectedBlockSlot;
   private final List<IRecipeSlotDrawable> alternativeSlots;

   public InteractiveMultiblockWidget(
      MultiblockStructureRecipe recipe,
      int width,
      int height,
      List<IRecipeSlotDrawable> materialSlots,
      IRecipeSlotDrawable selectedBlockSlot,
      List<IRecipeSlotDrawable> alternativeSlots
   ) {
      this.materialSlots = List.copyOf(materialSlots);
      this.selectedBlockSlot = selectedBlockSlot;
      this.alternativeSlots = List.copyOf(alternativeSlots);
      this.preview = new InteractiveMultiblockPreview(recipe, width, height, this);
      this.area = new ScreenRectangle(0, 0, width, height);
   }

   public ScreenPosition getPosition() {
      return POSITION;
   }

   public ScreenRectangle getArea() {
      return this.area;
   }

   public void drawWidget(GuiGraphics guiGraphics, double mouseX, double mouseY) {
      this.preview.drawWidget(guiGraphics, mouseX, mouseY);
   }

   public void getTooltip(ITooltipBuilder tooltip, double mouseX, double mouseY) {
      tooltip.addAll(this.preview.getTooltip(mouseX, mouseY));
   }

   public Optional<RecipeSlotUnderMouse> getSlotUnderMouse(double mouseX, double mouseY) {
      return this.preview.getSlotUnderMouse(mouseX, mouseY).map(this::slotFor).map(slot -> new RecipeSlotUnderMouse(slot, POSITION));
   }

   public boolean handleInput(double mouseX, double mouseY, IJeiUserInput input) {
      Key key = input.getKey();
      return key.m_84868_() != Type.MOUSE ? false : this.preview.handleMouseClick(mouseX, mouseY, key.m_84873_(), input.isSimulate());
   }

   public boolean handleMouseDragged(double mouseX, double mouseY, Key key, double dragX, double dragY) {
      return key.m_84868_() != Type.MOUSE ? false : this.preview.handleMouseDragged(mouseX, mouseY, key.m_84873_(), dragX, dragY);
   }

   public boolean handleMouseScrolled(double mouseX, double mouseY, double mouseV) {
      return this.preview.handleMouseScrolled(mouseX, mouseY, 0.0, mouseV);
   }

   public void tick() {
      this.preview.tick();
   }

   @Override
   public int materialSlotCount() {
      return this.materialSlots.size();
   }

   @Override
   public int alternativeSlotCount() {
      return this.alternativeSlots.size();
   }

   @Override
   public void drawMaterialSlot(GuiGraphics guiGraphics, int index, int x, int y) {
      IRecipeSlotDrawable slot = this.materialSlots.get(index);
      slot.setPosition(x, y);
      slot.draw(guiGraphics);
   }

   @Override
   public void drawSelectedBlockSlot(GuiGraphics guiGraphics, Block block, int x, int y) {
      setSlotStack(this.selectedBlockSlot, block, x, y);
      this.selectedBlockSlot.draw(guiGraphics);
   }

   @Override
   public void drawAlternativeSlot(GuiGraphics guiGraphics, int index, Block block, int x, int y) {
      IRecipeSlotDrawable slot = this.alternativeSlots.get(index);
      setSlotStack(slot, block, x, y);
      slot.draw(guiGraphics);
   }

   private IRecipeSlotDrawable slotFor(InteractiveMultiblockPreview.SlotReference reference) {
      return switch (reference.kind()) {
         case MATERIAL -> (IRecipeSlotDrawable)this.materialSlots.get(reference.index());
         case SELECTED -> this.selectedBlockSlot;
         case ALTERNATIVE -> (IRecipeSlotDrawable)this.alternativeSlots.get(reference.index());
      };
   }

   private static void setSlotStack(IRecipeSlotDrawable slot, Block block, int x, int y) {
      slot.setPosition(x, y);
      slot.clearDisplayOverrides();
      slot.createDisplayOverrides().addItemStack(new ItemStack(block));
   }
}
