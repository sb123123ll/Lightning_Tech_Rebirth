package com.moakiee.ae2lt.machine.overloadfactory;

import java.util.Objects;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;

public final class OverloadProcessingFactoryFluidHandler implements IFluidHandler {
   private final NotifyingFluidTank inputTank;
   private final NotifyingFluidTank outputTank;

   public OverloadProcessingFactoryFluidHandler(NotifyingFluidTank inputTank, NotifyingFluidTank outputTank) {
      this.inputTank = Objects.requireNonNull(inputTank, "inputTank");
      this.outputTank = Objects.requireNonNull(outputTank, "outputTank");
   }

   public int getTanks() {
      return 2;
   }

   public FluidStack getFluidInTank(int tank) {
      return switch (tank) {
         case 0 -> this.inputTank.getFluid();
         case 1 -> this.outputTank.getFluid();
         default -> FluidStack.EMPTY;
      };
   }

   public int getTankCapacity(int tank) {
      return switch (tank) {
         case 0 -> this.inputTank.getCapacity();
         case 1 -> this.outputTank.getCapacity();
         default -> 0;
      };
   }

   public boolean isFluidValid(int tank, FluidStack stack) {
      return tank == 0 && this.inputTank.isFluidValid(stack);
   }

   public int fill(FluidStack resource, FluidAction action) {
      return this.inputTank.fill(resource, action);
   }

   public FluidStack drain(FluidStack resource, FluidAction action) {
      return this.outputTank.drain(resource, action);
   }

   public FluidStack drain(int maxDrain, FluidAction action) {
      return this.outputTank.drain(maxDrain, action);
   }
}
