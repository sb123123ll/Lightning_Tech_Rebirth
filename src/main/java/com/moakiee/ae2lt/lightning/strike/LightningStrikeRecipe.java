package com.moakiee.ae2lt.lightning.strike;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.moakiee.ae2lt.registry.ModRecipeTypes;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

public final class LightningStrikeRecipe implements Recipe<LightningStrikeRecipeInput> {
   private final ResourceLocation id;
   private final boolean requiresNaturalLightning;
   private final Block centerInput;
   private final Block centerOutput;
   private final List<StructureRequirement> requirements;

   public LightningStrikeRecipe(
      ResourceLocation id, boolean requiresNaturalLightning, Block centerInput, Block centerOutput, List<StructureRequirement> requirements
   ) {
      this.id = Objects.requireNonNull(id, "id");
      this.requiresNaturalLightning = requiresNaturalLightning;
      this.centerInput = Objects.requireNonNull(centerInput, "centerInput");
      this.centerOutput = Objects.requireNonNull(centerOutput, "centerOutput");
      this.requirements = List.copyOf(Objects.requireNonNull(requirements, "requirements"));
   }

   public boolean requiresNaturalLightning() {
      return this.requiresNaturalLightning;
   }

   public Block centerInput() {
      return this.centerInput;
   }

   public Block centerOutput() {
      return this.centerOutput;
   }

   public List<StructureRequirement> requirements() {
      return this.requirements;
   }

   public ResourceLocation m_6423_() {
      return this.id;
   }

   public boolean matches(LightningStrikeRecipeInput input, Level level) {
      return false;
   }

   public ItemStack assemble(LightningStrikeRecipeInput input, RegistryAccess registries) {
      return new ItemStack(this.centerOutput);
   }

   public boolean m_8004_(int width, int height) {
      return true;
   }

   public ItemStack m_8043_(RegistryAccess registries) {
      return new ItemStack(this.centerOutput);
   }

   public NonNullList<Ingredient> m_7527_() {
      return NonNullList.m_122779_();
   }

   public RecipeSerializer<?> m_7707_() {
      return (RecipeSerializer<?>)ModRecipeTypes.LIGHTNING_STRIKE_SERIALIZER.get();
   }

   public RecipeType<?> m_6671_() {
      return (RecipeType<?>)ModRecipeTypes.LIGHTNING_STRIKE_TYPE.get();
   }

   public boolean m_5598_() {
      return true;
   }

   public static final class Serializer implements RecipeSerializer<LightningStrikeRecipe> {
      public LightningStrikeRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
         JsonArray requirementsJson = GsonHelper.m_13933_(json, "requirements");
         List<StructureRequirement> requirements = new ArrayList<>(requirementsJson.size());

         for (JsonElement element : requirementsJson) {
            requirements.add(StructureRequirement.fromJson(GsonHelper.m_13918_(element, "requirements[]")));
         }

         return new LightningStrikeRecipe(
            recipeId,
            GsonHelper.m_13855_(json, "requires_natural_lightning", false),
            (Block)BuiltInRegistries.f_256975_
               .m_6612_(ResourceLocation.m_135820_(GsonHelper.m_13906_(json, "center_input")))
               .orElseThrow(() -> new IllegalArgumentException("Unknown block id for center_input")),
            (Block)BuiltInRegistries.f_256975_
               .m_6612_(ResourceLocation.m_135820_(GsonHelper.m_13906_(json, "center_output")))
               .orElseThrow(() -> new IllegalArgumentException("Unknown block id for center_output")),
            requirements
         );
      }

      public LightningStrikeRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
         boolean requiresNatural = buffer.readBoolean();
         Block centerInput = (Block)BuiltInRegistries.f_256975_
            .m_6612_(buffer.m_130281_())
            .orElseThrow(() -> new IllegalStateException("Received unknown center_input block id"));
         Block centerOutput = (Block)BuiltInRegistries.f_256975_
            .m_6612_(buffer.m_130281_())
            .orElseThrow(() -> new IllegalStateException("Received unknown center_output block id"));
         int requirementCount = buffer.readInt();
         List<StructureRequirement> requirements = new ArrayList<>(requirementCount);

         for (int i = 0; i < requirementCount; i++) {
            requirements.add(StructureRequirement.fromNetwork(buffer));
         }

         return new LightningStrikeRecipe(recipeId, requiresNatural, centerInput, centerOutput, requirements);
      }

      public void toNetwork(FriendlyByteBuf buffer, LightningStrikeRecipe recipe) {
         buffer.writeBoolean(recipe.requiresNaturalLightning());
         buffer.m_130085_(BuiltInRegistries.f_256975_.m_7981_(recipe.centerInput()));
         buffer.m_130085_(BuiltInRegistries.f_256975_.m_7981_(recipe.centerOutput()));
         buffer.writeInt(recipe.requirements().size());

         for (StructureRequirement requirement : recipe.requirements()) {
            requirement.toNetwork(buffer);
         }
      }
   }
}
