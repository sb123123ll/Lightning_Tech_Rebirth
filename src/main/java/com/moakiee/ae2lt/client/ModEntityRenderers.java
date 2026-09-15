package com.moakiee.ae2lt.client;

import com.moakiee.ae2lt.client.core.MatrixCoreEffectRenderer;
import com.moakiee.ae2lt.client.core.TianshuCoreEffectRenderer;
import com.moakiee.ae2lt.registry.ModBlockEntities;
import com.moakiee.ae2lt.registry.ModBlocks;
import com.moakiee.ae2lt.registry.ModEntities;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.TntRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent.AddLayers;
import net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers;
import net.minecraftforge.client.event.ModelEvent.ModifyBakingResult;
import net.minecraftforge.client.event.ModelEvent.RegisterAdditional;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(
   modid = "ae2lt",
   bus = Bus.MOD,
   value = {Dist.CLIENT}
)
public final class ModEntityRenderers {
   private ModEntityRenderers() {
   }

   @SubscribeEvent
   public static void registerRenderLayers(FMLClientSetupEvent event) {
      event.enqueueWork(() -> ItemBlockRenderTypes.setRenderLayer((Block)ModBlocks.PIGMEE_MOLECULAR_ASSEMBLER.get(), RenderType.m_110463_()));
   }

   @SubscribeEvent
   public static void registerRenderers(RegisterRenderers event) {
      event.registerEntityRenderer((EntityType)ModEntities.OVERLOAD_TNT.get(), TntRenderer::new);
      event.registerEntityRenderer((EntityType)ModEntities.FLOATING_MATTER.get(), ItemEntityRenderer::new);
      event.registerEntityRenderer((EntityType)ModEntities.RITUAL_HYPERDIMENSIONAL_PIGMEE.get(), RitualHyperdimensionalPigmeeRenderer::new);
      event.registerBlockEntityRenderer((BlockEntityType)ModBlockEntities.LIGHTNING_SIMULATION_CHAMBER.get(), LightningSimulationChamberRenderer::new);
      event.registerBlockEntityRenderer((BlockEntityType)ModBlockEntities.LIGHTNING_ASSEMBLY_CHAMBER.get(), LightningAssemblyChamberRenderer::new);
      event.registerBlockEntityRenderer((BlockEntityType)ModBlockEntities.CRYSTAL_CATALYZER.get(), CrystalCatalyzerRenderer::new);
      event.registerBlockEntityRenderer((BlockEntityType)ModBlockEntities.FUMO.get(), FumoBlockRenderer::new);
      event.registerBlockEntityRenderer((BlockEntityType)ModBlockEntities.PIGMEE_MOLECULAR_ASSEMBLER.get(), PigmeeMolecularAssemblerRenderer::new);
      event.registerBlockEntityRenderer((BlockEntityType)ModBlockEntities.MATRIX_CONTROLLER.get(), MatrixCoreEffectRenderer::new);
      event.registerBlockEntityRenderer((BlockEntityType)ModBlockEntities.TIANSHU_SUPERCOMPUTER_CONTROLLER.get(), TianshuCoreEffectRenderer::new);
   }

   @SubscribeEvent
   public static void addPlayerLayers(AddLayers event) {
      for (String skin : event.getSkins()) {
         PlayerRenderer renderer = (PlayerRenderer)event.getSkin(skin);
         if (renderer != null) {
            renderer.m_115326_(new PhaseWingLayer(renderer, event.getEntityModels()));
         }
      }
   }

   @SubscribeEvent
   public static void registerAdditionalModels(RegisterAdditional event) {
      event.register(PigmeeMolecularAssemblerRenderer.LIGHTS_MODEL);
      event.register(HyperdimensionalPigmeeTextureLayer.MODEL);
   }

   @SubscribeEvent
   public static void wrapFumoItemModels(ModifyBakingResult event) {
      wrapFumoItemModel(event, "moakiee_fumo");
      wrapFumoItemModel(event, "cystrysu_fumo");
      wrapFumoItemModel(event, "pigmee_fumo");
      wrapFumoItemModel(event, "creative_pigmee_fumo");
      wrapFumoItemModel(event, "hyperdimensional_pigmee_fumo");
   }

   private static void wrapFumoItemModel(ModifyBakingResult event, String itemId) {
      ResourceLocation id = new ResourceLocation("ae2lt", itemId);
      ModelResourceLocation modelId = new ModelResourceLocation(id, "inventory");
      event.getModels()
         .computeIfPresent(
            modelId,
            (ignored, model) -> (BakedModel)(itemId.equals("hyperdimensional_pigmee_fumo")
                  ? new HyperdimensionalPigmeeBakedModel(model)
                  : new SpinningFumoBakedModel(model))
         );
   }
}
