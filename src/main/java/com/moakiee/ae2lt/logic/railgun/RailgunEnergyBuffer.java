package com.moakiee.ae2lt.logic.railgun;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.MEStorage;
import com.moakiee.ae2lt.config.AE2LTCommonConfig;
import com.moakiee.ae2lt.item.railgun.RailgunEnergyModuleRules;
import com.moakiee.ae2lt.item.railgun.RailgunEnergyModuleStorage;
import com.moakiee.ae2lt.item.railgun.RailgunEnergyRules;
import com.moakiee.ae2lt.logic.energy.AppFluxBridge;
import com.moakiee.ae2lt.registry.ModDataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.energy.IEnergyStorage;

public final class RailgunEnergyBuffer {
   private RailgunEnergyBuffer() {
   }

   public static long read(ItemStack stack) {
      Long v = ModDataComponents.RAILGUN_ENERGY_BUFFER.get(stack);
      return Math.max(0L, Math.min(capacity(stack), v == null ? 0L : v));
   }

   public static long capacity(ItemStack stack) {
      return RailgunEnergyModuleRules.capacityFromBaseAndModuleFe(AE2LTCommonConfig.railgunBufferCapacity(), RailgunEnergyModuleStorage.capacityFe(stack));
   }

   public static void write(ItemStack stack, long value) {
      long clamped = Math.max(0L, Math.min(capacity(stack), value));
      ModDataComponents.RAILGUN_ENERGY_BUFFER.set(stack, clamped);
   }

   public static void clamp(ItemStack stack) {
      write(stack, read(stack));
   }

   public static void refund(ItemStack stack, long amount) {
      if (amount > 0L) {
         write(stack, read(stack) + amount);
      }
   }

   public static boolean tryConsume(ItemStack stack, ServerPlayer player, long amount) {
      if (amount <= 0L) {
         return true;
      } else {
         long buffered = read(stack);
         if (buffered >= amount) {
            write(stack, buffered - amount);
            return true;
         } else {
            return false;
         }
      }
   }

   public static long refillFromNetwork(ItemStack stack, ServerPlayer player, long maxAmount) {
      if (stack != null && !stack.m_41619_() && player != null && maxAmount > 0L) {
         long room = capacity(stack) - read(stack);
         if (room <= 0L) {
            return 0L;
         } else if (AppFluxBridge.isAvailable() && AppFluxBridge.FE_KEY != null) {
            RailgunBinding.Result bound = RailgunBinding.resolve(stack, player);
            if (!bound.success()) {
               return 0L;
            } else {
               IGrid grid = bound.grid();
               if (grid == null) {
                  return 0L;
               } else {
                  MEStorage storage = grid.getStorageService().getInventory();
                  IActionSource source = IActionSource.ofPlayer(player);
                  long request = Math.min(room, maxAmount);
                  long got = storage.extract(AppFluxBridge.FE_KEY, request, Actionable.MODULATE, source);
                  if (got <= 0L) {
                     return 0L;
                  } else {
                     write(stack, read(stack) + got);
                     return got;
                  }
               }
            }
         } else {
            return 0L;
         }
      } else {
         return 0L;
      }
   }

   public static int receiveFe(ItemStack stack, int amount, boolean simulate) {
      int accepted = RailgunEnergyRules.receivableFe(read(stack), capacity(stack), amount);
      if (!simulate && accepted > 0) {
         write(stack, read(stack) + (long)accepted);
      }

      return accepted;
   }

   public static IEnergyStorage asEnergyStorage(final ItemStack stack) {
      return new IEnergyStorage() {
         public int receiveEnergy(int maxReceive, boolean simulate) {
            return RailgunEnergyBuffer.receiveFe(stack, maxReceive, simulate);
         }

         public int extractEnergy(int maxExtract, boolean simulate) {
            return 0;
         }

         public int getEnergyStored() {
            return (int)Math.min(2147483647L, RailgunEnergyBuffer.read(stack));
         }

         public int getMaxEnergyStored() {
            return (int)Math.min(2147483647L, RailgunEnergyBuffer.capacity(stack));
         }

         public boolean canExtract() {
            return false;
         }

         public boolean canReceive() {
            return true;
         }
      };
   }
}
