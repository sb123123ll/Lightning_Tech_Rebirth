package com.moakiee.ae2lt.logic.tianshu.terminal;

import appeng.api.implementations.blockentities.IViewCellStorage;
import appeng.api.inventories.InternalInventory;
import appeng.helpers.IPatternTerminalLogicHost;
import appeng.helpers.WirelessTerminalMenuHost;
import appeng.menu.ISubMenu;
import appeng.parts.encoding.PatternEncodingLogic;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.InternalInventoryHost;
import java.util.function.BiConsumer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

@Deprecated(
   forRemoval = false
)
public class TianshuWirelessPatternEncodingTermMenuHost
   extends WirelessTerminalMenuHost
   implements TianshuPatternTerminalHost,
   IPatternTerminalLogicHost,
   IViewCellStorage,
   InternalInventoryHost {
   private static final String TAG_PATTERN_LOGIC = "patternEncodingLogic";
   private static final String TAG_VIEW_CELLS = "viewcells";
   private final PatternEncodingLogic logic = new PatternEncodingLogic(this);
   private final AppEngInternalInventory viewCells = new AppEngInternalInventory(this, 5);
   private final TianshuTerminalState terminalState = new TianshuTerminalState();

   public TianshuWirelessPatternEncodingTermMenuHost(Player player, int inventorySlot, ItemStack stack, BiConsumer<Player, ISubMenu> returnToMainMenu) {
      super(player, inventorySlot, stack, returnToMainMenu);
      CompoundTag data = this.getItemStack().m_41737_("patternEncodingLogic");
      if (data != null) {
         this.logic.readFromNBT(data);
         this.viewCells.readFromNBT(data, "viewcells");
         this.terminalState.read(data, TianshuTerminalState.NbtFormat.WIRELESS);
      }

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

   public void markForSave() {
      CompoundTag data = this.getItemStack().m_41698_("patternEncodingLogic");
      this.logic.writeToNBT(data);
      this.viewCells.writeToNBT(data, "viewcells");
      this.terminalState.write(data, TianshuTerminalState.NbtFormat.WIRELESS);
   }

   public void saveChanges() {
      this.markForSave();
   }

   public void onChangeInventory(InternalInventory inventory, int slot) {
      this.markForSave();
   }

   public AppEngInternalInventory getViewCellStorage() {
      return this.viewCells;
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
