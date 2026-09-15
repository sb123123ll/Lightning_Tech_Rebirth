package com.moakiee.ae2lt.blockentity;

import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.IGridNodeListener.State;
import appeng.api.orientation.BlockOrientation;
import appeng.api.util.AECableType;
import appeng.blockentity.ServerTickingBlockEntity;
import appeng.blockentity.grid.AENetworkBlockEntity;
import appeng.util.SettingsFrom;
import com.moakiee.ae2lt.block.WirelessReceiverBlock;
import com.moakiee.ae2lt.grid.FrequencyBindingHelper;
import com.moakiee.ae2lt.grid.FrequencyBindingHost;
import com.moakiee.ae2lt.registry.ModBlockEntities;
import com.moakiee.ae2lt.registry.ModBlocks;
import com.moakiee.thunderbolt.api.channel.HighCapacityChannelOwner;
import java.util.EnumSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class WirelessReceiverBlockEntity extends AENetworkBlockEntity implements HighCapacityChannelOwner, FrequencyBindingHost, ServerTickingBlockEntity {
   private final FrequencyBindingHelper frequencyBinding = new FrequencyBindingHelper(this);

   public WirelessReceiverBlockEntity(BlockPos pos, BlockState state) {
      super((BlockEntityType)ModBlockEntities.WIRELESS_RECEIVER.get(), pos, state);
   }

   protected IManagedGridNode createMainNode() {
      return super.createMainNode().setTagName("wireless_receiver").setVisualRepresentation((ItemLike)ModBlocks.WIRELESS_RECEIVER.get()).setIdlePowerUsage(5.0);
   }

   public AECableType getCableConnectionType(Direction dir) {
      return AECableType.DENSE_SMART;
   }

   public Set<Direction> getGridConnectableSides(BlockOrientation orientation) {
      return EnumSet.allOf(Direction.class);
   }

   @Override
   public FrequencyBindingHelper getFrequencyBinding() {
      return this.frequencyBinding;
   }

   @Override
   public AENetworkBlockEntity getFrequencyBindingBlockEntity() {
      return this;
   }

   @Override
   public void saveFrequencyBindingChanges() {
      this.saveChanges();
   }

   @Override
   public void markFrequencyBindingForUpdate() {
      this.markForUpdate();
   }

   public boolean isConnected() {
      return this.frequencyBinding.isConnected();
   }

   public void onMainNodeStateChanged(State reason) {
      super.onMainNodeStateChanged(reason);
      this.frequencyBinding.onMainNodeStateChanged(reason);
   }

   public void serverTick() {
      this.frequencyBinding.serverTick();
   }

   public void onReady() {
      super.onReady();
      this.frequencyBinding.onReady();
   }

   public void m_7651_() {
      this.frequencyBinding.setRemoved();
      super.m_7651_();
   }

   public void onChunkUnloaded() {
      this.frequencyBinding.onChunkUnloaded();
      super.onChunkUnloaded();
   }

   public void m_6339_() {
      super.m_6339_();
      this.frequencyBinding.clearRemoved();
   }

   protected Item getItemFromBlockEntity() {
      return ((WirelessReceiverBlock)ModBlocks.WIRELESS_RECEIVER.get()).m_5456_();
   }

   public void m_183515_(CompoundTag tag) {
      super.m_183515_(tag);
      this.frequencyBinding.save(tag);
   }

   public void loadTag(CompoundTag tag) {
      super.loadTag(tag);
      this.frequencyBinding.load(tag);
   }

   public void exportSettings(SettingsFrom mode, CompoundTag output, @Nullable Player player) {
      super.exportSettings(mode, output, player);
      FrequencyBindingHelper.exportMemorySettings(mode, output, this.getFrequencyId());
   }

   public void importSettings(SettingsFrom mode, CompoundTag input, @Nullable Player player) {
      super.importSettings(mode, input, player);
      FrequencyBindingHelper.importMemorySettings(mode, input, this::setFrequency);
   }
}
