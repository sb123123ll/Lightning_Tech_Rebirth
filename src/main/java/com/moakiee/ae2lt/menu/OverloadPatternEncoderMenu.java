package com.moakiee.ae2lt.menu;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.inventories.InternalInventory;
import appeng.blockentity.crafting.IMolecularAssemblerSupportedPattern;
import appeng.menu.AEBaseMenu;
import appeng.menu.SlotSemantics;
import appeng.menu.guisync.GuiSync;
import appeng.menu.implementations.MenuTypeBuilder;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.InternalInventoryHost;
import com.moakiee.ae2lt.item.OverloadPatternItem;
import com.moakiee.ae2lt.overload.pattern.PatternConversionService;
import com.moakiee.ae2lt.overload.runtime.pattern.Ae2PlainPatternResolver;
import com.moakiee.ae2lt.overload.runtime.pattern.EditableOverloadPatternState;
import com.moakiee.ae2lt.overload.runtime.pattern.OverloadPatternEditState;
import com.moakiee.ae2lt.overload.runtime.pattern.OverloadPatternPayload;
import com.moakiee.ae2lt.overload.runtime.pattern.ParsedPatternDefinition;
import com.moakiee.ae2lt.registry.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class OverloadPatternEncoderMenu extends AEBaseMenu {
   private static final int RESULT_SLOT_INDEX = 1;
   public static final MenuType<OverloadPatternEncoderMenu> TYPE = Ae2ltMenuBuilder.buildUnregistered(
      MenuTypeBuilder.create(OverloadPatternEncoderMenu::new, OverloadPatternEncoderHost.class), new ResourceLocation("ae2lt", "overload_pattern_encoder")
   );
   public static final int MAX_INPUT_SLOTS = 18;
   public static final int MAX_OUTPUT_SLOTS = 18;
   public static final int SOURCE_X = 17;
   public static final int SOURCE_Y = 21;
   public static final int RESULT_X = 17;
   public static final int RESULT_Y = 69;
   public static final int PLAYER_INV_X = 8;
   public static final int PLAYER_INV_Y = 107;
   public static final int HOTBAR_X = 8;
   public static final int HOTBAR_Y = 165;
   private static final int OFFSCREEN_SLOT_X = -1000;
   private static final int OFFSCREEN_SLOT_Y = -1000;
   private static final int SLOT_SPACING = 18;
   @GuiSync(20)
   public OverloadPatternEditState syncedState = OverloadPatternEditState.empty();
   private final PatternConversionService conversionService = new PatternConversionService();
   private final AppEngInternalInventory sourceInventory;
   private final AppEngInternalInventory resultInventory;
   private final AppEngInternalInventory inputPreviewInventory;
   private final AppEngInternalInventory outputPreviewInventory;
   @Nullable
   private ParsedPatternDefinition parsedSource;
   private boolean sourceDirty = true;

   public OverloadPatternEncoderMenu(int id, Inventory playerInventory, OverloadPatternEncoderHost host) {
      super(TYPE, id, playerInventory, host);
      this.sourceInventory = new AppEngInternalInventory(new InternalInventoryHost() {
         public void saveChanges() {
            OverloadPatternEncoderMenu.this.sourceDirty = true;
            OverloadPatternEncoderMenu.this.clearEncodedResult();
         }

         public void onChangeInventory(InternalInventory inv, int slot) {
         }

         public boolean isClientSide() {
            return OverloadPatternEncoderMenu.this.isClientSide();
         }
      }, 1, 1);
      this.resultInventory = new AppEngInternalInventory(null, 1, 1);
      this.inputPreviewInventory = new AppEngInternalInventory(null, 18, 1);
      this.outputPreviewInventory = new AppEngInternalInventory(null, 18, 1);
      this.addSlot(new OverloadPatternEncoderMenu.SourcePatternSlot(this.sourceInventory, 0, 17, 21), SlotSemantics.ENCODED_PATTERN);
      this.addSlot(new OverloadPatternEncoderMenu.ResultPatternSlot(this.resultInventory, 0, 17, 69), SlotSemantics.CRAFTING_RESULT);

      for (int slot = 0; slot < 18; slot++) {
         this.addSlot(new OverloadPatternEncoderMenu.PreviewSlot(this.inputPreviewInventory, slot, -1000, -1000), SlotSemantics.PROCESSING_INPUTS);
      }

      for (int slot = 0; slot < 18; slot++) {
         this.addSlot(new OverloadPatternEncoderMenu.PreviewSlot(this.outputPreviewInventory, slot, -1000, -1000), SlotSemantics.PROCESSING_OUTPUTS);
      }

      this.addPlayerInventorySlots(playerInventory);
      this.registerClientAction("toggleInputMode", Integer.class, this::toggleInputMode);
      this.registerClientAction("toggleOutputMode", Integer.class, this::toggleOutputMode);
   }

   public void m_38946_() {
      if (this.isServerSide() && this.sourceDirty) {
         this.reloadFromSource();
      }

      if (this.isServerSide() && this.parsedSource != null && this.syncedState.canEncode() && this.resultInventory.getStackInSlot(0).m_41619_()) {
         this.encodeResult();
      }

      super.m_38946_();
   }

   public void m_6877_(Player player) {
      super.m_6877_(player);
      returnInventory(player, this.sourceInventory);
   }

   public ItemStack getSourceStack() {
      return this.sourceInventory.getStackInSlot(0);
   }

   public ItemStack getResultStack() {
      return this.resultInventory.getStackInSlot(0);
   }

   public void clientToggleInputMode(int slotIndex) {
      this.sendClientAction("toggleInputMode", slotIndex);
   }

   public void clientToggleOutputMode(int slotIndex) {
      this.sendClientAction("toggleOutputMode", slotIndex);
   }

   public ItemStack getInputPreviewStack(int slotIndex) {
      return slotIndex >= 0 && slotIndex < 18 ? ((Slot)this.f_38839_.get(2 + slotIndex)).m_7993_() : ItemStack.f_41583_;
   }

   public ItemStack getOutputPreviewStack(int slotIndex) {
      return slotIndex >= 0 && slotIndex < 18 ? ((Slot)this.f_38839_.get(20 + slotIndex)).m_7993_() : ItemStack.f_41583_;
   }

   private void toggleInputMode(int slotIndex) {
      if (this.isServerSide() && this.parsedSource != null) {
         this.syncedState = this.syncedState.toggleInputMode(slotIndex);
         this.clearEncodedResult();
      }
   }

   private void toggleOutputMode(int slotIndex) {
      if (this.isServerSide() && this.parsedSource != null) {
         this.syncedState = this.syncedState.toggleOutputMode(slotIndex);
         this.clearEncodedResult();
      }
   }

   private void encodeResult() {
      if (this.isServerSide() && this.parsedSource != null && this.syncedState.canEncode()) {
         OverloadPatternItem overloadItem = (OverloadPatternItem)ModItems.OVERLOAD_PATTERN.get();
         ItemStack stack = this.conversionService.createOverloadPatternStack(overloadItem, this.parsedSource, this.syncedState);
         this.resultInventory.setItemDirect(0, stack);
      }
   }

   private void reloadFromSource() {
      this.sourceDirty = false;
      this.parsedSource = null;
      this.clearPreviewInventories();
      this.clearEncodedResult();
      ItemStack sourceStack = this.sourceInventory.getStackInSlot(0);
      if (sourceStack.m_41619_()) {
         this.syncedState = OverloadPatternEditState.empty();
      } else {
         EditableOverloadPatternState editable = this.tryResolveEditableSource(sourceStack);
         if (editable == null) {
            this.syncedState = OverloadPatternEditState.empty();
         } else {
            this.parsedSource = editable.parsedPattern();
            this.syncedState = this.conversionService
               .createEditState(editable.parsedPattern(), editable.encodedPattern(), sourceStack.m_41720_() instanceof OverloadPatternItem);
            this.populatePreviewInventories(editable.parsedPattern());
         }
      }
   }

   @Nullable
   private EditableOverloadPatternState tryResolveEditableSource(ItemStack sourceStack) {
      if (sourceStack.m_41619_()) {
         return null;
      } else {
         try {
            ItemStack plainSource = this.tryResolvePlainSourceStack(sourceStack);
            if (plainSource == null) {
               return null;
            } else {
               IPatternDetails plainDetails = PatternDetailsHelper.decodePattern(plainSource, this.getPlayer().m_9236_());
               if (plainDetails != null && !(plainDetails instanceof IMolecularAssemblerSupportedPattern)) {
                  Ae2PlainPatternResolver resolver = new Ae2PlainPatternResolver(this.getPlayer().m_9236_());
                  return this.conversionService.resolveEditableSource(sourceStack, resolver).orElse(null);
               } else {
                  return null;
               }
            }
         } catch (RuntimeException var5) {
            return null;
         }
      }
   }

   @Nullable
   private ItemStack tryResolvePlainSourceStack(ItemStack sourceStack) {
      if (sourceStack.m_41720_() instanceof OverloadPatternItem overloadPatternItem) {
         try {
            OverloadPatternPayload payload = overloadPatternItem.readPayload(sourceStack).orElse(null);
            return payload != null ? payload.sourcePattern().toItemStack() : null;
         } catch (RuntimeException var4) {
            return null;
         }
      } else {
         return sourceStack;
      }
   }

   private void populatePreviewInventories(ParsedPatternDefinition parsedPattern) {
      this.clearPreviewInventories();
      int visibleInputs = Math.min(parsedPattern.inputs().size(), 18);

      for (int i = 0; i < visibleInputs; i++) {
         this.inputPreviewInventory.setItemDirect(i, normalizedPreview(parsedPattern.inputs().get(i).stack()));
      }

      int visibleOutputs = Math.min(parsedPattern.outputs().size(), 18);

      for (int i = 0; i < visibleOutputs; i++) {
         this.outputPreviewInventory.setItemDirect(i, normalizedPreview(parsedPattern.outputs().get(i).stack()));
      }
   }

   private void clearPreviewInventories() {
      for (int i = 0; i < 18; i++) {
         this.inputPreviewInventory.setItemDirect(i, ItemStack.f_41583_);
      }

      for (int i = 0; i < 18; i++) {
         this.outputPreviewInventory.setItemDirect(i, ItemStack.f_41583_);
      }
   }

   private void clearEncodedResult() {
      this.resultInventory.setItemDirect(0, ItemStack.f_41583_);
   }

   private void consumeSourcePattern() {
      if (this.isServerSide()) {
         if (!this.sourceInventory.getStackInSlot(0).m_41619_()) {
            this.sourceInventory.extractItem(0, 1, false);
            this.sourceDirty = true;
            this.clearEncodedResult();
         }
      }
   }

   private static ItemStack normalizedPreview(ItemStack stack) {
      ItemStack copy = stack.m_41777_();
      copy.m_41764_(1);
      return copy;
   }

   private void addPlayerInventorySlots(Inventory playerInventory) {
      for (int row = 0; row < 3; row++) {
         for (int column = 0; column < 9; column++) {
            int slotIndex = column + row * 9 + 9;
            this.addSlot(new Slot(playerInventory, slotIndex, 8 + column * 18, 107 + row * 18), SlotSemantics.PLAYER_INVENTORY);
         }
      }

      for (int column = 0; column < 9; column++) {
         this.addSlot(new Slot(playerInventory, column, 8 + column * 18, 165), SlotSemantics.PLAYER_HOTBAR);
      }
   }

   private static void returnInventory(Player player, AppEngInternalInventory inventory) {
      for (int slot = 0; slot < inventory.size(); slot++) {
         ItemStack stack = inventory.extractItem(slot, Integer.MAX_VALUE, false);
         if (!stack.m_41619_() && !player.m_150109_().m_36054_(stack)) {
            player.m_36176_(stack, false);
         }
      }
   }

   public ItemStack m_7648_(Player player, int idx) {
      if (idx == 1 && this.isServerSide()) {
         Slot resultSlot = this.m_38853_(idx);
         ItemStack before = resultSlot.m_7993_().m_41777_();
         if (before.m_41619_()) {
            return ItemStack.f_41583_;
         } else {
            super.m_7648_(player, idx);
            if (resultSlot.m_7993_().m_41619_()) {
               this.consumeSourcePattern();
               return before;
            } else {
               return ItemStack.f_41583_;
            }
         }
      } else {
         return super.m_7648_(player, idx);
      }
   }

   private static final class InventoryContainerAdapter extends SimpleContainer {
      private final AppEngInternalInventory inventory;

      private InventoryContainerAdapter(AppEngInternalInventory inventory) {
         super(inventory.size());
         this.inventory = inventory;
      }

      public int m_6643_() {
         return this.inventory.size();
      }

      public boolean m_7983_() {
         for (int slot = 0; slot < this.inventory.size(); slot++) {
            if (!this.inventory.getStackInSlot(slot).m_41619_()) {
               return false;
            }
         }

         return true;
      }

      public ItemStack m_8020_(int slot) {
         return this.inventory.getStackInSlot(slot);
      }

      public ItemStack m_7407_(int slot, int amount) {
         return this.inventory.extractItem(slot, amount, false);
      }

      public ItemStack m_8016_(int slot) {
         return this.inventory.extractItem(slot, Integer.MAX_VALUE, false);
      }

      public void m_6836_(int slot, ItemStack stack) {
         this.inventory.setItemDirect(slot, stack);
      }

      public void m_6211_() {
         for (int slot = 0; slot < this.inventory.size(); slot++) {
            this.inventory.setItemDirect(slot, ItemStack.f_41583_);
         }
      }

      public boolean m_6542_(Player player) {
         return true;
      }
   }

   private static final class PreviewSlot extends Slot {
      private PreviewSlot(AppEngInternalInventory inventory, int invSlot, int x, int y) {
         super(new OverloadPatternEncoderMenu.InventoryContainerAdapter(inventory), invSlot, x, y);
      }

      public boolean m_5857_(ItemStack stack) {
         return false;
      }

      public boolean m_8010_(Player player) {
         return false;
      }

      public ItemStack m_6201_(int amount) {
         return ItemStack.f_41583_;
      }
   }

   private final class ResultPatternSlot extends Slot {
      private ResultPatternSlot(AppEngInternalInventory inventory, int invSlot, int x, int y) {
         super(new OverloadPatternEncoderMenu.InventoryContainerAdapter(inventory), invSlot, x, y);
      }

      public boolean m_5857_(ItemStack stack) {
         return false;
      }

      public void m_142406_(Player player, ItemStack stack) {
         super.m_142406_(player, stack);
         OverloadPatternEncoderMenu.this.consumeSourcePattern();
      }
   }

   private final class SourcePatternSlot extends Slot {
      private SourcePatternSlot(AppEngInternalInventory inventory, int invSlot, int x, int y) {
         super(new OverloadPatternEncoderMenu.InventoryContainerAdapter(inventory), invSlot, x, y);
      }

      public boolean m_5857_(ItemStack stack) {
         return !stack.m_41619_() && PatternDetailsHelper.isEncodedPattern(stack) && OverloadPatternEncoderMenu.this.tryResolveEditableSource(stack) != null;
      }

      public int m_6641_() {
         return 1;
      }
   }
}
