package com.moakiee.ae2lt.machine.overloadfactory.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.moakiee.ae2lt.logic.FluidStackHelper;
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
import net.minecraftforge.fluids.FluidStack;

public final class OverloadProcessingRecipe implements Recipe<OverloadProcessingRecipeInput> {
   public static final long MIN_TOTAL_ENERGY = 5L;
   public static final int DEFAULT_LIGHTNING_COST = 4;
   public static final LightningKey.Tier DEFAULT_LIGHTNING_TIER = LightningKey.Tier.HIGH_VOLTAGE;
   private final ResourceLocation id;
   private final int priority;
   private final List<OverloadProcessingIngredient> itemInputs;
   private final FluidStack fluidInput;
   private final List<ItemStack> itemResults;
   private final FluidStack fluidResult;
   private final long totalEnergy;
   private final int lightningCost;
   private final LightningKey.Tier lightningTier;
   private final int totalInputCount;

   public OverloadProcessingRecipe(
      ResourceLocation id,
      int priority,
      List<OverloadProcessingIngredient> itemInputs,
      FluidStack fluidInput,
      List<ItemStack> itemResults,
      FluidStack fluidResult,
      long totalEnergy,
      int lightningCost,
      LightningKey.Tier lightningTier
   ) {
      this.id = Objects.requireNonNull(id, "id");
      Objects.requireNonNull(itemInputs, "itemInputs");
      Objects.requireNonNull(fluidInput, "fluidInput");
      Objects.requireNonNull(itemResults, "itemResults");
      Objects.requireNonNull(fluidResult, "fluidResult");
      Objects.requireNonNull(lightningTier, "lightningTier");
      if (itemInputs.size() > 9) {
         throw new IllegalArgumentException("itemInputs must contain at most 9 entries");
      } else if (itemResults.size() > 1) {
         throw new IllegalArgumentException("itemResults must contain at most 1 entry");
      } else if (itemInputs.isEmpty() && fluidInput.isEmpty()) {
         throw new IllegalArgumentException("recipe must define at least one item or fluid input");
      } else if (itemResults.isEmpty() && fluidResult.isEmpty()) {
         throw new IllegalArgumentException("recipe must define at least one item or fluid output");
      } else if (itemResults.stream().anyMatch(ItemStack::m_41619_)) {
         throw new IllegalArgumentException("itemResults cannot contain empty stacks");
      } else if (totalEnergy < 5L) {
         throw new IllegalArgumentException("totalEnergy must be at least 5");
      } else if (lightningCost <= 0) {
         throw new IllegalArgumentException("lightningCost must be positive");
      } else {
         this.priority = priority;
         this.itemInputs = List.copyOf(itemInputs);
         this.fluidInput = fluidInput.copy();
         this.itemResults = itemResults.stream().<ItemStack>map(ItemStack::m_41777_).toList();
         this.fluidResult = fluidResult.copy();
         this.totalEnergy = totalEnergy;
         this.lightningCost = lightningCost;
         this.lightningTier = lightningTier;
         this.totalInputCount = this.itemInputs.stream().mapToInt(OverloadProcessingIngredient::count).sum();
      }
   }

   public int priority() {
      return this.priority;
   }

   public List<OverloadProcessingIngredient> itemInputs() {
      return this.itemInputs;
   }

   public FluidStack fluidInput() {
      return this.fluidInput.copy();
   }

   public List<ItemStack> itemResults() {
      return this.itemResults.stream().<ItemStack>map(ItemStack::m_41777_).toList();
   }

   public FluidStack fluidResult() {
      return this.fluidResult.copy();
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

   public boolean matches(OverloadProcessingRecipeInput input, Level level) {
      return this.planMatch(input, 1).isPresent() && this.hasRequiredFluid(input.inputFluid(), 1);
   }

   public Optional<OverloadProcessingRecipeMatch> planMatch(OverloadProcessingRecipeInput input, int operations) {
      return operations > 0 && input != null ? this.prepareMatch(input).flatMap(plan -> plan.allocate(operations)) : Optional.empty();
   }

   public Optional<OverloadProcessingRecipe.MatchPlan> prepareMatch(OverloadProcessingRecipeInput input) {
      if (input == null) {
         return Optional.empty();
      } else {
         List<OverloadProcessingRecipeInput.SlotStack> slotStacks = input.slotStacks();
         if (this.itemInputs.isEmpty()) {
            return !slotStacks.isEmpty() ? Optional.empty() : Optional.of(new OverloadProcessingRecipe.MatchPlan(slotStacks, List.of()));
         } else if (!slotStacks.isEmpty() && slotStacks.size() <= 9) {
            List<OverloadProcessingRecipe.PreparedRequirement> requirements = new ArrayList<>(this.itemInputs.size());

            for (OverloadProcessingIngredient requirement : this.itemInputs) {
               List<Integer> matchingSlots = new ArrayList<>();
               long availableCount = 0L;

               for (int slotIndex = 0; slotIndex < slotStacks.size(); slotIndex++) {
                  OverloadProcessingRecipeInput.SlotStack slotStack = slotStacks.get(slotIndex);
                  if (requirement.ingredient().test(slotStack.stack())) {
                     matchingSlots.add(slotIndex);
                     availableCount += (long)slotStack.stack().m_41613_();
                  }
               }

               if (availableCount < (long)requirement.count()) {
                  return Optional.empty();
               }

               matchingSlots.sort(Comparator.comparingInt(slotIndexx -> slotStacks.get(slotIndexx).slot()));
               int[] matchingSlotArray = new int[matchingSlots.size()];

               for (int index = 0; index < matchingSlots.size(); index++) {
                  matchingSlotArray[index] = matchingSlots.get(index);
               }

               requirements.add(new OverloadProcessingRecipe.PreparedRequirement(requirement.count(), matchingSlotArray, availableCount));
            }

            requirements.sort(
               Comparator.<OverloadProcessingRecipe.PreparedRequirement>comparingInt(requirementx -> requirementx.matchingSlots().length)
                  .thenComparing(Comparator.comparingInt(OverloadProcessingRecipe.PreparedRequirement::baseCount).reversed())
            );
            return Optional.of(new OverloadProcessingRecipe.MatchPlan(slotStacks, requirements));
         } else {
            return Optional.empty();
         }
      }
   }

   public boolean hasRequiredFluid(FluidStack availableFluid, int operations) {
      if (operations <= 0) {
         return false;
      } else {
         return this.fluidInput.isEmpty()
            ? true
            : !availableFluid.isEmpty()
               && FluidStackHelper.sameFluidAndTag(this.fluidInput, availableFluid)
               && availableFluid.getAmount() >= multiplyExactToInt(this.fluidInput.getAmount(), operations);
      }
   }

   public List<ItemStack> getScaledItemResults(int operations) {
      return this.itemResults.stream().map(stack -> stack.m_255036_(multiplyExactToInt(stack.m_41613_(), operations))).toList();
   }

   public FluidStack getScaledFluidResult(int operations) {
      return this.fluidResult.isEmpty() ? FluidStack.EMPTY : new FluidStack(this.fluidResult, multiplyExactToInt(this.fluidResult.getAmount(), operations));
   }

   public ItemStack assemble(OverloadProcessingRecipeInput input, RegistryAccess registries) {
      return this.itemResults.isEmpty() ? ItemStack.f_41583_ : this.itemResults.get(0).m_41777_();
   }

   public boolean m_8004_(int width, int height) {
      return true;
   }

   public ItemStack m_8043_(RegistryAccess registries) {
      return this.itemResults.isEmpty() ? ItemStack.f_41583_ : this.itemResults.get(0).m_41777_();
   }

   public NonNullList<Ingredient> m_7527_() {
      NonNullList<Ingredient> ingredients = NonNullList.m_122779_();

      for (OverloadProcessingIngredient input : this.itemInputs) {
         ingredients.add(input.ingredient());
      }

      return ingredients;
   }

   public RecipeSerializer<?> m_7707_() {
      return (RecipeSerializer<?>)ModRecipeTypes.OVERLOAD_PROCESSING_SERIALIZER.get();
   }

   public RecipeType<?> m_6671_() {
      return (RecipeType<?>)ModRecipeTypes.OVERLOAD_PROCESSING_TYPE.get();
   }

   public boolean m_5598_() {
      return true;
   }

   public boolean m_142505_() {
      return this.totalEnergy < 5L
         || this.lightningCost <= 0
         || this.itemInputs.isEmpty() && this.fluidInput.isEmpty()
         || this.itemResults.isEmpty() && this.fluidResult.isEmpty()
         || this.itemInputs.stream().anyMatch(input -> input.ingredient().m_43908_().length == 0);
   }

   private FluidStack rawFluidInput() {
      return this.fluidInput;
   }

   List<ItemStack> rawItemResults() {
      return this.itemResults;
   }

   FluidStack rawFluidResult() {
      return this.fluidResult;
   }

   private boolean allocateRequirement(
      int requirementIndex,
      List<OverloadProcessingRecipe.PreparedRequirement> requirements,
      List<OverloadProcessingRecipeInput.SlotStack> slotStacks,
      int operations,
      int[] remainingCounts,
      int[] slotConsumptions
   ) {
      if (requirementIndex >= requirements.size()) {
         return true;
      } else {
         OverloadProcessingRecipe.PreparedRequirement requirement = requirements.get(requirementIndex);
         return this.allocateAcrossSlots(
            requirementIndex, requirements, requirement, slotStacks, operations, 0, requirement.baseCount() * operations, remainingCounts, slotConsumptions
         );
      }
   }

   private boolean allocateAcrossSlots(
      int requirementIndex,
      List<OverloadProcessingRecipe.PreparedRequirement> requirements,
      OverloadProcessingRecipe.PreparedRequirement requirement,
      List<OverloadProcessingRecipeInput.SlotStack> slotStacks,
      int operations,
      int slotCursor,
      int needed,
      int[] remainingCounts,
      int[] slotConsumptions
   ) {
      if (needed == 0) {
         return this.allocateRequirement(requirementIndex + 1, requirements, slotStacks, operations, remainingCounts, slotConsumptions);
      } else if (slotCursor >= requirement.matchingSlots().length) {
         return false;
      } else if (this.remainingCapacity(requirement.matchingSlots(), slotCursor, remainingCounts) < needed) {
         return false;
      } else {
         int slotIndex = requirement.matchingSlots()[slotCursor];
         int maxTake = Math.min(needed, remainingCounts[slotIndex]);
         int machineSlot = slotStacks.get(slotIndex).slot();

         for (int take = maxTake; take >= 0; take--) {
            if (take > 0) {
               remainingCounts[slotIndex] -= take;
               slotConsumptions[machineSlot] += take;
            }

            if (this.allocateAcrossSlots(
               requirementIndex, requirements, requirement, slotStacks, operations, slotCursor + 1, needed - take, remainingCounts, slotConsumptions
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

   private static int multiplyExactToInt(int value, int multiplier) {
      long result = (long)value * (long)multiplier;
      if (result > 2147483647L) {
         throw new IllegalArgumentException("scaled stack size exceeds integer range");
      } else {
         return (int)result;
      }
   }

   public final class MatchPlan {
      private final List<OverloadProcessingRecipeInput.SlotStack> slotStacks;
      private final List<OverloadProcessingRecipe.PreparedRequirement> requirements;

      private MatchPlan(List<OverloadProcessingRecipeInput.SlotStack> slotStacks, List<OverloadProcessingRecipe.PreparedRequirement> requirements) {
         this.slotStacks = slotStacks;
         this.requirements = requirements;
      }

      public long maxOperationsByAvailability() {
         long bound = Long.MAX_VALUE;

         for (OverloadProcessingRecipe.PreparedRequirement requirement : this.requirements) {
            bound = Math.min(bound, requirement.available() / (long)requirement.baseCount());
         }

         return bound;
      }

      public Optional<OverloadProcessingRecipeMatch> allocate(int operations) {
         if (operations <= 0) {
            return Optional.empty();
         } else {
            int[] slotConsumptions = new int[9];
            if (this.requirements.isEmpty()) {
               return Optional.of(new OverloadProcessingRecipeMatch(slotConsumptions));
            } else {
               for (OverloadProcessingRecipe.PreparedRequirement requirement : this.requirements) {
                  if (requirement.available() < (long)requirement.baseCount() * (long)operations) {
                     return Optional.empty();
                  }
               }

               int[] remainingCounts = new int[this.slotStacks.size()];

               for (int slotIndex = 0; slotIndex < remainingCounts.length; slotIndex++) {
                  remainingCounts[slotIndex] = this.slotStacks.get(slotIndex).stack().m_41613_();
               }

               return !OverloadProcessingRecipe.this.allocateRequirement(0, this.requirements, this.slotStacks, operations, remainingCounts, slotConsumptions)
                  ? Optional.empty()
                  : Optional.of(new OverloadProcessingRecipeMatch(slotConsumptions));
            }
         }
      }
   }

   private static record PreparedRequirement(int baseCount, int[] matchingSlots, long available) {
   }

   public static final class Serializer implements RecipeSerializer<OverloadProcessingRecipe> {
      public OverloadProcessingRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
         List<OverloadProcessingIngredient> itemInputs = new ArrayList<>();
         if (json.has("inputs")) {
            JsonArray inputsJson = GsonHelper.m_13933_(json, "inputs");
            itemInputs = new ArrayList<>(inputsJson.size());

            for (JsonElement element : inputsJson) {
               itemInputs.add(OverloadProcessingIngredient.fromJson(GsonHelper.m_13918_(element, "inputs[]")));
            }
         }

         List<ItemStack> itemResults = new ArrayList<>();
         if (json.has("results")) {
            JsonArray resultsJson = GsonHelper.m_13933_(json, "results");
            itemResults = new ArrayList<>(resultsJson.size());

            for (JsonElement element : resultsJson) {
               itemResults.add(RecipeSerializationHelper.itemStackFromJson(GsonHelper.m_13918_(element, "results[]")));
            }
         }

         return new OverloadProcessingRecipe(
            recipeId,
            GsonHelper.m_13824_(json, "priority", 0),
            itemInputs,
            RecipeSerializationHelper.optionalFluidStackFromJson(json, "inputFluid"),
            itemResults,
            RecipeSerializationHelper.optionalFluidStackFromJson(json, "resultFluid"),
            GsonHelper.m_13921_(json, "totalEnergy"),
            GsonHelper.m_13824_(json, "lightningCost", 4),
            RecipeSerializationHelper.enumFromJson(json, "lightningTier", OverloadProcessingRecipe.DEFAULT_LIGHTNING_TIER, LightningKey.Tier.values())
         );
      }

      public OverloadProcessingRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
         int inputCount = buffer.readInt();
         List<OverloadProcessingIngredient> itemInputs = new ArrayList<>(inputCount);

         for (int i = 0; i < inputCount; i++) {
            itemInputs.add(OverloadProcessingIngredient.fromNetwork(buffer));
         }

         FluidStack inputFluid = buffer.readBoolean() ? buffer.readFluidStack() : FluidStack.EMPTY;
         int resultCount = buffer.readInt();
         List<ItemStack> itemResults = new ArrayList<>(resultCount);

         for (int i = 0; i < resultCount; i++) {
            itemResults.add(buffer.m_130267_());
         }

         FluidStack resultFluid = buffer.readBoolean() ? buffer.readFluidStack() : FluidStack.EMPTY;
         return new OverloadProcessingRecipe(
            recipeId,
            buffer.readInt(),
            itemInputs,
            inputFluid,
            itemResults,
            resultFluid,
            buffer.readLong(),
            buffer.readInt(),
            (LightningKey.Tier)buffer.m_130066_(LightningKey.Tier.class)
         );
      }

      public void toNetwork(FriendlyByteBuf buffer, OverloadProcessingRecipe recipe) {
         buffer.writeInt(recipe.itemInputs().size());

         for (OverloadProcessingIngredient input : recipe.itemInputs()) {
            input.toNetwork(buffer);
         }

         FluidStack inputFluid = recipe.rawFluidInput();
         buffer.writeBoolean(!inputFluid.isEmpty());
         if (!inputFluid.isEmpty()) {
            buffer.writeFluidStack(inputFluid);
         }

         buffer.writeInt(recipe.rawItemResults().size());

         for (ItemStack itemResult : recipe.rawItemResults()) {
            buffer.m_130055_(itemResult);
         }

         FluidStack resultFluid = recipe.rawFluidResult();
         buffer.writeBoolean(!resultFluid.isEmpty());
         if (!resultFluid.isEmpty()) {
            buffer.writeFluidStack(resultFluid);
         }

         buffer.writeInt(recipe.priority());
         buffer.writeLong(recipe.totalEnergy());
         buffer.writeInt(recipe.lightningCost());
         buffer.m_130068_(recipe.lightningTier());
      }
   }
}
