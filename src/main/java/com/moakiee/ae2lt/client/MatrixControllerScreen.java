package com.moakiee.ae2lt.client;

import com.moakiee.ae2lt.logic.craft.MatrixCoreMode;
import com.moakiee.ae2lt.logic.craft.MatrixMultiblockScanIssue;
import com.moakiee.ae2lt.menu.MatrixControllerMenu;
import com.moakiee.ae2lt.network.MatrixControllerActionPacket;
import com.moakiee.ae2lt.network.NetworkInit;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class MatrixControllerScreen extends MultiblockControllerScreen<MatrixControllerMenu> {
   public MatrixControllerScreen(MatrixControllerMenu menu, Inventory playerInventory, Component title) {
      super(menu, playerInventory, title);
   }

   protected void m_7856_() {
      super.m_7856_();
      int x = this.f_97735_ - 18;
      int y = this.f_97736_;
      TextureToggleButton build = new TextureToggleButton(
         TextureToggleButton.ButtonType.QUICK_BUILD, state -> this.sendAction(MatrixControllerActionPacket.Action.AUTO_BUILD)
      );
      build.m_257544_(Tooltip.m_257550_(Component.m_237115_("ae2lt.matrix.gui.build")));
      build.m_264152_(x, y);
      this.m_142416_(build);
      TextureToggleButton upgrade = new TextureToggleButton(
         TextureToggleButton.ButtonType.PATTERN_STORAGE_UPGRADE, state -> this.sendAction(MatrixControllerActionPacket.Action.UPGRADE_PATTERN_STORAGE)
      );
      upgrade.m_257544_(Tooltip.m_257550_(Component.m_237115_("ae2lt.matrix.gui.upgrade")));
      upgrade.m_264152_(x, y + 22);
      this.m_142416_(upgrade);
   }

   private void sendAction(MatrixControllerActionPacket.Action action) {
      NetworkInit.sendToServer(
         new MatrixControllerActionPacket(((MatrixControllerMenu)this.f_97732_).token(), ((MatrixControllerMenu)this.f_97732_).getBlockPos(), action)
      );
   }

   protected void m_280003_(GuiGraphics guiGraphics, int mouseX, int mouseY) {
      this.drawTitle(guiGraphics);
      if (!((MatrixControllerMenu)this.f_97732_).isFormed()) {
         this.drawStatus(guiGraphics, Component.m_237115_("ae2lt.matrix.gui.header_unformed"), 8019476);
         this.drawUnformed(guiGraphics, this.issueText(), "ae2lt.matrix.gui.hint_unformed");
      } else {
         this.drawStatus(guiGraphics, Component.m_237110_("ae2lt.matrix.gui.header_formed", new Object[]{this.modeName()}), 1862452);
         boolean multidimensional = ((MatrixControllerMenu)this.f_97732_).getMode() == MatrixCoreMode.MULTIDIMENSIONAL;
         int y = 56;
         this.drawRow(
            guiGraphics,
            y,
            Component.m_237115_("ae2lt.matrix.gui.label_throughput"),
            multidimensional
               ? I18n.m_118938_("ae2lt.matrix.gui.value_unbounded", new Object[0])
               : formatCount((long)((MatrixControllerMenu)this.f_97732_).getOperationsPerTick()) + " op/t",
            multidimensional ? 2051705 : 1316383
         );
         y += 13;
         if (!multidimensional) {
            this.drawRow(
               guiGraphics,
               y,
               Component.m_237115_("ae2lt.matrix.gui.label_efficiency"),
               percent(((MatrixControllerMenu)this.f_97732_).getEfficiencyFactor()),
               1316383
            );
            y += 13;
         }

         this.drawRow(
            guiGraphics,
            y,
            Component.m_237115_("ae2lt.matrix.gui.label_patterns"),
            I18n.m_118938_(
               "ae2lt.matrix.gui.value_patterns",
               new Object[]{
                  formatCount((long)((MatrixControllerMenu)this.f_97732_).getPatternStorageCount()),
                  formatCount((long)((MatrixControllerMenu)this.f_97732_).getPatternSlotCount())
               }
            ),
            1316383
         );
         this.renderFooter(guiGraphics, multidimensional);
      }
   }

   private void renderFooter(GuiGraphics guiGraphics, boolean multidimensional) {
      if (multidimensional) {
         guiGraphics.m_280614_(this.f_96547_, Component.m_237115_("ae2lt.matrix.gui.heat_ignored"), 12, 144, 5659768, false);
      } else {
         this.drawRow(
            guiGraphics,
            138,
            Component.m_237115_("ae2lt.matrix.gui.label_heat"),
            percent(((MatrixControllerMenu)this.f_97732_).getNormalizedHeat()) + " · " + I18n.m_118938_(this.heatStateKey(), new Object[0]),
            this.heatTextColor()
         );
         boolean overload = ((MatrixControllerMenu)this.f_97732_).getMode() == MatrixCoreMode.OVERLOAD;
         this.drawGauge(
            guiGraphics, ((MatrixControllerMenu)this.f_97732_).getNormalizedHeat(), this.heatBarColor(), overload ? 0.42 : -1.0, overload ? 0.58 : -1.0
         );
      }
   }

   private Component modeName() {
      return Component.m_237115_("ae2lt.matrix.mode." + ((MatrixControllerMenu)this.f_97732_).getMode().name().toLowerCase(Locale.ROOT));
   }

   private Component issueText() {
      int ordinal = ((MatrixControllerMenu)this.f_97732_).getIssue();
      MatrixMultiblockScanIssue[] values = MatrixMultiblockScanIssue.values();
      String name = ordinal >= 0 && ordinal < values.length ? values[ordinal].name().toLowerCase(Locale.ROOT) : "unknown";
      return Component.m_237115_("ae2lt.matrix.issue." + name);
   }

   private String heatStateKey() {
      if (!((MatrixControllerMenu)this.f_97732_).isFormed()) {
         return "ae2lt.matrix.gui.heat_idle";
      } else {
         return switch (((MatrixControllerMenu)this.f_97732_).getMode()) {
            case OVERLOAD -> {
               double heat = ((MatrixControllerMenu)this.f_97732_).getNormalizedHeat();
               yield heat < 0.42 ? "ae2lt.matrix.gui.heat_cold" : (heat > 0.58 ? "ae2lt.matrix.gui.heat_hot" : "ae2lt.matrix.gui.heat_sweet");
            }
            case STABLE, QUANTUM -> {
               double heat = ((MatrixControllerMenu)this.f_97732_).getNormalizedHeat();
               yield heat < 0.35 ? "ae2lt.matrix.gui.heat_good" : (heat < 0.7 ? "ae2lt.matrix.gui.heat_warm" : "ae2lt.matrix.gui.heat_hot");
            }
            default -> "ae2lt.matrix.gui.heat_idle";
         };
      }
   }

   private int heatTextColor() {
      String var1 = this.heatStateKey();

      return switch (var1) {
         case "ae2lt.matrix.gui.heat_sweet", "ae2lt.matrix.gui.heat_good" -> 1862452;
         case "ae2lt.matrix.gui.heat_hot" -> 9382691;
         case "ae2lt.matrix.gui.heat_cold" -> 2051705;
         default -> 8019476;
      };
   }

   private int heatBarColor() {
      String var1 = this.heatStateKey();

      return switch (var1) {
         case "ae2lt.matrix.gui.heat_sweet", "ae2lt.matrix.gui.heat_good" -> -11747470;
         case "ae2lt.matrix.gui.heat_hot" -> -2534577;
         case "ae2lt.matrix.gui.heat_cold" -> -10773547;
         default -> -2574258;
      };
   }
}
