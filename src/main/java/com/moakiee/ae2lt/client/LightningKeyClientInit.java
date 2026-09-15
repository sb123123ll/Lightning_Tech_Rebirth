package com.moakiee.ae2lt.client;

import appeng.api.client.AEKeyRendering;
import appeng.api.util.AEColor;
import appeng.items.storage.BasicStorageCell;
import com.moakiee.ae2lt.client.railgun.RailgunClientBootstrap;
import com.moakiee.ae2lt.item.ElectroChimeCrystalItem;
import com.moakiee.ae2lt.item.FixedInfiniteCellItem;
import com.moakiee.ae2lt.item.railgun.ElectromagneticRailgunItem;
import com.moakiee.ae2lt.me.key.LightningKey;
import com.moakiee.ae2lt.me.key.LightningKeyType;
import com.moakiee.ae2lt.registry.ModItems;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(
   modid = "ae2lt",
   bus = Bus.MOD,
   value = {Dist.CLIENT}
)
public final class LightningKeyClientInit {
   private LightningKeyClientInit() {
   }

   @SubscribeEvent
   public static void onClientSetup(FMLClientSetupEvent event) {
      event.enqueueWork(
         () -> {
            RailgunClientBootstrap.install();
            ResearchNoteClientBootstrap.install();
            ShieldHitFeedbackClientBootstrap.install();
            if (ModList.get().isLoaded("curios")) {
               PigmeeCuriosClientBridge.registerRenderers();
            }

            AEKeyRendering.register(LightningKeyType.INSTANCE, LightningKey.class, LightningKeyRenderHandler.INSTANCE);
            ItemProperties.register(
               (Item)ModItems.ELECTRO_CHIME_CRYSTAL.get(),
               new ResourceLocation("ae2lt", "catalysis_stage"),
               (stack, level, entity, seed) -> (float)ElectroChimeCrystalItem.getCatalysisStage(stack) * 0.25F
            );
            ItemProperties.register((Item)ModItems.MYSTERIOUS_CELL.get(), new ResourceLocation("ae2lt", "cell_type"), (stack, level, entity, seed) -> {
               if (!FixedInfiniteCellItem.hasType(stack)) {
                  return 0.0F;
               } else {
                  return switch (FixedInfiniteCellItem.getType(stack)) {
                     case 1 -> 1.0F;
                     case 2 -> 2.0F;
                     default -> 0.0F;
                  };
               }
            });
            ItemProperties.register(
               (Item)ModItems.ELECTROMAGNETIC_RAILGUN.get(),
               new ResourceLocation("ae2lt", "ehv_model"),
               (stack, level, entity, seed) -> entity != null
                        && entity.m_6117_()
                        && entity.m_21211_() == stack
                        && stack.m_41720_() instanceof ElectromagneticRailgunItem
                     ? 1.0F
                     : 0.0F
            );
         }
      );
   }

   @SubscribeEvent
   public static void registerItemColors(net.minecraftforge.client.event.RegisterColorHandlersEvent.Item event) {
      event.register(
         (stack, tintIndex) -> AEColor.TRANSPARENT.getVariantByTintIndex(tintIndex) | 0xFF000000,
         new ItemLike[]{(ItemLike)ModItems.TIANSHU_PATTERN_ENCODING_TERMINAL.get()}
      );
      event.register(
         (stack, tintIndex) -> BasicStorageCell.getColor(stack, tintIndex) | 0xFF000000,
         new ItemLike[]{
            (ItemLike)ModItems.LIGHTNING_STORAGE_COMPONENT_I.get(),
            (ItemLike)ModItems.LIGHTNING_STORAGE_COMPONENT_II.get(),
            (ItemLike)ModItems.LIGHTNING_STORAGE_COMPONENT_III.get(),
            (ItemLike)ModItems.LIGHTNING_STORAGE_COMPONENT_IV.get(),
            (ItemLike)ModItems.LIGHTNING_STORAGE_COMPONENT_V.get(),
            (ItemLike)ModItems.PIGMEE_STORAGE_CELL.get(),
            (ItemLike)ModItems.VOID_CELL.get()
         }
      );
   }

   @SubscribeEvent
   public static void registerOverlays(RegisterGuiOverlaysEvent event) {
      event.registerAbove(VanillaGuiOverlay.ARMOR_LEVEL.id(), "celestweave_energy_level", CelestweaveArmorEnergyLevel.INSTANCE);
   }
}
