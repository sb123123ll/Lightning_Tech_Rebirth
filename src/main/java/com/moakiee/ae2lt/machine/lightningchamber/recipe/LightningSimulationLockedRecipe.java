package com.moakiee.ae2lt.machine.lightningchamber.recipe;

import com.moakiee.ae2lt.me.key.LightningKey;
import java.util.Arrays;
import java.util.Objects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class LightningSimulationLockedRecipe {
   private static final String TAG_RECIPE_ID = "RecipeId";
   private static final String TAG_RESULT = "Result";
   private static final String TAG_TOTAL_ENERGY = "TotalEnergy";
   private static final String TAG_LIGHTNING_COST = "LightningCost";
   private static final String TAG_LIGHTNING_TIER = "LightningTier";
   private static final String TAG_LEGACY_DUST_COST = "DustCost";
   private static final String TAG_INPUTS = "InputConsumptions";
   private final ResourceLocation recipeId;
   private final ItemStack result;
   private final long totalEnergy;
   private final int lightningCost;
   private final LightningKey.Tier lightningTier;
   private final int[] inputConsumptions;

   public LightningSimulationLockedRecipe(
      ResourceLocation recipeId, ItemStack result, long totalEnergy, int lightningCost, LightningKey.Tier lightningTier, int[] inputConsumptions
   ) {
      this.recipeId = Objects.requireNonNull(recipeId, "recipeId");
      this.result = Objects.requireNonNull(result, "result").m_41777_();
      this.totalEnergy = totalEnergy;
      this.lightningCost = lightningCost;
      this.lightningTier = Objects.requireNonNull(lightningTier, "lightningTier");
      if (result.m_41619_()) {
         throw new IllegalArgumentException("result cannot be empty");
      } else if (totalEnergy <= 0L) {
         throw new IllegalArgumentException("totalEnergy must be positive");
      } else if (lightningCost <= 0) {
         throw new IllegalArgumentException("lightningCost must be positive");
      } else if (inputConsumptions.length != 3) {
         throw new IllegalArgumentException("inputConsumptions must have length 3");
      } else {
         this.inputConsumptions = Arrays.copyOf(inputConsumptions, inputConsumptions.length);
      }
   }

   public static LightningSimulationLockedRecipe fromCandidate(LightningSimulationRecipeCandidate candidate) {
      LightningSimulationRecipe recipe = candidate.recipe();
      return new LightningSimulationLockedRecipe(
         recipe.m_6423_(), recipe.getResultStack(), recipe.totalEnergy(), recipe.lightningCost(), recipe.lightningTier(), candidate.match().inputConsumptions()
      );
   }

   public ResourceLocation recipeId() {
      return this.recipeId;
   }

   public ItemStack result() {
      return this.result.m_41777_();
   }

   public long totalEnergy() {
      return this.totalEnergy;
   }

   public int lightningCost() {
      return this.lightningCost;
   }

   public LightningKey.Tier lightningTier() {
      return this.lightningTier;
   }

   public int[] inputConsumptions() {
      return Arrays.copyOf(this.inputConsumptions, this.inputConsumptions.length);
   }

   public int inputConsumptionForSlot(int slot) {
      if (slot >= 0 && slot <= 2) {
         return this.inputConsumptions[slot];
      } else {
         throw new IllegalArgumentException("slot must be one of the three input slots");
      }
   }

   public CompoundTag toTag() {
      CompoundTag tag = new CompoundTag();
      tag.m_128359_("RecipeId", this.recipeId.toString());
      tag.m_128365_("Result", this.result.m_41739_(new CompoundTag()));
      tag.m_128356_("TotalEnergy", this.totalEnergy);
      tag.m_128405_("LightningCost", this.lightningCost);
      tag.m_128359_("LightningTier", this.lightningTier.m_7912_());
      tag.m_128365_("InputConsumptions", new IntArrayTag(Arrays.copyOf(this.inputConsumptions, this.inputConsumptions.length)));
      return tag;
   }

   @Nullable
   public static LightningSimulationLockedRecipe fromTag(CompoundTag tag) {
      if (tag.m_128441_("RecipeId") && tag.m_128425_("Result", 10)) {
         ItemStack result = ItemStack.m_41712_(tag.m_128469_("Result"));
         if (result.m_41619_()) {
            return null;
         } else {
            int[] inputConsumptions = tag.m_128465_("InputConsumptions");
            if (inputConsumptions.length != 3) {
               return null;
            } else {
               long totalEnergy = tag.m_128454_("TotalEnergy");
               int lightningCost = tag.m_128425_("LightningCost", 99) ? tag.m_128451_("LightningCost") : (tag.m_128451_("DustCost") > 0 ? 4 : 0);
               LightningKey.Tier lightningTier = tag.m_128425_("LightningTier", 8)
                  ? LightningKey.Tier.fromSerializedName(tag.m_128461_("LightningTier"))
                  : LightningSimulationRecipe.DEFAULT_LIGHTNING_TIER;
               return totalEnergy > 0L && lightningCost > 0
                  ? new LightningSimulationLockedRecipe(
                     ResourceLocation.m_135820_(tag.m_128461_("RecipeId")), result, totalEnergy, lightningCost, lightningTier, inputConsumptions
                  )
                  : null;
            }
         }
      } else {
         return null;
      }
   }
}
