package com.moakiee.ae2lt.api;

import com.moakiee.ae2lt.api.lightning.ILightningEnergyHandler;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

public final class AE2LTCapabilities {
   public static final Capability<ILightningEnergyHandler> LIGHTNING_ENERGY_BLOCK = CapabilityManager.get(new CapabilityToken<ILightningEnergyHandler>() {
   });
   public static final Capability<ILightningEnergyHandler> LIGHTNING_ENERGY_ITEM = LIGHTNING_ENERGY_BLOCK;

   private AE2LTCapabilities() {
   }
}
