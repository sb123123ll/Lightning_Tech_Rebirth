package com.moakiee.ae2lt.blockentity;

import com.moakiee.ae2lt.logic.EjectModeRegistry;
import com.moakiee.ae2lt.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

public class GhostOutputBlockEntity extends BlockEntity {
   private static final IItemHandler REJECTING_ITEM_HANDLER = new IItemHandler() {
      public int getSlots() {
         return 1;
      }

      public ItemStack getStackInSlot(int slot) {
         return ItemStack.f_41583_;
      }

      public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
         return stack;
      }

      public ItemStack extractItem(int slot, int amount, boolean simulate) {
         return ItemStack.f_41583_;
      }

      public int getSlotLimit(int slot) {
         return 0;
      }

      public boolean isItemValid(int slot, ItemStack stack) {
         return false;
      }
   };
   private static final IFluidHandler REJECTING_FLUID_HANDLER = new IFluidHandler() {
      public int getTanks() {
         return 1;
      }

      public FluidStack getFluidInTank(int tank) {
         return FluidStack.EMPTY;
      }

      public int getTankCapacity(int tank) {
         return 0;
      }

      public boolean isFluidValid(int tank, FluidStack stack) {
         return false;
      }

      public int fill(FluidStack resource, FluidAction action) {
         return 0;
      }

      public FluidStack drain(FluidStack resource, FluidAction action) {
         return FluidStack.EMPTY;
      }

      public FluidStack drain(int maxDrain, FluidAction action) {
         return FluidStack.EMPTY;
      }
   };

   public GhostOutputBlockEntity(BlockPos pos) {
      super((BlockEntityType)ModBlockEntities.GHOST_OUTPUT.get(), pos, Blocks.f_50016_.m_49966_());
   }

   public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
      if (this.f_58857_ != null && side != null && !EjectModeRegistry.isBypassed()) {
         EjectModeRegistry.EjectEntry entry = EjectModeRegistry.lookupByFace(this.f_58857_.m_46472_(), this.f_58858_.m_121878_(), side);
         if (entry == null) {
            return super.getCapability(capability, side);
         } else {
            BlockEntity host = entry.getHost();
            if (host != null && host.m_58904_() != null) {
               EjectModeRegistry.setBypass(true);

               LazyOptional var5;
               try {
                  var5 = host.getCapability(capability, side);
               } finally {
                  EjectModeRegistry.setBypass(false);
               }

               return var5;
            } else if (capability == ForgeCapabilities.ITEM_HANDLER) {
               return LazyOptional.of(() -> REJECTING_ITEM_HANDLER).cast();
            } else {
               return capability == ForgeCapabilities.FLUID_HANDLER ? LazyOptional.of(() -> REJECTING_FLUID_HANDLER).cast() : LazyOptional.empty();
            }
         }
      } else {
         return super.getCapability(capability, side);
      }
   }
}
