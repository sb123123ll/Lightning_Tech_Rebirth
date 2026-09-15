package com.moakiee.ae2lt.registry;

import com.moakiee.ae2lt.blockentity.AdvancedWirelessOverloadedControllerBlockEntity;
import com.moakiee.ae2lt.blockentity.AtmosphericIonizerBlockEntity;
import com.moakiee.ae2lt.blockentity.CrystalCatalyzerBlockEntity;
import com.moakiee.ae2lt.blockentity.ExtendedOverloadedPatternProviderBlockEntity;
import com.moakiee.ae2lt.blockentity.FirmamentConversionCoreBlockEntity;
import com.moakiee.ae2lt.blockentity.FumoBlockEntity;
import com.moakiee.ae2lt.blockentity.GhostOutputBlockEntity;
import com.moakiee.ae2lt.blockentity.LightningAssemblyChamberBlockEntity;
import com.moakiee.ae2lt.blockentity.LightningCollectorBlockEntity;
import com.moakiee.ae2lt.blockentity.LightningSimulationChamberBlockEntity;
import com.moakiee.ae2lt.blockentity.MatrixControllerBlockEntity;
import com.moakiee.ae2lt.blockentity.MatrixPatternStorageBlockEntity;
import com.moakiee.ae2lt.blockentity.MatrixPortBlockEntity;
import com.moakiee.ae2lt.blockentity.OverloadDeviceWorkbenchBlockEntity;
import com.moakiee.ae2lt.blockentity.OverloadProcessingFactoryBlockEntity;
import com.moakiee.ae2lt.blockentity.OverloadedControllerBlockEntity;
import com.moakiee.ae2lt.blockentity.OverloadedInterfaceBlockEntity;
import com.moakiee.ae2lt.blockentity.OverloadedPatternProviderBlockEntity;
import com.moakiee.ae2lt.blockentity.OverloadedPowerSupplyBlockEntity;
import com.moakiee.ae2lt.blockentity.PigmeeMentalmathUnitBlockEntity;
import com.moakiee.ae2lt.blockentity.PigmeeMolecularAssemblerBlockEntity;
import com.moakiee.ae2lt.blockentity.PigmeePatternProviderBlockEntity;
import com.moakiee.ae2lt.blockentity.TeslaCoilBlockEntity;
import com.moakiee.ae2lt.blockentity.TeslaCoilUpperBlockEntity;
import com.moakiee.ae2lt.blockentity.TianshuPatternStorageBlockEntity;
import com.moakiee.ae2lt.blockentity.TianshuSeedStorageBlockEntity;
import com.moakiee.ae2lt.blockentity.TianshuSupercomputerControllerBlockEntity;
import com.moakiee.ae2lt.blockentity.TianshuSupercomputerPortBlockEntity;
import com.moakiee.ae2lt.blockentity.WirelessOverloadedControllerBlockEntity;
import com.moakiee.ae2lt.blockentity.WirelessReceiverBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityType.Builder;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntities {
   public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.f_256922_, "ae2lt");
   public static final RegistryObject<BlockEntityType<LightningCollectorBlockEntity>> LIGHTNING_COLLECTOR = BLOCK_ENTITY_TYPES.register(
      "lightning_collector",
      () -> Builder.m_155273_(LightningCollectorBlockEntity::new, new Block[]{(Block)ModBlocks.LIGHTNING_COLLECTOR.get()}).m_58966_(null)
   );
   public static final RegistryObject<BlockEntityType<FirmamentConversionCoreBlockEntity>> FIRMAMENT_CONVERSION_CORE = BLOCK_ENTITY_TYPES.register(
      "firmament_conversion_core",
      () -> Builder.m_155273_(FirmamentConversionCoreBlockEntity::new, new Block[]{(Block)ModBlocks.FIRMAMENT_CONVERSION_CORE.get()}).m_58966_(null)
   );
   public static final RegistryObject<BlockEntityType<OverloadedControllerBlockEntity>> OVERLOADED_CONTROLLER = BLOCK_ENTITY_TYPES.register(
      "overloaded_controller",
      () -> Builder.m_155273_(OverloadedControllerBlockEntity::new, new Block[]{(Block)ModBlocks.OVERLOADED_CONTROLLER.get()}).m_58966_(null)
   );
   public static final RegistryObject<BlockEntityType<LightningSimulationChamberBlockEntity>> LIGHTNING_SIMULATION_CHAMBER = BLOCK_ENTITY_TYPES.register(
      "lightning_simulation_room",
      () -> Builder.m_155273_(LightningSimulationChamberBlockEntity::new, new Block[]{(Block)ModBlocks.LIGHTNING_SIMULATION_CHAMBER.get()}).m_58966_(null)
   );
   public static final RegistryObject<BlockEntityType<LightningAssemblyChamberBlockEntity>> LIGHTNING_ASSEMBLY_CHAMBER = BLOCK_ENTITY_TYPES.register(
      "lightning_assembly_chamber",
      () -> Builder.m_155273_(LightningAssemblyChamberBlockEntity::new, new Block[]{(Block)ModBlocks.LIGHTNING_ASSEMBLY_CHAMBER.get()}).m_58966_(null)
   );
   public static final RegistryObject<BlockEntityType<OverloadProcessingFactoryBlockEntity>> OVERLOAD_PROCESSING_FACTORY = BLOCK_ENTITY_TYPES.register(
      "overload_processing_factory",
      () -> Builder.m_155273_(OverloadProcessingFactoryBlockEntity::new, new Block[]{(Block)ModBlocks.OVERLOAD_PROCESSING_FACTORY.get()}).m_58966_(null)
   );
   public static final RegistryObject<BlockEntityType<TeslaCoilBlockEntity>> TESLA_COIL = BLOCK_ENTITY_TYPES.register(
      "tesla_coil", () -> Builder.m_155273_(TeslaCoilBlockEntity::new, new Block[]{(Block)ModBlocks.TESLA_COIL.get()}).m_58966_(null)
   );
   public static final RegistryObject<BlockEntityType<TeslaCoilUpperBlockEntity>> TESLA_COIL_UPPER = BLOCK_ENTITY_TYPES.register(
      "tesla_coil_upper", () -> Builder.m_155273_(TeslaCoilUpperBlockEntity::new, new Block[]{(Block)ModBlocks.TESLA_COIL.get()}).m_58966_(null)
   );
   public static final RegistryObject<BlockEntityType<AtmosphericIonizerBlockEntity>> ATMOSPHERIC_IONIZER = BLOCK_ENTITY_TYPES.register(
      "atmospheric_ionizer",
      () -> Builder.m_155273_(AtmosphericIonizerBlockEntity::new, new Block[]{(Block)ModBlocks.ATMOSPHERIC_IONIZER.get()}).m_58966_(null)
   );
   public static final RegistryObject<BlockEntityType<CrystalCatalyzerBlockEntity>> CRYSTAL_CATALYZER = BLOCK_ENTITY_TYPES.register(
      "crystal_catalyzer", () -> Builder.m_155273_(CrystalCatalyzerBlockEntity::new, new Block[]{(Block)ModBlocks.CRYSTAL_CATALYZER.get()}).m_58966_(null)
   );
   public static final RegistryObject<BlockEntityType<OverloadedPatternProviderBlockEntity>> OVERLOADED_PATTERN_PROVIDER = BLOCK_ENTITY_TYPES.register(
      "overloaded_pattern_provider",
      () -> Builder.m_155273_(OverloadedPatternProviderBlockEntity::new, new Block[]{(Block)ModBlocks.OVERLOADED_PATTERN_PROVIDER.get()}).m_58966_(null)
   );
   public static final RegistryObject<BlockEntityType<ExtendedOverloadedPatternProviderBlockEntity>> EXTENDED_OVERLOADED_PATTERN_PROVIDER = BLOCK_ENTITY_TYPES.register(
      "extended_overloaded_pattern_provider",
      () -> Builder.m_155273_(ExtendedOverloadedPatternProviderBlockEntity::new, new Block[]{(Block)ModBlocks.EXTENDED_OVERLOADED_PATTERN_PROVIDER.get()})
            .m_58966_(null)
   );
   public static final RegistryObject<BlockEntityType<OverloadedInterfaceBlockEntity>> OVERLOADED_INTERFACE = BLOCK_ENTITY_TYPES.register(
      "overloaded_interface",
      () -> Builder.m_155273_(OverloadedInterfaceBlockEntity::new, new Block[]{(Block)ModBlocks.OVERLOADED_INTERFACE.get()}).m_58966_(null)
   );
   public static final RegistryObject<BlockEntityType<OverloadedPowerSupplyBlockEntity>> OVERLOADED_POWER_SUPPLY = ModBlocks.hasOverloadedPowerSupply()
      ? BLOCK_ENTITY_TYPES.register(
         "overloaded_power_supply",
         () -> Builder.m_155273_(OverloadedPowerSupplyBlockEntity::new, new Block[]{(Block)ModBlocks.OVERLOADED_POWER_SUPPLY.get()}).m_58966_(null)
      )
      : null;
   public static final RegistryObject<BlockEntityType<WirelessReceiverBlockEntity>> WIRELESS_RECEIVER = BLOCK_ENTITY_TYPES.register(
      "wireless_receiver", () -> Builder.m_155273_(WirelessReceiverBlockEntity::new, new Block[]{(Block)ModBlocks.WIRELESS_RECEIVER.get()}).m_58966_(null)
   );
   public static final RegistryObject<BlockEntityType<WirelessOverloadedControllerBlockEntity>> WIRELESS_OVERLOADED_CONTROLLER = BLOCK_ENTITY_TYPES.register(
      "wireless_overloaded_controller",
      () -> Builder.m_155273_(WirelessOverloadedControllerBlockEntity::new, new Block[]{(Block)ModBlocks.WIRELESS_OVERLOADED_CONTROLLER.get()}).m_58966_(null)
   );
   public static final RegistryObject<BlockEntityType<AdvancedWirelessOverloadedControllerBlockEntity>> ADVANCED_WIRELESS_OVERLOADED_CONTROLLER = BLOCK_ENTITY_TYPES.register(
      "advanced_wireless_overloaded_controller",
      () -> Builder.m_155273_(AdvancedWirelessOverloadedControllerBlockEntity::new, new Block[]{(Block)ModBlocks.ADVANCED_WIRELESS_OVERLOADED_CONTROLLER.get()})
            .m_58966_(null)
   );
   public static final RegistryObject<BlockEntityType<PigmeeMentalmathUnitBlockEntity>> PIGMEE_MENTALMATH_UNIT = BLOCK_ENTITY_TYPES.register(
      "pigmee_mentalmath_unit",
      () -> Builder.m_155273_(PigmeeMentalmathUnitBlockEntity::new, new Block[]{(Block)ModBlocks.PIGMEE_MENTALMATH_UNIT.get()}).m_58966_(null)
   );
   public static final RegistryObject<BlockEntityType<PigmeePatternProviderBlockEntity>> PIGMEE_PATTERN_PROVIDER = BLOCK_ENTITY_TYPES.register(
      "pigmee_pattern_provider",
      () -> Builder.m_155273_(PigmeePatternProviderBlockEntity::new, new Block[]{(Block)ModBlocks.PIGMEE_PATTERN_PROVIDER.get()}).m_58966_(null)
   );
   public static final RegistryObject<BlockEntityType<PigmeeMolecularAssemblerBlockEntity>> PIGMEE_MOLECULAR_ASSEMBLER = BLOCK_ENTITY_TYPES.register(
      "pigmee_molecular_assembler",
      () -> Builder.m_155273_(PigmeeMolecularAssemblerBlockEntity::new, new Block[]{(Block)ModBlocks.PIGMEE_MOLECULAR_ASSEMBLER.get()}).m_58966_(null)
   );
   public static final RegistryObject<BlockEntityType<TianshuSupercomputerControllerBlockEntity>> TIANSHU_SUPERCOMPUTER_CONTROLLER = BLOCK_ENTITY_TYPES.register(
      "tianshu_supercomputer_controller",
      () -> Builder.m_155273_(TianshuSupercomputerControllerBlockEntity::new, new Block[]{(Block)ModBlocks.TIANSHU_SUPERCOMPUTER_CONTROLLER.get()})
            .m_58966_(null)
   );
   public static final RegistryObject<BlockEntityType<TianshuSupercomputerPortBlockEntity>> TIANSHU_SUPERCOMPUTER_PORT = BLOCK_ENTITY_TYPES.register(
      "tianshu_supercomputer_port",
      () -> Builder.m_155273_(TianshuSupercomputerPortBlockEntity::new, new Block[]{(Block)ModBlocks.TIANSHU_SUPERCOMPUTER_PORT.get()}).m_58966_(null)
   );
   public static final RegistryObject<BlockEntityType<TianshuSeedStorageBlockEntity>> TIANSHU_SEED_STORAGE = BLOCK_ENTITY_TYPES.register(
      "closed_loop_seed_storage",
      () -> Builder.m_155273_(TianshuSeedStorageBlockEntity::new, new Block[]{(Block)ModBlocks.CLOSED_LOOP_SEED_STORAGE.get()}).m_58966_(null)
   );
   public static final RegistryObject<BlockEntityType<TianshuPatternStorageBlockEntity>> TIANSHU_PATTERN_STORAGE = BLOCK_ENTITY_TYPES.register(
      "closed_loop_pattern_storage",
      () -> Builder.m_155273_(TianshuPatternStorageBlockEntity::new, new Block[]{(Block)ModBlocks.CLOSED_LOOP_PATTERN_STORAGE.get()}).m_58966_(null)
   );
   public static final RegistryObject<BlockEntityType<MatrixControllerBlockEntity>> MATRIX_CONTROLLER = BLOCK_ENTITY_TYPES.register(
      "matter_warping_matrix_controller",
      () -> Builder.m_155273_(MatrixControllerBlockEntity::new, new Block[]{(Block)ModBlocks.MATTER_WARPING_MATRIX_CONTROLLER.get()}).m_58966_(null)
   );
   public static final RegistryObject<BlockEntityType<MatrixPortBlockEntity>> MATRIX_PORT = BLOCK_ENTITY_TYPES.register(
      "matter_warping_matrix_port",
      () -> Builder.m_155273_(MatrixPortBlockEntity::new, new Block[]{(Block)ModBlocks.MATTER_WARPING_MATRIX_PORT.get()}).m_58966_(null)
   );
   public static final RegistryObject<BlockEntityType<MatrixPatternStorageBlockEntity>> MATRIX_PATTERN_STORAGE = BLOCK_ENTITY_TYPES.register(
      "matter_warping_matrix_pattern_storage",
      () -> Builder.m_155273_(
               MatrixPatternStorageBlockEntity::new,
               new Block[]{(Block)ModBlocks.MATTER_WARPING_MATRIX_PATTERN_STORAGE_T1.get(), (Block)ModBlocks.MATTER_WARPING_MATRIX_PATTERN_STORAGE_T2.get()}
            )
            .m_58966_(null)
   );
   public static final RegistryObject<BlockEntityType<GhostOutputBlockEntity>> GHOST_OUTPUT = BLOCK_ENTITY_TYPES.register(
      "ghost_output", () -> Builder.m_155273_((pos, state) -> new GhostOutputBlockEntity(pos), new Block[]{Blocks.f_50016_}).m_58966_(null)
   );
   public static final RegistryObject<BlockEntityType<FumoBlockEntity>> FUMO = BLOCK_ENTITY_TYPES.register(
      "fumo",
      () -> Builder.m_155273_(
               FumoBlockEntity::new,
               new Block[]{
                  (Block)ModFumos.MOAKIEE_FUMO.get(),
                  (Block)ModFumos.CYSTRYSU_FUMO.get(),
                  (Block)ModFumos.PIGMEE_FUMO.get(),
                  (Block)ModFumos.CREATIVE_PIGMEE_FUMO.get(),
                  (Block)ModFumos.HYPERDIMENSIONAL_PIGMEE_FUMO.get()
               }
            )
            .m_58966_(null)
   );
   public static final RegistryObject<BlockEntityType<OverloadDeviceWorkbenchBlockEntity>> OVERLOAD_DEVICE_WORKBENCH = BLOCK_ENTITY_TYPES.register(
      "overload_device_workbench",
      () -> Builder.m_155273_(OverloadDeviceWorkbenchBlockEntity::new, new Block[]{(Block)ModBlocks.OVERLOAD_DEVICE_WORKBENCH.get()}).m_58966_(null)
   );

   private ModBlockEntities() {
   }
}
