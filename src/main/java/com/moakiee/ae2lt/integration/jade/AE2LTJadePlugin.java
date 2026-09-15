package com.moakiee.ae2lt.integration.jade;

import com.moakiee.ae2lt.blockentity.FirmamentConversionCoreBlockEntity;
import com.moakiee.ae2lt.blockentity.LightningCollectorBlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin("ae2lt")
public class AE2LTJadePlugin implements IWailaPlugin {
   private static final LightningCollectorJadeProvider LIGHTNING_COLLECTOR_PROVIDER = new LightningCollectorJadeProvider();
   private static final FirmamentConversionCoreJadeProvider FIRMAMENT_CONVERSION_CORE_PROVIDER = new FirmamentConversionCoreJadeProvider();
   private static final FrequencyCardWirelessNodeJadeProvider FREQUENCY_CARD_WIRELESS_NODE_PROVIDER = new FrequencyCardWirelessNodeJadeProvider();

   public void register(IWailaCommonRegistration registration) {
      registration.registerBlockDataProvider(LIGHTNING_COLLECTOR_PROVIDER, LightningCollectorBlockEntity.class);
      registration.registerBlockDataProvider(FIRMAMENT_CONVERSION_CORE_PROVIDER, FirmamentConversionCoreBlockEntity.class);
      registration.registerBlockDataProvider(FREQUENCY_CARD_WIRELESS_NODE_PROVIDER, BlockEntity.class);
   }

   public void registerClient(IWailaClientRegistration registration) {
      registration.registerBlockComponent(LIGHTNING_COLLECTOR_PROVIDER, Block.class);
      registration.registerBlockComponent(FIRMAMENT_CONVERSION_CORE_PROVIDER, Block.class);
      registration.registerBlockComponent(FREQUENCY_CARD_WIRELESS_NODE_PROVIDER, Block.class);
   }
}
