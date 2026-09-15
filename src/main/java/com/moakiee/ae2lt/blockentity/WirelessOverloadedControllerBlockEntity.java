package com.moakiee.ae2lt.blockentity;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridConnection;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.IGridNodeListener.State;
import appeng.api.networking.pathing.ChannelMode;
import appeng.me.GridConnection;
import appeng.util.SettingsFrom;
import com.moakiee.ae2lt.block.WirelessOverloadedControllerBlock;
import com.moakiee.ae2lt.grid.FrequencyBindingHelper;
import com.moakiee.ae2lt.grid.WirelessFrequencyManager;
import com.moakiee.ae2lt.registry.ModBlockEntities;
import com.moakiee.ae2lt.registry.ModBlocks;
import com.moakiee.thunderbolt.api.channel.ConnectionChannelCapacityProvider;
import com.moakiee.thunderbolt.core.channel.HighCapacityChannelSupport;
import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class WirelessOverloadedControllerBlockEntity
   extends OverloadedControllerBlockEntity
   implements WirelessFrequencyManager.WirelessTransmitterNodeProvider,
   ConnectionChannelCapacityProvider {
   private int frequencyId = -1;

   public WirelessOverloadedControllerBlockEntity(BlockPos pos, BlockState blockState) {
      this((BlockEntityType<?>)ModBlockEntities.WIRELESS_OVERLOADED_CONTROLLER.get(), pos, blockState);
   }

   protected WirelessOverloadedControllerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
      super(type, pos, blockState);
   }

   @Override
   protected IManagedGridNode createMainNode() {
      return super.createMainNode()
         .setTagName("wireless_overloaded_controller")
         .setVisualRepresentation((ItemLike)ModBlocks.WIRELESS_OVERLOADED_CONTROLLER.get());
   }

   public boolean isAdvanced() {
      return false;
   }

   public int getConnectionChannelCapacity(ChannelMode mode) {
      return this.isAdvanced() ? 1073741823 : 32 * mode.getCableCapacityFactor();
   }

   @Nullable
   @Override
   public IGridNode getWirelessGridNode() {
      return this.getMainNode().getNode();
   }

   @Override
   public int getTransmitterFrequencyId() {
      return this.frequencyId;
   }

   public int getFrequencyId() {
      return this.frequencyId;
   }

   public int getGridUsedChannels() {
      IGrid grid = this.getMainNode().getGrid();
      return grid == null ? 0 : HighCapacityChannelSupport.countUsedChannels(grid);
   }

   public int getGridMaxChannels() {
      IGrid grid = this.getMainNode().getGrid();
      if (grid == null) {
         return 0;
      } else {
         ChannelMode channelMode = grid.getPathingService().getChannelMode();
         if (channelMode == ChannelMode.INFINITE) {
            return -1;
         } else {
            int overloadedCount = 0;
            int vanillaCount = 0;

            for (IGridNode node : HighCapacityChannelSupport.getAllControllerNodes(grid)) {
               if (node.getOwner() instanceof OverloadedControllerBlockEntity) {
                  overloadedCount++;
               } else {
                  vanillaCount++;
               }
            }

            int factor = Math.max(1, channelMode.getCableCapacityFactor());
            long cap = (long)overloadedCount * (long)HighCapacityChannelSupport.channelsPerController() * (long)factor
               + (long)vanillaCount * 32L * (long)factor;
            return (int)Math.min(2147483647L, cap);
         }
      }
   }

   public boolean isFrequencyActive() {
      if (this.frequencyId > 0 && this.f_58857_ != null) {
         WirelessFrequencyManager manager = WirelessFrequencyManager.get();
         if (manager == null) {
            return false;
         } else {
            WirelessFrequencyManager.TransmitterEntry entry = manager.findTransmitter(this.frequencyId);
            return entry != null
               && entry.dimension().equals(this.f_58857_.m_46472_())
               && entry.pos().equals(this.f_58858_)
               && this.getMainNode().getNode() != null;
         }
      } else {
         return false;
      }
   }

   public void setFrequency(int newFreqId) {
      if (newFreqId != this.frequencyId) {
         WirelessFrequencyManager manager = WirelessFrequencyManager.get();
         if (manager != null) {
            if (newFreqId > 0 && this.f_58857_ != null && !manager.canRegisterTransmitter(newFreqId, this.f_58857_.m_46472_(), this.f_58858_)) {
               this.markForUpdate();
            } else {
               if (this.frequencyId > 0) {
                  this.destroyAllVirtualConnections();
                  manager.unregisterTransmitter(this.frequencyId);
                  if (this.f_58857_ != null) {
                     manager.unregisterDevice(this.frequencyId, this.f_58857_.m_46472_(), this.f_58858_);
                  }
               }

               int oldFreqId = this.frequencyId;
               this.frequencyId = newFreqId;
               if (this.frequencyId > 0 && this.f_58857_ != null) {
                  if (!manager.registerTransmitter(this.frequencyId, this.f_58857_.m_46472_(), this.f_58858_, this.getMainNode().getNode(), this.isAdvanced())) {
                     this.frequencyId = oldFreqId;
                     if (this.frequencyId > 0) {
                        manager.registerTransmitter(this.frequencyId, this.f_58857_.m_46472_(), this.f_58858_, this.getMainNode().getNode(), this.isAdvanced());
                        manager.registerDevice(
                           this.frequencyId, new WirelessFrequencyManager.DeviceEntry(this.f_58857_.m_46472_(), this.f_58858_, true, this.isAdvanced())
                        );
                     }
                  } else {
                     manager.registerDevice(
                        this.frequencyId, new WirelessFrequencyManager.DeviceEntry(this.f_58857_.m_46472_(), this.f_58858_, true, this.isAdvanced())
                     );
                  }
               }

               this.saveChanges();
               this.markForUpdate();
            }
         }
      }
   }

   public void clearFrequency() {
      this.setFrequency(-1);
   }

   private void destroyAllVirtualConnections() {
      IGridNode node = this.getMainNode().getNode();
      if (node != null) {
         for (IGridConnection conn : new ArrayList(node.getConnections())) {
            if (conn instanceof GridConnection) {
               GridConnection gc = (GridConnection)conn;
               if (gc.getDirection(node) == null) {
                  gc.destroy();
               }
            }
         }
      }
   }

   public void onMainNodeStateChanged(State reason) {
      super.onMainNodeStateChanged(reason);
      if (reason == State.GRID_BOOT) {
         this.updateManagerRegistration();
      }
   }

   private void updateManagerRegistration() {
      WirelessFrequencyManager manager = WirelessFrequencyManager.get();
      if (manager != null && this.frequencyId > 0 && this.f_58857_ != null) {
         if (!manager.isFrequencyValid(this.frequencyId)) {
            this.frequencyId = -1;
            this.saveChanges();
            this.markForUpdate();
         } else if (!manager.registerTransmitter(this.frequencyId, this.f_58857_.m_46472_(), this.f_58858_, this.getMainNode().getNode(), this.isAdvanced())) {
            this.frequencyId = -1;
            this.saveChanges();
            this.markForUpdate();
         } else {
            manager.registerDevice(this.frequencyId, new WirelessFrequencyManager.DeviceEntry(this.f_58857_.m_46472_(), this.f_58858_, true, this.isAdvanced()));
            this.markForUpdate();
         }
      }
   }

   public void onReady() {
      super.onReady();
      this.updateManagerRegistration();
   }

   public void m_7651_() {
      WirelessFrequencyManager manager = WirelessFrequencyManager.get();
      if (manager != null && this.frequencyId > 0) {
         this.destroyAllVirtualConnections();
         manager.unregisterTransmitter(this.frequencyId);
         if (this.f_58857_ != null) {
            manager.unregisterDevice(this.frequencyId, this.f_58857_.m_46472_(), this.f_58858_);
         }
      }

      super.m_7651_();
   }

   public void m_6339_() {
      super.m_6339_();
      this.updateManagerRegistration();
   }

   @Override
   protected Item getItemFromBlockEntity() {
      return ((WirelessOverloadedControllerBlock)ModBlocks.WIRELESS_OVERLOADED_CONTROLLER.get()).m_5456_();
   }

   public static void wirelessServerTick(Level level, BlockPos pos, BlockState state, WirelessOverloadedControllerBlockEntity be) {
      OverloadedControllerBlockEntity.serverTick(level, pos, state, be);
   }

   public void m_183515_(CompoundTag tag) {
      super.m_183515_(tag);
      tag.m_128405_("FrequencyId", this.frequencyId);
   }

   public void loadTag(CompoundTag tag) {
      super.loadTag(tag);
      this.frequencyId = tag.m_128441_("FrequencyId") ? tag.m_128451_("FrequencyId") : -1;
   }

   public void exportSettings(SettingsFrom mode, CompoundTag output, @Nullable Player player) {
      super.exportSettings(mode, output, player);
      FrequencyBindingHelper.exportMemorySettings(mode, output, this.frequencyId);
   }

   public void importSettings(SettingsFrom mode, CompoundTag input, @Nullable Player player) {
      super.importSettings(mode, input, player);
      FrequencyBindingHelper.importMemorySettings(mode, input, this::setFrequency);
   }
}
