package com.moakiee.ae2lt.celestweave;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.MEStorage;
import com.moakiee.ae2lt.device.network.ArmorNetworkBinding;
import com.moakiee.ae2lt.device.network.BindingResolveResult;
import com.moakiee.ae2lt.logic.energy.AppFluxBridge;
import com.moakiee.ae2lt.registry.ModDataComponents;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.energy.IEnergyStorage;

public final class ArmorEnergyBuffer {
   private ArmorEnergyBuffer() {
   }

   public static long read(ItemStack stack) {
      return read(stack, null);
   }

   public static long read(ItemStack stack, Provider registries) {
      Long value = ModDataComponents.CELESTWEAVE_ENERGY_BUFFER.get(stack);
      return Math.max(0L, Math.min(capacity(stack, registries), value == null ? 0L : value));
   }

   public static long capacity(ItemStack stack) {
      return capacity(stack, null);
   }

   public static long capacity(ItemStack stack, Provider registries) {
      return ArmorEnergyRules.capacityForExtraModuleFe(ArmorEnergyModuleStorage.capacityFe(stack, registries));
   }

   public static void write(ItemStack stack, long value) {
      write(stack, null, value);
   }

   public static void write(ItemStack stack, Provider registries, long value) {
      if (stack != null && !stack.m_41619_()) {
         ModDataComponents.CELESTWEAVE_ENERGY_BUFFER.set(stack, Math.max(0L, Math.min(capacity(stack, registries), value)));
      }
   }

   public static void clamp(ItemStack stack) {
      write(stack, read(stack));
   }

   public static void clamp(ItemStack stack, Provider registries) {
      write(stack, registries, read(stack, registries));
   }

   public static boolean tryConsume(ItemStack stack, ServerPlayer player, long amount) {
      if (amount <= 0L) {
         return true;
      } else {
         RegistryAccess registries = player.m_9236_().m_9598_();
         long buffered = read(stack, registries);
         if (buffered >= amount) {
            write(stack, registries, buffered - amount);
            return true;
         } else {
            return false;
         }
      }
   }

   public static long refillFromNetwork(ItemStack stack, ServerPlayer player, long maxAmount) {
      if (stack != null && !stack.m_41619_() && player != null && maxAmount > 0L) {
         RegistryAccess registries = player.m_9236_().m_9598_();
         long room = capacity(stack, registries) - read(stack, registries);
         if (room <= 0L) {
            return 0L;
         } else if (AppFluxBridge.isAvailable() && AppFluxBridge.FE_KEY != null) {
            BindingResolveResult bound = ArmorNetworkBinding.INSTANCE.resolve(stack, player);
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
                     write(stack, registries, read(stack, registries) + got);
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
      return receiveFe(stack, null, amount, simulate);
   }

   public static int receiveFe(ItemStack stack, Provider registries, int amount, boolean simulate) {
      return (int)receiveFe(stack, registries, (long)amount, simulate);
   }

   public static long receiveFe(ItemStack stack, Provider registries, long amount, boolean simulate) {
      long accepted = Math.min(Math.max(0L, amount), Math.max(0L, capacity(stack, registries) - read(stack, registries)));
      if (!simulate && accepted > 0L) {
         write(stack, registries, read(stack, registries) + accepted);
      }

      return accepted;
   }

   public static IEnergyStorage asEnergyStorage(final ItemStack stack) {
      return new IEnergyStorage() {
         public int receiveEnergy(int maxReceive, boolean simulate) {
            return ArmorEnergyBuffer.receiveFe(stack, maxReceive, simulate);
         }

         public int extractEnergy(int maxExtract, boolean simulate) {
            return 0;
         }

         public int getEnergyStored() {
            return (int)Math.min(2147483647L, ArmorEnergyBuffer.read(stack));
         }

         public int getMaxEnergyStored() {
            return (int)Math.min(2147483647L, ArmorEnergyBuffer.capacity(stack));
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
