package com.moakiee.ae2lt.menu;

import appeng.menu.AEBaseMenu;
import appeng.menu.implementations.MenuTypeBuilder;
import com.moakiee.ae2lt.blockentity.TianshuSupercomputerControllerBlockEntity;
import com.moakiee.ae2lt.logic.tianshu.CpuInternalCoreProfile;
import com.moakiee.ae2lt.logic.tianshu.CpuMainCoreTier;
import com.moakiee.thunderbolt.core.crafting.algorithm.ForgeMenuTypeBuilderExtension;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

public class TianshuSupercomputerControllerMenu extends AEBaseMenu {
   public static final MenuType<TianshuSupercomputerControllerMenu> TYPE = ForgeMenuTypeBuilderExtension.buildUnregistered(
      MenuTypeBuilder.create(TianshuSupercomputerControllerMenu::new, TianshuSupercomputerControllerBlockEntity.class)
         .withMenuTitle(host -> host.m_58900_().m_60734_().m_49954_())
         .withInitialData(TianshuSupercomputerControllerMenu::writeExtraData, (host, menu, buf) -> menu.readExtraData(buf)),
      new ResourceLocation("ae2lt", "tianshu_supercomputer_controller")
   );
   private final BlockPos blockPos;
   private final TianshuSupercomputerControllerBlockEntity host;
   private final DataSlot formed = DataSlot.m_39401_();
   private final DataSlot tier = DataSlot.m_39401_();
   private final DataSlot storageUnits = DataSlot.m_39401_();
   private final DataSlot parallelUnits = DataSlot.m_39401_();
   private final DataSlot amplifierUnits = DataSlot.m_39401_();
   private final DataSlot closedLoopPatternStorages = DataSlot.m_39401_();
   private final DataSlot closedLoopSeedStorages = DataSlot.m_39401_();
   private final DataSlot parallelism = DataSlot.m_39401_();
   private final DataSlot capped = DataSlot.m_39401_();
   private final DataSlot issue = DataSlot.m_39401_();
   private final DataSlot[] storage = new DataSlot[]{DataSlot.m_39401_(), DataSlot.m_39401_(), DataSlot.m_39401_(), DataSlot.m_39401_()};
   private final DataSlot[] maxCopiesPerTick = new DataSlot[]{DataSlot.m_39401_(), DataSlot.m_39401_(), DataSlot.m_39401_(), DataSlot.m_39401_()};

   public TianshuSupercomputerControllerMenu(int id, Inventory inventory, TianshuSupercomputerControllerBlockEntity host) {
      super(TYPE, id, inventory, host);
      this.blockPos = host.m_58899_();
      this.host = host;
      this.syncFromHost();
      this.addSlots();
   }

   private void readExtraData(FriendlyByteBuf buf) {
      this.formed.m_6422_(buf.readBoolean() ? 1 : 0);
      this.tier.m_6422_(buf.m_130242_());
      this.storageUnits.m_6422_(buf.m_130242_());
      this.parallelUnits.m_6422_(buf.m_130242_());
      this.amplifierUnits.m_6422_(buf.m_130242_());
      this.closedLoopPatternStorages.m_6422_(buf.m_130242_());
      this.closedLoopSeedStorages.m_6422_(buf.m_130242_());
      this.setStorage(buf.readLong());
      this.parallelism.m_6422_(buf.m_130242_());
      this.setMaxCopiesPerTick(buf.readLong());
      this.capped.m_6422_(buf.readBoolean() ? 1 : 0);
      this.issue.m_6422_(buf.m_130242_());
   }

   public static void writeExtraData(TianshuSupercomputerControllerBlockEntity host, FriendlyByteBuf buf) {
      CpuInternalCoreProfile profile = host.getCoreProfile();
      buf.writeBoolean(host.isFormed());
      buf.m_130130_(profile.mainCore() == null ? -1 : profile.mainCore().ordinal());
      buf.m_130130_(profile.storageUnitCount());
      buf.m_130130_(profile.parallelUnitCount());
      buf.m_130130_(profile.amplifierUnitCount());
      buf.m_130130_(host.getFunctionProfile().closedLoopPatternStorageCount());
      buf.m_130130_(host.getFunctionProfile().closedLoopSeedStorageCount());
      buf.writeLong(profile.storageBytes());
      buf.m_130130_(profile.parallelism());
      buf.writeLong(profile.maxCopiesPerTick());
      buf.writeBoolean(profile.parallelCapped());
      buf.m_130130_(host.getPrimaryIssueOrdinal());
   }

   public void m_38946_() {
      this.syncFromHost();
      super.m_38946_();
   }

   private void syncFromHost() {
      CpuInternalCoreProfile profile = this.host.getCoreProfile();
      this.formed.m_6422_(this.host.isFormed() ? 1 : 0);
      this.tier.m_6422_(profile.mainCore() == null ? -1 : profile.mainCore().ordinal());
      this.storageUnits.m_6422_(profile.storageUnitCount());
      this.parallelUnits.m_6422_(profile.parallelUnitCount());
      this.amplifierUnits.m_6422_(profile.amplifierUnitCount());
      this.closedLoopPatternStorages.m_6422_(this.host.getFunctionProfile().closedLoopPatternStorageCount());
      this.closedLoopSeedStorages.m_6422_(this.host.getFunctionProfile().closedLoopSeedStorageCount());
      this.parallelism.m_6422_(profile.parallelism());
      this.capped.m_6422_(profile.parallelCapped() ? 1 : 0);
      this.issue.m_6422_(this.host.getPrimaryIssueOrdinal());
      this.setStorage(profile.storageBytes());
      this.setMaxCopiesPerTick(profile.maxCopiesPerTick());
   }

   private void addSlots() {
      this.m_38895_(this.formed);
      this.m_38895_(this.tier);
      this.m_38895_(this.storageUnits);
      this.m_38895_(this.parallelUnits);
      this.m_38895_(this.amplifierUnits);
      this.m_38895_(this.closedLoopPatternStorages);
      this.m_38895_(this.closedLoopSeedStorages);
      this.m_38895_(this.parallelism);
      this.m_38895_(this.capped);
      this.m_38895_(this.issue);

      for (DataSlot slot : this.storage) {
         this.m_38895_(slot);
      }

      for (DataSlot slot : this.maxCopiesPerTick) {
         this.m_38895_(slot);
      }
   }

   private void setStorage(long value) {
      for (int i = 0; i < 4; i++) {
         this.storage[i].m_6422_((int)(value >>> i * 16) & 65535);
      }
   }

   private void setMaxCopiesPerTick(long value) {
      for (int i = 0; i < 4; i++) {
         this.maxCopiesPerTick[i].m_6422_((int)(value >>> i * 16) & 65535);
      }
   }

   public boolean isFormed() {
      return this.formed.m_6501_() != 0;
   }

   public BlockPos getBlockPos() {
      return this.blockPos;
   }

   public int token() {
      return this.f_38840_;
   }

   public CpuMainCoreTier getTier() {
      int value = this.tier.m_6501_();
      return value >= 0 && value < CpuMainCoreTier.values().length ? CpuMainCoreTier.values()[value] : null;
   }

   public int getStorageUnits() {
      return this.storageUnits.m_6501_();
   }

   public int getParallelUnits() {
      return this.parallelUnits.m_6501_();
   }

   public int getAmplifierUnits() {
      return this.amplifierUnits.m_6501_();
   }

   public int getClosedLoopPatternStorages() {
      return this.closedLoopPatternStorages.m_6501_();
   }

   public int getClosedLoopSeedStorages() {
      return this.closedLoopSeedStorages.m_6501_();
   }

   public int getSuccessfulDispatchesPerTick() {
      return this.parallelism.m_6501_();
   }

   public boolean isCapped() {
      return this.capped.m_6501_() != 0;
   }

   public int getIssue() {
      return this.issue.m_6501_();
   }

   public long getStorageBytes() {
      long value = 0L;

      for (int i = 0; i < 4; i++) {
         value |= (long)(this.storage[i].m_6501_() & 65535) << i * 16;
      }

      return value;
   }

   public long getMaxCopiesPerTick() {
      long value = 0L;

      for (int i = 0; i < 4; i++) {
         value |= (long)(this.maxCopiesPerTick[i].m_6501_() & 65535) << i * 16;
      }

      return value;
   }

   public boolean m_6875_(Player player) {
      return player.m_20275_((double)this.blockPos.m_123341_() + 0.5, (double)this.blockPos.m_123342_() + 0.5, (double)this.blockPos.m_123343_() + 0.5) <= 64.0;
   }

   public ItemStack m_7648_(Player player, int index) {
      return ItemStack.f_41583_;
   }
}
