package com.moakiee.ae2lt.menu;

import com.moakiee.ae2lt.blockentity.MatrixControllerBlockEntity;
import com.moakiee.ae2lt.logic.craft.MatrixCoreMode;
import com.moakiee.ae2lt.logic.craft.MatrixCraftingMath;
import com.moakiee.ae2lt.logic.craft.MatrixCraftingProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.extensions.IForgeMenuType;

public class MatrixControllerMenu extends AbstractContainerMenu {
   public static final MenuType<MatrixControllerMenu> TYPE = IForgeMenuType.create(MatrixControllerMenu::clientCreate);
   private final BlockPos blockPos;
   private final MatrixControllerBlockEntity host;
   private final DataSlot formedSlot = DataSlot.m_39401_();
   private final DataSlot memberCountSlot = DataSlot.m_39401_();
   private final DataSlot patternStorageCountSlot = DataSlot.m_39401_();
   private final DataSlot patternSlotCountSlot = DataSlot.m_39401_();
   private final DataSlot craftingUnitCountSlot = DataSlot.m_39401_();
   private final DataSlot modeSlot = DataSlot.m_39401_();
   private final DataSlot issueSlot = DataSlot.m_39401_();
   private final DataSlot dispatchUnitCountSlot = DataSlot.m_39401_();
   private final DataSlot threadPowerSlot = DataSlot.m_39401_();
   private final DataSlot stableBaseOperationsSlot = DataSlot.m_39401_();
   private final DataSlot amplifierUnitCountSlot = DataSlot.m_39401_();
   private final DataSlot coolingUnitCountSlot = DataSlot.m_39401_();
   private final DataSlot coolingPowerSlot = DataSlot.m_39401_();
   private final DataSlot heatSlot = DataSlot.m_39401_();
   private final DataSlot efficiencySlot = DataSlot.m_39401_();
   private final DataSlot providerCallsRemainingSlot = DataSlot.m_39401_();
   private final DataSlot operationsPerTickHighSlot = DataSlot.m_39401_();
   private final DataSlot operationsPerTickLowSlot = DataSlot.m_39401_();

   public MatrixControllerMenu(int containerId, Inventory playerInventory, MatrixControllerBlockEntity host) {
      super(TYPE, containerId);
      this.blockPos = host.m_58899_();
      this.host = host;
      this.syncFromHost();
      this.addSyncSlots();
   }

   private MatrixControllerMenu(
      int containerId,
      BlockPos blockPos,
      boolean formed,
      int memberCount,
      int patternStorageCount,
      int patternSlotCount,
      int craftingUnitCount,
      int mode,
      int issue,
      int dispatchUnitCount,
      int threadPower,
      int stableBaseOperations,
      int amplifierUnitCount,
      int coolingUnitCount,
      int coolingPower,
      int heat,
      int efficiency,
      int providerCallsRemaining,
      int operationsPerTick
   ) {
      super(TYPE, containerId);
      this.blockPos = blockPos;
      this.host = null;
      this.formedSlot.m_6422_(formed ? 1 : 0);
      this.memberCountSlot.m_6422_(memberCount);
      this.patternStorageCountSlot.m_6422_(patternStorageCount);
      this.patternSlotCountSlot.m_6422_(patternSlotCount);
      this.craftingUnitCountSlot.m_6422_(craftingUnitCount);
      this.modeSlot.m_6422_(mode);
      this.issueSlot.m_6422_(issue);
      this.dispatchUnitCountSlot.m_6422_(dispatchUnitCount);
      this.threadPowerSlot.m_6422_(threadPower);
      this.stableBaseOperationsSlot.m_6422_(stableBaseOperations);
      this.amplifierUnitCountSlot.m_6422_(amplifierUnitCount);
      this.coolingUnitCountSlot.m_6422_(coolingUnitCount);
      this.coolingPowerSlot.m_6422_(coolingPower);
      this.heatSlot.m_6422_(heat);
      this.efficiencySlot.m_6422_(efficiency);
      this.providerCallsRemainingSlot.m_6422_(providerCallsRemaining);
      this.setOperationsPerTick(operationsPerTick);
      this.addSyncSlots();
   }

   private static MatrixControllerMenu clientCreate(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
      return new MatrixControllerMenu(
         containerId,
         buf.m_130135_(),
         buf.readBoolean(),
         buf.m_130242_(),
         buf.m_130242_(),
         buf.m_130242_(),
         buf.m_130242_(),
         buf.m_130242_(),
         buf.m_130242_(),
         buf.m_130242_(),
         buf.m_130242_(),
         buf.m_130242_(),
         buf.m_130242_(),
         buf.m_130242_(),
         buf.m_130242_(),
         buf.m_130242_(),
         buf.m_130242_(),
         buf.m_130242_(),
         buf.m_130242_()
      );
   }

   public static void writeExtraData(FriendlyByteBuf buf, MatrixControllerBlockEntity be) {
      MatrixCraftingProfile profile = be.getCraftingProfile();
      MatrixCraftingMath.Snapshot snapshot = be.getLimiterSnapshot();
      buf.m_130064_(be.m_58899_());
      buf.writeBoolean(be.isFormed());
      buf.m_130130_(be.getMemberCount());
      buf.m_130130_(be.getPatternStorageCount());
      buf.m_130130_(be.getPatternSlotCount());
      buf.m_130130_(be.getCraftingUnitCount());
      buf.m_130130_(profile.mode().ordinal());
      buf.m_130130_(be.getPrimaryIssueOrdinal());
      buf.m_130130_(profile.dispatchUnitCount());
      buf.m_130130_(saturate((long)Math.floor(profile.threadPower())));
      buf.m_130130_(saturate(Math.min(4096L, profile.stableBaseOperations())));
      buf.m_130130_(profile.amplifierUnitCount());
      buf.m_130130_(profile.coolingUnitCount());
      buf.m_130130_(scaleValue(profile.coolPower()));
      buf.m_130130_(scaleHeat(snapshot.normalizedHeat()));
      buf.m_130130_(scaleValue(snapshot.efficiencyFactor()));
      buf.m_130130_(be.getAvailableProviderCalls());
      buf.m_130130_(saturate(snapshot.operationsPerTick()));
   }

   public void m_38946_() {
      if (this.host != null) {
         this.syncFromHost();
      }

      super.m_38946_();
   }

   public boolean m_6875_(Player player) {
      if (this.host != null) {
         if (this.host.m_58901_() || this.host.m_58904_() == null) {
            return false;
         }

         if (player.m_9236_() != this.host.m_58904_()) {
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
      return ItemStack.f_41583_;
   }

   public BlockPos getBlockPos() {
      return this.blockPos;
   }

   public int token() {
      return this.f_38840_;
   }

   public boolean isFormed() {
      return this.formedSlot.m_6501_() != 0;
   }

   public int getMemberCount() {
      return this.memberCountSlot.m_6501_();
   }

   public int getPatternStorageCount() {
      return this.patternStorageCountSlot.m_6501_();
   }

   public int getPatternSlotCount() {
      return this.patternSlotCountSlot.m_6501_();
   }

   public int getCraftingUnitCount() {
      return this.craftingUnitCountSlot.m_6501_();
   }

   public MatrixCoreMode getMode() {
      int ordinal = this.modeSlot.m_6501_();
      MatrixCoreMode[] values = MatrixCoreMode.values();
      return ordinal >= 0 && ordinal < values.length ? values[ordinal] : MatrixCoreMode.NONE;
   }

   public int getIssue() {
      return this.issueSlot.m_6501_();
   }

   public int getDispatchUnitCount() {
      return this.dispatchUnitCountSlot.m_6501_();
   }

   public int getThreadPower() {
      return this.threadPowerSlot.m_6501_();
   }

   public int getStableBaseOperations() {
      return this.stableBaseOperationsSlot.m_6501_();
   }

   public int getAmplifierUnitCount() {
      return this.amplifierUnitCountSlot.m_6501_();
   }

   public int getCoolingUnitCount() {
      return this.coolingUnitCountSlot.m_6501_();
   }

   public double getCoolingPower() {
      return unscaleValue(this.coolingPowerSlot.m_6501_());
   }

   public double getNormalizedHeat() {
      return unscaleHeat(this.heatSlot.m_6501_());
   }

   public double getEfficiencyFactor() {
      return unscaleValue(this.efficiencySlot.m_6501_());
   }

   public int getProviderCallsRemaining() {
      return this.providerCallsRemainingSlot.m_6501_();
   }

   public int getOperationsPerTick() {
      return (this.operationsPerTickHighSlot.m_6501_() & 65535) << 16 | this.operationsPerTickLowSlot.m_6501_() & 65535;
   }

   private void syncFromHost() {
      MatrixCraftingProfile profile = this.host.getCraftingProfile();
      MatrixCraftingMath.Snapshot snapshot = this.host.getLimiterSnapshot();
      this.formedSlot.m_6422_(this.host.isFormed() ? 1 : 0);
      this.memberCountSlot.m_6422_(this.host.getMemberCount());
      this.patternStorageCountSlot.m_6422_(this.host.getPatternStorageCount());
      this.patternSlotCountSlot.m_6422_(this.host.getPatternSlotCount());
      this.craftingUnitCountSlot.m_6422_(this.host.getCraftingUnitCount());
      this.modeSlot.m_6422_(profile.mode().ordinal());
      this.issueSlot.m_6422_(this.host.getPrimaryIssueOrdinal());
      this.dispatchUnitCountSlot.m_6422_(profile.dispatchUnitCount());
      this.threadPowerSlot.m_6422_(saturate((long)Math.floor(profile.threadPower())));
      this.stableBaseOperationsSlot.m_6422_(saturate(Math.min(4096L, profile.stableBaseOperations())));
      this.amplifierUnitCountSlot.m_6422_(profile.amplifierUnitCount());
      this.coolingUnitCountSlot.m_6422_(profile.coolingUnitCount());
      this.coolingPowerSlot.m_6422_(scaleValue(profile.coolPower()));
      this.heatSlot.m_6422_(scaleHeat(snapshot.normalizedHeat()));
      this.efficiencySlot.m_6422_(scaleValue(snapshot.efficiencyFactor()));
      this.providerCallsRemainingSlot.m_6422_(this.host.getAvailableProviderCalls());
      this.setOperationsPerTick(saturate(snapshot.operationsPerTick()));
   }

   private void addSyncSlots() {
      this.m_38895_(this.formedSlot);
      this.m_38895_(this.memberCountSlot);
      this.m_38895_(this.patternStorageCountSlot);
      this.m_38895_(this.patternSlotCountSlot);
      this.m_38895_(this.craftingUnitCountSlot);
      this.m_38895_(this.modeSlot);
      this.m_38895_(this.issueSlot);
      this.m_38895_(this.dispatchUnitCountSlot);
      this.m_38895_(this.threadPowerSlot);
      this.m_38895_(this.stableBaseOperationsSlot);
      this.m_38895_(this.amplifierUnitCountSlot);
      this.m_38895_(this.coolingUnitCountSlot);
      this.m_38895_(this.coolingPowerSlot);
      this.m_38895_(this.heatSlot);
      this.m_38895_(this.efficiencySlot);
      this.m_38895_(this.providerCallsRemainingSlot);
      this.m_38895_(this.operationsPerTickHighSlot);
      this.m_38895_(this.operationsPerTickLowSlot);
   }

   private void setOperationsPerTick(int operationsPerTick) {
      this.operationsPerTickHighSlot.m_6422_(operationsPerTick >>> 16 & 65535);
      this.operationsPerTickLowSlot.m_6422_(operationsPerTick & 65535);
   }

   private static int scaleValue(double value) {
      if (!Double.isNaN(value) && !(value <= 0.0)) {
         double scaled = value * 10.0;
         return scaled >= 2.147483647E9 ? Integer.MAX_VALUE : (int)Math.round(scaled);
      } else {
         return 0;
      }
   }

   private static double unscaleValue(int value) {
      return (double)value / 10.0;
   }

   private static int scaleHeat(double value) {
      if (!Double.isNaN(value) && !(value <= 0.0)) {
         double scaled = value * 1000.0;
         return scaled >= 2.147483647E9 ? Integer.MAX_VALUE : (int)Math.round(scaled);
      } else {
         return 0;
      }
   }

   private static double unscaleHeat(int value) {
      return (double)value / 1000.0;
   }

   private static int saturate(long value) {
      if (value <= 0L) {
         return 0;
      } else {
         return value >= 2147483647L ? Integer.MAX_VALUE : (int)value;
      }
   }
}
