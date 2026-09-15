package com.moakiee.ae2lt.machine.lightningchamber.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.moakiee.ae2lt.me.key.LightningKey;
import com.moakiee.ae2lt.registry.ModRecipeTypes;
import com.moakiee.ae2lt.util.RecipeSerializationHelper;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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

public final class LightningSimulationRecipe implements Recipe<LightningSimulationRecipeInput> {
   public static final long MIN_TOTAL_ENERGY = 5L;
   public static final int DEFAULT_LIGHTNING_COST = 4;
   public static final LightningKey.Tier DEFAULT_LIGHTNING_TIER = LightningKey.Tier.HIGH_VOLTAGE;
   private final ResourceLocation id;
   private final int priority;
   private final List<LightningSimulationIngredient> inputs;
   private final ItemStack result;
   private final long totalEnergy;
   private final int lightningCost;
   private final LightningKey.Tier lightningTier;
   private final int totalInputCount;

   public LightningSimulationRecipe(
      ResourceLocation id,
      int priority,
      List<LightningSimulationIngredient> inputs,
      ItemStack result,
      long totalEnergy,
      int lightningCost,
      LightningKey.Tier lightningTier
   ) {
      this.id = Objects.requireNonNull(id, "id");
      Objects.requireNonNull(inputs, "inputs");
      Objects.requireNonNull(result, "result");
      Objects.requireNonNull(lightningTier, "lightningTier");
      if (inputs.isEmpty() || inputs.size() > 3) {
         throw new IllegalArgumentException("inputs must contain 1 to 3 entries");
      } else if (result.m_41619_()) {
         throw new IllegalArgumentException("result cannot be empty");
      } else if (totalEnergy < 5L) {
         throw new IllegalArgumentException("totalEnergy must be at least 5");
      } else if (lightningCost <= 0) {
         throw new IllegalArgumentException("lightningCost must be positive");
      } else {
         this.priority = priority;
         this.inputs = List.copyOf(inputs);
         this.result = result.m_41777_();
         this.totalEnergy = totalEnergy;
         this.lightningCost = lightningCost;
         this.lightningTier = lightningTier;
         this.totalInputCount = this.inputs.stream().mapToInt(LightningSimulationIngredient::count).sum();
      }
   }

   public int priority() {
      return this.priority;
   }

   public List<LightningSimulationIngredient> inputs() {
      return this.inputs;
   }

   public ItemStack getResultStack() {
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

   public int totalInputCount() {
      return this.totalInputCount;
   }

   public ResourceLocation m_6423_() {
      return this.id;
   }

   public boolean matches(LightningSimulationRecipeInput input, Level level) {
      return this.planMatch(input).isPresent();
   }

   public Optional<LightningSimulationRecipeMatch> planMatch(LightningSimulationRecipeInput input) {
      List<LightningSimulationRecipeInput.SlotStack> slotStacks = input.slotStacks();
      if (!slotStacks.isEmpty() && slotStacks.size() <= 3) {
         int[] slotFlexibility = new int[slotStacks.size()];
         List<List<Integer>> rawMatches = new ArrayList<>(this.inputs.size());

         for (LightningSimulationIngredient requirement : this.inputs) {
            List<Integer> matchingSlots = new ArrayList<>();
            int availableCount = 0;

            for (int slotIndex = 0; slotIndex < slotStacks.size(); slotIndex++) {
               LightningSimulationRecipeInput.SlotStack slotStack = slotStacks.get(slotIndex);
               if (requirement.ingredient().test(slotStack.stack())) {
                  matchingSlots.add(slotIndex);
                  availableCount += slotStack.stack().m_41613_();
                  slotFlexibility[slotIndex]++;
               }
            }

            if (availableCount < requirement.count()) {
               return Optional.empty();
            }

            rawMatches.add(matchingSlots);
         }

         List<LightningSimulationRecipe.RequirementState> requirements = new ArrayList<>(this.inputs.size());

         for (int requirementIndex = 0; requirementIndex < this.inputs.size(); requirementIndex++) {
            LightningSimulationIngredient requirement = this.inputs.get(requirementIndex);
            List<Integer> matchingSlots = rawMatches.get(requirementIndex);
            matchingSlots.sort(
               Comparator.<Integer>comparingInt(slotIndexx -> slotFlexibility[slotIndexx])
                  .thenComparing(Comparator.<Integer>comparingInt(slotIndexx -> slotStacks.get(slotIndexx).stack().m_41613_()).reversed())
            );
            requirements.add(new LightningSimulationRecipe.RequirementState(requirement.count(), matchingSlots.stream().mapToInt(Integer::intValue).toArray()));
         }

         requirements.sort(
            Comparator.comparingInt(LightningSimulationRecipe.RequirementState::matchingSlotCount)
               .thenComparing(Comparator.comparingInt(LightningSimulationRecipe.RequirementState::count).reversed())
         );
         int[] remainingCounts = slotStacks.stream().mapToInt(slotStackx -> slotStackx.stack().m_41613_()).toArray();
         int[] slotConsumptions = new int[3];
         return !this.allocateRequirement(0, requirements, slotStacks, remainingCounts, slotConsumptions)
            ? Optional.empty()
            : Optional.of(new LightningSimulationRecipeMatch(slotConsumptions));
      } else {
         return Optional.empty();
      }
   }

   public ItemStack assemble(LightningSimulationRecipeInput input, RegistryAccess registries) {
      return this.result.m_41777_();
   }

   public boolean m_8004_(int width, int height) {
      return true;
   }

   public ItemStack m_8043_(RegistryAccess registries) {
      return this.result.m_41777_();
   }

   public NonNullList<Ingredient> m_7527_() {
      NonNullList<Ingredient> ingredients = NonNullList.m_122779_();

      for (LightningSimulationIngredient input : this.inputs) {
         ingredients.add(input.ingredient());
      }

      return ingredients;
   }

   public RecipeSerializer<?> m_7707_() {
      return (RecipeSerializer<?>)ModRecipeTypes.LIGHTNING_SIMULATION_SERIALIZER.get();
   }

   public RecipeType<?> m_6671_() {
      return (RecipeType<?>)ModRecipeTypes.LIGHTNING_SIMULATION_TYPE.get();
   }

   public boolean m_5598_() {
      return true;
   }

   public boolean m_142505_() {
      return this.inputs.isEmpty()
         || this.result.m_41619_()
         || this.totalEnergy < 5L
         || this.lightningCost <= 0
         || this.inputs.stream().anyMatch(input -> input.ingredient().m_43908_().length == 0);
   }

   private ItemStack rawResult() {
      return this.result;
   }

   private boolean allocateRequirement(
      int requirementIndex,
      List<LightningSimulationRecipe.RequirementState> requirements,
      List<LightningSimulationRecipeInput.SlotStack> slotStacks,
      int[] remainingCounts,
      int[] slotConsumptions
   ) {
      if (requirementIndex >= requirements.size()) {
         return true;
      } else {
         LightningSimulationRecipe.RequirementState requirement = requirements.get(requirementIndex);
         return this.allocateAcrossSlots(requirementIndex, requirements, requirement, slotStacks, 0, requirement.count(), remainingCounts, slotConsumptions);
      }
   }

   private boolean allocateAcrossSlots(
      int requirementIndex,
      List<LightningSimulationRecipe.RequirementState> requirements,
      LightningSimulationRecipe.RequirementState requirement,
      List<LightningSimulationRecipeInput.SlotStack> slotStacks,
      int slotCursor,
      int needed,
      int[] remainingCounts,
      int[] slotConsumptions
   ) {
      if (needed == 0) {
         return this.allocateRequirement(requirementIndex + 1, requirements, slotStacks, remainingCounts, slotConsumptions);
      } else if (slotCursor >= requirement.matchingSlots.length) {
         return false;
      } else if (this.remainingCapacity(requirement.matchingSlots, slotCursor, remainingCounts) < needed) {
         return false;
      } else {
         int slotIndex = requirement.matchingSlots[slotCursor];
         int maxTake = Math.min(needed, remainingCounts[slotIndex]);
         int machineSlot = slotStacks.get(slotIndex).slot();

         for (int take = maxTake; take >= 0; take--) {
            if (take > 0) {
               remainingCounts[slotIndex] -= take;
               slotConsumptions[machineSlot] += take;
            }

            if (this.allocateAcrossSlots(
               requirementIndex, requirements, requirement, slotStacks, slotCursor + 1, needed - take, remainingCounts, slotConsumptions
            )) {
               return true;
            }

            if (take > 0) {
               slotConsumptions[machineSlot] -= take;
               remainingCounts[slotIndex] += take;
            }
         }

         return false;
      }
   }

   private int remainingCapacity(int[] matchingSlots, int startIndex, int[] remainingCounts) {
      int total = 0;

      for (int index = startIndex; index < matchingSlots.length; index++) {
         total += remainingCounts[matchingSlots[index]];
      }

      return total;
   }

   private static final class RequirementState {
      private final int count;
      private final int[] matchingSlots;

      private RequirementState(int count, int[] matchingSlots) {
         this.count = count;
         this.matchingSlots = matchingSlots;
      }

      private int count() {
         return this.count;
      }

      private int matchingSlotCount() {
         return this.matchingSlots.length;
      }
   }

   public static final class Serializer implements RecipeSerializer<LightningSimulationRecipe> {
      public LightningSimulationRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
         JsonArray inputsJson = GsonHelper.m_13933_(json, "inputs");
         List<LightningSimulationIngredient> inputs = new ArrayList<>(inputsJson.size());

         for (JsonElement element : inputsJson) {
            inputs.add(LightningSimulationIngredient.fromJson(GsonHelper.m_13918_(element, "inputs[]")));
         }

         return new LightningSimulationRecipe(
            recipeId,
            GsonHelper.m_13824_(json, "priority", 0),
            inputs,
            RecipeSerializationHelper.itemStackFromJson(json, "result"),
            GsonHelper.m_13921_(json, "totalEnergy"),
            GsonHelper.m_13824_(json, "lightningCost", 4),
            RecipeSerializationHelper.enumFromJson(json, "lightningTier", LightningSimulationRecipe.DEFAULT_LIGHTNING_TIER, LightningKey.Tier.values())
         );
      }

      public LightningSimulationRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
         int inputCount = buffer.readInt();
         List<LightningSimulationIngredient> inputs = new ArrayList<>(inputCount);

         for (int i = 0; i < inputCount; i++) {
            inputs.add(LightningSimulationIngredient.fromNetwork(buffer));
         }

         return new LightningSimulationRecipe(
            recipeId,
            buffer.readInt(),
            inputs,
            buffer.m_130267_(),
            buffer.readLong(),
            buffer.readInt(),
            (LightningKey.Tier)buffer.m_130066_(LightningKey.Tier.class)
         );
      }

      public void toNetwork(FriendlyByteBuf buffer, LightningSimulationRecipe recipe) {
         buffer.writeInt(recipe.inputs().size());

         for (LightningSimulationIngredient input : recipe.inputs()) {
            input.toNetwork(buffer);
         }

         buffer.writeInt(recipe.priority());
         buffer.m_130055_(recipe.rawResult());
         buffer.writeLong(recipe.totalEnergy());
         buffer.writeInt(recipe.lightningCost());
         buffer.m_130068_(recipe.lightningTier());
      }
   }
}
