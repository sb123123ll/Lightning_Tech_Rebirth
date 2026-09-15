package com.moakiee.ae2lt.integration.ae2wtlib;

import appeng.api.implementations.blockentities.IViewCellStorage;
import appeng.helpers.IPatternTerminalLogicHost;
import appeng.menu.ISubMenu;
import appeng.parts.encoding.PatternEncodingLogic;
import appeng.util.inv.AppEngInternalInventory;
import com.moakiee.ae2lt.logic.tianshu.terminal.ClosedLoopTerminalDraft;
import com.moakiee.ae2lt.logic.tianshu.terminal.ProcessingPatternTerminalDraft;
import com.moakiee.ae2lt.logic.tianshu.terminal.TianshuEncodingMode;
import com.moakiee.ae2lt.logic.tianshu.terminal.TianshuPatternTerminalHost;
import com.moakiee.ae2lt.logic.tianshu.terminal.TianshuTerminalState;
import de.mari_023.ae2wtlib.terminal.WTMenuHost;
import de.mari_023.ae2wtlib.wut.ItemWUT;
import java.util.function.BiConsumer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class TianshuWTMenuHost extends WTMenuHost implements TianshuPatternTerminalHost, IPatternTerminalLogicHost, IViewCellStorage {
   private static final String TAG_PATTERN_LOGIC = "patternEncodingLogic";
   private final PatternEncodingLogic logic = new PatternEncodingLogic(this);
   private final TianshuTerminalState terminalState = new TianshuTerminalState();

   public TianshuWTMenuHost(Player player, @Nullable Integer inventorySlot, ItemStack stack, BiConsumer<Player, ISubMenu> returnToMainMenu) {
      super(player, inventorySlot, stack, returnToMainMenu);
      this.readFromNbt();
      if (this.logic.getBlankPatternInv() instanceof AppEngInternalInventory inventory) {
         inventory.setMaxStackSize(0, 0);
      }
   }

   public PatternEncodingLogic getLogic() {
      return this.logic;
   }

   public Level getLevel() {
      return this.getPlayer().m_9236_();
   }

   @Override
   public boolean isUniversalWirelessTerminal() {
      return this.getItemStack().m_41720_() instanceof ItemWUT;
   }

   public ItemStack getMainMenuIcon() {
      return new ItemStack(Ae2wtlibIntegration.terminal());
   }

   protected void readFromNbt() {
      super.readFromNbt();
      CompoundTag data = this.getItemStack().m_41737_("patternEncodingLogic");
      if (data != null) {
         this.logic.readFromNBT(data);
         this.terminalState.read(data, TianshuTerminalState.NbtFormat.WIRELESS);
      }
   }

   public void saveChanges() {
      super.saveChanges();
      CompoundTag data = this.getItemStack().m_41698_("patternEncodingLogic");
      this.logic.writeToNBT(data);
      this.terminalState.write(data, TianshuTerminalState.NbtFormat.WIRELESS);
   }

   public void markForSave() {
      this.saveChanges();
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
}
