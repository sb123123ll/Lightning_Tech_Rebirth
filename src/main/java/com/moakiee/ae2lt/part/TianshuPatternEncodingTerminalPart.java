package com.moakiee.ae2lt.part;

import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.items.parts.PartModels;
import appeng.parts.PartModel;
import appeng.parts.encoding.PatternEncodingTerminalPart;
import appeng.util.inv.AppEngInternalInventory;
import com.moakiee.ae2lt.logic.tianshu.terminal.ClosedLoopTerminalDraft;
import com.moakiee.ae2lt.logic.tianshu.terminal.ProcessingPatternTerminalDraft;
import com.moakiee.ae2lt.logic.tianshu.terminal.TianshuEncodingMode;
import com.moakiee.ae2lt.logic.tianshu.terminal.TianshuPatternTerminalHost;
import com.moakiee.ae2lt.logic.tianshu.terminal.TianshuTerminalState;
import com.moakiee.ae2lt.menu.TianshuPatternEncodingTermMenu;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import org.jetbrains.annotations.Nullable;

public final class TianshuPatternEncodingTerminalPart extends PatternEncodingTerminalPart implements TianshuPatternTerminalHost {
   @PartModels
   private static final ResourceLocation MODEL_OFF = new ResourceLocation("ae2lt", "part/tianshu_pattern_encoding_terminal_off");
   @PartModels
   private static final ResourceLocation MODEL_ON = new ResourceLocation("ae2lt", "part/tianshu_pattern_encoding_terminal_on");
   private static final IPartModel MODELS_OFF = new PartModel(new ResourceLocation[]{MODEL_BASE, MODEL_OFF, MODEL_STATUS_OFF});
   private static final IPartModel MODELS_ON = new PartModel(new ResourceLocation[]{MODEL_BASE, MODEL_ON, MODEL_STATUS_ON});
   private static final IPartModel MODELS_HAS_CHANNEL = new PartModel(new ResourceLocation[]{MODEL_BASE, MODEL_ON, MODEL_STATUS_HAS_CHANNEL});
   private final TianshuTerminalState terminalState = new TianshuTerminalState();

   public TianshuPatternEncodingTerminalPart(IPartItem<?> partItem) {
      super(partItem);
      if (this.getLogic().getBlankPatternInv() instanceof AppEngInternalInventory inventory) {
         inventory.setMaxStackSize(0, 0);
      }
   }

   public MenuType<?> getMenuType(Player player) {
      return TianshuPatternEncodingTermMenu.TYPE;
   }

   public IPartModel getStaticModels() {
      return this.selectModel(MODELS_OFF, MODELS_ON, MODELS_HAS_CHANNEL);
   }

   @Override
   public TianshuEncodingMode getTianshuEncodingMode() {
      return this.terminalState.getEncodingMode();
   }

   @Override
   public void setTianshuEncodingMode(TianshuEncodingMode mode) {
      if (this.terminalState.setEncodingMode(mode)) {
         this.markForSave();
      }
   }

   @Override
   public boolean isMaintainableView() {
      return this.terminalState.isMaintainableView();
   }

   @Override
   public void setMaintainableView(boolean enabled) {
      if (this.terminalState.setMaintainableView(enabled)) {
         this.markForSave();
      }
   }

   @Nullable
   @Override
   public ClosedLoopTerminalDraft getClosedLoopTerminalDraft() {
      return this.terminalState.getClosedLoopDraft();
   }

   @Override
   public void setClosedLoopTerminalDraft(@Nullable ClosedLoopTerminalDraft draft) {
      if (this.terminalState.setClosedLoopDraft(draft)) {
         this.markForSave();
      }
   }

   @Nullable
   @Override
   public ProcessingPatternTerminalDraft getProcessingPatternTerminalDraft() {
      return this.terminalState.getProcessingDraft();
   }

   @Override
   public void setProcessingPatternTerminalDraft(@Nullable ProcessingPatternTerminalDraft draft) {
      if (this.terminalState.setProcessingDraft(draft)) {
         this.markForSave();
      }
   }

   public void readFromNBT(CompoundTag data) {
      super.readFromNBT(data);
      this.terminalState.read(data, TianshuTerminalState.NbtFormat.PART);
   }

   public void writeToNBT(CompoundTag data) {
      super.writeToNBT(data);
      this.terminalState.write(data, TianshuTerminalState.NbtFormat.PART);
   }
}
