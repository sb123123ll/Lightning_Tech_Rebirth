package com.moakiee.ae2lt;

import appeng.api.behaviors.GenericInternalInventory;
import appeng.api.config.Actionable;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.features.GridLinkables;
import appeng.api.implementations.blockentities.ICraftingMachine;
import appeng.api.implementations.items.IAEItemPowerStorage;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.storage.StorageCells;
import appeng.api.upgrades.Upgrades;
import appeng.block.AEBaseEntityBlock;
import appeng.blockentity.AEBaseBlockEntity;
import appeng.capabilities.Capabilities;
import appeng.core.definitions.AEItems;
import appeng.items.tools.powered.WirelessTerminalItem;
import com.moakiee.ae2lt.api.AE2LTCapabilities;
import com.moakiee.ae2lt.api.frequency.FrequencyApi;
import com.moakiee.ae2lt.api.lightning.ILightningEnergyHandler;
import com.moakiee.ae2lt.api.patternprovider.WirelessPatternProviderPolicy;
import com.moakiee.ae2lt.block.AtmosphericIonizerBlock;
import com.moakiee.ae2lt.block.CrystalCatalyzerBlock;
import com.moakiee.ae2lt.block.LightningAssemblyChamberBlock;
import com.moakiee.ae2lt.block.LightningCollectorBlock;
import com.moakiee.ae2lt.block.LightningSimulationChamberBlock;
import com.moakiee.ae2lt.block.MatrixPortBlock;
import com.moakiee.ae2lt.block.OverloadDeviceWorkbenchBlock;
import com.moakiee.ae2lt.block.OverloadProcessingFactoryBlock;
import com.moakiee.ae2lt.block.OverloadTntBlock;
import com.moakiee.ae2lt.block.OverloadedControllerBlock;
import com.moakiee.ae2lt.block.OverloadedInterfaceBlock;
import com.moakiee.ae2lt.block.OverloadedPatternProviderBlock;
import com.moakiee.ae2lt.block.OverloadedPowerSupplyBlock;
import com.moakiee.ae2lt.block.PigmeeMentalmathUnitBlock;
import com.moakiee.ae2lt.block.PigmeeMolecularAssemblerBlock;
import com.moakiee.ae2lt.block.PigmeePatternProviderBlock;
import com.moakiee.ae2lt.block.TeslaCoilBlock;
import com.moakiee.ae2lt.block.TianshuSeedStorageBlock;
import com.moakiee.ae2lt.block.TianshuSupercomputerPortBlock;
import com.moakiee.ae2lt.block.WirelessReceiverBlock;
import com.moakiee.ae2lt.blockentity.AdvancedWirelessOverloadedControllerBlockEntity;
import com.moakiee.ae2lt.blockentity.AtmosphericIonizerBlockEntity;
import com.moakiee.ae2lt.blockentity.CrystalCatalyzerBlockEntity;
import com.moakiee.ae2lt.blockentity.ExtendedOverloadedPatternProviderBlockEntity;
import com.moakiee.ae2lt.blockentity.FirmamentConversionCoreBlockEntity;
import com.moakiee.ae2lt.blockentity.LightningAssemblyChamberBlockEntity;
import com.moakiee.ae2lt.blockentity.LightningCollectorBlockEntity;
import com.moakiee.ae2lt.blockentity.LightningSimulationChamberBlockEntity;
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
import com.moakiee.ae2lt.blockentity.TianshuSupercomputerPortBlockEntity;
import com.moakiee.ae2lt.blockentity.WirelessOverloadedControllerBlockEntity;
import com.moakiee.ae2lt.blockentity.WirelessReceiverBlockEntity;
import com.moakiee.ae2lt.celestweave.ArmorEnergyBuffer;
import com.moakiee.ae2lt.config.AE2LTClientConfig;
import com.moakiee.ae2lt.config.AE2LTCommonConfig;
import com.moakiee.ae2lt.config.AE2LTConfigMigration;
import com.moakiee.ae2lt.crafting.matrix.core.CraftingCoreRegistry;
import com.moakiee.ae2lt.entity.OverloadTntEntity;
import com.moakiee.ae2lt.grid.WirelessFrequencyManager;
import com.moakiee.ae2lt.grid.api.FrequencyApiBridge;
import com.moakiee.ae2lt.grid.wirelesslink.WirelessLinkRegistry;
import com.moakiee.ae2lt.integration.ae2wtlib.Ae2wtlibIntegration;
import com.moakiee.ae2lt.integration.mekanism.MekanismArmorIntegration;
import com.moakiee.ae2lt.item.FixedInfiniteCellItem;
import com.moakiee.ae2lt.item.FumoBlockItem;
import com.moakiee.ae2lt.item.OverloadCrystalItem;
import com.moakiee.ae2lt.logic.InsertOnlyReturnInvWrapper;
import com.moakiee.ae2lt.logic.MachineAdapterRegistry;
import com.moakiee.ae2lt.logic.OverloadedPatternProviderLogic;
import com.moakiee.ae2lt.logic.UnlimitedReturnInventory;
import com.moakiee.ae2lt.logic.craft.BatchPatternEligibility;
import com.moakiee.ae2lt.logic.railgun.RailgunEnergyBuffer;
import com.moakiee.ae2lt.logic.tianshu.loop.ClosedLoopPatternDecoder;
import com.moakiee.ae2lt.me.GridLightningEnergyHandler;
import com.moakiee.ae2lt.me.cell.BulkLightningCellHandler;
import com.moakiee.ae2lt.me.cell.FixedInfiniteCellHandler;
import com.moakiee.ae2lt.me.cell.VoidCellHandler;
import com.moakiee.ae2lt.network.NetworkInit;
import com.moakiee.ae2lt.overload.pattern.OverloadPatternDecoder;
import com.moakiee.ae2lt.recipe.RecipeConflictScanner;
import com.moakiee.ae2lt.registry.LegacyRegistryAliases;
import com.moakiee.ae2lt.registry.ModAEKeyTypes;
import com.moakiee.ae2lt.registry.ModBlockEntities;
import com.moakiee.ae2lt.registry.ModBlocks;
import com.moakiee.ae2lt.registry.ModDamageTypes;
import com.moakiee.ae2lt.registry.ModEntities;
import com.moakiee.ae2lt.registry.ModFumos;
import com.moakiee.ae2lt.registry.ModItems;
import com.moakiee.ae2lt.registry.ModLootModifiers;
import com.moakiee.ae2lt.registry.ModMenuTypes;
import com.moakiee.ae2lt.registry.ModMobEffects;
import com.moakiee.ae2lt.registry.ModRecipeTypes;
import com.moakiee.ae2lt.registry.ModSounds;
import com.moakiee.ae2lt.registry.ModStructureTypes;
import com.moakiee.thunderbolt.CoreConfig;
import com.moakiee.thunderbolt.api.channel.ChannelSourceRegistry;
import com.moakiee.thunderbolt.core.crafting.batch.BatchExecutor;
import com.mojang.logging.LogUtils;
import java.util.EnumMap;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockSource;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.CreativeModeTab.Output;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.common.util.NonNullSupplier;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig.Type;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

@Mod("ae2lt")
public class AE2LightningTech {
   public static final String MODID = "ae2lt";
   private static final Logger LOG = LogUtils.getLogger();
   private static final CraftingCoreRegistry CRAFTING_CORE_REGISTRY = new CraftingCoreRegistry();
   private static final ResourceLocation BLOCK_ENTITY_CAP_PROVIDER_ID = new ResourceLocation("ae2lt", "block_entity_cap_provider");
   public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.f_279569_, "ae2lt");
   public static final RegistryObject<CreativeModeTab> MAIN_TAB = CREATIVE_MODE_TABS.register(
      "main",
      () -> CreativeModeTab.builder()
            .m_257941_(Component.m_237115_("itemGroup.ae2lt"))
            .withTabsBefore(new ResourceKey[]{CreativeModeTabs.f_256731_})
            .m_257737_(() -> ((OverloadCrystalItem)ModItems.OVERLOAD_CRYSTAL.get()).m_7968_())
            .m_257501_((parameters, output) -> {
               if (ModBlocks.hasSiliconBlock()) {
                  acceptCreative(output, ModBlocks.SILICON_BLOCK);
               }

               acceptCreative(output, ModBlocks.OVERLOAD_CRYSTAL_BLOCK);
               acceptCreative(output, ModBlocks.OVERLOAD_MACHINE_FRAME);
               acceptCreative(output, ModBlocks.FIRMAMENT_CONVERSION_CORE);
               acceptCreative(output, ModBlocks.OVERLOAD_TNT);
               acceptCreative(output, ModBlocks.FLAWLESS_BUDDING_OVERLOAD_CRYSTAL);
               acceptCreative(output, ModBlocks.FLAWED_BUDDING_OVERLOAD_CRYSTAL);
               acceptCreative(output, ModBlocks.CRACKED_BUDDING_OVERLOAD_CRYSTAL);
               acceptCreative(output, ModBlocks.DAMAGED_BUDDING_OVERLOAD_CRYSTAL);
               acceptCreative(output, ModBlocks.SMALL_OVERLOAD_CRYSTAL_BUD);
               acceptCreative(output, ModBlocks.MEDIUM_OVERLOAD_CRYSTAL_BUD);
               acceptCreative(output, ModBlocks.LARGE_OVERLOAD_CRYSTAL_BUD);
               acceptCreative(output, ModBlocks.OVERLOAD_CRYSTAL_CLUSTER);
               acceptCreative(output, ModBlocks.LIGHTNING_COLLECTOR);
               acceptCreative(output, ModBlocks.TESLA_COIL);
               acceptCreative(output, ModBlocks.ATMOSPHERIC_IONIZER);
               acceptCreative(output, ModBlocks.CRYSTAL_CATALYZER);
               acceptCreative(output, ModBlocks.LIGHTNING_SIMULATION_CHAMBER);
               acceptCreative(output, ModBlocks.LIGHTNING_ASSEMBLY_CHAMBER);
               acceptCreative(output, ModBlocks.OVERLOAD_PROCESSING_FACTORY);
               acceptCreative(output, ModBlocks.OVERLOADED_CONTROLLER);
               acceptCreative(output, ModBlocks.OVERLOADED_PATTERN_PROVIDER);
               acceptCreative(output, ModBlocks.EXTENDED_OVERLOADED_PATTERN_PROVIDER);
               acceptCreative(output, ModBlocks.OVERLOADED_INTERFACE);
               if (ModBlocks.hasOverloadedPowerSupply()) {
                  acceptCreative(output, ModBlocks.OVERLOADED_POWER_SUPPLY);
               }

               acceptCreative(output, ModBlocks.WIRELESS_RECEIVER);
               acceptCreative(output, ModBlocks.WIRELESS_OVERLOADED_CONTROLLER);
               acceptCreative(output, ModBlocks.ADVANCED_WIRELESS_OVERLOADED_CONTROLLER);
               acceptCreative(output, ModItems.OVERLOADED_CABLE);
               acceptCreative(output, ModItems.OVERLOADED_CABLE_WHITE);
               acceptCreative(output, ModItems.OVERLOADED_CABLE_ORANGE);
               acceptCreative(output, ModItems.OVERLOADED_CABLE_MAGENTA);
               acceptCreative(output, ModItems.OVERLOADED_CABLE_LIGHT_BLUE);
               acceptCreative(output, ModItems.OVERLOADED_CABLE_YELLOW);
               acceptCreative(output, ModItems.OVERLOADED_CABLE_LIME);
               acceptCreative(output, ModItems.OVERLOADED_CABLE_PINK);
               acceptCreative(output, ModItems.OVERLOADED_CABLE_GRAY);
               acceptCreative(output, ModItems.OVERLOADED_CABLE_LIGHT_GRAY);
               acceptCreative(output, ModItems.OVERLOADED_CABLE_CYAN);
               acceptCreative(output, ModItems.OVERLOADED_CABLE_PURPLE);
               acceptCreative(output, ModItems.OVERLOADED_CABLE_BLUE);
               acceptCreative(output, ModItems.OVERLOADED_CABLE_BROWN);
               acceptCreative(output, ModItems.OVERLOADED_CABLE_GREEN);
               acceptCreative(output, ModItems.OVERLOADED_CABLE_RED);
               acceptCreative(output, ModItems.OVERLOADED_CABLE_BLACK);
               acceptCreative(output, ModItems.LIGHTNING_ITEM_CELL_HOUSING);
               acceptCreative(output, ModItems.LIGHTNING_STORAGE_COMPONENT_I);
               acceptCreative(output, ModItems.LIGHTNING_STORAGE_COMPONENT_II);
               acceptCreative(output, ModItems.LIGHTNING_STORAGE_COMPONENT_III);
               acceptCreative(output, ModItems.LIGHTNING_STORAGE_COMPONENT_IV);
               acceptCreative(output, ModItems.LIGHTNING_STORAGE_COMPONENT_V);
               acceptCreative(output, ModItems.BULK_LIGHTNING_STORAGE_COMPONENT);
               acceptCreative(output, ModItems.LIGHTNING_CELL_COMPONENT_I);
               acceptCreative(output, ModItems.LIGHTNING_CELL_COMPONENT_II);
               acceptCreative(output, ModItems.LIGHTNING_CELL_COMPONENT_III);
               acceptCreative(output, ModItems.LIGHTNING_CELL_COMPONENT_IV);
               acceptCreative(output, ModItems.LIGHTNING_CELL_COMPONENT_V);
               acceptCreative(output, ModItems.BULK_LIGHTNING_CELL_COMPONENT);
               acceptCreative(output, ModItems.INFINITE_STORAGE_CELL);
               acceptCreative(output, ModItems.VOID_CELL);
               output.m_246342_(FixedInfiniteCellItem.createDisplayedResultStack(FixedInfiniteCellItem.CellOutcome.HIGH_VOLTAGE));
               output.m_246342_(FixedInfiniteCellItem.createDisplayedResultStack(FixedInfiniteCellItem.CellOutcome.EXTREME_HIGH_VOLTAGE));
               acceptCreative(output, ModBlocks.TIANSHU_SUPERCOMPUTER_CASING);
               acceptCreative(output, ModBlocks.PHASE_CHANGE_COOLING_UNIT);
               acceptCreative(output, ModBlocks.TIANSHU_SUPERCOMPUTER_GLASS);
               acceptCreative(output, ModBlocks.TIANSHU_SUPERCOMPUTER_CONTROLLER);
               acceptCreative(output, ModBlocks.TIANSHU_SUPERCOMPUTER_PORT);
               acceptCreative(output, ModBlocks.BASELINE_SUPERCOMPUTING_UNIT);
               acceptCreative(output, ModBlocks.QUANTUM_SUPERCOMPUTING_UNIT);
               acceptCreative(output, ModBlocks.OVERLOAD_SUPERCOMPUTING_UNIT);
               acceptCreative(output, ModBlocks.MULTIDIMENSIONAL_SUPERCOMPUTING_UNIT);
               acceptCreative(output, ModBlocks.TIANSHU_BLANK_UNIT);
               acceptCreative(output, ModBlocks.STORAGE_SUPERCOMPUTING_UNIT);
               acceptCreative(output, ModBlocks.PARALLEL_SUPERCOMPUTING_UNIT);
               acceptCreative(output, ModBlocks.TIANSHU_AMPLIFIER_UNIT);
               acceptCreative(output, ModBlocks.CLOSED_LOOP_PATTERN_STORAGE);
               acceptCreative(output, ModBlocks.CLOSED_LOOP_SEED_STORAGE);
               acceptCreative(output, ModItems.TIANSHU_PATTERN_ENCODING_TERMINAL);
               ModItems.TIANSHU_WIRELESS_PATTERN_ENCODING_TERMINAL.ifPresent(output::m_246326_);
               acceptCreative(output, ModBlocks.MATTER_WARPING_MATRIX_CASING);
               acceptCreative(output, ModBlocks.MATTER_WARPING_MATRIX_CONSTRAINT_FRAME);
               acceptCreative(output, ModBlocks.MATTER_WARPING_MATRIX_GLASS);
               acceptCreative(output, ModBlocks.MATTER_WARPING_MATRIX_CONTROLLER);
               acceptCreative(output, ModBlocks.MATTER_WARPING_MATRIX_PORT);
               acceptCreative(output, ModBlocks.MATTER_WARPING_MATRIX_STABLE_MAIN_CORE);
               acceptCreative(output, ModBlocks.MATTER_WARPING_MATRIX_QUANTUM_MAIN_CORE);
               acceptCreative(output, ModBlocks.MATTER_WARPING_MATRIX_OVERLOAD_MAIN_CORE);
               acceptCreative(output, ModBlocks.MATTER_WARPING_MATRIX_MULTIDIMENSIONAL_MAIN_CORE);
               acceptCreative(output, ModBlocks.MATTER_WARPING_MATRIX_THREAD_UNIT_T1);
               acceptCreative(output, ModBlocks.MATTER_WARPING_MATRIX_THREAD_UNIT_T2);
               acceptCreative(output, ModBlocks.MATTER_WARPING_MATRIX_THERMAL_CONTROL_UNIT_T1);
               acceptCreative(output, ModBlocks.MATTER_WARPING_MATRIX_THERMAL_CONTROL_UNIT_T2);
               acceptCreative(output, ModBlocks.MATTER_WARPING_MATRIX_PATTERN_STORAGE_T1);
               acceptCreative(output, ModBlocks.MATTER_WARPING_MATRIX_PATTERN_STORAGE_T2);
               acceptCreative(output, ModItems.MATTER_WARPING_MATRIX_PATTERN_STORAGE_UPGRADE);
               acceptCreative(output, ModItems.OVERLOAD_CRYSTAL);
               acceptCreative(output, ModItems.OVERLOAD_CRYSTAL_DUST);
               acceptCreative(output, ModItems.ELECTRO_CHIME_CRYSTAL);
               acceptCreative(output, ModItems.PERFECT_ELECTRO_CHIME_CRYSTAL);
               acceptCreative(output, ModItems.CLEAR_CONDENSATE);
               acceptCreative(output, ModItems.RAIN_CONDENSATE);
               acceptCreative(output, ModItems.THUNDERSTORM_CONDENSATE);
               acceptCreative(output, ModItems.FIRMAMENT_DUST);
               acceptCreative(output, ModItems.FIRMAMENT_MIXTURE);
               acceptCreative(output, ModItems.FIRMAMENT_ALLOY_INGOT);
               acceptCreative(output, ModItems.FIRMAMENT_ESSENCE);
               acceptCreative(output, ModItems.INACTIVE_FIRMAMENT_SPIRIT_CORE);
               acceptCreative(output, ModItems.FIRMAMENT_SPIRIT_CORE_OCULUS);
               acceptCreative(output, ModItems.FIRMAMENT_SPIRIT_CORE_CORE);
               acceptCreative(output, ModItems.FIRMAMENT_SPIRIT_CORE_CONDUIT);
               acceptCreative(output, ModItems.FIRMAMENT_SPIRIT_CORE_STRIDE);
               acceptCreative(output, ModItems.FIRMAMENT_SUPERCONDUCTING_WIRE);
               acceptCreative(output, ModItems.OVERLOAD_ALLOY_BLANK);
               acceptCreative(output, ModItems.OVERLOAD_ALLOY);
               acceptCreative(output, ModItems.OVERLOAD_ALLOY_PLATE);
               acceptCreative(output, ModItems.OVERLOAD_INSCRIBER_PRESS);
               acceptCreative(output, ModItems.UNOVERLOADED_CIRCUIT_BOARD);
               acceptCreative(output, ModItems.OVERLOAD_CIRCUIT_BOARD);
               acceptCreative(output, ModItems.OVERLOAD_PROCESSOR);
               acceptCreative(output, ModItems.OVERLOAD_SINGULARITY);
               acceptCreative(output, ModItems.ULTIMATE_OVERLOAD_CORE);
               acceptCreative(output, ModItems.BASIC_TOPOLOGICAL_LATTICE);
               acceptCreative(output, ModItems.DENSE_TOPOLOGICAL_LATTICE);
               acceptCreative(output, ModItems.ENTANGLED_TOPOLOGICAL_LATTICE);
               acceptCreative(output, ModItems.HYPERDIMENSIONAL_TOPOLOGICAL_LATTICE);
               acceptCreative(output, ModItems.LIGHTNING_COLLAPSE_MATRIX);
               acceptCreative(output, ModItems.FLOATING_MATTER);
               acceptCreative(output, ModItems.OVERLOAD_PATTERN);
               acceptCreative(output, ModItems.CLOSED_LOOP_PATTERN);
               acceptCreative(output, ModItems.OVERLOAD_PATTERN_ENCODER);
               acceptCreative(output, ModItems.OVERLOADED_WIRELESS_CONNECT_TOOL);
               acceptCreative(output, ModItems.OVERLOADED_FREQUENCY_CARD);
               acceptCreative(output, ModItems.OVERLOADED_PATTERN_PROVIDER_UPGRADE);
               acceptCreative(output, ModItems.EXTENDED_OVERLOADED_PATTERN_PROVIDER_UPGRADE);
               acceptCreative(output, ModItems.OVERLOADED_FILTER_COMPONENT);
               acceptCreative(output, ModBlocks.OVERLOAD_DEVICE_WORKBENCH);
               acceptCreative(output, ModItems.OVERLOAD_MODULE_BASE);
               acceptCreative(output, ModItems.CELESTWEAVE_OCULUS);
               acceptCreative(output, ModItems.CELESTWEAVE_CORE);
               acceptCreative(output, ModItems.CELESTWEAVE_CONDUIT);
               acceptCreative(output, ModItems.CELESTWEAVE_STRIDE);
               acceptCreative(output, ModItems.ENERGY_MODULE_T1);
               acceptCreative(output, ModItems.ENERGY_MODULE_T2);
               acceptCreative(output, ModItems.ENERGY_MODULE_T3);
               acceptCreative(output, ModItems.CELESTWEAVE_SUBMODULE_NIGHT_VISION);
               acceptCreative(output, ModItems.CELESTWEAVE_SUBMODULE_WATER_BREATHING);
               acceptCreative(output, ModItems.CELESTWEAVE_SUBMODULE_SATURATION);
               acceptCreative(output, ModItems.CELESTWEAVE_SUBMODULE_REACH_EXTENSION);
               acceptCreative(output, ModItems.CELESTWEAVE_SUBMODULE_MATRIX_SHIELD);
               acceptCreative(output, ModItems.CELESTWEAVE_SUBMODULE_PHASE_SHIELD);
               acceptCreative(output, ModItems.CELESTWEAVE_SUBMODULE_REFLECT);
               acceptCreative(output, ModItems.CELESTWEAVE_SUBMODULE_UNDYING);
               acceptCreative(output, ModItems.CELESTWEAVE_SUBMODULE_MULTIDIMENSIONAL_PROTECTION);
               acceptCreative(output, ModItems.CELESTWEAVE_SUBMODULE_PURIFICATION);
               acceptCreative(output, ModItems.CELESTWEAVE_SUBMODULE_RADIATION_PROTECTION);
               acceptCreative(output, ModItems.CELESTWEAVE_SUBMODULE_LASER_PROTECTION);
               acceptCreative(output, ModItems.CELESTWEAVE_SUBMODULE_PHASE_LOCK);
               acceptCreative(output, ModItems.CELESTWEAVE_SUBMODULE_FLIGHT);
               acceptCreative(output, ModItems.CELESTWEAVE_SUBMODULE_PHASE_FLIGHT);
               acceptCreative(output, ModItems.CELESTWEAVE_SUBMODULE_DASH);
               acceptCreative(output, ModItems.CELESTWEAVE_SUBMODULE_DIG_AFFINITY);
               acceptCreative(output, ModItems.CELESTWEAVE_SUBMODULE_MOVEMENT_ASSIST);
               acceptCreative(output, ModItems.ELECTROMAGNETIC_RAILGUN);
               acceptCreative(output, ModItems.RAILGUN_MODULE_CORE);
               acceptCreative(output, ModItems.RAILGUN_MODULE_COMPUTE);
               acceptCreative(output, ModItems.RAILGUN_MODULE_ACCELERATION);
               acceptCreative(output, ModItems.RAILGUN_MODULE_RANGE);
               acceptCreative(output, ModItems.RAILGUN_MODULE_OVERLOAD_EXECUTION);
               acceptCreative(output, ModItems.RAILGUN_MODULE_MULTIDIMENSIONAL_EXECUTION);
               output.m_246326_((ItemLike)ModFumos.MOAKIEE_FUMO_ITEM.get());
               output.m_246326_((ItemLike)ModFumos.CYSTRYSU_FUMO_ITEM.get());
            })
            .m_257652_()
   );
   public static final RegistryObject<CreativeModeTab> PIGMEE_TAB = CREATIVE_MODE_TABS.register(
      "pigmee",
      () -> CreativeModeTab.builder()
            .m_257941_(Component.m_237115_("itemGroup.ae2lt.pigmee"))
            .withTabsAfter(new ResourceKey[]{MAIN_TAB.getKey()})
            .m_257737_(() -> ((FumoBlockItem)ModFumos.PIGMEE_FUMO_ITEM.get()).m_7968_())
            .m_257501_((parameters, output) -> {
               output.m_246326_((ItemLike)ModFumos.PIGMEE_FUMO_ITEM.get());
               output.m_246326_((ItemLike)ModFumos.CREATIVE_PIGMEE_FUMO_ITEM.get());
               acceptCreative(output, ModBlocks.PIGMEE_MENTALMATH_UNIT);
               acceptCreative(output, ModBlocks.PIGMEE_PATTERN_PROVIDER);
               acceptCreative(output, ModBlocks.PIGMEE_MOLECULAR_ASSEMBLER);
               acceptCreative(output, ModItems.PIGMEE_CORE);
               acceptCreative(output, ModItems.PIGMEE_ITEM_CELL_HOUSING);
               acceptCreative(output, ModItems.PIGMEE_STORAGE_COMPONENT);
               acceptCreative(output, ModItems.PIGMEE_STORAGE_CELL);
            })
            .m_257652_()
   );
   private static final IItemHandlerModifiable WORKBENCH_REJECTING_ITEM_HANDLER = new IItemHandlerModifiable() {
      public int getSlots() {
         return 1;
      }

      public ItemStack getStackInSlot(int slot) {
         return ItemStack.f_41583_;
      }

      public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
         return stack;
      }

      public ItemStack extractItem(int slot, int amount, boolean simulate) {
         return ItemStack.f_41583_;
      }

      public int getSlotLimit(int slot) {
         return 0;
      }

      public boolean isItemValid(int slot, ItemStack stack) {
         return false;
      }

      public void setStackInSlot(int slot, ItemStack stack) {
      }
   };

   public static CraftingCoreRegistry craftingCoreRegistry() {
      return CRAFTING_CORE_REGISTRY;
   }

   public AE2LightningTech() {
      IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
      AE2LTConfigMigration.runIfNeeded();
      WirelessPatternProviderPolicy.setMaxDistanceSupplier(AE2LTCommonConfig::wirelessConnectorMaxDistance);
      ModFumos.register();
      LegacyRegistryAliases.register();
      ModBlocks.BLOCKS.register(modEventBus);
      ModBlockEntities.BLOCK_ENTITY_TYPES.register(modEventBus);
      ModEntities.ENTITY_TYPES.register(modEventBus);
      ModItems.ITEMS.register(modEventBus);
      ModMenuTypes.MENU_TYPES.register(modEventBus);
      ModRecipeTypes.RECIPE_SERIALIZERS.register(modEventBus);
      ModRecipeTypes.RECIPE_TYPES.register(modEventBus);
      ModMobEffects.EFFECTS.register(modEventBus);
      ModSounds.SOUND_EVENTS.register(modEventBus);
      ModStructureTypes.STRUCTURE_TYPES.register(modEventBus);
      ModStructureTypes.STRUCTURE_PIECES.register(modEventBus);
      ModLootModifiers.LOOT_MODIFIER_SERIALIZERS.register(modEventBus);
      CREATIVE_MODE_TABS.register(modEventBus);
      NetworkInit.register();
      if (FMLLoader.getLoadingModList().getModFileById("ae2wtlib") != null) {
         modEventBus.addListener(Ae2wtlibIntegration::onRegister);
      }

      modEventBus.addListener(ModAEKeyTypes::register);
      modEventBus.addListener(this::registerCapabilities);
      modEventBus.addListener(this::commonSetup);
      modEventBus.addListener(this::onConfigChanged);
      ModLoadingContext.get().registerConfig(Type.COMMON, AE2LTCommonConfig.SPEC);
      ModLoadingContext.get().registerConfig(Type.CLIENT, AE2LTClientConfig.SPEC, "ae2lt-client.toml");
      MinecraftForge.EVENT_BUS.addListener(this::onServerStarting);
      MinecraftForge.EVENT_BUS.addListener(this::onServerStopped);
      MinecraftForge.EVENT_BUS.addListener(this::onServerTick);
      MinecraftForge.EVENT_BUS.addGenericListener(BlockEntity.class, this::attachBlockEntityCapabilities);
      MinecraftForge.EVENT_BUS.addGenericListener(ItemStack.class, this::attachItemCapabilities);
   }

   private void registerCapabilities(RegisterCapabilitiesEvent event) {
      event.register(ILightningEnergyHandler.class);
   }

   private void attachBlockEntityCapabilities(AttachCapabilitiesEvent<BlockEntity> event) {
      if (hasAttachedCapabilitySupport((BlockEntity)event.getObject())) {
         AE2LightningTech.AttachedBlockEntityCapabilityProvider provider = new AE2LightningTech.AttachedBlockEntityCapabilityProvider(
            (BlockEntity)event.getObject()
         );
         event.addCapability(BLOCK_ENTITY_CAP_PROVIDER_ID, provider);
         event.addListener(provider::invalidate);
      }
   }

   private void attachItemCapabilities(AttachCapabilitiesEvent<ItemStack> event) {
      if (ModList.get().isLoaded("mekanism")) {
         MekanismArmorIntegration.attachCapabilities(event);
      }

      ItemStack stack = (ItemStack)event.getObject();
      Item item = stack.m_41720_();
      if (item == ModItems.ELECTROMAGNETIC_RAILGUN.get()) {
         attachItemEnergy(event, () -> RailgunEnergyBuffer.asEnergyStorage(stack));
      } else if (item == ModItems.CELESTWEAVE_OCULUS.get()
         || item == ModItems.CELESTWEAVE_CORE.get()
         || item == ModItems.CELESTWEAVE_CONDUIT.get()
         || item == ModItems.CELESTWEAVE_STRIDE.get()) {
         attachItemEnergy(event, () -> ArmorEnergyBuffer.asEnergyStorage(stack));
      } else if (ModItems.TIANSHU_WIRELESS_PATTERN_ENCODING_TERMINAL.isPresent() && item == ModItems.TIANSHU_WIRELESS_PATTERN_ENCODING_TERMINAL.get()) {
         attachItemEnergy(event, () -> new AE2LightningTech.ItemPowerSinkEnergyStorage(stack, (IAEItemPowerStorage)item));
      }
   }

   private static void attachItemEnergy(AttachCapabilitiesEvent<ItemStack> event, final NonNullSupplier<IEnergyStorage> supplier) {
      event.addCapability(new ResourceLocation("ae2lt", "item_energy"), new ICapabilityProvider() {
         public <T> LazyOptional<T> getCapability(Capability<T> capability, Direction side) {
            return capability == ForgeCapabilities.ENERGY ? LazyOptional.of(supplier).cast() : LazyOptional.empty();
         }
      });
   }

   private static boolean hasAttachedCapabilitySupport(BlockEntity blockEntity) {
      return blockEntity instanceof LightningCollectorBlockEntity
         || blockEntity instanceof FirmamentConversionCoreBlockEntity
         || blockEntity instanceof OverloadedControllerBlockEntity
         || blockEntity instanceof LightningSimulationChamberBlockEntity
         || blockEntity instanceof LightningAssemblyChamberBlockEntity
         || blockEntity instanceof TeslaCoilBlockEntity
         || blockEntity instanceof OverloadProcessingFactoryBlockEntity
         || blockEntity instanceof AtmosphericIonizerBlockEntity
         || blockEntity instanceof CrystalCatalyzerBlockEntity
         || blockEntity instanceof OverloadedPatternProviderBlockEntity
         || blockEntity instanceof ExtendedOverloadedPatternProviderBlockEntity
         || blockEntity instanceof OverloadedInterfaceBlockEntity
         || blockEntity instanceof OverloadedPowerSupplyBlockEntity
         || blockEntity instanceof WirelessOverloadedControllerBlockEntity
         || blockEntity instanceof AdvancedWirelessOverloadedControllerBlockEntity
         || blockEntity instanceof WirelessReceiverBlockEntity
         || blockEntity instanceof OverloadDeviceWorkbenchBlockEntity
         || blockEntity instanceof PigmeeMentalmathUnitBlockEntity
         || blockEntity instanceof PigmeePatternProviderBlockEntity
         || blockEntity instanceof PigmeeMolecularAssemblerBlockEntity
         || blockEntity instanceof MatrixPortBlockEntity
         || blockEntity instanceof TianshuSupercomputerPortBlockEntity;
   }

   private static IItemHandlerModifiable getItemHandlerCapability(BlockEntity blockEntity) {
      if (blockEntity instanceof LightningCollectorBlockEntity be) {
         return be.getAutomationInventory();
      } else if (blockEntity instanceof FirmamentConversionCoreBlockEntity be) {
         return be.getAutomationInventory();
      } else if (blockEntity instanceof LightningSimulationChamberBlockEntity be) {
         return be.getAutomationInventory();
      } else if (blockEntity instanceof LightningAssemblyChamberBlockEntity be) {
         return be.getAutomationInventory();
      } else if (blockEntity instanceof TeslaCoilBlockEntity be) {
         return be.getAutomationInventory();
      } else if (blockEntity instanceof OverloadProcessingFactoryBlockEntity be) {
         return be.getAutomationInventory();
      } else if (blockEntity instanceof AtmosphericIonizerBlockEntity be) {
         return be.getAutomationInventory();
      } else if (blockEntity instanceof CrystalCatalyzerBlockEntity be) {
         return be.getAutomationInventory();
      } else if (blockEntity instanceof OverloadDeviceWorkbenchBlockEntity) {
         return WORKBENCH_REJECTING_ITEM_HANDLER;
      } else {
         return blockEntity instanceof MatrixPortBlockEntity be ? be.getPatternItemHandler() : null;
      }
   }

   private static IFluidHandler getFluidHandlerCapability(BlockEntity blockEntity, Direction side) {
      if (blockEntity instanceof OverloadProcessingFactoryBlockEntity be) {
         return be.getFluidHandlerCapability(side);
      } else {
         return blockEntity instanceof CrystalCatalyzerBlockEntity be ? be.getFluidHandlerCapability(side) : null;
      }
   }

   private static IEnergyStorage getEnergyCapability(BlockEntity blockEntity, Direction side) {
      if (blockEntity instanceof LightningSimulationChamberBlockEntity be) {
         return be.getEnergyStorageCapability(side);
      } else if (blockEntity instanceof LightningAssemblyChamberBlockEntity be) {
         return be.getEnergyStorageCapability(side);
      } else if (blockEntity instanceof OverloadProcessingFactoryBlockEntity be) {
         return be.getEnergyStorageCapability(side);
      } else if (blockEntity instanceof TeslaCoilBlockEntity be) {
         return be.getEnergyStorageCapability(side);
      } else if (blockEntity instanceof CrystalCatalyzerBlockEntity be) {
         return be.getEnergyStorageCapability(side);
      } else if (blockEntity instanceof OverloadedControllerBlockEntity be) {
         return be.getEnergyStorageCapability(side);
      } else if (blockEntity instanceof WirelessOverloadedControllerBlockEntity be) {
         return be.getEnergyStorageCapability(side);
      } else {
         return blockEntity instanceof AdvancedWirelessOverloadedControllerBlockEntity be ? be.getEnergyStorageCapability(side) : null;
      }
   }

   private static ILightningEnergyHandler getLightningEnergyCapability(BlockEntity blockEntity) {
      if (blockEntity instanceof LightningCollectorBlockEntity be) {
         return new GridLightningEnergyHandler(be);
      } else if (blockEntity instanceof LightningSimulationChamberBlockEntity be) {
         return new GridLightningEnergyHandler(be);
      } else if (blockEntity instanceof LightningAssemblyChamberBlockEntity be) {
         return new GridLightningEnergyHandler(be);
      } else if (blockEntity instanceof OverloadProcessingFactoryBlockEntity be) {
         return new GridLightningEnergyHandler(be);
      } else {
         return blockEntity instanceof TeslaCoilBlockEntity be ? new GridLightningEnergyHandler(be) : null;
      }
   }

   private static GenericInternalInventory getGenericInternalInventoryCapability(BlockEntity blockEntity) {
      if (blockEntity instanceof OverloadedPatternProviderBlockEntity be) {
         OverloadedPatternProviderLogic logic = (OverloadedPatternProviderLogic)be.getLogic();
         return new InsertOnlyReturnInvWrapper((UnlimitedReturnInventory)logic.getInternalReturnInv(), logic);
      } else if (blockEntity instanceof ExtendedOverloadedPatternProviderBlockEntity be) {
         OverloadedPatternProviderLogic logic = (OverloadedPatternProviderLogic)be.getLogic();
         return new InsertOnlyReturnInvWrapper((UnlimitedReturnInventory)logic.getInternalReturnInv(), logic);
      } else if (blockEntity instanceof PigmeePatternProviderBlockEntity be) {
         return be.getReturnInventory();
      } else {
         return blockEntity instanceof OverloadedInterfaceBlockEntity be ? be.getExposedGenericInv() : null;
      }
   }

   private void commonSetup(FMLCommonSetupEvent event) {
      CoreConfig.requireChannelMaxFlow();
      FrequencyApi.setProvider(new FrequencyApiBridge());
      BatchExecutor.registerBatchEligibilityRule(BatchPatternEligibility::isEligible);
      event.enqueueWork(
         () -> {
            ChannelSourceRegistry.registerController("ae2lt:overloaded_controller", OverloadedControllerBlockEntity.class);
            CoreConfig.setChannelsPerController(AE2LTCommonConfig.overloadedControllerChannelsPerController());
            CoreConfig.setBatchCopyLimitedBlocks(AE2LTCommonConfig.batchCopyLimitedBlocks());
            LightningCollectorBlock lightningCollectorBlock = (LightningCollectorBlock)ModBlocks.LIGHTNING_COLLECTOR.get();
            BlockEntityType<LightningCollectorBlockEntity> lightningCollectorBeType = (BlockEntityType<LightningCollectorBlockEntity>)ModBlockEntities.LIGHTNING_COLLECTOR
               .get();
            lightningCollectorBlock.setBlockEntity(
               LightningCollectorBlockEntity.class, lightningCollectorBeType, null, LightningCollectorBlockEntity::serverTick
            );
            OverloadedControllerBlock controllerBlock = (OverloadedControllerBlock)ModBlocks.OVERLOADED_CONTROLLER.get();
            BlockEntityType<OverloadedControllerBlockEntity> controllerBeType = (BlockEntityType<OverloadedControllerBlockEntity>)ModBlockEntities.OVERLOADED_CONTROLLER
               .get();
            controllerBlock.setBlockEntity(OverloadedControllerBlockEntity.class, controllerBeType, null, OverloadedControllerBlockEntity::serverTick);
            LightningSimulationChamberBlock lightningChamberBlock = (LightningSimulationChamberBlock)ModBlocks.LIGHTNING_SIMULATION_CHAMBER.get();
            BlockEntityType<LightningSimulationChamberBlockEntity> lightningChamberBeType = (BlockEntityType<LightningSimulationChamberBlockEntity>)ModBlockEntities.LIGHTNING_SIMULATION_CHAMBER
               .get();
            lightningChamberBlock.setBlockEntity(
               LightningSimulationChamberBlockEntity.class, lightningChamberBeType, null, LightningSimulationChamberBlockEntity::serverTick
            );
            LightningAssemblyChamberBlock assemblyBlock = (LightningAssemblyChamberBlock)ModBlocks.LIGHTNING_ASSEMBLY_CHAMBER.get();
            BlockEntityType<LightningAssemblyChamberBlockEntity> assemblyBeType = (BlockEntityType<LightningAssemblyChamberBlockEntity>)ModBlockEntities.LIGHTNING_ASSEMBLY_CHAMBER
               .get();
            assemblyBlock.setBlockEntity(LightningAssemblyChamberBlockEntity.class, assemblyBeType, null, LightningAssemblyChamberBlockEntity::serverTick);
            OverloadProcessingFactoryBlock overloadProcessingFactoryBlock = (OverloadProcessingFactoryBlock)ModBlocks.OVERLOAD_PROCESSING_FACTORY.get();
            BlockEntityType<OverloadProcessingFactoryBlockEntity> overloadProcessingFactoryBeType = (BlockEntityType<OverloadProcessingFactoryBlockEntity>)ModBlockEntities.OVERLOAD_PROCESSING_FACTORY
               .get();
            overloadProcessingFactoryBlock.setBlockEntity(
               OverloadProcessingFactoryBlockEntity.class, overloadProcessingFactoryBeType, null, OverloadProcessingFactoryBlockEntity::serverTick
            );
            TeslaCoilBlock teslaCoilBlock = (TeslaCoilBlock)ModBlocks.TESLA_COIL.get();
            BlockEntityType<TeslaCoilBlockEntity> teslaCoilBeType = (BlockEntityType<TeslaCoilBlockEntity>)ModBlockEntities.TESLA_COIL.get();
            teslaCoilBlock.setBlockEntity(TeslaCoilBlockEntity.class, teslaCoilBeType, null, TeslaCoilBlockEntity::serverTick);
            AtmosphericIonizerBlock atmosphericIonizerBlock = (AtmosphericIonizerBlock)ModBlocks.ATMOSPHERIC_IONIZER.get();
            BlockEntityType<AtmosphericIonizerBlockEntity> atmosphericIonizerBeType = (BlockEntityType<AtmosphericIonizerBlockEntity>)ModBlockEntities.ATMOSPHERIC_IONIZER
               .get();
            atmosphericIonizerBlock.setBlockEntity(
               AtmosphericIonizerBlockEntity.class, atmosphericIonizerBeType, null, AtmosphericIonizerBlockEntity::serverTick
            );
            OverloadDeviceWorkbenchBlock overloadDeviceWorkbenchBlock = (OverloadDeviceWorkbenchBlock)ModBlocks.OVERLOAD_DEVICE_WORKBENCH.get();
            BlockEntityType<OverloadDeviceWorkbenchBlockEntity> overloadDeviceWorkbenchBeType = (BlockEntityType<OverloadDeviceWorkbenchBlockEntity>)ModBlockEntities.OVERLOAD_DEVICE_WORKBENCH
               .get();
            overloadDeviceWorkbenchBlock.setBlockEntity(OverloadDeviceWorkbenchBlockEntity.class, overloadDeviceWorkbenchBeType, null, null);
            CrystalCatalyzerBlock crystalCatalyzerBlock = (CrystalCatalyzerBlock)ModBlocks.CRYSTAL_CATALYZER.get();
            BlockEntityType<CrystalCatalyzerBlockEntity> crystalCatalyzerBeType = (BlockEntityType<CrystalCatalyzerBlockEntity>)ModBlockEntities.CRYSTAL_CATALYZER
               .get();
            crystalCatalyzerBlock.setBlockEntity(CrystalCatalyzerBlockEntity.class, crystalCatalyzerBeType, null, CrystalCatalyzerBlockEntity::serverTick);
            OverloadedPatternProviderBlock<OverloadedPatternProviderBlockEntity> block = (OverloadedPatternProviderBlock<OverloadedPatternProviderBlockEntity>)ModBlocks.OVERLOADED_PATTERN_PROVIDER
               .get();
            BlockEntityType<OverloadedPatternProviderBlockEntity> beType = (BlockEntityType<OverloadedPatternProviderBlockEntity>)ModBlockEntities.OVERLOADED_PATTERN_PROVIDER
               .get();
            block.setBlockEntity(OverloadedPatternProviderBlockEntity.class, beType, null, OverloadedPatternProviderBlockEntity::serverTick);
            OverloadedPatternProviderBlock<ExtendedOverloadedPatternProviderBlockEntity> extendedPatternProviderBlock = (OverloadedPatternProviderBlock<ExtendedOverloadedPatternProviderBlockEntity>)ModBlocks.EXTENDED_OVERLOADED_PATTERN_PROVIDER
               .get();
            BlockEntityType<ExtendedOverloadedPatternProviderBlockEntity> extendedPatternProviderBeType = (BlockEntityType<ExtendedOverloadedPatternProviderBlockEntity>)ModBlockEntities.EXTENDED_OVERLOADED_PATTERN_PROVIDER
               .get();
            extendedPatternProviderBlock.setBlockEntity(
               ExtendedOverloadedPatternProviderBlockEntity.class,
               extendedPatternProviderBeType,
               null,
               ExtendedOverloadedPatternProviderBlockEntity::serverTick
            );
            PigmeeMentalmathUnitBlock pigmeeMentalmathUnitBlock = (PigmeeMentalmathUnitBlock)ModBlocks.PIGMEE_MENTALMATH_UNIT.get();
            BlockEntityType<PigmeeMentalmathUnitBlockEntity> pigmeeMentalmathUnitBeType = (BlockEntityType<PigmeeMentalmathUnitBlockEntity>)ModBlockEntities.PIGMEE_MENTALMATH_UNIT
               .get();
            pigmeeMentalmathUnitBlock.setBlockEntity(PigmeeMentalmathUnitBlockEntity.class, pigmeeMentalmathUnitBeType, null, null);
            PigmeePatternProviderBlock pigmeePatternProviderBlock = (PigmeePatternProviderBlock)ModBlocks.PIGMEE_PATTERN_PROVIDER.get();
            BlockEntityType<PigmeePatternProviderBlockEntity> pigmeePatternProviderBeType = (BlockEntityType<PigmeePatternProviderBlockEntity>)ModBlockEntities.PIGMEE_PATTERN_PROVIDER
               .get();
            pigmeePatternProviderBlock.setBlockEntity(
               PigmeePatternProviderBlockEntity.class, pigmeePatternProviderBeType, null, PigmeePatternProviderBlockEntity::serverTick
            );
            PigmeeMolecularAssemblerBlock pigmeeAssemblerBlock = (PigmeeMolecularAssemblerBlock)ModBlocks.PIGMEE_MOLECULAR_ASSEMBLER.get();
            BlockEntityType<PigmeeMolecularAssemblerBlockEntity> pigmeeAssemblerBeType = (BlockEntityType<PigmeeMolecularAssemblerBlockEntity>)ModBlockEntities.PIGMEE_MOLECULAR_ASSEMBLER
               .get();
            pigmeeAssemblerBlock.setBlockEntity(PigmeeMolecularAssemblerBlockEntity.class, pigmeeAssemblerBeType, null, null);
            MatrixPortBlock matrixPortBlock = (MatrixPortBlock)ModBlocks.MATTER_WARPING_MATRIX_PORT.get();
            BlockEntityType<MatrixPortBlockEntity> matrixPortBeType = (BlockEntityType<MatrixPortBlockEntity>)ModBlockEntities.MATRIX_PORT.get();
            matrixPortBlock.setBlockEntity(MatrixPortBlockEntity.class, matrixPortBeType, null, MatrixPortBlockEntity::serverTick);
            TianshuSupercomputerPortBlock tianshuPortBlock = (TianshuSupercomputerPortBlock)ModBlocks.TIANSHU_SUPERCOMPUTER_PORT.get();
            BlockEntityType<TianshuSupercomputerPortBlockEntity> tianshuPortBeType = (BlockEntityType<TianshuSupercomputerPortBlockEntity>)ModBlockEntities.TIANSHU_SUPERCOMPUTER_PORT
               .get();
            tianshuPortBlock.setBlockEntity(TianshuSupercomputerPortBlockEntity.class, tianshuPortBeType, null, TianshuSupercomputerPortBlockEntity::serverTick);
            AEBaseBlockEntity.registerBlockEntityItem(
               (BlockEntityType)ModBlockEntities.TIANSHU_SEED_STORAGE.get(), ((TianshuSeedStorageBlock)ModBlocks.CLOSED_LOOP_SEED_STORAGE.get()).m_5456_()
            );
            OverloadedInterfaceBlock interfaceBlock = (OverloadedInterfaceBlock)ModBlocks.OVERLOADED_INTERFACE.get();
            BlockEntityType<OverloadedInterfaceBlockEntity> interfaceBeType = (BlockEntityType<OverloadedInterfaceBlockEntity>)ModBlockEntities.OVERLOADED_INTERFACE
               .get();
            interfaceBlock.setBlockEntity(OverloadedInterfaceBlockEntity.class, interfaceBeType, null, OverloadedInterfaceBlockEntity::serverTick);
            if (ModBlocks.hasOverloadedPowerSupply()) {
               OverloadedPowerSupplyBlock powerSupplyBlock = (OverloadedPowerSupplyBlock)ModBlocks.OVERLOADED_POWER_SUPPLY.get();
               BlockEntityType<OverloadedPowerSupplyBlockEntity> powerSupplyBeType = (BlockEntityType<OverloadedPowerSupplyBlockEntity>)ModBlockEntities.OVERLOADED_POWER_SUPPLY
                  .get();
               powerSupplyBlock.setBlockEntity(OverloadedPowerSupplyBlockEntity.class, powerSupplyBeType, null, OverloadedPowerSupplyBlockEntity::serverTick);
            }

            AEBaseBlockEntity.registerBlockEntityItem(lightningCollectorBeType, lightningCollectorBlock.m_5456_());
            AEBaseBlockEntity.registerBlockEntityItem(
               (BlockEntityType)ModBlockEntities.OVERLOADED_CONTROLLER.get(), ((OverloadedControllerBlock)ModBlocks.OVERLOADED_CONTROLLER.get()).m_5456_()
            );
            AEBaseBlockEntity.registerBlockEntityItem(
               (BlockEntityType)ModBlockEntities.OVERLOADED_PATTERN_PROVIDER.get(),
               ((OverloadedPatternProviderBlock)ModBlocks.OVERLOADED_PATTERN_PROVIDER.get()).m_5456_()
            );
            AEBaseBlockEntity.registerBlockEntityItem(
               (BlockEntityType)ModBlockEntities.EXTENDED_OVERLOADED_PATTERN_PROVIDER.get(),
               ((OverloadedPatternProviderBlock)ModBlocks.EXTENDED_OVERLOADED_PATTERN_PROVIDER.get()).m_5456_()
            );
            AEBaseBlockEntity.registerBlockEntityItem(pigmeeMentalmathUnitBeType, pigmeeMentalmathUnitBlock.m_5456_());
            AEBaseBlockEntity.registerBlockEntityItem(pigmeePatternProviderBeType, pigmeePatternProviderBlock.m_5456_());
            AEBaseBlockEntity.registerBlockEntityItem(pigmeeAssemblerBeType, pigmeeAssemblerBlock.m_5456_());
            AEBaseBlockEntity.registerBlockEntityItem(matrixPortBeType, matrixPortBlock.m_5456_());
            AEBaseBlockEntity.registerBlockEntityItem(interfaceBeType, interfaceBlock.m_5456_());
            if (ModBlocks.hasOverloadedPowerSupply()) {
               AEBaseBlockEntity.registerBlockEntityItem(
                  (BlockEntityType)ModBlockEntities.OVERLOADED_POWER_SUPPLY.get(),
                  ((OverloadedPowerSupplyBlock)ModBlocks.OVERLOADED_POWER_SUPPLY.get()).m_5456_()
               );
            }

            AEBaseBlockEntity.registerBlockEntityItem(
               (BlockEntityType)ModBlockEntities.LIGHTNING_SIMULATION_CHAMBER.get(),
               ((LightningSimulationChamberBlock)ModBlocks.LIGHTNING_SIMULATION_CHAMBER.get()).m_5456_()
            );
            AEBaseBlockEntity.registerBlockEntityItem(assemblyBeType, assemblyBlock.m_5456_());
            AEBaseBlockEntity.registerBlockEntityItem(overloadProcessingFactoryBeType, overloadProcessingFactoryBlock.m_5456_());
            AEBaseBlockEntity.registerBlockEntityItem(teslaCoilBeType, teslaCoilBlock.m_5456_());
            AEBaseBlockEntity.registerBlockEntityItem(atmosphericIonizerBeType, atmosphericIonizerBlock.m_5456_());
            AEBaseBlockEntity.registerBlockEntityItem(overloadDeviceWorkbenchBeType, overloadDeviceWorkbenchBlock.m_5456_());
            AEBaseBlockEntity.registerBlockEntityItem(crystalCatalyzerBeType, crystalCatalyzerBlock.m_5456_());
            setupWirelessControllerBlock(
               (AEBaseEntityBlock)ModBlocks.WIRELESS_OVERLOADED_CONTROLLER.get(),
               (BlockEntityType)ModBlockEntities.WIRELESS_OVERLOADED_CONTROLLER.get(),
               WirelessOverloadedControllerBlockEntity.class,
               (level, pos, state, be) -> WirelessOverloadedControllerBlockEntity.wirelessServerTick(
                     level, pos, state, (WirelessOverloadedControllerBlockEntity)be
                  )
            );
            setupWirelessControllerBlock(
               (AEBaseEntityBlock)ModBlocks.ADVANCED_WIRELESS_OVERLOADED_CONTROLLER.get(),
               (BlockEntityType)ModBlockEntities.ADVANCED_WIRELESS_OVERLOADED_CONTROLLER.get(),
               AdvancedWirelessOverloadedControllerBlockEntity.class,
               (level, pos, state, be) -> AdvancedWirelessOverloadedControllerBlockEntity.advancedWirelessServerTick(
                     level, pos, state, (AdvancedWirelessOverloadedControllerBlockEntity)be
                  )
            );
            WirelessReceiverBlock wirelessReceiverBlock = (WirelessReceiverBlock)ModBlocks.WIRELESS_RECEIVER.get();
            BlockEntityType<WirelessReceiverBlockEntity> wirelessReceiverBeType = (BlockEntityType<WirelessReceiverBlockEntity>)ModBlockEntities.WIRELESS_RECEIVER
               .get();
            wirelessReceiverBlock.setBlockEntity(WirelessReceiverBlockEntity.class, wirelessReceiverBeType, null, (level, pos, state, be) -> be.serverTick());
            AEBaseBlockEntity.registerBlockEntityItem(wirelessReceiverBeType, wirelessReceiverBlock.m_5456_());
            MachineAdapterRegistry.init();
            PatternDetailsHelper.registerDecoder(OverloadPatternDecoder.INSTANCE);
            PatternDetailsHelper.registerDecoder(ClosedLoopPatternDecoder.INSTANCE);
            StorageCells.addCellHandler(BulkLightningCellHandler.INSTANCE);
            StorageCells.addCellHandler(FixedInfiniteCellHandler.INSTANCE);
            StorageCells.addCellHandler(VoidCellHandler.INSTANCE);
            ModItems.registerStorageCellModels();
            Upgrades.add(AEItems.SPEED_CARD, (ItemLike)ModBlocks.LIGHTNING_SIMULATION_CHAMBER.get(), 4);
            Upgrades.add(AEItems.SPEED_CARD, (ItemLike)ModBlocks.LIGHTNING_ASSEMBLY_CHAMBER.get(), 4);
            Upgrades.add(AEItems.SPEED_CARD, (ItemLike)ModBlocks.OVERLOAD_PROCESSING_FACTORY.get(), 4);
            Upgrades.add(AEItems.FUZZY_CARD, (ItemLike)ModItems.INFINITE_STORAGE_CELL.get(), 1);
            Upgrades.add(AEItems.INVERTER_CARD, (ItemLike)ModItems.INFINITE_STORAGE_CELL.get(), 1);
            Upgrades.add(AEItems.FUZZY_CARD, (ItemLike)ModItems.OVERLOADED_FILTER_COMPONENT.get(), 1);
            Upgrades.add(AEItems.INVERTER_CARD, (ItemLike)ModItems.OVERLOADED_FILTER_COMPONENT.get(), 1);
            Upgrades.add(AEItems.FUZZY_CARD, (ItemLike)ModItems.VOID_CELL.get(), 1);
            Upgrades.add(AEItems.INVERTER_CARD, (ItemLike)ModItems.VOID_CELL.get(), 1);
            Upgrades.add(AEItems.CRAFTING_CARD, (ItemLike)ModBlocks.OVERLOADED_INTERFACE.get(), 1);
            Upgrades.add(AEItems.FUZZY_CARD, (ItemLike)ModBlocks.OVERLOADED_INTERFACE.get(), 1);
            registerAppliedFluxInductionCardCompat();
            registerOverloadTntDispenseBehavior();
            if (ModList.get().isLoaded("ae2wtlib")) {
               GridLinkables.register((ItemLike)ModItems.TIANSHU_WIRELESS_PATTERN_ENCODING_TERMINAL.get(), WirelessTerminalItem.LINKABLE_HANDLER);
               Ae2wtlibIntegration.verifyTerminalRegistration();
               Ae2wtlibIntegration.register();
            }
         }
      );
   }

   private void onConfigChanged(ModConfigEvent event) {
      if (event.getConfig().getSpec() == AE2LTCommonConfig.SPEC) {
         syncThunderboltConfig();
      }
   }

   private static void syncThunderboltConfig() {
      CoreConfig.setChannelsPerController(AE2LTCommonConfig.overloadedControllerChannelsPerController());
      CoreConfig.setBatchCopyLimitedBlocks(AE2LTCommonConfig.batchCopyLimitedBlocks());
   }

   private static void registerOverloadTntDispenseBehavior() {
      DispenserBlock.m_52672_(((OverloadTntBlock)ModBlocks.OVERLOAD_TNT.get()).m_5456_(), new DefaultDispenseItemBehavior() {
         protected ItemStack m_7498_(BlockSource source, ItemStack stack) {
            ServerLevel level = source.m_7727_();
            BlockPos pos = source.m_7961_().m_121945_((Direction)source.m_6414_().m_61143_(DispenserBlock.f_52659_));
            OverloadTntEntity tnt = new OverloadTntEntity(level, (double)pos.m_123341_() + 0.5, (double)pos.m_123342_(), (double)pos.m_123343_() + 0.5, null);
            level.m_7967_(tnt);
            level.m_6263_(null, tnt.m_20185_(), tnt.m_20186_(), tnt.m_20189_(), SoundEvents.f_12512_, SoundSource.BLOCKS, 1.0F, 1.0F);
            level.m_142346_(null, GameEvent.f_157810_, pos);
            stack.m_41774_(1);
            return stack;
         }
      });
   }

   private static void registerAppliedFluxInductionCardCompat() {
      ResourceLocation inductionId = new ResourceLocation("appflux", "induction_card");
      Item inductionCard = (Item)BuiltInRegistries.f_257033_.m_7745_(inductionId);
      if (inductionCard != null && inductionCard != Items.f_41852_) {
         Upgrades.add(inductionCard, (ItemLike)ModBlocks.OVERLOADED_PATTERN_PROVIDER.get(), 1, "group.pattern_provider.name");
         Upgrades.add(inductionCard, (ItemLike)ModBlocks.EXTENDED_OVERLOADED_PATTERN_PROVIDER.get(), 1, "group.pattern_provider.name");
         Upgrades.add(inductionCard, (ItemLike)ModBlocks.OVERLOADED_INTERFACE.get(), 1);
      }
   }

   private void onServerStarting(ServerStartingEvent event) {
      WirelessFrequencyManager.onServerStart(event.getServer());
      WirelessLinkRegistry.onServerStart(event.getServer());
      List<ResourceLocation> recipeConflicts = RecipeConflictScanner.scan(event.getServer().m_129894_());
      if (recipeConflicts.isEmpty()) {
         LOG.info("[AE2LT recipe conflict scan] no conflicts found");
      } else {
         LOG.warn("[AE2LT recipe conflict scan] {} matching recipe ids: {}", recipeConflicts.size(), recipeConflicts);
      }
   }

   private void onServerStopped(ServerStoppedEvent event) {
      WirelessLinkRegistry.onServerStop();
      WirelessFrequencyManager.onServerStop();
      CRAFTING_CORE_REGISTRY.clear();
      ModDamageTypes.clearCache();
   }

   private void onServerTick(ServerTickEvent event) {
      if (event.phase == Phase.END) {
         CRAFTING_CORE_REGISTRY.tickAll();
         WirelessFrequencyManager.flushPendingDeviceNotifications();
         WirelessLinkRegistry registry = WirelessLinkRegistry.get();
         if (registry != null) {
            registry.tick(event.getServer());
         }
      }
   }

   private static void acceptCreative(Output output, RegistryObject<? extends ItemLike> holder) {
      output.m_246326_((ItemLike)holder.get());
   }

   private static void setupWirelessControllerBlock(AEBaseEntityBlock block, BlockEntityType beType, Class beClass, BlockEntityTicker serverTicker) {
      block.setBlockEntity(beClass, beType, null, serverTicker);
      AEBaseBlockEntity.registerBlockEntityItem(beType, block.m_5456_());
   }

   private static final class AttachedBlockEntityCapabilityProvider implements ICapabilityProvider {
      private final BlockEntity blockEntity;
      private final EnumMap<Direction, LazyOptional<IFluidHandler>> fluidHandlers = new EnumMap<>(Direction.class);
      private final EnumMap<Direction, LazyOptional<IEnergyStorage>> energyHandlers = new EnumMap<>(Direction.class);
      private LazyOptional<IItemHandlerModifiable> itemHandler;
      private LazyOptional<IFluidHandler> nullSideFluidHandler;
      private LazyOptional<IEnergyStorage> nullSideEnergyHandler;
      private LazyOptional<IInWorldGridNodeHost> gridNodeHost;
      private LazyOptional<ICraftingMachine> craftingMachine;
      private LazyOptional<ILightningEnergyHandler> lightningEnergyHandler;
      private LazyOptional<GenericInternalInventory> genericInternalInventory;

      private AttachedBlockEntityCapabilityProvider(BlockEntity blockEntity) {
         this.blockEntity = blockEntity;
      }

      public <T> LazyOptional<T> getCapability(Capability<T> capability, Direction side) {
         if (capability == ForgeCapabilities.ITEM_HANDLER) {
            return this.itemHandler().cast();
         } else if (capability == ForgeCapabilities.FLUID_HANDLER) {
            return this.fluidHandler(side).cast();
         } else if (capability == ForgeCapabilities.ENERGY) {
            return this.energyHandler(side).cast();
         } else if (capability == Capabilities.IN_WORLD_GRID_NODE_HOST) {
            return this.gridNodeHost().cast();
         } else if (capability == Capabilities.CRAFTING_MACHINE) {
            return this.craftingMachine().cast();
         } else if (capability == AE2LTCapabilities.LIGHTNING_ENERGY_BLOCK) {
            return this.lightningEnergyHandler().cast();
         } else {
            return capability == Capabilities.GENERIC_INTERNAL_INV ? this.genericInternalInventory().cast() : LazyOptional.empty();
         }
      }

      private LazyOptional<IItemHandlerModifiable> itemHandler() {
         if (this.itemHandler == null) {
            IItemHandlerModifiable handler = AE2LightningTech.getItemHandlerCapability(this.blockEntity);
            if (handler != null) {
               this.itemHandler = LazyOptional.of(() -> handler);
            }
         }

         return this.itemHandler != null ? this.itemHandler : LazyOptional.empty();
      }

      private LazyOptional<IFluidHandler> fluidHandler(Direction side) {
         if (side == null) {
            if (this.nullSideFluidHandler == null) {
               IFluidHandler handler = AE2LightningTech.getFluidHandlerCapability(this.blockEntity, null);
               if (handler != null) {
                  this.nullSideFluidHandler = LazyOptional.of(() -> handler);
               }
            }

            return this.nullSideFluidHandler != null ? this.nullSideFluidHandler : LazyOptional.empty();
         } else {
            LazyOptional<IFluidHandler> cached = this.fluidHandlers.get(side);
            if (cached != null) {
               return cached;
            } else {
               IFluidHandler handler = AE2LightningTech.getFluidHandlerCapability(this.blockEntity, side);
               if (handler == null) {
                  return LazyOptional.empty();
               } else {
                  LazyOptional<IFluidHandler> optional = LazyOptional.of(() -> handler);
                  this.fluidHandlers.put((Enum)side, optional);
                  return optional;
               }
            }
         }
      }

      private LazyOptional<IEnergyStorage> energyHandler(Direction side) {
         if (side == null) {
            if (this.nullSideEnergyHandler == null) {
               IEnergyStorage handler = AE2LightningTech.getEnergyCapability(this.blockEntity, null);
               if (handler != null) {
                  this.nullSideEnergyHandler = LazyOptional.of(() -> handler);
               }
            }

            return this.nullSideEnergyHandler != null ? this.nullSideEnergyHandler : LazyOptional.empty();
         } else {
            LazyOptional<IEnergyStorage> cached = this.energyHandlers.get(side);
            if (cached != null) {
               return cached;
            } else {
               IEnergyStorage handler = AE2LightningTech.getEnergyCapability(this.blockEntity, side);
               if (handler == null) {
                  return LazyOptional.empty();
               } else {
                  LazyOptional<IEnergyStorage> optional = LazyOptional.of(() -> handler);
                  this.energyHandlers.put((Enum)side, optional);
                  return optional;
               }
            }
         }
      }

      private LazyOptional<IInWorldGridNodeHost> gridNodeHost() {
         if (this.gridNodeHost == null && this.blockEntity instanceof IInWorldGridNodeHost host) {
            this.gridNodeHost = LazyOptional.of(() -> host);
         }

         return this.gridNodeHost != null ? this.gridNodeHost : LazyOptional.empty();
      }

      private LazyOptional<ICraftingMachine> craftingMachine() {
         if (this.craftingMachine == null && this.blockEntity instanceof ICraftingMachine machine) {
            this.craftingMachine = LazyOptional.of(() -> machine);
         }

         return this.craftingMachine != null ? this.craftingMachine : LazyOptional.empty();
      }

      private LazyOptional<ILightningEnergyHandler> lightningEnergyHandler() {
         if (this.lightningEnergyHandler == null) {
            ILightningEnergyHandler handler = AE2LightningTech.getLightningEnergyCapability(this.blockEntity);
            if (handler != null) {
               this.lightningEnergyHandler = LazyOptional.of(() -> handler);
            }
         }

         return this.lightningEnergyHandler != null ? this.lightningEnergyHandler : LazyOptional.empty();
      }

      private LazyOptional<GenericInternalInventory> genericInternalInventory() {
         if (this.genericInternalInventory == null) {
            GenericInternalInventory inventory = AE2LightningTech.getGenericInternalInventoryCapability(this.blockEntity);
            if (inventory != null) {
               this.genericInternalInventory = LazyOptional.of(() -> inventory);
            }
         }

         return this.genericInternalInventory != null ? this.genericInternalInventory : LazyOptional.empty();
      }

      private void invalidate() {
         invalidate(this.itemHandler);
         invalidate(this.nullSideFluidHandler);
         invalidate(this.nullSideEnergyHandler);
         invalidate(this.gridNodeHost);
         invalidate(this.craftingMachine);
         invalidate(this.lightningEnergyHandler);
         invalidate(this.genericInternalInventory);
         this.fluidHandlers.values().forEach(AE2LightningTech.AttachedBlockEntityCapabilityProvider::invalidate);
         this.energyHandlers.values().forEach(AE2LightningTech.AttachedBlockEntityCapabilityProvider::invalidate);
         this.fluidHandlers.clear();
         this.energyHandlers.clear();
      }

      private static void invalidate(LazyOptional<?> optional) {
         if (optional != null) {
            optional.invalidate();
         }
      }
   }

   private static final class ItemPowerSinkEnergyStorage implements IEnergyStorage {
      private final ItemStack stack;
      private final IAEItemPowerStorage sink;

      private ItemPowerSinkEnergyStorage(ItemStack stack, IAEItemPowerStorage sink) {
         this.stack = stack;
         this.sink = sink;
      }

      public int receiveEnergy(int maxReceive, boolean simulate) {
         return (int)this.sink.injectAEPower(this.stack, (double)maxReceive, Actionable.ofSimulate(simulate));
      }

      public int extractEnergy(int maxExtract, boolean simulate) {
         return (int)this.sink.extractAEPower(this.stack, (double)maxExtract, Actionable.ofSimulate(simulate));
      }

      public int getEnergyStored() {
         return (int)this.sink.getAECurrentPower(this.stack);
      }

      public int getMaxEnergyStored() {
         return (int)this.sink.getAEMaxPower(this.stack);
      }

      public boolean canExtract() {
         return this.sink.getPowerFlow(this.stack).isAllowExtraction();
      }

      public boolean canReceive() {
         return this.sink.getPowerFlow(this.stack).isAllowInsertion();
      }
   }
}
