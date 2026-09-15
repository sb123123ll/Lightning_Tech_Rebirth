package com.moakiee.ae2lt.client;

import appeng.api.behaviors.ContainerItemStrategies;
import appeng.api.behaviors.EmptyingAction;
import appeng.api.client.AEKeyRendering;
import appeng.api.config.ActionItems;
import appeng.api.config.Settings;
import appeng.api.config.ViewItems;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.client.gui.Icon;
import appeng.client.gui.me.common.MEStorageScreen;
import appeng.client.gui.me.common.Repo;
import appeng.client.gui.me.common.RepoSlot;
import appeng.client.gui.me.common.StackSizeRenderer;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.ActionButton;
import appeng.client.gui.widgets.IconButton;
import appeng.client.gui.widgets.SettingToggleButton;
import appeng.client.gui.widgets.TabButton;
import appeng.client.gui.widgets.VerticalButtonBar;
import appeng.client.gui.widgets.TabButton.Style;
import appeng.core.definitions.AEItems;
import appeng.core.localization.ButtonToolTips;
import appeng.core.localization.Tooltips;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.ConfigValuePacket;
import appeng.core.sync.packets.InventoryActionPacket;
import appeng.helpers.InventoryAction;
import appeng.menu.SlotSemantic;
import appeng.menu.SlotSemantics;
import appeng.menu.me.common.GridInventoryEntry;
import appeng.menu.slot.FakeSlot;
import appeng.parts.encoding.EncodingMode;
import appeng.util.prioritylist.IPartitionList;
import com.moakiee.ae2lt.client.gui.AE2Button;
import com.moakiee.ae2lt.item.ClosedLoopPatternItem;
import com.moakiee.ae2lt.logic.AdvancedAECompat;
import com.moakiee.ae2lt.logic.tianshu.maintenance.InventoryMaintenanceBadge;
import com.moakiee.ae2lt.logic.tianshu.maintenance.InventoryMaintenanceStatus;
import com.moakiee.ae2lt.logic.tianshu.maintenance.ReservedStockMatchMode;
import com.moakiee.ae2lt.logic.tianshu.terminal.ProcessingPatternEncodingType;
import com.moakiee.ae2lt.logic.tianshu.terminal.TianshuEncodingMode;
import com.moakiee.ae2lt.logic.tianshu.terminal.TianshuPatternUploadRouting;
import com.moakiee.ae2lt.menu.Ae2ltSlotSemantics;
import com.moakiee.ae2lt.menu.TianshuPatternEncodingTermMenu;
import com.moakiee.ae2lt.mixin.client.AEBaseScreenAccessor;
import com.moakiee.ae2lt.mixin.client.VerticalButtonBarAccessor;
import com.moakiee.ae2lt.network.tianshu.MaintenanceSummarySyncPacket;
import com.moakiee.ae2lt.registry.ModItems;
import com.moakiee.ae2lt.util.SlotPositionAccess;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.Button.OnPress;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.anti_ad.mc.ipn.api.IPNIgnore;
import org.jetbrains.annotations.Nullable;

@IPNIgnore
public class TianshuPatternEncodingTermScreen<M extends TianshuPatternEncodingTermMenu> extends MEStorageScreen<M> {
   private final Map<EncodingMode, TianshuEncodingModePanel> modePanels = new EnumMap<>(EncodingMode.class);
   private final Map<TianshuEncodingMode, TabButton> modeTabs = new EnumMap<>(TianshuEncodingMode.class);
   private final TianshuClosedLoopEncodingPanel closedLoopPanel;
   private final List<TianshuPatternEncodingTermScreen.ProcessingMultiplierButton> processingModeButtons;
   private final AE2Button advancedEncoding;
   private final AE2Button overloadEncoding;
   private final RepoSlot networkBlankPatternSlot;
   private final Item blankPatternItem;
   @Nullable
   private GridInventoryEntry cachedNetworkBlankPatternEntry;
   private boolean awaitingMaintenanceEditor;
   private int requestedMaintenanceRevision;
   private int observedTianshuSelectionRevision = Integer.MIN_VALUE;
   private boolean observedMaintainableView;
   private long observedMaintenanceFilterRevision = Long.MIN_VALUE;
   private int observedEncodingAck;
   private final Map<AEKey, Long> syntheticMaintenanceEntries = new HashMap<>();
   private long nextSyntheticMaintenanceSerial = -10000000L;

   public TianshuPatternEncodingTermScreen(M menu, Inventory inventory, Component title, ScreenStyle style) {
      super(menu, inventory, title, style);

      for (EncodingMode mode : EncodingMode.values()) {
         TianshuEncodingModePanel panel = (TianshuEncodingModePanel)(switch (mode) {
            case CRAFTING -> new TianshuCraftingEncodingPanel(this, this.widgets);
            case PROCESSING -> new TianshuProcessingEncodingPanel(this, this.widgets);
            case SMITHING_TABLE -> new TianshuSmithingTableEncodingPanel(this, this.widgets);
            case STONECUTTING -> new TianshuStonecuttingEncodingPanel(this, this.widgets);
            default -> throw new IncompatibleClassChangeError();
         });
         TabButton tabButton = new TabButton(panel.getTabIconItem(), panel.getTabTooltip(), button -> menu.setMode(mode));
         tabButton.setStyle(Style.HORIZONTAL);
         int modeIndex = this.modeTabs.size();
         this.widgets.add("modePanel" + modeIndex, panel);
         this.widgets.add("modeTabButton" + modeIndex, tabButton);
         this.modeTabs.put(TianshuEncodingMode.fromAe2(mode), tabButton);
         this.modePanels.put(mode, panel);
      }

      this.widgets.add("encodePattern", new ActionButton(ActionItems.ENCODE, action -> menu.encode()));
      this.addExtraTab(
         TianshuEncodingMode.CLOSED_LOOP,
         ((ClosedLoopPatternItem)ModItems.CLOSED_LOOP_PATTERN.get()).m_7968_(),
         Component.m_237115_("ae2lt.tianshu.terminal.mode.closed_loop"),
         "modeTabButton4"
      );
      this.closedLoopPanel = new TianshuClosedLoopEncodingPanel(
         this, this.widgets, () -> this.switchToScreen(new TianshuClosedLoopPatternConfigScreen<M>(this))
      );
      this.widgets.add("closedLoopPanel", this.closedLoopPanel);
      this.processingModeButtons = List.of(
         this.addProcessingMultiplierButton("processingMultiply2", 2, 4),
         this.addProcessingMultiplierButton("processingMultiply5", 5, 10),
         this.addProcessingMultiplierButton("processingDivide2", -2, -4),
         this.addProcessingMultiplierButton("processingDivide5", -5, -10)
      );
      this.advancedEncoding = this.addCompactButton(
         "advancedEncodingButton",
         Component.m_237115_("ae2lt.tianshu.terminal.encoding.advanced.short"),
         () -> this.switchToScreen(new TianshuAdvancedPatternConfigScreen<M>(this))
      );
      this.overloadEncoding = this.addCompactButton(
         "overloadEncodingButton",
         Component.m_237115_("ae2lt.tianshu.terminal.encoding.overload.short"),
         () -> this.switchToScreen(new TianshuOverloadPatternConfigScreen<M>(this))
      );
      this.blankPatternItem = AEItems.BLANK_PATTERN.m_5456_();
      this.networkBlankPatternSlot = new TianshuPatternEncodingTermScreen.NetworkBlankPatternSlot(this.repo);
      this.observedEncodingAck = menu.triggeredUploadAck;
      this.replaceViewModeButton();
      this.addToLeftToolbar(new TianshuPatternEncodingTermScreen.MaintenanceOverviewButton());
   }

   public void m_7856_() {
      super.m_7856_();
      List<Slot> blankPatternSlots = ((TianshuPatternEncodingTermMenu)this.f_97732_).getSlots(SlotSemantics.BLANK_PATTERN);
      if (!blankPatternSlots.isEmpty()) {
         Slot disabledSlot = blankPatternSlots.get(0);
         SlotPositionAccess.set(this.networkBlankPatternSlot, disabledSlot.f_40220_, disabledSlot.f_40221_);
         ((TianshuPatternEncodingTermMenu)this.f_97732_).f_38839_.add(this.networkBlankPatternSlot);
      }
   }

   private AE2Button addCompactButton(String widgetId, Component label, Runnable onPress) {
      TianshuPatternEncodingTermScreen.CompactAE2Button button = new TianshuPatternEncodingTermScreen.CompactAE2Button(label, ignored -> onPress.run());
      this.widgets.add(widgetId, button);
      return button;
   }

   private TianshuPatternEncodingTermScreen.ProcessingMultiplierButton addProcessingMultiplierButton(String widgetId, int factor, int shiftedFactor) {
      AE2Button button = this.addCompactButton(
         widgetId,
         processingMultiplierLabel(factor),
         () -> ((TianshuPatternEncodingTermMenu)this.f_97732_).multiplyProcessing(m_96638_() ? shiftedFactor : factor)
      );
      return new TianshuPatternEncodingTermScreen.ProcessingMultiplierButton(button, factor, shiftedFactor);
   }

   private static Component processingMultiplierLabel(int factor) {
      return Component.m_237113_((factor < 0 ? "÷" : "×") + Math.abs(factor));
   }

   private void addExtraTab(TianshuEncodingMode mode, ItemStack icon, Component tooltip, String widgetId) {
      TabButton tab = new TabButton(icon, tooltip, button -> ((TianshuPatternEncodingTermMenu)this.f_97732_).setTianshuMode(mode));
      tab.setStyle(Style.HORIZONTAL);
      this.widgets.add(widgetId, tab);
      this.modeTabs.put(mode, tab);
   }

   protected void updateBeforeRender() {
      super.updateBeforeRender();
      this.observeEncodingAck();
      TianshuEncodingMode selected = ((TianshuPatternEncodingTermMenu)this.f_97732_).tianshuMode;

      for (EncodingMode mode : EncodingMode.values()) {
         boolean modeSelected = selected.ae2Mode() == mode;
         this.modePanels.get(mode).setVisible(modeSelected);
      }

      this.modeTabs.forEach((mode, button) -> button.setSelected(mode == selected));
      if (this.observedTianshuSelectionRevision != ((TianshuPatternEncodingTermMenu)this.f_97732_).tianshuSelectionRevision) {
         this.observedTianshuSelectionRevision = ((TianshuPatternEncodingTermMenu)this.f_97732_).tianshuSelectionRevision;
         this.awaitingMaintenanceEditor = false;
         this.removeSyntheticMaintenanceEntries();
         ((TianshuPatternEncodingTermMenu)this.f_97732_).resetClientTianshuScopedState();
      }

      this.syncSyntheticMaintenanceEntries();
      this.refreshMaintenancePartitionIfNeeded();
      if (((TianshuPatternEncodingTermMenu)this.f_97732_).hasTriggeredUploadAck()
         && TianshuRecipeTransferContext.isEncodingResultReady((TianshuPatternEncodingTermMenu)this.f_97732_, this.firstEncodedPattern())
         && ((TianshuPatternEncodingTermMenu)this.f_97732_).consumeTriggeredUpload()) {
         this.openUploadScreen(((TianshuPatternEncodingTermMenu)this.f_97732_).consumeDirectUploadRequest());
      } else if (this.awaitingMaintenanceEditor
         && ((TianshuPatternEncodingTermMenu)this.f_97732_).getMaintenanceEditorRevision() != this.requestedMaintenanceRevision
         && ((TianshuPatternEncodingTermMenu)this.f_97732_).getMaintenanceEditorData() != null) {
         this.awaitingMaintenanceEditor = false;
         this.switchToScreen(new TianshuMaintenanceRuleScreen(this, ((TianshuPatternEncodingTermMenu)this.f_97732_).getMaintenanceEditorData()));
      } else {
         boolean processing = selected == TianshuEncodingMode.PROCESSING;
         boolean shifted = m_96638_();
         this.processingModeButtons.forEach(control -> {
            control.button().f_93624_ = processing;
            control.button().m_93666_(processingMultiplierLabel(shifted ? control.shiftedFactor() : control.factor()));
         });
         boolean hasDraftInput = this.hasProcessingDraftInput();
         this.updateEncodingButton(
            this.advancedEncoding, ProcessingPatternEncodingType.ADVANCED, processing && AdvancedAECompat.canEncode(), hasDraftInput, "advanced"
         );
         this.updateEncodingButton(this.overloadEncoding, ProcessingPatternEncodingType.OVERLOAD, processing, hasDraftInput, "overload");
         boolean closedLoop = selected == TianshuEncodingMode.CLOSED_LOOP;
         this.closedLoopPanel.setVisible(closedLoop);
         this.setSlotsHidden(Ae2ltSlotSemantics.TIANSHU_GLOBAL_RESERVE_MARK, true);
      }
   }

   private void observeEncodingAck() {
      ItemStack current = this.firstEncodedPattern();
      if (this.observedEncodingAck != ((TianshuPatternEncodingTermMenu)this.f_97732_).triggeredUploadAck) {
         this.observedEncodingAck = ((TianshuPatternEncodingTermMenu)this.f_97732_).triggeredUploadAck;
         TianshuRecipeTransferContext.acceptEncodedPattern((TianshuPatternEncodingTermMenu)this.f_97732_, current);
      }
   }

   private void updateEncodingButton(AE2Button button, ProcessingPatternEncodingType type, boolean visible, boolean enabled, String key) {
      button.f_93624_ = visible;
      button.f_93623_ = enabled;
      boolean armed = ((TianshuPatternEncodingTermMenu)this.f_97732_).processingEncodingType.includes(type);
      button.m_93666_(Component.m_237115_("ae2lt.tianshu.terminal.encoding." + key + ".short").m_130940_(armed ? ChatFormatting.GREEN : ChatFormatting.WHITE));
      button.m_257544_(Tooltip.m_257550_(Component.m_237115_("ae2lt.tianshu.terminal.encoding." + key + (armed ? ".armed" : ""))));
   }

   private boolean hasProcessingDraftInput() {
      for (FakeSlot slot : ((TianshuPatternEncodingTermMenu)this.f_97732_).getProcessingInputSlots()) {
         if (!slot.m_7993_().m_41619_()) {
            return true;
         }
      }

      return false;
   }

   ItemStack firstEncodedPattern() {
      return ((TianshuPatternEncodingTermMenu)this.f_97732_)
         .getSlots(SlotSemantics.ENCODED_PATTERN)
         .stream()
         .<ItemStack>map(Slot::m_7993_)
         .filter(item -> !item.m_41619_())
         .findFirst()
         .orElse(ItemStack.f_41583_);
   }

   private void openUploadScreen(boolean directUploadRequested) {
      ItemStack stack = this.firstEncodedPattern();
      if (!stack.m_41619_()) {
         if (stack.m_41720_() instanceof ClosedLoopPatternItem) {
            ((TianshuPatternEncodingTermMenu)this.f_97732_).uploadEncodedPattern();
         } else {
            TianshuPatternUploadRouting.Route route = this.f_96541_.f_91073_ != null
               ? TianshuPatternUploadRouting.classify(stack, this.f_96541_.f_91073_)
               : TianshuPatternUploadRouting.Route.INVALID;
            switch (route) {
               case CLOSED_LOOP_STORAGE:
               case CRAFTING_ASSEMBLER:
                  ((TianshuPatternEncodingTermMenu)this.f_97732_).uploadEncodedPattern();
                  break;
               case PROCESSING_PROVIDER:
                  this.switchToScreen(new TianshuUploadTargetScreen<M>(this, directUploadRequested));
               case INVALID:
            }
         }
      }
   }

   public boolean openDirectUploadFallback() {
      if (!((TianshuPatternEncodingTermMenu)this.f_97732_).consumeTriggeredUpload()) {
         return false;
      } else if (!((TianshuPatternEncodingTermMenu)this.f_97732_).consumeDirectUploadRequest()) {
         return false;
      } else {
         this.switchToScreen(new TianshuUploadTargetScreen<M>(this, true));
         return true;
      }
   }

   private TianshuPatternEncodingTermScreen<M>.TianshuViewModeButton replaceViewModeButton() {
      VerticalButtonBar toolbar = ((AEBaseScreenAccessor)this).ae2lt$getVerticalToolbar();
      List<Button> buttons = ((VerticalButtonBarAccessor)toolbar).ae2lt$getButtons();

      for (int i = 0; i < buttons.size(); i++) {
         if (buttons.get(i) instanceof SettingToggleButton<?> settingButton && settingButton.getSetting() == Settings.VIEW_MODE) {
            TianshuPatternEncodingTermScreen<M>.TianshuViewModeButton replacement = new TianshuPatternEncodingTermScreen.TianshuViewModeButton();
            buttons.set(i, replacement);
            return replacement;
         }
      }

      throw new IllegalStateException("AE2 view-mode button is missing");
   }

   private void cycleViewMode(boolean reverse) {
      ViewItems current = (ViewItems)((TianshuPatternEncodingTermMenu)this.f_97732_).getConfigManager().getSetting(Settings.VIEW_MODE);
      ViewItems next;
      if (((TianshuPatternEncodingTermMenu)this.f_97732_).maintainableView) {
         ((TianshuPatternEncodingTermMenu)this.f_97732_).setMaintainableView(false);
         next = reverse ? ViewItems.CRAFTABLE : ViewItems.ALL;
      } else if (reverse) {
         next = switch (current) {
            case ALL -> null;
            case STORED -> ViewItems.ALL;
            case CRAFTABLE -> ViewItems.STORED;
            default -> throw new IncompatibleClassChangeError();
         };
      } else {
         next = switch (current) {
            case ALL -> ViewItems.STORED;
            case STORED -> ViewItems.CRAFTABLE;
            case CRAFTABLE -> null;
            default -> throw new IncompatibleClassChangeError();
         };
      }

      if (next == null) {
         ((TianshuPatternEncodingTermMenu)this.f_97732_).getConfigManager().putSetting(Settings.VIEW_MODE, ViewItems.ALL);
         ((TianshuPatternEncodingTermMenu)this.f_97732_).setMaintainableView(true);
      } else {
         this.setViewMode(next);
      }
   }

   private void setViewMode(ViewItems viewMode) {
      ((TianshuPatternEncodingTermMenu)this.f_97732_).getConfigManager().putSetting(Settings.VIEW_MODE, viewMode);
      NetworkHandler.instance().sendToServer(new ConfigValuePacket(Settings.VIEW_MODE, viewMode));
   }

   public boolean m_6375_(double mouseX, double mouseY, int button) {
      if (button == 2 && m_96638_() && this.getSlotUnderMouse() instanceof RepoSlot repoSlot) {
         if (!((TianshuPatternEncodingTermMenu)this.f_97732_).maintenanceAvailable) {
            if (this.f_96541_.f_91074_ != null) {
               this.f_96541_.f_91074_.m_5661_(Component.m_237115_("ae2lt.tianshu.maintenance.unavailable"), true);
            }

            return true;
         }

         GridInventoryEntry entry = repoSlot.getEntry();
         if (entry != null && entry.getWhat() != null) {
            MaintenanceSummarySyncPacket.Entry summary = ((TianshuPatternEncodingTermMenu)this.f_97732_).getMaintenanceSummaryEntry(entry.getWhat());
            if ((summary == null || !summary.ruleConfigured()) && !entry.isCraftable()) {
               if (this.f_96541_.f_91074_ != null) {
                  this.f_96541_.f_91074_.m_5661_(Component.m_237115_("ae2lt.tianshu.maintenance.unsupported"), true);
               }

               return true;
            }

            this.requestMaintenanceEditorFor(entry.getWhat());
            return true;
         }
      }

      if (this.getSlotUnderMouse() instanceof RepoSlot repoSlot && this.isSyntheticMaintenanceEntry(repoSlot.getEntry())) {
         return true;
      }

      if (this.f_96541_.f_91066_.f_92097_.m_90830_(button)) {
         Slot slot = this.getSlotUnderMouse();
         if (this.isClosedLoopMemberSlot(slot) && slot.m_6657_()) {
            int memberIndex = slot.m_150661_();
            AEItemKey key = AEItemKey.of(slot.m_7993_());
            long copies = Math.max(1L, ((TianshuPatternEncodingTermMenu)this.f_97732_).closedLoopDraftSync.copies(memberIndex));
            this.switchToScreen(new TianshuSetProcessingPatternAmountScreen<M>(this, new GenericStack(key, copies), newStack -> {
               if (newStack == null) {
                  InventoryActionPacket message = new InventoryActionPacket(InventoryAction.SET_FILTER, slot.f_40219_, ItemStack.f_41583_);
                  NetworkHandler.instance().sendToServer(message);
               } else {
                  ((TianshuPatternEncodingTermMenu)this.f_97732_).setClosedLoopMemberCopies(memberIndex, newStack.amount());
               }
            }));
            return true;
         }

         if (((TianshuPatternEncodingTermMenu)this.f_97732_).canModifyAmountForSlot(slot)) {
            GenericStack currentStack = GenericStack.fromItemStack(slot.m_7993_());
            if (currentStack != null) {
               this.switchToScreen(new TianshuSetProcessingPatternAmountScreen<M>(this, currentStack, newStack -> {
                  InventoryActionPacket message = new InventoryActionPacket(InventoryAction.SET_FILTER, slot.f_40219_, GenericStack.wrapInItemStack(newStack));
                  NetworkHandler.instance().sendToServer(message);
               }));
               return true;
            }
         }
      }

      return super.m_6375_(mouseX, mouseY, button);
   }

   protected void m_6597_(Slot slot, int slotIndex, int mouseButton, ClickType clickType) {
      if (slot instanceof RepoSlot repoSlot && this.isSyntheticMaintenanceEntry(repoSlot.getEntry())) {
         return;
      }

      super.m_6597_(slot, slotIndex, mouseButton, clickType);
   }

   protected void m_280072_(GuiGraphics graphics, int x, int y) {
      List<Component> multiplierTooltip = this.closedLoopPanel.getMultiplierTooltipAt(x - this.f_97735_, y - this.f_97736_);
      if (((TianshuPatternEncodingTermMenu)this.f_97732_).m_142621_().m_41619_() && multiplierTooltip != null) {
         this.drawTooltip(graphics, x, y, multiplierTooltip);
      } else if (((TianshuPatternEncodingTermMenu)this.f_97732_).m_142621_().m_41619_()
         && this.closedLoopPanel.isMouseOverStatus(x - this.f_97735_, y - this.f_97736_)) {
         this.drawTooltip(graphics, x, y, this.closedLoopPanel.buildStatusTooltip());
      } else {
         if (((TianshuPatternEncodingTermMenu)this.f_97732_).m_142621_().m_41619_()
            && ((TianshuPatternEncodingTermMenu)this.f_97732_).canModifyAmountForSlot(this.f_97734_)) {
            ArrayList<Component> itemTooltip = new ArrayList<>(this.m_280553_(this.f_97734_.m_7993_()));
            GenericStack unwrapped = GenericStack.fromItemStack(this.f_97734_.m_7993_());
            if (unwrapped != null) {
               itemTooltip.add(Tooltips.getAmountTooltip(ButtonToolTips.Amount, unwrapped));
            }

            itemTooltip.add(Tooltips.getSetAmountTooltip());
            this.drawTooltip(graphics, x, y, itemTooltip);
         } else if (((TianshuPatternEncodingTermMenu)this.f_97732_).m_142621_().m_41619_()
            && this.isClosedLoopMemberSlot(this.f_97734_)
            && this.f_97734_.m_6657_()) {
            ArrayList<Component> itemTooltip = new ArrayList<>(this.m_280553_(this.f_97734_.m_7993_()));
            TianshuClosedLoopEncodingPanel.appendMemberTooltip(
               itemTooltip, (TianshuPatternEncodingTermMenu)this.f_97732_, this.f_97734_.m_150661_(), this.f_97734_.m_7993_(), this.f_96541_.f_91073_
            );
            itemTooltip.add(Tooltips.getSetAmountTooltip());
            this.drawTooltip(graphics, x, y, itemTooltip);
         } else {
            super.m_280072_(graphics, x, y);
         }
      }
   }

   protected void renderGridInventoryEntryTooltip(GuiGraphics graphics, GridInventoryEntry entry, int x, int y) {
      MaintenanceSummarySyncPacket.Entry summary = entry != null
         ? ((TianshuPatternEncodingTermMenu)this.f_97732_).getMaintenanceSummaryEntry(entry.getWhat())
         : null;
      if (summary != null && summary.ruleConfigured()) {
         List<Component> lines = AEKeyRendering.getTooltip(entry.getWhat());
         if (Tooltips.shouldShowAmountTooltip(entry.getWhat(), summary.storedAmount())) {
            lines.add(Tooltips.getAmountTooltip(ButtonToolTips.StoredAmount, entry.getWhat(), summary.storedAmount()));
         }

         lines.add(
            Component.m_237110_("ae2lt.tianshu.maintenance.tooltip.thresholds", new Object[]{summary.lowerThreshold(), summary.upperThreshold()})
               .m_130940_(ChatFormatting.DARK_GRAY)
         );
         lines.add(Component.m_237110_("ae2lt.tianshu.maintenance.tooltip.batch", new Object[]{summary.amountPerJob()}).m_130940_(ChatFormatting.DARK_GRAY));
         lines.add(
            Component.m_237115_("ae2lt.tianshu.maintenance.status." + summary.status().name().toLowerCase(Locale.ROOT))
               .m_130940_(statusFormatting(summary.status()))
         );
         if (summary.globalReserve() != 0L) {
            lines.add(
               Component.m_237110_(
                     "ae2lt.tianshu.maintenance.tooltip.reserve",
                     new Object[]{
                        formatReserve(summary.globalReserve()),
                        Component.m_237115_(
                           summary.globalMode() == ReservedStockMatchMode.EXACT ? "ae2lt.tianshu.reserve.exact" : "ae2lt.tianshu.reserve.ignore_nbt"
                        )
                     }
                  )
                  .m_130940_(ChatFormatting.DARK_AQUA)
            );
         }

         lines.add(Component.m_237115_("ae2lt.tianshu.maintenance.tooltip.edit").m_130940_(ChatFormatting.GRAY));
         if (entry.getWhat() instanceof AEItemKey itemKey) {
            ItemStack stack = itemKey.getReadOnlyStack();
            graphics.renderTooltip(this.f_96547_, lines, stack.m_150921_(), stack, x, y);
         } else {
            graphics.m_280666_(this.f_96547_, lines, x, y);
         }
      } else {
         super.renderGridInventoryEntryTooltip(graphics, entry, x, y);
      }
   }

   private boolean isClosedLoopMemberSlot(Slot slot) {
      return slot != null && ((TianshuPatternEncodingTermMenu)this.f_97732_).getSlotSemantic(slot) == Ae2ltSlotSemantics.TIANSHU_CLOSED_LOOP_MEMBER;
   }

   protected EmptyingAction getEmptyingAction(Slot slot, ItemStack carried) {
      if (((TianshuPatternEncodingTermMenu)this.f_97732_).isProcessingPatternSlot(slot)) {
         EmptyingAction emptyingAction = ContainerItemStrategies.getEmptyingAction(carried);
         if (emptyingAction != null) {
            return emptyingAction;
         }
      }

      return super.getEmptyingAction(slot, carried);
   }

   public void requestMaintenanceEditorFor(AEKey key) {
      if (key != null) {
         this.requestedMaintenanceRevision = ((TianshuPatternEncodingTermMenu)this.f_97732_).getMaintenanceEditorRevision();
         this.awaitingMaintenanceEditor = true;
         ((TianshuPatternEncodingTermMenu)this.f_97732_).requestMaintenanceEditor(key);
      }
   }

   List<GridInventoryEntry> getNetworkEntriesForMaintenance() {
      return this.repo.getAllEntries().stream().filter(entry -> entry.getWhat() != null && !this.isSyntheticMaintenanceEntry(entry)).toList();
   }

   public void m_280092_(GuiGraphics graphics, Slot slot) {
      if (slot == this.networkBlankPatternSlot && !slot.m_6657_()) {
         Icon.BACKGROUND_BLANK_PATTERN.getBlitter().dest(slot.f_40220_, slot.f_40221_).blit(graphics);
      }

      if (this.isClosedLoopMemberSlot(slot) && slot.m_6657_()) {
         graphics.m_280480_(slot.m_7993_().m_255036_(1), slot.f_40220_, slot.f_40221_);
         long copies = Math.max(1L, ((TianshuPatternEncodingTermMenu)this.f_97732_).closedLoopDraftSync.copies(slot.m_150661_()));
         if (copies > 1L) {
            PoseStack poseStack = graphics.m_280168_();
            poseStack.m_85836_();
            poseStack.m_252880_(0.0F, 0.0F, 100.0F);
            StackSizeRenderer.renderSizeLabel(graphics, this.f_96547_, (float)slot.f_40220_, (float)slot.f_40221_, Long.toString(copies), false);
            poseStack.m_85849_();
         }
      } else {
         super.m_280092_(graphics, slot);
      }

      if (this.shouldShowCraftableIndicatorForSlot(slot)) {
         PoseStack poseStack = graphics.m_280168_();
         poseStack.m_85836_();
         poseStack.m_252880_(0.0F, 0.0F, 100.0F);
         StackSizeRenderer.renderSizeLabel(graphics, this.f_96547_, (float)(slot.f_40220_ - 11), (float)(slot.f_40221_ - 11), "+", false);
         poseStack.m_85849_();
      }

      GridInventoryEntry repoEntry = slot instanceof RepoSlot repoSlot ? repoSlot.getEntry() : null;
      if (repoEntry != null) {
         MaintenanceSummarySyncPacket.Entry summary = ((TianshuPatternEncodingTermMenu)this.f_97732_).getMaintenanceSummaryEntry(repoEntry.getWhat());
         if (summary != null && summary.ruleConfigured()) {
            int color = switch (InventoryMaintenanceBadge.from(summary.status())) {
               case GREEN -> -13382588;
               case YELLOW -> -13261;
               case RED -> -2280653;
               case GRAY -> -7829368;
            };
            graphics.m_280509_(slot.f_40220_ + 12, slot.f_40221_, slot.f_40220_ + 16, slot.f_40221_ + 4, color);
         }
      }
   }

   private void syncSyntheticMaintenanceEntries() {
      if (!((TianshuPatternEncodingTermMenu)this.f_97732_).maintainableView) {
         this.removeSyntheticMaintenanceEntries();
      } else {
         List<GridInventoryEntry> repoEntries = List.copyOf(this.repo.getAllEntries());
         HashSet<Long> presentSerials = new HashSet<>();
         HashSet<AEKey> realKeys = new HashSet<>();
         HashSet<Long> knownSyntheticSerials = new HashSet<>(this.syntheticMaintenanceEntries.values());

         for (GridInventoryEntry entry : repoEntries) {
            presentSerials.add(entry.getSerial());
            if (!knownSyntheticSerials.contains(entry.getSerial()) && entry.getWhat() != null) {
               realKeys.add(entry.getWhat());
            }
         }

         Map<AEKey, MaintenanceSummarySyncPacket.Entry> summaries = ((TianshuPatternEncodingTermMenu)this.f_97732_).getMaintenanceSummary();
         Iterator<Entry<AEKey, Long>> iterator = this.syntheticMaintenanceEntries.entrySet().iterator();

         while (iterator.hasNext()) {
            Entry<AEKey, Long> synthetic = iterator.next();
            MaintenanceSummarySyncPacket.Entry summary = summaries.get(synthetic.getKey());
            if (summary == null || !summary.ruleConfigured() || realKeys.contains(synthetic.getKey())) {
               if (presentSerials.contains(synthetic.getValue())) {
                  this.repo.handleUpdate(false, List.of(new GridInventoryEntry(synthetic.getValue(), null, 0L, 0L, false)));
               }

               iterator.remove();
            }
         }

         presentSerials.clear();

         for (GridInventoryEntry entryx : this.repo.getAllEntries()) {
            presentSerials.add(entryx.getSerial());
         }

         for (MaintenanceSummarySyncPacket.Entry summary : summaries.values()) {
            if (summary.ruleConfigured() && !realKeys.contains(summary.key())) {
               long serial = this.syntheticMaintenanceEntries.computeIfAbsent(summary.key(), ignored -> this.nextSyntheticMaintenanceSerial--);
               if (!presentSerials.contains(serial)) {
                  this.repo
                     .handleUpdate(
                        false,
                        List.of(new GridInventoryEntry(serial, summary.key(), summary.storedAmount(), summary.craftable() ? 0L : 1L, summary.craftable()))
                     );
               }
            }
         }
      }
   }

   private void refreshMaintenancePartitionIfNeeded() {
      long summaryRevision = ((TianshuPatternEncodingTermMenu)this.f_97732_).getMaintenanceSummaryRevision();
      if (this.observedMaintainableView != ((TianshuPatternEncodingTermMenu)this.f_97732_).maintainableView
         || ((TianshuPatternEncodingTermMenu)this.f_97732_).maintainableView && this.observedMaintenanceFilterRevision != summaryRevision) {
         this.observedMaintainableView = ((TianshuPatternEncodingTermMenu)this.f_97732_).maintainableView;
         this.observedMaintenanceFilterRevision = summaryRevision;
         this.repo.setPartitionList(this.createPartitionList(((TianshuPatternEncodingTermMenu)this.f_97732_).getViewCells()));
      }
   }

   @Nullable
   protected IPartitionList createPartitionList(List<ItemStack> viewCells) {
      final IPartitionList viewCellFilter = super.createPartitionList(viewCells);
      if (!((TianshuPatternEncodingTermMenu)this.f_97732_).maintainableView) {
         return viewCellFilter;
      } else {
         final Set<AEKey> maintainedKeys = ((TianshuPatternEncodingTermMenu)this.f_97732_)
            .getMaintenanceSummary()
            .values()
            .stream()
            .filter(entry -> entry.ruleConfigured())
            .map(entry -> entry.key())
            .collect(Collectors.toUnmodifiableSet());
         return new IPartitionList() {
            public boolean isListed(AEKey key) {
               return maintainedKeys.contains(key) && (viewCellFilter == null || viewCellFilter.isListed(key));
            }

            public boolean isEmpty() {
               return maintainedKeys.isEmpty();
            }

            public Iterable<AEKey> getItems() {
               return maintainedKeys;
            }
         };
      }
   }

   private void removeSyntheticMaintenanceEntries() {
      if (!this.syntheticMaintenanceEntries.isEmpty()) {
         List<GridInventoryEntry> removals = this.syntheticMaintenanceEntries
            .values()
            .stream()
            .map(serial -> new GridInventoryEntry(serial, null, 0L, 0L, false))
            .toList();
         this.syntheticMaintenanceEntries.clear();
         this.repo.handleUpdate(false, removals);
      }
   }

   private boolean isSyntheticMaintenanceEntry(GridInventoryEntry entry) {
      return entry != null && this.syntheticMaintenanceEntries.containsValue(entry.getSerial());
   }

   private static String formatReserve(long amount) {
      return amount < 0L ? "∞" : Long.toString(amount);
   }

   private static ChatFormatting statusFormatting(InventoryMaintenanceStatus status) {
      return switch (InventoryMaintenanceBadge.from(status)) {
         case GREEN -> ChatFormatting.GREEN;
         case YELLOW -> ChatFormatting.GOLD;
         case RED -> ChatFormatting.RED;
         case GRAY -> ChatFormatting.GRAY;
      };
   }

   protected List<Component> m_280553_(ItemStack stack) {
      List<Component> lines = super.m_280553_(stack);
      if (this.f_97734_ != null && this.shouldShowCraftableIndicatorForSlot(this.f_97734_)) {
         lines = new ArrayList<>(lines);
         lines.add(ButtonToolTips.Craftable.text().m_130940_(ChatFormatting.DARK_GRAY));
      }

      return lines;
   }

   private boolean shouldShowCraftableIndicatorForSlot(Slot slot) {
      SlotSemantic semantic = ((TianshuPatternEncodingTermMenu)this.f_97732_).getSlotSemantic(slot);
      if (semantic != SlotSemantics.CRAFTING_GRID
         && semantic != SlotSemantics.PROCESSING_INPUTS
         && semantic != SlotSemantics.SMITHING_TABLE_ADDITION
         && semantic != SlotSemantics.SMITHING_TABLE_BASE
         && semantic != SlotSemantics.SMITHING_TABLE_TEMPLATE
         && semantic != SlotSemantics.STONECUTTING_INPUT) {
         return false;
      } else {
         GenericStack slotContent = GenericStack.fromItemStack(slot.m_7993_());
         return slotContent != null && this.repo.isCraftable(slotContent.what());
      }
   }

   private GridInventoryEntry findNetworkBlankPatternEntry() {
      if (this.cachedNetworkBlankPatternEntry != null
         && this.repo.getAllEntries().contains(this.cachedNetworkBlankPatternEntry)
         && this.isBlankPatternEntry(this.cachedNetworkBlankPatternEntry)) {
         return this.cachedNetworkBlankPatternEntry;
      } else {
         this.cachedNetworkBlankPatternEntry = null;

         for (GridInventoryEntry entry : this.repo.getAllEntries()) {
            if (this.isBlankPatternEntry(entry)) {
               this.cachedNetworkBlankPatternEntry = entry;
               return entry;
            }
         }

         return null;
      }
   }

   private boolean isBlankPatternEntry(GridInventoryEntry entry) {
      if (entry.getWhat() instanceof AEItemKey key && key.getItem() == this.blankPatternItem) {
         return true;
      }

      return false;
   }

   public void m_7379_() {
      if (this.config.isClearGridOnClose()) {
         ((TianshuPatternEncodingTermMenu)this.f_97732_).clear();
      }

      super.m_7379_();
   }

   private static final class CompactAE2Button extends AE2Button {
      private static final float TEXT_SCALE = 0.65F;

      private CompactAE2Button(Component message, OnPress onPress) {
         super(message, onPress);
      }

      protected void m_87963_(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
         Component message = this.m_6035_();
         this.m_93666_(Component.m_237119_());
         super.m_87963_(graphics, mouseX, mouseY, partialTick);
         this.m_93666_(message);
         Font font = Minecraft.m_91087_().f_91062_;
         int color;
         int yOffset;
         if (!this.f_93623_) {
            color = -12501164;
            yOffset = -1;
         } else if (this.m_274382_()) {
            color = -11438953;
            yOffset = 0;
         } else {
            color = -855310;
            yOffset = 1;
         }

         float virtualWidth = (float)this.m_5711_() / 0.65F;
         float virtualHeight = (float)this.m_93694_() / 0.65F;
         float textX = (virtualWidth - (float)font.m_92852_(message)) / 2.0F;
         float textY = (virtualHeight - 9.0F) / 2.0F + 1.0F - (float)yOffset / 0.65F;
         PoseStack pose = graphics.m_280168_();
         pose.m_85836_();
         pose.m_252880_((float)this.m_252754_(), (float)this.m_252907_(), 10.0F);
         pose.m_85841_(0.65F, 0.65F, 1.0F);
         graphics.m_280614_(font, message, Math.round(textX), Math.round(textY), color, false);
         pose.m_85849_();
      }
   }

   private final class MaintenanceOverviewButton extends TextureToggleButton {
      private MaintenanceOverviewButton() {
         super(
            TextureToggleButton.ButtonType.INVENTORY_MAINTENANCE,
            ignored -> TianshuPatternEncodingTermScreen.this.switchToScreen(new TianshuGlobalReserveScreen(TianshuPatternEncodingTermScreen.this))
         );
         MutableComponent label = Component.m_237115_("ae2lt.tianshu.maintenance.overview_button");
         this.m_93666_(label);
         this.setTooltipAt(0, List.of(label));
      }
   }

   private final class NetworkBlankPatternSlot extends RepoSlot {
      private NetworkBlankPatternSlot(Repo repo) {
         super(repo, 0, 0, 0);
      }

      public GridInventoryEntry getEntry() {
         return TianshuPatternEncodingTermScreen.this.repo.hasPower() ? TianshuPatternEncodingTermScreen.this.findNetworkBlankPatternEntry() : null;
      }
   }

   private static record ProcessingMultiplierButton(AE2Button button, int factor, int shiftedFactor) {
   }

   private final class TianshuViewModeButton extends IconButton {
      private TianshuViewModeButton() {
         super(ignored -> TianshuPatternEncodingTermScreen.this.cycleViewMode(Screen.m_96638_()));
      }

      protected Icon getIcon() {
         if (((TianshuPatternEncodingTermMenu)TianshuPatternEncodingTermScreen.this.f_97732_).maintainableView) {
            return Icon.VIEW_MODE_CRAFTING;
         } else {
            return switch ((ViewItems)((TianshuPatternEncodingTermMenu)TianshuPatternEncodingTermScreen.this.f_97732_)
               .getConfigManager()
               .getSetting(Settings.VIEW_MODE)) {
               case ALL -> Icon.VIEW_MODE_ALL;
               case STORED -> Icon.VIEW_MODE_STORED;
               case CRAFTABLE -> Icon.VIEW_MODE_CRAFTING;
               default -> throw new IncompatibleClassChangeError();
            };
         }
      }

      public List<Component> getTooltipMessage() {
         MutableComponent var10000;
         if (((TianshuPatternEncodingTermMenu)TianshuPatternEncodingTermScreen.this.f_97732_).maintainableView) {
            var10000 = Component.m_237115_("ae2lt.tianshu.maintenance.view");
         } else {
            switch ((ViewItems)((TianshuPatternEncodingTermMenu)TianshuPatternEncodingTermScreen.this.f_97732_)
               .getConfigManager()
               .getSetting(Settings.VIEW_MODE)) {
               case ALL:
                  var10000 = Component.m_237115_(ButtonToolTips.StoredCraftable.getTranslationKey());
                  break;
               case STORED:
                  var10000 = Component.m_237115_(ButtonToolTips.StoredItems.getTranslationKey());
                  break;
               case CRAFTABLE:
                  var10000 = Component.m_237115_(ButtonToolTips.Craftable.getTranslationKey());
                  break;
               default:
                  throw new IncompatibleClassChangeError();
            }
         }

         MutableComponent value = var10000;
         return List.of(Component.m_237115_(ButtonToolTips.View.getTranslationKey()), value);
      }
   }
}
