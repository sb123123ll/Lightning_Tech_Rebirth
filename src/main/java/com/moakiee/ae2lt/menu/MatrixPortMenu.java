package com.moakiee.ae2lt.menu;

import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.inventories.InternalInventory;
import appeng.crafting.pattern.EncodedPatternItem;
import appeng.menu.slot.AppEngSlot;
import appeng.util.inv.AppEngInternalInventory;
import com.moakiee.ae2lt.blockentity.MatrixPortBlockEntity;
import com.moakiee.ae2lt.util.SlotPositionAccess;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.extensions.IForgeMenuType;
import org.jetbrains.annotations.Nullable;

public class MatrixPortMenu extends AbstractContainerMenu {
   public static final MenuType<MatrixPortMenu> TYPE = IForgeMenuType.create(MatrixPortMenu::clientCreate);
   public static final int COLUMNS = 9;
   public static final int VISIBLE_ROWS = 6;
   public static final int MAX_PATTERN_SLOTS = 3600;
   public static final int PATTERN_X = 13;
   public static final int PATTERN_Y = 21;
   public static final int PLAYER_INVENTORY_X = 13;
   public static final int PLAYER_INVENTORY_Y = 140;
   public static final int PLAYER_HOTBAR_Y = 198;
   private static final int SLOT_SPACING = 18;
   private static final int OFFSCREEN_SLOT = -10000;
   private final BlockPos blockPos;
   @Nullable
   private final MatrixPortBlockEntity host;
   private final List<MatrixPortMenu.MatrixPatternSlot> patternSlots = new ArrayList<>();
   private final int patternSlotCount;
   private long patternContentRevision;

   public MatrixPortMenu(int containerId, Inventory playerInventory, MatrixPortBlockEntity host) {
      this(containerId, playerInventory, host.m_58899_(), host, host.getTerminalPatternInventory(), host.getTerminalPatternInventory().size());
   }

   private MatrixPortMenu(
      int containerId,
      Inventory playerInventory,
      BlockPos blockPos,
      @Nullable MatrixPortBlockEntity host,
      InternalInventory patternInventory,
      int patternSlotCount
   ) {
      super(TYPE, containerId);
      this.blockPos = blockPos;
      this.host = host;
      this.patternSlotCount = patternSlotCount;

      for (int slot = 0; slot < patternSlotCount; slot++) {
         MatrixPortMenu.MatrixPatternSlot patternSlot = new MatrixPortMenu.MatrixPatternSlot(patternInventory, slot);
         SlotPositionAccess.set(patternSlot, -10000, -10000);
         this.patternSlots.add(patternSlot);
         this.m_38897_(patternSlot);
      }

      for (int row = 0; row < 3; row++) {
         for (int column = 0; column < 9; column++) {
            this.m_38897_(new Slot(playerInventory, column + row * 9 + 9, 13 + column * 18, 140 + row * 18));
         }
      }

      for (int column = 0; column < 9; column++) {
         this.m_38897_(new Slot(playerInventory, column, 13 + column * 18, 198));
      }
   }

   private static MatrixPortMenu clientCreate(int containerId, Inventory playerInventory, FriendlyByteBuf buffer) {
      BlockPos blockPos = buffer.m_130135_();
      int slotCount = Mth.m_14045_(buffer.m_130242_(), 0, 3600);
      AppEngInternalInventory clientInventory = new AppEngInternalInventory(slotCount);
      MatrixPortBlockEntity host = playerInventory.f_35978_.m_9236_().m_7702_(blockPos) instanceof MatrixPortBlockEntity port ? port : null;
      return new MatrixPortMenu(containerId, playerInventory, blockPos, host, clientInventory, slotCount);
   }

   public static void writeExtraData(FriendlyByteBuf buffer, MatrixPortBlockEntity host) {
      buffer.m_130064_(host.m_58899_());
      buffer.m_130130_(host.getTerminalPatternInventory().size());
   }

   public boolean m_6875_(Player player) {
      if (this.host != null) {
         if (this.host.m_58901_() || this.host.m_58904_() == null || player.m_9236_() != this.host.m_58904_()) {
            return false;
         }

         BlockEntity current = this.host.m_58904_().m_7702_(this.blockPos);
         if (current != this.host) {
            return false;
         }
      }

      return player.m_20275_((double)this.blockPos.m_123341_() + 0.5, (double)this.blockPos.m_123342_() + 0.5, (double)this.blockPos.m_123343_() + 0.5) <= 64.0;
   }

   public ItemStack m_7648_(Player player, int index) {
      if (index >= 0 && index < this.f_38839_.size()) {
         Slot source = (Slot)this.f_38839_.get(index);
         if (!source.m_6657_()) {
            return ItemStack.f_41583_;
         } else {
            ItemStack sourceStack = source.m_7993_();
            ItemStack original = sourceStack.m_41777_();
            boolean moved;
            if (index < this.patternSlotCount) {
               moved = this.m_38903_(sourceStack, this.patternSlotCount, this.f_38839_.size(), true);
            } else {
               if (!PatternDetailsHelper.isEncodedPattern(sourceStack)) {
                  return ItemStack.f_41583_;
               }

               moved = this.m_38903_(sourceStack, 0, this.patternSlotCount, false);
            }

            if (!moved) {
               return ItemStack.f_41583_;
            } else {
               if (sourceStack.m_41619_()) {
                  source.m_5852_(ItemStack.f_41583_);
               } else {
                  source.m_6654_();
               }

               source.m_142406_(player, sourceStack);
               return original;
            }
         }
      } else {
         return ItemStack.f_41583_;
      }
   }

   public int getPatternSlotCount() {
      return this.patternSlotCount;
   }

   public long getPatternContentRevision() {
      return this.patternContentRevision;
   }

   public void m_182406_(int slotId, int stateId, ItemStack stack) {
      super.m_182406_(slotId, stateId, stack);
      if (slotId >= 0 && slotId < this.patternSlotCount) {
         this.patternContentRevision++;
      }
   }

   public void m_182410_(int stateId, List<ItemStack> items, ItemStack carried) {
      super.m_182410_(stateId, items, carried);
      if (this.patternSlotCount > 0) {
         this.patternContentRevision++;
      }
   }

   public List<MatrixPortMenu.MatrixPatternSlot> getPatternSlots() {
      return Collections.unmodifiableList(this.patternSlots);
   }

   public static final class MatrixPatternSlot extends AppEngSlot {
      private final int patternIndex;

      private MatrixPatternSlot(InternalInventory inventory, int patternIndex) {
         super(inventory, patternIndex);
         this.patternIndex = patternIndex;
      }

      public int getPatternIndex() {
         return this.patternIndex;
      }

      public boolean m_5857_(ItemStack stack) {
         return PatternDetailsHelper.isEncodedPattern(stack) && super.m_5857_(stack);
      }

      public int m_6641_() {
         return 1;
      }

      public ItemStack getDisplayStack() {
         ItemStack pattern = super.getDisplayStack();
         if (!pattern.m_41619_() && pattern.m_41720_() instanceof EncodedPatternItem encodedPattern) {
            ItemStack output = encodedPattern.getOutput(pattern);
            if (!output.m_41619_()) {
               return output;
            }
         }

         return pattern;
      }
   }
}
