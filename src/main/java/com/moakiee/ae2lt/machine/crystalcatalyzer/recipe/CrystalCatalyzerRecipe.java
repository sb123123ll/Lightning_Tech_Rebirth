package com.moakiee.ae2lt.machine.crystalcatalyzer.recipe;

import com.google.gson.JsonObject;
import com.moakiee.ae2lt.me.key.LightningKey;
import com.moakiee.ae2lt.registry.ModRecipeTypes;
import com.moakiee.ae2lt.util.RecipeSerializationHelper;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

public final class CrystalCatalyzerRecipe implements Recipe<CrystalCatalyzerRecipeInput> {
   public static final int MIN_ENERGY_PER_CYCLE = 1;
   public static final int DEFAULT_LIGHTNING_COST = 1;
   public static final LightningKey.Tier DEFAULT_LIGHTNING_TIER = LightningKey.Tier.HIGH_VOLTAGE;
   private final ResourceLocation id;
   private final Optional<Ingredient> catalyst;
   private final int catalystCount;
   private final CrystalCatalyzerOutput output;
   private final int energyPerCycle;
   private final int lightningCost;
   private final LightningKey.Tier lightningTier;
   private final Mode mode;

   public CrystalCatalyzerRecipe(ResourceLocation id, Optional<Ingredient> catalyst, int catalystCount, ItemStack output, int energyPerCycle) {
      this(id, catalyst, catalystCount, CrystalCatalyzerOutput.ofItem(output), energyPerCycle, 1, DEFAULT_LIGHTNING_TIER, Mode.CRYSTAL);
   }

   public CrystalCatalyzerRecipe(
      ResourceLocation id,
      Optional<Ingredient> catalyst,
      int catalystCount,
      CrystalCatalyzerOutput output,
      int energyPerCycle,
      int lightningCost,
      LightningKey.Tier lightningTier,
      Mode mode
   ) {
      this.id = Objects.requireNonNull(id, "id");
      this.catalyst = Objects.requireNonNull(catalyst, "catalyst");
      this.catalystCount = catalystCount;
      this.output = Objects.requireNonNull(output, "output");
      this.energyPerCycle = energyPerCycle;
      this.lightningCost = lightningCost;
      this.lightningTier = Objects.requireNonNull(lightningTier, "lightningTier");
      this.mode = Objects.requireNonNull(mode, "mode");
      if (catalyst.isPresent() && catalystCount <= 0) {
         throw new IllegalArgumentException("catalystCount must be positive when catalyst is present");
      } else if (energyPerCycle < 1) {
         throw new IllegalArgumentException("energyPerCycle must be at least 1");
      } else if (lightningCost < 1) {
         throw new IllegalArgumentException("lightningCost must be at least 1");
      }
   }

   public Optional<Ingredient> catalyst() {
      return this.catalyst;
   }

   public int catalystCount() {
      return this.catalystCount;
   }

   public ItemStack getOutputTemplate() {
      return this.output.resolve();
   }

   public CrystalCatalyzerOutput outputSpec() {
      return this.output;
   }

   public int energyPerCycle() {
      return this.energyPerCycle;
   }

   public int lightningCost() {
      return this.lightningCost;
   }

   public LightningKey.Tier lightningTier() {
      return this.lightningTier;
   }

   public Mode mode() {
      return this.mode;
   }

   public ResourceLocation m_6423_() {
      return this.id;
   }

   public boolean catalystMatches(ItemStack stack) {
      if (this.catalyst.isEmpty()) {
         return stack.m_41619_();
      } else {
         return !stack.m_41619_() && this.catalyst.get().test(stack) ? stack.m_41613_() >= this.catalystCount : false;
      }
   }

   public boolean matches(CrystalCatalyzerRecipeInput input, Level level) {
      return this.catalystMatches(input.catalyst());
   }

   public ItemStack assemble(CrystalCatalyzerRecipeInput input, RegistryAccess registries) {
      return this.output.resolve();
   }

   public boolean m_8004_(int width, int height) {
      return true;
   }

   public ItemStack m_8043_(RegistryAccess registries) {
      return this.output.resolve();
   }

   public NonNullList<Ingredient> m_7527_() {
      NonNullList<Ingredient> list = NonNullList.m_122779_();
      this.catalyst.ifPresent(list::add);
      return list;
   }

   public RecipeSerializer<?> m_7707_() {
      return (RecipeSerializer<?>)ModRecipeTypes.CRYSTAL_CATALYZER_SERIALIZER.get();
   }

   public RecipeType<?> m_6671_() {
      return (RecipeType<?>)ModRecipeTypes.CRYSTAL_CATALYZER_TYPE.get();
   }

   public boolean m_5598_() {
      return true;
   }

   public boolean m_142505_() {
      return this.output.resolve().m_41619_() || this.energyPerCycle < 1 || this.lightningCost < 1 || this.catalyst.isPresent() && this.catalystCount <= 0;
   }

   public static final class Serializer implements RecipeSerializer<CrystalCatalyzerRecipe> {
      public CrystalCatalyzerRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
         Optional<Ingredient> catalyst = json.has("catalyst") ? Optional.of(Ingredient.m_43917_(json.get("catalyst"))) : Optional.empty();
         int catalystCount = GsonHelper.m_13824_(json, "catalystCount", 0);
         int energyPerCycle = GsonHelper.m_13927_(json, "energyPerCycle");
         int lightningCost = GsonHelper.m_13824_(json, "lightningCost", 1);
         LightningKey.Tier lightningTier = RecipeSerializationHelper.enumFromJson(
            json, "lightningTier", CrystalCatalyzerRecipe.DEFAULT_LIGHTNING_TIER, LightningKey.Tier.values()
         );
         Mode mode = RecipeSerializationHelper.enumFromJson(json, "mode", Mode.CRYSTAL, Mode.values());
         return new CrystalCatalyzerRecipe(
            recipeId,
            catalyst,
            catalystCount,
            CrystalCatalyzerOutput.fromJson(GsonHelper.m_13930_(json, "output")),
            energyPerCycle,
            lightningCost,
            lightningTier,
            mode
         );
      }

      public CrystalCatalyzerRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
         Optional<Ingredient> catalyst = buffer.readBoolean() ? Optional.of(Ingredient.m_43940_(buffer)) : Optional.empty();
         return new CrystalCatalyzerRecipe(
            recipeId,
            catalyst,
            buffer.readInt(),
            CrystalCatalyzerOutput.decode(buffer),
            buffer.readInt(),
            buffer.readInt(),
            (LightningKey.Tier)buffer.m_130066_(LightningKey.Tier.class),
            (Mode)buffer.m_130066_(Mode.class)
         );
      }

      public void toNetwork(FriendlyByteBuf buffer, CrystalCatalyzerRecipe recipe) {
         buffer.writeBoolean(recipe.catalyst().isPresent());
         recipe.catalyst().ifPresent(ingredient -> ingredient.m_43923_(buffer));
         buffer.writeInt(recipe.catalystCount());
         CrystalCatalyzerOutput.encode(buffer, recipe.outputSpec());
         buffer.writeInt(recipe.energyPerCycle());
         buffer.writeInt(recipe.lightningCost());
         buffer.m_130068_(recipe.lightningTier());
         buffer.m_130068_(recipe.mode());
      }
   }
}
