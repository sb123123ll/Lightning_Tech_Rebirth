package com.moakiee.ae2lt.machine.lightningassembly;

import java.util.Objects;
import net.minecraftforge.energy.IEnergyStorage;

public final class LightningAssemblyChamberEnergyStorage implements IEnergyStorage {
   private final long capacity;
   private final Runnable changeListener;
   private long storedEnergy;

   public LightningAssemblyChamberEnergyStorage(long capacity, Runnable changeListener) {
      if (capacity <= 0L) {
         throw new IllegalArgumentException("capacity must be positive");
      } else {
         this.capacity = capacity;
         this.changeListener = Objects.requireNonNull(changeListener, "changeListener");
      }
   }

   public int receiveEnergy(int maxReceive, boolean simulate) {
      if (maxReceive <= 0) {
         return 0;
      } else {
         long accepted = Math.min(Integer.toUnsignedLong(maxReceive), this.capacity - this.storedEnergy);
         if (accepted <= 0L) {
            return 0;
         } else {
            if (!simulate) {
               this.storedEnergy += accepted;
               this.changeListener.run();
            }

            return (int)accepted;
         }
      }
   }

   public int extractEnergy(int maxExtract, boolean simulate) {
      return 0;
   }

   public int extractInternal(long amount, boolean simulate) {
      if (amount <= 0L) {
         return 0;
      } else {
         long extracted = Math.min(this.storedEnergy, Math.min(amount, 2147483647L));
         if (extracted <= 0L) {
            return 0;
         } else {
            if (!simulate) {
               this.storedEnergy -= extracted;
               this.changeListener.run();
            }

            return (int)extracted;
         }
      }
   }

   public long getStoredEnergyLong() {
      return this.storedEnergy;
   }

   public long getCapacityLong() {
      return this.capacity;
   }

   public void setStoredEnergy(long storedEnergy) {
      long clamped = Math.max(0L, Math.min(storedEnergy, this.capacity));
      if (this.storedEnergy != clamped) {
         this.storedEnergy = clamped;
         this.changeListener.run();
      } else {
         this.storedEnergy = clamped;
      }
   }

   public void loadStoredEnergy(long storedEnergy) {
      this.storedEnergy = Math.max(0L, Math.min(storedEnergy, this.capacity));
   }

   public int getEnergyStored() {
      return (int)Math.min(2147483647L, this.storedEnergy);
   }

   public int getMaxEnergyStored() {
      return (int)Math.min(2147483647L, this.capacity);
   }

   public boolean canExtract() {
      return false;
   }

   public boolean canReceive() {
      return true;
   }
}
