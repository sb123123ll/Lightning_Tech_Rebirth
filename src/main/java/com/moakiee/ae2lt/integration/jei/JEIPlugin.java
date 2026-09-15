package com.moakiee.ae2lt.integration.jei;

import com.moakiee.ae2lt.client.CrystalCatalyzerScreen;
import com.moakiee.ae2lt.client.LightningAssemblyChamberScreen;
import com.moakiee.ae2lt.client.LightningSimulationChamberScreen;
import com.moakiee.ae2lt.client.OverloadProcessingFactoryScreen;
import com.moakiee.ae2lt.client.TeslaCoilScreen;
import com.moakiee.ae2lt.integration.ae2wtlib.TianshuWirelessTerminalFactory;
import com.moakiee.ae2lt.integration.jei.category.CrystalCatalyzerCategory;
import com.moakiee.ae2lt.integration.jei.category.FirmamentConversionCategory;
import com.moakiee.ae2lt.integration.jei.category.LightningAssemblyCategory;
import com.moakiee.ae2lt.integration.jei.category.LightningSimulationCategory;
import com.moakiee.ae2lt.integration.jei.category.LightningStrikeCategory;
import com.moakiee.ae2lt.integration.jei.category.LightningTransformCategory;
import com.moakiee.ae2lt.integration.jei.category.MultiblockStructureCategory;
import com.moakiee.ae2lt.integration.jei.category.OverloadGrowthCategory;
import com.moakiee.ae2lt.integration.jei.category.OverloadProcessingCategory;
import com.moakiee.ae2lt.integration.jei.category.TeslaCoilCategory;
import com.moakiee.ae2lt.integration.jei.compat.ae2jeiintegration.AE2JeiIntegrationCompat;
import com.moakiee.ae2lt.integration.recipeviewer.multiblock.MultiblockStructureRecipes;
import com.moakiee.ae2lt.menu.TianshuPatternEncodingTermMenu;
import com.moakiee.ae2lt.menu.TianshuWirelessPatternEncodingTermMenu;
import com.moakiee.ae2lt.registry.ModBlocks;
import com.moakiee.ae2lt.registry.ModItems;
import com.moakiee.ae2lt.registry.ModRecipeTypes;
import com.moakiee.ae2lt.util.RecipeManagerByTypeAccess;
import java.util.Collection;
import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.handlers.IGuiClickableArea;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IModIngredientRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.fml.ModList;

@JeiPlugin
public class JEIPlugin implements IModPlugin {
   private static final ResourceLocation ID = new ResourceLocation("ae2lt", "jei_plugin");
   private static final String EMI_MODID = "emi";

   public JEIPlugin() {
      AE2JeiIntegrationCompat.registerConverter();
   }

   public ResourceLocation getPluginUid() {
      return ID;
   }

   public void registerIngredients(IModIngredientRegistration registration) {
      registration.register(LightningJeiIngredients.TYPE, LightningJeiIngredients.INGREDIENTS, LightningJeiIngredients.HELPER, LightningJeiIngredients.RENDERER);
   }

   public void registerCategories(IRecipeCategoryRegistration registration) {
      IGuiHelper guiHelper = registration.getJeiHelpers().getGuiHelper();
      registration.addRecipeCategories(
         new IRecipeCategory[]{
            new OverloadGrowthCategory(guiHelper),
            new LightningAssemblyCategory(guiHelper),
            new LightningSimulationCategory(guiHelper),
            new LightningTransformCategory(guiHelper),
            new LightningStrikeCategory(guiHelper),
            new OverloadProcessingCategory(guiHelper),
            new TeslaCoilCategory(guiHelper),
            new CrystalCatalyzerCategory(guiHelper),
            new FirmamentConversionCategory(guiHelper)
         }
      );
      if (!isEmiLoaded()) {
         registration.addRecipeCategories(new IRecipeCategory[]{new MultiblockStructureCategory(guiHelper)});
      }
   }

   public void registerRecipes(IRecipeRegistration registration) {
      registration.addRecipes(OverloadGrowthCategory.TYPE, List.of(OverloadGrowthCategory.Page.values()));
      registration.addRecipes(TeslaCoilCategory.TYPE, List.of(TeslaCoilCategory.Page.values()));
      if (!isEmiLoaded()) {
         registration.addRecipes(MultiblockStructureCategory.TYPE, MultiblockStructureRecipes.all());
      }

      registration.addIngredientInfo((ItemLike)ModItems.PIGMEE_CORE.get(), new Component[]{Component.m_237115_("jei.ae2lt.pigmee_core.info")});
      ClientLevel level = Minecraft.m_91087_().f_91073_;
      if (level != null) {
         registration.addRecipes(
            CrystalCatalyzerCategory.TYPE,
            RecipeManagerByTypeAccess.byType(level.m_7465_(), (net.minecraft.world.item.crafting.RecipeType)ModRecipeTypes.CRYSTAL_CATALYZER_TYPE.get())
               .values()
               .stream()
               .filter(recipe -> !recipe.getOutputTemplate().m_41619_())
               .toList()
         );
         registration.addRecipes(
            LightningAssemblyCategory.TYPE,
            RecipeManagerByTypeAccess.byType(level.m_7465_(), (net.minecraft.world.item.crafting.RecipeType)ModRecipeTypes.LIGHTNING_ASSEMBLY_TYPE.get())
               .values()
               .stream()
               .toList()
         );
         registration.addRecipes(
            LightningSimulationCategory.TYPE,
            RecipeManagerByTypeAccess.byType(level.m_7465_(), (net.minecraft.world.item.crafting.RecipeType)ModRecipeTypes.LIGHTNING_SIMULATION_TYPE.get())
               .values()
               .stream()
               .toList()
         );
         registration.addRecipes(
            LightningTransformCategory.TYPE,
            RecipeManagerByTypeAccess.byType(level.m_7465_(), (net.minecraft.world.item.crafting.RecipeType)ModRecipeTypes.LIGHTNING_TRANSFORM_TYPE.get())
               .values()
               .stream()
               .toList()
         );
         registration.addRecipes(
            LightningStrikeCategory.TYPE,
            RecipeManagerByTypeAccess.byType(level.m_7465_(), (net.minecraft.world.item.crafting.RecipeType)ModRecipeTypes.LIGHTNING_STRIKE_TYPE.get())
               .values()
               .stream()
               .toList()
         );
         registration.addRecipes(
            OverloadProcessingCategory.TYPE,
            RecipeManagerByTypeAccess.byType(level.m_7465_(), (net.minecraft.world.item.crafting.RecipeType)ModRecipeTypes.OVERLOAD_PROCESSING_TYPE.get())
               .values()
               .stream()
               .toList()
         );
         registration.addRecipes(
            FirmamentConversionCategory.TYPE,
            RecipeManagerByTypeAccess.byType(level.m_7465_(), (net.minecraft.world.item.crafting.RecipeType)ModRecipeTypes.FIRMAMENT_CONVERSION_TYPE.get())
               .values()
               .stream()
               .toList()
         );
      }
   }

   public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
      registration.addRecipeCatalyst(new ItemStack((ItemLike)ModBlocks.LIGHTNING_ASSEMBLY_CHAMBER.get()), new RecipeType[]{LightningAssemblyCategory.TYPE});
      registration.addRecipeCatalyst(new ItemStack((ItemLike)ModBlocks.LIGHTNING_SIMULATION_CHAMBER.get()), new RecipeType[]{LightningSimulationCategory.TYPE});
      registration.addRecipeCatalyst(new ItemStack((ItemLike)ModBlocks.OVERLOAD_PROCESSING_FACTORY.get()), new RecipeType[]{OverloadProcessingCategory.TYPE});
      registration.addRecipeCatalyst(new ItemStack((ItemLike)ModBlocks.TESLA_COIL.get()), new RecipeType[]{TeslaCoilCategory.TYPE});
      registration.addRecipeCatalyst(new ItemStack((ItemLike)ModBlocks.CRYSTAL_CATALYZER.get()), new RecipeType[]{CrystalCatalyzerCategory.TYPE});
      registration.addRecipeCatalyst(new ItemStack((ItemLike)ModBlocks.FIRMAMENT_CONVERSION_CORE.get()), new RecipeType[]{FirmamentConversionCategory.TYPE});
      if (!isEmiLoaded()) {
         registration.addRecipeCatalyst(
            new ItemStack((ItemLike)ModBlocks.MATTER_WARPING_MATRIX_CONTROLLER.get()), new RecipeType[]{MultiblockStructureCategory.TYPE}
         );
         registration.addRecipeCatalyst(
            new ItemStack((ItemLike)ModBlocks.TIANSHU_SUPERCOMPUTER_CONTROLLER.get()), new RecipeType[]{MultiblockStructureCategory.TYPE}
         );
      }
   }

   public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
      IRecipeTransferHandlerHelper helper = registration.getTransferHelper();
      registration.addUniversalRecipeTransferHandler(
         new UniversalEncodePatternTransferHandler<>(TianshuPatternEncodingTermMenu.TYPE, TianshuPatternEncodingTermMenu.class, helper)
      );
      if (TianshuWirelessTerminalFactory.isAvailable()) {
         registration.addUniversalRecipeTransferHandler(
            new UniversalEncodePatternTransferHandler<>(TianshuWirelessPatternEncodingTermMenu.TYPE, TianshuWirelessPatternEncodingTermMenu.class, helper)
         );
      }
   }

   public void registerGuiHandlers(IGuiHandlerRegistration registration) {
      registration.addGuiContainerHandler(LightningAssemblyChamberScreen.class, clickableAreaHandler(83, 22, 42, 46, LightningAssemblyCategory.TYPE));
      registration.addGuiContainerHandler(LightningSimulationChamberScreen.class, clickableAreaHandler(82, 25, 35, 46, LightningSimulationCategory.TYPE));
      registration.addGuiContainerHandler(OverloadProcessingFactoryScreen.class, clickableAreaHandler(84, 46, 31, 10, OverloadProcessingCategory.TYPE));
      registration.addGuiContainerHandler(TeslaCoilScreen.class, clickableAreaHandler(43, 22, 36, 40, TeslaCoilCategory.TYPE));
      registration.addGuiContainerHandler(CrystalCatalyzerScreen.class, clickableAreaHandler(74, 33, 35, 10, CrystalCatalyzerCategory.TYPE));
   }

   private static <T extends AbstractContainerScreen<?>> IGuiContainerHandler<T> clickableAreaHandler(
      final int x, final int y, final int width, final int height, final RecipeType<?> recipeType
   ) {
      return new IGuiContainerHandler<T>() {
         public Collection<IGuiClickableArea> getGuiClickableAreas(T screen, double mouseX, double mouseY) {
            return List.of(IGuiClickableArea.createBasic(x, y, width, height, new RecipeType[]{recipeType}));
         }
      };
   }

   private static boolean isEmiLoaded() {
      return ModList.get().isLoaded("emi");
   }
}
