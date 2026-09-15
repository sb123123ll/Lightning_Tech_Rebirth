package com.moakiee.ae2lt.logic.energy;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.storage.IStorageService;
import appeng.api.stacks.AEKey;
import appeng.api.storage.MEStorage;
import appeng.api.storage.StorageCells;
import com.glodblock.github.appflux.api.IFluxCell;
import com.glodblock.github.appflux.common.me.cell.FluxCellInventory;
import com.glodblock.github.appflux.common.me.key.FluxKey;
import com.glodblock.github.appflux.common.me.key.type.EnergyType;
import com.glodblock.github.appflux.config.AFConfig;
import java.util.function.Supplier;
import mekanism.api.Action;
import mekanism.api.energy.IStrictEnergyHandler;
import mekanism.api.math.FloatingLong;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.config.MekanismConfig;
import mekanism.common.util.UnitDisplayUtils.EnergyUnit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;
import sonar.fluxnetworks.api.FluxCapabilities;
import sonar.fluxnetworks.api.energy.IFNEnergyStorage;

final class AppFluxAccess {
   static final AEKey FE_KEY = FluxKey.of(EnergyType.FE);
   static final long TRANSFER_RATE;
   private static final boolean FLUX_NETWORKS_LOADED = isClassPresent("sonar.fluxnetworks.api.energy.IFNEnergyStorage");
   private static final boolean MEKANISM_LOADED = isClassPresent("mekanism.common.capabilities.Capabilities");

   static boolean isFluxCell(ItemStack stack) {
      return !stack.m_41619_() && stack.m_41720_() instanceof IFluxCell;
   }

   @Nullable
   static Object createCapCache(ServerLevel level, BlockPos pos, Supplier<IGrid> gridSupplier) {
      return new AppFluxAccess.CapabilityTargetCache(level, pos);
   }

   @Nullable
   static TargetAccess resolveEnergyTarget(Object energyCapCache, Direction side) {
      if (energyCapCache instanceof AppFluxAccess.CapabilityTargetCache cache) {
         TargetAccess target = resolveFluxNetworkTarget(cache, side);
         if (target != null) {
            return target;
         } else {
            target = resolveMekanismTarget(cache, side);
            return target != null ? target : AppFluxAccess.ForgeEnergyTarget.resolve(cache, side);
         }
      } else {
         return null;
      }
   }

   static long simulateTarget(@Nullable TargetAccess access, long maxFe) {
      return access != null && maxFe > 0L ? Math.max(0L, access.simulateReceive(maxFe)) : 0L;
   }

   static long sendToTarget(@Nullable TargetAccess access, IStorageService storage, IActionSource source, long maxFe) {
      if (access != null && maxFe > 0L) {
         long requested = Math.max(0L, access.simulateReceive(maxFe));
         if (requested <= 0L) {
            return 0L;
         } else {
            long extracted = storage.getInventory().extract(FE_KEY, Math.min(requested, maxFe), Actionable.MODULATE, source);
            if (extracted <= 0L) {
               return 0L;
            } else {
               long accepted = Math.min(extracted, Math.max(0L, access.receive(extracted)));
               long leftover = extracted - accepted;
               if (leftover > 0L) {
                  storage.getInventory().insert(FE_KEY, leftover, Actionable.MODULATE, source);
               }

               return accepted;
            }
         }
      } else {
         return 0L;
      }
   }

   static long sendToTargetKnownDemand(@Nullable TargetAccess access, IStorageService storage, IActionSource source, long requested) {
      if (access != null && requested > 0L) {
         long extracted = storage.getInventory().extract(FE_KEY, requested, Actionable.MODULATE, source);
         if (extracted <= 0L) {
            return 0L;
         } else {
            long accepted = Math.min(extracted, Math.max(0L, access.receive(extracted)));
            long leftover = extracted - accepted;
            if (leftover > 0L) {
               storage.getInventory().insert(FE_KEY, leftover, Actionable.MODULATE, source);
            }

            return accepted;
         }
      } else {
         return 0L;
      }
   }

   static long sendToTargetKnownDemand(@Nullable TargetAccess access, BufferedMEStorage buffer, IActionSource source, long requested) {
      if (access != null && requested > 0L) {
         long extracted = buffer.extract(FE_KEY, requested, Actionable.MODULATE, source);
         if (extracted <= 0L) {
            return 0L;
         } else {
            long accepted = Math.min(extracted, Math.max(0L, access.receive(extracted)));
            long leftover = extracted - accepted;
            if (leftover > 0L) {
               buffer.insert(FE_KEY, leftover, Actionable.MODULATE, source);
            }

            return accepted;
         }
      } else {
         return 0L;
      }
   }

   static long sendToTargetRepeatedOptimistic(@Nullable TargetAccess access, BufferedMEStorage buffer, IActionSource source, long maxFe, int maxCalls) {
      if (access != null && maxFe > 0L && maxCalls > 0) {
         long totalAccepted = 0L;
         long remaining = 0L;

         try {
            for (int i = 0; i < maxCalls; i++) {
               if (remaining <= 0L) {
                  remaining = extractRepeatedBudget(access, buffer, source, maxFe, maxCalls - i);
                  if (remaining <= 0L) {
                     break;
                  }
               }

               long attempt = remaining < maxFe ? remaining : maxFe;
               long accepted = access.receive(attempt);
               if (accepted <= 0L) {
                  break;
               }

               if (accepted > attempt) {
                  accepted = attempt;
               }

               remaining -= accepted;
               totalAccepted = saturatingAdd(totalAccepted, accepted);
            }
         } finally {
            if (remaining > 0L) {
               buffer.returnFromDirectSend(remaining, source);
            }
         }

         return totalAccepted;
      } else {
         return 0L;
      }
   }

   private static long extractRepeatedBudget(TargetAccess access, BufferedMEStorage buffer, IActionSource source, long maxFe, int remainingCalls) {
      long budget = saturatingMul(maxFe, (long)remainingCalls);
      long extracted = buffer.extractForDirectSend(budget, source);
      if (extracted > 0L) {
         return extracted;
      } else {
         long simulated = access.simulateReceive(maxFe);
         long currentDemand = simulated < maxFe ? simulated : maxFe;
         if (currentDemand <= 0L) {
            return 0L;
         } else {
            long refillDemand = buffer.refillBudgetForDirectSend(currentDemand);
            if (refillDemand <= 0L) {
               return 0L;
            } else {
               buffer.refillForDirectSend(refillDemand, source);
               return buffer.extractForDirectSend(budget, source);
            }
         }
      }
   }

   static long getFluxCellCapacity(ItemStack stack) {
      if (StorageCells.getCellInventory(stack, null) instanceof FluxCellInventory fluxInv) {
         return Math.max(0L, fluxInv.getMaxEnergy());
      } else {
         return stack.m_41720_() instanceof IFluxCell fluxCell ? Math.max(0L, fluxCell.getBytes(stack)) : 0L;
      }
   }

   static void persistCellStorage(@Nullable MEStorage storage) {
      if (storage instanceof FluxCellInventory fluxInv) {
         fluxInv.persist();
      }
   }

   private AppFluxAccess() {
   }

   @Nullable
   private static TargetAccess resolveFluxNetworkTarget(AppFluxAccess.CapabilityTargetCache cache, Direction side) {
      if (!FLUX_NETWORKS_LOADED) {
         return null;
      } else {
         try {
            IFNEnergyStorage target = getTargetCapability(cache, side, FluxCapabilities.FN_ENERGY_STORAGE);
            return target != null ? new AppFluxAccess.FluxNetworkTarget(target) : null;
         } catch (LinkageError var3) {
            return null;
         }
      }
   }

   @Nullable
   private static TargetAccess resolveMekanismTarget(AppFluxAccess.CapabilityTargetCache cache, Direction side) {
      if (!MEKANISM_LOADED) {
         return null;
      } else {
         try {
            IStrictEnergyHandler target = getTargetCapability(cache, side, Capabilities.STRICT_ENERGY);
            return target != null ? AppFluxAccess.MekanismStrictTarget.create(target) : null;
         } catch (LinkageError var3) {
            return null;
         }
      }
   }

   @Nullable
   private static <T> T getTargetCapability(AppFluxAccess.CapabilityTargetCache cache, Direction side, Capability<T> capability) {
      BlockEntity target = cache.resolveTarget(side);
      return (T)(target == null ? null : target.getCapability(capability, side.m_122424_()).resolve().orElse(null));
   }

   private static int clampToInt(long value) {
      return value >= 2147483647L ? Integer.MAX_VALUE : (int)Math.max(0L, value);
   }

   private static long maxForgeEnergyFor(double feToJoules) {
      if (!Double.isNaN(feToJoules) && !(feToJoules <= 0.0)) {
         double max = 9.223372E18F / feToJoules;
         return max >= 9.223372E18F ? Long.MAX_VALUE : Math.max(1L, (long)max);
      } else {
         return Long.MAX_VALUE;
      }
   }

   private static long saturatingAdd(long a, long b) {
      long r = a + b;
      return ((a ^ r) & (b ^ r)) < 0L ? Long.MAX_VALUE : r;
   }

   private static long saturatingMul(long a, long b) {
      if (a > 0L && b > 0L) {
         return a > Long.MAX_VALUE / b ? Long.MAX_VALUE : a * b;
      } else {
         return 0L;
      }
   }

   private static boolean isClassPresent(String className) {
      try {
         Class.forName(className, false, AppFluxAccess.class.getClassLoader());
         return true;
      } catch (LinkageError | ClassNotFoundException var2) {
         return false;
      }
   }

   static {
      long rate = AFConfig.getFluxAccessorIO();
      TRANSFER_RATE = rate == 0L ? Long.MAX_VALUE : Math.max(0L, rate);
   }

   private static record CapabilityTargetCache(ServerLevel level, BlockPos pos) {
      private CapabilityTargetCache(ServerLevel level, BlockPos pos) {
         pos = pos.m_7949_();
         this.level = level;
         this.pos = pos;
      }

      @Nullable
      private BlockEntity resolveTarget(Direction side) {
         BlockPos targetPos = this.pos.m_121945_(side);
         return this.level.m_46749_(targetPos) ? this.level.m_7702_(targetPos) : null;
      }
   }

   private static record FluxNetworkTarget(IFNEnergyStorage target) implements TargetAccess {
      @Override
      public long simulateReceive(long maxFe) {
         return Math.max(0L, this.target.receiveEnergyL(maxFe, true));
      }

      @Override
      public long receive(long amountFe) {
         return Math.max(0L, this.target.receiveEnergyL(amountFe, false));
      }
   }

   private static record ForgeEnergyTarget(IEnergyStorage target) implements TargetAccess {
      @Nullable
      static TargetAccess resolve(AppFluxAccess.CapabilityTargetCache cache, Direction side) {
         IEnergyStorage target = AppFluxAccess.getTargetCapability(cache, side, ForgeCapabilities.ENERGY);
         return target != null ? new AppFluxAccess.ForgeEnergyTarget(target) : null;
      }

      @Override
      public long simulateReceive(long maxFe) {
         return (long)Math.max(0, this.target.receiveEnergy(AppFluxAccess.clampToInt(maxFe), true));
      }

      @Override
      public long receive(long amountFe) {
         return (long)Math.max(0, this.target.receiveEnergy(AppFluxAccess.clampToInt(amountFe), false));
      }
   }

   private static final class MekanismStrictTarget implements TargetAccess {
      private final IStrictEnergyHandler target;
      private final FloatingLong maxJoulesPerCall;
      private final long maxForgeEnergyPerCall;

      private MekanismStrictTarget(IStrictEnergyHandler target, FloatingLong maxJoulesPerCall, long maxForgeEnergyPerCall) {
         this.target = target;
         this.maxJoulesPerCall = maxJoulesPerCall;
         this.maxForgeEnergyPerCall = maxForgeEnergyPerCall;
      }

      static AppFluxAccess.MekanismStrictTarget create(IStrictEnergyHandler target) {
         double rate = ((FloatingLong)MekanismConfig.general.forgeConversionRate.get()).doubleValue();
         long maxFe = AppFluxAccess.maxForgeEnergyFor(rate);
         long cappedTransfer = AppFluxAccess.TRANSFER_RATE > 0L ? Math.min(AppFluxAccess.TRANSFER_RATE, maxFe) : 0L;
         FloatingLong maxJ = cappedTransfer > 0L ? EnergyUnit.FORGE_ENERGY.convertFrom(cappedTransfer) : FloatingLong.ZERO;
         return new AppFluxAccess.MekanismStrictTarget(target, maxJ, maxFe);
      }

      @Override
      public long simulateReceive(long maxFe) {
         return this.insert(maxFe, Action.SIMULATE);
      }

      @Override
      public long receive(long amountFe) {
         return this.insert(amountFe, Action.EXECUTE);
      }

      private long insert(long amountFe, Action action) {
         if (amountFe <= 0L) {
            return 0L;
         } else {
            long effectiveAmountFe = Math.min(amountFe, this.maxForgeEnergyPerCall);
            if (effectiveAmountFe <= 0L) {
               return 0L;
            } else {
               FloatingLong mekanismAmount = amountFe == AppFluxAccess.TRANSFER_RATE
                  ? this.maxJoulesPerCall
                  : EnergyUnit.FORGE_ENERGY.convertFrom(effectiveAmountFe);
               if (mekanismAmount.isZero()) {
                  return 0L;
               } else {
                  FloatingLong remainder = this.target.insertEnergy(mekanismAmount, action);
                  if (remainder.isZero()) {
                     return effectiveAmountFe;
                  } else if (remainder.greaterOrEqual(mekanismAmount)) {
                     return 0L;
                  } else {
                     FloatingLong acceptedJ = mekanismAmount.subtract(remainder);
                     return Math.min(effectiveAmountFe, EnergyUnit.FORGE_ENERGY.convertToAsLong(acceptedJ));
                  }
               }
            }
         }
      }
   }
}
