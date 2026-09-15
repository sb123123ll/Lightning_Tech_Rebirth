package com.moakiee.ae2lt.machine.crystalcatalyzer;

import com.moakiee.ae2lt.machine.overloadfactory.NotifyingFluidTank;
import java.util.Objects;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;

public final class CrystalCatalyzerFluidHandler implements IFluidHandler {
   private final NotifyingFluidTank tank;

   public CrystalCatalyzerFluidHandler(NotifyingFluidTank tank) {
      this.tank = Objects.requireNonNull(tank, "tank");
   }

   public int getTanks() {
      return 1;
   }

   public FluidStack getFluidInTank(int tankIndex) {
      return tankIndex == 0 ? this.tank.getFluid() : FluidStack.EMPTY;
   }

   public int getTankCapacity(int tankIndex) {
      return tankIndex == 0 ? this.tank.getCapacity() : 0;
   }

   public boolean isFluidValid(int tankIndex, FluidStack stack) {
      return tankIndex == 0 && this.tank.isFluidValid(stack);
   }

   public int fill(FluidStack resource, FluidAction action) {
      return this.tank.fill(resource, action);
   }

   public FluidStack drain(FluidStack resource, FluidAction action) {
      return FluidStack.EMPTY;
   }

   public FluidStack drain(int maxDrain, FluidAction action) {
      return FluidStack.EMPTY;
   }
}
