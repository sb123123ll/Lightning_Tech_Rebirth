package com.moakiee.ae2lt.logic;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.storage.MEStorage;
import com.moakiee.ae2lt.logic.energy.AppFluxBridge;
import net.minecraft.world.item.Item;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

public final class AppFluxHelper {
   @Nullable
   public static final AEKey FE_KEY = AppFluxBridge.FE_KEY;
   public static final long TRANSFER_RATE = AppFluxBridge.TRANSFER_RATE;

   private AppFluxHelper() {
   }

   public static boolean isAvailable() {
      return AppFluxBridge.isAvailable();
   }

   @Nullable
   public static Item getInductionCard() {
      return AppFluxBridge.getInductionCard();
   }

   public static boolean isInductionCard(Item item) {
      return AppFluxBridge.isInductionCard(item);
   }

   public static int simulateReceivable(IEnergyStorage target) {
      return !isAvailable() ? 0 : target.receiveEnergy(getTransferRateIntHint(), true);
   }

   public static int pullPowerFromNetwork(MEStorage meStorage, IEnergyStorage target, IActionSource source) {
      if (isAvailable() && FE_KEY != null) {
         int requested = target.receiveEnergy(getTransferRateIntHint(), true);
         if (requested <= 0) {
            return 0;
         } else {
            long extracted = meStorage.extract(FE_KEY, (long)requested, Actionable.MODULATE, source);
            if (extracted <= 0L) {
               return 0;
            } else {
               int accepted = target.receiveEnergy((int)Math.min(extracted, 2147483647L), false);
               long remainder = extracted - (long)accepted;
               if (remainder > 0L) {
                  meStorage.insert(FE_KEY, remainder, Actionable.MODULATE, source);
               }

               return accepted;
            }
         }
      } else {
         return 0;
      }
   }

   private static int getTransferRateIntHint() {
      return (int)Math.min(2147483647L, Math.max(0L, TRANSFER_RATE));
   }
}
