package com.moakiee.ae2lt.client;

import appeng.api.orientation.RelativeSide;
import appeng.api.parts.IPart;
import appeng.blockentity.AEBaseBlockEntity;
import appeng.blockentity.networking.CableBusBlockEntity;
import appeng.client.gui.AESubScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.widgets.TabButton;
import appeng.menu.SlotSemantics;
import com.moakiee.ae2lt.blockentity.OverloadProcessingFactoryBlockEntity;
import com.moakiee.ae2lt.menu.OverloadProcessingFactoryMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;

public class OverloadProcessingFactoryOutputConfigScreen extends AESubScreen<OverloadProcessingFactoryMenu, OverloadProcessingFactoryScreen> {
   private final OverloadProcessingFactoryOutputButton frontButton;
   private final OverloadProcessingFactoryOutputButton backButton;
   private final OverloadProcessingFactoryOutputButton leftButton;
   private final OverloadProcessingFactoryOutputButton rightButton;
   private final OverloadProcessingFactoryOutputButton topButton;
   private final OverloadProcessingFactoryOutputButton bottomButton;

   public OverloadProcessingFactoryOutputConfigScreen(OverloadProcessingFactoryScreen parent) {
      super(parent, "/screens/overload_processing_output_config.json");
      MutableComponent label = Component.m_237115_("block.ae2lt.overload_processing_factory");
      this.widgets.add("return", new TabButton(Icon.ARROW_LEFT, label, btn -> this.returnToParent()));
      OutputSidesClearButton clear = new OutputSidesClearButton(
         Component.m_237115_("ae2lt.gui.overload_factory.output_side.clear"), button -> ((OverloadProcessingFactoryMenu)this.f_97732_).clientClearOutputSides()
      );
      this.widgets.add("clear", clear);
      this.frontButton = this.addSideButton("front", RelativeSide.FRONT, "gui.tooltips.ae2.SideFront");
      this.backButton = this.addSideButton("back", RelativeSide.BACK, "gui.tooltips.ae2.SideBack");
      this.topButton = this.addSideButton("top", RelativeSide.TOP, "gui.tooltips.ae2.SideTop");
      this.rightButton = this.addSideButton("right", RelativeSide.RIGHT, "gui.tooltips.ae2.SideRight");
      this.bottomButton = this.addSideButton("bottom", RelativeSide.BOTTOM, "gui.tooltips.ae2.SideBottom");
      this.leftButton = this.addSideButton("left", RelativeSide.LEFT, "gui.tooltips.ae2.SideLeft");
   }

   protected void m_7856_() {
      super.m_7856_();
      this.setSlotsHidden(SlotSemantics.TOOLBOX, true);
   }

   protected void updateBeforeRender() {
      super.updateBeforeRender();
      this.refreshButtonDisplays();
      this.frontButton.setOn(((OverloadProcessingFactoryMenu)this.f_97732_).isOutputSideEnabled(RelativeSide.FRONT));
      this.backButton.setOn(((OverloadProcessingFactoryMenu)this.f_97732_).isOutputSideEnabled(RelativeSide.BACK));
      this.leftButton.setOn(((OverloadProcessingFactoryMenu)this.f_97732_).isOutputSideEnabled(RelativeSide.LEFT));
      this.rightButton.setOn(((OverloadProcessingFactoryMenu)this.f_97732_).isOutputSideEnabled(RelativeSide.RIGHT));
      this.topButton.setOn(((OverloadProcessingFactoryMenu)this.f_97732_).isOutputSideEnabled(RelativeSide.TOP));
      this.bottomButton.setOn(((OverloadProcessingFactoryMenu)this.f_97732_).isOutputSideEnabled(RelativeSide.BOTTOM));
   }

   public void m_7379_() {
      this.returnToParent();
   }

   public boolean m_7933_(int keyCode, int scanCode, int modifiers) {
      if (keyCode != 256 && !this.f_96541_.f_91066_.f_92092_.m_90832_(keyCode, scanCode)) {
         return super.m_7933_(keyCode, scanCode, modifiers);
      } else {
         this.returnToParent();
         return true;
      }
   }

   private OverloadProcessingFactoryOutputButton addSideButton(String widgetId, RelativeSide side, String labelKey) {
      OverloadProcessingFactoryOutputButton button = new OverloadProcessingFactoryOutputButton(
         Component.m_237115_(labelKey), press -> ((OverloadProcessingFactoryMenu)this.f_97732_).clientToggleOutputSide(side)
      );
      this.widgets.add(widgetId, button);
      return button;
   }

   private void refreshButtonDisplays() {
      OverloadProcessingFactoryBlockEntity host = ((OverloadProcessingFactoryMenu)this.f_97732_).getHost();
      if (host != null && host.m_58904_() != null) {
         for (RelativeSide relative : RelativeSide.values()) {
            Direction absolute = host.getOrientation().getSide(relative);
            this.getButton(relative).setDisplay(this.getDisplayIcon(host, absolute));
         }
      }
   }

   private OverloadProcessingFactoryOutputButton getButton(RelativeSide side) {
      return switch (side) {
         case FRONT -> this.frontButton;
         case BACK -> this.backButton;
         case LEFT -> this.leftButton;
         case RIGHT -> this.rightButton;
         case TOP -> this.topButton;
         case BOTTOM -> this.bottomButton;
         default -> throw new IncompatibleClassChangeError();
      };
   }

   private ItemLike getDisplayIcon(AEBaseBlockEntity host, Direction side) {
      Level level = host.m_58904_();
      if (level == null) {
         return null;
      } else {
         BlockPos pos = host.m_58899_().m_121945_(side);
         if (level.m_7702_(pos) instanceof CableBusBlockEntity cable) {
            IPart part = cable.getPart(side.m_122424_());
            if (part != null) {
               return part.getPartItem();
            }
         }

         return level.m_8055_(pos).m_60734_();
      }
   }
}
