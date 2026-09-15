package com.moakiee.ae2lt.client;

import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.style.StyleManager;
import com.moakiee.ae2lt.client.gui.FrequencyScreen;
import com.moakiee.ae2lt.client.hub.DeviceHubScreen;
import com.moakiee.ae2lt.integration.ae2wtlib.TianshuWirelessTerminalFactory;
import com.moakiee.ae2lt.menu.AtmosphericIonizerMenu;
import com.moakiee.ae2lt.menu.CrystalCatalyzerMenu;
import com.moakiee.ae2lt.menu.FrequencyMenu;
import com.moakiee.ae2lt.menu.LightningAssemblyChamberMenu;
import com.moakiee.ae2lt.menu.LightningCollectorMenu;
import com.moakiee.ae2lt.menu.LightningSimulationChamberMenu;
import com.moakiee.ae2lt.menu.MatrixControllerMenu;
import com.moakiee.ae2lt.menu.MatrixPortMenu;
import com.moakiee.ae2lt.menu.OverloadDeviceWorkbenchMenu;
import com.moakiee.ae2lt.menu.OverloadPatternEncoderMenu;
import com.moakiee.ae2lt.menu.OverloadProcessingFactoryMenu;
import com.moakiee.ae2lt.menu.OverloadedInterfaceMenu;
import com.moakiee.ae2lt.menu.OverloadedPatternProviderMenu;
import com.moakiee.ae2lt.menu.OverloadedPowerSupplyMenu;
import com.moakiee.ae2lt.menu.PigmeeMolecularAssemblerMenu;
import com.moakiee.ae2lt.menu.PigmeePatternProviderMenu;
import com.moakiee.ae2lt.menu.TeslaCoilMenu;
import com.moakiee.ae2lt.menu.TianshuPatternEncodingTermMenu;
import com.moakiee.ae2lt.menu.TianshuSeedStorageMenu;
import com.moakiee.ae2lt.menu.TianshuSupercomputerControllerMenu;
import com.moakiee.ae2lt.menu.TianshuWirelessPatternEncodingTermMenu;
import com.moakiee.ae2lt.menu.VoidCellMenu;
import com.moakiee.ae2lt.menu.hub.DeviceHubMenu;
import com.moakiee.ae2lt.registry.ModBlocks;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(
   modid = "ae2lt",
   bus = Bus.MOD,
   value = {Dist.CLIENT}
)
public class ModScreens {
   @SubscribeEvent
   public static void registerScreens(FMLClientSetupEvent event) {
      event.enqueueWork(() -> {
         MenuScreens.m_96206_(OverloadedPatternProviderMenu.TYPE, ModScreens::createOverloadedPatternProviderScreen);
         MenuScreens.m_96206_(PigmeePatternProviderMenu.TYPE, ModScreens::createPigmeePatternProviderScreen);
         MenuScreens.m_96206_(PigmeeMolecularAssemblerMenu.TYPE, ModScreens::createPigmeeMolecularAssemblerScreen);
         MenuScreens.m_96206_(OverloadPatternEncoderMenu.TYPE, OverloadPatternEncoderScreen::new);
         MenuScreens.m_96206_(OverloadDeviceWorkbenchMenu.TYPE, OverloadDeviceWorkbenchScreen::new);
         MenuScreens.m_96206_(OverloadedInterfaceMenu.TYPE, ModScreens::createOverloadedInterfaceScreen);
         if (ModBlocks.hasOverloadedPowerSupply()) {
            MenuScreens.m_96206_(OverloadedPowerSupplyMenu.TYPE, ModScreens::createOverloadedPowerSupplyScreen);
         }

         MenuScreens.m_96206_(LightningSimulationChamberMenu.TYPE, ModScreens::createLightningSimulationChamberScreen);
         MenuScreens.m_96206_(LightningAssemblyChamberMenu.TYPE, ModScreens::createLightningAssemblyChamberScreen);
         MenuScreens.m_96206_(LightningCollectorMenu.TYPE, ModScreens::createLightningCollectorScreen);
         MenuScreens.m_96206_(OverloadProcessingFactoryMenu.TYPE, ModScreens::createOverloadProcessingFactoryScreen);
         MenuScreens.m_96206_(TeslaCoilMenu.TYPE, ModScreens::createTeslaCoilScreen);
         MenuScreens.m_96206_(AtmosphericIonizerMenu.TYPE, ModScreens::createAtmosphericIonizerScreen);
         MenuScreens.m_96206_(FrequencyMenu.TYPE, FrequencyScreen::new);
         MenuScreens.m_96206_(CrystalCatalyzerMenu.TYPE, ModScreens::createCrystalCatalyzerScreen);
         MenuScreens.m_96206_(DeviceHubMenu.TYPE, DeviceHubScreen::new);
         MenuScreens.m_96206_(MatrixControllerMenu.TYPE, MatrixControllerScreen::new);
         MenuScreens.m_96206_(MatrixPortMenu.TYPE, MatrixPortScreen::new);
         MenuScreens.m_96206_(TianshuSupercomputerControllerMenu.TYPE, TianshuSupercomputerControllerScreen::new);
         MenuScreens.m_96206_(TianshuPatternEncodingTermMenu.TYPE, ModScreens::createTianshuPatternEncodingTermScreen);
         if (TianshuWirelessTerminalFactory.isAvailable()) {
            MenuScreens.m_96206_(TianshuWirelessPatternEncodingTermMenu.TYPE, ModScreens::createTianshuWirelessPatternEncodingTermScreen);
         }

         MenuScreens.m_96206_(TianshuSeedStorageMenu.TYPE, ModScreens::createTianshuSeedStorageScreen);
         MenuScreens.m_96206_(VoidCellMenu.TYPE, ModScreens::createVoidCellScreen);
      });
   }

   private static TianshuPatternEncodingTermScreen<TianshuPatternEncodingTermMenu> createTianshuPatternEncodingTermScreen(
      TianshuPatternEncodingTermMenu menu, Inventory inv, Component title
   ) {
      ScreenStyle style = StyleManager.loadStyleDoc("/screens/terminals/tianshu_terminal_entry.json");
      return new TianshuPatternEncodingTermScreen<>(menu, inv, title, style);
   }

   private static TianshuWirelessPatternEncodingTermScreen createTianshuWirelessPatternEncodingTermScreen(
      TianshuWirelessPatternEncodingTermMenu menu, Inventory inv, Component title
   ) {
      ScreenStyle style = StyleManager.loadStyleDoc("/screens/wireless_tianshu_terminal_entry.json");
      return new TianshuWirelessPatternEncodingTermScreen(menu, inv, title, style);
   }

   private static TianshuSeedStorageScreen createTianshuSeedStorageScreen(TianshuSeedStorageMenu menu, Inventory inv, Component title) {
      ScreenStyle style = StyleManager.loadStyleDoc("/screens/tianshu_seed_storage.json");
      return new TianshuSeedStorageScreen(menu, inv, title, style);
   }

   private static VoidCellScreen createVoidCellScreen(VoidCellMenu menu, Inventory inv, Component title) {
      ScreenStyle style = StyleManager.loadStyleDoc("/screens/void_cell.json");
      return new VoidCellScreen(menu, inv, title, style);
   }

   private static OverloadedPatternProviderScreen<OverloadedPatternProviderMenu> createOverloadedPatternProviderScreen(
      OverloadedPatternProviderMenu menu, Inventory inv, Component title
   ) {
      ScreenStyle style = StyleManager.loadStyleDoc("/screens/overloaded_pattern_provider.json");
      return new OverloadedPatternProviderScreen<>(menu, inv, title, style);
   }

   private static PigmeePatternProviderScreen createPigmeePatternProviderScreen(PigmeePatternProviderMenu menu, Inventory inv, Component title) {
      ScreenStyle style = StyleManager.loadStyleDoc("/screens/pigmee_pattern_provider.json");
      return new PigmeePatternProviderScreen(menu, inv, title, style);
   }

   private static PigmeeMolecularAssemblerScreen createPigmeeMolecularAssemblerScreen(PigmeeMolecularAssemblerMenu menu, Inventory inv, Component title) {
      ScreenStyle style = StyleManager.loadStyleDoc("/screens/pigmee_molecular_assembler.json");
      return new PigmeeMolecularAssemblerScreen(menu, inv, title, style);
   }

   private static OverloadedInterfaceScreen createOverloadedInterfaceScreen(OverloadedInterfaceMenu menu, Inventory inv, Component title) {
      ScreenStyle style = StyleManager.loadStyleDoc("/screens/overloaded_interface.json");
      return new OverloadedInterfaceScreen(menu, inv, title, style);
   }

   private static OverloadedPowerSupplyScreen createOverloadedPowerSupplyScreen(OverloadedPowerSupplyMenu menu, Inventory inv, Component title) {
      ScreenStyle style = StyleManager.loadStyleDoc("/screens/overloaded_power_supply.json");
      return new OverloadedPowerSupplyScreen(menu, inv, title, style);
   }

   private static LightningSimulationChamberScreen createLightningSimulationChamberScreen(LightningSimulationChamberMenu menu, Inventory inv, Component title) {
      ScreenStyle style = StyleManager.loadStyleDoc("/screens/lightning_simulation_room.json");
      return new LightningSimulationChamberScreen(menu, inv, title, style);
   }

   private static LightningAssemblyChamberScreen createLightningAssemblyChamberScreen(LightningAssemblyChamberMenu menu, Inventory inv, Component title) {
      ScreenStyle style = StyleManager.loadStyleDoc("/screens/lightning_assembly_chamber.json");
      return new LightningAssemblyChamberScreen(menu, inv, title, style);
   }

   private static LightningCollectorScreen createLightningCollectorScreen(LightningCollectorMenu menu, Inventory inv, Component title) {
      ScreenStyle style = StyleManager.loadStyleDoc("/screens/lightning_collector.json");
      return new LightningCollectorScreen(menu, inv, title, style);
   }

   private static OverloadProcessingFactoryScreen createOverloadProcessingFactoryScreen(OverloadProcessingFactoryMenu menu, Inventory inv, Component title) {
      ScreenStyle style = StyleManager.loadStyleDoc("/screens/overload_processing_factory.json");
      return new OverloadProcessingFactoryScreen(menu, inv, title, style);
   }

   private static TeslaCoilScreen createTeslaCoilScreen(TeslaCoilMenu menu, Inventory inv, Component title) {
      ScreenStyle style = StyleManager.loadStyleDoc("/screens/tesla_coil.json");
      return new TeslaCoilScreen(menu, inv, title, style);
   }

   private static AtmosphericIonizerScreen createAtmosphericIonizerScreen(AtmosphericIonizerMenu menu, Inventory inv, Component title) {
      ScreenStyle style = StyleManager.loadStyleDoc("/screens/atmospheric_ionizer.json");
      return new AtmosphericIonizerScreen(menu, inv, title, style);
   }

   private static CrystalCatalyzerScreen createCrystalCatalyzerScreen(CrystalCatalyzerMenu menu, Inventory inv, Component title) {
      ScreenStyle style = StyleManager.loadStyleDoc("/screens/crystal_catalyzer.json");
      return new CrystalCatalyzerScreen(menu, inv, title, style);
   }
}
