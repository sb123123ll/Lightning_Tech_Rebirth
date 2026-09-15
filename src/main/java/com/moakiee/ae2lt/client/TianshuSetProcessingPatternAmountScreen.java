package com.moakiee.ae2lt.client;

import appeng.api.stacks.GenericStack;
import appeng.client.gui.AESubScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.NumberEntryType;
import appeng.client.gui.me.common.ClientDisplaySlot;
import appeng.client.gui.widgets.NumberEntryWidget;
import appeng.client.gui.widgets.TabButton;
import appeng.core.localization.GuiText;
import appeng.menu.SlotSemantics;
import com.google.common.primitives.Longs;
import com.moakiee.ae2lt.menu.TianshuPatternEncodingTermMenu;
import java.util.function.Consumer;
import net.minecraft.world.item.ItemStack;

final class TianshuSetProcessingPatternAmountScreen<M extends TianshuPatternEncodingTermMenu> extends AESubScreen<M, TianshuPatternEncodingTermScreen<M>> {
   private final NumberEntryWidget amount;
   private final GenericStack currentStack;
   private final Consumer<GenericStack> setter;

   TianshuSetProcessingPatternAmountScreen(TianshuPatternEncodingTermScreen<M> parentScreen, GenericStack currentStack, Consumer<GenericStack> setter) {
      super(parentScreen, "/screens/set_processing_pattern_amount.json");
      this.currentStack = currentStack;
      this.setter = setter;
      this.widgets.addButton("save", GuiText.Set.text(), this::confirm);
      ItemStack icon = ((TianshuPatternEncodingTermMenu)this.m_6262_()).getHost().getMainMenuIcon();
      TabButton button = new TabButton(Icon.ARROW_LEFT, icon.m_41786_(), ignored -> this.returnToParent());
      this.widgets.add("back", button);
      this.amount = this.widgets.addNumberEntryWidget("amountToStock", NumberEntryType.of(currentStack.what()));
      this.amount.setLongValue(currentStack.amount());
      this.amount.setMaxValue(this.getMaxAmount());
      this.amount.setTextFieldStyle(this.style.getWidget("amountToStockInput"));
      this.amount.setMinValue(0L);
      this.amount.setHideValidationIcon(true);
      this.amount.setOnConfirm(this::confirm);
      this.addClientSideSlot(new ClientDisplaySlot(currentStack), SlotSemantics.MACHINE_OUTPUT);
   }

   protected void m_7856_() {
      super.m_7856_();
      this.setSlotsHidden(SlotSemantics.TOOLBOX, true);
   }

   private void confirm() {
      this.amount.getLongValue().ifPresent(newAmount -> {
         long constrainedAmount = Longs.constrainToRange(newAmount, 0L, this.getMaxAmount());
         this.setter.accept(constrainedAmount <= 0L ? null : new GenericStack(this.currentStack.what(), constrainedAmount));
         this.returnToParent();
      });
   }

   private long getMaxAmount() {
      return 999999L * (long)this.currentStack.what().getAmountPerUnit();
   }
}
