package com.moakiee.ae2lt.machine.common;

import java.util.Optional;
import net.minecraftforge.energy.IEnergyStorage;

public interface GridRecipeMachineHost<L, C> {
   boolean hasLockedRecipe();

   Optional<L> getLockedRecipe();

   Optional<L> lockCurrentRecipe();

   void resetProgressState();

   void setWorking(boolean var1);

   boolean pushOutResult();

   boolean hasAutoExportWork();

   void abortProcessing();

   long getConsumedEnergy();

   int getProcessingTicksSpent();

   boolean completeLockedRecipe(L var1, C var2);

   long getMachineStoredEnergy();

   IEnergyStorage getMachineEnergyStorage();

   int extractMachineEnergy(long var1);

   void onEnergyConsumed(int var1);
}
