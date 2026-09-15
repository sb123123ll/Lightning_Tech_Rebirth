package com.moakiee.ae2lt.blockentity;

import appeng.api.networking.GridFlags;
import appeng.api.networking.IGrid;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.security.IActionSource;
import appeng.api.orientation.BlockOrientation;
import appeng.api.util.AECableType;
import appeng.blockentity.grid.AENetworkBlockEntity;
import appeng.hooks.ticking.TickHandler;
import appeng.me.helpers.MachineSource;
import com.moakiee.ae2lt.block.PigmeeMentalmathUnitBlock;
import com.moakiee.ae2lt.crafting.timewheel.TimeWheelCraftingCpuPool;
import com.moakiee.ae2lt.crafting.timewheel.TimeWheelCraftingCpuPoolHost;
import com.moakiee.ae2lt.crafting.timewheel.TimeWheelCraftingCpuPoolProvider;
import com.moakiee.ae2lt.registry.ModBlockEntities;
import com.moakiee.ae2lt.registry.ModBlocks;
import com.moakiee.thunderbolt.api.crafting.cpu.ExtendedCraftingCpuClusterProvider;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class PigmeeMentalmathUnitBlockEntity extends AENetworkBlockEntity implements TimeWheelCraftingCpuPoolHost {
   public static final long STORAGE_BYTES = 256L;
   public static final int PARALLELISM = 1;
   private static final String TAG_CPU_POOL = "CpuPool";
   private final IActionSource actionSource = new MachineSource(this.getMainNode()::getNode);
   private final TimeWheelCraftingCpuPool cpuPool = new TimeWheelCraftingCpuPool(this, 256L, 1, 1L, false);
   private long lastCpuDirtyTick = Long.MIN_VALUE;

   public PigmeeMentalmathUnitBlockEntity(BlockPos pos, BlockState state) {
      super((BlockEntityType)ModBlockEntities.PIGMEE_MENTALMATH_UNIT.get(), pos, state);
   }

   protected IManagedGridNode createMainNode() {
      return super.createMainNode()
         .setTagName("pigmee_mentalmath_unit")
         .setVisualRepresentation((ItemLike)ModBlocks.PIGMEE_MENTALMATH_UNIT.get())
         .setIdlePowerUsage(1.0)
         .setFlags(new GridFlags[]{GridFlags.REQUIRE_CHANNEL})
         .addService(ExtendedCraftingCpuClusterProvider.class, this)
         .addService(TimeWheelCraftingCpuPoolProvider.class, this);
   }

   public AECableType getCableConnectionType(Direction dir) {
      return AECableType.DENSE_SMART;
   }

   public Set<Direction> getGridConnectableSides(BlockOrientation orientation) {
      return EnumSet.allOf(Direction.class);
   }

   @Override
   public TimeWheelCraftingCpuPool getTimeWheelCraftingCpuPool() {
      return this.cpuPool;
   }

   @Override
   public IActionSource getActionSource() {
      return this.actionSource;
   }

   @Override
   public IGrid getGrid() {
      return this.getMainNode().getGrid();
   }

   @Override
   public boolean isCpuActive() {
      return this.getMainNode().isActive() && this.getMainNode().getGrid() != null;
   }

   @Override
   public void markCpuDirty() {
      long now = TickHandler.instance().getCurrentTick();
      if (this.lastCpuDirtyTick != now) {
         this.lastCpuDirtyTick = now;
         this.saveChanges();
      }
   }

   @Override
   public Component getCpuDisplayName() {
      return Component.m_237115_("block.ae2lt.pigmee_mentalmath_unit");
   }

   @Nullable
   @Override
   public Level getCpuLevel() {
      return this.f_58857_;
   }

   public void m_183515_(CompoundTag tag) {
      super.m_183515_(tag);
      tag.m_128473_("CpuPool");
      if (this.cpuPool.hasPersistentState()) {
         CompoundTag poolTag = new CompoundTag();
         this.cpuPool.writeToNBT(poolTag, this.f_58857_ != null ? this.f_58857_.m_9598_() : null);
         if (!poolTag.m_128456_()) {
            tag.m_128365_("CpuPool", poolTag);
         }
      }
   }

   public void loadTag(CompoundTag tag) {
      super.loadTag(tag);
      this.cpuPool
         .readFromNBT(tag.m_128425_("CpuPool", 10) ? tag.m_128469_("CpuPool") : new CompoundTag(), this.f_58857_ != null ? this.f_58857_.m_9598_() : null);
   }

   public void onLoad() {
      super.onLoad();
      this.cpuPool.resolvePendingLoad();
   }

   public void addAdditionalDrops(Level level, BlockPos pos, List<ItemStack> drops) {
      super.addAdditionalDrops(level, pos, drops);
      this.cpuPool.addRemovalDrops(level, pos, drops);
   }

   public void m_6211_() {
      super.m_6211_();
      this.cpuPool.clearRemovedContent();
   }

   protected Item getItemFromBlockEntity() {
      return ((PigmeeMentalmathUnitBlock)ModBlocks.PIGMEE_MENTALMATH_UNIT.get()).m_5456_();
   }
}
