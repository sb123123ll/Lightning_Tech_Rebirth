package com.moakiee.ae2lt.integration.recipeviewer.multiblock;

import appeng.client.render.overlay.OverlayRenderType;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraftforge.client.model.data.ModelData;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class InteractiveMultiblockPreview {
   public static final int DEFAULT_WIDTH = 300;
   public static final int DEFAULT_HEIGHT = 184;
   public static final int MAX_ALTERNATIVE_SLOTS = 7;
   private static final int NAME_Y = 1;
   private static final int TOOLBAR_Y = 14;
   private static final int TOOLBAR_HEIGHT = 16;
   private static final int VIEW_X = 2;
   private static final int VIEW_Y = 31;
   private static final int PANEL_WIDTH = 105;
   private static final int FOOTER_HEIGHT = 17;
   private static final int PANEL_TAB_HEIGHT = 15;
   private static final int MATERIAL_ROW_HEIGHT = 26;
   private static final float DEFAULT_YAW = 225.0F;
   private static final float DEFAULT_PITCH = 30.0F;
   private static final float MIN_ZOOM = 0.35F;
   private static final float MAX_ZOOM = 3.5F;
   private static final float AUTO_ROTATION_PER_TICK = 0.35F;
   private static final float MODEL_Z = 80.0F;
   private static final float DEPTH_SCALE_FACTOR = 0.1F;
   private static final int PANEL_BACKGROUND = -15198184;
   private static final int VIEW_BACKGROUND = -15724528;
   private static final int BORDER_COLOR = -10329502;
   private static final int HOVER_COLOR = -13290187;
   private static final int BUTTON_COLOR = -14277082;
   private static final int TEXT_COLOR = -1;
   private static final int MUTED_TEXT_COLOR = -3092272;
   private static final int FOOTER_TEXT_COLOR = -16777216;
   private static final int SELECTED_GLOW_ALPHA = 132;
   private static final int HOVERED_GLOW_ALPHA = 190;
   private final MultiblockStructureRecipe recipe;
   private final int width;
   private final int height;
   private final int panelX;
   private final int panelY;
   private final int panelHeight;
   private final int viewWidth;
   private final int viewHeight;
   private final float baseScale;
   private final InteractiveMultiblockPreview.SlotDelegate slots;
   private final InteractiveMultiblockPreview.UiRect resetButton = new InteractiveMultiblockPreview.UiRect(2, 14, 16, 16);
   private final InteractiveMultiblockPreview.UiRect autoButton = new InteractiveMultiblockPreview.UiRect(20, 14, 16, 16);
   private final InteractiveMultiblockPreview.UiRect shellButton = new InteractiveMultiblockPreview.UiRect(38, 14, 16, 16);
   private final InteractiveMultiblockPreview.UiRect layerModeButton = new InteractiveMultiblockPreview.UiRect(56, 14, 16, 16);
   private final InteractiveMultiblockPreview.UiRect layerDownButton = new InteractiveMultiblockPreview.UiRect(76, 14, 16, 16);
   private final InteractiveMultiblockPreview.UiRect layerUpButton = new InteractiveMultiblockPreview.UiRect(145, 14, 16, 16);
   private float yaw = 225.0F;
   private float pitch = 30.0F;
   private float zoom = 1.0F;
   private float panX;
   private float panY;
   private boolean autoRotate = true;
   private boolean hideShell;
   private InteractiveMultiblockPreview.LayerMode layerMode = InteractiveMultiblockPreview.LayerMode.FULL;
   private int layer;
   private int materialScroll;
   private InteractiveMultiblockPreview.PanelTab panelTab = InteractiveMultiblockPreview.PanelTab.MATERIALS;
   private MultiblockStructureRecipe.Cell selectedCell;
   private MultiblockStructureRecipe.Cell hoveredCell;
   private InteractiveMultiblockPreview.UiRect transientPressedButton;
   private int transientPressedTicks;

   public InteractiveMultiblockPreview(MultiblockStructureRecipe recipe, int width, int height, InteractiveMultiblockPreview.SlotDelegate slots) {
      this.recipe = recipe;
      this.width = width;
      this.height = height;
      this.panelX = width - 105 - 2;
      this.panelY = 31;
      this.panelHeight = height - 31 - 17;
      this.viewWidth = this.panelX - 2 - 4;
      this.viewHeight = this.panelHeight;
      this.layer = (recipe.sizeY() - 1) / 2;
      this.slots = slots;
      float horizontal = (float)Math.hypot((double)recipe.sizeX(), (double)recipe.sizeZ());
      float projectedHeight = (float)recipe.sizeY() * 0.866F + horizontal * 0.5F;
      float widthScale = (float)this.viewWidth * 0.9F / Math.max(1.0F, horizontal);
      float heightScale = (float)this.viewHeight * 0.9F / Math.max(1.0F, projectedHeight);
      this.baseScale = Math.min(widthScale, heightScale);
   }

   public void drawWidget(GuiGraphics guiGraphics, double mouseX, double mouseY) {
      Minecraft minecraft = Minecraft.m_91087_();
      Font font = minecraft.f_91062_;
      this.hoveredCell = inside(2, 31, this.viewWidth, this.viewHeight, mouseX, mouseY) ? this.pickCell(mouseX, mouseY) : null;
      this.drawHeader(guiGraphics, font);
      this.drawToolbar(guiGraphics, font, mouseX, mouseY);
      this.drawViewport(guiGraphics);
      this.drawPanel(guiGraphics, font, mouseX, mouseY);
      this.drawFooter(guiGraphics, font);
   }

   private void drawHeader(GuiGraphics guiGraphics, Font font) {
      drawTrimmed(guiGraphics, font, this.recipe.title(), 2, 1, this.width - 70, -1);
      String dimensions = this.recipe.sizeX() + "x" + this.recipe.sizeY() + "x" + this.recipe.sizeZ();
      guiGraphics.m_280056_(font, dimensions, this.width - font.m_92895_(dimensions) - 2, 1, -3092272, false);
   }

   private void drawToolbar(GuiGraphics guiGraphics, Font font, double mouseX, double mouseY) {
      this.drawControlButton(guiGraphics, this.resetButton, MultiblockPreviewControls.PixelIcon.RESET_VIEW, false, true, mouseX, mouseY);
      this.drawControlButton(
         guiGraphics,
         this.autoButton,
         this.autoRotate ? MultiblockPreviewControls.PixelIcon.PAUSE : MultiblockPreviewControls.PixelIcon.PLAY,
         this.autoRotate,
         true,
         mouseX,
         mouseY
      );
      this.drawControlButton(
         guiGraphics,
         this.shellButton,
         this.hideShell ? MultiblockPreviewControls.PixelIcon.SHELL_HIDDEN : MultiblockPreviewControls.PixelIcon.SHELL,
         this.hideShell,
         true,
         mouseX,
         mouseY
      );
      this.drawControlButton(
         guiGraphics, this.layerModeButton, this.layerMode.icon(), this.layerMode != InteractiveMultiblockPreview.LayerMode.FULL, true, mouseX, mouseY
      );
      this.drawControlButton(guiGraphics, this.layerDownButton, MultiblockPreviewControls.PixelIcon.MINUS, false, this.layer > 0, mouseX, mouseY);
      this.drawControlButton(
         guiGraphics, this.layerUpButton, MultiblockPreviewControls.PixelIcon.PLUS, false, this.layer < this.recipe.sizeY() - 1, mouseX, mouseY
      );
      InteractiveMultiblockPreview.UiRect layerLabel = this.layerLabelRect();
      Component text = Component.m_237110_("jei.ae2lt.multiblock.layer", new Object[]{this.layer + 1, this.recipe.sizeY()});
      MultiblockPreviewControls.drawInsetLabel(guiGraphics, font, layerLabel.x(), layerLabel.y(), layerLabel.width(), layerLabel.height(), text);
   }

   private void drawControlButton(
      GuiGraphics guiGraphics,
      InteractiveMultiblockPreview.UiRect rect,
      MultiblockPreviewControls.PixelIcon icon,
      boolean pressed,
      boolean enabled,
      double mouseX,
      double mouseY
   ) {
      boolean transientPressed = this.transientPressedTicks > 0 && rect.equals(this.transientPressedButton);
      MultiblockPreviewControls.drawIconButton(
         guiGraphics,
         rect.x(),
         rect.y(),
         rect.width(),
         rect.height(),
         icon,
         pressed || transientPressed,
         transientPressed,
         enabled,
         rect.contains(mouseX, mouseY)
      );
   }

   private void drawViewport(GuiGraphics guiGraphics) {
      guiGraphics.m_280509_(2, 31, 2 + this.viewWidth, 31 + this.viewHeight, -15724528);
      guiGraphics.m_280637_(2, 31, this.viewWidth, this.viewHeight, -10329502);
      enableLocalScissor(guiGraphics, 3, 32, 2 + this.viewWidth - 1, 31 + this.viewHeight - 1);
      Minecraft client = Minecraft.m_91087_();
      BufferSource bufferSource = client.m_91269_().m_110104_();
      BlockRenderDispatcher blockRenderer = client.m_91289_();
      PoseStack pose = guiGraphics.m_280168_();
      float scale = this.renderScale();
      pose.m_85836_();
      pose.m_252880_(this.viewCenterX() + this.panX, this.viewCenterY() + this.panY, 80.0F);
      pose.m_85841_(scale, -scale, scale * 0.1F);
      pose.m_252781_(Axis.f_252529_.m_252977_(this.pitch));
      pose.m_252781_(Axis.f_252436_.m_252977_(this.yaw));
      pose.m_252880_((float)(-this.recipe.sizeX()) / 2.0F, (float)(-this.recipe.sizeY()) / 2.0F, (float)(-this.recipe.sizeZ()) / 2.0F);

      for (MultiblockStructureRecipe.Cell cell : this.recipe.cells()) {
         if (this.isVisible(cell) && cell.state().m_60799_() != RenderShape.ENTITYBLOCK_ANIMATED) {
            pose.m_85836_();
            pose.m_252880_((float)cell.localPos().m_123341_(), (float)cell.localPos().m_123342_(), (float)cell.localPos().m_123343_());
            blockRenderer.renderSingleBlock(cell.state(), pose, bufferSource, 15728880, OverlayTexture.f_118083_, ModelData.EMPTY, null);
            pose.m_85849_();
         }
      }

      bufferSource.m_109911_();
      if (this.selectedCell != null && this.selectedCell != this.hoveredCell && this.isVisible(this.selectedCell)) {
         renderGlowCube(pose, bufferSource, this.selectedCell, 132);
      }

      if (this.hoveredCell != null && this.isVisible(this.hoveredCell)) {
         renderGlowCube(pose, bufferSource, this.hoveredCell, 190);
      }

      pose.m_85849_();
      bufferSource.m_109912_(OverlayRenderType.getBlockHilightFace());
      Lighting.m_84931_();
      guiGraphics.m_280618_();
   }

   private static void renderGlowCube(PoseStack pose, MultiBufferSource bufferSource, MultiblockStructureRecipe.Cell cell, int alpha) {
      VertexConsumer consumer = bufferSource.m_6299_(OverlayRenderType.getBlockHilightFace());
      pose.m_85836_();
      pose.m_252880_((float)cell.localPos().m_123341_(), (float)cell.localPos().m_123342_(), (float)cell.localPos().m_123343_());
      Matrix4f matrix = pose.m_85850_().m_252922_();
      float low = -0.025F;
      float high = 1.025F;
      glowQuad(consumer, matrix, alpha, low, low, low, high, low, low, high, low, high, low, low, high, 0.0F, -1.0F, 0.0F);
      glowQuad(consumer, matrix, alpha, low, high, high, high, high, high, high, high, low, low, high, low, 0.0F, 1.0F, 0.0F);
      glowQuad(consumer, matrix, alpha, low, low, low, low, high, low, high, high, low, high, low, low, 0.0F, 0.0F, -1.0F);
      glowQuad(consumer, matrix, alpha, high, low, high, high, high, high, low, high, high, low, low, high, 0.0F, 0.0F, 1.0F);
      glowQuad(consumer, matrix, alpha, low, low, high, low, high, high, low, high, low, low, low, low, -1.0F, 0.0F, 0.0F);
      glowQuad(consumer, matrix, alpha, high, low, low, high, high, low, high, high, high, high, low, high, 1.0F, 0.0F, 0.0F);
      pose.m_85849_();
   }

   private static void glowQuad(
      VertexConsumer consumer,
      Matrix4f matrix,
      int alpha,
      float x1,
      float y1,
      float z1,
      float x2,
      float y2,
      float z2,
      float x3,
      float y3,
      float z3,
      float x4,
      float y4,
      float z4,
      float normalX,
      float normalY,
      float normalZ
   ) {
      consumer.m_252986_(matrix, x1, y1, z1).m_6122_(255, 220, 0, alpha).m_5601_(normalX, normalY, normalZ).m_5752_();
      consumer.m_252986_(matrix, x2, y2, z2).m_6122_(255, 220, 0, alpha).m_5601_(normalX, normalY, normalZ).m_5752_();
      consumer.m_252986_(matrix, x3, y3, z3).m_6122_(255, 220, 0, alpha).m_5601_(normalX, normalY, normalZ).m_5752_();
      consumer.m_252986_(matrix, x4, y4, z4).m_6122_(255, 220, 0, alpha).m_5601_(normalX, normalY, normalZ).m_5752_();
   }

   private void drawPanel(GuiGraphics guiGraphics, Font font, double mouseX, double mouseY) {
      guiGraphics.m_280509_(this.panelX, this.panelY, this.panelX + 105, this.panelY + this.panelHeight, -15198184);
      guiGraphics.m_280637_(this.panelX, this.panelY, 105, this.panelHeight, -10329502);
      InteractiveMultiblockPreview.UiRect materialsTab = this.materialsTabRect();
      InteractiveMultiblockPreview.UiRect detailsTab = this.detailsTabRect();
      MultiblockPreviewControls.drawTextButton(
         guiGraphics,
         font,
         materialsTab.x(),
         materialsTab.y(),
         materialsTab.width(),
         materialsTab.height(),
         Component.m_237115_("jei.ae2lt.multiblock.materials"),
         this.panelTab == InteractiveMultiblockPreview.PanelTab.MATERIALS,
         materialsTab.contains(mouseX, mouseY)
      );
      MultiblockPreviewControls.drawTextButton(
         guiGraphics,
         font,
         detailsTab.x(),
         detailsTab.y(),
         detailsTab.width(),
         detailsTab.height(),
         Component.m_237115_("jei.ae2lt.multiblock.details"),
         this.panelTab == InteractiveMultiblockPreview.PanelTab.DETAILS,
         detailsTab.contains(mouseX, mouseY)
      );
      if (this.panelTab == InteractiveMultiblockPreview.PanelTab.MATERIALS) {
         this.drawMaterials(guiGraphics, font, mouseX, mouseY);
      } else {
         this.drawDetails(guiGraphics, font);
      }
   }

   private void drawMaterials(GuiGraphics guiGraphics, Font font, double mouseX, double mouseY) {
      int contentTop = this.panelContentTop();
      int contentBottom = this.panelContentBottom();
      enableLocalScissor(guiGraphics, this.panelX + 1, contentTop, this.panelX + 105 - 1, contentBottom);
      List<MultiblockStructureRecipe.MaterialEntry> materials = this.recipe.materials();
      int count = Math.min(materials.size(), this.slots.materialSlotCount());

      for (int i = 0; i < count; i++) {
         int rowY = this.materialRowY(i);
         if (rowY + 26 > contentTop && rowY < contentBottom) {
            boolean hovered = inside(this.panelX + 1, rowY, 103, 26, mouseX, mouseY);
            guiGraphics.m_280509_(this.panelX + 1, rowY, this.panelX + 105 - 1, rowY + 26, hovered ? -13290187 : (i % 2 == 0 ? -14671840 : -14935012));
            this.slots.drawMaterialSlot(guiGraphics, i, this.panelX + 4, rowY + 4);
            MultiblockStructureRecipe.MaterialEntry material = materials.get(i);
            int textX = this.panelX + 27;
            int textWidth = 71;
            drawTrimmed(guiGraphics, font, material.block().m_49954_(), textX, rowY + 3, textWidth, -1);
            drawTrimmed(
               guiGraphics, font, Component.m_237110_("jei.ae2lt.multiblock.count", new Object[]{material.count()}), textX, rowY + 14, textWidth, -3092272
            );
         }
      }

      guiGraphics.m_280618_();
      this.drawMaterialScrollbar(guiGraphics);
   }

   private void drawMaterialScrollbar(GuiGraphics guiGraphics) {
      int maxScroll = this.maxMaterialScroll();
      if (maxScroll > 0) {
         int trackTop = this.panelContentTop() + 1;
         int trackHeight = this.panelContentHeight() - 2;
         int trackX = this.panelX + 105 - 4;
         int contentHeight = this.recipe.materials().size() * 26;
         int thumbHeight = Math.max(12, trackHeight * this.panelContentHeight() / contentHeight);
         int thumbTravel = Math.max(0, trackHeight - thumbHeight);
         int thumbY = trackTop + (int)Math.round((double)this.materialScroll / (double)maxScroll * (double)thumbTravel);
         guiGraphics.m_280509_(trackX, trackTop, trackX + 2, trackTop + trackHeight, -13619152);
         guiGraphics.m_280509_(trackX, thumbY, trackX + 2, thumbY + thumbHeight, -5197648);
      }
   }

   private void drawDetails(GuiGraphics guiGraphics, Font font) {
      int contentTop = this.panelContentTop();
      if (this.selectedCell == null) {
         List<FormattedCharSequence> lines = font.m_92923_(Component.m_237115_("jei.ae2lt.multiblock.select_hint"), 95);
         int y = contentTop + 5;

         for (FormattedCharSequence line : lines) {
            guiGraphics.m_280649_(font, line, this.panelX + 5, y, -3092272, false);
            y += 9 + 2;
         }
      } else {
         Block selectedBlock = this.selectedCell.state().m_60734_();
         this.slots.drawSelectedBlockSlot(guiGraphics, selectedBlock, this.panelX + 4, contentTop + 2);
         drawTrimmed(guiGraphics, font, selectedBlock.m_49954_(), this.panelX + 25, contentTop + 3, 78, -1);
         drawTrimmed(
            guiGraphics,
            font,
            Component.m_237110_("jei.ae2lt.multiblock.role_label", new Object[]{this.selectedCell.role()}),
            this.panelX + 4,
            contentTop + 24,
            97,
            -3092272
         );
         drawTrimmed(
            guiGraphics,
            font,
            Component.m_237110_(
               "jei.ae2lt.multiblock.position",
               new Object[]{this.selectedCell.localPos().m_123341_(), this.selectedCell.localPos().m_123342_(), this.selectedCell.localPos().m_123343_()}
            ),
            this.panelX + 4,
            contentTop + 35,
            97,
            -3092272
         );
         guiGraphics.m_280614_(font, Component.m_237115_("jei.ae2lt.multiblock.replacements"), this.panelX + 4, contentTop + 48, -1, false);
         List<Block> alternatives = this.visibleAlternatives();
         boolean allowsAir = this.selectedCell.alternatives().contains(Blocks.f_50016_);
         if (alternatives.size() == 1 && !allowsAir) {
            guiGraphics.m_280614_(font, Component.m_237115_("jei.ae2lt.multiblock.fixed"), this.panelX + 4, contentTop + 62, -3092272, false);
         } else {
            for (int i = 0; i < Math.min(alternatives.size(), this.slots.alternativeSlotCount()); i++) {
               int x = this.panelX + 4 + i % 4 * 23;
               int y = contentTop + 63 + i / 4 * 21;
               this.slots.drawAlternativeSlot(guiGraphics, i, alternatives.get(i), x, y);
            }

            if (allowsAir) {
               InteractiveMultiblockPreview.UiRect airRect = this.airAlternativeRect();
               guiGraphics.m_280509_(airRect.x(), airRect.y(), airRect.right(), airRect.bottom(), -14277082);
               guiGraphics.m_280637_(airRect.x(), airRect.y(), airRect.width(), airRect.height(), -10329502);
               String airMarker = "X";
               guiGraphics.m_280056_(font, airMarker, airRect.x() + (airRect.width() - font.m_92895_(airMarker)) / 2, airRect.y() + 5, -3092272, false);
            }
         }

         if (!this.selectedCell.rules().isEmpty()) {
            int ruleY = this.panelContentBottom() - 9 - 2;
            drawTrimmed(guiGraphics, font, this.selectedCell.rules().get(0), this.panelX + 4, ruleY, 97, -3092272);
         }
      }
   }

   private void drawFooter(GuiGraphics guiGraphics, Font font) {
      Component controls = Component.m_237115_("jei.ae2lt.multiblock.controls");
      drawCenteredTrimmed(guiGraphics, font, controls, new InteractiveMultiblockPreview.UiRect(2, this.height - 17 + 3, this.width - 4, 9), -16777216);
   }

   public List<Component> getTooltip(double mouseX, double mouseY) {
      List<Component> tooltip = new ArrayList<>();
      Component toolbarTooltip = this.toolbarTooltip(mouseX, mouseY);
      if (toolbarTooltip != null) {
         tooltip.add(toolbarTooltip);
         return tooltip;
      } else if (inside(2, 31, this.viewWidth, this.viewHeight, mouseX, mouseY)) {
         MultiblockStructureRecipe.Cell hovered = this.pickCell(mouseX, mouseY);
         if (hovered != null) {
            tooltip.add(hovered.state().m_60734_().m_49954_());
            tooltip.add(Component.m_237110_("jei.ae2lt.multiblock.role_label", new Object[]{hovered.role()}));
            tooltip.add(Component.m_237115_("jei.ae2lt.multiblock.click_for_details"));
         }

         return tooltip;
      } else if (!inside(this.panelX, this.panelY, 105, this.panelHeight, mouseX, mouseY)) {
         return tooltip;
      } else {
         if (this.panelTab == InteractiveMultiblockPreview.PanelTab.DETAILS && this.selectedCell != null) {
            InteractiveMultiblockPreview.UiRect airRect = this.airAlternativeRect();
            if (airRect != null && airRect.contains(mouseX, mouseY)) {
               tooltip.add(Component.m_237115_("jei.ae2lt.multiblock.air"));
            } else if (mouseY >= (double)(this.panelContentBottom() - 14)) {
               tooltip.addAll(this.selectedCell.rules());
            }
         }

         return tooltip;
      }
   }

   public Optional<InteractiveMultiblockPreview.SlotReference> getSlotUnderMouse(double mouseX, double mouseY) {
      if (this.panelTab == InteractiveMultiblockPreview.PanelTab.MATERIALS) {
         if (!inside(this.panelX, this.panelContentTop(), 105, this.panelContentHeight(), mouseX, mouseY)) {
            return Optional.empty();
         } else {
            for (int i = 0; i < Math.min(this.recipe.materials().size(), this.slots.materialSlotCount()); i++) {
               int rowY = this.materialRowY(i);
               if (rowY + 26 > this.panelContentTop() && rowY < this.panelContentBottom() && inside(this.panelX + 4, rowY + 4, 18, 18, mouseX, mouseY)) {
                  return Optional.of(new InteractiveMultiblockPreview.SlotReference(InteractiveMultiblockPreview.SlotKind.MATERIAL, i));
               }
            }

            return Optional.empty();
         }
      } else if (this.selectedCell == null) {
         return Optional.empty();
      } else if (inside(this.panelX + 4, this.panelContentTop() + 2, 18, 18, mouseX, mouseY)) {
         return Optional.of(new InteractiveMultiblockPreview.SlotReference(InteractiveMultiblockPreview.SlotKind.SELECTED, 0));
      } else {
         int visibleCount = Math.min(this.visibleAlternatives().size(), this.slots.alternativeSlotCount());

         for (int ix = 0; ix < visibleCount; ix++) {
            int x = this.panelX + 4 + ix % 4 * 23;
            int y = this.panelContentTop() + 63 + ix / 4 * 21;
            if (inside(x, y, 18, 18, mouseX, mouseY)) {
               return Optional.of(new InteractiveMultiblockPreview.SlotReference(InteractiveMultiblockPreview.SlotKind.ALTERNATIVE, ix));
            }
         }

         return Optional.empty();
      }
   }

   public boolean handleMouseClick(double mouseX, double mouseY, int button, boolean simulate) {
      if (button != 0 && button != 2) {
         return false;
      } else if (button == 0 && this.getSlotUnderMouse(mouseX, mouseY).isPresent()) {
         return false;
      } else {
         boolean handled = this.isClickableArea(mouseX, mouseY, button);
         if (!handled || simulate) {
            return handled;
         } else if (button == 0 && this.handleToolbarClick(mouseX, mouseY)) {
            return true;
         } else if (button == 0 && this.materialsTabRect().contains(mouseX, mouseY)) {
            playButtonClick();
            this.panelTab = InteractiveMultiblockPreview.PanelTab.MATERIALS;
            return true;
         } else if (button == 0 && this.detailsTabRect().contains(mouseX, mouseY)) {
            playButtonClick();
            this.panelTab = InteractiveMultiblockPreview.PanelTab.DETAILS;
            return true;
         } else if (inside(2, 31, this.viewWidth, this.viewHeight, mouseX, mouseY)) {
            this.autoRotate = false;
            if (button == 0) {
               this.selectedCell = this.pickCell(mouseX, mouseY);
               this.panelTab = this.selectedCell == null ? InteractiveMultiblockPreview.PanelTab.MATERIALS : InteractiveMultiblockPreview.PanelTab.DETAILS;
            }

            return true;
         } else {
            return false;
         }
      }
   }

   public boolean handleMouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
      if (!inside(2, 31, this.viewWidth, this.viewHeight, mouseX, mouseY)) {
         return false;
      } else {
         if (button != 2 && (button != 0 || !Screen.m_96638_())) {
            if (button != 0) {
               return false;
            }

            this.yaw = wrapDegrees(this.yaw + (float)dragX * 0.8F);
            this.pitch = Mth.m_14036_(this.pitch + (float)dragY * 0.8F, -85.0F, 85.0F);
         } else {
            this.panX += (float)dragX;
            this.panY += (float)dragY;
            this.clampPan();
         }

         this.autoRotate = false;
         return true;
      }
   }

   public boolean handleMouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
      if (inside(2, 31, this.viewWidth, this.viewHeight, mouseX, mouseY)) {
         float oldZoom = this.zoom;
         this.zoom = Mth.m_14036_((float)((double)this.zoom * Math.exp(scrollY * 0.12)), 0.35F, 3.5F);
         float ratio = this.zoom / oldZoom;
         float relativeX = (float)mouseX - this.viewCenterX();
         float relativeY = (float)mouseY - this.viewCenterY();
         this.panX = relativeX - ratio * (relativeX - this.panX);
         this.panY = relativeY - ratio * (relativeY - this.panY);
         this.clampPan();
         this.autoRotate = false;
         return true;
      } else if (this.panelTab == InteractiveMultiblockPreview.PanelTab.MATERIALS
         && inside(this.panelX, this.panelContentTop(), 105, this.panelContentHeight(), mouseX, mouseY)) {
         this.materialScroll = Mth.m_14045_(this.materialScroll - (int)Math.round(scrollY * 14.0), 0, this.maxMaterialScroll());
         return true;
      } else {
         return false;
      }
   }

   public void tick() {
      if (this.transientPressedTicks > 0 && --this.transientPressedTicks == 0) {
         this.transientPressedButton = null;
      }

      if (this.autoRotate) {
         this.yaw = wrapDegrees(this.yaw + 0.35F);
      }
   }

   private boolean handleToolbarClick(double mouseX, double mouseY) {
      if (this.resetButton.contains(mouseX, mouseY)) {
         this.pressButton(this.resetButton);
         playButtonClick();
         this.resetCamera();
         return true;
      } else if (this.autoButton.contains(mouseX, mouseY)) {
         this.pressButton(this.autoButton);
         playButtonClick();
         this.autoRotate = !this.autoRotate;
         return true;
      } else if (this.shellButton.contains(mouseX, mouseY)) {
         this.pressButton(this.shellButton);
         playButtonClick();
         this.hideShell = !this.hideShell;
         return true;
      } else if (this.layerModeButton.contains(mouseX, mouseY)) {
         this.pressButton(this.layerModeButton);
         playButtonClick();
         this.layerMode = this.layerMode.next();
         return true;
      } else if (this.layerDownButton.contains(mouseX, mouseY)) {
         if (this.layer > 0) {
            this.pressButton(this.layerDownButton);
            playButtonClick();
            this.layer--;
         }

         return true;
      } else if (this.layerUpButton.contains(mouseX, mouseY)) {
         if (this.layer < this.recipe.sizeY() - 1) {
            this.pressButton(this.layerUpButton);
            playButtonClick();
            this.layer++;
         }

         return true;
      } else {
         return false;
      }
   }

   private void pressButton(InteractiveMultiblockPreview.UiRect button) {
      this.transientPressedButton = button;
      this.transientPressedTicks = 2;
   }

   private static void playButtonClick() {
      Minecraft.m_91087_().m_91106_().m_120367_(SimpleSoundInstance.m_119752_((SoundEvent)SoundEvents.f_12490_.m_203334_(), 1.0F));
   }

   private boolean isClickableArea(double mouseX, double mouseY, int button) {
      return button == 2
         ? inside(2, 31, this.viewWidth, this.viewHeight, mouseX, mouseY)
         : this.resetButton.contains(mouseX, mouseY)
            || this.autoButton.contains(mouseX, mouseY)
            || this.shellButton.contains(mouseX, mouseY)
            || this.layerModeButton.contains(mouseX, mouseY)
            || this.layerDownButton.contains(mouseX, mouseY)
            || this.layerUpButton.contains(mouseX, mouseY)
            || this.materialsTabRect().contains(mouseX, mouseY)
            || this.detailsTabRect().contains(mouseX, mouseY)
            || inside(2, 31, this.viewWidth, this.viewHeight, mouseX, mouseY);
   }

   private Component toolbarTooltip(double mouseX, double mouseY) {
      if (this.resetButton.contains(mouseX, mouseY)) {
         return Component.m_237115_("jei.ae2lt.multiblock.tooltip.reset");
      } else if (this.autoButton.contains(mouseX, mouseY)) {
         return Component.m_237115_("jei.ae2lt.multiblock.tooltip.auto");
      } else if (this.shellButton.contains(mouseX, mouseY)) {
         return Component.m_237115_("jei.ae2lt.multiblock.tooltip.shell");
      } else if (this.layerModeButton.contains(mouseX, mouseY)) {
         return Component.m_237115_("jei.ae2lt.multiblock.tooltip.layer_mode").m_130946_(" · ").m_7220_(this.layerMode.label());
      } else {
         return !this.layerDownButton.contains(mouseX, mouseY)
               && !this.layerUpButton.contains(mouseX, mouseY)
               && !this.layerLabelRect().contains(mouseX, mouseY)
            ? null
            : Component.m_237115_("jei.ae2lt.multiblock.tooltip.layer");
      }
   }

   private MultiblockStructureRecipe.Cell pickCell(double mouseX, double mouseY) {
      MultiblockStructureRecipe.Cell closest = null;
      float closestDepth = Float.NEGATIVE_INFINITY;

      for (MultiblockStructureRecipe.Cell cell : this.recipe.cells()) {
         if (this.isVisible(cell)) {
            InteractiveMultiblockPreview.ProjectedBounds bounds = this.project(cell);
            float padding = Math.max(1.0F, this.renderScale() * 0.08F);
            if (mouseX >= (double)(bounds.minX() - padding)
               && mouseX <= (double)(bounds.maxX() + padding)
               && mouseY >= (double)(bounds.minY() - padding)
               && mouseY <= (double)(bounds.maxY() + padding)
               && bounds.depth() > closestDepth) {
               closest = cell;
               closestDepth = bounds.depth();
            }
         }
      }

      return closest;
   }

   private InteractiveMultiblockPreview.ProjectedBounds project(MultiblockStructureRecipe.Cell cell) {
      Matrix4f transform = this.modelTransform();
      float minX = Float.POSITIVE_INFINITY;
      float minY = Float.POSITIVE_INFINITY;
      float maxX = Float.NEGATIVE_INFINITY;
      float maxY = Float.NEGATIVE_INFINITY;
      float x = (float)cell.localPos().m_123341_();
      float y = (float)cell.localPos().m_123342_();
      float z = (float)cell.localPos().m_123343_();

      for (int dx = 0; dx <= 1; dx++) {
         for (int dy = 0; dy <= 1; dy++) {
            for (int dz = 0; dz <= 1; dz++) {
               Vector3f point = new Vector3f(x + (float)dx, y + (float)dy, z + (float)dz);
               transform.transformPosition(point);
               minX = Math.min(minX, point.x);
               minY = Math.min(minY, point.y);
               maxX = Math.max(maxX, point.x);
               maxY = Math.max(maxY, point.y);
            }
         }
      }

      Vector3f center = new Vector3f(x + 0.5F, y + 0.5F, z + 0.5F);
      transform.transformPosition(center);
      return new InteractiveMultiblockPreview.ProjectedBounds(minX, minY, maxX, maxY, center.z);
   }

   private Matrix4f modelTransform() {
      float scale = this.renderScale();
      return new Matrix4f()
         .translation(this.viewCenterX() + this.panX, this.viewCenterY() + this.panY, 80.0F)
         .scale(scale, -scale, scale * 0.1F)
         .rotateX((float)Math.toRadians((double)this.pitch))
         .rotateY((float)Math.toRadians((double)this.yaw))
         .translate((float)(-this.recipe.sizeX()) / 2.0F, (float)(-this.recipe.sizeY()) / 2.0F, (float)(-this.recipe.sizeZ()) / 2.0F);
   }

   private boolean isVisible(MultiblockStructureRecipe.Cell cell) {
      if (!cell.state().m_60795_() && (!this.hideShell || !cell.shell())) {
         int y = cell.localPos().m_123342_();

         return switch (this.layerMode) {
            case FULL -> true;
            case UP_TO -> y <= this.layer;
            case SINGLE -> y == this.layer;
         };
      } else {
         return false;
      }
   }

   private List<Block> visibleAlternatives() {
      return this.selectedCell == null ? List.of() : this.selectedCell.alternatives().stream().filter(block -> block != Blocks.f_50016_).toList();
   }

   private InteractiveMultiblockPreview.UiRect airAlternativeRect() {
      if (this.selectedCell != null && this.selectedCell.alternatives().contains(Blocks.f_50016_)) {
         int index = Math.min(this.visibleAlternatives().size(), 7);
         return new InteractiveMultiblockPreview.UiRect(this.panelX + 4 + index % 4 * 23, this.panelContentTop() + 63 + index / 4 * 21, 18, 18);
      } else {
         return null;
      }
   }

   private void resetCamera() {
      this.yaw = 225.0F;
      this.pitch = 30.0F;
      this.zoom = 1.0F;
      this.panX = 0.0F;
      this.panY = 0.0F;
   }

   private void clampPan() {
      this.panX = Mth.m_14036_(this.panX, (float)(-this.viewWidth) * 0.65F, (float)this.viewWidth * 0.65F);
      this.panY = Mth.m_14036_(this.panY, (float)(-this.viewHeight) * 0.65F, (float)this.viewHeight * 0.65F);
   }

   private int materialRowY(int index) {
      return this.panelContentTop() + index * 26 - this.materialScroll;
   }

   private int maxMaterialScroll() {
      return Math.max(0, this.recipe.materials().size() * 26 - this.panelContentHeight());
   }

   private float renderScale() {
      return this.baseScale * this.zoom;
   }

   private float viewCenterX() {
      return 2.0F + (float)this.viewWidth / 2.0F;
   }

   private float viewCenterY() {
      return 31.0F + (float)this.viewHeight / 2.0F;
   }

   private int panelContentTop() {
      return this.panelY + 15 + 2;
   }

   private int panelContentBottom() {
      return this.panelY + this.panelHeight - 1;
   }

   private int panelContentHeight() {
      return this.panelContentBottom() - this.panelContentTop();
   }

   private InteractiveMultiblockPreview.UiRect materialsTabRect() {
      return new InteractiveMultiblockPreview.UiRect(this.panelX + 1, this.panelY + 1, 51, 15);
   }

   private InteractiveMultiblockPreview.UiRect detailsTabRect() {
      InteractiveMultiblockPreview.UiRect materials = this.materialsTabRect();
      return new InteractiveMultiblockPreview.UiRect(materials.right(), this.panelY + 1, 103 - materials.width(), 15);
   }

   private InteractiveMultiblockPreview.UiRect layerLabelRect() {
      return new InteractiveMultiblockPreview.UiRect(94, 14, 49, 16);
   }

   private static void drawTrimmed(GuiGraphics guiGraphics, Font font, Component text, int x, int y, int maxWidth, int color) {
      String value = text.getString();
      if (font.m_92895_(value) > maxWidth) {
         String ellipsis = "...";
         value = font.m_92834_(value, Math.max(0, maxWidth - font.m_92895_(ellipsis))) + ellipsis;
      }

      guiGraphics.m_280056_(font, value, x, y, color, false);
   }

   private static void drawCenteredTrimmed(GuiGraphics guiGraphics, Font font, Component text, InteractiveMultiblockPreview.UiRect rect, int color) {
      String value = text.getString();
      int maxWidth = Math.max(0, rect.width() - 4);
      if (font.m_92895_(value) > maxWidth) {
         value = font.m_92834_(value, maxWidth);
      }

      int x = rect.x() + (rect.width() - font.m_92895_(value)) / 2;
      int y = rect.y() + (rect.height() - 9) / 2 + 1;
      guiGraphics.m_280056_(font, value, x, y, color, false);
   }

   private static boolean inside(int x, int y, int width, int height, double mouseX, double mouseY) {
      return mouseX >= (double)x && mouseX < (double)(x + width) && mouseY >= (double)y && mouseY < (double)(y + height);
   }

   private static void enableLocalScissor(GuiGraphics guiGraphics, int left, int top, int right, int bottom) {
      Matrix4f pose = guiGraphics.m_280168_().m_85850_().m_252922_();
      Vector3f first = pose.transformPosition(new Vector3f((float)left, (float)top, 0.0F));
      Vector3f second = pose.transformPosition(new Vector3f((float)right, (float)bottom, 0.0F));
      int screenLeft = (int)Math.floor((double)Math.min(first.x, second.x));
      int screenTop = (int)Math.floor((double)Math.min(first.y, second.y));
      int screenRight = (int)Math.ceil((double)Math.max(first.x, second.x));
      int screenBottom = (int)Math.ceil((double)Math.max(first.y, second.y));
      guiGraphics.m_280588_(screenLeft, screenTop, screenRight, screenBottom);
   }

   private static float wrapDegrees(float degrees) {
      float wrapped = degrees % 360.0F;
      return wrapped < 0.0F ? wrapped + 360.0F : wrapped;
   }

   private static enum LayerMode {
      FULL("jei.ae2lt.multiblock.mode.full"),
      UP_TO("jei.ae2lt.multiblock.mode.up_to"),
      SINGLE("jei.ae2lt.multiblock.mode.single");

      private final String translationKey;

      private LayerMode(String translationKey) {
         this.translationKey = translationKey;
      }

      Component label() {
         return Component.m_237115_(this.translationKey);
      }

      MultiblockPreviewControls.PixelIcon icon() {
         return switch (this) {
            case FULL -> MultiblockPreviewControls.PixelIcon.LAYERS_FULL;
            case UP_TO -> MultiblockPreviewControls.PixelIcon.LAYERS_UP_TO;
            case SINGLE -> MultiblockPreviewControls.PixelIcon.LAYERS_SINGLE;
         };
      }

      InteractiveMultiblockPreview.LayerMode next() {
         return switch (this) {
            case FULL -> UP_TO;
            case UP_TO -> SINGLE;
            case SINGLE -> FULL;
         };
      }
   }

   private static enum PanelTab {
      MATERIALS,
      DETAILS;
   }

   private static record ProjectedBounds(float minX, float minY, float maxX, float maxY, float depth) {
   }

   public interface SlotDelegate {
      int materialSlotCount();

      int alternativeSlotCount();

      void drawMaterialSlot(GuiGraphics var1, int var2, int var3, int var4);

      void drawSelectedBlockSlot(GuiGraphics var1, Block var2, int var3, int var4);

      void drawAlternativeSlot(GuiGraphics var1, int var2, Block var3, int var4, int var5);
   }

   public static enum SlotKind {
      MATERIAL,
      SELECTED,
      ALTERNATIVE;
   }

   public static record SlotReference(InteractiveMultiblockPreview.SlotKind kind, int index) {
   }

   private static record UiRect(int x, int y, int width, int height) {
      int right() {
         return this.x + this.width;
      }

      int bottom() {
         return this.y + this.height;
      }

      boolean contains(double mouseX, double mouseY) {
         return InteractiveMultiblockPreview.inside(this.x, this.y, this.width, this.height, mouseX, mouseY);
      }
   }
}
